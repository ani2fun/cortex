---
title: '18. Client side: keycloak-js, check-sso & silent refresh'
summary: How Cortex's SPA boots authentication without blocking the page, silently adopts an existing session, attaches the token to API calls, and refreshes it every 30 seconds — reading the real `keycloak-js` integration.
---

# 18. Client side: keycloak-js, check-sso & silent refresh

## TL;DR

> Cortex's SPA uses the official **`keycloak-js`** library, which implements the entire Authorization Code + PKCE flow (Chapters 5–6) for you. On startup it asks the server `/api/auth/config`; if auth is on, it runs `init({ onLoad: "check-sso", pkceMethod: "S256" })` — a **non-blocking** check that *silently adopts an existing session* without redirecting. If you're already logged in, it fetches your identity and arms a **30-second refresh timer**; if not, you stay anonymous until you click a gated action. This is the ADR-0013 gate, in code.

## 1. Motivation

You derived the flow by hand (Groups 2–3) precisely so you'd *trust* a library to run it — not so you'd hand-roll PKCE, redirects, and token refresh yourself (that's how subtle vulnerabilities are born). `keycloak-js` is that library: battle-tested, maintained alongside Keycloak, and aware of every quirk.

But a library still needs to be wired to *your* product decision. Cortex's wiring has to satisfy ADR-0013: the page must **render and run code before auth is ready** (frictionless reading), adopt a session **silently** if one exists, and only ever redirect to Keycloak when the user actually clicks a gated action. That non-blocking, "auth is optional until it isn't" boot is the interesting part, and it's worth reading.

## 2. Intuition (Analogy)

Walking into that library again. The system should **not** stop you at the door demanding a card — you came to read. Instead, a quiet sensor checks, *as you walk in*, whether you're already holding a valid card from a previous visit (you scanned it last week and it's still in your pocket). If so, the makerspace lights up as available — no interaction. If not, nothing happens; you browse freely, and you're only asked for a card the moment you reach for the 3D printer.

`onLoad: "check-sso"` is that quiet sensor: it checks for an existing session **without** redirecting you anywhere. `signIn()` (the redirect to Keycloak) only fires when you click **Edit** or **Sign in** — reaching for the printer.

## 3. Formal Definition — the boot states

Cortex models auth as a small state machine in `AuthStore`:

- **Disabled** — `/api/auth/config` said `enabled: false` (dev). The SPA never even loads `keycloak-js`; everything is open.
- **Anonymous** — auth is on, but no session was adopted. Reading and running work; gated actions prompt sign-in.
- **Authed(user, token)** — a session exists; the SPA holds the access token and the user's identity (from `/api/me`), and a refresh timer is running.

The transitions, from [`AuthBoot.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/AuthBoot.scala):

> 1. `GET /api/auth/config`. `enabled = false` → **Disabled**, stop. 2. Otherwise construct `keycloak-js` and `init({ onLoad: "check-sso" })`: session adopted → `GET /api/me`, publish **Authed**, start the refresh timer; no session → **Anonymous**. 3. Every 30s, `updateToken(60)`; a refresh failure means the session lapsed → back to **Anonymous**.

## 4. Worked Example — the real boot code

**Step 1 — the PKCE-enabled init options** ([`KeycloakClient.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/KeycloakClient.scala)). Note `pkceMethod = "S256"` — the library does all of Chapter 6 for you:

```scala
def checkSso: KeycloakInitOptions =
  js.Dynamic.literal(
    onLoad                    = "check-sso",   // adopt an existing session WITHOUT redirecting
    pkceMethod                = "S256",        // PKCE, handled by the library (Chapter 6)
    checkLoginIframe          = false,
    silentCheckSsoRedirectUri = dom.window.location.origin + "/" + AppRoutes.SilentCheckSso,
    silentCheckSsoFallback    = false
  ).asInstanceOf[KeycloakInitOptions]
```

**Step 2 — boot, non-blockingly** ([`AuthBoot.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/AuthBoot.scala)). The Keycloak *base* URL is derived from the realm issuer:

```scala
val baseUrl = issuerUrl.stripSuffix(s"/realms/$realm")  // keycloak-js wants the base, not the realm URL
val kc      = new Keycloak(KeycloakConfig(baseUrl, realm, clientId))
AuthStore.registerKeycloak(kc)

kc.init(KeycloakInitOptions.checkSso).toFuture.onComplete {
  case Success(true)  => onAuthenticated(kc)                          // a session was adopted
  case Success(false) => AuthStore.setStatus(AuthStore.Status.Anonymous)
  case Failure(err)   =>                                              // never block the page on auth
    dom.console.warn(s"auth: keycloak init failed (${err.getMessage}); treating as anonymous")
    AuthStore.setStatus(AuthStore.Status.Anonymous)
}
```

**Step 3 — on success, fetch identity and arm the refresh timer:**

```scala
private def onAuthenticated(kc: Keycloak): Unit =
  val token = kc.token.toOption.getOrElse("")
  ApiClient.getMe(token).onComplete {                  // GET /api/me with the bearer token
    case Success(user) =>
      AuthStore.setStatus(AuthStore.Status.Authed(user, token))
      startRefreshTimer(kc)                            // every 30s: updateToken(60)
    case Failure(_) => AuthStore.setStatus(AuthStore.Status.Anonymous)
  }
```

**Step 4 — sign-in is a redirect, fired only on a gated action** ([`AuthStore.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/AuthStore.scala)):

