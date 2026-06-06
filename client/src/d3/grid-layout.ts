// Grid layout — a 2-D matrix of cells for the trace-driven Visualise renderer
// (ADR-0018). Covers every 2-D structure the DSA book draws as a table: 2-D DP
// tables, adjacency matrices, grid-traversal problems, and any 2-D array.
//
// A 2-D Python list is a list-of-lists, so HeapToGraph emits it as two cell
// layers: one OUTER cell per outer-list element (its `slot` is the row index),
// each holding a reference; and one INNER cell per inner-list element (its
// `slot` is the column index). Because a reference to a collection has no
// single node to land on, each outer cell's edge *fans out* — one edge to
// every cell of its row. The matrix proper is the inner cells: inner cell
// (row, col) is placed at a grid position — `row` from its owning outer cell's
// slot, `col` from its own slot.
//
// The design decision — what to do with the outer (row-pointer) cells — is to
// HIDE them: this layout shows only the matrix. A layout cannot remove a node,
// so "hide" is realised by placing each outer cell coincident with its row's
// leftmost inner cell. HeapToGraph emits a root's cells before its children's
// (reachability is depth-first from the root), so the renderer draws the outer
// cells first — fully occluded by the inner cells stacked on top. The chosen
// alternative — a visible left-hand index column — was rejected: the A1b
// integer-index cursors land `i` AND `j` on outer (row-pointer) cells, so an
// index column would draw a column cursor `j` pointing at a *row*, actively
// misleading; hidden outer cells make that known cursor gap fail invisibly
// instead. And when the adapter later emits true (row, col) cursors on inner
// cells, the matrix is already the final form — no scaffolding column to undo.
//
// Known limitation — the fan edges. The renderer draws every edge between two
// present nodes; a layout cannot suppress one. So the outer -> inner fan edges
// are still drawn, along each row (the outer cell sits on its row's first
// cell). They overlap into roughly one muted line per row. A genuinely
// edge-free matrix needs the renderer to learn to suppress structural fan
// edges, or the adapter to not emit them — out of scope for a layout-only
// session (the A0 plan already flags this fan as "potentially noisy").
//
// A payload that is not 2-D-shaped — no cell-of-cells (a flat 1-D array), or a
// 3-D+ nesting — is handed to the generic graph layout, the way `array`
// degrades a no-cells payload and `linked-list` degrades a cyclic chain.
//
// No d3, no DOM: positions are pure arithmetic, unit-tested in plain Node (see
// grid-layout.test.ts). A new data structure adds a LayoutFn like this one —
// never a new renderer.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";
import { graphLayout } from "./graph-layout";

// Geometry. NODE_R / RING_R mirror NODE_RADIUS and the adornment-ring radius in
// graph-render.ts — the layout sizes the canvas around the renderer's nodes.
const NODE_R = 22;
const RING_R = NODE_R + 4;
const CELL_DX = 56; // centre-to-centre within a row — a snug, indexed strip
const CELL_DY = 68; // centre-to-centre between rows — leaves a cursor caret room
const REF_ROW_DY = 96; // from the matrix's last row down to the ref-target row
const PAD_X = 24; // gap from the outermost ring to the left / right border
const PAD_TOP = 48; // headroom above the top row for the cursor caret
const PAD_BOTTOM = 42; // room below the bottom row for a meta sub-label

/**
 * Place the union of every node + edge in a Visualise animation as a 2-D
 * matrix. Deterministic — positions are a pure function of `slot` and of the
 * cell-of-cells edge structure — so re-opening the modal always redraws
 * identically.
 */
