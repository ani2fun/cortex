---
title: Indexes and Performance
summary: How indexes work, when to add them, and how to read query plans. The module that turns "this query is slow" into "I know exactly why."
prereqs:
  - sql-foundations-filtering
  - sql-multiple-tables-joins
  - sql-schema-and-constraints-keys-and-references
---

# Indexes and Performance

Every other module so far has assumed your queries return correctly. This one is about returning correctly *fast*. Indexes are the single biggest lever — the difference between a query that takes 5 ms and the same query taking 5 minutes.

## Place in the curriculum

- **Prerequisites:** comfort with `JOIN`, `WHERE`, schema design.
- **Followed by:** [Transactions and Concurrency](/cortex/languages/sql/index). Indexes affect concurrency too — they reduce lock contention and enable better isolation.

## Chapters

1. [B-Tree Indexes](/cortex/languages/sql/indexes-and-performance/b-tree-indexes) — how a B-tree works, when an index helps, sargability, covering indexes.
2. [Other Index Types](/cortex/languages/sql/indexes-and-performance/other-index-types) — Hash, GIN, GiST, BRIN, partial indexes, expression indexes.
3. [EXPLAIN and Query Plans](/cortex/languages/sql/indexes-and-performance/explain-and-query-plans) — reading the planner's output, scan types, join algorithms, cost vs actual.
4. [Query Anti-Patterns](/cortex/languages/sql/indexes-and-performance/anti-patterns) — function-on-column, leading wildcards, OR vs UNION, the patterns that block index use.
