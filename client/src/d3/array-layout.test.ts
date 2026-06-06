// Unit tests for the array layout (array-layout.ts). The layout is a pure
// function — positions are arithmetic over `VizNode.slot`, no DOM, no RNG — so
// it runs in plain Node under vitest. The contract under test: cells form a row
// ordered by slot (not input order), every node lands inside the reported
// canvas, two arrays stack without overlapping, an object-array's ref targets
// sit below their cells, and the result is deterministic.

import { describe, it, expect } from "vitest";
import type { VizNode, VizEdge } from "./types";
import type { GraphLayout } from "./tree-layout";
import { arrayLayout } from "./array-layout";

// An array cell — id `${arrId}#${slot}`, the form HeapToGraph synthesises.
function cell(arrId: string, slot: number): VizNode {
  return { id: `${arrId}#${slot}`, label: String(slot), kind: "cell", meta: [], slot };
}

// A non-cell node — an instance, or a cell's ref target.
function obj(id: string): VizNode {
  return { id, label: id, kind: "instance", meta: [], slot: null };
}

function edge(from: string, to: string, label = ""): VizEdge {
  return { from, to, label };
}

// A whole array `arrId` of `n` cells, slots 0..n-1 in order.
function arrayCells(arrId: string, n: number): VizNode[] {
  return Array.from({ length: n }, (_, i) => cell(arrId, i));
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

// A comparable snapshot of a layout — for asserting determinism.
function snapshot(layout: GraphLayout): unknown {
  return {
    width: layout.width,
    height: layout.height,
    positions: [...layout.positions.entries()].sort((a, b) => a[0].localeCompare(b[0])),
  };
}

describe("arrayLayout — degenerate inputs", () => {
  it("an empty graph yields an empty, non-zero canvas", () => {
    const layout = arrayLayout([], []);
    expect(layout.positions.size).toBe(0);
    expect(layout.width).toBeGreaterThan(0);
    expect(layout.height).toBeGreaterThan(0);
  });

  it("a single cell sits inside a sane canvas", () => {
    const layout = arrayLayout([cell("L", 0)], []);
    expect(layout.positions.size).toBe(1);
    expectWithinCanvas(layout, ["L#0"]);
    // Big enough to actually contain a node circle (radius 22 in the renderer).
    expect(layout.width).toBeGreaterThanOrEqual(44);
    expect(layout.height).toBeGreaterThanOrEqual(44);
  });

  it("hands a payload with no cells to the generic graph layout", () => {
    // Two instances + an edge — not array-shaped; both must still be placed.
    const layout = arrayLayout([obj("a"), obj("b")], [edge("a", "b")]);
    expect(layout.positions.size).toBe(2);
    expectWithinCanvas(layout, ["a", "b"]);
  });
});

describe("arrayLayout — a row of cells", () => {
  it("places every cell of one array on a single row", () => {
    const cells = arrayCells("L", 4);
    const layout = arrayLayout(cells, []);
    const ys = cells.map((c) => layout.positions.get(c.id)!.y);
    expect(new Set(ys).size).toBe(1); // one shared y
    expectWithinCanvas(
      layout,
      cells.map((c) => c.id),
    );
  });

  it("orders cells left-to-right by slot, evenly spaced", () => {
    const cells = arrayCells("L", 5);
    const layout = arrayLayout(cells, []);
    const xs = cells.map((c) => layout.positions.get(c.id)!.x);
    for (let i = 1; i < xs.length; i += 1) expect(xs[i]).toBeGreaterThan(xs[i - 1]);
    const gaps = xs.slice(1).map((x, i) => x - xs[i]);
    for (const g of gaps) expect(g).toBeCloseTo(gaps[0]); // uniform column spacing
  });

  it("places a cell by its slot, not its position in the input array", () => {
    // Cells handed in shuffled order — x must still follow slot.
    const shuffled = [cell("L", 2), cell("L", 0), cell("L", 3), cell("L", 1)];
    const layout = arrayLayout(shuffled, []);
    const x = (s: number) => layout.positions.get(`L#${s}`)!.x;
    expect(x(0)).toBeLessThan(x(1));
    expect(x(1)).toBeLessThan(x(2));
    expect(x(2)).toBeLessThan(x(3));
  });

  it("a longer array yields a wider canvas", () => {
    const small = arrayLayout(arrayCells("L", 3), []);
    const big = arrayLayout(arrayCells("L", 12), []);
    expect(big.width).toBeGreaterThan(small.width);
  });
});

describe("arrayLayout — multiple arrays", () => {
  it("stacks two arrays as two non-overlapping rows", () => {
    const a = arrayCells("L", 4);
    const b = arrayCells("M", 4);
    const layout = arrayLayout([...a, ...b], []);
    expect(layout.positions.get("L#0")!.y).not.toBe(layout.positions.get("M#0")!.y);
    expectWithinCanvas(layout, [...a, ...b].map((c) => c.id));
  });

  it("each array keeps its own slot-0-leftmost ordering", () => {
    const layout = arrayLayout([...arrayCells("L", 3), ...arrayCells("M", 3)], []);
    expect(layout.positions.get("L#0")!.x).toBeLessThan(layout.positions.get("L#2")!.x);
    expect(layout.positions.get("M#0")!.x).toBeLessThan(layout.positions.get("M#2")!.x);
  });
});

describe("arrayLayout — cells with ref targets", () => {
  it("places a cell's ref target below the row, x-aligned under the cell", () => {
    // An array of two objects: L#0 -> N1, L#1 -> N2.
    const nodes = [cell("L", 0), cell("L", 1), obj("N1"), obj("N2")];
    const edges = [edge("L#0", "N1"), edge("L#1", "N2")];
    const layout = arrayLayout(nodes, edges);
    expectWithinCanvas(layout, ["L#0", "L#1", "N1", "N2"]);
    expect(layout.positions.get("N1")!.y).toBeGreaterThan(layout.positions.get("L#0")!.y);
    expect(layout.positions.get("N1")!.x).toBe(layout.positions.get("L#0")!.x);
    expect(layout.positions.get("N2")!.x).toBe(layout.positions.get("L#1")!.x);
  });
});

describe("arrayLayout — determinism", () => {
  it("lays the same array out identically across calls", () => {
    const cells = arrayCells("L", 6);
    expect(snapshot(arrayLayout(cells, []))).toEqual(snapshot(arrayLayout(cells, [])));
  });

  it("does not mutate its input arrays or their elements", () => {
    const cells = arrayCells("L", 3);
    const edges: VizEdge[] = [];
    arrayLayout(cells, edges);
    expect(cells).toHaveLength(3);
    expect(edges).toHaveLength(0);
    expect(cells[0]).toEqual({ id: "L#0", label: "0", kind: "cell", meta: [], slot: 0 });
  });
});
