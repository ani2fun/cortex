---
title: '6. Rollbacks'
summary: A rollback is the defined reversal of a changeset — the undo button for your schema. Liquibase auto-reverses known change types and lets you write `--rollback` for raw SQL. Learn to roll back by count, tag, or date.
---

# 6. Rollbacks

## TL;DR

> A **rollback** is the reversal of a changeset — how you *undo* a schema change. For structured change types Liquibase generates the reversal automatically (the opposite of "create table" is "drop table"); for raw SQL it can't guess, so you write **`--rollback <sql>`** yourself (as Cortex does on every changeset). You roll back **by count** (`rollbackCount 2`), **by tag** (`rollback v1.2`), or **by date**. Some changes are irreversible (a `DROP COLUMN` discards data) — and *that realization, forced by writing the rollback, is itself valuable.* Rollbacks are the safety net you design before you need it.

## 1. Motivation

Migrations move your schema *forward*. But deploys go wrong: a changeset has a bug, a release is pulled, a migration half-applies and you need to back out. Without a defined way *back*, your only options are panic and a restore-from-backup (slow, lossy). A rollback is the planned reversal — the schema equivalent of `git revert` — that lets you undo a change cleanly and quickly.

But rollbacks do something subtler and more important than provide an undo button: **writing the rollback forces you to think about reversibility at design time.** When you write `--rollback DROP TABLE submissions`, you confront, *before deploying*, the question "can this even be undone, and what happens to the data?" That confrontation catches a whole class of disasters — the irreversible `DROP COLUMN`, the data-losing type change — *while you're writing it*, not at 2 a.m. mid-incident. The rollback is both a safety net and a forcing function for careful schema design.

## 2. Intuition (Analogy)

**Undo (Ctrl-Z) — but you sometimes have to record the undo yourself.** A good editor knows how to reverse most actions: type a letter, Ctrl-Z deletes it. But some actions don't have an obvious inverse — "I flattened these three layers into one; how do you un-flatten them?" — and the editor either remembers the prior state or asks *you* to define the reversal.

Liquibase is the same. For structured changes it knows the inverse automatically (the undo of "add column" is "drop column"). For arbitrary SQL — "I ran this custom migration script" — it *can't* infer the reverse, so you supply it with `--rollback`. And crucially, some actions are genuinely *irreversible*: once you shred a document, "undo" can't bring it back. A `DROP COLUMN` that deletes a million rows of data is shredding — no rollback SQL can resurrect data that's gone. Recognizing *which* of your changes are reversible and which are shredding is the skill this chapter builds.

## 3. Formal Definition

- **Rollback.** The defined reversal of a changeset, restoring the schema to its state *before* that changeset applied. Stored with the changeset.
- **Automatic rollback.** For Liquibase's structured change types (`createTable`, `addColumn`, `createIndex`, …), Liquibase generates the inverse automatically. SQL-format changesets are raw SQL, so they *don't* get this — you write it.
- **`--rollback <sql>`.** In SQL format, the comment supplying the reversal SQL for the preceding changeset. (XML/YAML use a `<rollback>` block.)
- **Rollback commands:**
  - `rollbackCount <n>` — undo the last `n` changesets.
  - `rollback <tag>` — undo everything applied *after* a named **tag** (a labeled point in history, like a git tag).
  - `rollbackToDate <date>` — undo everything applied after a date.
  - `updateRollback`/`rollback-sql` — *preview* the rollback SQL without running it (do this first!).
- **Tags.** A `tagDatabase` operation marks a point in history (e.g. before a risky release) so you can roll back *to exactly there* later.
- **Irreversible changes.** Some changes lose data (`DROP COLUMN`, destructive `UPDATE`). Their "rollback" can restore the *structure* but not the *data* — which must come from a backup. Liquibase can't manufacture lost data; no tool can.

> A rollback is a changeset's defined inverse — auto-generated for structured changes, hand-written (`--rollback`) for raw SQL. Roll back by count, tag, or date. Always preview first, and respect that some changes can't truly be undone.

## 4. Worked Example — Cortex's rollbacks and rolling back

