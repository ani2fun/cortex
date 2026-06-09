package cortex.server.cortexPipeline

import cortex.server.config.CortexConfig
import cortex.server.content.MtimeCachedIndex
import cortex.shared.api.Endpoints.{ChapterPayload, CortexIndex}
import cortex.shared.book.CortexIndexWalker.{
  BookMeta,
  CortexDir,
  CortexEntry,
  CortexFile,
  IndexError,
  SectionMeta
}
import cortex.shared.book.{CortexIndexWalker, Frontmatter}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import zio.*

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/** Errors raised while reading or indexing Cortex content. Mapped to HTTP status codes in `ApiErrors`. */
sealed trait CortexFailure extends Product with Serializable

object CortexFailure:
  case object NotFound                          extends CortexFailure
  final case class IO(detail: String)           extends CortexFailure
  final case class IndexInvalid(detail: String) extends CortexFailure

/**
 * In-memory snapshot of the Cortex tree. Package-private because nothing outside the pipeline should observe
 * its shape — `reverseMaps` (book → chapter slug → relative file path) is an internal lookup structure used
 * by chapter-payload assembly. Callers only see `CortexIndex` and `ChapterPayload`. The cache watermark lives
 * in [[MtimeCachedIndex]], not here.
 */
final private[cortexPipeline] case class CortexState(
    index: CortexIndex,
    reverseMaps: Map[String, Map[String, String]]
)

/**
 * Filesystem accessor for the Cortex content tree. Internal seam — package-private; live impl built by
 * `CortexPipeline.live` against the on-disk content tree, fakes built in test sources.
 *
 *   - [[currentMtime]] is a cheap watermark over the whole tree (no file bodies read).
 *   - [[readFileSafe]] reads a chapter body, rejecting any path that escapes the cortex root.
 *   - [[loadRoots]] scans the tree into the in-memory [[CortexEntry]] form the pure
 *     [[cortex.shared.book.CortexIndexWalker]] consumes. The pipeline owns the walker invocation; this
 *     adapter only produces values.
 *
 * The split between filesystem scan and pure walker means tests construct `FakeCortexFs` with literal
 * `List[CortexEntry]` values and exercise the walker integration without filesystem ceremony.
 */
private[cortexPipeline] trait CortexFs:
  /** Cheap mtime-watermark over the whole tree. Reads no file bodies. */
  def currentMtime: IO[CortexFailure, Long]

  /** Path-traversal-safe file read. Rejects anything that escapes the cortex root. */
  def readFileSafe(rel: String): IO[CortexFailure, String]

  /** Scan the tree into the in-memory form the [[CortexIndexWalker]] consumes. */
  def loadRoots: IO[CortexFailure, List[CortexEntry]]

/**
 * Cached, deep facade over the Cortex content tree.
 *
 *   - `index` returns the global `CortexIndex` (rebuilt on mtime bump if `autoReload`).
 *   - `chapter` resolves the file path, reads the body, parses frontmatter, computes prev/next slugs, and
 *     assembles a `ChapterPayload` in one pass.
 *
 * Mirrors the ADR-0003 internal-seams pattern: the [[CortexFs]] accessor trait is package-private; the only
 * public surface is `index` + `chapter`. Lenient parsing per ADR-0001 — malformed `book.json`,
 * `_section.json`, and unterminated frontmatter fall through to humanised-slug defaults. The convention rules
 * (numeric-prefix ordering, max Section depth, Slug uniqueness, Frontmatter title fallback) live in the pure
 * shared module [[CortexIndexWalker]]; this pipeline owns the IO and the walker invocation, and delegates the
 * mtime-keyed cache to [[MtimeCachedIndex]] (shared with the Blog pipeline).
 */
trait CortexPipeline:
  def index: IO[CortexFailure, CortexIndex]
  def chapter(book: String, chapter: String): IO[CortexFailure, ChapterPayload]

