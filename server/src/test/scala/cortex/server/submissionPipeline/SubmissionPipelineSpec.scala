package cortex.server.submissionPipeline

import cortex.server.auth.VerifiedClaims
import cortex.server.codeRunPipeline.{CodeRunPipeline, RunFailure}
import cortex.server.cortexPipeline.{CortexFailure, CortexPipeline}
import cortex.shared.api.Endpoints.{
  Book,
  ChapterFrontmatter,
  ChapterPayload,
  ChapterRef,
  CortexIndex,
  RunRequest,
  RunResponse,
  RunResult,
  RunnableLanguageInfo,
  SubmitRequest
}
import zio.*
import zio.test.*

import java.util.concurrent.atomic.AtomicReference

object SubmissionPipelineSpec extends ZIOSpecDefault:

  private val user = VerifiedClaims(
    sub = "sub-1",
    preferredUsername = "tester",
    name = None,
    email = None
  )

  private val chapterMarkdown =
    """# Flip Things
      |
      |## The Problem
      |
      |Reverse it.
      |
      |```python run
      |# starter
      |```
      |
      |```testcases
      |{
      |  "args": [{"id": "arr", "label": "arr", "type": "int[]"}],
      |  "cases": [
      |    {"args": {"arr": "[1, 2]"}, "expected": "[2, 1]"},
      |    {"args": {"arr": "[]"}, "expected": "[]"}
      |  ]
      |}
      |```
      |""".stripMargin

  private def payload(raw: String): ChapterPayload =
    val ref = ChapterRef(slug = "problems/flip", title = "Flip")
    ChapterPayload(
      book = Book(title = "DSA", chapters = Seq(ref), slug = "dsa", description = ""),
      chapter = ref,
      frontmatter = ChapterFrontmatter(title = "Flip"),
      raw = raw,
      prevSlug = None,
      nextSlug = None
    )

  private def cortexWith(raw: String): CortexPipeline = new CortexPipeline:
    override def index: IO[CortexFailure, CortexIndex] =
      ZIO.fail(CortexFailure.NotFound)
    override def chapter(book: String, chapter: String): IO[CortexFailure, ChapterPayload] =
      if book == "dsa" && chapter == "problems/flip" then ZIO.succeed(payload(raw))
      else ZIO.fail(CortexFailure.NotFound)

  /** Echoes a canned stdout per received stdin — records every RunRequest for assertions. */
  final private class FakeRunner(outputsByStdin: Map[String, String]) extends CodeRunPipeline:
    private val recorded        = AtomicReference[List[RunRequest]](Nil)
    def calls: List[RunRequest] = recorded.get().reverse

    override def run(req: RunRequest): IO[RunFailure, RunResponse] =
      recorded.updateAndGet(req :: _)
      val stdout = req.stdin.flatMap(outputsByStdin.get).getOrElse("")
      ZIO.succeed(
        RunResponse(
          result = RunResult(
            statusDescription = "Accepted",
            stdout = stdout,
            compileOutput = "",
            memory = Some(1024L),
            stderr = "",
            statusId = 3,
            time = Some("0.01")
          ),
          language = RunnableLanguageInfo(id = 71, label = "Python", aliases = Seq("python"))
        )
      )

  final private class FakeStore(
      allowed: Set[String],
      priorCount: Int = 0,
      rows: List[SubmissionStore.Row] = Nil
  ) extends SubmissionStore:
    private val saved      = AtomicReference[List[SubmissionStore.NewSubmission]](Nil)
    private val deleted    = AtomicReference[List[String]](Nil)
    private val deletedIds = AtomicReference[List[Long]](Nil)
    def savedRows: List[SubmissionStore.NewSubmission] = saved.get().reverse
    def deletedFor: List[String]                       = deleted.get().reverse
    def deletedIdsFor: List[Long]                      = deletedIds.get().reverse

    override def isAllowed(username: String): Task[Boolean] = ZIO.succeed(allowed(username))

    override def countFor(username: String, book: String, chapter: String): Task[Int] =
      ZIO.succeed(priorCount)

    override def save(row: SubmissionStore.NewSubmission): Task[Long] =
      saved.updateAndGet(row :: _)
      ZIO.succeed(42L)

    override def listFor(username: String, book: String, chapter: String): Task[List[SubmissionStore.Row]] =
      ZIO.succeed(rows)

    override def deleteAll(username: String): Task[Int] =
      deleted.updateAndGet(username :: _)
      ZIO.succeed(3)

    override def deleteOne(username: String, id: Long): Task[Int] =
      deletedIds.updateAndGet(id :: _)
      ZIO.succeed(1)

  private val req = SubmitRequest(
    book = "dsa",
    chapter = "problems/flip",
    language = "python",
    source = "print('x')"
  )

  override def spec: Spec[Any, Any] = suite("SubmissionPipeline")(
    test("runs every authored case in order, judges trimmed output, saves, and reports accepted") {
      val runner = FakeRunner(Map("[1, 2]\n" -> "[2, 1]\n", "[]\n" -> "[]\n"))
      val store  = FakeStore(Set("tester"))
      val p      = SubmissionPipeline.from(store, runner, cortexWith(chapterMarkdown))
      for resp <- p.submit(user, req)
      yield assertTrue(
        resp.accepted,
        resp.saved,
        resp.submissionId == Some(42L),
        resp.results.map(_.passed) == Seq(true, true),
        resp.results.map(_.statusDescription) == Seq("Accepted", "Accepted"),
        runner.calls.map(_.stdin) == List(Some("[1, 2]\n"), Some("[]\n")),
        runner.calls.forall(_.source == "print('x')"),
        store.savedRows.map(r => (r.username, r.accepted, r.passedCases, r.totalCases)) ==
          List(("tester", true, 2, 2))
      )
    },
    test("a clean run with the wrong output is a Wrong Answer, still saved as not accepted") {
      val runner = FakeRunner(Map("[1, 2]\n" -> "[1, 2]\n", "[]\n" -> "[]\n"))
      val store  = FakeStore(Set("tester"))
      val p      = SubmissionPipeline.from(store, runner, cortexWith(chapterMarkdown))
      for resp <- p.submit(user, req)
      yield assertTrue(
        !resp.accepted,
        resp.results.map(_.passed) == Seq(false, true),
        resp.results.head.statusDescription == "Wrong Answer",
        store.savedRows.map(r => (r.accepted, r.passedCases)) == List((false, 1))
      )
    },
    test("non-allow-listed user → NotAllowed BEFORE any code executes") {
      val runner = FakeRunner(Map.empty)
      val store  = FakeStore(Set("ani2fun"))
      val p      = SubmissionPipeline.from(store, runner, cortexWith(chapterMarkdown))
      for out <- p.submit(user, req).either
      yield assertTrue(
        out == Left(SubmissionFailure.NotAllowed("tester")),
        runner.calls.isEmpty,
        store.savedRows.isEmpty
      )
    },
    test("per-problem cap → LimitReached before any code executes") {
      val runner = FakeRunner(Map.empty)
      val store  = FakeStore(Set("tester"), priorCount = SubmissionPipeline.MaxSubmissionsPerProblem)
      val p      = SubmissionPipeline.from(store, runner, cortexWith(chapterMarkdown))
      for out <- p.submit(user, req).either
      yield assertTrue(
        out == Left(SubmissionFailure.LimitReached(SubmissionPipeline.MaxSubmissionsPerProblem)),
        runner.calls.isEmpty,
        store.savedRows.isEmpty
      )
    },
    test("one below the cap still goes through") {
      val runner = FakeRunner(Map("[1, 2]\n" -> "[2, 1]\n", "[]\n" -> "[]\n"))
      val store  = FakeStore(Set("tester"), priorCount = SubmissionPipeline.MaxSubmissionsPerProblem - 1)
      val p      = SubmissionPipeline.from(store, runner, cortexWith(chapterMarkdown))
      for resp <- p.submit(user, req)
      yield assertTrue(resp.accepted, store.savedRows.length == 1)
    },
    test("unknown chapter → ChapterNotFound") {
      val p = SubmissionPipeline.from(
        FakeStore(Set("tester")),
        FakeRunner(Map.empty),
        cortexWith(chapterMarkdown)
      )
      for out <- p.submit(user, req.copy(chapter = "nope")).either
      yield assertTrue(out == Left(SubmissionFailure.ChapterNotFound))
    },
    test("chapter without a testcases fence → BadInput") {
      val p = SubmissionPipeline.from(
        FakeStore(Set("tester")),
        FakeRunner(Map.empty),
        cortexWith("# No fences here\n\nJust prose.")
      )
      for out <- p.submit(user, req).either
      yield assertTrue(
        out.swap.exists {
          case SubmissionFailure.BadInput(msg) => msg.contains("testcases")
          case _                               => false
        }
      )
    },
    test("deleteAll wipes only the caller's rows and reports the count") {
      val store = FakeStore(Set.empty)
      val p     = SubmissionPipeline.from(store, FakeRunner(Map.empty), cortexWith(chapterMarkdown))
      for resp <- p.deleteAll(user)
      yield assertTrue(resp.deleted == 3, store.deletedFor == List("tester"))
    },
    test("list projects the store's rows into the response, order preserved (newest first)") {
      val rows = List(
        SubmissionStore.Row(7L, "python", accepted = true, 2, 2, "print(1)", 1000L),
        SubmissionStore.Row(3L, "java", accepted = false, 1, 2, "class Main {}", 500L)
      )
      val store = FakeStore(Set("tester"), rows = rows)
      val p     = SubmissionPipeline.from(store, FakeRunner(Map.empty), cortexWith(chapterMarkdown))
      for resp <- p.list(user, "dsa", "problems/flip")
      yield assertTrue(
        resp.submissions.map(_.id) == Seq(7L, 3L),
        resp.submissions.map(_.language) == Seq("python", "java"),
        resp.submissions.map(_.accepted) == Seq(true, false),
        resp.submissions.map(s => (s.passedCases, s.totalCases)) == Seq((2, 2), (1, 2)),
        resp.submissions.head.source == "print(1)",
        resp.submissions.head.createdAtEpochMs == 1000L
      )
    },
    test("deleteOne removes a single row by id and reports the count") {
      val store = FakeStore(Set("tester"))
      val p     = SubmissionPipeline.from(store, FakeRunner(Map.empty), cortexWith(chapterMarkdown))
      for resp <- p.deleteOne(user, 7L)
      yield assertTrue(resp.deleted == 1, store.deletedIdsFor == List(7L))
    }
  )

object TestcasesExtractorSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("TestcasesExtractor")(
    test("extracts ordered stdin lines (declared arg order) and trims empty expected") {
      val md =
        """```testcases
          |{
          |  "args": [{"id": "nums", "type": "int[]"}, {"id": "target", "type": "int"}],
          |  "cases": [
          |    {"args": {"target": 9, "nums": "[2, 7]"}, "expected": "[0, 1]"},
          |    {"args": {"nums": "[3, 3]", "target": "6"}, "expected": ""}
          |  ]
          |}
          |```""".stripMargin
      val out = TestcasesExtractor.extract(md)
      assertTrue(
        out == Right(
          TestcasesExtractor.Spec(
            List(
              TestcasesExtractor.Case("[2, 7]\n9\n", Some("[0, 1]")),
              TestcasesExtractor.Case("[3, 3]\n6\n", None)
            )
          )
        )
      )
    },
    test("no fence / unterminated fence / bad JSON each yield a Left with a reason") {
      assertTrue(
        TestcasesExtractor.extract("# nothing").isLeft,
        TestcasesExtractor.extract("```testcases\n{\"args\": []}").isLeft,
        TestcasesExtractor.extract("```testcases\nnot json\n```").isLeft
      )
    }
  )
