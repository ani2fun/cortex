package cortex.client.util

import japgolly.scalajs.react.{Callback, ReactMouseEvent}
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * In-page hash-anchor scroll helpers used by the chapter reader's TOC and in-article anchor links.
 *
 * The "instant" scroll behaviour is the load-bearing detail: a page-wide `scroll-behavior: smooth` CSS rule
 * fights with scalajs-react Router's link interception — the router sees the `#anchor` URL change and
 * re-renders before the browser finishes its smooth scroll, which cancels the animation. Forcing `behavior:
 * 'instant'` sidesteps that. All three click sites and the on-mount fragment scroll go through here so the
 * workaround lives in one place.
 *
 * Click handlers are no-ops when the target element doesn't exist — in that case we let the click fall
 * through to default browser behaviour rather than swallowing it.
 */
object HashScroll:

  /** Scroll the element with id `slug` into view (instant, top-aligned). No-op if missing. */
  def scrollTo(slug: String): Unit =
    val el = dom.document.getElementById(slug)
    if el != null then
      val opts = js.Dynamic.literal(behavior = "instant", block = "start")
      val _    = el.asInstanceOf[js.Dynamic].scrollIntoView(opts)

  /**
   * Read the current URL fragment and scroll to it if it points at an existing element. Used on first mount
   * so a hard reload of `/cortex/foo/bar#section` lands at the section. Tolerant of malformed URI escapes —
   * an undecodable fragment is used as-is.
   */
  def scrollToCurrentHash(): Unit =
    val hash = dom.window.location.hash
    if hash != null && hash.length > 1 then
      val rawSlug = hash.substring(1)
      val slug    = Try(decodeURIComponent(rawSlug)).getOrElse(rawSlug)
      scrollTo(slug)

  /**
   * In-page hash-link click handler for React-mounted links. Prevents the default + stops propagation,
   * scrolls the target into view, and pushes the hash to history via `replaceState` (no router re-render).
   */
  def onHashLinkClick(e: ReactMouseEvent, slug: String): Callback =
    Callback(handleClick(slug, () => e.preventDefault(), () => e.stopPropagation()))

  /**
   * Like [[onHashLinkClick]] but for native `addEventListener` handlers, which receive a `dom.MouseEvent`
   * rather than scalajs-react's wrapped `ReactMouseEvent`.
   */
  def onHashLinkNative(e: dom.MouseEvent, slug: String): Unit =
    handleClick(slug, () => e.preventDefault(), () => e.stopPropagation())

  private def handleClick(slug: String, prevent: () => Unit, stop: () => Unit): Unit =
    val el = dom.document.getElementById(slug)
    if el != null then
      prevent()
      stop()
      val opts = js.Dynamic.literal(behavior = "instant", block = "start")
      el.asInstanceOf[js.Dynamic].scrollIntoView(opts)
      dom.window.history.replaceState(null, "", s"#$slug")

  private def decodeURIComponent(s: String): String =
    js.Dynamic.global.decodeURIComponent(s).asInstanceOf[String]
