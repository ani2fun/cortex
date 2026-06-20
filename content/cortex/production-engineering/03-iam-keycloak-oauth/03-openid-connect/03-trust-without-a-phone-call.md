---
title: '11. Trust without a phone call: JWKS & signature verification'
summary: How can the Cortex server be certain Keycloak signed a token, without calling Keycloak on every request? Public-key cryptography, a published set of keys, and one signature check — done locally, millions of times a second.
---

# 11. Trust without a phone call: JWKS & signature verification

## TL;DR

> Keycloak signs every token with a **private key** only it holds. It publishes the matching **public key** at a JSON URL called **JWKS** (`…/protocol/openid-connect/certs`). Any server — including Cortex's — downloads that public key *once*, caches it, and from then on **verifies** tokens by checking the signature locally with the public key. No per-request call to Keycloak. The token's `kid` says *which* public key to use; the header's `alg` (pinned to **RS256** by the verifier, not trusted from the token) says *how*. This is the cryptographic heart of stateless auth, and Cortex's `KeycloakAuthBackend.scala` does exactly this.

## 1. Motivation

We can read a token (Chapter 10). But reading isn't trusting — anyone can *write* a JSON payload that says `"sub": "admin"`. The question that makes tokens actually useful is: **how does the Cortex API know Keycloak — and only Keycloak — produced this token, and that nobody altered it?**

The naive answer is "ask Keycloak." For every single API request, call Keycloak: "is this token real?" That works, but it re-creates the very bottleneck stateless tokens were meant to avoid (Chapter 3) — now Keycloak is a single point of failure on the hot path of every request, in every region.

The elegant answer uses **asymmetric cryptography** so the server can verify *offline*. Keycloak signs with a secret it never shares; it publishes the *public* counterpart that can only *verify*, never *forge*. Download that public key once, and you can check a billion tokens without ever talking to Keycloak again. That's the trick this chapter unpacks.

## 2. Intuition (Analogy)

Think of a **wax seal from a signet ring** — but a magic one.

A medieval king seals letters with his signet ring. Anyone in the kingdom owns a *picture* of the seal, so they can check a letter's seal is genuine. But only the king has the *ring*, so only he can *make* a valid seal. Checking is public; sealing is private.

That's **asymmetric (public-key) signing**:

| Royal seal | RS256 token signing |
|---|---|
| The king's signet **ring** (secret) | Keycloak's **private key** — signs tokens, never shared |
| The **picture** of the seal (public) | Keycloak's **public key** — published at JWKS, verifies only |
| "Is this the king's seal?" | "Does this signature match Keycloak's public key?" |
| Anyone can check | Any server can verify, offline |
| Only the king can seal | Only Keycloak can sign |

