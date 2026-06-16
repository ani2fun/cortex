---
title: Client Stack — Why Each Library
summary: Scala.js, scalajs-react hooks, sttp + FetchBackend, the JS interop boundary — what each idiom buys us and what breaks if you swap it.
---

This chapter is the client-side companion to [Server Stack](/cortex/cortex-onboarding/deep-dive/server-stack). Same format: per-decision, **what we use**, **why this and not the obvious alternative**, **what breaks if you change it**. If you're new to Scala.js, the section on the hook builder is the one to internalise — every component in the codebase follows the same pattern.

The SPA's main parts — the router, the markdown/book renderer, the workbench, the coach client, and the API/auth plumbing — as a LikeC4 component view:

<iframe
  src="/c4/view/onboarding_client_components"
  width="100%"
  height="460"
  style="border: 1px solid var(--border, #2b2b2b); border-radius: 8px;"
  loading="lazy"
  title="Cortex SPA — component view"
></iframe>

## Why Scala.js + scalajs-react

The frontend is Scala 3 compiled to JavaScript via Scala.js. The React binding is `japgolly.scalajs-react` 3.0.x. Two concrete benefits:

1. **One language, two runtimes.** The same `RunRequest(language, source, stdin)` case class lives on both sides. Edit `api/openapi.yaml`, recompile, and both server and client break in step (see [Shared & Codegen](/cortex/cortex-onboarding/deep-dive/shared-and-codegen)). No copy-paste, no DTO drift.
2. **Pure logic on both sides.** `shared/runner/CodeExecutor` (package `cortex.shared.runner`) is the run-state machine: `Idle → Running → Done`. It compiles to JVM bytecode and runs in zio-test, *and* it compiles to JS and runs in the browser. Same code, two test surfaces. The component file (`client/src/main/scala/cortex/client/components/book/RunnableCodeBlock.scala`) calls this out explicitly: "State machine lives in `CodeExecutor` (shared module — testable on the JVM); this file owns the React surface only."

Alternatives we passed on:

| Stack | Why we passed |
|---|---|
| TypeScript + React | Loses the type-shared case classes; loses the cross-platform `CodeExecutor` test story. |
| Slinky (Scala.js + React via macros) | Different API, smaller community. `scalajs-react` is the de-facto choice. |
| Laminar | Excellent reactive model but no React ecosystem (lucide-react, the Monaco editor) without bridging. |

**If you remove Scala.js:** you'd be back to two implementations of `RunRequest`, two implementations of slug derivation, two test runners. The codebase doubles in size.

## The hook builder API — anatomy of a component

This is the single most important pattern in the client codebase. Every component is a `ScalaFnComponent` built with a chain of `.useX` calls. Once you internalise the shape, the rest is just plumbing.

The reference example is `client/src/main/scala/cortex/client/components/book/RunnableCodeBlock.scala`:

```scala
val Component =
  ScalaFnComponent
    .withHooks[Props]
    .useStateBy(p => CodeExecutor.initial(p.source))
    .render { (props, st) => /* … */ }
```

Three steps, each adding a parameter to every later block:

1. **`.withHooks[Props]`** opens the builder. Subsequent blocks will receive `props: Props`.
2. **`.useStateBy(initializer)`** adds a state hook. The initial value depends on `Props`, so we use the `By` variant — the initializer is `Props => State`. After this call, every later block receives `(props, st)`.
3. **`.render { (props, st) => … }`** closes the builder with a render function.

The "By" suffix runs throughout: `useState` (constant initial value), `useStateBy` (initial depends on Props), `useRef` / `useRefBy`, `useEffect` / `useEffectBy` / `useEffectWithDepsBy`. The non-`By` variant takes a value; the `By` variant takes a function from earlier hooks' values.

Each `.useX` is the Scala equivalent of one React hook. The order matters because each hook becomes a positional parameter — flip `.useState(0).useState(false)` to `.useState(false).useState(0)` and you'd have to flip every render-time access too.

