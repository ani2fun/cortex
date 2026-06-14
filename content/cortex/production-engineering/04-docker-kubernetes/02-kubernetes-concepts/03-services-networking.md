---
title: '7. Services & cluster networking'
summary: Pods are ephemeral and get a new IP every time they''re replaced — so you can never address them directly. A Service is a stable virtual IP and DNS name that load-balances across the Pods behind it.
---

# 7. Services & cluster networking

## TL;DR

> Pods are disposable (Chapter 6) — replaced constantly, with a new IP each time — so you can't reliably talk to one by its address. A **Service** fixes this: it's a *stable* virtual IP and DNS name that sits in front of a set of Pods (selected by **labels**) and **load-balances** requests across the healthy ones. Other components reach your app by the Service's *name* (`cortex`), never a Pod IP. The common type is **ClusterIP** (reachable only *inside* the cluster). Cortex's `Service` exposes the app on port 80 → the Pods' 8080, so everything internal — and the Ingress (Chapter 8) — reaches it by name.

## 1. Motivation

Chapter 6 left us with a problem we created on purpose: Pods are *cattle*. They're replaced on every crash, every node failure, every deploy — and each new Pod gets a *different IP*. That's great for resilience, but it makes Pods *unaddressable*: if the frontend hard-coded "talk to the backend Pod at 10.1.2.3," that address would be wrong minutes later when the Pod was replaced. You've made the app robust by making its individual parts ephemeral — and now nothing can find them.

A Service is the missing *stable front door*. It provides one unchanging virtual IP and one DNS name that *always* route to whatever healthy Pods currently back the app, no matter how many times those Pods are replaced underneath. It also **load-balances** — spreading traffic across all the replicas — and *only* sends traffic to Pods that are *ready* (Chapter 10), so a crashed or starting Pod silently drops out of rotation. The Service is the indirection that reconciles "Pods are disposable" with "the app must be reachable." It's the same lesson as docker-compose's service-name networking (Chapter 4), formalized and made dynamic for a world where the backends are constantly churning.

## 2. Intuition (Analogy)

A company's **main phone number and receptionist.** Individual employees come and go, change desks, work from home, get replaced — their direct extensions are unstable and you'd never put one on a business card. Instead, customers call *one* stable main number, and the receptionist routes each call to *whichever* available employee can handle it. Employees churn constantly; the *main number never changes*, and calls always reach *someone* who's actually available.

A Service is that main number plus the receptionist. The Pods are the employees — ephemeral, replaced, at ever-changing "extensions" (IPs). The Service is the permanent main number (stable virtual IP + DNS name), and its load-balancing is the receptionist routing each request to a *ready* Pod. Callers (other services, the Ingress) dial the *main number* (`cortex`); they never need to know which employee — which Pod — actually picks up.

## 3. Formal Definition

- **Service.** A stable network endpoint fronting a *set* of Pods. It has a stable **virtual IP** (ClusterIP) and a **DNS name** (`<service>.<namespace>.svc.cluster.local`, usually just `<service>` within a namespace). It **selects** its backing Pods by **label selector** (the same labels the Deployment sets) and **load-balances** across the *ready* ones.
- **Endpoints.** Kubernetes continuously updates the Service's set of backing Pod IPs as Pods come and go (and as readiness changes) — so the Service always points at currently-healthy Pods. This is the dynamic glue that makes the stable name work over churning Pods.
- **Service types:**
  - **`ClusterIP`** (default) — reachable *only inside* the cluster. The normal type for internal services (Cortex uses this; external traffic arrives via the Ingress, Chapter 8).
  - **`NodePort`** — also opens a port on every node's IP. Crude external access.
  - **`LoadBalancer`** — provisions an external (usually cloud) load balancer. Full external exposure.
  - **`Headless`** (`clusterIP: None`) — no virtual IP; returns the Pod IPs directly (for stateful apps that need per-Pod addressing).
- **`port` vs `targetPort`.** The Service listens on `port`; it forwards to the Pods' `targetPort` (the container's actual port). Cortex: `port: 80` → `targetPort: 8080`.
- **kube-proxy.** The component on each node that implements Service load-balancing (via iptables/IPVS), so the stable virtual IP transparently fans out to Pod IPs.

> A Service is a stable virtual IP + DNS name that label-selects a set of Pods and load-balances across the *ready* ones, with Kubernetes keeping the backing Pod set up to date as Pods churn. Address the app by the Service name; never a Pod IP.

## 4. Worked Example — Cortex's Service

Cortex's [`service.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/service.yaml):

```yaml
apiVersion: v1
kind: Service
metadata:
  name: cortex
spec:
  type: ClusterIP                       # internal-only; the Ingress reaches it (Chapter 8)
  selector:                             # which Pods back this Service — by LABEL
    app.kubernetes.io/name: cortex
    app.kubernetes.io/component: app
  ports:
    - name: http
      port: 80                          # the Service listens on 80...
      targetPort: 8080                  # ...and forwards to the Pods' container port 8080
```

