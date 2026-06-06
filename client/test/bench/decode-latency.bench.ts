// Phase 0 deliverable 6 / gap 2 (ADR-0025) — decode-path latency baseline.
//
// MarkedTrace.scala (Scala.js) decodes the tracer-emitted JSON via
// `js.JSON.parse(json)` plus a hand-coded structural walk that produces
// `HeapTrace`. Phase 2 will replace `js.JSON.parse` + the walk with a
// `circe.decode[HeapTrace](json)` call — historically that's 10-40× slower
// in the browser. The plan loops back to keep `js.JSON.parse` (with codegen-
// generated runtime validators) if Phase 2 blows past Phase 6's budget.
//
// This bench measures the CURRENT cost so Phase 2 has a number to compare
// against. The bench runs in Node (vitest default) which is structurally
// equivalent to the browser path: V8's JSON.parse implementation is the
// same engine code; the walk is plain JS. Both production browsers (Chrome,
// Firefox, Safari) use comparable native parsers.
//
// The walk mirrors the shape of `decodeTrace` → `decodeStep` → `decodeFrame`
// → `decodeValue` / `decodeObject` in MarkedTrace.scala — visiting every
// step's heap entries and every entry's fields/items. The bench INPUT is a
// programmatically-synthesised 500-step trace at ~30 KB (per the plan
// specification); a more realistic mix of small/medium/large traces is
// Phase 6 follow-up work.

import { bench, describe } from "vitest";

// ─────────────────────────────────────────────────────────────────────────
// Synthetic trace generator — produces a {steps, truncated} JSON string of
// the shape `MarkedTrace.parse` expects (post-marker extraction).
// ─────────────────────────────────────────────────────────────────────────

interface SyntheticConfig {
  /** Number of steps in the trace. */
  steps: number;
  /** Approx. number of heap objects per step. */
  heapObjectsPerStep: number;
  /** Approx. number of items per heap Arr. */
  itemsPerArr: number;
}

function synthTrace(cfg: SyntheticConfig): string {
  const heap: Record<string, unknown> = {};
  for (let h = 0; h < cfg.heapObjectsPerStep; h += 1) {
    const items = Array.from({ length: cfg.itemsPerArr }, (_, i) => ({
      ref: `node_${h}_${i}`,
    }));
    heap[`arr_${h}`] = { type: "list", items };
    for (let i = 0; i < cfg.itemsPerArr; i += 1) {
      heap[`node_${h}_${i}`] = {
        type: "instance",
        cls: "Node",
        fields: { val: i, label: `n${h}_${i}` },
      };
    }
  }
  const stepTemplate = {
    line: 1,
    event: "line",
    frames: [
      { fn: "algorithm", locals: { i: 0, n: cfg.heapObjectsPerStep } },
    ],
    heap,
  };
  const steps = Array.from({ length: cfg.steps }, (_, i) => ({
    ...stepTemplate,
    line: 1 + (i % 20),
  }));
  return JSON.stringify({ steps, truncated: false });
}

// ─────────────────────────────────────────────────────────────────────────
// Decode walk — structurally equivalent to MarkedTrace.scala's decodeTrace
// pipeline; visits every heap object and every Arr item / Dict entry so the
// per-step cost matches what production pays.
// ─────────────────────────────────────────────────────────────────────────

interface ParsedHeapObject {
  type?: string;
  cls?: string;
  fields?: Record<string, unknown>;
  items?: unknown[];
  entries?: unknown[];
}
interface ParsedStep {
  line?: number;
  event?: string;
  frames?: Array<{ fn?: string; locals?: Record<string, unknown> }>;
  heap?: Record<string, ParsedHeapObject>;
}
interface ParsedTrace {
  steps?: ParsedStep[];
  truncated?: boolean;
}

function decode(json: string): { stepCount: number; heapCount: number } {
  const root = JSON.parse(json) as ParsedTrace;
  let stepCount = 0;
  let heapCount = 0;
  const steps = root.steps ?? [];
  for (const s of steps) {
    stepCount += 1;
    const frames = s.frames ?? [];
    for (const f of frames) {
      const locals = f.locals ?? {};
      // Touch each local — simulates the type-switch walk in decodeValue.
      for (const k of Object.keys(locals)) {
        const v = locals[k];
        if (typeof v === "number") void v;
        else if (typeof v === "string") void v;
        else if (typeof v === "boolean") void v;
        else void v;
      }
    }
    const heap = s.heap ?? {};
    for (const id of Object.keys(heap)) {
      heapCount += 1;
      const obj = heap[id];
      const tp = obj.type;
      if (tp === "list" || tp === "tuple" || tp === "array") {
        const items = obj.items ?? [];
        for (const it of items) void it;
      } else if (tp === "dict") {
        const entries = obj.entries ?? [];
        for (const e of entries) void e;
      } else {
        const fields = obj.fields ?? {};
        for (const k of Object.keys(fields)) void fields[k];
      }
    }
  }
  return { stepCount, heapCount };
}

// ─────────────────────────────────────────────────────────────────────────
// Bench harness
// ─────────────────────────────────────────────────────────────────────────

const FIVE_HUNDRED_STEPS_CFG: SyntheticConfig = {
  steps: 500,
  heapObjectsPerStep: 4,
  itemsPerArr: 3,
};
const synthetic = synthTrace(FIVE_HUNDRED_STEPS_CFG);

// Sanity log so the bench output records the input size.
const synthSize = synthetic.length;
const stepProbe = decode(synthetic);
console.log(
  `[decode-latency.bench] synthetic input: ${synthSize} bytes, ${stepProbe.stepCount} steps, ${stepProbe.heapCount} total heap objects walked`
);

describe("MarkedTrace.parse + decodeTrace latency baseline", () => {
  bench(
    "JSON.parse + structural walk (500-step synthetic)",
    () => {
      const r = decode(synthetic);
      // Force the optimiser to keep the walk in the hot loop.
      if (r.stepCount !== 500) throw new Error("walked wrong step count");
    },
    { iterations: 100, warmupIterations: 10 }
  );
});
