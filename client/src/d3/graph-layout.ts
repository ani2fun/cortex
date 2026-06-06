// Generic graph layout — the universal fallback for the trace-driven Visualise
// renderer (ADR-0018) and the dedicated layout for the graphs section.
//
// One LayoutFn, two deterministic strategies picked from the union graph's
// topology:
//
//   - Tree-shaped (every node has in-degree <= 1 and is reachable from an
//     in-degree-0 root) -> d3-hierarchy's tidy-tree algorithm, run under a
//     synthetic super-root so a forest, a chain, or a row of isolated nodes
//     all lay out in one pass. Reingold-Tilford is a pure algorithm with no
//     RNG, so this branch is deterministic for free.
//
//   - Anything with a cycle or a merge point (in-degree >= 2) -> a d3-force
//     simulation, run synchronously to a fixed iteration count. d3-force is
//     seeded from an explicit PRNG here so the same graph always settles to
//     the same picture: the layout is computed once over the union of every
//     step (graph-render.ts) and re-opening the modal must redraw identically.
//
// No d3-selection / DOM — d3-force and d3-hierarchy are pure maths, so this
// file is unit-testable in plain Node (see graph-layout.test.ts). A new data
// structure adds a LayoutFn like this one — never a new renderer.

import * as d3 from "d3";
import type { VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout, NodePos } from "./tree-layout";

// Geometry. NODE_R / RING_R mirror NODE_RADIUS and the adornment-ring radius
// in graph-render.ts — the layout sizes the canvas around the renderer's nodes.
const NODE_R = 22;
const RING_R = NODE_R + 4;
const PAD_X = 24; // gap from the outermost ring to the left / right border
const PAD_TOP = 48; // headroom above the top row for the cursor caret
// Room below the bottom row for the node's TWO sub-label lines drawn outside the
// circle (graph-render: `viz-graph__class` at NODE_R+14, `viz-graph__meta` at
// NODE_R+26 ≈ 48 below the centre). 42 clipped the meta line of the lowest node.
const PAD_BOTTOM = 58;

// Tree strategy — centre-to-centre spacing between siblings / between depths.
const TREE_DX = 78;
const TREE_DY = 96;

// Force strategy.
const FORCE_LINK_DIST = 100; // target edge length
const FORCE_CHARGE = -520; // node repulsion
const FORCE_COLLIDE = RING_R + 12; // minimum centre-to-centre distance
const FORCE_ANCHOR = 0.07; // gentle pull to the origin — bounds disconnected fragments
const FORCE_TICKS = 320; // synchronous iterations (~converged at d3's default decay)
const FORCE_SEED = 0x5eed; // fixed seed — the force layout's determinism contract

interface SimNode extends d3.SimulationNodeDatum {
  id: string;
}

// A nested shape for d3.hierarchy; `children` is omitted on a leaf.
interface HierData {
  id: string;
  children?: HierData[];
}

// A forest: the in-degree-0 roots (in input order) and every node's children.
interface Forest {
  roots: string[];
  children: Map<string, string[]>;
}

/**
 * Place the union of every node + edge in a Visualise animation. Deterministic:
 * the same graph always yields the same positions and the same canvas size.
 */
export const graphLayout: LayoutFn = (nodes, edges) => {
  if (nodes.length === 0) return emptyLayout();

  const ids = new Set(nodes.map((n) => n.id));
  // Drop self-loops and dangling edges: neither informs positioning, and both
  // would confuse the topology check (a self-loop is a one-node cycle).
  const realEdges = edges.filter((e) => e.from !== e.to && ids.has(e.from) && ids.has(e.to));

  const forest = asForest(nodes, realEdges);
  const raw = forest !== null ? treePositions(forest) : forcePositions(nodes, realEdges);
  return frame(nodes, raw);
};

// A graph is a forest iff every node has in-degree <= 1 AND every node is
// reachable from an in-degree-0 root. The reachability check is what rejects a
// pure cycle: its nodes all have in-degree 1, but no root can ever enter it.
function asForest(nodes: VizNode[], edges: VizEdge[]): Forest | null {
  const inDeg = new Map<string, number>();
  const children = new Map<string, string[]>();
  for (const n of nodes) {
    inDeg.set(n.id, 0);
    children.set(n.id, []);
  }
  for (const e of edges) {
    inDeg.set(e.to, (inDeg.get(e.to) ?? 0) + 1);
    children.get(e.from)!.push(e.to);
  }
  for (const d of inDeg.values()) if (d > 1) return null; // a merge point

  const roots = nodes.filter((n) => inDeg.get(n.id) === 0).map((n) => n.id);
  if (roots.length === 0) return null; // every node has a parent -> a pure cycle

  const seen = new Set<string>();
  const stack = [...roots];
  while (stack.length > 0) {
    const id = stack.pop()!;
    if (seen.has(id)) continue;
    seen.add(id);
    for (const c of children.get(id) ?? []) stack.push(c);
  }
  if (seen.size !== nodes.length) return null; // an unreachable cycle component

  return { roots, children };
}

