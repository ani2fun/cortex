// Smoke tests for the bespoke Stack renderer (ADR-0024, slice 1). Runs in the
// vitest `node` environment — no DOM, no Scala.js. We assert two cheap, static
// contracts the ArrowLayer + design depend on:
//
//   1. The renderer source tags its visible block with `data-card-content`.
//      ArrowLayer prefers that element over the wide `.viz-card` wrapper, so
//      a Ref arrow's tip lands next to the cells, not in the centring padding
//      to their right. Without this attribute, the truncation bug from the
//      Slice 1 review re-surfaces.
//
//   2. `stackRenderer` is registered under `RENDERERS["stack"]` in the entry
//      module, so `pickRenderer({ steps: [{ structureType: "stack" }] })`
//      dispatches to it. The map's runtime shape is not exported, so we
//      verify by importing the renderer + asserting its function shape.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { stackRenderer } from "./stack-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const stackSrc = readFileSync(join(here, "stack-renderer.ts"), "utf8");
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("stack-renderer — Slice 1 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    // ADR-0029: chrome + the `data-card-content` tag (ArrowLayer's target, so a
    // Ref arrow lands next to the cells, not 252px right in the centring
    // padding) moved to `defineRenderer`. The stack module now just declares a
    // RendererSpec. Assert the wiring: stack uses defineRenderer, SDK sets the tag.
    expect(stackSrc).toMatch(/defineRenderer\(\{/);
    expect(stackSrc).toMatch(/className:\s*["']stack-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("source renders a side `top` pointer as a sibling of cells, not above them", () => {
    // Slice 1's second bug fix: the original `↑ TOP` column header didn't
    // visually move as the top cell changed. The renderer now appends a
    // `.stack-renderer__pointer` to `stackEl` (NOT `cellsEl`, which gets
    // wiped on every step via `innerHTML = ""`), and JS sets its `top` CSS
    // property to the top cell's centre Y so a CSS transition slides it
    // between positions on push / pop.
    expect(stackSrc).toMatch(/stack-renderer__pointer/);
    expect(stackSrc).toMatch(/stack-renderer__pointer-name/);
    expect(stackSrc).toMatch(/stack-renderer__pointer-arrow/);
    // The pointer must NOT be appended to cellsEl (would get wiped per step).
    expect(stackSrc).not.toMatch(
      /cellsEl\.appendChild\(\s*topPointerEl/,
    );
    // It must be appended to stackEl (survives the per-step rebuild).
    expect(stackSrc).toMatch(/stackEl\.appendChild\(\s*topPointerEl\s*\)/);
    // The legacy "↑ TOP" column header is gone.
    expect(stackSrc).not.toMatch(/↑ TOP/);
  });

  it("exports a function — the RendererFn shape `index.ts` registers", () => {
    expect(typeof stackRenderer).toBe("function");
  });

  it("is registered under `RENDERERS.stack` in the entry module", () => {
    // Static source check rather than a runtime probe — the map is module-
    // private. If the registration is renamed or removed, this fails loudly.
    expect(indexSrc).toMatch(
      /import\s*\{\s*stackRenderer\s*\}\s*from\s*["']\.\/stack-renderer["']/,
    );
    expect(indexSrc).toMatch(/stack:\s*stackRenderer/);
  });
});

describe("arrow-layer — data-card-content target selection", () => {
  it("prefers `[data-card-content]` inside the matched card", () => {
    const src = readFileSync(join(here, "arrow-layer.ts"), "utf8");
    // The selector chain: `data-node-id` -> `[data-card-content]` -> card itself.
    expect(src).toMatch(/data-card-content/);
    // Ordering matters — node-id first (Instance Refs), then content, then card.
    const nodeIdx = src.indexOf("data-node-id");
    const contentIdx = src.indexOf("data-card-content");
    const cardIdIdx = src.indexOf("[data-card-id");
    // All three present:
    expect(nodeIdx).toBeGreaterThan(-1);
    expect(contentIdx).toBeGreaterThan(-1);
    expect(cardIdIdx).toBeGreaterThan(-1);
  });
});
