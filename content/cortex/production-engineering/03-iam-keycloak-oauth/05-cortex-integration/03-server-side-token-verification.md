---
title: '19. Server side: verifying a bearer token end-to-end'
summary: Follow a bearer token into the Cortex server and watch it get verified — signature, issuer, audience, expiry — then turned into a typed identity. The real ZIO + Nimbus code behind everything in Chapter 11.
---

# 19. Server side: verifying a bearer token end-to-end

## TL;DR

> When a request arrives with `Authorization: Bearer <jwt>`, Cortex's server verifies it **locally** (Chapter 11): check the **RS256 signature** against Keycloak's cached **JWKS**, then the **issuer**, the **audience-or-azp**, and **expiry** — all without calling Keycloak. On success it extracts a typed **`VerifiedClaims(sub, preferredUsername, name, email)`**. Endpoints come in two flavors: **`verifyOptional`** (anonymous allowed — most endpoints) and **`verify`** (token required — `/api/me`, submissions). And a master switch, `AUTH_ENABLED`, can short-circuit the whole thing for local dev. This is ADR-0013's server half.

## 1. Motivation

Chapter 11 gave you the *cryptography* of verification. Now we read the *engineering*: how that check lives inside a real ZIO HTTP server, how a JWT becomes a typed Scala value your handlers can use, and how the optional-vs-required distinction from ADR-0013 is expressed in code. This is where the abstract "verify the token" becomes concrete lines you could write yourself — and where a few subtle Keycloak-specific details (the `azp` quirk, the disabled-auth fallbacks) matter.

## 2. Intuition (Analogy)

A nightclub's back-office, processing the wristbands the door scanned. For each guest:

1. **Is the wristband's hologram genuine?** (signature vs the public key) — forge-proof check.
2. **Was it issued by *our* venue, tonight?** (`iss` = our realm) — not last week's, not the club down the street.
3. **Is it *for our* guest list?** (`aud`/`azp`) — a wristband minted for a different event doesn't get in.
4. **Is it still tonight?** (`exp`) — yesterday's band is dead.

Only if *all four* pass does the back office write down "Guest: Ada (id #f1c2), here's what we know about her" — a clean record (the typed claims) the rest of the staff can use without re-checking the band. Any failure → "not a valid guest," politely.

## 3. Formal Definition — Cortex's verification pipeline

The verifier ([`KeycloakAuthBackend.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/KeycloakAuthBackend.scala)) is built once at startup and run per request:

```scala
// Build once: a JWKS source (fetch + cache Keycloak's public keys) and a JWT processor.
val jwksUrl = URI.create(s"${cfg.issuerUrl.stripSuffix("/")}/protocol/openid-connect/certs").toURL
val jwkSource: JWKSource[SecurityContext] = JWKSourceBuilder.create[SecurityContext](jwksUrl).build()

val processor = new DefaultJWTProcessor[SecurityContext]()
processor.setJWSKeySelector(new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)) // pin RS256
processor.setJWTClaimsSetVerifier(ClaimsVerifier(cfg))                                       // iss, aud/azp, exp

// Run per request — this is the ENTIRE check; no call to Keycloak on the hot path.
override def verify(token: String): IO[AuthFailure, VerifiedClaims] =
  ZIO.attemptBlocking(processor.process(token, null))
    .mapError(translate)        // Nimbus exceptions -> typed AuthFailure
    .flatMap(extractClaims)     // JWT claims -> VerifiedClaims
```

The four checks from Section 2 map exactly: `JWSVerificationKeySelector(RS256, jwkSource)` does the **signature** (with `kid` selection + JWKS caching, `jwksCacheTtlSec = 300`); `ClaimsVerifier(cfg)` does **iss / aud-or-azp / exp**.

The **Keycloak `azp` quirk** is worth calling out. For a public SPA, Keycloak often puts the client id in **`azp`** (authorized party) rather than `aud`. So Cortex's claims verifier accepts a token whose **`aud` contains the audience *or* whose `azp` equals it** — a small, real-world accommodation. (`audience` defaults to the `clientId`, `cortex-web`, per [`application.conf`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/application.conf).)

On success, the token becomes a typed value ([`VerifiedClaims.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/VerifiedClaims.scala)):

```scala
final case class VerifiedClaims(
    sub: String,                  // stable user id — what you key the DB on (Chapter 15)
    preferredUsername: String,    // the GitHub login, brokered through Keycloak
    name: Option[String],
    email: Option[String]
)
```

## 4. Worked Example — optional vs required, and the kill switch

ADR-0013 needs two endpoint flavors. The `Auth` service exposes both:

- **`verify(token): IO[AuthFailure, VerifiedClaims]`** — token *required*. Used by `/api/me` and submissions. No/invalid token → failure (401/503).
- **`verifyOptional(token): IO[AuthFailure, Option[VerifiedClaims]]`** — token *optional*. Used by hot endpoints like `/api/run`. No token → `None` (anonymous, allowed); a *present-but-invalid* token → failure.

