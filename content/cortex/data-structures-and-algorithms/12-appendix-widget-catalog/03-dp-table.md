---
title: dp-table
summary: 1D / 2D dynamic-programming table with two solver modes. Bottom-up fills cells incrementally with transient dependency arrows + a rule annotation strip; top-down drives a recursion tree above the table whose `return` events write leaf values back into cells and `cache-hit` events flash a memoised cell without descending. Per-cell status splits cleanly into a persistent layer (empty / filled / on-path) and a transient adornment layer (filling / depends-on / cached), so a filled cell becoming a predecessor of the next computation never loses its value.
prereqs: []
---

# `dp-table`

## Purpose

A single widget covers source Phase 14 (Dynamic Programming — 24 source diagrams across nine chapters, every one of which has paired *top-down* and *bottom-up* lessons over the same problem data). The model is a row × column grid filled in some order; the difference between the two solvers is the **path taken** to fill it, which is the pedagogical point.

Two `solver` flags steer the renderer — same table, two animation surfaces:

- `solver: "bottom-up"` — table fills incrementally. Each `fill` event highlights the filling cell, fades in transient dependency arrows from the predecessor cells listed in `depends`, and surfaces a one-line `rule` annotation below the table. Past `fill`s persist (the cells stay coloured + their values stay drawn) so by the final step the whole table is computed. The kind badge tints blue. Canonical examples: LIS, LCS, edit distance, knapsack.
- `solver: "top-down"` — table starts mostly empty (`-`). Events drive a recursion tree rendered above the table: `call` descends into a child node, `return` writes the leaf's value back into the named cell and marks the tree node terminal, `cache-hit` flashes a cached cell violet for one step without descending. The kind badge tints emerald. Canonical examples: memoised LCS, memoised LIS, edit-distance recursion sketch.

The data model is small: `initial` (R × C strings — `"-"` or `""` means "empty"), `events` (a tape of `fill` / `call` / `return` / `cache-hit` operations), and an optional `recursionTree` (top-down only). The renderer replays events to compute per-step state.

**Per-cell status splits into two layers** — this is the design decision the post-decision-tree audit drove:

- **Persistent** (`empty` / `filled` / `on-path`) — set once, never overwritten by later events. `fill` and `return` mark the cell `filled`. The `optimalPath` overlay (final step only) flips path cells to `on-path` (emerald halo).
- **Transient adornments** (`filling` / `depends-on` / `cached`) — added as additional modifier classes on top of the persistent status for the CURRENT step only. Stacked, not exclusive: a `filled` cell that's also a predecessor of the cell being computed right now reads as "filled (blue) + depends-on (amber outline)".

Splitting persistent vs. transient means a `filled` cell never loses its value when later steps reference it as a predecessor — no terminal-status preservation guard needed inside the event applier. The single guard that *does* apply is in top-down mode: a `call` event must not overwrite a node that's already `returned` (same pattern as decision-tree's `accepted`/`rejected`/`pruned` preservation).

> **Source spec**: `docs/migration/widget-specs/dp-table.md`
>
> **Scala module**: `client/src/main/scala/codefolio/client/components/cortex/widgets/DpTable.scala`
>
> **Event-tape model**: the renderer's per-step state is computed by folding `applyEvent` over `events[0..stepIdx]`. Adornments are reset at the head of each event so they NEVER leak across steps; persistent statuses accumulate. The `optimalPath` overlay (when present) is applied only at the final step.

## Payload schema (reference card)

