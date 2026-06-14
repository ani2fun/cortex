---
title: '10. Fibers: green threads you can fork'
summary: A fiber is a lightweight thread ZIO schedules itself — fork an effect to run it concurrently, join to await its result. You can have millions, and interruption is safe and automatic.
---

# 10. Fibers: green threads you can fork

## TL;DR

> A **fiber** is ZIO's lightweight thread — a unit of concurrent work the *runtime* schedules onto a small pool of real OS threads. You **`fork`** an effect to start it running concurrently (getting a `Fiber` handle), and **`join`** to await its result. Fibers are cheap: you can have *millions* where you could afford only thousands of OS threads. And because the runtime owns them, ZIO can **interrupt** a fiber safely — cancelling its work and running its resource finalizers — which underpins races, timeouts, and graceful shutdown.

## 1. Motivation

Servers do many things at once: handle thousands of simultaneous requests, run a background refresh while serving, query two databases in parallel. The OS gives you *threads*, but real threads are expensive — each costs ~1MB of stack and a context-switch the kernel manages, so a few thousand is a practical ceiling. Block a thread waiting on I/O and it's a megabyte sitting idle. Worse, raw threads are hard to *cancel* safely: `Thread.stop` is deprecated and dangerous, so "stop this work, it's no longer needed" is a genuinely hard problem.

Fibers fix both. They're scheduled by ZIO's runtime *over* a small set of OS threads (typically one per CPU core), so a fiber waiting on I/O costs almost nothing — the runtime parks it and runs another. Millions of concurrent fibers are routine. And because the runtime controls scheduling, it can *interrupt* a fiber at safe points, unwinding its resources cleanly. Fibers are why a ZIO server can hold 100,000 idle connections on a handful of threads, and why "race these two and cancel the loser" is a one-liner.

## 2. Intuition (Analogy)

A restaurant kitchen with **four chefs** (OS threads) and a smart **head chef** (the ZIO runtime) juggling **hundreds of orders** (fibers).

The naive approach is "one cook per order" — hire a new cook every time a ticket comes in. You'd need hundreds of cooks (threads), most standing idle waiting for an oven, and the kitchen can't physically fit them. Instead, the four chefs work *all* the orders: when an order is waiting on the oven (I/O), the chef sets it aside and picks up another. The head chef tracks every order's state and assigns chefs to whatever's ready to progress. Hundreds of orders, four chefs, nobody idle.

| Kitchen | ZIO |
|---|---|
| An order ticket | A fiber |
| A chef | An OS thread |
| The head chef juggling tickets | The ZIO runtime scheduler |
| Setting an order aside while the oven works | A fiber parking on I/O (costs ~nothing) |
| "Cancel that order" | Interrupting a fiber (safe, runs cleanup) |
| Starting a new ticket | `fork` |
| Waiting for an order to be done | `join` |

## 3. Formal Definition

