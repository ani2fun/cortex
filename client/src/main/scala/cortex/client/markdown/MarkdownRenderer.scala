package cortex.client.markdown

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Single-purpose facade in front of `client/src/markdown/render.ts`.
 *
 * The TS module owns the entire `unified` / `remark` / `rehype` pipeline — we don't write per-plugin Scala.js
 * facades. Instead, we cross the JS boundary exactly once (per chapter) and get back a `(html, toc)` pair.
 *
 * The pipeline itself is loaded **lazily** via a dynamic-import gateway in `loader.ts`, so the multi-MB
 * shiki/mermaid/d2/katex code only lands when a chapter page first mounts — pages that never render markdown
 * stay blissfully unaware.
 */
object MarkdownRenderer:

  // ---- Raw JS bindings ---------------------------------------------------

  @js.native
  private trait JsTocEntry extends js.Object:
    val depth: Int   = js.native
    val slug: String = js.native
    val text: String = js.native

  @js.native
  private trait JsRenderResult extends js.Object:
    val html: String              = js.native
    val toc: js.Array[JsTocEntry] = js.native

  /**
   * Resolves to a `renderChapter` function via dynamic-import. The loader caches the inner Promise on the JS
   * side, so subsequent calls are cheap.
   */
  @js.native @JSImport("@markdown/loader", "loadRenderChapter")
  private def loadRenderChapter(): js.Promise[js.Function1[String, js.Promise[JsRenderResult]]] =
    js.native

  // ---- Public API --------------------------------------------------------

  final case class TocEntry(depth: Int, slug: String, text: String)
  final case class Result(html: String, toc: List[TocEntry])

  /**
   * Render a chapter. The returned Future fails if the pipeline throws — mostly KaTeX / D2 syntax errors in
   * the source.
   */
  def render(source: String): Future[Result] =
    for
      renderFn <- loadRenderChapter().toFuture
      js       <- renderFn(source).toFuture
    yield Result(
      html = js.html,
      toc = js.toc.toList.map(e => TocEntry(e.depth, e.slug, e.text))
    )
