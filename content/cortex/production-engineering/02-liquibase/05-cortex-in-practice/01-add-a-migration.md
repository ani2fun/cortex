---
title: '13. Add a real migration to Cortex'
summary: Put it all together — add a new table to Cortex end-to-end. Write the changeset, register it in the master changelog, and watch it apply and record on startup. The same bookmarks table from Part 3''s capstone.
---

# 13. Add a real migration to Cortex

## TL;DR

> Time to do it for real. We'll add a **`bookmarks`** table to Cortex — the very table Part 3's capstone (Chapter 22) needed for its protected endpoint — end to end: write a SQL-formatted changeset (`v3-bookmarks.sql`) with a rollback, register it in the master changelog with one `include`, and let Cortex's startup migration apply and record it. It synthesizes the whole Part: changeset structure (Ch 3–4), the `DATABASECHANGELOG` record (Ch 5), the rollback (Ch 6), startup application (Ch 7), and append-only discipline (Ch 12). Watch it land as the fourth changeset below.

## 1. Motivation

You've learned every piece; this chapter assembles them into the actual workflow you'd follow on the job. Adding a table is the most common migration there is, and doing it *correctly* — as a reviewed, reversible, recorded changeset that applies identically everywhere — is the muscle this whole Part has been building. We deliberately add the `bookmarks` table from Part 3's capstone, so you can see how the two Parts connect: the IAM capstone *designed* the protected endpoint and its schema; here you *create* that schema the right way. By the end you'll have a repeatable recipe for "the database needs a new table," start to finish.

## 2. The recipe

Adding a migration to Cortex is four steps, each grounded in a chapter you've read:

1. **Write the changeset** as a new SQL file (Ch 3–4): identity, the SQL, a rollback (Ch 6).
2. **Register it** in the master changelog with one `include` (Ch 3) — append-only (Ch 12).
3. **Apply it** — locally via `docker compose up` (or `liquibase update`); in production it applies automatically at startup (Ch 7).
4. **Verify** it landed: the table exists and `DATABASECHANGELOG` has a new row (Ch 5).

That's it. The discipline you've internalized makes each step boring — which is exactly the goal.

## 3. Step 1 — write the changeset

