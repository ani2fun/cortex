---
title: '6. Build a Cortex skill'
summary: We design a real skill — workbench-author — that captures this repo's actual chapter and exercise conventions, walk through every authoring decision (name, description-as-trigger, body-vs-script, progressive disclosure), show the SKILL.md we'd ship, and build a linter that proves the draft is house-format compliant. The skill is designed here, not installed — that gap is the whole point.
---

# 6. Build a Cortex skill

## TL;DR

> Cortex ships **no skill of its own** — that's a real gap. So in this chapter we *build one*: a
> **`workbench-author`** skill that packages this repo's actual authoring conventions (the
> **9-section chapter template**, the collapsible `<details>` answers, the `quiz` JSON schema, and the
> **`/api/run` byte-verification** rule) so any agent writing Cortex content gets it right without
> re-deriving the rules. Everything from Part 5 lands here at once: a **name** that says what it is, a
> **description that doubles as the trigger** (Chapter 3), a **lean body** that holds the conventions,
> and a **pointer to a bundled script** for the mechanical verification step (Chapter 4) — progressive
> disclosure end to end. We show the full `SKILL.md` we'd ship, then write a **linter** that parses our
> own draft and asserts it's compliant. To be explicit: this skill is **designed in this chapter, not
> installed in the repo.** Closing that gap is left as the natural next step.

> 🚢 **Update — Cortex now *does* ship a skill.** The "Cortex ships no skill of its own" gap is closed: the **[Cortex Tutor](/cortex/cortex-onboarding/cortex-tutor/grounding-and-the-skill)** ships **`.claude/skills/socratic-tutor/`** — the six-step coaching rubric, per-gate pass criteria, and the verdict contract the gate must emit. It's a *different* skill than the `workbench-author` we design here (it teaches the *coach* how to grade, not an agent how to author chapters), but it's a real, shipped Agent Skill — and it's even **CI-gated by eval suites**, the diligence this chapter preaches. The design exercise below still stands on its own; just know the "no skill" premise is now historical.

## 1. Motivation

Here is a true story about *this book*. Every chapter you're reading obeys a strict, mostly-unwritten
contract: frontmatter with a `title` and a `summary`; then nine sections in a fixed order; then
*exactly one* ```quiz``` block whose `answer` must be character-for-character one of its `options`;
then `In the Wild`; then a `**Next:**` link. Each Practice exercise must be followed by a collapsible
`<details><summary><strong>Answer</strong></summary>` block with **blank lines** around the inner
markdown, or it silently renders as a wall of literal HTML. And every ````python run```` block must
actually *run* — historically byte-verified through the go-judge `/api/run` sandbox with an **Accepted**
verdict before it ships.

Those rules are real, and they are *load-bearing*: break one and the page renders wrong, the quiz
won't grade, or a "runnable" example doesn't run. Yet where do they live? Scattered. Some are in the
build manifest, some in a half-stale project `CLAUDE.md`, some only in the head of whoever wrote the
last chapter. When a *fresh agent* sits down to author chapter forty-seven, it has to re-derive the
contract from examples — and it gets details subtly wrong, exactly the failure mode Chapter 1
described: a capable generalist that doesn't know *this repo's* house format.

This is precisely the problem skills exist to solve. The authoring contract is **expertise**: pure
convention and procedure, relevant *only* when someone is writing under `content/cortex/`, useless
otherwise. That is the textbook shape of a skill (Chapter 5: knowledge → skill; live action → tool).
So we'll build it. The honest catch — and a recurring theme of this book — is that Cortex **does not
yet ship this skill**. We're going to *design* it properly, show the file we'd commit, and even lint
it. Installing it is the gap we name and leave open.

## 2. Intuition (Analogy)

Think of this book as a **textbook factory** with many writers — here, many *agent* writers, some
human-driven, some running autonomously in parallel (this very book was built by a multi-session
loop). A factory that wants consistent output doesn't re-explain the house style to every worker who
clocks in. It writes the style **once**, into a single **house style guide**, and *pins it to the
wall above every writing desk* — so the instant a worker sits down to draft a page, the guide is right
there, and they reach for it without anyone re-briefing them.

