// Bespoke Bitset renderer (ADR-0024 / ADR-0027, renderer #8) — built on the
// Renderer SDK. Per-card (like stack / queue): a bitset is a single Arr card, so
// it fires inside renderMultiCard via RENDERERS, not as a whole-graph renderer.
//
// A bitset reads as a horizontal row of fixed-width cells, each holding 0
// (clear) or 1 (set). The defining visual — the thing that makes it a bitset and
// not a generic `array-1d` row of small ints — is the SET/CLEAR contrast: set
// bits are filled with the accent, clear bits stay muted. A `n bits · k set`
// popcount summary sits above the row, and each cell carries its bit index.
//
// Slot semantics (array-1d): `node.slot === 0` is the leftmost (lowest-index)
// bit; higher slots extend to the right. Cells iterate low-index-first.
//
// The trace's only local is the structure itself (`bits`), so no index cursor is
// emitted; the bit touched on each step is flagged via the per-step `changed`
// diff (the cell whose value flipped), which this renderer renders as `--changed`.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

export const bitsetRenderer: RendererFn = defineRenderer({
  className: "bitset-renderer",
  build({ content }) {
    const bitsetEl = content;

    // Popcount summary above the row: `8 bits · 2 set`.
    const summary = document.createElement("div");
    summary.className = "bitset-renderer__summary";
    summary.setAttribute("aria-live", "polite");
    bitsetEl.appendChild(summary);

    const bitsEl = document.createElement("div");
    bitsEl.className = "bitset-renderer__bits";
    bitsetEl.appendChild(bitsEl);

    return {
      onStep(step: VizGraphStep): void {
        // Low-index-first: slot 0 (bit 0) leftmost, max slot rightmost.
        const slotted = step.nodes.filter((n) => n.slot !== null);
        const ordered = [...slotted].sort((a, b) => (a.slot as number) - (b.slot as number));

        const highlights = new Set(step.highlight);
        const changedSet = new Set(step.changed);
        const removedSet = new Set(step.removed);

        bitsEl.innerHTML = "";
        if (ordered.length === 0) {
          summary.textContent = "0 bits";
          const empty = document.createElement("div");
          empty.className = "bitset-renderer__bit bitset-renderer__bit--empty";
          empty.textContent = "(empty)";
          bitsEl.appendChild(empty);
          return;
        }

        const setCount = ordered.reduce((acc, n) => acc + (n.label === "1" ? 1 : 0), 0);
        summary.textContent = `${ordered.length} bits · ${setCount} set`;

        ordered.forEach((node: VizNode) => {
          const isSet = node.label === "1";

          const bit = document.createElement("div");
          bit.className = "bitset-renderer__bit";
          if (isSet) bit.classList.add("bitset-renderer__bit--set");
          if (highlights.has(node.id)) bit.classList.add("bitset-renderer__bit--new");
          if (changedSet.has(node.id)) bit.classList.add("bitset-renderer__bit--changed");
          if (removedSet.has(node.id)) bit.classList.add("bitset-renderer__bit--removed");
          bit.setAttribute("data-node-id", node.id);
          bit.setAttribute("data-slot", String(node.slot));

          const valueEl = document.createElement("span");
          valueEl.className = "bitset-renderer__bit-value";
          valueEl.textContent = node.label;
          bit.appendChild(valueEl);

          // Bit position (0-indexed, low bit on the left).
          const idxEl = document.createElement("span");
          idxEl.className = "bitset-renderer__bit-index";
          idxEl.textContent = String(node.slot);
          bit.appendChild(idxEl);

          bitsEl.appendChild(bit);
        });
      },
    };
  },
});
