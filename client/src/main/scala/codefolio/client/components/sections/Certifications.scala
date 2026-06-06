package codefolio.client.components.sections

import codefolio.client.components.icons.LucideIcons
import codefolio.client.components.ui.Section
import codefolio.client.data.PortfolioData
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Vertical timeline of diplomas and course certificates. Highlighted entries (e.g. the Diploma) get a star
 * marker and a subtly shaded card background.
 */
object Certifications:

  val Component = ScalaFnComponent[Unit] { _ =>
    val items   = PortfolioData.certifications.toList
    val lastIdx = items.length - 1

    Section("certifications", "certifications")(
      <.h2(^.className := "certifications__title", "Certifications"),
      <.p(
        ^.className := "certifications__subtitle",
        "A timeline of diplomas and course certificates. Click any card to open the PDF."
      ),
      <.div(
        ^.className := "certifications__timeline",
        <.div(^.className := "certifications__spine", ^.aria.hidden := true),
        items.zipWithIndex.toTagMod { case (cert, idx) =>
          val entryCls =
            if idx == lastIdx then "certifications__entry certifications__entry--last"
            else "certifications__entry"
          val markerCls =
            if cert.highlight then "certifications__marker certifications__marker--highlight"
            else "certifications__marker"
          val cardCls =
            if cert.highlight then "certifications__card certifications__card--highlight group"
            else "certifications__card group"
          val markerIcon =
            if cert.highlight then
              LucideIcons.Trophy(LucideIcons.withClass("certifications__marker-icon"))
            else
              LucideIcons.Star(LucideIcons.withClass("certifications__marker-icon"))

          <.div(
            ^.key       := idx,
            ^.className := entryCls,
            <.div(
              ^.className   := markerCls,
              ^.aria.hidden := true,
              markerIcon
            ),
            <.div(
              ^.className := "certifications__meta",
              <.span(^.className := "certifications__date", cert.date),
              <.span(^.className := "certifications__length", s"· ${cert.length}")
            ),
            <.a(
              ^.href      := cert.url,
              ^.target    := "_blank",
              ^.rel       := "noopener noreferrer",
              ^.className := cardCls,
              <.div(
                ^.className := "certifications__issuer-row",
                <.span(^.className := "certifications__issuer", cert.issuer)
              ),
              <.h3(
                ^.className := "certifications__name",
                cert.name,
                if cert.highlight then <.span(^.className := "certifications__badge", "Diploma")
                else EmptyVdom
              ),
              <.p(^.className := "certifications__description", cert.description),
              <.div(
                ^.className := "certifications__footer",
                cert.tags.toList.toTagMod { tag =>
                  <.span(^.key := tag, ^.className := "certifications__tag", tag)
                },
                <.span(
                  ^.className := "certifications__view",
                  "View certificate",
                  LucideIcons.ArrowRight(LucideIcons.withClass("certifications__view-arrow"))
                )
              )
            )
          )
        }
      )
    )
  }
