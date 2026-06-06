// Contract tests for the bespoke HashMap renderer (ADR-0029, renderer #2).
// Source-AST checks (DOM-less vitest env); the cross-structure rendering itself
// is verified in a real browser by the `hashmap-kind` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { hashmapRenderer } from "./hashmap-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const hmSrc = readFileSync(join(here, "hashmap-renderer.ts"), "utf8");

describe("hashmap-renderer — Slice 2 / ADR-0029 contract", () => {
  it("exports a RendererFn built on the SDK", () => {
    expect(typeof hashmapRenderer).toBe("function");
    expect(hmSrc).toMatch(/defineRenderer\(\{/);
    expect(hmSrc).toMatch(/className:\s*["']hashmap-renderer["']/);
  });

  it("is registered as a WHOLE-GRAPH renderer (bypasses the multi-card split)", () => {
    // structureType="hashmap" must dispatch BEFORE renderMultiCard so the
    // cross-collection structure is drawn whole, not shattered into cards.
    expect(indexSrc).toMatch(
      /import\s*\{\s*hashmapRenderer\s*\}\s*from\s*["']\.\/hashmap-renderer["']/,
    );
    expect(indexSrc).toMatch(/WHOLE_GRAPH_RENDERERS/);
    expect(indexSrc).toMatch(/hashmap:\s*hashmapRenderer/);
    // The dispatch runs before the hasLayoutKindAnnotations / multi-card branch.
    const wholeIdx = indexSrc.indexOf("pickWholeGraphRenderer(graph)");
    const multiIdx = indexSrc.indexOf("hasLayoutKindAnnotations(graph)");
    expect(wholeIdx).toBeGreaterThan(-1);
    expect(multiIdx).toBeGreaterThan(-1);
    expect(wholeIdx).toBeLessThan(multiIdx);
  });

  it("reconstructs buckets from edges (Dict-entry → cells → Entry instances)", () => {
    // The cross-structure walk: it must read edges, not just per-card nodes.
    expect(hmSrc).toMatch(/step\.edges/);
    expect(hmSrc).toMatch(/kind === "entry"/);
  });
});
