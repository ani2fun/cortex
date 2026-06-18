---
title: Access & allowlists
summary: Who may Submit code and Save coach history — one Postgres allowlist (`submission_allowlist`), granted by hand and checked live. Plus the separate knob that sets a learner's coach model tier.
---

> 📓 **One table, two gates, no restart.** Both "Submit code" and "Save coach history" are gated by the **same** `submission_allowlist` table in the cortex Postgres. It's checked **live on every request**, so granting or revoking is a single SQL statement — no redeploy, no pod restart. Keyed by the Keycloak **`preferred_username`** claim (the GitHub login for IdP users).

## Why an allowlist

Cortex is a personal homelab, not a hosted service. Anyone signed in can *run* code (sandboxed, quota-limited) and *use* the coach, but **persisting** to the homelab DB — saving a code submission or a coach transcript — is granted selectively, because stored data carries no durability guarantee. Access is by request (email **cortex.kakde.eu@gmail.com** with a GitHub username).

**There's nothing to pre-create.** A new user's Keycloak identity is auto-provisioned the first time they click **Continue with GitHub** — the production realm federates GitHub, so the account *is* their GitHub login. Granting access is therefore two steps: (1) they sign in once (account created automatically), and (2) you add their GitHub login to the table below. You never create accounts by hand in production; you only allow-list the ones that already signed in. (Local dev is different — there the test users are seeded in the realm import; see [Auth & GitHub sign-in → Adding a local dev user](/cortex/cortex-onboarding/runbooks/local-dev/auth-and-github-signin).)

Coach sessions are **ephemeral by default**: the tutor keeps them only for a sliding TTL and the SPA mirrors the transcript to your browser for refresh-safety. Keeping one past that is an explicit **Save** in the coach — and *that* Save is what this allowlist gates. An off-list visitor still coaches normally and still has the browser mirror; they just can't write a durable copy to the homelab DB. So the allowlist governs **two durable writes** — `POST /api/submissions` (Submit) and `POST /api/coach/saved` (Save) — and nothing about whether you can *use* either feature.

The table:

```sql
CREATE TABLE submission_allowlist (
  username   TEXT        PRIMARY KEY,   -- Keycloak preferred_username == GitHub login
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Both `POST /api/submissions` (Submit) and `POST /api/coach/saved` (Save) check `SELECT 1 FROM submission_allowlist WHERE username = ?`. A miss returns a friendly **403** with the request-access text and stores nothing — the coach transcript still survives a refresh from the browser mirror, it just isn't kept server-side.

## Manage the list — the SQL

```sql
-- who's allowed
SELECT username, granted_at FROM submission_allowlist ORDER BY granted_at DESC;

-- grant a GitHub user (idempotent — username is the primary key)
INSERT INTO submission_allowlist (username) VALUES ('<github-login>') ON CONFLICT DO NOTHING;

-- revoke
DELETE FROM submission_allowlist WHERE username = '<github-login>';
```

## Run it — local dev

```bash
# from the cortex repo root, with the stack up (./bin/dev or ./scripts/devcombined):
docker compose exec db psql -U cortex -d cortex \
  -c "INSERT INTO submission_allowlist (username) VALUES ('<github-login>') ON CONFLICT DO NOTHING;"
```

The local Keycloak realm seeds two accounts you can test with: **`tester`** is **already on the allowlist** (Submit and Save work out of the box), and the owner `ani2fun`. The realm also ships **`test1`** (password `test1`), which is **deliberately NOT allow-listed** — sign in as `test1`, press Save, and you'll get the request-access 403; then run the `INSERT` above for `test1` and Save again to watch it start working, no restart.

## Run it — production (K3s)

The shared Postgres lives in the `databases-prod` namespace; the `cortex` DB password is in the `cortex-db` secret (`apps-prod`, key `postgres-password`). From the kubectl host (ms-1):

```bash
PGPW=$(kubectl -n apps-prod get secret cortex-db -o jsonpath='{.data.postgres-password}' | base64 -d)

# ephemeral psql pod in the DB namespace, deleted on exit (--rm):
kubectl -n databases-prod run psql-allowlist --rm -it --restart=Never --image=postgres:16 \
  --env="PGPASSWORD=$PGPW" -- \
  psql -h postgresql.databases-prod.svc.cluster.local -U cortex -d cortex \
  -c "INSERT INTO submission_allowlist (username) VALUES ('<github-login>') ON CONFLICT DO NOTHING;"
```

Swap the `-c` SQL for the `SELECT` or `DELETE` above to list or revoke.

## Not the same thing: coach model *tier*

Don't conflate the allowlist with **`COACH_HOMELAB_USERS`** — a *tutor* env var, **not** this table. It decides a signed-in learner's **coach model tier**: the operator's homelab / Ollama models plus quota exemption, versus the BYOK default (Claude Sonnet). It's managed on the tutor deployment, and a change needs a pod restart:

```bash
kubectl -n apps-prod set env deployment/cortex-tutor COACH_HOMELAB_USERS='ani2fun,<other>'
```

| | `submission_allowlist` (this table) | `COACH_HOMELAB_USERS` (tutor env) |
|---|---|---|
| **Gates** | Saving code submissions + coach transcripts | Which coach **models** you may pick + quota exemption |
| **Lives in** | cortex Postgres | tutor deployment env |
| **Applied** | Live, per request — no restart | On tutor pod restart |

See [Tiers & BYOK](/cortex/cortex-onboarding/cortex-tutor/tiers-and-byok) for the tier story.

## Verify

1. Sign in as the user.
2. **Submit** a solution (problem page) or press **Save** on a coach transcript.
3. **Allowed** → it's stored (Submit shows "saved as submission #N"; Save shows the green "Saved to your homelab database" notice). **Not allowed** → a friendly 403 with the request-access message; nothing persists, and the coach transcript still survives a refresh from the browser mirror.
