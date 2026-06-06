// Fenwick (Binary Indexed Tree) layout for the trace-driven Visualise renderer (ADR-0018).
//
// Renders n elements as a horizontal row of circle nodes (1-indexed), with
// traversal arcs curving below the row:
//   - Query arcs (i → i − lowbit(i)): leftward below the row.
//   - Update arcs (i → i + lowbit(i)): rightward below the row.
//
// Coverage ranges are shown via node.meta[{name:"range",value:"[l,r]"}] annotations;
// graph-render renders meta text below each node via viz-graph__meta with no extra
// layout logic needed.
//
// Geometry constants mirror union-find-layout.ts (NODE_R=22, CELL=56, PAD_X=24)
// for visual consistency across the DSA book.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const NODE_R    = 22;    // must match graph-render.ts NODE_RADIUS
const CELL      = 56;    // center-to-center spacing
const PAD_X     = 24;
const MAX_ARC_H = 80;    // cap on traversal-arc depth below the row
const PAD_TOP   = 36;    // headroom for cursor labels above the row
const PAD_BOT   = MAX_ARC_H + 32;  // space for traversal arcs + annotation labels

/** Horizontal row of 1-indexed circle nodes; LSB-jump traversal arcs curve below the row. */
export const fenwickLayout: LayoutFn = (nodes: VizNode[], edges: VizEdge[]): GraphLayout => {
  const slotted = nodes.filter((n) => n.slot !== null && n.slot !== undefined);
  if (slotted.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + NODE_R * 2,
      height: PAD_TOP + NODE_R * 2 + PAD_BOT,
      edgePaths: new Map(),
    };
  }

  const maxSlot = Math.max(...slotted.map((n) => n.slot as number));
  const positions = new Map<string, NodePos>();
  const rowY = PAD_TOP + NODE_R;

  for (const n of slotted) {
    const s = n.slot as number;
    // 1-based slot: slot 1 → leftmost cell starting at PAD_X + NODE_R
    positions.set(n.id, { x: PAD_X + (s - 1) * CELL + NODE_R, y: rowY });
  }

  const width  = PAD_X * 2 + maxSlot * CELL;
  const height = PAD_TOP + NODE_R * 2 + PAD_BOT;

  // Build per-edge SVG path overrides — traversal arcs below the row.
  const edgePaths = new Map<string, string>();

  for (const e of edges) {
    const fromPos = positions.get(e.from);
    const toPos   = positions.get(e.to);
    if (fromPos === undefined || toPos === undefined) continue;

    const key = `${e.from}->${e.to}`;
    edgePaths.set(key, traversalArcPath(fromPos.x, fromPos.y, toPos.x, toPos.y));
  }

  return { positions, width, height, edgePaths };
};

/** Quadratic Bézier from the bottom of the source cell to the bottom of the target cell,
 *  curving below the row. Arc depth scales with horizontal distance, capped at MAX_ARC_H. */
function traversalArcPath(x1: number, y1: number, x2: number, y2: number): string {
  const botY = y1 + NODE_R;    // bottom of circle (same for both since y1 === y2)
  const midX = (x1 + x2) / 2;
  const dist = Math.abs(x2 - x1);
  const arcH = Math.min(dist * 0.55, MAX_ARC_H);
  const cpY  = botY + arcH;
  return `M ${x1} ${botY} Q ${midX} ${cpY} ${x2} ${botY}`;
}
