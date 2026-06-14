---
title: '4. Hooks'
summary: The model is probabilistic and might forget; a hook is code the harness guarantees to run at a lifecycle event — so "please remember to rebuild the index" becomes "the index always gets rebuilt," every single time.
---

# 4. Hooks

## TL;DR

> **A hook is a shell command the *harness* runs automatically at a lifecycle event** — before a
> tool runs, after a tool runs, when you submit a prompt, when the agent stops. The first-principle
> reason hooks exist: the model's behaviour is **probabilistic** — it *might* format the file, it
> *might* remember to rebuild the index, it might not. A hook is **deterministic code the harness
> promises to run**, no asking the model nicely. Use hooks for the things that *must always happen*:
> auto-format on save, rebuild a derived file, **block a dangerous command** (a `PreToolUse` hook can
> veto), inject context. Hooks turn *"please remember to X"* into *"X happens, guaranteed."* This
> repo has a real one: every time the agent writes a book chapter, the book index rebuilds itself.

## 1. Motivation

This book has a search index — a generated file that lists every chapter so the site can build its
table of contents. It is a **derived artifact**: whenever a chapter's content changes, the index is
stale until something regenerates it by running `python3 tools/gen_cortex_index.py`.

Now picture authoring fifty chapters with an agent. After *every* `Write` you'd have to remember to
say "...and rebuild the index." Miss it once and the site quietly serves a stale table of contents —
a bug nobody notices until a reader can't find a chapter. You could put "always rebuild the index
after editing content" in `CLAUDE.md` (Chapter 2), and the model would *usually* comply. But
"usually" is the problem. The model is a **probabilistic** thing: most turns it remembers, some turns
it's deep in a refactor and forgets, and you have no guarantee. A derived artifact being correct
*most of the time* is the same as it being **unreliable**.

So this repo doesn't ask the model at all. Its `.claude/settings.json` registers a **`PostToolUse`
hook** with matcher `Write|Edit`: every time the agent writes or edits a file, the harness reads the
file path, checks `grep -q 'content/cortex/'`, and if it matches, runs the index rebuild — *itself,
in code*. That hook fired on **every single chapter of this book** as it was saved, including this
one. Nobody remembered to rebuild the index, ever, because remembering was never the model's job. The
harness did it deterministically. That is the entire pitch of hooks: **take the thing that must
always happen, and make it impossible to forget by removing the model from the decision.**

## 2. Intuition (Analogy)

A hook is a **reflex**. Tap your knee and your leg kicks — there's no deliberation, no "should I
kick?", no chance you *forget* to kick because you were distracted. The signal arrives, the response
fires, every time. Compare that to a *decision*: "I'll stretch later" is something you can fully
intend and still never do. The model's good intentions are decisions; a hook is a reflex.

Other faithful versions of the same idea: an **automatic-door sensor** (you don't *ask* the door to
open — it opens when you cross the beam), a **factory safety interlock** (the press won't cycle while
your hand is in it, no matter what the operator remembers), or a **spell-checker that runs on every
save** whether you invoked it or not.

| | Asking the model (prompt / CLAUDE.md) | **A hook** |
|---|---|---|
| Who decides to run it | The model, each turn | The **harness**, automatically |
| Reliability | *Probabilistic* — usually, not always | **Deterministic** — every matching event |
| Can it be forgotten? | Yes (busy, long context, distraction) | **No** — it's code, not a choice |
| Can it **block** an action? | No (advice only) | **Yes** (`PreToolUse` can veto) |
| Best for | Judgement, nuance, "prefer X" | Invariants: "X *must* happen" |
| Failure looks like | Silent skip, stale artifact | Loud + consistent (it ran, see its output) |

The dividing line is **must vs. should**. "Prefer descriptive names" is a *should* — leave it to the
model. "The index is never stale" and "never `git push --force` to main" are *musts* — wire them to a
hook, because a must you only get "usually" is not a must at all.

## 3. Formal Definition

