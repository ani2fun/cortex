---
title: Observability & incidents
summary: What to look at when the site is slow or down — health endpoints, logs, and a first-five-minutes triage that follows the "what saturates first" order.
---

## The signals you have

| Signal | How | Tells you |
|---|---|---|
| **Health** | `curl https://cortex.kakde.eu/api/health` | Per-store status: `{status, postgres, redis, mongo}`. Postgres `down` = real problem; redis/mongo `down` = degraded, not down. |
| **Readiness** | `kubectl -n apps-prod get pods -l app.kubernetes.io/name=cortex` | Is the pod `Ready`? Restart count climbing = crash loop. |
| **Logs** | `kubectl -n apps-prod logs deploy/cortex -f` | Structured Logback output; Liquibase migration lines on startup. |
| **Tutor health** | `curl …/tutor/readyz` (via the proxy) | DB + JWKS + MCP reachability for the coach. |
| **Argo CD** | `argocd app get cortex` | Is the cluster in sync with git, or has something drifted? |

```bash
# the four commands you'll run most
kubectl -n apps-prod get pods -l app.kubernetes.io/name=cortex
kubectl -n apps-prod logs deploy/cortex --tail=200 -f
kubectl -n apps-prod describe pod -l app.kubernetes.io/name=cortex   # events: OOMKilled, probe fails, image pull
curl -s https://cortex.kakde.eu/api/health | jq .
```

## First five minutes when it's slow or down

Work the **"what saturates first"** order — it's the same ranked list the [failure-thresholds chapter](/cortex/system-design/capstones/cortex-failure-thresholds) derives from the architecture, so you're checking causes in their actual likelihood order:

1. **Is the single pod alive and Ready?** `get pods`. If it's `CrashLoopBackOff` or `OOMKilled` (exit 137), that's a full outage — one replica. → `describe pod` for the reason; check recent deploys.
2. **OOMKill?** Memory near the 1Gi limit (`describe` shows `OOMKilled`). A burst of large concurrent responses + GC can cross it. → restart restores service; repeated OOMs mean bump memory or investigate a leak.
3. **Is `/api/run` timing out but pages load?** go-judge is saturated or down. Up to 8 concurrent runs, each heavy run up to ~1Gi. → check the `go-judge` workload; the semaphore is protecting you, but the queue may be draining slowly.
4. **Postgres reachable?** `/api/health` shows `postgres`. The Hikari pool is 10 connections — heavy tutor concurrency can exhaust it. → check the `databases-prod` Postgres.
5. **Redis/Mongo down?** `/api/health`. These **fail open** — the site stays up; you've lost rate-limiting (anon runs no longer capped) and the greeting cache / event log. Degraded, not an outage.

The throughline: **Postgres down or the single pod down = outage; everything else degrades.** That asymmetry (one critical store, the rest fail-open) is a deliberate design choice — [Hello, end-to-end](/cortex/cortex-onboarding/how-it-works/hello-world-end-to-end) is the worked example of it.

## Incident hygiene

- **Capture before you fix.** `kubectl logs --previous` grabs the *crashed* container's logs before a restart erases them.
- **Check Argo CD drift.** If someone `kubectl edit`ed something live, `selfHeal` may be fighting them — `argocd app get cortex` shows out-of-sync resources.
- **Correlate with deploys.** Most incidents follow a change. `kubectl rollout history deploy/cortex` and recent `infra`/`cortex` commits are the first suspects; [rollback](/cortex/cortex-onboarding/runbooks/production/deploy-and-rollback) is one `git revert` away.
- **Tutor cost spikes.** If the Anthropic bill jumps, confirm non-allowlist users are on **BYOK** (their key, not yours) and check `COACH_HOMELAB_USERS` hasn't grown. Per-turn `TurnUsage.costUsd` is recorded, so the spend is measurable, not a mystery.

## What's deliberately not here

This is a homelab, not a NOC — there's no Prometheus/Grafana/alerting wired up by default. The honest observability story is *health endpoints + `kubectl logs` + Argo CD*. Turning the fire-and-forget logs into real metrics and dashboards is a concrete next step, sketched in the [data-intensive chapter](/cortex/system-design/capstones/cortex-data-intensive) — it's where the platform would go to grow up operationally.

> **Next:** that completes the runbooks. To go deeper on *why* these limits exist and what it takes to grow past them, read the [Cortex platform deep-dive](/cortex/system-design/capstones/cortex-platform-overview) in the System Design book.
