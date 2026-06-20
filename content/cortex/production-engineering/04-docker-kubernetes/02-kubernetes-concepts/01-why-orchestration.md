---
title: '5. Why orchestration? Desired state'
summary: At production scale you have many containers on many machines that crash, scale, and update constantly. Kubernetes manages them with one powerful idea — you declare the desired state, and it continuously makes reality match.
---

# 5. Why orchestration? Desired state

## TL;DR

> docker-compose runs a stack on *one* machine and doesn't heal crashes or update without downtime. Production needs more: many containers across many machines, restarted when they die, scaled on demand, updated with zero downtime. That's **orchestration**, and **Kubernetes** is the standard. Its core idea is **desired state**: you *declare* what you want ("run 3 replicas of this image"), and Kubernetes' **controllers continuously reconcile** — comparing actual state to desired and acting to close the gap, forever. You don't issue commands ("start a container"); you declare a goal and the system relentlessly maintains it.

## 1. Motivation

Picture running Cortex's stack not on your laptop but *in production*, for real users. Now the hard questions arrive. A container crashes at 3 a.m. — who restarts it? Traffic spikes — who starts more replicas, and on which machines? A machine dies — who reschedules its containers elsewhere? You deploy a new version — how do you replace the old containers *without dropping requests*? Doing any of this by hand, reliably, around the clock, across a fleet of machines, is impossible for humans. You'd need a tireless operator who watches everything and constantly fixes it.

Kubernetes *is* that operator, automated. But its genius isn't a pile of commands for "restart this, scale that, move the other." It's a *control-loop* model built on **desired state**. Instead of telling the system *what to do* (imperative), you tell it *what you want to be true* (declarative): "there should always be 3 healthy replicas of this image, reachable at this address." Kubernetes then runs **controllers** — loops that endlessly observe *actual* state, compare it to your *declared* desired state, and take whatever action closes the gap. A pod crashed? Actual is 2, desired is 3 → start one. A machine died? Reschedule its pods. You changed desired to 5? Start two more. You never command the individual actions; you maintain a *declaration*, and the system continuously bends reality toward it. This single idea — declare the goal, let controllers reconcile — is the foundation everything else in Kubernetes is built on.

## 2. Intuition (Analogy)

A **thermostat.** You don't manage your home's temperature by issuing commands — "turn the heater on now," "turn it off now," "on again." You'd never sleep. Instead you *declare a desired state*: "I want it to be 21°C." The thermostat then runs a continuous loop: read the *actual* temperature, compare to the *desired* 21°C, and act — heat if too cold, rest if just right — *forever*. Open a window and the room cools; the thermostat notices the gap and heats again, without you doing anything. You set the *goal*; the control loop maintains it against all disturbances.

Kubernetes is a thermostat for your containers. You declare "3 healthy replicas of Cortex." Its controllers continuously read actual state (how many healthy replicas exist?), compare to desired (3), and act (start one if a pod died, reschedule if a machine failed). Crash a pod — the "room cools" — and the controller "heats" by starting a replacement, no human required. You manage the *thermostat setting* (the desired state), never the individual heating cycles.

## 3. Formal Definition

- **Orchestration.** Automated management of containers across a cluster of machines: scheduling (placing containers on machines), self-healing (restarting/rescheduling failed ones), scaling (adjusting replica counts), networking, and rolling updates.
- **Kubernetes (K8s).** The dominant open-source container orchestrator. A **cluster** of machines (**nodes**), coordinated by a **control plane**.
- **Declarative / desired state.** You submit **objects** (YAML manifests) describing the *desired* state — e.g. a `Deployment` saying "3 replicas of image X." You don't run procedural commands; you declare goals.
- **Controllers & the reconciliation loop.** For each kind of object, a **controller** runs a loop: *observe* actual state → *compare* to desired → *act* to reduce the difference → repeat, forever. This **control loop** is the heart of Kubernetes. Self-healing, scaling, and rollouts are all the same loop reconciling different gaps.
- **Control plane.** The brain: the **API server** (where you submit desired state), **etcd** (the store of desired + actual state), the **scheduler** (decides which node runs a pod), and the **controller manager** (runs the controllers). The **kubelet** on each node runs the pods the control plane assigns.
- **Imperative vs declarative.** *Imperative*: "start a container" (you manage steps). *Declarative*: "there should be 3" (the system manages steps to reach it). Kubernetes is declarative — which is why it can *recover* (it always knows the goal) where a sequence of commands cannot.

> Kubernetes manages containers across many machines by reconciling **desired state**: you declare what should be true; controllers loop forever, observing actual state and acting to match it. Declare the goal; the control loop maintains it against crashes, failures, and load.

## 4. Worked Example — declarative beats imperative

Watch why "declare the goal" recovers where "run commands" can't:

```text
IMPERATIVE (a sequence of commands):
  > start container A      ✓ (1 running)
  > start container B      ✓ (2 running)
  > start container C      ✓ (3 running)
  ... A crashes at 3 a.m. ...
  -> now 2 running. The commands already ran. NOTHING restarts A.
     A human must notice and run `start A` again. (Who's awake?)

DECLARATIVE (Kubernetes — desired state = 3):
  > apply: { replicas: 3, image: cortex }      (you declare the GOAL)
  ... controller loop: actual 3 == desired 3 -> rest ...
  ... A crashes at 3 a.m. ...
  -> controller loop: actual 2 != desired 3 -> START a replacement (instantly, automatically)
  ... actual 3 == desired 3 -> rest ...
  ... a whole NODE dies, taking 2 pods ...
  -> controller loop: actual 1 != desired 3 -> reschedule 2 pods onto healthy nodes
```

The imperative version is a list of *past actions*; once they've run, the system has no memory of *intent*, so it can't recover from a later disturbance. The declarative version holds the *goal* in `etcd` permanently, so a controller can *always* tell when reality has drifted and fix it — at 3 a.m., during a node failure, under any disturbance, with no human. This is exactly how Cortex runs in production: a `Deployment` (Chapter 6) declares the desired replica count and image, and Kubernetes maintains it. When you deploy a new Cortex version, you don't command a careful container swap — you *change the desired image*, and the controller reconciles old → new with a rolling update (Chapter 14). Declare the destination; let the system drive.

## 5. Build It

Run this. It is a reconciliation loop — the literal heart of Kubernetes — healing a crash and absorbing a scale-up by always driving actual toward desired.

```python run
import random
random.seed(7)

class Cluster:
    def __init__(self, desired):
        self.desired = desired      # the GOAL you declared
        self.pods = []              # actual state

    def reconcile(self):
        """The control loop: observe actual vs desired, act to close the gap."""
        actual = len(self.pods)
        if actual < self.desired:
            for _ in range(self.desired - actual):
                self.pods.append(f"pod-{random.randint(100,999)}")
            print(f"   actual {actual} < desired {self.desired} -> started {self.desired-actual} pod(s)")
        elif actual > self.desired:
            removed = self.desired - actual
            self.pods = self.pods[:self.desired]
            print(f"   actual {actual} > desired {self.desired} -> removed {-removed} pod(s)")
        else:
            print(f"   actual {actual} == desired {self.desired} -> nothing to do")

c = Cluster(desired=3)
print("apply desired=3:");            c.reconcile()      # start 3
print("a pod crashes...");            c.pods.pop()        # actual now 2
print("control loop runs again:");    c.reconcile()      # heals back to 3
print("you declare desired=5:");      c.desired = 5; c.reconcile()   # scales up
print("\nfinal pods:", c.pods)
```

**Now break it.** Add a line that crashes *two* pods (`c.pods = c.pods[:-2]`) and call `c.reconcile()` again — the loop simply starts two replacements, because it only ever cares about the *gap* between actual and desired, never about *how* the gap appeared. That generality is the power: the *same* loop heals a crash, recovers a node failure, scales up, and scales down — because all of those are just "actual ≠ desired." You don't write recovery logic for each disaster; you declare the goal and one loop handles them all. That's why Kubernetes is robust where a script of commands is brittle.

## 6. Trade-offs & Complexity

| Kubernetes (declarative orchestration) | docker-compose / manual ops |
|---|---|
| Self-healing, scaling, rolling updates | Manual restarts, no auto-scale |
| Runs across many machines | Single host |
| Recovers from any drift (holds the goal) | Commands have no memory of intent |
| Huge conceptual surface to learn | Simple, but limited |
| Operational + cognitive overhead | Quick to start |

The cost of Kubernetes is *real and large*: a steep learning curve (pods, services, ingress, the whole vocabulary — the rest of this Part), operational complexity, and the temptation to adopt it before you need it. For a tiny app on one box, compose or a single container is genuinely simpler and correct. Kubernetes earns its complexity when you need *production* properties — high availability, zero-downtime deploys, scaling, multi-machine — that compose can't give. Cortex runs on a modest K8s cluster (the [Homelab from Scratch](/cortex/homelab-from-scratch) book builds one) precisely to get self-healing and clean rollouts. The desired-state model is the one idea that makes all that complexity *coherent* — learn it first, and the rest of Kubernetes is variations on it.

## 7. Edge Cases & Failure Modes

- **Thinking imperatively.** Running `kubectl run`/`delete` to fix things by hand fights the controllers — they'll undo your manual changes to restore desired state. Change the *declaration* instead.
- **Adopting K8s too early.** A single-container app on one server doesn't need a cluster. Kubernetes' complexity is only worth it for production-scale needs. Don't cargo-cult it.
- **Desired state that can't be met.** If you declare 3 replicas but the cluster lacks resources, the controller *keeps trying* (pods stay `Pending`). The loop won't magically create capacity — it surfaces the gap so you can add nodes or lower the ask.
- **Drift via manual changes.** Editing live objects with `kubectl edit` instead of updating your version-controlled manifests causes the cluster to diverge from your source of truth (GitOps, Chapter 13, addresses this).

## 8. Practice

> **Exercise 1 — Imperative or declarative?** Classify each: (a) "start one more container"; (b) "there should be 5 replicas"; (c) "the app should be reachable at cortex.kakde.eu". Which can self-heal, and why?

