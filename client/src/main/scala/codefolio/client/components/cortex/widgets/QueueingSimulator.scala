package codefolio.client.components.cortex.widgets

import codefolio.client.components.cortex.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try

/**
 * Queueing simulator — visualises the M/M/1 latency cliff `W = (1/µ) / (1 − ρ)` as a horizontal bar chart of
 * inflation factors across discrete utilisation samples, plus a live slider for the reader's current `ρ` that
 * highlights the matching bar and surfaces the corresponding mean response time / queue length / "danger"
 * verdict.
 *
 * Companion to lesson 5's `examples/05-littles-law-queueing/` Python simulator. The Python project does the
 * full event-driven Monte Carlo so you can compare empirical samples against the theory; this widget is the
 * *inline preview* that makes the cliff legible without leaving the page.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":         "M/M/1 latency vs utilisation — drag ρ to feel the cliff",
 *   "serviceTimeMs": 50,
 *   "initialRho":    0.7
 * }
 * }}}
 *
 *   - `serviceTimeMs` (1/µ) sets the baseline service time; the bar chart shows latency multipliers, the
 *     readout shows absolute milliseconds.
 *   - `initialRho` is the slider's starting position. Defaults to 0.7 (the standard production-target
 *     shoulder just before the cliff steepens).
 *
 * SVG is built as a string and injected via `dangerouslySetInnerHTML` — same pattern Mermaid + D2 +
 * ArrayTraversal + the other widgets use. State is just the current slider ρ; the bar chart and readout both
 * derive from it.
 */
