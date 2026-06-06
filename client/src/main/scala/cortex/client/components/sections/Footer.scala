package cortex.client.components.sections

import cortex.client.components.ToggleMode
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Footer — "Let's talk." block + two link columns + meta line.
 *
 * Mirrors the design system prototype's `.ftr` layout. The meta line carries the homelab one-liner and the
 * version stamp; the prototype's "v2.0 · paris ⌁ {year}" pattern is preserved.
 */
object Footer:

  private def metaLink(href: String, label: String, external: Boolean = false): VdomNode =
    <.a(
      ^.href      := href,
      ^.className := "footer__col-link",
      if external then TagMod(^.rel := "noopener noreferrer", ^.target := "_blank")
      else TagMod.empty,
      label,
      if external then <.span(^.className := "footer__col-link-arrow", " ↗") else EmptyVdom
    )

  val Component = ScalaFnComponent[Unit] { _ =>
    val year = new js.Date().getFullYear().toInt

    <.footer(
      ^.className := "footer container",
      <.div(
        ^.className := "footer__inner",
        <.div(
          ^.className := "footer__lede",
          <.h2(^.className := "footer__name", "Let's talk."),
          <.p(
            ^.className := "footer__sub",
            "Senior backend roles, JVM ecosystems, identity and platforms. Reach me at ",
            <.a(
              ^.href      := "mailto:a.r.kakde@gmail.com",
              ^.className := "footer__sub-link",
              "a.r.kakde@gmail.com"
            ),
            "."
          )
        ),
        <.div(
          ^.className := "footer__cols",
          <.div(
            ^.className := "footer__col",
            <.div(^.className := "footer__col-label", "Find me"),
            <.div(
              ^.className := "footer__col-list",
              metaLink("https://www.linkedin.com/in/aniketkakde/", "LinkedIn", external = true),
              metaLink("https://github.com/ani2fun", "GitHub", external = true),
              metaLink("mailto:a.r.kakde@gmail.com", "Email", external = false)
            )
          ),
          <.div(
            ^.className := "footer__col",
            <.div(^.className := "footer__col-label", "Read"),
            <.div(
              ^.className := "footer__col-list",
              metaLink("/", "Cortex"),
              metaLink("/blogs", "Blog"),
              metaLink("/Aniket-Kakde-CV-EN.pdf", "CV (PDF)")
            )
          )
        )
      ),
      <.div(
        ^.className := "footer__meta",
        <.span(
          ^.className := "footer__meta-credit",
          s"built with scala.js · served from a 4-node k3s cluster in my flat · © $year"
        ),
        <.div(
          ^.className := "footer__meta-right",
          <.span(^.className := "footer__meta-version", s"v2.0 · paris · $year"),
          <.div(^.className  := "footer__meta-toggle", ToggleMode.Component())
        )
      )
    )
  }
