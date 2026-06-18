package cortex.client.auth

import cortex.client.Page
import cortex.client.components.icons.{BrandIcons, LucideIcons}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The account control in the global header — an avatar that opens a calm anchored dropdown in the "Signed in
 * as → grouped rows → Sign out" format (ported from the Nav-redesign design). It adapts to the auth state:
 *
 *   - **Signed in** (`Authed`): identity (handle + "Signed in with GitHub"), a GitHub group (**View source**
 *     + **Star on GitHub** — the repo link folded out of the bar), **Manage account & data** → `/account`,
 *     and **Sign out**.
 *   - **Auth disabled** (`Disabled`, local dev): the same menu without Sign out.
 *   - **Signed out** (`Anonymous`, auth on): the menu collapses to a single **Sign in with GitHub** plus a
 *     **View source** row — GitHub *is* the sign-in.
 *   - `Loading`: nothing.
 *
 * Self-contained: subscribes to [[AuthStore]], mirroring the snapshot into local state. A fixed transparent
 * backdrop closes the dropdown on an outside click.
 */
object AccountMenu:

  final case class Props(ctl: RouterCtl[Page])

  private val GithubRepoUrl  = "https://github.com/ani2fun/cortex"
  private val GithubStarsUrl = s"$GithubRepoUrl/stargazers"

  /** Which face the control wears — derived from the auth status. */
  private enum Mode:
    case SignedIn(username: String)
    case Dev
    case Anon

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
            menu(Mode.SignedIn(user.preferredUsername), openS, props.ctl)
          case AuthStore.Status.Disabled  => menu(Mode.Dev, openS, props.ctl)
          case AuthStore.Status.Anonymous => menu(Mode.Anon, openS, props.ctl)
          case AuthStore.Status.Loading   => EmptyVdom
      }

  /** Avatar trigger + anchored dropdown (with a transparent click-outside backdrop while open). */
  private def menu(mode: Mode, openS: Hooks.UseState[Boolean], ctl: RouterCtl[Page]): VdomNode =
    val close = openS.setState(false)
    val (trigger, title): (VdomNode, String) = mode match
      case Mode.SignedIn(u) => (initialOf(u), s"@$u — account")
      case Mode.Dev         => (initialOf("dev"), "Local dev — your data")
      case Mode.Anon =>
        (LucideIcons.LogIn(LucideIcons.withClass("account-btn__glyph")): VdomNode, "Sign in")
    <.div(
      ^.className := "account-menu",
      <.button(
        ^.tpe        := "button",
        ^.className  := "account-btn",
        ^.title      := title,
        ^.aria.label := "Account",
        ^.onClick --> openS.modState(!_),
        trigger
      ),
      if openS.value then
        React.Fragment(
          <.div(^.className := "account-menu__backdrop", ^.onClick --> close),
          panel(mode, close, ctl)
        )
      else EmptyVdom
    )

  private def initialOf(name: String): VdomNode =
    name.headOption.map(_.toUpper.toString).getOrElse("?")

  private def panel(mode: Mode, close: Callback, ctl: RouterCtl[Page]): VdomNode =
    <.div(
      ^.className := "account-menu__panel",
      ^.role      := "menu",
      idHeader(mode),
      mode match
        case Mode.SignedIn(_) =>
          React.Fragment(
            sep,
            githubGroup(full = true),
            sep,
            manageGroup(close, ctl),
            sep,
            signOutGroup(close)
          )
        case Mode.Dev =>
          React.Fragment(sep, githubGroup(full = true), sep, manageGroup(close, ctl))
        case Mode.Anon =>
          React.Fragment(sep, signInGroup(close), sep, githubGroup(full = false))
    )

  // ── identity header ─────────────────────────────────────────────────────────

  private def idHeader(mode: Mode): VdomNode = mode match
    case Mode.SignedIn(u) =>
      <.div(
        ^.className := "account-menu__id",
        <.span(^.className := "account-menu__avatar", initialOf(u)),
        <.div(
          ^.className := "account-menu__id-text",
          <.div(^.className := "account-menu__id-label", "Signed in as"),
          <.div(^.className := "account-menu__handle", s"@$u"),
          <.div(
            ^.className := "account-menu__meta",
            BrandIcons.Github("account-menu__meta-icon"),
            "Signed in with GitHub"
          )
        )
      )
    case Mode.Dev =>
      <.div(
        ^.className := "account-menu__id",
        <.span(^.className := "account-menu__avatar", "D"),
        <.div(
          ^.className := "account-menu__id-text",
          <.div(^.className := "account-menu__id-label", "Local dev"),
          <.div(^.className := "account-menu__handle", "Auth disabled"),
          <.div(^.className := "account-menu__meta", "Everyone can edit — not a real account")
        )
      )
    case Mode.Anon =>
      <.div(
        ^.className := "account-menu__id",
        <.div(
          ^.className := "account-menu__id-text",
          <.div(^.className := "account-menu__id-label", "Not signed in"),
          <.div(
            ^.className := "account-menu__handle account-menu__handle--prompt",
            "Sign in to edit & run code."
          )
        )
      )

  // ── building blocks ───────────────────────────────────────────────────────

  private val sep: VdomNode = <.div(^.className := "account-menu__sep", ^.aria.hidden := true)

  private def githubGroup(full: Boolean): VdomNode =
    <.div(
      ^.className := "account-menu__group",
      rowLink(
        GithubRepoUrl,
        BrandIcons.Github("account-menu__row-icon"),
        "View source",
        meta = Some("ani2fun/cortex"),
        trailing = Some(LucideIcons.ExternalLink(LucideIcons.withClass("account-menu__row-ext")))
      ),
      if full then
        rowLink(
          GithubStarsUrl,
          LucideIcons.Star(LucideIcons.withClass("account-menu__row-icon")),
          "Star on GitHub",
          meta = None,
          trailing = Some(LucideIcons.ExternalLink(LucideIcons.withClass("account-menu__row-ext")))
        )
      else EmptyVdom
    )

  private def manageGroup(close: Callback, ctl: RouterCtl[Page]): VdomNode =
    <.div(
      ^.className := "account-menu__group",
      <.button(
        ^.tpe       := "button",
        ^.className := "account-menu__row",
        ^.onClick --> (close >> ctl.set(Page.Account)),
        LucideIcons.Settings(LucideIcons.withClass("account-menu__row-icon")),
        <.span(^.className := "account-menu__row-text", "Manage account & data")
      )
    )

  private def signOutGroup(close: Callback): VdomNode =
    <.div(
      ^.className := "account-menu__group",
      <.button(
        ^.tpe       := "button",
        ^.className := "account-menu__row account-menu__row--out",
        ^.onClick --> (close >> Callback(AuthStore.signOut())),
        LucideIcons.LogOut(LucideIcons.withClass("account-menu__row-icon")),
        <.span(^.className := "account-menu__row-text", "Sign out")
      )
    )

  private def signInGroup(close: Callback): VdomNode =
    <.div(
      ^.className := "account-menu__group account-menu__group--cta",
      <.button(
        ^.tpe       := "button",
        ^.className := "account-menu__signin-gh",
        ^.onClick --> (close >> Callback(AuthStore.openSignIn())),
        BrandIcons.Github("account-menu__signin-gh-icon"),
        "Sign in with GitHub"
      )
    )

  /** A link row: leading icon · label · flex spacer · optional mono meta · optional trailing icon. */
  private def rowLink(
      href: String,
      leading: VdomNode,
      text: String,
      meta: Option[String],
      trailing: Option[VdomNode]
  ): VdomNode =
    <.a(
      ^.href      := href,
      ^.target    := "_blank",
      ^.rel       := "noopener noreferrer",
      ^.className := "account-menu__row account-menu__row--gh",
      leading,
      <.span(^.className := "account-menu__row-text", text),
      <.span(^.className := "account-menu__row-sp"),
      meta.map(m => <.span(^.className := "account-menu__row-meta", m): VdomNode).getOrElse(EmptyVdom),
      trailing.getOrElse(EmptyVdom)
    )
