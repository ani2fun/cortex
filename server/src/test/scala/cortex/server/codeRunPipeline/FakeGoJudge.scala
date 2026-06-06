package cortex.server.codeRunPipeline

import cortex.server.codeRunPipeline.Languages.Language
import cortex.shared.api.Endpoints.RunResult
import zio.*

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only go-judge-shaped backend. The real go-judge supports every language (`supports = true`); the
 * optional `supportedIds` lets a test simulate a backend that does NOT support a given language, so the
 * pipeline's `BadInput` (no-supporting-backend) branch can be exercised. Records every invocation.
 */
final class FakeGoJudge(
    supportedIds: Option[Set[Int]],
    response: Task[RunResult]
) extends CodeExecutionBackend:

  private val recorded =
    AtomicReference(List.empty[FakeGoJudge.Call])

  override def supports(lang: Language): Boolean =
    supportedIds.forall(_.contains(lang.id))

  override def run(
      source: String,
      stdin: Option[String],
      lang: Language
  ): Task[RunResult] =
    ZIO.succeed(recorded.updateAndGet(_ :+ FakeGoJudge.Call(source, stdin, lang))) *> response

  def calls: List[FakeGoJudge.Call] = recorded.get()

object FakeGoJudge:

  final case class Call(source: String, stdin: Option[String], lang: Language)

  /** Universal backend (supports every language) that returns `result`. */
  def succeeding(result: RunResult): FakeGoJudge =
    FakeGoJudge(None, ZIO.succeed(result))

  /** Universal backend that fails with `error`. */
  def failing(error: Throwable): FakeGoJudge =
    FakeGoJudge(None, ZIO.fail(error))

  /** Backend that only supports the given Judge0 ids — for the "no supporting backend" path. */
  def supporting(ids: Set[Int], result: RunResult): FakeGoJudge =
    FakeGoJudge(Some(ids), ZIO.succeed(result))
