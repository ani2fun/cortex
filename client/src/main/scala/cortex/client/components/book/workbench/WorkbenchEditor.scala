package cortex.client.components.book.workbench

import cortex.client.api.ApiClient
import cortex.client.auth.{AuthStore, IdentityChip}
import cortex.client.components.book.{MonacoEditor, VisualiseModal}
import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.{RunRequest, RunResult, SubmitRequest, SubmitResponse, UserInfo}
import cortex.shared.book.Block
import cortex.shared.runner.CodeExecutor
import cortex.shared.runner.CodeExecutor.RunState
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * The stacked workbench editor — Code pane (toolbar: "Code" eyebrow · language dropdown · Edit · Visualise ·
 * Run) over the schema-driven [[TestCasesPanel]], separated by a draggable horizontal splitter. Used inline
 * in lesson chapters (fixed-height card) and as the right pane of [[ProblemWorkbench]] (fills its column).
 *
 * **Run semantics.** Run executes the ACTIVE test case only: stdin is one line per spec argument in declared
 * order (serialized exactly as typed in the console fields), the existing `/api/run` executes the live editor
 * code, and trimmed stdout is compared against the case's expected output — Accepted marks the case chip
 * passed; a mismatch shows Your-vs-Expected; a non-OK status surfaces stderr / compile output in the Your
 * panel. Cases without an expected value run verdict-free.
 *
 * Reuses [[RunnableCodeBlock]]'s machinery wholesale: the shared [[CodeExecutor]] state machine per language
 * tab (runId staleness, edit mode), the Monaco facade + external-ResizeObserver layout trick, the ADR-0013
 * auth gate on Edit, and the quota notice. The Visualise button traces the active tab with the active case's
 * stdin prefilled (see VisualiseModal.Props.initialStdin).
 */
