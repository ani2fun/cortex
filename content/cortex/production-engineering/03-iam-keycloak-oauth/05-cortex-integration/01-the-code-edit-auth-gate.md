---
title: '17. The code-edit auth gate (ADR-0013)'
summary: Before any code, a product decision. Cortex lets anyone RUN code anonymously but requires sign-in to EDIT or SUBMIT. Understand that decision — written up as an architecture decision record — because it shapes every line of the integration.
---

# 17. The code-edit auth gate (ADR-0013)

## TL;DR

> Cortex's auth isn't "log in to use the site." It's a deliberate, narrow **gate**: **anyone can read and run** the canonical code on any page (metered per-IP); you must **sign in to edit** code or **submit** a solution (then metered per-user). This is an *architecture decision* with reasons and consequences — captured here as **ADR-0013** — and it explains *why* the integration looks the way it does: optional auth on most endpoints, required auth on a few, and a rate limiter that meters differently for anonymous vs signed-in callers.

## 1. Motivation

Most tutorials show you *how* to wire auth but never *why this much and no more*. That's backwards — the security design should follow from a product decision, not the other way round. So before reading Cortex's client and server auth code (the next chapters), we pin down the decision they implement.

The decision answers a real tension. Cortex wants to be **frictionless**: a curious visitor should run the examples instantly, no account, no barrier. But running code costs real CPU on real servers (the go-judge sandbox), and *saving* your work or *submitting* solutions needs a stable identity to attach to. So Cortex draws the line precisely where identity first becomes necessary — and not one step sooner.

## 2. Intuition (Analogy)

A **public library with a makerspace**.

- **Browsing and reading** — walk in off the street, no card. (Reading chapters; running the canonical code.)
- **Using the 3D printer** — you can run a demo print the library pre-loaded, but to *load your own design* or *submit a model to the gallery*, you need a library card. (Editing code; submitting solutions.)
- **Why the line is there:** the printer costs materials (server CPU), and a gallery submission needs your name on it (a stable identity). Reading costs the library nothing and needs no identity.

The library doesn't make you sign up at the door — that would kill the very openness that makes it valuable. It asks for a card *exactly* when you do something that consumes scarce resources or needs to be attributed to you. That's the auth gate.

## 3. The decision, as an ADR

Engineering teams record significant choices as **Architecture Decision Records** — short documents stating the context, the decision, and its consequences, so future readers know *why*. Here is Cortex's, formalized:

---

**ADR-0013 — Gate code editing and submission behind authentication**

**Status:** Accepted.

**Context.** Cortex serves interactive code on every chapter. Running code consumes sandbox CPU and must be rate-limited to prevent abuse. Anonymous use is essential to the product's openness, but (a) abuse metering needs *some* caller identity, and (b) editing and submitting need a *stable, attributable* identity. We run Keycloak as the identity plane and the SPA is a public client.

**Decision.**
1. **Reading and running canonical code is anonymous.** No login required. Anonymous runs are metered **per source IP**.
2. **Editing code requires sign-in.** For anonymous visitors the editor is **read-only**; the **Edit** button shows a lock and opens the sign-in modal. Signed-in users get an editable editor and an identity chip.
3. **Submitting solutions requires sign-in** *and* authorization (the allow-list of Chapter 16). Signed-in runs are metered **per user**, with a quota notice as the budget runs low.
4. **Auth is optional at the transport layer.** Most endpoints accept a request with *or* without a bearer token (`verifyOptional`); a few (`/api/me`, submissions) *require* one (`verify`). When `AUTH_ENABLED=false` (local dev), the gate is disabled entirely and everything is open.

**Consequences.**
- The server needs **optional-auth** handling on hot endpoints (anonymous allowed) and **required-auth** on a few. (Chapter 19.)
- The rate limiter must support **two bucket types**: per-IP for anonymous, per-user for signed-in. (Chapter 20.)
- The client must boot auth **non-blockingly** (the page must render and run code before, or without, login) and only prompt on a gated action. (Chapter 18.)
- Disabling auth must be a single switch for local development.

---

That ADR is the spec the next three chapters implement. Everything — optional vs required verification, two rate-limit buckets, non-blocking client boot — falls out of it.

## 4. Worked Example — the gate in the UI

