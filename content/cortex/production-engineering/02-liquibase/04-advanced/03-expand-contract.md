---
title: '11. Refactoring data safely: expand/contract'
summary: You can''t atomically rename a column that running code depends on — during a deploy, old and new code run at once. The expand/contract pattern changes a live schema across several deploys, with zero downtime.
---

# 11. Refactoring data safely: expand/contract

## TL;DR

> The hardest migration is changing a schema that *running code already uses* — like renaming a column. You can't do it atomically, because during a rolling deploy **old code and new code run at the same time**, and they'd disagree about the column name. The **expand/contract** pattern (a.k.a. parallel change) solves it across *multiple* deploys: **Expand** — add the new shape alongside the old, write to both. **Migrate** — backfill old data into the new shape; both old and new code work. **Contract** — once every running instance uses the new shape, drop the old. No single deploy ever breaks, so there's zero downtime.

## 1. Motivation

Adding a *new* table or column is easy — nothing depends on it yet. The terror is changing something *load-bearing*: renaming `users.name` to `users.full_name`, splitting a column, changing a type. The naive migration — `ALTER TABLE users RENAME COLUMN name TO full_name` — seems fine until you remember *how deploys actually work*. You don't stop the whole fleet, swap the code, and restart; you do a **rolling deploy**: new pods come up while old pods still serve, for minutes. During that window, **old code (expecting `name`) and new code (expecting `full_name`) are both running against the same database.** Rename atomically and you instantly break whichever half hasn't been swapped — a guaranteed outage.

The insight that dissolves the problem: stop trying to make the change *atomic*. Instead, make the schema *temporarily support both shapes at once*, so that at *every* moment — including the messy middle of a deploy — *both* the old and new code can function. Then retire the old shape only after no code uses it. This is expand/contract, and it's the difference between "schema changes require downtime" and "schema changes are routine and invisible." Every large system that deploys without maintenance windows lives by it.

## 2. Intuition (Analogy)

**Replacing a bridge while traffic keeps flowing.** You can't demolish the old bridge and build a new one in its place without closing the road — that's downtime. So you don't. You **build the new bridge alongside the old one** (expand). For a while, *both* bridges carry traffic — cars can take either. You gradually **route traffic onto the new bridge** and copy over anything that lived on the old one (migrate). Only once *every* car is using the new bridge do you **demolish the old one** (contract). At no point is the river uncrossable. The trick is the *overlap period* where both exist — that's what lets you switch without ever stopping traffic.

Expand/contract is exactly this for a database column. The old column is the old bridge, the new column the new bridge, and the "overlap period where both work" is what spans the rolling deploy. You never demolish the old column while code still drives over it.

## 3. Formal Definition

**Expand/contract (parallel change)** changes a schema element that running code depends on, across *three phases spread over separate deploys*, such that no single deploy breaks compatibility:

1. **Expand** (deploy 1): Add the *new* schema element (column/table) *without removing the old*. Update the application to **write to both** old and new (dual-write) while still **reading the old**. Now the database supports both shapes; old code (reading/writing old) and new code (writing both) coexist.
2. **Migrate / backfill** (between deploys): A data migration copies existing old-column data into the new column, so the new column is complete for *all* rows, not just newly-written ones. Then deploy the app to **read the new** column (still writing both, for safety).
3. **Contract** (deploy 2+): Once *every* running instance reads/writes only the new shape (the old is unused), a final migration **drops the old** column and removes the dual-write code.

Key properties:

- **Backward + forward compatible at every step.** At no point does the live schema lack what *any currently-running* code version needs.
- **Reversible mid-flight.** Because the old shape survives until contract, you can roll back a deploy without data loss.
- **Spread across deploys, not one.** The whole point is that it's *not* atomic — it's a sequence of individually-safe steps.
- **The same pattern** applies to renames, type changes, splitting/merging columns, and table extractions.

> Expand (add new, keep old, dual-write) → Migrate (backfill, switch reads) → Contract (drop old). Each deploy is independently safe because the schema supports both old and new code throughout. Zero downtime, by never making the change atomic.

## 4. Worked Example — rename `submissions.username` → `submissions.user_sub`

Suppose Cortex wants to key submissions on the stable `sub` (Part 3, Chapter 15) instead of `username`. A naive `RENAME COLUMN` would break every running pod mid-deploy. Expand/contract instead:

```d2
direction: down

expand: "EXPAND (deploy 1)" {
  s1: "ADD COLUMN user_sub TEXT  (old `username` stays)" {shape: rectangle}
  c1: "App: WRITE both username + user_sub; READ username" {shape: rectangle}
  s1 -> c1: "both shapes valid"
}
migrate: "MIGRATE (between deploys)" {
  s2: "UPDATE submissions SET user_sub = lookup(username)  (backfill)" {shape: rectangle}
  c2: "App: WRITE both; READ user_sub" {shape: rectangle}
  s2 -> c2: "new column now complete"
}
contract: "CONTRACT (deploy 2)" {
  c3: "App: WRITE + READ user_sub only" {shape: rectangle}
  s3: "DROP COLUMN username  (now unused)" {shape: rectangle}
  c3 -> s3: "old shape retired"
}

expand -> migrate -> contract: "across separate, individually-safe deploys"
```

