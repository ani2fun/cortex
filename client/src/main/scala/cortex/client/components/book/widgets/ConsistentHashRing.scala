package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js
import scala.util.Try
import scala.util.hashing.MurmurHash3

/**
 * Consistent-hash-ring widget — the senior moment of Lesson 7 made visceral. A circular SVG ring with
 * physical-node markers, virtual-node markers, and key markers placed at deterministic hash-based positions
 * around it. Each key belongs to the next virtual node clockwise; the rendering colours every key by the
 * physical node that owns it.
 *
 * Two sliders: physical-node count (1–N) and virtual-nodes-per-physical (1–M). A live readout shows the
 * keys-per-node distribution and — the killer comparison — "adding one more node remaps X of K keys under
 * consistent hashing vs Y of K keys under plain modulo hashing". That's the empirical lesson: modulo hashing
 * almost-always remaps almost-all keys; consistent hashing remaps approximately K/N keys.
 *
 * Re-used in:
 *   - Lesson 7 (load balancing) — the canonical introduction.
 *   - Lesson 8 (caching) — client-side hash routing across a Redis / Memcached fleet.
 *   - Lesson 12 (sharding) — the entire shard map is one of these.
 *   - Capstone 37 (URL shortener) — partition map for the read path.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title":             "Consistent hashing — drag the sliders, watch which keys remap",
 *   "nodeCount":         4,
 *   "nodeRange":         [1, 8],
 *   "virtualNodes":      1,
 *   "virtualNodeRange":  [1, 50],
 *   "keyCount":          24
 * }
 * }}}
 *
 *   - `nodeCount` is the slider's starting value for physical nodes; `nodeRange` is the slider's bounds.
 *     Default range [1, 8] — enough to feel the law of large numbers without crowding the ring.
 *   - `virtualNodes` is the per-physical-node count. With 1, the load distribution is uneven (especially at
 *     low node counts). Crank it to 20-50 and the distribution smooths out — that's the demonstration the
 *     lesson wants.
 *   - `keyCount` is fixed by the spec (no slider). The widget computes ownership for every key at every
 *     slider change. With max 8 nodes × 50 vnodes × 24 keys it remains microsecond-fast.
 *
 * Hash function is two MurmurHash3 passes (different seeds) XORed and run through a final avalanche pass — a
 * single MurmurHash3 pass on short prefix-shared inputs ("node-0-v0", "node-1-v0", …) was empirically
 * clustering ring positions. Positions on the ring are `unsigned(hash) / 2^32 × 2π`. Key positions, virtual-
 * node positions, and physical-node primary positions are all derived from the same hash, so the
 * visualisation is reproducible.
 */
