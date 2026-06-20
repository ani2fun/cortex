---
title: '23. Threat-model practice & In the Wild'
summary: Sharpen your instincts on real OAuth breaches — each one a lesson from this Part, learned the hard way by someone else — then build a repeatable threat-modeling habit and meet the canonical RFCs.
---

# 23. Threat-model practice & In the Wild

## TL;DR

> The best way to internalize this Part is to study where real systems *broke*. "Sign in with Apple" let attackers forge any user. Facebook leaked 50 million access tokens. JWT libraries accepted unsigned tokens. Consent-phishing tricked users into granting real access. Each breach maps to a specific chapter you just read. This closing chapter walks those incidents, gives you a lightweight **threat-modeling habit** to catch the next one, and points you to the RFCs worth keeping on your shelf.

## 1. Motivation

You can read a spec and still not *feel* where the danger is. Breaches make it visceral. Every incident below was found in a real, production, often famous system — by attackers or researchers who asked the questions this Part trained you to ask: *Is the audience checked? Is the redirect exact? Is the signature actually verified? Did the user understand what they consented to?* Reading them is how "verify `aud`" stops being a line in a checklist and becomes a reflex.

## 2. Four breaches, four lessons

**① "Sign in with Apple" account takeover (2020).** Researcher Bhavuk Jain found that Apple's OIDC implementation would issue a valid ID token for *any* email the attacker requested, because of a flaw in how the token's subject was validated. With it, an attacker could get a token "proving" they were *you* at any app using Sign in with Apple — full account takeover, no password. **Lesson:** identity hinges on *verifying the right claims for the right party* (Chapters 9, 19). A token is only as trustworthy as the verification behind it. Bounty: $100,000.

**② Facebook's 50-million-token breach (2018).** A chain of bugs in the "View As" feature let attackers harvest *access tokens* for ~50 million accounts — and because a bearer token *is* its holder (Chapter 7), each one was a live key to an account. **Lesson:** bearer tokens are crown jewels; minimize blast radius with **short lifetimes** and guarded storage (Chapters 3, 7). The reason a leak isn't permanent is that tokens expire — *if* you made them short.

**③ The `alg: none` JWT library era (2015).** Multiple popular JWT libraries accepted tokens whose header said `alg: none` — i.e., *unsigned* — and happily trusted the payload. Attackers forged admin tokens by just writing the JSON. A related flaw let attackers swap `RS256` for `HS256` and sign with the *public* key. **Lesson:** never trust the token to tell you how to verify it; **pin the algorithm server-side** (Chapters 10, 19). Cortex pins RS256 precisely because of this class of bug.

**④ OAuth consent phishing (ongoing).** Attackers register a legitimate-looking OAuth app and email users a real provider consent screen asking for broad scopes (`read all mail`, `offline_access`). Users — trained to click "Allow" — grant a *real* token to the attacker's app. No password is stolen; the user *authorizes* the breach. **Lesson:** **scopes and consent are security** (Chapter 7). Least privilege and clear consent screens aren't UX niceties; they're the last line against a human clicking "Allow."

| Breach | Root question that would have caught it | Chapter |
|---|---|---|
| Sign in with Apple | "Is the token's subject/audience actually verified?" | 9, 19 |
| Facebook tokens | "How long does a leaked token stay valid?" | 3, 7 |
| `alg: none` | "Does the server pin the algorithm?" | 10, 19 |
| Consent phishing | "Does this app really need these scopes?" | 7 |

## 3. A threat-modeling habit you can actually keep

You don't need a heavyweight process. For any auth-touching feature, walk four questions — call it the **identity threat-model pass**:

1. **Spoofing** — *Could someone pretend to be another principal?* (Check: signature, `iss`, `aud`/`azp`, token freshness.)
2. **Tampering / Elevation** — *Could a request act beyond its rights?* (Check: derive the user from the token, not input; scope every query to the caller; enforce authorization server-side.)
3. **Information disclosure** — *Could a token or secret leak?* (Check: HTTPS, token in memory not `localStorage`, no secrets in JWTs, exact CORS.)
4. **Repudiation / Abuse** — *Could it be abused at scale or denied later?* (Check: rate limiting, audit logging, idempotency.)

Run those four on the bookmarks capstone, on every new endpoint, on every "log in with X." Most holes in this Part fall out of one of these questions — which is the point.

## 4. Build It — a threat-model worksheet

Run this. It turns the four-question pass into a checklist generator for any new endpoint — paste the output into your PR description.