```scala
def signIn(): Unit =
  keycloak.foreach { kc =>
    kc.login(KeycloakLoginOptions(redirectUri = dom.window.location.href, idpHint = "github"))
    // idpHint = "github" sends the user straight to GitHub (Chapter 15).
  }
```

**Step 5 — attach the token to API calls** ([`ApiClient.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/api/ApiClient.scala)). The bearer header rides along *only* when signed in:

```scala
def runCode(req: RunRequest): Future[RunResponse] =
  val base = runRequestFn(req)
  val request = AuthStore.current.status match
    case AuthStore.Status.Authed(_, token) => base.header("Authorization", s"Bearer $token")
    case _                                  => base   // anonymous: no header → per-IP metering
  backend.send(request)...
```

Read those five steps against ADR-0013: the page boots and runs code regardless of auth (`check-sso`, never blocking); a token is attached *if present* (optional auth); login is a redirect fired only when you reach for a gated action; and the session stays alive via a 30-second refresh. The whole gate, in five short pieces of real code.

## 5. Build It

Run this. It simulates the boot state machine and the refresh loop so you can watch the transitions ADR-0013 requires.

```python run
import time

class AuthStore:
    def __init__(self): self.status = "booting"
    def set(self, s): self.status = s; print(f"  -> status = {s}")

def boot(store, auth_enabled, has_existing_session):
    print("GET /api/auth/config")
    if not auth_enabled:
        return store.set("Disabled (dev) — everything open")
    print("init({ onLoad: 'check-sso', pkceMethod: 'S256' })  # non-blocking")
    if has_existing_session:
        print("GET /api/me")
        store.set("Authed(user, token) — refresh timer armed")
    else:
        store.set("Anonymous — page still works, prompt on gated action")

def attach_auth_header(store):
    if store.status.startswith("Authed"):
        return "Authorization: Bearer <token>   (metered per-user)"
    return "(no header)                        (metered per-IP)"

print("== visitor with no session =="); s = AuthStore(); boot(s, True, False)
print("   /api/run header:", attach_auth_header(s))
print("\n== returning user (session in cookie) =="); s2 = AuthStore(); boot(s2, True, True)
print("   /api/run header:", attach_auth_header(s2))
print("\n== local dev (AUTH_ENABLED=false) =="); s3 = AuthStore(); boot(s3, False, False)
```

**Now break it.** Change the no-session boot to `store.set("blocked — redirect to login")` instead of `Anonymous`. That would turn `check-sso` into a login wall — every visitor bounced to Keycloak before they can read. The single word "Anonymous" is what keeps Cortex frictionless; the library's `check-sso` mode is what makes "Anonymous" possible without a redirect.

## 6. Trade-offs & Complexity

| `check-sso` (Cortex) | `login-required` |
|---|---|
| Page renders before/without auth | Page blocked until login |
| Silent session adoption | Always a redirect |
| Frictionless reading (ADR-0013) | Hostile to casual visitors |
| Must handle "auth not ready yet" | Simpler state model |

`keycloak-js` offers both `onLoad` modes; Cortex picks `check-sso` because the ADR demands non-blocking reading. The cost is a slightly richer state machine (Booting → Anonymous/Authed/Disabled) and code that treats "auth still booting" as anonymous. Worth it.

## 7. Edge Cases & Failure Modes

- **Init fails (Keycloak down).** Cortex catches it and falls back to **Anonymous** — the page must never hard-fail because the IdP hiccuped. Reading and canonical runs keep working.
- **Refresh fails (session lapsed/revoked).** The 30-second timer's failure path sets **Anonymous** and clears the timer — the user silently drops to anonymous, and the next gated action re-prompts.
- **Token in JS memory.** `keycloak-js` keeps the access token in memory (not `localStorage`), reducing XSS exfiltration risk. Don't copy it into `localStorage`.
- **Base URL vs realm URL.** `keycloak-js` wants the Keycloak *base* URL; the server reports the *realm* issuer. Cortex strips `/realms/$realm` — get this wrong and init silently fails.

## 8. Practice

> **Exercise 1 — Why non-blocking?** In two sentences, connect `onLoad: "check-sso"` to ADR-0013. What product property would `login-required` destroy?

<details>
<summary><strong>Answer</strong></summary>

ADR-0013 requires that the page **render and run canonical code before — or entirely without — login**, and `onLoad: "check-sso"` is exactly the mode that delivers that: it *silently checks* for an existing session **without redirecting**, so a visitor with no session simply lands in the **Anonymous** state and keeps reading and running, while a returning visitor's session is adopted with zero interaction. `login-required` would destroy that **frictionless reading** property — it bounces *every* visitor to Keycloak before the page is usable, turning the open tutorial into a login wall and defeating the entire reason the gate is narrow.

The deeper point: `check-sso` is what makes "Anonymous" reachable *without a redirect*. Without it there is no non-blocking path, and ADR-0013's first decision (anonymous run) couldn't be honored on the client.

</details>

> **Exercise 2 — Trace a returning user.** List the network calls (config, init, me) a *returning, logged-in* user's browser makes on page load, and the resulting `AuthStore` status.

<details>
<summary><strong>Answer</strong></summary>

Walk the boot state machine from §3–§4 for a user who already has a valid session cookie at Keycloak:

1. **`GET /api/auth/config`** — the SPA asks its own server whether auth is on and for `{ issuerUrl, realm, clientId }`. Comes back `enabled: true`, so the boot continues (it does *not* stop at **Disabled**).
2. **`init({ onLoad: "check-sso", pkceMethod: "S256" })`** — `keycloak-js` runs the silent check against Keycloak's authorize endpoint via the hidden `silentCheckSsoRedirectUri` iframe/redirect. Because a session cookie exists, this resolves to **`Success(true)`** — a session is adopted and an access token is now in memory. (No visible redirect; the user sees nothing.)
3. **`GET /api/me`** with `Authorization: Bearer <token>` — exchanges the token for the displayable identity (`preferredUsername`, name, email).

Resulting status: **`Authed(user, token)`**, and the **30-second `updateToken(60)` refresh timer is armed**. (Contrast a visitor with no session: same calls 1 and 2, but step 2 resolves `Success(false)`, there is no `/api/me`, and the status is **Anonymous**.)

</details>

> **Exercise 3 — Follow the token.** Using the `runCode` snippet, explain what header an *anonymous* run sends versus a *signed-in* run, and how that choice reaches the rate limiter (preview of Chapter 20).

<details>
<summary><strong>Answer</strong></summary>

The `runCode` snippet branches on `AuthStore.current.status`:

- **Anonymous** (status is *not* `Authed`) → the `case _` arm sends the bare request with **no `Authorization` header**.
- **Signed in** (`Authed(_, token)`) → it adds **`Authorization: Bearer <token>`**.

That one header decision is what the server's rate limiter reads. On the server, `auth.verifyOptional(bearer)` turns *no header* into **`None`** (anonymous) and a *valid token* into **`Some(claims)`**; that fork picks the bucket key — `ip:<addr>` for `None`, `user:<sub>` for `Some` (Chapter 20). So the client's "attach the header iff signed in" rule directly selects whether the run spends from the shared per-IP budget or the caller's private per-user budget.

The key insight: the per-IP-vs-per-user *behavior* isn't configured anywhere central — it **emerges** from this client header choice meeting the server's `verifyOptional` fork. If the client forgot the header while signed in, the user would silently drop into the per-IP bucket. Client and server must agree.

</details>

```quiz
{
  "prompt": "What does `keycloak-js` `init({ onLoad: 'check-sso' })` do that makes Cortex's frictionless reading possible?",
  "input": "Choose one:",
  "options": [
    "Silently checks for an existing session WITHOUT redirecting, so the page renders and runs code whether or not you're logged in",
    "Forces every visitor to log in before the page loads",
    "Stores the user's password in localStorage",
    "Verifies the token's signature on the client"
  ],
  "answer": "Silently checks for an existing session WITHOUT redirecting, so the page renders and runs code whether or not you're logged in"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[keycloak-js — JavaScript adapter docs](https://www.keycloak.org/securing-apps/javascript-adapter)** — `init` options, `check-sso`, `updateToken`, and the silent-SSO redirect page.
- **[Cortex `AuthBoot.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/AuthBoot.scala)** & **[`AuthStore.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/auth/AuthStore.scala)** — the real boot sequence and state machine excerpted above.
- **[OAuth 2.0 for Browser-Based Apps — RFC 9700 / BCP](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps)** — current best practice for SPAs, which `keycloak-js` follows.

---

**Next:** the SPA sends `Authorization: Bearer <token>`. Now follow that token into the server and watch Cortex verify it end-to-end — the real ZIO + Nimbus code behind Chapter 11. → [19. Server side: verifying a bearer token end-to-end](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/server-side-token-verification)
