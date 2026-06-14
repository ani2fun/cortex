---
title: '16. Roles, groups & protocol mappers'
summary: Authentication says who you are; roles say what you may do. Learn RBAC in Keycloak, how protocol mappers carry roles into the token, and when a database allow-list (like Cortex's) beats roles-in-the-token.
---

# 16. Roles, groups & protocol mappers

## TL;DR

> **Roles** are named permissions you assign to users (directly or via **groups**); they're how Keycloak does **authorization** (RBAC). A **protocol mapper** is the rule that *injects* a user's roles (and other attributes) into the tokens Keycloak mints — that's how the `realm_access.roles` array got into the JWT you decoded in Chapter 10. But putting permissions *in the token* has a cost (they're stale until the next login), so some authorization — like Cortex's submission gate — is better done with a live **database lookup**. This chapter covers both, and when to choose which.

## 1. Motivation

We've spent fifteen chapters on *authentication* — proving who you are. The other half of IAM is *authorization* — deciding what you may do (Chapter 1's second question). The dominant model is **Role-Based Access Control (RBAC)**: don't grant permissions to people one by one; grant them to **roles**, and assign **roles** to people. "Editors may publish" is one rule, no matter how many editors come and go.

Keycloak has first-class roles, and it can stamp them into your tokens via **protocol mappers** so your API can authorize without asking Keycloak anything. That's powerful — but it introduces the freshness problem from Chapter 3 all over again (a role baked into a token is stale until the token refreshes). Knowing *when* to put authorization in the token versus *when* to look it up live is the senior skill this chapter builds.

## 2. Intuition (Analogy)

A film studio with a security gate:

- **Roles** are job titles printed on a badge: *Cast*, *Crew*, *Director*. The gate's rules reference titles, not names: "Directors may enter the editing suite." Hire a new director and they just get the badge — no rule changes.
- **Groups** are departments you drop people into: put someone in *Camera Department* and they inherit *Crew* automatically. Groups bundle role assignments so you manage teams, not individuals.
- **Protocol mappers** are what *prints the title onto the badge*. Without the printer, your title exists in HR's records but isn't on the badge the gate can read. The mapper copies "Director" from the directory onto the physical badge (the token).
- **A live lookup** is the gate **phoning HR** instead of trusting the badge: slower, but always current — essential for "this person was fired five minutes ago."

| Studio | Keycloak / your app |
|---|---|
| Job title on a badge | Role in the token (`realm_access.roles`) |
| Department | Group |
| The badge printer | Protocol mapper |
| Phoning HR at the gate | A live DB authorization lookup |

## 3. Formal Definition

- **Role** — a named permission. **Realm roles** are global to the realm; **client roles** are scoped to one client. Assign roles to users directly or through groups. A **composite role** bundles other roles (e.g., `default-roles-cortex`).
- **Group** — a named collection of users with role mappings (and attributes) attached; membership grants the group's roles. Groups can nest.
- **Protocol mapper** — a per-client rule that adds data to issued tokens: roles (→ `realm_access.roles` / `resource_access.<client>.roles`), user attributes, computed claims. Mappers are *why* a token carries anything beyond the standard claims.
- **Where authorization runs:**
  - **In the token (RBAC claim):** the API reads `realm_access.roles` from the verified JWT — *zero* extra calls, but the role set is **as fresh as the token** (stale until refresh/expiry).
  - **Live lookup:** the API checks a database/service at request time — always current, **revocable instantly**, at the cost of a lookup. This is the Chapter 3 trade-off, now applied to *authorization*.

## 4. Worked Example — roles in the token vs Cortex's allow-list

In Chapter 10 you decoded a token whose payload contained:

```json
"realm_access": { "roles": ["default-roles-apps-prod", "submitter"] }
```

That `submitter` role got there because a **"realm roles" protocol mapper** on the client copied the user's roles into the token. An API *could* authorize submissions by checking `"submitter" in realm_access.roles` — pure token-based RBAC, no lookup.

**But Cortex actually does it differently** — and the difference is instructive. Cortex gates submissions with a **Postgres allow-list table** (`submission_allowlist`, created by a Liquibase changeset — Part 2), checked live on each submit:

```sql
-- From Cortex's v2-submissions.sql changeset
CREATE TABLE submission_allowlist (
  username   TEXT        PRIMARY KEY,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO submission_allowlist (username) VALUES ('ani2fun'), ('tester');
```

