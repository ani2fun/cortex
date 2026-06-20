---
title: '12. The Keycloak mental model: realms, clients, users, roles'
summary: Keycloak implements everything in the last eight chapters so you don't have to. Its whole world is four nested ideas — realms, clients, users, roles — and once they click, the admin console stops being intimidating.
---

# 12. The Keycloak mental model: realms, clients, users, roles

## TL;DR

> **Keycloak** is an open-source Authorization Server (and Identity Provider) that implements OAuth 2.0 and OIDC — the whole protocol you just derived — as a product you run. Its model is four nested concepts: a **realm** is an isolated universe of identity; inside it, **clients** are the apps that ask for tokens, **users** are the people who log in, and **roles** are the permissions you assign. Cortex runs Keycloak as its identity plane. Explore the real `cortex` realm below, then we'll stand it up for ourselves.

## 1. Motivation

You *could* implement everything in Chapters 4–11 yourself: an `/authorize` endpoint, a `/token` endpoint, PKCE validation, RS256 signing, a JWKS publisher, key rotation, a login UI, password hashing, MFA, social login, an admin API… That is months of security-critical work where every bug is a breach.

Or you run **Keycloak**, which has done all of it, battle-tested, for over a decade. You get the standards-compliant endpoints, the login screens, the token signing and JWKS publishing, social/enterprise federation, and an admin console — for free. Your job shrinks to *configuration*: describe your apps and users, and Keycloak runs the protocol.

The catch is that Keycloak's admin console is vast, and it's easy to drown. The cure is a clear **mental model** — four ideas that organize everything else. Get those, and every screen in the console becomes "oh, this is just configuring a *client*," or "this is a *role*."

## 2. Intuition (Analogy)

Keycloak is an **office building's security system.**

- A **realm** is the *entire building's* security domain — its own set of tenants, its own keycards, its own front desk. A second building (realm) is completely separate: a keycard from one never opens a door in the other. (Cortex's local dev uses a realm called `cortex`; production uses `apps-prod`.)
- A **client** is a *door system* that wants to let people in — a specific app integrated with security. The Cortex SPA is a client; a future mobile app would be another client. Each is configured for how it talks to the security desk (public vs confidential, which redirect URIs, PKCE on/off).
- A **user** is a *person with a profile* in the building directory — credentials, email, name. The user `tester` lives in the `cortex` realm.
- A **role** is a *clearance level* you stamp on users — "Submitter," "Admin." Doors (clients/APIs) decide what each clearance may do.

| Building security | Keycloak |
|---|---|
| The whole building's security domain | **Realm** |
| A door/app integrated with security | **Client** |
| A person in the directory | **User** |
| A clearance level | **Role** |
| The front desk that issues keycards | The realm's token endpoints |

The nesting matters: *everything lives inside a realm.* Clients, users, roles, keys, login themes — all scoped to one realm. That isolation is why one Keycloak can safely serve many unrelated apps: give each its own realm (or share a realm if they share users).

## 3. Formal Definition

| Concept | Definition | In Cortex |
|---|---|---|
| **Realm** | An isolated namespace of users, clients, roles, and signing keys. Cross-realm, nothing is shared. | `cortex` (dev), `apps-prod` (prod). |
| **Client** | An application registered to obtain tokens. Has a type (**public**/confidential), allowed **redirect URIs**, **web origins** (CORS), flow settings, and PKCE config. | `cortex-web` — a public client. |
| **User** | An identity that can authenticate: credentials, profile attributes, group/role memberships. May be **local** or **federated** (from GitHub, LDAP…). | `tester` (local dev); real users come from GitHub. |
| **Role** | A named permission, assignable to users (directly or via groups). **Realm roles** are global; **client roles** are scoped to one client. | `submitter` (may submit solutions). |
| **Identity Provider (IdP)** | An *external* source of users Keycloak brokers to (GitHub, Google, SAML…). | GitHub, in `apps-prod`. |
| **Protocol mapper** | A rule that injects data (roles, attributes) into the tokens Keycloak mints. | Puts roles into `realm_access.roles`. |

