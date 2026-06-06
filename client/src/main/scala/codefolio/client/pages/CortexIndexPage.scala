package codefolio.client.pages

import codefolio.client.api.ApiClient
import codefolio.client.components.cortex.BookGrid
import codefolio.client.util.{AsyncFetch, PageTitle}
import codefolio.shared.api.Endpoints.CortexIndex
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Lists every book in the Cortex. Each card links to `/cortex/<slug>`, which the BookRedirectPage forwards to
 * the first chapter.
 */
object CortexIndexPage:

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AsyncFetch.initial[CortexIndex])
      .useEffectOnMountBy { (_, state) =>
        PageTitle.set("Cortex — Aniket Kakde") >>
          AsyncFetch.run(
            setState = state.setState,
            fetch = ApiClient.getCortexIndex,
            errorPrefix = "Failed to load index"
          )
      }
      .render { (_, state) =>
        <.main(
          ^.className := "container",
          <.section(
            ^.className := "px-4 md:px-8 pt-28 md:pt-32 pb-12",
            <.h1(
              ^.className := "text-3xl md:text-5xl font-bold text-center text-foreground mb-3",
              "Cortex"
            ),
            <.p(
              ^.className :=
                "text-center text-foreground/80 mb-10 text-sm md:text-base max-w-2xl mx-auto",
              "Notes I keep while reading, building, and chasing rabbit holes. Pick a topic and dive in."
            ),
            state.value.render(
              loaded = idx => BookGrid.Component(BookGrid.Props(idx.books.toList))
            )
          )
        )
      }
