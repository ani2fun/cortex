package cortex.server.http

import cortex.shared.AppRoutes
import zio.*
import zio.http.*

final case class StaticRoutes(routes: Routes[Any, Response], startupInfo: String)

object StaticRoutes:

  def from(staticDir: String, bookSlugs: List[String]): StaticRoutes =
    val fileServer = FileServer(staticDir)

    // index.html — served at `/`, at `/index.html`, and as the SPA fallback below.
    def staticIndex: ZIO[Any, Nothing, Response] =
      fileServer.serve(AppRoutes.IndexHtml)

    // A `trailing`-captured path is relative to `prefix`; FileServer resolves it under the static root
    // and enforces path-traversal containment.
    def trailingFileHandler(prefix: String): Handler[Any, Response, (zio.http.Path, Request), Response] =
      Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (path, _) =>
        fileServer.serve(s"$prefix/${path.encode.stripPrefix("/")}")
      }

    // Vite emits index.html + assets/*; img/ and certificates/ are copied from client/public/ at the
    // dist root; the CV is a single fixed file.
    val fixedRoutes: Routes[Any, Response] = Routes(
      Method.GET / Root                              -> handler(staticIndex),
      Method.GET / AppRoutes.IndexHtml               -> handler(staticIndex),
      Method.GET / AppRoutes.Assets / trailing       -> trailingFileHandler(AppRoutes.Assets),
      Method.GET / AppRoutes.Images / trailing       -> trailingFileHandler(AppRoutes.Images),
      Method.GET / AppRoutes.Certificates / trailing -> trailingFileHandler(AppRoutes.Certificates),
      Method.GET / AppRoutes.CvFile                  -> handler(fileServer.serve(AppRoutes.CvFile)),
      Method.GET / AppRoutes.SilentCheckSso          -> handler(fileServer.serve(AppRoutes.SilentCheckSso))
    )

    // index.html SPA fallback. A hard reload of any SPA path must return index.html so the client router can
    // re-resolve. The Cortex book index is `/` (a fixed route above); chapters live at `/{book}/{chapter}`,
    // so every book slug needs a `/{slug}` leaf + a `/{slug}/<rest>` nested fallback. The reserved non-book
    // segments (blogs, demo, and the legacy `/cortex/...` prefix) come from AppRoutes.SpaRoutes. Book slugs
    // are filesystem-driven, so they're enumerated at startup rather than baked into the shared constant.
    //
    // We deliberately don't add a catch-all `/ trailing` wildcard: zio-http's combined routing makes a
    // wildcard greedy enough to swallow all GETs — even ordering tapir ahead doesn't restore precedence —
    // so it would shadow `/api/*` and `/docs`. Enumerating reserved segments + book slugs avoids that.
    def fallbackFor(segment: String, nested: Boolean): List[Route[Any, Response]] =
      val leaf: Route[Any, Response] = Method.GET / segment -> handler(staticIndex)
      if nested then
        List(
          leaf,
          Method.GET / segment / trailing ->
            Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (_, _) => staticIndex }
        )
      else List(leaf)

    val spaFallback: List[Route[Any, Response]] =
      AppRoutes.SpaRoutes.flatMap(spa => fallbackFor(spa.segment, spa.hasNestedRoutes)) ++
        bookSlugs.flatMap(slug => fallbackFor(slug, nested = true))

    val routes: Routes[Any, Response] =
      if !fileServer.exists then Routes.empty
      else spaFallback.foldLeft(fixedRoutes)((acc, route) => acc ++ Routes(route))

    val startupInfo =
      if fileServer.exists then s"serving frontend from ${fileServer.root}"
      else s"no frontend bundle at ${fileServer.root} (dev mode — Vite serves the UI on :5173)"

    StaticRoutes(routes, startupInfo)

  /**
   * Immediate book directories under the Cortex content root, numeric prefix stripped — the book slugs used
   * in `/{book}/{chapter}` URLs. Drives the production SPA fallback so a hard reload of a chapter URL returns
   * index.html. Returns empty when the directory is absent (e.g. dev mode, where Vite serves the SPA and this
   * fallback is unused).
   */
  def bookSlugsFromDir(cortexRoot: String): List[String] =
    Option(new java.io.File(cortexRoot).listFiles)
      .map(_.toList.filter(_.isDirectory).map(_.getName).filterNot(_.startsWith(".")))
      .getOrElse(Nil)
      .map(_.replaceFirst("^\\d+[-_.]", ""))
      .sorted
