// Bespoke Trie renderer (ADR-0024 / ADR-0027, renderer #7 — cross-structure).
//
// A trie node is an `Instance(children: Ref→Dict[char → Ref→TrieNode], is_end)`,
// so the adapter routes each child through a Dict entry — a two-hop path:
//   parent --"children"--> entry(key = char) --"char"--> child
// The per-card path shatters that into edge-less TrieNode + Dict-entry cards
// (ADR-0025); even drawn whole, the entry nodes (label "·") are noise sitting
// two hops away from the real parent→child relationship.
//
// This WHOLE-GRAPH renderer COMPOSES direct parent→child edges through the
// entries (folding the Dict scaffolding away), labels each node with the
// CHARACTER on its incoming edge — so reading the nodes along a path spells the
// word, and the root is "•" — marks every `is_end` node terminal (a double-ring
// via `kind === "terminal"`), and delegates to the generic `renderGraph` +
// `trieLayout`, reusing all the SVG node/edge drawing. Same shape as the graph
// renderer, but composing through a Dict instead of an adjacency Arr.

import { renderGraph, type WidgetController, type RenderGraphOptions } from "./graph-render";
import { trieLayout } from "./trie-layout";
import type { LayoutFn } from "./tree-layout";
import type { RendererFn } from "./index";
import type { VizGraph, VizGraphStep, VizEdge, VizNode } from "./types";

const ROOT_LABEL = "•"; // the empty-string root node — the start of every word

/** A node's `meta` value for `name`, or null. */
function metaValue(node: VizNode, name: string): string | null {
  const f = node.meta.find((m) => m.name === name);
  return f ? f.value : null;
}

/** Compose parent→child trie edges through the children-Dict entries; relabel by char, mark terminals. */
function synthesizeStep(step: VizGraphStep): VizGraphStep {
  const entryIds = new Set(step.nodes.filter((n) => n.kind === "entry").map((n) => n.id));

  // Per Dict entry: its parent (the owning TrieNode, via the "children" in-edge)
  // and the (child, char) it maps to (via its single out-edge).
  const entryParent = new Map<string, string>();
  const entryChild = new Map<string, string>();
  const entryChar = new Map<string, string>();
  for (const e of step.edges) {
    if (entryIds.has(e.to)) entryParent.set(e.to, e.from); // parent --children--> entry
    if (entryIds.has(e.from)) {
      // entry --char--> child
      entryChild.set(e.from, e.to);
      entryChar.set(e.from, e.label);
    }
  }

  // The char on the edge leading INTO each child node — that node's identity.
  const charInto = new Map<string, string>();
  const direct: VizEdge[] = [];
  for (const eid of entryIds) {
    const parent = entryParent.get(eid);
    const child = entryChild.get(eid);
    if (parent === undefined || child === undefined) continue;
    charInto.set(child, entryChar.get(eid) ?? "");
    direct.push({ from: parent, to: child, label: "" });
  }

  const nodes: VizNode[] = step.nodes
    .filter((n) => n.kind !== "entry")
    .map((n) => {
      const isEnd = metaValue(n, "is_end") === "true";
      return {
        ...n,
        label: charInto.get(n.id) ?? ROOT_LABEL,
        kind: isEnd ? "terminal" : "trie",
        meta: [], // `is_end` is now the terminal ring; drop the raw flag sub-label
      };
    });
  const keepIds = new Set(nodes.map((n) => n.id));

  return {
    ...step,
    nodes,
    edges: direct.filter((e) => keepIds.has(e.from) && keepIds.has(e.to)),
    cursor: step.cursor.filter((c) => keepIds.has(c.target)),
    highlight: step.highlight.filter((id) => keepIds.has(id)),
    changed: step.changed.filter((id) => keepIds.has(id)),
    removed: step.removed.filter((id) => keepIds.has(id)),
  };
}

function synthesize(data: VizGraph): VizGraph {
  return { ...data, steps: data.steps.map(synthesizeStep) };
}

export const trieRenderer: RendererFn = (
  container: HTMLElement,
  data: VizGraph,
  _layout: LayoutFn,
  onStep?: (index: number) => void,
  options?: RenderGraphOptions,
): WidgetController => renderGraph(container, synthesize(data), trieLayout, onStep, options);
