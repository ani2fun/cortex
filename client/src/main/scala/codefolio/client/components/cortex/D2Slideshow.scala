package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

/**
 * Step-through controller for a group of pre-rendered D2 SVG frames.
 *
 * Markdown authors mark a run of D2 fences with `<div class="d2-slides" ...>`. `render.ts` groups those
 * frames into one placeholder, and `ChapterContent` mounts this component with the captured SVG for each
 * frame.
 */
object D2Slideshow:

  final case class Props(slides: List[String], caption: Option[String])

  private val StepDelayMs = 1200

  private def clamp(i: Int, count: Int): Int =
    if count <= 0 then 0
    else math.max(0, math.min(count - 1, i))

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      // Each .useState(initial) is the Scala equivalent of React's `const [x, setX] = useState(initial)`.
      // The literal fixes both the initial value and the inferred type, and the hook is threaded
      // positionally into every later block — flipping the order would flip the render parameters.
      .useState(0)     // indexS  : current slide cursor
      .useState(false) // playingS: auto-advance flag toggled by the Play/Pause button
      .useRefBy(_ => Option.empty[Int])
      .useEffectWithDepsBy((props, indexS, playingS, _) =>
        (props.slides.length, indexS.value, playingS.value)
      ) { (_, indexS, playingS, timeoutRef) => (count, index, playing) =>
        Callback {
          timeoutRef.value.foreach(dom.window.clearTimeout)
          timeoutRef.value = None

          if playing then
            if index >= count - 1 then playingS.setState(false).runNow()
            else
              val id = dom.window.setTimeout(
                () => indexS.setState(index + 1).runNow(),
                StepDelayMs.toDouble
              )
              timeoutRef.value = Some(id)
        }
      }
      .render { (props, indexS, playingS, _) =>
        val count = props.slides.length
        if count == 0 then EmptyVdom
        else
          val index   = clamp(indexS.value, count)
          val current = props.slides(index)
          val atStart = index == 0
          val atEnd   = index == count - 1

          val previous =
            playingS.setState(false) >> indexS.modState(i => clamp(i - 1, count))
          val next =
            playingS.setState(false) >> indexS.modState(i => clamp(i + 1, count))
          val reset =
            playingS.setState(false) >> indexS.setState(0)
          val togglePlay =
            if playingS.value then playingS.setState(false)
            else
              val rewind = if atEnd then indexS.setState(0) else Callback.empty
              rewind >> playingS.setState(true)

          <.div(
            ^.className := "d2-slideshow not-prose",
            props.caption
              .map(caption =>
                <.p(
                  ^.className := "d2-slideshow__caption",
                  caption
                ): VdomNode
              )
              .getOrElse(EmptyVdom),
            <.div(
              ^.className := "d2-slideshow__frame",
              D2Diagram.Component(D2Diagram.Props(current))
            ),
            <.div(
              ^.className := "d2-slideshow__controls",
              <.button(
                ^.tpe := "button",
                ^.onClick --> previous,
                ^.disabled   := atStart,
                ^.aria.label := "Previous slide",
                ^.className  := "d2-slideshow__button",
                LucideIcons.ArrowLeft(LucideIcons.withClass("d2-slideshow__button-icon")),
                "Prev"
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> togglePlay,
                ^.aria.label := (if playingS.value then "Pause slideshow" else "Play slideshow"),
                ^.className  := "d2-slideshow__button d2-slideshow__button--primary",
                if playingS.value then
                  LucideIcons.Pause(LucideIcons.withClass("d2-slideshow__button-icon"))
                else LucideIcons.Play(LucideIcons.withClass("d2-slideshow__button-icon")),
                if playingS.value then "Pause" else "Play"
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> next,
                ^.disabled   := atEnd,
                ^.aria.label := "Next slide",
                ^.className  := "d2-slideshow__button",
                "Next",
                LucideIcons.ArrowRight(LucideIcons.withClass("d2-slideshow__button-icon"))
              ),
              <.button(
                ^.tpe := "button",
                ^.onClick --> reset,
                ^.disabled   := atStart && !playingS.value,
                ^.aria.label := "Reset slideshow",
                ^.className  := "d2-slideshow__button d2-slideshow__button--icon",
                LucideIcons.RotateCcw(LucideIcons.withClass("d2-slideshow__button-icon"))
              ),
              <.span(
                ^.className := "d2-slideshow__progress",
                ^.aria.live := "polite",
                s"Frame ${index + 1} / $count"
              )
            )
          )
      }
