---
title: '8. The other grants — and the two you should never use'
summary: Not every client is a browser with a user to redirect. Meet client-credentials and device-code for the cases Authorization Code can't cover — and the two legacy grants that are traps.
---

# 8. The other grants — and the two you should never use

## TL;DR

> OAuth has several **grant types** — recipes for getting a token — because clients differ. **Authorization Code + PKCE** is the default for anything with a user and a browser (it's what Cortex uses). **Client Credentials** is for machine-to-machine calls with *no user*. **Device Authorization** is for input-constrained gadgets (TVs, CLIs). Two legacy grants — **Implicit** and **Resource Owner Password Credentials (ROPC)** — are deprecated traps: avoid them. Choosing the grant is mostly a flowchart, and this chapter is that flowchart.

## 1. Motivation

The Authorization Code flow assumes two things: there's a **human** to authenticate, and there's a **browser** to redirect. Plenty of real clients break one of those assumptions. A nightly billing job calling an internal API has no human. A smart TV has a human but a terrible keyboard and no real browser. A backend service talking to another backend has neither a user nor a redirect.

OAuth's answer is to offer different *grants* for different shapes of client. Picking the right one is a security decision — reach for the wrong grant and you reintroduce the very problems Chapters 2–6 solved.

## 2. Intuition (Analogy)

Think of grants as **different ways to get a key cut**, depending on who's asking:

- **You, in person, with ID** → the AS authenticates you and the app gets a key. (*Authorization Code + PKCE.*)
- **A trusted contractor with their own company badge**, picking up a key for a job that doesn't involve any tenant → the building manager checks the *company's* credentials, not a tenant's. (*Client Credentials* — the app authenticates as *itself*.)
- **A vending machine that can't read your ID**, so it prints a code and says "type this code at the front desk on your phone, then I'll get the key." (*Device Authorization.*)
- **Handing a stranger your house key to copy** → never do this. (*ROPC* — the app takes your actual password.)

## 3. Formal Definition — the grants

| Grant | Who authenticates | Use it for | Status |
|---|---|---|---|
| **Authorization Code + PKCE** | The **user**, at the AS, via browser redirect | SPAs, mobile, server-rendered web — anything with a user | ✅ **Default** |
| **Client Credentials** | The **client itself** (with its own secret) — *no user* | Machine-to-machine: a backend calling an API as itself | ✅ Correct for M2M |
| **Device Authorization** | The **user**, on a *second* device | TVs, consoles, CLIs — input-constrained or browserless | ✅ Correct for devices |
| **Refresh Token** | (none — renews an existing grant) | Silently getting new access tokens | ✅ (Chapter 7) |
| **Implicit** | The user, but tokens return *in the URL* | — | ⛔ **Deprecated** |
| **Resource Owner Password Credentials (ROPC)** | The app collects the user's **password** | — | ⛔ **Deprecated** |

**Why the two bans:**

- **Implicit** returned the access token directly in the redirect URL fragment (`#access_token=…`). That puts tokens in browser history, server logs, and `Referer` headers — leakable. Authorization Code + PKCE gives a browser app the same capability *safely*, so Implicit is retired (RFC 9700).
- **ROPC** has the app collect your username and password and send them to the AS. That is the **password anti-pattern from Chapter 2 wearing an OAuth badge** — the app sees your password, defeating the entire reason OAuth exists. Banned for anything but narrow legacy migration.

## 4. Worked Example — pick the grant

```d2
direction: down

start: "What kind of client are you?" {shape: diamond}

user_q: "Is there a human user\nto authenticate?" {shape: diamond}
browser_q: "Can it open a\nfull web browser?" {shape: diamond}

ac: "Authorization Code + PKCE\n(Cortex's choice)" {shape: rectangle; style.fill: "#dcfce7"}
device: "Device Authorization\n(TV, CLI, console)" {shape: rectangle; style.fill: "#dcfce7"}
cc: "Client Credentials\n(machine-to-machine)" {shape: rectangle; style.fill: "#dcfce7"}

start -> user_q
user_q -> browser_q: "yes, a user"
user_q -> cc: "no — just a service"
browser_q -> ac: "yes"
browser_q -> device: "no / constrained input"

implicit: "Implicit ⛔" {shape: rectangle; style.fill: "#fee2e2"}
ropc: "ROPC ⛔\n(app takes your password)" {shape: rectangle; style.fill: "#fee2e2"}
ac -> implicit: "the old way — don't" {style.stroke-dash: 3}
ac -> ropc: "never" {style.stroke-dash: 3}
```

Cortex is the top-left leaf: a human user, a real browser → **Authorization Code + PKCE**. If Cortex later grew a backend cron job that called its own API without a user — say, to pre-warm caches — *that* component would use **Client Credentials**, authenticating as itself with a confidential client secret stored server-side.

## 5. Build It

Run this little router — it encodes the flowchart and refuses the traps.

```python run
def choose_grant(has_user: bool, has_browser: bool, is_machine: bool) -> str:
    if is_machine and not has_user:
        return "client_credentials  (authenticate AS the app; no user)"
    if has_user and has_browser:
        return "authorization_code + PKCE  (the default — Cortex)"
    if has_user and not has_browser:
        return "device_authorization  (code on a second screen)"
    return "no standard grant fits — reconsider the client design"

cases = [
    ("Cortex SPA",            dict(has_user=True,  has_browser=True,  is_machine=False)),
    ("Nightly billing job",   dict(has_user=False, has_browser=False, is_machine=True)),
    ("Smart TV app",          dict(has_user=True,  has_browser=False, is_machine=False)),
]
for name, kw in cases:
    print(f"{name:22s} -> {choose_grant(**kw)}")

# The traps are not even options. If you find yourself wanting to pass a
# user's password to the token endpoint (ROPC), STOP — that's Chapter 2.
def ropc_is_a_trap(username, password):
    raise RuntimeError(
        "ROPC means the app handles the user's password — the anti-pattern OAuth exists to kill."
    )
try:
    ropc_is_a_trap("ada", "lovelace")
except RuntimeError as e:
    print("\nROPC:", e)
```

**Now break it.** Add a case for a CLI tool you want users to log into (e.g., `gh` or `kubectl oidc-login`): `has_user=True, has_browser=False`. The router picks **device authorization** — which is exactly what those real tools do: they print `enter code WXYZ-1234 at https://…` and you approve on your phone.

## 6. Trade-offs & Complexity

- **Client Credentials** is simple and correct for M2M — but it authenticates the *app*, not a user, so there's no user identity in the token. Don't use it to stand in for a user.
- **Device Authorization** adds a polling step (the device asks "approved yet?") and a second screen, but it's the only sane option for browserless gadgets.
- **The bans aren't pedantry.** Implicit and ROPC each re-open a hole earlier chapters closed. The cost of "just using the simple old grant" is paid in breaches.

## 7. Edge Cases & Failure Modes

- **Using Client Credentials for user actions.** If a token has no user identity, the Resource Server can't do per-user authorization or auditing. M2M tokens are for *system* actions.
- **ROPC "just for our own first-party app."** Tempting, still wrong: it trains users to type their password into apps and bypasses MFA, federation, and consent. Migrate to Authorization Code.
- **Implicit in old tutorials.** Huge amounts of pre-2020 OAuth content teach Implicit. If you see `response_type=token`, it's outdated.
- **Device flow without rate-limited polling.** The device must respect the `interval` and back off, or it hammers the token endpoint.

## 8. Practice

> **Exercise 1 — Match them up.** Assign a grant to each: (a) a Grafana dashboard logging users in; (b) a payments microservice calling a ledger service; (c) a Raspberry-Pi kiosk with no keyboard; (d) the `kubectl` CLI.

<details>
<summary><strong>Answer</strong></summary>

Run each through the §4 flowchart — *is there a user? can it open a browser?*

- **(a) Grafana dashboard logging users in — Authorization Code + PKCE.** A human user and a real browser → the default grant; Grafana redirects you to the AS to log in.
- **(b) Payments microservice calling a ledger service — Client Credentials.** No user at all, just a backend acting *as itself*; it authenticates with its own (confidential) secret. (Never use a user grant to stand in for a service.)
- **(c) Raspberry-Pi kiosk with no keyboard — Device Authorization.** There's a human, but no usable keyboard/browser → it shows "enter code WXYZ-1234 at https://…" and you approve on your phone (a second device).
- **(d) `kubectl` CLI — Device Authorization.** A user but no browser in the terminal → the device/CLI flow (`kubectl oidc-login` does exactly this: prints a code/URL, you approve on a second screen).

The pattern: *user + browser* → Authorization Code + PKCE; *user, no browser* → Device; *no user* → Client Credentials.

</details>

> **Exercise 2 — Why is ROPC banned?** In two sentences, connect ROPC back to the password anti-pattern of Chapter 2. What three protections (consent, MFA, federation) does it bypass?

<details>
<summary><strong>Answer</strong></summary>

ROPC has the **app collect your username and password** and send them to the token endpoint — which is the **password anti-pattern of Chapter 2 wearing an OAuth badge**: the app sees (and could store or log) your actual credential, defeating the entire reason OAuth exists, which was to hand out scoped, revocable tokens *without* exposing your password. Because the password goes straight to the app and on to the AS, ROPC structurally **bypasses three protections** the redirect-based Authorization Code flow gives you:

- **Consent** — there's no AS-rendered screen telling you *who* wants *what* scopes; the app just takes blanket access.
- **MFA** — a second factor lives in the AS's interactive login, which ROPC skips entirely (it's just username + password), so multi-factor can't be enforced.
- **Federation** — "Log in with GitHub/Google" and other identity brokering happen at the AS's login page, which ROPC never visits, so federated/social login is impossible.

