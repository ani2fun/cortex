---
title: Shared & Codegen — One Spec, Two Compilers
summary: How `api/openapi.yaml` becomes Scala code on both the JVM and JS sides, why the `shared` module exists at all, and what each codegen knob does.
---

The `shared` module is the smallest of the three sbt projects but it's the one that makes the rest of the codebase coherent. Almost nothing in `shared/src/` is hand-written — the meaningful content is generated from `api/openapi.yaml` at build time, into both a JVM target and a JS target. This chapter explains the cross-compile shape, the codegen plugin's settings, and the specific failure modes you'll hit.

## Why `shared` exists at all

You could put the case classes in `server/` and copy them by hand into `client/`. Most full-stack codebases do exactly that. It's a bad idea for one reason: **drift**. The DTO on the server says `language: String`, the DTO on the client says `lang: String`, and you find out at runtime that JSON keys don't match.

The fix is to put the types in a single source file that compiles to both target platforms. That's what a Scala.js cross-project does. From `build.sbt`:

```scala
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .enablePlugins(OpenapiCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-core"       % tapirV,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % tapirV,
      "io.circe"                    %%% "circe-generic"    % circeV
    ),
    openapiSwaggerFile  := (ThisBuild / baseDirectory).value / "api" / "openapi.yaml",
    openapiPackage      := "cortex.shared.api",
    openapiObject       := "Endpoints",
    openapiJsonSerdeLib := "circe"
  )
```

The result is two compiled artefacts — `sharedJVM` and `sharedJS` — that contain the **same source** linked against the right standard library and target. Both export `cortex.shared.api.Endpoints` with identical type signatures.

**If you remove the cross-project and put the types in `server/`:** the client can't see them, you write them again on the client side, they drift within a week.

## `crossType(CrossType.Pure)` — no platform-specific files

There are three cross-types in Scala.js:

- **`Pure`** — one source tree, compiles to both. *This is what we use.*
- **`Full`** — `shared/shared/src/`, `shared/jvm/src/`, `shared/js/src/`. Lets you include platform-specific code.
- **`Dummy`** — like `Pure` but no separate platform sources at all.

`Pure` is correct for `shared` because everything in it is platform-agnostic: case classes, tapir endpoint values, circe codecs. There's no `java.io.File` (JVM-only) and no `org.scalajs.dom` (JS-only). The shared `CodeExecutor` state machine is also platform-agnostic — it manipulates strings and numbers, no filesystem, no DOM.

**If you ever need a JVM-only helper in `shared`:** that's a sign the type doesn't belong in `shared`. Move it to `server/`. The cross-project shape exists to enforce "if it's here, both sides can use it".

## What `OpenapiCodegenPlugin` actually generates

When you run `sbt sharedJVM/compile` for the first time, the plugin:

1. **Reads `api/openapi.yaml`** (path from `openapiSwaggerFile`).
2. **Parses the OpenAPI 3 document** — paths, components/schemas, components/responses.
3. **Emits three Scala files** into each platform's `src_managed`:
   - `Endpoints.scala` — the `Endpoints` object containing tapir `Endpoint[I, E, O, R]` values for each path, plus nested case classes for every `components/schemas` entry.
   - `EndpointsJsonSerdes.scala` — circe `Encoder`/`Decoder` instances for every case class.
   - `EndpointsSchemas.scala` — tapir `Schema` instances (used for the OpenAPI doc round-trip and for runtime validation).
4. **Adds the generated dir to `sources`** so the next compile sees them.

The output lives at:

```
shared/.jvm/target/scala-3.6.2/src_managed/main/sbt-openapi-codegen/
shared/.js/target/scala-3.6.2/src_managed/main/sbt-openapi-codegen/
```

Both directories contain *the same Scala source*. The cross-project gives each one its own target so JVM and JS get separate `.class` / `.sjsir` files.

A schema entry like:

```yaml
RunRequest:
  type: object
  required: [language, source]
  properties:
    language:
      type: string
    source:
      type: string
    stdin:
      type: string
      nullable: true
```

becomes:

```scala
case class RunRequest(language: String, source: String, stdin: Option[String])
```

Required fields become non-`Option`; nullable fields become `Option[T]`. JSON keys are kept verbatim (no transformation), so writing camelCase in YAML keeps camelCase in JSON.

## Why a `String`-typed `language` and not an enum

`RunRequest.language` is a free-form `String` in `api/openapi.yaml`, even though the server only supports a fixed catalog of languages. Two reasons:

