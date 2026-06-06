// Unit tests for the generic graph layout (graph-layout.ts). The layout is a
// pure function — d3-force / d3-hierarchy maths, no DOM — so it runs in plain
// Node under vitest. The contract under test: every node gets a finite
// position inside the reported canvas, the topology reads correctly, and the
// whole thing is deterministic (re-opening the modal must redraw identically).

import { describe, it, expect } from "vitest";
import type { VizNode, VizEdge } from "./types";
import type { GraphLayout } from "./tree-layout";
import { graphLayout } from "./graph-layout";

function node(id: string): VizNode {
  return { id, label: id, kind: "instance", meta: [], slot: null };
}

function edge(from: string, to: string, label = "next"): VizEdge {
  return { from, to, label };
}

function nodeList(...ids: string[]): VizNode[] {
  return ids.map(node);
}

// Every listed node is a finite point strictly inside the reported canvas.
function expectWithinCanvas(layout: GraphLayout, ids: string[]): void {
  for (const id of ids) {
    const p = layout.positions.get(id);
    expect(p, `position for "${id}"`).toBeDefined();
    expect(Number.isFinite(p!.x), `x for "${id}"`).toBe(true);
    expect(Number.isFinite(p!.y), `y for "${id}"`).toBe(true);
    expect(p!.x).toBeGreaterThan(0);
    expect(p!.x).toBeLessThan(layout.width);
    expect(p!.y).toBeGreaterThan(0);
    expect(p!.y).toBeLessThan(layout.height);
  }
}

function minPairDistance(layout: GraphLayout, ids: string[]): number {
  let min = Infinity;
  for (let i = 0; i < ids.length; i += 1) {
    for (let j = i + 1; j < ids.length; j += 1) {
      const p = layout.positions.get(ids[i])!;
      const q = layout.positions.get(ids[j])!;
      min = Math.min(min, Math.hypot(p.x - q.x, p.y - q.y));
    }
  }
  return min;
}

// A comparable snapshot of a layout — for asserting determinism.
function snapshot(layout: GraphLayout): unknown {
  return {
    width: layout.width,
    height: layout.height,
    positions: [...layout.positions.entries()].sort((a, b) => a[0].localeCompare(b[0])),
  };
}

describe("graphLayout — degenerate inputs", () => {
  it("an empty graph yields an empty, non-zero canvas", () => {
    const layout = graphLayout([], []);
    expect(layout.positions.size).toBe(0);
    expect(layout.width).toBeGreaterThan(0);
    expect(layout.height).toBeGreaterThan(0);
  });

  it("a single node sits inside a sane canvas", () => {
    const layout = graphLayout(nodeList("a"), []);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["a"]);
    // Big enough to actually contain a node circle (radius 22 in the renderer).
    expect(layout.width).toBeGreaterThanOrEqual(44);
    expect(layout.height).toBeGreaterThanOrEqual(44);
  });

  it("ignores a self-loop", () => {
    const layout = graphLayout(nodeList("a"), [edge("a", "a")]);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["a"]);
  });

  it("ignores an edge to a node that is not present", () => {
    const layout = graphLayout(nodeList("a"), [edge("a", "ghost")]);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["a"]);
  });
});

