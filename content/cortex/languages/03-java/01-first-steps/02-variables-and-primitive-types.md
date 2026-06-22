---
title: Variables & Primitive Types
summary: Java is statically typed — every variable has a type fixed when you declare it and checked by the compiler on every line. The eight primitives hold their value directly; declarations, literals and their types, and var (inferred, not dynamic) — with the type-mismatch and out-of-range traps shown as real compiler errors.
prereqs: []
---

# Variables & Primitive Types — Typed Boxes for Values

A **variable** is a named place to keep a value so you can use it again. In Java that place has one extra property that shapes everything you write: a **type**, fixed when you declare the variable and checked by the compiler on every line. Java is **statically typed** — *static* meaning "decided before the program runs." You tell the compiler "this name holds an `int`," and from then on it guarantees the name only ever holds an `int`, refusing to compile any line that breaks the promise.

The simplest values are the **primitives** — eight built-in types for numbers, true/false, and single characters. A primitive variable holds its value **directly**: the box *is* the number. That word "directly" is doing quiet work — Tier 2 introduces the other kind of variable, a *reference*, which holds not a value but the location of one. Keep the distinction in your pocket; for now, primitives hold their value. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Declaring a variable: type, name, value](#1-declaring-a-variable-type-name-value)
2. [Static typing: the type is fixed and checked](#2-static-typing-the-type-is-fixed-and-checked)
3. [The eight primitive types](#3-the-eight-primitive-types)
4. [Literals: how you write a value fixes its type](#4-literals-how-you-write-a-value-fixes-its-type)
5. [`var`: inferred, not dynamic](#5-var-inferred-not-dynamic)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Declaring a variable: type, name, value

You **declare** a variable by writing its type, then its name, then `=` and a starting value. `int age = 25;` reads "make an `int` named `age`, starting at `25`." The type (`int`) comes first, and it is not optional.

```java run
public class Main {
    public static void main(String[] args) {
        int age = 25;
        System.out.println(age);
    }
}
```

**Output:**
```
25
```

**Analysis.** `int age = 25;` created a box of type `int`, named it `age`, and put `25` in it. `System.out.println(age)` looked up what `age` holds — `25` — and printed it. The box holds the number itself, not a pointer to it.

```d2
direction: right

name: "age  (a name)" {
  shape: oval
}

value: "int 25  (the value, held directly in the box)" {
  shape: rectangle
}

name -> value: "labels a box holding"
```

**Intuition.**
*Mechanism.* A declaration does two things at once: it tells the compiler the variable's **type** (so it can check every later use) and reserves a **box** that holds the value directly. The type becomes part of the variable forever; it is decided here, in the source, before the program runs.

*Concrete bite.* Leave out the type and the compiler doesn't know what `age` is — there is no box, and no permission to make one:

```java run
public class Main {
    public static void main(String[] args) {
        age = 25;
    }
}
```

**Compiler error:**
```
Main.java:3: error: cannot find symbol
        age = 25;
        ^
  symbol:   variable age
  location: class Main
1 error
```

"cannot find symbol" means the compiler looked for a declared variable named `age` and found none. In a dynamically typed language, `age = 25` would create the variable on the spot; Java instead insists you declare it — with a type — first.

*Earned rule.* Declare before you assign, and declare *with a type*: `int age = 25;`. The cost is a few more keystrokes than a language that conjures variables on first use; the payoff is that the compiler now knows `age`'s type and will catch, at compile time, every later line that misuses it — as the next section shows.

---

## 2. Static typing: the type is fixed and checked

Once a variable has a type, that type is fixed. You can change the *value* — reassign it — but only to another value of the same type, and the compiler checks this on every assignment.

```java run
public class Main {
    public static void main(String[] args) {
        int score = 10;
        score = 25;      // fine: 25 is an int, just like score
        System.out.println(score);
    }
}
```

**Output:**
```
25
```

**Analysis.** `score` started at `10`, then we reassigned it to `25`. Both are `int`s, so the compiler allowed it and the box's content changed from `10` to `25`. Reassignment changes the value; it never changes the type.

**Intuition.**
*Mechanism.* The type lives with the variable, not with whatever value you assign. On every assignment the compiler checks that the new value's type matches the variable's declared type; a mismatch is rejected before the program runs.

*Concrete bite.* Try to put text in an `int` and the compiler refuses — the canonical Java type error:

```java run
public class Main {
    public static void main(String[] args) {
        int n = "hello";
        System.out.println(n);
    }
}
```

**Compiler error:**
```
Main.java:3: error: incompatible types: String cannot be converted to int
        int n = "hello";
                ^
1 error
```

`"hello"` is text (a `String`); `n` is an `int`. There is no automatic way to turn arbitrary text into an integer, so the compiler stops you here, at compile time, rather than letting a nonsensical value flow into your program.

*Earned rule.* A variable holds exactly its declared type, checked on every assignment — so a whole class of "wrong kind of value" bugs is caught before the program runs. The cost is rigidity: when you genuinely need to convert between types (text to number, say), you must do it deliberately, with a conversion you'll meet in [Tutorial 5](/cortex/languages/java/first-steps/input-and-output). Static typing trades flexibility for a compiler that proves, on every build, that your values fit their boxes.

---

## 3. The eight primitive types

Java has exactly eight **primitive** types. You will use `int`, `double`, `boolean`, and `char` constantly; the other four are size variants you reach for occasionally. Here are six of them at work:

```java run
public class Main {
    public static void main(String[] args) {
        int count = 42;                    // whole number (the default integer type)
        long population = 8_000_000_000L;  // bigger whole numbers; note the L
        double price = 19.99;              // decimal number (the default)
        float ratio = 0.5f;                // smaller decimal; note the f
        boolean ready = true;              // true or false, nothing else
        char grade = 'A';                  // a single character, in single quotes
        System.out.println(count);
        System.out.println(population);
        System.out.println(price);
        System.out.println(ratio);
        System.out.println(ready);
        System.out.println(grade);
    }
}
```

**Output:**
```
42
8000000000
19.99
0.5
true
A
```

**Analysis.** Each line declared a differently typed box and printed its value. The output quietly shows three things: `long` and `int` both print as plain digits (the `L` on `8_000_000_000L` told the compiler "this literal is a `long`," which it must be, since the value is too big for an `int`); the underscores in `8_000_000_000L` are just visual grouping that the compiler ignores; and `char grade = 'A'` uses **single** quotes for one character, printing as `A`. Here is the whole set, by family:

| Type | Holds | Example literal | Size / range |
|---|---|---|---|
| `byte` | whole number | `120` | 8-bit, −128 … 127 |
| `short` | whole number | `1000` | 16-bit |
| `int` | whole number *(default)* | `42` | 32-bit, about ±2.1 billion |
| `long` | big whole number | `42L` | 64-bit |
| `float` | decimal | `0.5f` | 32-bit (fewer digits) |
| `double` | decimal *(default)* | `19.99` | 64-bit |
| `char` | one character | `'A'` | 16-bit |
| `boolean` | true / false | `true` | 1 bit, logically |

**Intuition.**
*Mechanism.* Each primitive reserves a fixed amount of space, and so can represent a fixed range of values — a `byte` is 8 bits (−128 to 127), an `int` 32 bits (about ±2.1 billion), a `long` 64 bits. The type is not just a label; it determines how many bits the box has.

*Concrete bite.* Because the size is fixed, a value outside a type's range will not fit — and the compiler says so:

```java run
public class Main {
    public static void main(String[] args) {
        byte b = 200;
        System.out.println(b);
    }
}
```

**Compiler error:**
```
Main.java:3: error: incompatible types: possible lossy conversion from int to byte
        byte b = 200;
                 ^
1 error
```

`200` is an ordinary `int`, but a `byte` tops out at `127`, so storing `200` would lose information. The compiler calls this a "lossy conversion" and refuses without an explicit instruction to truncate.

*Earned rule.* Pick the type by the range you need: `int` for ordinary whole numbers, `long` when they exceed ~2 billion, `double` for decimals, `boolean` and `char` for their obvious jobs; reach for `byte`, `short`, or `float` only when memory or an external format demands them. The cost of the wrong choice is either a compile error (a too-big literal) or — worse — a silent **overflow** at run time, when arithmetic pushes a value past the type's range without warning. That run-time trap is the subject of [Tutorial 3](/cortex/languages/java/first-steps/numbers-and-arithmetic).

---

## 4. Literals: how you write a value fixes its type

A **literal** is a value written directly in the source, like `25` or `'A'`. How you write it fixes *its own* type, independent of any variable — and that type must be compatible with where you put it. The defaults: a plain whole number is an `int`, and a number with a decimal point is a `double`. Suffixes and quotes choose otherwise.

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println(25);      // an int literal
        System.out.println(25L);     // a long literal (the L)
        System.out.println(3.14);    // a double literal
        System.out.println(3.14f);   // a float literal (the f)
        System.out.println('A');     // a char literal (single quotes)
        System.out.println("A");     // a String literal (double quotes)
    }
}
```

**Output:**
```
25
25
3.14
3.14
A
A
```

**Analysis.** The printed values look identical in pairs — `25` and `25L` both show `25`, `3.14` and `3.14f` both show `3.14`, `'A'` and `"A"` both show `A` — but the *types* differ: `int` vs `long`, `double` vs `float`, `char` vs `String`. The type is invisible in the output yet entirely real to the compiler. `L` makes a `long`, `f` makes a `float`, single quotes make a `char`, double quotes make a `String`.

**Intuition.**
*Mechanism.* The compiler gives every literal a type from how it is written, before considering where it goes. A bare integer literal is an `int` — always — even when the variable beside it is a `long`.

*Concrete bite.* That is why a number too big for an `int` is an error even when you "meant" a `long`:

```java run
public class Main {
    public static void main(String[] args) {
        int big = 3000000000;
        System.out.println(big);
    }
}
```

**Compiler error:**
```
Main.java:3: error: integer number too large
        int big = 3000000000;
                  ^
1 error
```

`3000000000` exceeds an `int`'s ~2.1 billion ceiling, and because the literal is an `int` by default, the compiler rejects it before it ever reaches the variable. Mark it as a `long` literal — add `L` — and store it in a `long` box, and it fits:

```java run
public class Main {
    public static void main(String[] args) {
        long big = 3_000_000_000L;
        System.out.println(big);
    }
}
```

**Output:**
```
3000000000
```

*Earned rule.* Write the literal in the form of the type you want: `L` for `long`, `f` for `float`, single quotes for `char`, double quotes for `String`; a plain integer is an `int` and a plain decimal is a `double`. The cost of forgetting is a compile error like "integer number too large" — annoying but honest, because it catches at compile time the mismatch that would otherwise become a wrong number at run time.

---

## 5. `var`: inferred, not dynamic

Spelling a type out twice can get tedious. Since Java 10, `var` lets the compiler **infer** a local variable's type from its initializer. It is a convenience, not a new kind of typing: the variable still has one fixed type; you simply did not write it.

```java run
public class Main {
    public static void main(String[] args) {
        var count = 42;       // compiler infers: int
        var price = 19.99;    // compiler infers: double
        var name = "Ada";     // compiler infers: String
        System.out.println(count);
        System.out.println(price);
        System.out.println(name);
    }
}
```

**Output:**
```
42
19.99
Ada
```

**Analysis.** `var count = 42` is exactly `int count = 42`: the compiler reads the initializer `42` (an `int` literal) and fixes `count`'s type to `int`. Likewise `price` becomes `double` (from `19.99`) and `name` becomes `String` (from `"Ada"`). `var` copied the type off the value on the right; it did not make the variable typeless.

**Intuition.**
*Mechanism.* `var` is resolved at **compile time**: the compiler looks at the initializer, works out its type, and writes that type into the variable as if you had typed it. The compiled bytecode is identical to the spelled-out version — there is no `var` left at run time.

*Concrete bite.* So `var` is not dynamic typing. The inferred type is just as fixed; reassign across types and it fails exactly as a spelled-out `int` would:

```java run
public class Main {
    public static void main(String[] args) {
        var x = 42;     // inferred: int
        x = "hello";    // x is an int — it can't hold text
        System.out.println(x);
    }
}
```

**Compiler error:**
```
Main.java:4: error: incompatible types: String cannot be converted to int
        x = "hello";
            ^
1 error
```

The error names `int`, even though you wrote `var` — proof that `x`'s type was fixed to `int` the instant it was initialized.

*Earned rule.* Use `var` when the initializer already makes the type obvious (`var total = 0;`, `var name = "Ada";`) to cut noise; keep the explicit type where naming it aids the reader. The boundaries are its cost: `var` needs an initializer on the same line (the compiler has nothing to infer from otherwise) and works only for local variables, never fields or parameters. Omit the initializer and it cannot even guess:

```java run
public class Main {
    public static void main(String[] args) {
        var y;
        y = 42;
        System.out.println(y);
    }
}
```

**Compiler error:**
```
Main.java:3: error: cannot infer type for local variable y
        var y;
            ^
  (cannot use 'var' on variable without initializer)
1 error
```

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| Java is statically typed: every variable's type is fixed at declaration | Declare with a type before use; the compiler checks every assignment |
| A primitive variable holds its value directly | The box *is* the number — no indirection (references arrive in Tier 2) |
| The type is fixed; only the value can change | Reassigning a different type (`int n = "hello"`) is a compile error |
| There are 8 primitives, each with a fixed size and range | An out-of-range value (`byte b = 200`) won't compile; `int`/`double` are the defaults |
| A literal's form fixes its own type | `25` is an `int`, `25L` a `long`, `3.14` a `double`, `'A'` a `char`, `"A"` a `String` |
| `var` infers the type at compile time | Convenience only — the type is still fixed; `var x = 42; x = "hi"` won't compile |

## 7. Gotcha checklist

- **`cannot find symbol … variable X` →** you used `X` without declaring it; add a type: `int X = …;`.
- **`incompatible types: String cannot be converted to int` →** you put the wrong type in a box (or reassigned across types); fix the value, or convert it deliberately.
- **`incompatible types: possible lossy conversion from … ` →** the value is outside the target type's range; use a wider type (`int`, `long`, `double`) or an explicit cast (Tutorial 3).
- **`integer number too large` →** a whole-number literal exceeds `int`; add `L` to make it a `long`, and store it in a `long`.
- **`cannot infer type for local variable` →** `var` with no initializer; give it a starting value on the same line, or write the explicit type.
- **A decimal loses precision unexpectedly →** you may want `double` (the default) instead of `float`; `float`'s 32 bits hold fewer digits (Tutorial 3).

---

*Predict, then check.* Write these four declarations and predict, for each, whether it compiles — and if not, the error's wording: `int a = 5;` · `int b = 5.0;` · `byte c = 5;` · `var d = 5; d = 5.5;`. Two compile and two do not. Decide which, and why, before running them. (Hint: think about each literal's *own* type — `5` versus `5.0` — and which box it is going into.)

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
