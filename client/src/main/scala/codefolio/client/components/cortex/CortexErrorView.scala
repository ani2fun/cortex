package codefolio.client.components.cortex

import japgolly.scalajs.react.vdom.html_<^.*

/**
 * The destructive-text + "← Back to Cortex" pair shown when a Cortex page's API call fails. Returned as a
 * `TagMod` so callers control the outer wrapper — `ChapterPage` puts it inside `<.main(container ...)` along
 * with its own loading branch; `BookRedirectPage` puts it inside the same outer `<.main` it uses for the
 * loading branch.
 */
object CortexErrorView:

  def apply(message: String): TagMod =
    TagMod(
      <.p(^.className := "text-destructive font-semibold", message),
      <.p(
        ^.className := "mt-4 text-sm",
        <.a(
          ^.href      := "/cortex",
          ^.className := "text-primary hover:underline",
          "← Back to Cortex"
        )
      )
    )
