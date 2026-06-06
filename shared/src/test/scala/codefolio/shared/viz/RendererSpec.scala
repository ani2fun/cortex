package codefolio.shared.viz

import codefolio.shared.viz.VizGraph.given
import io.circe.syntax.*
import zio.test.*

/**
 * JVM unit tests for the bespoke-renderer dispatch axis (ADR-0024). Asserts the
 * [[VizGraphStep.structureType]] field exists, defaults to `None`, is correctly populated by
 * [[HeapToGraph.adapt]] from either the explicit `viz-kind=` markdown hint or the segment-wide inference, and
 * round-trips through the circe encoder so the client's RENDERERS dispatch can read it off the JSON.
 *
 * Per-renderer fixtures (slice 1 = stack, slice 2 = hashmap, …) are appended here as each slice ships, giving
 * us a single place to lock in the JSON contract for every bespoke renderer.
 */
object RendererSpec extends ZIOSpecDefault:

  private def int(n: Long): HeapValue    = HeapValue.Scalar(HeapScalar.I(n))
  private def ref(id: String): HeapValue = HeapValue.Ref(id)

  private def lst(items: HeapValue*): HeapObject =
    HeapObject.Arr(ArrKind.Lst, items.toList)

  private def step(
      line: Int,
      fn: String,
      locals: List[(String, HeapValue)],
      heap: (String, HeapObject)*
  ): HeapStep =
    HeapStep(line, "line", List(HeapFrame(fn, locals)), heap.toMap)

  private val src = "line one\nline two\nline three"

  override def spec: Spec[Any, Any] = suite("RendererSpec — bespoke-renderer dispatch axis")(
    test("default VizGraphStep.structureType is None") {
      val s = VizGraphStep(
        nodes = Nil,
        edges = Nil,
        cursor = Nil,
        highlight = Nil,
        changed = Nil,
        removed = Nil,
        annotation = Annotation("line", "", ""),
        line = 1
      )
      assertTrue(s.structureType.isEmpty)
    },
    test("VizGraphStep encodes structureType as a JSON field when set") {
      val s = VizGraphStep(
        nodes = Nil,
        edges = Nil,
        cursor = Nil,
        highlight = Nil,
        changed = Nil,
        removed = Nil,
        annotation = Annotation("line", "", ""),
        line = 1,
        structureType = Some("stack")
      )
      val cur = s.asJson.hcursor.downField("structureType")
      assertTrue(cur.as[String].toOption.contains("stack"))
    },
    test("HeapToGraph.adapt — viz-kind hint overrides inference and stamps every step") {
      val trace = HeapTrace(
        List(
          step(1, "main", List("nums" -> ref("L")), "L"                -> lst(int(1), int(2), int(3))),
          step(2, "main", List("nums" -> ref("L"), "i" -> int(0)), "L" -> lst(int(1), int(2), int(3)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(
        trace,
        src,
        "array-1d",
        Some("nums"),
        None,
        "t",
        vizKind = Some("custom-foo") // not a real renderer; still must pass through
      )
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.contains("custom-foo")))
      )
    },
    test("HeapToGraph.adapt — no inference signal → structureType stays None (generic renderer)") {
      val trace = HeapTrace(
        List(
          step(1, "main", List("nums" -> ref("L")), "L" -> lst(int(7), int(8), int(9)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("nums"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.isEmpty))
      )
    },
    test("HeapToGraph.adapt — Arr with `top` local infers as stack") {
      // No explicit vizKind; the `top` marker local on an Arr-typed root → "stack".
      val trace = HeapTrace(
        List(
          step(1, "main", List("stk" -> ref("S"), "top" -> int(2)), "S" -> lst(int(5), int(7), int(9)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("stk"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.contains("stack")))
      )
    },
    test("HeapToGraph.adapt — Arr with `push`/`pop` function frames infers as stack") {
      val trace = HeapTrace(
        List(
          step(1, "main", List("stk" -> ref("S")), "S"                -> lst(int(5))),
          step(2, "push", List("stk" -> ref("S"), "x" -> int(9)), "S" -> lst(int(5), int(9))),
          step(3, "pop", List("stk" -> ref("S")), "S"                 -> lst(int(5)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("stk"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.contains("stack")))
      )
    },
    test("HeapToGraph.adapt — Arr with `front` only infers as queue") {
      val trace = HeapTrace(
        List(
          step(1, "main", List("q" -> ref("Q"), "front" -> int(0)), "Q" -> lst(int(1), int(2)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("q"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.contains("queue")))
      )
    },
    test("HeapToGraph.adapt — Arr with `front` + `back` infers as deque") {
      val trace = HeapTrace(
        List(
          step(
            1,
            "main",
            List("d" -> ref("D"), "front" -> int(0), "back" -> int(1)),
            "D" -> lst(int(1), int(2))
          )
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("d"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.contains("deque")))
      )
    },
    test("HeapToGraph.adapt — ② keeps a leading empty step so the structure grows from blank") {
      // stack = [] (empty Arr → 0 nodes), then it fills. dropEmptyEnds must keep that empty
      // declaration step so the animation opens BLANK instead of already-populated on the first append.
      val trace = HeapTrace(
        List(
          step(1, "main", List("stk" -> ref("S")), "S" -> lst()),
          step(2, "main", List("stk" -> ref("S")), "S" -> lst(int(3))),
          step(3, "main", List("stk" -> ref("S")), "S" -> lst(int(3), int(7)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("stk"), None, "t")
      assertTrue(
        result.isRight,
        result.exists { g =>
          val steps = g.cases.head.steps
          steps.headOption.exists(_.nodes.isEmpty) && steps.exists(_.nodes.nonEmpty)
        }
      )
    },
    test("HeapToGraph.adapt — ① a value change captions as a readable phrase, not a bare arrow") {
      // A cell value 2 → 9 across steps; narrate captions it "2 changed to 9" — no bare " → " titles.
      val trace = HeapTrace(
        List(
          step(1, "main", List("a" -> ref("A")), "A" -> lst(int(2))),
          step(2, "main", List("a" -> ref("A")), "A" -> lst(int(9)))
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "array-1d", Some("a"), None, "t")
      val titles = result.toOption.toList.flatMap(_.cases.head.steps.map(_.annotation.title))
      assertTrue(
        result.isRight,
        titles.exists(_.contains("changed to")),
        titles.forall(t => !t.contains(" → "))
      )
    },
    test(
      "Stack renderer fixture — push/pop trace produces structureType=stack and the top cell is the largest slot"
    ) {
      // Slice 1 contract — locks in what the bespoke stack renderer reads:
      //   1. structureType=stack stamped on every step (so RENDERERS["stack"] dispatch fires)
      //   2. every cell node carries a slot; the cell with the highest slot is the visual top.
      // The renderer sorts by slot descending to put the top cell first in the DOM.
      val trace = HeapTrace(
        List(
          step(1, "main", List("stk" -> ref("S")), "S"                -> lst(int(1))),
          step(2, "push", List("stk" -> ref("S"), "x" -> int(2)), "S" -> lst(int(1), int(2))),
          step(3, "pop", List("stk" -> ref("S")), "S"                 -> lst(int(1)))
        ),
        truncated = false
      )
      val r                = HeapToGraph.adapt(trace, src, "array-1d", Some("stk"), None, "t")
      val structureTypesOk = r.exists(_.cases.head.steps.forall(_.structureType.contains("stack")))

      // Last step has one cell (after pop). Step 2 (after push) has two cells with slots {0, 1}.
      val pushedStep    = r.toOption.map(_.cases.head.steps(1))
      val cellsOnPush   = pushedStep.map(_.nodes.filter(_.slot.isDefined)).getOrElse(Nil)
      val slotsOnPush   = cellsOnPush.flatMap(_.slot).toSet
      val topSlotOnPush = cellsOnPush.flatMap(_.slot).maxOption
      assertTrue(
        structureTypesOk,
        slotsOnPush == Set(0, 1),
        topSlotOnPush.contains(1) // the most-recently-pushed cell is at slot 1 — the top
      )
    },
    test(
      "HeapToGraph.adapt — root that's an Instance (not an Arr) doesn't infer queue/stack from name overlap"
    ) {
      // A binary tree whose nodes happen to have a `left`/`right` field; `front` is not in scope here, so
      // structureType stays None. The renderer falls back to the generic graph renderer.
      val trace = HeapTrace(
        List(
          step(
            1,
            "main",
            List("root" -> ref("R")),
            "R" -> HeapObject.Instance(
              "TreeNode",
              List(
                "val"   -> int(1),
                "left"  -> HeapValue.Scalar(HeapScalar.Null),
                "right" -> HeapValue.Scalar(HeapScalar.Null)
              )
            )
          )
        ),
        truncated = false
      )
      val result = HeapToGraph.adapt(trace, src, "tree-binary", Some("root"), None, "t")
      assertTrue(
        result.isRight,
        result.exists(_.cases.head.steps.forall(_.structureType.isEmpty))
      )
    }
  )
