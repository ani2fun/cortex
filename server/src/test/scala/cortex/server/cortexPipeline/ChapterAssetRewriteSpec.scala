package cortex.server.cortexPipeline

import zio.test.*

object ChapterAssetRewriteSpec extends ZIOSpecDefault:

  private val Book   = "system-design"
  private val InPart = "01-foundations/04-cap-and-pacelc.md"

  private def rewrite(body: String, chapterPath: String = InPart): String =
    ChapterAssetRewrite.rewrite(body, Book, chapterPath)

  override def spec: Spec[Any, Any] = suite("ChapterAssetRewrite")(
    suite("HTML <img src=\"…\">")(
      test("./diagrams/foo.svg → /api/cortex/asset/<book>/<part>/diagrams/foo.svg") {
        val out = rewrite("""<img src="./diagrams/foo.svg" alt="x" />""")
        assertTrue(
          out == """<img src="/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg" alt="x" />"""
        )
      },
      test("diagrams/foo.svg (no leading ./) resolves the same way") {
        val out = rewrite("""<img src="diagrams/foo.svg" />""")
        assertTrue(
          out == """<img src="/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg" />"""
        )
      },
      test("../shared/bar.png escapes the part directory") {
        val out = rewrite("""<img src="../shared/bar.png" />""")
        assertTrue(out == """<img src="/api/cortex/asset/system-design/shared/bar.png" />""")
      },
      test("absolute / URL is left alone") {
        val out = rewrite("""<img src="/already/absolute.svg" />""")
        assertTrue(out == """<img src="/already/absolute.svg" />""")
      },
      test("http(s):// URL is left alone") {
        val out = rewrite("""<img src="https://example.com/foo.png" />""")
        assertTrue(out == """<img src="https://example.com/foo.png" />""")
      },
      test("data: URL is left alone") {
        val out = rewrite("""<img src="data:image/png;base64,AAA" />""")
        assertTrue(out == """<img src="data:image/png;base64,AAA" />""")
      }
    ),
    suite("HTML <a href=\"…\">")(
      test("href to a non-md asset gets rewritten") {
        val out = rewrite("""<a href="./examples/foo/file.zip">download</a>""")
        assertTrue(
          out == """<a href="/api/cortex/asset/system-design/01-foundations/examples/foo/file.zip">download</a>"""
        )
      },
      test("href to a .md chapter cross-link is left alone") {
        val out = rewrite("""<a href="../2.building-blocks/13-consistency-models.md">link</a>""")
        assertTrue(out == """<a href="../2.building-blocks/13-consistency-models.md">link</a>""")
      },
      test("in-page anchor #section left alone") {
        val out = rewrite("""<a href="#worked-example">jump</a>""")
        assertTrue(out == """<a href="#worked-example">jump</a>""")
      }
    ),
    suite("Markdown ![alt](url) and [text](url)")(
      test("![alt](./diagrams/foo.svg) → rewritten") {
        val out = rewrite("![cap](./diagrams/foo.svg)")
        assertTrue(out == "![cap](/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg)")
      },
      test("[text](./diagrams/foo.svg \"title\") preserves title attribute") {
        val out = rewrite("""[cap](./diagrams/foo.svg "C4 view")""")
        assertTrue(
          out == """[cap](/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg "C4 view")"""
        )
      },
      test("[text](./other-chapter.md) — .md chapter cross-link is left alone") {
        val out = rewrite("[next](./05-latency-throughput-usl.md)")
        assertTrue(out == "[next](./05-latency-throughput-usl.md)")
      },
      test("[text](./other-chapter.md#section) anchored chapter link also left alone") {
        val out = rewrite("[next](./05-latency-throughput-usl.md#intuition)")
        assertTrue(out == "[next](./05-latency-throughput-usl.md#intuition)")
      },
      test("[text](#section) in-page anchor left alone") {
        val out = rewrite("[jump](#worked-example)")
        assertTrue(out == "[jump](#worked-example)")
      }
    ),
    suite("regex safety")(
      test("body containing $ and \\ in code blocks is not corrupted by the replacement") {
        // Replace-with-quoted: dollar signs and backslashes in the source body must survive
        // verbatim. This used to be a classic regex-replace footgun.
        val body =
          """A bash example uses $HOME and `awk '{print $1}'`.
            |
            |<img src="./diagrams/foo.svg" />
            |
            |LaTeX: $\sqrt{x}$ and a literal \$ sign.""".stripMargin
        val out = rewrite(body)
        assertTrue(
          out.contains("$HOME"),
          out.contains("$1"),
          out.contains("\\$ sign"),
          out.contains("$\\sqrt{x}$"),
          out.contains("""<img src="/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg" />""")
        )
      },
      test("multiple <img> tags on consecutive lines all rewrite independently") {
        val body =
          """<img src="./diagrams/a.svg" />
            |<img src="./diagrams/b.svg" />
            |<img src="https://cdn.example.com/c.png" />""".stripMargin
        val out = rewrite(body)
        assertTrue(
          out.contains("/api/cortex/asset/system-design/01-foundations/diagrams/a.svg"),
          out.contains("/api/cortex/asset/system-design/01-foundations/diagrams/b.svg"),
          out.contains("https://cdn.example.com/c.png")
        )
      }
    ),
    suite("path normalisation")(
      test("./././foo collapses to foo") {
        val out = rewrite("""<img src="./././diagrams/foo.svg" />""")
        assertTrue(
          out == """<img src="/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg" />"""
        )
      },
      test("doubled slashes collapse") {
        val out = rewrite("""<img src=".//diagrams//foo.svg" />""")
        assertTrue(
          out == """<img src="/api/cortex/asset/system-design/01-foundations/diagrams/foo.svg" />"""
        )
      },
      test("chapter at book root (no part dir) gets a clean prefix") {
        val out = rewrite("""<img src="./diagrams/foo.svg" />""", chapterPath = "intro.md")
        assertTrue(out == """<img src="/api/cortex/asset/system-design/diagrams/foo.svg" />""")
      }
    ),
    suite("legacy /cortex prefix stripping")(
      test("HTML href /cortex/<book>/<slug> → /<book>/<slug>") {
        val out = rewrite("""<a href="/cortex/system-design/intro">x</a>""")
        assertTrue(out == """<a href="/system-design/intro">x</a>""")
      },
      test("markdown [text](/cortex/<book>/<slug>) → /<book>/<slug>") {
        val out = rewrite("[next](/cortex/data-structures-and-algorithms/arrays)")
        assertTrue(out == "[next](/data-structures-and-algorithms/arrays)")
      },
      test("/cortex alone → /") {
        val out = rewrite("""<a href="/cortex">index</a>""")
        assertTrue(out == """<a href="/">index</a>""")
      },
      test("/api/cortex/asset/... output is left untouched (not a /cortex/ link)") {
        val out = rewrite("""<img src="/api/cortex/asset/system-design/x.svg" />""")
        assertTrue(out == """<img src="/api/cortex/asset/system-design/x.svg" />""")
      },
      test("other absolute URLs (e.g. /blogs/...) are left alone") {
        val out = rewrite("""<a href="/blogs/foo">b</a>""")
        assertTrue(out == """<a href="/blogs/foo">b</a>""")
      }
    )
  )
