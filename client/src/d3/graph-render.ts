// The one generic D3 renderer for the trace-driven Visualise feature (ADR-0018).
//
// Consumes a `VizGraph` (a sequence of graph states) and a pluggable `LayoutFn`,
// draws nodes + edges, and animates between steps. Everything here is shape-agnostic
// — *where* a node sits is the layout's job. Adding a data structure adds a layout,
// never a renderer. Knows nothing about Scala or React; `d3-transition` is loaded
// once by client/main.js.
//
// Layout is computed once from the UNION of every node + edge across all steps, so a
// node holds a stable slot for the whole animation and merely fades in / out as the
// steps that contain it come and go. D3 keyed joins (nodes by id, edges by from->to)
// diff each step against the last.
//
// Slice 1 (AlgoLens 4-pane shell): the internal play-loop Stepper and HTML transport
// buttons are removed. The caller (VisualiseModal / AlgoLens ControlsBar) now owns the
// play-loop via Scala's Stepper.hook. `renderGraph` returns a `WidgetController` that
// exposes `setStep(n, animate)` so the Scala side can push step changes into the D3
// canvas without re-mounting it.

import * as d3 from "d3";
import type { VizGraph, VizGraphStep, VizNode, VizEdge } from "./types";
import type { LayoutFn, GraphLayout } from "./tree-layout";

const MOVE_MS = 450;
const FADE_MS = 350;
const NODE_RADIUS = 22;
const SVG_NS = "http://www.w3.org/2000/svg";

// Bumped per render so each widget's arrowhead <marker> gets a unique id — two
// graphs on one page must not cross-reference each other's def.
let widgetSeq = 0;

interface EdgeDatum {
  id: string;
  from: string;
  to: string;
  label: string;
}

/**
 * Externally-drivable controller returned by `renderGraph` / `renderWidget`.
 *
 * After calling `renderWidget`, store the returned controller and push step changes
 * into it via `setStep(n, animate)`. The internal D3 canvas re-renders the requested
 * step (with optional D3 transitions). Calling `destroy()` is optional but good practice
 * when the container is removed from the DOM.
 */
export interface WidgetController {
  /** Jump to step `n` (clamped to valid range). `animate` triggers D3 transitions. */
  setStep(n: number, animate: boolean): void;
  /**
   * Slice 5 — push a hover key down. Any cursor-mark whose cursor names include
   * `name` lights up with the `--hovered` modifier. Pass `""` to clear. Persistent
   * across step changes: each `setStep` re-applies the current hover.
   */
  setHover(name: string): void;
  /** Total number of steps in the current case. */
  getStepCount(): number;
  /** Clean up any resources (currently a no-op; included for future use). */
  destroy(): void;
}

export interface RenderGraphOptions {
  /**
   * When false, skip the outer chrome (wrapper, title, caption, legend, truncation notice) —
   * the caller is rendering this graph as a sub-card inside its own chrome (multi-card mode,
   * Slice 3 auto-dispatch). Only the centred frame + SVG render into `container`. `setStep`
   * still animates per-step transitions; per-step captions become no-ops here (the parent
   * `renderMultiCard` owns the shared caption).
   */
  chrome?: boolean;
  /**
   * Slice 5 — canvas → caller hover callback. Fires with a cursor name when the
   * pointer enters a cursor-mark; fires with `""` when it leaves. The caller maps
   * the name to a shared hover-state and pushes it back via `setHover` so the
   * matching ident-token and locals row light up alongside the caret.
   */
  onHover?: (name: string) => void;
}

/** Render `data` into `container`. Idempotent — clears any prior render.
 *  Returns a WidgetController so the caller can push step changes externally. */
