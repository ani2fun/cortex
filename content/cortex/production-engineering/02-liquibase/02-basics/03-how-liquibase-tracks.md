---
title: '5. How Liquibase knows what''s applied: DATABASECHANGELOG'
summary: The database keeps its own record of which changesets it has run — the DATABASECHANGELOG table. It''s how `update` is safe to run a thousand times. Watch the schema fill in changeset by changeset.
---

# 5. How Liquibase knows what's applied: DATABASECHANGELOG

## TL;DR

> When Liquibase first runs, it creates a table *in your database* called **`DATABASECHANGELOG`**. Every time it applies a changeset, it writes a row recording the changeset's **id**, **author**, a **checksum (MD5SUM)**, the **order** it ran, and a timestamp. On the next `update`, Liquibase reads this table, sees which changesets are already there, and **skips them** — running only the new ones. This is why `update` is *idempotent*: safe to run on any database, any number of times. The database carries its own memory of where it is in history. Step through it below.

## 1. Motivation

Chapter 2 said `update` should run only the changesets a database is *missing* — like `git pull` replaying missing commits. But where does the "what have I already run?" knowledge live? It can't live in the changelog file (that's the *full* history, the same for every database). It can't live in your app's memory (that's gone on restart). It has to live *in each database itself*, because each database is at a potentially different point in history — your laptop at changeset 3, staging at 2, a fresh prod at 0.

So Liquibase stores the record *inside the database it's migrating*: a bookkeeping table, `DATABASECHANGELOG`. This is the elegant, self-contained design that makes everything work: a database *knows its own state* by reading its own tracking table, so Liquibase never has to be told "this one is at changeset 3" — it asks the database. That's what makes `update` safe to run automatically on every startup (Chapter 7), idempotently, with no external coordination.

## 2. Intuition (Analogy)

A **vaccination record card** you carry. The clinic doesn't keep a central list of who's had what; *you* carry the card, and it lists exactly which shots you've received and when. Walk into any clinic, hand over the card, and they read it: "had shots 1 and 2, needs 3" — and give you only shot 3. The card travels *with you*, so any clinic can bring you up to date without knowing your history in advance.

`DATABASECHANGELOG` is the database's vaccination card. It lists exactly which changesets that database has received. Point Liquibase at the database, it reads the card, sees "has changesets 1 and 2, missing 3," and applies only changeset 3. The record lives *with the database*, so the same `update` command does the right thing for a fresh database, a half-migrated one, or a fully up-to-date one.

## 3. Formal Definition

- **`DATABASECHANGELOG`** — a table Liquibase auto-creates (on first run) in the target database. One row per *applied* changeset. Key columns:
  - `ID`, `AUTHOR`, `FILENAME` — the changeset's identity and source file.
  - `DATEEXECUTED`, `ORDEREXECUTED` — when and in what order it ran.
  - `MD5SUM` — a **checksum** of the changeset's contents at apply time (Chapter 12).
  - `EXECTYPE` — `EXECUTED`, `RERAN`, etc.
- **`DATABASECHANGELOGLOCK`** — a companion table holding a *lock*, so two Liquibase processes can't migrate the same database simultaneously (e.g. two app replicas starting at once). One acquires the lock and migrates; the other waits.
- **The `update` algorithm** (the whole core loop): read the changelog (what *should* be applied) → read `DATABASECHANGELOG` (what *has* been applied) → run, in order, every changeset whose id+author+file isn't already recorded → write a row for each → done. Run it again: nothing to do.
- **Idempotence.** Because applied changesets are recorded and skipped, `update` converges the database to the changelog's state and is safe to run repeatedly.

> The database remembers its own history in `DATABASECHANGELOG`; `update` = "apply the changesets you don't already have, record each." That self-contained memory is what makes migrations idempotent and automatable.

## 4. Worked Example — watch the schema fill in

Step through applying Cortex's real changesets to a fresh database. The **left** panel is the changelog (the changesets to apply); the **Schema** panel shows tables appearing; and the **DATABASECHANGELOG** panel shows a tracking row written for each applied changeset — the database's growing memory. Use **Apply** / **Rollback** to move through history.

```d3 widget=migration-timeline
{
  "title": "Cortex's schema, applied changeset by changeset (watch DATABASECHANGELOG fill in)",
  "changesets": [
    { "id": "1-create-visits", "author": "cortex",
      "creates": ["visits"],
      "sql": "CREATE TABLE visits (\n  id    INT    PRIMARY KEY,\n  count BIGINT NOT NULL\n);\nINSERT INTO visits (id, count) VALUES (1, 0);",
      "note": "Liquibase auto-creates DATABASECHANGELOG (and the lock table) on first run, applies this changeset, then writes a tracking row for it. The `visits` table now exists." },
    { "id": "2-create-submission-allowlist", "author": "cortex",
      "creates": ["submission_allowlist"],
      "sql": "CREATE TABLE submission_allowlist (\n  username   TEXT        PRIMARY KEY,\n  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()\n);\nINSERT INTO submission_allowlist (username) VALUES ('ani2fun'), ('tester');",
      "note": "The second changeset adds the allow-list table and seeds it. A second row appears in DATABASECHANGELOG — the database now remembers it has run changesets 1 and 2." },
    { "id": "3-create-submissions", "author": "cortex",
      "creates": ["submissions"],
      "sql": "CREATE TABLE submissions (\n  id BIGSERIAL PRIMARY KEY, username TEXT NOT NULL,\n  book TEXT NOT NULL, chapter TEXT NOT NULL, accepted BOOLEAN NOT NULL,\n  created_at TIMESTAMPTZ NOT NULL DEFAULT now()\n);\nCREATE INDEX submissions_user_chapter_idx ON submissions (username, book, chapter);",
      "note": "The third changeset creates the submissions table + its index. Three tables, three DATABASECHANGELOG rows. Run `update` again now and NOTHING happens — every changeset is already recorded." }
  ]
}
```

> Two things to watch. First, **the DATABASECHANGELOG panel grows in lockstep with the schema** — every applied changeset leaves a tracking row (with a checksum, Chapter 12). Second, imagine the cursor at the end and you run `update` *again*: Liquibase reads those three rows, finds no un-recorded changesets, and does nothing. *That's* idempotence — and it's why Cortex can safely run `update` on **every** server startup (Chapter 7), whether the database is fresh or already migrated.

## 5. Build It

Run this. It's the entire `update` algorithm, including the `DATABASECHANGELOG` table and the idempotence that falls out of it.

```python run
import hashlib

CHANGELOG = [
    {"id": "1-create-visits",      "author": "cortex", "sql": "CREATE TABLE visits ..."},
    {"id": "2-create-allowlist",   "author": "cortex", "sql": "CREATE TABLE submission_allowlist ..."},
    {"id": "3-create-submissions", "author": "cortex", "sql": "CREATE TABLE submissions ..."},
]

def md5(cs):  # Liquibase records a checksum of the changeset's contents (Chapter 12)
    return "8:" + hashlib.md5(cs["sql"].encode()).hexdigest()[:8]

def update(databasechangelog):
    applied = {(r["id"], r["author"]) for r in databasechangelog}
    ran = 0
    for cs in CHANGELOG:                                   # in order
        key = (cs["id"], cs["author"])
        if key not in applied:                            # skip if already recorded
            print(f"   applying {cs['author']}:{cs['id']}")
            databasechangelog.append({
                "id": cs["id"], "author": cs["author"],
                "order": len(databasechangelog) + 1, "md5sum": md5(cs),
            })
            ran += 1
    print(f"   ({ran} changeset(s) applied)\n")
    return databasechangelog

# A fresh database: empty DATABASECHANGELOG.
db_changelog = []
print("first `update` on a FRESH database:")
update(db_changelog)
print("   DATABASECHANGELOG:")
for r in db_changelog: print("     ", r)

print("\nsecond `update` (idempotent — nothing to do):")
update(db_changelog)        # all 3 already recorded -> 0 applied
```

**Now break it.** Run `update(db_changelog)` a third, fourth, fifth time — it always reports "0 changeset(s) applied," because the `DATABASECHANGELOG` already has all three. Then append a *fourth* changeset to `CHANGELOG` and run `update` once more: only the new one applies, and a fourth row appears. That's the production reality — Cortex's startup migration is exactly this loop, so a server that restarts ten times in a day runs the migrations *once* (on whichever start first sees a missing changeset) and skips them the other nine. The tracking table makes "migrate on every boot" completely safe.

## 6. Trade-offs & Complexity

| Tracking table (DATABASECHANGELOG) | No tracking / manual notes |
|---|---|
| Database knows its own state | You guess what's been applied |
| `update` idempotent, auto-runnable | Risk of double-applying or skipping |
| Lock table prevents concurrent migration | Two replicas could clash |
| Checksums detect altered history (Ch 12) | Silent drift |
| A small bookkeeping table in your DB | "Cleaner" DB (but no safety) |

The "cost" — two extra Liquibase-managed tables in your database — is trivial and entirely worth it: those tables are *why* migrations are safe to automate. The one thing to internalize is that `DATABASECHANGELOG` is *load-bearing state* — don't manually delete or edit its rows casually, or Liquibase will lose track and try to re-apply (or wrongly skip) changesets. Treat it as Liquibase's private memory.

## 7. Edge Cases & Failure Modes

- **Manually editing `DATABASECHANGELOG`.** Deleting a row makes Liquibase think a changeset hasn't run — it'll try to re-apply it (e.g. re-`CREATE TABLE` → error). Use Liquibase commands (`changelogSync`, `clearCheckSums` — Chapter 12), not hand-edits.
- **A stuck lock.** If a Liquibase process dies mid-migration, the `DATABASECHANGELOGLOCK` row can stay locked, blocking future runs. The fix is `liquibase releaseLocks` (after confirming nothing is actually running).
- **Concurrent replicas.** Two app instances starting together both try to migrate; the lock table ensures one waits. This is exactly why Cortex's startup migration is safe even with multiple pods (Part 4).
- **Checksum mismatch.** If a changeset's contents change *after* it's recorded, the stored `MD5SUM` no longer matches — Liquibase errors to protect you from drift (Chapter 12).

## 8. Practice

> **Exercise 1 — Trace an update.** A database's `DATABASECHANGELOG` has rows for changesets 1 and 2, and the changelog has 1–4. Which changesets does `update` run, and what rows appear afterward?

<details>
<summary><strong>Answer</strong></summary>

Run the core loop from §3: read the changelog (what *should* be applied), read `DATABASECHANGELOG` (what *has* been applied), run only the difference, in order.

- **Already applied:** changesets 1 and 2 (rows exist).
- **In the changelog:** 1, 2, 3, 4.
- **`update` runs:** **3, then 4** — the two whose id+author+file isn't yet recorded — *in changelog order*. It **skips 1 and 2** because their rows are already present.

```text
DATABASECHANGELOG before:   [1, 2]
changelog says should-have:  1, 2, 3, 4
apply (in order):                  3, 4      <- only the missing ones
DATABASECHANGELOG after:    [1, 2, 3, 4]     <- a new row per applied changeset
```

Afterward the table has **four** rows — the original 1 and 2, plus a freshly written row for 3 and one for 4. Each new row carries the changeset's id, author, filename, its `ORDEREXECUTED`, a `DATEEXECUTED` timestamp, and an `MD5SUM` checksum (§3). The database's "vaccination card" now reads "has 1–4," so a subsequent `update` would find nothing to do.

</details>

> **Exercise 2 — Why idempotent?** Explain, using `DATABASECHANGELOG`, why running `update` five times in a row applies the migrations only once. Tie it to Cortex running `update` on every boot.

<details>
<summary><strong>Answer</strong></summary>

Idempotence falls directly out of how `update` uses `DATABASECHANGELOG`. Every time `update` applies a changeset, it **writes a tracking row** for it. And before running any changeset, it **checks whether that changeset's row already exists** and skips it if so. So:

- **1st `update`** on a fresh database: no rows exist, so it applies every changeset and writes a row for each.
- **2nd through 5th `update`:** all those rows now exist, so *every* changeset is skipped — "0 changeset(s) applied" each time.

The migrations run **once** (on the first call), and the four later calls are no-ops, because "have I run this?" is answered by durable state *in the database itself*, not by hope or external coordination. `update` therefore *converges* any database to the changelog's state and is safe to run any number of times.

**Why Cortex relies on this:** Cortex runs `liquibase update` on **every server startup** (Chapter 7). A server that restarts ten times in a day would, without tracking, try to re-`CREATE TABLE` and crash on the second boot. Because `DATABASECHANGELOG` records what's applied, the *first* boot that sees a missing changeset applies it, and every subsequent boot finds nothing to do — so "migrate on every boot" is completely safe. The tracking table is precisely what makes automatic, idempotent startup migration possible.

</details>

> **Exercise 3 — The lock's job.** Two Cortex pods start simultaneously and both call `update`. Describe what `DATABASECHANGELOGLOCK` does and why, without it, you might get errors.

<details>
<summary><strong>Answer</strong></summary>

`DATABASECHANGELOGLOCK` is a companion table holding a single *lock* row, whose job is to ensure **only one Liquibase process migrates a given database at a time** (§3). When two Cortex pods boot simultaneously and both call `update`:

1. Both try to **acquire the lock** by claiming that row. The database lets exactly **one** win.
2. The **winner** holds the lock and runs the migration — reading `DATABASECHANGELOG`, applying missing changesets, writing tracking rows.
3. The **loser waits** until the lock is released, then proceeds. By the time it runs, the winner has already recorded every changeset, so the loser finds nothing to do (idempotence, Exercise 2) and exits cleanly.

**Why you'd get errors without it:** the two `update` calls would interleave. Both pods would read the *same* empty/stale `DATABASECHANGELOG`, both conclude "changeset 3 isn't applied," and both try to run `CREATE TABLE submissions` — the second hits "table already exists" and crashes. You could also get half-written tracking rows or two processes mutating schema concurrently in conflicting ways. The lock serializes them so that the read-decide-apply-record loop runs atomically, one migrator at a time.

This is exactly why Cortex's startup migration is safe **even with multiple pods** (Part 4): the first pod to boot migrates, the lock makes the others wait, and the tracking table makes their eventual `update` a no-op.

</details>

```quiz
{
  "prompt": "How does Liquibase avoid re-applying a changeset it has already run?",
  "input": "Choose one:",
  "options": [
    "It records each applied changeset (id, author, checksum) as a row in the DATABASECHANGELOG table in the database, and skips changesets already recorded — making `update` idempotent",
    "It re-runs every changeset but ignores errors",
    "It stores the history in a local file on the app server",
    "It asks the developer each time"
  ],
  "answer": "It records each applied changeset (id, author, checksum) as a row in the DATABASECHANGELOG table in the database, and skips changesets already recorded — making `update` idempotent"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — DATABASECHANGELOG table](https://docs.liquibase.com/concepts/tracking-tables/databasechangelog-table.html)** & **[DATABASECHANGELOGLOCK](https://docs.liquibase.com/concepts/tracking-tables/databasechangeloglock-table.html)** — the tracking and lock tables in full.
- **[Liquibase — `update` command](https://docs.liquibase.com/commands/update/update.html)** — the core apply loop modeled above.
- **[Cortex `Migrations.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/Migrations.scala)** — where Cortex calls `liquibase.update`, relying on this idempotence to run safely on every startup (Chapter 7).

---

**Next:** the tracking table also records *how to undo* each change. When a migration goes wrong, you need a way back — meet rollbacks, the reversal you write and the safety net you hope never to need. → [6. Rollbacks](/cortex/production-engineering/liquibase/basics/rollbacks)
