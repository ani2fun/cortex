// Array layout — a row of indexed cells for the trace-driven Visualise renderer
// (ADR-0018). Covers every 1-D structure the DSA book draws as a sequence:
// arrays, strings, bit tricks, sorting, searching, 1-D DP, and array-backed
// stack / queue / heap.
//
// A `VizNode` carrying a non-null `slot` is an array cell. Cells are placed in a
// tight, contiguous row at x = slot * CELL_DX — so a value moves between fixed
// boxes the way an in-place sort does, and the column *is* the index. Placement
// reads `slot`, never input order. Cells are grouped by their owning array's id,
// so the rare payload holding two arrays stacks them as two rows instead of
// overlapping them.
//
// A node with no slot — a ref target an object-array cell points at — is laid
// out in a row below the cells, x-aligned under the cell that references it. A
// payload with no cells at all is not array-shaped (the viz-root resolved to an
// instance, say); it is handed to the generic graph layout, which draws a row /
// tree / graph correctly.
//
// No d3, no DOM: positions are pure arithmetic, unit-tested in plain Node (see
// array-layout.test.ts). A new data structure adds a LayoutFn like this one —
// never a new renderer.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";
import { graphLayout } from "./graph-layout";

// Geometry. NODE_R / RING_R mirror NODE_RADIUS and the adornment-ring radius in
// graph-render.ts — the layout sizes the canvas around the renderer's nodes.
const NODE_R = 22;
const RING_R = NODE_R + 4;
const CELL_DX = 56; // centre-to-centre within a row — a snug, indexed strip
const ROW_DY = 96; // between stacked array rows, and cells -> their ref targets
const PAD_X = 24; // gap from the outermost ring to the left / right border
const PAD_TOP = 48; // headroom above the top row for the cursor caret
const PAD_BOTTOM = 42; // room below the bottom row for a meta sub-label

/**
 * Place the union of every node + edge in a Visualise animation as a row of
 * indexed cells. Deterministic — positions are a pure function of `slot` and of
 * input order — so re-opening the modal always redraws identically.
 */
export const arrayLayout: LayoutFn = (nodes, edges) => {
  if (nodes.length === 0) return emptyLayout();

  const cells = nodes.filter((n) => n.slot !== null);
  // No cells -> the payload is not array-shaped. The generic graph layout draws
  // a row / tree / graph correctly; degrading to it beats mangling it here.
  if (cells.length === 0) return graphLayout(nodes, edges);

  const raw = new Map<string, NodePos>();

  // --- cells: one row per owning array, one column per slot ---
  // Rows are ordered by each array's first appearance in `nodes`, so the layout
  // is stable and the common lone-array case is simply row 0.
  const rowOf = new Map<string, number>();
  for (const c of cells) {
    const arr = arrayIdOf(c.id);
    if (!rowOf.has(arr)) rowOf.set(arr, rowOf.size);
  }
  for (const c of cells) {
    const row = rowOf.get(arrayIdOf(c.id)) ?? 0;
    raw.set(c.id, { x: (c.slot ?? 0) * CELL_DX, y: row * ROW_DY });
  }

  // --- non-cell nodes: a row below the cells ---
  // A `viz=array` payload that has any of these is an array of objects; each
  // such node hangs off one cell (a cell is one element -> one reference), so
  // x-align it under that cell. Anything unreferenced by a cell (defensive — a
  // malformed payload) takes the next free column past the aligned ones, so the
  // two groups never share a position.
  const others = nodes.filter((n) => n.slot === null);
  if (others.length > 0) {
    const otherIds = new Set(others.map((n) => n.id));
    const alignedX = new Map<string, number>();
    for (const e of edges) {
      const fromCell = raw.get(e.from); // only cells are in `raw` so far
      if (fromCell !== undefined && otherIds.has(e.to) && !alignedX.has(e.to)) {
        alignedX.set(e.to, fromCell.x);
      }
    }
    const otherRowY = rowOf.size * ROW_DY;
    let freeCol = 0;
    for (const x of alignedX.values()) {
      freeCol = Math.max(freeCol, Math.round(x / CELL_DX) + 1);
    }
    for (const o of others) {
      const x = alignedX.get(o.id);
      raw.set(o.id, { x: x ?? freeCol++ * CELL_DX, y: otherRowY });
    }
  }

  return frame(nodes, raw);
};

/** The owning array's id: a cell id is `${arrayId}#${index}` — split on the last `#`. */
function arrayIdOf(cellId: string): string {
  const hash = cellId.lastIndexOf("#");
  return hash > 0 ? cellId.slice(0, hash) : cellId;
}

// Translate raw positions into a padded canvas and report its size. Mirrors the
// inset discipline of graph-layout.ts: the outermost node rings clear the border
// by PAD_X, with caret headroom above and meta room below.
function frame(nodes: VizNode[], raw: Map<string, NodePos>): GraphLayout {
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;
  for (const n of nodes) {
    const p = raw.get(n.id);
    if (p === undefined) continue;
    minX = Math.min(minX, p.x);
    maxX = Math.max(maxX, p.x);
    minY = Math.min(minY, p.y);
    maxY = Math.max(maxY, p.y);
  }
  if (!Number.isFinite(minX)) return emptyLayout();

  const dx = PAD_X + RING_R - minX;
  const dy = PAD_TOP - minY;
  const positions = new Map<string, NodePos>();
  for (const n of nodes) {
    const p = raw.get(n.id) ?? { x: minX, y: minY };
    positions.set(n.id, { x: p.x + dx, y: p.y + dy });
  }
  return {
    positions,
    width: maxX - minX + 2 * (PAD_X + RING_R),
    height: maxY - minY + PAD_TOP + PAD_BOTTOM,
  };
}

function emptyLayout(): GraphLayout {
  return {
    positions: new Map(),
    width: 2 * (PAD_X + RING_R),
    height: PAD_TOP + PAD_BOTTOM,
  };
}
