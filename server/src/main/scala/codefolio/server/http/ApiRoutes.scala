package codefolio.server.http

import codefolio.server.auth.{Auth, AuthFailure, VerifiedClaims}
import codefolio.server.blogPipeline.BlogPipeline
import codefolio.server.codeRunPipeline.CodeRunPipeline
import codefolio.server.config.AppConfig
import codefolio.server.cortexPipeline.CortexPipeline
import codefolio.server.helloPipeline.HelloPipeline
import codefolio.shared.api.Endpoints
import codefolio.shared.api.Endpoints.{
  ApiError,
  AuthConfig,
  BlogIndex,
  BlogPostPayload,
  ChapterPayload,
  CortexIndex,
  Greeting,
  Quota,
  RecentCalls,
  RunRequest,
  RunResponse,
  UserInfo
}
import codefolio.shared.api.EndpointsJsonSerdes.*
import codefolio.shared.api.EndpointsSchemas.*
import sttp.model.StatusCode
import sttp.tapir.PublicEndpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.*
import zio.http.{Response, Routes}

object ApiRoutes:

  private val RecentLimit  = 10
  private val BearerPrefix = "Bearer "

  // Header names read off the request. Authorization carries the bearer token;
  // X-Real-IP / X-Forwarded-For yield the per-IP rate-limit key (set by the
  // k3s ingress — X-Real-IP is the trusted one, see ClientIp).
  private val AuthorizationHeader = "Authorization"
  private val RealIpHeader        = "X-Real-IP"
  private val ForwardedForHeader  = "X-Forwarded-For"
  private val RetryAfterHeader    = "Retry-After"

  def routes(
      appConfig: AppConfig,
      helloPipeline: HelloPipeline,
      codeRun: CodeRunPipeline,
      cortex: CortexPipeline,
      blog: BlogPipeline,
      auth: Auth,
      rateLimiter: RateLimiter
  ): Routes[Any, Response] =
    val endpoints =
      serverEndpoints(appConfig, helloPipeline, codeRun, cortex, blog, auth, rateLimiter)
    val swaggerEndpoints =
      SwaggerInterpreter()
        .fromServerEndpoints[Task](endpoints, "Codefolio API", "0.1.0")

    ZioHttpInterpreter().toHttp(endpoints ++ swaggerEndpoints)

  // The uniform error output for every fallible endpoint: an HTTP status code chosen at runtime, an
  // optional `Retry-After` header (populated only for 429s), and a JSON `ApiError` envelope.
  private val apiErrorOut =
    statusCode and header[Option[Int]](RetryAfterHeader) and jsonBody[ApiError]

  /**
   * Collapse a `HandlerFailure` into the `(status, retry-after, envelope)` triple `apiErrorOut` expects.
   * `ApiErrors.toHttp` owns the status + body; `ApiErrors.retryAfterSeconds` owns the header.
   */
  private def toResponse(
      failure: ApiErrors.HandlerFailure
  ): (StatusCode, Option[Int], ApiError) =
    val (status, envelope) = ApiErrors.toHttp(failure)
    (status, ApiErrors.retryAfterSeconds(failure), envelope)

  /** Pull the raw token out of an `Authorization: Bearer <token>` header (case-insensitive prefix). */
  private def bearerToken(authHeader: Option[String]): Option[String] =
    authHeader
      .map(_.trim)
      .collect {
        case h if h.regionMatches(true, 0, BearerPrefix, 0, BearerPrefix.length) =>
          h.substring(BearerPrefix.length).trim
      }
      .filter(_.nonEmpty)

  /**
   * Wire a derived endpoint to its pipeline logic: attach the uniform error output and the `HandlerFailure →
   * toResponse` mapping in one place. An endpoint built through this helper cannot ship without its error
   * plumbing (ADR-0012).
   */
  private def handlerEndpoint[I, O](
      base: PublicEndpoint[I, Unit, O, Any]
  )(logic: I => IO[ApiErrors.HandlerFailure, O]): ZServerEndpoint[Any, Any] =
    base
      .errorOut(apiErrorOut)
      .zServerLogic(input => logic(input).mapError(toResponse))

  private def serverEndpoints(
      appConfig: AppConfig,
      helloPipeline: HelloPipeline,
      codeRun: CodeRunPipeline,
      cortex: CortexPipeline,
      blog: BlogPipeline,
      auth: Auth,
      rateLimiter: RateLimiter
  ): List[ZServerEndpoint[Any, Any]] =

    // Endpoints are defined here rather than reusing the generated `Endpoints.*` values directly so the
    // server can choose the status code at runtime while still using generated request/response case
    // classes and codecs. `handlerEndpoint` / `withOptionalAuth` / `authedEndpoint` bundle the error +
    // auth wiring so it can't be forgotten. Health and auth-config stay on plain endpoints because they
    // never fail (degradation is part of the success body, not the error channel).

    /**
     * Wire an endpoint that accepts *optional* auth. The handler receives the verified claims (if any) plus
     * the per-IP rate-limit key. A malformed/expired token still fails 401 — sending garbage isn't the same
     * as sending nothing.
     */
    def withOptionalAuth[I, O](
        base: PublicEndpoint[I, Unit, O, Any]
    )(
        logic: (Option[VerifiedClaims], String, I) => IO[ApiErrors.HandlerFailure, O]
    ): ZServerEndpoint[Any, Any] =
      base
        .in(header[Option[String]](AuthorizationHeader))
        .in(header[Option[String]](RealIpHeader))
        .in(header[Option[String]](ForwardedForHeader))
        .errorOut(apiErrorOut)
        .zServerLogic { case (input, authHeader, realIp, forwardedFor) =>
          auth
            .verifyOptional(bearerToken(authHeader))
            .flatMap(claims => logic(claims, ClientIp.key(realIp, forwardedFor), input))
            .mapError(toResponse)
        }

    /** Wire a no-input endpoint that *requires* a valid bearer token. */
    def authedEndpoint[O](
        base: PublicEndpoint[Unit, Unit, O, Any]
    )(logic: VerifiedClaims => IO[ApiErrors.HandlerFailure, O]): ZServerEndpoint[Any, Any] =
      base
        .in(header[Option[String]](AuthorizationHeader))
        .errorOut(apiErrorOut)
        .zServerLogic { authHeader =>
          val effect: IO[ApiErrors.HandlerFailure, O] =
            bearerToken(authHeader) match
              case None        => ZIO.fail(AuthFailure.MissingToken)
              case Some(token) => auth.verify(token).flatMap(logic)
          effect.mapError(toResponse)
        }

    val helloEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "hello").out(jsonBody[Greeting])
    )(_ => helloPipeline.greet)

    val recentEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "recent").out(jsonBody[RecentCalls])
    )(_ => helloPipeline.recent(RecentLimit))

    val healthEndpoint: ZServerEndpoint[Any, Any] =
      Endpoints.getHealth.zServerLogic(_ => helloPipeline.health)

    // /api/run — optional auth. Authenticated callers are metered per `sub`; anonymous callers per IP.
    // Either bucket can fail `Throttled` → 429 + Retry-After before the request reaches the pipeline.
    val runEndpoint = withOptionalAuth(
      endpoint.post.in("api" / "run").in(jsonBody[RunRequest]).out(jsonBody[RunResponse])
    ) { (claims, ipKey, req) =>
      val gate: IO[ApiErrors.HandlerFailure, Quota] =
        claims match
          case Some(c) => rateLimiter.consumeAuthenticated(c.sub)
          case None    => rateLimiter.consumeAnonymous(ipKey)
      gate *> codeRun.run(req)
    }

    // /api/auth/config — public. Tells the SPA whether to boot keycloak-js, and with what coordinates.
    val authConfigEndpoint: ZServerEndpoint[Any, Any] =
      endpoint.get
        .in("api" / "auth" / "config")
        .out(jsonBody[AuthConfig])
        .zServerLogic { _ =>
          val a = appConfig.auth
          val payload =
            if a.enabled then
              AuthConfig(
                enabled = true,
                issuerUrl = Some(a.issuerUrl),
                realm = Some(a.realm),
                clientId = Some(a.clientId)
              )
            else AuthConfig(enabled = false, issuerUrl = None, realm = None, clientId = None)
          ZIO.succeed(payload)
        }

    // /api/me — requires a valid bearer. Projects the JWT claims + the caller's current run quota.
    val meEndpoint = authedEndpoint(
      endpoint.get.in("api" / "me").out(jsonBody[UserInfo])
    ) { claims =>
      rateLimiter
        .peekAuthenticated(claims.sub)
        .map(quota =>
          UserInfo(
            sub = claims.sub,
            preferredUsername = claims.preferredUsername,
            name = claims.name,
            email = claims.email,
            quota = quota
          )
        )
    }

    val cortexIndexEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "cortex" / "index").out(jsonBody[CortexIndex])
    )(_ => cortex.index)

    val cortexChapterEndpoint = handlerEndpoint(
      endpoint.get
        .in("api" / "cortex" / path[String]("book") / path[String]("chapter"))
        .out(jsonBody[ChapterPayload])
    ) { case (book, chapter) => cortex.chapter(book, chapter) }

    val blogIndexEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "blogs" / "index").out(jsonBody[BlogIndex])
    )(_ => blog.index)

    val blogPostEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "blogs" / path[String]("slug")).out(jsonBody[BlogPostPayload])
    )(slug => blog.post(slug))

    List(
      helloEndpoint,
      recentEndpoint,
      healthEndpoint,
      runEndpoint,
      authConfigEndpoint,
      meEndpoint,
      cortexIndexEndpoint,
      cortexChapterEndpoint,
      blogIndexEndpoint,
      blogPostEndpoint
    )
