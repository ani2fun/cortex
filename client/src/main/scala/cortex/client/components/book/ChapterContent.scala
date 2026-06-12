package cortex.client.components.book

import cortex.client.markdown.MarkdownRenderer
import cortex.client.util.HashScroll
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Owns the article half of the Chapter pipeline: drops the rendered HTML into an `<article>` and delegates
 * placeholder discovery + component mounting + decorations to [[BlockMounter]] (the seam shared with the
 * workbench tab panels and the Your-Turn modal).
 *
 * Discovery + structural validation live in `BlockDiscovery` (DOM walk + JS shims) and `shared.book.Blocks`
 * (pure decoders, JVM-tested). This module owns only the article-side React concerns: hook lifecycle, root
 * teardown across chapter changes, click delegation for in-article anchors, and hash scrolling.
 *
 * Roots are unmounted on each chapter change so we don't leak component trees.
 */
object ChapterContent:

  final case class Props(result: MarkdownRenderer.Result)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useRefToVdom[dom.html.Element]
      // Track the React roots we mount into placeholder divs. Ref so they survive re-renders.
      .useRefBy(_ => js.Array[BlockMounter.RootHandle]())
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
            BlockMounter.teardown(rootsRef.value)
            rootsRef.value = js.Array()
          }

          // articleRef.foreach's lambda is `Element => Unit`; the body runs imperatively when the
          // resulting Callback fires. Wrapping the body in `Callback { … }` would silently drop it
          // (a `-Wvalue-discard` warning at compile time, no mount at runtime — the bug that left
          // mermaid/runnable/d2 placeholders empty).
          val mountAll: Callback = articleRef.foreach { article =>
            article.addEventListener("click", onArticleClick)
            rootsRef.value = BlockMounter.mountInto(article, html)
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
