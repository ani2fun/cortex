package cortex.server.http

import cortex.server.auth.{Auth, AuthFailure, VerifiedClaims}
import cortex.server.blogPipeline.BlogPipeline
import cortex.server.codeRunPipeline.CodeRunPipeline
import cortex.server.config.AppConfig
import cortex.server.cortexPipeline.CortexPipeline
import cortex.server.helloPipeline.HelloPipeline
import cortex.server.submissionPipeline.SubmissionPipeline
import cortex.shared.api.Endpoints
import cortex.shared.api.Endpoints.{
  ApiError,
  AuthConfig,
  BlogIndex,
  BlogPostPayload,
  ChapterPayload,
  CortexIndex,
  DeleteSubmissionsResponse,
  Greeting,
  ListSubmissionsResponse,
  Quota,
  RecentCalls,
  RunRequest,
  RunResponse,
  SubmitRequest,
  SubmitResponse,
  UserInfo
}
import cortex.shared.api.EndpointsJsonSerdes.*
import cortex.shared.api.EndpointsSchemas.*
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

  // Same-origin base path the SPA uses to reach the cortex-tutor reverse proxy (TutorProxyRoutes).
  private val TutorProxyPath = "/tutor"

  /**
   * What `/api/auth/config` advertises to the SPA as the tutor base URL.
   *
   * cortex-tutor lost its public Ingress and is now a ClusterIP, so the browser can't reach its in-cluster
   * DNS directly — it goes through this backend's same-origin reverse proxy at `/tutor`. We therefore
   * advertise the RELATIVE path `"/tutor"` (never the raw internal DNS) whenever a tutor is configured, and
   * `None` when it isn't (so the coach UI degrades exactly as before). `configuredTutorBaseUrl` is
   * `AppConfig.tutorBaseUrl` — the internal DNS that stays server-side as the proxy target.
   */
  private[http] def advertisedTutorBaseUrl(configuredTutorBaseUrl: Option[String]): Option[String] =
    configuredTutorBaseUrl.map(_ => TutorProxyPath)

  def routes(
      appConfig: AppConfig,
      helloPipeline: HelloPipeline,
      codeRun: CodeRunPipeline,
      cortex: CortexPipeline,
      blog: BlogPipeline,
      auth: Auth,
      rateLimiter: RateLimiter,
      submissions: SubmissionPipeline
  ): Routes[Any, Response] =
    val endpoints =
      serverEndpoints(appConfig, helloPipeline, codeRun, cortex, blog, auth, rateLimiter, submissions)
    val swaggerEndpoints =
      SwaggerInterpreter()
        .fromServerEndpoints[Task](endpoints, "Cortex API", "0.1.0")

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
      rateLimiter: RateLimiter,
      submissions: SubmissionPipeline
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

    /** Like [[authedEndpoint]], for endpoints that also carry a request input (e.g. a JSON body). */
    def authedInputEndpoint[I, O](
        base: PublicEndpoint[I, Unit, O, Any]
    )(logic: (VerifiedClaims, I) => IO[ApiErrors.HandlerFailure, O]): ZServerEndpoint[Any, Any] =
      base
        .in(header[Option[String]](AuthorizationHeader))
        .errorOut(apiErrorOut)
        .zServerLogic { case (input, authHeader) =>
          val effect: IO[ApiErrors.HandlerFailure, O] =
            bearerToken(authHeader) match
              case None        => ZIO.fail(AuthFailure.MissingToken)
              case Some(token) => auth.verify(token).flatMap(claims => logic(claims, input))
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
          // Advertise the same-origin proxy path "/tutor" (not the raw in-cluster DNS, which the browser
          // can't resolve). `appConfig.tutorBaseUrl` — the internal DNS — stays server-side as the
          // TutorProxyRoutes target. See `advertisedTutorBaseUrl`.
          val tutorBaseUrl = advertisedTutorBaseUrl(appConfig.tutorBaseUrl)
          val payload =
            if a.enabled then
              AuthConfig(
                enabled = true,
                issuerUrl = Some(a.issuerUrl),
                realm = Some(a.realm),
                clientId = Some(a.clientId),
                tutorBaseUrl = tutorBaseUrl
              )
            else
              AuthConfig(
                enabled = false,
                issuerUrl = None,
                realm = None,
                clientId = None,
                tutorBaseUrl = tutorBaseUrl
              )
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
        // The hierarchical chapter slug rides in the `path` QUERY param, not a URL path segment, so
        // its `/` separators never need %2F-encoding (which proxies/servers handle inconsistently).
        .in("api" / "cortex" / path[String]("book") / "chapter")
        .in(query[String]("path"))
        .out(jsonBody[ChapterPayload])
    ) { case (book, chapterPath) => cortex.chapter(book, chapterPath) }

    val blogIndexEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "blogs" / "index").out(jsonBody[BlogIndex])
    )(_ => blog.index)

    val blogPostEndpoint = handlerEndpoint(
      endpoint.get.in("api" / "blogs" / path[String]("slug")).out(jsonBody[BlogPostPayload])
    )(slug => blog.post(slug))

    // /api/submissions — auth required; the allowlist gate lives inside the pipeline (403 with
    // request-access instructions). One submit = N executor runs, so it consumes the caller's
    // authenticated hourly bucket like a single /api/run would.
    val submitEndpoint = authedInputEndpoint(
      endpoint.post.in("api" / "submissions").in(jsonBody[SubmitRequest]).out(jsonBody[SubmitResponse])
    ) { (claims, req) =>
      rateLimiter.consumeAuthenticated(claims.sub) *> submissions.submit(claims, req)
    }

    // Self-service erasure: any authenticated caller may wipe their own stored submissions.
    val deleteSubmissionsEndpoint = authedEndpoint(
      endpoint.delete.in("api" / "submissions").out(jsonBody[DeleteSubmissionsResponse])
    )(claims => submissions.deleteAll(claims))

    // List the caller's submissions for one problem — book + hierarchical chapter as query params,
    // same addressing as GET /api/cortex/{book}/chapter. Powers the workbench Submissions tab.
    // Wired inline rather than via authedInputEndpoint: two query inputs + the auth header flatten to a
    // 3-tuple (tapir's ParamConcat), which the (input, authHeader) helper can't destructure.
    val listSubmissionsEndpoint: ZServerEndpoint[Any, Any] =
      endpoint.get
        .in("api" / "submissions")
        .in(query[String]("book"))
        .in(query[String]("chapter"))
        .in(header[Option[String]](AuthorizationHeader))
        .out(jsonBody[ListSubmissionsResponse])
        .errorOut(apiErrorOut)
        .zServerLogic { case (book, chapter, authHeader) =>
          val effect: IO[ApiErrors.HandlerFailure, ListSubmissionsResponse] =
            bearerToken(authHeader) match
              case None        => ZIO.fail(AuthFailure.MissingToken)
              case Some(token) => auth.verify(token).flatMap(c => submissions.list(c, book, chapter))
          effect.mapError(toResponse)
        }

    // Per-row erasure — delete one of the caller's own submissions (the Submissions tab trash).
    val deleteOneSubmissionEndpoint = authedInputEndpoint(
      endpoint.delete.in("api" / "submissions" / path[Long]("id")).out(jsonBody[DeleteSubmissionsResponse])
    )((claims, id) => submissions.deleteOne(claims, id))

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
      blogPostEndpoint,
      submitEndpoint,
      deleteSubmissionsEndpoint,
      listSubmissionsEndpoint,
      deleteOneSubmissionEndpoint
    )
