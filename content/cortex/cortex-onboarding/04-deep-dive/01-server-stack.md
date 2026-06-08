---
title: Server Stack — Why Each Library
summary: ZIO 2, tapir, circe, HikariCP, Lettuce, Mongo sync — what each one buys us, why we picked it over the alternative, and what breaks if you swap it out.
---

This chapter is a library-by-library tour of `server/`. The format is the same throughout: a section per decision, each with **what we use**, **why this and not the obvious alternative**, and **what breaks if you change it**. Read it alongside [Repository Tour](/cortex/codefolio-onboarding/start-here-repository-tour) — that one is a map; this one is the legend.

## Why ZIO 2 at all

The server is one big `ZIO[R, E, A]`. Three things this buys us that a plain `Future` server doesn't:

1. **Typed errors.** A handler that returns `IO[HelloFailure, Greeting]` cannot accidentally throw a `RuntimeException` — the compiler tracks the failure channel. The HTTP layer takes the error type and translates it to a status code; the handler never knows about HTTP.
2. **Layers as DI.** A `ZLayer[In, E, Out]` is a typed recipe for building `Out` from `In`. The runtime assembles the DAG; you don't write a constructor graph by hand.
3. **Resource safety.** `ZIO.acquireRelease` ties `HikariDataSource.close()`, `RedisClient.shutdown()`, and the `MongoClient` lifecycle to layer teardown. Ctrl-C `bin/dev` and you watch them close in order, no thread leaks.

The alternatives we considered:

| Stack | Why we passed |
|---|---|
| Akka HTTP + plain `Future` | No effect tracking; handlers throw and the framework hides it. |
| http4s + cats-effect | Excellent, but we'd be juggling two effect systems (cats-effect + zio-test). |
| Spring Boot | Reflection-heavy, slow to start, fights the type system. |

**If you remove ZIO:** the layer wiring at [server/Main.scala:54](server/src/main/scala/codefolio/server/Main.scala) collapses, `acquireRelease` goes away, and every handler grows a `try/catch` for resources it didn't allocate. Don't.

## The `provide(...)` block — order doesn't matter, the DAG does

[server/Main.scala:54-61](server/src/main/scala/codefolio/server/Main.scala) wires six layers:

```scala
program.provide(
  AppConfig.live,
  DataSource.live,
  HelloPipeline.live,
  CodeRunPipeline.live,
  CortexPipeline.live,
  HttpApp.live
)
```

Two things to internalise:

- **Order is irrelevant.** ZIO's macro looks at each layer's input/output types and assembles a topological order. You list the *set* of layers, not the sequence — adding a new store means adding *one* line.
- **Failure types stack up.** `HttpApp.live` is `ZLayer[…, Nothing, HttpApp]`; the other layers are infallible too. The combined effect's error channel is `Config.Error` (from `AppConfig.live` reading HOCON). If you make a layer's error type non-`Nothing`, it bubbles up — useful when you *want* the app to refuse to start without that store.

**If you change it:** if you add a new pipeline that depends on a not-yet-provided service, the compiler reports an unsatisfied requirement. The error message is verbose but accurate — read the type bounds before changing the constructor.

## Why tapir, not raw zio-http

zio-http alone gives you `Routes(Method.POST / "api" / "run" -> handler { … })`. That's fine for two endpoints. We have a dozen, each with a request body, a response body, and a list of failure status codes. Hand-rolling the JSON decoding, the OpenAPI YAML, and the Swagger UI from those would mean three sources of truth that drift.

tapir gives us **one** source: a generated `Endpoint[I, E, O, R]` value. From it we get:

- a request decoder (server side, [http/ApiRoutes.scala:53](server/src/main/scala/codefolio/server/http/ApiRoutes.scala))
- a response encoder (same)
- a typed sttp `Request` builder (client side, [client/api/ApiClient.scala](client/src/main/scala/codefolio/client/api/ApiClient.scala))
- the OpenAPI document at `/docs/` via `tapir-swagger-ui-bundle`

The `errorOut` is bolted on at the wire layer, not on the endpoint definition itself:

```scala
val helloEndpoint: ZServerEndpoint[Any, Any] =
  Endpoints.getHello
    .errorOut(statusCode and jsonBody[ApiError])
    .zServerLogic { _ =>
      helloPipeline.greet.mapError(ApiErrors.toHttp)
    }
```

