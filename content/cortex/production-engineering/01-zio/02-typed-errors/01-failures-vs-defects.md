---
title: '4. Failures vs defects'
summary: ZIO splits "things that go wrong" into two kinds — expected failures you put in the E type and handle, and defects (bugs) you don''t. Knowing which is which is the heart of robust error handling.
---

# 4. Failures vs defects

## TL;DR

> Not all errors are equal. A **failure** is an *expected* problem you've modeled in the **E** channel — "user not found," "rate limit exceeded" — and intend to handle. A **defect** is an *unexpected* problem, a bug — a null pointer, an array index out of bounds, "the impossible happened" — that isn't in `E` and should crash loudly rather than be silently swallowed. ZIO tracks failures in the type (`ZIO.fail`) and defects outside it (`ZIO.die`), and `ZIO.attempt` turns thrown exceptions into typed failures. Cortex models its failures as `sealed trait`s like `AuthFailure`.

## 1. Motivation

Most languages have one bucket for "things that go wrong": exceptions. That conflates two completely different situations. "The user typed a bad password" is *expected* — it'll happen thousands of times a day and you have a planned response (return 401). "I dereferenced a null that my logic guarantees is never null" is a *bug* — it should never happen, and if it does, hiding it is dangerous.

Treating both the same leads to two classic disasters: catching `Exception` so broadly that you swallow real bugs (and ship corrupted state), or letting expected conditions blow up as uncaught exceptions (and crashing on bad input). ZIO fixes this by making you *decide, in the type,* which problems are part of your domain (failures, in `E`) and which are bugs (defects, outside `E`). That single distinction is most of what separates fragile error handling from robust error handling.

## 2. Intuition (Analogy)

A restaurant kitchen.

- **"We're out of the salmon."** Expected. It happens. The waiter has a planned response: "May I suggest the trout?" This is a **failure** — a known, recoverable condition that's part of running a restaurant. You *model* it and *handle* it.
- **"The kitchen is on fire."** Not part of the plan. You don't have a menu substitution for this. You evacuate and call the fire department — you *abort loudly*, you don't quietly serve the next table. This is a **defect** — something has gone wrong with the machinery itself.

A good restaurant handles "out of salmon" gracefully and treats "kitchen on fire" as the emergency it is. A bad one either panics over a missing dish (crashing on expected input) or keeps serving while the kitchen burns (swallowing real bugs). ZIO's `E` channel is the menu of conditions you've planned for; everything else is a fire.

## 3. Formal Definition

- **Failure (the `E` channel).** An expected, *typed* error value. Created with `ZIO.fail(e)`. It appears in the type as `ZIO[R, E, A]`, so the compiler forces callers to acknowledge it. Failures are *recoverable* (Chapter 5).
- **Defect.** An *untyped*, unexpected error — a bug. Created with `ZIO.die(throwable)`, or produced when code throws an exception that *isn't* captured into `E`. Defects are *not* in the type; they represent "this should never happen." By default they propagate up and crash the fiber.
- **`ZIO.attempt(expr)`.** Runs side-effecting `expr`; if it throws, the exception becomes a *failure* with `E = Throwable`. This is the bridge from the exception world into the typed-error world.
- **`Cause[E]`.** ZIO's full description of *why* an effect ended: a `Fail(e)` (a typed failure), a `Die(throwable)` (a defect), an `Interrupt` (a fiber was cancelled), or combinations. You rarely build it; you sometimes inspect it.

> The decision you make for every error: *is this part of my domain (model it in `E` and handle it) or a bug (let it be a defect and crash)?* Put expected conditions in `E`; let genuine "impossible" cases die. Don't pollute `E` with bugs, and don't bury bugs as if they were expected.

## 4. Worked Example — Cortex's typed failures

Cortex models each domain's expected failures as a **sealed trait** — a closed set of cases the compiler can check you've handled. For example, the auth domain's failure type:

```scala
// Cortex's AuthFailure — the COMPLETE menu of things auth can expectedly go wrong with.
sealed trait AuthFailure
object AuthFailure:
  case object MissingToken           extends AuthFailure  // no Authorization header
  case object Expired                extends AuthFailure  // exp in the past
  case object InvalidSignature       extends AuthFailure  // didn't verify against JWKS
  final case class Malformed(why: String) extends AuthFailure

// The verifier's type SAYS it can fail with exactly these:
def verify(token: String): IO[AuthFailure, VerifiedClaims] = ...
```

The payoff is in the type `IO[AuthFailure, VerifiedClaims]`. A caller *cannot ignore* that this can fail, and because `AuthFailure` is `sealed`, when they handle it the compiler checks they've covered every case — add a new failure case later and every handler stops compiling until updated. Meanwhile, a genuine bug inside `verify` (say, an unexpected NPE from a library) is *not* an `AuthFailure` — it's a defect, and it crashes loudly rather than being mistaken for "invalid token." Expected vs unexpected, kept rigorously apart.

