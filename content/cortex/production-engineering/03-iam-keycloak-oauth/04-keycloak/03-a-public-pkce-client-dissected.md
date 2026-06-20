---
title: '14. A public PKCE client, dissected'
summary: Open up Cortex's `cortex-web` client and understand every setting — public vs confidential, redirect URIs, web origins, PKCE — and exactly what each one is defending against.
---

# 14. A public PKCE client, dissected

## TL;DR

> A Keycloak **client** is how an app is registered to obtain tokens, and its settings *are* its security posture. Cortex's `cortex-web` is **public** (no secret — it runs in your browser), uses the **standard flow** (Authorization Code), enforces **PKCE S256** (Chapter 6), and pins an **exact allow-list of redirect URIs and web origins**. Every one of those settings closes a specific attack. This chapter reads the real client config and explains each field as the defense it is.

## 1. Motivation

In Chapter 6 you learned *why* a public client needs PKCE. In Chapter 13 you ran the realm that contains `cortex-web`. Now we read that client's settings and connect each one to a threat — because a misconfigured client is the most common way OAuth deployments get breached. A loose `redirectUri`, a client wrongly marked confidential, PKCE left off — each is a real CVE waiting to happen. The good news: get a handful of fields right and the rest of the protocol's guarantees hold.

## 2. Intuition (Analogy)

A client config is like the **rules posted at a building's loading dock**:

- **"Deliveries only to these exact bay numbers"** (redirect URIs) — a truck can't just back up to any door; only registered bays accept the handoff.
- **"Badge type: contractor, no master key"** (public client) — this entrant is known to hold no master key, so the desk treats them accordingly.
- **"Say the passphrase at pickup"** (PKCE) — even with the right bay, you must prove you're the one who scheduled the delivery.
- **"Calls accepted only from these phone numbers"** (web origins / CORS) — the desk ignores requests from numbers it doesn't recognize.

Each rule is boring on its own. Together they're why a stranger can't reroute your package.

## 3. Formal Definition — the fields that matter

| Field | What it controls | The defense |
|---|---|---|
| **`publicClient`** | Whether the client holds a secret. `true` = no secret. | Honest modeling: a browser app *can't* keep a secret, so don't pretend it can. Forces PKCE instead. |
| **`standardFlowEnabled`** | Enables the **Authorization Code** flow (`response_type=code`). | The correct, secure flow for browser apps (Chapter 5). |
| **`pkce.code.challenge.method`** | `S256` requires PKCE with SHA-256. | Defeats authorization-code interception (Chapter 6). |
| **`redirectUris`** | The **exact** allow-list of URLs the code may be returned to. | Stops an attacker redirecting the code to their own site. *No open wildcards.* |
| **`webOrigins`** | Which origins may make CORS calls to Keycloak (e.g. the token endpoint). | Stops malicious origins from scripting token requests. |
| **`post.logout.redirect.uris`** | Where the user may be sent after logout. | Stops open-redirect abuse on logout. |
| **`directAccessGrantsEnabled`** | Enables the password grant (ROPC). | Convenient for *local testing only*; a liability in prod (Chapter 8). |

> **The mantra:** a public client trades the *secret* it can't keep for *PKCE* + an *exact redirect allow-list*. Those two together give a browser app the same safety a confidential client gets from its secret.

## 4. Worked Example — the real `cortex-web` client

From Cortex's [`cortex-realm.json`](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json):

```json
{
  "clientId": "cortex-web",
  "name": "Cortex SPA",
  "description": "Public PKCE client for the Cortex code-edit auth gate (ADR-0013).",
  "enabled": true,
  "protocol": "openid-connect",
  "publicClient": true,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "redirectUris": [
    "http://localhost:5173/*",
    "http://localhost:8080/*"
  ],
  "webOrigins": [
    "http://localhost:5173",
    "http://localhost:8080",
    "+"
  ],
  "attributes": {
    "pkce.code.challenge.method": "S256",
    "post.logout.redirect.uris": "http://localhost:5173/*##http://localhost:8080/*"
  }
}
```

Now the annotations:

