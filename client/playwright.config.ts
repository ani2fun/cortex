// Playwright config — Phase 0 foundations (ADR-0025).
//
// Two projects:
//   `fixtures` — fast, deterministic tests that load pre-computed trace JSON
//                fixtures from `test/e2e/fixtures/` via the render harness at
//                `/test-render.html` and assert the d3 renderer mounts. NO
//                backend required; Vite alone is enough. These are the Phase 0
//                "3 hardest-shape" baselines that gate the trace format.
//   `chapters` — Slice-1 regression baselines that navigate to real
//                /cortex/... chapter URLs. Need the full bin/dev stack
//                (backend + Keycloak optional). Skipped automatically when
//                the backend isn't responding so CI doesn't false-fail.
//
// Both projects spawn Vite via `webServer`. The chapter project additionally
// expects the backend on :8080; tests check `/api/health` in setup and
// `test.skip()` if it's unreachable.

import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./test/e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? "github" : "list",
  // Snapshot baselines live alongside the spec (Playwright's default).
  // Per-pixel tolerance lets fonts vary slightly between platforms — Phase 5
  // will tighten this with a pinned headless-browser image.
  expect: {
    toHaveScreenshot: { maxDiffPixels: 100, threshold: 0.2 },
    toMatchSnapshot:  { maxDiffPixels: 100, threshold: 0.2 },
  },
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    {
      name: "fixtures",
      testMatch: /(home|trace-shapes)\.spec\.ts$/,
      use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } },
    },
    {
      name: "chapters",
      testMatch: /stack-baselines\.spec\.ts$/,
      use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    stdout: "pipe",
    stderr: "pipe",
    timeout: 120_000,
  },
});
