package codefolio.client.components.cortex.widgets

import codefolio.client.components.cortex.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try

/**
 * Replication-lag widget — two parallel timelines (leader, replica) showing the same sequence of writes
 * arriving at different times. A draggable "read time" cursor on each timeline reveals what value the reader
 * sees if it queries at that moment: the leader always shows the latest committed write; the replica shows
 * whichever write has propagated through the lag window. When the reader's clock is in the lag window after a
 * write, the replica returns *stale data* — that's the read-your-writes hazard, highlighted in red on the
 * readout.
 *
 * Two sliders: `lagMs` (replication lag — sync replication ~10 ms; async cross-region ~50–500 ms) and
 * `readDelayMs` (how long after the latest write the reader queries). Drag `readDelayMs` below `lagMs` and
 * the replica's value diverges from the leader's by exactly one write. That is the difference between strong
 * reads and eventually-consistent reads, made visceral.
 *
 * Re-used in:
 *   - Lesson 11 (replication) — primary.
 *   - Lesson 13 (consistency models) — eventual / monotonic / causal in their canonical timeline shape.
 *   - Capstone 38 (news feed) — the "I posted a status but my own feed doesn't show it" failure mode.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":          "Replication lag — read-your-writes vs eventual",
 *   "lagMs":          80,
 *   "lagRange":       [0, 500],
 *   "readDelayMs":    30,
 *   "readDelayRange": [0, 500],
 *   "writeCount":     5,
 *   "writeIntervalMs": 100
 * }
 * }}}
 *
 *   - `lagMs` is the simulated time the replica is behind the leader on every write.
 *   - `readDelayMs` is how long after the *latest* write the reader queries the system.
 *   - `writeCount` (default 5) is how many writes to include in the timeline; `writeIntervalMs` (100) is the
 *     spacing between writes. The total timeline width is `writeCount * writeIntervalMs + lagMs +
 *     readDelayMs`, with the SVG scaled to fit.
 */
