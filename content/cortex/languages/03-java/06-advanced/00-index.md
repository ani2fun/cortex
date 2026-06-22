---
title: Advanced & Idiomatic
summary: Tier 5 of the Java book — functional Java, concurrency and the memory model, I/O, modern idioms, and shipping. The Streams API, threads and the happens-before rule, high-level concurrency and virtual threads, the JIT and GC, NIO.2 files, how the modern features compose into data-oriented design, and testing/build/packaging. Every example compiled and run.
prereqs: []
---

# Advanced & Idiomatic

This is Tier 5, the summit. With the full language and the standard library behind you, these seven chapters cover what separates competent Java from idiomatic, production-grade Java: declarative data processing with streams, the hard truths of concurrency and the Java Memory Model, how the JVM actually runs and reclaims your code, modern file I/O, the way the modern type-system features compose into one coherent style, and the testing, tooling, and packaging that ship it.

Seven chapters, in order:

1. [**Functional Java & the Streams API**](/cortex/languages/java/advanced/functional-java-and-streams) — lazy pipelines, collectors, `Optional`, and the parallel-stream hazard.
2. [**Concurrency: the Basics**](/cortex/languages/java/advanced/concurrency-the-basics) — threads, race conditions, `synchronized`, and happens-before.
3. [**Concurrency: High-Level & Virtual Threads**](/cortex/languages/java/advanced/concurrency-high-level-and-virtual-threads) — executors, atomics, `CompletableFuture`, and virtual threads.
4. [**The Java Memory Model & Performance**](/cortex/languages/java/advanced/the-java-memory-model-and-performance) — `volatile`, happens-before in depth, the JIT, and garbage collection.
5. [**I/O, Files & NIO.2**](/cortex/languages/java/advanced/io-files-and-nio2) — `Path`/`Files`, the stream name clash, and bytes vs characters.
6. [**Modern Java Idioms & the Type System**](/cortex/languages/java/advanced/modern-java-idioms) — records + sealed + patterns as one data-oriented design.
7. [**Testing, Tooling & Packaging**](/cortex/languages/java/advanced/testing-tooling-and-packaging) — JUnit 5, build tools, dependencies, and executable JARs.

Every code block with a ▶ Run button is live; the concurrency, performance, and tooling chapters include real captured runs (a data race, JIT and GC logs, a `mvn test` summary) where behavior is nondeterministic or project-level. The habit that matters most at this tier is **knowing the cost**: streams, parallelism, immutability, and abstractions all have trade-offs, and senior judgment is choosing them deliberately.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.