- **Fiber.** A lightweight, runtime-scheduled thread of execution. Cheap to create (no OS thread per fiber); a fiber blocked on I/O consumes essentially no resources. ZIO multiplexes many fibers over few OS threads.
- **`effect.fork`** — start `effect` on a *new fiber*, running concurrently; returns `Fiber[E, A]` immediately (does not wait).
- **`fiber.join`** — semantically wait for the fiber and get its `A` (or fail with its `E`). `fiber.await` gives the full `Exit` (success/failure/interruption).
- **`fiber.interrupt`** — cancel the fiber: it stops at the next safe point and runs its finalizers (Chapter 9's releases). Returns when interruption completes.
- **Structured concurrency.** ZIO ties fiber lifetimes to scopes: a fiber forked within a scope is interrupted when that scope ends, so you don't leak "zombie" background fibers. `forkScoped`/`forkDaemon` tune this.
- **Fork-join, the safe default.** Rather than raw `fork`, prefer high-level combinators (`zipPar`, `foreachPar`, `race` — Chapter 12) that fork *and* manage interruption for you.

> A fiber is a thread you can afford by the million, that the runtime schedules and can *safely cancel*. `fork` starts one; `join` awaits it; `interrupt` cancels it with cleanup. Concurrency becomes cheap and structured.

## 4. Worked Example — fork two, join both

```scala
// Sequentially: ~ time(a) + time(b). Each waits for the previous.
val sequential: Task[(A, B)] =
  for
    a <- queryUsers       // takes 100ms
    b <- queryOrders      // takes 100ms
  yield (a, b)            // ~200ms total

// Concurrently with fibers: ~ max(time(a), time(b)). Both run at once.
val concurrent: Task[(A, B)] =
  for
    fiberA <- queryUsers.fork     // start on a new fiber, returns immediately
    fiberB <- queryOrders.fork    // start another
    a      <- fiberA.join         // await the first
    b      <- fiberB.join         // await the second
  yield (a, b)                    // ~100ms total — they overlapped

// In practice you'd write the safe high-level form (Chapter 12):
val parallel: Task[(A, B)] = queryUsers <&> queryOrders   // zipPar — fork+join+interrupt managed
```

`fork` turns "do this, then that" into "start both, then collect." Two independent 100ms queries finish in ~100ms instead of ~200ms because the fibers overlap — while one waits on the database, the runtime runs the other. This is everyday in a Cortex request that needs, say, a cache lookup *and* a DB read: fork both, join both, halve the latency. And if the request is cancelled (client disconnects), interrupting the request fiber interrupts the forked children too — no orphaned queries.

## 5. Build It

Run this. It models the win: cooperative "fibers" over a single worker, overlapping I/O waits so total time is the *max*, not the *sum*.

```scala run
@main def run(): Unit =
  // A model of cooperative "fibers" over a single worker. We don't really sleep —
  // we simulate the scheduler and track *virtual* time, so the result is
  // deterministic (real OS threads would interleave the prints unpredictably).
  final case class Task(name: String, ms: Int)

  def sequential(tasks: List[Task]): Int =
    var total = 0
    for t <- tasks do
      println(s"   ${t.name}: start")
      total += t.ms // wait FULLY before the next one starts
      println(s"   ${t.name}: done (${t.ms}ms)")
    total // the SUM of the waits

  def concurrent(tasks: List[Task]): Int =
    tasks.foreach(t => println(s"   ${t.name}: start"))         // fork ALL — they park on I/O together
    tasks.foreach(t => println(s"   ${t.name}: done (${t.ms}ms)")) // one worker shuttles between them; join
    tasks.map(_.ms).maxOption.getOrElse(0)                       // overlapped -> the MAX wait

  val work = List(Task("users", 100), Task("orders", 100))

  println("sequential:")
  val s = sequential(work)
  println(s"   total ~${s}ms (sum)\n")

  println("concurrent (forked):")
  val c = concurrent(work)
  println(s"   total ~${c}ms (max) — overlapped on ONE worker")
```

**Now break it.** Add a third query and fork all three; total time stays ~100ms (the max), not ~300ms — because all three park on I/O concurrently while a single worker shuttles between them. That's the fiber model: concurrency limited by your *real* work, not by how many threads you can afford. Now imagine 100,000 of these — OS threads would die; fibers shrug.

## 6. Trade-offs & Complexity

| Fibers (ZIO) | Raw OS threads |
|---|---|
| Millions, cheap, park on I/O for free | Thousands max, ~1MB each |
| Safe, structured interruption | `Thread.stop` is unsafe/deprecated |
| Runtime scheduling, work-stealing | Kernel scheduling, heavier switches |
| Lifetimes tied to scopes (no leaks) | Manual lifecycle, easy to leak |
| New mental model to learn | Familiar but limited |

The cost is conceptual: "concurrent but not parallel-by-default," interruption semantics, structured lifetimes. The payoff is concurrency that scales to the problem instead of the thread budget, plus *safe cancellation* — which is what makes timeouts and races (next chapter) trivial and correct. Most of the time you won't even touch `fork` directly; the high-level combinators give you fibers' power with guardrails.

## 7. Edge Cases & Failure Modes

- **Leaking forked fibers.** A bare `fork` whose handle you drop can outlive its purpose. Prefer structured combinators (`zipPar`, `foreachPar`, `race`) or `forkScoped`, which interrupt children when the scope ends.
- **Expecting `fork` to wait.** `fork` returns *immediately*; the work runs concurrently. If you need the result, `join`. Forgetting to join means you never observe success/failure.
- **Blocking inside a fiber on the CPU pool.** A fiber that does a *blocking* call (JDBC) on the small CPU pool starves the scheduler — that's the entire next chapter (`attemptBlocking`).
- **Interruption at the wrong time.** Most code is interruptible at safe points; a critical section that must not be cut can be marked `uninterruptible`. Overusing that, though, defeats cancellation — use sparingly.

## 8. Practice

> **Exercise 1 — Sum or max?** You fork three independent 50ms queries and join all three. Roughly how long does it take, and why isn't it 150ms?

<details>
<summary><strong>Answer</strong></summary>

About **~50ms**, not 150ms. `fork` starts each query *immediately* and returns at once, so all three are in flight at the same time; the three `join`s then just await results that are already arriving in parallel. Total time is therefore the **max** of the three (~50ms), not the **sum** (~150ms). The reason it overlaps for free is that these are I/O-bound: while each query waits on the database, the runtime *parks* its fiber and runs another over the same small OS-thread pool. Only if the work were CPU-bound and you had fewer cores than queries would you start trending back toward the sum.

</details>

> **Exercise 2 — Why so cheap?** Explain why a fiber blocked on I/O costs almost nothing, while an OS thread blocked on I/O costs ~1MB. Tie it to the kitchen analogy.

<details>
<summary><strong>Answer</strong></summary>

An OS thread blocked on I/O is *occupied*: the kernel reserves its full ~1MB stack and keeps the thread in existence, doing nothing, until the call returns — a megabyte sitting idle. A fiber that hits I/O doesn't hold a thread at all: the runtime **parks** it (saves its small state) and hands the underlying OS thread to another ready fiber. The fiber waiting on I/O is just a bit of bookkeeping, so you can have millions of them. In the kitchen analogy: a blocked OS thread is a chef who *stands at the oven* for the whole bake, useless to everyone else; a parked fiber is an order ticket *set aside* on the rail while the four chefs (OS threads) keep working other tickets. The thread (chef) is never the thing that waits — the fiber (ticket) is.

</details>

> **Exercise 3 — Cancel cleanly.** A request forks two queries, then the client disconnects and the request fiber is interrupted. What happens to the two child fibers, and why does Chapter 9's `acquireRelease` matter here?

<details>
<summary><strong>Answer</strong></summary>

Both child fibers are **interrupted** too. Under structured concurrency, the children's lifetimes are tied to the parent (request) fiber's scope, so when the parent is interrupted its scope ends and the forked children are cancelled with it — no orphaned, "zombie" queries left running for a client that already left. This is where Chapter 9's `acquireRelease` earns its keep: interruption stops each fiber *at a safe point and runs its finalizers*, so a query that had acquired a database connection releases it on the way out. Without that, interruption would leak the very resources the connections hold. Cheap concurrency (`fork`) is only safe because cancellation is **safe + structured**: stop the work, *and* clean up after it.

</details>

```quiz
{
  "prompt": "What is a fiber in ZIO?",
  "input": "Choose one:",
  "options": [
    "A lightweight, runtime-scheduled thread — cheap enough to have millions, multiplexed over a few OS threads, and safely interruptible",
    "A new OS thread created per task",
    "A network fiber-optic connection",
    "A type of error in the E channel"
  ],
  "answer": "A lightweight, runtime-scheduled thread — cheap enough to have millions, multiplexed over a few OS threads, and safely interruptible"
}
```

## In the Wild

- **[ZIO docs — Fibers](https://zio.dev/reference/fiber/)** — `fork`, `join`, `interrupt`, and structured concurrency.
- **[ZIO docs — Interruption](https://zio.dev/reference/interruption/)** — how safe cancellation works and why it matters.
- **[Project Loom / virtual threads](https://openjdk.org/jeps/444)** — the JVM's own take on the same idea; useful contrast for where fibers came from and where the platform is going.

---

**Next:** fibers are great for *async* work — but a JDBC query *blocks* a thread, and blocking the wrong pool starves the scheduler. Meet `attemptBlocking`, and why Cortex wraps every database call in it. → [11. Blocking vs async: attemptBlocking](/cortex/production-engineering/zio/concurrency/blocking-vs-async)
