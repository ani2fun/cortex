// Stack and queue layouts for the trace-driven Visualise renderer (ADR-0018).
//
// stackLayout — vertical column of circle nodes, bottom-up. slot=0 sits at the
//   bottom (largest y); higher slots are stacked toward the top.
// queueLayout — horizontal row of circle nodes, left-to-right. slot=0 is the
//   front (smallest x); higher slots extend toward the back (right).
//
// Both layouts read `node.slot` for position and ignore edges — stacks and queues
// are slot-addressed, not pointer-linked. Nodes without a slot are silently skipped.
// Cursor labels (top / front / back) render above each target node via the generic
// graph renderer — no special cursor logic lives here.
//
// Geometry constants mirror graph-render.ts (NODE_RADIUS=22) and array-layout.ts
// (CELL_DX=56, PAD_X=24, PAD_TOP=48) so stacks and queues sit visually at the
// same scale as arrays in the DSA book.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const NODE_R = 22;  // must match graph-render.ts NODE_RADIUS
const CELL   = 56;  // center-to-center spacing (matches array-layout CELL_DX)
const PAD_X  = 24;
const PAD_TOP = 48; // headroom above nodes for cursor caret text
const PAD_BOT = 28;

/** Vertical stack — slot 0 at the bottom, increasing slots stacked upward. */
export const stackLayout: LayoutFn = (nodes: VizNode[], _edges: VizEdge[]): GraphLayout => {
  const slotted = nodes.filter((n) => n.slot !== null);
  if (slotted.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + NODE_R * 2,
      height: PAD_TOP + NODE_R * 2 + PAD_BOT,
    };
  }

  const maxSlot = Math.max(...slotted.map((n) => n.slot as number));
  const positions = new Map<string, NodePos>();
  const cx = PAD_X + NODE_R;

  for (const n of slotted) {
    const s = n.slot as number;
    positions.set(n.id, {
      x: cx,
      // slot 0 = bottom (largest y); slot maxSlot = top (smallest y)
      y: PAD_TOP + (maxSlot - s) * CELL + NODE_R,
    });
  }

  return {
    positions,
    width: PAD_X * 2 + NODE_R * 2,
    height: PAD_TOP + (maxSlot + 1) * CELL + PAD_BOT,
  };
};

/** Horizontal queue — slot 0 at the left (front), increasing slots toward the right (back). */
export const queueLayout: LayoutFn = (nodes: VizNode[], _edges: VizEdge[]): GraphLayout => {
  const slotted = nodes.filter((n) => n.slot !== null);
  if (slotted.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + NODE_R * 2,
      height: PAD_TOP + NODE_R * 2 + PAD_BOT,
    };
  }

  const maxSlot = Math.max(...slotted.map((n) => n.slot as number));
  const positions = new Map<string, NodePos>();
  const cy = PAD_TOP + NODE_R;

  for (const n of slotted) {
    const s = n.slot as number;
    positions.set(n.id, {
      x: PAD_X + s * CELL + NODE_R,
      y: cy,
    });
  }

  return {
    positions,
    width: PAD_X * 2 + (maxSlot + 1) * CELL,
    height: PAD_TOP + NODE_R * 2 + PAD_BOT,
  };
};
