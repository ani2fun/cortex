---
title: '13. The full path: git push → pod'
summary: Follow a single change end-to-end — from a git push on your laptop, through CI building an image, GitOps detecting the change, and Kubernetes rolling out a new Pod that serves cortex.kakde.eu.
---

# 13. The full path: git push → pod

## TL;DR

> Deploying Cortex is *fully automated and git-driven*. You **`git push`** a change. **CI (GitHub Actions)** builds the Docker image (Chapter 3) and pushes it to a registry (**ghcr.io**). You update the **manifest** (the image tag) in a git repo, and **GitOps (Argo CD)** — which continuously watches that repo as the *source of truth for desired state* — detects the change and applies it to the cluster. Kubernetes performs a **rolling update** (Chapter 14), and a new Pod serves `cortex.kakde.eu`. No one runs `kubectl apply` by hand; **git is the single source of truth**, and the cluster continuously reconciles itself toward what git says.

## 1. Motivation

We've met every Kubernetes piece in isolation — Deployment, Service, Ingress, probes, resources, security. This chapter assembles them into the *workflow*: how a change actually gets from your editor to production. And it introduces the idea that ties Kubernetes' "desired state" (Chapter 5) to your *daily practice*: **GitOps**. 

Here's the problem GitOps solves. Kubernetes is declarative — you give it manifests describing desired state. But *who* gives it those manifests, and how do you keep the cluster honest? If engineers run `kubectl apply` from their laptops, you get the old chaos back: undocumented changes, drift between "what's in git" and "what's actually running," no audit trail, no easy rollback, and the terror of "nobody's sure what's deployed." GitOps fixes this by making a **git repository the single source of truth for desired state**, and putting an agent *in the cluster* (Argo CD) that continuously *pulls* from git and reconciles the cluster to match. Now "deploy" means "merge to git," every change is reviewed and audited like code, the running state provably matches the repo, and rollback is `git revert`. The cluster's self-healing (Chapter 5) extends all the way out to *configuration*: drift from git is detected and corrected, just like a crashed Pod. This is how Cortex — and most modern Kubernetes shops — actually ship.

## 2. Intuition (Analogy)

A **published, version-controlled blueprint that the building continuously matches itself to.** Imagine a building that doesn't get renovated by contractors showing up and changing things on a whim. Instead, there's an *official master blueprint* in a vault (git), and a tireless inspector (the GitOps agent) who *constantly compares the actual building to the blueprint* and dispatches crews to fix any difference. Want to change the building? You don't touch the building — you *amend the blueprint* (a reviewed, recorded change), and the inspector makes reality match. Someone secretly moves a wall? The inspector notices it doesn't match the blueprint and *puts it back*.

That's GitOps. The blueprint is your git repo of manifests; the inspector is Argo CD; the building is the cluster. You never change production directly — you change the *declared desired state in git*, and the agent reconciles the cluster to it (and corrects any drift). "What's deployed" is *exactly* "what's in git," always, by construction.

## 3. Formal Definition

