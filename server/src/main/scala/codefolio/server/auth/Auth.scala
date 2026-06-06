package codefolio.server.auth

import codefolio.server.config.AuthConfig
import zio.*

/**
 * Internal seam: the actual JWT-validation primitive. Lives behind the public [[Auth]] facade. Live impl is
 * [[KeycloakAuthBackend]]; tests wire [[MockAuthBackend]] in its place.
 *
 * Package-private — callers always go through [[Auth]] (which adds the `enabled` short-circuit and the
 * `verifyOptional` ergonomics).
 */
private[auth] trait AuthBackend:
  def verify(token: String): IO[AuthFailure, VerifiedClaims]

/**
 * Public auth-verification facade for the HTTP layer. Two entry points:
 *
 *   - [[verify]] for endpoints that *require* a bearer (returns `AuthFailure.AuthDisabled` when auth is off
 *     in config — the HTTP layer maps that to 503).
 *   - [[verifyOptional]] for endpoints that accept anonymous *or* authenticated callers. When auth is
 *     disabled in config OR no token is sent, returns `None`. A *malformed or expired* token still returns a
 *     failure — sending garbage isn't the same as sending nothing.
 *
 * Pipelines never see [[Auth]] directly; the route wrappers in `server/http/ApiRoutes.scala` consume it and
 * thread `Option[VerifiedClaims]` (or short-circuit with the failure) into endpoint logic.
 */
trait Auth:
  def verify(token: String): IO[AuthFailure, VerifiedClaims]
  def verifyOptional(maybeToken: Option[String]): IO[AuthFailure, Option[VerifiedClaims]]

object Auth:

  /**
   * Direct construction for tests. Production wires via [[live]].
   *
   * `enabled = false` makes [[verify]] always return `AuthFailure.AuthDisabled` and [[verifyOptional]] always
   * return `None`, regardless of what the backend says. The backend can be a stub in that case.
   */
  def fromBackend(enabled: Boolean, backend: AuthBackend): Auth = AuthLive(enabled, backend)

  /**
   * Resource-free layer: when `enabled = true`, builds a [[KeycloakAuthBackend]] from the configured issuer
   * URL + audience. JWKS isn't fetched at boot — Nimbus' cached `JWKSource` lazily pulls on the first verify
   * call, which keeps server boot order Keycloak-independent.
   *
   * When `enabled = false`, the backend is replaced with a sentinel that fails every call with
   * `AuthFailure.AuthDisabled` — [[AuthLive.verify]] short-circuits before calling it anyway, but the slot
   * has to be filled to satisfy the type.
   */
  val live: ZLayer[AuthConfig, Throwable, Auth] = ZLayer.fromZIO {
    ZIO.service[AuthConfig].map { cfg =>
      val backend = if cfg.enabled then KeycloakAuthBackend.make(cfg) else DisabledBackend
      AuthLive(cfg.enabled, backend)
    }
  }

  /** Stand-in for the `AuthBackend` slot when auth is config-disabled. Never invoked in practice. */
  private val DisabledBackend: AuthBackend =
    new AuthBackend:
      override def verify(token: String): IO[AuthFailure, VerifiedClaims] =
        ZIO.fail(AuthFailure.AuthDisabled)

final private class AuthLive(enabled: Boolean, backend: AuthBackend) extends Auth:

  override def verify(token: String): IO[AuthFailure, VerifiedClaims] =
    if !enabled then ZIO.fail(AuthFailure.AuthDisabled)
    else backend.verify(token)

  override def verifyOptional(maybeToken: Option[String]): IO[AuthFailure, Option[VerifiedClaims]] =
    if !enabled then ZIO.succeed(None)
    else
      maybeToken match
        case None        => ZIO.succeed(None)
        case Some(token) => backend.verify(token).map(Some(_))
