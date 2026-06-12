// Markdown rendering pipeline.
//
// Port of `portfolio-app/src/lib/knowledgeMarkdown.tsx`. The custom remark
// plugins (D2 pre-pass, runnable-group merging, image unwrapping) and the
// custom remark-rehype code handler are preserved verbatim. The output
// difference: instead of running through `toJsxRuntime` to produce React
// nodes, this module emits an HTML string + an extracted table of contents.
// The Scala.js client injects the HTML via `dangerouslySetInnerHTML` and
// then walks the article DOM to React-portal-mount Scala.js components into
// `<div class="runnable-code|runnable-group|mermaid-block|d2-diagram">` stubs.
//
// Heavy renderers (D2 WASM engine, mermaid) are dynamic-imported so they
// only land in the chunk that loads this module — keeping the home page
// bundle small.

import { unified, type Plugin } from "unified";
import remarkParse from "remark-parse";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import remarkRehype from "remark-rehype";
import rehypeSlug from "rehype-slug";
import rehypeAutolinkHeadings from "rehype-autolink-headings";
import rehypeKatex from "rehype-katex";
import rehypePrettyCode from "rehype-pretty-code";
import { createCssVariablesTheme } from "shiki";
import rehypeStringify from "rehype-stringify";
import { visit } from "unist-util-visit";
import { fromHtml } from "hast-util-from-html";
import { defaultHandlers } from "mdast-util-to-hast";

import type { Root, Code, Html, Image, Paragraph } from "mdast";
import type { Element, ElementContent, Root as HastRoot, Text } from "hast";
import type { State } from "mdast-util-to-hast";

// Same alias map as `client/src/markdown/runtime.ts` (Prism) and
// `server/.../runner/Languages.scala`. Local copy avoids importing prism
// just for the alias resolution.
interface LanguageInfo {
  id: number;
  label: string;
  aliases: string[];
  // false for display-only tab languages (e.g. pseudocode) — they join a tab
  // group without needing a `run` meta marker and the client suppresses Run
  // controls for them.
  runnable: boolean;
}

// Each label leads with an emoji icon (mascot / brand colour) so tab strips
// and in-block headers are scannable at a glance. The icon is part of the
// label string — propagates to both `RunnableTabNode.languageLabel` and the
// `data-language-label` attribute on lone runnable-code placeholders.
const RUNNABLE_LANGUAGES: LanguageInfo[] = [
  {
    id: 0,
    label: "🧠 Pseudocode",
    aliases: ["pseudocode", "pseudo"],
    runnable: false,
  },
  {
    id: 71,
    label: "🐍 Python 3.8",
    aliases: ["python", "py", "python3"],
    runnable: true,
  },
  { id: 62, label: "☕ Java 13 (OpenJDK)", aliases: ["java"], runnable: true },
  // Scala — no emoji; the client renders the real Scala wave-mark next to the
  // label via BrandIcons.Scala (no native emoji evokes the brand cleanly).
  { id: 81, label: "Scala 3", aliases: ["scala"], runnable: true },
  { id: 50, label: "🔧 C (GCC 9.2)", aliases: ["c"], runnable: true },
  {
    id: 54,
    label: "➕ C++ (GCC 9.2)",
    aliases: ["cpp", "c++", "cxx"],
    runnable: true,
  },
  { id: 60, label: "🐹 Go 1.13", aliases: ["go", "golang"], runnable: true },
  { id: 73, label: "🦀 Rust 1.40", aliases: ["rust", "rs"], runnable: true },
  { id: 78, label: "💜 Kotlin 1.9", aliases: ["kotlin", "kt"], runnable: true },
  {
    id: 74,
    label: "🔷 TypeScript 3.7",
    aliases: ["typescript", "ts"],
    runnable: true,
  },
  {
    id: 63,
    label: "🟨 JavaScript (Node.js 12)",
    aliases: ["javascript", "js", "node"],
    runnable: true,
  },
  {
    id: 82,
    label: "🗄️ SQL (SQLite 3.27)",
    aliases: ["sql", "sqlite"],
    runnable: true,
  },
];

const aliasIndex = new Map<string, LanguageInfo>();
for (const lang of RUNNABLE_LANGUAGES) {
  for (const a of lang.aliases) aliasIndex.set(a.toLowerCase(), lang);
}

const resolveLanguage = (
  lang: string | null | undefined,
): LanguageInfo | null =>
  lang ? (aliasIndex.get(lang.toLowerCase()) ?? null) : null;

// ---- D2 array traversal slideshow expansion -----------------------------
//
// Markdown authors can write one compact marker instead of duplicating the
// same array diagram across every traversal frame:
//
//   <div class="d2-array-traversal"
//        data-caption="..."
//        data-rows="2"
//        data-cols="3"
//        data-values="value1, value2, value3, value4, value5, value6"></div>
//
// This expands to a normal `.d2-slides` marker plus generated D2 code nodes.

