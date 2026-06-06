// Binary-tree layout — one pluggable layout for the generic graph renderer (ADR-0018).
//
// Builds a tree from edges labelled `left` / `right`, finds the root (the node with no
// incoming edge), and runs the recursive subtree-width slot algorithm so skewed trees
// cascade diagonally rather than overlapping. A new data structure adds a `LayoutFn`
// like this one — never a new renderer. No D3, no DOM, no Scala.

import type { VizNode, VizEdge } from "./types";

export interface NodePos {
  x: number;
  y: number;
}

export interface GraphLayout {
  positions: Map<string, NodePos>;
  width: number;
  height: number;
  /** Optional per-edge SVG path overrides — keyed by `"fromId->toId"`. When present,
   *  the graph renderer uses these paths instead of computing straight lines. Used by
   *  the union-find layout to render curved parent arcs above the node row. */
  edgePaths?: Map<string, string>;
}

/** Places the union of every node/edge in the animation; the renderer reuses the result per step. */
export type LayoutFn = (nodes: VizNode[], edges: VizEdge[]) => GraphLayout;

const LAYOUT = {
  hSpacing: 70, // horizontal slot width per leaf
  vSpacing: 88, // vertical gap between levels — roomy enough for per-node meta sub-labels
  markerLane: 34, // headroom above the top row — fits the cursor caret
  paddingX: 20,
  paddingY: 18,
};

export const treeLayout: LayoutFn = (nodes, edges) => {
  const left = new Map<string, string>();
  const right = new Map<string, string>();
  for (const e of edges) {
    const lbl = e.label.toLowerCase();
    if (lbl === "right" || lbl === "r") right.set(e.from, e.to);
    else left.set(e.from, e.to);
  }
  const ids = new Set(nodes.map((n) => n.id));
  const incoming = new Set(edges.map((e) => e.to));
  const roots = nodes.filter((n) => !incoming.has(n.id));
  const rootId = roots.length > 0 ? roots[0].id : nodes.length > 0 ? nodes[0].id : null;

  const slot = new Map<string, number>();
  const depthOf = new Map<string, number>();
  const visited = new Set<string>();
  let nextSlot = 0;
  let maxDepth = 0;

  function visit(id: string, d: number): void {
    if (!ids.has(id) || visited.has(id)) return;
    visited.add(id);
    if (d > maxDepth) maxDepth = d;
    const l = left.get(id);
    const r = right.get(id);
    if (l !== undefined) visit(l, d + 1);
    let selfSlot: number;
    if (l !== undefined && r !== undefined) {
      const leftEnd = slot.get(l) ?? nextSlot;
      nextSlot += 1;
      visit(r, d + 1);
      const rightStart = slot.get(r) ?? nextSlot;
      selfSlot = (leftEnd + rightStart) / 2;
    } else if (l !== undefined) {
      selfSlot = nextSlot;
      nextSlot += 1;
    } else if (r !== undefined) {
      const mySlot = nextSlot;
      nextSlot += 1;
      visit(r, d + 1);
      const rightStart = slot.get(r) ?? nextSlot;
      selfSlot = (mySlot + rightStart) / 2;
    } else {
      selfSlot = nextSlot;
      nextSlot += 1;
    }
    slot.set(id, selfSlot);
    depthOf.set(id, d);
  }

  if (rootId !== null) visit(rootId, 0);
  // Defensive: any node not reachable from the root (a disconnected fragment) gets its own slot.
  for (const n of nodes) {
    if (!visited.has(n.id)) {
      slot.set(n.id, nextSlot);
      depthOf.set(n.id, 0);
      nextSlot += 1;
    }
  }

  let slotCount = 1;
  for (const s of slot.values()) slotCount = Math.max(slotCount, s + 1);

  const positions = new Map<string, NodePos>();
  for (const n of nodes) {
    const s = slot.get(n.id) ?? 0;
    const d = depthOf.get(n.id) ?? 0;
    positions.set(n.id, {
      x: LAYOUT.paddingX + (s + 0.5) * LAYOUT.hSpacing,
      y: LAYOUT.paddingY + LAYOUT.markerLane + (d + 0.5) * LAYOUT.vSpacing,
    });
  }
  const width = LAYOUT.paddingX * 2 + Math.max(slotCount, 1) * LAYOUT.hSpacing;
  const height = LAYOUT.paddingY * 2 + LAYOUT.markerLane + (maxDepth + 1) * LAYOUT.vSpacing;
  return { positions, width, height };
};
