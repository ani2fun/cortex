---
title: Booleans & Logic
summary: Java's boolean is its own type with exactly two values and no truthiness — a condition must be a real boolean, so boolean b = 1 (and if (1)) won't compile, and the =/== slip is caught at compile time. Comparisons, the logical operators &&/||/!, short-circuit evaluation as a guard, precedence, and the floating-point equality trap — every example compiled and run.
prereqs: []
---

# Booleans & Logic — Yes/No as a Real Type

Decisions in a program come down to yes-or-no questions, and Java gives those questions their own type: `boolean`, with exactly two values, `true` and `false`. What makes Java strict — and what trips up newcomers from C or Python — is that **nothing else counts as a yes or no.** A number is not a stand-in for a condition: `1` is not "true," `0` is not "false," and the compiler rejects code that confuses them. You build conditions from **comparisons** (`<`, `==`, …), combine them with the **logical operators** `&&`, `||`, `!`, and lean on **short-circuit** evaluation, which stops the moment the answer is known.

This builds on the [primitive types](/cortex/languages/java/first-steps/variables-and-primitive-types) — `boolean` is one of the eight — and the [comparison of `==` vs `.equals`](/cortex/languages/java/first-steps/strings-the-basics) from strings. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [The `boolean` type: no truthiness](#1-the-boolean-type-no-truthiness)
2. [Comparisons produce booleans](#2-comparisons-produce-booleans)
3. [The logical operators `&&`, `||`, `!`](#3-the-logical-operators---)
4. [Short-circuit evaluation](#4-short-circuit-evaluation)
5. [Precedence and a floating-point caveat](#5-precedence-and-a-floating-point-caveat)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. The `boolean` type: no truthiness

A `boolean` holds exactly one of two values, written `true` and `false` (lowercase, no quotes — they are keywords, not text). That is the whole type.

```java run
public class Main {
    public static void main(String[] args) {
        boolean isReady = true;
        boolean done = false;
        System.out.println(isReady);
        System.out.println(done);
    }
}
```

**Output:**
```
true
false
```

**Analysis.** Two `boolean` variables, two values, printed as `true` and `false`. There is no third option and no numeric stand-in: a `boolean` is *only* ever `true` or `false`.

**Intuition.**
*Mechanism.* `boolean` is a distinct type, unrelated to the integers. Java does not treat "zero" as false or "non-zero" as true the way C and Python do — there is no automatic conversion from a number to a `boolean` at all.

*Concrete bite.* So assigning a number to a `boolean` is not "0 is false, 1 is true" — it simply does not compile:

```java run
public class Main {
    public static void main(String[] args) {
        boolean b = 1;
        System.out.println(b);
    }
}
```

**Compiler error:**
```
Main.java:3: error: incompatible types: int cannot be converted to boolean
        boolean b = 1;
                    ^
1 error
```

`1` is an `int`, `b` is a `boolean`, and Java has no rule turning one into the other. The same strictness will reject `if (1)` when you meet `if` in the [next chapter](/cortex/languages/java/control-flow/conditionals) — a condition must be a `boolean`, never a number.

*Earned rule.* Conditions in Java are genuine `boolean`s; you cannot abbreviate "is this non-zero?" as the number itself. The cost is a few more characters (`count != 0` instead of `count`), and the benefit is that an entire family of C bugs — treating a stray integer as a truth value — cannot occur, because the compiler forbids the confusion outright.

---

## 2. Comparisons produce booleans

You rarely write `true`/`false` literals; you *compute* booleans by comparing values. The six comparison operators — `<`, `>`, `<=`, `>=`, `==` (equal), `!=` (not equal) — each take two values and produce a `boolean`.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(3 < 5);
        System.out.println(3 == 3);
        System.out.println(3 != 3);
        int age = 20;
        boolean adult = age >= 18;
        System.out.println(adult);
    }
}
```

**Output:**
```
true
true
false
true
```

**Analysis.** `3 < 5` is `true`, `3 == 3` is `true`, `3 != 3` is `false`. The last pair is the useful shape: `age >= 18` compares and stores the resulting `boolean` in `adult`, which a later decision can use. Note `==` asks "equal?"; from [Tutorial 4](/cortex/languages/java/first-steps/strings-the-basics), on *objects* `==` compares identity, but on primitives like these it compares values.

**Intuition.**
*Mechanism.* A comparison is an expression whose result type is `boolean`. `==` (two equals signs) is the *question* "are these equal?"; `=` (one equals sign) is the *action* "assign." They are different operators with different result types — a comparison yields a `boolean`, an assignment yields the value assigned.

*Concrete bite.* In C, writing `=` where you meant `==` is a classic silent bug. Java catches it at compile time, because an assignment's result is not a `boolean`:

```java run
public class Main {
    public static void main(String[] args) {
        int x = 3;
        boolean ok = (x = 5);
        System.out.println(ok);
    }
}
```

**Compiler error:**
```
Main.java:4: error: incompatible types: int cannot be converted to boolean
        boolean ok = (x = 5);
                        ^
1 error
```

`(x = 5)` assigns `5` to `x` and evaluates to the `int` `5`; storing that in a `boolean` fails to compile. The very typo that compiles-and-misbehaves in C is a compile error here.

*Earned rule.* Use `==` to compare and `=` to assign; if you slip, the compiler stops you wherever a `boolean` was required. The cost is that the protection only holds *where a boolean is expected* — `=` and `==` are both legal in other spots — so the discipline is to read every condition as a question, not a statement.

---

## 3. The logical operators `&&`, `||`, `!`

To combine yes/no answers, use the three logical operators: `&&` ("and" — true only if **both** sides are true), `||` ("or" — true if **either** side is true), and `!` ("not" — flips a `boolean`).

```java run
public class Main {
    public static void main(String[] args) {
        boolean a = true, b = false;
        System.out.println(a && b);
        System.out.println(a || b);
        System.out.println(!a);
    }
}
```

**Output:**
```
false
true
false
```

**Analysis.** `a && b` is `false` because `b` is false (and needs both). `a || b` is `true` because `a` is true (or needs only one). `!a` flips `true` to `false`. These three build every compound condition you will write.

**Intuition.**
*Mechanism.* Each operator combines `boolean`s into a `boolean`. `!` binds tightest, then `&&`, then `||` — so `!` applies to just the term it touches, not a whole expression.

*Concrete bite.* That precedence of `!` is where intuition fails — "not (a and b)" is **not** the same as "not-a and not-b":

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(!(true && false));
        System.out.println(!true && !false);
    }
}
```

**Output:**
```
true
false
```

`!(true && false)` is `!(false)` = `true`. But `!true && !false` is `false && true` = `false`. Negating a compound condition flips *and* to *or* (De Morgan's law): `!(a && b)` equals `!a || !b`, not `!a && !b`.

*Earned rule.* Build conditions from `&&`, `||`, `!`, and parenthesise when negating a compound test — `!(a && b)` means `!a || !b`. The cost of forgetting De Morgan is a condition that looks negated but tests the wrong thing; when in doubt, wrap the whole expression in `!( … )` rather than distributing the `!` by hand.

---

## 4. Short-circuit evaluation

`&&` and `||` are **short-circuiting**: they evaluate the left side first and stop as soon as the answer is decided. `false && anything` is `false` without ever looking at the right side; `true || anything` is `true` the same way. This is not just speed — it is a guard.

```java run
public class Main {
    public static void main(String[] args) {
        int x = 0;
        boolean ok = (x != 0) && (10 / x > 0);
        System.out.println(ok);
    }
}
```

**Output:**
```
false
```

**Analysis.** `x` is `0`, so `x != 0` is `false`, and `&&` stops there — the right side `10 / x` is **never evaluated**, so the divide-by-zero never happens. The whole expression is `false`, and the program runs cleanly. The first test guarded the second.

**Intuition.**
*Mechanism.* `&&` computes its left operand, and only if that is `true` does it compute the right. `||` is the mirror: it computes the right only if the left is `false`. The single-character cousins `&` and `|` do **not** short-circuit — they always evaluate both sides.

*Concrete bite.* Swap `&&` for `&` and the guard is gone — both sides run, and `10 / 0` throws:

```java run
public class Main {
    public static void main(String[] args) {
        int x = 0;
        boolean ok = (x != 0) & (10 / x > 0);
        System.out.println(ok);
    }
}
```

**Output** *(a thrown exception):*
```
Exception in thread "main" java.lang.ArithmeticException: / by zero
```

`&` evaluated `10 / x` even though `x != 0` was already `false`, and dividing by zero threw `ArithmeticException`. Same logic, one missing character, a crash instead of a clean `false`.

*Earned rule.* Use `&&` and `||` (not `&`/`|`) for conditions, and put the cheap, protective test first — `x != 0 && 10 / x > 0`, `s != null && s.length() > 0`. The cost is that order now matters: short-circuiting only protects the right side if the guard is on the left, so a misordered condition loses the protection and can still crash. (`null` and the `s != null` guard arrive properly in Tier 2.)

---

## 5. Precedence and a floating-point caveat

When `&&` and `||` mix, `&&` binds tighter than `||` — `a || b && c` means `a || (b && c)`. And one comparison deserves an early warning: `==` on floating-point numbers is treacherous, because decimals are stored approximately.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(true || false && false);
        System.out.println((true || false) && false);
    }
}
```

**Output:**
```
true
false
```

**Analysis.** `true || false && false` groups as `true || (false && false)` = `true || false` = `true`, because `&&` binds tighter. Forcing the other grouping with parentheses, `(true || false) && false` = `true && false` = `false`. The parentheses changed the answer.

**Intuition.**
*Mechanism.* Precedence is fixed grammar: `!` then `&&` then `||`, all looser than the comparison operators. So `a < b && c < d` already means `(a < b) && (c < d)` without parentheses — but mixing `&&` and `||` without them invites the wrong grouping.

*Concrete bite.* Floating-point equality is the comparison that lies. Decimals like `0.1` cannot be represented exactly in binary, so arithmetic drifts:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(0.1 + 0.2 == 0.3);
        System.out.println(0.1 + 0.2);
    }
}
```

**Output:**
```
false
0.30000000000000004
```

`0.1 + 0.2` is `0.30000000000000004`, not `0.3`, so `== 0.3` is `false`. Nothing is broken — `double` simply stores approximations, and `==` compares them exactly.

*Earned rule.* Parenthesise whenever `&&` and `||` mix, and never compare `double`s with `==` — test that the difference is within a small tolerance (`Math.abs(a - b) < 1e-9`) instead. The cost of trusting `==` on floating-point is a comparison that is *false* for values you consider equal, with no error to flag it — the same "silent wrong answer" hazard as [integer overflow](/cortex/languages/java/first-steps/numbers-and-arithmetic), in a different disguise.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| `boolean` is its own type; there is no truthiness | `boolean b = 1` won't compile; conditions must be real booleans |
| Comparisons produce a `boolean`; `==` asks, `=` assigns | `boolean ok = (x = 5)` is a compile error — the `=`/`==` slip is caught |
| `&&`/`||`/`!` combine booleans; `!` binds tightest | `!(a && b)` equals `!a || !b` (De Morgan), not `!a && !b` |
| `&&`/`||` short-circuit; `&`/`|` do not | A left-hand guard (`x != 0 && 10/x > 0`) protects the right side |
| `&&` binds tighter than `||`; floats are approximate | Parenthesise mixed logic; never compare `double`s with `==` |

## 7. Gotcha checklist

- **`incompatible types: int cannot be converted to boolean` →** you used a number as a condition (or wrote `=` for `==`); make it a real comparison.
- **A negated compound condition tests the wrong thing →** De Morgan: `!(a && b)` is `!a || !b`; wrap the whole expression in `!( … )`.
- **Divide-by-zero / null crash despite a guard →** you used `&`/`|` (no short-circuit) or put the guard on the wrong side; use `&&`/`||` with the guard first.
- **A mixed `&&`/`||` condition groups unexpectedly →** `&&` binds tighter than `||`; add parentheses.
- **Two equal-looking decimals compare as unequal →** floating-point approximation; compare with a tolerance, not `==`.

---

*Predict, then check.* Predict each line's output before running: `System.out.println(5 > 3);` · `System.out.println(5 > 3 && 2 > 4);` · `System.out.println(5 > 3 || 2 > 4);` · `System.out.println(!(5 > 3));`. Then a harder one: with `int n = 0;`, what does `System.out.println(n != 0 && 100 / n > 0);` print — and what would change if you wrote `&` instead of `&&`? Explain the second in terms of short-circuiting before you run it.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
