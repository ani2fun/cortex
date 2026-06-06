// Bespoke Linked-list renderer (ADR-0024 / ADR-0027, renderer #9 — rewritten to
// the Claude design's horizontal BOXES).
//
// A linked list is a chain of Instance nodes joined by `next` refs (a doubly-
// linked list adds `prev`). The generic graph path drew it as a vertical chain
// of circles — hiding the textbook horizontal shape. This renderer draws the
// design's form: boxed value nodes left-to-right, a curved `next` arrow between
// each pair, a `∅` null terminator, a `head` caret above the head node, and a
// `cur` caret below the visited node (each in its MarkerColors role colour,
// stamped on the cursor by the adapter). A doubly-linked list — detected by any
// `prev` edge — adds a return `prev` arrow under each pair and a `tail` caret.
//
// Self-aligning: the head/tail/cur carets are absolutely positioned WITHIN their
// node box, so no separate row needs its column widths matched to the node row.
// Per-card bespoke DOM on the Renderer SDK, same pattern as stack / queue /
// skiplist. Replaces the old delegate (renderGraph + linkedListLayout) which is
// why this is registered for both `list-single` and `list-double` in index.ts.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

const NULL_LABEL = "∅";
// Canon role colours (MarkerColors) for the doubly-linked list's two edges. The
// singly-linked `next` arrow stays muted (currentColor) — there's only one kind.
const NEXT_COLOR = "#5a8a5a"; // moss
const PREV_COLOR = "#8a4f7d"; // mulberry

/** A curved left→right `next` arrow (matches the design's bezier + arrowhead). */
function nextArrowSvg(color: string): string {
  return (
    `<svg class="list-renderer__edge-svg" width="34" height="22" viewBox="0 0 34 22" fill="none" preserveAspectRatio="none">` +
    `<path d="M1 11 C 11 11, 22 11, 32 11" stroke="${color}" stroke-width="1.3" fill="none"/>` +
    `<path d="M27 7 l5 4 -5 4" stroke="${color}" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>` +
    `</svg>`
  );
}

/** The doubly-linked edge: `next` arrow on top, return `prev` arrow underneath. */
function doubleArrowSvg(): string {
  return (
    `<svg class="list-renderer__edge-svg list-renderer__edge-svg--double" width="34" height="30" viewBox="0 0 34 30" fill="none" preserveAspectRatio="none">` +
    `<path d="M1 9 C 11 9, 22 9, 32 9" stroke="${NEXT_COLOR}" stroke-width="1.3" fill="none"/>` +
    `<path d="M27 5 l5 4 -5 4" stroke="${NEXT_COLOR}" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>` +
    `<path d="M32 21 C 22 21, 11 21, 1 21" stroke="${PREV_COLOR}" stroke-width="1.3" fill="none"/>` +
    `<path d="M6 17 l-5 4 5 4" stroke="${PREV_COLOR}" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>` +
    `</svg>`
  );
}

