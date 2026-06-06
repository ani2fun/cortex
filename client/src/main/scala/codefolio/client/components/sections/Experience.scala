package codefolio.client.components.sections

import codefolio.client.components.icons.LucideIcons
import codefolio.client.components.ui.Section
import codefolio.client.data.PortfolioData
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Experience — vertical accordion. Replaces the previous horizontal tab rail (which jailed the user inside a
 * fixed-height inner-scrolled pane on long roles like Europcar). One role open by default; clicking another
 * header swaps which is open. State is a single `Int` — multi-open felt fiddly and the design brief
 * explicitly calls for "one expanded by default".
 */
object Experience:

  /**
   * Split a tech list into 2–3 promoted "primary" chips + the rest. Falls back to first 3 / remainder when
   * explicit `primaryTech` isn't set yet.
   */
  private def splitTech(exp: PortfolioData.Experience): (List[String], List[String]) =
    val all     = exp.leveragedKnowledgeIn.toList
    val primary = exp.primaryTech.toOption.map(_.toList).getOrElse(all.take(3))
    val secondary = exp.secondaryTech.toOption
      .map(_.toList)
      .getOrElse(all.filterNot(primary.toSet.contains))
    (primary, secondary)

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(0)
      .render { (_, openS) =>
        val items = PortfolioData.experience
        val total = items.length

        Section("experience", "experience")(
          <.div(
            ^.className := "experience__inner",
            <.div(^.className := "experience__eyebrow", s"EXPERIENCE · $total ROLES"),
            <.h2(
              ^.className := "experience__title",
              "Where the",
              <.br,
              "hours went."
            ),
            <.div(
              ^.className := "experience__list",
              items.zipWithIndex.toList.toTagMod { case (exp, idx) =>
                renderRole(exp, openS.value == idx, openS.setState(idx))
              }
            )
          )
        )
      }

  private def renderRole(
      exp: PortfolioData.Experience,
      open: Boolean,
      onOpen: Callback
  ): VdomNode =
    val (primary, secondary) = splitTech(exp)
    val role                 = exp.roleTag.toOption.getOrElse(exp.position)

    <.article(
      ^.key       := exp.company.short,
      ^.className := s"experience__role${if open then " experience__role--open" else ""}"
    )(
      <.button(
        ^.className := "experience__role-header",
        ^.onClick --> onOpen,
        ^.aria.expanded := open,
        <.span(^.className := "experience__role-company", exp.company.short),
        <.span(^.className := "experience__role-meta", exp.time + " · " + role),
        <.span(
          ^.className := "experience__role-chevron",
          LucideIcons.ChevronDown(LucideIcons.withClass("experience__role-chevron-icon"))
        )
      ),
      // Always render the body so max-height can animate; the --open
      // modifier on the parent flips overflow + max-height.
      <.div(
        ^.className := "experience__role-body",
        renderBody(exp, primary, secondary)
      )
    )

  private def renderBody(
      exp: PortfolioData.Experience,
      primary: List[String],
      secondary: List[String]
  ): VdomNode =
    <.div(
      ^.className := "experience__role-body-inner",
      <.div(
        ^.className := "experience__role-position",
        exp.position,
        " · ",
        <.a(^.href := exp.company.url, ^.className := "experience__role-link", exp.company.name)
      ),
      if primary.nonEmpty then
        <.div(
          ^.className := "experience__primary-tags",
          primary.toTagMod(t =>
            <.span(^.key := t, ^.className := "experience__primary-tag", t)
          )
        )
      else EmptyVdom,
      <.p(^.className := "experience__summary", exp.description),
      <.ul(
        ^.className := "experience__bullets",
        exp.items.toList.zipWithIndex.toTagMod { case (text, i) =>
          <.li(^.key := i, ^.className := "experience__bullet", text)
        }
      ),
      renderResults(exp.results),
      if secondary.nonEmpty then
        <.div(
          ^.className := "experience__secondary-row",
          <.span(^.className := "experience__secondary-label", "ALSO"),
          secondary.toTagMod(t =>
            <.span(^.key := t, ^.className := "experience__secondary-tag", t)
          )
        )
      else EmptyVdom
    )

  private def renderResults(maybe: js.UndefOr[js.Array[String]]): VdomNode =
    val results = maybe.toOption.toList.flatMap(_.toList)
    if results.isEmpty then EmptyVdom
    else
      <.div(
        ^.className := "experience__results",
        <.div(^.className := "experience__results-title", "RESULTS"),
        <.ul(
          ^.className := "experience__results-list",
          results.zipWithIndex.toTagMod { case (res, i) =>
            <.li(^.key := i, ^.className := "experience__results-item", res)
          }
        )
      )