<details>
<summary><strong>Answer</strong></summary>

The distinction (§3): *imperative* commands an **action** (you manage the steps); *declarative* states a **goal** (the system manages the steps to reach it).

- **(a) "start one more container" → imperative.** It's a one-off action. Once it runs, the system retains *no memory of intent* — it just did a thing.
- **(b) "there should be 5 replicas" → declarative.** It states a goal to be kept true, stored as desired state.
- **(c) "the app should be reachable at cortex.kakde.eu" → declarative.** Also a goal — a condition the system should continuously maintain.

**Which self-heal: (b) and (c) — the declarative ones.** The reason is precise: a controller can only *recover* if it knows the goal. Because (b) and (c) are held as desired state in `etcd`, a controller can forever compare *actual* to *desired* and act on any gap — a crashed replica, a failed node — at 3 a.m., with no human. The imperative (a) leaves only a *past action* behind; when a disturbance later occurs, there's nothing that remembers what *should* be true, so nothing recovers. Self-healing is a property of holding intent, which only the declarative form does.

</details>

> **Exercise 2 — Trace the loop.** You declare desired=3. A node dies, killing 2 pods. Walk through what the reconciliation loop observes and does, step by step.

<details>
<summary><strong>Answer</strong></summary>

The control loop never reasons about *how* the gap appeared — only about the gap between actual and desired (§5). Step by step:

1. **Steady state.** Desired = 3, actual = 3 (three healthy pods, spread across nodes). `actual == desired` → the controller rests.
2. **The node dies.** It was running 2 of the 3 pods; those 2 are now gone. Actual drops to 1.
3. **Observe.** On its next pass, the controller reads actual = 1, desired = 3.
4. **Compare.** `1 < 3` → there's a gap of 2.
5. **Act.** The controller creates **2 replacement pods** to close the gap. The scheduler places them on the *remaining healthy nodes* (it won't schedule onto the dead one).
6. **Converge.** The replacements come up and pass their checks; actual returns to 3. `actual == desired` → the controller rests again.

No human intervened, and the loop didn't need special "node-failure logic" — it's the *same* loop that heals a single crash or absorbs a scale-up, because all of those are just "actual ≠ desired." (One caveat from §7: if the surviving nodes lack capacity for 2 more pods, the replacements stay `Pending` — the loop keeps *trying* but can't conjure capacity; it surfaces the gap so you can add nodes.)

</details>

> **Exercise 3 — When NOT K8s.** Give a concrete scenario where Kubernetes is the wrong choice and compose (or a single container) is better. Justify it.

<details>
<summary><strong>Answer</strong></summary>

**Scenario:** a personal side-project — say a small Flask API plus a Postgres, serving a handful of users, running on a *single* cheap VPS, deployed by one developer who can tolerate a few seconds of downtime on the rare deploy or crash.

**Why compose (or a single container) wins here:** Kubernetes earns its complexity only when you need *production-scale properties* it uniquely provides — high availability, zero-downtime rolling deploys, autoscaling, and scheduling across *many* machines (§6). This scenario needs *none* of them: there's one machine (so multi-node scheduling is moot), occasional brief downtime is acceptable (so zero-downtime rollouts aren't worth the cost), and load is flat (no autoscaling). Against that, Kubernetes brings a steep learning curve (pods, services, ingress, controllers) and real operational overhead — a cluster to run and maintain.

Adopting K8s here is **cargo-culting** (§7): paying a large, permanent complexity tax for capabilities you won't use. `docker compose up` on the VPS gives a reproducible multi-container stack with a fraction of the cognitive load — the right tool for the scale. The honest rule: reach for Kubernetes when production *demands* self-healing-across-machines and clean rollouts, not before.

</details>

```quiz
{
  "prompt": "What is the core idea behind how Kubernetes manages containers?",
  "input": "Choose one:",
  "options": [
    "Desired state: you declare what should be true (e.g. 3 replicas), and controllers run a continuous loop observing actual state and acting to make it match — self-healing by always closing the gap",
    "You manually issue commands to start and stop each container",
    "It compiles your code into containers",
    "It runs everything on a single machine"
  ],
  "answer": "Desired state: you declare what should be true (e.g. 3 replicas), and controllers run a continuous loop observing actual state and acting to make it match — self-healing by always closing the gap"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Kubernetes — What is Kubernetes / concepts](https://kubernetes.io/docs/concepts/overview/)** — the control-plane, nodes, and the declarative model.
- **[Kubernetes — Controllers & the reconciliation loop](https://kubernetes.io/docs/concepts/architecture/controller/)** — the desired-vs-actual loop at the heart of everything.
- **[Homelab from Scratch — Kubernetes base](/cortex/homelab-from-scratch/kubernetes-base/why-k3s)** — building the actual cluster Cortex runs on (the companion to this Part).

---

**Next:** the desired state you declare is made of objects. The most important ones: Pods (the unit that runs), and the Deployment that keeps the right number of them alive. Watch self-healing happen. → [6. Pods, ReplicaSets, Deployments](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/pods-deployments)