A **hook** is a shell command the harness executes automatically when a **lifecycle event** fires,
optionally gated by a **matcher**. It is configured in `settings.json`, not in the model's prompt —
the model neither writes nor invokes it.

The lifecycle **events** (the moments the harness can fire a hook):

| Event | Fires… | Typical use |
|---|---|---|
| `PreToolUse` | *before* a tool call runs | **Guardrail** — validate or **block** the action (e.g. veto a dangerous command). |
| `PostToolUse` | *after* a tool call succeeds | **React** — format the file, rebuild a derived artifact, lint. |
| `UserPromptSubmit` | when you send a message | **Inject context** — add a timestamp, branch name, reminder. |
| `Stop` | when the agent finishes its turn | Notify, run a final check, log. |
| `SessionStart` / `SubagentStop` | session begins / a subagent returns | Setup; post-process subagent output. |

| Term | Meaning |
|---|---|
| **Hook** | A shell command bound to an event; the harness runs it, not the model. |
| **Lifecycle event** | A named moment in the loop (`PreToolUse`, `PostToolUse`, …) the harness can fire on. |
| **Matcher** | A filter on *which tools* trigger the hook, e.g. `"Write\|Edit"` — fire only on Write or Edit, ignore Read/Bash. |
| **stdin payload** | The harness pipes the tool call's JSON to the hook on **stdin**; the hook reads it (e.g. `.tool_input.file_path` via `jq`) to act on the specifics. |
| **Veto (block)** | A `PreToolUse` hook can signal "do not run this action" — the harness then **skips** the tool call. `PostToolUse` cannot (the action already happened). |
| **Deterministic** | Same event → same command runs, every time. No sampling, no forgetting. |

> The one sentence to keep: **a hook is deterministic code the harness guarantees to run at a
> lifecycle event — turning "please remember to X" into "X always happens."** `PreToolUse` runs
> *before* and can **block**; `PostToolUse` runs *after* and **reacts**.

Two properties do all the work. First, **the harness runs it, not the model** — so it's outside the
probabilistic core; it can't be talked out of, forgotten, or skipped under load. Second, **it gets the
tool's JSON on stdin** — so it isn't blind; it can read *which file* was written and decide whether to
act, which is exactly how the real index hook checks for `content/cortex/` before doing anything.

## 4. Worked Example — the real index hook

Here is this repo's actual `PostToolUse` hook, lightly annotated:

```json
{ "matcher": "Write|Edit",
  "hooks": [{ "type": "command",
    "command": "jq -r '.tool_input.file_path // empty' | grep -q 'content/cortex/' && cd <repo> && python3 tools/gen_cortex_index.py 2>/dev/null || true" }] }
```

Read it left to right as a pipeline the harness runs *after every Write/Edit*: `jq` pulls
`file_path` out of the **stdin payload** → `grep -q` asks "is it under `content/cortex/`?" → **only if
so**, `cd` to the repo and rebuild the index. A `Write` to `README.md` flows through, `grep` finds no
match, and the chain stops — the hook fired but did nothing, which is correct. A `Write` to a chapter
matches, and the index rebuilds. The trailing `|| true` means the hook never *fails the turn*: even if
the rebuild errors, it swallows the error so your edit still stands.

```mermaid
flowchart TD
  M["model proposes<br/>Write content/.../04-hooks.md"] --> H{{"harness"}}
  H -->|PreToolUse hooks| PRE["guardrail check<br/>(none block → proceed)"]
  PRE --> EX["harness executes the Write<br/>file lands on disk"]
  EX -->|PostToolUse matcher Write\|Edit| HK["run hook command<br/>stdin = tool JSON"]
  HK --> JQ["jq → file_path"]
  JQ --> GREP{"under<br/>content/cortex/ ?"}
  GREP -->|yes| IDX["python3 gen_cortex_index.py<br/>index rebuilt"]
  GREP -->|no| SKIP["chain stops<br/>hook did nothing"]
  IDX --> BACK["result returns to model"]
  SKIP --> BACK
  classDef s fill:#1f2937,stroke:#6366f1,color:#e5e7eb,rx:6,ry:6;
  class M,PRE,EX,HK,JQ,IDX,SKIP,BACK s;
```

