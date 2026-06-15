package cortex.client.components.book.workbench

import cortex.client.auth.AuthStore
import cortex.client.components.icons.LucideIcons
import cortex.shared.tutor.TutorContract.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * The Coach tab of the problem workbench. When the host supplies a coach `problemId` (problem pages and DSA
 * "Your Turn"), it drives the live [[CoachController]] — the six-step Socratic interview against the
 * standalone cortex-tutor, with a header **model picker** ([[ModelSelect]]) and the visitor's BYOK key card
 * when the selected model needs one. Without a `problemId` (generic-dispatch fallback), or when the tutor is
 * unreachable / the tier isn't permitted, it degrades to the static manual prompts ([[StaticCoach]]).
 *
 * `whoami` is deferred until the tab is first opened (`active`), so a problem page that's never coached pays
 * no tutor round-trip.
 */
object CoachTab:

  final case class Props(
      coachProblemId: Option[String],
      origin: SessionOrigin = SessionOrigin.YourTurn,
      active: Boolean,
      snapshot: () => Option[WorkbenchEditor.Snapshot]
  )

  // ── server-step display metadata (the SERVER's six steps; the design mock labels differ) ──────────

  private def stepLabel(s: Step): String = s match
    case Step.Clarify   => "Clarify"
    case Step.Examples  => "Examples"
    case Step.Approach  => "Approach"
    case Step.Plan      => "Plan"
    case Step.Implement => "Implement"
    case Step.Test      => "Test"

  private def stepVerb(s: Step): String = s match
    case Step.Clarify   => "Restate & clarify"
    case Step.Examples  => "Work through examples"
    case Step.Approach  => "Choose an approach"
    case Step.Plan      => "Plan the solution"
    case Step.Implement => "Write your solution"
    case Step.Test      => "Test & debug"

  private def stepBlurb(s: Step): String = s match
    case Step.Clarify   => "Restate the problem in your own words and surface assumptions and edge cases."
    case Step.Examples  => "Work through concrete examples to pin down the expected behaviour."
    case Step.Approach  => "Brainstorm strategies and weigh their tradeoffs, then commit to one."
    case Step.Plan      => "Commit to a concrete plan before writing code."
    case Step.Implement => "Write the solution in the editor on the right, running it against the cases."
    case Step.Test      => "Run against tricky cases and debug any failures."

  private def composerHint(s: Step): String = s match
    case Step.Clarify   => "Restate the problem in your own words…"
    case Step.Examples  => "Give an example input and its expected output…"
    case Step.Approach  => "Describe your approach and its complexity…"
    case Step.Plan      => "Outline your step-by-step plan…"
    case Step.Implement => "Explain your implementation, then submit…"
    case Step.Test      => "Describe the cases you ran and what you found…"

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false) // everActive — defer whoami until the tab is first opened
      .useState("")    // composer draft (presentation state)
      .useEffectWithDepsBy((p, _, _) => p.active) { (_, everActiveS, _) => active =>
        if active then everActiveS.setState(true) else Callback.empty
      }
      .render { (props, everActiveS, draftS) =>
        props.coachProblemId match
          case None => StaticCoach.Component()
          case Some(pid) =>
            if !everActiveS.value then
              <.div(^.className := "coach", <.p(^.className := "coach__status", "Loading the coach…"))
            else
              CoachController.Component(
                CoachController.Props(
                  problemId = pid,
                  origin = props.origin,
                  snapshot = () =>
                    props.snapshot().map { s =>
                      CoachController.EditorSnapshot(
                        s.code,
                        s.language,
                        s.result.map(CoachController.summariseRun)
                      )
                    },
                  render = view => liveCoach(view, draftS)
                )
              )
      }

  // ── live coach ────────────────────────────────────────────────────────────────────────────────

  private def liveCoach(view: CoachController.View, draftS: Hooks.UseState[String]): VdomNode =
    view.stage match
      case CoachController.Stage.Connecting     => statusBox("Connecting to the coach…")
      case CoachController.Stage.NeedsSignIn    => signInBox
      case CoachController.Stage.Locked         => StaticCoach.Component() // graceful degrade (tier-gated)
      case CoachController.Stage.Unavailable(_) => StaticCoach.Component() // graceful degrade (unreachable)
      case CoachController.Stage.Coaching       => coaching(view, draftS)

  private def statusBox(msg: String): VdomNode =
    <.div(^.className := "coach", <.p(^.className := "coach__status", msg))

  private val signInBox: VdomNode =
    <.div(
      ^.className := "coach",
      <.p(^.className := "coach__status", "Sign in to start a coached session."),
      <.button(
        ^.tpe       := "button",
        ^.className := "coach__next-btn",
        ^.onClick --> Callback(AuthStore.openSignIn()),
        "Sign in"
      )
    )

  private def coaching(view: CoachController.View, draftS: Hooks.UseState[String]): VdomNode =
    val session      = view.session
    val stepIdx      = session.map(_.stepIndex).getOrElse(0)
    val curStep      = session.map(_.currentStep).getOrElse(Step.Clarify)
    val completed    = session.exists(_.completed)
    val thinking     = view.busy
    val showThinking = view.busy && view.streaming.isEmpty
    val stuck        = view.localRetries >= 3

    val header =
      <.div(
        ^.className := "coach__tracker",
        <.div(
          ^.className := "coach__tracker-left",
          <.span(
            ^.className := "coach__brand",
            LucideIcons.Target(LucideIcons.withClass("coach__brand-icon")),
            "Coach"
          ),
          <.div(
            ^.className := "coach__dots",
            Step.ordered.zipWithIndex.toVdomArray { case (st, i) =>
              val on   = i == stepIdx
              val done = i < stepIdx
              // Not buttons: the tutor's gate controls progression — you can't jump steps.
              <.span(
                ^.key   := s"dot-$i",
                ^.title := stepLabel(st),
                ^.className := ("coach__dot"
                  + (if on then " coach__dot--on" else "")
                  + (if done then " coach__dot--done" else "")),
                if done then LucideIcons.Check(LucideIcons.withClass("coach__dot-check"))
                else (i + 1).toString
              )
            }
          )
        ),
        ModelSelect.Component(
          ModelSelect.Props(
            models = view.models,
            selectedKey = view.selectedModel.map(_.key),
            locked = view.modelLocked,
            onChange = view.setModel
          )
        ),
        if completed then EmptyVdom
        else
          <.div(
            ^.className := "coach__progress",
            <.div(
              ^.className := "coach__progress-meta",
              <.span(
                ^.className := "coach__progress-step",
                s"Step ${stepIdx + 1} of 6 · ${stepLabel(curStep)}"
              ),
              <.span(
                ^.className := "coach__progress-pct",
                view.currentStepProgress.map(p => s"$p%").getOrElse("Not started")
              )
            ),
            <.div(
              ^.className := "coach__progress-track",
              <.div(
                ^.className := "coach__progress-fill",
                ^.style := js.Dynamic
                  .literal(width = s"${view.currentStepProgress.getOrElse(0)}%")
                  .asInstanceOf[js.Object]
              )
            )
          )
      )

    val stepHead =
      <.div(
        <.span(
          ^.className := "coach__eyebrow coach__eyebrow--primary",
          s"Step ${stepIdx + 1} of 6 · ${stepLabel(curStep)}"
        ),
        <.h2(^.className := "coach__verb", if completed then "Interview complete" else stepVerb(curStep))
      )

    val hintBox: VdomNode =
      view.lastResult
        .filter(_.verdict == Verdict.Retry)
        .flatMap(_.hint)
        .filter(_.nonEmpty)
        .map { h =>
          <.div(
            ^.className := (if stuck then "coach__note coach__note--stuck" else "coach__note"),
            <.div(
              ^.className := "coach__note-head",
              (if stuck then LucideIcons.Lightbulb else LucideIcons.Sparkles) (
                LucideIcons.withClass("coach__note-icon")
              ),
              <.span(
                ^.className := "coach__eyebrow coach__eyebrow--primary",
                if stuck then "Stuck? Here's a stronger hint" else "Coach hint"
              )
            ),
            <.p(^.className := "coach__note-body", h),
            if stuck then
              <.p(
                ^.className := "coach__note-ask",
                LucideIcons.Info(LucideIcons.withClass("coach__note-icon")),
                "Still stuck? Ask the coach a question instead — type it below and send. " +
                  "Questions don't count against you."
              )
            else EmptyVdom
          ): VdomNode
        }
        .getOrElse(EmptyVdom)

    val transcript: VdomNode =
      session match
        case Some(s) if s.messages.nonEmpty || view.streaming.nonEmpty || showThinking =>
          <.div(
            ^.className := "coach__transcript",
            s.messages.zipWithIndex.toVdomArray { case (m, i) => bubble(s"m-$i", m.role, m.content) },
            if view.streaming.nonEmpty then bubble("streaming", Role.Coach, view.streaming) else EmptyVdom,
            if showThinking then
              <.div(
                ^.className := "coach__msg coach__msg--coach",
                ^.key       := "thinking",
                <.div(
                  ^.className := "coach__bubble coach__bubble--thinking",
                  LucideIcons.Loader2(LucideIcons.withClass("coach__spin")),
                  <.span("Coach is thinking…")
                )
              )
            else EmptyVdom
          )
        case _ => EmptyVdom

    val errorBox: VdomNode =
      view.error.map(m => <.div(^.className := "coach__error", m): VdomNode).getOrElse(EmptyVdom)

    val footerArea: VdomNode =
      if completed then
        <.div(
          ^.className := "coach__handoff",
          <.p(
            ^.className := "coach__blurb",
            "You worked through all six steps. Reset to practise this problem again."
          ),
          <.button(
            ^.tpe       := "button",
            ^.className := "coach__ghost-btn",
            ^.onClick --> view.reset,
            LucideIcons.RotateCcw(LucideIcons.withClass("coach__ghost-btn-icon")),
            "Start over"
          )
        )
      else if view.needsKey && !view.hasByokKey then ByokCard.Component(view)
      else
        <.div(
          ^.className := "coach__composer",
          <.textarea(
            ^.className   := "coach__input",
            ^.rows        := 3,
            ^.value       := draftS.value,
            ^.placeholder := composerHint(curStep),
            ^.disabled    := thinking,
            ^.onChange ==> ((e: ReactEventFromInput) => draftS.setState(e.target.value))
          ),
          <.button(
            ^.tpe       := "button",
            ^.className := "coach__send",
            ^.disabled  := thinking || draftS.value.trim.isEmpty,
            ^.title     := "Send answer",
            ^.onClick --> (view.submit(curStep, draftS.value) >> draftS.setState("")),
            LucideIcons.Send(LucideIcons.withClass("coach__send-icon"))
          )
        )

    val byokFooter: VdomNode =
      if view.needsKey && view.hasByokKey then
        <.div(
          ^.className := "coach__byok-foot",
          <.button(
            ^.tpe       := "button",
            ^.className := "coach__ghost-btn",
            ^.onClick --> view.forgetByokKey,
            "Forget key"
          )
        )
      else EmptyVdom

    <.div(
      ^.className := "coach",
      header,
      stepHead,
      <.p(^.className := "coach__blurb", stepBlurb(curStep)),
      hintBox,
      transcript,
      errorBox,
      footerArea,
      byokFooter
    )

  private def bubble(key: String, role: Role, content: String): VdomNode =
    val side = if role == Role.User then "me" else "coach"
    <.div(
      ^.className := s"coach__msg coach__msg--$side",
      ^.key       := key,
      <.div(^.className := "coach__bubble", if content.nonEmpty then content else "…")
    )

  /** BYOK key entry, shown in place of the composer when the selected model needs the visitor's own key. */
  private object ByokCard:

    private case class KeyHint(label: String, host: String, placeholder: String, keysUrl: String)

    private def hintFor(provider: String): KeyHint =
      if provider.equalsIgnoreCase("openrouter") then
        KeyHint("OpenRouter", "openrouter.ai", "sk-or-v1-…", "https://openrouter.ai/keys")
      else
        KeyHint("Anthropic", "api.anthropic.com", "sk-ant-…", "https://console.anthropic.com/settings/keys")

    val Component =
      ScalaFnComponent
        .withHooks[CoachController.View]
        .useState("") // masked key draft
        .render { (view, draftS) =>
          val hint = hintFor(view.selectedModel.map(_.provider).getOrElse("openrouter"))
          <.div(
            ^.className := "coach__byok",
            <.div(
              ^.className := "coach__note",
              <.div(
                ^.className := "coach__note-head",
                LucideIcons.Sparkles(LucideIcons.withClass("coach__note-icon")),
                <.span(^.className := "coach__eyebrow coach__eyebrow--primary", "Bring your own key")
              ),
              <.p(
                ^.className := "coach__note-body",
                s"Coaching here runs on your own ${hint.label} API key — it stays in this browser tab " +
                  s"(sessionStorage), is sent only to ${hint.host} (never to this site), and is " +
                  "cleared when you sign out or close the tab."
              ),
              <.div(
                ^.className := "coach__byok-help",
                <.span(
                  ^.className := "coach__byok-which",
                  LucideIcons.Info(LucideIcons.withClass("coach__byok-help-icon")),
                  s"Use your ${hint.label} key (${hint.placeholder.takeWhile(_ != '…')}…)."
                ),
                <.a(
                  ^.className := "coach__byok-getkey",
                  ^.href      := hint.keysUrl,
                  ^.target    := "_blank",
                  ^.rel       := "noopener noreferrer",
                  LucideIcons.ExternalLink(LucideIcons.withClass("coach__byok-help-icon")),
                  s"Get a ${hint.label} key"
                )
              )
            ),
            <.div(
              ^.className := "coach__byok-row",
              <.input(
                ^.tpe         := "password",
                ^.className   := "coach__input coach__byok-input",
                ^.value       := draftS.value,
                ^.placeholder := hint.placeholder,
                ^.disabled    := view.keySaving,
                ^.onChange ==> ((e: ReactEventFromInput) => draftS.setState(e.target.value))
              ),
              <.button(
                ^.tpe       := "button",
                ^.className := "coach__next-btn",
                ^.disabled  := view.keySaving || draftS.value.trim.isEmpty,
                ^.onClick --> (view.saveByokKey(draftS.value) >> draftS.setState("")),
                if view.keySaving then "Validating…" else "Use this key"
              )
            )
          )
        }

