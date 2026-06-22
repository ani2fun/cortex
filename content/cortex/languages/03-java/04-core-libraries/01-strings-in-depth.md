---
title: Strings in Depth
summary: A String is immutable, so every "change" allocates a new object — which makes building a string with + in a loop quadratic, and StringBuilder the O(n) fix. Plus the pool and intern(), why StringBuilder doesn't override equals, formatting with formatted(), and multi-line text blocks (JDK 15). Every behavior shown with verified output.
prereqs: []
---

# Strings in Depth — Immutability and Its Cost

You learned in Tier 0 that a `String` is immutable: every method that seems to edit it returns a *new* object. That was a correctness fact; here it becomes a *performance* fact. Because each `+` builds a brand-new String, concatenating in a loop quietly does O(n²) work — and `StringBuilder`, a mutable character buffer, is the O(n) fix. This chapter also returns to the [pool from the object model](/cortex/languages/java/classes-and-objects/references-equality-and-the-object-model) with `intern()`, explains why `StringBuilder` (unlike `String`) doesn't compare by contents, and adds the modern tools for assembling text: `formatted()` and multi-line **text blocks**.

This is the deep pass of [Strings, the Basics](/cortex/languages/java/first-steps/strings-the-basics). Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Immutability and the cost of `+`](#1-immutability-and-the-cost-of-)
2. [`StringBuilder`: the O(n) fix](#2-stringbuilder-the-on-fix)
3. [The pool and `intern()`](#3-the-pool-and-intern)
4. [Formatting and text blocks](#4-formatting-and-text-blocks)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Immutability and the cost of `+`

A `String` never changes, so `s = s + "b"` does **not** extend `s` — it builds a new String and points `s` at it, discarding the old one. We can prove a new object appears each time with `System.identityHashCode`, which returns a number tied to a specific object:

```java run
public class Main {
    public static void main(String[] args) {
        String s = "a";
        int h1 = System.identityHashCode(s);
        s = s + "b";
        int h2 = System.identityHashCode(s);
        System.out.println(s);
        System.out.println(h1 == h2);
    }
}
```

**Output:**
```
ab
false
```

**Analysis.** After `s = s + "b"`, `s` reads `"ab"` — but `h1 == h2` is `false`, meaning `s` now refers to a *different* object than before. The `+` didn't modify the original `"a"`; it allocated a new `"ab"`. (The identity numbers themselves vary per run, which is why we compare them rather than print them.)

**Intuition.**
*Mechanism.* Each `+` on strings allocates a new String and copies all the characters into it. In a loop that appends `n` times, pass `k` copies `k` characters, so the total copying is `1 + 2 + … + n` ≈ n²/2 — **quadratic**. The work is invisible (the result is correct), but it grows with the square of the length.

*Concrete bite.* The `false` above is the proof that a new object was made for a single `+`. Now imagine a loop: `String out = ""; for (...) out += piece;` produces the right string but allocates and copies a fresh, ever-longer String on every iteration — fine for a handful of pieces, ruinous for thousands.

*Earned rule.* Treat `+` as fine for a *fixed, small* number of concatenations (it's readable, and the compiler optimizes a single expression), but never build a string by `+=` inside a loop. The cost of ignoring this is a program that works in tests and crawls in production on large input — a performance bug with no error, fixed by the next section.

---

## 2. `StringBuilder`: the O(n) fix

`StringBuilder` is a *mutable* character buffer. `append` adds to it **in place** — no new object per step — and `toString()` produces the final `String` once, at the end. Total work is linear.

```java run
public class Main {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i);
        }
        String result = sb.toString();
        System.out.println(result);
        System.out.println(result.length());
    }
}
```

**Output:**
```
01234
5
```

**Analysis.** The loop appended `0`..`4` into one growing buffer, and `toString()` produced `"01234"` at the end — a single String allocation instead of one per step. For building strings piece by piece (loops, conditionals, joining), `StringBuilder` is the tool; `append` returns the same builder, so calls also chain (`sb.append(x).append(y)`).

**Intuition.**
*Mechanism.* A `StringBuilder` holds a resizable `char[]` and mutates it; appending usually just writes into spare capacity (occasionally doubling the array), so n appends cost O(n) total, not O(n²). It is mutable precisely where `String` is not.

*Concrete bite.* Because `StringBuilder` is a plain mutable object that does **not** override `equals`, comparing two builders compares identity, not contents:

```java run
public class Main {
    public static void main(String[] args) {
        StringBuilder a = new StringBuilder("hi");
        StringBuilder b = new StringBuilder("hi");
        System.out.println(a.equals(b));
        System.out.println(a.toString().equals(b.toString()));
    }
}
```

**Output:**
```
false
true
```

`a.equals(b)` is `false` — `StringBuilder` inherits the default `equals` (same-object identity), so two builders are never "equal" unless they're the same object. To compare *contents*, convert to `String` first: `a.toString().equals(b.toString())` is `true`. (Why a class compares by identity unless it overrides `equals` is Tutorial 19's contract.)

*Earned rule.* Use `StringBuilder` to assemble a string across multiple steps, and call `toString()` once to finish; compare its contents via `String`, never with `StringBuilder.equals`. The cost is a little ceremony (build, then `toString`) and a mutable object to manage; the benefit is linear-time construction and a clear seam between "building" (mutable) and "built" (the immutable `String` you hand out).

---

## 3. The pool and `intern()`

From the object model: string *literals* are interned into a shared pool, so equal literals are the same object, but anything computed at run time is a fresh object. `intern()` lets you push a runtime string into the pool and get back the shared instance.

```java run
public class Main {
    public static void main(String[] args) {
        String a = "he";
        String b = a + "llo";   // built at run time from a variable — not pooled
        String c = "hello";
        System.out.println(b == c);
        System.out.println(b.intern() == c);
        System.out.println(b.equals(c));
    }
}
```

**Output:**
```
false
true
true
```

**Analysis.** `b` is `"he" + "llo"` computed from the *variable* `a`, so it's a new object, and `b == c` is `false` even though both read `"hello"`. `b.intern()` returns the pooled `"hello"` — the same object the literal `c` points at — so `b.intern() == c` is `true`. And `b.equals(c)` compares characters, so it's `true` regardless. (Note: `"he" + "llo"` written with two *literals* would be folded by the compiler and pooled — it's the variable `a` that makes `b` runtime-built.)

**Intuition.**
*Mechanism.* The pool holds one canonical instance per distinct string content. Literals go in at compile time; `intern()` looks up the content and returns the existing canonical instance (adding it if absent). It is the bridge from "a runtime string" to "the pooled one."

*Concrete bite.* `b == c` being `false` is the same identity trap as the object model: only pooled strings share identity, and a variable-built string isn't pooled. `intern()` *can* make `==` work — but needing `intern()` to compare strings is a sign you should be using `.equals`.

*Earned rule.* Compare string contents with `.equals`; reach for `intern()` only as a deliberate memory optimization (deduplicating many equal runtime strings), never as a way to make `==` "work." The cost of `intern()` is a pool lookup and a permanently retained string; the benefit, in the rare right case, is one shared instance instead of thousands of duplicates.

---

## 4. Formatting and text blocks

Two modern tools make assembling readable text easier. `formatted(...)` is the instance-method twin of [`String.format`](/cortex/languages/java/first-steps/input-and-output) — same `%s`/`%d` placeholders. A **text block** (`"""…"""`, JDK 15) is a multi-line string literal that preserves line breaks and strips the common leading indentation.

```java run
public class Main {
    public static void main(String[] args) {
        String name = "Ada";
        int score = 95;
        String report = """
            Name:  %s
            Score: %d
            """.formatted(name, score);
        System.out.print(report);
        System.out.println("[end]");
    }
}
```

**Output:**
```
Name:  Ada
Score: 95
[end]
```

**Analysis.** The text block spans three lines between the `"""` delimiters; Java stripped the common 12-space indentation (measured against the closing `"""`), leaving `Name:  %s` / `Score: %d` and a trailing newline. `.formatted(name, score)` then filled the placeholders. The `[end]` printed on its own line, confirming the block ended with a newline. Text blocks remove the old pain of `"line1\n" + "line2\n"` escaping for multi-line strings (SQL, JSON, HTML, reports).

**Intuition.**
*Mechanism.* A text block is just a `String` with friendlier syntax: the compiler removes the incidental leading whitespace (the minimum indentation across the content lines and the closing delimiter) and turns line breaks into `\n`. `formatted` is `String.format` as an instance method, so it reads left-to-right from the template.

*Concrete bite.* The indentation that's stripped is the *common minimum*, set by the least-indented line — including where you put the closing `"""`. Move the closing delimiter to the far left and *no* indentation is stripped, so every line keeps its leading spaces; align it under the content and the content's indentation disappears. The closing delimiter's position is a silent control knob.

*Earned rule.* Use text blocks for any multi-line literal and `formatted()` to fill them, aligning the closing `"""` with the content so indentation is stripped cleanly. The cost is remembering that the closing delimiter's column controls the stripping (a frequent surprise); the benefit is multi-line strings you can read and edit as the text they represent, without escape clutter.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| Every `+` on strings allocates a new String | `+=` in a loop is O(n²); a single `+` expression is fine |
| `StringBuilder` mutates one buffer; `toString()` finishes | Building piece-by-piece is O(n); the right tool for loops |
| `StringBuilder` doesn't override `equals` | Two builders compare by identity; compare contents via `String` |
| Literals are pooled; runtime-built strings are not | `b == "hello"` is false for a variable-built `b`; `intern()` returns the pooled one |
| Text blocks strip the common leading indentation | The closing `"""`'s column controls how much is stripped |

## 6. Gotcha checklist

- **String-building is slow on large input →** `+=` in a loop is quadratic; use a `StringBuilder` and one `toString()`.
- **Two `StringBuilder`s with the same text aren't `equals` →** it doesn't override `equals`; compare `a.toString().equals(b.toString())`.
- **A runtime-built string `== "literal"` is false →** only literals are pooled; use `.equals` (or `intern()` only as a memory optimization).
- **A text block has unexpected leading spaces →** the closing `"""` is too far left; align it under the content to strip the indentation.
- **Forgot `toString()` on a `StringBuilder` →** where a `String` is required you must convert; a stray builder won't match a `String` API.

---

*Predict, then check.* Predict the output of building `"0123456789"` two ways and printing the length of each: once with `String out = ""; for (int i=0;i<10;i++) out += i;`, once with a `StringBuilder`. Next, predict `new StringBuilder("x").equals(new StringBuilder("x"))`. Finally, write a text block holding two lines of JSON (`{` on its own line, a `"key": value` line, `}` on its own line) and predict where you must place the closing `"""` so the `{` and `}` sit at the left margin.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
