package cortex.client.components.book

import cortex.client.components.Theme
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

/**
 * Renders a single mermaid diagram with the same fullscreen-zoom affordance as D2 diagrams.
 *
 * The mermaid SVG is generated client-side by `client/src/markdown/runtime.ts#renderMermaidInto`. Once the
 * inline SVG is in the DOM we snapshot its `outerHTML` on open and hand it to [[DiagramZoom]] — the shared
 * zoom modal, also used by [[D2Diagram]]. This component owns only the mermaid-specific parts: rendering into
 * the inline container, re-rendering on theme flips, and capturing the SVG when the reader opens the modal.
 *
 * Re-renders when `props.source` changes or when the theme flips — mermaid bakes the resolved theme into the
 * SVG.
 */
object MermaidBlock:

  @js.native @JSImport("@markdown/runtime", "renderMermaidInto")
  private def renderMermaidInto(
      target: dom.HTMLElement,
      source: String,
      dark: Boolean
  ): js.Promise[Unit] = js.native

  final case class Props(source: String)

  private given ExecutionContext = JSExecutionContext.queue

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(Option.empty[String]) // errS
      .useState(Theme.current)        // modeS
      .useState(false)                // openS
      .useState(Option.empty[String]) // capturedSvgS
      .useRefToVdom[dom.html.Div]     // inlineRef (where mermaid renders into)
      // ─── render mermaid into the inline container ──────────────────────────────────────────────────────
      .useEffectWithDepsBy((p, _, modeS, _, _, _) => (p.source, modeS.value == Theme.Mode.Dark)) {
        (_, errS, _, _, _, inlineRef) => (source, isDark) =>
          inlineRef.foreach { el =>
            renderMermaidInto(el.asInstanceOf[dom.HTMLElement], source, isDark).toFuture
              .onComplete {
                case Success(_) =>
                  if errS.value.nonEmpty then errS.setState(None).runNow()
                case Failure(t) =>
                  errS.setState(Some(Option(t.getMessage).getOrElse("Diagram failed"))).runNow()
              }
          }
      }
      // ─── watch theme class on <html> so dark/light flips trigger re-render ──────────────────────────────
      .useEffectOnMountBy { (_, _, modeS, _, _, _) =>
        Callback {
          val obs = new dom.MutationObserver({ (_, _) =>
            val now = Theme.current
            if now != modeS.value then modeS.setState(now).runNow()
          })
          obs.observe(
            dom.document.documentElement,
            new dom.MutationObserverInit {
              attributes = true
              attributeFilter = js.Array("class")
            }
          )
        }
      }
      .render { (_, errS, _, openS, capturedSvgS, inlineRef) =>
        val close: Callback = openS.setState(false)

        // Snapshot the inline SVG's outerHTML, then open the shared zoom modal with it. Capturing on open
        // (rather than holding a DOM ref) lets DiagramZoom re-render the SVG without re-parenting the
        // original element.
        val openModal: Callback =
          inlineRef.foreach { el =>
            val svg = el.querySelector("svg")
            if svg != null then
              capturedSvgS.setState(Some(svg.asInstanceOf[dom.html.Element].outerHTML)).runNow()
          } >> openS.setState(true)

        errS.value match
          case Some(msg) =>
            <.div(
              ^.className := "mermaid__error",
              <.p(^.className   := "mermaid__error-title", "Mermaid render error"),
              <.pre(^.className := "mermaid__error-message", msg)
            )
          case None =>
            <.div(
              <.div(
                ^.className := "mermaid group not-prose",
                <.div.withRef(inlineRef)(
                  ^.className  := "mermaid__svg",
                  ^.aria.label := "Diagram"
                ),
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> openModal,
                  ^.aria.label := "Open diagram in fullscreen",
                  ^.className  := "mermaid__zoom-button",
                  LucideIcons.Maximize2(LucideIcons.withClass("mermaid__zoom-icon")),
                  "Zoom"
                )
              ),
              DiagramZoom.Component(
                DiagramZoom.Props(
                  capturedSvgS.value.getOrElse(""),
                  openS.value && capturedSvgS.value.isDefined,
                  close
                )
              )
            )
      }
