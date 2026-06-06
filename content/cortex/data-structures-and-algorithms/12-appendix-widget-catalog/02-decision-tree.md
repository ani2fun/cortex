---
title: decision-tree
summary: Backtracking enumeration / search rendered as a top-down tree of choices. One step per `enter` / `leaf` / `prune` / `backtrack` event; per-step node status (unseen / active / visited / accepted / rejected / pruned / popped) is computed from the event tape. Side panels track the current partial (`path`) and accumulated `solutions`. A transient amber arc fades in along the parent→child edge on backtrack to signal the return direction.
prereqs: []
---

# `decision-tree`

## Purpose

A single widget covers source Phase 11 (Backtracking — 13 source diagrams across four chapters). The model is a layered tree of choices: the root is the empty partial solution; each child edge is one decision; a DFS walk over the tree is the call stack of a recursive backtracker. The reader scrubs through the walk one event at a time and watches the tree light up the way a recursive call would actually explore it.

Two `kind` flags steer the surrounding semantics — same renderer, identical layout:

- `kind: "enumeration"` — every accepted leaf is one solution; the trace visits every reachable leaf (modulo pruning). The kind badge tints blue. Canonical examples: subsets, permutations, well-formed parentheses.
- `kind: "search"`      — the trace terminates as soon as a solution is found; remaining branches are typically pruned with `reason: "dead-end"`. The kind badge tints emerald. Canonical examples: grid path-find, backtrack-with-inverse on dead end.

The data model is small: `nodes` (with `id`, `label`, optional `parent`, optional `edgeLabel`) + `events` (a tape of `enter` / `leaf` / `prune` / `backtrack` operations). The renderer replays events to compute per-step state and projects to one fixed layered tree layout.

Node status is **computed**, not author-supplied:

- `unseen`   — node defined but not yet visited. Dashed slate outline, dim text.
- `active`   — the cursor is here right now (top of the recursion). Emerald rect, brightest fill.
- `visited`  — was active earlier; the cursor is now somewhere deeper or has returned past it. Blue rect.
- `accepted` — a `leaf` with `accept: true`. Emerald rect (more saturated) + ✓ checkmark in the top-right corner; the node's label is appended to the `solutions` panel.
- `rejected` — a `leaf` with `accept: false`. Slate rect; no append.
- `pruned`   — marked by a `prune` event. Red rect + dashed border + horizontal strike-through line, with a small reason pill rendered to the right (`dup` / `dead-end` / `constraint`). The whole subtree under the pruned node carries this status too.
- `popped`   — drawn after `backtrack` moves past this node and its subtree. Dimmed slate. Stays drawn so the reader can see the full exploration history at the end of the trace.

> **Source spec**: `docs/migration/widget-specs/decision-tree.md`
>
> **Scala module**: `client/src/main/scala/codefolio/client/components/cortex/widgets/DecisionTree.scala`
>
> **Event-tape model**: the renderer's per-step state is computed by folding `applyEvent` over `events[0..stepIdx]`. The `path` panel tracks the current partial (root→active labels) and `solutions` tracks the cumulative list of accepted leaves. The transient backtrack arc is keyed by `(childId, parentId)` and lives for exactly one step.

## Payload schema (reference card)

```ts
{
  title:  string,
  kind:   "enumeration" | "search",
  nodes:  [{
    id:         string,                 // unique within the spec
    label:      string,                 // display label, e.g. "[1,2]" or "(0,1)"
    parent?:    string,                 // omit for the root
    edgeLabel?: string                  // shown on the parent→child edge
  }],
  panels?: [{
    name: "path" | "solutions",         // panel rendered to the right of the canvas
    kind: "list"                        // only "list" supported today
  }],
  events: [
    // enter — descend along the edge to nodeId. Marks node active; pushes its label onto the path panel.
    { kind: "enter",     nodeId: string, msg: string },
    // leaf — terminal node. accept=true marks accepted + appends to solutions; accept=false marks rejected.
    { kind: "leaf",      nodeId: string, accept?: boolean, msg: string },
    // prune — mark node + entire subtree pruned; render a reason pill ("dup" / "dead-end" / "constraint").
    { kind: "prune",     nodeId: string, reason?: string, msg: string },
    // backtrack — pop the path panel; mark the prior subtree popped; nodeId becomes the new active (the parent we move to).
    { kind: "backtrack", nodeId: string, msg: string }
  ]
}
```

