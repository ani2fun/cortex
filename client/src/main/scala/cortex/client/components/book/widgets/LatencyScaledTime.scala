package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try

/**
 * Latency-scaled-time widget — renders a list of computer-operations on a horizontal log-scale axis spanning
 * 1 ns to 1 s (9 orders of magnitude). Reader's eye picks up the multiplicative gap at a glance; clicking /
 * hovering any bar shows the operation's raw nanoseconds and the same duration *rescaled to human time* using
 * the author-supplied `scaleSeconds` factor (default 1e9 = "1 ns ≡ 1 second").
 *
 * Companion to the latency table in `02-numbers-every-engineer-should-know.md`: tables tell, charts show.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":         "Latency landscape — median operations, log scale",
 *   "scaleSeconds":  1.0e9,
 *   "items": [
 *     { "label": "L1 cache",                  "ns": 1,           "highlight": true },
 *     { "label": "L2 cache",                  "ns": 4 },
 *     { "label": "Main memory",               "ns": 100,         "highlight": true },
 *     { "label": "Cross-region RTT (CA→NL)",  "ns": 150000000,   "highlight": true }
 *   ]
 * }
 * }}}
 *
 *   - `scaleSeconds` is the factor that converts a raw second back into the "human-scale" second the analogy
 *     uses. With `1e9`, a 1-ns operation lands at 1 second on the human scale and a 150 ms cross-region RTT
 *     lands at ~4 years 9 months.
 *   - `highlight: true` marks "memorise this one" rows (L1, main memory, cross-region in the canonical
 *     payload). Highlighted bars use the primary accent; their labels render bold.
 *   - `ns` must be ≥ 1; values outside the chart's 1 ns – 1 s range are clamped visually (very large values
 *     end at the right edge with their formatted label still legible).
 *
 * SVG is built as a string and injected via `dangerouslySetInnerHTML` — same pattern Mermaid + D2 +
 * ArrayTraversal use. Selection state lives in React so click/tap on any row updates the caption strip below
 * the chart.
 */
