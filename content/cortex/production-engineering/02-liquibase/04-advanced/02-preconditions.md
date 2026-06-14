---
title: '10. Preconditions'
summary: A precondition is a guard on a changeset — "only run if the table is empty" or "only on PostgreSQL." They make migrations defensive against unexpected database states and let you adapt to reality safely.
---

# 10. Preconditions

## TL;DR

> A **precondition** is a check Liquibase runs *before* a changeset, deciding whether it's safe to apply. "Only run if the `users` table exists," "only on PostgreSQL," "only if this table is empty." If the precondition isn't met, you choose what happens via **`onFail`**: `HALT` (stop — the default), `CONTINUE` (skip this changeset, try later), `MARK_RAN` (skip and record it as done), or `WARN` (log and proceed). Preconditions make migrations *defensive* — they adapt to the real state of a database instead of blindly assuming it, which matters when databases have history you didn't fully control.

## 1. Motivation

A changeset encodes an assumption about the database's state: "there's a `users` table to alter," "this column doesn't exist yet." Usually that assumption holds because the prior changesets established it. But not always. You might be adopting Liquibase on a database that *already has tables* (created before you started tracking). A changeset might need to behave differently if data is already present. A SQL feature might only exist on one database engine. Blindly running a changeset against a state it didn't expect produces confusing errors ("table already exists," "column not found") or, worse, applies a change in a context where it's wrong.

Preconditions let a changeset *check its assumptions and react*. Instead of "always `CREATE TABLE users`" (which fails if it exists), you write "if the `users` table does *not* exist, create it." Instead of "always run this Postgres-specific index," you write "only on PostgreSQL." The changeset becomes *self-aware* about the state it requires, and you control — explicitly — what happens when reality differs. This is how you safely adopt Liquibase onto an existing database, write changesets that tolerate partial history, and guard environment-specific logic.

## 2. Intuition (Analogy)

A **"before you operate" surgical checklist.** Before a surgeon makes an incision, the team runs preconditions: *Is this the right patient? The right site? Are they allergic to this anesthetic?* If a check fails, there's a defined protocol — *halt and resolve* (the default, for safety), or in some cases *note it and adapt the plan*. The checklist exists precisely because operating on the *assumption* that everything's fine — without verifying — is how catastrophic mistakes happen. You verify the state *before* the irreversible action.

A precondition is that pre-operation check for a schema change. *Before* the changeset runs its DDL, it verifies the database is in the expected state. If not, the `onFail` protocol decides: halt (stop and let a human resolve), skip, mark-as-done, or warn-and-continue. The changeset doesn't operate blind.

## 3. Formal Definition

- **Precondition.** A check evaluated *before* a changeset applies, controlling whether it runs. Declared per-changeset (or per-changelog). In SQL format: `--precondition-sql-check expectedResult:0 SELECT count(*) FROM ...`; structured formats have `<preConditions>` with typed checks.
- **Common precondition types:**
  - `tableExists` / `columnExists` / `not` (negation) — structural checks.
  - `sqlCheck expectedResult:N` — run a query, require a specific result (e.g. row count 0).
  - `dbms type:postgresql` — only on a given database engine.
  - `changeSetExecuted` — only if another changeset already ran.
- **`onFail` (and `onError`) behaviors** — what to do when the precondition is *not met*:
  - `HALT` (**default**) — stop the whole migration with an error. Safe default: an unmet assumption is a problem.
  - `CONTINUE` — skip *this* changeset for now; it may apply on a later run when the precondition is met.
  - `MARK_RAN` — skip it *and record it as applied* in `DATABASECHANGELOG` (useful when adopting Liquibase: "this table already exists, so consider this changeset done").
  - `WARN` — log a warning and run the changeset anyway.
- **Idempotent changesets.** A common use: `not tableExists onFail:MARK_RAN` before a `CREATE TABLE`, so the changeset is a no-op if the table already exists — letting you safely "baseline" an existing database.

> A precondition checks the database's state before a changeset runs; `onFail` decides what to do if the state is wrong (halt, skip, mark-ran, or warn). It makes migrations defensive and adoptable onto existing databases.

## 4. Worked Example — adopting Liquibase onto an existing database

The classic use: you're putting Liquibase onto a database that *already has* some tables (created before you tracked anything). A naive `CREATE TABLE visits` would fail ("already exists"). A precondition makes it safe:

```sql
--liquibase formatted sql

--changeset cortex:1-create-visits
--precondition-onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'visits'
CREATE TABLE visits (id INT PRIMARY KEY, count BIGINT NOT NULL);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;
```

Read the guard: *before* creating `visits`, run `SELECT count(*) ... WHERE table_name = 'visits'` and require the result to be `0` (i.e. the table does *not* exist). If it's `0`, the table is absent → the changeset runs and creates it. If it's `1` (the table already exists, from before Liquibase), the precondition *fails*, and `onFail:MARK_RAN` says "skip the DDL but **record the changeset as applied**." Now `DATABASECHANGELOG` shows changeset 1 as done — *without* trying to recreate the existing table — so future `update`s proceed cleanly from this baseline. You've adopted Liquibase onto a pre-existing schema without a single "already exists" error.

