// Contract tests for the bespoke Graph renderer (ADR-0027, renderer #3).
// Source-AST checks (DOM-less vitest env); the synthesised-edge rendering is
// verified in a real browser by the `graph-kind` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { graphRenderer } from "./graph-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const gSrc = readFileSync(join(here, "graph-renderer.ts"), "utf8");

describe("graph-renderer — ADR-0027 contract", () => {
  it("exports a RendererFn that delegates to renderGraph", () => {
    expect(typeof graphRenderer).toBe("function");
    expect(gSrc).toMatch(/renderGraph\(/);
    expect(gSrc).toMatch(/graphLayout/);
  });

  it("is registered as a WHOLE-GRAPH renderer under `graph`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*graphRenderer\s*\}\s*from\s*["']\.\/graph-renderer["']/,
    );
    expect(indexSrc).toMatch(/graph:\s*graphRenderer/);
  });

  it("synthesises direct edges by folding away adjacency cells", () => {
    // The cross-structure walk: drop kind==="cell" nodes, compose node→cell→target.
    expect(gSrc).toMatch(/kind === "cell"/);
    expect(gSrc).toMatch(/cellTargets/);
    // Relabels nodes from their `id` field (adapter labels them "GraphNode").
    expect(gSrc).toMatch(/m\.name === "id"/);
  });
});
