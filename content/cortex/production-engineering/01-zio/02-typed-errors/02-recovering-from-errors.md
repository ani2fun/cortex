---
title: '5. Recovering: catchAll, orElse, fold'
summary: An error in the E channel isn''t the end — it''s a value you can handle. Meet the combinators that catch failures and put an effect back on the success track, and watch one recover live.
---

# 5. Recovering: catchAll, orElse, fold

## TL;DR

> Because failures are *values* in the **E** channel, recovering from them is just transforming that channel — the mirror image of `map` on the success side. **`catchAll`** handles every error by returning a new effect; **`orElse`** falls back to an alternative effect; **`fold`/`foldZIO`** handle *both* the error and success branches at once. Recovering typically *changes the `E` type* — often to `Nothing` once you've handled everything, which is the compiler certifying "this can no longer fail." Below, watch an effect fail, short-circuit, and get caught.

## 1. Motivation

In Chapter 4 we put expected errors in `E`. But a type that says "this can fail with `NotFound`" is only useful if you can *do something* about the `NotFound` — return a guest page, try a cache, log and default. The beautiful symmetry of ZIO is that since both success and failure are channels of values, the *same kind of operations* work on both. You already know `map` and `flatMap` transform the success channel; recovering is the identical idea on the error channel.

And there's a compiler superpower here: handling an error *removes it from the type*. A `ZIO[R, NotFound, User]` that you `catchAll` into a default becomes `ZIO[R, Nothing, User]` — and `Nothing` in `E` means *the compiler now guarantees it cannot fail*. Recovery isn't just runtime behavior; it's a proof, checked at compile time, that you've dealt with everything.

## 2. Intuition (Analogy)

A train on two tracks: a **success track** (top) and an **error track** (bottom) — "railway-oriented programming." Each operation is a station.

Normally the train rolls along the success track, station to station (`map`, `flatMap`). The instant something fails, a switch throws and the train **derails onto the error track**, *skipping* the remaining success stations — there's no point rendering a page for a user you couldn't find. The train coasts along the error track until it reaches a **recovery station** (`catchAll`, `orElse`), which switches it *back* onto the success track with a fallback value. From there, normal stations resume.

| Railway | ZIO |
|---|---|
| Success track | the `A` channel |
| Error track | the `E` channel |
| A switch throwing | `ZIO.fail` derailing the train |
| Skipped success stations | short-circuiting (later `map`/`flatMap` skipped) |
| A recovery station | `catchAll` / `orElse` / `fold` |
| Back on the success track | error handled; `E` may become `Nothing` |

## 3. Formal Definition

The recovery combinators (all operate on the `E` channel):

