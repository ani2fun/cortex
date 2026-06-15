package cortex.client.api

import cortex.shared.tutor.TutorContract.*
import io.circe.Json
import io.circe.parser.parse
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js

/** What one combined client-direct BYOK call produced. */
final case class Turn(verdict: GateVerdict, coachReply: String)

/**
 * The BYOK client-direct provider — the browser half of the tutor's BYOK tier.
 *
 * One turn is ONE combined gate+coach call on the visitor's OWN key (never sent to any of our origins — the
 * tutor only assembles prompts and records outcomes): the tutor's prompt bundle carries the combined system +
 * transcript, the model is forced onto a `record_byok_turn` tool whose camelCase fields mirror the wire
 * [[GateVerdict]] plus the `coachReply` prose, and the parsed verdict + reply are posted back via
 * [[TutorApiClient.recordByokTurn]], where the SERVER applies the FSM transition.
 *
 * Provider-specific because the wire shape differs. [[OpenRouterByok]] (the primary BYOK path — one key, any
 * model) speaks the OpenAI-compatible chat-completions API; [[AnthropicByok]] (the purist "no middleman"
 * path) speaks the Anthropic Messages API. [[ByokProvider.forProvider]] picks by the catalog `provider`
 * string from `whoami`. Both stay fully client-side — the providers send permissive CORS for browser-direct
 * calls — so no key ever reaches our server.
 */
trait ByokProvider:
  /** Run one combined gate+coach turn client-direct on the user's key. */
  def runTurn(key: String, bundle: PromptBundle, userMessage: String): Future[Turn]

  /** Cheap key validation: a 2xx from a lightweight authenticated endpoint means the key is accepted. */
  def probe(key: String): Future[Boolean]

object ByokProvider:

  /** Pick the client-direct adapter for a catalog `provider` string (from `whoami` / the catalog). */
  def forProvider(provider: String): ByokProvider =
    if provider.equalsIgnoreCase("openrouter") then OpenRouterByok else AnthropicByok

  // ── the forced tool (camelCase fields mirror the wire GateVerdict + coachReply) ────────────────
  // The JSON-Schema body is shared; each provider wraps it in its own tool/function envelope.

  private[api] val ToolName        = "record_byok_turn"
  private[api] val ToolDescription = "Record the gate verdict and the coach reply for this turn."

  private def stringArray: Json =
    Json.obj("type" -> Json.fromString("array"), "items" -> Json.obj("type" -> Json.fromString("string")))

  /**
   * The verdict + coachReply object schema, shared by both providers' forced tools.
   *
   * Kept to the lowest common denominator of provider function-calling schemas: only a STRING `enum` (Gemini
   * rejects integer enums and `additionalProperties` — "Provider returned error"), with the allowed integer
   * ranges expressed in `description` instead. Claude/GPT accept this just as well.
   */
  private[api] val verdictSchema: Json = Json.obj(
    "type" -> Json.fromString("object"),
    "properties" -> Json.obj(
      "verdict" -> Json.obj(
        "type" -> Json.fromString("string"),
        "enum" -> Json.arr(List("pass", "retry", "off_topic", "question").map(Json.fromString)*)
      ),
      "score" -> Json.obj(
        "type"        -> Json.fromString("integer"),
        "description" -> Json.fromString("0 to 100, in steps of 10")
      ),
      "rubricHits" -> stringArray,
      "missing"    -> stringArray,
      "hint"       -> Json.obj("type" -> Json.fromString("string")),
      "nextHintLevel" -> Json.obj(
        "type"        -> Json.fromString("integer"),
        "description" -> Json.fromString("0 to 3")
      ),
      "coachReply" -> Json.obj("type" -> Json.fromString("string"))
    ),
    "required" -> Json.arr(List("verdict", "score", "coachReply").map(Json.fromString)*)
  )

  /** Decode the tool's camelCase argument object into a [[Turn]] (shared by both providers). */
  private[api] def turnFromToolInput(input: Json): Either[String, Turn] =
    for
      reply   <- input.hcursor.get[String]("coachReply").left.map(_ => "tool input missing coachReply")
      verdict <- input.as[GateVerdict].left.map(e => s"undecodable verdict: ${e.getMessage}")
    yield Turn(verdict, reply)

  /**
   * A friendly message for a non-2xx from a provider call. For OpenRouter the upstream provider's real error
   * is nested under `error.metadata.{raw,provider_name}`; surface it (and fall back to the raw body) so a
   * provider rejection — e.g. an unsupported schema construct — is actually diagnosable in the UI.
   */
  private[api] def providerError(provider: String, status: Int, body: String): String =
    val err    = parse(body).toOption.map(_.hcursor.downField("error"))
    val msg    = err.flatMap(_.get[String]("message").toOption)
    val raw    = err.flatMap(_.downField("metadata").get[String]("raw").toOption)
    val who    = err.flatMap(_.downField("metadata").get[String]("provider_name").toOption)
    val label  = who.fold(provider)(w => s"$provider/$w")
    val detail = List(msg, raw).flatten.distinct.mkString(" — ")
    status match
      case 401 => s"$provider rejected the API key — check it and try again."
      case 429 => s"$provider rate limit hit — wait a moment and resend."
      case _ =>
        if detail.nonEmpty then s"$label: $detail"
        else s"$label request failed (HTTP $status): ${body.take(200)}"

  /** A `fetch` init with the given headers + optional JSON body (sets Content-Type when a body is sent). */
  private[api] def fetchInit(
      method: String,
      headers: js.Dictionary[String],
      body: Option[String]
  ): dom.RequestInit =
    if body.isDefined then headers("Content-Type") = "application/json"
    val lit = js.Dynamic.literal(method = method, headers = headers.asInstanceOf[js.Any])
    body.foreach(b => lit.updateDynamic("body")(b))
    lit.asInstanceOf[dom.RequestInit]

