---
title: '9. OAuth authorizes; OIDC identifies'
summary: OAuth was built to delegate access, not to prove who you are — and using it for login has subtle holes. OpenID Connect is the thin, standard identity layer bolted on top that does login right.
---

# 9. OAuth authorizes; OIDC identifies

## TL;DR

> OAuth 2.0 answers *"may this app access this resource?"* — **authorization**. It was never designed to answer *"who is the user?"* — **authentication** — and bending it to do so (reading a user's identity out of an *access token*) is subtly unsafe. **OpenID Connect (OIDC)** is a small standard layer *on top of* OAuth that adds a proper answer: an **ID token** (a JWT *about the user*), a **UserInfo** endpoint, and a **discovery** document. When you "Log in with Google," you're using OIDC. Cortex uses OIDC — that's why its tokens carry a `preferred_username` and the SPA can show who you are.

## 1. Motivation

OAuth solved delegation beautifully, so developers reached for it to solve *login* too: "I got an access token from Google, so the user must be whoever Google says." It mostly worked — and then people found the holes.

The core problem: an **access token is meant for the Resource Server, not the client.** It's an opaque "bearer of this may access X" — it isn't *addressed to your app*, and it isn't *about the user* in any verifiable way. Two specific dangers followed:

- **The confused-deputy / token-substitution problem.** An access token your app received might have been minted for a *different* app. If you treat "I hold a valid token" as "this is my user," an attacker can feed you a token issued elsewhere and impersonate someone.
- **No standard identity shape.** Every provider returned user info differently. There was no agreed "here is the authenticated user, in a token your app can verify is for *you*."

In 2014 the industry standardized the fix: **OpenID Connect**. It doesn't replace OAuth — it's a *profile* on top of it that adds exactly the missing identity piece, in an interoperable way. Learn it once, log into anything.

## 2. Intuition (Analogy)

Back to the valet. An **access token** is the *valet key* — it lets the holder operate the car. Useful, but it says nothing trustworthy about *who* the valet is; it's just "whoever holds this may drive."

OIDC adds an **ID badge with the valet's photo, issued by the hotel and stamped 'for the front desk of Hotel X.'** Now the front desk (your app) can look at the badge and know *who* this is — and crucially, the badge says *who it was issued to* and *who it's about*, so a badge meant for a different hotel won't pass. The valet key lets you *do* things; the ID badge tells you *who's there*. Different jobs, different documents.

| | Access token (OAuth) | ID token (OIDC) |
|---|---|---|
| Audience | The **Resource Server / API** | The **client app** |
| Purpose | "Bearer may access resources" | "Here is *who* authenticated" |
| Who reads it | The API | Your app |
| Analogy | The valet key | The photo ID badge |

## 3. Formal Definition

**OpenID Connect** is an identity layer on top of OAuth 2.0. It keeps OAuth's flows (you still do Authorization Code + PKCE) and adds:

- **The `openid` scope.** Requesting it signals "I want authentication, not just access." This is the switch that turns an OAuth flow into an OIDC flow.
- **The ID token.** A **JWT** returned *to the client* alongside the access token. It is *about the user* and carries standard claims: `iss` (issuer), `sub` (the stable subject id — the real user identity), `aud` (the client it was issued *for*), `exp`/`iat` (validity), and optionally `email`, `name`, `preferred_username`, etc. Because `aud` names *your* client and the token is signed, you can verify it was minted *for you* — closing the substitution hole.
- **The UserInfo endpoint.** An API the client can call with the access token to fetch additional user claims.
- **Discovery.** A well-known URL — `…/.well-known/openid-configuration` — that publishes every endpoint and the keys location, so a client can configure itself automatically.

> **The rule that fixes the holes:** authenticate users from the **ID token** (it's *for your app* and *about the user*), not from a bare access token. The access token is for *calling APIs*; the ID token is for *knowing who logged in*.

## 4. Worked Example — discovery, live

OIDC's discovery document is a real URL you can fetch right now. Cortex's Keycloak publishes one at `https://keycloak.kakde.eu/realms/apps-prod/.well-known/openid-configuration`. It's a JSON map of the whole identity surface:

```json
{
  "issuer": "https://keycloak.kakde.eu/realms/apps-prod",
  "authorization_endpoint": ".../protocol/openid-connect/auth",
  "token_endpoint": ".../protocol/openid-connect/token",
  "userinfo_endpoint": ".../protocol/openid-connect/userinfo",
  "jwks_uri": ".../protocol/openid-connect/certs",
  "response_types_supported": ["code", "id_token", "..."],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256", "..."],
  "scopes_supported": ["openid", "profile", "email", "..."]
}
```

Everything a client needs is here: where to send the user (`authorization_endpoint`), where to exchange the code (`token_endpoint`), where to fetch the signing keys (`jwks_uri` — the star of Chapter 11), and what it supports. `keycloak-js` fetches this on startup so you never hard-code endpoints. *That single URL is why OIDC clients are interchangeable* — point the same library at any compliant provider and it configures itself.

## 5. Build It

Run this. It shows the danger of authenticating from an access token, and how an ID token's `aud` fixes it.

```python run
# Two tokens arrive at YOUR app ("cortex-web"). Which proves who the user is?

access_token = {
    "typ": "Bearer",
    "azp": "cortex-web",
    "sub": "user-123",
    # NOTE: an access token is addressed to the API, not to you.
    # It carries no promise it was minted *for your app*.
}

id_token = {
    "iss": "https://keycloak.kakde.eu/realms/apps-prod",
    "aud": "cortex-web",      # <-- addressed to YOUR client
    "sub": "user-123",        # <-- the stable user identity
    "preferred_username": "ani2fun",
}

MY_CLIENT_ID = "cortex-web"

def authenticate_from_id_token(tok):
    # The ID token is *for us* and *about the user* — verify the audience.
    if tok.get("aud") != MY_CLIENT_ID:
        return "REJECTED — this ID token was not issued for us"
    return f"Authenticated user: {tok['sub']} ({tok.get('preferred_username')})"

print(authenticate_from_id_token(id_token))   # OK — aud matches us

# An attacker feeds us an ID token minted for a DIFFERENT app:
stolen = dict(id_token, aud="some-other-app")
print(authenticate_from_id_token(stolen))     # REJECTED — wrong audience
```

**Now break it.** Delete the `aud` check from `authenticate_from_id_token` and re-run. The `stolen` token now "authenticates" — you just accepted a token minted for someone else's app. The `aud` claim, present and checked, is precisely what stops token substitution. (Cortex's server checks exactly this; Chapter 19.)

## 6. Trade-offs & Complexity

| | Plain OAuth for login (don't) | OpenID Connect (do) |
|---|---|---|
| Identity source | Bare access token (not for you) | ID token (`aud` = your client) |
| Interoperability | Provider-specific guessing | One standard, discovery-driven |
| Substitution safety | Weak | Strong (verify `iss` + `aud`) |
| Extra cost | "Less code" (and a vuln) | One scope + verifying a JWT |

OIDC's cost is tiny — request the `openid` scope and verify one more JWT — and the payoff is interoperability plus a closed security hole. There's almost no reason to do login with raw OAuth anymore.

## 7. Edge Cases & Failure Modes

- **Authenticating from the access token.** The original sin. Use the ID token for *who*, the access token for *what*.
- **Skipping `aud`/`iss` verification.** An ID token you don't check the audience and issuer on is barely better than no token. Always verify both.
- **Confusing the two tokens' audiences.** Access token `aud` ≈ the API; ID token `aud` = your client. Keycloak's nuance (it often puts the client in `azp` for public SPAs) is exactly what Cortex's server handles — Chapter 19.
- **Treating UserInfo as authentication.** UserInfo *enriches* an already-authenticated session; it isn't the proof of login itself.

## 8. Practice

> **Exercise 1 — Two tokens, two readers.** State which token (access or ID) each party should read, and for what: (a) the Cortex API deciding whether to run code; (b) the Cortex SPA deciding what name to show in the header.

<details>
<summary><strong>Answer</strong></summary>

The split follows the rule from §3: **access token = *what* you may do; ID token = *who* logged in.**

- **(a) The Cortex API → the access token.** The API is the Resource Server. It's deciding whether the *bearer* may perform an action (run code), so it reads the token that was minted *for an API* — checking the bearer is allowed, not "who is this person." It must *not* trust a bare access token as proof of identity (that's the original sin of §1), and it never reads the ID token, which isn't addressed to it.
- **(b) The Cortex SPA → the ID token.** The SPA is the *client* — the very party the ID token's `aud` names. It reads identity claims like `preferred_username` (or `name`) to render the header. The ID token is *about the user* and *for your app*, exactly the job here.

The deeper point: the same login produces two tokens precisely because "may you act?" and "who are you?" are different questions with different audiences. Reading the wrong one for the wrong job is how the substitution hole opens.

</details>

> **Exercise 2 — Fetch a discovery doc.** Open `https://accounts.google.com/.well-known/openid-configuration` in your browser. Find the `authorization_endpoint`, `token_endpoint`, and `jwks_uri`. What does publishing these enable?

<details>
<summary><strong>Answer</strong></summary>

You'll find three URLs under Google's domain: `authorization_endpoint` (where the user is sent to log in and consent), `token_endpoint` (where the client exchanges the authorization code for tokens), and `jwks_uri` (where the *public keys* live, so a client can verify token signatures — the star of Chapter 11).

Publishing them at a single, standard `.well-known/openid-configuration` URL is what makes OIDC clients **interchangeable**. A library like `keycloak-js` is handed *one* URL, fetches this JSON on startup, and configures *every* endpoint automatically — nothing is hard-coded. That's why pointing the same client code at Google, Keycloak, or any compliant provider "just works": each one self-describes its entire identity surface, so the client learns where to send the user, where to swap the code, and where to fetch keys without you wiring it up by hand.

</details>

> **Exercise 3 — Explain the hole.** In three sentences, explain to a teammate why "I have a valid access token, so I know who the user is" is unsafe, and what OIDC adds to fix it.

<details>
<summary><strong>Answer</strong></summary>

An access token is an opaque "bearer of this may access X" minted *for a Resource Server*, not *for your app* — so the fact that it's valid says nothing verifiable about *who* the user is, and crucially it carries no proof it was issued *to you*. That gap is the **token-substitution / confused-deputy** attack: an attacker can hand your app a valid token that was minted for a *different* app, and if you read identity out of it, you impersonate whoever that token belongs to. OIDC fixes it by adding an **ID token** — a signed JWT *about the user* whose **`aud` claim names your client** — so you authenticate from a token you can verify was made *for you* and *about the user*, closing the hole.

</details>

```quiz
{
  "prompt": "When your app needs to know WHO the authenticated user is, which token should it read — and what claim proves the token was minted for your app?",
  "input": "Choose one:",
  "options": [
    "The ID token; the `aud` (audience) claim",
    "The access token; the `scope` claim",
    "The refresh token; the `exp` claim",
    "The access token; the `iss` claim"
  ],
  "answer": "The ID token; the `aud` (audience) claim"
}
```

## In the Wild

- **[OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)** — the spec. §2 (the ID token) and §3.1 (the auth-code flow with OIDC) are the parts to read.
- **[OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)** — the `.well-known/openid-configuration` document that makes clients interchangeable.
- **[Cortex's discovery doc](https://keycloak.kakde.eu/realms/apps-prod/.well-known/openid-configuration)** — the real configuration `keycloak-js` reads when you load this site (if the realm is reachable).

---

**Next:** the ID token is a JWT. It's time to stop treating JWTs as magic strings and crack one open — header, payload, signature — and read every claim with our own eyes. → [10. Anatomy of a JWT, decoded by hand](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/anatomy-of-a-jwt)