/**
 * The static, no-backend six-step prompts — the Coach tab's fallback when there's no live tutor (generic
 * mount, tutor unreachable, or a tier without coaching). A static tracker with one generic hint + checklist
 * per guided phase; Implementation / Debugging hand off to the live editor on the right.
 */
private object StaticCoach:

  final private case class Phase(n: String, label: String, verb: String, blurb: String)

  private val Phases = Vector(
    Phase(
      "01",
      "Understanding",
      "Repeat the question",
      "Articulate the problem in your own words to confirm deep comprehension."
    ),
    Phase(
      "02",
      "Clarification",
      "Ask clarifying questions",
      "Probe edge cases and constraints before they bite in code."
    ),
    Phase("03", "Validation", "Use examples", "Build concrete cases to pin down the expected behaviour."),
    Phase(
      "04",
      "Planning",
      "Choose an approach",
      "Weigh approaches, tradeoffs, and complexity — then commit."
    ),
    Phase(
      "05",
      "Implementation",
      "Write your solution",
      "Code the chosen approach against the live test cases."
    ),
    Phase("06", "Debugging", "Test & debug", "Run against cases and fix what breaks.")
  )

  final private case class Guide(hint: String, checks: List[String])

  private val Guides = Map(
    0 -> Guide(
      "Say the problem back in your own words before touching code — the input, the output, and the one rule that makes it interesting.",
      List(
        "I can state the input and output types precisely",
        "I can restate the goal without re-reading the prompt",
        "I know what makes this problem non-trivial"
      )
    ),
    1 -> Guide(
      "Surface the unknowns now — assumptions are cheaper to fix here than in code.",
      List(
        "How large can the input get?",
        "Can it be empty, single-element, or contain duplicates?",
        "Is the input sorted or ordered in any useful way?"
      )
    ),
    2 -> Guide(
      "Pin down the expected behaviour with concrete cases before committing to an approach.",
      List(
        "I traced the given examples by hand",
        "I wrote down the smallest valid case",
        "I added at least one edge case of my own to the console below"
      )
    ),
    3 -> Guide(
      "Weigh at least two approaches by time and space, then commit to one before you code.",
      List(
        "I named the brute force and its complexity",
        "I know which pattern applies here, and why",
        "I said the target complexity out loud"
      )
    )
  )

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(0)
      .render { (_, activeS) =>
        val idx    = activeS.value
        val phase  = Phases(idx)
        val coding = idx >= 4

        val tracker =
          <.div(
            ^.className := "coach__tracker",
            <.div(
              ^.className := "coach__tracker-left",
              <.span(
                ^.className := "coach__brand",
                LucideIcons.Target(LucideIcons.withClass("coach__brand-icon")),
                "Coach"
              ),
              <.div(
                ^.className := "coach__dots",
                Phases.indices.toVdomArray { i =>
                  val on   = i == idx
                  val done = i < idx
                  <.button(
                    ^.key   := s"phase-$i",
                    ^.tpe   := "button",
                    ^.title := Phases(i).label,
                    ^.className := ("coach__dot"
                      + (if on then " coach__dot--on" else "")
                      + (if done then " coach__dot--done" else "")),
                    ^.onClick --> activeS.setState(i),
                    if done then LucideIcons.Check(LucideIcons.withClass("coach__dot-check"))
                    else Phases(i).n
                  )
                }
              )
            ),
            if coding then <.span(^.className := "coach__coding-badge", "Coding")
            else
              <.button(
                ^.tpe       := "button",
                ^.title     := "Skip the coaching and write code",
                ^.className := "coach__skip",
                ^.onClick --> activeS.setState(4),
                LucideIcons.Terminal(LucideIcons.withClass("coach__skip-icon")),
                "Skip to code"
              )
          )

        val stepHead =
          <.div(
            <.span(^.className := "coach__eyebrow", s"Step ${idx + 1} · ${phase.label}"),
            <.h2(^.className   := "coach__verb", phase.verb)
          )

        val body: VdomNode =
          if coding then
            <.div(
              ^.className := "coach__handoff",
              stepHead,
              <.p(
                ^.className := "coach__blurb",
                "The editor on the right is live — write your solution there and press Run to test it against the cases below it."
              ),
              <.button(
                ^.tpe       := "button",
                ^.className := "coach__ghost-btn",
                ^.onClick --> activeS.setState(3),
                LucideIcons.Target(LucideIcons.withClass("coach__ghost-btn-icon")),
                "Resume coaching"
              )
            )
          else
            val guide = Guides(idx)
            React.Fragment(
              stepHead,
              <.p(^.className := "coach__blurb", phase.blurb),
              <.div(
                ^.className := "coach__note",
                <.div(
                  ^.className := "coach__note-head",
                  LucideIcons.Sparkles(LucideIcons.withClass("coach__note-icon")),
                  <.span(^.className := "coach__eyebrow coach__eyebrow--primary", "Coach hint")
                ),
                <.p(^.className := "coach__note-body", guide.hint)
              ),
              <.div(
                ^.className := "coach__checks",
                guide.checks.zipWithIndex.toVdomArray { case (c, i) =>
                  <.div(
                    ^.key       := s"check-$i",
                    ^.className := "coach__check",
                    LucideIcons.Check(LucideIcons.withClass("coach__check-icon")),
                    <.span(^.className := "coach__check-text", c)
                  )
                }
              ),
              <.div(
                ^.className := "coach__actions",
                <.button(
                  ^.tpe       := "button",
                  ^.className := "coach__next-btn",
                  ^.onClick --> activeS.setState(math.min(5, idx + 1)),
                  "Next step",
                  LucideIcons.ArrowRight(LucideIcons.withClass("coach__next-btn-icon"))
                ),
                <.button(
                  ^.tpe       := "button",
                  ^.className := "coach__ghost-btn",
                  ^.onClick --> activeS.setState(4),
                  LucideIcons.Terminal(LucideIcons.withClass("coach__ghost-btn-icon")),
                  "Skip to code"
                )
              )
            )

        <.div(
          ^.className := "coach",
          <.p(
            ^.className := "coach__soon",
            LucideIcons.AlertTriangle(LucideIcons.withClass("coach__soon-icon")),
            "The interactive coach is on its way — these prompts are the manual version."
          ),
          tracker,
          body
        )
      }
