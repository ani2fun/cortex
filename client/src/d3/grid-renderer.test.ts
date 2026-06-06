// Contract tests for the bespoke 2-D matrix renderer (array-2d). Source-AST checks; the grid
// is verified in a real browser by the renderer gallery's `viz=array-2d` block.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { gridRenderer } from "./grid-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const gridSrc = readFileSync(join(here, "grid-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("grid-renderer — 2-D matrix contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof gridRenderer).toBe("function");
    expect(gridSrc).toMatch(/defineRenderer\(\{/);
    expect(gridSrc).toMatch(/className:\s*["']matrix-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("renders WHOLE-GRAPH for array-2d cards (the adapter splits a list-of-lists per row)", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*gridRenderer\s*\}\s*from\s*["']\.\/grid-renderer["']/,
    );
    // pickWholeGraphRenderer returns gridRenderer when any node is tagged array-2d.
    expect(indexSrc).toMatch(/layoutKind\s*===\s*["']array-2d["'][\s\S]*?gridRenderer/);
    // Also wired as the per-card layoutKind default, for completeness.
    expect(indexSrc).toMatch(/LAYOUT_RENDERERS[\s\S]*?"array-2d":\s*gridRenderer/);
  });

  it("derives (row, col) from cell→cell edges the same way grid-layout does", () => {
    // Outer cell's slot = row, via cell→cell edges; inner cell's slot = column.
    expect(gridSrc).toMatch(/step\.edges/);
    expect(gridSrc).toMatch(/rowOfInner/);
    expect(gridSrc).toMatch(/arrayIdOf/);
    expect(gridSrc).toMatch(/outerArrayIds/);
  });

  it("draws a column-index header, per-row labels, boxed cells + changed/active states", () => {
    expect(gridSrc).toMatch(/matrix-renderer__col-label/);
    expect(gridSrc).toMatch(/matrix-renderer__row-label/);
    expect(gridSrc).toMatch(/matrix-renderer__cell\b/);
    expect(gridSrc).toMatch(/matrix-renderer__cell--changed/);
    expect(gridSrc).toMatch(/matrix-renderer__cell--active/);
  });
});
