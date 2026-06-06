package cortex.server.auth

import cortex.server.config.AuthConfig
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.{JWKSource, JWKSourceBuilder}
import com.nimbusds.jose.proc.{JWSVerificationKeySelector, SecurityContext}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.{BadJWTException, DefaultJWTClaimsVerifier, DefaultJWTProcessor}
import zio.*

import java.net.URI
import scala.jdk.CollectionConverters.*

/**
 * Live [[AuthBackend]] backed by `com.nimbusds:nimbus-jose-jwt`.
 *
 * What this validates per token:
 *   1. RS256 signature against the realm's JWK set (`{issuerUrl}/protocol/openid-connect/certs`). JWKS is
 *      fetched lazily on the first call and cached in-process by Nimbus' `JWKSourceBuilder` machinery. 2.
 *      `iss` claim equals the configured issuer URL exactly. 3. `exp` is in the future, `iat` / `nbf` are not
 *      in the future (the default `DefaultJWTClaimsVerifier` behaviour). 4. Either `aud` contains the
 *      configured audience **or** the `azp` (authorized party) claim equals it. Keycloak's quirk: tokens
 *      issued to a public SPA client carry `aud: ["account"]` and `azp: "<clientId>"`, so checking only `aud`
 *      would reject every real token.
 *
 * The processor instance is built once and reused — it's thread-safe per the Nimbus docs.
 */
object KeycloakAuthBackend:

  /**
   * Construct the backend from config. Pure — doesn't make any network calls. The first `processor.process`
   * invocation fetches the JWKS.
   */
  private[auth] def make(cfg: AuthConfig): AuthBackend =
    val jwksUrl = URI.create(s"${cfg.issuerUrl.stripSuffix("/")}/protocol/openid-connect/certs").toURL
    val jwkSource: JWKSource[SecurityContext] =
      JWKSourceBuilder.create[SecurityContext](jwksUrl).build()

    val processor = new DefaultJWTProcessor[SecurityContext]()
    processor.setJWSKeySelector(new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource))
    processor.setJWTClaimsSetVerifier(ClaimsVerifier(cfg))

    new AuthBackend:
      override def verify(token: String): IO[AuthFailure, VerifiedClaims] =
        ZIO
          .attemptBlocking(processor.process(token, null))
          .mapError(translate)
          .flatMap(extractClaims)

  /**
   * Translate Nimbus' exception zoo into our minimal [[AuthFailure]] vocabulary. We distinguish "expired"
   * from "invalid" for log-readability — both map to 401 at the HTTP layer.
   *
   * Nimbus throws `BadJWTException("Expired JWT")` for expired tokens (string-match is unfortunately the
   * documented identification path: there's no typed subclass).
   */
  private def translate(t: Throwable): AuthFailure =
    val msg = Option(t.getMessage).getOrElse(t.getClass.getSimpleName)
    if msg.toLowerCase.contains("expired") then AuthFailure.TokenExpired
    else AuthFailure.InvalidToken(msg)

  /** Project the validated claims set into our internal [[VerifiedClaims]] shape. */
  private def extractClaims(set: JWTClaimsSet): IO[AuthFailure, VerifiedClaims] =
    Option(set.getSubject) match
      case None =>
        ZIO.fail(AuthFailure.InvalidToken("Missing 'sub' claim"))
      case Some(sub) =>
        // preferred_username is conventional for OIDC; falls back to sub so the API always has a non-empty
        // handle to render in the identity chip.
        val pref = Option(set.getStringClaim("preferred_username")).filter(_.nonEmpty).getOrElse(sub)
        val nm   = Option(set.getStringClaim("name")).filter(_.nonEmpty)
        val em   = Option(set.getStringClaim("email")).filter(_.nonEmpty)
        ZIO.succeed(VerifiedClaims(sub = sub, preferredUsername = pref, name = nm, email = em))

  /**
   * Sub-class of Nimbus' built-in verifier that adds the `azp`-or-`aud` check on top of issuer + standard
   * time claims. We use the two-arg constructor — no built-in audience check — and re-implement the check
   * ourselves in [[verify]] below so we can accept the Keycloak `azp` fallback too.
   */
  final private class ClaimsVerifier(cfg: AuthConfig)
      extends DefaultJWTClaimsVerifier[SecurityContext](
        new JWTClaimsSet.Builder().issuer(cfg.issuerUrl).build(),
        java.util.Set.of("sub", "exp", "iat")
      ):

    override def verify(claims: JWTClaimsSet, ctx: SecurityContext): Unit =
      super.verify(claims, ctx)
      val expected = cfg.audience
      val aud      = Option(claims.getAudience).map(_.asScala.toList).getOrElse(Nil)
      val azp      = Option(claims.getStringClaim("azp"))
      if !(aud.contains(expected) || azp.contains(expected)) then
        throw new BadJWTException(s"Token aud/azp does not include '$expected'")
