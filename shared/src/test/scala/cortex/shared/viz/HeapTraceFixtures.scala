package cortex.shared.viz

/**
 * Phase 0 — three "hardest shape" [[HeapTrace]] fixtures (ADR-0025).
 *
 * These fixtures are the empirical test of whether the existing `Instance / Arr / Dict + Ref / Scalar`
 * primitive set can express the 16 planned data-structure shapes from the bespoke-renderer programme
 * (ADR-0024). Each fixture is the *hardest case* in its family — picked because if these adapt + render
 * cleanly, the rest of the family is mechanical:
 *
 *   - [[avlRotationTrace]] — recursive `Instance(left, right, height, balance)` nodes around a right
 *     rotation. Exercises: tree restructuring (root rebinds), per-node scalar metadata (`height`, `balance`
 *     shown as sub-labels), edge label diff (`left`/`right` swap), the highlight/changed diff between
 *     consecutive steps.
 *   - [[graphBfsTrace]] — `Instance(id, neighbors: List[Ref])` nodes with cycles + revisits. Exercises: graph
 *     layout (non-tree), an Arr-of-Refs adjacency list, a `visited` Dict in the active frame's locals (a heap
 *     object whose role is "set"), step coalescing across queue-dequeue cycles, and edges that loop back.
 *   - [[hashmapChainedCollisionsTrace]] — `Dict(I → Ref→Arr[Ref→Node])` where each Arr is the bucket chain
 *     and each `Node(key, value, next)` Instance chains to the next bucket member via a `next` ref.
 *     Exercises: Dict with scalar keys + Ref values, Arr of Refs (the bucket), Instances with mixed scalar +
 *     Ref fields, AND a redundant chain representation (Arr position + next-ref) — the latter is what real
 *     Python `dict` + Java `HashMap` traces hit.
 *
 * Each `Fixture` carries the trace plus the metadata `HeapToGraph.adapt` needs (`source`, `layoutHint`,
 * `rootHint`, `title`, optional `vizKind`). Consumers (`HeapTraceFixturesSpec`, the sbt `genTraceFixtures`
 * task that writes the JSON for Playwright) drive the adapter the same way the Visualise modal does — so the
 * resulting VizCases is the bit-exact shape Phases 1+ will refactor against.
 */
