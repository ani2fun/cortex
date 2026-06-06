package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.Try

/**
 * Back-of-envelope estimation calculator — the interactive companion to lesson 3's Python BOTE template. The
 * reader picks a service preset (or types their own numbers) and the widget computes write/read QPS, annual
 * storage growth, and peak egress bandwidth live, using the exact formula the lesson body teaches:
 *
 *   - write QPS (avg) = DAU × writes-per-user-per-day / 86,400
 *   - read QPS (avg) = DAU × reads-per-user-per-day / 86,400
 *   - peak QPS = avg × peak-factor (default 3×)
 *   - storage/year = DAU × writes-per-user × 365 × bytes-per-write × replication-factor (default 3×)
 *   - egress at peak = read-QPS-peak × bytes-per-write
 *
 * The widget is the practice surface; the prose paragraph just above it is the *explanation* the reader can
 * always go back to. Numbers update on every keystroke / slider tick so the relationship between inputs and
 * outputs is immediately legible.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Back-of-envelope — pick a preset or punch in your own numbers",
 *   "peakFactor":        3,
 *   "replicationFactor": 3,
 *   "presets": [
 *     { "name": "Twitter/X (illustrative)",     "dau": 200000000,   "writesPerUser": 2,   "readsPerUser": 50,  "bytesPerWrite": 1000 },
 *     { "name": "YouTube views (illustrative)", "dau": 750000000,   "writesPerUser": 0,   "readsPerUser": 10,  "bytesPerWrite": 5000000 },
 *     { "name": "WhatsApp (illustrative)",      "dau": 2000000000,  "writesPerUser": 30,  "readsPerUser": 30,  "bytesPerWrite": 250 }
 *   ]
 * }
 * }}}
 *
 *   - `peakFactor` and `replicationFactor` are author-tunable; the lesson defaults (3× / 3×) match the prose.
 *   - Presets seed the four inputs on click; the reader can then override any field. The "Custom"
 *     pseudo-preset is just "whatever's currently in the inputs" — there's no extra schema entry for it.
 *
 * Same string-built SVG-or-HTML / scalajs-react / inline-CSS-class pattern as ArrayTraversal and
 * LatencyScaledTime. The only structural difference: this widget renders a `<form>`-shaped surface (number
 * inputs + buttons), not an SVG. Layout is a 2-column grid that collapses to 1-column on narrow screens.
 */
