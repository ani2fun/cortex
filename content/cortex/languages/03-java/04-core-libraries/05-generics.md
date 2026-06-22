---
title: Generics
summary: Generics parameterize code over a type, so List<String> guarantees only Strings at compile time. Generic classes and methods, bounded types (T extends Comparable) that unlock the bound's methods, wildcards (? extends / ? super, PECS) for flexible APIs, and type erasure — generics are compile-time only, so List<String> and List<Integer> are one class at run time. Every behavior and limit shown with verified output.
prereqs: []
---

# Generics — Type Safety Without Casts

You've used generics since [the collections](/cortex/languages/java/core-libraries/the-collections-framework): `List<Integer>` is "a list of `Integer`," and the `<Integer>` is a **type parameter**. Generics let a class or method work over *a* type the caller chooses, while the compiler enforces it — so `List<String>` accepts only `String`s and returns `String`s with no casting. The catch, and the second half of this chapter, is that generics are a **compile-time** device: the JVM erases them, so at run time `List<String>` and `List<Integer>` are the *same* `List`. That **type erasure** is what makes generics free, and also what they cannot do (`new T[]`, `instanceof List<String>`).

Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Generic classes: type parameters](#1-generic-classes-type-parameters)
2. [Generic methods and bounded types](#2-generic-methods-and-bounded-types)
3. [Wildcards and PECS](#3-wildcards-and-pecs)
4. [Type erasure](#4-type-erasure)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Generic classes: type parameters

A class can take a **type parameter** — written `<T>` — and use `T` as a stand-in type throughout. The caller picks the real type at `new`, and the compiler then enforces it everywhere.

```java run
class Box<T> {
    private T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}

public class Main {
    public static void main(String[] args) {
        Box<String> sb = new Box<>("hello");
        String s = sb.get();
        System.out.println(s.toUpperCase());
        Box<Integer> ib = new Box<>(42);
        System.out.println(ib.get() + 1);
    }
}
```

**Output:**
```
HELLO
43
```

**Analysis.** `Box<String>` made `T` mean `String`, so `get()` returned a `String` — usable as one (`toUpperCase`) with no cast. `Box<Integer>`'s `get()` returned an `Integer`, usable in arithmetic. One class definition, two type-safe instantiations. Before generics, a `Box` would store `Object` and force every `get()` to be cast back, with no compile-time guarantee.

**Intuition.**
*Mechanism.* The compiler substitutes the chosen type for `T` at each use site and type-checks against it. The guarantee is static: a `Box<String>` simply cannot, at compile time, be made to hold or return anything but a `String`.

*Concrete bite.* That guarantee shows up as a compile error the moment you misuse it:

```java run
class Box<T> {
    private T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}

public class Main {
    public static void main(String[] args) {
        Box<String> sb = new Box<>("hi");
        Integer n = sb.get();
        System.out.println(n);
    }
}
```

**Compiler error:**
```
Main.java:9: error: incompatible types: String cannot be converted to Integer
        Integer n = sb.get();
                          ^
1 error
```

`sb.get()` is statically a `String`, so assigning it to an `Integer` is rejected — the type parameter carried `String` all the way to the return type. The error is at compile time, not a `ClassCastException` at run time.

*Earned rule.* Parameterize a container or wrapper with `<T>` so its callers get type safety and skip casts. The cost is a more abstract class definition (and that `T` must be a reference type — `Box<int>` won't compile, only `Box<Integer>`); the benefit is that misuse becomes a compile error instead of a run-time cast failure.

---

## 2. Generic methods and bounded types

A *method* can have its own type parameter, declared before the return type: `<T> T pick(...)`. And a **bound** (`<T extends Something>`) restricts `T` to subtypes of `Something`, which unlocks that type's methods on `T`.

```java run
public class Main {
    static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static void main(String[] args) {
        System.out.println(max(3, 7));
        System.out.println(max("apple", "banana"));
    }
}
```

**Output:**
```
7
banana
```

**Analysis.** `max` works for any `T` that is `Comparable<T>` — so it found the larger `Integer` (`7`) and the later `String` (`"banana"`) with one definition. The bound `T extends Comparable<T>` is what lets the body call `a.compareTo(b)`: without it, the compiler knows nothing about `T` and won't allow any method beyond `Object`'s.

**Intuition.**
*Mechanism.* A bound is a compile-time promise about `T`'s capabilities. `T extends Comparable<T>` tells the compiler every `T` has `compareTo`, so the call type-checks; the caller, in turn, may only pass `Comparable` types.

*Concrete bite.* Drop the bound and the method can't use anything type-specific:

```java run
public class Main {
    static <T> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static void main(String[] args) { }
}
```

**Compiler error:**
```
Main.java:3: error: cannot find symbol
        return a.compareTo(b) >= 0 ? a : b;
                ^
  symbol:   method compareTo(T)
```

With an unbounded `T`, `a` is known only to be an `Object`, which has no `compareTo` — so the call won't compile. The bound isn't decoration; it's what makes `T`'s methods available.

*Earned rule.* Use a generic method when one algorithm applies across many types, and add a bound (`extends`) exactly when the body needs methods beyond `Object`'s. The cost of a bound is narrowing what callers may pass; the benefit is that the body can actually *do* something with `T` — compare it, measure it, call its interface — while staying type-safe.

---

## 3. Wildcards and PECS

`List<Integer>` is **not** a `List<Number>` — generics are *invariant*, so a method taking `List<Number>` rejects a `List<Integer>`. **Wildcards** restore flexibility: `? extends Number` means "some unknown subtype of `Number`," accepting `List<Integer>`, `List<Double>`, and so on.

```java run
import java.util.List;
import java.util.ArrayList;

public class Main {
    static double sum(List<? extends Number> nums) {
        double total = 0;
        for (Number n : nums) total += n.doubleValue();
        return total;
    }

    public static void main(String[] args) {
        List<Integer> ints = new ArrayList<>();
        ints.add(1); ints.add(2); ints.add(3);
        List<Double> dbls = new ArrayList<>();
        dbls.add(1.5); dbls.add(2.5);
        System.out.println(sum(ints));
        System.out.println(sum(dbls));
    }
}
```

**Output:**
```
6.0
4.0
```

**Analysis.** `sum` accepts `List<? extends Number>`, so it summed both a `List<Integer>` (`6.0`) and a `List<Double>` (`4.0`) — every element is *some* `Number`, which is all `sum` needs to call `doubleValue()`. The wildcard widened the parameter from "exactly `List<Number>`" to "a list of any `Number` subtype."

**Intuition.**
*Mechanism.* Without a wildcard, `List<Number>` matches only `List<Number>` (invariance — it has to, or you could put a `Double` into a `List<Integer>`). `? extends Number` is a **producer**: you can *read* `Number`s out, but not safely *add* (the exact element type is unknown). Its mirror, `? super Integer`, is a **consumer**: you can *add* `Integer`s but only read `Object`s — hence the mnemonic **PECS**, *Producer `extends`, Consumer `super`*.

*Concrete bite.* Declare the parameter as the invariant `List<Number>` and the same call is rejected:

```java run
import java.util.List;
import java.util.ArrayList;

public class Main {
    static double sum(List<Number> nums) {
        double total = 0;
        for (Number n : nums) total += n.doubleValue();
        return total;
    }

    public static void main(String[] args) {
        List<Integer> ints = new ArrayList<>();
        ints.add(1);
        System.out.println(sum(ints));
    }
}
```

**Compiler error:**
```
Main.java:12: error: incompatible types: List<Integer> cannot be converted to List<Number>
        System.out.println(sum(ints));
                               ^
```

`List<Integer>` is not a `List<Number>`, so `sum(ints)` won't compile — invariance in action. The `? extends Number` wildcard is exactly the fix.

*Earned rule.* Use `? extends T` for parameters you only **read** from (producers), and `? super T` for parameters you only **write** to (consumers) — PECS. The cost is wildcard syntax and the restriction it implies (a `? extends` list can't be added to); the benefit is APIs that accept the whole family of related generic types instead of one exact match.

---

## 4. Type erasure

Generics exist only at compile time. The compiler checks types and then **erases** them, so at run time there is just `List` — `List<String>` and `List<Integer>` are the *same* class.

```java run
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        List<String> ss = new ArrayList<>();
        List<Integer> is = new ArrayList<>();
        System.out.println(ss.getClass() == is.getClass());
        System.out.println(ss.getClass().getSimpleName());
    }
}
```

**Output:**
```
true
ArrayList
```

**Analysis.** Both lists report the *same* class — `ArrayList` — and `getClass() == getClass()` is `true`: at run time the `<String>` and `<Integer>` are gone. Generics were enforced during compilation and then erased, leaving plain `ArrayList`. This is why generics add no run-time overhead — and why some things are impossible.

**Intuition.**
*Mechanism.* Erasure replaces each type parameter with its bound (or `Object`) and inserts casts where needed, so the bytecode has no generic type information. The type safety was all proven at compile time; nothing remains to check at run time.

*Concrete bite.* Because the type argument is gone at run time, you cannot ask about it — `instanceof List<String>` is meaningless and won't compile:

```java run
import java.util.List;

public class Main {
    static void check(Object obj) {
        if (obj instanceof List<String>) {
            System.out.println("yes");
        }
    }

    public static void main(String[] args) { }
}
```

**Compiler error:**
```
Main.java:4: error: Object cannot be safely cast to List<String>
        if (obj instanceof List<String>) {
            ^
```

At run time there's only `List`, not `List<String>`, so `instanceof List<String>` can't be checked — the compiler rejects it (use the unbounded `instanceof List<?>` if you must test for "a list"). For the same reason you can't write `new T[]` or `new ArrayList<T>[10]` — there's no run-time `T` to allocate.

*Earned rule.* Rely on generics for compile-time safety, and remember the run-time blind spot: no `instanceof Type<Arg>`, no `new T[]`, no reflection on the type argument. The cost of erasure is these gaps (and the occasional "unchecked" warning when bridging legacy code); the benefit is zero run-time overhead and full backward compatibility with pre-generics code.

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| A type parameter `<T>` lets one class/method work over a chosen type | `Box<String>` returns `String` with no cast; misuse is a compile error |
| A bound (`T extends X`) unlocks `X`'s methods on `T` | Unbounded `T` is just `Object`; `a.compareTo(b)` needs the bound |
| Generics are invariant; wildcards widen them | `List<Integer>` ≠ `List<Number>`; `? extends Number` accepts both |
| PECS: Producer `extends`, Consumer `super` | Read from `? extends T`; write to `? super T` |
| Type erasure: generics are compile-time only | `List<String>`/`List<Integer>` are one class; no `instanceof List<String>`, no `new T[]` |

## 6. Gotcha checklist

- **`cannot find symbol: method …` on a type parameter →** `T` is unbounded (just `Object`); add a bound (`T extends Comparable<T>`) to use its methods.
- **`List<Integer> cannot be converted to List<Number>` →** generics are invariant; take `List<? extends Number>` for a read-only parameter.
- **You can't add to a `? extends` collection →** it's a producer; use `? super T` if the method needs to add `T`s.
- **`instanceof List<String>` won't compile →** erasure removes the type argument; test `instanceof List<?>` instead.
- **`new T[]` / `new ArrayList<T>[]` won't compile →** no run-time `T`; use a `List<T>` or an `Object[]` with a cast, accepting the unchecked warning.

---

*Predict, then check.* Write a `Pair<A, B>` class with two type parameters and a `first()`/`second()`; predict what `new Pair<String, Integer>("x", 1).second() + 1` evaluates to. Next, predict whether `static <T> T firstOrNull(List<T> xs)` compiles, and whether adding `xs.get(0).compareTo(...)` inside it does. Finally, predict `new ArrayList<String>().getClass() == new ArrayList<Double>().getClass()`, and explain in one sentence why `obj instanceof Map<String,Integer>` is rejected.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
