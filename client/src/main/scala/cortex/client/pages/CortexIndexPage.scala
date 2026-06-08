package cortex.client.pages

import cortex.client.api.ApiClient
import cortex.client.components.book.BookGrid
import cortex.client.components.icons.LucideIcons
import cortex.client.util.{AsyncFetch, PageTitle}
import cortex.shared.api.Endpoints.CortexIndex
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Cortex landing — the editorial home at `/`. A status-pill + italic-serif hero, then the book library
 * (fetched from `/api/cortex/index`). Mirrors the design system's `docs/Landing` surface; the book cards
 * reuse [[BookGrid]] (whose card the design was reverse-engineered from).
 */
object CortexIndexPage:

  private def stat(num: String, sup: String, label: String): VdomNode =
    <.div(
      ^.className := "cx-stat",
      <.div(
        ^.className := "cx-stat__num",
        num,
        if sup.nonEmpty then <.span(^.className := "cx-stat__sup", sup) else EmptyVdom
      ),
      <.div(^.className := "cx-stat__label", label)
    )

  private val hero: VdomNode =
    <.section(
      ^.className := "cx-hero",
      <.div(
        ^.className := "cx-hero__pill",
        <.span(^.className := "cx-hero__dot", ^.aria.hidden := true),
        "A reading-first knowledge base"
      ),
      <.h1(
        ^.className := "cx-hero__title",
        "Think in systems,",
        <.br,
        "not in tabs."
      ),
      <.p(
        ^.className := "cx-hero__lede",
        <.em(^.className := "cx-hero__lede-em", "Cortex"),
        " turns reading into a library you can return to — interactive books, linked notes, and runnable",
        " code, all set like a publication you'd want to read."
      ),
      <.div(
        ^.className := "cx-hero__ctas",
        <.a(
          ^.href      := "#library",
          ^.className := "cx-btn cx-btn--primary cx-btn--lg",
          LucideIcons.BookOpen(LucideIcons.withClass("cx-btn__icon")),
          "Start reading"
        ),
        <.a(
          ^.href      := "https://github.com/ani2fun/cortex",
          ^.target    := "_blank",
          ^.rel       := "noopener noreferrer",
          ^.className := "cx-btn cx-btn--outline cx-btn--lg",
          "Browse on GitHub"
        )
      ),
      <.div(
        ^.className := "cx-hero__stats",
        stat("Py · Java", "", "RUNNABLE IN-BROWSER"),
        stat("∞", "", "RE-READS, NO PAYWALL"),
        stat("0", "ads", "JUST THE WRITING"),
        stat("100%", "", "YOURS TO KEEP")
      )
    )

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
          ^.className := "cx-home",
          hero,
          <.section(
            ^.className := "cx-lib",
            ^.id        := "library",
            <.div(
              ^.className := "cx-lib__head",
              <.div(^.className := "cx-lib__eyebrow", "— The library"),
              <.h2(^.className  := "cx-lib__title", "Browse the books")
            ),
            state.value.render(
              loaded = idx => BookGrid.Component(BookGrid.Props(idx.books.toList)),
              loading = <.p(^.className := "cx-lib__status", "Loading the library…"),
              errored = msg => <.p(^.className := "cx-lib__status", msg)
            )
          )
        )
      }