Notice what the model sees: **nothing extra**. It asked to write a file; the file got written; the
index silently became correct as a side effect. The agent never reasoned about the index, never spent
a token on it, *can't* forget it. The contrast with putting "rebuild the index" in `CLAUDE.md` is the
whole lesson — that's advice the model follows probabilistically; this is machinery that fires
deterministically.

The repo also keeps a **second** real-world pattern in its toolbox: a *git-guardrails* skill that
installs a **`PreToolUse` hook** which inspects `Bash` commands and **blocks** dangerous ones
(`git push --force`, `git reset --hard`, `git clean`, `branch -D`) *before* they run. Same mechanism,
opposite direction: `PostToolUse` reacts after the fact; `PreToolUse` stands in front of the action
and can **veto** it. One automates a chore; the other prevents a catastrophe.

## 5. Build It

We can't run the real harness here, but a hook system is just **an event dispatcher**: register
commands under events, and when an event fires, run every command whose matcher matches. The model is
never consulted. Below we register a `PostToolUse` hook that "rebuilds the index" for any write under
`content/`, and a `PreToolUse` hook that **blocks** writes to a protected path. Then we simulate four
tool calls and watch the hooks fire deterministically — index rebuilt every matching time, protected
write vetoed.

```python run
HOOKS = {}  # event -> list of (matcher, name, fn)

def register(event, matcher, name, fn):
    HOOKS.setdefault(event, []).append((matcher, name, fn))

PROTECTED = "content/cortex/LOCKED.md"

def index_rebuild(event, tool, payload):
    if "content/" in payload["file_path"]:
        return ("ran", f"index rebuilt for {payload['file_path']}")
    return ("skip", "path not under content/, hook did nothing")

def protect_path(event, tool, payload):
    if payload["file_path"] == PROTECTED:
        return ("block", f"BLOCKED write to protected {PROTECTED}")
    return ("allow", "write permitted")

register("PostToolUse", "Write|Edit", "rebuild-index", index_rebuild)
register("PreToolUse", "Write", "git-guardrail", protect_path)

def dispatch(event, tool, payload):
    """The HARNESS runs every matching hook. The model is never asked."""
    fired, blocked = [], False
    for matcher, name, fn in HOOKS.get(event, []):
        if tool not in matcher.split("|"):
            continue
        outcome, msg = fn(event, tool, payload)
        fired.append((name, outcome, msg))
        if outcome == "block":
            blocked = True
    return fired, blocked

calls = [
    ("Write", {"file_path": "content/cortex/the-claude-stack/04-hooks.md"}),
    ("Edit",  {"file_path": "content/cortex/the-claude-stack/01-intro.md"}),
    ("Write", {"file_path": "README.md"}),
    ("Write", {"file_path": PROTECTED}),
]

rebuilds = 0
for tool, payload in calls:
    pre, blocked = dispatch("PreToolUse", tool, payload)
    for name, outcome, msg in pre:
        print(f"PreToolUse  {name:14} [{outcome:5}] {msg}")
    if blocked:
        print(f"  -> {tool} {payload['file_path']} VETOED; harness skips it\n")
        continue
    print(f"  -> {tool} {payload['file_path']} executed")
    post, _ = dispatch("PostToolUse", tool, payload)
    for name, outcome, msg in post:
        print(f"PostToolUse {name:14} [{outcome:5}] {msg}")
        if outcome == "ran":
            rebuilds += 1
    print()

print(f"index rebuilt {rebuilds} times, deterministically; 1 write vetoed")
```

Three things to see in the output. **The index rebuilds twice** — for the chapter `Write` and the
chapter `Edit` — without anyone asking; the `README.md` write matches the matcher but the hook's own
`content/` check makes it a no-op, exactly like the real `grep -q`. **The `Edit` skips the
guardrail** — its matcher is `"Write"` only, so the `PreToolUse` check never even runs for an Edit;
matchers decide *which tools trigger a hook*. And **the protected write is vetoed in `PreToolUse`**,
so the harness skips it and never reaches `PostToolUse` — the guardrail stood in front of the action.
Run it again: identical output. That sameness *is* determinism — the property a prompt can't promise.

