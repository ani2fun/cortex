---
title: '3. Changelogs and changesets'
summary: The two structures that hold your schema''s history. A changeset is the atomic, uniquely-identified unit of change; the changelog is the ordered list that includes them. Read Cortex''s real master changelog.
---

# 3. Changelogs and changesets

## TL;DR

> Liquibase has exactly two structural concepts. A **changeset** is the atomic unit of change — a uniquely identified (`id` + `author`) bundle of one or more operations, applied as a unit, exactly once. A **changelog** is the ordered list of changesets; a **master changelog** is the top-level file that `include`s the others, giving you a clean, append-only history. Cortex's `db.changelog-master.yaml` includes two SQL files, in order. That's the whole structure — everything else is detail about *what* a changeset contains.

## 1. Motivation

If migrations are version control for schema (Chapter 2), you need the equivalent of a commit and a commit history — concrete files you write and Liquibase reads. Those are the changeset and the changelog. Getting their roles straight up front prevents the two most common beginner mistakes: cramming unrelated changes into one giant changeset (an un-revertable, un-reviewable blob), and editing old changesets instead of appending new ones (corrupting shared history). The structure is simple, but it *encodes the discipline* — small, identified, append-only units — that makes the whole system trustworthy.

## 2. Intuition (Analogy)

A **ship's logbook**. Each entry is dated, signed by the officer on watch, and records *one* event ("0400: changed heading to 270°"). You never erase or rewrite an old entry — that would be falsifying the record; you add a *new* entry to correct course. Anyone can read the log top to bottom and reconstruct exactly how the ship got from port to here.

A **changeset** is one logbook entry: identified (`id`), signed (`author`), recording a specific change. The **changelog** is the logbook itself — the ordered, append-only sequence. And just as you'd never tear out a page, you never edit an applied changeset; you append a new one. The master changelog is the logbook's *index*, pointing at the chapters (the included files) in order.

## 3. Formal Definition

