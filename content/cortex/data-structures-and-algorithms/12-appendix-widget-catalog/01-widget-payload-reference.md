---
title: Widget Payload Reference
summary: The hand-authored `d3 widget=` JSON reference — the shared VizGraph schema plus one representative payload per layout. For the LIVE trace-driven renderers, see the Renderer Gallery.
prereqs: []
---

# Widget Payload Reference

A `d3 widget=<layout>` fenced block hand-authors a **static** diagram: you write the exact `VizGraph` JSON — nodes, edges, cursors, per-step annotations — and the layout draws it. Reach for one when you want a fixed, curated illustration that always renders the same way (a textbook BST, a single sliding-window snapshot, a labelled segment tree) and you do not want it tied to running code. When instead you want a diagram **derived from an actual program trace** — step through real Python/Java execution and watch the structure mutate — use a runnable `viz=…` block; those are demonstrated live in the [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery). The schema below is shared by every static layout, so each per-layout section only documents its **deltas**: the slot/edge/meta conventions specific to that structure, plus one representative payload copied verbatim.

## Common payload schema

Every inline `d3 widget=<layout>` fence carries a raw `VizGraph` object with the same shape. A graph is a `title` plus an ordered list of `steps`; the player walks the steps with Prev / Next / Play / Pause / Reset. Each step is a full snapshot — re-list every node and edge you want visible, not just the deltas.

```ts
{
  title: string,
  steps: [{
    nodes: [{
      id:         string,   // "${containerId}#${index}" or any id unique within the step
      label:      string,   // text drawn inside the node
      kind:       string,   // "node" by default; some layouts use "entry"/"leaf"/"root"/"terminal"
      slot:       number | null,  // 0/1-based position for slot-addressed layouts; null when order comes from edges
      meta:       [],       // optional [{ "name": string, "value": string }] — small text below the node
      cardId:     "",       // unused by static fences
      layoutKind: ""        // unused by static fences
    }],
    edges: [{
      from:  string,        // source node id
      to:    string,        // target node id
      label: string         // edge label: "left"/"right"/"next"/"prev", an edge weight, or ""
    }],
    cursor:     [],         // named carets: [{ "name": string, "target": nodeId, "color": "#6366f1" }]
    highlight:  [],         // node ids on the active path (blue/gold)
    changed:    [],         // node ids just written / compared / inserted (green flash)
    removed:    [],         // node ids being deleted (fade out)
    annotation: string,     // caption shown beneath the diagram for this step
    line:       0,          // optional source-line emphasis (0 = none)
    frames:     [],         // unused by static fences
    cardCursor: []          // unused by static fences
  }]
}
```

**Id convention**: ids follow `${containerId}#${index}` — a per-structure prefix, a `#`, then the index (e.g. `arr#0`, `arr#3`). Layouts that own two structures in one payload (two arrays, two chains) split on the **last** `#` to decide which structure a node belongs to. Layouts whose order is edge-derived (trees, lists, tries, graphs) instead use short opaque ids like `n0`, `A`, `root` — the rule is only that ids are unique within a step. The standard cursor colours are `#6366f1` (indigo, primary pointer), `#f59e0b` (amber, secondary pointer), `#10b981` (green, new/found), and `#ef4444` (red, delete target).

The sections below only call out each layout's **deltas** from this schema.

## Linear structures

### array-1d

A horizontal row of circle nodes, one per element, placed at `x = slot × CELL_DX` so the visual column equals the array index. The default renderer for 1-D array work — binary search, two-pointer, sliding window.

- **slot** drives x-position (0-based); an in-place swap moves the label between two fixed boxes.
- **cursor** carries named pointers (`left`, `right`, `mid`, `slow`, `fast`, `i`, `j`, …) as carets above the target cell.
- **highlight** marks the active window/subarray; **changed** flashes cells just written or compared.
- **meta** on a node renders small text below the circle; two distinct array ids in one payload stack as two rows.

> Layout: `client/src/d3/array-layout.ts` · widget id `array-1d`

