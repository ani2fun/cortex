---
title: Launch the Tutor
summary: Bring up the Python coach service and wire it to the SPA — the env it needs, the port map that lets it coexist with `bin/dev`, and the JWT detail that trips everyone up.
---

The Tutor is a **separate process in a separate repo**. The books and the code playground work without it; the live coach does not. This runbook starts it and connects it to your local SPA.

> **The whole point to internalize:** the coach needs a **real JWT**. So the live coach only works when **auth is ON for both** cortex and the tutor, *pointing at the same Keycloak*. Under the default `AUTH_ENABLED=false ./bin/dev`, the Coach tab falls back to the static, manual-prompts experience — which is fine for content work, but isn't the live tutor.

## 1. Set up the env

From the `cortex-tutor` repo root:

```bash
uv sync                 # create the venv (installs Python 3.12 if needed)
cp .env.example .env     # then edit .env — see below
```

The `.env` values that matter for local dev:

| Var | Local value | Why |
|---|---|---|
| `AUTH_ENABLED` | `true` | The coach needs a JWT. (Set `false` only to smoke-test the API with no identity.) |
| `KEYCLOAK_ISSUER_URL` | `http://localhost:8081/realms/cortex` | **Override the prod default** to point at `bin/dev`'s local Keycloak. |
| `KEYCLOAK_CLIENT_ID` | `cortex-web` | Same client the SPA uses. |
| `ANTHROPIC_API_KEY` | *your key* | Funds the **homelab-tier** coach + the gate. Leave blank if you'll only test BYOK. |
| `COACH_HOMELAB_USERS` | `tester` | Put the **Keycloak username you sign in as** here to get the homelab tier locally (default is `ani2fun`). |
| `DATABASE_URL` | `…@localhost:5433/cortex` | The tutor's **own** Postgres on **:5433** — so it coexists with cortex's `bin/dev` Postgres on :5432. |
| `MCP_URL` / `GROUNDING_PORT` | `http://localhost:8001/mcp` / `8001` | The grounding MCP server (8001, not 8081 — that's Keycloak). |
| `CORS_ALLOW_ORIGINS` | already includes `http://localhost:5173` | Lets the SPA call the tutor cross-origin. |

> **Port map (why nothing collides):** cortex's `bin/dev` uses 5173 (Vite), 8080 (server), 5432 (PG), 6379 (Redis), 27017 (Mongo), 5050 (go-judge), 8081 (Keycloak), 8090 (likec4). The tutor deliberately uses **8000** (API), **8001** (MCP), and **5433** (its own PG) so both stacks run side by side.

## 2. Start it

```bash
./bin/dev    # ★ one command: Postgres (:5433) + Liquibase migrate + FastAPI autoreload (:8000)
# — or the underlying make targets:
make up      # full container stack (postgres + liquibase + tutor) on :8000
make dev     # FastAPI autoreload only (assumes stores already up)
```

The tutor's **`./bin/dev`** mirrors cortex's: it frees :8000, brings up the tutor's own Postgres (host **:5433**, so it never collides with cortex's :5432), runs the Liquibase migrations (the `tutor` schema), and starts FastAPI with autoreload on **http://localhost:8000**. Smoke-test it:

```bash
curl http://localhost:8000/healthz      # liveness
curl http://localhost:8000/readyz       # readiness (DB + JWKS + MCP reachable)
```

## 3. Wire it into the SPA

Cortex learns the tutor's address from one env var, surfaced to the browser via `/api/auth/config` (see [Architecture](/cortex/cortex-onboarding/cortex-tutor/architecture)). You have two ways to run both stacks wired together.

**The one command (recommended) — `devcombined`.** From the **cortex** repo, a single launcher brings up *both* stacks (it requires cortex and cortex-tutor to be siblings under the same parent dir):

```bash
# in the cortex repo — launches cortex/bin/dev AND ../cortex-tutor/bin/dev,
# wires CORTEX_TUTOR_BASE_URL, prefixes both logs, Ctrl-C stops both.
./scripts/devcombined
# full Keycloak sign-in flow instead of the synthetic-principal dev path:
AUTH_ENABLED=true ./scripts/devcombined
```

**Or wire it by hand** — run the two `bin/dev`s in separate terminals and set the env var on cortex's:

```bash
# terminal 1 — the tutor
cd ../cortex-tutor && ./bin/dev
# terminal 2 — cortex, told where the tutor lives (+ auth on for the full sign-in flow)
CORTEX_TUTOR_BASE_URL=http://localhost:8000 AUTH_ENABLED=true ./bin/dev
```

Either way the chain completes: `CORTEX_TUTOR_BASE_URL` → `AppConfig` → `/api/auth/config` → the SPA's `AuthStore` → `TutorApiClient`. Open the site, sign in as **`tester`/`tester`**, open a problem's **Coach** tab, and you should get a live `whoami`. (With the default auth-off `devcombined`, the tutor uses a synthetic dev principal — set `FORCE_BYOK=true` in `cortex-tutor/.env` to exercise the BYOK path.)

## 4. Pick your tier locally

- **Homelab tier** (server-funded Claude): make sure your sign-in username (`tester`) is in `COACH_HOMELAB_USERS` *and* `ANTHROPIC_API_KEY` is set. The default model is the local `qwen-coach` — which needs `OLLAMA_URL` pointing at an Ollama instance; if you don't run Ollama, pick a cloud Claude model in the dropdown instead (it'll run client-direct on your key, exactly like BYOK).
- **BYOK tier** (you fund it): leave `tester` *out* of `COACH_HOMELAB_USERS`. Sign in, open the Coach, pick an OpenRouter or Anthropic model, and paste your own key when prompted. The key is held in `sessionStorage` and sent only to the provider — [never to any of our servers](/cortex/cortex-onboarding/cortex-tutor/tiers-and-byok).

## Without the Tutor

You don't need any of this to write content or test the runner. With the tutor unconfigured (no `CORTEX_TUTOR_BASE_URL`), the Coach tab shows the static fallback and everything else works normally. Wire the tutor in only when you're working on the coach itself.

> **Next:** [End-to-end & troubleshooting](/cortex/cortex-onboarding/runbooks/local-dev/end-to-end-and-troubleshooting) — verify a full coached turn, and fix the things that commonly break.