Every Cortex changeset carries its reversal. From `v2-submissions.sql`:

```sql
--changeset cortex:3-create-submissions
CREATE TABLE submissions ( ... );
CREATE INDEX submissions_user_chapter_idx ON submissions (...);
--rollback DROP TABLE submissions;          -- the defined reversal (drops the table AND its index)
```

The `--rollback DROP TABLE submissions;` is Cortex telling Liquibase exactly how to undo changeset 3. (Dropping the table also drops its index, so one statement reverses both operations.) Now suppose changeset 3 shipped with a bug and you need to back it out:

```text
# Preview FIRST — never roll back blind:
liquibase rollback-count-sql 1        # prints: DROP TABLE submissions;  (and removes its DATABASECHANGELOG row)

# Then actually roll it back:
liquibase rollbackCount 1             # undoes the last 1 changeset
```

Liquibase looks up the last applied changeset in `DATABASECHANGELOG` (Chapter 5), runs its `--rollback` SQL, and *removes its tracking row* — so the database is provably back to its pre-changeset-3 state, and a future `update` would re-apply changeset 3 cleanly. For a planned risky release, you'd first `tagDatabase before-v3`, then later `rollback before-v3` to undo *everything* after that tag in one command — a precise, named recovery point.

But notice what writing `--rollback DROP TABLE submissions` *revealed*: rolling back changeset 3 **deletes every submission row**. For a brand-new empty table that's fine. If `submissions` were full of users' accepted solutions, that rollback is *data loss* — and the right design might be to *not* drop the table on rollback, but to handle the bug forward (a new changeset) instead. The act of writing the rollback surfaced that the change is destructive to undo. That's the forcing function at work.

## 5. Build It

Run this. It models forward/rollback as a stack, shows rolling back by count, and exposes the irreversibility problem.

```python run
schema, changelog_table = set(), []          # current tables, and DATABASECHANGELOG

CHANGESETS = [
    {"id": "1-visits",      "forward": lambda s: s.add("visits"),
     "rollback": lambda s: s.discard("visits")},
    {"id": "2-allowlist",   "forward": lambda s: s.add("submission_allowlist"),
     "rollback": lambda s: s.discard("submission_allowlist")},
    {"id": "3-submissions", "forward": lambda s: s.add("submissions"),
     "rollback": lambda s: s.discard("submissions")},   # NOTE: drops the table -> data loss in real life
]

def update():
    for cs in CHANGESETS:
        if cs["id"] not in {r["id"] for r in changelog_table}:
            cs["forward"](schema); changelog_table.append({"id": cs["id"]})

def rollback_count(n):
    for _ in range(n):
        if not changelog_table: break
        last = changelog_table.pop()                       # most recent first
        cs = next(c for c in CHANGESETS if c["id"] == last["id"])
        print(f"   rolling back {cs['id']}")
        cs["rollback"](schema)                              # run its reversal

update()
print("after update — schema:", sorted(schema))
print("DATABASECHANGELOG:", [r["id"] for r in changelog_table], "\n")

rollback_count(1)                                          # undo the last changeset
print("after rollbackCount 1 — schema:", sorted(schema))
print("DATABASECHANGELOG:", [r["id"] for r in changelog_table])
print("\n⚠ In reality, rolling back '3-submissions' DROPs the table — any rows are GONE.")
```

