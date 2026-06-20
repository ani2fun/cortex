---
title: '2. The password anti-pattern'
summary: Why handing one app your password for another app is a catastrophe — and how "delegated authorization" was invented to kill the practice. This is the problem OAuth exists to solve.
---

# 2. The password anti-pattern

## TL;DR

> Once upon a time, apps that wanted to act on your behalf asked for your *password to another service*. "Give us your Gmail password and we'll find which of your contacts already use our app." This is the **password anti-pattern**, and it is a disaster: it hands over total, permanent, unscoped access and is impossible to revoke without changing your password everywhere. The fix — **delegated authorization** — lets you grant a *narrow, revocable* permission without ever revealing your password. That fix is OAuth, and this chapter is the "why" behind it.

## 1. Motivation

Rewind to the late 2000s. Social networks were exploding, and every new app had the same growth hustle: *"find your friends who are already here."* The easiest way to do that was to read your existing email contacts. So the signup form had a second step that, in hindsight, is jaw-dropping:

> **Step 2 of 3:** Enter your **Gmail address and password** so we can find your friends!

And millions of people did. They typed their Google password into Yelp, into LinkedIn, into a dozen startups. Each of those companies now had — sitting in their database, or at least passing through their servers — the *plaintext password to your email account*. The account that can reset every *other* password you own.

Think about everything that's wrong with this:

- **The app sees your password.** Even if it's well-meaning, it now stores or logs the keys to your entire digital life.
- **Access is total.** You wanted "read my contacts." You *gave* "read and send all my email, read my calendar, delete my account."
- **Access is permanent.** It works until you change your Google password — which also breaks every legitimate device you own.
- **You can't tell who has it.** Five apps have your password. One gets breached. *Which one?* You have no idea, and no way to revoke just that one.

This was untenable, and the industry knew it. The question that consumed a lot of smart people around 2007 was: *how can I let App A do one specific thing with my data on Service B, without giving App A my Service B password?*

The answer they converged on is the foundation of modern web identity. It's called **delegated authorization**, and the protocol is **OAuth**.

## 2. Intuition (Analogy)

You own a car and you're checking into a fancy hotel. You need the valet to park it. Do you hand them the key that:

- starts the car, **and**
- unlocks the glovebox where your documents are, **and**
- opens your house (it's on the same keyring), **and**
- works forever, even after you drive away?

Of course not. You hand them a **valet key**. A valet key is a deliberately *limited* credential: it starts the engine and drives a few miles, but it won't open the glovebox or the trunk, and the hotel can deactivate it the moment you leave.

That is exactly the idea behind delegated authorization.

| Valet parking | Delegated authorization (OAuth) |
|---|---|
| Your master key | Your password — total, permanent power |
| The valet key | An **access token** — narrow and revocable |
| "Drives, but no glovebox" | **Scopes** — "read contacts" but not "send mail" |
| The valet never copies your master key | The app never sees your password |
| The hotel can deactivate the valet key | You can revoke the token without changing your password |
| The valet key expires when you check out | Tokens **expire** on a timer |

The whole trick is to introduce a *third party you both trust* — the hotel's key system — that can mint limited keys on your behalf. In OAuth, that trusted third party is called the **authorization server**. (For Cortex, it's Keycloak.)

## 3. Formal Definition

**Delegated authorization** is a pattern in which a **resource owner** (you) grants a **client** (some app) limited access to resources held by a **resource server** (a service), *without* sharing the resource owner's credentials, by having a trusted **authorization server** issue a scoped, time-limited **access token**.

Pull that apart:

- The app never receives your password. It receives a **token**.
- The token is **scoped**: it carries exactly the permissions you consented to, and no more.
- The token is **time-limited**: it expires, so a leak is a temporary problem, not a permanent one.
- The token is **revocable**: the authorization server can kill it without touching your password.
- You **consent** explicitly: a screen tells you *who* wants *what*, and you approve or deny.

Hold onto that paragraph. The next five chapters are, essentially, "how do we actually build a system that has all five of those properties, securely, across untrusted networks?" — and the answer is OAuth 2.0.

> **A crucial distinction.** OAuth was designed for **authorization** (delegating access to *resources*), *not* for **authentication** (proving *who you are*). People immediately started abusing it for login ("Log in with Google"), which mostly worked but had subtle security holes. The proper "login" layer built *on top of* OAuth is **OpenID Connect** — the subject of Part 3's third group. For now, just file away: *OAuth delegates access; OIDC proves identity; they are different jobs done by related machinery.*

