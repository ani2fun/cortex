# Part 1 — ZIO: Effects, Layers & Concurrency

> **A program is a value.** That one idea — that "open this database connection" can be a thing you *hold* and *combine*, rather than a thing that just *happens* — is the whole of ZIO. This Part rebuilds it from first principles, then reads the real ZIO that powers Cortex's backend.

## Why start a backend with ZIO?

Every line of Cortex's server is a ZIO value. When you run a Python snippet on this site, your request lands in a `ZIO[R, E, A]` that describes — as data — "verify the caller, check the rate limit, send the code to the sandbox, decode the result". Nothing has *happened* yet; the description is handed to a runtime that executes it, in parallel where it can, with every error accounted for in the type. Understanding that is understanding the spine of the whole system.

## The mental model we'll build

`ZIO[R, E, A]` is a description of a program that:

- needs an environment **`R`** to run (its dependencies),
- may fail with an error of type **`E`** (the things that can go wrong, *in the type*),
- and, if it succeeds, produces an **`A`**.

Three type parameters, three of the hardest problems in programming — dependencies, errors, and success — made into ordinary values you can pass to functions.

## Chapters

**Why Effects?**
1. [What is a side effect, really?](/cortex/production-engineering/zio/why-effects/what-is-a-side-effect)
2. [The ZIO[R, E, A] type: a lazy recipe](/cortex/production-engineering/zio/why-effects/the-zio-type)
3. [Your first program: ZIOAppDefault](/cortex/production-engineering/zio/why-effects/your-first-zio-program)

**Typed Errors: the E Channel**
4. [Failures vs defects](/cortex/production-engineering/zio/typed-errors/failures-vs-defects)
5. [Recovering: catchAll, orElse, fold](/cortex/production-engineering/zio/typed-errors/recovering-from-errors)

**Dependencies as Values: ZLayer**
6. [The R channel: dependencies you don't have yet](/cortex/production-engineering/zio/zlayer/the-r-channel)
7. [Building services with ZLayer](/cortex/production-engineering/zio/zlayer/building-services)
8. [Wiring the app: provide & the dependency graph](/cortex/production-engineering/zio/zlayer/wiring-the-app)
9. [Resourceful layers: acquireRelease & scoped](/cortex/production-engineering/zio/zlayer/resourceful-layers)

**Concurrency & Fibers**
10. [Fibers: green threads you can fork](/cortex/production-engineering/zio/concurrency/fibers)
11. [Blocking vs async: attemptBlocking](/cortex/production-engineering/zio/concurrency/blocking-vs-async)
12. [Racing, timeouts & parallelism](/cortex/production-engineering/zio/concurrency/racing-timeouts-parallelism)

**Cortex in Practice**
13. [An HTTP API with Tapir + zio-http](/cortex/production-engineering/zio/cortex-in-practice/http-api-with-tapir)
14. [Configuration as a value: ZIO Config](/cortex/production-engineering/zio/cortex-in-practice/configuration-as-a-value)
15. [Testing ZIO](/cortex/production-engineering/zio/cortex-in-practice/testing-zio)

*Versions in play: Scala 3.6.2, ZIO 2.1.14, zio-http 3.0.1, Tapir 1.11.22.*

**Begin:** [1. What is a side effect, really? →](/cortex/production-engineering/zio/why-effects/what-is-a-side-effect)
