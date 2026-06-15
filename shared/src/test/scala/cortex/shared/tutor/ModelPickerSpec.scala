package cortex.shared.tutor

import cortex.shared.tutor.TutorContract.{ModelOption, Tier, Whoami}
import zio.test.*

object ModelPickerSpec extends ZIOSpecDefault:

  private val sonnet = ModelOption("claude-sonnet", "Claude Sonnet 4.6", "anthropic")
  private val haiku  = ModelOption("claude-haiku", "Claude Haiku 4.5", "anthropic")
  private val qwen   = ModelOption("qwen-coach", "Qwen Coach", "ollama") // hypothetical future local model
  private val gpt    = ModelOption("or-gpt-4.1", "GPT-4.1 (OpenRouter)", "openrouter")

  private def whoami(tier: Tier, models: List[ModelOption], default: String): Whoami =
    Whoami(
      sub = "u",
      preferredUsername = "ada",
      tier = tier,
      defaultModel = default,
      availableModels = models
    )

  override def spec: Spec[Any, Any] = suite("ModelPicker")(
    suite("resolveDefault")(
      test("picks the advertised defaultModel entry") {
        val w = whoami(Tier.Homelab, List(sonnet, haiku), "claude-haiku")
        assertTrue(ModelPicker.resolveDefault(w) == Some(haiku))
      },
      test("falls back to the first model when the default isn't in the list (version skew)") {
        val w = whoami(Tier.Byok, List(haiku), "claude-sonnet")
        assertTrue(ModelPicker.resolveDefault(w) == Some(haiku))
      },
      test("None when no models are offered") {
        val w = whoami(Tier.Homelab, Nil, "claude-sonnet")
        assertTrue(ModelPicker.resolveDefault(w) == None)
      }
    ),
    suite("byKey")(
      test("resolves a selectable key") {
        val w = whoami(Tier.Byok, List(sonnet, haiku), "claude-sonnet")
        assertTrue(ModelPicker.byKey(w, "claude-haiku") == Some(haiku))
      },
      test("None for a key not in the tier-filtered list") {
        val w = whoami(Tier.Byok, List(sonnet), "claude-sonnet")
        assertTrue(ModelPicker.byKey(w, "qwen-coach") == None)
      }
    ),
    suite("requiresKey — a cloud model needs the visitor's own key (any tier)")(
      test("Anthropic model ⇒ needs the visitor's key") {
        assertTrue(ModelPicker.requiresKey(sonnet))
      },
      test("OpenRouter model ⇒ needs the visitor's key") {
        assertTrue(ModelPicker.requiresKey(gpt))
      },
      test("operator picking a cloud model (dual-mode) ⇒ needs their own key") {
        assertTrue(ModelPicker.requiresKey(sonnet))
      },
      test("local model ⇒ no key (server-streamed)") {
        assertTrue(!ModelPicker.requiresKey(qwen))
      }
    ),
    suite("ModelOption provider helpers")(
      test("isAnthropic / isOpenRouter are case-insensitive on provider") {
        assertTrue(sonnet.isAnthropic) &&
        assertTrue(ModelOption("x", "X", "Anthropic").isAnthropic) &&
        assertTrue(gpt.isOpenRouter) &&
        assertTrue(ModelOption("y", "Y", "OpenRouter").isOpenRouter)
      },
      test("requiresUserKey ⇒ true for cloud BYOK providers, false for the local model") {
        assertTrue(sonnet.requiresUserKey) &&
        assertTrue(gpt.requiresUserKey) &&
        assertTrue(!qwen.requiresUserKey)
      },
      test("each provider helper is false for the other provider") {
        assertTrue(!qwen.isAnthropic) &&
        assertTrue(!sonnet.isOpenRouter)
      }
    )
  )
