package cortex.server.submissionPipeline

import cortex.server.auth.VerifiedClaims
import cortex.server.codeRunPipeline.{CodeRunPipeline, RunFailure}
import cortex.server.cortexPipeline.{CortexFailure, CortexPipeline}
import cortex.shared.api.Endpoints.{
  DeleteSubmissionsResponse,
  ListSubmissionsResponse,
  RunRequest,
  SubmissionCaseResult,
  SubmissionSummary,
  SubmitRequest,
  SubmitResponse
}
import zio.*

import javax.sql.DataSource as JDataSource

/**
 * Submissions — run a solution against ALL of a problem chapter's authored test cases server-side and persist
 * the attempt for allow-listed users (ADR-0003 deep-module shape: one public trait, internal
 * [[SubmissionStore]] seam for the Postgres bytes, pure [[TestcasesExtractor]] invoked by the pipeline).
 *
 * The allowlist is checked BEFORE any code executes — a non-allow-listed caller costs nothing but the lookup.
 * This is a homelab deployment: grants are manual (one psql INSERT, see v2-submissions.sql), data carries no
 * durability promise, and `deleteAll` is the self-service erasure path.
 */
sealed trait SubmissionFailure extends Product with Serializable

object SubmissionFailure:
  /** Authenticated, but the GitHub username is not on the submission allowlist. */
  final case class NotAllowed(username: String) extends SubmissionFailure

  /** The (book, chapter) pair doesn't resolve to a chapter. */
  case object ChapterNotFound extends SubmissionFailure

  /** Request can't be judged: no testcases fence, malformed spec, unknown language, oversized payload. */
  final case class BadInput(error: String) extends SubmissionFailure

  /** The execution backend failed or is not configured. */
  final case class ExecutionFailed(error: String, detail: Option[String] = None) extends SubmissionFailure

  /**
   * The caller hit the hard per-user-per-problem submission cap (abuse guard, even for allowlisted users).
   */
  final case class LimitReached(limit: Int) extends SubmissionFailure

  /** Postgres or content-store trouble — the homelab equivalent of a 500. */
  final case class Internal(detail: String) extends SubmissionFailure

/** Internal persistence seam — plain JDBC over the HikariCP pool, like the other pipelines' stores. */
private[submissionPipeline] trait SubmissionStore:
  def isAllowed(username: String): Task[Boolean]
  def countFor(username: String, book: String, chapter: String): Task[Int]
  def save(row: SubmissionStore.NewSubmission): Task[Long]
  def listFor(username: String, book: String, chapter: String): Task[List[SubmissionStore.Row]]
  def deleteAll(username: String): Task[Int]
  def deleteOne(username: String, id: Long): Task[Int]

private[submissionPipeline] object SubmissionStore:

  final case class NewSubmission(
      username: String,
      book: String,
      chapter: String,
      language: String,
      source: String,
      accepted: Boolean,
      passedCases: Int,
      totalCases: Int
  )

  /** A stored submission projected for the Submissions tab (the aggregate verdict + the saved code). */
  final case class Row(
      id: Long,
      language: String,
      accepted: Boolean,
      passedCases: Int,
      totalCases: Int,
      source: String,
      createdAtEpochMs: Long
  )

trait SubmissionPipeline:
  def submit(user: VerifiedClaims, req: SubmitRequest): IO[SubmissionFailure, SubmitResponse]

  def list(
      user: VerifiedClaims,
      book: String,
      chapter: String
  ): IO[SubmissionFailure, ListSubmissionsResponse]

  def deleteAll(user: VerifiedClaims): IO[SubmissionFailure, DeleteSubmissionsResponse]
  def deleteOne(user: VerifiedClaims, id: Long): IO[SubmissionFailure, DeleteSubmissionsResponse]

