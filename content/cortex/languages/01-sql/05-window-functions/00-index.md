---
title: Window Functions
summary: Aggregates without collapsing rows — running totals, ranks, lead/lag, top-N per group. The single most-asked SQL topic in modern interviews and the right tool for every "per row, but with context" question.
prereqs:
  - sql-aggregation-aggregate-functions
  - sql-foundations-ordering-and-pagination
---

# Window Functions

A regular aggregate (`GROUP BY country, SUM(sales)`) collapses N rows into 1 row. A **window function** runs the same aggregate but **doesn't collapse** — it leaves all N rows in place and *adds* a column with the aggregated value computed over a "window" of related rows. Running totals, per-row ranking, "the previous row's value," "top 3 per group" — all are window-function questions.

This is the module that turns "I can write SQL" into "I can write *modern* SQL." Window functions arrived in standard SQL in 2003 but were absent from MySQL until 8.0 (2018) and SQLite until 3.25 (2018), so a generation of engineers learned SQL without them and still reach for clunkier subquery patterns. **Once you know windows, you'll keep finding queries that should be windows.**

## Place in the curriculum

- **Prerequisites:** [Aggregation](/cortex/languages/sql/aggregation/index) — windows reuse the same aggregate functions (`SUM`, `COUNT`, `AVG`, `RANK`, etc.) but apply them over a *window of rows*, not a *group of rows*. Comfort with `GROUP BY` is essential. Also [Ordering and Pagination](/cortex/languages/sql/foundations/ordering-and-pagination) for the `ORDER BY` semantics inside `OVER`.
- **Followed by:** [CTEs and Recursion](/cortex/languages/sql/index). Many window-function patterns benefit from a CTE wrapper to name the windowed columns before filtering or further aggregating.

## Chapters

1. [Window Basics](/cortex/languages/sql/window-functions/window-basics) — the `OVER`, `PARTITION BY`, `ORDER BY` triple. The mental model that turns every window function into "a regular aggregate, computed over this window per row."
2. [Frames](/cortex/languages/sql/window-functions/frames) — `ROWS` vs `RANGE` vs `GROUPS`, `BETWEEN ... AND ...`, the default frame and why it surprises people.
3. [Ranking](/cortex/languages/sql/window-functions/ranking) — `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `NTILE`, `PERCENT_RANK`. The functions that label each row with its position.
4. [Value Functions](/cortex/languages/sql/window-functions/value-functions) — `LAG`, `LEAD`, `FIRST_VALUE`, `LAST_VALUE`, `NTH_VALUE`. "The previous row," "the first row in this window," etc.
5. [Window Patterns](/cortex/languages/sql/window-functions/window-patterns) — gaps and islands, sessionisation, top-N per group, running totals, the canonical "I see this in production" patterns.
