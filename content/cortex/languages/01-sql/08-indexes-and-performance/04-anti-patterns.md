---
title: Query Anti-Patterns
summary: The half-dozen predicate shapes that block index use, the rewrites that fix them, and the heuristics for spotting "this should be fast but isn't" bugs.
prereqs:
  - sql-indexes-and-performance-b-tree-indexes
  - sql-indexes-and-performance-explain-and-query-plans
---

# 1. Query Anti-Patterns

## The Hook

Six query patterns that look fine but quietly block index use, plus the rewrites:

```sql
-- ❌ Function on column.
WHERE LOWER(email) = 'alice@example.com'
-- ✅ Either store lowercase, or expression index.

-- ❌ Implicit cast.
WHERE id = '42'        -- if id is INT, the string '42' may force casting.
-- ✅ WHERE id = 42

-- ❌ Leading wildcard.
WHERE name LIKE '%foo%'
-- ✅ Trigram index, or rewrite to anchored LIKE 'foo%' if business allows.

-- ❌ OR across non-indexed columns.
WHERE country = 'X' OR score > 100
-- ✅ UNION ALL of two queries (each on its own index), or composite index.

-- ❌ NOT IN with a subquery.
WHERE id NOT IN (SELECT customer_id FROM orders)
-- ✅ NOT EXISTS (null-safe and faster).

-- ❌ Subquery that runs once per row.
SELECT *, (SELECT COUNT(*) FROM orders WHERE customer_id = c.id) FROM customers c
-- ✅ LEFT JOIN aggregating orders + GROUP BY in one pass.
```

This chapter is the catalogue: **patterns to avoid, fixes that work, and the rule of thumb for spotting them in code review**.

---

## Table of contents

