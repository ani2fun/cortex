package cortex.client.components

import japgolly.scalajs.react.*
import org.scalajs.dom

/**
 * Replacement for next-themes. The actual *initial* theme is set by the inline script in `client/index.html`
 * (which runs synchronously before any module script and avoids the flash-of-incorrect-theme), so this
 * object's job is just runtime toggling: read/write `<html class="dark">` and persist the choice to
 * `localStorage` under the same key the inline script reads.
 */
object Theme:

  enum Mode:
    case Light, Dark

  private val StorageKey = "theme"

  /**
   * Window event broadcast after every theme change. There is no shared store — the `<html>` class is the
   * single source of truth — so each mounted theme surface (the header/footer toggles, the Cortex
   * reading-prefs panel) listens for this and re-reads [[current]] to stay in sync with a change made by a
   * peer. Without it, a toggle's local state drifts from `<html>` and the next click appears to do nothing.
   */
  val ChangedEvent: String = "theme:changed"

  /** Read the theme currently applied to <html>. Stable to call at any time. */
  def current: Mode =
    if dom.document.documentElement.classList.contains("dark") then Mode.Dark
    else Mode.Light

  /** Apply a theme — flip the class on <html>, persist to localStorage, and broadcast [[ChangedEvent]]. */
  def set(mode: Mode): Callback = Callback {
    val isDark = mode == Mode.Dark
    dom.document.documentElement.classList.toggle("dark", isDark)
    try dom.window.localStorage.setItem(StorageKey, if isDark then "dark" else "light")
    catch case _: Throwable => () // private mode / disabled storage
    dom.window.dispatchEvent(new dom.Event(ChangedEvent))
  }

  /** Toggle between light and dark. */
  def toggle: Callback =
    set(current match
      case Mode.Dark  => Mode.Light
      case Mode.Light => Mode.Dark
    )
