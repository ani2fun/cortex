---
title: Concurrency — Threads, Processes & the GIL
summary: Threads share memory but the GIL lets only one run Python bytecode at a time, so threads speed up I/O-bound work, not CPU-bound; processes get true parallelism at the cost of shared memory. Threads with concurrent.futures, the GIL, locking shared state, and when to use processes.
prereqs: []
---

# Concurrency: Threads, Processes & the GIL

"Make it faster by doing things at once" is more subtle in Python than in most languages, because of one design decision. The thesis: **CPython has a Global Interpreter Lock (GIL) that lets only one thread execute Python bytecode at a time** — so threads give you concurrency for *waiting* (I/O-bound work overlaps), but **not** parallelism for *computing* (CPU-bound work doesn't speed up); for that you need separate *processes*, which sidestep the GIL but can't share memory directly. Get this and you'll pick the right tool every time.

This builds on [functions](/cortex/languages/python/how-python-works/functions-in-depth) and sets up [Async Python](/cortex/languages/python/advanced/async-python). Every runnable output below was produced by running the code; timing figures are marked illustrative (they vary), and the multiprocessing example is static because the sandbox can't spawn processes.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the interpreter is *actually doing*; (2) a **concrete bite** — a specific, runnable way the naive assumption fails; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of Contents

1. [Threads with `concurrent.futures`](#1-threads-with-concurrentfutures)
2. [The GIL: threads don't speed up CPU work](#2-the-gil-threads-dont-speed-up-cpu-work)
3. [Shared state needs a lock](#3-shared-state-needs-a-lock)
4. [Processes for CPU-bound parallelism](#4-processes-for-cpu-bound-parallelism)
5. [Choosing the right tool](#5-choosing-the-right-tool)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Threads with `concurrent.futures`

The modern way to run work concurrently is `ThreadPoolExecutor`. Its `.map` runs a function over many inputs using a pool of threads — ideal when each call spends time *waiting* (network, disk).

```python run
from concurrent.futures import ThreadPoolExecutor
import time

def fetch(url):
    time.sleep(0.1)          # pretend this is a slow network call
    return f"got {url}"

urls = ["a", "b", "c"]
with ThreadPoolExecutor(max_workers=3) as pool:
    results = list(pool.map(fetch, urls))
print(results)
```

**Output:**
```
['got a', 'got b', 'got c']
```

**Analysis.** Three `fetch` calls each "wait" 0.1s. Run sequentially they'd take ~0.3s; with three threads they overlap and finish in ~0.1s. `pool.map` preserves input order in the results. This is the sweet spot for threads: work that's mostly *waiting*, where overlapping the waits is a real win.

**Intuition.**
*Mechanism.* A `ThreadPoolExecutor` runs callables on worker threads that share the process's memory. While one thread is blocked on I/O (like `sleep` or a socket read), Python hands the GIL to another — so the *waits* overlap even though only one thread runs Python code at any instant.

*Concrete bite.* The catch is what overlaps: threads help only when the work *releases the GIL* (I/O, `sleep`). For pure computation they don't (§2). Many people add threads to a CPU-heavy loop expecting a speedup and get none — the bite is in the next section, where the timing makes it undeniable.

*Earned rule.* Reach for `ThreadPoolExecutor` to overlap **I/O-bound** work (HTTP requests, file/db reads) — the win scales with how much time is spent waiting. The cost: threads share memory, so any *shared mutable state* needs locking (§3), and they do nothing for CPU-bound work.

---

## 2. The GIL: threads don't speed up CPU work

The Global Interpreter Lock means exactly one thread executes Python bytecode at a time. So splitting a *computation* across threads doesn't make it finish faster — the threads take turns, they don't run in parallel.

```python run
import time
from concurrent.futures import ThreadPoolExecutor

def burn(n):
    s = 0
    for i in range(n):
        s += i
    return s

N = 5_000_000
t = time.perf_counter()
burn(N); burn(N)
seq = time.perf_counter() - t

t = time.perf_counter()
with ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(burn, [N, N]))
par = time.perf_counter() - t

print(f"sequential={seq:.3f}s  2-threads={par:.3f}s")
```

**Output (illustrative — exact times vary, but the two are roughly equal):**
```
sequential=0.246s  2-threads=0.219s
```

**Analysis.** Two CPU-bound `burn` calls take about the same wall-clock time whether run one after another or on two threads (~0.25s either way). If the threads ran in parallel, two-at-once would be ~half the sequential time — but it isn't, because the GIL serialises the bytecode. The threads interleave; they don't co-execute.

**Intuition.**
*Mechanism.* The GIL is a single lock every thread must hold to run Python bytecode. CPU-bound code holds it continuously (releasing only briefly every so often), so a second compute thread mostly *waits* for the lock. Net throughput is roughly one core's worth, no matter how many threads.

*Concrete bite.* The numbers are the bite: `2-threads` is *not* meaningfully faster than `sequential` — nowhere near the ~2× a real parallel speedup would give. Adding threads to CPU-bound work buys nothing (and can be slightly slower from switching overhead). The intuitive "more threads = faster" is simply false for computation in CPython.

*Earned rule.* Don't use threads to speed up CPU-bound work — use **processes** (§4). Threads are for overlapping waits, not for parallel computing. The cost/boundary: this is a *CPython* property (the GIL); other implementations differ, and recent CPython has experimental free-threaded builds — but for the Python you'll deploy today, treat CPU parallelism as a job for processes.

---

## 3. Shared state needs a lock

Threads share memory, which is convenient and dangerous: when two threads update the same variable, the updates can interleave and corrupt it. A `Lock` serialises access to shared state.

```python run
import threading

counter = 0
lock = threading.Lock()

def work():
    global counter
    for _ in range(100000):
        with lock:
            counter += 1

threads = [threading.Thread(target=work) for _ in range(4)]
for t in threads:
    t.start()
for t in threads:
    t.join()
print(counter)
```

**Output:**
```
400000
```

**Analysis.** Four threads each increment 100,000 times; with the `Lock`, the total is exactly `400000`. The `with lock:` block ensures only one thread performs the read-modify-write at a time, so no updates are lost. Remove the lock and you're gambling — the result is *sometimes* right and sometimes short, depending on thread timing.

**Intuition.**
*Mechanism.* `counter += 1` is not atomic — it compiles to *several* bytecodes. Disassembling it shows the read-modify-write:

```python run
import dis
def inc(c):
    c += 1
    return c
dis.dis(inc)
```
```
  2           RESUME                   0

  3           LOAD_FAST                0 (c)
              LOAD_CONST               1 (1)
              BINARY_OP               13 (+=)
              STORE_FAST               0 (c)

  4           LOAD_FAST                0 (c)
              RETURN_VALUE
```

*Concrete bite.* `c += 1` is `LOAD_FAST` (read) → `BINARY_OP` (add) → `STORE_FAST` (write) — three steps. If a thread switch happens *between* the read and the write, two threads read the same value, both add one, and both write back the same result: **one increment is lost**. The GIL guarantees each *bytecode* is atomic, but not a *sequence* of them — so on CPython this race often stays hidden (the simple counter above usually comes out right *without* a lock, by luck of timing), which is exactly what makes it a lurking bug that finally bites under load or on a more complex update.

*Earned rule.* Protect every shared mutable variable with a `Lock` (or avoid sharing — pass data via a `queue.Queue`, or return results from `pool.map`). Don't trust the GIL to make your updates safe: it serialises bytecodes, not your logic. The cost of a lock is contention (threads wait for it) and the risk of deadlock if you hold several — so keep critical sections small and lock ordering consistent.

---

## 4. Processes for CPU-bound parallelism

For real CPU parallelism you need separate **processes**, each with its own interpreter and its own GIL. `ProcessPoolExecutor` has the same API as `ThreadPoolExecutor`. (This sandbox can't spawn processes, so the example is shown statically — it runs on a normal machine.)

```python
from concurrent.futures import ProcessPoolExecutor

def burn(n):
    s = 0
    for i in range(n):
        s += i
    return s

if __name__ == "__main__":                     # required guard for process pools
    with ProcessPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(burn, [10_000_000] * 4))
    print(len(results))                        # 4 — computed on 4 cores in parallel
```

**Analysis.** Swapping `ThreadPoolExecutor` for `ProcessPoolExecutor` is a one-word change, but the effect is total: four CPU-bound `burn` calls run on four cores *simultaneously*, finishing in roughly the time of one (instead of four, as threads would). Each process has its own GIL, so there's no shared lock to serialise them. The `if __name__ == "__main__":` guard ([Tutorial 20](/cortex/languages/python/how-python-works/modules-and-packages)) is mandatory — process pools re-import your module in each worker.

**Intuition.**
*Mechanism.* `ProcessPoolExecutor` forks/spawns separate Python interpreters. They don't share memory, so there's no GIL contention between them — true parallelism. The cost is that arguments and results must be **pickled** (serialised) to cross the process boundary, and there's per-process startup overhead.

*Concrete bite.* The no-shared-memory rule is the trap: you can't simply mutate a shared object from worker processes — each gets its own *copy*, and changes don't propagate back. Code that "worked" with threads (mutating a shared list) silently does nothing with processes; you must *return* results (as `pool.map` does) or use explicit IPC (`multiprocessing.Queue`, shared memory). And unpicklable arguments (lambdas, open files) raise at submit time.

*Earned rule.* Use processes for **CPU-bound** parallelism — heavy computation that the GIL otherwise serialises; one worker per core is the usual sizing. The cost is real: pickling overhead, startup time, no shared memory, and the `__main__` guard — so processes pay off for *coarse* chunks of heavy work, not for many tiny tasks where the overhead dominates.

---

## 5. Choosing the right tool

The decision reduces to one question: is the work **I/O-bound** (waiting) or **CPU-bound** (computing)?

```python run
# A quick way to classify your own workload before choosing:
work = [
    ("download 100 URLs", "I/O-bound", "threads or async"),
    ("resize 100 images", "CPU-bound", "processes"),
    ("query a database", "I/O-bound", "threads or async"),
    ("train a model in pure Python", "CPU-bound", "processes"),
]
for task, kind, tool in work:
    print(f"{task:32} {kind:11} -> {tool}")
```

**Output:**
```
download 100 URLs                I/O-bound   -> threads or async
resize 100 images                CPU-bound   -> processes
query a database                 I/O-bound   -> threads or async
train a model in pure Python     CPU-bound   -> processes
```

**Analysis.** I/O-bound work (waiting on the network or disk) overlaps beautifully with threads — or with [async](/cortex/languages/python/advanced/async-python), which scales to far more concurrent waits per thread. CPU-bound work (pure computation) needs processes to use multiple cores. The GIL is why this split exists at all.

**Intuition.**
*Mechanism.* Threads and async overlap *waiting* (the GIL is released or yielded during I/O); processes overlap *computing* (each has its own GIL). Matching the tool to the bottleneck is the whole game — using the wrong one gives no speedup (threads on CPU work) or needless complexity (processes for a few HTTP calls).

*Concrete bite.* The classic mistake is reaching for `ProcessPoolExecutor` to "speed up" downloading many URLs. Processes there add pickling and startup cost for work that's just *waiting* — threads (or async) would overlap the waits with none of that overhead. Tool-to-workload mismatch makes concurrent code slower *and* more complex than the sequential version.

*Earned rule.* I/O-bound → threads (`ThreadPoolExecutor`) or async; CPU-bound → processes (`ProcessPoolExecutor`); neither → keep it sequential (concurrency adds bugs and overhead — only pay for it when there's a real bottleneck). The cost of every concurrency tool is added complexity (races, deadlocks, pickling, debugging), so reach for it only when measurement ([Performance & Profiling](/cortex/languages/python/advanced/performance-and-profiling)) shows the bottleneck is real.

---

## 6. Mental-model summary

| Principle | Consequence |
|-----------|-------------|
| The GIL lets one thread run Python bytecode at a time | Threads overlap *waiting*, not *computing* |
| Threads speed up **I/O-bound** work | `ThreadPoolExecutor` for network/disk; no win for CPU work |
| `x += 1` is multiple bytecodes, not atomic | Shared mutable state needs a `Lock` (the GIL is not enough) |
| Processes each have their own GIL | True CPU parallelism, but no shared memory (pickling, `__main__` guard) |
| Match the tool to the bottleneck | I/O → threads/async; CPU → processes; neither → sequential |

## 7. Gotcha checklist

- **Threads didn't speed up my computation →** the GIL serialises CPU work; use `ProcessPoolExecutor`.
- **A shared counter/list is occasionally wrong →** a race; guard shared mutable state with a `Lock` (don't trust the GIL).
- **Process pool: changes to a shared object vanish →** processes don't share memory; return results or use IPC.
- **`ProcessPoolExecutor` errors at startup →** add the `if __name__ == "__main__":` guard; ensure args/results are picklable.
- **Concurrency made it slower →** wrong tool (processes for I/O) or overhead exceeds the gain on tiny tasks; measure first.

---

*Predict, then check.* Take the §1 `fetch` example and predict its result list (and roughly its wall time vs sequential). Then predict whether wrapping the §2 `burn` in *four* threads instead of two would approach a 4× speedup — and why not. Finally, reason about the §3 counter: with the `Lock` it's `400000`; without it, why is the result *unpredictable* rather than reliably wrong? That last question is the GIL's subtlety in one sentence.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
