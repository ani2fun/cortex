// Pipeline guard for the three PILOT chapters — renders the real content files
// and asserts the workbench packaging holds (testcases absorbed, Your-Turn
// packaged, problem mode segmented). Skipped when the content tree is absent
// (e.g. a checkout without content/).
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

import { renderChapter } from "./render";

const ROOT = resolve(
  __dirname,
  "../../../content/cortex/data-structures-and-algorithms/02-linear-structures/01-arrays",
);

const read = (rel: string): string => readFileSync(resolve(ROOT, rel), "utf8");

const stripFrontmatter = (raw: string): string => {
  if (!raw.startsWith("---")) return raw;
  const end = raw.indexOf("\n---", 3);
  return end === -1 ? raw : raw.slice(end + 4);
};

describe.skipIf(!existsSync(ROOT))("pilot content packaging", () => {
  it("02-dynamic-arrays.md — inline workbench + Your-Turn card, no errors", async () => {
    const { html } = await renderChapter(stripFrontmatter(read("02-dynamic-arrays.md")));
    expect(html).not.toContain("workbench-error");
    // See It Work upgraded to the stacked editor…
    expect(html).toContain("workbench-inline");
    // …and the Your Turn section became the launch card (its own editor lives on data-tabs).
    expect(html).toContain('class="your-turn');
    expect(html).toContain('<template data-wb="statement">');
    const editorialTpl = html.slice(html.indexOf('data-wb="editorial"'));
    expect(editorialTpl).toContain("workbench-solution");
    // The chapter around the section is intact.
    expect(html).toContain(">How It Works<");
    expect(html).toContain(">Reflect &#x26; Connect<");
  });

  it("01-pattern.md — two-language inline workbench + palindrome Your-Turn", async () => {
    const { html } = await renderChapter(
      stripFrontmatter(read("04-pattern-two-pointers/01-pattern.md")),
    );
    expect(html).not.toContain("workbench-error");
    expect(html).toContain("workbench-inline");
    expect(html).toContain('class="your-turn');
    // Both See-It-Work languages merged into the workbench tabs.
    const tabsAttr = /class="workbench-inline[^>]*data-tabs="([^"]*)"/.exec(html)?.[1] ?? "";
    const tabs = JSON.parse(decodeURIComponent(tabsAttr)) as Array<{ language: string }>;
    expect(tabs.map((t) => t.language)).toEqual(["python", "java"]);
  });

  it("01-flip-characters.md — problem mode packages the whole page", async () => {
    const { html, toc } = await renderChapter(
      stripFrontmatter(read("04-pattern-two-pointers/02-problems/01-flip-characters.md")),
      { mode: "problem" },
    );
    expect(html).not.toContain("workbench-error");
    expect(html).toContain('class="problem-workbench');
    expect(toc).toEqual([]);
    // Description carries the prose, examples, quiz and constraints — not the editor.
    const desc = html.slice(
      html.indexOf('data-wb="description"'),
      html.indexOf('data-wb="ed-section"'),
    );
    expect(desc).toContain("The Problem");
    expect(desc).toContain("quiz-block");
    expect(desc).toContain("Constraints");
    expect(desc).not.toContain("workbench-inline");
    // All five details sections became ed-section templates, in order.
    const titles = [...html.matchAll(/data-wb-title="([^"]+)"/g)].map((m) => m[1]);
    expect(titles).toEqual([
      "Intuition",
      "Applying the Diagnostic Questions",
      "Approach",
      "Solution &#x26; Analysis",
      "Key Takeaway",
    ]);
    // The d3 widget survived inside the Intuition section; solutions became the viewer.
    expect(html).toContain("d3-widget");
    expect(html).toContain("workbench-solution");
  });
});
