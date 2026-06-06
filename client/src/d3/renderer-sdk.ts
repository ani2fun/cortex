// Renderer SDK (ADR-0029) — the shared scaffold every bespoke renderer
// (ADR-0024) is built on, so slices 2–16 are each ~the per-step DOM, not a
// copy of the chrome + controller + lifecycle plumbing.
//
// The chrome block (`.viz-graph` wrapper, title, `.viz-graph__frame`, caption,
// truncation notice, legend) was byte-identical between `graph-render.ts`'s
// `renderGraph` and `stack-renderer.ts`. `defineRenderer` owns it once, plus:
//   - the `data-card-content` tag on the renderer's content element (so
//     ArrowLayer aims at the visible content, not the wide card padding),
//   - the `WidgetController` (`setStep` / `setHover` / `getStepCount` /
//     `destroy`) the Scala Stepper drives,
//   - the per-step caption update (`step.annotation.title`),
//   - a `requestAnimationFrame` after each step so layout-dependent geometry
//     (e.g. a pointer that reads cell rects) settles before it's measured,
//   - a `ResizeObserver` on the content element,
//   - persistent hover: the current hover key is re-applied after every step.
//
// A renderer provides a `className` + a `build(ctx)` that returns a
// `RendererInstance` (`onStep` required; `onHover` / `onResize` / `destroy`
// optional). The instance owns ONLY the per-step DOM and its own geometry.

import type { VizGraph, VizGraphStep } from "./types";
import type { LayoutFn } from "./tree-layout";
import { buildLegend, type WidgetController, type RenderGraphOptions } from "./graph-render";
// Type-only — avoids a runtime cycle with ./index.ts (which imports the
// concrete renderers that call defineRenderer).
import type { RendererFn } from "./index";

/** What a bespoke renderer implements. The SDK calls these; it owns everything else. */
export interface RendererInstance {
  /** Render one step. `animate` mirrors `setStep`'s flag (most bespoke renderers ignore it). */
  onStep(step: VizGraphStep, index: number, animate: boolean): void;
  /** Inbound hover key (host → renderer). Re-applied after every step. Pass-through of `setHover`. */
  onHover?(name: string): void;
  /** Called via rAF after each step AND on content resize — for geometry that must read laid-out rects. */
  onResize?(): void;
  /** Release resources (the SDK already disconnects the ResizeObserver). */
  destroy?(): void;
}

/** Handed to `build`; everything a renderer needs without re-deriving the chrome. */
export interface RendererContext {
  /** The `.<className>[data-card-content]` element; append per-step DOM here. */
  content: HTMLElement;
  /** The `.viz-graph__frame` the content lives in (rarely needed directly). */
  frame: HTMLElement;
  /** The whole graph (all steps) — bespoke renderers that need cross-step state read it here. */
  data: VizGraph;
  /** The layout chosen for this graph (bespoke renderers may ignore it and self-position). */
  layout: LayoutFn;
  /** Outbound hover (renderer → host): call when the pointer enters/leaves an interactive mark. */
  emitHover(name: string): void;
}

export interface RendererSpec {
  /** CSS block class for the content element, e.g. "stack-renderer". */
  className: string;
  /** Construct the renderer instance once; the SDK drives it per step. */
  build(ctx: RendererContext): RendererInstance;
}

/**
 * Turn a {@link RendererSpec} into a {@link RendererFn} (the signature `index.ts`
 * registers + `renderMultiCard` invokes per card). Mirrors `renderGraph`'s
 * options: `chrome: false` skips the wrapper (sub-card mode); `onHover` is the
 * outbound hover callback.
 */
export function defineRenderer(spec: RendererSpec): RendererFn {
  return (
    container: HTMLElement,
    data: VizGraph,
    layout: LayoutFn,
    onStep?: (index: number) => void,
    options?: RenderGraphOptions,
  ): WidgetController => {
    container.innerHTML = "";
    const showChrome = options?.chrome ?? true;
    const emitHover = options?.onHover ?? ((_: string) => {});
    // Persistent hover key — re-applied after every step so the `--hovered`
    // state survives the per-step DOM rebuild (same contract as renderGraph).
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
      root.appendChild(frame);

      caption = document.createElement("p");
      caption.className = "viz-graph__caption";
      caption.setAttribute("aria-live", "polite");
      root.appendChild(caption);

      if (data.truncated) {
        const notice = document.createElement("p");
        notice.className = "viz-graph__notice";
        notice.textContent = "Trace truncated — showing the first part of the run.";
        root.appendChild(notice);
      }

      root.appendChild(buildLegend(data));
      container.appendChild(root);
    } else {
      frame = document.createElement("div");
      frame.className = "viz-graph__frame";
      container.appendChild(frame);
    }

    // The renderer's content element. `data-card-content` lets ArrowLayer aim
    // at the visible content rather than the wide centring padding around it.
    const content = document.createElement("div");
    content.className = spec.className;
    content.setAttribute("data-card-content", "");
    frame.appendChild(content);

    const instance = spec.build({ content, frame, data, layout, emitHover });
    const steps = data.steps;

    function renderStep(index: number, animate: boolean): void {
      if (index < 0 || index >= steps.length) return;
      const step = steps[index];
      if (caption !== null) caption.textContent = step.annotation.title;
      instance.onStep(step, index, animate);
      // Re-apply the persistent hover after the per-step rebuild.
      instance.onHover?.(currentHover);
      // Let layout settle (cell heights, etc.) before geometry reads rects.
      if (instance.onResize) requestAnimationFrame(() => instance.onResize?.());
    }

    let resizeObserver: ResizeObserver | null = null;
    if (instance.onResize) {
      resizeObserver = new ResizeObserver(() => instance.onResize?.());
      resizeObserver.observe(content);
    }

    if (steps.length > 0) {
      renderStep(0, false);
    } else if (caption !== null) {
      caption.textContent = "No steps to display.";
    }
    if (onStep !== undefined) onStep(0);

    return {
      setStep(n: number, animate: boolean): void {
        const idx = Math.max(0, Math.min(steps.length - 1, n));
        renderStep(idx, animate);
        if (onStep !== undefined) onStep(idx);
      },
      setHover(name: string): void {
        currentHover = name;
        instance.onHover?.(name);
      },
      getStepCount(): number {
        return steps.length;
      },
      destroy(): void {
        resizeObserver?.disconnect();
        instance.destroy?.();
      },
    };
  };
}
