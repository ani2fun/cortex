package codefolio.client.components.cortex.widgets

import codefolio.client.components.cortex.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Raft animator — step-replay of three scripted Raft scenarios over a 3-node cluster: leader election, log
 * replication, leader failover. Each step is a pre-baked snapshot of cluster state (per-node role, term, log,
 * votedFor) plus a caption and an optional "message in flight" arrow. The reader walks forward and back
 * through the steps; the widget renders the current snapshot as a 3-node SVG with roles, terms, log entries,
 * and any in-flight messages drawn between nodes.
 *
 * Scripted rather than simulated for the same reason `PartitionSimulator` is: a real Raft simulation would
 * either be too random (different result every replay) or too complex to drive with sliders. Pre-baked
 * scenarios make the failure-mode pedagogy crisp.
 *
 * Re-used in:
 *   - Lesson 14 (consensus) — primary.
 *   - Capstone 44 (payment system) — Raft-backed ledger.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":     "Raft — three scenarios, step by step",
 *   "scenarios": [
 *     {
 *       "name": "Leader election",
 *       "steps": [
 *         {
 *           "caption": "...",
 *           "nodes": [
 *             { "id": "N1", "role": "follower",  "term": 1, "log": ["x=1"], "votedFor": null },
 *             { "id": "N2", "role": "follower",  "term": 1, "log": ["x=1"], "votedFor": null },
 *             { "id": "N3", "role": "follower",  "term": 1, "log": ["x=1"], "votedFor": null }
 *           ],
 *           "message": { "from": "N1", "to": "N2", "label": "RequestVote(term=2)" }
 *         },
 *         ...
 *       ]
 *     }
 *   ]
 * }
 * }}}
 *
 *   - `role` ∈ {follower, candidate, leader, down}. "down" renders the node as struck out.
 *   - `log` is the list of committed + uncommitted entries; the lesson's scenarios keep it ≤ 5 entries so it
 *     fits horizontally inside the node SVG.
 *   - `message` is optional; if present, draws an arrow between the named nodes with the label.
 */
