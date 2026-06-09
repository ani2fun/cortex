---
title: Pivoting and Unpivoting
summary: Turning rows into columns (pivot) and columns into rows (unpivot) — the SQL patterns for spreadsheet-shaped reports and "long format" data preparation.
prereqs:
  - sql-aggregation-aggregate-functions
  - sql-row-functions-case-expressions
---

# 1. Pivoting and Unpivoting

## The Hook

Two table shapes:

**Long format** (one row per measurement):
```
date       | metric        | value
2026-04-01 | requests      | 1024
2026-04-01 | errors        | 12
2026-04-02 | requests      | 1100
2026-04-02 | errors        | 8
```

**Wide format** (one row per date, columns per metric):
```
date       | requests | errors
2026-04-01 | 1024     | 12
2026-04-02 | 1100     | 8
```

Long format is what databases natively store; wide format is what spreadsheets and dashboards display. **Pivoting** is going from long → wide; **unpivoting** is going from wide → long.

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
  subgraph LONG["Long format<br/>(one row per measurement)"]
    direction TB
    L1["date | metric    | value"]
    L2["4-01 | requests  | 1024"]
    L3["4-01 | errors    | 12"]
    L4["4-02 | requests  | 1100"]
    L5["4-02 | errors    | 8"]
  end
  subgraph WIDE["Wide format<br/>(one row per date)"]
    direction TB
    W1["date | requests | errors"]
    W2["4-01 | 1024     | 12"]
    W3["4-02 | 1100     | 8"]
  end

  LONG -- "PIVOT (CASE)" --> WIDE
  WIDE -- "UNPIVOT (UNION ALL)" --> LONG

  style LONG fill:#dbeafe,stroke:#3b82f6
  style WIDE fill:#bbf7d0,stroke:#16a34a
```

<p align="center"><strong>Pivot rotates rows into columns; unpivot does the reverse. Same data, different shape, different consumer needs.</strong></p>

This chapter covers the SQL patterns for both directions, with and without dialect-specific `PIVOT` syntax.

---

## Table of contents

1. [Pivoting with `CASE`](#pivot-with-case)
2. [Pivoting with `crosstab` (Postgres)](#crosstab)
3. [Unpivoting with `UNION ALL`](#unpivot-with-union-all)
4. [Unpivoting with `LATERAL` and arrays](#unpivot-with-lateral)
5. [Edge cases and pitfalls](#edge-cases-and-pitfalls)
6. [Production reality](#production-reality)
7. [Practice ladder](#practice-ladder)
8. [Cross-links](#cross-links)
9. [Final takeaway](#final-takeaway)

***

# Pivot with CASE

The most portable pivot — a `SUM` (or `MAX`) wrapped in a `CASE` per output column:

```sql run
CREATE TABLE measurements (date DATE, metric TEXT, value INT);
INSERT INTO measurements VALUES
  ('2026-04-01','requests',1024),('2026-04-01','errors',12),
  ('2026-04-02','requests',1100),('2026-04-02','errors',8);

-- Pivot: one row per date, columns per metric.
SELECT date,
       SUM(CASE WHEN metric = 'requests' THEN value END) AS requests,
       SUM(CASE WHEN metric = 'errors'   THEN value END) AS errors
FROM measurements
GROUP BY date
ORDER BY date;
```

Two output columns (`requests`, `errors`), one row per `date`. Works in every dialect. The trick is **NULL-as-skip**: `CASE` with no `ELSE` returns NULL for non-matching rows; `SUM` ignores NULL.

The limitation: **column names are hard-coded**. Adding a new metric ('latency') means editing the SQL. For dynamic pivots — where the column list comes from data — you generate the SQL in application code or use the dialect-specific `PIVOT` operator.

---

# Crosstab (Postgres)

Postgres's `tablefunc` extension provides a `crosstab` function:

```sql
CREATE EXTENSION IF NOT EXISTS tablefunc;

SELECT *
FROM crosstab(
  'SELECT date, metric, value FROM measurements ORDER BY date, metric',
  'SELECT DISTINCT metric FROM measurements ORDER BY metric'
) AS ct(date DATE, errors INT, requests INT);
```

Two arguments: the source query and the column-list query. The result is the pivoted table.

In practice, the `CASE`-based form is more readable than `crosstab` for hand-written queries. `crosstab` is useful when you're generating the pivot programmatically.

> **Dialect note:** SQL Server has a built-in `PIVOT` operator. Oracle has `PIVOT`. MySQL has neither — `CASE` is the answer.

---

# Unpivot with UNION ALL

The portable inverse — wide format to long:

```sql run
CREATE TABLE wide (date DATE, requests INT, errors INT);
INSERT INTO wide VALUES ('2026-04-01',1024,12),('2026-04-02',1100,8);

