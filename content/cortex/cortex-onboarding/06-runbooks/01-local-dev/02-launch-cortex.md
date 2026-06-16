---
title: Launch Cortex
summary: The one command that brings up the Scala app and its stores — what it does, what comes up where, and how to turn the auth gate on.
---

## The one command

From the `cortex` repo root:

```bash
./bin/dev
```

That's it. `bin/dev` is an orchestrator that brings up *everything except the Tutor* (which is a [separate step](/cortex/cortex-onboarding/runbooks/local-dev/launch-the-tutor)). Reading the script top to bottom is the best 5-minute investment you can make; here's what it does, in order:

1. **Sweeps stale processes.** Kills orphaned sbt watchers / forked server JVMs from a prior run (they reparent to PID 1 and pin a CPU), then frees ports **8080** (server) and **5173** (Vite).
2. **Sets the auth gate** — `AUTH_ENABLED=false` by default (see below).
3. **Clears the cross-compiled `shared` targets** for a clean start (two sbt processes race on the zinc cache otherwise). Skip with `DEV_NO_CLEAN=1 ./bin/dev` for a faster warm start.
4. **Starts the backing stores** with Docker: `db redis mongo go-judge likec4` (+ `keycloak` if auth is on). It **rebuilds go-judge first** (`docker compose build go-judge`) — layer-cached and fast unless the runner changed — so a Scala `/api/run` never NZECs on a stale sandbox image.
5. **Waits for health**, exports `EXECUTOR_URL` and `LIKEC4_URL`.
6. **Installs npm deps** (first run) and regenerates the viz render types.
7. **Starts the sbt watcher** (`~ ;client/fastLinkJS ;server/reStart`) — incremental relink of the client + auto-restart of the server.
8. **Waits for the server** on `:8080`, then **starts Vite** on `:5173` (staggered to avoid a zinc-cache race).

## Where to look

When it prints `all services up`, you have:

| URL | What |
|---|---|
| **http://localhost:5173** | **The site.** Vite dev server with HMR — *use this one.* It proxies `/api/*`, `/docs/*`, `/c4/*` to `:8080`. |
| http://localhost:8080 | ZIO server (API + static fallback). Don't open directly in dev — there's no client bundle there. |
| http://localhost:8080/docs | Swagger UI for the API. |
| http://localhost:8080/c4/view/index | The LikeC4 homelab overview (proxied to the likec4 container). |
| localhost:5432 / 6379 / 27017 | Postgres / Redis / Mongo (user/pass `cortex` for PG; db `cortex` for Mongo). |
| localhost:5050 | go-judge sandbox (`POST /run`). |

> **Always develop against `:5173`, never `:8080`.** In dev the server has no built client bundle, so `:8080` serves API + 404s for page routes. The Vite proxy is what stitches the SPA (5173) to the API (8080).

## Auth: off by default, on when you want the real flow

The auth gate (ADR-0013) is **off** under `bin/dev`. With it off:

- the editor is unlocked for everyone (no sign-in),
- the `/api/run` rate limiter no-ops,
- the server's JWT verifier short-circuits,
- the SPA never loads `keycloak-js`,
- **no Keycloak container starts** (so no ~30–60s realm-import wait).

That's the fast path for writing content and hacking on the app. To exercise the **real GitHub-sign-in flow** end to end:

```bash
AUTH_ENABLED=true ./bin/dev
```

Now `bin/dev` also starts a local **Keycloak** container, waits for the `cortex` realm to import, and wires the server + SPA to it (the **local** realm at `http://localhost:8081/realms/cortex`, never prod). Sign in at the modal as **`tester` / `tester`** (Keycloak admin is `admin`/`admin` at http://localhost:8081). Out of the box the local realm has no GitHub IdP, so "Continue with GitHub" lands on Keycloak's own login form. To wire up **real GitHub OAuth on localhost** (your own GitHub App), follow [Auth & GitHub sign-in on localhost](/cortex/cortex-onboarding/runbooks/local-dev/auth-and-github-signin).

> **You need `AUTH_ENABLED=true` to test the Tutor's homelab tier** (and any signed-in behavior), because the coach needs a real JWT. For BYOK, you also sign in — the tier just depends on whether your user is on the allowlist.

## Quick health checks

```bash
curl http://localhost:8080/api/health                  # {status, postgres, redis, mongo}
curl http://localhost:8080/api/cortex/index | jq .     # all books at a glance
curl -X POST http://localhost:8080/api/run \
  -H 'content-type: application/json' \
  -d '{"language":"python","code":"print(6*7)"}'        # → stdout "42", statusId 3 (Accepted)
```

If `/api/run` returns **503 `NotConfigured`**, `EXECUTOR_URL` isn't set — that's `bin/dev` not having reached go-judge, or you're running the server outside `bin/dev` without exporting it.

## Stopping

`Ctrl-C` in the `bin/dev` terminal runs a cleanup trap: it stops the sbt-revolver server, kills the watcher, and frees `:8080`. The Docker stores keep running (they're cheap and stateful) — stop them with `docker compose down` (add `-v` to wipe volumes). If a forked JVM ever survives, `sbt server/reStop` or `kill $(lsof -ti:8080)` clears it.

> **Next:** [Launch the Tutor](/cortex/cortex-onboarding/runbooks/local-dev/launch-the-tutor) — the coach is a second process; here's how to start it and wire it in.
