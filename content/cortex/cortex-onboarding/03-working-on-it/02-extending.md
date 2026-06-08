---
title: Extending the Project
summary: Three concrete recipes — add a Cortex chapter, add an API endpoint, add a runnable language. Follow these and you can do almost any feature work.
---

## Recipe 1: add a book chapter

The smallest meaningful change. The book (Cortex) is **convention over configuration** — the on-disk layout under `content/cortex/` *is* the index. There is no central manifest. Drop files in the right place and they show up.

1. Pick a book — say `system-design`. Find the section directory it should live in (e.g. `02-foundations/`).
2. Drop a markdown file there with a numeric prefix to control ordering:

   ```yaml
   ---
   title: Quorums in Practice
   summary: How read/write quorums actually behave under partial failure.
   ---

   ## Section 1
   ...
   ```

   Save as e.g. `content/cortex/system-design/02-foundations/03-quorums-in-practice.md`.

3. Reload the chapter URL in the browser. With `CORTEX_AUTO_RELOAD=true` (the dev default) the server re-walks the tree on the next request and the new chapter appears immediately. In prod, redeploy.

That's it — no manifest to edit. The chapter slug is derived from the path: `02-foundations/03-quorums-in-practice.md` → the route `/system-design/foundations-quorums-in-practice` (numeric prefixes stripped, segments joined with `-`). The browser path is root-based — there's no `/cortex/` prefix any more, though old `/cortex/<book>/<slug>` links still redirect to it.

If the title in the URL or sidebar comes out wrong, check the fallback chain: frontmatter `title:` → first `# H1` in the body → humanised filename.

### Adding a new book

Same idea, one level up. Make a new directory under `content/cortex/`:

```
content/cortex/
└── my-book/                       ← dirname becomes the book slug
    ├── book.json                  ← OPTIONAL — overrides the auto-derived metadata
    ├── 01-intro.md                ← chapter at the book root
    └── 02-deep-dive/              ← directory = collapsible sidebar section
        ├── _section.json          ← OPTIONAL — `{ "title": "Deep dive" }`
        ├── 01-part-one.md
        └── 02-internals/          ← arbitrary nesting (capped at 6 levels)
            └── 01-details.md
```

`book.json` (optional) lets you override the auto-derived title and add metadata:

```json
{
  "title": "My Book",
  "description": "What it's about.",
  "tags": ["..."],
  "estimatedReadingMinutes": 20
}
```

`_section.json` (optional, per directory) overrides the section's display title in the sidebar (defaults to the humanised dirname).

The book index at `/` (`CortexIndexPage`) pulls from `/api/cortex/index`, which auto-discovers every book directory under `content/cortex/`. No other code change needed — a new directory shows up as a new card.

### Bulk import from another markdown source

For larger imports (e.g. an mdbook tree), write a small script that walks the external source, mirrors the directory structure, rewrites internal cross-links to cortex URLs, and auto-tags supported fenced code blocks (the languages catalogued in `Languages.scala`) with ` run`.

## Recipe 2: add an API endpoint

This one threads through every layer. Take it slowly the first time.

```d2
direction: down
spec: api/openapi.yaml
shared: shared (codegen) {
  case_class: case classes
  endpoint: tapir Endpoint values
}
pipeline: server/myFeaturePipeline/MyPipeline.scala
wire: server/http/ApiRoutes.scala
client: client/api/ApiClient.scala
component: client/components/...

spec -> shared.case_class
spec -> shared.endpoint
shared.case_class -> pipeline
shared.endpoint -> wire
wire -> pipeline
shared.case_class -> client
shared.endpoint -> client
client -> component
```

### Step-by-step

1. **Edit `api/openapi.yaml`.** Add the path, request body schema, response body schema, and any error schema. Reference shared `ApiError` for failures so error shapes stay consistent across the API. Use camelCase for JSON fields — circe codecs match that by default.

2. **Run `sbt compile`.** Codegen emits new case classes and a new `Endpoints.<yourEndpoint>` value into `shared/`. Both server and client now break — that's the codegen safety net at work.

