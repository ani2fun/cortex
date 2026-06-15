package cortex.server.codeRunPipeline

import cortex.server.codeRunPipeline.Languages.Language
import cortex.server.config.RunnerConfig
import cortex.shared.api.Endpoints.{RunRequest, RunResponse, RunResult}
import zio.*

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets

/**
 * Errors that bubble out of `/api/run`. The HTTP layer (`ApiErrors.toHttp`) maps each variant to its proper
 * status code + envelope.
 */
sealed trait RunFailure extends Product with Serializable

object RunFailure:
  /** Bad request (unknown language, missing field). 400. */
  final case class BadInput(error: String, hint: Option[String] = None) extends RunFailure

  /** Payload too large (source > 64 KiB or stdin > 16 KiB). 413. */
  final case class PayloadTooLarge(error: String) extends RunFailure

  /** No execution backend configured. 503. */
  case object NotConfigured extends RunFailure

  /** The execution backend (go-judge) returned a non-2xx status or the request failed. 502. */
  final case class BackendFailure(error: String, detail: Option[String] = None) extends RunFailure

/**
 * Internal seam for `/api/run`. CONTEXT.md term: **Code Execution Backend**. One adapter in production —
 * go-judge (self-hosted sandbox) — plus the test fake `FakeGoJudge`. Production wires it via
 * [[CodeRunPipeline.live]]; tests inject fakes via [[CodeRunPipeline.from]].
 *
 * The pipeline resolves the language alias, then runs it on the first configured backend whose
 * `supports(lang)` is true. The single go-judge backend supports every language in [[Languages]] (`supports =
 * true`). The list shape is kept so tests can inject fakes and a second backend could be reintroduced without
 * reshaping the seam.
 */
private[codeRunPipeline] trait CodeExecutionBackend:
  def supports(lang: Language): Boolean
  def run(source: String, stdin: Option[String], lang: Language): Task[RunResult]

/**
 * Single-backend pipeline for `/api/run`.
 *
 *   - Validates payload size, resolves the language alias.
 *   - Runs the language on the first configured backend that supports it.
 *   - Wraps backend `Throwable`s as `RunFailure.BackendFailure`.
 *
 * Mirrors the ADR-0003 internal-seams pattern: the [[CodeExecutionBackend]] trait is package-private; the
 * only public surface is `run`. Wire-format mapping (JSON ↔ `RunResult`) lives in [[GoJudgeWire]] so it can
 * be unit-tested directly.
 */
trait CodeRunPipeline:
  def run(req: RunRequest): IO[RunFailure, RunResponse]