const isD2ArrayTraversalMarker = (node: unknown): node is Html =>
  !!node &&
  typeof node === "object" &&
  (node as { type?: unknown }).type === "html" &&
  /<div\b[^>]*class=(["'])[^"']*\bd2-array-traversal\b[^"']*\1/i.test(
    (node as Html).value,
  );

const parsePositiveInt = (raw: string | null, fallback: number): number => {
  const n = raw ? parseInt(raw, 10) : Number.NaN;
  return Number.isFinite(n) && n > 0 ? n : fallback;
};

const d2MdText = (s: string): string =>
  s.replace(/\|/g, "\\|").replace(/`/g, "\\`");

interface ArrayTraversalConfig {
  caption: string | null;
  rows: number;
  cols: number;
  values: string[];
}

const parseArrayTraversalConfig = (marker: Html): ArrayTraversalConfig => {
  const rows = parsePositiveInt(htmlAttr(marker.value, "data-rows"), 2);
  const cols = parsePositiveInt(htmlAttr(marker.value, "data-cols"), 3);
  const total = rows * cols;
  const rawValues = htmlAttr(marker.value, "data-values") ?? "";
  const values = rawValues
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

  return {
    caption: htmlAttr(marker.value, "data-caption"),
    rows,
    cols,
    values:
      values.length === total
        ? values
        : Array.from({ length: total }, (_, i) => `value${i + 1}`),
  };
};

const cellLabel = (index: number, cols: number): string => {
  const row = Math.floor(index / cols);
  const col = index % cols;
  return `[${row},${col}]`;
};

const cellStyle = (
  index: number,
  active: number | null,
  done: boolean,
): string => {
  if (done || (active !== null && index < active)) {
    return ' {style.fill: "#dcfce7"; style.stroke: "#16a34a"}';
  }
  if (active === index) {
    return ' {style.fill: "#fde68a"; style.stroke: "#d97706"}';
  }
  return "";
};

const arrayD2 = (
  title: string,
  cfg: ArrayTraversalConfig,
  active: number | null,
  done = false,
): string => {
  const cells = cfg.values
    .map((value, i) => {
      const id = `cell${i}`;
      return [
        `  ${id}: |md`,
        `    \`${cellLabel(i, cfg.cols)}\` ${d2MdText(value)}`,
        `  |${cellStyle(i, active, done)}`,
      ].join("\n");
    })
    .join("\n");

  return [
    `arr: "${title}" {`,
    `  grid-rows: ${cfg.rows}`,
    "  grid-gap: 0",
    cells,
    "}",
  ].join("\n");
};

const stateD2 = (
  cfg: ArrayTraversalConfig,
  active: number,
  done = false,
): string => {
  const row = Math.floor(active / cfg.cols);
  const col = active % cfg.cols;
  const reset = row > 0 && col === 0 && !done ? "\n\n  inner loop reset" : "";
  const doneLine = done ? "\n\n  ✓ done" : "";
  const title = done
    ? `Traversal complete — visited all ${cfg.values.length} cells`
    : `Visit arr[${row}][${col}]`;

  return [
    "direction: right",
    "",
    "state: |md",
    `  **row** = ${row}`,
    "",
    `  **column** = ${col}${reset}${doneLine}`,
    "|",
    "",
    arrayD2(title, cfg, done ? null : active, done),
  ].join("\n");
};

const d2ArrayTraversalSlides = (marker: Html): Array<Html | Code> => {
  const cfg = parseArrayTraversalConfig(marker);
  const captionAttr = cfg.caption
    ? ` data-caption="${escapeHtmlAttr(cfg.caption)}"`
    : "";
  const slideMarker: Html = {
    type: "html",
    value: `<div class="d2-slides"${captionAttr}>`,
  };
  const ready: Code = {
    type: "code",
    lang: "d2",
    meta: null,
    value: arrayD2(
      `Ready to traverse the ${cfg.rows} × ${cfg.cols} array`,
      cfg,
      null,
    ),
  };
  const visits: Code[] = cfg.values.map((_, i) => ({
    type: "code",
    lang: "d2",
    meta: null,
    value: stateD2(cfg, i),
  }));
  const complete: Code = {
    type: "code",
    lang: "d2",
    meta: null,
    value: stateD2(cfg, cfg.values.length - 1, true),
  };

  return [slideMarker, ready, ...visits, complete];
};

const remarkExpandD2ArrayTraversal: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;

    const out: unknown[] = [];
    for (const node of parent.children) {
      if (isD2ArrayTraversalMarker(node))
        out.push(...d2ArrayTraversalSlides(node));
      else out.push(node);
    }

    parent.children = out;
    for (const child of parent.children) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };

  walk(tree as { children?: unknown[] });
};

// ---- D2 pre-pass --------------------------------------------------------
//
// Renders every ```d2 fence to SVG via the WASM engine and stashes the
// result on node.data so the custom code handler can pick it up. Done in
// remark land because the D2 API is async and remark-rehype handlers must
// be sync.

const remarkRenderD2: Plugin<[], Root> = () => async (tree) => {
  const targets: Code[] = [];
  visit(tree, "code", (node: Code) => {
    if (node.lang === "d2") targets.push(node);
  });
  if (targets.length === 0) return;

  // Dynamic import — D2 ships a multi-MB WASM blob. Only load it when at
  // least one chapter actually has a d2 fence.
  const { D2 } = await import("@terrastruct/d2");
  const d2 = new D2();
  for (const node of targets) {
    const data = (node.data ??= {}) as Record<string, unknown>;
    try {
      const result = await d2.compile(node.value);
      const svg = await d2.render(result.diagram, result.renderOptions);
      data.d2Svg = svg;
    } catch (err) {
      data.d2Error = err instanceof Error ? err.message : "D2 render failed";
    }
  }
};

const isD2SlidesMarker = (node: unknown): node is Html =>
  !!node &&
  typeof node === "object" &&
  (node as { type?: unknown }).type === "html" &&
  /<div\b[^>]*class=(["'])[^"']*\bd2-slides\b[^"']*\1/i.test(
    (node as Html).value,
  );

const isClosingDiv = (node: unknown): node is Html =>
  !!node &&
  typeof node === "object" &&
  (node as { type?: unknown }).type === "html" &&
  /^<\/div>\s*$/i.test((node as Html).value.trim());

const htmlAttr = (html: string, name: string): string | null => {
  const re = new RegExp(`\\b${name}\\s*=\\s*(["'])(.*?)\\1`, "i");
  return html.match(re)?.[2] ?? null;
};

// Pull a `key=value` (quoted or bare) out of a fence's info-string `meta`.
// Used by ```d3 widget=array-traversal — the widget name is the only required
// key today, but the same parser will handle future per-instance options.
const parseMetaKv = (meta: string, key: string): string | null => {
  const quoted = new RegExp(`\\b${key}\\s*=\\s*(["'])(.*?)\\1`).exec(meta);
  if (quoted) return quoted[2];
  const bare = new RegExp(`\\b${key}\\s*=\\s*([^\\s"']+)`).exec(meta);
  return bare ? bare[1] : null;
};

const escapeHtmlAttr = (s: string): string =>
  s
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

const escapeHtmlText = (s: string): string =>
  s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

const d2SlideHtml = (node: Code): string => {
  const data = node.data as { d2Svg?: string; d2Error?: string } | undefined;
  if (data?.d2Svg) return data.d2Svg;
  if (data?.d2Error) {
    return [
      '<div class="d2-error">',
      '<p class="d2-error-title">D2 render error</p>',
      `<pre>${escapeHtmlText(data.d2Error)}</pre>`,
      "</div>",
    ].join("");
  }
  return "";
};

const buildD2SlidesHtml = (marker: Html, slides: Code[]): string => {
  const caption = htmlAttr(marker.value, "data-caption");
  const captionAttr = caption
    ? ` data-caption="${escapeHtmlAttr(caption)}"`
    : "";
  const body = slides
    .map((slide, i) => {
      const html = d2SlideHtml(slide);
      return `<div class="d2-slide" data-slide-index="${i}">${html}</div>`;
    })
    .join("");
  return `<div class="d2-slides not-prose"${captionAttr}>${body}</div>`;
};

// Groups:
//   <div class="d2-slides" ...>
//
//   ```d2
//   ...
//   ```
//   ```d2
//   ...
//   ```
//
// into one slideshow placeholder. The imported source currently lacks the
// closing </div> because the legacy import script stripped standalone closing
// divs, so this pass intentionally stops at the first non-D2 node too.
const remarkGroupD2Slides: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;
    const out: unknown[] = [];
    let i = 0;

    while (i < parent.children.length) {
      const node = parent.children[i];

      if (isD2SlidesMarker(node)) {
        const slides: Code[] = [];
        let j = i + 1;

        while (j < parent.children.length) {
          const child = parent.children[j] as Code | Html;
          if (child.type === "code" && child.lang === "d2") {
            slides.push(child);
            j++;
          } else {
            break;
          }
        }

        if (slides.length > 0) {
          const next = parent.children[j];
          out.push({
            type: "html",
            value: buildD2SlidesHtml(node, slides),
          } satisfies Html);
          i = isClosingDiv(next) ? j + 1 : j;
          continue;
        }
      }

      out.push(node);
      i++;
    }

    parent.children = out;
    for (const child of parent.children) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };

  walk(tree as { children?: unknown[] });
};

