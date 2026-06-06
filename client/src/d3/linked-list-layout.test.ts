// Unit tests for the linked-list layout (linked-list-layout.ts). The layout is
// a pure function — positions are arithmetic over the `next`-edge chain, no DOM,
// no RNG — so it runs in plain Node under vitest. The contract under test:
// `next` edges order the nodes into a left-to-right row, `prev` edges move
// nothing (a doubly-linked list lays out like a singly-linked one), two lists
// stack as two rows, a cyclic `next`-graph degrades to the generic graph
// layout, and the result is deterministic.

import { describe, it, expect } from "vitest";
import type { VizNode, VizEdge } from "./types";
import type { GraphLayout } from "./tree-layout";
import { linkedListLayout } from "./linked-list-layout";
import { graphLayout } from "./graph-layout";

// A ListNode instance — HeapToGraph emits one VizNode per ListNode.
function listNode(id: string, val: number): VizNode {
  return { id, label: String(val), kind: "ListNode", meta: [], slot: null };
}

function nextEdge(from: string, to: string): VizEdge {
  return { from, to, label: "next" };
}

function prevEdge(from: string, to: string): VizEdge {
  return { from, to, label: "prev" };
}

// A singly-linked chain n0 -> n1 -> ... of `n` nodes, with its `next` edges.
function chain(n: number): { nodes: VizNode[]; edges: VizEdge[] } {
  const nodes = Array.from({ length: n }, (_, i) => listNode(`n${i}`, i * 10));
  const edges: VizEdge[] = [];
  for (let i = 1; i < n; i += 1) edges.push(nextEdge(`n${i - 1}`, `n${i}`));
  return { nodes, edges };
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

// A comparable snapshot of a layout — for asserting determinism / fallback.
function snapshot(layout: GraphLayout): unknown {
  return {
    width: layout.width,
    height: layout.height,
    positions: [...layout.positions.entries()].sort((a, b) => a[0].localeCompare(b[0])),
  };
}

describe("linkedListLayout — degenerate inputs", () => {
  it("an empty graph yields an empty, non-zero canvas", () => {
    const layout = linkedListLayout([], []);
    expect(layout.positions.size).toBe(0);
    expect(layout.width).toBeGreaterThan(0);
    expect(layout.height).toBeGreaterThan(0);
  });

  it("a single node sits inside a sane canvas", () => {
    const layout = linkedListLayout([listNode("n0", 7)], []);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["n0"]);
    // Big enough to actually contain a node circle (radius 22 in the renderer).
    expect(layout.width).toBeGreaterThanOrEqual(44);
    expect(layout.height).toBeGreaterThanOrEqual(44);
  });

  it("ignores a self-loop (a degenerate one-node cycle)", () => {
    const layout = linkedListLayout([listNode("n0", 1)], [nextEdge("n0", "n0")]);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["n0"]);
  });

  it("ignores an edge to a node that is not present", () => {
    const layout = linkedListLayout([listNode("n0", 1)], [nextEdge("n0", "ghost")]);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["n0"]);
  });
});