```ts
{
  title:       string,
  solver:      "bottom-up" | "top-down",
  dimensions:  1 | 2,
  rowLabels?:  string[],                          // 2D only — leading "" for the 0-prefix
  colLabels?:  string[],                          // both — leading "" for the 0-prefix on 2D
  initial:     string[][],                        // R × C — "-" or "" means empty
  events: [
    // fill — bottom-up: write `value` into `cell`; arrows from `depends`; rule strip shows `rule`.
    { kind: "fill",      cell: [r, c], value: string, depends?: [r, c][], rule?: string, msg: string },
    // call — top-down: descend into `nodeId`. Sets tree node active.
    { kind: "call",      nodeId: string, msg: string },
    // return — top-down: pop the cursor; mark node returned. Optionally write `value` into `cell`.
    { kind: "return",    nodeId: string, value?: string, cell?: [r, c], msg: string },
    // cache-hit — top-down: flash `cell` violet for one step. No tree movement.
    { kind: "cache-hit", cell: [r, c], msg: string }
  ],
  recursionTree?: {                               // top-down only
    nodes: [{
      id:      string,
      label:   string,
      parent?: string                             // omit for root
    }]
  },
  optimalPath?:      [r, c][],                    // final-step overlay; cells flip to --on-path
  optimalPathLabel?: string                       // caption below the table; defaults to "optimal path"
}
```

**Required**: `title`, `solver`, `dimensions`, `initial` (non-empty), `events` (non-empty), `events[].kind` (closed set), `events[].msg`.
**Optional**: `rowLabels` / `colLabels` (default to empty headers), `events[].depends` / `events[].rule` (only `fill` cares), `events[].value` / `events[].cell` (only `fill` and `return` care; cache-hit needs `cell`), `events[].nodeId` (only `call` and `return` care), `recursionTree` (required for any `call` / `return` to be meaningful — top-down only), `optimalPath` / `optimalPathLabel`.

**Closed sets** (unknown values fall back to a safe default at parse time, so a typo never crashes the chapter):

- `solver` ∈ `{ "bottom-up", "top-down" }` — unknown → `"bottom-up"`.
- `dimensions` ∈ `{ 1, 2 }` — anything else → `2`.
- `events[].kind` ∈ `{ "fill", "call", "return", "cache-hit" }` — unknown → `"fill"`.

**Event-replay rules**:

- `fill` writes the value into the cell, marks the cell persistently `filled`, and records `cell` / `depends` / `rule` as TRANSIENT adornments visible this step only.
- `call` marks the named tree node `active` and pushes it onto the cursor path. Guard: if the node is already `returned`, the status is preserved (the `returned` paint stays) — a "re-call" event is allowed but doesn't strip the terminal mark.
- `return` pops the cursor (if the popped node matches `nodeId`), marks the node `returned`, and — if `cell` and `value` are present — writes the value into the cell + marks it `filled`.
- `cache-hit` only sets the transient `--cached` adornment on `cell`. No tree movement, no persistent status change.
- Any event resets ALL transient adornments at the head, so a `call` after a `fill` clears the previous fill's arrows.
- The `optimalPath` overlay applies ONLY at the final step; scrubbing backwards removes it.

**Cell-status combinations** (some are illegal, listed for reference):

| Persistent  | Adornments        | Meaning                                                    |
|---          |---                |---                                                         |
| `empty`     | —                 | Not yet computed                                           |
| `empty`     | `filling`         | This step's fill — value scrambling in, depends arrows fading |
| `empty`     | `cached`          | Top-down: cache-hit flashed on an unwritten cell (rare; typically the spec elides the writer subtrace) |
| `filled`    | —                 | Computed in a past step; canonical "done" state            |
| `filled`    | `depends-on`      | Predecessor of the cell being computed THIS step           |
| `filled`    | `cached`          | Top-down: cache-hit on a previously-written cell           |
| `on-path`   | —                 | Final-step overlay — part of the recovered answer trace    |

## Representative payloads

### Payload 1 — 1D bottom-up: Longest Increasing Subsequence

The canonical 1D DP. Single row of cells; each cell `i` holds the LIS length ending at index `i`. The fill order walks left-to-right; the rule annotation explains why each cell's value is what it is. Final-step overlay highlights the cells whose values were chained into the recovered LIS (`[2, 5, 7]`).

