package cortex.server.helloPipeline

import cortex.shared.api.Endpoints.Greeting
import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only `GreetingCache`. Pre-populate with `primed(g)` to simulate a cache hit; use `failing(error)` to
 * simulate Redis-down (both `get` and `put` fail). Records writes so tests can assert the canonical-form
 * invariant ("the value stored has `cached=false`").
 */
final class FakeGreetingCache private (
    storage: AtomicReference[Option[Greeting]],
    failure: Option[Throwable],
    pingValue: AtomicReference[Boolean]
) extends GreetingCache:

  override def get: Task[Option[Greeting]] = failure match
    case Some(e) => ZIO.fail(e)
    case None    => ZIO.succeed(storage.get())

  override def put(g: Greeting): Task[Unit] = failure match
    case Some(e) => ZIO.fail(e)
    case None    => ZIO.succeed(storage.set(Some(g)))

  override def ping: UIO[Boolean] = ZIO.succeed(pingValue.get())

  def stored: Option[Greeting] = storage.get()

object FakeGreetingCache:

  def empty: FakeGreetingCache =
    FakeGreetingCache(AtomicReference(None), None, AtomicReference(true))

  /** Cache pre-populated with `g`. Tests usually pass `g.copy(cached = false)` — the canonical form. */
  def primed(g: Greeting): FakeGreetingCache =
    FakeGreetingCache(AtomicReference(Some(g)), None, AtomicReference(true))

  /** Both `get` and `put` fail with `error`; ping returns false. */
  def failing(error: Throwable): FakeGreetingCache =
    FakeGreetingCache(AtomicReference(None), Some(error), AtomicReference(false))
