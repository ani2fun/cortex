---
title: Deploy & rollback
summary: How a commit becomes the live site through GitOps, how to force or watch a sync, and the two ways to roll back.
---

Production deploys are **GitOps**: you don't `kubectl apply`, you `git push`, and Argo CD reconciles the cluster to match. There are two source repos with two different triggers.

## Deploying an app or content change (the `cortex` repo)

A change to the Scala code *or the book content* (these very pages) ships as a **new container image**:

1. Merge to `main` in the `cortex` repo.
2. **GitHub Actions** builds and pushes `ghcr.io/ani2fun/cortex:latest` (and rebuilds the **likec4** image when any `content/cortex/**/c4/*.c4` changes).
3. Argo CD notices the new image (or you restart the rollout to pull it) and the new pod rolls out behind the readiness gate.

Because the content is **baked into the image** (`CORTEX_AUTO_RELOAD=false` in prod), publishing a chapter edit means shipping a new image — there's no live content reload in production. That's deliberate: it makes every deploy reproducible and atomic.

```bash
# force a fresh pull of :latest after CI publishes (from ms-1)
kubectl -n apps-prod rollout restart deploy/cortex
kubectl -n apps-prod rollout status  deploy/cortex   # watch the readiness-gated rollout
```

> **Pin images for real releases.** `:latest` is convenient for a personal homelab but means "newest build." For a reproducible deploy, set a digest/tag in the overlay and let Argo CD sync the manifest change instead of restarting.

## Deploying a manifest change (the `infra` repo)

A change to *how* it runs — replicas, resources, env, ingress — is a change to the Kubernetes manifests:

1. Edit under `infra/deploy/apps/cortex/` (base) or `…/overlays/prod/` (prod-specific).
2. Merge to `main` in the `infra` repo.
3. Argo CD's **automated sync** (`prune: true, selfHeal: true`) applies it within its sync window.

The Argo CD Application that wires this up ([`infra/deploy/platform/argocd/applications/cortex.yaml`](https://github.com/ani2fun/infra)) points at `deploy/apps/cortex/overlays/prod` and syncs into `apps-prod`. The same pattern exists for `cortex-tutor`, `go-judge`, and `likec4`.

```bash
# inspect / nudge sync (from ms-1, or the argocd CLI)
kubectl -n argocd get applications
argocd app get cortex
argocd app sync cortex          # force an immediate sync
```

## Watching a deploy

```bash
kubectl -n apps-prod get pods -l app.kubernetes.io/name=cortex -w
kubectl -n apps-prod describe pod -l app.kubernetes.io/name=cortex   # events: pulls, probe failures
curl -sI https://cortex.kakde.eu/api/health                          # 200 when the new pod is ready
```

The rollout is **readiness-gated**: the startup probe hits `/api/health` (up to ~150s of grace for the JVM + Liquibase to come up), and traffic only shifts once readiness passes — so a broken build fails to become ready rather than taking the site down. With **1 replica**, though, a `Recreate`-style gap is possible; the brief unavailability is the cost of the single-replica simplicity (see [Scaling & DR](/cortex/cortex-onboarding/runbooks/production/scaling-and-dr)).

## Rolling back

Two clean options, in order of preference:

**1. Revert the git commit** (the GitOps way — keeps git and the cluster in agreement):

```bash
# in whichever repo caused it (infra for manifests, cortex for app/content)
git revert <bad-sha> && git push      # Argo CD syncs the cluster back
```

**2. Roll back the Deployment** (faster, but now git and cluster disagree until you also revert):

```bash
kubectl -n apps-prod rollout undo deploy/cortex
```

Prefer the revert. With `selfHeal: true`, Argo CD will try to drag the cluster back toward git anyway, so a `kubectl`-only rollback is temporary unless git agrees.

## The Liquibase angle

The server runs **Liquibase migrations on startup**. A migration that fails stops the pod from becoming ready — which, on a single replica, means the rollout stalls on the old pod (good — no half-migrated state serving traffic) or, if the old pod is already gone, a brief outage. Test migrations locally first, and remember: a forward-only migration can't be "rolled back" by reverting the image — plan migrations to be backward-compatible across one deploy (expand/contract), the discipline the [Production Engineering → Liquibase](/cortex/production-engineering/liquibase) part teaches.

> **Next:** [Secrets & auth](/cortex/cortex-onboarding/runbooks/production/secrets-and-auth).
