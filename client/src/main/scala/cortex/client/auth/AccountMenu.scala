package cortex.client.auth

import cortex.client.api.{ApiClient, TutorApiClient}
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.util.{Failure, Success}

/**
 * The account entry in the global header — adapts to the auth state:
 *
 *   - **Signed in** (`Authed`): an initial-letter avatar opening an account modal with sign-out and a Danger
 *     zone — "Clear all coach history" and "Delete all my data" (coach conversations + code submissions).
 *   - **Signed out** (`Anonymous`, auth on): a **"Sign in"** button that opens the GitHub sign-in modal.
 *     (Previously the only sign-in entry points were the inline "Edit" buttons on code blocks.)
 *   - **Auth disabled** (`Disabled`, local dev): a "dev" avatar whose modal can clear the local coach
 *     history. Submissions deletion needs auth on (the cortex API rejects authed calls when off), so the full
 *     data-wipe is hidden there.
 *   - `Loading`: nothing.
 *
 * Self-contained: subscribes to [[AuthStore]], mirroring the snapshot into local state. Per-problem coach
 * clearing lives in the Coach tab ("Start over"); this is the GLOBAL home. Destructive actions are
 * confirm-guarded; a full wipe reloads so every surface rehydrates empty.
 */
object AccountMenu:

  private given ExecutionContext = JSExecutionContext.queue

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AuthStore.current)    // mirror of the auth snapshot
      .useState(false)                // modal open
      .useState(Option.empty[String]) // status line (in-flight / done / error)
      .useEffectOnMountBy { (_, snapS, _, _) =>
        CallbackTo {
          val unsubscribe = AuthStore.subscribe(s => snapS.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      .render { (_, snapS, openS, msgS) =>
        snapS.value.status match
          case AuthStore.Status.Authed(user, token) =>
            avatarAndModal(user.preferredUsername, Some(token), openS, msgS)
          case AuthStore.Status.Disabled =>
            // Local dev (auth off): no real account, but the dev coach data exists — let it be seen
            // and cleared. The "dev" label flags that this isn't a real session.
            avatarAndModal("dev", None, openS, msgS)
          case AuthStore.Status.Anonymous =>
            <.button(
              ^.tpe       := "button",
              ^.className := "account-signin",
              ^.onClick --> Callback(AuthStore.openSignIn()),
              "Sign in"
            )
          case AuthStore.Status.Loading => EmptyVdom
      }

  private def avatarAndModal(
      username: String,
      tokenOpt: Option[String],
      openS: Hooks.UseState[Boolean],
      msgS: Hooks.UseState[Option[String]]
  ): VdomNode =
    val initial = username.headOption.map(_.toUpper.toString).getOrElse("?")
    React.Fragment(
      <.button(
        ^.tpe       := "button",
        ^.className := "account-btn",
        ^.title := (if tokenOpt.isDefined then s"@$username — account & data" else "Local dev — your data"),
        ^.aria.label := "Account and data settings",
        ^.onClick --> (openS.setState(true) >> msgS.setState(None)),
        initial
      ),
      if openS.value then modal(username, tokenOpt, openS, msgS) else EmptyVdom
    )

  private def modal(
      username: String,
      tokenOpt: Option[String],
      openS: Hooks.UseState[Boolean],
      msgS: Hooks.UseState[Option[String]]
  ): VdomNode =
    val close = openS.setState(false) >> msgS.setState(None)

    def confirmThen(prompt: String)(run: => Unit): Callback =
      CallbackTo(dom.window.confirm(prompt)).flatMap(ok => if ok then Callback(run) else Callback.empty)

    val clearCoach: Callback =
      confirmThen(
        "Permanently delete ALL your coaching conversations across every problem? This can't be undone."
      ) {
        msgS.setState(Some("Clearing coach history…")).runNow()
        TutorApiClient.clearAllSessions().onComplete {
          case Success(r) => msgS.setState(Some(s"Cleared ${r.deleted} coaching session(s).")).runNow()
          case Failure(e) => msgS.setState(Some(s"Failed: ${e.getMessage}")).runNow()
        }
      }

    // The full wipe needs a bearer for the submissions half — only offered when actually signed in.
    val deleteEverythingNode: VdomNode =
      tokenOpt match
        case Some(token) =>
          val run = confirmThen(
            "Permanently delete ALL your data — every coaching conversation AND every saved code " +
              "submission? This cannot be undone."
          ) {
            msgS.setState(Some("Deleting all your data…")).runNow()
            val both =
              for
                _ <- TutorApiClient.clearAllSessions()
                _ <- ApiClient.deleteAllSubmissions(token)
              yield ()
            both.onComplete {
              case Success(_) => dom.window.location.reload()
              case Failure(e) => msgS.setState(Some(s"Failed: ${e.getMessage}")).runNow()
            }
          }
          <.button(
            ^.tpe       := "button",
            ^.className := "account-modal__danger-btn account-modal__danger-btn--max",
            ^.onClick --> run,
            LucideIcons.Trash2(LucideIcons.withClass("account-modal__danger-btn-icon")),
            "Delete all my data (coach + submissions)"
          )
        case None =>
          <.p(
            ^.className := "account-modal__danger-note",
            "Code submissions can't be managed in local-dev mode (auth is off). Run with auth enabled " +
              "to delete submissions or wipe everything."
          )

    val onScrim: ReactMouseEvent => Callback = e =>
      if e.target == e.currentTarget then close else Callback.empty

    <.div(
      ^.className  := "account-modal",
      ^.role       := "dialog",
      ^.aria.modal := true,
      ^.onClick ==> onScrim,
      <.div(
        ^.className := "account-modal__card",
        <.div(
          ^.className := "account-modal__head",
          <.button(
            ^.tpe        := "button",
            ^.className  := "account-modal__close",
            ^.aria.label := "Close",
            ^.onClick --> close,
            LucideIcons.X(LucideIcons.withClass("account-modal__close-icon"))
          ),
          <.div(
            ^.className := "account-modal__eyebrow",
            if tokenOpt.isDefined then s"Account · @$username" else "Local dev · auth off"
          ),
          <.h2(^.className := "account-modal__title", "Your data"),
          tokenOpt match
            case Some(_) =>
              <.button(
                ^.tpe       := "button",
                ^.className := "account-modal__signout",
                ^.onClick --> (close >> Callback(AuthStore.signOut())),
                "Sign out"
              )
            case None => EmptyVdom
        ),
        <.div(
          ^.className := "account-modal__danger",
          <.div(
            ^.className := "account-modal__danger-head",
            LucideIcons.AlertTriangle(LucideIcons.withClass("account-modal__danger-icon")),
            "Danger zone"
          ),
          <.p(
            ^.className := "account-modal__danger-note",
            "Permanent. Per-problem coach history clears with \"Start over\" inside the coach; individual " +
              "submissions from the Submissions tab."
          ),
          <.button(
            ^.tpe       := "button",
            ^.className := "account-modal__danger-btn",
            ^.onClick --> clearCoach,
            LucideIcons.Trash2(LucideIcons.withClass("account-modal__danger-btn-icon")),
            "Clear all coach history"
          ),
          deleteEverythingNode,
          msgS.value.map(m => <.p(^.className := "account-modal__msg", m)).getOrElse(EmptyVdom)
        )
      )
    )
