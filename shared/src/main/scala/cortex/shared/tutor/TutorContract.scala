package cortex.shared.tutor

import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}

/**
 * The Cortex Tutor HTTP contract, hand-ported from `cortex-tutor/api/tutor-openapi.yaml` (the single source
 * of truth, vendored alongside this repo). cortex's `OpenapiCodegenPlugin` is single-spec (`api/openapi.yaml`
 * → `Endpoints`), and the streaming `submitTurn` transport is hand-rolled regardless (native `EventSource`
 * can't set `Authorization`), so the small tutor client carries its own DTOs + circe codecs rather than
 * standing up a second codegen.
 *
 * Wire-shape contract:
 *   - every property is camelCase (matching the cortex API), so circe's field-name codecs map 1:1;
 *   - the string enums (`Step`/`Verdict`/`Role`/`SessionOrigin`/`SessionStatus`/`Tier`) carry an explicit
 *     `wire` value so the snake_case members (`off_topic`, `your_turn`, `problem_page`) round-trip exactly;
 *   - decoders ignore unknown fields, so a forward-compatible server addition won't break the client.
 *
 * The codecs intentionally don't lean on circe default-value injection (plain `circe-generic` does not honour
 * Scala defaults on decode) — every field decoded from the server is either required by the spec or modelled
 * as `Option`. The `= default` arguments are construction ergonomics only. (`Option` fields encode as `null`
 * when `None`, which the tutor accepts as "absent" — e.g. `SessionCreateRequest.model: null` ⇒ tier default.)
 */