```d3 widget=dp-table
{
  "title": "Longest Increasing Subsequence — bottom-up",
  "solver": "bottom-up",
  "dimensions": 1,
  "colLabels": ["10", "9", "2", "5", "3", "7"],
  "initial": [["1", "1", "1", "1", "1", "1"]],
  "events": [
    {"kind": "fill", "cell": [0, 0], "value": "1", "depends": [], "rule": "base — lis(0) = 1", "msg": "lis(0) = 1"},
    {"kind": "fill", "cell": [0, 1], "value": "1", "depends": [], "rule": "9 < 10 — can't extend", "msg": "lis(1) = 1"},
    {"kind": "fill", "cell": [0, 2], "value": "1", "depends": [], "rule": "2 < 10 and 2 < 9 — can't extend", "msg": "lis(2) = 1"},
    {"kind": "fill", "cell": [0, 3], "value": "2", "depends": [[0, 2]], "rule": "5 > 2 → 1 + lis(2) = 2", "msg": "lis(3) = 2"},
    {"kind": "fill", "cell": [0, 4], "value": "2", "depends": [[0, 2]], "rule": "3 > 2 → 1 + lis(2) = 2", "msg": "lis(4) = 2"},
    {"kind": "fill", "cell": [0, 5], "value": "3", "depends": [[0, 3], [0, 4]], "rule": "7 > 5 and 7 > 3 → 1 + max(lis(3), lis(4)) = 3", "msg": "lis(5) = 3"}
  ],
  "optimalPath": [[0, 2], [0, 3], [0, 5]],
  "optimalPathLabel": "LIS = [2, 5, 7]"
}
```

### Payload 2 — 2D bottom-up: Longest Common Subsequence

The textbook 2D DP. Row labels = the first string, column labels = the second; cell `[i, j]` holds `lcs(first[..i], second[..j])`. Match cells get `1 + dp[i-1][j-1]` (the diagonal predecessor); mismatch cells get `max(dp[i-1][j], dp[i][j-1])` (top OR left). Exercises:

- The 2D layout — row + column gutters with leading blanks for the 0-prefix.
- Multi-predecessor `depends`: the mismatch case lights up TWO predecessor cells.
- `initial` pre-populated with the 0-row + 0-column (base cases) so the trace only walks the meaningful interior cells.

```d3 widget=dp-table
{
  "title": "LCS of \"ab\" and \"acb\" — bottom-up",
  "solver": "bottom-up",
  "dimensions": 2,
  "rowLabels": ["", "a", "b"],
  "colLabels": ["", "a", "c", "b"],
  "initial": [
    ["0", "0", "0", "0"],
    ["0", "-", "-", "-"],
    ["0", "-", "-", "-"]
  ],
  "events": [
    {"kind": "fill", "cell": [1, 1], "value": "1", "depends": [[0, 0]], "rule": "match a = a → 1 + dp[0][0] = 1", "msg": "dp[1][1] = 1"},
    {"kind": "fill", "cell": [1, 2], "value": "1", "depends": [[0, 2], [1, 1]], "rule": "a ≠ c → max(dp[0][2], dp[1][1]) = 1", "msg": "dp[1][2] = 1"},
    {"kind": "fill", "cell": [1, 3], "value": "1", "depends": [[0, 3], [1, 2]], "rule": "a ≠ b → max(dp[0][3], dp[1][2]) = 1", "msg": "dp[1][3] = 1"},
    {"kind": "fill", "cell": [2, 1], "value": "1", "depends": [[1, 1], [2, 0]], "rule": "b ≠ a → max(dp[1][1], dp[2][0]) = 1", "msg": "dp[2][1] = 1"},
    {"kind": "fill", "cell": [2, 2], "value": "1", "depends": [[1, 2], [2, 1]], "rule": "b ≠ c → max(dp[1][2], dp[2][1]) = 1", "msg": "dp[2][2] = 1"},
    {"kind": "fill", "cell": [2, 3], "value": "2", "depends": [[1, 2]], "rule": "match b = b → 1 + dp[1][2] = 2", "msg": "dp[2][3] = 2 — answer"}
  ],
  "optimalPath": [[2, 3], [1, 2], [1, 1], [0, 0]],
  "optimalPathLabel": "LCS = \"ab\""
}
```

