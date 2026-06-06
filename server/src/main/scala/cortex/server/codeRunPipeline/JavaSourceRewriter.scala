package cortex.server.codeRunPipeline

import java.util.regex.Pattern

/**
 * Java's "public class name must match the file name" rule — combined with the fact that neither Judge0 nor
 * Piston let us specify a file name for the user's source — means a snippet that declares `public class
 * Solution { ... }` fails compilation on both backends, because the source is written to disk as `Main.java`
 * and the compiler refuses (`class Solution is public, should be declared in a file named Solution.java`).
 * The DSA chapters use `Solution` (and similar) as the idiomatic entry-point class.
 *
 * Fix: rewrite the source so the public class is `Main` before sending it to either backend. Word-boundary
 * substitution catches the declaration plus any self-references (e.g. `new Solution(); Solution.foo()`).
 * Anything else — no public class, already named `Main`, or non-Java languages — passes through untouched.
 *
 * Lives in `codeRunPipeline` and is invoked via [[Languages.effectiveSource]] — the wire adapters call that
 * rather than this directly, so [[GoJudgeWire]] doesn't have to know which language is special. The shape
 * (single static `normalizeEntrypoint`) is small enough to keep beside the table rather than promote to
 * `shared/`.
 */
private[codeRunPipeline] object JavaSourceRewriter:

  // Top-level class declaration anchored at line start (multiline mode). Optional `public`,
  // optional combination of other class-modifier keywords, then `class <Name>`. Anchoring at
  // line start avoids matching inner / nested classes (which are indented).
  private val topLevelClassPattern =
    """(?m)^(?:public\s+)?(?:(?:final|abstract|static|sealed|non-sealed)\s+)*class\s+(\w+)\b""".r

  // Quick check for an existing `Main` class anywhere — top-level or nested. If the source
  // already names a class `Main`, renaming another class to `Main` would collide.
  private val anyMainClassPattern =
    """\bclass\s+Main\b""".r

  /**
   * If `source` declares a top-level `class <Name>` where `<Name>` is not `Main`, rename every word-boundary
   * occurrence of `<Name>` to `Main` so the JVM can find the entry point. Covers both `public class X`
   * (compile-time error: file name mismatch) and plain `class X` (runtime error: Judge0 runs `java Main`
   * after writing the source as Main.java). Only the first top-level class is renamed (Judge0 / Piston
   * compile a single file; multiple top-level classes are uncommon in DSA snippets). Source that already
   * declares any `Main` class — top-level or nested — passes through to avoid collisions.
   */
  def normalizeEntrypoint(source: String): String =
    if anyMainClassPattern.findFirstIn(source).isDefined then source
    else
      topLevelClassPattern.findFirstMatchIn(source) match
        case Some(m) if m.group(1) != "Main" =>
          val original = m.group(1)
          source.replaceAll(s"\\b${Pattern.quote(original)}\\b", "Main")
        case _ => source
