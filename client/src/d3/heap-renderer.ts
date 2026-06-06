// Bespoke Heap renderer (ADR-0024 / ADR-0027, renderer #5) — array→tree.
//
// A binary heap is stored as an Arr, so the generic path renders it as a flat
// `array-1d` row — which hides the very thing that makes it a heap: the implicit
// binary tree (parent of `i` is `(i-1)/2`, children are `2i+1` / `2i+2`) and the
// heap property between a parent and its children.
//
// This renderer SYNTHESISES the tree edges from the array indices
// (`cell[i] → cell[2i+1]` left, `cell[i] → cell[2i+2]` right) and delegates to
// the generic `renderGraph` + `treeLayout` — the same reuse pattern as the graph
// renderer, but per-card (a heap is a single Arr card) and with the binary-tree
// layout. The cells keep their values, so a min/max-heap reads as a tree whose
// parents dominate their children.

import { renderGraph, type WidgetController, type RenderGraphOptions } from "./graph-render";
import { treeLayout, type LayoutFn } from "./tree-layout";
import type { RendererFn } from "./index";
import type { VizGraph, VizGraphStep, VizEdge } from "./types";

/** Add binary-tree edges derived from array indices; keep everything else. */
function synthesizeStep(step: VizGraphStep): VizGraphStep {
  const bySlot = new Map<number, string>();
  for (const n of step.nodes) {
    if (n.slot !== null) bySlot.set(n.slot, n.id);
  }
  const edges: VizEdge[] = [];
  for (const [slot, id] of bySlot) {
    const l = bySlot.get(2 * slot + 1);
    if (l !== undefined) edges.push({ from: id, to: l, label: "left" });
    const r = bySlot.get(2 * slot + 2);
    if (r !== undefined) edges.push({ from: id, to: r, label: "right" });
  }
  return { ...step, edges };
}

export const heapRenderer: RendererFn = (
  container: HTMLElement,
  data: VizGraph,
  _layout: LayoutFn,
  onStep?: (index: number) => void,
  options?: RenderGraphOptions,
): WidgetController =>
  renderGraph(
    container,
    { ...data, steps: data.steps.map(synthesizeStep) },
    treeLayout,
    onStep,
    options,
  );
