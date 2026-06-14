---
title: '11. Resources: requests & limits'
summary: A container can hog all of a node''s CPU and memory and starve its neighbors — or be starved itself. Requests guarantee a minimum and guide scheduling; limits cap the maximum. This is also why a Pod gets OOMKilled.
---

# 11. Resources: requests & limits

## TL;DR

> A node has finite CPU and memory, shared by all the Pods on it. Without rules, one greedy container can starve every neighbor. Kubernetes uses two numbers per container. A **request** is the *guaranteed minimum* the container is promised — the **scheduler** uses it to decide which node has room, and it's reserved for that Pod. A **limit** is the *hard ceiling* — exceed the **memory** limit and the container is **OOMKilled** (terminated); exceed the **CPU** limit and it's **throttled** (slowed, not killed). Cortex requests `100m` CPU / `256Mi` memory and is limited to `1000m` / `1Gi`. This is exactly the `OOMKilled` you saw the pod suffer in Chapter 6.

## 1. Motivation

Containers on the same node share that node's physical CPU and RAM — they're roommates with one fridge and one kitchen. Left ungoverned, this goes badly two ways. A **memory leak** in one container can consume *all* the node's RAM, and the Linux kernel starts killing processes to survive — possibly *your other, innocent* containers. A **CPU-hungry** container can monopolize the cores, leaving its neighbors unable to respond (failing their probes, Chapter 10, triggering needless restarts). And the *scheduler* (Chapter 5) needs to know how much each Pod needs to place it sensibly — pack too many onto one node and they all suffer; spread them blindly and you waste capacity.

Requests and limits give Kubernetes the numbers to manage this fairly. **Requests** are a *promise to the Pod* ("you will always have at least this much") and a *hint to the scheduler* ("this Pod needs this much room — find a node that has it"). **Limits** are a *promise to the neighbors* ("this Pod can never take more than this, so it can't starve you"). Together they turn a chaotic shared resource into a governed one — and they explain a class of production mysteries (the pod that keeps getting `OOMKilled`, the app that's inexplicably slow) that are really resource-configuration issues. Setting them well is one of the most impactful and most overlooked parts of running on Kubernetes.

## 2. Intuition (Analogy)

A **shared office with hot-desking and a power budget.** Each team books desks in advance (a **request**): "we need at least 4 desks." The office manager (scheduler) won't put a team in a room that can't fit their booking, and those desks are *reserved* — the team is guaranteed them. There's also a **fire-code maximum** per room (a **limit**): no team may exceed it, no matter how much they'd like to sprawl, so one team can never take over the whole floor and freeze everyone out.

The two numbers protect different parties. The *request* (booking) protects *you* — you're guaranteed your minimum and placed somewhere with room. The *limit* (fire-code max) protects *everyone else* — you can't grow without bound and starve the neighbors. And the consequences differ by resource: exceed your *space* allotment (memory) and you're *removed* from the room (OOMKilled); exceed your *noise* allotment (CPU) and you're just told to *quiet down* (throttled), not evicted.

## 3. Formal Definition

- **CPU and memory units.** CPU is measured in **millicores**: `1000m` = 1 full core, `100m` = 0.1 core. Memory in bytes with suffixes: `256Mi` (mebibytes), `1Gi` (gibibytes).
- **Request** (`resources.requests`). The amount **reserved** for the container. The **scheduler** sums requests to decide if a node has room, and places the Pod only where its requests fit. The container is *guaranteed* at least this much. Under contention, CPU is shared *proportionally to requests*.
- **Limit** (`resources.limits`). The **maximum** the container may use.
  - **Memory limit exceeded → OOMKilled.** Memory is incompressible — you can't "slow down" RAM use — so the kernel *kills* the container (exit reason `OOMKilled`), and the Deployment restarts it (Chapter 6).
  - **CPU limit exceeded → throttled.** CPU is compressible — the container is *slowed* (CPU cycles withheld), not killed. It runs, just slower.
- **Quality of Service (QoS) classes**, derived from requests/limits:
  - **Guaranteed** — requests == limits for all resources. Most stable; last to be evicted.
  - **Burstable** — requests < limits (can burst above request up to limit). Cortex is here.
  - **BestEffort** — no requests/limits. First evicted under node pressure. Avoid for anything important.
- **Eviction.** When a node runs low on memory, Kubernetes evicts Pods, preferring BestEffort, then Burstable over their requests — another reason to set requests.

> Requests = guaranteed minimum + scheduling hint (reserved for you); limits = hard ceiling (over memory → OOMKilled; over CPU → throttled). Set both so the scheduler places Pods well and no container can starve its neighbors.

## 4. Worked Example — Cortex's resources

Cortex's container declares (from [`deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)):

```yaml
resources:
  requests:
    cpu: 100m        # guaranteed 0.1 core; scheduler reserves this
    memory: 256Mi    # guaranteed 256 MiB
  limits:
    cpu: 1000m       # may burst up to 1 full core, then THROTTLED
    memory: 1Gi      # may use up to 1 GiB, then OOMKilled
```

Read both numbers' jobs. The **requests** (`100m` / `256Mi`) tell the scheduler "place this Pod on a node with at least 0.1 core and 256 MiB free, and reserve that for it." So Cortex is *guaranteed* a baseline even on a busy node, and the scheduler won't overpack. The **limits** (`1000m` / `1Gi`) cap the burst: Cortex can use up to a full core when busy (compiling a response, handling load), but if it tries to exceed 1 core it's *throttled* (slowed), and if its memory use exceeds 1 GiB it's **OOMKilled** — terminated, then restarted by the Deployment.

That OOMKill is exactly the `crash` event you watched in Chapter 6's self-healing widget: a pod (`cortex-7df-b2`) exceeded its memory limit, was killed with reason `OOMKilled`, and the ReplicaSet started a replacement. Now you know *why* it died — it's not a random crash, it's the memory *limit* doing its job (protecting the node's other workloads), and the *request/limit* gap (`256Mi`→`1Gi`) makes Cortex *Burstable*: it normally uses near its request, can burst toward the limit under load, and dies if it blows past it. Tuning these numbers is a real operational task: too-low a memory limit and you OOMKill under normal load; too-high and you waste capacity and risk starving neighbors. Cortex's modest values fit a homelab; a high-traffic service would profile its real usage and set them accordingly.

