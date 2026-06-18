import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import path from "node:path";

export default defineConfig({
  plugins: [
    // Tailwind v4 runs through @tailwindcss/vite. PostCSS is no longer in the loop.
    tailwindcss(),
    scalaJSPlugin({
      // sbt root is the parent dir (this `client/` is a sub-project).
      cwd: "..",
      // sbt module name — must match `lazy val client` in build.sbt.
      projectID: "client",
    }),
  ],

  resolve: {
    alias: {
      // `@markdown` → `client/src/markdown`. Same idea, for the Prism +
      // markdown-pipeline TS helpers Scala.js calls into.
      "@markdown": path.resolve(import.meta.dirname, "src/markdown"),
      // `@d3` → `client/src/d3`. The pure-TypeScript + D3 engine-driven widget
      // renderers the Scala.js bridge `@JSImport`s. See ADR-0017.
      "@d3":       path.resolve(import.meta.dirname, "src/d3"),
    },
  },

  server: {
    port: 5173,
    proxy: {
      // Forward API calls to the ZIO backend during development.
      "/api": "http://localhost:8080",
      "/docs": "http://localhost:8080",
      // /c4/* is reverse-proxied by the ZIO server to the LikeC4 SPA
      // container (see LikeC4ProxyRoutes + bin/dev's LIKEC4_URL export).
      "/c4": "http://localhost:8080",
      // /tutor/* is reverse-proxied by the ZIO server to cortex-tutor
      // (see TutorProxyRoutes; set CORTEX_TUTOR_BASE_URL for the backend).
      "/tutor": "http://localhost:8080",
    },
  },

  build: {
    outDir: "dist",
    emptyOutDir: true,
    // NO manualChunks. The heavy libs (monaco, shiki, mermaid, katex, d2) are all
    // reached only through dynamic import() — monaco via the React.lazy boundary in
    // src/markdown/monaco.ts, the rest via render.ts / runtime.ts — so Rollup already
    // emits each as its own on-demand chunk. Forcing them into *named* manualChunks made
    // Rollup co-locate shared runtime helpers (notably Vite's own `__vitePreload`) inside
    // those vendor chunks; the entry then STATICALLY imported the chunks just to reach
    // those helpers, dragging ~9 MB of editor/diagram/highlighter code onto the landing
    // page's critical path. Letting Vite chunk automatically keeps shared helpers in a
    // small shared chunk and leaves the heavy libs genuinely lazy. (Verified: the landing
    // page then fetches only the entry + a tiny prism chunk.)
    chunkSizeWarningLimit: 800,
  },
});
