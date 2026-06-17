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

  override def serve: Task[Unit] =
    ZIO.logInfo(s"Starting server on port ${cfg.port}; ${staticRoutes.startupInfo}") *>
      Server
        .serve(apiRoutes ++ cortexAssetRoutes ++ likec4Routes ++ tutorRoutes ++ staticRoutes.routes)
        .provide(
          // Cap request bodies at 512 KiB. zio-http's default is 100 KiB — too tight for a max-size
          // BYOK coach turn (code 64K + coachReply 64K + runResult 16K + text 16K ≈ 165 KiB), which
          // would 413 at the edge; 512 KiB clears that yet stays far under OOM territory (oversize is
          // a cheap pre-decode 413). Mirrors the tutor's coach_max_request_bytes.
          ZLayer.succeed(Server.Config.default.port(cfg.port).disableRequestStreaming(512 * 1024)),
          Server.live
        )
        .unit
