package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * K8s-reconcile widget — visualises Kubernetes' control loop. A Deployment declares a *desired* replica
 * count; the controller continuously works to make the *actual* set of pods match. A [[Stepper]] walks a
 * scripted sequence of events (deploy → schedule → ready → a pod crashes → reconcile → ready again) so the
 * reader watches self-healing happen: when actual drifts below desired, a "reconciling" banner appears and
 * the controller creates a replacement.
 *
 * Pure HTML, React-owned. Each step carries its own full pod list, so rendering is a direct read of the
 * current step (no replay). State = the stepper index.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Kubernetes self-healing",
 *   "steps": [
 *     { "event": "deploy",    "desired": 3, "note": "…",
 *       "pods": [ { "name": "cortex-a7", "status": "Pending" }, … ] },
 *     { "event": "ready",     "desired": 3, "note": "…",
 *       "pods": [ { "name": "cortex-a7", "status": "Ready" }, … ] },
 *     { "event": "crash",     "desired": 3, "note": "cortex-b3 OOMKilled",
 *       "pods": [ { "name": "cortex-b3", "status": "Crashed" }, … ] }
 *   ]
 * }
 * }}}
 *
 *   - pod `status` is one of `Pending | Running | Ready | Terminating | Crashed` (case-insensitive; unknown →
 *     Pending). "Actual ready" counts pods in `Ready`/`Running`.
 */
object K8sReconcile:

  enum PodStatus:
    case Pending, Running, Ready, Terminating, Crashed

  final case class Pod(name: String, status: PodStatus)
  final case class Step(event: String, desired: Int, pods: List[Pod], note: String)
  final case class Spec(title: Option[String], steps: List[Step])
  final case class Props(payload: String)

  private val StepDelayMs = 1700

  private def parseStatus(s: String): PodStatus = s.toLowerCase match
    case "running"     => PodStatus.Running
    case "ready"       => PodStatus.Ready
    case "terminating" => PodStatus.Terminating
    case "crashed"     => PodStatus.Crashed
    case _             => PodStatus.Pending

  private def parsePod(d: js.Dynamic): Pod =
    Pod(name = d.string("name").trim, status = parseStatus(d.string("status")))

  private def parseStep(d: js.Dynamic): Step =
    Step(
      event = d.string("event").trim,
      desired = d.int("desired", 1),
      pods = d.dynList("pods").map(parsePod).filter(_.name.nonEmpty),
      note = d.string("note").trim
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val steps = d.dynList("steps").map(parseStep).filter(_.event.nonEmpty)
      if steps.isEmpty then throw PayloadDecoder.invalid("steps must be non-empty")
      Spec(d.optString("title"), steps)
    }

  private def statusClass(s: PodStatus): String = s.toString.toLowerCase

  private def readyCount(pods: List[Pod]): Int =
    pods.count(p => p.status == PodStatus.Ready || p.status == PodStatus.Running)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .customBy { (_, specM) =>
        val stepCount = specM.value.toOption.fold(0)(_.steps.size)
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs.toDouble))
      }
      .render { (_, specM, stepper) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "K8s-reconcile payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val cursor = stepper.index
            val count  = spec.steps.size
            val step   = spec.steps(math.min(cursor, count - 1))
            val ready  = readyCount(step.pods)
            val synced = ready == step.desired

            val statusBar: VdomNode =
              <.div(
                ^.className := "k8s-reconcile__status",
                <.span(^.className := "k8s-reconcile__metric", s"desired: ${step.desired}"),
                <.span(^.className := "k8s-reconcile__metric", s"ready: $ready / ${step.desired}"),
                if synced then
                  <.span(
                    ^.className := "k8s-reconcile__banner k8s-reconcile__banner--ok",
                    "✓ converged — actual matches desired"
                  )
                else
                  <.span(
                    ^.className := "k8s-reconcile__banner k8s-reconcile__banner--recon",
                    "⟳ reconciling — controller is creating/replacing pods"
                  )
              )

            val pods: VdomNode =
              <.div(
                ^.className := "k8s-reconcile__pods",
                if step.pods.isEmpty then <.p(^.className := "k8s-reconcile__empty", "(no pods)")
                else
                  step.pods.toVdomArray { p =>
                    <.div(
                      ^.key       := p.name,
                      ^.className := s"k8s-reconcile__pod k8s-reconcile__pod--${statusClass(p.status)}",
                      <.span(^.className := "k8s-reconcile__pod-name", p.name),
                      <.span(^.className := "k8s-reconcile__pod-status", p.status.toString)
                    )
                  }
              )

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "k8s-reconcile__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Previous",
                    ^.className  := "k8s-reconcile__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("k8s-reconcile__button-icon")),
                    "Prev"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "k8s-reconcile__button k8s-reconcile__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("k8s-reconcile__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("k8s-reconcile__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Next",
                    ^.className  := "k8s-reconcile__button",
                    "Next",
                    LucideIcons.ArrowRight(LucideIcons.withClass("k8s-reconcile__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "k8s-reconcile__button k8s-reconcile__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("k8s-reconcile__button-icon"))
                  ),
                  <.span(
                    ^.className := "k8s-reconcile__progress",
                    s"Step ${cursor + 1} / ${math.max(1, count)}"
                  )
                )

            <.div(
              ^.className := "k8s-reconcile not-prose",
              spec.title
                .map(t => <.p(^.className := "k8s-reconcile__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              statusBar,
              pods,
              <.p(
                ^.className := "k8s-reconcile__caption",
                ^.aria.live := "polite",
                if step.note.nonEmpty then s"${step.event}: ${step.note}" else step.event
              ),
              controls
            )
      }
