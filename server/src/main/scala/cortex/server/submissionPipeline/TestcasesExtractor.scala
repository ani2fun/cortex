package cortex.server.submissionPipeline

import io.circe.{Json, parser}

/**
 * Pure extraction of a chapter's ```testcases fence into runnable cases. The submissions handler re-derives
 * test cases from the chapter source instead of trusting the client, so a stored "Accepted" verdict means the
 * authored cases actually passed.
 *
 * Mirrors the client pipeline's `remarkAbsorbTestcases` semantics on the server: first fence wins, one stdin
 * line per declared argument in declared order, values serialized exactly as authored (numbers and booleans
 * coerced to their string form), empty `expected` treated as absent.
 */
object TestcasesExtractor:

  /** One runnable case: the exact stdin to feed the program and the expected (trimmed-compare) stdout. */
  final case class Case(stdin: String, expected: Option[String])

  final case class Spec(cases: List[Case])

  private val FenceOpen  = "```testcases"
  private val FenceClose = "```"

  /** Find the first ```testcases fence and turn it into ordered stdin/expected pairs. */
  def extract(markdown: String): Either[String, Spec] =
    fenceBody(markdown) match
      case None       => Left("the chapter has no ```testcases fence")
      case Some(body) => parseSpec(body)

  private def fenceBody(markdown: String): Option[String] =
    val lines = markdown.linesIterator.toVector
    val open  = lines.indexWhere(_.trim.startsWith(FenceOpen))
    if open < 0 then None
    else
      val close = lines.indexWhere(_.trim == FenceClose, open + 1)
      if close < 0 then None
      else Some(lines.slice(open + 1, close).mkString("\n"))

  private def parseSpec(body: String): Either[String, Spec] =
    for
      json <- parser.parse(body).left.map(e => s"testcases fence is not valid JSON: ${e.getMessage}")
      cursor = json.hcursor
      argIds <- cursor
        .downField("args")
        .as[List[Json]]
        .left
        .map(_ => "testcases fence is missing an `args` array")
        .flatMap { args =>
          val ids = args.flatMap(_.hcursor.downField("id").as[String].toOption).filter(_.nonEmpty)
          if ids.isEmpty then Left("testcases fence declares no usable args") else Right(ids)
        }
      rawCases <- cursor
        .downField("cases")
        .as[List[Json]]
        .left
        .map(_ => "testcases fence is missing a `cases` array")
      _     <- if rawCases.isEmpty then Left("testcases fence declares no cases") else Right(())
      cases <- Right(rawCases.map(toCase(argIds, _)))
    yield Spec(cases)

  private def toCase(argIds: List[String], c: Json): Case =
    val argsObj = c.hcursor.downField("args")
    val values = argIds.map { id =>
      argsObj.downField(id).focus.map(jsonToString).getOrElse("")
    }
    val expected = c.hcursor.downField("expected").as[String].toOption.filter(_.nonEmpty)
    Case(stdin = values.mkString("\n") + "\n", expected = expected)

  /** Authored values are usually strings; numbers/booleans coerce like the client pipeline does. */
  private def jsonToString(j: Json): String =
    j.asString.getOrElse(
      j.asNumber.map(_.toString).orElse(j.asBoolean.map(_.toString)).getOrElse("")
    )
