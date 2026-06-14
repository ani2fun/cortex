---
title: '6. The R channel: dependencies you don''t have yet'
summary: The R in ZIO[R, E, A] is the set of services an effect needs to run — declared, not constructed. Learn to ask for a dependency and let someone else supply it later.
---

# 6. The R channel: dependencies you don't have yet

## TL;DR

> The **R** channel is an effect's *requirements* — the services it needs to run, tracked in the type. Instead of *constructing* a database connection inside your logic, you *declare* "I need a `DataSource`" with `ZIO.service[DataSource]`, and that need surfaces in `R`. The effect stays a pure description until someone *provides* the dependency later (Chapter 8). This is dependency injection done by the type system: the compiler tracks every requirement and refuses to run an effect that's still missing one.

## 1. Motivation

Real code needs *things*: a database pool, a Redis client, config, a clock. The naive approach is to reach out and grab them — `new HikariDataSource(...)` right where you need it, or a global `Database` object. Both are poison. Constructing a dependency inline welds your logic to one concrete implementation (good luck testing it without a real database). A global hides the dependency entirely — you can't tell what a function needs by looking at its signature, and you can't swap it.

ZIO's answer: make "what I need" part of the type. An effect that needs a `DataSource` has type `ZIO[DataSource, E, A]`. It doesn't know *which* `DataSource` — real, test, in-memory — only that it needs *a* `DataSource`. The actual one is supplied at the edge. Your logic becomes independent of construction, the compiler tracks every requirement, and tests just provide a fake. The `R` channel is dependency injection that you can't forget to wire, because the type won't compile until you do.

## 2. Intuition (Analogy)

A recipe that begins: **"Requires: a working oven, 200g flour."**

The recipe doesn't *build an oven*. That would be absurd — every cake recipe shipping with instructions to manufacture an oven. It *declares a requirement* and assumes someone will supply a real oven when it's time to bake. The same recipe runs in your kitchen, a friend's kitchen, or a test kitchen with a *toy* oven — because it depends on "an oven," not on *your specific* oven.

`ZIO.service[Oven]` is "Requires: an oven." The requirement lands in `R`. You bake (run) only once an oven is *provided*. Swap a real oven for a test oven and the recipe is unchanged — it never cared which oven, only that there was one.

## 3. Formal Definition

- **R (the environment).** The set of services an effect requires, expressed as an intersection type. `ZIO[DataSource & RedisClient, E, A]` needs *both* a `DataSource` and a `RedisClient`.
- **`ZIO.service[A]`** — access a service of type `A` from the environment; yields `ZIO[A, Nothing, A]` (it *requires* `A` and *produces* `A`). Use it to grab a whole service.
- **`ZIO.serviceWith[A](f)` / `ZIO.serviceWithZIO[A](f)`** — access a service and immediately call a (pure / effectful) method on it. The common shape: `ZIO.serviceWithZIO[RateLimiter](_.check(ip))`.
- **Requirements accumulate.** Compose two effects and their `R`s combine: `effectNeedingDb.flatMap(_ => effectNeedingRedis)` has `R = DataSource & RedisClient`. The compiler sums up everything the whole program needs.
- **`R = Any`** — needs nothing; runnable. Providing dependencies (Chapter 8) shrinks `R` toward `Any`.

> The discipline: **declare needs, don't construct them.** A function's `R` is an honest, compiler-checked list of what it depends on. You read a signature and *know* its dependencies — and you can supply different ones in production and in tests without touching the logic.

## 4. Worked Example — declaring vs grabbing

Two ways to write "increment the visit counter," one poisonous, one clean:

```scala
// ❌ POISON: construct the dependency inline. Welded to HikariCP + a real DB; untestable.
def badVisits: Task[Long] =
  ZIO.attemptBlocking {
    val ds = new HikariDataSource(realConfig)   // who closes this? how do you test it?
    ds.getConnection.createStatement.executeQuery("UPDATE visits ...")
    ...
  }

// ✓ CLEAN: DECLARE the need. R = JDataSource; the actual pool is provided later.
def visits: ZIO[JDataSource, Throwable, Long] =
  ZIO.serviceWithZIO[JDataSource] { ds =>
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try /* UPDATE visits ... RETURNING count */ finally conn.close()
    }
  }
```

