---
title: '9. Resourceful layers: acquireRelease & scoped'
summary: Some services must be opened AND closed — a connection pool, a Redis client, a file. `acquireRelease` and `scoped` guarantee the close runs no matter what. Read Cortex''s DataSource pool.
---

# 9. Resourceful layers: acquireRelease & scoped

## TL;DR

> A *resource* is anything you must **release** after acquiring: a database connection pool, a Redis client, an open file. Forget to release it and you leak — until the app falls over. ZIO's **`ZIO.acquireRelease(open)(close)`** pairs an acquire with a release and guarantees the release runs **even if the program fails, is interrupted, or the JVM is shutting down**. Wrapped in **`ZLayer.scoped`**, it becomes a *resourceful service* whose lifetime is tied to the app's. Cortex's `DataSource.live` builds its HikariCP pool this way, so the pool is always closed cleanly.

## 1. Motivation

Building a service (Chapter 7) is half the story for anything that holds an operating-system or network resource. A connection pool opens sockets; a Mongo client opens connections; a file handle holds a descriptor. Every one of these must be *closed*, and closed *reliably* — not just on the happy path, but when an exception fires mid-request, when the app is shut down, when a fiber is cancelled. The classic bug is a `try { use } finally { close }` that's correct in one place and forgotten in another, or that doesn't run on interruption — and three days later production is out of database connections.

ZIO makes resource safety structural. You declare, in one place, *how to open* and *how to close* a resource, and ZIO promises the close will run exactly once when the resource's *scope* ends — no matter how that scope ends. You can't forget the close, because it's welded to the acquire. For a long-lived server like Cortex, that guarantee is the difference between a pool that's cleanly shut down on deploy and one that leaks connections until the database refuses new ones.

## 2. Intuition (Analogy)

Renting climbing gear. The good shop doesn't just hand you a harness; it staples the *return slip* to the rental the moment you check it out. However your climb ends — you finish, you bail, you're rescued by helicopter — the system *knows* the gear comes back, because the return was bound to the rental at acquire time. You can't accidentally walk off with it; the bookkeeping is structural.

`acquireRelease(open)(close)` staples the `close` to the `open`. ZIO tracks the resource's **scope**; when the scope ends — success, failure, or interruption — the stapled `close` runs. You never write a fragile "remember to return it" at every exit; the return is part of the rental.

## 3. Formal Definition

- **`ZIO.acquireRelease(acquire)(release)`** — pairs an `acquire` effect with a `release` effect (run on `A`). It produces a `ZIO[Scope & R, E, A]`: the resource is valid for the lifetime of the enclosing **`Scope`**, and `release` is guaranteed to run when that scope closes — including on failure and **interruption** (fiber cancellation). `release` typically can't be allowed to fail, so it's often `.orDie`.
- **`Scope`** — ZIO's representation of a resource lifetime. Effects that open resources require a `Scope` in their `R`; closing the scope runs all its finalizers (in reverse order).
- **`ZLayer.scoped { ... }`** — turns a scoped, resource-acquiring effect into a `ZLayer`. The resource lives as long as the layer is *provided* — for an app-wide layer in `Main`, that's the whole application run. When the app ends, the scope closes and the resource is released.
- **`ZIO.acquireReleaseWith(acquire)(release)(use)`** — the "use it then release" variant for a *local* resource (open, use, close, all in one expression), when you don't need a long-lived service.

> Bind the close to the open, and let the *scope* decide when "the open ends." ZIO runs the close on every exit path — normal, error, interrupt, shutdown. Leaks become structurally impossible.

## 4. Worked Example — Cortex's DataSource pool

Here is Cortex's real [`DataSource.live`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/DataSource.scala) — a HikariCP connection pool as a resourceful layer:

```scala
object DataSource:
  val live: ZLayer[DbConfig, Throwable, JDataSource] =
    ZLayer.scoped {                                  // the pool lives as long as the app
      for
        cfg <- ZIO.service[DbConfig]
        ds <- ZIO.acquireRelease(                    // ACQUIRE: open the pool
          ZIO.attemptBlocking {
            val hc = HikariConfig()
            hc.setJdbcUrl(cfg.url)
            hc.setUsername(cfg.user)
            hc.setPassword(cfg.password)
            hc.setMaximumPoolSize(10)
            hc.setPoolName("cortex-pool")
            HikariDataSource(hc)
          }
        )(ds => ZIO.attempt(ds.close()).orDie)       // RELEASE: close it — guaranteed
      yield ds: JDataSource
    }
```

