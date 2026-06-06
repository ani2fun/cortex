// Bespoke Skip-list renderer (ADR-0024 / ADR-0027, renderer #12 — the final shape) —
// the multi-level GRID. Per-card bespoke DOM on the Renderer SDK (like segment-tree /
// fenwick), matching the Claude Design handoff's SkipListRenderer (advanced-renderers.jsx).
//
// A skip list is a sorted level-0 chain where each node also rises into a random number
// of express lanes: node `i` reaches level `level[i]` (its top lane). The generic list
// path would draw a flat chain — which hides the express lanes that make lookups
// O(log n). This renderer draws the design's GRID: one row per level (top lane first),
// one column per node (ordered by key), each node a cell in every row it reaches and a
// thin gap line elsewhere so columns stay aligned; `head` + `∅` sentinels bookend each
// row. The `cur` search pointer's whole column is emphasised in its role colour as it
// hops the lanes.
//
// Reads each SkipNode's value (label) + `level` (meta, top lane reached, 0-indexed) and
// orders columns by value (the level-0 chain order). The SkipNode `next` chain unions
// into one card; this renderer ignores the delegated layout + the `next` edges and
// positions the grid itself (so the graph-layout step-union caveat does not apply). This
// is the bespoke trace-driven sibling of the delegate-circle `skiplist-layout.ts`, which
// backs the hand-authored inline `d3 widget=skiplist` fence path.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

/** Read a numeric `meta` field by name, or null if absent / non-numeric. */
function metaNum(n: VizNode, name: string): number | null {
  const f = n.meta.find((m) => m.name === name);
  if (f === undefined) return null;
  const v = Number(f.value);
  return Number.isFinite(v) ? v : null;
}

export const skiplistRenderer: RendererFn = defineRenderer({
  className: "skiplist-renderer",
  build({ content }) {
    return {
      onStep(step: VizGraphStep): void {
        // Each SkipNode: value (label) + `level` = the top express lane it reaches.
        type Col = { node: VizNode; value: number; level: number };
        const cols: Col[] = [];
        for (const node of step.nodes) {
          const level = metaNum(node, "level");
          if (level === null) continue; // defensive: not a skip node
          const vf = node.meta.find((m) => m.name === "value");
          cols.push({ node, value: Number(vf?.value ?? node.label), level });
        }
        // Columns left→right in sorted key order (the level-0 chain order).
        cols.sort((a, b) => a.value - b.value);

        content.innerHTML = "";
        if (cols.length === 0) {
          const empty = document.createElement("div");
          empty.className = "skiplist-renderer__empty";
          empty.textContent = "(empty)";
          content.appendChild(empty);
          return;
        }

        const numLevels = Math.max(...cols.map((c) => c.level)) + 1;

        // The `cur` search pointer's node this step — its WHOLE column is emphasised
        // (one cursor; `head` is the reachability anchor, not drawn as active). Tint
        // from the emitted role colour (MarkerColors canon, stamped by the adapter) —
        // one palette across every structure (ADR-0016).
        const curCursors = step.cursor.filter((c) => c.name === "cur");
        const activeIds = new Set(curCursors.map((c) => c.target));
        const activeColor = curCursors[0]?.color ?? "";

        // One row per level, TOP express lane first (level numLevels-1 down to 0).
        for (let lvl = numLevels - 1; lvl >= 0; lvl--) {
          const row = document.createElement("div");
          row.className = "skiplist-renderer__row";

          const label = document.createElement("span");
          label.className = "skiplist-renderer__level-label";
          label.textContent = `L${lvl}`;
          row.appendChild(label);

          const nodesEl = document.createElement("div");
          nodesEl.className = "skiplist-renderer__nodes";
          // head | one column per node | ∅
          nodesEl.style.gridTemplateColumns = `auto repeat(${cols.length}, 1fr) auto`;

          const head = document.createElement("span");
          head.className = "skiplist-renderer__head";
          head.textContent = "head";
          nodesEl.appendChild(head);

          for (const c of cols) {
            const slot = document.createElement("div");
            slot.className = "skiplist-renderer__slot";
            if (c.level >= lvl) {
              // Present at this level — a value node.
              const node = document.createElement("div");
              node.className = "skiplist-renderer__node";
              if (activeIds.has(c.node.id)) {
                node.classList.add("skiplist-renderer__node--active");
                if (activeColor !== "") node.style.setProperty("--node-color", activeColor);
              }
              node.setAttribute("data-node-id", c.node.id);
              node.setAttribute("data-level", String(lvl));
              const v = document.createElement("span");
              v.textContent = String(c.value);
              node.appendChild(v);
              slot.appendChild(node);
            } else {
              // Absent at this level — a thin gap line keeps the column aligned.
              const gap = document.createElement("div");
              gap.className = "skiplist-renderer__gap";
              slot.appendChild(gap);
            }
            nodesEl.appendChild(slot);
          }

          const nul = document.createElement("span");
          nul.className = "skiplist-renderer__null";
          nul.textContent = "∅";
          nodesEl.appendChild(nul);

          row.appendChild(nodesEl);
          content.appendChild(row);
        }
      },
    };
  },
});
