// Phase 0 trace-shape Playwright tests (ADR-0025).
//
// Each test loads one HeapTraceFixtures fixture through the production d3
// renderer pipeline via the /test/e2e/render.html harness. The harness signals
// readiness with `data-render-state=ready` on `#harness-root`; we then assert
// the renderer DOM mounted, drive the controller to the final step, and capture
// a baseline screenshot.
//
// If a fixture fails to render here, the trace format is missing something
// (per ADR-0025) and Phase 1 schema design must absorb the extension before
// moving on.

import { test, expect, type Page } from "@playwright/test";

// `expectedCards` is the LIFETIME union of cardIds the multi-card renderer
// emits, NOT the per-step reachability count. The renderer collects every
// non-empty `cardId` across the trace and reserves a `<div class="viz-card">`
// for each one. groupCards only unions Instance-to-Instance refs, so a
// GraphNode pointing at an Arr-of-Refs does NOT union with the GraphNodes it
// reaches — they each stay separate cards.
// `expectedCards` is the LIFETIME union of cardIds the multi-card renderer
// emits, NOT the per-step reachability count. The renderer collects every
// non-empty `cardId` across the trace and reserves a `<div class="viz-card">`
// for each one. groupCards only unions Instance-to-Instance refs, so a
// GraphNode pointing at an Arr-of-Refs does NOT union with the GraphNodes it
// reaches — they each stay separate cards.
const FIXTURES = [
  // Per-step root resolution (ADR-0027) now follows the `root` rebind through
  // the rotation, so all three nodes stay in ONE unioned card (Y root, X left,
  // Z right) instead of the stale-root split that left 2 cards with faded
  // "removed" nodes. The rotated tree renders coherently.
  { name: "avl-rotation",                expectedCards: 1 },
  // 4 GraphNode instances (no inst-to-inst refs between them, all neighbours
  // go through an Arr) + 4 adjacency Arrs = 8 cards.
  { name: "graph-bfs",                   expectedCards: 8 },
  // map Dict + 2 bucket Arrs + 1 chained-Entry card (n_apple ↔ n_grape via
  // next) + 1 singleton Entry card (n_fig) = 5 cards.
  { name: "hashmap-chained-collisions",  expectedCards: 5 },
] as const;

/** Drive the harness controller to the last step so the screenshot lands on a stable terminal state. */
async function jumpToLastStep(page: Page): Promise<void> {
  await page.evaluate(() => {
    const c = (window as unknown as { __controller?: { setStep(n: number, animate: boolean): void; getStepCount(): number } }).__controller;
    if (c === undefined || c === null) throw new Error("__controller not set");
    c.setStep(c.getStepCount() - 1, false);
  });
}

for (const fixture of FIXTURES) {
  test(`fixture ${fixture.name} renders via the d3 pipeline`, async ({ page }) => {
    const errors: string[] = [];
    page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
    page.on("console", (msg) => {
      if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
    });

    await page.goto(`/test/e2e/render.html?fixture=${fixture.name}`);

    // Wait for the harness to either mount or fail. `state=ready` means the
    // d3 controller is live on `window.__controller`; any other state is a
    // hard fail surfaced via `#harness-error`.
    await page.waitForFunction(() => {
      const el = document.getElementById("harness-root");
      return el !== null && el.dataset.renderState !== "pending";
    }, { timeout: 15_000 });

    const state = await page.locator("#harness-root").getAttribute("data-render-state");
    if (state !== "ready") {
      const errText = await page.locator("#harness-error").textContent();
      throw new Error(`harness failed for ${fixture.name}: ${state} — ${errText ?? ""}`);
    }

    // Production renderer chrome is present.
    await expect(page.locator(".viz-graph")).toBeVisible();
    await expect(page.locator(".viz-graph__title")).toBeVisible();
    await expect(page.locator(".viz-graph__cards")).toBeAttached();

    // Each fixture surfaces a known number of `[data-card-id]` cards (see
    // FIXTURES table above). If this count changes, either the adapter's
    // groupCards logic changed (Phase 3 might) or the fixture itself drifted.
    await expect(page.locator(".viz-card[data-card-id]")).toHaveCount(fixture.expectedCards);

    // Drive to the terminal step so the screenshot baseline lands on the
    // stable end-of-trace shape (rotation done, BFS complete, collision
    // chain attached) rather than the initial state.
    await jumpToLastStep(page);
    // Renderers use `requestAnimationFrame` for layout positioning — yield
    // once so geometry settles before the screenshot lands.
    await page.evaluate(() => new Promise((resolve) => requestAnimationFrame(() => resolve(null))));

    expect(errors, `console / page errors during ${fixture.name}: ${errors.join(" | ")}`).toEqual([]);

    // Screenshot baseline. Phase 5 expands this with maxDiffPixels tuning per
    // chapter; for Phase 0 we lock the visible shape so Phases 1-3 regressions
    // are visible.
    await expect(page.locator(".viz-graph")).toHaveScreenshot(`${fixture.name}.png`, {
      animations: "disabled",
    });
  });
}

