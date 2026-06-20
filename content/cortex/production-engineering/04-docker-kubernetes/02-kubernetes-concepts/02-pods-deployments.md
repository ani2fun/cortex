---
title: '6. Pods, ReplicaSets, Deployments'
summary: The objects that actually run your app. A Pod is the smallest deployable unit; a ReplicaSet keeps N identical Pods alive; a Deployment manages ReplicaSets and gives you declarative updates and rollback. Watch one self-heal.
---

# 6. Pods, ReplicaSets, Deployments

## TL;DR

> Three nested objects run your app. A **Pod** is the smallest deployable unit — one (or a few tightly-coupled) containers sharing a network and lifecycle; Pods are *disposable* and *replaceable*. A **ReplicaSet** keeps exactly **N identical Pods** running, replacing any that die (the self-healing loop). A **Deployment** manages ReplicaSets and adds **declarative updates** (roll out a new version) and **rollback**. You almost never create Pods or ReplicaSets directly — you write a **Deployment**, and it manages the rest. Cortex is a Deployment. Below, watch the reconciliation loop heal a crashed pod live.

## 1. Motivation

Chapter 5 gave us the *idea* (desired state, reconciliation). Now we meet the actual *objects* that embody it for running an application. You need to know three, and crucially *which one you write* (the Deployment) versus which ones Kubernetes manages for you (Pods and ReplicaSets). Beginners often try to create Pods directly and are baffled when a crashed Pod stays dead — because a bare Pod has *nothing watching it*. The whole self-healing magic comes from the *layers above* the Pod. Understanding the Pod → ReplicaSet → Deployment hierarchy — what each adds — is how you go from "I made a container run once" to "I declared an app that stays running, scales, and updates cleanly."

## 2. Intuition (Analogy)

Think of a **shift of workers** at a 24/7 operation.

- A **Pod** is *one worker* on the floor — doing the job, but mortal: they get sick, go home, are replaced. You don't get attached to a specific worker; you care that *the work is staffed*. Pods are cattle, not pets — disposable and interchangeable.
- A **ReplicaSet** is the *staffing rule*: "there must always be **5 workers** on this floor." A supervisor enforces it: a worker leaves → call in a replacement, *immediately*, to keep the count at 5. That enforcement is self-healing.
- A **Deployment** is the *shift manager* who also handles *changing the procedure*: "we're switching to the new process." They bring in new-process workers *gradually* while old-process workers finish, so the floor is *never unstaffed* during the transition — and if the new process is a disaster, they can *revert* to the old one. That's rolling updates and rollback.

You, the owner, talk to the *shift manager* (the Deployment): "staff this floor with 5 workers running the new process." The manager handles staffing rules (ReplicaSet) and individual workers (Pods). You never personally hire one worker — you set policy and the hierarchy executes it.

## 3. Formal Definition

- **Pod.** The smallest deployable unit in Kubernetes. Usually **one container**, but can be a few **tightly-coupled** containers that share the Pod's **network namespace** (same IP/localhost) and storage — co-scheduled and co-lifecycled. Pods are **ephemeral**: they get a new IP each time, and are *replaced* (not repaired) when they die. You rarely create them directly.
- **ReplicaSet.** Ensures a specified **number of identical Pod replicas** are running. Its controller watches the actual Pod count and creates/deletes Pods to match the desired count — the reconciliation loop (Chapter 5) for "how many pods." If a Pod dies or a node fails, the ReplicaSet makes a replacement.
- **Deployment.** The object you actually write for a stateless app. It manages ReplicaSets and adds:
  - **Declarative updates** — change the Pod template (e.g. a new image) and the Deployment performs a **rolling update**: create a *new* ReplicaSet, scale it up while scaling the old one down, so there's no downtime (Chapter 14).
  - **Rollback** — revert to a previous ReplicaSet if a new version misbehaves.
  - **Self-healing & scaling** — via the ReplicaSet it owns.
- **The hierarchy:** **Deployment** → owns a → **ReplicaSet** → owns → **Pods** → run → containers. You declare the Deployment; the rest follows.
- **Labels & selectors.** A Deployment finds "its" Pods by **labels** (key/value tags) matched by a **selector** — the loose coupling that lets controllers manage sets of Pods.

