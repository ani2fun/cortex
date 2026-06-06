package cortex.shared.viz

import scala.collection.mutable

/**
 * Turns a [[HeapTrace]] into a [[VizGraph]] — the one generic, data-structure-agnostic adapter (ADR-0018).
 *
 * It does no algorithm- or shape-specific work. It picks a root object, walks the object graph by reference
 * reachability, maps objects to nodes and references to edges, and derives per-step emphasis from the diff
 * between consecutive heaps. The *shape* of the drawing is entirely the layout's job — adding a data
 * structure adds a layout, never a branch here.
 */
object HeapToGraph:

  /** Instance field names treated as a node's display value, in priority order. */
  private val ValueFields = List("val", "value", "data", "key", "item")

  /** The label shown for a node / cell / entry whose primary value is a reference, not a scalar. */
  private val RefLabel = "·"

  /**
   * Local-variable names treated as an array index when they hold an integer in range — the names that
   * conventionally denote a *position* in a sequence. Deliberately an allowlist: it keeps an accumulator or a
   * size (`count`, `total`, `n`) from being mistaken for a cursor. A position-bearing local with an
   * unconventional name simply isn't drawn as a cursor — an acceptable miss; see [[indexCursors]].
   */
  private val IndexNames: Set[String] = Set(
    "i",
    "j",
    "k",
    "l",
    "r",
    "m",
    "lo",
    "hi",
    "mid",
    "low",
    "high",
    "left",
    "right",
    "start",
    "end",
    "first",
    "last",
    "p",
    "q",
    "pivot",
    "idx",
    "index",
    "pos",
    "slow",
    "fast",
    "read",
    "write",
    "front",
    "back",
    "top"
  )

  /** Python-style (snake_case) builder-frame prefixes whose steps are dropped before adapting. */
  private val HelperFnPrefixes = List("from_", "to_", "build_")

  /**
   * Java-style (camelCase) builder-frame prefixes. Matched with an uppercase-next-char gate so a local with a
   * natural name like `total` / `top` / `make` isn't accidentally pruned — only `fromList`, `toArray`,
   * `buildTree`, `makeNode`, `createGraph`, … match (the next character after the prefix must be uppercase).
   * Skip the prefix-equals case (an exact match like `make` is almost certainly the *algorithm* method, not
   * scaffolding).
   */
  private val JavaBuilderPrefixes = List("from", "to", "build", "make", "create")

  private def isJavaBuilderFrame(fn: String): Boolean =
    JavaBuilderPrefixes.exists(p =>
      fn.length > p.length && fn.startsWith(p) && Character.isUpperCase(fn.charAt(p.length))
    )

  /**
   * A builder / constructor frame, whose steps are dropped before adapting so the trace steps through the
   * *algorithm*, not the scaffolding that builds the input or constructs each node. The objects such frames
   * create still appear, fully formed, in the surviving steps. `__init__` is the Python constructor name and
   * `<init>` is what the JVM tracer emits for Java constructors (ADR-0021 §"Slice 5") — both treated as
   * helper frames.
   */
  private def isHelperFrame(fn: String): Boolean =
    fn == "__init__" || fn == "<init>" ||
      HelperFnPrefixes.exists(fn.startsWith) || isJavaBuilderFrame(fn)

  /**
   * The active (innermost) frame's locals — `frames.head.locals` if any, else `Nil`. ADR-0021 made the call
   * stack explicit in [[HeapStep]] but the adapter still reads only the active frame; recursion-aware UI
   * would walk the full `frames` list.
   */
  private def headLocals(step: HeapStep): List[(String, HeapValue)] =
    step.frames.headOption.fold(List.empty)(_.locals)

  /** The active (innermost) frame's function name — `frames.head.fn` if any, else the empty string. */
  private def headFn(step: HeapStep): String =
    step.frames.headOption.fold("")(_.fn)

  /**
   * Adapt a whole trace into one [[VizGraph]] per test case ([[VizCases]]).
   *
   * A DSA `main` usually runs several test cases back-to-back; tracing them all yields one incoherent
   * montage. `adapt` segments the trace — a case boundary is the `viz-root` variable rebound to a *fresh*
   * structure — and adapts each segment independently, so each case is its own self-contained animation.
   *
   * @param trace
   *   the decoded heap trace
   * @param source
   *   the traced Python source — supplies per-step line annotations
   * @param layoutHint
   *   forwarded to [[VizGraph.layoutHint]]; selects the renderer's layout
   * @param rootHint
   *   optional variable name whose object is the structure root; also the variable segmentation tracks
   * @param vizCase
   *   optional `viz-case=N` override forcing exactly N cases — for drivers where boundary detection misfires
   *   (`viz-case=1` collapses an over-segmented trace back to a single animation)
   * @param title
   *   the modal title
   * @param vizKind
   *   optional `viz-kind=<name>` hint from the markdown fence attribute (ADR-0024). When set, every step's
   *   [[VizGraphStep.structureType]] is stamped with this value — overriding the segment-wide inference. The
   *   client-side RENDERERS registry reads `structureType` to pick a bespoke chrome (e.g. `"stack"` →
   *   StackRenderer). `None` falls back to inference; failure to infer leaves `structureType = None` and the
   *   generic renderer takes over.
   */
  def adapt(
      trace: HeapTrace,
      source: String,
      layoutHint: String,
      rootHint: Option[String],
      vizCase: Option[Int],
      title: String,
      vizKind: Option[String] = None
  ): Either[String, VizCases] =
    if trace.steps.isEmpty then Left("The trace produced no steps.")
    else
      val prepared = synthesizeStringArrays(trace, rootHint)
      val srcLines = source.split("\n", -1).toVector
      val kept     = prepared.steps.filterNot(s => isHelperFrame(headFn(s)))
      if kept.isEmpty then Left("The trace stepped only through builder frames.")
      else
        val segments = segment(kept, rootHint, vizCase)
        val results =
          segments.map(seg =>
            adaptOne(seg, srcLines, layoutHint, rootHint, title, prepared.truncated, vizKind)
          )
        val graphs = results.collect { case Right(g) => g }
        if graphs.nonEmpty then Right(VizCases(graphs))
        else Left(results.collectFirst { case Left(m) => m }.getOrElse("The trace produced no steps."))

  /**
   * When `rootHint` names a string scalar in the active frame's locals, materialise that string as a heap
   * `Arr` of single-char cells so the existing array layout + `indexCursors` (which draws `left` / `right`
   * etc. as pointers) work out of the box. Without this, string-based two-pointer problems like
   * `palindromeChecker(s)` have nothing in the heap — the viz canvas is empty.
   *
   * The synthesised object keeps a stable id `__cf_str_<name>` per step, so when the string is unchanged
   * across steps the diff logic sees a stable structure (and pointers move over fixed cells). When the string
   * mutates (rebound to a new value), the items list changes — the diff highlights that naturally.
   *
   * Skipped if the hint is dotted (instance path), the named local is already a `Ref`, or no frame holds a
   * string scalar with that name.
   */
  private def synthesizeStringArrays(trace: HeapTrace, rootHint: Option[String]): HeapTrace =
    rootHint match
      case Some(name) if !name.contains('.') =>
        val synthId = s"__cf_str_$name"
        val rewritten = trace.steps.map { step =>
          val maybeStr = step.frames.headOption.flatMap(_.locals.collectFirst {
            case (n, HeapValue.Scalar(HeapScalar.S(v))) if n == name => v
          })
          maybeStr match
            case None => step
            case Some(s) =>
              val items   = s.iterator.map(c => HeapValue.Scalar(HeapScalar.S(c.toString))).toList
              val newHeap = step.heap + (synthId -> HeapObject.Arr(ArrKind.Lst, items))
              val newFrames = step.frames match
                case Nil => Nil
                case head :: tail =>
                  val newLocals = head.locals.map {
                    case (n, _) if n == name => (n, HeapValue.Ref(synthId))
                    case other               => other
                  }
                  head.copy(locals = newLocals) :: tail
              step.copy(heap = newHeap, frames = newFrames)
        }
        trace.copy(steps = rewritten)
      case _ => trace

  /**
   * Adapt one segment — the steps of a single test case — into a [[VizGraph]]. This is the pre-segmentation
   * `adapt` body: resolve the segment's root, expand each step's subgraph, drop the driver's empty setup /
   * teardown steps off both ends, carry the structure through interior gaps, then diff + narrate.
   */
  private def adaptOne(
      seg: List[HeapStep],
      srcLines: Vector[String],
      layoutHint: String,
      rootHint: Option[String],
      title: String,
      truncated: Boolean,
      vizKind: Option[String]
  ): Either[String, VizGraph] =
    resolveRootId(HeapTrace(seg, truncated), rootHint, layoutHint) match
      case None =>
        Left(
          "Couldn't find a structure to visualise — add a `viz-root=<variable>` hint " +
            "naming the variable that holds the data structure."
        )
      case Some(rootId) =>
        val layoutOverride =
          if KnownLayoutKinds.contains(layoutHint) then Some(layoutHint) else None
        // ADR-0024 — segment-wide structure-type hint, computed once and stamped on every step. An explicit
        // markdown `viz-kind=<name>` attribute wins; otherwise we scan the segment for marker locals on the
        // root object (works for the simple Arr-as-stack/queue/deque case). Inference for instance-chain
        // shapes (Heap, Trie, UnionFind, …) is deferred — those chapters set `viz-kind=` explicitly.
        val structureType = vizKind.orElse(inferSegmentStructureType(seg, rootId))
        // Per-step root (ADR-0027 — in-place restructuring). The segment root is
        // resolved once from the first step, but a rotation / re-parenting rebinds
        // the root variable mid-segment (an AVL `rotateRight` sets `root = child`).
        // If we kept the fixed `rootId`, the post-rotation steps would render from
        // the STALE root — which is now a leaf — greying the real tree as "removed"
        // (ADR-0025). So each step uses the hint's CURRENT target instead.
        //
        // Guard: only adopt the new target if it still REACHES the original root.
        // A rotation reorganises but keeps every node reachable from the new root,
        // so `reachableFrom(newRoot)` contains the old root → switch. A recursive
        // descent (`insert(node.left, …)` with viz-root=`node`) rebinds to a child
        // subtree that does NOT reach back up to the original root → keep the
        // segment root, so the whole tree still renders. Stable-root traces are
        // unaffected (the target equals `rootId` every step).
        def stepRootFor(step: HeapStep): String =
          rootHint
            .flatMap(h => rootIdInStep(step, h))
            .filter(t => reachableFrom(t, step.heap).contains(rootId))
            .getOrElse(rootId)
        val built =
          seg.map(step => buildStep(step, stepRootFor(step), srcLines, layoutOverride, structureType))
        val visible = carryForward(dropEmptyEnds(built))
        val steps   = colorizeCursors(diffSteps(coalesce(visible)))
        if steps.forall(_.nodes.isEmpty) then
          Left("The chosen root never held a structure during the trace.")
        else Right(VizGraph(steps, layoutHint, title, truncated))

  /**
   * Marker-local detection for the bespoke-renderer dispatch (ADR-0024). When the root object is an Arr,
   * inspect the union of every step's frame locals + function names: a `top` local OR a `push` / `pop` call
   * is a stack; `front` + `back` (both) OR `addFirst` / `pollLast` / `addLast` / `pollFirst` calls is a
   * deque; `front` alone OR `enqueue` / `dequeue` calls is a queue. Inference is intentionally conservative —
   * misses fall back to the generic renderer, mis-tags would mis-render — so authors are expected to set
   * `viz-kind=` explicitly for anything ambiguous.
   */
  private def inferSegmentStructureType(seg: List[HeapStep], rootId: String): Option[String] =
    val rootEverArr = seg.exists(_.heap.get(rootId).exists(_.isInstanceOf[HeapObject.Arr]))
    if !rootEverArr then None
    else
      val localNames = seg.iterator.flatMap(_.frames.iterator.flatMap(_.locals.iterator.map(_._1))).toSet
      val fnNames    = seg.iterator.flatMap(_.frames.iterator.map(_.fn)).toSet
      val stackFns   = Set("push", "pop", "peek")
      val dequeFns   = Set("addFirst", "addLast", "pollFirst", "pollLast", "offerFirst", "offerLast")
      val queueFns   = Set("enqueue", "dequeue", "offer", "poll")
      if localNames.contains("top") || stackFns.exists(fnNames.contains) then Some("stack")
      else if (localNames.contains("front") && localNames.contains("back")) ||
        dequeFns.exists(fnNames.contains)
      then Some("deque")
      else if localNames.contains("front") || queueFns.exists(fnNames.contains) then Some("queue")
      else None

  /**
   * Split the kept steps into one sub-trace per test case. With no `rootHint` there is no variable to track,
   * so the whole trace is one case. Otherwise a case boundary is a step where the root variable points at a
   * *fresh* structure ([[caseBoundaries]]); the first boundary just establishes case 1, the rest are the
   * split points. `vizCase` (`viz-case=N`) overrides the count: `N - 1` split points are kept, so
   * `viz-case=1` yields a single case regardless of what the heuristic found.
   */
  private def segment(
      kept: List[HeapStep],
      rootHint: Option[String],
      vizCase: Option[Int]
  ): List[List[HeapStep]] =
    rootHint match
      case None => List(kept)
      case Some(hint) =>
        val v          = kept.toVector
        val boundaries = caseBoundaries(v, hint)
        val splits = vizCase match
          case Some(n) if n >= 1 => boundaries.drop(1).take(n - 1)
          case _                 => boundaries.drop(1)
        sliceAt(v, splits)

  /**
   * The step indices where a new test case begins — the root variable rebound to a fresh structure. "Fresh"
   * is checked against the set of objects reachable from the *current* case's root at the moment that case
   * began: a new object outside that set is a new case; an object inside it is the algorithm walking its own
   * structure (a recursive `insert(root.left, …)` rebinds the parameter to a child — not a new case). The
   * first index returned establishes case 1; later ones are the boundaries between cases.
   */
  private def caseBoundaries(v: Vector[HeapStep], hint: String): List[Int] =
    val out       = mutable.ListBuffer.empty[Int]
    var caseRoot  = Option.empty[String]
    var caseReach = Set.empty[String]
    v.indices.foreach { i =>
      rootIdInStep(v(i), hint).foreach { rid =>
        if caseRoot.forall(cr => rid != cr && !caseReach.contains(rid)) then
          out += i
          caseRoot = Some(rid)
          caseReach = reachableFrom(rid, v(i).heap).toSet
      }
    }
    out.toList

  /**
   * Resolve the `viz-root` variable within a *single* step — what segmentation tracks across the trace. A
   * dotted hint (`self.heap`) walks a local then instance fields; a bare hint is tried as a local, then as an
   * instance attribute. `None` when the variable is not in scope this step (e.g. a driver line between two
   * cases) — such steps are simply skipped for boundary detection.
   */
  private def rootIdInStep(step: HeapStep, hint: String): Option[String] =
    if hint.contains('.') then
      hint.split("\\.").toList match
        case Nil => None
        case head :: rest =>
          headLocals(step)
            .collectFirst { case (n, HeapValue.Ref(id)) if n == head => id }
            .flatMap(followFields(_, rest, step.heap))
    else
      headLocals(step)
        .collectFirst { case (n, HeapValue.Ref(id)) if n == hint => id }
        .orElse(
          step.heap.values.iterator.flatMap {
            case HeapObject.Instance(_, fields) =>
              fields.collectFirst { case (f, HeapValue.Ref(id)) if f == hint => id }
            case _ => None
          }.nextOption()
        )

  /** Cut `v` at the given ascending split indices: `[0, s0)`, `[s0, s1)`, …, `[sLast, end)`. */
  private def sliceAt(v: Vector[HeapStep], splits: List[Int]): List[List[HeapStep]] =
    val bounds = 0 :: splits ::: List(v.length)
    bounds.sliding(2).collect { case List(a, b) => v.slice(a, b).toList }.toList

  /**
   * Trim empty steps off both ends of a built segment — the driver's setup lines before the case's structure
   * exists, and its teardown / next-case lines after the structure leaves scope — but KEEP the LAST leading
   * empty step. That one frame is the structure's empty / just-declared state, so the animation opens BLANK
   * and visibly grows as elements are added, instead of opening already-populated on the first mutation.
   * Interior empty steps stay; [[carryForward]] fills them with the surrounding structure (it leaves this
   * leading one blank — there is no prior step to carry forward from).
   */
  private def dropEmptyEnds(steps: List[VizGraphStep]): List[VizGraphStep] =
    val noTrailing      = steps.reverse.dropWhile(_.nodes.isEmpty).reverse
    val (leading, rest) = noTrailing.span(_.nodes.isEmpty)
    leading.lastOption.toList ::: rest

  /**
   * The structure root. A `rootHint` is resolved three ways, so it can name more than "a local pointing at a
   * tree": a dotted path (`self.heap`) walks a local then instance fields; a bare name is tried first as a
   * local variable, then as an instance attribute (a heap kept in `self.heap` whose owner the author names by
   * the attribute alone). An array-valued local needs no special case — it is a local holding a `Ref`, and
   * `buildStep` expands whatever object it points at. Falls back to auto-detection.
   */
  private def resolveRootId(
      trace: HeapTrace,
      rootHint: Option[String],
      layoutHint: String
  ): Option[String] =
    val byHint = rootHint.flatMap { hint =>
      if hint.contains('.') then resolveDotted(trace, hint)
      else resolveLocal(trace, hint).orElse(resolveAttr(trace, hint))
    }
    byHint.orElse(autoDetectRoot(trace, layoutHint))

  /** The object id of the first local variable named `name` that holds a reference. */
  private def resolveLocal(trace: HeapTrace, name: String): Option[String] =
    trace.steps.iterator.flatMap(headLocals).collectFirst {
      case (n, HeapValue.Ref(id)) if n == name => id
    }

  /** The object id of the first instance attribute named `name` — e.g. a heap's `self.heap` list. */
  private def resolveAttr(trace: HeapTrace, name: String): Option[String] =
    trace.steps.iterator
      .flatMap(_.heap.values)
      .collect { case HeapObject.Instance(_, fields) => fields }
      .flatMap(_.collectFirst { case (f, HeapValue.Ref(id)) if f == name => id })
      .nextOption()

  /**
   * Resolve a dotted path `a.b.c` — local `a`, then instance field `b`, then field `c` — in the first step
   * where the whole chain holds. Lets `viz-root=self.heap` name a structure held behind an instance.
   */
  private def resolveDotted(trace: HeapTrace, path: String): Option[String] =
    path.split("\\.").toList match
      case Nil => None
      case head :: rest =>
        trace.steps.iterator.flatMap { step =>
          headLocals(step)
            .collectFirst { case (n, HeapValue.Ref(id)) if n == head => id }
            .flatMap(followFields(_, rest, step.heap))
        }.nextOption()

  /** Follow a chain of instance-field names from `id`; `None` if a segment is missing or non-instance. */
  private def followFields(
      id: String,
      fields: List[String],
      heap: Map[String, HeapObject]
  ): Option[String] =
    fields match
      case Nil => Some(id)
      case seg :: rest =>
        heap.get(id) match
          case Some(HeapObject.Instance(_, fs)) =>
            fs.collectFirst { case (f, HeapValue.Ref(to)) if f == seg => to }
              .flatMap(followFields(_, rest, heap))
          case _ => None

  /**
   * Auto-detect (no explicit `viz-root`). When the layout is array-shaped (`viz=array`), first look for an
   * `Arr` bound to a frame local in ANY step — a function-scoped array (e.g. a `def solve(arr)` parameter) is
   * popped before the final module-level step, so a final-heap-only scan would miss it and a competing dict
   * would win the reachability tiebreak, forcing the array layout onto a non-array (empty canvas — the "bare
   * `viz=array` next to a hashmap" bug). Otherwise fall back to the in-degree-0 object with the largest
   * reachable set in the final heap.
   */
  private def autoDetectRoot(trace: HeapTrace, layoutHint: String): Option[String] =
    val arrayRoot =
      if layoutHint.toLowerCase.contains("array") then
        trace.steps.iterator.flatMap { s =>
          headLocals(s).collectFirst {
            case (_, HeapValue.Ref(id)) if s.heap.get(id).exists(_.isInstanceOf[HeapObject.Arr]) => id
          }
        }.nextOption()
      else None
    arrayRoot.orElse {
      val heap = trace.steps.last.heap
      if heap.isEmpty then None
      else
        val referenced = heap.values.flatMap(outRefs).toSet
        val roots      = heap.keySet.diff(referenced)
        val pool       = if roots.nonEmpty then roots else heap.keySet
        pool.maxByOption(id => reachableFrom(id, heap).size)
    }

  /**
   * The eyebrow kind for a trace step's `event`. Maps the harness's raw event string into one of the four
   * canvas-annotation eyebrows (call / line / return / exception); anything unrecognised collapses to
   * `"line"` so the annotation still renders with neutral muted colour.
   */
  private def eyebrowOf(event: String): String = event match
    case "call"      => "call"
    case "return"    => "return"
    case "exception" => "exception"
    case _           => "line"

  /** One step → the subgraph of objects reachable from `rootId`, each expanded to its node(s). */
  private def buildStep(
      step: HeapStep,
      rootId: String,
      srcLines: Vector[String],
      layoutOverride: Option[String],
      structureType: Option[String]
  ): VizGraphStep =
    val srcLine =
      if step.line >= 1 && step.line <= srcLines.size then srcLines(step.line - 1).trim else ""
    val annotation = Annotation(eyebrow = eyebrowOf(step.event), title = "", body = srcLine)
    val vizFrames  = buildFrames(step)
    if !step.heap.contains(rootId) then
      VizGraphStep(
        Nil,
        Nil,
        Nil,
        Nil,
        Nil,
        Nil,
        annotation,
        step.line,
        frames = vizFrames,
        structureType = structureType
      )
    else
      val reachable = reachableFrom(rootId, step.heap)
      val rawNodes =
        reachable.flatMap(id => step.heap.get(id).toList.flatMap(nodesOf(id, _, step.heap)))
      val nodeIds = rawNodes.iterator.map(_.id).toSet
      val edges =
        reachable.flatMap(id => step.heap.get(id).toList.flatMap(edgesOf(id, _, step.heap, nodeIds)))
      val cardByObj  = groupCards(reachable, step.heap)
      val kindByCard = inferLayoutKinds(reachable, step.heap, cardByObj, layoutOverride)
      val nodes      = rawNodes.map(n => annotateCard(n, cardByObj, kindByCard))
      val refCursors = headLocals(step).collect {
        case (name, HeapValue.Ref(id)) if nodeIds.contains(id) => VizCursor(name, id)
      }
      val cursor = (refCursors ::: indexCursors(step, rootId, nodeIds)).distinct
      // Slice 4 — card-level cursors (separate from per-node `cursor`). An Arr / Dict
      // doesn't emit a parent node — only its cells / entries — so a Ref local like
      // `arr` can't appear in `cursor` (there's nothing to mark). We surface it here
      // instead: every active-frame Ref whose target is a card root (an Arr / Dict /
      // Instance that the renderer groups into a card) becomes a `cardCursor`. The
      // client's ArrowLayer reads this and aims at the matching `data-node-id`
      // element (the specific Instance node a Ref points at) — falling back to
      // `data-card-id` for Arr / Dict whose ids match the whole card rather than a
      // single node. Emitting the heap-object id (not the card-group representative)
      // is what lets the arrow tip land *on* a specific tree node instead of in the
      // card's empty padding.
      val cardCursors = headLocals(step).collect {
        case (name, HeapValue.Ref(id)) if cardByObj.contains(id) =>
          VizCursor(name, id)
      }
      VizGraphStep(
        nodes,
        edges,
        cursor,
        Nil,
        Nil,
        Nil,
        annotation,
        step.line,
        frames = vizFrames,
        cardCursor = cardCursors.distinct,
        structureType = structureType
      )

  /**
   * The set of layoutKind names this adapter recognises (Slice 3, auto-dispatch). When [[adapt]]'s
   * `layoutHint` argument is one of these, it's treated as a per-trace override: every card's `layoutKind` is
   * forced to it instead of being inferred per heap object. An empty or unrecognised hint triggers inference.
   * Includes the new shape-driven names AND the older overlap names so existing `viz=` fences (`binary-tree`,
   * `array`, `linked-list`, `grid`, `graph`) keep their previous override behaviour.
   */
  private val KnownLayoutKinds: Set[String] = Set(
    "tree-binary",
    "list-single",
    "list-double",
    "hashmap",
    "array-1d",
    "array-2d",
    "graph-generic",
    "binary-tree",
    "linked-list",
    "array",
    "grid",
    "graph"
  )

  /**
   * Group reachable heap objects into "cards" — units the renderer dispatches as one shape (Slice 3). Each
   * `Arr` and each `Dict` is its own card (their cells / entries always lay out as a single collection).
   * `Instance`s are merged by union-find over instance-to-instance reference edges, so a binary-tree's nodes
   * or a linked-list's nodes form one card together; an `Instance`-to-`Arr` / `Instance`-to-`Dict` reference
   * does *not* merge (the structures render as adjacent cards). The card id is the lexicographically-smallest
   * heap-object id in the group, for deterministic ordering downstream.
   */
  private def groupCards(
      reachable: List[String],
      heap: Map[String, HeapObject]
  ): Map[String, String] =
    val parent = mutable.Map.empty[String, String]
    reachable.foreach(id => parent(id) = id)

    def find(id: String): String =
      var root = id
      while parent(root) != root do root = parent(root)
      var cur = id
      while parent(cur) != root do
        val next = parent(cur)
        parent(cur) = root
        cur = next
      root

    def union(a: String, b: String): Unit =
      val ra = find(a)
      val rb = find(b)
      if ra != rb then
        // Lexicographically-smaller id becomes the root, so the representative is deterministic.
        if ra < rb then parent(rb) = ra else parent(ra) = rb

    reachable.foreach { id =>
      heap.get(id) match
        case Some(HeapObject.Instance(_, fields)) =>
          fields.foreach {
            case (_, HeapValue.Ref(toId))
                if heap.get(toId).exists(_.isInstanceOf[HeapObject.Instance]) =>
              union(id, toId)
            case _ => ()
          }
        case _ => ()
    }

    reachable.iterator.map(id => id -> find(id)).toMap

  /**
   * Per-card layout kind (Slice 3). For each unique card id, pick a representative heap object and infer its
   * shape; if `layoutOverride` is set (the trace's `vizHint`), every card uses it instead. Arr / Dict cards
   * always have one member, so the representative is that array / dict. Instance cards may have many members;
   * the representative is the lowest-id instance (matches the card id), and its field names drive the
   * inference per the plan §5 table:
   *
   *   - `left` + `right` → `tree-binary`
   *   - `next` + `prev` → `list-double`
   *   - `next` only → `list-single`
   *   - else → `graph-generic`
   */
  private def inferLayoutKinds(
      reachable: List[String],
      heap: Map[String, HeapObject],
      cardByObj: Map[String, String],
      layoutOverride: Option[String]
  ): Map[String, String] =
    val byCard = reachable.groupBy(cardByObj)
    byCard.map { case (cardId, _) =>
      val kind = layoutOverride.getOrElse(inferOneCard(cardId, heap))
      cardId -> kind
    }

  // Layout kind is inferred from the card's ROOT heap object (the array's
  // shape, the instance's field names), not its members — so no member list.
  private def inferOneCard(
      cardId: String,
      heap: Map[String, HeapObject]
  ): String =
    heap.get(cardId) match
      case Some(HeapObject.Arr(_, items)) =>
        val allArrItems = items.nonEmpty && items.forall {
          case HeapValue.Ref(id) => heap.get(id).exists(_.isInstanceOf[HeapObject.Arr])
          case _                 => false
        }
        if allArrItems then "array-2d" else "array-1d"
      case Some(HeapObject.Dict(_)) => "hashmap"
      case Some(HeapObject.Instance(_, fields)) =>
        val fieldNames = fields.iterator.map(_._1).toSet
        val hasLeft    = fieldNames.contains("left")
        val hasRight   = fieldNames.contains("right")
        val hasNext    = fieldNames.contains("next")
        val hasPrev    = fieldNames.contains("prev")
        if hasLeft && hasRight then "tree-binary"
        else if hasNext && hasPrev then "list-double"
        else if hasNext then "list-single"
        else "graph-generic"
      case None => "graph-generic"

  /**
   * Annotate one [[VizNode]] with its `cardId` and `layoutKind`. The node's owning heap object id is the `id`
   * itself for an instance, or everything before the first `#` for a cell / entry (the synthesised suffix
   * added by [[nodesOf]]). Falls back gracefully when the parent isn't in `cardByObj` — leaves the fields
   * empty so the renderer treats the trace as legacy (single-layout via `VizGraph.layoutHint`).
   */
  private def annotateCard(
      node: VizNode,
      cardByObj: Map[String, String],
      kindByCard: Map[String, String]
  ): VizNode =
    val ownerId = node.id.takeWhile(_ != '#')
    cardByObj.get(ownerId) match
      case Some(cardId) =>
        node.copy(cardId = cardId, layoutKind = kindByCard.getOrElse(cardId, ""))
      case None => node

  /**
   * Frame-local *integer* indices, drawn as cursors on an array's cells. An array algorithm advances integer
   * indices (`i`, `lo`, `hi`, `mid`) — but a Python `int` local is a [[HeapValue.Scalar]], not a `Ref`, so
   * the reference-cursor logic in [[buildStep]] never sees it and the array would render as an inert row with
   * no moving pointer. When the root object is an array, a local whose name denotes a position
   * ([[IndexNames]]) and whose value is a valid index becomes a [[VizCursor]] on cell `root#i`. The
   * valid-index gate is what keeps a length / accumulator local (`n`, `total`) — out of range — from drawing
   * a stray caret; [[IndexNames]] is the second gate. Only the root array is indexed: an integer indexing
   * some *other* reachable array can't be told apart from one indexing the root, so this stays deliberately
   * conservative.
   */
  private def indexCursors(step: HeapStep, rootId: String, nodeIds: Set[String]): List[VizCursor] =
    step.heap.get(rootId) match
      case Some(HeapObject.Arr(_, items)) =>
        val len = items.size
        headLocals(step).collect {
          case (name, HeapValue.Scalar(HeapScalar.I(v)))
              if IndexNames.contains(name) && v >= 0 && v < len && nodeIds.contains(s"$rootId#$v") =>
            VizCursor(name, s"$rootId#$v")
        }
      case _ => Nil

  /**
   * The VizNode(s) a heap object contributes. An instance is one node; an array is one `"cell"` node per
   * element, each carrying its index as `slot`; a dict is one `"entry"` node per pair, the key surfaced as a
   * `meta` field. Cell / entry ids are synthesised from the owning object's id so they stay stable while the
   * structure mutates — the per-step diff and the renderer's keyed joins depend on that stability.
   */
  private def nodesOf(id: String, obj: HeapObject, heap: Map[String, HeapObject]): List[VizNode] =
    obj match
      case inst: HeapObject.Instance =>
        val (label, meta) = nodeView(inst)
        List(VizNode(id, label, inst.cls, meta))
      case HeapObject.Arr(_, items) =>
        items.iterator.zipWithIndex.map { (item, i) =>
          VizNode(s"$id#$i", valueLabel(item), "cell", Nil, Some(i))
        }.toList
      case HeapObject.Dict(entries) =>
        entries.map { (k, v) =>
          VizNode(s"$id#${keyId(k)}", valueLabel(v), "entry", List(VizField("key", keyDisplay(k, heap))))
        }

  /**
   * The edges a heap object contributes. A reference to an instance is one edge; a reference to a collection
   * has no single node to land on, so it fans out to one edge per cell / entry.
   */
  private def edgesOf(
      id: String,
      obj: HeapObject,
      heap: Map[String, HeapObject],
      nodeIds: Set[String]
  ): List[VizEdge] =
    def edgesTo(from: String, to: String, label: String): List[VizEdge] =
      nodeIdsOf(to, heap).map(VizEdge(from, _, label)).filter(e => nodeIds(e.from) && nodeIds(e.to))
    obj match
      case HeapObject.Instance(_, fields) =>
        fields.flatMap {
          case (field, HeapValue.Ref(to)) => edgesTo(id, to, field)
          case _                          => Nil
        }
      case HeapObject.Arr(_, items) =>
        items.iterator.zipWithIndex.flatMap {
          case (HeapValue.Ref(to), i) => edgesTo(s"$id#$i", to, "")
          case _                      => Nil
        }.toList
      case HeapObject.Dict(entries) =>
        entries.flatMap { (k, v) =>
          val entryId = s"$id#${keyId(k)}"
          val keyEdges = k match
            case HeapValue.Ref(to) => edgesTo(entryId, to, "key")
            case _                 => Nil
          val valueEdges = v match
            case HeapValue.Ref(to) => edgesTo(entryId, to, keyDisplay(k, heap))
            case _                 => Nil
          keyEdges ++ valueEdges
        }

  /** The node ids a reference to `id` connects to — the instance itself, or every cell / entry. */
  private def nodeIdsOf(id: String, heap: Map[String, HeapObject]): List[String] =
    heap.get(id).toList.flatMap(obj => nodesOf(id, obj, heap).map(_.id))

  /** A field / item / entry-value's display label: an inline scalar, or `·` for a reference. */
  private def valueLabel(v: HeapValue): String = v match
    case HeapValue.Scalar(s) => scalarLabel(s)
    case HeapValue.Ref(_)    => RefLabel

  /**
   * A dict key's identity, for the synthesised entry id. A scalar key is itself; a reference key uses the
   * referenced object's id, so distinct tuple keys (a 2-D DP memo keyed by `(i, j)`) get distinct entries.
   */
  private def keyId(k: HeapValue): String = k match
    case HeapValue.Scalar(s) => scalarLabel(s)
    case HeapValue.Ref(id)   => "@" + id

  /** A dict key's display label: a scalar inline, a tuple `(0, 1)` spelled out, else `·`. */
  private def keyDisplay(k: HeapValue, heap: Map[String, HeapObject]): String = k match
    case HeapValue.Scalar(s) => scalarLabel(s)
    case HeapValue.Ref(id) =>
      heap.get(id) match
        case Some(HeapObject.Arr(_, items)) => items.map(valueLabel).mkString("(", ", ", ")")
        case _                              => RefLabel

  /**
   * A node's display: the primary value label (the first present value-field) plus the object's *other*
   * scalar fields as `meta` sub-labels — so per-node state (an AVL `height`, an RBT `color`, …) shows on the
   * diagram, not just the value.
   *
   * `null`-valued fields are dropped from `meta`: an empty `left` / `right` is a pointer slot, not state, and
   * `field=null` sub-labels under every leaf would clutter rather than teach.
   */
  private def nodeView(inst: HeapObject.Instance): (String, List[VizField]) =
    val byName     = inst.fields.toMap
    val valueField = ValueFields.find(byName.contains)
    val label = valueField.flatMap(byName.get) match
      case Some(HeapValue.Scalar(s)) => scalarLabel(s)
      case Some(HeapValue.Ref(_))    => RefLabel
      case None                      => inst.cls
    val nonValueScalars = inst.fields.collect {
      case (name, HeapValue.Scalar(s)) if !valueField.contains(name) && s != HeapScalar.Null =>
        VizField(name, scalarLabel(s))
    }
    // Multi-scalar classes (e.g. AVL `val` + `height`) include the value field's
    // name in meta too, so the user can map the unlabeled "1" in the circle to
    // its attribute name alongside the other fields. Single-scalar classes
    // (e.g. plain TreeNode with just `val`) skip the duplicate — the value in
    // the circle plus the class label below is unambiguous on its own.
    val meta =
      if nonValueScalars.isEmpty then Nil
      else
        val primary = valueField.flatMap { vf =>
          byName.get(vf).collect { case HeapValue.Scalar(s) => VizField(vf, scalarLabel(s)) }
        }
        primary.toList ++ nonValueScalars
    (label, meta)

  private def scalarLabel(s: HeapScalar): String = s match
    case HeapScalar.I(v) => v.toString
    case HeapScalar.D(v) => v.toString
    case HeapScalar.B(v) => v.toString
    case HeapScalar.S(v) => v
    case HeapScalar.Null => "null"

  /** The type-name badge shown in the StackFramesPanel for a scalar local. */
  private def scalarTypeName(s: HeapScalar): String = s match
    case HeapScalar.I(_) => "int"
    case HeapScalar.D(_) => "float"
    case HeapScalar.B(_) => "bool"
    case HeapScalar.S(_) => "str"
    case HeapScalar.Null => "None"

  /**
   * A local variable's display-ready `(typeName, value)` pair for the StackFramesPanel.
   *
   * Scalars use [[scalarTypeName]] / [[scalarLabel]]; references resolve the referenced object from the heap
   * — an instance shows its primary-value field (same as the diagram node label); a list / array is
   * abbreviated to at most three elements; a dict shows its entry count.
   */
  private def localDisplay(v: HeapValue, heap: Map[String, HeapObject]): (String, String) =
    v match
      case HeapValue.Scalar(s) => (scalarTypeName(s), scalarLabel(s))
      case HeapValue.Ref(id) =>
        heap.get(id) match
          case Some(inst @ HeapObject.Instance(cls, _)) =>
            val (label, _) = nodeView(inst)
            (cls, label)
          case Some(HeapObject.Arr(_, items)) =>
            val preview  = items.take(3).map(valueLabel).mkString(", ")
            val ellipsis = if items.size > 3 then ", …" else ""
            ("list", s"[$preview$ellipsis]")
          case Some(HeapObject.Dict(entries)) =>
            val n = entries.size
            ("dict", if n == 1 then "{1 entry}" else s"{$n entries}")
          case None => ("?", "?")

  /**
   * Builds the [[VizFrame]] list for a single [[HeapStep]]. Helper frames ([[isHelperFrame]]) and the Python
   * `self` variable are excluded; `isActive` is true only for the innermost frame.
   */
  private def buildFrames(step: HeapStep): List[VizFrame] =
    step.frames
      .filterNot(f => isHelperFrame(f.fn))
      .zipWithIndex
      .map { case (frame, i) =>
        val locals = frame.locals
          .filterNot { case (name, _) => name == "self" }
          .map { case (name, v) =>
            val (typeName, valueStr) = localDisplay(v, step.heap)
            VizLocal(name, typeName, valueStr, changed = false)
          }
        VizFrame(frame.fn, locals, isActive = i == 0)
      }

  /** Every object id referenced directly by an object's fields / items / entries. */
  private def outRefs(obj: HeapObject): List[String] = obj match
    case HeapObject.Instance(_, fields) => fields.collect { case (_, HeapValue.Ref(id)) => id }
    case HeapObject.Arr(_, items)       => items.collect { case HeapValue.Ref(id) => id }
    case HeapObject.Dict(entries) =>
      entries.flatMap { case (k, v) => List(k, v) }.collect { case HeapValue.Ref(id) => id }

  /** Ids reachable from `start` over references, in deterministic depth-first order. */
  private def reachableFrom(start: String, heap: Map[String, HeapObject]): List[String] =
    val seen = mutable.LinkedHashSet.empty[String]
    def go(id: String): Unit =
      if heap.contains(id) && seen.add(id) then outRefs(heap(id)).foreach(go)
    go(start)
    seen.toList

  /**
   * Replace every empty-graph step with the most recent non-empty structure, so the diagram never blanks out
   * while the trace steps through a frame whose locals don't reach the root — a `TreeNode.__init__`, a helper
   * call. The carried step keeps its own source line and annotation (the code pane still advances) but shows
   * the last structure as context, with no cursor: no local in that frame points into the structure. The
   * empty steps off both ends are already removed by [[dropEmptyEnds]]; this fills the interior gaps.
   */
  private def carryForward(steps: List[VizGraphStep]): List[VizGraphStep] =
    val out  = mutable.ListBuffer.empty[VizGraphStep]
    var last = Option.empty[VizGraphStep]
    steps.foreach { s =>
      if s.nodes.nonEmpty then
        out += s
        last = Some(s)
      else
        out += last.fold(s)(prev => s.copy(nodes = prev.nodes, edges = prev.edges))
    }
    out.toList

  /**
   * Drop only *exact* consecutive duplicates — a step whose source line AND visible state (nodes, edges,
   * cursor) all match the previous kept step. A step on a new source line is always kept, even when the
   * diagram looks identical: stepping the core algorithm should follow the code line by line, with the code
   * pane and caption advancing on every line, not skip lines that happen not to redraw the structure. The
   * builder / constructor frames that would otherwise flood the trace are already gone (see
   * [[isHelperFrame]]), so what survives here is the algorithm itself — every line of it worth a step. The
   * line-equal case still fires for a `call` / `return` event that doubles a line already emitted.
   */
  private def coalesce(steps: List[VizGraphStep]): List[VizGraphStep] = steps match
    case Nil => Nil
    case head :: tail =>
      val out = mutable.ListBuffer(head)
      tail.foreach { s =>
        val prev = out.last
        val differs =
          s.line != prev.line || s.nodes != prev.nodes || s.edges != prev.edges || s.cursor != prev.cursor
        if differs then out += s
      }
      out.toList

  /**
   * Derive each step's per-node diff against the previous step:
   *
   *   - `highlight` — ids present now but not before (a freshly created / attached node).
   *   - `changed` — ids present in both steps whose display label differs.
   *   - `removed` — ids present before but not now; the vanished node is re-emitted into *this* step's
   *     `nodes` (carrying its last-known label) so the renderer can draw it one final fading frame.
   *
   * Diffs read the original (pre-re-emit) node sets, so a re-emitted `removed` node never re-triggers as
   * removed in the following step. Finally each step's `annotation` is replaced with a generated narration of
   * that diff ([[narrate]]).
   */
  private def diffSteps(steps: List[VizGraphStep]): List[VizGraphStep] =
    val v = steps.toVector
    v.indices.map { i =>
      if i == 0 then v(i).copy(annotation = v(i).annotation.copy(title = narrate(None, v(i))))
      else
        val prev      = v(i - 1).nodes.iterator.map(n => n.id -> n.label).toMap
        val cur       = v(i).nodes.iterator.map(n => n.id -> n.label).toMap
        val highlight = v(i).nodes.iterator.map(_.id).filterNot(prev.contains).toList
        val changed = v(i).nodes.iterator
          .map(_.id)
          .filter(id => prev.get(id).exists(_ != cur(id)))
          .toList
        val removedNodes = v(i - 1).nodes.filterNot(n => cur.contains(n.id))
        // Mark which frame locals changed compared with the previous step.
        // Key: (fn, localName) → previous value; a local that newly appeared counts as changed.
        val prevLocals: Map[(String, String), String] =
          v(i - 1).frames.flatMap(f => f.locals.map(l => (f.fn, l.name) -> l.value)).toMap
        val updatedFrames = v(i).frames.map { frame =>
          val updatedLocals = frame.locals.map { local =>
            local.copy(changed = prevLocals.get((frame.fn, local.name)).exists(_ != local.value))
          }
          frame.copy(locals = updatedLocals)
        }
        // Slice 6 — `unchanged` = only the source line advanced; heap + active-frame locals identical.
        // Checked before re-emitting removed nodes: a step that fades a node out is structurally
        // significant and must not be filtered. Cursor comparison uses (name, target) pairs so colour
        // changes (assigned later by colorizeCursors) don't flip the flag.
        val prevCursorPairs = v(i - 1).cursor.iterator.map(c => (c.name, c.target)).toSet
        val curCursorPairs  = v(i).cursor.iterator.map(c => (c.name, c.target)).toSet
        val prevHeadLocals =
          v(i - 1).frames.headOption.map(_.locals.map(l => (l.name, l.typeName, l.value))).getOrElse(Nil)
        val curHeadLocals =
          v(i).frames.headOption.map(_.locals.map(l => (l.name, l.typeName, l.value))).getOrElse(Nil)
        val stepUnchanged =
          highlight.isEmpty && changed.isEmpty && removedNodes.isEmpty &&
            prevCursorPairs == curCursorPairs && prevHeadLocals == curHeadLocals
        val diffed = v(i).copy(
          nodes = v(i).nodes ++ removedNodes,
          highlight = highlight,
          changed = changed,
          removed = removedNodes.map(_.id),
          frames = updatedFrames,
          unchanged = stepUnchanged
        )
        diffed.copy(annotation = diffed.annotation.copy(title = narrate(Some(v(i - 1)), diffed)))
    }.toList

  /**
   * A short, human phrase describing what a step did — the diagram's caption. Reads the already-computed diff
   * (removed, then a freshly inserted node, then a value change) and finally cursor movement; a dict entry is
   * named by its key, so its insert reads `key = value`. The raw source line it replaces still shows,
   * highlighted, in the modal's code pane.
   */
  private def narrate(prev: Option[VizGraphStep], step: VizGraphStep): String =
    val nodeById                = step.nodes.iterator.map(n => n.id -> n).toMap
    def lbl(id: String): String = nodeById.get(id).map(_.label).getOrElse("?")
    def keyOf(id: String): Option[String] =
      nodeById.get(id).flatMap(_.meta.collectFirst { case VizField("key", k) => k })
    def named(id: String): String = keyOf(id).getOrElse(lbl(id))
    if step.removed.nonEmpty then "removed " + step.removed.map(named).mkString(", ")
    else if step.highlight.nonEmpty then
      val id = step.highlight.find(i => lbl(i) != RefLabel).getOrElse(step.highlight.head)
      keyOf(id) match
        case Some(k) => s"$k = ${lbl(id)}"
        case None =>
          step.edges.find(e => e.to == id && e.label.nonEmpty) match
            case Some(e) => s"inserted ${lbl(id)} as ${lbl(e.from)}.${e.label}"
            case None    => s"added ${lbl(id)}"
    else if step.changed.nonEmpty then
      val id  = step.changed.head
      val was = prev.flatMap(_.nodes.find(_.id == id)).map(_.label)
      (keyOf(id), was) match
        case (Some(k), Some(w)) => s"$k changed from $w to ${lbl(id)}"
        case (None, Some(w))    => s"$w changed to ${lbl(id)}"
        case (_, None)          => s"set ${lbl(id)}"
    else
      val prevTargets =
        prev.map(_.cursor.iterator.map(c => c.name -> c.target).toMap).getOrElse(Map.empty)
      val moved = step.cursor.filter(c => !prevTargets.get(c.name).contains(c.target))
      if moved.nonEmpty then moved.map(c => s"${c.name} moves to ${lbl(c.target)}").mkString(", ")
      else if prev.isEmpty then "initial structure"
      else step.annotation.body

  /**
   * Give every cursor its role colour ([[MarkerColors]]) — resolved once across the whole trace so a pointer
   * keeps one colour for the entire animation; unaliased names draw distinct fallback hues by first
   * appearance.
   */
  private def colorizeCursors(steps: List[VizGraphStep]): List[VizGraphStep] =
    val names =
      steps.iterator
        .flatMap(s => s.cursor.iterator.map(_.name) ++ s.cardCursor.iterator.map(_.name))
        .toList
    val colors = MarkerColors.assignColors(names)
    steps.map { s =>
      s.copy(
        cursor = s.cursor.map(c => c.copy(color = colors.getOrElse(c.name, ""))),
        cardCursor = s.cardCursor.map(c => c.copy(color = colors.getOrElse(c.name, "")))
      )
    }