object CortexPipeline:

  /** Direct construction from an explicit [[CortexFs]] accessor. Used by tests. */
  def from(cortexFs: CortexFs, autoReload: Boolean): UIO[CortexPipeline] =
    MtimeCachedIndex
      .make(autoReload, cortexFs.currentMtime, rebuildState(cortexFs))
      .map(state => CortexPipelineLive(cortexFs, state))

  /** Resource-free layer: builds a filesystem-backed [[CortexFs]] and an empty cache. */
  val live: ZLayer[CortexConfig, Nothing, CortexPipeline] =
    ZLayer.fromZIO {
      for
        cfg <- ZIO.service[CortexConfig]
        fs = LiveCortexFs(cfg.root)
        state <- MtimeCachedIndex.make(cfg.autoReload, fs.currentMtime, rebuildState(fs))
      yield CortexPipelineLive(fs, state)
    }

  /**
   * Scan the FS into a literal `CortexEntry` tree, walk it through the pure [[CortexIndexWalker]], and
   * assemble a [[CortexState]]. Walker errors are mapped to the pipeline's failure envelope here rather than
   * at the seam, so the FS adapter stays free of pipeline-level concerns. Passed to [[MtimeCachedIndex]] as
   * the rebuild step.
   */
  private def rebuildState(cortexFs: CortexFs): IO[CortexFailure, CortexState] =
    for
      roots <- cortexFs.loadRoots
      walked <- ZIO
        .fromEither(CortexIndexWalker.walk(roots))
        .mapError(indexErrorToFailure)
    yield CortexState(walked.index, walked.reverseMaps)

  // ===========================================================================
  // FS adapter — bytes ↔ values plus path-traversal containment. All convention
  // rules live in CortexIndexWalker; this adapter is intentionally thin.
  // ===========================================================================

  /** Lenient circe decoders for `book.json` and `_section.json`. Malformed JSON → `None` per ADR-0001. */
  private given Decoder[BookMeta]    = deriveDecoder
  private given Decoder[SectionMeta] = deriveDecoder

  final private class LiveCortexFs(root: String) extends CortexFs:

    private val rootFile: File = File(root).getAbsoluteFile

    private val rootPath: Path =
      if rootFile.isDirectory then rootFile.toPath.toRealPath()
      else rootFile.toPath.toAbsolutePath.normalize

    override def currentMtime: IO[CortexFailure, Long] =
      ZIO
        .attemptBlocking {
          if !rootFile.isDirectory then 0L
          else
            val stream = Files.walk(rootPath)
            try
              stream
                .iterator()
                .asScala
                .filter(p => Files.isRegularFile(p))
                .map(p => Files.getLastModifiedTime(p).toMillis)
                .foldLeft(0L)(math.max)
            finally stream.close()
        }
        .mapError(t => CortexFailure.IO(t.getMessage))

    override def readFileSafe(rel: String): IO[CortexFailure, String] =
      safeUnder(rel) match
        case Left(e) => ZIO.fail(e)
        case Right(file) =>
          ZIO
            .attemptBlocking {
              new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8)
            }
            .mapError(t => CortexFailure.IO(t.getMessage))

    override def loadRoots: IO[CortexFailure, List[CortexEntry]] =
      ZIO
        .attemptBlocking {
          if !rootFile.isDirectory then List.empty[CortexEntry]
          else listChildren(rootFile).map(loadEntry(_, atBookRoot = true))
        }
        .mapError(t => CortexFailure.IO(t.getMessage))

    // Path-traversal defense: resolve the candidate to its real on-disk path and reject
    // anything that escapes `rootPath`. Catches `..` segments, absolute-path inputs, and
    // symlinks pointing outside the cortex root.
    private def safeUnder(rel: String): Either[CortexFailure, File] =
      val candidate = File(rootFile, rel)
      if !candidate.exists() || !candidate.isFile() then Left(CortexFailure.NotFound)
      else
        // toRealPath throws on missing files / symlink loops; normalize() is the
        // non-symlink-resolving fallback. Containment is still enforced below either way.
        val real =
          try candidate.toPath.toRealPath()
          catch case _: Throwable => candidate.toPath.toAbsolutePath.normalize
        if real.startsWith(rootPath) then Right(candidate)
        else Left(CortexFailure.NotFound)

    private def loadEntry(f: File, atBookRoot: Boolean): CortexEntry =
      if f.isDirectory then loadDir(f, atBookRoot)
      else loadFile(f)

    // Load only the children the walker is going to look at: subdirectories (recursively) and
    // `.md` files. `book.json` / `_section.json` get hoisted into the dir's metadata above; other
    // files (txt, png, ...) aren't chapters and would be discarded by the walker anyway.
    private def loadDir(d: File, atBookRoot: Boolean): CortexDir =
      val children = listChildren(d).flatMap { c =>
        if c.isDirectory then Some(loadEntry(c, atBookRoot = false))
        else if c.getName.endsWith(".md") then Some(loadFile(c))
        else None
      }
      val bookMeta =
        if atBookRoot then readJson[BookMeta](File(d, "book.json")) else None
      val sectionMeta =
        if !atBookRoot then readJson[SectionMeta](File(d, "_section.json")) else None
      CortexDir(d.getName, bookMeta = bookMeta, sectionMeta = sectionMeta, children = children)

    private def loadFile(f: File): CortexFile =
      val content = scala.util
        .Try(new String(Files.readAllBytes(f.toPath), StandardCharsets.UTF_8))
        .getOrElse("")
      CortexFile(f.getName, content)

  // -- Shared FS helpers ---------------------------------------------------

  private def listChildren(dir: File): List[File] =
    Option(dir.listFiles()).getOrElse(Array.empty[File]).toList

  // Lenient on purpose: a malformed `book.json` / `_section.json` falls through to `None`
  // and the walker keeps building with humanised-slug fallbacks. A single bad meta file
  // shouldn't block the whole tree. See ADR-0001.
  // Pinned by the malformed-book.json spec in CortexPipelineSpec.
  private def readJson[A: Decoder](f: File): Option[A] =
    if !f.isFile then None
    else
      val raw = scala.util
        .Try(new String(Files.readAllBytes(f.toPath), StandardCharsets.UTF_8))
        .toOption
      raw.flatMap(decode[A](_).toOption)

  /** Map a walker-level `IndexError` into the pipeline's `IndexInvalid` envelope with a formatted detail. */
  private[cortexPipeline] def indexErrorToFailure(e: IndexError): CortexFailure = e match
    case IndexError.DuplicateSlug(bookSlug, slugs) =>
      CortexFailure.IndexInvalid(s"$bookSlug: duplicate chapter slugs: ${slugs.mkString(", ")}")
    case IndexError.MaxDepthExceeded(path) =>
      CortexFailure.IndexInvalid(
        s"section nesting exceeds max depth ${CortexIndexWalker.MaxSectionDepth} at $path"
      )
    case IndexError.InvalidSlug(path, slug) =>
      CortexFailure.IndexInvalid(s"chapter $path produces invalid slug '$slug'")

