---
title: End-to-end & troubleshooting
summary: Verify a full coached turn works, then a checklist of the real foot-guns — stale stacks, NZEC-ing Scala, the auth-off 500 — and their fixes.
---

## Verify the whole thing works

A two-minute smoke test that exercises every moving part.

**1. The app + stores (no tutor needed):**

```bash
curl http://localhost:8080/api/health
# {"status":"ok","postgres":"ok","redis":"ok","mongo":"ok"}
curl -X POST http://localhost:8080/api/run -H 'content-type: application/json' \
  -d '{"language":"python","code":"print(\"hello from go-judge\")"}'
# statusId 3 (Accepted), stdout "hello from go-judge"
```

**2. A coached turn** (requires the [tutor wired in](/cortex/cortex-onboarding/runbooks/local-dev/launch-the-tutor) and `AUTH_ENABLED=true`):

1. Open **http://localhost:5173**, sign in (`tester`/`tester`).
2. Go to any problem with a **Your Turn** — e.g. a DSA pattern problem.
3. Open the **Coach** tab. You should see the model picker populate (that's `whoami` succeeding).
4. Type an answer to *clarify* and submit. Watch the step tracker: the gate grades it, and either the coach streams a nudge (retry) or you advance to *examples*.

If step 3 shows the **static fallback** instead of a live picker, the SPA didn't get a `tutorBaseUrl` — re-check that you launched with `CORTEX_TUTOR_BASE_URL=…` **and** `AUTH_ENABLED=true`.

## Troubleshooting checklist

These are real foot-guns that have cost real time. Work top to bottom.

### `bin/dev` says "port is already allocated" (e.g. :5050)

An **orphaned Docker stack** is squatting the port — usually a leftover compose project from an agent worktree or a prior run that never tore down. Diagnose and clear it:

```bash
docker ps --format '{{.Names}}\t{{.Ports}}' | grep -E '5050|5432|6379|27017'   # who's holding them?
# if it's a stray project (name not starting with cortex-):
docker compose -p <orphan-project> down            # stop the squatter (keeps volumes)
docker compose -p cortex down --remove-orphans      # clean cortex's own stack
AUTH_ENABLED=false ./bin/dev                        # relaunch
```

### Scala `/api/run` fails with NZEC / `UnknownHostException`, Python is fine

The **go-judge image is stale** and missing its pre-warmed Scala coursier cache, so `scala-cli` tries to fetch the compiler over the (sandboxed, no-network) connection and dies. `bin/dev` rebuilds go-judge on launch now, but if you started the stack another way:

```bash
docker compose build go-judge && docker compose up -d go-judge
```

Sanity check the warm cache is present (should be ~135 files incl. a `scala3-compiler` jar):

```bash
docker run --rm --entrypoint /bin/sh "$(docker compose images -q go-judge)" \
  -c 'find /usr/local/share/coursier -type f | wc -l'
```

### `/api/auth/config` returns 500 with auth off

**Expected, by design.** Under `AUTH_ENABLED=false`, that endpoint 500s and the client logs *"treating as disabled."* It's pre-existing noise, not a bug — the SPA correctly proceeds in the no-auth path.

### `bin/dev` launched in the background "exited 0" but nothing's up

That's the launcher **detaching**, not finishing. The server lags the launch by a Scala compile (~1–2 min). Poll for it:

```bash
lsof -ti:8080        # empty until the server is actually listening
```

### Vite came up on a different port

If something already holds 5173, Vite picks another port but the assigned `PORT` env can be ignored — **read the actual port from Vite's `Local:` log line**, don't assume 5173.

### A stale forked JVM is holding :8080 after Ctrl-C

`bin/dev`'s trap usually catches this, but if a run was SIGKILLed:

```bash
sbt server/reStop            # clean stop of the revolver-managed server
# or, brute force:
kill $(lsof -ti:8080)
```

### A content edit changed a pile of unrelated `index.md`

The PostToolUse index hook (`tools/gen_cortex_index.py`) regenerates `index.md` across the content tree on any `content/cortex/**` edit. It now *preserves* hand-authored pages (those with YAML frontmatter), but if you see unexpected `index.md` churn, restore what you didn't mean to touch:

```bash
git restore --source=HEAD -- $(git diff --name-only | grep '/index\.md$')
```

### General "it's wedged" reset

In order, least to most destructive:

```bash
docker compose ps                       # are db/redis/mongo/go-judge/likec4 up?
sbt 'shared/compile'                    # did OpenAPI codegen run?
(cd client && rm -rf node_modules && npm install)   # broken native dep?
git clean -ndx | grep target            # preview stale sbt targets; -fdx to clear (loses local build artifacts)
```

### Tutor: `readyz` is failing

```bash
curl http://localhost:8000/readyz       # which dependency is down?
```

`readyz` checks Postgres (:5433), the JWKS endpoint (your `KEYCLOAK_ISSUER_URL`), and the MCP server (:8001). Anthropic reachability is a *metric*, never a gate — a missing `ANTHROPIC_API_KEY` won't fail readiness, it'll fail the turn (homelab tier) with a clear error.

> **Next:** that's local dev. For running the real thing, head to [Production → Topology](/cortex/cortex-onboarding/runbooks/production/topology).
