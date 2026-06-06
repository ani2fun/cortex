package codefolio.client.auth

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The signed-in identity pill shown in a runnable code block's header: an initial-letter avatar, the
 * `@handle`, and a small sign-out ✕.
 *
 * Initial-letter avatar by design decision (D9) — no GitHub picture URL, so no Keycloak attribute-mapper
 * dependency. The avatar's indigo gradient is set in CSS (`.identity-chip__avatar`).
 */
object IdentityChip:

  final case class Props(username: String, onSignOut: Callback)

  val Component = ScalaFnComponent[Props] { props =>
    val initial =
      props.username.headOption.map(_.toUpper.toString).getOrElse("?")

    <.span(
      ^.className := "identity-chip",
      ^.title     := "Signed in via GitHub",
      <.span(^.className := "identity-chip__avatar", initial),
      <.span(^.className := "identity-chip__handle", s"@${props.username}"),
      <.button(
        ^.tpe        := "button",
        ^.className  := "identity-chip__signout",
        ^.title      := "Sign out",
        ^.aria.label := "Sign out",
        ^.onClick --> props.onSignOut,
        LucideIcons.X(LucideIcons.withClass("identity-chip__signout-icon"))
      )
    )
  }
