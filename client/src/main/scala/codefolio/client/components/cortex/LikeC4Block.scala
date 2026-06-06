package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Wraps a LikeC4 view iframe (served from the `/c4` proxy) with a hover-visible Zoom button that opens a
 * near-fullscreen modal. The LikeC4 SPA owns pan/zoom inside the diagram itself, so unlike [[D2Diagram]] this
 * component does not apply an outer CSS scale — it just gives the SPA a bigger viewport to render into.
 *
 * The inline iframe and the modal iframe are two distinct `<iframe>` elements with the same `src`. Re-
 * parenting the original iframe into the modal would reload the LikeC4 SPA inside it and lose any pan / zoom
 * state the reader had set up.
 */
object LikeC4Block:

  private val DefaultHeight = 520

  // `loading` is a relatively new HTML attribute; scalajs-react's typed `^.` namespace
  // doesn't expose it yet, so build it via the generic `VdomAttr` escape hatch.
  private val loadingAttr = VdomAttr[String]("loading")

  /**
   * LikeC4's React Flow gates wheel-zoom on a modifier key (`zoomActivationKeyCode: Meta` on macOS, `Control`
   * elsewhere). Trackpad pinch works regardless because Chrome/Safari emit pinch gestures as `wheel +
   * ctrlKey:true`, which passes the gate; a bare mouse-wheel sends `ctrlKey:false` and is ignored.
   *
   * On macOS we show ⌘ first (the platform-native modifier) but include Ctrl too — both ultimately deliver
   * `ctrlKey:true` on the wheel event, and external Windows-style keyboards plugged into a Mac are common
   * enough that hiding the Ctrl option costs more than the extra glyph. On non-mac we just say Ctrl.
   */
  private val zoomShortcutLabel: String =
    val ua = dom.window.navigator.userAgent
    if ua.contains("Mac") || ua.contains("iPhone") || ua.contains("iPad") then "⌘ / Ctrl + Scroll"
    else "Ctrl + Scroll"

  final case class Props(src: String, height: Option[Int], title: Option[String])

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .useRefBy(_ => js.Array[js.Function0[Unit]]())
      .useEffectWithDepsBy((_, openS, _) => openS.value) { (_, openS, cleanupRef) => isOpen =>
        val tearDown: Callback = Callback {
          val arr = cleanupRef.value
          for i <- 0 until arr.length do arr(i)()
          cleanupRef.value = js.Array()
        }

        val install: Callback =
          if !isOpen then Callback.empty
          else
            Callback {
              val previousOverflow = dom.document.body.style.overflow
              dom.document.body.style.overflow = "hidden"

              val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
                if e.key == "Escape" then
                  e.preventDefault()
                  openS.setState(false).runNow()

              dom.document.addEventListener("keydown", onKey)
              cleanupRef.value.push { () =>
                dom.document.removeEventListener("keydown", onKey)
                dom.document.body.style.overflow = previousOverflow
                ()
              }
              ()
            }

        tearDown >> install
      }
      .render { (props, openS, _) =>
        val open: Callback  = openS.setState(true)
        val close: Callback = openS.setState(false)

        val labelTitle = props.title.getOrElse("LikeC4 diagram")

        <.div(
          <.div(
            ^.className := "likec4-iframe group not-prose",
            <.iframe(
              ^.className := "likec4-iframe__frame",
              ^.src       := props.src,
              ^.title     := labelTitle,
              // scalajs-react's typed `^.height` is silently dropped on
              // <iframe> in the React VDOM layer (verified in the DOM —
              // the attribute never lands). Use inline CSS instead.
              ^.style := js.Dynamic
                .literal(height = s"${props.height.getOrElse(DefaultHeight)}px")
                .asInstanceOf[js.Object],
              loadingAttr := "lazy"
            ),
            <.button(
              ^.tpe := "button",
              ^.onClick --> open,
              ^.aria.label := "Open diagram in fullscreen",
              ^.className  := "likec4-iframe__zoom-button",
              LucideIcons.Maximize2(LucideIcons.withClass("likec4-iframe__zoom-icon")),
              "Zoom"
            )
          ),
          if openS.value then
            <.div(
              ^.role       := "dialog",
              ^.aria.modal := true,
              ^.aria.label := s"$labelTitle — fullscreen view",
              ^.className  := "likec4-modal not-prose",
              ^.onClick --> close,
              <.div(
                ^.className := "likec4-modal__card",
                ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
                <.iframe(
                  ^.className := "likec4-modal__frame",
                  ^.src       := props.src,
                  ^.title     := labelTitle,
                  loadingAttr := "lazy"
                ),
                // LikeC4's viewer doesn't render React Flow controls (the bundle's
                // `enableControls: false` is hardcoded), so we surface the right
                // shortcuts ourselves. Wheel-zoom is gated on `zoomActivationKeyCode`
                // — Meta on macOS, Control elsewhere — which is why a bare mouse
                // wheel does nothing. Synthetic +/− buttons aren't an option:
                // d3-zoom under React Flow rejects untrusted wheel events.
                <.div(
                  ^.className   := "likec4-modal__hint",
                  ^.aria.hidden := true,
                  s"$zoomShortcutLabel to zoom · Drag to pan · Pinch on trackpad"
                ),
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> close,
                  ^.aria.label := "Close",
                  ^.className  := "likec4-modal__close",
                  LucideIcons.X(LucideIcons.withClass("likec4-modal__close-icon"))
                )
              )
            )
          else EmptyVdom
        )
      }
