---
title: '1. What a skill is'
summary: A skill packages expertise as a folder the model loads only when it's relevant. The mechanism that makes that possible — progressive disclosure across three levels — is the whole idea, and a real 55 KB skill in this repo shows why it matters.
---

# 1. What a skill is

## TL;DR

> An **Agent Skill** is a folder containing a **`SKILL.md`** — instructions (and optionally scripts
> and reference files) that package expertise for a *specific* kind of task. The breakthrough isn't
> the folder; it's **progressive disclosure**, the discipline of revealing information in **three
> levels**: **(L1)** every skill's short *name + description* is always in context — cheap, a few
> dozen tokens each; **(L2)** when a description matches the task at hand, the model loads that
> skill's full `SKILL.md` body; **(L3)** the body can point to bundled files or scripts that load (or
> run) *only* when actually needed. The payoff: an agent can have a *library* of deep expertise on
> call while carrying almost none of it at rest.

## 1. Motivation

You're an agent with a finite context window — say a million tokens, but every token costs money and,
past a point, *attention*. Your operator wants you to be expert at fifty different things: authoring
quiz exercises in this repo's exact format, filling PDF forms, building Excel models, running a
specific database migration safely, and forty-six more. Each "expertise" is a page or ten of precise
instructions.

Two bad options present themselves. **Paste it all into the system prompt**, always: now every
trivial request drags fifty playbooks' worth of tokens, you're paying for context you don't use, and
the model is hunting for the relevant page in a haystack of forty-nine irrelevant ones. **Or paste
none of it**: now you're a generalist who gets this repo's quiz format subtly wrong every time,
because nobody told you the rules.

Skills are the third option, and it's the one a good librarian would recognize instantly. A library
doesn't hand you every book when you walk in; it keeps a **catalog** (titles + one-line descriptions)
and fetches the *full book* only when you ask for that topic. A skill's `description` is its catalog
card — always present, nearly free. Its body is the book — fetched on demand. This repo proves the
stakes: the real `graphify` skill's body is **55 KB**. Always-loaded, that's a tax on every single
turn. Loaded *on demand*, it costs nothing until the moment you ask a knowledge-graph question — and
then it's exactly what you need.

## 2. Intuition (Analogy)

Picture an **expert's bookshelf**, organized for just-in-time recall.

- The **spines** face out: each shows a title and a one-line "what's inside." You can scan all of them
  at a glance, cheaply, all the time. That's **Level 1** — every skill's name and description, always
  in context.
- When a task matches a spine, you **pull that one book** off the shelf and open it. Now you have the
  full procedure in front of you — but only for the book you needed. That's **Level 2** — the matched
  skill's `SKILL.md` body loads.
- Inside the book, a chapter says *"for the exact tax tables, see Appendix C"* — and you flip there
  **only if this job needs the tables.** That's **Level 3** — bundled files or scripts the body
  references, loaded (or executed) on demand.

The genius is that you never confuse "knowing a book *exists*" with "having read it." A fluent expert
keeps the whole *catalog* in their head and the *contents* on the shelf. Skills give an agent the
same superpower: vast available expertise, tiny resting footprint.

| | Everything in the system prompt | Nothing (generalist) | **Skills (progressive disclosure)** |
|---|---|---|---|
| Resting context cost | Huge (all playbooks, always) | Zero | **Tiny (just descriptions)** |
| Knows the special procedure? | Yes, but buried | No | **Yes, loaded when relevant** |
| Scales to *many* expertises | No — context blows up | N/A | **Yes — add a folder** |
| Right info at the right time | Drowned in noise | Absent | **Surfaced on a match** |

## 3. Formal Definition

An **Agent Skill** is a directory whose required file is **`SKILL.md`**: a Markdown file with **YAML
frontmatter** (at minimum a `name` and a `description`) followed by a **Markdown body** of
instructions. The directory may also bundle **resources** — reference documents, templates, and
**executable scripts** the body can invoke.

**Progressive disclosure** is the loading strategy that makes skills scale:

| Level | What loads | When | Cost |
|---|---|---|---|
| **L1 — metadata** | every skill's `name` + `description` | **always** in context | a few dozen tokens each |
| **L2 — body** | one skill's full `SKILL.md` body | when its description **matches the task** | the body's size, once |
| **L3 — resources** | a bundled file or script the body names | only when **that step needs it** | only what's used |

Two properties follow. **Skills are model-discovered**: the model reads the L1 descriptions and
*decides* to load a skill when the task fits — so the **`description` is effectively the trigger**
(Chapter 3), which is why writing it well is the single most important part of a skill. And skills are
**composable and shareable**: because a skill is just a folder, it can live in your personal
`~/.claude/skills/`, in a project's `.claude/skills/`, or be distributed in a plugin (Chapter 7).

> The one line to remember: **a skill lets an agent *know that it knows something* (cheaply, always)
> and *recall the details* (fully, on demand).** Progressive disclosure is what separates "fifty
> playbooks I'm drowning in" from "fifty playbooks I can summon one at a time."

## 4. Worked Example — graphify, by the level

