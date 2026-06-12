package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The Description tab's "Now your turn!" answer-pick quiz: show an input, pick one of the options, Validate →
 * Correct / Wrong-try-again. Pure client-side state, single-shot, no persistence — picking a different option
 * resets the verdict (the design's behaviour).
 */
object QuizBlock:

  final case class Props(quiz: Block.Quiz)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(Option.empty[String])  // picked option
      .useState(Option.empty[Boolean]) // verdict: None until Validate is pressed
      .render { (props, pickedS, verdictS) =>
        val q       = props.quiz
        val picked  = pickedS.value
        val verdict = verdictS.value

        val status: VdomNode = verdict match
          case Some(true) =>
            <.span(^.className := "quiz__pill quiz__pill--ok", "Correct")
          case Some(false) =>
            <.span(^.className := "quiz__pill quiz__pill--bad", "Wrong — try again")
          case None =>
            picked match
              case Some(p) =>
                <.button(
                  ^.tpe       := "button",
                  ^.className := "quiz__pill quiz__pill--validate",
                  ^.onClick --> verdictS.setState(Some(p == q.answer)),
                  "Validate"
                )
              case None =>
                <.span(^.className := "quiz__pill quiz__pill--prompt", "Pick your answer")

        <.div(
          ^.className := "quiz",
          <.p(^.className := "quiz__title", q.prompt.getOrElse("Now your turn!")),
          <.div(
            ^.className := "quiz__card",
            <.div(
              ^.className := "quiz__io",
              <.span(^.className := "quiz__io-label", "Input: "),
              q.input
            ),
            <.div(
              ^.className := "quiz__io quiz__io--out",
              <.span(^.className := "quiz__io-label", "Output:"),
              status
            ),
            <.div(
              ^.className := "quiz__options",
              q.options.zipWithIndex.toVdomArray { case (o, i) =>
                val on  = picked.contains(o)
                val ok  = on && verdict.contains(true)
                val bad = on && verdict.contains(false)
                val cls =
                  "quiz__opt"
                    + (if ok then " quiz__opt--ok" else "")
                    + (if bad then " quiz__opt--bad" else "")
                    + (if on && verdict.isEmpty then " quiz__opt--on" else "")
                <.button(
                  ^.key       := s"opt-$i",
                  ^.tpe       := "button",
                  ^.className := cls,
                  ^.onClick --> (pickedS.setState(Some(o)) >> verdictS.setState(None)),
                  if ok then
                    <.span(
                      ^.className := "quiz__radio quiz__radio--ok",
                      LucideIcons.Check(LucideIcons.withClass("quiz__radio-icon"))
                    )
                  else if bad then
                    <.span(
                      ^.className := "quiz__radio quiz__radio--bad",
                      LucideIcons.X(LucideIcons.withClass("quiz__radio-icon"))
                    )
                  else
                    <.span(
                      ^.className := (if on then "quiz__radio quiz__radio--on" else "quiz__radio"),
                      if on then <.span(^.className := "quiz__radio-dot") else EmptyVdom
                    )
                  ,
                  <.span(^.className := "quiz__opt-label", o)
                )
              }
            )
          )
        )
      }
