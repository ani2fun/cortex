// Contract tests for the bespoke Segment-tree renderer (ADR-0027, renderer #10).
// Source-AST checks; the node-link tree + underlying array is verified in a real
// browser by the `segment-tree` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { segmentTreeRenderer } from "./segment-tree-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const segSrc = readFileSync(join(here, "segment-tree-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("segment-tree-renderer — ADR-0027 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof segmentTreeRenderer).toBe("function");
    expect(segSrc).toMatch(/defineRenderer\(\{/);
    expect(segSrc).toMatch(/className:\s*["']segment-tree-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is registered per-card under `segment-tree`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*segmentTreeRenderer\s*\}\s*from\s*["']\.\/segment-tree-renderer["']/,
    );
    expect(indexSrc).toMatch(/"segment-tree":\s*segmentTreeRenderer/);
  });

  it("reads lo/hi/value from meta and draws a node-link tree + underlying array", () => {
    // Range + aggregate come from the node's meta sub-labels.
    expect(segSrc).toMatch(/metaNum\(node, "lo"\)/);
    expect(segSrc).toMatch(/metaNum\(node, "hi"\)/);
    expect(segSrc).toMatch(/m\.name === "value"/);
    // Node-link tree: BFS levels over the left/right tree edges, drawn with SVG edges
    // between [lo,hi]+value node boxes (not the old range-bar grid).
    expect(segSrc).toMatch(/e\.label === "left" \|\| e\.label === "right"/);
    expect(segSrc).toMatch(/segment-tree-renderer__edge/);
    expect(segSrc).toMatch(/segment-tree-renderer__range/);
    expect(segSrc).toMatch(/createElementNS/);
    // The underlying array row (the leaves) at the bottom.
    expect(segSrc).toMatch(/segment-tree-renderer__array/);
    expect(segSrc).toMatch(/segment-tree-renderer__leaf/);
    // Only the `cur` traversal pointer marks the visited node, tinted in its role colour.
    expect(segSrc).toMatch(/c\.name === "cur"/);
    expect(segSrc).toMatch(/setProperty\(["']--node-color["']/);
  });
});
