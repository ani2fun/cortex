---
title: Schema and Constraints
summary: Designing a schema that's correct, fast, and evolvable — types, keys, references, and the normalisation forms in plain English.
prereqs:
  - sql-foundations-data-definition
---

# Schema and Constraints

Foundations covered DDL syntax. This module covers schema *design* — choosing types, modelling relationships, deciding when to normalise and when to denormalise, and the cascade of foreign-key behaviours that makes referential integrity a first-class concern.

## Place in the curriculum

- **Prerequisites:** [Data Definition](/cortex/languages/sql/foundations/data-definition) for the syntax. [Joins](/cortex/languages/sql/multiple-tables/joins) and [Anti-joins](/cortex/languages/sql/multiple-tables/anti-joins-and-existence) for the queries that motivate FK constraints.
- **Followed by:** [Indexes and Performance](/cortex/languages/sql/index). Schema choices determine which indexes are useful.

## Chapters

1. [Types](/cortex/languages/sql/schema-and-constraints/types) — choosing the right type for each column. Numeric, text, temporal, JSON, arrays.
2. [Keys and References](/cortex/languages/sql/schema-and-constraints/keys-and-references) — primary keys, foreign keys, `ON DELETE`/`ON UPDATE` cascade behaviour, unique and check constraints in depth.
3. [Normalisation](/cortex/languages/sql/schema-and-constraints/normalisation) — 1NF, 2NF, 3NF, BCNF in plain English. When to normalise; when to denormalise for performance.
