// Bespoke Array renderer — the DEFAULT for every `array-1d` card (no viz-kind needed).
// Ports the Claude Design handoff's ArrayRenderer (renderers.jsx): a clean row of boxed
// cells with per-cell pointer carets above and an index row below, replacing the generic
// circle layout. Arrays are the single most common shape in the book (arrays, two-pointer,
// sliding-window, sorting, DP), so this is wired as the layoutKind default in index.ts
// rather than gated on `viz-kind`. Per-card bespoke DOM on the Renderer SDK (like bitset /
// queue).
//
// Three column-aligned CSS-grid rows (`--n` columns):
//   1. pointers — each frame index-cursor (left/right/mid/i/…) drawn as a name + down-caret
//      over the cell it indexes, tinted by its ROLE colour (MarkerColors canon, stamped by
//      the adapter: left = blue, right = bordeaux, mid/cur = indigo, …).
//   2. cells    — a boxed value per element. A cursor's cell is tinted in the pointer's hue
//      (`--cursor`); a value that flipped this step rings green (`--changed`).
//   3. indices  — the 0-based positions.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

/** A downward caret SVG, inheriting the pointer's `--pcolor`. */
function makeCaret(): SVGSVGElement {
  const NS = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(NS, "svg");
  svg.setAttribute("width", "10");
  svg.setAttribute("height", "6");
  svg.setAttribute("viewBox", "0 0 10 6");
  svg.setAttribute("fill", "none");
  const path = document.createElementNS(NS, "path");
  path.setAttribute("d", "M1 1l4 4 4-4");
  path.setAttribute("stroke", "var(--pcolor)");
  path.setAttribute("stroke-width", "1.4");
  path.setAttribute("stroke-linecap", "round");
  path.setAttribute("stroke-linejoin", "round");
  svg.appendChild(path);
  return svg;
}

export const arrayRenderer: RendererFn = defineRenderer({
  className: "array-renderer",
  build({ content }) {
    const root = document.createElement("div");
    root.className = "array-renderer";
    const pointersEl = document.createElement("div");
    pointersEl.className = "array-renderer__pointers";
    const cellsEl = document.createElement("div");
    cellsEl.className = "array-renderer__cells";
    const indicesEl = document.createElement("div");
    indicesEl.className = "array-renderer__indices";
    root.appendChild(pointersEl);
    root.appendChild(cellsEl);
    root.appendChild(indicesEl);
    content.appendChild(root);

    return {
      onStep(step: VizGraphStep): void {
        // Cells in slot order (slot 0 leftmost).
        const cells = step.nodes
          .filter((n) => typeof n.slot === "number")
          .sort((a, b) => (a.slot as number) - (b.slot as number));

        pointersEl.innerHTML = "";
        cellsEl.innerHTML = "";
        indicesEl.innerHTML = "";

        if (cells.length === 0) {
          root.style.removeProperty("--n");
          const empty = document.createElement("div");
          empty.className = "array-renderer__empty";
          empty.textContent = "(empty)";
          cellsEl.appendChild(empty);
          return;
        }

        root.style.setProperty("--n", String(cells.length));

        // Frame index-cursors (left/right/mid/i/…) grouped by the cell slot they point at.
        // The adapter turns an integer local in IndexNames into a cursor on cell `arr#i`,
        // each carrying its role colour.
        const slotById = new Map<string, number>();
        cells.forEach((c) => slotById.set(c.id, c.slot as number));
        const pointersBySlot = new Map<number, { name: string; color: string }[]>();
        for (const cur of step.cursor) {
          const slot = slotById.get(cur.target);
          if (slot === undefined) continue;
          const list = pointersBySlot.get(slot) ?? [];
          list.push({ name: cur.name, color: cur.color });
          pointersBySlot.set(slot, list);
        }

        const changedSet = new Set(step.changed);
        const removedSet = new Set(step.removed);
        const highlightSet = new Set(step.highlight);

        for (const node of cells) {
          const slot = node.slot as number;
          const pointers = pointersBySlot.get(slot);

          // Pointer carets above this cell (one per cursor on it).
          const pslot = document.createElement("div");
          pslot.className = "array-renderer__cell-slot";
          if (pointers !== undefined) {
            const labelsEl = document.createElement("div");
            labelsEl.className = "array-renderer__labels";
            for (const p of pointers) {
              const ptr = document.createElement("div");
              ptr.className = "array-renderer__pointer";
              if (p.color !== "") ptr.style.setProperty("--pcolor", p.color);
              const name = document.createElement("span");
              name.className = "array-renderer__pointer-name";
              name.textContent = p.name;
              ptr.appendChild(name);
              ptr.appendChild(makeCaret());
              labelsEl.appendChild(ptr);
            }
            pslot.appendChild(labelsEl);
          }
          pointersEl.appendChild(pslot);

          // The cell.
          const cell = document.createElement("div");
          cell.className = "array-renderer__cell";
          if (pointers !== undefined) {
            cell.classList.add("array-renderer__cell--cursor");
            if (pointers[0].color !== "") cell.style.setProperty("--pcolor", pointers[0].color);
          }
          if (changedSet.has(node.id)) cell.classList.add("array-renderer__cell--changed");
          if (highlightSet.has(node.id)) cell.classList.add("array-renderer__cell--new");
          if (removedSet.has(node.id)) cell.classList.add("array-renderer__cell--removed");
          cell.setAttribute("data-node-id", node.id);
          cell.setAttribute("data-cell-index", String(slot));
          const val = document.createElement("span");
          val.className = "array-renderer__cell-value";
          val.textContent = node.label;
          cell.appendChild(val);
          cellsEl.appendChild(cell);

          // The index.
          const idx = document.createElement("div");
          idx.className = "array-renderer__index";
          idx.textContent = String(slot);
          indicesEl.appendChild(idx);
        }
      },
    };
  },
});
