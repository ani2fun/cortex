package cortex.client.pages

import cortex.client.api.ApiClient
import cortex.client.components.blog.PostGrid
import cortex.client.util.{AsyncFetch, AsyncResult, PageTitle}
import cortex.shared.api.Endpoints.BlogIndex
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Lists every published blog post — the `/blogs` route. Editorial header (mono eyebrow + italic-serif title +
 * lede) over a [[PostGrid]], speaking the same design language as the Landing: the `.blog__inner` column is
 * sized to match `.cx-home`, so the post cards line up with the library cards. The header stays put across
 * all three async states so the page never flashes a bare grid; `PostGrid` owns its own empty-state copy.
 */
object BlogIndexPage:

  /**
   * The editorial chrome, shared by every async state. `rightSlot` carries the post-count pill once loaded.
   */
  private def shell(rightSlot: VdomNode, body: VdomNode): VdomNode =
    <.main(
      ^.className := "blog",
      <.div(
        ^.className := "blog__inner",
        <.div(^.className := "blog__eyebrow", "— Writing"),
        <.div(
          ^.className := "blog__heading-row",
          <.h1(^.className := "blog__title", "Written to be re-read."),
          rightSlot
        ),
        <.p(
          ^.className := "blog__lede",
          "Long-form essays and field notes — from the homelab to the kitchen. Fewer posts, more depth;",
          " each one meant to stand on its own."
        ),
        body
      )
    )

  private def countPill(n: Int): VdomNode =
    if n <= 0 then EmptyVdom
    else <.span(^.className := "blog__count", s"$n ${if n == 1 then "post" else "posts"}")

  private def status(text: String): VdomNode =
    <.p(^.className := "blog__status", text)

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
        state.value match
          case AsyncResult.Loading      => shell(EmptyVdom, status("Loading posts…"))
          case AsyncResult.Errored(msg) => shell(EmptyVdom, status(msg))
          case AsyncResult.Loaded(idx) =>
            shell(
              countPill(idx.posts.length),
              PostGrid.Component(PostGrid.Props(idx.posts.toList))
            )
      }
