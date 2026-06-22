---
title: Numbers & Arithmetic
summary: Java arithmetic runs on fixed-width typed values, so the operands' types — not the math you intend — decide the result. Integer division truncates, mixing types promotes to the wider one, casts convert deliberately, and int overflow wraps silently to a negative number. Precedence and the Math class, with every trap shown as real output.
prereqs: []
---

# Numbers & Arithmetic — When the Type Decides the Answer

Java's arithmetic looks like the maths you already know — `+`, `-`, `*`, `/` — but it obeys one rule the maths classroom never mentioned: **the result is shaped by the operands' types, not by the answer you have in mind.** Divide two integers and the fraction vanishes; push an `int` past its range and it wraps silently to a negative number; mix an `int` with a `double` and the whole expression becomes a `double`. The same `/` means different things depending on what sits to its left and right. This chapter is about reading those types so the numbers come out right.

This builds directly on [the primitive types](/cortex/languages/java/first-steps/variables-and-primitive-types) — the sizes and ranges from that chapter are exactly what decides each result here. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [The arithmetic operators](#1-the-arithmetic-operators)
2. [Integer division truncates](#2-integer-division-truncates)
3. [Mixing numeric types, and casting](#3-mixing-numeric-types-and-casting)
4. [Silent integer overflow](#4-silent-integer-overflow)
5. [Precedence and the `Math` class](#5-precedence-and-the-math-class)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. The arithmetic operators

Five operators do the everyday arithmetic: `+` add, `-` subtract, `*` multiply, `/` divide (its own section, next), and `%` **remainder** — what is left over after a division.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(7 + 2);
        System.out.println(7 - 2);
        System.out.println(7 * 2);
        System.out.println(7 % 2);
    }
}
```

**Output:**
```
9
5
14
1
```

**Analysis.** `7 + 2 = 9`, `7 - 2 = 5`, `7 * 2 = 14`, and `7 % 2 = 1` because `7` is `3 * 2 + 1` — the remainder is `1`. `%` is how you test divisibility (`n % 2 == 0` means "even") and how you fold a value into a range.

**Intuition.**
*Mechanism.* `%` returns the remainder, and in Java its sign follows the **left** operand (the dividend), not the right.

*Concrete bite.* So it is not quite the mathematical "modulo," which is always non-negative:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(-7 % 2);
        System.out.println(7 % -2);
    }
}
```

**Output:**
```
-1
1
```

`-7 % 2` is `-1`, not `1` — the result took the sign of `-7`. `7 % -2` is `1`, taking the sign of `7`. The magnitude is the remainder; the sign comes from the dividend.

*Earned rule.* Use `%` for remainders and divisibility, but when you need an always-non-negative "wrap" of a possibly-negative number, `-7 % 2` will not give it — add the divisor back, or use `Math.floorMod`, which returns a non-negative result for a positive divisor. The cost of assuming `%` is mathematical modulo is an off-by-a-sign bug that appears precisely when the input goes negative.

---

## 2. Integer division truncates

`/` between two integers is **integer division**: it gives the whole-number quotient and discards the remainder.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(7 / 2);
        System.out.println(1 / 2);
        System.out.println(9 / 10);
    }
}
```

**Output:**
```
3
0
0
```

**Analysis.** `7 / 2` is `3` (not `3.5`), `1 / 2` is `0`, and `9 / 10` is `0`. The fraction is not rounded — it is **truncated**, dropped toward zero. Both operands are `int`, so the result is an `int`, and an `int` has nowhere to keep a fractional part.

**Intuition.**
*Mechanism.* When both operands are integer types, `/` computes the integer quotient; the result type is that integer type, so a fractional part simply cannot exist.

*Concrete bite.* The average bug — a `double` result produced one step too late:

```java run
public class Main {
    public static void main(String[] args) {
        int total = 7, count = 2;
        double average = total / count;   // looks right, isn't
        System.out.println(average);
    }
}
```

**Output:**
```
3.0
```

Even though `average` is a `double`, the right-hand side `total / count` is `int / int`, evaluated first — as `3` — and only *then* widened to `3.0`. The division already happened in integer-land; widening the answer afterward cannot recover the lost `.5`. So the "average" of 7 and 2 prints `3.0`.

*Earned rule.* Integer `/` truncates; to get a real quotient, make sure at least one operand is floating-point *before* the division runs. The cost of forgetting is a silent wrong answer rather than an error — `3.0` looks reasonable, which is what makes this Java's most common arithmetic bug. The fix is the next section's job.

---

## 3. Mixing numeric types, and casting

When operands have different numeric types, Java **promotes** the narrower to the wider before computing: `int / double` becomes `double / double`. To force a type yourself, **cast** with `(type)` written in front of a value.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(7 / 2.0);                  // one double operand → double division
        int total = 7, count = 2;
        System.out.println((double) total / count);   // cast an operand first
        System.out.println((int) 3.9);                // narrowing cast → truncates
    }
}
```

**Output:**
```
3.5
3.5
3
```

**Analysis.** `7 / 2.0`: the `2.0` is a `double`, so `7` is promoted to `7.0` and the division is `7.0 / 2.0 = 3.5`. `(double) total / count`: the cast turns `total` into `7.0` first — and `(double)` binds to just `total` — so it is `7.0 / 2 = 3.5`. And `(int) 3.9` is a **narrowing** cast: it chops the fraction, giving `3` (toward zero — `(int) 3.9` is `3`, not `4`).

**Intuition.**
*Mechanism.* A cast `(double) x` produces a value of the target type for that one spot; automatic promotion does the same when types mix. Precedence matters: a cast binds tightly, to the operand immediately after it — `(double) total / count` casts `total`, *then* divides.

*Concrete bite.* Cast the *result* instead of an operand and the bug survives:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println((double) (7 / 2));   // cast too late
    }
}
```

**Output:**
```
3.0
```

The parentheses force `7 / 2` first (integer division → `3`), and only then cast `3` to `3.0`. You converted the answer *after* the fraction was already gone. Contrast `(double) 7 / 2`, which casts `7` first and gives `3.5`.

*Earned rule.* To divide as reals, get a floating-point operand into the expression *before* the division — `(double) total / count`, not `(double) (total / count)`. The cost of casting is that **narrowing** casts (`(int) 3.9`) silently truncate, discarding information on purpose: you are telling the compiler "I accept the loss." That same silent loss, uninvited, is the next trap.

---

## 4. Silent integer overflow

An `int` is 32 bits, with a range of about −2.1 billion to +2.1 billion. When arithmetic produces a result outside that range, Java does **not** grow the type to fit — it **wraps around**, silently, to the opposite end. There is no error at all.

```java run
public class Main {
    public static void main(String[] args) {
        int max = Integer.MAX_VALUE;
        System.out.println(max);
        System.out.println(max + 1);
    }
}
```

**Output:**
```
2147483647
-2147483648
```

**Analysis.** `Integer.MAX_VALUE` is the largest `int`, `2147483647`. Add `1` and, instead of `2147483648`, you get `-2147483648` — the *smallest* `int`. The value rolled over the top of the range straight to the bottom, like a car odometer passing its maximum. No exception, no warning; just a negative number where a larger positive was expected.

**Intuition.**
*Mechanism.* `int + int` is always an `int`. The result keeps only the low 32 bits; when the true answer would need a 33rd bit, that bit is discarded, and because of how signed integers are encoded, discarding it flips the sign. The type never widens itself to fit.

*Concrete bite.* It hides in ordinary code — a product of two modest numbers:

```java run
public class Main {
    public static void main(String[] args) {
        int a = 100000;
        int b = 100000;
        System.out.println(a * b);     // ten billion — but computed as an int
    }
}
```

**Output:**
```
1410065408
```

`100000 * 100000` is ten billion, far beyond an `int`, so it wraps to a meaningless `1410065408`. Nothing looked dangerous; the type quietly betrayed the arithmetic.

*Earned rule.* When a value can exceed ~2 billion, use `long` (64 bits, up to ~9.2 quintillion), and make at least one operand a `long` so the operation runs in 64-bit: `100000L * 100000` gives the correct `10000000000`. The cost of ignoring overflow is the most dangerous kind of bug — a wrong number with nothing to flag it. When you must be certain, `Math.multiplyExact` and `Math.addExact` **throw** instead of wrapping:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(Math.multiplyExact(100000, 100000));
    }
}
```

