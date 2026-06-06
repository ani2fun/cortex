package codefolio.client.components.cortex

import codefolio.client.components.Theme
import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * Reading preferences panel + FAB. Four controls, all persisted to localStorage and applied as CSS custom
 * properties on `document.documentElement`:
 *
 *   - **Size** — `s` / `m` / `l` → `--reader-fs: 16.5 / 18 / 21 px` on the prose root.
 *   - **Leading** — `tight` / `comfortable` / `loose` → `--reader-lh: 1.55 / 1.8 / 2.05`.
 *   - **Family** — `serif` (default) / `sans` / `mono` → swaps `--reader-font` between Literata
 *     (`--font-read`), the site sans (`--font-sans`), and JetBrains Mono (`--font-code`). Instrument Serif
 *     italic is too heavy for body text — used only for display.
 *   - **Dark mode** — a second surface for the global light/dark theme. Delegates to [[Theme]] (the
 *     `<html>.dark` class + `localStorage["theme"]`); it does **not** own a separate pref, so it can never
 *     fight the header/footer toggles.
 *
 * Triggered by the Aa FAB in the right-edge fab stack OR the `T` key (skipped inside editable targets). Panel
 * is dismissed by `Esc`, clicks outside, or pressing `T`/Aa again.
 *
 * Dispatches `cortex:typePrefsChanged` after every apply so the mini-map can rebuild its tick positions
 * (prose height shifts with font size).
 */
