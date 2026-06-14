---
title: '2. The ZIO[R, E, A] type: a lazy recipe'
summary: One type, three questions. ZIO[R, E, A] describes a program that needs an R, may fail with an E, and succeeds with an A. Meet the channels and run one step by step.
---

# 2. The ZIO[R, E, A] type: a lazy recipe

## TL;DR

> The whole framework hangs on one type: **`ZIO[R, E, A]`**. Read it as *"a recipe that, given an environment of type **R**, will either fail with an error of type **E** or succeed with a value of type **A**."* Three type parameters capture three of the hardest problems in programming — **dependencies (R)**, **errors (E)**, and **results (A)** — as ordinary, composable values. You build small `ZIO`s with `succeed`/`fail`/`attempt` and glue them with `map`/`flatMap`. Below you'll watch one execute, channel by channel.

## 1. Motivation

Most languages answer "what does this function do?" with one type — its return type, `A`. But real programs also *need things* (a database connection, config) and *fail in specific ways* (not-found, timeout, bad input), and those facts are usually invisible: dependencies are smuggled in through globals, and errors are untyped exceptions that explode at runtime. You discover both the hard way.

ZIO puts all three in the type. `ZIO[R, E, A]` says, *to the compiler*: this program needs an `R`, can fail with an `E`, and yields an `A`. Now the compiler tracks your dependencies and your failure modes the same way it tracks your return values — and it won't let you forget either. That's a lot of safety from three letters.

## 2. Intuition (Analogy)

A recipe card with three labeled sections:

- **R — "Ingredients you must be supplied"** (a working oven, flour). The recipe can't run until someone provides them.
- **E — "How this can go wrong"** (the soufflé can collapse; the milk can be sour). Known failure modes, listed up front.
- **A — "What you get if it works"** (a cake).

`ZIO[Oven & Flour, Collapsed, Cake]` is a recipe needing an oven and flour, which might collapse, and otherwise yields a cake. You can read the card and know exactly what it requires, how it can fail, and what it produces — *before* you ever turn on the oven. Ordinary code hides the oven requirement in a global and the collapse in an exception; ZIO writes them on the card.

## 3. Formal Definition

`ZIO[R, E, A]` is a value describing an effect with three type parameters:

