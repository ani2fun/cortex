package cortex.client.components.book

import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Focus-mode chrome: the entry FAB, the persistent top-right hint pill ("Press F or Esc to exit"), and the
 * left-edge reveal zone. The actual fading of site chrome is owned by `body.cortex-focus-mode` CSS rules in
 * `cortex-reader.css` + `header.css`. Body-class management lives in `CortexReaderLayout` (so the F-key
 * listener can see/toggle it without prop drilling).
 */
object CortexFocusMode:

  /** Milliseconds before the focus-mode hint fades to its low-opacity resting state. */
  private val HintFadeMs = 2500

  final case class Props(focused: Boolean, onToggle: Callback)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      // Hint fade state — false means visible, true means dimmed. Reset to false when entering
      // focus, then auto-fades after HintFadeMs unless the cursor approaches the top edge.
      .useState(false)
      .useRef(js.undefined: js.UndefOr[Int])
      .useEffectWithDepsBy((p, _, _) => p.focused) { (_, fadedS, fadeTimerRef) => focused =>
        Callback {
          fadeTimerRef.value.foreach(dom.window.clearTimeout)
          if focused then
            fadedS.setState(false).runNow()
            val handle = dom.window.setTimeout(
              () => fadedS.setState(true).runNow(),
              HintFadeMs.toDouble
            )
            fadeTimerRef.value = handle
          else
            fadedS.setState(false).runNow()
            fadeTimerRef.value = js.undefined
        }
      }
      // Re-reveal the hint when the cursor approaches the top of the viewport (clientY < 80).
      .useEffectOnMountBy { (_, fadedS, fadeTimerRef) =>
        Callback {
          val onMove: js.Function1[dom.MouseEvent, Unit] = (e: dom.MouseEvent) =>
            if dom.document.body.classList.contains("cortex-focus-mode") && e.clientY < 80 then
              fadedS.setState(false).runNow()
              fadeTimerRef.value.foreach(dom.window.clearTimeout)
              val handle = dom.window.setTimeout(
                () => fadedS.setState(true).runNow(),
                HintFadeMs.toDouble
              )
              fadeTimerRef.value = handle
          dom.document.addEventListener("mousemove", onMove, useCapture = false)
          ()
        }
      }
      .render { (props, fadedS, _) =>
        val ariaLabel =
          if props.focused then "Exit focus mode (F)" else "Enter focus mode (F)"

        <.div(
          // Focus-mode entry FAB — sits in the right-edge fab stack, above the type/TOC stack.
          // Positioned by CSS at right: 20px; bottom: 188px (back-to-top 20 + 44 + 12 + type 44 + 12 +
          // TOC 44 + 12 = 188).
          <.button(
            ^.tpe          := "button",
            ^.className    := "cortex-reader-focus-fab",
            ^.aria.label   := ariaLabel,
            ^.aria.pressed := props.focused,
            ^.onClick --> props.onToggle,
            LucideIcons.Focus(LucideIcons.withClass("cortex-reader-focus-fab__icon"))
          ),
          // Hint pill — visible only when focused. Listens for clicks to exit focus mode (matches the
          // preview's behaviour where clicking the hint also exits).
          <.div(
            ^.className              := "cortex-reader-focus-hint",
            ^.role                   := "status",
            VdomAttr("data-focused") := (if props.focused then "true" else "false"),
            VdomAttr("data-faded")   := (if fadedS.value then "true" else "false"),
            ^.onClick --> props.onToggle,
            <.span(^.className := "cortex-reader-focus-hint__dot", ^.aria.hidden := true),
            <.span("Focus mode"),
            <.span(^.aria.hidden := true, " · "),
            <.span(
              "Press ",
              <.kbd("F"),
              " or ",
              <.kbd("Esc"),
              " to exit"
            )
          ),
          // Left-edge reveal zone — clicking it exits focus mode. Rendered only when focused.
          if props.focused then
            <.div(
              ^.className   := "cortex-reader-focus-edge",
              ^.aria.hidden := true,
              ^.onClick --> props.onToggle
            )
          else EmptyVdom
        )
      }
