---
title: '10. Probes: liveness, readiness, startup'
summary: Kubernetes restarts dead containers and routes traffic only to ready ones — but how does it know? Through probes. Liveness checks "is it alive?", readiness checks "can it serve?", startup gives slow apps time to boot.
---

# 10. Probes: liveness, readiness, startup

## TL;DR

> Kubernetes' self-healing and load-balancing rely on *knowing* each container's health, which it learns by running **probes**. A **liveness** probe asks "is this container *alive*?" — fail it and Kubernetes **restarts** the container (cures deadlocks/hangs). A **readiness** probe asks "can it *serve traffic right now*?" — fail it and the Service **stops sending traffic** to that Pod (without restarting it). A **startup** probe asks "has it *finished booting*?" — it gives slow-starting apps time before liveness kicks in. Cortex probes `/api/health` for startup and readiness, and a TCP check for liveness — which is *why* the fail-fast migration (Part 2) keeps traffic off a not-yet-ready pod.

## 1. Motivation

Two earlier promises depend on a capability we haven't explained. Chapter 6's *self-healing* ("restart a crashed Pod") and Chapter 7's *load-balancing to ready Pods only* both require Kubernetes to *know* a container's health — but a container being "running" (its process exists) tells you almost nothing. A process can be *running but deadlocked* (alive but useless — needs a restart). A process can be *running but not yet ready* (still loading data, running migrations — shouldn't get traffic *yet*, but doesn't need a restart). A process can be *slow to start* (a JVM warming up — don't kill it for being slow). "Running" conflates all of these, and getting them confused causes real outages: restart-looping a healthy-but-slow app, or routing traffic into a Pod that isn't ready.

Probes are how Kubernetes distinguishes these states. By periodically *asking* the container specific questions — "are you alive?", "are you ready?", "have you started?" — Kubernetes makes *informed* decisions: restart the truly-dead, withhold traffic from the not-ready, and patiently wait for the still-booting. This is also the missing link that makes Part 2's *fail-fast migration* actually safe: a Cortex pod whose startup migration fails *never passes its readiness probe*, so the Service never routes a single request to it. Probes are the sensory system that turns "desired state" into *correct* action.

## 2. Intuition (Analogy)

A **hospital's patient monitoring**, with three different checks for three different decisions:

- **Liveness = the heart monitor.** Is the patient *alive at all*? Flatline → **resuscitate** (restart). It's not asking "are they well," just "are they alive."
- **Readiness = the discharge/triage check.** Is the patient *ready to receive visitors / take on activity right now*? A post-op patient is *alive* but not ready for a marathon — *don't send anyone in yet*, but also *don't resuscitate* them; just *wait* until they're ready. (Traffic, not life support.)
- **Startup = the "still in surgery, don't disturb" sign.** A patient *mid-operation* shouldn't have the heart monitor's alarms misread as "dead" — give the procedure time to finish before normal monitoring begins.

Confuse these and you do harm: resuscitating a patient who's merely resting (restarting a slow app), or sending visitors to someone not ready (routing traffic to an unready Pod). Three checks, three distinct actions — *restart*, *withhold traffic*, *wait* — for three distinct states.

## 3. Formal Definition

A **probe** is a periodic check Kubernetes runs against a container. Each can be an `httpGet` (expect a 2xx/3xx), a `tcpSocket` (expect the port to accept a connection), or an `exec` (run a command, expect exit 0). Three kinds, three consequences:

| Probe | Question | On failure | Use it for |
|---|---|---|---|
| **Liveness** | "Is it *alive*?" | **Restart** the container | Deadlocks, hangs, unrecoverable states |
| **Readiness** | "Can it *serve now*?" | **Remove from Service endpoints** (no traffic; *no* restart) | "Not ready yet" — booting, dependency down, draining |
| **Startup** | "Has it *finished starting*?" | Restart (but holds off liveness/readiness until it passes) | Slow-booting apps (JVM, big init, migrations) |

Tuning knobs: `initialDelaySeconds` (wait before first probe), `periodSeconds` (how often), `failureThreshold` (consecutive fails before acting), `timeoutSeconds`. The **startup probe** exists to solve a specific trap: without it, you'd have to set the liveness probe's `initialDelay` long enough for the *slowest* boot, weakening it forever; the startup probe instead gives generous time to *boot* and then hands off to a *tight* liveness probe.

> Liveness = restart-if-dead; readiness = no-traffic-if-not-ready (no restart); startup = wait-for-slow-boot before the others. Together they let Kubernetes restart the truly broken, withhold traffic from the not-ready, and not kill the merely-slow.

## 4. Worked Example — Cortex's probes

Cortex's [`deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml) configures all three:

```yaml
startupProbe:                          # give the app time to boot (JVM + migrations)
  httpGet: { path: /api/health, port: 8080 }
  failureThreshold: 30                 # 30 × 5s = up to 150s to start before giving up
  periodSeconds: 5

readinessProbe:                        # only route traffic once /api/health is OK
  httpGet: { path: /api/health, port: 8080 }
  initialDelaySeconds: 5
  periodSeconds: 10

livenessProbe:                         # restart if the port stops accepting connections
  tcpSocket: { port: 8080 }
  initialDelaySeconds: 30
```

Trace Cortex's boot through its probes. When the container starts, the **startup probe** polls `/api/health` every 5s, tolerating up to 30 failures (~150s) — generous, because Cortex must boot the JVM *and* run Liquibase migrations (Part 2, Chapter 7) before it's healthy. Until the startup probe passes, liveness and readiness are *held off*, so a slow-but-fine boot is never mistaken for "dead." Once `/api/health` returns OK, the **readiness probe** takes over: the Service (Chapter 7) only adds this Pod to its endpoints — only sends it traffic — while `/api/health` keeps passing. And the **liveness probe** (a cheap TCP check on 8080) restarts the container if the port goes dead (a hang). 

Now the payoff for Part 2's *fail-fast*: recall a failed startup migration crashes the boot, so the server never opens port 8080 and `/api/health` never returns OK. That means the **readiness probe never passes**, so the Service routes *zero* traffic to the broken pod, and during a rolling deploy the new (broken) pod never becomes ready — so the rollout stalls with the *old healthy version still serving* (Part 2, Chapter 8). The probes are the mechanism that makes "a dead pod is better than a broken one" *actually keep users safe*: Kubernetes only sends traffic where readiness says "yes." The `/api/health` endpoint Cortex exposes (a simple ZIO handler, Part 1) is small but load-bearing — it's the single signal the whole orchestration trusts.

## 5. Build It

Run this. It models the three probes and the distinct action each triggers — including readiness withholding traffic from a not-yet-ready pod.

```python run
class Pod:
    def __init__(self):
        self.started = False; self.alive = True; self.health_ok = False
        self.restarts = 0; self.gets_traffic = False

    def check_probes(self):
        # STARTUP: until it passes, hold off the others.
        if not self.started:
            print("   startup probe: not started yet -> wait (liveness/readiness held off)")
            return
        # LIVENESS: dead -> restart.
        if not self.alive:
            self.restarts += 1; self.alive = True; self.health_ok = False; self.gets_traffic = False
            print(f"   liveness FAILED -> RESTART (count={self.restarts})")
            return
        # READINESS: not ready -> no traffic (but NO restart).
        self.gets_traffic = self.health_ok
        print(f"   readiness: {'OK -> Service sends traffic' if self.health_ok else 'NOT ready -> NO traffic (no restart)'}")

p = Pod()
print("boot: JVM + migrations running...");        p.check_probes()        # startup: wait
print("started, but /api/health not yet OK:");     p.started = True; p.check_probes()  # ready: no traffic
print("/api/health now OK:");                       p.health_ok = True; p.check_probes() # ready: traffic
print("the app deadlocks (port hangs):");          p.alive = False; p.check_probes()    # liveness: restart
print(f"\nfinal: restarts={p.restarts}, gets_traffic={p.gets_traffic}")
```

**Now break it.** Simulate a *failed startup migration*: keep `p.health_ok = False` forever (the server never becomes healthy). Run `check_probes()` repeatedly — the pod *never gets traffic* (readiness never passes), exactly as Part 2's fail-fast intends. Then *remove* the readiness probe entirely (make `gets_traffic = True` as soon as `started`): now the broken pod receives traffic the moment its container starts, serving 500s to users — the precise disaster readiness exists to prevent. The three probes aren't redundant: each guards a different failure mode, and dropping the readiness probe quietly undoes the safety of everything in Part 2.

## 6. Trade-offs & Complexity

| Well-tuned probes | No / bad probes |
|---|---|
| Dead containers restart automatically | Hung containers stay broken |
| Traffic only to ready Pods | Users hit not-ready Pods (500s) |
| Slow boots tolerated (startup probe) | Slow apps restart-loop |
| Fail-fast actually protects users | A broken pod still serves |
| Three probes to configure correctly | "Simpler" but unsafe |

The cost is *tuning*, and it's genuinely fiddly: too-aggressive liveness restart-loops a slow app; too-lax readiness sends traffic to broken Pods; a missing startup probe forces you to weaken liveness. Bad probe config is itself a common cause of outages (a liveness probe that hits a slow dependency can cascade restarts across a fleet). But *correct* probes are what make self-healing and safe rollouts real rather than aspirational. The key discipline: liveness should be *cheap and local* (don't check dependencies — that turns a dependency blip into a restart storm); readiness *can* reflect dependencies; and slow boots get a startup probe.