export const gridLayout: LayoutFn = (nodes, edges) => {
  if (nodes.length === 0) return emptyLayout();

  const cells = nodes.filter((n) => n.slot !== null);
  // No slot-bearing nodes -> the viz-root resolved to an instance, not a
  // list-of-lists. Not grid-shaped; the generic graph layout draws it correctly.
  if (cells.length === 0) return graphLayout(nodes, edges);

  const cellIds = new Set(cells.map((c) => c.id));
  const slotById = new Map<string, number>();
  for (const c of cells) slotById.set(c.id, c.slot ?? 0);

  const ids = new Set(nodes.map((n) => n.id));
  // Drop self-loops and dangling edges — neither informs placement.
  const real = edges.filter((e) => e.from !== e.to && ids.has(e.from) && ids.has(e.to));

  // A 2-D matrix is a list-of-lists: an OUTER array of row-pointer cells, each
  // fanning to the cells of one INNER array (a row). A cell -> cell edge names
  // the outer array (its `from`'s array id) and an inner cell (`to`).
  const outerArrayIds = new Set<string>();
  const rowOfInner = new Map<string, number>(); // inner cell id -> matrix row
  for (const e of real) {
    if (cellIds.has(e.from) && cellIds.has(e.to)) {
      outerArrayIds.add(arrayIdOf(e.from));
      // The owning outer cell's slot is the row index. First edge wins — a
      // deterministic pick if rows are aliased (the `[[0]*n]*m` gotcha).
      if (!rowOfInner.has(e.to)) rowOfInner.set(e.to, slotById.get(e.from) ?? 0);
    }
  }
  // Exactly one outer array is a clean 2-D matrix. None -> a flat 1-D array (no
  // cell-of-cells); two or more -> a 3-D+ nesting (a middle layer is both an
  // outer and an inner array). Either way it is not 2-D — degrade to the
  // generic graph layout.
  if (outerArrayIds.size !== 1) return graphLayout(nodes, edges);
  const outerArrayId = [...outerArrayIds][0];

  const innerCells = cells.filter((c) => arrayIdOf(c.id) !== outerArrayId);
  // Every inner cell must be a row member (have an owning outer cell). One that
  // is not means a reachable array that is not a row of this matrix — not a
  // clean 2-D shape; degrade rather than draw it half-right.
  if (innerCells.some((c) => !rowOfInner.has(c.id))) return graphLayout(nodes, edges);

  // --- inner cells: the matrix proper, placed by (row, col) ---
  const raw = new Map<string, NodePos>();
  const minColOfRow = new Map<number, number>();
  for (const c of innerCells) {
    const row = rowOfInner.get(c.id) ?? 0;
    const col = c.slot ?? 0;
    raw.set(c.id, { x: col * CELL_DX, y: row * CELL_DY });
    const lo = minColOfRow.get(row);
    if (lo === undefined || col < lo) minColOfRow.set(row, col);
  }

  // --- outer (row-pointer) cells: hidden behind their row's first cell ---
  // See the file header: placed coincident with the row's leftmost inner cell,
  // drawn first by the renderer and so fully occluded by the matrix on top.
  const outerCells = cells.filter((c) => arrayIdOf(c.id) === outerArrayId);
  for (const c of outerCells) {
    const row = c.slot ?? 0;
    const col = minColOfRow.get(row) ?? 0;
    raw.set(c.id, { x: col * CELL_DX, y: row * CELL_DY });
  }

  // --- non-cell nodes: a row below the matrix ---
  // A grid of objects (each inner cell holds a reference) hangs its targets in
  // a row below, x-aligned under the referencing cell — mirrors array-layout.
  const others = nodes.filter((n) => n.slot === null);
  if (others.length > 0) {
    const otherIds = new Set(others.map((n) => n.id));
    const alignedX = new Map<string, number>();
    for (const e of real) {
      const fromPos = raw.get(e.from);
      if (fromPos !== undefined && otherIds.has(e.to) && !alignedX.has(e.to)) {
        alignedX.set(e.to, fromPos.x);
      }
    }
    let maxRow = 0;
    for (const r of rowOfInner.values()) maxRow = Math.max(maxRow, r);
    for (const c of outerCells) maxRow = Math.max(maxRow, c.slot ?? 0);
    const otherY = maxRow * CELL_DY + REF_ROW_DY;
    let freeCol = 0;
    for (const x of alignedX.values()) freeCol = Math.max(freeCol, Math.round(x / CELL_DX) + 1);
    for (const o of others) {
      const x = alignedX.get(o.id);
      raw.set(o.id, { x: x ?? freeCol++ * CELL_DX, y: otherY });
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
// inset discipline of graph-layout.ts / array-layout.ts: the outermost node
// rings clear the border by PAD_X, with caret headroom above and meta room below.
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
