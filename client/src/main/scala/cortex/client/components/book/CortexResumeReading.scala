package cortex.client.components.book

import cortex.client.components.icons.LucideIcons
import cortex.client.util.ReaderState
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
 * Bottom-centred "Continue where you left off" toast — shown on chapter load when a per-pathname scroll
 * position is persisted in localStorage and the saved fraction is between 5% and 95% (outside that band means
 * "fresh page" or "essentially done").
 *
 *   - Persisted as a fraction (0.0–1.0) under `cortex-reader.resume:<pathname>`.
 *   - Toast appears 600ms after mount (lets layout settle); auto-dismisses after 8s if untouched.
 *   - Resume → `window.scrollTo({ top: scrollHeight * fraction, behavior: smooth })`.
 *   - X → dismiss (no persistence change).
 *
 * Per chat-4 user feedback, the percentage indicator was dropped from the toast — the bare "Continue where
 * you left off · Resume" copy stays.
 */
object CortexResumeReading:

  private val ShowDelayMs       = 600
  private val AutoHideMs        = 8000
  private val PersistThrottleMs = 220

  private val MinFraction = 0.05
  private val MaxFraction = 0.95

  private def storageKey: String = s"cortex-reader.resume:${dom.window.location.pathname}"

  private def readSaved(): Option[Double] =
    Try {
      val raw = dom.window.localStorage.getItem(storageKey)
      if raw == null then None
      else Try(raw.toDouble).toOption.filter(f => f > MinFraction && f < MaxFraction)
    }.toOption.flatten

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(false)
      .useState(0.0)
      .useEffectOnMountBy { (_, openS, savedS) =>
        Callback {
          readSaved() match
            case Some(f) =>
              savedS.setState(f).runNow()
              dom.window.setTimeout(() => openS.setState(true).runNow(), ShowDelayMs.toDouble)
              dom.window.setTimeout(() => openS.setState(false).runNow(), AutoHideMs.toDouble)
            case None => ()
        }
      }
      // Persist scroll fraction on every (throttled) scroll. Uses the same `ReaderState` helper as
      // the rest of the layout — no extra observation cost.
      .useEffectOnMountBy { (_, _, _) =>
        Callback {
          var pending: js.UndefOr[Int] = js.undefined
          val onScroll: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
            pending.foreach(dom.window.clearTimeout)
            pending = dom.window.setTimeout(
              () => {
                val f = ReaderState.currentScrollFraction()
                Try(dom.window.localStorage.setItem(storageKey, f.toString)).getOrElse(())
              },
              PersistThrottleMs.toDouble
            )
          dom.window.addEventListener("scroll", onScroll, useCapture = false)
          ()
        }
      }
      .render { (_, openS, savedS) =>
        val onResume: Callback = Callback {
          val doc    = dom.document.documentElement
          val total  = doc.scrollHeight.toDouble - doc.clientHeight.toDouble
          val target = (total * savedS.value).max(0.0)
          val opts   = js.Dynamic.literal(top = target, behavior = "smooth").asInstanceOf[js.Object]
          dom.window.asInstanceOf[js.Dynamic].scrollTo(opts)
          openS.setState(false).runNow()
        }
        val onDismiss: Callback = openS.setState(false)

        <.div(
          ^.className           := "cortex-reader-resume",
          VdomAttr("data-open") := (if openS.value then "true" else "false"),
          ^.role                := "status",
          VdomAttr("aria-live") := "polite",
          <.span(^.className := "cortex-reader-resume__label", "Continue where you left off"),
          <.button(
            ^.tpe       := "button",
            ^.className := "cortex-reader-resume__btn",
            ^.onClick --> onResume,
            "Resume"
          ),
          <.button(
            ^.tpe        := "button",
            ^.className  := "cortex-reader-resume__close",
            ^.aria.label := "Dismiss",
            ^.onClick --> onDismiss,
            LucideIcons.X(LucideIcons.withClass("cortex-reader-resume__close-icon"))
          )
        )
      }
