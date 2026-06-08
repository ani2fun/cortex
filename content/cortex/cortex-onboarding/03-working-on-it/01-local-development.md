---
title: Local Development
summary: How to run the app, what to expect from `bin/dev`, the env vars that matter, and the foot-guns we've already stepped on.
---

## Prerequisites

- **JDK 21+** (the build is happy on 17 but Liquibase is happier on 21).
- **sbt** (any modern version; `project/build.properties` pins the exact one).
- **Node 20+** and **npm**.
- **Docker + docker-compose** for the persistence layer (Postgres / Redis / Mongo) and the local **go-judge** sandbox. (go-judge runs `--privileged` for its cgroup sandbox — Docker Desktop handles this fine.)

A quick check:

```bash
java -version && sbt --version && node -v && docker compose version
```

## Day-one setup

```bash
git clone <repo> cortex
cd cortex
docker compose up -d db redis mongo go-judge   # first go-judge build is ~1.9 GB
(cd client && npm install)
sbt compile        # triggers OpenAPI codegen — first run downloads ~1 GB of deps (3–10 min)
```

That last `sbt compile` does two important things on first run:

1. **OpenAPI codegen.** `sbt-openapi-codegen` reads `api/openapi.yaml` and emits Scala into `shared/.{jvm,js}/target/scala-3.6.2/src_managed/main/sbt-openapi-codegen/`. If that step fails, *nothing* downstream will compile — server, client, or shared tests.
2. **Liquibase migration check.** Migrations run when the *server* starts, not at compile time. But the JDBC driver and migration files have to be on the classpath, so the build verifies the SQL syntax loads.

## Running

The day-to-day command is:

```bash
./bin/dev
```

That script starts two things in parallel:

- `sbt ~reStart` (incremental rebuild + auto-restart of the ZIO server on `:8080`).
- `cd client && npm run dev` (Vite dev server on `:5173`, watching the Scala.js linker output for changes).

Visit **`http://localhost:5173`**. The Vite dev server proxies `/api/*` and `/docs/*` to `:8080`. **Don't visit `:8080` directly** during dev — there's no client bundle there yet, only the server. (See `HttpApp.scala`'s static-handler comment: in dev the static dir doesn't exist, so the server skips mounting static routes entirely.)

Hot reload behaviour:

| Edit | What restarts |
| --- | --- |
| `client/src/**/*.{ts,scala}` | Scala.js relinks (sbt watcher) → Vite picks up the new JS → browser HMR |
| `client/src/markdown/render.ts` | Vite HMR — usually a reload, sometimes a full page refresh if the dynamic-import graph changed |
| `client/index.html`, `client/tailwind.css`, `client/src/styles/**/*.css` | Vite HMR (Tailwind v4 plugin re-runs `@apply`; if a stylesheet references an unknown utility the page goes blank — check the Vite log) |
| `server/**/*.scala` | sbt-revolver kills the JVM and starts a new one — frontend stays up |
| `api/openapi.yaml` | regenerates `shared/`; both projects recompile |
| `content/cortex/**/*.md` | served from disk by the running server — refresh the browser |

## Production build & run

```bash
sbt 'client/fastLinkJS'           # debug-grade JS, fast feedback
# or
sbt 'client/fullLinkJS'           # release-grade JS (longer link time, smaller output)

(cd client && npm run build)      # vite build → client/dist
sbt 'server/Compile/run'          # serves :8080 with the dist on disk
```

Or use Docker:

```bash
docker compose up --build app
```

The Dockerfile builds the SPA, copies the dist into the JVM image, and the server's static handler picks it up via `STATIC_DIR=/app/static`.

## Environment variables

These are the ones you'll actually touch. Every config value is read once in `config/AppConfig.scala` into a typed `AppConfig` tree (the HOCON block is rooted at `cortex { … }`); the names below are the `${?ENV}` overrides declared in `server/src/main/resources/application.conf`. Production overrides live in `docker-compose.yml` / the K8s manifests.

| Var | Default | What for |
| --- | --- | --- |
| `PORT` | `8080` | server listen port |
| `STATIC_DIR` | `./client/dist` (dev) / `/app/static` (Docker) | where the Vite output lives |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local Postgres (`…/cortex`, `cortex`/`cortex`) | Hello demo storage |
| `REDIS_URL` | `redis://localhost:6379` | Hello demo cache + `/api/run` rate limiter |
| `MONGO_URI`, `MONGO_DB` | `mongodb://localhost:27017` / `cortex` | Hello demo event log |
| `EXECUTOR_URL` | `http://localhost:5050` (`bin/dev`) / `http://go-judge:5050` (compose) | go-judge sandbox for `/api/run` |
| `EXECUTOR_AUTHN_TOKEN` | unset | optional bearer token (go-judge `ES_AUTH_TOKEN`) |
| `CORTEX_ROOT` | `./content/cortex` (dev) / `/app/content/cortex` (Docker) | where books live |
| `BLOG_ROOT` | `./content/blogs` (dev) / `/app/content/blogs` (Docker) | where blog posts live |
| `CORTEX_AUTO_RELOAD`, `BLOG_AUTO_RELOAD` | `true` (dev) / `false` (prod) | mtime-rewalk the content tree per request |
| `AUTH_ENABLED` | `true` (`application.conf`) / `false` (`bin/dev`) | enforce Keycloak JWT auth + rate limiting |
| `KEYCLOAK_ISSUER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` | prod realm `apps-prod` at `keycloak.kakde.eu`, client `cortex-web` | OIDC coordinates handed to the SPA via `/api/auth/config` |
| `LIKEC4_URL` | `http://likec4` (prod) / `http://localhost:8090` (`bin/dev`) | upstream for the `/c4/*` LikeC4 proxy |

