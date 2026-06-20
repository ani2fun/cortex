---
title: '14. Rollouts and rollbacks'
summary: A Deployment replaces old Pods with new ones gradually — new up, old drained, readiness-gated — so a release never drops a request. And if the new version is bad, rolling back is one command. Watch it happen.
---

# 14. Rollouts and rollbacks

## TL;DR

> When a Deployment's image changes, Kubernetes performs a **rolling update**: it brings up **new** Pods (a fresh ReplicaSet) and drains the **old** ones *gradually*, governed by **`maxSurge`** (how many extra Pods above desired) and **`maxUnavailable`** (how many below). Crucially, each new Pod must pass its **readiness probe** (Chapter 10) before an old one is retired — so there are always enough healthy Pods serving, and the release drops **zero requests**. If the new version is bad (fails readiness, or you spot a bug), **rollback** is one command (or a `git revert` under GitOps) — Kubernetes scales the old ReplicaSet back up. Watch the cutover below.

## 1. Motivation

Replacing a running app is the riskiest routine operation in production, and the naive ways are both bad. *Stop everything, swap, restart* (recreate) causes **downtime** — a gap where the app serves nothing. *Swap all at once* (if the new version is broken) causes a **total outage** — you've replaced every healthy Pod with a broken one simultaneously. Neither is acceptable for a service people rely on. What you want is to replace the fleet *gradually and safely*: introduce the new version a little at a time, *verify each new Pod is actually healthy before removing an old one*, keep enough capacity serving throughout, and be able to *instantly reverse* if something's wrong.

That's a rolling update, and it's one of the headline reasons to use Kubernetes (Chapter 5). It turns "deploy a new version" from a held-breath maintenance-window event into a routine, zero-downtime, *reversible* operation. The two ingredients that make it *safe* (not just gradual) are the **readiness probe** — which ensures a new Pod is genuinely ready before it takes traffic and before an old Pod is retired — and **rollback** — which makes a bad deploy a non-event because you can revert in seconds. Together with GitOps (Chapter 13), this is what lets a team deploy frequently and fearlessly: every release is gradual, verified, and undoable.

## 2. Intuition (Analogy)