## 5. Build It

Run this. It models the scheduler placing Pods by request, and limits enforcing the OOMKill-vs-throttle distinction.

```python run
NODE = {"cpu": 1000, "memory": 2048}      # one node: 1000m CPU, 2048Mi memory

def schedule(pods):
    used = {"cpu": 0, "memory": 0}; placed = []
    for p in pods:
        # Scheduler checks if the node has room for the REQUEST.
        if used["cpu"] + p["req_cpu"] <= NODE["cpu"] and used["memory"] + p["req_mem"] <= NODE["memory"]:
            used["cpu"] += p["req_cpu"]; used["memory"] += p["req_mem"]; placed.append(p["name"])
        else:
            print(f"   {p['name']}: UNSCHEDULABLE (no room for its request) -> stays Pending")
    print(f"   placed: {placed}  | reserved {used}")
    return placed

pods = [
    {"name": "cortex-1", "req_cpu": 100, "req_mem": 256, "lim_mem": 1024},
    {"name": "cortex-2", "req_cpu": 100, "req_mem": 256, "lim_mem": 1024},
    {"name": "hungry",   "req_cpu": 800, "req_mem": 1600, "lim_mem": 1700},
]
print("scheduling by request:")
schedule(pods)

def run_with_limit(name, actual_mem, actual_cpu, lim_mem, lim_cpu):
    if actual_mem > lim_mem:  return f"   {name}: mem {actual_mem}>{lim_mem} -> OOMKilled (terminated)"
    if actual_cpu > lim_cpu:  return f"   {name}: cpu {actual_cpu}>{lim_cpu} -> THROTTLED (slowed, alive)"
    return f"   {name}: within limits -> healthy"

print("\nenforcing limits at runtime:")
print(run_with_limit("cortex-1", actual_mem=1200, actual_cpu=500, lim_mem=1024, lim_cpu=1000))  # OOMKilled
print(run_with_limit("cortex-2", actual_mem=400,  actual_cpu=1500, lim_mem=1024, lim_cpu=1000)) # throttled
```

