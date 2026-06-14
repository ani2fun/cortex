# Part 3 — Identity & Access Management

> **Who are you, and what are you allowed to do?** Every system that holds something worth protecting has to answer those two questions, millions of times a day, in a few milliseconds, without ever being fooled. This Part builds the modern answer — OAuth 2.0, OpenID Connect, and Keycloak — from the ground up, and then shows you the exact machinery that runs when you click **Edit** on a code block anywhere on this site.

## The promise of this Part

By the end you will be able to:

- Explain, to a skeptical colleague, **why** apps stopped asking for your password — and what they ask for instead.
- Draw the **OAuth 2.0 Authorization Code flow** from memory, and say what each redirect is *for*.
- Explain **PKCE** as the answer to a specific attack, not as a magic incantation.
- **Decode a JWT by hand** and describe how a server trusts it without ever phoning the login server.
- Stand up **Keycloak**, model a realm, and wire an app to "Login with GitHub".
- Read Cortex's **real** auth code — client and server — and know what every line is defending against.

## How identity flows through Cortex

This is the system you'll understand completely by the end of the Part. Every box is a real, deployed component.

```d2
direction: right

user: "Reader" {shape: person}
spa: "Cortex SPA\n(public client)" {shape: rectangle}
api: "Cortex API\n(ZIO server)" {shape: rectangle}
kc: "Keycloak\n(Authorization Server)" {shape: hexagon}
gh: "GitHub\n(Identity Provider)" {shape: cloud}
jwks: "JWKS\n(public keys)" {shape: cylinder}

user -> spa: "reads & runs"
spa -> kc: "OAuth 2.0 + PKCE login"
kc -> gh: "brokers identity"
kc -> jwks: "publishes signing keys"
spa -> api: "Authorization: Bearer <token>"
api -> jwks: "verify signature (cached, offline)"
```

> Your browser holds a token. Keycloak issued it. The Cortex server verifies it — *without* calling Keycloak on every request. By the last chapter, that one sentence will feel obvious.

## Chapters

**The Problem of Identity** — *start from zero*
1. [Who are you, and why should I believe you?](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/who-are-you)
2. [The password anti-pattern](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/the-password-anti-pattern)
3. [How a server remembers you: sessions, cookies, tokens](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/sessions-cookies-tokens)

**OAuth 2.0 from First Principles**
4. [The four actors and the valet key](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/the-four-actors)
5. [The Authorization Code flow, step by step](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/authorization-code-flow)
6. [Why PKCE exists](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/why-pkce-exists)
7. [Access tokens, refresh tokens, scopes & consent](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/tokens-scopes-consent)
8. [The other grants — and the two you should never use](/cortex/production-engineering/iam-keycloak-oauth/oauth2-from-first-principles/the-other-grants)

**OpenID Connect: Identity on top of OAuth**
9. [OAuth authorizes; OIDC identifies](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/oauth-authorizes-oidc-identifies)
10. [Anatomy of a JWT, decoded by hand](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/anatomy-of-a-jwt)
11. [Trust without a phone call: JWKS & signature verification](/cortex/production-engineering/iam-keycloak-oauth/openid-connect/trust-without-a-phone-call)

**Keycloak: Batteries-Included Identity**
12. [The Keycloak mental model: realms, clients, users, roles](/cortex/production-engineering/iam-keycloak-oauth/keycloak/the-keycloak-mental-model)
13. [Run Keycloak locally: docker-compose + realm import](/cortex/production-engineering/iam-keycloak-oauth/keycloak/run-keycloak-locally)
14. [A public PKCE client, dissected](/cortex/production-engineering/iam-keycloak-oauth/keycloak/a-public-pkce-client-dissected)
15. ["Login with GitHub": identity brokering](/cortex/production-engineering/iam-keycloak-oauth/keycloak/login-with-github)
16. [Roles, groups & protocol mappers](/cortex/production-engineering/iam-keycloak-oauth/keycloak/roles-groups-mappers)

**Cortex's Real Integration**
17. [The code-edit auth gate (ADR-0013)](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/the-code-edit-auth-gate)
18. [Client side: keycloak-js, check-sso & silent refresh](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/client-side-keycloak-js)
19. [Server side: verifying a bearer token end-to-end](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/server-side-token-verification)
20. [Identity-aware rate limiting](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/identity-aware-rate-limiting)
21. [Hardening & failure modes](/cortex/production-engineering/iam-keycloak-oauth/cortex-integration/hardening-and-failure-modes)

**Capstone & Practice**
22. [Capstone: add a protected endpoint to Cortex](/cortex/production-engineering/iam-keycloak-oauth/capstone/add-a-protected-endpoint)
23. [Threat-model practice & In the Wild](/cortex/production-engineering/iam-keycloak-oauth/capstone/threat-model-and-in-the-wild)

---

> **A note on the demo.** Cortex deploys Keycloak as its identity plane. If you want to *operate* Keycloak as cluster infrastructure — running it on Kubernetes behind a real domain with a Postgres backing store — see [Homelab from Scratch → Keycloak as the identity plane](/cortex/homelab-from-scratch/stateful-services/keycloak-as-the-identity-plane). This Part is about *using* it from an application's point of view.

**Begin:** [1. Who are you, and why should I believe you? →](/cortex/production-engineering/iam-keycloak-oauth/the-problem-of-identity/who-are-you)
