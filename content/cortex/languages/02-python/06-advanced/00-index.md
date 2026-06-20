---
title: Advanced & Idiomatic
summary: Tier 5 — the tools and mental models that turn working Python into idiomatic, production Python. Type hints and mypy, the high-leverage standard library, the data model as one unified design, concurrency and the GIL, async, performance and profiling, and testing and packaging.
prereqs: []
---

# Advanced & Idiomatic Python

By now you can build real programs. Tier 5 is about building them *well* — typed, tested, fast where it matters, concurrent where it helps, and shipped so others can run them. The thesis of the tier: **idiomatic Python is mostly knowing which tool the language and standard library already give you** — a type checker, the right container, the data-model protocol, the correct concurrency model, a profiler, a test runner — instead of reinventing them.

Seven chapters, in order:

1. [**Type Hints & Static Typing**](/cortex/languages/python/advanced/type-hints) — annotations, `Protocol`, and `mypy`; documentation the tools can check (but the runtime ignores).
2. [**Standard-Library Tour**](/cortex/languages/python/advanced/standard-library-tour) — `collections`, `itertools`, `functools`: the high-leverage batteries.
3. [**The Data Model**](/cortex/languages/python/advanced/the-data-model) — every operator and built-in is a dunder; your objects plug into the language.
4. [**Concurrency: Threads, Processes & the GIL**](/cortex/languages/python/advanced/concurrency-and-the-gil) — why threads help I/O but not CPU, and when to use processes.
5. [**Async Python**](/cortex/languages/python/advanced/async-python) — cooperative single-threaded concurrency with `async`/`await`.
6. [**Performance, Profiling & Memory**](/cortex/languages/python/advanced/performance-and-profiling) — measure, don't guess; complexity dominates; `cProfile` and `__slots__`.
7. [**Testing, Debugging & Packaging**](/cortex/languages/python/advanced/testing-and-packaging) — `pytest`, logging, virtual environments, and shipping with `pyproject.toml`.

These draw on everything before — especially [the object model](/cortex/languages/python/how-python-works/the-object-model), [dunder methods](/cortex/languages/python/object-oriented/dunder-methods) (which [The Data Model](/cortex/languages/python/advanced/the-data-model) synthesizes), and [complexity](/cortex/languages/python/working-with-data/sequences). A note on the runnable blocks: a few topics here use tools the in-browser sandbox can't fully run (`mypy`, `pytest`, multiprocessing) — those are shown as clearly-labelled static examples, while everything testable in one Python file (threads, async, `__slots__`, `timeit`, the data model) is runnable and verified.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the interpreter is *actually doing*; (2) a **concrete bite** — a specific, runnable way the naive assumption fails; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

*This is the final tier. If you've read from [Tier 0](/cortex/languages/python/first-steps/what-is-python) to here, you've gone from "what is a program?" to typing, concurrency, and the data model — and, more importantly, you can now re-derive Python's behaviour from a handful of generative ideas rather than memorising it. That was the whole point.*
