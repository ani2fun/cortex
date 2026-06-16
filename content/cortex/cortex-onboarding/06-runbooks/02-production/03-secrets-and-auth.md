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

## What's where (quick map)

| Concern | Location |
|---|---|
| Cortex DB secret | `infra/deploy/apps/cortex/overlays/prod/sealedsecret.yaml` |
| Tutor secrets | `infra/deploy/apps/cortex-tutor/overlays/prod/sealedsecret.yaml` |
| Keycloak client spec | `infra/deploy/apps/cortex/keycloak-client.md` |
| Realm export (DR) | `infra/deploy/dr/` (keycloak realm export) |
| Secret recovery tree | `infra/deploy/dr/secret-recovery.md` |

> **Next:** [Scaling & DR](/cortex/cortex-onboarding/runbooks/production/scaling-and-dr).
