---
title: '3. Your first program: ZIOAppDefault'
summary: A recipe needs a chef. ZIOAppDefault is the runtime that runs your top-level effect — the one place effects actually happen. Meet it through Cortex''s real entry point.
---

# 3. Your first program: ZIOAppDefault

## TL;DR

> All those effect values have to *run* somewhere. That somewhere is the **edge of your program** — a single top-level effect handed to a **runtime**. ZIO gives you **`ZIOAppDefault`**: extend it, implement `def run`, and ZIO executes that one effect, performing every side effect it describes. This is "the end of the world" — the only place cooking happens. Cortex's `Main` is a `ZIOAppDefault` whose `run` is "apply migrations, then serve HTTP forever."

## 1. Motivation

If effects are values that don't run until handed to a runtime (Chapters 1–2), there must be *one place* you finally hand the whole program over. Letting effects run anywhere would re-introduce all the chaos we just escaped. So functional programs are shaped like a funnel: thousands of small effect values compose into *one* big effect, and that single value is run *once*, at `main`. Everything above is pure description; only the tip of the funnel touches the world.

`ZIOAppDefault` is that tip. It wires up a runtime (thread pools, the fiber scheduler, default services like `Console` and `Clock`), runs your top-level effect, and translates success/failure into a process exit code. You write the recipe; it's the chef.

## 2. Intuition (Analogy)

A play. The **script** is pure description — every line, every stage direction, written down, performed by nobody yet. You can edit it, copy it, combine scenes. **Opening night** is when the script is finally *performed* — the one moment description becomes action, lights and all.

`ZIOAppDefault.run` is opening night. Everything you built (Chapters 1–2 and onward) is the script; `run` is the single performance that brings it to life. And just as a play has *one* opening night, a ZIO program has *one* place where effects actually fire — the runtime at `main`. Keep all your "scripting" pure and shove the single "performance" to the very edge, and the whole program stays easy to reason about.

## 3. Formal Definition

- **`ZIOAppDefault`** — a trait you extend to make a runnable ZIO program. You implement `def run: ZIO[R, E, A]`; ZIO supplies a `main` that builds a default runtime and executes it.
- **The runtime** — the machinery that actually performs an effect: a fiber scheduler, thread pools (a CPU pool and a blocking pool), and built-in services (`Console`, `Clock`, `Random`, `System`). You rarely touch it directly; `ZIOAppDefault` configures a sensible default.
- **`bootstrap`** — an optional `ZLayer` you override to configure the runtime *before* `run` (e.g. install a config provider or custom logging). Cortex uses it to load configuration from a file.
- **"The end of the world"** — the FP nickname for this single run point. *Above* it: pure values. *At* it: execution. The discipline is to keep that boundary as thin and as late as possible.

> One program, one run point. The art of structuring a ZIO app is keeping everything a *value* until the very last moment, so the funnel's tip — where the messy real world lives — is small and isolated.

## 4. Worked Example — Cortex's real entry point

Here is Cortex's actual [`Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala), lightly trimmed:

```scala
object Main extends ZIOAppDefault:

  // bootstrap: configure the runtime BEFORE `run` — load config from application.conf.
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())

  override def run: ZIO[Any, Throwable, Unit] =
    // The whole program as ONE value: run migrations, THEN serve HTTP forever.
    val program: ZIO[HttpApp & DataSourceEnv, Throwable, Unit] =
      Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)

    // Provide every dependency, turning R into `Any` so it can run (Chapter 8).
    program.provide(
      AppConfig.live, dbCfg, redisCfg, mongoCfg,
      DataSource.live, HelloPipeline.live, CodeRunPipeline.live,
      CortexPipeline.live, BlogPipeline.live, Auth.live,
      RateLimiter.live, SubmissionPipeline.live, HttpApp.live
    )
```

Read it as the funnel. `Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)` is the *entire* server expressed as one effect value: apply Liquibase migrations (`*>` means "then, discarding the first result"), then ask the `HttpApp` service to `serve`. That value *needs* an `HttpApp` and a data source (`R = HttpApp & DataSourceEnv`). `.provide(...)` supplies all the services (Chapter 8), shrinking `R` to `Any`. `ZIOAppDefault` then runs the result. Nothing — not a migration, not a socket — happens until that final run. The whole server is a recipe, cooked once.

The `*>` is worth noticing: it sequences two effects and keeps the second's result. `Migrations.run *> serve` means "migrate, **then** serve" — and because migrations come *first* in the value, a failed migration short-circuits before the HTTP port ever opens (the fail-fast property you'll meet again in Part 2).

## 5. Build It

Run this. It models the funnel: build a top-level "app" value from smaller effect values, then a single `run()` performs them in order — your own miniature `ZIOAppDefault`.

```scala run
@main def run(): Unit =
  // Reuse the "effect as a thunk" idea from Chapter 2.
  def effect(label: String, fn: () => String): () => String = () =>
    println(s"  - running: $label")
    fn()

  def seq(effects: (() => String)*): () => String = () =>  // *> chaining: run in order, keep the last
    var result = ""
    effects.foreach(e => result = e())
    result

  // Build the program as ONE value (nothing runs yet)
  val migrate = effect("apply migrations", () => "schema v3")
  val serve   = effect("serve HTTP on :8080", () => "listening")
  val program = seq(migrate, serve)        // migrate *> serve

  println("Program built. Side effects so far: NONE.\n")

  // The single run point ("ZIOAppDefault.run")
  println("=== opening night: run() ===")
  val exitValue = program()
  println(s"exit: $exitValue")
```

