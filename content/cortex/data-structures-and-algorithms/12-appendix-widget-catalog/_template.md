<!--
TEMPLATE — copy this file as `<NN>-<layout-key>.md` when adding a new layout
chapter. NN is the next free numeric prefix (see existing chapters). Delete
this comment after copying.
-->
---
title: <layout-key>
summary: VizGraph <layout-key> layout — <one-sentence description>. Use d3 widget=<layout-key> for <use-case>.
prereqs: []
---

# `<layout-key>`

## Purpose

<Describe what topology the layout renders, how nodes and edges are interpreted, and what visual features it supports.>

- **<Feature 1>** — <description>
- **<Feature 2>** — <description>
- **Cursors** — <cursor names and their semantics>
- **Highlight** — <what highlighting means>
- **Changed** — <what changed means (green flash)>

> **TS module**: `client/src/d3/<layout-file>.ts`
>
> **Scala dispatch**: `"<layout-key>"` (and any aliases) route to `<layoutFn>` in `client/src/d3/index.ts`.
>
> **Geometry**: <key constants from the TS module, e.g. NODE_R, spacing constants, canvas sizing>.

## VizGraph schema (reference card)

Inline `d3 widget=<layout-key>` fences carry a raw `VizGraph` object. Minimal schema:

```ts
{
  title: string,
  steps: [{
    nodes: [{
      id:         string,
      label:      string,
      kind:       "node",   // or "entry" / "terminal" for special kinds
      slot:       null,     // or number if the layout uses slot for position
      meta:       [],
      cardId:     "",
      layoutKind: ""
    }],
    edges: [{
      from:  string,
      to:    string,
      label: string
    }],
    cursor:     [],
    highlight:  [],
    changed:    [],
    removed:    [],
    annotation: string,
    line:       0,
    frames:     [],
    cardCursor: []
  }]
}
```

<Any important schema conventions — id naming, slot semantics, edge label conventions.>

## Representative payloads

### Payload 1 — <description>

```d3 widget=<layout-key>
{
  "title": "<title>",
  "steps": [
    {
      "nodes": [],
      "edges": [],
      "cursor": [], "highlight": [], "changed": [], "removed": [],
      "annotation": "<annotation>",
      "line": 0, "frames": [], "cardCursor": []
    }
  ]
}
```

### Payload 2 — <description>

<Add 2-4 representative payloads following the same pattern.>

## Browser verification

Open this chapter at `http://localhost:5173/cortex/...` and:

1. Step through each payload (Prev / Next / Play / Pause / Reset).
2. <Specific visual checks for each payload.>
3. Confirm no `.d3-widget__error` divs and no console exceptions.
