---
title: The Markdown Pipeline
summary: How a `.md` file becomes the chapter you're reading. The unified pipeline, the placeholder pattern, and why we cross the JS boundary exactly once.
---

## The boundary problem

The remark / rehype ecosystem is JS-native. Each plugin is a small JS module; the pipeline is composed by chaining them. We could write Scala.js facades for each plugin (~30 of them), but that's a lot of boilerplate that doesn't pay for itself.

So we draw the boundary differently: **one TypeScript module owns the entire pipeline, and Scala calls it exactly once per chapter.** That module is `client/src/markdown/render.ts`. Scala-side, the contact surface is exactly this:

```scala
// In client/src/main/scala/cortex/client/markdown/MarkdownRenderer.scala
@js.native
@JSImport("@markdown/loader", "loadRenderChapter")
private def loadRenderChapter(): js.Promise[js.Function1[String, js.Promise[JsRenderResult]]] = js.native

def render(raw: String): Future[Result] =
  for
    renderFn <- loadRenderChapter().toFuture   // dynamic-import the pipeline on first use
    rendered <- renderFn(raw).toFuture
  yield toResult(rendered)
println("One import. One Future per chapter. Done.")
```

That's the entire JS-interop layer for chapter rendering. Everything else is regular Scala on one side and regular TS on the other.

## What the pipeline does

```d2
direction: down

raw: "raw markdown\n(string)" {
  shape: page
}

parse: remark-parse
preMermaid: extract mermaid sources\n(visit + cache)
preD2: render D2 → SVG\n(WASM compile + render)
preRunnable: merge adjacent\n```<lang> run``` fences
gfm: remark-gfm\n(tables, strikethrough)
math: remark-math + rehype-katex
shiki: rehype-pretty-code\n(shiki highlight)
slug: rehype-slug\n(stable id per heading)
toc: collect headings → TOC
toJsx: codeHandler\n(emit placeholder divs)
stringify: rehype-stringify

result: "{ html, toc }" {
  shape: page
}

raw -> parse
parse -> preMermaid
preMermaid -> preD2
preD2 -> preRunnable
preRunnable -> gfm
gfm -> math
math -> shiki
shiki -> slug
slug -> toc
toc -> toJsx
toJsx -> stringify
stringify -> result
```

The order matters:

- **mermaid extraction first** so the source is available later, after `shiki` has highlighted the rest.
- **D2 compilation** runs ahead of code-fence handling so a compiled SVG is on the AST when `codeHandler` decides what to emit.
- **Runnable merge** turns adjacent ` ```python run ` and ` ```javascript run ` fences into a single `runnable-group` AST node, which downstream becomes the tab UI.
- **Math before shiki** because `rehype-katex` doesn't need to see syntax-highlighted output, and shiki running over math source would corrupt it.

## The placeholder pattern

This is the single most important idea in the rendering pipeline. Internalise it, and the rest follows.

We can't render React inside a string of HTML. But we *can* render a `<div>` with a known class and some `data-*` attributes. Then we can walk the article DOM after mount and mount React components into those divs.

`render.ts`'s `codeHandler` emits these placeholders:

```html
<div class="runnable-code" data-lang="python" data-source="<encoded>"></div>
<div class="runnable-group" data-tabs="<encoded JSON array of tabs>"></div>
<div class="mermaid-block" data-mermaid-source="<encoded>"></div>
<div class="d2-diagram"><svg viewBox="...">...</svg></div>
```

D2 is special: it's pre-rendered by the WASM engine before stringification, so the SVG is **already in the HTML**. The placeholder div is there only to attach a React component (the zoom modal); the SVG inside survives untouched.

The walker on the Scala side lives in `client/src/main/scala/cortex/client/components/book/ChapterContent.scala`. The relevant shape is:

