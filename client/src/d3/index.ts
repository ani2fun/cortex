// Single entry point for the trace-driven Visualise renderer (ADR-0018). The
// Scala.js AlgoLens modal calls `renderWidget(containerId, jsonStr, onStep?,
// caseIndex?)` and receives a `WidgetController` back. The controller's
// `setStep(n, animate)` method lets the Scala-side ControlsBar (powered by
// Stepper.hook) push step changes into the D3 canvas without re-mounting it.
//
// This module — and everything it imports — is plain TypeScript + D3. It has no
// knowledge of Scala, Scala.js, or React.
//
// Slice 3 (auto-dispatch): the adapter (`HeapToGraph.adapt`) annotates each
// VizNode with `cardId` + `layoutKind`. When ANY node carries a non-empty
// `layoutKind`, this module dispatches per-card via `renderMultiCard` — each
// heap-object's nodes render in their own sub-canvas inside one stacked column,
// each with its matching layout. When no node carries `layoutKind` (hand-curated
// payloads from `d3 widget=` fences, or a legacy adapter result), it falls back
// to the original single-layout path via `graph.layoutHint`.

import type { VizCases, VizGraph, VizGraphStep, VizNode } from "./types";
import type { LayoutFn } from "./tree-layout";
import {
  renderGraph,
  buildLegend,
  type WidgetController,
  type RenderGraphOptions,
} from "./graph-render";
import { treeLayout } from "./tree-layout";
import { graphLayout } from "./graph-layout";
import { arrayLayout } from "./array-layout";
import { linkedListLayout } from "./linked-list-layout";
import { gridLayout } from "./grid-layout";
import { hashmapLayout } from "./hashmap-layout";
import { trieLayout } from "./trie-layout";
import { stackLayout, queueLayout } from "./stack-queue-layout";
import { unionFindLayout } from "./union-find-layout";
import { segmentTreeLayout } from "./segment-tree-layout";
import { fenwickLayout } from "./fenwick-layout";
import { bitsetLayout } from "./bitset-layout";
import { skiplistLayout } from "./skiplist-layout";
import { stackRenderer } from "./stack-renderer";
import { hashmapRenderer } from "./hashmap-renderer";
import { graphRenderer } from "./graph-renderer";
import { queueRenderer } from "./queue-renderer";
import { heapRenderer } from "./heap-renderer";
import { unionFindRenderer } from "./union-find-renderer";
import { trieRenderer } from "./trie-renderer";
import { bitsetRenderer } from "./bitset-renderer";
import { linkedListRenderer } from "./linked-list-renderer";
import { segmentTreeRenderer } from "./segment-tree-renderer";
import { fenwickRenderer } from "./fenwick-renderer";
import { skiplistRenderer } from "./skiplist-renderer";
import { arrayRenderer } from "./array-renderer";
import { gridRenderer } from "./grid-renderer";
import { btreeRenderer } from "./btree-renderer";

export type { WidgetController };

// layoutKind / layoutHint → layout. Adding a data structure adds an entry here,
// never a renderer. The new shape-driven keys (`tree-binary`, `list-single`,
// `list-double`, `array-1d`, `array-2d`, `graph-generic`, `hashmap`) come from
// Slice 3 / Slice 12 auto-dispatch. The older keys (`binary-tree`, `linked-list`,
// `array`, `grid`, `graph`) stay registered so existing `d3 widget=` fences AND
// the per-trace `VizGraph.layoutHint` override continue to resolve. The generic
// `graph` layout doubles as the fallback for an unrecognised name.
const LAYOUTS: Record<string, LayoutFn> = {
  // Slice 3 / 12 — new per-object dispatch kinds:
  "tree-binary": treeLayout,
  "list-single": linkedListLayout,
  "list-double": linkedListLayout,
  "array-1d": arrayLayout,
  "array-2d": gridLayout,
  "graph-generic": graphLayout,
  hashmap: hashmapLayout,
  trie: trieLayout,
  stack: stackLayout,
  queue: queueLayout,
  "union-find": unionFindLayout,
  "segment-tree": segmentTreeLayout,
  fenwick: fenwickLayout,
  bitset: bitsetLayout,
  skiplist: skiplistLayout,
  // Legacy keys — kept for existing `d3 widget=` fence payloads and per-trace overrides:
  "binary-tree": treeLayout,
  "linked-list": linkedListLayout,
  array: arrayLayout,
  grid: gridLayout,
  graph: graphLayout,
};

