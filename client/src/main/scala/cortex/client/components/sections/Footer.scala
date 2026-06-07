package cortex.client.components.sections

import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Footer — the design system's "Read it once. Keep it forever." block: an editorial lede + a library CTA,
 * link columns, and a mono meta strip carrying the homelab one-liner.
 */
object Footer:

  private def link(href: String, label: String, external: Boolean): VdomNode =
    <.a(
      ^.key       := label,
      ^.href      := href,
      ^.className := "cx-foot__link",
      if external then TagMod(^.target := "_blank", ^.rel := "noopener noreferrer") else TagMod.empty,
      label
    )

  private def col(head: String, links: List[(String, String, Boolean)]): VdomNode =
    <.div(
      ^.className := "cx-foot__col",
      <.div(^.className := "cx-foot__col-head", head),
      <.div(^.className := "cx-foot__col-list", links.toTagMod { case (h, l, e) => link(h, l, e) })
    )

  val Component = ScalaFnComponent[Unit] { _ =>
    val year = new js.Date().getFullYear().toInt
    <.footer(
      ^.className := "cx-foot",
      <.div(
        ^.className := "cx-foot__inner",
        <.div(
          ^.className := "cx-foot__grid",
          <.div(
            ^.className := "cx-foot__lede",
            <.h2(^.className := "cx-foot__lede-title", "Read it once.", <.br, "Keep it forever."),
            <.a(
              ^.href      := "/",
              ^.className := "cx-btn cx-btn--primary cx-btn--md",
              LucideIcons.BookOpen(LucideIcons.withClass("cx-btn__icon")),
              "Browse the library"
            )
          ),
          col(
            "Read",
            List(
              ("/", "Library", false),
              ("/blogs", "Blog", false),
              ("/Aniket-Kakde-CV-EN.pdf", "CV (PDF)", false)
            )
          ),
          col(
            "Connect",
            List(
              ("https://github.com/ani2fun", "GitHub", true),
              ("https://www.linkedin.com/in/aniketkakde/", "LinkedIn", true),
              ("mailto:a.r.kakde@gmail.com", "Email", true)
            )
          )
        ),
        <.div(
          ^.className := "cx-foot__meta",
          <.span(s"© $year Aniket Kakde"),
          <.span(
            ^.className := "cx-foot__meta-right",
            "Built for reading · served from a 4-node k3s cluster in my flat"
          )
        )
      )
    )
  }
