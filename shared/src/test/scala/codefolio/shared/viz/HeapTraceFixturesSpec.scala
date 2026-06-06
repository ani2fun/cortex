package codefolio.shared.viz

import zio.test.*

/**
 * Phase 0 — drive `HeapToGraph.adapt` against the three hardest-shape fixtures (ADR-0025) and assert each one
 * produces a non-empty `VizCases` with the expected layout characteristics.
 *
 * The Scala-side assertions prove the *adapter* handles the format. The companion Playwright tests
 * (`client/test/e2e/trace-shapes.spec.ts`) prove the *renderer* draws them. ADR-0025 cites the combination as
 * the evidence that the existing `HeapTrace` primitive set spans all 16 planned data-structure shapes.
 */
object HeapTraceFixturesSpec extends ZIOSpecDefault:

  private def adapt(f: HeapTraceFixtures.Fixture): Either[String, VizCases] =
    HeapToGraph.adapt(
      trace = f.trace,
      source = f.source,
      layoutHint = f.layoutHint,
      rootHint = f.rootHint,
      vizCase = None,
      title = f.title,
      vizKind = f.vizKind
    )

  /** Pull the single VizGraph out of a Right(VizCases) — the fixtures are all single-case. */
  private def soleCase(r: Either[String, VizCases]): VizGraph =
    r.toOption.flatMap(_.cases.headOption).getOrElse(
      throw new AssertionError(s"expected Right(VizCases) with one case, got: $r")
    )

  override def spec: Spec[Any, Any] = suite("HeapTraceFixtures")(
    test("avlRotationTrace adapts cleanly — tree-binary layout, root rebinds across steps") {
      val out = adapt(HeapTraceFixtures.avlRotationTrace)
      val g   = soleCase(out)
      // Every step keeps the 3 AVL nodes around (no creation/destruction during rotation).
      val nodeCount = g.steps.map(_.nodes.count(_.kind == "AvlNode"))
      // Adapter writes the layout hint through as-is and infers per-card layoutKind.
      val anyTreeBinary = g.steps.exists(_.nodes.exists(_.layoutKind == "tree-binary"))
      // height + balance must surface as meta sub-labels on at least one node.
      val anyHeightMeta =
        g.steps.exists(_.nodes.exists(_.meta.exists(_.name == "height")))
      val anyBalanceMeta =
        g.steps.exists(_.nodes.exists(_.meta.exists(_.name == "balance")))
      assertTrue(
        out.isRight,
        g.layoutHint == "tree-binary",
        g.steps.nonEmpty,
        nodeCount.forall(_ == 3),
        anyTreeBinary,
        anyHeightMeta,
        anyBalanceMeta
      )
    },
    test("graphBfsTrace adapts cleanly — graph layout with per-card adjacency arrays") {
      val out = adapt(HeapTraceFixtures.graphBfsTrace)
      val g   = soleCase(out)
      // GraphNode card → graph-generic (no left/right/next/prev fields → fallback).
      val anyGraphGeneric = g.steps.exists(_.nodes.exists(_.layoutKind == "graph-generic"))
      // Each adjacency Arr is its own card → array-1d (items are Refs to Instances, not Refs to Arrs).
      val anyArrayCard = g.steps.exists(_.nodes.exists(_.layoutKind == "array-1d"))
      // All four GraphNode instances reachable across the trace.
      val maxGraphNodes = g.steps.map(_.nodes.count(_.kind == "GraphNode")).max
      // visited/queue are NOT reachable from `root` (which points at a GraphNode); they live in
      // locals and intentionally don't surface as cards — see HeapTraceFixtures docstring. The
      // BFS *traversal* is what the trace visualises; visited/queue are bookkeeping.
      assertTrue(
        out.isRight,
        g.layoutHint == "",
        g.steps.nonEmpty,
        anyGraphGeneric,
        anyArrayCard,
        maxGraphNodes == 4
      )
    },
    test("hashmapChainedCollisionsTrace adapts cleanly — hashmap card with bucket chains") {
      val out = adapt(HeapTraceFixtures.hashmapChainedCollisionsTrace)
      val g   = soleCase(out)
      // Root Dict surfaces as a hashmap card via per-card inference.
      val anyHashmapCard = g.steps.exists(_.nodes.exists(_.layoutKind == "hashmap"))
      // Each bucket Arr surfaces as a 1-d array card.
      val anyArrayCard = g.steps.exists(_.nodes.exists(_.layoutKind == "array-1d"))
      // Entry nodes form linked-list cards via the `next` field.
      val anyListSingleCard = g.steps.exists(_.nodes.exists(_.layoutKind == "list-single"))
      // Final step shows the collision — bucket_0 now points at TWO Refs in chain order. The bucket
      // Arr's two cells surface as edges from `bucket_0#0` and `bucket_0#1` to their target Entries.
      val finalStep    = g.steps.last
      val bucket0Edges = finalStep.edges.count(_.from.startsWith("bucket_0#"))
      assertTrue(
        out.isRight,
        g.layoutHint == "",
        g.steps.nonEmpty,
        anyHashmapCard,
        anyArrayCard,
        anyListSingleCard,
        bucket0Edges >= 2
      )
    },
    test("every fixture in `all` round-trips through HeapToGraph.adapt") {
      val results  = HeapTraceFixtures.all.map(f => f.name -> adapt(f))
      val failures = results.collect { case (n, Left(msg)) => s"$n: $msg" }
      assertTrue(failures.isEmpty, results.size == 16)
    }
  )
