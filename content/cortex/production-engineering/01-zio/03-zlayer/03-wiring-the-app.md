---
title: '8. Wiring the app: provide & the dependency graph'
summary: `provide` hands a pile of layers to your program, and ZIO assembles the whole dependency graph automatically — shrinking R to Any. Watch Cortex wire its entire backend in one call.
---

# 8. Wiring the app: provide & the dependency graph

## TL;DR

> A program of type `ZIO[BigEnvironment, E, A]` can't run until its `R` is `Any`. **`provide`** is how you get there: hand it the `ZLayer`s for every service, and ZIO **automatically wires the dependency graph** — building each layer in the right order, feeding outputs to inputs, sharing shared services — until every requirement is met and `R` collapses to `Any`. Forget a layer and it's a *compile error* naming exactly what's missing. Cortex's `Main` provides its whole backend — datasource, pipelines, auth, rate limiter, HTTP — in a single `provide(...)`.

## 1. Motivation

You've built services as layers (Chapter 7), each declaring its inputs in its type. A real app has a *graph* of them: the HTTP layer needs the pipelines, the pipelines need the datasource and configs, the datasource needs the DB config. Wiring this by hand — instantiate in topological order, pass each into the next, share the one pool among everyone — is exactly the bookkeeping that rots as the app grows and you add a service in the middle.

`provide` deletes that bookkeeping. You give ZIO the *set* of layers, unordered, and it solves the graph: it reads each layer's input/output types, computes the construction order, builds each service once, and threads them together. You stop maintaining a wiring sequence; you just list the ingredients. And because the graph lives in types, ZIO verifies it at *compile time* — a missing or duplicated dependency fails the build with a precise message, not a 3 a.m. `NullPointerException`.

## 2. Intuition (Analogy)

Flat-pack furniture, two ways. The **bad** way: a 40-step instruction booklet you must follow in exact order, and if step 12 is wrong the wardrobe collapses. The **good** way: you dump all the parts on the floor and a robot that *understands how the parts connect* assembles them — it sees the shelf needs the side panels, the side panels need the base, and builds in the right order without you sequencing anything. Hand it the parts; get a wardrobe.

`provide` is that robot. You hand it the layers (parts) in any order; it reads the types (how parts connect) and assembles the graph correctly. You never write the assembly *sequence* — the types are the instructions, and ZIO follows them.

## 3. Formal Definition

- **`effect.provide(layer1, layer2, ...)`** — supply layers to satisfy `effect`'s `R`. ZIO performs **automatic layer construction (ALC)**: it builds a dependency graph from the layers' input/output types, constructs each in order, **memoizes** shared layers (built once, shared), and produces an effect with `R` reduced by what was provided. Provide everything → `R = Any` → runnable.
- **Compile-time checks.** If a required service has *no* layer, you get a "missing dependency" error naming the type. If *two* layers provide the same service ambiguously, you get an "ambiguous" error. The wiring is verified before the program runs.
- **`provideSome`** — provide *some* dependencies, leaving the rest in `R` (useful in tests, or to provide environment-specific layers higher up).
- **`ZLayer.make[Out](...)`** — build a single combined layer from parts, the same ALC mechanism, when you want a layer rather than a provided effect.
- **Memoization scope.** Within one `provide`, the same layer instance is shared by all consumers — so the *one* `DataSource.live` is the *one* pool everyone uses, not a pool per consumer.

> `provide` turns "a program that needs the world" into "a runnable program," by solving the dependency graph from the types. You list layers; ZIO computes the order and the sharing.

## 4. Worked Example — Cortex wires its backend

Cortex's [`Main`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala) provides its *entire* backend in one call:

```scala
val program: ZIO[HttpApp & DataSourceEnv, Throwable, Unit] =
  Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)

program.provide(
  AppConfig.live,            // reads application.conf
  dbCfg, redisCfg, mongoCfg, // configs sliced from AppConfig
  DataSource.live,           // the ONE HikariCP pool (Chapter 9)
  HelloPipeline.live,        // needs RedisConfig & MongoConfig & JDataSource
  CodeRunPipeline.live,
  CortexPipeline.live,
  BlogPipeline.live,
  Auth.live,                 // the Keycloak verifier (Part 3)
  RateLimiter.live,          // needs RedisConfig
  SubmissionPipeline.live,   // needs JDataSource
  HttpApp.live               // needs ALL the pipelines + auth + rate limiter
)
```

