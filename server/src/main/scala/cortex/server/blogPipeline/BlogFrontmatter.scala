package cortex.server.blogPipeline

import cortex.shared.api.Endpoints.{BlogMetaItem, BlogPostFrontmatter}

/**
 * Lenient YAML-frontmatter parser for blog posts.
 *
 * Why a separate parser from Cortex's [[cortex.shared.book.Frontmatter]]: blogs need richer fields
 * (`publishedAt`, `tags`, `readMinutes`, `eyebrow`, `meta`) that the simple line-based key:value parser used
 * by Cortex doesn't cleanly support. Sibling rather than extension keeps each parser narrow.
 *
 * Format conventions:
 *   - Frontmatter is delimited by `---` fences at the top of the file.
 *   - One `key: value` pair per line; whitespace around `:` is tolerated.
 *   - String values may be optionally quoted with `"..."` or `'...'`.
 *   - `tags` accepts inline-list syntax `[a, b, c]` — split on `,` and trim, brackets stripped.
 *   - `meta` is a semicolon-separated list of `label=value` pairs: `meta: Read Time=15 min; Active Prep=10
 *     minutes; Difficulty=Beginner` A pipe (`|`) is also accepted as the label/value delimiter for posts that
 *     prefer it.
 *   - `readMinutes` parses as `Int`; non-numeric → `None`.
 *
 * Lenient per ADR-0001: a malformed or unterminated frontmatter fence yields a frontmatter with `title =
 * humanise(slug)`, `publishedAt = ""`, and the entire file as body. The date parser in `BlogPipeline` then
 * sinks the post to the bottom of the index rather than failing the whole tree.
 */
object BlogFrontmatter:

  final case class Parsed(frontmatter: BlogPostFrontmatter, body: String)

  /** Full parse: extract frontmatter map + body. Falls back to `humanise(fallbackSlug)` for the title. */
  def parse(content: String, fallbackSlug: String): Parsed =
    val lines = content.split("\\R", -1).toIndexedSeq
    if lines.headOption.contains("---") then
      val end = lines.indexOf("---", 1)
      if end > 0 then
        val fields = lines.slice(1, end).flatMap(parseLine).toMap
        val body   = lines.drop(end + 1).mkString("\n")
        Parsed(toFrontmatter(fields, fallbackSlug), body)
      else Parsed(empty(fallbackSlug), content)
    else Parsed(empty(fallbackSlug), content)

  /** Strip the leading `---` fence + frontmatter and return only the body. Used by `post()` after re-read. */
  def stripFrontmatter(content: String): String =
    val lines = content.split("\\R", -1).toIndexedSeq
    if lines.headOption.contains("---") then
      val end = lines.indexOf("---", 1)
      if end > 0 then lines.drop(end + 1).mkString("\n") else content
    else content

  /** Humanise a slug into a display title: `l-reuteri-yogurt` → `L Reuteri Yogurt`. */
  def humanise(slug: String): String =
    slug
      .split("[-_]")
      .iterator
      .filter(_.nonEmpty)
      .map(w => w.headOption.fold("")(_.toUpper.toString) + w.drop(1))
      .mkString(" ")

  // ===========================================================================
  // Internals
  // ===========================================================================

  private def empty(fallbackSlug: String): BlogPostFrontmatter =
    BlogPostFrontmatter(
      title = humanise(fallbackSlug),
      summary = None,
      publishedAt = "",
      tags = None,
      readMinutes = None,
      eyebrow = None,
      meta = None
    )

  private def toFrontmatter(fields: Map[String, String], fallbackSlug: String): BlogPostFrontmatter =
    BlogPostFrontmatter(
      title = fields.getOrElse("title", humanise(fallbackSlug)),
      summary = fields.get("summary"),
      publishedAt = fields.getOrElse("publishedAt", ""),
      tags = fields.get("tags").map(parseInlineList),
      readMinutes = fields.get("readMinutes").flatMap(s => scala.util.Try(s.toInt).toOption),
      eyebrow = fields.get("eyebrow"),
      meta = fields.get("meta").map(parseMeta).filter(_.nonEmpty)
    )

  private def parseInlineList(raw: String): Seq[String] =
    raw
      .stripPrefix("[")
      .stripSuffix("]")
      .split(",")
      .iterator
      .map(stripQuotes)
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList

  private def parseMeta(raw: String): Seq[BlogMetaItem] =
    raw
      .split(";")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap { item =>
        val sep = item.indexWhere(c => c == '=' || c == '|')
        if sep <= 0 then None
        else
          val label = item.substring(0, sep).trim
          val value = item.substring(sep + 1).trim
          if label.nonEmpty && value.nonEmpty then Some(BlogMetaItem(label = label, value = value))
          else None
      }
      .toList

  private def parseLine(line: String): Option[(String, String)] =
    val idx = line.indexOf(':')
    if idx <= 0 then None
    else
      val key      = line.substring(0, idx).trim
      val rawValue = line.substring(idx + 1).trim
      val value    = stripQuotes(rawValue)
      if value.nonEmpty then Some(key -> value) else None

  private def stripQuotes(s0: String): String =
    val s = s0.trim
    if (s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")) then
      if s.length >= 2 then s.substring(1, s.length - 1) else s
    else s
