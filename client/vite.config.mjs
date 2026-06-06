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
      // `@data` → `client/src/data`. Lets the Scala.js client `@JSImport` the
      // TypeScript bridge and JSON files without brittle relative paths into
      // the linker output directory.
      "@data":     path.resolve(import.meta.dirname, "src/data"),
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
    },
  },

  build: {
    outDir: "dist",
    emptyOutDir: true,
    // Bundle-size hygiene: peel the heavy markdown-pipeline deps into their
    // own chunks so the home-page entry doesn't ship them. Browsers fetch
    // them on demand when the chapter page first lands.
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules")) {
            if (id.includes("/shiki") || id.includes("/@shikijs/")) return "shiki";
            if (id.includes("/mermaid")) return "mermaid";
            if (id.includes("/@terrastruct/d2")) return "d2";
            if (id.includes("/katex")) return "katex";
            if (id.includes("/prismjs")) return "prismjs";
            // Monaco core + the React wrapper + any web-worker entry points
            // ride together. Loaded only when a Cortex chapter mounts a
            // runnable block (Scala.js imports `@markdown/monaco` from inside
            // RunnableCodeBlock's render path).
            if (id.includes("/monaco-editor") || id.includes("/@monaco-editor/")) return "monaco";
          }
          return null;
        },
      },
    },
    // Quiet the warning at 800 KB; the manual-chunked vendor splits above
    // are intentionally large but lazily loaded.
    chunkSizeWarningLimit: 800,
  },
});