// Phase 2 (ADR-0029) — verify the SDK-ported StackRenderer renders the bespoke
// stack chrome (cells column + reversed-index + side pointer) in a real browser,
// not just via the source-AST unit tests. structureType="stack" dispatches to
// stackRenderer; this asserts the per-step DOM is intact after the SDK port.
test("stack-push renders the bespoke StackRenderer (SDK port intact)", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=stack-push`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  // Bespoke stack block present (NOT the generic SVG circles).
  await expect(page.locator(".stack-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // Three pushed cells; the first (top) cell carries the --top modifier.
  await expect(page.locator(".stack-renderer__cell")).toHaveCount(3);
  await expect(page.locator(".stack-renderer__cell").first()).toHaveClass(/stack-renderer__cell--top/);
  // The side pointer is present and labelled "top".
  await expect(page.locator(".stack-renderer__pointer-name")).toHaveText("top");
  // No generic SVG node circles in the bespoke render.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);

  expect(errors, `errors during stack-push: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("stack-push.png", {
    animations: "disabled",
  });
});

// Phase 2 (ADR-0029) — the cross-structure win. The generic multi-card path
// shattered a hashmap into edge-less cards (ADR-0025); the bespoke whole-graph
// HashMapRenderer reconstructs the Dict→Arr→Entry bucket chains into one
// coherent view. Asserts STRUCTURE (buckets + chain membership), not card
// counts — the Phase-0 mistake was asserting counts on broken output.
test("hashmap-kind renders bucket chains coherently (cross-structure)", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=hashmap-kind`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".hashmap-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // Two occupied buckets after the three inserts (apple+grape collide in 0; fig in 1).
  await expect(page.locator(".hashmap-renderer__bucket")).toHaveCount(2);
  // Bucket 0 holds the collision chain — TWO entries; bucket 1 holds one.
  const firstBucketEntries = page.locator(".hashmap-renderer__bucket").nth(0).locator(".hashmap-renderer__entry");
  const secondBucketEntries = page.locator(".hashmap-renderer__bucket").nth(1).locator(".hashmap-renderer__entry");
  await expect(firstBucketEntries).toHaveCount(2);
  await expect(secondBucketEntries).toHaveCount(1);
  // The chain is keyed: the collision bucket shows apple and grape.
  await expect(page.locator(".hashmap-renderer__entry-key").filter({ hasText: "apple" })).toHaveCount(1);
  await expect(page.locator(".hashmap-renderer__entry-key").filter({ hasText: "grape" })).toHaveCount(1);
  // A → connector joins the two collision entries.
  await expect(firstBucketEntries.locator("xpath=..").locator(".hashmap-renderer__connector")).toHaveCount(1);

  expect(errors, `errors during hashmap-kind: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("hashmap-kind.png", {
    animations: "disabled",
  });
});