**Now break it.** Make `migrate` fail: change its `fn` to `lambda: (_ for _ in ()).throw(RuntimeError("bad migration"))` (or simply `1/0`). Wrap `program()` in a try/except. The failure happens *before* `serve` runs — exactly Cortex's fail-fast: a broken schema crashes the boot instead of serving requests against a bad database. Ordering the effects in the value *is* ordering the real-world actions.

## 6. Trade-offs & Complexity

| One run point (ZIO) | Effects scattered (imperative) |
|---|---|
| One place to configure runtime, logging, errors | Setup duplicated and inconsistent |
| Whole program is one testable value | Tangled, hard to test as a unit |
| Clear "edge of the world" boundary | Effects everywhere, boundary unclear |
| Funnel discipline to maintain | Easier at first, messier at scale |

The discipline — *keep everything a value until `run`* — takes getting used to; the reflex to "just do the I/O here" is strong. But concentrating execution at one edge is what makes the rest of the program pure and composable. `ZIOAppDefault` makes that edge a single, well-lit place.

## 7. Edge Cases & Failure Modes

- **Doing I/O outside `run`.** Sneaking a `println` or DB call into a constructor re-introduces an effect outside the funnel. Keep object/layer construction pure; do work in effects.
- **Forgetting to `provide`.** If `R` isn't `Any` at the run point, it won't compile — the type is telling you a dependency is missing (Chapter 8). That error is a feature.
- **Blocking the main thread.** Long blocking work belongs on the blocking pool (`attemptBlocking`, Chapter 11), not the CPU pool, or you starve the scheduler.
- **Swallowing the exit code.** `run`'s failure becomes a non-zero process exit. In containers/Kubernetes (Part 4), that exit is how the orchestrator knows the app died — don't accidentally catch-all and exit 0 on failure.

## 8. Practice

> **Exercise 1 — Find the edge.** In Cortex's `Main`, identify the single expression that *is* the whole program, and the single call that runs it. How many places do effects actually fire?

<details>
<summary><strong>Answer</strong></summary>

- **The whole program** is one value: `Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)` — "apply migrations, then serve." Everything else (`.provide(...)`) just supplies its dependencies; it's still one `ZIO` value.
- **What runs it** is `ZIOAppDefault`'s machinery executing `run` — i.e. handing that single provided value to the runtime.
- **Effects fire in exactly one place:** the runtime at the application's edge ("the end of the world"). *Above* that point everything is a pure description; only the runtime turns the description into action.

That's the funnel: thousands of small effect values compose into one, and one runtime call performs it. If you can name more than one place effects fire, something has leaked out of the funnel.

</details>

> **Exercise 2 — Order matters.** Explain why `Migrations.run *> serve` (migrations first) gives fail-fast behavior, and what would change if you wrote `serve *> Migrations.run`.

<details>
<summary><strong>Answer</strong></summary>

`*>` sequences two effects — "do the left, then the right, keep the right's result" — and crucially it **short-circuits on failure** (Chapter 5): if the left effect fails, the right never runs.

- **`Migrations.run *> serve`** runs migrations *first*. If a migration fails, the whole effect fails *before* `serve` is reached, so the HTTP port never opens on a broken schema. That's fail-fast: the bad version refuses to start rather than serving corrupt data (the full payoff is in Part 2, Chapters 7–8).
- **`serve *> Migrations.run`** would open the port and start serving *first*, then attempt migrations — so the app could accept requests against an *un-migrated* schema (500s, missing tables), and a failed migration wouldn't stop the already-running server.

The lesson: ordering the effects in the *value* orders the real-world actions. The sequence isn't a comment about intent — it *is* the behavior.

</details>

> **Exercise 3 — Bootstrap.** What does Cortex's `bootstrap` layer do, and why must it run *before* `run`? (Hint: what would `AppConfig.live` need that isn't there yet otherwise?)

<details>
<summary><strong>Answer</strong></summary>

`bootstrap` installs the **config provider**: `Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())`, which makes `application.conf` the source for all configuration.

It must run *before* `run` because `run`'s layers — notably `AppConfig.live` — read configuration via `ZIO.config`, and that needs a config provider to *already be installed*. Without it, there's no source to resolve `db.url`, `auth.enabled`, etc., and config reading would fail at startup. `bootstrap` configures the *runtime itself* (the environment the program runs in), which is logically prior to the program; so ZIO runs it first, then runs your `run` effect inside that configured runtime.

</details>

```quiz
{
  "prompt": "In a ZIO application, where do side effects actually get performed?",
  "input": "Choose one:",
  "options": [
    "At one place — the runtime running the single top-level effect (ZIOAppDefault.run), 'the end of the world'",
    "Everywhere a ZIO value is defined",
    "Inside every `map` call",
    "Whenever a `val` holding a ZIO is created"
  ],
  "answer": "At one place — the runtime running the single top-level effect (ZIOAppDefault.run), 'the end of the world'"
}
```

## In the Wild

- **[ZIO docs — ZIOApp & ZIOAppDefault](https://zio.dev/reference/program/)** — building a runnable app and overriding `bootstrap`.
- **[ZIO docs — Runtime](https://zio.dev/reference/runtime/)** — what the runtime actually does (fibers, thread pools, default services).
- **[Cortex `Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala)** — the real entry point dissected above.

---

**Next:** our recipes can succeed — but the world fails constantly. ZIO's masterstroke is putting errors in the type. Meet the **E** channel, and the crucial line between a *failure* and a *defect*. → [4. Failures vs defects](/cortex/production-engineering/zio/typed-errors/failures-vs-defects)