// Tidy-tree positions via d3-hierarchy. A synthetic super-root makes a forest
// (or a bare row of isolated nodes) a single tree d3.tree can lay out in one
// pass; it is skipped by identity in the result and the depth axis shifted up
// so the real roots sit at the top of the canvas.
function treePositions(forest: Forest): Map<string, NodePos> {
  // `forest` is acyclic (asForest verified it), so this recursion terminates.
  const build = (id: string): HierData => {
    const kids = forest.children.get(id) ?? [];
    return kids.length > 0 ? { id, children: kids.map(build) } : { id };
  };
  // The super-root carries no real id — it is identified and skipped by
  // reference below, never by its id, so it cannot collide with a heap object.
  const rootData: HierData = { id: "", children: forest.roots.map(build) };

  const root = d3.hierarchy<HierData>(rootData, (d) => d.children);
  const laid = d3.tree<HierData>().nodeSize([TREE_DX, TREE_DY])(root);

  const positions = new Map<string, NodePos>();
  for (const node of laid.descendants()) {
    if (node === laid) continue; // the synthetic super-root
    // Shift up one level so the real roots (tree depth 1) land at y = 0.
    positions.set(node.data.id, { x: node.x, y: node.y - TREE_DY });
  }
  return positions;
}

// Force-directed positions for a genuine graph (a cycle or a merge). Run
// synchronously to a fixed tick count with an explicitly seeded RNG.
function forcePositions(nodes: VizNode[], edges: VizEdge[]): Map<string, NodePos> {
  const simNodes: SimNode[] = nodes.map((n) => ({ id: n.id }));
  const simLinks: d3.SimulationLinkDatum<SimNode>[] = edges.map((e) => ({
    source: e.from,
    target: e.to,
  }));

  const sim = d3
    .forceSimulation<SimNode>(simNodes)
    // Seed the RNG before adding forces so every force initialises from it.
    .randomSource(mulberry32(FORCE_SEED))
    .force(
      "link",
      d3
        .forceLink<SimNode, d3.SimulationLinkDatum<SimNode>>(simLinks)
        .id((d) => d.id)
        .distance(FORCE_LINK_DIST),
    )
    .force("charge", d3.forceManyBody<SimNode>().strength(FORCE_CHARGE))
    .force("collide", d3.forceCollide<SimNode>(FORCE_COLLIDE))
    .force("x", d3.forceX<SimNode>(0).strength(FORCE_ANCHOR))
    .force("y", d3.forceY<SimNode>(0).strength(FORCE_ANCHOR))
    .stop(); // kill d3's internal timer — we tick manually, synchronously
  sim.tick(FORCE_TICKS);

  const positions = new Map<string, NodePos>();
  for (const sn of simNodes) {
    positions.set(sn.id, {
      x: Number.isFinite(sn.x) ? (sn.x as number) : 0,
      y: Number.isFinite(sn.y) ? (sn.y as number) : 0,
    });
  }
  return positions;
}

// Translate raw positions into a padded canvas and report its size. The
// outermost node rings clear the border by PAD_X; the top / bottom rows leave
// room for the cursor caret and the meta sub-label drawn outside the circle.
function frame(nodes: VizNode[], raw: Map<string, NodePos>): GraphLayout {
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;
  for (const n of nodes) {
    const p = raw.get(n.id);
    if (p === undefined) continue;
    minX = Math.min(minX, p.x);
    maxX = Math.max(maxX, p.x);
    minY = Math.min(minY, p.y);
    maxY = Math.max(maxY, p.y);
  }
  if (!Number.isFinite(minX)) return emptyLayout();

  const dx = PAD_X + RING_R - minX;
  const dy = PAD_TOP - minY;
  const positions = new Map<string, NodePos>();
  for (const n of nodes) {
    const p = raw.get(n.id) ?? { x: minX, y: minY };
    positions.set(n.id, { x: p.x + dx, y: p.y + dy });
  }
  return {
    positions,
    width: maxX - minX + 2 * (PAD_X + RING_R),
    height: maxY - minY + PAD_TOP + PAD_BOTTOM,
  };
}

function emptyLayout(): GraphLayout {
  return {
    positions: new Map(),
    width: 2 * (PAD_X + RING_R),
    height: PAD_TOP + PAD_BOTTOM,
  };
}

// mulberry32 — a small, fast, well-distributed seeded PRNG returning [0, 1).
// d3-force's randomSource already defaults to a fixed-seed LCG in d3 7.x, but
// pinning our own keeps determinism a property of THIS file, independent of
// any future d3 default.
function mulberry32(seed: number): () => number {
  let s = seed >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