A skill is that pinned guide, with one upgrade the physical version can't manage: it's **pinned by
topic, not by location.** The guide only "appears on the wall" when the worker's current task matches
its subject — when they're authoring Cortex content. On any other task it stays rolled up, costing
nothing. That conditional appearance is **progressive disclosure** (Chapter 1), and the thing that
decides *when* the guide unrolls is its one-line subject heading — the `description`, which *is* the
trigger (Chapter 3).

And like any good style guide, it doesn't reprint the dictionary inside itself. For the truly
mechanical bits — "here's the exact procedure to check every code block compiles" — it says *"use the
checklist in the back"*, a **bundled script** the worker runs only when they reach that step
(Chapter 4). The guide stays short and readable; the tedious machinery lives in an appendix you flip
to on demand.

| | Re-brief every writer each time | One giant manual every writer must read cover-to-cover | **Pinned house-style guide (a skill)** |
|---|---|---|---|
| Cost to start a page | High — someone re-explains the rules | High — read everything, even irrelevant parts | **~Zero — glance at the pinned guide** |
| Consistency across writers | Drifts; each re-derivation differs | Good, if anyone actually reads it all | **High — one source, loaded on a match** |
| Shows up only when relevant | N/A (human has to ask) | No — always in the way | **Yes — only when authoring Cortex content** |
| Mechanical procedures | Re-explained or skipped | Inline, bloating the manual | **Pointed to as a bundled script (L3)** |

## 3. Formal Definition

We are designing one concrete artifact: a directory `workbench-author/` whose required file is
`SKILL.md` (Chapter 2 anatomy). Here is the design, decision by decision.

**The `name`: `workbench-author`.** A skill's name should read like a job title — short, lowercase,
hyphenated, and *descriptive of the task*, because the name is part of the always-loaded Level-1
metadata. `workbench-author` says exactly what the skill is *for* (authoring workbench/chapter
content) without naming the mechanism. We avoid cute names (`scribe`) — a future agent scanning the
skill catalog should infer the job from the name alone.

**The `description`: WHAT it does + WHEN to use it.** This is the single most important line in the
file, because **the description is the trigger** (Chapter 3): the model reads it at Level 1 and
*decides* whether to load the body. So it must name the action *and* the unambiguous condition. Ours:

> *"Author or convert a Cortex chapter or workbench exercise to house format. Use when writing or
> editing files under `content/cortex/` — the 9-section chapter template, the testcases/quiz/solution
> fences, collapsible `<details>` answers, and how to verify every runnable block byte-for-byte via
> go-judge `/api/run`."*

Note the structure: a WHAT clause ("Author or convert … to house format"), an explicit WHEN trigger
("Use when writing or editing files under `content/cortex/`"), and a tail listing the concrete cues a
matching task would mention (template, fences, `<details>`, verify, `/api/run`). Those cue words are
deliberate — they raise the odds the description matches when an agent is genuinely about to author
content, and *don't* match on unrelated work.

**The body: the conventions, lean.** The Level-2 body holds the house rules an author needs *in their
head while writing* — the 9-section skeleton, the collapsible-answer format, the quiz JSON schema, and
the verification rule. It is **prose, not a program**: instructions and a couple of tiny shape
examples, kept short on purpose (progressive disclosure means the body loads in full every time the
skill fires, so every wasted paragraph is a recurring tax).

**The bundled script: the mechanical step.** One task in the contract is purely *mechanical*: find
every ````python run```` block, submit each to `/api/run`, and require **Accepted**. That doesn't
belong as prose in the body — it belongs as a **Level-3 bundled script**, `scripts/verify_run.py`,
which the body merely *points to*. The body says *what* and *when*; the script does the *how*, and
loads only when an author reaches the verification step.

| Term | Meaning in this skill |
|---|---|
| **`workbench-author`** | The skill's `name`; the directory holding `SKILL.md` and `scripts/` |
| **Description-as-trigger** | The `description` line the model reads at L1 to decide whether to load the body (Ch. 3) |
| **9-section skeleton** | The fixed chapter order: TL;DR → Motivation → Intuition → Formal → Worked Example → Build It → Trade-offs → Edge Cases → Practice → quiz → In the Wild → Next |
| **Collapsible answer** | A `<details><summary><strong>Answer</strong></summary>` block after each exercise, with blank lines around the inner markdown |
| **`quiz` schema** | One ```quiz``` JSON object: `prompt`, `input`, `options` (≥2), `answer` (∈ `options`) |
| **Verification rule** | Every ````python run```` block must return **Accepted** from go-judge `/api/run`; scalafmt any touched Scala |
| **`scripts/verify_run.py`** | The bundled L3 script that extracts and submits every runnable block |
| **The gap** | This skill is *designed here*, **not installed** in the Cortex repo |

