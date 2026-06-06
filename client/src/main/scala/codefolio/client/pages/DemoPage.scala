package codefolio.client.pages

import codefolio.client.api.ApiClient
import codefolio.client.util.{AsyncFetch, AsyncResult, PageTitle}
import codefolio.shared.api.Endpoints.{Greeting, HelloEvent, RecentCalls}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * The Hello-counter demo page, kept as-is from the original codefolio skeleton — it exercises Postgres (visit
 * counter), Redis (cache), and MongoDB (recent log) end-to-end. Useful as a smoke test that the persistence
 * layer still works after each migration phase.
 *
 * Mounted at /demo; the home route ("/") hosts the migrated portfolio.
 */
object DemoPage:

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AsyncFetch.initial[Greeting])
      .useState(AsyncFetch.initial[RecentCalls])
      .useEffectOnMountBy { (_, greetingS, recentS) =>
        PageTitle.set("Demo — Aniket Kakde") >>
          AsyncFetch.run(
            setState = greetingS.setState,
            fetch = ApiClient.getHello,
            errorPrefix = "Failed to fetch greeting"
          ) >>
          AsyncFetch.run(
            setState = recentS.setState,
            fetch = ApiClient.getRecent,
            errorPrefix = "Failed to fetch recent calls"
          )
      }
      .render { (_, greetingS, recentS) =>
        <.main(
          ^.className := "min-h-[60vh] bg-slate-50 dark:bg-slate-900 flex items-center justify-center px-4 py-8",
          <.div(
            ^.className := "bg-white dark:bg-slate-800 shadow-xl rounded-2xl p-8 max-w-xl w-full space-y-6",
            <.h1(^.className := "text-3xl font-bold text-slate-900 dark:text-slate-50", "Codefolio Demo"),
            <.p(
              ^.className := "text-sm text-slate-600 dark:text-slate-300",
              "Postgres + Redis + MongoDB integration smoke test."
            ),
            renderGreeting(greetingS.value),
            renderRecent(recentS.value)
          )
        )
      }

  private val cardLoading: VdomNode =
    <.p(^.className := "text-slate-600 dark:text-slate-300", "Loading…")

  private def cardError(message: String): VdomNode =
    <.p(^.className := "text-red-600", s"Error: $message")

  private def renderGreeting(g: AsyncResult[Greeting]): VdomNode =
    <.section(
      ^.className := "border-t border-slate-100 dark:border-slate-700 pt-4",
      <.h2(
        ^.className := "text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-300 mb-2",
        "Greeting"
      ),
      g.render(
        loaded = gr =>
          <.div(
            <.p(^.className := "text-lg text-slate-800 dark:text-slate-200", gr.message),
            <.p(
              ^.className := "text-sm text-slate-600 dark:text-slate-300 mt-1",
              s"Visit count: ${gr.visits}"
            ),
            <.p(
              ^.className := (if gr.cached then "text-xs text-amber-600 mt-1"
                              else "text-xs text-emerald-600 mt-1"),
              if gr.cached then "↺ served from Redis cache"
              else "✓ fresh read from Postgres"
            )
          ),
        loading = cardLoading,
        errored = cardError
      )
    )

  private def renderRecent(r: AsyncResult[RecentCalls]): VdomNode =
    <.section(
      ^.className := "border-t border-slate-100 dark:border-slate-700 pt-4",
      <.h2(
        ^.className := "text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-300 mb-2",
        "Recent calls (MongoDB)"
      ),
      r.render(
        loaded = rc =>
          if rc.entries.isEmpty then
            <.p(
              ^.className := "text-slate-600 dark:text-slate-300 italic",
              "No entries yet — refresh after the first call."
            )
          else
            <.ul(
              ^.className := "text-sm text-slate-800 dark:text-slate-100 space-y-1",
              rc.entries.toTagMod { (e: HelloEvent) =>
                <.li(
                  ^.key := s"${e.timestampEpochMs}-${e.visits}",
                  <.span(
                    ^.className := "text-slate-600 dark:text-slate-300 mr-2",
                    formatTime(e.timestampEpochMs)
                  ),
                  <.span(s"visits=${e.visits}")
                )
              }
            )
        ,
        loading = cardLoading,
        errored = cardError
      )
    )

  private def formatTime(epochMs: Long): String =
    new js.Date(epochMs.toDouble).toISOString()
