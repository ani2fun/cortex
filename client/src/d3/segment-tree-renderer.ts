// Bespoke Segment-tree renderer (ADR-0024 / ADR-0027, renderer #10) — node-link tree.
//
// A segment tree's defining feature is that each node OWNS a contiguous range [lo,hi]
// of the underlying array and stores that slice's aggregate; the leaves are the array
// elements ([i,i]). This renderer draws it the textbook way (matching the design): a
// node-link binary tree — each node a small box showing [lo,hi] over its aggregate
// value, positioned over the CENTRE of its range and joined to its children by SVG
// edges — with the underlying array laid out as a row of indexed cells beneath it.
//
// Reads value/lo/hi from each node's `meta`; walks the left/right edges for the tree
// (BFS from the root — the node with no incoming tree edge — for levels); each node
// sits at x = centre-of-range · COL_W and y = level · LEVEL_H, so the geometry is a
// pure function of the structure and re-opening the modal redraws identically. The
// `cur` traversal cursor is emphasised each step in its MarkerColors role colour.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

const SVG_NS = "http://www.w3.org/2000/svg";
const COL_W = 66; // px per array column — node centres land on column centres
const LEVEL_H = 58; // px between tree levels
const PAD_TOP = 26; // px above the root row
const NODE_PAD = 30; // px below the deepest level before the array row
const ARRAY_H = 52; // px for the underlying-array row

/** Read a numeric `meta` field by name, or null if absent / non-numeric. */
function metaNum(n: VizNode, name: string): number | null {
  const f = n.meta.find((m) => m.name === name);
  if (f === undefined) return null;
  const v = Number(f.value);
  return Number.isFinite(v) ? v : null;
}

