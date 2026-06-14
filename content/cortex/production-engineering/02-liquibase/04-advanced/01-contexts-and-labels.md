---
title: '9. Contexts and labels'
summary: Sometimes a changeset should run in dev but not prod — seed data, test fixtures. Contexts and labels let you select which changesets run for a given environment or run, from one shared changelog.
---

# 9. Contexts and labels

## TL;DR

> One changelog, but not every changeset belongs everywhere: you want test seed data in **dev** but never in **prod**, or a one-off fix tagged for a single run. **Contexts** and **labels** are Liquibase's two filtering mechanisms. A **context** tags a changeset with *where it belongs* (`context:"dev"`), and you pass the active context at runtime (`--contexts=dev`) to select matching changesets. **Labels** tag changesets with arbitrary categories (`labels:"seed,experimental"`) and you filter with a label *expression* at runtime. Both let one source-of-truth changelog behave differently per environment — without forking it.

## 1. Motivation

The whole point of a changelog is that it's *one* shared history applied identically everywhere (Chapter 2). But reality has exceptions: you want a `tester` user and sample rows in *development* so you can click around, but you'd never insert fake data into *production*. You might have a changeset that's only relevant when a feature flag is on, or a hotfix you want to apply to exactly one environment. If your only tool is "the changelog runs in full, everywhere," you can't express these — you'd be tempted to maintain *different changelogs per environment*, which reintroduces the drift you worked so hard to eliminate.

Contexts and labels solve this without forking. You keep *one* changelog with *all* changesets, and *tag* the environment-specific ones. At runtime you say "I'm dev, run the dev-tagged ones too" or "I'm prod, skip them." The shared history stays shared; the *selection* varies. It's the difference between "different scripts per environment" (drift) and "one script, environment-aware filters" (controlled variation).

## 2. Intuition (Analogy)

A **recipe with optional steps marked for occasions.** A single master recipe might note: *"(for the kids' party) add sprinkles"* and *"(for the dinner version) add brandy."* It's *one* recipe — the core is identical every time — but certain steps are tagged for certain occasions, and the cook includes only the steps matching today's event. You don't keep a separate "kids' recipe" and "dinner recipe" that slowly drift apart; you keep one, with conditional steps.

A **context** is the *occasion* ("kids' party" = dev, "dinner" = prod): you declare the occasion at runtime, and occasion-tagged steps are included or skipped accordingly. A **label** is a finer *category* on a step ("contains-nuts", "spicy") that you can filter on with expressions ("include spicy AND NOT nuts"). Same recipe, different selections — no forking.

## 3. Formal Definition

