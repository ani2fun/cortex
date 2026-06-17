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
 * Models are grouped by `provider` in the menu ("Via OpenRouter" / "Direct · Anthropic"), each row prefixed
 * with a provider dot, and the trigger carries a small via-tag — so the repeated "(OpenRouter)" suffix the
 * server used to bake onto every label is gone and the names line up clean.
 *
 * The picker stays switchable mid-session, but a switch now no-ops while the coach is `busy` (a turn or
 * another switch is in flight, server-bounded to 30s). So when `busy` the trigger goes non-interactive and
 * dimmed rather than offering a switch that would be silently dropped. When the controller reports the model
 * `locked` it renders a read-only chip with a lock glyph instead of a dropdown.
 */
object ModelSelect:

  final case class Props(
      models: List[ModelOption],
      selectedKey: Option[String],
      locked: Boolean,
      busy: Boolean,
      onChange: String => Callback
  )

  /**
   * Strip a trailing "(OpenRouter)" / "(direct)" / "(local)" provider suffix the server bakes into a label —
   * the provider now lives in the group header and the via-tag, so the name reads clean. Matches any trailing
   * parenthetical: Scala.js's JS-RegExp backend rejects scoped inline flags like `(?i:…)`, so we avoid them.
   */
  private def cleanDisplay(display: String): String =
    display.replaceAll("\\s*\\([^)]*\\)\\s*$", "")

  /** Short tag shown on the trigger; capitalised fallback keeps a third provider readable. */
  private def providerTag(provider: String): String = provider.toLowerCase match
    case "openrouter" => "OpenRouter"
    case "anthropic"  => "Direct"
    case other        => other.capitalize

  private def groupLabel(provider: String): String = provider.toLowerCase match
    case "openrouter" => "Via OpenRouter"
    case "anthropic"  => "Direct · Anthropic"
    case other        => other.capitalize

  private def dotClass(provider: String): String = provider.toLowerCase match
    case "openrouter" => "modelselect__dot modelselect__dot--or"
    case "anthropic"  => "modelselect__dot modelselect__dot--an"
    case _            => "modelselect__dot modelselect__dot--other"

  /** Fixed group order: OpenRouter, then Anthropic-direct, then any other provider (alphabetical). */
  private def providerOrder(provider: String): Int = provider.toLowerCase match
    case "openrouter" => 0
    case "anthropic"  => 1
    case _            => 2

  /** Group models by provider in the fixed order, preserving the server's order within each group. */
  private def groupedModels(models: List[ModelOption]): List[(String, List[ModelOption])] =
    models.groupBy(_.provider).toList.sortBy { case (p, _) => (providerOrder(p), p.toLowerCase) }

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
              LucideIcons.Lock(LucideIcons.withClass("modelselect__glyph")),
              <.span(^.className := "modelselect__label", cleanDisplay(cur.display)),
              <.span(^.className := "modelselect__via", providerTag(cur.provider))
            )
          else
            // `busy` keeps the menu shut and the trigger inert — a switch would be dropped mid-turn.
            val open = openS.value && !props.busy
            val btnClass =
              "modelselect__btn"
                + (if open then " modelselect__btn--open" else "")
                + (if props.busy then " modelselect__btn--busy" else "")
            <.div(
              ^.className := "modelselect",
              <.button(
                ^.tpe := "button",
                ^.title :=
                  (if props.busy then "Switching pauses while the coach is working — one moment."
                   else "Switch the coach model anytime — your conversation carries over."),
                ^.className := btnClass,
                ^.onClick --> (if props.busy then Callback.empty else openS.modState(!_)),
                LucideIcons.Sparkles(LucideIcons.withClass("modelselect__glyph")),
                <.span(^.className := "modelselect__label", cleanDisplay(cur.display)),
                <.span(^.className := "modelselect__via", providerTag(cur.provider)),
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
                    groupedModels(models).zipWithIndex.toVdomArray { case ((provider, items), groupIdx) =>
                      <.div(
                        ^.key       := s"group-$provider",
                        ^.className := "modelselect__group-block",
                        if groupIdx > 0 then <.div(^.className := "modelselect__divider") else EmptyVdom,
                        <.div(
                          ^.className := "modelselect__group",
                          <.span(^.className := "modelselect__group-label", groupLabel(provider))
                        ),
                        items.toVdomArray { m =>
                          val on = m.key == cur.key
                          <.button(
                            ^.key := m.key,
                            ^.tpe := "button",
                            ^.className := (if on then "modelselect__item modelselect__item--active"
                                            else "modelselect__item"),
                            ^.onClick --> (props.onChange(m.key) >> openS.setState(false)),
                            <.span(^.className := dotClass(m.provider)),
                            <.span(^.className := "modelselect__item-label", cleanDisplay(m.display)),
                            if on then LucideIcons.Check(LucideIcons.withClass("modelselect__check"))
                            else EmptyVdom
                          )
                        }
                      )
                    }
                  )
                )
              else EmptyVdom
            )
      }