object QueueingSimulator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Spec(title: Option[String], serviceTimeMs: Double, initialRho: Double)

  final case class Props(payload: String)

  // Discrete ρ samples for the bar chart. Chosen so the reader sees the curve flat through 0.7 and explode
  // past 0.85 — the same shape the lesson's M/M/1 sweep table walks through.
  private val RhoSamples: List[Double] = List(0.10, 0.30, 0.50, 0.70, 0.85, 0.90, 0.93, 0.95, 0.97, 0.99)

  // ===========================================================================
  // Layout constants
  // ===========================================================================

  private val ViewBoxWidth     = 720.0
  private val ViewBoxHeight    = 260.0
  private val LeftPad          = 56.0
  private val RightPad         = 24.0
  private val TopPad           = 22.0
  private val ChartBottom      = 200.0 // y of the x-axis (bar baseline)
  private val MaxVisibleFactor = 12.0  // bars beyond this clip and overflow

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val svcMs   = d.double("serviceTimeMs", 50.0)
      val initRho = d.double("initialRho", 0.7)
      if svcMs <= 0 then throw PayloadDecoder.invalid("serviceTimeMs must be > 0")
      if initRho < 0 then throw PayloadDecoder.invalid("initialRho must be ≥ 0")
      if initRho >= 1 then
        throw PayloadDecoder.invalid("initialRho must be < 1 (ρ=1 has no stable solution)")
      Spec(title = d.optString("title"), serviceTimeMs = svcMs, initialRho = initRho)
    }

  // ===========================================================================
  // M/M/1 maths
  // ===========================================================================

  /** Latency inflation: how many times longer the response is than the service time. */
  private def inflation(rho: Double): Double =
    if rho >= 1.0 then Double.PositiveInfinity else 1.0 / (1.0 - rho)

  /** Mean response time in ms. */
  private def meanW(rho: Double, serviceMs: Double): Double = serviceMs * inflation(rho)

  /** Mean number of jobs queued (waiting only, not currently being served): L_q = ρ² / (1 − ρ). */
  private def meanQueueLength(rho: Double): Double =
    if rho >= 1.0 then Double.PositiveInfinity else (rho * rho) / (1.0 - rho)

  /** Verdict band for the readout. Aligns with the lesson's "production target ρ ≈ 0.6–0.7" rule. */
  private def verdict(rho: Double): (String, String) =
    if rho <= 0.6 then ("Comfortable", "safe")
    else if rho <= 0.75 then ("Production target", "ok")
    else if rho <= 0.85 then ("On the shoulder", "warn")
    else if rho <= 0.95 then ("On the cliff", "danger")
    else ("Disaster", "danger")

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  // Closest sample to the current slider — that's the highlighted bar.
  private def selectedSample(currentRho: Double): Double =
    RhoSamples.minBy(r => math.abs(r - currentRho))

  private def axisSvg: String =
    val y      = ChartBottom
    val gridYs = List(0.0, 0.25, 0.5, 0.75, 1.0).map(f => y - f * (y - TopPad))
    val gridLines = gridYs
      .map(gy =>
        s"""<line class="queueing-simulator__grid" x1="$LeftPad" y1="$gy" x2="${ViewBoxWidth - RightPad}" y2="$gy"/>"""
      )
      .mkString("\n")
    val xAxis =
      s"""<line class="queueing-simulator__axis" x1="$LeftPad" y1="$y" x2="${ViewBoxWidth - RightPad}" y2="$y"/>"""
    val yLabels = List(
      (0.0, "1×"),
      (0.25, "4×"),
      (0.5, "6×"),
      (0.75, "9×"),
      (1.0, s"${MaxVisibleFactor.toInt}×")
    ).map { case (f, label) =>
      val gy = y - f * (y - TopPad)
      s"""<text class="queueing-simulator__y-label" x="${LeftPad - 8}" y="${gy + 4}" text-anchor="end">${esc(
          label
        )}</text>"""
    }.mkString("\n")
    s"$gridLines\n$xAxis\n$yLabels"

  private def barSvg(rho: Double, selected: Boolean, index: Int, total: Int): String =
    val plotW    = ViewBoxWidth - LeftPad - RightPad
    val gap      = 6.0
    val slot     = plotW / total
    val barW     = slot - gap
    val barX     = LeftPad + index * slot + gap / 2
    val factor   = inflation(rho)
    val visible  = math.min(factor, MaxVisibleFactor)
    val barH     = (visible / MaxVisibleFactor) * (ChartBottom - TopPad)
    val barY     = ChartBottom - barH
    val overflow = factor > MaxVisibleFactor
    val barCls = if selected then "queueing-simulator__bar queueing-simulator__bar--selected"
    else "queueing-simulator__bar"
    val factorLabel =
      if factor.isInfinite then "→ ∞"
      else if factor < 10 then f"$factor%.1f×"
      else f"${factor.toInt}×"
    val factorY = if overflow then TopPad - 4 else barY - 4
    val overflowMarker =
      if overflow then
        // Small "torn" arrow on top of the bar to indicate the value is clipped.
        s"""<text class="queueing-simulator__overflow" x="${barX + barW / 2}" y="${TopPad + 8}" text-anchor="middle">↑</text>"""
      else ""
    val rhoLabel =
      s"""<text class="queueing-simulator__x-label" x="${barX + barW / 2}" y="${ChartBottom + 16}" text-anchor="middle">ρ=${f"$rho%.2f"}</text>"""
    s"""<g>
       |  <rect class="$barCls" x="$barX" y="$barY" width="$barW" height="$barH" rx="2"/>
       |  $overflowMarker
       |  <text class="queueing-simulator__bar-label" x="${barX + barW / 2}" y="$factorY" text-anchor="middle">${esc(
        factorLabel
      )}</text>
       |  $rhoLabel
       |</g>""".stripMargin

  private def buildSvg(selectedRho: Double): String =
    val selected = selectedSample(selectedRho)
    val bars = RhoSamples.zipWithIndex
      .map { case (rho, i) => barSvg(rho, rho == selected, i, RhoSamples.size) }
      .mkString("\n")
    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="queueing-simulator__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $axisSvg
       |  $bars
       |  <text class="queueing-simulator__chart-title" x="$LeftPad" y="${TopPad - 6}" text-anchor="start">Latency multiplier W / (1/µ) — capped at ${MaxVisibleFactor
        .toInt}×; bars above are off-chart</text>
       |</svg>""".stripMargin

  private def formatMs(ms: Double): String =
    if ms.isInfinite then "∞"
    else if ms >= 1_000 then f"${ms / 1_000}%.1f s"
    else if ms >= 10 then f"$ms%.0f ms"
    else f"$ms%.1f ms"

  private def formatQueueLength(l: Double): String =
    if l.isInfinite then "∞"
    else if l >= 100 then f"$l%.0f"
    else if l >= 10 then f"$l%.1f"
    else f"$l%.2f"

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 0.7) // overridden on mount once the spec parses
      .useEffectOnMountBy { (_, specM, rhoS) =>
        specM.value match
          case Right(spec) => rhoS.setState(spec.initialRho)
          case _           => Callback.empty
      }
      .render { (_, specM, rhoS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Queueing widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val rho                        = rhoS.value
            val w                          = meanW(rho, spec.serviceTimeMs)
            val lq                         = meanQueueLength(rho)
            val infl                       = inflation(rho)
            val (verdictText, verdictTone) = verdict(rho)

            <.div(
              ^.className := "queueing-simulator not-prose",
              spec.title
                .map(t => <.p(^.className := "queueing-simulator__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "queueing-simulator__frame",
                ^.dangerouslySetInnerHtml := buildSvg(rho)
              ),
              <.div(
                ^.className := "queueing-simulator__controls",
                <.label(
                  ^.className := "queueing-simulator__slider-label",
                  <.span(^.className := "queueing-simulator__slider-text", s"Utilisation ρ = ${f"$rho%.2f"}"),
                  <.input(
                    ^.tpe       := "range",
                    ^.className := "queueing-simulator__slider",
                    ^.min       := "0",
                    ^.max       := "99",
                    ^.value     := math.round(rho * 100).toString,
                    ^.onChange ==> { (e: ReactEventFromInput) =>
                      val v = Try(e.target.value.toDouble).getOrElse(70.0)
                      rhoS.setState(math.max(0.0, math.min(0.99, v / 100.0)))
                    }
                  )
                )
              ),
              <.div(
                ^.className := "queueing-simulator__readout",
                readoutRow(
                  "Mean response time W",
                  formatMs(w),
                  s"= ${f"$infl%.1f"}× the ${formatMs(spec.serviceTimeMs)} service time"
                ),
                readoutRow(
                  "Mean queue length L_q",
                  formatQueueLength(lq),
                  "jobs waiting (not counting the one being served)"
                ),
                readoutRow(
                  "Verdict",
                  verdictText,
                  verdictTone match
                    case "safe"   => "Plenty of headroom; small spikes absorb fine"
                    case "ok"     => "Standard production target — ρ ≈ 0.6–0.7"
                    case "warn"   => "Headroom shrinking; tail latency starts steepening"
                    case "danger" => "Past the cliff — any disturbance triggers minutes of queue drain"
                    case _        => ""
                )
              )
            )
      }

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "queueing-simulator__readout-row",
      <.span(^.className := "queueing-simulator__readout-label", label),
      <.span(^.className := "queueing-simulator__readout-value", value),
      <.span(^.className := "queueing-simulator__readout-note", note)
    )
