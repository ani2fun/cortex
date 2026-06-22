---
title: Nested & Anonymous Classes; Lambdas
summary: Classes can nest inside classes (static nested, or inner with a link to the enclosing instance); an anonymous class implements an interface inline; and a lambda is a compact anonymous implementation of a functional interface — one abstract method — turning behavior into a value you can pass and store. Method references shorten lambdas that just call a method. The bridge from objects to functional style, shown with verified output.
prereqs: []
---

# Nested & Anonymous Classes; Lambdas — Behavior as a Value

So far a method's *behavior* has been fixed where it's written. This chapter makes behavior something you can **pass around**. The path there runs through Java's ways of defining a class in a smaller scope: **nested classes** (a class inside a class), **anonymous classes** (an unnamed class implementing an interface right where it's used), and finally **lambdas** — a compact, anonymous implementation of a **functional interface** (an interface with exactly one abstract method). A lambda turns "what to do" into a value you can store in a variable, hand to a method, and call later. **Method references** shorten the common case of a lambda that just calls an existing method. Together these are the bridge from object-oriented to functional Java — and the foundation for streams in the next tier.

This builds on [methods](/cortex/languages/java/control-flow/methods) and [interfaces](/cortex/languages/java/robust-oop/abstract-classes-and-interfaces). Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Nested classes](#1-nested-classes)
2. [Anonymous classes](#2-anonymous-classes)
3. [Lambdas](#3-lambdas)
4. [Method references](#4-method-references)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Nested classes

A class can be declared inside another. A `static` **nested class** is just a class scoped to its enclosing one. A non-static **inner class** additionally holds a hidden link to an enclosing *instance*, so it can read that instance's fields.

```java run
class Outer {
    private int x = 10;

    static class StaticNested {
        int triple(int n) { return n * 3; }
    }

    class Inner {
        int addX(int n) { return n + x; }   // reads the outer instance's x
    }
}

public class Main {
    public static void main(String[] args) {
        Outer.StaticNested sn = new Outer.StaticNested();
        System.out.println(sn.triple(5));

        Outer outer = new Outer();
        Outer.Inner inner = outer.new Inner();
        System.out.println(inner.addX(5));
    }
}
```

**Output:**
```
15
15
```

**Analysis.** `StaticNested` needs no `Outer` instance — `new Outer.StaticNested()` — and can't see `x`. `Inner` is different: it's created *from* an `Outer` instance (`outer.new Inner()`) and can read that instance's `x` (so `addX(5)` is `5 + 10 = 15`). The `static` keyword on a nested class is exactly the same idea as on a field or method: `static` means "no enclosing instance," non-static means "tied to one."

**Intuition.**
*Mechanism.* An inner (non-static) class instance carries a reference to its enclosing instance, which is how it reaches `x`. A `static` nested class carries no such reference — it's a top-level class that merely lives in another's namespace.

*Concrete bite.* The practical rule: default a nested class to `static` unless it genuinely needs the enclosing instance. A non-static inner class keeps its outer object alive (a hidden reference), which can cause surprising memory retention; `static` avoids that link entirely.

*Earned rule.* Nest a helper class inside the class it serves to keep it scoped and private, and make it `static` unless it must access enclosing instance state. The cost of a non-static inner class is the hidden outer reference (memory and coupling); the benefit of nesting is locality — the helper lives exactly where it's used, not as a separate top-level file.

---

## 2. Anonymous classes

To implement an [interface](/cortex/languages/java/robust-oop/abstract-classes-and-interfaces) just once, right where you need it, you can write an **anonymous class** — `new Interface() { ... }` — which defines and instantiates an unnamed implementing class in one expression.

```java run
interface Greeter { String greet(String name); }

public class Main {
    public static void main(String[] args) {
        Greeter g = new Greeter() {
            @Override
            public String greet(String name) { return "Hello, " + name; }
        };
        System.out.println(g.greet("Ada"));
    }
}
```

**Output:**
```
Hello, Ada
```

**Analysis.** `new Greeter() { ... }` created an object of an unnamed class that implements `Greeter`, supplying `greet` inline. We never declared a named `class` — the implementation exists only as this one object. This is how Java passed behavior before lambdas: wrap it in an anonymous class implementing an interface.

**Intuition.**
*Mechanism.* The compiler generates a hidden class implementing the interface and instantiates it. The anonymous class can capture (read) effectively-final local variables from the enclosing scope, which is what makes it a self-contained bundle of behavior.

*Concrete bite.* The ceremony is the problem: four lines (`new Greeter() { @Override public String greet... }`) to express one line of actual logic. For an interface with a *single* method, almost all of that is noise — which is precisely what a lambda removes.

*Earned rule.* Reach for an anonymous class when you need a one-off implementation that has *multiple* methods or needs its own fields; otherwise prefer the lambda in the next section. The cost of an anonymous class is verbosity and a distinct generated class; the benefit is a complete inline implementation when a single-expression lambda isn't enough.

---

## 3. Lambdas

When the interface has exactly **one** abstract method — a **functional interface** — a **lambda** expresses the implementation as just `parameters -> body`. It's the anonymous class of §2 with all the ceremony removed, and it makes behavior a value.

```java run
interface Greeter { String greet(String name); }

public class Main {
    public static void main(String[] args) {
        Greeter g = name -> "Hello, " + name;
        System.out.println(g.greet("Ada"));

        Runnable r = () -> System.out.println("running");
        r.run();
    }
}
```

**Output:**
```
Hello, Ada
running
```

**Analysis.** `name -> "Hello, " + name` is a `Greeter` — exactly the anonymous class from §2, written as one expression. The compiler infers that `name` is the parameter of `greet` and the expression is its return value. `Runnable` (a built-in functional interface, `void run()`) works the same way with `()` for no parameters. The lambda *is* an object you stored in `g` and `r` and called later — behavior as a value.

**Intuition.**
*Mechanism.* A lambda is an implementation of a functional interface — one abstract method — so the compiler knows exactly which method the lambda body defines. The parameter types and return are inferred from that single method's signature.

*Concrete bite.* "Exactly one abstract method" is a hard requirement: a lambda can't target an interface with two:

```java run
interface TwoMethods { void a(); void b(); }

public class Main {
    public static void main(String[] args) {
        TwoMethods t = () -> System.out.println("?");
    }
}
```

**Compiler error:**
```
Main.java:4: error: incompatible types: TwoMethods is not a functional interface
        TwoMethods t = () -> System.out.println("?");
                       ^
    multiple non-overriding abstract methods found in interface TwoMethods
```

`TwoMethods` has two abstract methods, so a single lambda body can't say which it implements — "not a functional interface." A lambda needs a one-method target (mark your own with `@FunctionalInterface` to enforce it); for more than one method, use an anonymous class.

*Earned rule.* Use a lambda to implement a functional interface compactly — passing behavior to `sort`, `forEach`, a callback, a strategy. The cost is that it works only for single-method interfaces (and captures only effectively-final locals); the benefit is enormous expressiveness: logic becomes a value you store and pass, which is the entire premise of the Streams API in Tutorial 28.

---

## 4. Method references

A lambda that does nothing but call one existing method can be written even shorter as a **method reference**: `Type::method`. It names the method directly instead of wrapping it in `x -> x.method()`.

```java run
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("Charlie", "alice", "Bob"));
        names.sort(String::compareToIgnoreCase);
        System.out.println(names);
        names.forEach(System.out::println);
    }
}
```

**Output:**
```
[alice, Bob, Charlie]
alice
Bob
Charlie
```

**Analysis.** `names.sort(String::compareToIgnoreCase)` sorted case-insensitively — `String::compareToIgnoreCase` is a method reference standing in for the lambda `(a, b) -> a.compareToIgnoreCase(b)`, used as a `Comparator`. `names.forEach(System.out::println)` printed each name — `System.out::println` is the lambda `s -> System.out.println(s)`. Both are just shorter spellings of lambdas that delegate to one method.

**Intuition.**
*Mechanism.* A method reference is sugar for a lambda whose body is a single method call; the compiler matches the referenced method's shape to the functional interface's method. The forms — `Type::staticMethod`, `Type::instanceMethod`, `object::instanceMethod`, `Type::new` — all expand to the equivalent lambda.

*Concrete bite.* They read as the *intent*: `sort(String::compareToIgnoreCase)` says "sort by case-insensitive comparison" with no boilerplate parameters. The boundary is that a method reference only works when the lambda is *exactly* one call with matching arguments — any extra logic (a transform, a condition) needs a full lambda.

*Earned rule.* Prefer a method reference when a lambda's whole body is one method call with matching arguments (`String::toUpperCase`, `System.out::println`, `Objects::nonNull`); use a full lambda when there's any additional logic. The cost is learning the four reference forms; the benefit is code that names the operation directly — maximally concise and readable, especially in the stream pipelines ahead.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| A `static` nested class has no enclosing instance; an inner class does | Inner classes read the outer instance's fields (and hold a hidden reference to it) |
| An anonymous class implements an interface inline, unnamed | Good for one-off multi-method implementations; verbose for one method |
| A lambda implements a functional interface (one abstract method) | `params -> body` is behavior as a value you store, pass, and call |
| A lambda's target must have exactly one abstract method | Two methods → "not a functional interface"; mark yours `@FunctionalInterface` |
| A method reference replaces a lambda that just calls one method | `String::toUpperCase`, `System.out::println` — names the operation directly |

## 6. Gotcha checklist

- **`X is not a functional interface` →** the target interface has more than one abstract method; use an anonymous class, or reduce it to one method.
- **A lambda can't change a local variable it uses →** captured locals must be effectively final; use a field or an array/holder if you must mutate.
- **A nested class holds its outer object alive unexpectedly →** it's a non-static inner class; make it `static` if it doesn't need the enclosing instance.
- **A method reference won't compile where a lambda would →** the method's shape doesn't match the interface, or there's extra logic; write the full lambda.
- **Reached for an anonymous class for a one-method interface →** a lambda is shorter and clearer; reserve anonymous classes for multi-method or stateful cases.

---

*Predict, then check.* Write a functional interface `IntOp { int apply(int a, int b); }` and predict what `IntOp add = (a, b) -> a + b; System.out.println(add.apply(3, 4));` prints. Next, rewrite the §2 anonymous `Greeter` as a lambda and confirm identical output. Finally, predict the output of sorting `["banana","Apple","cherry"]` with `String::compareToIgnoreCase` then printing with `forEach(System.out::println)`, and explain what lambda each method reference stands for.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
