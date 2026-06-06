package codefolio.server

import codefolio.server.auth.Auth
import codefolio.server.blogPipeline.BlogPipeline
import codefolio.server.codeRunPipeline.CodeRunPipeline
import codefolio.server.config.{
  AppConfig,
  AuthConfig,
  BlogConfig,
  CortexConfig,
  DbConfig,
  MongoConfig,
  RateLimitConfig,
  RedisConfig,
  RunnerConfig
}
import codefolio.server.cortexPipeline.CortexPipeline
import codefolio.server.db.{DataSource, Migrations}
import codefolio.server.helloPipeline.HelloPipeline
import codefolio.server.http.RateLimiter
import org.slf4j.bridge.SLF4JBridgeHandler
import zio.*
import zio.config.typesafe.TypesafeConfigProvider

/**
 * Server entry point.
 *
 * The flow at a glance:
 *
 * Main.run │ ├── bootstrap: install Typesafe config provider so `application.conf` │ (and env-var overrides)
 * feed `AppConfig.live`. │ ├── Migrations.run (Liquibase against Postgres) │ └── HttpApp.serve (binds the
 * port, blocks until shutdown)
 *
 * `provide(...)` wires the **dependency layers** that produce each service from raw config + the JVM. ZIO
 * assembles them into a DAG; if any layer is missing the compiler complains with a "missing service" error
 * before the process ever starts. Order in the `provide` list does not matter.
 *
 * If you add a new service:
 *   1. Define it as `trait MyThing` + `object MyThing { val live: ZLayer[..., MyThing] }`. 2. Append
 *      `MyThing.live` to the `provide` list below. 3. Inject it into whichever layer needs it (typically
 *      `HttpApp.live`).
 */
object Main extends ZIOAppDefault:

  // Liquibase logs through `java.util.logging`; without this bridge those
  // records bypass our Logback config and sbt-revolver flags them as stderr
  // noise. `locally { ... }` runs at class load before any ZIO machinery.
  locally {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }

  // ZIO's `Config` API reads from a `ConfigProvider`. The default uses
  // env vars only; we override to pick up `application.conf` first and let
  // env vars layer over it. `AppConfig.live` then materialises the typed
  // case class via this provider.
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())

  // Project each sub-config out of `AppConfig` so downstream layers depend only on
  // the slice they actually read. Keeps every pipeline's interface honest about
  // what config it touches, and lets tests construct a single sub-config rather
  // than a fully-formed `AppConfig` for every layer they wire.
  private val dbCfg: ZLayer[AppConfig, Nothing, DbConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.db)

  private val redisCfg: ZLayer[AppConfig, Nothing, RedisConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.redis)

  private val mongoCfg: ZLayer[AppConfig, Nothing, MongoConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.mongo)

  private val runnerCfg: ZLayer[AppConfig, Nothing, RunnerConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.runner)

  private val cortexCfg: ZLayer[AppConfig, Nothing, CortexConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.cortex)

  private val blogCfg: ZLayer[AppConfig, Nothing, BlogConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.blog)

  private val authCfg: ZLayer[AppConfig, Nothing, AuthConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.auth)

  private val rateLimitCfg: ZLayer[AppConfig, Nothing, RateLimitConfig] =
    ZLayer.fromFunction((c: AppConfig) => c.runner.rateLimit)

  override def run: ZIO[Any, Throwable, Unit] =
    // Run schema migrations *before* binding the HTTP port — if they fail
    // we'd rather crash on boot than serve traffic against a stale schema.
    val program: ZIO[HttpApp & DataSourceEnv, Throwable, Unit] =
      Migrations.run *> ZIO.serviceWithZIO[HttpApp](_.serve)

    program
      .provide(
        AppConfig.live,       // reads `application.conf` + env vars
        dbCfg,                // AppConfig → DbConfig
        redisCfg,             // AppConfig → RedisConfig
        mongoCfg,             // AppConfig → MongoConfig
        runnerCfg,            // AppConfig → RunnerConfig
        cortexCfg,            // AppConfig → CortexConfig
        blogCfg,              // AppConfig → BlogConfig
        authCfg,              // AppConfig → AuthConfig
        rateLimitCfg,         // AppConfig → RateLimitConfig
        DataSource.live,      // HikariCP pool over Postgres
        HelloPipeline.live,   // /api/hello, /api/recent, /api/health (Postgres + Redis + Mongo)
        CodeRunPipeline.live, // /api/run (Piston / Code Runner)
        CortexPipeline.live,  // /api/cortex/*
        BlogPipeline.live,    // /api/blogs/*
        Auth.live,            // Keycloak JWT verifier (no-op when AUTH_ENABLED=false)
        RateLimiter.live,     // Redis token bucket for /api/run
        HttpApp.live          // tapir + zio-http + static + SPA fallback
      )

  // Type alias kept short to make the `run` signature read cleanly above.
  // (Migrations.run requires a DataSource; we surface that requirement
  // here so the `provide` list resolves cleanly.)
  private type DataSourceEnv = javax.sql.DataSource
