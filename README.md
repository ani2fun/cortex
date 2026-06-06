# codefolio

A fullstack Scala personal portfolio + interactive Cortex. Frontend
is **Scala.js + scalajs-react** (with a small TypeScript helper module
hosting the unified/remark/rehype markdown pipeline), backend is **ZIO +
zio-http + tapir**, the API contract is **OpenAPI-first** with code
generation, and everything runs in **Docker Compose**.

> **New here?** The full engineering tour lives in the Cortex
> itself. After `./bin/dev`, open
> <http://localhost:5173/cortex/codefolio-onboarding/overview> for a
> walk-through of the architecture, the request lifecycle, the markdown
> pipeline, and how to extend the project. The chapters are also
> readable as plain markdown under
> [`content/cortex/codefolio-onboarding/`](content/cortex/codefolio-onboarding/).

## What's inside

- **Portfolio landing page** — Hero, About, Experience (tab picker),
  Projects, Certifications timeline, Cortex preview, Footer.
- **Cortex** — `/cortex` lists books; `/cortex/<book>/<chapter>`
  renders markdown with KaTeX math, mermaid + D2 diagrams, GFM tables, and
  *runnable* code blocks. The markdown pipeline lives in TypeScript
  (`client/src/markdown/render.ts`) and is invoked from Scala.js through a
  lazy-loaded gateway, so its multi-MB dependency tree doesn't ship with
  the home page.
- **Code execution** — `POST /api/run` proxies to a self-hosted **go-judge**
  sandbox (`EXECUTOR_URL`), same image in dev and prod. The `RunnableCodeBlock`
  UI component lives in scalajs-react and exercises this end-to-end.
- **Hello demo** — kept at `/demo` (the original codefolio skeleton). Hits
  Postgres/Redis/Mongo end-to-end; useful as an integration smoke test.

## Backing stores

| Store | Role |
|---|---|
| **Postgres** | Visit counter for the kept Hello demo (Liquibase migrations, plain JDBC + HikariCP). |
| **Redis** (Lettuce) | Read-through cache for `/api/hello` (~10s TTL) — response carries a `cached` flag. |
| **MongoDB** (sync driver) | Append-only log of every `/api/hello` call; surfaced via `/api/recent`. |
| **go-judge** (sandbox) | `criyle/go-judge` + language toolchains; backs `/api/run` via its `/run` command API. Same privileged image in dev and prod (`EXECUTOR_URL`). |

## Quick start

```bash
docker compose up --build
```

Then open <http://localhost:8080>. Swagger UI is at <http://localhost:8080/docs>.

## Prerequisites

- JDK 21+
- sbt 1.10+
- Node 20+ (for the Vite frontend)
- Docker (for Postgres + Redis + MongoDB + code-runner + the production image)

## Local development — one command

```bash
./bin/dev
```

Brings up Postgres / Redis / Mongo / code-runner (in Docker), an sbt
watcher that re-runs `server/reStart` and `client/fastLinkJS` on source
change, and the Vite dev server on :5173 — all in one terminal with
prefixed, colour-coded logs. Ctrl-C stops everything.

| URL | What |
|---|---|
| <http://localhost:5173> | Frontend with Vite HMR (proxies `/api` and `/docs` to :8080) |
| <http://localhost:8080> | ZIO server (API + static fallback) |
| <http://localhost:8080/docs> | Swagger UI |

### Manual / four-terminal alternative

If you'd rather run pieces separately:

```bash
# A — backing stores
docker compose up db redis mongo code-runner

# B — backend, auto-restarts on source change
sbt "~server/reStart"

# C — Scala.js continuous compile (re-links on change, picked up by Vite HMR)
sbt "~client/fastLinkJS"

# D — Vite dev server (proxies /api and /docs to :8080)
cd client && npm install && npm run dev
```

## Environment variables

| Variable | What | Default |
|---|---|---|
| `PORT` | Server HTTP port | `8080` |
| `STATIC_DIR` | Where the Vite production bundle lives | `./client/dist` (in the prod image: `/app/static`) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres | `jdbc:postgresql://localhost:5432/codefolio` / `codefolio` / `codefolio` |
| `REDIS_URL` | Redis | `redis://localhost:6379` |
| `REDIS_TTL_SECS` | Cache TTL for the Hello payload | `10` |
| `MONGO_URI` / `MONGO_DB` | MongoDB | `mongodb://localhost:27017` / `codefolio` |
| `EXECUTOR_URL` | go-judge sandbox base URL | unset (`http://go-judge:5050` in compose; `http://localhost:5050` in bin/dev) |
| `EXECUTOR_AUTHN_TOKEN` | Optional bearer token (go-judge `ES_AUTH_TOKEN`) | unset |
| `CORTEX_ROOT` | On-disk root of the Cortex content tree | `./content/cortex` (in the prod image: `/app/content/cortex`) |
| `CORTEX_AUTO_RELOAD` | Re-walk the content tree on every index request when its mtime changes (drop a folder, refresh the page). Off in prod where content is baked into the image. | `true` |

