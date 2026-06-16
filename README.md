# cortex

An interactive, runnable computer-science knowledge base — the **Cortex** books,
a **blog**, and a three-store **demo** — live at **[cortex.kakde.eu](https://cortex.kakde.eu)**.
Frontend is **Scala.js + scalajs-react** (with a small TypeScript module hosting the
unified/remark/rehype markdown pipeline + the D3 visualisers), backend is **ZIO +
zio-http + tapir**, the API contract is **OpenAPI-first** with code generation.

> Split note: this repo was carved out of the old `codefolio` monorepo. The static
> portfolio now lives in its own repo at [kakde.eu](https://kakde.eu); everything
> here is the dynamic Cortex app.

> **New here?** The engineering tour lives in Cortex itself. After `./bin/dev`, open
> <http://localhost:5173/cortex-onboarding/overview> for a walk-through of the
> architecture, the request lifecycle, and the markdown pipeline; the **Cortex Tutor**
> section explains the Socratic AI coach, and the **Runbooks** section is copy-paste-able
> local + production setup so you can run the whole platform yourself. The chapters are also
> readable as plain markdown under
> [`content/cortex/cortex-onboarding/`](content/cortex/cortex-onboarding/).

## What's inside

- **Cortex books** — the index is at `/`; `/{book}/{chapter}` renders markdown with
  KaTeX math, mermaid + D2 diagrams, GFM tables, interactive D3 widgets, and *runnable*
  code blocks. The markdown pipeline lives in TypeScript (`client/src/markdown/`) and is
  invoked from Scala.js through a lazy-loaded gateway, so its multi-MB dependency tree
  loads only on a chapter page.
- **Blog** — `/blogs` lists posts; `/blogs/{slug}` renders a post through the same
  pipeline.
- **Code execution** — `POST /api/run` proxies to a self-hosted **go-judge** sandbox
  (`EXECUTOR_URL`), same image in dev and prod. Backs both the runnable code blocks and
  the Cortex "Visualise" tracer. The editor is **Monaco**.
- **Three-store demo** — `/demo` hits Postgres / Redis / MongoDB end-to-end; a live
  integration smoke test (read-through Redis cache over a Postgres visit counter, every
  call appended to Mongo).
- **Auth** — OIDC via Keycloak (`apps-prod` realm, `cortex-web` PKCE client, GitHub IdP).
  Reading is public; editing / running code is gated by sign-in. `AUTH_ENABLED=false`
  unlocks everything for local dev.
- **Architecture diagrams** — **LikeC4** models under `content/cortex/**/c4/*.c4` compile to
  a small SPA (built by `Dockerfile.likec4`) that the server reverse-proxies at `/c4/*`;
  chapters embed views via `<iframe src="/c4/view/...">`.
- **Cortex Tutor** — an optional Socratic AI coach for the *Your Turn* problems, living in a
  **separate** repo ([`cortex-tutor`](https://github.com/ani2fun/cortex-tutor), FastAPI). The
  SPA calls it directly (different origin, same Keycloak JWT) at `CORTEX_TUTOR_BASE_URL`; if
  unset, the Coach tab shows a static fallback. See the onboarding *Cortex Tutor* section.

## Backing stores

| Store | Role |
|---|---|
| **Postgres** | Visit counter for the demo (Liquibase migrations, plain JDBC + HikariCP). |
| **Redis** (Lettuce) | Read-through cache for `/api/hello` (~10s TTL) — response carries a `cached` flag. |
| **MongoDB** (sync driver) | Append-only log of every `/api/hello` call; surfaced via `/api/recent`. |
| **go-judge** (sandbox) | `criyle/go-judge` + language toolchains; backs `/api/run` via its `/run` command API. |

## Prerequisites

- JDK 21+ · sbt 1.10+ · Node 20+ (Vite) · Docker (stores + go-judge + likec4 + Keycloak)

## Local development — one command

```bash
./bin/dev
```

Brings up Postgres / Redis / Mongo / go-judge / likec4 / Keycloak (in Docker), an sbt
watcher that re-runs `server/reStart` + `client/fastLinkJS` on change, and the Vite dev
server on :5173 — one terminal, prefixed colour-coded logs, Ctrl-C stops everything.
`AUTH_ENABLED=false ./bin/dev` skips Keycloak for fast content/runner iteration.

**Both stacks at once** (cortex + the `cortex-tutor` coach, so the SPA's live coach works end-to-end):
[`./scripts/devcombined`](scripts/devcombined) — launches `cortex/bin/dev` and the sibling
`../cortex-tutor/bin/dev` together and wires `CORTEX_TUTOR_BASE_URL`. Requires cortex and cortex-tutor to
be siblings under one parent dir.

| URL | What |
|---|---|
| <http://localhost:5173> | Frontend with Vite HMR (proxies `/api`, `/docs`, `/c4` to :8080) |
| <http://localhost:8080> | ZIO server (API + static fallback) |
| <http://localhost:8080/docs/> | Swagger UI |

## Environment variables

| Variable | What | Default |
|---|---|---|
| `PORT` | Server HTTP port | `8080` |
| `STATIC_DIR` | Vite production bundle dir | `./client/dist` (prod image: `/app/static`) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres | `jdbc:postgresql://localhost:5432/cortex` / `cortex` / `cortex` |
| `REDIS_URL` / `REDIS_TTL_SECS` | Redis cache | `redis://localhost:6379` / `10` |
| `MONGO_URI` / `MONGO_DB` | MongoDB | `mongodb://localhost:27017` / `cortex` |
| `EXECUTOR_URL` | go-judge sandbox base URL | `http://go-judge:5050` (compose) |
| `CORTEX_ROOT` / `BLOG_ROOT` | Content tree roots | `./content/cortex` / `./content/blogs` |
| `CORTEX_AUTO_RELOAD` / `BLOG_AUTO_RELOAD` | Re-walk on mtime change (off in prod) | `true` |
| `AUTH_ENABLED` | Gate editing/run behind Keycloak sign-in | `true` |
| `KEYCLOAK_ISSUER_URL` / `KEYCLOAK_REALM` / `KEYCLOAK_CLIENT_ID` | OIDC config | apps-prod / `cortex-web` |
| `LIKEC4_URL` | LikeC4 SPA upstream for the `/c4` proxy | — |
| `CORTEX_TUTOR_BASE_URL` | Cortex Tutor base URL (surfaced to the SPA via `/api/auth/config`); unset → coach falls back to static | — |

## API surface

| Endpoint | Method | What |
|---|---|---|
| `/api/cortex/index` | GET | List books + chapter refs. |
| `/api/cortex/{book}/{chapter}` | GET | Frontmatter + raw markdown + prev/next slugs for one chapter. |
| `/api/blogs/index` · `/api/blogs/{slug}` | GET | Blog index / one post. |
| `/api/run` | POST | Execute a code snippet via the go-judge sandbox. |
| `/api/hello` · `/api/recent` · `/api/health` | GET | The three-store demo + a health ping. |
| `/docs/` | GET | Swagger UI. |
| `/`, `/{book}/{chapter}`, `/blogs`, `/demo`, `/assets/*`, `/img/*` | GET | Static / SPA-fallback. Legacy `/cortex/*` redirects to root. |

## Adding a book / chapter

Cortex is **convention over configuration** — the on-disk layout under `content/cortex/`
*is* the index, no central manifest. Every top-level directory is a book; nested
directories become collapsible sidebar sections; `.md` files are chapters.

1. **Make a directory** under `content/cortex/`. The dirname is the book slug
   (`[a-z0-9_-]+`), served at `/{book-slug}/...`.
2. **Drop in `.md` files** (and subdirectories for sections). Numeric prefixes
   (`01-`, `02-`, …) control ordering and are stripped before display.
3. **(Optional) `book.json`** at the book root: `{ "title", "description", "tags",
   "estimatedReadingMinutes", "order" }` — `order` controls book position on the index.
4. **(Optional) `_section.json`** per section dir: `{ "title": "Foundations" }`.
5. **(Optional) chapter frontmatter** (`title`, `summary`).

The chapter slug is derived from its path: `<book>/01-data-structures/02-arrays/03-traversal.md`
→ `/{book}/data-structures-arrays-traversal`. Collisions are caught at index-load and
surfaced as a server error. In dev (`CORTEX_AUTO_RELOAD=true`) the index rebuilds on mtime
change; in prod it's cached after first request (content is baked into the image).

### A new runnable language

Add an entry to `server/.../codeRunPipeline/Languages.scala` (`RUNNABLE_LANGUAGES`:
go-judge language id + label + aliases), and the matching Prism alias in
`client/src/markdown/`. `Languages` is the single source of truth for language dispatch.

## Frontend styling — Tailwind v4 + BEM

Tailwind v4 is CSS-first (no `tailwind.config.ts`). The entry point is
[`client/tailwind.css`](client/tailwind.css): imports Tailwind, declares the Scala.js
linker output as a `@source`, defines the shadcn HSL theme via `@theme inline`, restores
v3's centered `container`, and imports per-block BEM stylesheets under
[`client/src/styles/{sections,components}/`](client/src/styles/). Each is wrapped in
`@layer components { ... }` so call-site utilities win on conflict.

## Common commands

| Command | What |
|---|---|
| `sbt compile` / `sbt test` | Compile (triggers OpenAPI codegen for `shared`) / run tests |
| `sbt "~server/reStart"` / `sbt "~client/fastLinkJS"` | Backend auto-restart / Scala.js continuous link |
| `cd client && npm run build` | Production bundle to `client/dist/` (runs `client/fullLinkJS`) |
| `docker compose up --build` | Build + bring up all services |
| `docker compose exec db psql -U cortex` / `mongo mongosh cortex` | DB shells |

## API-first workflow

`api/openapi.yaml` is the source of truth. `sbt compile` runs `sbt-openapi-codegen`, which
writes tapir endpoints + case classes into the `shared` module under
`cortex.shared.api.Endpoints`. Backend and frontend compile against those types — schema
drift is a compile error. To change the API: edit `api/openapi.yaml` → `sbt compile` → fix
the resulting errors in `server/` (handlers) and `client/` (`ApiClient`).

## Stack

Scala 3 · sbt · ZIO 2 · zio-http · tapir · Postgres · Liquibase · Redis (Lettuce) ·
MongoDB · go-judge · Keycloak · Scala.js · scalajs-react · Vite · Tailwind v4 · unified ·
remark · rehype · shiki · mermaid · @terrastruct/d2 · KaTeX · D3 · Monaco.