/** A small upward caret (for the `cur` pointer below the active node). */
function caretUpSvg(): string {
  return `<svg width="10" height="6" viewBox="0 0 10 6" fill="none"><path d="M1 5l4-4 4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}

/** A small downward caret (for the `head` / `tail` labels above their node). */
function caretDownSvg(): string {
  return `<svg width="10" height="12" viewBox="0 0 10 12" fill="none"><path d="M5 1v8m0 0l-3-3m3 3l3-3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}

/** A pointer label (`head`/`tail` above, `cur` below) tinted in its role colour. */
function pointerLabel(cls: string, text: string, color: string, caret: string): HTMLElement {
  const el = document.createElement("div");
  el.className = cls;
  if (color !== "") el.style.color = color;
  if (cls.endsWith("__cur")) {
    el.innerHTML = `${caret}<span>${text}</span>`;
  } else {
    el.innerHTML = `<span>${text}</span>${caret}`;
  }
  return el;
}

/** One compartment of a node's box — the design's `PREV` / `NEXT` link cells. */
function fieldCell(cls: string, text: string): HTMLElement {
  const el = document.createElement("span");
  el.className = cls;
  el.textContent = text;
  return el;
}

export const linkedListRenderer: RendererFn = defineRenderer({
  className: "list-renderer",
  build({ content }) {
    return {
      onStep(step: VizGraphStep): void {
        // Chain pointers from THIS step's edges; detect doubly via any `prev`.
        const nextOf = new Map<string, string>();
        const hasIncomingNext = new Set<string>();
        let doubly = false;
        for (const e of step.edges) {
          const l = e.label.toLowerCase();
          if (l === "next" || l === "nxt") {
            nextOf.set(e.from, e.to);
            hasIncomingNext.add(e.to);
          } else if (l === "prev" || l === "previous") {
            doubly = true;
          }
        }
        const byId = new Map(step.nodes.map((n) => [n.id, n]));

        // Cursors: head (anchor + label), cur (the visited node), tail (doubly).
        const headCursor = step.cursor.find((c) => c.name === "head");
        const curCursor = step.cursor.find(
          (c) => c.name === "cur" || c.name === "curr" || c.name === "current",
        );
        const tailCursor = step.cursor.find((c) => c.name === "tail");

        // Order the chain: from the head cursor's target if present, else the
        // node with no incoming `next`; walk `next`. Append any stragglers.
        const headId =
          headCursor && byId.has(headCursor.target)
            ? headCursor.target
            : step.nodes.find((n) => !hasIncomingNext.has(n.id))?.id;
        const ordered: VizNode[] = [];
        const seen = new Set<string>();
        let walk: string | undefined = headId;
        while (walk !== undefined && byId.has(walk) && !seen.has(walk)) {
          seen.add(walk);
          ordered.push(byId.get(walk) as VizNode);
          walk = nextOf.get(walk);
        }
        for (const n of step.nodes) if (!seen.has(n.id)) ordered.push(n);

        content.innerHTML = "";
        if (ordered.length === 0) {
          const empty = document.createElement("div");
          empty.className = "list-renderer__empty";
          empty.textContent = "(empty list)";
          content.appendChild(empty);
          return;
        }

        const headColor = headCursor?.color ?? "";
        const curColor = curCursor?.color ?? "";
        const tailColor = tailCursor?.color ?? "";
        const activeId = curCursor?.target;
        const tailId = tailCursor && byId.has(tailCursor.target) ? tailCursor.target : undefined;
        const changed = new Set(step.changed);
        const fresh = new Set(step.highlight);

        // `content` already carries the `list-renderer` class (defineRenderer);
        // just flag the doubly variant for the two-arrow / tail styling.
        content.classList.toggle("list-renderer--double", doubly);

        const row = document.createElement("div");
        row.className = "list-renderer__row";

        ordered.forEach((n) => {
          const box = document.createElement("div");
          box.className = "list-renderer__node";
          if (n.id === activeId) {
            box.classList.add("list-renderer__node--active");
            if (curColor !== "") box.style.setProperty("--node-color", curColor);
          }
          if (changed.has(n.id)) box.classList.add("list-renderer__node--changed");
          if (fresh.has(n.id)) box.classList.add("list-renderer__node--new");
          box.setAttribute("data-node-id", n.id);

          // head / tail caret above this box; cur caret below.
          if (n.id === headId) box.appendChild(pointerLabel("list-renderer__head-label", "head", headColor, caretDownSvg()));
          if (tailId !== undefined && n.id === tailId)
            box.appendChild(pointerLabel("list-renderer__tail-label", "tail", tailColor, caretDownSvg()));

          // 3-compartment node — [PREV] VALUE [NEXT]; the PREV cell only on a doubly
          // list (matches the design's textbook node box).
          if (doubly)
            box.appendChild(fieldCell("list-renderer__field list-renderer__field--prev", "PREV"));
          const v = document.createElement("span");
          v.className = "list-renderer__field list-renderer__val";
          v.textContent = n.label;
          box.appendChild(v);
          box.appendChild(fieldCell("list-renderer__field list-renderer__field--next", "NEXT"));

          if (n.id === activeId) {
            const cur = pointerLabel("list-renderer__cur", curCursor?.name ?? "cur", curColor, caretUpSvg());
            box.appendChild(cur);
          }
          row.appendChild(box);

          // edge to the next node (or to ∅ after the tail).
          const edge = document.createElement("span");
          edge.className = "list-renderer__edge";
          edge.innerHTML = doubly ? doubleArrowSvg() : nextArrowSvg("currentColor");
          row.appendChild(edge);
        });

        const nul = document.createElement("span");
        nul.className = "list-renderer__null";
        nul.textContent = NULL_LABEL;
        row.appendChild(nul);

        content.appendChild(row);

        // Legend — only on a doubly list, where two arrow colours need disambiguating
        // (the singly `next` arrow is the lone, self-evident edge).
        if (doubly) {
          const legend = document.createElement("div");
          legend.className = "list-renderer__legend";
          legend.innerHTML =
            `<span class="list-renderer__legend-item">` +
            `<span class="list-renderer__legend-line" style="background:${NEXT_COLOR}"></span>next</span>` +
            `<span class="list-renderer__legend-item">` +
            `<span class="list-renderer__legend-line" style="background:${PREV_COLOR}"></span>prev</span>`;
          content.appendChild(legend);
        }
      },
    };
  },
});
