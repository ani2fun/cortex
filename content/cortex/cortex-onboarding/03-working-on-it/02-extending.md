---
title: Extending the Project
summary: Three concrete recipes — add a Cortex chapter, add an API endpoint, add a runnable language. Follow these and you can do almost any feature work.
---

## Recipe 1: add a Cortex chapter

The smallest meaningful change. Cortex is **convention over configuration** — the on-disk layout under `content/cortex/` *is* the index. There is no central manifest. Drop files in the right place and they show up.

1. Pick a book — say `distributed-systems`. Find the section directory it should live in (e.g. `02-foundations/`).
2. Drop a markdown file there with a numeric prefix to control ordering:

   ```yaml
   ---
   title: Quorums in Practice
   summary: How read/write quorums actually behave under partial failure.
   ---

   ## Section 1
   ...
   ```

   Save as e.g. `content/cortex/distributed-systems/02-foundations/03-quorums-in-practice.md`.

3. Reload the chapter URL in the browser. With `CORTEX_AUTO_RELOAD=true` (the dev default) the server re-walks the tree on the next request and the new chapter appears immediately. In prod, redeploy.

That's it — no manifest to edit. The chapter slug is derived from the path: `02-foundations/03-quorums-in-practice.md` → `/cortex/distributed-systems/foundations-quorums-in-practice` (numeric prefixes stripped, segments joined with `-`).

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

The home page's "Cortex" preview pulls from `/api/cortex/index` which auto-discovers every book directory under `content/cortex/`. No other code change needed.

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
handler: server/handlers/MyHandler.scala
wire: server/HttpApp.scala
client: client/api/ApiClient.scala
component: client/components/...

spec -> shared.case_class
spec -> shared.endpoint
shared.case_class -> handler
shared.endpoint -> wire
wire -> handler
shared.case_class -> client
shared.endpoint -> client
client -> component
```

### Step-by-step

1. **Edit `api/openapi.yaml`.** Add the path, request body schema, response body schema, and any error schema. Reference shared `ApiError` for failures so error shapes stay consistent across the API. Use camelCase for JSON fields — circe codecs match that by default.

2. **Run `sbt compile`.** Codegen emits new case classes and a new `Endpoints.<yourEndpoint>` value into `shared/`. Both server and client now break — that's the codegen safety net at work.

3. **Write the handler.** Create `server/.../handlers/MyHandler.scala`:

   ```scala
   trait MyHandler:
     def doThing(req: MyRequest): IO[MyFailure, MyResponse]

   final class MyHandlerLive(/* injected services */) extends MyHandler:
     def doThing(req: MyRequest): IO[MyFailure, MyResponse] = ???

   object MyHandler:
     val live: ZLayer[Deps, Nothing, MyHandler] = ZLayer.fromFunction(MyHandlerLive(_))

   enum MyFailure:
     case Invalid(detail: String)
     case NotFound

   object MyFailure:
     def toApiError(f: MyFailure): ApiError = f match
       case Invalid(d) => ApiError("Invalid input", Some(d), None)
       case NotFound   => ApiError("Not found", None, None)
   println("Handler boundary uses domain failures, not HTTP codes.")
   ```

   The handler is **not** HTTP-aware. It returns domain failures; the HTTP layer maps them to status codes.

4. **Wire it up in `HttpApp.scala`.** Pattern-match on the failure to choose a status:

   ```scala
   private val myEndpoint: ZServerEndpoint[Any, Any] =
     Endpoints.myEndpoint
       .errorOut(statusCode and jsonBody[ApiError])
       .zServerLogic { req =>
         myHandler.doThing(req).mapError { f =>
           val status = f match
             case MyFailure.Invalid(_) => StatusCode.BadRequest
             case MyFailure.NotFound   => StatusCode.NotFound
           (status, MyFailure.toApiError(f))
         }
       }
   println("Add to apiEndpoints: List[...]; Main.scala provides the layer.")
   ```

5. **Add the live layer to `Main.scala`'s `provide(...)` block.**

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
- Adding HTTP status codes inside the handler instead of in `HttpApp.scala`. Keep the handler pure-domain; only the wiring layer should know about HTTP.
- Skipping the `errorOut(...)` pairing on the endpoint. Without it, any failure becomes a 500 with no body — the client has nothing to display.

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
2. The BEM class names map back to stylesheet files by their block prefix: `.experience__role-card` → `client/src/styles/sections/experience.css`, `.cortex-reader-toc__link--active` → `client/src/styles/components/cortex-reader.css`. One file per block; the suffixes (`__element`, `--modifier`) are local to that file.

## Recipe 5: rename a route

Two places to change, in this order:

1. `client/.../Page.scala` and `client/.../Router.scala` — add the new case, route rule, and any links that point to the old route.
2. `server/.../HttpApp.scala`'s `staticRoutes` — add the new path to the SPA fallback list. **Without this, a hard reload of the new route will return 404.**

If you forget step 2, the SPA works for in-app navigation but reloads die. That's a real bug we've shipped before.

## Recipe 6: bump a dependency

- **Scala / Java**: edit `build.sbt`, run `sbt update`, then `sbt compile` and `sbt test`. Watch for binary-incompatible bumps in tapir, zio, and circe — they go in waves.
- **JS**: edit `client/package.json`, then `cd client && npm install`. Run `npm run build` to confirm the bundle still builds. Heavy deps (mermaid, d2, shiki, katex) deserve a manual size check after upgrading: a 200KB regression on the home page is a regression worth catching.

## A general principle

When you're not sure where a change should go, **start with the OpenAPI spec or the markdown content**, never with the implementation. Both are pure data, both have schema validation, and both are read by code on multiple sides. If you can express the change as "the contract is now X" or "the content is now Y", the implementation falls out of the codegen and the existing rendering logic.

The places where the codebase becomes complicated are the places where we *can't* push down to data — JS interop, React state in placeholders, and the go-judge wire mapping (`GoJudgeWire`). Treat those as cost centers and keep them small.