If `EXECUTOR_URL` is not set, `/api/run` returns 503 (`RunFailure.NotConfigured`). That's a feature: it makes the misconfiguration visible immediately rather than silently swallowing executions.

For fast content/runner iteration, `AUTH_ENABLED=false ./bin/dev` skips Keycloak entirely — the JWT verifier short-circuits, the rate limiter no-ops, the editor is unlocked for everyone, and the SPA never loads `keycloak-js`. `bin/dev` defaults to that; opt into the full sign-in flow with `AUTH_ENABLED=true ./bin/dev` (it then starts a local Keycloak container with a `tester`/`tester` user). See ADR-0013.

## Useful commands

```bash
sbt test                          # run shared codegen smoke spec + any other tests
sbt 'shared/compile'              # regenerate codegen after editing openapi.yaml
sbt 'project client; reload'      # poke the client subproject if it gets stuck
curl http://localhost:8080/api/health                      # liveness
curl http://localhost:8080/docs                            # Swagger UI for the API
curl http://localhost:8080/api/cortex/index | jq .      # all books at a glance
```

## Foot-guns we've already stepped on

These are real bugs that have shipped (or almost shipped) and the lessons attached.

### 1. Run sbt from the repo root, not from `client/`

If you `cd client && sbt`, sbt creates a stray `client/project/` directory and loads with whatever sbt version it finds first — typically not the version pinned by the real build. The two builds end up disagreeing about what's compiled, and you lose half a day. Always `sbt` from the repo root, even for "client-only" tasks: `sbt client/fastLinkJS`.

### 2. The walker bug — empty placeholders

```scala
// WRONG: silently drops the mounting work
val mountAll: Callback = articleRef.foreach { article =>
  Callback { /* ... */ }   // <-- this Callback is constructed but never run
}

// RIGHT: the body of articleRef.foreach IS the Callback
val mountAll: Callback = articleRef.foreach { article =>
  /* imperative mounting code */    // runs when the outer Callback fires
}
println("If your placeholders render empty, this is the first thing to check.")
```

There's a comment on this exact bug in `ChapterContent.scala`. The compiler doesn't catch it because both shapes type-check; only the outer one actually mounts.

### 3. `Some(uri"")` is a runtime error

sttp's `uri"…"` interpolator rejects the empty string at runtime, so we can't use `Some(uri"")` as a base-URI placeholder for "use whatever the browser thinks origin is". The correct value is `None` — `ApiClient.scala` does this. If you change it to `Some(...)` "for clarity", you'll break the production build the moment it tries to send a request.

### 4. CSS `[&>svg]` vs `[&_svg]` in D2

D2 SVGs land inside a `<div dangerouslySetInnerHtml>` wrapper, **not** as direct children of the styled card. Tailwind's `[&>svg]` (direct child) selector won't match — you need `[&_svg]` (descendant). If a CSS rule on D2 doesn't seem to apply, this is the second thing to check after "did Tailwind generate the class at all".

### 5. The `runId` ↔ `tag` consistency in `RunnableCodeBlock`

The `tag` used to drop late results **must** be computed inside the `modState` lambda, not from the render-time `s` snapshot. A render-time tag works for the first run after a clean render and silently fails after a cancel-then-run. Run state stuck on Cancel? This is it.

### 6. Browser, not curl

Verifying the server with `curl` and the build with `vite build` is necessary but **not sufficient**. Several bugs (the walker bug, the relative-URL bug) only manifest when an actual browser tries to render an actual page. **Always test the change in a browser before claiming it's done.**

## Logging

Server logs go through SLF4J + Logback. `Main.scala` installs a `SLF4JBridgeHandler` so Liquibase's `java.util.logging` records reach Logback too — without it, sbt-revolver flags Liquibase INFO output as stderr noise and clutters the dev console.

To turn the dial up:

```bash
# Logback config picks up env-var overrides; see logback.xml in server/src/main/resources/
ROOT_LOG_LEVEL=DEBUG ./bin/dev
```

## When the dev environment misbehaves

A quick, in-order checklist:

1. `docker compose ps` — are db / redis / mongo / go-judge all up?
2. `sbt 'shared/compile'` — did codegen run?
3. `cd client && rm -rf node_modules && npm install` — broken native deps?
4. `git clean -ndx | grep target` — stale sbt target dirs sometimes hide compile-cache poisoning; `git clean -fdx` clears them. (Do this only if you're OK losing local target/dist artifacts.)
5. Open the browser dev tools — is there a 404 on a `/assets/...js` chunk? Vite forgot to build a chunk; restart the dev server.
