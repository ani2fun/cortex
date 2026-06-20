---
title: '2. SKILL.md anatomy'
summary: Open the file and name every part — the `---`-delimited YAML frontmatter (required `name` + `description`) and the Markdown body — then dissect this repo's real 55 KB graphify SKILL.md to see why the description is the load-bearing line.
---

# 2. SKILL.md anatomy

## TL;DR

> A `SKILL.md` is just two parts glued together. **(1) YAML frontmatter** — the block fenced by two
> `---` lines at the very top — carries the metadata the model always sees: a required **`name`**
> (short, kebab-case) and a required **`description`** (one or two sentences saying *what* the skill
> does **and when** to use it), plus optional keys like **`trigger`** (a slash command). **(2) A
> Markdown body** — everything after the closing `---` — holds the actual instructions: headings,
> steps, code, and references to bundled files. Mapped to Chapter 1's levels: the **description is
> L1** (always in context, so it both *advertises* and *triggers* the skill), the **body is L2**
> (loads only on a description match), and files the body points to are **L3**. Get the frontmatter
> wrong and a perfect body never loads — which is why the `description` is the single most important
> line in the file.

## 1. Motivation

In Chapter 1 we established *why* skills exist: progressive disclosure lets an agent carry a library
of expertise while paying, at rest, for only the descriptions. But "a folder with a `SKILL.md`" is
still abstract until you open the file and see what's actually in it. This repo ships a real one —
`~/.claude/skills/graphify/SKILL.md`, **about 55 KB** — so we don't have to invent an example.

Here's the practical stake. You're going to *author* skills, and the failure modes are not in the
body — they're in the top five lines. Put your trigger text in the wrong place and the skill never
loads. Forget a required key and the loader rejects the file. Write a vague `description` and the
model can't tell when the skill applies, so 55 KB of carefully-written procedure sits on the shelf,
useless. The body is where the *work* goes; the frontmatter is where the *correctness* lives. To get
either right you need to know exactly which bytes do what — so this chapter opens the real file and
names every part.

## 2. Intuition (Analogy)

A `SKILL.md` is a **labeled recipe card**.

- The **top of the card** — the title and the little "*use this when you're making a weeknight
  curry*" line — is what you read **at a glance** while flipping through the box. You decide *whether
  to cook this* without reading the method. That's the **frontmatter**: `name` (the title) and
  `description` (the when-to-use line). You scan dozens of these cheaply.
- The **rest of the card** — the ingredient list and the numbered method — is what you read **only
  once you've pulled this card out to cook**. That's the **body**: the full procedure, read on
  demand.

Or, sharper: a `SKILL.md` is a **passport**. The **identity page** is tiny — a name and a one-line
description of who you are — and it's what gets *checked at every gate* (always in context, L1). It
*gates* a thick book of pages behind it (the body, L2) that nobody reads unless the identity page
matched first. A flawless book behind a blank identity page gets you nowhere; the gate never opens.

| | Recipe card / passport | `SKILL.md` |
|---|---|---|
| The glance-able header | Title + "use when making X" line / identity page | **Frontmatter:** `name` + `description` |
| When you read it | Always, while flipping / at every gate | **Always in context (L1)** |
| The detailed contents | The method / the book of pages | **Markdown body** |
| When you read *that* | Only once you've chosen this card | **On a description match (L2)** |
| "See appendix C for tables" | A pointer to another page | **Bundled file by relative path (L3)** |
| Failure if header is blank | You skip the card / the gate stays shut | **Body never loads** |

## 3. Formal Definition

A **`SKILL.md`** is a UTF-8 Markdown file with two regions:

1. **YAML frontmatter** — opens on the *first line* with a line that is exactly `---`, and ends at
   the next line that is exactly `---`. The lines between are `key: value` pairs.
   - **Required:** `name` — a short **kebab-case** identifier (e.g. `graphify`); and `description` —
     **one or two sentences** stating **what** the skill does **and when** to use it. The
     description is the text loaded into context at **L1**, so it serves double duty: it *advertises*
     the skill and it *is the trigger* the model matches against (Chapter 3).
   - **Optional (seen in the wild):** `trigger` (e.g. a slash command like `/graphify`), `license`,
     `allowed-tools`, and version markers.