**Now break it.** Add a fourth pod that requests more memory than the node has free — it's `UNSCHEDULABLE` and stays `Pending` forever (the scheduler can't conjure capacity; you'd add a node or lower the request — exactly the "desired state that can't be met" edge from Chapter 5). Then change `cortex-1`'s actual memory to just *under* its limit — it survives, but lower the *limit* to `300` and it OOMKills under the same load. This is the daily reality of resource tuning: the numbers are a contract between your app's real appetite and the node's capacity, and both OOMKills (limit too low) and unschedulable Pods (request too high) are configuration signals, not random failures.

## 6. Trade-offs & Complexity

| Set requests & limits | No resource governance |
|---|---|
| Scheduler places Pods sensibly | Overpacked nodes, contention |
| Guaranteed minimum (requests) | Pods starved under load |
| Neighbors protected (limits) | One leak takes down the node |
| Predictable OOMKill vs throttle | Random kernel OOM-kills (anyone) |
| Must profile + tune the numbers | "Simpler" but unstable |

The cost is *tuning*, and it requires actually knowing your app's resource appetite — which means profiling real usage, not guessing. Set memory limits too low and you OOMKill under normal traffic; too high and you waste capacity (and risk node pressure). Set requests too high and Pods go unschedulable or you pay for idle reservation; too low and Pods get starved or evicted first. There's no universal right answer — it depends on the workload — which is why this is genuinely one of the harder operational skills. But the alternative (no limits) is worse: a single leaky container can take down a whole node and everything on it. Even imperfect limits beat none.

## 7. Edge Cases & Failure Modes

- **OOMKilled loops.** A memory limit below the app's real working set causes repeated OOMKills (a crash loop that *looks* like a bug). Check `kubectl describe pod` for `OOMKilled` and raise the limit (or fix the leak).
- **CPU throttling masquerading as slowness.** A too-low CPU limit throttles the app under load — it's not "down," just mysteriously slow, and probes may time out. Profile and raise the CPU limit.
- **No requests = BestEffort = first evicted.** A Pod without requests is the first thing killed under node memory pressure. Always set requests for anything that matters.
- **Requests too high.** Over-requesting reserves capacity you don't use, leaving nodes "full" of idle reservations and making Pods unschedulable. Right-size to real usage.

## 8. Practice

> **Exercise 1 — Killed or throttled?** For each, say what happens: (a) a container exceeds its memory limit; (b) it exceeds its CPU limit; (c) it has no limits and the node runs out of memory.

<details>
<summary><strong>Answer</strong></summary>

The deciding property (§3) is whether the resource is *compressible*. Memory isn't — you can't "slow down" RAM use — so over-use is fatal. CPU is — you can withhold cycles — so over-use is merely slowing.

- **(a) exceeds memory limit → OOMKilled.** Memory is incompressible, so the kernel *terminates* the container with reason `OOMKilled` (exit 137), and the Deployment restarts it (Chapter 6). Killed, not slowed.
- **(b) exceeds CPU limit → throttled.** CPU is compressible, so the container is *slowed* — cycles are withheld — but it keeps running. It's not killed, just mysteriously sluggish (and may start failing latency-sensitive probes).
- **(c) no limits, node out of memory → eviction (and possibly a kernel OOM-kill of *anyone*).** A container with no requests/limits is `BestEffort` QoS — the *first* thing Kubernetes evicts under node memory pressure. Worse, without limits a leak can exhaust the whole node, and the kernel's OOM-killer may kill *innocent* neighbors. This is the chaos that requests/limits exist to prevent.

