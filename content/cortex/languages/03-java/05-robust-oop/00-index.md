---
title: Robust OOP & Error Handling
summary: Tier 4 of the Java book — the full object model and the tools for robust, well-structured programs. Inheritance and dynamic dispatch, abstract classes and interfaces, exceptions, lambdas and functional interfaces, sealed types with pattern matching, and how real projects are packaged and built. Every example compiled and run.
prereqs: []
---

# Robust OOP & Error Handling

This is Tier 4, where the object model becomes complete and Java turns toward modern, functional, and well-structured code. You'll specialize types with [inheritance](/cortex/languages/java/classes-and-objects/classes-and-objects) and watch polymorphism pick the right behavior at run time; define contracts with abstract classes and interfaces; handle failure deliberately with exceptions; turn behavior into values with lambdas; model closed sets of data with sealed types and consume them with pattern matching; and learn how packages, modules, and build tools organize and ship real code.

Six chapters, in order:

1. [**Inheritance & Polymorphism**](/cortex/languages/java/robust-oop/inheritance-and-polymorphism) — `extends`, overriding, `super`, dynamic dispatch, `final`, and the `Object` methods.
2. [**Abstract Classes & Interfaces**](/cortex/languages/java/robust-oop/abstract-classes-and-interfaces) — abstract methods, interfaces, `default` methods, and one-class/many-interfaces.
3. [**Exceptions**](/cortex/languages/java/robust-oop/exceptions) — checked vs unchecked, `try`/`catch`/`finally`, custom exceptions, and try-with-resources.
4. [**Nested & Anonymous Classes; Lambdas**](/cortex/languages/java/robust-oop/nested-and-anonymous-classes-and-lambdas) — nested classes, anonymous classes, lambdas, functional interfaces, and method references.
5. [**Sealed Classes & Pattern Matching**](/cortex/languages/java/robust-oop/sealed-classes-and-pattern-matching) — `sealed`, pattern matching for `instanceof`, switch patterns, and record patterns.
6. [**Packages, Modules & the Build**](/cortex/languages/java/robust-oop/packages-modules-and-the-build) — packages and the classpath, JPMS modules, JARs, and Maven/Gradle.

Every code block with a ▶ Run button is live; the project-level examples in the final chapter are shown as real terminal sessions. The highest-value habit at this tier is to **ask "what's the right tool for this shape of problem"** — inheritance vs interfaces vs sealed types each fit a different situation, and choosing well is most of good design.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.
