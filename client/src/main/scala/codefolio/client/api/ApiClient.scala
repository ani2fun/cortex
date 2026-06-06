package codefolio.client.api

import codefolio.client.auth.AuthStore
import codefolio.shared.api.Endpoints
import codefolio.shared.api.Endpoints.{
  AuthConfig,
  BlogIndex,
  BlogPostPayload,
  ChapterPayload,
  CortexIndex,
  Greeting,
  RecentCalls,
  RunRequest,
  RunResponse,
  UserInfo
}
import sttp.client3.{FetchBackend, SttpBackend}
import sttp.model.{StatusCode, Uri}
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext

/**
 * Typed HTTP client built **from the same tapir endpoints** the server implements. Single source of truth =
 * `api/openapi.yaml` → codegen → `Endpoints.scala` → consumed here.
 *
 * Every endpoint goes through [[callable]]: it turns a generated `Endpoints.*` value into an `I => Future[O]`
 * function — building the sttp request once, running it through the browser `fetch`, and turning a non-2xx
 * `Left` into a failed `Future` with a human-readable message. Adding an endpoint is one `callable(...)` line
 * plus a one-line public method; the request-build / send / error-map plumbing is not repeated per endpoint.
 *
 * The base URI is intentionally `None` (relative URLs). See the comment on `baseUri` below — `Some(uri"")`
 * would crash at runtime.
 */
object ApiClient:

  private given ExecutionContext = JSExecutionContext.queue

  private val backend: SttpBackend[Future, Any] = FetchBackend()
  // Same-origin: emit relative URIs (`/api/hello`) and let the browser's
  // FetchBackend resolve them against `window.location.origin`. In dev the
  // Vite proxy forwards `/api/*` to :8080; in prod the same server serves
  // both API and statics. (sttp's `uri""` interpolator rejects the empty
  // string at runtime, so we can't use it as a placeholder base.)
  private val baseUri: Option[Uri] = None

  /**
   * Turn a generated tapir endpoint into a callable `I => Future[O]`.
   *
   *   - `formatter` is the error-message style for this endpoint's error type `E` — `statusOnly` for
   *     endpoints whose only failure signal is the status code, `apiError` for those that return an
   *     `ApiError` envelope.
   *   - `messageFor` derives the human-readable prefix from the request input, so input-specific endpoints (a
   *     chapter, a blog post) can name what they were fetching.
   *
   * The sttp request is built once per endpoint (here), then reused for every call.
   */
  private def callable[I, E, O](
      endpoint: PublicEndpoint[I, E, O, Any],
      formatter: String => (StatusCode, E) => String
  )(messageFor: I => String): I => Future[O] =
    val request = SttpClientInterpreter().toRequestThrowDecodeFailures(endpoint, baseUri)
    input =>
      val describe = formatter(messageFor(input))
      backend.send(request(input)).flatMap { res =>
        res.body match
          case Right(value) => Future.successful(value)
          case Left(error)  => Future.failed(RuntimeException(describe(res.code, error)))
      }

  private def statusOnly(message: String): (StatusCode, Unit) => String =
    (code, _) => s"$message (${code.code})"

  private def apiError(message: String): (StatusCode, Endpoints.ApiError) => String =
    (code, error) =>
      val detail = error.detail.filter(_.nonEmpty).map(d => s": $d").getOrElse("")
      s"$message (${code.code}): ${error.error}$detail"

  private val helloCall  = callable(Endpoints.getHello, statusOnly)(_ => "Failed to fetch greeting")
  private val recentCall = callable(Endpoints.getRecent, statusOnly)(_ => "Failed to fetch recent calls")

  private val authConfigCall =
    callable(Endpoints.getAuthConfig, statusOnly)(_ => "Failed to fetch auth config")

  // /api/run is built directly (not via `callable`) so an `Authorization: Bearer` header can be attached
  // per-call when the user is signed in. The server treats the header as optional — anonymous callers just
  // omit it and fall into the per-IP rate-limit bucket.
  private val runRequestFn =
    SttpClientInterpreter().toRequestThrowDecodeFailures(Endpoints.runCode, baseUri)

  // /api/me is a secured endpoint — the bearer token is its security input.
  private val meRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.getMe, baseUri)

  private val cortexIndexCall =
    callable(Endpoints.getCortexIndex, apiError)(_ => "Failed to fetch Cortex index")

  private val cortexChapterCall =
    callable(Endpoints.getCortexChapter, apiError) { case (book, chapter) =>
      s"Failed to fetch chapter $book/$chapter"
    }

  private val blogIndexCall =
    callable(Endpoints.getBlogIndex, apiError)(_ => "Failed to fetch blog index")

  private val blogPostCall =
    callable(Endpoints.getBlogPost, apiError)(slug => s"Failed to fetch blog post $slug")

  // ---- Hello demo ----------------------------------------------------------

  def getHello: Future[Greeting] = helloCall(())

  def getRecent: Future[RecentCalls] = recentCall(())

  // ---- Code execution ------------------------------------------------------

  /**
   * Execute a snippet. When the visitor is signed in, the access token rides along as a Bearer header so the
   * server meters the call against the per-user (rather than per-IP) rate-limit bucket.
   */
  def runCode(req: RunRequest): Future[RunResponse] =
    val base = runRequestFn(req)
    val request = AuthStore.current.status match
      case AuthStore.Status.Authed(_, token) => base.header("Authorization", s"Bearer $token")
      case _                                 => base
    backend.send(request).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error)  => Future.failed(RuntimeException(apiError("Run failed")(res.code, error)))
    }

  // ---- Auth ----------------------------------------------------------------

  /** Public boot-time config — tells the SPA whether (and how) to initialise keycloak-js. */
  def getAuthConfig: Future[AuthConfig] = authConfigCall(())

  /** Identity claims + current run quota for the bearer-token holder. Requires a valid token. */
  def getMe(token: String): Future[UserInfo] =
    backend.send(meRequestFn(token)(())).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to fetch /api/me")(res.code, error)))
    }

  // ---- Cortex --------------------------------------------------------------

  def getCortexIndex: Future[CortexIndex] = cortexIndexCall(())

  def getCortexChapter(book: String, chapter: String): Future[ChapterPayload] =
    cortexChapterCall((book, chapter))

  // ---- Blog ----------------------------------------------------------------

  def getBlogIndex: Future[BlogIndex] = blogIndexCall(())

  def getBlogPost(slug: String): Future[BlogPostPayload] = blogPostCall(slug)