```python run
def threat_model(endpoint, *, requires_auth, keys_on, writes_user_data, has_scopes):
    rows = []
    # Spoofing
    rows.append(("Spoofing",
        "verify signature + iss + aud/azp + exp" if requires_auth
        else "optional auth: a PRESENT token must still verify"))
    # Tampering / Elevation
    rows.append(("Tampering/Elevation",
        f"derive user from token.{keys_on}, scope every query by it"
        if writes_user_data else "enforce authorization server-side, not in UI"))
    # Disclosure
    rows.append(("Disclosure", "HTTPS; token in memory; no secrets in JWT; exact CORS"))
    # Abuse
    rows.append(("Abuse/Repudiation", "rate-limit (per-user if signed in); log; make writes idempotent"))
    if has_scopes:
        rows.append(("Consent", "request least-privilege scopes; explain them"))
    print(f"# Threat model: {endpoint}")
    for k, v in rows:
        print(f"  - [{k}] {v}")

threat_model("POST /api/me/bookmarks",
             requires_auth=True, keys_on="sub", writes_user_data=True, has_scopes=False)
print()
threat_model("POST /api/run",
             requires_auth=False, keys_on="sub", writes_user_data=False, has_scopes=False)
```

**Now use it.** Change the inputs for an endpoint you're designing (or one from a project you maintain). The generated checklist is the minimum bar — every item maps to a chapter in this Part. If you can't tick one, you found your next task.

## 5. The canonical shelf

The specifications worth reading at least once — terse, authoritative, and the final word when a tutorial and your intuition disagree:

- **[RFC 6749 — OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)** — the framework. Read §1 (the problem) and §4.1 (Authorization Code).
- **[RFC 7636 — PKCE](https://datatracker.ietf.org/doc/html/rfc7636)** — the fix for public clients (Chapter 6).
- **[RFC 7519 — JWT](https://datatracker.ietf.org/doc/html/rfc7519)** & **[RFC 7517 — JWK/JWKS](https://datatracker.ietf.org/doc/html/rfc7517)** — token and key formats (Chapters 10, 11).
- **[OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)** — identity on top of OAuth (Chapter 9).
- **[RFC 9700 — OAuth 2.0 Security BCP](https://datatracker.ietf.org/doc/html/rfc9700)** — the modern hardening bible (Chapter 21). If you read one, read this.

## 6. Trade-offs & Complexity

Security is never "done"; it's a posture you maintain. The trade-off running through this entire Part has been **convenience vs. safety**, and the mature stance is: *default to safe, relax only with a documented reason, and re-check when things change.* Threat models go stale — a new endpoint, a new scope, a new IdP each reopen questions you thought settled. The habit, not any single checklist, is the deliverable.

## 7. Edge Cases & Failure Modes

- **"We use OAuth, so we're secure."** OAuth is a framework, not a guarantee. Every breach above happened *inside* an OAuth system. The guarantees only hold if the verification and configuration hold.
- **Copy-pasted auth code.** Most of these breaches spread because flawed patterns got copied. Understand *why* each line is there (this Part's whole goal) before you reuse it.
- **Stale threat models.** A model that was complete six months ago may miss today's new endpoint. Re-run the four-question pass whenever the surface changes.
- **Ignoring the human.** Consent phishing and password reuse are *people* problems. The best technical design still has to account for users who click "Allow."

## 8. Practice

> **Exercise 1 — Reverse-engineer a breach.** Pick one of the four incidents. In a paragraph, describe the exact request an attacker made and the single check that would have stopped it. Which chapter taught that check?

<details>
<summary><strong>Answer</strong></summary>

Any of the four works; the skill is naming the **exact request** and the **single check**. Taking the **`alg: none` JWT-library era** (§2 ③):

The attacker takes a legitimate token's payload, edits a claim that grants power (say `"sub": "admin"` or `"role": "admin"`), and re-encodes the token with a header of **`{"alg":"none"}`** and an **empty signature segment** — literally `base64url(header) + "." + base64url(payload) + "."`. They send it as `Authorization: Bearer <forged>`. A vulnerable library reads `alg: none` from the *token itself*, concludes "no signature to check," and trusts the attacker-written payload — instant forged admin, no key needed. (The RS256→HS256 variant is the same idea: the attacker signs with the *public* key while the server, told `HS256` by the token, uses that public key as an HMAC secret.) **The single check that stops it: pin the verification algorithm server-side** — decide RS256 in your own code and *never* let the token's `alg` choose, so a `none`/HS256 token is rejected before its payload is ever read. **Taught in Chapters 10 and 19** (Cortex pins it via `JWSVerificationKeySelector(RS256, …)`). Root lesson: never trust the token to tell you how to verify it.

</details>

> **Exercise 2 — Threat-model something real.** Run the four-question pass on a login or API feature in a project you have access to. Write the worksheet. Find at least one gap.

<details>
<summary><strong>Answer</strong></summary>

No fixed answer — the deliverable is a worksheet that walks all four questions from §3 (**Spoofing, Tampering/Elevation, Information disclosure, Repudiation/Abuse**) and names a *concrete* check or gap per row, not a vague "looks fine." A worked shape, for a hypothetical `POST /api/orders`:

- **Spoofing** — *Could someone pose as another principal?* Token signature pinned (RS256)? `iss`/`aud`-or-`azp`/`exp` checked? → **Gap found: `exp` is checked but there's no clock-skew leeway, and access tokens live 24h** — a stolen token is good all day. Fix: short TTL + refresh (Chapters 7, 19).
- **Tampering / Elevation** — *Could a request act beyond its rights?* Is the user derived from the **token**, or from a request field? Is every query scoped by the caller's id? → e.g. confirm there's no `customerId` in the body that the handler trusts (BOLA risk).
- **Information disclosure** — HTTPS everywhere? Token in **memory**, not `localStorage`? No secrets baked into JWTs? Exact CORS, not `*`?
- **Repudiation / Abuse** — Rate-limited (per-user when signed in)? Audit-logged? Writes idempotent?

The bar (§4): every item maps to a chapter, and **if you can't tick one, you've found your next task.** The point of the exercise is precisely to surface *at least one* ⚠️ — most real features have one hiding in disclosure (token storage / over-broad CORS) or abuse (no metering).

</details>

> **Exercise 3 — Defend a design review.** A teammate proposes returning the access token in a URL fragment "to keep it simple." Cite the specific breach class and RFC that say no, and propose the correct alternative.

<details>
<summary><strong>Answer</strong></summary>

What your teammate has described *is the OAuth **Implicit flow*** (`response_type=token`), where the authorization server returns the access token directly in the redirect's **URL fragment** (`#access_token=…`). It's not "a simple shortcut" — it's a named, now-**deprecated** pattern, and the reasons are the **information-disclosure** breach class (§3, question 3):

- A token in the URL **leaks**: it lands in browser history, gets captured by `Referer` headers to third parties, shows up in server/proxy access logs, and is readable by any script on the page. The token *is* its holder (Chapter 7), so any of those leaks is account access.
- There's **no PKCE protection** of the delivered credential and no client-side confirmation that the token was minted for this exact transaction — exactly the gap PKCE closes for public clients.

**The RFCs say no explicitly:** **RFC 9700 (OAuth 2.0 Security BCP)** states clients **MUST NOT** use the Implicit grant and **SHOULD use Authorization Code with PKCE**; the same guidance is in **OAuth 2.0 for Browser-Based Apps**. (You met both in Chapters 6 and 21.)

**Correct alternative:** the **Authorization Code flow with PKCE (S256)** — the redirect carries only a single-use, PKCE-bound **authorization code** (useless if intercepted without the `code_verifier`), which the client exchanges for tokens off the URL; the token never appears in a fragment, history, or log. This is precisely what `keycloak-js` does for Cortex (`pkceMethod: "S256"`, Chapter 18). "Keep it simple" is satisfied *better* by the library that runs the safe flow than by hand-returning a token in the URL.

</details>

```quiz
{
  "prompt": "The 'Sign in with Apple' (2020) and 'alg: none' (2015) breaches share a single root lesson. What is it?",
  "input": "Choose one:",
  "options": [
    "A token is only as trustworthy as the verification behind it — verify the right claims and pin the algorithm; never trust the token to vouch for itself",
    "Tokens should be longer",
    "OAuth should never be used for login",
    "Passwords are safer than tokens"
  ],
  "answer": "A token is only as trustworthy as the verification behind it — verify the right claims and pin the algorithm; never trust the token to vouch for itself"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Sign in with Apple — account takeover write-up (Bhavuk Jain, 2020)](https://bhavukjain.com/blog/2020/05/30/zeroday-signin-with-apple/)** — breach ①, by the researcher who found it.
- **[Facebook 2018 security update](https://about.fb.com/news/2018/09/security-update/)** & **[Krebs on Security analysis](https://krebsonsecurity.com/2018/10/facebook-50m-accounts-compromised/)** — breach ②.
- **[Auth0 — Critical vulnerabilities in JWT libraries](https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/)** — breach ③, the `alg: none` class.
- **[Microsoft — Illicit consent grant attack](https://learn.microsoft.com/en-us/security/operations/incident-response-playbook-app-consent)** — breach ④, consent phishing and how to respond.

---

## You've finished Part 3 🎉

You started not knowing why apps stopped asking for your password, and you can now derive OAuth 2.0 and PKCE from the attacks they prevent, decode a JWT by eye, stand up Keycloak, read Cortex's real client and server auth code, and threat-model a new endpoint. That's a genuinely senior understanding of identity — the part of production engineering that, done wrong, ends careers and companies.

**Where to go next in _Production Engineering_:**

- **[Part 1 — ZIO](/cortex/production-engineering/zio):** the framework all that server-side auth code is written in. Now that you've *read* `KeycloakAuthBackend.scala`, learn the `ZIO[R, E, A]`, `ZLayer`, and `attemptBlocking` that make it tick.
- **[Part 2 — Liquibase](/cortex/production-engineering/liquibase):** you saw the `bookmarks` and `submission_allowlist` tables — Part 2 is how those schemas are created and evolved safely.
- **[Part 4 — Docker & Kubernetes](/cortex/production-engineering/docker-kubernetes):** how Keycloak and the Cortex server are packaged and run in production behind `keycloak.kakde.eu` and `cortex.kakde.eu`.

The arc continues: you've **secured** the system; next, learn how it's **written**, how its **data evolves**, and how it **ships**.
