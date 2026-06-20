---
title: '21. Hardening & failure modes'
summary: A security sweep across the whole design. The attacks an OAuth/OIDC system must withstand, the misconfigurations that quietly defeat it, and exactly which setting or check stops each one.
---

# 21. Hardening & failure modes

## TL;DR

> A correct OAuth/OIDC design can still be breached by one loose setting. This chapter is the consolidated threat list: **redirect-URI attacks**, **token theft via XSS**, **CSRF**, **algorithm confusion**, **key rotation**, **clock skew**, **CORS**, and **logout/session** handling — each paired with the specific defense (and where Cortex implements it). Treat it as a checklist you run before shipping any identity integration.

## 1. Motivation

Everything so far has been "how it works." This chapter is "how it *fails*, and how to stop it." Security is asymmetric: you must close *every* hole; an attacker needs *one*. The protocol gives you strong guarantees, but only if the surrounding configuration holds — and the gaps are almost always boring misconfigurations, not exotic cryptography breaks. So we enumerate them, map each to a defense you've already met, and turn the scattered "Failure Modes" sections of earlier chapters into one runnable checklist.

## 2. Intuition (Analogy)

A bank vault with a flawless lock is irrelevant if the back door is propped open, the night guard accepts any uniform, the spare key is under the mat, and the alarm's clock is wrong so it never arms. Real breaches are almost never "they cracked the vault." They're "they walked in through something nobody checked." Hardening is walking the perimeter and trying every door yourself, before someone else does.

## 3. The threat catalog

| # | Attack | What it does | The defense | Where in Cortex |
|---|---|---|---|---|
| 1 | **Loose `redirect_uri`** | Code/token redirected to attacker's site | **Exact** host+port allow-list; wildcard only on path | `cortex-web.redirectUris` (Ch 14) |
| 2 | **Authorization-code interception** | Public-client code stolen and redeemed | **PKCE S256** | `pkce.code.challenge.method` (Ch 6) |
| 3 | **CSRF on the redirect** | Attacker completes *their* login in your client | **`state`** nonce, generated + checked | handled by `keycloak-js` (Ch 5) |
| 4 | **Token theft via XSS** | Script exfiltrates the access token | No XSS (CSP, escaping); token in **memory**, not `localStorage` | `keycloak-js` keeps token in memory (Ch 18) |
| 5 | **`alg: none` / RS256→HS256** | Forged or self-signed token accepted | **Pin RS256** server-side; never trust the token's `alg` | `JWSVerificationKeySelector(RS256, …)` (Ch 19) |
| 6 | **Wrong issuer / audience** | A token from elsewhere accepted | Verify **`iss`** exactly; **`aud` or `azp`** = us | `ClaimsVerifier` (Ch 19) |
| 7 | **Expired-token replay** | Old token reused | Check **`exp`** (+ small skew); short lifetimes | `ClaimsVerifier` + short TTL (Ch 7, 19) |
| 8 | **Key-rotation breakage** | Valid new-key tokens rejected | Cache JWKS with TTL **and** refetch on unseen `kid` | JWKS source, `jwksCacheTtlSec` (Ch 11) |
| 9 | **Open CORS** | Malicious origin scripts token calls | Exact **`webOrigins`** | `cortex-web.webOrigins` (Ch 14) |
| 10 | **Open-redirect on logout** | Logout bounces user to attacker site | **`post.logout.redirect.uris`** allow-list | client attributes (Ch 14) |
| 11 | **No TLS** | Tokens/codes sniffed in transit | **HTTPS everywhere**; `sslRequired` in prod | prod realm + ingress (Part 4) |
| 12 | **Refresh-token theft** | Long-lived access regained | **Rotation** + reuse detection; guard storage | Keycloak refresh settings (Ch 7) |

Every row is a hole that has breached real systems — and every defense is something you've already met. Hardening is mostly *making sure each row's defense is actually on.*

## 4. Worked Example — a hardening checklist run

Walk Cortex's posture against the catalog:

- **Redirect URIs (1):** exact hosts+ports (`localhost:5173/8080` in dev, `cortex.kakde.eu` in prod), path wildcard only. ✅
- **PKCE (2):** `S256` on `cortex-web`. ✅
- **State (3):** handled by `keycloak-js`. ✅
- **Token storage (4):** in memory via `keycloak-js`; never copied to `localStorage`. ✅
- **Algorithm pin (5):** server pins RS256 in the key selector. ✅
- **Issuer/audience (6):** `ClaimsVerifier` checks `iss` and `aud`-or-`azp`. ✅
- **Expiry (7):** checked, plus short access-token lifetimes + refresh. ✅
- **Key rotation (8):** JWKS cached 300s, refetched on unseen `kid`. ✅
- **CORS (9):** `webOrigins` scoped to real origins. ✅
- **Logout (10):** `post.logout.redirect.uris` allow-list. ✅
- **TLS (11):** dev uses `sslRequired: none` (localhost only); **prod requires HTTPS** at the ingress + realm. ✅
- **Refresh rotation (12):** a Keycloak realm setting to enable in production. ⚠️ verify it's on.

