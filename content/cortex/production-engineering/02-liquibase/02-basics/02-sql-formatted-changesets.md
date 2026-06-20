---
title: '4. SQL-formatted changesets'
summary: Liquibase speaks XML, YAML, JSON — and plain SQL. The SQL format lets you write ordinary SQL with two magic comments. Read Cortex''s real v1-init.sql and v2-submissions.sql.
---

# 4. SQL-formatted changesets

## TL;DR

> A changeset can be written in XML, YAML, JSON, or **plain SQL**. The **SQL format** is the most approachable: a file that starts with `--liquibase formatted sql`, with each changeset marked by a `--changeset author:id` comment and its reversal by `--rollback`. You write *normal SQL* — exactly what you'd run by hand — and Liquibase wraps it with identity, ordering, and tracking. Cortex uses this format: `v1-init.sql` creates the `visits` table; `v2-submissions.sql` creates the submission tables. Below, the real files, line by line.

## 1. Motivation

Liquibase's database-agnostic formats (XML/YAML) describe changes *abstractly* — `<createTable>`, `<addColumn>` — and Liquibase generates the right SQL for whatever database you target. That's powerful if you must support MySQL *and* Postgres *and* Oracle from one changelog. But most teams target *one* database, know its SQL dialect, and would rather just *write SQL* than learn an abstraction layer over it. For them, the **SQL format** is ideal: it's the SQL you already know, plus two comments to give Liquibase the identity and rollback it needs.

Cortex targets Postgres only, so it picks SQL format — and the result reads beautifully: a changeset *is* the `CREATE TABLE` you'd write anyway, with a one-line comment above it. No translation layer, no surprises about what SQL actually runs, full access to Postgres-specific features (`BIGSERIAL`, `TIMESTAMPTZ`, partial indexes). The trade — losing database portability — is one a single-database team gladly makes.

## 2. Intuition (Analogy)

Two ways to give cooking instructions to a kitchen. The **abstract** way: "prepare a starch suitable for the region," and a local chef decides whether that's rice, pasta, or potatoes (the XML/YAML format — Liquibase picks the dialect). The **direct** way: "boil 200g of spaghetti for 9 minutes" — precise, unambiguous, exactly what happens, but only right for *this* kitchen (the SQL format — you write the exact dialect).

If you're writing one cookbook for restaurants worldwide, the abstract instructions are worth it. If you cook in *your own kitchen* every night, the direct recipe is clearer and gives you every local technique. Cortex cooks in one kitchen (Postgres), so it writes direct SQL — with just a label on each recipe so the kitchen log knows which ones it's already made.

## 3. Formal Definition

The **SQL-formatted changelog** is a `.sql` file with Liquibase directives in comments:

- **`--liquibase formatted sql`** — the *required first line*; tells Liquibase to parse this `.sql` file as a changelog.
- **`--changeset author:id`** — marks the start of a changeset (everything until the next `--changeset` or EOF). `author:id` is its unique identity (Chapter 3). Optional attributes follow: `--changeset cortex:5 runOnChange:true labels:... context:...`.
- **The SQL itself** — ordinary statements (DDL and/or DML), run as that changeset, separated by `;`.
- **`--rollback <sql>`** — the reversal for this changeset (Chapter 6). Liquibase can't auto-reverse raw SQL, so you supply it.
- **Optional directives** — `--comment`, `--preconditions` (Chapter 10), `--validCheckSum` (Chapter 12), etc., as comment lines within a changeset.

> SQL format = your real SQL + `--changeset author:id` to identify it + `--rollback` to reverse it. You give up cross-database portability; you gain clarity and full dialect access. The file *is* the SQL that runs, with labels.

## 4. Worked Example — Cortex's real changesets

**`v1-init.sql`** — the first changeset, creating the visit counter ([source](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v1-init.sql)):

```sql
--liquibase formatted sql

--changeset cortex:1-create-visits
CREATE TABLE visits (
  id    INT    PRIMARY KEY,
  count BIGINT NOT NULL
);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;
```