For a longer-form walkthrough on a stateful diagram component see `client/src/main/scala/cortex/client/components/book/D2Slideshow.scala` — it has `useState`, `useState`, `useRefBy`, and `useEffectWithDepsBy` chained, and there's a comment explaining each.

**If you swap to React hooks-style code (a render function with inline `useState`):** scalajs-react doesn't support that shape. The hook builder is the binding; you can't escape it.

## `useEffectWithDepsBy` — the workhorse for side effects

Most useful effect-running pattern in the codebase. The shape, from `client/src/main/scala/cortex/client/components/book/ChapterContent.scala`:

```scala
.useEffectWithDepsBy((props, _, _) => props.result.html) {
  (_, articleRef, rootsRef) => _ =>
    val tearDown: Callback = Callback {
      val prev = rootsRef.value
      for i <- 0 until prev.length do Try(prev(i).unmount())
      rootsRef.value = js.Array()
    }

    val mountAll: Callback = articleRef.foreach { article =>
      // walk the article DOM, mount React components into placeholder divs
    }

    tearDown >> mountAll
}
```

Two functions stacked:

- **The `By` selector** — `(props, articleRef, rootsRef) => props.result.html` — picks out the dependency. The effect re-runs when this value changes (referential equality on the JS side).
- **The effect body** — `(props, articleRef, rootsRef) => deps => Callback`. Returns a `Callback` that runs after render commits.

The shape is ugly the first time you see it. It's also exactly the right shape: the selector decouples "what does this effect depend on" from "what does it do", which makes it easy to refactor a dependency without touching the body.

**If you wrap the body in another `Callback { … }`:** classic bug. `articleRef.foreach { article => … }` already returns a `Callback`. Wrapping the inside in `Callback { mount(...) }` constructs a new Callback that's never run — placeholders stay empty, no error fires. There's a comment on this in `ChapterContent.scala`. (See [Local Development](/cortex/cortex-onboarding/working-on-it/local-development) for the foot-gun list.)

## `useRefBy` — the leak-prevention pattern

Scalajs-react's `useRef` returns an immutable reference. `useRefBy` returns one whose initial value can be derived from earlier hooks. We use it for two purposes:

- **DOM refs** via `useRefToVdom[dom.html.Element]`, e.g. for the `<article>` element to walk after render.
- **Mutable bookkeeping** via `useRefBy(_ => js.Array[RootHandle]())`, e.g. for the array of React roots we mounted into placeholders.

The second use is what keeps `ChapterContent` from leaking. When the user navigates to a new chapter, `useEffectWithDepsBy` fires because `props.result.html` changed; the body first **tears down** the previous chapter's React roots, then mounts new ones. Without that ref, every chapter navigation would leak the previous chapter's React trees and double the DOM cost.

```scala
val tearDown: Callback = Callback {
  val prev = rootsRef.value
  for i <- 0 until prev.length do Try(prev(i).unmount())
  rootsRef.value = js.Array()
}
```

`Try(...)` because `unmount()` can throw if the underlying root is already gone (e.g. parent re-rendered without warning). Swallowing the throw is correct — we just want the ref clean.

**If you remove the tear-down:** Chrome's DevTools "React → Profiler" shows the leak immediately. After ten chapter navigations the page has ten React trees competing for the same DOM nodes.

## The placeholder walker — bridging HTML and React

Two-step rendering, fully explained in [Markdown Pipeline](/cortex/cortex-onboarding/how-it-works/markdown-pipeline) but worth the deep dive here:

1. The TS pipeline emits HTML with `<div class="runnable-code" data-source="…">` placeholders.
2. After `dangerouslySetInnerHTML` settles, the Scala walker `querySelectorAll`s each placeholder class and uses `ReactDOMClient.createRoot(node).render(…)` to mount a Scala.js React component into it.

The decode-then-dispatch split has a clean home. `BlockDiscovery.scala` (in `components/book/`) walks the article DOM and turns each placeholder into a value of the typed `Block` ADT — whose *structural* decoders live in the shared module at `cortex.shared.book.Blocks` (JVM-tested). `ChapterContent`'s `render` is then a **total `Block => VdomElement` match** (`Block.RunnableCode`, `Block.Mermaid`, `Block.D2Inline`, `Block.D3Widget`, `Block.TracedCode`, …). Adding a new interactive block is one new `Block` variant, one new component, and one new match arm — and because the match is exhaustive, the compiler won't let you forget the arm.

