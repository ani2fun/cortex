package codefolio.client.pages

import codefolio.client.api.ApiClient
import codefolio.client.components.cortex.CortexErrorView
import codefolio.client.util.AsyncFetch
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * `/cortex/<book>` → fetch the index, look up the book, redirect via `history.replaceState` to its first
 * chapter. Falls back to a clear error message if the book is unknown.
 *
 * The fetch returns the redirect target URL when successful (book found + has chapters); a missing book or an
 * empty book throws a clean message that AsyncFetch surfaces verbatim (no prefix).
 */
object BookRedirectPage:

  val Component =
    ScalaFnComponent
      .withHooks[String]
      .useState(AsyncFetch.initial[String]) // redirect-target URL once resolved
      .useEffectOnMountBy { (book, state) =>
        AsyncFetch.run(
          setState = state.setState,
          fetch = ApiClient.getCortexIndex.map { idx =>
            idx.books.find(_.slug == book) match
              case Some(b) if b.chapters.nonEmpty =>
                s"/cortex/$book/${b.chapters.head.slug}"
              case Some(_) =>
                throw new RuntimeException(s"Book '$book' has no chapters yet.")
              case None =>
                throw new RuntimeException(s"Book '$book' not found.")
          },
          errorPrefix = "",
          // Use replaceState so the back button doesn't bounce here.
          onLoaded = target => Callback(dom.window.location.replace(target))
        )
      }
      .render { (book, state) =>
        val loadingVdom: VdomNode =
          <.p(^.className := "text-muted-foreground", s"Loading $book…")
        <.main(
          ^.className := "container mx-auto py-16 text-center",
          state.value.render(
            loaded = _ => loadingVdom,
            loading = loadingVdom,
            errored = msg => <.div(CortexErrorView(msg))
          )
        )
      }
