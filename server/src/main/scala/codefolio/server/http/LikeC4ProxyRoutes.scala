package codefolio.server.http

import zio.*
import zio.http.*

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/**
 * Reverse-proxies `GET` requests under `/c4/` to the LikeC4 service.
 *
 * LikeC4 ships as a static SPA built with `--base /c4/`, so its index.html references `/c4/assets/...` paths.
 * The browser hits codefolio at `kakde.eu/c4/...`; codefolio forwards those requests to the upstream LikeC4
 * deployment. There is no public Ingress on the LikeC4 deployment.
 *
 * Posture mirrors piston: ClusterIP only + NetworkPolicy in apps-prod permitting codefolio → likec4 on port
 * 8080.
 *
 * v1 demo notes:
 *   - Upstream URL is supplied by `AppConfig.likec4.upstreamUrl` (env var `LIKEC4_URL`). Production defaults
 *     to the in-cluster Service `http://likec4`; bin/dev overrides to `http://localhost:8090` and docker
 *     compose to `http://likec4:8080`.
 *   - Forwards only the body bytes + Content-Type. Cache-Control / ETag are dropped to keep the pass-through
 *     simple; we can layer them in later.
 */
object LikeC4ProxyRoutes:

  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  def from(upstreamBaseUrl: String): Routes[Any, Response] =
    Routes(
      Method.GET / "c4" / trailing ->
        Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (rest, req) =>
          proxy(buildUpstreamUrl(upstreamBaseUrl, rest, req))
        }
    )

  private def buildUpstreamUrl(base: String, rest: zio.http.Path, req: Request): String =
    // `trailing` captures the segments after `/c4` without a leading slash, so
    // a request for `/c4/view/index` arrives as `rest.encode == "view/index"`.
    // We always insert the slash between `/c4` and the captured rest so empty
    // and non-empty paths both produce a well-formed upstream URL.
    val restPath = rest.encode.stripPrefix("/")
    val query    = req.url.queryParams.encode
    val querySep = if query.nonEmpty then "?" else ""
    s"$base/c4/$restPath$querySep$query"

  private def proxy(upstreamUrl: String): UIO[Response] =
    ZIO
      .attemptBlocking {
        val request = HttpRequest
          .newBuilder()
          .uri(URI.create(upstreamUrl))
          .GET()
          .timeout(Duration.ofSeconds(15))
          .build()
        val upstream = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        val status   = Status.fromInt(upstream.statusCode())
        val contentType =
          Option(upstream.headers().firstValue("content-type").orElse(null))
            .filter(_.nonEmpty)
            .getOrElse("application/octet-stream")
        Response(
          status = status,
          headers = Headers(Header.ContentType.parse(contentType).toOption.toList*),
          body = Body.fromArray(upstream.body())
        )
      }
      .catchAll(_ => ZIO.succeed(Response.status(Status.BadGateway)))