/**
 * A bespoke renderer (ADR-0024). Same shape as `renderGraph` but wraps the layout's nodes/edges
 * in renderer-specific chrome — a Stack's "↑ TOP" caret + reversed-index column, a Queue's
 * head/tail callouts, a Heap's parent/child overlays, …. The renderer is free to call
 * `renderGraph` internally for the underlying layout work and decorate the result, OR draw
 * everything itself. Returns the same `WidgetController` so the modal's Stepper can drive it.
 *
 * Signature mirrors `renderGraph` so a bespoke renderer can delegate to it for the geometry +
 * just add chrome on top (the common case for slices 1–17).
 */
export type RendererFn = (
  container: HTMLElement,
  data: VizGraph,
  layout: LayoutFn,
  onStep?: (index: number) => void,
  options?: RenderGraphOptions,
) => WidgetController;

/**
 * `structureType` → bespoke renderer (ADR-0024). Empty today — slices 1–17 each add one entry
 * (slice 1: `stack`, slice 2: `hashmap`, …). When a `VizGraphStep.structureType` doesn't match,
 * `pickRenderer` falls back to the generic `renderGraph`, preserving today's behaviour for every
 * chapter that hasn't set `viz-kind=` yet.
 *
 * Renderers live in their own modules (`stack-renderer.ts`, `queue-renderer.ts`, …) and import
 * `RendererFn` from here; the modules are wired in via `import { stackRenderer } from "./stack-renderer"`
 * additions when each slice ships.
 */
const RENDERERS: Record<string, RendererFn> = {
  stack: stackRenderer,
  queue: queueRenderer,
  deque: queueRenderer, // a deque is the same head/tail visual, both ends active
  heap: heapRenderer, // array-backed, rendered as its implicit binary tree
  "union-find": unionFindRenderer, // parent-array → forest with parent arcs
  bitset: bitsetRenderer, // fixed-width bit array, set bits filled / clear muted
  "list-single": linkedListRenderer, // chain + null terminator
  "list-double": linkedListRenderer, // same; prev arrows lay out + terminate alike
  "segment-tree": segmentTreeRenderer, // range-bar overlay — bars over the array slice each covers
  fenwick: fenwickRenderer, // responsibility staircase — bars over the (i−lowbit, i] slice each owns
  skiplist: skiplistRenderer, // multi-level grid — towers per node, express lanes as upper rows
};

/**
 * layoutKind → DEFAULT bespoke renderer, used when a card carries no `viz-kind`
 * structureType. An `array-1d` card renders as the bespoke ArrayRenderer (a cell row with
 * pointer carets + an index row) instead of the generic circle layout — arrays are the
 * book's most common shape, so they get the cell treatment by default with no per-fence
 * opt-in. `array` is the legacy hand-curated `layoutHint` alias for the same kind.
 *
 * Linked lists get the same default treatment. The chapters annotate runnable blocks with
 * `viz=linked-list` (a KnownLayoutKind, so HeapToGraph stamps every node's layoutKind =
 * `linked-list`) but usually omit `viz-kind=list-single`, leaving structureType empty. Without
 * an entry here those cards fell through to the generic circle/force graph (scattered nodes,
 * crossing `next` arrows). Route every linked-list layout alias to the bespoke LinkedListRenderer
 * — it auto-detects singly vs doubly from the step's `prev` edges, so one entry covers both.
 */
