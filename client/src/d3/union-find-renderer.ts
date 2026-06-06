// Bespoke Union-Find / Disjoint-Set-Union renderer (ADR-0024 / ADR-0027,
// renderer #6) — parent-array → forest.
//
// A DSU is stored as a `parent` array: `parent[i]` is element i's parent, and
// `parent[i] === i` marks i as a set representative (a root). The generic path
// renders this as a flat `array-1d` row of parent indices — which hides the two
// things that make it a union-find: WHICH set each element belongs to (the
// forest), and the path-compression that flattens those trees over time.
//
// This renderer reads each cell's value as a PARENT POINTER, relabels the cell
// to its own slot (the element's identity, 0..n-1), synthesises a child→parent
// edge for every non-root element and a self-edge for every root, and delegates
// to the generic `renderGraph` + `unionFindLayout` — a horizontal row of circles
// with parent arcs curving above the row and a small self-loop on each
// representative. Same per-card delegate pattern as the heap renderer, but the
// edges encode parent pointers (not array-index tree positions) and the label is
// the index (not the stored value, which is the parent we draw as an arc).
//
// Path compression reads beautifully: when `find` reparents a node, its stored
// value changes, so the adapter marks the cell `changed` and this renderer
// re-keys its edge to the new parent — the old arc fades out, the shorter one
// fades in.

import { renderGraph, type WidgetController, type RenderGraphOptions } from "./graph-render";
import { unionFindLayout } from "./union-find-layout";
import type { LayoutFn } from "./tree-layout";
import type { RendererFn } from "./index";
import type { VizGraph, VizGraphStep, VizEdge, VizNode } from "./types";

/**
 * Read each cell as a parent pointer: relabel it to its own slot (the element
 * id), and synthesise a child→parent edge per non-root cell / a self-edge per
 * root. `unionFindLayout` draws the arcs from `edgePaths`; a self-edge or a
 * `kind === "root"` node becomes the representative's self-loop.
 */
function synthesizeStep(step: VizGraphStep): VizGraphStep {
  const bySlot = new Map<number, string>();
  for (const n of step.nodes) {
    if (n.slot !== null) bySlot.set(n.slot, n.id);
  }

  const edges: VizEdge[] = [];
  const nodes: VizNode[] = step.nodes.map((n) => {
    if (n.slot === null) return n; // defensive: a stray instance ref — leave it be
    const parent = parseInt(n.label, 10);
    const isRoot = parent === n.slot;
    if (isRoot) {
      // Self-edge → unionFindLayout draws the representative's self-loop.
      edges.push({ from: n.id, to: n.id, label: "" });
    } else {
      const parentId = bySlot.get(parent);
      if (parentId !== undefined) edges.push({ from: n.id, to: parentId, label: "" });
    }
    // The circle shows the element's OWN index; the parent is the arc. Roots get
    // kind="root" so they read as representatives (the `root` sub-label + the
    // layout's self-loop branch); everything else keeps its `cell` kind.
    return { ...n, label: String(n.slot), kind: isRoot ? "root" : n.kind };
  });

  return { ...step, nodes, edges };
}

export const unionFindRenderer: RendererFn = (
  container: HTMLElement,
  data: VizGraph,
  _layout: LayoutFn,
  onStep?: (index: number) => void,
  options?: RenderGraphOptions,
): WidgetController =>
  renderGraph(
    container,
    { ...data, steps: data.steps.map(synthesizeStep) },
    unionFindLayout,
    onStep,
    options,
  );