- **CI (Continuous Integration).** On a `git push`, an automated pipeline (**GitHub Actions** for Cortex) builds and tests. For Cortex it runs the multi-stage Docker build (Chapter 3) and **pushes the image** to a container registry.
- **Container registry.** Where images live (Cortex uses **ghcr.io**, GitHub's registry: `ghcr.io/ani2fun/cortex`). Kubernetes *pulls* images from here to run them.
- **Manifest / desired-state repo.** A git repo holding the Kubernetes YAML (Deployment, Service, Ingress, …) — the *declared desired state*. A deploy is a commit here changing the image tag (or any config).
- **GitOps.** An operating model where **git is the single source of truth** for desired state, and an in-cluster agent continuously **pulls** from git and **reconciles** the cluster to match — applying changes and correcting drift. (Pull-based, unlike a CI pipeline *pushing* `kubectl apply`.)
- **Argo CD** (Cortex's GitOps agent). Watches the manifest repo; when it changes, syncs the cluster to it; flags/repairs drift. ([Homelab from Scratch → App-of-apps](/cortex/homelab-from-scratch/secrets-and-gitops/the-app-of-apps-pattern) builds this.)
- **The rollout.** When the Deployment's image changes, Kubernetes performs a **rolling update** (Chapter 14): new Pods up (readiness-gated, Chapter 10), old Pods drained, zero downtime.
- **The end-to-end path:** `git push` → CI builds + pushes image → update manifest in git → Argo CD detects + syncs → Deployment rolls out → Service routes to the new ready Pod → Ingress serves it at `cortex.kakde.eu`.

> Deploying = changing git. CI builds the image to a registry; GitOps (Argo CD) reconciles the cluster to the manifests in git; Kubernetes rolls out the new Pod. Git is the source of truth; the cluster continuously matches it.

## 4. Worked Example — a Cortex deploy, end to end

You add a chapter to Cortex and want it live. The entire path:

```d2
direction: right

dev: "You\n(git push)" {shape: person}
ci: "GitHub Actions\n(build image)" {shape: rectangle}
reg: "ghcr.io\n(image registry)" {shape: cylinder}
gitops_repo: "manifest repo\n(desired state)" {shape: cylinder}
argo: "Argo CD\n(GitOps agent in cluster)" {shape: hexagon}
k8s: "Kubernetes\n(rolling update)" {shape: rectangle}
pod: "New Pod\nserving cortex.kakde.eu" {shape: rectangle}

dev -> ci: "1. push"
ci -> reg: "2. build + push image"
ci -> gitops_repo: "3. update image tag"
gitops_repo -> argo: "4. Argo detects change"
argo -> k8s: "5. sync (apply manifest)"
k8s -> pod: "6. roll out (readiness-gated)"
```

Walk it. **(1)** You `git push`. **(2)** GitHub Actions runs the multi-stage Dockerfile (Chapter 3), producing a new image, and **pushes it** to `ghcr.io/ani2fun/cortex` with a new tag. **(3)** The pipeline updates the image tag in the *manifest repo* (the desired-state git repo) — a commit that *declares* "the Cortex Deployment should now run this new image." **(4)** **Argo CD**, running *in the cluster* and continuously watching that repo, sees the commit. **(5)** It **syncs**: applies the changed Deployment manifest to Kubernetes — making the *cluster's desired state* match *git's*. **(6)** The Deployment controller notices its Pod template changed and performs a **rolling update** (Chapter 14): it starts a new Pod with the new image, waits for it to pass readiness (Chapter 10 — including the startup migration, Part 2!), then retires the old Pod. The Service (Chapter 7) routes to the new ready Pod, and the Ingress (Chapter 8) serves it at `cortex.kakde.eu`. 

Notice every concept from this Part appears, in concert: the *image* (Ch 3), the *registry*, the *Deployment* rolling out (Ch 6, 14), *readiness* gating the cutover (Ch 10), the *Service* and *Ingress* exposing it (Ch 7–8) — all triggered by a *git commit*, with *no manual cluster command*. And the desired-state loop (Ch 5) now spans the whole system: git declares intent, Argo reconciles config, Kubernetes reconciles runtime. If someone manually `kubectl edit`s the Deployment, Argo notices the drift from git and *reverts it* — the cluster is continuously pulled back to what git says.

## 5. Build It

Run this. It models the GitOps pipeline: a git change flows to an image, a manifest update, and a cluster sync — and drift gets corrected.

```python run
# Git is the source of truth for desired state.
git_manifest = {"image": "ghcr.io/ani2fun/cortex:v1"}
cluster_running = {"image": "ghcr.io/ani2fun/cortex:v1"}   # actual state

def ci_build_and_push(new_tag):
    print(f"   CI: built + pushed ghcr.io/ani2fun/cortex:{new_tag}")
    git_manifest["image"] = f"ghcr.io/ani2fun/cortex:{new_tag}"   # commit the new tag to git
    print(f"   CI: updated manifest in git -> {git_manifest['image']}")

def argo_reconcile():
    # The GitOps loop: make the cluster match git (apply changes AND correct drift).
    if cluster_running["image"] != git_manifest["image"]:
        print(f"   Argo CD: drift detected (cluster={cluster_running['image']} != git={git_manifest['image']})")
        print(f"   Argo CD: syncing -> rolling update to {git_manifest['image']}")
        cluster_running["image"] = git_manifest["image"]
    else:
        print("   Argo CD: cluster matches git -> nothing to do")

print("== deploy a new version (git push) ==")
ci_build_and_push("v2")
argo_reconcile()
print("   now serving:", cluster_running["image"], "\n")

print("== someone manually edits the cluster (drift) ==")
cluster_running["image"] = "ghcr.io/ani2fun/cortex:hotfix-by-hand"
argo_reconcile()                                  # Argo reverts it to match git
print("   after reconcile:", cluster_running["image"], "(git is the source of truth)")
```

**Now break it.** In the drift section, the manual `kubectl`-style edit is *undone* by Argo, which restores what git declares — exactly the GitOps guarantee that "running state == git." Then try to "deploy" by editing `cluster_running` directly instead of `git_manifest`: the next `argo_reconcile()` *reverts your change*, because you changed reality without changing the *declared intent*. The only way to make a change *stick* is to change git. That discipline — deploy by commit, never by hand — is what gives you audit trails, reviews, reproducibility, and `git revert` rollback (Chapter 14). The cluster's relentless reconciliation, which heals crashed Pods (Chapter 5), now also heals configuration drift.

## 6. Trade-offs & Complexity

| GitOps (git-driven, pull-based) | Manual `kubectl apply` |
|---|---|
| Git is the source of truth; no drift | "What's deployed?" is a mystery |
| Every change reviewed + audited (it's a commit) | Untracked, unreviewed changes |
| Rollback = `git revert` | Hope you remember the old state |
| Self-healing config (drift corrected) | Drift accumulates silently |
| Agent + repos + CI to set up | "Just run kubectl" (until chaos) |

The cost is genuine setup and conceptual overhead: a CI pipeline, a container registry, a manifest repo, and a GitOps agent (Argo CD) installed and configured — plus learning the model. For a one-off deploy or a learning cluster, `kubectl apply` is faster to start. But for a system you *operate over time*, GitOps's payoff is enormous: deploys become reviewed commits, the cluster's state is always knowable (it's in git), drift self-corrects, and rollback is trivial. It's the same "declare desired state, let controllers reconcile" idea from Chapter 5, scaled up from "3 replicas" to "the entire configuration of the cluster." Cortex uses it precisely so that operating it is *boring* — which, for production, is the highest compliment.

## 7. Edge Cases & Failure Modes

- **Manual changes that drift from git.** `kubectl edit` in production fights the GitOps agent, which reverts it (and *should*). Make changes in git, not the cluster.
- **Image built but manifest not updated.** CI that pushes an image but forgets to bump the manifest tag means nothing deploys — the cluster still runs the old image. The *manifest* commit is what triggers the rollout.
- **A bad image that passes CI.** CI green doesn't guarantee a healthy runtime; the *rollout* (readiness probes, Chapter 10) is the real gate. A broken image fails readiness, stalls the rollout, and keeps the old version serving (Part 2's fail-fast synergy).
- **Registry/auth issues.** If the cluster can't pull the image (registry down, bad credentials), Pods stay `ImagePullBackOff`. The manifest is right; the *image fetch* failed — check registry access.

## 8. Practice

> **Exercise 1 — Order the steps.** Put in order: Argo syncs; you git push; CI pushes the image; rolling update; CI updates the manifest tag; the new Pod passes readiness.

<details>
<summary><strong>Answer</strong></summary>

The end-to-end path (§3), in order:

1. **You `git push`** — the change to the source repo, which triggers CI.
2. **CI pushes the image** — GitHub Actions runs the multi-stage Docker build (Chapter 3) and pushes the new image to `ghcr.io/ani2fun/cortex`.
3. **CI updates the manifest tag** — the pipeline commits the new image tag to the *manifest repo* (the declared desired state). *This* commit is what actually triggers a deploy.
4. **Argo syncs** — Argo CD, watching the manifest repo from inside the cluster, sees the commit and applies the changed Deployment, making the cluster's desired state match git's.
5. **Rolling update** — the Deployment controller notices its Pod template changed and starts a new Pod (Chapter 14).
6. **The new Pod passes readiness** — only once `/api/health` is OK (Chapter 10, including the startup migration) is the new Pod counted available and the old one retired.

The key ordering insight: the **image push (2) and the manifest commit (3) are distinct**, and it's the *manifest* commit that triggers the rollout — push an image but forget to bump the tag, and nothing deploys (§7). And readiness (6) gates the cutover: a broken Pod never passes, so the rollout stalls on the old version rather than going down.

</details>

> **Exercise 2 — Why git as truth?** In two sentences, explain what GitOps gives you that ad-hoc `kubectl apply` doesn't, and how drift gets corrected.

<details>
<summary><strong>Answer</strong></summary>

GitOps makes a git repo the **single source of truth** for desired state, so every deploy is a *reviewed, audited commit* — which means the running state is always knowable (it's exactly what's in git), and rollback is just `git revert` — whereas ad-hoc `kubectl apply` leaves undocumented, unreviewed changes and the perennial "what's actually deployed?" mystery (§6). **Drift** is corrected because an in-cluster agent (Argo CD) *continuously pulls* from git and *reconciles* the cluster to match — so if someone `kubectl edit`s production by hand, Argo notices it no longer matches git and *reverts it*, the same self-healing loop from Chapter 5 extended from "3 replicas" all the way to the entire cluster configuration.

</details>

> **Exercise 3 — Trace a rollback.** A deploy is bad. Using GitOps, how do you roll back, and why is it as simple as it is? (Preview of Chapter 14.)

<details>
<summary><strong>Answer</strong></summary>

**How:** `git revert` the bad manifest commit (the one that bumped the image tag). That produces a new commit re-declaring the *previous* desired state — the old image tag. Argo CD sees the manifest repo changed, syncs the cluster to match, and Kubernetes performs a rolling update *back* to the old version. You roll back the *exact same way* you rolled forward: by changing git.

**Why it's so simple:** under GitOps, "what's deployed" is *defined* by git, and git keeps the full, immutable history of every desired state you've ever declared. So a previous good state isn't something you have to *reconstruct from memory* — it's *already recorded*, one revert away. The reconciliation loop (§1) that heals crashed Pods does the rest: declare the old intent, and the agent makes reality match. (And if a bad image fails *readiness*, the rollout already stalled with the old version serving — Chapter 10's fail-fast synergy — so the revert often just cleans up a deploy that never fully took over. Chapter 14 covers `kubectl rollout undo` as the non-GitOps equivalent.)

</details>

```quiz
{
  "prompt": "In Cortex's GitOps workflow, how does a new version actually get deployed to the cluster?",
  "input": "Choose one:",
  "options": [
    "A commit changes the manifest (desired state) in git; an in-cluster agent (Argo CD) detects it and reconciles the cluster to match, triggering a rolling update — git is the single source of truth, no manual kubectl",
    "An engineer runs `kubectl apply` from their laptop for every deploy",
    "Kubernetes recompiles the source code on the node",
    "The image is emailed to the cluster"
  ],
  "answer": "A commit changes the manifest (desired state) in git; an in-cluster agent (Argo CD) detects it and reconciles the cluster to match, triggering a rolling update — git is the single source of truth, no manual kubectl"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Argo CD docs](https://argo-cd.readthedocs.io/)** & **[GitOps principles (OpenGitOps)](https://opengitops.dev/)** — the pull-based, git-as-truth model Cortex uses.
- **[GitHub Actions](https://docs.github.com/en/actions)** — the CI that builds and pushes Cortex's image to ghcr.io.
- **[Homelab from Scratch — GitOps with Argo CD](/cortex/homelab-from-scratch/secrets-and-gitops/install-argocd-on-wk-2)** — building the exact GitOps pipeline that deploys Cortex.

---

**Next:** step 6 of the path was a *rolling update*. Let's watch it in detail — new Pods replacing old with zero downtime — and how a bad deploy rolls back. → [14. Rollouts and rollbacks](/cortex/production-engineering/docker-kubernetes/cortex-in-production/rollouts-and-rollbacks)
