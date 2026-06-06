package codefolio.client.pages

import codefolio.client.api.ApiClient
import codefolio.client.components.blog.BlogPager
import codefolio.client.components.cortex.{ChapterContent, CortexErrorView}
import codefolio.client.markdown.MarkdownRenderer
import codefolio.client.util.{AsyncFetch, PageTitle}
import codefolio.shared.api.Endpoints.{BlogPostPayload, BlogSummary}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Reads `/api/blogs/{slug}`, runs the markdown body through the same client-side pipeline used by Cortex
 * chapters (so syntax-highlighted code blocks, KaTeX, D2, mermaid all work the same way), and lays out the
 * result in a single-column reader. Reuses [[ChapterContent]] so any future runnable-code / D2 placeholder
 * blocks embedded in a blog post mount the same way they do in a chapter.
 *
 * No sidebar, no TOC layout — blog posts are standalone reads. The post body's own `<nav
 * class="blog-post__toc">` (rendered as raw HTML in the markdown body) is the in-article TOC.
 */
object BlogPostPage:

  final case class Props(slug: String)

  final private case class Loaded(payload: BlogPostPayload, render: MarkdownRenderer.Result)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(AsyncFetch.initial[Loaded])
      .useEffectWithDepsBy((props, _) => props.slug) { (_, state) => slug =>
        AsyncFetch.run(
          setState = state.setState,
          fetch =
            for
              payload  <- ApiClient.getBlogPost(slug)
              rendered <- MarkdownRenderer.render(payload.raw)
            yield Loaded(payload, rendered),
          errorPrefix = s"Failed to load blog post $slug",
          onLoaded = loaded =>
            PageTitle.set(s"${loaded.payload.frontmatter.title} — Aniket Kakde")
        )
      }
      .render { (props, state) =>
        state.value.render(
          loaded = renderLoaded,
          loading = <.main(
            ^.className := "container mx-auto py-24 text-center text-muted-foreground",
            s"Loading ${props.slug}…"
          ),
          errored = msg => <.main(^.className := "container mx-auto py-16 text-center", CortexErrorView(msg))
        )
      }

  private def renderLoaded(loaded: Loaded): VdomNode =
    val payload = loaded.payload

    val prev: Option[BlogSummary] =
      payload.prevSlug.flatMap(s => if s == payload.post.slug then None else Some(stub(s)))
    val next: Option[BlogSummary] =
      payload.nextSlug.flatMap(s => if s == payload.post.slug then None else Some(stub(s)))

    <.div(
      ^.className := "pt-20",
      <.main(
        ^.className := "container mx-auto px-4 md:px-6 max-w-4xl",
        <.article(
          ^.className := "blog-post",
          ChapterContent.Component(ChapterContent.Props(loaded.render))
        )
      ),
      BlogPager.Component(BlogPager.Props(prev, next))
    )

  // The payload only carries prev/next slugs, not full summaries. The pager doesn't need the full record —
  // just slug + title — so we render a placeholder title from the slug. A future enhancement could embed
  // prev/next summaries in the payload to show the real titles in the pager cards.
  private def stub(slug: String): BlogSummary =
    BlogSummary(
      slug = slug,
      title = humanise(slug),
      summary = "",
      publishedAt = "",
      tags = None,
      readMinutes = None,
      eyebrow = None
    )

  private def humanise(slug: String): String =
    slug
      .split("[-_]")
      .iterator
      .filter(_.nonEmpty)
      .map(w => w.headOption.fold("")(_.toUpper.toString) + w.drop(1))
      .mkString(" ")
