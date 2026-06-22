---
title: Control Flow
summary: Tier 1 of the Java book. Six chapters take you from a single boolean decision to your first real programs — conditions, the four loop shapes, break/continue, arrays, and methods (including Java's pass-by-value rule). Every example compiled and run, with the recurring traps shown as real compiler errors and exceptions.
prereqs: []
---

# Control Flow

This is Tier 1. With [Tier 0](/cortex/languages/java/first-steps/what-java-is-and-running-code) behind you — values, numbers, text, I/O — these six chapters give a program the power to *decide* and to *repeat*, which is where it stops being a calculator and starts being software. You'll make choices with `boolean` logic and `if`/`switch`, repeat work with loops, steer that repetition with `break`/`continue`, store many values in arrays, and package logic into reusable methods.

Six chapters, in order:

1. [**Booleans & Logic**](/cortex/languages/java/control-flow/booleans-and-logic) — the yes/no type with no truthiness, comparisons, `&&`/`||`/`!`, and short-circuit evaluation as a guard.
2. [**Conditionals**](/cortex/languages/java/control-flow/conditionals) — `if`/`else if`/`else`, the `switch` statement and its fall-through trap, exhaustive `switch` expressions, and the ternary `?:`.
3. [**Loops**](/cortex/languages/java/control-flow/loops) — `while`, `do-while`, the classic `for`, and the enhanced `for`, plus the off-by-one and infinite-loop traps.
4. [**Loop Control & Patterns**](/cortex/languages/java/control-flow/loop-control-and-patterns) — `break`, `continue`, labeled `break`, and the accumulation idioms most loops are built from.
5. [**Arrays**](/cortex/languages/java/control-flow/arrays) — a fixed block of indexed slots, bounds checking at run time, iteration, jagged 2D arrays, and why an array isn't a growable list.
6. [**Methods**](/cortex/languages/java/control-flow/methods) — parameters, return types, `void`, overloading, and Java's pass-by-value rule (including how object references are passed by value).

Every code block with a ▶ Run button is live — change it, run it again, and watch what the compiler or the JVM says. The fastest way to learn at this tier is to **perturb working examples**: move a `break`, flip a `<` to `<=`, forget a `return`, and see exactly which kind of error you get — compile-time or run-time.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.