## 4. Worked Example

Here is the difference, side by side, for a concrete request: *an app wants to show you which of your contacts already use it.*

```d2
direction: down

bad: "❌ The anti-pattern" {
  you1: You {shape: person}
  app1: "Friend-Finder app" {shape: rectangle}
  google1: Google {shape: cylinder}
  you1 -> app1: "here is my Google PASSWORD"
  app1 -> google1: "log in as the user\n(full access, forever)"
}

good: "✓ Delegated authorization" {
  you2: You {shape: person}
  app2: "Friend-Finder app" {shape: rectangle}
  authz: "Authorization server\n(Google / Keycloak)" {shape: hexagon}
  google2: "Contacts API" {shape: cylinder}
  you2 -> authz: "I approve: 'read contacts' for this app"
  authz -> app2: "scoped token (read-only, expires)"
  app2 -> google2: "GET /contacts  (token, not password)"
}
```

In the bad version, the app holds your password and can do *anything, forever*. In the good version, the app holds a token that says *"may read contacts, expires in one hour"* — and your password never left Google. If Friend-Finder gets breached tomorrow, the attacker gets a soon-to-expire read-only contacts token, not the keys to your kingdom.

## 5. Build It

Let's *feel* the difference in revocability. Run this — it models "what can an attacker do if they steal what the app stored?"

```python run
import time

# ---- Scenario A: the app stored your PASSWORD (the anti-pattern) ----
stored_password = "my-super-secret-google-password"

def attacker_with_password(secret):
    # A password has no scope and no expiry. It can do everything, forever.
    return [
        "read all email",
        "send email as you",
        "read calendar",
        "reset your other passwords",
        "delete the account",
    ]

# ---- Scenario B: the app stored a scoped, expiring TOKEN ----
token = {
    "scopes": ["contacts:read"],
    "expires_at": time.time() + 3600,   # one hour from now
}

def attacker_with_token(tok):
    if time.time() > tok["expires_at"]:
        return ["(nothing — token expired)"]
    # The token can ONLY do what its scopes allow.
    allowed = []
    if "contacts:read" in tok["scopes"]:
        allowed.append("read contacts")
    return allowed

print("If they steal the stored PASSWORD, the attacker can:")
for cap in attacker_with_password(stored_password):
    print(f"   - {cap}")

print("\nIf they steal the stored TOKEN, the attacker can:")
for cap in attacker_with_token(token):
    print(f"   - {cap}")
```

**Now break it.** Change `"expires_at": time.time() + 3600` to `time.time() - 1` (already expired) and re-run. The stolen token now grants *nothing*. There is no equivalent line you can add to make a stolen password safe — that's the whole point. Scope and expiry are properties a password can never have.

## 6. Trade-offs & Complexity

Delegated authorization is strictly safer than the anti-pattern, but it isn't free:

| | Password anti-pattern | Delegated authorization (OAuth) |
|---|---|---|
| Security | Catastrophic | Strong |
| App complexity | Trivial (just store a password) | Real — redirects, token exchange, refresh |
| Moving parts | One | Four roles + a multi-step dance |
| Revocation | Impossible without a password change | One API call |
| User experience | One form | A redirect to the provider and back |

The cost is **protocol complexity**: instead of one form field, you now have a multi-step "dance" of redirects and token exchanges between four parties across the network. The next three chapters exist precisely to make that dance feel simple — because every step in it is the answer to *"how do we keep this secure when the network and the client can't be fully trusted?"*

## 7. Edge Cases & Failure Modes

- **"Log in with Google" ≠ OAuth-for-identity done right.** Using a bare access token as proof of *who you are* is the mistake OIDC fixes. (Group 3.)
- **Over-broad scopes.** An app that requests `mail.read` when it only needs `contacts.read` is re-creating the anti-pattern in spirit. Least privilege applies to scopes too.
- **Long-lived tokens that never expire.** A token that lives forever is, security-wise, a password with extra steps. Short access tokens + refresh tokens (Group 2) are the fix.
- **Storing tokens carelessly.** A token is a bearer credential — *whoever holds it, is it*. Leak it in a URL, a log, or `localStorage` exposed to XSS, and you've handed over the valet key.

## 8. Practice

> **Exercise 1 — Name the five properties.** Without scrolling up, list the five properties a delegated-authorization token has that a shared password does not. (Scope, expiry, revocability, no-credential-exposure, explicit consent.)