2. **Markdown body** — everything after the closing `---`. Ordinary Markdown: headings, prose,
   numbered steps, code blocks. This is the skill's **L2** content, loaded only when the description
   matches. The body may reference **bundled files by relative path**; those are **L3**, loaded or
   executed only when a step needs them.

| Term | Meaning | Level |
|---|---|---|
| **Frontmatter** | The `---`-delimited YAML block at the top | metadata |
| **`name`** (required) | Short kebab-case identifier for the skill | L1 |
| **`description`** (required) | One–two sentences: *what* + *when* — advertises **and** triggers | **L1** |
| **`trigger`** (optional) | An explicit invocation, e.g. a `/slash` command | L1 |
| **Body** | The Markdown after the closing `---`: instructions, steps, code | **L2** |
| **Bundled file** | A resource/script the body names by relative path | **L3** |

> The one line to remember: **the frontmatter is the part the model always sees; the body is the part
> it loads on a match.** Everything load-bearing about *whether* a skill fires lives in those few
> frontmatter lines — above all, the `description`.

## 4. Worked Example — dissecting the real graphify SKILL.md

Here's the shape of the file on disk and how its bytes map onto the three levels:

```mermaid
flowchart TD
  subgraph FILE["~/.claude/skills/graphify/SKILL.md (~55 KB)"]
    direction TB
    FM["--- (open fence)<br/>name: graphify<br/>description: \"any input … → knowledge graph. Use when …\"<br/>trigger: /graphify<br/>--- (close fence)"]
    BODY["# /graphify<br/>## Usage<br/>## What graphify is for<br/>## What You Must Do When Invoked (Steps 0–5)<br/>…"]
    FM --> BODY
  end
  DIR["skill directory also holds:<br/>.graphify_version"]
  FM -. "L1 — always in context (~30 tokens)" .-> L1[(description advertises + triggers)]
  BODY -. "L2 — loads on a description match (~55 KB)" .-> L2[(full procedure)]
  BODY -. "references files by relative path" .-> L3[(L3 — loaded only if a step needs it)]
  classDef s fill:#1f2937,stroke:#6366f1,color:#e5e7eb,rx:6,ry:6;
  class FM,BODY,DIR s;
```

The skill is a **directory** (`~/.claude/skills/graphify/`). Its required file is `SKILL.md`; the
directory also carries a `.graphify_version` file (an L3 resource the skill uses to track its own
version). Now the actual frontmatter, copied verbatim from the file:

```yaml
---
name: graphify
description: "any input (code, docs, papers, images, videos) to knowledge graph. Use when user asks any question about a codebase, documents, or project content - especially if graphify-out/ exists, treat the question as a /graphify query."
trigger: /graphify
---
```

