package cortex.server.http

import zio.*
import zio.http.*
import zio.stream.ZStream

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/**
 * Reverse-proxies `/tutor/<path...>` to the standalone cortex-tutor service.
 *
 * cortex-tutor used to carry its own public Ingress, so the SPA fetched it directly at a second origin. That
 * Ingress was removed — the tutor is now a ClusterIP reachable only from inside the cluster
 * (`http://cortex-tutor.apps-prod.svc.cluster.local`). A browser can't resolve that name, so the SPA now
 * talks to the tutor *same-origin* through this proxy: the browser hits `cortex.kakde.eu/tutor/v1/...`,
 * cortex forwards to `${AppConfig.tutorBaseUrl}/v1/...`. `/api/auth/config` advertises the relative base
 * `"/tutor"` (see `ApiRoutes.authConfigEndpoint`); the real in-cluster DNS stays server-side as the proxy
 * TARGET and is never handed to the browser.
 *
 * Posture mirrors [[LikeC4ProxyRoutes]] (the `/c4/` proxy) and piston: ClusterIP only + NetworkPolicy in
 * apps-prod permitting cortex → cortex-tutor. The difference from LikeC4 is that the tutor is authenticated
 * and streams: `submitTurn` returns Server-Sent Events, so the response body is passed through as a *chunked
 * stream* rather than buffered, and a small allowlist of request headers (`Authorization`, `Content-Type`,
 * `Accept`) is forwarded so the tutor can do its own JWT auth and content negotiation.
 *
 * Security — this is deliberately NOT an open proxy:
 *   - The upstream host is the FIXED `tutorBaseUrl`. Only the *path* (and query) come from the request, so a
 *     caller cannot retarget the proxy at an arbitrary host (no SSRF surface beyond the configured tutor).
 *   - Scoped strictly to the `/tutor/` prefix — no other path reaches this handler.
 *   - Only `Authorization`, `Content-Type`, and `Accept` are forwarded upstream. Cookies and every other
 *     header are dropped. We neither add nor strip the bearer token: the JWT is forwarded verbatim and the
 *     tutor remains the sole authority on auth.
 *   - On the response, only the status code and `Content-Type` are preserved; upstream cookies and other
 *     headers are not surfaced to the browser.
 *
 * When `tutorBaseUrl` is unset (`CORTEX_TUTOR_BASE_URL` absent) the proxy mounts but every call returns 503,
 * making the misconfiguration visible rather than silently 404-ing — the same posture `/api/run` takes when
 * its executor URL is missing.
 */
