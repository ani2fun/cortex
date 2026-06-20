package cortex.client.components.book

import cortex.shared.book.{Block, BlockDecodeError, Blocks}
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/**
 * Walk a rendered Chapter's `<article>` for Cortex Block placeholders, extract their attribute / child data,
 * URI-decode and JSON-parse where the markdown pipeline encoded them, and call into [[Blocks]] for structural
 * validation. The pure decoders live in `shared/`; this module is the JS-side adapter that owns the DOM walk
 * + URI / JSON shims that can't cross-compile to JVM.
 *
 * Errors are returned in [[DiscoveryResult.errors]] *and* simultaneously logged via `dom.console.warn` —
 * preserves the pre-extraction observable behaviour so existing diagnostics still work.
 */
object BlockDiscovery:

  /**
   * The outcome of one discovery pass over an article.
   *
   *   - `blocks` is the list of successfully-decoded `(placeholder, Block)` pairs in document order, ready to
   *     be rendered + mounted.
   *   - `errors` is the list of `(placeholder, BlockDecodeError)` pairs for placeholders that failed
   *     validation. Already logged to the console; surfaced here so callers (or future tests) can observe.
   */
  final case class DiscoveryResult(
      blocks: List[(dom.HTMLElement, Block)],
      errors: List[(dom.HTMLElement, BlockDecodeError)]
  )

  def discover(article: dom.Element): DiscoveryResult =
    val blocks = List.newBuilder[(dom.HTMLElement, Block)]
    val errors = List.newBuilder[(dom.HTMLElement, BlockDecodeError)]

    Discoverers.foreach { d =>
      val nodes = article.querySelectorAll(s"div.${d.className}")
      var i     = 0
      while i < nodes.length do
        val node = nodes.item(i).asInstanceOf[dom.HTMLElement]
        d.decode(node) match
          case Right(block) => blocks += node -> block
          case Left(err) =>
            warn(d.className, err)
            errors += node -> err
        i += 1
    }

    DiscoveryResult(blocks.result(), errors.result())

  // ---------------------------------------------------------------------------
  // Per-placeholder discoverers. Each owns its CSS class + the JS-side extraction
  // step (attribute reads, URI-decoding, JSON parsing of `data-tabs`, child-walk
  // for `d2-slides`). Structural validation is delegated to `Blocks.*`.
  // ---------------------------------------------------------------------------

  private trait Discoverer:
    def className: String
    def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block]

  private val Discoverers: List[Discoverer] =
    List(
      RunnableCode,
      RunnableGroup,
      WorkbenchInline,
      YourTurn,
      StandaloneCoach,
      ConceptCoach,
      ProblemWorkbench,
      QuizBlock,
      SolutionViewer,
      Mermaid,
      D2Slides,
      D2Inline,
      D3Widget,
      TracedCode,
      LikeC4
    )

  private object RunnableCode extends Discoverer:
    override val className: String = "runnable-code"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val lang    = nonEmpty(node.getAttribute("data-lang"))
      val src     = nonEmpty(node.getAttribute("data-source")).flatMap(uriDecode)
      val label   = nonEmpty(node.getAttribute("data-language-label"))
      val viz     = nonEmpty(node.getAttribute("data-viz"))
      val vizRoot = nonEmpty(node.getAttribute("data-viz-root"))
      val vizCase = nonEmpty(node.getAttribute("data-viz-case"))
      val vizKind = nonEmpty(node.getAttribute("data-viz-kind"))
      Blocks.decodeRunnableCode(lang, src, label, viz, vizRoot, vizCase, vizKind)

  private object RunnableGroup extends Discoverer:
    override val className: String = "runnable-group"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeRunnableGroup(tabsAttr(node))

  private object WorkbenchInline extends Discoverer:
    override val className: String = "workbench-inline"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeWorkbenchInline(tabsAttr(node), specAttr(node))

  private object YourTurn extends Discoverer:
    override val className: String = "your-turn"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeYourTurn(
        title = nonEmpty(node.getAttribute("data-title")),
        blurb = nonEmpty(node.getAttribute("data-blurb")),
        statementHtml = templateHtml(node, "statement").getOrElse(""),
        editorialHtml = templateHtml(node, "editorial").getOrElse(""),
        rawTabs = tabsAttr(node),
        rawSpec = specAttr(node)
      )

  private object StandaloneCoach extends Discoverer:
    override val className: String = "standalone-coach"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeStandaloneCoach(nonEmpty(node.getAttribute("data-coach-problem-id")))

  private object ConceptCoach extends Discoverer:
    override val className: String = "concept-coach"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeConceptCoach(nonEmpty(node.getAttribute("data-coach-problem-id")))

  private object ProblemWorkbench extends Discoverer:
    override val className: String = "problem-workbench"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val sections = templates(node).collect {
        case tpl if tpl.getAttribute("data-wb") == "ed-section" =>
          Option(tpl.getAttribute("data-wb-title")).getOrElse("Notes") -> tpl.innerHTML
      }
      Blocks.decodeProblemWorkbench(
        descriptionHtml = templateHtml(node, "description").getOrElse(""),
        sections = sections,
        rawTabs = tabsAttr(node),
        rawSpec = specAttr(node)
      )

  private object QuizBlock extends Discoverer:
    override val className: String = "quiz-block"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      nonEmpty(node.getAttribute("data-quiz")).flatMap(uriDecode) match
        case None => Left(BlockDecodeError.MissingAttribute("quiz-block", "data-quiz"))
        case Some(json) =>
          Try {
            val obj = js.JSON.parse(json)
            (
              obj.prompt.asInstanceOf[js.UndefOr[String]].toOption,
              obj.input.asInstanceOf[js.UndefOr[String]].toOption,
              obj.options.asInstanceOf[js.UndefOr[js.Array[String]]].toOption
                .map(_.toList)
                .getOrElse(Nil),
              obj.answer.asInstanceOf[js.UndefOr[String]].toOption
            )
          } match
            case Success((prompt, input, options, answer)) =>
              Blocks.decodeQuiz(prompt, input, options, answer)
            case Failure(t) =>
              dom.console.warn(s"chapter: skipping quiz-block — malformed data-quiz JSON: ${t.getMessage}")
              Left(BlockDecodeError.MissingAttribute("quiz-block", "data-quiz"))

  private object SolutionViewer extends Discoverer:
    override val className: String = "workbench-solution"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeSolutionViewer(
        rawTabs = tabsAttr(node),
        time = nonEmpty(node.getAttribute("data-time")),
        space = nonEmpty(node.getAttribute("data-space"))
      )

  private object Mermaid extends Discoverer:
    override val className: String = "mermaid-block"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val src = nonEmpty(node.getAttribute("data-mermaid-source")).flatMap(uriDecode)
      Blocks.decodeMermaid(src)

  private object D2Slides extends Discoverer:
    override val className: String = "d2-slides"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val slides = (0 until node.children.length).iterator.flatMap { j =>
        val child = node.children.item(j).asInstanceOf[dom.HTMLElement]
        if child.classList.contains("d2-slide") && child.innerHTML.nonEmpty then Some(child.innerHTML)
        else None
      }.toList
      val caption = nonEmpty(node.getAttribute("data-caption"))
      Blocks.decodeD2Slides(slides, caption)

  private object D2Inline extends Discoverer:
    override val className: String = "d2-diagram"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      Blocks.decodeD2Inline(node.innerHTML)

  private object D3Widget extends Discoverer:
    override val className: String = "d3-widget"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val widget  = nonEmpty(node.getAttribute("data-widget"))
      val payload = nonEmpty(node.getAttribute("data-payload")).flatMap(uriDecode)
      Blocks.decodeD3Widget(widget, payload)

  private object TracedCode extends Discoverer:
    // Mirrors the Mermaid pattern: `<block>-block` is the placeholder; the mounted widget renders its own
    // root class (here, `traced-code`) inside.
    override val className: String = "traced-code-block"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val lang = nonEmpty(node.getAttribute("data-lang"))
      val src  = nonEmpty(node.getAttribute("data-source")).flatMap(uriDecode)
      val companions = nonEmpty(node.getAttribute("data-companions")).flatMap(uriDecode)
        .map(parseCompanions).getOrElse(Nil)
      Blocks.decodeTracedCode(lang, src, companions)

    // Defensive JSON parse of the `data-companions` array:
    //   `[{language: "kotlin", source: "..."}, ...]`
    // Any parse failure silently produces an empty list — one bad companion doesn't kill the block.
    private def parseCompanions(json: String): List[Block.TraceCompanion] =
      Try {
        val arr = js.JSON.parse(json).asInstanceOf[js.Array[js.Dynamic]]
        arr.toList.flatMap { obj =>
          val lang = obj.language.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty)
          val src  = obj.source.asInstanceOf[js.UndefOr[String]].toOption.getOrElse("")
          lang.map(Block.TraceCompanion(_, src))
        }
      } match
        case Success(v) => v
        case Failure(t) =>
          dom.console.warn(s"chapter: skipping trace companions — malformed JSON: ${t.getMessage}")
          Nil

  private object LikeC4 extends Discoverer:
    override val className: String = "likec4-iframe"

    override def decode(node: dom.HTMLElement): Either[BlockDecodeError, Block] =
      val src    = nonEmpty(node.getAttribute("data-src"))
      val height = nonEmpty(node.getAttribute("data-height"))
      val title  = nonEmpty(node.getAttribute("data-title"))
      Blocks.decodeLikeC4(src, height, title)

  // ---------------------------------------------------------------------------
  // Shims
  // ---------------------------------------------------------------------------

  /**
   * URI-decode + JSON-parse a `data-tabs` attribute into RawTabs (shared by group + workbench + solution).
   */
  private def tabsAttr(node: dom.HTMLElement): List[Blocks.RawTab] =
    nonEmpty(node.getAttribute("data-tabs")).flatMap(uriDecode).map(parseRawTabs).getOrElse(Nil)

  /** URI-decode + JSON-parse a `data-spec` attribute into a RawTestSpec. */
  private def specAttr(node: dom.HTMLElement): Option[Blocks.RawTestSpec] =
    nonEmpty(node.getAttribute("data-spec")).flatMap(uriDecode).flatMap(parseRawSpec)

  // Defensive JSON → List[RawTab]. Anything that throws (malformed JSON, non-array root)
  // collapses to an empty list, which the shared decoders then reject as
  // `EmptyContent` — same observable outcome as the pre-extraction code's "skip the group".
  private def parseRawTabs(decoded: String): List[Blocks.RawTab] =
    Try {
      val parsed = js.JSON.parse(decoded).asInstanceOf[js.Array[js.Dynamic]]
      parsed.toList.map { obj =>
        Blocks.RawTab(
          language = obj.language.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          languageLabel = obj.languageLabel.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          source = obj.source.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          runnable = obj.runnable.asInstanceOf[js.UndefOr[Boolean]].toOption,
          viz = obj.viz.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          vizRoot = obj.vizRoot.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          vizCase = obj.vizCase.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          vizKind = obj.vizKind.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty)
        )
      }
    } match
      case Success(v) => v
      case Failure(t) =>
        dom.console.warn(s"chapter: malformed data-tabs JSON: ${t.getMessage}")
        Nil

  // Defensive JSON → RawTestSpec. `type` is a Scala keyword, hence selectDynamic.
  private def parseRawSpec(decoded: String): Option[Blocks.RawTestSpec] =
    Try {
      val obj = js.JSON.parse(decoded)
      val args = obj.args.asInstanceOf[js.Array[js.Dynamic]].toList.map { a =>
        Blocks.RawArgSpec(
          id = a.id.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          label = a.label.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          tpe = a.selectDynamic("type").asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty),
          placeholder = a.placeholder.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty)
        )
      }
      val cases = obj.cases.asInstanceOf[js.Array[js.Dynamic]].toList.map { c =>
        val argMap = c.args.asInstanceOf[js.UndefOr[js.Dictionary[String]]].toOption
          .map(_.toMap)
          .getOrElse(Map.empty[String, String])
        Blocks.RawTestCase(
          args = argMap,
          expected = c.expected.asInstanceOf[js.UndefOr[String]].toOption
        )
      }
      Blocks.RawTestSpec(args, cases)
    } match
      case Success(v) => Some(v)
      case Failure(t) =>
        dom.console.warn(s"chapter: malformed data-spec JSON: ${t.getMessage}")
        None

  /** Direct `<template data-wb=…>` children of a workbench placeholder, in document order. */
  private def templates(node: dom.HTMLElement): List[dom.HTMLElement] =
    (0 until node.children.length).iterator
      .map(node.children.item(_).asInstanceOf[dom.HTMLElement])
      .filter(c => c.tagName.equalsIgnoreCase("template") && c.hasAttribute("data-wb"))
      .toList

  /**
   * Inner HTML of the `<template data-wb="<kind>">` child. `innerHTML` on a template serializes its inert
   * content `DocumentFragment` — the packaged sub-tree never rendered, and `querySelectorAll` above never
   * descended into it (that's the whole anti-double-mount trick).
   */
  private def templateHtml(node: dom.HTMLElement, kind: String): Option[String] =
    templates(node).find(_.getAttribute("data-wb") == kind).map(_.innerHTML)

  private def nonEmpty(s: String): Option[String] =
    Option(s).filter(_.nonEmpty)

  // `decodeURIComponent` throws URIError on a malformed escape sequence (e.g. `%2`).
  // Wrap so one bad block doesn't kill the rest of the discovery pass; the caller
  // surfaces the absence as a `MissingAttribute` (the JS-side decode produced no value).
  private def uriDecode(encoded: String): Option[String] =
    Try(js.Dynamic.global.decodeURIComponent(encoded).asInstanceOf[String]) match
      case Success(v) => Some(v)
      case Failure(t) =>
        dom.console.warn(s"chapter: malformed URI in placeholder: ${t.getMessage}")
        None

  private def warn(blockKind: String, err: BlockDecodeError): Unit = err match
    case BlockDecodeError.MissingAttribute(_, attr) =>
      dom.console.warn(s"chapter: skipping $blockKind — missing $attr")
    case BlockDecodeError.EmptyContent(_, what) =>
      dom.console.warn(s"chapter: skipping $blockKind — empty $what")
    case BlockDecodeError.MalformedTab(i, missing) =>
      dom.console.warn(s"chapter: skipping $blockKind — tab $i missing $missing")
    case BlockDecodeError.MalformedSpec(_, what) =>
      dom.console.warn(s"chapter: skipping $blockKind — $what")