Read the shape: `ZLayer.scoped { ... acquireRelease(open)(close) ... }`. The **acquire** builds the HikariCP pool (a blocking operation, so `attemptBlocking` — Chapter 11). The **release** — `ds.close()` — is *stapled to it*. Because this is an app-wide layer provided in `Main`, the pool's scope is the *entire application*: it's opened once at startup and `close()`d once at shutdown, automatically, even if the app crashes or receives a termination signal (which is exactly what happens when Kubernetes stops the pod — Part 4). Notice the release is `.orDie`: a failure *to close* is treated as a defect (Chapter 4), because there's no sensible "recover from failing to close the pool" — you want to know loudly.

The single pool, built once via this layer and memoized by `provide` (Chapter 8), is shared by every pipeline. One open, one close, shared by all — resource safety and resource sharing in one small layer.

## 5. Build It

Run this. It models `acquireRelease` and shows the guarantee that matters: the release runs *even when the use fails*.

```scala run
@main def run(): Unit =
  val opened = scala.collection.mutable.ListBuffer.empty[String]
  val closed = scala.collection.mutable.ListBuffer.empty[String]

  // Model of acquireRelease: the release is BOUND to the acquire and runs no
  // matter how `use` exits — success or exception.
  def acquireRelease[A](name: String)(use: String => A): A =
    opened += name                                  // ACQUIRE
    println(s"   acquire: opened $name")
    try use(name)                                   // USE
    finally                                         // RELEASE — runs on success, on exception, on anything
      closed += name
      println(s"   release: closed $name")

  def run(useFails: Boolean): Unit =
    acquireRelease("cortex-pool") { pool =>
      println(s"   use: querying via $pool")
      if useFails then throw RuntimeException("query blew up mid-use!")
    }

  println("== happy path ==")
  run(useFails = false)

  println("\n== use FAILS — does release still run? ==")
  try run(useFails = true)
  catch case e: RuntimeException => println(s"   (caught: ${e.getMessage})")

  println("\nopened: " + opened.toList)
  println("closed: " + closed.toList)
  println("every acquired resource was released: " + (opened.length == closed.length))
```

**Now break it.** Try to make a resource leak: add a second `acquireRelease` *inside* the first's `use` and raise before reaching it — you'll find every resource that was *opened* still gets *closed*, in reverse order, because each release is bound to its own acquire. That's the structural guarantee. The only way to actually leak in ZIO is to bypass `acquireRelease` and open something with a raw `attempt` — which is exactly the pattern Cortex avoids by funneling every resource through a scoped layer.

## 6. Trade-offs & Complexity

| `acquireRelease` / `scoped` | Manual try/finally |
|---|---|
| Release guaranteed on *all* exits, incl. interruption | Easy to miss a path; interruption often unhandled |
| Release bound to acquire in one place | Open and close drift apart |
| Composes — scopes nest, finalizers run in order | Nested try/finally gets unreadable |
| Lifetime tied to layer/scope explicitly | Lifetime implicit and error-prone |

The cost is learning the `Scope` concept and the `acquireRelease` shape — slightly more than a `try/finally`. The payoff is that resource leaks, one of the nastiest classes of production bug (they work fine until load reveals them), become structurally hard to write. For anything holding a connection, file, or socket, scoped layers are non-negotiable in a long-running service.

## 7. Edge Cases & Failure Modes

- **Opening a resource with plain `attempt`.** `ZIO.attemptBlocking(new HikariDataSource(...))` *without* `acquireRelease` opens a pool nobody will close — a guaranteed leak. Always pair acquire with release.
- **Release that can fail.** If `release` throws and you don't `.orDie` or handle it, you can mask the real error or leave the scope in limbo. Treat "failed to close" as a defect or log-and-continue, deliberately.
- **Resource scope too wide or too narrow.** App-wide infra (a pool) belongs in a `Main`-level scoped layer. A *per-request* resource should be scoped to the request (`acquireReleaseWith`), or you hold it far too long.
- **Interruption not considered.** A `try/finally` that doesn't account for fiber interruption can skip cleanup. `acquireRelease` handles interruption for you — that's a key reason to use it over hand-rolled cleanup.

## 8. Practice

> **Exercise 1 — Pair them up.** For each resource, write the acquire and the release: (a) a file; (b) a Redis connection; (c) a temp directory. Which release should be `.orDie` and why?

<details>
<summary><strong>Answer</strong></summary>

Each is the same shape: `ZIO.acquireRelease(open)(close)`, with the close *stapled* to the open (§3) so it runs on every exit path.

- **(a) A file:** acquire = open the handle (`ZIO.attemptBlocking(Files.newInputStream(path))`); release = `is => ZIO.attempt(is.close()).orDie`.
- **(b) A Redis connection:** acquire = connect (`ZIO.attempt(RedisClient.connect(cfg))`); release = `conn => ZIO.attempt(conn.close()).orDie`.
- **(c) A temp directory:** acquire = create it (`ZIO.attempt(Files.createTempDirectory("work"))`); release = `dir => ZIO.attempt(deleteRecursively(dir)).orDie`.

