package cortex.shared.book

import cortex.shared.book.Blocks.RawTab
import zio.test.*

object BlocksSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("Blocks")(
    suite("decodeRunnableCode")(
      test("happy path — language + source present, label present") {
        val r = Blocks.decodeRunnableCode(Some("python"), Some("print('hi')"), Some("Python"))
        assertTrue(r == Right(Block.RunnableCode("python", "print('hi')", Some("Python"))))
      },
      test("happy path — language + source present, label absent") {
        val r = Blocks.decodeRunnableCode(Some("scala"), Some("println(1)"), None)
        assertTrue(r == Right(Block.RunnableCode("scala", "println(1)", None)))
      },
      test("empty languageLabel collapses to None (no UI difference between missing and empty)") {
        val r = Blocks.decodeRunnableCode(Some("c"), Some("int main(){}"), Some(""))
        assertTrue(r == Right(Block.RunnableCode("c", "int main(){}", None)))
      },
      test("missing language → MissingAttribute(data-lang)") {
        val r = Blocks.decodeRunnableCode(None, Some("print('hi')"), None)
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("runnable-code", "data-lang")))
      },
      test("missing source → MissingAttribute(data-source)") {
        val r = Blocks.decodeRunnableCode(Some("python"), None, None)
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("runnable-code", "data-source")))
      }
    ),
    suite("decodeStandaloneCoach")(
      test("author override present → Some(problemId)") {
        assertTrue(
          Blocks.decodeStandaloneCoach(Some("system-design/capstones/url-shortener")) ==
            Right(Block.StandaloneCoach(Some("system-design/capstones/url-shortener")))
        )
      },
      test("absent override → None (mounter falls back to the chapter problemId)") {
        assertTrue(Blocks.decodeStandaloneCoach(None) == Right(Block.StandaloneCoach(None)))
      }
    ),
    suite("decodeConceptCoach")(
      test("author override present → Some(problemId)") {
        assertTrue(
          Blocks.decodeConceptCoach(Some("system-design/foundations/cap-and-pacelc")) ==
            Right(Block.ConceptCoach(Some("system-design/foundations/cap-and-pacelc")))
        )
      },
      test("absent override → None (mounter falls back to the chapter problemId)") {
        assertTrue(Blocks.decodeConceptCoach(None) == Right(Block.ConceptCoach(None)))
      }
    ),
    suite("decodeRunnableGroup")(
      test("happy path — every tab fully populated") {
        val raws = List(
          RawTab(Some("python"), Some("Python"), Some("print('hi')"), Some(true)),
          RawTab(Some("scala"), Some("Scala"), Some("println(1)"), Some(false))
        )
        val r = Blocks.decodeRunnableGroup(raws)
        assertTrue(
          r == Right(
            Block.RunnableGroup(
              List(
                Block.Tab("python", "Python", "print('hi')", true),
                Block.Tab("scala", "Scala", "println(1)", false)
              )
            )
          )
        )
      },
      test("absent runnable defaults to true") {
        val raws = List(RawTab(Some("py"), Some("Python"), Some("x = 1"), None))
        val r    = Blocks.decodeRunnableGroup(raws)
        assertTrue(r == Right(Block.RunnableGroup(List(Block.Tab("py", "Python", "x = 1", true)))))
      },
      test("empty rawTabs → EmptyContent(tabs)") {
        val r = Blocks.decodeRunnableGroup(Nil)
        assertTrue(r == Left(BlockDecodeError.EmptyContent("runnable-group", "tabs")))
      },
      test("first tab missing language → MalformedTab(0, language)") {
        val raws = List(RawTab(None, Some("Python"), Some("x"), None))
        val r    = Blocks.decodeRunnableGroup(raws)
        assertTrue(r == Left(BlockDecodeError.MalformedTab(0, "language")))
      },
      test("second tab missing source → MalformedTab(1, source) — index reflects tab position") {
        val raws = List(
          RawTab(Some("py"), Some("Python"), Some("x"), None),
          RawTab(Some("sc"), Some("Scala"), None, None)
        )
        val r = Blocks.decodeRunnableGroup(raws)
        assertTrue(r == Left(BlockDecodeError.MalformedTab(1, "source")))
      },
      test("first error short-circuits — later malformed tabs don't change the reason") {
        val raws = List(
          RawTab(None, Some("Python"), Some("x"), None),
          RawTab(None, None, None, None) // also malformed; should not surface
        )
        val r = Blocks.decodeRunnableGroup(raws)
        assertTrue(r == Left(BlockDecodeError.MalformedTab(0, "language")))
      }
    ),
    suite("decodeMermaid")(
      test("happy path — source present") {
        val r = Blocks.decodeMermaid(Some("graph TD; A-->B"))
        assertTrue(r == Right(Block.Mermaid("graph TD; A-->B")))
      },
      test("missing source → MissingAttribute(data-mermaid-source)") {
        val r = Blocks.decodeMermaid(None)
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("mermaid-block", "data-mermaid-source")))
      }
    ),
    suite("decodeD2Slides")(
      test("happy path — slides present, caption absent") {
        val r = Blocks.decodeD2Slides(List("<svg>slide-1</svg>", "<svg>slide-2</svg>"), None)
        assertTrue(r == Right(Block.D2Slides(List("<svg>slide-1</svg>", "<svg>slide-2</svg>"), None)))
      },
      test("happy path — slides + caption present") {
        val r = Blocks.decodeD2Slides(List("<svg>x</svg>"), Some("Phase one"))
        assertTrue(r == Right(Block.D2Slides(List("<svg>x</svg>"), Some("Phase one"))))
      },
      test("empty caption normalises to None (matches pre-extraction behaviour)") {
        val r = Blocks.decodeD2Slides(List("<svg>x</svg>"), Some(""))
        assertTrue(r == Right(Block.D2Slides(List("<svg>x</svg>"), None)))
      },
      test("empty slides → EmptyContent(slides)") {
        val r = Blocks.decodeD2Slides(Nil, Some("Phase one"))
        assertTrue(r == Left(BlockDecodeError.EmptyContent("d2-slides", "slides")))
      }
    ),
    suite("decodeD2Inline")(
      test("happy path — non-empty SVG") {
        val r = Blocks.decodeD2Inline("<svg>...</svg>")
        assertTrue(r == Right(Block.D2Inline("<svg>...</svg>")))
      },
      test("empty inner HTML → EmptyContent(innerHTML)") {
        val r = Blocks.decodeD2Inline("")
        assertTrue(r == Left(BlockDecodeError.EmptyContent("d2-diagram", "innerHTML")))
      }
    ),
    suite("decodeD3Widget")(
      test("happy path — widget + payload present") {
        val r = Blocks.decodeD3Widget(Some("array-traversal"), Some("""{"items":[1,2,3]}"""))
        assertTrue(r == Right(Block.D3Widget("array-traversal", """{"items":[1,2,3]}""")))
      },
      test("missing widget → MissingAttribute(data-widget)") {
        val r = Blocks.decodeD3Widget(None, Some("""{}"""))
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("d3-widget", "data-widget")))
      },
      test("missing payload → MissingAttribute(data-payload)") {
        val r = Blocks.decodeD3Widget(Some("array-traversal"), None)
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("d3-widget", "data-payload")))
      }
    ),
    suite("decodeTracedCode")(
      test("happy path — language + source present") {
        val r = Blocks.decodeTracedCode(Some("python"), Some("print(1)"))
        assertTrue(r == Right(Block.TracedCode("python", "print(1)")))
      },
      test("missing language → MissingAttribute(data-lang)") {
        val r = Blocks.decodeTracedCode(None, Some("print(1)"))
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("traced-code-block", "data-lang")))
      },
      test("missing source → MissingAttribute(data-source)") {
        val r = Blocks.decodeTracedCode(Some("python"), None)
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("traced-code-block", "data-source")))
      }
    ),
    suite("decodeLikeC4")(
      test("happy path — src + height + title present") {
        val r = Blocks.decodeLikeC4(Some("/c4/view/foo"), Some("520"), Some("Foo overview"))
        assertTrue(r == Right(Block.LikeC4("/c4/view/foo", Some(520), Some("Foo overview"))))
      },
      test("happy path — src only, height + title absent") {
        val r = Blocks.decodeLikeC4(Some("/c4/view/foo"), None, None)
        assertTrue(r == Right(Block.LikeC4("/c4/view/foo", None, None)))
      },
      test("malformed height falls through as None (renderer falls back to default)") {
        val r = Blocks.decodeLikeC4(Some("/c4/view/foo"), Some("100%"), None)
        assertTrue(r == Right(Block.LikeC4("/c4/view/foo", None, None)))
      },
      test("empty title collapses to None (matches the empty-label pattern elsewhere)") {
        val r = Blocks.decodeLikeC4(Some("/c4/view/foo"), None, Some(""))
        assertTrue(r == Right(Block.LikeC4("/c4/view/foo", None, None)))
      },
      test("missing src → MissingAttribute(data-src)") {
        val r = Blocks.decodeLikeC4(None, Some("520"), Some("Foo"))
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("likec4-iframe", "data-src")))
      }
    ),
    suite("workbench decoders")(
      test("decodeWorkbenchInline — tabs + spec round-trip") {
        val r = Blocks.decodeWorkbenchInline(
          List(RawTab(Some("python"), Some("Python"), Some("print(1)"), Some(true), viz = Some("array"))),
          Some(rawSpec)
        )
        assertTrue(
          r == Right(
            Block.WorkbenchInline(
              List(Block.Tab("python", "Python", "print(1)", true, viz = Some("array"))),
              expectedSpec
            )
          )
        )
      },
      test("decodeWorkbenchInline — missing spec → MissingAttribute(data-spec)") {
        val r = Blocks.decodeWorkbenchInline(
          List(RawTab(Some("python"), Some("Python"), Some("x"), None)),
          None
        )
        assertTrue(r == Left(BlockDecodeError.MissingAttribute("workbench-inline", "data-spec")))
      },
      test("spec validation — arg without id → MalformedSpec") {
        val bad = rawSpec.copy(args = List(Blocks.RawArgSpec(None, Some("arr"), Some("int[]"), None)))
        val r   = Blocks.decodeWorkbenchInline(tabOnly, Some(bad))
        assertTrue(r == Left(BlockDecodeError.MalformedSpec("workbench-inline", "arg 0 is missing an id")))
      },
      test("spec validation — label falls back to id, empty placeholder collapses to None") {
        val raw = rawSpec.copy(args = List(Blocks.RawArgSpec(Some("n"), None, Some("int"), Some(""))))
        val r = Blocks.decodeWorkbenchInline(
          tabOnly,
          Some(raw.copy(cases = List(Blocks.RawTestCase(Map("n" -> "6"), None))))
        )
        assertTrue(
          r.map(_.spec.args) == Right(List(Block.ArgSpec("n", "n", "int", None)))
        )
      },
      test("spec validation — case referencing an unknown arg → MalformedSpec") {
        val bad = rawSpec.copy(cases = List(Blocks.RawTestCase(Map("nope" -> "1"), None)))
        val r   = Blocks.decodeWorkbenchInline(tabOnly, Some(bad))
        assertTrue(
          r == Left(BlockDecodeError.MalformedSpec(
            "workbench-inline",
            "case 0 references unknown arg `nope`"
          ))
        )
      },
      test("spec validation — empty args / empty cases → EmptyContent") {
        val noArgs  = Blocks.decodeWorkbenchInline(tabOnly, Some(rawSpec.copy(args = Nil)))
        val noCases = Blocks.decodeWorkbenchInline(tabOnly, Some(rawSpec.copy(cases = Nil)))
        assertTrue(
          noArgs == Left(BlockDecodeError.EmptyContent("workbench-inline", "spec args")),
          noCases == Left(BlockDecodeError.EmptyContent("workbench-inline", "spec cases"))
        )
      },
      test("decodeYourTurn — title falls back, empty blurb collapses, empty editorial allowed") {
        val r = Blocks.decodeYourTurn(Some(""), Some(""), "<p>statement</p>", "", tabOnly, Some(rawSpec))
        assertTrue(
          r.map(yt => (yt.title, yt.blurb, yt.editorialHtml)) == Right(("Your Turn", None, ""))
        )
      },
      test("decodeYourTurn — blank statement → EmptyContent(statement)") {
        val r = Blocks.decodeYourTurn(Some("T"), None, "  \n ", "<p>ed</p>", tabOnly, Some(rawSpec))
        assertTrue(r == Left(BlockDecodeError.EmptyContent("your-turn", "statement")))
      },
      test("decodeProblemWorkbench — sections map to EdSection in order") {
        val r = Blocks.decodeProblemWorkbench(
          "<p>desc</p>",
          List("Intuition" -> "<p>a</p>", "Approach" -> "<p>b</p>"),
          tabOnly,
          Some(rawSpec)
        )
        assertTrue(
          r.map(_.sections) == Right(
            List(Block.EdSection("Intuition", "<p>a</p>"), Block.EdSection("Approach", "<p>b</p>"))
          )
        )
      },
      test("decodeProblemWorkbench — blank description → EmptyContent(description)") {
        val r = Blocks.decodeProblemWorkbench("  ", Nil, tabOnly, Some(rawSpec))
        assertTrue(r == Left(BlockDecodeError.EmptyContent("problem-workbench", "description")))
      },
      test("decodeQuiz — happy path") {
        val r = Blocks.decodeQuiz(None, Some("n = 20"), List("19", "8"), Some("8"))
        assertTrue(r == Right(Block.Quiz(None, "n = 20", List("19", "8"), "8")))
      },
      test("decodeQuiz — answer not among options → MalformedSpec") {
        val r = Blocks.decodeQuiz(None, Some("n"), List("1", "2"), Some("3"))
        assertTrue(r == Left(BlockDecodeError.MalformedSpec("quiz-block", "answer is not among the options")))
      },
      test("decodeQuiz — fewer than two options → EmptyContent(options)") {
        val r = Blocks.decodeQuiz(None, Some("n"), List("1"), Some("1"))
        assertTrue(r == Left(BlockDecodeError.EmptyContent("quiz-block", "options")))
      },
      test("decodeSolutionViewer — happy path, empty meta collapses to None") {
        val r = Blocks.decodeSolutionViewer(tabOnly, Some("O(n)"), Some(""))
        assertTrue(
          r == Right(
            Block.SolutionViewer(List(Block.Tab("python", "Python", "x", true)), Some("O(n)"), None)
          )
        )
      },
      test("decodeSolutionViewer — empty tabs → EmptyContent(tabs)") {
        val r = Blocks.decodeSolutionViewer(Nil, None, None)
        assertTrue(r == Left(BlockDecodeError.EmptyContent("workbench-solution", "tabs")))
      }
    )
  )

  /** One valid tab — the tabs leg of workbench decoders is shared with `runnable-group`, tested above. */
  private val tabOnly = List(RawTab(Some("python"), Some("Python"), Some("x"), None))

  private val rawSpec = Blocks.RawTestSpec(
    args = List(Blocks.RawArgSpec(Some("arr"), Some("arr"), Some("int[]"), Some("[1, 2]"))),
    cases = List(
      Blocks.RawTestCase(Map("arr" -> "[1, 2]"), Some("[2, 1]")),
      Blocks.RawTestCase(Map("arr" -> "[]"), Some(""))
    )
  )

  private val expectedSpec = Block.TestSpec(
    args = List(Block.ArgSpec("arr", "arr", "int[]", Some("[1, 2]"))),
    cases = List(
      Block.TestCase(Map("arr" -> "[1, 2]"), Some("[2, 1]")),
      // Empty expected collapses to None — "run and show output, no verdict".
      Block.TestCase(Map("arr" -> "[]"), None)
    )
  )
