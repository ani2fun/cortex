// Contract tests for the bespoke Array renderer (ADR-0027) — the default for array-1d cards.
// Source-AST checks; the cell row is verified in a real browser by the `array` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { arrayRenderer } from "./array-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const arrSrc = readFileSync(join(here, "array-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("array-renderer — ADR-0027 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof arrayRenderer).toBe("function");
    expect(arrSrc).toMatch(/defineRenderer\(\{/);
    expect(arrSrc).toMatch(/className:\s*["']array-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is the DEFAULT renderer for array-1d cards (no viz-kind needed)", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*arrayRenderer\s*\}\s*from\s*["']\.\/array-renderer["']/,
    );
    // Registered under the layoutKind fallback (not RENDERERS, which is viz-kind-keyed).
    expect(indexSrc).toMatch(/LAYOUT_RENDERERS[\s\S]*?"array-1d":\s*arrayRenderer/);
    // pickRenderer falls back to LAYOUT_RENDERERS[layoutKind] when no structureType matches.
    expect(indexSrc).toMatch(/LAYOUT_RENDERERS\[layoutKind\]\s*\?\?\s*renderGraph/);
  });

  it("draws the cell row with pointer carets + an index row", () => {
    expect(arrSrc).toMatch(/array-renderer__cells/);
    expect(arrSrc).toMatch(/array-renderer__cell-value/);
    // Index-cursors become pointer carets, tinted by the cursor's role colour.
    expect(arrSrc).toMatch(/array-renderer__pointer/);
    expect(arrSrc).toMatch(/setProperty\(["']--pcolor["']/);
    expect(arrSrc).toMatch(/step\.cursor/);
    // Changed cells ring; an index row beneath.
    expect(arrSrc).toMatch(/array-renderer__cell--changed/);
    expect(arrSrc).toMatch(/array-renderer__index/);
  });
});
