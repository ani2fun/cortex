---
title: '2. Migrations as version control for your schema'
summary: Git tracks your code as an ordered log of commits applied to reach a known state. Migrations track your schema the same way — an ordered log of changesets. Liquibase is git for your database structure.
---

# 2. Migrations as version control for your schema

## TL;DR

> You already trust an ordered log of changes to manage your code: that's **git**. Each commit is a small, recorded, ordered change; applying them in order reproduces an exact state; a branch that's behind just replays the commits it's missing. **Migrations are the same idea for your database schema.** Each **changeset** is a small, recorded, ordered change; applying them in order reproduces an exact schema; a database that's behind replays the changesets it hasn't run. Liquibase is, quite literally, version control for your schema — and once you see the parallel, the whole tool clicks.

## 1. Motivation

In Chapter 1 we demanded four properties of schema change — explicit, ordered, recorded, reversible — and noted they sound like what git does for code. That's not a coincidence; it's the key insight. The reason git transformed software is that it turned "the state of the code" into "an ordered, replayable history of changes," so any two checkouts can be made identical by replaying the diffs between them. Databases have exactly the same need (keep many copies in sync) and exactly the same solution (an ordered, replayable history of changes), but for *schema* instead of *files*.

Internalizing the parallel makes Liquibase intuitive instead of arbitrary. A `changelog` is your schema's commit history. A `changeset` is a commit. The `DATABASECHANGELOG` table is the database's record of "which commits I've applied" — its equivalent of git's tracking of your current position in history. "Migrating" a database is "pulling and replaying the commits it's missing." Every Liquibase concept maps onto a git concept you already understand.

## 2. Intuition (Analogy)

Lay the two side by side and the mapping is exact:

| Git (versioning code) | Liquibase (versioning schema) |
|---|---|
| A **commit** — a small, recorded change | A **changeset** — a small, recorded change |
| The **commit history** (ordered log) | The **changelog** (ordered list of changesets) |
| A commit's **SHA / identity** | A changeset's **id + author** |
| `git log` — what's been applied | The **`DATABASECHANGELOG`** table |
| **Replaying commits** to reach a state | **Applying changesets** to reach a schema |
| A branch that's **behind** replays missing commits | A database that's behind runs missing changesets |
| **Revert** a commit | **Rollback** a changeset |
| A **merge conflict** | A **checksum mismatch** (Chapter 12) |

Just as you'd never email zip files of your codebase around and manually reconcile them (you use git), you never copy schemas around and manually reconcile them (you use migrations). The database keeps its own `git log` — the `DATABASECHANGELOG` table — recording exactly which changesets it has applied, so it always knows precisely where it is in history.

## 3. Formal Definition

- **Changeset** — the atomic unit of schema change: a uniquely identified (`id` + `author`) set of one or more SQL operations, applied as a unit, exactly once. The "commit" of schema history.
- **Changelog** — the ordered list of changesets, stored as files in your repo (committed alongside your code). The "commit history." Liquibase reads it top to bottom.
- **`DATABASECHANGELOG`** — a table Liquibase creates *in your database* to record which changesets have been applied (id, author, a checksum, order, timestamp). The database's own "`git log`" — its memory of where it is. (We'll watch it fill up in Chapter 5.)
- **Apply / update** — Liquibase compares the changelog (what *should* be applied) against `DATABASECHANGELOG` (what *has* been applied) and runs only the *new* changesets, in order. Like `git pull` replaying missing commits.
- **Rollback** — undo applied changesets in reverse order, using each changeset's defined reversal (Chapter 6). Like `git revert`.

> A changeset is a commit; the changelog is the history; `DATABASECHANGELOG` is the database's `git log`; migrating is replaying the missing commits. Version control, applied to schema.

## 4. Worked Example — the same workflow, two domains

Watch a feature's lifecycle in both worlds:

```text
CODE (git)                                  SCHEMA (Liquibase)
──────────                                  ──────────────────
1. Write a feature                          1. Write a changeset
   (edit files)                                (add a column / table)

2. git commit -m "add phone field"          2. Add changeset `5-add-phone` to the changelog

3. git push (review in a PR)                3. Commit the changelog file (review in the SAME PR)

4. Teammate: git pull                       4. Their DB: liquibase update
   -> replays your commit                      -> runs changeset 5 (and only 5)

5. Production deploy:                        5. App startup:
   git checkout <release>                       liquibase update applies any new changesets
```

