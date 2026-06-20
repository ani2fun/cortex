---
title: '10. Anatomy of a JWT, decoded by hand'
summary: A JWT looks like an unreadable blob of random characters. It is nothing of the sort — it is three pieces of base64url-encoded JSON you can read with your eyes. Crack one open and inspect every claim.
---

# 10. Anatomy of a JWT, decoded by hand

## TL;DR

> A **JWT** (JSON Web Token) is three base64url-encoded chunks joined by dots: **`header.payload.signature`**. The header says how it's signed; the payload is a JSON bag of **claims** (who, for whom, until when); the signature lets a server prove the token wasn't tampered with. Critically, a JWT is **signed, not encrypted** — *anyone* can read the payload, so never put secrets in it. Below you'll decode a real Keycloak token live, edit it, and watch the claims change.

## 1. Motivation

The first time you see a JWT, it looks like line noise:

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVC… .eyJpc3MiOiJodHRwczovL2tleWNsb2Fr… .ZmFrZS1zaWduYXR1cmU…
```

People treat it as an opaque magic string. That instinct is exactly backwards, and it leads to real bugs (like storing secrets in it, or trusting it without checking the signature). The truth is liberating: **a JWT is just JSON in a thin disguise.** The `eyJ…` prefix is the dead giveaway — it's what `{"` becomes in base64url, every single time. Once you can decode one by eye, JWTs stop being scary and start being *debuggable*.

## 2. Intuition (Analogy)

A JWT is a **tamper-evident envelope with a wax seal**, not a locked box.

- The **header** is the label on the front: "Sealed with the King's RS256 stamp."
- The **payload** is the letter inside — and the envelope is *transparent*. Anyone can read the letter. (That's why you don't write secrets in it.)
- The **signature** is the **wax seal**. You can't forge the King's seal without the King's signet ring (the private key), and if anyone alters the letter, the seal no longer matches. Tampering is *detectable*, even though reading is free.

| Envelope part | JWT part | Job |
|---|---|---|
| Transparent window | base64url encoding | Makes it readable/transportable — **not** secret |
| The label | Header | Which algorithm + which key signed it |
| The letter | Payload (claims) | The actual statements: who, for whom, until when |
| The wax seal | Signature | Proves integrity + authenticity (Chapter 11) |

The key insight people miss: **base64url is not encryption.** It's just a way to write bytes using URL-safe characters. Decoding it requires no key and reveals everything. The *security* is in the signature, not in the encoding.

## 3. Formal Definition

A JWT (RFC 7519) in its common signed form (a "JWS") is:

```
BASE64URL(header) + "." + BASE64URL(payload) + "." + BASE64URL(signature)
```

- **Header** — JSON with at least `alg` (signing algorithm, e.g. `RS256`) and `typ` (`JWT`). Keycloak adds `kid`, the **key id**, naming *which* public key verifies it (crucial in Chapter 11).
- **Payload** — JSON **claims**. Standard registered claims include `iss` (issuer), `sub` (subject — the stable user id), `aud` (audience), `exp` (expiry, Unix seconds), `iat` (issued-at), `nbf` (not-before), `jti` (token id). Providers add their own: Keycloak gives `azp`, `preferred_username`, `email`, `realm_access.roles`, etc.
- **Signature** — the bytes produced by signing `BASE64URL(header) + "." + BASE64URL(payload)` with the key named by `alg`/`kid`. Verifying it is Chapter 11.

> **Signed ≠ encrypted.** A standard JWT's payload is *plaintext-readable* by anyone who has the token. Put nothing in it you wouldn't put on a postcard. (There's a separate, rarer "JWE" form that *is* encrypted; OIDC ID/access tokens are normally JWS — readable.)

## 4. Worked Example — decode a real one

Below is a real-shaped Keycloak access token for the `cortex-web` client. **Decode it with your eyes** in the widget: the three colour-coded segments are header/payload/signature, the panels show the decoded JSON, and the table annotates each claim. Then **edit the token** in the box — change `preferred_username` to your name (you'll need to re-encode, or just tweak a visible character and watch the decode react). The signature won't verify — that's Chapter 11's job — but reading is free.

```d3 widget=jwt-inspector
{
  "title": "A captured Keycloak access token for cortex-web — decode it, edit it",
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InNCZjdjMlF4OUtwTCJ9.eyJleHAiOjE3MTgyODM2MDAsImlhdCI6MTcxODI4MzMwMCwianRpIjoiNmIxZjJjMzQtOWE3ZS00YzJkLWJiMTAtM2U1ZjBhMWQyYzQ0IiwiaXNzIjoiaHR0cHM6Ly9rZXljbG9hay5rYWtkZS5ldS9yZWFsbXMvYXBwcy1wcm9kIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImY6MWMyYjphbmkyZnVuLXN0YWJsZS11dWlkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY29ydGV4LXdlYiIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhbmkyZnVuIiwiZW1haWwiOiJhLnIua2FrZGVAZ21haWwuY29tIiwibmFtZSI6IkFuaWtldCBLYWtkZSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLWFwcHMtcHJvZCIsInN1Ym1pdHRlciJdfX0.ZmFrZS1zaWduYXR1cmUtbm90LXZlcmlmaWVkLWluLXRoaXMtd2lkZ2V0"
}
```

> Things to notice in the decoded claims: `iss` is Cortex's real realm URL; `azp` is `cortex-web` (the public client — recall Keycloak puts the client in `azp`, which Chapter 19's verifier checks); `sub` is the stable user id you'd key your database on (not the username); and `exp` is in the past, so the inspector flags it **expired** — this is a *captured sample*. The `realm_access.roles` array is Keycloak's RBAC riding inside the token (Chapter 16).

## 5. Build It

Run this. It decodes a JWT the way the widget does — pure base64url, no library, no key — proving there's no magic.

```python run
import base64, json

token = (
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InNCZjdjMlF4OUtwTCJ9"
    ".eyJleHAiOjE3MTgyODM2MDAsImlzcyI6Imh0dHBzOi8va2V5Y2xvYWsua2FrZGUuZXUvcmVhbG1zL2FwcHMtcHJvZCIsImF6cCI6ImNvcnRleC13ZWIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhbmkyZnVuIn0"
    ".c2lnbmF0dXJl"
)

def b64url_decode(seg: str) -> bytes:
    seg += "=" * (-len(seg) % 4)               # restore padding
    return base64.urlsafe_b64decode(seg)

header_b64, payload_b64, signature_b64 = token.split(".")

print("HEADER :", json.loads(b64url_decode(header_b64)))
print("PAYLOAD:", json.loads(b64url_decode(payload_b64)))
print("SIG    :", signature_b64, "(opaque bytes — verifying it needs the public key, Chapter 11)")

print("\nNotice: we read every claim WITHOUT any secret. base64url is encoding, not encryption.")
```

**Now break it.** Change a character inside `payload_b64` (say, flip a letter) and re-run. The decode still works (it's just bytes) — but in a *real* verification, that one altered character would make the signature check fail, because the signature was computed over the original bytes. Decoding always succeeds; **only verification catches tampering**. That distinction is the whole next chapter.

## 6. Trade-offs & Complexity

| | JWT (self-contained) | Opaque token (random string) |
|---|---|---|
| Readable by the holder | Yes — claims are visible | No — meaningless without the AS |
| Verify without calling the AS | Yes (signature check) | No — must introspect at the AS |
| Size | Larger (carries claims) | Tiny |
| Revoke instantly | Hard (Chapter 3) | Easy (AS forgets it) |
| Debuggability | High — paste into a decoder | Low |

JWTs trade size and easy-revocation for **self-contained verifiability** — any service can check one with just a public key, no network call. That's why they dominate OIDC and why Cortex uses them. The cost (revocation) is managed with short lifetimes, as we saw.

## 7. Edge Cases & Failure Modes

- **The `alg: none` attack.** Early JWT libraries accepted tokens whose header said `alg: none` (i.e., *unsigned*), letting attackers forge any payload. Always reject `none` and pin the expected algorithm. (Cortex's verifier pins `RS256` — Chapter 19.)
- **Putting secrets in the payload.** It's a postcard. Passwords, API keys, PII you wouldn't share — none belong in a JWT.
- **Confusing decode with verify.** Reading claims (no key) is *not* trusting them. A token is only trustworthy *after* signature + `iss` + `aud` + `exp` checks pass.
- **Trusting `alg` from the token itself.** An attacker controls the header. Don't let the token tell you how to verify it (e.g., switching `RS256` → `HS256` to abuse the public key as an HMAC secret). The *server* decides the algorithm.

## 8. Practice

> **Exercise 1 — Spot the segments.** In the widget's token, identify where the first dot is. Decode the header in your head: it starts `eyJhbGciOiJSUzI1Ni…` — what are the first two JSON keys, and what's the algorithm?

<details>
<summary><strong>Answer</strong></summary>

The first dot ends the **header** segment (everything before it is `BASE64URL(header)`); the second dot ends the payload, leaving the signature last — `header.payload.signature`.

You can read the header by eye because of the giveaway from §1: `eyJ` is always what `{"` becomes in base64url. Decoding `eyJhbGciOiJSUzI1Ni…` gives JSON beginning `{"alg":"RS256",…}`, so:

- the first two keys are **`alg`** and **`typ`**,
- the **algorithm is `RS256`** (RSA signature with SHA-256 — Chapter 11).

(Keycloak also adds `kid`, the key id naming *which* public key verifies it.) No key was needed to read any of this — base64url is encoding, not encryption.

</details>

> **Exercise 2 — Read the claims that matter.** From the decoded payload, write down `iss`, `aud`, `azp`, `sub`, and `exp`. For each, say in a few words why a verifier cares about it.

<details>
<summary><strong>Answer</strong></summary>

Decoding the widget's payload yields (the verifier cares about each because together they answer *did the right issuer mint this, for us, about whom, still valid?*):

- **`iss`** = `https://keycloak.kakde.eu/realms/apps-prod` — the **issuer**. The verifier checks it matches the *exact* expected realm URL; a valid signature from the wrong realm is still wrong.
- **`aud`** = `account` — the **audience** the token was minted for. The verifier confirms the token is intended for it (not minted for someone else), which is what closes the substitution hole.
- **`azp`** = `cortex-web` — the **authorized party** (the client). Keycloak puts the public SPA in `azp`; Cortex's verifier checks this to confirm the token came through *our* client (Chapter 19).
- **`sub`** = the stable user id (`f:1c2b:…`) — the **subject**. This, not the username, is the durable identity you key a database on; usernames and emails change, `sub` doesn't.
- **`exp`** = a Unix-seconds timestamp — **expiry**. The verifier rejects the token once the current time passes it, bounding the damage from a leaked token (the freshness trade-off from Chapter 3).

</details>

> **Exercise 3 — Why is `exp` essential?** The sample token is expired. Explain what a server should do when it sees a token whose `exp` is in the past, and why "the signature is valid" is *not* enough on its own.

<details>
<summary><strong>Answer</strong></summary>

When `exp` is in the past, the server must **reject the token** — treat it as unauthenticated — even though it decodes fine and even if the signature checks out (allowing only a small clock-skew leeway of a minute or two for mildly out-of-sync servers).

"The signature is valid" only proves the token is **authentic and untampered** — that Keycloak really minted *these exact bytes*. It says nothing about whether the token is still *meant to be honored*. A signature is permanent; a token's authority is deliberately temporary. Without an `exp` check, a token leaked or captured once (like this very sample) would be a *forever* key — exactly the freshness/revocation problem from Chapter 3. `exp` is what bounds the blast radius of a stolen token to a short window, which is why verification is **signature *and* `iss` *and* `aud` *and* `exp`**, all of which must pass — not the signature alone.

</details>

```quiz
{
  "prompt": "You base64url-decode a JWT's payload and read all the claims with no key at all. What does this prove about JWTs?",
  "input": "Choose one:",
  "options": [
    "They are signed, not encrypted — the payload is readable by anyone, so never store secrets in it",
    "They are encrypted, so only the server can read them",
    "The token is forged because real ones can't be decoded",
    "base64url decoding is the same as verifying the signature"
  ],
  "answer": "They are signed, not encrypted — the payload is readable by anyone, so never store secrets in it"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[RFC 7519 — JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)** — the spec, including the registered claim names (`iss`, `sub`, `aud`, `exp`, …).
- **[jwt.io](https://jwt.io/)** — paste any JWT and decode it; the widget above is a focused, offline cousin of this classic tool.
- **[Critical vulnerabilities in JWT libraries (Auth0, 2015)](https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/)** — the write-up that made the `alg: none` and `RS256→HS256` attacks famous. Essential reading before you ever verify a JWT yourself.

---

**Next:** we can *read* a token. But how does the Cortex server *trust* one — confirm Keycloak really signed it — without phoning Keycloak on every request? The answer is public-key cryptography and a little document called JWKS. → [11. Trust without a phone call: JWKS & signature verification](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/trust-without-a-phone-call)
