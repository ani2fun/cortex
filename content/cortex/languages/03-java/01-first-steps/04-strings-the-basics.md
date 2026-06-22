---
title: Strings, the Basics
summary: A String is an immutable object, not a primitive — so every "change" makes a new String, methods that ignore their result do nothing, and comparing text needs .equals (characters) not == (identity), a distinction the String pool makes deceptively easy to get wrong. Creating strings, everyday methods, concatenation, and a first look at == vs equals.
prereqs: []
---

# Strings, the Basics — Text as an Immutable Object

A **String** is a piece of text — characters in double quotes, like `"Ada"`. But unlike the [primitives of the last chapter](/cortex/languages/java/first-steps/variables-and-primitive-types), a String is an **object**, and two consequences of that flow through everything you do with text. First, a String is **immutable**: it can never be changed, so every method that seems to edit it actually returns a *new* String. Second, because a String is an object, comparing two strings means choosing between two questions — "the same object?" (`==`) or "the same characters?" (`.equals`) — and choosing wrong is one of the most common beginner bugs in Java.

This is a gentle first pass. The reference model under "object" gets its rigorous treatment in Tier 2, and Strings return in depth in Tier 3. For now, two ideas carry the chapter: a String is immutable, and you compare its contents with `.equals`. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Strings are objects made of characters](#1-strings-are-objects-made-of-characters)
2. [Immutability: methods return new strings](#2-immutability-methods-return-new-strings)
3. [Everyday string methods](#3-everyday-string-methods)
4. [Concatenation with `+`](#4-concatenation-with-)
5. [`==` vs `.equals` and the String pool](#5--vs-equals-and-the-string-pool)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Strings are objects made of characters

You write a String with **double** quotes. Because it is an object, it carries **methods** — actions you call on it with a dot, like `name.length()`.

```java run
public class Main {
    public static void main(String[] args) {
        String name = "Ada";
        System.out.println(name);
        System.out.println(name.length());
        char first = name.charAt(0);
        System.out.println(first);
    }
}
```

**Output:**
```
Ada
3
A
```

**Analysis.** `"Ada"` is a String — a sequence of characters, and an object. Being an object, it answers method calls: `name.length()` asks how many characters it has (`3`), and `name.charAt(0)` asks for the character at position `0` — the *first*, because counting starts at zero — which is `'A'`. Note the two kinds of quote from the last chapter: `charAt` returns a `char` (`'A'`, single quotes, one character), not a one-character String (`"A"`, double quotes).

**Intuition.**
*Mechanism.* A String variable is a **reference type**: `name` holds a reference to a String object stored elsewhere, rather than holding the characters directly the way an `int` holds its number. (The full reference model is Tier 2; here it is enough that a String is an object you send method calls to.)

*Concrete bite.* Positions run from `0` to `length − 1`, so reaching past the end is a run-time error:

```java run
public class Main {
    public static void main(String[] args) {
        String name = "Ada";
        System.out.println(name.charAt(3));
    }
}
```

**Output** *(a thrown exception; a stack trace follows the first line):*
```
Exception in thread "main" java.lang.StringIndexOutOfBoundsException: Index 3 out of bounds for length 3
```

`"Ada"` has length `3`, so the valid positions are `0`, `1`, `2`; `charAt(3)` is one past the end and throws.

*Earned rule.* Index strings from `0` to `length − 1`; `charAt(length)` is always off the end. The cost of a String being an object is that an out-of-range access fails at *run time*, with an exception, not at compile time — the compiler cannot know the index in advance. When the position comes from a calculation, check it against `length()` first.

---

## 2. Immutability: methods return new strings

A String never changes. Methods that look like they edit it — `toUpperCase`, `replace`, `strip` — actually return a **new** String and leave the original untouched. That property is called **immutability**.

```java run
public class Main {
    public static void main(String[] args) {
        String greeting = "hello";
        String shout = greeting.toUpperCase();
        System.out.println(shout);
        System.out.println(greeting);
    }
}
```

**Output:**
```
HELLO
hello
```

**Analysis.** `toUpperCase()` produced a *new* String, `"HELLO"`, which we stored in `shout`. The original `greeting` is still `"hello"`. The method did not edit the string in place; it built a new one and returned it.

**Intuition.**
*Mechanism.* Every String operation that "transforms" text returns a brand-new String object; the original's characters are fixed for its entire life. No method mutates a String in place, because none can.

*Concrete bite.* So calling such a method and ignoring its result does nothing at all:

```java run
public class Main {
    public static void main(String[] args) {
        String greeting = "hello";
        greeting.toUpperCase();   // result thrown away!
        System.out.println(greeting);
    }
}
```

**Output:**
```
hello
```

`toUpperCase()` ran and produced `"HELLO"`, but nothing caught the result, so it was discarded; `greeting` was never going to change. To keep the new value you must assign it: `greeting = greeting.toUpperCase();`.

*Earned rule.* Treat every String method as returning a new value you must capture — `s = s.strip();`, not `s.strip();`. The cost of immutability is exactly this gotcha (a method call that appears to do nothing) plus extra objects when you transform text repeatedly. The benefit is safety: a String you hand to other code can never be altered behind your back, which is what makes Strings dependable as constants and as map keys — a payoff you'll collect in Tier 3.

---

## 3. Everyday string methods

Strings come with a deep toolkit. A working handful covers most days: `length`, `charAt`, `substring`, `indexOf`, `contains`, `replace`, `toUpperCase`/`toLowerCase`, and `strip` (remove surrounding whitespace; `trim` is its older sibling).

```java run
public class Main {
    public static void main(String[] args) {
        String s = "  Hello, World  ";
        System.out.println(s.strip());
        System.out.println(s.strip().length());
        System.out.println("Hello, World".indexOf("World"));
        System.out.println("Hello, World".substring(7));
        System.out.println("Hello, World".replace("World", "Java"));
        System.out.println("Hello, World".contains("ello"));
    }
}
```

**Output:**
```
Hello, World
12
7
World
Hello, Java
true
```

**Analysis.** `strip()` removed the surrounding spaces, giving `"Hello, World"` (length `12`). `indexOf("World")` reported the position where `"World"` begins — `7`. `substring(7)` returned everything from position 7 onward, `"World"`. `replace` produced `"Hello, Java"`, and `contains("ello")` answered `true`. Notice methods **chain**: `s.strip().length()` runs `strip` first (a new String), then `length` on that result.

**Intuition.**
*Mechanism.* Each method reads the String and returns a result — a new String, an `int` index, or a `boolean` — without altering the original (immutability again). `indexOf` returns the position, or `-1` when the text is not found.

*Concrete bite.* That `-1` is a value, not an error, and it bites if you assume the search succeeded:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello".indexOf("z"));
    }
}
```

**Output:**
```
-1
```

`"z"` is not in `"Hello"`, so `indexOf` returns `-1` — a sentinel, not an exception. Hand that `-1` to `substring` or `charAt` and *that* call throws; the search itself quietly gave you a value you were supposed to check.

*Earned rule.* Check `indexOf`'s result against `-1` before using it as a position. The cost of a method that returns a sentinel rather than throwing is a *delayed* failure: you learn nothing is wrong at the `indexOf` call, only later when `-1` reaches code that cannot handle it. (Tier 5's `Optional` is the modern, typed alternative to sentinel return values.)

---

## 4. Concatenation with `+`

`+` between two strings joins them; `+` between a string and a number turns the number into text and joins. It is the quickest way to build output — and it hides a precedence surprise.

```java run
public class Main {
    public static void main(String[] args) {
        String name = "Ada";
        int age = 36;
        System.out.println("name: " + name + ", age: " + age);
        System.out.println("sum: " + 1 + 2);
        System.out.println(1 + 2 + " = sum");
    }
}
```

**Output:**
```
name: Ada, age: 36
sum: 12
3 = sum
```

**Analysis.** The first line joins text with `name` and `age` (the `int` `36` becomes `"36"`). The next two look symmetric but differ, because `+` is evaluated **left to right** and, once one side is a String, `+` means "join." In `"sum: " + 1 + 2`, the first `+` joins `"sum: "` and `1` into `"sum: 1"`, then `+ 2` joins to `"sum: 12"` — the numbers were never added. In `1 + 2 + " = sum"`, the first `+` is `int + int = 3` (no String yet), and only then does `3 + " = sum"` join into `"3 = sum"`.

**Intuition.**
*Mechanism.* `+` is left-associative; each `+` looks only at its immediate left and right. If either side is a String, it concatenates (converting the other side to text); only when both sides are numbers does it add.

*Concrete bite.* The `"sum: " + 1 + 2 → "sum: 12"` above is the bite — a "sum" that never added. Parenthesise the arithmetic to fix it:

```java run
public class Main {
    public static void main(String[] args) {
        System.out.println("sum: " + (1 + 2));
    }
}
```

**Output:**
```
sum: 3
```

The parentheses force `1 + 2 = 3` first, then the concatenation.

*Earned rule.* When you mix text and arithmetic with `+`, wrap the arithmetic in parentheses — `"sum: " + (1 + 2)`. The cost of `+`'s double meaning is this ordering trap; and there is a second cost — building long text with `+` inside a loop creates a new String on every step, which is quadratic work. Tier 3's `StringBuilder` is the linear-time fix, and the spot where this chapter's "every change makes a new String" stops being a curiosity and starts mattering for speed.

---

## 5. `==` vs `.equals` and the String pool

To compare strings you have two tools that look interchangeable but ask different questions. `==` asks "are these the **same object**?" `.equals` asks "do these have the **same characters**?" For text you nearly always mean the second.

```java run
public class Main {
    public static void main(String[] args) {
        String a = "hello";
        String b = "hello";
        String c = new String("hello");
        System.out.println(a.equals(b));
        System.out.println(a.equals(c));
        System.out.println(a == b);
        System.out.println(a == c);
    }
}
```

**Output:**
```
true
true
true
false
```

**Analysis.** `.equals` is `true` for both comparisons — `a`, `b`, and `c` all spell `"hello"`. But `==` disagrees: `a == b` is `true` while `a == c` is `false`. The cause is the **String pool**: identical string *literals* like the `"hello"` written for `a` and `b` are stored once and shared, so `a` and `b` are the *same* object and `==` happens to be true. `new String("hello")` deliberately builds a *separate* object, so `a == c` is false even though the characters match.

**Intuition.**
*Mechanism.* A String variable holds a reference. `==` compares the two references — same object? — while `.equals` compares the characters. The pool reuses literal strings, which makes `==` *coincidentally* true for literals: the most dangerous kind of "it works."

*Concrete bite.* The coincidence collapses the moment a string comes from anywhere but a literal — a `new String`, user input, a computation:

```java run
public class Main {
    public static void main(String[] args) {
        String typed = new String("yes");
        System.out.println(typed == "yes");
        System.out.println(typed.equals("yes"));
    }
}
```

**Output:**
```
false
true
```

The text is `"yes"` both ways, but `==` compared object identities — two different objects — and was `false`, while `.equals` compared characters and was `true`. A program that checks user input with `==` will reject correct answers seemingly at random, because typed-in or computed strings are not pooled.

*Earned rule.* Compare String *contents* with `.equals` (or `.equalsIgnoreCase`); reserve `==` for primitives and deliberate identity checks. The cost of the pool is that `==` *looks* right in quick tests with literals and then fails on real, non-literal strings — so never let a passing literal test convince you `==` is correct for text. This is one face of a deeper rule about objects: references and equality get their full pass in Tier 2, and the `equals`/`hashCode` contract behind hash-based collections in Tier 3.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| A String is an object (a reference type), not a primitive | It has methods (`.length()`, `.charAt(i)`); out-of-range access throws at run time |
| Strings are immutable | Every "edit" returns a new String; `s.strip();` alone changes nothing — assign it |
| String methods return values without mutating | `indexOf` returns a position or `-1`; check for `-1` before using it |
| `+` is left-associative and means "join" once a String is involved | `"sum: " + 1 + 2` is `"sum: 12"`; parenthesise arithmetic: `"sum: " + (1 + 2)` |
| `==` compares identity; `.equals` compares characters | `==` is true for shared literals, false for `new String`/input — use `.equals` |

## 7. Gotcha checklist

- **A string "edit" didn't take →** the method returned a new String you ignored; assign it back (`s = s.toUpperCase();`).
- **`StringIndexOutOfBoundsException` →** an index reached `length` or beyond; valid positions are `0 … length − 1`; check against `length()`.
- **A `+` "sum" concatenated instead of adding →** left-to-right `+` joined once a String appeared; wrap the arithmetic: `... + (a + b)`.
- **String comparison with `==` works in tests, fails in production →** you compared identity; literals are pooled but real input is not — use `.equals`.
- **`substring`/`charAt` throws on a search result →** `indexOf` returned `-1` (not found) and you used it as a position; test for `-1` first.

---

*Predict, then check.* Predict the four lines of output before running this: `String x = "java"; String y = "ja" + "va"; String z = new String("java");` then print `x == y`, `x.equals(y)`, `x == z`, and `x.equals(z)`. (Hint: `"ja" + "va"` is two *literals* joined by the compiler before the program runs — does that land it in the pool with `x`, or make a separate object like `z`?) Decide each `true`/`false`, then run it and reconcile any surprise with the rule that `==` compares identity while `.equals` compares characters.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