| Combinator | What it does | Resulting `E` |
|---|---|---|
| `effect.catchAll(e => fallbackEffect)` | Catch *every* error; return a new effect built from `e` | the fallback's `E` (often `Nothing`) |
| `effect.catchSome { case ... => ... }` | Catch *some* errors (a partial function); others pass through | possibly narrower `E` |
| `effect.orElse(other)` | If `effect` fails, run `other` instead (ignores the error value) | `other`'s `E` |
| `effect.fold(onError, onSuccess)` | Handle *both* branches with pure functions → a `UIO` | `Nothing` (can't fail) |
| `effect.foldZIO(onError, onSuccess)` | Handle both branches, each returning an *effect* | the branches' `E` |
| `effect.either` | Turn `ZIO[R, E, A]` into `ZIO[R, Nothing, Either[E, A]]` | `Nothing` (error moved into `A`) |
| `effect.retry(schedule)` | Re-run on failure per a `Schedule` (e.g. 3 times) | same `E` (if retries exhaust) |

> Recovering is `map`/`flatMap` for the error channel. And handling *all* errors is how `E` collapses to `Nothing` — the type-level certificate that the effect is now total. Watch the `E` parameter shrink as you add handlers; that's the compiler tracking your safety.

## 4. Worked Example — derail and recover

Step through a handler that looks up a user, *fails* with `NotFound`, skips the rendering step (short-circuit), and then **recovers** to a guest page. Watch the **E** channel light up red on the `fail` step and clear when `catchAll` catches it — and notice the success-track step in between is *skipped*.

```d3 widget=zio-effect-stepper
{
  "title": "find-then-render, with a NotFound that gets recovered",
  "env": ["DataSource"],
  "steps": [
    { "op": "ZIO.service[DataSource]", "kind": "access", "service": "DataSource", "yields": "ds",
      "note": "Pull the DataSource from R. Success track, value = ds." },
    { "op": "attemptBlocking(ds.findUser(id))", "kind": "effect", "yields": "maybeUser",
      "note": "Run the query. It returns no row for this id — so the next step decides to fail." },
    { "op": "ZIO.fail(NotFound(id))", "kind": "fail", "error": "NotFound(id)",
      "note": "No user found. We FAIL with a typed NotFound — the train derails onto the error channel (E)." },
    { "op": ".map(user => renderProfile(user))", "kind": "transform", "yields": "html",
      "note": "SKIPPED. Once on the error track, success-track steps like this `map` are short-circuited — there is no user to render." },
    { "op": ".catchAll(_ => ZIO.succeed(guestPage))", "kind": "recover", "yields": "guestPage",
      "note": "catchAll catches the NotFound and returns a fallback effect (the guest page). The train is back on the success track — and E has collapsed to Nothing: this can no longer fail." }
  ]
}
```

> Two things to internalize. First, **short-circuiting**: the `map(renderProfile)` step is *skipped* because the effect already failed — ZIO doesn't run success steps on a failed effect, just as a derailed train passes no more success stations. Second, **`E` collapses**: after `catchAll` handles the only error, the effect's type is `ZIO[DataSource, Nothing, Html]` — the compiler now *knows* it always produces an `Html`. That `Nothing` is a guarantee you earned by handling every case.

## 5. Build It

Run this. It implements the railway — success/error channels with `map`, `catchAll`, and `orElse` — so you can watch short-circuiting and recovery in code.

```scala run
enum Result[+A]:
  case Ok(value: A)
  case Err(error: String)
import Result.*

final case class Eff[A](run: () => Result[A]):
  def map[B](f: A => B): Eff[B] = Eff(() =>          // success track only
    run() match
      case Ok(v) =>
        println("   map: apply")
        Ok(f(v))
      case Err(e) =>
        println("   map: SKIPPED (on error track)")
        Err(e))
  def catchAll[B >: A](handler: String => Eff[B]): Eff[B] = Eff(() =>  // error track -> back to success
    run() match
      case Err(e) =>
        println(s"   catchAll: caught \"$e\", recovering")
        handler(e).run()
      case Ok(v) => Ok(v))
  def orElse[B >: A](other: Eff[B]): Eff[B] = catchAll(_ => other)

object Eff:
  def succeed[A](a: A): Eff[A]      = Eff(() => Ok(a))
  def fail(e: String): Eff[Nothing] = Eff(() => Err(e))

@main def run(): Unit =
  // A failing pipeline that recovers:
  val pipeline = Eff.fail("NotFound(42)")
    .map(user => s"profile<$user>")                // SKIPPED — already failed
    .catchAll(err => Eff.succeed("guest-page"))    // recover -> back on the success track

  println("running pipeline:")
  pipeline.run() match
    case Ok(v)  => println(s"result: ok $v")       // ok guest-page — recovered, can no longer fail
    case Err(e) => println(s"result: err $e")
```

**Now break it.** Remove the `.catchAll(...)` line from `pipeline` and run again — the result is now `Err("NotFound(42)")`, an *unhandled* failure. In ZIO, that difference shows up *in the type*: without the handler, `E = NotFound`; with it, `E = Nothing`. The compiler would refuse to let an unhandled `E` reach a place that requires totality — turning "did I handle this error?" from a runtime hope into a compile-time fact.

## 6. Trade-offs & Complexity

| Recover on the typed channel (ZIO) | try/catch (imperative) |
|---|---|
| Compiler tracks which errors remain | No record of what's handled |
| `E = Nothing` proves totality | "Did I catch everything?" is a guess |
| `catchSome`/`orElse`/`fold` compose cleanly | Nested try/catch gets messy |
| Retries/fallbacks are values (`retry`, `orElse`) | Hand-rolled retry loops |

The mental shift — "errors are values I transform" instead of "exceptions I catch" — takes a beat, but it buys composability (recoveries chain like any other effect) and the totality guarantee. The main pitfall is *over*-recovering: `catchAll` that swallows a *defect* you should have let crash (Chapter 4). Recover from failures; let bugs die.

## 7. Edge Cases & Failure Modes

- **`catchAll` swallowing defects.** `catchAll` handles the typed `E` (failures), not defects — but a too-eager broad recovery (e.g. `catchAllCause` used carelessly) can bury bugs. Recover failures; leave defects to crash.
- **Recovering too early.** Catch an error at the lowest level and you may lose context the caller needed. Sometimes the right move is to *transform* the error (`mapError`) and let a higher level decide.
- **Infinite retries.** `retry` with an unbounded schedule on a permanently-failing effect loops forever. Bound retries (count or duration) and add backoff (Chapter 12).
- **`orElse` hides the error.** `orElse` ignores *why* the first effect failed. If the reason matters (log it, branch on it), use `catchAll`/`catchSome` instead.

## 8. Practice

> **Exercise 1 — Pick the combinator.** For each, choose `catchAll`, `orElse`, `fold`, or `retry`: (a) on DB failure, return an empty list; (b) try the cache, else the DB; (c) produce an HTTP status from either branch; (d) re-attempt a flaky network call 3 times.

<details>
<summary><strong>Answer</strong></summary>

Match each to *what the situation does with the error* (§3):

- **(a) On DB failure, return an empty list → `catchAll`.** You're handling *every* error by returning a fallback effect (`_ => ZIO.succeed(Nil)`). You don't care about the error value, but `catchAll` is the general "catch and recover to a new effect" tool. (`orElse(ZIO.succeed(Nil))` works too, since you ignore the error — both are defensible; `catchAll` makes "swallow the error, default the value" explicit.)
- **(b) Try the cache, else the DB → `orElse`.** Two interchangeable success paths where you don't care *why* the first failed — `cacheLookup.orElse(dbLookup)`. That's exactly `orElse`'s job: fall back to an alternative effect, ignoring the error value.
- **(c) Produce an HTTP status from either branch → `fold`.** You must handle *both* channels — turn a failure into (say) a 404 and a success into a 200 — collapsing to a value that can't fail. `fold(onError, onSuccess)` does both branches at once and yields a `UIO` (`E = Nothing`). (Use `foldZIO` if each branch is itself an effect.)
- **(d) Re-attempt a flaky network call 3 times → `retry`.** "Try again on failure" is a `Schedule`, not a fallback value: `call.retry(Schedule.recurs(3))`. The `E` survives if the retries exhaust.

The tell: *one error → fallback effect* is `catchAll`; *one error → alternative effect, reason ignored* is `orElse`; *both branches → one value* is `fold`; *same effect again* is `retry`.

</details>

> **Exercise 2 — Watch `E` collapse.** Starting from `ZIO[R, NotFound, User]`, write the type after `.catchAll(_ => ZIO.succeed(guest))`. Why is the new `E` `Nothing`, and what does that let the compiler guarantee?

<details>
<summary><strong>Answer</strong></summary>

The type becomes:

```scala
ZIO[R, Nothing, User]
```

Why `Nothing`? `catchAll` replaces the *entire* error channel with the error type of the effect you return from the handler. Here the handler is `_ => ZIO.succeed(guest)`, and `ZIO.succeed` can't fail, so its `E` is `Nothing`. Since `catchAll` caught the *only* possible error (`NotFound`) and the replacement effect introduces no new error, there is nothing left in `E` — so it collapses to `Nothing`, the type with no values.

What that *guarantees*: an `E` of `Nothing` means "this effect can never take the error track" — there is no value the failure channel could ever hold. The compiler now treats the effect as **total**: it always produces a `User`. This is the §1 superpower — recovery isn't just runtime behaviour, it's a compile-time *proof* you've handled everything. You can hand this effect to any context that demands an infallible effect (a `UIO`), and the compiler will accept it precisely because `Nothing` certifies it cannot fail.

</details>

> **Exercise 3 — Short-circuit.** In the interactive widget, why is the `map(renderProfile)` step skipped? Relate it to the railway analogy and to what a `map` does on a failed effect.

<details>
<summary><strong>Answer</strong></summary>

The step before it, `ZIO.fail(NotFound(id))`, derails the effect onto the **error track**. `map` is a *success-track* operation: it transforms the `A` channel and does nothing to the `E` channel. So when `map(renderProfile)` meets an already-failed effect, there is no `user` value to apply `renderProfile` to — the function simply isn't called, and the failure passes straight through untouched. That "skip the rest of the success steps once you've failed" behaviour is **short-circuiting**.

In the railway picture: once the switch throws and the train is rolling along the bottom (error) track, it *coasts past every remaining success station* — there's no point rendering a profile page for a user you never found. It keeps coasting until it reaches a recovery station (`catchAll`), which switches it back onto the success track. You can see the same thing in the Build It code: `map`'s `Err(e)` branch prints `"map: SKIPPED (on error track)"` and just returns `Err(e)` unchanged. A `map` on a failed effect is a no-op that forwards the failure — which is exactly why pipelines don't run success logic on bad data.

</details>

```quiz
{
  "prompt": "After you `.catchAll(...)` every possible error of a `ZIO[R, NotFound, A]` into a fallback, what is the new error type — and what does it mean?",
  "input": "Choose one:",
  "options": [
    "Nothing — the compiler now guarantees the effect cannot fail",
    "NotFound — the error type never changes",
    "Throwable — all handled errors become Throwable",
    "Any — errors become untyped"
  ],
  "answer": "Nothing — the compiler now guarantees the effect cannot fail"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[ZIO docs — Error recovery](https://zio.dev/reference/error-management/recovering/)** — `catchAll`, `catchSome`, `orElse`, `fold`, `foldZIO`, and friends.
- **[ZIO docs — Retrying](https://zio.dev/reference/error-management/retrying/)** and **[Schedule](https://zio.dev/reference/schedule/)** — turning "try again" into composable values.
- **[Scott Wlaschin — Railway Oriented Programming](https://fsharpforfunandprofit.com/rop/)** — the canonical explanation of the two-track model used above (F#, but the idea is universal).

---

**Next:** we've mastered the `E` and `A` channels. Now the third — **R** — the dependencies an effect needs. Meet `ZLayer`, ZIO's answer to dependency injection, starting with what the `R` channel really is. → [6. The R channel: dependencies you don't have yet](/cortex/production-engineering/zio/zlayer/the-r-channel)
