package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Floating "back to top" button that fades in once the user has scrolled past a threshold. Useful on long DSA
 * chapters where the prev/next pager sits 8–10 screens down.
 */
object ScrollToTop:

  /** Pixels of vertical scroll before the button appears. */
  private val Threshold = 600

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(false)
      .useEffectBy { (_, visibleS) =>
        Callback {
          val handler: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            val shouldShow = dom.window.scrollY > Threshold
            if shouldShow != visibleS.value then visibleS.setState(shouldShow).runNow()
          dom.window.addEventListener("scroll", handler, useCapture = false)
          // Fire once at mount so the button is correct if the user lands
          // mid-page (e.g. via a deep link with a hash).
          handler(null.asInstanceOf[dom.Event])
        }
      }
      .render { (_, visibleS) =>
        val cls =
          if visibleS.value then "scroll-to-top scroll-to-top--visible"
          else "scroll-to-top scroll-to-top--hidden"
        <.button(
          ^.tpe        := "button",
          ^.className  := cls,
          ^.aria.label := "Back to top",
          ^.onClick --> Callback {
            dom.window.scrollTo(0, 0)
          },
          LucideIcons.ArrowUp(LucideIcons.withClass("scroll-to-top__icon"))
        )
      }
