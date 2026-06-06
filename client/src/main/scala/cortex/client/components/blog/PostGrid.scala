package cortex.client.components.blog

import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.BlogSummary
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Renders a grid of blog post cards from a `BlogIndex.posts` slice. Card markup uses the `blog__card*` BEM
 * classes from `client/src/styles/sections/blog.css` — analogous to `BookGrid` for Cortex. The empty-list
 * message is the component's responsibility because it's part of the grid surface.
 */
object PostGrid:

  final case class Props(posts: List[BlogSummary], limit: Option[Int] = None)

  val Component = ScalaFnComponent[Props] { props =>
    val visible = props.limit.fold(props.posts)(n => props.posts.take(n))
    if visible.isEmpty then
      <.p(
        ^.className := "blog__status",
        "No posts published yet — check back soon."
      )
    else
      <.div(^.className := "blog__grid", visible.toTagMod(renderCard))
  }

  private def renderCard(post: BlogSummary): VdomNode =
    val readingMinutes = post.readMinutes.fold("")(m => s" · $m min read")
    <.a(
      ^.key       := post.slug,
      ^.href      := s"/blogs/${post.slug}",
      ^.className := "blog__card group",
      <.div(
        ^.className := "blog__card-meta",
        LucideIcons.BookOpen(LucideIcons.withClass("blog__card-meta-icon")),
        <.span(
          ^.className := "blog__card-meta-text",
          s"${post.publishedAt}$readingMinutes"
        )
      ),
      post.eyebrow.filter(_.nonEmpty).map(e => <.div(^.className := "blog__card-eyebrow", e): VdomNode)
        .getOrElse(EmptyVdom),
      <.h2(^.className := "blog__card-title", post.title),
      <.p(^.className  := "blog__card-summary", post.summary),
      <.div(
        ^.className := "blog__card-footer",
        post.tags.toList.flatten.toTagMod { tag =>
          <.span(^.key := tag, ^.className := "blog__card-tag", tag)
        },
        <.span(
          ^.className := "blog__card-cta",
          "Read",
          LucideIcons.ArrowRight(LucideIcons.withClass("blog__card-cta-icon"))
        )
      )
    )