```d3 widget=array-1d
{
  "title": "Array A = [4, 2, 7, 1, 9, 3]",
  "steps": [
    {
      "nodes": [
        {"id": "a#0", "label": "4", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "a#1", "label": "2", "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "a#2", "label": "7", "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "a#3", "label": "1", "kind": "node", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "a#4", "label": "9", "kind": "node", "slot": 4, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "a#5", "label": "3", "kind": "node", "slot": 5, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [], "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "A = [4, 2, 7, 1, 9, 3]. Six elements, 0-indexed.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven array-1d renderer.

### list-single / list-double

A horizontal left-to-right chain of circle nodes ordered by `next`-labelled edges. Use `list-single` for singly-linked lists and `list-double` for doubly-linked lists (same layout, the renderer also draws `prev` arrows).

- **Node order is edge-derived**, not input order — the node with no incoming `next` edge is the head; `slot` is unused (`null`).
- **edges** carry `label: "next"` (defines chain order) or `label: "prev"` (doubly-linked back-pointer).
- **cursor** marks `head`, `cur`, `prev`, `slow`, `fast`, etc.; **removed** fades deleted nodes; **changed** flashes inserted/rewired nodes; two separate chains stack as two rows.
- A cycle in the `next`-graph falls back to the generic force-directed graph layout so the back-edge shows as a visible loop.

> Layout: `client/src/d3/linked-list-layout.ts` · widget id `list-single`

```d3 widget=list-single
{
  "title": "Singly-linked list: 1 → 2 → 3 → 4",
  "steps": [
    {
      "nodes": [
        {"id": "n0", "label": "1", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n1", "label": "2", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n2", "label": "3", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n3", "label": "4", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "n0", "to": "n1", "label": "next"},
        {"from": "n1", "to": "n2", "label": "next"},
        {"from": "n2", "to": "n3", "label": "next"}
      ],
      "cursor": [{"name": "head", "target": "n0", "color": "#6366f1"}],
      "highlight": [], "changed": [], "removed": [],
      "annotation": "Four-node singly-linked list. head → 1 → 2 → 3 → 4 → null.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven list-single / list-double renderer.

### stack + queue

Two slot-addressed layouts. `stack` is a **vertical column** (slot 0 at the bottom, higher slots stack upward); `queue` is a **horizontal row** (slot 0 at the front/left, higher slots toward the back/right). Both read `node.slot` and ignore edges.

- **stack** slot convention: slot 0 = bottom; the most-recently-pushed element has the highest slot and sits on top.
- **queue** slot convention: slot 0 = front (next to dequeue); newly enqueued elements get the highest slot at the back.
- **cursor** marks `top`, `front`, `back`, `ptr`, …; **changed** flashes newly pushed/just-dequeued nodes; **removed** fades popped/dequeued nodes.

> Layout: `client/src/d3/stack-queue-layout.ts` · widget id `stack`

```d3 widget=stack
{
  "title": "Stack — push 5 then push 6",
  "steps": [
    {
      "nodes": [
        {"id": "s0", "label": "2", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s1", "label": "8", "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s2", "label": "4", "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s3", "label": "7", "kind": "node", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [{"name": "top", "target": "s3", "color": "#6366f1"}],
      "highlight": [], "changed": [], "removed": [],
      "annotation": "Stack: [2, 8, 4, 7] (top=7 at slot 3).",
      "line": 0, "frames": [], "cardCursor": []
    },
    {
      "nodes": [
        {"id": "s0", "label": "2", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s1", "label": "8", "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s2", "label": "4", "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s3", "label": "7", "kind": "node", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s4", "label": "5", "kind": "node", "slot": 4, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [{"name": "top", "target": "s4", "color": "#6366f1"}],
      "highlight": [], "changed": ["s4"], "removed": [],
      "annotation": "push(5): new top = 5 at slot 4.",
      "line": 0, "frames": [], "cardCursor": []
    },
    {
      "nodes": [
        {"id": "s0", "label": "2", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s1", "label": "8", "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s2", "label": "4", "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s3", "label": "7", "kind": "node", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s4", "label": "5", "kind": "node", "slot": 4, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "s5", "label": "6", "kind": "node", "slot": 5, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [{"name": "top", "target": "s5", "color": "#6366f1"}],
      "highlight": [], "changed": ["s5"], "removed": [],
      "annotation": "push(6): new top = 6 at slot 5.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven stack + queue renderer.

### stack (call-stack)

The same `stack` layout applied to call frames: a LIFO column of activation records. Slot 0 is the bottom (oldest) frame, typically `main`; the topmost frame is the currently executing function. For recursion-**tree** diagrams (the call tree rather than the frame stack) use `tree-binary` instead, with caller→callee edges labelled `"left"` / `"right"`.

- **label** holds the function name or a short description; **slot** is the frame depth (0 = bottom).
- **cursor** marks `top` / `callee` / `base`; **changed** flashes frames just pushed; **removed** fades frames just popped.

> Layout: `client/src/d3/stack-queue-layout.ts` · widget id `stack`

```d3 widget=stack
{
  "title": "Call stack: main → foo → bar",
  "steps": [
    {
      "nodes": [
        {"id": "f0", "label": "main", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "f1", "label": "foo",  "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "f2", "label": "bar",  "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [{"name": "top", "target": "f2", "color": "#6366f1"}],
      "highlight": ["f2"], "changed": [], "removed": [],
      "annotation": "Call stack: main called foo, foo called bar. bar is the active frame (top).",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven call-stack renderer.

## Trees & heaps

### tree-binary

A rooted binary tree laid out by a Reingold-Tilford-style recursive slot algorithm. The root has no incoming edges; children sit below their parent in two columns (left subtree left, right subtree right).

- **edges** carry `label: "left"` or `label: "right"`; any other/unlabelled edge is treated as `"left"`. **slot** is unused (`null`) — structure comes from edges.
- **cursor** marks `cur`, `parent`, `node`, …; **highlight** marks the active search/traversal path; **changed** flashes compared/inserted nodes; **removed** fades deleted nodes.

> Layout: `client/src/d3/tree-layout.ts` · widget id `tree-binary`

```d3 widget=tree-binary
{
  "title": "BST: [1, 2, 3, 4, 5, 6, 7]",
  "steps": [
    {
      "nodes": [
        {"id": "n4", "label": "4", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n2", "label": "2", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n6", "label": "6", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n1", "label": "1", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n3", "label": "3", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n5", "label": "5", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n7", "label": "7", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "n4", "to": "n2", "label": "left"},
        {"from": "n4", "to": "n6", "label": "right"},
        {"from": "n2", "to": "n1", "label": "left"},
        {"from": "n2", "to": "n3", "label": "right"},
        {"from": "n6", "to": "n5", "label": "left"},
        {"from": "n6", "to": "n7", "label": "right"}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Complete BST with root 4. Left subtree ≤ 4, right subtree ≥ 4.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven tree-binary renderer.

### tree-binary (heap)

A heap is a complete binary tree, so it reuses the `tree-binary` layout and key unchanged. The only difference from a BST is where the edges come from: heap nodes get `left`/`right` edges from the array-index formula, not a comparison key.

- **Edge construction**: for a 1-indexed heap node `i`, add `{from: "n${i}", to: "n${2i}", label: "left"}` and `{from: "n${i}", to: "n${2i+1}", label: "right"}` when those children exist.
- Node ids `n1`, `n2`, … match the heap index. **cursor** marks the active sift position (`i`); **changed** flashes just-swapped nodes during sift-up / sift-down.

> Layout: `client/src/d3/tree-layout.ts` · widget id `tree-binary`

```d3 widget=tree-binary
{
  "title": "Min-heap: root=1, all parent ≤ children",
  "steps": [
    {
      "nodes": [
        {"id": "n1", "label": "1",  "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n2", "label": "3",  "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n3", "label": "5",  "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n4", "label": "7",  "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n5", "label": "9",  "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n6", "label": "11", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "n7", "label": "13", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "n1", "to": "n2", "label": "left"},  {"from": "n1", "to": "n3", "label": "right"},
        {"from": "n2", "to": "n4", "label": "left"},  {"from": "n2", "to": "n5", "label": "right"},
        {"from": "n3", "to": "n6", "label": "left"},  {"from": "n3", "to": "n7", "label": "right"}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Min-heap: parent ≤ children at every level. Minimum is always at the root.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven tree-binary (heap) renderer.

### trie

An N-ary prefix tree using the same recursive slot algorithm as `tree-binary`, but without left/right discrimination — every edge is parent→child and a node may have N children. Edge labels carry the single character (or short string for radix variants) consumed on that edge.

- **kind** is `"terminal"` for nodes that end a complete word (drawn with a double-ring marker) and `"node"` for intermediate prefix nodes. **slot** is unused (`null`).
- **Root convention**: the root has no incoming edge and `label: ""`; it is never terminal.
- **cursor** marks `cur` / `walk`; **highlight** marks the active path; **changed** flashes newly created nodes during insertion.

> Layout: `client/src/d3/trie-layout.ts` · widget id `trie`

```d3 widget=trie
{
  "title": "Trie: [\"do\", \"dog\", \"dot\", \"cat\", \"car\"]",
  "steps": [
    {
      "nodes": [
        {"id": "root", "label": "",   "kind": "node",     "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "d",    "label": "d",  "kind": "node",     "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "do",   "label": "o",  "kind": "terminal", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "dog",  "label": "g",  "kind": "terminal", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "dot",  "label": "t",  "kind": "terminal", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "c",    "label": "c",  "kind": "node",     "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "ca",   "label": "a",  "kind": "node",     "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "cat",  "label": "t",  "kind": "terminal", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "car",  "label": "r",  "kind": "terminal", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "root", "to": "d",   "label": "d"},
        {"from": "d",    "to": "do",  "label": "o"},
        {"from": "do",   "to": "dog", "label": "g"},
        {"from": "do",   "to": "dot", "label": "t"},
        {"from": "root", "to": "c",   "label": "c"},
        {"from": "c",    "to": "ca",  "label": "a"},
        {"from": "ca",   "to": "cat", "label": "t"},
        {"from": "ca",   "to": "car", "label": "r"}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Trie storing: do, dog, dot, cat, car. Terminal nodes (double ring) mark complete words.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven trie renderer.

### segment-tree

A complete binary tree addressed by 1-based BFS index — each node's position derives purely from its index (level `k = ⌊log₂ i⌋`, slot within level `s = i − 2^k`). Each node stores a range aggregate (sum, min, max).

- **kind** is `"node"` for internal range-covering nodes and `"leaf"` for single-element ranges (no children, only an incoming edge). Node ids are the BFS index as a string (`"1"` = root, left child of `k` = `"2k"`, right = `"2k+1"`).
- **edges** go **child → parent** with `label: ""`; **meta** carries `[{"name": "range", "value": "[l,r]"}]`.
- **highlight** marks the traversal/query path; **changed** flashes answer segments or just-updated nodes.

> Layout: `client/src/d3/segment-tree-layout.ts` · widget id `segment-tree`

```d3 widget=segment-tree
{
  "title": "Sum segment tree over A = [1, 3, 5, 7]",
  "steps": [
    {
      "nodes": [
        {"id": "1", "label": "16", "kind": "node", "slot": 1, "meta": [{"name": "range", "value": "[0,3]"}], "cardId": "", "layoutKind": ""},
        {"id": "2", "label": "4",  "kind": "node", "slot": 2, "meta": [{"name": "range", "value": "[0,1]"}], "cardId": "", "layoutKind": ""},
        {"id": "3", "label": "12", "kind": "node", "slot": 3, "meta": [{"name": "range", "value": "[2,3]"}], "cardId": "", "layoutKind": ""},
        {"id": "4", "label": "1",  "kind": "leaf", "slot": 4, "meta": [{"name": "range", "value": "[0,0]"}], "cardId": "", "layoutKind": ""},
        {"id": "5", "label": "3",  "kind": "leaf", "slot": 5, "meta": [{"name": "range", "value": "[1,1]"}], "cardId": "", "layoutKind": ""},
        {"id": "6", "label": "5",  "kind": "leaf", "slot": 6, "meta": [{"name": "range", "value": "[2,2]"}], "cardId": "", "layoutKind": ""},
        {"id": "7", "label": "7",  "kind": "leaf", "slot": 7, "meta": [{"name": "range", "value": "[3,3]"}], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "2", "to": "1", "label": ""},
        {"from": "3", "to": "1", "label": ""},
        {"from": "4", "to": "2", "label": ""},
        {"from": "5", "to": "2", "label": ""},
        {"from": "6", "to": "3", "label": ""},
        {"from": "7", "to": "3", "label": ""}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Sum segment tree built over A = [1, 3, 5, 7]. Root stores total sum 16; each node stores the sum of its range.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven segment-tree renderer.

### fenwick

A Fenwick (Binary Indexed Tree) drawn as a **1-indexed horizontal row** of cells — each cell `bit[i]` stores an aggregate over its power-of-two responsibility range.

- **kind** is always `"node"` (a flat BIT has no root/leaf split). Node ids are the 1-based index as a string; **slot** equals that index and drives x-position. **meta** carries `[{"name": "range", "value": "[l,r]"}]`.
- **edges** represent one LSB-jump step — query `i → i − lowbit(i)` or update `i → i + lowbit(i)` — and render as arcs **below** the row, with `label: "query" | "update" | ""`.
- **highlight** marks the traversal path; **changed** flashes cells read as an answer segment or just written.

> Layout: `client/src/d3/fenwick-layout.ts` · widget id `fenwick`

```d3 widget=fenwick
{
  "title": "Fenwick tree over A = [1, 2, 3, 4, 5, 6, 7, 8]",
  "steps": [
    {
      "nodes": [
        {"id": "1", "label": "1",  "kind": "node", "slot": 1, "meta": [{"name": "range", "value": "[1,1]"}], "cardId": "", "layoutKind": ""},
        {"id": "2", "label": "3",  "kind": "node", "slot": 2, "meta": [{"name": "range", "value": "[1,2]"}], "cardId": "", "layoutKind": ""},
        {"id": "3", "label": "3",  "kind": "node", "slot": 3, "meta": [{"name": "range", "value": "[3,3]"}], "cardId": "", "layoutKind": ""},
        {"id": "4", "label": "10", "kind": "node", "slot": 4, "meta": [{"name": "range", "value": "[1,4]"}], "cardId": "", "layoutKind": ""},
        {"id": "5", "label": "5",  "kind": "node", "slot": 5, "meta": [{"name": "range", "value": "[5,5]"}], "cardId": "", "layoutKind": ""},
        {"id": "6", "label": "11", "kind": "node", "slot": 6, "meta": [{"name": "range", "value": "[5,6]"}], "cardId": "", "layoutKind": ""},
        {"id": "7", "label": "7",  "kind": "node", "slot": 7, "meta": [{"name": "range", "value": "[7,7]"}], "cardId": "", "layoutKind": ""},
        {"id": "8", "label": "36", "kind": "node", "slot": 8, "meta": [{"name": "range", "value": "[1,8]"}], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "BIT built over A = [1, 2, 3, 4, 5, 6, 7, 8]. Each cell i stores the prefix sum of A[i−lowbit(i)+1..i]. Odd-indexed cells cover one element; even-indexed cells cover a power-of-2 span.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven fenwick renderer.

## Maps, sets & advanced

### hashmap

A dictionary rendered as a **vertical column of entry nodes**, one row per key→value pair, in insertion order — mirroring `print(d)` in Python or a `LinkedHashMap` iteration.

- **kind** must be `"entry"` to trigger this layout (a payload with no entries falls back to the generic graph layout). **label** is conventionally `"key: value"`. **slot** is unused (`null`).
- **edges** are optional: an entry → ref-target edge places a value object (e.g. a list) below all entries, x-aligned under its referencing entry.
- **cursor** marks `key` / `cur` / `found`; **changed** flashes inserted/updated/looked-up entries; **removed** fades deleted entries.

> Layout: `client/src/d3/hashmap-layout.ts` · widget id `hashmap`

```d3 widget=hashmap
{
  "title": "Python dict: {\"a\": 1, \"b\": 2, \"c\": 3, \"d\": 4}",
  "steps": [
    {
      "nodes": [
        {"id": "k0", "label": "a: 1", "kind": "entry", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "k1", "label": "b: 2", "kind": "entry", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "k2", "label": "c: 3", "kind": "entry", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "k3", "label": "d: 4", "kind": "entry", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [], "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Four-entry dict. Insertion order is iteration order in Python 3.7+.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven hashmap renderer.

### graph-generic

Arbitrary graphs — directed or undirected, cyclic or acyclic. The layout auto-selects a strategy by topology: tree-shaped inputs (in-degree ≤ 1, reachable from a root) use the Reingold-Tilford tidy-tree algorithm; cyclic or multi-parent inputs use a deterministic seeded d3-force simulation.

- Node ids are short labels (`A`, `B`, …); **label** commonly equals the id. **slot** is unused (`null`).
- **edges** carry `label: ""` for unweighted graphs or a numeric string for edge weights (rendered at the edge midpoint).
- **cursor** marks `cur` / `src` / `dst` / `visited`; **highlight** marks visited/active nodes; **changed** flashes just-visited or just-relaxed nodes.

> Layout: `client/src/d3/graph-layout.ts` · widget id `graph-generic`

```d3 widget=graph-generic
{
  "title": "Directed graph with 5 nodes",
  "steps": [
    {
      "nodes": [
        {"id": "A", "label": "A", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "B", "label": "B", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "C", "label": "C", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "D", "label": "D", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "E", "label": "E", "kind": "node", "slot": null, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "A", "to": "B", "label": ""},
        {"from": "A", "to": "C", "label": ""},
        {"from": "B", "to": "D", "label": ""},
        {"from": "C", "to": "D", "label": ""},
        {"from": "D", "to": "E", "label": ""}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Directed graph: A → B → D → E, A → C → D. D has two parents (B and C).",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven graph-generic renderer.

### union-find

A Disjoint Set Union (DSU) drawn as a horizontal row of `n` circle nodes, each labelled with its element index, with parent pointers as curved arcs bulging above the row.

- **kind** is `"node"` for non-roots (a Bézier arc from child to parent, arc height scaling with slot distance, capped at 80 px) or `"root"` for representatives (a small self-loop arc above the circle). **slot** equals the element's integer index.
- **edges**: include a self-edge `{from: "i", to: "i"}` for every root so its self-loop renders; non-roots add a directed child→parent edge with `label: ""`.
- **meta** on a root carries `[{"name": "rank", "value": "N"}]`; **changed** flashes path-compressed nodes.

> Layout: `client/src/d3/union-find-layout.ts` · widget id `union-find`

```d3 widget=union-find
{
  "title": "Union-Find — initial state (n=4)",
  "steps": [
    {
      "nodes": [
        {"id": "0", "label": "0", "kind": "root", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "1", "label": "1", "kind": "root", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "2", "label": "2", "kind": "root", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "3", "label": "3", "kind": "root", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "0", "to": "0", "label": ""},
        {"from": "1", "to": "1", "label": ""},
        {"from": "2", "to": "2", "label": ""},
        {"from": "3", "to": "3", "label": ""}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Initial state: parent = [0, 1, 2, 3]. Each element is its own root.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven union-find renderer.

### bitset

A fixed-width array of bits drawn as a **compact horizontal row** of circle nodes — each circle shows a single bit (`"0"` or `"1"`) at its slot position.

- **kind** is always `"node"`; bit 0 is the LSB (leftmost, slot 0) and bit n−1 the MSB (rightmost). Node ids are the bit position as a string. **edges** are always `[]`.
- **cursor** marks the bit being tested or operated on; **highlight** marks bits being scanned (golden); **changed** flashes bits just set or cleared.
- **meta** can carry an optional `[{"name": "popcount", "value": "4"}]` on a sentinel node.

> Layout: `client/src/d3/bitset-layout.ts` · widget id `bitset`

```d3 widget=bitset
{
  "title": "8-bit bitset: 10110100",
  "steps": [
    {
      "nodes": [
        {"id": "0", "label": "0", "kind": "node", "slot": 0, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "1", "label": "0", "kind": "node", "slot": 1, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "2", "label": "1", "kind": "node", "slot": 2, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "3", "label": "0", "kind": "node", "slot": 3, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "4", "label": "1", "kind": "node", "slot": 4, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "5", "label": "1", "kind": "node", "slot": 5, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "6", "label": "0", "kind": "node", "slot": 6, "meta": [], "cardId": "", "layoutKind": ""},
        {"id": "7", "label": "1", "kind": "node", "slot": 7, "meta": [], "cardId": "", "layoutKind": ""}
      ],
      "edges": [],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Initial state: 10110100₂ (= 180). Set bits at positions 2, 4, 5, 7.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven bitset renderer.

### skiplist

A multi-level skip list drawn as a **grid of circle nodes** — each (level, slot) pair maps to one circle. Level 0 (densest) sits at the bottom; higher levels are express lanes stacked above.

- **slot** sets the column (0-indexed); nodes sharing a key share a slot across levels. The level comes from **meta** `[{"name": "level", "value": "N"}]` (0 = bottom), not from `slot`.
- **kind** is `"node"`; **edges** are horizontal within-level arrows (`label: ""`), rendered as straight lines because same-level nodes share a y coordinate.
- **ID convention**: `"L{level}-{slot}"` when level follows position, or `"L{level}-{key}"` for key-stable ids across insert animations. **cursor**/**highlight** trace the search path; **changed** flashes inserted/found nodes.

> Layout: `client/src/d3/skiplist-layout.ts` · widget id `skiplist`

```d3 widget=skiplist
{
  "title": "Skip list: 3 levels, 6 nodes",
  "steps": [
    {
      "nodes": [
        {"id": "L0-0", "label": "3",  "kind": "node", "slot": 0, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L0-1", "label": "6",  "kind": "node", "slot": 1, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L0-2", "label": "7",  "kind": "node", "slot": 2, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L0-3", "label": "9",  "kind": "node", "slot": 3, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L0-4", "label": "12", "kind": "node", "slot": 4, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L0-5", "label": "17", "kind": "node", "slot": 5, "meta": [{"name": "level", "value": "0"}], "cardId": "", "layoutKind": ""},
        {"id": "L1-0", "label": "3",  "kind": "node", "slot": 0, "meta": [{"name": "level", "value": "1"}], "cardId": "", "layoutKind": ""},
        {"id": "L1-1", "label": "6",  "kind": "node", "slot": 1, "meta": [{"name": "level", "value": "1"}], "cardId": "", "layoutKind": ""},
        {"id": "L1-3", "label": "9",  "kind": "node", "slot": 3, "meta": [{"name": "level", "value": "1"}], "cardId": "", "layoutKind": ""},
        {"id": "L1-5", "label": "17", "kind": "node", "slot": 5, "meta": [{"name": "level", "value": "1"}], "cardId": "", "layoutKind": ""},
        {"id": "L2-0", "label": "3",  "kind": "node", "slot": 0, "meta": [{"name": "level", "value": "2"}], "cardId": "", "layoutKind": ""},
        {"id": "L2-3", "label": "9",  "kind": "node", "slot": 3, "meta": [{"name": "level", "value": "2"}], "cardId": "", "layoutKind": ""}
      ],
      "edges": [
        {"from": "L0-0", "to": "L0-1", "label": ""},
        {"from": "L0-1", "to": "L0-2", "label": ""},
        {"from": "L0-2", "to": "L0-3", "label": ""},
        {"from": "L0-3", "to": "L0-4", "label": ""},
        {"from": "L0-4", "to": "L0-5", "label": ""},
        {"from": "L1-0", "to": "L1-1", "label": ""},
        {"from": "L1-1", "to": "L1-3", "label": ""},
        {"from": "L1-3", "to": "L1-5", "label": ""},
        {"from": "L2-0", "to": "L2-3", "label": ""}
      ],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "Level 0 (bottom) holds all 6 nodes. Level 1 skips over 7 and 12. Level 2 is the sparsest express lane with only 3 and 9.",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

See it live → [Renderer Gallery](/cortex/data-structures-and-algorithms/appendix-widget-catalog-renderer-gallery) — the trace-driven skiplist renderer.
