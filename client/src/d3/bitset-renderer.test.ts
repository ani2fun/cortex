// Contract tests for the bespoke Bitset renderer (ADR-0027, renderer #8).
// Source-AST checks; the set/clear bit row is verified in a real browser by the
// `bitset` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { bitsetRenderer } from "./bitset-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const bitsetSrc = readFileSync(join(here, "bitset-renderer.ts"), "utf8");
const sdkSrc = readFileSync(join(here, "renderer-sdk.ts"), "utf8");

describe("bitset-renderer — ADR-0027 contract", () => {
  it("is built on the Renderer SDK, which tags the content block `data-card-content`", () => {
    expect(typeof bitsetRenderer).toBe("function");
    expect(bitsetSrc).toMatch(/defineRenderer\(\{/);
    expect(bitsetSrc).toMatch(/className:\s*["']bitset-renderer["']/);
    expect(sdkSrc).toMatch(/setAttribute\(["']data-card-content["']/);
  });

  it("is registered per-card under `bitset`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*bitsetRenderer\s*\}\s*from\s*["']\.\/bitset-renderer["']/,
    );
    expect(indexSrc).toMatch(/bitset:\s*bitsetRenderer/);
  });

  it("reads each cell as a bit: value 1 → set, and renders bit index + popcount", () => {
    // Set bits (value "1") get the `--set` modifier — the defining set/clear visual.
    expect(bitsetSrc).toMatch(/node\.label === "1"/);
    expect(bitsetSrc).toMatch(/bitset-renderer__bit--set/);
    // Popcount summary counts the set bits.
    expect(bitsetSrc).toMatch(/bitset-renderer__summary/);
    expect(bitsetSrc).toMatch(/set/);
    // Cells iterate low-index-first (bit 0 on the left).
    expect(bitsetSrc).toMatch(/\(a\.slot as number\) - \(b\.slot as number\)/);
    // The touched bit is flagged via the per-step `changed` diff (no index cursor).
    expect(bitsetSrc).toMatch(/bitset-renderer__bit--changed/);
  });
});
