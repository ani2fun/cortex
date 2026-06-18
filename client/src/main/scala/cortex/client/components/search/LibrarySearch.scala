package cortex.client.components.search

import cortex.client.Page
import cortex.client.api.ApiClient
import cortex.client.components.icons.LucideIcons
import cortex.client.util.{AsyncFetch, AsyncResult}
import cortex.shared.api.Endpoints.{BlogIndex, BlogSummary, Book, ChapterRef, CortexIndex}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * Library-wide search palette — the ⌘K command bar in the redesigned nav. One surface searches the **whole
 * library**: every book, every chapter, and every blog post. Built from the design's `.navsearch` + a modal
 * lifted from the `SignInModal` pattern.
 *
 * Two pieces, decoupled by a window `CustomEvent` (the codebase's `cortex:*` idiom):
 *
 *   - [[Trigger]] — the centered search affordance rendered inside the header. It owns no state; clicking it
 *     dispatches `cortex:openLibrarySearch`.
 *   - [[Component]] — the modal host, mounted once in [[cortex.client.Layout]] **outside** the
 *     (blur-filtered) header so its `position:fixed` overlay isn't trapped in a containing block. It holds
 *     open/query/active state, installs the global ⌘K listener, fetches the two indices once, and navigates
 *     via the `RouterCtl` handed down from Layout — so a result click is SPA navigation, no full reload.
 *
 * Filtering reuses the in-book search convention from `CortexSidebar` (lowercase substring + `<mark>`
 * highlight) — deliberately not a regex, since Scala.js's `Pattern` chokes on inline flags.
 */
object LibrarySearch:

  /** The custom event the header trigger dispatches; the host listens for it (alongside ⌘K). */
  val OpenEvent = "cortex:openLibrarySearch"

  /** Cap rendered rows so a one-letter query can't paint hundreds of nodes. */
  private val MaxResults = 30

  final case class Props(ctl: RouterCtl[Page])

  // ── search model ──────────────────────────────────────────────────────────

  /** A single searchable thing, carrying enough to render a row and route on click. */
  private enum Hit:
    case BookHit(b: Book)
    case ChapterHit(b: Book, c: ChapterRef)
    case PostHit(p: BlogSummary)

  private enum Kind:
    case Books, Chapters, Writing

  private def kindOf(h: Hit): Kind = h match
    case Hit.BookHit(_)       => Kind.Books
    case Hit.ChapterHit(_, _) => Kind.Chapters
    case Hit.PostHit(_)       => Kind.Writing

  private def kindLabel(k: Kind): String = k match
    case Kind.Books    => "Books"
    case Kind.Chapters => "Chapters"
    case Kind.Writing  => "Writing"

  private def hitTitle(h: Hit): String = h match
    case Hit.BookHit(b)       => b.title
    case Hit.ChapterHit(_, c) => c.title
    case Hit.PostHit(p)       => p.title

  /** Secondary line: where the hit lives, so two same-named chapters are distinguishable. */
  private def hitContext(h: Hit): String = h match
    case Hit.BookHit(b) =>
      val n = b.chapters.size
      s"Book · $n ${if n == 1 then "chapter" else "chapters"}"
    case Hit.ChapterHit(b, c) =>
      (b.title :: c.groupPath.toList.flatten.toList).mkString(" · ")
    case Hit.PostHit(p) => s"Writing · ${p.publishedAt}"

  /** SPA route for a hit — books redirect to their first chapter (`Page.BookRedirect`). */
  private def hitTarget(h: Hit): Page = h match
    case Hit.BookHit(b)       => Page.BookRedirect(b.slug)
    case Hit.ChapterHit(b, c) => Page.Chapter(b.slug, c.slug)
    case Hit.PostHit(p)       => Page.BlogPost(p.slug)

  /** Real href so the row supports middle-click / open-in-new-tab; the onClick intercepts for SPA nav. */
  private def hitHref(h: Hit): String = h match
    case Hit.BookHit(b)       => s"/${b.slug}"
    case Hit.ChapterHit(b, c) => s"/${b.slug}/${c.slug}"
    case Hit.PostHit(p)       => s"/blogs/${p.slug}"

  private def iconFor(h: Hit): VdomNode = h match
    case Hit.BookHit(_)       => LucideIcons.BookOpen(LucideIcons.withClass("library-search__row-glyph"))
    case Hit.ChapterHit(_, _) => LucideIcons.BookMarked(LucideIcons.withClass("library-search__row-glyph"))
    case Hit.PostHit(_)       => LucideIcons.Pencil(LucideIcons.withClass("library-search__row-glyph"))

  private def allHits(cx: CortexIndex, bi: BlogIndex): List[Hit] =
    val books       = cx.books.toList
    val bookHits    = books.map(Hit.BookHit(_))
    val chapterHits = books.flatMap(b => b.chapters.toList.map(c => Hit.ChapterHit(b, c)))
    val postHits    = bi.posts.toList.map(Hit.PostHit(_))
    bookHits ++ chapterHits ++ postHits

  private def filterHits(hits: List[Hit], query: String): List[Hit] =
    val q = query.trim.toLowerCase
    if q.isEmpty then Nil
    else hits.filter(h => hitTitle(h).toLowerCase.contains(q)).take(MaxResults)

  /** Wrap the matched substring in a `<mark>` — copied from `CortexSidebar.highlight`, palette-scoped. */
  private def highlight(title: String, query: String): VdomNode =
    val q = query.trim
    if q.isEmpty then title
    else
      val lower = title.toLowerCase
      val idx   = lower.indexOf(q.toLowerCase)
      if idx < 0 then title
      else
        val before = title.substring(0, idx)
        val hit    = title.substring(idx, idx + q.length)
        val after  = title.substring(idx + q.length)
        <.span(before, <.mark(^.className := "library-search__mark", hit), after)

  // ── trigger (lives in the header) ───────────────────────────────────────────

  /** The centered `.header__search` affordance; dispatches [[OpenEvent]] on click. No state of its own. */
  val Trigger =
    ScalaFnComponent
      .withHooks[Unit]
      .render { _ =>
        <.button(
          ^.tpe        := "button",
          ^.className  := "header__search",
          ^.aria.label := "Search the library (Cmd/Ctrl K)",
          ^.onClick --> Callback {
            val init = (new js.Object).asInstanceOf[dom.CustomEventInit]
            val ev   = new dom.CustomEvent(OpenEvent, init)
            val _    = dom.window.dispatchEvent(ev)
          },
          LucideIcons.Search(LucideIcons.withClass("header__search-icon")),
          <.span(^.className := "header__search-text", "Search the library…"),
          <.kbd(^.className  := "header__search-kbd", "⌘K")
        )
      }

  // ── modal host (lives in Layout) ────────────────────────────────────────────

  private def row(
      h: Hit,
      i: Int,
      isActive: Boolean,
      query: String,
      ctl: RouterCtl[Page],
      close: Callback,
      activeS: Hooks.UseState[Int]
  ): VdomNode =
    <.a(
      ^.key       := s"hit-$i",
      ^.href      := hitHref(h),
      ^.className := ("library-search__row" + (if isActive then " library-search__row--active" else "")),
      ^.onMouseEnter --> activeS.setState(i),
      ^.onClick ==> ((e: ReactMouseEvent) => e.preventDefaultCB >> close >> ctl.set(hitTarget(h))),
      <.span(^.className := "library-search__row-icon", iconFor(h)),
      <.span(
        ^.className := "library-search__row-body",
        <.span(^.className := "library-search__row-title", highlight(hitTitle(h), query)),
        <.span(^.className := "library-search__row-context", hitContext(h))
      )
    )

  private def renderResults(
      data: AsyncResult[(CortexIndex, BlogIndex)],
      query: String,
      results: List[Hit],
      activeIdx: Int,
      ctl: RouterCtl[Page],
      close: Callback,
      activeS: Hooks.UseState[Int]
  ): VdomNode =
    data match
      case AsyncResult.Loading =>
        <.p(^.className := "library-search__hint", "Loading the library…")
      case AsyncResult.Errored(msg) =>
        <.p(^.className := "library-search__hint library-search__hint--error", msg)
      case AsyncResult.Loaded(_) =>
        if query.trim.isEmpty then
          <.p(^.className := "library-search__hint", "Type to search books, chapters, and writing.")
        else if results.isEmpty then
          <.p(
            ^.className := "library-search__empty",
            "No matches for ",
            <.em(s""""${query.trim}""""),
            "."
          )
        else
          <.div(
            ^.className := "library-search__list",
            results.zipWithIndex.toTagMod { case (h, i) =>
              val label: VdomNode =
                if i == 0 || kindOf(results(i - 1)) != kindOf(h) then
                  <.div(
                    ^.key       := s"grp-$i",
                    ^.className := "library-search__group",
                    kindLabel(kindOf(h))
                  )
                else EmptyVdom
              TagMod(label, row(h, i, i == activeIdx, query, ctl, close, activeS))
            }
          )

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)                                        // open
      .useState("")                                           // query
      .useState(0)                                            // active result index
      .useState(AsyncFetch.initial[(CortexIndex, BlogIndex)]) // the two indices, fetched once
      // Fetch books + blog once on mount (the host mounts once for the whole session, in Layout).
      .useEffectOnMountBy { (_, _, _, _, dataS) =>
        AsyncFetch.run(
          setState = dataS.setState,
          fetch = ApiClient.getCortexIndex.zip(ApiClient.getBlogIndex),
          errorPrefix = "Failed to load the library"
        )
      }
      // Global ⌘K / Ctrl+K + the header trigger's custom event both open the palette.
      .useEffectOnMountBy { (_, openS, _, _, _) =>
        CallbackTo {
          val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
            if (e.metaKey || e.ctrlKey) && (e.key == "k" || e.key == "K") then
              e.preventDefault()
              openS.setState(true).runNow()
          val onOpen: js.Function1[dom.Event, Unit] = (_: dom.Event) => openS.setState(true).runNow()
          dom.window.addEventListener("keydown", onKey)
          dom.window.addEventListener(OpenEvent, onOpen)
          Callback {
            dom.window.removeEventListener("keydown", onKey)
            dom.window.removeEventListener(OpenEvent, onOpen)
          }
        }
      }
      // Focus the input whenever the palette opens (looked up by data-attribute, like the sidebar search).
      .useEffectWithDepsBy((_, openS, _, _, _) => openS.value) { (_, _, _, _, _) => open =>
        Callback {
          if open then
            val el = dom.document.querySelector("input[data-library-search='true']")
            if el != null then el.asInstanceOf[dom.html.Input].focus()
        }
      }
      .render { (props, openS, queryS, activeS, dataS) =>
        val ctl   = props.ctl
        val close = openS.setState(false) >> queryS.setState("") >> activeS.setState(0)

        if !openS.value then EmptyVdom
        else
          val query = queryS.value
          val results = dataS.value match
            case AsyncResult.Loaded((cx, bi)) => filterHits(allHits(cx, bi), query)
            case _                            => Nil
          val activeIdx =
            if results.isEmpty then 0 else math.max(0, math.min(activeS.value, results.size - 1))

          val onInputKey: ReactKeyboardEvent => Callback = e =>
            e.key match
              case "Escape" => e.preventDefaultCB >> close
              case "ArrowDown" =>
                e.preventDefaultCB >> activeS.modState(i =>
                  if results.isEmpty then 0 else math.min(i + 1, results.size - 1)
                )
              case "ArrowUp" =>
                e.preventDefaultCB >> activeS.modState(i => math.max(i - 1, 0))
              case "Enter" =>
                results.lift(activeIdx) match
                  case Some(h) => e.preventDefaultCB >> close >> ctl.set(hitTarget(h))
                  case None    => Callback.empty
              case _ => Callback.empty

          val onScrim: ReactMouseEvent => Callback = e =>
            if e.target == e.currentTarget then close else Callback.empty

          <.div(
            ^.className  := "library-search",
            ^.role       := "dialog",
            ^.aria.modal := true,
            ^.aria.label := "Search the library",
            ^.onClick ==> onScrim,
            <.div(
              ^.className := "library-search__card",
              <.div(
                ^.className := "library-search__head",
                LucideIcons.Search(LucideIcons.withClass("library-search__head-icon")),
                <.input(
                  ^.tpe                           := "search",
                  ^.className                     := "library-search__input",
                  ^.placeholder                   := "Search the library…",
                  ^.aria.label                    := "Search the library",
                  ^.value                         := query,
                  VdomAttr("data-library-search") := "true",
                  ^.onChange ==> { (e: ReactEventFromInput) =>
                    val v = e.target.value
                    queryS.setState(v) >> activeS.setState(0)
                  },
                  ^.onKeyDown ==> onInputKey
                ),
                <.kbd(^.className := "library-search__kbd", "Esc")
              ),
              <.div(
                ^.className := "library-search__results",
                renderResults(dataS.value, query, results, activeIdx, ctl, close, activeS)
              )
            )
          )
      }
