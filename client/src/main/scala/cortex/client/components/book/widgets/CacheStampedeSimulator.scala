package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try

/**
 * Cache-stampede widget — side-by-side timeline rectangles comparing the *origin pressure* of a cold-key
 * fan-out with vs without request coalescing. With N concurrent reads of a freshly-expired hot key:
 *
 *   - Without coalescing, every caller misses, every caller's request flies to the origin in parallel. The
 *     origin briefly sees N in-flight requests of duration `originLatencyMs` each — the canonical thundering-
 *     herd shape. If N exceeds the origin's capacity, the origin falls over, every request times out, and the
 *     cache never gets repopulated.
 *   - With coalescing (a "lease", a Redis SETNX lock, a singleflight in Go, a Java CompletableFuture memoised
 *     by key), the first miss takes the lock; the remaining N-1 callers wait on the in-flight fetch. The
 *     origin sees exactly 1 request.
 *
 * The rectangles in the SVG are the *origin's queue-depth-over-time* — width = origin latency, height = peak
 * in-flight requests. The N× area difference is the entire pedagogical point of the widget.
 *
 * Re-used in:
 *   - Lesson 8 (caching) — primary.
 *   - Lesson 11 (replication) — read-your-writes after a write-followed-by-stale-read race.
 *   - Capstone 37 (URL shortener) — cold-key fan-out on the read path.
 *   - Capstone 38 (news feed) — same shape on timeline assembly.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":               "Cache stampede — N concurrent reads on a cold key",
 *   "concurrency":         100,
 *   "concurrencyRange":    [10, 500],
 *   "originLatencyMs":     100,
 *   "originLatencyRange":  [10, 500],
 *   "originCapacity":      50
 * }
 * }}}
 *
 *   - `concurrency` is the N for the slider's starting value; `concurrencyRange` is the bounds. With
 *     concurrency = 100 and a 50-rps capacity origin, the no-coalescing case overflows by 2×.
 *   - `originLatencyMs` is how long each origin fetch takes; sets the rectangle width.
 *   - `originCapacity` is the threshold against which we paint a red "exceeded" verdict. Best understood as
 *     "concurrent requests the origin can serve without melting" — for an app server it's roughly the thread
 *     / coroutine pool; for a database it's the connection pool.
 *
 * SVG is built as a string and injected via `dangerouslySetInnerHTML`. State is just the two slider values;
 * the rectangles and readouts derive from them.
 */