**Output** *(a thrown exception; the full stack trace follows the first line, pointing at the `multiplyExact` call):*
```
Exception in thread "main" java.lang.ArithmeticException: integer overflow
```

Swapping a silent wrong answer for a loud `ArithmeticException` is usually the trade you want. *(Exceptions get their full treatment in Tutorial 24, in Tier 4.)*

---

## 5. Precedence and the `Math` class

When several operators meet in one expression, Java follows **precedence**: `*`, `/`, and `%` bind tighter than `+` and `-`, and parentheses override the order. For arithmetic beyond the basic operators, the `Math` class supplies `sqrt`, `pow`, `abs`, `max`, `min`, and more.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(2 + 3 * 4);     // * before + → 14
        System.out.println((2 + 3) * 4);   // parentheses → 20
        System.out.println(Math.max(10, 7));
        System.out.println(Math.sqrt(144.0));
        System.out.println(Math.pow(2, 10));
    }
}
```

**Output:**
```
14
20
10
12.0
1024.0
```

**Analysis.** `2 + 3 * 4` is `2 + 12 = 14`, because `*` binds tighter than `+`; parentheses force `5 * 4 = 20`. `Math.max` returns the larger value; `Math.sqrt(144.0)` is `12.0`; and `Math.pow(2, 10)` is `1024.0` — note the `.0`: `Math.pow` always returns a `double`, so even a whole-number power comes back as a decimal.

**Intuition.**
*Mechanism.* Precedence is fixed grammar the compiler applies before anything runs; `Math.pow` and `Math.sqrt` are library methods that take and return `double`.

*Concrete bite.* Assuming `Math.pow` yields an integer bites the moment you try to store its result in an `int`:

```java run
public class Main {
    public static void main(String[] args) {
        int kib = Math.pow(2, 10);
        System.out.println(kib);
    }
}
```

**Compiler error:**
```
Main.java:3: error: incompatible types: possible lossy conversion from double to int
        int kib = Math.pow(2, 10);
                          ^
