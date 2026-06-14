---
title: 'Part 5 — Agent Skills'
summary: How to give an agent deep, specialized expertise without drowning it in context. Agent Skills package know-how as a folder the model loads on demand — and the trick that makes it work, progressive disclosure, is the heart of this Part. Grounded in a real 55 KB skill this repo already ships.
---

# Part 5 — Agent Skills

> **You can't paste everything you know into every prompt.** Context is finite and not free, so an
> agent can't carry every convention, procedure, and playbook at all times. **Agent Skills** solve
> this: each skill is a folder with a `SKILL.md` that packages expertise — instructions, and
> optionally scripts and reference files — that the model **loads only when the task calls for it.**
> The mechanism is **progressive disclosure**, and once it clicks, you'll see it everywhere. This
> Part builds skills from first principles and ends by writing one for Cortex.

## The problem skills solve

Part 3 taught you to put instructions in a system prompt. But imagine you have *fifty* specialized
procedures — how to author a workbench exercise, how to format a financial model, how to run a
particular migration safely. Stuff all fifty into every system prompt and you've spent a fortune in
tokens and buried the model in instructions it doesn't need for *this* task. Leave them out and the
model doesn't know them. Skills thread the needle: keep a **one-line description** of each skill in
context always (cheap), and load the **full how-to** only when the description matches what the user
is doing. Expertise, just-in-time.

## The real example we already have

This isn't hypothetical. This machine ships a skill called **`graphify`** — turn any folder into a
navigable knowledge graph — and its `SKILL.md` is **55 KB** of detailed procedure. You would *never*
want 55 KB in context on every turn. So you don't: its one-line `description` ("any input … to
knowledge graph. Use when user asks any question about a codebase …") is the only thing always
loaded, and the 55 KB body loads *only* when you ask a knowledge-graph question. Chapter 2 dissects
that real file; Chapter 6 builds a brand-new one for Cortex's workbench conventions (a skill we don't
ship — yet).

## Chapters

1. **[What a skill is](/cortex/the-claude-stack/agent-skills/what-a-skill-is)** — packaged expertise
   and the three levels of progressive disclosure.
2. **[SKILL.md anatomy](/cortex/the-claude-stack/agent-skills/skill-md-anatomy)** — frontmatter and
   body, dissected from the real `graphify` skill.
3. **[Triggers & discovery](/cortex/the-claude-stack/agent-skills/triggers-and-discovery)** — the
   description *is* the trigger; how and when a skill loads.
4. **[Bundled resources & scripts](/cortex/the-claude-stack/agent-skills/bundled-resources-and-scripts)** —
   the third level: files and executable code a skill carries.
5. **[Skills vs MCP vs subagents](/cortex/the-claude-stack/agent-skills/skills-vs-mcp-vs-subagents)** —
   three ways to extend an agent, and when to reach for each.
6. **[Build a Cortex skill](/cortex/the-claude-stack/agent-skills/build-a-cortex-skill)** — author a
   real `workbench-author` SKILL.md from this repo's conventions.
7. **[Sharing & governance](/cortex/the-claude-stack/agent-skills/sharing-and-governance)** —
   versioning, distribution via plugins, and the safety of third-party skills.

---

**Begin:** before the file format, the idea — what *is* a skill, and what is the "progressive
disclosure" that lets an agent hold a library of expertise without carrying it all at once? →
[1. What a skill is](/cortex/the-claude-stack/agent-skills/what-a-skill-is)
