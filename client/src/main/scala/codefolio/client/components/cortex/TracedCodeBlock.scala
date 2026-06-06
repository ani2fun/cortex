package codefolio.client.components.cortex

import codefolio.client.api.ApiClient
import codefolio.client.components.icons.{BrandIcons, LucideIcons}
import codefolio.shared.api.Endpoints.RunRequest
import codefolio.shared.viz.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

/**
 * Step-through visualisation of a traced language source. The server-side `/api/run` is unchanged — the
 * tracer ([[PythonTracer]] / [[JvmTracer]]) wraps the user source in a harness, the client posts the wrapped
 * program, then [[MarkedTrace.parse]] splits the captured trace out of stdout and this component renders code
 * + locals panel + step controls.
 *
 * The harness is delimited by markers so the user's real output (if any) survives the parse and shows up in a
 * "Program output" panel.
 *
 * **Collapsed by default.** The header is always visible (so the chapter shows that an interactive tracer is
 * available); the body — source, locals, caption, program output — only appears once the reader clicks
 * "Trace" (which expands and runs) or "Show" (after a previous trace). A "Hide" toggle in the header
 * re-collapses without losing trace state.
 *
 * Supported fence forms: ` ```python trace ` and ` ```java trace `. The component renders a friendly error
 * for any other language so the chapter doesn't silently fail on unsupported fences.
 *
 * **Language tabs (Slice 7).** When the chapter ships adjacent `kotlin trace` / `scala trace` fences
 * alongside the primary traced fence, a tab strip appears in the header. Clicking a companion-language tab
 * shows its source with syntax highlighting and a `LanguageLockBanner` — no tracer controls (the trace is
 * always anchored to the primary language).
 */
