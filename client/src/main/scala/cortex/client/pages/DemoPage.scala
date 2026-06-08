package cortex.client.pages

import cortex.client.api.ApiClient
import cortex.client.components.icons.LucideIcons
import cortex.client.util.{AsyncFetch, AsyncResult, PageTitle}
import cortex.shared.api.Endpoints.{Greeting, HelloEvent, RecentCalls}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * The three-store showcase at `/demo`. Every hit to `/api/hello` reads through Redis over a Postgres visit
 * counter and appends to a MongoDB event log; this page renders the result and the recent log. Editorial-
 * skinned to match the Landing/Blog. "Run it again" re-issues the request so you can watch the cache flip
 * between a fresh Postgres read and a Redis hit (and the visit counter climb) — a live end-to-end smoke test.
 */
object DemoPage:

  /** Fire both fetches; shared by the on-mount effect and the "Run it again" button. */
  private def fetchAll(
      setG: AsyncResult[Greeting] => Callback,
      setR: AsyncResult[RecentCalls] => Callback
  ): Callback =
    AsyncFetch.run(setState = setG, fetch = ApiClient.getHello, errorPrefix = "Greeting fetch failed") >>
      AsyncFetch.run(setState = setR, fetch = ApiClient.getRecent, errorPrefix = "Recent-calls fetch failed")

  private def store(num: String, name: String, desc: TagMod): VdomNode =
    <.div(
      ^.className := "demo__store",
      <.div(^.className := "demo__store-num", num),
      <.div(^.className := "demo__store-name", name),
      <.p(^.className   := "demo__store-desc", desc)
    )

  private val stores: VdomNode =
    <.div(
      ^.className := "demo__stores",
      store(
        "01 · Postgres",
        "Visit counter",
        "The source of truth — one row, incremented on every cache miss."
      ),
      store(
        "02 · Redis",
        "Read-through cache",
        TagMod("A ~10s ", <.code("TTL"), " over the counter. A cache hit skips Postgres entirely.")
      ),
      store(
        "03 · MongoDB",
        "Event log",
        "Every call is appended here; the list below is the latest, newest first."
      )
    )

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AsyncFetch.initial[Greeting])
      .useState(AsyncFetch.initial[RecentCalls])
      .useEffectOnMountBy { (_, greetingS, recentS) =>
        PageTitle.set("Demo — Aniket Kakde") >> fetchAll(greetingS.setState, recentS.setState)
      }
      .render { (_, greetingS, recentS) =>
        <.main(
          ^.className := "demo",
          <.div(
            ^.className := "demo__inner",
            <.div(^.className := "demo__eyebrow", "— Live demo · three stores"),
            <.h1(^.className  := "demo__title", "Three stores,", <.br, "one request."),
            <.p(
              ^.className := "demo__lede",
              "Every hit to ",
              <.code("/api/hello"),
              " reads through Redis over a Postgres visit counter and appends to a MongoDB log. Run it",
              " again to watch the cache flip and the counter climb."
            ),
            stores,
            renderGreeting(greetingS.value),
            renderRecent(recentS.value),
            <.div(
              ^.className := "demo__actions",
              <.button(
                ^.tpe       := "button",
                ^.className := "cx-btn cx-btn--primary cx-btn--md",
                ^.onClick --> fetchAll(greetingS.setState, recentS.setState),
                LucideIcons.RotateCcw(LucideIcons.withClass("cx-btn__icon")),
                "Run it again"
              )
            )
          )
        )
      }

  private val loading: VdomNode                  = <.p(^.className := "demo__status", "Loading…")
  private def errored(message: String): VdomNode = <.p(^.className := "demo__error", s"Error: $message")

  private def renderGreeting(g: AsyncResult[Greeting]): VdomNode =
    <.section(
      ^.className := "demo__panel",
      <.div(^.className := "demo__panel-eyebrow", "Greeting · Postgres + Redis"),
      g.render(
        loaded = gr =>
          <.div(
            <.p(^.className := "demo__greeting-msg", gr.message),
            <.p(^.className := "demo__stat", "visit ", <.strong(s"#${gr.visits}")),
            if gr.cached then
              <.div(
                ^.className := "demo__badge demo__badge--cached",
                LucideIcons.RotateCcw(LucideIcons.withClass("demo__badge-icon")),
                "served from Redis cache"
              )
            else
              <.div(
                ^.className := "demo__badge demo__badge--fresh",
                LucideIcons.Check(LucideIcons.withClass("demo__badge-icon")),
                "fresh read from Postgres"
              )
          ),
        loading = loading,
        errored = errored
      )
    )

  private def renderRecent(r: AsyncResult[RecentCalls]): VdomNode =
    <.section(
      ^.className := "demo__panel",
      <.div(^.className := "demo__panel-eyebrow", "Recent calls · MongoDB"),
      r.render(
        loaded = rc =>
          if rc.entries.isEmpty then
            <.p(^.className := "demo__empty", "No entries yet — run it again after the first call.")
          else
            <.div(
              ^.className := "demo__recent",
              rc.entries.toTagMod { (e: HelloEvent) =>
                <.div(
                  ^.key       := s"${e.timestampEpochMs}-${e.visits}",
                  ^.className := "demo__recent-item",
                  <.span(^.className := "demo__recent-time", formatTime(e.timestampEpochMs)),
                  <.span(^.className := "demo__recent-visits", s"visit #${e.visits}")
                )
              }
            )
        ,
        loading = loading,
        errored = errored
      )
    )

  private def formatTime(epochMs: Long): String =
    new js.Date(epochMs.toDouble).toISOString()
