package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try
import scala.util.hashing.MurmurHash3

/**
 * Hot-shard widget — bar chart of per-shard request volume under three partitioning strategies (range, hash,
 * hash + virtual shards) applied to the same Zipfian-skewed workload. The reader watches one shard turn red
 * as the skew slider cranks up under "range" or "hash"; switching to "hash + virtual shards" smooths it back
 * out.
 *
 * The pedagogical hook is the "hot-shard factor" readout — max-load / mean-load. Under uniform load it's
 * ~1.0. Under Zipfian-skewed load with raw range / hash partitioning, it can climb to 3–8× on small shard
 * counts. Adding virtual shards (~50 per physical) drops it back near 1× because the law of large numbers
 * kicks in. That is the same insight as virtual nodes on the consistent-hash ring (Lesson 7), applied to
 * sharding.
 *
 * Re-used in:
 *   - Lesson 12 (sharding) — primary.
 *   - Capstone 38 (news feed) — fan-out partitioning for the timeline.
 *   - Capstone 39 (chat system) — channel partitioning.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":           "Hot shards — partitioning strategy vs Zipfian workload",
 *   "shardCount":      8,
 *   "shardCountRange": [2, 16],
 *   "skew":            1.2,
 *   "skewRange":       [0.0, 2.0],
 *   "keyCount":        500,
 *   "virtualPerShard": 50
 * }
 * }}}
 *
 *   - `skew` is the Zipfian exponent. 0 = uniform; ~1.0 = mildly skewed (the canonical "long tail"); 1.5+
 * \= severely skewed (one key dominates).
 *   - `keyCount` is the universe of distinct keys; the widget fires one request per key, weighted by the
 *     Zipfian distribution.
 *   - `virtualPerShard` is how many ring positions each physical shard claims under the "hash + virtual"
 *     strategy. 50 is a realistic production value (Cassandra defaults to 256).
 */