// ---- Runnable-group merge -----------------------------------------------
//
// Adjacent ```<lang> run``` fences merge into a single tabbed group. Walks
// each parent and replaces consecutive runnable code siblings with one
// merged node carrying the full tab list on data.runnableTabs. The custom
// code handler emits a `runnable-group` placeholder for the merged node.

interface RunnableTabNode {
  language: string;
  languageLabel: string;
  source: string;
  runnable: boolean;
  // `viz=<layout>` / `viz-root=<var>` / `viz-case=<n>` from the fence info string
  // — set only on a Python tab, opting it into the trace-driven "Visualise" button
  // (ADR-0018).
  viz?: string;
  vizRoot?: string;
  vizCase?: string;
  // ADR-0024 — `viz-kind=<name>` is the bespoke-renderer dispatch axis: it picks
  // *which* chrome wraps the diagram (`stack`, `queue`, `heap`, `trie`, …) on top
  // of whatever layout `viz=` selected. When omitted, HeapToGraph infers from the
  // root object's shape + locals; explicit hints win.
  vizKind?: string;
}

// True when a code fence should join a tab group. Runnable languages still
// require the `run` marker (today's behaviour); display-only languages like
// pseudocode opt in by language alone, so chapter authors don't have to write
// ` ```pseudocode run` everywhere.
const isRunnableCode = (node: {
  type: string;
  lang?: string | null;
  meta?: string | null;
}) => {
  if (node.type !== "code") return false;
  const lang = resolveLanguage(node.lang ?? null);
  if (!lang) return false;
  if (!lang.runnable) return true;
  const meta = typeof node.meta === "string" ? node.meta : "";
  return /\brun\b/.test(meta);
};

const remarkGroupRunnable: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;
    const out: unknown[] = [];
    let i = 0;
    while (i < parent.children.length) {
      const node = parent.children[i] as Code & {
        data?: Record<string, unknown>;
      };
      if (isRunnableCode(node)) {
        let j = i;
        const tabs: RunnableTabNode[] = [];
        while (j < parent.children.length) {
          const sibling = parent.children[j] as Code;
          if (!isRunnableCode(sibling)) break;
          const lang = resolveLanguage(sibling.lang ?? null)!;
          const sLang = sibling.lang ?? "";
          const sMeta = typeof sibling.meta === "string" ? sibling.meta : "";
          // `viz=` is honoured on Python AND Java fences — both have a working tracer
          // (PythonTracer.wrap / JvmTracer.wrap). Kotlin / Scala fences still parse the
          // attribute but the modal's `wrapForTrace` routes them through the Java
          // harness; chapters wanting that need an explicit decision.
          const canViz = /^(python|java)/i.test(sLang);
          tabs.push({
            language: sLang,
            languageLabel: lang.label,
            source: sibling.value,
            runnable: lang.runnable,
            viz: canViz ? (parseMetaKv(sMeta, "viz") ?? undefined) : undefined,
            vizRoot: canViz ? (parseMetaKv(sMeta, "viz-root") ?? undefined) : undefined,
            vizCase: canViz ? (parseMetaKv(sMeta, "viz-case") ?? undefined) : undefined,
            vizKind: canViz ? (parseMetaKv(sMeta, "viz-kind") ?? undefined) : undefined,
          });
          j++;
        }
        if (tabs.length > 1) {
          const first = parent.children[i] as Code & {
            data?: Record<string, unknown>;
          };
          first.data = { ...(first.data ?? {}), runnableTabs: tabs };
          out.push(first);
          i = j;
          continue;
        }
      }
      out.push(parent.children[i]);
      i++;
    }
    parent.children = out;
    for (const child of parent.children) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };
  walk(tree as { children?: unknown[] });
};

// ---- Trace-group merge --------------------------------------------------
//
// Adjacent ```kotlin trace / ```scala trace fences that immediately follow a
// ```java trace fence are absorbed into the java node as `traceCompanions`
// and removed from the tree. The codeHandler then encodes the companions into
// `data-companions` on the `traced-code-block` placeholder so `TracedCodeBlock`
// can render language-switching tabs with a `LanguageLockBanner` for the
// source-only languages (Kotlin, Scala).

interface TraceCompanionNode {
  language: string;
  source: string;
}

const isJavaTrace = (node: {
  type: string;
  lang?: string | null;
  meta?: string | null;
}) =>
  node.type === "code" &&
  /^java$/i.test(node.lang ?? "") &&
  /\btrace\b/.test(typeof node.meta === "string" ? node.meta : "");

const isCompanionTrace = (node: {
  type: string;
  lang?: string | null;
  meta?: string | null;
}) =>
  node.type === "code" &&
  (/^kotlin$/i.test(node.lang ?? "") || /^scala$/i.test(node.lang ?? "")) &&
  /\btrace\b/.test(typeof node.meta === "string" ? node.meta : "");

const remarkGroupTrace: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;
    const out: unknown[] = [];
    let i = 0;
    while (i < parent.children.length) {
      const node = parent.children[i] as Code & {
        data?: Record<string, unknown>;
      };
      if (isJavaTrace(node)) {
        const companions: TraceCompanionNode[] = [];
        let j = i + 1;
        while (j < parent.children.length) {
          const sibling = parent.children[j] as Code;
          if (!isCompanionTrace(sibling)) break;
          companions.push({ language: sibling.lang ?? "", source: sibling.value });
          j++;
        }
        if (companions.length > 0) {
          node.data = { ...(node.data ?? {}), traceCompanions: companions };
          out.push(node);
          i = j; // skip the absorbed companion nodes
          continue;
        }
      }
      out.push(parent.children[i]);
      i++;
    }
    parent.children = out;
    for (const child of parent.children) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };
  walk(tree as { children?: unknown[] });
};

// ---- Test-case absorption -------------------------------------------------
//
// A ```testcases fence (JSON) immediately after a runnable fence — or after a
// group already merged by remarkGroupRunnable — upgrades that block to the
// stacked "workbench" editor (Code over a schema-driven Test-cases console).
// The spec is stashed on the runnable node's data and the fence is spliced
// out; the code handler then emits a `workbench-inline` placeholder instead
// of `runnable-code` / `runnable-group`.
//
// Failure stays local (ADR-0001): a malformed spec leaves the runnable block
// rendering exactly as today and turns the fence into a visible error card.

interface TestSpecArg {
  id: string;
  label: string;
  type: string;
  placeholder?: string;
}

interface TestSpecCase {
  args: Record<string, string>;
  expected?: string;
}

interface TestSpecNode {
  args: TestSpecArg[];
  cases: TestSpecCase[];
}