> The one line to remember: **we put the *conventions* in the body and the *mechanical verification*
> behind a bundled script — and we lead with a description precise enough to fire exactly when an agent
> is about to author Cortex content, and never otherwise.**

## 4. Worked Example

First, watch the skill load during a real task. An agent is asked to *"add a chapter to the Skills
part of The Claude Stack."* It never read this book; it only sees the Level-1 catalog.

```mermaid
flowchart TD
  T([Task: "add a chapter under<br/>content/cortex/the-claude-stack/…"]) --> L1
  L1["**L1 — always loaded**<br/>workbench-author: 'Author or convert a Cortex<br/>chapter… Use when editing files under content/cortex/…'"]
  L1 -->|task path matches<br/>'content/cortex/'| L2["**L2 — loads now**<br/>the SKILL.md body:<br/>9-section skeleton · collapsible answers ·<br/>quiz schema · verification rule"]
  L2 -->|author reaches the<br/>'verify every block' step| L3["**L3 — loads if needed**<br/>scripts/verify_run.py<br/>(extract + submit each block)"]
  L1 -.->|unrelated task →<br/>never loads| X[("body + script<br/>stay on the shelf")]
  classDef s fill:#1f2937,stroke:#6366f1,color:#e5e7eb,rx:6,ry:6;
  class L1,L2,L3 s;
```

On a task that has nothing to do with content, `workbench-author` costs only its description. The
moment the task is "edit a file under `content/cortex/`", the description matches, the body loads, and
the agent suddenly knows the whole contract — then pulls in `verify_run.py` only when it reaches the
verification step. That's the entire value proposition: the conventions are *absent until relevant,
then fully present.*

Now the artifact itself. Here is the `SKILL.md` we would ship — frontmatter, then a lean body. (This
is the exact draft our §5 linter parses and checks.)

```markdown
---
name: workbench-author
description: "Author or convert a Cortex chapter or workbench exercise to house
  format. Use when writing or editing files under content/cortex/ - the 9-section
  chapter template, the testcases/quiz/solution fences, collapsible <details>
  answers, and how to verify every runnable block byte-for-byte via go-judge
  /api/run."
---

# workbench-author

Write Cortex content so it renders and verifies on the first try.

## The 9-section chapter skeleton

Every chapter file is frontmatter (title + summary), then # Title, then IN ORDER:
TL;DR (blockquote) -> 1. Motivation -> 2. Intuition (Analogy) ->
3. Formal Definition -> 4. Worked Example -> 5. Build It ->
6. Trade-offs -> 7. Edge Cases & Failure Modes -> 8. Practice ->
one quiz block -> In the Wild -> a final Next: link.

## Collapsible answers

Each Practice exercise is followed by a <details><summary><strong>Answer</strong>
</summary> ... </details> block. Leave a BLANK LINE after </summary> and a BLANK
LINE before </details> or the inner markdown will not render.

## The quiz JSON schema

Exactly one quiz fence per chapter. Its body is one JSON object with keys
prompt, input, options (an array of 2 or more strings), and answer. The answer
string MUST be character-for-character equal to one of the options.

## Verification rule

Run every python-run block through go-judge /api/run and require an Accepted
verdict before shipping. Run scalafmt on any touched Scala. See the bundled
script scripts/verify_run.py to extract and submit every block.
```

