package cortex.server.auth

/**
 * Why an attempt to verify a request's bearer token failed.
 *
 * Joins the `HandlerFailure` union in [[cortex.server.http.ApiErrors]] alongside the per-pipeline failure
 * types (`RunFailure`, `CortexFailure`, …). The HTTP layer is the only place that maps these to status codes
 * — pipelines never see an `AuthFailure` because the auth check runs *before* dispatch (per ADR-0012 shape: a
 * thin endpoint wrapper short-circuits with the failure if verification doesn't pass).
 *
 * `AuthDisabled` is a different shape of "failed" than the others: it means the operator turned auth off in
 * config (typically dev mode). Endpoints that require auth turn it into a 503 ("Auth is not enabled on this
 * server"); endpoints that *optionally* take auth treat it as "no claims" and proceed anonymously.
 */
sealed trait AuthFailure extends Product with Serializable

object AuthFailure:
  /** Endpoint requires a bearer token; the request didn't carry one. 401. */
  case object MissingToken extends AuthFailure

  /** Token's signature, issuer, audience, or shape didn't validate. 401. */
  final case class InvalidToken(detail: String) extends AuthFailure

  /** Token decoded fine but its `exp` claim is in the past. 401. */
  case object TokenExpired extends AuthFailure

  /**
   * The server has `cortex.auth.enabled = false`. Returned by [[Auth.verify]] (not
   * [[Auth.verifyOptional]], which short-circuits to `None` instead). Endpoints that *require* auth turn this
   * into a 503 in `ApiErrors.toHttp`; endpoints that don't require it never see this case.
   */
  case object AuthDisabled extends AuthFailure
