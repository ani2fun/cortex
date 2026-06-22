---
title: equals & hashCode
summary: The default equals/hashCode compare by identity, so two value-equal objects are not "equal" and don't work in hash-based collections. Override equals to define value equality — but you MUST override hashCode to agree, or a HashSet/HashMap silently fails to find your object and stores duplicates. The contract, the breakage, and the Objects.hash fix, all shown with verified output.
prereqs: []
---

# equals & hashCode — the Contract Behind Hash Collections

[The object model](/cortex/languages/java/classes-and-objects/references-equality-and-the-object-model) showed that `==` compares identity and `.equals` compares meaning — *if* the class defines what "meaning" is. By default it does not: a class inherits an `equals` that just checks identity (same object) and a `hashCode` tied to the object's address. So two `Point(1, 2)` objects are *not* equal, and they misbehave in the [hash-based collections](/cortex/languages/java/core-libraries/sets-and-maps) of the last chapter. To fix that you override `equals` to compare values — and here is the trap this chapter exists for: **if you override `equals`, you must override `hashCode` to match, or `HashSet`/`HashMap` will silently fail to find your objects and let duplicates in.** The two are a contract; honoring one without the other is worse than neither.

Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Default `equals` is identity](#1-default-equals-is-identity)
2. [Overriding `equals` for value equality](#2-overriding-equals-for-value-equality)
3. [The `hashCode` contract](#3-the-hashcode-contract)
4. [Generating both correctly](#4-generating-both-correctly)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Default `equals` is identity

A class that defines no `equals` inherits the one from `Object`, which returns `true` only for *the same object*. So two separately created objects with identical fields are not equal:

```java run
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
}

public class Main {
    public static void main(String[] args) {
        Point a = new Point(1, 2);
        Point b = new Point(1, 2);
        System.out.println(a == b);
        System.out.println(a.equals(b));
    }
}
```

**Output:**
```
false
false
```

**Analysis.** `a == b` is `false` because they are distinct objects (the object model's identity rule). But `a.equals(b)` is *also* `false` — the inherited `equals` is just an identity check, so by default `.equals` and `==` agree. The class hasn't said what it means for two points to be "equal," so Java assumes the safest default: only a thing equals itself.

**Intuition.**
*Mechanism.* `Object.equals(o)` is defined as `this == o` — pure identity. Unless a class overrides it, "equal" means "the same object," and value-equal-but-distinct objects compare `false`.

*Concrete bite.* The second `false` is the surprise: people expect `.equals` to compare contents, but for a class that didn't override it, `.equals` is identity too. A `List.contains`, a `Map` key lookup, a deduplicating `Set` — all use `.equals`, so all of them treat your two equal points as different.

*Earned rule.* If a class represents a *value* (a point, a money amount, a name) and two instances with the same fields should count as equal, you must override `equals` — the default identity behavior is correct only for objects whose identity *is* their meaning. The cost is writing (and maintaining) the method; the benefit is that equality means what your domain means, not just "same allocation."

---

## 2. Overriding `equals` for value equality

To define value equality, override `equals(Object o)`: check the argument is a `Point`, then compare the fields. (`o instanceof Point` tests the type; `(Point) o` casts it — the cast lets you reach the fields; pattern-matching `instanceof` in Tutorial 26 fuses the two.)

```java run
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }
}

public class Main {
    public static void main(String[] args) {
        Point a = new Point(1, 2);
        Point b = new Point(1, 2);
        System.out.println(a == b);
        System.out.println(a.equals(b));
    }
}
```

**Output:**
```
false
true
```

**Analysis.** Now `a.equals(b)` is `true` — the overridden method compared `x` and `y`, which match. `a == b` is still `false`, because `==` is identity and these remain two different objects. We've split the two notions cleanly: `==` for "same object," `.equals` for "same value." The `@Override` annotation asks the compiler to confirm we're really overriding `Object.equals` (correct signature: parameter type `Object`).

**Intuition.**
*Mechanism.* Overriding `equals` replaces identity comparison with your value comparison wherever `.equals` is called — including deep inside the collections. The `instanceof` guard also handles `null` and wrong types (returning `false`), satisfying `equals`'s contract that it never throws on a bad argument.

*Concrete bite.* A subtle slip: writing `public boolean equals(Point o)` (parameter `Point`, not `Object`) does *not* override `Object.equals` — it **overloads** it, so collections (which call `equals(Object)`) ignore your version and fall back to identity. The `@Override` annotation catches this at compile time; without it the bug is silent.

*Earned rule.* Override `equals(Object o)` (exact signature, with `@Override`) to compare the fields that define value-equality, guarding the type with `instanceof`. The cost is care with the signature and the fields you include; the benefit is value semantics everywhere `.equals` is used — but only if you also fix `hashCode`, which is the contract the next section enforces.

---

## 3. The `hashCode` contract

Here is the rule that makes or breaks hash collections: **if `a.equals(b)` is true, then `a.hashCode()` must equal `b.hashCode()`.** Override `equals` but leave `hashCode` as the inherited identity-based one, and equal objects get *different* hash codes — so a `HashSet`/`HashMap` looks in the wrong bucket and your object vanishes.

```java run
import java.util.HashSet;
import java.util.Set;

class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }
    // hashCode NOT overridden — still identity-based
}

public class Main {
    public static void main(String[] args) {
        Set<Point> set = new HashSet<>();
        set.add(new Point(1, 2));
        System.out.println(set.contains(new Point(1, 2)));
        set.add(new Point(1, 2));
        System.out.println(set.size());
    }
}
```

**Output:**
```
false
2
```

```d2
direction: right

p1: "Point(1,2) — object A\nidentity hashCode → 7" {
  shape: oval
}
p2: "Point(1,2) — object B\nidentity hashCode → 42" {
  shape: oval
}
buckets: "HashSet buckets" {
  grid-rows: 4
  b0: "0:  (empty)"
  b1: "1:  A"
  b2: "2:  B"
  b3: "3:  (empty)"
}

p1 -> buckets.b1: "lands in bucket 1"
p2 -> buckets.b2: "lands in bucket 2"
```

**Analysis.** This is broken in two visible ways. `contains(new Point(1, 2))` is `false` — the set computed the lookup point's (identity) hash code, went to *that* bucket, and didn't find the stored point, which lives in a *different* bucket. And `add`ing a second equal point grew the size to `2` — the set put it in yet another bucket without ever discovering it was a duplicate. The diagram shows the cause: value-equal points scatter to different buckets because identity `hashCode` ignores the fields. The `equals` method was never even consulted, because the set never looked in the right bucket.

**Intuition.**
*Mechanism.* A hash collection finds an object in two steps: hash to a bucket, then `.equals` within that bucket. If equal objects have unequal hash codes, step one sends them to different buckets, so step two never runs — `equals` is correct but unreachable. Overriding `equals` without `hashCode` breaks the *first* step.

*Concrete bite.* The `false` and the `2` are the breakage: a present element reports absent, and a duplicate slips in. This is among the most insidious Java bugs because the class *looks* right (`equals` works in isolation) and only fails inside hash collections.

*Earned rule.* Whenever you override `equals`, override `hashCode` in the same change so that equal objects produce equal hashes — never one without the other. The cost is a second method; the cost of skipping it is a class that passes `a.equals(b)` tests yet silently corrupts every `HashSet`/`HashMap` it's used in.

---

## 4. Generating both correctly

You rarely write `hashCode` by hand. `Objects.hash(...)` builds a hash from the same fields `equals` uses, and `Objects.equals(a, b)` null-safely compares fields. Use exactly the fields in both methods, and the contract holds.

```java run
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

public class Main {
    public static void main(String[] args) {
        Set<Point> set = new HashSet<>();
        set.add(new Point(1, 2));
        System.out.println(set.contains(new Point(1, 2)));
        set.add(new Point(1, 2));
        System.out.println(set.size());
    }
}
```

**Output:**
```
true
1
```

**Analysis.** With `hashCode` derived from `x` and `y`, two equal points now hash to the *same* bucket — so `contains` finds the stored point (`true`), and adding a duplicate is recognized and dropped (`size` stays `1`). The collection works because the contract holds: equal points → equal hashes → same bucket → `equals` confirms the match.

**Intuition.**
*Mechanism.* `Objects.hash(x, y)` combines the fields into one `int` deterministically, so equal field-values yield equal hashes; `equals` then confirms equality within the bucket. The two methods must use the *same fields* — a field in `equals` but not `hashCode` re-breaks the contract.

*Concrete bite.* The fix is exactly the inverse of §3's breakage: `true` and `1` instead of `false` and `2`. The only change was adding a `hashCode` consistent with `equals` — proof that the broken behavior was the missing `hashCode`, nothing else.

*Earned rule.* Generate `equals` and `hashCode` together from the same fields (`Objects.equals`/`Objects.hash`, or your IDE's generator), and keep them in sync when fields change. The cost is boilerplate that must stay consistent; the benefit is correctness in every hash collection — and Tutorial 21's `record` removes the cost entirely by generating both (and `toString`) for you from the components.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| The default `equals`/`hashCode` are identity-based | Value-equal objects compare `false` and don't dedup in collections |
| Override `equals(Object)` (with `@Override`) for value equality | `equals(Point)` only overloads — collections ignore it; `==` stays identity |
| Contract: equal objects must have equal hash codes | `equals` without `hashCode` breaks `HashSet`/`HashMap` |
| A hash collection hashes to a bucket, then `.equals` within it | Wrong hash → wrong bucket → present object reported absent, duplicates added |
| Generate both from the same fields (`Objects.hash`/`equals`) | Consistent hash + equality; a `record` generates them for free |

## 6. Gotcha checklist

- **A `HashSet`/`HashMap` can't find an object it contains →** you overrode `equals` but not `hashCode`; add a `hashCode` over the same fields.
- **Duplicates appear in a `Set` of "equal" objects →** same cause — inconsistent `hashCode`; override it to match `equals`.
- **Your `equals` is "ignored" by collections →** the signature is `equals(YourType)`, not `equals(Object)` — it overloads, not overrides; add `@Override` to catch it.
- **`equals` and `hashCode` use different fields →** they must use the *same* fields, or equal objects can hash differently.
- **Lots of boilerplate to keep in sync →** use `Objects.equals`/`Objects.hash`, your IDE's generator, or a `record` (Tutorial 21).

---

*Predict, then check.* For the §1 `Point` (no overrides), predict `new Point(1,2).equals(new Point(1,2))`. Add a correct `equals` but **no** `hashCode`, put one `Point(1,2)` in a `HashSet`, and predict both `contains(new Point(1,2))` and `size()` after adding a second equal point. Then add `hashCode` via `Objects.hash(x, y)` and predict the same two values again. Finally, explain why overriding `equals` with parameter type `Point` (not `Object`) would leave the set still broken even *with* a `hashCode`.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