const LAYOUT_RENDERERS: Record<string, RendererFn> = {
  "array-1d": arrayRenderer,
  array: arrayRenderer,
  "array-2d": gridRenderer,
  "linked-list": linkedListRenderer,
  "list-single": linkedListRenderer,
  "list-double": linkedListRenderer,
  // Slot/shape structures whose generic circle rendering HIDES the idea (a stack's
  // LIFO column, a bitset's set/clear contrast, a Fenwick staircase, …). A static
  // `d3 widget=<name>` fence carries no `structureType` (only runnable `viz-kind=`
  // blocks set one), so without an entry here these fences fell through to the
  // generic circle renderer — the catalog gallery and the segment-tree/fenwick
  // lesson fences rendered as plain circles instead of their bespoke widget. These
  // per-card renderers read only `slot` / `label` / `level`-meta, exactly what the
  // hand-authored payloads already carry, so routing the layoutKind to them needs
  // no payload change. (These layoutKinds are NEVER produced by the trace adapter —
  // they're not in HeapToGraph.KnownLayoutKinds and inferOneCard never returns them
  // — so this only upgrades static fences; runnable traces are untouched.)
  // segment-tree is the one exception: its renderer also needs `lo`/`hi` meta +
  // `left`/`right` edges, reconciled in the payloads (see segment-tree-renderer.ts).
  stack: stackRenderer,
  queue: queueRenderer,
  deque: queueRenderer,
  bitset: bitsetRenderer,
  skiplist: skiplistRenderer,
  fenwick: fenwickRenderer,
  "segment-tree": segmentTreeRenderer,
};

/**
 * WHOLE-GRAPH bespoke renderers (ADR-0029). Unlike `RENDERERS` (which fire
 * PER-CARD inside `renderMultiCard`), these own structures whose nodes connect
 * THROUGH a collection — a hashmap's `Dict → Arr → Entry`, a graph's adjacency
 * lists — which the per-card split shatters (cross-card edges are dropped by
 * `filterStepToCard`). They receive the FULL `VizGraph` and reconstruct the
 * structure from its nodes + edges, so they're dispatched BEFORE the multi-card
 * split. `structureType` (set by `viz-kind=` or inference) selects them.
 */
const WHOLE_GRAPH_RENDERERS: Record<string, RendererFn> = {
  hashmap: hashmapRenderer,
  graph: graphRenderer,
  trie: trieRenderer, // prefix tree: parent→child edges composed through the children Dict
  btree: btreeRenderer, // n-ary multi-key tree: nodes + child links both run THROUGH Arrs
};

/** The whole-graph renderer for a graph's structureType, or null if none matches. */
function pickWholeGraphRenderer(data: VizGraph): RendererFn | null {
  for (const step of data.steps) {
    const kind = step.structureType;
    if (typeof kind === "string" && kind.length > 0) {
      return WHOLE_GRAPH_RENDERERS[kind] ?? null;
    }
  }
  // A 2-D array (list-of-lists) is split into per-row cards by the adapter's card
  // grouping (each Arr is its own card), so the matrix can't be drawn per-card — it
  // must see every cell at once. Render it WHOLE-GRAPH (like hashmap / graph) when any
  // node is tagged `array-2d`; gridRenderer reconstructs the grid from nodes + edges.
  for (const step of data.steps) {
    if (step.nodes.some((n) => n.layoutKind === "array-2d")) return gridRenderer;
  }
  return null;
}

/**
 * Pick the renderer for a `VizGraph`'s structureType, falling back to the generic graph renderer
 * when no bespoke entry matches. structureType is read from the first step that carries one (set
 * by `HeapToGraph.adapt`, stable across steps for one segment). When no step carries one — every
 * chapter that hasn't opted into `viz-kind=` yet — `RENDERERS` lookup misses and the generic
 * renderer takes over, so today's behaviour is preserved bit-for-bit.
 */
function pickRenderer(data: VizGraph, layoutKind = ""): RendererFn {
  for (const step of data.steps) {
    const kind = step.structureType;
    if (typeof kind === "string" && kind.length > 0) {
      const r = RENDERERS[kind];
      if (r !== undefined) return r;
      break;
    }
  }
  // No bespoke `viz-kind` structureType — fall back to a layoutKind default (an `array-1d`
  // card gets the bespoke ArrayRenderer cell row), then the generic graph renderer.
  return LAYOUT_RENDERERS[layoutKind] ?? renderGraph;
}

/**
 * Render a `VizCases` payload into the DOM element with id `containerId`. A DSA
 * `main` usually runs several test cases; `caseIndex` (default 0) selects which
 * one to draw — the modal owns the "Case N / M" selector and re-invokes this
 * with a new index on a switch. `onStep` (if given) fires with the step index
 * whenever `setStep` is called, so a host can keep a code pane in sync.
 *
 * **Dispatch:** if any node in any step carries a non-empty `layoutKind`, the
 * trace was auto-dispatched by `HeapToGraph.adapt` (Slice 3) — `renderMultiCard`
 * splits the canvas into one sub-card per heap-object group. Otherwise (legacy
 * payloads, hand-curated `d3 widget=` JSON) the renderer falls back to the
 * single-layout path via `graph.layoutHint` (unrecognised hint → `graph-generic`).
 *
 * Returns a `WidgetController` — store it and call `setStep(n, animate)` to
 * advance the diagram. The play loop is owned by the caller (Scala Stepper.hook).
 * Returns `null` on error (container not found, parse failure, no cases).
 */
