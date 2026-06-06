package cortex.server.http

import cortex.shared.AppRoutes
import zio.*
import zio.http.*
import zio.test.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * Tests that the production server's SPA index.html fallback covers the reserved segments in
 * `AppRoutes.SpaRoutes` (blogs, demo, legacy cortex) plus every enumerated book slug. A hard reload of any
 * SPA path must return index.html so the client router can re-resolve; this spec runs real requests through
 * `StaticRoutes.from(...).routes` to prove the reserved routes are covered, that nested routes (and book
 * chapter paths) serve index.html for deep paths, and that a leaf-only / unknown route does not.
 */
object StaticRoutesSpec extends ZIOSpecDefault:

  private val IndexMarker = "SPA-INDEX-MARKER"
  // Sample book slugs the production fallback enumerates from the content tree.
  private val Books = List("system-design", "languages")

  override def spec: Spec[Any, Throwable] = suite("StaticRoutes")(
    test("serves index.html at the root") {
      ZIO.scoped {
        for
          dir  <- tempDist
          res  <- runGet(StaticRoutes.from(dir.toString, Books).routes, "/")
          body <- res.body.asString
        yield assertTrue(res.status == Status.Ok, body.contains(IndexMarker))
      }
    },
    test("serves index.html for every top-level SPA route in AppRoutes.SpaRoutes") {
      ZIO.scoped {
        for
          dir <- tempDist
          routes = StaticRoutes.from(dir.toString, Books).routes
          checked <- ZIO.foreach(AppRoutes.SpaRoutes) { spa =>
            runGet(routes, s"/${spa.segment}").flatMap { res =>
              res.body.asString.map(body => res.status == Status.Ok && body.contains(IndexMarker))
            }
          }
        yield assertTrue(AppRoutes.SpaRoutes.nonEmpty, checked.forall(identity))
      }
    },
    test("serves index.html for a deep path under a nested SPA route") {
      ZIO.scoped {
        for
          dir <- tempDist
          res <- runGet(
            StaticRoutes.from(dir.toString, Books).routes,
            "/cortex/distributed-systems/introduction"
          )
          body <- res.body.asString
        yield assertTrue(res.status == Status.Ok, body.contains(IndexMarker))
      }
    },
    test("does not serve a deep path under a leaf-only SPA route (demo has no nested routes)") {
      ZIO.scoped {
        for
          dir <- tempDist
          res <- runGet(StaticRoutes.from(dir.toString, Books).routes, "/demo/anything")
        yield assertTrue(res.status == Status.NotFound)
      }
    },
    test("serves index.html for a book chapter path /{book}/{chapter}") {
      ZIO.scoped {
        for
          dir  <- tempDist
          res  <- runGet(StaticRoutes.from(dir.toString, Books).routes, "/system-design/introduction")
          body <- res.body.asString
        yield assertTrue(res.status == Status.Ok, body.contains(IndexMarker))
      }
    },
    test("does not serve a path under an unknown (non-enumerated) book slug") {
      ZIO.scoped {
        for
          dir <- tempDist
          res <- runGet(StaticRoutes.from(dir.toString, Books).routes, "/not-a-book/whatever")
        yield assertTrue(res.status == Status.NotFound)
      }
    },
    test("mounts no routes when the dist directory is absent (dev mode)") {
      for
        tmp <- ZIO.attempt(Files.createTempDirectory("cortex-staticroutes-gone-"))
        _   <- ZIO.attempt(Files.delete(tmp))
        sr = StaticRoutes.from(tmp.toString, Books)
        res <- runGet(sr.routes, "/cortex")
      yield assertTrue(res.status == Status.NotFound, sr.startupInfo.contains("dev mode"))
    }
  )

  // StaticRoutes' handlers never fail (they produce a Response directly), and `runZIO` turns an unmatched
  // route into a 404 Response in the success channel — so this is an infallible effect. `runZIO` allocates
  // request-scoped resources, so it runs inside its own `ZIO.scoped`.
  private def runGet(routes: Routes[Any, Response], path: String): ZIO[Any, Nothing, Response] =
    ZIO.scoped(routes.runZIO(get(path)))

  private def get(path: String): Request =
    Request.get(
      URL.decode(path).toOption.getOrElse(throw new IllegalArgumentException(s"bad path: $path"))
    )

  private val tempDist: ZIO[Scope, Throwable, Path] =
    ZIO.acquireRelease(
      ZIO.attempt {
        val dir = Files.createTempDirectory("cortex-staticroutes-")
        Files.writeString(
          dir.resolve(AppRoutes.IndexHtml),
          s"<!doctype html><title>$IndexMarker</title>",
          StandardCharsets.UTF_8
        )
        dir
      }
    )(dir => ZIO.attempt(deleteRecursively(dir)).orDie)

  private def deleteRecursively(root: Path): Unit =
    if Files.exists(root) then
      val paths = Files.walk(root)
      try paths.iterator().asScala.toList.reverse.foreach(path => Files.deleteIfExists(path))
      finally paths.close()