-- Unpivot: one row per (date, metric, value).
SELECT date, 'requests' AS metric, requests AS value FROM wide
UNION ALL
SELECT date, 'errors',   errors   FROM wide
ORDER BY date, metric;
```

One `SELECT` per source column, `UNION ALL` to stack them. Works in every dialect.

For two columns, this is fine. For 50, the SQL grows tediously. Postgres has tidier alternatives:

---

# Unpivot with LATERAL and arrays

Postgres can unpivot using `LATERAL` and `unnest`:

```sql
SELECT w.date, m.metric, m.value
FROM wide w
CROSS JOIN LATERAL (VALUES
  ('requests', w.requests),
  ('errors',   w.errors)
) AS m(metric, value)
ORDER BY w.date, m.metric;
```

The inline `VALUES` pairs each (metric_name, value) per row of `wide`; `LATERAL` lets each row "fan out" into multiple result rows.

For very wide tables, this is more compact than long `UNION ALL` chains.

---

# Edge cases and pitfalls

## Pivot column names are hard-coded

Manual pivots can't pick up new metric values without editing SQL. For dynamic pivots, generate the SQL in app code (concatenate the metric list into the `CASE` chain), or use a BI tool that does pivoting client-side.

## Aggregation choice in pivots

`SUM(CASE ...)` aggregates if multiple rows share the (date, metric) tuple. If you want "the value at this exact tuple, not aggregated," use `MAX` (relying on the assumption that there's only one match) — or fix the source data so the (date, metric) tuple is unique.

## Type unification in unpivots

```sql
SELECT 'a' AS metric, requests AS value FROM wide
UNION ALL
SELECT 'b', revenue FROM wide;
```

If `requests` is `INT` and `revenue` is `NUMERIC`, the `value` column unifies to `NUMERIC`. Mismatched types may need explicit casts.

## NULL handling

In the `CASE`-based pivot, missing combinations come out as NULL. Use `COALESCE(..., 0)` if your downstream consumer expects 0.

---

# Production reality

The classic pivot use-case: **dashboards**. A dashboard shows columns "this week," "last week," "month," etc. Backend stores long format; query pivots:

```sql
SELECT user_id,
       SUM(CASE WHEN timestamp_ms >= NOW_MS - 7*86400000 THEN visits ELSE 0 END) AS visits_week,
       SUM(CASE WHEN timestamp_ms >= NOW_MS - 30*86400000 THEN visits ELSE 0 END) AS visits_month
FROM hello_events
GROUP BY user_id;
```

Two columns of pre-computed metrics, one row per user.

The classic unpivot: **converting wide imports to long format** for storage. CSV files often arrive wide (one column per metric); the database stores long; an unpivot is the bridge.

---

# Practice ladder

1. **Pivot a long-format table to wide using `SUM(CASE ...)`.** *Hint: one `CASE` per output column.*
2. **Unpivot a wide-format table to long using `UNION ALL`.** *Hint: one `SELECT` per source column.*
3. **Why does the pivot use `SUM` instead of just selecting the value?** *Hint: GROUP BY collapses; aggregates are required.*
4. **What if you want NULL → 0 in the pivot output?** *Hint: `COALESCE(SUM(...), 0)` or `SUM(CASE ... ELSE 0 END)`.*

***

# Cross-links

- **Previous in this module:** [JSON in SQL](/cortex/languages/sql/advanced-patterns/json-in-sql).
- **Next in this module:** [Time-Series Patterns](/cortex/languages/sql/advanced-patterns/time-series-patterns).
- **Cited:** [CASE Expressions](/cortex/languages/sql/row-functions/case-expressions) — the core of conditional pivots.

***

# Final Takeaway

Pivot/unpivot reshape data between long and wide. Three patterns to internalise:

1. **Pivot with `SUM(CASE WHEN ... THEN ... END)`.** Portable, readable, the universal answer.
2. **Unpivot with `UNION ALL` of one `SELECT` per source column.** Works everywhere.
3. **Hard-coded column lists are the limitation; for dynamic pivots, generate SQL in the application or use BI-tool pivoting.**
