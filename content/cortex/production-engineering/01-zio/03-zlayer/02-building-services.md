---
title: '7. Building services with ZLayer'
summary: A ZLayer is a recipe for constructing a service — possibly from other services. Learn the service pattern (a trait plus a `live` layer) and read Cortex''s real HelloPipeline.live.
---

# 7. Building services with ZLayer

## TL;DR

> If `ZIO.service[X]` *declares* a need for service `X`, a **`ZLayer[RIn, E, ROut]`** is the recipe for *building* `X` — it takes some input services `RIn`, may fail with `E`, and produces output service(s) `ROut`. The idiomatic shape (the **service pattern**) is a `trait` defining the capability plus a `val live: ZLayer[...]` that constructs the real implementation. Layers compose: one layer's output feeds another's input, and ZIO assembles the whole dependency graph for you. Cortex's `HelloPipeline.live` builds its service out of a Redis config, a Mongo config, and a datasource.

## 1. Motivation

You've separated "needing a service" (the `R` channel) from "having one." Now: who *makes* the service, and how do you make one service out of others? A `RateLimiter` needs a Redis connection; a `HelloPipeline` needs a database, a cache, and an event log. Constructing these by hand — in the right order, wiring outputs to inputs, remembering to close them — is exactly the tedious, error-prone glue that dependency-injection frameworks exist to remove.

`ZLayer` *is* that framework, but as plain values with no reflection or magic. A layer is a recipe: "give me a `RedisConfig` and I'll produce a `RateLimiter`." Because recipes are values, they *compose* — feed the `RateLimiter` layer into something that needs a rate limiter, and ZIO threads the dependency graph together, in dependency order, at compile time. You describe each service's construction once; ZIO wires the rest.

## 2. Intuition (Analogy)

A factory's **assembly instructions**. Each station has a card: *"Inputs: an engine and a chassis. Output: a car."* Another: *"Inputs: raw steel. Output: a chassis."* The cards don't build anything by themselves — they *describe* how to build one thing from other things. Hand the whole stack of cards to the factory manager and they figure out the order: make steel → make chassis → (with an engine) make car.

A `ZLayer[RIn, E, ROut]` is one assembly card: `RIn` are the inputs (other services), `ROut` is what it outputs, `E` is how assembly can fail. Stack the cards (compose layers) and ZIO is the manager that orders construction correctly. You never write "build the chassis before the car"; the *types* (one card's output is another's input) determine the order.

## 3. Formal Definition

- **`ZLayer[RIn, E, ROut]`** — a recipe that, given input services `RIn`, builds output services `ROut`, possibly failing with `E`. Aliases: a layer needing nothing is `ZLayer[Any, E, ROut]`; `ULayer[ROut]` can't fail.
- **The service pattern** (idiomatic ZIO 2):
  1. A `trait Service` describing the capability (the *interface*).
  2. A companion `object` with `val live: ZLayer[Deps, E, Service]` that constructs the real implementation.
  3. Optional accessor methods that delegate to `ZIO.serviceWithZIO[Service](...)`.
