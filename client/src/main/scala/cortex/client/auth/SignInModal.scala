package cortex.client.auth

import cortex.client.components.icons.{BrandIcons, LucideIcons}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * The GitHub sign-in modal — the moment "Edit" turns into a runnable thing.
 *
 * Rendered once, at the app root (by [[cortex.client.Main]]). It subscribes to [[AuthStore]] and shows itself
 * only while `modalOpen` is true, so deep components (`RunnableCodeBlock`) trigger it with a plain
 * `AuthStore.openSignIn()` and never have to thread modal state through props.
 *
 * Copy is editorial, ported verbatim from the Claude Design `Cortex Auth` artboard — italic-serif title, mono
 * eyebrow, plain-English lede, one indigo accent. The only deviation: the requested scope reads `user:email`
 * (what the realm's GitHub IdP actually asks for) rather than the design's `read:user`.
 */
object SignInModal:

  private val GithubSettingsUrl = "https://github.com/settings/applications"

  val Host =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(AuthStore.current)
      // Mirror AuthStore into local state so the modal re-renders on open/close/phase changes.
      .useEffectOnMountBy { (_, snap) =>
        CallbackTo {
          val unsubscribe = AuthStore.subscribe(s => snap.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      // Escape closes the modal (matches the design + standard dialog behaviour).
      .useEffectOnMountBy { (_, _) =>
        CallbackTo {
          val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
            if e.key == "Escape" && AuthStore.current.modalOpen then AuthStore.closeSignIn()
          dom.window.addEventListener("keydown", onKey)
          Callback(dom.window.removeEventListener("keydown", onKey))
        }
      }
      .render { (_, snap) =>
        val s = snap.value
        if !s.modalOpen then EmptyVdom
        else
          val redirecting = s.modalPhase == AuthStore.ModalPhase.Redirecting

          val onScrimClick: ReactMouseEvent => Callback = e =>
            if e.target == e.currentTarget then Callback(AuthStore.closeSignIn())
            else Callback.empty

          <.div(
            ^.className  := "sign-in-modal",
            ^.role       := "dialog",
            ^.aria.modal := true,
            ^.onClick ==> onScrimClick,
            <.div(
              ^.className := "sign-in-modal__card",
              // ── head ──────────────────────────────────────────────
              <.div(
                ^.className := "sign-in-modal__head",
                <.button(
                  ^.tpe        := "button",
                  ^.className  := "sign-in-modal__close",
                  ^.aria.label := "Close",
                  ^.onClick --> Callback(AuthStore.closeSignIn()),
                  LucideIcons.X(LucideIcons.withClass("sign-in-modal__close-icon"))
                ),
                <.div(
                  ^.className := "sign-in-modal__eyebrow",
                  <.span(^.className := "sign-in-modal__dot"),
                  "Cortex · Identify yourself"
                ),
                <.h2(
                  ^.className := "sign-in-modal__title",
                  "Sign in to ",
                  <.em("edit and run"),
                  "."
                ),
                <.p(
                  ^.className := "sign-in-modal__lede",
                  "Reading and running the snippet as-written are open. The moment you ",
                  <.em("edit"),
                  " the code, the sandbox needs a name on the request — so your run gets its own quota."
                )
              ),
              // ── CTA (full-width) ──────────────────────────────────
              <.button(
                ^.tpe := "button",
                ^.className := "sign-in-modal__cta" +
                  (if redirecting then " sign-in-modal__cta--loading" else ""),
                ^.disabled := redirecting,
                ^.onClick --> Callback(AuthStore.signIn()),
                if redirecting then
                  TagMod(
                    LucideIcons.Loader2(LucideIcons.withClass("sign-in-modal__cta-icon animate-spin")),
                    "Redirecting to GitHub…"
                  )
                else
                  TagMod(
                    BrandIcons.Github("sign-in-modal__cta-icon"),
                    "Continue with GitHub"
                  )
              ),
              // ── scope strip: exactly what GitHub is asked for ─────
              <.div(
                ^.className := "sign-in-modal__scope",
                LucideIcons.ShieldCheck(LucideIcons.withClass("sign-in-modal__scope-icon")),
                <.p(
                  ^.className := "sign-in-modal__scope-text",
                  <.b("Your handle, name & verified email"),
                  " — scope ",
                  <.code("user:email"),
                  ", nothing else. No repo access, no writes on your behalf. The token never leaves the " +
                    "sandbox."
                )
              ),
              // ── foot: fine print + skip ───────────────────────────
              <.div(
                ^.className := "sign-in-modal__foot",
                <.span(
                  ^.className := "sign-in-modal__fine",
                  "OAuth via GitHub — kakde.eu never sees your password; revoke any time from ",
                  <.a(
                    ^.href   := GithubSettingsUrl,
                    ^.target := "_blank",
                    ^.rel    := "noreferrer",
                    "GitHub → Settings → Applications"
                  ),
                  "."
                ),
                <.button(
                  ^.tpe       := "button",
                  ^.className := "sign-in-modal__skip",
                  ^.onClick --> Callback(AuthStore.closeSignIn()),
                  "Not now"
                )
              )
            )
          )
      }