export const segmentTreeRenderer: RendererFn = defineRenderer({
  className: "segment-tree-renderer",
  build({ content }) {
    const root = document.createElement("div");
    root.className = "segment-tree-renderer";
    content.appendChild(root);

    return {
      onStep(step: VizGraphStep): void {
        root.innerHTML = "";

        // Each node's covered range [lo,hi] + aggregate, read from meta.
        type Cell = { node: VizNode; lo: number; hi: number; value: string };
        const cells: Cell[] = [];
        for (const node of step.nodes) {
          const lo = metaNum(node, "lo");
          const hi = metaNum(node, "hi");
          if (lo === null || hi === null) continue; // defensive: not a seg node
          const vf = node.meta.find((m) => m.name === "value");
          cells.push({ node, lo, hi, value: vf?.value ?? node.label });
        }

        if (cells.length === 0) {
          root.style.removeProperty("width");
          root.style.removeProperty("height");
          const empty = document.createElement("div");
          empty.className = "segment-tree-renderer__empty";
          empty.textContent = "(empty)";
          root.appendChild(empty);
          return;
        }

        const n = Math.max(...cells.map((c) => c.hi)) + 1; // array width = root span

        // Level per node via BFS over the left/right tree edges. The root is the node
        // with no incoming tree edge; its level is 0, children one deeper.
        const children = new Map<string, string[]>();
        const hasParent = new Set<string>();
        for (const e of step.edges) {
          if (e.label === "left" || e.label === "right") {
            const kids = children.get(e.from);
            if (kids === undefined) children.set(e.from, [e.to]);
            else kids.push(e.to);
            hasParent.add(e.to);
          }
        }
        const level = new Map<string, number>();
        const queue: { id: string; lvl: number }[] = cells
          .filter((c) => !hasParent.has(c.node.id))
          .map((c) => ({ id: c.node.id, lvl: 0 }));
        while (queue.length > 0) {
          const head = queue.shift() as { id: string; lvl: number };
          level.set(head.id, head.lvl);
          for (const ch of children.get(head.id) ?? []) queue.push({ id: ch, lvl: head.lvl + 1 });
        }
        const maxLevel = Math.max(0, ...level.values());

        // Position: x over the centre of the node's [lo,hi] range; y by tree level.
        const pos = new Map<string, { x: number; y: number }>();
        for (const c of cells) {
          const lvl = level.get(c.node.id) ?? maxLevel;
          pos.set(c.node.id, { x: ((c.lo + c.hi) / 2 + 0.5) * COL_W, y: PAD_TOP + lvl * LEVEL_H });
        }

        const canvasW = n * COL_W;
        const treeH = PAD_TOP + maxLevel * LEVEL_H + NODE_PAD;
        root.style.width = `${canvasW}px`;
        root.style.height = `${treeH + ARRAY_H}px`;

        // ── Edges (parent → child), drawn behind the nodes ──────────────────────
        const svg = document.createElementNS(SVG_NS, "svg");
        svg.setAttribute("class", "segment-tree-renderer__svg");
        svg.setAttribute("width", String(canvasW));
        svg.setAttribute("height", String(treeH));
        for (const [from, kids] of children) {
          const p = pos.get(from);
          if (p === undefined) continue;
          for (const to of kids) {
            const cp = pos.get(to);
            if (cp === undefined) continue;
            const line = document.createElementNS(SVG_NS, "line");
            line.setAttribute("x1", String(p.x));
            line.setAttribute("y1", String(p.y));
            line.setAttribute("x2", String(cp.x));
            line.setAttribute("y2", String(cp.y));
            line.setAttribute("class", "segment-tree-renderer__edge");
            svg.appendChild(line);
          }
        }
        root.appendChild(svg);

        // ── Nodes: [lo,hi] over the aggregate value ─────────────────────────────
        // Only the `cur` traversal pointer marks the visited node (`root` is just the
        // reachability anchor); tint it in the cursor's role colour (MarkerColors).
        const curCursors = step.cursor.filter((c) => c.name === "cur");
        const cursorColor = new Map(curCursors.map((c) => [c.target, c.color]));
        const cursorIds = new Set(curCursors.map((c) => c.target));
        const changedSet = new Set(step.changed);

        for (const c of cells) {
          const p = pos.get(c.node.id) as { x: number; y: number };
          const node = document.createElement("div");
          node.className = "segment-tree-renderer__node";
          if (c.lo === c.hi) node.classList.add("segment-tree-renderer__node--leaf");
          if (cursorIds.has(c.node.id)) {
            node.classList.add("segment-tree-renderer__node--cursor");
            const col = cursorColor.get(c.node.id);
            if (col !== undefined && col !== "") node.style.setProperty("--node-color", col);
          }
          if (changedSet.has(c.node.id)) node.classList.add("segment-tree-renderer__node--changed");
          node.style.left = `${p.x}px`;
          node.style.top = `${p.y}px`;
          node.setAttribute("data-node-id", c.node.id);

          const range = document.createElement("span");
          range.className = "segment-tree-renderer__range";
          range.textContent = `[${c.lo},${c.hi}]`;
          node.appendChild(range);
          const val = document.createElement("span");
          val.className = "segment-tree-renderer__val";
          val.textContent = c.value;
          node.appendChild(val);

          root.appendChild(node);
        }

        // ── Underlying array row (the leaves, [i,i]), column-aligned ────────────
        const arrayEl = document.createElement("div");
        arrayEl.className = "segment-tree-renderer__array";
        const leafVal = new Map<number, string>();
        for (const c of cells) if (c.lo === c.hi) leafVal.set(c.lo, c.value);
        for (let i = 0; i < n; i++) {
          const leaf = document.createElement("div");
          leaf.className = "segment-tree-renderer__leaf";
          leaf.style.left = `${(i + 0.5) * COL_W}px`;
          const lv = document.createElement("span");
          lv.textContent = leafVal.get(i) ?? "·";
          leaf.appendChild(lv);
          const em = document.createElement("em");
          em.textContent = String(i);
          leaf.appendChild(em);
          arrayEl.appendChild(leaf);
        }
        root.appendChild(arrayEl);
      },
    };
  },
});
