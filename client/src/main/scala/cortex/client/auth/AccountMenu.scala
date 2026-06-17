package cortex.client.auth

import cortex.client.Page
import cortex.client.components.icons.{BrandIcons, LucideIcons}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The account entry in the global header — adapts to the auth state:
 *
 *   - **Signed in** (`Authed`): an initial-letter avatar opening a calm anchored dropdown — identity (handle
 *     + "Signed in with GitHub"), a note on where to manage data, and Sign out. No bulk-delete: per-problem
 *     coach history clears with "Start over" inside the coach, and submissions delete per-row in the
 *     Submissions tab.
 *   - **Signed out** (`Anonymous`, auth on): a **"Sign in"** button that opens the GitHub sign-in modal.
 *   - **Auth disabled** (`Disabled`, local dev): a "dev" avatar opening the same dropdown without a Sign out.
 *   - `Loading`: nothing.
 *
 * Self-contained: subscribes to [[AuthStore]], mirroring the snapshot into local state. A fixed transparent
 * backdrop closes the dropdown on an outside click (the same pattern as the coach's model picker).
 */
object AccountMenu:

  final case class Props(ctl: RouterCtl[Page])

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(AuthStore.current) // mirror of the auth snapshot
      .useState(false)             // dropdown open
      .useEffectOnMountBy { (_, snapS, _) =>
        CallbackTo {
          val unsubscribe = AuthStore.subscribe(s => snapS.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      .render { (props, snapS, openS) =>
        snapS.value.status match
          case AuthStore.Status.Authed(user, _) =>
            menu(user.preferredUsername, signedIn = true, openS, props.ctl)
          case AuthStore.Status.Disabled => menu("dev", signedIn = false, openS, props.ctl)
          case AuthStore.Status.Anonymous =>
            <.button(
              ^.tpe       := "button",
              ^.className := "account-signin",
              ^.onClick --> Callback(AuthStore.openSignIn()),
              "Sign in"
            )
          case AuthStore.Status.Loading => EmptyVdom
      }

  /** Avatar trigger + anchored dropdown (with a transparent click-outside backdrop while open). */
  private def menu(
      username: String,
      signedIn: Boolean,
      openS: Hooks.UseState[Boolean],
      ctl: RouterCtl[Page]
  ): VdomNode =
    val initial = username.headOption.map(_.toUpper.toString).getOrElse("?")
    val close   = openS.setState(false)
    <.div(
      ^.className := "account-menu",
      <.button(
        ^.tpe        := "button",
        ^.className  := "account-btn",
        ^.title      := (if signedIn then s"@$username — account" else "Local dev — your data"),
        ^.aria.label := "Account",
        ^.onClick --> openS.modState(!_),
        initial
      ),
      if openS.value then
        React.Fragment(
          <.div(^.className := "account-menu__backdrop", ^.onClick --> close),
          panel(username, initial, signedIn, close, ctl)
        )
      else EmptyVdom
    )

  private def panel(
      username: String,
      initial: String,
      signedIn: Boolean,
      close: Callback,
      ctl: RouterCtl[Page]
  ): VdomNode =
    <.div(
      ^.className := "account-menu__panel",
      ^.role      := "menu",
      // ── identity ──
      <.div(
        ^.className := "account-menu__id",
        <.span(^.className := "account-menu__avatar", initial),
        <.div(
          ^.className := "account-menu__id-text",
          if signedIn then
            React.Fragment(
              <.div(^.className := "account-menu__handle", s"@$username"),
              <.div(
                ^.className := "account-menu__meta",
                BrandIcons.Github("account-menu__meta-icon"),
                "Signed in with GitHub"
              )
            )
          else
            React.Fragment(
              <.div(^.className := "account-menu__handle", "Local dev"),
              <.div(^.className := "account-menu__meta", "Auth off — not a real account")
            )
        )
      ),
      // ── manage account & data → /account ──
      <.button(
        ^.tpe       := "button",
        ^.className := "account-menu__manage",
        ^.onClick --> (close >> ctl.set(Page.Account)),
        LucideIcons.Settings(LucideIcons.withClass("account-menu__manage-icon")),
        "Manage account & data"
      ),
      // ── data note: where deletions live ──
      <.div(
        ^.className := "account-menu__data",
        <.div(^.className := "account-menu__data-h", "Managing your data"),
        <.p(
          ^.className := "account-menu__data-body",
          "Coach history clears per problem with ",
          <.b("Start over"),
          " inside the coach; submissions delete individually from the ",
          <.b("Submissions"),
          " tab. For bulk deletes across everything, open ",
          <.b("Manage account & data"),
          " above."
        )
      ),
      // ── sign out (signed-in only) ──
      if signedIn then
        <.button(
          ^.tpe       := "button",
          ^.className := "account-menu__signout",
          ^.onClick --> (close >> Callback(AuthStore.signOut())),
          LucideIcons.LogOut(LucideIcons.withClass("account-menu__signout-icon")),
          "Sign out"
        )
      else EmptyVdom
    )