object WorkbenchEditor:

  @js.native @JSImport("@markdown/runtime", "highlightWithPrism")
  private def highlightWithPrism(code: String, lang: String): String = js.native

  /** Maps our runnable-language aliases (see Languages.scala) onto Monaco's language ids. */
  private def monacoLangFor(alias: String): String = alias.toLowerCase match
    case "python" | "py" | "python3"  => "python"
    case "java"                       => "java"
    case "scala"                      => "scala"
    case "c" | "cpp" | "c++" | "cxx"  => "cpp"
    case "go" | "golang"              => "go"
    case "rust" | "rs"                => "rust"
    case "kotlin" | "kt"              => "kotlin"
    case "typescript" | "ts"          => "typescript"
    case "javascript" | "js" | "node" => "javascript"
    case other                        => other

  /** File extension for the per-tab Monaco model path (cosmetic, but keeps model ids honest). */
  private def extFor(alias: String): String = alias.toLowerCase match
    case "python" | "py" | "python3"  => "py"
    case "java"                       => "java"
    case "scala"                      => "scala"
    case "c"                          => "c"
    case "cpp" | "c++" | "cxx"        => "cpp"
    case "go" | "golang"              => "go"
    case "rust" | "rs"                => "rs"
    case "kotlin" | "kt"              => "kt"
    case "typescript" | "ts"          => "ts"
    case "javascript" | "js" | "node" => "js"
    case _                            => "txt"

  // Unique per-component-instance prefix for Monaco model paths — several workbenches can share a
  // page (inline See-It-Work + Your-Turn), and model paths are global to the Monaco runtime.
  private var wbSeq = 0

  private def nextWbId(): String =
    wbSeq += 1
    s"wb-$wbSeq"

  /** Identifies the problem a Submit should be recorded against (book slug + hierarchical chapter slug). */
  final case class SubmitContext(book: String, chapter: String)

  final case class Props(
      tabs: List[Block.Tab],
      spec: Block.TestSpec,
      // Some(px) → standalone inline card of that height; None → fill the parent (problem workbench pane).
      fixedHeightPx: Option[Int] = Some(620),
      pctClamp: (Double, Double) = (26.0, 74.0),
      defaultPct: Double = 52.0,
      // Present only on problem pages: enables the Submit button (run ALL cases server-side + save).
      submitCtx: Option[SubmitContext] = None,
      // Push the active tab's code / language / last-run to the host (the coach folds it into
      // implement/test turns). None for surfaces with no coach.
      onSnapshot: Option[Snapshot => Callback] = None
  )

  /** The active tab's live code + language + last run result — pushed to the host on change. */
  final case class Snapshot(code: String, language: String, result: Option[RunResult])

  /** Whole-workbench state in one value so async run completions update editor + verdicts atomically. */
  final private case class WbState(
      editors: Vector[CodeExecutor.State],
      activeTab: Int,
      cases: Vector[Block.TestCase],
      values: Vector[Map[String, String]],
      activeCase: Int,
      passed: Set[Int],
      verdicts: Map[Int, TestCasesPanel.Verdict],
      codePct: Double,
      submitting: Boolean,
      // Outcome of the last Submit: Left = human-readable error (incl. the 403 access-request text),
      // Right = the server's verdict across all cases.
      submitNote: Option[Either[String, SubmitResponse]]
  )

  private object WbState:

    def init(p: Props): WbState =
      WbState(
        editors = p.tabs.map(t => CodeExecutor.initial(t.source)).toVector,
        activeTab = 0,
        cases = p.spec.cases.toVector,
        values = p.spec.cases.map(_.args).toVector,
        activeCase = 0,
        passed = Set.empty,
        verdicts = Map.empty,
        codePct = p.defaultPct,
        submitting = false,
        submitNote = None
      )

  private val StatusOk = 3

  private def quotaRunningLow(q: cortex.shared.api.Endpoints.Quota): Boolean =
    q.limit > 0 && q.used * 10 >= q.limit * 7

  private def resetClockUtc(epochMs: Long): String =
    val d  = new js.Date(epochMs.toDouble)
    val hh = d.getUTCHours().toInt
    val mm = d.getUTCMinutes().toInt
    f"$hh%02d:$mm%02d"

  private given ExecutionContext = JSExecutionContext.queue

  /** One stdin line per declared argument, serialized exactly as the console fields show them. */
  private def stdinFor(spec: Block.TestSpec, values: Map[String, String]): String =
    spec.args.map(a => values.getOrElse(a.id, "")).mkString("\n") + "\n"

  private def verdictFor(result: RunResult, expected: Option[String]): TestCasesPanel.Verdict =
    if result.statusId != StatusOk then
      TestCasesPanel.Verdict.Errored(
        result.statusDescription,
        Option(result.stderr).getOrElse(""),
        Option(result.compileOutput).getOrElse("")
      )
    else
      val stdout = Option(result.stdout).getOrElse("")
      expected match
        case Some(exp) if stdout.trim == exp.trim =>
          TestCasesPanel.Verdict.Accepted(result.time, result.memory)
        case Some(_) =>
          TestCasesPanel.Verdict.Wrong(stdout)
        case None =>
          TestCasesPanel.Verdict.Finished(stdout, result.time, result.memory)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useStateBy(WbState.init)
      .useState(AuthStore.current)
      // Mirror the global auth snapshot into local state so the block re-renders on sign-in/out.
      .useEffectOnMountBy { (_, _, authS) =>
        CallbackTo {
          val unsubscribe = AuthStore.subscribe(s => authS.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      .useState(false)                // VisualiseModal open
      .useRefToVdom[dom.html.Element] // whole-card ref (splitter math)
      .useRefToVdom[dom.html.Element] // Monaco wrapper ref (height observer)
      .useState(280)                  // Monaco explicit pixel height, driven by the wrapper's rect
      .useRefBy((_, _, _, _, _, _, _) => nextWbId())                  // stable model-path prefix
      .useRefBy((_, _, _, _, _, _, _, _) => Option.empty[js.Dynamic]) // live editor instance
      // Push the active tab's code / language / last run result to the host whenever they change, so a
      // coach (the left Coach tab) can attach the learner's solution to implement/test turns.
      .useEffectWithDepsBy { (_, st, _, _, _, _, _, _, _) =>
        val s  = st.value
        val ed = s.editors(s.activeTab)
        // RunResult has no Reusability instance; fingerprint it by hashCode so the deps tuple stays
        // (Int, String, Int). Re-emits the snapshot when the active tab, its code, or its run changes.
        (s.activeTab, ed.code, ed.result.hashCode)
      } { (props, st, _, _, _, _, _, _, _) => _ =>
        props.onSnapshot match
          case None => Callback.empty
          case Some(emit) =>
            val s = st.value
            emit(
              Snapshot(
                s.editors(s.activeTab).code,
                props.tabs(s.activeTab).language,
                s.editors(s.activeTab).result
              )
            )
      }
      .render { (props, st, authS, vizOpenS, cardRef, monacoWrapRef, monacoHeightS, wbIdRef, editorRef) =>
        val s       = st.value
        val tabIdx  = s.activeTab
        val tab     = props.tabs(tabIdx)
        val editor  = s.editors(tabIdx)
        val running = editor.runState == RunState.Running

        val status = authS.value.status
        // ADR-0013 edit gate, lifted to "signed-in ⇒ editable everywhere". A signed-in user (or
        // auth-disabled dev) edits every language tab on every page with no per-tab Edit click; an
        // anonymous visitor gets a read-only editor with a locked Edit affordance that opens sign-in.
        // Replaces the old per-tab CodeExecutor.editMode gate, which reset to read-only on every tab
        // switch and page navigation.
        val canEdit = status match
          case AuthStore.Status.Disabled     => true
          case AuthStore.Status.Authed(_, _) => true
          case _                             => false
        val editing = canEdit
        val dirty   = CodeExecutor.isDirty(editor, tab.source)
        val authedUser: Option[UserInfo] = status match
          case AuthStore.Status.Authed(user, _) => Some(user)
          case _                                => None

        val activeStdin = stdinFor(props.spec, s.values.lift(s.activeCase).getOrElse(Map.empty))

        def setEditor(i: Int, f: CodeExecutor.State => CodeExecutor.State): Callback =
          st.modState(prev => prev.copy(editors = prev.editors.updated(i, f(prev.editors(i)))))

        // Anonymous visitors click the locked Edit affordance → sign-in modal. Signed-in users have no
        // Edit button (the editor is already live); they get a Reset that reverts the active tab.
        val onSignInClick: Callback = Callback(AuthStore.openSignIn())

        // Reset reverts the active tab to its starter source — our state AND the live Monaco model,
        // since per-tab models own their content (no controlled `value` to push the revert for us).
        val onResetCode: Callback =
          setEditor(tabIdx, prev => CodeExecutor.setCode(prev, tab.source)) >>
            Callback {
              editorRef.value.foreach { ed =>
                val _ = ed.setValue(tab.source)
              }
            }

        val cancelRunCb: Callback =
          setEditor(tabIdx, CodeExecutor.cancel)

        def runCb: Callback = Callback.suspend {
          val snapshot  = st.value
          val runTab    = snapshot.activeTab
          val caseIdx   = snapshot.activeCase
          val ed        = snapshot.editors(runTab)
          val codeAtRun = ed.code
          val expected  = snapshot.cases.lift(caseIdx).flatMap(_.expected)
          val stdin     = stdinFor(props.spec, snapshot.values.lift(caseIdx).getOrElse(Map.empty))
          val nextEd    = CodeExecutor.started(ed)
          val handle    = nextEd.runId
          st.modState(prev =>
            prev.copy(
              editors = prev.editors.updated(runTab, nextEd),
              verdicts = prev.verdicts - caseIdx,
              // A fresh Run supersedes the last submission's verdict banner.
              submitNote = None
            )
          ) >> Callback {
            val req = RunRequest(props.tabs(runTab).language, codeAtRun, Some(stdin))
            ApiClient.runCode(req).onComplete {
              case scala.util.Success(resp) =>
                st.modState { prev =>
                  val cur = prev.editors(runTab)
                  if cur.runId != handle then prev // cancelled or superseded — drop the late result
                  else
                    val v = verdictFor(resp.result, expected)
                    prev.copy(
                      editors =
                        prev.editors.updated(runTab, CodeExecutor.completed(cur, handle, resp.result)),
                      verdicts = prev.verdicts.updated(caseIdx, v),
                      passed = v match
                        case _: TestCasesPanel.Verdict.Accepted => prev.passed + caseIdx
                        case _                                  => prev.passed
                    )
                }.runNow()
              case scala.util.Failure(err) =>
                st.modState { prev =>
                  val cur = prev.editors(runTab)
                  if cur.runId != handle then prev
                  else
                    prev.copy(
                      editors =
                        prev.editors.updated(runTab, CodeExecutor.failed(cur, handle, err.getMessage)),
                      verdicts = prev.verdicts.updated(
                        caseIdx,
                        TestCasesPanel.Verdict.Errored("Request failed", err.getMessage, "")
                      )
                    )
                }.runNow()
            }
          }
        }

        // ── Submit — run ALL cases server-side and save (allow-listed, problem pages only) ──
        def fireSubmit(token: String, ctx: SubmitContext): Callback = Callback.suspend {
          val snapshot = st.value
          val runTab   = snapshot.activeTab
          val code     = snapshot.editors(runTab).code
          val language = props.tabs(runTab).language
          st.modState(_.copy(submitting = true, submitNote = None)) >> Callback {
            ApiClient
              .submitSolution(token, SubmitRequest(ctx.book, ctx.chapter, language, code))
              .onComplete {
                case scala.util.Success(resp) =>
                  st.modState { prev =>
                    // Mirror the server's per-case verdicts onto the console: chips get their
                    // checks, the active case shows its verdict, custom cases are superseded.
                    val verdicts = resp.results.map { r =>
                      val v: TestCasesPanel.Verdict =
                        if r.passed then TestCasesPanel.Verdict.Accepted(r.time, r.memory)
                        else if r.statusDescription == "Wrong Answer" then
                          TestCasesPanel.Verdict.Wrong(r.stdout)
                        else TestCasesPanel.Verdict.Errored(r.statusDescription, "", "")
                      r.caseIndex -> v
                    }.toMap
                    prev.copy(
                      submitting = false,
                      submitNote = Some(Right(resp)),
                      passed = resp.results.filter(_.passed).map(_.caseIndex).toSet,
                      verdicts = verdicts
                    )
                  }.runNow()
                case scala.util.Failure(err) =>
                  st.modState(
                    _.copy(submitting = false, submitNote = Some(Left(err.getMessage)))
                  ).runNow()
              }
          }
        }

        val onSubmitClick: Callback = props.submitCtx match
          case None => Callback.empty
          case Some(ctx) =>
            status match
              case AuthStore.Status.Authed(_, token) => fireSubmit(token, ctx)
              case AuthStore.Status.Disabled =>
                st.modState(
                  _.copy(submitNote =
                    Some(
                      Left(
                        "Submissions need a signed-in identity, and auth is switched off on this " +
                          "server (AUTH_ENABLED=false). Start dev with auth on to try the full flow."
                      )
                    )
                  )
                )
              case _ => Callback(AuthStore.openSignIn())

        val onKeyDown: ReactKeyboardEvent => Callback = (e: ReactKeyboardEvent) =>
          if (e.metaKey || e.ctrlKey) && e.key == "Enter" then e.preventDefaultCB >> runCb
          else if e.altKey && tab.viz.isDefined && (e.key.equalsIgnoreCase("v") || e.key == "√") then
            e.preventDefaultCB >> vizOpenS.setState(true)
          else Callback.empty

        // ── Splitter drag — registers document-level listeners for the gesture's lifetime ──
        def startSplitDrag(e: ReactMouseEvent): Callback = Callback {
          e.preventDefault()
          dom.document.body.style.cursor = "row-resize"
          dom.document.body.style.setProperty("user-select", "none")
          val (lo, hi) = props.pctClamp
          val move: js.Function1[dom.MouseEvent, Unit] = ev =>
            cardRef.get.runNow().foreach { card =>
              val rect = card.getBoundingClientRect()
              if rect.height > 0 then
                val pct = (ev.clientY - rect.top) / rect.height * 100.0
                st.modState(_.copy(codePct = math.min(hi, math.max(lo, pct)))).runNow()
            }
          lazy val up: js.Function1[dom.MouseEvent, Unit] = _ => {
            dom.document.removeEventListener("mousemove", move)
            dom.document.removeEventListener("mouseup", up)
            dom.document.body.style.cursor = ""
            val _ = dom.document.body.style.removeProperty("user-select")
          }
          dom.document.addEventListener("mousemove", move)
          dom.document.addEventListener("mouseup", up)
        }

        // ── Toolbar ──
        val editControl: VdomNode =
          if !tab.runnable then EmptyVdom
          else if !canEdit then
            <.button(
              ^.tpe   := "button",
              ^.title := "Sign in with GitHub to edit",
              ^.onClick --> onSignInClick,
              ^.className := "wb__btn wb__btn--locked",
              <.span(^.className := "wb__lock", LucideIcons.Lock(LucideIcons.withClass("wb__lock-icon"))),
              LucideIcons.Pencil(LucideIcons.withClass("wb__btn-icon")),
              "Edit"
            )
          else if dirty then
            <.button(
              ^.tpe   := "button",
              ^.title := "Reset to the starter code",
              ^.onClick --> onResetCode,
              ^.className := "wb__btn",
              LucideIcons.RotateCcw(LucideIcons.withClass("wb__btn-icon")),
              "Reset"
            )
          else EmptyVdom

        val vizControl: VdomNode =
          if tab.viz.isDefined && tab.runnable then
            <.button(
              ^.tpe   := "button",
              ^.title := "Visualise (⌥V)",
              ^.onClick --> vizOpenS.setState(true),
              ^.aria.label := "Visualise code",
              ^.className  := "wb__btn wb__btn--viz",
              LucideIcons.Network(LucideIcons.withClass("wb__btn-icon")),
              "Visualise"
            )
          else EmptyVdom

        val runControl: VdomNode =
          if !tab.runnable then EmptyVdom
          else if running then
            <.button(
              ^.tpe := "button",
              ^.onClick --> cancelRunCb,
              ^.className := "wb__btn wb__btn--cancel",
              LucideIcons.Square(LucideIcons.withClass("wb__btn-icon")),
              "Cancel"
            )
          else
            <.button(
              ^.tpe   := "button",
              ^.title := "Run against the active case (⌘+Enter)",
              ^.onClick --> runCb,
              ^.className := "wb__btn wb__btn--run",
              if running then LucideIcons.Loader2(LucideIcons.withClass("wb__btn-icon"))
              else LucideIcons.Play(LucideIcons.withClass("wb__btn-icon")),
              "Run"
            )

        val identityNode: VdomNode =
          authedUser match
            case Some(user) =>
              IdentityChip.Component(
                IdentityChip.Props(user.preferredUsername, Callback(AuthStore.signOut()))
              )
            case None => EmptyVdom

        // Submit button + always-on hover tooltip describing the allow-list / access-request story.
        val submitControl: VdomNode =
          props.submitCtx match
            case None => EmptyVdom
            case Some(_) =>
              <.span(
                ^.className := "wb-submit",
                <.button(
                  ^.tpe      := "button",
                  ^.disabled := s.submitting,
                  ^.onClick --> onSubmitClick,
                  ^.className := "wb__btn wb__btn--submit",
                  if s.submitting then
                    LucideIcons.Loader2(LucideIcons.withClass("wb__btn-icon wb-submit__spin"))
                  else LucideIcons.Send(LucideIcons.withClass("wb__btn-icon")),
                  if s.submitting then "Submitting…" else "Submit"
                ),
                <.div(
                  ^.className := "wb-submit__tip",
                  ^.role      := "tooltip",
                  <.p(
                    ^.className := "wb-submit__tip-head",
                    "Submit runs your code against every test case on the server and saves the attempt."
                  ),
                  <.p(
                    "Saving is ",
                    <.strong("allow-listed"),
                    ": this is a personal homelab deployment for learning and experimentation — not a " +
                      "hosted service — and stored data carries no durability guarantee."
                  ),
                  <.p(
                    "To request access, email ",
                    <.strong("cortex.kakde.eu@gmail.com"),
                    " with your GitHub username; grants are made selectively. Prefer your own instance? " +
                      "Self-hosting instructions live in the cortex GitHub repository."
                  ),
                  <.p(
                    "You can erase your saved submissions at any time (",
                    <.code("DELETE /api/submissions"),
                    ")."
                  )
                )
              )

        val toolbar =
          <.div(
            ^.className := "wb__toolbar",
            <.div(
              ^.className := "wb__toolbar-title",
              <.span(
                ^.className := "wb__toolbar-icon",
                LucideIcons.Terminal(LucideIcons.withClass("wb__toolbar-icon-svg"))
              ),
              <.span(^.className := "wb__eyebrow", "Code")
            ),
            <.div(
              ^.className := "wb__toolbar-actions",
              identityNode,
              LangSelect.Component(
                LangSelect.Props(
                  tabs = props.tabs,
                  activeIdx = tabIdx,
                  onChange = i => st.modState(_.copy(activeTab = i, submitNote = None))
                )
              ),
              editControl,
              vizControl,
              runControl,
              submitControl
            )
          )

        // ── Editor surface ──
        val monacoView: VdomNode =
          val opts: js.Object = js.Dynamic
            .literal(
              readOnly = !editing,
              domReadOnly = !editing,
              padding = js.Dynamic.literal(top = 14, bottom = 14),
              fontFamily =
                "\"JetBrains Mono\", ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace",
              fontSize = 13,
              lineHeight = 22,
              tabSize = 4,
              insertSpaces = true,
              minimap = js.Dynamic.literal(enabled = false),
              scrollBeyondLastLine = false,
              wordWrap = "off",
              automaticLayout = true,
              renderLineHighlight = if editing then "line" else "none",
              scrollbar = js.Dynamic.literal(alwaysConsumeMouseWheel = false),
              fixedOverflowWidgets = true,
              cursorStyle = "line",
              renderValidationDecorations = "off"
            )
            .asInstanceOf[js.Object]

          val mProps = (new js.Object).asInstanceOf[MonacoEditor.EditorProps]
          // One Monaco MODEL per language tab (`path` mode). Swapping a single model's `value` on
          // tab change races the wrapper's change events and can write one tab's source into the
          // other tab's state — the language-switch corruption bug. With per-tab models, content,
          // undo history and cursor survive switches, and onChange only ever fires for the model
          // the user is actually typing in.
          mProps.path = s"${wbIdRef.value}/tab-$tabIdx.${extFor(tab.language)}"
          mProps.defaultValue = tab.source
          mProps.defaultLanguage = monacoLangFor(tab.language)
          mProps.saveViewState = true
          mProps.theme = "cortex-dark"
          // Explicit pixel height driven by the wrapper's observed rect — "100%" collapses to 0 on
          // first layout (see RunnableCodeBlock's identical trick). Here the wrapper's height is a
          // definite percentage of the card, so we read it instead of the content size.
          mProps.height = monacoHeightS.value
          mProps.options = opts
          mProps.loading = "Loading editor…"
          mProps.onChange = (next, _) =>
            val v = next.getOrElse("")
            st.modState(prev =>
              prev.copy(
                editors = prev.editors.updated(tabIdx, CodeExecutor.setCode(prev.editors(tabIdx), v)),
                // Editing the code invalidates the last submission verdict banner.
                submitNote = None
              )
            ).runNow()
          mProps.onMount = (ed, _) =>
            editorRef.value = Some(ed)
            monacoWrapRef.get.runNow().foreach { wrap =>
              val relayout: () => Unit = () =>
                val rect = wrap.getBoundingClientRect()
                if rect.width > 0 && rect.height > 0 then
                  monacoHeightS.setState(rect.height.toInt).runNow()
                  val _ = ed.layout(js.Dynamic.literal(width = rect.width, height = rect.height))
              val resizeCb: js.Function2[js.Array[dom.ResizeObserverEntry], dom.ResizeObserver, Unit] =
                (_, _) => relayout()
              val resizeObs = new dom.ResizeObserver(resizeCb)
              resizeObs.observe(wrap)
              // `<details>` toggle recovery — inline workbench cards can sit inside collapsibles
              // whose closed state reports a 0×0 rect at mount time (see RunnableCodeBlock).
              val ancestorDetails = wrap.closest("details")
              if ancestorDetails != null then
                val onToggle: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
                  if ancestorDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean] then relayout()
                ancestorDetails.addEventListener("toggle", onToggle)
              relayout()
            }

          <.div.withRef(monacoWrapRef)(
            ^.className := "wb__editor not-prose",
            ^.onKeyDown ==> onKeyDown,
            MonacoEditor.Component(mProps)
          )

        // Display-only tabs (pseudocode) keep the Prism-highlighted static block.
        val editorSurface: VdomNode =
          if tab.runnable then monacoView
          else
            <.pre(
              ^.className               := "wb__editor wb__editor--static not-prose",
              ^.dangerouslySetInnerHtml := s"<code>${highlightWithPrism(tab.source, tab.language)}</code>"
            )

        val editBar: VdomNode =
          if editing then
            val changed = CodeExecutor.changedLineCount(editor, tab.source)
            <.div(
              ^.className := "wb__edit-bar",
              if changed > 0 then
                <.span(
                  ^.className := "wb__edit-bar-dirty",
                  s"● $changed ${if changed == 1 then "line" else "lines"} changed"
                ): VdomNode
              else EmptyVdom,
              <.span(^.className := "wb__edit-bar-spacer"),
              <.span(
                ^.className := "wb__edit-bar-hint",
                <.kbd("⌘"),
                " ",
                <.kbd("↵"),
                " Run · session limited to 30s, 64MB"
              )
            )
          else EmptyVdom

        val quotaNotice: VdomNode =
          authedUser.map(_.quota).filter(quotaRunningLow) match
            case Some(q) =>
              <.div(
                ^.className := "quota-notice",
                ^.role      := "status",
                <.span(
                  ^.className := "quota-notice__icon",
                  LucideIcons.AlertTriangle(LucideIcons.withClass("quota-notice__icon-svg"))
                ),
                <.div(
                  <.div(^.className := "quota-notice__title", "Sandbox quota"),
                  <.div(
                    ^.className := "quota-notice__body",
                    "You've used ",
                    <.span(^.className := "quota-notice__figure", s"${q.used} / ${q.limit}"),
                    " runs this hour. Quota resets at ",
                    <.span(^.className := "quota-notice__figure", s"${resetClockUtc(q.resetEpochMs)} UTC"),
                    ". Anonymous read-only runs are unaffected."
                  )
                )
              )
            case None => EmptyVdom

        val vizModal: VdomNode =
          tab.viz match
            case Some(hint) =>
              VisualiseModal.Component(
                VisualiseModal.Props(
                  isOpen = vizOpenS.value,
                  onClose = vizOpenS.setState(false),
                  language = tab.language,
                  source = editor.code,
                  vizHint = hint,
                  vizRoot = tab.vizRoot,
                  vizCase = tab.vizCase,
                  vizKind = tab.vizKind,
                  title = "Code visualisation",
                  initialStdin = Some(activeStdin)
                )
              )
            case None => EmptyVdom

        val panel = TestCasesPanel.Component(
          TestCasesPanel.Props(
            spec = props.spec,
            cases = s.cases,
            values = s.values,
            activeIdx = s.activeCase,
            passed = s.passed,
            running = running,
            verdict = s.verdicts.get(s.activeCase),
            onSelect = i => st.modState(_.copy(activeCase = i)),
            // Custom test cases are an authenticated feature (same gate as Edit, ADR-0013):
            // anonymous visitors get the sign-in modal instead of a new case.
            onAdd =
              if canEdit then
                st.modState { prev =>
                  val blank = props.spec.args.map(a => a.id -> "").toMap
                  prev.copy(
                    cases = prev.cases :+ Block.TestCase(blank, None),
                    values = prev.values :+ blank,
                    activeCase = prev.cases.length
                  )
                }
              else Callback(AuthStore.openSignIn()),
            addLocked = !canEdit,
            customFrom = props.spec.cases.length,
            onReset = st.modState(prev =>
              prev.copy(
                cases = props.spec.cases.toVector,
                values = props.spec.cases.map(_.args).toVector,
                activeCase = 0,
                passed = Set.empty,
                verdicts = Map.empty
              )
            ),
            onArgChange = (argId, v) =>
              st.modState { prev =>
                val idx = prev.activeCase
                prev.copy(
                  values = prev.values.updated(idx, prev.values(idx) + (argId -> v)),
                  // Edited inputs invalidate that case's earlier verdict — an honest chip beats a stale ✓.
                  passed = prev.passed - idx,
                  verdicts = prev.verdicts - idx
                )
              }
          )
        )

        val submitBanner: VdomNode = s.submitNote match
          case None => EmptyVdom
          case Some(Left(msg)) =>
            <.div(
              ^.className := "wb-submit__banner wb-submit__banner--err",
              LucideIcons.AlertTriangle(LucideIcons.withClass("wb-submit__banner-icon")),
              <.span(msg)
            )
          case Some(Right(resp)) =>
            val total   = resp.results.length
            val passedN = resp.results.count(_.passed)
            val savedSuffix =
              resp.submissionId.map(id => s" · saved as submission #$id").getOrElse {
                if resp.saved then " · saved" else ""
              }
            if resp.accepted then
              <.div(
                ^.className := "wb-submit__banner wb-submit__banner--ok",
                LucideIcons.CircleCheck(LucideIcons.withClass("wb-submit__banner-icon")),
                <.span(s"Accepted — all $total cases passed$savedSuffix")
              )
            else
              <.div(
                ^.className := "wb-submit__banner wb-submit__banner--warn",
                LucideIcons.AlertTriangle(LucideIcons.withClass("wb-submit__banner-icon")),
                <.span(s"$passedN of $total cases passed$savedSuffix — check the failing chips below")
              )

        val cardStyle = props.fixedHeightPx match
          case Some(px) => js.Dynamic.literal(height = s"${px}px").asInstanceOf[js.Object]
          case None     => js.Dynamic.literal(height = "100%").asInstanceOf[js.Object]

        React.Fragment(
          <.div.withRef(cardRef)(
            ^.className := (if props.fixedHeightPx.isDefined then "wb" else "wb wb--fill"),
            ^.style     := cardStyle,
            <.div(
              ^.className := "wb__code-pane",
              ^.style     := js.Dynamic.literal(height = s"${s.codePct}%").asInstanceOf[js.Object],
              toolbar,
              editorSurface,
              editBar
            ),
            <.div(
              ^.className := "wb-split wb-split--h",
              ^.onMouseDown ==> startSplitDrag,
              <.div(^.className := "wb-split__line"),
              <.div(
                ^.className := "wb-split__grip",
                <.span(^.className := "wb-split__dot"),
                <.span(^.className := "wb-split__dot"),
                <.span(^.className := "wb-split__dot")
              )
            ),
            submitBanner,
            panel
          ),
          quotaNotice,
          vizModal
        )
      }
