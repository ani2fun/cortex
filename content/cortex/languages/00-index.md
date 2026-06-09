---
title: Languages
summary: A multi-language curriculum — SQL first, with Python and Scala intended to follow. Each language is a self-contained section with its own mental models, production-reality examples, and practice ladder.
prereqs: []
---

# Languages

A working engineer doesn't reach for a single language — they reach for the *right* language for the problem in front of them. SQL for "I have data and I need to ask it a question." Python for "glue this thing to that thing, fast." Scala or Java for "this needs to run reliably under load for years." Bash for "I just need to wire two services together." Each language has its own runtime, its own evaluation model, its own performance characteristics, its own ways of going wrong at 2 a.m.

This book is a curriculum across that landscape. Not "syntax for Python in 30 days." Not "100 SQL interview questions." A senior-engineer-grade treatment of *why* each language behaves the way it does, where it lives in real systems, and how to debug it when production catches fire.

Every section follows the same shape — a hook, the mental model, mechanics, edge cases, a "production reality" section, and a hint-driven practice ladder — so once you know how one section reads, every later one is familiar.

---

## What's here

1. [**SQL**](/cortex/languages/sql/index) — the data-querying language every backend engineer ends up touching. Postgres-canonical, with callouts where SQLite, MySQL, or SQL Server diverge. Ten modules from foundations through window functions, CTEs, indexes, and transactions.

## What's coming

The shape of the book is *one section per language*, and the SQL section is the template. Future sections planned, in rough order:

- **Python** — interpreter model, the GIL, when generators are the right answer, type-checking with `mypy`, the async/await mental model, `numpy` and `pandas` for data work.
- **Scala** — the JVM through a strongly-typed lens; the type system, immutability by default, `for` comprehensions, ZIO and effect systems, JS interop.
- **Bash** — the language nobody learns deliberately and everyone uses anyway. POSIX vs Bash-isms, `set -euo pipefail`, the `IFS` trap, signals and traps.
- **TypeScript** — JavaScript with brakes; the structural type system, declaration files, narrowing, why `any` is contagious.

Order isn't fixed. New sections will be added as time and demand warrant; each one is independent — you can read SQL without ever opening Python.

---

## Reading conventions

These are universal across every section in this book.

- Every chapter opens with a hook — a real-world scenario or "you've used this without realising" moment — before any formalism.
- Code blocks use the language's canonical syntax (no multi-language tabs). For SQL, that's PostgreSQL; brief callouts mark divergences from SQLite/MySQL/T-SQL when relevant.
- Every chapter closes with **Production reality**, **Practice ladder** (3–5 problems with hints, not solutions), and a **Final takeaway** of 2–4 punchy bullets.
- Cross-links use absolute paths (`/cortex/languages/sql-foundations-introduction-to-sql`) so a chapter still resolves if you've bookmarked or shared it.
- The DSA book ([Data Structures and Algorithms](/cortex/data-structures-and-algorithms/index)) is a frequent cross-reference — when a language chapter relies on a DSA concept (hash tables, B-trees, recursion), the link is one click away.

---

## How to read this book

Pick the section for whatever language you're trying to deepen. There is no "start here and walk forward" path across the whole book — the sections are independent. Within each section, there *is* a recommended order, laid out in that section's own index.

If you've never touched any of these languages and you're trying to become a backend engineer, the suggested order is: **SQL → Python → Bash → Scala or TypeScript**. SQL because every backend touches it; Python because the rest of the data-and-glue ecosystem assumes you know it; Bash because you'll need it the day a service breaks; Scala or TypeScript because you'll eventually need a real language with a type system.

But that's a suggestion, not a curriculum. Most readers will arrive here looking for one specific language. Click the section you need.
