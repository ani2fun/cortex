package codefolio.client.components.cortex.widgets

import codefolio.client.components.cortex.widgets.PayloadDecoder.*
import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Partition simulator — inline three-node KV cluster that walks the reader through canonical CAP/PACELC
 * scenarios as step-replay. Each scenario is a list of frames; each frame names the node values, the still-
 * reachable links between nodes, and any nodes whose last write was refused. The widget renders a frame, lets
 * the reader step (prev / next / play / reset), and narrates the current frame in a caption strip.
 *
 * Companion to `examples/04-cap-pacelc-simulator/` — that's the fully interactive Python cluster the reader
 * clones to play with arbitrary operations. This widget is the *inline preview* the lesson body needs so the
 * partition + heal pattern lands without leaving the page.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":  "Partition simulator — three-node KV store",
 *   "scenarios": [
 *     {
 *       "name": "CP / partition / minority refuses",
 *       "mode": "CP",
 *       "steps": [
 *         { "msg": "...", "nodes": { "A": "v0", "B": "v0", "C": "v0" },
 *           "links": [["A","B"], ["A","C"], ["B","C"]], "refused": [] },
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * }}}
 *
 *   - `nodes` is a label-per-node string (the current value of `x`).
 *   - `links` is the symmetric reachability graph at this frame. A missing pair means the partition severed
 *     it.
 *   - `refused` is optional; lists nodes whose write attempt was rejected in this frame (rendered with a
 *     "REFUSED" badge).
 *
 * Same string-built-SVG + scalajs-react + dangerouslySetInnerHTML pattern as ArrayTraversal. Layout: scenario
 * chips at top, the 3-node graph in the middle, step controls + a caption strip below.
 */