// Phase 2c (ADR-0027) — graph as a whole-graph renderer. The generic path
// rendered graph-bfs as isolated edge-less cards (ADR-0025); the bespoke
// GraphRenderer synthesises direct node→node edges (folding away the adjacency
// cells) and draws one connected graph. Asserts EDGES exist between nodes.
test("graph-kind renders a connected graph with synthesised edges", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=graph-kind`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 4 GraphNodes drawn as SVG circles (the generic renderGraph chrome the
  // GraphRenderer delegates to). The generic broken path produced 0 because the
  // nodes were split into separate cards.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(4);
  // The synthesised direct edges are drawn — at least the cycle A→B→D→A + A→C→D
  // gives 5 edges. Assert the renderer emitted edge paths (no adjacency cells).
  const edgeCount = await page.locator("path.viz-graph__edge, .viz-graph__edge").count();
  expect(edgeCount, "expected synthesised node→node edges").toBeGreaterThanOrEqual(5);
  // Nodes are relabelled from their `id` field (A/B/C/D), not the adapter's
  // class-name fallback ("GraphNode") and not the bare adjacency cells ("·").
  const labels = await page.locator(".viz-graph__value").allTextContents();
  expect(labels.join(""), `node labels: ${labels.join(",")}`).toMatch(/[ABCD]/);
  expect(labels.join(""), "adjacency cells should not leak in").not.toContain("·");

  expect(errors, `errors during graph-kind: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("graph-kind.png", {
    animations: "disabled",
  });
});

// Phase 2d (ADR-0027) — Queue renderer (#4), per-card on the SDK like stack.
// Horizontal FIFO row: head (front) left, tail (back) right.
test("queue renders a FIFO row with head/tail callouts", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=queue`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".queue-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // Three enqueued cells; first is head, last is tail.
  await expect(page.locator(".queue-renderer__cell")).toHaveCount(3);
  await expect(page.locator(".queue-renderer__cell").first()).toHaveClass(/queue-renderer__cell--head/);
  await expect(page.locator(".queue-renderer__cell").last()).toHaveClass(/queue-renderer__cell--tail/);
  // Head cell holds the first-enqueued value (10); tail holds the last (30).
  await expect(page.locator(".queue-renderer__cell--head .queue-renderer__cell-value")).toHaveText("10");
  await expect(page.locator(".queue-renderer__cell--tail .queue-renderer__cell-value")).toHaveText("30");
  // Not the bespoke stack block, not generic SVG circles.
  await expect(page.locator(".stack-renderer")).toHaveCount(0);
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);

  expect(errors, `errors during queue: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("queue.png", {
    animations: "disabled",
  });
});

// Phase 2e (ADR-0027) — Heap renderer (#5). An array-backed heap renders as its
// implicit binary tree (synthesised index-edges → renderGraph + treeLayout),
// not a flat array row.
test("heap renders as a binary tree (synthesised index edges)", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=heap`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 4 heap elements as SVG tree-node circles, connected by 3 synthesised edges
  // (root→2 children, then one grandchild): a tree, not a flat array.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(4);
  const edgeCount = await page.locator("path.viz-graph__edge, .viz-graph__edge").count();
  expect(edgeCount, "expected 3 parent→child heap edges").toBeGreaterThanOrEqual(3);
  // Root holds the min (10); all four values present.
  const labels = (await page.locator(".viz-graph__value").allTextContents()).join(",");
  expect(labels).toMatch(/10/);
  expect(labels).toMatch(/40/);

  expect(errors, `errors during heap: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("heap.png", {
    animations: "disabled",
  });
});

// Phase 2f (ADR-0027) — Union-Find renderer (#6), per-card delegate. A parent
// array renders as a forest: circles labelled by their OWN index, parent arcs
// curving above the row, a self-loop on each representative. The generic path
// would have shown the flat array of parent indices.
test("union-find renders the parent-array as a forest with parent arcs", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=union-find`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 5 elements drawn as SVG circles (the generic renderGraph chrome the renderer
  // delegates to), connected by synthesised parent arcs + root self-loops.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(5);
  const edgeCount = await page.locator("path.viz-graph__edge, .viz-graph__edge").count();
  expect(edgeCount, "expected parent arcs + root self-loops").toBeGreaterThanOrEqual(4);

  // Circles are relabelled to their OWN index (0..4) — NOT the stored parent
  // values, which at the final step are [0,0,0,0,4]. So "1"/"2"/"3" appearing as
  // node values proves the parent-pointer → element-index relabel ran.
  const labels = (await page.locator(".viz-graph__value").allTextContents()).join(",");
  expect(labels, `node values: ${labels}`).toMatch(/1/);
  expect(labels, `node values: ${labels}`).toMatch(/3/);
  // Representatives carry the `root` sub-label (root 0 of the merged set + the
  // singleton 4 are both roots at the terminal step).
  await expect(page.locator(".viz-graph__class").filter({ hasText: "root" })).not.toHaveCount(0);
  // Not the bespoke stack / queue blocks, and not a flat array of cells.
  await expect(page.locator(".stack-renderer")).toHaveCount(0);
  await expect(page.locator(".queue-renderer")).toHaveCount(0);

  expect(errors, `errors during union-find: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("union-find.png", {
    animations: "disabled",
  });
});