export function renderGraph(
  container: HTMLElement,
  data: VizGraph,
  layout: LayoutFn,
  onStep?: (index: number) => void,
  options?: RenderGraphOptions,
): WidgetController {
  container.innerHTML = "";
  const showChrome = options?.chrome ?? true;
  const onHover = options?.onHover;
  // Slice 5 — persistent hover key. Re-applied on every renderStep so the
  // `--hovered` modifier survives step changes (the cursor-mark tspans rebuild
  // each step but their hover state must follow the shared name key).
  let currentHover = "";

  let caption: HTMLParagraphElement | null = null;
  let frame: HTMLDivElement;

  if (showChrome) {
    const root = document.createElement("div");
    root.className = "viz-graph not-prose";

    if (data.title) {
      const titleEl = document.createElement("p");
      titleEl.className = "viz-graph__title";
      titleEl.textContent = data.title;
      root.appendChild(titleEl);
    }

    frame = document.createElement("div");
    frame.className = "viz-graph__frame";
    // Tag the node frame as the card's content so a frame→card arrow whose cursor names the
    // whole structure (e.g. union-find's `parent` array) lands on the centred nodes, not the
    // empty top of the card div.
    frame.setAttribute("data-card-content", "");
    root.appendChild(frame);

    caption = document.createElement("p");
    caption.className = "viz-graph__caption";
    caption.setAttribute("aria-live", "polite");
    root.appendChild(caption);

    if (data.truncated) {
      const notice = document.createElement("p");
      notice.className = "viz-graph__notice";
      notice.textContent =
        "Trace truncated — showing the first part of the run.";
      root.appendChild(notice);
    }

    // A static key for the node colours + the pointer caret.
    root.appendChild(buildLegend(data));

    container.appendChild(root);
  } else {
    // Bare mode — caller owns the outer wrapper, title, caption, legend, notice.
    frame = document.createElement("div");
    frame.className = "viz-graph__frame";
    frame.setAttribute("data-card-content", "");
    container.appendChild(frame);
  }

  const steps = data.steps;
  if (steps.length === 0) {
    if (caption !== null) caption.textContent = "No steps to display.";
    return noopController();
  }

  // Layout once from the union of every node + edge across all steps.
  const unionNodes = new Map<string, VizNode>();
  const unionEdges = new Map<string, VizEdge>();
  for (const s of steps) {
    for (const n of s.nodes) unionNodes.set(n.id, n);
    for (const e of s.edges) unionEdges.set(`${e.from}->${e.to}`, e);
  }
  const placed: GraphLayout = layout(
    [...unionNodes.values()],
    [...unionEdges.values()],
  );

  const svg = document.createElementNS(SVG_NS, "svg");
  svg.setAttribute("class", "viz-graph__svg");
  svg.setAttribute("viewBox", `0 0 ${placed.width} ${placed.height}`);
  svg.setAttribute("width", String(placed.width));
  svg.setAttribute("height", String(placed.height));
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", data.title || "Code visualisation");

  // Arrowhead marker — one shared <marker> every edge points at via marker-end, so
  // each edge reads as a directed parent→child reference.
  const arrowId = `viz-graph-arrow-${(widgetSeq += 1)}`;
  const defs = document.createElementNS(SVG_NS, "defs");
  const marker = document.createElementNS(SVG_NS, "marker");
  marker.setAttribute("id", arrowId);
  marker.setAttribute("class", "viz-graph__arrowhead");
  marker.setAttribute("viewBox", "0 0 10 10");
  marker.setAttribute("refX", "8.5");
  marker.setAttribute("refY", "5");
  marker.setAttribute("markerUnits", "userSpaceOnUse");
  marker.setAttribute("markerWidth", "9");
  marker.setAttribute("markerHeight", "9");
  marker.setAttribute("orient", "auto");
  const arrowPath = document.createElementNS(SVG_NS, "path");
  arrowPath.setAttribute("d", "M 1 1 L 9 5 L 1 9 Z");
  marker.appendChild(arrowPath);
  defs.appendChild(marker);
  svg.appendChild(defs);

  const edgesG = document.createElementNS(SVG_NS, "g");
  edgesG.setAttribute("class", "viz-graph__edges");
  const nodesG = document.createElementNS(SVG_NS, "g");
  nodesG.setAttribute("class", "viz-graph__nodes");
  svg.appendChild(edgesG);
  svg.appendChild(nodesG);
  frame.appendChild(svg);

  function centerOf(id: string): [number, number] {
    const p = placed.positions.get(id);
    if (p === undefined) return [0, 0];
    return [p.x, p.y];
  }

  function transformOf(id: string): string {
    const [x, y] = centerOf(id);
    return `translate(${x},${y})`;
  }

  function edgePath(fromId: string, toId: string): string {
    const override = placed.edgePaths?.get(`${fromId}->${toId}`);
    if (override !== undefined) return override;
    const [px, py] = centerOf(fromId);
    const [cx, cy] = centerOf(toId);
    const dx = cx - px;
    const dy = cy - py;
    const len = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy));
    const ux = dx / len;
    const uy = dy / len;
    return `M ${px + ux * NODE_RADIUS} ${py + uy * NODE_RADIUS} L ${cx - ux * NODE_RADIUS} ${cy - uy * NODE_RADIUS}`;
  }

  // Edge-label anchor — biased 0.66 toward the child rather than the true
  // midpoint. At the midpoint a near-vertical parent→child edge (e.g. a
  // single-child AVL chain, where TREE_DY/2 == NODE_RADIUS+26) drops the label
  // exactly onto the parent's class / meta sub-labels (`key=… height=…`). 0.66
  // lands it in the clear gap below those labels and above the child's circle.
  function edgeMid(fromId: string, toId: string): [number, number] {
    const [px, py] = centerOf(fromId);
    const [cx, cy] = centerOf(toId);
    const t = 0.66;
    return [px + (cx - px) * t, py + (cy - py) * t];
  }

  function renderStep(index: number, animate: boolean): void {
    const step: VizGraphStep = steps[index];
    const present = step.nodes;
    const presentIds = new Set(present.map((n) => n.id));
    const edges: EdgeDatum[] = [];
    for (const e of step.edges) {
      if (presentIds.has(e.from) && presentIds.has(e.to)) {
        edges.push({
          id: `${e.from}->${e.to}`,
          from: e.from,
          to: e.to,
          label: e.label,
        });
      }
    }
    const cursorsByNode = new Map<string, { name: string; color: string }[]>();
    for (const c of step.cursor) {
      const held = cursorsByNode.get(c.target);
      if (held !== undefined) held.push({ name: c.name, color: c.color });
      else cursorsByNode.set(c.target, [{ name: c.name, color: c.color }]);
    }
    const highlights = new Set(step.highlight);
    const changedSet = new Set(step.changed);
    const removedSet = new Set(step.removed);

    const traversedEdges = new Map<string, string>();
    if (index > 0) {
      const prevByName = new Map<string, string>();
      for (const c of steps[index - 1].cursor) prevByName.set(c.name, c.target);
      for (const c of step.cursor) {
        const was = prevByName.get(c.name);
        if (was !== undefined && was !== c.target) {
          const fwd = `${was}->${c.target}`;
          const bwd = `${c.target}->${was}`;
          if (edges.some((e) => e.id === fwd)) traversedEdges.set(fwd, c.color);
          else if (edges.some((e) => e.id === bwd))
            traversedEdges.set(bwd, c.color);
        }
      }
    }

    // --- edges ---
    const edgeSel = d3
      .select(edgesG)
      .selectAll("path.viz-graph__edge")
      .data(edges, (e: any) => e.id);
    const edgeEnter = edgeSel
      .enter()
      .append("path")
      .attr("class", "viz-graph__edge")
      .attr("fill", "none")
      .attr("marker-end", `url(#${arrowId})`)
      .attr("d", (e: any) => edgePath(e.from, e.to))
      .attr("opacity", 0);
    edgeSel.exit().remove();
    const edgeAll = edgeEnter.merge(edgeSel);
    edgeAll
      .classed("viz-graph__edge--traversed", (e: any) =>
        traversedEdges.has(e.id),
      )
      .style("stroke", (e: any) => traversedEdges.get(e.id) ?? null);
    if (animate) {
      edgeEnter.transition("edge-fade").duration(FADE_MS).attr("opacity", 1);
      edgeAll
        .transition("edge-move")
        .duration(MOVE_MS)
        .ease(d3.easeCubicInOut)
        .attr("d", (e: any) => edgePath(e.from, e.to));
    } else {
      edgeAll.attr("opacity", 1).attr("d", (e: any) => edgePath(e.from, e.to));
    }

    // --- edge labels ---
    const edgeLabelSel = d3
      .select(edgesG)
      .selectAll("text.viz-graph__edge-label")
      .data(edges, (e: any) => e.id);
    const edgeLabelEnter = edgeLabelSel
      .enter()
      .append("text")
      .attr("class", "viz-graph__edge-label")
      .attr("text-anchor", "middle")
      .attr("dy", "0.32em")
      .attr("x", (e: any) => edgeMid(e.from, e.to)[0])
      .attr("y", (e: any) => edgeMid(e.from, e.to)[1])
      .attr("opacity", 0)
      .text((e: any) => e.label);
    edgeLabelSel.exit().remove();
    const edgeLabelAll = edgeLabelEnter.merge(edgeLabelSel);
    edgeLabelAll.text((e: any) => e.label);
    if (animate) {
      edgeLabelEnter
        .transition("edge-label-fade")
        .duration(FADE_MS)
        .attr("opacity", 1);
      edgeLabelAll
        .transition("edge-label-move")
        .duration(MOVE_MS)
        .ease(d3.easeCubicInOut)
        .attr("x", (e: any) => edgeMid(e.from, e.to)[0])
        .attr("y", (e: any) => edgeMid(e.from, e.to)[1]);
    } else {
      edgeLabelAll
        .attr("opacity", 1)
        .attr("x", (e: any) => edgeMid(e.from, e.to)[0])
        .attr("y", (e: any) => edgeMid(e.from, e.to)[1]);
    }

    // --- nodes ---
    const nodeSel = d3
      .select(nodesG)
      .selectAll("g.viz-graph__node")
      .data(present, (n: any) => n.id);
    const nodeEnter = nodeSel
      .enter()
      .append("g")
      .attr("class", "viz-graph__node")
      .attr("transform", (n: any) => transformOf(n.id))
      // `data-node-id` lets ArrowLayer aim at THIS specific node when a Ref local
      // points to it — without it, the arrow can only target the surrounding card
      // div and the tip lands in empty padding.
      .attr("data-node-id", (n: any) => n.id)
      .attr("opacity", 0);
    nodeEnter
      .append("circle")
      .attr("class", "viz-graph__circle")
      .attr("r", NODE_RADIUS);
    nodeEnter
      .append("circle")
      .attr("class", "viz-graph__node-ring")
      .attr("r", NODE_RADIUS + 4);
    nodeEnter
      .append("text")
      .attr("class", "viz-graph__value")
      .attr("text-anchor", "middle")
      .attr("dy", "0.32em")
      .text((n: any) => n.label);
    nodeEnter
      .append("text")
      .attr("class", "viz-graph__class")
      .attr("text-anchor", "middle")
      .attr("y", NODE_RADIUS + 14)
      .text((n: any) => classLabel(n));
    nodeEnter
      .append("text")
      .attr("class", "viz-graph__meta")
      .attr("text-anchor", "middle")
      .attr("y", NODE_RADIUS + 26)
      .text((n: any) => metaText(n));
    nodeEnter
      .append("text")
      .attr("class", "viz-graph__cursor-mark")
      .attr("text-anchor", "middle")
      .attr("y", -(NODE_RADIUS + 10));
    nodeSel.exit().remove();
    const nodeAll = nodeEnter.merge(nodeSel);
    nodeAll
      .classed("viz-graph__node--cursor", (n: any) => cursorsByNode.has(n.id))
      .classed("viz-graph__node--new", (n: any) => highlights.has(n.id))
      .classed("viz-graph__node--changed", (n: any) => changedSet.has(n.id))
      .classed("viz-graph__node--removed", (n: any) => removedSet.has(n.id))
      // Persistent structural state (not a per-step diff): a trie word-end node
      // (kind "terminal") gets a double-ring. Other renderers never set this kind.
      .classed("viz-graph__node--terminal", (n: any) => n.kind === "terminal")
      // A synthesised linked-list null sentinel (kind "null") — a dashed ∅ circle
      // so a chain visibly ends at null. Only the linked-list renderer sets it.
      .classed("viz-graph__node--null", (n: any) => n.kind === "null");
    nodeAll.select("text.viz-graph__value").text((n: any) => n.label);
    nodeAll.select("text.viz-graph__class").text((n: any) => classLabel(n));
    nodeAll.select("text.viz-graph__meta").text((n: any) => metaText(n));
    nodeAll.select("text.viz-graph__cursor-mark").each(function (n: any) {
      const sel = d3.select(this as SVGTextElement);
      sel.selectAll("tspan").remove();
      const cs = cursorsByNode.get(n.id);
      if (cs === undefined || cs.length === 0) {
        // No cursor pointing here this step — drop name attr + handlers so the
        // hover gate (Slice 5) doesn't match a stale name on an invisible mark.
        sel.attr("data-cursor-names", null);
        sel.on("mouseenter", null);
        sel.on("mouseleave", null);
        return;
      }
      // Slice 5 — store every cursor name on the element (pipe-joined; cursor
      // names are identifiers, no pipe collision). `applyHover` splits and
      // matches the hover key to toggle `--hovered`.
      sel.attr("data-cursor-names", cs.map((c) => c.name).join("|"));
      if (onHover !== undefined) {
        // V1 hover emits the first cursor name on this caret. A multi-cursor
        // caret ("low, mid") reports only "low" here; the source side's name
        // matching still highlights both locals + both tokens, which is fine
        // for the DoD ("hover any name, see it everywhere").
        const firstName = cs[0].name;
        sel.on("mouseenter", () => onHover(firstName));
        sel.on("mouseleave", () => onHover(""));
      }
      cs.forEach((c, i) => {
        sel
          .append("tspan")
          .attr("fill", c.color || null)
          .text(i === 0 ? c.name : `, ${c.name}`);
      });
      sel
        .append("tspan")
        .attr("fill", cs[0].color || null)
        .text("  ▾");
    });
    const opacityOf = (n: any): number => (removedSet.has(n.id) ? 0.25 : 1);
    if (animate) {
      nodeEnter.transition("node-fade").duration(FADE_MS).attr("opacity", 1);
      nodeAll
        .transition("node-move")
        .duration(MOVE_MS)
        .ease(d3.easeCubicInOut)
        .attr("transform", (n: any) => transformOf(n.id))
        .attr("opacity", opacityOf);
    } else {
      nodeAll
        .attr("opacity", opacityOf)
        .attr("transform", (n: any) => transformOf(n.id));
    }

    if (caption !== null) caption.textContent = step.annotation.title;
    // Slice 5 — re-apply the persistent hover key. The cursor-mark tspans
    // rebuilt for this step; their `--hovered` class needs to follow the
    // shared hover state, not reset to false.
    applyHover(currentHover);
  }

  /**
   * Slice 5 — toggle `viz-graph__cursor-mark--hovered` on every cursor-mark
   * whose `data-cursor-names` list includes `name`. An empty `name` clears all.
   * Called by `setStep` (after rebuild) and `setHover` (when state changes
   * outside a step boundary).
   */
  function applyHover(name: string): void {
    d3.select(nodesG)
      .selectAll<SVGTextElement, unknown>("text.viz-graph__cursor-mark")
      .each(function () {
        const sel = d3.select(this);
        const attr = sel.attr("data-cursor-names");
        const names = attr === null || attr === "" ? [] : attr.split("|");
        const hovered = name !== "" && names.includes(name);
        sel.classed("viz-graph__cursor-mark--hovered", hovered);
      });
  }

  // Initial render at step 0 (no animation).
  renderStep(0, false);
  if (onStep !== undefined) onStep(0);

  // Return the external controller — the AlgoLens ControlsBar drives the step.
  return {
    setStep(n: number, animate: boolean): void {
      const idx = Math.max(0, Math.min(steps.length - 1, n));
      renderStep(idx, animate);
      if (onStep !== undefined) onStep(idx);
    },
    setHover(name: string): void {
      currentHover = name;
      applyHover(name);
    },
    getStepCount(): number {
      return steps.length;
    },
    destroy(): void {
      // No timers to clean up (play loop lives in Scala's Stepper.hook).
    },
  };
}

