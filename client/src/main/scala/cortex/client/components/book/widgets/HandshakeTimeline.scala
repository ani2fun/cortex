package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js
import scala.util.Try

/**
 * Handshake-timeline widget — stacked horizontal-bar comparison of an HTTP visit's latency budget across
 * protocol stacks and connection states. Each row is a scenario (e.g. "TCP + TLS 1.2 + HTTP/1.1, cold"), each
 * colored segment is a phase (DNS, TCP, TLS, QUIC, HTTP request+response, optional server processing), and
 * the slider scrubs the round-trip time so the reader can feel how cold cross-region (~150 ms) blows up the
 * handshake tax vs a warm pooled connection.
 *
 * Companion to lesson 6's prose pivot: every protocol choice is an RTT count. The bar chart makes the cliff
 * visible; the slider makes it visceral.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":     "Cold HTTPS visit — handshake tax by protocol stack",
 *   "rttMs":     50,
 *   "rttRange":  [5, 200],
 *   "scenarios": [
 *     {
 *       "name": "TCP + TLS 1.2 + HTTP/1.1 (cold)",
 *       "phases": [
 *         { "name": "DNS lookup",              "rttCount": 2, "kind": "dns" },
 *         { "name": "TCP handshake",           "rttCount": 1, "kind": "tcp" },
 *         { "name": "TLS 1.2 handshake",       "rttCount": 2, "kind": "tls" },
 *         { "name": "HTTP request + response", "rttCount": 1, "kind": "request" }
 *       ]
 *     },
 *     {
 *       "name": "QUIC + TLS 1.3 + HTTP/3 (cold)",
 *       "phases": [
 *         { "name": "DNS lookup",              "rttCount": 2, "kind": "dns" },
 *         { "name": "QUIC + TLS 1.3",          "rttCount": 1, "kind": "quic" },
 *         { "name": "HTTP request + response", "rttCount": 1, "kind": "request" }
 *       ]
 *     },
 *     {
 *       "name": "Fully pooled (warm)",
 *       "phases": [
 *         { "name": "HTTP request + response", "rttCount": 1, "kind": "request" }
 *       ]
 *     }
 *   ]
 * }
 * }}}
 *
 *   - `rttMs` is the slider's starting position and the value used to convert `rttCount` into wall-clock ms
 *     per phase. `rttRange` (defaulting to [5, 200]) sets the slider bounds — 5 ms is same-rack, 20 ms is
 *     same-region, 80 ms is same-continent, 150 ms is mobile / cross-region.
 *   - Each phase contributes `rttCount * rttMs + fixedMs` to the row total. `fixedMs` (default 0) is the
 *     escape hatch for non-RTT-dominated phases like server processing.
 *   - `kind` is one of `dns | tcp | tls | quic | request | processing | other`; it picks the segment colour
 *     and (when the bar is too narrow for the full name) the abbreviation. Unknown values fall back to
 *     `other`. The seven kinds are the legend's full vocabulary.
 *   - All scenarios share one x-axis scaled to the widest scenario's total, so a 5-ms cold visit and a 250-ms
 *     cold visit sit next to each other at the right visual ratio.
 *
 * SVG is built as a string and injected via `dangerouslySetInnerHTML` — same pattern Mermaid + D2 +
 * ArrayTraversal and the rest of the catalog use. Only the slider value lives in React state; the bar chart
 * and readout both derive from it.
 */
