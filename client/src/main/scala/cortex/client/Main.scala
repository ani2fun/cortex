package cortex.client

import cortex.client.auth.{AuthBoot, SignInModal}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.ReactDOMClient
import org.scalajs.dom

/**
 * SPA bootstrap.
 *
 * Vite/Scala.js calls `Main.main(Array.empty)` once when the bundle loads. We grab `<div id="root">` (defined
 * in `client/index.html`) and create a single React root that owns the entire SPA tree.
 *
 * The root renders two siblings under a fragment:
 *   - [[Router.Component]] — routing, page lookup, layout;
 *   - [[SignInModal.Host]] — the app-level GitHub sign-in modal, shown on demand via `AuthStore`.
 *
 * [[AuthBoot.run]] fires once here (before render) to resolve `/api/auth/config` and, if auth is enabled,
 * adopt any existing Keycloak session. It mounts above the router so a route change never re-boots auth.
 *
 * Look at this file second when onboarding (after `Router.scala`); it's intentionally tiny so the routing
 * surface stays the obvious entry.
 */
object Main:

  def main(args: Array[String]): Unit =
    val container = dom.document.getElementById("root")
    // Defensive: in tests / SSR-like scenarios `#root` might not exist yet.
    // In normal operation the index.html guarantees it, so this is just a
    // belt-and-braces noop rather than an error path worth surfacing.
    if container != null then
      AuthBoot.run()
      val root = ReactDOMClient.createRoot(container)
      root.render(
        React.Fragment(
          Router.Component(),
          SignInModal.Host()
        )
      )
