---
title: '12. Racing, timeouts & parallelism'
summary: With fibers underneath, concurrency becomes declarative one-liners — run effects in parallel, race them and cancel the loser, or bound any effect with a timeout. Safe interruption makes them all correct.
---

# 12. Racing, timeouts & parallelism

## TL;DR

> Fibers (Chapter 10) let ZIO offer concurrency as *combinators* — short, safe, declarative. **`zipPar`/`foreachPar`** run effects in parallel and collect results. **`race`** runs two effects and takes whichever finishes first, **interrupting the loser**. **`timeout`** races an effect against a timer, cancelling it if it's too slow. Because interruption is automatic and safe (it runs finalizers), the loser of a race or a timed-out effect cleans up after itself. You rarely touch `fork`; these combinators give you its power with guardrails.

## 1. Motivation

Raw `fork`/`join` is the assembly language of concurrency — powerful, but easy to misuse (leak a fiber, forget to interrupt the work you no longer need). In practice you want *intentions*: "do these in parallel," "whichever responds first," "give up after 2 seconds." ZIO encodes each intention as a combinator that forks the right fibers *and* manages their interruption for you. The result reads like a description of *what* you want concurrently, not a manual choreography of threads.

These three — parallel, race, timeout — cover the vast majority of real concurrency needs, and they're correct *because* of interruption: the slow query you timed out is actually *cancelled* (not left running and leaking a connection); the redundant request you raced and lost is *stopped*. Concurrency you can reason about, in one line each.

## 2. Intuition (Analogy)

Three everyday situations:

- **Parallel (`zipPar`):** you send two friends to two shops for two items. You wait until *both* return, then you have everything. Time taken = the *slower* errand, not the sum.
- **Race (`race`):** you're hungry and order from two delivery apps at once. The first to arrive wins; you *cancel* the other order so you're not charged twice or buried in food. Time = the *faster* one; the loser is stopped.
- **Timeout (`timeout`):** you call a friend but hang up after 30 seconds of ringing — you race the call against an egg timer; whichever "finishes" first wins, and a too-slow call is abandoned.

The crucial detail in race and timeout is the *cancellation*: you don't just ignore the loser, you *stop* it. ZIO's safe interruption is what makes that real — the cancelled delivery is truly cancelled, finalizers and all.

## 3. Formal Definition

| Combinator | Meaning | Interruption |
|---|---|---|
| `a <&> b` (`a.zipPar(b)`) | Run both in parallel; yield `(A, B)` when both succeed | If *one fails*, the other is interrupted, and the failure is raised |
| `ZIO.foreachPar(xs)(f)` | Apply `f` to all `xs` in parallel; collect results | First failure interrupts the rest |
| `ZIO.collectAllPar(effects)` | Run all in parallel; collect their results | as above |
| `a.race(b)` | Take whichever *finishes first* (success or failure) | The loser is interrupted |
| `a.raceFirst(b)` / `raceEither` | Variants on what "first" and the result mean | loser interrupted |
| `a.timeout(d)` | Race `a` against a `d`-long timer → `Option[A]` (`None` if too slow) | `a` is interrupted on timeout |
| `a.timeoutFail(e)(d)` | Like `timeout`, but *fail* with `e` instead of `None` | `a` interrupted |
| `ZIO.foreachPar(xs)(f).withParallelism(n)` | Bound parallelism to `n` at a time | — |

> Parallel = wait for all (failure cancels the rest). Race = take the first, cancel the loser. Timeout = race against a clock, cancel if slow. All built on fibers + safe interruption, so "cancel" really means *cancel*.

## 4. Worked Example — all three

```scala
// PARALLEL: a request that needs both a cache check and a DB read.
val both: Task[(CacheHit, Rows)] =
  checkCache <&> queryDb              // ~ max(t_cache, t_db), not the sum

// RACE: hedge a slow primary against a replica; take whoever answers first.
val fastest: Task[Rows] =
  queryPrimary.race(queryReplica)    // loser is interrupted — no wasted work continues

// TIMEOUT: never let a query hang a request forever.
val bounded: Task[Option[Rows]] =
  queryDb.timeout(2.seconds)         // None if it took > 2s; the query is cancelled

// Combine them: parallel fan-out with per-call timeouts, bounded parallelism.
val report: Task[List[Result]] =
  ZIO.foreachPar(sources)(s => fetch(s).timeout(1.second).map(_.getOrElse(Result.empty)))
     .withParallelism(8)             // at most 8 in flight at once
```