object LatencyScaledTime:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Item(label: String, ns: Double, highlight: Boolean)
  final case class Spec(title: Option[String], scaleSeconds: Double, items: List[Item])

  final case class Props(payload: String)

  // ===========================================================================
  // Layout constants — tuned so the widget fits inside a ~640-px prose column
  // without horizontal scroll. The SVG is rendered with viewBox + CSS width
  // 100%, so the absolute numbers are scale-invariant once in the chapter.
  // ===========================================================================

  private val ViewBoxWidth = 720.0
  private val LeftPad      = 168.0 // operation label column
  private val RightPad     = 28.0  // breathing room for the right-tip ns label
  private val TopPad       = 36.0  // axis labels above
  private val BottomPad    = 8.0   // SVG-internal bottom gutter
  private val RowHeight    = 28.0
  private val BarHeight    = 14.0

  private val MinNs   = 1.0
  private val MaxNs   = 1.0e9
  private val LogMin  = 0.0 // log10(MinNs)
  private val LogMax  = 9.0 // log10(MaxNs)
  private val PlotMin = LeftPad
  private val PlotMax = ViewBoxWidth - RightPad

  // ===========================================================================
  // Parsing — js.JSON.parse + js.Dynamic mirrors BlockDiscovery.parseRawTabs
  // and the ArrayTraversal widget. Anything that throws collapses to Left(msg)
  // and the component renders an error placeholder rather than crashing the
  // chapter.
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val items = d.dynList("items").map(it =>
        Item(label = it.string("label"), ns = it.double("ns"), highlight = it.bool("highlight"))
      )
      val scale = d.double("scaleSeconds", 1.0e9)
      if items.isEmpty then throw PayloadDecoder.invalid("items must be non-empty")
      if scale <= 0 then throw PayloadDecoder.invalid("scaleSeconds must be > 0")
      if items.exists(_.ns <= 0) then throw PayloadDecoder.invalid("every item.ns must be > 0")
      if items.exists(_.label.trim.isEmpty) then
        throw PayloadDecoder.invalid("every item.label must be non-empty")
      Spec(title = d.optString("title"), scaleSeconds = scale, items = items)
    }

  // ===========================================================================
  // Scales and formatting
  // ===========================================================================

  private def xScale(ns: Double): Double =
    val clamped = math.max(MinNs, math.min(MaxNs, ns))
    val logNs   = math.log10(clamped)
    PlotMin + (logNs - LogMin) / (LogMax - LogMin) * (PlotMax - PlotMin)

  /** Right-tip label: human-readable real-time. */
  private def formatNs(ns: Double): String =
    if ns < 1_000 then f"$ns%.0f ns"
    else if ns < 1_000_000 then f"${ns / 1_000}%.0f µs"
    else if ns < 1_000_000_000 then f"${ns / 1_000_000}%.0f ms"
    else f"${ns / 1_000_000_000}%.1f s"

  /** Pluralise — singular for n == 1, plural otherwise. Matches the lesson's table phrasing. */
  private def plural(n: Int): String = if n == 1 then "" else "s"

  /**
   * Rescale a real-time ns to human-time seconds using `scaleSeconds`, then format as the largest natural
   * unit + remainder. Mirrors the third column of the lesson's latency table ("1 minute 40 seconds", "4 years
   * 9 months", "5 days 19 hours").
   */
  private def humaniseSeconds(seconds: Double): String =
    val minute = 60.0
    val hour   = 3_600.0
    val day    = 86_400.0
    val month  = 30.0 * day // approximate; matches conventional informal usage
    val year   = 365.25 * day
    if seconds < 1 then f"$seconds%.2f seconds"
    else if seconds < minute then
      val s = math.round(seconds).toInt
      s"$s second${plural(s)}"
    else if seconds < hour then
      val m = (seconds / minute).toInt
      val s = math.round(seconds - m * minute).toInt
      if s > 0 then s"$m minute${plural(m)} $s second${plural(s)}"
      else s"$m minute${plural(m)}"
    else if seconds < day then
      val h = (seconds / hour).toInt
      val m = ((seconds - h * hour) / minute).toInt
      if m > 0 then s"$h hour${plural(h)} $m minute${plural(m)}"
      else s"$h hour${plural(h)}"
    else if seconds < month then
      val d = (seconds / day).toInt
      val h = ((seconds - d * day) / hour).toInt
      if h > 0 then s"$d day${plural(d)} $h hour${plural(h)}"
      else s"$d day${plural(d)}"
    else if seconds < year then
      val mo = (seconds / month).toInt
      val d  = ((seconds - mo * month) / day).toInt
      if d > 0 then s"$mo month${plural(mo)} $d day${plural(d)}"
      else s"$mo month${plural(mo)}"
    else
      val y  = (seconds / year).toInt
      val mo = ((seconds - y * year) / month).toInt
      if mo > 0 then s"$y year${plural(y)} $mo month${plural(mo)}"
      else s"$y year${plural(y)}"

  /** Apply the spec's scaleSeconds factor: ns × scaleSeconds × 1e-9 seconds. */
  private def toHumanSeconds(ns: Double, scaleSeconds: Double): Double =
    ns * scaleSeconds * 1.0e-9

  // ===========================================================================
  // SVG string building
  // ===========================================================================

  // Decade tick positions on the log axis: 1, 10, 100, 1k, 10k, 100k, 1M, 10M, 100M, 1G ns.
  private val DecadeTicks: List[Double] =
    List.tabulate(10)(i => math.pow(10.0, i.toDouble))

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def viewBoxHeight(itemCount: Int): Double =
    TopPad + itemCount * RowHeight + BottomPad

  private def axisSvg: String =
    val y0 = TopPad - 6
    val ticks = DecadeTicks
      .map { ns =>
        val x     = xScale(ns)
        val label = formatNs(ns)
        // Stagger labels slightly so 9-decade fit isn't crowded; major decades (ns/µs/ms/s starts) get bold.
        val major  = ns == 1 || ns == 1_000 || ns == 1_000_000 || ns == 1_000_000_000
        val labelY = y0 - 8
        val cls = if major then "latency-scaled-time__tick-label latency-scaled-time__tick-label--major"
        else "latency-scaled-time__tick-label"
        s"""<g>
           |  <line class="latency-scaled-time__tick" x1="$x" y1="${y0 - 4}" x2="$x" y2="$y0"/>
           |  <text class="$cls" x="$x" y="$labelY" text-anchor="middle">${esc(label)}</text>
           |</g>""".stripMargin
      }
      .mkString("\n")
    val axisLine =
      s"""<line class="latency-scaled-time__axis" x1="$PlotMin" y1="$y0" x2="$PlotMax" y2="$y0"/>"""
    s"$axisLine\n$ticks"

  private def rowSvg(item: Item, rowIndex: Int, selectedIndex: Int): String =
    val y       = TopPad + rowIndex * RowHeight
    val barY    = y + (RowHeight - BarHeight) / 2.0
    val barEndX = xScale(item.ns)
    val barW    = math.max(2.0, barEndX - PlotMin) // 2-px minimum so 1-ns bars are visible
    val isSel   = rowIndex == selectedIndex
    val labelCls = (item.highlight, isSel) match
      case (_, true)      => "latency-scaled-time__label latency-scaled-time__label--selected"
      case (true, false)  => "latency-scaled-time__label latency-scaled-time__label--highlight"
      case (false, false) => "latency-scaled-time__label"
    val barCls = (item.highlight, isSel) match
      case (_, true)      => "latency-scaled-time__bar latency-scaled-time__bar--selected"
      case (true, false)  => "latency-scaled-time__bar latency-scaled-time__bar--highlight"
      case (false, false) => "latency-scaled-time__bar"
    val nsLabelX = math.min(PlotMax + 4, barEndX + 6)
    // A transparent hit area spanning the full row keeps click targets generous on touch devices and
    // avoids requiring pixel-perfect bar aim on a 14-px-tall bar.
    val hitY = y
    val hitH = RowHeight
    s"""<g class="latency-scaled-time__row" data-index="$rowIndex">
       |  <rect class="latency-scaled-time__hit" x="0" y="$hitY" width="$ViewBoxWidth" height="$hitH"/>
       |  <text class="$labelCls" x="${PlotMin - 12}" y="${y + 18}" text-anchor="end">${esc(
        item.label
      )}</text>
       |  <rect class="$barCls" x="$PlotMin" y="$barY" width="$barW" height="$BarHeight" rx="3"/>
       |  <text class="latency-scaled-time__ns" x="$nsLabelX" y="${y + 18}" text-anchor="start">${esc(
        formatNs(item.ns)
      )}</text>
       |</g>""".stripMargin

  private def buildSvg(spec: Spec, selectedIndex: Int): String =
    val rows = spec.items.zipWithIndex
      .map { case (item, i) => rowSvg(item, i, selectedIndex) }
      .mkString("\n")
    s"""<svg viewBox="0 0 $ViewBoxWidth ${viewBoxHeight(spec.items.size)}"
       |     class="latency-scaled-time__svg" role="img"
       |     aria-label="${esc(spec.title.getOrElse("Latency landscape"))}"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $axisSvg
       |  $rows
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(-1) // selected row index; -1 = default-pick logic
      .render { (_, specM, selectedS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Latency widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val defaultIdx = pickDefaultIndex(spec.items)
            val selected   = if selectedS.value < 0 then defaultIdx else selectedS.value
            val current    = spec.items(selected)
            val human      = humaniseSeconds(toHumanSeconds(current.ns, spec.scaleSeconds))

            // Row click handler — read the data-index off the closest <g> with that attr. Avoids wiring 11
            // separate onClick attributes from Scala into the dangerouslySetInnerHTML SVG.
            val onClick: ReactEventFromHtml => Callback = e =>
              Callback {
                val target = e.target.asInstanceOf[org.scalajs.dom.Element]
                val row    = target.closest(".latency-scaled-time__row")
                if row != null then
                  val attr = row.getAttribute("data-index")
                  val idx  = Try(attr.toInt).getOrElse(-1)
                  if idx >= 0 && idx < spec.items.size then selectedS.setState(idx).runNow()
              }

            <.div(
              ^.className := "latency-scaled-time not-prose",
              spec.title
                .map(t => <.p(^.className := "latency-scaled-time__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "latency-scaled-time__frame",
                ^.onClick ==> onClick,
                ^.dangerouslySetInnerHtml := buildSvg(spec, selected)
              ),
              <.p(
                ^.className := "latency-scaled-time__caption",
                ^.aria.live := "polite",
                <.strong(s"${current.label}: "),
                s"${formatNs(current.ns)} of real time ",
                <.span(^.className := "latency-scaled-time__caption-arrow", " ≡ "),
                <.strong(human),
                s" on the human scale (1 ns ≡ ${humaniseSeconds(spec.scaleSeconds * 1.0e-9)})."
              )
            )
      }

  /**
   * Default-select the slowest non-highlighted row, falling back to the last item. Reader's eye lands on the
   * largest gap first — the canonical Cross-region RTT moment.
   */
  private def pickDefaultIndex(items: List[Item]): Int =
    val withIndex = items.zipWithIndex
    withIndex.maxByOption { case (it, _) => it.ns }.map(_._2).getOrElse(0)