- **Context.** A changeset attribute naming the environment(s)/situation(s) it belongs to: `--changeset cortex:99 context:"dev"`. At runtime you pass the *active* context (`liquibase update --contexts=dev`). **Rules:** a changeset with *no* context runs *always*; a changeset *with* a context runs *only* if its context matches the active one. Contexts support simple expressions (`context:"dev or test"`).
- **Label.** A changeset attribute for arbitrary categorization: `--changeset cortex:99 labels:"seed,experimental"`. At runtime you filter with a *label expression* (`--labels="seed and !experimental"`). Labels are richer for boolean filtering than contexts.
- **Context vs label (the distinction):** *Contexts* are usually about *where* (which environment you're running in — you set the context). *Labels* are about *what kind* of change it is — you write boolean expressions to select categories. Functionally they overlap; convention is "context = environment, label = category."
- **Default behavior.** Untagged changesets run everywhere. Tagging is *opt-in narrowing* — you only tag the exceptions.

> Contexts and labels let one changelog run *selectively*: tag the environment- or category-specific changesets, pass the active context/label filter at runtime, and the right subset runs — no per-environment changelog forks.

## 4. Worked Example — dev-only seed data

Cortex's local realm imports a `tester` user (Part 3); imagine the database equivalent — seed rows you want *only* in dev. With a context, one changelog serves both:

```sql
--liquibase formatted sql

--changeset cortex:1-create-visits
CREATE TABLE visits (id INT PRIMARY KEY, count BIGINT NOT NULL);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;

-- Seed data for local development ONLY — never runs in production.
--changeset cortex:99-dev-seed context:"dev"
INSERT INTO visits (id, count) VALUES (2, 42), (3, 99);
--rollback DELETE FROM visits WHERE id IN (2, 3);
```

Now the *same* changelog produces different results by environment:

```text
# Local dev — include dev-tagged changesets:
liquibase update --contexts=dev
#   runs: 1-create-visits  AND  99-dev-seed   (you get the sample rows)

# Production — no dev context passed:
liquibase update
#   runs: 1-create-visits  ONLY   (99-dev-seed is skipped — its context doesn't match)
```

The untagged `1-create-visits` runs everywhere (it's core schema). The `context:"dev"`-tagged `99-dev-seed` runs *only* when `--contexts=dev` is active — so production never gets the fake rows. One changelog, one history, environment-appropriate selection. In a startup-migration setup like Cortex's (Chapter 7), you'd pass the active context via configuration (e.g. an env var feeding `--contexts`), so the *same image* seeds in dev and stays clean in prod — the same "config selects behavior" idea as Part 1's `application.conf` env overrides.

## 5. Build It

Run this. It implements context filtering and shows one changelog producing different applied sets per environment.

```python run
CHANGELOG = [
    {"id": "1-create-visits",  "context": None,  "labels": set()},          # core: runs everywhere
    {"id": "2-submissions",    "context": None,  "labels": set()},          # core
    {"id": "99-dev-seed",      "context": "dev", "labels": {"seed"}},       # dev only
    {"id": "50-experimental",  "context": None,  "labels": {"experimental"}},
]

def should_run(cs, active_context, label_filter):
    # Context rule: no context -> always; has context -> only if it matches the active one.
    if cs["context"] is not None and cs["context"] != active_context:
        return False
    # Label filter: a simple "exclude these labels" expression.
    if label_filter and (cs["labels"] & label_filter["exclude"]):
        return False
    return True

def update(active_context, label_filter=None):
    return [cs["id"] for cs in CHANGELOG if should_run(cs, active_context, label_filter or {})]

print("dev   (--contexts=dev):            ", update("dev"))
print("prod  (no context):                ", update("prod"))
print("prod, excluding experimental:      ",
      update("prod", {"exclude": {"experimental"}}))
```

**Now break it.** Add `context:"prod"` to `2-submissions` (pretend it's prod-only) and run the `"dev"` line — `2-submissions` now *disappears* in dev, which would be a disaster (the core table is missing locally!). The lesson: **tag the exceptions, never the core.** Core schema must be untagged so it runs *everywhere*; only genuinely environment-specific changesets (seed data, one-offs) get a context. Misusing contexts on core schema reintroduces the very drift contexts were meant to prevent.

## 6. Trade-offs & Complexity

| Contexts/labels (one changelog) | Separate changelogs per environment |
|---|---|
| One shared history, no forking | N changelogs that drift apart |
| Environment differences are explicit + tagged | Differences are implicit, scattered |
| Same image, context via config | Different builds/scripts per env |
| Easy to misuse (tagging core schema) | "Simpler" but drift-prone |
| Two mechanisms to understand | One concept, many copies |

The cost is a little conceptual overhead (two filtering mechanisms, and the discipline to tag only exceptions) plus the risk of *over*-using them — context everything and you've effectively forked your changelog in disguise. Used sparingly — core schema untagged, a handful of clearly environment-specific changesets tagged — they're exactly the right tool for "mostly the same, with deliberate exceptions." Most changesets should have *no* context or label at all.

## 7. Edge Cases & Failure Modes

- **Tagging core schema with a context.** The cardinal mistake (shown above): a core table tagged `context:"prod"` won't exist in dev. Only tag genuinely environment-specific changesets.
- **Forgetting to pass the context.** If dev forgets `--contexts=dev`, the seed data silently doesn't run, and "it works in CI but not locally" confusion follows. Wire the context into config so it's not a manual flag.
- **Context vs label confusion.** Using contexts where you need boolean expressions (or vice versa) leads to awkward filters. Rule of thumb: context = environment, label = category with expressions.
- **Seed data leaking to prod.** The whole danger you're guarding against — double-check that data-insert changesets are context-restricted, and that prod never passes the dev context.

## 8. Practice

> **Exercise 1 — Tag or not?** For each, decide context/label or untagged: (a) `CREATE TABLE orders`; (b) inserting 100 fake test orders; (c) a Postgres-only performance index; (d) a one-time data fix for a specific incident.

<details>
<summary><strong>Answer</strong></summary>

The rule of thumb: **tag only the exceptions; leave core schema untagged** so it runs everywhere (§3, "Now break it").

- **(a) `CREATE TABLE orders` — untagged.** It's core schema; it must exist in *every* environment. Tagging it would make the table vanish where the context doesn't match — the cardinal mistake.
- **(b) 100 fake test orders — `context:"dev"` (or `test`).** Classic seed data: you want it in dev to click around, never in prod. Context = *where* it belongs.
- **(c) Postgres-only performance index — `dbms`/precondition, not a context.** "Only on Postgres" is a *database-engine* condition, best expressed with a `dbms type:postgresql` guard (Chapter 10), not an environment context — though you could carry a descriptive `label` like `performance` for filtering. The index itself is core, so don't context-restrict it by environment.
- **(d) One-time incident data fix — a `label` (e.g. `labels:"hotfix-1234"`).** A one-off you select for a specific run via a label expression. Labels are about *what kind* of change it is.

The throughline: untag the core, tag the genuinely environment- or category-specific exceptions, and most changesets should have *no* tag at all.

</details>

> **Exercise 2 — Write the run.** You have core schema (untagged) plus a `context:"dev"` seed changeset. Write the `liquibase update` command for local dev and for prod, and state which changesets each runs.

<details>
<summary><strong>Answer</strong></summary>

```text
# Local dev — declare the active context so dev-tagged changesets are included:
liquibase update --contexts=dev
#   runs: the untagged core schema  AND  the context:"dev" seed changeset

# Production — pass no context:
liquibase update
#   runs: the untagged core schema ONLY  (the dev seed is skipped — its context doesn't match)
```

The matching rule does the work (§3): a changeset with **no** context runs **always**, so the core schema applies in both. A changeset **with** `context:"dev"` runs **only** when `dev` is the active context, so the seed data lands in dev and is skipped in prod. One changelog, one shared history, environment-appropriate selection — no fork. (In Cortex's startup-migration setup you'd feed `--contexts` from config/an env var so the *same image* seeds in dev and stays clean in prod.)

</details>

> **Exercise 3 — The drift trap.** Explain how *overusing* contexts (tagging lots of changesets per environment) can recreate the very schema drift that migrations exist to prevent.

<details>
<summary><strong>Answer</strong></summary>

The whole value of a changelog is *one* shared history applied identically everywhere — that's what kills drift. A context is *opt-in narrowing*: each tag says "this changeset runs in some environments but not others." Tag a *handful* of genuine exceptions (seed data) and you're fine. But tag *lots* of changesets `context:"dev"` here, `context:"prod"` there, and the set of changesets that actually runs **diverges per environment** — dev's schema and prod's schema are now built from different subsets.

At that point you've effectively **forked your changelog in disguise**: it's one file, but it produces N different schemas, which is exactly the per-environment drift contexts were meant to prevent — except now the divergence is *implicit*, scattered across tags, and harder to see than separate files would be. The discipline is therefore: core schema **untagged** (runs everywhere, stays identical), and only the rare, genuinely environment-specific changeset tagged. Most changesets should carry no context at all.

</details>

```quiz
{
  "prompt": "What do Liquibase contexts let you do with a single changelog?",
  "input": "Choose one:",
  "options": [
    "Select which changesets run in a given environment/run (e.g. dev-only seed data) — untagged changesets run everywhere; context-tagged ones run only when their context is active",
    "Encrypt changesets per environment",
    "Run changesets in a random order",
    "Skip the DATABASECHANGELOG table"
  ],
  "answer": "Select which changesets run in a given environment/run (e.g. dev-only seed data) — untagged changesets run everywhere; context-tagged ones run only when their context is active"
}
```

## In the Wild

- **[Liquibase — Contexts](https://docs.liquibase.com/concepts/changelogs/attributes/contexts.html)** — declaring and passing contexts, and the matching rules.
- **[Liquibase — Label expressions](https://docs.liquibase.com/concepts/changelogs/attributes/labels.html)** — boolean filtering with labels.
- **[Liquibase — Contexts vs labels](https://docs.liquibase.com/concepts/changelogs/attributes/contexts.html)** — when to use which, with examples.

---

**Next:** sometimes a changeset should only run *if the database is in a certain state* — the table is empty, or it's Postgres. Preconditions let a changeset guard itself. → [10. Preconditions](/cortex/production-engineering/liquibase/advanced/preconditions)
