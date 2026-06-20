---
title: '5. The Authorization Code flow, step by step'
summary: The redirect dance that powers almost every "log in with…" button. Scrub through it one message at a time and watch a public client safely turn a click into a token.
---

# 5. The Authorization Code flow, step by step

## TL;DR

> The **Authorization Code flow** is OAuth's main event. The client never sees your password; instead it bounces your *browser* to the Authorization Server, you log in *there*, and the AS hands back a short-lived **authorization code** through a redirect. The client then swaps that code — over a separate, direct channel — for tokens. The split (a code through the browser, tokens through the back channel) is the whole point: even someone watching your browser only ever sees a single-use code, not your tokens. This is exactly how you sign in to Cortex.

## 1. Motivation

You've clicked "Log in with Google" a thousand times. Watch the address bar next time: it flickers from the app, to `accounts.google.com`, and back to the app, all in a second. That flicker is the Authorization Code flow happening in real time.

Why bounce the browser around at all? Because of a constraint we established in Chapter 4: the client is a **public** client — a SPA whose code anyone can read — so *you must never type your password into it*. The only party that should ever see your password is the Authorization Server. So the flow is built around one rule: **send the human to the AS to authenticate, and bring back only a token-shaped result.** Every redirect below is in service of that rule.

## 2. Intuition (Analogy)

Imagine picking up a package that needs ID, but you've sent a courier (the app) to fetch it for you. You don't give the courier your passport. Instead:

1. The courier drives *you* to the post office counter.
2. *You* show *your* ID to the clerk (the courier waits outside — they never see your passport).
3. The clerk gives you a **numbered claim slip** and you hand it to the courier.
4. The courier takes the slip to a *different* window and — proving they're the courier the slip was issued to — exchanges it for the package.

The claim slip is the **authorization code**: useless to a thief because it's single-use and must be redeemed by the right courier at the right window. Your passport (password) never left your hands. That's the flow.

## 3. Formal Definition

The **Authorization Code grant** (RFC 6749 §4.1), in modern form (with PKCE, which we motivate fully in the next chapter):

1. The client generates a random secret, the **code verifier**, and its hash, the **code challenge**.
2. The client redirects the **browser** to the AS's **authorization endpoint**, passing `client_id`, `redirect_uri`, `scope`, an anti-forgery `state`, and the `code_challenge`.
3. The AS authenticates the **Resource Owner** (you log in) and obtains consent.
4. The AS redirects the browser back to the client's `redirect_uri` with a single-use **authorization code** (and the `state` echoed back).
5. The client sends the code — plus the original **code verifier** — to the AS's **token endpoint** over a direct HTTPS call (the "back channel").
6. The AS verifies the code and the verifier, then returns an **access token** (and usually an **ID token** and **refresh token**).
7. The client calls the **Resource Server**, presenting the access token as `Authorization: Bearer <token>`.

The two channels matter enormously:

- The **front channel** (steps 2–4) runs *through the browser via redirects* — visible, interceptable, so it carries only a **single-use code**, never tokens.
- The **back channel** (steps 5–6) is a *direct* client-to-AS HTTPS call — not visible to the browser — so it's safe to carry the actual **tokens**.

## 4. Worked Example — scrub through it

Below is the real flow Cortex uses, as an interactive diagram. Use **Prev/Next** (or **Play**) to advance one message at a time; the caption explains each step. Watch how the *code* comes back through the browser, but the *tokens* only ever travel on the direct SPA→Keycloak call.