A few authoring choices worth calling out. The body is **four short sections**, not a transcript of
this chapter — it's the *checklist a writer needs*, nothing more (lean L2). Each rule is stated
operationally ("leave a BLANK LINE … or the inner markdown will not render") so an agent knows the
*failure mode*, not just the rule. And the verification section ends by **delegating** to
`scripts/verify_run.py` rather than inlining the extraction logic — that's the L2→L3 hand-off in
practice. (One self-referential nicety: inside a `SKILL.md` you'd write the fence as the literal three
backticks; here the rule reads "quiz fence" / "python-run block" because we're *showing* the file
inside another markdown file and can't nest the fences.)

## 5. Build It

We can't run a skill here, but we *can* prove our draft is correct — by writing a **linter for it**.
The drafted `SKILL.md` is embedded as a string; we parse the frontmatter and body, then assert the
rules from §3: the `name` is exactly `workbench-author`, the `description` carries the trigger cues
(`content/cortex`, `quiz`, `verify`), and the body mentions each required convention (the 9 sections,
collapsible answers, the quiz schema, `/api/run`). Then we run the same linter on a **broken** draft
with the verification section deleted, and watch it pinpoint the gap.

```python run
"""A linter for the workbench-author SKILL.md we drafted in this chapter.

It treats a SKILL.md as a string, parses the YAML-ish frontmatter and body,
and asserts the rules that make THIS skill correct:
  - name is exactly "workbench-author"
  - the description carries trigger cues (so the model knows WHEN to load it):
    "content/cortex", "quiz", "verify"
  - the body mentions the required house conventions:
    the 9-section skeleton, collapsible <details> answers,
    the quiz JSON schema, and the /api/run verification rule.

Deterministic, stdlib only. No network, no /api/run.
"""

# The ACTUAL drafted skill, embedded as a string (this is what ch.6 ships).
# Note: we build the quiz-fence marker from a variable so no literal
# triple-backtick ever appears inside this Python source.
FENCE = chr(96) * 3  # three backticks

GOOD_SKILL = """---
name: workbench-author
description: "Author or convert a Cortex chapter or workbench exercise to house
  format. Use when writing or editing files under content/cortex/ - the 9-section
  chapter template, the testcases/quiz/solution fences, collapsible <details>
  answers, and how to verify every runnable block byte-for-byte via go-judge
  /api/run."
---

# workbench-author

Write Cortex content so it renders and verifies on the first try.

## The 9-section chapter skeleton

Every chapter file is frontmatter (title + summary), then # Title, then IN ORDER:
TL;DR (blockquote) -> 1. Motivation -> 2. Intuition (Analogy) ->
3. Formal Definition -> 4. Worked Example -> 5. Build It ->
6. Trade-offs -> 7. Edge Cases & Failure Modes -> 8. Practice ->
one quiz block -> In the Wild -> a final Next: link.

## Collapsible answers

Each Practice exercise is followed by a <details><summary><strong>Answer</strong>
</summary> ... </details> block. Leave a BLANK LINE after </summary> and a BLANK
LINE before </details> or the inner markdown will not render.

## The quiz JSON schema

Exactly one quiz fence per chapter. Its body is one JSON object with keys
prompt, input, options (an array of 2 or more strings), and answer. The answer
string MUST be character-for-character equal to one of the options.

## Verification rule

Run every python-run block through go-judge /api/run and require an Accepted
verdict before shipping. Run scalafmt on any touched Scala. See the bundled
script scripts/verify_run.py to extract and submit every block.
"""

# A BROKEN draft: same skill but with the Verification section removed.
BROKEN_SKILL = GOOD_SKILL.replace(
    """## Verification rule

Run every python-run block through go-judge /api/run and require an Accepted
verdict before shipping. Run scalafmt on any touched Scala. See the bundled
script scripts/verify_run.py to extract and submit every block.
""",
    "",
)


def parse_skill(text):
    """Split a SKILL.md string into (frontmatter_dict, body). Tiny YAML subset:
    only top-level `key: value`, with values that may wrap onto indented lines."""
    lines = text.splitlines()
    assert lines and lines[0].strip() == "---", "must open with --- frontmatter"
    end = next(i for i in range(1, len(lines)) if lines[i].strip() == "---")
    front, body = {}, "\n".join(lines[end + 1:])
    key = None
    for raw in lines[1:end]:
        if raw and not raw[0].isspace() and ":" in raw:
            key, _, val = raw.partition(":")
            key = key.strip()
            front[key] = val.strip()
        elif key is not None and raw.strip():
            front[key] = (front[key] + " " + raw.strip()).strip()
    # Strip one layer of surrounding quotes from values.
    for k, v in front.items():
        if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
            front[k] = v[1:-1]
    return front, body


# What every correct workbench-author skill must contain.
TRIGGER_CUES = ["content/cortex", "quiz", "verify"]
BODY_RULES = {
    "9-section skeleton": ["Motivation", "Formal Definition", "In the Wild"],
    "collapsible answers": ["<details>", "</summary>", "BLANK LINE"],
    "quiz JSON schema": ["options", "answer", "equal to one of the options"],
    "/api/run verification": ["/api/run", "Accepted"],
}


def lint(label, text):
    front, body = parse_skill(text)
    name = front.get("name", "")
    desc = front.get("description", "")
    gaps = []

    # 1. name must be exact.
    name_ok = name == "workbench-author"
    if not name_ok:
        gaps.append('name is "%s", expected "workbench-author"' % name)

    # 2. description must carry every trigger cue (case-insensitive).
    low = desc.lower()
    cue_ok = {c: (c.lower() in low) for c in TRIGGER_CUES}
    for c, ok in cue_ok.items():
        if not ok:
            gaps.append('description missing trigger cue "%s"' % c)

    # 3. body must mention each required convention (all its markers present).
    rule_ok = {}
    for rule, markers in BODY_RULES.items():
        ok = all(m in body for m in markers)
        rule_ok[rule] = ok
        if not ok:
            missing = [m for m in markers if m not in body]
            gaps.append('body omits "%s" (missing: %s)' % (rule, ", ".join(missing)))

    print("== linting: %s ==" % label)
    print("  [%s] name == workbench-author" % ("PASS" if name_ok else "FAIL"))
    for c in TRIGGER_CUES:
        print("  [%s] description cue: %s" % ("PASS" if cue_ok[c] else "FAIL", c))
    for rule in BODY_RULES:
        print("  [%s] body convention: %s" % ("PASS" if rule_ok[rule] else "FAIL", rule))

    if gaps:
        print("  RESULT: FAIL - %d gap(s):" % len(gaps))
        for g in gaps:
            print("    - " + g)
    else:
        print("  RESULT: PASS - skill is house-format compliant")
    return not gaps


# A quick sanity check that the embedded skill really has exactly one quiz fence
# is something the real bundled verify_run.py would do; we model the idea here.
def count_quiz_fences(body_with_fences):
    return body_with_fences.count(FENCE + "quiz")


if __name__ == "__main__":
    good = lint("workbench-author (drafted)", GOOD_SKILL)
    print()
    broken = lint("workbench-author (broken: verification removed)", BROKEN_SKILL)
    print()
    # The good skill must pass; the broken one must fail on the verification rule.
    assert good is True, "the drafted skill should lint clean"
    assert broken is False, "the broken skill should be caught"
    print("linter self-check: good=PASS, broken=FAIL (verification gap caught) -> OK")
```

**Now break it further.** Delete `"content/cortex"` from `TRIGGER_CUES` and the broken draft's
description suddenly "passes" the cue check — exactly the Chapter 3 failure where a vague description
never fires (the skill would sit unused). Or change `BROKEN_SKILL` to also drop the `## Collapsible
answers` section, and the linter reports a *second* gap. The point: a skill is **code we trust an agent
to follow**, so it deserves the same verification rigor we demand of the chapters it produces — which
is why the linter's last act is to assert the good draft passes *and* the broken one fails. If either
assertion broke, the script would exit non-zero and you'd know the draft regressed.

## 6. Trade-offs & Complexity

| Ship `workbench-author` as a skill | Put the rules in the system prompt / CLAUDE.md | Enforce with a CI script only |
|---|---|---|
| Loads only when authoring content (cheap at rest) | Always-on tax on every unrelated turn | No authoring guidance at all — catches errors *after* the fact |
| One source of truth, versioned as a folder | Drifts across prompts; easy to fork accidentally | Rules live in test code, not author-facing |
| Agent *knows the rules while writing* | Agent knows them, but pays always | Agent writes blind, fails CI, loops |
| Mechanical step delegated to a bundled script (L3) | Script logic bloats the prompt | The script *is* the whole thing; no prose guidance |
| **Cost:** must be installed + maintained; a vague description mis-fires | Cheap to add; expensive to carry | Cheap to run; poor authoring DX |

The skill wins because the authoring contract is **needed at write-time, by the author, only on
content tasks** — precisely the progressive-disclosure sweet spot. Its costs are the usual ones
(Chapter 1): it depends on a good description, and it has to be *maintained* — a stale rule makes the
agent confidently wrong. A CI linter (like our §5 script, productionized) is **complementary, not a
substitute**: the skill prevents errors at authoring time; CI catches whatever still slips through.
The real, present cost here is blunter: **the skill isn't installed.** Until it is, every agent keeps
re-deriving the contract from examples — which is the gap this chapter makes concrete.

## 7. Edge Cases & Failure Modes

- **The skill doesn't exist yet (the headline gap).** This chapter *designs* `workbench-author`; the
  Cortex repo does **not** ship it. Designing without installing yields zero benefit at authoring time
  — closing that gap (commit `workbench-author/SKILL.md` + `scripts/verify_run.py`) is the actual
  follow-through.
- **Vague description → never fires.** Drop the `content/cortex/` trigger and the model won't load the
  skill when authoring, stranding the conventions (Chapter 3). Our linter guards exactly this by
  asserting the cue is present.
- **Over-broad description → always fires.** If the description said "use when writing any markdown",
  it would load on unrelated docs and reintroduce the bloat skills exist to avoid. Scope the trigger to
  `content/cortex/`.
- **Rules rot.** The repo's conventions evolve (slugs went hierarchical; `/api/run` is currently down,
  so blocks are verified on host `python3` as a stand-in). A skill that still teaches the old rule makes
  the agent confidently wrong — skills need maintenance like any code.