**Now break it.** Add a changeset whose forward step is `s.add("phone_data")` but whose `rollback` is `lambda s: s` (a no-op, because you can't un-delete data a real version would have lost). Roll it back: the schema "reverses" but the lost data is *not* restored — modeling the truth that a rollback restores *structure*, not *vanished data*. The lesson the model teaches in code: before you write a destructive change, decide whether its rollback is *honest* (truly reversible) or a *fiction* (structure-only), and if it's a fiction, prefer fixing forward over rolling back.

## 6. Trade-offs & Complexity

| Defined rollbacks | No rollback plan |
|---|---|
| Fast, clean back-out of a bad change | Panic + restore-from-backup |
| Forces reversibility thinking at design time | Discover irreversibility mid-incident |
| Roll back by count/tag/date precisely | Coarse, lossy recovery |
| You must *write* rollbacks for raw SQL | Less to write (until you need it) |
| Tags give named recovery points | No precise "undo to here" |

The cost is writing `--rollback` for every SQL changeset (Liquibase can't auto-generate it from raw SQL) and the discipline to *test* rollbacks, not just assume they work. The payoff is a fast, precise undo and — more valuably — being *forced* to confront reversibility before you deploy. Many teams also conclude that for *destructive* changes, "roll forward with a fix" beats "roll back into data loss," and use the rollback mainly for the reversible structural changes. Writing the rollback is what makes you decide which camp each change is in.

## 7. Edge Cases & Failure Modes

- **No `--rollback` on a SQL changeset.** Attempting to roll it back fails — Liquibase has no reversal. Write one, even if it's "this is irreversible, restore from backup" documented intent.
- **Irreversible changes presented as reversible.** A `--rollback` that recreates a dropped column restores the *structure* but not the *data*. Don't let a tidy rollback fool you into thinking a destructive change is safe to undo.
- **Rolling back without previewing.** Always run the `*-sql` preview first; a wrong rollback can do as much damage as a wrong migration.
- **Rollback across a release with data changes.** Rolling back schema while leaving (or losing) data can leave the system inconsistent. For risky changes, prefer expand/contract (Chapter 11) and forward fixes.

## 8. Practice

> **Exercise 1 — Write the inverse.** Give the `--rollback` for each: (a) `CREATE TABLE foo (...)`; (b) `ALTER TABLE users ADD COLUMN phone TEXT`; (c) `CREATE INDEX idx ON t (col)`. Which one, reversed, loses data, and when?

<details>
<summary><strong>Answer</strong></summary>

Each rollback is the *inverse* of the forward change — what restores the schema to its pre-changeset state (§3):

- **(a)** `CREATE TABLE foo (...)` → `--rollback DROP TABLE foo;`
- **(b)** `ALTER TABLE users ADD COLUMN phone TEXT` → `--rollback ALTER TABLE users DROP COLUMN phone;`
- **(c)** `CREATE INDEX idx ON t (col)` → `--rollback DROP INDEX idx;`

**Which loses data, and when?** (c) is purely structural — an index is *derived* from the table's rows, so dropping it loses nothing; the data is untouched and the index can be rebuilt any time. The data-losing ones are **(a) and (b), and the distinction is whether data has landed yet:**

- **(b)** is the clearest trap: if `phone` has been *populated*, `DROP COLUMN phone` discards every value in it. Reversed on an empty column it's clean; reversed after real phone numbers were written, it's data loss the rollback can't undo (§3 — a rollback restores *structure*, not vanished data).
- **(a)** loses data too once `foo` holds rows: `DROP TABLE foo` takes the table *and everything in it*. Empty table → harmless; full table → every row is gone.

The unifying rule: dropping a *structure* (an index) is always safe to reverse, but dropping a *container of data* (a column, a table) is only a clean undo while it's empty. The moment it holds data, the "reversal" is destructive — which is exactly the realization writing the `--rollback` is meant to force on you *before* you deploy.

</details>

> **Exercise 2 — Tag and recover.** Describe how you'd use `tagDatabase` before a risky release so that, if it goes wrong, you can roll the database back to *exactly* the pre-release point in one command.

<details>
<summary><strong>Answer</strong></summary>

A **tag** is a labeled point in history — a named bookmark in `DATABASECHANGELOG`, the schema equivalent of a git tag (§3). The recipe:

1. **Before the risky release, mark the current point:**

   ```text
   liquibase tag before-v3        # labels the last-applied changeset as "before-v3"
   ```

   This records the marker *now*, while the database is in its known-good, pre-release state.

2. **Deploy the release** — `liquibase update` applies the new changesets (say 3, 4, 5) on top of the tag.

3. **If it goes wrong, preview, then roll back to the tag in one command:**

   ```text
   liquibase rollback-sql before-v3   # PREVIEW first — print the reversal SQL, run nothing (§7)
   liquibase rollback before-v3       # undo EVERYTHING applied AFTER the tag, in reverse order
   ```

   `rollback <tag>` walks `DATABASECHANGELOG` backward, running each post-tag changeset's `--rollback` (5, then 4, then 3) and removing its tracking row, until the database is *exactly* back at the `before-v3` marker.

Why this beats `rollbackCount`: you don't have to *count* how many changesets the release added (easy to get wrong, especially if the count varied across environments). The tag names the recovery point precisely, so "undo to exactly there" is one command regardless of how many changesets landed after it. Always run the `*-sql` preview first — a wrong rollback can do as much damage as a wrong migration (§7) — and remember the usual caveat: this restores *structure* to the tagged point, but any data destroyed by the post-tag changesets won't come back.

</details>

> **Exercise 3 — Roll back or roll forward?** A changeset dropped a column that's now full of customer data, and it shipped. Argue whether to roll back or to "fix forward" with a new changeset, and what each does to the data.

<details>
<summary><strong>Answer</strong></summary>

The crucial fact first: the destructive change **already shipped and ran**, so the column's data is *already gone*. The decision isn't "how do I avoid losing data" — it's "given the data is lost, which recovery is least bad." Neither path can resurrect data from the schema alone; a rollback restores *structure*, not *vanished data* (§3).

**What each option does to the data:**

- **Roll back** (`rollbackCount`/`rollback <tag>`, running the changeset's reversal). The reversal of `DROP COLUMN` is `ADD COLUMN`, so this **re-creates the column — empty**. It restores the *shape* the old code expects, which may stop immediate errors, but the customer data is *still missing*. It does **not** undo the deletion; it just puts an empty container back.
- **Fix forward** (a *new* changeset that re-adds the column, then repopulates it). The `ADD COLUMN` again gives you an empty column — but now you also write the step that *refills* it, from the **only place the data still exists: a backup** (or a replica/audit log, if one captured it). A new changeset like `ADD COLUMN phone TEXT;` followed by a backfill from restored data is the *only* route that actually recovers the customer values.

**Argument:** prefer **fix forward**. Rolling back here is a tidy-looking *fiction* — it makes the schema reversible on paper while the data stays lost, and it muddies history by un-recording the changeset (§7's "irreversible changes presented as reversible"). Fixing forward is honest: it keeps the history append-only (a pushed commit you don't rewrite, Chapter 2), and it's the only option that confronts the real problem — *getting the data back from a backup*. This is the general lesson of the chapter: for *destructive* changes, "roll forward with a fix" beats "roll back into a false sense of recovery." (And the way to never be here again is to have caught it at design time, when writing the `--rollback` should have flagged the `DROP COLUMN` as data-losing.)

</details>

```quiz
{
  "prompt": "For a SQL-formatted changeset, why must you write a `--rollback` yourself, and what can a rollback NOT do?",
  "input": "Choose one:",
  "options": [
    "Liquibase can't infer how to reverse arbitrary SQL, so you supply it; and a rollback restores structure but cannot bring back data that a destructive change deleted",
    "Liquibase always auto-generates rollbacks, so you never write one",
    "A rollback restores all deleted data automatically",
    "Rollbacks only work on XML changelogs"
  ],
  "answer": "Liquibase can't infer how to reverse arbitrary SQL, so you supply it; and a rollback restores structure but cannot bring back data that a destructive change deleted"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Rollback](https://docs.liquibase.com/workflows/liquibase-community/using-rollback.html)** — `--rollback`, `rollbackCount`, `rollback <tag>`, `rollbackToDate`, and previewing.
- **[Liquibase — Tagging your database](https://docs.liquibase.com/commands/utility/tag.html)** — named recovery points for precise rollbacks.
- **[Cortex `v2-submissions.sql`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v2-submissions.sql)** — every changeset carries its `--rollback`, the reversal dissected above.

---

**Next:** we've written and reversed changesets. Now, *when* do they run? Cortex applies migrations automatically at server startup — before it serves a single request. Let's read that. → [7. Running on startup](/cortex/production-engineering/liquibase/running-migrations/running-on-startup)