> A Pod runs your container(s) and is disposable; a ReplicaSet keeps N Pods alive (self-healing); a Deployment manages ReplicaSets and adds zero-downtime updates and rollback. Write the Deployment; it manages the rest.

## 4. Worked Example — watch a Deployment self-heal

Cortex's [`deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml) declares the app (abridged):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cortex
spec:
  replicas: 1                          # desired Pod count (the GOAL)
  selector:
    matchLabels: { app.kubernetes.io/name: cortex }   # finds "its" Pods by label
  template:                            # the Pod template — what each replica looks like
    metadata:
      labels: { app.kubernetes.io/name: cortex }
    spec:
      containers:
        - name: cortex
          image: ghcr.io/ani2fun/cortex:latest          # the image from Chapter 3
          ports: [{ containerPort: 8080 }]
          # (env, probes, resources, security — Chapters 9–12)
```

You apply this one object. Kubernetes creates a ReplicaSet for it, which creates `replicas: 1` Pod running the `ghcr.io/ani2fun/cortex` image. Now the reconciliation loop takes over forever. Step through it below — a Deployment with `desired=3` (shown larger than Cortex's 1, to make self-healing vivid): the pods come up, one **crashes** (an `OOMKilled`), and the controller **notices the gap and schedules a replacement** until it's back to 3/3 — all with no human action.

```d3 widget=k8s-reconcile
{
  "title": "A Deployment self-healing (desired = 3): watch the controller replace a crashed pod",
  "steps": [
    { "event": "apply", "desired": 3, "note": "You apply the Deployment. The ReplicaSet creates 3 Pods; the scheduler is placing them on nodes.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Pending" }, { "name": "cortex-7df-b2", "status": "Pending" }, { "name": "cortex-7df-c3", "status": "Pending" } ] },
    { "event": "scheduled", "desired": 3, "note": "Pods scheduled onto nodes and their containers are starting up.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Running" }, { "name": "cortex-7df-b2", "status": "Running" }, { "name": "cortex-7df-c3", "status": "Running" } ] },
    { "event": "ready", "desired": 3, "note": "All 3 pass their readiness checks (Chapter 10). actual = desired = 3. The controller rests.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Ready" }, { "name": "cortex-7df-b2", "status": "Ready" }, { "name": "cortex-7df-c3", "status": "Ready" } ] },
    { "event": "crash", "desired": 3, "note": "cortex-7df-b2 is OOMKilled (ran out of memory — Chapter 11). Actual ready drops to 2/3.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Ready" }, { "name": "cortex-7df-b2", "status": "Crashed" }, { "name": "cortex-7df-c3", "status": "Ready" } ] },
    { "event": "reconcile", "desired": 3, "note": "The ReplicaSet's controller observes 2 < 3 and creates a REPLACEMENT pod (cortex-7df-d4). No human involved.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Ready" }, { "name": "cortex-7df-c3", "status": "Ready" }, { "name": "cortex-7df-d4", "status": "Pending" } ] },
    { "event": "ready", "desired": 3, "note": "The replacement passes its checks. actual = desired = 3 again. Self-healed — automatically.",
      "pods": [ { "name": "cortex-7df-a1", "status": "Ready" }, { "name": "cortex-7df-c3", "status": "Ready" }, { "name": "cortex-7df-d4", "status": "Ready" } ] }
  ]
}
```

> Notice you never *commanded* a restart. The crashed `b2` is simply *gone* (Pods aren't repaired, they're replaced), and a brand-new `d4` takes its place — a different name, a different IP, the same role. That's the cattle-not-pets model: the ReplicaSet cares about the *count of healthy pods*, not any individual one. Cortex runs `replicas: 1`, so a crash means a brief gap while the single replacement starts (acceptable for a homelab app); a production service wanting *no* gap would run `replicas: 2+` so survivors serve while the replacement spins up.

## 5. Build It

Run this. It's the Pod/ReplicaSet/Deployment hierarchy: a ReplicaSet self-heals to a count, and a Deployment performs a rolling update by swapping ReplicaSets.

```python run
class ReplicaSet:
    def __init__(self, image, desired):
        self.image, self.desired, self.pods = image, desired, []
    def reconcile(self):
        while len(self.pods) < self.desired:
            self.pods.append(f"{self.image}-pod{len(self.pods)+1}")   # heal up to desired
        self.pods = self.pods[:self.desired]
    def crash(self, i): self.pods.pop(i)                              # a pod dies