- **Body bloat defeats the point.** Pasting this entire chapter into the body means paying its full
  cost on every content task. Keep L2 to the checklist; push mechanics to the L3 script.
- **Packaged an action as instructions.** "Run every block through `/api/run`" is an *action*. The
  skill can only *say* to do it and point at a script; it can't perform the HTTP call itself. The doing
  belongs to the bundled script (or a tool/MCP) — the skill carries the *know-how*, not the capability
  (Chapter 5).

## 8. Practice

> **Exercise 1 — Justify the trigger.** Our description says *"Use when writing or editing files under
> `content/cortex/`."* Explain (a) why scoping the trigger to that path is better than "use when
> writing documentation", and (b) which §5 linter check would fail if someone deleted the
> `content/cortex` cue from the description — and what real-world symptom that deletion would cause.

<details>
<summary><strong>Answer</strong></summary>

The description **is the trigger** (Chapter 3): it's read at Level 1 and decides whether the body
loads, so its scope directly controls *when* the skill fires.

- **(a)** `content/cortex/` is the **exact, unambiguous condition** under which these conventions
  apply — that path *is* the book. "Use when writing documentation" is over-broad: it would match
  READMEs, code comments, design docs, any prose anywhere, loading the 9-section/quiz/verify
  conventions where they're irrelevant. That reintroduces the always-on bloat skills exist to avoid
  (the §6 over-broad failure mode) and erodes the model's trust in the trigger. A precise path scopes
  the skill to fire when it's genuinely useful and stay silent otherwise.
