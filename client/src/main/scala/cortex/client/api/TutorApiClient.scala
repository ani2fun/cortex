package cortex.client.api

import cortex.client.auth.AuthStore
import cortex.shared.tutor.TutorContract.*
import io.circe.parser.parse
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

/**
 * Hand-written HTTP client for the Cortex Tutor service. Unlike [[ApiClient]] (same-origin, relative URLs,
 * tapir-generated), the tutor lives at a **different origin** (`tutorBaseUrl`, surfaced from
 * `/api/auth/config` via [[AuthStore.tutorBaseUrl]]) and its `submitTurn` streams over SSE — so this client
 * speaks `fetch` directly: one HTTP mechanism for both the plain JSON calls and the stream.
 *
 *   - Non-streaming calls ([[whoami]], [[createSession]], [[getSession]], [[resetSession]],
 *     [[getPromptBundle]], [[recordByokTurn]]) return a `Future`, failing with a [[TutorHttpError]] (carrying
 *     the status) on non-2xx so callers can branch (401 expiry, 403 byok_required, 422 bad model).
 *   - [[submitTurn]] streams the coach reply over `fetch` + a `ReadableStream` reader (native `EventSource`
 *     can't attach `Authorization`), parsing `state → token… → done` SSE frames and pushing them to
 *     callbacks. It returns a [[TurnStream]] handle whose `cancel()` aborts the turn.
 *
 * When `tutorBaseUrl` is absent (config unreachable, or the server didn't report one) every call fails fast —
 * the coach UI degrades to its fallback.
 */
