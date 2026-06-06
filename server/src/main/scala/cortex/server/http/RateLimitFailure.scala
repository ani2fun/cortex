package cortex.server.http

/**
 * Raised when a caller exceeds their `/api/run` rate-limit window.
 *
 * Joins the `HandlerFailure` union in [[ApiErrors]]; mapped to HTTP 429 plus a `Retry-After` header carrying
 * [[RateLimitFailure.Throttled.retryAfterSec]].
 */
sealed trait RateLimitFailure extends Product with Serializable

object RateLimitFailure:
  /** The current window is full. `retryAfterSec` is the whole-second wait until it resets (always ≥ 1). */
  final case class Throttled(retryAfterSec: Int) extends RateLimitFailure