const isTestcasesFence = (node: unknown): node is Code =>
  !!node &&
  typeof node === "object" &&
  (node as Code).type === "code" &&
  (node as Code).lang === "testcases";

/** Parse + validate a testcases fence body. Returns the spec or a reason string. */
const parseTestSpec = (raw: string): TestSpecNode | string => {
  let json: unknown;
  try {
    json = JSON.parse(raw);
  } catch (err) {
    return `not valid JSON: ${err instanceof Error ? err.message : String(err)}`;
  }
  const obj = json as { args?: unknown; cases?: unknown };
  if (!Array.isArray(obj.args) || obj.args.length === 0)
    return "`args` must be a non-empty array";
  const args: TestSpecArg[] = [];
  for (const a of obj.args as Array<Record<string, unknown>>) {
    if (!a || typeof a.id !== "string" || a.id === "")
      return "every arg needs a string `id`";
    args.push({
      id: a.id,
      label: typeof a.label === "string" && a.label !== "" ? a.label : a.id,
      type: typeof a.type === "string" ? a.type : "",
      placeholder:
        typeof a.placeholder === "string" ? a.placeholder : undefined,
    });
  }
  const ids = new Set(args.map((a) => a.id));
  if (ids.size !== args.length) return "duplicate arg ids";
  if (!Array.isArray(obj.cases) || obj.cases.length === 0)
    return "`cases` must be a non-empty array";
  const cases: TestSpecCase[] = [];
  for (const c of obj.cases as Array<Record<string, unknown>>) {
    const argsIn = (c?.args ?? {}) as Record<string, unknown>;
    if (typeof argsIn !== "object" || Array.isArray(argsIn))
      return "every case needs an `args` object";
    const vals: Record<string, string> = {};
    for (const [k, v] of Object.entries(argsIn)) {
      if (!ids.has(k)) return `case arg \`${k}\` is not a declared arg id`;
      vals[k] =
        typeof v === "string"
          ? v
          : typeof v === "number" || typeof v === "boolean"
            ? String(v)
            : "";
    }
    cases.push({
      args: vals,
      expected: typeof c?.expected === "string" ? c.expected : undefined,
    });
  }
  return { args, cases };
};

const remarkAbsorbTestcases: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;
    const kids = parent.children;
    let i = 0;
    while (i < kids.length) {
      const node = kids[i] as Code & { data?: Record<string, unknown> };
      const next = kids[i + 1];
      if (isTestcasesFence(next)) {
        const tabs = node?.data?.runnableTabs as
          | RunnableTabNode[]
          | undefined;
        const anchorRunnable =
          (tabs?.some((t) => t.runnable) ?? false) ||
          (isRunnableCode(node) &&
            (resolveLanguage(node.lang ?? null)?.runnable ?? false));
        if (anchorRunnable) {
          const spec = parseTestSpec((next as Code).value);
          if (typeof spec === "string") {
            const errNode = next as Code & { data?: Record<string, unknown> };
            errNode.data = { ...(errNode.data ?? {}), testcasesError: spec };
            i += 2;
            continue;
          }
          node.data = { ...(node.data ?? {}), testcasesSpec: spec };
          kids.splice(i + 1, 1);
          i++;
          continue;
        }
      }
      i++;
    }
    for (const child of kids) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };
  walk(tree as { children?: unknown[] });
};

// ---- Solution-viewer merge ------------------------------------------------
//
// ```python solution time=O(n) space=O(1)``` fences (and adjacent siblings in
// other languages) become a read-only solution viewer — language tabs + lock
// badge + complexity chips — used inside problem editorials. Even a lone
// fence is routed to the viewer; `solution` in the info string IS the opt-in.

interface SolutionTabNode {
  language: string;
  languageLabel: string;
  source: string;
}

const isSolutionFence = (node: unknown): node is Code => {
  if (!node || typeof node !== "object") return false;
  const c = node as Code;
  if (c.type !== "code" || !resolveLanguage(c.lang ?? null)) return false;
  return /\bsolution\b/.test(typeof c.meta === "string" ? c.meta : "");
};

const remarkGroupSolution: Plugin<[], Root> = () => (tree) => {
  const walk = (parent: { children?: unknown[] } | null) => {
    if (!parent || !Array.isArray(parent.children)) return;
    const out: unknown[] = [];
    let i = 0;
    while (i < parent.children.length) {
      const node = parent.children[i] as Code & {
        data?: Record<string, unknown>;
      };
      if (isSolutionFence(node)) {
        const tabs: SolutionTabNode[] = [];
        let time: string | null = null;
        let space: string | null = null;
        let j = i;
        while (j < parent.children.length) {
          const sibling = parent.children[j] as Code;
          if (!isSolutionFence(sibling)) break;
          const lang = resolveLanguage(sibling.lang ?? null)!;
          const sMeta = typeof sibling.meta === "string" ? sibling.meta : "";
          tabs.push({
            language: sibling.lang ?? "",
            languageLabel: lang.label,
            source: sibling.value,
          });
          time = time ?? parseMetaKv(sMeta, "time");
          space = space ?? parseMetaKv(sMeta, "space");
          j++;
        }
        node.data = {
          ...(node.data ?? {}),
          solutionTabs: tabs,
          solutionTime: time ?? undefined,
          solutionSpace: space ?? undefined,
        };
        out.push(node);
        i = j;
        continue;
      }
      out.push(parent.children[i]);
      i++;
    }
    parent.children = out;
    for (const child of parent.children) {
      if (
        child &&
        typeof child === "object" &&
        "children" in (child as object)
      ) {
        walk(child as { children?: unknown[] });
      }
    }
  };
  walk(tree as { children?: unknown[] });
};

// ---- Unwrap images ------------------------------------------------------
//
// `![alt](url)` is parsed as a paragraph wrapping an image node. Unwrap it
// so the image isn't rendered inside a <p> (which would prevent the figure
// post-processing in the custom handler).

const remarkUnwrapImages: Plugin<[], Root> = () => (tree) => {
  visit(tree, "paragraph", (node: Paragraph, index, parent) => {
    if (
      parent &&
      typeof index === "number" &&
      node.children.length === 1 &&
      node.children[0].type === "image"
    ) {
      parent.children.splice(index, 1, node.children[0] as Image);
    }
  });
};

// ---- Rewrite LikeC4 iframes to discoverable placeholders ----------------
//
// Authors embed LikeC4 views as raw `<iframe src="/c4/view/...">` tags in
// markdown. `remark-parse` produces an `html` node whose `value` is the raw
// HTML, and `remark-rehype` with `allowDangerousHtml: true` passes it through
// as a `raw` HAST node — never as an `iframe` element we could visit.
//
// So we rewrite at the mdast stage: walk every `html` node, find iframes
// whose `src` starts with `/c4/`, and replace them with a placeholder
// `<div class="likec4-iframe" data-src=... data-height=... data-title=...>`.
// `BlockDiscovery` picks the div up and mounts the LikeC4Block component
// (which renders its own iframe + zoom button + modal).
//
// Bare-bones regex over a known-shape author input — our content writes
// `<iframe ...></iframe>` with whitespace between attrs, no `>` in attr
// values, and an explicit closing tag.