This split is intentional. The codegen'd endpoint describes the *contract* (path, body shapes); the wire layer adds the *transport policy* (how a domain failure becomes an HTTP status). If you collapsed the two, every error response would have to be enumerated in `api/openapi.yaml`, and the `mapError` function would become a giant pattern match in the codegen'd file.

**If you remove tapir:** you'd hand-write request/response codecs, hand-wire OpenAPI, and re-derive the same types twice (server + client). Drift becomes inevitable. (See [Shared & Codegen](/cortex/codefolio-onboarding/deep-dive-shared-and-codegen) for why this matters.)

## The `mapError` boundary — handlers don't know about HTTP

The error translation lives in exactly one place: [http/ApiErrors.scala](server/src/main/scala/codefolio/server/http/ApiErrors.scala).

```scala
type HandlerFailure = RunFailure | CortexFailure | HelloFailure

def toHttp(failure: HandlerFailure): (StatusCode, ApiError) = failure match
  case RunFailure.BadInput(error, hint)        => StatusCode.BadRequest      -> ApiError(error, None, hint)
  case RunFailure.PayloadTooLarge(error)       => StatusCode.PayloadTooLarge -> ApiError(error, None, None)
  case RunFailure.NotConfigured                => StatusCode.ServiceUnavailable -> …
  case RunFailure.BackendFailure(error, d)     => StatusCode.BadGateway     -> ApiError(error, d, None)
  case CortexFailure.NotFound                  => StatusCode.NotFound        -> ApiError("Not found", None, None)
  // …
```

`HandlerFailure` is a Scala 3 **union type** — `RunFailure | CortexFailure | HelloFailure`. The compiler forces `toHttp` to be exhaustive across all three pipelines. Add a new `CortexFailure` variant and the match becomes non-exhaustive, the build breaks, and you can't ship a handler that returns an unmapped error.

Why not put the status code inside the failure case class itself (like `RunFailure.BadInput(StatusCode.BadRequest, …)`)? Because then `RunFailure` would depend on tapir/sttp's `StatusCode`, leaking HTTP into a pure-domain enum. Pipelines would couple to the wire layer. The current shape lets you unit-test pipelines without importing a single tapir type.

**If you change it:** moving the mapping into the handler means every handler now imports `tapir-core`, and a refactor of HTTP statuses (e.g. moving a 400 to 422) becomes a multi-file change instead of a single match arm.

## Why circe (and not zio-json)

This was a forced choice, not a preference. From the [CLAUDE.md](CLAUDE.md) note: `sbt-openapi-codegen` only emits codecs for **circe** and **jsoniter**. zio-json isn't supported, so the entire JSON path goes through circe.

Settings live in [build.sbt:47-65](build.sbt):

```scala
openapiJsonSerdeLib := "circe"
```

That single setting causes the codegen to emit `EndpointsJsonSerdes.scala` with circe `Encoder`/`Decoder` instances next to the case classes.

**If you change it to "jsoniter":** every place that imports `EndpointsJsonSerdes` keeps compiling, but you'll need to swap the tapir integration to `tapir-json-jsoniter` in `build.sbt` and rewrite any hand-written `Encoder` extensions. There aren't many — the project sticks to generated codecs — so it's doable, just not free.

**If you try to switch to zio-json:** the codegen plugin won't help, and you'll be hand-writing serdes for ~15 case classes, which defeats the point of the codegen.

## The three-store pattern — public trait + internal seams

[server/helloPipeline/HelloPipeline.scala:73](server/src/main/scala/codefolio/server/helloPipeline/HelloPipeline.scala) is the canonical pattern. The public surface is small:

```scala
trait HelloPipeline:
  def greet: IO[HelloFailure, Greeting]
  def recent(limit: Int): IO[HelloFailure, RecentCalls]
  def health: UIO[HealthStatus]
```

The internals are three package-private traits — `Visits` (Postgres), `GreetingCache` (Redis), `EventLog` (Mongo) — composed inside one module. This is documented in [docs/adr/0003](docs/adr/) ("deep modules"): one entry point, several private seams, no public sub-services.

The *alternative* (one module per store, all public, wired in `Main.scala`) is what most ZIO tutorials show. It's worse for two reasons:

1. **Surface area.** You'd have `VisitsRepo`, `RedisCache`, `HelloEventLog` all as `ZLayer` services. Anything in the codebase could reach into any of them. Now every refactor of the demo flow ripples into call sites that shouldn't know about Redis.
2. **Test seams.** When the seams are private to the module, the only public test is "does `greet` return a sensible `Greeting` under each combination of store states". That's the test you actually want — not "does the cache return what you wrote into it", which is the trivial test that the small-modules layout encourages.

