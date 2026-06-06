package cortex.server.helloPipeline

import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only `Visits` accessor. Factory methods (`succeeding` / `failing`) cover the test scenarios; direct
 * construction is private. Records call counts so tests can assert "Postgres was hit exactly once" or "the
 * cache hit short-circuited the cold path".
 */
final class FakeVisits private (
    increment: Long => Task[Long],
    pingValue: AtomicReference[Boolean]
) extends Visits:

  private val callCount = AtomicReference(0L)

  override def incrementAndGet: Task[Long] =
    ZIO.succeed(callCount.updateAndGet(_ + 1L)).flatMap(increment)

  override def ping: UIO[Boolean] = ZIO.succeed(pingValue.get())

  def calls: Long = callCount.get()

  def setPing(v: Boolean): Unit = pingValue.set(v)

object FakeVisits:

  /** First call returns 1, second 2, and so on. Ping returns true. */
  def succeeding(): FakeVisits =
    FakeVisits(n => ZIO.succeed(n), AtomicReference(true))

  /** Every `incrementAndGet` fails with `error`; ping returns false. */
  def failing(error: Throwable): FakeVisits =
    FakeVisits(_ => ZIO.fail(error), AtomicReference(false))
