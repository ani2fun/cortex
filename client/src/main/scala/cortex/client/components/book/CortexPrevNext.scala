package cortex.client.components.book

import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.ChapterRef
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Big prev/next chapter cards rendered at the foot of the prose — replaces the smaller text-only pager. Each
 * card is an `<a>` with an eyebrow row (Previous / Next + arrow), the chapter title (display italic), and a
 * short blurb if one is in scope. Adjacency is computed server-side and threaded through via `prev`/`next`
 * props; this component is purely presentational.
 */
object CortexPrevNext:

  final case class Props(bookSlug: String, prev: Option[ChapterRef], next: Option[ChapterRef])

  private def card(bookSlug: String, ch: ChapterRef, isNext: Boolean): VdomNode =
    val cls =
      if isNext then "cortex-reader-prev-next__card cortex-reader-prev-next__card--next"
      else "cortex-reader-prev-next__card cortex-reader-prev-next__card--prev"
    val eyebrowCls =
      if isNext then "cortex-reader-prev-next__eyebrow cortex-reader-prev-next__eyebrow--next"
      else "cortex-reader-prev-next__eyebrow"
    val arrowIcon =
      if isNext then
        LucideIcons.ArrowRight(LucideIcons.withClass("cortex-reader-prev-next__arrow"))
      else
        LucideIcons.ArrowLeft(LucideIcons.withClass("cortex-reader-prev-next__arrow"))
    <.a(
      ^.href       := s"/$bookSlug/${ch.slug}",
      ^.className  := cls,
      ^.aria.label := (if isNext then s"Next chapter: ${ch.title}" else s"Previous chapter: ${ch.title}"),
      <.span(
        ^.className := eyebrowCls,
        if isNext then EmptyVdom else arrowIcon,
        if isNext then "Next" else "Previous",
        if isNext then arrowIcon else EmptyVdom
      ),
      <.h4(^.className := "cortex-reader-prev-next__title", ch.title)
    )

  val Component = ScalaFnComponent[Props] { props =>
    if props.prev.isEmpty && props.next.isEmpty then EmptyVdom
    else
      <.nav(
        ^.className  := "cortex-reader-prev-next",
        ^.aria.label := "Chapter navigation",
        props.prev match
          case Some(p) => card(props.bookSlug, p, isNext = false)
          case None    => <.span(^.className := "cortex-reader-prev-next__placeholder"),
        props.next match
          case Some(n) => card(props.bookSlug, n, isNext = true)
          case None    => EmptyVdom
      )
  }
