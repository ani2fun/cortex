package codefolio.client.components.cortex.widgets

import codefolio.client.components.cortex.widgets.PayloadDecoder.*
import codefolio.client.components.icons.LucideIcons
import codefolio.client.d3.D3
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/**
 * Decision-tree stepper — ninth widget in the D3 catalog. Animates backtracking-style enumeration as a
 * top-down tree of choices: the root is the empty partial solution, each child edge is one choice, and a DFS
 * walk over the tree mirrors the call stack of a recursive backtracking routine.
 *
 * Step semantics — each event emits one widget step:
 *   - `enter` — descend along the edge to the child. Mark the node `active`; push its label onto the `path`
 *     panel.
 *   - `leaf` — terminal node. `accept: true` marks `accepted` (emerald halo + checkmark) and appends the
 *     node's label to the `solutions` panel; `accept: false` marks `rejected` (slate dim, no append).
 *   - `prune` — mark the node and its entire subtree `pruned` (strike-through, reduced opacity); draw a small
 *     badge with the reason (`dup` / `dead-end` / `constraint`). Path is NOT pushed.
 *   - `backtrack` — pop the path panel; the node we're moving up to becomes `active`; the previously- active
 *     subtree dims to `popped` but stays drawn. A transient arc fades in along the parent→child edge to
 *     signal the return direction and fades out by the next step.
 *
 * Node status is COMPUTED per step from the event tape, not author-supplied. Authors only declare the tree
 * topology (`nodes` with `parent` pointers + optional `edgeLabel`) and the event list.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Subsets of [1, 2, 3]",
 *   "kind":  "enumeration",                  // "enumeration" | "search" — affects leaf-rejected styling
 *   "nodes": [
 *     {"id": "root", "label": "[]"},
 *     {"id": "n1",   "label": "[1]", "parent": "root", "edgeLabel": "+1"}
 *   ],
 *   "panels": [
 *     {"name": "path",      "kind": "list"},
 *     {"name": "solutions", "kind": "list"}
 *   ],
 *   "events": [
 *     {"kind": "enter",     "nodeId": "root", "msg": "Start []"},
 *     {"kind": "enter",     "nodeId": "n1",   "msg": "Choose 1"},
 *     {"kind": "leaf",      "nodeId": "n1",   "accept": true, "msg": "Record [1]"},
 *     {"kind": "backtrack", "nodeId": "root", "msg": "Unchoose 1"},
 *     {"kind": "prune",     "nodeId": "n2",   "reason": "constraint", "msg": "..."}
 *   ]
 * }
 * }}}
 *
 * The widget shares the canon-pointer pattern with [[LinkedList]] for keyed `<g>` node selections and the DFS
 * sibling-spread tree layout with [[CallStack]]'s recursion-tree mode. Backtrack arcs reuse the curved-Bézier
 * construction `LinkedList` uses for `cycleTarget`.
 *
 * No payload-supplied markers — node status is the only thing the renderer paints, and it's all computed.
 */
