package codefolio.shared.viz

/**
 * Role-based colour canon for cursor pointers (ADR-0016 / ADR-0018).
 *
 * A pointer's colour is decided by its *role*, not the author: `head` / `root` / `i` are deep blue (entry
 * point), `current` / `mid` indigo (the active cursor), `fast` / `right` bordeaux (the contentious second),
 * `previous` / `j` mulberry, `next` / `successor` moss-green, `tail` / `parent` bordeaux — so a reader who
 * has seen one chapter recognises a pointer's colour everywhere.
 *
 * Palette is brand-aligned with the codefolio editorial identity (indigo-on-cream); ported verbatim from the
 * Claude Design handoff's `window.POINTER_COLORS` in `ui_kits/visualise/steps.js`. Same shape as the previous
 * Tailwind-default palette; only the hexes change.
 *
 * The trace-driven Visualise reads *real* Python variable names which it cannot rename, so this canon adds an
 * `aliases` layer mapping common real-code names (`cur`, `nxt`, `prev`, `lo`, …) onto canonical roles, and a
 * `fallback` palette of distinct hues for names with no role at all. Was previously paired with a strict
 * `MarkerCanon` table for hand-authored widgets that hard-rejected unknown names; that table was retired
 * along with its callers (ArrayTraversal / BinaryTree / LinkedList / GraphExplorer / StackQueue / HeapTree)
 * in slices 11b–15, leaving this module as the single source of truth.
 *
 * Pure data — cross-compiles to the JVM (HeapToGraph tests) and Scala.js (the adapter at runtime).
 */
object MarkerColors:

  /** Canonical pointer name → role colour. Mirrors `window.POINTER_COLORS` in the design handoff. */
  val canon: Map[String, String] = Map(
    // ── Deep blue #3a5a8c — entry point / opens a range / primary loop ──
    "head"  -> "#3a5a8c",
    "root"  -> "#3a5a8c",
    "i"     -> "#3a5a8c",
    "left"  -> "#3a5a8c",
    "low"   -> "#3a5a8c",
    "slow"  -> "#3a5a8c",
    "front" -> "#3a5a8c",
    "read"  -> "#3a5a8c",
    "base"  -> "#3a5a8c",
    // ── Indigo #4f5bd5 — active position / cursor of activity (the brand accent) ──
    "current" -> "#4f5bd5",
    "mid"     -> "#4f5bd5",
    "top"     -> "#4f5bd5",
    "write"   -> "#4f5bd5",
    "found"   -> "#4f5bd5",
    "ptr"     -> "#4f5bd5",
    "end"     -> "#4f5bd5",
    // ── Mulberry #8a4f7d — trailing pointer / inner loop ──
    "j"        -> "#8a4f7d",
    "previous" -> "#8a4f7d",
    "p"        -> "#8a4f7d",
    "start"    -> "#8a4f7d",
    // ── Moss green #5a8a5a — saved-aside / one step forward ──
    "next"        -> "#5a8a5a",
    "successor"   -> "#5a8a5a",
    "predecessor" -> "#5a8a5a",
    "kth"         -> "#5a8a5a",
    // ── Bordeaux #a13e3e — explicit end / contentious second slot ──
    "tail"   -> "#a13e3e",
    "dummy"  -> "#a13e3e",
    "parent" -> "#a13e3e",
    "last"   -> "#a13e3e",
    "fast"   -> "#a13e3e",
    "right"  -> "#a13e3e",
    "high"   -> "#a13e3e",
    "swap"   -> "#a13e3e",
    "back"   -> "#a13e3e",
    // ── Mulberry #8a4f7d — LCA query 2 (paired with `p` for distinguishability) ──
    "q" -> "#8a4f7d"
  )

  /**
   * Step-event marker colours — mirrors `window.MARKER_COLORS` in the design handoff.
   *
   * These flag *what happened to a value* this step (changed / removed / returned), not what role a pointer
   * plays. Read off by the canvas annotation system (call / return / exception eyebrows), the
   * `frame__local--changed|--removed|--return` modifiers, and the `MARKER_COLORS.ref` arrow from a frame's
   * REF row to the heap card it points at.
   */
  val markers: Map[String, String] = Map(
    "changed"         -> "#6a9656", // moss green   — new / modified this step
    "removed"         -> "#a13e3e", // bordeaux     — out of scope this step
    "returned"        -> "#4f5bd5", // indigo   — frame's return value
    "returnedDarker"  -> "#4a6a3c", // dark moss    — canvas annotation eyebrow on light theme
    "returnedLighter" -> "#9bbf86", // light moss   — canvas annotation eyebrow on dark theme
    "ref"             -> "#3a5a8c", // deep blue    — REF arrow from frame row to heap card
    "exception"       -> "#a13e3e", // bordeaux     — exception eyebrow on canvas annotation
    "visited"         -> "#6a9656", // moss green   — graph: visited node
    "frontier"        -> "#4f5bd5"  // indigo   — graph: frontier node
  )

  /** Real-code variable name → a canonical name. The trace aliases liberally; the bespoke widgets reject. */
  val aliases: Map[String, String] = Map(
    "cur"    -> "current",
    "curr"   -> "current",
    "node"   -> "current",
    "cnode"  -> "current",
    "runner" -> "current",
    "walk"   -> "current",
    "prev"   -> "previous",
    "pre"    -> "previous",
    "nxt"    -> "next",
    "tmp"    -> "next",
    "temp"   -> "next",
    "lo"     -> "low",
    "hi"     -> "high",
    "l"      -> "left",
    "r"      -> "right",
    "succ"   -> "successor",
    "pred"   -> "predecessor",
    "tree"   -> "root",
    "h"      -> "head",
    "first"  -> "head"
  )

  /** Distinct hues for names with no canonical role — assigned by order of first appearance. */
  val fallback: Vector[String] =
    Vector("#3a5a8c", "#4f5bd5", "#a13e3e", "#8a4f7d", "#5a8a5a", "#c5a572", "#91b5c2")

  /** The role colour for `name`, if it has one — a direct canon hit, then an alias → canon hit. */
  def roleColor(name: String): Option[String] =
    canon.get(name).orElse(aliases.get(name).flatMap(canon.get))

  /**
   * Assign a colour to every name in `names` (given in first-appearance order). Canon / alias names get their
   * role colour; the rest draw from the `fallback` palette, counting only the unknowns so each gets a
   * distinct hue. Stable for the whole trace — a pointer keeps one colour across every step.
   */
  def assignColors(names: List[String]): Map[String, String] =
    var unknown = 0
    names.distinct.map { name =>
      val color = roleColor(name).getOrElse {
        val c = fallback(math.floorMod(unknown, fallback.size))
        unknown += 1
        c
      }
      name -> color
    }.toMap