If `EXECUTOR_URL` is not configured, `/api/run` returns 503 with a hint to
set it.

## API surface

| Endpoint | Method | What |
|---|---|---|
| `/api/hello` | GET | Increment Postgres counter, cache in Redis, log to Mongo. |
| `/api/recent` | GET | Last 10 `/api/hello` calls, newest first (from Mongo). |
| `/api/health` | GET | 200 if all three stores are reachable. |
| `/api/run` | POST | Execute a code snippet via the go-judge sandbox. |
| `/api/cortex/index` | GET | List books + chapter refs. |
| `/api/cortex/{book}/{chapter}` | GET | Frontmatter + raw markdown + prev/next slugs for one chapter. |
| `/docs` | GET | Swagger UI |
| `/`, `/cortex/...`, `/demo`, `/blogs`, `/assets/*`, `/img/*`, `/certificates/*`, `/Aniket-Kakde-CV-EN.pdf` | GET | Static / SPA-fallback content. |

## Adding content

### A new book / chapter

Cortex is **convention over configuration** — the on-disk layout under
`content/cortex/` *is* the index. There is no central manifest. Every
top-level directory is a book; nested directories become collapsible
sidebar sections; `.md` files are chapters.

```
content/cortex/
└── <book-slug>/                 ← every immediate subdir is a book
    ├── book.json                ← OPTIONAL — title/description/tags/estMins overrides
    ├── 01-overview.md           ← chapter at the book root
    └── 02-foundations/          ← directory = section (renders as a sidebar group)
        ├── _section.json        ← OPTIONAL — `{ "title": "Foundations" }`
        ├── 01-introduction.md
        └── 02-deeper/           ← nest as deep as you want (capped at 6 levels)
            └── 01-details.md
```

To add a new book:

1. **Make a directory** under `content/cortex/`. The dirname is the
   book's slug — must match `[a-z0-9_-]+` and shows up at
   `/cortex/<book-slug>/...`.
2. **Drop in `.md` files** (and optionally subdirectories for sections).
   Use a numeric prefix on filenames + dirnames to control ordering —
   `01-`, `02-`, … the prefix is stripped before display. Without a
   prefix, entries fall back to alphabetic order.
3. **(Optional) `book.json`** at the book root — overrides any of:
   ```json
   { "title": "My Book", "description": "...", "tags": ["x"], "estimatedReadingMinutes": 30 }
   ```
   Without it the title is humanised from the dirname.
4. **(Optional) `_section.json`** in any section dir to pretty-up the
   sidebar header: `{ "title": "Foundations" }`. Without it the title is
   humanised from the dirname (`01-foundations` → `Foundations`).
5. **(Optional) chapter frontmatter** for a custom title/summary:
   ```markdown
   ---
   title: My Chapter
   summary: Optional one-liner shown above the body.
   ---

   ## Section heading
   ...
   ```
   Without frontmatter, the title falls back to the first `# H1` in the
   body, then to the humanised filename.

The chapter slug is derived deterministically from its path: e.g.
`<book>/01-data-structures/02-arrays/03-traversal.md` →
`/cortex/<book>/data-structures-arrays-traversal`. Slug collisions are
caught at index-load time and surfaced as a server error listing the
duplicates — rename one of the colliding segments.

In dev (`CORTEX_AUTO_RELOAD=true`, the default) the server watches
file mtimes and rebuilds its in-memory index automatically, so dropping
a folder and refreshing the browser is enough. In prod the index is
cached after first request — redeploy to pick up changes.

### A one-shot import (e.g. an mdbook tree)

[`scripts/import_dsa.py`](scripts/import_dsa.py) is a worked example:
it walks an external mdbook source, mirrors its directory tree under
`content/cortex/`, rewrites cross-chapter links to cortex URLs, and
auto-tags every Piston-supported fenced code block with ` run` so it
becomes executable. Adapt it (or copy it) for similar imports.

### A new runnable language

Edit `server/.../runner/Languages.scala` to add an entry to
`RUNNABLE_LANGUAGES` (Judge0 language ID + label + aliases), and add the
matching alias mapping in `client/src/markdown/runtime.ts` (Prism alias
table). For Piston-only languages, also add a row to `Piston.scala`'s
`pistonLanguage` map.

## Frontend styling — Tailwind v4 + BEM

Tailwind v4 is configured CSS-first (no `tailwind.config.ts`, no
`postcss.config.mjs`). The single entry point is
[`client/tailwind.css`](client/tailwind.css), which:

- imports Tailwind v4 via `@import "tailwindcss"`
- declares the Scala.js linker output as a `@source` so classes emitted
  into the linked JS make it into the CSS bundle
- defines the shadcn HSL theme variables and bakes them into utilities
  with `@theme inline`
