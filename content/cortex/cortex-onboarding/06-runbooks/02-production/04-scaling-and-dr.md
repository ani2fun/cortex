---
title: Scaling & DR
summary: The scaling knobs you have today (and the one that isn't safe to just bump), backups, and the cold-metal disaster-recovery drill.
---

## Scaling knobs you have today

| Knob | Where | Effect | Caveat |
|---|---|---|---|
| **Vertical** (CPU/RAM) | `deployment.yaml` resources | Bigger pod absorbs more readers + bigger code runs | Bounded by the node; 1Gi already OOM-kills near the limit |
| **go-judge capacity** | the shared `go-judge` workload | More headroom for concurrent `/api/run` | The real bottleneck for the playground (see below) |
| **Replicas** | `replicas:` | More web pods | ⚠️ **not a free bump today** — read on |

> ⚠️ **Why `replicas: 2` isn't a no-op.** Two things are currently per-pod: the **in-memory content index** (fine — each pod rebuilds it, content is identical) and the **`/api/run` concurrency semaphore of 8** (*not* fine — two pods means a global cap of 16, not 8, so go-judge could see double the concurrent load). And the rate limiter is Redis-backed (shared, so that's fine). Scaling the web tier safely means moving the semaphore to a **Redis-backed distributed limiter** first. That migration — plus autoscaling the executor fleet — is exactly the [scaling roadmap in the System Design book](/cortex/system-design/capstones/scaling-cortex-like-leetcode). Until then, treat 1 replica as the supported configuration.

For *real* scale (LeetCode-shaped load), the staged path — stateless web tier → autoscaled go-judge fleet → pooled/replicated stores → CDN → async judge queue — is worked out end to end, with capacity numbers, in the [Cortex platform deep-dive](/cortex/system-design/capstones/cortex-platform-overview).

## Backups

Two things must be backed up off-cluster; everything else is reconstructible from git:

```bash
scripts/dr/postgres-backup.sh ~/secure-dr-backups/            # all DBs incl. cortex + tutor schema + keycloak
scripts/dr/sealed-secrets-key-backup.sh ~/secure-dr-backups/  # the master key that decrypts every SealedSecret
```

- **Postgres** holds the only *unique* state: visit counts, and every tutor session/message. Mongo (hello events) and Redis (cache + counters) are non-critical and fail-open — losing them costs nothing durable.
- **The sealed-secrets master key** is what makes the committed `SealedSecret`s decryptable. Without it, you can rebuild the cluster but not its secrets.

## Disaster recovery: the cold-metal rebuild

The `infra` repo carries a full, copy-pasteable **[DR runbook](https://github.com/ani2fun/infra/blob/main/deploy/dr/RUNBOOK.md)** (`deploy/dr/RUNBOOK.md`) that rebuilds the entire cluster from blank Ubuntu installs. It is layered, and **each layer has a gate** you verify before continuing:

| Layer | Brings up |
|---|---|
| 0 | Host OS prep (all four nodes; vm-1 last for the edge allowlist) |
| 1 | Router port-forwards + Cloudflare DNS |
| 2 | WireGuard mesh |
| 3 | K3s + Calico |
| **4** | **Sealed-Secrets controller + master-key restore** ← the linchpin |
| 5 | Traefik + edge firewall |
| 6 | cert-manager + ClusterIssuers |
| 7 | Argo CD + the Applications (cortex, cortex-tutor, go-judge, likec4, …) |
| 8 | PostgreSQL + DB restore |
| 9 | Keycloak (realm) |
| 10 | Public-reachability verification |

After Layer 7, Argo CD syncs the apps — and they'll **CrashLoopBackOff until Layer 8 brings up Postgres**. That's expected, not a failure. Final verification curls every public host for a 200.

> **The order matters.** Secrets (L4) before the apps that need them (L7) before the database they connect to (L8). Following the runbook out of order is the fastest way to a confusing half-broken cluster.

## A realistic "scale it for a workshop" checklist

If you expect a burst (say you're running a class on the site):

1. **Bump go-judge** headroom first — it's the playground bottleneck.
2. **Bump cortex CPU/RAM** vertically (still 1 replica).
3. **Confirm BYOK** is the default for non-allowlist users, so coach token cost scales with *their* keys, not yours (the [cost model](/cortex/system-design/capstones/cortex-storage-and-cost) shows why this is the difference between $7 and $5,000 a month).
4. Watch `/api/health` and go-judge saturation (next runbook).

> **Next:** [Observability & incidents](/cortex/cortex-onboarding/runbooks/production/observability-and-incidents).
