// Drift guard for the viz render types (ADR-0026).
//
// `src/d3/types.generated.ts` is generated from `viz-schema.yaml` by
// `npm run codegen:viz` (openapi-typescript). This test regenerates into a
// temp file and asserts it matches the committed copy — so a `viz-schema.yaml`
// edit without a matching `npm run codegen:viz` re-run fails in the normal
// test suite, the same way the old `bin/gen-ts-types.py` guard worked.
//
// The Scala side is guarded separately by `VizSchemaConformanceSpec`
// (shared/.../viz), which asserts the hand-written VizGraph.scala case-class
// fields match the same yaml. Together: Scala ↔ yaml ↔ TS can't silently drift.

import { describe, it, expect } from "vitest";
import { spawnSync } from "node:child_process";
import { readFileSync, mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const clientRoot = resolve(here, "..", "..");
const committed = join(clientRoot, "src", "d3", "types.generated.ts");
const schema = resolve(clientRoot, "..", "viz-schema.yaml");

describe("viz-schema.yaml → types.generated.ts drift guard", () => {
  it("regenerated TS matches the committed file", () => {
    const tmp = join(mkdtempSync(join(tmpdir(), "viz-codegen-")), "out.ts");
    const run = spawnSync(
      "npx",
      ["openapi-typescript", schema, "-o", tmp],
      { cwd: clientRoot, encoding: "utf8" },
    );
    expect(run.status, run.stderr).toBe(0);

    const regenerated = readFileSync(tmp, "utf8");
    const onDisk = readFileSync(committed, "utf8");
    expect(
      regenerated,
      "types.generated.ts is stale — run `npm run codegen:viz` and commit the result",
    ).toBe(onDisk);

    // Pin the invariants slice 1 (and earlier) depend on, so a schema edit
    // that loosens them is loud here as well as in the diff.
    expect(onDisk).toMatch(/slot: number \| null/);
    expect(onDisk).toMatch(/structureType: string \| null/);
    expect(onDisk).toMatch(/cardId: string/);
    expect(onDisk).toMatch(/layoutKind: string/);
  });
});
