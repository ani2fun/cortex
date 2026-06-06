# Stack renderer DOM walk

> Slice 1 of 17 (ADR-0024). Verifies that a chapter with `viz-kind=stack`
> renders the bespoke `.stack-renderer` block — top label + reversed-index
> column + colored top cell — instead of the generic SVG circle layout.

Open: <http://localhost:5173/cortex/data-structures-and-algorithms/02-linear-structures/05-stack/08-pattern-reversal/02-problems/01-stack-inversion>

Trigger:

1. Wait for the Visualise button on the chapter's runnable code fence to mount.
2. Click the Visualise button (`button[aria-label="Visualise code"]`).
3. Wait for the modal to open (`.algolens-grid` exists in the DOM).
4. Wait for the trace to finish (`.algolens__status--error` absent, scrubber present).
5. Step forward 3–4 times via the Stepper's "Next" button so `reversed_stack`
   accumulates at least two cells (the loop pushes one cell per pass).

Assertions:

- Modal root present: `document.querySelector(".algolens-grid") !== null`
- Bespoke block present: `document.querySelector(".stack-renderer") !== null`
- Side pointer present + label: `document.querySelector(".stack-renderer__pointer-name")?.textContent === "top"`
- Side pointer has an SVG arrow: `document.querySelector(".stack-renderer__pointer-arrow") !== null`
- Side pointer moves with the top cell — step forward through a push and the
  pointer's inline `top` CSS value should DECREASE (smaller Y = higher on screen);
  step through a pop and it should INCREASE. Lock in with:
  ```js
  const ptr = document.querySelector(".stack-renderer__pointer");
  const yBeforePush = Number(ptr.style.top.replace("px", ""));
  document.querySelector('button[aria-label="Next step"]').click();
  await new Promise(r => setTimeout(r, 400));  // wait for transition
  const yAfterPush = Number(ptr.style.top.replace("px", ""));
  yAfterPush < yBeforePush;  // top rose → pointer slid up
  ```
- Cells column present: `document.querySelector(".stack-renderer__cells") !== null`
- Top cell exists and is the FIRST cell:
  `document.querySelectorAll(".stack-renderer__cell")[0]?.classList.contains("stack-renderer__cell--top") === true`
- Top cell has `--top-color` CSS var set (terracotta `#c8693e` by default):
  `getComputedStyle(document.querySelector(".stack-renderer__cell--top")).getPropertyValue("--top-color").trim().length > 0`
- Renderer block carries `data-card-content` so ArrowLayer aims at the visible
  cells, not the wide centring padding around them. The arrow's end x should
  match the renderer's right edge to within a few pixels:
  ```js
  const arrow = document.querySelector("path.algolens-arrows__path");
  const endX = Number(arrow.getAttribute("d").split(" ").pop().split(",")[0]);
  const host = document.querySelector(".algolens-arrows").getBoundingClientRect();
  const content = document.querySelector("[data-card-content]").getBoundingClientRect();
  Math.abs((endX + host.left) - content.right) < 4;  // → true
  ```
- Reversed-index column is monotonically decreasing top→bottom:
  walk `.stack-renderer__cell-index` text content; numbers should read
  `N-1, N-2, …, 1, 0` from top to bottom.
- Generic graph chrome (`.viz-graph__caption`, `.viz-graph__legend`) still
  renders alongside — the bespoke renderer keeps the title / caption / legend
  for consistency with other modals; only the per-step canvas is bespoke.
- Generic SVG node circles are *gone*:
  `document.querySelectorAll(".viz-graph__circle").length === 0` inside the
  same canvas (the bespoke renderer composes div cells, not SVG circles).

> If any assertion fails, do not silently update this file. Investigate
> whether the renderer regressed or whether the design intentionally moved
> the element; update the matching renderer source AND this file together.