The clean version's type, `ZIO[JDataSource, Throwable, Long]`, *announces* "I need a data source." It doesn't know or care whether that's a HikariCP pool against Postgres (production) or an in-memory H2 (a test). When Cortex's `HelloPipeline` does exactly this — `ZIO.attemptBlocking { val conn = ds.getConnection; ... }` against a `ds` it received, never one it constructed — it's choosing testability and flexibility. The pool is built *once*, centrally (Chapter 9), and provided to everything that declared it needs one.

## 5. Build It

Run this. It models the `R` channel as an explicit "environment" you must supply before an effect can run — and shows how the same logic runs against a real or a fake dependency.

```scala run
@main def run(): Unit =
  // The service our effect will require.
  trait Db:
    def increment(): Int

  // An effect that REQUIRES an environment before it can run (a tiny `R` channel).
  final case class REff[A](needs: Set[String], run: Map[String, Db] => A):
    // call a method on the required service; the requirement travels with it
    def use[B](method: A => B): REff[B] = REff(needs, env => method(run(env)))
    // supply the dependencies -> runnable; refuse if any are missing
    def provide(env: (String, Db)*): A =
      val provided = env.toMap
      val missing  = needs -- provided.keySet
      if missing.nonEmpty then
        throw RuntimeException(s"won't run: missing dependencies $missing") // the 'R != Any' compile error
      run(provided)

  object REff:
    // declare a requirement; yield the service from the environment
    def service(name: String): REff[Db] = REff(Set(name), env => env(name))

  // A REAL datasource and a FAKE one — the same effect runs against either.
  class RealDb extends Db:
    private var n = 0
    def increment(): Int = { n += 1; n }
  class FakeDb extends Db:
    def increment(): Int = 999 // deterministic, for tests

  // Declare a need — NO construction here:
  val visitCount: REff[Int] = REff.service("datasource").use(_.increment())
  println(s"This effect requires: ${visitCount.needs}")

  // Same effect, two worlds:
  println(s"with real db: ${visitCount.provide("datasource" -> RealDb())}") // 1
  println(s"with fake db: ${visitCount.provide("datasource" -> FakeDb())}") // 999

  // Forget to provide it -> it refuses to run (the type-level guarantee, here at runtime):
  try visitCount.provide()
  catch case e: RuntimeException => println(s"error: ${e.getMessage}")
```

**Now break it.** Call `visitCount.provide()` with no arguments (already shown) — it refuses, listing the missing dependency. In real ZIO this isn't a runtime error at all: an effect whose `R` isn't `Any` simply *won't compile* at the run point. The requirement you declared is a promissory note the compiler makes you honor before the program can run.

## 6. Trade-offs & Complexity

| Declare-and-provide (R channel) | Construct inline / globals |
|---|---|
| Dependencies visible in the type | Hidden in code or globals |
| Swap real/test trivially | Welded to one implementation |
| Compiler enforces wiring | Forgotten wiring = runtime crash |
| One central place to build services | Scattered, duplicated construction |

The cost is indirection: you can't just `new` the thing and go; you declare a need and wire it elsewhere. For a throwaway script that's overkill. For a system with databases, caches, and tests, it's the difference between a codebase you can test and evolve and one welded to its infrastructure. The compiler tracking `R` means you *cannot* ship an effect with an unmet dependency.

## 7. Edge Cases & Failure Modes

- **Constructing in the effect.** `ZIO.attempt(new HikariDataSource(...))` inside business logic re-welds you to a concrete dependency and usually leaks the resource. Declare the need; build the pool centrally (Chapter 9).
- **Giant `R`.** If one effect requires a dozen services, that's a smell — it's doing too much. Split it; let small effects have small `R`s.
- **Leaking `R` upward forever.** Provide dependencies at a sensible boundary (often the app edge). Threading a huge `R` through every layer untouched suggests you should provide some locally.
- **`Has`-style boilerplate (old ZIO 1).** Modern ZIO 2 (Cortex's version) uses plain intersection types for `R` — if you find `Has[...]` in a tutorial, it's outdated.

## 8. Practice

> **Exercise 1 — Read the requirements.** What does `ZIO[DataSource & RedisClient & Clock, Throwable, Report]` need to run? How many things must be provided before `R` is `Any`?

<details>
<summary><strong>Answer</strong></summary>

Read the `R` channel as an *intersection type* — the `&` means "all of these at once" (§3). So this effect requires **three** services to run: a `DataSource`, a `RedisClient`, **and** a `Clock`. Until all three are supplied, `R` is not `Any` and the effect won't run (in real ZIO, it won't even compile at the run point).

