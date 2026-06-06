// Segment-tree layout — BFS-indexed complete binary tree for the trace-driven
// Visualise renderer (ADR-0018).
//
// Each node id is a 1-based BFS index (root = 1, left child of k = 2k,
// right child of k = 2k+1). The layout derives tree geometry purely from these
// indices: level = floor(log2(id)), slot within level = id − 2^level. A node
// at level k, slot s is centred over its 2^(H−1−k) leaf columns, where H is
// the total height of the tree (derived from the maximum BFS index present).
//
// Edges go from child to parent (same convention as tree-binary layout).
// Range annotations (node.meta with name "range") are rendered automatically
// by graph-render.ts — no extra logic needed here.
//
// Geometry constants match tree-layout.ts (vSpacing = 88, markerLane = 34)
// for visual consistency across the DSA book. hSpacing is 60 (narrower than
// the 70 used for BSTs) because segment trees can be wide at the leaf level.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const H_SPACE     = 60;   // horizontal space per leaf slot
const V_SPACE     = 88;   // vertical gap between levels — same as tree-layout.ts
const MARKER_LANE = 34;   // headroom above root for cursor caret
const PAD_X       = 24;
const PAD_Y       = 18;

/**
 * BFS-indexed complete binary tree layout. Node ids must be 1-based integer
 * strings ("1", "2", "3", …). Edges are child→parent (or omitted entirely —
 * the layout ignores edges and derives all positions from ids). The maximum
 * BFS index in the payload determines the tree height and leaf-column count.
 */
export const segmentTreeLayout: LayoutFn = (nodes: VizNode[], _edges: VizEdge[]): GraphLayout => {
  if (nodes.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + H_SPACE,
      height: PAD_Y * 2 + MARKER_LANE + V_SPACE,
    };
  }

  const bfsIds: number[] = [];
  for (const n of nodes) {
    const idx = parseInt(n.id, 10);
    if (!isNaN(idx) && idx >= 1) bfsIds.push(idx);
  }
  if (bfsIds.length === 0) {
    return {
      positions: new Map(),
      width: PAD_X * 2 + H_SPACE,
      height: PAD_Y * 2 + MARKER_LANE + V_SPACE,
    };
  }

  const maxId     = Math.max(...bfsIds);
  const maxLevel  = Math.floor(Math.log2(maxId));   // deepest level (0-based)
  const H         = maxLevel + 1;                    // total number of levels
  const leafSlots = 1 << maxLevel;                   // = 2^maxLevel leaf columns

  const positions = new Map<string, NodePos>();

  for (const n of nodes) {
    const idx = parseInt(n.id, 10);
    if (isNaN(idx) || idx < 1) continue;

    const level       = Math.floor(Math.log2(idx));
    const slotInLevel = idx - (1 << level);               // 0-based slot within the level
    const leafSpan    = 1 << (maxLevel - level);          // leaf columns covered by this node
    const leftLeaf    = slotInLevel * leafSpan;
    const centerSlot  = leftLeaf + (leafSpan - 1) / 2;   // fractional center column

    positions.set(n.id, {
      x: PAD_X + (centerSlot + 0.5) * H_SPACE,
      y: PAD_Y + MARKER_LANE + (level + 0.5) * V_SPACE,
    });
  }

  const width  = PAD_X * 2 + leafSlots * H_SPACE;
  const height = PAD_Y * 2 + MARKER_LANE + H * V_SPACE;

  return { positions, width, height };
};
