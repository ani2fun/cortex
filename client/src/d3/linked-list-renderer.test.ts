// Contract tests for the bespoke Linked-list renderer (ADR-0027, renderer #9 —
// rewritten to the design's horizontal boxes). Source-AST checks, matching the
// repo's renderer-test convention (vitest runs in node — no DOM environment).
//
// The actual DOM output (4 horizontal value boxes, head/cur carets, curved next
// arrows, ∅ terminator, no generic circles) was verified by rendering the module
// directly in the browser over a list-single VizGraph; a `linked-list` Playwright
// fixture covers it in CI once the trace fixtures are regenerated.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { linkedListRenderer } from "./linked-list-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const llSrc = readFileSync(join(here, "linked-list-renderer.ts"), "utf8");
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");

describe("linked-list-renderer — bespoke horizontal boxes (ADR-0027)", () => {
  it("is a defineRenderer drawing the design's list-renderer blocks", () => {
    expect(typeof linkedListRenderer).toBe("function");
    expect(llSrc).toMatch(/defineRenderer\(/);
    expect(llSrc).toMatch(/className:\s*"list-renderer"/);
    // boxed value nodes, curved next edges, a ∅ terminator
    expect(llSrc).toMatch(/list-renderer__node/);
    expect(llSrc).toMatch(/list-renderer__edge/);
    expect(llSrc).toMatch(/list-renderer__null/);
  });

  it("reads head/cur cursors for the carets + the active node tint", () => {
    expect(llSrc).toMatch(/name === "head"/);
    expect(llSrc).toMatch(/name === "cur"/);
    expect(llSrc).toMatch(/list-renderer__head-label/);
    expect(llSrc).toMatch(/list-renderer__cur/);
    expect(llSrc).toMatch(/list-renderer__node--active/);
  });

  it("orders by the next chain + flags the doubly variant on a prev edge", () => {
    expect(llSrc).toMatch(/=== "next"/);
    expect(llSrc).toMatch(/=== "prev"/);
    expect(llSrc).toMatch(/list-renderer--double/);
  });

  it("no longer delegates to the generic renderGraph + linkedListLayout (vertical circles)", () => {
    // No delegation CALL/IMPORT — the header comment may still mention them historically.
    expect(llSrc).not.toMatch(/renderGraph\(/);
    expect(llSrc).not.toMatch(/import\s*\{[^}]*linkedListLayout/);
  });

  it("is registered per-card for both list-single and list-double", () => {
    expect(indexSrc).toMatch(/"list-single":\s*linkedListRenderer/);
    expect(indexSrc).toMatch(/"list-double":\s*linkedListRenderer/);
  });

  it("draws 3-compartment nodes (PREV | VALUE | NEXT) + a doubly next/prev legend", () => {
    expect(llSrc).toMatch(/list-renderer__field--prev/);
    expect(llSrc).toMatch(/list-renderer__field--next/);
    expect(llSrc).toMatch(/list-renderer__val/);
    // The PREV cell is doubly-only; the legend disambiguates the two edge colours.
    expect(llSrc).toMatch(/if \(doubly\)[\s\S]*?field--prev/);
    expect(llSrc).toMatch(/list-renderer__legend/);
  });
});
