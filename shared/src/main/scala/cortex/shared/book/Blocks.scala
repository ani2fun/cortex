package cortex.shared.book

/**
 * Cortex **Blocks** — typed payloads of the placeholder `<div>`s that the markdown pipeline emits inside a
 * Chapter's rendered HTML. The pipeline emits `RunnableCode`, `RunnableGroup`, `Mermaid`, `D2Slides`,
 * `D2Inline`, `D3Widget`, `TracedCode`, `LikeC4`, plus the workbench family — `WorkbenchInline` (runnable
 * fences with an absorbed ```testcases spec), `YourTurn` (packaged exercise section opening a modal
 * workbench), `ProblemWorkbench` (a whole problem page), `Quiz`, and `SolutionViewer`. Each one mounts a
 * Scala.js React component on the client.
 *
 * This module owns the **structural validation** that turns raw attribute / child data into a typed `Block`
 * (or a `BlockDecodeError`). It runs identically on the JVM and on Scala.js so the validation is
 * unit-testable without a DOM. The Scala.js DOM walk + URI / JSON shims live in
 * `client.components.cortex.BlockDiscovery`, which feeds these decoders.
 *
 * Mirrors ADR-0004's server-side bytes↔values split: live wire adapters do the IO, pure modules do the
 * shape-checking, tests bypass IO and exercise the pure modules directly.
 */
