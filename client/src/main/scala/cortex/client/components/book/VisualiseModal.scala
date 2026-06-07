package cortex.client.components.book

import cortex.client.api.ApiClient
import cortex.client.components.book.widgets.Stepper
import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.RunRequest
import cortex.shared.viz.VizGraph.given
import cortex.shared.viz.{HeapToGraph, VizCases, VizFrame, VizGraphStep, VizLocal}
import io.circe.syntax.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

/**
 * The AlgoLens 4-pane Visualise modal (ADR-0018, Slice 1). Opened from a runnable code block's "Visualise"
 * button; traces the *current editor content*, adapts the heap trace into a [[VizCases]] with `HeapToGraph`,
 * and hands the JSON to the standalone D3 renderer (`@d3/index`).
 *
 * Layout: TopBar / [Code | Canvas | Frames (placeholder)] / ControlsBar / OutputPanel — all inside an
 * `algolens-grid` CSS grid that fills `viz-modal__card`. Stepper.hook owns the play-loop; the D3 renderer
 * receives step changes via `WidgetController.setStep` (Effect C) — it has no internal play loop of its own.
 *
 * Keyboard: `Space` play/pause, `←`/`→` step, `R` restart, `F` final step, `Esc` close (or dismiss help),
 * `+`/`-`/`0` zoom.
 */
