// SVG overlay drawing curves from each active-frame `Ref` local in the
// StackFramesPanel to the matching multi-card sub-canvas (Slice 4).
//
// The overlay mounts into an empty host div (`.algolens-arrows`) that Scala
// renders inside `.algolens__panes`. The host is `position: absolute; inset: 0`
// (see algolens.css) so the SVG spans every pane — both the frames pane (source
// of arrows) and the canvas pane (targets). Coordinates come from
// `getBoundingClientRect()` minus the host's own rect, so the math is correct
// regardless of CSS transforms on either pane (the canvas pane scales with
// VisualiseModal's zoom; getBoundingClientRect already returns post-transform
// coords).
//
// Selectors are hardcoded to the AlgoLens DOM shape:
//   - source: `.algolens-frame:not(.algolens-frame--parent) [data-local-id="N"]`
//             (active frame only — parent frames are dimmed and their locals
//              don't drive cursors)
//   - target: `[data-card-id="C"]`
//
// Recompute triggers:
//   - `setEndpoints(eps)` — fresh data (called on step / phase / case change)
//   - `refresh()` — same data, new coords (called on zoom change)
//   - ResizeObserver on the host's parent — fires on window / panes resize

export interface ArrowEndpoint {
  /** Cursor name; matches `[data-local-id]` on the source row. */
  cursorName: string;
  /** Heap-object id of the card; matches `[data-card-id]` on the target. */
  cardId: string;
  /** Stroke colour (from `VizCursor.color`, MarkerColors palette). */
  color: string;
}

export interface ArrowLayerController {
  setEndpoints(endpoints: ArrowEndpoint[]): void;
  refresh(): void;
  /**
   * Slice 5 — apply the shared hover key. The arrow path + label whose
   * `cursorName` matches `name` get the `--hovered` modifier; pass `""` to
   * clear. Persistent across redraws (each `draw()` re-applies it).
   */
  setHover(name: string): void;
  destroy(): void;
}

const SVG_NS = "http://www.w3.org/2000/svg";

/**
 * Mount an arrow overlay into `host`. `host.parentElement` must contain both
 * the source (active frame locals) and target (canvas cards) elements — for
 * AlgoLens that's `.algolens__panes`. The host element is appended to under
 * `position: absolute; inset: 0;` so the SVG covers the panes row.
 */