object HotShardSimulator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  enum Strategy:
    case Range, Hash, HashVirtual

  final case class Spec(
      title: Option[String],
      shardCount: Int,
      shardMin: Int,
      shardMax: Int,
      skew: Double,
      skewMin: Double,
      skewMax: Double,
      keyCount: Int,
      virtualPerShard: Int
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val sc            = d.double("shardCount", 8.0).toInt
      val (sMin, sMx)   = d.intRange("shardCountRange", (2, 16))
      val skew          = d.double("skew", 1.2)
      val (skMin, skMx) = d.doubleRange("skewRange", (0.0, 2.0))
      val kc            = d.double("keyCount", 500.0).toInt
      val vps           = d.double("virtualPerShard", 50.0).toInt
      if sMin < 2 || sMx < sMin then
        throw PayloadDecoder.invalid("shardCountRange must satisfy 2 ≤ min ≤ max")
      if skMin < 0 || skMx < skMin then throw PayloadDecoder.invalid("skewRange must satisfy 0 ≤ min ≤ max")
      if sc < sMin || sc > sMx then throw PayloadDecoder.invalid("shardCount out of range")
      if skew < skMin || skew > skMx then throw PayloadDecoder.invalid("skew out of range")
      if kc < 10 || kc > 5000 then throw PayloadDecoder.invalid("keyCount must be in [10, 5000]")
      if vps < 1 || vps > 500 then throw PayloadDecoder.invalid("virtualPerShard must be in [1, 500]")
      Spec(d.optString("title"), sc, sMin, sMx, skew, skMin, skMx, kc, vps)
    }

  // ===========================================================================
  // Workload — Zipfian-weighted keys
  // ===========================================================================

  /** Zipf weight for rank `r` (1-based): 1 / r^skew. */
  private def zipfWeight(rank: Int, skew: Double): Double =
    if rank <= 0 then 0.0 else math.pow(rank.toDouble, -skew)

  /** Total weighted request volume by shard, under the given strategy + Zipfian skew. */
  private def loadByShard(
      shardCount: Int,
      skew: Double,
      keyCount: Int,
      strategy: Strategy,
      virtualPerShard: Int
  ): Array[Double] =
    val out = Array.ofDim[Double](shardCount)
    var j   = 1
    while j <= keyCount do
      val owner = strategy match
        case Strategy.Range =>
          // Range partition: first 1/N of keys → shard 0, next 1/N → shard 1, etc.
          math.min(shardCount - 1, ((j - 1).toLong * shardCount / keyCount).toInt)
        case Strategy.Hash =>
          val h        = MurmurHash3.stringHash(s"key-$j")
          val unsigned = h.toLong & 0xffffffffL
          (unsigned % shardCount).toInt
        case Strategy.HashVirtual =>
          val h        = MurmurHash3.stringHash(s"key-$j")
          val unsigned = h.toLong & 0xffffffffL
          val totalV   = shardCount.toLong * virtualPerShard.toLong
          val vNode    = (unsigned % totalV).toInt
          vNode / virtualPerShard
      out(owner) += zipfWeight(j, skew)
      j += 1
    out

  private def hotShardFactor(load: Array[Double]): Double =
    if load.isEmpty then 0.0
    else
      val total = load.sum
      val mean  = total / load.length
      if mean <= 0 then 0.0 else load.max / mean

  private def percent(load: Array[Double]): Array[Double] =
    val total = load.sum
    if total <= 0 then load else load.map(_ / total * 100.0)

  // ===========================================================================
  // Layout
  // ===========================================================================

  private val ViewBoxWidth  = 720.0
  private val ViewBoxHeight = 320.0
  private val LeftPad       = 56.0
  private val RightPad      = 24.0
  private val TopPad        = 36.0
  private val ChartBottom   = 250.0
  private val MaxBarH       = ChartBottom - TopPad
  private val PlotW         = ViewBoxWidth - LeftPad - RightPad

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def buildSvg(load: Array[Double], hotFactor: Double): String =
    val pct    = percent(load)
    val maxPct = math.max(pct.max, 1.0)
    val gap    = 4.0
    val slot   = PlotW / load.length
    val barW   = slot - gap
    val bars = load.indices.map { i =>
      val barH   = (pct(i) / maxPct) * MaxBarH
      val barX   = LeftPad + i * slot + gap / 2
      val barY   = ChartBottom - barH
      val isHot  = pct(i) >= maxPct * 0.85 && hotFactor > 1.5
      val barCls = if isHot then "hot-shard__bar hot-shard__bar--hot" else "hot-shard__bar"
      val labelY = if barH > 18 then barY + 14 else barY - 6
      val labelCls =
        if barH > 18 then "hot-shard__bar-label" else "hot-shard__bar-label hot-shard__bar-label--outside"
      val xLabel =
        s"""<text class="hot-shard__x-label" x="${barX + barW / 2}" y="${ChartBottom + 16}" text-anchor="middle">${i + 1}</text>"""
      val percentLabel =
        s"""<text class="$labelCls" x="${barX + barW / 2}" y="$labelY" text-anchor="middle">${esc(
            f"${pct(i)}%.0f%%"
          )}</text>"""
      s"""<g>
         |  <rect class="$barCls" x="$barX" y="$barY" width="$barW" height="$barH" rx="2"/>
         |  $percentLabel
         |  $xLabel
         |</g>""".stripMargin
    }.mkString("\n")

    val xAxis =
      s"""<line class="hot-shard__axis" x1="$LeftPad" y1="$ChartBottom" x2="${LeftPad + PlotW}" y2="$ChartBottom"/>"""
    val yLabel =
      s"""<text class="hot-shard__y-label" x="${LeftPad - 8}" y="$TopPad" text-anchor="end">% of total load</text>"""

    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="hot-shard__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $yLabel
       |  $xAxis
       |  $bars
       |  <text class="hot-shard__x-axis-title" x="${LeftPad + PlotW / 2}" y="${ChartBottom + 36}" text-anchor="middle">shard index</text>
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 8)
      .useStateBy(_ => 1.2)
      .useStateBy(_ => Strategy.Hash)
      .useEffectOnMountBy { (_, specM, shardS, skewS, _) =>
        specM.value match
          case Right(spec) => shardS.setState(spec.shardCount) >> skewS.setState(spec.skew)
          case _           => Callback.empty
      }
      .render { (_, specM, shardS, skewS, stratS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Hot-shard widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val shardCount = shardS.value
            val skew       = skewS.value
            val strategy   = stratS.value
            val load       = loadByShard(shardCount, skew, spec.keyCount, strategy, spec.virtualPerShard)
            val factor     = hotShardFactor(load)
            val verdict =
              if factor < 1.3 then ("Even distribution", "ok")
              else if factor < 2.0 then ("Mildly hot", "warn")
              else if factor < 4.0 then ("Hot shard", "bad")
              else ("Severely hot shard", "bad")

            <.div(
              ^.className := "hot-shard not-prose",
              spec.title
                .map(t => <.p(^.className := "hot-shard__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "hot-shard__frame",
                ^.dangerouslySetInnerHtml := buildSvg(load, factor)
              ),
              <.div(
                ^.className := "hot-shard__controls",
                <.div(
                  ^.className := "hot-shard__strategy-row",
                  <.span(^.className := "hot-shard__strategy-label", "Partitioning strategy:"),
                  strategyButton(Strategy.Range, strategy, stratS, "Range"),
                  strategyButton(Strategy.Hash, strategy, stratS, "Hash"),
                  strategyButton(
                    Strategy.HashVirtual,
                    strategy,
                    stratS,
                    s"Hash + virtual (${spec.virtualPerShard}/shard)"
                  )
                ),
                sliderRow(
                  s"Shards: $shardCount",
                  spec.shardMin,
                  spec.shardMax,
                  shardCount,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(spec.shardCount)
                    shardS.setState(math.max(spec.shardMin, math.min(spec.shardMax, v)))
                  }
                ),
                sliderRow(
                  f"Zipf skew exponent: $skew%.2f",
                  (spec.skewMin * 100).toInt,
                  (spec.skewMax * 100).toInt,
                  (skew * 100).toInt,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(120)
                    skewS.setState(math.max(spec.skewMin, math.min(spec.skewMax, v.toDouble / 100.0)))
                  }
                )
              ),
              <.div(
                ^.className := "hot-shard__readout",
                readoutRow(
                  "Hot-shard factor",
                  f"$factor%.2f×",
                  "max load / mean load. 1× is perfect; >2× is a hot shard you can feel"
                ),
                readoutRow(
                  "Verdict",
                  verdict._1,
                  verdict._2 match
                    case "ok" =>
                      "Distribution is approximately uniform — your hottest shard is doing close to its fair share"
                    case "warn" => "Some skew, but tolerable — most production workloads run here"
                    case "bad" =>
                      "One shard is doing the work of many — under load it will saturate before the others"
                    case _ => ""
                ),
                readoutRow(
                  "What virtual shards do",
                  if strategy == Strategy.HashVirtual then "in use"
                  else s"${spec.virtualPerShard}× more ring positions",
                  "they smooth *medium-popularity* clustering at low shard counts. They do NOT help if one single key carries the load — that needs replication or directory carve-out."
                )
              )
            )
      }

  private def strategyButton(
      s: Strategy,
      current: Strategy,
      state: hooks.Hooks.UseState[Strategy],
      label: String
  ): VdomNode =
    val cls =
      if s == current then "hot-shard__strategy hot-shard__strategy--active"
      else "hot-shard__strategy"
    <.button(
      ^.tpe       := "button",
      ^.className := cls,
      ^.key       := s.toString,
      ^.onClick --> state.setState(s),
      label
    )

  private def sliderRow(
      label: String,
      min: Int,
      max: Int,
      value: Int,
      onInput: ReactEventFromInput => Callback
  ): VdomNode =
    <.label(
      ^.className := "hot-shard__slider-row",
      <.span(^.className := "hot-shard__slider-text", label),
      <.input(
        ^.tpe       := "range",
        ^.className := "hot-shard__slider",
        ^.min       := min.toString,
        ^.max       := max.toString,
        ^.step      := "1",
        ^.value     := value.toString,
        ^.onChange ==> onInput
      )
    )

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "hot-shard__readout-row",
      <.span(^.className := "hot-shard__readout-label", label),
      <.span(^.className := "hot-shard__readout-value", value),
      <.span(^.className := "hot-shard__readout-note", note)
    )