```d3 widget=oauth-pkce-flow
{
  "title": "Authorization Code + PKCE — how you sign in to Cortex",
  "actors": [
    { "id": "ua",  "label": "Your Browser" },
    { "id": "spa", "label": "Cortex SPA" },
    { "id": "as",  "label": "Keycloak" },
    { "id": "rs",  "label": "Cortex API" }
  ],
  "variants": [
    {
      "id": "pkce",
      "label": "Authorization Code + PKCE",
      "summary": "The SPA is a public client, so PKCE binds the flow to the app that started it.",
      "steps": [
        { "from": "spa", "to": "spa", "kind": "compute", "label": "make verifier + challenge",
          "detail": "Step 1 — The SPA invents a random secret (the code_verifier) and computes code_challenge = SHA-256(verifier). It keeps the verifier private and will only send the hash for now." },
        { "from": "spa", "to": "ua", "kind": "redirect", "label": "navigate to /authorize",
          "detail": "Step 2 — The SPA redirects your browser to Keycloak's authorization endpoint, attaching client_id, redirect_uri, scope, an anti-forgery 'state', and the code_challenge." },
        { "from": "ua", "to": "as", "kind": "redirect", "label": "GET /authorize?…",
          "detail": "Step 3 — Your browser lands on Keycloak. Note: the SPA is no longer involved — you are now talking to the Authorization Server directly." },
        { "from": "as", "to": "ua", "kind": "request", "label": "login + consent",
          "detail": "Step 4 — Keycloak shows a login page (for Cortex it brokers to GitHub). You authenticate HERE, to Keycloak — never to the SPA. Your password never touches the app." },
        { "from": "ua", "to": "as", "kind": "request", "label": "authenticate",
          "detail": "Step 5 — You prove who you are to Keycloak and consent to the requested scopes." },
        { "from": "as", "to": "ua", "kind": "redirect", "label": "redirect back ?code=…",
          "detail": "Step 6 — Keycloak redirects your browser back to the SPA's redirect_uri carrying a single-use authorization code (and echoing 'state'). The code is useless on its own." },
        { "from": "ua", "to": "spa", "kind": "redirect", "label": "deliver code to SPA",
          "detail": "Step 7 — The browser hands the code to the SPA (it's in the callback URL the SPA now reads). The front-channel part is done — and only a code ever travelled through the browser." },
        { "from": "spa", "to": "as", "kind": "token", "label": "POST /token (code + verifier)",
          "detail": "Step 8 — On the BACK channel (a direct HTTPS call, not via the browser), the SPA sends the code AND the original code_verifier. Keycloak hashes the verifier and checks it equals the code_challenge from step 2." },
        { "from": "as", "to": "spa", "kind": "token", "label": "access + id + refresh tokens",
          "detail": "Step 9 — The check passes, so Keycloak returns an access token (a JWT), an ID token (who you are), and a refresh token. These tokens only ever travelled on the back channel." },
        { "from": "spa", "to": "rs", "kind": "data", "label": "GET /api (Bearer token)",
          "detail": "Step 10 — Finally the SPA calls the Cortex API with Authorization: Bearer <access token>. The API verifies the signature locally and serves the request. You are signed in." }
      ]
    }
  ]
}
```

> Pause on **Step 6** and **Step 8**. The authorization code comes back through the *browser* (front channel, interceptable) — so it's single-use and worthless alone. The *tokens* only ever appear on the *direct* SPA→Keycloak call (back channel). That separation is the security of the whole flow.

## 5. Build It

Run this. It implements the flow's core safety property — that the code is *single-use* and bound to the verifier — without the network parts, so you can see the logic.

```python run
import hashlib, base64, secrets

def b64(b): return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

# ---- Step 1: client makes a verifier + challenge (PKCE) ----
verifier  = b64(secrets.token_bytes(32))
challenge = b64(hashlib.sha256(verifier.encode()).digest())
print("verifier  (kept secret by the SPA):", verifier[:16], "…")
print("challenge (sent in the redirect)   :", challenge[:16], "…")

# ---- Authorization Server state ----
issued_codes = {}

def authorize(challenge):
    # Step 2-6: user logs in (assume success), AS stores the challenge with a one-time code.
    code = secrets.token_urlsafe(8)
    issued_codes[code] = {"challenge": challenge, "used": False}
    return code

def token_endpoint(code, presented_verifier):
    rec = issued_codes.get(code)
    if rec is None or rec["used"]:
        return "invalid_grant — code unknown or already used"
    # The code is single-use: burn it immediately.
    rec["used"] = True
    # PKCE check: does SHA256(presented_verifier) match the stored challenge?
    recomputed = b64(hashlib.sha256(presented_verifier.encode()).digest())
    if recomputed != rec["challenge"]:
        return "invalid_grant — PKCE verifier does not match challenge"
    return "ACCESS_TOKEN (+ id, refresh)"

# ---- The happy path ----
code = authorize(challenge)
print("\nAS issued code:", code)
print("Redeem with correct verifier:", token_endpoint(code, verifier))

# ---- Replay the same code: rejected (single-use) ----
print("Replay the same code:        ", token_endpoint(code, verifier))
```

