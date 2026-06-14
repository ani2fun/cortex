package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * ZIO-effect-stepper widget — visualises a `ZIO[R, E, A]` value as a runnable recipe. The program is a list
 * of operations; a [[Stepper]] "runs" them one at a time, and a state panel shows ZIO's three channels
 * evolving: the **R** environment (which services have been pulled in), the **A** success value, and the
 * **E** error channel. A `fail` step derails execution onto the error track — subsequent steps are *skipped*
 * (ZIO short-circuits) until a `recover` step catches it. This makes the "railway" model of typed errors
 * concrete.
 *
 * Pure HTML, React-owned. State = the stepper index.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Running a request handler",
 *   "env":   ["DataSource", "RateLimiter"],
 *   "steps": [
 *     { "op": "ZIO.service[DataSource]", "kind": "access",    "service": "DataSource", "yields": "ds",   "note": "…" },
 *     { "op": "attemptBlocking(ds.query)", "kind": "effect",   "yields": "rows",  "note": "…" },
 *     { "op": "ZIO.fail(NotFound)",        "kind": "fail",     "error": "NotFound", "note": "…" },
 *     { "op": "catchAll(_ => default)",    "kind": "recover",  "yields": "default", "note": "…" },
 *     { "op": "map(render)",               "kind": "transform","yields": "html",  "note": "…" }
 *   ]
 * }
 * }}}
 *
 *   - `kind` is one of `access | effect | transform | fail | recover`. `access` pulls `service` out of R (and
 *     yields a value); `fail` sets the error channel; `recover` catches a failure (only meaningful on the
 *     error track) and yields a fallback. Unknown kinds fall back to `effect`.
 *   - `yields` labels the success value (A) after the step; `error` is the failure value for `fail`.
 */