Walk the phases as Liquibase changesets and app deploys:

- **Expand (changeset + deploy 1):** `ALTER TABLE submissions ADD COLUMN user_sub TEXT;` (nullable, old `username` untouched). Deploy app code that writes *both* columns on insert and still reads `username`. *Every running pod — old (username-only) and new (dual-write) — works,* because the old column still exists and the new one is optional.
- **Migrate (changeset + deploy):** a backfill `UPDATE submissions SET user_sub = ... WHERE user_sub IS NULL;` fills the new column for historical rows. Then deploy code that *reads* `user_sub` (still writing both). Now the new column is the source of truth, the old a safety net.
- **Contract (changeset + deploy 2):** once *no* pod references `username`, ship code that uses only `user_sub`, then a changeset `ALTER TABLE submissions DROP COLUMN username;`. The old shape is gone, the rename complete — and at no point did a deploy break.

Notice this is *three* small, reviewed changesets and *three* deploys, not one risky `RENAME`. That's the trade expand/contract makes: more steps, each boring and safe, instead of one step that's fast and catastrophic. (And it's exactly why a *slow backfill* shouldn't run at startup — Chapter 7 — but as a separate migration step, so booting pods stay fast.)

## 5. Build It

Run this. It simulates a rolling deploy where old and new code run simultaneously, and shows the naive rename breaking while expand/contract survives.

```python run
# A row is a dict. "Old code" reads `username`; "new code" reads `user_sub`.
def old_code_read(row): return row["username"]          # expects the OLD column
def new_code_read(row): return row["user_sub"]          # expects the NEW column

# --- NAIVE atomic rename: drop username, add user_sub in one step ---
row = {"username": "ada"}
print("== naive RENAME, mid rolling-deploy (both code versions live) ==")
renamed = {"user_sub": row["username"]}                  # username GONE
print("   new code reads:", new_code_read(renamed))      # works
try:
    print("   old code reads:", old_code_read(renamed))  # BREAKS — column gone
except KeyError as e:
    print(f"   old code BREAKS: missing {e}  -> OUTAGE for not-yet-upgraded pods\n")

# --- EXPAND/CONTRACT: keep both columns during the overlap ---
print("== expand/contract ==")
row = {"username": "ada"}                                # start state
# EXPAND: add user_sub, dual-write, keep username
row["user_sub"] = row["username"]                        # dual-write
print("   expand: both work -> old:", old_code_read(row), "| new:", new_code_read(row))
# MIGRATE: backfill done; switch reads to new (still dual-writing)
print("   migrate: new column complete; reads switch to user_sub")
# CONTRACT: only after NO code reads username, drop it
del row["username"]
print("   contract: drop username ->", "new still works:", new_code_read(row))
print("\nAt no step did a live code version break — zero downtime.")
```

**Now break it.** Try to "speed up" expand/contract by doing the `del row["username"]` (contract) *during* the expand phase — immediately, old code breaks again, because you removed the old shape while old code still needs it. The discipline that makes expand/contract safe is *patience*: the old shape may only be dropped once you've **proven no running code uses it**, which means waiting out the deploy(s). Rushing the contract is just the naive rename in disguise. The safety is entirely in the *sequencing and the overlap*, not in any one clever SQL statement.

## 6. Trade-offs & Complexity

| Expand/contract | Naive atomic change |
|---|---|
| Zero downtime, even mid-deploy | Breaks not-yet-upgraded code → outage |
| Reversible at each step | Hard to roll back safely |
| Each deploy individually safe | One all-or-nothing risky step |
| 3 changesets + 3 deploys + dual-write code | 1 changeset |
| Temporary dual-write complexity | None |

The cost is real and not small: a single conceptual change ("rename a column") becomes three deploys, multiple changesets, and a period of *dual-write code* you later remove. For a tiny app with maintenance windows (you can just take it down for 30 seconds), that's overkill — do the simple rename during downtime. But for any system that deploys continuously without downtime, expand/contract isn't optional; it's the *only* safe way to evolve load-bearing schema. The complexity is the price of never taking the system down — and for most production services, that price is worth paying.

## 7. Edge Cases & Failure Modes

- **Contracting too early.** Dropping the old shape before *every* instance has stopped using it is the naive rename in disguise. Confirm (via metrics/logs) that nothing reads the old column before contract.
- **Forgetting the backfill.** Adding the new column but never backfilling historical rows means old data has `NULL` in the new column — new code that assumes it's populated breaks. Backfill before switching reads.
- **Backfill locking a huge table.** A single `UPDATE` over 500M rows can lock the table for a long time. Backfill in *batches* (e.g. 10k rows at a time) to avoid a long lock — and don't run it at startup (Chapter 7).
- **Leaving dual-write code forever.** The contract phase includes *removing* the dual-write and the old column. Skipping cleanup leaves cruft and a misleading schema. Finish the contract.

## 8. Practice

