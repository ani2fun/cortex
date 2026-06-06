package codefolio.server.content

import zio.*

/**
 * A single in-memory value rebuilt from a filesystem tree, invalidated by an mtime watermark.
 *
 * Both the Cortex and Blog pipelines cache a derived index (not file bodies — those are re-read on demand)
 * and rebuild it when the content tree changes on disk. This module owns that cache-and-invalidate behaviour
 * so each pipeline only supplies its own filesystem seam and its pure walker:
 *
 *   - [[get]] returns the cached value, rebuilding on the first call and — when `autoReload` is on — whenever
 *     the observed mtime advances past the watermark captured at the last build.
 *   - `currentMtime` is a cheap whole-tree watermark; `rebuild` scans the tree and produces the derived
 *     value. Both are supplied once at construction and are expected to be stable, re-runnable effects (they
 *     close over a fixed FS accessor).
 *
 * The watermark is read *before* the rebuild, so a content edit that lands mid-rebuild is caught by the next
 * [[get]] rather than being silently folded into the current build under a newer watermark.
 *
 * Concurrency: two requests racing during a content edit can both observe a stale cache and both rebuild —
 * harmless, because `rebuild` is idempotent (last write wins) and the stale-serve window is bounded by a
 * single rebuild's worth of work. `autoReload` is off in production (the cache fills once at first request)
 * and on under `bin/dev`.
 *
 * The watermark lives here, not inside the cached value `S` — pipelines keep their state type free of cache
 * bookkeeping. New filesystem-driven pipelines reuse this module rather than reimplementing the dance.
 */
final class MtimeCachedIndex[E, S] private (
    autoReload: Boolean,
    currentMtime: IO[E, Long],
    rebuild: IO[E, S],
    cache: Ref[Option[MtimeCachedIndex.Entry[S]]]
):
  import MtimeCachedIndex.Entry

  /** The cached value. Rebuilds on the first call, and on an mtime advance when `autoReload` is on. */
  def get: IO[E, S] =
    cache.get.flatMap {
      case None => loadAndCache
      case Some(entry) =>
        if !autoReload then ZIO.succeed(entry.value)
        else
          currentMtime.flatMap { mt =>
            if mt > entry.watermark then loadAndCache
            else ZIO.succeed(entry.value)
          }
    }

  // Watermark first, then rebuild: a failed rebuild never writes the cache, so the next `get` retries.
  private def loadAndCache: IO[E, S] =
    for
      mt    <- currentMtime
      value <- rebuild
      _     <- cache.set(Some(Entry(value, mt)))
    yield value

object MtimeCachedIndex:

  /** The cached value paired with the mtime watermark observed when it was built. */
  final private case class Entry[S](value: S, watermark: Long)

  /**
   * Build an empty cache. The first [[MtimeCachedIndex.get]] triggers a rebuild; subsequent calls serve from
   * cache until the watermark advances (when `autoReload` is on).
   */
  def make[E, S](
      autoReload: Boolean,
      currentMtime: IO[E, Long],
      rebuild: IO[E, S]
  ): UIO[MtimeCachedIndex[E, S]] =
    Ref.make(Option.empty[Entry[S]]).map(new MtimeCachedIndex(autoReload, currentMtime, rebuild, _))
