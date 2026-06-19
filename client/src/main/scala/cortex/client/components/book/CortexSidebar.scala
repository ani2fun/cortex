package cortex.client.components.book

import cortex.client.components.icons.LucideIcons
import cortex.client.util.ReaderState
import cortex.shared.api.Endpoints.{Book, ChapterRef}
import cortex.shared.book.SidebarForest
import cortex.shared.book.SidebarForest.{Leaf, Node, Section}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Cortex reader's left-panel **Sidebar Forest**. Tree assembly lives in [[SidebarForest]] (shared,
 * JVM-tested); this file owns the React rendering plus the editorial UX bolted onto each chapter row:
 *
 *   - **Search** — top-of-rail input that filters the chapter list by title (case-insensitive substring
 *     match). ⌘K / Ctrl+K focuses; Esc clears.
 *   - **Reading-progress rail** — 3px indigo fill on the left edge of each row, height proportional to the
 *     last-saved scroll position for that chapter.
 *   - **Collapsible rail** — 280px ↔ 64px transition driven by the parent layout's `data-side` attribute.
 *     When collapsed (and not peeking), an alternate compact "rail" renders numbered chapter tiles + section
 *     icon tiles with hover tooltips.
 *   - **Hover-peek** — collapsed sidebar expands temporarily on 450ms hover; the persisted preference is
 *     unchanged. Implemented via `onMouseEnter` / `onMouseLeave` timers reporting up to the layout.
 *   - **Sync flash** — listens for `cortex:syncFlash` (dispatched by `CortexToc` on entry click); pulses the
 *     active rail tile / row for 800ms so the user sees the connection between the two TOC surfaces.
 *   - **Progress ring on the active rail tile** — writes the throttled `scrollFraction` (passed from the
 *     layout) as a CSS `--progress` custom property; the rail-tile `::after` paints a conic-gradient ring.
 */
