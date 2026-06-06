package cortex.client.components.book

import cortex.client.util.HashScroll
import cortex.client.util.ReaderState
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Right-edge vertical mini-map: a thin track with one tick per H1 / H2 / H3 in the chapter, a fill that rises
 * with scroll, and labels that surface on tile hover. Click a tick to jump.
 *
 *   - The page-level `<h1 id="chapter-top" class="cortex-reader-prose__title">` is included as the first tick
 *     at 0%. The chapter title is rendered outside `.chapter-content`, so we look it up by id and treat it as
 *     the "level 1" anchor.
 *   - H2 and H3 inside `.chapter-content` (slugged by rehype-slug) become level-2 / level-3 ticks at their
 *     measured vertical position inside the prose.
 *   - Tick width is differentiated per level via `--level-1` / `--level-2` / `--level-3` BEM modifiers —
 *     wider for shallower depths.
 *   - Ticks are rebuilt on mount, resize, and when the type-controls dispatch `cortex:typePrefsChanged`
 *     (prose height changes with font size).
 *   - Fill height tracks `ReaderState.currentScrollFraction()` on every scroll.
 *   - Active tick mirrors the right-TOC's active heading via the same `cortex:activeHeading` event the sticky
 *     bar listens to.
 *   - Hidden under 1100px (matches the preview) and in focus mode (CSS rules in `cortex-reader.css`).
 */
object CortexMiniMap:

  private case class Tick(slug: String, text: String, topPct: Double, level: Int)

  /**
   * Wait for ChapterContent to mount its dangerouslySetInnerHTML payload before measuring headings. 250ms is
   * comfortably longer than React's commit + the next browser paint, but well below the point where a user
   * notices the mini-map appearing.
   */
  private val BuildDelayMs = 250

  /**
   * Recompute tick positions. Returns an empty list if the prose container is missing or too short to
   * meaningfully map.
   *
   * Coordinate system: positions are expressed as a 0..1 fraction of the *prose* span — from the top of the
   * page-level H1 chapter title (when present) to the bottom of `.chapter-content`. Ticks land proportionally
   * inside that span regardless of how tall the mini-map track itself is.
   */
  private def measureTicks(): List[Tick] =
    val prose = dom.document.querySelector(".chapter-content")
    if prose == null then List.empty
    else
      val proseEl     = prose.asInstanceOf[dom.html.Element]
      val proseRect   = proseEl.getBoundingClientRect()
      val proseBottom = proseRect.bottom + dom.window.scrollY
      // The chapter title h1 lives outside `.chapter-content` — it's a sibling rendered by
      // ChapterPage. Anchor the top of the mapped span on it when present, otherwise on the top of
      // the prose itself.
      val title   = dom.document.querySelector("#chapter-top")
      val titleEl = if title == null then null else title.asInstanceOf[dom.html.Element]
      val topAbs =
        if titleEl != null then titleEl.getBoundingClientRect().top + dom.window.scrollY
        else proseRect.top + dom.window.scrollY
      val spanHeight = proseBottom - topAbs
      if spanHeight < 200 then List.empty
      else
        val buf = scala.collection.mutable.ListBuffer.empty[Tick]
        if titleEl != null then
          val text = Option(titleEl.textContent).getOrElse("").trim
          if text.nonEmpty then buf += Tick("chapter-top", text, 0.0, 1)
        val nodes = prose.querySelectorAll(".chapter-content h2[id], .chapter-content h3[id]")
        val len   = nodes.length
        var i     = 0
        while i < len do
          val h     = nodes.item(i).asInstanceOf[dom.html.Element]
          val abs   = h.getBoundingClientRect().top + dom.window.scrollY
          val pct   = ((abs - topAbs) / spanHeight).max(0.0).min(1.0)
          val id    = h.id
          val text  = Option(h.textContent).getOrElse("").trim
          val level = if h.tagName.equalsIgnoreCase("h2") then 2 else 3
          if id.nonEmpty && text.nonEmpty then buf += Tick(id, text, pct, level)
          i += 1
        buf.toList

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(List.empty[Tick])
      .useState(0.0)
      .useState(Option.empty[String])
      // Build ticks shortly after mount, on resize, and whenever type prefs change.
      .useEffectOnMountBy { (_, ticksS, _, _) =>
        Callback {
          def rebuild(): Unit =
            ticksS.setState(measureTicks()).runNow()

          dom.window.setTimeout(() => rebuild(), BuildDelayMs.toDouble)
          val onResize: js.Function1[dom.Event, Unit] = (_: dom.Event) => rebuild()
          val onPrefs: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            // Defer one frame so layout has reflowed under the new --reader-fs / --reader-lh / etc.
            val _ = dom.window.setTimeout(() => rebuild(), 0)
          dom.window.addEventListener("resize", onResize, useCapture = false)
          dom.window.addEventListener("cortex:typePrefsChanged", onPrefs, useCapture = false)
          ()
        }
      }
      // Scroll listener — drive the fill height.
      .useEffectOnMountBy { (_, _, fillS, _) =>
        Callback {
          val onScroll: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            fillS.setState(ReaderState.currentScrollFraction()).runNow()
          dom.window.addEventListener("scroll", onScroll, useCapture = false)
          fillS.setState(ReaderState.currentScrollFraction()).runNow()
          ()
        }
      }
      // Active-tick — listen for the same active-heading event the sticky bar uses.
      .useEffectOnMountBy { (_, _, _, activeS) =>
        Callback {
          val onActive: js.Function1[dom.CustomEvent, Unit] = (e: dom.CustomEvent) =>
            val slug = e.detail.asInstanceOf[String]
            activeS.setState(Option(slug).filter(_.nonEmpty)).runNow()
          dom.window.addEventListener(
            "cortex:activeHeading",
            onActive.asInstanceOf[js.Function1[dom.Event, Unit]],
            useCapture = false
          )
          ()
        }
      }
      .render { (_, ticksS, fillS, activeS) =>
        val ticks = ticksS.value
        if ticks.isEmpty then EmptyVdom
        else
          val active = activeS.value
          val fillStyle =
            js.Dynamic
              .literal(height = s"${(fillS.value * 100).max(0.0).min(100.0)}%")
              .asInstanceOf[js.Object]
          <.aside(
            ^.className  := "cortex-reader-minimap",
            ^.aria.label := "Article mini-map",
            <.div(
              ^.className := "cortex-reader-minimap__track",
              <.div(^.className := "cortex-reader-minimap__fill", ^.style := fillStyle),
              ticks.toTagMod { tick =>
                val isActive = active.contains(tick.slug)
                val base     = "cortex-reader-minimap__tick"
                val level    = s"cortex-reader-minimap__tick--level-${tick.level}"
                val act      = if isActive then " cortex-reader-minimap__tick--active" else ""
                <.button(
                  ^.key        := s"mm-${tick.slug}",
                  ^.tpe        := "button",
                  ^.className  := s"$base $level$act",
                  ^.aria.label := s"Jump to ${tick.text}",
                  ^.style := js.Dynamic
                    .literal(top = s"${(tick.topPct * 100).max(0.0).min(100.0)}%")
                    .asInstanceOf[js.Object],
                  ^.onClick --> Callback(HashScroll.scrollTo(tick.slug)),
                  <.span(^.className := "cortex-reader-minimap__tick-label", tick.text)
                )
              }
            )
          )
      }