object TracedCodeBlock:

  @js.native @JSImport("@markdown/runtime", "highlightWithPrism")
  private def highlightWithPrism(code: String, lang: String): String = js.native

  /** A source-only companion language tab (e.g. Kotlin, Scala) alongside the primary traced language. */
  final case class Companion(language: String, source: String)

  final case class Props(
      language: String,
      source: String,
      companions: List[Companion] = Nil
  )

  // ---------------------------------------------------------------------------
  // Trace data — the heap-snapshot trace comes from the shared PythonTracer.
  // ---------------------------------------------------------------------------

  /** A ready trace: the decoded heap-snapshot steps plus the user program's own stdout. */
  final private case class Trace(steps: List[HeapStep], programStdout: String)

  // ---------------------------------------------------------------------------
  // UI state machine
  // ---------------------------------------------------------------------------

  sealed private trait Phase
  private case object Idle                                                       extends Phase
  private case object Running                                                    extends Phase
  final private case class Failed(message: String)                               extends Phase
  final private case class Ready(trace: Trace, stepIndex: Int, playing: Boolean) extends Phase

  private val StepDelayMs = 700

  private given ExecutionContext = JSExecutionContext.queue

  // ---------------------------------------------------------------------------
  // Component
  // ---------------------------------------------------------------------------

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState[Phase](Idle)
      // Body is auto-hidden by default — chapter prose doesn't get a giant code+locals panel sitting under
      // every fence. The header stays visible so the affordance is discoverable.
      .useState(false)
      .useRefBy(_ => Option.empty[Int])
      // depsBy returns a primitive tuple so we don't need a `Reusability[Phase]`. The effect only cares
      // about the auto-advance loop: (playing, idx, totalSteps). Idle / Running / Failed collapse to
      // (false, -1, 0) — guaranteed to differ from any Ready trigger, so the cancel-then-install loop runs.
      .useEffectWithDepsBy { (_, phaseS, _, _) =>
        phaseS.value match
          case Ready(t, i, p) => (p, i, t.steps.length)
          case _              => (false, -1, 0)
      } { (_, phaseS, _, timeoutRef) => (playing, idx, total) =>
        Callback {
          timeoutRef.value.foreach(dom.window.clearTimeout)
          timeoutRef.value = None
          if playing then
            if idx < total - 1 then
              val id = dom.window.setTimeout(
                () =>
                  phaseS
                    .modState {
                      case Ready(t, i, true) if i < t.steps.length - 1 => Ready(t, i + 1, true)
                      case Ready(t, i, _)                              => Ready(t, i, false)
                      case other                                       => other
                    }
                    .runNow(),
                StepDelayMs.toDouble
              )
              timeoutRef.value = Some(id)
            else
              phaseS.modState {
                case Ready(t, i, _) => Ready(t, i, false)
                case other          => other
              }.runNow()
        }
      }
      // langTabS: 0 = primary traced language, 1+ = companion index (1-based into props.companions).
      // Lives after the auto-play effect so neither the deps nor effect fn are affected.
      .useState(0)
      .render { (props, phaseS, visibleS, _, langTabS) =>
        if !isSupportedTrace(props.language) then
          <.div(
            ^.className := "traced-code__error",
            <.p(^.className := "traced-code__error-title", "Traced execution: language not supported"),
            <.p(
              ^.className := "traced-code__error-message",
              s"Supported fence forms: ` ```python trace ` and ` ```java trace `; got '${props.language}'."
            )
          )
        else
          val activeTab   = langTabS.value
          val selectTab   = (i: Int) => langTabS.setState(i)
          val isCompanion = activeTab > 0 && activeTab <= props.companions.size

          // ── Companion (source-only) tab is active ─────────────────────────────
          if isCompanion then
            val c = props.companions(activeTab - 1)
            renderCompanionView(props, c, activeTab, selectTab)
          else
            // ── Primary traced language is active ─────────────────────────────
            // Choose the tracer and request language based on the fence language.
            val (wrapped, reqLang) =
              if isPython(props.language) then (PythonTracer.wrap(props.source), "python")
              else (JvmTracer.wrap(props.source), "java")

            val runTrace: Callback =
              visibleS.setState(true) >> phaseS.setState(Running) >> Callback {
                val req = RunRequest(language = reqLang, source = wrapped, stdin = None)
                ApiClient.runCode(req).onComplete {
                  case Success(resp) =>
                    val r      = resp.result
                    val ok     = r.statusId == 3
                    val parsed = MarkedTrace.parse(Option(r.stdout).getOrElse(""))
                    val trace  = Trace(parsed.trace.steps, parsed.programStdout)
                    if !ok && trace.steps.isEmpty then
                      val errMsg =
                        Seq(
                          Option(r.statusDescription).filter(_.nonEmpty),
                          Option(r.compileOutput).filter(_.nonEmpty).map(s => s"compile: $s"),
                          Option(r.stderr).filter(_.nonEmpty).map(s => s"stderr: $s")
                        ).flatten.mkString(" — ")
                      phaseS.setState(Failed(if errMsg.nonEmpty then errMsg else "execution failed")).runNow()
                    else phaseS.setState(Ready(trace, 0, false)).runNow()
                  case Failure(t) =>
                    phaseS.setState(Failed(Option(t.getMessage).getOrElse("network error"))).runNow()
                }
              }

            val visible = visibleS.value
            // Hiding while auto-playing would advance steps invisibly; pause as part of the hide step so
            // re-showing leaves the reader on a deterministic step. `visible` is captured at render time,
            // so we know whether this toggle is a hide-action (and need to pause) or a show-action (no-op).
            val toggleVisible: Callback =
              if visible then
                visibleS.setState(false) >> phaseS.modState {
                  case Ready(t, i, _) => Ready(t, i, false)
                  case other          => other
                }
              else visibleS.setState(true)

            phaseS.value match
              case Idle =>
                renderShell(
                  props,
                  idleControls(runTrace, visible, toggleVisible, hasTrace = false),
                  None,
                  hasTrace = false,
                  visible = visible,
                  activeTab = activeTab,
                  selectTab = selectTab
                )
              case Running =>
                renderShell(
                  props,
                  runningControls,
                  None,
                  hasTrace = false,
                  visible = visible,
                  activeTab = activeTab,
                  selectTab = selectTab
                )
              case Failed(msg) =>
                renderShell(
                  props,
                  failedControls(msg, runTrace, visible, toggleVisible),
                  None,
                  hasTrace = false,
                  visible = visible,
                  activeTab = activeTab,
                  selectTab = selectTab
                )
              case Ready(trace, idx, playing) =>
                val ctrls = readyControls(
                  idx,
                  trace.steps.length,
                  playing,
                  visible,
                  onPrev = phaseS.modState {
                    case Ready(t, i, _) => Ready(t, math.max(0, i - 1), false)
                    case other          => other
                  },
                  onNext = phaseS.modState {
                    case Ready(t, i, _) => Ready(t, math.min(t.steps.length - 1, i + 1), false)
                    case other          => other
                  },
                  onReset = phaseS.modState {
                    case Ready(t, _, _) => Ready(t, 0, false)
                    case other          => other
                  },
                  onTogglePlay = phaseS.modState {
                    case Ready(t, i, p) =>
                      if !p && i >= t.steps.length - 1 then Ready(t, 0, true)
                      else Ready(t, i, !p)
                    case other => other
                  },
                  onReTrace = runTrace,
                  onToggleVisible = toggleVisible
                )
                renderShell(
                  props,
                  ctrls,
                  Some((trace, idx)),
                  hasTrace = true,
                  visible = visible,
                  activeTab = activeTab,
                  selectTab = selectTab
                )
      }

  /** Fence-language tag carried through to the scalar display so `true`/`null` show for Java fences. */
  private enum DisplayLang:
    case Py, Jvm

  private def displayLang(language: String): DisplayLang =
    if isJava(language) then DisplayLang.Jvm else DisplayLang.Py

  private def isPython(lang: String): Boolean =
    val l = lang.toLowerCase
    l == "python" || l == "py" || l == "python3"

  private def isJava(lang: String): Boolean = lang.toLowerCase == "java"

  private def isSupportedTrace(lang: String): Boolean = isPython(lang) || isJava(lang)

  /** Short display label for the primary traced language tab. */
  private def primaryLanguageLabel(lang: String): String =
    if isJava(lang) then "☕ Java · traced"
    else "🐍 Python · traced"

  /** Short tab label for the primary language when a tab strip is shown (no "· traced" suffix). */
  private def primaryTabLabel(lang: String): String =
    if isJava(lang) then "☕ Java"
    else "🐍 Python"

  /**
   * Tab label for a companion language. Kotlin gets its emoji; Scala gets the text (brand icon added in JSX).
   */
  private def companionTabLabel(lang: String): String = lang.toLowerCase match
    case "kotlin" | "kt" => "💜 Kotlin"
    case _               => lang.capitalize // "Scala" — brand icon prepended by renderTabStrip

  private def isScala(lang: String): Boolean = lang.toLowerCase == "scala"

  // ---------------------------------------------------------------------------
  // Subviews
  // ---------------------------------------------------------------------------

  /**
   * Render the multi-language tab strip — shown only when `props.companions` is non-empty. Tab 0 is the
   * primary traced language; tabs 1+ are source-only companions in order.
   */
  private def renderTabStrip(props: Props, activeTab: Int, selectTab: Int => Callback): VdomElement =
    <.div(
      ^.className := "traced-code__tabs",
      // Tab 0 — primary language (always first)
      <.button(
        ^.key := "0",
        ^.tpe := "button",
        ^.className :=
          (if activeTab == 0 then "traced-code__tab traced-code__tab--active"
           else "traced-code__tab"),
        ^.onClick --> selectTab(0),
        primaryTabLabel(props.language)
      ),
      // Tabs 1+ — companion languages
      props.companions.zipWithIndex.toTagMod { case (c, idx) =>
        val tabIdx   = idx + 1
        val isActive = activeTab == tabIdx
        <.button(
          ^.key := tabIdx.toString,
          ^.tpe := "button",
          ^.className :=
            (if isActive then "traced-code__tab traced-code__tab--active"
             else "traced-code__tab"),
          ^.onClick --> selectTab(tabIdx),
          // Scala gets the real wave-mark brand icon (no clean emoji); all others use the emoji prefix.
          if isScala(c.language) then
            TagMod(
              BrandIcons.Scala("traced-code__brand-icon traced-code__brand-icon--scala"),
              companionTabLabel(c.language)
            )
          else companionTabLabel(c.language)
        )
      }
    )

  /**
   * Lock banner shown when a source-only companion tab is active. Explains that the trace is anchored to the
   * primary language and invites the reader to switch back.
   */
  private def renderLockBanner(companionLang: String, switchBack: Callback): VdomElement =
    val langDisplay = companionTabLabel(companionLang)
    <.div(
      ^.className := "traced-code__lock-banner",
      LucideIcons.Lock(LucideIcons.withClass("traced-code__lock-banner-icon")),
      <.span(
        ^.className := "traced-code__lock-banner-text",
        s"Trace frozen · $langDisplay is source-only — "
      ),
      <.button(
        ^.tpe       := "button",
        ^.className := "traced-code__lock-banner-link",
        ^.onClick --> switchBack,
        "switch back to step"
      )
    )

  /**
   * Renders the full block when a source-only companion tab (Kotlin, Scala, …) is active. Shows the source
   * with syntax highlighting and a `LanguageLockBanner`; no trace controls, no locals panel.
   */
  private def renderCompanionView(
      props: Props,
      companion: Companion,
      activeTab: Int,
      selectTab: Int => Callback
  ): VdomElement =
    // `renderSource` now uses the language string directly for Prism highlighting.
    <.div(
      ^.className := "traced-code",
      <.div(
        ^.className := "traced-code__header",
        renderTabStrip(props, activeTab, selectTab),
        renderLockBanner(companion.language, selectTab(0))
      ),
      <.div(
        ^.className := "traced-code__body",
        renderSource(companion.source, companion.language, None)
      )
    )

  private def renderShell(
      props: Props,
      controls: VdomElement,
      readyState: Option[(Trace, Int)],
      hasTrace: Boolean,
      visible: Boolean,
      activeTab: Int,
      selectTab: Int => Callback
  ): VdomElement =
    val rootClasses =
      val base = if hasTrace then "traced-code traced-code--has-trace" else "traced-code"
      if visible then base else s"$base traced-code--collapsed"
    val hasTabs = props.companions.nonEmpty
    <.div(
      ^.className := rootClasses,
      <.div(
        ^.className := "traced-code__header",
        // When companions exist, replace the plain language label with a tab strip.
        if hasTabs then renderTabStrip(props, activeTab, selectTab)
        else <.span(^.className := "traced-code__language-label", primaryLanguageLabel(props.language)),
        controls
      ),
      if !visible then EmptyVdom
      else
        val lang = displayLang(props.language)
        React.Fragment(
          <.div(
            ^.className := "traced-code__body",
            renderSource(props.source, props.language, readyState.map { case (t, i) => t.steps(i).line }),
            readyState match
              case Some((trace, idx)) => renderLocalsPanel(trace.steps(idx), lang)
              case None               => EmptyVdom
          ),
          readyState match
            case Some((trace, idx)) =>
              val step   = trace.steps(idx)
              val fnName = step.frames.headOption.fold("")(_.fn)
              <.p(
                ^.className := "traced-code__caption",
                ^.aria.live := "polite",
                s"${step.event} in $fnName() — line ${step.line}"
              )
            case None => EmptyVdom,
          readyState match
            case Some((trace, _)) if trace.programStdout.nonEmpty =>
              <.details(
                ^.className := "traced-code__details",
                <.summary(^.className := "traced-code__details-summary", "Program output"),
                <.pre(^.className     := "traced-code__details-pre", trace.programStdout)
              )
            case _ => EmptyVdom
        )
    )

  private def renderSource(source: String, language: String, currentLine: Option[Int]): VdomElement =
    val prismLang = language.toLowerCase match
      case "java"          => "java"
      case "kotlin" | "kt" => "kotlin"
      case "scala"         => "scala"
      case _               => "python" // default: python / pseudocode
    val lines = source.split("\n", -1)
    <.pre(
      ^.className := "traced-code__source not-prose",
      lines.zipWithIndex.toVdomArray { case (line, i) =>
        val lineNo    = i + 1
        val isCurrent = currentLine.contains(lineNo)
        val cls =
          if isCurrent then "traced-code__line traced-code__line--current"
          else "traced-code__line"
        <.span(
          ^.key       := lineNo.toString,
          ^.className := cls,
          <.span(^.className := "traced-code__line-number", f"$lineNo%4d"),
          <.code(
            ^.className               := "traced-code__line-code",
            ^.dangerouslySetInnerHtml := highlightWithPrism(if line.isEmpty then " " else line, prismLang)
          )
        )
      }
    )

  private def renderLocalsPanel(step: HeapStep, lang: DisplayLang): VdomElement =
    val active = step.frames.headOption
    val fnName = active.fold("")(_.fn)
    val locals = active.fold(List.empty[(String, HeapValue)])(_.locals)
    <.div(
      ^.className := "traced-code__locals",
      <.p(^.className := "traced-code__locals-title", s"Locals — $fnName()"),
      if locals.isEmpty then
        <.p(^.className := "traced-code__locals-empty", "(no locals yet)"): VdomNode
      else
        <.table(
          ^.className := "traced-code__locals-table",
          <.tbody(
            locals.toVdomArray { case (k, v) =>
              <.tr(
                ^.key := k,
                <.td(^.className := "traced-code__locals-name", k),
                <.td(^.className := "traced-code__locals-value", displayValue(v, step.heap, lang))
              )
            }
          )
        )
    )

  /**
   * Render a captured local for the locals table: scalars inline, objects as a compact type tag. Booleans and
   * null are language-tagged so a Java fence shows `true` / `false` / `null` and a Python fence keeps `True`
   * / `False` / `None` — the rest of the rendering is identical across languages.
   */
  private def displayValue(v: HeapValue, heap: Map[String, HeapObject], lang: DisplayLang): String =
    v match
      case HeapValue.Scalar(HeapScalar.I(n)) => n.toString
      case HeapValue.Scalar(HeapScalar.D(d)) => d.toString
      case HeapValue.Scalar(HeapScalar.B(b)) =>
        lang match
          case DisplayLang.Jvm => if b then "true" else "false"
          case DisplayLang.Py  => if b then "True" else "False"
      case HeapValue.Scalar(HeapScalar.S(s)) => "\"" + s + "\""
      case HeapValue.Scalar(HeapScalar.Null) =>
        lang match
          case DisplayLang.Jvm => "null"
          case DisplayLang.Py  => "None"
      case HeapValue.Ref(id) =>
        heap.get(id) match
          case Some(HeapObject.Instance(cls, _))         => cls
          case Some(HeapObject.Arr(ArrKind.Lst, items))  => s"list[${items.size}]"
          case Some(HeapObject.Arr(ArrKind.Tup, items))  => s"tuple[${items.size}]"
          case Some(HeapObject.Arr(ArrKind.JArr, items)) => s"array[${items.size}]"
          case Some(HeapObject.Dict(entries))            => s"dict[${entries.size}]"
          case None                                      => "<ref>"

  // Eye-icon toggle for show/hide. Lives in the header alongside other controls; label flips with state.
  private def visibilityToggle(visible: Boolean, onToggle: Callback): VdomElement =
    <.button(
      ^.tpe := "button",
      ^.onClick --> onToggle,
      ^.aria.label := (if visible then "Hide tracer" else "Show tracer"),
      ^.className  := "traced-code__button traced-code__button--ghost",
      if visible then LucideIcons.EyeOff(LucideIcons.withClass("traced-code__button-icon"))
      else LucideIcons.Eye(LucideIcons.withClass("traced-code__button-icon")),
      if visible then "Hide" else "Show"
    )

  private def idleControls(
      onTrace: Callback,
      visible: Boolean,
      onToggleVisible: Callback,
      hasTrace: Boolean
  ): VdomElement =
    <.div(
      ^.className := "traced-code__controls",
      <.button(
        ^.tpe := "button",
        ^.onClick --> onTrace,
        ^.className := "traced-code__button traced-code__button--primary",
        LucideIcons.Play(LucideIcons.withClass("traced-code__button-icon")),
        "Trace"
      ),
      // Only show Hide/Show when there's something in the body worth toggling. In Idle with no prior trace,
      // hiding the (empty) body has no effect — and the "Trace" button itself acts as the expand affordance.
      if hasTrace || visible then visibilityToggle(visible, onToggleVisible) else EmptyVdom
    )

  private def runningControls: VdomElement =
    <.div(
      ^.className := "traced-code__controls",
      <.span(
        ^.className := "traced-code__loading",
        LucideIcons.Loader2(LucideIcons.withClass("traced-code__loading-icon")),
        "Tracing…"
      )
    )

  private def failedControls(
      message: String,
      onRetry: Callback,
      visible: Boolean,
      onToggleVisible: Callback
  ): VdomElement =
    <.div(
      ^.className := "traced-code__controls",
      <.span(^.className := "traced-code__error-inline", message),
      <.button(
        ^.tpe := "button",
        ^.onClick --> onRetry,
        ^.className := "traced-code__button",
        LucideIcons.RotateCcw(LucideIcons.withClass("traced-code__button-icon")),
        "Retry"
      ),
      visibilityToggle(visible, onToggleVisible)
    )

  private def readyControls(
      idx: Int,
      total: Int,
      playing: Boolean,
      visible: Boolean,
      onPrev: Callback,
      onNext: Callback,
      onReset: Callback,
      onTogglePlay: Callback,
      onReTrace: Callback,
      onToggleVisible: Callback
  ): VdomElement =
    val atStart = idx == 0
    val atEnd   = idx == total - 1
    <.div(
      ^.className := "traced-code__controls",
      // Step controls only make sense while the body is visible. Hidden state keeps the trace in memory but
      // collapses to just the language label + Show toggle.
      if visible then
        React.Fragment(
          <.button(
            ^.tpe := "button",
            ^.onClick --> onPrev,
            ^.disabled   := atStart,
            ^.aria.label := "Previous step",
            ^.className  := "traced-code__button",
            LucideIcons.ArrowLeft(LucideIcons.withClass("traced-code__button-icon")),
            "Prev"
          ),
          <.button(
            ^.tpe := "button",
            ^.onClick --> onTogglePlay,
            ^.aria.label := (if playing then "Pause" else "Play"),
            ^.className  := "traced-code__button traced-code__button--primary",
            if playing then LucideIcons.Pause(LucideIcons.withClass("traced-code__button-icon"))
            else LucideIcons.Play(LucideIcons.withClass("traced-code__button-icon")),
            if playing then "Pause" else "Play"
          ),
          <.button(
            ^.tpe := "button",
            ^.onClick --> onNext,
            ^.disabled   := atEnd,
            ^.aria.label := "Next step",
            ^.className  := "traced-code__button",
            "Next",
            LucideIcons.ArrowRight(LucideIcons.withClass("traced-code__button-icon"))
          ),
          <.button(
            ^.tpe := "button",
            ^.onClick --> onReset,
            ^.disabled   := atStart,
            ^.aria.label := "Reset",
            ^.className  := "traced-code__button traced-code__button--icon",
            LucideIcons.RotateCcw(LucideIcons.withClass("traced-code__button-icon"))
          ),
          <.span(
            ^.className := "traced-code__progress",
            s"Step ${idx + 1} / $total"
          ),
          <.button(
            ^.tpe := "button",
            ^.onClick --> onReTrace,
            ^.aria.label := "Re-run trace",
            ^.className  := "traced-code__button traced-code__button--ghost",
            "Re-trace"
          )
        )
      else
        <.span(
          ^.className := "traced-code__progress",
          s"trace ready · ${total} step${if total == 1 then "" else "s"}"
        )
      ,
      visibilityToggle(visible, onToggleVisible)
    )