> **The realm is the unit of isolation; the client is the unit of integration; the user is the unit of identity; the role is the unit of permission.** Hold those four sentences and the console organizes itself.

## 4. Worked Example — explore the real `cortex` realm

Below is Cortex's actual local-development realm (from [`cortex-realm.json`](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)), rendered as an interactive tree. **Click the realm, then each client, user, and role** to see its configuration and what each field does. This is the same structure you'll configure in the admin console — and the same JSON Keycloak imports on startup (Chapter 13).

```d3 widget=keycloak-realm-explorer
{
  "title": "Cortex's identity model — click any node",
  "realm": {
    "name": "cortex",
    "note": "A realm is a self-contained universe of identity. Everything below — clients, users, roles — lives only inside this realm. Production uses a separate realm, 'apps-prod'.",
    "fields": [
      { "key": "realm", "value": "cortex" },
      { "key": "enabled", "value": "true" },
      { "key": "displayName", "value": "Cortex (local)" },
      { "key": "sslRequired", "value": "none  (dev only — prod requires HTTPS)" }
    ],
    "groups": [
      {
        "id": "clients",
        "label": "Clients (apps)",
        "items": [
          {
            "id": "cortex-web",
            "label": "cortex-web",
            "badge": "public · PKCE",
            "note": "The Cortex SPA. A PUBLIC client: it runs in your browser and can keep no secret, so it uses PKCE (S256) instead of a client_secret. standardFlow = Authorization Code flow.",
            "fields": [
              { "key": "protocol", "value": "openid-connect" },
              { "key": "publicClient", "value": "true" },
              { "key": "standardFlowEnabled", "value": "true  (Authorization Code)" },
              { "key": "pkce.code.challenge.method", "value": "S256" },
              { "key": "redirectUris", "value": "http://localhost:5173/*, http://localhost:8080/*" },
              { "key": "webOrigins", "value": "http://localhost:5173, http://localhost:8080" }
            ]
          }
        ]
      },
      {
        "id": "users",
        "label": "Users",
        "items": [
          {
            "id": "tester",
            "label": "tester",
            "badge": "local dev user",
            "note": "A local username/password account, handy for development so you don't need GitHub. In production, users arrive by logging in with GitHub (brokered identity).",
            "fields": [
              { "key": "username", "value": "tester" },
              { "key": "email", "value": "tester@cortex.local" },
              { "key": "firstName", "value": "Cortex" },
              { "key": "lastName", "value": "Tester" },
              { "key": "credentials", "value": "password: tester (non-temporary)" }
            ]
          }
        ]
      },
      {
        "id": "roles",
        "label": "Realm roles",
        "items": [
          {
            "id": "default-roles",
            "label": "default-roles-cortex",
            "badge": "default",
            "note": "Composite role every user gets automatically — grants the baseline scopes (view profile, etc.).",
            "fields": [
              { "key": "type", "value": "realm role (composite)" },
              { "key": "assignedTo", "value": "every user by default" }
            ]
          },
          {
            "id": "submitter",
            "label": "submitter",
            "badge": "permission",
            "note": "An example application role. A protocol mapper puts it into realm_access.roles, and the Cortex API could check for it before accepting a solution submission.",
            "fields": [
              { "key": "type", "value": "realm role" },
              { "key": "appears in token", "value": "realm_access.roles" }
            ]
          }
        ]
      },
      {
        "id": "idps",
        "label": "Identity providers",
        "items": [
          {
            "id": "github",
            "label": "GitHub",
            "badge": "brokered (prod)",
            "note": "In the apps-prod realm, Keycloak brokers login to GitHub: you authenticate with GitHub, Keycloak creates/links a local user and mints Cortex tokens. Chapter 15 covers this.",
            "fields": [
              { "key": "alias", "value": "github" },
              { "key": "providerId", "value": "github (OAuth2)" },
              { "key": "used by", "value": "idpHint=github on the login redirect" }
            ]
          }
        ]
      }
    ]
  }
}
```

