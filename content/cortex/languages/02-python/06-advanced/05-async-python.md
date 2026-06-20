---
title: Async Python
summary: async/await is cooperative single-threaded concurrency — a coroutine yields control at each await, so one thread juggles thousands of waiting operations without threads or locks. Coroutines, asyncio.run, gather for concurrency, the event loop, and the blocking-call and missing-await traps.
prereqs: []
---

# Async Python — Cooperative Concurrency on One Thread

Async is the other answer to "do many things at once," and it's a different shape from threads. The thesis: **`async`/`await` is *cooperative, single-threaded* concurrency — a coroutine runs until it hits an `await`, where it voluntarily yields control back to the event loop, which runs another coroutine while the first waits.** One thread juggles thousands of in-flight I/O operations, with no GIL contention and no locks (because only one piece of code runs at a time). The price is that you must use it "all the way," and a single blocking call freezes everything.

This is the I/O-bound alternative to [threads](/cortex/languages/python/advanced/concurrency-and-the-gil), built on [generators/coroutines](/cortex/languages/python/how-python-works/iterators-and-generators). Every runnable output below was produced by running the code; timing figures are illustrative (they vary slightly).

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the interpreter is *actually doing*; (2) a **concrete bite** — a specific, runnable way the naive assumption fails; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of Contents