object CacheStampedeSimulator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Spec(
      title: Option[String],
      concurrency: Int,
      concurrencyMin: Int,
      concurrencyMax: Int,
      originLatencyMs: Int,
      originLatencyMin: Int,
      originLatencyMax: Int,
      originCapacity: Int
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val conc        = d.double("concurrency", 100.0).toInt
      val (cMin, cMx) = d.intRange("concurrencyRange", (10, 500))
      val origMs      = d.double("originLatencyMs", 100.0).toInt
      val (oMin, oMx) = d.intRange("originLatencyRange", (10, 500))
      val cap         = d.double("originCapacity", 50.0).toInt
      if cMin < 1 || cMx < cMin then
        throw PayloadDecoder.invalid("concurrencyRange must satisfy 1 ≤ min ≤ max")
      if oMin < 1 || oMx < oMin then
        throw PayloadDecoder.invalid("originLatencyRange must satisfy 1 ≤ min ≤ max")
      if conc < cMin || conc > cMx then
        throw PayloadDecoder.invalid(s"concurrency must fall within concurrencyRange [$cMin, $cMx]")
      if origMs < oMin || origMs > oMx then
        throw PayloadDecoder.invalid(s"originLatencyMs must fall within originLatencyRange [$oMin, $oMx]")
      if cap < 1 then throw PayloadDecoder.invalid("originCapacity must be ≥ 1")
      Spec(d.optString("title"), conc, cMin, cMx, origMs, oMin, oMx, cap)
    }

  // ===========================================================================
  // Layout
  // ===========================================================================

  private val ViewBoxWidth  = 720.0
  private val ViewBoxHeight = 360.0
  private val TopPad        = 40.0  // panel caption
  private val PanelHPad     = 30.0  // padding inside each panel
  private val PanelVPad     = 60.0  // top/bottom inside the panel (y-axis labels live here)
  private val PanelWidth    = 320.0
  private val PanelHeight   = 240.0 // total panel height
  private val PanelGap      = 40.0  // gap between the two panels
  private val PanelLeftX    = (ViewBoxWidth - 2 * PanelWidth - PanelGap) / 2
  private val PanelRightX   = PanelLeftX + PanelWidth + PanelGap
  private val PlotTop       = TopPad + 16
  private val PlotBottom    = PlotTop + PanelHeight - PanelVPad
  private val MaxBarH       = PlotBottom - PlotTop

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def panelSvg(
      panelX: Double,
      title: String,
      subtitle: String,
      peakInflight: Int,
      yAxisMax: Int,
      durationMs: Int,
      durationAxisMax: Int,
      exceeded: Boolean,
      colorClass: String
  ): String =
    val plotLeftX  = panelX + PanelHPad
    val plotRightX = panelX + PanelWidth - PanelHPad
    val plotW      = plotRightX - plotLeftX
    val plotH      = MaxBarH

    val barH =
      if yAxisMax <= 0 then 0.0
      else math.max(2.0, peakInflight.toDouble / yAxisMax.toDouble * plotH)
    val barW =
      if durationAxisMax <= 0 then 0.0
      else math.max(2.0, durationMs.toDouble / durationAxisMax.toDouble * plotW)
    val barY = PlotBottom - barH

    val frame =
      s"""<rect class="cache-stampede__panel-frame" x="$panelX" y="$TopPad" width="$PanelWidth" height="$PanelHeight" rx="6"/>"""

    val titleStr =
      s"""<text class="cache-stampede__panel-title" x="${panelX + PanelWidth / 2}" y="${TopPad + 22}" text-anchor="middle">${esc(
          title
        )}</text>"""

    val subtitleStr =
      s"""<text class="cache-stampede__panel-subtitle" x="${panelX + PanelWidth / 2}" y="${TopPad + 38}" text-anchor="middle">${esc(
          subtitle
        )}</text>"""

    // y-axis: 0 at PlotBottom, yAxisMax at PlotTop
    val yAxis =
      s"""<line class="cache-stampede__axis" x1="$plotLeftX" y1="$PlotTop" x2="$plotLeftX" y2="$PlotBottom"/>"""
    val xAxis =
      s"""<line class="cache-stampede__axis" x1="$plotLeftX" y1="$PlotBottom" x2="$plotRightX" y2="$PlotBottom"/>"""

    val yMaxLabel =
      s"""<text class="cache-stampede__axis-label" x="${plotLeftX - 6}" y="${PlotTop + 4}" text-anchor="end">${esc(
          yAxisMax.toString
        )}</text>"""
    val yMinLabel =
      s"""<text class="cache-stampede__axis-label" x="${plotLeftX - 6}" y="${PlotBottom + 4}" text-anchor="end">0</text>"""
    val xMaxLabel =
      s"""<text class="cache-stampede__axis-label" x="$plotRightX" y="${PlotBottom + 16}" text-anchor="end">${esc(
          s"${durationAxisMax} ms"
        )}</text>"""

    // The rectangle is the origin-pressure area: width = duration, height = peak inflight.
    val bar =
      s"""<rect class="cache-stampede__bar $colorClass" x="$plotLeftX" y="$barY" width="$barW" height="$barH" rx="2"/>"""

    val barCaption = peakInflight match
      case 1 => s"1 origin fetch · $durationMs ms"
      case n => s"$n inflight · $durationMs ms"

    // Inline label inside the bar (or above it if too short)
    val labelY = if barH > 24 then barY + barH / 2 + 4 else barY - 4
    val labelClass =
      if barH > 24 then "cache-stampede__bar-label"
      else "cache-stampede__bar-label cache-stampede__bar-label--above"
    val barLabel =
      s"""<text class="$labelClass" x="${plotLeftX + math.min(
          barW,
          80
        ) / 2 + 4}" y="$labelY" text-anchor="start">${esc(
          barCaption
        )}</text>"""

    val verdictClass =
      if exceeded then "cache-stampede__verdict cache-stampede__verdict--bad"
      else "cache-stampede__verdict cache-stampede__verdict--ok"
    val verdictText =
      if exceeded then "Origin overloaded"
      else "Origin OK"
    val verdict =
      s"""<text class="$verdictClass" x="${panelX + PanelWidth / 2}" y="${TopPad + PanelHeight - 14}" text-anchor="middle">${esc(
          verdictText
        )}</text>"""

    s"$frame\n$titleStr\n$subtitleStr\n$yAxis\n$xAxis\n$yMaxLabel\n$yMinLabel\n$xMaxLabel\n$bar\n$barLabel\n$verdict"

  private def buildSvg(concurrency: Int, originLatencyMs: Int, originCapacity: Int): String =
    val durationAxisMax = math.max(originLatencyMs, 10)
    val yAxisMax        = math.max(concurrency, 2)

    val withoutPanel = panelSvg(
      panelX = PanelLeftX,
      title = "Without coalescing",
      subtitle = s"$concurrency callers each fetch origin",
      peakInflight = concurrency,
      yAxisMax = yAxisMax,
      durationMs = originLatencyMs,
      durationAxisMax = durationAxisMax,
      exceeded = concurrency > originCapacity,
      colorClass = "cache-stampede__bar--without"
    )

    val withPanel = panelSvg(
      panelX = PanelRightX,
      title = "With coalescing",
      subtitle = "First miss takes a lease; rest wait on it",
      peakInflight = 1,
      yAxisMax = yAxisMax,
      durationMs = originLatencyMs,
      durationAxisMax = durationAxisMax,
      exceeded = 1 > originCapacity,
      colorClass = "cache-stampede__bar--with"
    )

    val yLabelText =
      s"""<text class="cache-stampede__y-title" x="${PanelLeftX - 18}" y="${PlotTop + MaxBarH / 2}" text-anchor="middle" transform="rotate(-90 ${PanelLeftX - 18} ${PlotTop + MaxBarH / 2})">in-flight at origin</text>"""

    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="cache-stampede__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $yLabelText
       |  $withoutPanel
       |  $withPanel
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 100)
      .useStateBy(_ => 100)
      .useEffectOnMountBy { (_, specM, concS, latS) =>
        specM.value match
          case Right(spec) =>
            concS.setState(spec.concurrency) >> latS.setState(spec.originLatencyMs)
          case _ => Callback.empty
      }
      .render { (_, specM, concS, latS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Cache-stampede widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val n        = concS.value
            val ms       = latS.value
            val capacity = spec.originCapacity
            // The "savings ratio" is how many *fewer* origin requests the coalesced path causes per stampede.
            val ratio             = n.toDouble
            val withoutOriginReqs = n
            val withOriginReqs    = 1
            // user-visible p99 latency: both paths wait roughly originLatencyMs (the in-flight request
            // duration). The difference is the *origin load*, not the user latency. We surface this
            // explicitly because it's the most common misunderstanding about request coalescing.
            val userVisibleP99 = ms

            <.div(
              ^.className := "cache-stampede not-prose",
              spec.title
                .map(t => <.p(^.className := "cache-stampede__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "cache-stampede__frame",
                ^.dangerouslySetInnerHtml := buildSvg(n, ms, capacity)
              ),
              <.div(
                ^.className := "cache-stampede__controls",
                sliderRow(
                  s"Concurrent callers (N) = $n",
                  spec.concurrencyMin,
                  spec.concurrencyMax,
                  n,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(spec.concurrency)
                    concS.setState(math.max(spec.concurrencyMin, math.min(spec.concurrencyMax, v)))
                  }
                ),
                sliderRow(
                  s"Origin fetch latency = $ms ms",
                  spec.originLatencyMin,
                  spec.originLatencyMax,
                  ms,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(spec.originLatencyMs)
                    latS.setState(math.max(spec.originLatencyMin, math.min(spec.originLatencyMax, v)))
                  }
                )
              ),
              <.div(
                ^.className := "cache-stampede__readout",
                readoutRow(
                  "Origin requests per stampede",
                  s"$withoutOriginReqs   →   $withOriginReqs",
                  s"coalescing eliminates ${withoutOriginReqs - withOriginReqs} of them"
                ),
                readoutRow(
                  "Origin capacity verdict",
                  if n > capacity then s"Overloaded ($n > $capacity inflight)"
                  else s"Within capacity (≤ $capacity)",
                  if n > capacity then
                    "Naive read-through cascades to a crash; the coalesced path stays at 1 inflight regardless of N"
                  else "Coalescing still wins on origin CPU + connection-pool churn"
                ),
                readoutRow(
                  "User-visible p99 latency",
                  s"~${userVisibleP99} ms",
                  "Coalescing does not lower user-visible latency — every caller still waits on the same origin fetch. The win is on the *origin*, not the user."
                ),
                readoutRow(
                  "Stampede ratio",
                  f"${ratio}%.0f×",
                  "fewer origin calls under coalescing — exactly N× cheaper on origin per cold-key fan-out"
                )
              )
            )
      }

  private def sliderRow(
      label: String,
      min: Int,
      max: Int,
      value: Int,
      onInput: ReactEventFromInput => Callback
  ): VdomNode =
    <.label(
      ^.className := "cache-stampede__slider-row",
      <.span(^.className := "cache-stampede__slider-text", label),
      <.input(
        ^.tpe       := "range",
        ^.className := "cache-stampede__slider",
        ^.min       := min.toString,
        ^.max       := max.toString,
        ^.step      := "1",
        ^.value     := value.toString,
        ^.onChange ==> onInput
      )
    )

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "cache-stampede__readout-row",
      <.span(^.className := "cache-stampede__readout-label", label),
      <.span(^.className := "cache-stampede__readout-value", value),
      <.span(^.className := "cache-stampede__readout-note", note)
    )