Create `server/src/main/resources/db/changelog/changes/v3-bookmarks.sql` (following Cortex's `vN-*.sql` convention from Chapter 4):

```sql
--liquibase formatted sql

--changeset cortex:4-create-bookmarks
CREATE TABLE bookmarks (
  sub        TEXT        NOT NULL,
  book       TEXT        NOT NULL,
  chapter    TEXT        NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (sub, book, chapter)
);
CREATE INDEX bookmarks_by_user ON bookmarks (sub, created_at DESC);
--rollback DROP TABLE bookmarks;
```

Everything you learned is here: the magic first line (Ch 4); identity `cortex:4-create-bookmarks` — note the id is **`4`**, the next number after Cortex's existing `1`/`2`/`3` (append-only, Ch 12); the table keyed on `sub` (the stable Keycloak subject id from Part 3, Ch 15 — *ownership built into the primary key*); a supporting index in the *same* changeset (one logical change, Ch 3); and a `--rollback` (Ch 6) that drops the table. Because `bookmarks` is brand-new, dropping it on rollback loses nothing — an *honest* rollback (Ch 6), unlike one that would discard live data.

## 4. Step 2 — register it in the master changelog

Add one `include` to `db.changelog-master.yaml` (Ch 3) — appending, never editing the existing entries:

```yaml
databaseChangeLog:
  - include:
      file: changes/v1-init.sql
      relativeToChangelogFile: true
  - include:
      file: changes/v2-submissions.sql
      relativeToChangelogFile: true
  - include:                                 # ← the only edit: append one entry
      file: changes/v3-bookmarks.sql
      relativeToChangelogFile: true
```

One new `include`, appended at the end. The existing entries are untouched (Ch 12's immutability), so their checksums stay valid and they won't re-run. Commit `v3-bookmarks.sql` *and* this changelog edit together, in the same PR as the `bookmarks` feature code (Ch 2 — schema and code travel together).

## 5. Step 3 & 4 — apply and verify (watch it land)

On the next `docker compose up` (or production deploy), Cortex's startup migration (Ch 7) runs `liquibase update`, which — being idempotent (Ch 5) — sees that changesets 1–3 are already recorded and applies *only* the new changeset 4, then records it. Step through the full history below; the **fourth** changeset is the one you just added, landing on top of Cortex's real three:

```d3 widget=migration-timeline
{
  "title": "Adding bookmarks (changeset 4) on top of Cortex's real schema",
  "changesets": [
    { "id": "1-create-visits", "author": "cortex", "creates": ["visits"],
      "sql": "CREATE TABLE visits (id INT PRIMARY KEY, count BIGINT NOT NULL);",
      "note": "Already applied on any existing database — recorded in DATABASECHANGELOG, so `update` skips it." },
    { "id": "2-create-submission-allowlist", "author": "cortex", "creates": ["submission_allowlist"],
      "sql": "CREATE TABLE submission_allowlist (username TEXT PRIMARY KEY, granted_at TIMESTAMPTZ NOT NULL DEFAULT now());",
      "note": "Already applied — skipped." },
    { "id": "3-create-submissions", "author": "cortex", "creates": ["submissions"],
      "sql": "CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY, username TEXT NOT NULL, ...);",
      "note": "Already applied — skipped." },
    { "id": "4-create-bookmarks", "author": "cortex", "creates": ["bookmarks"],
      "sql": "CREATE TABLE bookmarks (\n  sub        TEXT NOT NULL,\n  book       TEXT NOT NULL,\n  chapter    TEXT NOT NULL,\n  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n  PRIMARY KEY (sub, book, chapter)\n);\nCREATE INDEX bookmarks_by_user ON bookmarks (sub, created_at DESC);",
      "note": "THE NEW ONE. Liquibase applies only this changeset, creates the bookmarks table + index, and writes its DATABASECHANGELOG row. Every environment converges to this exact schema." }
  ]
}
```

To verify by hand against the running local stack (Part 3, Ch 13 brought Postgres up via `docker compose`):

```text
# Connect to the dev database and confirm the table and the tracking row:
docker compose exec db psql -U cortex -d cortex -c "\d bookmarks"
#   -> shows the bookmarks table with its columns and primary key

docker compose exec db psql -U cortex -d cortex \
  -c "SELECT id, author, orderexecuted FROM databasechangelog ORDER BY orderexecuted;"
#   -> 4 rows: ... 4-create-bookmarks | cortex | 4    (your changeset, recorded)
```

The table exists; `DATABASECHANGELOG` has its row; and because the same changelog runs everywhere, *every* Cortex database — your laptop, CI, staging, production — now has an identical `bookmarks` table. Part 3's capstone endpoint (`POST /api/me/bookmarks`) has the schema it needs, created the right way.

## 6. Build It

Run this. It's the full add-a-migration flow as code: append a changeset, run the idempotent update, verify the table and the tracking row.

```python run
# Cortex's existing applied history (a running database).
databasechangelog = ["1-create-visits", "2-create-submission-allowlist", "3-create-submissions"]
schema = {"visits", "submission_allowlist", "submissions"}

# STEP 1+2: the changelog, with the new changeset appended.
CHANGELOG = [
    {"id": "1-create-visits"},
    {"id": "2-create-submission-allowlist"},
    {"id": "3-create-submissions"},
    {"id": "4-create-bookmarks", "creates": "bookmarks", "index": "bookmarks_by_user"},  # ← appended
]

# STEP 3: startup migration (idempotent update).
def update():
    for cs in CHANGELOG:
        if cs["id"] not in databasechangelog:
            print(f"   applying {cs['id']}")
            if cs.get("creates"): schema.add(cs["creates"])
            databasechangelog.append(cs["id"])
        else:
            print(f"   skip {cs['id']} (already applied)")

print("startup: liquibase update")
update()

# STEP 4: verify.
print("\nschema now:", sorted(schema))
print("bookmarks table exists:", "bookmarks" in schema)
print("DATABASECHANGELOG:", databasechangelog)
print("changeset 4 recorded:", "4-create-bookmarks" in databasechangelog)
```

**Now break it.** Run `update()` a second time — every changeset, including your new `4-create-bookmarks`, is now skipped (all recorded). That's the production reality: your migration applies *once*, on the first deploy that includes it, and is a no-op forever after. Then try giving your new changeset the id `3-create-submissions` (a duplicate) instead of `4-...` — in real Liquibase you'd get a checksum/identity conflict (Ch 12), because you'd be redefining applied history. The append-only, next-number discipline is what keeps the whole thing safe.

## 7. Trade-offs & Complexity

This *is* the synthesis, so the "trade-off" is the whole Part's thesis: a four-step recipe (write, register, apply, verify) versus a one-line manual `CREATE TABLE`. The recipe is more steps, but every step buys a guarantee — reviewable history, identical environments, automatic application, a rollback, drift protection. For Cortex's `bookmarks` table, that's the difference between "it exists on prod and *probably* matches the other environments" and "it provably exists, identically, everywhere, with a record of when and how." Once it's muscle memory, the recipe is barely slower than the manual command — and infinitely safer.

## 8. Practice

> **Exercise 1 — Do it for real.** If you're running Cortex locally (Part 3, Ch 13), actually add `v3-bookmarks.sql`, register it, `docker compose up`, and verify the table and the `DATABASECHANGELOG` row.

<details>
<summary><strong>Answer</strong></summary>

This is the four-step recipe (§2) executed against the running local stack:

1. **Write** `server/src/main/resources/db/changelog/changes/v3-bookmarks.sql` with the magic first line, identity `cortex:4-create-bookmarks` (the next id after Cortex's `1`/`2`/`3`), the `CREATE TABLE bookmarks (...)` keyed on `(sub, book, chapter)`, the supporting `bookmarks_by_user` index in the *same* changeset, and a `--rollback DROP TABLE bookmarks;` (§3).
2. **Register** it by **appending** one `include` for `changes/v3-bookmarks.sql` to `db.changelog-master.yaml`, leaving the existing `v1`/`v2` entries untouched (§4).
3. **Apply** with `docker compose up` — Cortex's startup migration (Ch 7) runs `liquibase update`, which is idempotent (Ch 5): it skips the already-recorded changesets 1–3 and applies *only* your new changeset 4.
4. **Verify** against the dev database:

```text
docker compose exec db psql -U cortex -d cortex -c "\d bookmarks"
#   -> the table with its columns and primary key

docker compose exec db psql -U cortex -d cortex \
  -c "SELECT id, author, orderexecuted FROM databasechangelog ORDER BY orderexecuted;"
#   -> a 4th row: 4-create-bookmarks | cortex | 4
```

The table exists and `DATABASECHANGELOG` has its row — and because the same changelog runs everywhere, every Cortex database converges to this identical `bookmarks` table.

</details>

> **Exercise 2 — Add a column later.** Next week you need `bookmarks.note TEXT`. Write the *new* changeset (id and rollback) — and explain why you do NOT edit `4-create-bookmarks` (Ch 12).

<details>
<summary><strong>Answer</strong></summary>

Append a brand-new changeset — id `5`, the next number after `4` — that alters the existing table:

```sql
--changeset cortex:5-add-bookmarks-note
ALTER TABLE bookmarks ADD COLUMN note TEXT;
--rollback ALTER TABLE bookmarks DROP COLUMN note;
```

Register it with one more `include` (for `v4-bookmarks-note.sql`, or wherever you place it) appended to the master changelog, and the idempotent startup migration applies *only* changeset 5 on the next deploy.

**Why not edit `4-create-bookmarks`:** changeset 4 has already been **applied** (your laptop, CI, prod), so it's recorded in `DATABASECHANGELOG` and won't re-run, and its content is fingerprinted by a **checksum** (Ch 12). Editing it to add the column inline would (a) change its checksum → `update` halts with a validation error, and (b) even if forced, cause **silent divergence** — a *fresh* database would build `bookmarks` *with* `note`, while every database that already ran the original changeset 4 keeps the table *without* it and never sees the edit. Appending changeset 5 instead runs *everywhere*, so all environments converge. History stays append-only.

</details>

> **Exercise 3 — Tie the book together.** Connect this table to Part 3, Chapter 22: which column enforces ownership, why is it `sub` and not `username` (Part 3, Ch 15), and how does the index support "list my bookmarks"?

<details>
<summary><strong>Answer</strong></summary>

- **Which column enforces ownership: `sub`.** It's part of the **primary key** `(sub, book, chapter)`, so *ownership is built into the key itself* (§3) — a bookmark literally cannot exist without belonging to a subject, and the protected endpoint (Part 3, Ch 22) scopes every read/write to the caller's own `sub`.
- **Why `sub` and not `username`:** `sub` is the **stable Keycloak subject id** (Part 3, Ch 15). A `username` (or email) can be *changed or reassigned*, which would silently break ownership and orphan or mis-attribute rows; `sub` is the immutable, canonical identity for "who this is," so keying on it makes ownership durable. (It's the same reason the expand/contract chapter imagined re-keying `submissions` from `username` to `user_sub`.)
- **How the index supports "list my bookmarks":** `bookmarks_by_user ON bookmarks (sub, created_at DESC)` has `sub` as its leading column, so a query `WHERE sub = $me ORDER BY created_at DESC` is served directly by an index range-scan — the rows for one user are already grouped and pre-sorted newest-first, so listing a user's bookmarks is fast and needs no separate sort. The schema's key and index are shaped exactly around the access pattern the IAM capstone needs.

</details>

```quiz
{
  "prompt": "To add a `bookmarks` table to Cortex correctly, what do you do?",
  "input": "Choose one:",
  "options": [
    "Write a new SQL changeset file (with identity + rollback), APPEND one `include` to the master changelog, and let the idempotent startup migration apply and record it — never editing existing changesets",
    "Run `ALTER TABLE` by hand on production",
    "Edit changeset 3 to add the table inline",
    "Delete DATABASECHANGELOG and re-run everything"
  ],
  "answer": "Write a new SQL changeset file (with identity + rollback), APPEND one `include` to the master changelog, and let the idempotent startup migration apply and record it — never editing existing changesets"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Add changesets workflow](https://docs.liquibase.com/start/tutorials/home.html)** — the end-to-end add-a-changeset flow this chapter mirrors.
- **[Cortex `db/changelog`](https://github.com/ani2fun/cortex/tree/main/server/src/main/scala/cortex/server/db)** & **[changes/](https://github.com/ani2fun/cortex/tree/main/server/src/main/resources/db/changelog/changes)** — the real changelog and changesets your `v3-bookmarks.sql` would join.
- **[Part 3 — Capstone: add a protected endpoint](/cortex/production-engineering/iam-keycloak-oauth/capstone/add-a-protected-endpoint)** — the IAM chapter that designed the `bookmarks` endpoint this table backs.

---

## You've finished Part 2 🎉

You started at "why can't I just `ALTER TABLE`?" and you can now manage a production schema the way professionals do: every change an explicit, ordered, recorded, reversible changeset; the database tracking its own history in `DATABASECHANGELOG`; migrations applied automatically and idempotently at startup, fail-fast on error; advanced moves (contexts, preconditions, expand/contract) for the hard cases; and checksums keeping everyone honest. Cortex's schema is no longer a mystery — it's a versioned history you can read and extend.

**Where to go next in _Production Engineering_:**

- **[Part 1 — ZIO](/cortex/production-engineering/zio):** the `Migrations.run` effect and the `DataSource` it uses are ZIO values — Part 1 is the framework underneath.
- **[Part 3 — Identity & Access Management](/cortex/production-engineering/iam-keycloak-oauth):** the `submission_allowlist` and `bookmarks` tables you migrated are what the auth gate and capstone use.
- **[Part 4 — Docker & Kubernetes](/cortex/production-engineering/docker-kubernetes):** how the migration-running app is packaged and deployed — including why fail-fast migrations and health probes work together.

You've learned how the system's **data evolves**. Next: how it **ships**.