Line by line: `--liquibase formatted sql` declares the file a changelog. `--changeset cortex:1-create-visits` opens a changeset with author `cortex`, id `1-create-visits`. Then two *ordinary* SQL statements — create the table, seed the single counter row — run as that one changeset. `--rollback DROP TABLE visits;` is the reversal (Chapter 6). That's a complete, identified, reversible unit of schema history, and it's *just SQL* with a label.

**`v2-submissions.sql`** — *two* changesets in one file ([source](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v2-submissions.sql)):

```sql
--liquibase formatted sql

--changeset cortex:2-create-submission-allowlist
CREATE TABLE submission_allowlist (
  username   TEXT        PRIMARY KEY,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO submission_allowlist (username) VALUES ('ani2fun');
INSERT INTO submission_allowlist (username) VALUES ('tester');
--rollback DROP TABLE submission_allowlist;

--changeset cortex:3-create-submissions
CREATE TABLE submissions (
  id           BIGSERIAL   PRIMARY KEY,
  username     TEXT        NOT NULL,
  book         TEXT        NOT NULL,
  chapter      TEXT        NOT NULL,
  language     TEXT        NOT NULL,
  source       TEXT        NOT NULL,
  accepted     BOOLEAN     NOT NULL,
  passed_cases INT         NOT NULL,
  total_cases  INT         NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX submissions_user_chapter_idx
  ON submissions (username, book, chapter, created_at DESC);
--rollback DROP TABLE submissions;
```

This one file holds *two* changesets — `2-create-submission-allowlist` and `3-create-submissions` — each starting at its `--changeset` line. The first creates the allow-list (the very table Part 3, Chapter 16 used to gate submissions!) and *seeds* it with two usernames (DML in a changeset is fine — migrations cover data too). The second creates the `submissions` table, full of Postgres-specific types (`BIGSERIAL`, `TIMESTAMPTZ`), plus an index — and notice the index is in the *same* changeset as the table, because a table and its essential index are one logical change. Each changeset has its own `--rollback`. Read top to bottom, it *is* the SQL you'd run, annotated just enough for Liquibase to track and reverse it.

## 5. Build It

Run this. It's a tiny parser for the SQL-formatted changelog — it splits a `.sql` file into identified changesets, exactly as Liquibase does.

```python run
changelog = """--liquibase formatted sql

--changeset cortex:1-create-visits
CREATE TABLE visits (id INT PRIMARY KEY, count BIGINT NOT NULL);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;

--changeset cortex:2-create-submissions
CREATE TABLE submissions (id BIGSERIAL PRIMARY KEY, username TEXT NOT NULL);
--rollback DROP TABLE submissions;
"""

def parse(text):
    lines = text.splitlines()
    assert lines[0].strip() == "--liquibase formatted sql", "must start with the magic line"
    changesets, current = [], None
    for ln in lines[1:]:
        s = ln.strip()
        if s.startswith("--changeset"):
            author, cid = s.split(None, 1)[1].split(":", 1)        # author:id
            current = {"author": author, "id": cid, "sql": [], "rollback": None}
            changesets.append(current)
        elif s.startswith("--rollback"):
            current["rollback"] = s[len("--rollback"):].strip()
        elif s and not s.startswith("--") and current:
            current["sql"].append(s)
    return changesets

for cs in parse(changelog):
    print(f"changeset {cs['author']}:{cs['id']}")
    for stmt in cs["sql"]:
        print(f"    forward : {stmt}")
    print(f"    rollback: {cs['rollback']}\n")
```

**Now break it.** Delete the `--liquibase formatted sql` first line and re-run — the `assert` fires, because Liquibase wouldn't recognize the file as a changelog without it (it'd treat the whole thing as one anonymous SQL script). Then remove a `--changeset` line: the statements below it now have *no identity*, so the parser can't attribute them — mirroring how Liquibase needs each changeset explicitly marked to track it in `DATABASECHANGELOG`. The two magic comments aren't decoration; they're the minimum Liquibase needs to turn "a SQL file" into "tracked, reversible history."

