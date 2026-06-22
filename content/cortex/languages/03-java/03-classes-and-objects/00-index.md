---
title: Classes & Objects
summary: Tier 2 of the Java book — the OOP leap. Four chapters move from procedural code to objects: defining classes, hiding state behind a controlled interface, the class-vs-instance distinction, and the reference model (stack/heap, == vs .equals, null) that underlies every Java surprise. Every example compiled and run.
prereqs: []
---

# Classes & Objects

This is Tier 2, where Java becomes object-oriented. [Tier 1](/cortex/languages/java/control-flow/booleans-and-logic) gave you decisions, loops, arrays, and methods — procedural building blocks. Here the unit of design changes: instead of methods acting on loose data, you bundle data and behavior into **classes** and build **objects** from them. The tier ends with the chapter everything rests on — the reference model — which turns the "surprises" of earlier tiers into predictable consequences.

Four chapters, in order:

1. [**Classes & Objects**](/cortex/languages/java/classes-and-objects/classes-and-objects) — fields and methods bundled into a blueprint, `new`, constructors, `this`, and independent instances.
2. [**Encapsulation & Access Modifiers**](/cortex/languages/java/classes-and-objects/encapsulation-and-access-modifiers) — `private`, getters/setters that enforce invariants, the four access levels, and immutability by design.
3. [**`static` vs Instance**](/cortex/languages/java/classes-and-objects/static-vs-instance) — class-level shared state vs per-object state, why a `static` method has no `this`, `static final` constants, and `static` blocks.
4. [**References, Equality & the Object Model**](/cortex/languages/java/classes-and-objects/references-equality-and-the-object-model) — the flagship: stack vs heap, values vs references, `==` vs `.equals` (with the Integer cache and String pool), and `null`.

Every code block with a ▶ Run button is live. At this tier the highest-value habit is to **draw the references**: when an example aliases two variables or compares two objects, sketch which variables point at which heap objects before you predict the output. The reference model chapter gives you that picture explicitly.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.
