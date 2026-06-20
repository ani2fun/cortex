package cortex.client.components.book

import cortex.client.components.book.workbench.CoachTab
import cortex.client.components.icons.LucideIcons
import cortex.shared.tutor.TutorContract.{SessionOrigin, Track}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The **conceptual coach** — the four-step understanding check (explain → apply → analyze → defend) mounted
 * inline at the end of a prose lesson (System Design and the other conceptual books). It reuses the live
 * [[CoachTab]] / `CoachController` exactly as [[StandaloneCoachBlock]] does, but pins the session to the
 * CONCEPTUAL track, so the steps, rubrics and prompts are the *understanding* ladder rather than the six-step
 * coding interview — there is no code to write here.
 *
 * It borrows the `standalone-coach__*` framing (self-contained card with its own scroll region) and adds a
 * `concept-coach` hook class for any future divergence. `coachProblemId` is the tutor join key: an author
 * override (`data-coach-problem-id`) or, by default, the chapter's `<book>/<chapter-slug>`.
 */
object ConceptCoachBlock:

  final case class Props(coachProblemId: Option[String])

  val Component =
    ScalaFnComponent[Props] { props =>
      <.div(
        ^.className := "concept-coach standalone-coach not-prose",
        <.div(
          ^.className := "standalone-coach__intro",
          LucideIcons.Target(LucideIcons.withClass("standalone-coach__intro-icon")),
          <.span(
            ^.className := "standalone-coach__intro-text",
            "Check your understanding with the coach — four short steps: explain the idea, apply it, " +
              "weigh the trade-offs, then defend your reasoning. Sign in to start."
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
                track = Track.Conceptual,
                active = true
              )
            )
          )
        )
      )
    }
