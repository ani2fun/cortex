package cortex.client.components.book

import cortex.client.markdown.MarkdownRenderer
import cortex.client.util.ReaderState
import cortex.shared.api.Endpoints.Book
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Owns the reader's outer chrome: the 2-column grid (sidebar | main), the floating action stack (back-to-top,
 * focus, type, TOC), and the cross-cutting state that the chrome shares — sidebar collapse, hover-peek, focus
 * mode, the throttled scroll fraction, and the active heading.
 *
 *   - **Collapse** persists to `localStorage["cortex-reader.sideCollapsed"]`. Auto-collapses below 1200px
 *     until the user takes manual control via the toggle pill or the `[` key, after which auto-collapse stops
 *     nagging for the session.
 *   - **Hover-peek** is a temporary expand on rail hover (450ms in, 180ms out) that does NOT touch the
 *     persisted preference — peek state lives in the layout so the grid CSS can react.
 *   - **Focus mode** (`F` key) toggles `body.cortex-focus-mode` which fades every chrome surface (header,
 *     sidebar, fabs, both TOCs, sticky bar, mini-map) and recentres the prose at 760px.
 *   - **Scroll fraction** + **active heading** are computed here once per tick and threaded down to the
 *     sidebar, sticky bar, and mini-map — avoids N parallel scroll listeners.
 */
