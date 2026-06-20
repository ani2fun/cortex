---
title: '11. Blocking vs async: attemptBlocking'
summary: ZIO runs fibers on a small CPU pool. A blocking call — JDBC, a file read, Thread.sleep — holds a thread hostage and starves the scheduler. `attemptBlocking` moves it to a dedicated pool. Cortex wraps every database call.
---

# 11. Blocking vs async: attemptBlocking

## TL;DR

> ZIO schedules fibers over a **small CPU pool** (about one thread per core), which is perfect for non-blocking work. But a **blocking** operation — a JDBC query, a file read, `Thread.sleep` — *holds its OS thread hostage* until it returns. Do that on the CPU pool and you starve the scheduler: a few blocking calls and *no other fibers can run*. **`ZIO.attemptBlocking`** runs the effect on a separate, **unbounded blocking pool** instead, keeping the CPU pool free. Cortex wraps every JDBC, Redis, and Mongo call in `attemptBlocking` for exactly this reason.

## 1. Motivation

Fibers (Chapter 10) get their power from a key assumption: a fiber that *waits* gets *parked*, freeing its thread for other fibers. That works for *asynchronous* I/O, where "waiting" means "registered a callback and yielded." But a huge amount of real-world code is **blocking**: the JDBC driver, most file APIs, `Thread.sleep`, many Java libraries. A blocking call doesn't yield — it *occupies* its OS thread, doing nothing, until the operation completes.

Now the danger. ZIO's CPU pool has maybe 8 threads (one per core). If 8 fibers each make a blocking JDBC call, all 8 threads are stuck waiting on the database — and *every other fiber in the system is frozen*, including the ones that would handle new requests, refresh tokens, or respond to a health check. Your server appears hung while the CPUs sit idle. This is one of the most common and most baffling ZIO (and async-runtime) production incidents: "the app froze but CPU is 0%."

The fix is to never run blocking work on the CPU pool. `attemptBlocking` shifts it to a *separate*, growable pool dedicated to blocking calls, so the CPU pool stays free to schedule everyone else. Knowing which of your calls block — and wrapping them — is essential, not optional, in a ZIO server.

## 2. Intuition (Analogy)

A small team of **expediters** at a restaurant pass (the CPU pool) whose whole job is to keep orders *moving* — glance at a ticket, route it, move on, never lingering. The system works because no expediter ever *stops*.

Now one expediter is told to *personally wait by the oven* for a 20-minute roast (a blocking call). They're now useless for 20 minutes — standing there, not routing anything. Do that to all four expediters and the pass *jams*: tickets pile up, nothing moves, even though the kitchen has capacity. The fix isn't more expediters; it's a *separate* runner whose job is to wait by ovens (the blocking pool), leaving the expediters free to keep the line flowing.

`attemptBlocking` is "send this to the oven-waiting runner." The expediters (CPU pool) never block; the runners (blocking pool) absorb the waiting.

## 3. Formal Definition

- **CPU (default) pool.** A small, fixed thread pool (~`#cores`) where ZIO runs fibers' non-blocking work and CPU computation. Optimized for throughput of work that *yields* — never block here.
- **Blocking pool.** A separate, *unbounded* (grows as needed) thread pool for operations that hold a thread. Blocking calls here don't affect the CPU pool's ability to schedule other fibers.
- **`ZIO.attemptBlocking(expr)`** — run side-effecting, *blocking* `expr` on the blocking pool; failures become `E = Throwable`. (`ZIO.attemptBlockingIO` for `IOException`-typed; `ZIO.blocking(effect)` shifts an existing effect to the blocking pool.)
- **`attemptBlockingInterrupt`** — for blocking calls that can be cancelled via thread interruption; lets ZIO interrupt the blocking operation (with caveats — not all blocking APIs honor interruption).
- **What blocks?** JDBC, most `java.io`/`java.net` synchronous APIs, `Thread.sleep`, `synchronized` waits, many older Java SDKs. **What doesn't?** ZIO's own combinators, `ZIO.sleep`, properly async clients (some Redis/HTTP drivers).

> The rule: **CPU pool for computation and async I/O; blocking pool for anything that holds a thread.** `attemptBlocking` is how you put a blocking call where it belongs. Get this wrong and a healthy-looking server silently freezes.

## 4. Worked Example — Cortex wraps its JDBC

Every database call in Cortex runs through `attemptBlocking`. From the `HelloPipeline` visit counter:

```scala
// JDBC is BLOCKING — getConnection, executeQuery all hold the thread.
// attemptBlocking runs this on the blocking pool, NOT the CPU pool.
override def incrementAndGet: Task[Long] = ZIO.attemptBlocking {
  val conn = ds.getConnection                 // blocks
  try
    conn.setAutoCommit(false)
    val stmt = conn.prepareStatement(
      "UPDATE visits SET count = count + 1 WHERE id = 1 RETURNING count"
    )
    val rs = stmt.executeQuery()              // blocks
    rs.next(); rs.getLong("count")
  finally conn.close()                        // blocks
}
```

