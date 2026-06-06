package cortex.client.util

import org.scalajs.dom

import scala.util.Try

/**
 * localStorage-backed persistence for two pieces of per-chapter UX state surfaced in the Cortex sidebar:
 *
 *   - **progress** — 0..100 percent, written on scroll while reading, replayed as a 3px indigo rail next to
 *     each chapter row in the sidebar.
 *   - **minutes** — words-per-minute estimate computed once from a chapter's raw markdown body, replayed as a
 *     small mono-stamp ("12m") at the right of each chapter row.
 *
 * Stored under keys `cortex:progress:<slug>` and `cortex:minutes:<slug>` respectively; reads tolerate
 * missing/garbage values and return the defaults so a corrupt entry never breaks the sidebar render.
 */
object ReaderState:

  // Average reading speed for technical prose. ~225 wpm is what most "time to read" widgets cite;
  // 220 makes the math friendlier (660 words / 220 = 3 min) without changing the rounded result.
  private val WordsPerMinute = 220

  private def storage: Option[dom.Storage] =
    Try(Option(dom.window.localStorage)).toOption.flatten

  private def progressKey(slug: String): String = s"cortex:progress:$slug"
  private def minutesKey(slug: String): String  = s"cortex:minutes:$slug"

  private def setItem(key: String, value: String): Unit =
    storage.foreach(s => Try(s.setItem(key, value)))

  private def getItem(key: String): Option[String] =
    storage.flatMap(s => Try(Option(s.getItem(key))).toOption.flatten)

  /** 0..100 — clamped, integer. Missing or non-numeric → 0. */
  def progressFor(slug: String): Int =
    getItem(progressKey(slug)).flatMap(s => Try(s.toInt).toOption).map(clampPercent).getOrElse(0)

  /** Persist 0..100 progress. Values outside the range are clamped. */
  def saveProgress(slug: String, percent: Int): Unit =
    setItem(progressKey(slug), clampPercent(percent).toString)

  /** Cached minutes for a chapter, or `None` if we haven't measured it yet. */
  def minutesFor(slug: String): Option[Int] =
    getItem(minutesKey(slug)).flatMap(s => Try(s.toInt).toOption).filter(_ > 0)

  /** Compute minutes from a raw markdown body, cache, and return. Always ≥ 1 so the UI never says "0m". */
  def saveMinutesFromRaw(slug: String, raw: String): Int =
    val minutes = math.max(1, math.round(countWords(raw).toDouble / WordsPerMinute).toInt)
    setItem(minutesKey(slug), minutes.toString)
    minutes

  /** Word count via whitespace split. Markdown punctuation is good enough at this resolution. */
  private def countWords(raw: String): Int =
    if raw == null || raw.isEmpty then 0
    else raw.trim.split("\\s+").count(_.nonEmpty)

  private def clampPercent(p: Int): Int = math.max(0, math.min(100, p))

  /**
   * Ratio (0.0..1.0) of the document that's been scrolled past. Anchored on document height so it works for
   * the whole page rather than an inner scroll frame — chapter pages scroll the document, not a container.
   * Returns 1.0 when the page is shorter than the viewport (nothing to scroll = "fully read").
   */
  def currentScrollFraction(): Double =
    val docEl   = dom.document.documentElement
    val scrollY = dom.window.scrollY
    val client  = docEl.clientHeight.toDouble
    val total   = docEl.scrollHeight.toDouble
    val span    = total - client
    if span <= 0 then 1.0
    else math.max(0.0, math.min(1.0, scrollY / span))

  /** Save the current scroll position as a progress percent for `slug`. */
  def saveScrollProgress(slug: String): Unit =
    saveProgress(slug, math.round(currentScrollFraction() * 100).toInt)

  /** Slug → (progress%, minutesOpt). One pass so the sidebar can render N rows without N round-trips. */
  def snapshotFor(slugs: Iterable[String]): Map[String, (Int, Option[Int])] =
    slugs.iterator.map(s => s -> (progressFor(s), minutesFor(s))).toMap
