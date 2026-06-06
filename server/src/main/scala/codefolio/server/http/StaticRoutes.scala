package codefolio.server.http

import codefolio.shared.AppRoutes
import zio.*
import zio.http.*

final case class StaticRoutes(routes: Routes[Any, Response], startupInfo: String)

object StaticRoutes:

  def from(staticDir: String): StaticRoutes =
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

    // SPA fallback, derived from AppRoutes.SpaRoutes (single source of truth shared with the client
    // Router — see ADR-0009). A hard reload of e.g. /cortex/distributed-systems/introduction must return
    // index.html so the client-side router can re-resolve the page; routes with nested children also serve
    // index.html for any deeper path.
    //
    // We deliberately don't add a catch-all `/ trailing` wildcard: zio-http's combined routing makes a
    // wildcard greedy enough to swallow all GETs — even ordering tapir ahead doesn't restore precedence —
    // so it would shadow `/api/*` and `/docs`. Typo'd URLs returning 404 is the lesser evil.
    val spaFallback: List[Route[Any, Response]] =
      AppRoutes.SpaRoutes.flatMap { spa =>
        val leaf: Route[Any, Response] = Method.GET / spa.segment -> handler(staticIndex)
        if spa.hasNestedRoutes then
          List(
            leaf,
            Method.GET / spa.segment / trailing ->
              Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (_, _) => staticIndex }
          )
        else List(leaf)
      }

    val routes: Routes[Any, Response] =
      if !fileServer.exists then Routes.empty
      else spaFallback.foldLeft(fixedRoutes)((acc, route) => acc ++ Routes(route))

    val startupInfo =
      if fileServer.exists then s"serving frontend from ${fileServer.root}"
      else s"no frontend bundle at ${fileServer.root} (dev mode — Vite serves the UI on :5173)"

    StaticRoutes(routes, startupInfo)
