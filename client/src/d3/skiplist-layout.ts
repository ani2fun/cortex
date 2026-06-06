// Skip-list layout for the trace-driven Visualise renderer (ADR-0018).
//
// Renders a multi-level skip list as a grid of circle nodes:
//   - Column (x) is determined by `node.slot` (0-indexed horizontal position).
//   - Row    (y) is determined by the level read from meta[{name:"level",value:"N"}].
//     Level 0 (densest) appears at the bottom; higher levels stack upward.
//
// Edges within a level are straight horizontal arrows rendered by the default
// graph-render.ts line renderer — no edgePaths override is needed since all
// same-level nodes share the same y coordinate.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const NODE_R  = 22;    // must match graph-render.ts NODE_RADIUS
const CELL    = 72;    // center-to-center horizontal spacing
const PAD_X   = 24;
const ROW_H   = 64;    // vertical spacing between levels
const PAD_TOP = 28;
const PAD_BOT = 28;

/** Read the numeric `level` value from a node's meta array; defaults to 0. */
function nodeLevel(n: VizNode): number {
  if (!n.meta || n.meta.length === 0) return 0;
  const entry = (n.meta as Array<{ name: string; value: string }>).find(
    (m) => m.name === "level",
  );
  return entry !== undefined ? parseInt(entry.value, 10) : 0;
}

/** Multi-row skip-list grid; level 0 at the bottom, higher levels above. */
export const skiplistLayout: LayoutFn = (nodes: VizNode[], _edges: VizEdge[]): GraphLayout => {
  const slotted = nodes.filter((n) => n.slot !== null && n.slot !== undefined);
  if (slotted.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + NODE_R * 2,
      height: PAD_TOP + ROW_H + PAD_BOT,
      edgePaths: new Map(),
    };
  }

  const maxSlot  = Math.max(...slotted.map((n) => n.slot as number));
  const maxLevel = Math.max(...slotted.map(nodeLevel));

  const positions = new Map<string, NodePos>();

  for (const n of slotted) {
    const slot  = n.slot as number;
    const level = nodeLevel(n);
    const x = PAD_X + slot * CELL + NODE_R;
    const y = PAD_TOP + (maxLevel - level) * ROW_H + NODE_R;
    positions.set(n.id, { x, y });
  }

  const width  = PAD_X * 2 + (maxSlot + 1) * CELL;
  const height = PAD_TOP + (maxLevel + 1) * ROW_H + PAD_BOT;

  return { positions, width, height, edgePaths: new Map() };
};
