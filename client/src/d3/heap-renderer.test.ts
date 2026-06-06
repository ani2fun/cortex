// Contract tests for the bespoke Heap renderer (ADR-0027, renderer #5).
// Source-AST checks; the array→tree rendering is verified in a real browser by
// the `heap` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { heapRenderer } from "./heap-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const hSrc = readFileSync(join(here, "heap-renderer.ts"), "utf8");

describe("heap-renderer — ADR-0027 contract", () => {
  it("is a RendererFn that delegates to renderGraph + treeLayout", () => {
    expect(typeof heapRenderer).toBe("function");
    expect(hSrc).toMatch(/renderGraph\(/);
    expect(hSrc).toMatch(/treeLayout/);
  });

  it("is registered per-card under `heap`", () => {
    expect(indexSrc).toMatch(/import\s*\{\s*heapRenderer\s*\}\s*from\s*["']\.\/heap-renderer["']/);
    expect(indexSrc).toMatch(/heap:\s*heapRenderer/);
  });

  it("synthesises binary-tree edges from array indices", () => {
    // parent i → children 2i+1 (left) / 2i+2 (right).
    expect(hSrc).toMatch(/2 \* slot \+ 1/);
    expect(hSrc).toMatch(/2 \* slot \+ 2/);
    expect(hSrc).toMatch(/label: "left"/);
    expect(hSrc).toMatch(/label: "right"/);
  });
});
