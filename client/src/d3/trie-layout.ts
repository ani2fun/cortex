// N-ary trie layout for the trace-driven Visualise renderer (ADR-0018).
// Positions nodes in a hierarchical top-down tree using the same Reingold-Tilford-style
// leaf-slot algorithm as tree-layout.ts, but without left/right edge discrimination —
// all edges are parent→child and every node may have N children.
//
// Terminal nodes (kind === "terminal") carry a double-ring marker in the renderer;
// the layout treats them identically to regular nodes for positioning purposes.
//
// Edge labels carry the single character (or short string for radix trie variants)
// written on each trie edge — the generic graph renderer already renders these as
// midpoint text labels.

import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

const LAYOUT = {
  hSpacing: 72,    // horizontal slot width per leaf
  vSpacing: 80,    // vertical gap between levels
  markerLane: 34,  // headroom above the top row for cursor carets
  paddingX: 20,
  paddingY: 18,
};

export const trieLayout: LayoutFn = (nodes, edges) => {
  if (nodes.length === 0) return emptyLayout();

  // Build children map: parent → ordered list of children (edge-declaration order).
  const children = new Map<string, string[]>();
  const hasIncoming = new Set<string>();
  for (const e of edges) {
    const list = children.get(e.from);
    if (list !== undefined) {
      if (!list.includes(e.to)) list.push(e.to);
    } else {
      children.set(e.from, [e.to]);
    }
    hasIncoming.add(e.to);
  }

  const ids = new Set(nodes.map((n) => n.id));
  // Root = node with no incoming edges that is present in this step's nodes.
  const roots = nodes.filter((n) => !hasIncoming.has(n.id));
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

    const ch = children.get(id) ?? [];
    if (ch.length === 0) {
      // Leaf — takes the next free slot.
      slot.set(id, nextSlot);
      depthOf.set(id, d);
      nextSlot += 1;
      return;
    }

    // Internal node — recurse into children first, then sit at their midpoint.
    for (const c of ch) visit(c, d + 1);
    const firstSlot = slot.get(ch[0]) ?? nextSlot;
    const lastSlot = slot.get(ch[ch.length - 1]) ?? nextSlot;
    slot.set(id, (firstSlot + lastSlot) / 2);
    depthOf.set(id, d);
  }

  if (rootId !== null) visit(rootId, 0);
  // Defensive: disconnected nodes (not reachable from root) each get their own slot.
  for (const n of nodes) {
    if (!visited.has(n.id)) {
      slot.set(n.id, nextSlot);
      depthOf.set(n.id, 0);
      nextSlot += 1;
    }
  }

  let slotCount = 1;
  for (const s of slot.values()) slotCount = Math.max(slotCount, Math.ceil(s) + 1);

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

function emptyLayout(): GraphLayout {
  return {
    positions: new Map(),
    width: LAYOUT.paddingX * 2 + LAYOUT.hSpacing,
    height: LAYOUT.paddingY * 2 + LAYOUT.markerLane + LAYOUT.vSpacing,
  };
}