> Notice how the four concepts nest: the realm contains clients, users, roles, and IdPs. Click `cortex-web` and you're looking at the exact settings that made the PKCE flow in Chapter 6 work — `publicClient: true` and `pkce.code.challenge.method: S256`. Click `submitter` and you're looking at the role that rides inside a token's `realm_access.roles` (Chapter 10's decoded sample had exactly that).

## 5. Build It

You'll stand up the real thing in the next chapter. For now, run this — it models the four concepts as nested data and answers "can this user do this at this client?", which is the question Keycloak + your API answer together.

```python run
# A tiny in-memory "realm" — the shape Keycloak stores, minus 99% of the features.
realm = {
    "name": "cortex",
    "clients": {
        "cortex-web": {"public": True, "pkce": "S256",
                       "redirect_uris": ["http://localhost:5173/*"]},
    },
    "users": {
        "tester": {"password": "tester", "roles": {"default", "submitter"}},
        "guest":  {"password": "guest",  "roles": {"default"}},
    },
}

def login(realm, client_id, username, password, redirect_uri):
    client = realm["clients"].get(client_id)
    if client is None:
        return "unknown_client"
    # Exact redirect-uri allow-list check (Chapter 6's redirect rule).
    if not any(redirect_uri.startswith(u.rstrip("*")) for u in client["redirect_uris"]):
        return "invalid_redirect_uri"
    user = realm["users"].get(username)
    if user is None or user["password"] != password:
        return "invalid_credentials"
    # Keycloak would mint a token here, embedding the user's roles.
    return f"token for {username}; realm_access.roles = {sorted(user['roles'])}"

print(login(realm, "cortex-web", "tester", "tester", "http://localhost:5173/cb"))
print(login(realm, "cortex-web", "guest",  "guest",  "http://localhost:5173/cb"))
print(login(realm, "cortex-web", "tester", "tester", "http://evil.example/cb"))  # blocked
print(login(realm, "unknown",    "tester", "tester", "http://localhost:5173/cb"))
```

**Now break it.** Add a new client `"cortex-mobile"` with its own redirect URIs and log a user in through it. Notice the *user* and their *roles* didn't change — only the *client* did. That separation (one user, many clients, shared roles) is exactly why an Authorization Server is worth running: identity defined once, reused everywhere.

## 6. Trade-offs & Complexity

| Run Keycloak | Build your own AS |
|---|---|
| Standards-compliant endpoints out of the box | Months of security-critical work |
| Social/enterprise federation, MFA, themes included | Each feature built and audited by you |
| One place for identity across many apps | Reinvented per app, inconsistently |
| Operational cost: a service to run + learn | "Full control" you mostly don't want |

The trade is **operational complexity for security correctness**. You take on running (or hosting) Keycloak and learning its model — in exchange for not hand-rolling cryptographic protocols. For anything beyond a toy, that's an easy trade. (Operating Keycloak on Kubernetes is its own topic — see [Homelab from Scratch → Keycloak as the identity plane](/cortex/homelab-from-scratch/stateful-services/keycloak-as-the-identity-plane).)

## 7. Edge Cases & Failure Modes

- **One realm to rule them all.** Dumping unrelated apps and users into a single realm couples them. Use separate realms for separate trust domains; share a realm only when apps genuinely share users.
- **Realm roles vs client roles.** Realm roles are global; client roles are scoped to one client. Mixing them up leads to over- or under-granting. Start with realm roles unless you need per-client scoping.
- **Confusing users with clients.** A user is a *person*; a client is an *app*. A backend service is a *client* (often confidential), not a user — don't model a service as a fake human.
- **Editing prod in the console with no source of truth.** Click-ops in the admin console drifts from any committed config. Cortex imports its realm from JSON (next chapter) so the realm is *version-controlled*.

## 8. Practice

> **Exercise 1 — Place the concept.** For each, name realm / client / user / role: (a) `cortex-web`; (b) `tester`; (c) `submitter`; (d) `apps-prod`.

<details>
<summary><strong>Answer</strong></summary>

Matching each to the four nested concepts (*realm = isolation, client = integration, user = identity, role = permission*):

