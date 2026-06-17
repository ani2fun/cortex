package cortex.client.components.book.workbench

import cortex.client.api.{ByokProvider, TutorApiClient}
import cortex.client.auth.{AuthStore, ByokKeyStore}
import cortex.shared.api.Endpoints.RunResult
import cortex.shared.tutor.TutorContract.*
import cortex.shared.tutor.ModelPicker
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

/**
 * The headless **CoachController** — the client half of the six-step tutor FSM, rendered render-prop so the
 * problem-page Coach tab (and, later, a standalone-coach shell) share one controller.
 *
 * Lifecycle (lazy pick-then-start — the tutor pins the coach model immutably at session creation):
 *   1. on mount, `GET /v1/whoami` → the caller's tier + the models they may pick; default the picker to
 *      `defaultModel`. **No session is created yet** — the learner picks a model first. 2. the first
 *      [[View.submit]] creates the session with the selected model (HTTP 422 ⇒ "pick another"; 403 ⇒
 *      tier-locked; 401 ⇒ sign-in), then runs the turn. A returning learner whose session already exists is
 *      *resumed* (its pinned model wins) instead of submitting the typed answer for the wrong step. 3. once a
 *      session exists the model is fixed ([[View.modelLocked]]); the picker shows it read-only.
 *
 * Turn transport stays server-authoritative off [[CoachSession.byok]]: BYOK ⇒ `getPromptBundle` →
 * client-direct Anthropic on the visitor's own key → `recordByokTurn`; homelab ⇒ `submitTurn` SSE.
 */
