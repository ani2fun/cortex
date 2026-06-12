package cortex.server.codeRunPipeline

import cortex.server.codeRunPipeline.Languages.Language
import cortex.shared.api.Endpoints.RunResult
import io.circe.Json
import io.circe.parser.parse

/**
 * Pure wire-format adapter for the go-judge execution backend.
 *
 * go-judge (`criyle/go-judge`) is a raw command-runner sandbox: its `POST /run` takes a `cmd` describing argv
 * + files + resource limits + files to copy in/out, and returns each command's status, exit code, and
 * captured streams. Unlike Judge0 it has no `language_id` abstraction and no compile/run orchestration — that
 * lives here, driven by each language's [[Languages.GoJudgeSpec]].
 *
 * Compile + run happen in ONE sandbox invocation via `/bin/sh -c` (so Java's many `.class` files never have
 * to be enumerated for a separate run step). For compiled languages the wrapper records the compiler's exit
 * code (`__cf_crc`) and stderr (`__cf_cerr`) to files that are copied out, letting [[parseRunResult]]
 * reconstruct a distinct `Compilation Error` (statusId 6) with the compiler output — matching the
 * Piston/Judge0 semantics the client already renders.
 *
 * Exercised directly by `GoJudgeWireSpec` against golden fixtures so wire-level mapping bugs surface without
 * a stub HTTP server.
 */
private[codeRunPipeline] object GoJudgeWire:

  /** Path appended to the configured base URL. */
  val Path: String = "/run"

  // Marker files the compile wrapper writes into the work dir and copies out.
  private val CompileRcFile  = "__cf_crc"
  private val CompileErrFile = "__cf_cerr"

  /** Max bytes captured per output stream (matches the prior Code Runner cap). */
  private val MaxOutputBytes: Int = 1024 * 1024

  /** Process/thread ceiling — a JVM + JIT + GC + in-process compiler comfortably exceed the usual 60. */
  private val ProcLimit: Int = 256

  /**
   * Build the go-judge `/run` request body. The source is written to the language's `sourceFile` in the
   * sandbox work dir (`/w`); Java is entrypoint-normalised first via [[Languages.effectiveSource]].
   */
  def buildRequestBody(source: String, stdin: Option[String], lang: Language): String =
    val spec = lang.goJudge
    val src  = Languages.effectiveSource(lang, source)
    val copyOut = spec.compile match
      // `?` marks the file optional so a missing marker never fails the run.
      case Some(_) => Json.arr(Json.fromString(s"$CompileRcFile?"), Json.fromString(s"$CompileErrFile?"))
      case None    => Json.arr()
    val cmd = Json.obj(
      "args" -> Json.arr(Json.fromString("/bin/sh"), Json.fromString("-c"), Json.fromString(shellBody(spec))),
      "env" -> Json.arr(
        Json.fromString("PATH=/usr/bin:/bin:/usr/local/bin"),
        Json.fromString("HOME=/w"),
        Json.fromString("GOCACHE=/w/.cache"),
        Json.fromString("GOPATH=/w/go")
      ),
      "files" -> Json.arr(
        Json.obj("content" -> Json.fromString(stdin.getOrElse(""))), // fd 0: stdin
        Json.obj("name"    -> Json.fromString("stdout"), "max" -> Json.fromInt(MaxOutputBytes)), // fd 1
        Json.obj("name"    -> Json.fromString("stderr"), "max" -> Json.fromInt(MaxOutputBytes))  // fd 2
      ),
      "cpuLimit"    -> Json.fromLong(spec.cpuSeconds.toLong * 1_000_000_000L),
      "clockLimit"  -> Json.fromLong(spec.clockSeconds.toLong * 1_000_000_000L),
      "memoryLimit" -> Json.fromLong(spec.memoryMiB.toLong * 1024L * 1024L),
      "procLimit"   -> Json.fromInt(ProcLimit),
      "copyIn"      -> Json.obj(spec.sourceFile -> Json.obj("content" -> Json.fromString(src))),
      "copyOut"     -> copyOut
    )
    Json.obj("cmd" -> Json.arr(cmd)).noSpaces

  // Interpreted languages just run. Compiled languages run the compiler first, capturing its exit code +
  // stderr to marker files; if it failed we exit 0 WITHOUT running (go-judge reports Accepted) and
  // parseRunResult detects the non-zero compile rc.
  private def shellBody(spec: Languages.GoJudgeSpec): String =
    spec.compile match
      case None => spec.run
      case Some(c) =>
        s"""$c 2>$CompileErrFile; echo $$? >$CompileRcFile; """ +
          s"""if [ "$$(cat $CompileRcFile)" != "0" ]; then exit 0; fi; ${spec.run}"""

  /**
   * Parse a go-judge `/run` response into our canonical [[RunResult]].
   *
   * `compiled` selects whether to inspect the compile-marker files. go-judge returns an array (one entry per
   * `cmd` — we send one); output streams are RAW (not base64); `time` is nanoseconds and `memory` is bytes,
   * both normalised here to the Judge0-shaped [[RunResult]] contract (seconds-as-string, KB).
   */
  def parseRunResult(compiled: Boolean)(body: String): RunResult =
    val json = parse(body) match
      case Right(j) => j
      case Left(e)  => throw new RuntimeException(s"go-judge returned invalid JSON: ${e.getMessage}")

    val r          = json.hcursor.downN(0)
    val status     = r.get[String]("status").getOrElse("InternalError")
    val exitStatus = r.get[Int]("exitStatus").getOrElse(0)
    val files      = r.downField("files")
    val stdout     = files.get[String]("stdout").toOption.getOrElse("")
    val stderr     = files.get[String]("stderr").toOption.getOrElse("")
    val memory     = r.get[Long]("memory").toOption.map(_ / 1024L)
    val time       = r.get[Long]("time").toOption.map(ns => f"${ns.toDouble / 1e9}%.3f")

    val compileFailed =
      compiled && files.get[String](CompileRcFile).toOption.exists(rc => rc.trim.nonEmpty && rc.trim != "0")

    if compileFailed then
      RunResult(
        stdout = "",
        stderr = "",
        compileOutput = files.get[String](CompileErrFile).toOption.getOrElse(""),
        statusId = 6,
        statusDescription = "Compilation Error",
        time = time,
        memory = memory
      )
    else
      val (statusId, statusDescription) = status match
        case "Accepted" if exitStatus == 0 => (3, "Accepted")
        case "TimeLimitExceeded"           => (5, "Time Limit Exceeded")
        case "InternalError" | "FileError" => (13, "Internal Error")
        case _                             => (11, "Runtime Error (NZEC)")
      RunResult(
        stdout = stdout,
        stderr = stderr,
        compileOutput = "",
        statusId = statusId,
        statusDescription = statusDescription,
        time = time,
        memory = memory
      )
