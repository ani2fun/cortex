package codefolio.server

import codefolio.server.auth.Auth
import codefolio.server.blogPipeline.BlogPipeline
import codefolio.server.codeRunPipeline.CodeRunPipeline
import codefolio.server.config.AppConfig
import codefolio.server.cortexPipeline.CortexPipeline
import codefolio.server.helloPipeline.HelloPipeline
import codefolio.server.http.{ApiRoutes, CortexAssetRoutes, LikeC4ProxyRoutes, RateLimiter, StaticRoutes}
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
    AppConfig & HelloPipeline & CodeRunPipeline & CortexPipeline & BlogPipeline & Auth & RateLimiter,
    Nothing,
    HttpApp
  ] =
    ZLayer.fromFunction(HttpAppLive(_, _, _, _, _, _, _))

final private class HttpAppLive(
    cfg: AppConfig,
    helloPipeline: HelloPipeline,
    codeRun: CodeRunPipeline,
    cortex: CortexPipeline,
    blog: BlogPipeline,
    auth: Auth,
    rateLimiter: RateLimiter
) extends HttpApp:

  private val apiRoutes =
    ApiRoutes.routes(cfg, helloPipeline, codeRun, cortex, blog, auth, rateLimiter)

  private val cortexAssetRoutes = CortexAssetRoutes.from(cfg.cortex.root)
  private val likec4Routes      = LikeC4ProxyRoutes.from(cfg.likec4.upstreamUrl)
  private val staticRoutes      = StaticRoutes.from(cfg.staticDir)

  override def serve: Task[Unit] =
    ZIO.logInfo(s"Starting server on port ${cfg.port}; ${staticRoutes.startupInfo}") *>
      Server
        .serve(apiRoutes ++ cortexAssetRoutes ++ likec4Routes ++ staticRoutes.routes)
        .provide(
          ZLayer.succeed(Server.Config.default.port(cfg.port)),
          Server.live
        )
        .unit
