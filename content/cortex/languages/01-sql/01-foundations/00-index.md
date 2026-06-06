---
title: Foundations
summary: The half-dozen ideas every later SQL chapter assumes — what SQL is, the logical order it runs your clauses in, projection, filtering, ordering, and how rows get into the table in the first place.
prereqs: []
---

# Foundations

Before any join, aggregate, or window function makes sense, six things have to be in place: **what SQL is** (a declarative query language, not a procedural one), **the logical order in which a SELECT actually runs** (FROM → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT — *not* the order you write them), **how to project columns**, **how to filter rows** (and the NULL trap that catches every junior engineer at least once), **how to sort and paginate** (and why `LIMIT/OFFSET` falls over at scale), and **how to define and modify the data** in the first place.

This module covers all six. Every later chapter in the SQL section leans on the vocabulary built here — so if you skip it, you'll find yourself bluffing through query plans for the rest of your career.

## Place in the curriculum

- **Prerequisites:** none. Basic programming literacy is enough; no prior SQL experience assumed.
- **Followed by:** every other module. The [Introduction](/cortex/languages/sql-foundations-introduction-to-sql) chapter especially is cited by every join, aggregate, and window-function chapter.

## Chapters

1. [Introduction to SQL](/cortex/languages/sql-foundations-introduction-to-sql) — what SQL is, declarative vs imperative, the **logical execution order** that every SQL question hinges on, dialect map, the sample schema used across the rest of the book.
2. [SELECT and Projection](/cortex/languages/sql-foundations-select-and-projection) — column expressions, aliases, `DISTINCT`, the alias-namespace trap, computed columns.
3. [Filtering](/cortex/languages/sql-foundations-filtering) — `WHERE`, predicates, `BETWEEN`/`IN`/`LIKE`, and the **NULL trap** that breaks intuitive boolean logic.
4. [Ordering and Pagination](/cortex/languages/sql-foundations-ordering-and-pagination) — `ORDER BY`, `NULLS FIRST/LAST`, the `LIMIT/OFFSET` tax, **keyset pagination** as the production answer.
5. [Data Definition](/cortex/languages/sql-foundations-data-definition) — `CREATE`/`ALTER`/`DROP TABLE`, types, constraints (an introduction; deep treatment in module 7).
6. [Data Manipulation](/cortex/languages/sql-foundations-data-manipulation) — `INSERT`/`UPDATE`/`DELETE`, `RETURNING`, `ON CONFLICT`, the basic transactional shape (deep treatment in module 9).