Notice the changelog is *committed alongside the code in the same PR*. That's the deep payoff: your schema change and the code that depends on it travel together, are reviewed together, and are deployed together. When Cortex's `SubmissionPipeline` code was added, the `submissions` table changeset was added *in the same change* — so there is never a moment where the code expects a table the schema doesn't have. The schema's history and the code's history are *one* history, versioned by the same git and applied by Liquibase. (And in production, Cortex runs `liquibase update` automatically at boot — Chapter 7 — so "deploy the code" and "migrate the schema" are a single, atomic step.)

## 5. Build It

Run this. It builds a mini "version control for schema": a changelog, a per-database record of applied changesets, and an `update` that replays only what's missing — exactly Liquibase's core loop.

```python run
# The CHANGELOG — committed in git, the single source of truth (like commit history).
CHANGELOG = [
    {"id": "1-init",        "sql": "CREATE TABLE visits (...)"},
    {"id": "2-submissions", "sql": "CREATE TABLE submissions (...)"},
    {"id": "3-add-phone",   "sql": "ALTER TABLE users ADD COLUMN phone TEXT"},
]

def update(db_changelog_table):
    """Like `liquibase update` / `git pull`: run only changesets not yet applied, in order."""
    applied_ids = {row["id"] for row in db_changelog_table}
    for cs in CHANGELOG:
        if cs["id"] not in applied_ids:               # the gate: apply each EXACTLY once
            print(f"   applying {cs['id']}: {cs['sql']}")
            db_changelog_table.append({"id": cs["id"], "order": len(db_changelog_table) + 1})
    return db_changelog_table

# A brand-new database: its DATABASECHANGELOG starts empty.
fresh_db = []
print("fresh database — runs the WHOLE history:")
update(fresh_db)
print("   DATABASECHANGELOG now:", [r["id"] for r in fresh_db], "\n")

# A database already at changeset 2 (e.g. staging): runs only what it's missing.
staging_db = [{"id": "1-init", "order": 1}, {"id": "2-submissions", "order": 2}]
print("staging (already at #2) — runs only the NEW changeset:")
update(staging_db)
print("   DATABASECHANGELOG now:", [r["id"] for r in staging_db])
```

**Now break it.** Run `update(fresh_db)` a *second* time — nothing happens, because every changeset is already in `DATABASECHANGELOG`. That idempotence ("apply each exactly once, ever") is the heart of migrations: you can run `liquibase update` on any database, any number of times, and it converges to the changelog's state without re-running or double-applying anything. It's `git pull` for your schema — safe to run repeatedly, doing only what's needed.

## 6. Trade-offs & Complexity

| Schema as version control (Liquibase) | Schema as "whatever's in prod" |
|---|---|
| History, review, audit — like code | Opaque current state, no history |
| Any DB reproducible from the changelog | Can't recreate environments reliably |
| Idempotent `update` converges any DB | Manual reconciliation, error-prone |
| Schema change ships *with* the code | Schema and code drift apart |
| Changelog files to maintain | Nothing to maintain (until it breaks) |

The overhead is a changelog you maintain and a tool in your deploy. But it's overhead you *already accept for your code* via git — and you'd never dream of managing code without version control. The argument is simply: your schema is at least as important and at least as in-need-of-history as your code, so version it the same way. The cognitive cost is near zero once you see it *is* version control.

## 7. Edge Cases & Failure Modes

- **Editing an already-applied changeset.** Like rewriting a pushed git commit — it breaks the shared history. Liquibase detects it as a *checksum mismatch* (Chapter 12). Add a *new* changeset instead.
- **Reordering changesets.** Order is identity here; shuffling applied changesets corrupts the history. Append new ones; never reorder old ones.
- **Out-of-band changes.** A manual `ALTER` that bypasses Liquibase makes the real schema diverge from `DATABASECHANGELOG`'s story — the drift returns. Everything goes through the changelog.
- **Treating the changelog as mutable.** Once a changeset has run somewhere, treat it as immutable history (like a pushed commit). Future changes are *new* changesets.

## 8. Practice

> **Exercise 1 — Map it.** For each git concept, name the Liquibase equivalent: commit, commit history, `git log`, `git pull`, `git revert`, merge conflict.

<details>
<summary><strong>Answer</strong></summary>

The mapping from §2, term for term:

- **commit** → **changeset** — a small, recorded, ordered, uniquely-identified (`id` + `author`) unit of change.
- **commit history** → **changelog** — the ordered list of changesets, committed in your repo; Liquibase reads it top to bottom.
- **`git log`** → the **`DATABASECHANGELOG`** table — the database's *own* record of which changesets it has applied, so it always knows where it is in history.
- **`git pull`** → **`liquibase update`** — compare history against what's applied and replay only the missing changesets, in order.
- **`git revert`** → **rollback** — undo applied changesets in reverse order, using each one's defined reversal.
- **merge conflict** → **checksum mismatch** — Liquibase's signal that the shared history was altered (an applied changeset was edited).

