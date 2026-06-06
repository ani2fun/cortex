package codefolio.client.components.cortex

import codefolio.client.components.icons.BrandIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * A tabbed group of runnable code blocks. The markdown pipeline merges adjacent ` ```<lang> run ` fences into
 * a single placeholder div with a `data-tabs` JSON payload; the post-mount walker decodes that payload into
 * `Tab` records and instantiates this component.
 *
 * Each tab renders a `bare` RunnableCodeBlock (no inner card — the group provides the outer card) toggled via
 * `hidden`, mirroring the original TS port. Per-tab state lives inside each child block.
 */
object RunnableCodeGroup:

  final case class Tab(
      language: String,
      languageLabel: String,
      source: String,
      runnable: Boolean = true,
      viz: Option[String] = None,
      vizRoot: Option[String] = None,
      vizCase: Option[Int] = None,
      vizKind: Option[String] = None
  )

  final case class Props(tabs: List[Tab])

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(0)
      .render { (props, activeS) =>
        if props.tabs.isEmpty then EmptyVdom
        else if props.tabs.sizeIs == 1 then
          val only = props.tabs.head
          RunnableCodeBlock.Component(
            RunnableCodeBlock.Props(
              only.language,
              only.source,
              Some(only.languageLabel),
              runnable = only.runnable,
              viz = only.viz,
              vizRoot = only.vizRoot,
              vizCase = only.vizCase,
              vizKind = only.vizKind
            )
          )
        else
          val active = if activeS.value >= props.tabs.size then 0 else activeS.value
          <.div(
            ^.className := "rcg",
            <.div(
              ^.role      := "tablist",
              ^.className := "rcg__tablist",
              props.tabs.zipWithIndex.toTagMod { case (tab, i) =>
                val isActive = i == active
                val cls =
                  if isActive then "rcg__tab rcg__tab--active"
                  else "rcg__tab"
                <.button(
                  ^.key           := s"${tab.language}-$i",
                  ^.role          := "tab",
                  ^.tpe           := "button",
                  ^.aria.selected := isActive,
                  ^.onClick --> activeS.setState(i),
                  ^.className := cls,
                  // Real brand icon prefix where the emoji-only label is too vague
                  // (Scala). Other languages keep their emoji prefix in the string.
                  if tab.language.equalsIgnoreCase("scala") then
                    TagMod(BrandIcons.Scala("rcg__brand-icon rcg__brand-icon--scala"), tab.languageLabel)
                  else tab.languageLabel
                )
              }
            ),
            props.tabs.zipWithIndex.toTagMod { case (tab, i) =>
              <.div(
                ^.key    := s"${tab.language}-$i",
                ^.hidden := (i != active),
                RunnableCodeBlock.Component(
                  RunnableCodeBlock.Props(
                    language = tab.language,
                    source = tab.source,
                    languageLabel = Some(tab.languageLabel),
                    bare = true,
                    hideLanguageLabel = true,
                    runnable = tab.runnable,
                    viz = tab.viz,
                    vizRoot = tab.vizRoot,
                    vizCase = tab.vizCase,
                    vizKind = tab.vizKind
                  )
                )
              )
            }
          )
      }
