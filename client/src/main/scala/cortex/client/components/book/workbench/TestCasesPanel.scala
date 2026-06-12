package cortex.client.components.book.workbench

import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The schema-driven Test-cases console — the lower half of the stacked workbench editor. Purely
 * presentational: [[WorkbenchEditor]] owns all state (live cases, per-case values, verdicts, the run) and
 * feeds it down; this component renders chips, one labelled field per argument, the status line, and the
 * Your-vs-Expected output panels, per the design.
 *
 * Field-span rule from the design: a lone argument, any array type (`int[]`, `char[]`, …), and `string`s span
 * the full row; scalar args (`k`, `target`, …) sit two-up.
 */
object TestCasesPanel:

  /** Outcome of running the ACTIVE case. Verdicts for other cases live in WorkbenchEditor's state. */
  enum Verdict:
    case Accepted(time: Option[String], memoryKb: Option[Long])
    case Wrong(got: String)
    case Errored(statusDescription: String, stderr: String, compileOutput: String)

    /** Ran fine but the case has no expected output — show stdout, no judgement. */
    case Finished(stdout: String, time: Option[String], memoryKb: Option[Long])

  final case class Props(
      spec: Block.TestSpec,
      cases: Vector[Block.TestCase],
      values: Vector[Map[String, String]],
      activeIdx: Int,
      passed: Set[Int],
      running: Boolean,
      verdict: Option[Verdict],
      onSelect: Int => Callback,
      onAdd: Callback,
      onReset: Callback,
      onArgChange: (String, String) => Callback,
      // Adding custom cases is auth-gated (the parent's onAdd opens the sign-in modal when locked;
      // this flag just dresses the buttons with the lock affordance + tooltip).
      addLocked: Boolean = false,
      // Indices >= customFrom are user-added cases — they have no expected output by design, and
      // the Expected panel explains that instead of showing a bare dash.
      customFrom: Int = Int.MaxValue
  )

  private def spansRow(spec: Block.TestSpec, arg: Block.ArgSpec): Boolean =
    spec.args.sizeIs == 1 || arg.tpe.contains("[]") || arg.tpe == "string"

  val Component =
    ScalaFnComponent[Props] { props =>
      val active = props.cases.lift(props.activeIdx)
      val vals   = props.values.lift(props.activeIdx).getOrElse(Map.empty)

      val header =
        <.div(
          ^.className := "wb-tests__head",
          <.div(
            ^.className := "wb-tests__head-title",
            <.span(
              ^.className := "wb-tests__head-icon",
              LucideIcons.ListChecks(LucideIcons.withClass("wb-tests__head-icon-svg"))
            ),
            <.span(^.className := "wb-tests__eyebrow", "Test cases")
          ),
          <.div(
            ^.className := "wb-tests__head-actions",
            <.button(
              ^.tpe := "button",
              ^.title := (if props.addLocked then "Sign in with GitHub to add custom test cases"
                          else "Add test case"),
              ^.onClick --> props.onAdd,
              ^.className := "wb__btn" + (if props.addLocked then " wb__btn--locked" else ""),
              if props.addLocked then
                <.span(^.className := "wb__lock", LucideIcons.Lock(LucideIcons.withClass("wb__lock-icon")))
              else EmptyVdom,
              LucideIcons.Plus(LucideIcons.withClass("wb__btn-icon")),
              "Add test case"
            ),
            <.button(
              ^.tpe        := "button",
              ^.title      := "Reset test cases",
              ^.aria.label := "Reset test cases",
              ^.onClick --> props.onReset,
              ^.className := "wb__btn wb__btn--icon",
              LucideIcons.RotateCcw(LucideIcons.withClass("wb__btn-icon"))
            )
          )
        )

      val chips =
        <.div(
          ^.className := "wb-tests__chips",
          props.cases.indices.toVdomArray { i =>
            val on = i == props.activeIdx
            <.button(
              ^.key := s"case-$i",
              ^.tpe := "button",
              ^.className := ("wb-tests__chip"
                + (if on then " wb-tests__chip--on" else "")
                + (if props.passed(i) then " wb-tests__chip--passed" else "")),
              ^.onClick --> props.onSelect(i),
              if props.passed(i) then LucideIcons.Check(LucideIcons.withClass("wb-tests__chip-check"))
              else EmptyVdom,
              s"Case ${i + 1}"
            )
          },
          <.button(
            ^.tpe := "button",
            ^.title := (if props.addLocked then "Sign in with GitHub to add custom test cases"
                        else "Add test case"),
            ^.aria.label := "Add test case",
            ^.className  := "wb-tests__chip wb-tests__chip--add",
            ^.onClick --> props.onAdd,
            if props.addLocked then LucideIcons.Lock(LucideIcons.withClass("wb-tests__chip-check"))
            else LucideIcons.Plus(LucideIcons.withClass("wb-tests__chip-check"))
          )
        )

      val status: VdomNode =
        if props.running then
          <.span(
            ^.className := "wb-tests__status-running",
            LucideIcons.Loader2(LucideIcons.withClass("wb-tests__spinner")),
            <.span(
              ^.className := "wb-tests__status-meta",
              s"Running against case ${props.activeIdx + 1}…"
            )
          )
        else
          props.verdict match
            case Some(Verdict.Accepted(time, memKb)) =>
              <.span(
                ^.className := "wb-tests__status-accepted",
                LucideIcons.CircleCheck(LucideIcons.withClass("wb-tests__status-icon")),
                "Accepted",
                time.map(t => <.span(^.className := "wb-tests__status-meta", s"${t}s"): VdomNode)
                  .getOrElse(EmptyVdom),
                memKb
                  .map(m =>
                    <.span(^.className := "wb-tests__status-meta", s"${(m / 1024).toInt} MB"): VdomNode
                  )
                  .getOrElse(EmptyVdom)
              )
            case Some(Verdict.Wrong(_)) =>
              <.span(
                ^.className := "wb-tests__status-wrong",
                LucideIcons.X(LucideIcons.withClass("wb-tests__status-icon")),
                "Wrong answer"
              )
            case Some(Verdict.Errored(desc, _, _)) =>
              <.span(
                ^.className := "wb-tests__status-wrong",
                LucideIcons.AlertTriangle(LucideIcons.withClass("wb-tests__status-icon")),
                desc
              )
            case Some(Verdict.Finished(_, time, memKb)) =>
              <.span(
                ^.className := "wb-tests__status-finished",
                LucideIcons.CircleCheck(LucideIcons.withClass("wb-tests__status-icon")),
                "Finished",
                time.map(t => <.span(^.className := "wb-tests__status-meta", s"${t}s"): VdomNode)
                  .getOrElse(EmptyVdom),
                memKb
                  .map(m =>
                    <.span(^.className := "wb-tests__status-meta", s"${(m / 1024).toInt} MB"): VdomNode
                  )
                  .getOrElse(EmptyVdom)
              )
            case None =>
              <.span(
                ^.className := "wb-tests__status-idle",
                s"Press Run to test against case ${props.activeIdx + 1}."
              )

      val argFields =
        <.div(
          ^.className := "wb-tests__grid",
          props.spec.args.toVdomArray { arg =>
            <.div(
              ^.key := s"arg-${arg.id}",
              ^.className := (if spansRow(props.spec, arg) then "wb-legend wb-legend--span"
                              else "wb-legend"),
              <.span(
                ^.className := "wb-legend__label",
                arg.label,
                <.span(^.className := "wb-legend__type", arg.tpe)
              ),
              <.input(
                ^.className   := "wb-legend__input",
                ^.value       := vals.getOrElse(arg.id, ""),
                ^.placeholder := arg.placeholder.getOrElse(""),
                ^.spellCheck  := false,
                ^.onChange ==> ((e: ReactEventFromInput) => props.onArgChange(arg.id, e.target.value))
              )
            )
          }
        )

      val (yourOut, yourTint): (VdomNode, String) = props.verdict match
        case Some(Verdict.Accepted(_, _)) =>
          // On accept the run's stdout matches expected modulo trailing whitespace — echo expected so
          // the two panels read identically.
          (
            <.pre(
              ^.className := "wb-tests__out-pre",
              active.flatMap(_.expected).getOrElse("")
            ),
            " wb-legend--ok"
          )
        case Some(Verdict.Wrong(got)) =>
          (
            <.pre(
              ^.className := "wb-tests__out-pre",
              if got.trim.nonEmpty then got else "(no output)"
            ),
            " wb-legend--err"
          )
        case Some(Verdict.Errored(_, stderr, compileOutput)) =>
          (
            <.div(
              Option(compileOutput).filter(_.nonEmpty).map { co =>
                <.pre(^.className := "wb-tests__out-pre wb-tests__out-pre--compile", co): VdomNode
              }.getOrElse(EmptyVdom),
              Option(stderr).filter(_.nonEmpty).map { e =>
                <.pre(^.className := "wb-tests__out-pre wb-tests__out-pre--stderr", e): VdomNode
              }.getOrElse(EmptyVdom)
            ),
            " wb-legend--err"
          )
        case Some(Verdict.Finished(stdout, _, _)) =>
          (
            <.pre(
              ^.className := "wb-tests__out-pre",
              if stdout.trim.nonEmpty then stdout else "(no output)"
            ),
            ""
          )
        case None =>
          (<.pre(^.className := "wb-tests__out-pre wb-tests__out-pre--empty", "—"), "")

      val outputs =
        <.div(
          ^.className := "wb-tests__grid wb-tests__grid--outputs",
          <.div(
            ^.className := s"wb-legend wb-legend--span$yourTint",
            <.span(^.className := "wb-legend__label", "Your output"),
            yourOut
          ),
          if props.activeIdx >= props.customFrom then
            // Custom case: there's nothing authoritative to compare against, so the panel is
            // disabled with the reason spelled out rather than showing a bare dash.
            <.div(
              ^.className := "wb-legend wb-legend--span wb-legend--disabled",
              <.span(^.className := "wb-legend__label", "Expected output"),
              <.p(
                ^.className := "wb-tests__custom-note",
                "Custom case — there's no expected output to judge against. Run shows your program's output as-is."
              )
            )
          else
            <.div(
              ^.className := "wb-legend wb-legend--span",
              <.span(^.className := "wb-legend__label", "Expected output"),
              active.flatMap(_.expected) match
                case Some(exp) => <.pre(^.className := "wb-tests__out-pre", exp)
                case None      => <.pre(^.className := "wb-tests__out-pre wb-tests__out-pre--empty", "—")
            )
        )

      <.div(
        ^.className := "wb-tests",
        header,
        <.div(
          ^.className := "wb-tests__body",
          chips,
          <.div(^.className := "wb-tests__status", status),
          argFields,
          outputs
        )
      )
    }
