// Lightweight runtime helpers the Scala.js client invokes via @JSImport.
// Two responsibilities:
//   1. Prism-based syntax highlighting for the runnable code editor.
//   2. On-demand mermaid rendering (theme-aware), called by MermaidBlock
//      after innerHTML injection of the rendered chapter HTML.
//
// KaTeX CSS is imported here too — once any chapter loads (including via
// MarkdownRenderer.render), the math styles are present on the page.

import Prism from "prismjs";
import "prismjs/components/prism-clike";
import "prismjs/components/prism-c";
import "prismjs/components/prism-cpp";
import "prismjs/components/prism-go";
import "prismjs/components/prism-java";
import "prismjs/components/prism-kotlin";
import "prismjs/components/prism-javascript";
import "prismjs/components/prism-python";
import "prismjs/components/prism-rust";
import "prismjs/components/prism-scala";
import "prismjs/components/prism-sql";
import "prismjs/components/prism-typescript";
import "prismjs/themes/prism-tomorrow.css";
import "katex/dist/katex.min.css";

// Map our runnable-language aliases (matching server-side `Languages.scala`)
// onto Prism grammar names.
const PRISM_BY_ALIAS: Record<string, string> = {
  python: "python",
  py: "python",
  python3: "python",
  java: "java",
  scala: "scala",
  c: "c",
  cpp: "cpp",
  "c++": "cpp",
  cxx: "cpp",
  go: "go",
  golang: "go",
  rust: "rust",
  rs: "rust",
  kotlin: "kotlin",
  kt: "kotlin",
  typescript: "typescript",
  ts: "typescript",
  javascript: "javascript",
  js: "javascript",
  node: "javascript",
  sql: "sql",
  sqlite: "sql",
};

const escapeHtml = (s: string): string =>
  s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

export function highlightWithPrism(code: string, languageAlias: string): string {
  const prismLang = PRISM_BY_ALIAS[languageAlias.toLowerCase()];
  const grammar = prismLang ? Prism.languages[prismLang] : undefined;
  if (!grammar) return escapeHtml(code);
  return Prism.highlight(code, grammar, prismLang);
}

// ---- Mermaid -----------------------------------------------------------
//
// Lazy-loads the mermaid bundle on first call, then re-renders the diagram
// into `target` whenever the theme changes (the Scala.js MermaidBlock
// component wires that up via its theme-watching effect).

let mermaidPromise: Promise<typeof import("mermaid").default> | null = null;
const loadMermaid = (): Promise<typeof import("mermaid").default> => {
  if (!mermaidPromise) {
    mermaidPromise = import("mermaid").then((m) => m.default);
  }
  return mermaidPromise;
};

let mermaidIdCounter = 0;

/** Render a mermaid diagram into `target.innerHTML`. Returns the resolved
  * promise so Scala.js can chain follow-up work; rejects on syntax errors.
  */
export async function renderMermaidInto(
  target: HTMLElement,
  source: string,
  dark: boolean
): Promise<void> {
  const mermaid = await loadMermaid();
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: "strict",
    theme: dark ? "dark" : "default",
    fontFamily: "inherit",
  });
  const id = `mermaid-${++mermaidIdCounter}`;
  const { svg } = await mermaid.render(id, source);
  target.innerHTML = svg;
}
