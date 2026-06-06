// Smoke test (Phase 0 deliverable 1). Loads the home page and asserts the
// document title contains "Cortex" — confirms Playwright + Vite are wired
// before the substantive trace-shape tests run.

import { test, expect } from "@playwright/test";

test("home page loads and document title mentions the site", async ({ page }) => {
  await page.goto("/");
  // The static <title> in client/index.html is the author's name. Anchor on a
  // stable substring rather than the full string so a copy edit doesn't break
  // the smoke test.
  await expect(page).toHaveTitle(/Aniket Kakde|Cortex/);
  // Root mount point exists — proves index.html was served, not a 404.
  await expect(page.locator("#root")).toBeAttached();
});
