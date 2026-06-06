package codefolio.client.auth

import codefolio.shared.AppRoutes
import org.scalajs.dom

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Thin Scala.js facade over `keycloak-js` (v26).
 *
 * Only the surface codefolio actually uses is bound — construction, `init`, `login`, `logout`, `updateToken`,
 * and the `token` / `authenticated` fields. The richer keycloak-js API (account management, role helpers, …)
 * is intentionally left unbound.
 *
 * The auth flow is SPA-style OIDC with PKCE: `login()` triggers a full-page redirect to Keycloak (and on to
 * GitHub via `idpHint`), the browser returns to `redirectUri`, and the next `init({ onLoad: "check-sso" })`
 * picks the session back up. See [[AuthBoot]] for the orchestration.
 */

/** Config object for `new Keycloak({...})`. `url` is the Keycloak *base* URL (no `/realms/...`). */
trait KeycloakConfig extends js.Object

object KeycloakConfig:

  def apply(url: String, realm: String, clientId: String): KeycloakConfig =
    js.Dynamic.literal(url = url, realm = realm, clientId = clientId).asInstanceOf[KeycloakConfig]

/** Options for `Keycloak.init`. */
trait KeycloakInitOptions extends js.Object

object KeycloakInitOptions:

  /**
   * `check-sso` boot: silently adopt an existing Keycloak session if present, otherwise stay anonymous.
   *
   * `silentCheckSsoRedirectUri` is load-bearing. Without it, keycloak-js runs the `check-sso` probe as a
   * **full top-level redirect** to Keycloak and back — so every page load visibly reloads twice and flashes
   * OAuth parameters through the address bar. Pointing it at a tiny same-origin `silent-check-sso.html` makes
   * the probe run inside a hidden iframe instead: no navigation, no reload.
   *
   * `silentCheckSsoFallback = false` keeps it that way — if the iframe probe can't reach a verdict (e.g. a
   * browser blocking third-party storage) we stay anonymous rather than falling back to the redirect probe. A
   * missed session is far better UX than a surprise double reload.
   */
  def checkSso: KeycloakInitOptions =
    js.Dynamic
      .literal(
        onLoad = "check-sso",
        pkceMethod = "S256",
        checkLoginIframe = false,
        silentCheckSsoRedirectUri = dom.window.location.origin + "/" + AppRoutes.SilentCheckSso,
        silentCheckSsoFallback = false
      )
      .asInstanceOf[KeycloakInitOptions]

/**
 * Options for `Keycloak.login`. `idpHint = "github"` skips the realm login page, going straight to GitHub.
 */
trait KeycloakLoginOptions extends js.Object

object KeycloakLoginOptions:

  def apply(redirectUri: String, idpHint: String): KeycloakLoginOptions =
    js.Dynamic.literal(redirectUri = redirectUri, idpHint = idpHint).asInstanceOf[KeycloakLoginOptions]

/** Options for `Keycloak.logout`. */
trait KeycloakLogoutOptions extends js.Object

object KeycloakLogoutOptions:

  def apply(redirectUri: String): KeycloakLogoutOptions =
    js.Dynamic.literal(redirectUri = redirectUri).asInstanceOf[KeycloakLogoutOptions]

/** The `keycloak-js` default export — a constructor. */
@js.native
@JSImport("keycloak-js", JSImport.Default)
class Keycloak(@unused config: KeycloakConfig) extends js.Object:
  /** Boot the adapter. Resolves to `true` when an existing session was adopted. */
  def init(options: KeycloakInitOptions): js.Promise[Boolean] = js.native

  /** Redirect to Keycloak to authenticate. The browser leaves the page. */
  def login(options: KeycloakLoginOptions): js.Promise[Unit] = js.native

  /** Redirect to Keycloak to end the session. The browser leaves the page. */
  def logout(options: KeycloakLogoutOptions): js.Promise[Unit] = js.native

  /** Refresh the access token if it expires within `minValidity` seconds. Resolves `true` if refreshed. */
  def updateToken(minValidity: Int): js.Promise[Boolean] = js.native

  /** The current access token (a JWT), once authenticated. */
  val token: js.UndefOr[String] = js.native

  /** Whether a session is currently active. */
  val authenticated: js.UndefOr[Boolean] = js.native
