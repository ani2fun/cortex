package cortex.shared.viz

import zio.test.*

/**
 * JVM unit tests for [[HeapToGraph]] — tree reconstruction from hand-built [[HeapTrace]] fixtures, the
 * inserted-node highlight, the changed / removed node diff, edge labels, the cursor, root resolution (hint +
 * auto-detect), step coalescing, carry-forward across frames that don't reach the root, and the truncated
 * flag. Also the array / dict adapter: list cells carrying their index, dict entries keyed by identity,
 * collection diffs and narration, and root resolution through an instance attribute. Integer index locals
 * (`i` / `lo` / `hi` / `mid`) drawn as role-coloured cursors on array cells — gated on an in-range value and
 * an index-like name. And multi-case segmentation: a trace split into one [[VizGraph]] per test case,
 * recursion not mistaken for a boundary, and the `viz-case` count override. No Python, no DOM.
 *
 * Most fixtures are single-case; [[adaptSingle]] adapts and unwraps the lone case so those assertions read
 * against a `VizGraph`. The segmentation tests call `HeapToGraph.adapt` directly and assert on `VizCases`.
 */
object HeapToGraphSpec extends ZIOSpecDefault:

  private def int(n: Long): HeapValue    = HeapValue.Scalar(HeapScalar.I(n))
  private val nul: HeapValue             = HeapValue.Scalar(HeapScalar.Null)
  private def ref(id: String): HeapValue = HeapValue.Ref(id)

  private def treeNode(value: Long, left: HeapValue, right: HeapValue): HeapObject =
    HeapObject.Instance("TreeNode", List("val" -> int(value), "left" -> left, "right" -> right))

  private def str(s: String): HeapValue = HeapValue.Scalar(HeapScalar.S(s))

  private def lst(items: HeapValue*): HeapObject = HeapObject.Arr(ArrKind.Lst, items.toList)

  private def dict(entries: (HeapValue, HeapValue)*): HeapObject = HeapObject.Dict(entries.toList)

  private def instance(cls: String, fields: (String, HeapValue)*): HeapObject =
    HeapObject.Instance(cls, fields.toList)

  private def step(
      line: Int,
      locals: List[(String, HeapValue)],
      heap: (String, HeapObject)*
  ): HeapStep =
    HeapStep(line, "line", List(HeapFrame("fn", locals)), heap.toMap)

  /** A step with a pre-built heap Map — for the multi-case fixtures that share one heap across steps. */
  private def mstep(
      line: Int,
      locals: List[(String, HeapValue)],
      heap: Map[String, HeapObject]
  ): HeapStep =
    HeapStep(line, "line", List(HeapFrame("fn", locals)), heap)

  /** adapt for the single-case fixtures — returns the one case, so the assertions read against a VizGraph. */
  private def adaptSingle(
      trace: HeapTrace,
      source: String,
      layoutHint: String,
      rootHint: Option[String],
      title: String
  ): Either[String, VizGraph] =
    HeapToGraph.adapt(trace, source, layoutHint, rootHint, None, title).map(_.cases.head)

  // tree [1, 2, 3, 4] — A=1 (l=B, r=C), B=2 (l=D, r=null), C=3, D=4
  private val before = step(
    2,
    List("tree" -> ref("A")),
    "A" -> treeNode(1, ref("B"), ref("C")),
    "B" -> treeNode(2, ref("D"), nul),
    "C" -> treeNode(3, nul, nul),
    "D" -> treeNode(4, nul, nul)
  )

  // after — node 9 (E) attached as B's right child
  private val after = step(
    3,
    List("tree" -> ref("A"), "node" -> ref("B")),
    "A" -> treeNode(1, ref("B"), ref("C")),
    "B" -> treeNode(2, ref("D"), ref("E")),
    "C" -> treeNode(3, nul, nul),
    "D" -> treeNode(4, nul, nul),
    "E" -> treeNode(9, nul, nul)
  )

  private val src = "line one\nline two\nline three"

  override def spec: Spec[Any, Any] = suite("HeapToGraph")(
    test("viz=array auto-detects a function-scoped Arr even when the final step holds only a dict") {
      // Step 1 (inside the function) binds `arr` to a list; the final step (after the
      // function returns) holds only a higher-reachability dict — mirroring
      // `def solve(self, arr): … return count`. A final-heap-only scan would miss `arr`
      // and the dict would win, rendering an empty canvas; the all-steps Arr scan picks
      // the list.
      val inFn = step(1, List("arr" -> ref("L")), "L" -> lst(int(5), int(2)))
      val afterReturn = step(
        2,
        List("seen" -> ref("D")),
        "D"  -> dict(str("a") -> ref("E1"), str("b") -> ref("E2")),
        "E1" -> instance("Entry", "v" -> int(1)),
        "E2" -> instance("Entry", "v" -> int(2))
      )
      val result =
        adaptSingle(HeapTrace(List(inFn, afterReturn), truncated = false), "x = 1\ny = 2", "array", None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.exists(_.nodes.exists(_.label == "5"))),
        result.exists(_.steps.exists(_.nodes.exists(_.label == "2")))
      )
    },
    test("reconstructs the tree, highlights the inserted node, labels edges") {
      val result = adaptSingle(
        HeapTrace(List(before, after), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.head.nodes.size == 4),
        result.exists(_.steps.last.nodes.size == 5),
        result.exists(_.steps.last.nodes.exists(_.label == "9")),
        result.exists(_.steps.last.highlight == List("E")),
        result.exists(_.steps.last.edges.contains(VizEdge("B", "E", "right"))),
        result.exists(_.steps.last.edges.contains(VizEdge("A", "B", "left"))),
        result.exists(_.steps.last.cursor.size == 2),
        result.exists(_.steps.last.cursor.exists(c => c.name == "node" && c.target == "B")),
        result.exists(_.steps.last.cursor.exists(c => c.name == "node" && c.color == "#4f5bd5")),
        result.exists(_.steps.last.cursor.exists(c => c.name == "tree" && c.color == "#3a5a8c")),
        result.exists(_.steps.last.annotation.title == "inserted 9 as 2.right"),
        result.exists(_.layoutHint == "binary-tree")
      )
    },
    test("auto-detects the root when no hint is given") {
      val result =
        adaptSingle(HeapTrace(List(before), truncated = false), src, "binary-tree", None, "t")
      assertTrue(result.isRight, result.exists(_.steps.head.nodes.size == 4))
    },
    test("coalesces only exact consecutive duplicates (same line and same state)") {
      val result = adaptSingle(
        HeapTrace(List(before, before), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(result.exists(_.steps.size == 1))
    },
    test("keeps a step on a new source line even when the diagram is unchanged") {
      // identical heap + locals to `before`, only the source line differs — stepping
      // the core algorithm should follow the code line by line, not just the redraws
      val nextLine = step(
        3,
        List("tree" -> ref("A")),
        "A" -> treeNode(1, ref("B"), ref("C")),
        "B" -> treeNode(2, ref("D"), nul),
        "C" -> treeNode(3, nul, nul),
        "D" -> treeNode(4, nul, nul)
      )
      val result = adaptSingle(
        HeapTrace(List(before, nextLine), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(result.isRight, result.exists(_.steps.size == 2))
    },
    test("propagates the truncated flag") {
      val result =
        adaptSingle(HeapTrace(List(before), truncated = true), src, "binary-tree", Some("tree"), "t")
      assertTrue(result.exists(_.truncated))
    },
    test("an empty trace is a Left") {
      val result = adaptSingle(HeapTrace(Nil, truncated = false), src, "binary-tree", None, "t")
      assertTrue(result.isLeft)
    },
    test("surfaces non-pointer scalar fields as node meta") {
      val avl = step(
        1,
        List("tree" -> ref("A")),
        "A" -> HeapObject.Instance(
          "AvlNode",
          List("key" -> int(10), "left" -> nul, "right" -> nul, "height" -> int(2))
        )
      )
      val result =
        adaptSingle(HeapTrace(List(avl), truncated = false), src, "binary-tree", Some("tree"), "t")
      // Multi-scalar instances now surface the value field's name alongside the
      // others — readers can map the unlabeled "10" inside the circle to its
      // attribute name (`key=10`) instead of guessing.
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.head.label == "10"),
        result.exists(
          _.steps.head.nodes.head.meta == List(VizField("key", "10"), VizField("height", "2"))
        )
      )
    },
    test("carries the structure forward through a step whose frame doesn't reach the root") {
      // a helper frame mid-trace — its heap holds an unrelated object, no tree node
      val helper = step(
        1,
        List("self" -> ref("X")),
        "X" -> HeapObject.Instance("TreeNode", List("val" -> int(0), "left" -> nul, "right" -> nul))
      )
      val result = adaptSingle(
        HeapTrace(List(before, helper, after), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 3),
        result.exists(_.steps.forall(_.nodes.nonEmpty)),
        result.exists(_.steps(1).nodes.size == 4),
        result.exists(_.steps(1).cursor.isEmpty),
        result.exists(_.steps(1).annotation.title == "line one"),
        result.exists(_.steps.last.nodes.size == 5),
        result.exists(_.steps.last.highlight == List("E"))
      )
    },
    test("flags a node whose display value changed since the previous step") {
      val s1 = step(1, List("tree" -> ref("A")), "A" -> treeNode(5, nul, nul))
      val s2 = step(2, List("tree" -> ref("A")), "A" -> treeNode(7, nul, nul))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.head.changed.isEmpty),
        result.exists(_.steps.last.changed == List("A")),
        result.exists(_.steps.last.removed.isEmpty),
        result.exists(_.steps.last.nodes.head.label == "7"),
        result.exists(_.steps.last.annotation.title == "5 changed to 7")
      )
    },
    test("re-emits a removed node once, carrying its last-known label") {
      // s1: A has B as its left child; s2: A.left is nulled, so B is gone
      val s1 = step(
        1,
        List("tree" -> ref("A")),
        "A" -> treeNode(1, ref("B"), nul),
        "B" -> treeNode(2, nul, nul)
      )
      val s2 = step(2, List("tree" -> ref("A")), "A" -> treeNode(1, nul, nul))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.head.removed.isEmpty),
        result.exists(_.steps.last.removed == List("B")),
        result.exists(_.steps.last.nodes.size == 2),
        result.exists(_.steps.last.nodes.exists(n => n.id == "B" && n.label == "2")),
        result.exists(_.steps.last.annotation.title == "removed 2")
      )
    },
    test("drops builder and constructor frames before adapting") {
      val tree3 = Map(
        "A" -> treeNode(1, ref("B"), ref("C")),
        "B" -> treeNode(2, nul, nul),
        "C" -> treeNode(3, nul, nul)
      )
      val builder =
        HeapStep(1, "line", List(HeapFrame("from_level_order", List("tree" -> ref("A")))), tree3)
      val ctor =
        HeapStep(1, "line", List(HeapFrame("__init__", List("self" -> ref("B")))), tree3)
      val solve =
        HeapStep(
          2,
          "line",
          List(HeapFrame("solve", List("tree" -> ref("A"), "node" -> ref("B")))),
          tree3
        )
      val result = adaptSingle(
        HeapTrace(List(builder, ctor, solve), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(
        result.isRight,
        // builder (from_*) + constructor (__init__) frames dropped → only `solve` survives
        result.exists(_.steps.size == 1),
        result.exists(_.steps.head.cursor.exists(_.name == "node"))
      )
    },
    test("renders a Python list as one indexed cell per element") {
      val s = step(1, List("nums" -> ref("L")), "L" -> lst(int(10), int(20), int(30)))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.map(_.id) == List("L#0", "L#1", "L#2")),
        result.exists(_.steps.head.nodes.map(_.label) == List("10", "20", "30")),
        result.exists(_.steps.head.nodes.map(_.slot) == List(Some(0), Some(1), Some(2))),
        result.exists(_.steps.head.nodes.forall(_.kind == "cell")),
        result.exists(_.steps.head.edges.isEmpty)
      )
    },
    test("highlights an appended list cell and narrates the append") {
      val s1 = step(1, List("nums" -> ref("L")), "L" -> lst(int(10), int(20)))
      val s2 = step(2, List("nums" -> ref("L")), "L" -> lst(int(10), int(20), int(30)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.last.highlight == List("L#2")),
        result.exists(_.steps.last.annotation.title == "added 30")
      )
    },
    test("flags a list cell whose value changed since the previous step") {
      val s1 = step(1, List("nums" -> ref("L")), "L" -> lst(int(1), int(2), int(3)))
      val s2 = step(2, List("nums" -> ref("L")), "L" -> lst(int(1), int(9), int(3)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.last.changed == List("L#1")),
        result.exists(_.steps.last.nodes.find(_.id == "L#1").exists(_.label == "9")),
        result.exists(_.steps.last.annotation.title == "2 changed to 9")
      )
    },
    test("re-emits a popped list cell once, carrying its last-known value") {
      val s1 = step(1, List("nums" -> ref("L")), "L" -> lst(int(1), int(2), int(3)))
      val s2 = step(2, List("nums" -> ref("L")), "L" -> lst(int(1), int(2)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.last.removed == List("L#2")),
        result.exists(_.steps.last.nodes.exists(n => n.id == "L#2" && n.label == "3")),
        result.exists(_.steps.last.annotation.title == "removed 3")
      )
    },
    test("renders a Python dict as one entry node per key") {
      val s = step(1, List("freq" -> ref("D")), "D" -> dict(str("a") -> int(1), str("b") -> int(2)))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "hash", Some("freq"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.map(_.id) == List("D#a", "D#b")),
        result.exists(_.steps.head.nodes.map(_.label) == List("1", "2")),
        result.exists(_.steps.head.nodes.forall(_.kind == "entry")),
        result.exists(_.steps.head.nodes.forall(_.slot.isEmpty)),
        result.exists(
          _.steps.head.nodes.flatMap(_.meta) == List(VizField("key", "a"), VizField("key", "b"))
        )
      )
    },
    test("narrates a dict insertion with its key") {
      val s1 = step(1, List("freq" -> ref("D")), "D" -> dict(str("a") -> int(1)))
      val s2 = step(2, List("freq" -> ref("D")), "D" -> dict(str("a") -> int(1), str("b") -> int(2)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "hash", Some("freq"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.last.highlight == List("D#b")),
        result.exists(_.steps.last.annotation.title == "b = 2")
      )
    },
    test("narrates a dict value update with its key") {
      val s1 = step(1, List("freq" -> ref("D")), "D" -> dict(str("a") -> int(1)))
      val s2 = step(2, List("freq" -> ref("D")), "D" -> dict(str("a") -> int(5)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "hash", Some("freq"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.last.changed == List("D#a")),
        result.exists(_.steps.last.annotation.title == "a changed from 1 to 5")
      )
    },
    test("draws a list of references as cells linked to their targets") {
      val s = step(
        1,
        List("nodes" -> ref("L")),
        "L"  -> lst(ref("N1"), ref("N2")),
        "N1" -> treeNode(1, nul, nul),
        "N2" -> treeNode(2, nul, nul)
      )
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nodes"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.size == 4),
        result.exists(_.steps.head.nodes.exists(n => n.id == "L#0" && n.label == "·" && n.slot.contains(0))),
        result.exists(_.steps.head.nodes.exists(n => n.id == "N1" && n.label == "1")),
        result.exists(_.steps.head.edges.contains(VizEdge("L#0", "N1", ""))),
        result.exists(_.steps.head.edges.contains(VizEdge("L#1", "N2", "")))
      )
    },
    test("resolves viz-root through an instance attribute (self.heap)") {
      val s = step(
        1,
        List("self" -> ref("M")),
        "M" -> instance("MinHeap", "heap" -> ref("L")),
        "L" -> lst(int(1), int(3), int(5))
      )
      val trace  = HeapTrace(List(s), truncated = false)
      val dotted = adaptSingle(trace, src, "array", Some("self.heap"), "t")
      val bare   = adaptSingle(trace, src, "array", Some("heap"), "t")
      assertTrue(
        dotted.isRight,
        dotted.exists(_.steps.head.nodes.size == 3),
        dotted.exists(_.steps.head.nodes.forall(_.kind == "cell")),
        bare.isRight,
        bare.exists(_.steps.head.nodes.size == 3)
      )
    },
    test("auto-detects an array root when no hint is given") {
      val s      = step(1, List("nums" -> ref("L")), "L" -> lst(int(1), int(2), int(3)))
      val result = adaptSingle(HeapTrace(List(s), truncated = false), src, "array", None, "t")
      assertTrue(result.isRight, result.exists(_.steps.head.nodes.size == 3))
    },
    test("links an instance to every cell of a list it holds") {
      val s = step(
        1,
        List("g" -> ref("G")),
        "G"  -> instance("Graph", "nodes" -> ref("L")),
        "L"  -> lst(ref("N1"), ref("N2")),
        "N1" -> treeNode(1, nul, nul),
        "N2" -> treeNode(2, nul, nul)
      )
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "graph", Some("g"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.size == 5),
        result.exists(_.steps.head.edges.contains(VizEdge("G", "L#0", "nodes"))),
        result.exists(_.steps.head.edges.contains(VizEdge("G", "L#1", "nodes"))),
        result.exists(_.steps.head.edges.contains(VizEdge("L#0", "N1", ""))),
        result.exists(_.steps.head.edges.contains(VizEdge("L#1", "N2", "")))
      )
    },
    test("keys dict entries by identity so tuple keys do not collide") {
      val s = step(
        1,
        List("memo" -> ref("D")),
        "D"  -> dict(ref("T1") -> int(5), ref("T2") -> int(8)),
        "T1" -> HeapObject.Arr(ArrKind.Tup, List(int(0), int(0))),
        "T2" -> HeapObject.Arr(ArrKind.Tup, List(int(0), int(1)))
      )
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "grid", Some("memo"), "t")
      val entries = result.toOption.toList.flatMap(_.steps.head.nodes).filter(_.kind == "entry")
      assertTrue(
        result.isRight,
        entries.size == 2,
        entries.map(_.id).distinct.size == 2,
        entries.map(_.label) == List("5", "8"),
        entries.flatMap(_.meta) == List(VizField("key", "(0, 0)"), VizField("key", "(0, 1)"))
      )
    },
    test("draws an integer index local as a role-coloured cursor on an array cell") {
      val s = step(1, List("nums" -> ref("L"), "i" -> int(1)), "L" -> lst(int(10), int(20), int(30)))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.cursor.exists(c => c.name == "i" && c.target == "L#1")),
        result.exists(_.steps.head.cursor.exists(c => c.name == "i" && c.color == "#3a5a8c"))
      )
    },
    test("does not draw an integer index local that has run out of range") {
      // i has stepped past the last index — there is no cell to land on
      val s = step(1, List("nums" -> ref("L"), "i" -> int(5)), "L" -> lst(int(10), int(20)))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(result.isRight, result.exists(_.steps.head.cursor.isEmpty))
    },
    test("does not draw a non-index-named integer local as a cursor") {
      // `count` holds a valid index value but is an accumulator, not a position
      val s =
        step(1, List("nums" -> ref("L"), "count" -> int(1)), "L" -> lst(int(10), int(20), int(30)))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(result.isRight, result.exists(_.steps.head.cursor.isEmpty))
    },
    test("advances an integer index cursor as its value changes between steps") {
      val s1 = step(1, List("nums" -> ref("L"), "i" -> int(0)), "L" -> lst(int(10), int(20), int(30)))
      val s2 = step(2, List("nums" -> ref("L"), "i" -> int(1)), "L" -> lst(int(10), int(20), int(30)))
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "array", Some("nums"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.head.cursor.exists(c => c.name == "i" && c.target == "L#0")),
        result.exists(_.steps.last.cursor.exists(c => c.name == "i" && c.target == "L#1"))
      )
    },
    test("draws low / high / mid index cursors, each its own role colour") {
      val s = step(
        1,
        List("nums" -> ref("L"), "lo" -> int(0), "hi" -> int(4), "mid" -> int(2)),
        "L" -> lst(int(1), int(3), int(5), int(7), int(9))
      )
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "array", Some("nums"), "t")
      val cur = result.toOption.toList.flatMap(_.steps.head.cursor)
      assertTrue(
        result.isRight,
        cur.exists(c => c.name == "lo" && c.target == "L#0" && c.color == "#3a5a8c"),
        cur.exists(c => c.name == "hi" && c.target == "L#4" && c.color == "#a13e3e"),
        cur.exists(c => c.name == "mid" && c.target == "L#2" && c.color == "#4f5bd5")
      )
    },
    test("draws no integer index cursors when the root is not an array") {
      // an integer local alongside a tree — an index has no cell to land on
      val s = step(1, List("tree" -> ref("A"), "i" -> int(0)), "A" -> treeNode(1, nul, nul))
      val result =
        adaptSingle(HeapTrace(List(s), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.cursor.exists(_.name == "tree")),
        result.exists(_.steps.head.cursor.forall(_.name != "i"))
      )
    },
    test("segments a multi-case trace into one VizGraph per case, propagating truncated") {
      // tree T1 (A=1, left child B=2), then — past a driver line that holds no tree — a fresh
      // tree T2 (C=7, left child D=8): `tree` rebound to a brand-new structure starts a new case.
      val t1 = Map("A" -> treeNode(1, ref("B"), nul), "B" -> treeNode(2, nul, nul))
      val t2 = Map("C" -> treeNode(7, ref("D"), nul), "D" -> treeNode(8, nul, nul))
      val trace = HeapTrace(
        List(
          mstep(1, List("tree" -> ref("A")), t1),
          mstep(2, List("tree" -> ref("A")), t1),
          mstep(3, List("x" -> int(0)), Map.empty),
          mstep(4, List("tree" -> ref("C")), t2),
          mstep(5, List("tree" -> ref("C")), t2)
        ),
        truncated = true
      )
      val result = HeapToGraph.adapt(trace, src, "binary-tree", Some("tree"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.size == 2),
        result.exists(_.cases.forall(_.truncated)),
        result.exists(_.cases.head.steps.size == 2),
        result.exists(_.cases.head.steps.head.nodes.map(_.label) == List("1", "2")),
        result.exists(_.cases(1).steps.size == 2),
        result.exists(_.cases(1).steps.head.nodes.map(_.label) == List("7", "8")),
        result.exists(_.cases(1).steps.head.removed.isEmpty)
      )
    },
    test("does not mistake recursion into a child for a new case") {
      // one tree A=1 → B=2 → D=4; the root variable walks A → B → D → A the way a recursive
      // `insert(root.left, …)` rebinds its parameter. Each id is inside the case's own structure.
      val tree = Map(
        "A" -> treeNode(1, ref("B"), nul),
        "B" -> treeNode(2, ref("D"), nul),
        "D" -> treeNode(4, nul, nul)
      )
      def at(line: Int, id: String): HeapStep = mstep(line, List("root" -> ref(id)), tree)
      val result = HeapToGraph.adapt(
        HeapTrace(List(at(1, "A"), at(2, "B"), at(3, "D"), at(4, "A")), truncated = false),
        src,
        "binary-tree",
        Some("root"),
        None,
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.cases.size == 1),
        result.exists(_.cases.head.steps.size == 4),
        result.exists(_.cases.head.steps(1).cursor.exists(c => c.name == "root" && c.target == "B"))
      )
    },
    test("viz-case overrides the detected case count") {
      // three single-node trees → three cases auto-detected; viz-case forces the count.
      val trace = HeapTrace(
        List(
          mstep(1, List("tree" -> ref("A")), Map("A" -> treeNode(1, nul, nul))),
          mstep(2, List("tree" -> ref("C")), Map("C" -> treeNode(2, nul, nul))),
          mstep(3, List("tree" -> ref("F")), Map("F" -> treeNode(3, nul, nul)))
        ),
        truncated = false
      )
      def caseCount(vizCase: Option[Int]): Int =
        HeapToGraph
          .adapt(trace, src, "binary-tree", Some("tree"), vizCase, "t")
          .toOption
          .fold(0)(_.cases.size)
      assertTrue(caseCount(None) == 3, caseCount(Some(1)) == 1, caseCount(Some(2)) == 2)
    },
    test("does not segment when no viz-root hint is given") {
      // two fresh trees but no hint — with no variable to track, the whole trace is one case.
      val trace = HeapTrace(
        List(
          mstep(1, List("tree" -> ref("A")), Map("A" -> treeNode(1, nul, nul))),
          mstep(2, List("tree" -> ref("C")), Map("C" -> treeNode(2, nul, nul)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "binary-tree", None, None, "t")
      assertTrue(result.isRight, result.exists(_.cases.size == 1))
    },
    // ── Slice 2: StackFramesPanel ────────────────────────────────────────────────
    test("frames panel — two HeapFrames produce two VizFrames, innermost is active") {
      val multiFrame = HeapStep(
        line = 1,
        event = "line",
        frames = List(
          HeapFrame("solve", List("node" -> ref("A"), "depth" -> int(2))),
          HeapFrame("main", List("tree" -> ref("A")))
        ),
        heap = Map("A" -> treeNode(5, nul, nul))
      )
      val result = adaptSingle(
        HeapTrace(List(multiFrame), truncated = false),
        src,
        "binary-tree",
        Some("node"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.frames.size == 2),
        // innermost frame
        result.exists(_.steps.head.frames.head.fn == "solve"),
        result.exists(_.steps.head.frames.head.isActive),
        result.exists(_.steps.head.frames.head.locals.exists(l =>
          l.name == "depth" && l.typeName == "int" && l.value == "2"
        )),
        result.exists(_.steps.head.frames.head.locals.exists(l =>
          l.name == "node" && l.typeName == "TreeNode"
        )),
        // caller frame
        result.exists(_.steps.head.frames.last.fn == "main"),
        result.exists(!_.steps.head.frames.last.isActive)
      )
    },
    test("frames panel — helper caller frames are excluded from the VizFrame list") {
      // Active frame is "insert" (kept), caller is "__init__" (helper — filtered out).
      val helperCaller = HeapStep(
        line = 1,
        event = "line",
        frames = List(
          HeapFrame("insert", List("root" -> ref("A"), "val" -> int(9))),
          HeapFrame("__init__", List("self" -> ref("A")))
        ),
        heap = Map("A" -> treeNode(5, nul, nul))
      )
      val result = adaptSingle(
        HeapTrace(List(helperCaller), truncated = false),
        src,
        "binary-tree",
        Some("root"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.frames.size == 1),
        result.exists(_.steps.head.frames.head.fn == "insert"),
        result.exists(_.steps.head.frames.head.isActive)
      )
    },
    test("frames panel — changed flag set on a local whose value differs from the previous step") {
      val s1 = step(1, List("depth" -> int(0)), "A" -> treeNode(5, nul, nul))
      val s2 = step(2, List("depth" -> int(1)), "A" -> treeNode(5, nul, nul))
      val result = adaptSingle(
        HeapTrace(List(s1, s2), truncated = false),
        src,
        "binary-tree",
        None,
        "t"
      )
      assertTrue(
        result.isRight,
        // step 0: no previous — changed must be false for all locals
        result.exists(_.steps.head.frames.flatMap(_.locals).forall(!_.changed)),
        // step 1: depth went 0 → 1, changed must be true
        result.exists(_.steps.last.frames.flatMap(_.locals).exists(l => l.name == "depth" && l.changed))
      )
    },
    test("segments a multi-case array trace") {
      val a1 = Map("L1" -> lst(int(1), int(2)))
      val a2 = Map("L2" -> lst(int(9), int(8), int(7)))
      val trace = HeapTrace(
        List(
          mstep(1, List("nums" -> ref("L1")), a1),
          mstep(2, List("nums" -> ref("L1")), a1),
          mstep(3, List("nums" -> ref("L2")), a2),
          mstep(4, List("nums" -> ref("L2")), a2)
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array", Some("nums"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.size == 2),
        result.exists(_.cases.head.steps.head.nodes.size == 2),
        result.exists(_.cases(1).steps.head.nodes.map(_.label) == List("9", "8", "7"))
      )
    },
    // ─── Slice 3 — auto-dispatch (layoutKind + cardId) ──────────────────────────
    test("auto-dispatch — empty layoutHint infers array-1d from a 1D list") {
      val s      = step(1, List("xs" -> ref("L")), "L" -> lst(int(1), int(2), int(3)))
      val result = adaptSingle(HeapTrace(List(s), truncated = false), src, "", Some("xs"), "t")
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.nonEmpty),
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "array-1d")),
        result.exists(_.steps.head.nodes.forall(_.cardId == "L"))
      )
    },
    test("auto-dispatch — nested lists infer array-2d") {
      val heap = Map(
        "M"  -> lst(ref("R1"), ref("R2")),
        "R1" -> lst(int(1), int(2)),
        "R2" -> lst(int(3), int(4))
      )
      val result =
        adaptSingle(
          HeapTrace(List(mstep(1, List("m" -> ref("M")), heap)), truncated = false),
          src,
          "",
          Some("m"),
          "t"
        )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.exists(n => n.cardId == "M" && n.layoutKind == "array-2d"))
      )
    },
    test("auto-dispatch — Instance with left+right infers tree-binary; connected nodes share one cardId") {
      val heap = Map(
        "A" -> treeNode(1, ref("B"), ref("C")),
        "B" -> treeNode(2, nul, nul),
        "C" -> treeNode(3, nul, nul)
      )
      val result =
        adaptSingle(
          HeapTrace(List(mstep(1, List("t" -> ref("A")), heap)), truncated = false),
          src,
          "",
          Some("t"),
          "t"
        )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "tree-binary")),
        // Union-find merges all three tree nodes into one card (representative = lex-smallest = "A").
        result.exists(_.steps.head.nodes.forall(_.cardId == "A"))
      )
    },
    test("auto-dispatch — Instance with `next` only infers list-single") {
      val heap = Map(
        "N1" -> instance("ListNode", "val" -> int(1), "next" -> ref("N2")),
        "N2" -> instance("ListNode", "val" -> int(2), "next" -> nul)
      )
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("head" -> ref("N1")), heap)), truncated = false),
        src,
        "",
        Some("head"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "list-single"))
      )
    },
    test("auto-dispatch — Instance with `next`+`prev` infers list-double") {
      val heap = Map(
        "N1" -> instance("DNode", "val" -> int(1), "next" -> ref("N2"), "prev" -> nul),
        "N2" -> instance("DNode", "val" -> int(2), "next" -> nul, "prev" -> ref("N1"))
      )
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("head" -> ref("N1")), heap)), truncated = false),
        src,
        "",
        Some("head"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "list-double"))
      )
    },
    test("auto-dispatch — Dict infers hashmap") {
      val heap = Map("D" -> dict(str("a") -> int(1), str("b") -> int(2)))
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("d" -> ref("D")), heap)), truncated = false),
        src,
        "",
        Some("d"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "hashmap")),
        result.exists(_.steps.head.nodes.forall(_.cardId == "D"))
      )
    },
    test("auto-dispatch — Instance without recognized field shape falls back to graph-generic") {
      val heap = Map("X" -> instance("Foo", "name" -> str("hi"), "weight" -> int(5)))
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("x" -> ref("X")), heap)), truncated = false),
        src,
        "",
        Some("x"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "graph-generic"))
      )
    },
    test("auto-dispatch — known layoutHint forces every card to that kind (override)") {
      // Heap shape would infer tree-binary, but the override forces "graph-generic".
      val heap = Map(
        "A" -> treeNode(1, ref("B"), ref("C")),
        "B" -> treeNode(2, nul, nul),
        "C" -> treeNode(3, nul, nul)
      )
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("t" -> ref("A")), heap)), truncated = false),
        src,
        "graph-generic",
        Some("t"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.forall(_.layoutKind == "graph-generic"))
      )
    },
    // ─── Slice 6: unchanged flag ─────────────────────────────────────────────────
    test("unchanged — step 0 is always false regardless of content") {
      val result =
        adaptSingle(HeapTrace(List(before), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(result.isRight, result.exists(_.steps.head.unchanged == false))
    },
    test("unchanged — true when only the source line advances (identical heap + locals)") {
      // same tree and same locals as `before` but on line 3 instead of line 2
      val sameAtLine3 = step(
        3,
        List("tree" -> ref("A")),
        "A" -> treeNode(1, ref("B"), ref("C")),
        "B" -> treeNode(2, ref("D"), nul),
        "C" -> treeNode(3, nul, nul),
        "D" -> treeNode(4, nul, nul)
      )
      val result = adaptSingle(
        HeapTrace(List(before, sameAtLine3), truncated = false),
        src,
        "binary-tree",
        Some("tree"),
        "t"
      )
      assertTrue(
        result.isRight,
        result.exists(_.steps.size == 2),
        result.exists(_.steps.last.unchanged == true)
      )
    },
    test("unchanged — false when a new node is added (highlight non-empty)") {
      val result =
        adaptSingle(HeapTrace(List(before, after), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(result.isRight, result.exists(_.steps.last.unchanged == false))
    },
    test("unchanged — false when cursor moves with otherwise identical heap") {
      val s1 = step(
        1,
        List("tree" -> ref("A")),
        "A" -> treeNode(1, ref("B"), nul),
        "B" -> treeNode(2, nul, nul)
      )
      val s2 = step(
        2,
        List("tree" -> ref("A"), "node" -> ref("B")),
        "A" -> treeNode(1, ref("B"), nul),
        "B" -> treeNode(2, nul, nul)
      )
      val result =
        adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "binary-tree", Some("tree"), "t")
      assertTrue(result.isRight, result.exists(_.steps.last.unchanged == false))
    },
    test("unchanged — false when an active-frame local changes its value") {
      val s1     = step(1, List("tree" -> ref("A"), "depth" -> int(0)), "A" -> treeNode(1, nul, nul))
      val s2     = step(2, List("tree" -> ref("A"), "depth" -> int(1)), "A" -> treeNode(1, nul, nul))
      val result = adaptSingle(HeapTrace(List(s1, s2), truncated = false), src, "binary-tree", None, "t")
      assertTrue(result.isRight, result.exists(_.steps.last.unchanged == false))
    },
    test("auto-dispatch — mixed heap (tree + independent dict) emits two distinct cards") {
      val heap = Map(
        "A" -> instance("Root", "val" -> int(1), "left" -> ref("B"), "right" -> nul, "memo" -> ref("D")),
        "B" -> treeNode(2, nul, nul),
        "D" -> dict(str("a") -> int(1))
      )
      val result = adaptSingle(
        HeapTrace(List(mstep(1, List("root" -> ref("A")), heap)), truncated = false),
        src,
        "",
        Some("root"),
        "t"
      )
      // The Root + leaf B are merged via `left`; D is a Dict reached via the `memo` field
      // but isn't merged (Instance→Dict refs don't join). Expect a tree-binary card "A"
      // and a hashmap card "D", with cells/entries inheriting the owner's card.
      assertTrue(
        result.isRight,
        result.exists(_.steps.head.nodes.exists(n => n.cardId == "A" && n.layoutKind == "tree-binary")),
        result.exists(_.steps.head.nodes.exists(n => n.cardId == "D" && n.layoutKind == "hashmap")),
        result.exists(_.steps.head.nodes.map(_.cardId).distinct.size == 2)
      )
    }
  )