Read `report` as a sentence: "fetch all `sources` in parallel, at most 8 at a time, giving each a 1-second timeout, substituting an empty result for any that time out." That single expression forks up to 8 fibers, bounds their concurrency, times each out (interrupting slow ones so they don't leak connections), and collects the results — choreography that would be dozens of fragile lines with raw threads. A Cortex handler that needs to enrich a response from several sources, without letting any one slow dependency stall the whole request, is exactly this shape.

The interruption guarantees are what make it safe: in `fastest`, the moment the replica answers, the primary query is *cancelled* (its connection released via Chapter 9 finalizers); in `bounded`, a query exceeding 2s is *stopped*, not abandoned-but-still-running.

## 5. Build It

Run this. It implements parallel, race-with-cancel, and timeout, so you can watch the loser actually get cancelled.

```scala run
@main def run(): Unit =
  // We model the scheduler over virtual time so the run is deterministic
  // (real async would make the `~ms` and the cancel ordering flaky).
  final case class Work(name: String, ms: Int, fail: Boolean = false)

  // What a piece of work yields once its time elapses (success or failure):
  def settle(w: Work): String =
    if w.fail then s"${w.name} FAILED" else s"${w.name}(${w.ms}ms)"

  // parallel: run all, keep all results; wall time is the MAX, not the sum.
  def parallel(ws: List[Work]): (List[String], Int) =
    (ws.map(settle), ws.map(_.ms).max)

  // race: the work with the smaller ms settles first; the loser is INTERRUPTED.
  def race(a: Work, b: Work): String =
    val (winner, loser) = if a.ms <= b.ms then (a, b) else (b, a)
    println(s"   ${loser.name}: CANCELLED (interrupted)") // the loser cleans up
    settle(winner)                                        // first to SETTLE — success or failure

  // timeout: if the work is slower than the limit, interrupt it and yield None.
  def timeout(w: Work, limitMs: Int): Option[String] =
    if w.ms <= limitMs then Some(settle(w))
    else
      println(s"   ${w.name}: CANCELLED (interrupted)")
      None

  val (both, ms) = parallel(List(Work("cache", 80), Work("db", 120))) // parallel
  println(s"parallel: $both ~${ms}ms (max, not sum)\n")

  val winner = race(Work("primary", 150), Work("replica", 60))        // race + cancel loser
  println(s"race winner: $winner\n")

  val slow = timeout(Work("slow-query", 500), 100)                    // timeout
  println(s"timeout result: $slow (None = it was cancelled)")
```

**Now break it.** In `race`, make the *winner* the one set to `fail = true` (e.g. `Work("primary", 60, fail = true)` racing `Work("replica", 150)`). The race surfaces the *first to finish* — even though it failed — and still cancels the slower one. That mirrors ZIO's `race`: "first to *settle*," success or failure, loser interrupted. If you instead want "first *success*, ignoring failures," that's a different combinator (`raceFirst`/custom) — a reminder to pick the racer that matches your intent.

## 6. Trade-offs & Complexity

| Concurrency combinators (ZIO) | Hand-rolled threads/futures |
|---|---|
| Declarative: state the intention | Imperative choreography |
| Interruption handled correctly | Manual cancellation, often skipped |
| Failure semantics defined (fail-fast) | Ad-hoc, easy to leak on error |
| `withParallelism` bounds fan-out | Manual semaphores |
| Must learn the combinator zoo | Familiar but error-prone |

The cost is learning *which* combinator matches your intent — `race` vs `raceFirst`, `timeout` vs `timeoutFail`, fail-fast `foreachPar` vs collecting all errors — and the interruption semantics underneath. The payoff is concurrency that's correct by construction: no leaked fibers, no zombie queries, no "I forgot to cancel the other request." For server code, that correctness is worth far more than the learning curve.

## 7. Edge Cases & Failure Modes

- **Unbounded `foreachPar`.** Fanning out over 10,000 items forks 10,000 fibers at once — possibly hammering a downstream service. Use `.withParallelism(n)` to cap in-flight work.
- **Timeout doesn't stop blocking work.** `timeout` interrupts at safe points; a *blocking* call (Chapter 11) that ignores interruption keeps running on the blocking pool even after the timeout returns `None`. Design timeouts knowing this.
- **Racing effects with side effects.** If both raced effects have observable side effects (two writes), the loser may have *partially* run before interruption. Make raced effects idempotent or side-effect-free where possible.
- **Fail-fast surprises.** `foreachPar` interrupts siblings on the first failure — usually what you want, but if you need *all* results (including failures), use `validatePar`/collect with `either`. Choose deliberately.

## 8. Practice

> **Exercise 1 — Pick the combinator.** For each: (a) fetch a user and their orders together; (b) query two replicas, take the faster; (c) never let an external call exceed 3s; (d) process 5,000 files, 10 at a time.

<details>
<summary><strong>Answer</strong></summary>

- **(a) fetch a user and their orders together → `user <&> orders` (`zipPar`).** Two independent reads you need *both* of; run in parallel, get `(User, Orders)` in ~max time. (If one fails, the other is interrupted.)
- **(b) two replicas, take the faster → `queryReplicaA.race(queryReplicaB)`.** First to finish wins; the loser is interrupted so no wasted query keeps running. (This is the hedged-request / "tail at scale" pattern.)
- **(c) never let an external call exceed 3s → `call.timeout(3.seconds)`** (or `timeoutFail` if you'd rather fail than get `None`). Races the call against a clock and interrupts it if it's too slow.
- **(d) process 5,000 files, 10 at a time → `ZIO.foreachPar(files)(process).withParallelism(10)`.** Parallel fan-out, but `withParallelism(10)` caps in-flight work at 10 so you don't fork 5,000 fibers at once and hammer the disk/downstream.

The skill is matching the *intention* to the combinator: "both" = `zipPar`, "first wins" = `race`, "cap the wait" = `timeout`, "bounded fan-out" = `foreachPar.withParallelism`.

</details>

> **Exercise 2 — Reason about cancellation.** In `queryPrimary.race(queryReplica)`, the replica wins. What happens to the primary's database connection, and which chapter's mechanism guarantees it's released?

<details>
<summary><strong>Answer</strong></summary>

The moment the replica settles, `race` **interrupts the loser** — so `queryPrimary`'s fiber is cancelled, and its database connection is **released**, not leaked. The guarantee comes from **Chapter 9's `acquireRelease`/resource finalizers**: interruption stops the fiber at a safe point and runs its finalizers, so the connection acquired for the primary query is returned to the pool on the way out. This is the whole reason `race` is *correct* and not just fast — "take the first" would be a connection leak per race if the loser kept running or dropped its connection without cleanup. Safe interruption (Chapter 10) plus resource finalizers (Chapter 9) are what make "cancel the loser" actually mean cancelled-and-cleaned-up.

</details>

> **Exercise 3 — Bound the blast.** You write `ZIO.foreachPar(userIds)(notify)` over a million ids and your push service falls over. What single change fixes it, and what's the trade-off in throughput?

<details>
<summary><strong>Answer</strong></summary>

Add **`.withParallelism(n)`** — e.g. `ZIO.foreachPar(userIds)(notify).withParallelism(50)`. Unbounded `foreachPar` forks a *million* fibers at once, firing a million simultaneous calls that flatten the push service; `withParallelism(n)` caps in-flight work at `n`, so at most `n` notifications run concurrently and the rest queue. The trade-off is **latency/throughput vs. safety**: a smaller `n` is gentler on the downstream but takes longer to drain all million (you've traded peak parallelism for a sustainable rate). You tune `n` to what the push service can actually absorb — fast enough to finish in reasonable time, slow enough that it stays up. (`fork`-everything is rarely what you want against a real downstream; bound the blast.)

</details>

```quiz
{
  "prompt": "What does `a.race(b)` do when `a` finishes first?",
  "input": "Choose one:",
  "options": [
    "Returns a's result and interrupts (cancels) b, running b's finalizers so it cleans up",
    "Waits for both a and b, then returns a",
    "Returns a but leaves b running in the background forever",
    "Runs a and b sequentially"
  ],
  "answer": "Returns a's result and interrupts (cancels) b, running b's finalizers so it cleans up"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[ZIO docs — Parallelism](https://zio.dev/reference/concurrency/)** — `zipPar`, `foreachPar`, `withParallelism`, and fail-fast semantics.
- **[ZIO docs — Racing & timeouts](https://zio.dev/reference/concurrency/#racing)** — `race`, `raceFirst`, `timeout`, `timeoutFail`.
- **[Jeff Dean & Luiz Barroso — "The Tail at Scale"](https://research.google/pubs/the-tail-at-scale/)** — why hedged requests (the `race` pattern) tame tail latency in real systems.

---

**Next:** the building blocks are complete. Time to assemble them into a real HTTP API the way Cortex does — with Tapir and zio-http. → [13. An HTTP API with Tapir + zio-http](/cortex/production-engineering/zio/cortex-in-practice/http-api-with-tapir)
