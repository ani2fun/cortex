package cortex.server

import cortex.server.auth.Auth
import cortex.server.blogPipeline.BlogPipeline
import cortex.server.codeRunPipeline.CodeRunPipeline
import cortex.server.config.AppConfig
import cortex.server.cortexPipeline.CortexPipeline
import cortex.server.helloPipeline.HelloPipeline
import cortex.server.http.{ApiRoutes, CortexAssetRoutes, LikeC4ProxyRoutes, RateLimiter, StaticRoutes}
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
      SubmissionPipeline,
    Nothing,
    HttpApp
  ] =
    ZLayer.fromFunction(HttpAppLive(_, _, _, _, _, _, _, _))

final private class HttpAppLive(
    cfg: AppConfig,
    helloPipeline: HelloPipeline,
    codeRun: CodeRunPipeline,
    cortex: CortexPipeline,
    blog: BlogPipeline,
    auth: Auth,
    rateLimiter: RateLimiter,
    submissions: SubmissionPipeline
) extends HttpApp:

  private val apiRoutes =
    ApiRoutes.routes(cfg, helloPipeline, codeRun, cortex, blog, auth, rateLimiter, submissions)

  private val cortexAssetRoutes = CortexAssetRoutes.from(cfg.cortex.root)
  private val likec4Routes      = LikeC4ProxyRoutes.from(cfg.likec4.upstreamUrl)
  // Enumerate book slugs from the content tree so the production SPA fallback serves index.html for
  // `/{book}/{chapter}` hard reloads without a greedy wildcard that would shadow /api and /docs.
  private val staticRoutes = StaticRoutes.from(cfg.staticDir, StaticRoutes.bookSlugsFromDir(cfg.cortex.root))

  override def serve: Task[Unit] =
    ZIO.logInfo(s"Starting server on port ${cfg.port}; ${staticRoutes.startupInfo}") *>
      Server
        .serve(apiRoutes ++ cortexAssetRoutes ++ likec4Routes ++ staticRoutes.routes)
        .provide(
          ZLayer.succeed(Server.Config.default.port(cfg.port)),
          Server.live
        )
        .unit