- **(a) `cortex-web` → client.** It's an *app* registered to obtain tokens (the Cortex SPA) — the unit of integration.
- **(b) `tester` → user.** It's a *person* who can authenticate (a local dev account with a password) — the unit of identity.
- **(c) `submitter` → role.** It's a named *permission* you stamp on users (may submit solutions) — the unit of permission.
- **(d) `apps-prod` → realm.** It's an *isolated universe of identity* (Cortex's production realm) — the unit of isolation; nothing inside it is shared with the `cortex` dev realm.

The tell each time is the question the thing answers: *which app?* (client), *which person?* (user), *may they do what?* (role), *which separate world?* (realm).

</details>

> **Exercise 2 — Design a second app.** Cortex wants to add an admin dashboard as a separate app. Should it be a new *client* in the same realm, or a new *realm*? Justify it in terms of shared users and isolation.

<details>
<summary><strong>Answer</strong></summary>

Make it a **new client in the same realm** — *not* a new realm.

The deciding question is always: **do these apps share users?** The admin dashboard is administered by the *same people* who already exist in the Cortex realm — you want one identity, signed in once, usable across both the SPA and the dashboard, with the *same* `sub` and the *same* roles deciding who's an admin. That's exactly the "one user, many clients, shared roles" separation the chapter calls the whole point of running an Authorization Server: identity defined once, reused everywhere. A second *client* gets its own redirect URIs, web origins, and flow settings while reusing the realm's users, roles, and signing keys.

A new *realm* is the wrong tool because a realm is the **unit of isolation** — across realms, *nothing* is shared: not users, not roles, not keys. You'd be forcing admins to maintain a *separate* account, re-logging into a disconnected universe, with no shared roles. Reach for a separate realm only for a genuinely separate *trust domain* (unrelated app, unrelated users) — which an in-house admin dashboard is not.

</details>

> **Exercise 3 — Read the explorer.** Using the interactive realm above, list the three settings on `cortex-web` that, together, make the Chapter 6 PKCE flow possible. (Hint: client type, flow, PKCE method.)

<details>
<summary><strong>Answer</strong></summary>

Three settings on `cortex-web`, working together:

- **`publicClient: true`** — the *client type*. The SPA runs in your browser and can keep no secret, so it's declared public. This is the honest admission that *forces* the next two settings to carry the security a secret normally would.
- **`standardFlowEnabled: true`** — the *flow*. This turns on the **Authorization Code flow** (`response_type=code`), the correct, secure flow for a browser app.
- **`pkce.code.challenge.method: S256`** — the *PKCE method*. Requires PKCE with SHA-256, which defeats authorization-code interception — the exact defense that makes a *public* (secret-less) client safe.

Why all three are needed: a public client without PKCE *is* the vulnerable variant from Chapter 6 — it has dropped its secret but added nothing to replace it. `publicClient: true` says "I have no secret," `standardFlowEnabled` picks the code flow, and `S256` is what plugs the resulting hole. Drop the third and the client is exposed; together they give a browser app the safety a confidential client gets from its secret.

</details>

```quiz
{
  "prompt": "In Keycloak, what is the unit of ISOLATION — the boundary across which users, clients, roles, and signing keys are NOT shared?",
  "input": "Choose one:",
  "options": [
    "The realm",
    "The client",
    "The role",
    "The user"
  ],
  "answer": "The realm"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Keycloak — Core concepts and terms](https://www.keycloak.org/docs/latest/server_admin/#core-concepts-and-terms)** — the official glossary of realms, clients, users, roles, and more.
- **[Keycloak — Getting started](https://www.keycloak.org/getting-started/getting-started-docker)** — spin up Keycloak and create a realm/client/user in a few minutes; pairs with the next chapter.
- **[`cortex-realm.json`](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)** — the real realm rendered in the explorer above, as version-controlled JSON.

---

**Next:** enough theory — let's run Keycloak on our own machine, import the `cortex` realm, and log in as `tester`. → [13. Run Keycloak locally: docker-compose + realm import](/cortex/production-engineering/iam-keycloak-oauth/keycloak/run-keycloak-locally)
