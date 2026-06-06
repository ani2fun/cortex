// Vitest bench config — kept separate from `vitest.config.mjs` so the unit
// suite (`npm test`) and the bench suite (`npm run test:e2e:bench`) don't
// share `include` globs. Phase 0 ships exactly one bench
// (`test/bench/decode-latency.bench.ts`); Phase 6 wires the perf budget
// CI gate against the numbers this file produces.
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["test/bench/**/*.bench.ts"],
    // Each bench runs many iterations internally; one global retry is enough.
    retry: 0,
  },
});