object HeapTraceFixtures:

  /** A single Phase-0 fixture — trace + the metadata the adapter consumes. */
  final case class Fixture(
      name: String,
      title: String,
      source: String,
      rootHint: Option[String],
      layoutHint: String,
      vizKind: Option[String],
      trace: HeapTrace
  )

  // ─── helpers (mirrors HeapToGraphSpec for consistency) ──────────────────────────

  private def int(n: Long): HeapValue     = HeapValue.Scalar(HeapScalar.I(n))
  private def str(s: String): HeapValue   = HeapValue.Scalar(HeapScalar.S(s))
  private def bool(b: Boolean): HeapValue = HeapValue.Scalar(HeapScalar.B(b))
  private val nul: HeapValue              = HeapValue.Scalar(HeapScalar.Null)
  private def ref(id: String): HeapValue  = HeapValue.Ref(id)

  private def lst(items: HeapValue*): HeapObject =
    HeapObject.Arr(ArrKind.Lst, items.toList)

  private def dict(entries: (HeapValue, HeapValue)*): HeapObject =
    HeapObject.Dict(entries.toList)

  private def instance(cls: String, fields: (String, HeapValue)*): HeapObject =
    HeapObject.Instance(cls, fields.toList)

  private def step(
      line: Int,
      fn: String,
      locals: List[(String, HeapValue)],
      heap: (String, HeapObject)*
  ): HeapStep =
    HeapStep(line, "line", List(HeapFrame(fn, locals)), heap.toMap)

  // ─── Fixture 1: AVL right rotation ──────────────────────────────────────────────
  //
  // Before:        After:
  //     z              y
  //    /              / \
  //   y              x   z
  //  /
  // x
  //
  // x.val=1, y.val=2, z.val=3; heights pre-rotation z=3, y=2, x=1, balance z=+2.

  private object Avl:

    // Pre-rotation tree — z is root.
    private def nodeX(height: Int = 1, balance: Int = 0): HeapObject =
      instance(
        "AvlNode",
        "val"     -> int(1),
        "left"    -> nul,
        "right"   -> nul,
        "height"  -> int(height.toLong),
        "balance" -> int(balance.toLong)
      )

    private def nodeY(left: HeapValue, right: HeapValue, height: Int, balance: Int): HeapObject =
      instance(
        "AvlNode",
        "val"     -> int(2),
        "left"    -> left,
        "right"   -> right,
        "height"  -> int(height.toLong),
        "balance" -> int(balance.toLong)
      )

    private def nodeZ(left: HeapValue, right: HeapValue, height: Int, balance: Int): HeapObject =
      instance(
        "AvlNode",
        "val"     -> int(3),
        "left"    -> left,
        "right"   -> right,
        "height"  -> int(height.toLong),
        "balance" -> int(balance.toLong)
      )

    // Step 1 — initial tree, root = z, balance = +2 detected.
    private val s1 = step(
      line = 7,
      fn = "rotateRight",
      locals = List("root" -> ref("Z"), "node" -> ref("Z")),
      "Z" -> nodeZ(ref("Y"), nul, height = 3, balance = 2),
      "Y" -> nodeY(ref("X"), nul, height = 2, balance = 1),
      "X" -> nodeX(height = 1, balance = 0)
    )

    // Step 2 — pick the left child (y) as the new root candidate. `child` cursor introduced.
    private val s2 = step(
      line = 9,
      fn = "rotateRight",
      locals = List("root" -> ref("Z"), "node" -> ref("Z"), "child" -> ref("Y")),
      "Z" -> nodeZ(ref("Y"), nul, height = 3, balance = 2),
      "Y" -> nodeY(ref("X"), nul, height = 2, balance = 1),
      "X" -> nodeX(height = 1, balance = 0)
    )

    // Step 3 — rotation done. y is new root. z is y's right child with updated height/balance.
    private val s3 = step(
      line = 13,
      fn = "rotateRight",
      locals = List("root" -> ref("Y"), "node" -> ref("Y"), "child" -> ref("Y")),
      "Y" -> nodeY(ref("X"), ref("Z"), height = 2, balance = 0),
      "X" -> nodeX(height = 1, balance = 0),
      "Z" -> nodeZ(nul, nul, height = 1, balance = 0)
    )

    val trace: HeapTrace = HeapTrace(List(s1, s2, s3), truncated = false)

    val source: String =
      """def rotate_right(root):
        |    # AVL right rotation around `root`.
        |    # Detects left-heavy imbalance and rebases the subtree so `root.left`
        |    # becomes the new subtree root, returning it to the caller.
        |    node = root
        |    if node.balance >= 2:
        |        # Left-heavy — promote node.left as new root.
        |        child = node.left
        |        node.left = child.right
        |        child.right = node
        |        node.height = 1 + max(h(node.left), h(node.right))
        |        child.height = 1 + max(h(child.left), h(child.right))
        |        return child
        |    return node
        |""".stripMargin

  /** Right rotation in a 3-node AVL — root rebinds, heights/balance update, no rebalanced subtree gap. */
  val avlRotationTrace: Fixture = Fixture(
    name = "avl-rotation",
    title = "AVL right rotation",
    source = Avl.source,
    rootHint = Some("root"),
    layoutHint = "tree-binary",
    vizKind = None,
    trace = Avl.trace
  )

  // ─── Fixture 2: Graph BFS with revisits ─────────────────────────────────────────
  //
  // Tiny directed graph with a cycle:
  //     A -> B -> D -> A
  //     A -> C -> D
  // BFS from A. Revisit attempts on A (from D) and D (from C) are skipped via
  // a `visited` Dict in the locals.

  private object Bfs:

    private def gNode(id: String): HeapObject =
      instance("GraphNode", "id" -> str(id), "neighbors" -> ref(s"neigh_$id"))

    // Adjacency lists as Arr heap objects.
    private val adjA = lst(ref("B"), ref("C"))
    private val adjB = lst(ref("D"))
    private val adjC = lst(ref("D"))
    private val adjD = lst(ref("A"))

    private def nodeA: HeapObject = gNode("A")
    private def nodeB: HeapObject = gNode("B")
    private def nodeC: HeapObject = gNode("C")
    private def nodeD: HeapObject = gNode("D")

    private val heapAllNodes: Map[String, HeapObject] = Map(
      "A"       -> nodeA,
      "B"       -> nodeB,
      "C"       -> nodeC,
      "D"       -> nodeD,
      "neigh_A" -> adjA,
      "neigh_B" -> adjB,
      "neigh_C" -> adjC,
      "neigh_D" -> adjD
    )

    // The `visited` set is a Dict from node-id (string) → I(1). We rebuild it
    // per step rather than mutating in place — the diff naturally surfaces
    // each addition.
    private def visitedDict(ids: String*): HeapObject =
      dict(ids.map(id => str(id) -> int(1)).toList*)

    // The `queue` is an Arr of Refs in FIFO order — head at index 0.
    private def queueArr(ids: String*): HeapObject =
      lst(ids.map(ref).toList*)

    private def heapWithRoots(visitedIds: Seq[String], queueIds: Seq[String]): Map[String, HeapObject] =
      heapAllNodes ++ Map(
        "visited" -> visitedDict(visitedIds*),
        "queue"   -> queueArr(queueIds*)
      )

    private def mk(
        line: Int,
        node: Option[String],
        visitedIds: Seq[String],
        queueIds: Seq[String]
    ): HeapStep =
      val locals =
        List(
          "root"    -> ref("A"),
          "visited" -> ref("visited"),
          "queue"   -> ref("queue")
        ) ++ node.toList.map(n => "node" -> ref(n))
      HeapStep(line, "line", List(HeapFrame("bfs", locals)), heapWithRoots(visitedIds, queueIds))

    // The trace walks one full BFS from A. Each step is one logical bfs() loop
    // iteration — dequeue, mark visited, enqueue unvisited neighbours.
    val trace: HeapTrace = HeapTrace(
      List(
        // s1: initial — queue=[A], visited={}
        mk(2, None, Nil, Seq("A")),
        // s2: dequeue A — node=A, visited={A}, queue=[]
        mk(4, Some("A"), Seq("A"), Nil),
        // s3: enqueue A's unvisited neighbours B, C — queue=[B,C]
        mk(7, Some("A"), Seq("A"), Seq("B", "C")),
        // s4: dequeue B — node=B, visited={A,B}, queue=[C]
        mk(4, Some("B"), Seq("A", "B"), Seq("C")),
        // s5: enqueue B's unvisited neighbour D — queue=[C,D]
        mk(7, Some("B"), Seq("A", "B"), Seq("C", "D")),
        // s6: dequeue C — node=C, visited={A,B,C}, queue=[D]
        mk(4, Some("C"), Seq("A", "B", "C"), Seq("D")),
        // s7: C's neighbour D already in queue/visited — no enqueue (revisit skip)
        mk(7, Some("C"), Seq("A", "B", "C"), Seq("D")),
        // s8: dequeue D — node=D, visited={A,B,C,D}, queue=[]
        mk(4, Some("D"), Seq("A", "B", "C", "D"), Nil),
        // s9: D's neighbour A already visited — no enqueue (revisit skip)
        mk(7, Some("D"), Seq("A", "B", "C", "D"), Nil)
      ),
      truncated = false
    )

    val source: String =
      """def bfs(root):
        |    visited = {}
        |    queue = [root]
        |    while queue:
        |        node = queue.pop(0)
        |        visited[node.id] = 1
        |        for n in node.neighbors:
        |            if n.id not in visited:
        |                queue.append(n)
        |""".stripMargin

  /**
   * BFS over a 4-node directed graph with a cycle; `visited` Dict in locals; queue carry-forward.
   *
   * `layoutHint = ""` (no override) — lets the adapter's per-card inference run. The GraphNode card gets
   * `graph-generic`, each adjacency Arr gets `array-1d`. With an explicit hint like `"graph"` every card
   * would be uniformly tagged with the override (production behaviour for `viz=graph` fences). We
   * deliberately exercise the per-card path here so the fixture stresses inference too.
   */
  val graphBfsTrace: Fixture = Fixture(
    name = "graph-bfs",
    title = "BFS over a directed graph with revisits",
    source = Bfs.source,
    rootHint = Some("root"),
    layoutHint = "",
    vizKind = None,
    trace = Bfs.trace
  )

  // ─── Fixture 3: HashMap with chained collisions ─────────────────────────────────
  //
  // Real Python dict / Java HashMap with separate-chaining collision resolution.
  // The top-level Dict maps bucket-index (integer) → Ref(bucket-Arr). Each bucket
  // Arr lists Refs to Node instances IN CHAIN ORDER. Each Node also carries a
  // `next` ref to the next bucket member (or null). The Arr ordering and the
  // `next` ref redundantly express the chain — real traces have both because
  // the dict object has an array-of-buckets backing and each bucket entry has
  // a `next` pointer.

  private object Hash:

    private def node(key: String, value: Long, next: HeapValue): HeapObject =
      instance("Entry", "key" -> str(key), "value" -> int(value), "next" -> next)

    // The Dict's keys are bucket indices (Int scalars). Its values are Refs to
    // the bucket Arrs.
    private def bucketsDict(entries: (Long, HeapValue)*): HeapObject =
      dict(entries.map((k, v) => int(k) -> v).toList*)

    private def bucketArr(ids: String*): HeapObject =
      lst(ids.map(ref).toList*)

    // Step 1 — empty map. locals: map=Ref(map).
    private val s1 = step(
      line = 3,
      fn = "put",
      locals = List("map" -> ref("map"), "key" -> str("apple"), "value" -> int(1)),
      "map" -> bucketsDict()
    )

    // Step 2 — insert "apple" → bucket 0 created, single-element chain.
    private val s2 = step(
      line = 7,
      fn = "put",
      locals = List("map" -> ref("map"), "key" -> str("apple"), "value" -> int(1)),
      "map"      -> bucketsDict(0L -> ref("bucket_0")),
      "bucket_0" -> bucketArr("n_apple"),
      "n_apple"  -> node("apple", 1, nul)
    )

    // Step 3 — insert "fig" → bucket 1 created.
    private val s3 = step(
      line = 3,
      fn = "put",
      locals = List("map" -> ref("map"), "key" -> str("fig"), "value" -> int(3)),
      "map"      -> bucketsDict(0L -> ref("bucket_0"), 1L -> ref("bucket_1")),
      "bucket_0" -> bucketArr("n_apple"),
      "bucket_1" -> bucketArr("n_fig"),
      "n_apple"  -> node("apple", 1, nul),
      "n_fig"    -> node("fig", 3, nul)
    )

    // Step 4 — insert "grape" → collision with bucket 0, chain extended.
    private val s4 = step(
      line = 9,
      fn = "put",
      locals = List("map" -> ref("map"), "key" -> str("grape"), "value" -> int(5)),
      "map"      -> bucketsDict(0L -> ref("bucket_0"), 1L -> ref("bucket_1")),
      "bucket_0" -> bucketArr("n_apple", "n_grape"),
      "bucket_1" -> bucketArr("n_fig"),
      "n_apple"  -> node("apple", 1, ref("n_grape")),
      "n_grape"  -> node("grape", 5, nul),
      "n_fig"    -> node("fig", 3, nul)
    )

    val trace: HeapTrace = HeapTrace(List(s1, s2, s3, s4), truncated = false)

    val source: String =
      """def put(map, key, value):
        |    idx = hash(key) % 4
        |    if idx not in map:
        |        map[idx] = []
        |    bucket = map[idx]
        |    for entry in bucket:
        |        if entry.key == key:
        |            entry.value = value
        |            return
        |    bucket.append(Entry(key, value, None))
        |""".stripMargin

  /**
   * Three-insert hashmap trace with one bucket collision; both Arr-order and next-ref encode the chain.
   *
   * `layoutHint = ""` (no override) — same reasoning as [[graphBfsTrace]]: lets per-card inference surface
   * the Dict as `hashmap`, the bucket Arrs as `array-1d`, and the Entry instance chain (linked via the `next`
   * field) as `list-single`. Production usage would set `viz=hashmap` and every card would be tagged
   * `hashmap`; the Phase-0 fixture instead exercises inference end-to-end.
   */
  val hashmapChainedCollisionsTrace: Fixture = Fixture(
    name = "hashmap-chained-collisions",
    title = "HashMap with separate-chaining collisions",
    source = Hash.source,
    rootHint = Some("map"),
    layoutHint = "",
    vizKind = None,
    trace = Hash.trace
  )

  // ─── Fixture 4: Stack push sequence (bespoke renderer #1 — ADR-0029) ────────────
  //
  // An Arr pushed onto, with a `top` index local. `vizKind = "stack"` forces
  // structureType=stack so the bespoke StackRenderer (now on the SDK) fires.
  // Exists so the SDK port is verified in a real browser (Playwright), not just
  // by the source-AST unit tests.

  private object Stack:

    private def mk(line: Int, items: List[Long], top: Long): HeapStep =
      step(
        line,
        "push",
        List("stack" -> ref("S"), "top" -> int(top)),
        "S" -> HeapObject.Arr(ArrKind.Lst, items.map(int))
      )

    val trace: HeapTrace = HeapTrace(
      List(
        mk(3, List(10), 0),
        mk(3, List(10, 20), 1),
        mk(3, List(10, 20, 30), 2)
      ),
      truncated = false
    )

    val source: String =
      """def push_all(stack, values):
        |    top = -1
        |    for v in values:
        |        stack.append(v)
        |        top += 1
        |""".stripMargin

  /** Three pushes onto a stack; `vizKind="stack"` → bespoke StackRenderer (SDK renderer #1). */
  val stackPushTrace: Fixture = Fixture(
    name = "stack-push",
    title = "Stack — push sequence",
    source = Stack.source,
    rootHint = Some("stack"),
    layoutHint = "",
    vizKind = Some("stack"),
    trace = Stack.trace
  )

  /**
   * Same trace as [[hashmapChainedCollisionsTrace]] but `vizKind="hashmap"` → the bespoke WHOLE-GRAPH
   * HashMapRenderer (ADR-0029, renderer #2) fires instead of the generic multi-card path. This is the fixture
   * that proves cross-structure rendering: the Dict→Arr→Entry bucket chains render as one coherent view (the
   * generic path shattered them into edge-less cards — ADR-0025).
   */
  val hashmapKindTrace: Fixture = Fixture(
    name = "hashmap-kind",
    title = "HashMap — separate chaining",
    source = Hash.source,
    rootHint = Some("map"),
    layoutHint = "",
    vizKind = Some("hashmap"),
    trace = Hash.trace
  )

  /**
   * Same trace as [[graphBfsTrace]] but `vizKind="graph"` → the bespoke whole-graph GraphRenderer (ADR-0027,
   * renderer #3) fires. It synthesises direct node→node edges from the adjacency-list cells and relabels
   * nodes by `id`, so the graph renders as connected A→B→D→A circles instead of the generic path's isolated
   * edge-less cards (ADR-0025).
   */
  val graphKindTrace: Fixture = Fixture(
    name = "graph-kind",
    title = "Graph — BFS traversal",
    source = Bfs.source,
    rootHint = Some("root"),
    layoutHint = "",
    vizKind = Some("graph"),
    trace = Bfs.trace
  )

  // ─── Fixture 7: Queue enqueue/dequeue (bespoke renderer #4 — ADR-0027) ──────────
  //
  // An Arr used FIFO: enqueue appends at the back, dequeue removes from the front
  // (pop(0) — the Arr shrinks from the head). `vizKind="queue"` → the bespoke
  // QueueRenderer (head on the left, tail on the right).

  private object Queue:

    private def mk(line: Int, items: List[Long]): HeapStep =
      step(
        line,
        "enqueue_all",
        List("queue" -> ref("Q")),
        "Q" -> HeapObject.Arr(ArrKind.Lst, items.map(int))
      )

    // Enqueues only — appends grow the tail with a clean per-step diff (only the
    // new tail cell highlights). A dequeue via `pop(0)` would shift every array
    // index, producing a noisy all-cells-changed diff; a clean FIFO-dequeue viz
    // needs front-pointer tracking in the renderer (a future refinement).
    val trace: HeapTrace = HeapTrace(
      List(
        mk(3, List(10)),
        mk(3, List(10, 20)),
        mk(3, List(10, 20, 30))
      ),
      truncated = false
    )

    val source: String =
      """def enqueue_all(queue, values):
        |    for v in values:
        |        queue.append(v)  # enqueue at the tail
        |""".stripMargin

  /** Enqueue 10/20/30; `vizKind="queue"` → bespoke QueueRenderer (head left, tail right). */
  val queueTrace: Fixture = Fixture(
    name = "queue",
    title = "Queue — FIFO",
    source = Queue.source,
    rootHint = Some("queue"),
    layoutHint = "",
    vizKind = Some("queue"),
    trace = Queue.trace
  )

  // ─── Fixture 8: Binary min-heap (bespoke renderer #5 — ADR-0027) ─────────────────
  //
  // An array-backed min-heap built by successive inserts. `vizKind="heap"` → the
  // bespoke HeapRenderer synthesises the implicit binary tree from array indices
  // (parent i → children 2i+1 / 2i+2) and renders it as a tree, not a flat row.
  // Values keep the min-heap property: 10 < {20,30}; 20 < 40.

  private object Heap:

    private def mk(line: Int, items: List[Long]): HeapStep =
      step(
        line,
        "insert",
        List("heap" -> ref("H")),
        "H" -> HeapObject.Arr(ArrKind.Lst, items.map(int))
      )

    val trace: HeapTrace = HeapTrace(
      List(
        mk(3, List(10)),
        mk(3, List(10, 20)),
        mk(3, List(10, 20, 30)),
        mk(3, List(10, 20, 30, 40))
      ),
      truncated = false
    )

    val source: String =
      """def build_min_heap(heap, values):
        |    for v in values:
        |        heap.append(v)   # then sift up to restore the heap property
        |""".stripMargin

  /** Array-backed min-heap; `vizKind="heap"` → bespoke HeapRenderer (array → implicit tree). */
  val heapTrace: Fixture = Fixture(
    name = "heap",
    title = "Binary min-heap",
    source = Heap.source,
    rootHint = Some("heap"),
    layoutHint = "",
    vizKind = Some("heap"),
    trace = Heap.trace
  )

  // ─── Fixture 9: Union-Find / DSU with path compression (bespoke renderer #6 — ADR-0027) ──
  //
  // A disjoint-set forest is stored as a `parent` array: `parent[i] == i` marks a
  // root, otherwise i points at `parent[i]`. `vizKind="union-find"` → the bespoke
  // UnionFindRenderer reads each cell as a parent pointer, relabels the circle to
  // its own index, and draws parent arcs (a self-loop on each root). The trace
  // unions {0,1} then {2,3}, merges those two sets under 0 (leaving a 3 → 2 → 0
  // chain), then path-compresses 3 straight to the root — the cell whose value
  // changes is what `diffSteps` flags, so the shortened arc reads as the win.

  private object UnionFind:

    private def mk(line: Int, parent: List[Long]): HeapStep =
      step(
        line,
        "union",
        List("parent" -> ref("P")),
        "P" -> HeapObject.Arr(ArrKind.Lst, parent.map(int))
      )

    val trace: HeapTrace = HeapTrace(
      List(
        mk(1, List(0, 1, 2, 3, 4)), // all singletons — every element is its own root
        mk(4, List(0, 0, 2, 3, 4)), // union(0, 1): 1 attaches under 0
        mk(4, List(0, 0, 2, 2, 4)), // union(2, 3): 3 attaches under 2
        mk(4, List(0, 0, 0, 2, 4)), // union(0, 2): 2 attaches under 0 → 3 → 2 → 0 chain
        mk(10, List(0, 0, 0, 0, 4)) // find(3) path-compresses: 3 now points straight at 0
      ),
      truncated = false
    )

    val source: String =
      """def union(parent, a, b):
        |    ra, rb = find(parent, a), find(parent, b)
        |    if ra != rb:
        |        parent[rb] = ra    # attach one root under the other
        |
        |def find(parent, x):
        |    root = x
        |    while parent[root] != root:
        |        root = parent[root]
        |    parent[x] = root       # path compression
        |    return root
        |""".stripMargin

  /** Parent-array DSU with union + path compression; `vizKind="union-find"` → bespoke UnionFindRenderer. */
  val unionFindTrace: Fixture = Fixture(
    name = "union-find",
    title = "Union-Find — union + path compression",
    source = UnionFind.source,
    rootHint = Some("parent"),
    layoutHint = "",
    vizKind = Some("union-find"),
    trace = UnionFind.trace
  )

  // ─── Fixture 10: Trie (prefix tree) — bespoke renderer #7 (ADR-0027) ─────────────
  //
  // A trie node is an `Instance(children: Ref→Dict[char → Ref→TrieNode], is_end)`.
  // Nodes connect THROUGH a children Dict, so the per-card path shatters it
  // (TrieNode cards + Dict-entry cards) just like hashmap/graph did — hence a
  // WHOLE-GRAPH renderer. The TrieRenderer composes `parent --children--> entry
  // --char--> child` into a single char-labelled parent→child edge, drops the
  // Dict scaffolding, and marks `is_end` nodes terminal. The trace inserts "cat",
  // then "car" (shares the "ca" prefix, branches at 'a'), then "do" (a second
  // top-level branch) — the canonical prefix-sharing + branching shape.

  private object Trie:

    // A TrieNode Instance + its children Dict (char → Ref(child)). `is_end` surfaces
    // as a meta sub-label; the renderer reads it to mark word-end nodes terminal.
    private def tnode(childrenRef: String, isEnd: Boolean): HeapObject =
      instance("TrieNode", "children" -> ref(childrenRef), "is_end" -> bool(isEnd))

    private def kids(entries: (String, String)*): HeapObject =
      dict(entries.map((ch, target) => str(ch) -> ref(target)).toList*)

    private def mk(word: String, heap: (String, HeapObject)*): HeapStep =
      HeapStep(
        7,
        "line",
        List(HeapFrame("insert", List("root" -> ref("root"), "word" -> str(word)))),
        heap.toMap
      )

    // s1 — insert "cat": root → c → a → t(end).
    private val s1 = mk(
      "cat",
      "root"    -> tnode("ch_root", isEnd = false),
      "ch_root" -> kids("c" -> "c"),
      "c"       -> tnode("ch_c", isEnd = false),
      "ch_c"    -> kids("a" -> "a"),
      "a"       -> tnode("ch_a", isEnd = false),
      "ch_a"    -> kids("t" -> "t"),
      "t"       -> tnode("ch_t", isEnd = true),
      "ch_t"    -> kids()
    )

    // s2 — insert "car": shares "ca", branches at 'a' into r(end).
    private val s2 = mk(
      "car",
      "root"    -> tnode("ch_root", isEnd = false),
      "ch_root" -> kids("c" -> "c"),
      "c"       -> tnode("ch_c", isEnd = false),
      "ch_c"    -> kids("a" -> "a"),
      "a"       -> tnode("ch_a", isEnd = false),
      "ch_a"    -> kids("t" -> "t", "r" -> "r"),
      "t"       -> tnode("ch_t", isEnd = true),
      "ch_t"    -> kids(),
      "r"       -> tnode("ch_r", isEnd = true),
      "ch_r"    -> kids()
    )

    // s3 — insert "do": a second top-level branch root → d → o(end).
    private val s3 = mk(
      "do",
      "root"    -> tnode("ch_root", isEnd = false),
      "ch_root" -> kids("c" -> "c", "d" -> "d"),
      "c"       -> tnode("ch_c", isEnd = false),
      "ch_c"    -> kids("a" -> "a"),
      "a"       -> tnode("ch_a", isEnd = false),
      "ch_a"    -> kids("t" -> "t", "r" -> "r"),
      "t"       -> tnode("ch_t", isEnd = true),
      "ch_t"    -> kids(),
      "r"       -> tnode("ch_r", isEnd = true),
      "ch_r"    -> kids(),
      "d"       -> tnode("ch_d", isEnd = false),
      "ch_d"    -> kids("o" -> "o"),
      "o"       -> tnode("ch_o", isEnd = true),
      "ch_o"    -> kids()
    )

    val trace: HeapTrace = HeapTrace(List(s1, s2, s3), truncated = false)

    val source: String =
      """def insert(root, word):
        |    node = root
        |    for ch in word:
        |        if ch not in node.children:
        |            node.children[ch] = TrieNode()
        |        node = node.children[ch]
        |    node.is_end = True
        |""".stripMargin

  /** Insert "cat"/"car"/"do" into a trie; `vizKind="trie"` → bespoke whole-graph TrieRenderer. */
  val trieTrace: Fixture = Fixture(
    name = "trie",
    title = "Trie — prefix tree",
    source = Trie.source,
    rootHint = Some("root"),
    layoutHint = "",
    vizKind = Some("trie"),
    trace = Trie.trace
  )

  // ─── Fixture 11: Bitset (bespoke renderer #8 — ADR-0027) ─────────────────────────
  //
  // A fixed-width bit array: each cell holds 0 (clear) or 1 (set). `vizKind="bitset"`
  // → the bespoke BitsetRenderer draws a horizontal row of bit cells — set bits
  // filled, clear bits muted — with a `n bits · k set` popcount summary above the
  // row. Like [[heapTrace]], the only local is the structure itself (`bits`); the
  // loop index isn't surfaced, so no stray index cursor is drawn (the renderer
  // marks the touched bit via the per-step `changed` diff instead). The trace
  // allocates 8 clear bits, sets three (1, 4, 6), then clears one (4) — set then
  // clear, each op flipping exactly one cell so the diff reads as the bit that moved.

  private object Bitset:

    private def mk(line: Int, bits: List[Long]): HeapStep =
      step(
        line,
        "apply_ops",
        List("bits" -> ref("B")),
        "B" -> HeapObject.Arr(ArrKind.Lst, bits.map(int))
      )

    val trace: HeapTrace = HeapTrace(
      List(
        mk(2, List(0, 0, 0, 0, 0, 0, 0, 0)), // allocate 8 clear bits
        mk(4, List(0, 1, 0, 0, 0, 0, 0, 0)), // set bit 1
        mk(4, List(0, 1, 0, 0, 1, 0, 0, 0)), // set bit 4
        mk(4, List(0, 1, 0, 0, 1, 0, 1, 0)), // set bit 6
        mk(4, List(0, 1, 0, 0, 0, 0, 1, 0))  // clear bit 4
      ),
      truncated = false
    )

    val source: String =
      """def apply_ops(bits, ops):
        |    # bits starts all-zero; each op sets (1) or clears (0) one position
        |    for i, val in ops:
        |        bits[i] = val
        |""".stripMargin

  /** Allocate 8 bits, set 1/4/6, then clear 4; `vizKind="bitset"` → bespoke BitsetRenderer. */
  val bitsetTrace: Fixture = Fixture(
    name = "bitset",
    title = "Bitset — set & clear",
    source = Bitset.source,
    rootHint = Some("bits"),
    layoutHint = "",
    vizKind = Some("bitset"),
    trace = Bitset.trace
  )

  // ─── Fixture 12: Linked list (bespoke renderer #9 — ADR-0027) ────────────────────
  //
  // A singly-linked list: `Instance(value, next: Ref)` nodes chained head→tail.
  // The adapter groups the chain into ONE card (Instance-to-Instance refs union),
  // emits a `next`-labelled edge per link, and — from the `head` / `cur` Ref
  // locals — a `head` caret on the first node + a `cur` caret that advances, all
  // for free. The generic path already lays this out as a left-to-right chain via
  // linkedListLayout. `vizKind="list-single"` → the bespoke LinkedListRenderer adds
  // the one thing the generic path omits: a null sentinel (a dashed ∅ circle) after
  // the tail — the node whose `next` is None, so no outgoing `next` edge — so the
  // chain visibly terminates at null.
  //
  // The trace TRAVERSES a static chain (`cur = head; while cur: cur = cur.next`)
  // rather than mutating it: the structure is identical every step, so the union
  // of `next` edges stays a single simple path (a linear forest) and linkedListLayout
  // keeps the horizontal chain — an append trace rebinds the old tail's `next` from
  // null to the new node, giving that node two `next` targets across the union and
  // forcing the layout's force-graph fallback. Pointer-chasing is also the canonical
  // linked-list operation, and the moving `cur` is what keeps the steps distinct.

  private object LinkedList:

    private def node(value: Long, next: HeapValue): HeapObject =
      instance("ListNode", "value" -> int(value), "next" -> next)

    // Static chain 1 → 2 → 3 → null; only the `cur` traversal pointer advances.
    private val chain: List[(String, HeapObject)] = List(
      "N1" -> node(1, ref("N2")),
      "N2" -> node(2, ref("N3")),
      "N3" -> node(3, nul)
    )

    private def mk(cur: String): HeapStep =
      step(4, "traverse", List("head" -> ref("N1"), "cur" -> ref(cur)), chain*)

    val trace: HeapTrace = HeapTrace(
      List(mk("N1"), mk("N2"), mk("N3")),
      truncated = false
    )

    val source: String =
      """def traverse(head):
        |    cur = head
        |    while cur is not None:
        |        visit(cur.value)
        |        cur = cur.next
        |""".stripMargin

  /**
   * Traverse a static 1→2→3 list with a `cur` pointer; `vizKind="list-single"` → bespoke LinkedListRenderer.
   */
  val linkedListTrace: Fixture = Fixture(
    name = "linked-list",
    title = "Linked list — traversal",
    source = LinkedList.source,
    rootHint = Some("head"),
    layoutHint = "",
    vizKind = Some("list-single"),
    trace = LinkedList.trace
  )

  // ─── Fixture 13: Segment tree (bespoke renderer #10 — ADR-0027) ──────────────────
  //
  // A sum segment tree over the array [3,1,4,2]: each node covers a range `[lo,hi]`
  // and stores the aggregate of that slice; leaves are the individual elements
  // (`[i,i]`). Modelled as an `Instance(value, lo, hi, left, right)` binary tree
  // (like AVL) — `value` (the aggregate) is in `ValueFields` so it's the in-circle
  // label AND, as a multi-scalar class, stays in `meta` alongside `lo`/`hi`; the
  // renderer reads value/lo/hi uniformly from `meta`, and `left`/`right` refs
  // become the tree edges it walks to assign each node a level.
  //
  // `vizKind="segment-tree"` → the bespoke SegmentTreeRenderer draws the RANGE-BAR
  // OVERLAY: a column grid `n` wide, each node a bar at `grid-column: lo+1 / hi+2`
  // on its level's row, so every node sits directly over the array slice it covers
  // (leaves styled as the array cells, an index row beneath). The structure is the
  // star — the generic tree path would draw a plain binary tree that hides the
  // "each node owns a contiguous range" idea.
  //
  // The tree is STATIC; a `cur` pointer descends root → [0,1] → [1,1] (a point
  // query/update path), which keeps the steps distinct without mutating the
  // structure (no graph-layout step-union to worry about — this renderer positions
  // bars itself from lo/hi/level, never via a delegated layout). `root` is the
  // stable reachability anchor (rootHint); the renderer emphasises only the `cur`
  // node each step.

  private object SegTree:

    // `value` (in ValueFields) becomes the in-circle label → the adapter narrates
    // "cur → 4" not "cur → SegNode"; it ALSO stays in `meta` (multi-scalar class),
    // so the renderer still reads value/lo/hi uniformly from `meta`.
    private def seg(lo: Int, hi: Int, sum: Long, left: HeapValue, right: HeapValue): HeapObject =
      instance(
        "SegNode",
        "value" -> int(sum),
        "lo"    -> int(lo),
        "hi"    -> int(hi),
        "left"  -> left,
        "right" -> right
      )

    private def leaf(at: Int, v: Long): HeapObject = seg(at, at, v, nul, nul)

    // Static complete tree over [3,1,4,2]; only `cur` advances.
    private val nodes: List[(String, HeapObject)] = List(
      "root" -> seg(0, 3, 10, ref("L01"), ref("R23")),
      "L01"  -> seg(0, 1, 4, ref("n0"), ref("n1")),
      "R23"  -> seg(2, 3, 6, ref("n2"), ref("n3")),
      "n0"   -> leaf(0, 3),
      "n1"   -> leaf(1, 1),
      "n2"   -> leaf(2, 4),
      "n3"   -> leaf(3, 2)
    )

    private def mk(cur: String): HeapStep =
      step(5, "query_point", List("root" -> ref("root"), "cur" -> ref(cur)), nodes*)

    // Descend to index 1: root [0,3] → L01 [0,1] → n1 [1,1].
    val trace: HeapTrace = HeapTrace(
      List(mk("root"), mk("L01"), mk("n1")),
      truncated = false
    )

    val source: String =
      """def query_point(root, i):
        |    node = root
        |    while node.lo != node.hi:        # descend until a leaf [i, i]
        |        mid = (node.lo + node.hi) // 2
        |        node = node.left if i <= mid else node.right
        |    return node.sum
        |""".stripMargin

  /**
   * Point-descend a static sum segment tree over [3,1,4,2]; `vizKind="segment-tree"` → bespoke
   * SegmentTreeRenderer.
   */
  val segmentTreeTrace: Fixture = Fixture(
    name = "segment-tree",
    title = "Segment tree — range sums over [3, 1, 4, 2]",
    source = SegTree.source,
    rootHint = Some("root"),
    layoutHint = "",
    vizKind = Some("segment-tree"),
    trace = SegTree.trace
  )

  // ─── Fixture 14: Fenwick tree (Binary Indexed Tree) — bespoke renderer #11 ───────
  //
  // A Fenwick / BIT stores prefix-sum partial aggregates in a 1-indexed array
  // `tree[1..n]`: `tree[i]` holds the sum of the half-open range `(i - lowbit(i), i]`
  // where `lowbit(i) = i & -i`. A prefix-sum query walks `i -= lowbit(i)` down to 0,
  // accumulating O(log n) disjoint ranges that tile `[1..i]`.
  //
  // `vizKind="fenwick"` → the bespoke FenwickRenderer draws the RESPONSIBILITY
  // STAIRCASE: a column grid `n` wide (one column per index), each cell a bar at
  // `grid-column: (i-lowbit+1) / (i+1)` on its low-bit level (`log2(lowbit)`), so
  // every cell sits directly over the slice it is responsible for — index 8 spans
  // the whole array, 4 the left half, the odd indices are singletons. The cursor
  // climbs the staircase as the query jumps.
  //
  // Stored with a DUMMY slot 0 (the conventional unused `tree[0]`) so the Arr slot
  // equals the Fenwick index: a frame-local `i` (an IndexName) then lands the
  // adapter's index-cursor on cell `tree#i` (slot == index — without the dummy it
  // would be off by one). The array is STATIC; only `i` (and `acc`) advance — a
  // prefix-sum query(7) descending 7 → 6 → 4 — so the steps stay distinct with no
  // structure mutation (the renderer positions bars itself from the slot: no
  // delegated layout, no graph-layout step-union to keep well-formed).
  //
  // Underlying array [1,2,3,4,5,6,7,8] → BIT = [_,1,3,3,10,5,11,7,36]; query(7)
  // accumulates tree[7] + tree[6] + tree[4] = 7 + 11 + 10 = 28 = 1+2+…+7.

  private object Fenwick:

    // tree[1..8] over arr = [1..8]; slot 0 is the unused 1-indexing sentinel.
    private val bit: List[Long] = List(0, 1, 3, 3, 10, 5, 11, 7, 36)

    private def mk(i: Long, acc: Long): HeapStep =
      step(
        4,
        "prefix_sum",
        List("tree" -> ref("T"), "i" -> int(i), "acc" -> int(acc)),
        "T" -> HeapObject.Arr(ArrKind.Lst, bit.map(int))
      )

    // query(7): acc += tree[7]=7 (i=7) → tree[6]=11 (i=6) → tree[4]=10 (i=4); i→0 stops.
    val trace: HeapTrace = HeapTrace(
      List(mk(7, 7), mk(6, 18), mk(4, 28)),
      truncated = false
    )

    val source: String =
      """def prefix_sum(tree, i):
        |    acc = 0
        |    while i > 0:
        |        acc += tree[i]      # tree[i] covers (i - lowbit(i), i]
        |        i -= i & -i         # jump to the next responsible cell
        |    return acc
        |""".stripMargin

  /** Prefix-sum query over a static Fenwick tree; `vizKind="fenwick"` → bespoke FenwickRenderer. */
  val fenwickTrace: Fixture = Fixture(
    name = "fenwick",
    title = "Fenwick tree (BIT) — prefix-sum query(7)",
    source = Fenwick.source,
    rootHint = Some("tree"),
    layoutHint = "",
    vizKind = Some("fenwick"),
    trace = Fenwick.trace
  )

  // ─── Fixture 15: Skip list — bespoke renderer #12 (final shape) ──────────────────
  //
  // A skip list is a sorted level-0 linked chain where each node is ALSO present in a
  // random number of express lanes above it: node i reaches level `level[i]`
  // (0-indexed), so it occupies grid rows 0..level[i]. Search drops from the top lane,
  // advancing while the next key < target and dropping a level when it would overshoot
  // — O(log n) expected.
  //
  // `vizKind="skiplist"` → the bespoke SkipListRenderer draws the design's GRID: one
  // row per level (top lane first), one column per node (sorted order), each node a
  // cell in every row it reaches and a thin gap line elsewhere so columns stay aligned;
  // `head` and `∅` sentinels bookend each row. Matches the Claude Design handoff's
  // SkipListRenderer (advanced-renderers.jsx) — a bespoke CSS grid like segment-tree /
  // fenwick, NOT the delegate-circle skiplist-layout.ts (that backs the inline
  // `d3 widget=skiplist` fence path; see the appendix widget-catalog entry).
  //
  // Modelled as a level-0 `next` chain of Instance("SkipNode", value, level) — the
  // Instance→Instance refs union into ONE card (per-card, like the linked list); the
  // renderer reads `value` (label) + `level` (meta, the top lane reached), orders
  // columns by value, and emphasises the `cur` search pointer's whole column. The
  // structure is STATIC; only `cur` advances (search 19: express-hops to 7, then finds
  // 19), so steps stay distinct with no mutation — the renderer positions the grid
  // itself, so there is no graph-layout step-union to keep well-formed. `head` is the
  // reachability anchor (rootHint); the renderer emphasises only the `cur` column.
  //
  // Example mirrors the design mock: [3,7,12,19,25,31,47] with levels [0,2,0,1,2,0,0]
  // (7 and 25 are the level-2 express nodes; 19 reaches level 1), 3 levels total.

  private object SkipList:

    private def node(value: Long, level: Int, next: HeapValue): HeapObject =
      instance("SkipNode", "value" -> int(value), "level" -> int(level), "next" -> next)

    // Sorted level-0 chain; `level` = the top express lane each key reaches (0-indexed).
    private val nodes: List[(String, HeapObject)] = List(
      "n3"  -> node(3, 0, ref("n7")),
      "n7"  -> node(7, 2, ref("n12")),
      "n12" -> node(12, 0, ref("n19")),
      "n19" -> node(19, 1, ref("n25")),
      "n25" -> node(25, 2, ref("n31")),
      "n31" -> node(31, 0, ref("n47")),
      "n47" -> node(47, 0, nul)
    )

    private def mk(cur: String): HeapStep =
      step(5, "search", List("head" -> ref("n3"), "target" -> int(19), "cur" -> ref(cur)), nodes*)

    // search(19): hop the level-2 express lane to 7, then descend to find 19.
    val trace: HeapTrace = HeapTrace(
      List(mk("n7"), mk("n19")),
      truncated = false
    )

    val source: String =
      """def search(head, target):
        |    node = head
        |    for level in range(head.level, -1, -1):     # top express lane, descend
        |        while node.forward[level] and node.forward[level].value < target:
        |            node = node.forward[level]          # hop along this lane
        |    node = node.forward[0]
        |    return node if node and node.value == target else None
        |""".stripMargin

  /** Search a static 3-level skip list for 19; `vizKind="skiplist"` → bespoke SkipListRenderer. */
  val skiplistTrace: Fixture = Fixture(
    name = "skiplist",
    title = "Skip list — search(19) over [3, 7, 12, 19, 25, 31, 47]",
    source = SkipList.source,
    rootHint = Some("head"),
    layoutHint = "",
    vizKind = Some("skiplist"),
    trace = SkipList.trace
  )

  // ─── Fixture 16: plain array — bespoke ArrayRenderer (default for array-1d) ──────
  //
  // The single most common shape in the book. Unlike every other fixture this carries
  // `vizKind = None`: a plain `array-1d` card has no bespoke structureType, so it must
  // render via the ArrayRenderer BY DEFAULT (the layoutKind fallback in index.ts), not
  // the generic circle layout. Models a two-pointer reverse of [1,2,3,4,5]: `left`/`right`
  // index locals (an IndexNames pair) walk inward, the adapter draws them as cell cursors
  // (blue `left` / bordeaux `right` via MarkerColors), and each swap flips two cells so
  // `diffSteps` marks them `changed`.

  private object ArrayTwoPointer:

    private def mk(line: Int, arr: List[Long], left: Long, right: Long): HeapStep =
      step(
        line,
        "reverse",
        List("arr" -> ref("A"), "left" -> int(left), "right" -> int(right)),
        "A" -> HeapObject.Arr(ArrKind.Lst, arr.map(int))
      )

    // reverse([1,2,3,4,5]): swap ends inward until the pointers meet.
    val trace: HeapTrace = HeapTrace(
      List(
        mk(4, List(1, 2, 3, 4, 5), 0, 4),
        mk(4, List(5, 2, 3, 4, 1), 1, 3),
        mk(4, List(5, 4, 3, 2, 1), 2, 2)
      ),
      truncated = false
    )

    val source: String =
      """def reverse(arr):
        |    left, right = 0, len(arr) - 1
        |    while left < right:
        |        arr[left], arr[right] = arr[right], arr[left]
        |        left, right = left + 1, right - 1
        |    return arr
        |""".stripMargin

  /**
   * Two-pointer reverse over a plain array; `vizKind = None` → bespoke ArrayRenderer via the array-1d
   * default.
   */
  val arrayTrace: Fixture = Fixture(
    name = "array",
    title = "Array — two-pointer reverse of [1, 2, 3, 4, 5]",
    source = ArrayTwoPointer.source,
    rootHint = Some("arr"),
    layoutHint = "",
    vizKind = None,
    trace = ArrayTwoPointer.trace
  )

  /** Every fixture, in deterministic order — used by both the spec and the sbt `genTraceFixtures` task. */
  val all: List[Fixture] = List(
    avlRotationTrace,
    graphBfsTrace,
    hashmapChainedCollisionsTrace,
    stackPushTrace,
    hashmapKindTrace,
    graphKindTrace,
    queueTrace,
    heapTrace,
    unionFindTrace,
    trieTrace,
    bitsetTrace,
    linkedListTrace,
    segmentTreeTrace,
    fenwickTrace,
    skiplistTrace,
    arrayTrace
  )
