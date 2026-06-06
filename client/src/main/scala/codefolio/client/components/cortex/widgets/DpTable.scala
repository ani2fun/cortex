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
 * DP-table stepper — tenth widget in the D3 catalog. Animates a 1D or 2D dynamic-programming table in either
 * of two solver modes — bottom-up or top-down — over the same payload shape.
 *
 *   - `solver: "bottom-up"` — table fills incrementally per `fill` event. Each event highlights the filling
 *     cell, fades in transient dependency arrows from the predecessor cells listed in `depends`, and surfaces
 *     a one-line `rule` annotation below the table. At the final step an `optimalPath` overlay paints the
 *     recovered-answer cells with a halo + caption.
 *
 *   - `solver: "top-down"` — table starts mostly empty (`-`). Events drive a recursion tree rendered above
 *     the table: `call` descends into a child node, `return` writes the cell value and marks the node
 *     terminal, `cache-hit` flashes a cached cell green without descending. The tree's topology is authored
 *     in `recursionTree`; status (unseen / active / returned) is computed from the event tape.
 *
 * Per-cell status splits cleanly into two layers so a `filled` cell can become `depends-on` later without
 * losing its value or status: a PERSISTENT status (`empty` / `filled` / `on-path`) lives on the cell across
 * all steps; a TRANSIENT adornment (`filling` / `depends-on` / `cached`) is reset each step and stacked on
 * top via additional modifier classes. This dodges the terminal-status preservation problem the decision-tree
 * widget had to guard against: cells are filled exactly once, and the "transient" overlay never mutates the
 * underlying status.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":       "LCS of \"ab\" and \"acb\" — bottom-up",
 *   "solver":      "bottom-up",                  // "bottom-up" | "top-down"
 *   "dimensions":  2,                            // 1 | 2
 *   "rowLabels":   ["", "a", "b"],               // 2D only; ignored when dimensions == 1
 *   "colLabels":   ["", "a", "c", "b"],          // both 1D + 2D
 *   "initial":     [["0","0","0","0"], ...],     // R × C strings; "-" or "" means empty
 *   "events": [
 *     {"kind":"fill","cell":[1,1],"value":"1","depends":[[0,0]],"rule":"...","msg":"..."},
 *     {"kind":"call","nodeId":"r-2-3","msg":"..."},
 *     {"kind":"return","nodeId":"r-1-2","value":"1","cell":[1,2],"msg":"..."},
 *     {"kind":"cache-hit","cell":[1,2],"msg":"..."}
 *   ],
 *   "recursionTree": {
 *     "nodes": [
 *       {"id":"r-2-3","label":"lcs(2,3)"},
 *       {"id":"r-1-3","label":"lcs(1,3)","parent":"r-2-3"}
 *     ]
 *   },
 *   "optimalPath":      [[2,3],[1,2],[1,1],[0,0]],
 *   "optimalPathLabel": "LCS = \"ab\""
 * }
 * }}}
 *
 * The widget shares the canon-pointer pattern with [[LinkedList]] for keyed `<g>` selections, the
 * sibling-spread tree layout with [[DecisionTree]] (recursion tree in top-down mode), and the event-tape
 * replay shape with [[CallStack]] (cursor-based active-node tracking). No payload- supplied markers — cell
 * status is computed.
 */