Why a DB lookup instead of a role-in-the-token? **Freshness and control.** To grant or revoke submission access, Cortex inserts/deletes one row and it takes effect on the *next request* — no waiting for a token to expire, no forcing a re-login. The cost is one indexed primary-key lookup per submit, which is nothing. For a permission that changes rarely and matters immediately, the live lookup wins.

```d2
direction: right

token_path: "Authorization in the token (RBAC)" {
  t: "Verified JWT\nrealm_access.roles" {shape: rectangle}
  api1: "API reads roles\n(no extra call)" {shape: rectangle}
  t -> api1: "fast, but stale until refresh"
}

db_path: "Authorization by live lookup (Cortex)" {
  jwt: "Verified JWT\n(identity only)" {shape: rectangle}
  api2: "API checks\nsubmission_allowlist" {shape: rectangle}
  db: "Postgres" {shape: cylinder}
  jwt -> api2: "who are you?"
  api2 -> db: "may THIS user submit?"
  db -> api2: "yes / no — always current"
}
```

The takeaway: **identity** comes from the token (authentication — always); **authorization** can come from *either* the token (fast, coarse, slightly stale) *or* a live lookup (current, revocable, costs a query). Mature systems use both — coarse roles in the token for cheap gating, live lookups for anything that must be instantly revocable.

## 5. Build It

Run this. It implements both strategies and exposes their one real difference: what happens the instant you revoke access.

```python run
# A token issued a while ago, carrying a role baked in at login time.
verified_token = {
    "sub": "kc-user-1000",
    "preferred_username": "ani2fun",
    "realm_access": {"roles": ["default", "submitter"]},   # snapshot from login
}

# The live source of truth (Cortex's allow-list), editable RIGHT NOW.
allowlist = {"ani2fun", "tester"}

def may_submit_via_role(tok):
    return "submitter" in tok["realm_access"]["roles"]      # reads the token

def may_submit_via_lookup(tok):
    return tok["preferred_username"] in allowlist            # reads live state

print("role-in-token :", may_submit_via_role(verified_token))    # True
print("live lookup   :", may_submit_via_lookup(verified_token))  # True

# ---- Now REVOKE access right now ----
allowlist.discard("ani2fun")
print("\n-- after revoking ani2fun --")
print("role-in-token :", may_submit_via_role(verified_token))    # STILL True (stale!)
print("live lookup   :", may_submit_via_lookup(verified_token))  # False (instant)
```

**Now break it.** Notice the role-in-token check *still says True* after revocation — the old token still carries `submitter` until it expires or the user re-logs-in. The live lookup flips to False immediately. That single divergence is the whole decision: if a permission must be revocable *now*, don't bake it into the token.

## 6. Trade-offs & Complexity

| | Roles in the token (RBAC claim) | Live DB/service lookup |
|---|---|---|
| Speed | Fastest — no extra call | One lookup per request |
| Freshness | Stale until token refresh | Always current |
| Instant revocation | No | Yes |
| Coupling | API depends only on the token | API depends on a store being up |
| Best for | Coarse, slow-changing roles (admin, tier) | Fine-grained, must-be-current gates |

Neither is "right." Use roles-in-token for broad, stable capabilities (is this an admin? a paying tier?) and live lookups for anything sensitive or fast-changing (is this account suspended? on the allow-list?). Cortex uses identity-from-token + live-allow-list for submissions, which is a textbook application of the rule.

## 7. Edge Cases & Failure Modes

- **Stale roles.** A token minted before a demotion still carries the old role until it expires. For privileged roles, prefer short token lifetimes *or* a live check.
- **Token bloat.** Mapping huge role/group sets into every token makes tokens large (they ride on every request). Map only what the API actually reads.
- **Client roles vs realm roles confusion.** They land in *different* claims (`resource_access.<client>.roles` vs `realm_access.roles`). Read the right one.
- **Authorization only in the front-end.** Hiding a button is UX, not security. Every permission must be enforced at the API with the verified token (and/or a lookup). Never trust the client to authorize itself.

## 8. Practice

> **Exercise 1 — Choose the mechanism.** For each, pick "role in token" or "live lookup" and justify: (a) "is an admin"; (b) "account suspended for abuse 30 seconds ago"; (c) "is on the paid tier"; (d) "is currently allowed to submit solutions."

<details>
<summary><strong>Answer</strong></summary>

The deciding question every time: **must a change take effect *before the current token expires*?** If yes → live lookup. If it's coarse and slow-changing → role in the token.

