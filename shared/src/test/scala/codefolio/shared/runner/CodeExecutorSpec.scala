package codefolio.shared.runner

import codefolio.shared.api.Endpoints.RunResult
import zio.test.*

object CodeExecutorSpec extends ZIOSpecDefault:

  private val sample = RunResult(
    stdout = "ok\n",
    stderr = "",
    compileOutput = "",
    statusId = 3,
    statusDescription = "Accepted",
    time = None,
    memory = None
  )

  override def spec: Spec[Any, Any] = suite("CodeExecutor")(
    test("initial sets Idle, ReadOnly, no result, no error") {
      val s = CodeExecutor.initial("print(1)")
      assertTrue(
        s.code == "print(1)",
        s.runState == CodeExecutor.RunState.Idle,
        s.editMode == CodeExecutor.EditMode.ReadOnly,
        s.result.isEmpty,
        s.error.isEmpty
      )
    },
    test("enterEdit flips editMode to Editing, leaving everything else") {
      val s0 = CodeExecutor.initial("x")
      val s1 = CodeExecutor.enterEdit(s0)
      assertTrue(
        s1.editMode == CodeExecutor.EditMode.Editing,
        s1.code == s0.code,
        s1.runState == s0.runState,
        s1.runId == s0.runId
      )
    },
    test("cancelEdit reverts code to source and returns to ReadOnly") {
      val edited = CodeExecutor.setCode(CodeExecutor.enterEdit(CodeExecutor.initial("orig")), "tweaked")
      val s      = CodeExecutor.cancelEdit(edited, "orig")
      assertTrue(
        s.code == "orig",
        s.editMode == CodeExecutor.EditMode.ReadOnly
      )
    },
    test("cancelEdit keeps a prior result intact") {
      val done = CodeExecutor.completed(
        CodeExecutor.started(CodeExecutor.enterEdit(CodeExecutor.initial("orig"))),
        CodeExecutor.started(CodeExecutor.enterEdit(CodeExecutor.initial("orig"))).runId,
        sample
      )
      val s = CodeExecutor.cancelEdit(done, "orig")
      assertTrue(s.result == Some(sample), s.editMode == CodeExecutor.EditMode.ReadOnly)
    },
    test("changedLineCount counts only the differing lines") {
      val s = CodeExecutor.setCode(CodeExecutor.initial("a\nb\nc"), "a\nB\nc\nd")
      assertTrue(CodeExecutor.changedLineCount(s, "a\nb\nc") == 2)
    },
    test("started clears result + error and issues a fresh handle") {
      val running = CodeExecutor.started(CodeExecutor.initial("x"))
      val done    = CodeExecutor.completed(running, running.runId, sample).copy(error = Some("old"))
      val s1      = CodeExecutor.started(done)
      assertTrue(
        s1.runState == CodeExecutor.RunState.Running,
        s1.result.isEmpty,
        s1.error.isEmpty,
        s1.runId != done.runId
      )
    },
    test("completed applies the result when the handle matches the latest run") {
      val running = CodeExecutor.started(CodeExecutor.initial("x"))
      val done    = CodeExecutor.completed(running, running.runId, sample)
      assertTrue(
        done.runState == CodeExecutor.RunState.Done,
        done.result == Some(sample)
      )
    },
    test("completed ignores stale results (handle mismatch)") {
      val running    = CodeExecutor.started(CodeExecutor.initial("x"))
      val rerun      = CodeExecutor.started(running)
      val staleApply = CodeExecutor.completed(rerun, running.runId, sample)
      assertTrue(staleApply == rerun)
    },
    test("failed applies the error when the handle matches") {
      val running = CodeExecutor.started(CodeExecutor.initial("x"))
      val errored = CodeExecutor.failed(running, running.runId, "boom")
      assertTrue(
        errored.runState == CodeExecutor.RunState.Done,
        errored.error == Some("boom")
      )
    },
    test("failed ignores stale errors (handle mismatch)") {
      val running    = CodeExecutor.started(CodeExecutor.initial("x"))
      val rerun      = CodeExecutor.started(running)
      val staleApply = CodeExecutor.failed(rerun, running.runId, "boom")
      assertTrue(staleApply == rerun)
    },
    test("cancel returns Idle and issues a fresh handle") {
      val running   = CodeExecutor.started(CodeExecutor.initial("x"))
      val cancelled = CodeExecutor.cancel(running)
      assertTrue(
        cancelled.runState == CodeExecutor.RunState.Idle,
        cancelled.runId != running.runId
      )
    },
    test("a result for a cancelled run is ignored") {
      val running   = CodeExecutor.started(CodeExecutor.initial("x"))
      val cancelled = CodeExecutor.cancel(running)
      val applied   = CodeExecutor.completed(cancelled, running.runId, sample)
      assertTrue(applied == cancelled)
    },
    test("setCode updates the editor without touching runState or runId") {
      val s0 = CodeExecutor.started(CodeExecutor.initial("x"))
      val s1 = CodeExecutor.setCode(s0, "y")
      assertTrue(
        s1.code == "y",
        s1.runState == s0.runState,
        s1.runId == s0.runId
      )
    },
    test("isDirty true when code differs from source") {
      val s0 = CodeExecutor.initial("x")
      val s1 = CodeExecutor.setCode(s0, "y")
      assertTrue(
        !CodeExecutor.isDirty(s0, "x"),
        CodeExecutor.isDirty(s1, "x")
      )
    }
  )
