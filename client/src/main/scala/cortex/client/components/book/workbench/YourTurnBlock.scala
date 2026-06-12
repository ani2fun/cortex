package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The `## Your Turn` exercise section, rendered as the SAME two-pane workbench problem pages use — inline in
 * the chapter flow, no popup: Description tab = the packaged statement, Editorial tab = the authored
 * `<details>Editorial`, starter tabs + test cases in the editor pane. A slim kicker row above the panel keeps
 * the section scannable while reading past it.
 *
 * No Submit here — Your-Turn exercises aren't addressable problems, so there's nothing to record a submission
 * against.
 */
object YourTurnBlock:

  final case class Props(block: Block.YourTurn)

  /** "🐍 Python 3.8" → "Python" — the kicker's meta wants language names, not versions. */
  private def shortLang(tab: Block.Tab): String =
    tab.languageLabel
      .replaceAll("^[^\\p{ASCII}]+\\s*", "")
      .trim
      .split("\\s+")
      .headOption
      .getOrElse(tab.language)

  val Component =
    ScalaFnComponent[Props] { props =>
      val b     = props.block
      val cases = b.spec.cases.length
      val langs = b.tabs.map(shortLang).distinct.mkString(" · ")

      val sections =
        if b.editorialHtml.trim.nonEmpty then List(Block.EdSection("Editorial", b.editorialHtml))
        else Nil

      <.section(
        ^.className := "yt-inline not-prose",
        <.div(
          ^.className := "yt-inline__head",
          <.span(
            ^.className := "yt-inline__kicker",
            LucideIcons.Target(LucideIcons.withClass("yt-inline__kicker-icon")),
            "Your Turn"
          ),
          <.span(
            ^.className := "yt-inline__meta",
            s"$cases test case${if cases == 1 then "" else "s"} · $langs"
          )
        ),
        ProblemWorkbench.Component(
          ProblemWorkbench.Props(
            title = b.title,
            difficulty = None,
            topics = Nil,
            descriptionHtml = b.statementHtml,
            sections = sections,
            tabs = b.tabs,
            spec = b.spec,
            heightCss = "min(calc(100vh - 7rem), 820px)",
            submitCtx = None
          )
        )
      )
    }
