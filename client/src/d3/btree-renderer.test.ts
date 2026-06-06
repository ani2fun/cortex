// Contract tests for the bespoke B-tree renderer (ADR-0029, cross-structure whole-graph).
// Source-AST checks; the n-ary multi-key tree + child pointers are verified in a real
// browser against the B-tree chapter's `viz-kind=btree` block.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { btreeRenderer } from "./btree-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const btSrc = readFileSync(join(here, "btree-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("btree-renderer — ADR-0029 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof btreeRenderer).toBe("function");
    expect(btSrc).toMatch(/defineRenderer\(\{/);
    expect(btSrc).toMatch(/className:\s*["']btree-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is registered as a WHOLE-GRAPH renderer under `btree` (gets the full graph, not per-card)", () => {
    expect(indexSrc).toMatch(/import\s*\{\s*btreeRenderer\s*\}\s*from\s*["']\.\/btree-renderer["']/);
    // Must live in WHOLE_GRAPH_RENDERERS (dispatched before the multi-card split that
    // would otherwise shatter the through-Arr parent→child links).
    const whole = indexSrc.slice(indexSrc.indexOf("WHOLE_GRAPH_RENDERERS"));
    expect(whole).toMatch(/btree:\s*btreeRenderer/);
  });

  it("reconstructs each BNode's key row + ordered children from the keys/children Arr edges", () => {
    // A node's keys come through `"keys"` edges; its children through `"children"` edges
    // then a second `""` hop to the child BNode.
    expect(btSrc).toMatch(/e\.label === "keys"/);
    expect(btSrc).toMatch(/e\.label === "children"/);
    expect(btSrc).toMatch(/kind === "BNode"/);
    // Ordered by the cell's slot (the list index).
    expect(btSrc).toMatch(/\.slot/);
    expect(btSrc).toMatch(/\.sort\(/);
  });

  it("draws node boxes of key cells + SVG child pointers, and flashes new keys", () => {
    expect(btSrc).toMatch(/createElementNS/);
    expect(btSrc).toMatch(/btree-renderer__edge/);
    expect(btSrc).toMatch(/btree-renderer__node/);
    expect(btSrc).toMatch(/btree-renderer__key/);
    // New / changed key diff + cursor tint.
    expect(btSrc).toMatch(/step\.highlight/);
    expect(btSrc).toMatch(/btree-renderer__key--new/);
    expect(btSrc).toMatch(/--node-color/);
    expect(btSrc).toMatch(/cardCursor/);
  });

  it("tints the cursor'd node but draws NO on-node name badge (the ArrowLayer owns the label)", () => {
    // The shared ArrowLayer routes a labelled pointer to the node's top-centre; an
    // on-node badge there collided with the arrow's tip. Keep tint-only — regression guard.
    expect(btSrc).toMatch(/btree-renderer__node--cursor/);
    expect(btSrc).not.toMatch(/btree-renderer__badge/);
  });
});
