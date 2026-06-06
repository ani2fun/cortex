// Bespoke Queue / Deque renderer (ADR-0024 / ADR-0027, renderer #4) — built on
// the Renderer SDK. Per-card (like stack): a queue is a single Arr card, so it
// fires inside renderMultiCard via RENDERERS, not as a whole-graph renderer.
//
// A queue reads as a horizontal row of cells, FRONT (head) on the left and BACK
// (tail) on the right — dequeue removes from the head, enqueue appends at the tail;
// the two ends carry DISTINCT head (blue) / tail (bordeaux) callouts. A deque
// (double-ended, structureType "deque") shares the cell row but renders a SYMMETRIC
// variant — a bidirectional ⇄ hint, front/back labels, and one shared end colour —
// so the two no longer look identical. Both register to this renderer; it branches
// on step.structureType.
//
// Slot semantics (queueLayout): `node.slot === 0` is the front (leftmost);
// higher slots extend toward the back (right). Cells iterate front-first.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

const HEAD_COLOR = "#3a5a8c"; // canon blue (MarkerColors `head`) — the dequeue end
const TAIL_COLOR = "#a13e3e"; // canon bordeaux (MarkerColors `tail`) — the enqueue end
// A deque's two ends are SYMMETRIC (add + remove at both), so it colours both ends the
// SAME (the design's single --end-color) and labels them front / back — visually distinct
// from the queue's asymmetric dequeue-head / enqueue-tail.
const DEQUE_END_COLOR = "#4f5bd5"; // canon indigo (the brand accent)

export const queueRenderer: RendererFn = defineRenderer({
  className: "queue-renderer",
  build({ content }) {
    const queueEl = content;

    // Direction hint above the row: dequeue ← head … tail ← enqueue.
    const hint = document.createElement("div");
    hint.className = "queue-renderer__hint";
    const hHead = document.createElement("span");
    hHead.className = "queue-renderer__hint-head";
    hHead.textContent = "dequeue ←";
    const hTail = document.createElement("span");
    hTail.className = "queue-renderer__hint-tail";
    hTail.textContent = "← enqueue";
    hint.appendChild(hHead);
    hint.appendChild(hTail);
    queueEl.appendChild(hint);

    const cellsEl = document.createElement("div");
    cellsEl.className = "queue-renderer__cells";
    queueEl.appendChild(cellsEl);

    return {
      onStep(step: VizGraphStep): void {
        // Deque variant — same cell row, but a bidirectional ⇄ hint + front/back labels +
        // one symmetric end colour, so it no longer looks identical to the FIFO queue.
        const isDeque = step.structureType === "deque";
        queueEl.classList.toggle("queue-renderer--deque", isDeque);
        hHead.textContent = isDeque ? "⇄ front" : "dequeue ←";
        hTail.textContent = isDeque ? "back ⇄" : "← enqueue";

        // Front-first: slot 0 (front/head) leftmost, max slot (back/tail) rightmost.
        const slotted = step.nodes.filter((n) => n.slot !== null);
        const ordered = [...slotted].sort((a, b) => (a.slot as number) - (b.slot as number));

        const highlights = new Set(step.highlight);
        const changedSet = new Set(step.changed);
        const removedSet = new Set(step.removed);

        cellsEl.innerHTML = "";
        if (ordered.length === 0) {
          const empty = document.createElement("div");
          empty.className = "queue-renderer__cell queue-renderer__cell--empty";
          empty.textContent = "(empty)";
          cellsEl.appendChild(empty);
          return;
        }

        ordered.forEach((node: VizNode, i) => {
          const isHead = i === 0;
          const isTail = i === ordered.length - 1;

          const cell = document.createElement("div");
          cell.className = "queue-renderer__cell";
          if (isHead) {
            cell.classList.add("queue-renderer__cell--head");
            cell.style.setProperty("--head-color", isDeque ? DEQUE_END_COLOR : HEAD_COLOR);
          }
          if (isTail) {
            cell.classList.add("queue-renderer__cell--tail");
            cell.style.setProperty("--tail-color", isDeque ? DEQUE_END_COLOR : TAIL_COLOR);
          }
          if (highlights.has(node.id)) cell.classList.add("queue-renderer__cell--new");
          if (changedSet.has(node.id)) cell.classList.add("queue-renderer__cell--changed");
          if (removedSet.has(node.id)) cell.classList.add("queue-renderer__cell--removed");
          cell.setAttribute("data-node-id", node.id);
          cell.setAttribute("data-slot", String(node.slot));

          // Head / tail callout above the end cells (both, if the queue holds one).
          const marker = document.createElement("span");
          marker.className = "queue-renderer__cell-marker";
          if (isHead && isTail) marker.textContent = isDeque ? "front · back" : "head · tail";
          else if (isHead) marker.textContent = isDeque ? "front" : "head";
          else if (isTail) marker.textContent = isDeque ? "back" : "tail";
          else marker.textContent = "";
          cell.appendChild(marker);

          const valueEl = document.createElement("span");
          valueEl.className = "queue-renderer__cell-value";
          valueEl.textContent = node.label;
          cell.appendChild(valueEl);

          // Position from the front (0 = head).
          const idxEl = document.createElement("span");
          idxEl.className = "queue-renderer__cell-index";
          idxEl.textContent = String(i);
          cell.appendChild(idxEl);

          cellsEl.appendChild(cell);
        });
      },
    };
  },
});