So **three things** must be provided. Each one you supply shrinks `R` toward `Any`: provide the `DataSource` and `R` drops to `RedisClient & Clock`; add the `RedisClient` and it's `Clock`; add the `Clock` and `R` finally collapses to `Any` — runnable. (The `Throwable` and `Report` are the `E` and `A` channels and have nothing to do with what must be *provided* — only the `R` side is a list of requirements.)

</details>

> **Exercise 2 — Refactor the poison.** Rewrite a function that does `new RedisClient(...)` internally so it instead *declares* a `RedisClient` requirement. What does its new type look like, and why is it now testable?

<details>
<summary><strong>Answer</strong></summary>

The poison version constructs the dependency inline, welding the logic to one concrete client:

```scala
// ❌ POISON: builds the client itself — untestable, and who closes it?
def cacheGet(key: String): Task[Option[String]] =
  ZIO.attempt {
    val redis = new RedisClient("redis://prod:6379")   // hard-wired to a real server
    redis.get(key)
  }
```

Refactor it to *declare* the need with `ZIO.serviceWithZIO` instead of `new`-ing one:

```scala
// ✓ CLEAN: declares "I need a RedisClient"; the real one is provided later.
def cacheGet(key: String): ZIO[RedisClient, Throwable, Option[String]] =
  ZIO.serviceWithZIO[RedisClient](redis => ZIO.attempt(redis.get(key)))
```

The new type is `ZIO[RedisClient, Throwable, Option[String]]` — the requirement has surfaced in `R`. It's now testable because the function no longer *cares which* `RedisClient` it gets, only that it gets one: in production you provide a real client against Redis, and in a test you provide an in-memory fake — the logic is unchanged. Construction has moved out of the logic and to the edge (Chapter 8), exactly the §3 discipline: declare needs, don't construct them.

</details>

> **Exercise 3 — Sum the needs.** If effect A has `R = DataSource` and effect B has `R = Redis`, what is the `R` of `A *> B`? Why does composing effects *combine* their requirements?

<details>
<summary><strong>Answer</strong></summary>

The combined `R` is:

```scala
DataSource & Redis
```

`A *> B` is "run `A`, then run `B`" — a single effect that *does both*. To run the whole thing you need everything `A` needs **and** everything `B` needs, so their requirements combine with `&` (the intersection type, §3). Requirements *accumulate* under composition because the combined effect can't run unless every part of it can: if either `DataSource` or `Redis` is missing, some step would have an unmet need, so the type honestly demands both.

This is the whole point of tracking `R` in the type — the compiler *sums up* the dependencies of an entire program automatically. You never tally them by hand; you just compose effects, and `R` grows to reflect the full, honest set of services the assembled program requires. Providing them later (Chapter 8) shrinks that intersection back toward `Any`.

</details>

```quiz
{
  "prompt": "What does the R channel in `ZIO[R, E, A]` represent, and how do you get a service from it?",
  "input": "Choose one:",
  "options": [
    "R is the set of services the effect requires; you access one with `ZIO.service[X]` and provide the real one later",
    "R is the return value; you read it with `.get`",
    "R is the retry count; you set it with `.retry`",
    "R is the runtime; you start it with `.run`"
  ],
  "answer": "R is the set of services the effect requires; you access one with `ZIO.service[X]` and provide the real one later"
}
```

## In the Wild

- **[ZIO docs — The environment (`R`)](https://zio.dev/reference/contextual/)** — accessing services and how `R` composes.
- **[ZIO docs — `ZIO.service` and `serviceWithZIO`](https://zio.dev/reference/service-pattern/)** — the access combinators used above.
- **[Cortex `HelloPipeline.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/helloPipeline/HelloPipeline.scala)** — real effects that *declare* their `JDataSource` / Redis / Mongo needs rather than constructing them.

---

**Next:** you can *declare* a need for a `DataSource` — but something has to *build* one. That's a `ZLayer`: a recipe for constructing a service. Build Cortex's `HelloPipeline.live`. → [7. Building services with ZLayer](/cortex/production-engineering/zio/zlayer/building-services)
