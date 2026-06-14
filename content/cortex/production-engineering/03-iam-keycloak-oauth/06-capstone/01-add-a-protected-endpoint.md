---
title: '22. Capstone: add a protected endpoint to Cortex'
summary: Put it all together. Design a brand-new authenticated endpoint for Cortex — token required, per-user storage, identity-aware metering, hardened — following the exact patterns from this Part.
---

# 22. Capstone: add a protected endpoint to Cortex

## TL;DR

> Everything in this Part converges here: you'll design **`POST /api/me/bookmarks`** — "bookmark this chapter," a per-user feature that *requires* sign-in. We'll specify it end-to-end: a **bearer-secured** endpoint, **required** token verification, **per-user** storage keyed on the stable `sub`, **identity-aware** behavior, and a pass through the **hardening checklist**. It reuses Cortex's real patterns (Tapir secure endpoints, `verify`, the `Authorization` header), so it's a blueprint you could actually merge.

## 1. The brief

Add a feature: a signed-in reader can **bookmark** a chapter and list their bookmarks. Anonymous visitors can't bookmark (there's no stable identity to attach a bookmark to). This is a textbook "requires authentication, scoped to the user" feature — exactly the shape of Cortex's existing submissions endpoints.

**Acceptance criteria:**
- `POST /api/me/bookmarks` with a chapter slug → creates a bookmark for the current user. **Requires** a valid token.
- `GET /api/me/bookmarks` → lists *only the caller's* bookmarks.
- Anonymous requests → `401`.
- A user can never see or delete another user's bookmarks.

## 2. Design — the five decisions

Every protected endpoint answers the same five questions you've met across this Part:

| Decision | For bookmarks | Chapter |
|---|---|---|
| **Required or optional auth?** | **Required** — no identity, no bookmark | 17, 19 |
| **What identity do we key on?** | The stable **`sub`** (never username/email) | 15, 19 |
| **Authorization beyond authN?** | Ownership: a user only touches *their* rows | 1, 16 |
| **Metering?** | Per-user is natural (signed-in only) | 20 |
| **Hardening?** | The Chapter 21 checklist | 21 |

Answer those five and the implementation is mechanical.

## 3. The data model

A bookmark belongs to a user and points at a chapter. Key everything on `sub`:

```sql
-- A Liquibase changeset (Part 2 covers the mechanics)
CREATE TABLE bookmarks (
  sub        TEXT        NOT NULL,        -- the stable user id from the token
  book       TEXT        NOT NULL,
  chapter    TEXT        NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (sub, book, chapter)        -- one bookmark per user per chapter
);
CREATE INDEX bookmarks_by_user ON bookmarks (sub, created_at DESC);
```

Note the `PRIMARY KEY (sub, …)`: ownership is *built into the schema*. A query for "my bookmarks" is `WHERE sub = ?` — there's no way to accidentally return someone else's, because every read is scoped by the caller's `sub`.

## 4. The endpoint — secured by construction

Cortex defines endpoints with **Tapir**, and a *secure* endpoint takes the bearer token as a typed security input — the same pattern as the real `submitSolution` / `listMySubmissions`. Sketch:

```scala
// Endpoint definition (shared): the bearer token is a SECURITY input, so the
// handler can't run without one — "required auth" enforced by the type.
val createBookmark =
  endpoint.post
    .in("api" / "me" / "bookmarks")
    .securityIn(auth.bearer[String]())          // <-- Authorization: Bearer <token>
    .in(jsonBody[BookmarkRequest])
    .out(jsonBody[BookmarkResponse])
    .errorOut(/* 401 / 403 / 429 */ ...)

// Server logic: verify (required), then act AS that identity.
val createBookmarkServer =
  createBookmark.serverSecurityLogic(token => auth.verify(token))   // Chapter 19; fails ⇒ 401
    .serverLogic { claims => req =>
      // `claims.sub` is the ONLY user this handler will ever write for.
      bookmarks.add(claims.sub, req.book, req.chapter)              // ownership by construction
    }
```

Two things make this safe *by construction*:

1. **`securityIn(auth.bearer)` + `serverSecurityLogic(auth.verify)`** means the handler literally cannot execute without a verified token — "required auth" is enforced by the endpoint's type, not by a check you might forget.
2. **The handler only ever sees `claims.sub`** and writes/reads with it. There's no `userId` parameter from the request to tamper with — the identity comes from the *verified token*, never from client input. This kills the "broken access control" bug (Chapter 1) at the design level.

The client side reuses Chapter 18's pattern exactly — attach the bearer header when signed in:

```scala
def addBookmark(token: String, req: BookmarkRequest): Future[BookmarkResponse] =
  backend.send(createBookmarkRequestFn(token)(req))   // toSecureRequest…: token ⇒ Authorization header
```

## 5. Build It — the handler's logic, runnable

Run this. It's the endpoint's core decisions — required auth, ownership, no cross-user access — as a self-contained model.

```python run
class Forbidden(Exception): ...
class Unauthorized(Exception): ...

bookmarks = []   # rows: {"sub", "book", "chapter"}

def verify(token):
    # Stand-in for Chapter 19's real verifier. None ⇒ no/invalid token.
    return {"sub": "user-ada"} if token == "ada.jwt" else None

def create_bookmark(token, book, chapter):
    claims = verify(token)
    if claims is None:
        raise Unauthorized("401 — sign in to bookmark")        # REQUIRED auth
    bookmarks.append({"sub": claims["sub"], "book": book, "chapter": chapter})
    return f"bookmarked {book}/{chapter} for {claims['sub']}"

def list_my_bookmarks(token):
    claims = verify(token)
    if claims is None:
        raise Unauthorized("401")
    # Scoped to the caller's sub — cannot return anyone else's.
    return [b for b in bookmarks if b["sub"] == claims["sub"]]

print(create_bookmark("ada.jwt", "production-engineering", "iam/oauth"))
try:
    create_bookmark(None, "production-engineering", "iam/oauth")     # anonymous
except Unauthorized as e:
    print(e)
print("ada's bookmarks:", list_my_bookmarks("ada.jwt"))

# An attacker with their OWN valid token still can't see ada's rows:
bookmarks.append({"sub": "user-eve", "book": "x", "chapter": "y"})
print("eve sees only:", [b for b in bookmarks if b["sub"] == "user-eve"])
```

**Now break it.** Add a `user_id` parameter to `create_bookmark` and write the row with *that* instead of `claims["sub"]`. Now a caller could bookmark *as someone else* by passing a different id — the exact broken-access-control hole from Chapter 1. The fix is the design rule above: **derive the user from the verified token, never from request input.** Delete the parameter; trust only `claims.sub`.

## 6. Hardening pass (Chapter 21 checklist)

Before "done," run the catalog:

- **Required auth (17, 19):** ✅ enforced by `serverSecurityLogic(auth.verify)`.
- **Ownership (1, 16):** ✅ every query scoped by `claims.sub`; no user id from input.
- **Verification (11, 19):** ✅ RS256 pinned, `iss`/`aud`-or-`azp`/`exp` checked (inherited from `auth.verify`).
- **Transport (21):** ✅ HTTPS in prod; token in `Authorization` header.
- **Metering (20):** consider a per-user write limit so a script can't create millions of bookmarks.
- **Input validation:** validate `book`/`chapter` against known slugs; don't store arbitrary strings.
- **Idempotency:** the `PRIMARY KEY (sub, book, chapter)` makes a double-POST a no-op, not a duplicate.

Pass all rows and the endpoint is production-shaped — not just "works," but *safe*.

## 7. Trade-offs & Complexity

