package cortex.shared.tutor

import cortex.shared.tutor.TutorContract.{ModelOption, Whoami}

/**
 * Pure, framework-free model-picker logic shared by the coach UI and its tests — kept out of
 * [[CoachController]] (Scala.js, untestable here) so the rules live in `shared` and run under `sharedJVM`
 * zio-test.
 *
 * The catalog is server-authoritative: `GET /v1/whoami` returns only the models a caller's tier may pick
 * ([[Whoami.availableModels]], default-first) plus the [[Whoami.defaultModel]] key. The client never invents
 * availability — it resolves a selection against that list and decides whether the selection needs the
 * visitor's own Anthropic key.
 */
object ModelPicker:

  /**
   * The initial selection: the [[Whoami.defaultModel]] entry, falling back to the first available model if
   * the advertised default isn't in the list (a server/version skew). `None` only when no models are offered.
   */
  def resolveDefault(whoami: Whoami): Option[ModelOption] =
    whoami.availableModels
      .find(_.key == whoami.defaultModel)
      .orElse(whoami.availableModels.headOption)

  /** Resolve a user-chosen catalog key against the tier-filtered list; `None` if it isn't selectable. */
  def byKey(whoami: Whoami, key: String): Option[ModelOption] =
    whoami.availableModels.find(_.key == key)

  /**
   * Whether running the coach on `model` requires the visitor's own API key.
   *
   * True for any **cloud** model (OpenRouter or Anthropic) — the turn is client-direct on the user's key,
   * whether they're an external BYOK user or the **operator** picking a cloud model over their local one
   * (dual-mode). The local wk-1 model is server-streamed and needs no key. Tier no longer matters here — it
   * only gates the menu; the chosen model's provider decides whether a key is needed.
   */
  def requiresKey(model: ModelOption): Boolean =
    model.requiresUserKey