object EstimationCalculator:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Preset(
      name: String,
      dau: Double,
      writesPerUser: Double,
      readsPerUser: Double,
      bytesPerWrite: Double
  )

  final case class Spec(
      title: Option[String],
      peakFactor: Double,
      replicationFactor: Double,
      presets: List[Preset]
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Constants — the universal "five numbers" from the lesson body. SecondsPerDay
  // uses the rounded 100,000 the lesson teaches, not the precise 86,400.
  // ===========================================================================

  private val SecondsPerDay = 100_000.0
  private val DaysPerYear   = 365.0

  // ===========================================================================
  // Parsing — js.JSON.parse + js.Dynamic, same as the other widgets.
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val peakF = d.double("peakFactor", 3.0)
      val replF = d.double("replicationFactor", 3.0)
      val presets = d.dynList("presets").map(p =>
        Preset(
          name = p.string("name"),
          dau = p.double("dau"),
          writesPerUser = p.double("writesPerUser"),
          readsPerUser = p.double("readsPerUser"),
          bytesPerWrite = p.double("bytesPerWrite")
        )
      )
      if presets.isEmpty then throw PayloadDecoder.invalid("presets must be non-empty")
      if peakF <= 0 then throw PayloadDecoder.invalid("peakFactor must be > 0")
      if replF <= 0 then throw PayloadDecoder.invalid("replicationFactor must be > 0")
      if presets.exists(_.name.trim.isEmpty) then
        throw PayloadDecoder.invalid("every preset.name must be non-empty")
      Spec(d.optString("title"), peakF, replF, presets)
    }

  // ===========================================================================
  // Formatting helpers
  // ===========================================================================

  private def fmtNumber(n: Double): String =
    if n.isNaN || n.isInfinite then "—"
    else if n >= 1.0e12 then f"${n / 1.0e12}%.1f T"
    else if n >= 1.0e9 then f"${n / 1.0e9}%.1f B"
    else if n >= 1.0e6 then f"${n / 1.0e6}%.1f M"
    else if n >= 1.0e3 then f"${n / 1.0e3}%.1f K"
    else f"$n%.1f"

  private def fmtBytes(bytes: Double): String =
    if bytes.isNaN || bytes.isInfinite then "—"
    else if bytes >= 1.0e15 then f"${bytes / 1.0e15}%.1f PB"
    else if bytes >= 1.0e12 then f"${bytes / 1.0e12}%.1f TB"
    else if bytes >= 1.0e9 then f"${bytes / 1.0e9}%.1f GB"
    else if bytes >= 1.0e6 then f"${bytes / 1.0e6}%.1f MB"
    else if bytes >= 1.0e3 then f"${bytes / 1.0e3}%.1f KB"
    else f"$bytes%.0f B"

  private def fmtBitsPerSec(bytesPerSec: Double): String =
    val bps = bytesPerSec * 8
    if bps.isNaN || bps.isInfinite then "—"
    else if bps >= 1.0e12 then f"${bps / 1.0e12}%.1f Tbps"
    else if bps >= 1.0e9 then f"${bps / 1.0e9}%.1f Gbps"
    else if bps >= 1.0e6 then f"${bps / 1.0e6}%.1f Mbps"
    else if bps >= 1.0e3 then f"${bps / 1.0e3}%.1f Kbps"
    else f"$bps%.0f bps"

  private def fmtRatio(reads: Double, writes: Double): String =
    if writes <= 0 then "—"
    else f"${reads / writes}%.0f×"

  // ===========================================================================
  // Component
  // ===========================================================================

  // The user-mutable state lives in four numeric fields. We hold each as a
  // String so partial-edit states (an empty input, a typed-but-not-yet-numeric
  // value) survive across renders without snapping back to the parsed Double.
  private case class FieldsState(dau: String, writes: String, reads: String, bytes: String)

  private object FieldsState:

    def from(p: Preset): FieldsState =
      FieldsState(
        dau = p.dau.toLong.toString,
        writes = stripTrailingZero(p.writesPerUser),
        reads = stripTrailingZero(p.readsPerUser),
        bytes = p.bytesPerWrite.toLong.toString
      )

  private def stripTrailingZero(d: Double): String =
    if d == d.toLong.toDouble then d.toLong.toString else d.toString

  private def parseField(s: String): Double =
    Try(s.trim.toDouble).toOption.filter(_ >= 0).getOrElse(0.0)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(FieldsState("0", "0", "0", "0"))
      .useState(-1)
      .useEffectOnMountBy { (_, specM, fieldsS, presetS) =>
        specM.value match
          case Right(spec) if presetS.value < 0 && spec.presets.nonEmpty =>
            fieldsS.setState(FieldsState.from(spec.presets.head)) >> presetS.setState(0)
          case _ => Callback.empty
      }
      .render { (_, specM, fieldsS, presetS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Estimation widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val fields        = fieldsS.value
            val dau           = parseField(fields.dau)
            val writesPerUser = parseField(fields.writes)
            val readsPerUser  = parseField(fields.reads)
            val bytesPerWrite = parseField(fields.bytes)

            val writeQpsAvg       = dau * writesPerUser / SecondsPerDay
            val readQpsAvg        = dau * readsPerUser / SecondsPerDay
            val writeQpsPeak      = writeQpsAvg * spec.peakFactor
            val readQpsPeak       = readQpsAvg * spec.peakFactor
            val writesPerYear     = dau * writesPerUser * DaysPerYear
            val storagePerYear    = writesPerYear * bytesPerWrite * spec.replicationFactor
            val egressBytesAtPeak = readQpsPeak * bytesPerWrite

            <.div(
              ^.className := "estimation-calculator not-prose",
              spec.title
                .map(t => <.p(^.className := "estimation-calculator__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              // Preset chips
              <.div(
                ^.className := "estimation-calculator__presets",
                spec.presets.zipWithIndex.toVdomArray { case (preset, idx) =>
                  val active = presetS.value == idx
                  <.button(
                    ^.key := s"preset-$idx",
                    ^.tpe := "button",
                    ^.className := s"estimation-calculator__preset${
                        if active then " estimation-calculator__preset--active" else ""
                      }",
                    ^.onClick --> (fieldsS.setState(FieldsState.from(preset)) >> presetS.setState(idx)),
                    preset.name
                  )
                }
              ),
              // Inputs
              <.div(
                ^.className := "estimation-calculator__fields",
                inputField(
                  "Daily active users",
                  "DAU — round to the nearest million",
                  fields.dau,
                  "users",
                  (s, v) => s.copy(dau = v),
                  fieldsS,
                  presetS
                ),
                inputField(
                  "Writes / user / day",
                  "e.g. 2 for tweets, 30 for messaging",
                  fields.writes,
                  "per day",
                  (s, v) => s.copy(writes = v),
                  fieldsS,
                  presetS
                ),
                inputField(
                  "Reads / user / day",
                  "e.g. 50 for timelines, 10 for video views",
                  fields.reads,
                  "per day",
                  (s, v) => s.copy(reads = v),
                  fieldsS,
                  presetS
                ),
                inputField(
                  "Bytes per write",
                  "1 KB text, 5 MB image, 5 GB hour-of-video",
                  fields.bytes,
                  "bytes",
                  (s, v) => s.copy(bytes = v),
                  fieldsS,
                  presetS
                )
              ),
              // Output panel
              <.div(
                ^.className := "estimation-calculator__outputs",
                outputRow("Write QPS (avg)", fmtNumber(writeQpsAvg), "writes/s"),
                outputRow(
                  s"Write QPS (peak ×${stripTrailingZero(spec.peakFactor)})",
                  fmtNumber(writeQpsPeak),
                  "writes/s"
                ),
                outputRow("Read QPS (avg)", fmtNumber(readQpsAvg), "reads/s"),
                outputRow(
                  s"Read QPS (peak ×${stripTrailingZero(spec.peakFactor)})",
                  fmtNumber(readQpsPeak),
                  "reads/s"
                ),
                outputRow("Read / write ratio", fmtRatio(readsPerUser, writesPerUser), ""),
                outputRow(
                  s"Storage growth / year (×${stripTrailingZero(spec.replicationFactor)} replication)",
                  fmtBytes(storagePerYear),
                  ""
                ),
                outputRow("Egress at peak", fmtBitsPerSec(egressBytesAtPeak), "")
              )
            )
      }

  /**
   * One labelled numeric input row. Edits write the typed value into the targeted field and clear the
   * active-preset highlight (so the chip strip honestly reflects whether the inputs still match the preset).
   */
  private def inputField(
      label: String,
      hint: String,
      value: String,
      suffix: String,
      copyWith: (FieldsState, String) => FieldsState,
      fieldsS: Hooks.UseState[FieldsState],
      presetS: Hooks.UseState[Int]
  ): VdomNode =
    <.label(
      ^.className := "estimation-calculator__field",
      <.span(^.className := "estimation-calculator__field-label", label),
      <.div(
        ^.className := "estimation-calculator__field-row",
        <.input(
          ^.tpe       := "number",
          ^.className := "estimation-calculator__field-input",
          ^.value     := value,
          ^.min       := "0",
          ^.onChange ==> { (e: ReactEventFromInput) =>
            val v = e.target.value
            fieldsS.modState(s => copyWith(s, v)) >> presetS.setState(-1)
          }
        ),
        <.span(^.className := "estimation-calculator__field-suffix", suffix)
      ),
      <.span(^.className := "estimation-calculator__field-hint", hint)
    )

  /** One row in the read-only output panel. */
  private def outputRow(label: String, value: String, suffix: String): VdomNode =
    val suffixNode: VdomNode =
      if suffix.nonEmpty then
        <.span(^.className := "estimation-calculator__output-suffix", s" $suffix")
      else EmptyVdom
    <.div(
      ^.className := "estimation-calculator__output-row",
      <.span(^.className := "estimation-calculator__output-label", label),
      <.span(
        ^.className := "estimation-calculator__output-value",
        value,
        suffixNode
      )
    )
