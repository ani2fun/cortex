// Phase 0 deliverable 7 (gap 3) — Slice-1 stack chapter baselines (ADR-0025).
//
// Three representative `viz-kind=stack` chapters that shipped in Slice 1, each
// pinned as a Playwright baseline so Phases 1-3 (schema codegen, server-side
// adapter move, HeapToGraph decomposition) can't silently regress them.
//
// THE PHASE-0 INTENT IS PATTERN, NOT FULL CI COVERAGE: the chapters call
// /api/run (Piston) for real traces, which needs the full `bin/dev` stack up
// (server + Piston reachable). When the backend isn't responding the tests
// `test.skip()` cleanly so the suite still passes locally without the stack.
// Phase 5 expands this to every viz-kind chapter and adds the request mocking
// + screenshot tolerance tuning needed for reliable CI runs.
//
// Slugs (cross-checked against content/cortex/data-structures-and-algorithms/):
//   stack-inversion              — Group A canonical sweep target
//                                  pattern-reversal/02-problems/01-stack-inversion
//   parentheses-checker          — Group A different stack pattern
//                                  pattern-sequence-validation/02-problems/01-parentheses-checker
//   succeeding-inferior-element  — Group C, root-switched from next_smaller to stack
//                                  pattern-next-closest-occurrence/02-problems/02-succeeding-inferior-element

import { test, expect, type Page } from "@playwright/test";

const SECTION = "02-linear-structures/05-stack";
const CHAPTERS = [
  {
    slug: `${SECTION}/08-pattern-reversal/02-problems/01-stack-inversion`,
    name: "stack-inversion",
  },
  {
    slug: `${SECTION}/11-pattern-sequence-validation/02-problems/01-parentheses-checker`,
    name: "parentheses-checker",
  },
  {
    slug: `${SECTION}/10-pattern-next-closest-occurrence/02-problems/02-succeeding-inferior-element`,
    name: "succeeding-inferior-element",
  },
] as const;

/**
 * Ping the dev backend; skip the whole spec if it's not up. Phase 0 deliberately
 * skips rather than fails so the suite stays green locally without `bin/dev`.
 */
async function backendUp(page: Page): Promise<boolean> {
  try {
    const res = await page.request.get("/api/health", { timeout: 2_000 });
    return res.ok();
  } catch {
    return false;
  }
}

test.describe("Slice-1 stack chapter baselines", () => {
  test.beforeEach(async ({ page }) => {
    const ok = await backendUp(page);
    test.skip(!ok, "backend not reachable on :8080 — start `./bin/dev` to run these");
  });

  for (const chapter of CHAPTERS) {
    test(`${chapter.name} renders the stack bespoke renderer`, async ({ page }) => {
      const errors: string[] = [];
      page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));

      await page.goto(`/cortex/data-structures-and-algorithms/${chapter.slug}`);

      // Wait for the Visualise button to mount on the runnable code block. The
      // exact aria-label matches the stack-renderer DOM walk file
      // (client/test/renderers/stack.dom.md).
      const visualiseBtn = page.locator('button[aria-label="Visualise code"]').first();
      await visualiseBtn.waitFor({ state: "visible", timeout: 20_000 });
      await visualiseBtn.click();

      // Modal mounts.
      await expect(page.locator(".algolens-grid").first()).toBeVisible({ timeout: 15_000 });

      // The trace runs against Piston — give it a generous timeout, then wait
      // for the bespoke stack-renderer block (i.e. trace done + dispatched).
      await expect(page.locator(".stack-renderer").first()).toBeVisible({ timeout: 30_000 });

      // Jump to the end of the trace so the screenshot lands on the terminal
      // shape (full reversed stack, balanced parens, completed sweep).
      // Inside the modal, the Stepper exposes its controller via the .stepper
      // root's `data-step-count` attribute; the simplest portable path is to
      // click "End" if present, else click "Next" until disabled.
      const endBtn = page.locator('button[aria-label="Jump to last step"]');
      if (await endBtn.count() > 0) {
        await endBtn.first().click();
      } else {
        const nextBtn = page.locator('button[aria-label="Next step"]').first();
        // Cap iterations defensively so a busted Stepper doesn't loop forever.
        for (let i = 0; i < 200; i += 1) {
          if (!(await nextBtn.isEnabled())) break;
          await nextBtn.click();
        }
      }
      // Yield once for layout to settle.
      await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

      expect(errors, `console / page errors during ${chapter.name}: ${errors.join(" | ")}`)
        .toEqual([]);

      await expect(page.locator(".algolens-grid").first()).toHaveScreenshot(
        `${chapter.name}.png`,
        { animations: "disabled" },
      );
    });
  }
});