final private class CortexPipelineLive(
    cortexFs: CortexFs,
    state: MtimeCachedIndex[CortexFailure, CortexState]
) extends CortexPipeline:

  override def index: IO[CortexFailure, CortexIndex] =
    state.get.map(_.index)

  override def chapter(book: String, chapter: String): IO[CortexFailure, ChapterPayload] =
    if !CortexIndexWalker.slugLike(book) || !CortexIndexWalker.chapterPathLike(chapter) then
      ZIO.fail(CortexFailure.NotFound)
    else
      for
        st <- state.get
        bk <- ZIO
          .fromOption(st.index.books.find(_.slug == book))
          .orElseFail(CortexFailure.NotFound: CortexFailure)
        relPath <- ZIO
          .fromOption(st.reverseMaps.get(book).flatMap(_.get(chapter)))
          .orElseFail(CortexFailure.NotFound: CortexFailure)
        chapterIndex = bk.chapters.indexWhere(_.slug == chapter)
        ch           = bk.chapters(chapterIndex)
        raw <- cortexFs.readFileSafe(s"$book/$relPath")
        parsed   = Frontmatter.parse(raw, fallbackTitle = ch.title)
        prevSlug = if chapterIndex > 0 then Some(bk.chapters(chapterIndex - 1).slug) else None
        nextSlug =
          if chapterIndex < bk.chapters.length - 1 then Some(bk.chapters(chapterIndex + 1).slug)
          else None
      yield ChapterPayload(
        book = bk,
        chapter = ch,
        frontmatter = parsed.frontmatter,
        // Rewrite relative asset URLs (./diagrams/foo.svg, ../shared/bar.png) to absolute
        // /api/cortex/asset/<book>/<resolved-path>. Lets authors write portable markdown
        // that works on GitHub and inside Cortex without learning a URL convention. See
        // [[ChapterAssetRewrite]] for what does and doesn't get rewritten.
        raw = ChapterAssetRewrite.rewrite(parsed.body, book, relPath),
        prevSlug = prevSlug,
        nextSlug = nextSlug
      )