object DecisionTree:

  // ---------------------------------------------------------------------------
  // Schema — parsed lazily from the JSON payload string. Each widget owns its
  // own schema; shared keeps Block.D3Widget structurally loose (ADR-0006).
  // ---------------------------------------------------------------------------

  // One node definition. `parent` absent → root; `edgeLabel` shown on the
  // parent→child edge (optional).
  final case class NodeDef(id: String, label: String, parent: Option[String], edgeLabel: Option[String])

  // One panel descriptor. `kind` is "list"; future kinds (e.g. "array") could
  // be added without changing the renderer's outer shape.
  final case class PanelDef(name: String, kind: String)

  // One event in the tape. `kind` is the closed set { "enter", "leaf",
  // "prune", "backtrack" }; the renderer's applier dispatches on it. `reason`
  // applies to `prune` only (closed set "dup" / "dead-end" / "constraint";
  // unknown falls back to "dup"). `accept` applies to `leaf` only.
  final case class Event(
      kind: String,
      nodeId: String,
      reason: Option[String],
      accept: Option[Boolean],
      msg: String
  )

  final case class Spec(
      kind: String, // "enumeration" | "search"
      nodes: List[NodeDef],
      panels: List[PanelDef],
      events: List[Event],
      title: Option[String]
  )

  final case class Props(payload: String)

  // ---------------------------------------------------------------------------
  // Closed-set constants. Unknown values collapse to safe defaults at parse
  // time so a typo never crashes the chapter.
  // ---------------------------------------------------------------------------

  private val KindEnumeration = "enumeration"
  private val KindSearch      = "search"
  private val Kinds           = Set(KindEnumeration, KindSearch)

  private val EventEnter     = "enter"
  private val EventLeaf      = "leaf"
  private val EventPrune     = "prune"
  private val EventBacktrack = "backtrack"
  private val EventKinds     = Set(EventEnter, EventLeaf, EventPrune, EventBacktrack)

  private val ReasonDup        = "dup"
  private val ReasonDeadEnd    = "dead-end"
  private val ReasonConstraint = "constraint"
  private val Reasons          = Set(ReasonDup, ReasonDeadEnd, ReasonConstraint)

  private val PanelList = "list"

  // ---------------------------------------------------------------------------
  // Layout constants — sized so a 6-12 node tree fits a prose column without
  // horizontal scroll. Panels render as HTML to the right; their width is set
  // in CSS, not the SVG viewBox.
  // ---------------------------------------------------------------------------

  private val NodeWidth        = 78.0
  private val NodeHeight       = 30.0
  private val NodeSlotGap      = 24.0 // horizontal gap between sibling subtree slots
  private val LevelHeight      = 74.0 // vertical gap between depth levels
  private val PaddingX         = 16.0
  private val PaddingY         = 14.0
  private val EdgeLabelGapY    = 6.0  // vertical bias of edge label off the midpoint
  private val ReasonBadgeDx    = 10.0 // horizontal offset of reason badge from node right edge
  private val ReasonBadgeWidth = 60.0 // matches the badge rect width in renderReasonBadges
  private val StepDelayMs      = 1700
  private val FadeMs           = 350.0
  private val SvgNs            = "http://www.w3.org/2000/svg"

  // ---------------------------------------------------------------------------
  // Parsing — each parser is small + total; PayloadDecoder.run collapses any
  // thrown exception into Left(message) and the renderer shows an inline error.
  // ---------------------------------------------------------------------------

  private def parseNode(d: js.Dynamic): NodeDef =
    NodeDef(
      id = d.string("id"),
      label = d.string("label"),
      parent = d.optString("parent"),
      edgeLabel = d.optString("edgeLabel")
    )

  private def parsePanel(d: js.Dynamic): PanelDef =
    val rawKind = d.optString("kind").getOrElse(PanelList)
    val kind    = if rawKind == PanelList then PanelList else PanelList
    PanelDef(name = d.string("name"), kind = kind)

  private def parseEvent(d: js.Dynamic): Event =
    val rawKind   = d.optString("kind").getOrElse(EventEnter)
    val kind      = if EventKinds.contains(rawKind) then rawKind else EventEnter
    val rawReason = d.optString("reason")
    val reason    = rawReason.map(r => if Reasons.contains(r) then r else ReasonDup)
    Event(
      kind = kind,
      nodeId = d.string("nodeId"),
      reason = reason,
      accept = d.optBool("accept"),
      msg = d.string("msg")
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val rawKind = d.optString("kind").getOrElse(KindEnumeration)
      val kind    = if Kinds.contains(rawKind) then rawKind else KindEnumeration
      val nodes   = d.dynList("nodes").map(parseNode).filter(_.id.nonEmpty)
      val panels  = d.dynList("panels").map(parsePanel).filter(_.name.nonEmpty)
      val events  = d.dynList("events").map(parseEvent)
      if nodes.isEmpty then throw PayloadDecoder.invalid("nodes must be non-empty")
      if events.isEmpty then throw PayloadDecoder.invalid("events must be non-empty")
      Spec(
        kind = kind,
        nodes = nodes,
        panels = panels,
        events = events,
        title = d.optString("title")
      )
    }

  // ---------------------------------------------------------------------------
  // Per-step state — computed by replaying events [0..stepIdx]. Node status
  // values: "unseen" | "active" | "visited" | "accepted" | "rejected" |
  // "pruned" | "popped". Path = labels along the root→active path. Solutions
  // = node labels recorded on `leaf accept=true` events, in order.
  //
  // `lastBacktrack` holds the (childId, parentId) pair for one step so the
  // transient backtrack arc can render along that edge.
  // ---------------------------------------------------------------------------

  private val StatusUnseen   = "unseen"
  private val StatusActive   = "active"
  private val StatusVisited  = "visited"
  private val StatusAccepted = "accepted"
  private val StatusRejected = "rejected"
  private val StatusPruned   = "pruned"
  private val StatusPopped   = "popped"

  final case class WalkState(
      nodeStatus: Map[String, String],
      activeNode: Option[String],
      pathIds: List[String],                  // root → active, node IDs
      solutions: List[String],                // labels of accepted leaves, in order
      pruneReasons: Map[String, String],      // nodeId → reason (persists)
      lastBacktrack: Option[(String, String)] // (poppedChildId, newActiveParentId) — one step
  )

  private def emptyWalkState(spec: Spec): WalkState =
    WalkState(
      nodeStatus = spec.nodes.map(_.id -> StatusUnseen).toMap,
      activeNode = None,
      pathIds = Nil,
      solutions = Nil,
      pruneReasons = Map.empty,
      lastBacktrack = None
    )

  // Children-of map — precomputed once per spec for the prune-subtree walk
  // and the layout.
  private def buildChildrenOf(spec: Spec): Map[String, List[String]] =
    val byId        = spec.nodes.map(_.id).toSet
    val accumulator = mutable.Map.empty[String, mutable.ArrayBuffer[String]]
    for n <- spec.nodes do
      n.parent match
        case Some(pid) if byId.contains(pid) =>
          accumulator.getOrElseUpdate(pid, mutable.ArrayBuffer.empty[String]) += n.id
        case _ => ()
    accumulator.view.mapValues(_.toList).toMap

  // All descendants of `id` including `id` itself. Used by the `prune` event
  // applier to mark a whole subtree.
  private def subtreeIds(rootId: String, childrenOf: Map[String, List[String]]): List[String] =
    val out = mutable.ArrayBuffer.empty[String]
    def walk(id: String): Unit =
      out += id
      childrenOf.getOrElse(id, Nil).foreach(walk)
    walk(rootId)
    out.toList

  // Apply one event. Returns the new state. Each event clears `lastBacktrack`
  // unless it itself is a backtrack (the arc lives exactly one step).
  private def applyEvent(
      state: WalkState,
      event: Event,
      childrenOf: Map[String, List[String]],
      labelOf: Map[String, String]
  ): WalkState =
    event.kind match
      case EventEnter =>
        // Demote the previously-active node to `visited` BEFORE the new active
        // takes over — but ONLY if it was still `active`. Terminal statuses
        // (accepted / rejected / pruned) MUST be preserved: an internal node
        // that was leaf-accepted earlier (Payload 2 — every prefix is itself a
        // recorded subset) keeps its ✓ for the rest of the trace, even as the
        // cursor descends past it. Wiping the accepted status here would mean
        // ancestors lose their ✓ the moment we step into a child.
        val demoted = state.activeNode match
          case Some(prev) if prev != event.nodeId =>
            state.nodeStatus.get(prev) match
              case Some(StatusActive) => state.nodeStatus.updated(prev, StatusVisited)
              case _                  => state.nodeStatus
          case _ => state.nodeStatus
        // The new active node likewise preserves a terminal status — entering
        // a previously-accepted node (rare but possible) doesn't strip its ✓.
        val updated = demoted.get(event.nodeId) match
          case Some(StatusAccepted) | Some(StatusRejected) | Some(StatusPruned) => demoted
          case _ => demoted.updated(event.nodeId, StatusActive)
        state.copy(
          nodeStatus = updated,
          activeNode = Some(event.nodeId),
          pathIds = state.pathIds :+ event.nodeId,
          lastBacktrack = None
        )
      case EventLeaf =>
        val accept = event.accept.getOrElse(true)
        val newStatus =
          if accept then state.nodeStatus.updated(event.nodeId, StatusAccepted)
          else state.nodeStatus.updated(event.nodeId, StatusRejected)
        val newSolutions =
          if accept then state.solutions :+ labelOf.getOrElse(event.nodeId, event.nodeId)
          else state.solutions
        state.copy(
          nodeStatus = newStatus,
          solutions = newSolutions,
          lastBacktrack = None
        )
      case EventPrune =>
        // Mark this node + its whole subtree as pruned. Record the reason on
        // the head node so the badge renders.
        val ids       = subtreeIds(event.nodeId, childrenOf)
        val newStatus = ids.foldLeft(state.nodeStatus)((m, id) => m.updated(id, StatusPruned))
        val reason    = event.reason.getOrElse(ReasonDup)
        state.copy(
          nodeStatus = newStatus,
          pruneReasons = state.pruneReasons.updated(event.nodeId, reason),
          lastBacktrack = None
        )
      case EventBacktrack =>
        // Pop the path back to `event.nodeId` (the parent we're moving to).
        // The previously-active child + its descendants become `popped`. The
        // popped child id is whatever was on top of the path stack.
        val poppedChild = state.pathIds.lastOption
        val newPath =
          if state.pathIds.lastOption.exists(_ != event.nodeId) then state.pathIds.init
          else state.pathIds
        // Mark popped subtree (popped child + its descendants) as `popped`,
        // but only those that aren't accepted or pruned (those keep their
        // terminal status — `accepted` stays emerald, `pruned` stays striked).
        val withPopped = poppedChild match
          case Some(cid) =>
            val descendants = subtreeIds(cid, childrenOf)
            descendants.foldLeft(state.nodeStatus) { (m, id) =>
              m.get(id) match
                case Some(StatusAccepted) => m
                case Some(StatusPruned)   => m
                case Some(StatusRejected) => m
                case _                    => m.updated(id, StatusPopped)
            }
          case None => state.nodeStatus
        // Same preservation rule as EventEnter — backtracking onto a node
        // that's already accepted/rejected/pruned keeps the terminal status.
        // The cursor's current position is still trackable via the path panel
        // and the caption; the tree shows what was decided about each node.
        val withActive = withPopped.get(event.nodeId) match
          case Some(StatusAccepted) | Some(StatusRejected) | Some(StatusPruned) => withPopped
          case _ => withPopped.updated(event.nodeId, StatusActive)
        val backtrackEdge = poppedChild.map(cid => (cid, event.nodeId))
        state.copy(
          nodeStatus = withActive,
          activeNode = Some(event.nodeId),
          pathIds = newPath,
          lastBacktrack = backtrackEdge
        )
      case _ =>
        state.copy(lastBacktrack = None)

  private def computeState(
      spec: Spec,
      stepIdx: Int,
      childrenOf: Map[String, List[String]],
      labelOf: Map[String, String]
  ): WalkState =
    var s     = emptyWalkState(spec)
    val bound = math.min(stepIdx, spec.events.size - 1)
    for i <- 0 to bound do s = applyEvent(s, spec.events(i), childrenOf, labelOf)
    s

  // ---------------------------------------------------------------------------
  // Layout — Reingold-Tilford-flavoured: leaves take the next free slot;
  // internal nodes sit at the midpoint of their children's slot range. Each
  // node's `(slot, depth)` is stable across steps (topology never changes).
  // Output: (id → (xSlot, depth)) + maxDepth + slotCount.
  // ---------------------------------------------------------------------------

  private def computeLayout(
      spec: Spec,
      childrenOf: Map[String, List[String]]
  ): (Map[String, (Double, Int)], Int, Double) =
    val byId      = spec.nodes.map(n => n.id -> n).toMap
    val roots     = spec.nodes.filter(n => n.parent.forall(p => !byId.contains(p))).map(_.id)
    val positions = mutable.Map.empty[String, (Double, Int)]
    val nextSlot  = Array(0.0)
    var maxDepth  = 0
    def visit(id: String, depth: Int): Unit =
      if depth > maxDepth then maxDepth = depth
      val children = childrenOf.getOrElse(id, Nil)
      if children.isEmpty then
        val mySlot = nextSlot(0)
        nextSlot(0) += 1.0
        positions(id) = (mySlot, depth)
      else
        children.foreach(visit(_, depth + 1))
        val firstSlot = positions(children.head)._1
        val lastSlot  = positions(children.last)._1
        positions(id) = ((firstSlot + lastSlot) / 2.0, depth)
    roots.foreach(visit(_, 0))
    val slotCount = nextSlot(0).max(1.0)
    (positions.toMap, maxDepth, slotCount)

  // ---------------------------------------------------------------------------
  // SVG bootstrap — first paint only. viewBox + width/height refreshed per
  // render because the canvas size depends on the layout.
  // ---------------------------------------------------------------------------

  private def ensureSvg(host: dom.html.Element, spec: Spec): dom.Element =
    val existing = host.querySelector("svg")
    if existing != null then existing.asInstanceOf[dom.Element]
    else
      val svg = dom.document.createElementNS(SvgNs, "svg").asInstanceOf[dom.Element]
      svg.setAttribute("class", "decision-tree__svg")
      svg.setAttribute("role", "img")
      svg.setAttribute("aria-label", spec.title.getOrElse(s"Decision tree (${spec.kind})"))
      svg.setAttribute("xmlns", SvgNs)
      svg.setAttribute("viewBox", "0 0 400 200")
      svg.setAttribute("width", "400")
      svg.setAttribute("height", "200")
      host.appendChild(svg)
      svg

  // ---------------------------------------------------------------------------
  // Per-step render — idempotent. Edges + edge labels keyed by `${pid}->${cid}`;
  // nodes keyed by id; reason badges keyed by id. The backtrack arc is a
  // singleton — at most one per step.
  // ---------------------------------------------------------------------------

  private def renderStep(
      svgEl: dom.Element,
      spec: Spec,
      stepIdx: Int,
      animate: Boolean,
      childrenOf: Map[String, List[String]],
      labelOf: Map[String, String],
      positions: Map[String, (Double, Int)],
      maxDepth: Int,
      slotCount: Double
  ): Unit =
    val svg     = D3.select(svgEl)
    val state   = computeState(spec, stepIdx, childrenOf, labelOf)
    val fadeDur = if animate then FadeMs else 0.0

    val treeWidth = math.max(1.0, slotCount) * (NodeWidth + NodeSlotGap) - NodeSlotGap
    // Reserve room on the right for a reason badge when any prune event
    // exists (a prune on the rightmost node would otherwise clip the badge
    // outside the viewBox).
    val reasonPadRight =
      if spec.events.exists(_.kind == EventPrune) then ReasonBadgeDx + ReasonBadgeWidth else 0.0
    val totalW = PaddingX * 2 + treeWidth + reasonPadRight
    val totalH = PaddingY * 2 + (maxDepth + 1).toDouble * LevelHeight

    val _ = svg.attr("viewBox", s"0 0 $totalW $totalH")
    val _ = svg.attr("width", totalW.toString)
    val _ = svg.attr("height", totalH.toString)

    def nodeX(id: String): Double       = PaddingX + positions(id)._1 * (NodeWidth + NodeSlotGap)
    def nodeY(id: String): Double       = PaddingY + positions(id)._2.toDouble * LevelHeight
    def nodeCenterX(id: String): Double = nodeX(id) + NodeWidth / 2.0
    def nodeCenterY(id: String): Double = nodeY(id) + NodeHeight / 2.0
    def nodeBottomY(id: String): Double = nodeY(id) + NodeHeight
    def nodeTopY(id: String): Double    = nodeY(id)

    renderEdges(svg, spec, state, nodeCenterX, nodeBottomY, nodeTopY, fadeDur)
    renderEdgeLabels(svg, spec, state, nodeCenterX, nodeY, fadeDur)
    renderBacktrackArc(svg, state, nodeCenterX, nodeBottomY, nodeTopY, fadeDur)
    renderNodes(svg, spec, state, nodeX, nodeY, fadeDur)
    renderReasonBadges(svg, state, nodeX, nodeCenterY, fadeDur)

  // ── Edges ─────────────────────────────────────────────────────────────────
  // Drawn first so nodes sit on top. Edge state derives from the CHILD's
  // status: if the child is `pruned`, the edge is dashed grey; if `active`
  // or `accepted`, the edge brightens; if `popped`, it dims.

  private def renderEdges(
      svg: D3.Selection,
      spec: Spec,
      state: WalkState,
      nodeCenterX: String => Double,
      nodeBottomY: String => Double,
      nodeTopY: String => Double,
      fadeDur: Double
  ): Unit =
    val nodeIds = spec.nodes.iterator.map(_.id).toSet
    val edges   = spec.nodes.flatMap(n => n.parent.filter(nodeIds.contains).map(p => (p, n.id)))
    val data: js.Array[js.Any] = edges.map { case (pid, cid) =>
      val x1          = nodeCenterX(pid)
      val y1          = nodeBottomY(pid)
      val x2          = nodeCenterX(cid)
      val y2          = nodeTopY(cid)
      val childStatus = state.nodeStatus.getOrElse(cid, StatusUnseen)
      js.Dynamic
        .literal(
          id = s"$pid->$cid",
          d = s"M $x1 $y1 L $x2 $y2",
          status = childStatus
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val dFn: js.Function2[js.Any, Int, js.Any]   = (d, _) => d.asInstanceOf[js.Dynamic].d
    val classFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val s = d.asInstanceOf[js.Dynamic].status.asInstanceOf[String]
      s"decision-tree__edge decision-tree__edge--$s"
    val sel = svg.selectAll("path.decision-tree__edge").data(data, keyFn)
    val _ = sel
      .enter()
      .append("path")
      .attr("class", classFn)
      .attr("d", dFn)
      .attr("opacity", 0)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("path.decision-tree__edge")
    val _   = all.attr("class", classFn).attr("d", dFn)
    val _   = sel.exit().remove()

  // ── Edge labels ───────────────────────────────────────────────────────────
  // Author-supplied `edgeLabel` on the child node. Sits at the midpoint of
  // the edge, slightly above the line. Keyed by child id.

  private def renderEdgeLabels(
      svg: D3.Selection,
      spec: Spec,
      state: WalkState,
      nodeCenterX: String => Double,
      nodeY: String => Double,
      fadeDur: Double
  ): Unit =
    val nodeIds = spec.nodes.iterator.map(_.id).toSet
    val labelled = spec.nodes.flatMap { n =>
      for
        pid <- n.parent.filter(nodeIds.contains)
        lbl <- n.edgeLabel
      yield (pid, n.id, lbl)
    }
    val data: js.Array[js.Any] = labelled.map { case (pid, cid, lbl) =>
      val midX = (nodeCenterX(pid) + nodeCenterX(cid)) / 2.0
      // Midpoint y is between parent's bottom and child's top; we draw at the
      // midpoint of the FULL parent→child segment minus a small upward bias
      // so the label sits above the line.
      val midY = (nodeY(pid) + NodeHeight + nodeY(cid)) / 2.0 - EdgeLabelGapY
      js.Dynamic
        .literal(
          id = cid,
          text = lbl,
          x = midX,
          y = midY,
          dim = state.nodeStatus.getOrElse(cid, StatusUnseen) match
            case StatusUnseen | StatusPopped | StatusPruned => true
            case _                                          => false
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] =
      (d, _) => s"label-${d.asInstanceOf[js.Dynamic].id.asInstanceOf[String]}"
    val xFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].x
    val yFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].y
    val textFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].text
    val classFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val dim = d.asInstanceOf[js.Dynamic].dim.asInstanceOf[Boolean]
      if dim then "decision-tree__edge-label decision-tree__edge-label--dim"
      else "decision-tree__edge-label"
    val sel = svg.selectAll("text.decision-tree__edge-label").data(data, keyFn)
    val _ = sel
      .enter()
      .append("text")
      .attr("class", classFn)
      .attr("text-anchor", "middle")
      .attr("x", xFn)
      .attr("y", yFn)
      .attr("opacity", 0)
      .text(textFn)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("text.decision-tree__edge-label")
    val _   = all.attr("class", classFn).attr("x", xFn).attr("y", yFn).text(textFn)
    val _   = sel.exit().remove()

  // ── Transient backtrack arc ───────────────────────────────────────────────
  // Lives one step. When the current event is `backtrack`, draw a curved
  // Bézier from the popped child back up to the parent. The arc fades in
  // along with the step, then is removed on the next render (because the
  // next state's `lastBacktrack` is None or refers to a different pair).
  // Keyed by `${childId}->${parentId}` so a consecutive backtrack draws a
  // different element rather than morphing the old one.

  private def renderBacktrackArc(
      svg: D3.Selection,
      state: WalkState,
      nodeCenterX: String => Double,
      nodeBottomY: String => Double,
      nodeTopY: String => Double,
      fadeDur: Double
  ): Unit =
    val data: js.Array[js.Any] = state.lastBacktrack match
      case Some((childId, parentId)) =>
        val cx = nodeCenterX(childId)
        val cy = nodeTopY(childId)
        val px = nodeCenterX(parentId)
        val py = nodeBottomY(parentId)
        // Bias the control point off to the side so the arc reads as a
        // distinct return path rather than overlapping the down-edge.
        val ctrlX = (cx + px) / 2.0 + math.max(28.0, math.abs(cx - px) * 0.35)
        val ctrlY = (cy + py) / 2.0
        js.Array(
          js.Dynamic
            .literal(
              id = s"$childId->$parentId",
              d = s"M $cx $cy Q $ctrlX $ctrlY $px $py"
            )
            .asInstanceOf[js.Any]
        )
      case None => js.Array[js.Any]()
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val dFn: js.Function2[js.Any, Int, js.Any]   = (d, _) => d.asInstanceOf[js.Dynamic].d
    val sel = svg.selectAll("path.decision-tree__backtrack-arc").data(data, keyFn)
    val _ = sel
      .enter()
      .append("path")
      .attr("class", "decision-tree__backtrack-arc")
      .attr("d", dFn)
      .attr("opacity", 0)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("path.decision-tree__backtrack-arc")
    val _   = all.attr("d", dFn)
    val _   = sel.exit().remove()

  // ── Nodes ─────────────────────────────────────────────────────────────────
  // Group keyed by node id. Class encodes status — `active` / `visited` /
  // `accepted` / `rejected` / `pruned` / `popped` / `unseen`. A strike-through
  // line overlays pruned nodes (drawn as part of the same group on enter, but
  // shown via CSS opacity based on the modifier class).

  private def renderNodes(
      svg: D3.Selection,
      spec: Spec,
      state: WalkState,
      nodeX: String => Double,
      nodeY: String => Double,
      fadeDur: Double
  ): Unit =
    val data: js.Array[js.Any] = spec.nodes.map { n =>
      val x      = nodeX(n.id)
      val y      = nodeY(n.id)
      val status = state.nodeStatus.getOrElse(n.id, StatusUnseen)
      js.Dynamic
        .literal(
          id = n.id,
          label = n.label,
          status = status,
          x = x,
          y = y
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val transformFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val dyn = d.asInstanceOf[js.Dynamic]
      val x   = dyn.x.asInstanceOf[Double]
      val y   = dyn.y.asInstanceOf[Double]
      s"translate($x, $y)"
    val classFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val s = d.asInstanceOf[js.Dynamic].status.asInstanceOf[String]
      s"decision-tree__node decision-tree__node--$s"
    val sel = svg.selectAll("g.decision-tree__node").data(data, keyFn)
    val enter = sel
      .enter()
      .append("g")
      .attr("class", classFn)
      .attr("transform", transformFn)
      .attr("opacity", 0)
    val _ = enter
      .append("rect")
      .attr("class", "decision-tree__node-rect")
      .attr("rx", 5)
      .attr("width", NodeWidth)
      .attr("height", NodeHeight)
    val _ = enter
      .append("text")
      .attr("class", "decision-tree__node-text")
      .attr("text-anchor", "middle")
      .attr("x", NodeWidth / 2)
      .attr("y", NodeHeight / 2 + 4)
      .text(((d, _) => d.asInstanceOf[js.Dynamic].label): js.Function2[js.Any, Int, js.Any])
    // Strike-through line — visible only when modifier class is `--pruned`
    // (CSS hides it otherwise via opacity: 0).
    val _ = enter
      .append("line")
      .attr("class", "decision-tree__node-strike")
      .attr("x1", 8)
      .attr("y1", NodeHeight / 2)
      .attr("x2", NodeWidth - 8)
      .attr("y2", NodeHeight / 2)
    // Accepted checkmark — visible only when modifier class is `--accepted`.
    val _ = enter
      .append("text")
      .attr("class", "decision-tree__node-check")
      .attr("text-anchor", "end")
      .attr("x", NodeWidth - 6)
      .attr("y", 12)
      .text("✓")
    val _   = enter.transition().duration(fadeDur).ease(D3.easeCubicInOut).attr("opacity", 1)
    val _   = sel.exit().remove()
    val all = svg.selectAll("g.decision-tree__node")
    val _   = all.attr("class", classFn)
    val _ = all
      .select("text.decision-tree__node-text")
      .text(((d, _) => d.asInstanceOf[js.Dynamic].label): js.Function2[js.Any, Int, js.Any])
    // Snap-set transform via .attr (CSS animates — pattern hint #5).
    val _ = all.attr("transform", transformFn).attr("opacity", 1)

  // ── Reason badges ─────────────────────────────────────────────────────────
  // Small pill rendered to the right of a pruned node carrying the reason
  // text. Keyed by node id; entries persist once added (the prune state
  // accumulates).

  private def renderReasonBadges(
      svg: D3.Selection,
      state: WalkState,
      nodeX: String => Double,
      nodeCenterY: String => Double,
      fadeDur: Double
  ): Unit =
    val data: js.Array[js.Any] = state.pruneReasons.toList.map { case (id, reason) =>
      val bx = nodeX(id) + NodeWidth + ReasonBadgeDx
      val by = nodeCenterY(id)
      js.Dynamic
        .literal(
          id = id,
          text = reason,
          x = bx,
          y = by
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      s"reason-${d.asInstanceOf[js.Dynamic].id.asInstanceOf[String]}"
    val transformFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val dyn = d.asInstanceOf[js.Dynamic]
      val x   = dyn.x.asInstanceOf[Double]
      val y   = dyn.y.asInstanceOf[Double]
      s"translate($x, $y)"
    val textFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].text
    val sel                                       = svg.selectAll("g.decision-tree__reason").data(data, keyFn)
    val enter = sel
      .enter()
      .append("g")
      .attr("class", "decision-tree__reason")
      .attr("transform", transformFn)
      .attr("opacity", 0)
    val _ = enter
      .append("rect")
      .attr("class", "decision-tree__reason-rect")
      .attr("rx", 8)
      .attr("x", 0)
      .attr("y", -9)
      .attr("width", 60)
      .attr("height", 18)
    val _ = enter
      .append("text")
      .attr("class", "decision-tree__reason-text")
      .attr("text-anchor", "middle")
      .attr("x", 30)
      .attr("y", 4)
      .text(textFn)
    val _   = enter.transition().duration(fadeDur).ease(D3.easeCubicInOut).attr("opacity", 1)
    val _   = sel.exit().remove()
    val all = svg.selectAll("g.decision-tree__reason")
    val _   = all.attr("transform", transformFn).attr("opacity", 1)
    val _   = all.select("text.decision-tree__reason-text").text(textFn)

  // ---------------------------------------------------------------------------
  // Component — React owns the host div; D3 owns the SVG inside it. Panels
  // (path + solutions) render as HTML aside the SVG; they re-render via
  // React state (the stepper.index dep triggers the effect).
  // ---------------------------------------------------------------------------

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .customBy { (_, specM) =>
        val stepCount = specM.value.toOption.fold(0)(_.events.size)
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs.toDouble))
      }
      .useRefToVdom[dom.html.Element]
      .useRefBy(_ => false) // hasRendered (mutable; avoids re-render cycle)
      .useEffectWithDepsBy((_, specM, stepper, _, _) =>
        (specM.value.toOption.fold(0)(_.events.size), stepper.index)
      ) { (_, specM, _, hostRef, hasRenderedRef) => (_, index) =>
        specM.value.toOption.filter(s => s.events.nonEmpty && s.nodes.nonEmpty) match
          case Some(spec) =>
            hostRef.foreach { host =>
              val svgEl                            = ensureSvg(host, spec)
              val animate                          = hasRenderedRef.value
              val childrenOf                       = buildChildrenOf(spec)
              val labelOf                          = spec.nodes.map(n => n.id -> n.label).toMap
              val (positions, maxDepth, slotCount) = computeLayout(spec, childrenOf)
              renderStep(
                svgEl,
                spec,
                index,
                animate,
                childrenOf,
                labelOf,
                positions,
                maxDepth,
                slotCount
              )
              if !hasRenderedRef.value then hasRenderedRef.value = true
            }
          case None => Callback.empty
      }
      .render { (_, specM, stepper, hostRef, _) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Decision-tree payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val count = spec.events.size
            val idx   = stepper.index
            val currentEvent =
              if count == 0 then Event(EventEnter, "", None, None, "No events defined.")
              else spec.events(idx)
            val childrenOf = buildChildrenOf(spec)
            val labelOf    = spec.nodes.map(n => n.id -> n.label).toMap
            val state      = computeState(spec, idx, childrenOf, labelOf)

            val kindBadge: VdomNode =
              <.div(
                ^.className := "decision-tree__kind-row",
                <.span(
                  ^.className := s"decision-tree__kind-badge decision-tree__kind-badge--${spec.kind}",
                  spec.kind
                )
              )

            val panels: VdomNode =
              if spec.panels.isEmpty then EmptyVdom
              else
                <.aside(
                  ^.className := "decision-tree__panels",
                  spec.panels.toVdomArray { p =>
                    val items: List[String] = p.name match
                      case "path" =>
                        state.pathIds.map(id => labelOf.getOrElse(id, id))
                      case "solutions" => state.solutions
                      case _           => Nil
                    <.section(
                      ^.key       := p.name,
                      ^.className := "decision-tree__panel",
                      <.h4(^.className := "decision-tree__panel-title", p.name),
                      if items.isEmpty then
                        <.p(^.className := "decision-tree__panel-empty", "—")
                      else
                        <.ol(
                          ^.className := "decision-tree__panel-list",
                          items.toVdomArray(item =>
                            <.li(^.key := item, ^.className := "decision-tree__panel-item", item)
                          )
                        )
                    )
                  }
                )

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "decision-tree__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Previous step",
                    ^.className  := "decision-tree__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("decision-tree__button-icon")),
                    "Prev"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.disabled   := count == 0,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "decision-tree__button decision-tree__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("decision-tree__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("decision-tree__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Next step",
                    ^.className  := "decision-tree__button",
                    "Next",
                    LucideIcons.ArrowRight(LucideIcons.withClass("decision-tree__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "decision-tree__button decision-tree__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("decision-tree__button-icon"))
                  ),
                  <.span(
                    ^.className := "decision-tree__progress",
                    s"Step ${idx + 1} / ${math.max(1, count)}"
                  )
                )

            <.div(
              ^.className := s"decision-tree decision-tree--${spec.kind} not-prose",
              spec.title
                .map(t => <.p(^.className := "decision-tree__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              kindBadge,
              <.div(
                ^.className := "decision-tree__canvas-row",
                <.div(
                  ^.className := "decision-tree__canvas"
                ).withRef(hostRef),
                panels
              ),
              <.p(
                ^.className := "decision-tree__caption",
                ^.aria.live := "polite",
                if currentEvent.msg.nonEmpty then currentEvent.msg else " "
              ),
              controls
            )
      }