class Deployment:
    def __init__(self, image, replicas):
        self.rs = ReplicaSet(image, replicas); self.rs.reconcile()
    def status(self): return f"{self.rs.image}: {self.rs.pods}"
    def update_image(self, new_image):
        # Rolling update: new ReplicaSet up while old scales down (Chapter 14, simplified).
        print(f"   rolling update -> {new_image}")
        self.rs = ReplicaSet(new_image, self.rs.desired); self.rs.reconcile()

d = Deployment("cortex:v1", replicas=3)
print("applied:", d.status())

print("a pod crashes...")
d.rs.crash(1)
print("   after crash:", d.rs.pods)
d.rs.reconcile()                              # ReplicaSet self-heals
print("   self-healed:", d.rs.pods, "\n")

print("deploy a new version:")
d.update_image("cortex:v2")                   # Deployment swaps ReplicaSets
print("   now serving:", d.status())
```

**Now break it.** Try to make a *bare Pod* self-heal: remove the `ReplicaSet` and just keep a list of pod strings; crash one, and *nothing* brings it back — because a bare Pod has no controller watching it. That's exactly why you don't create Pods directly: the self-healing lives in the *ReplicaSet* (the count-keeper), and the clean updates live in the *Deployment* (the ReplicaSet-swapper). Write the Deployment, and you inherit both. The hierarchy isn't bureaucracy — each layer adds a capability (run → keep N alive → update safely) you genuinely need.

## 6. Trade-offs & Complexity

| Deployment (manages the hierarchy) | Bare Pods |
|---|---|
| Self-healing (via ReplicaSet) | A dead Pod stays dead |
| Rolling updates + rollback | Manual, downtime-prone swaps |
| Declarative scaling (`replicas: N`) | Manual create/delete |
| The object you write for stateless apps | Almost never the right choice |
| Three concepts to understand | One concept (but useless alone) |

There's little trade-off *between* these — you essentially always use a Deployment for a stateless app like Cortex; bare Pods and raw ReplicaSets are things you almost never write by hand. The real "cost" is conceptual: understanding the three-layer hierarchy and the label/selector coupling. The payoff is that one declarative object gives you self-healing, scaling, zero-downtime updates, and rollback for free. (Stateful apps — databases — use a cousin, `StatefulSet`, with stable identities and storage; Cortex's *app* is stateless, so a Deployment fits, while its Postgres is managed separately.)

## 7. Edge Cases & Failure Modes

- **Creating bare Pods.** A Pod created directly has no controller, so it won't be recreated if it dies. Always use a Deployment (or another controller) for anything that should *stay* running.
- **`replicas: 1` and availability.** A single replica means a crash (or a node failure, or even a rolling update) causes a brief outage while the replacement starts. For zero-gap availability, run 2+.
- **Mismatched labels/selectors.** If a Deployment's `selector` doesn't match its Pod template `labels`, it can't find or manage its Pods — a common, confusing misconfiguration. Keep them consistent.
- **Treating Pods as pets.** Relying on a specific Pod's name or IP breaks the moment it's replaced (which is constant). Address the app via a *Service* (Chapter 7), never a Pod IP.

## 8. Practice

> **Exercise 1 — Who does what?** For each capability, name the responsible object: (a) keep 3 pods alive; (b) run the container; (c) roll out a new image without downtime; (d) revert a bad deploy.

<details>
<summary><strong>Answer</strong></summary>

Each layer of the hierarchy **Deployment → ReplicaSet → Pod** adds exactly one capability (§3):

- **(a) keep 3 pods alive → the ReplicaSet.** Its controller watches the actual Pod count and creates/deletes Pods to match the desired number — the self-healing loop for "how many pods."
- **(b) run the container → the Pod.** The smallest deployable unit; it actually holds and runs the container(s).
- **(c) roll out a new image without downtime → the Deployment.** It performs the rolling update — standing up a *new* ReplicaSet while scaling the old one down, so there's no gap.
- **(d) revert a bad deploy → the Deployment.** Rollback is a Deployment feature: it can revert to a previous ReplicaSet if a new version misbehaves.

Notice (c) and (d) both live at the Deployment — updates and rollback are precisely what the Deployment adds *on top of* the ReplicaSet's self-healing. That's why the one object you actually write is the Deployment: it inherits (a) and (b) through the layers it owns, and contributes (c) and (d) itself.

</details>

> **Exercise 2 — Why replaced, not repaired?** Explain why Kubernetes *replaces* a crashed Pod with a new one (new name, new IP) rather than restarting the same Pod, and what that implies about how you should address the app.

<details>
<summary><strong>Answer</strong></summary>

**Why replaced, not repaired:** Pods are designed to be **ephemeral and disposable — cattle, not pets** (§2–3). The reconciliation model cares only about the *count of healthy pods matching the spec*, not about any individual pod's identity. So when a pod dies, the simplest, most robust action is to discard it entirely and create a *fresh* one from the same Pod template — a clean instance with no leftover broken state. Trying to *repair* a crashed pod in place would mean reasoning about its corrupted internals; *replacing* it sidesteps that, and it's the *same* mechanism whether one pod crashed, a node failed, or you scaled up. A new pod naturally gets a new name and a new IP because it's genuinely a new object.

**What it implies for addressing the app:** you must **never** rely on a specific Pod's name or IP — those are valid only until the next replacement, which is constant (§7). Address the app through the stable abstraction in front of the pods — a **Service** (Chapter 7) — which provides one unchanging name/IP that always routes to whatever healthy pods currently exist. Treating a pod as a pet (pinning to its IP) breaks the moment the cattle model does what it's designed to do.

</details>

> **Exercise 3 — One replica risk.** Cortex runs `replicas: 1`. Describe what a user experiences when its single pod is OOMKilled, and what changing to `replicas: 2` would improve.

<details>
<summary><strong>Answer</strong></summary>

**With `replicas: 1`:** there's exactly one pod, so when it's OOMKilled there are *zero* healthy pods until a replacement is built. The reconciliation loop notices `actual 0 < desired 1` and schedules a new pod immediately — but "immediately" still means the pod must be *scheduled, its container started, and its readiness check passed* before it can serve. During that window, requests have nowhere to go: users experience a **brief outage** (errors / connection failures, or a hang) until the replacement comes up. For a homelab app this short gap is acceptable (§4), which is why Cortex runs a single replica.

**What `replicas: 2` improves:** with two pods, an OOMKill of one leaves the *other still serving*. The Service (Chapter 7) simply stops routing to the dead pod and sends all traffic to the survivor while the ReplicaSet builds a replacement — so users see **no outage at all**, just (briefly) less capacity. The general principle (§7): a single replica means *any* disruption — a crash, a node failure, even a rolling update — causes a gap, because there's no survivor to cover it. Running 2+ is what buys zero-gap availability.

</details>

```quiz
{
  "prompt": "What is the relationship between a Pod, a ReplicaSet, and a Deployment?",
  "input": "Choose one:",
  "options": [
    "A Deployment manages ReplicaSets (adding rolling updates + rollback); a ReplicaSet keeps N identical Pods alive (self-healing); a Pod runs the container(s). You write the Deployment; it manages the rest",
    "They are three names for the same thing",
    "A Pod manages Deployments which manage ReplicaSets",
    "A ReplicaSet is a faster kind of Pod"
  ],
  "answer": "A Deployment manages ReplicaSets (adding rolling updates + rollback); a ReplicaSet keeps N identical Pods alive (self-healing); a Pod runs the container(s). You write the Deployment; it manages the rest"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Kubernetes — Pods](https://kubernetes.io/docs/concepts/workloads/pods/)**, **[ReplicaSets](https://kubernetes.io/docs/concepts/workloads/controllers/replicaset/)**, **[Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)** — the three objects, official docs.
- **[Kubernetes — Labels and selectors](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/)** — the coupling that lets controllers manage Pod sets.
- **[Cortex `deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)** — the real Deployment (in the sibling `infra` repo) that runs Cortex.

---

**Next:** Pods are ephemeral — new IP every time they're replaced. So how does *anything* reliably reach your app? Through a stable abstraction in front of the Pods: the Service. → [7. Services & cluster networking](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/services-networking)
