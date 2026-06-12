// Tiny dynamic-import gateway for the markdown pipeline.
//
// `render.ts` pulls in unified + ~25 remark/rehype plugins + shiki + d2 +
// katex + mermaid — together a few MB of JS + WASM. Most pages of the SPA
// (home, Cortex index, demo) never need any of it. We hide the static
// `@JSImport("@markdown/render", "renderChapter")` from Scala.js behind a
// dynamic `import("./render")` so the whole pipeline lands in its own
// chunk that browsers only fetch when a chapter page mounts.
//
// This module itself is tiny — safe to keep eagerly loaded.

import type { RenderOptions, RenderResult } from "./render";

type RenderChapterFn = (
  source: string,
  opts?: RenderOptions,
) => Promise<RenderResult>;

let cachedRender: Promise<RenderChapterFn> | null = null;

/** Lazily load the markdown render module on first call. The returned
 *  function is cached so subsequent chapter renders skip the chunk fetch.
 */
export function loadRenderChapter(): Promise<RenderChapterFn> {
  if (!cachedRender) {
    cachedRender = import("./render").then((m) => m.renderChapter);
  }
  return cachedRender;
}