describe("graphLayout — tree-shaped graphs (d3-hierarchy path)", () => {
  it("places a root above its children, with the children on one level", () => {
    const layout = graphLayout(nodeList("r", "a", "b"), [
      edge("r", "a", "left"),
      edge("r", "b", "right"),
    ]);
    const r = layout.positions.get("r")!;
    const a = layout.positions.get("a")!;
    const b = layout.positions.get("b")!;
    expect(r.y).toBeLessThan(a.y); // root above
    expect(a.y).toBe(b.y); // siblings share a level
    expect(a.x).not.toBe(b.x); // siblings separated
    // A tidy tree centres the parent over its children.
    expect(r.x).toBeGreaterThanOrEqual(Math.min(a.x, b.x));
    expect(r.x).toBeLessThanOrEqual(Math.max(a.x, b.x));
    expectWithinCanvas(layout, ["r", "a", "b"]);
  });

  it("lays a chain out at uniformly spaced, increasing depths", () => {
    const layout = graphLayout(nodeList("a", "b", "c"), [edge("a", "b"), edge("b", "c")]);
    const a = layout.positions.get("a")!;
    const b = layout.positions.get("b")!;
    const c = layout.positions.get("c")!;
    expect(a.y).toBeLessThan(b.y);
    expect(b.y).toBeLessThan(c.y);
    expect(b.y - a.y).toBe(c.y - b.y); // uniform depth spacing
    expectWithinCanvas(layout, ["a", "b", "c"]);
  });

  it("lays isolated nodes out as a single row", () => {
    const layout = graphLayout(nodeList("a", "b", "c"), []);
    const ys = ["a", "b", "c"].map((id) => layout.positions.get(id)!.y);
    expect(ys[0]).toBe(ys[1]);
    expect(ys[1]).toBe(ys[2]);
    const xs = ["a", "b", "c"].map((id) => layout.positions.get(id)!.x).sort((p, q) => p - q);
    expect(xs[0]).toBeLessThan(xs[1]);
    expect(xs[1]).toBeLessThan(xs[2]);
    expectWithinCanvas(layout, ["a", "b", "c"]);
  });

  it("places a disconnected forest without overlapping its components", () => {
    const layout = graphLayout(nodeList("r1", "a1", "r2", "a2"), [
      edge("r1", "a1"),
      edge("r2", "a2"),
    ]);
    const ids = ["r1", "a1", "r2", "a2"];
    expectWithinCanvas(layout, ids);
    expect(minPairDistance(layout, ids)).toBeGreaterThan(30);
  });
});

describe("graphLayout — cyclic / merging graphs (d3-force path)", () => {
  it("places every node of a 3-cycle inside the canvas, none coincident", () => {
    const ids = ["a", "b", "c"];
    const layout = graphLayout(nodeList(...ids), [
      edge("a", "b"),
      edge("b", "c"),
      edge("c", "a"),
    ]);
    expect(layout.positions.size).toBe(3);
    expectWithinCanvas(layout, ids);
    expect(minPairDistance(layout, ids)).toBeGreaterThan(10);
  });

  it("places every node of a diamond DAG (a merge point)", () => {
    const ids = ["a", "b", "c", "d"];
    const layout = graphLayout(nodeList(...ids), [
      edge("a", "b"),
      edge("a", "c"),
      edge("b", "d"),
      edge("c", "d"),
    ]);
    expect(layout.positions.size).toBe(4);
    expectWithinCanvas(layout, ids);
    expect(minPairDistance(layout, ids)).toBeGreaterThan(10);
  });
});

describe("graphLayout — determinism", () => {
  it("a tree-shaped graph lays out identically across calls", () => {
    const ns = nodeList("r", "a", "b", "c");
    const es = [edge("r", "a"), edge("r", "b"), edge("b", "c")];
    expect(snapshot(graphLayout(ns, es))).toEqual(snapshot(graphLayout(ns, es)));
  });

  it("a cyclic graph (force path) lays out identically across calls", () => {
    const ns = nodeList("a", "b", "c", "d");
    const es = [edge("a", "b"), edge("b", "c"), edge("c", "d"), edge("d", "a")];
    expect(snapshot(graphLayout(ns, es))).toEqual(snapshot(graphLayout(ns, es)));
  });

  it("does not mutate its input arrays or their elements", () => {
    const ns = nodeList("a", "b");
    const es = [edge("a", "b"), edge("b", "a")]; // a cycle — exercises the force path
    graphLayout(ns, es);
    expect(ns).toHaveLength(2);
    expect(es).toHaveLength(2);
    expect(ns[0]).toEqual({ id: "a", label: "a", kind: "instance", meta: [], slot: null });
    expect(es[0]).toEqual({ from: "a", to: "b", label: "next" });
    expect(es[1]).toEqual({ from: "b", to: "a", label: "next" });
  });
});