```scala
// Pseudo-Scala — see ChapterContent.scala for the full version.
val mountAll: Callback = articleRef.foreach { article =>
  val newRoots = js.Array[RootHandle]()

  for div <- article.querySelectorAll("div.runnable-code") do
    val node = div.asInstanceOf[dom.HTMLElement]
    val lang = node.getAttribute("data-lang")
    val src  = decode(node.getAttribute("data-source"))
    val vdom = RunnableCodeBlock.Component(RunnableCodeBlock.Props(lang, src))
    newRoots.push(mount(node, vdom))   // ReactDOMClient.createRoot(node).render(vdom)

  // ... same for runnable-group, mermaid-block, d2-diagram

  rootsRef.value = newRoots
}

println("HTML in. React out. One walker per chapter.")
```

There are two subtle bugs the codebase has already paid for, both worth knowing:

1. **Don't wrap the body in `Callback { … }`.** `articleRef.foreach { article => … }` already returns a `Callback`. Wrapping its body in another `Callback { … }` makes the `mount(...)` calls happen in a discarded inner Callback — placeholders stay empty in the DOM and there's no runtime error. There's a comment on this in `ChapterContent.scala`; if you see empty placeholders, this is the first thing to check.
2. **Track the roots and unmount them.** Without that, every chapter navigation leaks the previous chapter's React trees. The `useRefBy` holding `js.Array[RootHandle]` plus a tear-down pass at the start of every effect run is what prevents this.

## Lazy loading

The pipeline pulls in **a lot** of code: shiki ships ~1MB of grammar JSON, mermaid is ~600KB, the D2 WASM blob is ~3MB. We don't want any of that on the home page.

```d2
direction: right

home: "CortexIndexPage (/)" {
  shape: page
}
chapter: ChapterPage {
  shape: page
}
loader: client/src/markdown/loader.ts
render: render.ts pipeline
chunks: shiki / mermaid / d2 / katex chunks {
  style.fill: "#cce5ff"
}

home -> loader: never imported
chapter -> loader: dynamic import on first chapter
loader -> render
render -> chunks: dynamic import\nper plugin
```

Two layers of laziness:

1. **`loader.ts`** is the only module Scala imports directly. It does `await import("./render")` the first time it's called and caches the result. So merely loading the SPA bundle doesn't pull in `render.ts` at all.
2. **Inside `render.ts`**, the heavy plugins (`shiki`, `mermaid`, `@terrastruct/d2`, `katex`) are themselves loaded with `await import("...")`. Vite's `manualChunks` setting (in `client/vite.config.mjs`) names these chunks so they ship as separate files browsers can fetch on demand and cache forever.

The book index at `/` (and the blog index at `/blogs`) doesn't touch any of this. Only `ChapterPage` and `BlogPostPage` do.

## Where each format gets handled

| Format | Plugin / file | Output |
| --- | --- | --- |
| Headings, paragraphs, links | remark-parse + rehype-stringify | normal HTML |
| Tables, strikethrough, autolinks | remark-gfm | normal HTML |
| `$math$` | remark-math + rehype-katex | KaTeX-rendered span |
| ` ```python ` (no `run`) | rehype-pretty-code (shiki) | syntax-highlighted `<pre>` |
| ` ```python run ` (single) | custom codeHandler | `<div class="runnable-code">` placeholder |
| Adjacent ` ```py run ` + ` ```js run ` | custom merge pre-pass | `<div class="runnable-group">` placeholder |
| ` ```mermaid ` | mermaid pre-pass + codeHandler | `<div class="mermaid-block">` placeholder |
| ` ```d2 ` | D2 WASM pre-pass + codeHandler | `<div class="d2-diagram">` with SVG inside |

## Quick mental check

If you change `render.ts`:

- Did you add a new placeholder class? **Add a corresponding walker branch in `ChapterContent.scala`**, otherwise the placeholder will sit empty.
- Did you add a new dependency? **Update `vite.config.mjs`'s `manualChunks`** so it doesn't bloat the home page.
- Did you change the data shape going to a placeholder? **Update both `render.ts` and the matching Scala component's Props decoding** (the walker calls `decodeURIComponent` on `data-*` attrs — the pair of encoder and decoder must agree).
