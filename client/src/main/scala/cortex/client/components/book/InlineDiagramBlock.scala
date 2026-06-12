package cortex.client.components.book

import cortex.client.components.book.widgets.Stepper
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success, Try}

/**
 * Inline D3 diagram renderer for `d3 widget=<layoutKind>` fences whose layout name is in
 * [[D3WidgetBlock.KnownLayouts]] (Phase C / D / E of ADR-0023; see also slices 10–21).
 *
 * The component wraps the raw fence payload (a `VizGraph` `{steps:[...]}` object) into the `VizCases`
 * structure that `renderWidget` in `@d3/index` expects, injects `layoutHint = widget` so the TS renderer
 * selects the right layout, then mounts the D3 diagram into a host div. `renderWidget` draws a STATIC diagram
 * and returns a controller; [[cortex.client.components.book.widgets.Stepper]] owns the play loop and pushes
 * each step index into it, with a compact control bar (reset / prev / play / next / "k / n") shown only for
 * multi-step payloads.
 *
 * **Error guards (Slice 22 / ADR-0023).** Malformed JSON, payloads that don't match the layout's schema, and
 * exceptions thrown inside `renderWidget` all surface as an inline `<.d3-widget__error>` block (uniform with
 * [[D3WidgetBlock]]'s unknown-widget fallback) instead of crashing the whole chapter render.
 */