object PartitionSimulator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Step(
      msg: String,
      nodes: Map[String, String],
      links: Set[(String, String)],
      refused: Set[String]
  )

  final case class Scenario(name: String, mode: String, steps: List[Step])

  final case class Spec(title: Option[String], scenarios: List[Scenario])

  final case class Props(payload: String)

  // ===========================================================================
  // Layout constants — three nodes in a row inside a 720-wide viewBox. Choices
  // here keep the widget under the 640-px prose column when scaled down.
  // ===========================================================================

  private val ViewBoxWidth  = 720.0
  private val ViewBoxHeight = 220.0
  private val NodeRadius    = 36.0
  private val NodeYCenter   = 92.0

  private val NodeXPositions: Map[String, Double] = Map(
    "A" -> 160.0,
    "B" -> 360.0,
    "C" -> 560.0
  )

  private val StepDelayMs = 1800

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val scenarios = d.dynList("scenarios").map { s =>
        Scenario(name = s.string("name"), mode = s.string("mode"), steps = s.dynList("steps").map(parseStep))
      }
      if scenarios.isEmpty then throw PayloadDecoder.invalid("scenarios must be non-empty")
      if scenarios.exists(_.steps.isEmpty) then
        throw PayloadDecoder.invalid("every scenario must have ≥ 1 step")
      if scenarios.exists(_.name.trim.isEmpty) then
        throw PayloadDecoder.invalid("every scenario.name must be non-empty")
      Spec(title = d.optString("title"), scenarios = scenarios)
    }

  private def parseStep(s: js.Dynamic): Step =
    val nodesObj =
      s.selectDynamic("nodes").asInstanceOf[js.UndefOr[js.Dictionary[String]]].toOption.getOrElse(
        js.Dictionary()
      )
    val nodes = nodesObj.toMap
    val rawLinks =
      s.selectDynamic("links").asInstanceOf[js.UndefOr[js.Array[js.Array[String]]]].toOption.getOrElse(
        js.Array()
      )
    val links = rawLinks.toList.flatMap { pair =>
      if pair.length >= 2 then
        val a = pair(0)
        val b = pair(1)
        // Symmetric: store as a sorted pair so a→b and b→a are the same edge.
        val (lo, hi) = if a <= b then (a, b) else (b, a)
        Some((lo, hi))
      else None
    }.toSet
    val refused =
      s.selectDynamic("refused").asInstanceOf[js.UndefOr[js.Array[String]]].toOption.getOrElse(js.Array()).toSet
    Step(msg = s.string("msg"), nodes = nodes, links = links, refused = refused)

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  // All node-pairs the cluster could form an edge between, sorted so each
  // unordered pair is canonicalised (a <= b). Listed explicitly because the
  // node set is fixed and the compiler can't prove `combinations(2)` always
  // yields two-element lists.
  private val EdgePairs: List[(String, String)] = List(
    ("A", "B"),
    ("A", "C"),
    ("B", "C")
  )

  private def linkSvg(step: Step): String =
    EdgePairs
      .map { case (a, b) =>
        val present = step.links.contains((a, b))
        val xa      = NodeXPositions(a)
        val xb      = NodeXPositions(b)
        // Curve the A↔C edge upward so it doesn't overlap the A↔B and B↔C edges.
        val isOuter = a == "A" && b == "C"
        val pathD =
          if isOuter then
            val midX = (xa + xb) / 2
            val arcY = NodeYCenter - 56.0
            s"M $xa $NodeYCenter Q $midX $arcY $xb $NodeYCenter"
          else
            s"M $xa $NodeYCenter L $xb $NodeYCenter"
        val cls = if present then "partition-simulator__link"
        else "partition-simulator__link partition-simulator__link--broken"
        s"""<path class="$cls" d="$pathD" fill="none"/>"""
      }
      .mkString("\n")

  private def nodeSvg(node: String, value: String, step: Step): String =
    val cx      = NodeXPositions(node)
    val cy      = NodeYCenter
    val refused = step.refused.contains(node)
    val nodeCls = if refused then "partition-simulator__node partition-simulator__node--refused"
    else "partition-simulator__node"
    val valueCls = if refused then "partition-simulator__node-value partition-simulator__node-value--refused"
    else "partition-simulator__node-value"
    val badge =
      if refused then
        s"""<g class="partition-simulator__badge">
           |  <rect x="${cx - 30}" y="${cy + NodeRadius + 6}" width="60" height="16" rx="8"/>
           |  <text x="$cx" y="${cy + NodeRadius + 17}" text-anchor="middle">REFUSED</text>
           |</g>""".stripMargin
      else ""
    s"""<g>
       |  <circle class="$nodeCls" cx="$cx" cy="$cy" r="$NodeRadius"/>
       |  <text class="partition-simulator__node-label" x="$cx" y="${cy - 6}" text-anchor="middle">$node</text>
       |  <text class="$valueCls" x="$cx" y="${cy + 14}" text-anchor="middle">x=${esc(value)}</text>
       |  $badge
       |</g>""".stripMargin

  private def buildSvg(step: Step): String =
    val nodes = NodeXPositions.keys.toList.sorted
      .map(n => nodeSvg(n, step.nodes.getOrElse(n, "—"), step))
      .mkString("\n")
    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="partition-simulator__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  ${linkSvg(step)}
       |  $nodes
       |</svg>""".stripMargin

  private def clamp(i: Int, count: Int): Int =
    if count <= 0 then 0 else math.max(0, math.min(count - 1, i))

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(0) // scenario index
      // Stepper hook drives the inner step axis. Input.stepCount is the active scenario's step count,
      // recomputed every render — when the user switches scenarios, the dep tuple in Stepper's
      // play-loop effect changes and the timer cancels. The scenario-reset effect below explicitly
      // resets the index to 0 so the new scenario starts from step 0 rather than the clamped tail.
      .customBy { (_, specM, scenarioS) =>
        val stepCount =
          specM.value.toOption.flatMap(_.scenarios.lift(scenarioS.value)).fold(0)(_.steps.size)
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs.toDouble))
      }
      .useEffectWithDepsBy((_, specM, scenarioS, _) =>
        (specM.value.toOption.fold(0)(_.scenarios.size), scenarioS.value)
      ) { (_, _, _, stepper) => _ =>
        // Reset to step 0 when scenario changes and stop playback. `stepper.reset` bundles both.
        stepper.reset
      }
      .render { (_, specM, scenarioS, stepper) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Partition simulator payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val scenarioIdx = clamp(scenarioS.value, math.max(1, spec.scenarios.size))
            val scenario    = spec.scenarios(scenarioIdx)
            val count       = scenario.steps.size
            val stepIdx     = stepper.index
            val step        = scenario.steps(stepIdx)
            val modeBadge   = scenario.mode.toUpperCase

            <.div(
              ^.className := "partition-simulator not-prose",
              spec.title
                .map(t => <.p(^.className := "partition-simulator__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "partition-simulator__scenarios",
                spec.scenarios.zipWithIndex.toVdomArray { case (sc, idx) =>
                  val active = scenarioIdx == idx
                  <.button(
                    ^.key := s"scenario-$idx",
                    ^.tpe := "button",
                    ^.className := s"partition-simulator__scenario${
                        if active then " partition-simulator__scenario--active" else ""
                      }",
                    ^.onClick --> scenarioS.setState(idx),
                    <.span(
                      ^.className := s"partition-simulator__mode-badge partition-simulator__mode-badge--${sc.mode.toLowerCase}",
                      sc.mode.toUpperCase
                    ),
                    <.span(^.className := "partition-simulator__scenario-name", sc.name)
                  )
                }
              ),
              <.div(
                ^.className               := "partition-simulator__frame",
                ^.dangerouslySetInnerHtml := buildSvg(step)
              ),
              <.p(
                ^.className := "partition-simulator__caption",
                ^.aria.live := "polite",
                <.span(
                  ^.className := s"partition-simulator__mode-badge partition-simulator__mode-badge--${scenario.mode.toLowerCase}",
                  modeBadge
                ),
                <.span(
                  ^.className := "partition-simulator__caption-text",
                  if step.msg.nonEmpty then step.msg else " "
                )
              ),
              <.div(
                ^.className := "partition-simulator__controls",
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> stepper.previous,
                  ^.disabled   := stepper.atStart,
                  ^.aria.label := "Previous step",
                  ^.className  := "partition-simulator__button",
                  LucideIcons.ArrowLeft(LucideIcons.withClass("partition-simulator__button-icon")),
                  "Prev"
                ),
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> stepper.togglePlay,
                  ^.disabled   := count == 0,
                  ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                  ^.className  := "partition-simulator__button partition-simulator__button--primary",
                  if stepper.isPlaying then
                    LucideIcons.Pause(LucideIcons.withClass("partition-simulator__button-icon"))
                  else LucideIcons.Play(LucideIcons.withClass("partition-simulator__button-icon")),
                  if stepper.isPlaying then "Pause" else "Play"
                ),
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> stepper.next,
                  ^.disabled   := stepper.atEnd,
                  ^.aria.label := "Next step",
                  ^.className  := "partition-simulator__button",
                  "Next",
                  LucideIcons.ArrowRight(LucideIcons.withClass("partition-simulator__button-icon"))
                ),
                <.button(
                  ^.tpe := "button",
                  ^.onClick --> stepper.reset,
                  ^.disabled   := stepper.atStart && !stepper.isPlaying,
                  ^.aria.label := "Reset",
                  ^.className  := "partition-simulator__button partition-simulator__button--icon",
                  LucideIcons.RotateCcw(LucideIcons.withClass("partition-simulator__button-icon"))
                ),
                <.span(
                  ^.className := "partition-simulator__progress",
                  s"Step ${stepIdx + 1} / ${math.max(1, count)}"
                )
              )
            )
      }
