// Heavy Monaco implementation — loaded lazily by ./monaco (React.lazy).
//
// This module pulls in the full editor stack: monaco-editor, @monaco-editor/react,
// the per-language Monarch tokenisers, and the editor web worker (~3.8 MB total).
// It is imported only via the dynamic `import("./monaco-impl")` in ./monaco, so Vite
// emits it as an on-demand chunk that the browser fetches the first time a runnable
// code block actually mounts an editor — never on the home page or plain chapters.
//
// On first import the side-effects below wire up:
//
//   1. The editor web worker (Vite `?worker` URL import).
//   2. Language tokeniser contributions for exactly the languages we support
//      server-side (see Languages.scala). No language *workers* — we don't
//      want IntelliSense / type-checking; the editor is a runner, not an IDE.
//   3. A custom theme that blends with the surrounding `.rcb__editor` card
//      (background #2d2d2d).
//   4. `loader.config({ monaco })` so @monaco-editor/react resolves to the
//      npm-bundled monaco-editor instead of fetching from a CDN at runtime.

// @ts-expect-error — Vite's ?worker query is resolved at build time.
import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";

import * as monaco from "monaco-editor";
import { Editor, loader } from "@monaco-editor/react";

// Side-effect language imports. Each pulls in the Monarch tokeniser for that
// language. cpp covers both C and C++.
import "monaco-editor/esm/vs/basic-languages/python/python.contribution";
import "monaco-editor/esm/vs/basic-languages/java/java.contribution";
import "monaco-editor/esm/vs/basic-languages/scala/scala.contribution";
import "monaco-editor/esm/vs/basic-languages/cpp/cpp.contribution";
import "monaco-editor/esm/vs/basic-languages/go/go.contribution";
import "monaco-editor/esm/vs/basic-languages/rust/rust.contribution";
import "monaco-editor/esm/vs/basic-languages/kotlin/kotlin.contribution";
import "monaco-editor/esm/vs/basic-languages/typescript/typescript.contribution";
import "monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution";

// Monaco asks the host page how to spawn workers. With only `editor.worker`
// imported above, we can ignore the requested label and return the editor
// worker for every request.
(self as unknown as { MonacoEnvironment: { getWorker: (id: string, label: string) => Worker } })
  .MonacoEnvironment = {
  getWorker: () => new EditorWorker(),
};

// Custom dark theme that matches the existing `.rcb__editor` (#2d2d2d) card
// surround so the editor doesn't look like a transplanted VS Code window. The
// token rules mirror the static-fence palette (indigo keyword, teal function/type,
// green string, amber number, muted-italic comment) so runnable + rendered code match.
monaco.editor.defineTheme("cortex-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [
    { token: "keyword", foreground: "7E88E6" },
    { token: "type", foreground: "54B3A1" },
    { token: "function", foreground: "54B3A1" },
    { token: "string", foreground: "84B87D" },
    { token: "number", foreground: "D59A55" },
    { token: "regexp", foreground: "84B87D" },
    { token: "comment", foreground: "8A857C", fontStyle: "italic" },
  ],
  colors: {
    "editor.background": "#2d2d2d",
    "editor.lineHighlightBackground": "#3a3a3a40",
    "editorGutter.background": "#2d2d2d",
  },
});

// Pin @monaco-editor/react to the npm-bundled monaco we just configured.
// Without this it would attempt to load monaco from a CDN at runtime.
loader.config({ monaco });

export default Editor;