Three blocking operations — `getConnection`, `executeQuery`, `close` — all safely on the blocking pool because the whole block is wrapped in `ZIO.attemptBlocking`. Now picture the alternative: if this ran on the CPU pool, a burst of concurrent requests would park every CPU thread on the database, and Cortex would stop serving *anything* — health checks, static files, other endpoints — until the queries returned. By shifting blocking work aside, the CPU pool keeps scheduling the thousands of *other* fibers (request routing, token verification, the HTTP server loop). The same pattern wraps Cortex's Redis (Lettuce) and Mongo blocking calls, and even the HikariCP pool *construction* in `DataSource.live` (Chapter 9) — building a pool blocks too.

This is also why Cortex's pool is sized deliberately (`setMaximumPoolSize(10)`): the blocking pool can grow, but the *database* can only handle so many concurrent connections. The pool bounds DB concurrency; `attemptBlocking` keeps that bounded blocking off the CPU pool.

## 5. Build It

Run this. It demonstrates the starvation: blocking work on a tiny shared pool jams everything; moving it to a separate pool keeps the fast lane flowing.

```scala run
@main def run(): Unit =
  // A model of two thread pools. We compute the wait analytically (virtual time)
  // instead of really sleeping, so the printed timings are deterministic.
  val cpuWorkers  = 2   // the small, fixed CPU pool (sized to cores)
  val nBlocks     = 4
  val blockEachMs = 200 // each blocking call holds its thread this long (like JDBC)

  // Time to drain `count` blocking calls through `workers` threads (virtual ms):
  def drainMs(count: Int, eachMs: Int, workers: Int): Int =
    math.ceil(count.toDouble / workers).toInt * eachMs

  // ❌ Put BLOCKING work on the CPU pool — it jams the quick tasks behind it.
  println("blocking on the CPU pool (2 threads):")
  val jam = drainMs(nBlocks, blockEachMs, cpuWorkers) // 4 blocks / 2 threads = 2 rounds = 400ms
  println(f"   quick tasks waited behind blocking work — ${jam / 1000.0}%.2fs\n")

  // ✓ Send BLOCKING work to the blocking pool (8 threads) — the CPU pool stays free.
  println("blocking on the blocking pool (8 threads), CPU pool free:")
  // 4 blocks absorbed off the CPU pool; the fast lane never blocks, so quick tasks run now:
  val quickWaitMs = 0
  println(f"   quick tasks finished immediately (${quickWaitMs / 1000.0}%.3fs)")
```

**Now break it.** In the first (bad) case, raise `cpuWorkers` to absorb the blocking — you've just "fixed" it by throwing threads at the CPU pool, which is exactly the wrong instinct: in a real server the CPU pool is sized to cores for good reason, and blocking calls are unbounded. The right fix is structural: keep blocking work *off* the CPU pool entirely. That's `attemptBlocking` in one line versus a fragile pool-sizing guess.

## 6. Trade-offs & Complexity

| `attemptBlocking` (separate pool) | Blocking on the CPU pool |
|---|---|
| CPU pool stays free; server stays responsive | A few blocks freeze everything |
| Blocking pool grows to absorb waits | CPU pool is fixed/small |
| You must *know* which calls block | "It just froze and CPU is 0%" |
| One wrapper per blocking call | No wrapper, hidden landmine |

The cost is *knowledge and discipline*: you have to recognize blocking APIs (JDBC, file I/O, legacy SDKs) and wrap them. There's no compiler check for "this blocks" — it's a property of the underlying library. The payoff is a server that doesn't mysteriously hang under load. When in doubt, if a call talks to a database, disk, or a synchronous Java client, wrap it.

## 7. Edge Cases & Failure Modes

- **The silent freeze.** The signature symptom of blocking-on-CPU-pool: the app stops responding but CPU usage is near zero (threads parked, not computing). If you see that, hunt for unwrapped blocking calls.
- **Unbounded blocking pool meets a bounded resource.** The blocking pool can spawn many threads, but your database accepts limited connections. Bound the real resource (HikariCP `maximumPoolSize`), not just the pool.
- **Wrapping non-blocking work.** `attemptBlocking` around already-async code just wastes a thread shuffle. Wrap *blocking* calls, not everything.
- **Uninterruptible blocking.** Many blocking calls ignore thread interruption, so a `timeout` (Chapter 12) may not actually stop them — it stops *waiting* for them, but the thread stays busy. Use `attemptBlockingInterrupt` where the API supports it, and design timeouts accordingly.

## 8. Practice

