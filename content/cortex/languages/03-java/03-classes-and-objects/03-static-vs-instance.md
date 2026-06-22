---
title: static vs Instance
summary: static members belong to the class — one shared copy — while instance members belong to each object. A static method has no this, so it cannot touch instance fields; static final makes shared constants; and a static block initializes class state once, lazily, when the class is first used. Every distinction shown with verified output.
prereqs: []
---

# `static` vs Instance — the Class vs the Object

You've used `static` since `main` and met it again on helper methods; now it earns a precise definition. A **`static`** member belongs to the **class itself** — there is exactly one copy, shared by everything — while an **instance** member belongs to each **object**, with a fresh copy per `new`. That one distinction decides where a value lives, who can see it, and what a method is allowed to touch: a `static` method has no object, hence no `this`, hence no access to instance fields. The same keyword gives you class-wide **constants** (`static final`) and one-time class setup (a `static` block).

This is the deep pass of [the `static` you've used on methods](/cortex/languages/java/control-flow/methods), now set against the [instance state of objects](/cortex/languages/java/classes-and-objects/classes-and-objects). Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [`static` fields are shared](#1-static-fields-are-shared)
2. [`static` vs instance methods](#2-static-vs-instance-methods)
3. [`static final` constants](#3-static-final-constants)
4. [`static` initialization blocks](#4-static-initialization-blocks)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. `static` fields are shared

A `static` field is stored on the **class**, so every object sees the same one. An instance field is stored on each **object**, so each has its own. Here `count` is shared (how many widgets exist) and `id` is per-widget:

```java run
class Widget {
    static int count = 0;   // one copy, shared by all Widgets
    int id;                 // a separate copy in each Widget

    Widget() {
        count++;
        id = count;
    }
}

public class Main {
    public static void main(String[] args) {
        Widget a = new Widget();
        Widget b = new Widget();
        Widget c = new Widget();
        System.out.println("ids: " + a.id + " " + b.id + " " + c.id);
        System.out.println("count: " + Widget.count);
    }
}
```

**Output:**
```
ids: 1 2 3
count: 3
```

```d2
direction: right

cls: "class Widget\nstatic count = 3   (one shared copy)" {
  shape: package
}
a: "a : Widget\nid = 1" { shape: rectangle }
b: "b : Widget\nid = 2" { shape: rectangle }
c: "c : Widget\nid = 3" { shape: rectangle }

a -> cls
b -> cls
c -> cls
```

**Analysis.** Each constructor incremented the one shared `count` (`1`, `2`, `3`) and copied that into its own `id` — so the three widgets have distinct ids `1, 2, 3`, while `count` is a single value, `3`, reached through the class as `Widget.count`. The diagram shows it: three objects each with their own `id`, all pointing at one class that holds the single `count`.

**Intuition.**
*Mechanism.* A `static` field is allocated once, when the class is loaded, and lives on the class. An instance field is allocated with each object, on the object. `Widget.count` names the class's copy; `a.id` names that object's copy.

*Concrete bite.* Because the static field is shared, a change through *any* path is seen by *all* — `count` reached `3` because three different constructors all incremented the same variable. That is the feature (a class-wide tally) and the hazard (shared mutable state that any instance can change).

*Earned rule.* Use a `static` field for state that genuinely belongs to the class as a whole — a counter, a shared cache, a registry — and an instance field for anything that differs per object. The cost of `static` mutable state is exactly its sharing: it is effectively global, so changes from anywhere are visible everywhere, which makes it hard to reason about and unsafe under concurrency (a theme that returns in Tier 5).

---

## 2. `static` vs instance methods

An instance method runs *on an object* and so has access to that object's fields through the implicit `this`. A `static` method runs *on the class* — there is no object, no `this`, and therefore no instance fields to reach.

```java run
class Widget {
    static int count = 0;
    int id;
    Widget() { count++; id = count; }

    void describe() { System.out.println("id " + id + " of " + count); }  // instance: sees both
    static int total() { return count; }                                   // static: only static
}

public class Main {
    public static void main(String[] args) {
        new Widget();
        Widget w = new Widget();
        w.describe();
        System.out.println(Widget.total());
    }
}
```

**Output:**
```
id 2 of 2
2
```

**Analysis.** `describe()` is an instance method called on `w`, so it sees both `w.id` (`2`) and the shared `count` (`2`). `total()` is `static` — called as `Widget.total()` with no object — and it can read `count` because `count` is also `static`, but it has no `id` to read. Instance methods can touch everything; static methods can touch only static members.

**Intuition.**
*Mechanism.* An instance method receives a hidden `this`; an unqualified field name resolves against it. A `static` method has no `this`, so an unqualified *instance* field name has nothing to resolve against — the compiler rejects it.

*Concrete bite.* Reading an instance field from a static method is a compile error:

```java run
class Widget {
    int id = 5;
    static void show() {
        System.out.println(id);
    }
}

public class Main {
    public static void main(String[] args) {
        Widget.show();
    }
}
```

**Compiler error:**
```
Main.java:4: error: non-static variable id cannot be referenced from a static context
        System.out.println(id);
                           ^
1 error
```

`show()` is `static`, so there is no `this` and no particular widget whose `id` to print — "non-static variable `id` … from a static context." (This is the same rule that stopped `Rectangle.area()` last tier: instance state needs an instance.)

*Earned rule.* Make a method `static` when it doesn't depend on any one object's state — a utility that works purely from its arguments — and an instance method when it operates on `this` object's fields. The cost of guessing wrong is a compile error the moment a static method reaches for instance state; the upside is that the signature tells the truth — a `static` method visibly needs no object, so you can call it without making one.

---

## 3. `static final` constants

Combine `static` (one shared copy) with `final` (assigned once, never changed) and you get a **constant**: a single, immutable, class-wide value. Convention names them in `UPPER_SNAKE_CASE`.

```java run
class Circle {
    static final double PI = 3.14159;
    double radius;
    Circle(double radius) { this.radius = radius; }
    double area() { return PI * radius * radius; }
}

public class Main {
    public static void main(String[] args) {
        Circle c = new Circle(2.0);
        System.out.println(c.area());
        System.out.println(Circle.PI);
    }
}
```

**Output:**
```
12.56636
3.14159
```

**Analysis.** `PI` is one shared value (no point giving every circle its own copy of a constant), and `final` means nothing can change it. `area()` reads it like any field; `Circle.PI` reads it through the class. A `static final` is the right home for a fixed value used across all instances.

**Intuition.**
*Mechanism.* `static` gives one copy; `final` forbids reassignment after its single initialization. Together the compiler guarantees a constant: shared and unchangeable, often inlined at compile time for primitives.

*Concrete bite.* Trying to reassign a `static final` is rejected, just like an instance `final`:

```java run
class Config {
    static final int MAX = 100;
    static void raise() { MAX = 200; }
}

public class Main {
    public static void main(String[] args) { }
}
```

**Compiler error:**
```
Main.java:3: error: cannot assign a value to static final variable MAX
    static void raise() { MAX = 200; }
                          ^
1 error
```

`MAX` is `static final`, set once at declaration, so `raise()`'s attempt to change it won't compile. A constant is constant — there is no method that can move it.

*Earned rule.* Use `static final` for fixed, shared values — limits, conversion factors, configuration that never changes at run time — and name them in `UPPER_SNAKE_CASE` so readers know they're constants. The cost is none for truly fixed values; the discipline is reserving it for things that are *actually* constant, since a `static final` you later need to vary forces a real redesign.

---

## 4. `static` initialization blocks

A field initialized with a simple value can be set inline. When a class's `static` state needs *computation* to set up, a **`static` block** — `static { … }` — runs once, when the class is first loaded, to do that work.

```java run
class Lookup {
    static final int[] SQUARES = new int[5];
    static {
        for (int i = 0; i < SQUARES.length; i++) {
            SQUARES[i] = i * i;
        }
        System.out.println("static block ran");
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("main start");
        System.out.println(Lookup.SQUARES[3]);
        System.out.println(Lookup.SQUARES[4]);
    }
}
```

**Output:**
```
main start
static block ran
9
16
```

**Analysis.** Read the order carefully: `main start` printed *first*, and only then did `static block ran` appear — because the `Lookup` class isn't loaded until it's first *used*, which is the moment `main` reaches `Lookup.SQUARES`. At that point the `static` block ran once, filling `SQUARES` (`0, 1, 4, 9, 16`), so the subsequent reads returned `9` and `16`. A `static` block is the constructor for class-level state.

**Intuition.**
*Mechanism.* A class is initialized **lazily** — the first time it is actively used — and its `static` initializers and `static` blocks run then, in source order, exactly **once** per class. Object construction is per-object; class initialization is per-class, one time.

*Concrete bite.* The single `static block ran` line is the proof of "once": `main` touched `Lookup` twice (`SQUARES[3]` and `SQUARES[4]`), yet the block ran a single time. Class initialization does not repeat, no matter how many times the class is used or how many objects you create.

*Earned rule.* Use a `static` block for class-level setup that needs more than a literal — building a lookup table, validating configuration, registering something — and rely on its run-once, lazy-on-first-use timing. The cost is that the *moment* it runs is subtle (first use, not program start), so a `static` block with side effects or ordering dependencies can surprise you; keep them simple and side-effect-light.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| A `static` member belongs to the class; an instance member to each object | One shared `count` vs a per-object `id`; reach statics via `ClassName.x` |
| A `static` method has no `this` | It cannot read instance fields — doing so is a compile error |
| `static final` is a shared, unchangeable constant | Reassigning it won't compile; name it `UPPER_SNAKE_CASE` |
| `static` blocks run once, lazily, on first use of the class | Class init is per-class and one-time, unlike per-object construction |
| Shared mutable `static` state is effectively global | Convenient as a tally/cache, but hard to reason about and unsafe under threads |

## 6. Gotcha checklist

- **`non-static variable … cannot be referenced from a static context` →** a `static` method (or `main`) touched an instance field; make the method non-static, or pass/create an object.
- **A counter or cache behaves "globally" across objects →** it's a `static` field, shared by all instances; use an instance field for per-object state.
- **`cannot assign a value to static final variable` →** you tried to change a constant; `static final` is set once — redesign if it must vary.
- **A `static` block seems to run at the "wrong" time →** classes initialize lazily on first use, not at program start; don't depend on early side effects.
- **You called `ClassName.method()` and it failed →** that method is an instance method needing an object; call it on an instance instead.

---

*Predict, then check.* Predict the output of `new Widget(); new Widget(); System.out.println(Widget.count);` for the §1 class. Next, decide whether a method `static double areaOf(double r)` (using only `r` and `PI`) would compile inside `Circle` — and whether `static double area()` (using the instance field `radius`) would. Finally, predict whether `static block ran` would print if `main` *never* mentioned `Lookup` at all — and explain why.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
