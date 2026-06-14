---
title: '4. The four actors and the valet key'
summary: OAuth 2.0 is a play with exactly four characters. Learn who they are and what each wants, and the whole protocol stops being an alphabet soup of endpoints and becomes an obvious conversation.
---

# 4. The four actors and the valet key

## TL;DR

> OAuth 2.0 has **four roles**: the **Resource Owner** (you), the **Client** (the app that wants to act for you), the **Authorization Server** (the trusted party that issues tokens — Keycloak), and the **Resource Server** (the API holding the goods). Every OAuth flow is just these four passing messages so that the Client ends up holding a **scoped token** instead of your password. Learn the cast and the plot writes itself.

## 1. Motivation

People bounce off OAuth because they meet it as a pile of endpoints — `/authorize`, `/token`, `/userinfo` — and a fog of parameters — `response_type`, `grant_type`, `client_id`, `redirect_uri`. It feels arbitrary.

It isn't. OAuth is a *negotiation between four parties who don't fully trust each other*, conducted over a network where messages can be intercepted. Every endpoint and parameter exists to answer one question: *"how do these four safely cooperate so the app gets a limited key, the user keeps their password, and an eavesdropper gets nothing?"*

So before any endpoint, meet the cast. Once you can name who's talking and what they want, the protocol becomes a story you could almost re-derive yourself.

## 2. Intuition (Analogy)

Back to the hotel valet from the last chapter — now with all four characters on stage.

```d2
direction: right

owner: "Resource Owner\n(You — the car's owner)" {
  shape: person
}
client: "Client\n(The valet)" {
  shape: rectangle
}
authz: "Authorization Server\n(Hotel key desk)" {
  shape: hexagon
}
resource: "Resource Server\n(Your car)" {
  shape: rectangle
}

owner -> authz: "1. I authorize the valet" {style.stroke-dash: 0}
authz -> client: "2. here's a valet key (token)"
client -> resource: "3. drive with the valet key"
```

- **You** own the car. Nobody may touch it without your say-so. You are the **Resource Owner**.
- **The valet** wants to do something *for* you (park the car). They are the **Client**. Crucially, *you don't fully trust the valet* — that's why they get a valet key, not the master key.
- **The hotel's key desk** is the only party trusted to mint keys. It checks that *you* approved this, then issues a limited valet key. It is the **Authorization Server**.
- **The car** enforces what the valet key allows — it starts, but the glovebox stays locked. The car is the **Resource Server**.

The genius of the arrangement: *the valet and the car never have to trust each other directly.* They both trust the **key desk**. That indirection — funnel all trust through one authority that issues verifiable keys — is the load-bearing idea of OAuth, and of Keycloak.

## 3. Formal Definition

Here are the four roles, by their official names (RFC 6749 §1.1), each mapped to Cortex.

| Role | Who it is | What it wants | In Cortex |
|---|---|---|---|
| **Resource Owner** | The human who owns the data/capability. | To grant *limited* access without handing over their password. | **You**, the signed-in reader. |
| **Client** | The application requesting access on the owner's behalf. | A token it can present to the API. | The **Cortex SPA** (the JavaScript running in your browser). |
| **Authorization Server (AS)** | The trusted party that authenticates the owner and issues tokens. | To verify identity + consent, then mint scoped, signed tokens. | **Keycloak** (`keycloak.kakde.eu`). |
| **Resource Server (RS)** | The API that holds the protected resources. | To serve requests *only* when shown a valid token. | The **Cortex API** (`/api/run`, `/api/submissions`). |

Two supporting concepts you'll meet constantly:

- **Client types.** A **confidential client** can keep a secret (it runs on a server you control — e.g., a backend). A **public client** *cannot* keep a secret (it runs where users can read its code — a browser SPA or a mobile app). Cortex's SPA is a **public client**, and that single fact drives almost every security decision in the next two chapters (it's *why* PKCE is mandatory).
- **Endpoints on the AS.** The **authorization endpoint** (`/authorize`) is where the *user's browser* goes to log in and consent. The **token endpoint** (`/token`) is where the *client* exchanges a code for tokens. Keep them straight: one is for the human, one is for the app.

> **The one-sentence model:** the Client wants to call the Resource Server, but instead of carrying the Resource Owner's password, it carries a token minted by the Authorization Server after the Owner consented. Everything else is detail about *how to do that safely*.

## 4. Worked Example

Map the cast onto a real Cortex action: *running an edited Python snippet, signed in.*

```d2
direction: right

you: "Resource Owner\n(You)" {shape: person}
spa: "Client\nCortex SPA (public)" {shape: rectangle}
kc: "Authorization Server\nKeycloak" {shape: hexagon}
api: "Resource Server\nCortex /api/run" {shape: rectangle}

you -> spa: "click Run"
spa -> kc: "(if needed) get a token for this user"
kc -> spa: "scoped access token (JWT)"
spa -> api: "POST /api/run\nAuthorization: Bearer <token>"
api -> api: "verify token, then execute"
```

