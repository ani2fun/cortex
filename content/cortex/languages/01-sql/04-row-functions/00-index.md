---
title: Row Functions
summary: Per-row computation — strings, numbers, dates and times, NULL handling, and the CASE expression. The "make each row a little smarter" half of SQL, where most data-quality fixes live.
prereqs:
  - sql-foundations-introduction-to-sql
  - sql-foundations-select-and-projection
---

# Row Functions

Aggregate functions ([previous module](/cortex/languages/sql/aggregation/index)) summarise *many* rows into *one*. **Row functions** are the opposite — they take *one* row and produce *one* value, on a per-row basis. They're the per-row computation layer: lowercase a name, parse a date, substitute a default for a NULL, branch on a value with `CASE`. Most data-quality fixes — "this column has trailing whitespace," "these dates are in the wrong timezone," "NULL means default" — happen here.

## Place in the curriculum

- **Prerequisites:** [Foundations](/cortex/languages/sql/foundations/index). The `SELECT` projection and `WHERE` filtering chapters introduce row functions in passing; this module covers the catalogue properly.
- **Followed by:** [Window Functions](/cortex/languages/sql/window-functions/index). Window functions blend per-row and per-group computation — they keep the original row and *also* compute aggregates over a window. The row-function fluency you build here makes window functions much easier.

## Chapters

1. [Strings](/cortex/languages/sql/row-functions/strings) — `LOWER`, `UPPER`, `TRIM`, `LENGTH`, `SUBSTRING`, `REPLACE`, `CONCAT`, `LIKE`/`SIMILAR TO`/regex.
2. [Numbers](/cortex/languages/sql/row-functions/numbers) — arithmetic, `ROUND`, `FLOOR`, `CEIL`, `MOD`, `ABS`, type coercion and integer-division traps.
3. [Dates and Times](/cortex/languages/sql/row-functions/dates-and-times) — `DATE_TRUNC`, `EXTRACT`, `INTERVAL` arithmetic, timezones, parsing and formatting.
4. [NULL and Three-Valued Logic](/cortex/languages/sql/row-functions/null-and-three-valued-logic) — `COALESCE`, `NULLIF`, `IS DISTINCT FROM`, the truth tables that explain every NULL bug, and the patterns to make NULL-handling explicit.
5. [CASE Expressions](/cortex/languages/sql/row-functions/case-expressions) — `CASE WHEN ... THEN ... ELSE ... END`. The if-else chain in SQL; powers bucket categorisation, conditional aggregation, and pivot patterns.
