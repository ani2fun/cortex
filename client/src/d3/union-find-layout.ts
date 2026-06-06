// Union-Find (Disjoint Set Union) layout for the trace-driven Visualise renderer (ADR-0018).
//
// Renders n elements as a horizontal row of circle nodes (one per slot), with
// parent arcs curving above the row:
//   - Non-root nodes: a quadratic Bézier from child top → parent top, height
//     proportional to |parentSlot - childSlot| * CELL, capped at MAX_ARC_HEIGHT.
//   - Root nodes (kind "root"): a small self-loop arc above the circle.
//
// Rank annotations use node.meta[{name:"rank",value:"N"}] — graph-render already
// renders meta text below each node via viz-graph__meta, so no extra logic is needed.
//
// Geometry constants mirror stack-queue-layout.ts (NODE_R=22, CELL=56, PAD_X=24)
// for visual consistency across the DSA book.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const NODE_R       = 22;   // must match graph-render.ts NODE_RADIUS
const CELL         = 56;   // center-to-center spacing
const PAD_X        = 24;
const MAX_ARC_H    = 80;   // cap on arc height (keeps tall DSU forests from blowing the SVG)
const ARC_HEADROOM = 20;   // margin above the tallest arc
const PAD_TOP      = MAX_ARC_H + ARC_HEADROOM + 8;  // = 108 — fits arcs + cursor labels
const PAD_BOT      = 28;

/** Horizontal row of circle nodes; parent pointers rendered as curved arcs above the row. */
export const unionFindLayout: LayoutFn = (nodes: VizNode[], edges: VizEdge[]): GraphLayout => {
  const slotted = nodes.filter((n) => n.slot !== null);
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
    positions.set(n.id, { x: PAD_X + s * CELL + NODE_R, y: rowY });
  }

  const width  = PAD_X * 2 + (maxSlot + 1) * CELL;
  const height = PAD_TOP + NODE_R * 2 + PAD_BOT;

  // Build per-edge SVG path overrides — curved arcs above the row.
  const edgePaths = new Map<string, string>();
  const kindById  = new Map(slotted.map((n) => [n.id, n.kind]));

  for (const e of edges) {
    const fromPos = positions.get(e.from);
    const toPos   = positions.get(e.to);
    if (fromPos === undefined || toPos === undefined) continue;

    const key = `${e.from}->${e.to}`;

    if (e.from === e.to || kindById.get(e.from) === "root") {
      // Self-loop: small circular arc starting and ending at the top of the circle.
      edgePaths.set(key, selfLoopPath(fromPos.x, fromPos.y));
    } else {
      // Parent arc: quadratic Bézier curving above the row.
      edgePaths.set(key, parentArcPath(fromPos.x, fromPos.y, toPos.x, toPos.y));
    }
  }

  return { positions, width, height, edgePaths };
};

/** Quadratic Bézier from the top of the child circle to the top of the parent circle,
 *  bulging above the row. Arc height scales with horizontal distance, capped at MAX_ARC_H. */
function parentArcPath(x1: number, y1: number, x2: number, y2: number): string {
  const topY  = y1 - NODE_R;               // top of circle (same for both since y1 === y2)
  const midX  = (x1 + x2) / 2;
  const dist  = Math.abs(x2 - x1);
  const arcH  = Math.min(dist * 0.55, MAX_ARC_H);
  const cpY   = topY - arcH;
  return `M ${x1} ${topY} Q ${midX} ${cpY} ${x2} ${topY}`;
}

/** Small oval self-loop above the node for root elements (parent[i] == i). */
function selfLoopPath(cx: number, cy: number): string {
  const topY  = cy - NODE_R;
  const loopW = NODE_R * 0.8;   // half-width of the loop
  const loopH = NODE_R * 1.1;   // height of the loop above the node top
  const x1    = cx - loopW;
  const x2    = cx + loopW;
  // Cubic Bézier: start at left of top, arc up and over, return to right of top.
  const cpY   = topY - loopH;
  return `M ${x1} ${topY} C ${x1} ${cpY} ${x2} ${cpY} ${x2} ${topY}`;
}