Read it the way the model does. The **`name`** is `graphify` — short, kebab-case, the skill's handle.
The **`description`** is doing three jobs in one sentence-and-a-half: it says **what** ("any input …
to knowledge graph"), it says **when** ("Use when user asks any question about a codebase, documents,
or project content"), and it even encodes a **heuristic** ("especially if `graphify-out/` exists,
treat the question as a `/graphify` query"). That entire string is **L1** — it's in context on *every*
turn, costing ~30 tokens, and it's the text the model pattern-matches to decide whether to pull the
body. The optional **`trigger: /graphify`** gives users an explicit invocation. That's the whole
identity page — a few dozen tokens gating 55 KB.

Below the closing `---` the **body** begins, and it's structured like a manual. It opens with the
`# /graphify` H1, then `## Usage` (a menu of every command form), `## What graphify is for` (scope),
and `## What You Must Do When Invoked` — a numbered procedure, **Steps 0–5**, the agent follows in
order. None of that ~55 KB is in context right now, as you read this chapter, because nothing here
matched the description. Ask "how does the auth module work in this repo?" and the description matches,
the body loads, and the agent suddenly has all six steps. That asymmetry — tiny always, huge on demand
— is exactly the Chapter 1 picture, now visible in one real file.

## 5. Build It

The cleanest way to internalize the format is to **parse it**. Below is a tiny `SKILL.md` parser
written in pure standard-library Python — no `yaml` import. It splits on the `---` fences, reads the
frontmatter `key: value` lines into a dict, lists the `##` headings in the body, and then **validates**
that the two required keys are present. We run it on a valid graphify-shaped sample (PASS), then on a
second sample whose frontmatter is missing `description` (a specific FAIL).

```python run
# A SKILL.md PARSER — no yaml lib, pure stdlib string ops.
# Splits on the `---` fences, parses frontmatter `key: value` lines into a
# dict, lists the `##` headings in the body, then VALIDATES that the two
# required keys (name, description) are present.

FENCE = "-" * 3  # three dashes: the frontmatter delimiter


def parse_skill_md(text):
    """Return (frontmatter_dict, body_headings, error_or_None)."""
    lines = text.splitlines()

    # A SKILL.md must OPEN with a fence line. If it doesn't, there is no
    # frontmatter at all — that is itself a structural failure.
    if not lines or lines[0].strip() != FENCE:
        return {}, [], "no opening '---' fence: frontmatter is missing"

    # Find the CLOSING fence (the next line that is exactly '---').
    close = None
    for i in range(1, len(lines)):
        if lines[i].strip() == FENCE:
            close = i
            break
    if close is None:
        return {}, [], "no closing '---' fence: frontmatter never ends"

    front_lines = lines[1:close]   # between the two fences
    body_lines = lines[close + 1:]  # everything after the body fence

    # Parse the frontmatter: each non-blank line is `key: value`.
    front = {}
    for raw in front_lines:
        line = raw.strip()
        if not line:
            continue
        if ":" not in line:
            continue  # not a key/value line; ignore (lenient parser)
        key, value = line.split(":", 1)
        key = key.strip()
        value = value.strip()
        # Strip one layer of surrounding quotes if present.
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        front[key] = value

    # Collect the body's section headings (lines starting with '## ').
    headings = []
    for raw in body_lines:
        if raw.startswith("## "):
            headings.append(raw[3:].strip())

    return front, headings, None


def validate(front):
    """A SKILL.md REQUIRES name + description. Return (ok, message)."""
    required = ["name", "description"]
    missing = [k for k in required if k not in front or not front[k]]
    if missing:
        return False, "FAIL: missing required key(s): " + ", ".join(missing)
    return True, "PASS: required keys present (name, description)"


def report(label, text):
    front, headings, err = parse_skill_md(text)
    print("=== " + label + " ===")
    if err is not None:
        print("FAIL: " + err)
        print("")
        return
    print("frontmatter keys: " + ", ".join(front.keys()))
    print("name        = " + front.get("name", "<none>"))
    # Truncate the (long) description so the output stays tidy & deterministic.
    desc = front.get("description", "<none>")
    if len(desc) > 60:
        desc = desc[:57] + "..."
    print("description = " + desc)
    print("trigger     = " + front.get("trigger", "<none>"))
    print("body '##' headings: " + ", ".join(headings))
    ok, msg = validate(front)
    print(msg)
    print("")


# --- Sample 1: a VALID SKILL.md (graphify's real shape) -----------------
SAMPLE_OK = (
    FENCE + "\n"
    "name: graphify\n"
    'description: "any input to knowledge graph. '
    'Use when user asks any question about a codebase."\n'
    "trigger: /graphify\n"
    + FENCE + "\n"
    "\n"
    "# /graphify\n"
    "\n"
    "## Usage\n"
    "Run the pipeline on a folder.\n"
    "\n"
    "## What graphify is for\n"
    "Turn files into a queryable knowledge graph.\n"
    "\n"
    "## What You Must Do When Invoked\n"
    "Follow the numbered steps in order.\n"
)

# --- Sample 2: BROKEN — frontmatter is missing `description` ------------
SAMPLE_BAD = (
    FENCE + "\n"
    "name: lonely-skill\n"
    "trigger: /lonely\n"
    + FENCE + "\n"
    "\n"
    "# /lonely\n"
    "\n"
    "## Usage\n"
    "This skill forgot its description, so L1 has nothing to advertise.\n"
)

report("Sample 1: valid graphify-shaped SKILL.md", SAMPLE_OK)
report("Sample 2: missing 'description'", SAMPLE_BAD)
```

Running it prints exactly:

```
=== Sample 1: valid graphify-shaped SKILL.md ===
frontmatter keys: name, description, trigger
name        = graphify
description = any input to knowledge graph. Use when user asks any ques...
trigger     = /graphify
body '##' headings: Usage, What graphify is for, What You Must Do When Invoked
PASS: required keys present (name, description)

=== Sample 2: missing 'description' ===
frontmatter keys: name, trigger
name        = lonely-skill
description = <none>
trigger     = /lonely
body '##' headings: Usage
FAIL: missing required key(s): description
```

**Now break it.** Sample 1 PASSes — it has both required keys, the parser recovers the three
frontmatter values, and it finds the body's three `##` headings (the same shape as the real graphify
file). Sample 2 FAILs with a *specific* diagnosis — `missing required key(s): description` — and notice
*why* that matters: a skill with no `description` has **nothing to put in L1**. The model would have no
when-to-use text to match against, so the body, however good, could never be triggered. That is the
frontmatter failure mode made concrete: the loader (and our parser) rejects the file before the body
ever matters. The body is the *work*; the frontmatter is the *gate*.

