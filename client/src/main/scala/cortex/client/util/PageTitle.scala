package cortex.client.util

import japgolly.scalajs.react.*
import org.scalajs.dom

/**
 * Tiny effect helper: set `document.title` on mount and reset to a default on unmount. Use from any page
 * component:
 *
 * {{{
 * .useEffectOnMountBy(_ => PageTitle.set("Cortex — Aniket Kakde"))
 * }}}
 */
object PageTitle:

  val Default: String = "Aniket Kakde — Backend-leaning Software Engineer"

  /** Set `document.title`. Returns a Callback so it composes inside hooks. */
  def set(title: String): Callback = Callback {
    if dom.document.title != title then dom.document.title = title
  }