object RaftAnimator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  enum NodeRole:
    case Follower, Candidate, Leader, Down

  final case class NodeState(
      id: String,
      role: NodeRole,
      term: Int,
      log: List[String],
      votedFor: Option[String]
  )

  final case class Message(from: String, to: String, label: String)

  final case class Step(caption: String, nodes: List[NodeState], message: Option[Message])

  final case class Scenario(name: String, steps: List[Step])

  final case class Spec(title: Option[String], scenarios: List[Scenario])

  final case class Props(payload: String)

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parseRole(s: String): NodeRole = s.toLowerCase match
    case "leader"    => NodeRole.Leader
    case "candidate" => NodeRole.Candidate
    case "down"      => NodeRole.Down
    case _           => NodeRole.Follower

  private def parseNode(d: js.Dynamic): NodeState =
    NodeState(
      id = d.string("id"),
      role = parseRole(d.string("role", "follower")),
      term = d.double("term", 1.0).toInt,
      log = d.stringList("log").getOrElse(Nil),
      votedFor = d.optString("votedFor")
    )

  private def parseMessage(d: Option[js.Dynamic]): Option[Message] =
    d.flatMap { md =>
      val from  = md.string("from")
      val to    = md.string("to")
      val label = md.string("label")
      if from.isEmpty || to.isEmpty then None else Some(Message(from, to, label))
    }

  private def parseStep(d: js.Dynamic): Step =
    Step(
      caption = d.string("caption"),
      nodes = d.dynList("nodes").map(parseNode),
      message = parseMessage(d.optObj("message"))
    )

  private def parseScenario(d: js.Dynamic): Scenario =
    Scenario(name = d.string("name"), steps = d.dynList("steps").map(parseStep))

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val scenarios = d.dynList("scenarios").map(parseScenario)
      if scenarios.isEmpty then throw PayloadDecoder.invalid("scenarios must be non-empty")
      if scenarios.exists(_.steps.isEmpty) then
        throw PayloadDecoder.invalid("every scenario.steps must be non-empty")
      Spec(d.optString("title"), scenarios)
    }

  // ===========================================================================
  // Layout
  // ===========================================================================

  private val ViewBoxWidth  = 720.0
  private val ViewBoxHeight = 360.0
  private val CenterX       = ViewBoxWidth / 2
  private val CenterY       = 170.0
  private val NodeR         = 56.0
  private val NodeRingR     = 110.0 // distance from center to each node centre

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  /** Position of node at index `i` of `n` evenly around a horizontal arc. */
  private def nodePos(i: Int, n: Int): (Double, Double) =
    // For 3 nodes: place them at top, bottom-left, bottom-right.
    val angle = -math.Pi / 2 + 2 * math.Pi * i / math.max(1, n)
    (CenterX + NodeRingR * math.cos(angle), CenterY + NodeRingR * math.sin(angle))

  private def roleClass(r: NodeRole): String = r match
    case NodeRole.Leader    => "raft-animator__node raft-animator__node--leader"
    case NodeRole.Candidate => "raft-animator__node raft-animator__node--candidate"
    case NodeRole.Follower  => "raft-animator__node raft-animator__node--follower"
    case NodeRole.Down      => "raft-animator__node raft-animator__node--down"

  private def roleBadge(r: NodeRole): String = r match
    case NodeRole.Leader    => "LEADER"
    case NodeRole.Candidate => "CANDIDATE"
    case NodeRole.Follower  => "follower"
    case NodeRole.Down      => "DOWN"

  // ===========================================================================
  // SVG
  // ===========================================================================

  private def nodeSvg(node: NodeState, x: Double, y: Double): String =
    val cls      = roleClass(node.role)
    val badge    = roleBadge(node.role)
    val logStr   = if node.log.isEmpty then "—" else node.log.mkString(" · ")
    val votedFor = node.votedFor.map(v => s"voted: $v").getOrElse("")
    val voteY    = y + 28
    val logY     = y + 44
    s"""<g>
       |  <circle class="$cls" cx="$x" cy="$y" r="$NodeR"/>
       |  <text class="raft-animator__node-id" x="$x" y="${y - 16}" text-anchor="middle">${esc(
        node.id
      )}</text>
       |  <text class="raft-animator__node-badge" x="$x" y="${y - 2}" text-anchor="middle">${esc(
        badge
      )}</text>
       |  <text class="raft-animator__node-term" x="$x" y="${y + 14}" text-anchor="middle">term ${node.term}</text>
       |  ${
        if votedFor.nonEmpty then
          s"""<text class="raft-animator__node-votedfor" x="$x" y="$voteY" text-anchor="middle">${esc(
              votedFor
            )}</text>"""
        else ""
      }
       |  <text class="raft-animator__node-log" x="$x" y="$logY" text-anchor="middle">log: ${esc(
        logStr
      )}</text>
       |</g>""".stripMargin

  private def messageSvg(message: Message, positions: Map[String, (Double, Double)]): String =
    (positions.get(message.from), positions.get(message.to)) match
      case (Some((x1, y1)), Some((x2, y2))) =>
        val dx     = x2 - x1
        val dy     = y2 - y1
        val dist   = math.sqrt(dx * dx + dy * dy)
        val ux     = dx / dist
        val uy     = dy / dist
        val startX = x1 + ux * NodeR
        val startY = y1 + uy * NodeR
        val endX   = x2 - ux * NodeR
        val endY   = y2 - uy * NodeR
        val midX   = (startX + endX) / 2
        val midY   = (startY + endY) / 2 - 8
        s"""<g>
           |  <line class="raft-animator__arrow" x1="$startX" y1="$startY" x2="$endX" y2="$endY"
           |        marker-end="url(#raftArrow)"/>
           |  <text class="raft-animator__arrow-label" x="$midX" y="$midY" text-anchor="middle">${esc(
            message.label
          )}</text>
           |</g>""".stripMargin
      case _ => ""

  private def buildSvg(step: Step): String =
    val n = step.nodes.size
    val positions = step.nodes.zipWithIndex.map { case (node, i) =>
      val (x, y) = nodePos(i, n)
      node.id -> (x, y)
    }.toMap

    val nodes = step.nodes.zipWithIndex.map { case (node, i) =>
      val (x, y) = nodePos(i, n)
      nodeSvg(node, x, y)
    }.mkString("\n")

    val arrow = step.message.map(messageSvg(_, positions)).getOrElse("")

    val arrowMarker =
      s"""<defs>
         |  <marker id="raftArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
         |    <path d="M 0 0 L 10 5 L 0 10 z" class="raft-animator__arrow-head"/>
         |  </marker>
         |</defs>""".stripMargin

    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="raft-animator__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $arrowMarker
       |  $arrow
       |  $nodes
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(0) // current scenario index
      .useState(0) // current step index
      .render { (_, specM, scenarioIdxS, stepIdxS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Raft-animator widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val sIdx       = math.max(0, math.min(spec.scenarios.size - 1, scenarioIdxS.value))
            val scenario   = spec.scenarios(sIdx)
            val stepIdx    = math.max(0, math.min(scenario.steps.size - 1, stepIdxS.value))
            val step       = scenario.steps(stepIdx)
            val totalSteps = scenario.steps.size

            <.div(
              ^.className := "raft-animator not-prose",
              spec.title
                .map(t => <.p(^.className := "raft-animator__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "raft-animator__scenarios",
                spec.scenarios.zipWithIndex.map { case (sc, i) =>
                  val cls =
                    if i == sIdx then "raft-animator__scenario raft-animator__scenario--active"
                    else "raft-animator__scenario"
                  <.button(
                    ^.tpe       := "button",
                    ^.className := cls,
                    ^.key       := i.toString,
                    ^.onClick --> (scenarioIdxS.setState(i) >> stepIdxS.setState(0)),
                    sc.name
                  ): VdomNode
                }.toVdomArray
              ),
              <.div(
                ^.className               := "raft-animator__frame",
                ^.dangerouslySetInnerHtml := buildSvg(step)
              ),
              <.p(
                ^.className := "raft-animator__caption",
                ^.aria.live := "polite",
                step.caption
              ),
              <.div(
                ^.className := "raft-animator__controls",
                <.button(
                  ^.tpe       := "button",
                  ^.className := "raft-animator__button",
                  ^.disabled  := (stepIdx <= 0),
                  ^.onClick --> stepIdxS.setState(stepIdx - 1),
                  "← Back"
                ),
                <.span(
                  ^.className := "raft-animator__progress",
                  s"step ${stepIdx + 1} / $totalSteps"
                ),
                <.button(
                  ^.tpe       := "button",
                  ^.className := "raft-animator__button raft-animator__button--primary",
                  ^.disabled  := (stepIdx >= totalSteps - 1),
                  ^.onClick --> stepIdxS.setState(stepIdx + 1),
                  "Next →"
                ),
                <.button(
                  ^.tpe       := "button",
                  ^.className := "raft-animator__button",
                  ^.onClick --> stepIdxS.setState(0),
                  "Reset"
                )
              )
            )
      }