export function renderWidget(
  containerId: string,
  jsonStr: string,
  onStep?: (index: number) => void,
  caseIndex?: number,
  onHover?: (name: string) => void,
): WidgetController | null {
  const container = document.getElementById(containerId);
  if (container === null) {
    console.error(`[d3] renderWidget: container #${containerId} not found`);
    return null;
  }

  let data: VizCases;
  try {
    data = JSON.parse(jsonStr) as VizCases;
  } catch (err) {
    container.textContent = "Widget error: could not parse visualization data.";
    console.error("[d3] renderWidget: JSON parse failed", err);
    return null;
  }

  const cases = data.cases ?? [];
  if (cases.length === 0) {
    container.textContent = "Widget error: no test cases to visualize.";
    return null;
  }

  const idx = Math.max(0, Math.min(cases.length - 1, caseIndex ?? 0));
  const graph = cases[idx];

  // ADR-0029 — whole-graph bespoke renderers (hashmap, …) take precedence over
  // the multi-card split: they draw the cross-collection structure the per-card
  // path can't. structureType (from `viz-kind=`) selects them.
  const wholeGraphRenderer = pickWholeGraphRenderer(graph);
  if (wholeGraphRenderer !== null) {
    const layout = LAYOUTS[graph.layoutHint] ?? graphLayout;
    return wholeGraphRenderer(container, graph, layout, onStep, { onHover });
  }

  if (hasLayoutKindAnnotations(graph)) {
    return renderMultiCard(container, graph, LAYOUTS, onStep, onHover);
  }
  // ADR-0024 — pick a bespoke renderer (Stack / Queue / Heap / …) if the graph's structureType
  // matches one in RENDERERS; otherwise fall through to the generic graph renderer. Either way
  // the layout (geometry) is chosen by `graph.layoutHint`; the renderer wraps it in chrome.
  const layout = LAYOUTS[graph.layoutHint] ?? graphLayout;
  const renderer = pickRenderer(graph, graph.layoutHint);
  return renderer(container, graph, layout, onStep, { onHover });
}

/** True when at least one node in at least one step carries a per-object `layoutKind`. */
function hasLayoutKindAnnotations(graph: VizGraph): boolean {
  for (const step of graph.steps) {
    for (const node of step.nodes) {
      if (node.layoutKind !== undefined && node.layoutKind !== "") return true;
    }
  }
  return false;
}

/**
 * Multi-card renderer (Slice 3 auto-dispatch). Splits a `VizGraph` into one
 * sub-canvas per `cardId` (the heap-object grouping key set by `HeapToGraph`),
 * each rendered with its own layout. Cards stack in a single column ordered by
 * `cardId` ascending — `HeapToGraph` picks the lexicographically-smallest
 * heap-object id as each group's representative, so the order is deterministic
 * and stable across steps. Title, caption, truncation notice, and legend are
 * shared across all cards; each card's sub-renderer runs in `chrome: false` mode.
 * `setStep` fans out to every sub-controller and updates the shared caption.
 */
