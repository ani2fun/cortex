# Renderer DOM-walk smoke tests (ADR-0024)

This directory holds one Markdown file per bespoke renderer, each describing a
browser DOM-walk smoke test for the renderer's chrome. The tests are run by
Claude via the `mcp__Claude_Preview__preview_eval` workflow against a live
`bin/dev` server: open the chapter, click **Visualise**, run each assertion
via `document.querySelector` + `.textContent` / `.classList` checks.

The format is deliberately plain Markdown — every file is *also* readable
documentation of what the renderer should look like in the DOM, which makes
reviewing a bespoke renderer's contract a one-file affair.

## File anatomy

```markdown
# <Renderer name> DOM walk

Open: <chapter URL>
Trigger: <how to surface the renderer>

Assertions:
- <CSS-selector or DOM expectation, one per line>
- …
```

Each `Assertions:` bullet is a single observable claim. Keep them small and
precise so a regression points at exactly one thing — e.g.
"`.stack-renderer__cell--top` exists" rather than "the stack looks right".

## Adding a new test

Per the renderer slice template in
`~/.claude/plans/before-we-continue-to-concurrent-sun.md`:

1. Copy `_template.dom.md` to `<renderer-name>.dom.md` (e.g.
   `stack.dom.md` for slice 1).
2. Fill in the chapter URL, the trigger steps, and the assertion list.
3. The slice's verification step runs the DOM walk via MCP `preview_eval`
   on a `bin/dev` server.

## Why this lives outside the Scala test tree

These tests need a running Vite dev server + Monaco + the Visualise modal,
none of which the JVM test suite owns. The Scala-side coverage is
`shared/src/test/scala/codefolio/shared/viz/RendererSpec.scala` (the JSON
contract — `structureType` shape, inference firing); the DOM walk covers
"the bespoke chrome actually renders".
