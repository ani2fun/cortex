package codefolio.client.components.sections

import codefolio.client.components.icons.LucideIcons
import codefolio.client.components.ui.Section
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Hero — editorial dark-mode-first landing section.
 *
 * Layout from top:
 *   1. status pill (live "currently at …" mono caption with pulsing green dot) 2. display name in italic
 *      Instrument Serif with tiny "EU" superscript 3. lede paragraph with one editorial italic emphasis 4.
 *      hierarchical CTAs (filled primary / ghost secondary) — Download CV, See selected work, Cortex, Blog 5.
 *      4-column stat strip (numbers in display serif, captions in mono)
 *
 * All copy is hardcoded — these strings change rarely, putting them in JSON trades a grep for a build-time
 * indirection that adds nothing.
 */
object Hero:

  /** Stat strip — short headline number + small mono caption underneath. */
  private val stats: List[(String, String, String)] = List(
    ("10", "yrs", "ON PRODUCTION SYSTEMS"),
    ("4", "SECTORS", "MOBILITY · AUTOMOTIVE · MEDIA · LOGISTICS"),
    ("€M", "", "PILOT → FUNDED PROGRAMME"),
    ("6", " COMPANIES", "Europcar Audi Disneyland-Paris Dassault UPS Bell-Labs")
  )

  val Component = ScalaFnComponent[Unit] { _ =>
    Section("hero", "hero")(
      <.div(
        ^.className := "hero__inner",
        <.div(
          ^.className := "hero__status",
          <.span(^.className := "hero__status-dot", ^.aria.hidden := true),
          <.span(^.className := "hero__status-text", "Currently at Europcar · open to senior backend roles")
        ),
        <.h1(
          ^.className := "hero__name",
          "Aniket Kakde",
          <.sup(^.className := "hero__name-sup", "EU")
        ),
        <.p(
          ^.className := "hero__lede",
          <.em("Backend-leaning"),
          " Software Engineer. Currently at Europcar, building the unified B2B/B2C customer identity platform.",
          <.br,
          "Previously: Helped build Audi's in-house video annotation platform and data pipeline used to generate ground-truth training data for autonomous driving models, and helped lead a Disney pilot that became a multi-million-euro replacement of their legacy marketing platform."
        ),
        <.div(
          ^.className := "hero__cta-row",
          <.a(
            ^.href      := "/Aniket-Kakde-CV-EN.pdf",
            ^.className := "hero__cta hero__cta--primary",
            LucideIcons.Download(LucideIcons.withClass("hero__cta-icon")),
            "Download CV"
          ),
          <.a(
            ^.href      := "#work",
            ^.className := "hero__cta hero__cta--ghost",
            LucideIcons.ArrowRight(LucideIcons.withClass("hero__cta-icon")),
            "See selected work"
          ),
          <.a(
            ^.href      := "/cortex",
            ^.className := "hero__cta hero__cta--ghost",
            LucideIcons.BookOpen(LucideIcons.withClass("hero__cta-icon")),
            "Cortex"
          ),
          <.a(
            ^.href      := "/blogs",
            ^.className := "hero__cta hero__cta--ghost",
            LucideIcons.Pencil(LucideIcons.withClass("hero__cta-icon")),
            "Blog"
          )
        ),
        <.div(
          ^.className := "hero__stats",
          stats.toTagMod { case (num, sup, label) =>
            <.div(
              ^.className := "hero__stat",
              ^.key       := label,
              <.div(
                ^.className := "hero__stat-num",
                num,
                if sup.nonEmpty then <.span(^.className := "hero__stat-sup", sup) else EmptyVdom
              ),
              <.div(^.className := "hero__stat-label", label)
            )
          }
        )
      )
    )
  }
