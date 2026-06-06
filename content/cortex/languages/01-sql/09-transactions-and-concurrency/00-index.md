---
title: Transactions and Concurrency
summary: ACID, isolation levels, MVCC, and the locks that determine what happens when multiple transactions touch the same data.
prereqs:
  - sql-foundations-data-manipulation
---

# Transactions and Concurrency

A transaction is a group of statements that succeed or fail together. Concurrency is what happens when more than one transaction runs at the same time. The intersection — what guarantees you get, what anomalies you don't — is one of the most consequential and least-understood areas of SQL.

## Place in the curriculum

- **Prerequisites:** [Data Manipulation](/cortex/languages/sql-foundations-data-manipulation) for `BEGIN`/`COMMIT`/`ROLLBACK`. Some familiarity with [Indexes](/cortex/languages/sql-index) helps for the locking discussion.

## Chapters

1. [ACID and Transactions](/cortex/languages/sql-transactions-and-concurrency-acid-and-transactions) — Atomicity, Consistency, Isolation, Durability. What each means in practice.
2. [Isolation Levels](/cortex/languages/sql-transactions-and-concurrency-isolation-levels) — the four standard levels, the anomalies they prevent, snapshot vs serializable.
3. [MVCC and Locking](/cortex/languages/sql-transactions-and-concurrency-mvcc-and-locking) — Postgres's MVCC mental model, row vs table locks, deadlocks, advisory locks.
