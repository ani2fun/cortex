---
title: Working with Multiple Tables
summary: How to combine rows from two or more tables — joins, set operators, subqueries, and the anti-join family. The half of SQL where the planner starts to matter and where most production query bugs live.
prereqs:
  - sql-foundations-introduction-to-sql
  - sql-foundations-filtering
---

# Working with Multiple Tables

A single-table query is the *easy* SQL. The interesting questions — "which customers haven't ordered in six months", "what's the running revenue by country", "find orders whose customer no longer exists" — all involve **combining rows from more than one table**. That combination has many shapes: joins, set operators, subqueries, anti-joins. This module covers the shapes you'll use daily, the shapes that hide bugs, and the rule of thumb for which one to reach for in each situation.

The pivot from single-table to multi-table SQL is also the pivot where **the query planner starts to matter**. A nested-loop join over a million rows can take seconds; the same join with the right index can take milliseconds. The chapters here teach you the shape of each operation; the [Indexes and Performance](/cortex/languages/sql/index) module later teaches you how the planner *executes* them.

## Place in the curriculum

- **Prerequisites:** [Foundations](/cortex/languages/sql/foundations/index). The logical execution order, projection, and filtering must be in your fingertips before joins make sense.
- **Followed by:** [Aggregation](/cortex/languages/sql/aggregation/index). Once you can combine rows, the next thing you'll want is to summarise them.

## Chapters

1. [Joins](/cortex/languages/sql/multiple-tables/joins) — `INNER`/`LEFT`/`RIGHT`/`FULL`/`CROSS`, `ON` vs `WHERE`, multi-table joins, the most common mistakes.
2. [Set Operators](/cortex/languages/sql/multiple-tables/set-operators) — `UNION`/`UNION ALL`/`INTERSECT`/`EXCEPT`. Combine *result-sets*, not tables.
3. [Subqueries](/cortex/languages/sql/multiple-tables/subqueries) — scalar, derived tables, `IN`, `EXISTS`, `ANY`/`ALL`, and the correlated-subquery pattern.
4. [Anti-joins and Existence](/cortex/languages/sql/multiple-tables/anti-joins-and-existence) — "rows where no match" — `NOT EXISTS`, `LEFT JOIN ... IS NULL`, why `NOT IN` is the wrong tool when `NULL`s are in play.
