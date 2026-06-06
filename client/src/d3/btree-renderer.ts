// Bespoke B-tree renderer (ADR-0024 / ADR-0029, cross-structure whole-graph) — n-ary multi-key tree.
//
// A B-tree node is an `Instance(keys: Ref→Arr[int], children: Ref→Arr[Ref→BNode], leaf)`,
// so the adapter routes BOTH a node's keys and its child links through Arrays — each two
// hops from the node:
//   node --"keys"-->     keyCell(slot i)                 (the i-th key, a scalar)
//   node --"children"--> childCell(slot i) --""--> child BNode
// The generic binary tree-layout only understands left/right, and the per-card split
// shatters every BNode (connected only THROUGH the children Arr) into edge-less cards —
// which is why `viz=binary-tree` drew a B-tree with no edges.
//
// This WHOLE-GRAPH renderer (dispatched before the multi-card split, like the trie/hashmap
// renderers) reconstructs each BNode's key row + ordered child list from the edges, folds
// the Arr scaffolding away, and draws the textbook picture: every node is a BOX of key
// cells, and child pointers hang from the gaps BETWEEN keys (k keys ⇒ k+1 children). The
// tree is balanced by construction, so a pure structural layout redraws identically when
// the modal re-opens. Freshly-inserted keys flash; a local pointing at a node tints it.

import type { VizGraphStep, VizNode } from "./types";
import { defineRenderer } from "./renderer-sdk";
import type { RendererFn } from "./index";

const SVG_NS = "http://www.w3.org/2000/svg";
const KEY_W = 34; // px per key cell
const BOX_H = 32; // px node-box height
const BOX_PAD_X = 5; // px inner horizontal padding inside a box
const LEVEL_H = 82; // px between tree levels (room for edges + cursor badges)
const SIBLING_GAP = 30; // px between sibling subtrees
const PAD = 18; // px canvas padding

interface BTreeNode {
  id: string;
  keys: { id: string; label: string }[]; // key cells, slot order
  childIds: string[]; // child BNode ids, slot order
  leaf: boolean;
}

/** Rebuild each BNode's key row + ordered children from the step's nodes/edges. */
function reconstruct(step: VizGraphStep): { nodes: Map<string, BTreeNode>; rootId: string | null } {
  const byId = new Map<string, VizNode>();
  for (const n of step.nodes) byId.set(n.id, n);

  // childCell --""--> child BNode  (the second hop of a parent→child link)
  const cellChild = new Map<string, string>();
  for (const e of step.edges) {
    if (e.label === "" && byId.get(e.to)?.kind === "BNode") cellChild.set(e.from, e.to);
  }

  const nodes = new Map<string, BTreeNode>();
  const isChild = new Set<string>();
  for (const n of step.nodes) {
    if (n.kind !== "BNode") continue;
    const keyCells: { slot: number; id: string; label: string }[] = [];
    const childCells: { slot: number; childId: string }[] = [];
    for (const e of step.edges) {
      if (e.from !== n.id) continue;
      const cell = byId.get(e.to);
      if (cell === undefined) continue;
      if (e.label === "keys") {
        keyCells.push({ slot: cell.slot ?? 0, id: cell.id, label: cell.label });
      } else if (e.label === "children") {
        const childId = cellChild.get(cell.id);
        if (childId !== undefined) childCells.push({ slot: cell.slot ?? 0, childId });
      }
    }
    keyCells.sort((a, b) => a.slot - b.slot);
    childCells.sort((a, b) => a.slot - b.slot);
    for (const c of childCells) isChild.add(c.childId);
    const leafMeta = n.meta.find((m) => m.name === "leaf");
    nodes.set(n.id, {
      id: n.id,
      keys: keyCells.map((k) => ({ id: k.id, label: k.label })),
      childIds: childCells.map((c) => c.childId),
      leaf: leafMeta ? leafMeta.value === "true" : childCells.length === 0,
    });
  }

  // Root = the only BNode that is no node's child. Prefer one with keys (defensive
  // against a transient mid-split frame where an empty new root is being wired up).
  let rootId: string | null = null;
  for (const [id, node] of nodes) {
    if (isChild.has(id)) continue;
    if (rootId === null || node.keys.length > 0) rootId = id;
  }
  return { nodes, rootId };
}

function boxWidth(node: BTreeNode): number {
  return Math.max(1, node.keys.length) * KEY_W + 2 * BOX_PAD_X;
}

interface Pos {
  x: number;
  y: number;
}

