---
title: Advanced Patterns
summary: Hierarchies and graphs, JSON in SQL, pivoting/unpivoting, and time-series patterns — the topics that don't fit cleanly in other modules but show up regularly in production.
prereqs:
  - sql-window-functions-window-patterns
  - sql-ctes-and-recursion-recursive-ctes
---

# Advanced Patterns

The SQL toolkit is mostly composed by Module 9. This last module covers four patterns that don't fit cleanly elsewhere but show up routinely in real production work.

## Place in the curriculum

- **Prerequisites:** comfort with the rest of the curriculum, especially CTEs and window functions.

## Chapters

1. [Hierarchies and Graphs](/cortex/languages/sql/advanced-patterns/hierarchies-and-graphs) — adjacency lists, closure tables, materialised paths, ltree (Postgres). When recursion isn't enough.
2. [JSON in SQL](/cortex/languages/sql/advanced-patterns/json-in-sql) — `JSONB`, indexing JSON paths, when to use JSON vs columns.
3. [Pivoting and Unpivoting](/cortex/languages/sql/advanced-patterns/pivoting-and-unpivoting) — turning rows into columns and vice versa.
4. [Time-Series Patterns](/cortex/languages/sql/advanced-patterns/time-series-patterns) — bucketing, gap-filling, retention windows, the patterns specific to time-stamped data at scale.
