package cortex.server.http

import zio.*
import zio.http.*
import zio.test.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * Tests the security-critical part of static file serving: path-traversal containment, content-type
 * resolution, and the read-or-404 contract. Both [[StaticRoutes]] and [[CortexAssetRoutes]] route through
 * this module, so the `..` / absolute-path / symlink-escape rejections are pinned once here rather than being
 * absent on two separate route sets.
 */
object FileServerSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Throwable] = suite("FileServer")(
    suite("serve")(
      test("serves an existing file with 200 and its bytes") {
        ZIO.scoped {
          for
            root <- tempRoot
            _    <- write(root.resolve("hello.txt"), "hello world")
            res  <- FileServer(root.toString).serve("hello.txt")
            body <- res.body.asString
          yield assertTrue(res.status == Status.Ok, body == "hello world")
        }
      },
      test("404s a missing file") {
        ZIO.scoped {
          for
            root <- tempRoot
            res  <- FileServer(root.toString).serve("nope.txt")
          yield assertTrue(res.status == Status.NotFound)
        }
      },
      test("404s a `..` traversal that escapes the root") {
        ZIO.scoped {
          for
            root    <- tempRoot
            outside <- ZIO.attempt(Files.createTempFile("cortex-fileserver-outside-", ".txt"))
            _       <- ZIO.attempt(Files.writeString(outside, "secret"))
            _       <- ZIO.addFinalizer(ZIO.attempt(Files.deleteIfExists(outside)).orDie)
            res     <- FileServer(root.toString).serve(s"../${outside.getFileName.toString}")
          yield assertTrue(res.status == Status.NotFound)
        }
      },
      test("404s an absolute-path input") {
        ZIO.scoped {
          for
            root <- tempRoot
            res  <- FileServer(root.toString).serve("/etc/hosts")
          yield assertTrue(res.status == Status.NotFound)
        }
      },
      test("404s a directory") {
        ZIO.scoped {
          for
            root <- tempRoot
            _    <- ZIO.attempt(Files.createDirectories(root.resolve("sub")))
            res  <- FileServer(root.toString).serve("sub")
          yield assertTrue(res.status == Status.NotFound)
        }
      },
      test("404s an empty or blank relative path") {
        ZIO.scoped {
          for
            root <- tempRoot
            a    <- FileServer(root.toString).serve("")
            b    <- FileServer(root.toString).serve("/")
          yield assertTrue(a.status == Status.NotFound, b.status == Status.NotFound)
        }
      },
      test("404s a symlink that escapes the root") {
        ZIO.scoped {
          for
            root    <- tempRoot
            outside <- ZIO.attempt(Files.createTempFile("cortex-fileserver-symlink-", ".txt"))
            _       <- ZIO.attempt(Files.writeString(outside, "secret"))
            _       <- ZIO.addFinalizer(ZIO.attempt(Files.deleteIfExists(outside)).orDie)
            // Some CI filesystems disallow symlink creation — skip the assertion there rather than fail.
            created <- ZIO.attempt(Files.createSymbolicLink(root.resolve("link.txt"), outside)).either
            res     <- FileServer(root.toString).serve("link.txt")
          yield created match
            case Right(_) => assertTrue(res.status == Status.NotFound)
            case Left(_)  => assertTrue(true)
        }
      },
      test("404s everything when the root directory does not exist") {
        for
          tmp <- ZIO.attempt(Files.createTempDirectory("cortex-fileserver-gone-"))
          _   <- ZIO.attempt(Files.delete(tmp))
          fs = FileServer(tmp.toString)
          res <- fs.serve("anything.txt")
        yield assertTrue(!fs.exists, res.status == Status.NotFound)
      }
    ),
    suite("ContentTypes")(
      test("maps frontend bundle extensions") {
        assertTrue(
          ContentTypes.forName("main.js").startsWith("application/javascript"),
          ContentTypes.forName("main.mjs").startsWith("application/javascript"),
          ContentTypes.forName("app.css").startsWith("text/css"),
          ContentTypes.forName("index.html").startsWith("text/html"),
          ContentTypes.forName("font.woff2") == "font/woff2",
          ContentTypes.forName("blob.wasm") == "application/wasm"
        )
      },
      test("maps Cortex asset extensions") {
        assertTrue(
          ContentTypes.forName("diagram.svg") == "image/svg+xml",
          ContentTypes.forName("photo.png") == "image/png",
          ContentTypes.forName("notes.md").startsWith("text/plain"),
          ContentTypes.forName("graph.d2").startsWith("text/plain"),
          ContentTypes.forName("model.dsl").startsWith("text/plain")
        )
      },
      test("is case-insensitive and falls back to octet-stream") {
        assertTrue(
          ContentTypes.forName("PHOTO.PNG") == "image/png",
          ContentTypes.forName("mystery.bin") == "application/octet-stream",
          ContentTypes.forName("no-extension") == "application/octet-stream"
        )
      }
    )
  )

  private val tempRoot: ZIO[Scope, Throwable, Path] =
    ZIO.acquireRelease(ZIO.attempt(Files.createTempDirectory("cortex-fileserver-"))) { root =>
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
      try paths.iterator().asScala.toList.reverse.foreach(path => Files.deleteIfExists(path))
      finally paths.close()