1. **Validation is server-side.** `server/.../codeRunPipeline/Languages.scala` holds the catalog and resolves incoming aliases via `Languages.resolve` — the client doesn't need to know the alias list at compile time.
2. **Codegen and enums are awkward.** OpenAPI `enum` for a string field generates a Scala 3 union of singleton string types, which is technically fine but ugly to pattern-match against. We dodge it.

If the client sends `python3`, the server resolves the alias. If it sends `cobol`, the server returns `RunFailure.BadInput("Unsupported language …")` (mapped to 400). The client doesn't have to maintain its own copy of the language list.

**If you change `language` to a YAML `enum`:** you'll get codegen output that doesn't pattern-match cleanly, and you'll need to keep the YAML enum in sync with `Languages.scala`. Don't.

## Why circe (and not jsoniter)

`build.sbt`:

```scala
openapiJsonSerdeLib := "circe"
```

The plugin supports circe and jsoniter. We picked circe because:

- It's the most widely-used Scala JSON library and matches what tapir's other ZIO integrations expect.
- The codegen-emitted codecs are `Encoder`/`Decoder` instances, which compose with the rest of the tapir-circe ecosystem.
- jsoniter is faster but its macros are heavier and the error messages are worse during development.

The trade-off vs zio-json: zio-json isn't supported by the codegen plugin at all. We could hand-write zio-json codecs, but then we'd lose the auto-generated ones, which is the whole point of the plugin. (Discussed in [Server Stack](/cortex/cortex-onboarding/deep-dive/server-stack#why-circe-and-not-zio-json).)

**If you change to `"jsoniter"`:** swap `tapir-json-circe` for `tapir-json-jsoniter` in `build.sbt`, run `sbt clean compile`, and verify the OpenAPI doc round-trips. Most callers won't notice.

## Why `tapir-json-circe` and `circe-generic` together

Two libraries do related work:

- **`tapir-json-circe`** wires circe into tapir — gives `jsonBody[T]`, `Schema`-derivation hooks, and the OpenAPI integration.
- **`circe-generic`** provides the macros that derive `Encoder[T]` / `Decoder[T]` for case classes.

The codegen could emit hand-written codecs, but it leans on `circe-generic` to keep the generated file small. The trade-off is one transitive dependency for a much smaller `EndpointsJsonSerdes.scala`.

**If you remove `circe-generic`:** the generated codecs won't compile. You'd need a different serde lib (jsoniter), or you'd hand-write codecs.

## The `-Wconf:src=.*src_managed/.*:s` silencer

`build.sbt`:

```scala
"-Wconf:src=.*src_managed/.*:s",
```

The codegen-emitted `Endpoints.scala` uses imports we'd flag as unused if we authored the file (`import sttp.tapir.json.circe._` and friends are imported eagerly even when a particular endpoint doesn't need them). With `-Wunused:all` enabled project-wide, those imports would generate warnings on every clean build. The silencer scopes the warning suppression *to files under `src_managed`*, leaving our own code's `-Wunused:all` intact.

The choice is deliberate: we want unused-warnings on hand-written code (catches real dead imports) but not on generated code (we don't author it, can't reasonably fix it). Pattern: `src=…regex…:s`. The `:s` means *silence* (other actions are `:e` error, `:w` warning, `:i` info).

**If you remove this line:** clean builds output ~8 warnings per generated file. Real warnings get lost in the noise; over time you stop reading the build output.

## The Scala 3 E198 silencer

`build.sbt`:

```scala
"-Wconf:msg=unused local definition:s"
```

Scala 3 occasionally mis-attributes an "unused local definition" warning to the closing paren of a method-chain expression (a known false positive). Silenced project-wide with this matcher; revisit when upstream fixes the diagnostic.

`msg=…` matches against the warning's text body. Targeted enough to be safe, broad enough to catch the false positives.

**If you remove this line and the false positive comes back:** every `for ... yield ...` block on a chained call gets a spurious warning. We'd start reflexively `@nowarn`-ing them, which is worse than silencing one specific message.

## How `shared` depends on tapir

```scala
"com.softwaremill.sttp.tapir" %%% "tapir-core"       % tapirV,
"com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % tapirV,
"io.circe"                    %%% "circe-generic"    % circeV,
```

Three things to know:

- **`%%%`** (three `%`s) is the cross-project artefact lookup — picks `tapir-core_sjs1_3` for the JS variant and `tapir-core_3` for the JVM. Without `%%%` you'd hard-code the wrong artifact for one platform.
- **No `tapir-zio` here.** That dependency is server-only — `shared` describes the contract, not the runtime, so the ZIO interpreter belongs in `server`. The split keeps `sharedJS` from accidentally pulling in JVM-only code.
- **No `tapir-sttp-client` here.** Same reason — client-only, lives in the `client` module.

**If you put `tapir-zio` in `shared`:** the JS compile fails because ZIO isn't fully Scala.js-friendly (the `zio` artefact has JVM-only bits). The split is a hard requirement, not a stylistic preference.

## What's outside the codegen — the `runner`, `book`, and `viz` shared types

Hand-written modules under `shared/src/main/scala/cortex/shared/`:

- **`runner/CodeExecutor.scala`** (package `cortex.shared.runner`) — pure state machine for `Idle → Running → Done`. Used by `RunnableCodeBlock` (browser) and tested by zio-test (JVM). Zero Scala.js-isms; zero JVM-isms.
- **`book/SidebarForest.scala`**, **`book/CortexIndexWalker.scala`**, **`book/Blocks.scala`**, **`book/Frontmatter.scala`** (package `cortex.shared.book`) — the pure flat-list-to-tree sidebar builder, the on-disk index walker, the structural block decoders, and the frontmatter parser. Used by the server (emitting the chapter index payload) and the client (rendering the sidebar + decoding placeholders). The package is `book`, **not** `cortex`: a former nested `cortex` subpackage was renamed during the repo split so it wouldn't collide with the root `cortex.*` prefix.
- **`viz/`** (package `cortex.shared.viz`) — the data-structure-agnostic `HeapToGraph` adapter and `VizGraph` model behind the Visualise modal, plus the shared `MarkerColors` canon.

These are the shared modules we author by hand. Each one is a small, pure data structure or state machine that both sides need to agree on. Writing them in `shared` and unit-testing on the JVM means the browser inherits the test suite for free.

**Rule of thumb:** if you'd write the same code twice for both sides, write it in `shared` instead. If only one side needs it, keep it on that side.

## The smoke test

There's exactly one hand-written test in `shared/src/test/`. Its job is to import the generated `Endpoints` and assert the codegen ran. If `sbt test` passes, the generation worked and the schemas line up. We don't need exhaustive tests of the generated codecs — they're either correct (because circe-generic worked) or the import doesn't compile (because the codegen failed).

**If you add more tests in `shared`:** they'll run on both JVM and JS. Make sure they're platform-agnostic (no `java.io.File`, no `org.scalajs.dom`). zio-test runs natively on both.

## The contract is a compile-time enforcement

This is the headline benefit. Edit `api/openapi.yaml` and rename `RunRequest.language` to `RunRequest.lang`. Then:

1. `sbt sharedJVM/compile` regenerates `Endpoints.scala`. The case class now has `lang: String`.
2. The server breaks: `CodeRunPipeline.scala` references `req.language` — that field doesn't exist.
3. The client breaks: `ApiClient.runCode(RunRequest(...))` — the constructor signature changed.
4. **Both must be updated** before anything compiles.

That last bullet is the magic. The build is your migration script. You can't ship a half-renamed schema because `sbt compile` won't let you. Compare to the alternative:

| Without codegen | With codegen |
|---|---|
| Server changes `language → lang`. | YAML changes `language → lang`. |
| Client still sends `{ "language": "..." }`. | Both sides break the moment YAML is saved. |
| 200 OK at the wire layer. | Build fails at the codegen step. |
| Server pattern-matches `lang` against an empty value. | No artefact gets built. |
| Eventually somebody tests in a browser and sees "Bad input — language required". | Issue surfaces in the IDE, no commit possible. |

This is also why the [Extending the Project](/cortex/cortex-onboarding/working-on-it/extending) recipe for "add an API endpoint" insists on starting at `api/openapi.yaml` and chasing compile errors — that's the codegen safety net at work.

## Where to look first when codegen breaks

| Symptom | First place |
|---|---|
| `sbt compile` fails with "object Endpoints not found" | The codegen never ran — check `openapiSwaggerFile` path, then `sbt clean sharedJVM/compile` |
| YAML edits not picked up | Codegen caches; `sbt clean` once after a `git pull` |
| Generated case class fields look wrong | Check the YAML — required vs nullable, camelCase vs snake_case |
| OpenAPI doc at `/docs/` is stale | Server didn't restart after codegen — `sbt server/reStart` |
| One platform compiles, the other doesn't | A platform-specific import leaked into `shared` (very rare in `Pure`) |

## The one-line summary

`shared` is the place where the contract becomes code. The OpenAPI YAML is the spec, the codegen plugin emits Scala, the cross-project compiles it for both target platforms, and the rest of the codebase is downstream of that shape. Touch the spec, rebuild, watch both sides break in step — that's the feature, not a bug.
