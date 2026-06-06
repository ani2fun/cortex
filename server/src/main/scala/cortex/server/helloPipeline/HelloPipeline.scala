package cortex.server.helloPipeline

import cortex.server.config.{MongoConfig, RedisConfig}
import cortex.shared.api.Endpoints.{Greeting, HealthStatus, HelloEvent, RecentCalls}
import cortex.shared.api.EndpointsJsonSerdes.given
import com.mongodb.client.model.{IndexOptions, Indexes, Sorts}
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection}
import com.mongodb.{ConnectionString, MongoClientSettings}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.{ClientOptions, RedisClient, SetArgs, TimeoutOptions}
import org.bson.Document
import zio.*

import java.util.concurrent.TimeUnit
import javax.sql.DataSource as JDataSource

/**
 * Errors that escape the Hello pipeline. The orchestration absorbs Redis and Mongo failures (per ADR-0002),
 * so the only failures that surface are the canonical-store ones: Postgres-down on `/api/hello` and
 * Mongo-down on `/api/recent`. Both map to 503 in `ApiErrors.toHttp`.
 */
sealed trait HelloFailure extends Product with Serializable

object HelloFailure:
  /** Postgres unreachable AND no usable cached Greeting. 503. */
  final case class GreetingUnavailable(detail: Option[String] = None) extends HelloFailure

  /** Mongo unreachable while serving `/api/recent`. 503. */
  final case class RecentUnavailable(detail: Option[String] = None) extends HelloFailure

/**
 * Postgres-backed Visit Count accessor. Internal seam — package-private; live impl built by
 * `HelloPipeline.live`, fakes built in test sources.
 */
private[helloPipeline] trait Visits:
  def incrementAndGet: Task[Long]
  def ping: UIO[Boolean]

/**
 * Read-through Greeting cache. `get` decodes a previously-stored canonical (`cached=false`) Greeting and
 * returns it raw — flipping the Cached Flag is the orchestration's job, not the cache's. Internal seam.
 */
private[helloPipeline] trait GreetingCache:
  def get: Task[Option[Greeting]]
  def put(g: Greeting): Task[Unit]
  def ping: UIO[Boolean]

/** Append-only Mongo log of Hello Events. Internal seam. */
private[helloPipeline] trait EventLog:
  def append(event: HelloEvent): Task[Unit]
  def recent(limit: Int): Task[List[HelloEvent]]
  def ping: UIO[Boolean]

/**
 * Cached Flag invariant: the canonical form persisted in Redis is always `cached=false`. Reads flip the flag.
 * Kept package-private because the rule has no meaning outside the Hello pipeline.
 */
extension (g: Greeting) private[helloPipeline] def markCached: Greeting = g.copy(cached = true)

/**
 * Three-store demo for `/api/hello`, `/api/recent`, `/api/health`.
 *
 *   - `greet` reads Redis → falls back to a Postgres counter increment → appends a Mongo event.
 *   - `recent` reads Mongo (newest first).
 *   - `health` pings each store via the same accessors `greet` uses.
 *
 * The Degraded rule (ADR-0002) is implemented at this seam, not inside the accessors — Redis and Mongo
 * failures are caught and logged so the request still returns a Greeting as long as Postgres is reachable or
 * the cache holds a Greeting.
 */
trait HelloPipeline:
  def greet: IO[HelloFailure, Greeting]
  def recent(limit: Int): IO[HelloFailure, RecentCalls]
  def health: UIO[HealthStatus]

