// Contract tests for the bespoke Skip-list renderer (ADR-0027, renderer #12).
// Source-AST checks; the multi-level grid is verified in a real browser by the
// `skiplist` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { skiplistRenderer } from "./skiplist-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const sklSrc = readFileSync(join(here, "skiplist-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("skiplist-renderer — ADR-0027 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof skiplistRenderer).toBe("function");
    expect(sklSrc).toMatch(/defineRenderer\(\{/);
    expect(sklSrc).toMatch(/className:\s*["']skiplist-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is registered per-card under `skiplist`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*skiplistRenderer\s*\}\s*from\s*["']\.\/skiplist-renderer["']/,
    );
    expect(indexSrc).toMatch(/skiplist:\s*skiplistRenderer/);
  });

  it("draws the multi-level grid: columns by key, a node per level it reaches", () => {
    // Each node's top express lane comes from `level` meta; value from label / `value` meta.
    expect(sklSrc).toMatch(/metaNum\(node, "level"\)/);
    expect(sklSrc).toMatch(/m\.name === "value"/);
    // Columns left→right in sorted key order (the level-0 chain).
    expect(sklSrc).toMatch(/cols\.sort\(\(a, b\) => a\.value - b\.value\)/);
    // Present at this level → a node; otherwise a thin gap line.
    expect(sklSrc).toMatch(/c\.level >= lvl/);
    expect(sklSrc).toMatch(/skiplist-renderer__gap/);
    // Only the `cur` search pointer marks the active column (head is just the anchor)…
    expect(sklSrc).toMatch(/c\.name === "cur"/);
    // …tinted in the emitted role colour (MarkerColors canon) — one palette.
    expect(sklSrc).toMatch(/setProperty\(["']--node-color["']/);
  });
});