| Param | Name | Meaning | Common values |
|---|---|---|---|
| **R** | Environment | Services/dependencies the effect needs to run | `Any` (needs nothing), `DataSource`, `R1 & R2` |
| **E** | Error | The typed failure the effect may produce | `Nothing` (can't fail), `Throwable`, your `sealed trait` |
| **A** | Success | The value produced on success | `Int`, `User`, `Unit` |

Handy aliases you'll see constantly:

- **`UIO[A]`** = `ZIO[Any, Nothing, A]` — needs nothing, *cannot fail*, yields `A`.
- **`Task[A]`** = `ZIO[Any, Throwable, A]` — needs nothing, may throw, yields `A`.
- **`IO[E, A]`** = `ZIO[Any, E, A]` — needs nothing, fails with typed `E`.
- **`RIO[R, A]`** = `ZIO[R, Throwable, A]` — needs `R`, may throw.

Constructors and combinators (the verbs):

- `ZIO.succeed(a)` — a recipe that always succeeds with `a` (use for *pure* values).
- `ZIO.fail(e)` — a recipe that always fails with `e`.
- `ZIO.attempt(expr)` — wrap side-effecting `expr`; failures become `E = Throwable`.
- `effect.map(f)` — transform the success `A` (success track).
- `effect.flatMap(f)` — run `effect`, then use its `A` to build the *next* effect (sequencing).

> `map` and `flatMap` are how recipes combine. `flatMap` is sequencing — "do this, *then* (using the result) do that." A `for`-comprehension is just sugar over `flatMap`, and it's how most ZIO reads.

## 4. Worked Example — run one, channel by channel

Below is a `ZIO` that needs two services (R), does some work, and produces HTML (A) — exactly the shape of a Cortex request handler. **Step through it** and watch the **R** environment get consumed, the **A** value evolve, and (since this is the happy path) the **E** channel stay clean.

```d3 widget=zio-effect-stepper
{
  "title": "ZIO[RateLimiter & DataSource, AppError, Html] — a request handler, run step by step",
  "env": ["RateLimiter", "DataSource"],
  "steps": [
    { "op": "ZIO.serviceWithZIO[RateLimiter](_.check(ip))", "kind": "access", "service": "RateLimiter", "yields": "allowed",
      "note": "Pull the RateLimiter out of R and run its check. R now has one of its requirements satisfied; the success value is `allowed`." },
    { "op": "ZIO.service[DataSource]", "kind": "access", "service": "DataSource", "yields": "ds",
      "note": "Pull the DataSource out of R. Both services are now accounted for — when we `provide` them later (Chapter 8), R becomes `Any`." },
    { "op": "attemptBlocking { ds.getConnection.query(sql) }", "kind": "effect", "yields": "rows",
      "note": "Perform the blocking JDBC query as an effect value. On success the A channel now holds `rows`." },
    { "op": ".map(rows => renderHtml(rows))", "kind": "transform", "yields": "html",
      "note": "Transform the success value with `map` — a pure function on the A channel. The final A is `html`." }
  ]
}
```

> Notice the program is *built* as a value (the four operations) and only *means* anything when run. Each `access` step is a requirement being pulled from **R**; `map` works purely on **A**; nothing here touches **E** because nothing failed — that's the next chapter. When you later `provide` the two services, the **R** in the type shrinks to `Any` and the recipe becomes runnable.

## 5. Build It

Run this. It's a tiny `ZIO`-like type in Scala — success + error channels, `succeed`/`fail`, and `map`/`flatMap` — so you can feel how recipes compose without anything running until `.run()`.

```scala run
// A miniature ZIO[Any, E, A]: a description with success + error channels.
enum Result[+A]:
  case Ok(value: A)
  case Err(error: String)
import Result.*

final case class Effect[+A](thunk: () => Result[A]):      // +A: covariant like ZIO — runs only on .run()
  def map[B](f: A => B): Effect[B] = Effect(() =>          // transform success (A track)
    thunk() match
      case Ok(v)  => Ok(f(v))
      case Err(e) => Err(e))                               // errors short-circuit
  def flatMap[B](f: A => Effect[B]): Effect[B] = Effect(() => // sequence: A builds the next Effect
    thunk() match
      case Ok(v)  => f(v).thunk()
      case Err(e) => Err(e))
  def run(): String = thunk() match
    case Ok(v)  => s"SUCCESS: $v"
    case Err(e) => s"FAILED: $e"

object Effect:
  def succeed[A](a: A): Effect[A]      = Effect(() => Ok(a))
  def fail(e: String): Effect[Nothing] = Effect(() => Err(e))

@main def run(): Unit =
  // Build a recipe — NOTHING runs yet:
  val program = Effect.succeed(2)
    .map(_ + 3)                         // A: 2 -> 5
    .flatMap(x => Effect.succeed(x * 10)) // A: 5 -> 50
  println("Built the program. Has it run? No.")
  println(program.run())                // NOW it runs: SUCCESS: 50
  println(program.run())                // referentially transparent — run again, same answer
```

**Now break it.** Insert a failing step: change the `flatMap` to `.flatMap(x => Effect.fail("boom"))`. Re-run and watch `map`/`flatMap` *short-circuit* — once the error channel is set, later transforms are skipped and you get `FAILED: boom`. You just rebuilt ZIO's typed-error short-circuiting (the subject of Chapter 4 and 5) in a few lines.

## 6. Trade-offs & Complexity

| Three type params (ZIO) | One return type (plain code) |
|---|---|
| Dependencies tracked by the compiler | Dependencies hidden in globals |
| Failure modes visible in the type | Errors are untyped exceptions |
| More to type and learn | Less ceremony, less safety |
| Refactors are compiler-guided | Refactors break at runtime |

`ZIO[R, E, A]` asks you to be explicit about things most code leaves implicit. That's more upfront thought — but the compiler then becomes a relentless assistant: forget to provide a dependency, or to handle an error, and it tells you *at compile time* instead of at 3 a.m. For long-lived production code, that trade pays for itself many times over.

## 7. Edge Cases & Failure Modes

- **`E = Nothing` means "cannot fail."** `Nothing` has no values, so a `ZIO[R, Nothing, A]` has nothing it could fail with — the compiler knows it always succeeds. Powerful: it lets you *prove* an effect handles all its errors.
- **`R = Any` means "needs nothing."** Once every dependency is provided, `R` becomes `Any` and the effect is runnable. Watching `R` shrink to `Any` is how you know you've wired everything (Chapter 8).
- **`succeed` vs `attempt`.** `succeed` is for pure values and has `E = Nothing`; `attempt` is for impure code and has `E = Throwable`. Mixing them up either fires effects too early or loses error typing.
- **Forgetting it's lazy.** A `ZIO` in a `val` has done *nothing*. Beginners expect side effects from defining one; nothing happens until the runtime runs it (Chapter 3).

## 8. Practice

> **Exercise 1 — Read the type.** In English, describe `ZIO[DataSource, NotFound, User]`. What does it need, how can it fail, what does it produce?

<details>
<summary><strong>Answer</strong></summary>

Read it one channel at a time (R, E, A):

- **R = `DataSource`** — it *needs* a data source to run; until one is provided, it can't execute.
- **E = `NotFound`** — it *may fail* with exactly a `NotFound` (and, because the error is typed, *only* that — the compiler knows the complete set of expected failures).
- **A = `User`** — on success it *produces* a `User`.

In plain English: *"given a data source, this either fails because the user wasn't found, or yields a user."* The power is that all three facts — its dependency, its failure mode, and its result — are visible in the signature, so the compiler tracks them for you.

</details>

> **Exercise 2 — Pick the alias.** Which alias fits each: (a) reads config, never fails; (b) needs a `Redis`, may throw; (c) parses a string, fails with `ParseError`.

<details>
<summary><strong>Answer</strong></summary>

Pick by what sits in each channel:

- **(a) `URIO[Config, A]`** — needs a `Config` (R), *cannot* fail (the `U` means `E = Nothing`).
- **(b) `RIO[Redis, A]`** — needs a `Redis` (R), and "may throw" means `E = Throwable` (the `R…IO` family).
- **(c) `IO[ParseError, A]`** — needs nothing (R = `Any`), fails with a *typed* `ParseError` (E).

The trick: the aliases are just shorthands for "what's in R, E, A." `U` = un-failing (`E = Nothing`); `Task` = `Any`+`Throwable`; `IO` = typed error, no deps; `RIO` = deps + `Throwable`; `URIO` = deps, can't fail.

</details>

> **Exercise 3 — Compose.** Using the Scala `Effect` from above, write a program that succeeds with `7`, doubles it, then (via `flatMap`) fails if the result is over `10`. Predict the output, then run it.

<details>
<summary><strong>Answer</strong></summary>

```scala
val program = Effect.succeed(7)
  .map(_ * 2)                                              // success track: 7 -> 14
  .flatMap(x => if x > 10 then Effect.fail(s"too big: $x") else Effect.succeed(x))
println(program.run())
```

**Predicted output: `FAILED: too big: 14`.** Reasoning from the channels: `succeed(7)` puts `7` on the success track; `map(_ * 2)` transforms it to `14` (still success); then `flatMap` inspects `14`, sees `14 > 10`, and returns `Effect.fail(...)` — which moves the program onto the **error channel**. Any later `map`/`flatMap` would now *short-circuit*, and `run()` reports `FAILED: too big: 14`. (Change `7` to `4` and the result is `SUCCESS: 8` — the doubled value never exceeds 10, so it stays on the success track.)

</details>

```quiz
{
  "prompt": "What do the three parameters of `ZIO[R, E, A]` represent?",
  "input": "Choose one:",
  "options": [
    "R = the environment/dependencies it needs, E = the error it may fail with, A = the value it produces on success",
    "R = retries, E = exceptions, A = async",
    "R = result, E = endpoint, A = action",
    "R = runtime, E = effect, A = argument"
  ],
  "answer": "R = the environment/dependencies it needs, E = the error it may fail with, A = the value it produces on success"
}
```

## In the Wild

- **[ZIO docs — The ZIO data type](https://zio.dev/reference/core/zio/)** — the three channels and the type aliases, official.
- **[ZIO docs — `map` and `flatMap`](https://zio.dev/reference/core/zio/zio#operations)** — composing effects.
- **[Cortex `Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala)** — a real program whose pieces are exactly these `ZIO` values, wired together (you'll read it next chapter).

---

**Next:** a recipe is useless until a chef cooks it. Meet `ZIOAppDefault` — the runtime that runs your top-level effect — through Cortex's real entry point. → [3. Your first program: ZIOAppDefault](/cortex/production-engineering/zio/why-effects/your-first-zio-program)
