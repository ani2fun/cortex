package cortex.shared.book

import cortex.shared.api.Endpoints.{Book, ChapterRef, CortexIndex}

/**
 * Pure assembler for the Cortex tree.
 *
 * Takes an in-memory `CortexEntry` tree (the FS adapter materialises this from disk) and produces a
 * `CortexIndex` plus a per-book reverse map (chapter slug → relative file path). The seam keeps the
 * convention rules — numeric-prefix ordering, max Section depth, Slug uniqueness, Frontmatter title fallback
 * chain — in one named module that JVM tests can exercise with literal fixtures.
 *
 * Lenient by ADR-0001: `BookMeta` and `SectionMeta` are `Option` — the FS adapter passes `None` when the
 * matching `book.json` / `_section.json` is missing or malformed, and the walker falls back to a humanised
 * directory name. The same leniency applies to Chapter `Frontmatter` via [[Frontmatter.extractTitle]].
 *
 * Mirrors the pattern in [[SidebarForest]] and [[cortex.shared.runner.CodeExecutor]]: a pure shared module
 * behind an internal seam, callable from any platform that lands in `shared`.
 */
object CortexIndexWalker:

  /** Maximum nesting of Sections inside a Book. Exceeding this surfaces as `IndexError.MaxDepthExceeded`. */
  val MaxSectionDepth: Int = 6

  // ===========================================================================
  // Pre-decoded metadata. The FS adapter's circe layer lands here; the walker
  // never sees raw bytes or JSON.
  // ===========================================================================

  /**
   * `order` controls the book's position on the Cortex index: books with an `order` sort ascending and ahead
   * of any book without one, which fall to the end sorted alphabetically by directory name.
   */
  final case class BookMeta(
      title: Option[String],
      description: Option[String],
      tags: Option[Seq[String]],
      estimatedReadingMinutes: Option[Int],
      order: Option[Int] = None
  )

  /**
   * `defaultStatus` cascades to every chapter beneath this section unless a chapter's own frontmatter
   * `essential:` overrides it. Recognised values are `"essential"` and `"optional"`; anything else is ignored
   * and inheritance from the enclosing section / book root continues unchanged.
   */
  final case class SectionMeta(
      title: Option[String],
      summary: Option[String],
      defaultStatus: Option[String] = None
  )

  /** Book-level default for chapters without any explicit essential/optional cascade. */
  val DefaultEssential: Boolean = true

  /** Parse a `defaultStatus` value into a Boolean; `None` for unrecognised strings (no inheritance flip). */
  private def parseDefaultStatus(s: String): Option[Boolean] = s.toLowerCase match
    case "essential" => Some(true)
    case "optional"  => Some(false)
    case _           => None

  // ===========================================================================
  // Input ADT — the in-memory tree the walker operates on.
  // ===========================================================================

  sealed trait CortexEntry { def name: String }

  /** A chapter source file. `name` includes the `.md` suffix; `content` is the raw markdown. */
  final case class CortexFile(name: String, content: String) extends CortexEntry

  /**
   * A directory. `bookMeta` is populated only at the top-level (Book root); `sectionMeta` only at nested
   * dirs. Children are listed in their FS-discovery order — the walker re-sorts by numeric prefix.
   */
  final case class CortexDir(
      name: String,
      bookMeta: Option[BookMeta] = None,
      sectionMeta: Option[SectionMeta] = None,
      children: List[CortexEntry] = Nil
  ) extends CortexEntry

  // ===========================================================================
  // Output.
  // ===========================================================================

  /** `reverseMaps`: book slug → (chapter slug → relative file path within the book). */
  final case class WalkResult(
      index: CortexIndex,
      reverseMaps: Map[String, Map[String, String]]
  )

  // ===========================================================================
  // Errors. The pipeline maps these to `CortexFailure.IndexInvalid` with a
  // formatted detail string; the walker stays free of the pipeline's failure
  // union.
  // ===========================================================================

  enum IndexError:
    case DuplicateSlug(bookSlug: String, slugs: List[String])
    case MaxDepthExceeded(pathInBook: String)
    case InvalidSlug(pathInBook: String, slug: String)

  // ===========================================================================
  // Public API.
  // ===========================================================================

  /**
   * Walk a list of root-level entries. Each top-level `CortexDir` whose name is slug-like, does not start
   * with `_` or `.`, and is not a reserved companion-asset directory ([[ReservedAuxDirs]]) becomes a Book;
   * other top-level entries are silently skipped (so `_drafts/`, `.git/`, and stray files can sit alongside
   * real books without failing the index). The same filter applies uniformly at every level — Books,
   * Sections, and Chapters.
   *
   * Books are ordered by `book.json#order` (see [[BookMeta]]); Sections and Chapters by numeric prefix.
   */
  def walk(roots: List[CortexEntry]): Either[IndexError, WalkResult] =
    val bookDirs = roots
      .collect { case d: CortexDir if includesAsContent(d.name) => d }
      .sortBy(d => (d.bookMeta.flatMap(_.order).getOrElse(Int.MaxValue), d.name.toLowerCase))
    val results = bookDirs.map(d => buildBook(d, d.name))
    results.collectFirst { case Left(e) => e } match
      case Some(e) => Left(e)
      case None =>
        val books = results.collect { case Right((b, _)) => b }
        val maps  = results.collect { case Right((b, m)) => b.slug -> m }.toMap
        Right(WalkResult(CortexIndex(books), maps))

  /**
   * Directory names that are companion source/asset locations rather than chapter content.
   *
   *   - `examples` — runnable code projects sitting next to a lesson (their `README.md` and any internal docs
   *     would otherwise pollute the sidebar as rogue chapters).
   *   - `c4` — LikeC4 `.c4` source files for a Part's interactive diagrams. Collected by the LikeC4 build
   *     pipeline (`Dockerfile.likec4`) into the LikeC4 SPA project root; never directly served from Cortex.
   *
   * The check is on the order-prefix-stripped name, so both `examples/` and `01-examples/` qualify. Add new
   * reserved names sparingly: a single literal is much easier to reason about than a regex.
   */
  val ReservedAuxDirs: Set[String] = Set("examples", "c4")

  /**
   * Whether a directory name is eligible to become a Book / Section. Excludes `_*`, `.*`, anything
   * non-slug-like, and reserved-aux directory names ([[ReservedAuxDirs]]).
   */
  def includesAsContent(name: String): Boolean =
    slugLike(name) &&
      !name.startsWith("_") &&
      !name.startsWith(".") &&
      !ReservedAuxDirs.contains(stripOrderPrefix(name))

  /** Reject anything that isn't a simple slug — letters, digits, hyphens, underscores; non-empty. */
  def slugLike(s: String): Boolean =
    s.nonEmpty && s.forall(c => c.isLetterOrDigit || c == '-' || c == '_')

  /**
   * A hierarchical chapter slug: a non-empty `/`-joined path whose every segment is itself [[slugLike]].
   * Empty segments (`""`, from a leading/trailing/doubled `/`) and `..` are rejected, so this doubles as the
   * path-traversal guard for the multi-segment `{chapter}` route param.
   */
  def chapterPathLike(s: String): Boolean =
    s.nonEmpty && s.split("/", -1).forall(slugLike)

  /** Strip a leading numeric ordering prefix: `01-foo` → `foo`, `1.bar` → `bar`. */
  def stripOrderPrefix(name: String): String =
    val m = "^\\d+[._-]?".r.findPrefixOf(name).getOrElse("")
    name.drop(m.length)

  /** Humanise a slug into a display title: `singly-linked-list` → `Singly Linked List`. */
  def humanise(name: String): String =
    val cleaned = stripOrderPrefix(name).stripSuffix(".md")
    cleaned
      .split("[-_.]")
      .iterator
      .filter(_.nonEmpty)
      .map(w => w.head.toUpper.toString + w.tail.toLowerCase)
      .mkString(" ")

  /**
   * Slugify a single path segment: lowercase letters/digits, underscore preserved, every other run of chars
   * collapsed to a single hyphen. Leading and trailing hyphens trimmed.
   */
  def slugify(seg: String): String =
    val sb       = new StringBuilder
    var lastDash = false
    seg.foreach {
      case c if c.isLetterOrDigit =>
        sb.append(c.toLower)
        lastDash = false
      case '_' =>
        sb.append('_')
        lastDash = false
      case _ if !lastDash && sb.nonEmpty =>
        sb.append('-')
        lastDash = true
      case _ =>
    }
    val s = sb.toString
    if s.endsWith("-") then s.dropRight(1) else s

  // ===========================================================================
  // Internals.
  // ===========================================================================

  private def buildBook(
      bookDir: CortexDir,
      slug: String
  ): Either[IndexError, (Book, Map[String, String])] =
    val acc = scala.collection.mutable.ListBuffer.empty[(ChapterRef, String)]
    walkSection(bookDir, Vector.empty, Vector.empty, DefaultEssential, acc) match
      case Left(e) => Left(e)
      case Right(()) =>
        val chRefs = acc.iterator.map(_._1).toList
        val dups   = chRefs.groupBy(_.slug).filter(_._2.size > 1).keys.toList.sorted
        if dups.nonEmpty then Left(IndexError.DuplicateSlug(slug, dups))
        else
          val reverseMap  = acc.iterator.map { case (ch, path) => ch.slug -> path }.toMap
          val title       = bookDir.bookMeta.flatMap(_.title).getOrElse(humanise(slug))
          val description = bookDir.bookMeta.flatMap(_.description).getOrElse("")
          val tags        = bookDir.bookMeta.flatMap(_.tags)
          val estMins     = bookDir.bookMeta.flatMap(_.estimatedReadingMinutes)
          val book = Book(
            slug = slug,
            title = title,
            description = description,
            tags = tags,
            estimatedReadingMinutes = estMins,
            chapters = chRefs
          )
          Right((book, reverseMap))

  /**
   * Recursive section walk. `pathInBook` accumulates FS-name segments from the book root down to `dir`;
   * `groupPath` accumulates Section display titles for the chapter's `groupPath` field. The two diverge
   * because a section directory's display title can come from `_section.json#title` while its FS name still
   * carries the numeric ordering prefix.
   *
   * `inheritedEssential` carries the cascading essential/optional default down the tree. A section's
   * `_section.json#defaultStatus` flips this for itself and its descendants until another `_section.json`
   * overrides it; a chapter's frontmatter `essential:` overrides per-chapter.
   */
  private def walkSection(
      dir: CortexDir,
      pathInBook: Vector[String],
      groupPath: Vector[String],
      inheritedEssential: Boolean,
      acc: scala.collection.mutable.ListBuffer[(ChapterRef, String)]
  ): Either[IndexError, Unit] =
    if groupPath.length > MaxSectionDepth then
      Left(IndexError.MaxDepthExceeded(pathInBook.mkString("/")))
    else
      val children = ordered(dir.children)
      val mdFiles = children.collect {
        case f: CortexFile
            if f.name.endsWith(".md") && !f.name.startsWith("_") && !f.name.startsWith(".") =>
          f
      }
      val sectionDirs = children.collect {
        case d: CortexDir if includesAsContent(d.name) => d
      }
      val groupOpt = if groupPath.isEmpty then None else Some(groupPath.toSeq)

      val chapterError = mdFiles.iterator
        .map { f =>
          val pathSegs = pathInBook :+ f.name
          val slug     = chapterSlugFromPath(pathSegs.toList)
          if !chapterPathLike(slug) then Left(IndexError.InvalidSlug(pathSegs.mkString("/"), slug))
          else
            val fallback  = humanise(f.name.stripSuffix(".md"))
            val title     = Frontmatter.extractTitle(f.content, fallback)
            val essential = Frontmatter.extractEssential(f.content).getOrElse(inheritedEssential)
            val relPath   = pathSegs.mkString("/")
            acc += ((
              ChapterRef(slug = slug, title = title, groupPath = groupOpt, essential = Some(essential)),
              relPath
            ))
            Right(())
        }
        .collectFirst { case Left(e) => e }

      chapterError match
        case Some(e) => Left(e)
        case None =>
          sectionDirs.foldLeft[Either[IndexError, Unit]](Right(())) {
            case (Left(e), _) => Left(e)
            case (Right(()), d) =>
              val secTitle = d.sectionMeta.flatMap(_.title).getOrElse(humanise(d.name))
              val secDefault = d.sectionMeta
                .flatMap(_.defaultStatus)
                .flatMap(parseDefaultStatus)
                .getOrElse(inheritedEssential)
              walkSection(d, pathInBook :+ d.name, groupPath :+ secTitle, secDefault, acc)
          }

  /**
   * Build a hierarchical chapter slug from its in-book path segments. `["02-system", "01-next-step.md"]` →
   * `"system/next-step"`. Each segment is `.md`-stripped, order-prefix-stripped, slugified, and joined with
   * `/` so the slug mirrors the directory tree. Ordering prefixes are intentionally dropped: the URL encodes
   * a chapter's *identity*, not its *position*, so reordering chapters never churns their URLs (or the
   * tutor's problem_id).
   */
  private def chapterSlugFromPath(pathSegs: List[String]): String =
    pathSegs.iterator
      .map(_.stripSuffix(".md"))
      .filter(_.nonEmpty)
      .map(seg => slugify(stripOrderPrefix(seg)))
      .filter(_.nonEmpty)
      .mkString("/")

  /** Sort children by numeric prefix (`01-foo` before `02-bar`), then by lowercased name. */
  private def ordered(entries: List[CortexEntry]): List[CortexEntry] =
    entries.sortBy(c => (extractOrder(c.name), c.name.toLowerCase))

  /**
   * Numeric ordering key for sibling entries.
   *
   *   - `index.md` (or a bare `index` directory) sorts first within its level. The Part-level
   *     `01-foundations/index.md` is the landing page for "Part 1 — Foundations" and should appear *above*
   *     `01-what-system-design-means.md` in the sidebar, not after the last lesson.
   *   - `01-foo`, `02-bar`, … sort by their leading integer.
   *   - Everything else falls to the end (`Int.MaxValue`).
   */
  private def extractOrder(name: String): Int =
    val stripped = name.stripSuffix(".md")
    if stripped == "index" then -1
    else "^(\\d+)".r.findPrefixMatchOf(name).map(_.group(1).toInt).getOrElse(Int.MaxValue)