The headline: *over memory → killed; over CPU → throttled; no limits → first evicted and a danger to the whole node.*

</details>

> **Exercise 2 — Explain the OOMKill.** Connect this chapter to Chapter 6: when the widget's pod was `OOMKilled`, which number did it exceed, and what restarted it?

<details>
<summary><strong>Answer</strong></summary>

It exceeded its **memory *limit*** — the `1Gi` ceiling (not the `256Mi` request, which is only the *guaranteed minimum* the scheduler reserves). When the container's actual memory use crossed `1Gi`, the kernel killed it with reason `OOMKilled` (exit 137) — because memory is incompressible, there's no "slow it down," only terminate.

What restarted it was the **Deployment** (via its ReplicaSet), doing exactly the *self-healing* from Chapter 6: actual state (a dead Pod) drifted from desired state (one running replica), so the controller started a replacement to reconcile. So the `crash` event you watched wasn't a random failure — it was the memory *limit* doing its job (protecting the node's other workloads from this Pod's appetite), and the controller doing *its* job (restoring desired state). The request/limit gap (`256Mi` → `1Gi`) is what makes Cortex *Burstable*: it normally runs near its request, can burst toward the limit under load, and dies only if it blows past the ceiling.

</details>

> **Exercise 3 — Right-size.** Cortex requests `256Mi` and limits `1Gi`. What QoS class is that, and what's the risk of (a) lowering the limit to `300Mi`, (b) raising the request to `2Gi`?

<details>
<summary><strong>Answer</strong></summary>

**QoS class: Burstable.** Requests are set but *less than* limits (`256Mi` < `1Gi`), which is the definition of Burstable (§3) — not Guaranteed (which needs requests == limits) and not BestEffort (no requests/limits). It can run at its request and *burst* up toward the limit under load.

- **(a) lowering the limit to `300Mi`:** the ceiling now sits barely above the request, so the moment Cortex's real working set exceeds `300Mi` — easily reached under normal load (JVM + a chunky request) — it gets **OOMKilled**, likely repeatedly (an OOMKill loop that *looks* like a bug but is really a too-tight limit). You've removed almost all burst headroom.
- **(b) raising the request to `2Gi`:** the *scheduler* now must find a node with `2Gi` free and *reserves* it for Cortex whether or not it's used. Risks: the Pod may become **unschedulable** (`Pending`) if no node has that much, and you *waste capacity* — `2Gi` of reservation sits idle on the node, making it look "full" and blocking other Pods, while Cortex actually uses a fraction of it.

Right-sizing is the balance: the *limit* must be above real peak usage (or you OOMKill), and the *request* should track real baseline usage (too high wastes/blocks, too low risks starvation and earlier eviction). Both OOMKills and Pending pods are *configuration signals*, not random failures.

</details>

```quiz
{
  "prompt": "In Kubernetes, what happens when a container exceeds its MEMORY limit versus its CPU limit?",
  "input": "Choose one:",
  "options": [
    "Exceeding the memory limit gets the container OOMKilled (terminated, then restarted); exceeding the CPU limit gets it throttled (slowed, but kept alive)",
    "Both cause the container to be killed immediately",
    "Both just slow the container down",
    "Exceeding either limit deletes the whole node"
  ],
  "answer": "Exceeding the memory limit gets the container OOMKilled (terminated, then restarted); exceeding the CPU limit gets it throttled (slowed, but kept alive)"
}
```

## In the Wild

- **[Kubernetes — Managing resources for containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)** — requests, limits, units, and behavior.
- **[Kubernetes — Pod QoS classes](https://kubernetes.io/docs/concepts/workloads/pods/pod-qos/)** — Guaranteed / Burstable / BestEffort and eviction order.
- **[Cortex `deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)** — the real `requests`/`limits` (and the `OOMKilled` they explain).

---

**Next:** by default a container can run as root with broad privileges — a gift to an attacker who breaks in. The security context locks it down: non-root, dropped capabilities, no privilege escalation. → [12. Security context](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/security-context)