// Phase 2g (ADR-0027) — Trie renderer (#7), whole-graph cross-structure. A
// prefix tree's children route through a Dict, so the generic path shattered it
// into edge-less TrieNode + Dict-entry cards. The renderer composes direct
// parent→child edges (folding the Dict away), labels nodes by the char on their
// incoming edge, and double-rings word-end nodes.
test("trie renders a prefix tree with char nodes + terminal rings", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=trie`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 7 nodes (root + c,a,t,r,d,o) — the prefix sharing is what keeps it at 7:
  // "cat"+"car"+"do" share the "ca" prefix, so it's NOT 3 separate word-paths.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(7);
  // 6 parent→child edges synthesised through the children Dict (root→c, root→d,
  // c→a, a→t, a→r, d→o). The generic broken path produced 0 connecting edges.
  const edgeCount = await page.locator("path.viz-graph__edge, .viz-graph__edge").count();
  expect(edgeCount, "expected composed parent→child trie edges").toBeGreaterThanOrEqual(6);

  // Each node is labelled by the char on its incoming edge — reading a path spells
  // the word. The root is "•" (U+2022). The Dict-entry placeholder "·" (U+00B7)
  // must NOT leak in (it's the scaffolding the renderer folds away).
  const labels = (await page.locator(".viz-graph__value").allTextContents()).join("");
  for (const ch of ["c", "a", "t", "r", "d", "o"]) {
    expect(labels, `node values: ${labels}`).toContain(ch);
  }
  expect(labels, "root start node present").toContain("•");
  expect(labels, "Dict-entry scaffolding must not leak").not.toContain("·");

  // Three word-end nodes (cat→t, car→r, do→o) carry the terminal double-ring.
  await expect(page.locator(".viz-graph__node--terminal")).toHaveCount(3);

  expect(errors, `errors during trie: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("trie.png", {
    animations: "disabled",
  });
});

// Phase 2h (ADR-0027) — Bitset renderer (#8), per-card on the SDK like stack /
// queue. A fixed-width bit row: set bits (value 1) filled, clear bits (value 0)
// muted, with a popcount summary. Distinguishes itself from a generic array-1d
// row of small ints by the set/clear contrast — that's the bitset's whole point.
test("bitset renders a set/clear bit row with a popcount summary", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=bitset`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".bitset-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 8 bit cells; at the terminal step [0,1,0,0,0,0,1,0] exactly two are set
  // (bits 1 and 6), each holding value "1".
  await expect(page.locator(".bitset-renderer__bit")).toHaveCount(8);
  await expect(page.locator(".bitset-renderer__bit--set")).toHaveCount(2);
  const setValues = await page.locator(".bitset-renderer__bit--set .bitset-renderer__bit-value").allTextContents();
  expect(setValues).toEqual(["1", "1"]);
  // The popcount summary reflects the two set bits out of eight.
  await expect(page.locator(".bitset-renderer__summary")).toHaveText(/8 bits · 2 set/);
  // Bit 4 was just cleared (1 → 0) on the final step, so exactly one cell carries
  // the changed ring — and its value is "0" (the clear, not a set).
  await expect(page.locator(".bitset-renderer__bit--changed")).toHaveCount(1);
  await expect(page.locator(".bitset-renderer__bit--changed .bitset-renderer__bit-value")).toHaveText("0");
  // Not the bespoke stack / queue blocks, and not generic SVG circles.
  await expect(page.locator(".stack-renderer")).toHaveCount(0);
  await expect(page.locator(".queue-renderer")).toHaveCount(0);
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);

  expect(errors, `errors during bitset: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("bitset.png", {
    animations: "disabled",
  });
});