## 7. Edge Cases & Failure Modes

- **Liveness probe that checks dependencies.** If liveness fails because (say) the database is briefly down, Kubernetes restarts the app — which doesn't fix the database and just removes capacity. Keep liveness *local and cheap*; put dependency checks in *readiness*.
- **No startup probe for a slow app.** Without it, liveness fires during a long boot and restart-loops the app forever. Add a startup probe with a generous `failureThreshold`.
- **Readiness that never reflects "draining."** During shutdown, a Pod should fail readiness *before* it stops accepting connections, so traffic drains gracefully. Otherwise in-flight requests get dropped.
- **Probe timeouts too tight.** A `timeoutSeconds` shorter than a healthy response time marks healthy Pods as failed under load — a self-inflicted outage. Tune to real latencies.

## 8. Practice

> **Exercise 1 — Which probe?** For each, name liveness/readiness/startup and the action: (a) the app deadlocked; (b) it's still loading a cache; (c) the JVM takes 90s to warm up; (d) a dependency is temporarily down (don't restart).

<details>
<summary><strong>Answer</strong></summary>

Match each state to the *action* it needs (§3): restart the truly-dead, withhold traffic from the not-ready, wait for the still-booting.

- **(a) deadlocked → liveness; action = restart.** The process is alive but useless and can't recover on its own. Only a restart cures a hang — exactly what a failed liveness probe triggers.
- **(b) loading a cache → readiness; action = withhold traffic (no restart).** It's healthy, just not *ready to serve yet*. You don't want to kill it (restarting wastes the loading work); you want the Service to *not* route traffic until it's ready.
- **(c) JVM warming up 90s → startup; action = wait, holding off liveness/readiness.** A slow boot must not be mistaken for "dead." A startup probe with a generous `failureThreshold` gives it time, *then* hands off to a tight liveness probe.
- **(d) dependency temporarily down → readiness; action = withhold traffic (do NOT restart).** Restarting the app won't fix the *dependency* — it just removes capacity. Reflect the dependency in *readiness* so traffic pauses; the moment the dependency returns, readiness passes again. (Putting this in liveness causes the restart-storm of Exercise 3.)

