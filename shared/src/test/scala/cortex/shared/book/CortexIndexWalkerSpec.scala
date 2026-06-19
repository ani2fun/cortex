package cortex.shared.book

import cortex.shared.book.CortexIndexWalker.{
  BookMeta,
  CortexDir,
  CortexEntry,
  CortexFile,
  IndexError,
  SectionMeta
}
import zio.test.*

object CortexIndexWalkerSpec extends ZIOSpecDefault:

  // ===========================================================================
  // Tree-construction helpers — keep tests readable without inventing a DSL.
  // ===========================================================================

  private def chapter(name: String, content: String = ""): CortexFile =
    CortexFile(name, content)

  private def book(
      slug: String,
      meta: Option[BookMeta] = None,
      children: List[CortexEntry] = Nil
  ): CortexDir =
    CortexDir(slug, bookMeta = meta, children = children)

  private def section(
      name: String,
      meta: Option[SectionMeta] = None,
      children: List[CortexEntry] = Nil
  ): CortexDir =
    CortexDir(name, sectionMeta = meta, children = children)

  override def spec: Spec[Any, Any] = suite("CortexIndexWalker")(
    suite("walk - happy paths")(
      test("empty roots produces an empty index") {
        val result = CortexIndexWalker.walk(Nil)
        assertTrue(
          result.isRight,
          result.toOption.exists(r => r.index.books.isEmpty && r.reverseMaps.isEmpty)
        )
      },
      test("a single chapter at book root yields one Book with one ChapterRef") {
        val tree     = book("intro-book", children = List(chapter("hello.md", "# Hello\n\nbody")))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val books    = r.index.books
        assertTrue(
          books.size == 1,
          books.head.slug == "intro-book",
          books.head.title == "Intro Book",
          books.head.description == "",
          books.head.tags.isEmpty,
          books.head.estimatedReadingMinutes.isEmpty,
          books.head.chapters.size == 1,
          books.head.chapters.head.slug == "hello",
          books.head.chapters.head.title == "Hello",
          books.head.chapters.head.groupPath.isEmpty
        )
      },
      test("numeric prefixes order chapters within a book") {
        val tree = book(
          "ds",
          children = List(
            chapter("10-late.md", "# Late"),
            chapter("01-first.md", "# First"),
            chapter("02-second.md", "# Second")
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(
          r.index.books.head.chapters.map(_.slug) == List("first", "second", "late")
        )
      },
      test("'index.md' sorts before numbered siblings within its directory") {
        // Each Part has its own index.md that introduces the section. It should land in the sidebar
        // *above* the part's first numbered lesson, not after the last one.
        val tree = book(
          "guide",
          children = List(
            section(
              "01-foundations",
              children = List(
                chapter("03-third.md", "# Third"),
                chapter("01-first.md", "# First"),
                chapter("index.md", "# Foundations overview"),
                chapter("02-second.md", "# Second")
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(
          r.index.books.head.chapters.map(_.slug) ==
            List("foundations/index", "foundations/first", "foundations/second", "foundations/third")
        )
      },
      test("nested sections become groupPath entries; chapter slug joins all order-stripped segments") {
        val tree = book(
          "guide",
          children = List(
            chapter("01-start-here.md", "# Start Here"),
            section(
              "02-system",
              meta = Some(SectionMeta(title = Some("System"), summary = None)),
              children = List(chapter("01-next-step.md", "# Next Step"))
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val chapters = r.index.books.head.chapters
        assertTrue(
          chapters.map(_.slug) == List("start-here", "system/next-step"),
          chapters.head.groupPath.isEmpty,
          chapters(1).groupPath == Some(Seq("System"))
        )
      },
      test("a chapter's frontmatter `group:` lifts it into a SEPARATE top-level section, slug unchanged") {
        val tree = book(
          "guide",
          children = List(
            section(
              "07-capstones",
              meta = Some(SectionMeta(title = Some("Capstones"), summary = None)),
              children = List(
                chapter("37-url-shortener.md", "---\ntitle: 'URL shortener'\n---\n# URL shortener"),
                chapter(
                  "47-cortex-overview.md",
                  "---\ntitle: 'Cortex'\ngroup: 'Cortex Platform'\n---\n# Cortex"
                )
              )
            )
          )
        )
        val Right(r)           = CortexIndexWalker.walk(List(tree)): @unchecked
        val chapters           = r.index.books.head.chapters
        def find(slug: String) = chapters.find(_.slug == slug)
        assertTrue(
          // The slug is the order-stripped directory path — `group:` leaves it untouched (no URL churn).
          find("capstones/cortex-overview").isDefined,
          // A plain capstone stays under its directory section.
          find("capstones/url-shortener").exists(_.groupPath == Some(Seq("Capstones"))),
          // The grouped capstone is lifted OUT into its own top-level section — sibling to Capstones, not nested.
          find("capstones/cortex-overview").exists(_.groupPath == Some(Seq("Cortex Platform")))
        )
      },
      test("section title falls back to humanised dir name when SectionMeta is absent") {
        val tree = book(
          "guide",
          children = List(
            section(
              "data-structures",
              children = List(chapter("intro.md", "# Intro"))
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(
          r.index.books.head.chapters.head.groupPath == Some(Seq("Data Structures"))
        )
      },
      test("BookMeta propagates title, description, tags, estimatedReadingMinutes") {
        val tree = book(
          "ds",
          meta = Some(
            BookMeta(
              title = Some("Data Structures"),
              description = Some("Architecture notes"),
              tags = Some(Seq("scala", "zio")),
              estimatedReadingMinutes = Some(12)
            )
          ),
          children = List(chapter("01-arrays.md", "# Arrays"))
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val b        = r.index.books.head
        assertTrue(
          b.title == "Data Structures",
          b.description == "Architecture notes",
          b.tags == Some(Seq("scala", "zio")),
          b.estimatedReadingMinutes == Some(12)
        )
      },
      test("BookMeta absent → title humanises slug; description empty; tags/mins absent") {
        val tree     = book("data-structures", children = List(chapter("intro.md", "# Intro")))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val b        = r.index.books.head
        assertTrue(
          b.title == "Data Structures",
          b.description == "",
          b.tags.isEmpty,
          b.estimatedReadingMinutes.isEmpty
        )
      },
      test("books sort by BookMeta.order; unordered books fall to the end alphabetically") {
        val withOrder = (slug: String, ord: Int) =>
          book(
            slug,
            meta = Some(BookMeta(None, None, None, None, Some(ord))),
            children = List(chapter("c.md", "# C"))
          )
        val tree = List(
          withOrder("zeta", 2),
          withOrder("alpha", 1),
          book("yak", children = List(chapter("c.md", "# C"))),
          book("box", children = List(chapter("c.md", "# C")))
        )
        val Right(r) = CortexIndexWalker.walk(tree): @unchecked
        assertTrue(r.index.books.map(_.slug) == List("alpha", "zeta", "box", "yak"))
      },
      test("frontmatter title wins over body H1 for chapter title") {
        val raw = """---
                    |title: Frontmatter Wins
                    |---
                    |# H1 Loses""".stripMargin
        val tree     = book("b", children = List(chapter("01-c.md", raw)))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.title == "Frontmatter Wins")
      },
      test("first H1 is used as chapter title when no frontmatter title is present") {
        val tree     = book("b", children = List(chapter("01-c.md", "# H1 Wins\n\nbody")))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.title == "H1 Wins")
      },
      test("humanised filename is used when neither frontmatter title nor H1 is present") {
        val tree     = book("b", children = List(chapter("01-singly-linked-list.md", "no heading\n")))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.title == "Singly Linked List")
      }
    ),
    suite("walk - filtering & skipping")(
      test("non-slug-like top-level dirs are silently skipped") {
        val tree = List(
          book("real-book", children = List(chapter("01-c.md", "# C"))),
          CortexDir(name = "_drafts", children = List(chapter("c.md", ""))),
          CortexDir(name = ".git", children = Nil)
        )
        val Right(r) = CortexIndexWalker.walk(tree): @unchecked
        assertTrue(r.index.books.map(_.slug) == Seq("real-book"))
      },
      test("files at root are silently skipped (only dirs become books)") {
        val tree = List(
          chapter("README.md", "# readme"),
          book("real-book", children = List(chapter("01-c.md", "# C")))
        )
        val Right(r) = CortexIndexWalker.walk(tree): @unchecked
        assertTrue(r.index.books.map(_.slug) == Seq("real-book"))
      },
      test("section dirs starting with '_' or '.' are skipped") {
        val tree = book(
          "b",
          children = List(
            chapter("01-c.md", "# C"),
            section("_hidden", children = List(chapter("inner.md", "# Inner"))),
            section(".cache", children = List(chapter("cached.md", "# X")))
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("c"))
      },
      test("section dirs named 'examples' are skipped (companion source code, not chapters)") {
        // Runnable example projects sit next to lessons under examples/<NN>-<slug>/. Their
        // README.md would otherwise appear as a rogue chapter in the sidebar.
        val tree = book(
          "system-design",
          children = List(
            chapter("01-intro.md", "# Intro"),
            section(
              "examples",
              children = List(
                section(
                  "04-cap-pacelc-simulator",
                  children = List(chapter("README.md", "# How to run"))
                )
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("intro"))
      },
      test("'01-examples' is also skipped — the order prefix doesn't change the role") {
        val tree = book(
          "b",
          children = List(
            chapter("01-c.md", "# C"),
            section("01-examples", children = List(chapter("README.md", "# How to run")))
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("c"))
      },
      test("section dirs named 'c4' are skipped (LikeC4 source files, not chapters)") {
        // Each Part holds its lesson .c4 sources in a sibling c4/ directory; the
        // LikeC4 build pipeline collects them into the SPA project root. Cortex
        // must not try to render them as chapters even if a stray .md lands
        // there during authoring.
        val tree = book(
          "system-design",
          children = List(
            chapter("01-intro.md", "# Intro"),
            section(
              "01-foundations",
              children = List(
                chapter("01-what.md", "# What"),
                section(
                  "c4",
                  children = List(chapter("authoring-notes.md", "# Author notes"))
                )
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("intro", "foundations/what"))
      },
      test("a book at the top level literally named 'examples' is skipped too") {
        val tree = List(
          CortexDir(name = "examples", children = List(chapter("README.md", "# top-level"))),
          book("real-book", children = List(chapter("01-c.md", "# C")))
        )
        val Right(r) = CortexIndexWalker.walk(tree): @unchecked
        assertTrue(r.index.books.map(_.slug) == Seq("real-book"))
      },
      test("chapter files starting with '_' or '.' are skipped") {
        val tree = book(
          "b",
          children = List(
            chapter("01-c.md", "# C"),
            chapter("_draft.md", "# Draft"),
            chapter(".hidden.md", "# Hidden")
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("c"))
      },
      test("non-.md files inside a section are not chapters") {
        val tree = book(
          "b",
          children = List(
            chapter("notes.txt", "ignored"),
            chapter("01-c.md", "# C")
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.map(_.slug) == List("c"))
      }
    ),
    suite("walk - errors")(
      test("duplicate chapter slugs surface as IndexError.DuplicateSlug") {
        val tree = book(
          "b",
          children = List(
            chapter("01-overview.md", "# First"),
            chapter("02-overview.md", "# Second")
          )
        )
        val result = CortexIndexWalker.walk(List(tree))
        result match
          case Left(IndexError.DuplicateSlug(bookSlug, slugs)) =>
            assertTrue(bookSlug == "b", slugs == List("overview"))
          case other => assertNever(s"expected DuplicateSlug, got $other")
      },
      test("section depth at MaxSectionDepth (6) is allowed") {
        val deepest = chapter("c.md", "# C")
        val tree = (1 to CortexIndexWalker.MaxSectionDepth)
          .foldRight[CortexEntry](deepest) { (i, inner) =>
            section(s"s$i", children = List(inner))
          }
        val root     = book("b", children = List(tree))
        val Right(r) = CortexIndexWalker.walk(List(root)): @unchecked
        assertTrue(
          r.index.books.head.chapters.size == 1,
          r.index.books.head.chapters.head.groupPath.exists(_.size == CortexIndexWalker.MaxSectionDepth)
        )
      },
      test("section depth above MaxSectionDepth surfaces as MaxDepthExceeded") {
        val deepest = chapter("c.md", "# C")
        val tree = (1 to CortexIndexWalker.MaxSectionDepth + 1)
          .foldRight[CortexEntry](deepest) { (i, inner) =>
            section(s"s$i", children = List(inner))
          }
        val root = book("b", children = List(tree))
        CortexIndexWalker.walk(List(root)) match
          case Left(IndexError.MaxDepthExceeded(path)) =>
            assertTrue(path.startsWith("s1/"))
          case other => assertNever(s"expected MaxDepthExceeded, got $other")
      }
    ),
    suite("reverseMap")(
      test("each chapter slug maps to its in-book relative file path") {
        val tree = book(
          "guide",
          children = List(
            chapter("01-start.md", "# Start"),
            section(
              "02-system",
              meta = Some(SectionMeta(title = Some("System"), summary = None)),
              children = List(chapter("01-next.md", "# Next"))
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val map      = r.reverseMaps("guide")
        assertTrue(
          map("start") == "01-start.md",
          map("system/next") == "02-system/01-next.md"
        )
      }
    ),
    suite("essential / optional cascade")(
      test("chapter inherits essential = true at book root when no metadata is present") {
        val tree     = book("b", children = List(chapter("01-c.md", "# C")))
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.essential == Some(true))
      },
      test("_section.json#defaultStatus = optional flips the cascade for descendants") {
        val tree = book(
          "b",
          children = List(
            section(
              "01-extras",
              meta = Some(SectionMeta(title = None, summary = None, defaultStatus = Some("optional"))),
              children = List(
                chapter("01-a.md", "# A"),
                chapter("02-b.md", "# B")
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        val flags    = r.index.books.head.chapters.map(_.essential)
        assertTrue(flags == List(Some(false), Some(false)))
      },
      test("chapter frontmatter essential: true wins against an optional section default") {
        val raw = """---
                    |essential: true
                    |---
                    |# Mandatory exception""".stripMargin
        val tree = book(
          "b",
          children = List(
            section(
              "01-extras",
              meta = Some(SectionMeta(None, None, defaultStatus = Some("optional"))),
              children = List(chapter("01-must-read.md", raw))
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.essential == Some(true))
      },
      test("optional default cascades through nested sections without re-statement") {
        val tree = book(
          "b",
          children = List(
            section(
              "01-extras",
              meta = Some(SectionMeta(None, None, defaultStatus = Some("optional"))),
              children = List(
                section(
                  "01-nested",
                  children = List(chapter("01-inner.md", "# Inner"))
                )
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.essential == Some(false))
      },
      test("a nested section can flip the cascade back to essential") {
        val tree = book(
          "b",
          children = List(
            section(
              "01-extras",
              meta = Some(SectionMeta(None, None, defaultStatus = Some("optional"))),
              children = List(
                section(
                  "01-must-read-pocket",
                  meta = Some(SectionMeta(None, None, defaultStatus = Some("essential"))),
                  children = List(chapter("01-inner.md", "# Inner"))
                )
              )
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.essential == Some(true))
      },
      test("unrecognised defaultStatus values fall through silently to inheritance") {
        val tree = book(
          "b",
          children = List(
            section(
              "01-extras",
              meta = Some(SectionMeta(None, None, defaultStatus = Some("yolo"))),
              children = List(chapter("01-a.md", "# A"))
            )
          )
        )
        val Right(r) = CortexIndexWalker.walk(List(tree)): @unchecked
        assertTrue(r.index.books.head.chapters.head.essential == Some(true))
      }
    ),
    suite("public helpers")(
      test("slugify lowercases and collapses non-alphanumerics to single hyphens") {
        assertTrue(
          CortexIndexWalker.slugify("Hello World!") == "hello-world",
          CortexIndexWalker.slugify("foo--bar") == "foo-bar",
          CortexIndexWalker.slugify("keep_underscore") == "keep_underscore",
          CortexIndexWalker.slugify("-trim-") == "trim"
        )
      },
      test("humanise strips order prefix and capitalises each word") {
        assertTrue(
          CortexIndexWalker.humanise("01-singly-linked-list") == "Singly Linked List",
          CortexIndexWalker.humanise("data-structures") == "Data Structures",
          CortexIndexWalker.humanise("1.intro") == "Intro"
        )
      },
      test("stripOrderPrefix removes leading digits with optional separator") {
        assertTrue(
          CortexIndexWalker.stripOrderPrefix("01-foo") == "foo",
          CortexIndexWalker.stripOrderPrefix("1.bar") == "bar",
          CortexIndexWalker.stripOrderPrefix("10_baz") == "baz",
          CortexIndexWalker.stripOrderPrefix("noprefix") == "noprefix"
        )
      },
      test("slugLike accepts letters, digits, hyphens, underscores; rejects others") {
        assertTrue(
          CortexIndexWalker.slugLike("hello-world_2"),
          !CortexIndexWalker.slugLike(""),
          !CortexIndexWalker.slugLike("has space"),
          !CortexIndexWalker.slugLike("has.dot")
        )
      },
      test("chapterPathLike accepts '/'-joined slug segments; rejects empty segments and '..'") {
        assertTrue(
          CortexIndexWalker.chapterPathLike("arrays"),
          CortexIndexWalker.chapterPathLike("linear-structures/arrays/two-sum"),
          !CortexIndexWalker.chapterPathLike(""),
          !CortexIndexWalker.chapterPathLike("a//b"),
          !CortexIndexWalker.chapterPathLike("/leading"),
          !CortexIndexWalker.chapterPathLike("trailing/"),
          !CortexIndexWalker.chapterPathLike("a/../b")
        )
      }
    )
  )