- **Required vs optional:** bookmarks are inherently per-user, so required auth is correct. (Contrast `/api/run`, which is optional — Chapter 17.) Choosing wrong here would either lock out legitimate anonymous use or attach data to no one.
- **Roles vs ownership:** bookmarks need *ownership* (your rows), not *roles* (a capability). No protocol mapper needed — the `sub` in the verified token is enough. (Chapter 16's "key on identity, not roles" applied.)
- **Store choice:** Postgres (consistent, queryable) vs a cache. Bookmarks want durability, so Postgres — same as submissions.

## 8. Practice

> **Exercise 1 — Add delete.** Design `DELETE /api/me/bookmarks/{book}/{chapter}`. Write the security logic and the SQL `WHERE` clause. How does ownership-by-`sub` make "can't delete someone else's" automatic?

<details>
<summary><strong>Answer</strong></summary>

Reuse the exact secure-by-construction pattern from §4 — required auth via Tapir, then act *as* the verified identity:

```scala
val deleteBookmark =
  endpoint.delete
    .in("api" / "me" / "bookmarks" / path[String]("book") / path[String]("chapter"))
    .securityIn(auth.bearer[String]())                 // required auth, enforced by the type

deleteBookmark
  .serverSecurityLogic(token => auth.verify(token))    // Chapter 19; absent/invalid ⇒ 401
  .serverLogic { claims => (book, chapter) =>
    bookmarks.delete(claims.sub, book, chapter)        // ONLY ever the caller's sub
  }
```

```sql
DELETE FROM bookmarks
WHERE sub = ?            -- the caller's claims.sub, from the verified token
  AND book = ? AND chapter = ?;
```

**Ownership is automatic because the `WHERE sub = ?` is bound to `claims.sub`, which comes from the verified token — never from request input.** The path only carries `{book}/{chapter}`; there is no `userId` to tamper with. So even if an attacker names *someone else's* `book/chapter`, the `sub = <their own>` predicate means the row simply **doesn't match** — zero rows deleted. You can't delete what the query filters you away from. (Return **204** on success and on a no-match alike — idempotent, and it doesn't reveal whether another user's bookmark exists.) This is Broken Object Level Authorization (OWASP API1) defeated *by construction*, exactly as the chapter argues.

</details>

> **Exercise 2 — Make it optional-aware.** Suppose product wants anonymous users to *preview* bookmarking (client-side only, not saved). What changes server-side (hint: probably nothing) and what changes client-side?

<details>
<summary><strong>Answer</strong></summary>

- **Server-side: nothing changes — and that's the point.** A *preview* is never persisted, so it never touches `POST /api/me/bookmarks`. The endpoint stays **required-auth** (a real save still needs a `sub` to own the row). Crucially, you must **not** loosen it to `verifyOptional` to accommodate the preview — anonymous writes would then have no owner, reintroducing the exact "data attached to no one" failure the gate exists to prevent. The preview lives entirely above the API.
- **Client-side: everything changes.** When anonymous, the UI lets the user *toggle* the bookmark icon and holds that intent in **local component/store state only** (optionally `localStorage` for the session) — visibly "previewing," nothing sent. The moment they invoke a real save it becomes a **gated action**: fire `signIn()` (Chapter 18's redirect), and on return — now `Authed` — replay the intent by calling `addBookmark(token, …)` with the bearer header attached. Anonymous preview is a *client illusion*; persistence still crosses the gate.

This mirrors ADR-0013 precisely: reading/previewing is free and anonymous; the *write* is where identity becomes mandatory, and the server stays the authoritative gate regardless of any client-side affordance.

</details>

> **Exercise 3 — Threat-model your endpoint.** Run the full Chapter 21 catalog against your `DELETE` design. Find at least one row that needs attention (hint: metering, or input validation on the slug).

<details>
<summary><strong>Answer</strong></summary>

Walk the 12-row catalog against `DELETE /api/me/bookmarks/{book}/{chapter}`. Most rows are **inherited and already ✅** because the endpoint reuses Cortex's verified-auth path: algorithm pin (5), `iss`/`aud`-or-`azp` (6), expiry (7), JWKS rotation (8) all come for free from `auth.verify`; redirect/CORS/logout/TLS (1, 9, 10, 11) are realm-level and unchanged; token storage (4) is `keycloak-js` in memory. The rows that **need active attention** for *this* endpoint:

- **Authorization / ownership (the headline).** Not a row in the OAuth catalog but the one that matters most here: scope the delete by `claims.sub` (Exercise 1). Miss it → Broken Object Level Authorization.
- **Abuse / metering (row 12-adjacent, Chapter 20).** A destructive write needs a **per-user rate limit** so a script can't hammer deletes; per-user is natural since the endpoint is signed-in-only. ⚠️
- **Input validation on the slug.** `{book}/{chapter}` are attacker-controlled path segments — **validate against known book/chapter slugs**, don't pass raw strings into queries or echo them back. Parameterized SQL already blocks injection, but reject nonsense early. ⚠️
- **Idempotency / repudiation.** Make a repeat delete a **204 no-op** (not an error), and **audit-log** who deleted what, so the action can't be silently denied later. ⚠️

At least one row needing attention, as asked: **metering** (a per-user delete limit) and **slug input validation** are the two that don't come free with `auth.verify` — they're the endpoint-specific work.

</details>

> **Exercise 4 — Build it for real.** If you're running Cortex locally (Chapter 13), implement this end-to-end: a Liquibase changeset (Part 2), the Tapir endpoint, the handler, and the client call. Verify anonymous gets 401 and two users can't see each other's bookmarks.

<details>
<summary><strong>Answer</strong></summary>

This is the hands-on capstone, so the "answer" is the **blueprint plus the two acceptance checks** that prove it's safe. Assemble the four pieces already specified in this chapter:

1. **Liquibase changeset (Part 2):** the `CREATE TABLE bookmarks` from §3 — `PRIMARY KEY (sub, book, chapter)` plus the `bookmarks_by_user` index. Ownership and idempotency are baked into the schema.
2. **Tapir endpoint (§4):** `POST`/`GET` (and the Exercise-1 `DELETE`) with `securityIn(auth.bearer[String]())` so a handler **cannot run without a verified token**.
3. **Handler (§4–§5):** `serverSecurityLogic(auth.verify)` then write/read keyed **only on `claims.sub`** — no `userId` from the request. Slot it beside `submitSolution` in `ApiRoutes.scala`.
4. **Client call (§4):** `addBookmark(token, …)` attaching the bearer header iff signed in (Chapter 18's pattern).

**Verify — the two checks are the deliverable, not the code:**

- **Anonymous → 401.** `curl -X POST localhost:8080/api/me/bookmarks` with **no `Authorization` header** must return **401** (the `serverSecurityLogic(auth.verify)` rejects the missing token). If it succeeds, your endpoint is `verifyOptional` by mistake — fix it.
- **Cross-user isolation.** Sign in as two GitHub identities (two `sub`s), have each create a bookmark, then `GET /api/me/bookmarks` as each. **Each sees only their own**, and a `DELETE` of the other's `book/chapter` deletes **zero rows**. That proves ownership-by-`sub` holds end-to-end.

If both checks pass, you've reproduced ADR-0013's gate on a brand-new endpoint: required auth, per-user ownership by construction — production-shaped, not just "works."

</details>

```quiz
{
  "prompt": "In a protected endpoint, where should the handler get the user id it writes data for?",
  "input": "Choose one:",
  "options": [
    "From the verified token's `sub` claim — never from a request parameter",
    "From a `user_id` field in the request body",
    "From the URL path",
    "From an unauthenticated header the client sets"
  ],
  "answer": "From the verified token's `sub` claim — never from a request parameter"
}
```

## In the Wild

- **[Cortex `ApiRoutes.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/http/ApiRoutes.scala)** — the real route wiring; your endpoint slots in beside submissions.
- **[Tapir — Authentication & secure endpoints](https://tapir.softwaremill.com/en/latest/endpoint/security.html)** — the `securityIn` / `serverSecurityLogic` pattern used above.
- **[OWASP — Broken Object Level Authorization (API1)](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)** — the #1 API risk, which "derive the user from the token, not the request" defeats by construction.

---

**Next:** the final chapter — sharpen your instincts on real-world OAuth breaches, and the canonical RFCs to keep on your shelf. → [23. Threat-model practice & In the Wild](/cortex/production-engineering/iam-keycloak-oauth/capstone/threat-model-and-in-the-wild)
