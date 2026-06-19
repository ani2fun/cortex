// Smoke test: the renderer entry module and its whole import graph
// (index -> graph-render -> stepper -> graph-layout / tree-layout -> d3) load
// cleanly. `renderWidget` itself needs a DOM, so it is not invoked here — this
// only guards against a broken import or syntax error in that module graph.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { renderWidget } from "./index";

describe("renderWidget — module wiring", () => {
  it("is exported as a function", () => {
    expect(typeof renderWidget).toBe("function");
  });
});

// Phase A (bespoke static fences): a `d3 widget=<name>` fence carries no
// structureType, so `pickRenderer` falls back to LAYOUT_RENDERERS[layoutHint].
// These slot/shape structures must be wired there so static catalog/lesson
// fences render with their bespoke widget instead of generic circles. Source-AST
// check (DOM render is covered by the Playwright fixtures).
describe("LAYOUT_RENDERERS — bespoke static-fence defaults", () => {
  const here = dirname(fileURLToPath(import.meta.url));
  const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
  const block =
    indexSrc.match(/const LAYOUT_RENDERERS[^{]*\{([\s\S]*?)\n\};/)?.[1] ?? "";

  it("extracts a non-empty LAYOUT_RENDERERS block", () => {
    expect(block.length).toBeGreaterThan(0);
  });

  it.each([
    ["stack", "stackRenderer"],
    ["queue", "queueRenderer"],
    ["deque", "queueRenderer"],
    ["bitset", "bitsetRenderer"],
    ["skiplist", "skiplistRenderer"],
    ["fenwick", "fenwickRenderer"],
    ["segment-tree", "segmentTreeRenderer"],
  ])("routes layoutKind %s → %s as a default renderer", (kind, renderer) => {
    expect(block).toMatch(
      new RegExp(`["']?${kind}["']?:\\s*${renderer}`),
    );
  });
});
