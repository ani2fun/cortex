package cortex.shared.runner

/**
 * Execution limits for the code-run pipeline's backend (go-judge).
 *
 * The tracer harness uses [[Limits.maxStdoutBytes]] to size its dynamic-truncation budget (the Python-harness
 * "quarter-drop" loop, ported for Java). Both client and server can import this object — it lives in `shared`
 * so neither side needs to call an endpoint just to know the backend's caps.
 *
 * Hard-coded for now (no `/api/runner-info` endpoint). `maxStdoutBytes` matches the go-judge per-stream
 * collector cap configured in [[cortex.server.codeRunPipeline.GoJudgeWire]] (1 MB).
 */
object BackendLimits:

  /** Execution limits for one backend. */
  final case class Limits(
      /** Maximum bytes accepted from the program's stdout stream. */
      maxStdoutBytes: Int,
      /** Maximum bytes the backend will accept as the source payload. */
      maxSourceBytes: Int,
      /** Default run-time timeout in milliseconds (client-side budget hint). */
      defaultRunTimeoutMs: Int
  )

  /** Limits for the self-hosted go-judge backend. */
  val goJudge: Limits = Limits(
    maxStdoutBytes = 1024 * 1024, // 1 MB — matches GoJudgeWire's per-stream collector cap
    maxSourceBytes = 64 * 1024,   // 64 KB
    defaultRunTimeoutMs = 10_000
  )