- **`publicClient: true`** — the SPA is JavaScript in your browser; its code is readable in dev tools, so it holds *no secret*. This is the single fact that drives everything else.
- **`standardFlowEnabled: true`** — Authorization Code flow. (`implicitFlowEnabled` is absent/false — good; implicit is dead, Chapter 8.)
- **`pkce.code.challenge.method: S256`** — *the* line that makes the public client safe. Without it, `cortex-web` would be the vulnerable variant from Chapter 6's diagram.
- **`redirectUris`** — the dev URLs of the SPA (`:5173` is Vite, `:8080` is the packaged server). The `/*` is a *path* wildcard under exact hosts/ports — **not** an open `*`. Production swaps these for `https://cortex.kakde.eu/*`.
- **`webOrigins`** — CORS allow-list for browser calls to Keycloak. (The `+` means "use the redirect URIs' origins too." Be deliberate with it.)
- **`directAccessGrantsEnabled: true`** — enables the password grant so you can `curl` a token in local testing (Chapter 13's Build It). In production you'd typically turn this **off** — there's no reason for a browser SPA to use ROPC.
- **`post.logout.redirect.uris`** — the `##`-separated allow-list of safe post-logout destinations.

Notice what's *not* here: no `secret`. A public client has none — and that's correct, not a mistake.

## 5. Build It

Run this. It's a miniature of Keycloak's redirect-URI check — the field most often misconfigured into a vulnerability.

```python run
import fnmatch

# cortex-web's allow-list (path wildcard under EXACT host:port)
ALLOWED = ["http://localhost:5173/*", "http://localhost:8080/*"]

def redirect_allowed(uri: str) -> bool:
    return any(fnmatch.fnmatch(uri, pat) for pat in ALLOWED)

tests = [
    "http://localhost:5173/auth/callback",   # legit SPA callback
    "http://localhost:8080/",                 # legit packaged server
    "http://localhost:5173.evil.com/cb",      # look-alike host
    "https://evil.example/steal?x=",          # attacker's site
    "http://localhost:9999/cb",               # wrong port
]
for t in tests:
    print(f"{'ALLOW' if redirect_allowed(t) else 'BLOCK'}  {t}")
```

**Now break it.** Change `ALLOWED` to the dangerous `["http://localhost:5173*"]` (note: no `/` before `*`). Re-run and watch `http://localhost:5173.evil.com/cb` flip to **ALLOW** — the missing slash turned an exact-host rule into a prefix match an attacker can satisfy with a look-alike domain. *This is a real class of OAuth bug.* The lesson: redirect allow-lists must be exact about host **and** port, with wildcards only on the path.

## 6. Trade-offs & Complexity

| Public client (`cortex-web`) | Confidential client (a backend) |
|---|---|
| No secret to leak or rotate | Has a secret — must be stored safely |
| Security from PKCE + exact redirects | Security from the secret (+ optionally PKCE) |
| Right for SPAs, mobile, CLIs | Right for server-side apps and M2M |
| Can't be trusted with privileged scopes | Can hold higher-trust capabilities |

Choosing public vs confidential isn't a preference — it's dictated by *where the code runs*. If users can read it, it's public, full stop. Trying to make a browser app "confidential" by hiding a secret in JS is the most common self-inflicted OAuth wound.

## 7. Edge Cases & Failure Modes

- **Wildcard redirect URIs.** `*` or sloppy prefixes (`http://host*`) let attackers capture codes. Always exact host+port; wildcard only the path, and prefer registering specific callback paths.
- **A "confidential" SPA.** Embedding a `client_secret` in front-end code is the same as publishing it. If it's in the browser, it's public.
- **`directAccessGrants` left on in prod.** ROPC enabled means someone can trade username+password for tokens, bypassing the browser flow, MFA, and federation. Off unless you have a specific legacy reason.
- **Over-broad `webOrigins`.** A `*` (or careless `+`) origin invites cross-origin token requests. Scope it to your real front-end origins.

## 8. Practice

> **Exercise 1 — Read the posture.** From the `cortex-web` JSON, list the three settings that make it a *safe* public client and name the attack each prevents.

<details>
<summary><strong>Answer</strong></summary>

The three settings, each as the defense it is:

- **`pkce.code.challenge.method: S256`** — prevents **authorization-code interception** (Chapter 6). An attacker who steals the returned code can't redeem it without the original `code_verifier`, which never left the real client. *This* is the line that makes a secret-less client safe; without it `cortex-web` is the vulnerable variant.
- **`redirectUris` as an exact host+port allow-list** (`http://localhost:5173/*`, `http://localhost:8080/*`) — prevents **authorization-code redirection / theft**. The code may only be returned to a registered URL, so an attacker can't have it delivered to their own site. The `/*` wildcards only the *path*, never the host or port.
- **`standardFlowEnabled: true`** (Authorization Code flow), with implicit flow *off* — prevents the **token-in-the-URL exposure of the dead implicit flow** (Chapter 8). The code flow keeps tokens out of redirect URLs and browser history.

The mantra from §3: a public client trades the *secret it can't keep* for **PKCE + an exact redirect allow-list** — those two together give a browser app the safety a confidential client gets from its secret. (Bonus correct answers include `webOrigins` scoped to real origins, defending against cross-origin token requests.)

</details>

> **Exercise 2 — Production diff.** Write the `redirectUris` and `webOrigins` you'd use for production (`cortex.kakde.eu`). Why must these change from the localhost values, and what breaks if they don't?

<details>
<summary><strong>Answer</strong></summary>

For production you'd register the real HTTPS origin:

```json
"redirectUris": [
  "https://cortex.kakde.eu/*"
],
"webOrigins": [
  "https://cortex.kakde.eu"
]
```

(and the matching `post.logout.redirect.uris` of `https://cortex.kakde.eu/*`).

**Why they must change:** these lists are *exact* allow-lists of host, scheme, and port — and the production app is served from `https://cortex.kakde.eu`, a completely different origin from `http://localhost:5173`. Keycloak compares the incoming redirect/origin against the registered values *exactly*; the dev values simply don't match the prod origin.

**What breaks if they don't:**

- **Login breaks** — when the SPA at `cortex.kakde.eu` finishes the flow and Keycloak is asked to redirect the code back to `https://cortex.kakde.eu/...`, that URL isn't in `redirectUris`, so Keycloak refuses with an "Invalid redirect URI" error. This refusal is *correct* and is the very defense from §3: it's what stops an attacker registering a rogue callback.
- **Token calls break** — with `cortex.kakde.eu` missing from `webOrigins`, the browser's CORS request to Keycloak's token endpoint is rejected, so even a successful redirect can't be exchanged for tokens.

Crucially, you do **not** keep the `localhost` entries in prod — leaving them registered widens the allow-list to dev origins that should never be honored on the production server.

</details>

> **Exercise 3 — Spot the vuln.** A teammate sets `redirectUris: ["https://app.example.com*"]` (no slash). Construct a redirect URI an attacker could register that this rule wrongly allows. How do you fix it?

<details>
<summary><strong>Answer</strong></summary>

The missing `/` is the whole bug: `https://app.example.com*` is a **prefix match**, not an exact-host rule, so anything *beginning* with that string passes — including a host the attacker controls:

```text
https://app.example.com.evil.com/steal
```

That URL starts with `https://app.example.com`, so the loose pattern says **ALLOW** — and now an attacker registers a look-alike domain (`app.example.com.evil.com`) and has the authorization **code delivered to their server**. (Other variants the prefix wrongly admits: `https://app.example.com@evil.com/cb`, `https://app.example.commerce.evil.com/cb`.) This is a real, well-known class of OAuth bug — see RFC 9700 §2.1 on redirect-URI validation.

**The fix:** be exact about **host *and* port**, and wildcard only the *path* — put the slash back:

```json
"redirectUris": ["https://app.example.com/*"]
```

Now `app.example.com.evil.com` no longer matches, because the rule pins the host before the path wildcard begins. Better still, register the *specific* callback paths (e.g. `https://app.example.com/auth/callback`) rather than any path wildcard at all. The rule from §7: never a bare `*` or a sloppy prefix — exact host+port, path wildcard only.

</details>

```quiz
{
  "prompt": "Why does `cortex-web` have NO client secret — and what replaces it as the client's security?",
  "input": "Choose one:",
  "options": [
    "It is a public client (its code runs in the browser); PKCE (S256) plus an exact redirect-URI allow-list replace the secret",
    "Someone forgot to set the secret",
    "Browsers encrypt the secret automatically",
    "Public clients don't need any security"
  ],
  "answer": "It is a public client (its code runs in the browser); PKCE (S256) plus an exact redirect-URI allow-list replace the secret"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Keycloak — Managing OIDC clients](https://www.keycloak.org/docs/latest/server_admin/#_oidc_clients)** — every client setting, including the PKCE and redirect-URI options shown here.
- **[RFC 9700 §2.1 — redirect URI validation](https://datatracker.ietf.org/doc/html/rfc9700)** — why exact matching matters and how loose matching gets exploited.
- **[`cortex-realm.json` — the `cortex-web` client](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)** — the real config dissected above.

---

**Next:** `tester` is a local account, but real Cortex users never set a password here — they click "Login with GitHub." How does Keycloak broker an external identity provider into your realm? → [15. "Login with GitHub": identity brokering](/cortex/production-engineering/iam-keycloak-oauth/keycloak/login-with-github)