## 6. Trade-offs & Complexity

| | A hook (deterministic) | Asking the model (prompt / CLAUDE.md) | Doing it manually |
|---|---|---|---|
| Reliability | **Always** fires on the event | *Usually* — probabilistic | Only when you remember |
| Cost | ~free; runs out-of-band, no tokens | Spends context/tokens each turn | Your time, every time |
| Flexibility | Rigid — it's fixed code | **Adapts** with judgement | Total, but slow |
| Can block harm | **Yes** (`PreToolUse` veto) | No (advice only) | N/A |
| Failure mode | Silent if mis-scoped / wrong matcher | Silent skip, stale result | Forgotten entirely |
| Best for | **Invariants** that must always hold | Nuanced, case-by-case behaviour | One-off / irreversible calls |

The cost of a hook is its rigidity: it runs *exactly* what you wrote, with zero judgement. That's a
feature for invariants ("index never stale") and a footgun for anything needing nuance — a too-broad
hook that reformats every file on every save can fight the model, churn diffs, or slow each turn. The
discipline is **scope tightly** (precise matcher, a guard inside the command like the real `grep`) and
reserve hooks for genuine *musts*. Everything judgement-shaped belongs in the prompt, not a hook.

## 7. Edge Cases & Failure Modes

- **Matcher too broad / too narrow.** Matching `*` reformats unrelated files and fights the agent;
  matching `Write` but not `Edit` misses half the edits (see the Build It). Match the exact tools, and
  guard *inside* the command (the real hook's `grep -q 'content/cortex/'`).
- **A failing hook breaking the turn.** If a `PostToolUse` command exits non-zero it can surface as an
  error. The real hook ends with `2>/dev/null || true` precisely so a rebuild hiccup never fails the
  edit. Decide deliberately whether a hook *should* be allowed to fail loudly or swallow errors.
- **Silent no-op mistaken for "it ran."** A hook that matched but whose inner guard skipped looks a lot
  like a hook that fired — both finish quietly. When debugging, check the hook actually did work, not
  just that it was invoked.
- **`PostToolUse` can't undo.** It runs *after* the action; it cannot prevent a bad write — only react.
  Anything that must be *stopped* belongs in `PreToolUse`, the only event that can veto.
- **Slow hooks tax every turn.** A heavy command (full build, network call) on a hot event adds latency
  to *every* matching tool call. Keep hooks cheap, or scope them to rare events.
- **Non-determinism leaking back in.** A hook that shells out to something flaky (network, a service
  that may be down) reintroduces the very unreliability hooks exist to remove. Prefer local, pure,
  fast commands so "guaranteed to run" also means "guaranteed to behave."

## 8. Practice

> **Exercise 1 — Prompt or hook?** Your team rule is "every committed `.scala` file must be
> scalafmt-clean." A teammate adds *"always run scalafmt before finishing"* to `CLAUDE.md` and calls it
> solved. From first principles, why is that not equivalent to a hook — and which event would you wire
> it to?

<details>
<summary><strong>Answer</strong></summary>

A `CLAUDE.md` line is **advice to a probabilistic actor**: the model reads it and *usually* complies,
but a busy turn or a long context can crowd it out, and nothing guarantees it runs (§2). "Usually
formatted" means the invariant doesn't actually hold — some commits slip through unformatted, which is
the same as having no rule.

A hook removes the model from the decision: the **harness** runs the formatter itself, deterministically,
on a lifecycle event (§3). For "every file is formatted after it changes," wire a **`PostToolUse`** hook
with matcher `Write|Edit` that runs scalafmt on the written path — it fires after the write lands and
fixes formatting as a side effect, every time, no remembering. (If instead you wanted to *block* an
unformatted commit, that's a `PreToolUse` guard on the `git commit` Bash call — the veto direction.)
The principle: **musts go to hooks, shoulds stay in the prompt.**

</details>

> **Exercise 2 — `PreToolUse` vs `PostToolUse`.** You want to stop `git push --force` from ever
> running. Could a `PostToolUse` hook achieve this? Which event must you use, and what capability of
> that event makes it the only option?

<details>
<summary><strong>Answer</strong></summary>

No — `PostToolUse` fires *after* the tool call has already executed (§3). By the time it runs, the
force-push has happened; the hook could log it or shout, but the damage is done. You can't un-push from
"after."

You must use **`PreToolUse`**, because it is the only event that runs *before* the action and can
**veto** it: the hook inspects the proposed `Bash` command, sees `git push --force`, returns a block
signal, and the harness **skips the call entirely** (the protected-path write in the Build It is the
same mechanism). The load-bearing capability is the **block/veto** — `PreToolUse` stands *between
intent and effect*, which is exactly the seam the harness owns (Chapter 1). This is the repo's real
git-guardrails pattern.

</details>

> **Exercise 3 — Why the inner `grep`?** The real hook matches `Write|Edit` *and* then runs
> `grep -q 'content/cortex/'` inside the command. The matcher already narrowed it to writes — so why
> the second check? What goes wrong if you delete the `grep`?

<details>
<summary><strong>Answer</strong></summary>

The **matcher** filters by *tool* — "only on Write or Edit" — but it can't filter by *which file* was
written (§3). Every Write/Edit in the whole repo (source code, configs, `README.md`) would match the
matcher. The inner `grep -q 'content/cortex/'` adds the **content filter**: it reads `file_path` from
the stdin payload and only rebuilds the index when the change is actually a book file.

Delete the `grep` and the index would rebuild on *every* file write anywhere in the project — editing a
Scala file, a `.gitignore`, anything — burning time on a no-op rebuild and coupling unrelated work to
the book index (the over-broad-hook failure from §6/§7). The lesson: **matchers gate by tool, an inner
guard gates by payload** — together they scope the hook to *exactly* the events that should trigger it.
You saw this in the Build It: `README.md` matched the matcher but the `content/` check made it a no-op.

</details>

```quiz
{
  "prompt": "Why use a hook to rebuild the book index instead of just telling the model (in CLAUDE.md) to rebuild it after editing content?",
  "input": "Choose one:",
  "options": [
    "Because the harness runs a hook deterministically on every matching event, while the model follows a CLAUDE.md instruction only probabilistically and can forget it",
    "Because hooks make the language model itself smarter and less likely to make mistakes",
    "Because CLAUDE.md instructions are not allowed to mention shell commands",
    "Because a hook runs the rebuild faster than the same command would run otherwise"
  ],
  "answer": "Because the harness runs a hook deterministically on every matching event, while the model follows a CLAUDE.md instruction only probabilistically and can forget it"
}
```

## In the Wild

- **[Claude Code — Hooks reference](https://docs.claude.com/en/docs/claude-code/hooks)** — the full
  list of lifecycle events (`PreToolUse`, `PostToolUse`, `UserPromptSubmit`, `Stop`, …), matcher
  syntax, the JSON-on-stdin contract, and how a `PreToolUse` hook signals a block.
- **[Claude Code — Get started with hooks](https://docs.claude.com/en/docs/claude-code/hooks-guide)**
  — a hands-on walkthrough that builds exactly our kind of example: run a command automatically after a
  file is written/edited.
- **[Claude Code settings (`settings.json`)](https://docs.claude.com/en/docs/claude-code/settings)** —
  where hooks (and permissions) actually live; the same file this repo uses to register the real
  index-rebuild `PostToolUse` hook you dissected in §4.

---

**Next:** hooks fire *automatically*; sometimes you want to *invoke* a packaged action *on purpose* —
a reusable command or a bundle of expertise you call by name. →
[5. Slash commands & skills](/cortex/the-claude-stack/claude-code-in-action/slash-commands-and-skills)