<details>
<summary><strong>Answer</strong></summary>

The five properties from §3 — and the *reason* each one matters is that a password has the opposite:

- **Scope** — the token carries exactly the permissions you consented to (`contacts:read`), not "everything"; a password is unscoped by nature.
- **Expiry** — the token dies on a timer, so a leak is *temporary*; a password works until you change it everywhere.
- **Revocability** — the authorization server can kill the token with one API call without touching your password; you cannot "revoke" a password you've shared except by changing it.
- **No credential exposure** — the app only ever holds a *token*, never your password, so a breach of the app can't reveal the keys to your whole account.
- **Explicit consent** — a screen showed you *who* wanted *what* and you approved it; typing your password into a form grants blanket access with no itemized agreement.

The unifying idea: each property is a way of making access **narrow, temporary, and under your control** — none of which a shared secret can ever be.

</details>

> **Exercise 2 — Spot the anti-pattern in the wild.** Find a real app or device that still asks for a third-party password (hint: some email clients, some "account aggregator" finance apps, some smart-home integrations). Write one paragraph on what damage a breach of that app would do.

<details>
<summary><strong>Answer</strong></summary>

Take the classic example: a personal-finance aggregator that asks for your *online-banking username and password* to "sync your transactions" (the practice that pre-dated open-banking APIs). Because it holds your actual bank credential, that credential is **unscoped, non-expiring, and exposed**: the app wanted read-only transaction history, but the password it stores can do *everything* you can do — view balances, initiate transfers, change your address, set up payees. If the aggregator is breached, the attacker doesn't get a soon-to-expire read token scoped to "transactions"; they get the **plaintext keys to your bank account**, working until you notice and change the password (which also breaks every other service you gave it to). You also **can't tell which** aggregator leaked, and there's **no consent record** scoping what each one may do. That is every failure from §1 at once — the precise reason open-banking moved to OAuth-style scoped, revocable tokens.

</details>

> **Exercise 3 — Scope a request.** A calendar app wants to "add the events I book to my Google Calendar." Which *single* scope should it request, and which scopes would be over-reach? Why does asking for less make users *more* likely to click "Allow"?

<details>
<summary><strong>Answer</strong></summary>

The job is *write events*, so it should request the single **write/events scope** — Google's `https://www.googleapis.com/auth/calendar.events` (write access to events), and nothing more. Anything broader is over-reach:

- **Full-calendar read/write** (`.../auth/calendar`) — lets it read and delete *all* your existing events and other calendars; far more than "add the events I book."
- **Contacts, Gmail, Drive scopes** — completely unrelated to calendaring; requesting them is the anti-pattern in spirit (§7, "over-broad scopes"), grabbing power "just in case."

Asking for less makes users **more** likely to click *Allow* for two reinforcing reasons: (1) the consent screen lists exactly what's requested, so a narrow ask reads as "this app only touches my calendar events," which is easy to trust; a broad ask reads as "this app wants my whole Google account," which triggers hesitation. (2) Least privilege is honest — it matches the app's real need, so there's nothing alarming to second-guess. Least privilege is therefore not just safer (§7) but better conversion: the smaller the request, the lower the friction to grant it.

</details>

```quiz
{
  "prompt": "What is the single most important thing OAuth gives you that typing your password into a third-party app does not?",
  "input": "Choose one:",
  "options": [
    "A scoped, revocable, expiring grant that never exposes your password",
    "Faster login",
    "A nicer-looking consent screen",
    "Encryption of the password in transit"
  ],
  "answer": "A scoped, revocable, expiring grant that never exposes your password"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[The OAuth 2.0 Authorization Framework — RFC 6749, §1](https://datatracker.ietf.org/doc/html/rfc6749#section-1)** — the introduction literally opens by describing the password anti-pattern as the problem to solve. Reading the first two pages is genuinely enlightening.
- **["My Favorite User Experience" — the history of OAuth](https://oauth.net/about/introduction/)** — oauth.net's own short origin story.
- **[Google's "Less secure apps" shutdown](https://support.google.com/accounts/answer/6010255)** — Google spent years killing password-based access to Gmail in favor of OAuth. This is the anti-pattern being deliberately retired at planetary scale.

---

**Next:** before we can hand out tokens, we need to understand a humbler problem: HTTP forgets you the instant you stop talking to it. How does a server remember you're logged in at all? → [3. How a server remembers you: sessions, cookies, tokens](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/sessions-cookies-tokens)
