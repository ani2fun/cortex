package cortex.client.pages

import cortex.client.api.{ApiClient, TutorApiClient}
import cortex.client.auth.AuthStore
import cortex.client.components.icons.{BrandIcons, LucideIcons}
import cortex.client.util.{PageTitle, ReaderState}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext
import scala.util.{Failure, Success}

/**
 * The `/account` page — a signed-in user's profile + a **Danger zone** for bulk data deletion across every
 * problem/lesson: all coach history (live sessions + saved transcripts + browser mirrors), all code
 * submissions, or everything at once. Reached from the header avatar's "Manage account & data" entry.
 *
 * Deleting the sign-in **identity** itself is handled by linking out to Keycloak's own account console (the
 * cortex server holds no Keycloak-admin privilege — it only verifies JWTs), where a user can self-delete when
 * the realm enables it. Each data action is `window.confirm`-guarded and reports progress inline; the full
 * wipe reloads so every surface rehydrates empty. Auth-off (local dev) offers only the coach clear — there's
 * no identity to delete and the submissions API rejects authed calls when off.
 */
object AccountPage:

  private given ExecutionContext = JSExecutionContext.queue

  private enum Status:
    case Busy(message: String)
    case Done(message: String)
    case Failed(message: String)

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AuthStore.current)    // mirror of the auth snapshot
      .useState(Option.empty[Status]) // in-flight / done / error for the last action
      .useEffectOnMountBy { (_, authS, _) =>
        CallbackTo {
          PageTitle.set("Account — Aniket Kakde").runNow()
          val unsubscribe = AuthStore.subscribe(s => authS.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      .render { (_, authS, statusS) =>
        <.main(
          ^.className := "account-page",
          <.div(
            ^.className := "account-page__inner",
            authS.value.status match
              case AuthStore.Status.Authed(user, token) =>
                signedIn(user.preferredUsername, user.name, user.email, Some(token), statusS)
              case AuthStore.Status.Disabled =>
                signedIn("dev", None, None, None, statusS)
              case AuthStore.Status.Anonymous => anonymous
              case AuthStore.Status.Loading   => EmptyVdom
          )
        )
      }

  // ── signed-out ────────────────────────────────────────────────────────────────────────────────
  private val anonymous: VdomNode =
    <.div(
      ^.className := "account-page__signin",
      <.h1(^.className := "account-page__title", "Your account"),
      <.p(^.className  := "account-page__lede", "Sign in to manage your data and your account."),
      <.button(
        ^.tpe       := "button",
        ^.className := "account-page__signin-btn",
        ^.onClick --> Callback(AuthStore.openSignIn()),
        "Sign in"
      )
    )

  // ── signed-in (or auth-off "dev") ───────────────────────────────────────────────────────────────
  private def signedIn(
      handle: String,
      name: Option[String],
      email: Option[String],
      token: Option[String],
      statusS: Hooks.UseState[Option[Status]]
  ): VdomNode =
    val real = token.isDefined
    React.Fragment(
      <.h1(^.className := "account-page__title", "Your account"),
      identityCard(handle, name, email, real),
      dangerZone(token, statusS)
    )

  private def identityCard(
      handle: String,
      name: Option[String],
      email: Option[String],
      real: Boolean
  ): VdomNode =
    val initial = handle.headOption.map(_.toUpper.toString).getOrElse("?")
    <.section(
      ^.className := "account-page__identity",
      <.span(^.className := "account-page__avatar", initial),
      <.div(
        ^.className := "account-page__id-text",
        <.div(^.className := "account-page__handle", if real then s"@$handle" else "Local dev"),
        <.div(
          ^.className := "account-page__meta",
          if real then
            React.Fragment(BrandIcons.Github("account-page__meta-icon"), "Signed in with GitHub")
          else "Auth off — not a real account"
        ),
        name.filter(_.trim.nonEmpty).whenDefined(n => <.div(^.className := "account-page__field", n)),
        email.filter(_.trim.nonEmpty).whenDefined(e => <.div(^.className := "account-page__field", e))
      )
    )

  // ── danger zone ─────────────────────────────────────────────────────────────────────────────────
  private def dangerZone(token: Option[String], statusS: Hooks.UseState[Option[Status]]): VdomNode =
    val busy = statusS.value match
      case Some(Status.Busy(_)) => true
      case _                    => false

    def confirmThen(prompt: String)(run: => Unit): Callback =
      CallbackTo(dom.window.confirm(prompt)).flatMap(ok => if ok then Callback(run) else Callback.empty)

    def setStatus(s: Status): Unit = statusS.setState(Some(s)).runNow()

    // Coach history = live tutor sessions + (when signed in) durable saved transcripts + browser mirrors.
    val clearCoach: Callback =
      confirmThen(
        "Permanently delete ALL your coaching conversations — live sessions AND saved transcripts — " +
          "across every problem? This can't be undone."
      ) {
        setStatus(Status.Busy("Clearing coach history…"))
        val savedF: Future[Int] = token match
          case Some(t) => ApiClient.deleteAllSavedCoach(t).map(_.deleted)
          case None    => Future.successful(0)
        val flow =
          for
            live  <- TutorApiClient.clearAllSessions()
            saved <- savedF
          yield live.deleted + saved
        flow.onComplete {
          case Success(n) =>
            ReaderState.clearAllCoachMirrors()
            setStatus(Status.Done(s"Cleared your coach history ($n item(s) removed)."))
          case Failure(e) => setStatus(Status.Failed(s"Couldn't clear coach history: ${e.getMessage}"))
        }
      }

    val deleteSubs: Callback = token match
      case None => Callback.empty
      case Some(t) =>
        confirmThen(
          "Permanently delete ALL your code submissions across every problem? This can't be undone."
        ) {
          setStatus(Status.Busy("Deleting submissions…"))
          ApiClient.deleteAllSubmissions(t).onComplete {
            case Success(r) => setStatus(Status.Done(s"Deleted ${r.deleted} submission(s)."))
            case Failure(e) => setStatus(Status.Failed(s"Couldn't delete submissions: ${e.getMessage}"))
          }
        }

    val deleteAll: Callback = token match
      case None => Callback.empty
      case Some(t) =>
        confirmThen(
          "Permanently delete ALL your data — every coaching conversation AND every code submission? " +
            "This cannot be undone. The page will reload when it's done."
        ) {
          setStatus(Status.Busy("Deleting all your data…"))
          val flow =
            for
              _ <- TutorApiClient.clearAllSessions()
              _ <- ApiClient.deleteAllSavedCoach(t)
              _ <- ApiClient.deleteAllSubmissions(t)
            yield ()
          flow.onComplete {
            case Success(_) =>
              ReaderState.clearAllLocal()
              dom.window.location.reload()
            case Failure(e) => setStatus(Status.Failed(s"Couldn't delete all your data: ${e.getMessage}"))
          }
        }

    <.section(
      ^.className := "account-page__danger",
      <.div(
        ^.className := "account-page__danger-head",
        LucideIcons.AlertTriangle(LucideIcons.withClass("account-page__danger-icon")),
        "Danger zone"
      ),
      <.p(
        ^.className := "account-page__danger-note",
        "Per-problem coach history also clears with ",
        <.b("Start over"),
        " inside the coach, and submissions delete one at a time from the ",
        <.b("Submissions"),
        " tab. The actions below wipe everything at once."
      ),
      statusBanner(statusS.value),
      actionCard(
        "Delete all coach history",
        "Every coaching conversation — live sessions and saved transcripts — across all problems. Your " +
          "browser copies are cleared too.",
        "Delete coach history",
        clearCoach,
        busy
      ),
      if token.isDefined then
        React.Fragment(
          actionCard(
            "Delete all submissions",
            "Every code submission you've saved across all problems.",
            "Delete submissions",
            deleteSubs,
            busy
          ),
          actionCard(
            "Delete all my data",
            "Everything above, plus your in-browser reading progress. The page reloads when it's done.",
            "Delete all my data",
            deleteAll,
            busy
          ),
          // Identity deletion proper: a link out to Keycloak's own account console (the server stays
          // unprivileged). The data actions above run first; deleting the identity doesn't erase its data.
          accountConsoleCard(AuthStore.accountConsoleUrl)
        )
      else
        <.p(
          ^.className := "account-page__danger-note",
          "Submissions, the full wipe, and account deletion need a signed-in identity — run with auth on to " +
            "manage them."
        )
    )

  private def statusBanner(s: Option[Status]): VdomNode = s match
    case Some(Status.Busy(m)) =>
      <.div(
        ^.className := "account-page__status account-page__status--busy",
        LucideIcons.Loader2(LucideIcons.withClass("account-page__status-icon account-page__spin")),
        m
      )
    case Some(Status.Done(m)) =>
      <.div(
        ^.className := "account-page__status account-page__status--ok",
        LucideIcons.CircleCheck(LucideIcons.withClass("account-page__status-icon")),
        m
      )
    case Some(Status.Failed(m)) =>
      <.div(
        ^.className := "account-page__status account-page__status--err",
        LucideIcons.AlertTriangle(LucideIcons.withClass("account-page__status-icon")),
        m
      )
    case None => EmptyVdom

  private def actionCard(
      title: String,
      desc: String,
      btnLabel: String,
      onRun: Callback,
      busy: Boolean
  ): VdomNode =
    <.div(
      ^.className := "account-page__card",
      <.div(
        ^.className := "account-page__card-text",
        <.h3(^.className := "account-page__card-title", title),
        <.p(^.className  := "account-page__card-desc", desc)
      ),
      <.button(
        ^.tpe       := "button",
        ^.className := "account-page__btn account-page__btn--danger",
        ^.disabled  := busy,
        ^.onClick --> onRun,
        LucideIcons.Trash2(LucideIcons.withClass("account-page__btn-icon")),
        btnLabel
      )
    )

  /**
   * Identity deletion sits one origin away — in Keycloak's account console. We render a link rather than a
   * delete call so the cortex server never needs Keycloak-admin rights. `url` is `None` only in the unlikely
   * case keycloak-js can't build it; we then disable the control rather than guess a URL.
   */
  private def accountConsoleCard(url: Option[String]): VdomNode =
    <.div(
      ^.className := "account-page__card",
      <.div(
        ^.className := "account-page__card-text",
        <.h3(^.className := "account-page__card-title", "Delete my account"),
        <.p(
          ^.className := "account-page__card-desc",
          "Permanently remove your sign-in identity in Keycloak's own account console. Delete your data " +
            "above first — removing the identity doesn't erase the data tied to it. Opens in a new tab."
        )
      ),
      url match
        case Some(u) =>
          <.a(
            ^.className := "account-page__btn account-page__btn--danger",
            ^.href      := u,
            ^.target    := "_blank",
            ^.rel       := "noopener noreferrer",
            LucideIcons.ExternalLink(LucideIcons.withClass("account-page__btn-icon")),
            "Open account console"
          )
        case None =>
          <.button(^.tpe := "button", ^.className := "account-page__btn", ^.disabled := true, "Unavailable")
    )
