package cortex.server.http

import cortex.server.blogPipeline.BlogFailure
import cortex.server.codeRunPipeline.RunFailure
import cortex.server.cortexPipeline.CortexFailure
import cortex.server.helloPipeline.HelloFailure
import sttp.model.StatusCode
import zio.test.*

object ApiErrorsSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Nothing] = suite("ApiErrors")(
    test("maps RunFailure variants to HTTP status codes") {
      assertTrue(
        ApiErrors.toHttp(RunFailure.BadInput("bad"))._1 == StatusCode.BadRequest,
        ApiErrors.toHttp(RunFailure.PayloadTooLarge("large"))._1 == StatusCode.PayloadTooLarge,
        ApiErrors.toHttp(RunFailure.NotConfigured)._1 == StatusCode.ServiceUnavailable,
        ApiErrors.toHttp(RunFailure.BackendFailure("upstream"))._1 == StatusCode.BadGateway
      )
    },
    test("maps CortexFailure variants to HTTP status codes") {
      assertTrue(
        ApiErrors.toHttp(CortexFailure.NotFound)._1 == StatusCode.NotFound,
        ApiErrors.toHttp(CortexFailure.IO("boom"))._1 == StatusCode.InternalServerError,
        ApiErrors.toHttp(CortexFailure.IndexInvalid("bad index"))._1 ==
          StatusCode.InternalServerError
      )
    },
    test("maps RunFailure variants to stable ApiError envelopes") {
      val (_, notConfigured) = ApiErrors.toHttp(RunFailure.NotConfigured)
      val (_, badInput) = ApiErrors.toHttp(
        RunFailure.BadInput("Nope", hint = Some("Choose a supported language."))
      )
      val (_, backend) = ApiErrors.toHttp(
        RunFailure.BackendFailure("go-judge failed", detail = Some("upstream 500"))
      )

      assertTrue(
        notConfigured.error == "Code execution is not configured on this server.",
        notConfigured.detail.isEmpty,
        notConfigured.hint.exists(_.contains("Set EXECUTOR_URL")),
        badInput.error == "Nope",
        badInput.detail.isEmpty,
        badInput.hint == Some("Choose a supported language."),
        backend.error == "go-judge failed",
        backend.detail == Some("upstream 500"),
        backend.hint.isEmpty
      )
    },
    test("maps CortexFailure variants to stable ApiError envelopes") {
      val (_, notFound)     = ApiErrors.toHttp(CortexFailure.NotFound)
      val (_, ioErr)        = ApiErrors.toHttp(CortexFailure.IO("disk gone"))
      val (_, indexInvalid) = ApiErrors.toHttp(CortexFailure.IndexInvalid("dup slug"))

      assertTrue(
        notFound.error == "Not found",
        notFound.detail.isEmpty,
        ioErr.error == "Cortex IO error",
        ioErr.detail == Some("disk gone"),
        indexInvalid.error == "Cortex index is invalid",
        indexInvalid.detail == Some("dup slug")
      )
    },
    test("maps HelloFailure variants to 503 ServiceUnavailable") {
      assertTrue(
        ApiErrors.toHttp(HelloFailure.GreetingUnavailable())._1 == StatusCode.ServiceUnavailable,
        ApiErrors.toHttp(HelloFailure.RecentUnavailable())._1 == StatusCode.ServiceUnavailable
      )
    },
    test("maps HelloFailure variants to stable ApiError envelopes") {
      val (_, greeting) = ApiErrors.toHttp(HelloFailure.GreetingUnavailable(Some("postgres timeout")))
      val (_, recent)   = ApiErrors.toHttp(HelloFailure.RecentUnavailable(Some("mongo down")))

      assertTrue(
        greeting.error == "Greeting unavailable",
        greeting.detail == Some("postgres timeout"),
        greeting.hint.isEmpty,
        recent.error == "Recent calls unavailable",
        recent.detail == Some("mongo down"),
        recent.hint.isEmpty
      )
    },
    test("maps BlogFailure variants to HTTP status codes") {
      assertTrue(
        ApiErrors.toHttp(BlogFailure.NotFound)._1 == StatusCode.NotFound,
        ApiErrors.toHttp(BlogFailure.IO("boom"))._1 == StatusCode.InternalServerError,
        ApiErrors.toHttp(BlogFailure.IndexInvalid("bad index"))._1 ==
          StatusCode.InternalServerError
      )
    },
    test("maps BlogFailure variants to stable ApiError envelopes") {
      val (_, notFound)     = ApiErrors.toHttp(BlogFailure.NotFound)
      val (_, ioErr)        = ApiErrors.toHttp(BlogFailure.IO("disk gone"))
      val (_, indexInvalid) = ApiErrors.toHttp(BlogFailure.IndexInvalid("dup slug"))

      assertTrue(
        notFound.error == "Not found",
        notFound.detail.isEmpty,
        ioErr.error == "Blog IO error",
        ioErr.detail == Some("disk gone"),
        indexInvalid.error == "Blog index is invalid",
        indexInvalid.detail == Some("dup slug")
      )
    }
  )
