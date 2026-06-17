package cortex.server.coachPipeline

import cortex.server.auth.VerifiedClaims
import cortex.shared.api.Endpoints.{
  DeleteCoachResponse,
  ListSavedCoachResponse,
  SaveCoachRequest,
  SaveCoachResponse,
  SavedCoachSummary
}
import zio.*

import javax.sql.DataSource as JDataSource

/**
 * Coach-save — persist an explicit snapshot of a coach transcript for allow-listed users, gated by the SAME
 * `submission_allowlist` as POST /api/submissions (ADR-0003 deep-module shape: one public trait, an internal
 * [[CoachSaveStore]] seam for the Postgres bytes). The tutor runs the live interview and keeps sessions only
 * ephemerally (TTL/purge); this is the "keep my work" path into the homelab DB.
 *
 * The allowlist is checked before the snapshot is stored. The transcript arrives FROM the client (the tutor's
 * CoachSession wire JSON) — the server treats it as an opaque, size-capped jsonb blob and never re-reads the
 * tutor. Grants are manual (one psql INSERT into submission_allowlist), data carries no durability promise,
 * and `deleteAll` is the self-service erasure path.
 */
sealed trait CoachSaveFailure extends Product with Serializable

object CoachSaveFailure:
  /** Authenticated, but the GitHub username is not on the (shared submission) allowlist. */
  final case class NotAllowed(username: String) extends CoachSaveFailure

  /** Transcript is empty, oversized, or not a JSON object. */
  final case class BadInput(error: String) extends CoachSaveFailure

  /** The caller hit the hard per-user-per-problem saved-snapshot cap. */
  final case class LimitReached(limit: Int) extends CoachSaveFailure

  /** Postgres trouble — the homelab equivalent of a 500. */
  final case class Internal(detail: String) extends CoachSaveFailure

/** Internal persistence seam — plain JDBC over the HikariCP pool, like the other pipelines' stores. */
private[coachPipeline] trait CoachSaveStore:
  def isAllowed(username: String): Task[Boolean]
  def countFor(username: String, problemId: String): Task[Int]
  def save(row: CoachSaveStore.NewSaved): Task[Long]
  def listFor(username: String, problemId: String): Task[List[CoachSaveStore.Row]]
  def deleteAll(username: String): Task[Int]
  def deleteOne(username: String, id: Long): Task[Int]

private[coachPipeline] object CoachSaveStore:

  final case class NewSaved(
      username: String,
      problemId: String,
      sessionId: String,
      stepIndex: Int,
      completed: Boolean,
      messageCount: Int,
      transcriptJson: String
  )

  /** A stored snapshot projected for the saved-sessions view (metadata + the transcript blob). */
  final case class Row(
      id: Long,
      problemId: String,
      sessionId: String,
      stepIndex: Int,
      completed: Boolean,
      messageCount: Int,
      transcriptJson: String,
      createdAtEpochMs: Long
  )

trait CoachSavePipeline:
  def save(user: VerifiedClaims, req: SaveCoachRequest): IO[CoachSaveFailure, SaveCoachResponse]
  def list(user: VerifiedClaims, problemId: String): IO[CoachSaveFailure, ListSavedCoachResponse]
  def deleteAll(user: VerifiedClaims): IO[CoachSaveFailure, DeleteCoachResponse]
  def deleteOne(user: VerifiedClaims, id: Long): IO[CoachSaveFailure, DeleteCoachResponse]

