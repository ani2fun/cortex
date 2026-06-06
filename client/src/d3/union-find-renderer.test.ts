// Contract tests for the bespoke Union-Find renderer (ADR-0027, renderer #6).
// Source-AST checks; the parent-array → forest rendering is verified in a real
// browser by the `union-find` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { unionFindRenderer } from "./union-find-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const ufSrc = readFileSync(join(here, "union-find-renderer.ts"), "utf8");

describe("union-find-renderer — ADR-0027 contract", () => {
  it("is a RendererFn that delegates to renderGraph + unionFindLayout", () => {
    expect(typeof unionFindRenderer).toBe("function");
    expect(ufSrc).toMatch(/renderGraph\(/);
    expect(ufSrc).toMatch(/unionFindLayout/);
  });

  it("is registered per-card under `union-find`", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*unionFindRenderer\s*\}\s*from\s*["']\.\/union-find-renderer["']/,
    );
    expect(indexSrc).toMatch(/"union-find":\s*unionFindRenderer/);
  });

  it("reads each cell as a parent pointer and synthesises parent / self edges", () => {
    // value === slot → root (self-loop); otherwise a child→parent arc.
    expect(ufSrc).toMatch(/parseInt\(n\.label, 10\)/);
    expect(ufSrc).toMatch(/parent === n\.slot/);
    // Root cells become kind "root" (the layout's self-loop branch + `root` sub-label).
    expect(ufSrc).toMatch(/kind:\s*isRoot\s*\?\s*"root"/);
    // The circle is relabelled to its own slot (element identity), not the stored parent.
    expect(ufSrc).toMatch(/label:\s*String\(n\.slot\)/);
  });
});