describe("linkedListLayout — a horizontal chain", () => {
  it("places every node of a chain on a single row", () => {
    const { nodes, edges } = chain(4);
    const layout = linkedListLayout(nodes, edges);
    const ys = nodes.map((n) => layout.positions.get(n.id)!.y);
    expect(new Set(ys).size).toBe(1); // one shared y — a flat row
    expectWithinCanvas(
      layout,
      nodes.map((n) => n.id),
    );
  });

  it("orders nodes left-to-right following `next`, evenly spaced", () => {
    const { nodes, edges } = chain(5);
    const layout = linkedListLayout(nodes, edges);
    const xs = nodes.map((n) => layout.positions.get(n.id)!.x);
    for (let i = 1; i < xs.length; i += 1) expect(xs[i]).toBeGreaterThan(xs[i - 1]);
    const gaps = xs.slice(1).map((x, i) => x - xs[i]);
    for (const g of gaps) expect(g).toBeCloseTo(gaps[0]); // uniform column spacing
  });

  it("places a node by its `next`-order, not its position in the input array", () => {
    // Nodes handed in shuffled order; the `next` edges still chain n0..n3.
    const nodes = [listNode("n2", 20), listNode("n0", 0), listNode("n3", 30), listNode("n1", 10)];
    const edges = [nextEdge("n0", "n1"), nextEdge("n1", "n2"), nextEdge("n2", "n3")];
    const layout = linkedListLayout(nodes, edges);
    const x = (id: string) => layout.positions.get(id)!.x;
    expect(x("n0")).toBeLessThan(x("n1"));
    expect(x("n1")).toBeLessThan(x("n2"));
    expect(x("n2")).toBeLessThan(x("n3"));
  });

  it("a longer chain yields a wider canvas", () => {
    const small = chain(3);
    const big = chain(10);
    expect(linkedListLayout(big.nodes, big.edges).width).toBeGreaterThan(
      linkedListLayout(small.nodes, small.edges).width,
    );
  });

  it("lays out a chain whose edges carry no label", () => {
    // No `next`/`prev` label — the layout treats every non-`prev` edge as a
    // chain edge, so an oddly-named singly-linked list still rows out.
    const nodes = [listNode("n0", 1), listNode("n1", 2), listNode("n2", 3)];
    const edges: VizEdge[] = [
      { from: "n0", to: "n1", label: "" },
      { from: "n1", to: "n2", label: "" },
    ];
    const layout = linkedListLayout(nodes, edges);
    const ys = nodes.map((n) => layout.positions.get(n.id)!.y);
    expect(new Set(ys).size).toBe(1);
    expect(layout.positions.get("n0")!.x).toBeLessThan(layout.positions.get("n2")!.x);
  });

  it("ignores a non-chain pointer (a `random` edge) when ordering the chain", () => {
    // "Copy list with random pointer": a `next` chain plus stray `random` edges.
    // The `random` edge must neither reorder the row nor trip the cycle fallback.
    const { nodes, edges } = chain(3);
    const layout = linkedListLayout(nodes, [...edges, { from: "n0", to: "n2", label: "random" }]);
    const ys = nodes.map((n) => layout.positions.get(n.id)!.y);
    expect(new Set(ys).size).toBe(1);
    expect(layout.positions.get("n0")!.x).toBeLessThan(layout.positions.get("n1")!.x);
    expect(layout.positions.get("n1")!.x).toBeLessThan(layout.positions.get("n2")!.x);
  });
});

describe("linkedListLayout — doubly-linked lists", () => {
  it("a doubly-linked list lays out exactly like its singly-linked twin", () => {
    // `prev` edges must not move anything — the `next` chain fixes the order.
    const { nodes, edges } = chain(4);
    const doubly = [...edges];
    for (let i = 1; i < nodes.length; i += 1) doubly.push(prevEdge(`n${i}`, `n${i - 1}`));
    expect(snapshot(linkedListLayout(nodes, doubly))).toEqual(
      snapshot(linkedListLayout(nodes, edges)),
    );
  });

  it("places a doubly-linked list on a single row", () => {
    const { nodes, edges } = chain(3);
    const doubly = [...edges, prevEdge("n1", "n0"), prevEdge("n2", "n1")];
    const layout = linkedListLayout(nodes, doubly);
    const ys = nodes.map((n) => layout.positions.get(n.id)!.y);
    expect(new Set(ys).size).toBe(1);
    expectWithinCanvas(
      layout,
      nodes.map((n) => n.id),
    );
  });
});

describe("linkedListLayout — multiple chains", () => {
  it("stacks two separate lists as two non-overlapping rows", () => {
    const a = [listNode("a0", 1), listNode("a1", 2)];
    const b = [listNode("b0", 3), listNode("b1", 4)];
    const edges = [nextEdge("a0", "a1"), nextEdge("b0", "b1")];
    const layout = linkedListLayout([...a, ...b], edges);
    expect(layout.positions.get("a0")!.y).not.toBe(layout.positions.get("b0")!.y);
    expectWithinCanvas(layout, ["a0", "a1", "b0", "b1"]);
  });

  it("each chain keeps its own head-leftmost ordering", () => {
    const a = [listNode("a0", 1), listNode("a1", 2), listNode("a2", 3)];
    const b = [listNode("b0", 4), listNode("b1", 5), listNode("b2", 6)];
    const edges = [
      nextEdge("a0", "a1"),
      nextEdge("a1", "a2"),
      nextEdge("b0", "b1"),
      nextEdge("b1", "b2"),
    ];
    const layout = linkedListLayout([...a, ...b], edges);
    expect(layout.positions.get("a0")!.x).toBeLessThan(layout.positions.get("a2")!.x);
    expect(layout.positions.get("b0")!.x).toBeLessThan(layout.positions.get("b2")!.x);
  });
});

