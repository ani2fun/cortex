package codefolio.shared.viz

/**
 * The raw heap-snapshot trace produced by a tracer harness (ADR-0018, ADR-0021).
 *
 * Each [[HeapStep]] is one tracer event: the source line, the call stack (a list of [[HeapFrame]]s, innermost
 * first), and a snapshot of the reachable object graph (the "heap"). [[HeapToGraph]] turns a `HeapTrace` into
 * a renderable [[VizGraph]].
 *
 * Lives in `shared` so the adapter that consumes these types cross-compiles to the JVM (unit tests) and to
 * Scala.js (the Visualise modal). The client decodes the harness JSON into these types directly; there are no
 * codecs here — `HeapToGraphSpec` builds `HeapTrace` values by hand.
 */

/** A scalar leaf value inside a heap snapshot. */
enum HeapScalar:
  case I(value: Long)
  case D(value: Double)
  case B(value: Boolean)
  case S(value: String)
  case Null

/** A field / slot value: an inline scalar, or a reference to a heap object by its id. */
enum HeapValue:
  case Scalar(value: HeapScalar)
  case Ref(id: String)

/**
 * Whether a sequence object came from a Python `list`, a Python `tuple`, or a native Java array (`int[]`,
 * `Object[]`, …). The renderer uses this for cell badging / bracket styling; otherwise the three are
 * structurally identical sequences of `HeapValue` items.
 */
enum ArrKind:
  case Lst, Tup, JArr

/** A heap object: a class instance, a list/tuple/array, or a dict. */
enum HeapObject:
  case Instance(cls: String, fields: List[(String, HeapValue)])
  case Arr(kind: ArrKind, items: List[HeapValue])
  case Dict(entries: List[(HeapValue, HeapValue)])

/**
 * One stack frame in a traced step — the enclosing function name and the frame's local variables. In a
 * [[HeapStep]], `frames.head` is the innermost (active) frame; the rest are pending callers, inner-to-outer.
 * Active-frame-only consumers ([[HeapToGraph]], `TracedCodeBlock.renderLocalsPanel`) read `frames.head`; a
 * recursion-aware UI reads the full list. See ADR-0021.
 */
final case class HeapFrame(
    fn: String,
    locals: List[(String, HeapValue)]
)

/**
 * One traced step.
 *
 *   - `line` — 1-based source line about to run / just run.
 *   - `event` — the tracer event (`line` / `call` / `return`).
 *   - `frames` — the call stack, innermost first; non-empty for any step the harness emits.
 *   - `heap` — every reachable object this step, id → object.
 */
final case class HeapStep(
    line: Int,
    event: String,
    frames: List[HeapFrame],
    heap: Map[String, HeapObject]
)

/** A whole trace. `truncated` is set when the harness dropped steps to fit its payload budget. */
final case class HeapTrace(steps: List[HeapStep], truncated: Boolean)
