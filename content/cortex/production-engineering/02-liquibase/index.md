# Part 2 — Liquibase: Evolving a Database Without Fear

> **Your code lives in git. Your database doesn't.** When you change a function you can diff it, review it, revert it. But a production database is a single, stateful, irreplaceable object full of real users' data — and changing its shape is the most dangerous routine thing an engineer does. Liquibase brings the discipline of version control to the one place that needs it most.

## The problem in one sentence

The schema on your laptop, the schema in the test environment, and the schema in production have *drifted apart*, nobody is quite sure how, and the next deploy is going to find out the hard way.

Liquibase fixes this by making every schema change an explicit, ordered, recorded **changeset** — applied exactly once, in the same order, everywhere, with a rollback plan attached. Cortex runs its migrations on server startup, *before* the HTTP port opens, so a bad schema can never serve a single request.

## Chapters

**The Problem**
1. [Why you can't just ALTER TABLE in production](/cortex/production-engineering/liquibase/the-problem/why-not-alter-table)
2. [Migrations as version control for your schema](/cortex/production-engineering/liquibase/the-problem/migrations-as-version-control)

**Liquibase Basics**
3. [Changelogs and changesets](/cortex/production-engineering/liquibase/basics/changelogs-and-changesets)
4. [SQL-formatted changesets](/cortex/production-engineering/liquibase/basics/sql-formatted-changesets)
5. [How Liquibase knows what's applied: DATABASECHANGELOG](/cortex/production-engineering/liquibase/basics/how-liquibase-tracks)
6. [Rollbacks](/cortex/production-engineering/liquibase/basics/rollbacks)

**Running Migrations**
7. [Running on startup](/cortex/production-engineering/liquibase/running-migrations/running-on-startup)
8. [The fail-fast philosophy](/cortex/production-engineering/liquibase/running-migrations/fail-fast)

**Advanced Liquibase**
9. [Contexts and labels](/cortex/production-engineering/liquibase/advanced/contexts-and-labels)
10. [Preconditions](/cortex/production-engineering/liquibase/advanced/preconditions)
11. [Refactoring data safely: expand/contract](/cortex/production-engineering/liquibase/advanced/expand-contract)
12. [Checksums, when they bite, and how to recover](/cortex/production-engineering/liquibase/advanced/checksums)

**Cortex in Practice**
13. [Add a real migration to Cortex](/cortex/production-engineering/liquibase/cortex-in-practice/add-a-migration)

*Versions in play: Liquibase 4.30.0, PostgreSQL 17, HikariCP.*

**Begin:** [1. Why you can't just ALTER TABLE in production →](/cortex/production-engineering/liquibase/the-problem/why-not-alter-table)