object SubmissionPipeline:

  private val StatusOk = 3

  /**
   * Hard per-user-per-problem cap, checked before anything executes. Not a rolling window — once hit, no
   * further attempts are stored for that problem (delete-and-resubmit via DELETE /api/submissions resets it).
   * An abuse guard for the homelab executor, applying to allowlisted users too.
   */
  private[submissionPipeline] val MaxSubmissionsPerProblem = 100

  val live: ZLayer[JDataSource & CodeRunPipeline & CortexPipeline, Nothing, SubmissionPipeline] =
    ZLayer.fromFunction((ds: JDataSource, run: CodeRunPipeline, cortex: CortexPipeline) =>
      from(liveStore(ds), run, cortex)
    )

  /** Test factory: inject fakes for all three seams (mirrors CodeRunPipeline.from). */
  private[submissionPipeline] def from(
      store: SubmissionStore,
      codeRun: CodeRunPipeline,
      cortex: CortexPipeline
  ): SubmissionPipeline =
    SubmissionPipelineLive(store, codeRun, cortex)

  final private class SubmissionPipelineLive(
      store: SubmissionStore,
      codeRun: CodeRunPipeline,
      cortex: CortexPipeline
  ) extends SubmissionPipeline:

    override def submit(user: VerifiedClaims, req: SubmitRequest): IO[SubmissionFailure, SubmitResponse] =
      for
        allowed <- store
          .isAllowed(user.preferredUsername)
          .mapError(t => SubmissionFailure.Internal(s"allowlist lookup failed: ${t.getMessage}"))
        _ <- ZIO.fail(SubmissionFailure.NotAllowed(user.preferredUsername)).unless(allowed)
        priorCount <- store
          .countFor(user.preferredUsername, req.book, req.chapter)
          .mapError(t => SubmissionFailure.Internal(s"submission count lookup failed: ${t.getMessage}"))
        _ <- ZIO
          .fail(SubmissionFailure.LimitReached(MaxSubmissionsPerProblem))
          .when(priorCount >= MaxSubmissionsPerProblem)
        payload <- cortex.chapter(req.book, req.chapter).mapError {
          case CortexFailure.NotFound             => SubmissionFailure.ChapterNotFound
          case CortexFailure.IO(detail)           => SubmissionFailure.Internal(detail)
          case CortexFailure.IndexInvalid(detail) => SubmissionFailure.Internal(detail)
        }
        spec <- ZIO
          .fromEither(TestcasesExtractor.extract(payload.raw))
          .mapError(SubmissionFailure.BadInput.apply)
        results <- ZIO.foreach(spec.cases.zipWithIndex) { case (c, i) => judge(req, c, i) }
        accepted = results.forall(_.passed)
        id <- store
          .save(
            SubmissionStore.NewSubmission(
              username = user.preferredUsername,
              book = req.book,
              chapter = req.chapter,
              language = req.language,
              source = req.source,
              accepted = accepted,
              passedCases = results.count(_.passed),
              totalCases = results.length
            )
          )
          .mapError(t => SubmissionFailure.Internal(s"saving the submission failed: ${t.getMessage}"))
      yield SubmitResponse(
        accepted = accepted,
        saved = true,
        submissionId = Some(id),
        results = results
      )

    override def list(
        user: VerifiedClaims,
        book: String,
        chapter: String
    ): IO[SubmissionFailure, ListSubmissionsResponse] =
      store
        .listFor(user.preferredUsername, book, chapter)
        .mapBoth(
          t => SubmissionFailure.Internal(s"listing submissions failed: ${t.getMessage}"),
          rows =>
            ListSubmissionsResponse(
              submissions = rows.map(r =>
                SubmissionSummary(
                  id = r.id,
                  language = r.language,
                  accepted = r.accepted,
                  passedCases = r.passedCases,
                  totalCases = r.totalCases,
                  source = r.source,
                  createdAtEpochMs = r.createdAtEpochMs
                )
              )
            )
        )

    override def deleteAll(user: VerifiedClaims): IO[SubmissionFailure, DeleteSubmissionsResponse] =
      store
        .deleteAll(user.preferredUsername)
        .mapBoth(
          t => SubmissionFailure.Internal(s"deleting submissions failed: ${t.getMessage}"),
          n => DeleteSubmissionsResponse(deleted = n)
        )

    override def deleteOne(
        user: VerifiedClaims,
        id: Long
    ): IO[SubmissionFailure, DeleteSubmissionsResponse] =
      store
        .deleteOne(user.preferredUsername, id)
        .mapBoth(
          t => SubmissionFailure.Internal(s"deleting submission failed: ${t.getMessage}"),
          n => DeleteSubmissionsResponse(deleted = n)
        )

    /** Run one case through the execution backend and judge trimmed stdout against the expected output. */
    private def judge(
        req: SubmitRequest,
        c: TestcasesExtractor.Case,
        index: Int
    ): IO[SubmissionFailure, SubmissionCaseResult] =
      codeRun
        .run(RunRequest(language = req.language, source = req.source, stdin = Some(c.stdin)))
        .mapError {
          case RunFailure.BadInput(error, _)     => SubmissionFailure.BadInput(error)
          case RunFailure.PayloadTooLarge(error) => SubmissionFailure.BadInput(error)
          case RunFailure.NotConfigured =>
            SubmissionFailure.ExecutionFailed("Code execution is not configured on this server.")
          case RunFailure.BackendFailure(error, detail) =>
            SubmissionFailure.ExecutionFailed(error, detail)
        }
        .map { resp =>
          val r      = resp.result
          val stdout = Option(r.stdout).getOrElse("")
          val ranOk  = r.statusId == StatusOk
          val passed = ranOk && c.expected.forall(e => stdout.trim == e.trim)
          // The backend says "Accepted" for any clean exit; a clean exit with the wrong output is a
          // Wrong Answer from the submitter's point of view.
          val status =
            if !ranOk then r.statusDescription
            else if passed then "Accepted"
            else "Wrong Answer"
          SubmissionCaseResult(
            caseIndex = index,
            passed = passed,
            statusDescription = status,
            stdout = stdout,
            expected = c.expected,
            time = r.time,
            memory = r.memory
          )
        }

  // ---------------------------------------------------------------------------
  // Live JDBC store — same plain-PreparedStatement shape as the other pipelines.
  // ---------------------------------------------------------------------------

  private def liveStore(ds: JDataSource): SubmissionStore =
    new SubmissionStore:
      override def isAllowed(username: String): Task[Boolean] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement("SELECT 1 FROM submission_allowlist WHERE username = ?")
          try
            stmt.setString(1, username)
            val rs = stmt.executeQuery()
            try rs.next()
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def countFor(username: String, book: String, chapter: String): Task[Int] =
        ZIO.attemptBlocking {
          val conn = ds.getConnection
          try
            val stmt = conn.prepareStatement(
              "SELECT COUNT(*) FROM submissions WHERE username = ? AND book = ? AND chapter = ?"
            )
            try
              stmt.setString(1, username)
              stmt.setString(2, book)
              stmt.setString(3, chapter)
              val rs = stmt.executeQuery()
              try
                if rs.next() then rs.getInt(1) else 0
              finally rs.close()
            finally stmt.close()
          finally conn.close()
        }

      override def save(row: SubmissionStore.NewSubmission): Task[Long] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement(
            """INSERT INTO submissions
              |  (username, book, chapter, language, source, accepted, passed_cases, total_cases)
              |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              |RETURNING id""".stripMargin
          )
          try
            stmt.setString(1, row.username)
            stmt.setString(2, row.book)
            stmt.setString(3, row.chapter)
            stmt.setString(4, row.language)
            stmt.setString(5, row.source)
            stmt.setBoolean(6, row.accepted)
            stmt.setInt(7, row.passedCases)
            stmt.setInt(8, row.totalCases)
            val rs = stmt.executeQuery()
            try
              if rs.next() then rs.getLong(1) else 0L
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def listFor(
          username: String,
          book: String,
          chapter: String
      ): Task[List[SubmissionStore.Row]] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement(
            """SELECT id, language, accepted, passed_cases, total_cases, source, created_at
              |FROM submissions
              |WHERE username = ? AND book = ? AND chapter = ?
              |ORDER BY created_at DESC, id DESC""".stripMargin
          )
          try
            stmt.setString(1, username)
            stmt.setString(2, book)
            stmt.setString(3, chapter)
            val rs = stmt.executeQuery()
            try
              val buf = List.newBuilder[SubmissionStore.Row]
              while rs.next() do
                buf += SubmissionStore.Row(
                  id = rs.getLong("id"),
                  language = rs.getString("language"),
                  accepted = rs.getBoolean("accepted"),
                  passedCases = rs.getInt("passed_cases"),
                  totalCases = rs.getInt("total_cases"),
                  source = rs.getString("source"),
                  createdAtEpochMs = rs.getTimestamp("created_at").getTime
                )
              buf.result()
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def deleteAll(username: String): Task[Int] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement("DELETE FROM submissions WHERE username = ?")
          try
            stmt.setString(1, username)
            stmt.executeUpdate()
          finally stmt.close()
        finally conn.close()
      }

      override def deleteOne(username: String, id: Long): Task[Int] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          // Scoped to the owner so one user can never delete another's submission by id.
          val stmt = conn.prepareStatement("DELETE FROM submissions WHERE username = ? AND id = ?")
          try
            stmt.setString(1, username)
            stmt.setLong(2, id)
            stmt.executeUpdate()
          finally stmt.close()
        finally conn.close()
      }
