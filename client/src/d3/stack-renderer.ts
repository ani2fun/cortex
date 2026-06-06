// Bespoke Stack renderer (ADR-0024, slice 1 of 17) — now built on the Renderer
// SDK (ADR-0029, renderer #1).
//
// A stack reads as a column of rectangular cells with hairline borders, the
// topmost cell highlighted in the `top` pointer-canon colour, a reversed-index
// column to the right of each cell, AND a side `top →` pointer that slides into
// place next to whichever cell is currently the top. The shape is structurally
// different from SVG circles, so this renderer composes the DOM itself; the
// `layout` arg is intentionally unused (geometry is CSS flex + a JS-computed
// pointer offset).
//
// The SDK (`defineRenderer`) owns the chrome (title / frame / caption / legend /
// truncation notice), the `data-card-content` tag on `.stack-renderer`, the
// WidgetController, the per-step caption, the rAF-after-step that lets layout
// settle before `positionPointer` reads cell rects, the ResizeObserver, and the
// persistent-hover re-apply. This module owns only the cells + pointer.
//
// Slot semantics: `node.slot === 0` is the bottom of the stack; `slot === N-1`
// is the top. With CSS `justify-content: flex-end` on the cells column the stack
// is bottom-anchored, so push lifts the top UP rather than pushing the bottom
// DOWN; the side pointer's `top` CSS property follows the new top cell's centre,
// and a CSS transition slides it between positions.
//
// `setHover("top")` lights the side pointer up via `--hovered` so a hover on the
// `top` ident in the source / locals panel reads as one linked set with canvas.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

// Canonical `top` pointer-canon colour from MarkerColors — used when the
// step's cursors don't include a `top` (most stack traces: `top` is an int
// holding the value, not a ref pointer the tracer can colour).
const TOP_CANON_COLOR = "#4f5bd5";

const SVG_NS = "http://www.w3.org/2000/svg";

function makeArrowSvg(): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, "svg");
  svg.setAttribute("class", "stack-renderer__pointer-arrow");
  svg.setAttribute("width", "20");
  svg.setAttribute("height", "10");
  svg.setAttribute("viewBox", "0 0 20 10");
  svg.setAttribute("aria-hidden", "true");
  const path = document.createElementNS(SVG_NS, "path");
  path.setAttribute("d", "M1 5 L17 5 M13 1 L17 5 L13 9");
  path.setAttribute("stroke", "currentColor");
  path.setAttribute("stroke-width", "1.5");
  path.setAttribute("stroke-linecap", "round");
  path.setAttribute("stroke-linejoin", "round");
  path.setAttribute("fill", "none");
  svg.appendChild(path);
  return svg;
}

function topColorFor(step: VizGraphStep): string {
  // If the trace has a `top` cursor with a target node we'd use its colour
  // (some traces colour the canon `top` role explicitly via the cursor-color
  // pass). Otherwise fall back to the canonical pointer-canon hue.
  const c = step.cursor.find((c) => c.name === "top");
  return c?.color ?? TOP_CANON_COLOR;
}

export const stackRenderer: RendererFn = defineRenderer({
  className: "stack-renderer",
  build({ content }) {
    // `content` is the `.stack-renderer[data-card-content]` element the SDK
    // created. `position: relative` (from the CSS block) anchors the pointer.
    const stackEl = content;

    // Cells column — bottom-anchored (CSS `justify-content: flex-end`).
    const cellsEl = document.createElement("div");
    cellsEl.className = "stack-renderer__cells";
    stackEl.appendChild(cellsEl);

    // Side pointer — `top →` aligned with the current top cell. Sibling of
    // cellsEl (NOT inside it) so the per-step `cellsEl.innerHTML = ""` doesn't
    // wipe it. Absolute positioning + a CSS transition on `top` slides it.
    const topPointerEl = document.createElement("div");
    topPointerEl.className = "stack-renderer__pointer stack-renderer__pointer--hidden";
    const topPointerName = document.createElement("span");
    topPointerName.className = "stack-renderer__pointer-name";
    topPointerName.textContent = "top";
    topPointerEl.appendChild(topPointerName);
    topPointerEl.appendChild(makeArrowSvg());
    stackEl.appendChild(topPointerEl);

    function positionPointer(): void {
      // The top cell is the first DOM child of cellsEl (cells iterate top-first
      // via the slot-DESC sort). Read its centre Y in stackEl coords; cells +
      // pointer share stackEl as the positioning context.
      const topCell = cellsEl.firstElementChild as HTMLElement | null;
      if (topCell === null || topCell.classList.contains("stack-renderer__cell--empty")) {
        topPointerEl.classList.add("stack-renderer__pointer--hidden");
        return;
      }
      topPointerEl.classList.remove("stack-renderer__pointer--hidden");
      const stackRect = stackEl.getBoundingClientRect();
      const cellRect = topCell.getBoundingClientRect();
      const cy = cellRect.top - stackRect.top + cellRect.height / 2;
      topPointerEl.style.top = `${cy}px`;
    }

    function applyHover(name: string): void {
      topPointerEl.classList.toggle(
        "stack-renderer__pointer--hovered",
        name === "top",
      );
    }

    return {
      onStep(step: VizGraphStep): void {
        // Slot-sorted, top-first. Defensive: nodes with `slot === null`
        // (instance refs that snuck into the same VizGraph) are skipped — a
        // stack only renders its cells.
        const slotted = step.nodes.filter((n) => n.slot !== null);
        const ordered = [...slotted].sort(
          (a, b) => (b.slot as number) - (a.slot as number),
        );

        const topColor = topColorFor(step);
        topPointerEl.style.color = topColor;

        const highlights = new Set(step.highlight);
        const changedSet = new Set(step.changed);
        const removedSet = new Set(step.removed);

        cellsEl.innerHTML = "";
        if (ordered.length === 0) {
          // Empty stack — render a placeholder so the chrome doesn't collapse
          // to a 0-height column between steps. The pointer hides.
          const empty = document.createElement("div");
          empty.className = "stack-renderer__cell stack-renderer__cell--empty";
          empty.textContent = "(empty)";
          cellsEl.appendChild(empty);
        } else {
          ordered.forEach((node: VizNode, i) => {
            const cell = document.createElement("div");
            const isTop = i === 0;
            cell.className = "stack-renderer__cell";
            if (isTop) {
              cell.classList.add("stack-renderer__cell--top");
              cell.style.setProperty("--top-color", topColor);
            }
            if (highlights.has(node.id)) cell.classList.add("stack-renderer__cell--new");
            if (changedSet.has(node.id)) cell.classList.add("stack-renderer__cell--changed");
            if (removedSet.has(node.id)) cell.classList.add("stack-renderer__cell--removed");
            cell.setAttribute("data-node-id", node.id);
            cell.setAttribute("data-slot", String(node.slot));

            const valueEl = document.createElement("span");
            valueEl.className = "stack-renderer__cell-value";
            valueEl.textContent = node.label;
            cell.appendChild(valueEl);

            const idxEl = document.createElement("span");
            idxEl.className = "stack-renderer__cell-index";
            // Depth-from-top label: top cell shows N-1, bottom shows 0.
            idxEl.textContent = String(ordered.length - 1 - i);
            cell.appendChild(idxEl);

            cellsEl.appendChild(cell);
          });
        }
      },
      onHover(name: string): void {
        applyHover(name);
      },
      onResize(): void {
        positionPointer();
      },
    };
  },
});