object Blocks:

  /**
   * `RawTab` is the JSON-decoded shape of one entry in a `data-tabs` array. Every field is `Option` because
   * the JSON arrives untyped — `js.JSON.parse` on the client produces `js.Dynamic`, and the field-by-field
   * extraction can yield a missing string. The shared decoder validates required-vs-optional centrally.
   */
  final case class RawTab(
      language: Option[String],
      languageLabel: Option[String],
      source: Option[String],
      runnable: Option[Boolean],
      viz: Option[String] = None,
      vizRoot: Option[String] = None,
      vizCase: Option[String] = None,
      vizKind: Option[String] = None
  )

  /**
   * Decode a `runnable-code` placeholder.
   *
   *   - `language` (from `data-lang`) and `source` (from URI-decoded `data-source`) are required.
   *   - `languageLabel` (from `data-language-label`) is optional — falls through as `None`.
   */
  def decodeRunnableCode(
      language: Option[String],
      source: Option[String],
      languageLabel: Option[String],
      viz: Option[String] = None,
      vizRoot: Option[String] = None,
      vizCase: Option[String] = None,
      vizKind: Option[String] = None
  ): Either[BlockDecodeError, Block.RunnableCode] =
    for
      lang <- language.toRight(BlockDecodeError.MissingAttribute("runnable-code", "data-lang"))
      src  <- source.toRight(BlockDecodeError.MissingAttribute("runnable-code", "data-source"))
    yield Block.RunnableCode(
      lang,
      src,
      languageLabel.filter(_.nonEmpty),
      viz.filter(_.nonEmpty),
      vizRoot.filter(_.nonEmpty),
      vizCase.flatMap(_.toIntOption),
      vizKind.filter(_.nonEmpty)
    )

  /**
   * Decode a `runnable-group` placeholder. Empty `tabs` is treated as a "skip me" signal — the markdown
   * pipeline can emit a placeholder with no children if the source group block is empty, and rendering an
   * empty tab strip is worse than rendering nothing.
   *
   * Each `RawTab` requires `language`, `languageLabel`, and `source`. `runnable` defaults to `true` when
   * absent (older payloads / future tabs that omit the field still get a runnable tab).
   */
  def decodeRunnableGroup(
      rawTabs: List[RawTab]
  ): Either[BlockDecodeError, Block.RunnableGroup] =
    decodeTabs(rawTabs, "runnable-group").map(Block.RunnableGroup(_))

  /** Shared RawTab-list validation used by `runnable-group` and the workbench placeholders. */
  private def decodeTabs(
      rawTabs: List[RawTab],
      blockKind: String
  ): Either[BlockDecodeError, List[Block.Tab]] =
    if rawTabs.isEmpty then Left(BlockDecodeError.EmptyContent(blockKind, "tabs"))
    else
      val zero: Either[BlockDecodeError, List[Block.Tab]] = Right(Nil)
      rawTabs.zipWithIndex.foldLeft(zero) {
        case (Left(e), _) => Left(e)
        case (Right(acc), (raw, i)) =>
          for
            lang  <- raw.language.toRight(BlockDecodeError.MalformedTab(i, "language"))
            label <- raw.languageLabel.toRight(BlockDecodeError.MalformedTab(i, "languageLabel"))
            src   <- raw.source.toRight(BlockDecodeError.MalformedTab(i, "source"))
          yield acc :+ Block.Tab(
            lang,
            label,
            src,
            raw.runnable.getOrElse(true),
            raw.viz.filter(_.nonEmpty),
            raw.vizRoot.filter(_.nonEmpty),
            raw.vizCase.flatMap(_.toIntOption),
            raw.vizKind.filter(_.nonEmpty)
          )
      }

  // ---------------------------------------------------------------------------
  // Workbench decoders — `workbench-inline`, `your-turn`, `problem-workbench`,
  // `quiz-block`, `workbench-solution` (ADR-pending: the stacked editor + test
  // cases console family).
  // ---------------------------------------------------------------------------

  /** JSON-decoded shape of one entry in a `data-spec` args array (all-Option, like [[RawTab]]). */
  final case class RawArgSpec(
      id: Option[String],
      label: Option[String],
      tpe: Option[String],
      placeholder: Option[String]
  )

  /** JSON-decoded shape of one entry in a `data-spec` cases array. */
  final case class RawTestCase(
      args: Map[String, String],
      expected: Option[String]
  )

  /** JSON-decoded shape of a full `data-spec` payload. */
  final case class RawTestSpec(
      args: List[RawArgSpec],
      cases: List[RawTestCase]
  )

  /**
   * Validate a `data-spec` payload. The markdown pipeline already validated the authored JSON (a bad fence
   * becomes a visible `.workbench-error` card, not a placeholder), so failures here mean a malformed
   * placeholder — decoded leniently but reported precisely.
   */
  private def decodeSpec(
      rawSpec: Option[RawTestSpec],
      blockKind: String
  ): Either[BlockDecodeError, Block.TestSpec] =
    for
      spec <- rawSpec.toRight(BlockDecodeError.MissingAttribute(blockKind, "data-spec"))
      args <- spec.args.zipWithIndex.foldLeft[Either[BlockDecodeError, List[Block.ArgSpec]]](Right(Nil)) {
        case (Left(e), _) => Left(e)
        case (Right(acc), (raw, i)) =>
          raw.id
            .toRight(BlockDecodeError.MalformedSpec(blockKind, s"arg $i is missing an id"))
            .map(id =>
              acc :+ Block.ArgSpec(
                id = id,
                label = raw.label.filter(_.nonEmpty).getOrElse(id),
                tpe = raw.tpe.getOrElse(""),
                placeholder = raw.placeholder.filter(_.nonEmpty)
              )
            )
      }
      _ <- if args.isEmpty then Left(BlockDecodeError.EmptyContent(blockKind, "spec args")) else Right(())
      cases <- {
        val ids = args.map(_.id).toSet
        spec.cases.zipWithIndex.foldLeft[Either[BlockDecodeError, List[Block.TestCase]]](Right(Nil)) {
          case (Left(e), _) => Left(e)
          case (Right(acc), (raw, i)) =>
            raw.args.keys.find(k => !ids.contains(k)) match
              case Some(bad) =>
                Left(BlockDecodeError.MalformedSpec(blockKind, s"case $i references unknown arg `$bad`"))
              case None =>
                Right(acc :+ Block.TestCase(raw.args, raw.expected.filter(_.nonEmpty)))
        }
      }
      _ <- if cases.isEmpty then Left(BlockDecodeError.EmptyContent(blockKind, "spec cases")) else Right(())
    yield Block.TestSpec(args, cases)

  /** Decode a `workbench-inline` placeholder — runnable tabs + the absorbed test-case spec. */
  def decodeWorkbenchInline(
      rawTabs: List[RawTab],
      rawSpec: Option[RawTestSpec]
  ): Either[BlockDecodeError, Block.WorkbenchInline] =
    for
      tabs <- decodeTabs(rawTabs, "workbench-inline")
      spec <- decodeSpec(rawSpec, "workbench-inline")
    yield Block.WorkbenchInline(tabs, spec)

  /**
   * Decode a `your-turn` placeholder. `statementHtml` / `editorialHtml` are the inner HTML of the two
   * `<template data-wb=…>` children. The statement is required (a launch card with an empty modal Description
   * is worse than the authored prose); the editorial may be empty — the modal then hides that tab's content
   * behind a "no editorial yet" note.
   */
  def decodeYourTurn(
      title: Option[String],
      blurb: Option[String],
      statementHtml: String,
      editorialHtml: String,
      rawTabs: List[RawTab],
      rawSpec: Option[RawTestSpec]
  ): Either[BlockDecodeError, Block.YourTurn] =
    for
      tabs <- decodeTabs(rawTabs, "your-turn")
      spec <- decodeSpec(rawSpec, "your-turn")
      _ <-
        if statementHtml.trim.isEmpty then Left(BlockDecodeError.EmptyContent("your-turn", "statement"))
        else Right(())
    yield Block.YourTurn(
      title = title.filter(_.nonEmpty).getOrElse("Your Turn"),
      blurb = blurb.filter(_.nonEmpty),
      statementHtml = statementHtml,
      editorialHtml = editorialHtml,
      tabs = tabs,
      spec = spec
    )

  /**
   * Decode a `problem-workbench` placeholder — the whole problem page. `sections` arrives as `(title, html)`
   * pairs from the `<template data-wb="ed-section">` children, in document order. Sections may be empty (a
   * problem without an editorial yet); the description may not.
   */
  def decodeProblemWorkbench(
      descriptionHtml: String,
      sections: List[(String, String)],
      rawTabs: List[RawTab],
      rawSpec: Option[RawTestSpec]
  ): Either[BlockDecodeError, Block.ProblemWorkbench] =
    for
      tabs <- decodeTabs(rawTabs, "problem-workbench")
      spec <- decodeSpec(rawSpec, "problem-workbench")
      _ <-
        if descriptionHtml.trim.isEmpty then
          Left(BlockDecodeError.EmptyContent("problem-workbench", "description"))
        else Right(())
    yield Block.ProblemWorkbench(
      descriptionHtml = descriptionHtml,
      sections = sections.map(Block.EdSection(_, _)),
      tabs = tabs,
      spec = spec
    )

  /**
   * Decode a `quiz-block` placeholder. The pipeline validated the authored JSON, so this re-checks the same
   * invariants on the decoded payload: an input line, at least two options, and an answer that is one of
   * them.
   */
  def decodeQuiz(
      prompt: Option[String],
      input: Option[String],
      options: List[String],
      answer: Option[String]
  ): Either[BlockDecodeError, Block.Quiz] =
    for
      in  <- input.filter(_.nonEmpty).toRight(BlockDecodeError.MissingAttribute("quiz-block", "input"))
      ans <- answer.filter(_.nonEmpty).toRight(BlockDecodeError.MissingAttribute("quiz-block", "answer"))
      _ <-
        if options.sizeIs < 2 then Left(BlockDecodeError.EmptyContent("quiz-block", "options")) else Right(())
      _ <-
        if options.contains(ans) then Right(())
        else Left(BlockDecodeError.MalformedSpec("quiz-block", "answer is not among the options"))
    yield Block.Quiz(prompt.filter(_.nonEmpty), in, options, ans)

  /**
   * Decode a `workbench-solution` placeholder — the read-only solution viewer inside problem editorials.
   * Solution tabs reuse [[RawTab]] (the JSON carries language / languageLabel / source; `runnable` is absent
   * and irrelevant — the viewer never runs anything).
   */
  def decodeSolutionViewer(
      rawTabs: List[RawTab],
      time: Option[String],
      space: Option[String]
  ): Either[BlockDecodeError, Block.SolutionViewer] =
    decodeTabs(rawTabs, "workbench-solution").map(
      Block.SolutionViewer(_, time.filter(_.nonEmpty), space.filter(_.nonEmpty))
    )

  /** Decode a `mermaid-block` placeholder. Source (URI-decoded `data-mermaid-source`) is required. */
  def decodeMermaid(
      source: Option[String]
  ): Either[BlockDecodeError, Block.Mermaid] =
    source
      .toRight(BlockDecodeError.MissingAttribute("mermaid-block", "data-mermaid-source"))
      .map(Block.Mermaid(_))

  /**
   * Decode a `standalone-coach` placeholder. The author's `data-coach-problem-id` is optional — when absent
   * the mounter falls back to the chapter's coach problemId — so this always succeeds.
   */
  def decodeStandaloneCoach(
      coachProblemId: Option[String]
  ): Either[BlockDecodeError, Block.StandaloneCoach] =
    Right(Block.StandaloneCoach(coachProblemId))

  /**
   * Decode a `d2-slides` placeholder. `slides` is the inner-HTML of each `div.d2-slide` child element, in
   * order. Empty `slides` is treated as "skip me" (no useful diagram to render). Caption is optional;
   * empty-string captions are normalised to `None` to match the original behaviour.
   */
  def decodeD2Slides(
      slides: List[String],
      caption: Option[String]
  ): Either[BlockDecodeError, Block.D2Slides] =
    if slides.isEmpty then Left(BlockDecodeError.EmptyContent("d2-slides", "slides"))
    else Right(Block.D2Slides(slides, caption.filter(_.nonEmpty)))

  /**
   * Decode a `d2-diagram` placeholder. `svgHtml` is the inner-HTML of the placeholder element (the rendered
   * SVG markup). Empty content → skip.
   */
  def decodeD2Inline(
      svgHtml: String
  ): Either[BlockDecodeError, Block.D2Inline] =
    if svgHtml.isEmpty then Left(BlockDecodeError.EmptyContent("d2-diagram", "innerHTML"))
    else Right(Block.D2Inline(svgHtml))

  /**
   * Decode a `d3-widget` placeholder. `widget` (from `data-widget`) names a Scala.js + D3 component in the
   * client-side catalog; `payload` (URI-decoded `data-payload`) is the raw JSON the widget interprets. The
   * shared decoder only validates that both are present and non-empty — per-widget schema validation lives in
   * the widget itself, mirroring the `D2Slides(slides: List[String])` precedent where shared keeps the
   * payload structural and the client renderer parses it.
   */
  def decodeD3Widget(
      widget: Option[String],
      payload: Option[String]
  ): Either[BlockDecodeError, Block.D3Widget] =
    for
      name <- widget.toRight(BlockDecodeError.MissingAttribute("d3-widget", "data-widget"))
      data <- payload.toRight(BlockDecodeError.MissingAttribute("d3-widget", "data-payload"))
    yield Block.D3Widget(name, data)

  /**
   * Decode a `traced-code-block` placeholder. `language` (from `data-lang`) is the runtime to execute under
   * (e.g. "python" or "java"). `source` (URI-decoded `data-source`) is the user program. `companions` carries
   * zero or more source-only language tabs (e.g. Kotlin, Scala) decoded from `data-companions`; their source
   * is displayed with syntax highlighting and a `LanguageLockBanner` (no tracer). The actual tracer wrapper
   * lives on the client — the server-side `/api/run` is unchanged. See ADR-0007 / ADR-0021.
   */
  def decodeTracedCode(
      language: Option[String],
      source: Option[String],
      companions: List[Block.TraceCompanion] = Nil
  ): Either[BlockDecodeError, Block.TracedCode] =
    for
      lang <- language.toRight(BlockDecodeError.MissingAttribute("traced-code-block", "data-lang"))
      src  <- source.toRight(BlockDecodeError.MissingAttribute("traced-code-block", "data-source"))
    yield Block.TracedCode(lang, src, companions)

  /**
   * Decode a `likec4-iframe` placeholder. `src` (from `data-src`) is the URL of the LikeC4 view (e.g.
   * `/c4/view/foo`) and is the only required attribute. `height` (from `data-height`) is the inline iframe
   * height in pixels; malformed values fall through as `None` so the renderer uses its default. `title` (from
   * `data-title`) is the accessible label for the iframe; empty-string is normalised to `None`.
   *
   * Validation stays minimal — the LikeC4 SPA itself decides what to render for a given `src`, and a 404
   * inside the iframe is a runtime concern, not a placeholder-decode concern.
   */
  def decodeLikeC4(
      src: Option[String],
      height: Option[String],
      title: Option[String]
  ): Either[BlockDecodeError, Block.LikeC4] =
    src
      .toRight(BlockDecodeError.MissingAttribute("likec4-iframe", "data-src"))
      .map(s => Block.LikeC4(s, height.flatMap(_.toIntOption), title.filter(_.nonEmpty)))