describe("linkedListLayout — reverse-in-place pedagogy (reciprocal edges)", () => {
  it("treats `a→b` and `b→a` (both labelled `next`) as one chain edge, keeping the row layout", () => {
    // The union of every step in a reverse-in-place animation contains BOTH
    // directions of each adjacent pair (step 1: n0→n1, last step: n1→n0).
    // Before the dedupe, this trips the linear-forest check and the layout
    // falls back to the graph layout. The fix keeps the first-seen direction
    // (graph-render.ts feeds union edges in step order, so step 1 wins) and
    // re-establishes the horizontal chain.
    const nodes = [listNode("n0", 1), listNode("n1", 2), listNode("n2", 3), listNode("n3", 4)];
    const edges = [
      // First seen — defines the chain direction.
      nextEdge("n0", "n1"),
      nextEdge("n1", "n2"),
      nextEdge("n2", "n3"),
      // Reverse of the same edges — produced by later animation steps. Each
      // duplicates an unordered pair already in the set, so they're dropped
      // for layout but still rendered per-step over the fixed positions.
      nextEdge("n1", "n0"),
      nextEdge("n2", "n1"),
      nextEdge("n3", "n2"),
    ];
    const layout = linkedListLayout(nodes, edges);
    const ys = nodes.map((n) => layout.positions.get(n.id)!.y);
    expect(new Set(ys).size).toBe(1); // single row
    const x = (id: string) => layout.positions.get(id)!.x;
    expect(x("n0")).toBeLessThan(x("n1"));
    expect(x("n1")).toBeLessThan(x("n2"));
    expect(x("n2")).toBeLessThan(x("n3"));
  });

  it("does NOT fall back to graphLayout when the union has reciprocal next edges", () => {
    // Concrete regression check for the dispatch path — previously the union
    // forced asLinearForest to return null (outDeg > 1) and the layout matched
    // graphLayout. After the dedupe it should match a clean chain instead.
    const nodes = [listNode("n0", 1), listNode("n1", 2)];
    const reciprocal = [nextEdge("n0", "n1"), nextEdge("n1", "n0")];
    expect(snapshot(linkedListLayout(nodes, reciprocal))).not.toEqual(
      snapshot(graphLayout(nodes, reciprocal)),
    );
  });
});

describe("linkedListLayout — cyclic next-graphs fall back to the graph layout", () => {
  it("delegates a full cycle (the list loops onto itself) to graphLayout", () => {
    const nodes = [listNode("n0", 1), listNode("n1", 2), listNode("n2", 3)];
    const edges = [nextEdge("n0", "n1"), nextEdge("n1", "n2"), nextEdge("n2", "n0")];
    expect(snapshot(linkedListLayout(nodes, edges))).toEqual(snapshot(graphLayout(nodes, edges)));
    expectWithinCanvas(linkedListLayout(nodes, edges), ["n0", "n1", "n2"]);
  });

  it("delegates a rho-shaped list (a tail entering a loop) to graphLayout", () => {
    // n0 -> n1 -> n2 -> n3 -> n1 : n1 has next-in-degree 2 — not a linear forest.
    const nodes = [listNode("n0", 1), listNode("n1", 2), listNode("n2", 3), listNode("n3", 4)];
    const edges = [
      nextEdge("n0", "n1"),
      nextEdge("n1", "n2"),
      nextEdge("n2", "n3"),
      nextEdge("n3", "n1"),
    ];
    expect(snapshot(linkedListLayout(nodes, edges))).toEqual(snapshot(graphLayout(nodes, edges)));
    expectWithinCanvas(linkedListLayout(nodes, edges), ["n0", "n1", "n2", "n3"]);
  });
});

describe("linkedListLayout — determinism", () => {
  it("lays the same list out identically across calls", () => {
    const { nodes, edges } = chain(6);
    expect(snapshot(linkedListLayout(nodes, edges))).toEqual(
      snapshot(linkedListLayout(nodes, edges)),
    );
  });

  it("does not mutate its input arrays or their elements", () => {
    const { nodes, edges } = chain(3);
    linkedListLayout(nodes, edges);
    expect(nodes).toHaveLength(3);
    expect(edges).toHaveLength(2);
    expect(nodes[0]).toEqual({ id: "n0", label: "0", kind: "ListNode", meta: [], slot: null });
    expect(edges[0]).toEqual({ from: "n0", to: "n1", label: "next" });
  });
});
