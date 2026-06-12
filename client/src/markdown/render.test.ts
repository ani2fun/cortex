// Pipeline tests for the workbench additions: ```testcases absorption,
// ```quiz / ```solution fences, the Your-Turn section packager, and
// problem-mode whole-document packaging. Pure node — no DOM, no Scala.js.
import { describe, expect, it } from "vitest";

import { renderChapter } from "./render";

const attr = (html: string, selectorClass: string, name: string): string | null => {
  // Placeholder divs serialize as <div class="…selectorClass…" data-x="…">.
  const re = new RegExp(
    `<div[^>]*class="[^"]*\\b${selectorClass}\\b[^"]*"[^>]*\\b${name}="([^"]*)"`,
  );
  return html.match(re)?.[1] ?? null;
};

const decodeJsonAttr = (html: string, cls: string, name: string): unknown => {
  const raw = attr(html, cls, name);
  return raw === null ? null : JSON.parse(decodeURIComponent(raw));
};

const SPEC_JSON = `{
  "args": [{"id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 3]"}],
  "cases": [
    {"args": {"arr": "[1, 2, 3]"}, "expected": "[3, 2, 1]"},
    {"args": {"arr": "[]"}, "expected": "[]"}
  ]
}`;

const RUNNABLE_PAIR = [
  "```python run viz=array viz-root=arr",
  "print('py')",
  "```",
  "",
  "```java run",
  "System.out.println();",
  "```",
].join("\n");

