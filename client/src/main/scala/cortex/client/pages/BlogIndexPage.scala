package cortex.client.pages

import cortex.client.api.ApiClient
import cortex.client.components.blog.PostGrid
import cortex.client.util.{AsyncFetch, PageTitle}
import cortex.shared.api.Endpoints.BlogIndex
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Lists every published blog post — the `/blogs` route. Mirrors `CortexIndexPage` shape: hero (title + lede)
 * over a `PostGrid`. The grid renders an empty-state message when the index is empty.
 */
object BlogIndexPage:

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AsyncFetch.initial[BlogIndex])
      .useEffectOnMountBy { (_, state) =>
        PageTitle.set("Blog — Aniket Kakde") >>
          AsyncFetch.run(
            setState = state.setState,
            fetch = ApiClient.getBlogIndex,
            errorPrefix = "Failed to load blog index"
          )
      }
      .render { (_, state) =>
        <.main(
          ^.className := "container",
          <.section(
            ^.className := "px-4 md:px-8 pt-28 md:pt-32 pb-12",
            <.h1(
              ^.className := "text-3xl md:text-5xl font-bold text-center text-foreground mb-3",
              "Blog"
            ),
            <.p(
              ^.className :=
                "text-center text-foreground/80 mb-10 text-sm md:text-base max-w-2xl mx-auto",
              "Articles and write-ups. Pick a post to start reading."
            ),
            state.value.render(
              loaded = idx => PostGrid.Component(PostGrid.Props(idx.posts.toList))
            )
          )
        )
      }