const escapeAttr = (s: string): string =>
  s
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

const remarkRewriteLikeC4Iframes: Plugin<[], Root> = () => (tree) => {
  visit(tree, "html", (node: Html) => {
    if (!node.value.includes("/c4/")) return;
    node.value = node.value.replace(
      /<iframe\b([^>]*)>\s*<\/iframe>/gi,
      (full, attrs: string) => {
        const srcMatch = /\bsrc="(\/c4\/[^"]+)"/.exec(attrs);
        if (!srcMatch) return full;
        const heightMatch = /\bheight="([^"]+)"/.exec(attrs);
        const titleMatch = /\btitle="([^"]+)"/.exec(attrs);
        const heightAttr = heightMatch
          ? ` data-height="${escapeAttr(heightMatch[1])}"`
          : "";
        const titleAttr = titleMatch
          ? ` data-title="${escapeAttr(titleMatch[1])}"`
          : "";
        return `<div class="likec4-iframe" data-src="${escapeAttr(srcMatch[1])}"${heightAttr}${titleAttr}></div>`;
      },
    );
  });
};

// ---- Custom code handler ------------------------------------------------
//
// Routes:
//   ```d2     → <div class="d2-diagram">{rendered SVG}</div>
//                (or .d2-error for compile failures)
//   <div class="d2-slides"> + D2 fences
//              → <div class="d2-slides"><div class="d2-slide">...</div>...</div>
//   ```mermaid → <div class="mermaid-block" data-mermaid-source="...">  (placeholder)
//   ```<lang> run → <div class="runnable-code" data-...>                (placeholder)
//   merged group → <div class="runnable-group" data-tabs="...">         (placeholder)
//   anything else → default <pre><code class="language-x">…</code></pre>
//
// The placeholders are React-portal-mounted by Scala.js after innerHTML
// injection.

/** Visible authoring-error card (mirrors `.d2-error`) for workbench fences. */
const workbenchErrorElement = (message: string): Element => ({
  type: "element",
  tagName: "div",
  properties: { className: ["workbench-error", "not-prose"] },
  children: [
    {
      type: "element",
      tagName: "p",
      properties: { className: ["workbench-error__title"] },
      children: [{ type: "text", value: "Workbench authoring error" }],
    },
    {
      type: "element",
      tagName: "pre",
      properties: {},
      children: [{ type: "text", value: message }],
    },
  ],
});

/** Build the RunnableTabNode for a lone runnable fence (viz attrs included). */
const tabFromCode = (node: Code): RunnableTabNode => {
  const lang = resolveLanguage(node.lang ?? null)!;
  const sLang = node.lang ?? "";
  const meta = typeof node.meta === "string" ? node.meta : "";
  const canViz = /^(python|java)/i.test(sLang);
  return {
    language: sLang,
    languageLabel: lang.label,
    source: node.value,
    runnable: lang.runnable,
    viz: canViz ? (parseMetaKv(meta, "viz") ?? undefined) : undefined,
    vizRoot: canViz ? (parseMetaKv(meta, "viz-root") ?? undefined) : undefined,
    vizCase: canViz ? (parseMetaKv(meta, "viz-case") ?? undefined) : undefined,
    vizKind: canViz ? (parseMetaKv(meta, "viz-kind") ?? undefined) : undefined,
  };
};

