---
title: '7. Running on startup'
summary: Cortex applies its migrations automatically when the server boots — before it opens the HTTP port. Read the real Migrations.scala and see how "deploy the code" and "migrate the schema" become one atomic step.
---

# 7. Running on startup

## TL;DR

> Migrations have to run *somewhere*, and Cortex's choice is: **at server startup, before the HTTP port opens.** Its `Main` runs `Migrations.run *> serve` (Chapter 3 of Part 1) — apply migrations, *then* serve — so the app physically cannot accept a request against an un-migrated schema. The real `Migrations.scala` is small: get a connection, hand the master changelog to Liquibase, call `update`. Because `update` is idempotent (Chapter 5), running it on *every* boot is safe — fresh databases get the full history, up-to-date ones get a no-op.

## 1. Motivation

You have changesets and you know `update` applies them. The remaining question is *who runs it and when*. There are roughly three schools: a separate manual step an operator runs before deploying; a CI/CD pipeline step; or *the application itself at startup*. Each has trade-offs, but the application-at-startup approach has a compelling property for a system like Cortex: it makes "deploy the new code" and "migrate the schema the code needs" **the same atomic action**. There's no window where the new code is running but the schema hasn't caught up, and no separate step an operator can forget.

This matters most in a containerized, auto-deployed world (Part 4). When Kubernetes rolls out a new Cortex image, the *container itself* migrates the database as it boots, then serves. You don't orchestrate a separate "migration job" before the "app deploy" and hope they stay in sync — the app guarantees it for you. The ordering (`Migrations.run *> serve`) is the linchpin: by sequencing the migration *before* the server in a single ZIO value, the code makes it impossible to serve a request against a schema that isn't ready.

## 2. Intuition (Analogy)

A restaurant that **sets up the kitchen before unlocking the front door**. The staff arrive, fire up the stoves, stock the line, run the checklist — *and only then* flip the sign to "Open." A customer who walks in is *guaranteed* a working kitchen, because the door simply doesn't unlock until setup is done. There's no awkward window where diners are seated but the kitchen isn't ready.

Cortex boots the same way. `Migrations.run` is "set up the kitchen" (bring the schema to the required version); `serve` is "unlock the front door" (open the HTTP port). Because they happen *in that order, in one sequence*, the door never opens onto an unprepared kitchen. A request can't arrive before the schema is ready, because the thing that accepts requests hasn't started yet.

## 3. Formal Definition

- **Startup migration.** The application runs `liquibase update` against its database as part of its boot sequence, *before* binding its listening port — so no request is served until the schema is current.
- **Ordering as a guarantee.** In ZIO, `Migrations.run *> serve` sequences the two effects (Part 1, Chapter 3): the server effect doesn't begin until the migration effect completes successfully. The *type and the order* encode "migrate before serve."
- **Idempotence makes it safe to repeat.** Because `update` skips already-applied changesets (Chapter 5), running it on every boot is harmless on an up-to-date database and corrective on a behind one.
- **The lock makes it safe to parallelize.** With multiple replicas booting at once, `DATABASECHANGELOGLOCK` (Chapter 5) ensures only one migrates while the others wait — so concurrent startup migrations don't clash.
- **Alternatives.** A separate migration step (CI job, init container, manual command) decouples migration from app start — useful for very large/slow migrations you don't want blocking every boot (Chapter 11), or where you want a human gate. Cortex's migrations are small, so startup is the simplest correct choice.

> Run `update` at startup, before serving, sequenced as `migrate *> serve`. Idempotence makes "every boot" safe; the lock makes "every replica" safe. The result: the app can't serve against an un-migrated schema, and deploy-code and migrate-schema are one step.

## 4. Worked Example — Cortex's Migrations.scala

