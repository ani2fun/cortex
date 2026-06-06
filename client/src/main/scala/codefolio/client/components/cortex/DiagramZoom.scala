package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * The fullscreen zoom modal shared by [[D2Diagram]] and [[MermaidBlock]].
 *
 * Both diagram blocks differ in how they *obtain* their SVG — D2 receives it pre-rendered as a prop, Mermaid
 * renders it client-side and snapshots it on open — but the modal itself (zoom state, the
 * `Escape`/`+`/`-`/`0` keyboard shortcuts, the body-scroll lock, and the toolbar + viewport chrome) is
 * identical. It lives here so a fix lands once.
 *
 * The parent owns the open/closed state and passes it in as `isOpen` (and `onClose` to flip it back). This
 * component is always mounted; it owns only the zoom level, which it resets whenever it closes — so each open
 * starts at 100%. When `isOpen` is false it renders nothing.
 */
object DiagramZoom:

  /**
   * @param svgHtml
   *   the SVG markup to display inside the modal
   * @param isOpen
   *   whether the modal is currently shown — owned by the parent block
   * @param onClose
   *   flips the parent's open state back to closed (invoked by the backdrop, the close button, and `Escape`)
   */
  final case class Props(svgHtml: String, isOpen: Boolean, onClose: Callback)

  private val MinZoom = 0.5
  private val MaxZoom = 4.0
  private val Step    = 0.25

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(1.0)                                 // zoomS
      .useRefBy(_ => js.Array[js.Function0[Unit]]()) // cleanupRef (modal keydown + scroll-lock teardown)
      // ─── modal keyboard handling + body scroll lock, keyed on open/closed ──────────────────────────────
      .useEffectWithDepsBy((props, _, _) => props.isOpen) { (props, zoomS, cleanupRef) => isOpen =>
        val tearDown: Callback = Callback {
          val arr = cleanupRef.value
          for i <- 0 until arr.length do arr(i)()
          cleanupRef.value = js.Array()
        }

        val install: Callback =
          // Closed: reset the zoom so the next open starts at 100%. Open: lock scroll + bind shortcuts.
          if !isOpen then zoomS.setState(1.0)
          else
            Callback {
              val previousOverflow = dom.document.body.style.overflow
              dom.document.body.style.overflow = "hidden"

              val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
                e.key match
                  case "Escape" =>
                    e.preventDefault()
                    props.onClose.runNow()
                  case "+" | "=" =>
                    e.preventDefault()
                    zoomS.modState(z => math.min(MaxZoom, z + Step)).runNow()
                  case "-" =>
                    e.preventDefault()
                    zoomS.modState(z => math.max(MinZoom, z - Step)).runNow()
                  case "0" =>
                    e.preventDefault()
                    zoomS.setState(1.0).runNow()
                  case _ => ()

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
      .render { (props, zoomS, _) =>
        if !props.isOpen then EmptyVdom
        else
          val zoomIn: Callback    = zoomS.modState(z => math.min(MaxZoom, z + Step))
          val zoomOut: Callback   = zoomS.modState(z => math.max(MinZoom, z - Step))
          val zoomReset: Callback = zoomS.setState(1.0)

          <.div(
            ^.role       := "dialog",
            ^.aria.modal := true,
            ^.aria.label := "Diagram fullscreen view",
            ^.className  := "diagram-modal not-prose",
            ^.onClick --> props.onClose,
            <.div(
              ^.className := "diagram-modal__toolbar",
              ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
              <.button(
                ^.tpe := "button",
                ^.onClick --> zoomOut,
                ^.aria.label := "Zoom out",
                ^.className  := "diagram-modal__button",
                LucideIcons.ZoomOut(LucideIcons.withClass("diagram-modal__button-icon"))
              ),
              <.span(
                ^.className := "diagram-modal__zoom-readout",
                s"${math.round(zoomS.value * 100).toInt}%"
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> zoomIn,
                ^.aria.label := "Zoom in",
                ^.className  := "diagram-modal__button",
                LucideIcons.ZoomIn(LucideIcons.withClass("diagram-modal__button-icon"))
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> zoomReset,
                ^.aria.label := "Reset zoom",
                ^.className  := "diagram-modal__button",
                LucideIcons.RotateCcw(LucideIcons.withClass("diagram-modal__button-icon"))
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> props.onClose,
                ^.aria.label := "Close",
                ^.className  := "diagram-modal__button",
                LucideIcons.X(LucideIcons.withClass("diagram-modal__button-icon"))
              )
            ),
            <.div(
              ^.className := "diagram-modal__viewport",
              <.div(
                ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
                ^.style := js.Dynamic
                  .literal(
                    width = s"${85.0 * zoomS.value}vw",
                    height = s"${78.0 * zoomS.value}vh"
                  )
                  .asInstanceOf[js.Object],
                ^.className := "diagram-modal__card",
                <.div(
                  ^.className               := "diagram-modal__svg not-prose",
                  ^.dangerouslySetInnerHtml := props.svgHtml
                )
              )
            )
          )
      }