```scala
// Sketch of how a handler uses each (conceptual):
//  /api/run  — anonymous OK:
val identity: Option[VerifiedClaims] = auth.verifyOptional(bearer)   // None = anonymous
rateLimiter.check(identity, clientIp)                                // per-user OR per-IP (Chapter 20)

//  /api/submissions — token required:
val who: VerifiedClaims = auth.verify(bearer)                        // fails if absent/invalid
submissions.record(who.preferredUsername, ...)
```

And the **master switch** (`AUTH_ENABLED`, from `application.conf`):

> When `enabled = false` (the local-dev default), the verifier short-circuits: **required-auth** endpoints return **503**, **optional-auth** endpoints treat every caller as **anonymous**, and the rate limiter is a **no-op**. `/api/auth/config` then reports `enabled=false`, so the SPA skips initialising `keycloak-js` (Chapter 18).

That one flag cleanly disables the entire gate for development — exactly the consequence ADR-0013 called for.

The supporting endpoints tie it together: **`/api/auth/config`** publishes `{ enabled, issuerUrl, realm, clientId }` so the SPA can configure itself; **`/api/me`** is the required-auth endpoint the client calls right after login to turn a token into a displayable identity.

## 5. Build It

Run this. It's Cortex's verification pipeline in miniature — the four checks, the azp quirk, and the optional-vs-required split — so you can see each gate do its job.

```python run
import time

EXPECTED_ISS = "https://keycloak.kakde.eu/realms/apps-prod"
EXPECTED_AUD = "cortex-web"

def verify(token, *, signature_ok=True):
    # 1. signature (here: a stand-in; real code checks RS256 vs JWKS)
    if not signature_ok:
        return ("fail", "invalid signature")
    # 2. issuer
    if token.get("iss") != EXPECTED_ISS:
        return ("fail", "wrong issuer")
    # 3. audience OR azp (the Keycloak public-client quirk)
    aud = token.get("aud", [])
    aud = aud if isinstance(aud, list) else [aud]
    if EXPECTED_AUD not in aud and token.get("azp") != EXPECTED_AUD:
        return ("fail", "wrong audience/azp")
    # 4. expiry
    if token.get("exp", 0) < time.time():
        return ("fail", "expired")
    return ("ok", {"sub": token["sub"], "preferred_username": token.get("preferred_username")})

def verify_optional(bearer):
    if bearer is None:
        return ("anonymous", None)        # no token -> allowed, anonymous
    return verify(bearer)                  # present -> must be valid

good = {"iss": EXPECTED_ISS, "azp": "cortex-web", "sub": "f1c2",
        "preferred_username": "ani2fun", "exp": time.time() + 300}

print("required, good token :", verify(good))
print("required, no token   :", "fail (401/503)")
print("optional, no token   :", verify_optional(None))      # anonymous OK
print("optional, bad issuer :", verify_optional(dict(good, iss='https://evil/realms/x')))
```

**Now break it.** Delete the `azp` check (`and token.get("azp") != EXPECTED_AUD`) and re-run. The good token — which has `azp` but *no matching `aud`* — now **fails** with "wrong audience." That's exactly the bug you'd hit verifying Keycloak public-client tokens if you only checked `aud`. Cortex's accommodation of `azp` is not sloppiness; it's correctness for how Keycloak mints SPA tokens.

## 6. Trade-offs & Complexity

| Local verification (Cortex) | Remote introspection |
|---|---|
| Per-request: a signature check (µs) | Per-request: a call to Keycloak |
| Resilient — works if Keycloak blips | Coupled to Keycloak uptime |
| No instant revocation (Chapter 3) | Instant revocation |
| `azp`/`aud` + clock-skew nuances to handle | Provider handles validity |

Cortex chooses local verification (scale, resilience) and manages revocation with short token lifetimes (Chapter 7). The complexity it takes on — JWKS caching, the `azp` accommodation, clock-skew leeway, the optional/required split — is modest and lives in one well-tested backend, exactly where it belongs.

## 7. Edge Cases & Failure Modes

- **Only checking `aud`.** Rejects valid Keycloak public-client tokens that carry the client in `azp`. Accept `aud` *or* `azp` (as Cortex does).
- **Not pinning the algorithm.** Pin RS256 in the key selector; never trust the token's `alg` (Chapter 10's forgery).
- **JWKS fetched per request, or cached forever.** Cache with a TTL (300s) *and* refresh on an unseen `kid`; otherwise you either bottleneck on JWKS or break key rotation.
- **Clock skew.** Allow a small leeway on `exp`/`nbf` so mildly out-of-sync servers don't reject good tokens.
- **Leaking why verification failed.** Return a generic 401 to clients; log the specific reason server-side. Don't tell an attacker *which* check failed.

## 8. Practice

> **Exercise 1 — The four checks.** List the four things Cortex verifies on a token, in order, and state what each one defends against. Why must the signature be first?

<details>
<summary><strong>Answer</strong></summary>

In order (the nightclub back-office from §2):

