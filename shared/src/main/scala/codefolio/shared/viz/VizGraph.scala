package codefolio.shared.viz

import io.circe.Encoder
import io.circe.generic.semiauto.*

/**
 * The renderer's contract — a generic, layout-agnostic graph animation (ADR-0018).
 *
 * [[HeapToGraph]] produces a `VizGraph` from a [[HeapTrace]]; the Scala.js Visualise modal circe-encodes it
 * to JSON and hands it to the standalone D3 renderer (`client/src/d3`). The renderer draws nodes + edges and
 * animates the steps; *where* a node sits is decided by a pluggable layout selected from
 * [[VizGraph.layoutHint]]. The renderer is one generic module — adding a data structure adds a layout, never
 * a renderer.
 */

/** An extra display field on a node — a non-pointer scalar (e.g. `height=2`), shown as a sub-label. */
final case class VizField(name: String, value: String)

/**
 * A node in one animation step.
 *
 *   - `kind` — the originating Python class for an instance, or a synthetic kind for a collection member:
 *     `"cell"` (an array / list element) or `"entry"` (a dict entry). It drives CSS.
 *   - `meta` — the object's other scalar fields (everything but the primary value), surfaced as small
 *     sub-labels so the diagram shows per-node state (an AVL `height`, a colour, a dict entry's `key`, …),
 *     not just the value.
 *   - `slot` — an array cell's 0-based index; `None` for instances and dict entries. The array layout places
 *     cells by `slot`, so a value moves between fixed boxes the way an in-place sort does.
 *   - `cardId` — the per-object grouping key (Slice 3, auto-dispatch). Cells / entries share their owning
 *     `Arr` / `Dict`'s heap-object id; `Instance`s reachable from one another via field references
 *     ([[HeapToGraph]] union-find) share the representative instance id; an isolated `Instance` is its own
 *     card. The renderer groups nodes by `cardId` and dispatches each card to its [[layoutKind]]. Empty when
 *     the payload was hand-curated (no adapter), in which case the renderer falls back to the single-layout
 *     [[VizGraph.layoutHint]] path.
 *   - `layoutKind` — the inferred layout per card (Slice 3). One of `tree-binary`, `list-single`,
 *     `list-double`, `hashmap`, `array-1d`, `array-2d`, `graph-generic` — or the older overlap names
 *     (`binary-tree`, `linked-list`, `array`, `grid`, `graph`) when the trace's [[VizGraph.layoutHint]]
 *     forces a per-trace override. Every node from one heap object's group carries the same value.
 */
final case class VizNode(
    id: String,
    label: String,
    kind: String,
    meta: List[VizField],
    slot: Option[Int] = None,
    cardId: String = "",
    layoutKind: String = ""
)

/** A directed edge; `label` is the field it came from (e.g. "left" / "right"). */
final case class VizEdge(from: String, to: String, label: String)

/**
 * A frame-local variable pointing into the structure: `name` is the Python variable, `target` the id of the
 * node it references, `color` its role colour from [[MarkerColors]] (assigned by `HeapToGraph`, stable for
 * the whole trace). Drawn as a labelled, role-coloured caret over the node, so a step holding several
 * references — a long-lived `root` plus a moving `node` — shows *which* pointer sits where.
 */
final case class VizCursor(name: String, target: String, color: String = "")

/**
 * A rich, editorial annotation for one animation step — rendered beside the heap card.
 *
 *   - `eyebrow` — the tracer event kind: `"call"` / `"line"` / `"return"` / `"exception"`. Drives the eyebrow
 *     colour (deep blue / muted / moss / bordeaux) via `.canvas__annotation-eyebrow--${eyebrow}`.
 *   - `title` — short diff summary (what `narrate` historically produced); rendered in editorial italic.
 *   - `body` — the source line that this step is executing; rendered as sans body text under the title.
 *   - `link` — optional `<a class="canvas__annotation-link">` href; reserved for future deep links.
 */
final case class Annotation(
    eyebrow: String,
    title: String,
    body: String,
    link: Option[String] = None
)

/** One local variable in a stack frame — display-ready for the StackFramesPanel (Slice 2). */
final case class VizLocal(
    name: String,     // variable name
    typeName: String, // inferred type label ("int", "TreeNode", "list", …)
    value: String,    // display string
    changed: Boolean  // true when this var's value differs from the previous step
)

/**
 * A stack frame captured at one animation step, rendered by the StackFramesPanel (Slice 2).
 *
 * `frames.head` in a [[VizGraphStep]] is the innermost (active) frame. Helper frames ([[HeapToGraph]]
 * `isHelperFrame`) and the Python `self` variable are excluded. `isActive` is true only for the innermost
 * frame (index 0 in the call stack).
 */
final case class VizFrame(fn: String, locals: List[VizLocal], isActive: Boolean)

