---
title: '13. Run Keycloak locally: docker-compose + realm import'
summary: Theory becomes muscle memory when you run the real thing. Bring up Keycloak with one command, have it import Cortex's realm on boot, and log in as `tester`.
---

# 13. Run Keycloak locally: docker-compose + realm import

## TL;DR

> Cortex ships a `docker-compose.yml` that runs Keycloak in **dev mode** and **imports the `cortex` realm from JSON on startup** — so `docker compose up keycloak` gives you a fully-configured Authorization Server with the `cortex-web` client and a `tester` user, every time, with no clicking. Importing the realm from a version-controlled file (rather than configuring by hand) is the single most important operational habit in this chapter: your identity config becomes code.

## 1. Motivation

You can read about realms and clients forever, but it doesn't *stick* until you watch Keycloak boot, log in, and see a real token come back. The friction is setup: a hand-configured Keycloak is a pile of clicks you can't reproduce and a teammate can't share.

Cortex solves this the way it solves everything — declaratively. The realm lives in a JSON file in the repo; Keycloak reads it on startup with `--import-realm`. Bring the container up and the realm, the `cortex-web` client (PKCE and all), and the `tester` user simply *exist*. Tear it down, bring it up — identical. That reproducibility is what makes local auth development bearable, and it's the same idea you'll see scaled up to production GitOps in Part 4.

## 2. Intuition (Analogy)

Two ways to set up a new office's security system:

- **Click-ops:** a technician walks in and manually programs every keycard reader, enters every employee, sets every clearance — by hand, from memory. Works once. Now do it identically for the branch office, and again after a reset. Good luck.
- **Import from blueprint:** you hand the system a *configuration file* — "here are the doors, here are the people, here are the clearances" — and it programs itself, identically, every time.

`--import-realm` is the blueprint. The realm JSON is the blueprint file. You never *click* Cortex's identity into existence; you *declare* it once and import it forever.

## 3. Formal Definition

Keycloak can be configured three ways: clicking in the **admin console**, calling the **admin REST API**, or **importing a realm** from a JSON file at startup. Cortex uses import:

- **`start-dev`** runs Keycloak in development mode — HTTP allowed, in-memory or simple storage, relaxed hostname checks. *Never* in production (Part 4 uses `start` with a Postgres store and strict hostname).
- **`--import-realm`** tells Keycloak, on boot, to import any realm JSON found in `/opt/keycloak/data/import`. Existing realms aren't overwritten by default — it's a "create if absent" on first boot.
- The **realm JSON** (`cortex-realm.json`) is a full export: realm settings, clients, users, roles, identity providers — everything the explorer in Chapter 12 showed.

## 4. Worked Example — the real compose service