// Phase 2i (ADR-0027) — Linked-list renderer (#9), per-card BESPOKE DOM (rewritten
// to the design's horizontal boxes; structureType `list-single` → `linkedListRenderer`
// in RENDERERS, NOT the generic SVG path). A chain of Instance nodes (joined by
// `next`) renders left-to-right as value boxes, a `next` arrow after each box, a `∅`
// null terminator span, a `head` caret over the head box and a `cur` caret under the
// visited box. Asserts the `.list-renderer__*` HTML grid — explicitly NOT `.viz-graph`
// SVG circles.
test("linked-list renders a horizontal box chain ending in a null sentinel", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=linked-list`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".list-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 3 ListNode boxes joined by a `next` arrow after each (incl. the tail→∅ arrow),
  // terminated by a single `∅` null sentinel span (not itself a node box).
  await expect(page.locator(".list-renderer__node")).toHaveCount(3);
  await expect(page.locator(".list-renderer__edge")).toHaveCount(3);
  await expect(page.locator(".list-renderer__null")).toHaveCount(1);
  await expect(page.locator(".list-renderer__null")).toHaveText("∅");

  // Node values spell the chain 1/2/3.
  const labels = (await page.locator(".list-renderer__val").allTextContents()).join("");
  for (const ch of ["1", "2", "3"]) {
    expect(labels, `node values: ${labels}`).toContain(ch);
  }

  // The `head` and `cur` Ref locals surface as carets on their boxes (free from the
  // trace): at the terminal step `head` labels N1 and `cur` marks the visited N3.
  await expect(page.locator(".list-renderer__node--active")).toHaveCount(1);
  await expect(page.locator(".list-renderer__head-label")).toHaveCount(1);
  await expect(page.locator(".list-renderer__head-label")).toContainText("head");
  await expect(page.locator(".list-renderer__cur")).toHaveCount(1);
  await expect(page.locator(".list-renderer__cur")).toContainText("cur");

  // Singly-linked: no doubly variant, no PREV compartment, no two-arrow legend.
  await expect(page.locator(".list-renderer--double")).toHaveCount(0);
  await expect(page.locator(".list-renderer__field--prev")).toHaveCount(0);
  await expect(page.locator(".list-renderer__legend")).toHaveCount(0);

  // Bespoke HTML — explicitly NOT the generic SVG renderer, nor the other bespoke blocks.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);
  await expect(page.locator(".stack-renderer")).toHaveCount(0);
  await expect(page.locator(".queue-renderer")).toHaveCount(0);
  await expect(page.locator(".bitset-renderer")).toHaveCount(0);

  expect(errors, `errors during linked-list: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("linked-list.png", {
    animations: "disabled",
  });
});

// Phase 2j (ADR-0027) — Segment-tree renderer (#10), per-card bespoke DOM. The
// range-bar overlay: each node is a bar spanning the array slice [lo,hi] it
// covers, stacked by tree level over an index row, so the "segments" are literal.
// Asserts the bar grid + the `cur` descent, not generic SVG circles.
test("segment-tree renders the range-bar overlay with a cur descent", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=segment-tree`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".segment-tree-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 7 nodes (root + 2 internal + 4 leaf nodes).
  await expect(page.locator(".segment-tree-renderer__node")).toHaveCount(7);
  await expect(page.locator(".segment-tree-renderer__node--leaf")).toHaveCount(4);
  // Every node carries its covered [lo,hi] range label; the root spans the whole array.
  await expect(page.locator(".segment-tree-renderer__range")).toHaveCount(7);
  await expect(page.locator(".segment-tree-renderer__range").filter({ hasText: "[0,3]" })).toHaveCount(1);
  // The underlying-array row beneath the tree: one indexed cell per array slot (0..3).
  await expect(page.locator(".segment-tree-renderer__leaf")).toHaveCount(4);
  await expect(page.locator(".segment-tree-renderer__leaf em").last()).toHaveText("3");
  // The root holds the full-range sum 3+1+4+2 = 10.
  const values = (await page.locator(".segment-tree-renderer__val").allTextContents());
  expect(values, `node values: ${values.join(",")}`).toContain("10");

  // The `cur` pointer has descended root → [0,1] → [1,1]: exactly one node is the
  // cursor, and it's the leaf holding array[1] = 1.
  await expect(page.locator(".segment-tree-renderer__node--cursor")).toHaveCount(1);
  await expect(page.locator(".segment-tree-renderer__node--cursor .segment-tree-renderer__val")).toHaveText("1");

  // Bespoke DOM — not the generic SVG renderer, not the other bespoke blocks.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);
  await expect(page.locator(".bitset-renderer")).toHaveCount(0);
  await expect(page.locator(".stack-renderer")).toHaveCount(0);

  expect(errors, `errors during segment-tree: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("segment-tree.png", {
    animations: "disabled",
  });
});

