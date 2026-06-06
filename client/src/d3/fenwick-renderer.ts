// Bespoke Fenwick / Binary-Indexed-Tree renderer (ADR-0024 / ADR-0027, renderer
// #11) — the RESPONSIBILITY STAIRCASE. Per-card bespoke DOM on the Renderer SDK
// (like segment-tree / bitset).
//
// A Fenwick tree stores prefix-sum partial aggregates in a 1-indexed array
// `tree[1..n]`: cell `i` is RESPONSIBLE for the half-open range `(i-lowbit(i), i]`
// (lowbit(i) = i & -i, the lowest set bit). The generic `array-1d` path renders a
// flat row of partial sums — which hides the one idea that makes it a BIT: each
// index owns a power-of-two-aligned slice, and a prefix-sum query hops up those
// slices in O(log n). This renderer draws the STAIRCASE: a column grid `n` wide,
// each cell a bar spanning its responsibility columns on its low-bit level
// (`log2(lowbit)`), so index 8 spans the whole array, 4 the left half, the odd
// indices are singletons. The `i` cursor (the adapter's index-cursor on the root
// array) climbs the staircase as the query jumps `i -= lowbit(i)`.
//
// Everything geometric is derived from the cell's SLOT (the Fenwick index) — no
// `meta` needed. The array is stored with a dummy slot 0 (the unused 1-indexing
// sentinel `tree[0]`) so slot == index; this renderer skips slot 0. CSS grid does
// the geometry: a bar spanning columns covers the inter-column gaps so it reads as
// one contiguous segment, while the empty cells between bars ARE the BIT's binary
// structure — meaningful negative space, not gaps to fill.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

/** Lowest set bit of i (i & -i) — the width of the range cell i is responsible for. */
function lowbit(i: number): number {
  return i & -i;
}

export const fenwickRenderer: RendererFn = defineRenderer({
  className: "fenwick-renderer",
  build({ content }) {
    const grid = document.createElement("div");
    grid.className = "fenwick-renderer__grid";
    content.appendChild(grid);

    return {
      onStep(step: VizGraphStep): void {
        // Fenwick cells: slot >= 1. Slot 0 is the unused 1-indexing sentinel
        // (`tree[0]`), kept only so the Arr slot equals the Fenwick index.
        type Cell = { node: VizNode; i: number };
        const cells: Cell[] = [];
        for (const node of step.nodes) {
          if (typeof node.slot === "number" && node.slot >= 1) {
            cells.push({ node, i: node.slot });
          }
        }

        grid.innerHTML = "";
        if (cells.length === 0) {
          grid.classList.add("fenwick-renderer__grid--empty");
          grid.textContent = "(empty)";
          return;
        }
        grid.classList.remove("fenwick-renderer__grid--empty");

        const n = Math.max(...cells.map((c) => c.i)); // 1-indexed array width
        // Low-bit level = log2(lowbit(i)). A bigger lowbit spans a wider range, so
        // it sits HIGHER in the staircase (a smaller grid-row — row 1 is the top).
        const levelOf = (i: number): number => Math.round(Math.log2(lowbit(i)));
        const maxLevel = Math.max(...cells.map((c) => levelOf(c.i)));

        // The `i` traversal cursor's cell(s) this step — emphasised as it climbs
        // the staircase (the adapter emits one index-cursor per active index local).
        const cursorIds = new Set(step.cursor.map((c) => c.target));
        // Each cursor carries its ROLE colour (MarkerColors canon, stamped by the
        // adapter): `i` is deep blue, `mid`/`cur` indigo, `right` bordeaux, …. Tint
        // the highlighted cell in that hue rather than a hardcoded accent, so the same
        // pointer name reads the same colour on every structure — one palette (ADR-0016).
        const cursorColor = new Map(step.cursor.map((c) => [c.target, c.color]));
        const changedSet = new Set(step.changed);

        // n columns, one per Fenwick index 1..n.
        grid.style.gridTemplateColumns = `repeat(${n}, var(--fen-col, 44px))`;

        for (const c of cells) {
          const i = c.i;
          const lb = lowbit(i);
          const level = levelOf(i);
          const lo = i - lb; // half-open low end: cell i covers (lo, i]
          const isSingleton = lb === 1; // odd index — responsible for itself only

          const bar = document.createElement("div");
          bar.className = "fenwick-renderer__node";
          if (isSingleton) bar.classList.add("fenwick-renderer__node--singleton");
          if (cursorIds.has(c.node.id)) {
            bar.classList.add("fenwick-renderer__node--cursor");
            const col = cursorColor.get(c.node.id);
            if (col !== undefined && col !== "") bar.style.setProperty("--fen-cursor-color", col);
          }
          if (changedSet.has(c.node.id)) bar.classList.add("fenwick-renderer__node--changed");
          bar.style.gridRow = String(maxLevel - level + 1);
          bar.style.gridColumn = `${lo + 1} / ${i + 1}`;
          bar.setAttribute("data-node-id", c.node.id);
          bar.setAttribute("data-index", String(i));

          const val = document.createElement("span");
          val.className = "fenwick-renderer__node-value";
          val.textContent = c.node.label;
          bar.appendChild(val);

          // Half-open responsibility range `(lo,i]` — the BIT's teaching point. The
          // right endpoint doubles as the cell's identity (which index this bar is).
          const range = document.createElement("span");
          range.className = "fenwick-renderer__node-range";
          range.textContent = `(${lo},${i}]`;
          bar.appendChild(range);

          grid.appendChild(bar);
        }

        // Index row beneath the staircase — the 1-based Fenwick indices 1..n,
        // each aligned under its column so the responsibility spans read off cleanly.
        for (let i = 1; i <= n; i++) {
          const idx = document.createElement("div");
          idx.className = "fenwick-renderer__index";
          idx.style.gridRow = String(maxLevel + 2);
          idx.style.gridColumn = `${i} / ${i + 1}`;
          idx.textContent = String(i);
          grid.appendChild(idx);
        }
      },
    };
  },
});
