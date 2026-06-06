package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import codefolio.shared.api.Endpoints.Book
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Renders a grid of Book cards from a `CortexIndex.books` slice. Used by both `CortexIndexPage` (full list)
 * and the home-page `Cortex` section (first six). Card markup lives once; per-page chrome (heading, "Browse
 * all" CTA) stays at the call site.
 *
 * Markup uses the `cortex__card*` BEM classes from `client/src/styles/sections/cortex.css`. The empty-list
 * message is the component's responsibility because it's part of the grid surface; "no books" reads better
 * inside the grid container than as a sibling of the absent grid.
 */
object BookGrid:

  final case class Props(books: List[Book], limit: Option[Int] = None)

  val Component = ScalaFnComponent[Props] { props =>
    val visible = props.limit.fold(props.books)(n => props.books.take(n))
    if visible.isEmpty then
      <.p(
        ^.className := "cortex__status",
        "No books published yet — check back soon."
      )
    else
      <.div(^.className := "cortex__grid", visible.toTagMod(renderCard))
  }

  private def renderCard(book: Book): VdomNode =
    val chapterCount   = book.chapters.size
    val plural         = if chapterCount == 1 then "" else "s"
    val readingMinutes = book.estimatedReadingMinutes.fold("")(m => s" · $m min read")
    <.a(
      ^.key       := book.slug,
      ^.href      := s"/cortex/${book.slug}",
      ^.className := "cortex__card group",
      <.div(
        ^.className := "cortex__card-meta",
        LucideIcons.BookOpen(LucideIcons.withClass("cortex__card-meta-icon")),
        <.span(^.className := "cortex__card-meta-text", s"$chapterCount chapter$plural$readingMinutes")
      ),
      <.h2(^.className := "cortex__card-title", book.title),
      <.p(^.className  := "cortex__card-description", book.description),
      <.div(
        ^.className := "cortex__card-footer",
        book.tags.toList.flatten.toTagMod { tag =>
          <.span(^.key := tag, ^.className := "cortex__card-tag", tag)
        },
        <.span(
          ^.className := "cortex__card-cta",
          "Read",
          LucideIcons.ArrowRight(LucideIcons.withClass("cortex__card-cta-icon"))
        )
      )
    )