describe("testcases absorption", () => {
  it("merges runnable fences + testcases into one workbench-inline placeholder", async () => {
    const md = `${RUNNABLE_PAIR}\n\n\`\`\`testcases\n${SPEC_JSON}\n\`\`\`\n`;
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-inline");
    expect(html).not.toContain("runnable-group");
    expect(html).not.toContain("workbench-error");

    const tabs = decodeJsonAttr(html, "workbench-inline", "data-tabs") as Array<
      Record<string, unknown>
    >;
    expect(tabs).toHaveLength(2);
    expect(tabs[0].language).toBe("python");
    expect(tabs[0].viz).toBe("array");
    expect(tabs[0].vizRoot).toBe("arr");
    expect(tabs[1].language).toBe("java");

    const spec = decodeJsonAttr(html, "workbench-inline", "data-spec") as {
      args: Array<{ id: string }>;
      cases: Array<{ expected?: string }>;
    };
    expect(spec.args[0].id).toBe("arr");
    expect(spec.cases).toHaveLength(2);
    expect(spec.cases[0].expected).toBe("[3, 2, 1]");
  });

  it("upgrades a lone runnable fence too, carrying its viz attrs into the tab", async () => {
    const md = [
      "```python run viz=array",
      "print('x')",
      "```",
      "",
      "```testcases",
      SPEC_JSON,
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-inline");
    expect(html).not.toContain('class="runnable-code"');
    const tabs = decodeJsonAttr(html, "workbench-inline", "data-tabs") as Array<
      Record<string, unknown>
    >;
    expect(tabs).toHaveLength(1);
    expect(tabs[0].viz).toBe("array");
  });

  it("coerces numeric case values to strings", async () => {
    const md = [
      "```python run",
      "print('x')",
      "```",
      "",
      "```testcases",
      `{"args": [{"id": "n", "label": "n", "type": "int"}], "cases": [{"args": {"n": 6}, "expected": "3"}]}`,
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    const spec = decodeJsonAttr(html, "workbench-inline", "data-spec") as {
      cases: Array<{ args: Record<string, string> }>;
    };
    expect(spec.cases[0].args.n).toBe("6");
  });

  it("degrades to the existing group + an error card when the JSON is invalid", async () => {
    const md = `${RUNNABLE_PAIR}\n\n\`\`\`testcases\n{not json}\n\`\`\`\n`;
    const { html } = await renderChapter(md);
    expect(html).toContain("runnable-group");
    expect(html).not.toContain("workbench-inline");
    expect(html).toContain("workbench-error");
    expect(html).toContain("not valid JSON");
  });

  it("flags a testcases fence with no preceding runnable block", async () => {
    const md = ["Some prose.", "", "```testcases", SPEC_JSON, "```"].join("\n");
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-error");
    expect(html).toContain("must immediately follow a runnable");
  });

  it("rejects a case referencing an undeclared arg id", async () => {
    const md = [
      "```python run",
      "print('x')",
      "```",
      "",
      "```testcases",
      `{"args": [{"id": "a", "label": "a", "type": "int"}], "cases": [{"args": {"b": "1"}}]}`,
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-error");
    expect(html).toContain("not a declared arg id");
  });
});

describe("quiz fence", () => {
  it("emits a quiz-block placeholder for valid JSON", async () => {
    const md = [
      "```quiz",
      `{"input": "n = 20", "options": ["19", "8"], "answer": "8"}`,
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    const quiz = decodeJsonAttr(html, "quiz-block", "data-quiz") as {
      input: string;
      options: string[];
      answer: string;
    };
    expect(quiz.input).toBe("n = 20");
    expect(quiz.options).toEqual(["19", "8"]);
    expect(quiz.answer).toBe("8");
  });

  it("errors when the answer is not among the options", async () => {
    const md = [
      "```quiz",
      `{"input": "n", "options": ["1", "2"], "answer": "3"}`,
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-error");
    expect(html).not.toContain("quiz-block");
  });
});

describe("solution fences", () => {
  it("merges adjacent solution fences into one read-only viewer placeholder", async () => {
    const md = [
      "```python solution time=O(n) space=O(1)",
      "def f(): pass",
      "```",
      "",
      "```java solution",
      "class S {}",
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    const tabs = decodeJsonAttr(html, "workbench-solution", "data-tabs") as Array<
      Record<string, unknown>
    >;
    expect(tabs).toHaveLength(2);
    expect(tabs[0].language).toBe("python");
    expect(attr(html, "workbench-solution", "data-time")).toBe("O(n)");
    expect(attr(html, "workbench-solution", "data-space")).toBe("O(1)");
  });

  it("routes even a lone solution fence to the viewer", async () => {
    const md = ["```python solution", "def f(): pass", "```"].join("\n");
    const { html } = await renderChapter(md);
    expect(html).toContain("workbench-solution");
    expect(html).not.toContain("<pre");
  });
});

const YOUR_TURN_DOC = [
  "# Chapter Title",
  "",
  "## Intro",
  "",
  "Some intro prose.",
  "",
  "## Your Turn",
  "",
  "Implement the thing yourself.",
  "",
  "```python run",
  "# Your code goes here",
  "```",
  "",
  "```testcases",
  SPEC_JSON,
  "```",
  "",
  "<details>",
  "<summary>Editorial</summary>",
  "",
  "The full walkthrough.",
  "",
  "```python solution",
  "def solved(): pass",
  "```",
  "",
  "</details>",
  "",
  "## Reflect",
  "",
  "Closing prose.",
].join("\n");

describe("Your Turn packaging", () => {
  it("replaces the section with a launch-card placeholder holding templates", async () => {
    const { html } = await renderChapter(YOUR_TURN_DOC);
    expect(html).toContain('class="your-turn not-prose"');
    expect(attr(html, "your-turn", "data-title")).toBe("Chapter Title");
    expect(attr(html, "your-turn", "data-blurb")).toContain(
      "Implement the thing yourself",
    );
    expect(attr(html, "your-turn", "data-tabs")).toBeTruthy();
    expect(attr(html, "your-turn", "data-spec")).toBeTruthy();
    // Section content was lifted into templates…
    expect(html).toContain('<template data-wb="statement">');
    expect(html).toContain('<template data-wb="editorial">');
    const editorialTpl = html.slice(html.indexOf('data-wb="editorial"'));
    expect(editorialTpl).toContain("workbench-solution");
    expect(editorialTpl).toContain("The full walkthrough.");
    // …the heading anchor survives on the card, and surrounding sections stay.
    expect(attr(html, "your-turn", "id")).toBe("your-turn");
    expect(html).toContain(">Intro<");
    expect(html).toContain(">Reflect<");
    // The statement template does NOT contain the editor (it lives on data-tabs).
    const statementTpl = html.slice(
      html.indexOf('data-wb="statement"'),
      html.indexOf('data-wb="editorial"'),
    );
    expect(statementTpl).not.toContain("workbench-inline");
  });

  it("leaves a prose-only Your Turn section untouched (back-compat)", async () => {
    const md = [
      "## Your Turn",
      "",
      "Just prose, no starter block.",
      "",
      "```python run",
      "print('demo')",
      "```",
    ].join("\n");
    const { html } = await renderChapter(md);
    expect(html).not.toContain('class="your-turn');
    expect(html).toContain('class="runnable-code"');
  });
});

const PROBLEM_DOC = [
  "# Flip Things",
  "",
  "## The Problem",
  "",
  "Reverse the array in place.",
  "",
  "## Examples",
  "",
  "Example prose.",
  "",
  "```quiz",
  `{"input": "arr = [a, b]", "options": ["[a, b]", "[b, a]"], "answer": "[b, a]"}`,
  "```",
  "",
  "```python run viz=array",
  "# starter",
  "```",
  "",
  "```testcases",
  SPEC_JSON,
  "```",
  "",
  "<details>",
  "<summary><h2>Intuition</h2></summary>",
  "",
  "Walk inward from both ends.",
  "",
  "```python",
  "x = 1",
  "```",
  "",
  "</details>",
  "<details>",
  "<summary><h2>Solution &amp; Analysis</h2></summary>",
  "",
  "```python solution time=O(n) space=O(1)",
  "def flip(arr): pass",
  "```",
  "",
  "</details>",
].join("\n");

describe("problem-mode packaging", () => {
  it("packages the whole document into one problem-workbench placeholder", async () => {
    const { html, toc } = await renderChapter(PROBLEM_DOC, { mode: "problem" });
    expect(html).toContain('class="problem-workbench not-prose"');
    expect(toc).toEqual([]);
    expect(attr(html, "problem-workbench", "data-tabs")).toBeTruthy();
    expect(attr(html, "problem-workbench", "data-spec")).toBeTruthy();

    const descTpl = html.slice(
      html.indexOf('data-wb="description"'),
      html.indexOf('data-wb="ed-section"'),
    );
    expect(descTpl).toContain("Reverse the array in place.");
    expect(descTpl).toContain("quiz-block");
    expect(descTpl).not.toContain("workbench-inline");

    expect(html).toContain('data-wb-title="Intuition"');
    expect(html).toContain('data-wb-title="Solution & Analysis"'.replace("&", "&#x26;"));
    const intuition = html.slice(
      html.indexOf('data-wb-title="Intuition"'),
      html.indexOf("Solution"),
    );
    // Highlighting survived inside the template (shiki emits CSS-var spans).
    expect(intuition).toContain("--shiki-");
    const lastTpl = html.slice(html.lastIndexOf("ed-section"));
    expect(lastTpl).toContain("workbench-solution");
  });

  it("handles a combined </details><details> raw node between sections", async () => {
    const md = [
      "```python run",
      "# starter",
      "```",
      "",
      "```testcases",
      SPEC_JSON,
      "```",
      "",
      "<details>",
      "<summary>First</summary>",
      "",
      "Alpha body.",
      "",
      "</details>",
      "<details>",
      "<summary>Second</summary>",
      "",
      "Beta body.",
      "",
      "</details>",
    ].join("\n");
    const { html } = await renderChapter(md, { mode: "problem" });
    expect(html).toContain('data-wb-title="First"');
    expect(html).toContain('data-wb-title="Second"');
    const first = html.slice(
      html.indexOf('data-wb-title="First"'),
      html.indexOf('data-wb-title="Second"'),
    );
    expect(first).toContain("Alpha body.");
    expect(first).not.toContain("Beta body.");
  });

  it("falls back to the prose layout when no workbench block exists", async () => {
    const md = ["# T", "", "## The Problem", "", "Prose only."].join("\n");
    const { html, toc } = await renderChapter(md, { mode: "problem" });
    expect(html).not.toContain("problem-workbench");
    expect(html).toContain(">The Problem<");
    expect(toc.length).toBeGreaterThan(0);
  });

  it("does not package in default chapter mode", async () => {
    const { html } = await renderChapter(PROBLEM_DOC);
    expect(html).not.toContain("problem-workbench");
    expect(html).toContain("workbench-inline");
  });
});