Read it against the Deployment (Chapter 6). The Service's `selector` (`app.kubernetes.io/name: cortex`) matches the *labels* the Deployment stamps on its Pods — that label match is the entire coupling. So this Service automatically fronts *whatever* Cortex Pods currently exist: scale to 3, all 3 are behind it; a Pod crashes and is replaced, the new one (new IP) is automatically added to the Service's endpoints and the dead one removed — *without changing the Service*. Anything in the cluster reaches Cortex at the stable name `cortex` (or `cortex.databases-prod.svc.cluster.local` fully qualified), on port 80, and the Service forwards to a ready Pod's 8080. The Pods churn; `cortex:80` is forever. (This is exactly why, back in docker-compose, the app addressed `db:5432` by name — Services are the cluster-scale version of that idea, but dynamic over a *set* of replicas.)

Because it's `ClusterIP`, this Service is *not* reachable from the public internet — only from inside the cluster. External users reach Cortex through the Ingress (Chapter 8), which itself routes to this Service by name. Layered indirection: internet → Ingress → Service → ready Pod.

## 5. Build It

Run this. It models a Service: a stable name fronting a churning set of Pods, load-balancing only across ready ones.

```python run
import itertools

class Service:
    def __init__(self, name, selector):
        self.name, self.selector = name, selector
        self._rr = itertools.count()
    def endpoints(self, all_pods):
        # Dynamically select READY pods whose labels match the selector.
        return [p for p in all_pods
                if p["ready"] and self.selector.items() <= p["labels"].items()]
    def route(self, all_pods):
        eps = self.endpoints(all_pods)
        if not eps: return "503 — no ready pods"
        return eps[next(self._rr) % len(eps)]["ip"]        # round-robin load-balance

svc = Service("cortex", selector={"app": "cortex"})

pods = [
    {"ip": "10.1.0.5", "labels": {"app": "cortex"}, "ready": True},
    {"ip": "10.1.0.6", "labels": {"app": "cortex"}, "ready": True},
    {"ip": "10.1.0.7", "labels": {"app": "cortex"}, "ready": False},   # starting up — excluded
    {"ip": "10.9.9.9", "labels": {"app": "other"},  "ready": True},    # different app — excluded
]

print("Service 'cortex' routes 4 requests (load-balanced across READY, matching pods):")
for _ in range(4):
    print("   ->", svc.route(pods))

print("\na pod is REPLACED (new IP), Service auto-updates its endpoints:")
pods[0] = {"ip": "10.1.0.99", "labels": {"app": "cortex"}, "ready": True}   # b2 replaced by d4
print("   ->", svc.route(pods), "(callers still dial 'cortex' — unchanged)")
```

**Now break it.** Mark *all* matching pods `"ready": False` (e.g. mid-deploy, none ready yet) and route — the Service returns `503`, because it only routes to *ready* Pods and there are none. That's correct behavior: the Service shields callers from Pods that aren't ready to serve, rather than sending traffic into a starting or broken Pod. Then add a new ready Pod and watch traffic resume automatically. The caller's code never changed — it always just dials `cortex`. The Service absorbs *all* the churn (replacement, scaling, readiness) behind one stable name, which is precisely why you address apps by Service, never by Pod IP.

## 6. Trade-offs & Complexity

| Service (stable front door) | Addressing Pods directly |
|---|---|
| Stable name/IP over churning Pods | Breaks every time a Pod is replaced |
| Load-balances across replicas | No balancing |
| Only routes to *ready* Pods | Can hit a starting/dead Pod |
| Auto-updates endpoints | Manual, impossible to maintain |
| One more object + concept | "Simpler" but unusable |

There's essentially no trade-off to *avoid* using a Service — addressing Pods directly is simply broken in a system where Pods churn. The conceptual cost is understanding the layers (Service → endpoints → Pods, coupled by labels) and the types (when ClusterIP vs LoadBalancer vs headless). For Cortex, ClusterIP is exactly right: internal stability, with the Ingress handling external exposure. The one genuine subtlety is that load-balancing happens at the connection/L4 level by default — long-lived connections (websockets, gRPC streams) can pin to one Pod, which matters for some apps (and is part of why an L7 Ingress, Chapter 8, exists).

## 7. Edge Cases & Failure Modes

- **Hard-coding Pod IPs.** The original sin — a Pod IP is valid only until that Pod is replaced (minutes). Always use the Service name.
- **Selector/label mismatch.** If a Service's `selector` doesn't match any Pod's labels, it has *zero* endpoints and returns `503`/connection-refused — a very common "why can't anything reach my app?" bug. Verify the labels match the Deployment's.
- **No ready Pods.** A Service with all-unready backends serves nothing (correctly). If that's unexpected, the Pods are failing readiness (Chapter 10) — look there, not at the Service.
- **Expecting external access from ClusterIP.** A ClusterIP Service is *internal only*. Trying to hit it from your laptop won't work; you need an Ingress (Chapter 8), a `port-forward`, or a `LoadBalancer`/`NodePort`.

