// Side-effect import: Tailwind base styles.
import "./tailwind.css";

// Side-effect import: d3-transition patches `selection.prototype.transition`
// and `.interrupt` when its top-level module body runs — without this, any
// `D3.select(...).transition().attr(...)` chain inside the Scala.js widgets
// silently no-ops (the element's `__transition` scheduler slot stays
// undefined). It MUST live here, not in the Scala.js facade: d3's barrel
// re-exports d3-transition via `export *`, but d3-selection has
// `"sideEffects": false` so the bundler tree-shakes the path that would
// touch d3-transition's index.js; and a Scala.js `@JSImport(_, Namespace)`
// reference inside `D3.scala` gets DCE'd by the JS linker because the val
// is private and never read.
//
// The bare `import "d3-transition"` *worked* in dev (Vite respects the
// package's `"sideEffects"` list), but Rollup tree-shook it out of the
// production bundle anyway. The pinned `window.*` assignment below is the
// minimum reference Rollup can't elide — it forces the named import to
// stay live, which keeps the `import` statement itself in the output, which
// runs d3-transition's prototype-patching side-effects on module init.
import { transition as _d3Transition, interrupt as _d3Interrupt } from "d3-transition";
window.__d3TransitionLoaded = { _d3Transition, _d3Interrupt };

// Side-effect import: triggers the Scala.js MainModuleInitializer
// (cortex.client.Main.main), which mounts the React tree.
import "scalajs:main.js";