object CoachSavePipeline:

  /** Hard per-user-per-problem cap on stored snapshots (abuse guard, applies to allowlisted users too). */
  private[coachPipeline] val MaxSavedPerProblem = 100

  /** Transcript size cap (chars) — well under the server's 512 KB request limit, with a clear 400. */
  private[coachPipeline] val MaxTranscriptChars = 400_000

  val live: ZLayer[JDataSource, Nothing, CoachSavePipeline] =
    ZLayer.fromFunction((ds: JDataSource) => from(liveStore(ds)))

  /** Test factory: inject a fake store. */
  private[coachPipeline] def from(store: CoachSaveStore): CoachSavePipeline =
    CoachSavePipelineLive(store)

  final private class CoachSavePipelineLive(store: CoachSaveStore) extends CoachSavePipeline:

    override def save(
        user: VerifiedClaims,
        req: SaveCoachRequest
    ): IO[CoachSaveFailure, SaveCoachResponse] =
      for
        allowed <- store
          .isAllowed(user.preferredUsername)
          .mapError(t => CoachSaveFailure.Internal(s"allowlist lookup failed: ${t.getMessage}"))
        _ <- ZIO.fail(CoachSaveFailure.NotAllowed(user.preferredUsername)).unless(allowed)
        _ <- ZIO.fail(CoachSaveFailure.BadInput("The transcript is empty.")).when(req.transcript.isEmpty)
        _ <- ZIO
          .fail(CoachSaveFailure.BadInput(s"The transcript exceeds the ${MaxTranscriptChars}-char limit."))
          .when(req.transcript.length > MaxTranscriptChars)
        _ <- ZIO
          .fail(CoachSaveFailure.BadInput("The transcript must be a JSON object."))
          .unless(req.transcript.trim.startsWith("{"))
        priorCount <- store
          .countFor(user.preferredUsername, req.problemId)
          .mapError(t => CoachSaveFailure.Internal(s"saved-count lookup failed: ${t.getMessage}"))
        _ <- ZIO
          .fail(CoachSaveFailure.LimitReached(MaxSavedPerProblem))
          .when(priorCount >= MaxSavedPerProblem)
        id <- store
          .save(
            CoachSaveStore.NewSaved(
              username = user.preferredUsername,
              problemId = req.problemId,
              sessionId = req.sessionId,
              stepIndex = req.stepIndex,
              completed = req.completed,
              messageCount = req.messageCount,
              transcriptJson = req.transcript
            )
          )
          .mapError(t => CoachSaveFailure.Internal(s"saving the coach snapshot failed: ${t.getMessage}"))
      yield SaveCoachResponse(saved = true, savedId = id)

    override def list(
        user: VerifiedClaims,
        problemId: String
    ): IO[CoachSaveFailure, ListSavedCoachResponse] =
      store
        .listFor(user.preferredUsername, problemId)
        .mapBoth(
          t => CoachSaveFailure.Internal(s"listing saved coach sessions failed: ${t.getMessage}"),
          rows =>
            ListSavedCoachResponse(
              saved = rows.map(r =>
                SavedCoachSummary(
                  id = r.id,
                  problemId = r.problemId,
                  sessionId = r.sessionId,
                  stepIndex = r.stepIndex,
                  completed = r.completed,
                  messageCount = r.messageCount,
                  transcript = r.transcriptJson,
                  createdAtEpochMs = r.createdAtEpochMs
                )
              )
            )
        )

    override def deleteAll(user: VerifiedClaims): IO[CoachSaveFailure, DeleteCoachResponse] =
      store
        .deleteAll(user.preferredUsername)
        .mapBoth(
          t => CoachSaveFailure.Internal(s"deleting saved coach sessions failed: ${t.getMessage}"),
          n => DeleteCoachResponse(deleted = n)
        )

    override def deleteOne(user: VerifiedClaims, id: Long): IO[CoachSaveFailure, DeleteCoachResponse] =
      store
        .deleteOne(user.preferredUsername, id)
        .mapBoth(
          t => CoachSaveFailure.Internal(s"deleting saved coach session failed: ${t.getMessage}"),
          n => DeleteCoachResponse(deleted = n)
        )

  // ---------------------------------------------------------------------------
  // Live JDBC store — same plain-PreparedStatement shape as the other pipelines.
  // ---------------------------------------------------------------------------

  private def liveStore(ds: JDataSource): CoachSaveStore =
    new CoachSaveStore:
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

      override def countFor(username: String, problemId: String): Task[Int] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement(
            "SELECT COUNT(*) FROM coach_saved_session WHERE username = ? AND problem_id = ?"
          )
          try
            stmt.setString(1, username)
            stmt.setString(2, problemId)
            val rs = stmt.executeQuery()
            try if rs.next() then rs.getInt(1) else 0
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def save(row: CoachSaveStore.NewSaved): Task[Long] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.prepareStatement(
            """INSERT INTO coach_saved_session
              |  (username, problem_id, session_id, step_index, completed, message_count, transcript_json)
              |VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
              |RETURNING id""".stripMargin
          )
          try
            stmt.setString(1, row.username)
            stmt.setString(2, row.problemId)
            stmt.setString(3, row.sessionId)
            stmt.setInt(4, row.stepIndex)
            stmt.setBoolean(5, row.completed)
            stmt.setInt(6, row.messageCount)
            stmt.setString(7, row.transcriptJson)
            val rs = stmt.executeQuery()
            try if rs.next() then rs.getLong(1) else 0L
            finally rs.close()
          finally stmt.close()
        finally conn.close()
      }

      override def listFor(username: String, problemId: String): Task[List[CoachSaveStore.Row]] =
        ZIO.attemptBlocking {
          val conn = ds.getConnection
          try
            val stmt = conn.prepareStatement(
              """SELECT id, problem_id, session_id, step_index, completed, message_count,
                |       transcript_json::text AS transcript_text, created_at
                |FROM coach_saved_session
                |WHERE username = ? AND problem_id = ?
                |ORDER BY created_at DESC, id DESC""".stripMargin
            )
            try
              stmt.setString(1, username)
              stmt.setString(2, problemId)
              val rs = stmt.executeQuery()
              try
                val buf = List.newBuilder[CoachSaveStore.Row]
                while rs.next() do
                  buf += CoachSaveStore.Row(
                    id = rs.getLong("id"),
                    problemId = rs.getString("problem_id"),
                    sessionId = rs.getString("session_id"),
                    stepIndex = rs.getInt("step_index"),
                    completed = rs.getBoolean("completed"),
                    messageCount = rs.getInt("message_count"),
                    transcriptJson = rs.getString("transcript_text"),
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
          val stmt = conn.prepareStatement("DELETE FROM coach_saved_session WHERE username = ?")
          try
            stmt.setString(1, username)
            stmt.executeUpdate()
          finally stmt.close()
        finally conn.close()
      }

      override def deleteOne(username: String, id: Long): Task[Int] = ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          // Scoped to the owner so one user can never delete another's snapshot by id.
          val stmt = conn.prepareStatement("DELETE FROM coach_saved_session WHERE username = ? AND id = ?")
          try
            stmt.setString(1, username)
            stmt.setLong(2, id)
            stmt.executeUpdate()
          finally stmt.close()
        finally conn.close()
      }