- restores v3's centered `container` behavior with a small
  `@utility container { margin-inline: auto; }` override
- imports per-component BEM stylesheets

Every section / component className is extracted into a
`.block__element--modifier` rule under
[`client/src/styles/sections/`](client/src/styles/sections/) (page-level
sections like Hero, About, Experience) or
[`client/src/styles/components/`](client/src/styles/components/)
(cross-section primitives — cortex-reader shells, diagrams, the
runnable-code editor). Each file is wrapped in `@layer components { ... }`
so utilities applied at the call site (`^.className := "experience pt-40"`)
still win on conflict — same model the project already relies on.

The Scala companion to this is
[`Section`](client/src/main/scala/codefolio/client/components/ui/Section.scala) —
a tiny primitive every home-page section uses to set its `id` (for
hash-link nav) and apply its BEM block class.

## Common sbt commands

| Command | What it does |
|---|---|
| `sbt clean` | Delete all `target/` directories across modules |
| `sbt compile` | Compile everything; triggers OpenAPI codegen for `shared` |
| `sbt test` | Run all tests (currently the shared codegen smoke spec) |
| `sbt server/run` | Run the backend once (no auto-reload) |
| `sbt "~server/reStart"` | Run the backend with auto-restart on source change |
| `sbt server/reStop` | Stop the backgrounded backend started by `reStart` |
| `sbt "~client/fastLinkJS"` | Continuously link Scala.js for development |
| `sbt client/fullLinkJS` | One-shot optimised Scala.js link (production) |
| `sbt server/Universal/stage` | Stage the server launcher |
| `sbt update` | Resolve and download all dependencies |

## Common frontend (npm) commands — run from `client/`

| Command | What it does |
|---|---|
| `npm install` | Install Vite, React, Tailwind, the markdown pipeline deps |
| `npm run dev` | Start the Vite dev server on :5173 (proxies `/api` and `/docs` to :8080) |
| `npm run build` | Production bundle to `client/dist/` (calls `sbt client/fullLinkJS`) |
| `npm run preview` | Locally serve the built `dist/` to verify the production bundle |

## Docker

| Command | What it does |
|---|---|
| `docker compose up --build` | Build the image and bring up all services (db + redis + mongo + code-runner + app) |
| `docker compose up db redis mongo code-runner` | Just the backing stores (used during local dev) |
| `docker compose up app` | Just the app container (assumes the stores are already up) |
| `docker compose down` | Stop and remove containers (volumes preserved) |
| `docker compose down -v` | Same plus drop the `pgdata` and `mongodata` volumes — **wipes the DBs** |
| `docker compose logs -f app` | Tail backend logs |
| `docker compose exec db psql -U codefolio` | Open a `psql` shell |
| `docker compose exec redis redis-cli` | Open a `redis-cli` shell |
| `docker compose exec mongo mongosh codefolio` | Open a `mongosh` shell |

## API-first workflow

`api/openapi.yaml` is the source of truth. `sbt compile` triggers
`sbt-openapi-codegen`, which writes tapir endpoint descriptions and case
classes into the cross-compiled `shared` module under
`codefolio.shared.api.Endpoints`. Both backend and frontend compile against
those generated types — schema drift becomes a compile error.

To change the API:
1. Edit `api/openapi.yaml`.
2. Run `sbt compile`.
3. Fix the resulting compile errors in `server/` (handlers) and `client/`
   (`ApiClient`).

## Trying it out

After `./bin/dev`:

```bash
# Hello demo: cold call
curl http://localhost:8080/api/hello

# Within 10s — cached=true, served from Redis
curl http://localhost:8080/api/hello

# Last 10 hello calls, from MongoDB
curl http://localhost:8080/api/recent

# Liveness across all three stores
curl http://localhost:8080/api/health

# Run a Python snippet via the local code-runner
curl -X POST http://localhost:8080/api/run \
  -H 'Content-Type: application/json' \
  -d '{"language":"python","source":"print(2+2)"}'

# List Cortex books
curl http://localhost:8080/api/cortex/index

# Fetch one chapter (raw markdown + frontmatter)
curl http://localhost:8080/api/cortex/distributed-systems/introduction
```

In the browser:

- <http://localhost:5173/> — landing page
- <http://localhost:5173/cortex> — book index
- <http://localhost:5173/cortex/distributed-systems/introduction> — chapter reader (markdown + mermaid + D2 + KaTeX + runnable code)
- <http://localhost:5173/demo> — kept Hello demo

## Stack

Scala 3 · sbt · ZIO 2 · zio-http · tapir · Postgres · Liquibase · Redis
(Lettuce) · MongoDB · Scala.js · scalajs-react · Vite · Tailwind v4 ·
unified · remark · rehype · shiki · mermaid · @terrastruct/d2 · KaTeX ·
Prism · react-simple-code-editor.
