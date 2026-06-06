package cortex.client.components.blog

import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.BlogSummary
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Two-card prev/next pager between blog posts. `prev` is the older post (in publication order — chronological
 * "what was posted before this"), `next` is the newer one. Either or both may be empty at the ends of the
 * index; the component renders nothing if both are empty.
 */
object BlogPager:

  final case class Props(prev: Option[BlogSummary], next: Option[BlogSummary])

  val Component = ScalaFnComponent[Props] { props =>
    if props.prev.isEmpty && props.next.isEmpty then EmptyVdom
    else
      <.div(
        ^.className := "blog__pager",
        props.prev match
          case Some(p) =>
            <.a(
              ^.href      := s"/blogs/${p.slug}",
              ^.className := "blog__pager-card",
              <.span(
                ^.className := "blog__pager-direction",
                LucideIcons.ArrowLeft(LucideIcons.withClass("h-3 w-3 inline mr-1")),
                "Older"
              ),
              <.span(^.className := "blog__pager-title", p.title)
            )
          case None => <.span(),
        props.next match
          case Some(n) =>
            <.a(
              ^.href      := s"/blogs/${n.slug}",
              ^.className := "blog__pager-card blog__pager-card--next",
              <.span(
                ^.className := "blog__pager-direction",
                "Newer ",
                LucideIcons.ArrowRight(LucideIcons.withClass("h-3 w-3 inline ml-1"))
              ),
              <.span(^.className := "blog__pager-title", n.title)
            )
          case None => EmptyVdom
      )
  }
