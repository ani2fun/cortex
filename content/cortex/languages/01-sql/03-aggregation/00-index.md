---
title: Aggregation
summary: How to summarise rows — `GROUP BY`, the eight aggregate functions you'll use 90% of the time, and the multi-dimensional aggregation tools (`ROLLUP`, `CUBE`, `GROUPING SETS`) that produce subtotals in a single query.
prereqs:
  - sql-foundations-introduction-to-sql
  - sql-multiple-tables-joins
---

# Aggregation

A `SELECT` over `customers` returns one row per customer. A `SELECT` *plus* `GROUP BY country` returns one row per country, with the per-country aggregates (count, sum, average, min, max) computed across that country's rows. Aggregation is the operation that turns "many rows" into "one row per group" — the language for *summarising* data instead of listing it.

This module covers the three pieces: the mechanics of `GROUP BY` and `HAVING`, the catalogue of aggregate functions and their non-obvious corners (especially around `NULL`), and the multi-dimensional grouping operators (`ROLLUP`, `CUBE`, `GROUPING SETS`) that compute subtotals at multiple levels in one query.

## Place in the curriculum

- **Prerequisites:** [Foundations](/cortex/languages/sql/foundations/index) and [Joins](/cortex/languages/sql/multiple-tables/joins). `GROUP BY` is a step in the [logical execution order](/cortex/languages/sql/foundations/introduction-to-sql#the-logical-execution-order), and most aggregation queries operate on joined tables.
- **Followed by:** [Window Functions](/cortex/languages/sql/index). Window functions generalise aggregation — you can keep one row per *original* row while computing aggregates across a window of related rows.

## Chapters

1. [GROUP BY and HAVING](/cortex/languages/sql/aggregation/group-by-and-having) — the mechanics: how rows collapse into groups, what columns are legal in `SELECT` after a `GROUP BY`, and the difference between `WHERE` (filters rows before grouping) and `HAVING` (filters groups after).
2. [Aggregate Functions](/cortex/languages/sql/aggregation/aggregate-functions) — `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `STRING_AGG`/`GROUP_CONCAT`, plus the modifiers (`DISTINCT`, `FILTER`) that change their meaning. Plus the NULL behaviour that surprises everyone the first time.
3. [Grouping Sets, ROLLUP, CUBE](/cortex/languages/sql/aggregation/grouping-sets-rollup-cube) — multi-dimensional aggregation in a single query: subtotals by country, subtotals by month, subtotals by both. The right tool for spreadsheet-style "what does the data look like at every level."
