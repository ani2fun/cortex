// Contract tests for the bespoke Fenwick / BIT renderer (ADR-0027, renderer #11).
// Source-AST checks; the responsibility staircase is verified in a real browser by
// the `fenwick` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { fenwickRenderer } from "./fenwick-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const fenSrc = readFileSync(join(here, "fenwick-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("fenwick-renderer — ADR-0027 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof fenwickRenderer).toBe("function");
    expect(fenSrc).toMatch(/defineRenderer\(\{/);
    expect(fenSrc).toMatch(/className:\s*["']fenwick-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is registered per-card under `fenwick`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*fenwickRenderer\s*\}\s*from\s*["']\.\/fenwick-renderer["']/,
    );
    expect(indexSrc).toMatch(/fenwick:\s*fenwickRenderer/);
  });

  it("derives the staircase geometry from each cell's slot (the Fenwick index)", () => {
    // Lowbit (i & -i) drives both the span width and the level.
    expect(fenSrc).toMatch(/i & -i/);
    // Level = log2(lowbit); a bigger lowbit sits higher up the staircase.
    expect(fenSrc).toMatch(/Math\.log2\(lowbit\(i\)\)/);
    // Skip the dummy slot 0 (the unused 1-indexing sentinel `tree[0]`).
    expect(fenSrc).toMatch(/node\.slot >= 1/);
    // A bar spans its (lo,i] columns: grid-column (lo+1) .. (i+1).
    expect(fenSrc).toMatch(/gridColumn\s*=\s*`\$\{lo \+ 1\} \/ \$\{i \+ 1\}`/);
    // Half-open responsibility-range label (lo,i].
    expect(fenSrc).toMatch(/\(\$\{lo\},\$\{i\}\]/);
    // The `i` index-cursor marks the cell being visited each step.
    expect(fenSrc).toMatch(/step\.cursor\.map\(\(c\) => c\.target\)/);
    // Honors the per-pointer ROLE colour (MarkerColors canon) — tints the cursor
    // cell in the emitted hue (`i` → blue) instead of a hardcoded accent.
    expect(fenSrc).toMatch(/setProperty\(["']--fen-cursor-color["']/);
  });
});