object CortexSidebar:

  /**
   * Delay before hover-peek expands the rail. Long enough to ignore swipes-through, short enough to feel
   * responsive when the user lingers.
   */
  private val PeekInMs = 450

  /** Delay before hover-peek collapses again after mouseleave. Smooths out brief cursor escapes. */
  private val PeekOutMs = 180

  /** Sync-flash animation duration — matches the keyframes in cortex-reader.css. */
  private val FlashMs = 800

  final case class Props(
      book: Book,
      activeChapterSlug: String,
      collapsed: Boolean,
      peek: Boolean,
      scrollFraction: Double,
      onToggleCollapsed: Callback,
      onPeekChange: Boolean => Callback
  )

  private case class RowState(progress: Int, minutes: Option[Int], number: Int)

  private val EmptyRow = RowState(0, None, 0)

  private def numberLabel(n: Int): String = if n <= 0 then "" else f"$n%02d"

  private def filterChapters(chapters: Seq[ChapterRef], query: String): Seq[ChapterRef] =
    val q = query.trim.toLowerCase
    if q.isEmpty then chapters else chapters.filter(_.title.toLowerCase.contains(q))

  private def highlight(title: String, query: String): VdomNode =
    val q = query.trim
    if q.isEmpty then title
    else
      val lower = title.toLowerCase
      val ql    = q.toLowerCase
      val idx   = lower.indexOf(ql)
      if idx < 0 then title
      else
        val before = title.substring(0, idx)
        val hit    = title.substring(idx, idx + q.length)
        val after  = title.substring(idx + q.length)
        <.span(before, <.mark(^.className := "cortex-reader-sidebar__mark", hit), after)

  private def renderNode(
      node: Node,
      activeSlug: String,
      onLinkClick: Callback,
      bookSlug: String,
      query: String,
      rows: Map[String, RowState]
  ): VdomNode = node match
    case Leaf(ch) =>
      val isActive = ch.slug == activeSlug
      val cls =
        if isActive then
          "cortex-reader-sidebar__chapter-link cortex-reader-sidebar__chapter-link--active"
        else "cortex-reader-sidebar__chapter-link"
      val row = rows.getOrElse(ch.slug, EmptyRow)
      <.li(
        ^.key       := s"leaf-${ch.slug}",
        ^.className := "cortex-reader-sidebar__chapter-item",
        <.a(
          ^.href := s"/$bookSlug/${ch.slug}",
          ^.onClick --> onLinkClick,
          ^.className := cls,
          <.span(^.className := "cortex-reader-sidebar__num", numberLabel(row.number)),
          <.span(^.className := "cortex-reader-sidebar__name", highlight(ch.title, query))
        )
      )
    case s @ Section(title, depth, children) =>
      val open = s.containsActive(activeSlug) || query.trim.nonEmpty
      // The Cortex-platform capstone documents the live homelab this very site runs on. Flag it so the
      // sidebar can give it a distinct accent — a visual cue that these chapters describe the system the
      // reader is currently looking at, not a generic textbook example.
      val homelab = title.contains("Cortex Platform")
      <.li(
        ^.key := s"sec-$depth-$title",
        ^.className := {
          val base = s"cortex-reader-sidebar__section cortex-reader-sidebar__section--depth-$depth"
          if homelab then s"$base cortex-reader-sidebar__section--homelab" else base
        },
        <.details(
          ^.className := "cortex-reader-sidebar__section-details",
          ^.open      := open,
          <.summary(
            ^.className := "cortex-reader-sidebar__section-summary",
            LucideIcons.ChevronRight(
              LucideIcons.withClass("cortex-reader-sidebar__section-chevron")
            ),
            <.span(^.className := "cortex-reader-sidebar__section-name", title)
          ),
          <.ul(
            ^.className := "cortex-reader-sidebar__section-children",
            children.toTagMod(child =>
              renderNode(child, activeSlug, onLinkClick, bookSlug, query, rows)
            )
          )
        )
      )

  /**
   * Top-level walk to render the compact 64px rail. Chapter leaves at the root become numbered tiles; each
   * top-level section becomes an icon tile labelled by its title. We deliberately don't recurse past the
   * first level — the rail is for orientation, not navigation.
   */
  private def renderRail(
      forest: List[Node],
      bookSlug: String,
      activeSlug: String,
      onLinkClick: Callback,
      onToggleCollapsed: Callback,
      rows: Map[String, RowState]
  ): VdomNode =
    val chapterLeaves = forest.collect { case Leaf(ch) => ch }
    val sections      = forest.collect { case Section(t, _, _) => t }
    val showDivider   = chapterLeaves.nonEmpty && sections.nonEmpty

    <.nav(
      ^.className  := "cortex-reader-sidebar__rail",
      ^.aria.label := "Chapter rail",
      chapterLeaves.toTagMod { ch =>
        val isActive = ch.slug == activeSlug
        val row      = rows.getOrElse(ch.slug, EmptyRow)
        val cls =
          if isActive then "cortex-reader-sidebar__rail-tile cortex-reader-sidebar__rail-tile--active"
          else "cortex-reader-sidebar__rail-tile"
        <.a(
          ^.key       := s"rail-${ch.slug}",
          ^.href      := s"/$bookSlug/${ch.slug}",
          ^.className := cls,
          // Clicking any rail tile re-expands the sidebar AND navigates. The expand-on-click happens
          // first; the link follows. We don't preventDefault — let the router pick it up.
          ^.onClick --> (onToggleCollapsed >> onLinkClick),
          numberLabel(row.number),
          <.span(
            ^.className := "cortex-reader-sidebar__rail-tip",
            s"${numberLabel(row.number)} · ${ch.title}"
          )
        )
      },
      if showDivider then
        <.span(^.className := "cortex-reader-sidebar__rail-divider", ^.aria.hidden := true)
      else EmptyVdom,
      sections.toTagMod { title =>
        <.button(
          ^.key        := s"rail-sec-$title",
          ^.tpe        := "button",
          ^.className  := "cortex-reader-sidebar__rail-tile cortex-reader-sidebar__rail-tile--section",
          ^.aria.label := title,
          ^.onClick --> onToggleCollapsed,
          LucideIcons.ChevronRight(LucideIcons.withClass("cortex-reader-sidebar__rail-tile__icon")),
          <.span(^.className := "cortex-reader-sidebar__rail-tip", title)
        )
      }
    )

  private def renderTogglePill(collapsed: Boolean, onToggle: Callback): VdomNode =
    val label = if collapsed then "Expand chapter sidebar" else "Collapse chapter sidebar"
    <.button(
      ^.tpe          := "button",
      ^.className    := "cortex-reader-sidebar__toggle",
      ^.aria.label   := label,
      ^.aria.pressed := collapsed,
      ^.onClick --> onToggle,
      LucideIcons.ChevronLeft(LucideIcons.withClass("cortex-reader-sidebar__toggle-icon")),
      <.span(^.className := "cortex-reader-sidebar__toggle-label", "Collapse"),
      <.span(^.className := "cortex-reader-sidebar__toggle-kbd", "[")
    )

  private def renderInner(
      book: Book,
      activeSlug: String,
      onLinkClick: Callback,
      query: String,
      onQuery: String => Callback,
      rows: Map[String, RowState]
  ): VdomNode =
    val filtered = filterChapters(book.chapters, query)
    val forest   = SidebarForest.build(filtered)
    <.div(
      ^.className := "cortex-reader-sidebar__expanded",
      <.div(
        <.a(^.href       := "/", ^.className := "cortex-reader-sidebar__back", "← Cortex"),
        <.h2(^.className := "cortex-reader-sidebar__title", book.title),
        if book.description.nonEmpty then
          <.p(^.className := "cortex-reader-sidebar__description", book.description)
        else EmptyVdom,
        <.label(
          ^.className := "cortex-reader-sidebar__search",
          LucideIcons.Search(LucideIcons.withClass("cortex-reader-sidebar__search-icon")),
          <.input(
            ^.tpe                          := "search",
            ^.placeholder                  := "Search chapters",
            ^.aria.label                   := "Search chapters in this book",
            ^.value                        := query,
            ^.className                    := "cortex-reader-sidebar__search-input",
            VdomAttr("data-cortex-search") := "true",
            ^.onChange ==> { (e: ReactEventFromInput) => onQuery(e.target.value) },
            ^.onKeyDown ==> { (e: ReactKeyboardEvent) =>
              if e.key == "Escape" then e.preventDefaultCB >> onQuery("")
              else Callback.empty
            }
          )
        )
      ),
      <.nav(
        if forest.isEmpty then
          <.p(
            ^.className := "cortex-reader-sidebar__empty",
            "No chapters match ",
            <.em(s""""${query.trim}""""),
            "."
          )
        else
          <.ul(
            ^.className := "cortex-reader-sidebar__tree",
            forest.toTagMod(node =>
              renderNode(node, activeSlug, onLinkClick, book.slug, query, rows)
            )
          )
      )
    )

  /**
   * Pulse the active rail tile + active chapter row when the right-TOC dispatches a sync-flash event. We
   * toggle the `--flash` modifier classes via DOM-querySelector to avoid threading another piece of state
   * through the render — the flash is a one-shot animation that ends on its own.
   */
  private def flashActiveElements(): Unit =
    val tile = dom.document.querySelector(".cortex-reader-sidebar__rail-tile--active")
    val row  = dom.document.querySelector(".cortex-reader-sidebar__chapter-link--active")
    List(Option(tile), Option(row)).flatten.foreach { el =>
      el.classList.remove("cortex-reader-sidebar__rail-tile--flash")
      el.classList.remove("cortex-reader-sidebar__chapter-link--flash")
      // Force reflow so the animation re-triggers when we re-add the class.
      val _ = el.asInstanceOf[dom.html.Element].offsetWidth
      if el.classList.contains("cortex-reader-sidebar__rail-tile--active") then
        el.classList.add("cortex-reader-sidebar__rail-tile--flash")
      if el.classList.contains("cortex-reader-sidebar__chapter-link--active") then
        el.classList.add("cortex-reader-sidebar__chapter-link--flash")
      dom.window.setTimeout(
        () => {
          el.classList.remove("cortex-reader-sidebar__rail-tile--flash")
          el.classList.remove("cortex-reader-sidebar__chapter-link--flash")
        },
        FlashMs.toDouble
      )
    }

  /**
   * "Reveal in Explorer" — scroll the sidebar so the active chapter row (and rail tile in collapsed mode) is
   * visible. `block: "nearest"` makes this a no-op when the row is already on-screen and a minimal scroll
   * otherwise, so a re-click of the same chapter doesn't jitter. Deferred one tick via setTimeout(0) so
   * React's commit + the `<details open>` toggle have laid out before we measure scroll position.
   */
  private def revealActive(): Unit =
    val cb: js.Function0[Unit] = () =>
      val opts = js.Dynamic.literal(block = "center", inline = "nearest", behavior = "smooth")
      val nodes = dom.document.querySelectorAll(
        ".cortex-reader-sidebar__chapter-link--active, .cortex-reader-sidebar__rail-tile--active"
      )
      var i = 0
      while i < nodes.length do
        nodes.item(i).asInstanceOf[js.Dynamic].scrollIntoView(opts)
        i += 1
    dom.window.setTimeout(cb, 0.0)
    ()

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .useState("")
      .useRef(js.undefined: js.UndefOr[Int])
      .useRef(js.undefined: js.UndefOr[Int])
      // Sync-flash listener — fires on cortex:syncFlash custom events from CortexToc.
      .useEffectOnMountBy { (_, _, _, _, _) =>
        Callback {
          val onFlash: js.Function1[dom.Event, Unit] = (_: dom.Event) => flashActiveElements()
          dom.window.addEventListener("cortex:syncFlash", onFlash, useCapture = false)
          ()
        }
      }
      // Reveal-in-explorer: scroll the active chapter row into view on mount + whenever the URL's
      // chapter slug changes (sidebar click, prev/next, breadcrumb, direct load).
      .useEffectWithDepsBy((props, _, _, _, _) => props.activeChapterSlug) { (_, _, _, _, _) => _ =>
        Callback(revealActive())
      }
      .render { (props, openS, queryS, peekInRef, peekOutRef) =>
        val close: Callback = openS.setState(false) >> queryS.setState("")

        val storage = ReaderState.snapshotFor(props.book.chapters.map(_.slug))
        val rows: Map[String, RowState] = props.book.chapters.iterator.zipWithIndex.map {
          case (ch, idx) =>
            val (p, m) = storage.getOrElse(ch.slug, (0, None))
            ch.slug -> RowState(p, m, idx + 1)
        }.toMap

        val expandedInner =
          renderInner(
            props.book,
            props.activeChapterSlug,
            Callback.empty,
            queryS.value,
            q => queryS.setState(q),
            rows
          )

        // Forest needed twice (expanded inner + rail) but build is cheap; this avoids passing it down.
        val forest = SidebarForest.build(filterChapters(props.book.chapters, ""))
        val rail = renderRail(
          forest,
          props.book.slug,
          props.activeChapterSlug,
          Callback.empty,
          props.onToggleCollapsed,
          rows
        )

        // Hover-peek timers. Only arm when the sidebar is collapsed; cancel any pending timer when
        // the user re-enters or leaves quickly.
        val onMouseEnter: Callback = Callback {
          if props.collapsed then
            peekOutRef.value.foreach(dom.window.clearTimeout)
            peekInRef.value.foreach(dom.window.clearTimeout)
            val handle = dom.window.setTimeout(
              () => props.onPeekChange(true).runNow(),
              PeekInMs.toDouble
            )
            peekInRef.value = handle
        }
        val onMouseLeave: Callback = Callback {
          peekInRef.value.foreach(dom.window.clearTimeout)
          if props.peek then
            val handle = dom.window.setTimeout(
              () => props.onPeekChange(false).runNow(),
              PeekOutMs.toDouble
            )
            peekOutRef.value = handle
        }

        <.div(
          <.button(
            ^.tpe := "button",
            ^.onClick --> openS.setState(true),
            ^.aria.label := "Open chapters",
            ^.className  := "cortex-reader-sidebar__open-button",
            LucideIcons.Menu(LucideIcons.withClass("cortex-reader-sidebar__open-icon"))
          ),
          if openS.value then
            <.div(
              ^.className := "cortex-reader-sidebar__drawer-overlay",
              ^.onClick --> close,
              <.aside(
                ^.className := "cortex-reader-sidebar__drawer-panel",
                ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> close,
                  ^.aria.label := "Close chapters",
                  ^.className  := "cortex-reader-sidebar__close-button",
                  LucideIcons.X(LucideIcons.withClass("cortex-reader-sidebar__close-icon"))
                ),
                renderInner(
                  props.book,
                  props.activeChapterSlug,
                  close,
                  queryS.value,
                  q => queryS.setState(q),
                  rows
                )
              )
            )
          else EmptyVdom,
          <.aside(
            ^.className  := "cortex-reader-sidebar__desktop",
            ^.aria.label := "Chapter navigation",
            ^.onMouseEnter --> onMouseEnter,
            ^.onMouseLeave --> onMouseLeave,
            renderTogglePill(props.collapsed, props.onToggleCollapsed),
            // When collapsed (and not peeking), only the rail is visible; when expanded or peeking,
            // the full inner is shown. We render both so CSS can transition between them; the active
            // one is selected by the layout's `data-side` / `data-peek` attributes.
            expandedInner,
            rail
          )
        )
      }
