package cortex.client.api

import cortex.client.auth.AuthStore
import cortex.shared.api.Endpoints
import cortex.shared.api.Endpoints.{
  AuthConfig,
  BlogIndex,
  BlogPostPayload,
  ChapterPayload,
  CortexIndex,
  DeleteCoachResponse,
  DeleteSubmissionsResponse,
  Greeting,
  ListSavedCoachResponse,
  ListSubmissionsResponse,
  RecentCalls,
  RunRequest,
  RunResponse,
  SaveCoachRequest,
  SaveCoachResponse,
  SubmitRequest,
  SubmitResponse,
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

  // /api/submissions (POST) is secured the same way.
  private val submitRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.submitSolution, baseUri)

  // /api/submissions (GET) + /api/submissions/{id} (DELETE) — both bearer-secured.
  private val listSubmissionsRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.listMySubmissions, baseUri)

  private val deleteOneSubmissionRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.deleteOneSubmission, baseUri)

  // /api/submissions (DELETE, no id) — wipe ALL the caller's submissions (account "delete all data").
  private val deleteAllSubmissionsRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.deleteMySubmissions, baseUri)

  private val saveCoachRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.saveCoachSession, baseUri)

  private val listSavedCoachRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.listMySavedCoach, baseUri)

  private val deleteOneSavedCoachRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.deleteOneSavedCoach, baseUri)

  private val deleteAllSavedCoachRequestFn =
    SttpClientInterpreter().toSecureRequestThrowDecodeFailures(Endpoints.deleteMySavedCoach, baseUri)

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

  /**
   * Submit a solution against ALL of a problem's test cases (server-side) and save it. Requires a signed-in
   * user; the server additionally allow-lists who may save. Error messages keep the server's `hint` too — for
   * a 403 that's the request-access instructions, which the workbench surfaces verbatim.
   */
  def submitSolution(token: String, req: SubmitRequest): Future[SubmitResponse] =
    backend.send(submitRequestFn(token)(req)).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          val detail = error.detail.filter(_.nonEmpty).map(d => s" $d").getOrElse("")
          val hint   = error.hint.filter(_.nonEmpty).map(h => s" $h").getOrElse("")
          Future.failed(RuntimeException(s"${error.error}.$detail$hint"))
    }

  /** List the signed-in user's stored submissions for one problem, newest first. */
  def listMySubmissions(token: String, book: String, chapter: String): Future[ListSubmissionsResponse] =
    backend.send(listSubmissionsRequestFn(token)((book, chapter))).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to load submissions")(res.code, error)))
    }

  /** Delete one of the signed-in user's own submissions (the Submissions tab trash). */
  def deleteOneSubmission(token: String, id: Long): Future[DeleteSubmissionsResponse] =
    backend.send(deleteOneSubmissionRequestFn(token)(id)).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to delete submission")(res.code, error)))
    }

  /** Delete ALL of the signed-in user's submissions (the account "delete all my data" path). */
  def deleteAllSubmissions(token: String): Future[DeleteSubmissionsResponse] =
    backend.send(deleteAllSubmissionsRequestFn(token)(())).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to delete submissions")(res.code, error)))
    }

  // ---- Coach-save (allow-listed) -------------------------------------------

  /**
   * Save a snapshot of the coach transcript. Requires a signed-in user; the server additionally allow-lists
   * who may save. Keeps the server's `hint` too — for a 403 that's the request-access instructions, which the
   * coach surfaces verbatim (mirrors `submitSolution`).
   */
  def saveCoachSession(token: String, req: SaveCoachRequest): Future[SaveCoachResponse] =
    backend.send(saveCoachRequestFn(token)(req)).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          val detail = error.detail.filter(_.nonEmpty).map(d => s" $d").getOrElse("")
          val hint   = error.hint.filter(_.nonEmpty).map(h => s" $h").getOrElse("")
          Future.failed(RuntimeException(s"${error.error}.$detail$hint"))
    }

  /** List the signed-in user's saved coach snapshots for one problem, newest first. */
  def listMySavedCoach(token: String, problemId: String): Future[ListSavedCoachResponse] =
    backend.send(listSavedCoachRequestFn(token)(problemId)).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to load saved coach sessions")(res.code, error)))
    }

  /** Delete one saved coach snapshot the user owns. */
  def deleteOneSavedCoach(token: String, id: Long): Future[DeleteCoachResponse] =
    backend.send(deleteOneSavedCoachRequestFn(token)(id)).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to delete saved coach session")(res.code, error)))
    }

  /** Delete ALL of the user's saved coach snapshots (self-service erasure). */
  def deleteAllSavedCoach(token: String): Future[DeleteCoachResponse] =
    backend.send(deleteAllSavedCoachRequestFn(token)(())).flatMap { res =>
      res.body match
        case Right(value) => Future.successful(value)
        case Left(error) =>
          Future.failed(RuntimeException(apiError("Failed to delete saved coach sessions")(res.code, error)))
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
