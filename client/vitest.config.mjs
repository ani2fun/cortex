// Vitest config — deliberately separate from vite.config.mjs. Vitest picks
// this up in preference, so the test run never loads @scala-js/vite-plugin-
// scalajs (which would shell out to sbt). The d3 layout functions under test
// are pure maths — no DOM, no Scala.js — so the default Node environment is
// all they need.
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
});