**Which release should be `.orDie`?** Essentially *all* of them — and for the same reason. A `release` is meant to *clean up*; there is no sensible "recover from failing to close the file / connection / directory." If the close itself fails, that's not a domain failure you handle, it's a **defect** (Chapter 4): something is wrong with the machinery, and you want to know *loudly* rather than swallow it. `.orDie` converts a failed release into a defect, which is exactly Cortex's choice for `ds.close()` in `DataSource.live`. (The one nuance from §7: if a failed cleanup is genuinely tolerable — e.g. best-effort temp-dir deletion — you might *deliberately* log-and-continue instead; but the default for "must close" resources is `.orDie`.)

</details>

> **Exercise 2 — Scope the lifetime.** Should a HTTP server's thread pool be scoped to the *app* or to a *request*? What about a transaction's connection? Justify each.

<details>
<summary><strong>Answer</strong></summary>

The rule (§7): scope a resource to the *narrowest lifetime over which it's actually needed* — app-wide infrastructure to the app, per-request resources to the request.

- **HTTP server's thread pool → scoped to the *app*.** It's long-lived shared infrastructure: one pool serves *every* request for the whole run of the server. Opening and closing it per request would be absurdly expensive (you'd spin up and tear down threads on every call) and pointless. So it belongs in a `Main`-level `ZLayer.scoped` — opened once at startup, closed once at shutdown, just like Cortex's `DataSource` pool.
- **A transaction's connection → scoped to the *request* (or the transaction).** A transaction is inherently short-lived: borrow a connection, run the statements, commit/rollback, *return it immediately*. Holding it for the app's lifetime would pin a connection forever and starve the pool. This is the `acquireReleaseWith(acquire)(release)(use)` "open, use, close, all in one expression" case (§3) — the connection's scope is exactly the transaction.

The justification is the same both times: a scope too *wide* holds a resource longer than needed (a per-request connection held app-wide leaks capacity), and a scope too *narrow* re-opens shared infra needlessly (a per-request thread pool is wasteful). Match the scope to the real lifetime.

</details>

> **Exercise 3 — Prove no leak.** In the Build It model, explain precisely why the "use fails" path still closes the pool, and connect it to why Cortex's pool is closed even when Kubernetes kills the pod.

<details>
<summary><strong>Answer</strong></summary>

In the Build It model, the release lives in a `finally` clause bound to the acquire (`acquireRelease` wraps `use` in `try ... finally { close }`). When `useFails = true` throws mid-use, the `RuntimeException` propagates out of `use` — but the `finally` runs **on the way out, regardless of how the block exits**, so `closed += name` runs before the exception leaves. The close isn't on the happy path; it's *bound to the acquire*, so the exception can't skip it. That's why `opened` and `closed` end up the same length even on the failing path — the release is structural, not something the success code remembers to do.

The connection to Cortex: `ZIO.acquireRelease(open)(close)` is that same staple, but stronger. It guarantees the `close` runs when the resource's **scope** ends, on *every* exit path — success, failure, **and interruption** (§3). When Kubernetes stops the pod it sends a termination signal; ZIO treats that as interrupting the running fibers, which **closes the app-wide scope** that the `DataSource` layer lives in, which runs the stapled `ds.close()`. Because the pool's scope is the whole application (it's a `Main`-level `ZLayer.scoped`), "the app is ending" *is* "close the pool" — automatically, even though no application code caught the shutdown. A plain `try/finally` can miss exactly this case (it often doesn't account for fiber interruption, §7); `acquireRelease` handles it, which is why the pool is shut down cleanly on deploy instead of leaking connections until the database refuses new ones.

</details>

```quiz
{
  "prompt": "What does `ZIO.acquireRelease(open)(close)` guarantee about the `close` effect?",
  "input": "Choose one:",
  "options": [
    "It runs when the resource's scope ends — on success, failure, OR interruption — so the resource can't leak",
    "It runs only if the program succeeds",
    "It runs only if you remember to call it",
    "It runs before the resource is opened"
  ],
  "answer": "It runs when the resource's scope ends — on success, failure, OR interruption — so the resource can't leak"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[ZIO docs — Scope & resource management](https://zio.dev/reference/resource/)** — `acquireRelease`, `Scope`, `scoped`, and finalizer ordering.
- **[ZIO docs — `ZLayer.scoped`](https://zio.dev/reference/contextual/zlayer/#scoped-layers)** — turning a resource into a service.
- **[Cortex `DataSource.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/db/DataSource.scala)** — the real HikariCP scoped layer dissected above.

---

**Next:** services and dependencies, done. Now the other reason ZIO exists — doing many things at once, safely. Meet **fibers**, ZIO's lightweight concurrency. → [10. Fibers: green threads you can fork](/cortex/production-engineering/zio/concurrency/fibers)
