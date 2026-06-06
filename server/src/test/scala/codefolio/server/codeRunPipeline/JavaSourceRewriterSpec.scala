package codefolio.server.codeRunPipeline

import zio.test.*

object JavaSourceRewriterSpec extends ZIOSpecDefault:

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private val java: Languages.Language =
    Languages.resolve("java").getOrElse(throw new IllegalStateException("java must be in Languages.all"))

  // ── Spec ────────────────────────────────────────────────────────────────────

  override def spec: Spec[Any, Any] = suite("JavaSourceRewriter")(
    suite("normalizeEntrypoint")(
      test("rewrites `public class Solution` and its self-references to Main") {
        val src =
          """public class Solution {
            |  public static void main(String[] args) {
            |    Solution s = new Solution();
            |    System.out.println(Solution.class.getName());
            |  }
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(
          out.contains("public class Main"),
          !out.contains("public class Solution"),
          out.contains("Main s = new Main()"),
          out.contains("Main.class.getName()")
        )
      },
      test("passes through when the public class is already Main") {
        val src =
          """public class Main {
            |  public static void main(String[] args) {
            |    System.out.println("hi");
            |  }
            |}""".stripMargin
        assertTrue(JavaSourceRewriter.normalizeEntrypoint(src) == src)
      },
      test("renames a non-public top-level class too — Judge0 runs `java Main` regardless") {
        val src =
          """class Helper {
            |  public static void main(String[] args) {
            |    System.out.println(1);
            |  }
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(out.contains("class Main"), !out.contains("class Helper"))
      },
      test("only renames word-boundary occurrences (does not match substrings)") {
        val src =
          """public class Sol {
            |  public static void main(String[] args) {
            |    String SolHelper = "do not touch substring SolHelper";
            |    Sol x = new Sol();
            |  }
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(
          out.contains("public class Main"),
          out.contains("Main x = new Main()"),
          out.contains("\"do not touch substring SolHelper\""),
          out.contains("String SolHelper =")
        )
      },
      test("handles modifier ordering — `public final class Foo`") {
        val src =
          """public final class Foo {
            |  public static void main(String[] args) {}
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(out.contains("public final class Main"), !out.contains("class Foo"))
      },
      test("handles `public abstract class Bar`") {
        val src =
          """public abstract class Bar {
            |  public static void main(String[] args) {}
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(out.contains("public abstract class Main"))
      },
      test("rewrites a non-public top-level `class Solution` (Judge0 fails to find Main otherwise)") {
        val src =
          """import java.util.*;
            |
            |class Solution {
            |  static class Node { int val; Node next; }
            |  public static void main(String[] args) { new Solution(); }
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(
          out.contains("class Main {"),
          !out.contains("class Solution"),
          out.contains("new Main()"),
          // The nested `static class Node` must NOT be touched.
          out.contains("static class Node {")
        )
      },
      test("does not touch indented (nested) class declarations") {
        val src =
          """class Outer {
            |  static class Inner {}
            |  public static void main(String[] args) {}
            |}""".stripMargin
        val out = JavaSourceRewriter.normalizeEntrypoint(src)
        assertTrue(
          out.contains("class Main"),
          // Inner stays Inner — we only renamed the outermost (top-level) class.
          out.contains("static class Inner {}")
        )
      },
      test("passes through when an existing `Main` class is present (avoid collision)") {
        val src =
          """class Helper {
            |  void aux() {}
            |}
            |public class Main {
            |  public static void main(String[] args) { new Helper(); }
            |}""".stripMargin
        assertTrue(JavaSourceRewriter.normalizeEntrypoint(src) == src)
      }
    ),
    suite("Languages.effectiveSource — tracer sentinel bypass")(
      // Risk R3 mitigation: a wrapped JVM harness source begins with `// __CF_TRACER__` so
      // Languages.effectiveSource skips JavaSourceRewriter.normalizeEntrypoint entirely.
      // Without this bypass the rewriter would rename the inner user-class inside the wrapper,
      // breaking the harness structure.
      test("Java source with leading sentinel is passed through untouched") {
        // Harness wrapper declares `public class Main`; the inner user-class is `Solution`.
        // Without the sentinel, the rewriter would see `Solution` as the top-level class and
        // attempt to rename it — corrupting the harness.
        val src =
          s"""${Languages.TracerSentinel}
             |public class Main {
             |  public static void main(String[] args) {
             |    Solution s = new Solution();
             |  }
             |  // inner user class — must NOT be renamed
             |  static class Solution {
             |    void run() {}
             |  }
             |}""".stripMargin
        val out = Languages.effectiveSource(java, src)
        assertTrue(
          out == src,
          out.contains("class Solution"),
          out.contains("public class Main")
        )
      },
      test("Java source with sentinel preceded only by whitespace is also passed through") {
        val src =
          s"""  ${Languages.TracerSentinel}
             |public class Main {}""".stripMargin
        val out = Languages.effectiveSource(java, src)
        assertTrue(out == src)
      },
      test("Java source WITHOUT sentinel still goes through normalizeEntrypoint") {
        val src =
          """public class Solution {
            |  public static void main(String[] args) {}
            |}""".stripMargin
        val out = Languages.effectiveSource(java, src)
        assertTrue(
          out.contains("public class Main"),
          !out.contains("public class Solution")
        )
      },
      test("non-Java language with sentinel is passed through (sentinel is Java-only guard)") {
        val python = Languages.resolve("python").getOrElse(
          throw new IllegalStateException("python must be in Languages.all")
        )
        val src = s"${Languages.TracerSentinel}\nprint('hello')"
        val out = Languages.effectiveSource(python, src)
        // Python always passes through — sentinel makes no difference here, but must not break it
        assertTrue(out == src)
      },
      test("sentinel value matches the documented literal") {
        // The sentinel literal is part of the public contract between server and client harness.
        // This test pins it so a typo in a future refactor fails loudly.
        assertTrue(Languages.TracerSentinel == "// __CF_TRACER__")
      }
    )
  )