Here's the gate's behavior on a single code block, by caller state:

| State | Read code | Run canonical code | Edit code | Submit solution | Metered by |
|---|---|---|---|---|---|
| **Anonymous** | ✅ | ✅ | 🔒 → sign-in modal | 🔒 → sign-in | per **IP** |
| **Signed in** | ✅ | ✅ | ✅ editable + identity chip | ✅ (if allow-listed) | per **user** |
| **Auth disabled** (dev) | ✅ | ✅ | ✅ open to all | ✅ | (no metering) |

This is exactly what Cortex's `RunnableCodeBlock` implements: anonymous Monaco renders read-only with a locked **Edit** badge that opens the sign-in modal; **Run** always works (the canonical source executes, metered per-IP); signed-in flips Monaco editable and shows an identity chip and an hourly-quota notice as the budget runs low. You can watch it live: open any runnable code block on this site, click **Run** (works), then click **Edit** while signed out (lock → sign-in).

## 5. Build It

Run this. It's the gate as a pure decision function — the same branching the UI and API perform.

```python run
def gate(action, signed_in, auth_enabled=True, allow_listed=False):
    if not auth_enabled:
        return "ALLOW (auth disabled — dev mode)"
    if action in ("read", "run"):
        return "ALLOW (anonymous OK; metered per-IP)" if not signed_in \
               else "ALLOW (signed in; metered per-user)"
    if action == "edit":
        return "ALLOW (editor unlocked)" if signed_in else "PROMPT SIGN-IN (editor read-only)"
    if action == "submit":
        if not signed_in:
            return "PROMPT SIGN-IN"
        return "ALLOW (submitted)" if allow_listed else "403 (signed in, but not on allow-list)"
    return "unknown action"

for action in ("read", "run", "edit", "submit"):
    print(f"{action:7s} anon : {gate(action, signed_in=False)}")
    print(f"{action:7s} user : {gate(action, signed_in=True, allow_listed=(action!='submit') or True)}")
print("\nedit  anon, dev:", gate("edit", signed_in=False, auth_enabled=False))
```

**Now break it.** Change `gate("run", signed_in=False)` to require sign-in (move `"run"` out of the anonymous-OK branch). Notice how that one change would force *every visitor to log in before running an example* — destroying the frictionless reading experience. The ADR's first decision (anonymous run) is doing real product work; this function shows exactly where.

## 6. Trade-offs & Complexity

| Gate where Cortex gates (edit/submit) | Gate everything (login wall) | Gate nothing |
|---|---|---|
| Frictionless reading + run | Hostile to casual readers | No abuse control, no attribution |
| Identity only when needed | Identity always (overkill) | — |
| Two rate-limit modes to build | One mode | One mode |
| Optional + required auth on the server | All-required (simpler) | None |

Cortex's middle path costs *implementation complexity* — optional-vs-required auth, two metering modes, non-blocking boot — in exchange for the best product experience. A login wall is simpler to build and worse to use; no gate is simplest and unsafe. The decision is a classic "spend engineering to buy user experience and safety."

## 7. Edge Cases & Failure Modes

- **Auth still booting when the user clicks Edit.** The client must treat "auth not ready yet" like anonymous (read-only), not crash. (Chapter 18 handles the boot states.)
- **Anonymous abuse.** Per-IP metering is weaker than per-user (NAT, rotating IPs), so anonymous limits are tighter. The gate accepts this as the price of openness.
- **Auth disabled in production by accident.** `AUTH_ENABLED=false` opens *everything*. It's a dev-only switch; shipping it to prod would remove the gate entirely.
- **Front-end-only gating.** Locking the *Edit button* is UX; the *server* must still reject unauthenticated submits. The gate is enforced on both sides, with the server authoritative.

## 8. Practice

> **Exercise 1 — Justify the line.** In two sentences, explain why Cortex gates *edit* but not *run*. What scarce resource or requirement appears at "edit" that isn't present at "run"?

<details>
<summary><strong>Answer</strong></summary>

