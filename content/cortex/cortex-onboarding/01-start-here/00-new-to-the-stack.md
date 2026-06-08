---
title: New to the stack? Read this first
summary: A plain-English primer on every technology this codebase uses, plus links to learn each one — written for a junior developer who has never touched Scala, Scala.js, React, or ZIO.
---

# New to the stack? Read this first

This project uses a lot of technology you may never have seen. **That's fine.** This page is a five-minute, jargon-free tour of *what each piece is and why it exists*, so the rest of the onboarding book makes sense. Skim it now; come back when a term trips you up.

> **The one-sentence summary:** the whole app — frontend *and* backend — is written in **one language (Scala)**, the backend runs on the JVM, the frontend is compiled into JavaScript that runs in the browser, and a single API description keeps the two halves in sync automatically.

## The mental model

```
   You write Scala ──┬──► compiles to JVM bytecode ──► the server (backend)
                     └──► compiles to JavaScript   ──► the browser (frontend)

   One OpenAPI file describes the API ──► generates the Scala types BOTH sides use
```

If you remember only that picture, you're 80% of the way there.

## A glossary of every scary word in this book

You do **not** need to know these before starting — just recognise them.

| Term | In one breath |
|---|---|
| **Scala 3** | A programming language for the JVM (like Java, but more concise and functional). [Learn](https://docs.scala-lang.org/scala3/book/introduction.html) |
| **Scala.js** | A compiler that turns Scala into **JavaScript**, so the *same language* runs in the browser. [Learn](https://www.scala-js.org/doc/) |
| **Cross-compilation** | Writing a piece of code **once** and compiling it to **two** targets (JVM *and* JavaScript). The `shared/` folder does this — one `case class` is literally the same on server and client. |
| **sbt** | Scala's build tool — compiles, tests, packages. You'll type `sbt compile`, `sbt test`, `./bin/dev`. [Learn](https://www.scala-sbt.org/1.x/docs/) |
| **ZIO** | A library for writing **effects** — values that *describe* work (read a DB, call an API) without running it yet. The runtime runs them. Think "a recipe, not the cooking". [Learn](https://zio.dev/overview/getting-started) |
| **`ZIO[R, E, A]`** | An effect that needs environment `R`, may fail with error `E`, or succeed with value `A`. Shorthands: `IO[E, A]` (no environment), `UIO[A]` (can't fail), `Task[A]` (may throw any error). |
| **ZLayer** | ZIO's **dependency injection**: a typed recipe for building a component (like a DB pool) from its inputs. No Spring, no annotations. |
| **`for { … } yield …`** (over effects) | Does **not** loop — it **sequences** effects one after another, like chaining `await` in JavaScript. |
| **tapir** | Describe an HTTP endpoint as a plain Scala **value** (its path, inputs, outputs, errors), then *interpret* that one description as a server route **and** a client call. [Learn](https://tapir.softwaremill.com/en/latest/) |
| **circe** | The JSON library — turns Scala objects into JSON and back. |
| **HOCON** | A friendlier superset of JSON used for config (`application.conf`). `${?ENV_VAR}` means "override this with an environment variable if it's set". |
| **scalajs-react** | A Scala.js wrapper over **React** (the UI library). Components are Scala functions. [Learn](https://github.com/japgolly/scalajs-react) · [React itself](https://react.dev/learn) |
| **`Callback`** | In scalajs-react, side effects aren't run immediately — they're wrapped in a `Callback` value (essentially a `() => Unit`) that React runs after rendering. |
| **`Future`** | Scala's standard async result — the equivalent of a JavaScript `Promise`. |
| **Vite** | The frontend dev server + bundler. Gives instant hot-reload at `localhost:5173`. [Learn](https://vitejs.dev/) |
| **Tailwind (v4)** | "Utility-first" CSS: you style by composing small classes (`flex gap-2 rounded`) instead of writing CSS files. [Learn](https://tailwindcss.com/docs) |
| **OpenAPI codegen** | A build step that reads `api/openapi.yaml` and **writes Scala source** (the request/response types) so you never hand-sync the API between server and client. |
| **`src_managed`** | sbt's folder for **generated** (not hand-written) source code — that's where the codegen output lands. |
| **SPA** | Single-Page Application — the browser loads one HTML page and JavaScript swaps the content as you navigate. |
| **Deep module / seam** | Codebase vocabulary: a *deep module* is a package with a tiny public surface and lots hidden inside; a *seam* is a small internal interface you can swap a fake into for testing. |
| **ADR** | "Architecture Decision Record" — short docs in `docs/adr/` explaining *why* a design was chosen. Optional reading, referenced often. |

## Recommended reading order for a beginner

1. **This page** (you're here).
2. **[Hello World, End to End](/cortex/codefolio-onboarding/how-it-works-hello-world-end-to-end)** — the single best chapter to start with. It traces one real request through every layer, so the abstract pieces above become concrete.
3. **[Overview](/cortex/codefolio-onboarding/start-here-overview)** and **[Repository Tour](/cortex/codefolio-onboarding/start-here-repository-tour)** — the big-picture map.
4. **[Local Development](/cortex/codefolio-onboarding/working-on-it-local-development)** — get it running on your machine.
5. The **Deep dive** section — read these as *reference* when you need them, not cover-to-cover on day one.

## External tutorials worth an afternoon

- **Scala 3 in a hurry** — the [official Scala 3 Book](https://docs.scala-lang.org/scala3/book/introduction.html) (read "A Taste of Scala").
- **Effects & ZIO** — [ZIO: Getting Started](https://zio.dev/overview/getting-started).
- **The frontend model** — [React: Thinking in React](https://react.dev/learn) then [scalajs-react](https://github.com/japgolly/scalajs-react).
- **The API-first idea** — [tapir's "Quickstart"](https://tapir.softwaremill.com/en/latest/quickstart.html).

Don't try to master these first. Get the app running ([Local Development](/cortex/codefolio-onboarding/working-on-it-local-development)), read the Hello World trace, and learn each library the moment you first need it.