**If you flatten it:** the ADR-0003 invariant breaks, the surface area expands, and bugs that only show up across stores (cache returns stale value while Mongo fails to append) become harder to localise.

## Fail-soft for non-critical stores (ADR-0002)

Inside `HelloPipeline.greet`, Redis and Mongo failures are **logged and swallowed**. Only Postgres failure escapes:

```scala
val readFromCache: UIO[Option[Greeting]] =
  cache.get
    .map(_.map(_.markCached))
    .catchAll(e => ZIO.logWarning(s"Redis GET failed: ${e.getMessage}").as(None))
```

This is documented in [docs/adr/0002](docs/adr/). The design rule is: **a non-critical store outage degrades the response, it doesn't break it.** A request to `/api/hello` should return a value as long as Postgres is reachable; the user shouldn't see a 500 because Redis is rebooting.

The flip side, written explicitly in the ADR, is that **you must monitor the warning logs** — silent fallback is silent failure if you don't. We accept that trade in this skeleton because the alternative ("any store outage is a hard failure") is much more user-visible and much more annoying during dev.

**If you change it to `.orDie` or remove the `.catchAll`:** every `bin/dev` shutdown becomes flaky (Redis closes a few hundred ms before Mongo does, and the in-flight request fails the whole `/api/hello`).

## HikariCP + plain JDBC (and not zio-jdbc)

The persistence layer is plain JDBC with a HikariCP pool wrapped in a `ZLayer`. From [db/DataSource.scala:11-27](server/src/main/scala/codefolio/server/db/DataSource.scala):

```scala
val live: ZLayer[AppConfig, Throwable, JDataSource] =
  ZLayer.scoped {
    for
      cfg <- ZIO.service[AppConfig]
      ds <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val hc = HikariConfig()
          hc.setJdbcUrl(cfg.db.url); hc.setUsername(cfg.db.user); hc.setPassword(cfg.db.password)
          hc.setMaximumPoolSize(10)
          hc.setPoolName("codefolio-pool")
          HikariDataSource(hc)
        }
      )(ds => ZIO.attempt(ds.close()).orDie)
    yield ds: JDataSource
  }
```

The repos write `PreparedStatement`s by hand. Why not zio-jdbc?

1. **It's archived.** `dev.zio:zio-jdbc` 0.1.x is no longer maintained.
2. **Version conflict.** zio-jdbc 0.1.x pulls in a `zio-schema` version that collides with zio-http 3's. You can dependency-override it, but you're now maintaining a version pin against an archived library.
3. **Cheap and works.** The skeleton runs ~3 SQL statements total. A 30-line `PreparedStatement` repo is fine.

The pool size is deliberately small (`setMaximumPoolSize(10)`) — this is a personal site, not a high-throughput service. A larger pool would just hold idle connections.

**If you swap to zio-jdbc / Doobie / Quill:** you'll spend more time on dependency conflicts than on app code. For a project this size, the savings aren't there yet.

## Lettuce + `ZIO.fromCompletionStage` (Redis)

Lettuce is the Redis client. It's async-by-default and returns `RedisFuture` (which extends `CompletionStage`). The wrapper pattern:

```scala
ZIO.fromCompletionStage(async.get(CacheKey).toCompletableFuture)
  .map(Option(_))
  .flatMap {
    case Some(json) => ZIO.fromEither(decode[Greeting](json)).map(Some(_)).catchAll(_ => ZIO.none)
    case None       => ZIO.none
  }
```

`ZIO.fromCompletionStage` is the bridge: any `CompletionStage`-returning Java client becomes a `ZIO` effect, with cancellation propagating to the underlying future where supported.

Why Lettuce, not zio-redis? The same shape as zio-jdbc above — zio-redis is younger, has a smaller op surface, and would mean a different connection-management story than the other two clients. Lettuce keeps the codebase coherent: every store is "Java client + ZIO wrapper".

**If you swap to zio-redis:** you change the wrapper pattern only for Redis, the layer wiring grows a new shape, and every contributor has to learn two different async idioms.

## Mongo sync driver + `ZIO.attemptBlocking`

For Mongo we use the sync driver, not the reactive driver. The wrapper:

```scala
override def recent(limit: Int): Task[List[HelloEvent]] = ZIO.attemptBlocking {
  val cursor = coll.find().sort(Sorts.descending("timestampEpochMs")).limit(limit).iterator()
  try
    val buf = List.newBuilder[HelloEvent]
    while cursor.hasNext do buf += /* … */
    buf.result()
  finally cursor.close()
}
```

`ZIO.attemptBlocking` runs the block on the **blocking-io thread pool**, not the main work-stealing pool. That's the right home for sync JDBC-style code: it doesn't block compute threads, and the pool grows on demand.

The sync driver is simpler to read than the reactive one, and a Mongo write here is a single document append — there's no streaming or backpressure to manage. If we ever needed streaming, we'd swap to the reactive driver and replace `attemptBlocking` with `ZIO.fromReactivePublisher`.

**If you change it to the reactive driver without changing the wrapper:** the calls would still work but you'd lose the cancellation and backpressure benefits of the reactive API — that's worse than keeping sync.

## Liquibase YAML + JUL→SLF4J bridge

Migrations live in [server/src/main/resources/db/changelog/](server/src/main/resources/db/changelog/) and run on server boot via [db/Migrations.scala](server/src/main/scala/codefolio/server/db/Migrations.scala). The master changelog is YAML, included SQL changesets are plain `.sql` with Liquibase's `--changeset` markers.

Why YAML over Flyway-style numbered SQL files?

- **Format flexibility.** Liquibase YAML can `include:` SQL, XML, or YAML changesets. We use SQL today, but the door is open.
- **Rollback annotations.** Each changeset can declare its rollback inline (`--rollback DROP TABLE …`).
- **Built-in checksumming.** Liquibase tracks applied changesets in a `databasechangelog` table — modify a shipped changeset and it refuses to apply.

The annoying thing: **Liquibase logs through `java.util.logging`** by default, and sbt-revolver tags everything on stderr as `[ERROR]`. We bridge JUL→SLF4J in `Main.scala` (`SLF4JBridgeHandler.install()`) and `logback.xml` adds a `LevelChangePropagator` to keep the bridge cheap. Without this you spend the first ten minutes thinking startup is broken.

**If you remove the bridge:** every migration run paints the dev console red and you get used to ignoring `[ERROR]`. Then you ignore a real error. Don't.

## The path-traversal guard in `CortexPipeline`

[cortexPipeline/CortexPipeline.scala:142-152](server/src/main/scala/codefolio/server/cortexPipeline/CortexPipeline.scala) is a small but load-bearing piece of code:

```scala
private def safeUnder(rel: String): Either[CortexFailure, File] =
  val candidate = File(rootFile, rel)
  if !candidate.exists() || !candidate.isFile() then Left(CortexFailure.NotFound)
  else
    val real =
      try candidate.toPath.toRealPath()
      catch case _: Throwable => candidate.toPath.toAbsolutePath.normalize
    if real.startsWith(rootPath) then Right(candidate)
    else Left(CortexFailure.NotFound)
```

The web exposes `/api/cortex/{book}/{chapter}` and the slug becomes a relative file path. Without this guard, a slug like `../../../../etc/passwd` would happily resolve to a file outside the cortex root. Two defences:

1. **`toRealPath()`** resolves symlinks and `..` segments to a canonical absolute path.
2. **`startsWith(rootPath)`** rejects anything that escaped the configured root.

The fallback to `.toAbsolutePath.normalize` handles the case where `toRealPath()` throws (e.g. a non-existent file) — without it, a 404 lookup would surface as a 500.

**If you remove either check:** path traversal is one Bash command away from being exploitable. This is the kind of code that *looks* defensive and unnecessary until it isn't.

## Static fallback list — derived, not wildcard

[http/StaticRoutes.scala](server/src/main/scala/codefolio/server/http/StaticRoutes.scala) builds two route sets: a handful of *fixed* routes (`/`, `/index.html`, `/assets/*`, the CV PDF, …) plus an SPA `index.html` fallback **derived** from `AppRoutes.SpaRoutes` — the single source of truth for the SPA route topology (ADR-0009):

```scala
// shared/AppRoutes.scala — the topology, read by the client Router AND the server
val SpaRoutes = List(
  SpaRoute("demo",   hasNestedRoutes = false),
  SpaRoute("cortex", hasNestedRoutes = true),
  SpaRoute("blogs",  hasNestedRoutes = true)
)

// server/http/StaticRoutes.scala — derive one leaf route per SpaRoute, plus a
// `/segment/trailing` route for the nested ones, so /cortex/foo/bar reloads cleanly.
val spaFallback = AppRoutes.SpaRoutes.flatMap { spa => /* leaf (+ nested) */ }
```