object CodeRunPipeline:

  /**
   * Direct construction from explicit backends in priority order. Used by tests; production wires via
   * [[live]].
   */
  def from(backends: List[CodeExecutionBackend]): CodeRunPipeline =
    CodeRunPipelineLive(backends)

  /**
   * Resource-free layer: builds the go-judge adapter from `RunnerConfig`. If `executorUrl` is unset, the
   * pipeline returns `NotConfigured` per request (the misconfiguration is made visible rather than silently
   * swallowed). There is no runtime failover — a single backend, by design.
   */
  /**
   * Global cap on CONCURRENT code executions — bounds go-judge fan-out so an authenticated user firing many
   * runs at once can't exhaust the exec node's memory (the per-hour rate limit caps the RATE, not
   * concurrency). Excess runs queue for a permit. Tune here.
   */
  private val MaxConcurrentRuns = 8L

  val live: ZLayer[RunnerConfig, Nothing, CodeRunPipeline] =
    ZLayer {
      for
        cfg  <- ZIO.service[RunnerConfig]
        gate <- Semaphore.make(MaxConcurrentRuns)
      yield CodeRunPipelineLive(liveBackends(cfg), Some(gate))
    }

  // ---------------------------------------------------------------------------
  // Live adapter — a thin shell wrapping GoJudgeWire's JSON ↔ RunResult mapping
  // in the `postJson` HTTP transport. The wire layer owns the mapping so it can
  // be unit-tested against golden fixtures without an HTTP server.
  // ---------------------------------------------------------------------------

  private def liveBackends(cfg: RunnerConfig): List[CodeExecutionBackend] =
    cfg.executorUrl
      .filter(_.nonEmpty)
      .map(url => LiveGoJudgeBackend(url, cfg.executorAuthToken.filter(_.nonEmpty)))
      .toList

  final private class LiveGoJudgeBackend(baseUrl: String, authToken: Option[String])
      extends CodeExecutionBackend:

    override def supports(lang: Language): Boolean = true

    override def run(
        source: String,
        stdin: Option[String],
        lang: Language
    ): Task[RunResult] =
      val body = GoJudgeWire.buildRequestBody(source, stdin, lang)
      // go-judge uses bearer-token auth (ES_AUTH_TOKEN). Optional — when unset the in-cluster
      // NetworkPolicy is the access control and no header is sent.
      val extraHeaders = authToken.fold(Map.empty[String, String])(t => Map("Authorization" -> s"Bearer $t"))
      postJson(
        baseUrl,
        GoJudgeWire.Path,
        body,
        GoJudgeWire.parseRunResult(lang.goJudge.compile.isDefined),
        "go-judge",
        extraHeaders
      )

  // ---- Shared HTTP plumbing ----------------------------------------------

  // Force HTTP/1.1: go-judge serves plaintext HTTP/1.1. Java HttpClient's
  // default (HTTP_2) sends `Connection: Upgrade, HTTP2-Settings: …` h2c
  // discovery headers on every plaintext POST, which some servers reject with
  // a bare-text 400 before the handler sees it. Pinning HTTP/1.1 sidesteps the
  // h2c handshake entirely.
  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(java.time.Duration.ofSeconds(10))
      .build()

  /**
   * POST `payload` (JSON string) to `${baseUrl}${pathAndQuery}` with the standard JSON content type and a
   * 100s timeout, then either parse the response body via `parse` or fail with a `${errorPrefix} returned
   * <status>: <body>` runtime exception. `extraHeaders` lets the backend add per-request auth (go-judge's
   * `Authorization: Bearer …`) without rebuilding the whole request flow.
   *
   * Wraps the entire send + parse in `ZIO.attemptBlocking` so the JDK's blocking HTTP client doesn't pin a
   * platform thread.
   */
  private[codeRunPipeline] def postJson[A](
      baseUrl: String,
      pathAndQuery: String,
      payload: String,
      parse: String => A,
      errorPrefix: String,
      extraHeaders: Map[String, String] = Map.empty
  ): Task[A] =
    ZIO.attemptBlocking {
      val cleanedBase = baseUrl.replaceAll("/+$", "")
      val builder = HttpRequest
        .newBuilder()
        .uri(URI.create(s"$cleanedBase$pathAndQuery"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        // 100 s, not 30: a cold `scala-cli` run can outlast 30 s; go-judge's own
        // per-language clock limit fires first so the failure reads as a clean
        // TLE rather than an opaque HTTP timeout.
        .timeout(java.time.Duration.ofSeconds(100))
        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
      extraHeaders.foreach { case (k, v) => builder.header(k, v) }
      val resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
      if resp.statusCode() < 200 || resp.statusCode() >= 300 then
        throw new RuntimeException(s"$errorPrefix returned ${resp.statusCode()}: ${resp.body()}")
      parse(resp.body())
    }

final private class CodeRunPipelineLive(
    backends: List[CodeExecutionBackend],
    runGate: Option[Semaphore] = None
) extends CodeRunPipeline:

  override def run(req: RunRequest): IO[RunFailure, RunResponse] =
    for
      _ <- validate(req)
      lang <- ZIO
        .fromOption(Languages.resolve(req.language))
        .mapError(_ => RunFailure.BadInput(s"Language '${req.language}' is not runnable"))
      // Bound concurrent executions globally (go-judge fan-out guard); excess runs queue for a permit.
      result <- runGate match
        case Some(gate) => gate.withPermit(pickAndRun(lang, req))
        case None       => pickAndRun(lang, req)
    yield RunResponse(result = result, language = lang.info)

  private def validate(req: RunRequest): IO[RunFailure, Unit] =
    val sourceBytes = req.source.getBytes(StandardCharsets.UTF_8).length
    val stdinBytes  = req.stdin.fold(0)(_.getBytes(StandardCharsets.UTF_8).length)
    if sourceBytes > Languages.MaxSourceBytes then
      ZIO.fail(RunFailure.PayloadTooLarge("Source exceeds size limit"))
    else if stdinBytes > Languages.MaxStdinBytes then
      ZIO.fail(RunFailure.PayloadTooLarge("stdin exceeds size limit"))
    else ZIO.unit

  /**
   * Run on the first backend whose `supports(lang)` is true. Empty list → `NotConfigured`; non-empty but no
   * support → `BadInput`. Backend exceptions are wrapped as [[RunFailure.BackendFailure]].
   */
  private def pickAndRun(lang: Language, req: RunRequest): IO[RunFailure, RunResult] =
    if backends.isEmpty then ZIO.fail(RunFailure.NotConfigured)
    else
      backends.find(_.supports(lang)) match
        case Some(b) => b.run(req.source, req.stdin, lang).mapError(toBackendFailure)
        case None =>
          ZIO.fail(
            RunFailure.BadInput(
              s"Language '${lang.label}' is not supported by the configured execution backend."
            )
          )

  private def toBackendFailure(t: Throwable): RunFailure =
    RunFailure.BackendFailure(error = "Code execution failed", detail = Option(t.getMessage))
