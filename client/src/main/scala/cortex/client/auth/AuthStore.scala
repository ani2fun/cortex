package cortex.client.auth

import cortex.shared.api.Endpoints.UserInfo
import org.scalajs.dom

import scala.util.Try

/**
 * Global auth state for the SPA — a tiny observable store, deliberately *not* a React context.
 *
 * Why a singleton store rather than `React.createContext`: the auth state is async (keycloak-js boot, 30s
 * token refresh) and is read by exactly one deep component family (`RunnableCodeBlock`) plus the app-root
 * sign-in modal. A store with explicit `subscribe` keeps that wiring trivial and avoids threading a provider
 * through the router. Components mirror [[current]] into local `useState` and re-render on [[subscribe]]
 * notifications.
 *
 * [[AuthBoot]] owns all the writes (it holds the `keycloak-js` handle); UI code only calls the public actions
 * ([[openSignIn]], [[closeSignIn]], [[signIn]], [[signOut]]) and reads [[current]].
 */
object AuthStore:

  /** Where the visitor stands with respect to authentication. */
  enum Status:
    /** Boot in flight — `/api/auth/config` not yet resolved. */
    case Loading

    /** Auth is switched off server-side (`AUTH_ENABLED=false`); everyone may edit freely. */
    case Disabled

    /** Auth is on; nobody is signed in. */
    case Anonymous

    /** Auth is on; a verified user holds a live access token. */
    case Authed(user: UserInfo, token: String)

  /** Sign-in modal lifecycle. `Redirecting` is the brief window after the CTA before the browser leaves. */
  enum ModalPhase:
    case Idle, Redirecting

  /** Immutable snapshot handed to subscribers. */
  final case class Snapshot(status: Status, modalOpen: Boolean, modalPhase: ModalPhase)

  private var snapshot: Snapshot =
    Snapshot(Status.Loading, modalOpen = false, ModalPhase.Idle)

  private var listeners: List[Snapshot => Unit] = Nil

  // Set once by AuthBoot after `new Keycloak(...)`. None while auth is Loading/Disabled.
  private var keycloak: Option[Keycloak] = None

  // The tutor service base URL from `/api/auth/config` (a *different origin* than the cortex API).
  // Boot-time config, not reactive state — set once by AuthBoot, read by the tutor client. None until
  // config resolves, or if the server reports no tutor (the coach UI then degrades).
  private var tutorBase: Option[String] = None

  /** The current snapshot. Components seed local state from this on mount. */
  def current: Snapshot = snapshot

  /** The tutor service base URL, or `None` if the tutor isn't configured / config hasn't resolved. */
  def tutorBaseUrl: Option[String] = tutorBase

  /**
   * Keycloak's **account-console** URL for the signed-in user, or `None` when auth is off / not yet booted.
   * keycloak-js builds it (carrying the realm + a referrer back to the app), so the SPA never reconstructs
   * the issuer URL by hand. The account console is where a user can permanently delete their own sign-in
   * identity (when the realm enables self-service deletion) — the server holds no Keycloak-admin privilege.
   */
  def accountConsoleUrl: Option[String] =
    keycloak.flatMap(kc => Try(kc.createAccountUrl()).toOption).map(_.trim).filter(_.nonEmpty)

  /** Register a listener; returns an unsubscribe thunk to run on component unmount. */
  def subscribe(listener: Snapshot => Unit): () => Unit =
    listeners = listener :: listeners
    () => listeners = listeners.filterNot(_ eq listener)

  // ── Writes — AuthBoot only ───────────────────────────────────────────────

  private[auth] def registerKeycloak(kc: Keycloak): Unit = keycloak = Some(kc)

  /** Capture the tutor base URL from `/api/auth/config` (blank → treated as absent). */
  private[auth] def setTutorBaseUrl(url: Option[String]): Unit =
    tutorBase = url.map(_.trim).filter(_.nonEmpty)

  private[auth] def setStatus(status: Status): Unit = update(_.copy(status = status))

  /** Replace the access token on an existing `Authed` status (called by the 30s refresh timer). */
  private[auth] def refreshToken(token: String): Unit =
    update { s =>
      s.status match
        case Status.Authed(user, _) => s.copy(status = Status.Authed(user, token))
        case _                      => s
    }

  // ── Public actions — UI ──────────────────────────────────────────────────

  /** Open the sign-in modal (triggered by clicking a locked "Edit" button while anonymous). */
  def openSignIn(): Unit = update(_.copy(modalOpen = true, modalPhase = ModalPhase.Idle))

  /** Dismiss the sign-in modal without authenticating. */
  def closeSignIn(): Unit = update(_.copy(modalOpen = false, modalPhase = ModalPhase.Idle))

  /**
   * Begin the GitHub sign-in redirect. Shows the `Redirecting` spinner, then hands off to keycloak-js, which
   * navigates the browser away. Returns to the current URL once the dance completes.
   */
  def signIn(): Unit =
    update(_.copy(modalPhase = ModalPhase.Redirecting))
    keycloak.foreach { kc =>
      kc.login(KeycloakLoginOptions(redirectUri = dom.window.location.href, idpHint = "github"))
      ()
    }

  /** End the session. keycloak-js redirects to Keycloak's logout endpoint, then back to the site root. */
  def signOut(): Unit =
    ByokKeyStore.clearAll() // the visitor's own BYOK keys must not outlive their sign-in
    keycloak match
      case Some(kc) =>
        kc.logout(KeycloakLogoutOptions(redirectUri = dom.window.location.origin))
        ()
      case None =>
        // Auth disabled or never booted — nothing to revoke; just drop local state.
        setStatus(Status.Anonymous)

  private def update(f: Snapshot => Snapshot): Unit =
    val next = f(snapshot)
    if next != snapshot then
      snapshot = next
      listeners.foreach(_(next))