3. **Write the pipeline.** Create a new deep-module package `server/.../myFeaturePipeline/MyPipeline.scala` (one public trait + a `live` layer, mirroring `HelloPipeline` / `CortexPipeline`):

   ```scala
   trait MyPipeline:
     def doThing(req: MyRequest): IO[MyFailure, MyResponse]

   final class MyPipelineLive(/* injected services */) extends MyPipeline:
     def doThing(req: MyRequest): IO[MyFailure, MyResponse] = ???

   object MyPipeline:
     val live: ZLayer[Deps, Nothing, MyPipeline] = ZLayer.fromFunction(MyPipelineLive(_))

   enum MyFailure:
     case Invalid(detail: String)
     case NotFound
   println("Pipeline boundary uses domain failures, not HTTP codes.")
   ```

   The pipeline is **not** HTTP-aware. It returns domain failures; the HTTP layer maps them to status codes. Add `MyFailure` to the `HandlerFailure` union in `http/ApiErrors.scala` and a case to its `toHttp` match — the compiler then forces you to map the new error before it'll build.

4. **Wire it up in `http/ApiRoutes.scala`.** `ApiRoutes` composes every pipeline's tapir endpoints (`HttpApp` just binds the whole set to zio-http). Use the `handlerEndpoint` helper so the `(StatusCode, ApiError)` error output and the `HandlerFailure → toHttp` mapping are wired the same way as every other endpoint (ADR-0012):

   ```scala
   private val myEndpoint: ZServerEndpoint[Any, Any] =
     handlerEndpoint(Endpoints.myEndpoint) { req =>
       myPipeline.doThing(req)   // mapError(ApiErrors.toHttp) is applied by handlerEndpoint
     }
   println("Add to the apiEndpoints list; Main.scala provides the layer.")
   ```

5. **Add `MyPipeline.live` to `Main.scala`'s `provide(...)` block.**

6. **Add the client method to `ApiClient.scala`.** sttp + tapir gives this for free:

   ```scala
   private val doThingRequest:
       MyRequest => sttp.client3.Request[Either[Unit, MyResponse], Any] =
     SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.myEndpoint, baseUri)

   def doThing(req: MyRequest): Future[MyResponse] =
     backend.send(doThingRequest(req)).flatMap {
       case res if res.body.isRight => Future.successful(res.body.toOption.get)
       case res                     => Future.failed(RuntimeException(s"Failed (${res.code.code})"))
     }
   println("Same shape as runCode, getCortexIndex, etc.")
   ```

7. **Use it in a component.** Standard `useEffectWithDepsBy(...) { ApiClient.doThing(req).onComplete { ... } }` shape.

### Common mistakes

- Forgetting step 5 (the layer). The compiler will complain about an unsatisfied dependency, but the message is verbose; don't panic.
- Adding HTTP status codes inside the pipeline instead of in `http/ApiErrors.scala`. Keep the pipeline pure-domain; only the wiring layer should know about HTTP.
- Forgetting to extend the `HandlerFailure` union and its `toHttp` match. The union is exhaustive, so a new failure type that isn't mapped is a compile error — that's the safety net, not an obstacle. Using `handlerEndpoint` (rather than hand-rolling `errorOut(...)` + `mapError`) wires the uniform error output for you; skip it and any failure becomes a 500 with no body.

## Recipe 3: add a runnable language

Touches both server (catalog) and client (Prism syntax highlighting).

1. **Add to `server/.../codeRunPipeline/Languages.scala`.** Each entry has an id, label, aliases, and a `GoJudgeSpec` — the source filename plus the shell `compile` (optional) and `run` commands go-judge executes inside the sandbox, e.g. `GoJudgeSpec(sourceFile = "main.rb", compile = None, run = "ruby main.rb")`. The toolchain for that language must also be installed in the go-judge image (`runner/go-judge/Dockerfile`).

2. **Add to `client/src/markdown/runtime.ts`'s Prism setup.** Prism needs the grammar imported and registered:

   ```ts
   import "prismjs/components/prism-elixir";
   ```

   Without this, `highlightWithPrism(code, "elixir")` falls back to plain text and the editor shows un-highlighted source.