### Payload 3 — 2D top-down with recursion tree (same LCS, with a cache-hit)

The same LCS problem in `solver: "top-down"` mode. The recursion tree renders above the table; each tree node represents one recursive call to `lcs(i, j)`. The cell-write events (`return`) mark the corresponding table cell `filled` with the returned value. The `cache-hit` event flashes a cell violet for one step to signal "we already memoised this answer — skip the recursive descent".

The trace deliberately elides the deep subrecursion under `lcs(1, 2)` (which would compute `lcs(0, 2) = 0` and `lcs(1, 1) = 1` before settling) — the chapter focuses on the cache-hit mechanic. The `initial` table pre-populates `dp[1][1] = "1"` and `dp[1][2] = "1"` to simulate "we computed these earlier and cached them"; the pre-populated `dp[1][2]` is what enables the visible cache hit at step 5, and the pre-populated `dp[1][1]` keeps the final-step optimal-path overlay coherent (otherwise an emerald `on-path` cell would carry no value).

Exercises:

- The `solver: "top-down"` (emerald) badge — visually distinct from bottom-up.
- The recursion-tree subview above the table — three nodes, two-level layered layout.
- `call` event marking a tree node `active` (emerald halo) and pushing it onto the cursor.
- `return` event marking the node `returned` (slate) AND writing the value into the named cell (the cell flips from empty → filled with the value text).
- `cache-hit` event flashing a cell violet without touching the tree at all.
- The final two `return` events back-fill the parent and root cells, producing the same final cell values as Payload 2 (modulo the elided subrecursion).

```d3 widget=dp-table
{
  "title": "LCS of \"ab\" and \"acb\" — top-down (with cache-hit)",
  "solver": "top-down",
  "dimensions": 2,
  "rowLabels": ["", "a", "b"],
  "colLabels": ["", "a", "c", "b"],
  "initial": [
    ["0", "0", "0", "0"],
    ["0", "1", "1", "-"],
    ["0", "-", "-", "-"]
  ],
  "events": [
    {"kind": "call",      "nodeId": "r-2-3", "msg": "Start: lcs(2, 3). The subrecursion under lcs(1, 2) is elided — dp[1][1] and dp[1][2] are pre-populated to simulate \"computed earlier and cached\"."},
    {"kind": "call",      "nodeId": "r-1-3", "msg": "Recurse left into lcs(1, 3) to explore the left option."},
    {"kind": "return",    "nodeId": "r-1-3", "value": "1", "cell": [1, 3], "msg": "lcs(1, 3) = 1. Cache it (write to dp[1][3])."},
    {"kind": "call",      "nodeId": "r-2-2", "msg": "Now recurse right into lcs(2, 2). Its body wants lcs(1, 2)…"},
    {"kind": "cache-hit", "cell": [1, 2], "msg": "lcs(1, 2) is already cached (dp[1][2] = 1) — skip recursion."},
    {"kind": "return",    "nodeId": "r-2-2", "value": "1", "cell": [2, 2], "msg": "lcs(2, 2) returns 1. Cache (dp[2][2] = 1)."},
    {"kind": "return",    "nodeId": "r-2-3", "value": "2", "cell": [2, 3], "msg": "lcs(2, 3) returns 2 — the answer. Cache (dp[2][3] = 2)."}
  ],
  "recursionTree": {
    "nodes": [
      {"id": "r-2-3", "label": "lcs(2,3)"},
      {"id": "r-1-3", "label": "lcs(1,3)", "parent": "r-2-3"},
      {"id": "r-2-2", "label": "lcs(2,2)", "parent": "r-2-3"}
    ]
  },
  "optimalPath": [[2, 3], [2, 2], [1, 1]],
  "optimalPathLabel": "LCS = \"ab\" (traced back through the cached cells)"
}
```