- **(b)** The `description cue: content/cortex` check would flip to **FAIL** (it's the first entry in
  `TRIGGER_CUES`). The real-world symptom is the **vague-description failure**: an agent authoring a
  chapter wouldn't have a clear path-based reason to load the skill, so the conventions stay on the
  shelf and the agent re-derives (and mis-applies) the house format — the exact problem the skill was
  built to prevent.

</details>

> **Exercise 2 — Body or bundled script?** For each item, decide whether it belongs in the `SKILL.md`
> **body** (L2) or in the bundled `scripts/verify_run.py` (L3), and give the one-line principle: (a)
> "the chapter sections must appear in this exact order"; (b) "scan the finished file, extract each
> runnable python block, POST it to `/api/run`, and fail unless the verdict is Accepted"; (c) "the
> quiz `answer` must be one of the `options`."

<details>
<summary><strong>Answer</strong></summary>

The principle (Chapter 4, restated in §3): the **body** holds *conventions the author needs in their
head while writing*; the **bundled script** holds *mechanical procedures executed at one specific
step*. Prose you read → L2; a procedure you run → L3.

- **(a) Section order → body (L2).** It's a rule the author must *know while drafting* every section;
  it shapes the writing itself. Stating it costs a line and there's nothing to "run". Body.
- **(b) Extract-and-submit each block → bundled script (L3).** This is a concrete, repeatable
  *procedure* (parse the file, find the blocks, make HTTP calls, check verdicts) invoked only when the
  author reaches the verification step. That's `scripts/verify_run.py`; the body merely *points* to it.