object ConsistentHashRing:

  // ===========================================================================
  // Schema
  // ===========================================================================

  final case class Spec(
      title: Option[String],
      nodeCount: Int,
      nodeMin: Int,
      nodeMax: Int,
      virtualNodes: Int,
      virtualNodesMin: Int,
      virtualNodesMax: Int,
      keyCount: Int
  )

  final case class Props(payload: String)

  // ===========================================================================
  // Hash function — MurmurHash3, mixed two-stream + finalizer.
  //
  // Single-pass MurmurHash3 of short prefix-shared strings ("node-0-v0",
  // "node-1-v0", …) was empirically clustering on the ring (the
  // four-physical-nodes case showed positions spread over only ~50° out
  // of 360°). Mixing two MurmurHash3 streams with different seeds and
  // putting the result through one more avalanche pass yields the
  // approximately-uniform spread the consistent-hashing visualisation
  // depends on.
  // ===========================================================================

  private val SeedA: Int = 0x9747b28c
  private val SeedB: Int = 0xa1b2c3d4

  private def hash32(s: String): Int =
    val a = MurmurHash3.stringHash(s, SeedA)
    val b = MurmurHash3.stringHash(s, SeedB)
    var h = a ^ Integer.rotateLeft(b, 17)
    h ^= h >>> 16
    h *= 0x85ebca6b
    h ^= h >>> 13
    h *= 0xc2b2ae35
    h ^= h >>> 16
    h

  /** Map any string deterministically to an angle in [0, 2π). */
  private def positionOf(s: String): Double =
    val signed = hash32(s)
    val norm   = (signed.toLong & 0xffffffffL).toDouble / 0x100000000L.toDouble
    norm * 2 * math.Pi

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val nc            = d.double("nodeCount", 4.0).toInt
      val (nMin, nMx)   = d.intRange("nodeRange", (1, 8))
      val vn            = d.double("virtualNodes", 1.0).toInt
      val (vnMin, vnMx) = d.intRange("virtualNodeRange", (1, 50))
      val kc            = d.double("keyCount", 24.0).toInt
      if nMin < 1 || nMx < nMin then throw PayloadDecoder.invalid("nodeRange must satisfy 1 ≤ min ≤ max")
      if vnMin < 1 || vnMx < vnMin then
        throw PayloadDecoder.invalid("virtualNodeRange must satisfy 1 ≤ min ≤ max")
      if nc < nMin || nc > nMx then
        throw PayloadDecoder.invalid(s"nodeCount must fall within nodeRange [$nMin, $nMx]")
      if vn < vnMin || vn > vnMx then
        throw PayloadDecoder.invalid(s"virtualNodes must fall within virtualNodeRange [$vnMin, $vnMx]")
      if kc < 1 || kc > 64 then throw PayloadDecoder.invalid("keyCount must be in [1, 64]")
      Spec(d.optString("title"), nc, nMin, nMx, vn, vnMin, vnMx, kc)
    }

  // ===========================================================================
  // Ring data — vnodes sorted, key ownership, per-node load
  // ===========================================================================

  final private case class VNode(physicalIdx: Int, vIdx: Int, pos: Double)

  /**
   * Build all virtual nodes (one per (physical, vIdx) pair) sorted by angular position. We use Scala's stdlib
   * `sortInPlaceBy` rather than `java.util.Arrays.sort(Array[Object], Comparator)` because the JVM- style
   * overload silently breaks on Scala.js for non-primitive arrays — the dropped sort manifests as
   * pathological load distributions (the bug this method existed for, observed on Lesson 7's first build).
   */
  private def buildVNodes(nodeCount: Int, virtualNodes: Int): Array[VNode] =
    val out = Array.tabulate(nodeCount * virtualNodes) { idx =>
      val i = idx / virtualNodes
      val k = idx % virtualNodes
      VNode(i, k, positionOf(s"node-$i-v$k"))
    }
    out.sortInPlaceBy(_.pos)
    out

  /** Owner physical index of a key, picked as the first vnode clockwise (i.e. next increasing position). */
  private def ownerOfKey(keyPos: Double, sortedVNodes: Array[VNode]): Int =
    var lo = 0
    var hi = sortedVNodes.length
    while lo < hi do
      val mid = (lo + hi) >>> 1
      if sortedVNodes(mid).pos < keyPos then lo = mid + 1 else hi = mid
    if lo == sortedVNodes.length then sortedVNodes(0).physicalIdx
    else sortedVNodes(lo).physicalIdx

  private def keyPositions(keyCount: Int): Array[Double] =
    val arr = Array.ofDim[Double](keyCount)
    var j   = 0
    while j < keyCount do
      arr(j) = positionOf(s"key-$j")
      j += 1
    arr

  private def ownershipArray(nodeCount: Int, virtualNodes: Int, keyCount: Int): Array[Int] =
    val v   = buildVNodes(nodeCount, virtualNodes)
    val out = Array.ofDim[Int](keyCount)
    val pos = keyPositions(keyCount)
    var j   = 0
    while j < keyCount do
      out(j) = ownerOfKey(pos(j), v)
      j += 1
    out

  private def keysPerNode(ownership: Array[Int], nodeCount: Int): Array[Int] =
    val out = Array.ofDim[Int](nodeCount)
    var j   = 0
    while j < ownership.length do
      val o = ownership(j)
      if o >= 0 && o < nodeCount then out(o) += 1
      j += 1
    out

  /** Plain modulo ownership: key j maps to (hash(key_j) % N). */
  private def moduloOwnership(nodeCount: Int, keyCount: Int): Array[Int] =
    val out = Array.ofDim[Int](keyCount)
    var j   = 0
    while j < keyCount do
      val unsigned = hash32(s"key-$j").toLong & 0xffffffffL
      out(j) = (unsigned % nodeCount).toInt
      j += 1
    out

  /** Number of keys that change owner when going from N to N+1 nodes under a given ownership function. */
  private def diffCount(a: Array[Int], b: Array[Int]): Int =
    var changed = 0
    var j       = 0
    while j < a.length do
      if a(j) != b(j) then changed += 1
      j += 1
    changed

  // ===========================================================================
  // Colours — 8 distinct hues for up to 8 physical nodes
  // ===========================================================================

  private val NodeColors: Array[String] = Array(
    "#3b82f6", // blue-500
    "#10b981", // emerald-500
    "#f59e0b", // amber-500
    "#8b5cf6", // violet-500
    "#f43f5e", // rose-500
    "#14b8a6", // teal-500
    "#ec4899", // pink-500
    "#84cc16"  // lime-500
  )

  private def nodeColor(idx: Int): String = NodeColors(idx % NodeColors.length)

  // ===========================================================================
  // SVG layout
  // ===========================================================================

  private val ViewBoxWidth  = 720.0
  private val ViewBoxHeight = 420.0
  private val RingCx        = 360.0
  private val RingCy        = 210.0
  private val RingR         = 150.0
  private val PhysNodeR     = 12.0
  private val VNodeR        = 3.5
  private val KeyR          = 4.0
  private val VNodeRingR    = RingR
  private val PhysRingR     = RingR
  private val KeyRingR      = RingR + 26.0

  /** Map ring angle (0 at top, increasing clockwise) to SVG coordinates. */
  private def polarToXy(angle: Double, r: Double): (Double, Double) =
    val a = angle - math.Pi / 2 // -π/2 puts angle 0 at the top of the ring
    (RingCx + r * math.cos(a), RingCy + r * math.sin(a))

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def primaryNodePosition(i: Int): Double = positionOf(s"node-$i-v0")

  private def buildSvg(nodeCount: Int, virtualNodes: Int, keyCount: Int): String =
    val vnodes    = buildVNodes(nodeCount, virtualNodes)
    val ownership = ownershipArray(nodeCount, virtualNodes, keyCount)
    val keyPos    = keyPositions(keyCount)

    val ringCircle =
      s"""<circle class="consistent-hash-ring__ring" cx="$RingCx" cy="$RingCy" r="$RingR"/>"""

    val vnodeMarkers = vnodes
      .map { vn =>
        val (x, y) = polarToXy(vn.pos, VNodeRingR)
        // primary vnode (vIdx == 0) gets the physical-node marker treatment below; skip drawing it as a small vnode.
        if vn.vIdx == 0 then ""
        else
          s"""<circle class="consistent-hash-ring__vnode" cx="$x" cy="$y" r="$VNodeR" fill="${nodeColor(
              vn.physicalIdx
            )}"/>"""
      }
      .mkString("\n")

    val physMarkers = (0 until nodeCount)
      .map { i =>
        val (x, y) = polarToXy(primaryNodePosition(i), PhysRingR)
        val color  = nodeColor(i)
        s"""<g class="consistent-hash-ring__phys">
           |  <circle class="consistent-hash-ring__phys-disk" cx="$x" cy="$y" r="$PhysNodeR" fill="$color"/>
           |  <text class="consistent-hash-ring__phys-label" x="$x" y="${y + 4}" text-anchor="middle">${i + 1}</text>
           |</g>""".stripMargin
      }
      .mkString("\n")

    val keyMarkers = (0 until keyCount)
      .map { j =>
        val (x, y) = polarToXy(keyPos(j), KeyRingR)
        val color  = nodeColor(ownership(j))
        s"""<circle class="consistent-hash-ring__key" cx="$x" cy="$y" r="$KeyR" fill="$color"/>"""
      }
      .mkString("\n")

    val centerLabel =
      s"""<text class="consistent-hash-ring__center" x="$RingCx" y="${RingCy - 8}" text-anchor="middle">${esc(
          "Hash ring"
        )}</text>
         |<text class="consistent-hash-ring__center-sub" x="$RingCx" y="${RingCy + 14}" text-anchor="middle">${nodeCount} node${
          if nodeCount == 1 then ""
          else "s"
        } × $virtualNodes vnode${if virtualNodes == 1 then "" else "s"}</text>""".stripMargin

    s"""<svg viewBox="0 0 $ViewBoxWidth $ViewBoxHeight"
       |     class="consistent-hash-ring__svg" role="img"
       |     xmlns="http://www.w3.org/2000/svg">
       |  $ringCircle
       |  $vnodeMarkers
       |  $physMarkers
       |  $keyMarkers
       |  $centerLabel
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useStateBy(_ => 4)
      .useStateBy(_ => 1)
      .useEffectOnMountBy { (_, specM, nodeS, vnodeS) =>
        specM.value match
          case Right(spec) =>
            nodeS.setState(spec.nodeCount) >> vnodeS.setState(spec.virtualNodes)
          case _ => Callback.empty
      }
      .render { (_, specM, nodeS, vnodeS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Consistent-hash-ring widget payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val n         = nodeS.value
            val v         = vnodeS.value
            val ownership = ownershipArray(n, v, spec.keyCount)
            val perNode   = keysPerNode(ownership, n)
            val total     = spec.keyCount.toDouble

            // Cost-of-adding-one-more-node, both methods.
            val nextN = n + 1
            val chRemap =
              diffCount(ownership, ownershipArray(nextN, v, spec.keyCount))
            val modRemap =
              diffCount(moduloOwnership(n, spec.keyCount), moduloOwnership(nextN, spec.keyCount))

            <.div(
              ^.className := "consistent-hash-ring not-prose",
              spec.title
                .map(t => <.p(^.className := "consistent-hash-ring__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "consistent-hash-ring__frame",
                ^.dangerouslySetInnerHtml := buildSvg(n, v, spec.keyCount)
              ),
              <.div(
                ^.className := "consistent-hash-ring__controls",
                sliderRow(
                  s"Physical nodes: $n",
                  spec.nodeMin,
                  spec.nodeMax,
                  n,
                  (e: ReactEventFromInput) => {
                    val newVal = Try(e.target.value.toInt).getOrElse(spec.nodeCount)
                    nodeS.setState(math.max(spec.nodeMin, math.min(spec.nodeMax, newVal)))
                  }
                ),
                sliderRow(
                  s"Virtual nodes per physical: $v",
                  spec.virtualNodesMin,
                  spec.virtualNodesMax,
                  v,
                  (e: ReactEventFromInput) => {
                    val newVal = Try(e.target.value.toInt).getOrElse(spec.virtualNodes)
                    vnodeS.setState(
                      math.max(spec.virtualNodesMin, math.min(spec.virtualNodesMax, newVal))
                    )
                  }
                )
              ),
              <.div(
                ^.className := "consistent-hash-ring__loads",
                <.p(^.className := "consistent-hash-ring__loads-label", "Keys per node (load distribution)"),
                <.div(
                  ^.className := "consistent-hash-ring__loads-grid",
                  (0 until n).map { i =>
                    val count = perNode(i)
                    val pct   = if total > 0 then math.round(count / total * 100).toInt else 0
                    <.div(
                      ^.className := "consistent-hash-ring__load-item",
                      ^.key       := i.toString,
                      <.span(
                        ^.className := "consistent-hash-ring__swatch",
                        ^.style := js.Dictionary("backgroundColor" -> nodeColor(i)).asInstanceOf[js.Object]
                      ),
                      <.span(
                        ^.className := "consistent-hash-ring__load-text",
                        s"Node ${i + 1}: $count ($pct%)"
                      )
                    ): VdomNode
                  }.toVdomArray
                )
              ),
              <.div(
                ^.className := "consistent-hash-ring__readout",
                readoutRow(
                  s"Adding 1 more node ($nextN total)",
                  s"$chRemap of ${spec.keyCount} keys remap",
                  "Consistent hashing — only keys near the new vnode positions move"
                ),
                readoutRow(
                  "Same swap, plain modulo hashing",
                  s"$modRemap of ${spec.keyCount} keys remap",
                  "Modulo — almost everything moves because `j % (N+1) ≠ j % N` for most j"
                ),
                readoutRow(
                  "Gap",
                  if chRemap == 0 then "—" else f"${modRemap.toDouble / chRemap}%.1f×",
                  "fewer keys remapped under consistent hashing — that's the win"
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
      ^.className := "consistent-hash-ring__slider-row",
      <.span(^.className := "consistent-hash-ring__slider-text", label),
      <.input(
        ^.tpe       := "range",
        ^.className := "consistent-hash-ring__slider",
        ^.min       := min.toString,
        ^.max       := max.toString,
        ^.step      := "1",
        ^.value     := value.toString,
        ^.onChange ==> onInput
      )
    )

  private def readoutRow(label: String, value: String, note: String): VdomNode =
    <.div(
      ^.className := "consistent-hash-ring__readout-row",
      <.span(^.className := "consistent-hash-ring__readout-label", label),
      <.span(^.className := "consistent-hash-ring__readout-value", value),
      <.span(^.className := "consistent-hash-ring__readout-note", note)
    )