**Now break it.** Change the redemption line to `token_endpoint(code, "a-different-verifier")` (simulating a thief who stole the code but not the verifier) and re-run with a fresh `code`. You'll get `invalid_grant — PKCE verifier does not match`. The stolen code is worthless. That is the attack the *next* chapter is entirely about.

## 6. Trade-offs & Complexity

Why this elaborate dance instead of something simpler?

| Simpler idea | Why OAuth rejects it |
|---|---|
| App collects your password and logs in for you | The anti-pattern — app sees your password (Chapter 2). |
| AS returns the **token** directly in the redirect (the old "Implicit flow") | Tokens then travel through the browser/URL — leakable in history, logs, `Referer`. Now deprecated. |
| App and AS share a secret to skip the code | Impossible for a *public* client — it can't keep a secret. |

The Authorization Code flow is more steps, but each step removes a way to get robbed. The redirect bounce keeps your password at the AS; the single-use code keeps the browser channel safe; the back-channel exchange keeps tokens off the browser. The cost is genuine complexity — which is exactly why you use a library (`keycloak-js`, Chapter 18) rather than hand-rolling it.

## 7. Edge Cases & Failure Modes

- **Forgetting `state`.** The `state` parameter is an anti-CSRF nonce: the client generates it, the AS echoes it, the client checks it matches. Skip it and an attacker can trick your client into completing *their* login. (Modern libraries handle this for you.)
- **Loose `redirect_uri` matching.** The AS must match the `redirect_uri` *exactly* against a registered allow-list. A wildcard or substring match lets an attacker redirect the code to themselves. (Chapter 21 returns to this.)
- **Reusing an authorization code.** Codes are single-use; a well-behaved AS rejects a replayed code *and* may revoke tokens already issued for it.
- **The implicit flow.** If you find a tutorial returning tokens directly in the URL fragment (`response_type=token`), it's teaching the deprecated implicit flow. Don't. Authorization Code + PKCE replaced it for exactly the leak reasons above.

## 8. Practice

> **Exercise 1 — Label the channels.** For each message in the interactive diagram, mark it *front channel* (through the browser) or *back channel* (direct). Then state, in one sentence, why tokens must never appear on the front channel.

<details>
<summary><strong>Answer</strong></summary>

Using the §3 split — the **front channel** is anything routed *through the browser via redirects* (visible, interceptable); the **back channel** is a *direct* SPA→Keycloak HTTPS call (invisible to the browser):

- **Steps 1–7 (make verifier/challenge → navigate to `/authorize` → login + consent → redirect back with `?code=` → browser delivers code to SPA): front channel.** Everything up to and including the code coming back travels through the browser. (Step 1, "make verifier + challenge," is local SPA compute, but the request it prepares goes out on the front channel.)
- **Steps 8–9 (`POST /token` with code + verifier → receive access/ID/refresh tokens): back channel.** The code is redeemed and the tokens are returned on the *direct* call, never via the browser.
- **Step 10 (`GET /api` with the Bearer token): a direct SPA→Resource Server call** — also not a browser redirect; the token is sent in an `Authorization` header, not a navigation.

**Why tokens must never appear on the front channel:** the front channel passes through the browser, where values land in URL history, server logs, and `Referer` headers and can be intercepted — so anything that travels there must be useless if stolen (a single-use code), never a reusable credential like an access or refresh token.

