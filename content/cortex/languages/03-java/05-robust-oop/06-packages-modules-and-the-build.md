---
title: Packages, Modules & the Build
summary: Packages namespace classes and the classpath finds them; access modifiers gain a second meaning across package boundaries. The JPMS module system (module-info.java) makes a JAR's dependencies and exported packages explicit, enforcing strong encapsulation even on public types. JARs bundle compiled classes, and Maven/Gradle automate the whole build. Shown with real, verified terminal sessions.
prereqs: []
---

# Packages, Modules & the Build — Organizing and Shipping Code

Everything so far has been one file. Real programs are hundreds of classes across many files, pulling in libraries, built and shipped as artifacts — and Java has a layered system for that. **Packages** group related classes into namespaces, and the **classpath** tells the tools where to find them. **Access modifiers** gain their full meaning here: `public` vs package-private is a *boundary*, enforced between packages. The **module system** (JPMS, `module-info.java`) goes a level up: a module declares which packages it **exports** and which others it **requires**, so even a `public` class stays hidden unless its package is exported. **JARs** bundle compiled classes into one file, and **Maven/Gradle** automate compiling, dependency-fetching, and packaging.

Because these examples span *multiple files* and use the `javac`/`jar`/`java` tools, they're shown as real terminal sessions — every command and its output was run and captured locally (the in-page ▶ Run sandbox compiles a single file, so it can't build a multi-package project). Every output below was produced by actually running these commands.

> **How to read the Intuition boxes.** Each one is built in three moves: (1) the **mechanism** — what the compiler and the JVM are *actually doing*; (2) a **concrete bite** — a specific, runnable failure (often a real compiler error), shown so the trap is visible; (3) the **earned rule** — the decision heuristic, now justified rather than asserted, plus its cost.

---

## Table of contents