A second common pattern is environment/engine guards: `--precondition-sql-check ... dbms type:postgresql` ensures a Postgres-specific changeset (a partial index, a `TIMESTAMPTZ` quirk) only runs on Postgres, complementing the contexts of Chapter 9. Together, contexts (*which* environment) and preconditions (*what state* the database is in) let your one changelog adapt safely to reality.

## 5. Build It

Run this. It implements preconditions with the `onFail` behaviors, including `MARK_RAN` for baselining an existing database.

```python run
def precondition_table_absent(name, existing_tables):
    return name not in existing_tables          # "not tableExists"

def apply_changeset(cs, schema, changelog_table):
    pre = cs.get("precondition")
    if pre and not pre["check"](schema):
        on_fail = pre["onFail"]
        if on_fail == "HALT":
            raise RuntimeError(f"precondition failed for {cs['id']} — HALT")
        if on_fail == "MARK_RAN":
            print(f"   {cs['id']}: precondition unmet -> MARK_RAN (recorded as done, DDL skipped)")
            changelog_table.append(cs["id"]); return
        if on_fail == "CONTINUE":
            print(f"   {cs['id']}: precondition unmet -> CONTINUE (skipped, not recorded)"); return
    print(f"   {cs['id']}: precondition OK -> applying")
    cs["apply"](schema); changelog_table.append(cs["id"])

# A database that ALREADY HAS `visits` (created before Liquibase) but not `submissions`.
existing = {"visits"}
log = []

changesets = [
    {"id": "1-create-visits",
     "precondition": {"check": lambda s: precondition_table_absent("visits", s), "onFail": "MARK_RAN"},
     "apply": lambda s: s.add("visits")},
    {"id": "2-create-submissions",
     "precondition": {"check": lambda s: precondition_table_absent("submissions", s), "onFail": "MARK_RAN"},
     "apply": lambda s: s.add("submissions")},
]

print("adopting Liquibase onto a database that already has `visits`:")
for cs in changesets:
    apply_changeset(cs, existing, log)
print("\nschema:", sorted(existing))
print("DATABASECHANGELOG:", log, "  (both changesets recorded — clean baseline)")
```

**Now break it.** Change changeset 1's `onFail` to `"HALT"` and re-run against the database that already has `visits`. The migration now *aborts* — because the precondition "visits is absent" is false and HALT says "stop on an unmet assumption." That's the *right* default for most changesets (an unexpected state should stop you), and exactly *wrong* for baselining, where you *expect* the table to exist and want `MARK_RAN`. Choosing the `onFail` behavior to match your intent — strict by default, lenient where you're deliberately adapting — is the skill.

## 6. Trade-offs & Complexity

| Preconditions (defensive) | Assume-the-state changesets |
|---|---|
| Adapt safely to real DB state | "already exists" / "not found" errors |
| Adopt Liquibase onto existing DBs | Painful baselining |
| Engine/environment guards | Wrong-engine SQL fails confusingly |
| Idempotent, re-runnable changesets | Brittle to partial history |
| Extra checks to write and reason about | Simpler changesets (until reality differs) |

The cost is added complexity per changeset — a precondition is more to write, and the `onFail` behaviors take thought (the wrong one can either halt unexpectedly or silently skip a needed change). Used judiciously — for baselining, engine guards, and genuinely state-dependent logic — they're invaluable. Used everywhere, they make changesets hard to read and can mask real problems (a `WARN` that hides a failure). Most clean, greenfield changesets (like Cortex's, applied to fresh databases) need *no* preconditions; reach for them when the database's state is genuinely uncertain.

## 7. Edge Cases & Failure Modes

- **`MARK_RAN` hiding a real divergence.** Marking a changeset as ran when its precondition failed assumes the desired state already exists. If it *doesn't* (a different table by that name), you've recorded a lie. Use `MARK_RAN` only when you're sure the precondition failing means "already done."
- **`WARN`/`CONTINUE` masking failures.** Permissive `onFail` can let a migration "succeed" while skipping needed changes. Default to `HALT` unless you have a specific reason.
- **Slow precondition queries.** A precondition that runs an expensive query on a huge table adds time to every migration check. Keep them cheap.
- **Over-guarding clean changesets.** Wrapping every greenfield changeset in `not tableExists` clutters the changelog and signals uncertainty that isn't there. Guard only genuinely uncertain changesets.

## 8. Practice

> **Exercise 1 — Pick the `onFail`.** For each, choose `HALT`, `CONTINUE`, or `MARK_RAN`: (a) baselining a table that already exists; (b) a changeset that requires a prior one that's missing; (c) a Postgres-only index running against MySQL by mistake.

<details>
<summary><strong>Answer</strong></summary>

Match the `onFail` to your *intent*: strict by default, lenient only where you're deliberately adapting (§3).