1. **Signature** (RS256 vs Keycloak's JWKS, `kid`-selected) — defends against **forgery**: proves the token was minted by Keycloak's private key and not tampered with in transit. Without this, every other field is just attacker-supplied JSON.
2. **Issuer (`iss`)** — defends against a **valid token from the wrong realm/provider**: it must be *our* realm, not another tenant's or a look-alike's.
3. **Audience or `azp`** — defends against a **token minted for a different client** being replayed at us (the confused-deputy case); Cortex accepts `aud` containing `cortex-web` *or* `azp == cortex-web` (the Keycloak public-client quirk).
4. **Expiry (`exp`, + small skew)** — defends against **replay of an old/stolen token**; short lifetimes keep the leak window small.

**Signature must be first** because it's what makes the other three *trustworthy*. `iss`, `aud`, and `exp` are just claims *inside* the token; if the signature is invalid, an attacker wrote those claims themselves, so checking them on an unverified token is meaningless. You authenticate the messenger before you believe the message — and pinning RS256 here (never trusting the token's own `alg`) is what closes the `alg: none` / RS256→HS256 forgery class.

</details>

> **Exercise 2 — optional vs required.** For each endpoint, choose `verify` or `verifyOptional` and justify it per ADR-0013: `/api/run`, `/api/me`, `POST /api/submissions`, `GET /api/health`.

<details>
<summary><strong>Answer</strong></summary>

Match each endpoint to the gate: required auth only where identity is *necessary*; optional where anonymous use is a product goal.

- **`/api/run` → `verifyOptional`.** ADR-0013 makes anonymous running first-class, so a missing token must mean **`None` (anonymous, allowed)**, metered per-IP. A *present* token must still verify (so a signed-in user gets their per-user budget) — which is exactly what `verifyOptional` does: `None` for no token, failure for a present-but-invalid one.
- **`/api/me` → `verify`.** Its entire job is to turn a token into an identity; with no token there's nothing to return. Token **required** → 401/503 without one.
- **`POST /api/submissions` → `verify`.** Submitting is gated *and* attributed (allow-list keyed on identity), so a stable verified `sub` is mandatory. **Required.**
- **`GET /api/health` → neither (no auth).** It's an unauthenticated liveness/readiness probe for load balancers and uptime checks; it exposes no user data and must answer even when the IdP is down. Wrapping it in auth would be both pointless and harmful (a Keycloak blip would make you look unhealthy).

The rule: `verifyOptional` when anonymous is a *valid* state for that endpoint; `verify` when the endpoint is meaningless or unsafe without an identity; no auth at all for infrastructure endpoints that carry no user data.

</details>

> **Exercise 3 — The azp quirk.** In two sentences, explain why Cortex accepts a token whose `azp` (not `aud`) equals `cortex-web`. What about Keycloak public clients makes this necessary?

<details>
<summary><strong>Answer</strong></summary>

For a **public SPA client**, Keycloak frequently records the client id in the **`azp` (authorized party)** claim rather than populating `aud` — a public client requesting tokens only for itself often gets no matching `aud` entry, so a verifier that checked *only* `aud` would reject Cortex's own perfectly valid tokens. Cortex therefore accepts a token whose `aud` contains `cortex-web` **or** whose `azp` equals `cortex-web`, which is the correct way to confirm "this token was meant for *us*" given how Keycloak mints SPA tokens.

This is correctness, not laxness: the check still pins the token to *our* client (`cortex-web`) — it just looks in both fields Keycloak might use. Drop the `azp` branch and you'd hit the exact "wrong audience" failure on legitimate logins that the chapter's "Now break it" demonstrates.

</details>

```quiz
{
  "prompt": "Which endpoint flavor does Cortex use for `/api/run`, and why?",
  "input": "Choose one:",
  "options": [
    "verifyOptional — anonymous callers are allowed (and metered per-IP); a present token must still be valid",
    "verify — every run requires a valid token",
    "No verification at all",
    "Remote introspection on every request"
  ],
  "answer": "verifyOptional — anonymous callers are allowed (and metered per-IP); a present token must still be valid"
}
```

## In the Wild

- **[Nimbus JOSE + JWT](https://connect2id.com/products/nimbus-jose-jwt)** — the Java library Cortex uses (`DefaultJWTProcessor`, `JWKSourceBuilder`, `JWSVerificationKeySelector`).
- **[Cortex `KeycloakAuthBackend.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/KeycloakAuthBackend.scala)** & **[`VerifiedClaims.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/auth/VerifiedClaims.scala)** — the real verifier and typed identity.
- **[Keycloak — `azp` and audience handling](https://www.keycloak.org/docs/latest/server_admin/#_audience)** — why public clients land the client id in `azp`, motivating Cortex's check.

---

**Next:** verification produced an identity — *or* `None` for anonymous. That fork feeds straight into the rate limiter, which meters anonymous callers by IP and signed-in callers by user. → [20. Identity-aware rate limiting](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/identity-aware-rate-limiting)
