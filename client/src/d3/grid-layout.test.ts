// Unit tests for the grid layout (grid-layout.ts). The layout is a pure
// function — positions are arithmetic over `VizNode.slot` and the cell-of-cells
// edge structure, no DOM, no RNG — so it runs in plain Node under vitest. The
// contract under test: inner cells form aligned rows and columns placed by
// (row, col), outer row-pointer cells are hidden behind their row's first cell,
// a payload that is not 2-D-shaped degrades to the generic graph layout, and
// the result is deterministic.

import { describe, it, expect } from "vitest";
import type { VizNode, VizEdge } from "./types";
import type { GraphLayout } from "./tree-layout";
import { gridLayout } from "./grid-layout";
import { graphLayout } from "./graph-layout";

// A cell — id `${arrId}#${slot}`, the form HeapToGraph synthesises.
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

// The outer (row-pointer) cell id for row `r` of matrix `outerId`.
function outer(outerId: string, r: number): string {
  return `${outerId}#${r}`;
}

// The inner (matrix) cell id at (row `r`, col `c`) of matrix `outerId`.
function inner(outerId: string, r: number, c: number): string {
  return `${outerId}_r${r}#${c}`;
}

// A full `rows`x`cols` 2-D matrix, the shape HeapToGraph emits for a list-of-
// lists: one outer cell per row (slot = row), one inner cell per element
// (slot = col), and the outer -> inner fan edges.
function matrix(outerId: string, rows: number, cols: number): { nodes: VizNode[]; edges: VizEdge[] } {
  const nodes: VizNode[] = [];
  const edges: VizEdge[] = [];
  for (let r = 0; r < rows; r += 1) {
    nodes.push(cell(outerId, r));
    for (let c = 0; c < cols; c += 1) {
      nodes.push(cell(`${outerId}_r${r}`, c));
      edges.push(edge(outer(outerId, r), inner(outerId, r, c)));
    }
  }
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

describe("gridLayout — degenerate inputs", () => {
  it("an empty graph yields an empty, non-zero canvas", () => {
    const layout = gridLayout([], []);
    expect(layout.positions.size).toBe(0);
    expect(layout.width).toBeGreaterThan(0);
    expect(layout.height).toBeGreaterThan(0);
  });

  it("a 1x1 matrix sits inside a sane canvas", () => {
    const { nodes, edges } = matrix("M", 1, 1);
    const layout = gridLayout(nodes, edges);
    expect(layout.positions.size).toBe(2); // the outer cell + the lone inner cell
    expectWithinCanvas(layout, [outer("M", 0), inner("M", 0, 0)]);
    // Big enough to actually contain a node circle (radius 22 in the renderer).
    expect(layout.width).toBeGreaterThanOrEqual(44);
    expect(layout.height).toBeGreaterThanOrEqual(44);
  });

  it("hands a payload with no cells to the generic graph layout", () => {
    // Two instances + an edge — not grid-shaped; both must still be placed.
    const layout = gridLayout([obj("a"), obj("b")], [edge("a", "b")]);
    expect(layout.positions.size).toBe(2);
    expectWithinCanvas(layout, ["a", "b"]);
  });

  it("hands a flat 1-D array (no cell-of-cells) to the generic graph layout", () => {
    // Cells but no cell -> cell edge: a `viz=grid` hint on a 1-D array.
    const nodes = [cell("L", 0), cell("L", 1), cell("L", 2)];
    expect(snapshot(gridLayout(nodes, []))).toEqual(snapshot(graphLayout(nodes, [])));
  });
});

describe("gridLayout — a 2-D matrix", () => {
  it("places inner cells in aligned rows and columns", () => {
    const { nodes, edges } = matrix("M", 2, 3);
    const layout = gridLayout(nodes, edges);
    const innerIds = [
      inner("M", 0, 0), inner("M", 0, 1), inner("M", 0, 2),
      inner("M", 1, 0), inner("M", 1, 1), inner("M", 1, 2),
    ];
    expectWithinCanvas(layout, innerIds);
    // Each row's cells share one y; each column's cells share one x.
    for (let r = 0; r < 2; r += 1) {
      const ys = [0, 1, 2].map((c) => layout.positions.get(inner("M", r, c))!.y);
      expect(new Set(ys).size).toBe(1);
    }
    for (let c = 0; c < 3; c += 1) {
      const xs = [0, 1].map((r) => layout.positions.get(inner("M", r, c))!.x);
      expect(new Set(xs).size).toBe(1);
    }
    // Two distinct rows, three distinct columns.
    expect(new Set(innerIds.map((id) => layout.positions.get(id)!.y)).size).toBe(2);
    expect(new Set(innerIds.map((id) => layout.positions.get(id)!.x)).size).toBe(3);
  });

  it("places a cell at its (row, col) — col grows rightward, row grows downward", () => {
    const { nodes, edges } = matrix("M", 3, 3);
    const layout = gridLayout(nodes, edges);
    const at = (r: number, c: number) => layout.positions.get(inner("M", r, c))!;
    expect(at(1, 2).x).toBeGreaterThan(at(1, 0).x); // col 2 right of col 0
    expect(at(2, 1).y).toBeGreaterThan(at(0, 1).y); // row 2 below row 0
  });

  it("spaces rows and columns evenly", () => {
    const { nodes, edges } = matrix("M", 3, 4);
    const layout = gridLayout(nodes, edges);
    const xs = [0, 1, 2, 3].map((c) => layout.positions.get(inner("M", 0, c))!.x);
    const ys = [0, 1, 2].map((r) => layout.positions.get(inner("M", r, 0))!.y);
    const xGaps = xs.slice(1).map((x, i) => x - xs[i]);
    const yGaps = ys.slice(1).map((y, i) => y - ys[i]);
    for (const g of xGaps) expect(g).toBeCloseTo(xGaps[0]);
    for (const g of yGaps) expect(g).toBeCloseTo(yGaps[0]);
    expect(xGaps[0]).toBeGreaterThan(0);
    expect(yGaps[0]).toBeGreaterThan(0);
  });

  it("places cells by slot, not by their order in the input", () => {
    const { nodes, edges } = matrix("M", 2, 3);
    // Same matrix, nodes handed in reverse — placement must not change.
    const reversed = gridLayout([...nodes].reverse(), edges);
    expect(snapshot(reversed)).toEqual(snapshot(gridLayout(nodes, edges)));
  });

  it("a bigger matrix yields a bigger canvas", () => {
    const small = matrix("M", 2, 2);
    const big = matrix("M", 5, 6);
    const sl = gridLayout(small.nodes, small.edges);
    const bl = gridLayout(big.nodes, big.edges);
    expect(bl.width).toBeGreaterThan(sl.width);
    expect(bl.height).toBeGreaterThan(sl.height);
  });
});

describe("gridLayout — outer row-pointer cells", () => {
  it("hides each outer cell coincident with its row's first inner cell", () => {
    const { nodes, edges } = matrix("M", 3, 4);
    const layout = gridLayout(nodes, edges);
    for (let r = 0; r < 3; r += 1) {
      expect(layout.positions.get(outer("M", r))).toEqual(
        layout.positions.get(inner("M", r, 0)),
      );
    }
  });
});

describe("gridLayout — jagged and sparse matrices", () => {
  it("lays out a jagged matrix by absolute column", () => {
    // row 0 has 3 columns, row 1 has 2 — columns 0 and 1 still line up.
    const nodes = [
      cell("M", 0), cell("M", 1),
      cell("M_r0", 0), cell("M_r0", 1), cell("M_r0", 2),
      cell("M_r1", 0), cell("M_r1", 1),
    ];
    const edges = [
      edge("M#0", "M_r0#0"), edge("M#0", "M_r0#1"), edge("M#0", "M_r0#2"),
      edge("M#1", "M_r1#0"), edge("M#1", "M_r1#1"),
    ];
    const layout = gridLayout(nodes, edges);
    expectWithinCanvas(layout, nodes.map((n) => n.id));
    expect(layout.positions.get(inner("M", 1, 0))!.x).toBe(layout.positions.get(inner("M", 0, 0))!.x);
    expect(layout.positions.get(inner("M", 1, 1))!.x).toBe(layout.positions.get(inner("M", 0, 1))!.x);
    expect(layout.positions.get(inner("M", 1, 0))!.y).toBeGreaterThan(
      layout.positions.get(inner("M", 0, 0))!.y,
    );
  });

  it("handles an empty row between two populated rows", () => {
    // [[a, b], [], [c, d]] — row 1 has no inner cells.
    const nodes = [
      cell("M", 0), cell("M", 1), cell("M", 2),
      cell("M_r0", 0), cell("M_r0", 1),
      cell("M_r2", 0), cell("M_r2", 1),
    ];
    const edges = [
      edge("M#0", "M_r0#0"), edge("M#0", "M_r0#1"),
      edge("M#2", "M_r2#0"), edge("M#2", "M_r2#1"),
    ];
    const layout = gridLayout(nodes, edges);
    expect(layout.positions.size).toBe(7);
    expectWithinCanvas(layout, nodes.map((n) => n.id));
    // The empty row's outer cell sits between the two populated rows.
    const emptyRowY = layout.positions.get(outer("M", 1))!.y;
    expect(emptyRowY).toBeGreaterThan(layout.positions.get(inner("M", 0, 0))!.y);
    expect(emptyRowY).toBeLessThan(layout.positions.get(inner("M", 2, 0))!.y);
  });
});

describe("gridLayout — non-2-D payloads fall back to the graph layout", () => {
  it("degrades a 3-D nesting (cell -> cell -> cell) to graphLayout", () => {
    // Two layers of cell-of-cells: a middle cell is both outer and inner.
    const nodes = [cell("C", 0), cell("M", 0), cell("I", 0)];
    const edges = [edge("C#0", "M#0"), edge("M#0", "I#0")];
    expect(snapshot(gridLayout(nodes, edges))).toEqual(snapshot(graphLayout(nodes, edges)));
    expectWithinCanvas(gridLayout(nodes, edges), ["C#0", "M#0", "I#0"]);
  });
});

describe("gridLayout — a grid of objects", () => {
  it("places ref targets in a row below the matrix, aligned under their cell", () => {
    const { nodes, edges } = matrix("M", 2, 2);
    const objs = [obj("o0"), obj("o1"), obj("o2"), obj("o3")];
    const refEdges = [
      edge(inner("M", 0, 0), "o0"), edge(inner("M", 0, 1), "o1"),
      edge(inner("M", 1, 0), "o2"), edge(inner("M", 1, 1), "o3"),
    ];
    const layout = gridLayout([...nodes, ...objs], [...edges, ...refEdges]);
    expectWithinCanvas(layout, [...nodes, ...objs].map((n) => n.id));
    // Every ref target sits below every matrix cell.
    const cellYs = nodes.map((n) => layout.positions.get(n.id)!.y);
    const maxCellY = Math.max(...cellYs);
    for (const o of objs) expect(layout.positions.get(o.id)!.y).toBeGreaterThan(maxCellY);
    // Each target is x-aligned under the cell that references it.
    expect(layout.positions.get("o0")!.x).toBe(layout.positions.get(inner("M", 0, 0))!.x);
    expect(layout.positions.get("o3")!.x).toBe(layout.positions.get(inner("M", 1, 1))!.x);
  });
});

describe("gridLayout — determinism", () => {
  it("lays the same matrix out identically across calls", () => {
    const { nodes, edges } = matrix("M", 3, 4);
    expect(snapshot(gridLayout(nodes, edges))).toEqual(snapshot(gridLayout(nodes, edges)));
  });

  it("does not mutate its input arrays or their elements", () => {
    const { nodes, edges } = matrix("M", 2, 2);
    gridLayout(nodes, edges);
    expect(nodes).toHaveLength(6); // 2 outer cells + 4 inner cells
    expect(edges).toHaveLength(4);
    expect(nodes[0]).toEqual({ id: "M#0", label: "0", kind: "cell", meta: [], slot: 0 });
    expect(edges[0]).toEqual({ from: "M#0", to: "M_r0#0", label: "" });
  });
});