- **(a) Baselining a table that already exists → `MARK_RAN`.** The precondition "table is absent" fails *because the table is already there from before Liquibase*. You want to **skip the DDL but record the changeset as applied**, so `DATABASECHANGELOG` shows a clean baseline and future runs proceed — exactly the adoption pattern.
- **(b) Requires a missing prior changeset → `HALT`.** A needed prerequisite isn't there: that's a genuinely broken assumption, and the safe default is to **stop and let a human resolve it** rather than charge ahead into an undefined state. (`CONTINUE` would only fit if the prerequisite is *expected later* and this changeset can safely retry on a future run.)
- **(c) Postgres-only index hitting MySQL → `HALT`** (and really, guard it with `dbms type:postgresql`). Running Postgres-specific SQL on MySQL is a mistake you *want* surfaced loudly, not silently skipped — halting forces you to confront the wrong-engine deploy. A permissive `WARN`/`CONTINUE` here would hide a real problem.

The skill is choosing the behavior to match intent: `MARK_RAN` for "already done," `HALT` for "an unexpected state should stop me."

</details>

> **Exercise 2 — Write a guard.** Write a precondition (in pseudo-SQL-format) that makes a `CREATE TABLE bookmarks` safe to run whether or not the table already exists.

<details>
<summary><strong>Answer</strong></summary>

Guard the `CREATE` with a "table is absent" check and `onFail:MARK_RAN`, so it creates the table when missing and records-as-done (skipping the DDL) when it already exists:

```sql
--changeset cortex:5-create-bookmarks
--precondition-onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'bookmarks'
CREATE TABLE bookmarks (sub TEXT NOT NULL, book TEXT NOT NULL, chapter TEXT NOT NULL, PRIMARY KEY (sub, book, chapter));
--rollback DROP TABLE bookmarks;
```

How it behaves: the check requires `count(*) = 0` (the table does **not** exist). If it's `0`, the precondition passes → the `CREATE TABLE` runs. If it's `1` (the table is already there), the precondition *fails* and `onFail:MARK_RAN` says "skip the DDL but **record the changeset as applied**" — no "already exists" error, and `DATABASECHANGELOG` is left consistent so later runs proceed cleanly. (`not tableExists` in a structured format is the equivalent guard.)

</details>

> **Exercise 3 — Adopt an existing DB.** Describe how you'd onboard Liquibase onto a production database that already has 20 tables, using preconditions + `MARK_RAN`, without recreating anything.

<details>
<summary><strong>Answer</strong></summary>

Goal: get Liquibase *tracking* a database whose 20 tables already exist, **without re-running any DDL** against them. The plan:

1. **Author a changeset per existing object** (or generate them — `generateChangeLog` can reverse-engineer the current schema into changesets), each describing a table that's already in production.
2. **Guard every one with a "not exists" precondition and `onFail:MARK_RAN`** — e.g. `--precondition-sql-check expectedResult:0 SELECT count(*) ... WHERE table_name = 'X'`. Because each table *does* exist, every precondition *fails*, and `MARK_RAN` records the changeset as applied **while skipping its DDL** — so nothing is recreated and you get zero "already exists" errors.
3. **Run `update` against production.** All 20 baseline changesets land in `DATABASECHANGELOG` as applied; the schema is untouched. (`changelogSync` is the purpose-built cousin that does this marking directly — Chapter 10.)
4. **From here, append normally.** New changesets (the 21st object onward) carry no such guard and apply for real, on top of a clean baseline.

The essence: `MARK_RAN` lets you *tell Liquibase "these already happened"* without touching the live schema — turning an existing, untracked database into a tracked one safely.

</details>

```quiz
{
  "prompt": "What is a Liquibase precondition, and what does `onFail:MARK_RAN` do when it isn't met?",
  "input": "Choose one:",
  "options": [
    "A check run before a changeset to verify the database state; MARK_RAN skips the changeset's changes but records it as applied — useful for baselining an existing database",
    "A retry policy that re-runs the changeset on failure",
    "An encryption setting for the changeset",
    "A way to run changesets in parallel"
  ],
  "answer": "A check run before a changeset to verify the database state; MARK_RAN skips the changeset's changes but records it as applied — useful for baselining an existing database"
}
```

## In the Wild

- **[Liquibase — Preconditions](https://docs.liquibase.com/concepts/changelogs/preconditions.html)** — all precondition types and the `onFail`/`onError` behaviors.
- **[Liquibase — Adding Liquibase to an existing project](https://docs.liquibase.com/workflows/liquibase-community/existing-project.html)** — the baselining workflow that leans on preconditions + `MARK_RAN`.
- **[Liquibase — `changelogSync`](https://docs.liquibase.com/commands/utility/changelog-sync.html)** — the related command for marking changesets as applied without running them.

---

**Next:** the hardest migration of all — changing a schema that *running code already depends on*, with zero downtime. Meet the expand/contract pattern. → [11. Refactoring data safely: expand/contract](/cortex/production-engineering/liquibase/advanced/expand-contract)