object ReplicationLagSimulator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Spec(
      title: Option[String],
      lagMs: Int,
      lagMin: Int,
      lagMax: Int,
      readDelayMs: Int,
      readDelayMin: Int,
      readDelayMax: Int,
      writeCount: Int,
      writeIntervalMs: Int
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val lag           = d.double("lagMs", 80.0).toInt
      val (lMin, lMx)   = d.intRange("lagRange", (0, 500))
      val rd            = d.double("readDelayMs", 30.0).toInt
      val (rdMin, rdMx) = d.intRange("readDelayRange", (0, 500))
      val wc            = d.double("writeCount", 5.0).toInt
      val wi            = d.double("writeIntervalMs", 100.0).toInt
      if lMin < 0 || lMx < lMin then throw PayloadDecoder.invalid("lagRange must satisfy 0 ≤ min ≤ max")
      if rdMin < 0 || rdMx < rdMin then
        throw PayloadDecoder.invalid("readDelayRange must satisfy 0 ≤ min ≤ max")
      if lag < lMin || lag > lMx then throw PayloadDecoder.invalid("lagMs out of range")
      if rd < rdMin || rd > rdMx then throw PayloadDecoder.invalid("readDelayMs out of range")
      if wc < 1 || wc > 12 then throw PayloadDecoder.invalid("writeCount must be in [1, 12]")
      if wi < 10 then throw PayloadDecoder.invalid("writeIntervalMs must be ≥ 10")
      Spec(d.optString("title"), lag, lMin, lMx, rd, rdMin, rdMx, wc, wi)
    }

  // ===========================================================================
  // Layout
  // ===========================================================================

  private val ViewBoxWidth = 720.0
  private val LeftPad      = 110.0
  private val RightPad     = 30.0
  private val TopPad       = 36.0
  private val LaneHeight   = 80.0
  private val LaneGap      = 14.0
  private val EventR       = 9.0

  private case class Write(idx: Int, leaderTime: Int, replicaTime: Int)

  private def writes(spec: Spec, lagMs: Int): List[Write] =
    (0 until spec.writeCount).toList.map { i =>
      val t = (i + 1) * spec.writeIntervalMs
      Write(i + 1, t, t + lagMs)
    }

  /** Latest write whose arrival time ≤ now. None if no write has arrived yet. */
  private def latestVisible(ws: List[Write], now: Int, onReplica: Boolean): Option[Int] =
    ws.filter(w => (if onReplica then w.replicaTime else w.leaderTime) <= now)
      .maxByOption(_.idx)
      .map(_.idx)

  // ===========================================================================
  // SVG
  // ===========================================================================

  private def buildSvg(spec: Spec, lagMs: Int, readDelayMs: Int): String =
    val ws      = writes(spec, lagMs)
    val timeMax = ws.last.replicaTime + readDelayMs + spec.writeIntervalMs
    val plotW   = ViewBoxWidth - LeftPad - RightPad
    val laneLY  = TopPad
    val laneRY  = TopPad + LaneHeight + LaneGap
    val nowAtMs = ws.last.leaderTime + readDelayMs

    def tx(t: Int): Double =
      if timeMax <= 0 then LeftPad else LeftPad + (t.toDouble / timeMax.toDouble) * plotW

    // Lane labels
    val leaderLabel =
      s"""<text class="replication-lag__lane-label" x="${LeftPad - 12}" y="${laneLY + LaneHeight / 2 + 4}" text-anchor="end">Leader</text>"""
    val replicaLabel =
      s"""<text class="replication-lag__lane-label" x="${LeftPad - 12}" y="${laneRY + LaneHeight / 2 + 4}" text-anchor="end">Replica</text>"""

    // Lane backgrounds + baselines
    val leaderBg =
      s"""<rect class="replication-lag__lane" x="$LeftPad" y="$laneLY" width="$plotW" height="$LaneHeight" rx="4"/>"""
    val replicaBg =
      s"""<rect class="replication-lag__lane" x="$LeftPad" y="$laneRY" width="$plotW" height="$LaneHeight" rx="4"/>"""
    val leaderBaseline =
      s"""<line class="replication-lag__baseline" x1="$LeftPad" y1="${laneLY + LaneHeight - 16}" x2="${LeftPad + plotW}" y2="${laneLY + LaneHeight - 16}"/>"""
    val replicaBaseline =
      s"""<line class="replication-lag__baseline" x1="$LeftPad" y1="${laneRY + LaneHeight - 16}" x2="${LeftPad + plotW}" y2="${laneRY + LaneHeight - 16}"/>"""

    // Write events on leader + replica
    val leaderEvents = ws.map { w =>
      val x = tx(w.leaderTime)
      val y = laneLY + LaneHeight - 16
      s"""<g>
         |  <circle class="replication-lag__write" cx="$x" cy="$y" r="$EventR"/>
         |  <text class="replication-lag__write-label" x="$x" y="${y - 14}" text-anchor="middle">W${w.idx}</text>
         |</g>""".stripMargin
    }.mkString("\n")
    val replicaEvents = ws.map { w =>
      val x = tx(w.replicaTime)
      val y = laneRY + LaneHeight - 16
      s"""<g>
         |  <circle class="replication-lag__write" cx="$x" cy="$y" r="$EventR"/>
         |  <text class="replication-lag__write-label" x="$x" y="${y - 14}" text-anchor="middle">W${w.idx}</text>
         |</g>""".stripMargin
    }.mkString("\n")

    // Arrows between matching writes showing the lag
    val lagArrows = ws.map { w =>
      val x1 = tx(w.leaderTime)
      val x2 = tx(w.replicaTime)
      val y1 = laneLY + LaneHeight - 16
      val y2 = laneRY + LaneHeight - 16
      s"""<line class="replication-lag__arrow" x1="$x1" y1="${y1 + EventR}" x2="$x2" y2="${y2 - EventR}"/>"""
    }.mkString("\n")

    // "Now" cursor — vertical line through both lanes
    val nowX = tx(nowAtMs)
    val nowCursor =
      s"""<g>
         |  <line class="replication-lag__cursor" x1="$nowX" y1="$laneLY" x2="$nowX" y2="${laneRY + LaneHeight}"/>
         |  <text class="replication-lag__cursor-label" x="$nowX" y="${laneRY + LaneHeight + 16}" text-anchor="middle">reader queries here (t=${nowAtMs} ms)</text>
         |</g>""".stripMargin

    val height = laneRY + LaneHeight + 36

    s"""<svg viewBox="0 0 $ViewBoxWidth $height"
       |     class="replication-lag__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $leaderBg
       |  $replicaBg
       |  $leaderBaseline
       |  $replicaBaseline
       |  $leaderLabel
       |  $replicaLabel
       |  $lagArrows
       |  $leaderEvents
       |  $replicaEvents
       |  $nowCursor
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 80)
      .useStateBy(_ => 30)
      .useEffectOnMountBy { (_, specM, lagS, readS) =>
        specM.value match
          case Right(spec) =>
            lagS.setState(spec.lagMs) >> readS.setState(spec.readDelayMs)
          case _ => Callback.empty
      }
      .render { (_, specM, lagS, readS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Replication-lag widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val lag       = lagS.value
            val read      = readS.value
            val ws        = writes(spec, lag)
            val nowMs     = ws.last.leaderTime + read
            val onLeader  = latestVisible(ws, nowMs, onReplica = false).getOrElse(0)
            val onReplica = latestVisible(ws, nowMs, onReplica = true).getOrElse(0)
            val stale     = onReplica < onLeader

            <.div(
              ^.className := "replication-lag not-prose",
              spec.title
                .map(t => <.p(^.className := "replication-lag__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "replication-lag__frame",
                ^.dangerouslySetInnerHtml := buildSvg(spec, lag, read)
              ),
              <.div(
                ^.className := "replication-lag__controls",
                sliderRow(
                  s"Replication lag: $lag ms",
                  spec.lagMin,
                  spec.lagMax,
                  lag,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(spec.lagMs)
                    lagS.setState(math.max(spec.lagMin, math.min(spec.lagMax, v)))
                  }
                ),
                sliderRow(
                  s"Reader queries: ${read} ms after the last write",
                  spec.readDelayMin,
                  spec.readDelayMax,
                  read,
                  (e: ReactEventFromInput) => {
                    val v = Try(e.target.value.toInt).getOrElse(spec.readDelayMs)
                    readS.setState(math.max(spec.readDelayMin, math.min(spec.readDelayMax, v)))
                  }
                )
              ),
              <.div(
                ^.className := "replication-lag__readout",
                readoutRow(
                  "Leader read",
                  if onLeader == 0 then "no writes yet" else s"sees W$onLeader",
                  "always returns the latest committed write"
                ),
                readoutRow(
                  "Replica read",
                  if onReplica == 0 then "no writes yet" else s"sees W$onReplica",
                  if stale then
                    s"stale by ${onLeader - onReplica} write${if onLeader - onReplica == 1 then "" else "s"} — the read-your-writes hazard"
                  else "in sync with the leader for this query"
                ),
                readoutRow(
                  "Verdict",
                  if stale then "Stale read" else "Fresh",
                  if stale then
                    "If the reader is the same user who just wrote, they'll see their own write disappear. Mitigations: read-your-writes routing (read from leader for a TTL after a write), monotonic-read tracking, or accept the staleness and pin to a single replica per session."
                  else "Reader's clock is past the propagation window; consistency is intact for this query"
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
      ^.className := "replication-lag__slider-row",
      <.span(^.className := "replication-lag__slider-text", label),
      <.input(
        ^.tpe       := "range",
        ^.className := "replication-lag__slider",
        ^.min       := min.toString,
        ^.max       := max.toString,
        ^.step      := "1",
        ^.value     := value.toString,
        ^.onChange ==> onInput
      )
    )

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "replication-lag__readout-row",
      <.span(^.className := "replication-lag__readout-label", label),
      <.span(^.className := "replication-lag__readout-value", value),
      <.span(^.className := "replication-lag__readout-note", note)
    )
