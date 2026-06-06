package cortex.client.components.book

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Slim chrome bar that slides in below the site header once the chapter H1 has scrolled out of view. Shows
 * `Ch. NN · <Chapter title> / <Active section>` so the user has lightweight wayfinding once the masthead is
 * no longer visible. No percent or time-remaining indicators (intentionally dropped per chat-4 user feedback
 * — visual signals only).
 *
 * Activation is driven by the H1's bounding rect via a passive scroll listener; the active section name is
 * supplied by the parent layout (which subscribes to the right-TOC's `cortex:activeHeading` event).
 */
object CortexStickyBar:

  final case class Props(
      chapterNumber: Int,
      chapterTitle: String,
      activeSectionTitle: Option[String]
  )

  private def numberLabel(n: Int): String = if n <= 0 then "" else f"$n%02d"

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .useEffectOnMountBy { (_, visibleS) =>
        Callback {
          val onScroll: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            val h1 = dom.document.querySelector(".cortex-reader-prose__title")
            val past =
              if h1 == null then false
              else h1.asInstanceOf[dom.html.Element].getBoundingClientRect().bottom < 8
            if past != visibleS.value then visibleS.setState(past).runNow()
          dom.window.addEventListener("scroll", onScroll, useCapture = false)
          onScroll(null.asInstanceOf[dom.Event])
          ()
        }
      }
      .render { (props, visibleS) =>
        val num = numberLabel(props.chapterNumber)
        <.div(
          ^.className         := "cortex-reader-sticky-bar",
          VdomAttr("data-on") := (if visibleS.value then "true" else "false"),
          ^.role              := "presentation",
          <.span(
            ^.className := "cortex-reader-sticky-bar__chapter",
            if num.nonEmpty then TagMod("Ch. ", <.strong(num), " · ", <.strong(props.chapterTitle))
            else <.strong(props.chapterTitle)
          ),
          props.activeSectionTitle match
            case Some(section) =>
              TagMod(
                <.span(^.className := "cortex-reader-sticky-bar__sep", ^.aria.hidden := true, "/"),
                <.span(^.className := "cortex-reader-sticky-bar__section", section)
              )
            case None => TagMod.empty
        )
      }
