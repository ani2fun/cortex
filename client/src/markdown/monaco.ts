// Lazy boundary for the Monaco editor.
//
// The editor stack — monaco-editor + @monaco-editor/react + the per-language
// Monarch tokenisers + the editor web worker — is ~3.8 MB. Only runnable code
// blocks ever mount it, yet a static `@JSImport("@markdown/monaco")` from the
// Scala.js facade used to drag that whole chunk into the entry's static import
// graph, so the home page and plain chapters modulepreloaded + downloaded it
// for nothing.
//
// The heavy module now lives in ./monaco-impl; this wrapper hides it behind
// React.lazy(() => import("./monaco-impl")) so Vite emits it as an on-demand
// chunk fetched only when an editor first mounts. The Scala.js facade
// (MonacoEditor.scala) still @JSImports the default export of THIS module
// unchanged — the laziness lives entirely here, no Scala changes required.

import { createElement, lazy, Suspense } from "react";

// monaco-impl's default export is @monaco-editor/react's `Editor`; its top-level
// side-effects (worker registration, theme, loader.config) run on chunk load,
// before the lazy component first renders.
const RealEditor = lazy(() => import("./monaco-impl"));

// Brief fallback for the chunk-fetch window (distinct from @monaco-editor/react's
// own `loading` prop, which covers editor initialisation after the chunk arrives).
export default function MonacoEditor(props: Record<string, unknown>) {
  return createElement(
    Suspense,
    {
      fallback: createElement(
        "div",
        { className: "rcb__editor-loading" },
        "Loading editor…",
      ),
    },
    createElement(RealEditor, props),
  );
}
