package cortex.server.cortexPipeline

import cortex.shared.book.CortexIndexWalker.CortexEntry
import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only [[CortexFs]] accessor. Holds an in-memory snapshot (mtime + literal in-memory tree + file bodies)
 * and serves it from the trait methods. Tests simulate content edits via [[advance]] without touching the
 * filesystem.
 *
 * Because the seam returns a raw [[CortexEntry]] tree rather than a pre-built `CortexState`, every test that
 * uses this fake also exercises the real [[cortex.shared.book.CortexIndexWalker]] — the walker integration is
 * on the pipeline side of the seam, not faked away.
 *
 * `loadRootsCalls` records how often `loadRoots` was invoked so cache-hit and auto-reload assertions can be
 * sharp ("rebuilt exactly twice, then served from cache").
 */
final class FakeCortexFs private (
    snapshot: AtomicReference[FakeCortexFs.Snapshot],
    loadCount: AtomicReference[Int]
) extends CortexFs:

  override def currentMtime: IO[CortexFailure, Long] =
    ZIO.succeed(snapshot.get().mtime)

  override def readFileSafe(rel: String): IO[CortexFailure, String] =
    ZIO
      .fromOption(snapshot.get().files.get(rel))
      .orElseFail(CortexFailure.NotFound: CortexFailure)

  override def loadRoots: IO[CortexFailure, List[CortexEntry]] =
    ZIO.succeed {
      val _ = loadCount.updateAndGet(_ + 1)
      snapshot.get().roots
    }

  /** Move time forward and swap the literal tree in one step. */
  def advance(toMtime: Long, newRoots: List[CortexEntry]): Unit =
    snapshot.set(snapshot.get().copy(mtime = toMtime, roots = newRoots))

  def loadRootsCalls: Int = loadCount.get()

object FakeCortexFs:

  /**
   * Frozen view of the world the fake is serving. `roots` is the literal in-memory tree the pipeline walker
   * consumes; `files` maps `book/relPath` keys (the same shape the live pipeline asks `readFileSafe` for) to
   * raw chapter bodies, used for chapter-payload assembly.
   */
  final case class Snapshot(
      mtime: Long,
      roots: List[CortexEntry],
      files: Map[String, String] = Map.empty
  )

  def apply(initial: Snapshot): FakeCortexFs =
    new FakeCortexFs(AtomicReference(initial), AtomicReference(0))
