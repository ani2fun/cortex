package codefolio.shared.cortex

import codefolio.shared.api.Endpoints.ChapterFrontmatter

/**
 * Lenient YAML-frontmatter parser for Cortex Chapters.
 *
 * Lives in `shared` so the index walker and the chapter-payload assembler share the same implementation.
 * JVM-tested directly without filesystem fixtures.
 *
 * Lenient by ADR-0001: an unterminated `---` fence is treated as plain body with no frontmatter parsed —
 * matching the resilience policy that a typo in one chapter shouldn't break the whole book.
 */
object Frontmatter:

  final case class Parsed(frontmatter: ChapterFrontmatter, body: String)

  /**
   * Full parse: extract frontmatter map + body. Title falls back to `fallbackTitle` when `title:` is absent
   * (or the frontmatter fence is malformed). Body is returned verbatim when no closing fence is found.
   */
  def parse(content: String, fallbackTitle: String): Parsed =
    val lines = content.split("\\R", -1).toIndexedSeq
    if lines.headOption.contains("---") then
      val end = lines.indexOf("---", 1)
      if end > 0 then
        val fields = lines.slice(1, end).flatMap(parseLine).toMap
        val body   = lines.drop(end + 1).mkString("\n")
        Parsed(
          frontmatter = ChapterFrontmatter(
            title = fields.getOrElse("title", fallbackTitle),
            summary = fields.get("summary"),
            essential = fields.get("essential").flatMap(toBooleanOpt)
          ),
          body = body
        )
      else Parsed(emptyFrontmatter(fallbackTitle), content)
    else Parsed(emptyFrontmatter(fallbackTitle), content)

  /**
   * Per-chapter override of the essential/optional status indicator, parsed without splitting the rest of the
   * frontmatter. Used by the index walker so a per-chapter override can win against the section-level
   * cascade. Returns `None` when no `---` fence is present, the fence is unterminated, the key is absent, or
   * the value is anything other than `true|false` (ADR-0001: malformed values fall through to inheritance,
   * never to an error).
   */
  def extractEssential(content: String): Option[Boolean] =
    parseLines(content).get("essential").flatMap(toBooleanOpt)

  /**
   * Title-only parse used during indexing. Falls back through: frontmatter `title:` → first `# ` heading in
   * the body → `fallback`.
   */
  def extractTitle(content: String, fallback: String): String =
    parseLines(content)
      .get("title")
      .orElse(firstH1(stripFrontmatter(content)))
      .getOrElse(fallback)

  // ===========================================================================
  // Internals
  // ===========================================================================

  private def stripFrontmatter(content: String): String =
    val lines = content.split("\\R", -1).toIndexedSeq
    if lines.headOption.contains("---") then
      val end = lines.indexOf("---", 1)
      if end > 0 then lines.drop(end + 1).mkString("\n") else content
    else content

  private def parseLines(content: String): Map[String, String] =
    val lines = content.split("\\R", -1).toIndexedSeq
    if !lines.headOption.contains("---") then Map.empty
    else
      val end = lines.indexOf("---", 1)
      if end <= 0 then Map.empty
      else lines.slice(1, end).flatMap(parseLine).toMap

  private def firstH1(body: String): Option[String] =
    body.linesIterator
      .collectFirst { case l if l.startsWith("# ") => l.stripPrefix("# ").trim }
      .filter(_.nonEmpty)

  private def parseLine(line: String): Option[(String, String)] =
    val idx = line.indexOf(':')
    if idx <= 0 then None
    else
      val key      = line.substring(0, idx).trim
      val rawValue = line.substring(idx + 1).trim
      val value    = stripQuotes(rawValue)
      if value.nonEmpty then Some(key -> value) else None

  private def stripQuotes(s: String): String =
    if (s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")) then
      if s.length >= 2 then s.substring(1, s.length - 1) else s
    else s

  private def toBooleanOpt(s: String): Option[Boolean] = s.toLowerCase match
    case "true"  => Some(true)
    case "false" => Some(false)
    case _       => None

  private def emptyFrontmatter(fallbackTitle: String): ChapterFrontmatter =
    ChapterFrontmatter(title = fallbackTitle, summary = None, essential = None)