/** N-ary layout: subtree extents (so wide nodes never overlap), centred parents, depth rows. */
function layoutTree(
  nodes: Map<string, BTreeNode>,
  rootId: string | null,
): { pos: Map<string, Pos>; width: number; height: number } {
  const pos = new Map<string, Pos>();
  const extent = new Map<string, number>();
  let maxDepth = 0;

  function computeExtent(id: string, seen: Set<string>): number {
    const node = nodes.get(id);
    if (node === undefined || seen.has(id)) return KEY_W;
    seen.add(id);
    const own = boxWidth(node);
    if (node.childIds.length === 0) {
      extent.set(id, own);
      return own;
    }
    let kids = 0;
    for (const c of node.childIds) kids += computeExtent(c, seen);
    kids += (node.childIds.length - 1) * SIBLING_GAP;
    const e = Math.max(own, kids);
    extent.set(id, e);
    return e;
  }

  function place(id: string, depth: number, leftX: number, seen: Set<string>): void {
    const node = nodes.get(id);
    if (node === undefined || seen.has(id)) return;
    seen.add(id);
    if (depth > maxDepth) maxDepth = depth;
    const ext = extent.get(id) ?? boxWidth(node);
    const y = PAD + depth * LEVEL_H;
    if (node.childIds.length === 0) {
      pos.set(id, { x: leftX + ext / 2, y });
      return;
    }
    let kidsWidth = 0;
    for (const c of node.childIds) kidsWidth += extent.get(c) ?? 0;
    kidsWidth += (node.childIds.length - 1) * SIBLING_GAP;
    let cx = leftX + (ext - kidsWidth) / 2;
    const centers: number[] = [];
    for (const c of node.childIds) {
      const cext = extent.get(c) ?? 0;
      place(c, depth + 1, cx, seen);
      const cp = pos.get(c);
      if (cp !== undefined) centers.push(cp.x);
      cx += cext + SIBLING_GAP;
    }
    const x = centers.length > 0 ? (centers[0] + centers[centers.length - 1]) / 2 : leftX + ext / 2;
    pos.set(id, { x, y });
  }

  if (rootId !== null) {
    computeExtent(rootId, new Set());
    place(rootId, 0, PAD, new Set());
  }
  const width = (rootId !== null ? (extent.get(rootId) ?? 0) : 0) + 2 * PAD;
  const height = PAD + maxDepth * LEVEL_H + BOX_H + PAD;
  return { pos, width, height };
}

export const btreeRenderer: RendererFn = defineRenderer({
  className: "btree-renderer",
  build({ content }) {
    const root = document.createElement("div");
    root.className = "btree-renderer";
    content.appendChild(root);

    return {
      onStep(step: VizGraphStep): void {
        root.innerHTML = "";
        const { nodes, rootId } = reconstruct(step);
        if (nodes.size === 0 || rootId === null) {
          root.style.removeProperty("width");
          root.style.removeProperty("height");
          const empty = document.createElement("div");
          empty.className = "btree-renderer__empty";
          empty.textContent = "(empty)";
          root.appendChild(empty);
          return;
        }

        const { pos, width, height } = layoutTree(nodes, rootId);
        root.style.width = `${width}px`;
        root.style.height = `${height}px`;

        const newIds = new Set(step.highlight);
        const changedIds = new Set(step.changed);
        // A local (cursor / cardCursor) pointing at a BNode tints that node in the
        // pointer's role colour — same as every other bespoke renderer. The NAME is NOT
        // drawn on the node: the shared ArrowLayer already routes a labelled pointer from
        // the stack frame to the node's top-centre, so a target-end badge here collided
        // with that arrow's tip. First colour wins when several locals (a BNode is both a
        // node and a card root) point at one node.
        const cursorColorByNode = new Map<string, string>();
        for (const c of [...step.cursor, ...step.cardCursor]) {
          if (nodes.has(c.target) && !cursorColorByNode.has(c.target)) {
            cursorColorByNode.set(c.target, c.color);
          }
        }

        // ── Child pointers (SVG), behind the boxes. Child i hangs from divider i. ──
        const svg = document.createElementNS(SVG_NS, "svg");
        svg.setAttribute("class", "btree-renderer__svg");
        svg.setAttribute("width", String(width));
        svg.setAttribute("height", String(height));
        for (const node of nodes.values()) {
          const p = pos.get(node.id);
          if (p === undefined || node.childIds.length === 0) continue;
          const w = boxWidth(node);
          const rowLeft = p.x - w / 2 + BOX_PAD_X;
          const rowW = node.keys.length * KEY_W;
          const denom = Math.max(1, node.childIds.length - 1);
          node.childIds.forEach((cid, i) => {
            const cp = pos.get(cid);
            if (cp === undefined) return;
            const fromX = rowLeft + (i * rowW) / denom;
            const line = document.createElementNS(SVG_NS, "line");
            line.setAttribute("x1", String(fromX));
            line.setAttribute("y1", String(p.y + BOX_H));
            line.setAttribute("x2", String(cp.x));
            line.setAttribute("y2", String(cp.y));
            line.setAttribute("class", "btree-renderer__edge");
            svg.appendChild(line);
          });
        }
        root.appendChild(svg);

        // ── Node boxes (HTML), one row of key cells each ──────────────────────────
        for (const node of nodes.values()) {
          const p = pos.get(node.id);
          if (p === undefined) continue;
          const box = document.createElement("div");
          box.className = "btree-renderer__node";
          if (node.leaf) box.classList.add("btree-renderer__node--leaf");
          box.style.left = `${p.x}px`;
          box.style.top = `${p.y}px`;
          box.setAttribute("data-node-id", node.id);

          const cursorColor = cursorColorByNode.get(node.id);
          if (cursorColor !== undefined) {
            box.classList.add("btree-renderer__node--cursor");
            if (cursorColor !== "") box.style.setProperty("--node-color", cursorColor);
          }

          if (node.keys.length === 0) {
            const cell = document.createElement("span");
            cell.className = "btree-renderer__key btree-renderer__key--empty";
            cell.textContent = "·";
            box.appendChild(cell);
          } else {
            node.keys.forEach((k, i) => {
              const cell = document.createElement("span");
              cell.className = "btree-renderer__key";
              if (i > 0) cell.classList.add("btree-renderer__key--divided");
              if (newIds.has(k.id)) cell.classList.add("btree-renderer__key--new");
              if (changedIds.has(k.id)) cell.classList.add("btree-renderer__key--changed");
              cell.textContent = k.label;
              box.appendChild(cell);
            });
          }
          root.appendChild(box);
        }
      },
    };
  },
});
