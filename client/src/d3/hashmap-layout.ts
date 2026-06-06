// Dict / HashMap layout for the trace-driven Visualise renderer (ADR-0018).
// Covers Python dict and Java HashMap heap shapes — the two primary associative
// containers in the DSA book.
//
// Entry nodes (kind === "entry") are stacked in a single vertical column in input
// order, mirroring how a dict prints: each row is one key→value pair. Input order
// is the iteration order of the traced container (insertion order for Python 3.7+
// dicts and Java LinkedHashMap; undefined for HashMap, but stable within a step).
//
// When no entry nodes are present (a HashMap that has not yet been populated, or a
// hand-curated payload using only graph nodes) the layout degrades to graphLayout
// so the renderer still produces a sensible diagram.
//
// No D3, no DOM: positions are pure arithmetic. A new data structure adds a
// LayoutFn like this one — never a new renderer.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";
import { graphLayout } from "./graph-layout";

// Geometry. NODE_R / RING_R mirror NODE_RADIUS and the adornment-ring radius in
// graph-render.ts — the layout sizes the canvas around the renderer's nodes.
const NODE_R = 22;
const RING_R = NODE_R + 4;
const ENTRY_DY = 68; // centre-to-centre between vertically stacked entries
const COL_DX = 72;   // column offset for non-entry ref-target nodes
const PAD_X = 24;    // gap from outermost ring to left / right border
const PAD_TOP = 48;  // headroom above the top entry for a cursor caret
const PAD_BOTTOM = 42; // room below the bottom entry for a meta sub-label

/**
 * Place dict/HashMap entry nodes in a single vertical column, one entry per row.
 * Non-entry nodes reachable from an entry are x-aligned under that entry and
 * placed one row below all entries; unreferenced non-entry nodes take the next
 * free column past the aligned ones.
 */
export const hashmapLayout: LayoutFn = (nodes, edges) => {
  if (nodes.length === 0) return emptyLayout();

  const entries = nodes.filter((n) => n.kind === "entry");
  // No entries → payload is not hashmap-shaped; degrade to generic graph layout.
  if (entries.length === 0) return graphLayout(nodes, edges);

  const raw = new Map<string, NodePos>();

  // Entries: vertical column in input order.
  entries.forEach((entry, i) => {
    raw.set(entry.id, { x: 0, y: i * ENTRY_DY });
  });

  // Non-entry nodes (ref-target values an entry points at): placed in a row below
  // all entries, x-aligned under the entry that references them (if discoverable
  // via an edge). Anything unreferenced takes the next free column.
  const others = nodes.filter((n) => n.kind !== "entry");
  if (others.length > 0) {
    const otherIds = new Set(others.map((n) => n.id));
    const alignedX = new Map<string, number>();
    for (const e of edges) {
      const fromPos = raw.get(e.from);
      if (
        fromPos !== undefined &&
        otherIds.has(e.to) &&
        !alignedX.has(e.to)
      ) {
        alignedX.set(e.to, fromPos.x);
      }
    }
    const baseY = entries.length * ENTRY_DY;
    let freeCol = 0;
    for (const x of alignedX.values()) {
      freeCol = Math.max(freeCol, Math.round(x / COL_DX) + 1);
    }
    for (const o of others) {
      const x = alignedX.get(o.id);
      raw.set(o.id, { x: x ?? freeCol++ * COL_DX, y: baseY });
    }
  }

  return frame(nodes, raw);
};

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