## 6. Trade-offs & Complexity

Where should a given piece of information go — frontmatter or body?

| Decision | Put it in **frontmatter** (L1) | Put it in the **body** (L2) |
|---|---|---|
| Resting context cost | Paid on **every** turn — keep it tiny | **Zero** until a description match |
| What belongs here | `name`, the when-to-use `description`, `trigger` | the procedure, steps, examples, code |
| Effect of size | A long description taxes every turn | A 55 KB body is free until relevant |
| If you put the procedure here | Defeats progressive disclosure (always-on bulk) | Correct: bulk loads on demand |
| If you put when-to-use *only* here vs. body | Right — the model needs it to match | Too late — body loads *after* the match |
| Format strictness | Strict: valid YAML, required keys, fences | Lenient: ordinary Markdown |

The rule of thumb falls straight out of the levels. **Frontmatter is expensive real estate** (it's in
context always), so it holds only what's needed to *decide and trigger*: a short name, a sharp
description, an optional trigger. **The body is cheap** (it loads only on a match), so *everything
substantial* goes there — and there's no penalty for a large, thorough body, which is exactly why
graphify can afford 55 KB of procedure. The one genuine cost of the format is its **frontmatter
strictness**: the fences must be exact, the YAML must parse, and the required keys must be present, or
the file is rejected. Cheap to get right once you know the shape — which is the point of this chapter.

## 7. Edge Cases & Failure Modes

- **Missing required key.** No `name` or no `description` → the loader rejects the file (our §5 parser
  reproduces this exact FAIL). A skill with no description has nothing to advertise at L1.
- **Malformed fences.** The frontmatter must *open on line 1* with `---` and *close* with a matching
  `---`. A missing or misplaced fence means the parser can't tell metadata from body — frontmatter is
  silently lost or the whole block is treated as body.
- **Procedure smuggled into the description.** Cramming steps or long context into the `description`
  puts bulk at **L1** (always-on), defeating progressive disclosure — the very tax skills exist to
  avoid (Chapter 1 §7). Keep the description to one or two sentences; push detail to the body.
- **When-to-use buried in the body.** If the *trigger conditions* live only in the body (L2), the
  model can't see them until *after* it has already decided to load — too late. The "when" must be in
  the **description** (Chapter 3).
- **YAML quoting traps.** A `description` containing a colon, `#`, or other YAML-special characters
  can mis-parse unless quoted. graphify's description is wrapped in double quotes for exactly this
  reason. When in doubt, quote the value.
- **Body references a file that isn't bundled.** An L3 pointer (relative path) to a script or doc that
  doesn't exist in the skill directory fails only *when that step runs* — a late, confusing error.
  Ship the referenced files alongside `SKILL.md` (Chapter 4).

## 8. Practice

> **Exercise 1 — Label the regions.** Given the real graphify file, identify: (a) the two delimiter
> lines that bound the frontmatter, (b) every required frontmatter key and its value, (c) one optional
> key, and (d) where the body begins. For each, state which progressive-disclosure level it belongs to.

<details>
<summary><strong>Answer</strong></summary>

