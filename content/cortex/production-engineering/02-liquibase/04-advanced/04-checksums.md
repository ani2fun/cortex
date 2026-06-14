---
title: '12. Checksums, when they bite, and how to recover'
summary: Liquibase fingerprints every applied changeset with a checksum. Edit an applied changeset and the fingerprint changes — Liquibase stops you, protecting against silently rewriting history. Learn why, and how to recover.
---

# 12. Checksums, when they bite, and how to recover

## TL;DR

> When Liquibase applies a changeset, it stores an **MD5SUM** — a fingerprint of the changeset's contents — in `DATABASECHANGELOG` (Chapter 5). On every later run it *recomputes* each applied changeset's checksum and compares. If you've **edited an already-applied changeset**, the new fingerprint won't match the stored one, and Liquibase **halts with a validation error**. This isn't Liquibase being difficult — it's protecting you from silently diverging databases (your edit ran on new databases but *not* on ones that already applied the old version). The golden rule: **never edit an applied changeset; append a new one.** When checksums legitimately need updating, `clearCheckSums`, `validCheckSum`, and `runOnChange` are the escape hatches.

## 1. Motivation

Chapters 2 and 3 kept warning: *don't edit an applied changeset; append a new one.* Checksums are the mechanism that *enforces* that rule, and understanding them turns a baffling error ("validation failed: checksum changed") into an obvious one. The danger they guard against is subtle but serious. Say changeset 3 ran on production last week. Today you edit changeset 3's SQL (to fix a typo, add a column). Your *local* database is fresh, so it applies your *edited* version. But production already ran the *old* version and won't re-run changeset 3 (it's recorded as applied, Chapter 5). Now production and your laptop have run *different* SQL under the *same* changeset id — silent divergence, the exact drift migrations exist to prevent. The bug could lurk for months.

Checksums catch this immediately. By fingerprinting each changeset at apply time and re-checking it every run, Liquibase notices the *instant* a changeset's content differs from what was recorded as applied, and stops — forcing you to confront "you're rewriting history that already ran somewhere." It's the schema equivalent of git refusing a non-fast-forward push over a commit someone else already has. Annoying in the moment, invaluable in aggregate.

## 2. Intuition (Analogy)

A **notarized, sealed contract.** When both parties sign a contract, a notary stamps a *seal* over the text — a tamper-evident mark tied to those exact words. If someone later quietly edits a clause, the seal no longer matches the (altered) text, and any official who checks it *immediately knows the document was changed after signing*. The seal doesn't prevent you from writing a *new, properly-signed amendment*; it only prevents *silently rewriting the original* and pretending it was always that way.

A checksum is that notary seal on a changeset. It's computed over the changeset's exact text at the moment it's "signed" (applied) and recorded. Edit the text afterward and the seal mismatches — Liquibase flags the forgery. You're free to write a *new* changeset (an amendment); you're stopped from *rewriting the signed one*. That's the whole job: make tampering with applied history *visible*, not impossible.

## 3. Formal Definition