- **(a) "is an admin" → role in token.** Admin is a broad, stable capability that changes rarely; reading `realm_access.roles` from the verified JWT costs zero extra calls. (If admin were *highly* sensitive you'd pair it with short token lifetimes, but the default is fine.)
- **(b) "account suspended 30 seconds ago" → live lookup.** This is the textbook case for *instant revocation*. A role baked into a token stays `True` until the token expires — a suspended abuser would keep their access for the rest of the token's life. Only a live check flips to denied *now*.
- **(c) "is on the paid tier" → role in token.** A billing tier is coarse and changes infrequently (a plan change can reasonably take effect on the next token refresh); cheap token-based RBAC fits.
- **(d) "is currently allowed to submit solutions" → live lookup.** This is *exactly* what Cortex does — a Postgres `submission_allowlist` checked per submit — because granting/revoking must take effect on the **next request** (insert/delete one row), not after a re-login, and the cost is one indexed primary-key lookup.

The pattern: **identity always from the token; authorization from the token when coarse and stable, from a live lookup when it must be instantly revocable.** Mature systems use both.

</details>

> **Exercise 2 — Trace the mapper.** Explain, in two sentences, how the `submitter` role traveled from Keycloak's user directory into the `realm_access.roles` array of the JWT in Chapter 10.

<details>
<summary><strong>Answer</strong></summary>

The user has the `submitter` realm role assigned in Keycloak's directory (directly, or inherited via a group), but a role in the directory is invisible to your API until something *writes it onto the token*. At login, a **"realm roles" protocol mapper** configured on the client does exactly that — it copies the user's assigned realm roles into the issued JWT under the **`realm_access.roles`** claim, which is why the decoded Chapter 10 payload showed `"realm_access": { "roles": [..., "submitter"] }`.

(The studio analogy: the role is the job title in HR's records; the protocol mapper is the *badge printer* that stamps the title onto the physical badge the gate can read.)

</details>

> **Exercise 3 — Justify Cortex.** Argue why Cortex gates submissions with a Postgres allow-list rather than a Keycloak role. What does it gain, and what does it pay?

<details>
<summary><strong>Answer</strong></summary>

**What Cortex gains: freshness and control — instant revocation.** Submission access is a permission that must take effect *the moment it changes*. With the Postgres `submission_allowlist` table, granting or revoking is a single `INSERT`/`DELETE`, and it's honored on the **very next request** — no waiting for a token to expire, no forcing the user to re-login. A Keycloak role carried in the token can't do this: a token minted *before* a revocation still carries `submitter` in `realm_access.roles` until it expires, so a revoked user could keep submitting for the rest of the token's lifetime (the stale-role problem, which is just Chapter 3's freshness trade-off applied to *authorization*).

**What it pays: one indexed lookup per submit.** Instead of reading a claim that's already in the verified JWT (zero extra calls), the API must hit Postgres on each submission — a primary-key lookup on `username`, which is negligible — and it accepts a dependency on that store being up.

**Why the trade is right here:** the permission changes rarely but matters *immediately*, so paying a tiny, cheap query to get instant revocability is an easy win. Note the division of labor: **identity still comes from the token** (the API reads the verified `sub`/`preferred_username` — authentication is always token-based); only the *authorization* decision is moved to the live lookup. That's the textbook application of the chapter's rule: coarse, stable capabilities in the token; anything that must be instantly revocable, look up live.

</details>

```quiz
{
  "prompt": "You revoke a user's permission. Which authorization mechanism reflects the change IMMEDIATELY, before the user's current token expires?",
  "input": "Choose one:",
  "options": [
    "A live database/service lookup at request time",
    "A role baked into the token by a protocol mapper",
    "The token's signature",
    "The `iss` claim"
  ],
  "answer": "A live database/service lookup at request time"
}
```

## In the Wild

- **[Keycloak — Roles, groups, and the user model](https://www.keycloak.org/docs/latest/server_admin/#assigning-permissions-using-roles-and-groups)** — RBAC and groups in depth.
- **[Keycloak — Protocol mappers](https://www.keycloak.org/docs/latest/server_admin/#_protocol-mappers)** — how to put roles and attributes into tokens.
- **[Cortex `v2-submissions.sql`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/db/changelog/changes/v2-submissions.sql)** — the real allow-list table behind Cortex's submission gate (and a preview of Part 2, Liquibase).

---

**Next:** we've built the whole protocol and configured Keycloak. Time to put it together in the real system — starting with the product decision that frames Cortex's entire auth design: the code-edit auth gate. → [17. The code-edit auth gate (ADR-0013)](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/the-code-edit-auth-gate)