The one ⚠️ — confirming refresh-token rotation is enabled in prod — is exactly the kind of "boring setting nobody checked" that this chapter exists to catch.

## 5. Build It

Run this. It's an automated hardening linter — feed it a config and it flags the dangerous settings, just like a pre-ship review.

```python run
def audit(cfg):
    findings = []
    # 1. redirect URIs must be exact host+port (no bare '*', no missing slash before '*')
    for uri in cfg["redirect_uris"]:
        if uri == "*" or (uri.endswith("*") and not uri.rstrip("*").endswith("/")):
            findings.append(f"[HIGH] loose redirect_uri: {uri!r} (host-prefix match — look-alike domain risk)")
    # 2. PKCE
    if cfg.get("pkce") != "S256":
        findings.append("[HIGH] PKCE not S256 — public client open to code interception")
    # 5. algorithm pinned server-side
    if cfg.get("server_alg_pin") != "RS256":
        findings.append("[HIGH] server does not pin RS256 — alg-confusion risk")
    # 9. CORS
    if "*" in cfg.get("web_origins", []):
        findings.append("[MED] wildcard webOrigins — scope to real front-end origins")
    # 11. TLS in prod
    if cfg["env"] == "prod" and cfg.get("ssl_required") == "none":
        findings.append("[HIGH] sslRequired=none in PROD — tokens sniffable")
    # 12. refresh rotation
    if cfg["env"] == "prod" and not cfg.get("refresh_rotation"):
        findings.append("[MED] refresh-token rotation off in prod")
    return findings

good = dict(env="prod", redirect_uris=["https://cortex.kakde.eu/*"], pkce="S256",
            server_alg_pin="RS256", web_origins=["https://cortex.kakde.eu"],
            ssl_required="external", refresh_rotation=True)
bad  = dict(env="prod", redirect_uris=["https://cortex.kakde.eu*"], pkce="plain",
            server_alg_pin=None, web_origins=["*"], ssl_required="none", refresh_rotation=False)

print("AUDIT — hardened config:"); print("  clean ✅" if not audit(good) else "\n".join("  "+f for f in audit(good)))
print("\nAUDIT — sloppy config:");  print("\n".join("  " + f for f in audit(bad)))
```

**Now break it.** Take the `good` config and flip one field to a `bad` value — say `pkce="plain"` or `redirect_uris=["https://cortex.kakde.eu*"]` (drop the slash). Watch a single change surface a HIGH finding. That's the lesson of this whole chapter: a flawless protocol plus one sloppy field equals a breach. Run the linter; check every row.

## 6. Trade-offs & Complexity

Hardening trades **convenience for safety**, and the tension is real:

- **Exact redirect URIs** mean updating config when URLs change (vs. a lazy wildcard that's a vuln).
- **Short token lifetimes** mean more refreshes (vs. long-lived tokens with bigger blast radius).
- **Local verification** trades instant revocation for scale (Chapter 3's recurring theme).
- **Strict CORS** means listing every origin (vs. `*` that invites abuse).

The senior move is to make the *safe* choice the *default* and only loosen with a documented reason. Every relaxation should be a deliberate, reviewed decision — not a convenience someone reached for and forgot.

## 7. Edge Cases & Failure Modes

- **Defense-in-depth gaps.** No single control is enough. PKCE *and* exact redirects *and* `state` *and* TLS — remove any one and an attack path opens.
- **Dev settings in prod.** `sslRequired: none`, `AUTH_ENABLED=false`, `directAccessGrants` on, `admin/admin` — all fine on a laptop, catastrophic in production. Keep environments distinctly configured.
- **Silent failures.** A verifier that "fails open" (accepts on error) is worse than one that fails closed. Decide failure behavior explicitly and log it.
- **Untested rotation/expiry.** Key rotation and token expiry only matter if they actually work. Test them — rotate a key in staging and confirm old and new tokens behave correctly.

## 8. Practice

> **Exercise 1 — Run the catalog.** Pick any OAuth integration you can inspect (a side project, a tutorial repo). Walk the 12-row table and mark each ✅/⚠️/❌. What's the worst hole you find?

<details>
<summary><strong>Answer</strong></summary>

There's no fixed answer — the deliverable is the *habit* of walking all 12 rows and grading each defense as actually-on (✅), present-but-unverified (⚠️), or missing (❌). To do it like §4's posture review, for each row find the concrete artifact that proves the defense, e.g.: redirect URIs (1) — read the client's allow-list, confirm exact host+port and no bare/`*`-suffix wildcard; PKCE (2) — confirm `S256`; algorithm pin (5) — confirm the *server* pins RS256 and never reads the token's `alg`; CORS (9) — confirm `webOrigins` lists real origins, not `*`; TLS (11) — confirm HTTPS is required in prod.

On *picking the worst hole*, rank by **blast radius × likelihood**, and remember security is asymmetric (§1): one open door defeats every closed one. The HIGH-severity rows are the usual winners — a loose/prefix `redirect_uri` (1) (a look-alike domain captures auth codes), an unpinned algorithm (5) (`alg: none`/HS256 forgery → any forged identity), or `sslRequired: none` in prod (11) (tokens sniffable). The lesson worth stating: the worst hole is almost always a *boring misconfiguration*, not exotic crypto — exactly the ⚠️ this chapter exists to surface before an attacker does.

</details>

> **Exercise 2 — One field, one breach.** For three rows in the table, describe the *exact* attack that becomes possible if that single defense is misconfigured.

<details>
<summary><strong>Answer</strong></summary>

Three rows, each as "the one field → the exact attack":

- **Row 1 — loose `redirect_uri` (e.g. `https://app.example.com*`, no slash).** A **prefix match** now accepts a *look-alike domain*: `https://app.example.com.evil.net` shares the prefix. The attacker starts an Authorization Code flow pointing the `redirect_uri` at their look-alike host; Keycloak, seeing a "matching" registered URI, sends the **authorization code to the attacker**, who redeems it for the victim's tokens. (PKCE doesn't save you here if the attacker drives the whole flow.)
- **Row 5 — algorithm not pinned server-side.** The verifier trusts the token's own `alg` header. The attacker submits a token with **`alg: none`** (unsigned) — the server skips signature checking and believes the payload — or swaps **RS256→HS256** and signs with the *public* key the server already trusts. Either way they **forge an admin/any-user token** and walk in.
- **Row 7 — `exp` not checked (or no skew/short TTL).** A **stolen or logged token never dies**. An attacker who captured a token (from a log, a proxy, an old session) **replays it indefinitely**; the only reason a real leak isn't permanent is that `exp` is checked *and* lifetimes are short — remove the check and a single leak is forever.

The through-line: each is one missing field turning a *flawless protocol* into a breach — which is the chapter's whole thesis.

</details>

> **Exercise 3 — Threat-model a new endpoint.** Cortex adds `DELETE /api/submissions/{id}`. List the auth/authorization checks and the hardening settings that must be right for it to be safe. (You'll build the safe version in the capstone.)

<details>
<summary><strong>Answer</strong></summary>

A destructive, per-user write — so split it into **authentication**, **authorization**, and **hardening**:

- **AuthN (required):** token **required** (`verify`, not `verifyOptional`) — no anonymous deletes; reject absent/invalid tokens with 401. Full verification inherited: RS256 pinned, `iss` / `aud`-or-`azp` / `exp` checked.
- **AuthZ — ownership (the critical one):** the delete must be **scoped to the caller's `sub`**: `DELETE ... WHERE id = ? AND sub = <claims.sub>`. Derive the owner from the **verified token, never from a request parameter**. Without the `sub` predicate, `{id}` is an attacker-controlled object reference and any user can delete *anyone's* submission — **Broken Object Level Authorization** (OWASP API1), the #1 API risk. A non-matching id should return **404** (not 403), so you don't even confirm the row exists to a stranger.
- **Hardening settings:** **HTTPS** in prod (token rides the `Authorization` header). **Idempotency** — deleting an already-gone row is a no-op `204/404`, not an error. **Rate-limit per-user** so a script can't mass-delete. **Input-validate `{id}`** (well-formed, expected type). **Generic errors** — don't leak whether an id exists. **Audit-log** the delete (who/what/when) for repudiation.

The one-line takeaway: required auth gets you *a* user; the **ownership predicate on `sub`** is what stops that user from acting on *another's* data — and that's enforced in the query, by construction, not by a check you might forget.

</details>

```quiz
{
  "prompt": "A team ships an OIDC integration with perfect PKCE and signature verification, but sets `redirect_uris: ['https://app.example.com*']` (no slash). What's the risk?",
  "input": "Choose one:",
  "options": [
    "A look-alike domain (e.g. app.example.com.evil.net) matches the prefix, letting an attacker capture authorization codes",
    "None — PKCE makes redirect URIs irrelevant",
    "Tokens will be encrypted incorrectly",
    "The signature check will fail"
  ],
  "answer": "A look-alike domain (e.g. app.example.com.evil.net) matches the prefix, letting an attacker capture authorization codes"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[OAuth 2.0 Security Best Current Practice — RFC 9700](https://datatracker.ietf.org/doc/html/rfc9700)** — the authoritative hardening checklist; this chapter is a friendly tour of it.
- **[OWASP Top 10](https://owasp.org/Top10/)** — Broken Access Control (A01) and Security Misconfiguration (A05) are exactly the failure classes catalogued here.
- **[Keycloak — Server hardening / production guide](https://www.keycloak.org/server/configuration-production)** — the prod settings (TLS, hostname, secrets) that move you off the dev defaults.

---

**Next:** theory and integration done. Now *apply* it — design and add a brand-new protected endpoint to Cortex, end-to-end, with every check from this Part in place. → [22. Capstone: add a protected endpoint to Cortex](/cortex/production-engineering/iam-keycloak-oauth/capstone/add-a-protected-endpoint)