object HandshakeTimeline:

  // ===========================================================================
  // Schema
  // ===========================================================================

  enum PhaseKind:
    case Dns, Tcp, Tls, Quic, Request, Processing, Other

  final case class Phase(name: String, rttCount: Double, fixedMs: Double, kind: PhaseKind)
  final case class Scenario(name: String, phases: List[Phase])

  final case class Spec(
      title: Option[String],
      rttMs: Double,
      rttMin: Double,
      rttMax: Double,
      scenarios: List[Scenario]
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Layout constants
  // ===========================================================================

  private val ViewBoxWidth = 760.0
  private val LeftPad      = 200.0 // scenario-name column
  private val RightPad     = 72.0  // total-ms label after each bar
  private val TopPad       = 36.0  // axis caption + breathing room above first row
  private val BottomPad    = 12.0
  private val RowHeight    = 56.0
  private val BarHeight    = 24.0

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parseKind(s: String): PhaseKind = s.toLowerCase match
    case "dns"        => PhaseKind.Dns
    case "tcp"        => PhaseKind.Tcp
    case "tls"        => PhaseKind.Tls
    case "quic"       => PhaseKind.Quic
    case "request"    => PhaseKind.Request
    case "processing" => PhaseKind.Processing
    case _            => PhaseKind.Other

  private def parsePhase(d: js.Dynamic): Phase =
    Phase(
      name = d.string("name").trim,
      rttCount = d.double("rttCount"),
      fixedMs = d.double("fixedMs"),
      kind = parseKind(d.string("kind"))
    )

  private def parseScenario(d: js.Dynamic): Scenario =
    Scenario(name = d.string("name").trim, phases = d.dynList("phases").map(parsePhase))

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val rttMs     = d.double("rttMs", 50.0)
      val (lo, hi)  = d.doubleRange("rttRange", (5.0, 200.0))
      val scenarios = d.dynList("scenarios").map(parseScenario)
      if scenarios.isEmpty then throw PayloadDecoder.invalid("scenarios must be non-empty")
      if scenarios.exists(_.phases.isEmpty) then
        throw PayloadDecoder.invalid("every scenario.phases must be non-empty")
      if scenarios.exists(_.name.isEmpty) then
        throw PayloadDecoder.invalid("every scenario.name must be non-empty")
      if scenarios.flatMap(_.phases).exists(_.name.isEmpty) then
        throw PayloadDecoder.invalid("every phase.name must be non-empty")
      if scenarios.flatMap(_.phases).exists(p => p.rttCount < 0 || p.fixedMs < 0) then
        throw PayloadDecoder.invalid("phase.rttCount and phase.fixedMs must be ≥ 0")
      if lo <= 0 || hi <= lo then throw PayloadDecoder.invalid("rttRange must satisfy 0 < min < max")
      if rttMs < lo || rttMs > hi then
        throw PayloadDecoder.invalid(s"rttMs ($rttMs) must fall within rttRange [$lo, $hi]")
      Spec(d.optString("title"), rttMs, lo, hi, scenarios)
    }

  // ===========================================================================
  // Maths
  // ===========================================================================

  private def phaseMs(p: Phase, rttMs: Double): Double = p.rttCount * rttMs + p.fixedMs

  private def scenarioTotalMs(s: Scenario, rttMs: Double): Double =
    s.phases.iterator.map(phaseMs(_, rttMs)).sum

  private def rttBand(rttMs: Double): String =
    if rttMs <= 10 then "same rack / same DC"
    else if rttMs <= 35 then "same region"
    else if rttMs <= 90 then "same continent"
    else if rttMs <= 150 then "cross-continent fibre"
    else "mobile / cross-region"

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def kindClass(k: PhaseKind): String = k match
    case PhaseKind.Dns        => "handshake-timeline__phase--dns"
    case PhaseKind.Tcp        => "handshake-timeline__phase--tcp"
    case PhaseKind.Tls        => "handshake-timeline__phase--tls"
    case PhaseKind.Quic       => "handshake-timeline__phase--quic"
    case PhaseKind.Request    => "handshake-timeline__phase--request"
    case PhaseKind.Processing => "handshake-timeline__phase--processing"
    case PhaseKind.Other      => "handshake-timeline__phase--other"

  private def kindAbbrev(k: PhaseKind): String = k match
    case PhaseKind.Dns        => "DNS"
    case PhaseKind.Tcp        => "TCP"
    case PhaseKind.Tls        => "TLS"
    case PhaseKind.Quic       => "QUIC"
    case PhaseKind.Request    => "HTTP"
    case PhaseKind.Processing => "PROC"
    case PhaseKind.Other      => ""

  private def kindLabel(k: PhaseKind): String = k match
    case PhaseKind.Dns        => "DNS"
    case PhaseKind.Tcp        => "TCP handshake"
    case PhaseKind.Tls        => "TLS handshake"
    case PhaseKind.Quic       => "QUIC + TLS 1.3"
    case PhaseKind.Request    => "HTTP request + response"
    case PhaseKind.Processing => "Server processing"
    case PhaseKind.Other      => "Other"

  private def formatMs(ms: Double): String =
    if ms.isInfinite then "∞"
    else if ms >= 10_000 then f"${ms / 1_000}%.1f s"
    else if ms >= 1_000 then f"${ms / 1_000}%.2f s"
    else if ms >= 10 then f"$ms%.0f ms"
    else f"$ms%.1f ms"

  private def viewBoxHeight(scenarioCount: Int): Double =
    TopPad + scenarioCount * RowHeight + BottomPad

  private def phaseSegmentSvg(phase: Phase, x: Double, y: Double, w: Double): String =
    val rect =
      s"""<rect class="handshake-timeline__phase ${kindClass(phase.kind)}"
         |  x="$x" y="$y" width="$w" height="$BarHeight" rx="2"/>""".stripMargin
    val labelY = y + BarHeight / 2 + 4
    val label =
      if w >= 110 then
        s"""<text class="handshake-timeline__phase-label" x="${x + w / 2}" y="$labelY" text-anchor="middle">${esc(
            phase.name
          )}</text>"""
      else if w >= 38 && kindAbbrev(phase.kind).nonEmpty then
        s"""<text class="handshake-timeline__phase-label handshake-timeline__phase-label--abbrev" x="${x + w / 2}" y="$labelY" text-anchor="middle">${kindAbbrev(
            phase.kind
          )}</text>"""
      else ""
    s"$rect\n$label"

  private def buildSvg(spec: Spec, rttMs: Double): String =
    val totals   = spec.scenarios.map(s => scenarioTotalMs(s, rttMs))
    val maxTotal = if totals.isEmpty then 0.0 else totals.max
    val plotW    = ViewBoxWidth - LeftPad - RightPad
    val msToPx: Double => Double =
      ms => if maxTotal <= 0 then 0.0 else (ms / maxTotal) * plotW

    val rows = spec.scenarios.zipWithIndex.map { case (sc, i) =>
      val rowTopY = TopPad + i * RowHeight
      val barY    = rowTopY + (RowHeight - BarHeight) / 2
      val total   = scenarioTotalMs(sc, rttMs)

      // Lay segments out left-to-right, accumulating x.
      val (segments, _) = sc.phases.foldLeft((List.empty[String], LeftPad)) { case ((acc, xPos), phase) =>
        val w = msToPx(phaseMs(phase, rttMs))
        (acc :+ phaseSegmentSvg(phase, xPos, barY, w), xPos + w)
      }

      val nameY = barY + BarHeight / 2 + 4
      val nameLabel =
        s"""<text class="handshake-timeline__scenario-label" x="${LeftPad - 12}" y="$nameY" text-anchor="end">${esc(
            sc.name
          )}</text>"""
      val totalX = LeftPad + msToPx(total) + 8
      val totalLabel =
        s"""<text class="handshake-timeline__total" x="$totalX" y="$nameY" text-anchor="start">${esc(
            formatMs(total)
          )}</text>"""

      (segments :+ nameLabel :+ totalLabel).mkString("\n")
    }.mkString("\n")

    val caption =
      s"""<text class="handshake-timeline__caption" x="$LeftPad" y="${TopPad - 14}" text-anchor="start">${esc(
          s"Wall-clock latency at RTT = ${formatMs(rttMs)} (${rttBand(rttMs)})"
        )}</text>"""

    s"""<svg viewBox="0 0 $ViewBoxWidth ${viewBoxHeight(spec.scenarios.size)}"
       |     class="handshake-timeline__svg" role="img"
       |     aria-label="${esc(spec.title.getOrElse("Handshake timeline"))}"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $caption
       |  $rows
       |</svg>""".stripMargin

  // ===========================================================================
  // Legend
  // ===========================================================================

  private def legendKinds(spec: Spec): List[PhaseKind] =
    val present = spec.scenarios.flatMap(_.phases).map(_.kind).distinct
    // Preserve a stable order so the legend reads dns → tcp → tls → quic → request → processing → other.
    val canonical = List(
      PhaseKind.Dns,
      PhaseKind.Tcp,
      PhaseKind.Tls,
      PhaseKind.Quic,
      PhaseKind.Request,
      PhaseKind.Processing,
      PhaseKind.Other
    )
    canonical.filter(present.contains)

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 50.0)
      .useEffectOnMountBy { (_, specM, rttS) =>
        specM.value match
          case Right(spec) => rttS.setState(spec.rttMs)
          case _           => Callback.empty
      }
      .render { (_, specM, rttS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Handshake-timeline widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val rttMs   = rttS.value
            val totals  = spec.scenarios.map(s => s.name -> scenarioTotalMs(s, rttMs))
            val maxPair = totals.maxBy(_._2)
            val minPair = totals.minBy(_._2)
            val ratio =
              if minPair._2 <= 0 then Double.PositiveInfinity else maxPair._2 / minPair._2

            <.div(
              ^.className := "handshake-timeline not-prose",
              spec.title
                .map(t => <.p(^.className := "handshake-timeline__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "handshake-timeline__frame",
                ^.dangerouslySetInnerHtml := buildSvg(spec, rttMs)
              ),
              <.div(
                ^.className := "handshake-timeline__controls",
                <.label(
                  ^.className := "handshake-timeline__slider-label",
                  <.span(
                    ^.className := "handshake-timeline__slider-text",
                    s"Round-trip time = ${formatMs(rttMs)} (${rttBand(rttMs)})"
                  ),
                  <.input(
                    ^.tpe       := "range",
                    ^.className := "handshake-timeline__slider",
                    ^.min       := spec.rttMin.toInt.toString,
                    ^.max       := spec.rttMax.toInt.toString,
                    ^.step      := "1",
                    ^.value     := math.round(rttMs).toString,
                    ^.onChange ==> { (e: ReactEventFromInput) =>
                      val v = Try(e.target.value.toDouble).getOrElse(spec.rttMs)
                      rttS.setState(math.max(spec.rttMin, math.min(spec.rttMax, v)))
                    }
                  )
                )
              ),
              <.div(
                ^.className := "handshake-timeline__legend",
                legendKinds(spec).map { k =>
                  <.span(
                    ^.className := "handshake-timeline__legend-item",
                    ^.key       := k.toString,
                    <.span(^.className := s"handshake-timeline__swatch ${kindClass(k)}"),
                    <.span(^.className := "handshake-timeline__legend-text", kindLabel(k))
                  ): VdomNode
                }.toVdomArray
              ),
              if spec.scenarios.size >= 2 then
                <.div(
                  ^.className := "handshake-timeline__readout",
                  readoutRow("Slowest", maxPair._1, formatMs(maxPair._2)),
                  readoutRow("Fastest", minPair._1, formatMs(minPair._2)),
                  readoutRow(
                    "Cold/warm gap",
                    if ratio.isInfinite then "—" else f"${ratio}%.1f×",
                    "more handshakes ⇒ more RTTs ⇒ more wall-clock at every RTT"
                  )
                )
              else EmptyVdom
            )
      }

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "handshake-timeline__readout-row",
      <.span(^.className := "handshake-timeline__readout-label", label),
      <.span(^.className := "handshake-timeline__readout-value", value),
      <.span(^.className := "handshake-timeline__readout-note", note)
    )
