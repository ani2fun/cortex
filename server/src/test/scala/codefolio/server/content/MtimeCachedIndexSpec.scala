package codefolio.server.content

import zio.*
import zio.test.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Direct tests for the mtime-keyed cache shared by the Cortex and Blog pipelines. The pipelines' own specs
 * exercise this module through `*.from(fakeFs, autoReload)`; this spec pins the invalidation logic itself —
 * rebuild-on-first-call, serve-from-cache, rebuild-on-advance, freeze-when-disabled, failure propagation.
 */
object MtimeCachedIndexSpec extends ZIOSpecDefault:

  /** A counting rebuild over a mutable mtime + value, so tests can assert exactly how many rebuilds ran. */
  final private class Probe(initialMtime: Long, initialValue: String):
    private val mtime        = AtomicReference(initialMtime)
    private val value        = AtomicReference(initialValue)
    private val rebuildCount = AtomicReference(0)

    val currentMtime: IO[String, Long] = ZIO.succeed(mtime.get())

    val rebuild: IO[String, String] =
      ZIO.succeed {
        val _ = rebuildCount.updateAndGet(_ + 1)
        value.get()
      }

    /** Move time forward and swap the value the next rebuild will produce. */
    def advance(toMtime: Long, toValue: String): Unit =
      mtime.set(toMtime)
      value.set(toValue)

    def rebuilds: Int = rebuildCount.get()

  override def spec: Spec[Any, Any] = suite("MtimeCachedIndex")(
    test("first get triggers a rebuild") {
      val probe = Probe(1L, "v1")
      for
        idx   <- MtimeCachedIndex.make(autoReload = true, probe.currentMtime, probe.rebuild)
        value <- idx.get
      yield assertTrue(value == "v1", probe.rebuilds == 1)
    },
    test("repeated get with an unchanged mtime serves from cache (rebuilds once)") {
      val probe = Probe(1L, "v1")
      for
        idx <- MtimeCachedIndex.make(autoReload = true, probe.currentMtime, probe.rebuild)
        _   <- idx.get
        _   <- idx.get
        _   <- idx.get
      yield assertTrue(probe.rebuilds == 1)
    },
    test("autoReload=true rebuilds when the mtime watermark advances") {
      val probe = Probe(1L, "old")
      for
        idx <- MtimeCachedIndex.make(autoReload = true, probe.currentMtime, probe.rebuild)
        v1  <- idx.get
        _ = probe.advance(toMtime = 2L, toValue = "new")
        v2 <- idx.get
      yield assertTrue(v1 == "old", v2 == "new", probe.rebuilds == 2)
    },
    test("autoReload=false freezes the cache even when the mtime advances") {
      val probe = Probe(1L, "old")
      for
        idx <- MtimeCachedIndex.make(autoReload = false, probe.currentMtime, probe.rebuild)
        v1  <- idx.get
        _ = probe.advance(toMtime = 2L, toValue = "new")
        v2 <- idx.get
      yield assertTrue(v1 == "old", v2 == "old", probe.rebuilds == 1)
    },
    test("an equal (not advanced) mtime does not rebuild") {
      val probe = Probe(5L, "v1")
      for
        idx <- MtimeCachedIndex.make(autoReload = true, probe.currentMtime, probe.rebuild)
        _   <- idx.get
        _ = probe.advance(toMtime = 5L, toValue = "v2") // same watermark — the check is strict `>`
        value <- idx.get
      yield assertTrue(value == "v1", probe.rebuilds == 1)
    },
    test("a failed rebuild propagates and leaves the cache empty so the next get retries") {
      for
        attempts <- Ref.make(0)
        rebuild = attempts
          .getAndUpdate(_ + 1)
          .flatMap(n => if n == 0 then ZIO.fail("boom") else ZIO.succeed("recovered"))
        idx    <- MtimeCachedIndex.make(autoReload = true, ZIO.succeed(1L), rebuild)
        first  <- idx.get.either
        second <- idx.get
      yield assertTrue(first == Left("boom"), second == "recovered")
    },
    test("a failed currentMtime read propagates") {
      for
        idx <- MtimeCachedIndex.make[String, String](
          autoReload = true,
          currentMtime = ZIO.fail("mtime unavailable"),
          rebuild = ZIO.succeed("v")
        )
        out <- idx.get.either
      yield assertTrue(out == Left("mtime unavailable"))
    }
  )