object TutorContract:

  // ── string enums (exact wire values) ────────────────────────────────────────

  enum Step(val wire: String):
    case Clarify   extends Step("clarify")
    case Examples  extends Step("examples")
    case Approach  extends Step("approach")
    case Plan      extends Step("plan")
    case Implement extends Step("implement")
    case Test      extends Step("test")

  object Step:
    /** Canonical order — the six-step FSM, used to render the tracker and derive step index. */
    val ordered: List[Step]               = List(Clarify, Examples, Approach, Plan, Implement, Test)
    def fromWire(s: String): Option[Step] = ordered.find(_.wire == s)
    given Encoder[Step]                   = Encoder.encodeString.contramap(_.wire)
    given Decoder[Step] = Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown step: $s"))

  enum Verdict(val wire: String):
    case Pass     extends Verdict("pass")
    case Retry    extends Verdict("retry")
    case OffTopic extends Verdict("off_topic")
    case Question extends Verdict("question")

  object Verdict:
    private val all                          = List(Pass, Retry, OffTopic, Question)
    def fromWire(s: String): Option[Verdict] = all.find(_.wire == s)
    given Encoder[Verdict]                   = Encoder.encodeString.contramap(_.wire)
    given Decoder[Verdict] = Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown verdict: $s"))

  enum Role(val wire: String):
    case User  extends Role("user")
    case Coach extends Role("coach")

  object Role:
    private val all                       = List(User, Coach)
    def fromWire(s: String): Option[Role] = all.find(_.wire == s)
    given Encoder[Role]                   = Encoder.encodeString.contramap(_.wire)
    given Decoder[Role] = Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown role: $s"))

  enum SessionOrigin(val wire: String):
    case YourTurn    extends SessionOrigin("your_turn")
    case ProblemPage extends SessionOrigin("problem_page")

  object SessionOrigin:
    private val all                                = List(YourTurn, ProblemPage)
    def fromWire(s: String): Option[SessionOrigin] = all.find(_.wire == s)
    given Encoder[SessionOrigin]                   = Encoder.encodeString.contramap(_.wire)

    given Decoder[SessionOrigin] =
      Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown origin: $s"))

  enum SessionStatus(val wire: String):
    case Active    extends SessionStatus("active")
    case Completed extends SessionStatus("completed")

  object SessionStatus:
    private val all                                = List(Active, Completed)
    def fromWire(s: String): Option[SessionStatus] = all.find(_.wire == s)
    given Encoder[SessionStatus]                   = Encoder.encodeString.contramap(_.wire)

    given Decoder[SessionStatus] =
      Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown status: $s"))

  /**
   * The caller's coach tier (`GET /v1/whoami`). `homelab` runs the gate + coach on the server's Anthropic key
   * (turn via `submitTurn` SSE); `byok` is client-direct on the visitor's own key (`getPromptBundle` →
   * Anthropic → `recordByokTurn`). The tier is pinned onto a session at creation as [[CoachSession.byok]].
   */
  enum Tier(val wire: String):
    case Homelab extends Tier("homelab")
    case Byok    extends Tier("byok")

  object Tier:
    private val all                       = List(Homelab, Byok)
    def fromWire(s: String): Option[Tier] = all.find(_.wire == s)
    given Encoder[Tier]                   = Encoder.encodeString.contramap(_.wire)
    given Decoder[Tier] = Decoder.decodeString.emap(s => fromWire(s).toRight(s"unknown tier: $s"))

  // ── payloads (dependency order: leaves before composites) ───────────────────

  final case class ApiError(error: String, detail: Option[String] = None, hint: Option[String] = None)

  object ApiError:
    given Codec[ApiError] = deriveCodec

  /**
   * One selectable coach model from `GET /v1/whoami`'s `availableModels` (already tier-filtered server-side).
   * `key` is the stable catalog id sent as [[SessionCreateRequest.model]]; `provider` is the backend family
   * (e.g. `openrouter`, `anthropic`) — a BYOK key is required for cloud providers, not the local model (see
   * [[ModelPicker.requiresKey]]).
   */
  final case class ModelOption(key: String, display: String, provider: String):
    /** Anthropic-backed (Claude) models — the BYOK turn is client-direct on the user's Anthropic key. */
    def isAnthropic: Boolean = provider.equalsIgnoreCase("anthropic")

    /** OpenRouter-backed models (one key, any model) — the BYOK turn is client-direct on the user's key. */
    def isOpenRouter: Boolean = provider.equalsIgnoreCase("openrouter")

    /** A cloud BYOK provider needing the visitor's own key (vs the homelab local model, which is keyless). */
    def requiresUserKey: Boolean = isAnthropic || isOpenRouter

  object ModelOption:
    given Codec[ModelOption] = deriveCodec

  /**
   * `GET /v1/whoami` — the caller's identity, coach tier, and the models they may pick. `availableModels` is
   * already filtered to the tier (default-first); `defaultModel` is the catalog key used when
   * [[SessionCreateRequest.model]] is omitted.
   */
  final case class Whoami(
      sub: String,
      preferredUsername: String,
      tier: Tier,
      defaultModel: String,
      availableModels: List[ModelOption]
  )

  object Whoami:
    given Codec[Whoami] = deriveCodec

  final case class CoachMessage(role: Role, step: Step, content: String, createdAtEpochMs: Long)

  object CoachMessage:
    given Codec[CoachMessage] = deriveCodec

  final case class StepScore(step: Step, score: Int)

  object StepScore:
    given Codec[StepScore] = deriveCodec

  final case class GateVerdict(
      verdict: Verdict,
      score: Int = 0,
      rubricHits: List[String] = Nil,
      missing: List[String] = Nil,
      hint: String = "",
      nextHintLevel: Int = 0
  )

  object GateVerdict:
    given Encoder[GateVerdict] = deriveEncoder

    // Lenient decode: only `verdict` is required; the rest fall back to their Scala defaults when a BYOK
    // provider omits them (e.g. Gemini returns only the tool's required fields). Plain circe-generic does
    // not inject Scala defaults on decode, so the fallbacks are spelled out. Missing *or* null both default.
    given Decoder[GateVerdict] = Decoder.instance { c =>
      for
        verdict       <- c.get[Verdict]("verdict")
        score         <- c.get[Option[Int]]("score").map(_.getOrElse(0))
        rubricHits    <- c.get[Option[List[String]]]("rubricHits").map(_.getOrElse(Nil))
        missing       <- c.get[Option[List[String]]]("missing").map(_.getOrElse(Nil))
        hint          <- c.get[Option[String]]("hint").map(_.getOrElse(""))
        nextHintLevel <- c.get[Option[Int]]("nextHintLevel").map(_.getOrElse(0))
      yield GateVerdict(verdict, score, rubricHits, missing, hint, nextHintLevel)
    }

  final case class CoachSession(
      sessionId: String,
      problemId: String,
      origin: SessionOrigin,
      status: SessionStatus,
      currentStep: Step,
      stepIndex: Int,
      completed: Boolean,
      messages: List[CoachMessage],
      scores: List[StepScore],
      rubricVersion: String,
      /**
       * Tier, pinned at creation (server-authoritative): `Some(true)` = BYOK (turn client-direct via
       * `getPromptBundle` + `recordByokTurn`; `submitTurn` answers 403), else homelab. `Option` for
       * decode-compatibility with pre-tier payloads.
       */
      byok: Option[Boolean] = None,
      /**
       * The session's chosen coach model, by stable catalog key (`None` on pre-selection sessions → the tier
       * default applies). Echoes [[SessionCreateRequest.model]]; pinned at creation (`reset` to change).
       */
      model: Option[String] = None
  )

  object CoachSession:
    given Codec[CoachSession] = deriveCodec

  final case class SessionCreateRequest(
      problemId: String,
      origin: SessionOrigin = SessionOrigin.YourTurn,
      /**
       * Optional coach model, by stable catalog key (see `GET /v1/whoami` → `availableModels`). `None` ⇒ the
       * tier default. Validated server-side against the caller's tier allow-list (HTTP 422 if
       * unknown/disallowed) and pinned at creation — ignored on resume; `reset` to change it.
       */
      model: Option[String] = None
  )

  object SessionCreateRequest:
    given Codec[SessionCreateRequest] = deriveCodec

  /** Re-point an active session's coach model (dual-mode switch). POST `/v1/sessions/{id}/model`. */
  final case class ModelChangeRequest(model: String)

  object ModelChangeRequest:
    given Codec[ModelChangeRequest] = deriveCodec

  /** Result of the global "clear all my coach chats" — how many sessions were deleted. */
  final case class ClearAllResult(deleted: Int)

  object ClearAllResult:
    given Codec[ClearAllResult] = deriveCodec

  final case class TurnRequest(
      step: Step,
      text: String,
      turnId: Option[String] = None,
      code: Option[String] = None,
      language: Option[String] = None,
      runResult: Option[String] = None
  )

  object TurnRequest:
    given Codec[TurnRequest] = deriveCodec

  final case class TurnUsage(
      inputTokens: Option[Long] = None,
      outputTokens: Option[Long] = None,
      cacheReadTokens: Option[Long] = None,
      cacheWriteTokens: Option[Long] = None,
      costUsd: Option[Double] = None
  )

  object TurnUsage:
    given Codec[TurnUsage] = deriveCodec

  final case class TurnResult(
      sessionId: String,
      step: Step,
      stepIndex: Int,
      verdict: Verdict,
      advanced: Boolean,
      completed: Boolean,
      reply: CoachMessage,
      score: Option[Int] = None,
      hint: Option[String] = None,
      usage: Option[TurnUsage] = None
  )

  object TurnResult:
    given Codec[TurnResult] = deriveCodec

  final case class PromptBundle(step: Step, system: String, messages: List[CoachMessage], model: String)

  object PromptBundle:
    given Codec[PromptBundle] = deriveCodec

  final case class ByokRecordRequest(
      step: Step,
      text: String,
      coachReply: String,
      verdict: GateVerdict,
      turnId: Option[String] = None,
      code: Option[String] = None,
      language: Option[String] = None,
      runResult: Option[String] = None
  )

  object ByokRecordRequest:
    given Codec[ByokRecordRequest] = deriveCodec

  // ── SSE events (union; transport hand-rolled, discriminator = JSON `type`) ───

  /**
   * One decoded SSE frame from `submitTurn`. The stream emits, in order: a single `State` (the
   * post-transition session, before the first token), zero or more `Token`s, then a terminal `Done` carrying
   * the `TurnResult`. A coach failure server-side degrades to a gate-hint token + `Done`, so `Failed` is
   * reserved for an explicit `error` frame.
   */
  enum TurnEvent:
    case State(session: CoachSession)
    case Token(text: String)
    case Done(result: TurnResult)
    case Failed(error: ApiError)

  object TurnEvent:

    /** Decode one parsed SSE `data:` payload, dispatching on the `type` discriminator. */
    def fromJson(json: Json): Decoder.Result[TurnEvent] =
      val c = json.hcursor
      c.get[String]("type").flatMap {
        case "state" => c.get[CoachSession]("session").map(State.apply)
        case "token" => c.get[String]("text").map(Token.apply)
        case "done"  => c.get[TurnResult]("result").map(Done.apply)
        case "error" => c.get[ApiError]("error").map(Failed.apply)
        case other   => Left(DecodingFailure(s"unknown turn event type: $other", c.history))
      }
