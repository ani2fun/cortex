package cortex.server.codeRunPipeline

import cortex.server.codeRunPipeline.Languages.Language
import zio.test.*

/**
 * Golden-fixture tests for the pure go-judge wire adapter: the `/run` request shape (sh -c command, copyIn
 * source, stdin/stdout/stderr collectors, per-language limits, compile markers) and the response →
 * `RunResult` mapping (status/exit → statusId, the compile-marker → Compilation Error path, ns→seconds time,
 * bytes→KB memory). Exercised without an HTTP server so wire bugs surface at test time.
 */
object GoJudgeWireSpec extends ZIOSpecDefault:

  private val python: Language =
    Languages.resolve("python").getOrElse(throw new IllegalStateException("python missing"))

  private val java: Language =
    Languages.resolve("java").getOrElse(throw new IllegalStateException("java missing"))

  private def jstr(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

  /** Build a go-judge `/run` response (array of one cmd result). */
  private def resp(
      status: String = "Accepted",
      exitStatus: Int = 0,
      stdout: String = "",
      stderr: String = "",
      crc: Option[String] = None,
      cerr: Option[String] = None,
      time: Option[Long] = None,
      memory: Option[Long] = None
  ): String =
    val files = List(
      Some(s""""stdout": ${jstr(stdout)}"""),
      Some(s""""stderr": ${jstr(stderr)}"""),
      crc.map(c => s""""__cf_crc": ${jstr(c)}"""),
      cerr.map(c => s""""__cf_cerr": ${jstr(c)}""")
    ).flatten.mkString(", ")
    s"""[{
       |  "status": "$status",
       |  "exitStatus": $exitStatus,
       |  "time": ${time.map(_.toString).getOrElse("null")},
       |  "memory": ${memory.map(_.toString).getOrElse("null")},
       |  "files": { $files }
       |}]""".stripMargin

  override def spec: Spec[Any, Any] = suite("GoJudgeWire")(
    suite("buildRequestBody")(
      test("interpreted language: sh -c run command, source copied in, no compile markers") {
        val body = GoJudgeWire.buildRequestBody("print('hi')", Some("data"), python)
        assertTrue(
          body.contains("\"/bin/sh\""),
          body.contains("python3 main.py"),
          body.contains("\"main.py\""),          // copyIn key
          body.contains("print('hi')"),          // raw source (not base64)
          body.contains("\"content\":\"data\""), // stdin collector content
          !body.contains("__cf_crc")             // interpreted → no compile markers
        )
      },
      test("compiled language: wraps compile+run with markers and normalises the Java entrypoint") {
        val body = GoJudgeWire.buildRequestBody(
          "public class Solution { public static void main(String[] a){} }",
          None,
          java
        )
        assertTrue(
          body.contains("javac Main.java"),
          body.contains("java -cp . Main"),
          body.contains("__cf_crc"),          // marker in shell + copyOut
          body.contains("\"Main.java\""),     // copyIn key
          body.contains("public class Main"), // entrypoint normalised Solution -> Main
          !body.contains("class Solution")
        )
      },
      test("absent stdin serialises as empty content") {
        val body = GoJudgeWire.buildRequestBody("x=1", None, python)
        assertTrue(body.contains("\"content\":\"\""))
      }
    ),
    suite("parseRunResult")(
      test("Accepted + exit 0 maps to statusId 3 and normalises time ns→s, memory bytes→KB") {
        val r = GoJudgeWire.parseRunResult(compiled = false)(
          resp(stdout = "hello\n", time = Some(12_000_000L), memory = Some(5_632_000L))
        )
        assertTrue(
          r.stdout == "hello\n",
          r.statusId == 3,
          r.statusDescription == "Accepted",
          r.time == Some("0.012"),
          r.memory == Some(5500L) // 5 632 000 bytes — go-judge reports bytes, the contract is KB
        )
      },
      test("non-zero exit maps to Runtime Error (NZEC)") {
        val r = GoJudgeWire.parseRunResult(compiled = false)(
          resp(status = "Nonzero Exit Status", exitStatus = 1, stderr = "boom")
        )
        assertTrue(r.statusId == 11, r.statusDescription == "Runtime Error (NZEC)", r.stderr == "boom")
      },
      test("Accepted status but non-zero exit still maps to Runtime Error") {
        val r = GoJudgeWire.parseRunResult(compiled = false)(resp(status = "Accepted", exitStatus = 2))
        assertTrue(r.statusId == 11)
      },
      test("TimeLimitExceeded maps to statusId 5") {
        val r = GoJudgeWire.parseRunResult(compiled = false)(resp(status = "TimeLimitExceeded"))
        assertTrue(r.statusId == 5, r.statusDescription == "Time Limit Exceeded")
      },
      test("compiled: non-zero compile rc maps to Compilation Error with compiler output") {
        val r = GoJudgeWire.parseRunResult(compiled = true)(
          resp(crc = Some("1\n"), cerr = Some("Main.java:1: error: ';' expected"))
        )
        assertTrue(
          r.statusId == 6,
          r.statusDescription == "Compilation Error",
          r.compileOutput.contains("error"),
          r.stdout.isEmpty
        )
      },
      test("compiled: zero compile rc + clean run maps to Accepted") {
        val r = GoJudgeWire.parseRunResult(compiled = true)(
          resp(stdout = "done\n", crc = Some("0\n"))
        )
        assertTrue(r.statusId == 3, r.stdout == "done\n", r.compileOutput.isEmpty)
      },
      test("InternalError maps to statusId 13") {
        val r = GoJudgeWire.parseRunResult(compiled = false)(resp(status = "InternalError"))
        assertTrue(r.statusId == 13)
      },
      test("malformed JSON throws a RuntimeException") {
        val ex = scala.util.Try(GoJudgeWire.parseRunResult(compiled = false)("not json")).failed.toOption
        assertTrue(
          ex.exists(_.isInstanceOf[RuntimeException]),
          ex.exists(_.getMessage.contains("invalid JSON"))
        )
      }
    )
  )