The point of the table isn't trivia — it's that *every* Liquibase concept is a git concept you already trust, just pointed at schema instead of files. Once the mapping clicks, the tool stops feeling arbitrary.

</details>

> **Exercise 2 — Reproduce a database.** Explain how, given only the changelog and an empty database, you can reproduce an *exact* schema — and why that's the same guarantee `git checkout` gives you for code.

<details>
<summary><strong>Answer</strong></summary>

The changelog is the *full, ordered history* of every schema change — and applying changesets is a deterministic, replayable operation. So starting from an empty database, `liquibase update` reads the changelog top to bottom and runs every changeset in order; since the database's `DATABASECHANGELOG` begins empty, it runs the *whole* history. The end state is a function purely of the changelog, so it is **identical** every time, on every empty database you point it at:

```text
empty DB + changelog ──(update, replays all changesets in order)──▶ the exact schema
```

This is the same guarantee `git checkout <commit>` gives you for code. Git reproduces an exact working tree by replaying the recorded history of changes up to a known point; the tree is a deterministic function of the commit history, so any checkout of the same commit yields byte-identical files. Liquibase reproduces an exact *schema* by replaying the recorded changesets; the schema is a deterministic function of the changelog. In both cases the magic is the same: **state is reconstructed from an ordered, replayable log of changes**, which is why two checkouts (or two fresh databases) can always be made identical. It's exactly how a brand-new Cortex anywhere ends up with a schema provably identical to production.

</details>

> **Exercise 3 — Why immutable?** Explain why editing an applied changeset is like rewriting a pushed git commit, and what goes wrong when two databases have run *different versions* of "the same" changeset.

<details>
<summary><strong>Answer</strong></summary>

A pushed git commit is *shared history* — other people's branches are built on top of it, identified by its SHA. Rewriting it (amending after pushing) changes that identity out from under everyone, and git's history-tracking breaks: the same logical commit now has two different forms in two places, and reconciling them is a mess. An **applied changeset is shared history too**. Once it has run on *any* database, that database's `DATABASECHANGELOG` has recorded it — including a **checksum** of its contents (Chapter 5). Editing the changeset afterward is rewriting history that's already been committed somewhere.

What goes wrong when two databases ran *different versions* of "the same" changeset (same `id` + `author`, different SQL):

- **The schemas silently diverge.** Database A ran the old SQL, database B ran the edited SQL — but both record the changeset as "applied," so neither *knows* it's now inconsistent with the other. The drift Chapter 1 warned about returns, this time invisibly.
- **Liquibase catches it as a checksum mismatch.** On the next `update`, the changeset's recomputed checksum no longer matches the stored one (the git equivalent of a *merge conflict*), and Liquibase errors to stop you from compounding the divergence (Chapter 12).
- **Re-running won't fix it.** `update` skips changesets already recorded by id+author, so editing the SQL does *not* cause the new version to re-apply on databases that already ran the old one.

The discipline that avoids all of this is the same one git teaches: treat applied changesets as immutable. To change something, **append a new changeset**, exactly as you'd add a new commit rather than rewrite a pushed one.

</details>

```quiz
{
  "prompt": "In the 'migrations are version control for schema' analogy, what is the database's `DATABASECHANGELOG` table equivalent to in git?",
  "input": "Choose one:",
  "options": [
    "`git log` — the record of which commits (changesets) have already been applied, so it knows where it is in history",
    "A merge conflict",
    "The remote repository",
    "The .gitignore file"
  ],
  "answer": "`git log` — the record of which commits (changesets) have already been applied, so it knows where it is in history"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Concepts (changelog, changeset, DATABASECHANGELOG)](https://docs.liquibase.com/concepts/home.html)** — the core vocabulary, which now maps cleanly onto git in your head.
- **[Martin Fowler — Evolutionary Database Design](https://martinfowler.com/articles/evodb.html)** — the foundational essay on versioning and evolving schemas alongside code.
- **[Cortex `db.changelog-master.yaml`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/db.changelog-master.yaml)** — Cortex's changelog: its schema's "commit history," which you'll read in detail next.

---

**Next:** enough theory — let's read the actual files. Meet the changelog and the changeset, the two structures that hold your schema's history, through Cortex's real `db.changelog-master.yaml`. → [3. Changelogs and changesets](/cortex/production-engineering/liquibase/basics/changelogs-and-changesets)
