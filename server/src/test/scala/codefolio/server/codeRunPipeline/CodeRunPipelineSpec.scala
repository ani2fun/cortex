package codefolio.server.codeRunPipeline

import codefolio.shared.api.Endpoints.{RunRequest, RunResult}
import zio.*
import zio.test.*

object CodeRunPipelineSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("CodeRunPipeline")(
    test("rejects unknown languages before backend selection") {
      val pipeline = CodeRunPipeline.from(Nil)
      pipeline
        .run(RunRequest(language = "not-a-language", source = "println(1)", stdin = None))
        .either
        .map(o => assertTrue(o == Left(RunFailure.BadInput("Language 'not-a-language' is not runnable"))))
    },
    test("rejects oversized source before language resolution") {
      val pipeline = CodeRunPipeline.from(Nil)
      val source   = "x" * (Languages.MaxSourceBytes + 1)
      pipeline
        .run(RunRequest(language = "python", source = source, stdin = None))
        .either
        .map(o => assertTrue(o == Left(RunFailure.PayloadTooLarge("Source exceeds size limit"))))
    },
    test("rejects oversized stdin before language resolution") {
      val pipeline = CodeRunPipeline.from(Nil)
      val stdin    = "x" * (Languages.MaxStdinBytes + 1)
      pipeline
        .run(RunRequest(language = "python", source = "print(input())", stdin = Some(stdin)))
        .either
        .map(o => assertTrue(o == Left(RunFailure.PayloadTooLarge("stdin exceeds size limit"))))
    },
    test("returns NotConfigured when no backend is configured") {
      val pipeline = CodeRunPipeline.from(Nil)
      pipeline
        .run(RunRequest(language = "python", source = "print('ok')", stdin = None))
        .either
        .map(o => assertTrue(o == Left(RunFailure.NotConfigured)))
    },
    test("runs on the configured backend and echoes the language") {
      val backend  = FakeGoJudge.succeeding(sampleResult)
      val pipeline = CodeRunPipeline.from(List(backend))
      for res <- pipeline.run(RunRequest(language = "python", source = "print('ok')", stdin = None))
      yield assertTrue(
        res.result == sampleResult,
        res.language.id == 71,
        backend.calls.size == 1,
        backend.calls.head.source == "print('ok')"
      )
    },
    test("returns BadInput when no configured backend supports the language") {
      // A backend that only supports python; a SQL request should not reach it.
      val backend  = FakeGoJudge.supporting(Set(71), sampleResult)
      val pipeline = CodeRunPipeline.from(List(backend))
      pipeline
        .run(RunRequest(language = "sql", source = "SELECT 1", stdin = None))
        .either
        .map {
          case Left(RunFailure.BadInput(msg, _)) =>
            assertTrue(msg.contains("SQL"), msg.contains("not supported"), backend.calls.isEmpty)
          case other => assertNever(s"unexpected outcome: $other")
        }
    },
    test("wraps a backend Throwable as BackendFailure") {
      val backend  = FakeGoJudge.failing(new RuntimeException("boom"))
      val pipeline = CodeRunPipeline.from(List(backend))
      pipeline
        .run(RunRequest(language = "python", source = "x", stdin = None))
        .either
        .map {
          case Left(RunFailure.BackendFailure(error, detail)) =>
            assertTrue(error == "Code execution failed", detail == Some("boom"))
          case other => assertNever(s"unexpected outcome: $other")
        }
    }
  )

  private val sampleResult: RunResult = RunResult(
    stdout = "ok\n",
    stderr = "",
    compileOutput = "",
    statusId = 3,
    statusDescription = "Accepted",
    time = None,
    memory = None
  )
