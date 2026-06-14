---
title: '6. Why PKCE exists'
summary: PKCE is not a magic incantation — it is the precise answer to one specific theft. Build the attack first, then watch nine characters of cryptography make the stolen code worthless.
---

# 6. Why PKCE exists

## TL;DR

> A **public client** (a SPA or mobile app) can't keep a secret, so when the Authorization Server sends back an authorization **code** through the browser, a malicious app can **intercept** it — and, with no client secret to stop them, redeem it for your tokens. **PKCE** (Proof Key for Code Exchange, "pixie") fixes this: the real client first sends a *hash* of a random secret (the `code_challenge`), and at redemption must present the *original* secret (the `code_verifier`). The thief stole the code but not the verifier, so the token endpoint rejects them. Cortex's `cortex-web` client sets `pkce.code.challenge.method: S256` for exactly this reason.

## 1. Motivation

In the last chapter, the Authorization Code flow looked airtight: the password stays at the AS, tokens stay on the back channel, and only a single-use code travels through the browser. So where's the hole?

Here it is. For a **confidential** client (a backend), redeeming the code at the token endpoint requires a `client_secret` only the real client knows — so even if someone steals the code, they can't redeem it. But a **public** client *has no secret*. On a phone or desktop, apps claim redirect URIs like `myapp://callback`, and the operating system happily routes the redirect to *whichever* app registered it — including a malicious one. The attacker grabs the code mid-redirect, walks up to the token endpoint, and… with no secret required, gets your tokens.

This is the **authorization code interception attack**, and it was common enough that in 2015 the IETF published RFC 7636 — PKCE — specifically to kill it. Today it's mandatory for public clients, which is why every SPA, including Cortex's, uses it.

The right way to understand PKCE is to *build the attack first*, then watch nine characters of math close the door.

## 2. Intuition (Analogy)

You order a package and get a claim ticket to pick it up. Trouble: a pickpocket can lift the ticket from your pocket, walk to the counter, and collect *your* package — the counter only checks the ticket, not *who* holds it.