> **Exercise 1 — Name the phases.** For changing a column's *type* (e.g. `INT` → `BIGINT`) that code depends on, write the expand, migrate, and contract steps.

<details>
<summary><strong>Answer</strong></summary>

A type change is just a rename to a differently-typed column — same three phases, each its own deploy, so the schema supports both shapes throughout (§3). Say the column is `count INT` becoming `BIGINT`:

- **Expand (deploy 1):** add the new column alongside the old — `ALTER TABLE t ADD COLUMN count_big BIGINT;` — and deploy app code that **dual-writes** (writes both `count` and `count_big`) while still **reading** the old `count`. Old pods (INT-only) and new pods (dual-write) both work.
- **Migrate (between deploys):** **backfill** historical rows — `UPDATE t SET count_big = count WHERE count_big IS NULL;` (in *batches* for a big table, §7) — so `count_big` is complete for every row. Then deploy code that **reads `count_big`** (still writing both, as a safety net).
- **Contract (deploy 2+):** once *no* running pod references the old `count`, ship code that uses only `count_big`, then `ALTER TABLE t DROP COLUMN count;` (and, if you want the original name back, a later expand/contract to rename `count_big` → `count`).

At no single deploy does the live schema lack what a running code version needs — that's what makes it zero-downtime.

</details>

> **Exercise 2 — Why not atomic?** In one paragraph, explain precisely why a rolling deploy makes an atomic `RENAME COLUMN` unsafe, referencing the moment old and new code coexist.

<details>
<summary><strong>Answer</strong></summary>

A rolling deploy never swaps the whole fleet at once: new pods come up *while old pods keep serving*, for minutes, so during that window **old code (expecting `name`) and new code (expecting `full_name`) are both running against the same database**. An atomic `ALTER TABLE ... RENAME COLUMN name TO full_name` makes the change *instantaneous and total* — the very moment it commits, the column has exactly one name. Whichever half of the fleet hasn't been swapped is now wrong: if you rename to `full_name`, every still-running old pod queries a `name` column that no longer exists and breaks; if you'd somehow kept `name`, the new pods break instead. There's no instant at which a single column name satisfies both code versions, so the rename guarantees an outage for the not-yet-upgraded half. Expand/contract dissolves this by making the schema *temporarily support both shapes*, so the messy overlap is survivable.

</details>

> **Exercise 3 — Spot the rush.** A teammate proposes "expand and contract in the same deploy to save time." Explain why that's just the naive rename, and what guarantees they're giving up.

<details>
<summary><strong>Answer</strong></summary>

Expand *adds* the new shape; contract *drops* the old. Do both **in one deploy** and the old shape exists and is gone within that single rollout — which is precisely an atomic rename with extra steps. The safety of expand/contract lives entirely in the **overlap period**: the old shape must survive *until you've proven no running code uses it*, which means waiting out the deploy(s). Collapse expand and contract together and there is no overlap — old pods still serving during the rollout lose the old column mid-flight and break, exactly the outage the pattern exists to prevent.

The guarantees they give up:

- **Backward/forward compatibility at every step** — there's now a moment the live schema lacks what running old code needs.
- **Reversibility mid-flight** — once the old column is dropped in the same deploy, a rollback has no old shape to fall back to, risking data loss.
- **Zero downtime** — the whole point. "Saving time" here just buys back the catastrophic-but-fast failure mode expand/contract was designed to trade away.

The discipline that makes it safe is *patience*: drop the old shape only after metrics/logs confirm nothing reads it (§7). Rushing the contract is the naive rename wearing a disguise.

</details>

```quiz
{
  "prompt": "Why does renaming a column that running code uses require the expand/contract pattern instead of a single `ALTER TABLE RENAME`?",
  "input": "Choose one:",
  "options": [
    "During a rolling deploy old and new code run simultaneously; expand/contract keeps the schema compatible with BOTH (add new + keep old + dual-write, then backfill, then drop old) so no deploy ever breaks",
    "RENAME COLUMN is not valid SQL",
    "Liquibase can't rename columns",
    "Renaming is slower than adding a column"
  ],
  "answer": "During a rolling deploy old and new code run simultaneously; expand/contract keeps the schema compatible with BOTH (add new + keep old + dual-write, then backfill, then drop old) so no deploy ever breaks"
}
```

## In the Wild

- **[Martin Fowler — ParallelChange (expand/contract)](https://martinfowler.com/bliki/ParallelChange.html)** — the canonical description of the pattern.
- **[Liquibase — Refactoring data without downtime](https://www.liquibase.com/blog/zero-downtime-database-migrations)** — applying expand/contract with Liquibase changesets.
- **[GitHub Engineering — online schema migrations (gh-ost)](https://github.blog/2016-08-01-gh-ost-github-s-online-migration-tool-for-mysql/)** — how a huge system does zero-downtime schema change in practice (the same principles, industrial scale).

---

**Next:** Liquibase protects you from accidentally rewriting applied history with **checksums** — and when they "bite," it can be confusing. Learn what they are, why they fire, and how to recover. → [12. Checksums, when they bite, and how to recover](/cortex/production-engineering/liquibase/advanced/checksums)
