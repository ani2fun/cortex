package cortex.client.auth

import org.scalajs.dom

/**
 * The visitor's own BYOK API keys — `sessionStorage` only, keyed per provider, by design:
 *
 *   - **this tab only, never persisted**: closing the tab forgets them; no localStorage, no cookie;
 *   - **never sent to our origins**: [[cortex.client.api.ByokProvider]] sends each key only to its own
 *     provider (OpenRouter / Anthropic) — the tutor records outcomes, not keys;
 *   - **wiped on sign-out** ([[AuthStore.signOut]] calls [[clearAll]] before the redirect).
 *
 * Per-provider so a visitor can hold (say) an OpenRouter key and an Anthropic key independently; the coach
 * uses the one matching the selected model's provider ([[get]]).
 */
object ByokKeyStore:

  private val Prefix = "cortex.tutor.byokKey." // per-provider slot: <Prefix><provider>

  private def slot(provider: String): String = Prefix + provider.toLowerCase

  def get(provider: String): Option[String] =
    Option(dom.window.sessionStorage.getItem(slot(provider))).map(_.trim).filter(_.nonEmpty)

  def set(provider: String, key: String): Unit =
    dom.window.sessionStorage.setItem(slot(provider), key.trim)

  def clear(provider: String): Unit =
    dom.window.sessionStorage.removeItem(slot(provider))

  /** Every stored provider→key pair (scans sessionStorage by prefix) — seeds the coach's key state. */
  def all: Map[String, String] =
    val ss = dom.window.sessionStorage
    (0 until ss.length).flatMap { i =>
      Option(ss.key(i))
        .filter(_.startsWith(Prefix))
        .flatMap(k => Option(ss.getItem(k)).map(_.trim).filter(_.nonEmpty).map(k.drop(Prefix.length) -> _))
    }.toMap

  /** Wipe every stored BYOK key (sign-out). Keys are collected before removal so the scan is stable. */
  def clearAll(): Unit =
    val ss   = dom.window.sessionStorage
    val keys = (0 until ss.length).flatMap(i => Option(ss.key(i))).filter(_.startsWith(Prefix))
    keys.foreach(ss.removeItem)