function renderMultiCard(
  container: HTMLElement,
  data: VizGraph,
  layouts: Record<string, LayoutFn>,
  onStep?: (index: number) => void,
  onHover?: (name: string) => void,
): WidgetController {
  // First step's node carries the layoutKind we use for the card. A given cardId
  // is stable across steps (the adapter assigns it deterministically), so picking
  // the first occurrence is enough.
  const cardKinds = new Map<string, string>();
  for (const step of data.steps) {
    for (const node of step.nodes) {
      const cid = node.cardId;
      if (cid !== "" && !cardKinds.has(cid)) {
        const kind = node.layoutKind || data.layoutHint || "graph-generic";
        cardKinds.set(cid, kind);
      }
    }
  }

  container.innerHTML = "";
  const root = document.createElement("div");
  root.className = "viz-graph not-prose";

  if (data.title) {
    const titleEl = document.createElement("p");
    titleEl.className = "viz-graph__title";
    titleEl.textContent = data.title;
    root.appendChild(titleEl);
  }

  const cardsContainer = document.createElement("div");
  cardsContainer.className = "viz-graph__cards";
  root.appendChild(cardsContainer);

  const caption = document.createElement("p");
  caption.className = "viz-graph__caption";
  caption.setAttribute("aria-live", "polite");
  root.appendChild(caption);

  if (data.truncated) {
    const notice = document.createElement("p");
    notice.className = "viz-graph__notice";
    notice.textContent = "Trace truncated — showing the first part of the run.";
    root.appendChild(notice);
  }

  root.appendChild(buildLegend(data));
  container.appendChild(root);

  // No groups at all (empty trace, defensive): leave just the chrome.
  if (cardKinds.size === 0) {
    if (data.steps.length > 0) caption.textContent = data.steps[0].annotation.title;
    return {
      setStep(n: number, _animate: boolean): void {
        const idx = Math.max(0, Math.min(data.steps.length - 1, n));
        if (idx < data.steps.length)
          caption.textContent = data.steps[idx].annotation.title;
        if (onStep !== undefined) onStep(idx);
      },
      setHover(_name: string): void {},
      getStepCount(): number {
        return data.steps.length;
      },
      destroy(): void {},
    };
  }

  const sortedCardIds = [...cardKinds.keys()].sort();
  const subControllers: WidgetController[] = [];

  for (const cid of sortedCardIds) {
    const cardEl = document.createElement("div");
    cardEl.className = "viz-card";
    cardEl.setAttribute("data-card-id", cid);
    cardsContainer.appendChild(cardEl);

    const subSteps: VizGraphStep[] = data.steps.map((step) =>
      filterStepToCard(step, cid),
    );
    const subGraph: VizGraph = { ...data, steps: subSteps };
    const kind = cardKinds.get(cid) ?? "graph-generic";
    const layoutFn = layouts[kind] ?? graphLayout;

    // ADR-0024 — per-card bespoke renderer dispatch. structureType is stamped
    // per step (stable across steps for one segment), so `pickRenderer(subGraph)`
    // returns the bespoke renderer when set + matched; otherwise it falls back
    // to `renderGraph`. Identical contract to the single-card path above.
    const subRenderer = pickRenderer(subGraph, kind);
    const sub = subRenderer(cardEl, subGraph, layoutFn, undefined, {
      chrome: false,
      onHover,
    });
    subControllers.push(sub);
  }

  if (data.steps.length > 0) caption.textContent = data.steps[0].annotation.title;
  if (onStep !== undefined) onStep(0);

  return {
    setStep(n: number, animate: boolean): void {
      const idx = Math.max(0, Math.min(data.steps.length - 1, n));
      for (const sub of subControllers) sub.setStep(idx, animate);
      if (idx < data.steps.length)
        caption.textContent = data.steps[idx].annotation.title;
      if (onStep !== undefined) onStep(idx);
    },
    // Slice 5 — fan the hover key out to every sub-renderer; cursor-marks in
    // any card whose names include `name` light up.
    setHover(name: string): void {
      for (const sub of subControllers) sub.setHover(name);
    },
    getStepCount(): number {
      return data.steps.length;
    },
    destroy(): void {
      for (const sub of subControllers) sub.destroy();
    },
  };
}

/**
 * Filter one step's nodes, edges, cursors, and per-node diff sets to a single `cardId`.
 * Edges whose endpoints don't both live in this card are dropped; cursors targeting
 * nodes outside this card are dropped; highlight / changed / removed ids are intersected
 * with the surviving node-id set so each sub-renderer's diff stays internally consistent.
 */
function filterStepToCard(step: VizGraphStep, cardId: string): VizGraphStep {
  const nodes = step.nodes.filter((n: VizNode) => n.cardId === cardId);
  const ids = new Set(nodes.map((n) => n.id));
  return {
    ...step,
    nodes,
    edges: step.edges.filter((e) => ids.has(e.from) && ids.has(e.to)),
    cursor: step.cursor.filter((c) => ids.has(c.target)),
    highlight: step.highlight.filter((id) => ids.has(id)),
    changed: step.changed.filter((id) => ids.has(id)),
    removed: step.removed.filter((id) => ids.has(id)),
  };
}