const codeHandler = (state: State, node: Code): Element | undefined => {
  const data = node.data as
    | {
        d2Svg?: string;
        d2Error?: string;
        runnableTabs?: RunnableTabNode[];
        testcasesSpec?: TestSpecNode;
        testcasesError?: string;
        solutionTabs?: SolutionTabNode[];
        solutionTime?: string;
        solutionSpace?: string;
      }
    | undefined;

  // ```testcases that survived remarkAbsorbTestcases is an authoring error —
  // either its JSON failed validation (reason on data) or it doesn't follow a
  // runnable fence at all.
  if (node.lang === "testcases") {
    return workbenchErrorElement(
      data?.testcasesError
        ? `\`\`\`testcases fence ignored — ${data.testcasesError}.`
        : "A ```testcases fence must immediately follow a runnable ```<lang> run block.",
    );
  }

  // ```quiz — answer-pick quiz card (JSON body) in a problem Description.
  if (node.lang === "quiz") {
    let parsed: {
      prompt?: unknown;
      input?: unknown;
      options?: unknown;
      answer?: unknown;
    };
    try {
      parsed = JSON.parse(node.value);
    } catch (err) {
      return workbenchErrorElement(
        `\`\`\`quiz fence is not valid JSON: ${err instanceof Error ? err.message : String(err)}.`,
      );
    }
    const options = Array.isArray(parsed.options)
      ? parsed.options.map((o) => String(o))
      : [];
    const answer = typeof parsed.answer === "string" ? parsed.answer : "";
    if (
      typeof parsed.input !== "string" ||
      options.length < 2 ||
      answer === "" ||
      !options.includes(answer)
    ) {
      return workbenchErrorElement(
        "```quiz needs {input: string, options: [2+ strings], answer: one of options}.",
      );
    }
    return {
      type: "element",
      tagName: "div",
      properties: {
        className: ["quiz-block", "not-prose"],
        "data-quiz": encodeURIComponent(
          JSON.stringify({
            prompt: typeof parsed.prompt === "string" ? parsed.prompt : undefined,
            input: parsed.input,
            options,
            answer,
          }),
        ),
      },
      children: [],
    };
  }

  // Read-only solution viewer (merged by remarkGroupSolution).
  if (data?.solutionTabs) {
    const properties: Record<string, string | string[]> = {
      className: ["workbench-solution"],
      "data-tabs": encodeURIComponent(JSON.stringify(data.solutionTabs)),
    };
    if (data.solutionTime) properties["data-time"] = data.solutionTime;
    if (data.solutionSpace) properties["data-space"] = data.solutionSpace;
    return {
      type: "element",
      tagName: "div",
      properties,
      children: [],
    };
  }

  // Runnable fence(s) + absorbed ```testcases → stacked workbench editor.
  if (data?.testcasesSpec) {
    const tabs = data.runnableTabs ?? [tabFromCode(node)];
    return {
      type: "element",
      tagName: "div",
      properties: {
        className: ["workbench-inline"],
        "data-tabs": encodeURIComponent(JSON.stringify(tabs)),
        "data-spec": encodeURIComponent(JSON.stringify(data.testcasesSpec)),
      },
      children: [],
    };
  }

  if (node.lang === "d2" && data?.d2Svg) {
    const fragment = fromHtml(data.d2Svg, { fragment: true });
    return {
      type: "element",
      tagName: "div",
      properties: { className: ["d2-diagram", "not-prose"] },
      children: fragment.children as ElementContent[],
    };
  }
  if (node.lang === "d2" && data?.d2Error) {
    return {
      type: "element",
      tagName: "div",
      properties: { className: ["d2-error", "not-prose"] },
      children: [
        {
          type: "element",
          tagName: "p",
          properties: { className: ["d2-error-title"] },
          children: [{ type: "text", value: "D2 render error" }],
        },
        {
          type: "element",
          tagName: "pre",
          properties: {},
          children: [{ type: "text", value: data.d2Error }],
        },
      ],
    };
  }
  if (node.lang === "mermaid") {
    return {
      type: "element",
      tagName: "div",
      properties: {
        className: ["mermaid-block"],
        "data-mermaid-source": encodeURIComponent(node.value),
      },
      children: [],
    };
  }

  // ```d3 widget=array-traversal   → interactive D3 widget placeholder.
  // The body is the raw payload (JSON in v1) — the client-side widget owns the
  // schema. Mirrors the D2Slides precedent: shared keeps the payload structural,
  // each widget interprets it.
  if (node.lang === "d3") {
    const meta = typeof node.meta === "string" ? node.meta : "";
    const widget = parseMetaKv(meta, "widget");
    if (widget) {
      return {
        type: "element",
        tagName: "div",
        properties: {
          className: ["d3-widget"],
          "data-widget": widget,
          "data-payload": encodeURIComponent(node.value),
        },
        children: [],
      };
    }
    // No widget= key — fall through to default code rendering so authors get
    // a visible "this fence is wrong" pre block rather than silent emptiness.
  }

  const meta = typeof node.meta === "string" ? node.meta : "";
  const runRequested = /\brun\b/.test(meta);
  const traceRequested = /\btrace\b/.test(meta);

  // ```python trace  / ```java trace  → step-through visualisation placeholder.
  // Server-side runs the code unchanged via /api/run; the client wraps it in a
  // language-specific tracer harness (PythonTracer / JvmTracer) and parses the
  // marker-delimited trace from stdout. Slice 3: Python + Java both supported.
  //
  // Slice 7: adjacent ```kotlin trace / ```scala trace fences absorbed by
  // remarkGroupTrace are encoded as `data-companions` on the java placeholder —
  // `TracedCodeBlock` renders them as source-only language-switching tabs.
  if (traceRequested && node.lang) {
    const lang = resolveLanguage(node.lang);
    const isTracedLang = /^python/i.test(node.lang) || /^java$/i.test(node.lang);
    if (lang && isTracedLang) {
      const companions =
        (node.data?.traceCompanions as TraceCompanionNode[] | undefined) ?? [];
      const properties: Record<string, string | string[]> = {
        className: ["traced-code-block"],
        "data-lang": node.lang,
        "data-source": encodeURIComponent(node.value),
      };
      if (companions.length > 0) {
        properties["data-companions"] = encodeURIComponent(
          JSON.stringify(companions),
        );
      }
      return {
        type: "element",
        tagName: "div",
        // `traced-code-block` is the placeholder (mirrors `mermaid-block`); the mounted Scala.js
        // component renders its own `traced-code` root inside so CSS rules don't apply twice.
        properties,
        children: [],
      };
    }
  }

  if (data?.runnableTabs) {
    return {
      type: "element",
      tagName: "div",
      properties: {
        className: ["runnable-group"],
        "data-tabs": encodeURIComponent(JSON.stringify(data.runnableTabs)),
      },
      children: [],
    };
  }

  if (runRequested && node.lang) {
    const lang = resolveLanguage(node.lang);
    if (lang && lang.runnable) {
      // `viz=` / `viz-root=` opt a lone Python OR Java runnable fence into the
      // Visualise button (ADR-0018 / ADR-0021). Grouped fences carry these per-tab
      // via runnableTabs. Kotlin / Scala fall back to source-only display.
      const canViz = /^(python|java)/i.test(node.lang);
      const viz = canViz ? parseMetaKv(meta, "viz") : null;
      const vizRoot = canViz ? parseMetaKv(meta, "viz-root") : null;
      const vizCase = canViz ? parseMetaKv(meta, "viz-case") : null;
      const vizKind = canViz ? parseMetaKv(meta, "viz-kind") : null;
      const properties: Record<string, string | string[]> = {
        className: ["runnable-code"],
        "data-lang": node.lang,
        "data-language-label": lang.label,
        "data-source": encodeURIComponent(node.value),
      };
      if (viz) properties["data-viz"] = viz;
      if (vizRoot) properties["data-viz-root"] = vizRoot;
      if (vizCase) properties["data-viz-case"] = vizCase;
      if (vizKind) properties["data-viz-kind"] = vizKind;
      return {
        type: "element",
        tagName: "div",
        properties,
        children: [],
      };
    }
  }

  return defaultHandlers.code(state, node) as Element;
};

// ---- Workbench packaging --------------------------------------------------
//
// Two late hast plugins (registered AFTER rehype-pretty-code so every moved
// sub-tree keeps its shiki highlighting) lift authored sections into single
// placeholder divs whose children are inert `<template data-wb="…">` elements:
//
//   rehypePackageYourTurn — the `## Your Turn` section (statement + starter
//     workbench + an Editorial <details>) becomes one `.your-turn` launch-card
//     placeholder; the Scala component opens the packaged workbench in a modal.
//   rehypePackageProblem — in problem mode the WHOLE document becomes one
//     `.problem-workbench` placeholder (description template + one ed-section
//     template per top-level <details>).
//
// Templates matter: browsers parse template children into a detached
// DocumentFragment, so BlockDiscovery's querySelectorAll over the article
// cannot see nested placeholders (no double-mounting) and nothing renders
// before the workbench mounts the panel content itself.

interface LooseNode {
  type: string;
  value?: string;
  tagName?: string;
  properties?: Record<string, unknown>;
  children?: LooseNode[];
}

const hasClass = (n: LooseNode, cls: string): boolean => {
  const c = n.properties?.className;
  if (Array.isArray(c)) return c.includes(cls);
  if (typeof c === "string") return c.split(" ").includes(cls);
  return false;
};

