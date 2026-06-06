// Contract tests for the Renderer SDK (ADR-0029). vitest runs in the `node`
// environment (no DOM), so these are static source checks like the other d3
// suites; the SDK's runtime behaviour is verified in a real browser by the
// Playwright fixtures (stack + the cross-structure shapes).

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { defineRenderer } from "./renderer-sdk";

const here = dirname(fileURLToPath(import.meta.url));
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("renderer-sdk — defineRenderer contract", () => {
  it("exports a factory that returns a RendererFn", () => {
    const fn = defineRenderer({
      className: "test-renderer",
      build: () => ({ onStep: () => {} }),
    });
    expect(typeof fn).toBe("function");
  });

  it("owns the chrome that was duplicated across renderers", () => {
    // The block that was byte-identical in graph-render.ts + stack-renderer.ts.
    expect(sdkSrc).toMatch(/viz-graph not-prose/);
    expect(sdkSrc).toMatch(/viz-graph__frame/);
    expect(sdkSrc).toMatch(/viz-graph__caption/);
    expect(sdkSrc).toMatch(/viz-graph__notice/);
    expect(sdkSrc).toMatch(/buildLegend\(data\)/);
  });

  it("tags the content element `data-card-content` so ArrowLayer can target it", () => {
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
    // className comes from the spec, applied to the content element.
    expect(sdkSrc).toMatch(/content\.className = spec\.className/);
  });

  it("returns the full WidgetController surface", () => {
    for (const method of ["setStep", "setHover", "getStepCount", "destroy"]) {
      expect(sdkSrc).toMatch(new RegExp(`${method}\\(`));
    }
  });

  it("lets layout settle (rAF) and observes resize for geometry-reading renderers", () => {
    expect(sdkSrc).toMatch(/requestAnimationFrame\(/);
    expect(sdkSrc).toMatch(/new ResizeObserver\(/);
  });

  it("re-applies the persistent hover after every step", () => {
    // currentHover survives the per-step rebuild — same contract as renderGraph.
    expect(sdkSrc).toMatch(/instance\.onHover\?\.\(currentHover\)/);
  });

  it("honours chrome:false (sub-card mode) and the outbound onHover option", () => {
    expect(sdkSrc).toMatch(/options\?\.chrome \?\? true/);
    expect(sdkSrc).toMatch(/options\?\.onHover/);
  });
});