The asymmetry is everything. If signing and verifying used the *same* key (that's "symmetric," like `HS256`), then anyone who can *verify* could also *forge*. With RS256, the public key can *only check*, never *create* — so it's safe to hand to the whole world. That's why the verifier can live on every Cortex pod with no secret at all.

## 3. Formal Definition

- **RS256** = RSA signature with SHA-256. Keycloak hashes `header.payload`, then signs the hash with its **RSA private key**. A verifier recomputes the hash and uses the **RSA public key** to confirm the signature matches. Forging requires the private key; verifying needs only the public one.
- **JWKS** (JSON Web Key Set) = a JSON document listing the AS's current public keys, each with a **`kid`** (key id), at a well-known URL. Cortex's is derived as `{issuerUrl}/protocol/openid-connect/certs`.
- **`kid` matching** = the token's header carries a `kid`; the verifier picks the JWKS key with the same `kid`. This lets the AS **rotate** keys: publish a new key, start signing with it, keep the old one in JWKS until old tokens expire.
- **Verification** = check, in order: the **signature** (against the right public key, with the **pinned** algorithm), then `iss` (issuer is exactly who we expect), `aud`/`azp` (the token is for us), and `exp`/`nbf`/`iat` (it's within its validity window, allowing small clock skew). *All* must pass.

> **Pin the algorithm; don't trust the header.** The verifier decides "I only accept RS256," rather than believing the token's own `alg`. This blocks the `alg: none` and `RS256→HS256` confusion attacks from Chapter 10. Cortex pins RS256 explicitly.

## 4. Worked Example — Cortex's real verifier

This is the actual setup code from Cortex's server, in [`KeycloakAuthBackend.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/KeycloakAuthBackend.scala). It uses the Nimbus JOSE library and reads almost like the definition above:

```scala
// Derive the JWKS URL from the realm's issuer URL, then build a key source
// that fetches + caches Keycloak's public keys.
val jwksUrl = URI.create(s"${cfg.issuerUrl.stripSuffix("/")}/protocol/openid-connect/certs").toURL
val jwkSource: JWKSource[SecurityContext] =
  JWKSourceBuilder.create[SecurityContext](jwksUrl).build()

val processor = new DefaultJWTProcessor[SecurityContext]()
// Pin RS256 — the verifier chooses the algorithm, NOT the token's header.
processor.setJWSKeySelector(new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource))
// Then enforce the claim rules: iss must match, aud/azp must contain us, exp must be future.
processor.setJWTClaimsSetVerifier(ClaimsVerifier(cfg))

// Per request: this is the ENTIRE verification — local, no call to Keycloak.
ZIO.attemptBlocking(processor.process(token, null)).mapError(translate).flatMap(extractClaims)
```

Read it against Section 3. `JWKSourceBuilder` fetches and caches the JWKS (Cortex caches it for `jwksCacheTtlSec = 300` seconds and re-fetches on a `kid` it hasn't seen — so key rotation just works). `JWSVerificationKeySelector(RS256, …)` pins the algorithm and selects the key by `kid`. `ClaimsVerifier` enforces `iss`/`aud`/`exp`. And `processor.process(token, null)` is the whole check — **a local computation**, no network call to Keycloak on the request path. (Chapter 19 walks the surrounding ZIO code; here, focus on the crypto.)

## 5. Build It

Run this. It does real RS256 signing and verification so you can watch the asymmetry: the private key signs, the *public* key verifies, and a tampered token fails.

```python run
# Uses Python's built-in `hashlib`/`hmac`? No — we need asymmetric keys.
# `cryptography` ships in the sandbox; if not, the concept stands regardless.
try:
    from cryptography.hazmat.primitives.asymmetric import rsa, padding
    from cryptography.hazmat.primitives import hashes
    from cryptography.exceptions import InvalidSignature
except ImportError:
    print("cryptography not available here — but the logic below is what RS256 does.")
    raise SystemExit

# Keycloak holds BOTH; it publishes only the public one at JWKS.
private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
public_key  = private_key.public_key()        # <-- this is what JWKS publishes

signing_input = b"eyJhbG.eyJzdWIiOiJhbmkyZnVuIn0"   # header.payload bytes

# ---- Keycloak signs (needs the PRIVATE key) ----
signature = private_key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())

# ---- Cortex verifies (needs only the PUBLIC key) ----
def verify(data, sig):
    try:
        public_key.verify(sig, data, padding.PKCS1v15(), hashes.SHA256())
        return "VALID — Keycloak signed this, untampered"
    except InvalidSignature:
        return "INVALID — forged or altered"

print("Genuine token:  ", verify(signing_input, signature))

# ---- Tamper with the payload: signature no longer matches ----
tampered = b"eyJhbGc.eyJzdWIiOiJhZG1pbiJ9"   # attacker changed sub -> admin
print("Tampered token: ", verify(tampered, signature))

# ---- Can an attacker with ONLY the public key forge a signature? ----
print("\nThe public key can verify but CANNOT sign — forging needs the private key Keycloak never shares.")
```

**Now break it.** Try to make the tampered token verify. You can't — to produce a signature that matches the altered bytes you'd need the private key, which never leaves Keycloak. That one-way property (verify-yes, forge-no) is *exactly* why the public key is safe to publish to the entire internet at the JWKS URL.

## 6. Trade-offs & Complexity

| | Local verification (JWKS + RS256) | Remote introspection (call the AS) |
|---|---|---|
| Per-request cost | A local signature check (microseconds) | A network round-trip to the AS |
| AS as bottleneck | No — verify offline | Yes — every request hits it |
| Instant revocation | No (token valid till `exp`) | Yes (AS can say "revoked") |
| Key rotation | Handled via `kid` + JWKS refresh | N/A |
| Best for | High-volume, multi-service systems | When instant revocation is mandatory |

Cortex chooses local verification — scale and resilience over instant revocation — and manages the downside with short token lifetimes (Chapter 7). For most systems that's the right call; the AS staying up is no longer on the critical path of every request.

## 7. Edge Cases & Failure Modes

- **Trusting `alg` from the token.** The classic forge. *Pin* the algorithm server-side (Cortex pins RS256). Never let the token choose how it's verified.
- **Not caching JWKS (or caching forever).** Fetching JWKS per request re-creates the bottleneck; caching forever breaks key rotation. The sweet spot: cache with a TTL *and* refresh on an unseen `kid`. Cortex does both.
- **Clock skew.** `exp` is checked against the verifier's clock. Allow a small leeway (a minute or two) so mildly out-of-sync servers don't reject valid tokens.
- **Wrong issuer.** A valid signature from the *wrong* Keycloak realm is still wrong. Always check `iss` matches the exact expected realm URL.
- **JWKS endpoint down at cold start.** If the verifier can't fetch keys on boot, it can't verify anything. Cache resiliently and fail loudly.

## 8. Practice

> **Exercise 1 — Why publish the public key?** In two sentences, explain why it's safe to publish Keycloak's signing public key to the entire internet, but catastrophic to publish the private key.

<details>
<summary><strong>Answer</strong></summary>

The public key can only **verify** signatures, never **create** them — that's the asymmetry of RS256 — so handing it to the whole world lets anyone *check* that Keycloak signed a token while giving them zero power to *forge* one. The private key is the opposite: it can *sign*, so anyone holding it could mint a token claiming `"sub": "admin"` that verifies perfectly — leaking it means anyone can impersonate any user, which is total compromise. The one-way property (verify-yes, forge-no) is precisely *why* the public key is safe at the JWKS URL and the private key must never leave Keycloak.

</details>

> **Exercise 2 — Trace a rotation.** Keycloak rotates its signing key. Old tokens carry the old `kid`; new tokens carry a new `kid`. Describe how a verifier that "caches JWKS but refreshes on an unseen `kid`" keeps working through the rotation without downtime.

<details>
<summary><strong>Answer</strong></summary>

The `kid` in each token's header is what makes this seamless — it names *which* public key verifies that token, so the verifier never has to guess.

- **Before rotation:** the verifier has the old key cached and verifies old-`kid` tokens locally, no network call.
- **Keycloak rotates:** it publishes a *new* key alongside the old one in JWKS, starts signing new tokens with the new `kid`, but keeps the old key in JWKS until all old tokens expire.
- **First new token arrives:** its `kid` isn't in the cache. Rather than reject it, the verifier treats an *unseen `kid`* as a signal to **re-fetch JWKS**, picks up the new key, caches it, and verifies — succeeding on the very first try.
- **Meanwhile:** old-`kid` tokens still verify against the still-cached old key. Both key generations validate side by side during the overlap window.

So there's no downtime: the verifier never needed a restart or a config change. Publishing both keys during the overlap + selecting by `kid` + refreshing on an unknown `kid` means the rotation is invisible to every running pod. (Pure TTL caching *alone* would briefly fail new tokens until the TTL lapsed; the unseen-`kid` refresh is what closes that gap. Cortex does both.)

</details>

> **Exercise 3 — Order the checks.** List the checks Cortex performs on a token (signature, `iss`, `aud`/`azp`, `exp`) and argue why signature must come first. What would go wrong if you read claims *before* verifying the signature?

<details>
<summary><strong>Answer</strong></summary>

Cortex checks, in order:

1. **Signature** — against the right JWKS public key (selected by `kid`), using the **pinned** algorithm (`RS256`, chosen by the verifier, *not* read from the token's `alg`).
2. **`iss`** — the issuer is *exactly* the expected realm URL.
3. **`aud` / `azp`** — the token was minted for us (Keycloak puts the public client in `azp`).
4. **`exp`** (and `nbf`/`iat`) — it's within its validity window, allowing small clock skew.

**Signature must come first because it is what makes every other claim trustworthy.** Until the signature verifies, the payload is just *attacker-controllable JSON* — recall from Chapter 10 that anyone can *write* `{"iss": "...the-real-realm...", "aud": "cortex-web", "sub": "admin"}` and base64url it. If you read and *act on* claims before verifying, an attacker simply forges a payload with all the "correct" values and you wave it through; the `iss`/`aud`/`exp` checks become theater because the attacker filled them in. Verifying the signature first proves the bytes genuinely came from Keycloak and weren't altered — *only then* is it meaningful to ask what `iss`, `aud`, and `exp` say. (Reading claims to *decide which key/issuer to check against* is fine; **trusting** them before the signature passes is the bug.)

</details>

```quiz
{
  "prompt": "Why can the Cortex API verify a Keycloak token WITHOUT calling Keycloak on each request?",
  "input": "Choose one:",
  "options": [
    "It caches Keycloak's public key (from JWKS) and checks the RS256 signature locally — the public key verifies but cannot forge",
    "It stores every issued token in a shared database",
    "Tokens are encrypted with a shared secret both sides know",
    "It calls Keycloak once per request to introspect the token"
  ],
  "answer": "It caches Keycloak's public key (from JWKS) and checks the RS256 signature locally — the public key verifies but cannot forge"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[RFC 7517 — JSON Web Key (JWK) & JWKS](https://datatracker.ietf.org/doc/html/rfc7517)** — the format of the keys document Cortex fetches.
- **[Cortex's JWKS endpoint](https://keycloak.kakde.eu/realms/apps-prod/protocol/openid-connect/certs)** — the actual public keys Cortex verifies against (live JSON, if the realm is reachable).
- **[`KeycloakAuthBackend.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/KeycloakAuthBackend.scala)** — the real verifier excerpted above. Chapter 19 reads the rest of it.

---

**Next:** we've earned the entire protocol from first principles. Time to meet the product that implements it so you don't have to — Keycloak — starting with its mental model, explored interactively. → [12. The Keycloak mental model: realms, clients, users, roles](/cortex/production-engineering/iam-keycloak-oauth/keycloak/the-keycloak-mental-model)
