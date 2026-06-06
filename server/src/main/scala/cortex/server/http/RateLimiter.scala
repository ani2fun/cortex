package cortex.server.http

import cortex.server.config.{AuthConfig, RateLimitBucket, RateLimitConfig, RedisConfig}
import cortex.shared.api.Endpoints.Quota
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.{ClientOptions, RedisClient, TimeoutOptions}
import zio.*

import java.util.concurrent.TimeUnit

/**
 * Fixed-window rate limiter for `/api/run`, backed by Redis.
 *
 * Two buckets:
 *   - **anonymous** — keyed on the client IP, short window (default 10 / 60s);
 *   - **authenticated** — keyed on the JWT `sub`, hourly window (default 100 / 3600s).
 *
 * Each is a plain fixed-window counter: one `INCR` per call against a key bucketed by the window floor, with
 * `EXPIRE` set to the window length so stale windows self-clean. (The plan's "burst" idea doesn't map onto a
 * fixed-window counter — the window cap *is* the ceiling, so anonymous is a flat 10/min.)
 *
 * **Fail-open:** a Redis error is logged and treated as "window empty" — per ADR-0002, rate limiting is a
 * non-critical store concern and a cache outage must never block code execution.
 *
 * When auth is disabled (`cortex.auth.enabled = false`, the `bin/dev` default) the limiter is a no-op:
 * every `consume*` call succeeds and no Redis connection is opened.
 */
trait RateLimiter:
  /** Consume one anonymous slot for `ipKey`. Fails [[RateLimitFailure.Throttled]] when the window is full. */
  def consumeAnonymous(ipKey: String): IO[RateLimitFailure, Quota]

  /** Consume one authenticated slot for `sub`. Fails [[RateLimitFailure.Throttled]] when full. */
  def consumeAuthenticated(sub: String): IO[RateLimitFailure, Quota]

  /** Read `sub`'s current authenticated-window usage **without** consuming a slot (for `/api/me`). */
  def peekAuthenticated(sub: String): UIO[Quota]

object RateLimiter:

  /**
   * Resource-safe layer. Opens its own Lettuce connection (same "Java client + 2s timeout" pattern as
   * `HelloPipeline`'s cache) and releases it on shutdown. When auth is disabled, returns the no-op limiter
   * and opens nothing.
   */
  val live: ZLayer[AuthConfig & RateLimitConfig & RedisConfig, Throwable, RateLimiter] =
    ZLayer.scoped {
      for
        authCfg <- ZIO.service[AuthConfig]
        rlCfg   <- ZIO.service[RateLimitConfig]
        limiter <-
          if !authCfg.enabled then ZIO.succeed(noop(rlCfg))
          else
            ZIO.serviceWithZIO[RedisConfig] { redisCfg =>
              openConnection(redisCfg).map(conn => RedisRateLimiter(conn, rlCfg))
            }
      yield limiter
    }

  /** No-op limiter for auth-disabled (dev) mode — every call succeeds, no Redis touched. */
  private def noop(cfg: RateLimitConfig): RateLimiter =
    new RateLimiter:
      private def free(b: RateLimitBucket): Quota = Quota(0, b.limit, b.windowSeconds, 0L)
      override def consumeAnonymous(ipKey: String): IO[RateLimitFailure, Quota] =
        ZIO.succeed(free(cfg.anonymous))
      override def consumeAuthenticated(sub: String): IO[RateLimitFailure, Quota] =
        ZIO.succeed(free(cfg.authenticated))
      override def peekAuthenticated(sub: String): UIO[Quota] =
        ZIO.succeed(free(cfg.authenticated))

  private def openConnection(
      cfg: RedisConfig
  ): ZIO[Scope, Throwable, StatefulRedisConnection[String, String]] =
    for
      client <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val c = RedisClient.create(cfg.url)
          c.setOptions(
            ClientOptions.builder
              .timeoutOptions(TimeoutOptions.enabled(java.time.Duration.ofSeconds(2)))
              .build
          )
          c
        }
      )(c => ZIO.attempt(c.shutdown()).orDie)
      conn <- ZIO.acquireRelease(ZIO.attemptBlocking(client.connect()))(c =>
        ZIO.attempt(c.close()).orDie
      )
    yield conn

