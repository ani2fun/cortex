package codefolio.server.blogPipeline

import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only [[BlogFs]] accessor. Holds an in-memory snapshot (mtime + slug→raw map) and serves it from the
 * trait methods. `loadPostsCalls` records how often `loadPosts` was invoked so cache-hit and auto-reload
 * assertions can be sharp.
 */
final class FakeBlogFs private (
    snapshot: AtomicReference[FakeBlogFs.Snapshot],
    loadCount: AtomicReference[Int]
) extends BlogFs:

  override def currentMtime: IO[BlogFailure, Long] =
    ZIO.succeed(snapshot.get().mtime)

  override def readPostSafe(slug: String): IO[BlogFailure, String] =
    ZIO
      .fromOption(snapshot.get().posts.find(_._1 == slug).map(_._2))
      .orElseFail(BlogFailure.NotFound: BlogFailure)

  override def loadPosts: IO[BlogFailure, List[(String, String)]] =
    ZIO.succeed {
      val _ = loadCount.updateAndGet(_ + 1)
      snapshot.get().posts
    }

  /** Move time forward and swap the post list in one step. */
  def advance(toMtime: Long, newPosts: List[(String, String)]): Unit =
    snapshot.set(FakeBlogFs.Snapshot(toMtime, newPosts))

  def loadPostsCalls: Int = loadCount.get()

object FakeBlogFs:

  /** Frozen view of the world the fake is serving — mtime watermark + an ordered list of `(slug, raw)`. */
  final case class Snapshot(mtime: Long, posts: List[(String, String)])

  def apply(initial: Snapshot): FakeBlogFs =
    new FakeBlogFs(AtomicReference(initial), AtomicReference(0))
