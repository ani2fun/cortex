package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * Dark-inverted popover that surfaces above a text selection inside the chapter prose. Four actions:
 *
 *   - **Copy** — plain text to clipboard.
 *   - **Quote** — text wrapped in curly quotes + a `— Aniket Kakde` attribution tail.
 *   - **Highlight** — wraps the selection in a `<mark>` with indigo accents. `surroundContents` is used
 *     inside a try/catch — it throws on cross-block selections, which we swallow (the user gets the visual
 *     nothing-happened feedback, no console noise). Highlights are DOM-only by design; persisting them across
 *     reloads is a follow-up.
 *   - **Define** — opens the first word of the selection in Wiktionary in a new tab. Pragmatic fallback for
 *     the common "look this up" intent.
 *
 * The popover is positioned in document coordinates from `Range.getBoundingClientRect()` (+ `scrollY`), so it
 * stays attached to the selection even after the page scrolls under it.
 */
object CortexSelectionActions:

  /** Minimum selection length before the popover appears — single-char accidents shouldn't open it. */
  private val MinChars = 2

  /** Milliseconds the per-button "Done" state is shown before snapping back. */
  private val DoneMs = 900

  private enum Act:
    case Copy, Quote, Highlight, Define

  private def labelFor(a: Act): String = a match
    case Act.Copy      => "Copy"
    case Act.Quote     => "Quote"
    case Act.Highlight => "Highlight"
    case Act.Define    => "Define"

  /**
   * Restrict the popover to selections that live entirely inside `.chapter-content`. Selections that span
   * outside (e.g. a header click + drag into the prose) are ignored.
   */
  private def isInsideProse(node: dom.Node): Boolean =
    val prose = dom.document.querySelector(".chapter-content")
    prose != null && (prose == node || prose.contains(node))

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(false)                      // open
      .useState((0.0, 0.0))                 // (left, top) in document coords
      .useState(Option.empty[Act])          // last action — drives the "Done" indicator
      .useRef(null.asInstanceOf[dom.Range]) // last selection's range, for Highlight
      .useEffectOnMountBy { (_, openS, posS, _, rangeRef) =>
        Callback {
          val onSelection: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            // Defer one frame so the selection has settled before we read it. requestAnimationFrame
            // is the standard debounce for selectionchange.
            val _ = dom.window.asInstanceOf[js.Dynamic].requestAnimationFrame { (_: Double) =>
              val sel = dom.window.getSelection()
              if sel == null || sel.isCollapsed || sel.rangeCount == 0 then
                openS.setState(false).runNow()
              else
                val range = sel.getRangeAt(0)
                if !isInsideProse(range.commonAncestorContainer) then
                  openS.setState(false).runNow()
                else
                  val text = sel.toString.trim
                  if text.length < MinChars then openS.setState(false).runNow()
                  else
                    rangeRef.value = range
                    val rect = range.getBoundingClientRect()
                    val left = rect.left + rect.width / 2 + dom.window.scrollX
                    val top  = rect.top + dom.window.scrollY
                    posS.setState((left, top)).runNow()
                    openS.setState(true).runNow()
              ()
            }
          dom.document.addEventListener("selectionchange", onSelection, useCapture = false)

          // Dismiss when the user clicks outside the popover (the popover itself stops propagation).
          val onDown: js.Function1[dom.MouseEvent, Unit] = (e: dom.MouseEvent) =>
            val target = e.target.asInstanceOf[dom.Element]
            val pop    = dom.document.querySelector(".cortex-reader-selpop")
            if pop == null || (target != null && !pop.contains(target)) then
              openS.setState(false).runNow()
          dom.document.addEventListener("mousedown", onDown, useCapture = false)
          ()
        }
      }
      .render { (_, openS, posS, doneActS, rangeRef) =>
        val (left, top) = posS.value
        val style = js.Dynamic
          .literal(left = s"${left}px", top = s"${top}px")
          .asInstanceOf[js.Object]

        def perform(act: Act): Callback = Callback {
          val sel  = dom.window.getSelection()
          val text = if sel != null then sel.toString.trim else ""
          act match
            case Act.Copy =>
              dom.window.navigator.asInstanceOf[js.Dynamic].clipboard.writeText(text)
              ()
            case Act.Quote =>
              val quoted = "“" + text + "”" + "\n\n— Aniket Kakde"
              dom.window.navigator.asInstanceOf[js.Dynamic].clipboard.writeText(quoted)
              ()
            case Act.Highlight =>
              val range = rangeRef.value
              if range != null then
                val mark = dom.document.createElement("mark")
                mark.setAttribute("class", "cortex-reader-highlight")
                Try(range.surroundContents(mark)).getOrElse(())
                if sel != null then sel.removeAllRanges()
              ()
            case Act.Define =>
              val first =
                text.split("\\s+").headOption.getOrElse("").replaceAll("[^A-Za-z\\-']", "")
              if first.nonEmpty then
                val url = s"https://en.wiktionary.org/wiki/${js.Dynamic.global.encodeURIComponent(first)}"
                val _   = dom.window.open(url, "_blank")
              ()
          doneActS.setState(Some(act)).runNow()
          dom.window.setTimeout(
            () => {
              doneActS.setState(None).runNow()
              openS.setState(false).runNow()
            },
            DoneMs.toDouble
          )
          ()
        }

        def actionButton(act: Act, icon: VdomNode): VdomNode =
          val isDone  = doneActS.value.contains(act)
          val display = if isDone then "Done" else labelFor(act)
          <.button(
            ^.key                  := s"selpop-${labelFor(act)}",
            ^.tpe                  := "button",
            VdomAttr("data-act")   := labelFor(act),
            VdomAttr("data-state") := (if isDone then "done" else ""),
            ^.aria.label           := labelFor(act),
            ^.onClick --> perform(act),
            icon,
            <.span(display)
          )

        <.div(
          ^.className           := "cortex-reader-selpop",
          VdomAttr("data-open") := (if openS.value then "true" else "false"),
          ^.role                := "toolbar",
          ^.aria.label          := "Selection actions",
          ^.style               := style,
          ^.onMouseDown ==> { (e: ReactMouseEvent) => e.stopPropagationCB },
          actionButton(Act.Copy, LucideIcons.Copy(LucideIcons.withClass("cortex-reader-selpop__icon"))),
          actionButton(Act.Quote, LucideIcons.Quote(LucideIcons.withClass("cortex-reader-selpop__icon"))),
          <.span(^.className := "cortex-reader-selpop__divider", ^.aria.hidden := true),
          actionButton(
            Act.Highlight,
            LucideIcons.Highlighter(LucideIcons.withClass("cortex-reader-selpop__icon"))
          ),
          actionButton(
            Act.Define,
            LucideIcons.BookMarked(LucideIcons.withClass("cortex-reader-selpop__icon"))
          )
        )
      }