**Required**: `title`, `kind`, `nodes` (non-empty), `events` (non-empty), `events[].msg`, `events[].kind` (closed set), `nodes[].id`, `nodes[].label`.
**Optional**: `panels` (defaults to no panels — fine for tiny demos), per-node `parent` (absent → root) and `edgeLabel`, per-event `accept` (only `leaf` cares; defaults to `true`), per-event `reason` (only `prune` cares; defaults to `dup` for unknown values).

**Closed sets** (unknown values fall back to a safe default at parse time, so a typo never crashes the chapter):

- `kind` ∈ `{ "enumeration", "search" }` — unknown → `"enumeration"`.
- `events[].kind` ∈ `{ "enter", "leaf", "prune", "backtrack" }` — unknown → `"enter"`.
- `prune.reason` ∈ `{ "dup", "dead-end", "constraint" }` — unknown → `"dup"`.

**Event-replay rules**:

- `enter` demotes the previously-active node to `visited` (so a scrubbed-back mid-trace state reads correctly), marks the new node `active`, and pushes its label onto the path.
- `leaf` does NOT pop the path — the cursor stays on the leaf. The very next event is typically `backtrack` or another `enter` (under the same parent if siblings remain).
- `prune` marks the whole subtree pruned (including the head) but does NOT touch `path` or `active`. It just records the reason; the next event (usually `backtrack`) moves the cursor.
- `backtrack`'s `nodeId` is the node we're moving UP to. The popped child + its descendants dim to `popped`, with accepted/pruned/rejected statuses preserved (terminal statuses don't get overwritten). The active flips to `nodeId`. The path's last entry pops off.

**Panel semantics**:

- `path` = labels along the root→active path (so the topmost entry is always the root, the bottommost is the current active). It empties when there's no active cursor.
- `solutions` = labels of nodes that received a `leaf accept=true` event, in event order. Append-only — backtracks don't remove solutions.

## Representative payloads

### Payload 1 — minimum (3-node tree exercising every event kind)

The smallest interesting payload — root + two leaves, with one `accept=true` and one `prune` (with `reason: "constraint"`). Exercises the renderer's empty-state path (one `enter` per node), the accept ✓ overlay, the prune strike-through + reason pill, and the backtrack arc.

```d3 widget=decision-tree
{
  "title": "3-node tree — every event kind",
  "kind": "enumeration",
  "nodes": [
    {"id": "root", "label": "[]"},
    {"id": "a",    "label": "[A]", "parent": "root", "edgeLabel": "A"},
    {"id": "b",    "label": "[B]", "parent": "root", "edgeLabel": "B"}
  ],
  "panels": [
    {"name": "path",      "kind": "list"},
    {"name": "solutions", "kind": "list"}
  ],
  "events": [
    {"kind": "enter",     "nodeId": "root", "msg": "Start at the empty partial []."},
    {"kind": "enter",     "nodeId": "a",    "msg": "Choose A — descend into [A]."},
    {"kind": "leaf",      "nodeId": "a",    "accept": true, "msg": "[A] is a solution. Record it."},
    {"kind": "backtrack", "nodeId": "root", "msg": "Unchoose A — back to root."},
    {"kind": "prune",     "nodeId": "b",    "reason": "constraint", "msg": "[B] violates a constraint. Prune."}
  ]
}
```

### Payload 2 — subsets of [1, 2, 3] (the canonical unconditional enumeration)

The textbook backtracking demo — eight subsets across an 8-node tree. Each `enter` descends to a child; each `leaf accept=true` records the current partial; each `backtrack` returns to the parent. Exercises:

- The full path-panel push/pop cycle: `path` grows as we descend, shrinks as we backtrack.
- The `solutions` panel accumulating each subset in order: `[]` → `[]` `[1]` → … → all 8 subsets by the final step.
- The `visited` (blue) trail of past-active ancestors as the cursor descends — at any mid-trace step you see `root` blue, `n1` blue, `n1-2` blue, `n1-2-3` active (emerald).
- The `popped` (dimmed) leaves remaining drawn after backtrack so you can see what's already been explored.

```d3 widget=decision-tree
{
  "title": "Subsets of [1, 2, 3]",
  "kind": "enumeration",
  "nodes": [
    {"id": "root",    "label": "[]"},
    {"id": "n1",      "label": "[1]",     "parent": "root",   "edgeLabel": "+1"},
    {"id": "n1-2",    "label": "[1,2]",   "parent": "n1",     "edgeLabel": "+2"},
    {"id": "n1-2-3",  "label": "[1,2,3]", "parent": "n1-2",   "edgeLabel": "+3"},
    {"id": "n1-3",    "label": "[1,3]",   "parent": "n1",     "edgeLabel": "+3"},
    {"id": "n2",      "label": "[2]",     "parent": "root",   "edgeLabel": "+2"},
    {"id": "n2-3",    "label": "[2,3]",   "parent": "n2",     "edgeLabel": "+3"},
    {"id": "n3",      "label": "[3]",     "parent": "root",   "edgeLabel": "+3"}
  ],
  "panels": [
    {"name": "path",      "kind": "list"},
    {"name": "solutions", "kind": "list"}
  ],
  "events": [
    {"kind": "enter",     "nodeId": "root",    "msg": "Start []"},
    {"kind": "leaf",      "nodeId": "root",    "accept": true, "msg": "Record []"},
    {"kind": "enter",     "nodeId": "n1",      "msg": "Choose 1"},
    {"kind": "leaf",      "nodeId": "n1",      "accept": true, "msg": "Record [1]"},
    {"kind": "enter",     "nodeId": "n1-2",    "msg": "Choose 2"},
    {"kind": "leaf",      "nodeId": "n1-2",    "accept": true, "msg": "Record [1, 2]"},
    {"kind": "enter",     "nodeId": "n1-2-3",  "msg": "Choose 3"},
    {"kind": "leaf",      "nodeId": "n1-2-3",  "accept": true, "msg": "Record [1, 2, 3]"},
    {"kind": "backtrack", "nodeId": "n1-2",    "msg": "Unchoose 3"},
    {"kind": "backtrack", "nodeId": "n1",      "msg": "Unchoose 2"},
    {"kind": "enter",     "nodeId": "n1-3",    "msg": "Choose 3 (under [1])"},
    {"kind": "leaf",      "nodeId": "n1-3",    "accept": true, "msg": "Record [1, 3]"},
    {"kind": "backtrack", "nodeId": "n1",      "msg": "Unchoose 3"},
    {"kind": "backtrack", "nodeId": "root",    "msg": "Unchoose 1"},
    {"kind": "enter",     "nodeId": "n2",      "msg": "Choose 2"},
    {"kind": "leaf",      "nodeId": "n2",      "accept": true, "msg": "Record [2]"},
    {"kind": "enter",     "nodeId": "n2-3",    "msg": "Choose 3"},
    {"kind": "leaf",      "nodeId": "n2-3",    "accept": true, "msg": "Record [2, 3]"},
    {"kind": "backtrack", "nodeId": "n2",      "msg": "Unchoose 3"},
    {"kind": "backtrack", "nodeId": "root",    "msg": "Unchoose 2"},
    {"kind": "enter",     "nodeId": "n3",      "msg": "Choose 3"},
    {"kind": "leaf",      "nodeId": "n3",      "accept": true, "msg": "Record [3]"}
  ]
}
```

### Payload 3 — well-formed parens with n=2 (pruning on a constraint violation)

The canonical conditional-enumeration demo. The right subtree under root (starting with `)`) violates the "close count cannot exceed open count" constraint, so it's pruned at first descent. Exercises:

- A `prune` event with `reason: "constraint"` — the whole `)`-rooted subtree (just one node here, but the mechanism generalises to deeper subtrees) gets the red dashed strike-through + the reason pill.
- The persistent reason pill — it stays drawn after the cursor moves on (the pill is anchored to the pruned head node, not the cursor).
- A non-trivial tree where the left subtree fully explores two accepted leaves (`(())` and `()()`) before the prune happens at the very end.

```d3 widget=decision-tree
{
  "title": "Well-formed parens, n = 2",
  "kind": "enumeration",
  "nodes": [
    {"id": "root",      "label": "\"\""},
    {"id": "lp",        "label": "(",      "parent": "root",   "edgeLabel": "("},
    {"id": "lplp",      "label": "((",     "parent": "lp",     "edgeLabel": "("},
    {"id": "lplprp",    "label": "(()",    "parent": "lplp",   "edgeLabel": ")"},
    {"id": "balanced1", "label": "(())",   "parent": "lplprp", "edgeLabel": ")"},
    {"id": "lprp",      "label": "()",     "parent": "lp",     "edgeLabel": ")"},
    {"id": "lprplp",    "label": "()(",    "parent": "lprp",   "edgeLabel": "("},
    {"id": "balanced2", "label": "()()",   "parent": "lprplp", "edgeLabel": ")"},
    {"id": "rp",        "label": ")",      "parent": "root",   "edgeLabel": ")"}
  ],
  "panels": [
    {"name": "path",      "kind": "list"},
    {"name": "solutions", "kind": "list"}
  ],
  "events": [
    {"kind": "enter",     "nodeId": "root",      "msg": "Start with empty string"},
    {"kind": "enter",     "nodeId": "lp",        "msg": "Append ( — open=1, close=0"},
    {"kind": "enter",     "nodeId": "lplp",      "msg": "Append ( — open=2, close=0"},
    {"kind": "enter",     "nodeId": "lplprp",    "msg": "Append ) — open=2, close=1"},
    {"kind": "enter",     "nodeId": "balanced1", "msg": "Append ) — open=2, close=2"},
    {"kind": "leaf",      "nodeId": "balanced1", "accept": true, "msg": "Record (())"},
    {"kind": "backtrack", "nodeId": "lplprp",    "msg": "Unwind one"},
    {"kind": "backtrack", "nodeId": "lplp",      "msg": "Unwind one"},
    {"kind": "backtrack", "nodeId": "lp",        "msg": "Unwind to ("},
    {"kind": "enter",     "nodeId": "lprp",      "msg": "Try ) — open=1, close=1"},
    {"kind": "enter",     "nodeId": "lprplp",    "msg": "Append ( — open=2, close=1"},
    {"kind": "enter",     "nodeId": "balanced2", "msg": "Append )"},
    {"kind": "leaf",      "nodeId": "balanced2", "accept": true, "msg": "Record ()()"},
    {"kind": "backtrack", "nodeId": "lprplp",    "msg": "Unwind"},
    {"kind": "backtrack", "nodeId": "lprp",      "msg": "Unwind"},
    {"kind": "backtrack", "nodeId": "lp",        "msg": "Unwind"},
    {"kind": "backtrack", "nodeId": "root",      "msg": "All ( branches done — try )"},
    {"kind": "prune",     "nodeId": "rp",        "reason": "constraint", "msg": "Cannot start with ) — close > open."}
  ]
}
```

### Payload 4 — grid path-find (`kind: "search"`, prune-after-success)

Backtracking SEARCH — the trace terminates as soon as `(2,2)` is reached, and the remaining `(1,0)`-rooted subtree is pruned with `reason: "dead-end"` to communicate "we don't need to explore further; we already have an answer". Exercises:

- The `kind: "search"` badge (emerald) — visually distinct from the enumeration variants above.
- An accepted leaf that's NOT a recursion-base case but a search success: `(2,2)` accepts because the goal matches, not because options were exhausted.
- A `prune` event with `reason: "dead-end"` applied to an alternate sibling AFTER a solution was found — the canonical "branch-and-bound style pruning".
- A single-panel layout (`path` only — `solutions` would only ever hold one entry in a search trace).

```d3 widget=decision-tree
{
  "title": "Find path to (2, 2)",
  "kind": "search",
  "nodes": [
    {"id": "s00",   "label": "(0,0)"},
    {"id": "s01",   "label": "(0,1)", "parent": "s00",  "edgeLabel": "→"},
    {"id": "s02",   "label": "(0,2)", "parent": "s01",  "edgeLabel": "→"},
    {"id": "s12",   "label": "(1,2)", "parent": "s02",  "edgeLabel": "↓"},
    {"id": "s22",   "label": "(2,2)", "parent": "s12",  "edgeLabel": "↓"},
    {"id": "s10",   "label": "(1,0)", "parent": "s00",  "edgeLabel": "↓"},
    {"id": "s11",   "label": "(1,1)", "parent": "s10",  "edgeLabel": "→"}
  ],
  "panels": [{"name": "path", "kind": "list"}],
  "events": [
    {"kind": "enter",     "nodeId": "s00", "msg": "Start at (0,0)"},
    {"kind": "enter",     "nodeId": "s01", "msg": "Try → (0,1)"},
    {"kind": "enter",     "nodeId": "s02", "msg": "Try → (0,2)"},
    {"kind": "enter",     "nodeId": "s12", "msg": "Try ↓ (1,2)"},
    {"kind": "enter",     "nodeId": "s22", "msg": "Try ↓ (2,2)"},
    {"kind": "leaf",      "nodeId": "s22", "accept": true, "msg": "Goal reached — return path."},
    {"kind": "prune",     "nodeId": "s10", "reason": "dead-end", "msg": "Solution found — prune alt branches."}
  ]
}
```

### Payload 5 — backtrack via `gInverse` on a dead-end (`leaf accept=false`)

Backtracking SEARCH where the first branch hits a literal dead end (`leaf accept=false`) and the algorithm undoes a transformation via an inverse operator to try the other branch. Exercises:

- A `leaf accept=false` — the leaf is marked `rejected` (slate, no checkmark, no append to solutions). Distinct from `prune` semantics: rejection is "I reached the bottom and it doesn't work", pruning is "I'm not even going down this branch".
- A `backtrack` to a non-root parent immediately followed by a sibling `enter` — the canonical "try the other choice" mechanic.
- A search trace where the path panel grows, shrinks back by one, then grows again — the path is a moving cursor, not a history.
- The minimum tree (4 nodes) that demonstrates both `leaf accept=false` and a successful sibling recovery.

```d3 widget=decision-tree
{
  "title": "Backtrack via gInverse on dead end",
  "kind": "search",
  "nodes": [
    {"id": "s0",   "label": "init"},
    {"id": "s0a",  "label": "S1",   "parent": "s0",  "edgeLabel": "g"},
    {"id": "s0aa", "label": "S2",   "parent": "s0a", "edgeLabel": "g"},
    {"id": "s0ab", "label": "S3",   "parent": "s0a", "edgeLabel": "g'"}
  ],
  "panels": [{"name": "path", "kind": "list"}],
  "events": [
    {"kind": "enter",     "nodeId": "s0",   "msg": "Initial state"},
    {"kind": "enter",     "nodeId": "s0a",  "msg": "Apply g → S1"},
    {"kind": "enter",     "nodeId": "s0aa", "msg": "Apply g again → S2"},
    {"kind": "leaf",      "nodeId": "s0aa", "accept": false, "msg": "Dead end — S2 isn't a goal."},
    {"kind": "backtrack", "nodeId": "s0a",  "msg": "Apply gInverse — undo S2; back to S1."},
    {"kind": "enter",     "nodeId": "s0ab", "msg": "Apply g' instead → S3"},
    {"kind": "leaf",      "nodeId": "s0ab", "accept": true,  "msg": "Solution found at S3."}
  ]
}
```

## Compression notes

When porting a source `// Interactive Diagram (N frames): …` for this widget, target **5–14 widget steps** (source diagrams range 4–57 frames; the longest single source diagram is the 57-frame "build + backtrack" demo). Compression strategy:

- **Per-frame "the cursor moves down one node" beats** → fold into a single `enter` event with a descriptive `msg`. The renderer fades the new active highlight; you don't need an intermediate frame.
- **Compute steps inside a node** (e.g. checking a constraint before recursing) → fold into the parent's `enter.msg` ("Open count = 2; close count = 0 — descend") or into a `prune.msg` if the constraint fails. Don't emit a separate event for the bookkeeping.
- **Multi-level descent then backtrack** (the most common pattern) → one `enter` per step down, one `leaf` at the bottom, one `backtrack` per step up. Subsets of [1,2,3] (Payload 2) = 22 events covering all 8 subsets — that's the canonical compression.
- **Pruned-subtree** in source typically renders frame-by-frame as the algorithm "checks then skips" each branch — collapse to ONE `prune` event on the subtree head with the canonical reason. The renderer marks the whole subtree pruned in one shot.
- **Backtracking-search with early exit** → emit the success `leaf accept=true`, then a single `prune reason=dead-end` on the remaining-but-unexplored branch root. The reader sees the unexplored fork dim out without needing the algorithm to actually walk into it.

The 57-frame "build + backtrack" source diagram compresses to roughly 14 events: 7 down-and-up descent/return pairs around a single 8-node subsets-style tree — same compression as Payload 2.

## Browser verification

Open this chapter at `http://localhost:5173/cortex/data-structures-and-algorithms/appendix-widget-catalog-decision-tree` and:

1. Exercise step controls on each payload (Prev / Next / Play / Pause / Reset).
2. Confirm Payload 1's 5 steps walk through every event kind: step 1 marks `root` active; step 2 descends to `a` (active), `root` demotes to `visited`; step 3 paints `a` with the emerald accepted fill + ✓ in the top-right; the path stays `[] [A]` and solutions reads `[A]`; step 4 backtracks to root (the amber arc fades in along the `root`→`a` edge for that step, then disappears at step 5), `a` dims to popped, path is `[]`; step 5 paints `b` red with strike-through + the `constraint` pill to its right. Solutions is still `[A]` only.
3. Confirm Payload 2's 8-subset enumeration: the `path` panel grows/shrinks as the cursor descends and backtracks; the `solutions` panel accumulates `[]` → `[1]` → `[1,2]` → `[1,2,3]` → `[1,3]` → `[2]` → `[2,3]` → `[3]` across the 22 steps. The `visited` (blue) trail of past-active ancestors should be visible whenever the cursor is at a deeper node (e.g. at step 7 — `n1-2-3` is active emerald; `n1-2`, `n1`, and `root` are all blue visited). After step 8 (the `[1,2,3]` accept), watch the backtrack arc fade in along `n1-2-3 → n1-2` at step 9, then `n1-2 → n1` at step 10.
4. Confirm Payload 3's prune badge: by step 18 the `rp` node is red + dashed + striked, with a `constraint` pill rendered to its right. The pill persists (this is the final step). Solutions reads `(())` `()()`. Before step 18, the right subtree of the tree is in the `unseen` state (dashed slate); the prune at step 18 flips just the `rp` node (it has no children in this payload).
5. Confirm Payload 4 wears the `search` (emerald) kind badge rather than `enumeration` (blue). Watch the trace walk → → ↓ ↓ to `(2,2)`, accept it (emerald + ✓), then prune the `(1,0)` subtree at step 7 with the `dead-end` reason. Path panel reads `(0,0) (0,1) (0,2) (1,2) (2,2)` at the end (the trace doesn't backtrack — it terminates on success).
6. Confirm Payload 5's `leaf accept=false`: step 4 paints `s0aa` slate (rejected) WITHOUT a checkmark and WITHOUT appending to a solutions list (this payload has no solutions panel — only path). Step 5 backtracks one level to `s0a`, the path shrinks from `init S1 S2` to `init S1`, the amber arc fades in along `s0aa → s0a`. Step 6 enters the sibling `s0ab` (the path grows back to `init S1 S3`); step 7 paints it emerald accepted + ✓.
7. Confirm the amber backtrack arc is visible for exactly ONE step on each `backtrack` event (Payloads 1, 2, 3, 5) — it fades in along the parent→child Bézier and disappears on the next step.
8. Confirm no `.d3-widget__error` divs render in the page.
9. Confirm devtools console is clean: no widget exceptions; no `decision-tree:` marker-canon warnings (this widget doesn't use markers — node status is computed).

If any payload fails, fix and re-verify before committing.
