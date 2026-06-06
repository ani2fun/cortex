package cortex.server.http

/**
 * Derives a per-client rate-limit key from request headers.
 *
 * Prefers `X-Real-IP`: the k3s nginx ingress sets it to the connecting peer's address and **overwrites** any
 * client-supplied value, so it cannot be spoofed to dodge the limiter. Falls back to the **last** hop of
 * `X-Forwarded-For` — the entry the trusted ingress appended; *earlier* hops are client-controlled when the
 * ingress runs with `use-forwarded-headers`, so the first hop must never be trusted as the key.
 *
 * With neither header present (a direct hit, or local dev) every caller collapses onto a single shared
 * `"unknown"` bucket — acceptable, because the limiter is only active when auth is enabled and "auth enabled
 * + no ingress" is not a combination that occurs in the deployed topology.
 */
object ClientIp:

  /** Trustworthy client key: `X-Real-IP`, else the last `X-Forwarded-For` hop, else `"unknown"`. */
  def key(xRealIp: Option[String], xForwardedFor: Option[String]): String =
    xRealIp
      .map(_.trim)
      .filter(_.nonEmpty)
      .orElse(lastForwardedForHop(xForwardedFor))
      .getOrElse("unknown")

  /** The final hop of an `X-Forwarded-For` chain — the one appended by the trusted proxy. */
  private def lastForwardedForHop(xForwardedFor: Option[String]): Option[String] =
    xForwardedFor
      .map(_.split(",").toList)
      .getOrElse(Nil)
      .map(_.trim)
      .filter(_.nonEmpty)
      .lastOption
