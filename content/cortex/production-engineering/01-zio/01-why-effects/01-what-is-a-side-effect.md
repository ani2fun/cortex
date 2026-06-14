---
title: '1. What is a side effect, really?'
summary: A side effect is anything a function does besides return a value — printing, mutating, reading the clock, calling the network. They make code hard to reason about and test. ZIO''s whole idea is to turn effects into values.
---

# 1. What is a side effect, really?

## TL;DR

> A function is **pure** if its result depends only on its inputs and it does nothing else — same input, same output, no surprises. A **side effect** is *anything else*: printing, mutating a variable, reading the clock, hitting the network or a database. Side effects make programs hard to reason about, test, and combine, because the same line can do different things at different times. ZIO's founding move is to make an effect a **value** — a *description* of "do this later" — so you get the convenience of effects with the reasoning power of pure values.

## 1. Motivation

Here is a function. Is it safe to call it twice?

```scala
def nextId(): Int =
  counter += 1   // mutates a shared variable
  counter
```

You can't tell without knowing about `counter` — a thing *outside* the function. Call it twice and you get different answers; call it from two threads and you might get the *same* answer (a race). The function's behavior depends on hidden state and on *when* and *where* you call it. That hidden dependence is a **side effect**, and it's the root of a huge fraction of real bugs: flaky tests, race conditions, "works on my machine," heisenbugs that vanish when you add a `println`.

Now contrast:

```scala
def add(a: Int, b: Int): Int = a + b
```

`add(2, 3)` is `5`. Always. Everywhere. You can call it a thousand times, on a thousand threads, and reason about it like grade-school arithmetic. The difference between these two functions — and how to get the *power* of the first with the *safety* of the second — is what this whole Part is about.

## 2. Intuition (Analogy)

A **recipe** versus the **act of cooking**.

A recipe is a *value*. It's a piece of paper. You can read it, photocopy it, email it, combine "make the sauce" with "boil the pasta" into a bigger recipe, and put it on the shelf — all *without any food being cooked*. Nothing happens to the kitchen just because the recipe exists.

Cooking is the *act* — the side effects. Heat, mess, a meal that exists now and didn't before. You can only cook once per set of ingredients; you can't "un-fry" an egg.

Ordinary effectful code **mixes these up**: writing `launchMissiles()` is like the recipe *cooking itself the instant you read it*. ZIO **separates** them: `ZIO.attempt(launchMissiles())` is a *recipe* — a value describing the launch — that does nothing until a "chef" (the runtime) actually runs it, at the very end, on purpose.

| The kitchen | Programming |
|---|---|
| A recipe (paper) | An *effect value* — a description of work |
| Copying/combining recipes | `map`, `flatMap`, composing effects |
| Putting a recipe on the shelf | Holding a `ZIO` in a `val` — nothing runs |
| Cooking (heat, mess, a meal) | Actually performing the effect |
| The chef who finally cooks | The ZIO **runtime** at the program's edge |

This separation — *describe everything as values, run them only at the very end* — is why ZIO programs are easy to test, retry, time out, and run in parallel. You can manipulate the recipe all you like before anything irreversible happens.

## 3. Formal Definition

- **Referential transparency.** An expression is *referentially transparent* if you can replace it with its result without changing the program's meaning. `add(2,3)` can be replaced by `5` anywhere — transparent. `nextId()` cannot be replaced by `1` — calling it again gives `2`. Referential transparency is the precise definition of "no surprises."
- **Pure function.** A function whose output depends only on its inputs and which has no observable effect besides returning that output. Pure functions are referentially transparent.
- **Side effect.** Any observable interaction with the world outside a function's return value: mutating state, I/O (console, file, network, DB), reading the clock or a random source, throwing exceptions. These break referential transparency.
- **Effect value (the ZIO idea).** A *first-class value* that **describes** a side-effecting computation without performing it. Performing it is a separate, explicit step. `ZIO.attempt(expr)` wraps a side-effecting `expr` into such a value.

> The slogan: **"Programs as values."** Instead of *doing* I/O as a statement executes, you *build a value* that says what I/O to do, compose those values into a bigger one, and hand the final value to a runtime that performs it. Description and execution become two different times.

## 4. Worked Example

Watch eager-effect vs described-effect, side by side:

```scala
// EAGER: the effect happens the instant this line is reached.
val a: Unit = println("hi")     // "hi" is printed RIGHT NOW, during definition
val list1 = List(a, a)          // prints nothing more — `a` is already Unit; the effect is spent

// DESCRIBED (ZIO): the effect is a VALUE; nothing prints yet.
val b: ZIO[Any, Nothing, Unit] = ZIO.succeed(println("hi"))  // wait — still eager inside succeed!
val c: ZIO[Any, Throwable, Unit] = ZIO.attempt(println("hi")) // a recipe; prints when RUN
val list2 = List(c, c)          // a list of TWO recipes — running it prints "hi" twice
```

The crucial contrast is `a` vs `c`. `a` is the *result* of printing (which already happened); putting it in a list twice prints nothing extra. `c` is a *description* of printing; a list of two `c`s, when run, prints twice — because each element is a recipe to be cooked. (Note: `ZIO.succeed` is for values you *know* are pure; side-effecting code goes in `ZIO.attempt`, which also captures exceptions — Chapter 4.)

This is why Cortex's entire server is built from effect values. When a request arrives, the handler *builds* a `ZIO` describing "check the rate limit, query the database, render the result" — and the runtime performs it. Because it's a value first, the framework can retry it, time it out, run parts in parallel, and test it — none of which you can do to a statement that already executed.

## 5. Build It

Run this. It builds the "effect as a value" idea in plain Scala — a *thunk* (`() => A`, a zero-argument function) is a description; calling it is the execution.

```scala run
@main def run(): Unit =
  // EAGER: the effect fires when `eager` is evaluated.
  def eagerGreeting(): String =
    println(">> cooking immediately")
    "done"

  println("Defining eager value...")
  val eager = eagerGreeting()                 // ">> cooking immediately" prints HERE
  println(s"eager = $eager")
  println(s"Using it twice changes nothing: ${List(eager, eager)}\n")

  // DESCRIBED: wrap the effect in a thunk — a VALUE describing the work.
  def greetingEffect(): String =              // a "recipe": nothing runs yet
    println(">> cooking now")
    "done"

  println("Defining described value... (nothing cooked yet)")
  val recipe: () => String = () => greetingEffect()  // hold the function, don't call it
  println("Built the recipe. Still nothing cooked.\n")

  // A program is a VALUE we can copy and combine before running:
  val program = List(recipe, recipe)          // two recipes
  println("Now run them (cook):")
  program.foreach(step => step())             // NOW the effect fires — twice
```

**Now break it.** In the eager version, try to make `List(eager, eager)` print twice — you can't, because the print already happened when `eager` was evaluated; `eager` is just the string `"done"`. In the described version, the effect fires exactly when *you* choose, as many times as you run it. That control — *when* and *how often* an effect happens — is the entire payoff of "programs as values."

## 6. Trade-offs & Complexity

| Effects as statements (imperative) | Effects as values (ZIO) |
|---|---|
| Familiar, direct | A layer of indirection to learn |
| Runs as you read it | Runs only when handed to the runtime |
| Hard to retry/timeout/parallelize | Trivial to retry/timeout/parallelize |
| Tests touch the real world | Tests can run effects in controlled ways |
| Errors are exceptions (untyped) | Errors can be values in the type (Chapter 4) |

The cost is real: a beginner stares at `ZIO.attempt(...)` and asks "why not just call it?" The payoff arrives the moment you need to do anything *to* an effect — retry it three times, race two of them, swap the real database for a fake in a test. You can't do those things to a statement that already ran; you can do all of them to a value. ZIO bets that production code needs those powers constantly. It's right.

## 7. Edge Cases & Failure Modes

