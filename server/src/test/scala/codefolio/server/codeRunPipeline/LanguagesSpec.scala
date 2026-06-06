package codefolio.server.codeRunPipeline

import zio.test.*

/**
 * Pins the Languages table as the single source of truth for the Code Run pipeline: alias resolution,
 * internal consistency (unique ids, unique aliases), and the per-language source preprocessing the wire
 * adapters delegate here. A duplicated id or alias — the kind of bug that used to hide across `GoJudgeWire` /
 * `JavaSourceRewriter` — fails here instead of at runtime.
 */
object LanguagesSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("Languages")(
    suite("resolve")(
      test("resolves a canonical alias") {
        assertTrue(Languages.resolve("python").map(_.id) == Some(71))
      },
      test("resolves a secondary alias") {
        assertTrue(
          Languages.resolve("py").map(_.id) == Some(71),
          Languages.resolve("golang").map(_.id) == Some(60)
        )
      },
      test("is case-insensitive and trims surrounding whitespace") {
        assertTrue(
          Languages.resolve("  PYTHON ").map(_.id) == Some(71),
          Languages.resolve("Java").map(_.id) == Some(62)
        )
      },
      test("returns None for an unknown or blank alias") {
        assertTrue(
          Languages.resolve("cobol").isEmpty,
          Languages.resolve("").isEmpty,
          Languages.resolve("   ").isEmpty
        )
      }
    ),
    suite("internal consistency")(
      test("every language has a unique Judge0 id") {
        val ids = Languages.all.map(_.id)
        assertTrue(ids.distinct.size == ids.size)
      },
      test("no alias is claimed by two languages") {
        val aliases = Languages.all.flatMap(_.aliases.map(_.toLowerCase))
        assertTrue(aliases.distinct.size == aliases.size)
      },
      test("every alias resolves back to its own language") {
        val mismatches =
          for
            lang  <- Languages.all
            alias <- lang.aliases
            if Languages.resolve(alias).map(_.id) != Some(lang.id)
          yield alias
        assertTrue(mismatches.isEmpty)
      },
      test("every language has a runnable go-judge spec") {
        val bad = Languages.all.filter(l => l.goJudge.sourceFile.isEmpty || l.goJudge.run.isEmpty)
        assertTrue(bad.isEmpty)
      }
    ),
    suite("effectiveSource")(
      test("rewrites the Java entrypoint class to Main") {
        val java = Languages.resolve("java").getOrElse(throw new IllegalStateException("java missing"))
        val src =
          """public class Solution {
            |  public static void main(String[] args) {}
            |}""".stripMargin
        val out = Languages.effectiveSource(java, src)
        assertTrue(out.contains("public class Main"), !out.contains("class Solution"))
      },
      test("passes non-Java source through untouched") {
        val python =
          Languages.resolve("python").getOrElse(throw new IllegalStateException("python missing"))
        val src = "class Solution:\n    pass"
        assertTrue(Languages.effectiveSource(python, src) == src)
      }
    )
  )
