package codefolio.server.helloPipeline

import codefolio.shared.api.Endpoints.{Greeting, HelloEvent}
import zio.*
import zio.test.*

object HelloPipelineSpec extends ZIOSpecDefault:

  private val redisDown    = RuntimeException("redis unavailable")
  private val mongoDown    = RuntimeException("mongo unavailable")
  private val postgresDown = RuntimeException("postgres unavailable")

  override def spec: Spec[Any, Any] = suite("HelloPipeline")(
    suite("greet")(
      test("happy path: cache hit returns Greeting with cached=true") {
        val canonical = Greeting(message = "Hello from ZIO + Postgres", visits = 7L, cached = false)
        val visits    = FakeVisits.succeeding()
        val cache     = FakeGreetingCache.primed(canonical)
        val eventLog  = FakeEventLog.empty
        val pipeline  = HelloPipeline.from(visits, cache, eventLog)
        for greeting <- pipeline.greet
        yield assertTrue(
          greeting.cached,
          greeting.visits == 7L,
          greeting.message == "Hello from ZIO + Postgres",
          visits.calls == 0L,        // cache hit short-circuits Postgres
          eventLog.stored.size == 1, // event still appended
          eventLog.stored.head.visits == 7L
        )
      },
      test("cache miss: cold read increments Postgres, caches canonical form, appends event") {
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.empty
        val eventLog = FakeEventLog.empty
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        for greeting <- pipeline.greet
        yield assertTrue(
          !greeting.cached,
          greeting.visits == 1L,
          visits.calls == 1L,
          eventLog.stored.size == 1,
          eventLog.stored.head.visits == 1L,
          // Cached Flag invariant: canonical stored form is cached=false
          cache.stored.exists(g => !g.cached && g.visits == 1L)
        )
      },
      test("Cached Flag invariant: stored is cached=false, returned-on-hit is cached=true") {
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.empty
        val eventLog = FakeEventLog.empty
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        for
          coldGreeting <- pipeline.greet
          warmGreeting <- pipeline.greet
        yield assertTrue(
          !coldGreeting.cached,               // first call: cold read, cached=false
          warmGreeting.cached,                // second call: cache hit, cached=true
          warmGreeting.visits == 1L,          // same visit count served from cache
          visits.calls == 1L,                 // Postgres only hit once
          cache.stored.exists(g => !g.cached) // stored form remains canonical
        )
      },
      test("Redis GET down: degrades to cold read, still returns a Greeting") {
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.failing(redisDown)
        val eventLog = FakeEventLog.empty
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        for greeting <- pipeline.greet
        yield assertTrue(
          !greeting.cached,
          greeting.visits == 1L,
          visits.calls == 1L,
          eventLog.stored.size == 1
        )
      },
      test("Mongo append down: still returns a Greeting (degrade silently)") {
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.empty
        val eventLog = FakeEventLog.failing(mongoDown)
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        for greeting <- pipeline.greet
        yield assertTrue(
          !greeting.cached,
          greeting.visits == 1L,
          visits.calls == 1L
        )
      },
      test("Postgres down with empty cache: GreetingUnavailable") {
        val visits   = FakeVisits.failing(postgresDown)
        val cache    = FakeGreetingCache.empty
        val eventLog = FakeEventLog.empty
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        pipeline.greet.either.map {
          case Left(HelloFailure.GreetingUnavailable(detail)) =>
            assertTrue(detail.exists(_.contains("postgres unavailable")))
          case _ =>
            assertTrue(false)
        }
      },
      test("Postgres down but cache hit: cache saves us, returns cached Greeting") {
        val canonical = Greeting(message = "Hello from ZIO + Postgres", visits = 99L, cached = false)
        val visits    = FakeVisits.failing(postgresDown)
        val cache     = FakeGreetingCache.primed(canonical)
        val eventLog  = FakeEventLog.empty
        val pipeline  = HelloPipeline.from(visits, cache, eventLog)
        for greeting <- pipeline.greet
        yield assertTrue(
          greeting.cached,
          greeting.visits == 99L,
          visits.calls == 0L
        )
      }
    ),
    suite("recent")(
      test("returns events newest-first") {
        // Use a failing cache so each greet goes through the cold read and increments the counter
        // — otherwise call #1 would prime the cache and calls #2/#3 would short-circuit.
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.failing(redisDown)
        val eventLog = FakeEventLog.empty
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        for
          _      <- pipeline.greet
          _      <- pipeline.greet
          _      <- pipeline.greet
          recent <- pipeline.recent(2)
        yield assertTrue(
          recent.entries.size == 2,
          recent.entries.map(_.visits) == List(3L, 2L)
        )
      },
      test("Mongo down: RecentUnavailable") {
        val visits   = FakeVisits.succeeding()
        val cache    = FakeGreetingCache.empty
        val eventLog = FakeEventLog.failing(mongoDown)
        val pipeline = HelloPipeline.from(visits, cache, eventLog)
        pipeline.recent(10).either.map {
          case Left(HelloFailure.RecentUnavailable(detail)) =>
            assertTrue(detail.exists(_.contains("mongo unavailable")))
          case _ =>
            assertTrue(false)
        }
      }
    ),
    suite("health")(
      test("all stores up: status=ok, all flags true") {
        val pipeline = HelloPipeline.from(
          FakeVisits.succeeding(),
          FakeGreetingCache.empty,
          FakeEventLog.empty
        )
        for h <- pipeline.health
        yield assertTrue(
          h.status == "ok",
          h.postgres,
          h.redis,
          h.mongo
        )
      },
      test("Redis down: status=degraded, redis=false, others true") {
        val pipeline = HelloPipeline.from(
          FakeVisits.succeeding(),
          FakeGreetingCache.failing(redisDown),
          FakeEventLog.empty
        )
        for h <- pipeline.health
        yield assertTrue(
          h.status == "degraded",
          h.postgres,
          !h.redis,
          h.mongo
        )
      },
      test("Postgres down: status=degraded, postgres=false") {
        val pipeline = HelloPipeline.from(
          FakeVisits.failing(postgresDown),
          FakeGreetingCache.empty,
          FakeEventLog.empty
        )
        for h <- pipeline.health
        yield assertTrue(
          h.status == "degraded",
          !h.postgres,
          h.redis,
          h.mongo
        )
      },
      test("all stores down: status=degraded, all flags false") {
        val pipeline = HelloPipeline.from(
          FakeVisits.failing(postgresDown),
          FakeGreetingCache.failing(redisDown),
          FakeEventLog.failing(mongoDown)
        )
        for h <- pipeline.health
        yield assertTrue(
          h.status == "degraded",
          !h.postgres,
          !h.redis,
          !h.mongo
        )
      }
    )
  )
