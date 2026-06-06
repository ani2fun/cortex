package cortex.server.helloPipeline

import cortex.shared.api.Endpoints.HelloEvent
import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only `EventLog`. `empty` is the happy path: appends are stored and `recent` reads them back
 * newest-first. `failing(error)` simulates Mongo-down: append, recent, and ping all fail / return false.
 */
final class FakeEventLog private (
    appended: AtomicReference[List[HelloEvent]],
    failure: Option[Throwable],
    pingValue: AtomicReference[Boolean]
) extends EventLog:

  override def append(event: HelloEvent): Task[Unit] = failure match
    case Some(e) => ZIO.fail(e)
    case None    => ZIO.succeed(appended.updateAndGet(_ :+ event)).unit

  override def recent(limit: Int): Task[List[HelloEvent]] = failure match
    case Some(e) => ZIO.fail(e)
    case None    => ZIO.succeed(appended.get().reverse.take(limit))

  override def ping: UIO[Boolean] = ZIO.succeed(pingValue.get())

  def stored: List[HelloEvent] = appended.get()

object FakeEventLog:

  def empty: FakeEventLog =
    FakeEventLog(AtomicReference(List.empty), None, AtomicReference(true))

  /** Every operation fails with `error`; ping returns false. */
  def failing(error: Throwable): FakeEventLog =
    FakeEventLog(AtomicReference(List.empty), Some(error), AtomicReference(false))