object CoachController:

  /** Where the controller is in its lifecycle (distinct from the *server* step in [[CoachSession]]). */
  enum Stage:
    case Connecting                   // whoami in flight
    case NeedsSignIn                  // anonymous, or 401 (expired JWT)
    case Coaching                     // whoami resolved — picker + composer live (session optional)
    case Locked                       // 403 — coaching not permitted for this tier
    case Unavailable(message: String) // whoami/tutor unreachable — host degrades to a static fallback

  /** The server steps that embed a code editor — their turns carry the editor snapshot. */
  def isCodeStep(s: Step): Boolean = s == Step.Implement || s == Step.Test

  /**
   * The host's live editor state, pulled at submit time for implement/test turns. `runResult` is already a
   * textual summary (see [[summariseRun]]); the controller stays free of the workbench's `RunResult`
   * plumbing.
   */
  final case class EditorSnapshot(code: String, language: String, runResult: Option[String])

  final case class Props(
      problemId: String,
      // Distinguishes the coaching surface to the tutor: problem pages vs an inline "Your Turn".
      origin: SessionOrigin = SessionOrigin.YourTurn,
      /** Pulls the right-pane editor's latest snapshot for code steps; `None` for composer-only shells. */
      snapshot: () => Option[EditorSnapshot] = () => None,
      render: View => VdomNode
  )

  /** Everything a shell needs to render a coached session. */
  final case class View(
      stage: Stage,
      session: Option[CoachSession],
      tier: Option[Tier],
      models: List[ModelOption],
      selectedModel: Option[ModelOption],
      // True once a session exists — the coach model is pinned server-side, so the picker is read-only.
      modelLocked: Boolean,
      setModel: String => Callback,
      busy: Boolean,
      streaming: String,
      lastResult: Option[TurnResult],
      // Live per-step progress: the gate score for the CURRENT step's latest attempt (None until attempted).
      currentStepProgress: Option[Int],
      // Consecutive retries on the current step (client-only "stuck" detector; 0 on a fresh step).
      localRetries: Int,
      error: Option[String],
      submit: (Step, String) => Callback,
      reset: Callback,
      // True when the selected model needs the visitor's own Anthropic key (BYOK tier + Anthropic model).
      needsKey: Boolean,
      hasByokKey: Boolean,
      keySaving: Boolean,
      saveByokKey: String => Callback,
      forgetByokKey: Callback
  )

  private given ExecutionContext = JSExecutionContext.queue

  /** Client-generated idempotency key for a turn (secure-context `crypto.randomUUID`, else a v4 shim). */
  private def newTurnId(): String =
    val crypto = js.Dynamic.global.crypto
    if !js.isUndefined(crypto) && !js.isUndefined(crypto.randomUUID) then
      crypto.randomUUID().asInstanceOf[String]
    else
      val hex = "0123456789abcdef"
      (0 until 36).map {
        case 8 | 13 | 18 | 23 => '-'
        case 14               => '4'
        case 19               => hex((js.Math.random() * 4).toInt + 8)
        case _                => hex((js.Math.random() * 16).toInt)
      }.mkString

  private val RunSummaryMax = 4000

  /**
   * Textual, bounded summary of a run for the coach (the prompt assembly doesn't need megabytes of stdout).
   */
  def summariseRun(r: RunResult): String =
    val full = List(
      Some(s"status: ${r.statusDescription}"),
      Option(r.compileOutput).filter(_.nonEmpty).map(c => s"compile output:\n$c"),
      Option(r.stderr).filter(_.nonEmpty).map(e => s"stderr:\n$e"),
      Option(r.stdout).filter(_.nonEmpty).map(o => s"stdout:\n$o")
    ).flatten.mkString("\n")
    if full.length <= RunSummaryMax then full else full.take(RunSummaryMax) + "\n…(truncated)"

  /**
   * Fold the editor snapshot into the message the BYOK model judges — MIRRORS the tutor's server-side
   * `gate.compose_answer` for homelab turns. Keep the `[workbench …]` / `[run result …]` markers in lockstep
   * with the step guides.
   */
  private def composeByokAnswer(step: Step, text: String, snap: Option[EditorSnapshot]): String =
    if !isCodeStep(step) then text
    else
      snap.filter(_.code.trim.nonEmpty) match
        case Some(s) =>
          val lang = s.language.trim.toLowerCase
          val run = s.runResult match
            case Some(r) => s"[run result]\n$r"
            case None    => "[run result: none]"
          s"$text\n\n[workbench snapshot — $lang]\n```$lang\n${s.code}\n```\n\n$run"
        case None =>
          s"$text\n\n[workbench: no code attached to this message]"

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(Option.empty[Whoami])       // whoami (tier + selectable models)
      .useState(Option.empty[String])       // selected model key (None until whoami resolves)
      .useState(Option.empty[CoachSession]) // authoritative server state (None until first turn)
      .useState[Stage](Stage.Connecting)    // controller lifecycle
      .useState("")                         // in-flight streamed coach reply
      .useState(false)                      // a turn (or session create) is in flight
      .useState(Option.empty[TurnResult])   // last committed turn (score/hint/verdict)
      .useState(Option.empty[String])       // transient turn/create error
      .useState(ByokKeyStore.all)           // visitor's own BYOK keys, per provider (BYOK tier)
      .useState(false)                      // key validation probe in flight
      .useState(0)                          // localRetries — client-only "stuck" counter (current step)
      .useRef(Option.empty[TutorApiClient.TurnStream]) // in-flight stream handle (cancel on unmount)
      .useEffectOnMountBy {
        (props, whoamiS, selectedKeyS, sessionS, stageS, _, _, _, _, _, _, _, streamRef) =>
          CallbackTo {
            AuthStore.current.status match
              case AuthStore.Status.Anonymous =>
                stageS.setState(Stage.NeedsSignIn).runNow()
              case _ =>
                TutorApiClient.whoami().onComplete {
                  case Success(w) =>
                    (whoamiS.setState(Some(w)) >>
                      selectedKeyS.setState(ModelPicker.resolveDefault(w).map(_.key)) >>
                      stageS.setState(Stage.Coaching)).runNow()
                    // Restore an in-progress session so a refresh CONTINUES the journey (never resets);
                    // a never-started problem (204) leaves it None → lazy-create on the first submit.
                    TutorApiClient.getActiveSession(props.problemId).onComplete {
                      case Success(Some(session)) =>
                        val pick = session.model.fold(Callback.empty)(m => selectedKeyS.setState(Some(m)))
                        (sessionS.setState(Some(session)) >> pick).runNow()
                      case _ => () // none, or a transient read error — fall back to lazy create
                    }
                  case Failure(e: TutorApiClient.TutorHttpError) if e.status == 401 =>
                    stageS.setState(Stage.NeedsSignIn).runNow()
                  case Failure(e) =>
                    stageS.setState(Stage.Unavailable(e.getMessage)).runNow()
                }
            // Cleanup: abort any in-flight stream so its callbacks don't touch an unmounted host.
            Callback(streamRef.value.foreach(_.cancel()))
          }
      }
      // Reset the local "stuck" retry counter whenever the server step advances.
      .useEffectWithDepsBy((_, _, _, sessionS, _, _, _, _, _, _, _, _, _) =>
        sessionS.value.map(_.stepIndex).getOrElse(0)
      ) { (_, _, _, _, _, _, _, _, _, _, _, retriesS, _) => _ =>
        retriesS.setState(0)
      }
      .render {
        (
            props,
            whoamiS,
            selectedKeyS,
            sessionS,
            stageS,
            streamingS,
            busyS,
            resultS,
            errorS,
            byokKeyS,
            keySavingS,
            retriesS,
            streamRef
        ) =>

          val tier          = whoamiS.value.map(_.tier)
          val models        = whoamiS.value.map(_.availableModels).getOrElse(Nil)
          val selectedModel = selectedKeyS.value.flatMap(k => models.find(_.key == k))
          // A cloud model needs the visitor's own key whoever picks it (external BYOK user OR the
          // operator picking cloud over their local model — dual-mode). The local model needs none.
          val needsKey = selectedModel.exists(ModelPicker.requiresKey)

          // Commit a turn result; a Retry on the current step bumps the local "stuck" counter (Pass
          // advances + the step-change effect resets it; Question/OffTopic don't count).
          def applyResult(r: TurnResult): Callback =
            resultS.setState(Some(r)) >>
              (if r.verdict == Verdict.Retry then retriesS.modState(_ + 1) else Callback.empty)

          // ── run one turn against an existing session (transport decided by session.byok) ──
          def doTurn(s: CoachSession, step: Step, text: String): Callback =
            val snapshot   = if isCodeStep(step) then props.snapshot() else None
            val optimistic = CoachMessage(Role.User, step, text, 0L)
            val seeded     = s.copy(messages = s.messages :+ optimistic)
            val prelude =
              busyS.setState(true) >> streamingS.setState("") >> resultS.setState(None) >>
                errorS.setState(None) >> sessionS.setState(Some(seeded))

            if s.byok.contains(true) then
              // Route the client-direct call to the adapter for the session's pinned model's provider
              // (OpenRouter — the primary path — or Anthropic-direct). The key never reaches our server.
              val providerName = s.model
                .flatMap(k => models.find(_.key == k))
                .orElse(selectedModel)
                .map(_.provider)
                .getOrElse("openrouter") // primary BYOK path; matches saveByokKey's fallback default
              val byokProvider = ByokProvider.forProvider(providerName)
              byokKeyS.value.get(providerName) match
                case None =>
                  errorS.setState(Some("Add your API key below to use the coach."))
                case Some(key) =>
                  val turnId = newTurnId()
                  prelude >> Callback {
                    val flow =
                      for
                        bundle <- TutorApiClient.getPromptBundle(s.sessionId, step)
                        turn   <- byokProvider.runTurn(key, bundle, composeByokAnswer(step, text, snapshot))
                        result <- TutorApiClient.recordByokTurn(
                          s.sessionId,
                          ByokRecordRequest(
                            step = step,
                            text = text,
                            coachReply = turn.coachReply,
                            verdict = turn.verdict,
                            turnId = Some(turnId),
                            code = snapshot.map(_.code),
                            language = snapshot.map(_.language),
                            runResult = snapshot.flatMap(_.runResult)
                          )
                        )
                        // The turn is committed server-side the moment `recordByokTurn` succeeds. Don't let a
                        // failed refresh discard it (which would re-run the provider call — re-spending the
                        // user's key — on the next submit): fall back to the optimistic session + the coach
                        // reply, which the next successful load reconciles against the server. The fallback
                        // keeps the pre-turn step; a verdict that advanced it reconciles on that next load.
                        fresh <- TutorApiClient
                          .getSession(s.sessionId)
                          .recover { case _ => seeded.copy(messages = seeded.messages :+ result.reply) }
                      yield (result, fresh)
                    flow.onComplete {
                      case Success((result, fresh)) =>
                        (sessionS.setState(Some(fresh)) >> applyResult(result) >>
                          busyS.setState(false)).runNow()
                      case Failure(e) =>
                        (errorS.setState(Some(e.getMessage)) >> busyS.setState(false)).runNow()
                    }
                  }
            else
              val request = TurnRequest(
                step = step,
                text = text,
                turnId = Some(newTurnId()),
                code = snapshot.map(_.code),
                language = snapshot.map(_.language),
                runResult = snapshot.flatMap(_.runResult)
              )
              prelude >> Callback {
                val handle = TutorApiClient.submitTurn(
                  s.sessionId,
                  request,
                  onState = st => sessionS.setState(Some(st)).runNow(),
                  onToken = t => streamingS.modState(_ + t).runNow(),
                  onDone = r =>
                    (sessionS.modState(_.map(cur => cur.copy(messages = cur.messages :+ r.reply))) >>
                      applyResult(r) >> streamingS.setState("") >>
                      busyS.setState(false)).runNow(),
                  onError = {
                    case TutorApiClient.TurnFailure.Stale(st) =>
                      (sessionS.setState(Some(st)) >> streamingS.setState("") >>
                        busyS.setState(false)).runNow()
                    case TutorApiClient.TurnFailure.Message(m) =>
                      (errorS.setState(Some(m)) >> streamingS.setState("") >>
                        busyS.setState(false)).runNow()
                  }
                )
                streamRef.value = Some(handle)
              }

          // ── first submit: create the session with the chosen model, then turn (or resume) ──
          def createThenTurn(step: Step, text: String): Callback =
            selectedKeyS.value match
              case None =>
                errorS.setState(Some("Pick a coach model to start."))
              case Some(key) =>
                busyS.setState(true) >> errorS.setState(None) >> Callback {
                  TutorApiClient.createSession(props.problemId, props.origin, Some(key)).onComplete {
                    case Success(session) =>
                      // Lock the picker to the server-pinned model (may differ from the pick on resume).
                      session.model.foreach(m => selectedKeyS.setState(Some(m)).runNow())
                      if session.messages.isEmpty && !session.completed then
                        // Fresh session — submit the typed answer for its (clarify) current step.
                        doTurn(session, session.currentStep, text).runNow()
                      else
                        // Resumed an in-progress/completed session — load it; the learner answers its
                        // real current step rather than mis-submitting this text for the wrong step.
                        (sessionS.setState(Some(session)) >> busyS.setState(false)).runNow()
                    case Failure(e: TutorApiClient.TutorHttpError) if e.status == 422 =>
                      (errorS.setState(Some("That model isn't available — pick another.")) >>
                        busyS.setState(false)).runNow()
                    case Failure(e: TutorApiClient.TutorHttpError) if e.status == 401 =>
                      stageS.setState(Stage.NeedsSignIn).runNow()
                    case Failure(e: TutorApiClient.TutorHttpError) if e.status == 403 =>
                      stageS.setState(Stage.Locked).runNow()
                    case Failure(e) =>
                      (errorS.setState(Some(e.getMessage)) >> busyS.setState(false)).runNow()
                  }
                }

          def submit(step: Step, answer: String): Callback = Callback.suspend {
            val text = answer.trim
            if text.isEmpty || busyS.value then Callback.empty
            else
              sessionS.value match
                case Some(s) => doTurn(s, step, text)
                case None    => createThenTurn(step, text)
          }

          // Pre-session, switching just updates the pick. With a session, re-point it server-side
          // (dual-mode: the new model's provider re-derives the transport) and adopt the fresh state;
          // a tier-disallowed model (422) reverts the picker to the session's current model.
          //
          // The session branch holds `busyS` for the whole re-point: `submit` guards on `busyS`, so a turn
          // can't fire against the pre-switch session/transport while the switch is in flight (their
          // `sessionS` writes would otherwise race last-write-wins). A switch is likewise ignored while a
          // turn — or an earlier switch — is still running.
          def setModel(key: String): Callback = Callback.suspend {
            sessionS.value match
              case None                   => selectedKeyS.setState(Some(key)) >> errorS.setState(None)
              case Some(_) if busyS.value => Callback.empty
              case Some(s) =>
                selectedKeyS.setState(Some(key)) >> errorS.setState(None) >> busyS.setState(
                  true
                ) >> Callback {
                  TutorApiClient.changeModel(s.sessionId, key).onComplete {
                    case Success(fresh) =>
                      (sessionS.setState(Some(fresh)) >>
                        fresh.model.fold(Callback.empty)(m => selectedKeyS.setState(Some(m))) >>
                        busyS.setState(false)).runNow()
                    case Failure(e: TutorApiClient.TutorHttpError) if e.status == 422 =>
                      (selectedKeyS.setState(s.model) >>
                        errorS.setState(Some("That model isn't available — pick another.")) >>
                        busyS.setState(false)).runNow()
                    case Failure(e) =>
                      (selectedKeyS.setState(s.model) >> errorS.setState(Some(e.getMessage)) >>
                        busyS.setState(false)).runNow()
                  }
                }
          }

          // ── store the visitor's key after a validation probe (sessionStorage; this tab only) ──
          def saveByokKey(raw: String): Callback = Callback.suspend {
            val key = raw.trim
            if key.isEmpty || keySavingS.value then Callback.empty
            else
              // Validate against the selected model's provider (OpenRouter by default — the primary path).
              val providerName = selectedModel.map(_.provider).getOrElse("openrouter")
              val provider     = ByokProvider.forProvider(providerName)
              keySavingS.setState(true) >> errorS.setState(None) >> Callback {
                provider.probe(key).onComplete {
                  case Success(true) =>
                    ByokKeyStore.set(providerName, key)
                    (byokKeyS.modState(_ + (providerName -> key)) >> keySavingS.setState(false)).runNow()
                  case Success(false) =>
                    (errorS.setState(Some("That key was rejected — check it and try again.")) >>
                      keySavingS.setState(false)).runNow()
                  case Failure(e) =>
                    (errorS.setState(Some(s"Couldn't validate the key (${e.getMessage}).")) >>
                      keySavingS.setState(false)).runNow()
                }
              }
          }

          val forgetByokKey: Callback =
            selectedModel.map(_.provider) match
              case Some(p) => Callback(ByokKeyStore.clear(p)) >> byokKeyS.modState(_ - p)
              case None    => Callback.empty

          // "Start over" is now a permanent hard delete (this problem's history) — confirm first.
          val doReset: Callback =
            sessionS.value match
              case None => Callback.empty
              case Some(s) =>
                CallbackTo(
                  dom.window.confirm("Delete this conversation and start over? This can't be undone.")
                ).flatMap { ok =>
                  if !ok then Callback.empty
                  else
                    (resultS.setState(None) >> streamingS.setState("") >> busyS.setState(false) >>
                      errorS.setState(None)) >> Callback {
                      TutorApiClient.resetSession(s.sessionId).onComplete {
                        case Success(fresh) => sessionS.setState(Some(fresh)).runNow()
                        case Failure(e)     => errorS.setState(Some(e.getMessage)).runNow()
                      }
                    }
                }

          val currentStepOpt = sessionS.value.map(_.currentStep)
          val liveScore: Option[Int] =
            resultS.value.filter(r => currentStepOpt.contains(r.step)).flatMap(_.score)

          props.render(
            View(
              stage = stageS.value,
              session = sessionS.value,
              tier = tier,
              models = models,
              selectedModel = selectedModel,
              modelLocked = false, // dual-mode: the picker stays switchable for the session's life
              setModel = setModel,
              busy = busyS.value,
              streaming = streamingS.value,
              lastResult = resultS.value,
              currentStepProgress = liveScore,
              localRetries = retriesS.value,
              error = errorS.value,
              submit = submit,
              reset = doReset,
              needsKey = needsKey,
              hasByokKey = selectedModel.exists(m => byokKeyS.value.contains(m.provider)),
              keySaving = keySavingS.value,
              saveByokKey = saveByokKey,
              forgetByokKey = forgetByokKey
            )
          )
      }