You'd think a single `Method.GET / trailing -> staticIndex` catch-all would be cleaner still. It is. It also breaks the API.

zio-http's route matcher doesn't reliably resolve specific tapir routes ahead of a sibling wildcard. Adding `/ trailing` shadows `/api/*` and `/docs/*`, both of which return JSON. You'd get HTML where you expected JSON, with no error, until you noticed the Swagger UI was mysteriously the home page.

The mitigation isn't a hand-maintained list any more — it's the derivation from `AppRoutes.SpaRoutes`. Add a new top-level SPA route there and **both** the client router and the server fallback pick it up; there is no separate server-side mirror to forget. `StaticRoutesSpec` runs real requests through the derived routes to prove every `SpaRoute` is covered.

**If you swap in a `/ trailing` wildcard:** test `/api/health` immediately. You'll get HTML.

## Configuration shape and env var binding

[config/AppConfig.scala](server/src/main/scala/codefolio/server/config/AppConfig.scala) is a flat case-class tree:

```scala
final case class AppConfig(
    port: Int,
    staticDir: String,
    db: DbConfig,
    redis: RedisConfig,
    mongo: MongoConfig,
    runner: RunnerConfig,
    cortex: CortexConfig
)

object AppConfig:
  val config: Config[AppConfig] = deriveConfig[AppConfig].nested("codefolio")
  val live: ZLayer[Any, Config.Error, AppConfig] = ZLayer.fromZIO(ZIO.config(config))
```

Three things going on:

- **`deriveConfig`** uses Magnolia to derive a `Config[AppConfig]` from the case class shape — no annotation noise, no manual key→field mapping.
- **`.nested("codefolio")`** prefixes every key with `codefolio.*`. Reads from a HOCON `codefolio { … }` block; reads env vars as `CODEFOLIO_DB_URL`, `CODEFOLIO_REDIS_URL`, etc.
- **`ZIO.config`** picks up the bootstrap `ConfigProvider` set in `Main.scala` — Typesafe HOCON + env-var override.

**If you switch to zio-config without `.nested("codefolio")`:** every env var loses its prefix and you start clashing with system-level vars (`PORT`, `MONGO_URL`). The prefix is the namespace.

## What's *not* in the server

Worth naming explicitly:

- **No SSR.** The server hands the SPA `index.html` and a string of markdown. React rendering happens in the browser. (See [Markdown Pipeline](/cortex/codefolio-onboarding/how-it-works-markdown-pipeline) for why.)
- **OIDC auth (Keycloak).** `/api/run` and the Cortex code editor are gated by Keycloak / OIDC (ADR-0013): the server validates JWTs (nimbus-jose-jwt), the SPA signs in via `keycloak-js` (PKCE), and a per-JWT token bucket rate-limits `/api/run`. `AUTH_ENABLED=false` short-circuits the whole gate for local dev (every caller is anonymous, no rate limit). See `config/AppConfig.scala`'s `AuthConfig` and the `/api/auth/config` endpoint.
- **No write traffic from production.** Postgres / Redis / Mongo are exercised by the Hello demo at `/demo`. The portfolio reads from disk and bundled JSON. This is by design — keeps prod stateless.

## Where to look first when something on the server breaks

| Symptom | First file |
|---|---|
| 500 with no body | `http/ApiErrors.scala` — failure case unmapped, defaults to 500 |
| Layer wiring error at compile time | `Main.scala` `provide(...)` — missing layer |
| Liquibase logs flagged `[ERROR]` | `Main.scala` SLF4JBridge install missing |
| Static route returns JSON / API returns HTML | `http/StaticRoutes.scala` — wildcard shadowing |
| `/api/cortex/...` returns 404 on a known chapter | `cortexPipeline/CortexPipeline.scala` slug derivation, or stale mtime cache |
| `/api/run` returns 503 with `EXECUTOR_URL` set | `codeRunPipeline/Languages.scala` — alias missing, or go-judge unreachable |

## The one-line summary

The server is a deep ZIO graph of layers, with one error union, one config tree, and one HTTP wire layer that translates domain failures to status codes. Every store is a "Java client + ZIO wrapper" — the *same* wrapper shape three times — and every public service is a small trait over a deep module. Most of the difficulty in extending it is figuring out where in this shape your new code goes, not how to write it.
