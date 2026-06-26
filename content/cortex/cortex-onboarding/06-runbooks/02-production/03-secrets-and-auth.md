---
title: Secrets & auth
summary: How production secrets stay in git safely (Sealed Secrets), and how Keycloak gates the app and the coach.
---

## Secrets: sealed, in git

Production secrets live **in the `infra` git repo** — safely, because they're **Sealed Secrets**. A `SealedSecret` is encrypted with the cluster's public key and can *only* be decrypted by the in-cluster controller's private key, so it's safe to commit. Argo CD applies the `SealedSecret`; the controller unseals it into a real `Secret` the pod mounts.

The secrets each workload needs:

| Secret | Used by | Holds |
|---|---|---|
| `cortex-db` | cortex | the Postgres password (`DB_PASSWORD` ← `secretKeyRef`) |
| `cortex-tutor` sealed secret | cortex-tutor | `ANTHROPIC_API_KEY`, `MCP_SERVICE_TOKEN`, DB creds (`overlays/prod/sealedsecret.yaml`) |
| `keycloak-github-oauth` | Keycloak | the GitHub OAuth client secret (the IdP federation) |

To **add or rotate** a secret, seal the plaintext and commit the sealed output (the plaintext never touches git):

```bash
# produce a SealedSecret from a literal, scoped to its namespace
kubectl -n apps-prod create secret generic cortex-db \
  --from-literal=postgres-password='<new-password>' \
  --dry-run=client -o yaml \
| kubeseal --format yaml > deploy/apps/cortex/overlays/prod/sealedsecret.yaml
git add … && git commit && git push     # Argo CD applies it; the controller unseals it
```

> **The master key is the crown jewel.** The sealed-secrets controller's private key is what decrypts *everything*. It's backed up off-cluster (`scripts/dr/sealed-secrets-key-backup.sh`) and **restoring it is Layer 4 of the DR rebuild** — lose it and every committed `SealedSecret` becomes unrecoverable ciphertext. Guard that backup.

## Auth: one Keycloak, two services

Both Cortex and the Tutor validate JWTs against the **same** Keycloak realm — `apps-prod` at **keycloak.kakde.eu** — using the **`cortex-web`** public PKCE client. Signing into the site signs you into the coach; there is no second login.

- **Cortex (server):** `AUTH_ENABLED=true`, `KEYCLOAK_ISSUER_URL=https://keycloak.kakde.eu/realms/apps-prod`. The server validates RS256 signatures against the realm JWKS and the `cortex-web` audience. It hands the SPA its OIDC coordinates via the public `/api/auth/config`.
- **SPA:** `keycloak-js` runs the PKCE flow, gets a token, and sends it on `/api/run`, the submission endpoints, and every tutor call.
- **Tutor:** validates the *same* JWT. Your **tier** (homelab vs BYOK) is decided by whether your username is in `COACH_HOMELAB_USERS` — see [Tiers & BYOK](/cortex/cortex-onboarding/cortex-tutor/tiers-and-byok).

The `cortex-web` client must exist in the realm with `cortex.kakde.eu` redirect URIs and web origins (documented in `infra/deploy/apps/cortex/keycloak-client.md`). The GitHub IdP federation lets "Continue with GitHub" work in production — the homelab Keycloak federates GitHub, where the local dev realm does not.

**What a signed-in user can manage.** The header avatar opens a calm account dropdown (identity + Sign out); bulk data deletion lives on the **/account** page (*Manage account & data*). Coach sessions are **ephemeral** — a sliding TTL with a background purge — and mirrored to the browser; the only durable server copies are submissions and the transcripts allow-listed users explicitly **Save**. Deleting the Keycloak *identity* itself is self-service through Keycloak's **account console**: the apps hold no admin service-account, so `/account`'s **Delete my account** card links out to the console (where the user self-deletes) rather than asking the server to delete an identity it has no rights to touch. That requires the realm's **Delete Account** required action enabled and the `delete-account` role on users — add it to `default-roles-apps-prod` so federated GitHub users inherit it. See [Local dev → Auth & GitHub sign-in](/cortex/cortex-onboarding/runbooks/local-dev/auth-and-github-signin) for the exact realm switches.

## Troubleshooting: the Coach shows the static "manual prompts" fallback

If signed-in users see the Coach's pink *"the interactive coach is on its way — these prompts are the manual version"* banner instead of the live model picker, the SPA isn't getting a usable `whoami` from the tutor. The Coach degrades to its static fallback **by design** whenever `GET /v1/whoami` doesn't return a clean `200` — so this is almost always the tutor **failing to validate your token**, not missing or deleted code.

The tell: an **anonymous** `whoami` returns `401`, but an **authenticated** one returns `500`. That isolates it to **signing-key (JWKS) resolution**, which only runs on authenticated requests:

```bash
kubectl -n apps-prod logs deploy/cortex-tutor --since=10m | grep -i whoami
# … "GET /v1/whoami HTTP/1.1" 500   ← authed (broken)
# … "GET /v1/whoami HTTP/1.1" 401   ← anon (fine)
```

**Root cause we hit (2026-06):** the tutor fetches the realm JWKS from the **public** Keycloak (`keycloak.kakde.eu`, behind **Cloudflare**). With Cloudflare **Browser Integrity Check** (or Bot Fight Mode) enabled, the edge **403s** the tutor's `Python-urllib` fetch — *error 1010, "banned based on browser signature."* The cortex Scala server hits the same URL via a Java client whose UA isn't flagged, so **its** auth keeps working — which is why the site logs you in fine but the coach is dead. Confirm from inside the pod:

```bash
kubectl -n apps-prod exec deploy/cortex-tutor -c tutor -- python -c \
"import urllib.request as u; print(u.urlopen('https://keycloak.kakde.eu/realms/apps-prod/protocol/openid-connect/certs', timeout=5).status)"
# 403  → Cloudflare is blocking the JWKS fetch
```

**Fixes, in order of durability:**

1. **Durable — fetch JWKS off the public edge.** Set `KEYCLOAK_JWKS_URL` on the tutor Deployment to the **in-cluster** Keycloak Service so validation never touches Cloudflare (the `iss` check still uses the public issuer): `http://keycloak.identity.svc.cluster.local/realms/apps-prod/protocol/openid-connect/certs`. See `cortex-tutor` **ADR-0003**. Bot protection can then stay on.
2. **Immediate — relax the edge for the OIDC path.** Turn off **Browser Integrity Check** (Cloudflare → Security → Settings), or add a WAF custom rule that **Skips** it for `keycloak.kakde.eu` `/realms/*`. The running pod's next `whoami` then succeeds (PyJWT refetches; no restart needed). Note: **Bot Fight Mode** *cannot* be exempted by a custom rule on the free plan — turn it off, or use fix #1.

Either way, hard-refresh the Coach tab; you should get the live model picker. (The tutor also now logs `tutor.jwks_unavailable` and returns a clean `503` instead of a silent `500` on any future JWKS failure — ADR-0003.)

## What's where (quick map)

| Concern | Location |
|---|---|
| Cortex DB secret | `infra/deploy/apps/cortex/overlays/prod/sealedsecret.yaml` |
| Tutor secrets | `infra/deploy/apps/cortex-tutor/overlays/prod/sealedsecret.yaml` |
| Keycloak client spec | `infra/deploy/apps/cortex/keycloak-client.md` |
| Realm export (DR) | `infra/deploy/dr/` (keycloak realm export) |
| Secret recovery tree | `infra/deploy/dr/secret-recovery.md` |

> **Next:** [Scaling & DR](/cortex/cortex-onboarding/runbooks/production/scaling-and-dr).
