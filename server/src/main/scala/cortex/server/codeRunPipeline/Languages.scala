package cortex.server.codeRunPipeline

import cortex.shared.api.Endpoints.RunnableLanguageInfo

/**
 * The single source of truth for every runnable language and everything the pipeline needs to dispatch it to
 * the execution backend.
 *
 * Each [[Language]] bundles:
 *   - `info` — the public, codegen'd API shape ([[RunnableLanguageInfo]]: a Judge0-derived id, a display
 *     label, and the aliases the markdown layer / user can type). The id is the canonical key the client
 *     uses.
 *   - `goJudge` — the [[GoJudgeSpec]]: how to compile and/or run this language inside the go-judge sandbox
 *     (source filename, optional compile command, run command, and resource limits). This replaces the old
 *     per-backend ids (Piston runtime name / Judge0 submission body) now that go-judge is the single backend
 *     — go-judge is a raw command runner, so dispatch is "what to exec", authored here once per language.
 *
 * Adding a language is one entry in [[all]], not a change spread across multiple files. The numeric `id` is
 * retained for client/codec stability (the client keys cached traces by it).
 */
object Languages:

  /** Hard limit on `source` size, in UTF-8 bytes. */
  val MaxSourceBytes: Int = 64 * 1024

  /** Hard limit on `stdin` size, in UTF-8 bytes. */
  val MaxStdinBytes: Int = 16 * 1024

  /**
   * How to build + run one language inside the go-judge sandbox.
   *
   *   - `sourceFile` — the filename the source is written to in the sandbox work dir (`/w`). Java is
   *     `Main.java` because the entrypoint class is normalised to `Main` (see [[effectiveSource]]).
   *   - `compile` — the compile command (shell), or `None` for interpreted languages. When present,
   *     [[GoJudgeWire]] wraps compile + run so a compile failure surfaces as a distinct `Compilation Error`
   *     (statusId 6) with the compiler output, mirroring the old Piston/Judge0 behaviour.
   *   - `run` — the command (shell) that runs the program (the compiled artifact, or the interpreter).
   *   - resource limits — per-language because JVM-based compilers (Kotlin, scala-cli) need far more time and
   *     memory than a Python interpreter. Defaults suit interpreted + fast native compiles.
   */
  final case class GoJudgeSpec(
      sourceFile: String,
      compile: Option[String],
      run: String,
      cpuSeconds: Int = 15,
      clockSeconds: Int = 30,
      memoryMiB: Int = 512
  )

  // id of Java — the one language whose source needs entrypoint normalisation before execution (the
  // `public class` name must match the on-disk file, which the sandbox writes as Main.java).
  private val JavaId: Int = 62

  val all: List[Language] = List(
    Language(
      RunnableLanguageInfo(id = 71, label = "Python 3", aliases = Seq("python", "py", "python3")),
      GoJudgeSpec(sourceFile = "main.py", compile = None, run = "python3 main.py")
    ),
    Language(
      RunnableLanguageInfo(id = JavaId, label = "Java 21 (OpenJDK)", aliases = Seq("java")),
      GoJudgeSpec(sourceFile = "Main.java", compile = Some("javac Main.java"), run = "java -cp . Main")
    ),
    Language(
      RunnableLanguageInfo(id = 81, label = "Scala 3", aliases = Seq("scala")),
      // scala-cli compiles + runs in one step; it fetches its toolchain via coursier (network), which the
      // sandbox denies — best-effort until the coursier cache is pre-warmed into the image (follow-up).
      GoJudgeSpec(
        sourceFile = "main.scala",
        compile = None,
        run = "scala-cli run main.scala --quiet --server=false",
        cpuSeconds = 60,
        clockSeconds = 120,
        memoryMiB = 1024
      )
    ),
    Language(
      RunnableLanguageInfo(id = 50, label = "C (GCC)", aliases = Seq("c")),
      GoJudgeSpec(sourceFile = "main.c", compile = Some("gcc -O2 main.c -o __cf_bin"), run = "./__cf_bin")
    ),
    Language(
      RunnableLanguageInfo(id = 54, label = "C++ (GCC)", aliases = Seq("cpp", "c++", "cxx")),
      GoJudgeSpec(sourceFile = "main.cpp", compile = Some("g++ -O2 main.cpp -o __cf_bin"), run = "./__cf_bin")
    ),
    Language(
      RunnableLanguageInfo(id = 60, label = "Go", aliases = Seq("go", "golang")),
      GoJudgeSpec(sourceFile = "main.go", compile = Some("go build -o __cf_bin main.go"), run = "./__cf_bin")
    ),
    Language(
      RunnableLanguageInfo(id = 73, label = "Rust", aliases = Seq("rust", "rs")),
      GoJudgeSpec(sourceFile = "main.rs", compile = Some("rustc -O main.rs -o __cf_bin"), run = "./__cf_bin")
    ),
    Language(
      RunnableLanguageInfo(id = 78, label = "Kotlin", aliases = Seq("kotlin", "kt")),
      GoJudgeSpec(
        sourceFile = "main.kt",
        compile = Some("kotlinc main.kt -include-runtime -d __cf.jar"),
        run = "java -jar __cf.jar",
        cpuSeconds = 60,
        clockSeconds = 90,
        memoryMiB = 1024
      )
    ),
    Language(
      RunnableLanguageInfo(id = 74, label = "TypeScript", aliases = Seq("typescript", "ts")),
      GoJudgeSpec(sourceFile = "main.ts", compile = None, run = "tsx main.ts")
    ),
    Language(
      RunnableLanguageInfo(
        id = 63,
        label = "JavaScript (Node.js)",
        aliases = Seq("javascript", "js", "node")
      ),
      GoJudgeSpec(sourceFile = "main.js", compile = None, run = "node main.js")
    ),
    Language(
      RunnableLanguageInfo(id = 82, label = "SQL (SQLite 3)", aliases = Seq("sql", "sqlite")),
      GoJudgeSpec(sourceFile = "main.sql", compile = None, run = "sqlite3 :memory: < main.sql")
    )
  )

  /** One runnable language plus its go-judge dispatch spec. See the object docstring. */
  final case class Language(info: RunnableLanguageInfo, goJudge: GoJudgeSpec):
    def id: Int              = info.id
    def label: String        = info.label
    def aliases: Seq[String] = info.aliases

  /** Lowercased alias → language. Built once at class-load. */
  private val aliasIndex: Map[String, Language] =
    all.flatMap(lang => lang.aliases.map(a => a.toLowerCase -> lang)).toMap

  /**
   * Resolve an alias (case-insensitive). Returns `None` for unknown languages so the pipeline can return a
   * 400.
   */
  def resolve(alias: String): Option[Language] =
    Option(alias).map(_.trim.toLowerCase).filter(_.nonEmpty).flatMap(aliasIndex.get)

  /**
   * Sentinel that tracer-wrapped sources carry as their very first line.
   *
   * [[effectiveSource]] skips [[JavaSourceRewriter.normalizeEntrypoint]] when this sentinel is present: the
   * harness already emits a valid `public class Main` and re-running normalisation would be a no-op at best
   * and a destructive word-boundary rename at worst if the user's inner class happens to share the first
   * top-level name.
   *
   * The constant is defined here (rather than in the client) so both the server bypass and the client harness
   * can share the same literal without duplication.
   */
  val TracerSentinel: String = "// __CF_TRACER__"

  /**
   * The source to actually write into the sandbox: Java gets its entrypoint class renamed to `Main` (see
   * [[JavaSourceRewriter]]); every other language passes through untouched.
   *
   * Exception: Java sources that begin with [[TracerSentinel]] skip normalisation — the JVM harness already
   * emits a well-formed `public class Main` wrapper and must not be altered.
   */
  def effectiveSource(lang: Language, source: String): String =
    if lang.id == JavaId && !source.trim.startsWith(TracerSentinel) then
      JavaSourceRewriter.normalizeEntrypoint(source)
    else source
