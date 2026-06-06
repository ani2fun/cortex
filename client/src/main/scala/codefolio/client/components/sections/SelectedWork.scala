package codefolio.client.components.sections

import codefolio.client.components.icons.LucideIcons
import codefolio.client.components.ui.Section
import codefolio.client.data.PortfolioData
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * SelectedWork — three-row scannable strip between Hero and About.
 *
 * The "30-second skim path" the design brief calls out as the missing piece of the original site. Each row
 * condenses a single role into: company-italic | dates · location · role mono | one-sentence blurb \| primary
 * tech tags right-aligned | arrow ↗
 *
 * Picks are driven by the `featured: true` flag in `experienceData.json`. If no entries are flagged we fall
 * back to the first three (which match Europcar / Audi / Disney by virtue of reverse-chronological JSON order
 * minus the one-month Dassault stint at index 1 — see `pickFeatured`).
 */
object SelectedWork:

  /**
   * Drop the months/days noise from a "August 2022 – Present" string, keeping just the years for the meta
   * row. "August 2022" → "2022".
   */
  private def yearsOnly(time: String): String =
    val tokens = time.split("[-–—]").map(_.trim)
    tokens.map { tok =>
      val words = tok.split("\\s+").filter(_.nonEmpty)
      words.lastOption.getOrElse(tok)
    }.mkString(" – ")

  /**
   * Pick the entries to render. Honours an explicit `featured: true` flag if any rows have it; otherwise
   * picks the first three excluding the one-month Dassault stint.
   */
  private def pickFeatured(all: js.Array[PortfolioData.Experience]): List[PortfolioData.Experience] =
    val flagged = all.filter(_.featured.getOrElse(false)).toList
    if flagged.nonEmpty then flagged.take(3)
    else all.filterNot(_.company.short == "Dassault").toList.take(3)

  val Component = ScalaFnComponent[Unit] { _ =>
    val featured = pickFeatured(PortfolioData.experience)

    Section("work", "selected-work")(
      <.div(
        ^.className := "selected-work__inner",
        <.div(
          ^.className := "selected-work__heading-row",
          <.div(
            ^.className := "selected-work__heading",
            <.div(^.className := "selected-work__eyebrow", "SELECTED WORK · 2017 — PRESENT"),
            <.h2(
              ^.className := "selected-work__title",
              "Three rooms",
              <.br,
              "I helped build."
            )
          ),
          <.p(
            ^.className := "selected-work__intro",
            "A 30-second skim. The full story — bullets, results, stack — lives in ",
            <.a(^.href := "#experience", ^.className := "selected-work__intro-link", "Experience"),
            "."
          )
        ),
        <.div(
          ^.className := "selected-work__rows",
          featured.toTagMod { exp =>
            val years    = yearsOnly(exp.time)
            val location = exp.location.toOption.getOrElse("")
            val role     = exp.roleTag.toOption.getOrElse("")
            val metaBits = List(years, location, role).filter(_.nonEmpty)
            val blurb    = exp.selectedWorkBlurb.toOption.getOrElse(exp.description)
            val tags     = exp.primaryTech.toOption.getOrElse(exp.leveragedKnowledgeIn).take(4)

            <.a(
              ^.key       := exp.company.short,
              ^.href      := "#experience",
              ^.className := "selected-work__row",
              <.span(^.className := "selected-work__row-rail", ^.aria.hidden := true),
              <.div(
                ^.className := "selected-work__row-company",
                exp.company.displayName.toOption.getOrElse(exp.company.short)
              ),
              <.div(
                ^.className := "selected-work__row-meta",
                metaBits.mkString(" · ")
              ),
              <.div(
                ^.className := "selected-work__row-blurb",
                blurb
              ),
              <.div(
                ^.className := "selected-work__row-tags",
                tags.map(t => <.span(^.key := t, ^.className := "selected-work__row-tag", t)).toTagMod
              ),
              <.div(
                ^.className := "selected-work__row-arrow",
                LucideIcons.ArrowRight(LucideIcons.withClass("selected-work__row-arrow-icon"))
              )
            )
          }
        )
      )
    )
  }