/** No-op controller returned when there are no steps to render. */
function noopController(): WidgetController {
  return {
    setStep(_n: number, _animate: boolean): void {},
    setHover(_name: string): void {},
    getStepCount(): number {
      return 0;
    },
    destroy(): void {},
  };
}

export function buildLegend(data: VizGraph): HTMLDivElement {
  const legend = document.createElement("div");
  legend.className = "viz-graph__legend";
  legend.append(
    legendItem(
      "viz-graph__legend-swatch--cursor",
      "",
      "a variable points here",
    ),
    legendItem("viz-graph__legend-swatch--new", "", "new this step"),
  );
  if (data.steps.some((s) => s.changed.length > 0))
    legend.append(
      legendItem("viz-graph__legend-swatch--changed", "", "value changed"),
    );
  if (data.steps.some((s) => s.removed.length > 0))
    legend.append(
      legendItem("viz-graph__legend-swatch--removed", "", "removed"),
    );
  legend.append(
    legendItem(
      "viz-graph__legend-swatch--pointer",
      "▾",
      "pointer — labelled with the variable",
    ),
  );
  return legend;
}

function legendItem(
  modifier: string,
  glyph: string,
  text: string,
): HTMLSpanElement {
  const item = document.createElement("span");
  item.className = "viz-graph__legend-item";
  const swatch = document.createElement("span");
  swatch.className = `viz-graph__legend-swatch ${modifier}`;
  swatch.textContent = glyph;
  const label = document.createElement("span");
  label.textContent = text;
  item.append(swatch, label);
  return item;
}

function metaText(n: VizNode): string {
  return n.meta.map((f) => `${f.name}=${f.value}`).join("  ");
}

// Class label shown below each instance node — gives the user the type without
// taking up scarce space inside the circle. Empty for collection members
// (`cell` / `entry`) since the parent card's title already conveys "this is a
// list / dict" — repeating it under every cell would be noise. Also empty
// when the value label IS the class name (a class with no value field falls
// back to the class name as its in-circle label — no point repeating it).
function classLabel(n: VizNode): string {
  // Synthetic structural kinds aren't class names — collection members
  // (`cell` / `entry`), trie nodes (`trie` / `terminal`), and a linked-list null
  // sentinel (`null`) carry their meaning in the parent card / the in-circle char
  // / the ring / the ∅ glyph, not a sub-label.
  if (
    n.kind === "cell" ||
    n.kind === "entry" ||
    n.kind === "trie" ||
    n.kind === "terminal" ||
    n.kind === "null"
  )
    return "";
  if (n.label === n.kind) return "";
  return n.kind;
}
