package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The Coach tab of the problem workbench — DESIGN ONLY for the pilot. A static six-step tracker (the design's
 * Understanding → Debugging phases) with one generic hint + checklist per guided phase, identical for every
 * problem; no agent, no backend. The Implementation and Debugging steps hand over to the live editor on the
 * right ("the editor on the right is live…"), matching the design's coding hand-off card.
 */
object CoachTab:

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

  // Generic study-method content (user decision: same for every problem until the real coach ships).
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
          // Up top and in red so nobody mistakes the static prompts for the real coach.
          <.p(
            ^.className := "coach__soon",
            LucideIcons.AlertTriangle(LucideIcons.withClass("coach__soon-icon")),
            "The interactive coach is on its way — these prompts are the manual version."
          ),
          tracker,
          body
        )
      }
