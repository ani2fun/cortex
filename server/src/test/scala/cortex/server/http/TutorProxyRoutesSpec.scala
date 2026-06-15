package cortex.server.http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import zio.*
import zio.http.*
import zio.test.*

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

/**
 * Behavioural contract for the cortex-tutor reverse proxy.
 *
 * cortex-tutor lost its public Ingress and is now a ClusterIP, so the SPA reaches it same-origin through
 * the `/tutor/` prefix on the cortex backend. These tests run real requests through
 * `TutorProxyRoutes.from(...).routes`
 * against a JDK-native stub upstream (same pattern as `PostJsonSpec`) and pin the load-bearing properties:
 *
 *   - the path under `/tutor` is forwarded verbatim to the FIXED upstream (only the path varies — no open
 *     proxy),
 *   - the `Authorization` header (the JWT) and a request body ride along on POST,
 *   - the upstream status code, `Content-Type`, and body are passed back, and SSE (`text/event-stream`)
 *     bodies stream through intact,
 *   - cookies / unrelated request headers are NOT forwarded,
 *   - an unconfigured proxy (no `tutorBaseUrl`) returns 503 rather than silently 404-ing.
 */
object TutorProxyRoutesSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Throwable] = suite("TutorProxyRoutes")(
    test("GET forwards the sub-path verbatim to the fixed upstream and returns its status + body") {
      withEchoServer() { (url, recorded) =>
        for
          res  <- run(TutorProxyRoutes.from(Some(url)), Request.get("/tutor/v1/whoami"))
          body <- res.body.asString
          seen <- recorded.get
        yield assertTrue(
          res.status == Status.Ok,
          // The upstream sees exactly `/v1/whoami` — `/tutor` is stripped, the rest is preserved.
          seen.path == "/v1/whoami",
          seen.method == "GET",
          body.contains("\"echo\":\"/v1/whoami\"")
        )
      }
    },
    test("forwards the Authorization header (the JWT) upstream unchanged") {
      withEchoServer() { (url, recorded) =>
        val req = Request
          .get("/tutor/v1/whoami")
          .addHeader("Authorization", "Bearer the-jwt-token")
        for
          _    <- run(TutorProxyRoutes.from(Some(url)), req)
          seen <- recorded.get
        yield assertTrue(seen.authorization.contains("Bearer the-jwt-token"))
      }
    },
    test("does NOT forward cookies or unrelated request headers (not an open header pass-through)") {
      withEchoServer() { (url, recorded) =>
        val req = Request
          .get("/tutor/v1/whoami")
          .addHeader("Cookie", "session=secret")
          .addHeader("X-Internal-Trace", "should-be-dropped")
        for
          _    <- run(TutorProxyRoutes.from(Some(url)), req)
          seen <- recorded.get
        yield assertTrue(
          seen.cookie.isEmpty,
          !seen.headerNames.exists(_.equalsIgnoreCase("x-internal-trace"))
        )
      }
    },
    test("POST forwards the request body and preserves the upstream status + Content-Type") {
      withEchoServer(status = 201, contentType = "application/json") { (url, recorded) =>
        val req = Request
          .post("/tutor/v1/sessions", Body.fromString("""{"problemId":"p1"}"""))
          .addHeader("Content-Type", "application/json")
        for
          res         <- run(TutorProxyRoutes.from(Some(url)), req)
          seen        <- recorded.get
          contentType <- ZIO.succeed(res.header(Header.ContentType).map(_.renderedValue))
        yield assertTrue(
          res.status == Status.Created,
          seen.method == "POST",
          seen.body == """{"problemId":"p1"}""",
          contentType.exists(_.startsWith("application/json"))
        )
      }
    },
    test("streams an SSE response body through with text/event-stream preserved") {
      // sse-starlette frames: `data:` lines terminated by a blank line. The proxy must pass these back as a
      // chunked stream so the SPA's submitTurn reader sees them — and keep the event-stream content type.
      val sse = "data: {\"t\":\"state\"}\n\ndata: {\"t\":\"token\"}\n\n"
      withEchoServer(status = 200, contentType = "text/event-stream", bodyOverride = Some(sse)) {
        (url, _) =>
          for
            res         <- run(TutorProxyRoutes.from(Some(url)), Request.post("/tutor/v1/sessions/s1/turns", Body.empty))
            body        <- res.body.asString
            contentType <- ZIO.succeed(res.header(Header.ContentType).map(_.renderedValue))
          yield assertTrue(
            res.status == Status.Ok,
            contentType.exists(_.contains("text/event-stream")),
            body == sse
          )
      }
    },
    test("preserves a non-2xx upstream status (e.g. 401) so the SPA can branch on tutor auth failures") {
      withEchoServer(status = 401) { (url, _) =>
        run(TutorProxyRoutes.from(Some(url)), Request.get("/tutor/v1/whoami"))
          .map(res => assertTrue(res.status == Status.Unauthorized))
      }
    },
    test("unconfigured proxy (no tutorBaseUrl) returns 503 for /tutor/* rather than 404") {
      val routes = TutorProxyRoutes.from(None)
      for
        getRes  <- run(routes, Request.get("/tutor/v1/whoami"))
        postRes <- run(routes, Request.post("/tutor/v1/sessions", Body.fromString("{}")))
      yield assertTrue(
        getRes.status == Status.ServiceUnavailable,
        postRes.status == Status.ServiceUnavailable
      )
    }
  ) @@ TestAspect.sequential

  // `runZIO` turns an unmatched route into a 404 Response in the success channel and allocates
  // request-scoped resources, so each call runs inside its own `ZIO.scoped` (mirrors StaticRoutesSpec).
  private def run(routes: Routes[Any, Response], req: Request): ZIO[Any, Throwable, Response] =
    ZIO.scoped(routes.runZIO(req))

  // ---------------------------------------------------------------------------
  // What the stub upstream recorded about the request the proxy forwarded.
  // ---------------------------------------------------------------------------
  private final case class Recorded(
      method: String,
      path: String,
      authorization: Option[String],
      cookie: Option[String],
      headerNames: List[String],
      body: String
  )

  private object Recorded:
    val empty: Recorded = Recorded("", "", None, None, Nil, "")

  // ---------------------------------------------------------------------------
  // JDK-native echo upstream (com.sun.net.httpserver — zero new deps, like
  // PostJsonSpec). It records the inbound request into a Ref and replies with the
  // requested status + content-type. The default body echoes the path it saw so
  // the test can prove the sub-path was forwarded; `bodyOverride` supplies a raw
  // body (used for the SSE stream case).
  // ---------------------------------------------------------------------------
  private def withEchoServer[A](
      status: Int = 200,
      contentType: String = "application/json",
      bodyOverride: Option[String] = None
  )(run: (String, Ref[Recorded]) => Task[A]): Task[A] =
    ZIO.scoped {
      for
        recorded <- Ref.make(Recorded.empty)
        url      <- echoServer(status, contentType, bodyOverride, recorded)
        result   <- run(url, recorded)
      yield result
    }

  private def echoServer(
      status: Int,
      contentType: String,
      bodyOverride: Option[String],
      recorded: Ref[Recorded]
  ): ZIO[Scope, Throwable, String] =
    ZIO.acquireRelease(
      ZIO.attemptBlocking {
        val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(
          "/",
          new HttpHandler:
            override def handle(exchange: HttpExchange): Unit =
              val reqBody  = new String(exchange.getRequestBody.readAllBytes(), StandardCharsets.UTF_8)
              val reqHdrs  = exchange.getRequestHeaders
              val hdrNames = reqHdrs.keySet().asScala.toList
              // Unsafe Ref poke from the JDK handler thread — fine here: the test awaits the proxy call
              // before reading the Ref, so this write always happens-before that read.
              Unsafe.unsafe { implicit u =>
                Runtime.default.unsafe.run(
                  recorded.set(
                    Recorded(
                      method = exchange.getRequestMethod,
                      path = exchange.getRequestURI.getPath,
                      authorization = Option(reqHdrs.getFirst("Authorization")),
                      cookie = Option(reqHdrs.getFirst("Cookie")),
                      headerNames = hdrNames,
                      body = reqBody
                    )
                  )
                )
              }
              val payload = bodyOverride.getOrElse(s"""{"echo":"${exchange.getRequestURI.getPath}"}""")
              val bytes   = payload.getBytes(StandardCharsets.UTF_8)
              exchange.getResponseHeaders.set("Content-Type", contentType)
              exchange.sendResponseHeaders(status, bytes.length.toLong)
              val os = exchange.getResponseBody
              try os.write(bytes)
              finally os.close()
        )
        server.start()
        server
      }
    )(s => ZIO.attempt(s.stop(0)).orDie).map(s => s"http://127.0.0.1:${s.getAddress.getPort}")