**Replacing the staff on a 24/7 help desk without ever leaving the phones unattended.** You don't send all the old agents home and *then* bring the new ones in (the phones ring unanswered — downtime). And you don't swap them all at once before the new agents have proven they can handle calls (chaos if they can't). Instead: you bring in *one* new agent, *verify they're handling calls correctly* (readiness), and only *then* let *one* old agent leave. Repeat — new in, verified, old out — until the whole shift is replaced. The phones are *never* unattended, and there are always enough capable agents on the line.

And if a new agent turns out to be terrible? You *immediately bring the experienced agents back* (rollback) — you kept them on call precisely because the new batch was unproven. A rolling update is this disciplined, verified, reversible staff swap, applied to your Pods: new Pod up, *proven ready*, old Pod out, repeat — and the old version kept ready to restore at a moment's notice.

## 3. Formal Definition

- **Rolling update.** The default Deployment update strategy. Changing the Pod template (e.g. a new image) creates a **new ReplicaSet** (Chapter 6); Kubernetes scales it *up* while scaling the *old* ReplicaSet *down*, gradually, until all replicas run the new version.
- **`maxSurge`.** How many Pods *above* the desired count may exist during the update (extra capacity to bring new Pods up before removing old). E.g. `maxSurge: 1` with `replicas: 3` allows up to 4 Pods transiently.
- **`maxUnavailable`.** How many Pods *below* desired may be unavailable during the update. `maxUnavailable: 0` means "never drop below the desired count" (safest; requires surge).
- **Readiness gating.** A new Pod is only counted as "available" — and an old Pod only retired — once the new Pod passes its **readiness probe** (Chapter 10). This is what makes the rollout *safe*: broken new Pods never get traffic and *stall* the rollout instead of replacing healthy Pods.
- **Rollback.** Revert to a previous version. Kubernetes keeps a **revision history** of ReplicaSets; `kubectl rollout undo` scales the previous ReplicaSet back up and the bad one down. Under GitOps (Chapter 13), rollback is **`git revert`** — re-declaring the old desired state, which Argo reconciles.
- **`kubectl rollout status` / `pause` / `resume`.** Watch, pause, or resume a rollout. A stalled rollout (new Pods failing readiness) holds at the old version — *failing safe*.
- **Other strategies** (beyond a basic Deployment): **blue-green** (run the new version in full, switch traffic at once) and **canary** (route a small % of traffic to the new version first). Rolling update is the built-in default; these are patterns/tools layered on top.

> A rolling update brings new Pods up and old Pods down gradually, bounded by `maxSurge`/`maxUnavailable`, with each new Pod *readiness-gated* before an old one retires — so a release drops no requests and a broken version stalls instead of taking over. Rollback restores the previous ReplicaSet (or `git revert`).

## 4. Worked Example — watch a zero-downtime rollout

You deploy Cortex `v2`. Step through the rolling update below (desired = 3): a new `v2` Pod surges up, passes readiness, and only *then* does an old `v1` Pod drain — repeating until the whole fleet is `v2`, with healthy Pods serving the entire time.

```d3 widget=k8s-reconcile
{
  "title": "Rolling update v1 → v2 (desired = 3): new up + readiness-gated before old retires",
  "steps": [
    { "event": "steady (v1)", "desired": 3, "note": "Three v1 Pods serving. You change the image to v2.",
      "pods": [ { "name": "cortex-v1-a", "status": "Ready" }, { "name": "cortex-v1-b", "status": "Ready" }, { "name": "cortex-v1-c", "status": "Ready" } ] },
    { "event": "surge", "desired": 3, "note": "maxSurge: a new v2 Pod starts WHILE all three v1 keep serving. Capacity is temporarily 4 — no v1 retired yet.",
      "pods": [ { "name": "cortex-v1-a", "status": "Ready" }, { "name": "cortex-v1-b", "status": "Ready" }, { "name": "cortex-v1-c", "status": "Ready" }, { "name": "cortex-v2-d", "status": "Pending" } ] },
    { "event": "v2 ready → drain v1", "desired": 3, "note": "The v2 Pod PASSES its readiness probe. Only NOW is one v1 Pod (c) drained. Healthy Pods served throughout.",
      "pods": [ { "name": "cortex-v1-a", "status": "Ready" }, { "name": "cortex-v1-b", "status": "Ready" }, { "name": "cortex-v1-c", "status": "Terminating" }, { "name": "cortex-v2-d", "status": "Ready" } ] },
    { "event": "progress", "desired": 3, "note": "v1-c is gone; a second v2 Pod starts. The pattern repeats: new up, proven ready, old out.",
      "pods": [ { "name": "cortex-v1-a", "status": "Ready" }, { "name": "cortex-v1-b", "status": "Ready" }, { "name": "cortex-v2-d", "status": "Ready" }, { "name": "cortex-v2-e", "status": "Pending" } ] },
    { "event": "progress", "desired": 3, "note": "v2-e ready → drain v1-b. Two v2 serving, one v1 left.",
      "pods": [ { "name": "cortex-v1-a", "status": "Ready" }, { "name": "cortex-v1-b", "status": "Terminating" }, { "name": "cortex-v2-d", "status": "Ready" }, { "name": "cortex-v2-e", "status": "Ready" } ] },
    { "event": "finishing", "desired": 3, "note": "Last v2 Pod starting; last v1 draining as it becomes ready.",
      "pods": [ { "name": "cortex-v1-a", "status": "Terminating" }, { "name": "cortex-v2-d", "status": "Ready" }, { "name": "cortex-v2-e", "status": "Ready" }, { "name": "cortex-v2-f", "status": "Pending" } ] },
    { "event": "done (v2)", "desired": 3, "note": "All three Pods are v2 and Ready. Zero requests dropped at any point — and if v2 had failed readiness, the rollout would have STALLED on v1 instead.",
      "pods": [ { "name": "cortex-v2-d", "status": "Ready" }, { "name": "cortex-v2-e", "status": "Ready" }, { "name": "cortex-v2-f", "status": "Ready" } ] }
  ]
}
```

> The key beat is **"v2 ready → drain v1"**: an old Pod is retired *only after* a new one has *proven* itself ready. That readiness gate (Chapter 10) is what makes the rollout *safe*, not just gradual — and it's the exact mechanism behind Part 2's fail-fast guarantee. If `v2` had a broken startup migration, its Pods would *never pass readiness*, so *no v1 Pod would ever be drained* — the rollout would **stall with v1 still fully serving**, and `kubectl rollout status` would show it stuck. A bad deploy becomes a *non-event* (old version keeps running) instead of an outage. To fully recover, you **roll back**: `kubectl rollout undo` (or, under Cortex's GitOps, `git revert` the manifest commit — Chapter 13), and Kubernetes scales the old ReplicaSet back to full.

## 5. Build It

Run this. It performs a readiness-gated rolling update and a rollback, showing capacity never drops below what's needed.

```python run
def rolling_update(old_ver, new_ver, replicas, new_pod_ready):
    old = [f"{old_ver}-{i}" for i in range(replicas)]   # all old, ready
    new = []
    print(f"start: {replicas}× {old_ver} ready")
    while old:
        # SURGE: bring up a new pod (maxSurge=1) while old still serve.
        np = f"{new_ver}-{len(new)}"
        print(f"   surge {np} (capacity {len(old)+len(new)+1}); old still serving")
        if not new_pod_ready(np):
            print(f"   {np} FAILED readiness -> rollout STALLS, {len(old)}× {old_ver} keep serving")
            return ("stalled", old, new)
        new.append(np)
        retired = old.pop()                              # only NOW retire an old pod
        print(f"   {np} ready -> drain {retired}  (serving: {len(old)}×{old_ver} + {len(new)}×{new_ver})")
    print(f"done: {replicas}× {new_ver} ready, 0 requests dropped\n")
    return ("complete", old, new)

# Healthy rollout: every new pod becomes ready.
rolling_update("v1", "v2", replicas=3, new_pod_ready=lambda p: True)

# Bad rollout: new pods never pass readiness (e.g. failed migration).
status, old, new = rolling_update("v2", "v3-broken", replicas=3, new_pod_ready=lambda p: False)
print(f"result: {status} — still on the old version, no outage")
print("rollback: `kubectl rollout undo` / `git revert` -> old ReplicaSet back to full")
```

**Now break it.** The broken rollout (`v3-broken`, never ready) *stalls* on the *first* new Pod — it never drains a single old Pod, so the old version keeps fully serving. That's the safety property: a bad deploy can't take you down because the readiness gate refuses to retire healthy Pods for unhealthy ones. Then make `new_pod_ready` return `True` only for the *first two* pods and `False` for the third — the rollout gets *partway* and stalls, leaving a healthy mix until you roll back. Compare this to a *recreate* strategy (kill all old, then start new): a broken new version there means *total* downtime. Rolling + readiness-gating + easy rollback is what makes frequent deploys safe — you can ship often *because* a bad ship is cheap to survive and undo.

## 6. Trade-offs & Complexity

| Rolling update (readiness-gated) | Recreate / swap-all-at-once |
|---|---|
| Zero downtime — capacity maintained | Downtime gap (recreate) |
| Broken version *stalls*, doesn't take over | Total outage on a bad deploy |
| One-command / `git revert` rollback | Manual, scrambled recovery |
| Needs surge capacity + good readiness probes | Simpler, but unsafe |
| Two versions briefly coexist | One version at a time |

The cost is twofold. *Resource*: a rolling update needs transient *surge* capacity (room for the extra Pods), and you must have *accurate readiness probes* — a probe that returns ready too early defeats the whole safety mechanism (Pods take traffic before they can serve). *Compatibility*: because two versions briefly run at once, the new version must be compatible with the old during the overlap — which is *exactly* the expand/contract discipline from Part 2, Chapter 11 (a schema or API change must work for both versions during the rollout). That coupling is the deepest connection in this book: **safe rollouts and safe migrations are the same problem** — never break the running version during a gradual change. Get readiness and backward-compatibility right, and rolling updates make deploys boring; get them wrong, and you've traded downtime for subtler breakage.

## 7. Edge Cases & Failure Modes

- **Readiness probe too lenient.** If new Pods report ready before they can actually serve, the rollout retires old Pods and routes traffic into not-really-ready new ones — dropping requests. The whole safety hinges on an *honest* readiness probe (Chapter 10).
- **Incompatible versions during overlap.** Two versions run simultaneously mid-rollout; if the new one's database/API changes break the old (or vice versa), you get errors *during* the deploy. Use expand/contract (Part 2, Ch 11) for breaking changes.
- **No surge room.** `maxSurge: 0` with `maxUnavailable: 0` is impossible (can't add new without removing old); a cluster too full to surge stalls the rollout. Ensure capacity headroom.
- **Forgetting rollback exists.** A stalled or bad rollout is recoverable — `rollout undo` or `git revert`. Panicking and hand-editing makes it worse. Trust the rollback.

## 8. Practice

> **Exercise 1 — Why readiness-gated?** Explain why an old Pod is retired only *after* a new Pod passes readiness, and what would go wrong if Kubernetes retired old Pods immediately.

<details>
<summary><strong>Answer</strong></summary>

Readiness gating (§3) is what makes a rolling update *safe*, not merely *gradual*. A new Pod is counted "available" — and an old Pod retired — *only* once the new Pod passes its readiness probe (Chapter 10). The reason: it guarantees there are always enough *genuinely healthy* Pods serving throughout the cutover, so the release drops **zero requests**.

**If old Pods were retired immediately** (before the new ones proved ready), two failures appear:

- **A broken new version causes an outage.** If the new image is bad — say a failed startup migration, so it never becomes ready — retiring old Pods on sight means you've replaced healthy, serving Pods with ones that *can't serve*. Capacity collapses; users hit errors. With gating, the broken Pod *never passes readiness*, so *no* old Pod is drained — the rollout **stalls with the old version fully serving** (a non-event instead of an outage).
- **Even a *good* version drops requests.** A new Pod that's "running" but still booting (JVM warming, caches loading) isn't ready to serve yet. Retire an old Pod before the new one is truly ready, and traffic routes into a not-yet-ready Pod — dropped requests during every deploy.

So the gate is the whole safety mechanism: *prove the replacement works before discarding what works.* It's the exact mechanism behind Part 2's fail-fast guarantee.

</details>

> **Exercise 2 — Bad deploy.** A new image fails its readiness probe. Walk through what the rolling update does (does it take you down?), and how you recover.

<details>
<summary><strong>Answer</strong></summary>

**Does it take you down? No.** Walk the rollout:

1. Kubernetes creates a new ReplicaSet and **surges** up the first new Pod (`maxSurge`) *while all old Pods keep serving* — capacity is temporarily *higher*, nothing retired yet.
2. The new Pod **fails its readiness probe**, so it's never counted available.
3. Because an old Pod is retired only *after* a new one becomes ready (§3), and the new one never does, **no old Pod is ever drained**. The rollout **stalls** on the very first new Pod.
4. The old version keeps serving at full capacity the entire time. `kubectl rollout status` shows it stuck/waiting; `kubectl describe` shows the readiness-probe failures. A bad deploy is a *non-event*, not an outage.

**How you recover: roll back.**

- Non-GitOps: `kubectl rollout undo` — Kubernetes scales the *previous* ReplicaSet (kept in revision history) back to full and scales the broken one down.
- Under Cortex's GitOps (Chapter 13): `git revert` the manifest commit, re-declaring the old image tag; Argo reconciles the cluster back to it.

Contrast a *recreate* strategy (kill all old, then start new): there, a broken new version means *total downtime*. Rolling + readiness-gating + easy rollback is exactly why you can deploy frequently and fearlessly — a bad ship is cheap to survive and trivial to undo.

</details>

> **Exercise 3 — The deep connection.** Explain why a rolling update (two versions coexisting) requires the *same* backward-compatibility discipline as Part 2's expand/contract migrations.

<details>
<summary><strong>Answer</strong></summary>

During a rolling update, the new and old versions **run at the same time** — for the whole duration of the rollout, *some* Pods are `v1` and *some* are `v2`, both serving live traffic against the *same database*. That overlap is unavoidable: it's the very thing that gives you zero downtime (new up before old down).

The consequence: **the two versions must be compatible *with each other* during the overlap.** If `v2` ships a *breaking* schema or API change — drops a column `v1` still reads, renames a field `v1` still writes — then while both run, one of them hits data it can't handle, and you get errors *during the deploy* (not a clean cutover, but a window of breakage hitting whichever version is on the wrong side of the change).

This is *precisely* the expand/contract discipline from Part 2, Chapter 11: never make a single change that breaks the currently-running version. You **expand** first (add the new column/field in a way both versions tolerate), deploy the code that uses it, and only **contract** (remove the old shape) in a *later* release once no running version needs it. So the deepest connection in the book: **safe rollouts and safe migrations are the same problem** — *never break the running version during a gradual change.* The rolling update is the runtime version; expand/contract is the data version; both forbid a breaking change while two versions coexist.

</details>

```quiz
{
  "prompt": "Why does a Kubernetes rolling update achieve zero downtime even if the new version is broken?",
  "input": "Choose one:",
  "options": [
    "It brings up new Pods and only retires old ones AFTER each new Pod passes its readiness probe — so a broken version fails readiness and STALLS the rollout (old version keeps serving) instead of replacing healthy Pods",
    "It takes the app offline during the swap to be safe",
    "It deletes all old Pods first, then starts new ones",
    "It runs the new version on a separate internet"
  ],
  "answer": "It brings up new Pods and only retires old ones AFTER each new Pod passes its readiness probe — so a broken version fails readiness and STALLS the rollout (old version keeps serving) instead of replacing healthy Pods"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Kubernetes — Rolling updates & Deployment strategy](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment)** — `maxSurge`, `maxUnavailable`, and readiness gating.
- **[Kubernetes — Rolling back a Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-back-a-deployment)** — `kubectl rollout undo` and revision history.
- **[Martin Fowler — BlueGreenDeployment](https://martinfowler.com/bliki/BlueGreenDeployment.html)** & **[CanaryRelease](https://martinfowler.com/bliki/CanaryRelease.html)** — the deploy strategies layered on top of rolling updates.

---

**Next:** the final skill — when something *does* go wrong in production, how do you look inside a running Pod? Logs, events, and reading a crash. → [15. Observing a live pod](/cortex/production-engineering/docker-kubernetes/cortex-in-production/observing-a-live-pod)