The throughline: *restart* (liveness), *withhold traffic* (readiness), *wait* (startup) — three checks for three distinct states, and confusing them does harm.

</details>

> **Exercise 2 — Fail-fast link.** Explain, using readiness, why a Cortex pod with a failed startup migration receives zero traffic and stalls a rolling deploy (tie to Part 2, Chapter 8).

<details>
<summary><strong>Answer</strong></summary>

Fail-fast (Part 2, Ch 8) crashes the boot when a startup migration fails — so the server *never opens port 8080* and `/api/health` *never returns OK*. Trace the consequences through the probes:

- **Zero traffic.** Cortex's readiness probe is an `httpGet` on `/api/health`. If that endpoint never returns OK, the **readiness probe never passes**, so the Service (Chapter 7) never adds this Pod to its endpoints. The Service only routes to Pods readiness says "yes" to — so a broken pod gets *exactly zero* requests. No user ever hits a 500 from it.
- **Stalled rollout.** During a rolling deploy (Ch 14), Kubernetes retires an old Pod only *after* the new one becomes *ready*. Since the broken new Pod never passes readiness, no old Pod is ever drained — the rollout *stalls* with the old, healthy version still fully serving.

That's the payoff of "a dead pod is better than a broken one": readiness is the mechanism that *enforces* it. Fail-fast guarantees a broken boot stays un-ready, and readiness guarantees un-ready pods get no traffic and can't displace healthy ones. The `/api/health` endpoint is small but load-bearing — it's the single signal the whole orchestration trusts.

