---
title: Types
summary: Choosing the right column type — numeric, character, temporal, JSON, arrays. The decisions that determine storage cost, query speed, and which bugs you'll hit later.
prereqs:
  - sql-foundations-data-definition
---

# 1. Types

## The Hook

A schema review at code-review time:

```sql
CREATE TABLE events (
  id          VARCHAR(50)  PRIMARY KEY,
  created_at  VARCHAR(20),
  amount      FLOAT,
  data        TEXT
);
```

Four columns, four wrong types. `id` as `VARCHAR` means PK lookups can't use compact integer indexes. `created_at` as `VARCHAR` means date arithmetic requires casting on every read. `amount` as `FLOAT` means binary-float drift on financial sums. `data` as `TEXT` containing JSON means no JSON indexing, no querying inside the JSON, just opaque blobs.

Every one of those decisions is permanent — changing a column type after data lands costs a migration with downtime risk. **Schema-design choices are made on day 1 and live forever.**

This chapter is the type-selection guide: which type for which kind of column, and what each choice locks you into. By the end you'll be able to design a schema that's correct, fast, and survives the data growing beyond your prototype expectations.

---

## Table of contents

1. [Numeric types](#numeric-types)
2. [Character types](#character-types)
3. [Temporal types](#temporal-types)
4. [Boolean](#boolean)
5. [JSON / JSONB](#json-jsonb)
6. [Arrays (Postgres)](#arrays)
7. [UUID](#uuid)
8. [Type selection rules](#type-selection-rules)
9. [Edge cases and pitfalls](#edge-cases-and-pitfalls)
10. [Production reality](#production-reality)
11. [Practice ladder](#practice-ladder)
12. [Cross-links](#cross-links)
13. [Final takeaway](#final-takeaway)

***

# Numeric types

Recap from [Data Definition](/cortex/languages/sql/foundations/data-definition#column-types):

| Type | Use case |
|---|---|
| `INTEGER` | Most ID columns, counts |
| `BIGINT` | Unbounded counters, timestamps in ms, large IDs |
| `SMALLINT` | Tiny ranges (rare) |
| `NUMERIC(p, s)` | **Money**, exact decimal |
| `DOUBLE PRECISION` | Approximate floats — physics, ML, *not money* |
| `REAL` | Single-precision float — even less precision |

The decisions:

1. **`INT` vs `BIGINT` for IDs.** `INT` overflows at 2.1 billion. If you're certain your table will stay below that, `INT` saves 4 bytes per row (which compounds across indexes). For an "events" table that grows by millions per day, use `BIGINT`. For a "countries" table with 200 rows, `INT` is fine.
2. **`NUMERIC` vs `DOUBLE PRECISION`.** Money goes in `NUMERIC`. Physics measurements in `DOUBLE PRECISION`. If you're not sure, `NUMERIC(p, s)` is the safer default — it's slower but exact. The chapter on [Numbers](/cortex/languages/sql/row-functions/numbers) has the full discussion.
3. **`NUMERIC(p, s)` choice.** `NUMERIC(12, 2)` is fine for application-scale money (12 total digits, 2 after decimal — up to ±$10 billion with cent precision). Crypto and scientific applications use higher precision.

---

# Character types

| Type | Behaviour |
|---|---|
| `TEXT` | Variable-length, no length limit |
| `VARCHAR(n)` | Variable-length, max length n |
| `CHAR(n)` | Fixed-length, padded to n with spaces |

**Default to `TEXT` in Postgres.** No performance difference vs `VARCHAR(n)`, no arbitrary length cap to revisit later.

`VARCHAR(n)` has one legitimate use: when you want the database to enforce a maximum length as a constraint. For an externally-provided field where exceeding the length is a bug, `VARCHAR(n)` is the schema enforcing it.

`CHAR(n)` is rarely useful. Padding to length surprises. Avoid.

> **Dialect note:** MySQL distinguishes between `VARCHAR` (max ~65k characters) and `TEXT`/`MEDIUMTEXT`/`LONGTEXT` (longer); the indexing behaviour differs. SQL Server has `VARCHAR(MAX)` for unlimited. Postgres's `TEXT` is the simplest.

---

# Temporal types

Recap from [Dates and Times](/cortex/languages/sql/row-functions/dates-and-times):

| Type | When |
|---|---|
| `DATE` | Calendar date, no time |
| `TIMESTAMP` | Date + time, **no** timezone — usually wrong |
| `TIMESTAMPTZ` | Date + time, normalised to UTC — usually right |
| `INTERVAL` | A duration |

**Default to `TIMESTAMPTZ` for any "when did this happen" column.** Storing UTC and converting on display is the correct approach. `TIMESTAMP` (without TZ) is a footgun for cross-timezone applications.

`DATE` is fine for fields that genuinely don't care about time-of-day — birthdates, holidays, due dates.

For **epoch-millisecond timestamps** (codefolio's `hello_events.timestamp_ms`), use `BIGINT`. Portable across SQL/NoSQL/JS, no timezone ambiguity at storage.

---

# Boolean

```sql
is_active BOOLEAN NOT NULL DEFAULT TRUE
```

Postgres's first-class `BOOLEAN` accepts `TRUE`, `FALSE`, `NULL`. Use it.

> **Dialect note:** SQL Server uses `BIT`. MySQL aliases `BOOLEAN` to `TINYINT(1)`. Postgres's true `BOOLEAN` is the cleanest.

---

# JSON / JSONB

Postgres has two JSON types:

- **`JSON`** — stored as text. Validated for syntax. Queries re-parse on every access.
- **`JSONB`** — stored binary. Decomposed into a queryable form. Indexable. Slightly slower writes (decomposing), much faster reads.

**Use `JSONB`** in production unless you have a specific reason not to. The full treatment is in [JSON in SQL](/cortex/languages/sql/index) (Advanced Patterns).

```sql
CREATE TABLE events (
  id BIGINT PRIMARY KEY,
  payload JSONB NOT NULL
);
CREATE INDEX events_payload_user_idx ON events ((payload->>'user_id'));
```

That expression index makes `WHERE payload->>'user_id' = '42'` fast.

When to use JSON:
- Genuinely schemaless data — webhooks, audit logs of arbitrary changes, configuration blobs.
- Nested data where modelling each field as a column would create dozens of mostly-NULL columns.

When *not* to:
- Structured data with a known schema. Every field that has a known meaning belongs in its own column. JSON is a fallback, not a default.

---

# Arrays

Postgres supports array column types: `INTEGER[]`, `TEXT[]`, etc.

```sql
CREATE TABLE posts (
  id BIGINT PRIMARY KEY,
  tags TEXT[]
);
```

When useful:
- Short, bounded lists (tags on a post, recipient list on an email).
- Querying with array operators (`tags @> ARRAY['urgent']` for "contains").

When not:
- Long lists or lists that grow unboundedly. Use a child table.
- Lists you'll JOIN against. Arrays are awkward to join.

The general rule: **prefer a child table for relational data; arrays for opaque short lists**.

---

# UUID

Globally unique 128-bit identifier. Useful when:

- ID generation can't be centralised (multi-region, offline-capable client).
- You don't want sequential IDs to leak business information ("user 12 signed up after user 11").
- Distributed systems where coordinator-free unique IDs simplify architecture.

Trade-offs:
- 16 bytes per row vs 4 (INT) or 8 (BIGINT). Larger indexes.
- Random UUIDs (v4) make B-tree inserts random — write performance suffers on large tables.
- UUID v7 (time-ordered) avoids the random-insert problem; modern code prefers v7.

Postgres has the `uuid` type and the `pgcrypto` extension's `gen_random_uuid()`.

---

# Type selection rules

| Question | Type |
|---|---|
| ID, growing, business data | `BIGINT GENERATED ALWAYS AS IDENTITY` (or UUID v7) |
| ID, small lookup table | `INT GENERATED ALWAYS AS IDENTITY` |
| Money | `NUMERIC(12, 2)` |
| Counts that won't reach 2B | `INT` |
| Counts that might (events, requests) | `BIGINT` |
| Text, no length limit needed | `TEXT` |
| Text, hard cap | `VARCHAR(n)` |
| When-did-it-happen timestamp | `TIMESTAMPTZ` |
| Just a date (birthdate, due date) | `DATE` |
| Booleans | `BOOLEAN` |
| Schemaless / variable-shape blob | `JSONB` |
| Short bounded list of primitives | `array` (Postgres) or child table |
| Externally-generated ID | `UUID` |

---

# Edge cases and pitfalls

## Storing IDs as strings

Don't. Even if the ID has letters (e.g., `'INV-12345'`), break it into components: `kind TEXT NOT NULL`, `seq BIGINT NOT NULL`. String IDs make joins and indexes slower; component IDs are flexible.

## Choosing too narrow a type

Migrating from `INT` to `BIGINT` is *much* worse than starting with `BIGINT`. The conversion rewrites every row. Default to `BIGINT` for any column that might grow.

## Storing timestamps as `INT` Unix seconds

Year 2038 problem. Use `BIGINT` (milliseconds) or `TIMESTAMPTZ`.

## Implicit casts

`'42' = 42` works in some dialects but not others. Type columns correctly so this never matters.

---

# Production reality

The codefolio schema (from CLAUDE.md):

- `visits.id`: `INT GENERATED ALWAYS AS IDENTITY` — single-row counter table, will never exceed 2B IDs.
- `visits.count`: `BIGINT NOT NULL DEFAULT 0` — the counter itself, unbounded.
- `hello_events.id`: `BIGINT` — append-only events, will exceed INT range.
- `hello_events.timestamp_ms`: `BIGINT` — epoch milliseconds; portable across systems.

Each choice reflects the type-selection rules above. The schema was designed before the system grew; the types accommodate growth without migration.

---

# Practice ladder

1. **What type for a price column?** *Hint: NUMERIC.*
2. **What type for a boolean is_paid flag?** *Hint: BOOLEAN.*
3. **What type for an event timestamp?** *Hint: TIMESTAMPTZ (or BIGINT ms-since-epoch for portability).*
4. **What type for a 1-million-rows table's primary key?** *Hint: INT is fine; if growth is uncertain, BIGINT.*
5. **A schema has `email VARCHAR(255)`. Should you change to TEXT?** *Hint: in Postgres, no performance difference; either is fine; pick based on whether the cap is meaningful.*
6. **A column will store free-form JSON payloads. JSON or JSONB?** *Hint: JSONB for production.*

***

# Cross-links

- **Previous module:** [CTEs and Recursion](/cortex/languages/sql/ctes-and-recursion/index).
- **Next in this module:** [Keys and References](/cortex/languages/sql/schema-and-constraints/keys-and-references).
- **Cross-reference:** [Numbers](/cortex/languages/sql/row-functions/numbers) for the NUMERIC vs FLOAT decision; [Dates and Times](/cortex/languages/sql/row-functions/dates-and-times) for the TIMESTAMP vs TIMESTAMPTZ decision.

***

# Final Takeaway

Type choice is permanent. Three patterns to internalise:

1. **Default to the type that survives growth.** `BIGINT` for IDs, `TEXT` for text, `TIMESTAMPTZ` for timestamps, `NUMERIC` for money. Migration costs are real.
2. **Stringly-typed columns are a code smell.** Numbers in TEXT, dates in VARCHAR, booleans in INT — each is the schema giving up its job.
3. **JSONB for genuinely schemaless data; columns for everything else.** Reach for JSONB last, after considering whether the data should be modelled as columns.

Master these three and your schemas survive the prototype-to-production transition without painful rewrites.