Contrast a defect:

```scala
// This should be impossible if our invariants hold. If it happens, it's a BUG — die.
case _ => ZIO.die(new IllegalStateException("unreachable: claims without a subject"))
```

`ZIO.die` says "I am not modeling this as a normal outcome; if you see it, something is broken." It won't appear in `E`, and no caller will be asked to "handle" it — because the right response to a bug is to fix the bug, not to paper over it.

## 5. Build It

Run this. It models the two channels and shows why conflating them is dangerous — a broad catch that swallows a bug looks fine until it ships corrupted data.

```scala run
@main def run(): Unit =
  // EXPECTED failures are VALUES in a sealed type (the E channel) — recoverable.
  enum Failure:
    case NotFound(uid: String)

  val db = Map("ada" -> Some("Ada"), "broken" -> None)

  // Failures come back as Left (a value). A DEFECT (bug) is THROWN, not returned.
  def findUser(uid: String): Either[Failure, String] = db.get(uid) match
    case None             => Left(Failure.NotFound(uid))                            // expected failure
    case Some(None)       => throw RuntimeException("invariant violated: stored a None user") // DEFECT — a bug
    case Some(Some(user)) => Right(user)

  // GOOD handler: recover from the FAILURE value; let a DEFECT propagate (we don't catch it).
  def handle(uid: String): String = findUser(uid) match
    case Right(user) => s"200 $user"
    case Left(fail)  => s"404 $fail"      // expected: planned response

  println(handle("ada"))     // 200 Ada
  println(handle("ghost"))   // 404 NotFound(ghost) — expected failure handled gracefully

  // The dangerous anti-pattern: a broad catch that swallows the DEFECT too.
  def reckless(uid: String): String =
    try handle(uid)
    catch case _: Throwable => "404 (swallowed)"   // catches the bug — bad!

  println(reckless("broken"))  // 404 (swallowed) — a BUG silently mislabeled as a normal 404
```

**Now break it.** Notice `reckless("broken")` returns a tidy "404 (swallowed)" for what is actually a corrupted record — the bug is now invisible, and you'll ship it. Now call `handle("broken")` directly (it doesn't catch the bug): the defect — the thrown `RuntimeException` — propagates and crashes the program *loudly*, which is exactly what you want: a bug you can see and fix beats a bug silently swallowed. That's the whole argument for separating failures (recoverable values) from defects (crash-worthy bugs).

## 6. Trade-offs & Complexity

| Typed failures + defects (ZIO) | One exception bucket |
|---|---|
| Compiler enforces handling expected errors | Easy to forget a case |
| Bugs crash loudly, not silently swallowed | `catch (Exception)` hides bugs |
| `E` documents the real failure modes | Failure modes are invisible |
| You must *decide* failure vs defect | No decision, but no safety |

The cost is a small, recurring decision: for each error, *failure or defect?* That decision is the point — it forces you to distinguish "part of my domain" from "a bug," which is exactly the thinking that prevents both swallowed bugs and crashes on expected input. The compiler then holds you to it.

## 7. Edge Cases & Failure Modes

- **`catch (Exception)` / catch-all.** The cardinal sin: it swallows defects along with failures. In ZIO, prefer typed `E` handling (Chapter 5); reserve broad catches for the very edge, and even then inspect the `Cause` to tell failures from defects.
- **Modeling bugs as failures.** Putting "impossible" cases in `E` forces every caller to handle something that can't happen — noise that hides the real failure modes. Let bugs be defects.
- **Letting expected conditions throw.** The reverse: a "not found" thrown as an uncaught exception crashes on perfectly normal input. Model it in `E`.
- **Losing the cause.** When you do catch broadly, `Cause[E]` distinguishes `Fail` (failure), `Die` (defect), and `Interrupt`. Logging the full cause keeps you from mistaking one for another.

## 8. Practice

> **Exercise 1 — Failure or defect?** Classify each: (a) user submits an expired token; (b) a `match` hits a case you believed unreachable; (c) the payment API returns "insufficient funds"; (d) you divide by a denominator your code guarantees is non-zero, and it's zero.

<details>
<summary><strong>Answer</strong></summary>

The one question to ask each time (§3): *is this part of my domain — model it in `E` and handle it — or a bug I should let crash?*

- **(a) Expired token — failure.** Tokens expire constantly; it's a known, recoverable condition with a planned response (return 401). Model it in `E`, e.g. as `AuthFailure.Expired`.
- **(b) Unreachable `match` case hit — defect.** You *believed* it was unreachable, so reaching it means an invariant you were counting on is broken. That's the textbook `ZIO.die(new IllegalStateException("unreachable"))` — crash loudly so you can fix the bug, don't bury it in `E`.
- **(c) "Insufficient funds" — failure.** This is the payment domain working exactly as designed; it'll happen all day and you have a response (decline the order). It belongs in `E`.
- **(d) Division by a "guaranteed non-zero" denominator that's zero — defect.** Your code *guaranteed* it couldn't be zero, so a zero means the guarantee is violated — "the impossible happened." Let it die.