## 8. Practice

> **Exercise 1 — Why not the Pod?** In two sentences, explain why you address Cortex as `cortex` (the Service) rather than a Pod IP, referencing what happens when a Pod is replaced.

<details>
<summary><strong>Answer</strong></summary>

A Pod is ephemeral: every time it's replaced — on a crash, node failure, or deploy — it comes back with a *different IP* (§1), so any hard-coded Pod IP is wrong minutes later and the caller can no longer find the app. The `cortex` **Service** gives one *stable* virtual IP and DNS name that always routes to whatever healthy Pods currently back the app (Kubernetes auto-updates the Service's endpoints as Pods churn), so you address the unchanging name and let the Service absorb all the replacement and rescheduling underneath.

</details>

> **Exercise 2 — Trace a request.** A request hits the `cortex` Service. Walk through how it reaches a specific container: selector → endpoints → load-balance → targetPort.

<details>
<summary><strong>Answer</strong></summary>

The Service turns a stable name into a specific container in four steps (§3):

1. **Selector → which Pods.** The Service's **label selector** (`app.kubernetes.io/name: cortex`, `component: app`) defines *which* Pods are "its" backends — the same labels the Deployment stamps on its Pods. That label match is the entire coupling.
2. **Endpoints → which are ready.** Kubernetes continuously maintains the Service's **endpoints**: the IPs of the matching Pods that are currently **ready** (Chapter 10). A starting, crashed, or unready Pod is *not* in the set, so it never receives traffic.
3. **Load-balance → pick one.** `kube-proxy` (via iptables/IPVS) picks *one* ready endpoint from that set — load-balancing requests across the healthy replicas.
4. **targetPort → the container's port.** The request arrived on the Service's `port` (80); the Service forwards it to the chosen Pod's `targetPort` (8080), the container's actual listening port.

So: `cortex:80` → (label-selected, ready) endpoints → one ready Pod → its `:8080`. The caller only ever knows the stable name and port; the Service resolves it to a live container, request by request.

</details>

> **Exercise 3 — Debug zero endpoints.** Your app's Service returns connection-refused. The Pods are Running. What's the most likely misconfiguration, and how do you check it?

<details>
<summary><strong>Answer</strong></summary>

**Most likely cause: a selector/label mismatch.** The Service finds its backends *only* by matching its `selector` against Pod **labels** (§3, §7). If the Service's selector doesn't exactly match the labels the Deployment puts on its Pods (a typo, a wrong key, a missing label), the Service selects **zero Pods** — so it has *no endpoints* and refuses connections, even though the Pods themselves are perfectly healthy and `Running`. (Healthy pods that nothing routes to is the giveaway: the problem is the *coupling*, not the pods.)

**How to check it:**

- Look at the Service's endpoints — `kubectl get endpoints cortex` (or `kubectl describe service cortex`). **Empty endpoints** confirm the diagnosis: the Service is backing nothing.
- Compare the two label sets: the Service's `spec.selector` versus the Pods' actual labels (`kubectl get pods --show-labels`, or the Deployment's `template.metadata.labels`). They must match *exactly*.

Fix by aligning them (correct the selector or the Pod labels so they agree), and the endpoints populate immediately. (A secondary possibility worth ruling out: the Pods *are* selected but none are **ready** — §7 — which also yields no endpoints; check readiness probes if the labels match.)

</details>

```quiz
{
  "prompt": "Why do you address a Kubernetes app through a Service rather than a Pod's IP?",
  "input": "Choose one:",
  "options": [
    "Pods are ephemeral (new IP each time they're replaced); a Service gives a stable IP/DNS name that load-balances across the currently-ready Pods, auto-updating as Pods churn",
    "Services make the app run faster",
    "Pod IPs are encrypted and unreadable",
    "Services replace the need for a Deployment"
  ],
  "answer": "Pods are ephemeral (new IP each time they're replaced); a Service gives a stable IP/DNS name that load-balances across the currently-ready Pods, auto-updating as Pods churn"
}
```

## In the Wild

- **[Kubernetes — Service](https://kubernetes.io/docs/concepts/services-networking/service/)** — types, selectors, endpoints, and load-balancing.
- **[Kubernetes — DNS for Services and Pods](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/)** — how the `<service>.<namespace>.svc.cluster.local` names work.
- **[Cortex `service.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/service.yaml)** — the real ClusterIP Service fronting Cortex's Pods.

---

**Next:** the Service is internal-only. To let the *public internet* reach `cortex.kakde.eu` — with HTTPS — you need an Ingress, plus automatic TLS and DNS. → [8. Ingress, TLS & DNS](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/ingress-tls-dns)
