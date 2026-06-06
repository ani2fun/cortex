package cortex.client.components.book

import cortex.client.markdown.MarkdownRenderer
import cortex.client.util.HashScroll
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * Owns the post-render mounting half of the Chapter pipeline: drops the rendered HTML into an `<article>` via
 * `dangerouslySetInnerHTML`, asks [[BlockDiscovery]] to find every Cortex Block placeholder, and React-
 * portal-mounts a Scala.js component into each one.
 *
 * Discovery + structural validation live in `BlockDiscovery` (DOM walk + JS shims) and `shared.cortex.Blocks`
 * (pure decoders, JVM-tested). This module owns only the React-side concerns: hook lifecycle, root teardown
 * across chapter changes, click delegation for in-article anchors, and the total `Block => VdomElement`
 * dispatch.
 *
 * Roots are unmounted on each chapter change so we don't leak component trees.
 */
object ChapterContent:

  final case class Props(result: MarkdownRenderer.Result)

  // We can't depend on japgolly's createRoot wrapper here (its API is wrapped); call ReactDOMClient
  // directly via a tiny facade.
  @js.native
  @js.annotation.JSImport("react-dom/client", "createRoot")
  private def createRoot(container: dom.Node): RootHandle = js.native

  @js.native
  private trait RootHandle extends js.Object:
    def render(node: js.Any): Unit = js.native
    def unmount(): Unit            = js.native

  /**
   * Mount a scalajs-react `Unmounted` into a DOM container by routing it through `ReactDOMClient.createRoot`.
   * Returns the root handle so the caller can unmount on cleanup.
   */
  private def mount(container: dom.Node, vdom: VdomElement): RootHandle =
    val root = createRoot(container)
    root.render(vdom.rawElement.asInstanceOf[js.Any])
    root

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useRefToVdom[dom.html.Element]
      // Track the React roots we mount into placeholder divs. Ref so they survive re-renders.
      .useRefBy(_ => js.Array[RootHandle]())
      // Rebuild whenever the rendered HTML changes (a new chapter). We set the article's innerHTML
      // IMPERATIVELY here, NOT via dangerouslySetInnerHTML — so React never re-applies it on an
      // UNRELATED re-render. Opening / closing the Visualise modal re-renders an ancestor; a
      // dangerouslySetInnerHTML reset would wipe the React roots we mount into the placeholders —
      // blanking the chapter on close AND tearing down the just-opened modal's RunnableCodeBlock on
      // open (the modal vanishes the instant it appears). With imperative innerHTML the article has
      // NO React children, so React leaves its contents — and the mounted roots / any open modal —
      // untouched across re-renders; only an html change re-fires this effect and rebuilds it.
      .useEffectWithDepsBy((props, _, _) => props.result.html) {
        (_, articleRef, rootsRef) => html =>
          val tearDown: Callback = Callback {
            val prev = rootsRef.value
            for i <- 0 until prev.length do Try(prev(i).unmount())
            rootsRef.value = js.Array()
          }

          // articleRef.foreach's lambda is `Element => Unit`; the body runs imperatively when the
          // resulting Callback fires. Wrapping the body in `Callback { … }` would silently drop it
          // (a `-Wvalue-discard` warning at compile time, no mount at runtime — the bug that left
          // mermaid/runnable/d2 placeholders empty).
          val mountAll: Callback = articleRef.foreach { article =>
            article.innerHTML = html
            article.addEventListener("click", onArticleClick)

            val newRoots = js.Array[RootHandle]()
            BlockDiscovery.discover(article).blocks.foreach { case (node, block) =>
              val _ = newRoots.push(mount(node, render(block)))
            }
            rootsRef.value = newRoots
            decorateAnchorLinks(article)
            decorateCodeBlocks(article)
            val _ = dom.window.setTimeout(() => HashScroll.scrollToCurrentHash(), 0)
          }

          tearDown >> mountAll
      }
      // No unmount-only cleanup hook: useEffectWithDepsBy(html) tears down the previous roots on each
      // chapter change; on a full ChapterContent unmount the placeholder DOM goes too, so the orphaned
      // roots are harmless GC garbage.
      .render { (props, articleRef, _) =>
        <.article.withRef(articleRef)(^.className := "chapter-content")
      }

  // Total `Block => VdomElement` dispatch. Adding a new Block variant breaks the match
  // exhaustively at compile time — the missing-case error names exactly what's missing.
  private def render(block: Block): VdomElement = block match
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
    case Block.Mermaid(source) =>
      MermaidBlock.Component(MermaidBlock.Props(source))
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

  // Delegated click handler for in-article hash links. The rehype-autolink-headings pass adds
  // `<a href="#slug">` to each heading, and TOC bullets in markdown also resolve to the same shape.
  // We find the anchor (if any) and hand off to HashScroll, which owns the bypass-router-and-scroll
  // logic shared with CortexToc.
  private val onArticleClick: js.Function1[dom.MouseEvent, Unit] = (e: dom.MouseEvent) =>
    val target = e.target.asInstanceOf[dom.Element]
    val anchor =
      if target != null && target.matches("a[href^='#']") then target
      else if target != null then target.closest("a[href^='#']")
      else null
    if anchor != null && !e.defaultPrevented then
      val href = anchor.getAttribute("href")
      if href != null && href.length > 1 then
        // The new "anchor pill" we inject prepends a "#"-icon link with class .cortex-prose-anchor.
        // Clicking it copies the deep link to the clipboard + flashes a "Copied" badge, in addition
        // to scrolling. Other in-article links keep the existing scroll-only behaviour.
        if anchor.classList.contains("cortex-prose-anchor") then
          e.preventDefault()
          val slug = href.substring(1)
          HashScroll.scrollTo(slug)
          dom.window.history.replaceState(null, "", s"#$slug")
          val url       = dom.window.location.href.split("#").headOption.getOrElse("") + s"#$slug"
          val clipboard = dom.window.navigator.asInstanceOf[js.Dynamic].clipboard
          if !js.isUndefined(clipboard) && clipboard != null then
            val _ = clipboard.writeText(url)
          anchor.setAttribute("data-copied", "true")
          val _ = dom.window.setTimeout(
            () => anchor.setAttribute("data-copied", "false"),
            1200.0
          )
          ()
        else HashScroll.onHashLinkNative(e, href.substring(1))

  /**
   * Inject a small `#` anchor link to the left of every `<h2>` / `<h3>` / `<h4>` that has an `id`. Click
   * copies the deep link + flashes "Copied"; the click handler above owns that path.
   */
  private def decorateAnchorLinks(article: dom.html.Element): Unit =
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
  private def decorateCodeBlocks(article: dom.html.Element): Unit =
    val pres = article.querySelectorAll(":scope pre")
    val len  = pres.length
    var i    = 0
    while i < len do
      val pre    = pres.item(i).asInstanceOf[dom.html.Element]
      val parent = pre.parentElement
      val alreadyWrapped =
        parent != null && parent.classList.contains("cortex-code-block")
      val insideCustom =
        pre.closest(
          ".runnable-code, .runnable-code-group, .traced-code, .mermaid, .d2-slideshow, .d2-diagram, .d3-widget, .likec4-block"
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
