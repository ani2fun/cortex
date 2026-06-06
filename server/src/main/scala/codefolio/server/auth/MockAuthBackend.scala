package codefolio.server.auth

import zio.*

/**
 * Test fake [[AuthBackend]]. Lives in `main` (not `test`) so route-layer integration tests can wire it via a
 * [[Auth]] layer; never invoked in production (the live layer always picks [[KeycloakAuthBackend]] when auth
 * is enabled).
 *
 * Token shapes recognised:
 *   - `"valid:<sub>"` → success with the given subject + a canned preferred username
 *   - `"expired:<sub>"` → fails with [[AuthFailure.TokenExpired]]
 *   - anything else → fails with [[AuthFailure.InvalidToken]]
 */
object MockAuthBackend:

  private[auth] def make(): AuthBackend =
    new AuthBackend:
      override def verify(token: String): IO[AuthFailure, VerifiedClaims] =
        token match
          case t if t.startsWith("valid:") =>
            val sub = t.drop("valid:".length)
            ZIO.succeed(
              VerifiedClaims(
                sub = sub,
                preferredUsername = sub,
                name = Some("Mock User"),
                email = Some(s"$sub@example.test")
              )
            )
          case t if t.startsWith("expired:") =>
            ZIO.fail(AuthFailure.TokenExpired)
          case other =>
            ZIO.fail(AuthFailure.InvalidToken(s"Mock backend rejected token: '$other'"))

  /**
   * Test layer: returns an [[Auth]] with `enabled = true` whose backend is the mock above. Tests provide this
   * in place of [[Auth.live]] so they don't need a Keycloak (or even an `AuthConfig`).
   */
  val layer: ULayer[Auth] = ZLayer.succeed(Auth.fromBackend(enabled = true, backend = make()))
