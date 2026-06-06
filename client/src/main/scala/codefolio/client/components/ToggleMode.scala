package codefolio.client.components

import codefolio.client.components.icons.LucideIcons
import codefolio.client.components.ui.Button
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Dark/light theme toggle button. Mirrors portfolio-app's ToggleMode:
 *   - Outline button at icon size
 *   - Sun icon when dark (clicking switches to light), Moon when light
 *   - On the very first render we show a disabled placeholder, since the `<html>` class might not yet be
 *     observable until the inline-script bootstrap has run; this mirrors the `mounted` guard in the original.
 *
 * Local component state mirrors `Theme.current` and is updated in-step with `Theme.toggle`, which keeps this
 * component re-rendering in sync with the actual <html> class. Components elsewhere that depend on the theme
 * should rely on Tailwind's `dark:` selectors and re-render via their own state.
 */
object ToggleMode:

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(false)                        // mounted
      .useState(Theme.Mode.Light: Theme.Mode) // current — initialised post-mount
      .useEffectOnMountBy { (_, mounted, mode) =>
        mode.setState(Theme.current) >> mounted.setState(true)
      }
      // Resync on every theme change — including ones made by *other* toggle
      // instances or the Cortex reading-prefs panel. The local `mode` state is
      // only a render mirror of `<html>`; this listener keeps it from drifting,
      // which is what made the toggle need two clicks to "catch up".
      .useEffectOnMountBy { (_, _, mode) =>
        Callback {
          val onChange: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            mode.setState(Theme.current).runNow()
          dom.window.addEventListener(Theme.ChangedEvent, onChange)
          ()
        }
      }
      .render { (_, mounted, modeS) =>
        if !mounted.value then
          <.button(
            ^.className  := Button.classes(Button.Variant.Outline, Button.Size.Icon),
            ^.disabled   := true,
            ^.aria.label := "Theme toggle"
          )
        else
          val isDark   = modeS.value == Theme.Mode.Dark
          val nextMode = if isDark then Theme.Mode.Light else Theme.Mode.Dark
          // `Theme.set` broadcasts `theme:changed`; the listener above re-reads
          // `<html>` into `modeS`, so the click handler doesn't update state
          // itself — that keeps every toggle surface on one code path.
          val onClick = Theme.set(nextMode)
          <.button(
            ^.className  := Button.classes(Button.Variant.Outline, Button.Size.Icon),
            ^.aria.label := (if isDark then "Switch to light mode" else "Switch to dark mode"),
            ^.onClick --> onClick,
            if isDark then
              LucideIcons.Sun(LucideIcons.withClass("hover:cursor-pointer hover:text-primary"))
            else
              LucideIcons.Moon(LucideIcons.withClass("hover:cursor-pointer hover:text-primary"))
          )
      }