That's why it's banned for anything beyond narrow legacy migration: it re-opens the exact hole — app-handles-your-password — that the whole protocol was built to close, and strips the protections that the proper flow's redirect makes possible.

</details>

> **Exercise 3 — Design Cortex's cron.** Suppose Cortex adds a nightly job that calls `/api` to recompute statistics, with no user involved. Which grant, which client type (public/confidential), and where does its secret live?

<details>
<summary><strong>Answer</strong></summary>

- **Grant — Client Credentials.** There is *no user* to authenticate, just a backend acting as itself, which is precisely what Client Credentials is for (§3): the cron job authenticates *as the application* and gets a token with no user identity in it.
- **Client type — confidential.** The job runs on a server Cortex controls, not on any user's device, so it *can* keep a secret — making it a confidential client (the opposite of the public SPA from Chapter 4).
- **Where the secret lives — server-side only, never in any user-facing code.** Its `client_secret` (or, better, a private key for private-key-JWT client authentication) lives in the server's environment/secret store — e.g. an env var injected at deploy, a mounted secret, or a vault — readable only by the job process. It must *never* appear in the SPA, the repo, logs, or anything shipped to a browser, because that's exactly what would turn a confidential client back into an exposed one.

One caveat from §7: the resulting token has **no user identity**, so it's only valid for *system* actions (recomputing global stats) — the API can't and shouldn't do per-user authorization or auditing with it. A separate Keycloak client (its own `client_id`/secret, scoped to just the stats action) is the clean way to register this.

</details>

```quiz
{
  "prompt": "A backend service needs to call another internal API on its own behalf, with no user involved. Which grant is correct?",
  "input": "Choose one:",
  "options": [
    "Client Credentials — the service authenticates as itself",
    "Authorization Code + PKCE",
    "Resource Owner Password Credentials (ROPC)",
    "Implicit"
  ],
  "answer": "Client Credentials — the service authenticates as itself"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[oauth.net — Grant types](https://oauth.net/2/grant-types/)** — a clean index of every grant with one-line "use this when" guidance.
- **[RFC 8628 — OAuth 2.0 Device Authorization Grant](https://datatracker.ietf.org/doc/html/rfc8628)** — the spec behind every "enter this code on your phone" TV login.
- **[RFC 9700 §2.1.2 & §2.4 — why Implicit and ROPC are out](https://datatracker.ietf.org/doc/html/rfc9700)** — the security rationale for the two bans, from the OAuth working group.

---

**Next:** we've handled *authorization* — getting a token to access resources. But how do you use OAuth to learn *who the user is*? That needs a layer built on top: OpenID Connect. → [9. OAuth authorizes; OIDC identifies](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/oauth-authorizes-oidc-identifies)