object VisualiseModal:

  // ─── JS interop ───────────────────────────────────────────────────────────────

  /**
   * Opaque controller returned by `renderWidget`. Scala drives the play loop via Stepper.hook; D3 just
   * follows `setStep` calls. Slice 5 adds `setHover` so the modal can push the shared hover name down to the
   * canvas's cursor-marks.
   */
  @js.native
  trait WidgetController extends js.Object:
    def setStep(n: Int, animate: Boolean): Unit = js.native
    def setHover(name: String): Unit            = js.native
    def getStepCount(): Int                     = js.native
    def destroy(): Unit                         = js.native

  // Returns `WidgetController | null` in TypeScript; js.UndefOr treats null as "not defined"
  // (isDefined / toOption both check `!= null && != undefined`).
  @js.native @JSImport("@d3/index", "renderWidget")
  private def renderWidget(
      containerId: String,
      jsonStr: String,
      onStep: js.UndefOr[js.Function1[Int, Unit]],
      caseIndex: js.UndefOr[Int],
      onHover: js.UndefOr[js.Function1[String, Unit]]
  ): js.UndefOr[WidgetController] = js.native

  /** Slice 4 — SVG overlay controller. The TS module owns the SVG; Scala just feeds endpoints. */
  @js.native
  trait ArrowLayerController extends js.Object:
    def setEndpoints(endpoints: js.Array[js.Object]): Unit = js.native
    def refresh(): Unit                                    = js.native
    def setHover(name: String): Unit                       = js.native
    def destroy(): Unit                                    = js.native

  @js.native @JSImport("@d3/arrow-layer", "mountArrowLayer")
  private def mountArrowLayer(host: dom.HTMLElement): ArrowLayerController = js.native

  @js.native @JSImport("react-dom", "createPortal")
  private def reactCreatePortal(
      child: japgolly.scalajs.react.facade.React.Node,
      container: dom.Node
  ): japgolly.scalajs.react.facade.React.Node = js.native

  private def portal(content: VdomElement): VdomNode =
    VdomNode(reactCreatePortal(content.rawElement, dom.document.body))

  // ─── Props + Phase ────────────────────────────────────────────────────────────

  /**
   * @param isOpen
   *   whether the modal is shown — owned by the parent code block
   * @param onClose
   *   flips the parent's open state back to closed
   * @param language
   *   the source language id (`"python"`, `"java"`, …) — picks the matching tracer harness
   * @param source
   *   the live code-editor content to trace
   * @param vizHint
   *   the fence's `viz=` value — the layout hint (e.g. `binary-tree`)
   * @param vizRoot
   *   the optional `viz-root=` variable naming the structure root
   * @param vizCase
   *   the optional `viz-case=` override forcing the number of test cases
   * @param vizKind
   *   the optional `viz-kind=` attribute (ADR-0024) — names the bespoke renderer that should wrap this
   *   diagram (`stack`, `queue`, `heap`, …). Overrides [[HeapToGraph]]'s segment-wide structure-type
   *   inference; `None` falls back to inference (and inference's `None` falls back to the generic renderer).
   * @param title
   *   shown in the TopBar title strip and on the diagram
   */
  final case class Props(
      isOpen: Boolean,
      onClose: Callback,
      language: String,
      source: String,
      vizHint: String,
      vizRoot: Option[String],
      vizCase: Option[Int],
      vizKind: Option[String],
      title: String
  )

  private var hostSeq: Int = 0

  private def nextHostId(): String =
    hostSeq += 1
    s"viz-modal-host-$hostSeq"

  // Slice 4 — derive the ArrowLayer overlay host's element id from the renderer
  // host id, so a single nextHostId() bump keeps them in lockstep.
  private def arrowsHostId(base: String): String = s"$base-arrows"

  /**
   * Slice 4 — build the per-step list of arrow endpoints fed to the ArrowLayer.
   *
   * Reads `step.cardCursor` (Slice 4 field on [[VizGraphStep]]). [[HeapToGraph]] already populated it from
   * the active-frame Refs whose target is a card root (Arr, Dict, or Instance-group representative), with
   * `target` set to the cardId directly. So this is a one-to-one mapping; no node-id resolution needed and no
   * per-cell index cursors leak through.
   */
  private def endpointsForCurrentStep(
      phase: Phase,
      caseIdx: Int,
      stepIdx: Int
  ): js.Array[js.Object] =
    val out = js.Array[js.Object]()
    phase match
      case Ready(cases, _, _) =>
        cases.cases.lift(caseIdx).flatMap(_.steps.lift(stepIdx)).foreach { step =>
          step.cardCursor.foreach { c =>
            if c.target.nonEmpty then
              out.push(
                js.Dynamic
                  .literal(cursorName = c.name, cardId = c.target, color = c.color)
                  .asInstanceOf[js.Object]
              )
              ()
          }
        }
      case _ => ()
    out

  sealed private trait Phase
  private case object Idle                                                             extends Phase
  private case object Tracing                                                          extends Phase
  final private case class Failed(message: String)                                     extends Phase
  final private case class Ready(cases: VizCases, json: String, programStdout: String) extends Phase

  /** The source line the prev / current / next trace step runs (1-based line number + trimmed text). */
  final private case class LineContext(
      prev: Option[(Int, String)],
      current: Option[(Int, String)],
      next: Option[(Int, String)]
  )

  /**
   * Slice 6 — Translate a *filtered* Stepper index to the corresponding real step index.
   *
   * When diff mode is off, returns `filteredIdx` unchanged. When on, collects the indices of all
   * non-`unchanged` steps from the current case and looks up position `filteredIdx` in that list. Falls back
   * to `filteredIdx` itself (no-op) when the list is shorter than expected — should not happen in practice
   * because the Stepper's stepCount equals the filtered list size.
   */
  private def computeRealStep(phase: Phase, caseIdx: Int, diffMode: Boolean, filteredIdx: Int): Int =
    if !diffMode then filteredIdx
    else
      phase match
        case Ready(cases, _, _) =>
          cases.cases
            .lift(caseIdx)
            .flatMap(g => g.steps.zipWithIndex.filter(!_._1.unchanged).map(_._2).lift(filteredIdx))
            .getOrElse(filteredIdx)
        case _ => filteredIdx

  private val MinZoom     = 0.5
  private val MaxZoom     = 4.0
  private val ZoomStep    = 0.25
  private val StepDelayMs = 600.0

  private given ExecutionContext = JSExecutionContext.queue

  private val traceCache = scala.collection.mutable.Map
    .empty[
      (String, String, Option[String], String, Option[Int], Option[String], String, String),
      Ready
    ]

  /**
   * Pick the right tracer harness for the source language — Python source goes through `PythonTracer.wrap`,
   * JVM languages through `JvmTracer.wrap`. Both produce a self-contained runnable program that emits a
   * `[HEAP_TRACE_BEGIN]…[HEAP_TRACE_END]` marker pair on stdout, so the downstream `MarkedTrace.parse` is
   * shared.
   */
  private def wrapForTrace(language: String, source: String): String =
    language.toLowerCase match
      case "java" | "kotlin" | "scala" => JvmTracer.wrap(source)
      case _                           => PythonTracer.wrap(source)

  /**
   * Normalise the language id we send to `/api/run`. JvmTracer's harness is Java source regardless of the
   * user's language tag (Kotlin / Scala tabs still trace through the Java compiler in v1 — ADR-0021).
   */
  private def runLanguageFor(language: String): String =
    language.toLowerCase match
      case "java" | "kotlin" | "scala" => "java"
      case _                           => "python"

  /** Stable id of the stdin textarea — used by `reTrace` to read the live value. */
  private val StdinInputId = "algolens-stdin-input"

  /** Pull the current stdin value out of the DOM. `None` if the textarea isn't mounted or is empty. */
  private def readStdin(): Option[String] =
    Option(dom.document.getElementById(StdinInputId))
      .map(_.asInstanceOf[dom.html.TextArea].value)
      .filter(_.nonEmpty)

  private def runTrace(props: Props, setPhase: Phase => Unit, force: Boolean = false): Unit =
    val stdin = readStdin()
    val key = (
      props.language,
      props.source,
      props.vizRoot,
      props.vizHint,
      props.vizCase,
      props.vizKind,
      props.title,
      stdin.getOrElse("")
    )
    (if force then None else traceCache.get(key)) match
      case Some(ready) => setPhase(ready)
      case None =>
        setPhase(Tracing)
        val wrapped = wrapForTrace(props.language, props.source)
        ApiClient
          .runCode(RunRequest(language = runLanguageFor(props.language), source = wrapped, stdin = stdin))
          .onComplete {
            case Success(resp) =>
              val r      = resp.result
              val parsed = PythonTracer.parse(Option(r.stdout).getOrElse(""))
              HeapToGraph.adapt(
                parsed.trace,
                props.source,
                props.vizHint,
                props.vizRoot,
                props.vizCase,
                props.title,
                props.vizKind
              ) match
                case Right(vc) =>
                  val ready = Ready(vc, vc.asJson.noSpaces, parsed.programStdout)
                  traceCache.update(key, ready)
                  setPhase(ready)
                case Left(msg) =>
                  val runErr =
                    if parsed.trace.steps.isEmpty && r.statusId != 3 then
                      Seq(
                        Option(r.statusDescription).filter(_.nonEmpty),
                        Option(r.compileOutput).filter(_.nonEmpty).map(s => s"compile: $s"),
                        Option(r.stderr).filter(_.nonEmpty).map(s => s"stderr: $s")
                      ).flatten.mkString(" — ")
                    else ""
                  setPhase(Failed(if runErr.nonEmpty then runErr else msg))
            case Failure(t) =>
              setPhase(Failed(Option(t.getMessage).getOrElse("network error")))
          }

  // ─── Component ────────────────────────────────────────────────────────────────

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState[Phase](Idle)                             // phaseS       (1)
      .useState(0)                                       // caseS        (2)
      .useState(1.0)                                     // zoomS        (3)
      .useRefBy(_ => js.Array[js.Function0[Unit]]())     // cleanupRef   (4)
      .useRefBy(_ => nextHostId())                       // hostId       (5)
      .useState(false)                                   // helpOpenS    (6)
      .useRefBy(_ => false)                              // helpOpenRef  (7)
      .useState(false)                                   // outputCollapsedS (8)
      .useRefBy(_ => Option.empty[WidgetController])     // controllerRef (9)
      .useRefBy(_ => Option.empty[Stepper.Output])       // stepperRef   (10)
      .useRefBy(_ => Option.empty[ArrowLayerController]) // arrowRef    (11)
      .useState(Option.empty[String])                    // hoveredS    (12, Slice 5)
      .useState(false)                                   // diffModeS   (13, Slice 6)
      // Stepper.hook drives the play loop — stepCount derived from Ready phase.
      // In diff mode stepCount is the number of non-`unchanged` steps so the scrubber
      // and atEnd/atStart flags work on filtered positions.
      // Total params into customBy: props(0) + 13 hooks = 14.
      .customBy { (_, phaseS, caseS, _, _, _, _, _, _, _, _, _, _, diffModeS) =>
        val totalSteps = phaseS.value match
          case Ready(cases, _, _) => cases.cases.lift(caseS.value).fold(0)(_.steps.size)
          case _                  => 0
        val stepCount =
          if !diffModeS.value then totalSteps
          else
            phaseS.value match
              case Ready(cases, _, _) =>
                cases.cases.lift(caseS.value).fold(0)(g => g.steps.count(!_.unchanged))
              case _ => 0
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs))
      }                     // stepper      (14)
      .useState(false)      // timelineOpenS  (15, Slice 7)
      .useState(false)      // shortcutsOpenS (16, Slice 7)
      .useRefBy(_ => false) // timelineOpenRef  (17, Slice 7)
      .useRefBy(_ => false) // shortcutsOpenRef (18, Slice 7)
      .useState(false)      // copiedS          (19, Slice 8)
      .useState(false)      // isDarkS          (20, Slice 9)
      // ─── Effect A: open/close — scroll-lock + keyboard shortcuts ─────────────
      // Deps: isOpen. Keyboard handler is installed once per open and reads
      // stepperRef each event to avoid stale closures on the Stepper callbacks.
      // Also mounts the Slice 4 ArrowLayer when the modal opens; destroy is
      // pushed onto cleanupRef so the existing tearDown loop drops it on close.
      .useEffectWithDepsBy((props, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) =>
        props.isOpen
      ) {
        (
            props,
            phaseS,
            caseS,
            zoomS,
            cleanupRef,
            hostId,
            helpOpenS,
            helpOpenRef,
            _,
            controllerRef,
            stepperRef,
            arrowRef,
            hoveredS,
            diffModeS,
            _,
            timelineOpenS,
            shortcutsOpenS,
            timelineOpenRef,
            shortcutsOpenRef,
            _,      // copiedS — not needed in this effect
            isDarkS // Slice 9
        ) => isOpen =>
          val tearDown: Callback = Callback {
            val arr = cleanupRef.value
            for i <- 0 until arr.length do arr(i)()
            cleanupRef.value = js.Array()
          }

          val install: Callback =
            if !isOpen then
              // Slice 8 — clear the URL hash without adding a browser history entry.
              Callback {
                dom.window.history.replaceState(null, "", dom.window.location.pathname)
              } >>
                Callback {
                  controllerRef.value.foreach(_.destroy())
                  controllerRef.value = None
                } >>
                phaseS.setState(Idle) >> zoomS.setState(1.0) >>
                caseS.setState(0) >> helpOpenS.setState(false) >>
                hoveredS.setState(None) >> diffModeS.setState(false) >>
                timelineOpenS.setState(false) >> shortcutsOpenS.setState(false) >>
                Callback {
                  helpOpenRef.value = false
                  timelineOpenRef.value = false
                  shortcutsOpenRef.value = false
                }
            else
              Callback {
                // Capture the page scroll offset so closing the modal returns the reader
                // to the code block they launched it from — not the top of the article.
                // The page scrolls at the window level (document.scrollingElement is <html>),
                // so window.scrollY / scrollTo is the correct axis. Restored in the cleanup
                // thunk below (which runs on close via tearDown).
                val savedScrollY = dom.window.scrollY
                val prevOverflow = dom.document.body.style.overflow
                dom.document.body.style.overflow = "hidden"

                val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
                  e.key match
                    case "Escape" =>
                      e.preventDefault()
                      if helpOpenRef.value then
                        helpOpenRef.value = false
                        helpOpenS.setState(false).runNow()
                      else if shortcutsOpenRef.value then
                        shortcutsOpenRef.value = false
                        shortcutsOpenS.setState(false).runNow()
                      else if timelineOpenRef.value then
                        timelineOpenRef.value = false
                        timelineOpenS.setState(false).runNow()
                      else props.onClose.runNow()
                    case " " =>
                      e.preventDefault()
                      stepperRef.value.foreach(_.togglePlay.runNow())
                    case "ArrowLeft" =>
                      e.preventDefault()
                      stepperRef.value.foreach(_.previous.runNow())
                    case "ArrowRight" =>
                      e.preventDefault()
                      stepperRef.value.foreach(_.next.runNow())
                    case "r" | "R" =>
                      e.preventDefault()
                      stepperRef.value.foreach(_.reset.runNow())
                    case "f" | "F" =>
                      e.preventDefault()
                      stepperRef.value.foreach { s =>
                        // Jump to final step by jumping to a large index (Stepper clamps)
                        s.jumpTo(Int.MaxValue).runNow()
                      }
                    case "t" | "T" =>
                      e.preventDefault()
                      val next = !timelineOpenRef.value
                      timelineOpenRef.value = next
                      timelineOpenS.setState(next).runNow()
                    case "?" =>
                      e.preventDefault()
                      shortcutsOpenRef.value = true
                      shortcutsOpenS.setState(true).runNow()
                    case "+" | "=" =>
                      e.preventDefault()
                      zoomS.modState(z => math.min(MaxZoom, z + ZoomStep)).runNow()
                    case "-" =>
                      e.preventDefault()
                      zoomS.modState(z => math.max(MinZoom, z - ZoomStep)).runNow()
                    case "0" =>
                      e.preventDefault()
                      zoomS.setState(1.0).runNow()
                    case _ => ()

                dom.document.addEventListener("keydown", onKey)
                cleanupRef.value.push { () =>
                  dom.document.removeEventListener("keydown", onKey)
                  dom.document.body.style.overflow = prevOverflow
                  // Return to the launch position (see savedScrollY above). The page sets
                  // `scroll-behavior: smooth`, which the 2-arg scrollTo(x, y) form honours —
                  // that would animate the restore (and freezes outright in a backgrounded
                  // tab). Use the options form with behavior:"instant" to snap straight back.
                  // Runs after the overflow is restored so the document can scroll, and
                  // post-commit so the article is at full height again.
                  dom.window
                    .asInstanceOf[js.Dynamic]
                    .scrollTo(js.Dynamic.literal(top = savedScrollY, left = 0.0, behavior = "instant"))
                  ()
                }

                // Slice 4 — mount the ArrowLayer once per open. The host div is
                // rendered in JSX (sibling of the three panes); by the time this
                // effect fires post-commit, the div is in the DOM.
                val arrowHostEl = dom.document.getElementById(arrowsHostId(hostId.value))
                if arrowHostEl != null then
                  val ctrl = mountArrowLayer(arrowHostEl.asInstanceOf[dom.HTMLElement])
                  arrowRef.value = Some(ctrl)
                  val _ = cleanupRef.value.push { () =>
                    ctrl.destroy()
                    arrowRef.value = None
                    ()
                  }

                runTrace(props, p => phaseS.setState(p).runNow())
                // Slice 9 — snapshot the current theme so the in-modal button stays in sync.
                isDarkS.setState(dom.document.documentElement.classList.contains("dark")).runNow()
                ()
              }

          tearDown >> install
      }
      // ─── Effect B: (re-)mount D3 renderer when (json, caseIndex) changes ─────
      // A new trace OR a case-selector switch destroys the old controller and
      // calls renderWidget with the new JSON + case, storing the returned
      // WidgetController in controllerRef for Effects C to use. Slice 5 wires
      // the canvas→Scala hover callback in here — `onHover` translates an
      // empty string from the cursor-mark's mouseleave into `None`.
      .useEffectWithDepsBy { (_, phaseS, caseS, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) =>
        phaseS.value match
          case Ready(_, json, _) => (json, caseS.value)
          case _                 => ("", 0)
      } {
        (_, _, _, _, _, hostId, _, _, _, controllerRef, _, _, hoveredS, _, _, _, _, _, _, _, _) =>
          (json, caseIdx) =>
            Callback {
              controllerRef.value.foreach(_.destroy())
              controllerRef.value = None
              if json.nonEmpty then
                val onHover: js.Function1[String, Unit] = (name: String) =>
                  if name.isEmpty then hoveredS.setState(None).runNow()
                  else hoveredS.setState(Some(name)).runNow()
                controllerRef.value =
                  renderWidget(hostId.value, json, js.undefined, caseIdx, onHover).toOption
            }
      }
      // ─── Effect C: push Stepper step index into the D3 controller ─────────────
      // Runs whenever Stepper.hook advances the index (play, click, keyboard).
      // In diff mode `stepper.index` is a filtered position; translate to the
      // real step index before handing it to the D3 controller.
      .useEffectWithDepsBy((_, _, _, _, _, _, _, _, _, _, _, _, _, _, stepper, _, _, _, _, _, _) =>
        stepper.index
      ) {
        (_, phaseS, caseS, _, _, _, _, _, _, controllerRef, _, _, _, diffModeS, stepper, _, _, _, _, _, _) =>
          filteredIdx =>
            Callback {
              val realIdx = computeRealStep(phaseS.value, caseS.value, diffModeS.value, filteredIdx)
              controllerRef.value.foreach(_.setStep(realIdx, animate = true))
            }
      }
      // ─── Effect E: push arrow endpoints into the ArrowLayer ──────────────────
      // Slice 4. Deps: (phaseTag, caseIdx, stepIndex, zoomBp). Zoom is projected
      // to basis points (×100, Int) so we don't need a Reusability[Double]
      // import — zoom is a multiple of ZoomStep=0.25, so the projection is
      // lossless. On every change we recompute endpoints from the current
      // step's cursors + node→cardId map and call setEndpoints. Zoom-only
      // changes still recompute coords (cards scale visually;
      // getBoundingClientRect returns post-transform coords).
      .useEffectWithDepsBy {
        (_, phaseS, caseS, zoomS, _, _, _, _, _, _, _, _, _, diffModeS, stepper, _, _, _, _, _, _) =>
          val phaseTag = phaseS.value match
            case Ready(_, _, _) => 1
            case _              => 0
          val zoomBp = math.round(zoomS.value * 100).toInt
          (phaseTag, caseS.value, stepper.index, zoomBp)
      } {
        (_, phaseS, caseS, _, _, _, _, _, _, _, _, arrowRef, _, diffModeS, stepper, _, _, _, _, _, _) => _ =>
          Callback {
            val realIdx = computeRealStep(phaseS.value, caseS.value, diffModeS.value, stepper.index)
            val eps     = endpointsForCurrentStep(phaseS.value, caseS.value, realIdx)
            arrowRef.value.foreach(_.setEndpoints(eps))
          }
      }
      // ─── Effect F: push hover key into D3 controller + ArrowLayer (Slice 5) ──
      // Deps: hoveredS. The shared hover state (an ident name, a local name, or
      // a cursor name) is converted to the empty-string sentinel used across
      // the JS bridge; both controllers re-apply their `--hovered` classes.
      .useEffectWithDepsBy { (_, _, _, _, _, _, _, _, _, _, _, _, hoveredS, _, _, _, _, _, _, _, _) =>
        hoveredS.value
      } { (_, _, _, _, _, _, _, _, _, controllerRef, _, arrowRef, _, _, _, _, _, _, _, _, _) => hovered =>
        Callback {
          val arg = hovered.getOrElse("")
          controllerRef.value.foreach(_.setHover(arg))
          arrowRef.value.foreach(_.setHover(arg))
        }
      }
      // ─── Effect G: one-shot deep-link jump on Ready transition (Slice 8) ────────
      // Fires once when phaseTag transitions to 1. Reads the URL hash before
      // Effect H overwrites it and jumps to the encoded step + case. diffMode is
      // always off at modal-open time (reset in Effect A), so filteredIdx == realIdx
      // and we can pass the parsed step number directly to jumpTo.
      // Declaration order matters: this must stay before Effect H so React runs it
      // first in the same commit batch (both fire when phaseTag goes 0→1).
      .useEffectWithDepsBy {
        (_, phaseS, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) =>
          phaseS.value match
            case Ready(_, _, _) => 1
            case _              => 0
      } {
        (_, _, caseS, _, _, _, _, _, _, _, stepperRef, _, _, _, _, _, _, _, _, _, _) => phaseTag =>
          if phaseTag != 1 then Callback.empty
          else
            Callback {
              val rawHash = dom.window.location.hash.stripPrefix("#")
              if rawHash.nonEmpty then
                val params = rawHash
                  .split("&")
                  .flatMap { part =>
                    val kv = part.split("=", 2)
                    if kv.length == 2 then Some(kv(0) -> kv(1)) else None
                  }
                  .toMap
                // Apply case first; the step jump is relative to the case anyway.
                params.get("case").flatMap(_.toIntOption).foreach { caseNum =>
                  caseS.setState((caseNum - 1).max(0)).runNow()
                }
                params.get("step").flatMap(_.toIntOption).foreach { stepNum =>
                  // diffMode is off → filtered index == real index
                  val realIdx = (stepNum - 1).max(0)
                  stepperRef.value.foreach(_.jumpTo(realIdx).runNow())
                }
            }
      }
      // ─── Effect H: write current step to URL hash on every step / case change ───
      // Slice 8. Fires on every (phaseTag, filteredIdx, caseIdx) change while Ready.
      // Encodes the real step (1-based, unfiltered) so deep links survive diff-mode
      // toggle. Hash is cleared via history.replaceState in Effect A on close.
      .useEffectWithDepsBy {
        (_, phaseS, caseS, _, _, _, _, _, _, _, _, _, _, _, stepper, _, _, _, _, _, _) =>
          val phaseTag = phaseS.value match
            case Ready(_, _, _) => 1
            case _              => 0
          (phaseTag, stepper.index, caseS.value)
      } {
        (_, phaseS, caseS, _, _, _, _, _, _, _, _, _, _, diffModeS, stepper, _, _, _, _, _, _) =>
          (phaseTag, filteredIdx, caseIdx) =>
            Callback {
              if phaseTag == 1 then
                val realIdx = computeRealStep(phaseS.value, caseIdx, diffModeS.value, filteredIdx)
                val hash =
                  if caseIdx == 0 then s"step=${realIdx + 1}"
                  else s"step=${realIdx + 1}&case=${caseIdx + 1}"
                dom.window.location.hash = hash
            }
      }
      .render {
        (
            props,
            phaseS,
            caseS,
            zoomS,
            _,
            hostId,
            helpOpenS,
            helpOpenRef,
            outputCollapsedS,
            _,
            stepperRef,
            _,
            hoveredS,
            diffModeS,
            stepper,
            timelineOpenS,
            shortcutsOpenS,
            timelineOpenRef,
            shortcutsOpenRef,
            copiedS, // Slice 8
            isDarkS  // Slice 9
        ) =>
          // Sync latest Stepper.Output into the ref — the keyboard handler
          // (installed once in Effect A) reads this to avoid stale closures.
          stepperRef.value = Some(stepper)

          if !props.isOpen then EmptyVdom
          else
            val phase    = phaseS.value
            val caseIdx  = caseS.value
            val zoom     = zoomS.value
            val hovered  = hoveredS.value
            val diffMode = diffModeS.value

            val zoomIn: Callback    = zoomS.modState(z => math.min(MaxZoom, z + ZoomStep))
            val zoomOut: Callback   = zoomS.modState(z => math.max(MinZoom, z - ZoomStep))
            val zoomReset: Callback = zoomS.setState(1.0)

            // Slice 5 — hover handlers for ident tokens and locals rows. Each
            // mouseLeave clears unconditionally; React fires the leave on the
            // old element before the mouseEnter on the new one, so the common
            // path lands on the right name.
            val onHoverName: String => Callback = name => hoveredS.setState(Some(name))
            val onHoverLeave: Callback          = hoveredS.setState(None)

            // Slice 7 — timeline drawer and shortcuts overlay open/close helpers.
            // Each setter syncs both the state hook and the mutable ref so the
            // keyboard handler (installed once in Effect A) can read current state.
            val setTimelineOpen: Boolean => Callback = open =>
              Callback { timelineOpenRef.value = open } >> timelineOpenS.setState(open)
            val setShortcutsOpen: Boolean => Callback = open =>
              Callback { shortcutsOpenRef.value = open } >> shortcutsOpenS.setState(open)

            // Unfiltered steps list — the timeline always shows all steps, with
            // unchanged ones greyed out so the user can see the full execution.
            val currentSteps: List[VizGraphStep] = phase match
              case Ready(cases, _, _) => cases.cases.lift(caseIdx).fold(List.empty)(_.steps)
              case _                  => List.empty

            // Jump from a real step index (timeline row click) to its filtered
            // position. Closing the drawer on jump is intentional UX: user clicked
            // to arrive at a step — the drawer has done its job.
            val onTimelineJump: Int => Callback = realIdx =>
              stepper.jumpTo(realToFilteredStep(phase, caseIdx, diffMode, realIdx))

            val reTrace: Callback =
              caseS.setState(0) >>
                Callback(runTrace(props, p => phaseS.setState(p).runNow(), force = true))

            val selectCase: Int => Callback = i => caseS.setState(i)

            val setHelp: Boolean => Callback = open =>
              Callback { helpOpenRef.value = open } >> helpOpenS.setState(open)

            // Slice 8 — copy current URL to clipboard and flash "Copied!" for 1.6 s.
            val shareCallback: Callback = Callback {
              val _ = dom.window.navigator
                .asInstanceOf[js.Dynamic]
                .clipboard
                .writeText(dom.window.location.href)
              copiedS.setState(true).runNow()
              js.timers.setTimeout(1600)(copiedS.setState(false).runNow())
            }

            // Slice 9 — theme toggle: flips the site's `.dark` class on <html> and
            // syncs the in-modal button. The Header's Sun/Moon button modifies the
            // same class, so both stay in sync naturally via isDarkS.
            val isDark: Boolean = isDarkS.value

            val onThemeToggle: Callback = Callback {
              val html    = dom.document.documentElement
              val nowDark = html.classList.contains("dark")
              if nowDark then html.classList.remove("dark")
              else html.classList.add("dark")
              isDarkS.setState(!nowDark).runNow()
            }

            // Total step count (used for "of N" annotation in diff mode).
            val stepCount = phase match
              case Ready(cases, _, _) => cases.cases.lift(caseIdx).fold(0)(_.steps.size)
              case _                  => 0

            // Slice 6 — In diff mode the Stepper operates on filtered (non-`unchanged`)
            // steps; translate the visible index to the real step index before reading
            // into the step list. `computeRealStep` re-derives the mapping each render.
            val realStepIdx = computeRealStep(phase, caseIdx, diffMode, stepper.index)

            // Visible step count: filtered when diffMode on.
            val visibleStepCount =
              if diffMode then
                phase match
                  case Ready(cases, _, _) =>
                    cases.cases.lift(caseIdx).fold(0)(g => g.steps.count(!_.unchanged))
                  case _ => 0
              else stepCount

            val currentLine: Option[Int] = phase match
              case Ready(cases, _, _) =>
                cases.cases.lift(caseIdx).flatMap(_.steps.lift(realStepIdx)).map(_.line)
              case _ => None

            // The "next line to execute" — Python Tutor convention: distinguish what
            // *just ran* (this step) from what *will run next* (next step). Helps the
            // reader predict branching outcomes ahead of stepping. Empty when there's
            // no next step (last step of the run) or the next step lands on the same
            // line (no point underlining the same row in two colours).
            val nextLine: Option[Int] = phase match
              case Ready(cases, _, _) =>
                cases.cases
                  .lift(caseIdx)
                  .flatMap(_.steps.lift(realStepIdx + 1))
                  .map(_.line)
                  .filterNot(currentLine.contains)
              case _ => None

            val currentFrames: List[VizFrame] = phase match
              case Ready(cases, _, _) =>
                cases.cases.lift(caseIdx).flatMap(_.steps.lift(realStepIdx)).fold(List.empty)(_.frames)
              case _ => List.empty

            // Prev / current / next source lines for the canvas footer — the line each
            // adjacent trace step runs, paired with its trimmed source text.
            val lineContext: LineContext =
              val sourceLines = props.source.split("\n", -1)
              def lineAt(stepIdx: Int): Option[(Int, String)] =
                val ln: Option[Int] = phase match
                  case Ready(cases, _, _) =>
                    cases.cases.lift(caseIdx).flatMap(_.steps.lift(stepIdx)).map(_.line)
                  case _ => None
                ln.flatMap(n => sourceLines.lift(n - 1).map(t => (n, t.trim)))
              LineContext(lineAt(realStepIdx - 1), lineAt(realStepIdx), lineAt(realStepIdx + 1))

            val modalRoot: VdomElement =
              <.div(
                ^.role       := "dialog",
                ^.aria.modal := true,
                ^.aria.label := "Visualise code",
                ^.className  := "viz-modal not-prose",
                ^.onClick --> props.onClose,
                <.div(
                  ^.className := "viz-modal__viewport",
                  <.div(
                    ^.className := "viz-modal__card",
                    ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
                    ^.style := js.Dynamic
                      .literal(width = "95vw", height = "90vh")
                      .asInstanceOf[js.Object],
                    <.div(
                      ^.className := "algolens-grid",
                      // ── Row 1: TopBar ─────────────────────────────────────
                      renderTopBar(
                        props,
                        phase,
                        caseIdx,
                        zoom,
                        reTrace,
                        zoomIn,
                        zoomOut,
                        zoomReset,
                        helpOpenS.value,
                        setHelp,
                        selectCase,
                        copiedS.value,
                        shareCallback,
                        isDark,
                        onThemeToggle
                      ),
                      // ── Row 2: Three panes ────────────────────────────────
                      <.div(
                        ^.className := "algolens__panes",
                        MonacoSourcePane.Component(
                          MonacoSourcePane.Props(props.source, props.language, currentLine, nextLine)
                        ),
                        renderCanvasPane(phase, hostId.value, zoom, lineContext),
                        renderFramesPane(currentFrames, hovered, onHoverName, onHoverLeave),
                        // Slice 4 — overlay host for the ArrowLayer. SVG mounted
                        // by mountArrowLayer; absolute-positioned over the 3 panes.
                        <.div(
                          ^.id        := arrowsHostId(hostId.value),
                          ^.className := "algolens-arrows"
                        )
                      ),
                      // ── Row 3: Controls bar ───────────────────────────────
                      renderControlsBar(
                        stepper,
                        visibleStepCount,
                        currentLine
                      ),
                      // ── Row 4: Output panel ───────────────────────────────
                      renderOutputPanel(
                        outputCollapsedS.value,
                        outputCollapsedS.modState(!_),
                        phase match { case Ready(_, _, out) => out; case _ => "" }
                      )
                    )
                  )
                )
              )

            portal(
              <.div(
                // Slice 7 — timeline drawer + shortcuts overlay sit alongside
                // the modal root in the same portal container (appended to body).
                // Both are `position: fixed` so the wrapper div has no visual
                // footprint; it's just the required single-element portal root.
                renderTimelineDrawer(
                  open = timelineOpenS.value,
                  onClose = setTimelineOpen(false),
                  steps = currentSteps,
                  realStepIdx = realStepIdx,
                  onJump = onTimelineJump
                ),
                renderShortcutsOverlay(
                  open = shortcutsOpenS.value,
                  onClose = setShortcutsOpen(false)
                ),
                modalRoot
              )
            )
      }

  // ─── TopBar ───────────────────────────────────────────────────────────────────

  private def renderTopBar(
      props: Props,
      phase: Phase,
      caseIdx: Int,
      zoom: Double,
      reTrace: Callback,
      zoomIn: Callback,
      zoomOut: Callback,
      zoomReset: Callback,
      helpOpen: Boolean,
      setHelp: Boolean => Callback,
      selectCase: Int => Callback,
      copied: Boolean,
      onShare: Callback,
      isDark: Boolean,
      onThemeToggle: Callback
  ): VdomElement =
    <.div(
      ^.className := "algolens__topbar",
      // Brand: CORTEX · LAB / Visualise / AlgoLens
      <.div(
        ^.className := "algolens__topbar-brand",
        <.span(^.className := "algolens__topbar-eyebrow", "CORTEX · LAB"),
        <.span(
          ^.className := "algolens__topbar-mark",
          <.em("Visualise"),
          <.span(^.className := "algolens__topbar-divider", "/"),
          <.span(^.className := "algolens__topbar-product", "AlgoLens")
        )
      ),
      // What is being traced
      <.div(
        ^.className := "viz-modal__title",
        <.span(^.className := "viz-modal__title-eyebrow", "VISUALISING"),
        <.span(^.className := "viz-modal__title-name", props.title)
      ),
      // Case selector — only when the trace has multiple cases
      renderCaseStrip(phase, caseIdx, selectCase),
      // Action buttons
      <.div(
        ^.className := "algolens__topbar-actions",
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := "Re-trace",
          ^.title      := "Re-trace",
          ^.onClick --> reTrace,
          LucideIcons.RotateCcw(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := "Zoom out",
          ^.title      := "Zoom out (-)",
          ^.onClick --> zoomOut,
          LucideIcons.ZoomOut(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        <.span(
          ^.className := "algolens__topbar-zoom-readout",
          s"${math.round(zoom * 100).toInt}%"
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := "Zoom in",
          ^.title      := "Zoom in (+)",
          ^.onClick --> zoomIn,
          LucideIcons.ZoomIn(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := "Reset zoom",
          ^.title      := "Reset zoom (0)",
          ^.onClick --> zoomReset,
          LucideIcons.Maximize2(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        // Slice 9 — Theme toggle: flips the site-wide `.dark` class on <html>.
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := (if isDark then "Switch to light mode" else "Switch to dark mode"),
          ^.title      := (if isDark then "Light mode" else "Dark mode"),
          ^.onClick --> onThemeToggle,
          if isDark then LucideIcons.Sun(LucideIcons.withClass("algolens__topbar-btn-icon"))
          else LucideIcons.Moon(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        renderHelp(helpOpen, setHelp),
        // Slice 8 — Share button: copies current URL (with #step=N) to clipboard.
        <.button(
          ^.tpe := "button",
          ^.className :=
            (if copied then "algolens__topbar-btn algolens__topbar-btn--copied"
             else "algolens__topbar-btn"),
          ^.aria.label := "Share — copy step link",
          ^.title      := "Share (copy step link)",
          ^.onClick --> onShare,
          if copied then <.span("Copied!")
          else LucideIcons.Share2(LucideIcons.withClass("algolens__topbar-btn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens__topbar-btn",
          ^.aria.label := "Close",
          ^.title      := "Close (Esc)",
          ^.onClick --> props.onClose,
          LucideIcons.X(LucideIcons.withClass("algolens__topbar-btn-icon"))
        )
      )
    )

  // ─── Case selector strip ──────────────────────────────────────────────────────

  private def renderCaseStrip(phase: Phase, caseIdx: Int, selectCase: Int => Callback): VdomNode =
    phase match
      case Ready(cases, _, _) if cases.cases.sizeIs > 1 =>
        <.nav(
          ^.className  := "algolens__topbar-cases",
          ^.aria.label := "Test case",
          ^.role       := "tablist",
          <.span(^.className := "algolens__topbar-cases-label", "CASE"),
          cases.cases.zipWithIndex.toVdomArray { case (_, i) =>
            <.button(
              ^.key        := i.toString,
              ^.tpe        := "button",
              ^.role       := "tab",
              ^.aria.label := s"Test case ${i + 1}",
              ^.className :=
                (if i == caseIdx then "algolens__topbar-case-btn algolens__topbar-case-btn--active"
                 else "algolens__topbar-case-btn"),
              ^.onClick --> selectCase(i),
              (i + 1).toString
            )
          }
        )
      case _ => EmptyVdom

  // ─── Help (i) button + popover ────────────────────────────────────────────────

  private def renderHelp(open: Boolean, setHelp: Boolean => Callback): VdomElement =
    <.div(
      ^.className := "viz-modal__help",
      <.button(
        ^.tpe                     := "button",
        ^.className               := "algolens__topbar-btn",
        ^.aria.label              := "How Visualise works",
        ^.title                   := "How it works",
        VdomAttr("aria-expanded") := (if open then "true" else "false"),
        VdomAttr("aria-haspopup") := "dialog",
        ^.onClick --> setHelp(!open),
        LucideIcons.Info(LucideIcons.withClass("algolens__topbar-btn-icon"))
      ),
      if open then
        TagMod(
          <.div(^.className := "viz-modal__help-backdrop", ^.onClick --> setHelp(false)),
          renderHelpPopover(setHelp(false))
        )
      else EmptyVdom
    )

  private def renderHelpPopover(close: Callback): VdomElement =
    <.div(
      ^.className  := "viz-modal__help-popover",
      ^.role       := "dialog",
      ^.aria.label := "How Visualise works",
      <.div(
        ^.className := "viz-modal__help-head",
        <.span(^.className := "viz-modal__help-heading", "How Visualise works"),
        <.button(
          ^.tpe        := "button",
          ^.className  := "viz-modal__help-close",
          ^.aria.label := "Close",
          ^.onClick --> close,
          LucideIcons.X(LucideIcons.withClass("viz-modal__help-close-icon"))
        )
      ),
      helpSection(
        "What this is",
        "We ran your code and recorded the data structure after every line. " +
          "This is a replay of the real run — not a simulation."
      ),
      helpSection(
        "Navigating",
        "← / → or Prev / Next move one step. Space toggles play. R restarts, " +
          "F jumps to the final step. The code pane highlights the active line."
      ),
      helpSection(
        "Multiple test cases",
        "When the program runs several cases, a Case strip appears in the toolbar. " +
          "Each case replays independently from step 0."
      ),
      <.div(
        ^.className := "viz-modal__help-section",
        <.span(^.className := "viz-modal__help-section-title", "Keyboard shortcuts"),
        <.ul(
          ^.className := "algolens__help-shortcuts",
          helpShortcut("Space", "Play / Pause"),
          helpShortcut("← →", "Step backward / forward"),
          helpShortcut("R", "Restart"),
          helpShortcut("F", "Jump to final step"),
          helpShortcut("T", "Open execution timeline"),
          helpShortcut("?", "Open shortcuts overlay"),
          helpShortcut("+ −", "Zoom in / out"),
          helpShortcut("0", "Reset zoom"),
          helpShortcut("Esc", "Close (or dismiss this popover)")
        )
      )
    )

  private def helpSection(title: String, body: String): VdomElement =
    <.div(
      ^.className := "viz-modal__help-section",
      <.span(^.className := "viz-modal__help-section-title", title),
      <.p(^.className    := "viz-modal__help-section-body", body)
    )

  private def helpShortcut(key: String, desc: String): VdomElement =
    <.li(
      ^.className := "algolens__help-shortcut",
      <.kbd(^.className := "algolens__help-key", key),
      <.span(desc)
    )

  // ─── Canvas pane ──────────────────────────────────────────────────────────────

  private def renderCanvasPane(
      phase: Phase,
      hostId: String,
      zoom: Double,
      lineCtx: LineContext
  ): VdomElement =
    <.div(
      ^.className := "algolens__pane algolens__pane--canvas",
      phase match
        case Idle | Tracing =>
          <.div(
            ^.className := "algolens__status",
            LucideIcons.Loader2(LucideIcons.withClass("algolens__status-icon")),
            <.span("Tracing the code…")
          )
        case Failed(msg) =>
          <.div(
            ^.className := "algolens__status algolens__status--error",
            <.p(^.className   := "algolens__status-title", "Couldn't visualise this code"),
            <.pre(^.className := "algolens__status-detail", msg)
          )
        case Ready(_, _, _) =>
          React.Fragment(
            // The diagram occupies (and centres in) the pane; the source-line
            // footer is pinned at the bottom.
            <.div(
              ^.className := "algolens-canvas-stage",
              <.div(
                ^.style := js.Dynamic
                  .literal(
                    transform = s"scale($zoom)",
                    transformOrigin = "top left"
                  )
                  .asInstanceOf[js.Object],
                <.div(^.id := hostId, ^.className := "viz-modal__host")
              )
            ),
            renderCanvasLines(lineCtx)
          )
    )

  /**
   * Source-line footer under the diagram: the line the previous / current / next trace step runs. The
   * human-friendly "what changed" sentence stays on the diagram itself, so these are complementary, not
   * duplicate, views — and they put the otherwise-empty lower pane to use.
   */
  private def renderCanvasLines(ctx: LineContext): VdomElement =
    def row(label: String, info: Option[(Int, String)], modifier: String): VdomElement =
      <.div(
        ^.className := s"algolens-canvas-line $modifier",
        <.span(^.className := "algolens-canvas-line__label", label),
        info.fold[VdomNode](
          <.span(^.className := "algolens-canvas-line__code algolens-canvas-line__code--none", "—")
        ) { case (n, text) =>
          <.code(
            ^.className := "algolens-canvas-line__code",
            <.span(^.className := "algolens-canvas-line__num", s"L$n"),
            text
          )
        }
      )
    <.div(
      ^.className := "algolens-canvas-lines",
      row("Prev line", ctx.prev, "algolens-canvas-line--prev"),
      row("Current line", ctx.current, "algolens-canvas-line--current"),
      row("Next line", ctx.next, "algolens-canvas-line--next")
    )

  // ─── Frames pane — StackFramesPanel (Slice 2) ────────────────────────────────

  private def renderFramesPane(
      frames: List[VizFrame],
      hovered: Option[String],
      onHoverName: String => Callback,
      onHoverLeave: Callback
  ): VdomElement =
    val n = frames.size
    <.div(
      ^.className := "algolens__pane algolens__pane--frames",
      <.div(
        ^.className := "algolens-frames",
        <.header(
          ^.className := "algolens-frames__head",
          <.span(
            ^.className := "algolens-frames__eyebrow",
            s"STACK · $n ${if n == 1 then "FRAME" else "FRAMES"}"
          ),
          <.span(^.className := "algolens-frames__hint", "newest on top")
        ),
        if frames.isEmpty then
          <.div(
            ^.className := "algolens-frames__placeholder",
            <.span("No frames yet — press ▶ or → to start")
          )
        else
          <.div(
            ^.className := "algolens-frames__list",
            frames.zipWithIndex.toVdomArray { case (frame, idx) =>
              renderFrameCard(frame, idx, hovered, onHoverName, onHoverLeave)
            }
          )
      )
    )

  private def renderFrameCard(
      frame: VizFrame,
      idx: Int,
      hovered: Option[String],
      onHoverName: String => Callback,
      onHoverLeave: Callback
  ): VdomElement =
    val cls =
      if frame.isActive then "algolens-frame"
      else "algolens-frame algolens-frame--parent"
    <.div(
      ^.key       := idx.toString,
      ^.className := cls,
      <.div(
        ^.className := "algolens-frame__head",
        <.span(
          ^.className := "algolens-frame__title",
          <.em(frame.fn),
          <.span(^.className := "algolens-frame__paren", "()")
        )
      ),
      if frame.locals.nonEmpty then
        <.div(
          ^.className := "algolens-frame__locals",
          frame.locals.zipWithIndex.toVdomArray { case (local, li) =>
            renderFrameLocal(local, li, hovered, onHoverName, onHoverLeave)
          }
        )
      else EmptyVdom
    )

  private def renderFrameLocal(
      local: VizLocal,
      idx: Int,
      hovered: Option[String],
      onHoverName: String => Callback,
      onHoverLeave: Callback
  ): VdomElement =
    val isHovered = hovered.contains(local.name)
    val cls =
      val base = if local.changed then "algolens-frame__local algolens-frame__local--changed"
      else "algolens-frame__local"
      if isHovered then s"$base algolens-frame__local--hovered" else base
    <.div(
      ^.key       := idx.toString,
      ^.className := cls,
      // Slice 4 — ArrowLayer source anchor. Scoped to the active frame in CSS
      // via `.algolens-frame:not(.algolens-frame--parent) [data-local-id]`.
      VdomAttr("data-local-id") := local.name,
      ^.onMouseEnter --> onHoverName(local.name),
      ^.onMouseLeave --> onHoverLeave,
      <.span(^.className := "algolens-frame__local-name", local.name),
      <.span(^.className := "algolens-frame__local-type", local.typeName),
      <.span(^.className := "algolens-frame__local-val", local.value),
      <.span() // 4th grid column — reserved for future change indicator dot
    )

  // ─── Controls bar ─────────────────────────────────────────────────────────────

  private def renderControlsBar(
      stepper: Stepper.Output,
      stepCount: Int,
      currentLine: Option[Int]
  ): VdomElement =
    val pct     = if stepCount <= 1 then 0.0 else stepper.index.toDouble / (stepCount - 1) * 100.0
    val lineStr = currentLine.fold("—")(_.toString)

    // CSS custom property `--pct` controls the scrubber filled-track gradient.
    val scrubStyle = {
      val o = js.Dynamic.literal()
      o.updateDynamic("--pct")(s"${pct.toInt}%")
      o.asInstanceOf[js.Object]
    }

    <.div(
      ^.className := "algolens-controls",
      // Transport buttons: |< < ▶/⏸ > >|
      <.div(
        ^.className := "algolens-controls__transport",
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-controls__tbtn",
          ^.title      := "Jump to start (R)",
          ^.aria.label := "Jump to start",
          ^.disabled   := stepper.atStart,
          ^.onClick --> stepper.reset,
          LucideIcons.ChevronsLeft(LucideIcons.withClass("algolens-controls__tbtn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-controls__tbtn",
          ^.title      := "Previous step (←)",
          ^.aria.label := "Previous step",
          ^.disabled   := stepper.atStart,
          ^.onClick --> stepper.previous,
          LucideIcons.ChevronLeft(LucideIcons.withClass("algolens-controls__tbtn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-controls__tbtn algolens-controls__tbtn--play",
          ^.title      := "Play / Pause (Space)",
          ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
          ^.onClick --> stepper.togglePlay,
          if stepper.isPlaying then
            LucideIcons.Pause(LucideIcons.withClass("algolens-controls__tbtn-icon"))
          else
            LucideIcons.Play(LucideIcons.withClass("algolens-controls__tbtn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-controls__tbtn",
          ^.title      := "Next step (→)",
          ^.aria.label := "Next step",
          ^.disabled   := stepper.atEnd,
          ^.onClick --> stepper.next,
          LucideIcons.ChevronRight(LucideIcons.withClass("algolens-controls__tbtn-icon"))
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-controls__tbtn",
          ^.title      := "Jump to end (F)",
          ^.aria.label := "Jump to end",
          ^.disabled   := stepper.atEnd,
          ^.onClick --> stepper.jumpTo(math.max(0, stepCount - 1)),
          LucideIcons.ChevronsRight(LucideIcons.withClass("algolens-controls__tbtn-icon"))
        )
      ),
      // Scrubber + step meta
      <.div(
        ^.className := "algolens-controls__scrubber-wrap",
        <.input(
          ^.tpe        := "range",
          ^.min        := "0",
          ^.max        := math.max(0, stepCount - 1).toString,
          ^.step       := "1",
          ^.value      := stepper.index.toString,
          ^.aria.label := "Scrub timeline",
          ^.className  := "algolens-controls__scrubber",
          ^.style      := scrubStyle,
          ^.disabled   := (stepCount <= 1),
          ^.onChange ==> { (e: ReactEventFromInput) =>
            val i = e.target.value.toIntOption.getOrElse(0)
            stepper.jumpTo(i)
          }
        ),
        <.div(
          ^.className := "algolens-controls__step-meta",
          <.span(
            ^.className := "algolens-controls__step-counter",
            <.b(s"Step ${stepper.index + 1}"),
            <.span(s" / $stepCount")
          ),
          <.span(^.className := "algolens-controls__sep", "·"),
          <.span(
            <.span(^.className := "algolens-controls__meta-label", "LINE "),
            <.b(lineStr)
          )
        )
      )
    )

  // ─── Output panel (collapsible) ───────────────────────────────────────────────

  private def renderOutputPanel(collapsed: Boolean, toggle: Callback, stdout: String): VdomElement =
    <.div(
      ^.className := (if collapsed then "algolens-output algolens-output--collapsed"
                      else "algolens-output"),
      <.header(
        ^.className := "algolens-output__head",
        <.div(
          ^.className := "algolens-output__tabs",
          <.button(
            ^.tpe       := "button",
            ^.className := "algolens-output__tab algolens-output__tab--active",
            "OUTPUT"
          )
        ),
        <.button(
          ^.tpe        := "button",
          ^.className  := "algolens-output__collapse",
          ^.aria.label := (if collapsed then "Expand output" else "Collapse output"),
          ^.onClick --> toggle,
          if collapsed then
            LucideIcons.ChevronRight(LucideIcons.withClass("algolens-output__collapse-icon"))
          else
            LucideIcons.ChevronDown(LucideIcons.withClass("algolens-output__collapse-icon"))
        )
      ),
      if !collapsed then
        <.div(
          ^.className := "algolens-output__body",
          <.pre(
            ^.className := "algolens-output__pre",
            if stdout.nonEmpty then <.span(stdout)
            else
              <.span(
                ^.className := "algolens-output__placeholder",
                "// run output will appear here"
              )
          )
        )
      else EmptyVdom
    )

  // ─── Timeline drawer (Slice 7) ────────────────────────────────────────────────

  /**
   * Translate a real step index back to its position in the filtered (non-`unchanged`) list, for use when
   * diff mode is on and a user clicks a timeline row. If the real index does not appear in the filtered list
   * (i.e. the row is itself `unchanged`), return `realIdx` unchanged — the Stepper will clamp to the nearest
   * valid position.
   */
  private def realToFilteredStep(phase: Phase, caseIdx: Int, diffMode: Boolean, realIdx: Int): Int =
    if !diffMode then realIdx
    else
      phase match
        case Ready(cases, _, _) =>
          cases.cases
            .lift(caseIdx)
            .flatMap(g =>
              g.steps.zipWithIndex
                .filter(!_._1.unchanged)
                .map(_._2)
                .zipWithIndex
                .collectFirst { case (ri, fi) if ri == realIdx => fi }
            )
            .getOrElse(realIdx)
        case _ => realIdx

  /**
   * Slide-in execution timeline — one row per step, showing step number, source line, and annotation. Active
   * step is highlighted; `unchanged` steps are dimmed. Clicking a row jumps to that step. `T` toggles the
   * drawer; `Esc` closes it (handled in Effect A's layered Esc logic).
   */
  private def renderTimelineDrawer(
      open: Boolean,
      onClose: Callback,
      steps: List[VizGraphStep],
      realStepIdx: Int,
      onJump: Int => Callback
  ): VdomNode =
    if !open then EmptyVdom
    else
      <.aside(
        ^.className  := "algolens-timeline",
        ^.role       := "dialog",
        ^.aria.label := "Execution timeline",
        ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
        <.header(
          ^.className := "algolens-timeline__head",
          <.span(^.className := "algolens-timeline__eyebrow", s"TIMELINE · ${steps.size} STEPS"),
          <.em(^.className   := "algolens-timeline__title", "Execution"),
          <.button(
            ^.tpe       := "button",
            ^.className := "algolens-timeline__close",
            ^.onClick --> onClose,
            ^.title := "Close (Esc)",
            LucideIcons.X(LucideIcons.withClass("algolens__topbar-btn-icon"))
          )
        ),
        <.div(
          ^.className := "algolens-timeline__list",
          steps.zipWithIndex.toVdomArray { case (step, i) =>
            val isActive = i == realStepIdx
            val cls =
              "algolens-timeline__row" +
                (if isActive then " algolens-timeline__row--active" else "") +
                (if step.unchanged then " algolens-timeline__row--unchanged" else "")
            <.button(
              ^.key       := i.toString,
              ^.className := cls,
              ^.onClick --> onJump(i),
              <.span(^.className := "algolens-timeline__step", f"${i + 1}%02d"),
              <.span(^.className := "algolens-timeline__event", "·"),
              <.span(^.className := "algolens-timeline__line", s"L${step.line}"),
              <.span(^.className := "algolens-timeline__summary", step.annotation.title),
              if step.unchanged then
                <.span(
                  ^.className := "algolens-timeline__unchanged-tag",
                  ^.title     := "No state change",
                  "no Δ"
                )
              else EmptyVdom
            )
          }
        ),
        <.footer(
          ^.className := "algolens-timeline__foot",
          "Click any row to jump · Esc closes"
        )
      )

  // ─── Shortcuts overlay (Slice 7) ──────────────────────────────────────────────

  /**
   * Full-screen keyboard shortcuts overlay. Click the backdrop to close; `Esc` closes it via Effect A's
   * layered handler. Listed shortcuts mirror the help popover's shortcut section but in the overlay style
   * (backdrop + centred card).
   */
  private def renderShortcutsOverlay(open: Boolean, onClose: Callback): VdomNode =
    if !open then EmptyVdom
    else
      <.div(
        ^.className := "algolens-shortcuts",
        ^.onClick --> onClose,
        <.div(
          ^.className := "algolens-shortcuts__card",
          ^.onClick ==> { (e: ReactEvent) => e.stopPropagationCB },
          <.header(
            ^.className := "algolens-shortcuts__head",
            <.span(^.className := "algolens-shortcuts__eyebrow", "KEYBOARD · ?"),
            <.em("Shortcuts"),
            <.button(
              ^.tpe       := "button",
              ^.className := "algolens-shortcuts__close",
              ^.onClick --> onClose,
              LucideIcons.X(LucideIcons.withClass("algolens__topbar-btn-icon"))
            )
          ),
          <.div(
            ^.className := "algolens-shortcuts__rows",
            shortcutRow("← / →", "Step backward / forward"),
            shortcutRow("Space", "Play / Pause"),
            shortcutRow("R", "Reset to step 0"),
            shortcutRow("F", "Jump to final step"),
            shortcutRow("T", "Open execution timeline"),
            shortcutRow("?", "Open this panel"),
            shortcutRow("+ −", "Zoom in / out"),
            shortcutRow("0", "Reset zoom"),
            shortcutRow("Esc", "Close overlay")
          ),
          <.footer(
            ^.className := "algolens-shortcuts__foot",
            "The trace runs locally — your code never leaves the browser."
          )
        )
      )

  /** One row in the shortcuts overlay — `<kbd>` key + label. */
  private def shortcutRow(key: String, label: String): VdomElement =
    <.div(
      ^.className := "algolens-shortcuts__row",
      <.kbd(^.className  := "algolens-shortcuts__kbd", key),
      <.span(^.className := "algolens-shortcuts__label", label)
    )
