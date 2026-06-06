// Smoke test: the renderer entry module and its whole import graph
// (index -> graph-render -> stepper -> graph-layout / tree-layout -> d3) load
// cleanly. `renderWidget` itself needs a DOM, so it is not invoked here — this
// only guards against a broken import or syntax error in that module graph.

import { describe, it, expect } from "vitest";
import { renderWidget } from "./index";

describe("renderWidget — module wiring", () => {
  it("is exported as a function", () => {
    expect(typeof renderWidget).toBe("function");
  });
});