object TutorApiClient:

  private given ExecutionContext = JSExecutionContext.queue

  /**
   * A non-2xx from a non-streaming call, carrying the status so callers can branch (e.g. 401 vs 403 vs 422).
   */
  final class TutorHttpError(val status: Int, message: String) extends RuntimeException(message)

  /** Terminal failure surfaced to `submitTurn`'s `onError`; the controller degrades on it. */
  enum TurnFailure:
    /** HTTP 409 — another tab/device advanced the FSM first; body carries the authoritative state. */
    case Stale(session: CoachSession)

    /** Any other failure (unconfigured, transport, non-2xx, malformed frame). */
    case Message(text: String)

  /** Handle to an in-flight streamed turn. */
  final class TurnStream private[TutorApiClient] (private val controller: dom.AbortController):
    /** Abort the turn (e.g. on component unmount or a fresh submit). */
    def cancel(): Unit = controller.abort()

  // ── non-streaming calls ─────────────────────────────────────────────────────

  /** The caller's identity, tier, and selectable coach models — drives the model picker on coach entry. */
  def whoami(): Future[Whoami] =
    getJson[Whoami]("/v1/whoami")

  def createSession(
      problemId: String,
      origin: SessionOrigin = SessionOrigin.YourTurn,
      model: Option[String] = None
  ): Future[CoachSession] =
    postJson[SessionCreateRequest, CoachSession](
      "/v1/sessions",
      SessionCreateRequest(problemId, origin, model)
    )

  def getSession(sessionId: String): Future[CoachSession] =
    getJson[CoachSession](s"/v1/sessions/${enc(sessionId)}")

  /** The caller's in-progress session for a problem (so a refresh restores it), or `None` (204/404). */
  def getActiveSession(problemId: String): Future[Option[CoachSession]] =
    sendOpt[CoachSession](s"/v1/sessions/active?problemId=${enc(problemId)}")

  def resetSession(sessionId: String): Future[CoachSession] =
    sendExpect[CoachSession](s"/v1/sessions/${enc(sessionId)}/reset", "POST", None)

  /** Re-point an active session's coach model (dual-mode switch); returns the updated session. */
  def changeModel(sessionId: String, model: String): Future[CoachSession] =
    postJson[ModelChangeRequest, CoachSession](
      s"/v1/sessions/${enc(sessionId)}/model",
      ModelChangeRequest(model)
    )

  /** Permanently delete ALL of the caller's coach sessions + messages. */
  def clearAllSessions(): Future[ClearAllResult] =
    sendExpect[ClearAllResult]("/v1/sessions/clear-all", "POST", Some("{}"))

  def getPromptBundle(sessionId: String, step: Step): Future[PromptBundle] =
    getJson[PromptBundle](s"/v1/sessions/${enc(sessionId)}/prompt-bundle?step=${step.wire}")

  def recordByokTurn(sessionId: String, req: ByokRecordRequest): Future[TurnResult] =
    postJson[ByokRecordRequest, TurnResult](s"/v1/sessions/${enc(sessionId)}/turns/byok-record", req)

  // ── streamed turn (SSE over fetch) ───────────────────────────────────────────

  def submitTurn(
      sessionId: String,
      req: TurnRequest,
      onState: CoachSession => Unit,
      onToken: String => Unit,
      onDone: TurnResult => Unit,
      onError: TurnFailure => Unit
  ): TurnStream =
    val controller = new dom.AbortController()
    base match
      case None =>
        onError(TurnFailure.Message("The tutor is not configured (no tutorBaseUrl)."))
      case Some(b) =>
        val init = requestInit("POST", Some(req.asJson.noSpaces), controller.signal)
        dom.fetch(b + s"/v1/sessions/${enc(sessionId)}/turns", init).toFuture.onComplete {
          case Success(res) => routeTurnResponse(res, onState, onToken, onDone, onError)
          case Failure(e)   => onError(TurnFailure.Message(s"The tutor is unreachable (${e.getMessage})."))
        }
    new TurnStream(controller)

  private def routeTurnResponse(
      res: dom.Response,
      onState: CoachSession => Unit,
      onToken: String => Unit,
      onDone: TurnResult => Unit,
      onError: TurnFailure => Unit
  ): Unit =
    val contentType = Option(res.headers.get("content-type")).getOrElse("")
    if res.status == 409 then
      res.text().toFuture.foreach { txt =>
        parse(txt).flatMap(_.as[CoachSession]) match
          case Right(session) => onError(TurnFailure.Stale(session))
          case Left(_)        => onError(TurnFailure.Message("Another session advanced first."))
      }
    else if !res.ok then
      res.text().toFuture.foreach(txt => onError(TurnFailure.Message(errorMessage(txt, res.status))))
    else if contentType.contains("text/event-stream") then
      streamSse(res, onState, onToken, onDone, onError)
    else
      // 200 but not a stream — the spec also models a plain `TurnResult` body; accept it.
      res.text().toFuture.foreach { txt =>
        parse(txt).flatMap(_.as[TurnResult]) match
          case Right(result) => onDone(result)
          case Left(_)       => onError(TurnFailure.Message("Unexpected non-stream response from the tutor."))
      }

  /** Read the `ReadableStream`, decode UTF-8 chunks, split on blank lines, dispatch each SSE frame. */
  private def streamSse(
      res: dom.Response,
      onState: CoachSession => Unit,
      onToken: String => Unit,
      onDone: TurnResult => Unit,
      onError: TurnFailure => Unit
  ): Unit =
    val reader     = res.body.asInstanceOf[js.Dynamic].getReader().asInstanceOf[StreamReader]
    val decoderDyn = js.Dynamic.newInstance(js.Dynamic.global.TextDecoder)("utf-8")
    var buffer     = ""
    var doneSeen   = false

    def dispatch(frame: String): Unit =
      val payload = frame.linesIterator
        .filter(_.startsWith("data:"))
        .map(_.stripPrefix("data:").stripPrefix(" "))
        .mkString("\n")
      if payload.nonEmpty then
        parse(payload).flatMap(TurnEvent.fromJson) match
          case Right(TurnEvent.State(session)) => onState(session)
          case Right(TurnEvent.Token(text))    => onToken(text)
          case Right(TurnEvent.Done(result))   => doneSeen = true; onDone(result)
          case Right(TurnEvent.Failed(err))    => doneSeen = true; onError(TurnFailure.Message(err.error))
          case Left(_)                         => () // ping / comment / unmodelled frame — ignore

    def drainFrames(): Unit =
      var sep = buffer.indexOf("\n\n")
      while sep >= 0 do
        dispatch(buffer.substring(0, sep))
        buffer = buffer.substring(sep + 2)
        sep = buffer.indexOf("\n\n")

    def pump(): Unit =
      reader.read().toFuture.onComplete {
        case Success(chunk) =>
          if chunk.done then
            if buffer.trim.nonEmpty then dispatch(buffer) // flush an unterminated trailing frame
            if !doneSeen then onError(TurnFailure.Message("The tutor stream ended unexpectedly."))
          else
            // sse-starlette delimits with CRLF (event:…\r\ndata:…\r\n\r\n); normalise the whole buffer
            // to LF so the \n\n frame split + linesIterator work (and a \r\n split across chunks is fine).
            val decoded =
              decoderDyn.decode(chunk.value, js.Dynamic.literal(stream = true)).asInstanceOf[String]
            buffer = (buffer + decoded).replace("\r\n", "\n").replace("\r", "\n")
            drainFrames()
            pump()
        case Failure(e) =>
          onError(TurnFailure.Message(s"The tutor stream failed (${e.getMessage})."))
      }

    pump()

  // ── plumbing ─────────────────────────────────────────────────────────────────

  private def base: Option[String] = AuthStore.tutorBaseUrl.map(_.stripSuffix("/"))

  private def getJson[O: Decoder](path: String): Future[O] =
    sendExpect[O](path, "GET", None)

  /** Like [[getJson]] but a 204/404 (no such resource) → `None` rather than a failure. */
  private def sendOpt[O: Decoder](path: String): Future[Option[O]] =
    base match
      case None => Future.failed(RuntimeException("The tutor is not configured (no tutorBaseUrl)."))
      case Some(b) =>
        dom.fetch(b + path, requestInit("GET", None, js.undefined)).toFuture.flatMap { res =>
          if res.status == 204 || res.status == 404 then Future.successful(None)
          else
            res.text().toFuture.flatMap { txt =>
              if res.ok then
                parse(txt).flatMap(_.as[O]) match
                  case Right(value) => Future.successful(Some(value))
                  case Left(err) =>
                    Future.failed(RuntimeException(s"Tutor: malformed response (${err.getMessage})"))
              else Future.failed(new TutorHttpError(res.status, errorMessage(txt, res.status)))
            }
        }

  private def postJson[I: Encoder, O: Decoder](path: String, body: I): Future[O] =
    sendExpect[O](path, "POST", Some(body.asJson.noSpaces))

  private def sendExpect[O: Decoder](path: String, method: String, body: Option[String]): Future[O] =
    base match
      case None => Future.failed(RuntimeException("The tutor is not configured (no tutorBaseUrl)."))
      case Some(b) =>
        dom.fetch(b + path, requestInit(method, body, js.undefined)).toFuture.flatMap { res =>
          res.text().toFuture.flatMap { txt =>
            if res.ok then
              parse(txt).flatMap(_.as[O]) match
                case Right(value) => Future.successful(value)
                case Left(err) =>
                  Future.failed(RuntimeException(s"Tutor: malformed response (${err.getMessage})"))
            else Future.failed(new TutorHttpError(res.status, errorMessage(txt, res.status)))
          }
        }

  /**
   * Build a `fetch` init: JSON content-type when there's a body, bearer when signed in, optional abort
   * signal.
   */
  private def requestInit(
      method: String,
      body: Option[String],
      signal: js.UndefOr[dom.AbortSignal]
  ): dom.RequestInit =
    val headers = js.Dictionary[String]()
    AuthStore.current.status match
      case AuthStore.Status.Authed(_, token) => headers("Authorization") = s"Bearer $token"
      case _                                 => ()
    if body.isDefined then headers("Content-Type") = "application/json"
    val lit = js.Dynamic.literal(method = method, headers = headers.asInstanceOf[js.Any])
    body.foreach(b => lit.updateDynamic("body")(b))
    signal.foreach(s => lit.updateDynamic("signal")(s.asInstanceOf[js.Any]))
    lit.asInstanceOf[dom.RequestInit]

  /** Prefer the tutor's `ApiError.error`, falling back to the status code. */
  private def errorMessage(body: String, status: Int): String =
    parse(body).flatMap(_.as[ApiError]).toOption
      .map(e => e.detail.filter(_.nonEmpty).fold(e.error)(d => s"${e.error}: $d"))
      .getOrElse(s"Tutor request failed (HTTP $status)")

  private def enc(segment: String): String = js.URIUtils.encodeURIComponent(segment)

  // ── minimal ReadableStream facades (the typed scala-js-dom surface is awkward across versions) ──

  @js.native
  private trait StreamReader extends js.Object:
    def read(): js.Promise[StreamChunk] = js.native

  @js.native
  private trait StreamChunk extends js.Object:
    val done: Boolean = js.native
    val value: js.Any = js.native