object CortexReaderLayout:

  /** Viewport width below which the sidebar auto-collapses (until the user toggles manually). */
  private val AutoCollapseBreakpoint = 1200

  /** Throttle for the shared scroll telemetry (sidebar progress, mini-map fill, sticky bar). */
  private val ScrollThrottleMs = 200

  /** localStorage key for the user's explicit collapse preference. */
  private val CollapseStorageKey = "cortex-reader.sideCollapsed"

  final case class Props(
      book: Book,
      activeChapterSlug: String,
      toc: List[MarkdownRenderer.TocEntry],
      content: VdomNode,
      chapterTitle: String
  )

  private def readStoredCollapse(): Option[Boolean] =
    Option(dom.window.localStorage.getItem(CollapseStorageKey)).flatMap {
      case "1" => Some(true)
      case "0" => Some(false)
      case _   => None
    }

  private def writeStoredCollapse(v: Boolean): Unit =
    dom.window.localStorage.setItem(CollapseStorageKey, if v then "1" else "0")

  private def isEditableTarget(e: dom.KeyboardEvent): Boolean =
    val t = e.target.asInstanceOf[dom.Element]
    t != null && (t.matches("input, textarea, [contenteditable], [contenteditable='true']"))

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      // collapsed | peek | focused | scrollFraction | activeHeadingSlug
      .useState(false)
      .useState(false)
      .useState(false)
      .useState(0.0)
      .useState(Option.empty[String])
      // userOverride — true once the user toggles collapse manually this session.
      // We don't trigger re-renders on changes, so a ref is the right shape.
      .useRef(false)
      // On mount: read localStorage, fire initial auto-collapse decision, install resize listener.
      .useEffectOnMountBy { (_, collapsedS, _, _, _, _, overrideRef) =>
        Callback {
          readStoredCollapse() match
            case Some(v) =>
              overrideRef.value = true
              collapsedS.setState(v).runNow()
            case None =>
              if dom.window.innerWidth < AutoCollapseBreakpoint then
                collapsedS.setState(true).runNow()

          val onResize: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            if !overrideRef.value then
              val narrow = dom.window.innerWidth < AutoCollapseBreakpoint
              if narrow != collapsedS.value then collapsedS.setState(narrow).runNow()
          dom.window.addEventListener("resize", onResize, useCapture = false)
          ()
        }
      }
      // `[` key — toggle collapse (skip when typing in inputs).
      .useEffectOnMountBy { (_, collapsedS, _, _, _, _, overrideRef) =>
        Callback {
          val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
            if !isEditableTarget(e) && e.key == "[" && !e.metaKey && !e.ctrlKey && !e.altKey then
              e.preventDefault()
              overrideRef.value = true
              // `modState`, not `setState(!collapsedS.value)`: this listener is
              // installed once on mount, so `collapsedS.value` is frozen at the
              // mount-render value. The functional update form always receives
              // the current state, so `[` toggles both ways instead of only
              // ever collapsing.
              collapsedS
                .modState { current =>
                  val next = !current
                  writeStoredCollapse(next)
                  next
                }
                .runNow()
          dom.window.addEventListener("keydown", onKey, useCapture = false)
          ()
        }
      }
      // `F` key — toggle focus mode; `Esc` — exit focus mode. Both run off a
      // mount-installed listener, so they use `modState` to act on the *live*
      // state rather than the stale captured `focusedS.value`.
      .useEffectOnMountBy { (_, _, _, focusedS, _, _, _) =>
        Callback {
          val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
            if !isEditableTarget(e) then
              if (e.key == "f" || e.key == "F") && !e.metaKey && !e.ctrlKey && !e.altKey then
                e.preventDefault()
                // Same mount-installed stale-closure trap as the `[` handler
                // above — `modState` so `F` toggles focus mode both ways.
                focusedS.modState(!_).runNow()
              else if e.key == "Escape" then
                // `modState`, not `setState(false)` gated on `focusedS.value`:
                // the captured `focusedS.value` is frozen at the mount-render
                // value (`false`), so the old guard never passed and `Esc`
                // never exited focus mode. Setting `false` unconditionally is a
                // harmless no-op (no re-render) when not already focused.
                focusedS.modState(_ => false).runNow()
          dom.window.addEventListener("keydown", onKey, useCapture = false)
          ()
        }
      }
      // Reflect focused state onto <body> so cortex-reader.css + header.css can fade chrome.
      .useEffectWithDepsBy((_, _, _, focusedS, _, _, _) => focusedS.value) {
        (_, _, _, _, _, _, _) => isFocused =>
          Callback {
            val body = dom.document.body
            if isFocused then body.classList.add("cortex-focus-mode")
            else body.classList.remove("cortex-focus-mode")
          }
      }
      // Shared scroll telemetry — single throttled listener for every consumer (sidebar progress,
      // sticky bar, mini-map fill, resume-reading persistence reads the same fraction directly).
      .useEffectOnMountBy { (_, _, _, _, fractionS, _, _) =>
        Callback {
          var pending: js.UndefOr[Int] = js.undefined
          val onScroll: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            pending.foreach(dom.window.clearTimeout)
            pending = dom.window.setTimeout(
              () => {
                val f = ReaderState.currentScrollFraction()
                fractionS.setState(f).runNow()
              },
              ScrollThrottleMs.toDouble
            )
          dom.window.addEventListener("scroll", onScroll, useCapture = false)
          fractionS.setState(ReaderState.currentScrollFraction()).runNow()
          ()
        }
      }
      // Active heading — listen for the cortex:activeHeading custom event dispatched by CortexToc.
      // The right-TOC owns the IntersectionObserver; the layout subscribes so it can feed the
      // sticky bar without duplicating the observer.
      .useEffectOnMountBy { (_, _, _, _, _, activeS, _) =>
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
      .render { (props, collapsedS, peekS, focusedS, fractionS, activeHeadingS, overrideRef) =>
        val chapters      = props.book.chapters
        val activeIdx     = chapters.indexWhere(_.slug == props.activeChapterSlug)
        val chapterNumber = if activeIdx >= 0 then activeIdx + 1 else 0

        val onToggleCollapsed: Callback = Callback {
          overrideRef.value = true
          val next = !collapsedS.value
          writeStoredCollapse(next)
          collapsedS.setState(next).runNow()
        }

        val onPeekChange: Boolean => Callback = on => peekS.setState(on)

        val onToggleFocused: Callback = focusedS.setState(!focusedS.value)

        val activeSection: Option[String] =
          activeHeadingS.value.flatMap(slug => props.toc.find(_.slug == slug).map(_.text))

        val sideAttr = if collapsedS.value then "collapsed" else "expanded"
        val peekAttr = if peekS.value && collapsedS.value then "on" else ""

        <.div(
          ^.className           := "cortex-reader-layout",
          VdomAttr("data-side") := sideAttr,
          VdomAttr("data-peek") := peekAttr,
          CortexSidebar.Component(
            CortexSidebar.Props(
              book = props.book,
              activeChapterSlug = props.activeChapterSlug,
              collapsed = collapsedS.value,
              peek = peekS.value,
              scrollFraction = fractionS.value,
              onToggleCollapsed = onToggleCollapsed,
              onPeekChange = onPeekChange
            )
          ),
          <.main(^.className := "cortex-reader-layout__main", props.content),
          CortexStickyBar.Component(
            CortexStickyBar.Props(
              chapterNumber = chapterNumber,
              chapterTitle = props.chapterTitle,
              activeSectionTitle = activeSection
            )
          ),
          CortexMiniMap.Component(),
          CortexResumeReading.Component(),
          CortexSelectionActions.Component(),
          CortexTypePrefs.Component(),
          CortexToc.Component(CortexToc.Props(props.toc)),
          CortexFocusMode.Component(
            CortexFocusMode.Props(focused = focusedS.value, onToggle = onToggleFocused)
          ),
          ScrollToTop.Component()
        )
      }