The `graphify` skill in this repo is a textbook case. Watch the three levels with real numbers.

```mermaid
flowchart TD
  Q([User: "How does the auth module work?"]) --> L1
  L1["**L1 — always loaded**<br/>graphify: 'any input → knowledge graph.<br/>Use when user asks about a codebase…'<br/>(~30 tokens)"]
  L1 -->|description matches the task| L2["**L2 — loads now**<br/>the full SKILL.md body<br/>(~55 KB of procedure)"]
  L2 -->|step needs examples| L3["**L3 — loads if needed**<br/>a bundled query-examples file"]
  L1 -.->|no match → never loads| X[("body stays on the shelf")]
  classDef s fill:#1f2937,stroke:#6366f1,color:#e5e7eb,rx:6,ry:6;
  class L1,L2,L3 s;
```

On a turn that has *nothing* to do with knowledge graphs, graphify costs you ~30 tokens — its
description — and its 55 KB body never loads. The instant you ask "how does X work in this codebase?",
the description matches, the body loads, and the agent suddenly has the full procedure. That asymmetry
— *almost free until relevant, then fully present* — is the entire value proposition, and it's why a
single agent can ship dozens of skills without bloating every conversation.

## 5. Build It

Let's make the savings undeniable. This models a tiny skill library and compares "paste every body
always" against progressive disclosure — at rest, on a match, and when a bundled file is needed.

```python run
SKILLS = {
    "graphify": {"desc": 30, "body": 14000, "files": {"query_examples": 800}},
    "pdf":      {"desc": 20, "body": 3000,  "files": {"fill_form": 600}},
    "xlsx":     {"desc": 22, "body": 2500,  "files": {}},
}

def progressive(match=None, need_file=None):
    """L1 always; L2 if a skill's description matched; L3 if a bundled file is needed."""
    cost = sum(s["desc"] for s in SKILLS.values())          # L1: ALL descriptions, always
    loaded = ["L1: all descriptions"]
    if match:
        cost += SKILLS[match]["body"]                       # L2: the matched skill's body
        loaded.append(f"L2: {match} body")
        if need_file and need_file in SKILLS[match]["files"]:
            cost += SKILLS[match]["files"][need_file]       # L3: one bundled file
            loaded.append(f"L3: {match}/{need_file}")
    return cost, loaded

naive = sum(s["desc"] + s["body"] + sum(s["files"].values()) for s in SKILLS.values())
print(f"naive  (all bodies always):  {naive:>6} tokens")
for label, (m, ffile) in {
    "progressive, idle":            (None, None),
    "progressive, graphify task":   ("graphify", None),
    "progressive, + bundled file":  ("graphify", "query_examples"),
}.items():
    cost, loaded = progressive(m, ffile)
    print(f"{label:<28} {cost:>6} tokens   {loaded}")
```

**Now break it.** At idle, progressive disclosure costs **72** tokens versus the naive **20,972** — a
~290× difference for carrying the *same* library. Now change the idle case to also load every body
"just in case" and watch it collapse back to the naive number: the savings exist *only* because L2/L3
stay on the shelf until a description matches. That match is everything — which is why a vague
description (Chapter 3) is the classic way to break a skill: it either never triggers (knowledge
stranded) or always triggers (savings gone).

## 6. Trade-offs & Complexity

| Skills (progressive disclosure) | Everything in the system prompt | A bespoke tool/MCP server |
|---|---|---|
| Tiny resting cost; scales to many | Huge resting cost; doesn't scale | Heavier to build than a Markdown file |
| Pure knowledge/instructions, no infra | Same, but always-on | Best for *actions* and live data |
| Loads on a description match | Always present (no match needed) | Invoked explicitly by the model |
| A bad description mis-triggers | Can't mis-trigger; just always there | Schema is explicit, less ambiguity |
| Authoring = write a Markdown file | Authoring = edit one big prompt | Authoring = write + run a server |

A skill is the right tool when the thing you're adding is **expertise** — how to do something,
conventions, a procedure — that's mostly instructions and reference material. It's the *wrong* tool
when you need a live **action** against the world (call an API, query a database): that's a tool or an
MCP server (Chapter 5). The cost of skills is their dependence on good descriptions and the small
indirection of on-demand loading; the benefit is a library of deep know-how at near-zero resting cost.

## 7. Edge Cases & Failure Modes

- **Vague description → never triggers.** If the `description` doesn't clearly say *when* to use the
  skill, the model won't load it, and the expertise sits unused. Write trigger conditions explicitly
  (Chapter 3).
- **Over-broad description → always triggers.** The opposite failure: a skill that loads for
  everything reintroduces the bloat you were avoiding. Scope it.
- **Giant always-on content.** Putting the whole procedure in the *description* (L1) instead of the
  *body* (L2) defeats progressive disclosure — you pay the big cost on every turn.
- **Should've been a tool.** Packaging an *action* (send email, run a query) as a skill gives the
  model instructions but no way to *act*. Use a tool/MCP for actions (Chapter 5).