object InlineDiagramBlock:

  // ─── JS interop ─────────────────────────────────────────────────────────────
  // Duplicate of the @JSImport in VisualiseModal — both resolve to the same
  // bundled symbol at runtime; extracting a shared D3Renderer.scala is deferred
  // to a later cleanup slice.

  @js.native
  trait WidgetController extends js.Object:
    def setStep(n: Int, animate: Boolean): Unit = js.native
    def setHover(name: String): Unit            = js.native
    def getStepCount(): Int                     = js.native
    def destroy(): Unit                         = js.native

  // Returns `WidgetController | null` in TypeScript; `js.UndefOr` treats null as not-defined.
  @js.native @JSImport("@d3/index", "renderWidget")
  private def renderWidget(
      containerId: String,
      jsonStr: String,
      onStep: js.UndefOr[js.Function1[Int, Unit]],
      caseIndex: js.UndefOr[Int],
      onHover: js.UndefOr[js.Function1[String, Unit]]
  ): js.UndefOr[WidgetController] = js.native

  // ─── Host-id generator ──────────────────────────────────────────────────────
  private var hostSeq: Int = 0

  private def nextHostId(): String =
    hostSeq += 1
    s"inline-diagram-host-$hostSeq"

  // ─── Props ──────────────────────────────────────────────────────────────────
  final case class Props(widget: String, payload: String)

  // ─── Payload normalisation ──────────────────────────────────────────────────
  // `renderWidget` in `index.ts` expects a `VizCases` structure `{cases: [VizGraph]}`.
  // Inline fence payloads are raw `VizGraph` objects `{steps: [...]}` without a `cases`
  // wrapper. This helper:
  //   1. Detects raw `VizGraph` by the absence of a `cases` field.
  //   2. Injects `layoutHint = widget` so the single-layout fallback path in `renderWidget`
  //      selects the correct TS layout function (array-1d → arrayLayout, etc.).
  //   3. Normalises every step's `nodes` field to `[]` when missing — `hasLayoutKindAnnotations`
  //      in `index.ts` iterates `step.nodes` and throws on `undefined`.
  //   4. Wraps the graph in `{cases: [graph]}`.
  //
  // If the payload already contains `cases`, it is passed through unchanged (authors may
  // directly embed a full VizCases blob if they need multi-case diagrams).
  //
  // Returns `Left(errorMessage)` on parse / shape failure so the caller can show the inline
  // error placeholder instead of forwarding broken JSON to the TS renderer.
  private def wrapPayload(widget: String, payloadStr: String): Either[String, String] =
    Try {
      val raw = js.JSON.parse(payloadStr).asInstanceOf[js.Dynamic]
      if !js.isUndefined(raw.cases) then payloadStr
      else
        raw.updateDynamic("layoutHint")(widget)
        val stepsAny = raw.steps
        if !js.isUndefined(stepsAny) then
          val steps = stepsAny.asInstanceOf[js.Array[js.Dynamic]]
          var i     = 0
          while i < steps.length do
            val step = steps(i)
            if js.isUndefined(step.nodes) then
              step.updateDynamic("nodes")(js.Array[js.Dynamic]())
            i += 1
        js.JSON.stringify(js.Dynamic.literal(cases = js.Array(raw)))
    }.toEither.left.map(e => Option(e.getMessage).getOrElse(e.toString))

  // Per-step dwell while playing. Matches the modal's pacing closely enough that an inline diagram
  // and its full-screen Visualise counterpart feel like the same animation.
  private val StepDelayMs: Double = 1100.0

  // ─── Component ──────────────────────────────────────────────────────────────
  val Component = ScalaFnComponent
    .withHooks[Props]
    .useRefBy(_ => nextHostId())                   // hostIdRef     (1)
    .useRefBy(_ => Option.empty[WidgetController]) // controllerRef (2)
    .useState(Option.empty[String])                // errorS        (3)
    // Step count is only known once renderWidget has parsed the payload; 0 until mounted, which keeps
    // the control bar hidden (Stepper treats ≤ 1 as "nothing to scrub").
    .useState(0) // stepCountS    (4)
    // renderWidget draws a STATIC diagram and hands back a controller — the play loop is the caller's
    // (see @d3/index.ts). Stepper.hook owns the index + play/pause machinery; Effect (7) pushes each
    // index change into the controller. Without this, every multi-step inline diagram froze on step 0.
    .customBy((_, _, _, _, stepCountS) =>
      Stepper.hook(Stepper.Input(stepCountS.value, StepDelayMs))
    ) // stepper (5)
    .useEffectOnMountBy { (props, hostIdRef, controllerRef, errorS, stepCountS, _) =>
      // Mount the D3 renderer into the host div once on component mount. Failures land in
      // `errorS` so the next render swaps the host div for the inline error placeholder.
      Callback {
        wrapPayload(props.widget, props.payload) match
          case Left(msg) =>
            errorS.setState(Some(s"Could not parse payload JSON: $msg")).runNow()
          case Right(json) =>
            Try(
              renderWidget(hostIdRef.value, json, js.undefined, js.undefined, js.undefined).toOption
            ) match
              case Success(Some(ctrl)) =>
                controllerRef.value = Some(ctrl)
                stepCountS.setState(ctrl.getStepCount()).runNow()
              case Success(None) =>
                errorS
                  .setState(
                    Some(
                      "renderWidget could not draw this payload — open the browser console for the underlying cause."
                    )
                  )
                  .runNow()
              case Failure(err) =>
                val msg = Option(err.getMessage).getOrElse(err.toString)
                errorS.setState(Some(s"renderWidget threw: $msg")).runNow()
      }
    }
    // Effect (7): fan the Stepper's current index into the D3 controller (play / prev / next / reset).
    .useEffectWithDepsBy((_, _, _, _, _, stepper) => stepper.index) {
      (_, _, controllerRef, _, _, _) => idx =>
        Callback(controllerRef.value.foreach(_.setStep(idx, animate = true)))
    }
    .render { (props, hostIdRef, _, errorS, stepCountS, stepper) =>
      val canvasOrError: VdomNode = errorS.value match
        case Some(message) =>
          <.div(
            ^.className := "d3-widget__error",
            <.p(
              ^.className := "d3-widget__error-title",
              s"""Widget "${props.widget}" failed to render"""
            ),
            <.p(^.className := "d3-widget__error-message", message)
          )
        case None =>
          <.div(^.id := hostIdRef.value, ^.className := "inline-diagram__canvas")

      // Playback controls — only for multi-step diagrams that rendered cleanly. A single-step (or
      // errored) diagram renders exactly as before: canvas + Show JSON, no chrome.
      val controls: VdomNode =
        if errorS.value.isEmpty && stepCountS.value > 1 then
          val playIcon = if stepper.isPlaying then LucideIcons.Pause else LucideIcons.Play
          <.div(
            ^.className := "inline-diagram__controls",
            <.button(
              ^.tpe       := "button",
              ^.className := "inline-diagram__ctrl",
              ^.title     := "Reset to first step",
              ^.onClick --> stepper.reset,
              LucideIcons.RotateCcw(LucideIcons.withClass("inline-diagram__ctrl-icon"))
            ),
            <.button(
              ^.tpe       := "button",
              ^.className := "inline-diagram__ctrl",
              ^.title     := "Previous step",
              ^.disabled  := stepper.atStart,
              ^.onClick --> stepper.previous,
              LucideIcons.ChevronLeft(LucideIcons.withClass("inline-diagram__ctrl-icon"))
            ),
            <.button(
              ^.tpe       := "button",
              ^.className := "inline-diagram__ctrl inline-diagram__ctrl--play",
              ^.title     := (if stepper.isPlaying then "Pause" else "Play"),
              ^.onClick --> stepper.togglePlay,
              playIcon(LucideIcons.withClass("inline-diagram__ctrl-icon"))
            ),
            <.button(
              ^.tpe       := "button",
              ^.className := "inline-diagram__ctrl",
              ^.title     := "Next step",
              ^.disabled  := stepper.atEnd,
              ^.onClick --> stepper.next,
              LucideIcons.ChevronRight(LucideIcons.withClass("inline-diagram__ctrl-icon"))
            ),
            <.span(
              ^.className := "inline-diagram__step",
              s"${stepper.index + 1} / ${stepCountS.value}"
            )
          )
        else EmptyVdom

      <.div(
        ^.className := "inline-diagram",
        canvasOrError,
        controls
      )
    }
