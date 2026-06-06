# <Renderer name> DOM walk

> Replace placeholders below with the renderer's actual chapter URL, CSS
> selectors, and callout text. The format matches every other DOM walk file
> in this directory so a regression in one slice surfaces the same shape of
> failure as a regression in another.

Open: <http://localhost:5173/cortex/data-structures-and-algorithms/02-linear-structures/...>

Trigger:

1. Wait for the Visualise button on the chapter's runnable code fence to mount.
2. Click the Visualise button (`button[aria-label="Visualise code"]`).
3. Wait for the modal to open (`.algolens-grid` exists in the DOM).
4. Step forward 1–2 times via the Stepper's "Next" button if the renderer's
   chrome only appears after the first heap mutation.

Assertions:

- Modal root present: `document.querySelector(".algolens-grid") !== null`
- Bespoke block present: `document.querySelector(".<renderer-name>-renderer") !== null`
- Primary callout text matches: `document.querySelector(".<renderer-name>-renderer__<element>")?.textContent === "<expected text>"`
- Highlighted cell exists: `document.querySelector(".<renderer-name>-renderer__cell--<state>") !== null`
- Generic fallback chrome is *gone*: `document.querySelector(".viz-graph__caption")` still works, but the
  bespoke renderer owns its own card chrome (`.<renderer-name>-renderer__card` or similar)

> If any assertion fails, do not silently update this file. Investigate
> whether the renderer regressed or whether the design intentionally moved
> the element; update the matching renderer source AND this file together.
