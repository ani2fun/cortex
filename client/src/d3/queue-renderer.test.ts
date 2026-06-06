// Contract tests for the bespoke Queue/Deque renderer (ADR-0027, renderer #4).
// Source-AST checks (DOM-less vitest env); the FIFO row is verified in a real
// browser by the `queue` Playwright fixture.

import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { queueRenderer } from "./queue-renderer";

const here = dirname(fileURLToPath(import.meta.url));
const indexSrc = readFileSync(join(here, "index.ts"), "utf8");
const qSrc = readFileSync(join(here, "queue-renderer.ts"), "utf8");

describe("queue-renderer — ADR-0027 contract", () => {
  it("is a RendererFn built on the SDK", () => {
    expect(typeof queueRenderer).toBe("function");
    expect(qSrc).toMatch(/defineRenderer\(\{/);
    expect(qSrc).toMatch(/className:\s*["']queue-renderer["']/);
  });

  it("is registered per-card for both queue and deque", () => {
    expect(indexSrc).toMatch(
      /import\s*\{\s*queueRenderer\s*\}\s*from\s*["']\.\/queue-renderer["']/,
    );
    expect(indexSrc).toMatch(/queue:\s*queueRenderer/);
    expect(indexSrc).toMatch(/deque:\s*queueRenderer/);
  });

  it("orders cells front-first and marks head/tail ends", () => {
    // slot 0 = front (head, leftmost); ascending slot → tail (rightmost).
    expect(qSrc).toMatch(/\(a\.slot as number\) - \(b\.slot as number\)/);
    expect(qSrc).toMatch(/queue-renderer__cell--head/);
    expect(qSrc).toMatch(/queue-renderer__cell--tail/);
  });

  it("renders a DISTINCT deque variant when structureType is deque", () => {
    // Branches on the deque structureType — bidirectional hint, front/back labels,
    // and one symmetric end colour (vs the queue's asymmetric blue/bordeaux).
    expect(qSrc).toMatch(/structureType\s*===\s*["']deque["']/);
    expect(qSrc).toMatch(/queue-renderer--deque/);
    expect(qSrc).toMatch(/DEQUE_END_COLOR/);
    // front/back labels replace head/tail when isDeque.
    expect(qSrc).toMatch(/isDeque\s*\?\s*["']front["']/);
    expect(qSrc).toMatch(/isDeque\s*\?\s*["']back["']/);
  });
});