- **You** are the Resource Owner: it's *your* per-user rate-limit budget being spent, *your* identity attached to the submission.
- The **SPA** is the Client. It is *public* — anyone can open dev tools and read its code — so it can hold no secret. It must get a token without one.
- **Keycloak** is the Authorization Server. It already authenticated you (via GitHub) and minted the token.
- The **Cortex API** is the Resource Server. It doesn't call Keycloak per request — it verifies the token's signature locally and trusts the identity inside (Chapter 19 reads that exact code).

Every later chapter is "zoom in on one arrow in this picture." Chapter 5 zooms into `spa → kc → spa` (how the token is actually obtained). Chapter 6 explains why that arrow is dangerous for a *public* client and how PKCE saves it. Chapter 19 zooms into `api verify`.

## 5. Build It

Run this. It's the four roles as four functions, wired into one honest little OAuth-shaped exchange — no real crypto, just the *shape* of who-talks-to-whom.

```python run
import secrets

# ---- Authorization Server (Keycloak's job) ----
class AuthorizationServer:
    def __init__(self):
        self._codes = {}    # one-time authorization codes
        self._tokens = {}    # issued tokens -> (user, scopes)

    def authorize(self, resource_owner, client_id, scopes):
        # The owner has consented in their browser. Mint a SHORT-LIVED code.
        code = secrets.token_urlsafe(8)
        self._codes[code] = (resource_owner, client_id, scopes)
        return code

    def exchange(self, code, client_id):
        # The client redeems the code at the token endpoint.
        owner, expected_client, scopes = self._codes.pop(code, (None, None, None))
        if owner is None or client_id != expected_client:
            return None
        token = secrets.token_urlsafe(16)
        self._tokens[token] = (owner, scopes)
        return token

    def verify(self, token):
        return self._tokens.get(token)  # the Resource Server will ask this


# ---- Resource Server (Cortex API's job) ----
class ResourceServer:
    def __init__(self, auth_server):
        self.auth = auth_server

    def run_code(self, token):
        identity = self.auth.verify(token)
        if identity is None:
            return "401 — no valid token"
        user, scopes = identity
        if "code:run" not in scopes:
            return f"403 — {user} lacks scope code:run"
        return f"200 — executed code as {user}"


# ---- The play, in four lines ----
kc  = AuthorizationServer()
api = ResourceServer(kc)

# Resource Owner "ada" consents; Client "cortex-web" gets a code, then a token.
code  = kc.authorize(resource_owner="ada", client_id="cortex-web", scopes={"code:run"})
token = kc.exchange(code, client_id="cortex-web")
print("Client received token:", token[:8], "…")
print(api.run_code(token))                 # 200 — executed as ada
print(api.run_code("a-made-up-token"))     # 401 — forged
```

**Now break it.** In `kc.authorize(...)`, change `scopes={"code:run"}` to `scopes=set()` (no scopes) and re-run. The token is still valid (authentication succeeds) but `run_code` now returns **403** — the Resource Server authorized the *action* separately from accepting the *identity*. That's the authN/authZ split from Chapter 1, now living inside the four-actor model.

## 6. Trade-offs & Complexity

Why four roles instead of two (just "app" and "API")? Because separating them buys you things:

| Benefit of separating the roles | What you'd lose by merging them |
|---|---|
| One Authorization Server secures *many* apps and APIs | Every app re-implements login, badly |
| The API never sees passwords — only tokens | Passwords sprayed across every service |
| Users consent once, to a party they recognize | "Type your password into this random app" |
| Revocation, MFA, audit live in *one* place | Scattered, inconsistent security |

The cost is **indirection**: more moving parts, a multi-step dance, and the need to run (or rent) an Authorization Server. For a one-off toy, that's overkill — you'd just check a password. For anything real, centralizing identity in an AS like Keycloak is the difference between security you can reason about and a sprawl you can't.

## 7. Edge Cases & Failure Modes

- **Confusing the two AS endpoints.** `/authorize` is a *browser redirect* for the human; `/token` is a *back-channel call* by the client. Sending the wrong thing to the wrong one is a classic beginner bug.
- **Treating a public client as confidential.** A browser SPA *cannot* keep a secret. Designs that assume it can (e.g., embedding a `client_secret` in JS) are broken on arrival — the secret is right there in dev tools.
- **The Resource Server trusting the Client directly.** The RS must trust *the token's issuer (the AS)*, not the client that presents it. Anyone can present a token; only a valid signature matters.
- **One AS, but no consent.** Skipping the Resource Owner's consent turns delegated authorization back into "the app just does what it wants." Consent is what keeps *you* in control.

