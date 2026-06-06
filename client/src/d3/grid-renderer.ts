// Bespoke 2-D matrix renderer — the DEFAULT for every `array-2d` card (a Python
// list-of-lists / Java int[][], auto-detected by HeapToGraph). Ports the Claude Design
// handoff's `.matrix-renderer` (Visualise.html): a grid of boxed cells with a column-index
// header row and a per-row index label, replacing the generic circle layout that a 2-D
// array used to fall through to.
//
// The trace shape (see grid-layout.ts, which does the same derivation for positions): a
// list-of-lists is two cell layers joined by edges — one OUTER (row-pointer) cell per outer
// element (its `slot` is the ROW index) fanning to the cells of one INNER list (a row); each
// INNER cell's `slot` is the COLUMN index. So a cell→cell edge names the outer array (`from`)
// and an inner cell (`to`); the owning outer cell's slot is the row. We render only the inner
// cells (the matrix proper); the outer row-pointer cells are structural and hidden, matching
// the layout's "hide the outer cells" decision.

import type { VizGraphStep } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

/** The owning array's id: a cell id is `${arrayId}#${index}` — split on the last `#`. */
function arrayIdOf(cellId: string): string {
  const hash = cellId.lastIndexOf("#");
  return hash > 0 ? cellId.slice(0, hash) : cellId;
}

interface Placed {
  id: string;
  label: string;
  row: number;
  col: number;
}

export const gridRenderer: RendererFn = defineRenderer({
  className: "matrix-renderer",
  build({ content }) {
    const root = document.createElement("div");
    root.className = "matrix-renderer";
    content.appendChild(root);

    return {
      onStep(step: VizGraphStep): void {
        root.innerHTML = "";

        const cells = step.nodes.filter((n) => typeof n.slot === "number");
        if (cells.length === 0) {
          const empty = document.createElement("div");
          empty.className = "matrix-renderer__empty";
          empty.textContent = "(empty)";
          root.appendChild(empty);
          return;
        }

        const cellIds = new Set(cells.map((c) => c.id));
        const ids = new Set(step.nodes.map((n) => n.id));
        const slotById = new Map<string, number>();
        for (const c of cells) slotById.set(c.id, c.slot as number);

        // Derive the matrix exactly like gridLayout: cell→cell edges name the outer array
        // (`from`) + an inner cell (`to`); the outer cell's slot is the row index.
        const real = step.edges.filter((e) => e.from !== e.to && ids.has(e.from) && ids.has(e.to));
        const outerArrayIds = new Set<string>();
        const rowOfInner = new Map<string, number>();
        for (const e of real) {
          if (cellIds.has(e.from) && cellIds.has(e.to)) {
            outerArrayIds.add(arrayIdOf(e.from));
            if (!rowOfInner.has(e.to)) rowOfInner.set(e.to, slotById.get(e.from) ?? 0);
          }
        }

        // Inner cells = the matrix proper (everything not in the single outer array).
        const placed: Placed[] = [];
        if (outerArrayIds.size === 1) {
          const outerArrayId = [...outerArrayIds][0];
          // Tag the matrix root with the outer-array (grid) id so the frame→card arrow for
          // the `grid` cursor has a target — the whole-graph render has no per-card div, so
          // without this the ArrowLayer can't resolve the grid cursor to anything.
          root.setAttribute("data-card-id", outerArrayId);
          for (const c of cells) {
            if (arrayIdOf(c.id) === outerArrayId) continue; // skip outer (row-pointer) cells
            const row = rowOfInner.get(c.id);
            if (row === undefined) continue;
            placed.push({ id: c.id, label: c.label, row, col: c.slot as number });
          }
        }
        // Degrade gracefully: if the shape didn't resolve to a clean matrix (e.g. mid-build
        // with no rows linked yet), lay the cells out as a single row by slot.
        if (placed.length === 0) {
          [...cells]
            .sort((a, b) => (a.slot as number) - (b.slot as number))
            .forEach((c) => placed.push({ id: c.id, label: c.label, row: 0, col: c.slot as number }));
        }

        const maxRow = placed.reduce((m, p) => Math.max(m, p.row), 0);
        const maxCol = placed.reduce((m, p) => Math.max(m, p.col), 0);
        const byRC = new Map<string, Placed>();
        for (const p of placed) byRC.set(`${p.row},${p.col}`, p);

        const changedSet = new Set(step.changed);
        const highlightSet = new Set(step.highlight);
        const removedSet = new Set(step.removed);
        // A cursor that lands on an inner cell highlights it (the adapter currently lands
        // integer index cursors on the outer/row cells, which we don't render — so this is
        // future-proofing for true (row, col) cursors; the changed-cell ring drives today's
        // fill animation).
        const cursorByCell = new Map<string, string>();
        for (const cur of step.cursor) {
          if (byRC.size > 0 && [...byRC.values()].some((p) => p.id === cur.target)) {
            cursorByCell.set(cur.target, cur.color);
          }
        }

        // --- column-index header row ---
        const colLabels = document.createElement("div");
        colLabels.className = "matrix-renderer__col-labels";
        const corner = document.createElement("div");
        corner.className = "matrix-renderer__corner";
        colLabels.appendChild(corner);
        for (let col = 0; col <= maxCol; col++) {
          const cl = document.createElement("div");
          cl.className = "matrix-renderer__col-label";
          cl.textContent = String(col);
          colLabels.appendChild(cl);
        }
        root.appendChild(colLabels);

        // --- data rows (row-index label + a cell per column) ---
        for (let row = 0; row <= maxRow; row++) {
          const rowEl = document.createElement("div");
          rowEl.className = "matrix-renderer__row";
          const rowLabel = document.createElement("div");
          rowLabel.className = "matrix-renderer__row-label";
          rowLabel.textContent = String(row);
          rowEl.appendChild(rowLabel);
          for (let col = 0; col <= maxCol; col++) {
            const cell = document.createElement("div");
            cell.className = "matrix-renderer__cell";
            const p = byRC.get(`${row},${col}`);
            if (p !== undefined) {
              cell.textContent = p.label;
              cell.setAttribute("data-node-id", p.id);
              const cursorColor = cursorByCell.get(p.id);
              if (cursorColor !== undefined) {
                cell.classList.add("matrix-renderer__cell--active");
                if (cursorColor !== "") cell.style.setProperty("--pcolor", cursorColor);
              }
              if (changedSet.has(p.id)) cell.classList.add("matrix-renderer__cell--changed");
              if (highlightSet.has(p.id)) cell.classList.add("matrix-renderer__cell--new");
              if (removedSet.has(p.id)) cell.classList.add("matrix-renderer__cell--removed");
            } else {
              cell.classList.add("matrix-renderer__cell--hole");
            }
            rowEl.appendChild(cell);
          }
          root.appendChild(rowEl);
        }
      },
    };
  },
});
