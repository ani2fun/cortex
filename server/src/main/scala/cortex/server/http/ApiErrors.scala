package cortex.server.http

import cortex.server.auth.AuthFailure
import cortex.server.blogPipeline.BlogFailure
import cortex.server.codeRunPipeline.RunFailure
import cortex.server.cortexPipeline.CortexFailure
import cortex.server.helloPipeline.HelloFailure
import cortex.shared.api.Endpoints.ApiError
import sttp.model.StatusCode

object ApiErrors:

  /**
   * Anything a handler can fail with that the HTTP layer must translate into a status code + envelope. New
   * handler error types are added to this union and to the match in [[toHttp]] — the compiler flags missing
   * cases at the call site.
   */
  type HandlerFailure =
    RunFailure | CortexFailure | HelloFailure | BlogFailure | AuthFailure | RateLimitFailure

  def toHttp(failure: HandlerFailure): (StatusCode, ApiError) = failure match
    case RunFailure.BadInput(error, hint) =>
      StatusCode.BadRequest -> ApiError(error = error, detail = None, hint = hint)
    case RunFailure.PayloadTooLarge(error) =>
      StatusCode.PayloadTooLarge -> ApiError(error = error, detail = None, hint = None)
    case RunFailure.NotConfigured =>
      StatusCode.ServiceUnavailable -> ApiError(
        error = "Code execution is not configured on this server.",
        detail = None,
        hint = Some("Set EXECUTOR_URL to the go-judge sandbox base URL.")
      )
    case RunFailure.BackendFailure(error, detail) =>
      StatusCode.BadGateway -> ApiError(error = error, detail = detail, hint = None)
    case CortexFailure.NotFound =>
      StatusCode.NotFound -> ApiError(error = "Not found", detail = None, hint = None)
    case CortexFailure.IO(detail) =>
      StatusCode.InternalServerError ->
        ApiError(error = "Cortex IO error", detail = Some(detail), hint = None)
    case CortexFailure.IndexInvalid(detail) =>
      StatusCode.InternalServerError ->
        ApiError(error = "Cortex index is invalid", detail = Some(detail), hint = None)
    case HelloFailure.GreetingUnavailable(detail) =>
      StatusCode.ServiceUnavailable ->
        ApiError(error = "Greeting unavailable", detail = detail, hint = None)
    case HelloFailure.RecentUnavailable(detail) =>
      StatusCode.ServiceUnavailable ->
        ApiError(error = "Recent calls unavailable", detail = detail, hint = None)
    case BlogFailure.NotFound =>
      StatusCode.NotFound -> ApiError(error = "Not found", detail = None, hint = None)
    case BlogFailure.IO(detail) =>
      StatusCode.InternalServerError ->
        ApiError(error = "Blog IO error", detail = Some(detail), hint = None)
    case BlogFailure.IndexInvalid(detail) =>
      StatusCode.InternalServerError ->
        ApiError(error = "Blog index is invalid", detail = Some(detail), hint = None)
    case AuthFailure.MissingToken =>
      StatusCode.Unauthorized ->
        ApiError(
          error = "Authentication required",
          detail = Some("This endpoint requires a Bearer token."),
          hint = Some("Sign in via Keycloak and send the access token as `Authorization: Bearer …`.")
        )
    case AuthFailure.InvalidToken(detail) =>
      StatusCode.Unauthorized ->
        ApiError(error = "Invalid bearer token", detail = Some(detail), hint = None)
    case AuthFailure.TokenExpired =>
      StatusCode.Unauthorized ->
        ApiError(
          error = "Bearer token has expired",
          detail = None,
          hint = Some("Refresh your session and retry.")
        )
    case AuthFailure.AuthDisabled =>
      StatusCode.ServiceUnavailable ->
        ApiError(
          error = "Authentication is not enabled on this server",
          detail = None,
          hint = Some("Set AUTH_ENABLED=true and configure KEYCLOAK_ISSUER_URL.")
        )
    case RateLimitFailure.Throttled(retryAfterSec) =>
      StatusCode.TooManyRequests ->
        ApiError(
          error = "Rate limit exceeded",
          detail = Some("Too many /api/run requests."),
          hint = Some(s"Retry after ${retryAfterSec}s, or sign in for a higher quota.")
        )

  /**
   * Seconds to advertise in the `Retry-After` response header. Set only for a throttle failure; every other
   * handler failure yields `None` (no header emitted). Kept separate from [[toHttp]] so that function stays
   * focused on the status + body envelope.
   */
  def retryAfterSeconds(failure: HandlerFailure): Option[Int] = failure match
    case RateLimitFailure.Throttled(retryAfterSec) => Some(retryAfterSec)
    case _                                         => None
