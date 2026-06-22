---
title: Modern Java Idioms & the Type System
summary: The modern features aren't separate tricks — they compose into one design. records (immutable data) + sealed (closed type sets) + pattern matching (consume by shape) give data-oriented programming with compiler-checked exhaustiveness; Optional replaces null at boundaries; immutability and var keep code safe and concise. A holistic synthesis of Tiers 3–5, shown with verified output.
prereqs: []
---

# Modern Java Idioms & the Type System, Holistically

You've met the modern features one at a time — [records and sealed types](/cortex/languages/java/core-libraries/enums-and-records), [pattern matching](/cortex/languages/java/robust-oop/sealed-classes-and-pattern-matching), [`Optional` and streams](/cortex/languages/java/advanced/functional-java-and-streams), `var`. This chapter shows they're not a grab-bag of tricks but a **coherent design**. Together, **`record` + `sealed` + pattern matching** give *data-oriented programming*: model your data as a closed set of immutable shapes, and let the compiler force you to handle every one. **`Optional`** pushes `null` out of your domain at the edges. **Immutability** makes objects safe to share, and **`var`** removes redundant noise. The thesis is composition: each feature is good alone, but their real power is how they fit together into code that is concise, safe, and checked.

This synthesizes Tiers 3–5. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Data-oriented design: records + sealed + patterns](#1-data-oriented-design-records--sealed--patterns)
2. [`Optional` over `null`](#2-optional-over-null)
3. [Immutability and `var`](#3-immutability-and-var)
4. [Composing it all](#4-composing-it-all)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Data-oriented design: records + sealed + patterns

The three features lock together: a **`sealed`** interface defines a closed set of cases, each case is an immutable **`record`**, and a **pattern `switch`** consumes them — destructuring components and, because the set is closed, checked for exhaustiveness with no `default`.

```java run
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

public class Main {
    static double area(Shape s) {
        return switch (s) {
            case Circle(double r) -> Math.PI * r * r;
            case Rectangle(double w, double h) -> w * h;
            case Triangle(double b, double h) -> 0.5 * b * h;
        };
    }

    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Rectangle(3, 4), new Triangle(6, 8) };
        for (Shape s : shapes) {
            System.out.printf("%s -> %.2f%n", s.getClass().getSimpleName(), area(s));
        }
    }
}
```

**Output:**
```
Circle -> 12.57
Rectangle -> 12.00
Triangle -> 24.00
```

```d2
direction: down

record: "record\nimmutable data shape" { shape: rectangle }
sealed: "sealed\nclosed set of cases" { shape: rectangle }
pattern: "pattern switch\nconsume + destructure" { shape: rectangle }
exhaustive: "exhaustiveness\ncompiler-checked" { shape: rectangle }

dop: "data-oriented design" {
  shape: package
}

record -> dop: "the shapes"
sealed -> dop: "the closed set"
pattern -> dop: "the operations"
dop -> exhaustive: "guarantees"
```

**Analysis.** Each shape is a one-line `record` (data with generated `equals`/`hashCode`/`toString`); `sealed` declares the *complete* set; and the pattern `switch` destructures each (`Circle(double r)`) to compute its area — with **no `default`**, because the compiler knows the set is closed. The data lives in the records, the behavior lives in the `switch` (outside the data), and the type system guarantees every case is handled. That's data-oriented programming: a closed algebra of data, with operations added externally.

**Intuition.**
*Mechanism.* `sealed` gives the compiler the full list of subtypes; record patterns bind components directly; the exhaustive `switch` is verified complete. The three features share one purpose — making a closed set of data shapes safe to process.

*Concrete bite.* The payoff is the compiler as a checklist: as [Tutorial 26 verified](/cortex/languages/java/robust-oop/sealed-classes-and-pattern-matching), adding a fourth permitted shape makes *every* non-exhaustive `switch` stop compiling until you handle it — "the switch expression does not cover all possible input values." Compare the [inheritance](/cortex/languages/java/robust-oop/inheritance-and-polymorphism) alternative (an `abstract double area()` per subclass): use polymorphism when behavior travels *with* open subtypes; use sealed + patterns when the type set is *closed* and operations are added from outside.

*Earned rule.* Reach for `record` + `sealed` + pattern `switch` to model closed sets of structured data — results, events, AST nodes, protocol messages — and process them exhaustively. The cost is choosing this over class polymorphism (and maintaining the `permits` list, which the compiler enforces); the benefit is concise, immutable data plus operations the compiler proves complete — a whole class of "forgot a case" bugs eliminated.

---

## 2. `Optional` over `null`

`null` is the [billion-dollar mistake](/cortex/languages/java/classes-and-objects/references-equality-and-the-object-model) — invisible in the type, fatal when dereferenced. The idiom is to keep `null` out of your domain: wrap a maybe-absent value in `Optional` at the boundary, and chain `map`/`filter`/`orElse` to handle absence declaratively.

```java run
import java.util.Optional;
import java.util.Map;

public class Main {
    static Optional<String> lookup(Map<String, String> m, String key) {
        return Optional.ofNullable(m.get(key));
    }

    public static void main(String[] args) {
        Map<String, String> config = Map.of("host", "localhost", "port", "8080");
        String host = lookup(config, "host").map(String::toUpperCase).orElse("UNKNOWN");
        String region = lookup(config, "region").orElse("us-east");
        int port = lookup(config, "port").map(Integer::parseInt).orElse(80);
        System.out.println(host);
        System.out.println(region);
        System.out.println(port);
    }
}
```

**Output:**
```
LOCALHOST
us-east
8080
```

**Analysis.** `lookup` wraps the possibly-`null` `map.get` in `Optional.ofNullable`, so absence becomes a typed `Optional.empty()` — never a raw `null` escaping into the program. The callers then *compose*: `host` was present, so `map(String::toUpperCase)` ran and `orElse` was skipped; `region` was absent, so `map` was skipped and `orElse("us-east")` supplied the default; `port` was present and parsed. No `if (x != null)`, no `NullPointerException` — absence is handled by the chain.

**Intuition.**
*Mechanism.* `Optional` makes "might be absent" part of the *type*, so the compiler forces you to deal with it before extracting a value. `map` transforms only if present; `orElse`/`orElseGet` supply defaults; the value can't be used as if present when it isn't.

*Concrete bite.* The discipline is to convert at the boundary and never let `null` leak inward — `Optional.ofNullable` at the edge, an `Optional` return type for "may not find one." But `Optional` is for *return values*, not fields or parameters (a `null` `Optional` field is the worst of both worlds), and `opt.get()` on empty throws, so it isn't a license to skip handling.

*Earned rule.* Return `Optional<T>` from methods that may find nothing, build it with `ofNullable` at boundaries, and consume it with `map`/`filter`/`orElse`; keep `null` out of your domain logic. The cost is wrapping/unwrapping and discipline about where `Optional` belongs (returns, not fields); the benefit is that "absent" is a checked, composable case instead of a runtime crash.

---

## 3. Immutability and `var`

Modern Java leans on **immutable** objects — `record`s and `final` fields — because an object that can't change is safe to share, cache, and use as a key, with no defensive copies and no aliasing surprises. And **`var`** removes redundant type noise where the right-hand side already says the type.

```java run
import java.util.List;

public class Main {
    record Point(int x, int y) {
        Point translate(int dx, int dy) { return new Point(x + dx, y + dy); }
    }

    public static void main(String[] args) {
        var origin = new Point(0, 0);
        var moved = origin.translate(3, 4);
        System.out.println(origin);
        System.out.println(moved);
        var names = List.of("Ada", "Linus");
        System.out.println(names.size());
    }
}
```

**Output:**
```
Point[x=0, y=0]
Point[x=3, y=4]
2
```

**Analysis.** `Point` is immutable, so `translate` doesn't mutate — it returns a *new* `Point`, leaving `origin` untouched (`Point[x=0, y=0]`) while `moved` is the new value (`Point[x=3, y=4]`). This "transform by producing a new value" is the immutable idiom (like a `String` operation). `var` inferred the types — `Point`, `Point`, `List<String>` — from the initializers, cutting noise without losing the [static type](/cortex/languages/java/first-steps/variables-and-primitive-types) (it's still fully typed).

**Intuition.**
*Mechanism.* An immutable object's state is fixed at construction; "changes" allocate a new object. That makes it inherently thread-safe and alias-safe (nothing can change it behind your back). `var` is pure compile-time inference — the variable has a concrete type, just not spelled out.

*Concrete bite.* Immutability has the cost you'd expect: "updating" deep structures means rebuilding them, and very hot allocation paths may matter — but as [Tutorial 21 verified](/cortex/languages/java/core-libraries/enums-and-records), a `record`'s components are `final` and can't be reassigned, which is exactly what makes sharing safe. And `var` aids brevity, not obscurity: use it when the initializer makes the type obvious (`var p = new Point(...)`), not when it hides a non-obvious type.

*Earned rule.* Default to immutability (`record`s, `final` fields, transform-by-new) for data you share or use as keys, and use `var` where the type is obvious from the right-hand side. The cost is extra allocations and verbosity for deep updates (immutability) and potential opacity if overused (`var`); the benefit is data that's safe to share without defensive copies and code with less redundant ceremony.

---

## 4. Composing it all

The features earn their keep *together*. Here a small domain — payments — uses `sealed` + `record`s for the data, a pattern `switch` for behavior, a [stream](/cortex/languages/java/advanced/functional-java-and-streams) to aggregate, and `Optional` for a query that might find nothing:

```java run
import java.util.List;
import java.util.Optional;

public class Main {
    sealed interface Payment permits Cash, Card {}
    record Cash(double amount) implements Payment {}
    record Card(double amount, String last4) implements Payment {}

    static double fee(Payment p) {
        return switch (p) {
            case Cash c -> 0.0;
            case Card c -> c.amount() * 0.03;
        };
    }
    static double amountOf(Payment p) {
        return switch (p) {
            case Cash c -> c.amount();
            case Card c -> c.amount();
        };
    }

    public static void main(String[] args) {
        List<Payment> payments = List.of(
            new Cash(100), new Card(200, "1234"), new Card(50, "9999"));
        double totalFees = payments.stream().mapToDouble(Main::fee).sum();
        Optional<Payment> biggest = payments.stream()
            .max((a, b) -> Double.compare(amountOf(a), amountOf(b)));
        System.out.printf("total fees: %.2f%n", totalFees);
        System.out.println("biggest: " + biggest.map(p -> p.getClass().getSimpleName()).orElse("none"));
    }
}
```

**Output:**
```
total fees: 7.50
biggest: Card
```

**Analysis.** Five features in one short program: `sealed Payment` (closed set) of `record`s (immutable data); a pattern `switch` computing each payment's `fee` (cash is free, card is 3% — `6.00 + 1.50 = 7.50`); a `stream().mapToDouble(...).sum()` aggregating fees; and `stream().max(...)` returning an `Optional<Payment>` consumed with `map(...).orElse("none")`. Each feature does its job and they compose cleanly — data, behavior, aggregation, and absence-handling, with the compiler checking the switches are exhaustive and `null` nowhere in sight.

**Intuition.**
*Mechanism.* The composition works because the features share a philosophy: make data explicit and immutable (records), make the set of cases closed and checkable (sealed), make operations declarative (patterns, streams), and make absence a type (Optional). They were designed to fit.

*Concrete bite.* This is "data-oriented" Java — declarative, like the [stream pipelines](/cortex/languages/java/advanced/functional-java-and-streams) of a few chapters ago, but with the compiler enforcing exhaustiveness and types throughout. The trade-off is knowing when to use it: this style shines for data-processing and modeling closed domains; classic OOP (encapsulated mutable objects with polymorphic behavior) still fits stateful, open-ended designs.

*Earned rule.* Compose the modern features — sealed records for data, pattern switches and streams for operations, Optional for absence — when modeling and transforming data; reach for classic object-oriented design (mutable state, inheritance) when behavior and identity dominate. The cost is judgment about which paradigm fits; the benefit is that, for the large class of data-shaped problems, modern Java is concise, immutable, and compiler-verified end to end.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| `record` + `sealed` + pattern `switch` = data-oriented design | Immutable data, a closed case set, and compiler-checked exhaustive operations |
| Exhaustiveness over a sealed type is compiler-enforced | Add a case and every non-exhaustive `switch` stops compiling — a built-in checklist |
| `Optional` makes absence a type; keep `null` out of the domain | `ofNullable` at boundaries; `map`/`orElse` to handle absence; not for fields |
| Immutability makes objects safe to share; `var` cuts noise | Transform-by-new; `var` where the initializer makes the type obvious |
| The features compose by shared philosophy | Data-oriented Java is concise and checked; classic OOP fits stateful, open designs |

## 6. Gotcha checklist

- **A pattern `switch` needs a `default` you don't want →** model the cases as a `sealed` type so the compiler proves exhaustiveness without one.
- **`null` leaked deep into the code →** wrap at the boundary with `Optional.ofNullable` and return `Optional`; don't pass `null` around.
- **`Optional` as a field or parameter feels wrong →** it is; `Optional` is for return values — use a nullable field or a real default instead.
- **A "modified" record didn't change →** records are immutable; `translate`-style methods return a *new* record; capture the result.
- **Reached for `var` and the type got unclear →** use `var` only when the initializer makes the type obvious; spell it out otherwise.

---

*Predict, then check.* Add a `Pentagon(double side)` to the §1 `Shape` hierarchy and predict what the compiler says about `area` until you add its case. Next, predict the three lines printed by §2 if `config` also contained `"port" -> "abc"` and you parsed it with `.map(Integer::parseInt)` — would `orElse(80)` save you? Finally, extend §4 to also print the *count* of `Card` payments using a stream `filter` with a pattern (`p instanceof Card`), and predict the result.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