Here's the actual Keycloak service from Cortex's [`docker-compose.yml`](https://github.com/ani2fun/cortex/blob/main/docker-compose.yml):

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  command: ["start-dev", "--import-realm"]
  environment:
    KC_BOOTSTRAP_ADMIN_USERNAME: admin
    KC_BOOTSTRAP_ADMIN_PASSWORD: admin
  volumes:
    - ./docker/keycloak/import:/opt/keycloak/data/import:ro   # the realm JSON, mounted read-only
  ports:
    - "8081:8080"        # Keycloak on http://localhost:8081
```

Read it line by line:

- **`image: …/keycloak:26.0`** — the Keycloak version (Cortex pins it; production runs 26.5.x).
- **`command: ["start-dev", "--import-realm"]`** — dev mode *and* import any realm in the mounted folder.
- **`KC_BOOTSTRAP_ADMIN_*`** — the master admin account for the admin console at `http://localhost:8081` (dev only; production uses secrets).
- **`volumes: … /opt/keycloak/data/import:ro`** — mounts the repo's `docker/keycloak/import/` (containing `cortex-realm.json`) into the import directory, read-only.
- **`ports: "8081:8080"`** — Keycloak listens on 8080 inside the container, exposed as 8081 on your machine.

Bring it up and log in:

```text
# Start just Keycloak (or the whole stack with ./bin/dev)
docker compose up keycloak

# Then:
#  Admin console:  http://localhost:8081  (admin / admin)
#  The cortex realm, cortex-web client, and tester user are already there.
#  Discovery doc:  http://localhost:8081/realms/cortex/.well-known/openid-configuration
```

Open the discovery URL and you'll see the very endpoints from Chapter 9 — `authorization_endpoint`, `token_endpoint`, `jwks_uri` — now live on your laptop. The `tester` user (password `tester`) lets you complete a real login without GitHub.

## 5. Build It — drive the real token endpoint

With Keycloak running, you can obtain a real token from the command line and decode it with the Chapter 10 widget. This uses the password grant *purely for local testing* (never in a real app — Chapter 8):

```text
# Get a token as `tester` from the running local Keycloak:
curl -s http://localhost:8081/realms/cortex/protocol/openid-connect/token \
  -d grant_type=password \
  -d client_id=cortex-web \
  -d username=tester \
  -d password=tester \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])"

# Paste the printed JWT into the JWT-inspector widget in Chapter 10.
# You'll see iss=http://localhost:8081/realms/cortex, azp=cortex-web, preferred_username=tester.
```

Run the simulator below to rehearse what the import gives you, even without Docker handy:

```python run
# Models "what exists after --import-realm" so you know what to expect.
imported_realm = {
    "realm": "cortex",
    "clients": ["cortex-web (public, PKCE S256)"],
    "users":   ["tester / tester"],
    "endpoints": {
        "issuer":        "http://localhost:8081/realms/cortex",
        "authorization": ".../protocol/openid-connect/auth",
        "token":         ".../protocol/openid-connect/token",
        "jwks":          ".../protocol/openid-connect/certs",
    },
}
print("After `docker compose up keycloak`, you have:")
for k, v in imported_realm.items():
    print(f"  {k}: {v}")
print("\nNo clicking required — the realm JSON declared all of it.")
```

**Now break it.** Imagine deleting the `volumes:` line. Keycloak would still start — but the `cortex` realm, the `cortex-web` client, and `tester` would all be *gone*, and your SPA's login would fail with "realm not found." That one mounted file is the difference between reproducible auth and a pile of manual clicks.

## 6. Trade-offs & Complexity

| Realm import (declarative) | Admin-console click-ops |
|---|---|
| Reproducible, version-controlled, reviewable | One-off, undocumented, drifts |
| A teammate runs one command | A teammate re-clicks from a wiki |
| Diffable in a PR | Invisible until something breaks |
| Slightly more upfront (export the JSON) | "Faster" until you need it twice |

The only real cost of import is producing the JSON in the first place — and you can *export* it from a console you configured once (`kc.sh export`), then commit it. After that, declarative wins on every axis that matters in a team.

## 7. Edge Cases & Failure Modes

- **`start-dev` in production.** Dev mode disables HTTPS enforcement and hostname checks. It's for laptops only. Production uses `start` with a real database, `KC_HOSTNAME`, and TLS (Part 4).
- **Import doesn't overwrite.** On a second boot, `--import-realm` won't clobber an existing realm. If you change the JSON and want it applied, you may need to remove the realm (or use explicit override flags) — otherwise you'll wonder why your edit "didn't take."
- **Port confusion.** Keycloak is `8081` on the host (mapped from `8080` inside). The issuer URL the SPA and server use must match exactly, port included — a mismatch breaks `iss` verification (Chapter 11).
- **Bootstrap admin in the file.** Dev compose hard-codes `admin/admin`. Never do that in production; inject admin credentials via secrets.

## 8. Practice

> **Exercise 1 — Trace the mount.** Explain what happens, step by step, from `docker compose up keycloak` to the `cortex-web` client existing. Which line in the compose file makes the realm appear?

<details>
<summary><strong>Answer</strong></summary>

Step by step:

1. `docker compose up keycloak` starts the `quay.io/keycloak/keycloak:26.0` container.
2. The **`volumes:`** line mounts the repo's `./docker/keycloak/import/` (which contains `cortex-realm.json`) into the container at `/opt/keycloak/data/import`, read-only.
3. The **`command: ["start-dev", "--import-realm"]`** runs Keycloak in dev mode *and* tells it, on boot, to import any realm JSON it finds in that import directory.
4. Keycloak reads `cortex-realm.json` and creates the whole realm — the `cortex` realm, the `cortex-web` client (public, PKCE S256), the `tester` user, roles — exactly the structure the Chapter 12 explorer showed. (It's "create if absent": on a *first* boot the realm is created.)
5. Keycloak finishes starting and serves the realm at `http://localhost:8081` — `cortex-web` now exists, with no clicking.

**The line that makes the realm appear is the `volumes:` mount** — it's what delivers the realm JSON into the import directory. (The `--import-realm` flag is what *reads* it, but without the mount there's nothing to read — which is exactly the "Now break it" failure: delete the `volumes:` line and the realm, client, and user vanish.)

</details>

> **Exercise 2 — Find the endpoints.** With Keycloak running, open the local discovery document. Copy out the `token_endpoint` and `jwks_uri`. Which chapter's verifier consumes the `jwks_uri`?

<details>
<summary><strong>Answer</strong></summary>

Open `http://localhost:8081/realms/cortex/.well-known/openid-configuration` and you'll find:

- **`token_endpoint`** = `http://localhost:8081/realms/cortex/protocol/openid-connect/token` — where a client exchanges an authorization code (or, for local testing, a password grant) for tokens.
- **`jwks_uri`** = `http://localhost:8081/realms/cortex/protocol/openid-connect/certs` — where Keycloak publishes its *public* signing keys.

**The `jwks_uri` is consumed by Chapter 11's verifier** (JWKS & signature verification) — Cortex's `KeycloakAuthBackend` derives this exact URL from the issuer, fetches and caches the public keys, and uses them to check each token's RS256 signature *locally*, with no per-request call back to Keycloak. These are the same endpoints from Chapter 9's discovery doc, now live on your laptop.

</details>

> **Exercise 3 — Break and fix.** Predict what login error you'd see if the issuer in the SPA config said `localhost:8080` but Keycloak was actually on `8081`. Which verification step fails, and why?

<details>
<summary><strong>Answer</strong></summary>

The login breaks, and it breaks *before* any token is even verified: the SPA's OIDC library would try to reach Keycloak (discovery, `/authorize`, `/token`) at `http://localhost:8080`, where **nothing is listening** — Keycloak is on `8081`. So the first symptom is a connection failure / discovery error ("could not load `.well-known/openid-configuration`") and login never completes.

But the chapter's deeper point is the **`iss` verification step** from Chapter 11. The issuer must match *exactly*, **port included**. Even if you somehow obtained a token, the token Keycloak mints carries `iss: http://localhost:8081/realms/cortex` (its *real* address), while the verifier configured with `localhost:8080` expects `iss: http://localhost:8080/...`. The strings differ, so the `iss` check fails and the token is rejected — a valid signature from an issuer string you didn't expect is still wrong.

**The fix:** make the issuer/port in the SPA (and server) config match where Keycloak actually runs — `8081` — so the discovery URL resolves *and* the `iss` claim matches. Port confusion (host `8081` mapped from container `8080`) is the classic trap here.

</details>

```quiz
{
  "prompt": "What does `--import-realm` plus a mounted realm JSON give Cortex's local Keycloak?",
  "input": "Choose one:",
  "options": [
    "A reproducible, version-controlled realm (client + users + roles) created automatically on boot, no clicking",
    "Automatic HTTPS certificates",
    "A production-ready deployment",
    "A way to store user passwords in the browser"
  ],
  "answer": "A reproducible, version-controlled realm (client + users + roles) created automatically on boot, no clicking"
}
```

## In the Wild

- **[Keycloak — Importing and exporting realms](https://www.keycloak.org/server/importExport)** — the official guide to `--import-realm` and `kc.sh export`.
- **[Keycloak on Docker / `start-dev`](https://www.keycloak.org/getting-started/getting-started-docker)** — the dev-mode quickstart Cortex's compose file is built on.
- **[Cortex `docker-compose.yml`](https://github.com/ani2fun/cortex/blob/main/docker-compose.yml)** and **[`docker/keycloak/import/cortex-realm.json`](https://github.com/ani2fun/cortex/blob/main/docker/keycloak/import/cortex-realm.json)** — the real service and realm file from this chapter.

---

**Next:** the realm imported a client called `cortex-web`. Let's open it up and understand every setting that makes it a *safe public client* — and what each one defends against. → [14. A public PKCE client, dissected](/cortex/production-engineering/iam-keycloak-oauth/keycloak/a-public-pkce-client-dissected)
