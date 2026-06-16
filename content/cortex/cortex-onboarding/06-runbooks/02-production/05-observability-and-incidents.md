---
title: Observability & incidents
summary: What to look at when the site is slow or down — health endpoints, logs, and a first-five-minutes triage that follows the "what saturates first" order.
---

## The signals you have

| Signal | How | Tells you |
|---|---|---|
| **Health** | `curl https://cortex.kakde.eu/api/health` | Per-store status: `{status, postgres, redis, mongo}`. Postgres `down` = real problem; redis/mongo `down` = degraded, not down. |
| **Readiness** | `kubectl -n apps-prod get pods -l app.kubernetes.io/name=cortex` | Is the pod `Ready`? Restart count climbing = crash loop. |
| **Logs** | `kubectl -n apps-prod logs deploy/cortex -f` (live tail), or **Grafana → Explore → VictoriaLogs** for history | Structured Logback output + Liquibase migration lines. VictoriaLogs retains 30d, so logs survive pod restarts (plain `kubectl logs` doesn't). |
| **Tutor health** | `curl …/tutor/readyz` (via the proxy) | DB + JWKS + MCP reachability for the coach. |
| **Argo CD** | `argocd app get cortex` | Is the cluster in sync with git, or has something drifted? |
| **Metrics** | Grafana — `grafana.kakde.eu` (GitHub login, `ani2fun`) | Container CPU/memory vs the 1Gi limit, restart count, OOMKills, node pressure — trended over time, not just at the moment of failure. See *Metrics & dashboards* below. |

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

## Metrics, logs & dashboards

The homelab grew a metrics stack (June 2026), so the observability story is no longer just health endpoints + logs. In the `monitoring` namespace: **VictoriaMetrics** (single-node TSDB) + **vmagent** (scraper) + **node-exporter** + **kube-state-metrics**, with **Grafana** at **`grafana.kakde.eu`** as the front end (sign in with GitHub — `ani2fun` only). It's Argo-synced from `infra/deploy/apps/monitoring/`, same as everything else.

What it buys you *for cortex today, with zero app changes*: cortex's pod is already scraped via cAdvisor + kube-state-metrics, so you can watch its **container CPU / memory against the 1Gi limit**, **restart count**, and **OOMKills** on a dashboard and over time — the same failure modes from the triage list above, but trended instead of only visible in `describe pod` at the moment it dies. node-exporter also tells you whether `wk-1` (where go-judge runs untrusted code) is under real memory pressure.

**Logs, too — also zero app changes.** A **VictoriaLogs** store plus a **Vector** DaemonSet on every node ship every pod's stdout/stderr into the same Grafana. Open **Explore → VictoriaLogs** and query with [LogsQL](https://docs.victoriametrics.com/victorialogs/logsql/): `namespace:apps-prod pod:cortex*` for cortex (add `error` to filter, `_time:5m` to scope). Retention is 30d, so unlike `kubectl logs` **a crashed pod's logs are still there afterwards** — the "capture before you fix" step above becomes a query, not a race against the restart.

To get cortex's *application* metrics — request rates, latencies, the Hikari pool's 10 connections, tutor turn costs — the backend has to expose them; the scrape side is already waiting:

1. Add a Prometheus endpoint to the Scala backend (Micrometer `PrometheusMeterRegistry` at `/metrics`).
2. Add scrape annotations to the cortex Deployment in `infra` — vmagent's `annotated-pods` job finds them automatically, no central config change:
   ```yaml
   spec:
     template:
       metadata:
         annotations:
           prometheus.io/scrape: "true"
           prometheus.io/port: "8080"
           prometheus.io/path: "/metrics"
   ```
3. Build a cortex dashboard in Grafana against the `VictoriaMetrics` datasource.

Stack internals and ops live in the homelab guide ([Observability with VictoriaMetrics and Grafana](https://notebook.kakde.eu/infrastructure/k8s-homelab/17-observability-and-monitoring.html)) and the `infra` DR runbook (Layer 11).

## What's still deliberately not here

This is still a homelab, not a NOC. **Distributed tracing** (Tempo) and **alerting / paging** (Alertmanager) are not wired up — on purpose, until the need is real. Metrics + logs answer "is it healthy, what is it doing, and what did it just say"; the jump to traces and paging is sketched in the [data-intensive chapter](/cortex/system-design/capstones/cortex-data-intensive), where the platform grows up operationally.

> **Next:** that completes the runbooks. To go deeper on *why* these limits exist and what it takes to grow past them, read the [Cortex platform deep-dive](/cortex/system-design/capstones/cortex-platform-overview) in the System Design book.
