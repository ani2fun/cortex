package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import cortex.shared.tutor.TutorContract.ModelOption
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The coach model picker — the header dropdown over `GET /v1/whoami`'s `availableModels` (already
 * tier-filtered server-side). Mirrors [[LangSelect]]'s button + chevron + menu pattern so the two workbench
 * dropdowns stay visually in step.
 *
 * The tutor pins the coach model at session creation, so once a session is live the picker is **locked**: it
 * renders the chosen model as a static chip instead of an interactive dropdown (changing it has no effect).
 */
object ModelSelect:

  final case class Props(
      models: List[ModelOption],
      selectedKey: Option[String],
      locked: Boolean,
      onChange: String => Callback
  )

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false) // menu open
      .render { (props, openS) =>
        val models = props.models
        if models.isEmpty then EmptyVdom
        else
          val cur = props.selectedKey.flatMap(k => models.find(_.key == k)).getOrElse(models.head)

          if props.locked then
            // Pinned for the life of the session — a read-only chip, not a dropdown.
            <.div(
              ^.className := "modelselect modelselect--locked",
              ^.title     := "The coach model is fixed for this session — Reset to change it.",
              LucideIcons.Sparkles(LucideIcons.withClass("modelselect__glyph")),
              <.span(^.className := "modelselect__label", cur.display)
            )
          else
            val open = openS.value
            <.div(
              ^.className := "modelselect",
              <.button(
                ^.tpe   := "button",
                ^.title := "Switch the coach model anytime — your conversation carries over.",
                ^.className := (if open then "modelselect__btn modelselect__btn--open"
                                else "modelselect__btn"),
                ^.onClick --> openS.modState(!_),
                LucideIcons.Sparkles(LucideIcons.withClass("modelselect__glyph")),
                <.span(^.className := "modelselect__label", cur.display),
                LucideIcons.ChevronDown(
                  LucideIcons.withClass(
                    if open then "modelselect__chevron modelselect__chevron--open" else "modelselect__chevron"
                  )
                )
              ),
              if open then
                React.Fragment(
                  <.div(^.className := "modelselect__backdrop", ^.onClick --> openS.setState(false)),
                  <.div(
                    ^.className := "modelselect__menu",
                    models.toVdomArray { m =>
                      val on = m.key == cur.key
                      <.button(
                        ^.key := m.key,
                        ^.tpe := "button",
                        ^.className := (if on then "modelselect__item modelselect__item--active"
                                        else "modelselect__item"),
                        ^.onClick --> (props.onChange(m.key) >> openS.setState(false)),
                        <.span(^.className := "modelselect__item-label", m.display),
                        if on then LucideIcons.Check(LucideIcons.withClass("modelselect__check"))
                        else EmptyVdom
                      )
                    }
                  )
                )
              else EmptyVdom
            )
      }