/** The primary BYOK path: the visitor's own OpenRouter key — one key, any model, OpenAI-compatible. */
object OpenRouterByok extends ByokProvider:
  import ByokProvider.*

  private given ExecutionContext = JSExecutionContext.queue

  private val ApiBase = "https://openrouter.ai/api/v1"
  // Optional attribution headers OpenRouter uses for its rankings (harmless if unrecognised).
  private val Referer = "https://cortex.kakde.eu"
  private val Title   = "Cortex Tutor"

  /** OpenRouter's authenticated key-info endpoint — 2xx means the key is accepted. */
  def probe(key: String): Future[Boolean] =
    dom.fetch(s"$ApiBase/key", requestInit("GET", key, None)).toFuture.map(_.ok)

  def runTurn(key: String, bundle: PromptBundle, userMessage: String): Future[Turn] =
    // OpenAI shape: the system prompt is the first message (no top-level `system`).
    val system = Json.obj("role" -> Json.fromString("system"), "content" -> Json.fromString(bundle.system))
    val history = bundle.messages.map { m =>
      Json.obj(
        "role"    -> Json.fromString(if m.role == Role.Coach then "assistant" else "user"),
        "content" -> Json.fromString(m.content)
      )
    }
    val user     = Json.obj("role" -> Json.fromString("user"), "content" -> Json.fromString(userMessage))
    val messages = (system +: history) :+ user
    val body = Json.obj(
      "model"       -> Json.fromString(bundle.model),
      "max_tokens"  -> Json.fromInt(1024),
      "temperature" -> Json.fromInt(0), // the gate half must be deterministic, same as the server gate
      "messages"    -> Json.arr(messages*),
      "tools" -> Json.arr(
        Json.obj(
          "type" -> Json.fromString("function"),
          "function" -> Json.obj(
            "name"        -> Json.fromString(ToolName),
            "description" -> Json.fromString(ToolDescription),
            "parameters"  -> verdictSchema
          )
        )
      ),
      "tool_choice" -> Json.obj(
        "type"     -> Json.fromString("function"),
        "function" -> Json.obj("name" -> Json.fromString(ToolName))
      )
    )
    dom.fetch(s"$ApiBase/chat/completions", requestInit("POST", key, Some(body.noSpaces))).toFuture.flatMap {
      res =>
        res.text().toFuture.flatMap { txt =>
          if !res.ok then Future.failed(RuntimeException(providerError("OpenRouter", res.status, txt)))
          else
            parseTurn(txt) match
              case Right(turn) => Future.successful(turn)
              case Left(err)   => Future.failed(RuntimeException(s"Unexpected OpenRouter response: $err"))
        }
    }

  /** Pull the forced function-call `arguments` (a JSON string) out of a chat-completions response. */
  private def parseTurn(responseText: String): Either[String, Turn] =
    for
      json <- parse(responseText).left.map(_.getMessage)
      args <- json.hcursor
        .downField("choices")
        .downArray
        .downField("message")
        .downField("tool_calls")
        .downArray
        .downField("function")
        .get[String]("arguments")
        .left
        .map(_ => "no function tool_call in the response")
      input <- parse(args).left.map(e => s"undecodable tool arguments: ${e.getMessage}")
      turn  <- turnFromToolInput(input)
    yield turn

  /** `fetch` init for a direct browser→OpenRouter call (Bearer auth; OpenRouter sends permissive CORS). */
  private def requestInit(method: String, key: String, body: Option[String]): dom.RequestInit =
    fetchInit(
      method,
      js.Dictionary[String]("Authorization" -> s"Bearer $key", "HTTP-Referer" -> Referer, "X-Title" -> Title),
      body
    )

