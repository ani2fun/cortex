package cortex.server

import cortex.server.auth.Auth
import cortex.server.blogPipeline.BlogPipeline
import cortex.server.codeRunPipeline.CodeRunPipeline
import cortex.server.config.AppConfig
import cortex.server.cortexPipeline.CortexPipeline
import cortex.server.helloPipeline.HelloPipeline
import cortex.server.http.{
  ApiRoutes,
  CortexAssetRoutes,
  LikeC4ProxyRoutes,
  RateLimiter,
  StaticRoutes,
  TutorProxyRoutes
}
import cortex.server.coachPipeline.CoachSavePipeline
import cortex.server.submissionPipeline.SubmissionPipeline
import zio.*
import zio.http.*

/**
 * Thin HTTP-binding layer.
 *
 * API endpoint construction, error mapping, and production static-file serving live in `server.http`.
 * `HttpApp` keeps only the runtime composition: build the route tables, log what is mounted, bind the port,
 * and block until shutdown.
 */
trait HttpApp:
  def serve: Task[Unit]

object HttpApp:

  val live: ZLayer[
    AppConfig & HelloPipeline & CodeRunPipeline & CortexPipeline & BlogPipeline & Auth & RateLimiter &
      SubmissionPipeline & CoachSavePipeline,
    Nothing,
    HttpApp
  ] =
    ZLayer.fromFunction(HttpAppLive(_, _, _, _, _, _, _, _, _))

final private class HttpAppLive(
    cfg: AppConfig,
    helloPipeline: HelloPipeline,
    codeRun: CodeRunPipeline,
    cortex: CortexPipeline,
    blog: BlogPipeline,
    auth: Auth,
    rateLimiter: RateLimiter,
    submissions: SubmissionPipeline,
    coachSave: CoachSavePipeline
) extends HttpApp:

  private val apiRoutes =
    ApiRoutes.routes(cfg, helloPipeline, codeRun, cortex, blog, auth, rateLimiter, submissions, coachSave)

  private val cortexAssetRoutes = CortexAssetRoutes.from(cfg.cortex.root)
  private val likec4Routes      = LikeC4ProxyRoutes.from(cfg.likec4.upstreamUrl)
  // Same-origin reverse proxy for the now-internal-only cortex-tutor (ClusterIP, no public Ingress). The
  // SPA calls /tutor/* on this origin; we forward to the in-cluster tutor at cfg.tutorBaseUrl.
  private val tutorRoutes = TutorProxyRoutes.from(cfg.tutorBaseUrl)
  // Enumerate book slugs from the content tree so the production SPA fallback serves index.html for
  // `/{book}/{chapter}` hard reloads without a greedy wildcard that would shadow /api and /docs.
  private val staticRoutes = StaticRoutes.from(cfg.staticDir, StaticRoutes.bookSlugsFromDir(cfg.cortex.root))

  // Fill the Cortex + Blog index caches at boot rather than lazily on the first request, so the first
  // visitor after a redeploy doesn't pay the content-tree walk. Forked in `serve` so it never blocks the
  // bind; failures are logged and ignored — the lazy path still rebuilds on demand if this loses a race.
  private val prewarmIndexes: UIO[Unit] =
    (cortex.index.unit.catchAll(e => ZIO.logWarning(s"Cortex index pre-warm failed: $e")) <&>
      blog.index.unit.catchAll(e => ZIO.logWarning(s"Blog index pre-warm failed: $e"))).unit

  override def serve: Task[Unit] =
    ZIO.logInfo(s"Starting server on port ${cfg.port}; ${staticRoutes.startupInfo}") *>
      prewarmIndexes.forkDaemon *>
      Server
        .serve(apiRoutes ++ cortexAssetRoutes ++ likec4Routes ++ tutorRoutes ++ staticRoutes.routes)
        .provide(
          // Cap request bodies at 512 KiB. zio-http's default is 100 KiB — too tight for a max-size
          // BYOK coach turn (code 64K + coachReply 64K + runResult 16K + text 16K ≈ 165 KiB), which
          // would 413 at the edge; 512 KiB clears that yet stays far under OOM territory (oversize is
          // a cheap pre-decode 413). Mirrors the tutor's coach_max_request_bytes.
          //
          // responseCompression: gzip/deflate responses over ~1 KiB. The frontend ships a multi-MB
          // Scala.js bundle that crosses the WireGuard tunnel + home uplink uncompressed today;
          // compressing at the origin shrinks the bytes *before* that bottleneck (not just edge→user).
          ZLayer.succeed(
            Server.Config.default
              .port(cfg.port)
              .disableRequestStreaming(512 * 1024)
              .responseCompression(
                Server.Config.ResponseCompressionConfig(
                  contentThreshold = 1024,
                  options = IndexedSeq(
                    Server.Config.CompressionOptions.gzip(),
                    Server.Config.CompressionOptions.deflate()
                  )
                )
              )
          ),
          Server.live
        )
        .unit