## 8. Practice

> **Exercise 1 — Cast the roles.** A photo-printing website lets you "import photos from Google Photos." Name the four OAuth roles in this scenario. Which one is the public client? Which one holds the photos?

<details>
<summary><strong>Answer</strong></summary>

Map the four roles (§3) onto the scenario — the question to ask for each is *whose data, who wants it, who mints the key, who guards the goods*:

- **Resource Owner — you**, the person whose Google Photos these are. Nobody may touch them without your say-so.
- **Client — the photo-printing website**, the app asking to act on your behalf (to import your photos). It wants a token, not your Google password.
- **Authorization Server — Google's OAuth/account system**, the trusted party that authenticates you and mints a scoped token after you consent.
- **Resource Server — the Google Photos API**, which **holds the photos** and serves them only when shown a valid token.

**Which is the public client?** It depends on the printing site's architecture: if the import is driven by JavaScript running in your browser, *that* is a **public client** (its code is readable, it can keep no secret); if the import is done by the site's **backend**, that backend is a **confidential client** (it can hold a `client_secret` server-side). The role that **holds the photos** is the **Resource Server** (Google Photos API) — never the client.

</details>

> **Exercise 2 — Public or confidential?** Classify each client and say why: (a) a React single-page app; (b) a Node.js backend calling a payments API; (c) an iPhone app; (d) a cron job on your own server. Which ones can keep a secret?

<details>
<summary><strong>Answer</strong></summary>

The one test (§3): **can the client keep a secret from its own users?** That is true only when the code runs somewhere the user can't read it — a server you control. Anything that runs *on the user's device* is a **public** client.

- **(a) React SPA — public.** Its JavaScript is downloaded and runs in the user's browser; anyone can open dev tools and read it. A `client_secret` embedded in it is visible, so it can't keep one. (This is exactly Cortex's SPA — and why PKCE is mandatory.)
- **(b) Node.js backend calling a payments API — confidential.** It runs on a server you control; users never see its code or environment, so it *can* hold a `client_secret`.
- **(c) iPhone app — public.** The app binary is distributed to users' devices and can be inspected/decompiled; a baked-in secret is extractable, so it's public (and uses PKCE).
- **(d) Cron job on your own server — confidential.** Server-side, not user-facing; it can safely store a secret (and would typically use the Client Credentials grant — Chapter 8).

So **(b) and (d) can keep a secret; (a) and (c) cannot.** The dividing line is *where the code runs*, not what language or framework it's written in.

</details>

> **Exercise 3 — Why the indirection?** In two sentences, explain to a teammate why the Resource Server should verify the *token* rather than ask the *Client* "are you allowed?" What attack does that prevent?

<details>
<summary><strong>Answer</strong></summary>

The Resource Server must trust the **token's issuer (the Authorization Server)**, not the client presenting it, because *anyone* can present a token — so the only thing that proves authorization is a **valid, unforgeable signature** the RS can check itself (§7). If the RS instead just *asked the client* "are you allowed?", a malicious client would simply answer "yes," and the RS would have outsourced its security decision to the very party it shouldn't trust.

This prevents **client-asserted-authorization / impersonation**: by anchoring trust in the AS's signature rather than the caller's word, a forged or rogue client can't talk its way past the check — it would need to forge the AS's signature, which it can't. (It's the same principle as the Honan failure from Chapter 1: never let "they claimed it" stand in for "it was proven.")

</details>

```quiz
{
  "prompt": "In Cortex, the JavaScript SPA running in your browser is which OAuth role — and what crucial limitation does that role have?",
  "input": "Choose one:",
  "options": [
    "The Client, and as a public client it cannot keep a secret",
    "The Authorization Server, which mints tokens",
    "The Resource Server, which holds the data",
    "The Resource Owner, which is the human user"
  ],
  "answer": "The Client, and as a public client it cannot keep a secret"
}
```

## In the Wild

- **[RFC 6749 §1.1 — Roles](https://datatracker.ietf.org/doc/html/rfc6749#section-1.1)** — the four roles, in the authors' own words. Short and worth reading once.
- **[oauth.net — OAuth 2.0 roles and terminology](https://oauth.net/2/)** — a friendly hub with diagrams of the same cast.
- **[Cortex realm: the `cortex-web` client](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)** — the real definition of Cortex's public client. `"publicClient": true` is the line that makes the SPA a public client; you'll dissect this file in Chapter 14.

---

**Next:** now we zoom into the most important arrow — how the Client actually obtains a token. We'll step through the **Authorization Code flow** one message at a time, with an interactive diagram you can scrub. → [5. The Authorization Code flow, step by step](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/authorization-code-flow)