object ZioEffectStepper:

  enum StepKind:
    case Access, Effect, Transform, Fail, Recover

  final case class Step(
      op: String,
      kind: StepKind,
      service: Option[String],
      yields: Option[String],
      error: Option[String],
      note: String
  )

  final case class Spec(title: Option[String], env: List[String], steps: List[Step])
  final case class Props(payload: String)

  private val StepDelayMs = 1500

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parseKind(s: String): StepKind = s.toLowerCase match
    case "access"    => StepKind.Access
    case "transform" => StepKind.Transform
    case "fail"      => StepKind.Fail
    case "recover"   => StepKind.Recover
    case _           => StepKind.Effect

  private def parseStep(d: js.Dynamic): Step =
    Step(
      op = d.string("op").trim,
      kind = parseKind(d.string("kind")),
      service = d.optString("service"),
      yields = d.optString("yields"),
      error = d.optString("error"),
      note = d.string("note").trim
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val steps = d.dynList("steps").map(parseStep).filter(_.op.nonEmpty)
      if steps.isEmpty then throw PayloadDecoder.invalid("steps must be non-empty")
      Spec(d.optString("title"), d.stringList("env").getOrElse(Nil), steps)
    }

  // ===========================================================================
  // Replay — fold the steps up to the cursor to derive the channel state.
  // ===========================================================================

  final private case class RunState(
      value: String,
      failed: Option[String],
      consumed: Set[String],
      classes: Vector[
        String
      ] // per step index: past | current | current-fail | current-recover | recovered | skipped | future
  )

  private def replay(spec: Spec, cursor: Int): RunState =
    var value                  = "()"
    var failed: Option[String] = None
    var consumed               = Set.empty[String]
    val cls                    = Array.fill(spec.steps.size)("future")
    var i                      = 0
    while i <= cursor && i < spec.steps.size do
      val st = spec.steps(i)
      if failed.isDefined then
        if st.kind == StepKind.Recover then
          failed = None
          value = st.yields.getOrElse("recovered")
          cls(i) = if i == cursor then "current-recover" else "recovered"
        else cls(i) = "skipped"
      else
        st.kind match
          case StepKind.Access =>
            st.service.foreach(s => consumed = consumed + s)
            value = st.yields.getOrElse(value)
          case StepKind.Effect | StepKind.Transform =>
            value = st.yields.getOrElse(value)
          case StepKind.Fail =>
            failed = Some(st.error.getOrElse("error"))
          case StepKind.Recover => () // no-op on the success track
        cls(i) =
          if i == cursor then if failed.isDefined then "current-fail" else "current"
          else "past"
      i += 1
    RunState(value, failed, consumed, cls.toVector)

  private def kindLabel(k: StepKind): String = k match
    case StepKind.Access    => "R"
    case StepKind.Effect    => "IO"
    case StepKind.Transform => "map"
    case StepKind.Fail      => "fail"
    case StepKind.Recover   => "catch"

  // ===========================================================================
  // Component
  // ===========================================================================

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
              <.p(^.className   := "d3-widget__error-title", "ZIO-effect-stepper payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val cursor = stepper.index
            val state  = replay(spec, cursor)
            val count  = spec.steps.size

            val program: VdomNode =
              <.ol(
                ^.className := "zio-effect__program",
                spec.steps.zipWithIndex.toVdomArray { case (st, i) =>
                  <.li(
                    ^.key       := i.toString,
                    ^.className := s"zio-effect__step zio-effect__step--${state.classes(i)}",
                    <.span(
                      ^.className := s"zio-effect__kind zio-effect__kind--${st.kind.toString.toLowerCase}",
                      kindLabel(st.kind)
                    ),
                    <.code(^.className := "zio-effect__op", st.op)
                  )
                }
              )

            val envPanel: VdomNode =
              if spec.env.isEmpty then EmptyVdom
              else
                <.div(
                  ^.className := "zio-effect__channel",
                  <.p(^.className := "zio-effect__channel-label", "R — environment"),
                  <.div(
                    ^.className := "zio-effect__env",
                    spec.env.toVdomArray { svc =>
                      val on = state.consumed.contains(svc)
                      <.span(
                        ^.key       := svc,
                        ^.className := s"zio-effect__svc${if on then " zio-effect__svc--on" else ""}",
                        (if on then "✓ " else "") + svc
                      )
                    }
                  )
                )

            val channelBadge: VdomNode =
              state.failed match
                case Some(e) =>
                  <.div(
                    ^.className := "zio-effect__channel",
                    <.p(^.className    := "zio-effect__channel-label", "E — error channel"),
                    <.span(^.className := "zio-effect__value zio-effect__value--fail", s"failed: $e")
                  )
                case None =>
                  <.div(
                    ^.className := "zio-effect__channel",
                    <.p(^.className    := "zio-effect__channel-label", "A — success value"),
                    <.span(^.className := "zio-effect__value zio-effect__value--ok", state.value)
                  )

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "zio-effect__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Previous",
                    ^.className  := "zio-effect__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("zio-effect__button-icon")),
                    "Prev"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "zio-effect__button zio-effect__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("zio-effect__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("zio-effect__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Next",
                    ^.className  := "zio-effect__button",
                    "Next",
                    LucideIcons.ArrowRight(LucideIcons.withClass("zio-effect__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "zio-effect__button zio-effect__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("zio-effect__button-icon"))
                  ),
                  <.span(^.className := "zio-effect__progress", s"Step ${cursor + 1} / ${math.max(1, count)}")
                )

            <.div(
              ^.className := "zio-effect not-prose",
              spec.title
                .map(t => <.p(^.className := "zio-effect__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "zio-effect__body",
                program,
                <.div(^.className := "zio-effect__panel", envPanel, channelBadge)
              ),
              <.p(
                ^.className := "zio-effect__caption",
                ^.aria.live := "polite",
                spec.steps.lift(cursor).map(_.note).filter(_.nonEmpty).getOrElse(" ")
              ),
              controls
            )
      }
