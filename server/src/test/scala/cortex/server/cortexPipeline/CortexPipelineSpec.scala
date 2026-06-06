package cortex.server.cortexPipeline

import cortex.server.config.CortexConfig
import cortex.shared.book.CortexIndexWalker.{BookMeta, CortexDir, CortexEntry, CortexFile}
import zio.*
import zio.test.*

import java.nio.charset.StandardCharsets
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object CortexPipelineSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Throwable] = suite("CortexPipeline")(
    test("builds an index and chapter payload from the content tree") {
      ZIO.scoped {
        for
          root <- tempRoot
          _ <- write(
            root.resolve("book-one/book.json"),
            """{
              |  "title": "Book One",
              |  "description": "Architecture notes",
              |  "tags": ["scala", "zio"],
              |  "estimatedReadingMinutes": 12
              |}""".stripMargin
          )
          _ <- write(
            root.resolve("book-one/01-start-here.md"),
            """---
              |title: Start Here
              |summary: Begin with the system shape.
              |---
              |# Heading From Body
              |
              |The first chapter body.
              |""".stripMargin
          )
          _ <- write(
            root.resolve("book-one/02-system/_section.json"),
            """{"title": "System"}"""
          )
          _ <- write(
            root.resolve("book-one/02-system/01-next-step.md"),
            """# Next Step
              |
              |The second chapter body.
              |""".stripMargin
          )
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            idx      <- pipeline.index.mapError(toThrowable)
            payload  <- pipeline.chapter("book-one", "start-here").mapError(toThrowable)
          yield
            val book     = idx.books.head
            val chapters = book.chapters
            assertTrue(
              idx.books.map(_.slug) == List("book-one"),
              book.title == "Book One",
              book.description == "Architecture notes",
              book.tags == Some(Seq("scala", "zio")),
              book.estimatedReadingMinutes == Some(12),
              chapters.map(_.slug) == List("start-here", "system-next-step"),
              chapters(1).groupPath == Some(Seq("System")),
              payload.frontmatter.title == "Start Here",
              payload.frontmatter.summary == Some("Begin with the system shape."),
              payload.raw.trim == "# Heading From Body\n\nThe first chapter body.",
              payload.prevSlug.isEmpty,
              payload.nextSlug == Some("system-next-step")
            )
          ).provideLayer(pipelineLayer(root))
        yield result
      }
    },
    test("rejects path traversal shaped book and chapter slugs") {
      ZIO.scoped {
        for
          root <- tempRoot
          _    <- write(root.resolve("book/01-safe.md"), "# Safe")
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            outcome  <- pipeline.chapter("../book", "safe").either
          yield assertTrue(outcome == Left(CortexFailure.NotFound)))
            .provideLayer(pipelineLayer(root))
        yield result
      }
    },
    test("fails the index when two files produce the same chapter slug") {
      ZIO.scoped {
        for
          root <- tempRoot
          _    <- write(root.resolve("book/01-overview.md"), "# First")
          _    <- write(root.resolve("book/02-overview.md"), "# Second")
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            outcome  <- pipeline.index.either
          yield outcome match
            case Left(CortexFailure.IndexInvalid(detail)) =>
              assertTrue(
                detail.contains("book"),
                detail.contains("duplicate chapter slugs"),
                detail.contains("overview")
              )
            case _ =>
              assertNever(s"expected IndexInvalid, got $outcome")
          )
            .provideLayer(pipelineLayer(root))
        yield result
      }
    },
    test("falls back to humanised slug when book.json is malformed") {
      ZIO.scoped {
        for
          root <- tempRoot
          _    <- write(root.resolve("data-structures/book.json"), "{not json")
          _    <- write(root.resolve("data-structures/01-arrays.md"), "# Arrays\n")
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            idx      <- pipeline.index.mapError(toThrowable)
          yield
            val book = idx.books.head
            assertTrue(
              idx.books.map(_.slug) == List("data-structures"),
              book.title == "Data Structures",
              book.description == "",
              book.tags.isEmpty,
              book.estimatedReadingMinutes.isEmpty
            )
          ).provideLayer(pipelineLayer(root))
        yield result
      }
    },
    test("treats unterminated frontmatter as plain body") {
      ZIO.scoped {
        for
          root <- tempRoot
          _    <- write(root.resolve("book/book.json"), """{"title": "Book"}""")
          _ <- write(
            root.resolve("book/01-broken.md"),
            """---
              |title: Should Not Be Parsed
              |summary: Also ignored
              |
              |Body content with no closing fence.
              |""".stripMargin
          )
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            payload  <- pipeline.chapter("book", "broken").mapError(toThrowable)
          yield assertTrue(
            payload.frontmatter.title == "Broken",
            payload.frontmatter.summary.isEmpty,
            payload.raw.startsWith("---"),
            payload.raw.contains("Body content with no closing fence.")
          )).provideLayer(pipelineLayer(root))
        yield result
      }
    },
    test("rebuilds the index when content mtime advances (autoReload=true)") {
      ZIO.scoped {
        for
          root <- tempRoot
          file = root.resolve("book/01-c.md")
          _ <- write(file, "# Original Title")
          result <- (for
            pipeline <- ZIO.service[CortexPipeline]
            idx1     <- pipeline.index.mapError(toThrowable)
            _        <- write(file, "# Updated Title")
            // Bump mtime by a wide margin so the MtimeCachedIndex watermark check fires
            // even on filesystems with coarse timestamp resolution.
            _ <- ZIO.attempt(
              Files.setLastModifiedTime(
                file,
                FileTime.fromMillis(java.lang.System.currentTimeMillis() + 60_000L)
              )
            )
            idx2 <- pipeline.index.mapError(toThrowable)
          yield assertTrue(
            idx1.books.head.chapters.head.title == "Original Title",
            idx2.books.head.chapters.head.title == "Updated Title"
          )).provideLayer(pipelineLayer(root))
        yield result
      }
    },
    // The cache + autoReload tests below exercise CortexPipelineLive against the FakeCortexFs seam: they
    // assert the pipeline correctly wires MtimeCachedIndex AND the walker integration, without filesystem
    // ceremony. The mtime-cache invalidation logic itself is pinned directly by MtimeCachedIndexSpec; the
    // live-FS test above additionally verifies that LiveCortexFs.currentMtime walks the tree on disk.
    test("cache hit when mtime is unchanged: loadRoots runs once across multiple reads") {
      val fake = FakeCortexFs(
        FakeCortexFs.Snapshot(mtime = 100L, roots = List(bookEntry("only", "Only")))
      )
      for
        pipeline <- CortexPipeline.from(fake, autoReload = true)
        _        <- pipeline.index.mapError(toThrowable)
        _        <- pipeline.index.mapError(toThrowable)
        _        <- pipeline.index.mapError(toThrowable)
      yield assertTrue(fake.loadRootsCalls == 1)
    },
    test("autoReload=true rebuilds the cache when mtime advances") {
      val fake = FakeCortexFs(
        FakeCortexFs.Snapshot(mtime = 100L, roots = List(bookEntry("a", "Old")))
      )
      for
        pipeline <- CortexPipeline.from(fake, autoReload = true)
        out1     <- pipeline.index.mapError(toThrowable)
        _        <- ZIO.succeed(fake.advance(toMtime = 200L, newRoots = List(bookEntry("a", "New"))))
        out2     <- pipeline.index.mapError(toThrowable)
      yield assertTrue(
        out1.books.head.title == "Old",
        out2.books.head.title == "New",
        fake.loadRootsCalls == 2
      )
    },
    test("autoReload=false freezes the cache even when mtime advances") {
      val fake = FakeCortexFs(
        FakeCortexFs.Snapshot(mtime = 100L, roots = List(bookEntry("a", "Old")))
      )
      for
        pipeline <- CortexPipeline.from(fake, autoReload = false)
        out1     <- pipeline.index.mapError(toThrowable)
        _        <- ZIO.succeed(fake.advance(toMtime = 200L, newRoots = List(bookEntry("a", "New"))))
        out2     <- pipeline.index.mapError(toThrowable)
      yield assertTrue(
        out1.books.head.title == "Old",
        out2.books.head.title == "Old",
        fake.loadRootsCalls == 1
      )
    },
    // The fake-based tests above exercise the walker integration on the happy path. This one
    // additionally proves that walker-level errors (DuplicateSlug here) flow through
    // `loadAndCache → CortexIndexWalker.walk → indexErrorToFailure` without filesystem ceremony.
    test("walker errors from the FakeCortexFs seam surface as IndexInvalid") {
      val roots = List(
        CortexDir(
          name = "b",
          children = List(
            CortexFile("01-overview.md", "# First"),
            CortexFile("02-overview.md", "# Second")
          )
        )
      )
      val fake = FakeCortexFs(FakeCortexFs.Snapshot(mtime = 1L, roots = roots))
      for
        pipeline <- CortexPipeline.from(fake, autoReload = false)
        outcome  <- pipeline.index.either
      yield outcome match
        case Left(CortexFailure.IndexInvalid(detail)) =>
          assertTrue(detail.contains("duplicate chapter slugs"))
        case other => assertNever(s"expected IndexInvalid, got $other")
    }
  )

  /** Minimal book entry — a directory whose `BookMeta.title` carries the display title. */
  private def bookEntry(slug: String, title: String): CortexEntry =
    CortexDir(
      name = slug,
      bookMeta = Some(
        BookMeta(
          title = Some(title),
          description = None,
          tags = None,
          estimatedReadingMinutes = None
        )
      ),
      children = List.empty
    )

  private def pipelineLayer(root: Path): ZLayer[Any, Nothing, CortexPipeline] =
    ZLayer.succeed(CortexConfig(root = root.toString, autoReload = true)) >>> CortexPipeline.live

  private val tempRoot: ZIO[Scope, Throwable, Path] =
    ZIO.acquireRelease(ZIO.attempt(Files.createTempDirectory("cortex-cortex-test-"))) { root =>
      ZIO.attempt(deleteRecursively(root)).orDie
    }

  private def write(path: Path, content: String): Task[Unit] =
    ZIO.attemptBlocking {
      Files.createDirectories(path.getParent)
      Files.writeString(path, content, StandardCharsets.UTF_8)
      ()
    }

  private def deleteRecursively(root: Path): Unit =
    if Files.exists(root) then
      val paths = Files.walk(root)
      try
        paths.iterator().asScala.toList.reverse.foreach(path => Files.deleteIfExists(path))
      finally paths.close()

  private def toThrowable(failure: CortexFailure): RuntimeException =
    RuntimeException(failure.toString)