PKCE adds a twist. When you *order*, you whisper a random word to the clerk — say, "albatross" — but the clerk only writes down a **scrambled** version of it on file (a one-way hash they can't reverse). Your claim ticket still looks normal. Now at pickup, the counter demands: *"say the word."* You say "albatross"; the clerk scrambles it and checks it matches the file. ✔

The pickpocket has your ticket — but they never heard the word. They can't say "albatross," and the scramble on file can't be reversed to recover it. **The ticket is now worthless without the word.** That word is the `code_verifier`; the scrambled version on file is the `code_challenge`.

> PKCE doesn't stop the pickpocket from *stealing the ticket*. It makes the *stolen ticket useless* — the defense is at **redemption**, not delivery. Keep that distinction; the interactive diagram below makes it vivid.

## 3. Formal Definition

**PKCE** (RFC 7636) augments the Authorization Code flow with a proof that the party redeeming the code is the same party that started the flow:

1. Before redirecting, the client generates a high-entropy random string, the **`code_verifier`**.
2. It computes the **`code_challenge`** = `BASE64URL(SHA-256(code_verifier))` and a `code_challenge_method` of `S256`.
3. It sends *only* the `code_challenge` on the front-channel authorization request.
4. The AS stores the challenge against the code it issues.
5. At the token endpoint, the client must present the original **`code_verifier`**.
6. The AS computes `SHA-256(code_verifier)` and checks it equals the stored challenge. No match → no tokens.

Why it works: the challenge is a **one-way hash**, so seeing it (on the front channel) reveals nothing about the verifier. Only the originating client knows the verifier. An interceptor holds the code but cannot produce a verifier that hashes to the challenge — SHA-256 is not reversible — so redemption fails. Use `S256`, never the legacy `plain` method (where challenge == verifier, which leaks it).

## 4. Worked Example — attack, then defense

The diagram has **two variants** — toggle between them with the buttons. First run **"Without PKCE (vulnerable)"** and watch the attacker walk away with your tokens. Then switch to **"With PKCE (secure)"** and watch the *exact same theft* fail at the token endpoint.

```d3 widget=oauth-pkce-flow
{
  "title": "The authorization-code interception attack — and how PKCE stops it",
  "actors": [
    { "id": "spa", "label": "Real SPA" },
    { "id": "ua",  "label": "Browser / OS" },
    { "id": "as",  "label": "Keycloak" },
    { "id": "atk", "label": "Attacker app" }
  ],
  "variants": [
    {
      "id": "plain",
      "label": "Without PKCE (vulnerable)",
      "summary": "A public client has no secret, so a stolen code can be redeemed by anyone.",
      "steps": [
        { "from": "spa", "to": "ua", "kind": "redirect", "label": "navigate to /authorize",
          "detail": "The SPA redirects to Keycloak with client_id and redirect_uri — but NO code_challenge. There is nothing binding this flow to this app." },
        { "from": "ua", "to": "as", "kind": "redirect", "label": "GET /authorize",
          "detail": "You log in at Keycloak normally and consent." },
        { "from": "as", "to": "ua", "kind": "redirect", "label": "redirect ?code=…",
          "detail": "Keycloak returns a single-use authorization code through the browser / OS redirect." },
        { "from": "ua", "to": "atk", "kind": "attack", "label": "malicious app grabs the code",
          "detail": "A malicious app registered the SAME redirect URI (e.g. myapp://callback). The OS routes the redirect to it, and it intercepts the code." },
        { "from": "atk", "to": "as", "kind": "attack", "label": "POST /token (code only)",
          "detail": "The attacker redeems the code at the token endpoint. The client is PUBLIC — no client_secret is required — so Keycloak cannot tell the attacker from the real app." },
        { "from": "as", "to": "atk", "kind": "attack", "label": "tokens — stolen",
          "detail": "Keycloak issues access + refresh tokens to the ATTACKER. They are now you. This is the attack RFC 7636 was written to stop." }
      ]
    },
    {
      "id": "pkce",
      "label": "With PKCE (secure)",
      "summary": "The code is bound to a secret only the real app knows — so a stolen code is worthless.",
      "steps": [
        { "from": "spa", "to": "spa", "kind": "compute", "label": "make verifier + challenge",
          "detail": "The SPA invents a random code_verifier and sends only its hash: code_challenge = SHA-256(verifier). The verifier stays private on the device." },
        { "from": "spa", "to": "ua", "kind": "redirect", "label": "navigate /authorize(challenge)",
          "detail": "The authorization request now carries the code_challenge. Seeing the hash reveals nothing about the verifier." },
        { "from": "ua", "to": "as", "kind": "redirect", "label": "GET /authorize(challenge)",
          "detail": "Keycloak stores the challenge alongside the code it is about to issue." },
        { "from": "as", "to": "ua", "kind": "redirect", "label": "redirect ?code=…",
          "detail": "Same as before — a single-use code comes back through the browser." },
        { "from": "ua", "to": "atk", "kind": "attack", "label": "attacker STILL grabs the code",
          "detail": "PKCE does NOT prevent the interception — the attacker still steals the code. The defense is at redemption, not delivery." },
        { "from": "atk", "to": "as", "kind": "attack", "label": "POST /token (code, NO verifier)",
          "detail": "The attacker tries to redeem the code — but the token endpoint now demands the code_verifier whose SHA-256 equals the stored challenge. The attacker never saw the verifier, and the hash can't be reversed." },
        { "from": "as", "to": "atk", "kind": "attack", "label": "rejected",
          "detail": "Keycloak rejects the exchange: invalid_grant. No verifier, no tokens. The stolen code is worthless." },
        { "from": "spa", "to": "as", "kind": "token", "label": "POST /token (code + verifier)",
          "detail": "The real SPA redeems the code with the matching verifier it kept private…" },
        { "from": "as", "to": "spa", "kind": "token", "label": "tokens — only the real app",
          "detail": "…and only the app that started the flow succeeds. PKCE binds the code to its originator." }
      ]
    }
  ]
}
```

> The two flows are *identical* up to the theft. The only difference is what happens when the stolen code is redeemed: without PKCE, the token endpoint has no question to ask; with PKCE, it asks "say the word," and the thief can't.

## 5. Build It

Run this. It's the entire PKCE check in a dozen lines — generate, challenge, and the verifier test that defeats the thief.

```python run
import hashlib, base64, secrets

def b64url(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

# ---- The real client, step 1: make a verifier + challenge ----
code_verifier  = b64url(secrets.token_bytes(32))                       # the secret
code_challenge = b64url(hashlib.sha256(code_verifier.encode()).digest())  # the one-way hash

print("code_verifier  (private to the app):", code_verifier)
print("code_challenge (sent in the open)  :", code_challenge)
print("Can you get the verifier back from the challenge? SHA-256 is one-way, so: no.\n")

# ---- Authorization Server: store the challenge with the code ----
stored_challenge = code_challenge   # AS files this away under the issued code

def token_endpoint(presented_verifier):
    recomputed = b64url(hashlib.sha256(presented_verifier.encode()).digest())
    if recomputed == stored_challenge:
        return "200 — tokens issued"
    return "400 invalid_grant — verifier does not match challenge"

# ---- The real app redeems with the verifier it kept ----
print("Real app  (has verifier):  ", token_endpoint(code_verifier))

# ---- The attacker stole the CODE but never saw the verifier ----
attacker_guess = b64url(secrets.token_bytes(32))   # best they can do is guess
print("Attacker  (guessing):      ", token_endpoint(attacker_guess))
```

**Now break it.** Try to make the attacker win. Replace `attacker_guess` with anything you like — they don't have `code_verifier`, and to forge a matching value they'd need to *reverse SHA-256*, which is computationally infeasible. The only string that passes is the verifier itself, which never left the real app. That's PKCE: a lock whose key was never transmitted.

## 6. Trade-offs & Complexity

PKCE is almost free, which is why it became mandatory:

| Property | Cost |
|---|---|
| Extra security against code interception | One random string + one SHA-256, done by a library |
| Works for clients that *can't* hold a secret | None — that's the whole point |
| No server-side storage of secrets per client | None |
| Defends even confidential clients (defense in depth) | Negligible |

The only real "cost" is conceptual — you have to understand *why* it's there, which you now do. In practice, `keycloak-js` (Chapter 18) generates the verifier and challenge for you; you just set `pkceMethod: "S256"`. Cortex does exactly that.

## 7. Edge Cases & Failure Modes

- **Using `plain` instead of `S256`.** The `plain` method sets `code_challenge = code_verifier` — sending the secret in the clear and defeating the purpose. Always `S256`. Cortex pins `pkce.code.challenge.method: S256` in its realm.
- **Low-entropy verifiers.** A guessable verifier is a guessable lock. RFC 7636 requires 43–128 chars of high entropy; libraries handle this.
- **Thinking PKCE replaces `state`.** It doesn't. `state` defends against CSRF on the redirect; PKCE defends against code interception. You want both.
- **Assuming PKCE protects the *delivery* of the code.** It doesn't — re-read the diagram. It makes a stolen code *unredeemable*. Exact-match `redirect_uri` registration is still what limits *where* the code can be delivered.

## 8. Practice

> **Exercise 1 — Where's the defense?** In one sentence, explain why PKCE stops the attack at the *token endpoint* and not at the *redirect*. Why is that the right place?

<details>
<summary><strong>Answer</strong></summary>

PKCE can't prevent the **interception** of the code at the redirect — on a public client the OS/browser may hand the redirect to a malicious app, and there's no secret there to stop it — so instead it makes the *stolen code unredeemable* by demanding, **at the token endpoint**, the `code_verifier` whose `SHA-256` equals the challenge filed earlier; the thief has the code but not the verifier, so redemption fails.

That's the **right place** because redemption is the **one chokepoint the real Authorization Server controls** and where tokens are actually issued: you can't reliably stop a code from being grabbed in transit on a public client, but you *can* refuse to mint tokens for anyone who can't prove they started the flow. Defending at the point of *value creation* (tokens) rather than the point of *delivery* (the redirect) is what makes the theft pointless — the lock is on the vault, not the hallway.

</details>

> **Exercise 2 — Reverse the hash.** Take the `code_challenge` printed by the Build It program. Try to compute the `code_verifier` from it. What property of SHA-256 makes this hopeless, and how does that property *become* the security?

<details>
<summary><strong>Answer</strong></summary>

You can't compute it. The relevant property is that SHA-256 is a **one-way (preimage-resistant) hash**: given a digest, there is no feasible way to recover an input that produces it — your only option is to *guess* inputs and hash each one, and with a high-entropy verifier (43–128 chars) the search space is astronomically large, so brute force is computationally infeasible.

How that property **becomes** the security: PKCE sends only the *hash* (`code_challenge`) on the public front channel, where an attacker can read it — but because the hash can't be reversed, seeing it reveals **nothing** about the `code_verifier`. The token endpoint, however, can cheaply verify a *claimed* verifier by hashing it forward and comparing. So one-wayness gives exactly the asymmetry you want: **trivial to check, impossible to forge.** The verifier itself never travels on the interceptable channel, so the only party who can pass the check is the app that generated it — which is the whole defense (§3).

</details>

> **Exercise 3 — Confidential vs public.** A backend server (confidential client) already has a `client_secret`. Does it *need* PKCE? Argue both sides, then look up what RFC 9700 recommends.

<details>
<summary><strong>Answer</strong></summary>

**The "doesn't strictly need it" side:** a confidential client already proves itself at the token endpoint with its `client_secret`. An attacker who intercepts the code still can't redeem it, because they lack the secret — so the specific code-interception attack PKCE was invented for (§1) is *already* blocked. In that narrow sense PKCE is redundant for a backend.

**The "should use it anyway" side (defense in depth):** the `client_secret` is a single static credential — if it ever leaks (a committed `.env`, a logged request, a misconfigured proxy), the interception attack is wide open again. PKCE adds a **per-flow** secret that's freshly generated each time and never stored, so even a leaked `client_secret` doesn't let an attacker redeem a stolen code without also having that flow's verifier. It costs almost nothing (§6: one random string + one SHA-256), and it also closes some authorization-code-injection variants.

**What RFC 9700 (the OAuth 2.0 Security BCP) recommends:** use **PKCE for *all* clients**, confidential ones included — it's no longer "public clients only." So the right answer in practice is *yes, use PKCE on the backend too*, layered on top of the client secret.

</details>

```quiz
{
  "prompt": "An attacker successfully intercepts the authorization code in a PKCE flow. Why can't they get tokens?",
  "input": "Choose one:",
  "options": [
    "The token endpoint requires the code_verifier, and SHA-256 can't be reversed from the challenge they saw",
    "The code is encrypted so they can't read it",
    "PKCE prevents the code from ever being intercepted",
    "Keycloak detects the attacker's IP address"
  ],
  "answer": "The token endpoint requires the code_verifier, and SHA-256 can't be reversed from the challenge they saw"
}
```

## In the Wild

- **[RFC 7636 — Proof Key for Code Exchange](https://datatracker.ietf.org/doc/html/rfc7636)** — the spec, including the §1 description of the interception attack that motivates it. Surprisingly readable.
- **[RFC 9700 — OAuth 2.0 Security BCP, §2.1.1](https://datatracker.ietf.org/doc/html/rfc9700)** — why PKCE is now recommended for *all* clients, confidential ones included.
- **[Cortex realm — `pkce.code.challenge.method: S256`](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)** — the one line in Cortex's `cortex-web` client that switches PKCE on. You'll see it in context in Chapter 14.

---

**Next:** the flow ends with the client holding tokens — plural. What's actually *in* an access token versus a refresh token, what do scopes limit, and why does consent matter? → [7. Access tokens, refresh tokens, scopes & consent](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/tokens-scopes-consent)