- **(c) `answer` ∈ `options` → body (L2).** It's a structural *rule of the quiz format* the author
  applies while writing the quiz JSON — like (a), a convention, not a procedure. (A productionized
  `verify_run.py` could *also* check it mechanically, but the rule the author follows lives in the
  body.)

The throughline: (a) and (c) are *rules you write by*; (b) is a *procedure you run*. Rules → body;
runnable procedure → bundled script.

</details>

> **Exercise 3 — Name the gap, then close it.** A teammate says: "Great, so Cortex now auto-formats
> every chapter via the `workbench-author` skill." In two or three sentences, correct them: what is the
> actual status of this skill, why does the design alone produce *zero* benefit at authoring time, and
> what two concrete files would have to be committed to change that?

<details>
<summary><strong>Answer</strong></summary>

The teammate is wrong about the status. This chapter **designs** `workbench-author` — we wrote the
`SKILL.md` and even linted it — but **Cortex does not ship it**; it isn't installed anywhere the model
loads skills from (the headline gap of §1, §6, §7).

A skill only helps when its description is in the agent's Level-1 catalog so it can *fire* on a
matching task. A design that lives only inside this chapter is never in any catalog, so it loads for no
one and changes no authoring behavior — benefit is exactly zero until it's installed.

To close the gap you'd commit the actual artifact: **`workbench-author/SKILL.md`** (frontmatter + the
lean body we drafted) and its **`scripts/verify_run.py`** (the L3 verifier the body points to), placed
where this project's agents discover skills. Only then does "Cortex auto-formats chapters via the
skill" become true.

</details>

```quiz
{
  "prompt": "In the workbench-author skill, where should the procedure \"find every runnable python block in the finished chapter and submit each to /api/run, requiring an Accepted verdict\" live?",
  "input": "Choose one:",
  "options": [
    "As a bundled Level-3 script (scripts/verify_run.py) that the SKILL.md body points to, loaded only when the author reaches the verification step",
    "Inlined in full in the SKILL.md description, so it's always in context",
    "Pasted verbatim into the body of every chapter the skill produces",
    "Nowhere — verification can't be part of a skill at all"
  ],
  "answer": "As a bundled Level-3 script (scripts/verify_run.py) that the SKILL.md body points to, loaded only when the author reaches the verification step"
}
```

## In the Wild

- **[Anthropic Skills — `skill-creator` and the official examples](https://github.com/anthropics/skills)** —
  Anthropic's own repository of skills, including a `skill-creator` skill whose entire job is authoring
  *other* skills. The closest public analog to what we built here: a `SKILL.md` that encodes
  conventions for producing well-formed output.
- **[Claude Docs — Agent Skills authoring guide](https://docs.claude.com/en/docs/agents-and-tools/agent-skills)** —
  the authoritative spec for the `SKILL.md` format, the `name`/`description` frontmatter, bundling
  scripts and resources, and where skills are discovered — the rules our `workbench-author` design
  follows.
- **[The real `graphify` skill in this repo](https://github.com/ani2fun/cortex)** —
  `~/.claude/skills/graphify/SKILL.md`: a shipping, installed skill (a 55 KB body behind a one-line
  description, with a bundled CLI it shells out to). The living example of the structure
  `workbench-author` imitates — and a reminder of what "installed" looks like, which our skill is not.

---

**Next:** we've designed a skill — now how do skills *travel*? Versioning, distributing them inside
plugins, and the trust question every third-party skill raises (a skill is instructions an agent will
follow — so who wrote it?). →
[7. Sharing & governance](/cortex/the-claude-stack/agent-skills/sharing-and-governance)