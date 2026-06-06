// Bitset layout for the trace-driven Visualise renderer (ADR-0018).
//
// Renders n bits as a compact horizontal row of small circle nodes (0-indexed).
// No traversal arcs — bitsets have no directed connections.
//
// Smaller NODE_R (18) and CELL (44) than other layouts so wide bitsets (16+ bits)
// stay within a reasonable viewport width.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const NODE_R  = 18;    // layout radius — circle centers are positioned using this value
const CELL    = 44;    // center-to-center spacing
const PAD_X   = 24;
const PAD_TOP = 32;    // headroom for cursor labels above the row
const PAD_BOT = 28;    // bottom margin

/** Compact horizontal row of 0-indexed circle nodes. No edges. */
export const bitsetLayout: LayoutFn = (nodes: VizNode[], _edges: VizEdge[]): GraphLayout => {
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
    // 0-based slot: slot 0 → leftmost cell starting at PAD_X + NODE_R
    positions.set(n.id, { x: PAD_X + s * CELL + NODE_R, y: rowY });
  }

  const width  = PAD_X * 2 + (maxSlot + 1) * CELL;
  const height = PAD_TOP + NODE_R * 2 + PAD_BOT;

  return { positions, width, height, edgePaths: new Map() };
};