The whole migration runner is this [`Migrations.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/Migrations.scala):

```scala
object Migrations:
  private val ChangelogPath = "db/changelog/db.changelog-master.yaml"   // the master from Chapter 3

  val run: ZIO[JDataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[JDataSource] { ds =>                 // needs the DataSource (Part 1, Ch 6-9)
      ZIO.attemptBlocking {                                  // JDBC is blocking (Part 1, Ch 11)
        val conn = ds.getConnection
        try
          val database  = DatabaseFactory.getInstance
                             .findCorrectDatabaseImplementation(JdbcConnection(conn))
          val liquibase = Liquibase(ChangelogPath, ClassLoaderResourceAccessor(), database)
          liquibase.update("")                               // <-- the idempotent apply loop (Chapter 5)
        finally conn.close()
      } *> ZIO.logInfo("Liquibase migrations applied")
    }
```

Read it as a synthesis of everything so far. It's a `ZIO` value (Part 1) that *requires a `JDataSource`* in its `R` channel — so it's wired by the same `provide` as everything else (Part 1, Chapter 8). The work is `attemptBlocking` because JDBC blocks (Part 1, Chapter 11). Inside, it borrows a connection, points Liquibase at the **master changelog on the classpath** (`db/changelog/db.changelog-master.yaml`, Chapter 3), and calls `liquibase.update("")` — the idempotent loop that reads `DATABASECHANGELOG` and applies any new changesets (Chapter 5). Then `Main` runs it *before* `serve`:

```scala
// From Main (Part 1, Chapter 3): migrate, THEN serve — one ordered value.
val program = Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)
```

That `*>` is the whole guarantee. On a fresh database, `Migrations.run` creates all of Cortex's tables, then the server starts. On an already-migrated database (a routine restart), `update` finds nothing to do and the server starts immediately. Either way, the HTTP port opens *only after* the schema is at the version the code expects. Deploy a new image with a new changeset, and the booting container migrates and then serves — atomically, no separate step.

## 5. Build It

Run this. It models the boot sequence and proves the property: the "server" never accepts a request before migrations finish.

```python run
served_requests = []

def run_migrations(db_applied, changelog):
    print("BOOT: running migrations (door still locked)...")
    for cs in changelog:
        if cs not in db_applied:
            print(f"   applied {cs}")
            db_applied.append(cs)
    print("BOOT: schema ready.\n")

def serve():
    print("DOOR UNLOCKED: now accepting requests")
    served_requests.append("GET /api/run")

def boot(db_applied, changelog):
    run_migrations(db_applied, changelog)     # migrate FIRST
    serve()                                    # *> THEN serve

# Fresh database: full history applied before serving.
fresh = []
boot(fresh, ["1-visits", "2-allowlist", "3-submissions"])
print("requests served only AFTER schema ready:", served_requests, "\n")

# Restart on an up-to-date database: migrations are a no-op, server starts immediately.
served_requests.clear()
boot(["1-visits", "2-allowlist", "3-submissions"], ["1-visits", "2-allowlist", "3-submissions"])
```

**Now break it.** Swap the order in `boot` to `serve()` *then* `run_migrations(...)`. Now the door unlocks *before* the schema is ready — a request could hit `submissions` before the table exists, producing the exact "relation does not exist" 500s that the correct ordering prevents. The single line `Migrations.run *> serve` (migrate first) is what makes that error structurally impossible. Order, once again, *is* the guarantee.

## 6. Trade-offs & Complexity

| Migrate at startup (Cortex) | Separate migration step |
|---|---|
| Deploy-code and migrate-schema are atomic | Two steps to coordinate |
| No "code ahead of schema" window | Possible drift window |
| Nothing for an operator to forget | A step that can be skipped |
| Blocks boot if a migration is slow | Slow migrations don't block every boot |
| Lock handles concurrent replicas | Manual coordination |

The main trade-off is **slow migrations**: a changeset that takes ten minutes (a big backfill or index build) would make *every* boot take ten minutes, and a Kubernetes readiness probe might time out and kill the pod mid-migration. For *small, fast* migrations (Cortex's), startup is simplest and safest. For *large* ones, you'd split them out (an init container or a one-off job) so the app boot stays fast — which is exactly the expand/contract thinking of Chapter 11. Know your migrations' cost and choose accordingly.

## 7. Edge Cases & Failure Modes

- **Slow migration vs. health probes.** A long startup migration can exceed a Kubernetes `startupProbe`/`readinessProbe` window, and the orchestrator kills the booting pod. Either bound migrations to be fast, or run big ones out-of-band (Part 4 + Chapter 11).
- **Migration fails at boot.** Cortex *crashes* rather than serving a broken schema — that's the next chapter (fail-fast), and it's the right behavior.
- **Many replicas migrating at once.** The lock table serializes them; without it you'd get races. Don't disable locking to "speed up" parallel boots.
- **Classpath changelog not found.** `Migrations.scala` loads the changelog from the *classpath* (`ClassLoaderResourceAccessor`). If the changelog files aren't packaged into the jar/image, `update` finds nothing — make sure they ship in the build (Part 4's Dockerfile copies them).

## 8. Practice

> **Exercise 1 — Why this order?** Explain what `Migrations.run *> serve` guarantees, and the concrete error a user would hit if the order were reversed.

<details>
<summary><strong>Answer</strong></summary>

`*>` sequences two effects: the right one (`serve`) does not begin until the left one (`Migrations.run`) completes successfully (§3). So the guarantee is **the HTTP port opens only after the schema is at the version the code expects** — the app physically cannot accept a request against an un-migrated schema, because the thing that accepts requests (`serve`) hasn't started yet. It's "set up the kitchen, *then* unlock the door."

Reverse it to `serve *> Migrations.run` and the door unlocks first. A request can now arrive *before* the migration creates a table — say a `POST /api/submissions` lands while the `submissions` table doesn't exist yet — and the user hits a **`relation "submissions" does not exist`** error (a 500). The correct ordering makes that error *structurally impossible*: order is the guarantee.

</details>

> **Exercise 2 — Restart safety.** A Cortex pod restarts 20 times today due to a node issue. How many times do the migrations actually *apply*, and why is that safe? (Tie it to Chapter 5.)

<details>
<summary><strong>Answer</strong></summary>

The changesets apply **exactly once** — on the very first boot against that database. The other 19 restarts apply *nothing*. The reason is **idempotence** (Chapter 5): `liquibase.update("")` reads `DATABASECHANGELOG`, sees every changeset is already recorded as applied, and does nothing — a no-op. `update` only ever runs changesets it hasn't run before.

That's precisely why running `update` on *every* boot is safe rather than reckless: a fresh database gets the full history, an up-to-date one gets a harmless no-op, so the server starts immediately. (And if several replicas restart at once, `DATABASECHANGELOGLOCK` serializes them so only one migrates while the others wait — Chapter 5.) "Run it every time" is correct *because* `update` is idempotent.

</details>

> **Exercise 3 — When NOT startup.** Describe a migration for which running-at-startup would be a bad idea, and what you'd do instead.

<details>
<summary><strong>Answer</strong></summary>

A **slow** migration — a large data backfill or building an index on a huge table, anything taking minutes. Running it at startup makes *every* boot take that long, and worse, a Kubernetes `startupProbe`/`readinessProbe` can time out and kill the pod *mid-migration* (§7). Startup migration is the right default only for *small, fast* changes like Cortex's.

Instead, run the slow change **out-of-band** — as a separate one-off migration step (an init container, a dedicated migration Job, or a manual `liquibase update`) that isn't on the app's boot path, so booting pods stay fast. This is exactly the expand/contract thinking of Chapter 11: a slow backfill belongs in its own migrate step, not blocking `serve`.

</details>

```quiz
{
  "prompt": "Why does Cortex run `Migrations.run *> serve` (migrate, then serve) at startup?",
  "input": "Choose one:",
  "options": [
    "So the HTTP port opens only AFTER the schema is at the version the code expects — making it impossible to serve a request against an un-migrated schema",
    "To make the server start faster",
    "Because Liquibase requires the server to be running first",
    "To run migrations after every request"
  ],
  "answer": "So the HTTP port opens only AFTER the schema is at the version the code expects — making it impossible to serve a request against an un-migrated schema"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Integrating with your application (Java API)](https://docs.liquibase.com/tools-integrations/java-api/home.html)** — running `liquibase.update` programmatically, as `Migrations.scala` does.
- **[12-Factor App — Admin processes](https://12factor.net/admin-processes)** — the broader principle of running schema migrations as part of the release, not a manual afterthought.
- **[Cortex `Migrations.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/Migrations.scala)** & **[`Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala)** — the real startup migration and the `migrate *> serve` ordering.

---

**Next:** what happens when a startup migration *fails*? Cortex crashes — on purpose. Meet the fail-fast philosophy and why a dead pod beats a half-migrated one. → [8. The fail-fast philosophy](/cortex/production-engineering/liquibase/running-migrations/fail-fast)