3. **Update the OpenAPI enum, if you use one.** `RunRequest.language` is a free-form string in the spec, but if you've added validation, extend the validator to include the new language.

4. **Smoke test.** Open a chapter, write ` ```ruby run ` ... ``` , and click Run. If go-judge is up and the `GoJudgeSpec` commands are right, output appears. If not, check the server log and the go-judge container: an unknown alias returns `RunFailure.BadInput` (400); a missing toolchain surfaces as a compile/runtime error in the `RunResult`.

## Recipe 4: change the look of a chapter

This one is split between two files. The split surprises people.

- **HTML emitted** by the markdown pipeline → edit `client/src/markdown/render.ts` and the `codeHandler` / pre-passes within it.
- **CSS for the rendered HTML** → edit `client/src/styles/components/chapter-content.css`. The `.chapter-content` rule there owns the prose typography and headings/code/img overrides. For specific elements, edit the matching BEM stylesheet:
  - `client/src/styles/components/diagrams.css` for D2 + Mermaid (`.diagram`, `.diagram-modal`, `.mermaid`)
  - `client/src/styles/components/runnable-code.css` for the runnable editor + tab group (`.rcb`, `.rcg`)
  - `client/src/styles/components/cortex-reader.css` for the sidebar / TOC / pager / breadcrumb shells (`.cortex-reader-sidebar`, `.cortex-reader-toc`, `.cortex-reader-pager`, `.cortex-reader-breadcrumb`)

When in doubt, **inspect the element in dev tools**. Two name-mapping tricks:

1. The placeholder class names emitted by the markdown pipeline map back to component files: `runnable-code` placeholder → `RunnableCodeBlock.scala`, `d2-diagram` → `D2Diagram.scala`, `mermaid-block` → `MermaidBlock.scala`. Each is the entire React tree for that placeholder, and is grep-able.
2. The BEM class names map back to stylesheet files by their block prefix: `.cortex-reader-toc__link--active` → `client/src/styles/components/cortex-reader.css`, `.rcb__run-button` → `client/src/styles/components/runnable-code.css`. One file per block; the suffixes (`__element`, `--modifier`) are local to that file.

## Recipe 5: rename or add a top-level route

The single source of truth for the SPA route topology is `AppRoutes.SpaRoutes` in `shared/.../AppRoutes.scala` (ADR-0009): the client router builds its rules from it, and the server's `index.html` fallback derives from it. So for a *new top-level* route:

1. Add a `SpaRoute(segment, hasNestedRoutes)` entry to `AppRoutes.SpaRoutes`, and a matching `Page` case + route rule in `client/.../Page.scala` and `client/.../Router.scala`.
2. That's usually it for the server — `server/.../http/StaticRoutes.scala` derives the SPA fallback from `AppRoutes.SpaRoutes`, so a hard reload of the new route returns `index.html` automatically. (Book-slug routes are handled separately: `StaticRoutes` also lists the book directories it finds on disk.)

Before this was derived, the server fallback was a hand-maintained list and forgetting to update it meant in-app navigation worked but hard reloads 404'd — a real bug we shipped. The derivation removes that footgun: add the route in one place and both runtimes pick it up.

## Recipe 6: bump a dependency

- **Scala / Java**: edit `build.sbt`, run `sbt update`, then `sbt compile` and `sbt test`. Watch for binary-incompatible bumps in tapir, zio, and circe — they go in waves.
- **JS**: edit `client/package.json`, then `cd client && npm install`. Run `npm run build` to confirm the bundle still builds. Heavy deps (mermaid, d2, shiki, katex) deserve a manual size check after upgrading: a 200KB regression on the home page is a regression worth catching.

## A general principle

When you're not sure where a change should go, **start with the OpenAPI spec or the markdown content**, never with the implementation. Both are pure data, both have schema validation, and both are read by code on multiple sides. If you can express the change as "the contract is now X" or "the content is now Y", the implementation falls out of the codegen and the existing rendering logic.

The places where the codebase becomes complicated are the places where we *can't* push down to data — JS interop, React state in placeholders, and the go-judge wire mapping (`GoJudgeWire`). Treat those as cost centers and keep them small.
