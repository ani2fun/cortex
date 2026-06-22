---
title: Testing, Tooling & Packaging
summary: Tests turn "I think it works" into "the machine proves it works every build." Java's assert is disabled by default, which is why you use JUnit 5 — @Test methods with assertions (assertEquals, assertThrows) that always run, via mvn test. Build tools (Maven/Gradle) resolve dependencies and package your code, and an executable JAR ships it. The capstone, shown with real terminal sessions.
prereqs: []
---

# Testing, Tooling & Packaging — Shipping Reliable Java

This is the last chapter, and it's about everything *around* the code that makes it trustworthy and shippable. **Tests** turn "I think it works" into "the machine verifies it works, on every build" — and Java's built-in `assert` is disabled by default, which is exactly why real projects use **JUnit 5**, whose `@Test` methods always run. **Build tools** (Maven and Gradle) take over the manual `javac`/`jar` dance from [Tutorial 27](/cortex/languages/java/robust-oop/packages-modules-and-the-build): they resolve dependencies from coordinates, compile, run the tests, and package the result. And an **executable JAR** is how you hand someone a program they can run with one command. A good **debugging** strategy ties it together when something still goes wrong.

These examples use multi-file projects and the `javac`/`mvn`/`jar` tools, so they're shown as real terminal sessions — every command and its output was run and captured locally. Every output below was produced by actually running these commands.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Testing with assertions](#1-testing-with-assertions)
2. [JUnit 5](#2-junit-5)
3. [Build tools and dependencies](#3-build-tools-and-dependencies)
4. [Shipping an executable JAR](#4-shipping-an-executable-jar)
5. [Debugging strategy](#5-debugging-strategy)
6. [Mental-model summary](#6-mental-model-summary)
7. [Gotcha checklist](#7-gotcha-checklist)

---

## 1. Testing with assertions

The core idea of a test is an **assertion**: a claim that must hold, checked automatically. Java has a built-in `assert` statement — but it's **disabled unless you pass `-ea`** (enable assertions), which makes it unsuitable as your testing tool.

```java
public class Main {
    static int add(int a, int b) { return a + b; }
    public static void main(String[] args) {
        assert add(2, 2) == 5 : "add(2,2) should be 5";
        System.out.println("passed");
    }
}
```

Run it three ways:

```
$ java -ea Main          # assertions ENABLED, passing version (2+3==5)
all assertions passed

$ java Main              # assertions DISABLED by default — the wrong assert is SKIPPED
passed

$ java -ea Main          # assertions ENABLED, the wrong assert (2+2==5) now fires
Exception in thread "main" java.lang.AssertionError: add(2,2) should be 5
```

**Output** *(the three runs above are real captured sessions):*
```
all assertions passed
passed
Exception in thread "main" java.lang.AssertionError: add(2,2) should be 5
```

**Analysis.** With `-ea`, a true assertion passes silently and a false one throws `AssertionError` with its message — that's a check. But **without** `-ea` (the default), the assertion is *skipped entirely*: the program printed `passed` even though `add(2, 2) == 5` is false. An assertion the JVM ignores by default is worthless as a test, which is the whole motivation for a real testing framework whose checks *always* run.

**Intuition.**
*Mechanism.* `assert cond : msg` throws `AssertionError` if `cond` is false — but only when assertions are enabled (`-ea`); otherwise the JVM strips the check. They were designed for internal sanity checks during development, not as a production test mechanism.

*Concrete bite.* The middle run is the trap: `java Main` printed `passed` for code that's wrong, because the assertion was disabled. Relying on `assert` for tests means your "tests" silently do nothing in any normal run — which is why no one tests with bare `assert`.

*Earned rule.* Use `assert` only for optional internal invariant checks (and know it's off by default); use a real test framework for actual tests. The cost of `assert` is exactly that default-off behavior; the benefit of a framework — next — is checks that always run, with rich assertions and reporting.

---

## 2. JUnit 5

**JUnit 5** is the standard. You write test methods annotated `@Test`, each asserting expected behavior with `assertEquals`, `assertThrows`, and friends; the framework discovers and runs them all, every time. Here's a class and its test:

```java
// src/main/java/Calculator.java
public class Calculator {
    public int add(int a, int b) { return a + b; }
    public int divide(int a, int b) { return a / b; }
}

// src/test/java/CalculatorTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    @Test
    void addsTwoNumbers() {
        assertEquals(5, new Calculator().add(2, 3));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> new Calculator().divide(1, 0));
    }
}
```

Run the tests with the build tool:

```
$ mvn test
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in CalculatorTest
[INFO] Results:
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Output** *(real captured `mvn test` summary):*
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Analysis.** JUnit found both `@Test` methods and ran them: `addsTwoNumbers` checked `add(2, 3)` equals `5` with `assertEquals(expected, actual)`, and `divideByZeroThrows` checked that dividing by zero throws — `assertThrows(ExceptionType, lambda)` runs the [lambda](/cortex/languages/java/robust-oop/nested-and-anonymous-classes-and-lambdas) and passes only if it throws the named [exception](/cortex/languages/java/robust-oop/exceptions). Both passed: `Tests run: 2, Failures: 0`. Unlike `assert`, these checks *always* run — and a failure fails the build, so broken code can't ship.

**Intuition.**
*Mechanism.* JUnit's runner reflects over the test classes, invokes each `@Test` method in isolation, and records pass/fail per assertion. `assertEquals(expected, actual)` reports both values on mismatch; `assertThrows` captures and verifies an exception; failures become a non-zero build result.

*Concrete bite.* The power is regression protection: once `divideByZeroThrows` exists, anyone who later breaks that behavior gets a red build immediately — the test is a permanent, executable specification. Tests also *document* intent (the method names say what should happen) and enable fearless refactoring (change the code, re-run, see green).

*Earned rule.* Write JUnit tests for the behavior you care about — the happy path, edge cases, and the exceptions — and run them in your build so failures block shipping. The cost is writing and maintaining tests (and they're code that can rot); the benefit is a machine-checked safety net that catches regressions instantly and lets you refactor without fear.

---

## 3. Build tools and dependencies

Past a couple of files, manual `javac` doesn't scale — you need dependencies (libraries), a test phase, and packaging. **Maven** and **Gradle** automate all of it from a project descriptor. Maven's `pom.xml` declares the project and its dependencies by **coordinates** (`groupId:artifactId:version`); the tool fetches them (and their transitive dependencies) from a repository:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>calc</artifactId>
  <version>1.0</version>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.2</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

The Gradle equivalent of that dependency is one line — `testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")`. Then `mvn package` (or `gradle build`) runs the whole lifecycle: compile → test → package.

**Analysis.** The `<dependency>` block is *all* it takes to use JUnit — Maven downloads `junit-jupiter` and everything *it* needs, puts them on the test classpath, and the `@Test` code compiles. The `<scope>test</scope>` keeps it out of the shipped artifact. No manually downloaded JARs, no hand-built classpath — the coordinates plus the tool do it. (`mvn package` produced the `Tests run: 2` result above as part of its lifecycle.)

**Intuition.**
*Mechanism.* A build tool models the project as a descriptor (`pom.xml`/`build.gradle`) plus a fixed lifecycle of phases. It resolves the dependency graph transitively from repositories (Maven Central), caches artifacts locally, compiles, runs tests, and packages — reproducibly, the same on every machine.

*Concrete bite.* The transitive resolution is the real value: declaring one dependency pulls in its dependencies automatically, with version conflict mediation. Doing this by hand — tracking which JAR needs which other JAR — is exactly the "JAR hell" build tools were invented to end.

*Earned rule.* Use a build tool (Maven or Gradle) for any real project; declare dependencies by coordinates and let it resolve, compile, test, and package. The cost is learning the tool's model and conventions (and occasional dependency-conflict debugging); the benefit is reproducible builds with managed transitive dependencies — the baseline for collaborating and shipping.

---

## 4. Shipping an executable JAR

To hand someone a runnable program, package it as an **executable JAR**: a single archive whose manifest names the `Main-Class`, so `java -jar` launches it directly.

```
$ javac Greeting.java
$ jar --create --file app.jar --main-class Greeting Greeting.class
$ java -jar app.jar
Hello from an executable JAR!
```

**Output** *(real captured session):*
```
Hello from an executable JAR!
```

**Analysis.** `jar --create` bundled the compiled class into `app.jar` and recorded `Greeting` as the entry point; `java -jar app.jar` read that manifest and ran `main` — one file, one command. In practice a build tool produces this for you (`mvn package` builds a JAR; plugins like the Shade/Assembly plugin or Spring Boot's repackaging build a "fat JAR" bundling all dependencies, so the single file runs with no extra classpath).

**Intuition.**
*Mechanism.* A JAR is a ZIP of classes plus a `META-INF/MANIFEST.MF`. A `Main-Class` entry makes it executable; `java -jar` reads the manifest and launches that class. For dependencies, a fat/uber JAR bundles them in, or the manifest's `Class-Path` references them.

*Concrete bite.* A plain JAR contains *your* classes only — if your code uses libraries, `java -jar app.jar` fails with `NoClassDefFoundError` unless those libraries are on the classpath or bundled into a fat JAR. "It runs in my IDE but not as a JAR" is almost always missing dependencies in the artifact.

*Earned rule.* Ship an executable JAR (a fat JAR for apps with dependencies) built by your build tool, and run it with `java -jar`. The cost is configuring the packaging (fat-JAR plugin) and a larger artifact; the benefit is a single, self-contained file anyone with a JVM can run — the "write once, run anywhere" of [Tutorial 1](/cortex/languages/java/first-steps/what-java-is-and-running-code) delivered as one command.

---

## 5. Debugging strategy

When a test goes red or production misbehaves, debug methodically rather than by guesswork. The toolkit: **read the stack trace** (it names the exception and the exact line — start at the top frame in *your* code); **reproduce** the failure reliably (a failing test is the ideal repro); **use a debugger** to set breakpoints and inspect state, or add targeted logging; and **bisect** — narrow the problem by halving (which commit, which input, which method) until the cause is isolated.

**Analysis.** A stack trace is not noise — it's a map: the exception type tells you *what*, the message tells you *why*, and the frames tell you *where*, top to bottom through the call chain. The single most effective debugging move is to turn the bug into a **failing test**: it reproduces the problem on demand, tells you the moment you've fixed it (green), and stays as a regression guard forever after.

**Intuition.**
*Mechanism.* A debugger pauses the JVM at a breakpoint and lets you inspect variables and step through execution; logging records state over time; a failing test pins the exact behavior. Each converts "it's broken somewhere" into concrete, observable facts.

*Concrete bite.* The anti-pattern is changing code randomly hoping the symptom disappears — it often hides the bug instead of fixing it, or breaks something else. Reproduce first, understand the cause, *then* fix; a fix you can't explain isn't a fix.

*Earned rule.* Debug by reproducing (ideally as a failing test), reading the stack trace to the cause, and inspecting state with a debugger or logging — not by guessing. The cost is the discipline to understand before editing; the benefit is fixes that actually address the cause, with a regression test ensuring the bug stays dead.

---

## 6. Mental-model summary

| Principle | Consequence |
|---|---|
| `assert` is disabled by default (`-ea` to enable) | It silently skips in normal runs — useless as a test mechanism |
| JUnit `@Test` methods with assertions always run | `assertEquals`/`assertThrows` check behavior; a failure fails the build |
| Build tools resolve dependencies by coordinates and run the lifecycle | One `<dependency>` pulls in transitive deps; `mvn test`/`package` does it all |
| An executable JAR's manifest names the `Main-Class` | `java -jar app.jar` runs it; a fat JAR bundles dependencies |
| Debug by reproduce → read the trace → inspect → fix | A failing test is the best repro and a permanent regression guard |

## 7. Gotcha checklist

- **`assert` "tests" pass for broken code →** assertions are off by default; use JUnit, whose checks always run (or `-ea` for internal invariants).
- **`mvn`/`gradle` can't find a class you use →** the dependency isn't declared; add it by coordinates in `pom.xml`/`build.gradle`.
- **`java -jar app.jar` throws `NoClassDefFoundError` →** the JAR lacks its dependencies; build a fat/uber JAR or set the classpath.
- **`no main manifest attribute` from `java -jar` →** the JAR has no `Main-Class`; build it with `--main-class` (or the build tool's config).
- **Debugging by random edits →** reproduce the failure as a test, read the stack trace to the cause, then fix — and keep the test.

---

*Predict, then check.* Predict what `java Main` (no `-ea`) prints for a program whose only statement is a *false* `assert`, versus `java -ea Main`. Next, predict whether a JUnit test `assertEquals(4, calc.add(2, 3))` passes, and what the failure message would show. Finally, write a `pom.xml` `<dependency>` for a hypothetical library `com.acme:widgets:2.1.0` and explain what `mvn package` does with it — and why a fat JAR is needed to run the result with `java -jar`.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
