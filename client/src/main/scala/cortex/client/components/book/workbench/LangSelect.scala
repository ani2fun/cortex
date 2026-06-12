package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Language dropdown in the workbench Code toolbar — replaces the old tab strip. One entry per workbench tab;
 * a brand glyph + the pipeline's language label with its leading emoji stripped (the dropdown brings its own
 * glyph column, per the design).
 */
object LangSelect:

  final case class Props(
      tabs: List[Block.Tab],
      activeIdx: Int,
      onChange: Int => Callback
  )

  /** Brand glyph + its tint class, mirroring the design's `LANGS` glyph column. */
  private def glyphFor(language: String): (String, String) = language.toLowerCase match
    case l if l.startsWith("python") || l == "py" => ("▸", "langselect__glyph--python")
    case "java"                                   => ("◆", "langselect__glyph--java")
    case l if l.startsWith("scala")               => ("◗", "langselect__glyph--scala")
    case _                                        => ("•", "langselect__glyph--other")

  /** "🐍 Python 3.8" → "Python 3.8" — the emoji is the old tab strip's icon; the dropdown has its own. */
  private def displayLabel(tab: Block.Tab): String =
    val stripped = tab.languageLabel.replaceAll("^[^\\p{ASCII}]+\\s*", "").trim
    if stripped.nonEmpty then stripped else tab.language

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .render { (props, openS) =>
        val open                    = openS.value
        val cur                     = props.tabs.lift(props.activeIdx).getOrElse(props.tabs.head)
        val (curGlyph, curGlyphCls) = glyphFor(cur.language)

        <.div(
          ^.className := "langselect",
          <.button(
            ^.tpe       := "button",
            ^.title     := "Select language",
            ^.className := (if open then "langselect__btn langselect__btn--open" else "langselect__btn"),
            ^.onClick --> openS.modState(!_),
            <.span(^.className := s"langselect__glyph $curGlyphCls", curGlyph),
            displayLabel(cur),
            LucideIcons.ChevronDown(
              LucideIcons.withClass(
                if open then "langselect__chevron langselect__chevron--open" else "langselect__chevron"
              )
            )
          ),
          if open then
            React.Fragment(
              <.div(^.className := "langselect__backdrop", ^.onClick --> openS.setState(false)),
              <.div(
                ^.className := "langselect__menu",
                props.tabs.zipWithIndex.toVdomArray { case (tab, i) =>
                  val on                = i == props.activeIdx
                  val (glyph, glyphCls) = glyphFor(tab.language)
                  <.button(
                    ^.key := s"lang-$i",
                    ^.tpe := "button",
                    ^.className := (if on then "langselect__item langselect__item--active"
                                    else "langselect__item"),
                    ^.onClick --> (props.onChange(i) >> openS.setState(false)),
                    <.span(^.className := s"langselect__glyph $glyphCls", glyph),
                    <.span(^.className := "langselect__item-label", displayLabel(tab)),
                    if on then LucideIcons.Check(LucideIcons.withClass("langselect__check"))
                    else EmptyVdom
                  )
                }
              )
            )
          else EmptyVdom
        )
      }