- **Stale or wrong instructions.** A skill is trusted know-how; if it's outdated, the agent confidently
  does the wrong thing. Skills need maintenance like any other code (Chapter 7).

## 8. Practice

> **Exercise 1 — Name the three levels.** A teammate says "a skill is just a big instruction file the
> model always reads." Correct them using progressive disclosure: what are the three levels, what
> loads at each, and *when*?

<details>
<summary><strong>Answer</strong></summary>

The teammate has it backwards: a skill is the *opposite* of "always read." Its whole point is that the
big instruction file is **not** always loaded (§3). The three levels:

- **L1 — metadata (`name` + `description`):** loaded **always**, for *every* skill. Cheap (a few dozen
  tokens each). This is the catalog the model scans.
- **L2 — the `SKILL.md` body:** loaded **only when that skill's description matches the current task**.
  This is the full procedure — potentially large (graphify's is 55 KB), which is exactly why you don't
  want it always on.
- **L3 — bundled resources/scripts:** loaded or executed **only when a step in the body actually needs
  them** (a reference file, a script).

So the correct statement is: a skill keeps a *cheap description* in context always, and reveals the
*expensive details* only on demand. "Always reads the big file" describes the system-prompt approach
skills exist to replace.

</details>

> **Exercise 2 — Do the savings math.** Using the §5 model's numbers, compute the resting (idle)
> context cost under progressive disclosure vs. the naive "load every body" approach, and state in one
> sentence why the gap exists.

<details>
<summary><strong>Answer</strong></summary>

From §5: descriptions are 30 + 20 + 22 = **72** tokens; bodies are 14000 + 3000 + 2500 = 19500; bundled
files are 800 + 600 = 1400.

- **Progressive, idle:** only L1 loads → **72 tokens**.
- **Naive (all bodies + files always):** 72 + 19500 + 1400 = **20,972 tokens**.

The gap (~290×) exists because at rest **only the descriptions are in context** — every body and
bundled file stays "on the shelf" (L2/L3) until a description matches a task. You carry the *catalog*,
not the *library*. Add a fourth or fiftieth skill and the resting cost grows by only its description,
not its whole body — which is what makes skills *scale* where a fat system prompt can't.

</details>

> **Exercise 3 — Skill or not?** For each, say whether it's a good fit for a *skill* or belongs
> elsewhere, and why: (a) "the exact 9-section format and rules for authoring a quiz exercise in this
> repo"; (b) "fetch the current price of a stock"; (c) "the company's brand voice and tone guidelines
> for blog posts."

<details>
<summary><strong>Answer</strong></summary>

The test (§6): skills are for **expertise/instructions/conventions** loaded on demand; **actions
against the live world** belong in a tool or MCP server.

- **(a) Quiz-authoring format — a skill.** It's pure procedure/convention (how to structure the file,
  the rules), exactly what a `SKILL.md` body holds, and it's only relevant when authoring a quiz — a
  textbook progressive-disclosure case. (This is literally Chapter 6's `workbench-author` skill.)
- **(b) Current stock price — NOT a skill; a tool/MCP.** This is a live *action* fetching real-time
  data from the world. A skill can only carry *instructions*; it can't perform the lookup. You need a
  tool the model can call (Part 3 ch5 / Part 4).
- **(c) Brand voice guidelines — a skill.** Like (a), it's reference knowledge/conventions, relevant
  only when writing posts. Bundle the style guide as the body (and maybe example posts as L3
  resources).

The throughline: (a) and (c) are *knowledge*; (b) is an *action*. Knowledge → skill; action → tool.

</details>

```quiz
{
  "prompt": "What is \"progressive disclosure\" in the context of Agent Skills?",
  "input": "Choose one:",
  "options": [
    "Loading information in levels — name+description always, the full SKILL.md body only when the description matches the task, and bundled files only when a step needs them",
    "Gradually revealing the answer to the user one sentence at a time",
    "Encrypting the skill so only authorized agents can read it",
    "Always loading every skill's full body so the model never misses anything"
  ],
  "answer": "Loading information in levels — name+description always, the full SKILL.md body only when the description matches the task, and bundled files only when a step needs them"
}
```

## In the Wild

- **[Anthropic — Introducing Agent Skills](https://www.anthropic.com/news/skills)** — the announcement
  and the case for skills as composable, model-invoked expertise. The primary source.
- **[Claude Docs — Agent Skills](https://docs.claude.com/en/docs/agents-and-tools/agent-skills)** — the
  authoritative guide to the `SKILL.md` format, progressive disclosure, and where skills live.
- **[The real `graphify` skill](https://github.com/ani2fun/cortex)** — this repo's installed skill
  (`~/.claude/skills/graphify/SKILL.md`): a 55 KB body behind a one-line description — progressive
  disclosure you can open and read. Dissected in Chapter 2.

---

**Next:** now open the file. What exactly goes in the frontmatter, what goes in the body, and what can
we learn by reading a real, shipping `SKILL.md` line by line? →
[2. SKILL.md anatomy](/cortex/the-claude-stack/agent-skills/skill-md-anatomy)