object TutorProxyRoutes:

  // Request headers we forward upstream. Strict allowlist: the JWT (the tutor authenticates itself), plus
  // content negotiation. Everything else — cookies included — is dropped.
  private val ForwardedRequestHeaders = List("Authorization", "Content-Type", "Accept")

  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  // Bound concurrent in-flight proxied calls so a burst can't exhaust the blocking pool / upstream
  // sockets — the same posture the run-concurrency semaphore gives /api/run. The count spans admission
  // through the response body stream's finaliser (so it covers the long SSE hold, not just the header
  // phase); an over-cap call sheds with 503 rather than queueing. Process-global, created once like the
  // client above.
  private val MaxConcurrentProxied = 32L

  private val inFlight: Ref[Long] =
    Unsafe.unsafe(implicit u => Ref.unsafe.make(0L))

  /**
   * Build the `/tutor/` routes. `upstreamBaseUrl` is `AppConfig.tutorBaseUrl`: when `None`, the tutor is not
   * configured and every call short-circuits to 503.
   */
  def from(upstreamBaseUrl: Option[String]): Routes[Any, Response] =
    upstreamBaseUrl match
      case None      => unconfiguredRoutes
      case Some(url) => configuredRoutes(url.stripSuffix("/"))

  private val unconfiguredRoutes: Routes[Any, Response] =
    val handler = Handler.fromFunctionZIO[(zio.http.Path, Request)] { _ =>
      ZIO.succeed(Response.status(Status.ServiceUnavailable))
    }
    Routes(
      Method.GET / "tutor" / trailing  -> handler,
      Method.POST / "tutor" / trailing -> handler
    )

  private def configuredRoutes(base: String): Routes[Any, Response] =
    Routes(
      Method.GET / "tutor" / trailing ->
        Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (rest, req) =>
          proxy(base, rest, req, body = None)
        },
      Method.POST / "tutor" / trailing ->
        Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (rest, req) =>
          // Buffer the request body fully before forwarding. Tutor POSTs (createSession, submitTurn,
          // reset, byok-record) carry small JSON payloads; the *response* is what streams (SSE), not the
          // request. A failure reading the client body surfaces as 502.
          req.body.asArray
            .flatMap(bytes => proxy(base, rest, req, body = Some(bytes)))
            .catchAll(_ => ZIO.succeed(Response.status(Status.BadGateway)))
        }
    )

  private def buildUpstreamUrl(base: String, rest: zio.http.Path, req: Request): String =
    // `trailing` captures the segments after `/tutor` without a leading slash, so a request for
    // `/tutor/v1/whoami` arrives as `rest.encode == "v1/whoami"`. We always insert the slash between
    // `/tutor`-stripped base and the captured rest so empty and non-empty paths both produce a well-formed
    // upstream URL. The host is fixed (`base`); only this path + query come from the caller.
    val restPath = rest.encode.stripPrefix("/")
    // `QueryParams.encode` already prefixes a `?` when non-empty; strip it before re-adding our own
    // separator, or the query doubles up (`prompt-bundle??step=…`) and the tutor 422s on the bad param.
    val query    = req.url.queryParams.encode.stripPrefix("?")
    val querySep = if query.nonEmpty then "?" else ""
    s"$base/$restPath$querySep$query"

  /**
   * Forward one request to the fixed tutor upstream and stream the response back.
   *
   * The blocking `httpClient.send(..., ofInputStream)` returns once the upstream *status + headers* are in;
   * the body is then read lazily off the returned `InputStream` and re-streamed to the browser via
   * `Body.fromStreamChunked`. That keeps SSE working end-to-end: tokens are flushed to the client as the
   * tutor emits them, not buffered until the turn completes. Send-time failures (connect refused, timeout)
   * map to 502; a failure *mid-stream* propagates through the chunked body and the SPA's SSE reader already
   * treats a truncated stream as "stream ended unexpectedly".
   */
  private def proxy(
      base: String,
      rest: zio.http.Path,
      req: Request,
      body: Option[Array[Byte]]
  ): UIO[Response] =
    val upstreamUrl = buildUpstreamUrl(base, rest, req)
    // Admit at most MaxConcurrentProxied calls; shed the rest with 503. Uninterruptible through the
    // header phase so an interrupt can't sneak between the increment and the stream that owns the
    // decrement; once the body stream is handed back, ITS finaliser releases the permit (covering the
    // long SSE hold and a mid-turn client disconnect alike). A send failure decrements via `catchAll`.
    inFlight
      .modify(n => if n >= MaxConcurrentProxied then (false, n) else (true, n + 1))
      .flatMap { admitted =>
        if !admitted then ZIO.succeed(Response.status(Status.ServiceUnavailable))
        else
          ZIO
            .attemptBlocking {
              val builder = HttpRequest
                .newBuilder()
                .uri(URI.create(upstreamUrl))
                .timeout(Duration.ofSeconds(30))

              // Re-attach only the allowlisted request headers (case-insensitive lookup off the request).
              ForwardedRequestHeaders.foreach { name =>
                req.rawHeader(name).foreach(value => builder.header(name, value))
              }

              body match
                case Some(bytes) => builder.POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                case None        => builder.GET()

              val upstream = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
              val status   = Status.fromInt(upstream.statusCode())
              val contentType =
                Option(upstream.headers().firstValue("content-type").orElse(null))
                  .filter(_.nonEmpty)
                  .getOrElse("application/octet-stream")

              // Stream the upstream body through unbuffered. `fromStreamChunked` uses chunked
              // transfer-encoding (no Content-Length), which is what an SSE response needs.
              //
              // Bound the stream's lifetime: if the upstream emits nothing for >200s (a stuck or
              // slow-loris stream), end it so the cortex thread + upstream socket are reclaimed rather
              // than held open indefinitely. 200s clears the legitimate worst case (the Ollama coach can
              // take ~180s to first token); the SPA's SSE reader already treats a truncated stream as
              // "stream ended unexpectedly". The `ensuring` releases the concurrency permit on any stream
              // end — completion, the timeout, an upstream error, or a client disconnect.
              val bodyStream =
                ZStream.fromInputStream(upstream.body()).timeout(200.seconds).ensuring(inFlight.update(_ - 1))
              Response(
                status = status,
                headers = Headers(Header.ContentType.parse(contentType).toOption.toList*),
                body = Body.fromStreamChunked(bodyStream)
              )
            }
            .catchAll(_ => inFlight.update(_ - 1).as(Response.status(Status.BadGateway)))
      }
      .uninterruptible
