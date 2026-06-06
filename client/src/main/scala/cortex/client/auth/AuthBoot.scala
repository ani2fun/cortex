package cortex.client.auth

import cortex.client.api.ApiClient
import cortex.shared.api.Endpoints.AuthConfig
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext
import scala.util.{Failure, Success}

/**
 * One-shot auth bootstrap, run once from [[cortex.client.Main]] before the router mounts.
 *
 * Sequence:
 *   1. GET `/api/auth/config`. `enabled = false` → [[AuthStore.Status.Disabled]] and stop (dev mode; the SPA
 *      never loads keycloak-js). 2. Otherwise construct `keycloak-js` and `init({ onLoad: "check-sso" })`.
 *      - session adopted → GET `/api/me`, publish [[AuthStore.Status.Authed]], start the refresh timer;
 *      - no session → [[AuthStore.Status.Anonymous]]. 3. Every 30s, `updateToken(60)`; a refresh failure
 *        means the session lapsed → back to `Anonymous`.
 *
 * Failure handling is deliberately lenient (ADR-0001 / ADR-0002 spirit): if `/api/auth/config` or the
 * Keycloak `init` can't be reached, the rest of the site must still work, so we fall back to a status that
 * doesn't gate reading or running canonical code.
 */
object AuthBoot:

  private given scala.concurrent.ExecutionContext = JSExecutionContext.queue

  private val RefreshIntervalMs     = 30000
  private val RefreshMinValiditySec = 60

  /** Kick off the bootstrap. Fire-and-forget — all results land in [[AuthStore]]. */
  def run(): Unit =
    ApiClient.getAuthConfig.onComplete {
      case Success(cfg) if !cfg.enabled =>
        AuthStore.setStatus(AuthStore.Status.Disabled)
      case Success(cfg) =>
        initKeycloak(cfg)
      case Failure(err) =>
        dom.console.warn(s"auth: /api/auth/config unreachable (${err.getMessage}); treating as disabled")
        AuthStore.setStatus(AuthStore.Status.Disabled)
    }

  private def initKeycloak(cfg: AuthConfig): Unit =
    (cfg.issuerUrl, cfg.realm, cfg.clientId) match
      case (Some(issuerUrl), Some(realm), Some(clientId)) =>
        // keycloak-js wants the Keycloak *base* URL, not the realm URL the server reports as `issuerUrl`.
        val baseUrl = issuerUrl.stripSuffix(s"/realms/$realm")
        val kc      = new Keycloak(KeycloakConfig(baseUrl, realm, clientId))
        AuthStore.registerKeycloak(kc)

        kc.init(KeycloakInitOptions.checkSso).toFuture.onComplete {
          case Success(true) =>
            onAuthenticated(kc)
          case Success(false) =>
            AuthStore.setStatus(AuthStore.Status.Anonymous)
          case Failure(err) =>
            dom.console.warn(s"auth: keycloak init failed (${err.getMessage}); treating as anonymous")
            AuthStore.setStatus(AuthStore.Status.Anonymous)
        }

      case _ =>
        // enabled = true but coordinates missing — a server misconfiguration. Fail safe to anonymous.
        dom.console.warn("auth: /api/auth/config enabled but issuerUrl/realm/clientId missing")
        AuthStore.setStatus(AuthStore.Status.Anonymous)

  /** Session adopted: fetch the identity projection, publish `Authed`, and arm the refresh timer. */
  private def onAuthenticated(kc: Keycloak): Unit =
    val token = kc.token.toOption.getOrElse("")
    ApiClient.getMe(token).onComplete {
      case Success(user) =>
        AuthStore.setStatus(AuthStore.Status.Authed(user, token))
        startRefreshTimer(kc)
      case Failure(err) =>
        dom.console.warn(s"auth: /api/me failed (${err.getMessage}); treating as anonymous")
        AuthStore.setStatus(AuthStore.Status.Anonymous)
    }

  /**
   * Every 30s ask keycloak-js to refresh the token if it's within 60s of expiry. On a refresh failure the
   * session has lapsed: drop to anonymous and **clear the interval** — without that, the timer would keep
   * polling a dead `updateToken` every 30s for the life of the page.
   */
  private def startRefreshTimer(kc: Keycloak): Unit =
    var intervalId: js.UndefOr[Int] = js.undefined
    intervalId = dom.window.setInterval(
      () =>
        kc.updateToken(RefreshMinValiditySec).toFuture.onComplete {
          case Success(refreshed) =>
            if refreshed then AuthStore.refreshToken(kc.token.toOption.getOrElse(""))
          case Failure(err) =>
            dom.console.warn(s"auth: token refresh failed (${err.getMessage}); session ended")
            AuthStore.setStatus(AuthStore.Status.Anonymous)
            intervalId.foreach(dom.window.clearInterval)
        },
      RefreshIntervalMs.toDouble
    )