/** The purist BYOK path: the visitor's own Anthropic key, called directly at `api.anthropic.com`. */
object AnthropicByok extends ByokProvider:
  import ByokProvider.*

  private given ExecutionContext = JSExecutionContext.queue

  private val ApiBase = "https://api.anthropic.com"

  /** Cheap key validation: list models with the candidate key — 2xx means Anthropic accepts it. */
  def probe(key: String): Future[Boolean] =
    dom.fetch(s"$ApiBase/v1/models", requestInit("GET", key, None)).toFuture.map(_.ok)

  def runTurn(key: String, bundle: PromptBundle, userMessage: String): Future[Turn] =
    val messages =
      bundle.messages.map { m =>
        Json.obj(
          "role"    -> Json.fromString(if m.role == Role.Coach then "assistant" else "user"),
          "content" -> Json.fromString(m.content)
        )
      } :+ Json.obj("role" -> Json.fromString("user"), "content" -> Json.fromString(userMessage))
    val body = Json.obj(
      "model"       -> Json.fromString(bundle.model),
      "max_tokens"  -> Json.fromInt(1024),
      "temperature" -> Json.fromInt(0), // the gate half must be deterministic, same as the server gate
      "system"      -> Json.fromString(bundle.system),
      "messages"    -> Json.arr(messages*),
      "tools" -> Json.arr(
        Json.obj(
          "name"         -> Json.fromString(ToolName),
          "description"  -> Json.fromString(ToolDescription),
          "input_schema" -> verdictSchema
        )
      ),
      "tool_choice" -> Json.obj("type" -> Json.fromString("tool"), "name" -> Json.fromString(ToolName))
    )
    dom.fetch(s"$ApiBase/v1/messages", requestInit("POST", key, Some(body.noSpaces))).toFuture.flatMap {
      res =>
        res.text().toFuture.flatMap { txt =>
          if !res.ok then Future.failed(RuntimeException(providerError("Anthropic", res.status, txt)))
          else
            parseTurn(txt) match
              case Right(turn) => Future.successful(turn)
              case Left(err)   => Future.failed(RuntimeException(s"Unexpected Anthropic response: $err"))
        }
    }

  /** Pull the forced `tool_use` block out of a Messages-API response and split verdict / reply. */
  private def parseTurn(responseText: String): Either[String, Turn] =
    for
      json   <- parse(responseText).left.map(_.getMessage)
      blocks <- json.hcursor.downField("content").as[List[Json]].left.map(_.getMessage)
      input <- blocks
        .find(_.hcursor.get[String]("type").contains("tool_use"))
        .flatMap(_.hcursor.downField("input").focus)
        .toRight("no tool_use block in the response")
      turn <- turnFromToolInput(input)
    yield turn

  /** `fetch` init for a direct browser→Anthropic call (the dangerous-direct header opts into CORS). */
  private def requestInit(method: String, key: String, body: Option[String]): dom.RequestInit =
    fetchInit(
      method,
      js.Dictionary[String](
        "x-api-key"                                 -> key,
        "anthropic-version"                         -> "2023-06-01",
        "anthropic-dangerous-direct-browser-access" -> "true"
      ),
      body
    )