- **Hidden effects in "pure-looking" code.** A function that reads `System.currentTimeMillis()` or a global cache *looks* pure but isn't. The tell: does calling it twice always give the same answer? If not, there's an effect hiding.
- **`ZIO.succeed` around impure code.** `succeed` promises the value is pure; wrapping a side effect in it (e.g. `ZIO.succeed(println(...))`) makes the effect fire eagerly *inside* `succeed`, defeating the point. Use `ZIO.attempt` for anything that can fail or interact with the world.
- **"It worked when I added a print."** A classic heisenbug: the print changed timing or flushed a buffer. Side effects make timing matter; pure values don't.
- **Shared mutable state.** The most dangerous effect — two threads mutating one variable — produces races that are nearly impossible to reproduce. Describing state changes as effects (and using ZIO's safe concurrency, Chapter 10) tames them.

## 8. Practice

> **Exercise 1 — Pure or not?** Classify each and justify: (a) `def square(x) = x*x`; (b) `def now() = System.currentTimeMillis`; (c) `def greet(n) = s"hi $n"`; (d) `def save(u) = db.insert(u)`.

<details>
<summary><strong>Answer</strong></summary>

Apply the one test from §3: *same input → same output, and nothing else observable?*

- **(a) `square` — pure.** Its result depends only on `x`; `square(3)` is always `9`. You could replace `square(3)` with `9` anywhere (referentially transparent).
- **(b) `now` — not pure.** It takes no input yet returns a *different* value each call, because it reads a hidden input from the world (the clock). You can't replace `now()` with any fixed value.
- **(c) `greet` — pure.** Output depends only on `n` and it does nothing else; `greet("Ada")` is always `"hi Ada"`.
- **(d) `save` — not pure.** It performs I/O — it mutates the database. That observable interaction with the outside world *is* the side effect, regardless of what it returns.

The dividing line is never "does it do math?" — it's "does the result depend only on the arguments, with no observable effect?"

</details>

> **Exercise 2 — Spot the hidden effect.** A teammate insists `def discount(p) = p * randomFactor()` is pure "because it just does math." Explain why it isn't, and what would make it pure.

<details>
<summary><strong>Answer</strong></summary>

The *multiplication* is pure, but `randomFactor()` is the effect hiding in plain sight: it reads a hidden, ever-changing input (the random-number generator's state). Call `discount(100)` twice and you get two different answers — so it is **not referentially transparent**, which is the precise definition of "not pure" (§3). The tell is exactly the one from Exercise 1(b): same input, different output ⇒ a hidden input from the world.

To make it pure, *turn the hidden input into an explicit one* — push the effect out to the caller:

```scala
def discount(p: Double, factor: Double): Double = p * factor   // pure: same inputs → same output
```

Now whoever calls it decides where the random factor comes from (generating it is an effect, done once, at the edge). This is the whole move ZIO formalizes: keep the core pure by making its dependencies — including randomness and time — explicit values, and shove the actual effect to the end of the world.

</details>

> **Exercise 3 — Recipe vs cooking.** In your own words, explain why a `List` of two effect *values* can do work twice, but a `List` of two *results* cannot. Tie it back to referential transparency.

<details>
<summary><strong>Answer</strong></summary>

An effect *value* is a **recipe** — a description of work that hasn't happened yet (a thunk, `() => A`). A list of two recipes holds two descriptions; *running* the list performs each one, so the effect happens twice. A *result* is what's left **after** the work already happened — the cooking is done, and a finished meal can't re-cook itself. A list of two results just holds the same outcome twice.

Tie to referential transparency: a *result* is referentially transparent — it's an ordinary value you can copy as many times as you like with zero consequence. An *effectful action* is **not** referentially transparent, so if you want to be able to hold it, duplicate it, and decide when/how often it runs, you must first turn it into a *value* (a recipe). That conversion — action → value — is the founding move of effect systems, and it's exactly why `List(recipe, recipe)` cooks twice while `List(result, result)` cooks nothing.

</details>

```quiz
{
  "prompt": "What is the core idea that lets ZIO make effects easy to retry, time out, and test?",
  "input": "Choose one:",
  "options": [
    "An effect is a VALUE describing work, run only when handed to the runtime — separate from performing it",
    "ZIO runs every effect twice for safety",
    "ZIO disables all side effects",
    "ZIO makes the network faster"
  ],
  "answer": "An effect is a VALUE describing work, run only when handed to the runtime — separate from performing it"
}
```

## In the Wild

- **[ZIO docs — "What is an Effect?"](https://zio.dev/overview/getting-started)** — ZIO's own framing of effects-as-values.
- **[Wikipedia — Referential transparency](https://en.wikipedia.org/wiki/Referential_transparency)** — the precise property pure functions have and effects break.
- **[John De Goes — "The Death of Final Tagless" / effect-system talks](https://www.youtube.com/results?search_query=john+de+goes+zio)** — the philosophy behind ZIO from its creator (any intro talk works).

---

**Next:** now that you see *why* we want effects-as-values, meet the actual type that represents one — `ZIO[R, E, A]` — and run one step by step. → [2. The ZIO[R, E, A] type: a lazy recipe](/cortex/production-engineering/zio/why-effects/the-zio-type)