- **Layer constructors:**
  - `ZLayer.succeed(value)` — a layer from an already-built value (no deps, can't fail).
  - `ZLayer.fromFunction(deps => impl)` — build from input services, purely.
  - `ZLayer.fromZIO(effect)` / `ZLayer(effect)` — build via an *effect* (when construction does I/O).
  - `ZLayer.scoped(...)` — build a resource that needs cleanup (Chapter 9).
- **Composition:**
  - `a >>> b` — *vertical*: feed `a`'s output into `b`'s input (`a` then `b`).
  - `a ++ b` — *horizontal*: combine two independent layers into one providing both outputs.

> A layer is "how to build a service." Plain values, composed by types — the dependency graph is wired by the compiler, not by you remembering an order.

## 4. Worked Example — Cortex's HelloPipeline.live

Here is Cortex's real [`HelloPipeline`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/helloPipeline/HelloPipeline.scala), the textbook service pattern. First the *interface*, then the *layer*:

```scala
// 1. The trait — WHAT the service can do (no implementation).
trait HelloPipeline:
  def greet: IO[HelloFailure, Greeting]
  def recent(limit: Int): IO[HelloFailure, RecentCalls]
  def health: UIO[HealthStatus]

object HelloPipeline:
  // 2. The `live` layer — HOW to build the real one, from THREE input services.
  val live: ZLayer[RedisConfig & MongoConfig & JDataSource, Throwable, HelloPipeline] =
    ZLayer.scoped {
      for
        redisCfg <- ZIO.service[RedisConfig]    // pull each input out of the environment
        mongoCfg <- ZIO.service[MongoConfig]
        ds       <- ZIO.service[JDataSource]
        cache    <- liveCache(redisCfg)         // open the Redis connection (a resource)
        events   <- liveEventLog(mongoCfg)      // open the Mongo client (a resource)
      yield HelloPipelineLive(liveVisits(ds), cache, events)   // assemble the implementation
    }
```

Read the type `ZLayer[RedisConfig & MongoConfig & JDataSource, Throwable, HelloPipeline]` as the assembly card: *inputs* = a Redis config, a Mongo config, and a datasource; *output* = a `HelloPipeline`; *may fail* with `Throwable` (opening a connection can fail). The `for`-comprehension pulls each input from the environment (`ZIO.service[...]`), opens the cache and event-log resources, and `yield`s the assembled `HelloPipelineLive`. Crucially, this layer *declares its own inputs in its type* — so whatever provides `HelloPipeline.live` must, in turn, provide a `RedisConfig`, a `MongoConfig`, and a `JDataSource`. The graph is encoded in the types, and Chapter 8 shows ZIO assembling it.

(That `ZLayer.scoped` is because the cache and event log are *resources* that must be closed — Chapter 9. A purely-computed service would use `ZLayer.fromFunction`.)

## 5. Build It

Run this. It models layers as "build recipes" with inputs and outputs, and an assembler that wires them in dependency order — your own miniature `ZLayer` graph.

```scala run
@main def run(): Unit =
  // A recipe: build an output service from named input services.
  final case class Layer(
      provides: String,
      needs: List[String],
      build: Map[String, String] => String
  )

  // Leaf layers (no inputs) — like ZLayer.succeed:
  val redisCfg = Layer("RedisConfig", Nil, _ => "redis://localhost:6379")
  val ds       = Layer("DataSource", Nil, _ => "pg-pool")

  // A layer that NEEDS other services — like HelloPipeline.live:
  val hello = Layer(
    "HelloPipeline",
    List("RedisConfig", "DataSource"),
    env => s"HelloPipeline(cache=${env("RedisConfig")}, db=${env("DataSource")})"
  )

  def assemble(target: String, allLayers: List[Layer]): String =
    val byOutput = allLayers.map(l => l.provides -> l).toMap
    val built    = scala.collection.mutable.Map.empty[String, String]
    def build(name: String): String =
      if built.contains(name) then built(name)
      else
        val layer = byOutput(name)   // missing -> NoSuchElementException (the 'missing dep' error)
        layer.needs.foreach(build)   // build inputs FIRST (dependency order)
        val svc = layer.build(built.toMap)
        built(name) = svc
        println(s"   built $name")
        svc
    build(target)

  println("Assembling HelloPipeline (ZIO figures out the order):")
  val result = assemble("HelloPipeline", List(redisCfg, ds, hello))
  println(s"=> $result")
```

**Now break it.** Remove `ds` from the `allLayers` list passed to `assemble` and re-run. The assembler hits `byOutput("DataSource")` and throws a `NoSuchElementException` — it can't build `HelloPipeline` because a required input layer is missing. In real ZIO this is a *compile-time* error with a precise message ("missing `DataSource`"), not a runtime crash. The dependency graph being in the types means ZIO checks the whole wiring before your program ever runs (Chapter 8).

## 6. Trade-offs & Complexity

| ZLayer service pattern | Manual construction / DI frameworks |
|---|---|
| Wiring checked by the compiler | Runtime DI errors (or hand-wiring) |
| No reflection/magic — just values | Annotation/reflection magic |
| Test by providing a different layer | Mocking frameworks |
| Resource cleanup built in (Chapter 9) | Manual close, easy to leak |
| Trait + live boilerplate per service | Less ceremony, less safety |

The cost is ceremony: every service is a trait plus a `live` layer plus maybe accessors. For three services it feels heavy; for thirty (Cortex has many) it's what keeps wiring sane, testable, and compiler-verified. The service pattern is repetitive on purpose — predictable structure you (and the compiler) can rely on.

## 7. Edge Cases & Failure Modes

- **Putting logic in the trait.** The trait is the *interface*; keep it abstract. Implementation lives in the `live` layer's constructed instance, so you can provide alternatives.
- **A layer with too many inputs.** A `ZLayer` needing ten services often signals a service doing too much — split it.
- **Building resources without `scoped`.** If construction opens something (a connection, a file), use `ZLayer.scoped`/`acquireRelease` (Chapter 9) or you'll leak it. `HelloPipeline.live` uses `scoped` for exactly this reason.
- **Memoization surprises.** By default, providing the *same* layer twice builds it *once* (layers are memoized within a provide). Usually what you want (one shared pool), but know it so you're not surprised a "fresh" layer is shared.

## 8. Practice

> **Exercise 1 — Read the card.** For `ZLayer[DataSource, Throwable, SubmissionPipeline]`, name the input(s), the output, and how it can fail.

<details>
<summary><strong>Answer</strong></summary>

Read the type as an assembly card, `ZLayer[RIn, E, ROut]` (§3):

- **Input (`RIn` = `DataSource`):** one input service — a `DataSource`. To build this layer, something must first provide a `DataSource`.
- **Output (`ROut` = `SubmissionPipeline`):** it produces a `SubmissionPipeline` — that's the service this card knows how to assemble.
- **Failure (`E` = `Throwable`):** *constructing* it can fail with a `Throwable` (e.g. opening a connection or preparing a statement during build throws). Note this is the failure of *assembly*, not of the pipeline's later operations.

In one sentence: "give me a `DataSource` and I'll build you a `SubmissionPipeline`, though the building might throw." Because `DataSource` is declared as its input *in the type*, whoever provides this layer must, in turn, provide a `DataSource` — the dependency is encoded for ZIO to wire (Chapter 8).

</details>

> **Exercise 2 — Write a layer.** Sketch a `trait Clock { def now: UIO[Long] }` and a `ZLayer.succeed`-based `live` for a real clock and a `test` layer returning a fixed time. Why does having two layers make time testable?

<details>
<summary><strong>Answer</strong></summary>

The trait is the *interface* (keep it abstract, §7); each layer is a different *implementation* of it, both built with `ZLayer.succeed` (a leaf layer from an already-built value, no deps, can't fail):

```scala
trait Clock:
  def now: UIO[Long]

object Clock:
  // real: reads the actual system clock (an effect, wrapped in succeed at the edge)
  val live: ULayer[Clock] =
    ZLayer.succeed(new Clock:
      def now: UIO[Long] = ZIO.succeed(System.currentTimeMillis()))

  // test: a frozen clock that always returns the same instant
  def test(fixed: Long): ULayer[Clock] =
    ZLayer.succeed(new Clock:
      def now: UIO[Long] = ZIO.succeed(fixed))
```

Having two layers makes time testable because *the logic depends on `Clock`, not on the wall clock*. Code that calls `ZIO.serviceWithZIO[Clock](_.now)` declares a `Clock` requirement (`R`) and never reads `System.currentTimeMillis` itself — so reading the clock, an effect that is otherwise non-deterministic (Chapter 1: same call, different answer), is pushed out to whatever provides the layer. In production you `provide(Clock.live)`; in a test you `provide(Clock.test(0L))` and now "what time is it?" is a fixed, reproducible value. You can assert on timestamps, test "has the token expired?" at an exact instant, and never get a flaky test that depends on *when* it ran. Swapping the implementation without touching the logic is the entire payoff of the service pattern.

</details>

> **Exercise 3 — Compose.** You have `redisConfigLayer: ULayer[RedisConfig]` and `rateLimiterLayer: ZLayer[RedisConfig, Throwable, RateLimiter]`. Write the composition that yields a `ZLayer[Any, Throwable, RateLimiter]`, and say which operator (`>>>` or `++`) you used and why.

<details>
<summary><strong>Answer</strong></summary>

```scala
val rateLimiter: ZLayer[Any, Throwable, RateLimiter] =
  redisConfigLayer >>> rateLimiterLayer
```

Use **`>>>` (vertical composition)** because one layer's *output* feeds the other's *input*. `redisConfigLayer` produces a `RedisConfig` (its `ROut`), and `rateLimiterLayer` *needs* a `RedisConfig` (its `RIn`). `>>>` plugs the first's output into the second's input — "build the config, then build the rate limiter from it." The config requirement gets satisfied internally, so the combined layer needs nothing (`RIn = Any`) and outputs the `RateLimiter`: exactly `ZLayer[Any, Throwable, RateLimiter]`.

Why not `++`? `++` is *horizontal* — it combines two **independent** layers into one that provides *both* outputs side by side, when neither feeds the other. Here they're not independent: the rate limiter *depends on* the config, so `redisConfigLayer ++ rateLimiterLayer` wouldn't wire the config into the limiter, and its type would still demand a `RedisConfig` as input rather than collapsing to `Any`. A dependency between layers means vertical (`>>>`); peers with no relationship mean horizontal (`++`).

</details>

```quiz
{
  "prompt": "What is a `ZLayer[RIn, E, ROut]`?",
  "input": "Choose one:",
  "options": [
    "A recipe for constructing the service(s) ROut from input services RIn, possibly failing with E",
    "A logging layer that wraps every effect",
    "The retry policy for an effect",
    "A network layer in the OSI model"
  ],
  "answer": "A recipe for constructing the service(s) ROut from input services RIn, possibly failing with E"
}
```

## In the Wild

- **[ZIO docs — ZLayer](https://zio.dev/reference/contextual/zlayer/)** — constructors, composition (`>>>`, `++`), and memoization.
- **[ZIO docs — The Service Pattern](https://zio.dev/reference/service-pattern/)** — trait + `live` + accessors, the shape used above.
- **[Cortex `HelloPipeline.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/helloPipeline/HelloPipeline.scala)** — the real `live` layer dissected here.

---

**Next:** you have a stack of layers. How does the whole graph get assembled and handed to your program? Meet `provide`, and read Cortex's `Main` wire its entire backend in one call. → [8. Wiring the app: provide & the dependency graph](/cortex/production-engineering/zio/zlayer/wiring-the-app)