The pattern is the only reasonable way to mix bulk-rendered HTML and React-managed widgets in markdown. SSR isn't an option because the markdown pipeline depends on browser-only modules (D2's WASM blob, mermaid's lazy renderer). Hydration isn't an option either — there's no React tree to hydrate from, just a string of HTML.

**If you try to render markdown directly into JSX without placeholders:** you'll either hand-write a React MDX-style compiler (huge investment), or you'll lose the syntax highlighting / KaTeX / D2 / mermaid plugins, all of which are JS-native and expect to operate on an HTML string.

## `runId` — discarding stale results without cancelling them

`client/src/main/scala/cortex/client/components/book/RunnableCodeBlock.scala` has a small but illuminating piece:

```scala
def runCb: Callback = Callback.suspend {
  val snapshot  = st.value
  val codeAtRun = snapshot.code
  val nextState = CodeExecutor.started(snapshot)
  val tag       = nextState.runId
  st.modState(_ => nextState) >> Callback {
    val req = RunRequest(props.language, codeAtRun, None)
    ApiClient.runCode(req).onComplete {
      case Success(resp) => st.modState(prev => CodeExecutor.completed(prev, tag, resp.result)).runNow()
      case Failure(err)  => st.modState(prev => CodeExecutor.failed(prev, tag, err.getMessage)).runNow()
    }
  }
}
```

Each click of Run computes a new `tag` (the next runId), increments state, and fires the request. When the response arrives, the handler calls `CodeExecutor.completed(prev, tag, …)` — and `CodeExecutor` (in `shared/src/main/scala/cortex/shared/runner/CodeExecutor.scala`) checks whether `tag == prev.runId`. If not, the user has cancelled or fired another run; the late result is silently dropped.

This is necessary because **sttp's `FetchBackend` returns a plain `Future` with no cancellation hook.** The browser `fetch()` API supports `AbortController`, but the sttp backend doesn't expose it. So we can't cancel an in-flight HTTP request — we can only ignore its eventual reply.

The runId is computed inside `Callback.suspend` (deferred to *the moment the Callback runs*), not at render time. A render-time tag would work for the first run after a clean render and silently fail after a cancel-then-run, because the closed-over `s.runId` would be stale. Compute inside the lambda, not outside.