export function mountArrowLayer(host: HTMLElement): ArrowLayerController {
  const scope = host.parentElement;
  if (scope === null) {
    console.error("[d3] mountArrowLayer: host has no parent — cannot resolve endpoints");
    return {
      setEndpoints(): void {},
      refresh(): void {},
      destroy(): void {},
    };
  }

  const svg = document.createElementNS(SVG_NS, "svg");
  svg.setAttribute("class", "algolens-arrows__svg");
  svg.setAttribute("aria-hidden", "true");
  // <defs> with an arrowhead marker — `marker-end` on each path picks this up.
  // `context-stroke` makes the head inherit the arrow's stroke color (set per
  // path from the cursor's role colour) so the arrow + tip read as one shape.
  const defs = document.createElementNS(SVG_NS, "defs");
  const marker = document.createElementNS(SVG_NS, "marker");
  marker.setAttribute("id", "algolens-arrow-head");
  marker.setAttribute("viewBox", "0 0 10 10");
  marker.setAttribute("refX", "9");
  marker.setAttribute("refY", "5");
  marker.setAttribute("markerWidth", "6");
  marker.setAttribute("markerHeight", "6");
  marker.setAttribute("orient", "auto-start-reverse");
  const markerPath = document.createElementNS(SVG_NS, "path");
  markerPath.setAttribute("d", "M 0 0 L 10 5 L 0 10 z");
  markerPath.setAttribute("fill", "context-stroke");
  marker.appendChild(markerPath);
  defs.appendChild(marker);
  svg.appendChild(defs);
  host.appendChild(svg);

  let endpoints: ArrowEndpoint[] = [];
  // Slice 5 — persistent hover key; re-applied at the end of every `draw()`.
  let currentHover = "";

  const applyHover = (): void => {
    const paths = svg.querySelectorAll<SVGPathElement>(
      "path.algolens-arrows__path",
    );
    paths.forEach((p) => {
      const matched =
        currentHover !== "" && p.getAttribute("data-cursor-name") === currentHover;
      p.classList.toggle("algolens-arrows__path--hovered", matched);
    });
    const labels = svg.querySelectorAll<SVGTextElement>(
      "text.algolens-arrows__label",
    );
    labels.forEach((t) => {
      const matched =
        currentHover !== "" && t.getAttribute("data-cursor-name") === currentHover;
      t.classList.toggle("algolens-arrows__label--hovered", matched);
    });
  };

  const draw = (): void => {
    // Clear previous paths / labels but preserve the <defs> arrowhead marker —
    // re-creating it every step churns the SVG and breaks `context-stroke`.
    Array.from(svg.children).forEach((child) => {
      if (child.tagName !== "defs") svg.removeChild(child);
    });

    // Span the panes' full SCROLL CONTENT, not just the visible viewport. At
    // narrow widths the `.algolens__panes` grid stacks vertically and scrolls
    // (overflow-y: auto), so a frame's locals row can sit below the fold; without
    // this the viewport-pinned overlay clips any arrow whose endpoint is scrolled
    // out of view (the reported "truncated arrow"). The host is an out-of-flow
    // overlay, so growing it doesn't feed back into the scroll height; it degrades
    // to the viewport height when nothing scrolls (wide side-by-side layout).
    host.style.height = `${scope.scrollHeight}px`;

    const hostRect = host.getBoundingClientRect();
    if (hostRect.width === 0 || hostRect.height === 0) return;

    // Size the SVG to the host so pixel coords align with the panes' content box.
    svg.setAttribute("width", String(hostRect.width));
    svg.setAttribute("height", String(hostRect.height));

    // Top of the canvas content (the structure) — every arrow routes ABOVE this
    // line so it never crosses the nodes/edges sitting between its target and the
    // frame panel (the old right-edge approach drew straight through them).
    let structTop = Infinity;
    scope.querySelectorAll("[data-card-content], [data-card-id]").forEach((el) => {
      structTop = Math.min(structTop, el.getBoundingClientRect().top - hostRect.top);
    });
    const overY = (Number.isFinite(structTop) ? structTop : 0) - 24;

    endpoints.forEach((ep, i) => {
      const src = scope.querySelector(
        `.algolens-frame:not(.algolens-frame--parent) [data-local-id="${cssEscape(ep.cursorName)}"]`,
      );
      // Target selection priority:
      //   1. A specific node element (`data-node-id` matches the heap-object id) —
      //      Instance Refs that point at one node.
      //   2. The card's tagged content element (`[data-card-content]` inside the
      //      matched card) — bespoke renderers (Slice 1+: stack / hashmap / heap /
      //      …) center a narrow visible block inside the wide `.viz-card`; tagging
      //      the inner block lets the arrow tip land next to the actual content,
      //      not in the empty centring padding to its right.
      //   3. The card div itself (`data-card-id` matches the cardId) — the legacy
      //      fallback for the generic graph renderer, where the SVG fills the
      //      card so the right-edge calculation lines up with the content.
      const cardEl = scope.querySelector(
        `[data-card-id="${cssEscape(ep.cardId)}"]`,
      );
      const dst =
        scope.querySelector(`[data-node-id="${cssEscape(ep.cardId)}"]`) ??
        cardEl?.querySelector("[data-card-content]") ??
        cardEl;
      if (src === null || dst === null || dst === undefined) return;

      const srcRect = src.getBoundingClientRect();
      const dstRect = dst.getBoundingClientRect();
      if (srcRect.width === 0 || dstRect.width === 0) return;

      // Source: left edge of the locals row (frames pane sits to the right of
      // the canvas pane, so the arrow LEAVES the locals row from its left).
      const sx = srcRect.left - hostRect.left;
      const sy = srcRect.top + srcRect.height / 2 - hostRect.top;
      // Target: the node's TOP-centre, reached by ORTHOGONAL (engineering-diagram)
      // routing rather than a smooth arch: the arrow leaves the frame HORIZONTALLY,
      // turns UP into the gutter channel, runs ACROSS above the structure, then drops
      // straight DOWN into the node's top. Sharp right-angle corners (H → V → H → V);
      // the across run sits above the structure so it never crosses the nodes between
      // the target and the frame panel.
      const tx = dstRect.left + dstRect.width / 2 - hostRect.left;
      const ty = dstRect.top - hostRect.top;
      // Bus height for the across-run: above the structure, but (a) clamped so it never
      // leaves the canvas top — tall / top-heavy structures (trie, graph) used to send the
      // straight across-line way above the viewport — and (b) staggered per-arrow (index i)
      // into separate lanes so several arrows don't pile onto the same horizontal line.
      const topY = Math.max(Math.min(overY, sy, ty) - 8 - i * 7, 8);
      // Short horizontal lead-out before turning up, clamped so the vertical channel
      // never leaves the canvas on the left (narrow stacked layout, where the frames
      // pane sits below and the source x is near the left edge).
      const elbowX = Math.max(sx - 18, 8);
      const d = `M ${sx},${sy} H ${elbowX} V ${topY} H ${tx} V ${ty}`;

      const path = document.createElementNS(SVG_NS, "path");
      path.setAttribute("class", "algolens-arrows__path");
      path.setAttribute("d", d);
      path.setAttribute("data-cursor-name", ep.cursorName);
      path.setAttribute("marker-end", "url(#algolens-arrow-head)");
      // Inline STYLE (not the `stroke` attribute) — `.algolens-arrows__path { stroke: … }`
      // in CSS is a property and would override a presentation attribute, forcing every
      // arrow to the same --primary indigo. Inline style wins, so each arrow keeps its
      // pointer's MarkerColors role colour (head=blue, cur=indigo, right=bordeaux, …).
      if (ep.color !== "") path.style.stroke = ep.color;
      svg.appendChild(path);

      // Cursor name floats just outside the source end so it doesn't overlap
      // the locals row itself.
      const label = document.createElementNS(SVG_NS, "text");
      label.setAttribute("class", "algolens-arrows__label");
      label.setAttribute("x", String(sx - 6));
      label.setAttribute("y", String(sy - 6));
      label.setAttribute("text-anchor", "end");
      label.setAttribute("data-cursor-name", ep.cursorName);
      // Inline style (see the path stroke note) so the label matches its arrow's role colour.
      if (ep.color !== "") label.style.fill = ep.color;
      label.textContent = ep.cursorName;
      svg.appendChild(label);
    });
    // Slice 5 — re-apply persistent hover to the freshly drawn path / label.
    applyHover();
  };

  const ro = new ResizeObserver(() => draw());
  ro.observe(scope);

  return {
    setEndpoints(eps: ArrowEndpoint[]): void {
      endpoints = eps;
      draw();
    },
    refresh(): void {
      draw();
    },
    setHover(name: string): void {
      currentHover = name;
      applyHover();
    },
    destroy(): void {
      ro.disconnect();
      if (svg.parentNode !== null) svg.parentNode.removeChild(svg);
    },
  };
}

// CSS.escape polyfill — CSS.escape is broadly available but the codebase targets
// es2020 and the type lib doesn't always include it; this trivial fallback
// covers the heap-object ids and Python identifiers we feed in.
function cssEscape(value: string): string {
  if (typeof CSS !== "undefined" && typeof CSS.escape === "function") {
    return CSS.escape(value);
  }
  return value.replace(/[^a-zA-Z0-9_-]/g, (ch) => `\\${ch}`);
}
