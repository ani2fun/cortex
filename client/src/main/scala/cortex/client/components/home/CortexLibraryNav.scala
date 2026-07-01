package cortex.client.components.home

import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.Book
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Library navigation — the HelloInterview "dashboard" view. A **Your Dashboard** home link, then an
 * expandable **Learn** group listing every book. It renders inside the reader's left sidebar as the alternate
 * view that "Back to Main" toggles to (see [[cortex.client.components.book.CortexSidebar]]), letting a reader
 * jump between books without leaving the page.
 *
 * Interaction:
 *   - **Your Dashboard** → navigates to the library home `/` (and fires `onNavigate`, e.g. to close the
 *     mobile drawer).
 *   - The **active** book (the one being read) → clicking it returns to that book's chapter tree in place via
 *     `onReturn` (no navigation).
 *   - **Other** books → deep-link to `/{slug}`; the SPA `Router` intercepts and `BookRedirect` lands on the
 *     book's first chapter.
 */
object CortexLibraryNav:

  final case class Props(
      books: List[Book],
      activeSlug: Option[String] = None,
      onReturn: Callback = Callback.empty,
      onNavigate: Callback = Callback.empty
  )

  val Component = ScalaFnComponent[Props] { props =>
    <.nav(
      ^.className  := "cx-lnav",
      ^.aria.label := "Library navigation",
      <.a(
        ^.href      := "/",
        ^.className := "cx-lnav__item",
        ^.onClick --> props.onNavigate,
        LucideIcons.Home(LucideIcons.withClass("cx-lnav__icon")),
        <.span(^.className := "cx-lnav__label", "Your Dashboard")
      ),
      <.div(^.className := "cx-lnav__divider", ^.aria.hidden := true),
      <.details(
        ^.className := "cx-lnav__group",
        ^.open      := true,
        <.summary(
          ^.className := "cx-lnav__head",
          LucideIcons.Lightbulb(LucideIcons.withClass("cx-lnav__icon")),
          <.span(^.className := "cx-lnav__label", "Learn"),
          LucideIcons.ChevronDown(LucideIcons.withClass("cx-lnav__caret"))
        ),
        <.div(
          ^.className := "cx-lnav__list",
          props.books.toTagMod { book =>
            val active = props.activeSlug.contains(book.slug)
            <.a(
              ^.key       := book.slug,
              ^.href      := s"/${book.slug}",
              ^.className := s"cx-lnav__book${if active then " cx-lnav__book--active" else ""}",
              ^.onClick ==> { (e: ReactMouseEvent) =>
                // The current book is the "you are here" row — clicking it returns to its chapter tree
                // in place rather than reloading the page. Other books navigate normally.
                if active then e.preventDefaultCB >> props.onReturn
                else props.onNavigate
              },
              <.span(^.className := "cx-lnav__book-title", book.title),
              LucideIcons.ChevronRight(LucideIcons.withClass("cx-lnav__book-caret"))
            )
          }
        )
      )
    )
  }
