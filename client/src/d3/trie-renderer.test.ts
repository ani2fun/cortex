// Contract tests for the bespoke Trie renderer (ADR-0027, renderer #7).
// Source-AST checks; the prefix-tree rendering (char-labelled nodes, terminal
// double-rings, folded-away Dict scaffolding) is verified in a real browser by
// the `trie` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { trieRenderer } from "./trie-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const trieSrc = readFileSync(join(here, "trie-renderer.ts"), "utf8");
const graphRenderSrc = readFileSync(join(here, "graph-render.ts"), "utf8");

describe("trie-renderer — ADR-0027 contract", () => {
  it("is a RendererFn that delegates to renderGraph + trieLayout", () => {
    expect(typeof trieRenderer).toBe("function");
    expect(trieSrc).toMatch(/renderGraph\(/);
    expect(trieSrc).toMatch(/trieLayout/);
  });

  it("is registered as a WHOLE-GRAPH renderer under `trie`", () => {
    expect(indexSrc).toMatch(/import\s*\{\s*trieRenderer\s*\}\s*from\s*["']\.\/trie-renderer["']/);
    // Registered in WHOLE_GRAPH_RENDERERS (cross-structure: nodes connect through a Dict).
    const block = indexSrc.slice(
      indexSrc.indexOf("WHOLE_GRAPH_RENDERERS"),
      indexSrc.indexOf("WHOLE_GRAPH_RENDERERS") + 260,
    );
    expect(block).toMatch(/trie:\s*trieRenderer/);
  });

  it("composes parent→child edges through the children-Dict entries", () => {
    // Entry nodes (kind "entry") are the Dict scaffolding; the renderer folds them
    // away into direct parent→child edges and drops them from the node set.
    expect(trieSrc).toMatch(/kind === "entry"/);
    expect(trieSrc).toMatch(/entryParent/);
    expect(trieSrc).toMatch(/entryChild/);
    // The char on the incoming edge becomes the node's label (root → "•").
    expect(trieSrc).toMatch(/charInto/);
  });

  it("marks `is_end` nodes terminal so renderGraph draws the double-ring", () => {
    expect(trieSrc).toMatch(/is_end/);
    expect(trieSrc).toMatch(/"terminal"\s*:\s*"trie"/);
    // graph-render applies the --terminal class + suppresses the synthetic kind text.
    expect(graphRenderSrc).toMatch(/viz-graph__node--terminal/);
    expect(graphRenderSrc).toMatch(/n\.kind === "terminal"/);
  });
});