final private class RedisRateLimiter(
    connection: StatefulRedisConnection[String, String],
    cfg: RateLimitConfig
) extends RateLimiter:

  private val async = connection.async()

  override def consumeAnonymous(ipKey: String): IO[RateLimitFailure, Quota] =
    consume(cfg.anonymous, "anon", ipKey)

  override def consumeAuthenticated(sub: String): IO[RateLimitFailure, Quota] =
    consume(cfg.authenticated, "auth", sub)

  override def peekAuthenticated(sub: String): UIO[Quota] =
    for
      nowSec <- Clock.currentTime(TimeUnit.SECONDS)
      window = windowOf(cfg.authenticated, nowSec)
      used <- readCount(redisKey("auth", sub, window.floor))
    yield Quota(used.toInt, cfg.authenticated.limit, cfg.authenticated.windowSeconds, window.resetEpochMs)

  // ── internals ───────────────────────────────────────────────────────────

  private case class Window(floor: Long, resetEpochMs: Long, retryAfterSec: Int)

  private def windowOf(bucket: RateLimitBucket, nowSec: Long): Window =
    val floor    = (nowSec / bucket.windowSeconds) * bucket.windowSeconds
    val resetSec = floor + bucket.windowSeconds
    Window(floor, resetSec * 1000L, math.max(1, (resetSec - nowSec).toInt))

  private def redisKey(scope: String, key: String, floor: Long): String =
    s"rl:$scope:$key:$floor"

  private def consume(
      bucket: RateLimitBucket,
      scope: String,
      key: String
  ): IO[RateLimitFailure, Quota] =
    for
      nowSec <- Clock.currentTime(TimeUnit.SECONDS)
      window = windowOf(bucket, nowSec)
      count <- increment(redisKey(scope, key, window.floor), bucket.windowSeconds)
      quota = Quota(count.toInt, bucket.limit, bucket.windowSeconds, window.resetEpochMs)
      result <-
        if count > bucket.limit then ZIO.fail(RateLimitFailure.Throttled(window.retryAfterSec))
        else ZIO.succeed(quota)
    yield result

  /**
   * `INCR` the window key and refresh its `EXPIRE`.
   *
   * The `EXPIRE` runs on **every** call, not just the first hit. If a transient Redis error ever dropped the
   * `EXPIRE`, a key with no TTL would keep counting forever and throttle that caller permanently; refreshing
   * the TTL each call is self-healing. Re-extending a window-floored key's TTL is harmless — the next window
   * uses a different key, so an over-long TTL only leaves a dead key lingering briefly.
   *
   * Fail-open: a Redis error is logged and counts as 0 (ADR-0002 — rate limiting must not block runs).
   */
  private def increment(key: String, ttlSec: Int): UIO[Long] =
    (for
      n <- ZIO.fromCompletionStage(async.incr(key).toCompletableFuture)
      _ <- ZIO.fromCompletionStage(async.expire(key, ttlSec.toLong).toCompletableFuture)
    yield n.longValue)
      .catchAll(e => ZIO.logWarning(s"RateLimiter INCR/EXPIRE failed for $key: ${e.getMessage}").as(0L))

  /** `GET` the window key as a `Long`. Fail-open + absent → 0. */
  private def readCount(key: String): UIO[Long] =
    ZIO
      .fromCompletionStage(async.get(key).toCompletableFuture)
      .map(v => Option(v).flatMap(_.toLongOption).getOrElse(0L))
      .catchAll(e => ZIO.logWarning(s"RateLimiter GET failed for $key: ${e.getMessage}").as(0L))
