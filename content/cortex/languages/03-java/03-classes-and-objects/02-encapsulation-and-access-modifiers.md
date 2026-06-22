---
title: Encapsulation & Access Modifiers
summary: Encapsulation hides an object's data behind a controlled interface — fields go private, reached only through methods that can enforce invariants. private/package-private/protected/public set visibility; getters and setters gate access; and final fields with no setters make an object immutable and safe to share. Every rule shown as real output or a real compiler error.
prereqs: []
---

# Encapsulation & Access Modifiers — Controlling Access

A class that exposes its raw fields cannot defend itself: anyone can set `balance` to `-999`, and the class is powerless to object. **Encapsulation** is the fix — hide the data (`private`) and expose only methods that *control* how it changes, so the class can enforce its own rules (its *invariants*). The four **access modifiers** — `private`, package-private (the default), `protected`, and `public` — are the dial that sets who can see each field and method. Pushed to its limit, encapsulation gives you **immutable** objects: data set once at construction, never changed, and therefore safe to share without fear of the aliasing surprise from the last chapter.

This builds on [classes and objects](/cortex/languages/java/classes-and-objects/classes-and-objects) — especially the [aliasing](/cortex/languages/java/classes-and-objects/classes-and-objects) that immutability tames. Every output below was produced by compiling and running the code.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Hiding data with `private`](#1-hiding-data-with-private)
2. [Getters, setters, and invariants](#2-getters-setters-and-invariants)
3. [The four access levels](#3-the-four-access-levels)
4. [Immutability by design](#4-immutability-by-design)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Hiding data with `private`

Marking a field `private` means it can be touched only from *inside* its own class. Code elsewhere must go through the methods the class chooses to expose — a **getter** to read, for instance.

```java run
class Account {
    private int balance;
    Account(int initial) { balance = initial; }
    int getBalance() { return balance; }
}

public class Main {
    public static void main(String[] args) {
        Account acct = new Account(100);
        System.out.println(acct.getBalance());
    }
}
```

**Output:**
```
100
```

**Analysis.** `balance` is `private`, so `main` cannot read it directly — it asks through `getBalance()`, a method the `Account` class deliberately provides. The class now stands between its data and the outside world: every access goes through code the class controls.

**Intuition.**
*Mechanism.* `private` is enforced by the **compiler**: a `private` member is invisible outside its declaring class, and any reference to it from elsewhere is rejected before the program runs.

*Concrete bite.* Reach for the field directly and it won't compile:

```java run
class Account {
    private int balance;
    Account(int initial) { balance = initial; }
}

public class Main {
    public static void main(String[] args) {
        Account acct = new Account(100);
        System.out.println(acct.balance);
    }
}
```

**Compiler error:**
```
Main.java:8: error: balance has private access in Account
        System.out.println(acct.balance);
                               ^
1 error
```

`acct.balance` is rejected because `balance` is `private` to `Account` — `main` is outside that class. The wall is real and checked at compile time, not a convention you can quietly ignore.

*Earned rule.* Make fields `private` by default and expose behavior, not data. The cost is the boilerplate of accessor methods for the data you *do* want to share; the benefit is that the set of ways to touch a field shrinks from "everywhere" to "the handful of methods in this class," which is what makes the next section's invariants enforceable.

---

## 2. Getters, setters, and invariants

The point of routing access through methods is that a method can **check**. A setter (or any mutating method) can reject or adjust bad input, so the object never enters an invalid state — something a bare field can never do.

```java run
class Account {
    private int balance;
    Account(int initial) { balance = initial; }
    int getBalance() { return balance; }
    void deposit(int amount) {
        if (amount <= 0) {
            System.out.println("ignored invalid deposit: " + amount);
            return;
        }
        balance += amount;
    }
}

public class Main {
    public static void main(String[] args) {
        Account acct = new Account(100);
        acct.deposit(50);
        acct.deposit(-30);
        System.out.println(acct.getBalance());
    }
}
```

**Output:**
```
ignored invalid deposit: -30
150
```

**Analysis.** `deposit(50)` passed the check and raised the balance to `150`; `deposit(-30)` was caught by the guard and rejected, leaving the balance untouched. The method enforced the **invariant** "deposits must be positive" — something the class can guarantee precisely because `balance` is `private` and `deposit` is the only way in.

**Intuition.**
*Mechanism.* A mutating method is a gate: it runs validation before changing state, so the object's invariants hold after every call. A `private` field plus controlled mutators means there is *no path* to an invalid value.

*Concrete bite.* A `public` field has no gate — it accepts anything:

```java run
class Account {
    public int balance;
}

public class Main {
    public static void main(String[] args) {
        Account acct = new Account();
        acct.balance = -999;
        System.out.println(acct.balance);
    }
}
```

**Output:**
```
-999
```

With `balance` public, `acct.balance = -999` simply succeeds — the class never gets a chance to object, and a "bank account" now holds `-999`. There is no method to enforce a rule, because the assignment bypasses methods entirely.

*Earned rule.* Expose mutation through methods that validate, not through public fields, so invariants are checked at the one place state changes. The cost is more code than a public field — a getter and a guarded setter instead of a bare variable; the benefit is that "an account can't go below its rules" is enforced by construction, not by hoping every caller behaves.

---

## 3. The four access levels

Every field, method, and constructor has one of four visibilities. From most to least restrictive:

| Modifier | Visible to | Use for |
|---|---|---|
| `private` | the same class only | internal state and helpers |
| *(none)* — package-private | classes in the same package | package-internal collaboration |
| `protected` | same package **and** subclasses | members subclasses need (Tutorial 22) |
| `public` | everyone | the class's intended interface |

`private` is per-**class**, not per-file: even another class sitting in the same file cannot see a `private` member.

```java run
class Account {
    private int balance = 100;
}

class Auditor {
    int peek(Account a) { return a.balance; }
}

public class Main {
    public static void main(String[] args) { }
}
```

**Compiler error:**
```
Main.java:5: error: balance has private access in Account
    int peek(Account a) { return a.balance; }
                                  ^
1 error
```

**Analysis.** `Auditor` is a *different class*, so even though it sits in the same file as `Account`, it cannot read `Account`'s `private` `balance`. Privacy is scoped to the declaring class, full stop — proximity in the file grants no access.

**Intuition.**
*Mechanism.* The compiler resolves each access against the member's modifier and the accessing class's relationship to the declaring class (same class? same package? subclass?). `protected` adds subclass access, which only becomes meaningful with inheritance — its full story is Tutorial 22.

*Concrete bite.* The error above is the demonstration: same file, different class, access denied. The unit of privacy is the class, which is exactly why a helper in the same file still has to go through `Account`'s public methods.

*Earned rule.* Default to `private`, widen only as far as a real collaborator needs — package-private for same-package helpers, `public` only for the deliberate interface, `protected` reserved for what subclasses must reach. The cost of starting narrow is occasionally widening later; the cost of starting `public` is that every field becomes part of your contract, and you can never quietly take it back once other code depends on it.

---

## 4. Immutability by design

The strongest form of encapsulation is to allow no changes at all. A `final` field can be assigned exactly once — at declaration or in the constructor — and never again. A class whose fields are all `final`, with no setters, is **immutable**: its state is fixed for life.

```java run
class Point {
    final int x;
    final int y;
    Point(int x, int y) { this.x = x; this.y = y; }
}

public class Main {
    public static void main(String[] args) {
        Point p = new Point(3, 4);
        System.out.println(p.x + "," + p.y);
    }
}
```

**Output:**
```
3,4
```

**Analysis.** `x` and `y` are `final`, set once in the constructor. After that, a `Point` can be read but never altered — there is no setter, and the `final` fields cannot be reassigned. The object is frozen the moment construction finishes.

**Intuition.**
*Mechanism.* `final` on a field is a compile-time guarantee of single assignment: the compiler verifies the field is set exactly once (every constructor path assigns it) and rejects any later assignment.

*Concrete bite.* Add a method that tries to change a `final` field and it won't compile:

```java run
class Point {
    final int x;
    Point(int x) { this.x = x; }
    void moveTo(int newX) { x = newX; }
}

public class Main {
    public static void main(String[] args) { }
}
```

**Compiler error:**
```
Main.java:4: error: cannot assign a value to final variable x
    void moveTo(int newX) { x = newX; }
                            ^
1 error
```

`moveTo` tries to reassign `x`, but `x` is `final` — already assigned in the constructor — so the compiler refuses. To "move" an immutable point you don't mutate it; you build a *new* `Point`.

*Earned rule.* Make a class immutable — `private final` fields, no setters, set everything in the constructor — whenever you can, especially for values you'll share or use as keys. The cost is that "changing" an immutable object means creating a new one (more allocations), and very deep updates get verbose; the benefit is that an immutable object is safe to alias freely — the [aliasing surprise from the last chapter](/cortex/languages/java/classes-and-objects/classes-and-objects) can't bite, because there is nothing to change behind your back. (Tutorial 21's `record` makes immutable data classes nearly free to write.)

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| `private` hides a member from all code outside its class | `acct.balance` from elsewhere is a compile error; go through methods |
| Mutating methods can validate; public fields cannot | A guarded `deposit` rejects `-30`; a public `balance = -999` always succeeds |
| Four access levels, narrowest-first: private → package → protected → public | Default to `private`; `private` is per-class, not per-file |
| `final` fields are assigned once; no setters → immutable object | Reassigning a `final` field won't compile; "change" means make a new object |
| Immutable objects are safe to share | Aliasing can't surprise you when nothing can mutate |

## 6. Gotcha checklist

- **`X has private access in Y` →** you touched a `private` member from outside its class (even a sibling class in the same file); use a public method.
- **An object reached an invalid state →** you exposed a public field with no validation; make it `private` and gate changes through a method.
- **You meant a field to be internal but other code depends on it →** it was `public` (or you let it leak); start `private` and widen deliberately.
- **`cannot assign a value to final variable` →** you tried to change a `final` field after construction; assign it once in the constructor, or drop `final` if it must change.
- **A shared object changed unexpectedly →** it's mutable and aliased; make it immutable (`final` fields, no setters) so sharing is safe.

---

*Predict, then check.* Give `Account` a `withdraw(int amount)` that refuses to let the balance go negative — predict the output of `new Account(100)`, then `withdraw(150)`, then `getBalance()`. Next, predict the compiler's reaction to adding `acct.balance -= 10;` in `main` when `balance` is `private`. Finally, decide: if `Point` were *not* immutable and two variables aliased the same `Point`, what could go wrong that immutability prevents?

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
