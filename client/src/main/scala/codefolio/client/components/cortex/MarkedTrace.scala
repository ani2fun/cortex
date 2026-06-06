package codefolio.client.components.cortex

import codefolio.shared.viz.*

import scala.scalajs.js
import scala.util.Try

/**
 * Shared marker-protocol parser for tracer harnesses.
 *
 * Both [[PythonTracer]] and [[JvmTracer]] emit their step-snapshot JSON between `__CFHEAP_BEGIN__` /
 * `__CFHEAP_END__` markers. [[parse]] extracts the two parts so each tracer's `parse` method can delegate
 * here rather than carrying its own copy of the decode pipeline.
 *
 * The decode pipeline (`decodeTrace` → `decodeStep` → `decodeFrame` → `decodeValue` / `decodeObject` /
 * `entriesOf`) is backend-language-agnostic: both the Python harness and the JVM harness emit the same
 * `{steps, truncated}` JSON shape with the same `{line, event, frames, heap}` step structure, so a single
 * decoder covers both.
 */
object MarkedTrace:

  /** A parsed trace plus the user program's own stdout (everything printed before the begin marker). */
  final case class TraceResult(trace: HeapTrace, programStdout: String)

  private val BeginMarker = "__CFHEAP_BEGIN__"
  private val EndMarker   = "__CFHEAP_END__"

  // ---------------------------------------------------------------------------
  // Parsing — split the marker-delimited JSON out of stdout and decode it.
  // ---------------------------------------------------------------------------

  /**
   * Split the harness output: everything before `__CFHEAP_BEGIN__` is the user's program stdout; the JSON
   * between the markers is the trace. Missing markers (compile error, fatal harness exception) → an empty
   * trace and the raw stdout as program output.
   */
  def parse(stdout: String): TraceResult =
    val beginIdx = stdout.lastIndexOf(BeginMarker)
    if beginIdx < 0 then TraceResult(HeapTrace(Nil, truncated = false), stdout.stripSuffix("\n"))
    else
      val afterBegin = stdout.substring(beginIdx + BeginMarker.length)
      val endIdx     = afterBegin.indexOf(EndMarker)
      val jsonRaw    = if endIdx < 0 then afterBegin else afterBegin.substring(0, endIdx)
      val programOut = stdout.substring(0, beginIdx).stripSuffix("\n")
      val trace      = Try(decodeTrace(jsonRaw.trim)).getOrElse(HeapTrace(Nil, truncated = false))
      TraceResult(trace, programOut)

  // ---------------------------------------------------------------------------
  // Private decode helpers — all in terms of js.Dynamic / js.JSON to stay
  // Scala.js compatible with no JVM-only imports.
  // ---------------------------------------------------------------------------

  private def decodeTrace(json: String): HeapTrace =
    val root      = js.JSON.parse(json)
    val truncated = root.truncated.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
    val stepsArr  = root.steps.asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]]
    val steps     = stepsArr.toOption.fold(List.empty[HeapStep])(_.toList.map(decodeStep))
    HeapTrace(steps, truncated)

  private def decodeStep(s: js.Dynamic): HeapStep =
    val line      = s.line.asInstanceOf[js.UndefOr[Int]].getOrElse(0)
    val event     = s.event.asInstanceOf[js.UndefOr[String]].getOrElse("line")
    val framesArr = s.frames.asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]]
    val frames    = framesArr.toOption.fold(List.empty[HeapFrame])(_.toList.map(decodeFrame))
    val heap = entriesOf(s.heap).collect {
      case (k, v) if v != null && !js.isUndefined(v) => k -> decodeObject(v)
    }.toMap
    HeapStep(line, event, frames, heap)

  private def decodeFrame(f: js.Dynamic): HeapFrame =
    val fn     = f.fn.asInstanceOf[js.UndefOr[String]].getOrElse("")
    val locals = entriesOf(f.locals).map { case (k, v) => k -> decodeValue(v) }
    HeapFrame(fn, locals)

  /** Own-enumerable string keys of a JS object, as `(key, value)` pairs. */
  private def entriesOf(d: js.Dynamic): List[(String, js.Dynamic)] =
    if js.isUndefined(d) || d == null then Nil
    else
      val obj = d.asInstanceOf[js.Object]
      js.Object.keys(obj).toList.map(k => k -> d.selectDynamic(k))

  private def decodeValue(v: js.Dynamic): HeapValue =
    if v == null then HeapValue.Scalar(HeapScalar.Null)
    else
      js.typeOf(v) match
        case "number" =>
          val d = v.asInstanceOf[Double]
          if d.isWhole then HeapValue.Scalar(HeapScalar.I(d.toLong))
          else HeapValue.Scalar(HeapScalar.D(d))
        case "boolean" => HeapValue.Scalar(HeapScalar.B(v.asInstanceOf[Boolean]))
        case "string"  => HeapValue.Scalar(HeapScalar.S(v.asInstanceOf[String]))
        case "object" =>
          v.ref.asInstanceOf[js.UndefOr[String]].toOption match
            case Some(id) => HeapValue.Ref(id)
            case None     => HeapValue.Scalar(HeapScalar.Null)
        case _ => HeapValue.Scalar(HeapScalar.Null)

  private def decodeObject(d: js.Dynamic): HeapObject =
    d.selectDynamic("type").asInstanceOf[js.UndefOr[String]].getOrElse("object") match
      // `"list"` / `"tuple"` come from the Python harness; `"array"` comes from the JVM harness (Slice 4
      // onwards) for native Java arrays (`int[]`, `Object[]`, …). All three are structurally identical
      // sequences of items; only the [[ArrKind]] tag differs so the renderer can badge them appropriately.
      case "list" | "tuple" | "array" =>
        val tpe = d.selectDynamic("type").asInstanceOf[String]
        val kind =
          if tpe == "tuple" then ArrKind.Tup
          else if tpe == "array" then ArrKind.JArr
          else ArrKind.Lst
        val items = d.items
          .asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]]
          .toOption
          .fold(List.empty[HeapValue])(_.toList.map(decodeValue))
        HeapObject.Arr(kind, items)
      case "dict" =>
        val entries = d.entries
          .asInstanceOf[js.UndefOr[js.Array[js.Array[js.Dynamic]]]]
          .toOption
          .fold(List.empty[(HeapValue, HeapValue)]) { arr =>
            arr.toList.collect {
              case pair if pair.length >= 2 => decodeValue(pair(0)) -> decodeValue(pair(1))
            }
          }
        HeapObject.Dict(entries)
      case _ =>
        val cls    = d.cls.asInstanceOf[js.UndefOr[String]].getOrElse("object")
        val fields = entriesOf(d.fields).map { case (k, v) => k -> decodeValue(v) }
        HeapObject.Instance(cls, fields)