const stripHtmlTags = (s: string): string =>
  s
    .replace(/<[^>]*>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .trim();

interface DetailsGroup {
  /** Index (in the scanned node list) where the depth-0 `<details>` opens. */
  start: number;
  /** Index where depth returns to 0 — may equal the next group's `start`
   *  when one raw node holds `</details>\n<details>` back to back. */
  end: number;
  /** Plain-text `<summary>` title ("" when none was found). */
  title: string;
  /** Body nodes — between the summary close and the final `</details>`. */
  body: LooseNode[];
}

// `<details>` arrives as opaque hast `raw` nodes (the pipeline has no
// rehype-raw), with parsed markdown elements interleaved between the opening
// and closing tags. This scanner walks raw-node token streams with a depth
// counter, so a single raw node carrying close + open of two adjacent groups
// is split correctly.
const detailsTokenRe = /<details\b[^>]*>|<\/details\s*>/gi;

const scanDetailsGroups = (nodes: LooseNode[]): DetailsGroup[] => {
  const groups: DetailsGroup[] = [];
  let depth = 0;
  let current: {
    start: number;
    title: string;
    body: LooseNode[];
    pending: string;
    awaitingSummary: boolean;
  } | null = null;

  const flushPending = () => {
    if (current && current.pending.trim() !== "")
      current.body.push({ type: "raw", value: current.pending });
    if (current) current.pending = "";
  };

  for (let i = 0; i < nodes.length; i++) {
    const n = nodes[i];
    if (
      n.type === "raw" &&
      typeof n.value === "string" &&
      /<\/?details/i.test(n.value)
    ) {
      const feedText = (text: string) => {
        if (!current) return; // depth-0 filler between groups (whitespace)
        if (current.awaitingSummary) {
          const sm = /<summary\b[^>]*>([\s\S]*?)<\/summary\s*>/i.exec(text);
          if (sm) {
            current.title = stripHtmlTags(sm[1]);
            current.awaitingSummary = false;
            current.pending += text.slice(sm.index + sm[0].length);
            return;
          }
        }
        current.pending += text;
      };
      detailsTokenRe.lastIndex = 0;
      let last = 0;
      let m: RegExpExecArray | null;
      while ((m = detailsTokenRe.exec(n.value))) {
        if (m.index > last) feedText(n.value.slice(last, m.index));
        const isClose = m[0][1] === "/";
        if (isClose) {
          if (depth > 0) depth--;
          if (depth === 0 && current) {
            flushPending();
            groups.push({
              start: current.start,
              end: i,
              title: current.title,
              body: current.body,
            });
            current = null;
          } else if (current) {
            current.pending += m[0];
          }
        } else {
          if (depth === 0) {
            current = {
              start: i,
              title: "",
              body: [],
              pending: "",
              awaitingSummary: true,
            };
          } else if (current) {
            current.pending += m[0];
          }
          depth++;
        }
        last = detailsTokenRe.lastIndex;
      }
      if (last < n.value.length) feedText(n.value.slice(last));
      flushPending(); // keep fragment order relative to following elements
    } else if (current) {
      flushPending();
      current.body.push(n);
    }
  }
  return groups;
};

// hast models `<template>` content as a detached `content` Root — children of
// the element itself are ignored by hast-util-to-html. Mirrors the browser's
// template.content DocumentFragment.
const wbTemplate = (
  kind: string,
  children: LooseNode[],
  extra?: Record<string, string>,
): LooseNode & { content: { type: "root"; children: LooseNode[] } } => ({
  type: "element",
  tagName: "template",
  properties: { "data-wb": kind, ...(extra ?? {}) },
  children: [],
  content: { type: "root", children },
});

interface RenderCtx {
  /** Text of the document's leading h1, captured by the h1-strip plugin. */
  docTitle: string;
  /** Set by rehypePackageProblem when the whole doc was packaged. */
  problemPackaged: boolean;
}

const rehypePackageYourTurn =
  (ctx: RenderCtx): Plugin<[], HastRoot> =>
  () =>
  (tree) => {
    const kids = tree.children as unknown as LooseNode[];
    const h2Idx = kids.findIndex(
      (c) =>
        c.type === "element" &&
        c.tagName === "h2" &&
        collectText(c as unknown as Element)
          .trim()
          .toLowerCase() === "your turn",
    );
    if (h2Idx === -1) return;
    let end = h2Idx + 1;
    while (
      end < kids.length &&
      !(kids[end].type === "element" && kids[end].tagName === "h2")
    ) {
      end++;
    }
    const slice = kids.slice(h2Idx + 1, end);
    const wbIdx = slice.findIndex(
      (c) => c.type === "element" && hasClass(c, "workbench-inline"),
    );
    // No starter-with-testcases block → an ordinary prose section. Leave it
    // exactly as authored (back-compat for every non-pilot chapter).
    if (wbIdx === -1) return;
    const wb = slice[wbIdx];

    const groups = scanDetailsGroups(slice);
    const editorial = groups.find((g) =>
      g.title.toLowerCase().includes("editorial"),
    );
    const statement = slice.filter((n, i) => {
      if (i === wbIdx) return false;
      if (editorial && i >= editorial.start && i <= editorial.end) return false;
      return true;
    });
    const blurbNode = statement.find(
      (n) => n.type === "element" && n.tagName === "p",
    );
    const heading = kids[h2Idx];
    const headingId =
      typeof heading.properties?.id === "string"
        ? (heading.properties.id as string)
        : undefined;

    const card: LooseNode = {
      type: "element",
      tagName: "div",
      properties: {
        className: ["your-turn", "not-prose"],
        ...(headingId ? { id: headingId } : {}),
        "data-title": ctx.docTitle || "Your Turn",
        "data-blurb": blurbNode
          ? collectText(blurbNode as unknown as Element).trim()
          : "",
        "data-tabs": String(wb.properties?.["data-tabs"] ?? ""),
        "data-spec": String(wb.properties?.["data-spec"] ?? ""),
      },
      children: [
        wbTemplate("statement", statement),
        wbTemplate("editorial", editorial ? editorial.body : []),
      ],
    };
    kids.splice(h2Idx, end - h2Idx, card as never);
  };

const rehypePackageProblem =
  (ctx: RenderCtx, enabled: boolean): Plugin<[], HastRoot> =>
  () =>
  (tree) => {
    if (!enabled) return;
    const kids = tree.children as unknown as LooseNode[];
    const wbIdx = kids.findIndex(
      (c) => c.type === "element" && hasClass(c, "workbench-inline"),
    );
    if (wbIdx === -1) {
      // ADR-0001 leniency: structure didn't parse — render as a normal
      // prose chapter rather than a broken workbench.
      console.warn(
        "[markdown] problem mode: no runnable block with a ```testcases fence — falling back to the prose layout",
      );
      return;
    }
    const wb = kids[wbIdx];
    const groups = scanDetailsGroups(kids);
    const firstGroupStart =
      groups.length > 0 ? groups[0].start : kids.length;
    const description = kids.filter(
      (_, i) => i < firstGroupStart && i !== wbIdx,
    );

    const packaged: LooseNode = {
      type: "element",
      tagName: "div",
      properties: {
        className: ["problem-workbench", "not-prose"],
        "data-tabs": String(wb.properties?.["data-tabs"] ?? ""),
        "data-spec": String(wb.properties?.["data-spec"] ?? ""),
      },
      children: [
        wbTemplate("description", description),
        ...groups.map((g) =>
          wbTemplate("ed-section", g.body, {
            "data-wb-title": g.title || "Notes",
          }),
        ),
      ],
    };
    tree.children = [packaged as never];
    ctx.problemPackaged = true;
  };

// ---- TOC extraction -----------------------------------------------------
//
// Runs after rehype-slug so each <h*> has an id. Collects {depth, slug,
// text}; the Scala.js sidebar renders these directly.

export interface TocEntry {
  depth: number;
  slug: string;
  text: string;
}

const collectText = (
  node: Element | Text | ElementContent | undefined,
): string => {
  if (!node) return "";
  if (node.type === "text") return (node as Text).value;
  if (node.type === "element") {
    const el = node as Element;
    // Skip rehype-autolink-headings appended <span class="heading-anchor-icon">.
    const cls = el.properties?.className;
    const list = Array.isArray(cls)
      ? cls
      : typeof cls === "string"
        ? cls.split(" ")
        : [];
    if (list.includes("heading-anchor") || list.includes("heading-anchor-icon"))
      return "";
    return (el.children ?? []).map(collectText).join("");
  }
  return "";
};

const rehypeCollectToc =
  (collector: TocEntry[]): Plugin<[], HastRoot> =>
  () =>
  (tree) => {
    visit(tree, "element", (node: Element) => {
      if (!/^h[1-6]$/.test(node.tagName)) return;
      const depth = parseInt(node.tagName[1], 10);
      const id = node.properties?.id;
      const slug = typeof id === "string" ? id : "";
      if (!slug) return;
      collector.push({ depth, slug, text: collectText(node) });
    });
  };

// ---- Public API ---------------------------------------------------------

export interface RenderResult {
  /** Article HTML, ready for `dangerouslySetInnerHTML` / `innerHTML`. */
  html: string;
  /** Headings + slugs in document order. */
  toc: TocEntry[];
}

// Shiki "CSS variables" theme — emits var(--shiki-token-*) for every token color
// instead of baked hexes, so code recolors with the site's --syn-* palette
// (mapped onto a dark code slab in chapter-content.css). Built once at module load.
const cssVarsTheme = createCssVariablesTheme({
  name: "cortex",
  variablePrefix: "--shiki-",
  fontStyle: true,
});

export interface RenderOptions {
  /** `problem` packages the whole document into a `.problem-workbench`
   *  placeholder (two-pane workbench layout); default is the prose chapter. */
  mode?: "chapter" | "problem";
}

/** Render a chapter's raw markdown source. */
export async function renderChapter(
  source: string,
  opts?: RenderOptions,
): Promise<RenderResult> {
  const toc: TocEntry[] = [];
  const mode = opts?.mode ?? "chapter";
  const renderCtx: RenderCtx = { docTitle: "", problemPackaged: false };

  const processor = unified()
    .use(remarkParse)
    .use(remarkGfm)
    .use(remarkMath)
    .use(remarkExpandD2ArrayTraversal)
    .use(remarkRenderD2)
    .use(remarkGroupD2Slides)
    .use(remarkGroupRunnable)
    .use(remarkAbsorbTestcases)
    .use(remarkGroupSolution)
    .use(remarkGroupTrace)
    .use(remarkUnwrapImages)
    .use(remarkRewriteLikeC4Iframes)
    .use(remarkRehype, {
      handlers: { code: codeHandler },
      // Allow our custom div placeholders + d2's inline SVG markup to pass
      // through unmodified.
      allowDangerousHtml: true,
    })
    .use(rehypeSlug)
    // Strip the leading h1 — ChapterPage already renders the title as a
    // page-level <h1>. Chapters that have no h1 are unaffected. The text is
    // kept on renderCtx: the Your-Turn launch card reuses it as the modal
    // workbench's title.
    .use((() => (tree: HastRoot) => {
      const idx = tree.children.findIndex(
        (c) => c.type === "element" && (c as Element).tagName === "h1",
      );
      if (idx !== -1) {
        renderCtx.docTitle = collectText(tree.children[idx] as Element).trim();
        tree.children.splice(idx, 1);
      }
    }) as Plugin<[], HastRoot>)
    .use(rehypeAutolinkHeadings, {
      behavior: "append",
      properties: {
        className: ["heading-anchor"],
        ariaLabel: "Link to section",
      },
      content: {
        type: "element",
        tagName: "span",
        properties: { className: ["heading-anchor-icon"] },
        children: [{ type: "text", value: "#" }],
      },
    })
    .use(rehypeCollectToc(toc))
    // Collapse the trailing "Sources & Verify" section into a <details> so it
    // doesn't clutter the chapter by default. Runs after rehypeCollectToc so the
    // heading keeps its slug id + TOC entry (collectToc visits recursively, so it
    // still finds the heading once it's nested inside <summary>). The generic
    // .chapter-content details/summary styling renders the chevron + box.
    .use((() => (tree: HastRoot) => {
      const kids = tree.children;
      const idx = kids.findIndex(
        (c) =>
          c.type === "element" &&
          (c as Element).tagName === "h2" &&
          collectText(c as Element).trim().toLowerCase() === "sources & verify",
      );
      if (idx === -1) return;
      // Take the heading + every following sibling up to the next h2 (or end).
      let end = idx + 1;
      while (
        end < kids.length &&
        !(kids[end].type === "element" && (kids[end] as Element).tagName === "h2")
      ) {
        end++;
      }
      const heading = kids[idx] as Element;
      const body = kids.slice(idx + 1, end);
      const summary: Element = {
        type: "element",
        tagName: "summary",
        properties: {},
        children: [heading],
      };
      const details: Element = {
        type: "element",
        tagName: "details",
        properties: { className: ["cortex-sources-details"] },
        children: [summary, ...body],
      };
      kids.splice(idx, end - idx, details);
    }) as Plugin<[], HastRoot>)
    .use(rehypeKatex)
    .use(rehypePrettyCode, {
      theme: cssVarsTheme,
      keepBackground: true,
      defaultLang: "plaintext",
      bypassInlineCode: true,
    })
    // Workbench packagers run AFTER rehype-pretty-code so the sub-trees they
    // move into <template> children are already shiki-highlighted.
    .use(rehypePackageYourTurn(renderCtx))
    .use(rehypePackageProblem(renderCtx, mode === "problem"))
    .use(rehypeStringify, { allowDangerousHtml: true });

  const file = await processor.process(source);
  // Markdown inside a raw HTML <summary> (the collapsible Q/A and trace blocks
  // author it as `<summary>… `code` …</summary>`) is NOT parsed by remark — the
  // whole <details>…<summary> is an opaque HTML block — so authored `code`
  // spans survive as literal backticks, which read as stray quotation marks in
  // the serif reader font. Convert backtick spans to <code> within every
  // <summary>, leaving the rest of the raw HTML untouched.
  const html = String(file).replace(
    /(<summary\b[^>]*>)([\s\S]*?)(<\/summary>)/g,
    (_m, open: string, inner: string, close: string) =>
      open +
      inner.replace(
        /`([^`\n]+)`/g,
        (_s, code: string) =>
          `<code>${code.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</code>`,
      ) +
      close,
  );
  // A packaged problem page has no prose headings — the workbench renders its
  // own chrome — so the TOC collected mid-pipeline is meaningless there.
  return { html, toc: renderCtx.problemPackaged ? [] : toc };
}