- **MD5SUM (checksum).** A hash of a changeset's contents (its SQL/changes, plus some metadata), computed when the changeset is applied and stored in the `MD5SUM` column of `DATABASECHANGELOG`. (The leading `8:` you may see is the checksum *algorithm version*.)
- **Validation on update.** Before applying new changesets, Liquibase recomputes the checksum of each *already-applied* changeset and compares it to the stored value. A mismatch → **`ValidationFailedException`**, halting the run.
- **Why it halts:** a mismatch means an applied changeset's *content changed* since it ran. Liquibase can't safely know whether to re-run it (it's recorded as applied) — and the change likely *didn't* run on databases that applied the old version. So it stops and makes you decide.
- **Legitimate recovery tools:**
  - **`clearCheckSums`** — wipe all stored checksums; they're recomputed (and re-stored) on the next run. Use when you've *intentionally and safely* changed applied changesets across *all* environments, or are recovering a known-good state.
  - **`validCheckSum: <sum>`** — on a changeset, *whitelist* an additional acceptable checksum, so both the old and new fingerprints validate. Surgical alternative to clearing everything.
  - **`runOnChange: true`** — a changeset attribute meaning "re-run this whenever its content changes" (and update its checksum). Designed for *idempotent, replaceable* objects like **views** or stored procedures, where you *do* edit-in-place by design.
  - **`changelogSync`** — mark changesets as applied *without running them* (Chapter 10's baselining cousin), aligning the tracking table with reality.
- **The rule.** For *tables/data* (non-idempotent changes), **never edit an applied changeset** — append a new one. `runOnChange` is for *replaceable* objects only.

> A checksum fingerprints a changeset at apply time; a later content change makes the fingerprint mismatch and Liquibase halts, catching silent history-rewrites. Recover with `clearCheckSums`/`validCheckSum` (when the edit is intentional and safe everywhere) or use `runOnChange` for replaceable objects like views. Default: don't edit applied changesets.

## 4. Worked Example — the bite and the fix

```text
# Monday: changeset 3 applies to prod AND your laptop. Both store MD5SUM = abc123.
DATABASECHANGELOG: (3-create-submissions, MD5SUM=abc123)

# Wednesday: you EDIT changeset 3's SQL (add a column inline). Its content hash is now def456.
# You run `liquibase update` locally:

  ValidationFailedException: 1 changesets check sum
    cortex:3-create-submissions was: abc123 but is now: def456

# Liquibase HALTS. Why this is GOOD:
#   - prod already ran the OLD changeset 3; it won't re-run it -> prod never gets your new column
#   - your laptop (fresh) would have run the NEW version -> silent divergence
#   - the checksum caught it before the drift happened.
```

The **correct fix** is almost never to force the checksum — it's to *undo the edit and append a new changeset*:

```sql
# Revert changeset 3 to its original (applied) form, then ADD:
--changeset cortex:4-add-submissions-extra-column
ALTER TABLE submissions ADD COLUMN extra TEXT;
--rollback ALTER TABLE submissions DROP COLUMN extra;
```

Now changeset 4 applies *everywhere* (prod runs it on next deploy; your laptop runs it too), and changeset 3's checksum is untouched. History stayed append-only; no divergence. The *legitimate* uses of `clearCheckSums`/`validCheckSum` are narrow: you changed something cosmetic (a comment, formatting) that genuinely doesn't alter behavior, or you're recovering a known-good baseline across *all* environments at once. And `runOnChange` is for a *view* you intend to redefine in place — there, editing the changeset is the *design*, and Liquibase re-runs it and updates the checksum. For Cortex's tables, none of that applies: the rule is simply *append*.

## 5. Build It

Run this. It implements checksum validation and shows the halt — then the correct (append) and incorrect (force) responses.

```python run
import hashlib

def checksum(sql): return "8:" + hashlib.md5(sql.encode()).hexdigest()[:8]

# DATABASECHANGELOG: changeset 3 was applied with its ORIGINAL sql.
applied = {"3-create-submissions": {"sql": "CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY)",
                                     "md5": checksum("CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY)")}}

def validate(changelog):
    for cs in changelog:
        rec = applied.get(cs["id"])
        if rec and checksum(cs["sql"]) != rec["md5"]:
            raise RuntimeError(
                f"checksum mismatch: {cs['id']} was {rec['md5']} but is now {checksum(cs['sql'])}")

# You EDITED changeset 3 in place (added a column inline):
edited_changelog = [{"id": "3-create-submissions",
                     "sql": "CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY, extra TEXT)"}]
print("== editing an applied changeset ==")
try:
    validate(edited_changelog)
except RuntimeError as e:
    print("   HALT:", e)
    print("   (good — this caught silent drift before it happened)\n")

# ✓ CORRECT FIX: revert #3, APPEND #4.
correct_changelog = [
    {"id": "3-create-submissions", "sql": "CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY)"},  # original
    {"id": "4-add-extra",          "sql": "ALTER TABLE submissions ADD COLUMN extra TEXT"},        # new
]
validate(correct_changelog)   # #3 matches its stored checksum; #4 is new -> no error
print("== append a new changeset ==")
print("   validates cleanly: #3 unchanged, #4 is new -> applies everywhere")
```

**Now break it.** Try to "fix" the first case by editing `applied["3-create-submissions"]["md5"]` to match your edited SQL (simulating a careless `clearCheckSums`). Validation now passes — but you've just *endorsed the divergence*: production still has the old table shape, your laptop the new one, and Liquibase will never reconcile them. That's the trap of forcing checksums: it silences the alarm without fixing the fire. The append approach (changeset 4) is the only one that actually makes every database converge. When a checksum bites, the question is almost always "what new changeset do I append?", not "how do I make the error go away?"

## 6. Trade-offs & Complexity

| Checksums (enforced immutability) | No integrity check |
|---|---|
| Catches silent history-rewrites early | Drift sneaks in undetected |
| Forces append-only discipline | "Just edit the changeset" → divergence |
| Clear recovery tools for real cases | No guardrail at all |
| Occasional confusing "bite" | No friction (until a silent bug) |
| `runOnChange` for replaceable objects | — |

The cost is the occasional bewildering error and the temptation to "make it go away" with `clearCheckSums` — which, used carelessly, defeats the protection. The skill is recognizing that a checksum mismatch is almost always *telling you to append, not to override*. Embrace the constraint: treating applied changesets as immutable (like pushed git commits) is what keeps every database provably in sync. The few legitimate overrides (`validCheckSum` for cosmetic edits, `runOnChange` for views) are narrow and deliberate.

## 7. Edge Cases & Failure Modes

- **`clearCheckSums` as a reflex.** Running it to silence an error *without* understanding the mismatch can endorse drift across environments. Diagnose first; clear only when you've made the change safely everywhere.
- **`runOnChange` on a table.** Meant for idempotent, replaceable objects (views, procs). Putting it on a `CREATE TABLE` would try to re-run it on every edit → "already exists" errors. Tables: append; views: `runOnChange`.
- **Whitespace/formatting "bites."** Reformatting an applied changeset (even without semantic change) alters its checksum and triggers a mismatch. Use `validCheckSum` to whitelist the new sum, or just don't reformat applied changesets.
- **Cross-tool checksum versions.** Upgrading Liquibase can change the checksum *algorithm* (the `8:` prefix); Liquibase handles legacy versions, but a major upgrade may require a `clearCheckSums` once, deliberately, across all environments.

## 8. Practice

> **Exercise 1 — Why it halted.** A teammate edited changeset 2 and now `update` fails with a checksum error. Explain, in terms of divergence, *why* halting is the correct behavior.

<details>
<summary><strong>Answer</strong></summary>

Changeset 2 was already **applied** somewhere (prod, CI, other laptops), so it's recorded in `DATABASECHANGELOG` and those databases **will not re-run it** (Chapter 5). Editing its SQL changes its content, so its recomputed checksum no longer matches the stored `MD5SUM` — and Liquibase halts with a `ValidationFailedException` (§3).

Halting is correct because the alternative is **silent divergence**: a *fresh* database (your laptop) would apply the *edited* version, while every database that already ran the *old* version keeps the old shape and never sees the edit. Same changeset id, two different SQLs, two different schemas — the exact drift migrations exist to prevent, and a bug that could lurk for months. The checksum catches it the *instant* the content differs, forcing you to confront "you're rewriting history that already ran somewhere" before the drift can happen. It's git refusing a non-fast-forward push over a commit others already have: annoying now, invaluable in aggregate.

</details>

> **Exercise 2 — Append, don't edit.** They want to add a column to a table created in changeset 2. Show the correct change (which existing changeset do you touch, and what do you add?).

<details>
<summary><strong>Answer</strong></summary>

You touch **no existing changeset** — changeset 2 stays exactly as applied, so its checksum remains valid and it never re-runs. You **append a new changeset** that alters the table:

```sql
--changeset cortex:5-add-orders-note
ALTER TABLE orders ADD COLUMN note TEXT;
--rollback ALTER TABLE orders DROP COLUMN note;
```

(Use the next free id — `5` here — after the existing `1`–`4`.) The new changeset applies *everywhere*: databases that already ran changeset 2 pick it up on their next `update`, and a fresh database runs `1…5` in order — every environment converges to the same schema, with history kept **append-only**. Editing changeset 2 in place would instead trip the checksum and risk divergence (the edit would run on fresh databases but not on ones that already applied the original). When a checksum bites, the question is "what new changeset do I append?", not "how do I silence the error?"

</details>

> **Exercise 3 — When to override.** Give one legitimate use each for `validCheckSum`, `runOnChange`, and `clearCheckSums`, and one *illegitimate* use of `clearCheckSums` that hides drift.

<details>
<summary><strong>Answer</strong></summary>

Each override is narrow and deliberate (§3):

- **`validCheckSum: <sum>` — legitimate:** you reformatted or reworded a *comment* in an applied changeset (a whitespace/cosmetic change with **no** behavioral effect). Whitelisting the new fingerprint lets both the old and new sums validate, surgically, without touching anything else.
- **`runOnChange: true` — legitimate:** a **replaceable, idempotent object** like a database **view** or stored procedure that you *intend* to redefine in place. Editing the changeset *is* the design; Liquibase re-runs it on change and updates its checksum. (Never on a `CREATE TABLE` — that would try to recreate the table and error.)
- **`clearCheckSums` — legitimate:** you made an intentional, safe change across **all** environments at once, or you're recovering a known-good baseline (or doing a one-time, deliberate reset after a major Liquibase upgrade changed the checksum *algorithm*). Checksums are wiped and recomputed on the next run.
- **`clearCheckSums` — illegitimate:** running it as a *reflex* to silence a checksum error you don't understand — e.g. after editing an applied table changeset on your laptop. It "endorses the divergence": prod still has the old shape, your laptop the new one, and Liquibase will never reconcile them. It silences the alarm without putting out the fire — the correct move is to *append a new changeset*, not override.

</details>

```quiz
{
  "prompt": "You edit an already-applied changeset and `liquibase update` halts with a checksum mismatch. What's the right response?",
  "input": "Choose one:",
  "options": [
    "Revert the edit and append a NEW changeset — the mismatch is protecting you from silent divergence (the edited version would run on fresh DBs but not on ones that already applied the old version)",
    "Run clearCheckSums to make the error go away and continue",
    "Delete the DATABASECHANGELOG table",
    "Edit the stored MD5SUM by hand to match"
  ],
  "answer": "Revert the edit and append a NEW changeset — the mismatch is protecting you from silent divergence (the edited version would run on fresh DBs but not on ones that already applied the old version)"
}
```

## In the Wild

- **[Liquibase — Checksums (MD5SUM)](https://docs.liquibase.com/concepts/changelogs/changeset-checksums.html)** — how they're computed, validated, and the `validCheckSum`/`runOnChange` attributes.
- **[Liquibase — `clearCheckSums` & `changelogSync`](https://docs.liquibase.com/commands/utility/clear-checksums.html)** — the recovery commands and when to use them.
- **[Liquibase — Best practices: immutable changesets](https://docs.liquibase.com/concepts/bestpractices.html)** — the append-don't-edit rule checksums enforce.

---

**Next:** you now know Liquibase end to end. Let's put it all together by adding a *real* migration to Cortex — a new table, applied and tracked — start to finish. → [13. Add a real migration to Cortex](/cortex/production-engineering/liquibase/cortex-in-practice/add-a-migration)
