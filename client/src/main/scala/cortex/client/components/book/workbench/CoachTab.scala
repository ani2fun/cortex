package cortex.client.components.book.workbench

import cortex.client.auth.AuthStore
import cortex.client.components.icons.LucideIcons
import cortex.shared.tutor.TutorContract.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

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
      // Which coaching ladder this surface runs (six-step coding vs four-step conceptual).
      track: Track = Track.Problem,
      active: Boolean
  )

  // ── server-step display metadata (the SERVER's six steps; the design mock labels differ) ──────────

  private def stepLabel(s: Step): String = s match
    case Step.Clarify   => "Clarify"
    case Step.Examples  => "Examples"
    case Step.Approach  => "Approach"
    case Step.Plan      => "Plan"
    case Step.Implement => "Implement"
    case Step.Test      => "Test"
    case Step.Explain   => "Explain"
    case Step.Apply     => "Apply"
    case Step.Analyze   => "Analyze"
    case Step.Defend    => "Defend"

  private def stepVerb(s: Step): String = s match
    case Step.Clarify   => "Restate & clarify"
    case Step.Examples  => "Work through examples"
    case Step.Approach  => "Choose an approach"
    case Step.Plan      => "Plan the solution"
    case Step.Implement => "Write your solution"
    case Step.Test      => "Test & debug"
    case Step.Explain   => "Explain the idea"
    case Step.Apply     => "Apply the idea"
    case Step.Analyze   => "Weigh the trade-offs"
    case Step.Defend    => "Defend your reasoning"

  private def stepBlurb(s: Step): String = s match
    case Step.Clarify  => "Restate the problem in your own words and surface assumptions and edge cases."
    case Step.Examples => "Work through concrete examples to pin down the expected behaviour."
    case Step.Approach => "Brainstorm strategies and weigh their tradeoffs, then commit to one."
    case Step.Plan     => "Commit to a concrete plan before writing code."
    case Step.Implement =>
      "Write and run your solution in the editor on the right, then paste it into the coach below to " +
        "pass this step."
    case Step.Test =>
      "Test your solution on tricky cases in the editor, then paste it into the coach below with what " +
        "you found."
    case Step.Explain =>
      "In your own words, explain this lesson's main idea — what it is, why it exists, and how it works."
    case Step.Apply =>
      "Apply the idea to a concrete scenario and reason through what actually happens."
    case Step.Analyze =>
      "Weigh the trade-offs: when it helps, when it breaks, and the alternatives."
    case Step.Defend =>
      "Make a reasoned judgement and defend it against an obvious counter-point."

  private def composerHint(s: Step): String = s match
    case Step.Clarify   => "Restate the problem in your own words…"
    case Step.Examples  => "Give an example input and its expected output…"
    case Step.Approach  => "Describe your approach and its complexity…"
    case Step.Plan      => "Outline your step-by-step plan…"
    case Step.Implement => "Paste your solution from the editor here, then send to pass this step…"
    case Step.Test      => "Paste your tested solution and what you found, then send…"
    case Step.Explain   => "Explain the idea in your own words…"
    case Step.Apply     => "Walk through a concrete example…"
    case Step.Analyze   => "Name a trade-off or failure mode — and why…"
    case Step.Defend    => "State your position and defend it…"

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
                  track = props.track,
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
    val curStep      = session.map(_.currentStep).getOrElse(view.steps.headOption.getOrElse(Step.Clarify))
    val completed    = session.exists(_.completed)
    val thinking     = view.busy
    val showThinking = view.busy && view.streaming.isEmpty
    val stuck        = view.localRetries >= 3

    // Chat navigation, all scoped to THIS coach instance via closest(".coach") so multiple coaches on a
    // page never cross-scroll. The step dots double as tabs (jump to a step's section); a floating up/down
    // control jumps to the start / latest. scrollIntoView is called dynamically to dodge the
    // ScrollIntoViewOptions type surface.
    def scrollWithin(from: dom.Element, sel: String, block: String): Unit =
      val root = from.closest(".coach")
      if root != null then
        val target = root.querySelector(sel)
        if target != null then
          val _ = target.asInstanceOf[js.Dynamic].scrollIntoView(js.Dynamic.literal(
            behavior = "smooth",
            block = block
          ))

    // Jump to a step's section; fall back to the latest if that step has no messages yet (just advanced).
    def stepJump(i: Int): ReactMouseEvent => Callback = e =>
      val from = e.currentTarget.asInstanceOf[dom.Element]
      Callback {
        val root = from.closest(".coach")
        if root != null then
          val tgt = Option(root.querySelector(s"[data-coach-step='$i']"))
            .getOrElse(root.querySelector("[data-coach-anchor='bottom']"))
          if tgt != null then
            val _ = tgt.asInstanceOf[js.Dynamic].scrollIntoView(js.Dynamic.literal(
              behavior = "smooth",
              block = "start"
            ))
      }

    def jump(sel: String, block: String): ReactMouseEvent => Callback = e =>
      val from = e.currentTarget.asInstanceOf[dom.Element]
      Callback(scrollWithin(from, sel, block))

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
            view.steps.zipWithIndex.toVdomArray { case (st, i) =>
              val on   = i == stepIdx
              val done = i < stepIdx
              val cls = "coach__dot" + (if on then " coach__dot--on" else "") +
                (if done then " coach__dot--done" else "")
              val glyph: VdomNode =
                if done then LucideIcons.Check(LucideIcons.withClass("coach__dot-check"))
                else (i + 1).toString
              // Done/current dots double as tabs: click to jump to that step's section. Future steps stay
              // inert — the tutor's gate controls progression, you can't jump ahead.
              if done || on then
                <.button(
                  ^.key       := s"dot-$i",
                  ^.tpe       := "button",
                  ^.className := (cls + " coach__dot--nav"),
                  ^.title     := s"Jump to Step ${i + 1} · ${stepLabel(st)}",
                  ^.onClick ==> stepJump(i),
                  glyph
                )
              else
                <.span(
                  ^.key       := s"dot-$i",
                  ^.title     := s"Step ${i + 1} · ${stepLabel(st)} (upcoming)",
                  ^.className := cls,
                  glyph
                )
            }
          )
        ),
        ModelSelect.Component(
          ModelSelect.Props(
            models = view.models,
            selectedKey = view.selectedModel.map(_.key),
            locked = view.modelLocked,
            busy = view.busy,
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
                s"Step ${stepIdx + 1} of ${view.steps.size} · ${stepLabel(curStep)}"
              ),
              <.span(
                ^.className := "coach__progress-pct",
                view.currentStepProgress.map(p => s"$p%").getOrElse("Not started")
              )
            ),
            // The actionable "what to do" for this step, kept in the sticky header so it's always on
            // screen — you can scroll a long transcript and still see where you are and what's being asked.
            <.div(^.className := "coach__tracker-verb", stepVerb(curStep)),
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

    // The step eyebrow + verb now live in the STICKY header (coach__tracker) so they survive scrolling.
    // The scrollable body leads with the fuller blurb — or a completion headline once the interview is
    // done and the header's progress block (which carries the verb) is hidden.
    val intro: VdomNode =
      if completed then
        <.h2(
          ^.className := "coach__verb",
          if view.track == Track.Conceptual then "Understanding check complete" else "Interview complete"
        )
      else <.p(^.className := "coach__blurb", stepBlurb(curStep))

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

    val thinkingBubble: VdomNode =
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

    val transcript: VdomNode =
      session match
        case Some(s) if s.messages.nonEmpty || view.streaming.nonEmpty || showThinking =>
          // Group the conversation into per-step sections with a labelled divider, so a long interview
          // reads as distinct steps and the header dots can jump to each. groupBy preserves per-step order;
          // steps run sequentially (the gate never goes back), so Step.ordered yields the right sequence.
          val byStep = s.messages.zipWithIndex.groupBy(_._1.step)
          <.div(
            ^.className := "coach__transcript",
            view.steps.zipWithIndex.toVdomArray { case (st, i) =>
              byStep.get(st) match
                case None => EmptyVdom
                case Some(msgs) =>
                  val done = i < stepIdx
                  <.div(
                    ^.className                 := "coach__step-group",
                    ^.key                       := s"grp-$i",
                    VdomAttr("data-coach-step") := i.toString,
                    <.div(
                      ^.className := "coach__step-divider",
                      <.span(
                        ^.className :=
                          ("coach__step-badge" + (if done then " coach__step-badge--done" else "")),
                        if done then LucideIcons.Check(LucideIcons.withClass("coach__step-badge-icon"))
                        else (i + 1).toString
                      ),
                      <.span(^.className := "coach__step-divider-label", s"Step ${i + 1} · ${stepLabel(st)}")
                    ),
                    msgs.toVdomArray { case (m, idx) => bubble(s"m-$idx", m.role, m.content) }
                  )
            },
            if view.streaming.nonEmpty then bubble("streaming", Role.Coach, view.streaming) else EmptyVdom,
            thinkingBubble
          )
        case _ =>
          // Before the first turn, the conceptual coach greets with an opening question so the learner knows
          // exactly what to do (the coding coach opens via its own first turn instead). It's hidden the moment
          // a real message exists or the first turn starts streaming (those hit the case above).
          if view.track == Track.Conceptual && !completed then
            <.div(
              ^.className := "coach__transcript",
              bubble(
                "opening",
                Role.Coach,
                "Welcome — let's check how well this lesson landed. To start: in your own words, explain " +
                  "this lesson's core idea — what it is, why it exists, and how it works. Give it your best " +
                  "shot, and I'll help you sharpen it."
              )
            )
          else EmptyVdom

    val errorBox: VdomNode =
      view.error.map(m => <.div(^.className := "coach__error", m): VdomNode).getOrElse(EmptyVdom)

    // Human label for the BYOK footer note — only surfaced when a key is in play (OpenRouter / Anthropic).
    val providerLabel =
      view.selectedModel.map(_.provider) match
        case Some(p) if p.equalsIgnoreCase("anthropic") => "Anthropic"
        case _                                          => "OpenRouter"

    val footerArea: VdomNode =
      if view.transcriptArchived then
        <.div(
          ^.className := "coach__handoff",
          <.p(
            ^.className := "coach__blurb",
            "This conversation has ended on the server (saved sessions expire) — it's restored here from " +
              "your browser. Save it to keep a copy, or start fresh."
          ),
          <.button(
            ^.tpe       := "button",
            ^.className := "coach__ghost-btn",
            ^.onClick --> view.reset,
            LucideIcons.RotateCcw(LucideIcons.withClass("coach__ghost-btn-icon")),
            "Start fresh"
          )
        )
      else if completed then
        <.div(
          ^.className := "coach__handoff",
          <.p(
            ^.className := "coach__blurb",
            if view.track == Track.Conceptual then
              "You worked through all four steps. Reset to practise this lesson again."
            else "You worked through all six steps. Reset to practise this problem again."
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
        Composer.Component(
          Composer.Props(
            draft = draftS,
            disabled = thinking,
            placeholder = composerHint(curStep),
            onSend = view.submit(curStep, draftS.value) >> draftS.setState(""),
            needsKey = view.needsKey,
            hasByokKey = view.hasByokKey,
            providerLabel = providerLabel,
            forgetKey = view.forgetByokKey
          )
        )

    // Coach-save (allow-listed): persist a snapshot to the homelab DB. Shown once a session exists; gating
    // is server-side and mirrors Submit — a non-allowlisted save returns the request-access 403 verbatim.
    val saveBar: VdomNode =
      if session.isEmpty then EmptyVdom
      else
        val saving = view.saveStatus == CoachController.SaveStatus.Saving
        val saveLabel = view.saveStatus match
          case CoachController.SaveStatus.Saving => "Saving…"
          case CoachController.SaveStatus.Saved  => "Saved"
          case _                                 => "Save"
        <.div(
          ^.className := "coach__savebar",
          <.span(
            ^.className := "coach__save",
            <.button(
              ^.tpe       := "button",
              ^.className := "coach__save-btn",
              ^.disabled  := saving,
              ^.onClick --> view.save,
              if saving then LucideIcons.Loader2(LucideIcons.withClass("coach__save-icon coach__save-spin"))
              else LucideIcons.BookMarked(LucideIcons.withClass("coach__save-icon")),
              saveLabel
            ),
            // Hover/focus tooltip — the allow-list / request-access / erase story, mirroring the editor's
            // Submit tooltip so both gated actions read the same.
            <.div(
              ^.className := "coach__save-tip",
              ^.role      := "tooltip",
              <.p(
                ^.className := "coach__save-tip-head",
                "Save keeps this coaching conversation in the homelab database so you can revisit your " +
                  "reasoning later."
              ),
              <.p(
                "Saving is ",
                <.strong("allow-listed"),
                ": this is a personal homelab deployment for learning and experimentation — not a hosted " +
                  "service — and stored data carries no durability guarantee. Your browser already keeps " +
                  "this transcript across refreshes whether or not you save."
              ),
              <.p(
                "To request access, email ",
                <.strong("cortex.kakde.eu@gmail.com"),
                " with your GitHub username; grants are made selectively."
              ),
              <.p(
                "You can erase your saved transcripts at any time (",
                <.code("DELETE /api/coach/saved"),
                ")."
              )
            )
          ),
          view.saveStatus match
            // Success notification — confirm the transcript is stored. Worded to stay accurate after later
            // turns: a save DID happen; saving again captures progress made since.
            case CoachController.SaveStatus.Saved =>
              <.span(
                ^.className := "coach__save-ok",
                ^.role      := "status",
                LucideIcons.CircleCheck(LucideIcons.withClass("coach__save-ok-icon")),
                "Saved to your homelab database — run Save again to capture later progress."
              ): VdomNode
            case CoachController.SaveStatus.Failed(msg) =>
              <.span(
                ^.className := "coach__save-note",
                LucideIcons.AlertTriangle(LucideIcons.withClass("coach__save-ok-icon")),
                msg
              ): VdomNode
            case _ => EmptyVdom
        )

    // Floating up/down jump control — only once there's enough conversation to move through.
    val scrollNav: VdomNode =
      if session.exists(_.messages.length >= 2) then
        <.div(
          ^.className := "coach__scrollnav",
          <.button(
            ^.tpe        := "button",
            ^.className  := "coach__scrollnav-btn",
            ^.title      := "Jump to the start of the conversation",
            ^.aria.label := "Jump to the start of the conversation",
            ^.onClick ==> jump("[data-coach-anchor='top']", "start"),
            LucideIcons.ArrowUp(LucideIcons.withClass("coach__scrollnav-icon"))
          ),
          <.button(
            ^.tpe        := "button",
            ^.className  := "coach__scrollnav-btn",
            ^.title      := "Jump to the latest message",
            ^.aria.label := "Jump to the latest message",
            ^.onClick ==> jump("[data-coach-anchor='bottom']", "end"),
            LucideIcons.ArrowDown(LucideIcons.withClass("coach__scrollnav-icon"))
          )
        )
      else EmptyVdom

    <.div(
      ^.className := "coach",
      header,
      <.div(^.className := "coach__anchor", VdomAttr("data-coach-anchor") := "top"),
      intro,
      hintBox,
      transcript,
      errorBox,
      saveBar,
      footerArea,
      <.div(^.className := "coach__anchor", VdomAttr("data-coach-anchor") := "bottom"),
      scrollNav
    )

  private def bubble(key: String, role: Role, content: String): VdomNode =
    val side = if role == Role.User then "me" else "coach"
    <.div(
      ^.className := s"coach__msg coach__msg--$side",
      ^.key       := key,
      <.div(^.className := "coach__bubble", if content.nonEmpty then content else "…")
    )

  /**
   * The answer composer — a docked compose surface, visually distinct from the reply bubbles above it. It
   * carries a Write/Preview toggle, a markdown toolbar (bold, italic, inline code, bullet list, code fence),
   * Enter-to-send (Shift+Enter for a newline), a lightweight markdown preview, and the BYOK "Forget key"
   * control folded into its footer. The draft lives in the parent's state so it survives stage transitions.
   */
  private object Composer:

    final case class Props(
        draft: Hooks.UseState[String],
        disabled: Boolean,
        placeholder: String,
        onSend: Callback,
        needsKey: Boolean,
        hasByokKey: Boolean,
        providerLabel: String,
        forgetKey: Callback
    )

    /** Leading ```info-string (e.g. "python") on a fence's first line. */
    private val FenceInfo = "^[A-Za-z0-9_+\\-]*\\n".r

    private def escapeHtml(s: String): String =
      s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Wrap the selection in `pre`/`suf`; returns (newValue, selStart, selEnd). */
    private def wrapSelection(v: String, s: Int, e: Int, pre: String, suf: String): (String, Int, Int) =
      val sel = v.substring(s, e)
      (v.substring(0, s) + pre + sel + suf + v.substring(e), s + pre.length, s + pre.length + sel.length)

    /** Prefix the selection with a "- " bullet on its own line. */
    private def insertList(v: String, s: Int, e: Int): (String, Int, Int) =
      val sel   = v.substring(s, e)
      val lead  = if s > 0 && v.charAt(s - 1) != '\n' then "\n" else ""
      val caret = s + lead.length + 2 + sel.length
      (v.substring(0, s) + lead + "- " + sel + v.substring(e), caret, caret)

    /** Insert a ```python fence around the selection (or a placeholder), selecting the body. */
    private def insertFence(v: String, s: Int, e: Int): (String, Int, Int) =
      val sel   = v.substring(s, e)
      val lead  = if s > 0 && v.charAt(s - 1) != '\n' then "\n" else ""
      val body  = if sel.nonEmpty then sel else "# your code here"
      val out   = v.substring(0, s) + lead + "```python\n" + body + "\n```\n" + v.substring(e)
      val caret = s + lead.length + "```python\n".length
      (out, caret, caret + body.length)

    /**
     * A deliberately tiny markdown→HTML pass for the live preview ONLY (not the heavy async
     * MarkdownRenderer): HTML-escape, then ``` fences → <pre><code>, inline `code`, **bold**, *italic*, "- "
     * bullets, newlines → <br>. Mirrors the design mock's `mdLite`.
     */
    private def mdLite(src: String): String =
      val parts = src.split("```", -1)
      val out   = new StringBuilder
      var i     = 0
      while i < parts.length do
        if i % 2 == 1 then
          val seg  = FenceInfo.findPrefixOf(parts(i)).fold(parts(i))(m => parts(i).substring(m.length))
          val code = if seg.endsWith("\n") then seg.dropRight(1) else seg
          out.append("<pre class=\"cmp-pre\"><code>").append(escapeHtml(code)).append("</code></pre>")
        else
          var t = escapeHtml(parts(i))
          t = t.replaceAll("`([^`]+)`", "<code class=\"cmp-ic\">$1</code>")
          t = t.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>")
          t = t.replaceAll("\\*([^*\\n]+)\\*", "<em>$1</em>")
          // Bullets line-by-line — Scala.js's JS-RegExp backend rejects the (?m) inline flag.
          val bulleted =
            t.split("\n", -1).map(l => if l.startsWith("- ") then "•&nbsp;" + l.substring(2) else l)
          out.append(bulleted.mkString("\n").replace("\n", "<br>"))
        i += 1
      out.toString

    val Component =
      ScalaFnComponent
        .withHooks[Props]
        .useState(false) // preview mode
        .useRefToVdom[dom.html.TextArea]
        .render { (props, previewS, areaRef) =>
          val draft   = props.draft
          val preview = previewS.value
          val canSend = !props.disabled && draft.value.trim.nonEmpty

          // Apply a pure (value, selStart, selEnd) ⇒ (value', start', end') transform to the textarea: write
          // back through controlled state, then restore the caret on the next tick (after React commits).
          def fmt(f: (String, Int, Int) => (String, Int, Int)): Callback =
            areaRef.get.flatMap {
              case Some(el) =>
                val (out, ns, ne) = f(el.value, el.selectionStart, el.selectionEnd)
                draft.setState(out) >> Callback {
                  js.timers.setTimeout(0) {
                    el.focus()
                    el.setSelectionRange(ns, ne)
                  }
                  ()
                }
              case None => Callback.empty
            }

          val onKeyDown: ReactKeyboardEvent => Callback = e =>
            if e.key == "Enter" && !e.shiftKey then
              e.preventDefaultCB >> (if canSend then props.onSend else Callback.empty)
            else Callback.empty

          def fmtBtn(
              title: String,
              body: VdomNode,
              f: (String, Int, Int) => (String, Int, Int),
              extra: String = ""
          ): VdomNode =
            <.button(
              ^.tpe       := "button",
              ^.className := (if extra.nonEmpty then s"coach__fmt $extra" else "coach__fmt"),
              ^.title     := title,
              ^.disabled  := props.disabled,
              ^.onClick --> fmt(f),
              body
            )

          val tabs =
            <.div(
              ^.className := "coach__composer-tabs",
              <.button(
                ^.tpe := "button",
                ^.className := (if preview then "coach__composer-tab"
                                else "coach__composer-tab coach__composer-tab--on"),
                ^.onClick --> previewS.setState(false),
                "Write"
              ),
              <.button(
                ^.tpe := "button",
                ^.className := (if preview then "coach__composer-tab coach__composer-tab--on"
                                else "coach__composer-tab"),
                ^.onClick --> previewS.setState(true),
                "Preview"
              )
            )

          val bar =
            <.div(
              ^.className := "coach__composer-bar",
              fmtBtn("Bold (**)", <.b("B"), wrapSelection(_, _, _, "**", "**")),
              fmtBtn("Italic (*)", <.i("I"), wrapSelection(_, _, _, "*", "*")),
              fmtBtn(
                "Inline code (`)",
                LucideIcons.Code(LucideIcons.withClass("coach__fmt-icon")),
                wrapSelection(_, _, _, "`", "`")
              ),
              fmtBtn(
                "Bulleted list",
                LucideIcons.List(LucideIcons.withClass("coach__fmt-icon")),
                insertList(_, _, _)
              ),
              <.span(^.className := "coach__fmt-sep"),
              fmtBtn(
                "Code block (```)",
                React.Fragment(LucideIcons.Terminal(LucideIcons.withClass("coach__fmt-icon")), "Code block"),
                insertFence(_, _, _),
                "coach__fmt--code"
              ),
              <.span(^.className := "coach__composer-bar-sp"),
              <.span(
                ^.className := "coach__composer-hint",
                <.kbd("⏎"),
                " send · ",
                <.kbd("⇧⏎"),
                " newline"
              ),
              <.button(
                ^.tpe       := "button",
                ^.className := "coach__composer-send",
                ^.disabled  := !canSend,
                ^.title     := "Send answer",
                ^.onClick --> props.onSend,
                "Send",
                LucideIcons.Send(LucideIcons.withClass("coach__composer-send-icon"))
              )
            )

          val byokFoot: VdomNode =
            if props.needsKey && props.hasByokKey then
              <.div(
                ^.className := "coach__composer-foot",
                <.span(
                  ^.className := "coach__byok-note",
                  LucideIcons.Lock(LucideIcons.withClass("coach__byok-note-icon")),
                  s"Coaching on your ${props.providerLabel} key · stays in this tab"
                ),
                <.button(
                  ^.tpe       := "button",
                  ^.className := "coach__composer-forget",
                  ^.onClick --> props.forgetKey,
                  "Forget key"
                )
              )
            else EmptyVdom

          <.div(
            ^.className := "coach__composer",
            <.div(
              ^.className := "coach__composer-top",
              <.span(
                ^.className := "coach__composer-label",
                LucideIcons.Pencil(LucideIcons.withClass("coach__composer-label-icon")),
                "Your answer · Markdown"
              ),
              tabs
            ),
            <.div(
              ^.className := "coach__composer-box",
              if preview then
                <.div(
                  ^.className               := "coach__composer-preview",
                  ^.dangerouslySetInnerHtml := mdLite(draft.value)
                )
              else
                <.textarea.withRef(areaRef)(
                  ^.className   := "coach__composer-area",
                  ^.rows        := 4,
                  ^.value       := draft.value,
                  ^.placeholder := props.placeholder,
                  ^.disabled    := props.disabled,
                  ^.onChange ==> ((e: ReactEventFromInput) => draft.setState(e.target.value)),
                  ^.onKeyDown ==> onKeyDown
                )
              ,
              if preview then EmptyVdom else bar
            ),
            byokFoot
          )
        }

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