object CortexTypePrefs:

  private val StorageKey = "cortex-reader.typePrefs"

  private val SizeMap = Map("s" -> "16.5px", "m" -> "18px", "l" -> "21px")
  private val LeadMap = Map("tight" -> "1.55", "comfortable" -> "1.8", "loose" -> "2.05")

  private val FontMap = Map(
    "serif" -> "var(--font-read)",
    "sans"  -> "var(--font-sans)",
    "mono"  -> "var(--font-code)"
  )

  private case class Prefs(size: String, lead: String, font: String):
    def withSize(s: String): Prefs = copy(size = s)
    def withLead(l: String): Prefs = copy(lead = l)
    def withFont(f: String): Prefs = copy(font = f)

  private val Defaults = Prefs("m", "comfortable", "serif")

  private def readPrefs(): Prefs =
    Try {
      val raw = dom.window.localStorage.getItem(StorageKey)
      if raw == null then Defaults
      else
        val obj = js.JSON.parse(raw).asInstanceOf[js.Dynamic]
        Prefs(
          size = Option(obj.size.asInstanceOf[String]).getOrElse(Defaults.size),
          lead = Option(obj.lead.asInstanceOf[String]).getOrElse(Defaults.lead),
          font = Option(obj.font.asInstanceOf[String]).getOrElse(Defaults.font)
        )
    }.getOrElse(Defaults)

  private def writePrefs(p: Prefs): Unit =
    val obj = js.Dynamic.literal(size = p.size, lead = p.lead, font = p.font)
    Try(dom.window.localStorage.setItem(StorageKey, js.JSON.stringify(obj))).getOrElse(())

  /**
   * Apply prefs by writing CSS custom properties to the documentElement and toggling the dark class.
   * Dispatches a `cortex:typePrefsChanged` event so dependent widgets (mini-map) can re-measure.
   */
  private def applyPrefs(p: Prefs): Unit =
    val style = dom.document.documentElement.asInstanceOf[js.Dynamic].style
    style.setProperty("--reader-fs", SizeMap.getOrElse(p.size, "18px"))
    style.setProperty("--reader-lh", LeadMap.getOrElse(p.lead, "1.8"))
    style.setProperty("--reader-font", FontMap.getOrElse(p.font, "var(--font-read)"))
    val init = (new js.Object).asInstanceOf[dom.CustomEventInit]
    init.detail = p.size
    val ev = new dom.CustomEvent("cortex:typePrefsChanged", init)
    val _  = dom.window.dispatchEvent(ev)

  private def isEditableTarget(e: dom.KeyboardEvent): Boolean =
    val t = e.target.asInstanceOf[dom.Element]
    t != null && (t.matches("input, textarea, [contenteditable], [contenteditable='true']"))

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(false)
      .useState(Defaults)
      .useState(Theme.Mode.Light: Theme.Mode)
      .useEffectOnMountBy { (_, _, prefsS, _) =>
        Callback {
          val initial = readPrefs()
          prefsS.setState(initial).runNow()
          applyPrefs(initial)
        }
      }
      // Theme is owned globally by `Theme`; mirror it into local state and keep
      // it live via `theme:changed` so the Dark-mode switch always reflects the
      // real theme — even when it was changed from the header toggle.
      .useEffectOnMountBy { (_, _, _, themeS) =>
        Callback {
          themeS.setState(Theme.current).runNow()
          val onChange: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            themeS.setState(Theme.current).runNow()
          dom.window.addEventListener(Theme.ChangedEvent, onChange)
          ()
        }
      }
      .useEffectOnMountBy { (_, openS, _, _) =>
        Callback {
          val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
            if !isEditableTarget(e) then
              if (e.key == "t" || e.key == "T") && !e.metaKey && !e.ctrlKey && !e.altKey then
                e.preventDefault()
                openS.setState(!openS.value).runNow()
              else if e.key == "Escape" && openS.value then openS.setState(false).runNow()
          dom.window.addEventListener("keydown", onKey, useCapture = false)
          ()
        }
      }
      // Click outside dismisses the panel. The FAB stops propagation so clicking it stays as the
      // toggle path rather than dismissing.
      .useEffectOnMountBy { (_, openS, _, _) =>
        Callback {
          val onDown: js.Function1[dom.MouseEvent, Unit] = (e: dom.MouseEvent) =>
            if openS.value then
              val t     = e.target.asInstanceOf[dom.Element]
              val panel = dom.document.querySelector(".cortex-reader-type-prefs")
              val fab   = dom.document.querySelector(".cortex-reader-type-prefs-fab")
              val inside =
                (panel != null && t != null && panel.contains(t)) ||
                  (fab != null && t != null && fab.contains(t))
              if !inside then openS.setState(false).runNow()
          dom.document.addEventListener("mousedown", onDown, useCapture = false)
          ()
        }
      }
      .render { (_, openS, prefsS, themeS) =>
        val p           = prefsS.value
        val themeIsDark = themeS.value == Theme.Mode.Dark

        def setAndPersist(next: Prefs): Callback = Callback {
          prefsS.setState(next).runNow()
          writePrefs(next)
          applyPrefs(next)
        }

        def seg(group: String, current: String, value: String, label: String): VdomNode =
          <.button(
            ^.key               := s"$group-$value",
            ^.tpe               := "button",
            VdomAttr("data-tc") := group,
            VdomAttr("data-v")  := value,
            ^.aria.pressed      := (current == value),
            ^.onClick --> setAndPersist(group match
              case "size" => p.withSize(value)
              case "lead" => p.withLead(value)
              case "font" => p.withFont(value)
              case _      => p
            ),
            label
          )

        <.div(
          // FAB
          <.button(
            ^.tpe           := "button",
            ^.className     := "cortex-reader-type-prefs-fab",
            ^.aria.label    := "Reading preferences (T)",
            ^.aria.expanded := openS.value,
            ^.aria.controls := "cortex-reader-type-prefs",
            ^.onClick --> openS.setState(!openS.value),
            LucideIcons.Type(LucideIcons.withClass("cortex-reader-type-prefs-fab__icon"))
          ),
          // Panel
          <.aside(
            ^.id                  := "cortex-reader-type-prefs",
            ^.className           := "cortex-reader-type-prefs",
            VdomAttr("data-open") := (if openS.value then "true" else "false"),
            ^.role                := "dialog",
            ^.aria.label          := "Reading preferences",
            <.h3(^.className := "cortex-reader-type-prefs__title", "Reading preferences"),
            // Size
            <.div(
              ^.className := "cortex-reader-type-prefs__group",
              <.div(^.className := "cortex-reader-type-prefs__label", <.span("Size")),
              <.div(
                ^.className  := "cortex-reader-type-prefs__seg",
                ^.role       := "radiogroup",
                ^.aria.label := "Font size",
                seg("size", p.size, "s", "Small"),
                seg("size", p.size, "m", "Medium"),
                seg("size", p.size, "l", "Large")
              )
            ),
            // Leading
            <.div(
              ^.className := "cortex-reader-type-prefs__group",
              <.div(^.className := "cortex-reader-type-prefs__label", <.span("Leading")),
              <.div(
                ^.className  := "cortex-reader-type-prefs__seg",
                ^.role       := "radiogroup",
                ^.aria.label := "Line height",
                seg("lead", p.lead, "tight", "Tight"),
                seg("lead", p.lead, "comfortable", "Comfortable"),
                seg("lead", p.lead, "loose", "Loose")
              )
            ),
            // Family
            <.div(
              ^.className := "cortex-reader-type-prefs__group",
              <.div(^.className := "cortex-reader-type-prefs__label", <.span("Type family")),
              <.div(
                ^.className  := "cortex-reader-type-prefs__seg cortex-reader-type-prefs__seg--family",
                ^.role       := "radiogroup",
                ^.aria.label := "Type family",
                seg("font", p.font, "serif", "Serif"),
                seg("font", p.font, "sans", "Sans"),
                seg("font", p.font, "mono", "Mono")
              )
            ),
            // Dark mode toggle — delegates to the global `Theme` (single source
            // of truth). `Theme.set` broadcasts `theme:changed`; the listener
            // above refreshes `themeS`, which re-renders this switch and every
            // other toggle surface in lock-step.
            <.div(
              ^.className := "cortex-reader-type-prefs__group",
              <.button(
                ^.tpe          := "button",
                ^.className    := "cortex-reader-type-prefs__row",
                ^.aria.pressed := themeIsDark,
                ^.onClick --> Theme.set(
                  if themeIsDark then Theme.Mode.Light else Theme.Mode.Dark
                ),
                <.span(^.className := "cortex-reader-type-prefs__row-label", "Dark mode"),
                <.span(^.className := "cortex-reader-type-prefs__switch", ^.aria.hidden := true)
              )
            )
          )
        )
      }