### Payload 4 — 2D bottom-up: Edit distance

Edit-distance is the canonical "three predecessors" DP — at every interior cell the minimum is taken over the diagonal (substitute / match), the cell above (insert), and the cell to the left (delete). Exercises:

- Cells with THREE `depends` predecessors (the substitute / insert / delete trio).
- The match case `dp[2][2]` carrying ONLY the diagonal predecessor (no min over alternatives — when chars match the edit count just passes through).
- A non-square table (4 rows × 3 cols).
- An `optimalPath` overlay that traces the diagonal `[3,2] → [2,2] → [1,1] → [0,0]` corresponding to the recovered edit sequence ("insert e, insert f").

```d3 widget=dp-table
{
  "title": "Edit distance \"ab\" → \"ebf\" — bottom-up",
  "solver": "bottom-up",
  "dimensions": 2,
  "rowLabels": ["", "e", "b", "f"],
  "colLabels": ["", "a", "b"],
  "initial": [
    ["0", "1", "2"],
    ["1", "-", "-"],
    ["2", "-", "-"],
    ["3", "-", "-"]
  ],
  "events": [
    {"kind": "fill", "cell": [1, 1], "value": "1", "depends": [[0, 0], [0, 1], [1, 0]], "rule": "e ≠ a → 1 + min(dp[0][0], dp[0][1], dp[1][0]) = 1 + 0", "msg": "ed(e, a) = 1"},
    {"kind": "fill", "cell": [1, 2], "value": "2", "depends": [[0, 1], [0, 2], [1, 1]], "rule": "e ≠ b → 1 + min(dp[0][1], dp[0][2], dp[1][1]) = 1 + 1", "msg": "ed(e, ab) = 2"},
    {"kind": "fill", "cell": [2, 1], "value": "2", "depends": [[1, 0], [1, 1], [2, 0]], "rule": "b ≠ a → 1 + min(dp[1][0], dp[1][1], dp[2][0]) = 1 + 1", "msg": "ed(eb, a) = 2"},
    {"kind": "fill", "cell": [2, 2], "value": "1", "depends": [[1, 1]], "rule": "b = b match → dp[1][1] passes through", "msg": "ed(eb, ab) = 1"},
    {"kind": "fill", "cell": [3, 1], "value": "3", "depends": [[2, 0], [2, 1], [3, 0]], "rule": "f ≠ a → 1 + min(2, 2, 3) = 3", "msg": "ed(ebf, a) = 3"},
    {"kind": "fill", "cell": [3, 2], "value": "2", "depends": [[2, 1], [2, 2], [3, 1]], "rule": "f ≠ b → 1 + min(2, 1, 3) = 2", "msg": "ed(ebf, ab) = 2 — answer"}
  ],
  "optimalPath": [[3, 2], [2, 2], [1, 1], [0, 0]],
  "optimalPathLabel": "2 edits: insert e, insert f"
}
```

### Payload 5 — 2D bottom-up: 0/1 Knapsack

Knapsack — the canonical "carry-over OR take" DP. Each row is one item; each column is a weight capacity. A cell takes the max of "skip this item" (the cell above, same column) and "take this item" (value + the cell above offset by item weight). Exercises:

- A wider table (4 × 5) — verifies the canvas's `overflow-x-auto` kicks in on narrow viewports without squeezing the cells.
- A `value` cell carrying a 2-digit string ("5") to verify the cell-text doesn't overflow the rect.
- Single-predecessor "carry over" fills sitting beside multi-predecessor "take this item or carry over" fills, in the same trace.
- An optimal-path overlay that's NOT contiguous in rows — `[3,4] → [2,4] → [1,1] → [0,0]` jumps from row 3 → row 2 → row 1 (skipping item 3 → taking item 2 → taking item 1).

