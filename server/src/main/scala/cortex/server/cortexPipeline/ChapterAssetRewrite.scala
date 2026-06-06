package cortex.server.cortexPipeline

import java.util.regex.Matcher

/**
 * Rewrite relative asset URLs in chapter markdown to absolute `/api/cortex/asset/...` URLs.
 *
 * Chapter authors write natural relative paths (`./diagrams/foo.svg`, `../shared/bar.png`) and the pipeline
 * rewrites them to `/api/cortex/asset/<book>/<resolved-path>` before sending the chapter payload to the
 * client. This keeps lesson markdown portable — the same `./diagrams/foo.svg` reference works on GitHub's
 * file viewer and inside Cortex without authors needing to know URL conventions.
 *
 * What gets rewritten:
 *   - HTML `<img src="...">` and `<a href="...">` (and similar attrs on `<source>`, `<video>`, etc.)
 *   - Markdown `[text](url)` and `![alt](url)`
 *
 * What gets left alone:
 *   - Already-absolute URLs: `/...`, `http(s)://...`, `mailto:`, `data:`, `tel:`, `javascript:`
 *   - In-page anchors: `#section`
 *   - Chapter cross-links (`.md` suffix or `.md#anchor` / `.md?query`) — those are a routing concern handled
 *     separately (or currently broken; see TODO)
 *
 * Path normalisation: `.` and `..` segments are collapsed against the chapter's directory. A
 * `../shared/bar.png` from a chapter at `01-foundations/04-cap-and-pacelc.md` resolves to `shared/bar.png`,
 * not `01-foundations/../shared/bar.png`.
 */
object ChapterAssetRewrite:

  /** Default URL prefix where assets get mounted. Matches `CortexAssetRoutes.from`. */
  val DefaultAssetPrefix: String = "/api/cortex/asset"

  /**
   * Rewrite all relative asset URLs in `raw` markdown.
   *
   * @param raw
   *   chapter markdown body (frontmatter already stripped)
   * @param bookSlug
   *   book slug, e.g. `"system-design"`
   * @param chapterPathInBook
   *   chapter's path within the book, e.g. `"01-foundations/04-cap-and-pacelc.md"`
   * @param assetPrefix
   *   URL prefix to mount assets under
   */
  def rewrite(
      raw: String,
      bookSlug: String,
      chapterPathInBook: String,
      assetPrefix: String = DefaultAssetPrefix
  ): String =
    val chapterDir =
      val idx = chapterPathInBook.lastIndexOf('/')
      if idx < 0 then "" else chapterPathInBook.substring(0, idx)

    def toAbsolute(rel: String): String =
      val joined =
        if chapterDir.isEmpty then rel
        else s"$chapterDir/$rel"
      val normalized = normalize(joined.split('/').toIndexedSeq).mkString("/")
      s"$assetPrefix/$bookSlug/$normalized"

    // Relative asset URLs become absolute `/api/cortex/asset/...`; absolute in-content links that still
    // carry the legacy `/cortex` prefix are stripped to match the prefix-less routing on cortex.kakde.eu.
    // Everything else (other absolute URLs, in-page anchors, `.md` cross-links) passes through unchanged.
    def transformUrl(url: String): String =
      if isRewritable(url) then toAbsolute(url)
      else stripLegacyCortexPrefix(url)

    // HTML `src="..."` and `href="..."` (double-quoted; single-quoted is exceedingly rare
    // in markdown bodies and would be picked up by a future pass if it becomes needed).
    val htmlAttrPattern = """((?:src|href)\s*=\s*)"([^"]+)"""".r
    // Markdown `[text](url)` and `![alt](url)`, optionally followed by a `"title"`.
    val mdLinkPattern = """(!?)\[([^\]]*)\]\(([^)\s]+)((?:\s+"[^"]*")?)\)""".r

    val afterHtml = htmlAttrPattern.replaceAllIn(
      raw,
      m =>
        val url = m.group(2)
        val out = transformUrl(url)
        if out != url then Matcher.quoteReplacement(s"""${m.group(1)}"$out"""")
        else Matcher.quoteReplacement(m.matched)
    )

    mdLinkPattern.replaceAllIn(
      afterHtml,
      m =>
        val url = m.group(3)
        val out = transformUrl(url)
        if out != url then
          Matcher.quoteReplacement(s"""${m.group(1)}[${m.group(2)}]($out${m.group(4)})""")
        else Matcher.quoteReplacement(m.matched)
    )

  /**
   * Strip the legacy `/cortex` URL prefix from an absolute in-content link so it matches the prefix-less
   * routing on cortex.kakde.eu (`/cortex/<book>/<slug>` → `/<book>/<slug>`, and `/cortex` → `/`). All other
   * URLs pass through unchanged — notably the asset-rewrite output `/api/cortex/asset/...`, which does not
   * start with `/cortex/`.
   */
  private[cortexPipeline] def stripLegacyCortexPrefix(url: String): String =
    if url == "/cortex" then "/"
    else if url.startsWith("/cortex/") then url.stripPrefix("/cortex")
    else url

  /** True iff `url` is a relative asset URL we want to rewrite. */
  private def isRewritable(url: String): Boolean =
    url.nonEmpty &&
      !url.startsWith("/") &&
      !url.startsWith("#") &&
      !url.contains("://") &&
      !url.startsWith("mailto:") &&
      !url.startsWith("data:") &&
      !url.startsWith("tel:") &&
      !url.startsWith("javascript:") &&
      !endsWithMd(url)

  /** Match `foo.md`, `foo.md#anchor`, `foo.md?query` — these are chapter cross-links. */
  private def endsWithMd(url: String): Boolean =
    val cut = url.indexOf('#') match
      case -1 => url.indexOf('?') match
          case -1 => url
          case i  => url.substring(0, i)
      case i => url.substring(0, i)
    cut.endsWith(".md")

  /**
   * Collapse `.` and `..` segments. Empty segments (from leading or doubled slashes) are dropped. A `..` at
   * the start (or after only `..`s) is preserved — it means "escape the book root", which is a no-op as far
   * as Cortex's chapter-scoped resolution goes, but we keep the literal form so the result is honest about
   * what was asked.
   */
  private[cortexPipeline] def normalize(segs: IndexedSeq[String]): IndexedSeq[String] =
    segs.foldLeft(Vector.empty[String]):
      case (acc, "." | "") => acc
      case (acc, "..") =>
        if acc.nonEmpty && acc.last != ".." then acc.init
        else acc :+ ".."
      case (acc, seg) => acc :+ seg