</details>

> **Exercise 2 — Trace a theft.** An attacker captures the redirect in step 6 and steals the authorization code. Walk through what happens when they try to redeem it at the token endpoint. What stops them? (Foreshadow: it's the verifier.)

<details>
<summary><strong>Answer</strong></summary>

Walk it step by step. The attacker now holds the single-use **code** from step 6 and races to the token endpoint to redeem it:

1. They send `POST /token` with the stolen code. But step 5 of the flow requires presenting the original **`code_verifier`** — the random secret the *real* SPA generated in step 1 and **kept private**, never putting it on the front channel.
2. Keycloak stored only the **`code_challenge`** = `SHA-256(verifier)` against that code (step 2). To redeem, it recomputes `SHA-256(presented_verifier)` and checks it equals the stored challenge.
3. The attacker never saw the verifier — only the challenge was ever public, and SHA-256 **can't be reversed** to recover the verifier from it. So they can't produce a value that hashes to the challenge.
4. Keycloak returns **`invalid_grant`**. No verifier, no tokens. (And the code is single-use anyway, so even a replay is dead on arrival.)

**What stops them is PKCE — the verifier.** The code is bound to a secret only the originating app holds, which makes a *stolen code worthless*. That binding, and why it's the right defense, is the whole of the next chapter.

</details>

> **Exercise 3 — Watch it live.** On any site with "Log in with Google/GitHub", open dev tools → Network, and log in. Find the request to `/authorize` and the one to `/token`. What parameters does each carry? Which one is your browser navigating to, and which is a background `fetch`/`POST`?

<details>
<summary><strong>Answer</strong></summary>

What you'll see maps exactly onto the §3 channels:

- **`/authorize` — a browser *navigation* (front channel).** It appears as a top-level document request (the address bar changes to the provider's domain), and it's a `GET` whose **query string** carries `response_type=code`, `client_id`, `redirect_uri`, `scope`, `state` (the anti-CSRF nonce), and `code_challenge` + `code_challenge_method=S256`. Note these are all in the URL — visible — which is fine because only a *code* will come back here.
- **`/token` — a background `POST` (back channel).** It is *not* a navigation; it's an XHR/`fetch` the SPA makes directly to the provider. Its **request body** carries `grant_type=authorization_code`, the `code` you just received, the `redirect_uri`, `client_id`, and the original **`code_verifier`** — and its **response body** contains the `access_token`, `id_token`, and `refresh_token`.

The key observation: your **browser navigates to `/authorize`** (so its parameters are in the URL and visible), while **`/token` is a direct background call** (so the *tokens* in its response never touch the address bar, history, or `Referer`). Seeing the verifier in the `/token` body but only the challenge in the `/authorize` URL is PKCE working in the wild.

</details>

```quiz
{
  "prompt": "In the Authorization Code flow, what travels back to the client through the browser redirect (the front channel)?",
  "input": "Choose one:",
  "options": [
    "A single-use authorization code — not the tokens",
    "The access token and refresh token",
    "The user's password",
    "The client secret"
  ],
  "answer": "A single-use authorization code — not the tokens"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[RFC 6749 §4.1 — Authorization Code Grant](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1)** — the canonical step-by-step. Read it alongside the diagram above.
- **[OAuth 2.0 Security Best Current Practice — RFC 9700](https://datatracker.ietf.org/doc/html/rfc9700)** — the modern guidance that makes Authorization Code + PKCE the default and retires the implicit flow.
- **[The OAuth 2.0 Playground](https://www.oauth.com/playground/)** — step through a real Authorization Code flow in your browser, watching every parameter. Pairs perfectly with the diagram here.

---

**Next:** the diagram above quietly relied on a thing called PKCE. The thief in Exercise 2 was stopped by it. Now we earn it from scratch — by first building the attack it defends against. → [6. Why PKCE exists](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/why-pkce-exists)