object DpTable:

  // ---------------------------------------------------------------------------
  // Schema — parsed lazily from the JSON payload string. Each widget owns its
  // own schema; shared keeps Block.D3Widget structurally loose (ADR-0006).
  // ---------------------------------------------------------------------------

  // A tree node (top-down mode only). `parent` absent → root.
  final case class TreeNodeDef(id: String, label: String, parent: Option[String])

  // One event in the tape.
  //   - "fill"      (bottom-up): writes `value` into `cell`; `depends` and `rule` are
  //                 transient — drawn for this step only.
  //   - "call"      (top-down) : descend into `nodeId`. Sets node active.
  //   - "return"    (top-down) : pop `nodeId` from the cursor; mark it returned. Optionally
  //                 writes `value` into `cell` (the leaf-result write-back).
  //   - "cache-hit" (top-down) : flash `cell` cached for this step; no tree movement.
  final case class Event(
      kind: String,
      cell: Option[(Int, Int)],
      value: Option[String],
      depends: List[(Int, Int)],
      rule: Option[String],
      nodeId: Option[String],
      msg: String
  )

  // Recursion-tree topology (top-down only).
  final case class RecursionTree(nodes: List[TreeNodeDef])

  final case class Spec(
      solver: String, // "bottom-up" | "top-down"
      dimensions: Int,
      rowLabels: List[String],
      colLabels: List[String],
      initial: List[List[String]],
      events: List[Event],
      recursionTree: Option[RecursionTree],
      optimalPath: List[(Int, Int)],
      optimalPathLabel: Option[String],
      title: Option[String]
  )

  final case class Props(payload: String)

  // ---------------------------------------------------------------------------
  // Closed-set constants. Unknown values collapse to safe defaults at parse
  // time so a typo never crashes the chapter.
  // ---------------------------------------------------------------------------

  private val SolverBottomUp = "bottom-up"
  private val SolverTopDown  = "top-down"
  private val Solvers        = Set(SolverBottomUp, SolverTopDown)

  private val EventFill     = "fill"
  private val EventCall     = "call"
  private val EventReturn   = "return"
  private val EventCacheHit = "cache-hit"
  private val EventKinds    = Set(EventFill, EventCall, EventReturn, EventCacheHit)

  // Persistent cell statuses — a cell is in exactly one of these at any step.
  private val CellEmpty  = "empty"
  private val CellFilled = "filled"
  private val CellOnPath = "on-path"

  // Transient cell adornments — added as extra modifier classes on top of the persistent
  // status for the current step only. A `filled` cell can also be `depends-on` (it's a
  // predecessor of the cell being computed right now).
  private val AdornFilling   = "filling"
  private val AdornDependsOn = "depends-on"
  private val AdornCached    = "cached"

  // Tree-node statuses (top-down mode). Cursor-movement events guard against overwriting
  // terminal `returned`, same pattern as decision-tree's accepted/rejected/pruned.
  private val TreeUnseen   = "unseen"
  private val TreeActive   = "active"
  private val TreeReturned = "returned"

  // ---------------------------------------------------------------------------
  // Layout constants — sized so a typical 4×5 grid fits a prose column without
  // horizontal scroll and a 6-node recursion tree fits above without crowding.
  // ---------------------------------------------------------------------------

  private val CellWidth       = 46.0
  private val CellHeight      = 34.0
  private val CellGap         = 2.0
  private val RowLabelWidth   = 36.0 // gutter for row labels on 2D tables
  private val ColLabelHeight  = 20.0
  private val PaddingX        = 12.0
  private val PaddingY        = 12.0
  private val TreeNodeWidth   = 72.0
  private val TreeNodeHeight  = 26.0
  private val TreeSlotGap     = 20.0
  private val TreeLevelHeight = 56.0
  private val TreeTablePad    = 18.0 // gap between tree and table in top-down mode
  private val StepDelayMs     = 1700
  private val FadeMs          = 350.0
  private val SvgNs           = "http://www.w3.org/2000/svg"

  // ---------------------------------------------------------------------------
  // Parsing — each parser is small + total; PayloadDecoder.run collapses any
  // thrown exception into Left(message) and the renderer shows an inline error.
  // ---------------------------------------------------------------------------

  // [r, c] tuple — first two array entries coerced to ints. Returns None if the
  // array is missing or has fewer than 2 entries.
  private def parseCellRef(any: js.Any): Option[(Int, Int)] =
    any.asInstanceOf[js.UndefOr[js.Array[js.Any]]].toOption match
      case Some(arr) if arr.length >= 2 =>
        val r = js.Dynamic.global.Number(arr(0)).asInstanceOf[Double].toInt
        val c = js.Dynamic.global.Number(arr(1)).asInstanceOf[Double].toInt
        Some((r, c))
      case _ => None

  private def parseCellList(d: js.Dynamic, field: String): List[(Int, Int)] =
    d.selectDynamic(field)
      .asInstanceOf[js.UndefOr[js.Array[js.Any]]]
      .toOption
      .fold(List.empty[(Int, Int)])(_.toList.flatMap(parseCellRef))

  private def parseOptCellRef(d: js.Dynamic, field: String): Option[(Int, Int)] =
    parseCellRef(d.selectDynamic(field).asInstanceOf[js.Any])

  private def parseEvent(d: js.Dynamic): Event =
    val rawKind = d.optString("kind").getOrElse(EventFill)
    val kind    = if EventKinds.contains(rawKind) then rawKind else EventFill
    Event(
      kind = kind,
      cell = parseOptCellRef(d, "cell"),
      value = d.optString("value"),
      depends = parseCellList(d, "depends"),
      rule = d.optString("rule"),
      nodeId = d.optString("nodeId"),
      msg = d.string("msg")
    )

  private def parseTreeNode(d: js.Dynamic): TreeNodeDef =
    TreeNodeDef(
      id = d.string("id"),
      label = d.string("label"),
      parent = d.optString("parent")
    )

  private def parseRecursionTree(d: js.Dynamic): RecursionTree =
    val nodes = d.dynList("nodes").map(parseTreeNode).filter(_.id.nonEmpty)
    RecursionTree(nodes = nodes)

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val rawSolver = d.optString("solver").getOrElse(SolverBottomUp)
      val solver    = if Solvers.contains(rawSolver) then rawSolver else SolverBottomUp
      val rawDims   = d.optInt("dimensions").getOrElse(2)
      val dims      = if rawDims == 1 then 1 else 2
      val rowLabels = d.stringList("rowLabels").getOrElse(Nil)
      val colLabels = d.stringList("colLabels").getOrElse(Nil)
      val initial   = d.stringMatrix("initial").getOrElse(Nil)
      if initial.isEmpty then throw PayloadDecoder.invalid("initial must be non-empty")
      val events = d.dynList("events").map(parseEvent)
      if events.isEmpty then throw PayloadDecoder.invalid("events must be non-empty")
      val recursionTree = d.optObj("recursionTree").map(parseRecursionTree)
      val optimalPath   = parseCellList(d, "optimalPath")
      Spec(
        solver = solver,
        dimensions = dims,
        rowLabels = rowLabels,
        colLabels = colLabels,
        initial = initial,
        events = events,
        recursionTree = recursionTree,
        optimalPath = optimalPath,
        optimalPathLabel = d.optString("optimalPathLabel"),
        title = d.optString("title")
      )
    }

  // ---------------------------------------------------------------------------
  // Per-step state — computed by replaying events [0..stepIdx].
  //
  // `cellValues` carries the current display string per cell — equal to the
  // initial value until a `fill` (bottom-up) or a `return cell=..` (top-down)
  // overrides it.
  //
  // `cellStatus` carries the PERSISTENT status (`empty` / `filled` / `on-path`).
  // The transient adornments (`filling` / `depends-on` / `cached`) live in the
  // separate "current-step" fields below — they only apply to the step the
  // reader is looking at, never accumulate.
  //
  // Tree status fields apply in top-down mode only and follow the
  // terminal-status preservation rule from the decision-tree post-ship audit:
  // cursor-movement events (`call`) MUST guard against overwriting a node
  // that's already `returned`.
  // ---------------------------------------------------------------------------

  final case class TableState(
      cellValues: Map[(Int, Int), String],
      cellStatus: Map[(Int, Int), String],
      currentFilling: Option[(Int, Int)],
      currentDepends: List[(Int, Int)],
      currentRule: Option[String],
      currentCached: Option[(Int, Int)],
      // Top-down only.
      treeStatus: Map[String, String],
      activeNode: Option[String],
      cursorPath: List[String], // root → active (debug / verification)
      onPath: Set[(Int, Int)]   // applied at final step when optimalPath is non-empty
  )

  private def emptyState(spec: Spec): TableState =
    val rows = spec.initial
    val cells =
      for
        r <- rows.indices
        c <- rows(r).indices
      yield (r, c) -> rows(r)(c)
    val values = cells.toMap
    val statuses = values.map { case (rc, v) =>
      rc -> (if isEmptyValue(v) then CellEmpty else CellFilled)
    }
    val treeStatus = spec.recursionTree.fold(Map.empty[String, String]) { rt =>
      rt.nodes.map(_.id -> TreeUnseen).toMap
    }
    TableState(
      cellValues = values,
      cellStatus = statuses,
      currentFilling = None,
      currentDepends = Nil,
      currentRule = None,
      currentCached = None,
      treeStatus = treeStatus,
      activeNode = None,
      cursorPath = Nil,
      onPath = Set.empty
    )

  // A cell's "value" is "empty" if it's the canonical placeholder. The widget
  // accepts both "" and "-" so authors can use whichever is more readable for
  // a given table (knapsack uses "-", LCS uses "-", boolean DPs use "F"/"T"
  // and there's no empty representation).
  private def isEmptyValue(s: String): Boolean = s.isEmpty || s == "-"

  // Reset the per-step transient adornments. Called at the head of every
  // applyEvent — even non-fill events flush the previous step's filling /
  // depends / rule / cached overlays.
  private def clearTransient(state: TableState): TableState =
    state.copy(
      currentFilling = None,
      currentDepends = Nil,
      currentRule = None,
      currentCached = None
    )

  private def applyEvent(
      state: TableState,
      event: Event
  ): TableState =
    val base = clearTransient(state)
    event.kind match
      case EventFill =>
        // Bottom-up: write the cell value AND mark it persistently filled, plus
        // record the transient adornments (current filling + depends + rule).
        event.cell match
          case Some(rc) =>
            val newValues = event.value.fold(base.cellValues)(v => base.cellValues.updated(rc, v))
            val newStatus = base.cellStatus.updated(rc, CellFilled)
            base.copy(
              cellValues = newValues,
              cellStatus = newStatus,
              currentFilling = Some(rc),
              currentDepends = event.depends,
              currentRule = event.rule
            )
          case None => base
      case EventCall =>
        // Top-down: descend into the named tree node. Demote the previous
        // active to "unseen-or-better" — actually leave it alone; the visit
        // happens implicitly when we return. Mark new node active UNLESS it's
        // already returned (terminal status preservation, lesson from
        // DecisionTree Payload 2 audit).
        event.nodeId match
          case Some(id) =>
            val updatedStatus = base.treeStatus.get(id) match
              case Some(TreeReturned) => base.treeStatus
              case _                  => base.treeStatus.updated(id, TreeActive)
            base.copy(
              treeStatus = updatedStatus,
              activeNode = Some(id),
              cursorPath = base.cursorPath :+ id
            )
          case None => base
      case EventReturn =>
        // Top-down: pop the cursor; mark the popped node `returned`; optionally
        // write the value into the named cell.
        event.nodeId match
          case Some(id) =>
            val newStatus = base.treeStatus.updated(id, TreeReturned)
            val newPath =
              if base.cursorPath.lastOption.contains(id) then base.cursorPath.init else base.cursorPath
            val newActive = newPath.lastOption
            val (newValues, newCellStatus) = event.cell match
              case Some(rc) =>
                val vs = event.value.fold(base.cellValues)(v => base.cellValues.updated(rc, v))
                (vs, base.cellStatus.updated(rc, CellFilled))
              case None => (base.cellValues, base.cellStatus)
            base.copy(
              treeStatus = newStatus,
              activeNode = newActive,
              cursorPath = newPath,
              cellValues = newValues,
              cellStatus = newCellStatus
            )
          case None => base
      case EventCacheHit =>
        // Top-down: flash the named cell green for one step. No tree movement.
        base.copy(currentCached = event.cell)
      case _ => base

  // Final-step overlay: paint the optimal-path cells with `on-path`. Called
  // AFTER applyEvent walks every event. The overlay is purely visual — it
  // doesn't disturb cellValues.
  private def applyOptimalPath(state: TableState, optimalPath: Set[(Int, Int)]): TableState =
    if optimalPath.isEmpty then state
    else
      val newStatus = optimalPath.foldLeft(state.cellStatus)((m, rc) => m.updated(rc, CellOnPath))
      state.copy(cellStatus = newStatus, onPath = optimalPath)

  // Children-of map for the recursion tree (empty in bottom-up mode).
  private def buildChildrenOf(spec: Spec): Map[String, List[String]] =
    spec.recursionTree match
      case None => Map.empty
      case Some(rt) =>
        val byId        = rt.nodes.map(_.id).toSet
        val accumulator = mutable.Map.empty[String, mutable.ArrayBuffer[String]]
        for n <- rt.nodes do
          n.parent match
            case Some(pid) if byId.contains(pid) =>
              accumulator.getOrElseUpdate(pid, mutable.ArrayBuffer.empty[String]) += n.id
            case _ => ()
        accumulator.view.mapValues(_.toList).toMap

  private def computeState(spec: Spec, stepIdx: Int): TableState =
    var s     = emptyState(spec)
    val bound = math.min(stepIdx, spec.events.size - 1)
    for i <- 0 to bound do s = applyEvent(s, spec.events(i))
    val isFinal = bound == spec.events.size - 1
    if isFinal && spec.optimalPath.nonEmpty then applyOptimalPath(s, spec.optimalPath.toSet)
    else s

  // ---------------------------------------------------------------------------
  // Layout — table cell positions and (in top-down mode) the recursion-tree
  // node positions. Both are pure functions of the spec — they don't depend
  // on the current step.
  // ---------------------------------------------------------------------------

  // (xOffset, yOffset) of the top-left of the cell grid. In 2D mode we leave a
  // gutter for row labels on the left; in 1D mode we don't.
  private def gridOrigin(spec: Spec): (Double, Double) =
    val xOff = PaddingX + (if spec.dimensions == 2 then RowLabelWidth else 0.0)
    val yOff = PaddingY + ColLabelHeight
    (xOff, yOff)

  private def cellX(spec: Spec, col: Int): Double =
    val (xOff, _) = gridOrigin(spec)
    xOff + col.toDouble * (CellWidth + CellGap)

  private def cellY(spec: Spec, row: Int, treeH: Double): Double =
    val (_, yOff) = gridOrigin(spec)
    treeH + yOff + row.toDouble * (CellHeight + CellGap)

  private def tableWidth(spec: Spec): Double =
    val (xOff, _) = gridOrigin(spec)
    val nCols     = spec.initial.head.length
    xOff + nCols.toDouble * (CellWidth + CellGap) - CellGap + PaddingX

  private def tableHeight(spec: Spec, treeH: Double): Double =
    val (_, yOff) = gridOrigin(spec)
    val nRows     = spec.initial.length
    treeH + yOff + nRows.toDouble * (CellHeight + CellGap) - CellGap + PaddingY

  // ── Recursion-tree layout (top-down mode only) ────────────────────────────
  // Same Reingold-Tilford-flavoured packing as DecisionTree: leaves take the
  // next free slot; internal nodes sit at the midpoint of their children's
  // slot range.

  private def computeTreeLayout(
      spec: Spec,
      childrenOf: Map[String, List[String]]
  ): (Map[String, (Double, Int)], Int, Double) =
    spec.recursionTree match
      case None => (Map.empty, 0, 0.0)
      case Some(rt) =>
        val byId      = rt.nodes.map(n => n.id -> n).toMap
        val roots     = rt.nodes.filter(n => n.parent.forall(p => !byId.contains(p))).map(_.id)
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

  private def treeBlockHeight(spec: Spec, maxDepth: Int): Double =
    spec.recursionTree match
      case None    => 0.0
      case Some(_) => (maxDepth + 1).toDouble * TreeLevelHeight + TreeTablePad + PaddingY

  // ---------------------------------------------------------------------------
  // SVG bootstrap — first paint only.
  // ---------------------------------------------------------------------------

  private def ensureSvg(host: dom.html.Element, spec: Spec): dom.Element =
    val existing = host.querySelector("svg")
    if existing != null then existing.asInstanceOf[dom.Element]
    else
      val svg = dom.document.createElementNS(SvgNs, "svg").asInstanceOf[dom.Element]
      svg.setAttribute("class", "dp-table__svg")
      svg.setAttribute("role", "img")
      svg.setAttribute("aria-label", spec.title.getOrElse(s"DP table (${spec.solver})"))
      svg.setAttribute("xmlns", SvgNs)
      svg.setAttribute("viewBox", "0 0 400 200")
      svg.setAttribute("width", "400")
      svg.setAttribute("height", "200")
      host.appendChild(svg)
      svg

  // ---------------------------------------------------------------------------
  // Per-step render — idempotent. Cells keyed by `r-c`; dep arrows keyed by
  // `r1-c1->r2-c2`; tree nodes keyed by id; tree edges keyed by `pid->cid`.
  // ---------------------------------------------------------------------------

  private def renderStep(
      svgEl: dom.Element,
      spec: Spec,
      stepIdx: Int,
      animate: Boolean,
      treePositions: Map[String, (Double, Int)],
      treeMaxDepth: Int,
      treeSlotCount: Double
  ): Unit =
    val svg     = D3.select(svgEl)
    val state   = computeState(spec, stepIdx)
    val fadeDur = if animate then FadeMs else 0.0

    val treeH = treeBlockHeight(spec, treeMaxDepth)
    val w     = math.max(tableWidth(spec), treeMinWidth(spec, treeSlotCount))
    val h     = tableHeight(spec, treeH)

    val _ = svg.attr("viewBox", s"0 0 $w $h")
    val _ = svg.attr("width", w.toString)
    val _ = svg.attr("height", h.toString)

    renderColLabels(svg, spec, treeH, fadeDur)
    renderRowLabels(svg, spec, treeH, fadeDur)
    renderCells(svg, spec, state, treeH, fadeDur)
    renderDepArrows(svg, spec, state, treeH, fadeDur)
    if spec.recursionTree.isDefined then
      renderTreeEdges(svg, spec, state, treePositions, fadeDur)
      renderTreeNodes(svg, spec, state, treePositions, fadeDur)

  // Tree-mode SVG must be at least wide enough for the tree (which can span
  // more than the table). Returns the minimum width the tree block needs.
  private def treeMinWidth(spec: Spec, slotCount: Double): Double =
    if spec.recursionTree.isEmpty then 0.0
    else PaddingX * 2 + math.max(1.0, slotCount) * (TreeNodeWidth + TreeSlotGap) - TreeSlotGap

  // ── Column labels ─────────────────────────────────────────────────────────
  // Drawn above the table. Keyed by column index so a relabel propagates.
  // Author-supplied `colLabels`; missing slots fall back to "" (empty).

  private def renderColLabels(
      svg: D3.Selection,
      spec: Spec,
      treeH: Double,
      fadeDur: Double
  ): Unit =
    val nCols = spec.initial.head.length
    val data: js.Array[js.Any] = (0 until nCols).map { c =>
      val label = spec.colLabels.lift(c).getOrElse("")
      js.Dynamic
        .literal(
          id = s"col-$c",
          text = label,
          x = cellX(spec, c) + CellWidth / 2.0,
          y = treeH + PaddingY + ColLabelHeight - 6.0
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any]  = (d, _) => d.asInstanceOf[js.Dynamic].id
    val xFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].x
    val yFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].y
    val textFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].text
    val sel = svg.selectAll("text.dp-table__col-label").data(data, keyFn)
    val _ = sel
      .enter()
      .append("text")
      .attr("class", "dp-table__col-label")
      .attr("text-anchor", "middle")
      .attr("x", xFn)
      .attr("y", yFn)
      .text(textFn)
      .attr("opacity", 0)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("text.dp-table__col-label")
    val _   = all.attr("x", xFn).attr("y", yFn).text(textFn)
    val _   = sel.exit().remove()

  // ── Row labels (2D only) ──────────────────────────────────────────────────
  // Drawn in the left gutter. Keyed by row index.

  private def renderRowLabels(
      svg: D3.Selection,
      spec: Spec,
      treeH: Double,
      fadeDur: Double
  ): Unit =
    if spec.dimensions == 1 then
      // Remove any row labels left from a prior render (defensive — we don't
      // expect the spec to change mid-session, but it costs nothing).
      val _ = svg.selectAll("text.dp-table__row-label").data(js.Array[js.Any]()).exit().remove()
      return ()
    val nRows = spec.initial.length
    val data: js.Array[js.Any] = (0 until nRows).map { r =>
      val label     = spec.rowLabels.lift(r).getOrElse("")
      val (xOff, _) = gridOrigin(spec)
      js.Dynamic
        .literal(
          id = s"row-$r",
          text = label,
          x = xOff - 8.0,
          y = cellY(spec, r, treeH) + CellHeight / 2.0 + 4.0
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any]  = (d, _) => d.asInstanceOf[js.Dynamic].id
    val xFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].x
    val yFn: js.Function2[js.Any, Int, js.Any]    = (d, _) => d.asInstanceOf[js.Dynamic].y
    val textFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].text
    val sel = svg.selectAll("text.dp-table__row-label").data(data, keyFn)
    val _ = sel
      .enter()
      .append("text")
      .attr("class", "dp-table__row-label")
      .attr("text-anchor", "end")
      .attr("x", xFn)
      .attr("y", yFn)
      .text(textFn)
      .attr("opacity", 0)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("text.dp-table__row-label")
    val _   = all.attr("x", xFn).attr("y", yFn).text(textFn)
    val _   = sel.exit().remove()

  // ── Cells ─────────────────────────────────────────────────────────────────
  // Each cell is a `<g>` with `<rect>` + `<text>`. Class encodes the persistent
  // status plus any current-step adornment(s). Keyed by `r-c`.

  private def renderCells(
      svg: D3.Selection,
      spec: Spec,
      state: TableState,
      treeH: Double,
      fadeDur: Double
  ): Unit =
    val nRows = spec.initial.length
    val nCols = spec.initial.head.length
    val data: js.Array[js.Any] = (for
      r <- 0 until nRows
      c <- 0 until nCols
    yield
      val rc        = (r, c)
      val value     = state.cellValues.getOrElse(rc, spec.initial(r).lift(c).getOrElse(""))
      val status    = state.cellStatus.getOrElse(rc, CellEmpty)
      val isFilling = state.currentFilling.contains(rc)
      val isDepends = state.currentDepends.contains(rc)
      val isCached  = state.currentCached.contains(rc)
      js.Dynamic
        .literal(
          id = s"cell-$r-$c",
          row = r,
          col = c,
          value = value,
          status = status,
          isFilling = isFilling,
          isDepends = isDepends,
          isCached = isCached,
          x = cellX(spec, c),
          y = cellY(spec, r, treeH)
        )
        .asInstanceOf[js.Any]
    ).toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val transformFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val dyn = d.asInstanceOf[js.Dynamic]
      val x   = dyn.x.asInstanceOf[Double]
      val y   = dyn.y.asInstanceOf[Double]
      s"translate($x, $y)"
    val classFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val dyn       = d.asInstanceOf[js.Dynamic]
      val status    = dyn.status.asInstanceOf[String]
      val filling   = dyn.isFilling.asInstanceOf[Boolean]
      val depends   = dyn.isDepends.asInstanceOf[Boolean]
      val cached    = dyn.isCached.asInstanceOf[Boolean]
      val base      = s"dp-table__cell dp-table__cell--$status"
      val withFill  = if filling then s"$base dp-table__cell--$AdornFilling" else base
      val withDep   = if depends then s"$withFill dp-table__cell--$AdornDependsOn" else withFill
      val withCache = if cached then s"$withDep dp-table__cell--$AdornCached" else withDep
      withCache
    val textFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val v = d.asInstanceOf[js.Dynamic].value.asInstanceOf[String]
      if v.isEmpty then "-" else v
    val sel = svg.selectAll("g.dp-table__cell").data(data, keyFn)
    val enter = sel
      .enter()
      .append("g")
      .attr("class", classFn)
      .attr("transform", transformFn)
      .attr("opacity", 0)
    val _ = enter
      .append("rect")
      .attr("class", "dp-table__cell-rect")
      .attr("rx", 3)
      .attr("width", CellWidth)
      .attr("height", CellHeight)
    val _ = enter
      .append("text")
      .attr("class", "dp-table__cell-text")
      .attr("text-anchor", "middle")
      .attr("x", CellWidth / 2.0)
      .attr("y", CellHeight / 2.0 + 4.0)
      .text(textFn)
    val _   = enter.transition().duration(fadeDur).ease(D3.easeCubicInOut).attr("opacity", 1)
    val _   = sel.exit().remove()
    val all = svg.selectAll("g.dp-table__cell")
    val _   = all.attr("class", classFn).attr("transform", transformFn).attr("opacity", 1)
    val _   = all.select("text.dp-table__cell-text").text(textFn)

  // ── Dependency arrows (transient — current step only) ─────────────────────
  // Drawn from each predecessor cell's centre to the filling cell's centre.
  // Keyed by `r1-c1->r2-c2`. D3's exit() automatically removes them on the
  // next step when the data array empties.

  private def renderDepArrows(
      svg: D3.Selection,
      spec: Spec,
      state: TableState,
      treeH: Double,
      fadeDur: Double
  ): Unit =
    val data: js.Array[js.Any] = state.currentFilling match
      case Some(target) =>
        state.currentDepends.map { src =>
          val (sr, sc) = src
          val (tr, tc) = target
          val x1       = cellX(spec, sc) + CellWidth / 2.0
          val y1       = cellY(spec, sr, treeH) + CellHeight / 2.0
          val x2       = cellX(spec, tc) + CellWidth / 2.0
          val y2       = cellY(spec, tr, treeH) + CellHeight / 2.0
          // Shrink the segment so the head sits at the edge of the target
          // cell, not the dead-centre — the arrow reads cleaner this way.
          val dx     = x2 - x1
          val dy     = y2 - y1
          val len    = math.max(1.0, math.sqrt(dx * dx + dy * dy))
          val ux     = dx / len
          val uy     = dy / len
          val shrink = math.min(CellWidth, CellHeight) / 2.0 - 2.0
          val endX   = x2 - ux * shrink
          val endY   = y2 - uy * shrink
          js.Dynamic
            .literal(
              id = s"dep-$sr-$sc->$tr-$tc",
              d = s"M $x1 $y1 L $endX $endY",
              endX = endX,
              endY = endY,
              ux = ux,
              uy = uy
            )
            .asInstanceOf[js.Any]
        }.toJSArray
      case None => js.Array[js.Any]()
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val dFn: js.Function2[js.Any, Int, js.Any]   = (d, _) => d.asInstanceOf[js.Dynamic].d
    val sel                                      = svg.selectAll("path.dp-table__dep-arrow").data(data, keyFn)
    val _ = sel
      .enter()
      .append("path")
      .attr("class", "dp-table__dep-arrow")
      .attr("d", dFn)
      .attr("opacity", 0)
      .transition()
      .duration(fadeDur)
      .ease(D3.easeCubicInOut)
      .attr("opacity", 1)
    val all = svg.selectAll("path.dp-table__dep-arrow")
    val _   = all.attr("d", dFn)
    val _   = sel.exit().remove()

  // ── Recursion-tree edges (top-down mode only) ─────────────────────────────
  // Straight parent→child lines. Class encodes child status so a returned
  // path stays drawn dim after the cursor backs out.

  private def renderTreeEdges(
      svg: D3.Selection,
      spec: Spec,
      state: TableState,
      positions: Map[String, (Double, Int)],
      fadeDur: Double
  ): Unit =
    val rt      = spec.recursionTree.get
    val nodeIds = rt.nodes.iterator.map(_.id).toSet
    val edges   = rt.nodes.flatMap(n => n.parent.filter(nodeIds.contains).map(p => (p, n.id)))
    val data: js.Array[js.Any] = edges.map { case (pid, cid) =>
      val px          = treeNodeCenterX(positions, pid)
      val py          = treeNodeBottomY(positions, pid)
      val cx          = treeNodeCenterX(positions, cid)
      val cy          = treeNodeTopY(positions, cid)
      val childStatus = state.treeStatus.getOrElse(cid, TreeUnseen)
      js.Dynamic
        .literal(
          id = s"tree-$pid->$cid",
          d = s"M $px $py L $cx $cy",
          status = childStatus
        )
        .asInstanceOf[js.Any]
    }.toJSArray
    val keyFn: js.Function2[js.Any, Int, js.Any] = (d, _) => d.asInstanceOf[js.Dynamic].id
    val dFn: js.Function2[js.Any, Int, js.Any]   = (d, _) => d.asInstanceOf[js.Dynamic].d
    val classFn: js.Function2[js.Any, Int, js.Any] = (d, _) =>
      val s = d.asInstanceOf[js.Dynamic].status.asInstanceOf[String]
      s"dp-table__tree-edge dp-table__tree-edge--$s"
    val sel = svg.selectAll("path.dp-table__tree-edge").data(data, keyFn)
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
    val all = svg.selectAll("path.dp-table__tree-edge")
    val _   = all.attr("class", classFn).attr("d", dFn)
    val _   = sel.exit().remove()

  // ── Recursion-tree nodes (top-down mode only) ─────────────────────────────
  // Same `<g> + <rect> + <text>` shape as DecisionTree.

  private def renderTreeNodes(
      svg: D3.Selection,
      spec: Spec,
      state: TableState,
      positions: Map[String, (Double, Int)],
      fadeDur: Double
  ): Unit =
    val rt = spec.recursionTree.get
    val data: js.Array[js.Any] = rt.nodes.map { n =>
      val (slot, depth) = positions(n.id)
      val x             = PaddingX + slot * (TreeNodeWidth + TreeSlotGap)
      val y             = PaddingY + depth.toDouble * TreeLevelHeight
      val status        = state.treeStatus.getOrElse(n.id, TreeUnseen)
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
      s"dp-table__tree-node dp-table__tree-node--$s"
    val sel = svg.selectAll("g.dp-table__tree-node").data(data, keyFn)
    val enter = sel
      .enter()
      .append("g")
      .attr("class", classFn)
      .attr("transform", transformFn)
      .attr("opacity", 0)
    val _ = enter
      .append("rect")
      .attr("class", "dp-table__tree-node-rect")
      .attr("rx", 4)
      .attr("width", TreeNodeWidth)
      .attr("height", TreeNodeHeight)
    val _ = enter
      .append("text")
      .attr("class", "dp-table__tree-node-text")
      .attr("text-anchor", "middle")
      .attr("x", TreeNodeWidth / 2.0)
      .attr("y", TreeNodeHeight / 2.0 + 4.0)
      .text(((d, _) => d.asInstanceOf[js.Dynamic].label): js.Function2[js.Any, Int, js.Any])
    val _   = enter.transition().duration(fadeDur).ease(D3.easeCubicInOut).attr("opacity", 1)
    val all = svg.selectAll("g.dp-table__tree-node")
    val _   = all.attr("class", classFn).attr("transform", transformFn).attr("opacity", 1)
    val _ = all
      .select("text.dp-table__tree-node-text")
      .text(((d, _) => d.asInstanceOf[js.Dynamic].label): js.Function2[js.Any, Int, js.Any])
    val _ = sel.exit().remove()

  private def treeNodeCenterX(positions: Map[String, (Double, Int)], id: String): Double =
    val (slot, _) = positions(id)
    PaddingX + slot * (TreeNodeWidth + TreeSlotGap) + TreeNodeWidth / 2.0

  private def treeNodeTopY(positions: Map[String, (Double, Int)], id: String): Double =
    val (_, depth) = positions(id)
    PaddingY + depth.toDouble * TreeLevelHeight

  private def treeNodeBottomY(positions: Map[String, (Double, Int)], id: String): Double =
    treeNodeTopY(positions, id) + TreeNodeHeight

  // ---------------------------------------------------------------------------
  // Component — React owns the host div; D3 owns the SVG inside it. Solver
  // badge + rule annotation + optimal-path caption render as HTML siblings.
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
      .useRefBy(_ => false) // hasRendered
      .useEffectWithDepsBy((_, specM, stepper, _, _) =>
        (specM.value.toOption.fold(0)(_.events.size), stepper.index)
      ) { (_, specM, _, hostRef, hasRenderedRef) => (_, index) =>
        specM.value.toOption.filter(s => s.events.nonEmpty && s.initial.nonEmpty) match
          case Some(spec) =>
            hostRef.foreach { host =>
              val svgEl                                  = ensureSvg(host, spec)
              val animate                                = hasRenderedRef.value
              val childrenOf                             = buildChildrenOf(spec)
              val (treePositions, treeMaxDepth, slotCnt) = computeTreeLayout(spec, childrenOf)
              renderStep(
                svgEl,
                spec,
                index,
                animate,
                treePositions,
                treeMaxDepth,
                slotCnt
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
              <.p(^.className   := "d3-widget__error-title", "DP-table payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val count = spec.events.size
            val idx   = stepper.index
            val currentEvent =
              if count == 0 then Event(EventFill, None, None, Nil, None, None, "No events defined.")
              else spec.events(idx)
            val state   = computeState(spec, idx)
            val isFinal = idx == count - 1

            val solverBadge: VdomNode =
              <.div(
                ^.className := "dp-table__solver-row",
                <.span(
                  ^.className := s"dp-table__solver-badge dp-table__solver-badge--${spec.solver}",
                  spec.solver
                )
              )

            // Rule annotation — shown under the table while a fill event is
            // active (bottom-up). The rule comes from the current event's
            // `rule` field; if absent the strip stays blank (preserves layout
            // height so the table doesn't jump as the reader scrubs).
            val rule: VdomNode =
              <.p(
                ^.className := "dp-table__rule",
                state.currentRule.getOrElse(" ")
              )

            // Optimal-path caption — rendered as a separate strip below the
            // rule annotation. Appears only at the final step.
            val pathCaption: VdomNode =
              if isFinal && spec.optimalPath.nonEmpty then
                <.p(
                  ^.className := "dp-table__path-caption",
                  <.span(^.className := "dp-table__path-marker", "↳"),
                  " ",
                  spec.optimalPathLabel.getOrElse("optimal path")
                )
              else <.p(^.className := "dp-table__path-caption dp-table__path-caption--blank", " ")

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "dp-table__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Previous step",
                    ^.className  := "dp-table__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("dp-table__button-icon")),
                    "Prev"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.disabled   := count == 0,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "dp-table__button dp-table__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("dp-table__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("dp-table__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Next step",
                    ^.className  := "dp-table__button",
                    "Next",
                    LucideIcons.ArrowRight(LucideIcons.withClass("dp-table__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "dp-table__button dp-table__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("dp-table__button-icon"))
                  ),
                  <.span(
                    ^.className := "dp-table__progress",
                    s"Step ${idx + 1} / ${math.max(1, count)}"
                  )
                )

            <.div(
              ^.className := s"dp-table dp-table--${spec.solver} not-prose",
              spec.title
                .map(t => <.p(^.className := "dp-table__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              solverBadge,
              <.div(
                ^.className := "dp-table__canvas"
              ).withRef(hostRef),
              rule,
              pathCaption,
              <.p(
                ^.className := "dp-table__caption",
                ^.aria.live := "polite",
                if currentEvent.msg.nonEmpty then currentEvent.msg else " "
              ),
              controls
            )
      }
