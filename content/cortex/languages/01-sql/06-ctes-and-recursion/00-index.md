---
title: CTEs and Recursion
summary: `WITH` clauses for naming intermediate query results, and recursive CTEs for hierarchies, graphs, and generated sequences.
prereqs:
  - sql-multiple-tables-subqueries
---

# CTEs and Recursion

A subquery in `FROM` works fine for one or two layers of computation. Past that, queries become unreadable nests of parentheses. **Common Table Expressions** (`WITH name AS (subquery)`) are the readability fix — name your intermediate results, then refer to them by name in the main query.

CTEs also unlock **recursion** — queries that reference themselves to traverse hierarchies (employee/manager trees), graphs (friendship networks), or to generate sequences (calendar tables, date ranges).

## Place in the curriculum

- **Prerequisites:** [Subqueries](/cortex/languages/sql/multiple-tables/subqueries) — a CTE is a named subquery; the syntax is the only difference.
- **Followed by:** [Schema and Constraints](/cortex/languages/sql/index). Once you can express complex queries cleanly, modelling them well at the schema level is the natural next concern.

## Chapters

1. [Non-recursive CTEs](/cortex/languages/sql/ctes-and-recursion/non-recursive-ctes) — `WITH name AS (...)`. Multi-step queries, modularity, materialisation hints.
2. [Recursive CTEs](/cortex/languages/sql/ctes-and-recursion/recursive-ctes) — `WITH RECURSIVE`. Walking hierarchies, generating sequences, graph traversal.