/**
 * One animation step: the graph state + emphasis + the source line that produced it.
 *
 *   - `cursor` — the frame's local variables that point into the structure, each a name → node-id pair. Only
 *     Refs whose target is itself a node (an `Instance`, or a synthesised cell / entry for index cursors); a
 *     Ref to an `Arr` / `Dict` doesn't appear here because those objects don't emit a parent node — the
 *     per-node cursor mark would have nothing to land on. See [[cardCursor]] for that case.
 *   - `cardCursor` — Slice 4. Refs from the active frame whose target is a card root (an Arr, Dict, or
 *     Instance-group representative). The target is the `cardId` itself, NOT a node id, so the client's
 *     ArrowLayer can draw a curve from the locals row to the matching card without resolving through nodes.
 *   - `highlight` — node ids that are new this step (freshly created / attached).
 *   - `changed` — node ids whose display value differs from the previous step.
 *   - `removed` — node ids gone since the previous step, re-emitted into `nodes` once so the renderer can
 *     fade them out; they are absent from every later step.
 *   - `annotation` — a rich editorial annotation for this step: event-kind eyebrow (call / line / return /
 *     exception), short italic title (the diff summary), and a body line (the source line being executed).
 *     Rendered as `.canvas__annotation--rich` beside the heap card; `title` alone is also used as the
 *     timeline row's summary.
 *   - `line` — 1-based source line, so the modal's code pane can highlight in sync.
 *   - `frames` — the call stack at this step, for the StackFramesPanel (Slice 2); innermost frame first.
 *   - `unchanged` — Slice 6. True when only the source line advanced: the heap (all node ids + every node's
 *     label, kind, meta, slot) AND the active-frame locals (name, typeName, value) are both identical to the
 *     previous included step. When diff mode is on, the modal filters these steps out so the user steps
 *     through only the structural changes. Always false for step 0.
 *   - `structureType` — the bespoke-renderer axis (ADR-0024). Orthogonal to `VizNode.layoutKind`: layoutKind
 *     decides *where* nodes sit (tree / list / array / hashmap geometry); `structureType` decides *which*
 *     chrome wraps them ("stack", "queue", "deque", "heap", "trie", "union-find", …). Set on every step of a
 *     graph, derived once at adapt time from either the markdown `viz-kind=` attribute (explicit hint, wins)
 *     or [[HeapToGraph]]'s structure-type inference (e.g. an `Arr` whose locals include `top` / `push` /
 *     `pop` → `"stack"`). `None` falls back to the generic renderer, preserving today's behaviour.
 *
 * `highlight` is a persistent-layer cue (a new node keeps its tint); `changed` / `removed` are
 * transient-layer cues (a one-step flash / fade) — the renderer keeps the two on separate class sets.
 */
final case class VizGraphStep(
    nodes: List[VizNode],
    edges: List[VizEdge],
    cursor: List[VizCursor],
    highlight: List[String],
    changed: List[String],
    removed: List[String],
    annotation: Annotation,
    line: Int,
    frames: List[VizFrame] = Nil,      // Slice 2 — call stack for the StackFramesPanel
    cardCursor: List[VizCursor] = Nil, // Slice 4 — card-level cursors for the ArrowLayer
    unchanged: Boolean = false, // Slice 6 — true when only source line changed; heap + locals identical
    structureType: Option[String] = None // ADR-0024 — bespoke-renderer dispatch axis
)

/** One traced test case's animation. `truncated` mirrors [[HeapTrace.truncated]]. */
final case class VizGraph(
    steps: List[VizGraphStep],
    layoutHint: String,
    title: String,
    truncated: Boolean
)

/**
 * The full payload handed to the D3 renderer — one [[VizGraph]] per traced test case.
 *
 * A DSA `main` typically runs several test cases in sequence; [[HeapToGraph]] segments the trace at each case
 * boundary (the viz-root variable rebound to a fresh structure) and emits one `VizGraph` per case. The
 * payload is a *list* of graphs rather than one graph carrying boundary indices because ADR-0018 computes
 * layout once over the union of every step — and each case is a different structure with a different union,
 * so each wants its own. The modal renders one case at a time and offers a "Case N / M" selector; a
 * single-case trace is just a `VizCases` of one.
 */
final case class VizCases(cases: List[VizGraph])

object VizGraph:
  given Encoder[VizField]     = deriveEncoder
  given Encoder[VizNode]      = deriveEncoder
  given Encoder[VizEdge]      = deriveEncoder
  given Encoder[VizCursor]    = deriveEncoder
  given Encoder[Annotation]   = deriveEncoder
  given Encoder[VizLocal]     = deriveEncoder
  given Encoder[VizFrame]     = deriveEncoder
  given Encoder[VizGraphStep] = deriveEncoder
  given Encoder[VizGraph]     = deriveEncoder
  given Encoder[VizCases]     = deriveEncoder