**If you swap `FetchBackend` for an sttp backend with cancellation:** you can keep the runId pattern (it doesn't hurt) and additionally call `.cancel()` on the in-flight request. Until then, the runId is the only thing standing between you and "I clicked Cancel and the old result still showed up" bugs.

## Why `FetchBackend` and not the others

`client/src/main/scala/cortex/client/api/ApiClient.scala`:

```scala
private val backend: SttpBackend[Future, Any] = FetchBackend()
```

sttp ships several backends. For Scala.js the realistic choices are:

- **`FetchBackend`** — uses the browser `fetch()` API. Streams not exposed, but cancellation could be added via `AbortController` if sttp exposed it.
- **`FetchMonixBackend`** — same, but with Monix `Task`. Adds a dependency for no benefit on this codebase.
- **`XhrBackend`** — uses `XMLHttpRequest`. Older, less ergonomic. No reason to prefer it.
- **`ZioJsBackend`** — uses ZIO on the JS side. We don't run ZIO in the browser; it would be a heavy import for one client.

`FetchBackend` is the right default and the smallest dependency surface.

**If you swap to `XhrBackend`:** nothing visible breaks, but you've added kilobytes of code that does what `fetch()` does for free.

## Why `baseUri = None` and not `Some(uri"...")`

```scala
private val baseUri: Option[Uri] = None
```

The base URI tells sttp how to construct the absolute URL for each request. `None` means: emit a relative path (`/api/run`), and let the browser resolve it against `window.location.origin`. In dev, Vite's proxy rewrites `/api/*` to `http://localhost:8080`; in prod, it goes to the same JVM that served `index.html`.

The obvious alternative — `Some(uri"")` — looks "more explicit". It also crashes at runtime: sttp's `uri"..."` interpolator rejects the empty string. There's no way to express "use whatever the browser thinks origin is" via a `Some(...)`. The only correct value is `None`.

**If you change it to `Some(uri"http://localhost:8080")`:** the dev build works. Production breaks the moment the SPA is served from a domain other than `localhost:8080`. Don't.

## The JS interop boundary — one module, not many

`client/src/main/scala/cortex/client/markdown/MarkdownRenderer.scala` is the entire contact surface between Scala and the markdown *pipeline*:

```scala
@js.native
@JSImport("@markdown/loader", "loadRenderChapter")
private def loadRenderChapter(): js.Promise[js.Function1[String, js.Promise[JsRenderResult]]] =
  js.native

def render(source: String): Future[Result] =
  for
    renderFn <- loadRenderChapter().toFuture
    js       <- renderFn(source).toFuture
  yield Result(html = js.html, toc = js.toc.toList.map(e => TocEntry(e.depth, e.slug, e.text)))
```

One `@JSImport`. One Scala-side function. Behind that single boundary lives `client/src/markdown/render.ts`, which composes 30+ remark/rehype plugins, runs the D2 WASM compiler, calls shiki, etc. None of that complexity is visible to Scala.

The alternative shape (one Scala.js facade per JS plugin) was tried in early prototyping and discarded:

- 30+ `@JSImport`s, each with its own type signature.
- Each plugin's options are an opaque JS object — you'd hand-write Scala.js facade traits with `var foo: js.UndefOr[String] = js.undefined`, repeating the JS shape twice.
- Every plugin upgrade means re-checking the facade.

Drawing the boundary at the *pipeline* level — one `renderChapter(raw): { html, toc }` function — is one boundary, one type, and one place to update when a plugin changes.

**If you start adding per-plugin facades:** stop. The client does have a handful of other `@JSImport`s — each for a *self-contained* JS dependency with a stable surface: `lucide-react` icons, `keycloak-js` (PKCE sign-in), `d3` and the D3 widget renderer, the Monaco editor (`@markdown/monaco`, the in-chapter code editor), `react-dom`'s `createPortal`, and the small `@markdown/runtime` helpers (Prism highlight, Mermaid render). The rule isn't "only one import ever" — it's "don't shred a single coherent pipeline into 30 facades". The whole remark/rehype pipeline stays behind the lone `@markdown/loader` boundary.

## Why a single `@scala-js/vite-plugin-scalajs` config

Vite handles the bundling. The Scala.js plugin's job is to expose the linker output (`client/target/scala-3.x/cortex-client-fastopt/main.js`) as an ES module Vite can resolve.

`client/vite.config.mjs`:

```javascript
scalaJSPlugin({
  cwd: "..",
  projectID: "client",
})
```

The `cwd: ".."` is the trickiest setting. Without it, the plugin invokes a fresh sbt inside `client/`, which has no `build.sbt` of its own. The plugin would silently produce nothing.

`projectID: "client"` is the sbt module name — i.e. how to find `client/fastLinkJS` in the sbt model.

**If you remove `cwd: ".."`:** you'll spend an afternoon debugging an empty bundle and a Vite log that says "module 'scalajs:main.js' not found". The fix is one line.

## Tailwind v4 + BEM — and the `@apply` footgun

Two layers of CSS in this codebase:

- **Utility classes at call sites** — `^.className := "flex items-center gap-2 …"`. These live in the Scala source.
- **BEM block stylesheets** — `client/src/styles/{sections,components}/*.css`. Each block (`.cortex-reader-toc`, `.rcb`, `.diagram`) has its own file. Inside the file, modifiers (`--active`, `--last`) are listed below the base block.

The BEM blocks use `@apply` to compose tailwind utilities into named classes. Tailwind v4's `@apply` is **stricter than v3** — unknown utilities now error at compile time and serve a blank page in Vite. The migration scrubbed several typos:

- `text-2sm` (not a tailwind class — was a typo for `text-sm`)
- `items-left` (not a flexbox alignment — `items-start` is what's meant)
- `custom-icon` (a name that looked like a utility but had no underlying CSS rule)
- `group` and `container` — these are *marker* classes, not utilities. They have zero CSS in v4 so they must stay literal in markup, not be `@apply`'d.

The other v4 surprise: `container` lost its auto-centering. We restore it explicitly in `client/tailwind.css`:

```css
@utility container {
  margin-inline: auto;
}
```

Every `<main className="container">` callsite relies on this rule. Removing it makes everything go left-flush.

**If you upgrade Tailwind v4 and remove the `@utility container` block:** the entire layout shifts left without any error. It's a visual-only failure, very easy to miss in CI.

## Theme bootstrap — why it lives in `index.html`

Dark mode is a `.dark` ancestor class on `<html>`. Set on first paint via an inline script in `client/index.html`, *before* React mounts:

```html
<script>
  (function () {
    var saved = localStorage.getItem("theme");
    var prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    var dark = saved ? saved === "dark" : prefersDark;
    if (dark) document.documentElement.classList.add("dark");
  })();
</script>
```

Why inline, not in a Scala.js module? Because the Scala.js bundle loads *after* the HTML parses. Putting the theme decision in Scala means a flash of light theme on every page load, even if the user prefers dark. Browsers are much faster than module systems at running a 10-line script.

**If you move it into a Scala.js component:** the page will flash white on every dark-mode load. Users notice this immediately.

## Bundle splitting — `manualChunks` is load-bearing

`client/vite.config.mjs`:

```javascript
build: {
  rollupOptions: {
    output: {
      manualChunks(id) {
        if (id.includes("node_modules")) {
          if (id.includes("/shiki") || id.includes("/@shikijs/")) return "shiki";
          if (id.includes("/mermaid")) return "mermaid";
          if (id.includes("/@terrastruct/d2")) return "d2";
          if (id.includes("/katex")) return "katex";
          if (id.includes("/prismjs")) return "prismjs";
        }
        return null;
      },
    },
  },
  chunkSizeWarningLimit: 800,
}
```

These libraries are huge:

- shiki: ~1MB of grammar JSON
- mermaid: ~600KB
- @terrastruct/d2: ~3MB (WASM blob)
- katex: ~280KB

Without `manualChunks` they'd be inlined in the main bundle and the home page would ship them. With them named separately, Vite emits them as on-demand chunks; the markdown pipeline `import()`s each one only when a chapter actually contains that block type. Home page bundle stays small.

`chunkSizeWarningLimit: 800` is bumped because manual chunks are *intentionally* large.

**If you remove `manualChunks`:** the home page bundle balloons by ~5MB and first-paint slows accordingly. Run `npm run build` and inspect `dist/assets/*.js` sizes after any change.

## Where to look first when something on the client breaks

| Symptom | First file |
|---|---|
| Placeholders render as empty `<div>`s | `ChapterContent.scala` — the walker bug (Callback wrapping its own body) |
| Run button stuck on "Cancel" | `RunnableCodeBlock.scala` — runId computed at render time instead of inside `Callback.suspend` |
| Hard reload of an in-app route (`/{book}/{chapter}`, `/blogs/...`) returns 404 | server `StaticRoutes.scala` (yes, server-side — the SPA fallback derivation or the on-disk book-slug scan is missing the route) |
| Page goes blank, Vite log shows "@apply rejected" | `client/src/styles/**/*.css` — typo in a `@apply` directive |
| Bundle size warning for the home chunk | `vite.config.mjs` — `manualChunks` lost a heavy dep |
| Dark mode flashes on load | `client/index.html` — bootstrap script removed or moved |

## The one-line summary

The client is a `scalajs-react` SPA where every component is a hook builder chain, every JS-side concern lives behind exactly one `@JSImport`, and every interactive markdown block is a placeholder + walker pair. Most of the difficulty in extending it is figuring out *which* hook variant you want — and once you've internalised that, the rest is plumbing.
