---
title: Prerequisites
summary: The five tools you need on your machine before anything else, with a one-line check for each.
---

You need five things installed. Cortex (the Scala app) needs the first four; the Tutor (the Python service) adds the fifth.

| Tool | Why | Pinned by |
|---|---|---|
| **JDK 21+** | The server + build run on the JVM (17 works; Liquibase prefers 21). | `.tool-versions` |
| **sbt** | The Scala build tool (drives codegen, server, client). | `project/build.properties` |
| **Node 20+** & npm | Vite bundles the Scala.js output; Tailwind v4 runs as a Vite plugin. | `.tool-versions` |
| **Docker** + compose | Backing stores (Postgres / Redis / Mongo), the **go-judge** sandbox, the **likec4** renderer, and optionally Keycloak. | `docker-compose.yml` |
| **uv** | The Tutor's Python (3.12) package/venv manager. *Only needed for the coach.* | `cortex-tutor/pyproject.toml` |

> **Tip:** `.tool-versions` pins exact JDK/sbt/Node versions for [asdf](https://asdf-vm.com) / [mise](https://mise.jdx.dev). If you use either, `mise install` (or `asdf install`) in the repo root gets you the right versions in one shot.

## One-line check

Run this from the `cortex` repo root — every line should print a version, not "command not found":

```bash
java -version && sbt --version && node -v && docker compose version
```

And for the Tutor (from anywhere):

```bash
uv --version
```

If `uv` is missing: `curl -LsSf https://astral.sh/uv/install.sh | sh` (macOS/Linux), then re-open your shell.

## Disk and first-run downloads

The first build pulls a lot, once:

- **sbt** downloads ~1 GB of Scala/JVM dependencies on the first `compile` (3–10 min).
- **go-judge** builds a ~1.9 GB image (it layers every language toolchain — Python, JDK, `scala-cli` with a warm coursier cache — onto [`criyle/go-judge`](https://github.com/criyle/go-judge)).
- **npm** installs the client's `node_modules` on first `bin/dev`.

Budget ~15 minutes and a few GB of disk for a cold start. Every run after is fast.

## The repos you'll want side by side

The coach lives in a *separate* repo. Clone both as siblings so the runbooks' relative paths line up:

```
homelab/
├── cortex/          # this repo — the Scala app + the books
└── cortex-tutor/    # the Python coach (only if you want the live tutor)
```

(Operators also have a third sibling, `infra/`, for the production manifests — that's the [Production runbooks](/cortex/cortex-onboarding/runbooks/production).)

> **Next:** [Launch Cortex](/cortex/cortex-onboarding/runbooks/local-dev/launch-cortex).