## 6. Trade-offs & Complexity

| SQL format (Cortex) | XML/YAML abstract format |
|---|---|
| Write real SQL you already know | Learn `<createTable>` etc. abstractions |
| Full access to dialect features | Limited to what the abstraction covers |
| What runs is exactly what you see | Liquibase generates the SQL |
| **Locked to one database** | Database-portable |
| Some auto-rollback lost (write `--rollback`) | More auto-rollback for known change types |

The big trade is **portability**: SQL-format changesets run only on the dialect you wrote them in, and Liquibase can't auto-generate rollbacks for arbitrary SQL (you write `--rollback`). For a multi-database product, the XML/YAML abstraction earns its keep. For a single-Postgres app like Cortex, SQL format is clearer, more powerful, and honest about what executes — an easy choice. You can even *mix*: a YAML master (good at structure) including SQL changesets (good at being SQL), which is exactly Cortex's setup.

## 7. Edge Cases & Failure Modes

- **Missing the magic first line.** Without `--liquibase formatted sql`, the file isn't parsed as a changelog. It's the single most common SQL-format mistake.
- **No `--rollback` on raw SQL.** Liquibase can't infer how to reverse arbitrary SQL. If you omit `--rollback`, rolling back that changeset fails (Chapter 6). Write it.
- **Statement splitting.** Liquibase splits on `;` by default. Stored procedures or functions containing semicolons need `--changeset ... splitStatements:false` or an explicit `endDelimiter`.
- **DML without thinking about idempotence.** A changeset's `INSERT` runs once (it's a changeset), so it won't double-insert — but if you *edit* it later, that's rewriting applied history. Seed data via a new changeset, not by editing the old one.

## 8. Practice

> **Exercise 1 — Label the parts.** In `v1-init.sql`, point to: the magic first line, the changeset identity, the forward SQL, and the rollback.

<details>
<summary><strong>Answer</strong></summary>

Annotating Cortex's real `v1-init.sql` against the four parts from §3:

```sql
--liquibase formatted sql            <- the MAGIC FIRST LINE: tells Liquibase to parse this .sql file as a changelog (required, must be first)

--changeset cortex:1-create-visits   <- the CHANGESET IDENTITY: author `cortex`, id `1-create-visits`, unique together; opens the changeset
CREATE TABLE visits (                  ⎫
  id    INT    PRIMARY KEY,            ⎬  the FORWARD SQL: ordinary statements that run as this changeset
  count BIGINT NOT NULL                ⎪  (create the table, then seed the single counter row)
);                                     ⎪
INSERT INTO visits (id, count) VALUES (1, 0);   ⎭
--rollback DROP TABLE visits;         <- the ROLLBACK: the reversal SQL for this changeset (Chapter 6)
```

- **Magic first line** — `--liquibase formatted sql`. Without it the file isn't recognized as a changelog at all (§7's most common mistake).
- **Changeset identity** — `--changeset cortex:1-create-visits`. The `author:id` is how `DATABASECHANGELOG` will record "this one ran."
- **Forward SQL** — the `CREATE TABLE` and the `INSERT`: just normal SQL, run as one changeset.
- **Rollback** — `--rollback DROP TABLE visits;`: how Liquibase undoes the changeset, since it can't auto-reverse raw SQL.

The whole file is *exactly the SQL you'd run by hand*, with two comments giving Liquibase the identity and reversibility it needs.

</details>

> **Exercise 2 — Write one.** Write a SQL-formatted changeset (with identity and rollback) that adds a `phone TEXT` column to a `users` table. What's the `--rollback`?

<details>
<summary><strong>Answer</strong></summary>

```sql
--changeset cortex:5-add-users-phone
ALTER TABLE users ADD COLUMN phone TEXT;
--rollback ALTER TABLE users DROP COLUMN phone;
```

The pieces, per §3:

- **`--changeset cortex:5-add-users-phone`** — opens the changeset with a unique `author:id`. (If this is the only changeset in a brand-new file, the file also needs `--liquibase formatted sql` as its first line.)
- **The forward SQL** — one ordinary `ALTER TABLE ... ADD COLUMN` statement.
- **`--rollback ALTER TABLE users DROP COLUMN phone;`** — the inverse of "add a column" is "drop that column." You must write it yourself: this is raw SQL, so Liquibase can't infer the reversal (§3, and Chapter 6).

One honest caveat worth noting: that rollback reverses the *structure* cleanly, but if the column had been populated before you rolled back, dropping it discards that data — the reversal can't bring it back. For a column added empty (as here) it's a clean, true undo; the moment real data lands in it, "drop the column" becomes a destructive reversal to think twice about (Chapter 6).

</details>

> **Exercise 3 — Format choice.** Cortex targets only Postgres and chose SQL format. Give one scenario where you'd instead choose the XML/YAML abstract format, and what you'd gain and lose.

<details>
<summary><strong>Answer</strong></summary>

**Scenario:** you ship a product that customers install on *their* database — some run MySQL, some Postgres, some Oracle — and you must drive all of them from *one* changelog. (A packaged/on-prem application is the classic case.) Here the SQL format fails you: SQL-format changesets are written in one dialect and run only on that dialect.

**Choose XML/YAML.** You describe changes *abstractly* — `<createTable>`, `<addColumn>` — and Liquibase generates the correct dialect-specific SQL for whatever database it's pointed at (§1).

- **Gain:** *database portability* — one changelog applies identically across MySQL, Postgres, and Oracle. You also get **more automatic rollback**: for known structured change types, Liquibase derives the inverse (the undo of `addColumn` is `dropColumn`), so you write fewer `--rollback`s by hand.
- **Lose:** you trade away the things SQL format gives Cortex — writing the *real SQL you already know* instead of learning the `<createTable>` abstraction; *what you see is exactly what runs* (no generation layer to surprise you); and *full access to dialect-specific features* (Postgres `BIGSERIAL`, `TIMESTAMPTZ`, partial indexes) that the portable abstraction may not cover.

For a single-Postgres app like Cortex, none of the portability is worth those costs, so SQL format wins. The instant you must target *multiple* databases from one changelog, the abstraction earns its keep. (And you can mix: a YAML master that *includes* SQL changesets — Cortex's actual layout.)

</details>

```quiz
{
  "prompt": "In a Liquibase SQL-formatted changelog, what do the `--changeset author:id` and `--rollback` comments do?",
  "input": "Choose one:",
  "options": [
    "`--changeset author:id` marks and uniquely identifies a changeset; `--rollback` supplies the SQL to reverse it — wrapping ordinary SQL with identity and reversibility",
    "They are ignored comments with no effect",
    "They connect to the database",
    "They encrypt the SQL statements"
  ],
  "answer": "`--changeset author:id` marks and uniquely identifies a changeset; `--rollback` supplies the SQL to reverse it — wrapping ordinary SQL with identity and reversibility"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — SQL format](https://docs.liquibase.com/concepts/changelogs/sql-format.html)** — the full directive reference (`--changeset`, `--rollback`, `splitStatements`, etc.).
- **[Liquibase — Changelog formats compared](https://docs.liquibase.com/concepts/changelogs/changelog-formats.html)** — when to choose SQL vs XML/YAML/JSON.
- **[Cortex `v1-init.sql`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v1-init.sql)** & **[`v2-submissions.sql`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v2-submissions.sql)** — the real changesets dissected above.

---

**Next:** you've written changesets — but how does the database *know* which ones it has already run, so it never re-applies them? Meet the `DATABASECHANGELOG` table, and watch the schema fill in changeset by changeset. → [5. How Liquibase knows what's applied: DATABASECHANGELOG](/cortex/production-engineering/liquibase/basics/how-liquibase-tracks)
