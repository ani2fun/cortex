package cortex.client.components.book

import cortex.client.components.book.workbench.CoachTab
import cortex.client.components.icons.LucideIcons
import cortex.shared.tutor.TutorContract.SessionOrigin
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The **standalone coach** — the six-step Socratic interview mounted inline in a theory lesson (System Design
 * and the other prose books) where there is no code editor. It reuses the live [[CoachTab]] /
 * `CoachController` exactly as the problem-page Coach tab does; the only differences are the framing — a
 * self-contained card with its own scroll region, so the coach's sticky steps-header pins inside the block
 * rather than the page — and that the implement/test steps lean on the composer instead of a right-pane
 * editor (the coach already takes the answer as pasted/typed text, so nothing else is needed).
 *
 * `coachProblemId` is the tutor join key: an author override (`data-coach-problem-id`) or, by default, the
 * chapter's `<book>/<chapter-slug>`. `None` degrades to the static manual prompts, same as the Coach tab.
 */
object StandaloneCoachBlock:

  final case class Props(coachProblemId: Option[String])

  val Component =
    ScalaFnComponent[Props] { props =>
      <.div(
        ^.className := "standalone-coach not-prose",
        <.div(
          ^.className := "standalone-coach__intro",
          LucideIcons.Target(LucideIcons.withClass("standalone-coach__intro-icon")),
          <.span(
            ^.className := "standalone-coach__intro-text",
            "Work through this with the coach — six Socratic steps, from restating the idea to defending " +
              "your approach. Sign in to start."
          )
        ),
        <.div(
          ^.className := "standalone-coach__frame",
          <.div(
            ^.className := "standalone-coach__scroll",
            CoachTab.Component(
              CoachTab.Props(
                coachProblemId = props.coachProblemId,
                origin = SessionOrigin.YourTurn,
                active = true
              )
            )
          )
        )
      )
    }