This list is *unordered* — and that's the point. `HttpApp.live` needs the pipelines; the pipelines need `DataSource.live`; `DataSource.live` needs `dbCfg`; `dbCfg` comes from `AppConfig.live`. ZIO reads all those input/output types and figures out the build order itself: config first, then the pool, then the pipelines, then the HTTP layer. The single `DataSource.live` is **memoized** — `HelloPipeline`, `SubmissionPipeline`, and everyone else share *one* connection pool, not one each. And the program's `R` (`HttpApp & DataSourceEnv`) is fully satisfied, so it compiles and runs. Add a new pipeline tomorrow that needs Redis, drop its layer into this list, and ZIO re-solves the graph — you never touch a wiring sequence.

Leave out `redisCfg` and the build fails with a message like *"missing RedisConfig, required by RateLimiter.live"* — the compiler pointing at the exact gap.

## 5. Build It

Run this. It's an automatic layer-construction solver: hand it layers in *any* order and it topologically sorts and builds them, sharing each one — exactly what `provide` does.

```scala run
@main def run(): Unit =
  final case class Layer(
      provides: String,
      needs: Set[String],
      build: Map[String, String] => String
  )

  // Layers in DELIBERATELY scrambled order — provide() doesn't care.
  val layers = List(
    Layer("HttpApp", Set("HelloPipeline", "RateLimiter"),
      e => s"HttpApp(${e("HelloPipeline")}, ${e("RateLimiter")})"),
    Layer("DataSource", Set("DbConfig"), e => "pool@" + e("DbConfig")),
    Layer("AppConfig", Set(), _ => "conf"),
    Layer("DbConfig", Set("AppConfig"), e => "db<" + e("AppConfig") + ">"),
    Layer("RedisConfig", Set("AppConfig"), e => "redis<" + e("AppConfig") + ">"),
    Layer("HelloPipeline", Set("DataSource", "RedisConfig"), e => "Hello(" + e("DataSource") + ")"),
    Layer("RateLimiter", Set("RedisConfig"), e => "RL(" + e("RedisConfig") + ")")
  )

  def provide(target: String, layers: List[Layer]): String =
    val byOut = layers.map(l => l.provides -> l).toMap
    val built = scala.collection.mutable.Map.empty[String, String]
    val order = scala.collection.mutable.ListBuffer.empty[String]
    def build(name: String): String =
      if built.contains(name) then built(name) // memoized — built ONCE, shared
      else
        val l = byOut.getOrElse(name, throw RuntimeException(s"missing dependency: $name")) // the compile error
        l.needs.toList.sorted.foreach(build)   // build inputs first
        val svc = l.build(built.toMap)
        built(name) = svc
        order += name
        svc
    val result = build(target)
    println("build order ZIO computed: " + order.mkString(" -> "))
    result

  println("=> " + provide("HttpApp", layers))
```

**Now break it.** Delete the `"RedisConfig"` layer from the list and re-run. The solver reaches `RateLimiter`'s need for `RedisConfig`, finds no layer, and raises *"missing dependency: RedisConfig"* — naming the gap, just like ZIO's compile error. Notice you never wrote a build order: `provide` derived `AppConfig → DbConfig → DataSource → RedisConfig → HelloPipeline/RateLimiter → HttpApp` purely from the types. That's the whole magic — wiring as a *solved* problem, not a *maintained* one.

## 6. Trade-offs & Complexity

| `provide` (automatic wiring) | Manual wiring / runtime DI |
|---|---|
| Order computed from types | You maintain a build sequence |
| Missing dep = compile error | Missing dep = runtime crash |
| Shared services memoized automatically | Manual singleton management |
| Add a service: drop a layer in the list | Re-thread the wiring |
| Errors can be cryptic for big graphs | Errors are runtime stack traces |

The main friction is *diagnosing* a wiring error in a large graph — ZIO's messages have improved a lot but can still take a moment to parse. That's a small price for never maintaining a construction order and for catching every missing/duplicate dependency at compile time. The bigger the app, the more `provide` earns its keep.

## 7. Edge Cases & Failure Modes

- **Missing layer.** "Missing dependency: X" means no provided layer outputs `X`. Add its layer. (Often you forgot a config slice.)
- **Ambiguous layer.** Two layers provide the same service → ZIO can't choose. Remove one or scope them.
- **Accidentally un-shared services.** If you `provide` the same layer in two *separate* `provide` calls, you get *two* instances (two pools!). Provide shared infrastructure once, at a common boundary.
- **Providing too late/too early.** Provide app-wide infra at the edge (`Main`); provide test doubles in tests via `provideSome`/a different layer. Mixing these up leaks test config into prod or vice versa.

## 8. Practice

> **Exercise 1 — Solve the graph.** Given layers `AppConfig` (no deps), `DbConfig`(needs AppConfig), `DataSource`(needs DbConfig), `Repo`(needs DataSource), what order does `provide` build them in to satisfy a program needing `Repo`?

<details>
<summary><strong>Answer</strong></summary>