From §4:

- **(a) Delimiters:** the line `---` on the *first* line of the file (open fence) and the next `---`
  line (close fence). Everything between them is the frontmatter.
- **(b) Required keys:** `name: graphify` and `description: "any input … treat the question as a
  /graphify query."` — both are **L1** (always in context).
- **(c) Optional key:** `trigger: /graphify` (also L1; an explicit invocation, not required by the
  format).
- **(d) Body:** begins immediately after the closing `---`, with the `# /graphify` H1, then `## Usage`,
  `## What graphify is for`, `## What You Must Do When Invoked`. The body is **L2** — loaded only when
  the description matches.

So three frontmatter lines (name, description, trigger) are L1; the ~55 KB body is L2; any file the
body points to (e.g. `.graphify_version`) is L3.

</details>

> **Exercise 2 — Why the description, not the body?** A teammate writes a skill whose frontmatter is
> just `name: pdf-fill` and puts a clear "use this when the user wants to fill a PDF form" sentence as
> the first line of the **body**. Why won't this trigger reliably, and what's the one-line fix?

<details>
<summary><strong>Answer</strong></summary>

It won't trigger because the "when to use" sentence is in the **body (L2)**, and the body loads *only
after* the model has already matched on the description. At decision time the model sees only the
frontmatter — here, just a bare `name` with no `description` — so it has nothing describing *when* the
skill applies, and won't load it. The body's helpful sentence arrives too late to influence the very
decision it was meant to inform (§3, §7).

**Fix:** move that sentence into the frontmatter as the `description`:
`description: "Fill a PDF form. Use when the user wants to fill out or complete a PDF form."` Now the
when-to-use text is at **L1**, visible on every turn, where the match actually happens.

</details>

> **Exercise 3 — Run the validator in your head.** Apply the §5 parser's `validate()` rule to this
> frontmatter, then say what the parser would print for `name`, `description`, and the PASS/FAIL line:
>
> ```
> ---
> name: db-migrate
> trigger: /migrate
> ---
> ```

<details>
<summary><strong>Answer</strong></summary>

`validate()` requires both `name` *and* `description` to be present and non-empty. Here `name =
db-migrate` is present, `trigger = /migrate` is present, but **`description` is absent**. So:

- `name        = db-migrate`
- `description = <none>` (the parser's default when the key is missing)
- and the validation line: **`FAIL: missing required key(s): description`**

This is the same failure as §5's Sample 2 (`lonely-skill`): a structurally well-formed frontmatter
that's still invalid because the one load-bearing field — the L1 `description` — is missing. The
`trigger` being present doesn't help; an optional key can't substitute for a required one.

</details>

```quiz
{
  "prompt": "In a SKILL.md, which part is loaded into the model's context on EVERY turn — and therefore must contain the when-to-use information?",
  "input": "Choose one:",
  "options": [
    "The YAML frontmatter — specifically the name and description keys",
    "The entire Markdown body, including all numbered steps",
    "Only the files the body references by relative path",
    "Nothing is loaded until the user explicitly types the trigger command"
  ],
  "answer": "The YAML frontmatter — specifically the name and description keys"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Claude Docs — Agent Skills](https://docs.claude.com/en/docs/agents-and-tools/agent-skills)** — the
  authoritative reference for the `SKILL.md` format: the frontmatter keys, the body, and how
  progressive disclosure loads them. Start here for the canonical field list.
- **[Anthropic Cookbook — agent_skills](https://github.com/anthropics/anthropic-cookbook)** — runnable
  examples of real `SKILL.md` files you can open and compare against the anatomy in this chapter.
- **[The real `graphify` skill](https://github.com/ani2fun/cortex)** — this repo's installed
  `~/.claude/skills/graphify/SKILL.md`: the exact frontmatter we dissected in §4, atop a ~55 KB body —
  a working file you can open and read end to end.

---

**Next:** the `description` keeps doing the load-bearing work — it's not just metadata, it's the
*trigger*. How does the model actually decide a description matches, and how do you write one (and a
`name`) that fires when it should and stays silent when it shouldn't? →
[3. Triggers & discovery](/cortex/the-claude-stack/agent-skills/triggers-and-discovery)
