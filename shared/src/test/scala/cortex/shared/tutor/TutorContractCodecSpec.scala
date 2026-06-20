package cortex.shared.tutor

import cortex.shared.tutor.TutorContract.*
import io.circe.parser.{decode, parse}
import io.circe.syntax.*
import zio.test.*

object TutorContractCodecSpec extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = suite("TutorContract codecs")(
    suite("SessionCreateRequest payload")(
      test("includes the chosen model key when set") {
        val json = SessionCreateRequest("two-sum", SessionOrigin.YourTurn, Some("claude-haiku")).asJson
        assertTrue(
          json.hcursor.get[String]("problemId") == Right("two-sum"),
          json.hcursor.get[String]("origin") == Right("your_turn"),
          json.hcursor.get[Option[String]]("model") == Right(Some("claude-haiku"))
        )
      },
      test("omits the model (null/absent) when None ⇒ the tutor applies the tier default") {
        val json = SessionCreateRequest("two-sum").asJson
        // circe reads a missing-or-null field as None, which the tutor treats as 'use the tier default'.
        assertTrue(json.hcursor.get[Option[String]]("model") == Right(None))
      },
      test("round-trips with a model") {
        val req = SessionCreateRequest("p", SessionOrigin.ProblemPage, Some("claude-sonnet"))
        assertTrue(decode[SessionCreateRequest](req.asJson.noSpaces) == Right(req))
      },
      test("round-trips without a model") {
        val req = SessionCreateRequest("p", SessionOrigin.YourTurn, None)
        assertTrue(decode[SessionCreateRequest](req.asJson.noSpaces) == Right(req))
      },
      test("defaults the track to problem, and round-trips a conceptual request") {
        val conceptual = SessionCreateRequest("p", SessionOrigin.YourTurn, None, Track.Conceptual)
        assertTrue(
          SessionCreateRequest("two-sum").asJson.hcursor.get[String]("track") == Right("problem"),
          conceptual.asJson.hcursor.get[String]("track") == Right("conceptual"),
          decode[SessionCreateRequest](conceptual.asJson.noSpaces) == Right(conceptual)
        )
      }
    ),
    suite("Track + conceptual steps")(
      test("Track round-trips both wire values") {
        assertTrue(
          (Track.Problem: Track).asJson.noSpaces == "\"problem\"",
          (Track.Conceptual: Track).asJson.noSpaces == "\"conceptual\"",
          decode[Track]("\"conceptual\"") == Right(Track.Conceptual),
          decode[Track]("\"bogus\"").isLeft
        )
      },
      test("conceptual steps round-trip via the shared Step codec, and orderedFor is the 4-step ladder") {
        assertTrue(
          (Step.Explain: Step).asJson.noSpaces == "\"explain\"",
          decode[Step]("\"defend\"") == Right(Step.Defend),
          Step.orderedFor(Track.Conceptual) == List(Step.Explain, Step.Apply, Step.Analyze, Step.Defend),
          Step.orderedFor(Track.Problem) == Step.ordered
        )
      }
    ),
    suite("CoachSession track")(
      test("decodes its track, and tolerates an absent track (older browser mirror) as None") {
        val withTrack =
          """{"sessionId":"s","problemId":"p","origin":"your_turn","track":"conceptual","status":"active","currentStep":"explain","stepIndex":0,"completed":false,"messages":[],"scores":[],"rubricVersion":"v"}"""
        val withoutTrack =
          """{"sessionId":"s","problemId":"p","origin":"your_turn","status":"active","currentStep":"clarify","stepIndex":0,"completed":false,"messages":[],"scores":[],"rubricVersion":"v"}"""
        assertTrue(
          decode[CoachSession](withTrack).map(_.track) == Right(Some(Track.Conceptual)),
          decode[CoachSession](withoutTrack).map(_.track) == Right(None)
        )
      }
    ),
    suite("Whoami decode")(
      test("decodes tier + default + tier-filtered models, ignoring unknown server fields") {
        val wire =
          """{
            |  "sub": "user-123",
            |  "preferredUsername": "ada",
            |  "tier": "byok",
            |  "defaultModel": "claude-sonnet",
            |  "availableModels": [
            |    {"key":"claude-sonnet","display":"Claude Sonnet 4.6","provider":"anthropic"},
            |    {"key":"claude-haiku","display":"Claude Haiku 4.5","provider":"anthropic"}
            |  ],
            |  "extraServerField": "forward-compatible — must be ignored"
            |}""".stripMargin
        val expected = Whoami(
          sub = "user-123",
          preferredUsername = "ada",
          tier = Tier.Byok,
          defaultModel = "claude-sonnet",
          availableModels = List(
            ModelOption("claude-sonnet", "Claude Sonnet 4.6", "anthropic"),
            ModelOption("claude-haiku", "Claude Haiku 4.5", "anthropic")
          )
        )
        assertTrue(decode[Whoami](wire) == Right(expected))
      },
      test("decodes the homelab tier") {
        val wire =
          """{"sub":"s","preferredUsername":"u","tier":"homelab","defaultModel":"claude-sonnet","availableModels":[]}"""
        assertTrue(decode[Whoami](wire).map(_.tier) == Right(Tier.Homelab))
      },
      test("rejects an unknown tier value") {
        val wire =
          """{"sub":"s","preferredUsername":"u","tier":"enterprise","defaultModel":"x","availableModels":[]}"""
        assertTrue(decode[Whoami](wire).isLeft)
      }
    ),
    suite("ModelOption decode")(
      test("decodes a catalog entry") {
        val r = parse("""{"key":"claude-haiku","display":"Claude Haiku 4.5","provider":"anthropic"}""")
          .flatMap(_.as[ModelOption])
        assertTrue(r == Right(ModelOption("claude-haiku", "Claude Haiku 4.5", "anthropic")))
      }
    )
  )
