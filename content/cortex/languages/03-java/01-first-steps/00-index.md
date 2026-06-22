---
title: First Steps
summary: Tier 0 of the Java book, for readers with zero programming background. Five chapters take you from "what is a program?" to a working interactive program — compiling and running code, values and the eight primitives, arithmetic and its traps, text, and input/output — with every example compiled and run, and every compiler error shown for real.
prereqs: []
---

# First Steps

This is Tier 0 — the beginning. It assumes you have **never programmed before**, in any language. By the end of these five chapters you will have written, compiled, and run real Java: doing arithmetic, building text, reading what a person types, and printing answers back. Along the way you'll meet the idea that runs through the whole book — that Java checks your program **twice**, once when the compiler reads it and again when the JVM runs it — and you'll learn to read both kinds of message.

Five chapters, in order:

1. [**What Java Is & Running Code**](/cortex/languages/java/first-steps/what-java-is-and-running-code) — what a program is, the compile-then-run loop (`javac` and the JVM), why `public static void main` looks the way it does, and `println` — the command that lets a program talk back.
2. [**Variables & Primitive Types**](/cortex/languages/java/first-steps/variables-and-primitive-types) — naming values so you can reuse them, static typing, and the eight built-in kinds of value: whole numbers, decimals, true/false, and single characters.
3. [**Numbers & Arithmetic**](/cortex/languages/java/first-steps/numbers-and-arithmetic) — the operators, why integer division truncates, and why an `int` can silently wrap past its range to a negative number.
4. [**Strings, the Basics**](/cortex/languages/java/first-steps/strings-the-basics) — building and reshaping text, why a String can never be changed, and the `==`-vs-`.equals` trap that comparing text is full of.
5. [**Input & Output**](/cortex/languages/java/first-steps/input-and-output) — formatted output with `printf`, reading typed input with `Scanner`, turning text into numbers, and writing a first interactive program.

Every code block with a ▶ Run button is live — click it, change the code, run it again. The fastest way to learn at this tier is to **perturb working examples**: change a number, swap a word, break something on purpose, and see what the compiler or the JVM says. You cannot harm anything; the worst case is an error message, and reading error messages is itself a skill these chapters build.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.
