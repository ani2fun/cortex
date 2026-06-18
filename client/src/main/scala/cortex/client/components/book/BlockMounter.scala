package cortex.client.components.book

import cortex.client.components.book.workbench.{
  ProblemWorkbench,
  QuizBlock,
  SolutionViewerBlock,
  WorkbenchEditor,
  YourTurnBlock
}
import cortex.shared.book.Block
import japgolly.scalajs.react.vdom.VdomElement
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * The HTML→React mounting seam shared by every surface that injects rendered chapter HTML: the chapter
 * `<article>` (via [[ChapterContent]]) and the workbench tab panels / Your-Turn modal (which inject the
 * packaged `<template>` HTML and need the same placeholder discovery + component mounting + decoration
 * semantics for nested blocks — d3 widgets, solution viewers, quizzes, mermaid, …).
 *
 * Owns: the `ReactDOMClient.createRoot` facade, the total `Block => VdomElement` dispatch, and the
 * post-injection DOM decorations (heading anchors, code-block chrome). [[ChapterContent]] keeps the
 * article-only concerns (click delegation, hash scroll, hook lifecycle).
 */
object BlockMounter:

  // We can't depend on japgolly's createRoot wrapper here (its API is wrapped); call ReactDOMClient
  // directly via a tiny facade.
  @js.native
  @js.annotation.JSImport("react-dom/client", "createRoot")
  private def createRoot(container: dom.Node): RootHandle = js.native

  @js.native
  trait RootHandle extends js.Object:
    def render(node: js.Any): Unit = js.native
    def unmount(): Unit            = js.native

  /**
   * Per-mount context threaded to the blocks that need page identity the packaged HTML can't carry —
   * currently just the coach `problemId` (`<book>/<chapter-slug>`) so an inline Your-Turn can open a live
   * coaching session. Defaults to empty, so non-chapter mounts (workbench panels, generic fallbacks) are
   * unaffected and their coach degrades to the static prompts.
   */
  final case class MountContext(coachProblemId: Option[String] = None)

  object MountContext:
    val empty: MountContext = MountContext()

  /**
   * Mount a scalajs-react `Unmounted` into a DOM container by routing it through `ReactDOMClient.createRoot`.
   * Returns the root handle so the caller can unmount on cleanup.
   */
  def mount(container: dom.Node, vdom: VdomElement): RootHandle =
    val root = createRoot(container)
    root.render(vdom.rawElement.asInstanceOf[js.Any])
    root

  /**
   * Inject `html` into `container`, mount a component onto every discovered placeholder, and apply the
   * standard decorations. Returns the mounted roots — callers own their teardown (pass back to [[teardown]]
   * when the surface unmounts or re-injects).
   */
  def mountInto(
      container: dom.html.Element,
      html: String,
      ctx: MountContext = MountContext.empty
  ): js.Array[RootHandle] =
    container.innerHTML = html
    val roots = js.Array[RootHandle]()
    BlockDiscovery.discover(container).blocks.foreach { case (node, block) =>
      val _ = roots.push(mount(node, render(block, ctx)))
    }
    decorateAnchorLinks(container)
    decorateCodeBlocks(container)
    roots

  /**
   * Unmount every root, swallowing the React "already unmounted" complaints on double-teardown. The actual
   * unmounts are deferred a tick: teardown is typically called from an effect cleanup that runs while React
   * is still committing the parent tree (modal close, chapter change), and synchronously unmounting a nested
   * root there trips React 18's "unmount a root while React was already rendering" race warning. The roots
   * array is snapshotted because callers reset it immediately after this returns.
   */
  def teardown(roots: js.Array[RootHandle]): Unit =
    val snapshot = roots.jsSlice()
    val _ = dom.window.setTimeout(
      () => for i <- 0 until snapshot.length do Try(snapshot(i).unmount()),
      0
    )

  // Total `Block => VdomElement` dispatch. Adding a new Block variant breaks the match
  // exhaustively at compile time — the missing-case error names exactly what's missing.
  def render(block: Block, ctx: MountContext = MountContext.empty): VdomElement = block match
    case Block.RunnableCode(language, source, languageLabel, viz, vizRoot, vizCase, vizKind) =>
      RunnableCodeBlock.Component(
        RunnableCodeBlock.Props(
          language,
          source,
          languageLabel,
          viz = viz,
          vizRoot = vizRoot,
          vizCase = vizCase,
          vizKind = vizKind
        )
      )
    case Block.RunnableGroup(tabs) =>
      RunnableCodeGroup.Component(RunnableCodeGroup.Props(tabs.map(toGroupTab)))
    case Block.WorkbenchInline(tabs, spec) =>
      WorkbenchEditor.Component(WorkbenchEditor.Props(tabs, spec))
    case yt: Block.YourTurn =>
      YourTurnBlock.Component(YourTurnBlock.Props(yt, ctx.coachProblemId))
    case pw: Block.ProblemWorkbench =>
      ProblemWorkbench.Component(ProblemWorkbench.Props.fromBlock(pw))
    case q: Block.Quiz =>
      QuizBlock.Component(QuizBlock.Props(q))
    case Block.SolutionViewer(tabs, time, space) =>
      SolutionViewerBlock.Component(SolutionViewerBlock.Props(tabs, time, space))
    case Block.Mermaid(source) =>
      MermaidBlock.Component(MermaidBlock.Props(source))
    case Block.StandaloneCoach(overrideId) =>
      StandaloneCoachBlock.Component(StandaloneCoachBlock.Props(overrideId.orElse(ctx.coachProblemId)))
    case Block.D2Slides(slides, caption) =>
      D2Slideshow.Component(D2Slideshow.Props(slides, caption))
    case Block.D2Inline(svgHtml) =>
      D2Diagram.Component(D2Diagram.Props(svgHtml))
    case Block.D3Widget(widget, payload) =>
      D3WidgetBlock.Component(D3WidgetBlock.Props(widget, payload))
    case Block.TracedCode(language, source, companions) =>
      TracedCodeBlock.Component(
        TracedCodeBlock.Props(
          language,
          source,
          companions.map(c => TracedCodeBlock.Companion(c.language, c.source))
        )
      )
    case Block.LikeC4(src, height, title) =>
      LikeC4Block.Component(LikeC4Block.Props(src, height, title))

  private def toGroupTab(t: Block.Tab): RunnableCodeGroup.Tab =
    RunnableCodeGroup.Tab(
      language = t.language,
      languageLabel = t.languageLabel,
      source = t.source,
      runnable = t.runnable,
      viz = t.viz,
      vizRoot = t.vizRoot,
      vizCase = t.vizCase,
      vizKind = t.vizKind
    )

  /**
   * Inject a small `#` anchor link to the left of every `<h2>` / `<h3>` / `<h4>` that has an `id`. Click
   * copies the deep link + flashes "Copied"; ChapterContent's click delegation owns that path.
   */
  private[book] def decorateAnchorLinks(article: dom.html.Element): Unit =
    val headings = article.querySelectorAll(":scope h2[id], :scope h3[id], :scope h4[id]")
    val len      = headings.length
    var i        = 0
    while i < len do
      val h = headings.item(i).asInstanceOf[dom.html.Element]
      // Skip if we've already decorated (e.g. on a re-mount the new innerHTML wipes children, but be
      // defensive in case rehype-autolink-headings also added something we'd duplicate).
      val first   = h.firstElementChild
      val already = first != null && first.classList.contains("cortex-prose-anchor")
      if !already then
        val a = dom.document.createElement("a").asInstanceOf[dom.html.Element]
        a.setAttribute("class", "cortex-prose-anchor")
        a.setAttribute("href", s"#${h.id}")
        a.setAttribute("aria-label", s"Copy link to ${h.textContent}")
        a.innerHTML =
          """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
             </svg>"""
        val _ = h.insertBefore(a, h.firstChild)
      i += 1

  /**
   * Wrap every standalone `<pre>` in a `.cortex-code-block` div with a header strip showing the language
   * (read from `pre[data-language]` if the markdown pipeline set it) and a Copy button. Standalone means "not
   * already inside one of our custom blocks" — runnable code / traced code own their own chrome.
   */
  private[book] def decorateCodeBlocks(article: dom.html.Element): Unit =
    val pres = article.querySelectorAll(":scope pre")
    val len  = pres.length
    var i    = 0
    while i < len do
      val pre    = pres.item(i).asInstanceOf[dom.html.Element]
      val parent = pre.parentElement
      val alreadyWrapped =
        parent != null && parent.classList.contains("cortex-code-block")
      // `.pwb__html` (workbench Description/Editorial panels) opts out too: problem examples are
      // quiet left-rule boxes there (styled by workbench.css), not full code cards with a
      // language header + Copy button.
      val insideCustom =
        pre.closest(
          ".runnable-code, .runnable-code-group, .traced-code, .mermaid, .d2-slideshow, .d2-diagram, .d3-widget, .likec4-block, .workbench-inline, .workbench-solution, .pwb__html"
        ) != null
      if !alreadyWrapped && !insideCustom then
        // Language hint: rehype-highlight sets a `language-xxx` class on the inner <code> element;
        // fall back to pre.dataset.language if the pipeline set it directly.
        val code = pre.querySelector("code")
        val langClass =
          if code != null then
            val classes = code.getAttribute("class")
            if classes == null then ""
            else
              classes
                .split("\\s+")
                .find(_.startsWith("language-"))
                .map(_.stripPrefix("language-"))
                .getOrElse("")
          else ""
        val language =
          if langClass.nonEmpty then langClass
          else
            pre.getAttribute("data-language") match
              case null => ""
              case s    => s
        val filename = pre.getAttribute("data-filename") match
          case null => ""
          case s    => s

        val wrapper = dom.document.createElement("div").asInstanceOf[dom.html.Element]
        wrapper.setAttribute("class", "cortex-code-block")
        val head = dom.document.createElement("div").asInstanceOf[dom.html.Element]
        head.setAttribute("class", "cortex-code-block__head")
        val langSpan =
          if language.nonEmpty then
            val s = dom.document.createElement("span").asInstanceOf[dom.html.Element]
            s.setAttribute("class", "cortex-code-block__lang")
            s.textContent = language.toUpperCase
            Some(s)
          else None
        val fileSpan =
          if filename.nonEmpty then
            val s = dom.document.createElement("span").asInstanceOf[dom.html.Element]
            s.setAttribute("class", "cortex-code-block__file")
            s.textContent = filename
            Some(s)
          else None
        val copyBtn = dom.document.createElement("button").asInstanceOf[dom.html.Element]
        copyBtn.setAttribute("type", "button")
        copyBtn.setAttribute("class", "cortex-code-block__copy")
        copyBtn.setAttribute("data-state", "idle")
        copyBtn.setAttribute("aria-label", "Copy code to clipboard")
        copyBtn.innerHTML =
          """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
             </svg>
             <span class="cortex-code-block__copy-label">Copy</span>"""
        copyBtn.addEventListener(
          "click",
          ((_: dom.Event) =>
            val clipboard = dom.window.navigator.asInstanceOf[js.Dynamic].clipboard
            if !js.isUndefined(clipboard) && clipboard != null then
              val _ = clipboard.writeText(pre.textContent)
            copyBtn.setAttribute("data-state", "copied")
            val label = copyBtn.querySelector(".cortex-code-block__copy-label")
            if label != null then label.textContent = "Copied"
            val _ = dom.window.setTimeout(
              () => {
                copyBtn.setAttribute("data-state", "idle")
                if label != null then label.textContent = "Copy"
              },
              1400.0
            )
            ()
          ): js.Function1[dom.Event, Unit]
        )
        langSpan.foreach(head.appendChild)
        fileSpan.foreach(head.appendChild)
        head.appendChild(copyBtn)

        // Insert wrapper in place of pre, then move pre inside wrapper.
        val anchor = pre.parentElement
        if anchor != null then
          val _ = anchor.insertBefore(wrapper, pre)
          val _ = wrapper.appendChild(head)
          val _ = wrapper.appendChild(pre)
      i += 1
