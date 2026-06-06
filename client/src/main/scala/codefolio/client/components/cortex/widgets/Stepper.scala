package codefolio.client.components.cortex.widgets

import japgolly.scalajs.react.*
import japgolly.scalajs.react.Reusability.DecimalImplicitsWithoutTolerance.reusabilityDouble
import org.scalajs.dom

/**
 * Shared play-loop + step-state machine for D3 Widgets that animate over discrete steps.
 *
 * The catalog widgets (Array Traversal, Linked-list, Partition Simulator, …) all run the same dance: a step
 * index, a `playing` flag, a `setTimeout` that ticks the index forward, and a control bar wired to prev /
 * next / reset / play / pause / jump callbacks. Each widget used to own its own copy; this hook owns the
 * machinery so a widget supplies only `(stepCount, delayMs)` and renders its own SVG + control chrome over
 * the returned [[Output]].
 *
 * Per-widget tempo stays per-widget: a widget that wants speed control owns its own `useState(speed)` and
 * feeds the resulting `delayMs` back into [[Input]]. The hook reschedules the timer whenever `delayMs`
 * changes (it's in the effect's dep tuple) so a 2× toggle takes effect immediately, not at the next tick.
 *
 * What stays inside the hook: the step-index state, the playing flag, the timeout id ref, the play-loop
 * effect, and the four state-mutating callbacks. What stays outside: parsing the widget's payload, the SVG
 * render effect, the speed-multiplier UI (when present), and the per-widget control-bar markup — those are
 * widget-specific styling and behaviour the hook deliberately doesn't constrain.
 */
object Stepper:

  /**
   * Reactive input — the play-loop effect's deps include every field, so a change to either restarts the
   * timer with the new value.
   *
   *   - `stepCount`: total steps the widget can scrub through. Hidden when ≤ 1 (the control bar is the
   *     widget's call; the hook just returns sane values).
   *   - `delayMs`: how long each "playing" tick waits before advancing. Widget computes this — e.g.
   *     `StepDelayMs / max(0.25, speed)` for a speed-multiplier UI.
   */
  final case class Input(stepCount: Int, delayMs: Double)

  /**
   * What the calling widget gets back. The `index` is already clamped into `[0, stepCount - 1]` (or 0 when
   * `stepCount == 0`), so the widget can pass it straight into `spec.steps(index)` for rendering.
   *
   * Each callback bundles "pause then mutate" — so a user clicking *Next* while playing pauses too, matching
   * the original widget behaviour.
   */
  final case class Output(
      index: Int,
      isPlaying: Boolean,
      atStart: Boolean,
      atEnd: Boolean,
      previous: Callback,
      next: Callback,
      reset: Callback,
      togglePlay: Callback,
      jumpTo: Int => Callback
  )

  val hook: CustomHook[Input, Output] =
    CustomHook[Input]
      .useState(0)                      // step index (raw — render uses the clamped Output.index)
      .useState(false)                  // playing
      .useRefBy(_ => Option.empty[Int]) // play timeout id
      .useEffectWithDepsBy((input, indexS, playingS, _) =>
        (input.stepCount, indexS.value, playingS.value, input.delayMs)
      ) { (_, indexS, playingS, timeoutRef) => (count, index, playing, delayMs) =>
        Callback {
          timeoutRef.value.foreach(dom.window.clearTimeout)
          timeoutRef.value = None
          if playing then
            if index >= count - 1 then playingS.setState(false).runNow()
            else
              val id = dom.window.setTimeout(
                () => indexS.setState(index + 1).runNow(),
                delayMs
              )
              timeoutRef.value = Some(id)
        }
      }
      .buildReturning { (input, indexS, playingS, _) =>
        val count     = input.stepCount
        val safeCount = math.max(1, count)
        val idx       = clamp(indexS.value, safeCount)
        val atStart   = idx == 0
        val atEnd     = count == 0 || idx == count - 1
        val previous  = playingS.setState(false) >> indexS.modState(i => clamp(i - 1, safeCount))
        val next      = playingS.setState(false) >> indexS.modState(i => clamp(i + 1, safeCount))
        val reset     = playingS.setState(false) >> indexS.setState(0)
        val togglePlay =
          if playingS.value then playingS.setState(false)
          else
            val rewind = if atEnd then indexS.setState(0) else Callback.empty
            rewind >> playingS.setState(true)
        val jumpTo = (i: Int) => playingS.setState(false) >> indexS.setState(clamp(i, safeCount))
        Output(idx, playingS.value, atStart, atEnd, previous, next, reset, togglePlay, jumpTo)
      }

  private def clamp(i: Int, safeCount: Int): Int =
    math.max(0, math.min(safeCount - 1, i))
