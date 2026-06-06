package cortex.client.components.ui

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Section wrapper used by every home-page section.
 *
 * Sets the `id` for hash-link navigation (Header anchors target it via `goToAnchor`) and applies the BEM
 * block class. Section-specific spacing — `pt-32`, `scroll-mt-24`, etc. — belongs in the .block class itself,
 * not in this primitive.
 */
object Section:

  def apply(id: String, blockClass: String)(children: VdomNode*): VdomElement =
    <.section(^.id := id, ^.className := blockClass, children.toTagMod)