1. [Function on indexed column](#function-on-column)
2. [Implicit casts](#implicit-casts)
3. [Leading wildcards](#leading-wildcards)
4. [OR across non-indexed columns](#or-across-columns)
5. [`NOT IN` with subqueries](#not-in-with-subqueries)
6. [Per-row subqueries](#per-row-subqueries)
7. [`SELECT *` in shipped code](#select-star)
8. [Edge cases and pitfalls](#edge-cases-and-pitfalls)
9. [Production reality](#production-reality)
10. [Practice ladder](#practice-ladder)
11. [Cross-links](#cross-links)
12. [Final takeaway](#final-takeaway)

***

# Function on column

Already covered in [B-Tree Indexes: Sargability](/cortex/languages/sql/indexes-and-performance/b-tree-indexes#sargability) and [Strings](/cortex/languages/sql/row-functions/strings#sargability). The summary:

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  Q1["WHERE email = 'x'<br/>(column on left, constant on right)"]
  Q2["WHERE LOWER(email) = 'x'<br/>(function wraps column)"]
  IDX[B-tree index on email]
  USE[Index scan<br/>O(log n)]
  SKIP[Sequential scan<br/>O(n)]

  Q1 --> IDX
  Q2 -.- IDX
  IDX -- sargable --> USE
  Q2 --> SKIP

  style Q1 fill:#bbf7d0,stroke:#16a34a
  style Q2 fill:#fecaca,stroke:#ef4444
  style USE fill:#bbf7d0,stroke:#16a34a
  style SKIP fill:#fecaca,stroke:#ef4444
```

<p align="center"><strong>Sargability: function-on-column hides the column from the index. The index exists but is invisible to the planner. Fix with expression index, or normalise on insert so the function isn't needed.</strong></p>

```sql
-- ❌ Index on email is hidden by LOWER.
WHERE LOWER(email) = 'alice@example.com'

-- ✅ (1) Store lowercase on insert. Then WHERE email = ...
-- ✅ (2) Expression index.
CREATE INDEX users_email_lower_idx ON users (LOWER(email));
```

Audit checklist: any function call wrapping a column in `WHERE` is a candidate non-sargability bug.

---

# Implicit casts

```sql
-- ❌ id is INT; '42' is TEXT. Postgres coerces; the cast may hide the index.
WHERE id = '42'

-- ❌ Worse: cast-on-column.
WHERE CAST(id AS TEXT) = '42'

-- ✅ Match types.
WHERE id = 42
```

In application code with parameterised queries, the language driver typically chooses the right type. The bug shows up in hand-written SQL, ORM-generated SQL with mismatched types, or schemas where the column type is wrong (e.g., `id` stored as `TEXT`).

---

# Leading wildcards

```sql
-- ❌ Anchored at the end — B-tree on name can't help.
WHERE name LIKE '%foo'
WHERE name LIKE '%foo%'

-- ✅ Anchored at the start.
WHERE name LIKE 'foo%'

-- ✅ For unanchored: trigram index.
CREATE EXTENSION pg_trgm;
CREATE INDEX users_name_trgm_idx ON users USING GIN (name gin_trgm_ops);
-- Now WHERE name LIKE '%foo%' is fast.
```

For full-text search at scale, trigram indexes (Postgres) or `tsvector` + GIN are the answer. Brute-force `LIKE '%pattern%'` over a million-row table is unacceptable.

---

# OR across columns

```sql
-- ❌ OR across two non-indexed columns. Often results in Seq Scan.
WHERE country = 'X' OR score > 100

-- ✅ UNION ALL of two queries, each indexable.
SELECT * FROM customers WHERE country = 'X'
UNION ALL
SELECT * FROM customers WHERE score > 100 AND country <> 'X';

-- ✅ Or, composite index spanning both columns (if the cardinality favours it).
CREATE INDEX customers_country_score_idx ON customers (country, score);
```

The `UNION ALL` rewrite assumes you can de-duplicate the overlap (otherwise you'd `UNION`). The planner often does this rewrite automatically (Bitmap Or scans) — but for complex predicates, doing it explicitly is safer.

---

# NOT IN with subqueries

The famous bug from [Anti-joins and Existence](/cortex/languages/sql/multiple-tables/anti-joins-and-existence#not-in):

```sql
-- ❌ Returns no rows if any inner result is NULL. Also slower than NOT EXISTS in some planners.
WHERE id NOT IN (SELECT customer_id FROM orders)

-- ✅ Null-safe and well-optimised.
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = customers.id)
```

`NOT EXISTS` plans to a hash anti-join — fast and null-safe.

---

# Per-row subqueries

A correlated subquery in `SELECT` runs (logically) once per outer row. For a million rows × an inner query that does any real work, that's a million queries:

```sql
-- ❌ Runs the inner SELECT once per customer.
SELECT *, (SELECT COUNT(*) FROM orders WHERE customer_id = c.id) AS order_count
FROM customers c;

-- ✅ One pass over both tables.
SELECT c.*, COALESCE(o.order_count, 0) AS order_count
FROM customers c
LEFT JOIN (
  SELECT customer_id, COUNT(*) AS order_count FROM orders GROUP BY customer_id
) o ON o.customer_id = c.id;
```

Modern Postgres often rewrites correlated subqueries into joins, but not always. For hot paths, write the join form.

---

# SELECT *

Covered in [SELECT and Projection](/cortex/languages/sql/foundations/select-and-projection#select-star). Quick recap:

- Returns more data than needed (slower wire transfer).
- Blocks index-only scans.
- Brittle if the schema changes.

Production code lists columns explicitly. `SELECT *` is fine for ad-hoc psql exploration.

---

# Edge cases and pitfalls

## Implicit cast in JOIN ... ON

```sql
JOIN orders o ON o.customer_id = c.id::TEXT
```

Cast-on-column in `ON` is the same anti-pattern as in `WHERE`. Match types throughout.

## "It's faster on small data"

Many anti-patterns work fine on prototype-scale data and explode in production. The 5-row table doesn't care about your `LIKE '%foo%'`; the 5-million-row one does. Profile against production-scale data.

## OR-EXISTS rewrites

```sql
WHERE EXISTS (a) OR EXISTS (b)
```

Often plans badly. Sometimes a `UNION ALL` of two `EXISTS`-filtered queries is faster.

## Reading `EXPLAIN` for these

A query with one of these anti-patterns typically shows up in `EXPLAIN ANALYZE` as a `Seq Scan` where you expected an `Index Scan`, with high actual time. The diagnosis flow is in [EXPLAIN and Query Plans: common diagnoses](/cortex/languages/sql/indexes-and-performance/explain-and-query-plans#common-diagnoses).

---

# Operational discipline: VACUUM, ANALYZE, REINDEX

The anti-patterns above live in queries; the **operational** anti-patterns live in the *absence* of routine maintenance. Three commands that silently degrade performance when neglected:

**`ANALYZE`** — refreshes table statistics the planner uses for cost estimates. Without recent stats, the planner makes bad choices (the "estimated rows wildly off actual" symptom from the previous chapter). Postgres autovacuum runs `ANALYZE` automatically, but for tables with churn that exceeds autovacuum's thresholds, **schedule a weekly `ANALYZE`** on hot tables. After bulk loads or large data shifts, `ANALYZE` immediately.

**`VACUUM`** — reclaims space from dead row versions (a consequence of MVCC). Autovacuum handles this in normal operation. **Long-running transactions hold back the vacuum horizon** and are the most common cause of unexpected table bloat — the `VACUUM` runs but can't reclaim space because some open transaction might still need the dead rows.

**`REINDEX CONCURRENTLY`** — rebuilds indexes that have accumulated bloat (typically from heavy UPDATE/DELETE workloads). Bloated indexes are larger than necessary and slower to scan. **Schedule a weekly `REINDEX CONCURRENTLY` for indexes on heavily-mutated tables.** The `CONCURRENTLY` form avoids blocking writes during the rebuild.

For very large tables (10M+ rows): consider **partitioning** by a natural axis (date, tenant_id) — `CREATE TABLE ... PARTITION BY RANGE (created_at)` in modern Postgres. Each partition is a separate physical table, which makes `VACUUM`, `REINDEX`, and queries that prune entire partitions dramatically faster.

---

# Production reality

A code-review checklist for SQL touching hot paths:

- ✅ No function-on-column in `WHERE`.
- ✅ No implicit casts (parameter types match column types).
- ✅ No unanchored `LIKE` without a trigram index.
- ✅ No `NOT IN` with subqueries (use `NOT EXISTS`).
- ✅ No correlated subqueries in `SELECT` for million-row tables.
- ✅ No `SELECT *` in shipped queries.
- ✅ Composite indexes ordered by query pattern (most-selective first).
- ✅ Every new query has an `EXPLAIN ANALYZE` checked at PR review time.

A weekly maintenance checklist for the database:

- ✅ `ANALYZE` ran (autovacuum handles most cases; verify on hot tables).
- ✅ No long-running transactions blocking vacuum (`pg_stat_activity` for transactions older than a few minutes is suspicious).
- ✅ Index bloat checked; `REINDEX CONCURRENTLY` for heavily-mutated indexes.
- ✅ For 10M+ row tables: partitioning strategy in place.

Most production SQL bugs that look like "the database is slow" trace back to one of these. The discipline of avoiding them turns "the database is the bottleneck" into "the database is fine; check the network."

---

# Practice ladder

1. **Rewrite `WHERE LOWER(email) = 'x'` to be sargable.** *Hint: expression index, or normalised storage.*
2. **Why might `WHERE id = '42'` (id is INT) be slower than `WHERE id = 42`?** *Hint: implicit cast.*
3. **Rewrite `WHERE id NOT IN (SELECT customer_id FROM orders)` to be null-safe.** *Hint: `NOT EXISTS`.*
4. **Why is this query potentially slow?**
   ```sql
   SELECT *, (SELECT COUNT(*) FROM orders WHERE customer_id = c.id) FROM customers c;
   ```
   *Hint: correlated subquery in SELECT, runs per row.*
5. **Provide a fast pattern for `WHERE name LIKE '%foo%'` on a million-row table.** *Hint: trigram GIN index.*

***

# Cross-links

- **Previous in this module:** [EXPLAIN and Query Plans](/cortex/languages/sql/indexes-and-performance/explain-and-query-plans).
- **Module complete.** Next: [Transactions and Concurrency](/cortex/languages/sql/transactions-and-concurrency/index).

***

# Final Takeaway

Anti-patterns are the predicates that block index use. Three patterns to internalise:

1. **Function-on-column kills indexes.** `LOWER(col)`, `CAST(col AS X)`, `col + 1` — all of them. Fix: store the normalised form, or build an expression index.
2. **`NOT IN` with subqueries → `NOT EXISTS`.** The null-safety and the plan are both better.
3. **A code-review checklist beats a debug session.** Catch these patterns before they ship; production query plans should be examined, not assumed.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>