1 error
```

`Math.pow` returns `1024.0`, a `double`, and a `double` will not fit into an `int` without losing its fractional part — so the compiler refuses. The fix is a deliberate cast, `int kib = (int) Math.pow(2, 10);`, which gives `1024`.

*Earned rule.* Let precedence and `Math` do the arithmetic, but remember that `Math.pow` and `Math.sqrt` return `double` — assign to a `double`, or cast deliberately when you truly want an `int`, accepting the truncation. Reach for parentheses whenever the intended grouping is not obvious at a glance; they cost nothing and prevent precedence surprises.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| The operands' types decide an operator's behaviour | The same `/` means integer or floating-point division depending on its operands |
| `int / int` truncates toward zero | `7 / 2` is `3`; `double average = 7 / 2;` prints `3.0`, not `3.5` |
| Mixed types promote to the wider; casts convert deliberately | Put a floating-point operand in *before* dividing: `(double) total / count` |
| Narrowing casts silently drop information | `(int) 3.9` is `3`; you are accepting the loss on purpose |
| `int` arithmetic wraps past its range with no error | `Integer.MAX_VALUE + 1` is negative; use `long`, or `Math.*Exact` to throw |
| `*` `/` `%` bind tighter than `+` `-`; `Math.pow`/`sqrt` return `double` | Parenthesise for clarity; store `Math.pow` results in a `double` |

## 7. Gotcha checklist

- **A division gives a whole number when you expected a fraction →** both operands are integers; make one floating-point (`(double) a / b` or `a / 2.0`).
- **`double x = a / b;` is `.0` →** the division ran as `int / int` before the widening; cast an operand, not the result.
- **A sum or product is suddenly negative or absurd →** `int` overflow; switch to `long` (with an `L` operand) or use `Math.addExact` / `Math.multiplyExact`.
- **`incompatible types: possible lossy conversion from double to int` →** you stored a `double` (often a `Math.pow`/`sqrt` result) in an `int`; assign to a `double` or cast explicitly.
- **`%` of a negative number has the "wrong" sign →** the remainder follows the dividend; use `Math.floorMod` for a non-negative result.
- **An expression groups unexpectedly →** check precedence (`*` before `+`); add parentheses to force your intended order.

---

*Predict, then check.* Predict the exact output of each line before running it: `System.out.println(5 / 2);` · `System.out.println(5 / 2.0);` · `System.out.println((double) 5 / 2);` · `System.out.println((double) (5 / 2));`. Three of the four differ. Explain, in terms of *when* the division happens and *what types* its operands have, why two print `2.5`, one prints `2`, and one prints `2.0`.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