// Phase 2k (ADR-0027) — Fenwick / BIT renderer (#11), per-card bespoke DOM. The
// responsibility staircase: each cell is a bar spanning the half-open slice
// (i-lowbit, i] it owns, stacked by low-bit level over an index row, so the
// "responsibility ranges" are literal. Asserts the bar grid + the `i` query
// cursor climbing it, not generic SVG circles.
test("fenwick renders the responsibility staircase with a climbing cursor", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=fenwick`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".fenwick-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 8 cells (Fenwick indices 1..8); the dummy slot 0 is NOT drawn.
  await expect(page.locator(".fenwick-renderer__node")).toHaveCount(8);
  // 4 singletons — odd indices 1,3,5,7 (lowbit 1, responsible for one element).
  await expect(page.locator(".fenwick-renderer__node--singleton")).toHaveCount(4);
  // Each cell carries its half-open responsibility range; index 8 spans the array.
  await expect(page.locator(".fenwick-renderer__node-range").filter({ hasText: "(0,8]" })).toHaveCount(1);
  await expect(page.locator(".fenwick-renderer__node-range").filter({ hasText: "(0,4]" })).toHaveCount(1);
  await expect(page.locator(".fenwick-renderer__node-range").filter({ hasText: "(6,7]" })).toHaveCount(1);
  // An index row 1..8 beneath the staircase.
  await expect(page.locator(".fenwick-renderer__index")).toHaveCount(8);
  await expect(page.locator(".fenwick-renderer__index").first()).toHaveText("1");
  await expect(page.locator(".fenwick-renderer__index").last()).toHaveText("8");
  // The root bar (index 8) holds the full-array sum 1+2+…+8 = 36.
  const values = await page.locator(".fenwick-renderer__node-value").allTextContents();
  expect(values, `cell values: ${values.join(",")}`).toContain("36");

  // query(7) descended 7 → 6 → 4: at the last step exactly one cell is the cursor,
  // and it's index 4 (the (0,4] bar holding tree[4] = 10).
  await expect(page.locator(".fenwick-renderer__node--cursor")).toHaveCount(1);
  await expect(page.locator(".fenwick-renderer__node--cursor")).toHaveAttribute("data-index", "4");
  await expect(page.locator(".fenwick-renderer__node--cursor .fenwick-renderer__node-value")).toHaveText("10");

  // Bespoke DOM — not the generic SVG renderer, not the other bespoke blocks.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);
  await expect(page.locator(".segment-tree-renderer")).toHaveCount(0);
  await expect(page.locator(".bitset-renderer")).toHaveCount(0);

  expect(errors, `errors during fenwick: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("fenwick.png", {
    animations: "disabled",
  });
});

