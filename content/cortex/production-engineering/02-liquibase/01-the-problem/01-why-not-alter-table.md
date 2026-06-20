---
title: '1. Why you can''t just ALTER TABLE in production'
summary: A production database is a single, stateful, irreplaceable object full of real users'' data. Changing its shape by hand is the most dangerous routine thing an engineer does — and it''s how environments silently drift apart.
---

# 1. Why you can't just ALTER TABLE in production

## TL;DR

> Your code is reproducible — you can rebuild it from source anytime. Your **database is not**: it's a single, stateful, irreplaceable object holding real users' data, and you only get one of it. Changing its schema by hand — SSHing in and running `ALTER TABLE` — is unrepeatable, undocumented, and drifts across environments until a deploy fails in a way nobody can explain. The discipline that fixes this is **migrations**: making every schema change an explicit, ordered, recorded, reversible step. That discipline is what Liquibase provides.

## 1. Motivation

Here's a tale every team eventually lives. A feature needs a new column, so an engineer connects to the production database and runs `ALTER TABLE users ADD COLUMN phone TEXT;`. It works. The feature ships. Months later, a new hire sets up a fresh environment from the schema in the repo — which *doesn't have* the `phone` column, because nobody wrote it down. Their environment is subtly different from production. A deploy that assumed `phone` exists fails in staging but not prod, or worse, fails in prod but not staging. Now three databases (laptop, staging, prod) have *drifted apart* in ways nobody can fully enumerate, and every deploy is a small prayer.

The root problem: **a database is state, and state is the hard part.** You can delete and rebuild your compiled code a thousand times with zero consequence. You cannot delete and rebuild your production database — it holds the only copy of your customers' orders, messages, money. So changes to its *shape* can't be casual. A botched `ALTER TABLE` on a 500-million-row table can lock it for an hour (an outage) or, worse, drop data you can't get back. The asymmetry between "code, which is reproducible" and "data, which is precious and singular" is why schema change deserves its own discipline.

## 2. Intuition (Analogy)

Renovating a **skyscraper while people live and work inside it**.

Building new code is like erecting a *new* building: if you get it wrong, knock it down and start over, no harm done. Changing a production schema is *renovating an occupied tower*. You can't evacuate everyone; the elevators must keep running; and if you cut the wrong support beam, the building doesn't just "throw an exception" — it falls, with people inside. So you don't let a contractor wander in and start swinging a sledgehammer based on memory. You require *blueprints* (every change written down), *permits and inspections* (review), a *sequence* (you can't install the 40th floor before the 39th), and a *demolition plan you could reverse* if an inspection fails mid-way.

`ALTER TABLE` typed live into a prod console is the sledgehammer with no blueprint. Migrations are the blueprints, permits, sequence, and reversal plan — the engineering discipline that lets you change a load-bearing, occupied structure without it collapsing.

## 3. Formal Definition

The problems with ad-hoc, manual schema change:

- **Unrepeatable.** A command typed once into one database leaves no artifact. You can't re-run it identically on the next environment, and you can't prove what was run.
- **Schema drift.** Without a single recorded sequence, environments (dev, CI, staging, prod) diverge. "Works on my machine" becomes "works on my *schema*."
- **No review or audit.** A live `ALTER` bypasses code review, leaves no history of *who* changed *what*, *when*, or *why*.
- **No rollback.** If the change is wrong, there's no defined way back — and some changes (a dropped column) are irreversible without a backup.
- **Downtime risk.** Naive DDL can take locks that block reads/writes on large tables for a long time — an outage hiding inside a one-line command.

A **migration** is the cure: a schema change captured as an *explicit, versioned, ordered, recorded* unit that is applied *programmatically and identically* to every database. The four properties to demand of any schema change:

> **Explicit** (written down as an artifact), **ordered** (a defined sequence, applied once each), **recorded** (the database knows which changes it has), and **reversible** (a defined way back). Manual `ALTER TABLE` has none of these; a migration has all four.

## 4. Worked Example — two histories

Watch the same change play out manually vs. as a migration.

**Manual (the drift machine):**

```text
# Dev laptop:   ALTER TABLE submissions ADD COLUMN language TEXT;   (Tuesday, undocumented)
# Staging:      (forgot)
# Production:   ALTER TABLE submissions ADD COLUMN language VARCHAR(20);   (Friday, slightly different!)
#
# Result: three schemas, two subtly different column types, zero record of any of it.
# Next deploy assumes TEXT; staging has no column at all -> 500s nobody can reproduce locally.
```

**As a migration (the cure):**