Notice the pattern: (a) and (c) are conditions you *planned for*; (b) and (d) are violated invariants. Planned-for ⇒ failure in `E`; violated invariant ⇒ defect that crashes.

</details>

> **Exercise 2 — Model it.** Write a `sealed trait PaymentFailure` with the expected failure cases for charging a card. Which "errors" did you deliberately *leave out* because they're bugs?

<details>
<summary><strong>Answer</strong></summary>

Model the closed menu of things that *expectedly* go wrong when charging a card — the conditions a caller has a planned response to — as a `sealed trait` (so the compiler can check every case is handled, §4):

```scala
sealed trait PaymentFailure
object PaymentFailure:
  case object InsufficientFunds            extends PaymentFailure  // decline, ask for another card
  case object CardDeclined                 extends PaymentFailure  // issuer said no
  case object CardExpired                  extends PaymentFailure  // expiry in the past
  final case class InvalidCard(why: String) extends PaymentFailure // failed Luhn / bad number
  case object GatewayUnavailable           extends PaymentFailure  // processor down — retry later
```

What you deliberately *leave out*, because they're **defects, not failures**:

- A null `Charge` object, an NPE, an index-out-of-bounds — bugs in your own code.
- "The processor returned a response shape our parser can't handle / a status we don't recognize" — a violated assumption about the integration. That's `ZIO.die`, not a `PaymentFailure` case; putting it in `E` would force every caller to "handle" something that means *your code is broken*.

The discipline from §3: don't pollute `E` with bugs (it forces handling of the impossible and hides the real failure modes), and don't bury bugs as if they were expected. `PaymentFailure` is the menu; everything else is a fire.

</details>

> **Exercise 3 — Catch the bug.** In the Build It example, explain precisely why `reckless` is dangerous and `handle` is safe, in terms of failures vs defects.

<details>
<summary><strong>Answer</strong></summary>

The Build It code has two kinds of "wrong": a **failure** that comes back as a *value* (`Left(NotFound)`) and a **defect** that is *thrown* (the `RuntimeException` for the corrupted `Some(None)` record).

- **`handle` is safe** because it only pattern-matches on the *value* channel — it recovers from `Left(fail)` with a planned 404 and lets the success case through. It never wraps the call in a `try/catch`, so when the *defect* is thrown it does **not** intercept it: the bug propagates and crashes loudly. `handle` recovers failures and leaves defects alone — exactly the §3 discipline.
- **`reckless` is dangerous** because its `catch case _: Throwable` is a catch-*all*: it cannot tell a failure from a defect. It swallows the thrown `RuntimeException` and relabels a *corrupted record* as a tidy `"404 (swallowed)"`. The bug is now invisible — no crash, no log, just a normal-looking 404 — so you ship corrupted state. That is the cardinal sin from §7: a broad catch that buries defects along with failures.

The lesson: recover from the *typed* failure (the `Left`/`E` channel); never let a catch-all stand between you and a defect, because a bug you can *see* and fix beats a bug silently mislabeled as expected.

</details>

```quiz
{
  "prompt": "What is the difference between a ZIO failure and a defect?",
  "input": "Choose one:",
  "options": [
    "A failure is an expected, typed error in the E channel you intend to handle; a defect is an unexpected bug, outside E, that should crash loudly",
    "Failures are slow; defects are fast",
    "Failures happen in tests; defects happen in production",
    "There is no difference — they're both exceptions"
  ],
  "answer": "A failure is an expected, typed error in the E channel you intend to handle; a defect is an unexpected bug, outside E, that should crash loudly"
}
```

## In the Wild

- **[ZIO docs — Error Management: Failures vs Defects](https://zio.dev/reference/error-management/sequential-and-parallel-errors/)** and **[Types of errors](https://zio.dev/reference/error-management/types/)** — the official treatment of `E`, defects, and `Cause`.
- **[ZIO docs — `Cause`](https://zio.dev/reference/error-management/cause/)** — the full anatomy of "why an effect ended."
- **[Cortex `auth` package](https://github.com/ani2fun/cortex/tree/main/server/src/main/scala/cortex/server/auth)** — real sealed-trait failure types in a production codebase.

---

**Next:** modeling failures is half the job; *recovering* from them is the other half. Meet `catchAll`, `orElse`, and `fold` — and watch an effect derail onto the error channel and get caught. → [5. Recovering: catchAll, orElse, fold](/cortex/production-engineering/zio/typed-errors/recovering-from-errors)