// Phase 2l (ADR-0027) — Skip-list renderer (#12, the final shape), per-card bespoke
// DOM. The multi-level grid: one row per level (top express lane first), one column
// per node ordered by key, a boxed cell where a node reaches that level and a thin gap
// line elsewhere. Asserts the grid + the `cur` search column, not generic SVG circles.
test("skiplist renders the multi-level grid with an active search column", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=skiplist`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".skiplist-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 3 levels → 3 rows; the top row is the L2 express lane, the bottom is L0.
  await expect(page.locator(".skiplist-renderer__row")).toHaveCount(3);
  await expect(page.locator(".skiplist-renderer__level-label").first()).toHaveText("L2");
  await expect(page.locator(".skiplist-renderer__level-label").last()).toHaveText("L0");
  // 12 value cells: 7 at L0 + 3 at L1 (7, 19, 25) + 2 at L2 (7, 25).
  await expect(page.locator(".skiplist-renderer__node")).toHaveCount(12);
  // 9 gap lines keep columns aligned: 4 at L1 + 5 at L2 (L0 is full).
  await expect(page.locator(".skiplist-renderer__gap")).toHaveCount(9);
  // head + ∅ sentinels bookend each row.
  await expect(page.locator(".skiplist-renderer__head")).toHaveCount(3);
  await expect(page.locator(".skiplist-renderer__null")).toHaveCount(3);

  // search(19) ended on key 19 (level 1) → its WHOLE column is active: the L0 + L1 cells.
  await expect(page.locator(".skiplist-renderer__node--active")).toHaveCount(2);
  const activeTexts = await page.locator(".skiplist-renderer__node--active").allTextContents();
  expect(new Set(activeTexts), `active cells: ${activeTexts.join(",")}`).toEqual(new Set(["19"]));

  // Bespoke DOM — not the generic SVG renderer, not the other bespoke blocks.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);
  await expect(page.locator(".fenwick-renderer")).toHaveCount(0);
  await expect(page.locator(".segment-tree-renderer")).toHaveCount(0);

  expect(errors, `errors during skiplist: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("skiplist.png", {
    animations: "disabled",
  });
});

// Phase 2m (ADR-0027) — Array renderer, the DEFAULT for array-1d cards (no viz-kind). A
// clean cell row with pointer carets above and an index row below, replacing the generic
// circle layout. Asserts the cells + carets, NOT generic SVG circles. The fixture carries
// `vizKind = None`, so this also proves the layoutKind-default routing.
test("array renders a cell row with pointer carets, not generic circles", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
  page.on("console", (msg) => {
    if (msg.type() === "error") errors.push(`console.error: ${msg.text()}`);
  });

  await page.goto(`/test/e2e/render.html?fixture=array`);
  await page.waitForFunction(() => {
    const el = document.getElementById("harness-root");
    return el !== null && el.dataset.renderState !== "pending";
  }, { timeout: 15_000 });
  expect(await page.locator("#harness-root").getAttribute("data-render-state")).toBe("ready");

  await expect(page.locator(".array-renderer").first()).toBeVisible();
  await jumpToLastStep(page);
  await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => r(null))));

  // 5 cells + an index row 0..4.
  await expect(page.locator(".array-renderer__cell")).toHaveCount(5);
  await expect(page.locator(".array-renderer__index")).toHaveCount(5);
  await expect(page.locator(".array-renderer__index").first()).toHaveText("0");
  await expect(page.locator(".array-renderer__index").last()).toHaveText("4");
  // Last step of the reverse: [5, 4, 3, 2, 1].
  const values = await page.locator(".array-renderer__cell-value").allTextContents();
  expect(values, `cell values: ${values.join(",")}`).toEqual(["5", "4", "3", "2", "1"]);
  // left + right pointers have met at slot 2 → two carets over one cursor cell.
  await expect(page.locator(".array-renderer__pointer")).toHaveCount(2);
  await expect(page.locator(".array-renderer__pointer-name").filter({ hasText: "left" })).toHaveCount(1);
  await expect(page.locator(".array-renderer__pointer-name").filter({ hasText: "right" })).toHaveCount(1);
  await expect(page.locator(".array-renderer__cell--cursor")).toHaveCount(1);
  // The swapped pair (slots 1, 3) is flagged changed this step.
  await expect(page.locator(".array-renderer__cell--changed")).toHaveCount(2);

  // Bespoke cell row — NOT the generic circle layout, not other bespoke blocks.
  await expect(page.locator(".viz-graph__circle")).toHaveCount(0);
  await expect(page.locator(".skiplist-renderer")).toHaveCount(0);

  expect(errors, `errors during array: ${errors.join(" | ")}`).toEqual([]);
  await expect(page.locator(".viz-graph")).toHaveScreenshot("array.png", {
    animations: "disabled",
  });
});