1. [Coroutines and `await`](#1-coroutines-and-await)
2. [Concurrency with `gather`](#2-concurrency-with-gather)
3. [Don't block the event loop](#3-dont-block-the-event-loop)
4. [`await` only works inside `async`](#4-await-only-works-inside-async)
5. [A concurrent pipeline](#5-a-concurrent-pipeline)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Coroutines and `await`

An `async def` defines a **coroutine**. Calling it doesn't run it — it returns a coroutine object. You run the top one with `asyncio.run`, and *inside* async code you `await` other coroutines.

```python run
import asyncio

async def greet(name):
    await asyncio.sleep(0.01)
    return f"Hello, {name}"

print(asyncio.run(greet("Ada")))
```

**Output:**
```
Hello, Ada
```

**Analysis.** `greet` is a coroutine; `asyncio.run(greet("Ada"))` starts the event loop, runs the coroutine to completion, and returns its result. The `await asyncio.sleep(0.01)` is a *non-blocking* pause — it yields control to the loop, which could run other coroutines during that hundredth of a second.

**Intuition.**
*Mechanism.* `async def` makes a coroutine function; calling it builds a coroutine object that does nothing until *driven* — by `asyncio.run` (at the top) or `await` (inside other coroutines). `await x` runs `x` and suspends the current coroutine until `x` is done, handing the loop a chance to run others meanwhile.

*Concrete bite.* The #1 async mistake: calling a coroutine without `await` or `run`, so the body never executes:

```python run
import asyncio

async def greet(name):
    return f"Hello, {name}"

result = greet("Ada")          # no await / no run - body does NOT execute
print(type(result).__name__)   # a coroutine object, not the string
```
```
coroutine
```

`greet("Ada")` returned a *coroutine object*, not `"Hello, Ada"` — the function body never ran. (Python also prints a `RuntimeWarning: coroutine 'greet' was never awaited` to stderr.) The result is useless until you `await` it or pass it to `asyncio.run`.

*Earned rule.* Always `await` a coroutine (or hand it to `asyncio.run`/`gather`); a bare call just builds an object. The cost of forgetting is silent no-ops and a `RuntimeWarning` — so when async code "does nothing," look for a missing `await` first.

---

## 2. Concurrency with `gather`

`await`ing coroutines one at a time runs them *sequentially*. To run them *concurrently* — overlapping their waits — pass them to `asyncio.gather`.

```python run
import asyncio, time

async def slow(x):
    await asyncio.sleep(0.2)
    return x

async def main():
    t = time.perf_counter()
    r = await asyncio.gather(slow(1), slow(2), slow(3))
    return r, round(time.perf_counter() - t, 1)

print(asyncio.run(main()))
```

**Output (illustrative timing):**
```
([1, 2, 3], 0.2)
```

**Analysis.** Three `slow` calls each wait 0.2s. `gather` runs them concurrently, so the *total* is ~0.2s, not 0.6s — the waits overlapped. `gather` returns results in the **order you passed them** (`[1, 2, 3]`), regardless of which finished first.

**Intuition.**
*Mechanism.* `gather` schedules all the coroutines on the loop at once. Each runs until its `await asyncio.sleep`, yields, and the loop moves to the next — so all three sleeps are "in flight" simultaneously on one thread. Total time ≈ the *longest* single wait, not the sum.

*Concrete bite.* Awaiting them one at a time instead is serial — the waits don't overlap:

```python run
import asyncio, time

async def slow(x):
    await asyncio.sleep(0.2)
    return x

async def main():
    t = time.perf_counter()
    a = await slow(1)     # await one...
    b = await slow(2)     # ...then the next: SEQUENTIAL
    return [a, b], round(time.perf_counter() - t, 1)

print(asyncio.run(main()))
```
```
([1, 2], 0.4)
```

Two 0.2s waits awaited in sequence take ~0.4s — each `await` finishes before the next starts. Writing `await` per call is the easy way to *accidentally* serialise concurrent work; `gather` is what makes it overlap.

*Earned rule.* Use `gather` (or `asyncio.as_completed`/task groups) to run independent coroutines concurrently; reserve sequential `await`s for steps that genuinely depend on each other. The cost: `gather` runs everything to completion and, by default, cancels the rest if one raises (or collects exceptions with `return_exceptions=True`) — so handle failures deliberately.

---

## 3. Don't block the event loop

The loop is **one thread**. A coroutine keeps control until it `await`s — so a *blocking* call (one that doesn't yield, like `time.sleep` or heavy computation) freezes every other coroutine.

```python run
import asyncio, time

async def bad(x):
    time.sleep(0.2)        # BLOCKING - freezes the event loop
    return x

async def main():
    t = time.perf_counter()
    await asyncio.gather(bad(1), bad(2), bad(3))
    return round(time.perf_counter() - t, 1)

print(asyncio.run(main()))
```

**Output (illustrative timing):**
```
0.6
```

**Analysis.** This is the §2 program with one change — `time.sleep` instead of `await asyncio.sleep` — and the time jumps from 0.2s to 0.6s. `time.sleep` is *blocking*: it doesn't yield to the loop, so each `bad` runs to completion before the next starts. `gather` couldn't overlap anything, because no coroutine ever gave up control.

**Intuition.**
*Mechanism.* Concurrency happens only at `await` points. A blocking call (`time.sleep`, a synchronous DB driver, a big CPU loop) holds the single thread without yielding, so the loop can't switch — every other coroutine stalls until it returns. Async buys nothing if your code never awaits.

*Concrete bite.* The output is the bite: `0.6` instead of `0.2`. Swapping in the blocking `time.sleep` silently destroyed the concurrency — the program is now as slow as sequential, despite the `gather`. A real-world version is calling a synchronous `requests.get` (instead of an async HTTP client) inside a coroutine: it looks async, runs serial.

*Earned rule.* Inside `async` code, use **async-native** calls (`await asyncio.sleep`, `aiohttp`, async DB drivers); for unavoidable blocking work, push it off the loop with `await asyncio.to_thread(fn, ...)` (or `loop.run_in_executor`). The cost of one stray blocking call is the loss of *all* concurrency — which is why async demands async libraries throughout.

---

## 4. `await` only works inside `async`

`await` is syntax that only the event loop understands, so it's only legal inside an `async def`. This is why async tends to "spread": to await something, your function must itself be `async`.

```python run
async def regular():
    pass

def bad():
    await regular()   # await outside an async function
```

**Output:**
```
  File "/w/main.py", line 5
    await regular()   # await outside an async function
    ^^^^^^^^^^^^^^^
SyntaxError: 'await' outside async function
```

**Analysis.** `bad` is a plain (`def`) function, so `await` inside it is a `SyntaxError` — caught at parse time, before anything runs. To call an async function and use its result, the caller must itself be `async` (or be the top-level `asyncio.run`).

**Intuition.**
*Mechanism.* `await` compiles to "suspend this coroutine and yield to the loop" — meaningless outside a coroutine, so Python rejects it syntactically. The consequence is **"async all the way"**: a sync function can't `await`, so making one function async tends to force its callers async too, up to the `asyncio.run` at the top.

*Concrete bite.* The `SyntaxError` above is the bite — you can't sprinkle `await` into ordinary code. Teams discover this when adding one async call deep in a sync codebase forces a cascade of `async def`s up the call chain (or an awkward `asyncio.run` in the middle, which has its own problems). Async is a property of the whole call stack, not a single function.

*Earned rule.* Decide async at the *boundary*: an async program has `asyncio.run` once at the top and `async`/`await` throughout the I/O path. The cost is that async is "colored" — it doesn't mix freely with sync code — so adopt it for I/O-heavy programs as a whole, not as a local tweak to one function.

---

## 5. A concurrent pipeline

Putting it together: fan out many independent I/O operations with `gather`, then combine the results — the canonical async pattern for "fetch a lot of things at once."

```python run
import asyncio

async def fetch_user(uid):
    await asyncio.sleep(0.1)        # simulate a DB/network call
    return {"id": uid, "name": f"user{uid}"}

async def main():
    users = await asyncio.gather(*(fetch_user(i) for i in range(3)))
    return [u["name"] for u in users]

print(asyncio.run(main()))
```

**Output:**
```
['user0', 'user1', 'user2']
```

**Analysis.** `gather(*(fetch_user(i) for i in range(3)))` unpacks a generator of coroutines into `gather`, running all three "fetches" concurrently (~0.1s total, not 0.3s), then we map the results. This scales: `range(1000)` would run a thousand concurrent fetches on one thread — where threads would need a thousand OS threads, async needs one. That density is async's superpower for I/O.

**Intuition.**
*Mechanism.* Each `fetch_user` is cheap until it `await`s, then yields; the loop keeps all of them in flight, resuming each as its wait completes. One thread, thousands of concurrent waits, no locks (only one coroutine runs at any instant, so shared state is safe without synchronisation — unlike [threads](/cortex/languages/python/advanced/concurrency-and-the-gil)).

*Concrete bite.* The pattern still hinges on actually driving the top coroutine — drop the `asyncio.run` and nothing happens (the whole pipeline is just an unrun coroutine object, the §1 trap at program scale). And remember every `fetch_user` must use async I/O; one blocking call inside it (§3) collapses the thousand-way concurrency back to serial.

*Earned rule.* Use async for **high-concurrency I/O** — many simultaneous network/DB calls — where its one-thread density beats threads; reach for threads when a library has no async version, and processes for CPU work. The cost is the "all the way" discipline (async libraries, one `asyncio.run`, no blocking calls) — worth it at scale, overkill for a handful of calls where threads are simpler.

---

## 6. Mental-model summary

| Principle | Consequence |
|-----------|-------------|
| `async def` makes a coroutine; it runs only when awaited/run | A bare call returns a coroutine object and does nothing |
| `await` suspends and yields to the loop | Sequential `await`s are serial; `gather` overlaps them |
| The loop is single-threaded | A blocking call (`time.sleep`, sync I/O) freezes all coroutines |
| `await` is illegal outside `async def` | Async is "colored" — it spreads up the call stack to `asyncio.run` |
| One thread handles thousands of concurrent waits | No locks needed (one coroutine runs at a time); ideal for I/O |

## 7. Gotcha checklist

- **Coroutine "didn't run" / `RuntimeWarning: never awaited` →** you called it without `await`/`asyncio.run`.
- **Async code is as slow as sequential →** you `await`ed serially (use `gather`) or made a blocking call (use async I/O / `asyncio.to_thread`).
- **`SyntaxError: 'await' outside async function` →** the enclosing function must be `async def`.
- **One sync call ruined concurrency →** `time.sleep`/`requests`/sync drivers block the loop; use async equivalents.
- **Async feels like it's spreading everywhere →** it is ("colored"); commit to it at the program boundary, with one `asyncio.run` at the top.

---

*Predict, then check.* Predict the total time of `gather(slow(1), slow(2))` versus `await slow(1); await slow(2)` when each `slow` waits 0.3s. Then predict what `greet("x")` returns *without* `await` (its type). Finally, predict what happens to the §3 timing if you change `time.sleep(0.2)` to `await asyncio.sleep(0.2)`. Those three predictions are the entire async model: awaited overlaps, unawaited does nothing, blocking ruins it.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
