package codefolio.server.auth

/**
 * Internal projection of the signature-validated JWT claims for a single request.
 *
 * Never serialised to the wire — the corresponding API response shape is `codefolio.shared.api.Endpoints.
 * UserInfo` (which wraps these claims plus the caller's current per-user rate-limit snapshot). Keeping the
 * two types separate means we can extract more claims server-side without expanding the public contract.
 *
 * `preferredUsername` carries the GitHub login when the user signed in through the realm's GitHub IdP
 * (Keycloak's GitHub identity provider populates `preferred_username` from the user's GitHub handle).
 */
final case class VerifiedClaims(
    sub: String,
    preferredUsername: String,
    name: Option[String],
    email: Option[String]
)