/**
 * The decoded payload of a single placeholder `<div>` in a rendered Chapter. Each variant maps 1:1 to a
 * Scala.js React component on the client; the renderer (`ChapterContent`) is a total `Block => VdomElement`
 * dispatch.
 */
sealed trait Block

object Block:

  final case class RunnableCode(
      language: String,
      source: String,
      languageLabel: Option[String],
      viz: Option[String] = None,
      vizRoot: Option[String] = None,
      vizCase: Option[Int] = None,
      vizKind: Option[String] = None
  ) extends Block

  final case class Tab(
      language: String,
      languageLabel: String,
      source: String,
      runnable: Boolean,
      viz: Option[String] = None,
      vizRoot: Option[String] = None,
      vizCase: Option[Int] = None,
      vizKind: Option[String] = None
  )

  final case class RunnableGroup(tabs: List[Tab]) extends Block

  final case class Mermaid(source: String) extends Block

  /**
   * A standalone six-step coach for theory lessons (no code editor) — the live coach mounted inline in prose.
   * `coachProblemId` is an optional author override (`data-coach-problem-id`); when absent the mounter falls
   * back to the chapter's `<book>/<chapter-slug>` join key.
   */
  final case class StandaloneCoach(coachProblemId: Option[String]) extends Block

  final case class D2Slides(
      slides: List[String],
      caption: Option[String]
  ) extends Block

  final case class D2Inline(svgHtml: String) extends Block

  /**
   * Named entry in the client-side D3 widget catalog plus the raw JSON payload it interprets. Schema is
   * deliberately loose at this layer — each widget owns its own decoder, so the catalog can grow without
   * regenerating shared types.
   */
  final case class D3Widget(widget: String, payload: String) extends Block

  /**
   * Step-through visualisation for a code block whose execution we want to inspect. `language` is the primary
   * traced runtime ("python" or "java"); `source` is the user program. On the client, a tracer harness wraps
   * the source and posts to the existing `/api/run`; the trace is parsed out of stdout and rendered as code +
   * locals panel + step controls. `companions` carries zero or more source-only language tabs (e.g. Kotlin,
   * Scala) that display the equivalent source with a `LanguageLockBanner` but no tracer — they share the same
   * multi-tab UI as the primary traced language.
   */
  final case class TracedCode(
      language: String,
      source: String,
      companions: List[Block.TraceCompanion] = Nil
  ) extends Block

  /**
   * A source-only companion language tab inside a [[TracedCode]] block. These are emitted when the chapter
   * ships adjacent `kotlin trace` / `scala trace` fences alongside a primary `java trace` fence. The
   * companion tab shows the source with syntax highlighting and a `LanguageLockBanner` ("Trace frozen ·
   * &lt;language&gt; is source-only — switch back to Java") but runs no tracer.
   */
  final case class TraceCompanion(language: String, source: String)

  /**
   * Embedded LikeC4 diagram view, surfaced as an `<iframe>` pointing at the LikeC4 SPA (proxied under `/c4`).
   * `src` is the upstream URL (e.g. `/c4/view/foundations_cp_cluster_containers`); `height` is the optional
   * inline pixel height the author asked for (renderer falls back to a default when absent); `title` is the
   * optional accessible label (already markdown-author-controlled via the original `title` attribute).
   *
   * The component wraps the iframe with a hover-visible Zoom button that opens a near-fullscreen modal —
   * mirrors the `D2Inline` affordance but delegates pan/zoom inside the diagram to the LikeC4 SPA itself.
   */
  final case class LikeC4(src: String, height: Option[Int], title: Option[String]) extends Block

  /**
   * One named argument of a workbench test-case spec. `tpe` is the display type label (`int[]`, `string`,
   * `int`, …) — it also drives the console's field-span rule (arrays/strings span the full row) and is never
   * interpreted beyond that. `label` falls back to `id` at decode time.
   */
  final case class ArgSpec(id: String, label: String, tpe: String, placeholder: Option[String])

  /**
   * One test case: argument values keyed by [[ArgSpec.id]] (serialized exactly as they should appear in the
   * console fields and on stdin), plus the expected stdout. `expected = None` means "run and show the output,
   * no verdict" — used for freshly added cases.
   */
  final case class TestCase(args: Map[String, String], expected: Option[String])

  /** The schema-driven test-case console payload of a workbench block (from a ```testcases fence). */
  final case class TestSpec(args: List[ArgSpec], cases: List[TestCase])

  /** One accordion section of a problem editorial: a `<details>` summary title + its body HTML. */
  final case class EdSection(title: String, html: String)

  /**
   * The inline stacked workbench — Code pane (language dropdown · Edit · Visualise · Run) over the
   * schema-driven Test-cases console. Emitted when runnable fence(s) are immediately followed by a
   * \```testcases fence. Replaces `RunnableCode` / `RunnableGroup` for that group only; everything else in
   * the chapter renders as before.
   */
  final case class WorkbenchInline(tabs: List[Tab], spec: TestSpec) extends Block

  /**
   * A packaged `## Your Turn` exercise section. Renders as a launch card in the chapter flow; opening it
   * mounts the full problem workbench in a modal — Description tab from `statementHtml`, an Editorial section
   * from `editorialHtml`, and the starter `tabs` + `spec` in the editor pane. `title` is the chapter's H1
   * (the modal needs a workbench title; the section heading itself is always "Your Turn").
   */
  final case class YourTurn(
      title: String,
      blurb: Option[String],
      statementHtml: String,
      editorialHtml: String,
      tabs: List[Tab],
      spec: TestSpec
  ) extends Block

  /**
   * A whole problem chapter packaged into the two-pane workbench: Description / Editorial / Coach tabs on the
   * left, editor + test-cases console on the right. The page-level chrome (title, difficulty badge, topic
   * pills) comes from the chapter frontmatter, not from this block.
   */
  final case class ProblemWorkbench(
      descriptionHtml: String,
      sections: List[EdSection],
      tabs: List[Tab],
      spec: TestSpec
  ) extends Block

  /**
   * Answer-pick quiz card (the Description tab's "Now your turn!" interaction): show `input`, pick one of
   * `options`, Validate → Correct / Wrong-try-again. Single-shot client state, no persistence.
   */
  final case class Quiz(
      prompt: Option[String],
      input: String,
      options: List[String],
      answer: String
  ) extends Block

  /**
   * Read-only solution viewer used inside problem editorials: language tabs + a lock "Read-only" badge +
   * optional complexity meta chips. Tabs reuse [[Tab]]; `runnable` is meaningless here (nothing executes).
   */
  final case class SolutionViewer(
      tabs: List[Tab],
      time: Option[String],
      space: Option[String]
  ) extends Block

/**
 * Why a placeholder `<div>` could not be turned into a `Block`. Both the JVM specs and the Scala.js
 * `BlockDiscovery` adapter use these — the adapter logs them via `console.warn` and continues (preserving
 * pre-extraction behaviour: one bad block doesn't block the rest of the chapter).
 */
sealed trait BlockDecodeError

object BlockDecodeError:
  /** A required HTML attribute was absent (or URI-decoding produced no value). */
  final case class MissingAttribute(blockKind: String, attr: String) extends BlockDecodeError

  /** Required content was empty (e.g. a `d2-diagram` with no inner SVG). */
  final case class EmptyContent(blockKind: String, what: String) extends BlockDecodeError

  /** A tab inside a `runnable-group` was missing a required field. `index` is the tab position (0-based). */
  final case class MalformedTab(index: Int, missingField: String) extends BlockDecodeError

  /**
   * A workbench `data-spec` / quiz payload violated a structural invariant (bad arg id, unknown case key, …).
   */
  final case class MalformedSpec(blockKind: String, what: String) extends BlockDecodeError
