package cortex.server.codeRunPipeline

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * Pins the user-facing error string emitted by `CodeRunPipeline.postJson` on a non-2xx response. The wire
 * parser (`GoJudgeWireSpec`) covers JSON ↔ `RunResult` mapping; this spec covers the one user-observable
 * piece of the transport — the message format `"$errorPrefix returned $status: $body"` — which surfaces
 * inside `RunFailure.BackendFailure.detail` and ends up in the API error envelope.
 */
object PostJsonSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Throwable] = suite("CodeRunPipeline.postJson")(
    test("200 with JSON body — parse callback runs and the result is returned") {
      withStubServer(status = 200, body = """{"echo":"hi"}""") { url =>
        CodeRunPipeline
          .postJson(
            baseUrl = url,
            pathAndQuery = "/run",
            payload = """{"q":1}""",
            parse = (s: String) => s,
            errorPrefix = "go-judge"
          )
          .map(body => assertTrue(body == """{"echo":"hi"}"""))
      }
    },
    test("503 with body — fails with exact \"$prefix returned $status: $body\" message") {
      withStubServer(status = 503, body = "Server overloaded") { url =>
        CodeRunPipeline
          .postJson(
            baseUrl = url,
            pathAndQuery = "/run",
            payload = "{}",
            parse = (_: String) => (),
            errorPrefix = "go-judge"
          )
          .either
          .map {
            case Left(t)  => assertTrue(t.getMessage == "go-judge returned 503: Server overloaded")
            case Right(_) => assertNever("expected failure on 503")
          }
      }
    },
    test("4xx with body and a different prefix — message uses the supplied prefix") {
      withStubServer(status = 400, body = """{"error":"bad"}""") { url =>
        CodeRunPipeline
          .postJson(
            baseUrl = url,
            pathAndQuery = "/run",
            payload = "{}",
            parse = (_: String) => (),
            errorPrefix = "executor"
          )
          .either
          .map {
            case Left(t) =>
              assertTrue(t.getMessage == """executor returned 400: {"error":"bad"}""")
            case Right(_) => assertNever("expected failure on 400")
          }
      }
    },
    test("connection refused — fails with the JDK-native ConnectException family") {
      // Port 1 is reserved by IANA and never listened on by any tool we'd run locally,
      // so the OS rejects the TCP SYN immediately. Faster + more deterministic than
      // picking a random unbound port and racing.
      CodeRunPipeline
        .postJson(
          baseUrl = "http://127.0.0.1:1",
          pathAndQuery = "/run",
          payload = "{}",
          parse = (_: String) => (),
          errorPrefix = "Piston"
        )
        .either
        .map {
          case Left(t) =>
            // We don't assert the message — that's a JDK-version-specific string. The
            // contract here is just "connection failure surfaces as a Throwable, not
            // as a 200-with-empty-body."
            assert(t)(isSubtype[java.io.IOException](anything))
          case Right(_) => assertNever("expected failure on connection refused")
        }
    } @@ TestAspect.timeout(15.seconds)
  ) @@ TestAspect.sequential

  // ---------------------------------------------------------------------------
  // Tiny in-process HTTP server. JDK-native (com.sun.net.httpserver) — zero new
  // deps. Each test acquires its own server on an ephemeral port, runs the
  // assertion, and tears it down via `ZIO.acquireRelease`.
  // ---------------------------------------------------------------------------

  private def withStubServer[A](status: Int, body: String)(
      run: String => Task[A]
  ): Task[A] =
    ZIO.scoped {
      stubServer(status, body).flatMap(url => run(url))
    }

  private def stubServer(status: Int, body: String): ZIO[Scope, Throwable, String] =
    ZIO.acquireRelease(
      ZIO.attemptBlocking {
        val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(
          "/",
          new HttpHandler:
            override def handle(exchange: HttpExchange): Unit =
              val bytes = body.getBytes(StandardCharsets.UTF_8)
              exchange.getResponseHeaders.set("Content-Type", "application/json")
              exchange.sendResponseHeaders(status, bytes.length.toLong)
              val os = exchange.getResponseBody
              try os.write(bytes)
              finally os.close()
        )
        server.start()
        server
      }
    )(s => ZIO.attempt(s.stop(0)).orDie).map(s => s"http://127.0.0.1:${s.getAddress.getPort}")
