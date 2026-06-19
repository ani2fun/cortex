---
title: Runbooks — overview
summary: Copy-paste-able procedures to bring the whole platform up yourself — locally on one machine, and in production on the homelab. Start here to pick your path.
---

> 📓 **A runbook is a recipe, not an essay.** The rest of this book explains *why* Cortex is shaped the way it is. This section is the opposite: terse, ordered, copy-paste-able steps to get the thing **running** — with just enough "why" to recover when a step fails. If you follow a runbook top to bottom and it doesn't work, that's a bug in the runbook; fix it, because it lives in the same repo as the code.

## Two environments, two runbooks

| | [Local Dev](/cortex/cortex-onboarding/runbooks/local-dev) | [Production](/cortex/cortex-onboarding/runbooks/production) |
|---|---|---|
| **Where** | Your laptop | The homelab K3s cluster (4 nodes) |
| **How it starts** | `./bin/dev` (+ the tutor in a second terminal) | ArgoCD syncs from the `infra` repo (GitOps) |
| **Auth** | Off by default (`AUTH_ENABLED=false`) | On (Keycloak `apps-prod` realm) |
| **Stores** | Docker containers on `localhost` | In-cluster Postgres / Redis / Mongo |
| **You want this when** | Writing content, hacking on the app, trying the coach | Operating the live site, deploying, debugging an incident |

If your goal is *"read the books and run some code,"* you don't need any of this — just visit the live site. These runbooks are for **running the platform yourself**: contributing, self-hosting, or operating it.

## The shape of the system you're starting

Before the steps, hold this picture — it's what "up" means. Cortex is one Scala binary; the Tutor is a second (Python) service; both lean on the same backing stores and the same Keycloak. Locally you start the stores with Docker and the two apps by hand; in production ArgoCD does it all.

<iframe
  src="/c4/view/onboarding_cortex_container"
  width="100%"
  height="460"
  style="border: 1px solid var(--border, #2b2b2b); border-radius: 8px;"
  loading="lazy"
  title="Cortex — container view (what 'up' means)"
></iframe>

## Fastest path to a running stack (local)

For the impatient — the four commands, expanded in the [Local Dev](/cortex/cortex-onboarding/runbooks/local-dev) runbooks:

```bash
# BOTH stacks, one command (cortex + cortex-tutor must be siblings under one parent):
./scripts/devcombined           # launches cortex/bin/dev + ../cortex-tutor/bin/dev, wires them, prefixed logs
open http://localhost:5173       # the cortex SPA (the tutor is on :8000)
```

Prefer to run them separately? Each stack has its own launcher:

```bash
./bin/dev                              # cortex alone (SPA :5173, server :8080, stores, go-judge, likec4)
( cd ../cortex-tutor && ./bin/dev )    # the tutor alone (FastAPI :8000, its Postgres :5433)
# if running cortex alone but want the live coach, tell it where the tutor is:
CORTEX_TUTOR_BASE_URL=http://localhost:8000 ./bin/dev
```

## What's in each runbook

**[Local Dev](/cortex/cortex-onboarding/runbooks/local-dev)**

- [Prerequisites](/cortex/cortex-onboarding/runbooks/local-dev/prerequisites) — JDK, sbt, Node, Docker, uv.
- [Launch Cortex](/cortex/cortex-onboarding/runbooks/local-dev/launch-cortex) — `bin/dev` step by step; auth on vs off; what comes up where.
- [Launch the Tutor](/cortex/cortex-onboarding/runbooks/local-dev/launch-the-tutor) — the Python service and how to wire it to the SPA.
- [End-to-end & troubleshooting](/cortex/cortex-onboarding/runbooks/local-dev/end-to-end-and-troubleshooting) — verify a coached turn; the foot-guns and their fixes.
- [Auth & GitHub sign-in on localhost](/cortex/cortex-onboarding/runbooks/local-dev/auth-and-github-signin) — sign in against the *local* Keycloak; register your own GitHub OAuth App for localhost.

**[Production](/cortex/cortex-onboarding/runbooks/production)**

- [Topology](/cortex/cortex-onboarding/runbooks/production/topology) — the 4-node cluster, ArgoCD, the edge.
- [Deploy & rollback](/cortex/cortex-onboarding/runbooks/production/deploy-and-rollback) — the GitOps flow; how a change reaches the site; how to revert.
- [Secrets & auth](/cortex/cortex-onboarding/runbooks/production/secrets-and-auth) — sealed secrets, the Keycloak realm.
- [Scaling & DR](/cortex/cortex-onboarding/runbooks/production/scaling-and-dr) — the knobs you have today, and the disaster-recovery drill.
- [Observability & incidents](/cortex/cortex-onboarding/runbooks/production/observability-and-incidents) — health, logs, and what to check first when it's slow.
- [Serving performance & the Cloudflare edge](/cortex/cortex-onboarding/runbooks/production/serving-performance-and-edge) — compression, immutable caching, the lazy landing bundle, and the Cloudflare proxy; the `curl` proofs, the measured before/after, and a cluster smoke-test.

**Cross-cutting**

- [Access & allowlists](/cortex/cortex-onboarding/runbooks/access-and-allowlists) — grant or revoke who may **Submit code** and **Save coach history** (one Postgres table, checked live, no restart); plus the separate coach model-tier knob. Applies to both local dev and production.

> **Next:** [Local Dev → Prerequisites](/cortex/cortex-onboarding/runbooks/local-dev/prerequisites).