</details>

> **Exercise 3 — Don't restart-loop.** A teammate's liveness probe calls the database. The DB blips for 20s and the whole fleet restarts. Explain why, and how to fix it.

<details>
<summary><strong>Answer</strong></summary>

**Why it happens:** a liveness failure means *restart the container*. If the liveness probe *checks the database*, then a 20s DB blip makes the probe fail on *every* Pod at once — so Kubernetes restarts the *entire fleet* simultaneously. And restarting the app does nothing to fix the database, so the new Pods' liveness probes fail too: you've turned a brief, recoverable dependency hiccup into a self-inflicted, fleet-wide restart storm — *removing capacity* exactly when the system is already stressed.

**The fix:** keep liveness *local and cheap* — it should answer only "is *this process* alive?" (e.g. a TCP check on the port, or a trivial in-process handler), never "is a *dependency* up?" Put the dependency check in the **readiness** probe instead. Then a DB blip makes Pods *not ready* (so the Service withholds traffic, no restart), and the *instant* the DB returns, readiness passes and traffic resumes — no restarts, no cascade. The discipline (§6): liveness = local-only, readiness = may reflect dependencies. A dependency outage should *pause traffic*, never *trigger restarts*.

</details>

```quiz
{
  "prompt": "What's the difference between a liveness probe and a readiness probe?",
  "input": "Choose one:",
  "options": [
    "Liveness asks 'is it alive?' — failure RESTARTS the container; readiness asks 'can it serve now?' — failure removes it from the Service (no traffic) WITHOUT restarting it",
    "They are identical; the names are interchangeable",
    "Liveness checks the database; readiness checks the network",
    "Readiness restarts the container; liveness withholds traffic"
  ],
  "answer": "Liveness asks 'is it alive?' — failure RESTARTS the container; readiness asks 'can it serve now?' — failure removes it from the Service (no traffic) WITHOUT restarting it"
}
```

## In the Wild

- **[Kubernetes — Liveness, readiness & startup probes](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes)** — the three probe types and their semantics.
- **[Kubernetes — Configure probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)** — the tuning knobs and common pitfalls.
- **[Cortex `deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)** — the real startup/readiness/liveness probes against `/api/health`.

---

**Next:** a container can hog all of a node's CPU and memory, starving its neighbors — or be starved itself. Resource requests and limits keep everyone fair, and explain the `OOMKilled` you saw earlier. → [11. Resources: requests & limits](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/resources)