```text
# One artifact, committed to git, reviewed in a PR:
#   changeset cortex:4-add-submission-language
#     ALTER TABLE submissions ADD COLUMN language TEXT NOT NULL DEFAULT 'python';
#
# Applied programmatically to dev, CI, staging, prod — IDENTICALLY, exactly once each.
# The database records that changeset 4 ran. Every environment is provably in the same state.
```

This is exactly how Cortex works. Its schema isn't a thing anyone alters by hand — it's a *sequence of recorded changesets* (you'll meet `v1-init.sql`, `v2-submissions.sql` in Chapter 4) applied automatically at startup by Liquibase (Chapter 7). Set up a fresh Cortex anywhere and it runs the *same* changesets in the *same* order, so its schema is *guaranteed* identical to production. No drift, no mystery, no Friday-afternoon `ALTER`.

## 5. Build It

Run this. It simulates schema drift from manual changes versus the consistency of an ordered, recorded migration list.

```python run
# Three environments, each just a SET of applied schema changes.
dev, staging, prod = set(), set(), set()

# --- Manual, ad-hoc changes: applied wherever someone remembered ---
dev.add("add_language_TEXT")                 # Tuesday, on the laptop
prod.add("add_language_VARCHAR20")           # Friday, on prod, slightly different
# staging: nobody ran anything

print("== after manual changes ==")
print("dev    :", dev)
print("staging:", staging)
print("prod   :", prod)
print("all environments identical?", dev == staging == prod, "\n")

# --- Migrations: ONE ordered list applied identically everywhere ---
CHANGELOG = ["1_init", "2_submissions", "3_add_language_TEXT"]   # the single source of truth

def migrate(env_applied):
    for change in CHANGELOG:
        if change not in env_applied:        # apply each exactly once, in order
            env_applied.add(change)
    return env_applied

dev2, staging2, prod2 = set(), set(), set()
for env in (dev2, staging2, prod2):
    migrate(env)

print("== after running the SAME changelog everywhere ==")
print("all environments identical?", dev2 == staging2 == prod2)
print("each knows exactly what it has:", sorted(prod2))
```

**Now break it.** In the manual section, you can't make the three environments match without manually reconciling them and *hoping* you remembered every difference. In the migration section, they're identical *by construction* — and adding a change is just appending to `CHANGELOG` and re-running `migrate` everywhere. That gap — "hope I remembered" vs "identical by construction" — is the entire value proposition of migrations, and the reason no serious system changes a production schema by hand.

## 6. Trade-offs & Complexity

| Migrations (Liquibase) | Manual `ALTER TABLE` |
|---|---|
| Reproducible across environments | Drift, "works on my schema" |
| Reviewed in a PR, audited | No review, no history |
| Ordered, applied exactly once | Ad-hoc, unrepeatable |
| Defined rollback path | No way back |
| A tool + discipline to adopt | "Just run the SQL" (until it bites) |

The cost is real upfront discipline: every schema change becomes a committed artifact and a process, not a quick console command. For a one-person prototype with a throwaway database, that can feel like overhead. The instant there's a *second* environment, a *teammate*, or *data you can't lose* — which is every real system — manual schema change becomes a liability that compounds, and migrations become the only sane option. The rest of this Part is how to do them well.

## 7. Edge Cases & Failure Modes

- **The "tiny harmless" manual fix.** "I'll just add an index by hand, it's nothing" is how drift starts. Even trivial changes go through a migration, or your environments stop matching.
- **Locking DDL on big tables.** `ALTER TABLE ... ADD COLUMN` with a non-null default, or adding an index without `CONCURRENTLY` (Postgres), can lock a large table for a long time — an outage. (Chapter 11's expand/contract addresses this.)
- **Irreversible changes.** `DROP COLUMN` discards data permanently. A migration tool encourages a *thought-out, reviewed* drop with a backup, instead of a hasty live one.
- **Editing data, not just schema.** Migrations also cover *data* changes (backfills, seed rows — like Cortex's allow-list inserts). The same discipline applies: explicit, ordered, recorded.

## 8. Practice

> **Exercise 1 — Name the four.** List the four properties a schema change should have (explicit, ordered, recorded, reversible) and, for each, the bad thing that happens to a manual `ALTER` without it.

<details>
<summary><strong>Answer</strong></summary>

The four properties from §3, each paired with the failure mode a manual `ALTER TABLE` invites by lacking it:

- **Explicit** — the change is a written artifact you can re-run identically. Without it, a command typed once into one console leaves *no record*: you can't reproduce it, and you can't even prove what was run.
- **Ordered** — a defined sequence, each step applied exactly once. Without it, changes land ad-hoc wherever someone remembered, so two databases can run "the same" change in different forms (recall the `TEXT` vs `VARCHAR(20)` drift) and you can't reconstruct how a schema got to its current shape.
- **Recorded** — the database itself knows which changes it has. Without it, nothing tracks *who* changed *what*, *when*, or *why*; there's no audit trail and no way to ask a database where it stands.
- **Reversible** — a defined way back. Without it, a bad change has no clean undo — your only recourse is panic and a restore-from-backup, and some changes (a dropped column) can't be undone at all without that backup.

The through-line: manual `ALTER` has *none* of the four, which is precisely why environments drift and a deploy can fail in a way nobody can explain. A migration has *all four* by construction.

</details>

> **Exercise 2 — Trace the drift.** Describe a concrete sequence of three manual changes across dev/staging/prod that leaves them mutually inconsistent. Then show how a changelog would have prevented it.

<details>
<summary><strong>Answer</strong></summary>

A concrete drift sequence — three manual changes, three resulting schemas:

```text
1. Dev (Tue):   ALTER TABLE submissions ADD COLUMN language TEXT;        -- undocumented
2. Staging:     (nobody ran anything)                                    -- forgotten
3. Prod (Fri):  ALTER TABLE submissions ADD COLUMN language VARCHAR(20); -- same intent, different type
```

Now the three environments are mutually inconsistent: dev has `language TEXT`, prod has `language VARCHAR(20)`, staging has *no column at all*. A deploy whose code assumes the column exists works on dev, 500s on staging (column missing), and behaves subtly differently on prod (a `VARCHAR(20)` silently truncates a long value that `TEXT` would have kept). Worst of all, there's *zero record* of any of it, so nobody can even enumerate the differences.

How a changelog prevents it: the column becomes **one** committed artifact —

```sql
--changeset cortex:4-add-submission-language
ALTER TABLE submissions ADD COLUMN language TEXT NOT NULL DEFAULT 'python';
```

— applied *programmatically and identically* to dev, CI, staging, and prod, exactly once each. There is only one definition of the column, so the type can't diverge; every database records that changeset 4 ran, so "staging forgot" is impossible (running `update` applies whatever it's missing). The environments are identical *by construction* rather than *by hoping everyone remembered*.

</details>

> **Exercise 3 — Code vs data.** Explain why you can fearlessly rebuild your compiled code but not your production database. How does that asymmetry justify treating schema change specially?

<details>
<summary><strong>Answer</strong></summary>

Compiled code is **reproducible**: it's a deterministic function of your source, which lives in git. Delete the binary, the build directory, the whole checkout — and you can regenerate an *identical* copy from source any time, with zero consequence. The artifact is disposable because the recipe that produces it is preserved.

A production database is **state**, and state is the hard part. It's a single, stateful, irreplaceable object holding the *only* copy of your customers' orders, messages, and money. There is no source you can rebuild it from — the data *is* the original. Delete it and it's gone; corrupt it and there's no recompile. On top of that, changing its *shape* isn't free: a botched `ALTER TABLE` on a 500-million-row table can take a lock that blocks reads and writes for an hour (an outage), or drop data you can't get back.

That asymmetry — *code is reproducible, data is precious and singular* — is exactly what justifies treating schema change specially. Because you can't just "rebuild and retry," every change to the database's shape has to be careful: **explicit, ordered, recorded, reversible** (§3). You afford code the casual freedom to be thrown away precisely *because* you can rebuild it; the database earns the opposite discipline precisely *because* you can't. Migrations are that discipline.

</details>

```quiz
{
  "prompt": "Why is changing a production schema with a manual `ALTER TABLE` dangerous, compared with a migration?",
  "input": "Choose one:",
  "options": [
    "It's unrepeatable, undocumented, and drifts across environments — with no record, no review, and no defined rollback",
    "ALTER TABLE is always slower than a migration",
    "Migrations encrypt the database",
    "Manual changes can't be written in SQL"
  ],
  "answer": "It's unrepeatable, undocumented, and drifts across environments — with no record, no review, and no defined rollback"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[GitLab — Database outage of January 31 (2017)](https://about.gitlab.com/blog/postmortem-of-database-outage-of-january-31/)** — a legendary postmortem about the perils of manual operations on a production database. Required reading.
- **[Liquibase — Why database change management?](https://www.liquibase.com/resources/guides/database-version-control)** — the case for migrations, from the tool's makers.
- **[Cortex `db/changelog`](https://github.com/ani2fun/cortex/tree/main/server/src/main/resources/db/changelog)** — Cortex's schema as a recorded sequence of changesets, never a manual `ALTER`.

---

**Next:** if "explicit, ordered, recorded" sounds like what git does for code, you're exactly right. Let's make the analogy precise: migrations are version control for your schema. → [2. Migrations as version control for your schema](/cortex/production-engineering/liquibase/the-problem/migrations-as-version-control)
