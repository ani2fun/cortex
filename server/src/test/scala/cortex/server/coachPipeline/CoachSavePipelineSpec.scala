package cortex.server.coachPipeline

import cortex.server.auth.VerifiedClaims
import cortex.shared.api.Endpoints.SaveCoachRequest
import zio.*
import zio.test.*

import java.util.concurrent.atomic.AtomicReference

object CoachSavePipelineSpec extends ZIOSpecDefault:

  private val user = VerifiedClaims(
    sub = "sub-1",
    preferredUsername = "tester",
    name = None,
    email = None
  )

  private val transcript = """{"sessionId":"s1","messages":[]}"""

  private def req(t: String = transcript): SaveCoachRequest = SaveCoachRequest(
    sessionId = "s1",
    problemId = "dsa/flip",
    stepIndex = 2,
    messageCount = 4,
    completed = false,
    transcript = t
  )

  final private class FakeStore(
      allowed: Set[String],
      priorCount: Int = 0,
      rows: List[CoachSaveStore.Row] = Nil
  ) extends CoachSaveStore:
    private val saved                            = AtomicReference[List[CoachSaveStore.NewSaved]](Nil)
    private val deleted                          = AtomicReference[List[String]](Nil)
    private val deletedIds                       = AtomicReference[List[Long]](Nil)
    def savedRows: List[CoachSaveStore.NewSaved] = saved.get().reverse
    def deletedFor: List[String]                 = deleted.get().reverse
    def deletedIdsFor: List[Long]                = deletedIds.get().reverse

    override def isAllowed(username: String): Task[Boolean]               = ZIO.succeed(allowed(username))
    override def countFor(username: String, problemId: String): Task[Int] = ZIO.succeed(priorCount)

    override def save(row: CoachSaveStore.NewSaved): Task[Long] =
      saved.updateAndGet(row :: _)
      ZIO.succeed(42L)

    override def listFor(username: String, problemId: String): Task[List[CoachSaveStore.Row]] =
      ZIO.succeed(rows)

    override def deleteAll(username: String): Task[Int] =
      deleted.updateAndGet(username :: _)
      ZIO.succeed(3)

    override def deleteOne(username: String, id: Long): Task[Int] =
      deletedIds.updateAndGet(id :: _)
      ZIO.succeed(1)

  override def spec: Spec[Any, Any] = suite("CoachSavePipeline")(
    test("allow-listed save stores the snapshot verbatim and reports the new id") {
      val store = FakeStore(Set("tester"))
      val p     = CoachSavePipeline.from(store)
      for resp <- p.save(user, req())
      yield assertTrue(
        resp.saved,
        resp.savedId == 42L,
        store.savedRows.map(r =>
          (r.username, r.problemId, r.sessionId, r.stepIndex, r.messageCount, r.transcriptJson)
        ) == List(("tester", "dsa/flip", "s1", 2, 4, transcript))
      )
    },
    test("non-allow-listed user → NotAllowed, nothing stored") {
      val store = FakeStore(Set("ani2fun"))
      val p     = CoachSavePipeline.from(store)
      for out <- p.save(user, req()).either
      yield assertTrue(out == Left(CoachSaveFailure.NotAllowed("tester")), store.savedRows.isEmpty)
    },
    test("per-problem cap → LimitReached, nothing stored") {
      val store = FakeStore(Set("tester"), priorCount = CoachSavePipeline.MaxSavedPerProblem)
      val p     = CoachSavePipeline.from(store)
      for out <- p.save(user, req()).either
      yield assertTrue(
        out == Left(CoachSaveFailure.LimitReached(CoachSavePipeline.MaxSavedPerProblem)),
        store.savedRows.isEmpty
      )
    },
    test("empty transcript → BadInput") {
      val p = CoachSavePipeline.from(FakeStore(Set("tester")))
      for out <- p.save(user, req(t = "")).either
      yield assertTrue(out.swap.exists(_.isInstanceOf[CoachSaveFailure.BadInput]))
    },
    test("a non-object transcript → BadInput") {
      val p = CoachSavePipeline.from(FakeStore(Set("tester")))
      for out <- p.save(user, req(t = "[1,2,3]")).either
      yield assertTrue(out.swap.exists(_.isInstanceOf[CoachSaveFailure.BadInput]))
    },
    test("list projects the store's rows into the response, order preserved (newest first)") {
      val rows = List(
        CoachSaveStore.Row(7L, "dsa/flip", "s2", 5, true, 10, "{}", 2000L),
        CoachSaveStore.Row(3L, "dsa/flip", "s1", 1, false, 2, "{}", 1000L)
      )
      val p = CoachSavePipeline.from(FakeStore(Set("tester"), rows = rows))
      for resp <- p.list(user, "dsa/flip")
      yield assertTrue(
        resp.saved.map(_.id) == Seq(7L, 3L),
        resp.saved.map(_.stepIndex) == Seq(5, 1),
        resp.saved.map(_.completed) == Seq(true, false),
        resp.saved.head.createdAtEpochMs == 2000L
      )
    },
    test("deleteAll wipes the caller's rows and reports the count") {
      val store = FakeStore(Set.empty)
      val p     = CoachSavePipeline.from(store)
      for resp <- p.deleteAll(user)
      yield assertTrue(resp.deleted == 3, store.deletedFor == List("tester"))
    },
    test("deleteOne removes a single row by id and reports the count") {
      val store = FakeStore(Set("tester"))
      val p     = CoachSavePipeline.from(store)
      for resp <- p.deleteOne(user, 7L)
      yield assertTrue(resp.deleted == 1, store.deletedIdsFor == List(7L))
    }
  )