```text
AppConfig → DbConfig → DataSource → Repo
```

`provide` performs automatic layer construction: it reads each layer's input/output types and builds inputs *before* the things that need them (§3). The chain here is linear — each service needs exactly the one before it — so there's only one valid topological order. `Repo` needs `DataSource`, which needs `DbConfig`, which needs `AppConfig`, which needs nothing; so construction starts at the only leaf (`AppConfig`) and works *up* the chain to the requested `Repo`.

The point of the exercise: you hand `provide` these four layers **in any order you like**, and ZIO computes this sequence itself from the types. You never write the build order — the types *are* the instructions, and wiring becomes a solved problem rather than a maintained one.

</details>

> **Exercise 2 — Read the error.** A build fails with "missing dependency: MongoConfig, required by HelloPipeline.live." What's the fix, and what does it tell you about `HelloPipeline.live`'s type?

<details>
<summary><strong>Answer</strong></summary>

**The fix:** add a layer that *outputs* `MongoConfig` to the `provide(...)` list (e.g. the `mongoCfg` slice of `AppConfig`). The message "missing dependency: X" means *no provided layer produces X* (§7) — so you supply one. (Almost always it's a forgotten config slice, exactly as in Cortex's `Main`, where `dbCfg, redisCfg, mongoCfg` are listed alongside the pipelines.)

**What it tells you about the type:** the "required by `HelloPipeline.live`" part reveals that `MongoConfig` is one of that layer's *inputs* — its `RIn` includes `MongoConfig`. Indeed `HelloPipeline.live` has type `ZLayer[RedisConfig & MongoConfig & JDataSource, Throwable, HelloPipeline]`, so providing it obliges you to also provide all three of its inputs. This is the dependency graph being checked *at compile time*: the type encodes the need, ZIO sees the gap, and it names both the missing service **and** who required it — pointing you straight at the fix instead of a 3 a.m. `NullPointerException`.

</details>

> **Exercise 3 — One pool, shared.** Explain how memoization ensures `HelloPipeline` and `SubmissionPipeline` share *one* `DataSource`, and what would go wrong if each built its own pool.

<details>
<summary><strong>Answer</strong></summary>

Within a single `provide`, ZIO **memoizes** each layer: a given layer is built *once*, and that one instance is shared by every consumer that needs it (§3). Both `HelloPipeline.live` and `SubmissionPipeline.live` declare `JDataSource` as an input, so when ZIO solves the graph it constructs `DataSource.live` a *single* time and threads that same pool into both pipelines. They aren't each handed a private copy — they're handed *the* pool.

What would go wrong if each built its own pool:

- **Resource exhaustion.** A HikariCP pool holds, say, 10 connections. One pool per consumer means N pools and N × 10 connections — you'd multiply your real connection count and can exhaust the database's connection limit (it starts refusing new connections).
- **Wasted/duplicated resources.** Multiple pools mean duplicated sockets, threads, and memory for something that was meant to be shared infrastructure.
- **Lifecycle confusion.** Each pool is a separate resource to open and close (Chapter 9); more pools means more things that can leak or be shut down inconsistently.

This is also why §7 warns against `provide`-ing the same layer in *two separate* `provide` calls: that *defeats* memoization (each call has its own memo table) and gives you two pools. Provide shared infrastructure **once**, at a common boundary like `Main`, and memoization guarantees one pool for everyone.

</details>

```quiz
{
  "prompt": "What does `.provide(layerA, layerB, ...)` do for a program with a large `R`?",
  "input": "Choose one:",
  "options": [
    "Automatically builds the dependency graph from the layers' types (in the right order, sharing shared ones), reducing R toward Any",
    "Runs the program N times, once per layer",
    "Logs every layer as it executes",
    "Requires you to list the layers in exact dependency order"
  ],
  "answer": "Automatically builds the dependency graph from the layers' types (in the right order, sharing shared ones), reducing R toward Any"
}
```

## In the Wild

- **[ZIO docs — Automatic layer construction](https://zio.dev/reference/contextual/zlayer/#automatic-layer-construction)** — how `provide`/`ZLayer.make` solve the graph, and reading the errors.
- **[ZIO docs — Providing dependencies](https://zio.dev/reference/contextual/#providing-dependencies)** — `provide`, `provideSome`, and scoping.
- **[Cortex `Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala)** — the real one-call wiring of the whole backend.

---

**Next:** some services aren't just *built* — they're *opened* and must be *closed*: a connection pool, a Redis client. Meet `acquireRelease` and `scoped`, and read how Cortex's `DataSource` guarantees its pool is always shut down. → [9. Resourceful layers: acquireRelease & scoped](/cortex/production-engineering/zio/zlayer/resourceful-layers)