Both *run* and *edit* cost CPU, so cost alone can't draw the line — what *edit* adds is the need for a **stable, attributable identity**: edited code (and the submission that follows) has to *belong to someone* so it can be saved, metered per-user, and later submitted under the allow-list. Running the canonical source needs no such attribution — it's the same code for everyone, meterable by the coarse `ip:` signal — so Cortex gates exactly where identity first becomes *necessary* (edit/submit) and not one step sooner, because asking for a login at *run* would destroy the frictionless reading the ADR exists to protect.

The principle: gate on the *requirement* (attribution/ownership), not merely on the *cost* — cost is handled by per-IP metering, identity is handled by the gate.

</details>

> **Exercise 2 — Write a consequence.** The ADR's decision #1 (anonymous run, per-IP) forces a specific capability on the rate limiter. State it. (Preview of Chapter 20.)

<details>
<summary><strong>Answer</strong></summary>

The limiter must support **two bucket types** and pick between them from the caller's identity: a **per-IP** bucket (`ip:<addr>`) for anonymous runs, and a **per-user** bucket (`user:<sub>`) for signed-in ones. Decision #1 is what forces the per-IP capability specifically — because anonymous callers have *no stable identity* to key on, the only signal left to meter them by is their source IP.

That's the whole consequence: "anonymous run" can't mean "unmetered" (CPU is scarce), and it can't be metered per-user (there's no user), so it *must* be metered per-IP — a coarse, shared key. The decision to allow anonymous runs directly creates the requirement for an IP-keyed bucket alongside the user-keyed one.

</details>

> **Exercise 3 — Draft an ADR.** Pick a feature on any site you use (e.g., "comment on a post"). Write a three-part ADR (Context / Decision / Consequences) for where it should place its auth gate.

<details>
<summary><strong>Answer</strong></summary>

There's no single right answer — the skill is making each part *follow* from the one before, exactly as ADR-0013 does (Context names a tension → Decision draws a line → Consequences are the costs that line forces). A worked example for "comment on a post":

**ADR — Gate commenting behind authentication; reading comments is anonymous.**

- **Context.** Posts and their comments are public — anonymous reading is core to reach and SEO. But a comment is *authored content* that must be attributable (moderation, edit/delete, abuse bans, reputation) and is a prime spam/abuse target. We run an IdP and the site is a public client.
- **Decision.** (1) **Reading** posts and comments is **anonymous** — no login. (2) **Posting a comment requires sign-in** — the author is taken from the verified token's `sub`, never from request input. (3) Voting/reporting also requires sign-in (needs a stable identity to dedupe). (4) Server enforces the gate; the UI only *shows* a sign-in prompt on the comment box.
- **Consequences.** Need **optional auth** on read endpoints, **required auth** on write; abuse metering keys **per-user** for comments (a stable identity exists) and **per-IP** only for anonymous reads; the client must render the thread before auth is ready and prompt only when the user clicks "Comment"; moderation/bans become possible *because* every comment carries a `sub`.

The tell that you've done it right: each Consequence is something the Decision *forced*, not a wish-list — just as in ADR-0013, "anonymous run" forced the per-IP bucket and "required auth on submit" forced the optional/required split.

</details>

```quiz
{
  "prompt": "Under Cortex's auth gate (ADR-0013), what can an anonymous visitor do?",
  "input": "Choose one:",
  "options": [
    "Read chapters and run the canonical code (metered per-IP) — but not edit or submit",
    "Nothing until they log in",
    "Everything, including editing and submitting",
    "Only read, not run, code"
  ],
  "answer": "Read chapters and run the canonical code (metered per-IP) — but not edit or submit"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Michael Nygard — "Documenting Architecture Decisions"](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions.html)** — the original ADR format used above. A five-minute read that will change how you record decisions.
- **[Cortex `RunnableCodeBlock.scala`](https://github.com/ani2fun/cortex/blob/main/client/src/main/scala/cortex/client/components/book/RunnableCodeBlock.scala)** — the component that implements the gate (read-only Monaco, locked Edit, identity chip, quota notice).
- **[OWASP — Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)** — why server-side enforcement (not just UI gating) is non-negotiable.

---

**Next:** the client side of the gate. How does the SPA boot Keycloak without blocking the page, adopt an existing session silently, and keep the token fresh? Read the real `keycloak-js` integration. → [18. Client side: keycloak-js, check-sso & silent refresh](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/client-side-keycloak-js)