```d3 widget=dp-table
{
  "title": "0/1 Knapsack — capacity 4, items [(w=1,v=1), (w=3,v=4), (w=2,v=3)]",
  "solver": "bottom-up",
  "dimensions": 2,
  "rowLabels": ["", "item1", "item2", "item3"],
  "colLabels": ["w=0", "w=1", "w=2", "w=3", "w=4"],
  "initial": [
    ["0", "0", "0", "0", "0"],
    ["0", "-", "-", "-", "-"],
    ["0", "-", "-", "-", "-"],
    ["0", "-", "-", "-", "-"]
  ],
  "events": [
    {"kind": "fill", "cell": [1, 1], "value": "1", "depends": [[0, 1], [0, 0]], "rule": "fit (w=1): max(skip=0, take=1+dp[0][0]) = 1", "msg": "dp[1][1] = 1"},
    {"kind": "fill", "cell": [1, 2], "value": "1", "depends": [[0, 2], [0, 1]], "rule": "fit: max(0, 1+0) = 1", "msg": "dp[1][2] = 1"},
    {"kind": "fill", "cell": [1, 3], "value": "1", "depends": [[0, 3], [0, 2]], "rule": "fit: max(0, 1+0) = 1", "msg": "dp[1][3] = 1"},
    {"kind": "fill", "cell": [1, 4], "value": "1", "depends": [[0, 4], [0, 3]], "rule": "fit: max(0, 1+0) = 1", "msg": "dp[1][4] = 1"},
    {"kind": "fill", "cell": [2, 3], "value": "4", "depends": [[1, 3], [1, 0]], "rule": "fit (w=3): max(skip=1, take=4+dp[1][0]) = 4", "msg": "dp[2][3] = 4"},
    {"kind": "fill", "cell": [2, 4], "value": "5", "depends": [[1, 4], [1, 1]], "rule": "fit (w=3): max(skip=1, take=4+dp[1][1]) = 5", "msg": "dp[2][4] = 5"},
    {"kind": "fill", "cell": [3, 4], "value": "5", "depends": [[2, 4], [2, 2]], "rule": "fit (w=2): max(skip=5, take=3+dp[2][2]) = 5", "msg": "dp[3][4] = 5 — answer"}
  ],
  "optimalPath": [[3, 4], [2, 4], [1, 1], [0, 0]],
  "optimalPathLabel": "Take items 1 and 2 — total value 5"
}
```

## Compression notes

When porting a source `// Interactive Diagram (N frames): …` for this widget, target **6–12 widget steps** (source diagrams range 16–68 frames; the longest is the 68-frame "edit distance top-down" trace). Compression strategy:

- **Per-cell "fill-in-progress" beats** in source (highlight predecessors → write value → unhighlight → move cursor) → fold into a single `fill` event. The renderer paints the predecessor arrows, the rule annotation, and the value-transition in one step.
- **Multi-frame base-case fills** at the top row / left column → seed them in `initial` instead of emitting `fill` events. The reader sees the base cases pre-populated when the widget mounts; the trace only walks the interior cells where the recurrence is interesting.
- **Top-down deep subrecursion** (the lcs/edit-distance source diagrams walk *every* recursive call, including base cases) → render only the recursion tree to the depth where pruning / cache hits become visible; pre-populate deeper cells in `initial` and reference them via `cache-hit` events. Payload 3's elided lcs(1, 2) subtree is the canonical example.
- **Backtrack / answer-reconstruction frames** at the end of source diagrams → don't emit events for them. Use the `optimalPath` overlay instead — it lights up the answer cells at the final step in one shot.
- **Multi-step value-update animations** (the source's number-scramble effect when a cell's value changes) → not modelled — the value flips instantly when the `fill` event lands. Authors who want a multi-step buildup should split into two events (e.g. one `fill` for the comparison + one `fill` for the choice), but most DPs don't need this.