- **Changeset.** The atomic unit. Identified by `id` + `author` (together unique within a changelog). Contains one or more **changes** (DDL/DML — create table, add column, insert rows). Applied as a unit and recorded once in `DATABASECHANGELOG`. Best practice: **one logical change per changeset**, so it can be applied, recorded, and rolled back independently.
- **Changelog.** An ordered list of changesets, in a file. Liquibase reads it top to bottom; order is significant (it's the history).
- **Master changelog.** A top-level changelog that **`include`s** (or `includeAll`s) other changelog files, rather than holding all changesets directly. This keeps each release's changes in its own file and the master as a clean, append-only index.
- **Formats.** Changelogs can be written in **XML, YAML, JSON, or SQL**. The *master* is often YAML/XML (good at structure/includes); the *changesets* themselves are often SQL (Chapter 4). Cortex uses a YAML master that includes SQL changesets.
- **`include` vs `includeAll`.** `include` lists files explicitly (deterministic order, what Cortex uses); `includeAll` globs a directory (convenient, but order depends on naming).

> Two ideas: the changeset (an identified, atomic, append-only unit of change) and the changelog (the ordered list of them, often a master that includes per-release files). Append, never edit; one logical change per changeset.

## 4. Worked Example — Cortex's master changelog

Cortex's [`db.changelog-master.yaml`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/db.changelog-master.yaml) is the entry point Liquibase reads. It holds *no changesets itself* — it's a pure index:

```yaml
databaseChangeLog:
  - include:
      file: changes/v1-init.sql            # release 1: the visits table
      relativeToChangelogFile: true
  - include:
      file: changes/v2-submissions.sql     # release 2: the submission tables
      relativeToChangelogFile: true
```

Read it as the logbook index. The master lists, *in order*, the changelog files for each chunk of schema history: `v1-init.sql` first, then `v2-submissions.sql`. `relativeToChangelogFile: true` means "find these files relative to *this* file," so the structure is portable. When Cortex grows a new schema change, you don't touch the existing files — you add `changes/v3-bookmarks.sql` and append one more `include` entry to the master. The master stays a clean, append-only index; each release's actual SQL lives in its own file, reviewable on its own.

This split — a structural master that *includes*, and per-release files that *contain* the changesets — is the standard, scalable layout. The master is your table of contents; the included files are the chapters. Liquibase reads the master, expands the includes in order, and gets one flat, ordered sequence of changesets to apply.

## 5. Build It

Run this. It models the master/include structure and shows Liquibase flattening it into one ordered changeset list — and why "one logical change per changeset" matters for rollback.

```python run
# Per-release changelog files, each holding one or more changesets.
v1_init = [
    {"id": "1-create-visits", "author": "cortex", "changes": ["CREATE TABLE visits", "INSERT visits row"]},
]
v2_submissions = [
    {"id": "2-create-allowlist",  "author": "cortex", "changes": ["CREATE TABLE submission_allowlist", "seed rows"]},
    {"id": "3-create-submissions","author": "cortex", "changes": ["CREATE TABLE submissions", "CREATE INDEX ..."]},
]

# The MASTER changelog: a pure index that includes the files IN ORDER.
master = [
    ("include", v1_init),
    ("include", v2_submissions),
]

def flatten(master):
    """Liquibase expands includes top-to-bottom into ONE ordered changeset list."""
    out = []
    for kind, payload in master:
        out.extend(payload)            # each include contributes its changesets, in order
    return out

changesets = flatten(master)
print("Liquibase sees this flat, ordered history:")
for i, cs in enumerate(changesets, 1):
    print(f"  {i}. {cs['author']}:{cs['id']}  -> {cs['changes']}")

# Identity = id+author, and it must be UNIQUE:
ids = [f"{c['author']}:{c['id']}" for c in changesets]
print("\nall changeset ids unique?", len(ids) == len(set(ids)))
```

**Now break it.** Give two changesets the same `id` + `author` (e.g. set both to `cortex:2-create-allowlist`) and re-run — `len(ids) == len(set(ids))` becomes `False`. Real Liquibase would error: a changeset's identity *must* be unique, because that identity is how `DATABASECHANGELOG` records "this one ran" (Chapter 5). Then try merging both `v2` changesets into one giant changeset — it still "works," but now you can't apply, record, or roll back the table and the index *independently*, which is exactly why "one logical change per changeset" is the rule.

## 6. Trade-offs & Complexity

| Master + included files (Cortex) | One giant changelog file |
|---|---|
| Per-release files, reviewed in isolation | One ever-growing file, merge-conflict prone |
| Clean append-only master index | Everyone edits the same file |
| `include` = deterministic order | Order tangled with edits |
| A bit more structure to set up | Simpler at first, messy at scale |

The cost is a slightly more elaborate layout (a master plus per-release files) than dumping everything in one file. For a handful of changesets it's marginal; as the history grows to dozens of releases and a team edits in parallel, the split is what keeps the master conflict-free and each release reviewable. The `include`-with-explicit-files approach (over `includeAll` globbing) trades a touch of convenience for *deterministic, reviewed* ordering — the right call for something as order-sensitive as schema history.

## 7. Edge Cases & Failure Modes

- **Multiple changes per changeset.** Bundling a table *and* an unrelated index *and* a backfill into one changeset means they apply (and roll back) all-or-nothing, and the `DATABASECHANGELOG` record is coarse. Prefer one logical change each.
- **Non-unique ids.** Two changesets sharing `id`+`author` break Liquibase's tracking. Keep ids unique and descriptive (Cortex uses `2-create-allowlist`, etc.).
- **`includeAll` ordering surprises.** Globbing a directory orders files by name; a poorly-named new file can land in the wrong place. Explicit `include` (Cortex's choice) avoids this.
- **Editing an included file's old changeset.** Same sin as Chapter 2 — it's rewriting applied history (checksum mismatch, Chapter 12). Append a new file/changeset.

## 8. Practice

> **Exercise 1 — Identify the parts.** In Cortex's master, what is the master changelog, what are the included files, and where do the actual changesets live?

<details>
<summary><strong>Answer</strong></summary>

Reading Cortex's `db.changelog-master.yaml` against the three roles from §3:

- **The master changelog** is `db.changelog-master.yaml` itself — a top-level changelog that holds *no changesets of its own*. It's a pure **index** (the logbook's table of contents), listing other files in order.
- **The included files** are the two it points at: `changes/v1-init.sql` and `changes/v2-submissions.sql`, named in order via `include` entries (with `relativeToChangelogFile: true` so they're found relative to the master). These are the "chapters."
- **The actual changesets live in those included `.sql` files** — never in the master. `v1-init.sql` contains `cortex:1-create-visits`; `v2-submissions.sql` contains `cortex:2-create-submission-allowlist` and `cortex:3-create-submissions`.

The whole point of the split: the master is a clean, append-only index that *includes*, while each release's real SQL *contains* the changesets. Liquibase reads the master, expands the includes top-to-bottom, and gets one flat, ordered sequence to apply.

</details>

> **Exercise 2 — Add a release.** Sketch the master-changelog edit and the new file you'd add for a `bookmarks` table, following Cortex's pattern. Which existing files do you touch? (Hint: only one, and only to append.)

<details>
<summary><strong>Answer</strong></summary>

Following Cortex's master-includes-SQL layout, adding a `bookmarks` table is two moves — a new file, and a one-line *append* to the master.

**New file** `changes/v3-bookmarks.sql` (its own reviewable chapter, with a unique changeset id and a rollback):

```sql
--liquibase formatted sql

--changeset cortex:4-create-bookmarks
CREATE TABLE bookmarks (
  id         BIGSERIAL   PRIMARY KEY,
  username   TEXT        NOT NULL,
  book       TEXT        NOT NULL,
  chapter    TEXT        NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE bookmarks;
```

**Append to the master** `db.changelog-master.yaml` — one more `include` at the end:

```yaml
  - include:
      file: changes/v3-bookmarks.sql
      relativeToChangelogFile: true
```

**Which existing files do you touch? Exactly one — the master — and only to *append*.** You do **not** edit `v1-init.sql` or `v2-submissions.sql`; their changesets are already-applied history (the logbook page you never tear out, §2). Appending keeps the master a clean, append-only index and the prior releases immutable, which is the entire discipline the structure exists to enforce.

</details>

> **Exercise 3 — One change each.** Take a changeset that creates a table *and* backfills it *and* adds an index. Split it into separate changesets and explain what independent rollback you gain.

<details>
<summary><strong>Answer</strong></summary>

Start with the all-in-one blob and split it by *logical change*:

```sql
-- BEFORE: three unrelated logical changes crammed into one changeset
--changeset cortex:7-reports-everything
CREATE TABLE reports (id BIGSERIAL PRIMARY KEY, body TEXT NOT NULL);
INSERT INTO reports (body) SELECT summary FROM legacy_notes;   -- a backfill
CREATE INDEX reports_body_idx ON reports (body);
```

```sql
-- AFTER: one logical change per changeset, each independently revertable
--changeset cortex:7-create-reports
CREATE TABLE reports (id BIGSERIAL PRIMARY KEY, body TEXT NOT NULL);
--rollback DROP TABLE reports;

--changeset cortex:8-backfill-reports
INSERT INTO reports (body) SELECT summary FROM legacy_notes;
--rollback DELETE FROM reports;

--changeset cortex:9-index-reports
CREATE INDEX reports_body_idx ON reports (body);
--rollback DROP INDEX reports_body_idx;
```

What the split buys you (the "one logical change per changeset" rule from §3, made concrete):

- **Independent rollback.** You can drop *just* the index (`9`) to test a different one, *without* tearing down the table or re-running the expensive backfill. With the blob, rollback is all-or-nothing: undoing the index means undoing the table and the data too.
- **Independent re-application and recording.** Each changeset gets its own `DATABASECHANGELOG` row (Chapter 5), so the database records — at a useful granularity — exactly which of the three steps ran. A coarse blob records only "the whole thing ran," telling you nothing about partial state.
- **Independent review.** A reviewer can reason about the table, the backfill, and the index separately — and the genuinely risky one (the backfill, which touches data) stands on its own instead of hiding inside a DDL changeset.

The blob "works," but it forfeits all of this — which is exactly why the rule exists.

</details>

```quiz
{
  "prompt": "What is the relationship between a master changelog and changesets in Cortex's setup?",
  "input": "Choose one:",
  "options": [
    "The master changelog `include`s per-release files (in order); those files contain the actual changesets — the atomic, uniquely-identified units of change",
    "The master changelog contains all the SQL directly and changesets are unused",
    "Changesets include the master changelog",
    "They are the same thing"
  ],
  "answer": "The master changelog `include`s per-release files (in order); those files contain the actual changesets — the atomic, uniquely-identified units of change"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Changelog & changeset](https://docs.liquibase.com/concepts/changelogs/home.html)** — the structures, `include`/`includeAll`, and best practices.
- **[Liquibase — Changelog formats (XML/YAML/JSON/SQL)](https://docs.liquibase.com/concepts/changelogs/changelog-formats.html)** — the format choices behind Cortex's YAML-master-includes-SQL layout.
- **[Cortex `db.changelog-master.yaml`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/db.changelog-master.yaml)** — the real master index dissected above.

---

**Next:** Cortex writes its changesets in *plain SQL* with a couple of magic comments. Let's read `v1-init.sql` and `v2-submissions.sql` and learn the SQL-formatted changelog. → [4. SQL-formatted changesets](/cortex/production-engineering/liquibase/basics/sql-formatted-changesets)