> **Exercise 1 — Blocks or not?** Classify: (a) a JDBC `executeQuery`; (b) `ZIO.sleep(1.second)`; (c) `Thread.sleep(1000)`; (d) reading a file with `java.nio` synchronously; (e) a ZIO `map`.

<details>
<summary><strong>Answer</strong></summary>

- **(a) JDBC `executeQuery` — BLOCKS.** The driver holds the OS thread until the database answers. Wrap in `attemptBlocking`.
- **(b) `ZIO.sleep(1.second)` — does NOT block.** It's ZIO's own asynchronous timer: the fiber *parks* and the thread is freed; nothing is held. Safe on the CPU pool.
- **(c) `Thread.sleep(1000)` — BLOCKS.** It occupies the actual OS thread for a full second, doing nothing. The classic landmine — use `ZIO.sleep` instead, or wrap it.
- **(d) synchronous `java.nio` file read — BLOCKS.** Most synchronous `java.io`/`java.nio` calls hold the thread. Wrap in `attemptBlocking`.
- **(e) a ZIO `map` — does NOT block.** Pure CPU transformation of a value; it belongs on the CPU pool.

The dividing line is whether the call *yields* while it waits (b, e) or *occupies* a thread until it returns (a, c, d). There's no compiler check for "this blocks" — it's a property of the underlying library, so you have to recognize it.

</details>

> **Exercise 2 — Diagnose.** A teammate's ZIO service "hangs under load, but the CPU graph is flat." Give your first hypothesis and how you'd confirm it.

<details>
<summary><strong>Answer</strong></summary>

First hypothesis: **a blocking call running on the CPU pool is starving the scheduler.** Flat CPU + unresponsive app is the signature symptom — the small CPU pool (~one thread per core) has every thread *parked* on a blocking operation (a JDBC query, `Thread.sleep`, a synchronous SDK), so no other fiber — request routing, health checks, anything — can be scheduled. The CPUs sit idle because the threads are waiting, not computing.

Confirm it with a **thread dump** under load: you'll see the CPU-pool threads stuck inside a blocking native call (e.g. a socket read in the JDBC driver) rather than in ZIO's scheduler. Then hunt for the unwrapped blocking call and put it on the blocking pool with `attemptBlocking`. (If raising the CPU-pool size "fixes" it, that's confirmation of the diagnosis, *not* the right fix — the structural fix is keeping blocking work off that pool.)

</details>

> **Exercise 3 — Two bounds.** Cortex sets `maximumPoolSize(10)` on HikariCP *and* runs queries via `attemptBlocking`. Explain what each bound protects, and why you need both.

<details>
<summary><strong>Answer</strong></summary>

They protect two *different* resources, so neither replaces the other:

- **`attemptBlocking`** protects the **CPU pool / the scheduler**. It shifts the thread-holding JDBC work onto the separate, growable blocking pool, so blocking calls never freeze the small CPU pool that schedules every other fiber. Without it, a burst of queries jams the whole server.
- **`maximumPoolSize(10)`** protects the **database**. Because the blocking pool is *unbounded*, it would happily spawn hundreds of threads all opening connections — but the database only accepts so many. HikariCP caps concurrent connections at 10, queuing the rest, so a load spike can't overwhelm Postgres.

You need both because the unbounded blocking pool that keeps the CPU pool safe is exactly what *endangers* the bounded database. `attemptBlocking` keeps blocking off the CPU pool; the connection-pool size bounds how much of that blocking hits the real, limited resource. Move the waiting aside, *and* bound the scarce thing it waits on.

</details>

```quiz
{
  "prompt": "Why must a blocking JDBC call run on ZIO's blocking pool (via `attemptBlocking`) rather than the default CPU pool?",
  "input": "Choose one:",
  "options": [
    "A blocking call holds its thread; on the small CPU pool a few of them starve the scheduler and freeze all other fibers",
    "The blocking pool is faster at SQL",
    "JDBC only works on the blocking pool",
    "It makes queries run in parallel automatically"
  ],
  "answer": "A blocking call holds its thread; on the small CPU pool a few of them starve the scheduler and freeze all other fibers"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[ZIO docs — Blocking operations](https://zio.dev/reference/concurrency/#blocking)** — `attemptBlocking`, `blocking`, `attemptBlockingInterrupt`, and the two pools.
- **[ZIO docs — Default services & runtime threads](https://zio.dev/reference/runtime/)** — how the CPU and blocking pools are configured.
- **[Cortex `HelloPipeline.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/helloPipeline/HelloPipeline.scala)** & **[`DataSource.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/DataSource.scala)** — every blocking JDBC/pool call wrapped in `attemptBlocking`.

---

**Next:** with fibers and the right pools, we can compose concurrency declaratively — run things in parallel, race them, and bound them with timeouts. → [12. Racing, timeouts & parallelism](/cortex/production-engineering/zio/concurrency/racing-timeouts-parallelism)