The 68-frame "edit distance top-down" source diagram compresses to roughly 11 events: 5 `call`s descending into the recursion + 5 `return`s back-filling cells + 1 final `optimalPath` overlay. A 36-frame "edit distance bottom-up" diagram compresses to ~8 `fill` events plus the `optimalPath` overlay, just like Payload 4 here.

## Browser verification

Open this chapter at `http://localhost:5173/cortex/data-structures-and-algorithms/appendix-widget-catalog-dp-table` and:

1. Exercise step controls on each payload (Prev / Next / Play / Pause / Reset).
2. Confirm Payload 1's 6 steps walk the LIS table left-to-right: step 1 paints `dp[0][0]` filled (blue); step 4 paints `dp[0][3] = 2` filling (emerald) with an amber dashed arrow from `dp[0][2]`; step 6 paints `dp[0][5] = 3` filling with arrows from BOTH `dp[0][3]` and `dp[0][4]`. At the final step, the optimal-path overlay flips cells 2/3/5 to emerald (`--on-path`) and the caption reads `LIS = [2, 5, 7]`. Scrubbing back to step 5 removes the overlay (cells revert to filled blue).
3. Confirm Payload 2's 2D layout: the row gutter shows `a` / `b` and the column gutter shows `a` / `c` / `b` (with leading blank cells for the 0-prefix). Step 1 fills `dp[1][1] = 1` with one arrow from `dp[0][0]`; step 2 fills `dp[1][2] = 1` with TWO arrows (from `dp[0][2]` and `dp[1][1]` — the mismatch case). Step 6 fills `dp[2][3] = 2` with one arrow (the match case, diagonal only). Final-step overlay traces `[2,3] → [1,2] → [1,1] → [0,0]` emerald.
4. Confirm Payload 3's top-down mode: the kind badge reads `top-down` (emerald), and the recursion tree renders above the table (three nodes: `lcs(2,3)` at the root with `lcs(1,3)` and `lcs(2,2)` as children). At step 1 (`call r-2-3`) the root flips active (emerald). At step 3 (`return r-1-3 value=1 cell=[1,3]`) the `lcs(1,3)` tree node turns `returned` (slate) AND the table cell `dp[1][3]` flips from `-` to `1` and turns blue (filled). At step 5 (`cache-hit cell=[1,2]`) `dp[1][2]` flashes violet (the cached adornment) — and since `dp[1][2]` was pre-populated as `1` in `initial`, the violet flash appears OVER the already-blue filled cell, not on an empty one. The cache-hit does NOT advance the cursor or modify the tree. By the final step, every tree node is `returned` and the optimal-path overlay paints `dp[2][3]`, `dp[2][2]`, and `dp[1][1]` emerald.
5. Confirm Payload 4's three-predecessor cells: step 1 fills `dp[1][1] = 1` with THREE amber arrows (from `dp[0][0]`, `dp[0][1]`, `dp[1][0]`). Step 4's match case fills `dp[2][2] = 1` with only ONE arrow (the diagonal `dp[1][1]`). Final-step caption reads `2 edits: insert e, insert f` with the diagonal path painted emerald.
6. Confirm Payload 5's 4 × 5 layout: the widest table here. On a narrow viewport the canvas should scroll horizontally inside its own container — cell sizes do NOT shrink. At step 7, `dp[3][4] = 5` fills with arrows from `dp[2][4]` AND `dp[2][2]`. Final-step overlay traces the non-contiguous path `[3,4] → [2,4] → [1,1] → [0,0]` emerald — note the row-3 step is skipped (item 3 wasn't taken).
7. Confirm the rule annotation strip below the table updates each step (matches the current event's `rule` text) and is blank (preserves height) on non-fill events like `call` / `return` / `cache-hit` in Payload 3.
8. Confirm no `.d3-widget__error` divs render in the page.
9. Confirm devtools console is clean: no widget exceptions; no `dp-table:` warnings (this widget doesn't use marker canon — cell status is computed).

If any payload fails, fix and re-verify before committing.