object HelloPipeline:

  private val CacheKey = "cortex:greeting:latest"

  /** Direct construction from explicit accessors. Used by tests; production wires via [[live]]. */
  def from(visits: Visits, cache: GreetingCache, eventLog: EventLog): HelloPipeline =
    HelloPipelineLive(visits, cache, eventLog)

  /**
   * Resource-safe layer: builds a Hikari-backed `Visits`, a Lettuce-backed `GreetingCache`, and a
   * Mongo-backed `EventLog`, releasing the underlying clients in reverse order on shutdown.
   */
  val live: ZLayer[RedisConfig & MongoConfig & JDataSource, Throwable, HelloPipeline] =
    ZLayer.scoped {
      for
        redisCfg <- ZIO.service[RedisConfig]
        mongoCfg <- ZIO.service[MongoConfig]
        ds       <- ZIO.service[JDataSource]
        cache    <- liveCache(redisCfg)
        events   <- liveEventLog(mongoCfg)
      yield HelloPipelineLive(liveVisits(ds), cache, events)
    }

  // ---------------------------------------------------------------------------
  // Live adapters
  // ---------------------------------------------------------------------------

  private def liveVisits(ds: JDataSource): Visits =
    new Visits:
      override def incrementAndGet: Task[Long] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          conn.setAutoCommit(false)
          val stmt = conn.prepareStatement(
            "UPDATE visits SET count = count + 1 WHERE id = 1 RETURNING count"
          )
          try
            val rs = stmt.executeQuery()
            try
              val n = if rs.next() then rs.getLong(1) else 0L
              conn.commit()
              n
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def ping: UIO[Boolean] =
        ZIO
          .attemptBlocking {
            val conn = ds.getConnection
            try conn.isValid(2)
            finally conn.close()
          }
          .catchAll(_ => ZIO.succeed(false))

  private def liveCache(cfg: RedisConfig): ZIO[Scope, Throwable, GreetingCache] =
    val ttl = cfg.ttlSecs.seconds
    for
      client <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val c = RedisClient.create(cfg.url)
          // Without an explicit command timeout Lettuce will block on a stalled
          // connection effectively forever — see ADR-0002. Cap commands at 2s so
          // the Degraded path actually returns within a request budget.
          c.setOptions(
            ClientOptions.builder
              .timeoutOptions(TimeoutOptions.enabled(java.time.Duration.ofSeconds(2)))
              .build
          )
          c
        }
      )(c => ZIO.attempt(c.shutdown()).orDie)
      connection <- ZIO.acquireRelease(
        ZIO.attemptBlocking(client.connect())
      )(c => ZIO.attempt(c.close()).orDie)
    yield liveCacheImpl(connection, ttl)

  private def liveCacheImpl(
      connection: StatefulRedisConnection[String, String],
      ttl: Duration
  ): GreetingCache =
    val async = connection.async()
    new GreetingCache:
      override def get: Task[Option[Greeting]] =
        ZIO
          .fromCompletionStage(async.get(CacheKey).toCompletableFuture)
          .map(Option(_))
          .flatMap {
            case Some(json) =>
              ZIO.fromEither(decode[Greeting](json)).map(Some(_)).catchAll(_ => ZIO.none)
            case None => ZIO.none
          }

      override def put(g: Greeting): Task[Unit] =
        val args = SetArgs.Builder.ex(ttl.toSeconds)
        ZIO.fromCompletionStage(async.set(CacheKey, g.asJson.noSpaces, args).toCompletableFuture).unit

      override def ping: UIO[Boolean] =
        ZIO
          .fromCompletionStage(async.ping().toCompletableFuture)
          .map(_ == "PONG")
          .catchAll(_ => ZIO.succeed(false))

  private def liveEventLog(cfg: MongoConfig): ZIO[Scope, Throwable, EventLog] =
    for
      client <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          // Default serverSelectionTimeoutMS is 30s — that hangs every /api/hello
          // when Mongo is down post-startup. Trim to 2s so the Degraded path
          // (ADR-0002) actually returns within the request budget.
          val settings = MongoClientSettings.builder
            .applyConnectionString(ConnectionString(cfg.uri))
            .applyToClusterSettings { b =>
              b.serverSelectionTimeout(2, TimeUnit.SECONDS); ()
            }
            .applyToSocketSettings { b =>
              b.connectTimeout(2, TimeUnit.SECONDS); ()
            }
            .build
          MongoClients.create(settings)
        }
      )(c => ZIO.attempt(c.close()).orDie)
      coll <- ZIO.attemptBlocking {
        val db   = client.getDatabase(cfg.database)
        val coll = db.getCollection("hello_events")
        coll.createIndex(
          Indexes.descending("timestampEpochMs"),
          IndexOptions().name("ts_desc")
        )
        coll
      }
    yield liveEventLogImpl(client, coll)

  private def liveEventLogImpl(
      client: MongoClient,
      coll: MongoCollection[Document]
  ): EventLog =
    new EventLog:
      override def append(event: HelloEvent): Task[Unit] = ZIO.attemptBlocking {
        val doc = Document()
          .append("timestampEpochMs", java.lang.Long.valueOf(event.timestampEpochMs))
          .append("visits", java.lang.Long.valueOf(event.visits))
        coll.insertOne(doc)
        ()
      }

      override def recent(limit: Int): Task[List[HelloEvent]] = ZIO.attemptBlocking {
        val cursor = coll
          .find()
          .sort(Sorts.descending("timestampEpochMs"))
          .limit(limit)
          .iterator()
        try
          val buf = List.newBuilder[HelloEvent]
          while cursor.hasNext do
            val d = cursor.next()
            buf += HelloEvent(
              timestampEpochMs = d.getLong("timestampEpochMs"),
              visits = d.getLong("visits")
            )
          buf.result()
        finally cursor.close()
      }

      override def ping: UIO[Boolean] =
        ZIO
          .attemptBlocking(client.getDatabase("admin").runCommand(Document("ping", 1)))
          .as(true)
          .catchAll(_ => ZIO.succeed(false))

final private class HelloPipelineLive(
    visits: Visits,
    cache: GreetingCache,
    eventLog: EventLog
) extends HelloPipeline:

  override def greet: IO[HelloFailure, Greeting] =
    val readFromCache: UIO[Option[Greeting]] =
      cache.get
        .map(_.map(_.markCached))
        .catchAll(e => ZIO.logWarning(s"Redis GET failed: ${e.getMessage}").as(None))

    val coldRead: IO[HelloFailure, Greeting] =
      visits.incrementAndGet
        .mapError(e => HelloFailure.GreetingUnavailable(Option(e.getMessage)))
        .map(count => Greeting(message = "Hello from ZIO + Postgres", visits = count, cached = false))
        .tap(g => cache.put(g).catchAll(e => ZIO.logWarning(s"Redis SET failed: ${e.getMessage}")))

    for
      maybeCached <- readFromCache
      greeting <- maybeCached match
        case Some(g) => ZIO.succeed(g)
        case None    => coldRead
      _ <- eventLog
        .append(HelloEvent(java.lang.System.currentTimeMillis(), greeting.visits))
        .catchAll(e => ZIO.logWarning(s"Mongo append failed: ${e.getMessage}"))
    yield greeting

  override def recent(limit: Int): IO[HelloFailure, RecentCalls] =
    eventLog
      .recent(limit)
      .map(RecentCalls(_))
      .mapError(e => HelloFailure.RecentUnavailable(Option(e.getMessage)))

  override def health: UIO[HealthStatus] =
    for
      pg <- visits.ping
      rd <- cache.ping
      mg <- eventLog.ping
      ok = pg && rd && mg
    yield HealthStatus(
      status = if ok then "ok" else "degraded",
      postgres = pg,
      redis = rd,
      mongo = mg
    )
