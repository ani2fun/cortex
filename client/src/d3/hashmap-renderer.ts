// Bespoke HashMap renderer (ADR-0024 / ADR-0029, renderer #2 — separate-chaining).
//
// This is the renderer that forced the SDK to handle CROSS-STRUCTURE shapes
// (ADR-0025 audit finding). A hashmap's nodes connect THROUGH collections —
// `Dict(index → Ref→Arr[Ref→Entry])` — so the generic multi-card path shatters
// it into edge-less cards (Dict card, bucket-Arr cards, Entry-chain cards) with
// no visible bucket→chain relationship. This renderer is dispatched as a
// WHOLE-GRAPH renderer (see index.ts `WHOLE_GRAPH_RENDERERS`): it receives the
// FULL VizGraph and reconstructs the hashmap from the nodes + edges, drawing
// each bucket index next to its chain of `key: value` entries.
//
// Reconstruction per step (from the generic adapter output):
//   - Dict entries  → buckets.  kind === "entry"; meta `key` = the bucket index;
//     a Dict-entry's out-edges point at its bucket Arr's CELLS.
//   - bucket cells  → kind === "cell"; ordered by `slot`; each cell's out-edge
//     points at the Entry instance it holds.
//   - Entry nodes   → the chain members. label = the entry's value; meta `key` =
//     the entry's key. Rendered as `key: value`.
// The intermediary cell nodes (label "·") are implementation detail and are not
// drawn — only the index and the entry chain, which is the pedagogical shape.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

interface Bucket {
  index: string; // display label, e.g. "0"
  indexNum: number; // numeric for ordering (NaN sorts last)
  entries: VizNode[]; // chain order
}

/** A node's `meta` value for `name`, or null. */
function metaValue(node: VizNode, name: string): string | null {
  const f = node.meta.find((m) => m.name === name);
  return f ? f.value : null;
}

/** Reconstruct the buckets (index + ordered entry chain) from one step's nodes + edges. */
function reconstruct(step: VizGraphStep): Bucket[] {
  const nodeById = new Map<string, VizNode>(step.nodes.map((n) => [n.id, n]));
  const outTargets = (id: string): string[] =>
    step.edges.filter((e) => e.from === id).map((e) => e.to);

  const dictEntries = step.nodes.filter((n) => n.kind === "entry");
  const buckets: Bucket[] = dictEntries.map((de) => {
    const index = metaValue(de, "key") ?? de.label;
    // Dict-entry → bucket cells (sorted by slot) → Entry instances.
    const cells = outTargets(de.id)
      .map((id) => nodeById.get(id))
      .filter((n): n is VizNode => n !== undefined && n.slot !== null)
      .sort((a, b) => (a.slot as number) - (b.slot as number));
    const entries = cells.flatMap((cell) =>
      outTargets(cell.id)
        .map((id) => nodeById.get(id))
        .filter((n): n is VizNode => n !== undefined),
    );
    return { index, indexNum: Number(index), entries };
  });

  buckets.sort((a, b) => {
    if (Number.isNaN(a.indexNum) && Number.isNaN(b.indexNum)) return a.index.localeCompare(b.index);
    if (Number.isNaN(a.indexNum)) return 1;
    if (Number.isNaN(b.indexNum)) return -1;
    return a.indexNum - b.indexNum;
  });
  return buckets;
}

const ENTRY_NEW_COLOR = "#4f5bd5"; // indigo, matches the canon "new this step" hue

export const hashmapRenderer: RendererFn = defineRenderer({
  className: "hashmap-renderer",
  build({ content }) {
    return {
      onStep(step: VizGraphStep): void {
        const buckets = reconstruct(step);
        const highlights = new Set(step.highlight);
        const changed = new Set(step.changed);

        content.innerHTML = "";

        if (buckets.length === 0) {
          const empty = document.createElement("div");
          empty.className = "hashmap-renderer__empty";
          empty.textContent = "(empty map)";
          content.appendChild(empty);
          return;
        }

        buckets.forEach((bucket) => {
          const row = document.createElement("div");
          row.className = "hashmap-renderer__bucket";

          const idx = document.createElement("span");
          idx.className = "hashmap-renderer__index";
          idx.textContent = bucket.index;
          row.appendChild(idx);

          const chain = document.createElement("div");
          chain.className = "hashmap-renderer__chain";

          if (bucket.entries.length === 0) {
            const dash = document.createElement("span");
            dash.className = "hashmap-renderer__connector";
            dash.textContent = "∅";
            chain.appendChild(dash);
          }

          bucket.entries.forEach((entry, i) => {
            if (i > 0) {
              const conn = document.createElement("span");
              conn.className = "hashmap-renderer__connector";
              conn.textContent = "→";
              chain.appendChild(conn);
            }
            const box = document.createElement("div");
            box.className = "hashmap-renderer__entry";
            if (highlights.has(entry.id)) {
              box.classList.add("hashmap-renderer__entry--new");
              box.style.setProperty("--entry-new-color", ENTRY_NEW_COLOR);
            }
            if (changed.has(entry.id)) box.classList.add("hashmap-renderer__entry--changed");
            box.setAttribute("data-node-id", entry.id);

            const key = metaValue(entry, "key");
            if (key !== null) {
              const keyEl = document.createElement("span");
              keyEl.className = "hashmap-renderer__entry-key";
              keyEl.textContent = key;
              box.appendChild(keyEl);
              const sep = document.createElement("span");
              sep.className = "hashmap-renderer__entry-sep";
              sep.textContent = ":";
              box.appendChild(sep);
            }
            const val = document.createElement("span");
            val.className = "hashmap-renderer__entry-value";
            val.textContent = entry.label;
            box.appendChild(val);

            chain.appendChild(box);
          });

          row.appendChild(chain);
          content.appendChild(row);
        });
      },
    };
  },
});