1. [Packages and the classpath](#1-packages-and-the-classpath)
2. [Encapsulation across packages](#2-encapsulation-across-packages)
3. [Modules: `module-info.java`](#3-modules-module-infojava)
4. [JARs and build tools](#4-jars-and-build-tools)
5. [Mental-model summary](#5-mental-model-summary)
6. [Gotcha checklist](#6-gotcha-checklist)

---

## 1. Packages and the classpath

A class declares its **package** with a `package` line, and its directory must match the package name. Other packages reach it by its fully-qualified name or an `import`. Here a `Main` in `com.example` imports a utility from `com.example.util`:

```java
// src/com/example/util/Text.java
package com.example.util;
public class Text {
    public static String shout(String s) { return s.toUpperCase() + "!"; }
}

// src/com/example/Main.java
package com.example;
import com.example.util.Text;
public class Main {
    public static void main(String[] args) {
        System.out.println(Text.shout("hello"));
    }
}
```

Compile all sources into an output directory, then run by fully-qualified class name:

```
$ javac -d out $(find src -name '*.java')
$ java -cp out com.example.Main
HELLO!
```

**Output:**
```
HELLO!
```

**Analysis.** `javac -d out` wrote the compiled classes mirroring the package structure (`out/com/example/Main.class`, `out/com/example/util/Text.class`); `java -cp out com.example.Main` set the **classpath** to `out` and ran the class by its full name `com.example.Main`. The `import` let `Main` write `Text` instead of `com.example.util.Text` — it's a compile-time shorthand, nothing more.

**Intuition.**
*Mechanism.* A package is a namespace whose name *is* the directory path; the classpath is the list of roots where the JVM looks for `.class` files. `java com.example.Main` means "find `com/example/Main.class` under some classpath root." `import` only abbreviates names; it does not "include" anything.

*Concrete bite.* The directory must match the package — `package com.example.util` *must* live in `…/com/example/util/`, or compilation fails. And `import` is not C's `#include`: it copies no code, just lets you use a short name; the class is found at run time via the classpath.

*Earned rule.* Organize classes into packages by feature/layer, matching directories to package names, and run with the right classpath. The cost is the directory discipline and getting the classpath right (a frequent "could not find or load main class" cause); the benefit is namespaced, collision-free organization that scales from one file to thousands.

---

## 2. Encapsulation across packages

[Access modifiers](/cortex/languages/java/classes-and-objects/encapsulation-and-access-modifiers) showed their full meaning needs packages. A `public` member is visible everywhere; a **package-private** one (no modifier) is visible only *within its own package*. Across a package boundary, package-private is invisible — even to code that can see the class.

```java
// com.example.util.Text  (whisper has no modifier → package-private)
public class Text {
    public static String shout(String s) { return s.toUpperCase() + "!"; }
    static String whisper(String s) { return s.toLowerCase(); }
}

// com.example.Main — a DIFFERENT package — tries to call whisper
public class Main {
    public static void main(String[] args) {
        System.out.println(Text.whisper("HELLO"));
    }
}
```

```
$ javac -d out2 $(find src2 -name '*.java')
```

**Compiler error:**
```
src2/com/example/Main.java:5: error: whisper(String) is not public in Text; cannot be accessed from outside package
        System.out.println(Text.whisper("HELLO"));
                               ^
```

**Analysis.** `Text` is `public` (so `Main` can use it) and `shout` is `public` (so `Main` can call it) — but `whisper` is package-private, belonging to `com.example.util`. `Main` lives in `com.example`, a *different* package, so `whisper` is off-limits: "not public … cannot be accessed from outside package." The boundary is the package, and the default (no modifier) stops at it.

**Intuition.**
*Mechanism.* The compiler resolves each access against the member's modifier *and* the accessing code's package. Package-private grants access only to code in the same package; crossing a package boundary requires `public` (or `protected` for subclasses).

*Concrete bite.* This is real encapsulation, not convention: a package can expose a `public` API while keeping helper classes and methods package-private, and no outside code can reach them — the compiler enforces it. It's how libraries hide their internals from consumers.

*Earned rule.* Make a package's intended API `public` and keep everything else package-private (the default) so internals stay internal across the boundary. The cost is thinking about what belongs to the API; the benefit is that a package's surface is exactly what you marked `public` — but, as the next section shows, the classpath has a hole this can't close.

---

## 3. Modules: `module-info.java`

On the classpath, any `public` type in any package is reachable by anyone — package-private hides *members*, but a `public` class in an "internal" package is still exposed. The **module system** (JPMS, JDK 9+) closes that: a module declares which packages it `exports` and which modules it `requires`, in a `module-info.java` at its root.

```java
// src/com.example.app/module-info.java
module com.example.app {
    // requires java.base implicitly
    exports com.example;          // only this package is visible to other modules
    // com.example.util is NOT exported → strongly encapsulated
}
```

Compile and run with the **module path** instead of the classpath:

```
$ javac -d out --module-source-path src $(find src -name '*.java')
$ java --module-path out -m com.example.app/com.example.Main
HELLO!
```

**Output:**
```
HELLO!
```

```d2
direction: right

mod: "module com.example.app" {
  shape: package
  api: "com.example\n(exported)" { shape: rectangle }
  internal: "com.example.util\n(not exported)" { shape: rectangle }
}
client: "another module\nrequires com.example.app" {
  shape: rectangle
}

client -> mod.api: "can use"
client -> mod.internal: "cannot access"
```

**Analysis.** The module compiled and ran the same program, but now `com.example.app` controls its surface explicitly: `exports com.example` makes that package visible to other modules, while `com.example.util` — even though `Text` is `public` — is **not** exported, so no other module can use it at all. The diagram shows the boundary: a requiring module reaches the exported package and is blocked from the internal one. `requires` likewise makes every dependency explicit, so the module graph is known at compile and launch time.

**Intuition.**
*Mechanism.* A module is a named set of packages plus a `module-info` declaring `exports` (its public API to other modules) and `requires` (its dependencies). The module system enforces these at compile *and* run time — a non-exported package is inaccessible across modules regardless of `public`, and a missing `requires` fails fast.

*Concrete bite.* This is "strong encapsulation": the classpath's hole (public-but-internal types leaking) is sealed. It's why the JDK itself is modularized — `java.base`, `java.sql`, etc. — and why reflective access into JDK internals now requires explicit `--add-opens`.

*Earned rule.* Use modules when you ship a library or large app and want to *enforce* its API surface and dependencies, not just document them. The cost is real friction (every dependency must be `requires`d, reflective and classpath tricks break, and much of the ecosystem still runs on the classpath); the benefit is reliable, declared boundaries — so for application code the classpath is often fine, and modules earn their keep for libraries and platforms.

---

## 4. JARs and build tools

A **JAR** (Java ARchive) is a zip of compiled classes plus a manifest. With a `Main-Class`, it's *executable* — `java -jar` runs it directly:

```
$ jar --create --file app.jar --main-class com.example.Main -C out .
$ java -jar app.jar
HELLO!
```

**Output:**
```
HELLO!
```

**Analysis.** `jar --create` bundled the `out` directory's classes into `app.jar` and recorded `com.example.Main` as the entry point in the manifest, so `java -jar app.jar` found and ran it. A JAR is the standard unit of distribution — one file to ship, put on a classpath, or run. But assembling sources, fetching libraries, and packaging by hand doesn't scale, which is what build tools automate.

**Intuition.**
*Mechanism.* **Maven** and **Gradle** are build tools that, from a project descriptor, download declared dependencies (from repositories like Maven Central), compile, test, and package into a JAR — turning the manual `javac`/`jar` dance into one command (`mvn package`, `gradle build`). They define the project's coordinates, dependencies, and lifecycle.

*Concrete bite.* The descriptor is the project's source of truth. A Maven `pom.xml` dependency —
```
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>33.0.0-jre</version>
</dependency>
```
or the Gradle equivalent `implementation("com.google.guava:guava:33.0.0-jre")` — declares a library by coordinates, and the tool resolves it (and *its* dependencies, transitively) so you never manage JARs by hand.

*Earned rule.* Use `javac`/`jar` to understand what's happening, but use a build tool (Maven or Gradle) for any real project — declare dependencies by coordinates and let it handle resolution, compilation, and packaging. The cost is learning the tool and its conventions; the benefit is reproducible builds with managed, transitive dependencies, which is non-negotiable past a couple of files. (Tutorial 34 returns to build tools alongside testing and shipping a runnable JAR.)

---

## 5. Mental-model summary

| Principle | Consequence |
|---|---|
| A package is a namespace; its name must match the directory | The classpath roots are where the JVM finds `.class` files |
| `import` only abbreviates names; it includes no code | The class is resolved at run time via the classpath |
| Package-private is invisible across a package boundary | A library exposes `public` API and hides internals by default |
| A module `exports`/`requires` packages explicitly (JPMS) | Even a `public` type in a non-exported package is inaccessible |
| JARs bundle classes; Maven/Gradle automate the build | Declare dependencies by coordinates; the tool resolves and packages |

## 6. Gotcha checklist

- **`could not find or load main class` →** the classpath is wrong or the package/directory don't match; run with `-cp <root>` and the fully-qualified name.
- **`package X does not exist` / `cannot find symbol` →** the dependency isn't on the classpath, or the directory doesn't match the `package` declaration.
- **A `public` class is still reachable you wanted hidden →** package-private only hides members; use a module and *not* `exports`-ing its package for strong encapsulation.
- **A modular app fails with `module not found` / `package not visible` →** add the needed `requires`/`exports` to `module-info.java`; the module graph is enforced.
- **Managing library JARs by hand →** use Maven or Gradle; declare dependencies by coordinates and let it resolve them transitively.

---

*Predict, then check.* Given `package com.shop.model;` in a file, predict which directory it must live in. Next, if `com.shop.model.Order` has a `public` field and a package-private `validate()` method, predict which a class in `com.shop.web` can access. Finally, for a module that declares only `exports com.shop.api;`, predict whether another module can use a `public` class in `com.shop.internal` — and what one line in `module-info.java` would change the answer.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
