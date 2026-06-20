---
title: '15. Observing a live pod'
summary: When something breaks in production, you need to look inside. kubectl''s get, describe, logs, and top are your eyes — and CrashLoopBackOff, OOMKilled, and ImagePullBackOff are the words the cluster uses to tell you what''s wrong.
---

# 15. Observing a live pod

## TL;DR

> When a Pod misbehaves, you diagnose it with four `kubectl` commands. **`get pods`** shows status at a glance (`Running`, `CrashLoopBackOff`, `Pending`). **`describe pod`** shows the *why* — its **events** (scheduling, image pulls, probe failures, OOMKills) and last state. **`logs`** shows the application's own output (and `logs --previous` shows a *crashed* container's last words). **`top pod`** shows live CPU/memory. The cluster speaks in status words you must learn to read: **`CrashLoopBackOff`** (keeps crashing), **`OOMKilled`** (out of memory, Chapter 11), **`ImagePullBackOff`** (can't fetch the image), **`Pending`** (can't be scheduled). Knowing where to look — and what those words mean — is the difference between a five-minute fix and an hour of guessing.

## 1. Motivation

Everything in this Part has been about making the system *run correctly* — self-healing, rolling updates, probes, resources. But production *will* surprise you: a Pod crash-loops, a deploy stalls, an app gets mysteriously slow. When that happens, the skill that matters is **observability** — the ability to *look inside* a running system and understand its state. A Kubernetes cluster is not a black box; it's constantly emitting signals about what every Pod is doing and why. The difference between a senior and a junior engineer in an incident is rarely *knowledge of Kubernetes internals* — it's *knowing which command to run to see the truth*, and being fluent in the status vocabulary the cluster uses to describe failure.

This closing chapter gives you that diagnostic toolkit. It's deliberately practical: a small set of commands that answer "what's happening?", and a glossary of the status words that tell you *what kind* of problem you have (which usually points straight at the fix). Crucially, almost every failure mode you'll see maps to a concept you've already learned in this Part: `OOMKilled` is a resource limit (Chapter 11), a stalled rollout is readiness failing (Chapters 10, 14), `ImagePullBackOff` is a registry/image problem (Chapter 13). Observability is where all the theory becomes a debugging reflex.

## 2. Intuition (Analogy)

A **doctor examining a patient.** When someone's unwell, a good doctor doesn't guess — they run a sequence of checks, each revealing a layer: *vital signs* at a glance (is the patient stable, crashing?), the *chart and history* (what's happened, what interventions, what alarms fired?), the patient's *own words* (where does it hurt?), and *live monitors* (heart rate, oxygen *right now*). Each tool answers a different question, and together they localize the problem.

The `kubectl` commands are that examination. `get pods` is the vital-signs glance (status, restarts). `describe pod` is the chart — the *history of events* (scheduled here, image pulled, probe failed, OOMKilled) that tells the *story* of what went wrong. `logs` is the patient's own words — what the *application* itself reported. `top` is the live monitor — CPU and memory *now*. A skilled operator runs them in sequence, each narrowing the diagnosis, just as a doctor moves from "is this serious?" to "here's exactly what's wrong and why."

## 3. Formal Definition

The core observability commands and what each reveals:

| Command | Answers | Reveals |
|---|---|---|
| **`kubectl get pods`** | "What's the status?" | `Running` / `Pending` / `CrashLoopBackOff` / etc., restart count, age |
| **`kubectl describe pod <name>`** | "*Why* is it like this?" | **Events** (scheduling, image pull, probe results, OOMKills), last state, the reason a Pod is stuck |
| **`kubectl logs <name>`** | "What did the *app* say?" | The container's stdout/stderr. `--previous` shows a *crashed* container's final output; `-f` follows live |
| **`kubectl top pod <name>`** | "How much CPU/memory *now*?" | Live resource usage (vs the requests/limits, Chapter 11) |
| **`kubectl get events`** | "What's happening cluster-wide?" | A stream of recent events across objects |
| **`kubectl rollout status deploy/<name>`** | "Is the deploy progressing?" | Whether a rolling update is advancing or stalled (Chapter 14) |
| **`kubectl exec -it <name> -- sh`** | "Let me look around inside" | A shell in the container (for deeper poking) |

**The status vocabulary you must read fluently** (each usually points at the fix):

- **`Running`** — the container is up. (Not necessarily *ready* — check readiness, Chapter 10.)
- **`Pending`** — not yet scheduled. Usually *no node has room* for its resource requests (Chapter 11), or a volume/affinity can't be satisfied.
- **`CrashLoopBackOff`** — the container starts, crashes, restarts, crashes — repeatedly, with Kubernetes backing off (waiting longer) between attempts. The app is failing at startup. → `logs --previous` for the crash reason.
- **`OOMKilled`** (in the Pod's last state) — the container exceeded its **memory limit** and the kernel killed it (Chapter 11). → raise the limit or fix the leak.
- **`ImagePullBackOff` / `ErrImagePull`** — Kubernetes can't *fetch the image* (wrong tag, registry down, bad credentials, Chapter 13). → fix the image reference or registry access.
- **`Error` / non-zero exit** — the process exited with a failure. → `logs` for the application error.

> Diagnose with `get` (status) → `describe` (events/why) → `logs` (app output) → `top` (resources). Learn the status words — `CrashLoopBackOff`, `OOMKilled`, `ImagePullBackOff`, `Pending` — because each names a *kind* of problem that points straight at the cause (and usually to a concept from this Part).

## 4. Worked Example — diagnosing Cortex's OOMKilled pod

Recall the pod that got `OOMKilled` in Chapters 6 and 11. Here's how you'd actually *find* and *understand* it:

```text
# 1. GLANCE — what's the status?
$ kubectl get pods
NAME                      READY   STATUS             RESTARTS   AGE
cortex-7df-b2             0/1     CrashLoopBackOff   4          6m      # <- restarting repeatedly

# 2. WHY? — describe shows the events and last state.
$ kubectl describe pod cortex-7df-b2
...
    Last State:     Terminated
      Reason:       OOMKilled                # <- exceeded its memory LIMIT (Chapter 11)
      Exit Code:    137                       # 137 = 128 + 9 (SIGKILL) — the OOM signature
...
  Events:
    Warning  BackOff   ...  Back-off restarting failed container

# 3. THE APP'S WORDS — what was it doing before it died?
$ kubectl logs cortex-7df-b2 --previous          # --previous: the CRASHED container's logs
... [cortex] processing large request ...
... (no error — it was killed by the kernel, not a code exception)

# 4. RESOURCES — is it really near its limit?
$ kubectl top pod cortex-7df-b2
NAME            CPU    MEMORY
cortex-7df-b2   250m   1018Mi                    # <- right against its 1Gi limit
```

Read the diagnosis as a story the commands tell together. `get pods` flags `CrashLoopBackOff` (it keeps dying). `describe` reveals the *reason*: `OOMKilled`, exit code `137` — the kernel killed it for exceeding its **memory limit** (the `1Gi` from Chapter 11). `logs --previous` shows *no application error* — confirming it wasn't a code bug but an external kill (the kernel, not a thrown exception). `top` confirms it's pinned at ~1Gi, right at the limit. The fix is now obvious and *concept-backed*: either raise the memory limit (Chapter 11) if the workload legitimately needs more, or find why a request uses so much memory. You didn't *guess* — you *looked*, and the cluster's own vocabulary (`OOMKilled`, `137`) named the problem. Every failure mode in this Part has a similar signature you can read: a stalled rollout (`kubectl rollout status` says "waiting", `describe` shows readiness-probe failures — Chapters 10, 14); a Pending pod (`describe` says "Insufficient memory" — Chapter 11); an `ImagePullBackOff` (`describe` shows the pull error — Chapter 13). Observability turns "it's broken" into "here's exactly which concept failed, and the fix."

## 5. Build It

Run this. It's a tiny diagnostic engine: feed it a Pod's symptoms and it routes you to the cause and the relevant chapter — the reasoning an operator does in an incident.

```python run
def diagnose(status, last_reason=None, exit_code=None, logs="", events=""):
    if status == "Pending":
        return "Pending -> the scheduler can't place it. Likely insufficient node resources for its requests (Ch 11) -> add capacity or lower requests."
    if status == "ImagePullBackOff":
        return "ImagePullBackOff -> can't fetch the image (wrong tag / registry / credentials, Ch 13) -> fix the image reference or registry access."
    if status == "CrashLoopBackOff":
        if last_reason == "OOMKilled" or exit_code == 137:
            return "CrashLoopBackOff + OOMKilled (exit 137) -> exceeded its MEMORY LIMIT (Ch 11) -> raise the limit or fix the leak. Check `top`."
        if "migration" in logs.lower() or "liquibase" in logs.lower():
            return "CrashLoopBackOff -> startup migration failed (Part 2, fail-fast, Ch 8) -> fix the changeset; old version keeps serving (Ch 14)."
        return "CrashLoopBackOff -> failing at startup -> `logs --previous` for the exception; check config/secrets (Ch 9)."
    if status == "Running" and "readiness" in events.lower():
        return "Running but not Ready -> readiness probe failing (Ch 10) -> Service sends no traffic; check the dependency or /api/health."
    return "Running -> healthy."

# A few real-world symptom sets:
print(diagnose("CrashLoopBackOff", last_reason="OOMKilled", exit_code=137))
print(diagnose("Pending"))
print(diagnose("ImagePullBackOff"))
print(diagnose("CrashLoopBackOff", logs="Liquibase: changeset 5 failed"))
print(diagnose("Running", events="Readiness probe failed: connection refused"))
```

**Now break it.** Add a new symptom — say a Pod stuck `Terminating` for a long time (usually a finalizer or a slow graceful shutdown / `terminationGracePeriod`) — and extend `diagnose` to recognize it. The exercise is the real skill: building, in your head, a *map from symptom to cause to fix*, where the "cause" is almost always a concept you've now learned. That map is what makes an experienced operator fast: they see `OOMKilled` and *immediately* think "memory limit, Chapter 11," not "hmm, let me start guessing." Observability isn't a separate body of knowledge — it's the *recall path* back to everything this Part taught, triggered by the cluster's status words.

## 6. Trade-offs & Complexity

This is the practical-skills capstone, so the "trade-off" is about *depth of observability*. The four `kubectl` commands here are the *floor* — enough to diagnose most everyday issues — but production observability goes deeper: **centralized logging** (so you can search logs across all Pods and across restarts, since a Pod's local logs vanish when it's deleted), **metrics** (Prometheus + Grafana, for trends and alerting beyond a `top` snapshot), and **distributed tracing** (following one request across services). Those add real infrastructure and cost, and a homelab-scale Cortex may rely mostly on `kubectl` + basic logging, while a large system invests heavily in the full stack. The principle is constant regardless of scale: **a running system should be *interrogable*** — you should always be able to *ask it* what it's doing and get a truthful answer. `kubectl describe` and `logs` are where that habit starts; the bigger tools are the same instinct, industrialized.

## 7. Edge Cases & Failure Modes

- **Logs vanish with the Pod.** A deleted/replaced Pod's local logs are *gone* — which is why production ships logs to a *central* store. For a crashed-but-not-yet-replaced container, `logs --previous` is your window before it disappears.
- **`describe` events expire.** Kubernetes events are retained for a limited time (often an hour). Diagnose promptly, or capture events; for history, you need persistent logging/monitoring.
- **`top` needs metrics-server.** `kubectl top` requires the metrics-server component installed. If it's missing, `top` fails — use a monitoring stack instead.
- **Looking at the wrong layer.** A Service returning errors might be a *Pod* problem (Pods unready), an *Ingress* problem (routing), or a *dependency* problem (the database). Work the layers (Ingress → Service → Pod → app logs) rather than fixating on one.

## 8. Practice

> **Exercise 1 — Match symptom to cause.** For each status, name the likely cause and the chapter: (a) `OOMKilled`; (b) `Pending`; (c) `ImagePullBackOff`; (d) `Running` but `0/1` Ready; (e) `CrashLoopBackOff` with "migration failed" in the logs.

<details>
<summary><strong>Answer</strong></summary>

Each status word names a *kind* of problem that points straight at a concept from this Part — that recall path is the whole skill (§3):

- **(a) `OOMKilled` → memory *limit* exceeded (Chapter 11).** The container crossed its memory limit and the kernel killed it (exit 137). Fix: raise the limit or fix the leak. *Not* an application exception.
- **(b) `Pending` → the scheduler can't place it (Chapter 11/5).** Usually *no node has room* for its resource *requests* (or a volume/affinity can't be satisfied). Fix: add capacity or lower the request.
- **(c) `ImagePullBackOff` → can't fetch the image (Chapter 13).** Wrong tag, registry down, or bad credentials. The manifest is fine; the *image fetch* failed. Fix: correct the image reference or registry access.
- **(d) `Running` but `0/1` Ready → readiness probe failing (Chapter 10).** The container is up but `/api/health` isn't passing, so the Service sends it *no traffic*. Fix: check the dependency or the health endpoint.
- **(e) `CrashLoopBackOff` + "migration failed" → fail-fast startup migration (Part 2, Ch 8).** The boot crashes on a bad changeset (by design). Fix the changeset; meanwhile the old version keeps serving and the rollout stalls (Chapter 14).

The point isn't memorizing statuses — it's that each word is a *recall trigger* back to a concept you already learned, which is what lets an experienced operator skip the guessing.

</details>

> **Exercise 2 — Diagnose in order.** A Cortex Pod is `CrashLoopBackOff`. Write the sequence of `kubectl` commands you'd run, and what each would tell you.

<details>
<summary><strong>Answer</strong></summary>

Run them in the order that *narrows the diagnosis* — the doctor's examination from §2: glance → chart → patient's words → live monitor.

1. **`kubectl get pods`** — the vital-signs glance. Confirms the status (`CrashLoopBackOff`) and shows the **restart count** and age — how bad, how long.
2. **`kubectl describe pod <name>`** — the chart. Shows the **Events** and **Last State**: the *reason* it died (e.g. `OOMKilled` + exit `137` → memory limit, Chapter 11; or `BackOff restarting failed container`), probe failures, scheduling info. This usually names the *kind* of problem.
3. **`kubectl logs <name> --previous`** — the patient's own words. `--previous` shows the *crashed* container's final output (the live container is too young to have logs). This is where an application exception or `Liquibase: changeset N failed` (Part 2 fail-fast) shows up — *or* the absence of any error (confirming an external kill, not a code bug).
4. **`kubectl top pod <name>`** — the live monitor. Shows current CPU/memory vs the limits (Chapter 11) — e.g. pinned at `~1Gi` confirms a memory problem.

The ordering matters: each command answers a *different* question and rules out a layer, so you reach "here's exactly which concept failed, and the fix" by *looking*, not guessing. (If `describe` showed `OOMKilled`, you're already done at step 2–4: memory limit, raise it or fix the leak.)

</details>

> **Exercise 3 — Why central logging?** Explain why a production cluster ships logs to a central store rather than relying on `kubectl logs`, referencing what happens to a Pod's logs when it's replaced.

<details>
<summary><strong>Answer</strong></summary>

Because **a Pod's logs are local and ephemeral — they vanish when the Pod is deleted or replaced** (§7). And in this Part's whole model, Pods are *constantly* replaced: a crash triggers self-healing (Chapter 6), a rolling update retires old Pods (Chapter 14), an OOMKill restarts the container (Chapter 11). The moment a Pod is gone, `kubectl logs` for it returns *nothing* — the evidence is destroyed exactly when you most want it (right after a failure, to investigate).

`kubectl logs --previous` is only a *narrow* window: it shows the *last* crashed container of a *still-existing* Pod — useless once the Pod itself is deleted, and it keeps only one prior generation. Likewise `kubectl describe` events *expire* after about an hour. So relying on `kubectl` alone means your diagnostic history evaporates.

**Central logging** (ship every Pod's stdout/stderr to a store like Loki/Elasticsearch as it's produced) fixes this: logs *outlive* the Pods that wrote them, are *searchable across all Pods and all restarts*, and survive the very replacements that erase local logs. The underlying principle (§6) holds at every scale: *a running system should be interrogable* — you should always be able to ask it what happened and get a truthful answer, even after the Pod that knew is gone. `kubectl logs` is where that habit starts; central logging is the same instinct, made durable.

</details>

```quiz
{
  "prompt": "A Pod shows `CrashLoopBackOff`, and `kubectl describe pod` shows its last state was `OOMKilled` with exit code 137. What does this tell you?",
  "input": "Choose one:",
  "options": [
    "The container exceeded its memory LIMIT and was killed by the kernel (Chapter 11) — raise the limit or fix the memory usage; it's not an application exception",
    "The image failed to download",
    "The readiness probe is misconfigured",
    "The node ran out of disk space"
  ],
  "answer": "The container exceeded its memory LIMIT and was killed by the kernel (Chapter 11) — raise the limit or fix the memory usage; it's not an application exception"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Kubernetes — kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)** & **[Debugging running Pods](https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/)** — the diagnostic commands above.
- **[Kubernetes — Application introspection & debugging](https://kubernetes.io/docs/tasks/debug/debug-application/)** — reading status, events, and logs systematically.
- **[Homelab from Scratch — Quick health check](/cortex/homelab-from-scratch/operate-and-recover/quick-health-check)** — observing the actual cluster Cortex runs on.

---

## You've finished Part 4 — and the whole book 🎉🎉

You started Part 4 unsure what a container even was. You can now read Cortex's real `Dockerfile`, `docker-compose.yml`, and Kubernetes manifests and explain every line: a multi-stage image (Ch 3), a Deployment self-healing toward desired state (Ch 5–6), a Service and Ingress exposing it over HTTPS (Ch 7–8), config and secrets injected from outside (Ch 9), probes gating traffic and restarts (Ch 10), resources bounding it (Ch 11), a security context hardening it (Ch 12), GitOps shipping it (Ch 13), rolling updates deploying it without downtime (Ch 14), and the commands to look inside when it breaks (Ch 15). You can take an app from your laptop to `cortex.kakde.eu`.

## The arc, complete

This book had one spine — the life of a real backend service — and you've now walked all of it through the very system serving these pages:

- **[Part 1 — ZIO](/cortex/production-engineering/zio):** you **wrote** it — effects as values, ZLayer, typed errors, fibers.
- **[Part 2 — Liquibase](/cortex/production-engineering/liquibase):** you gave its data a **safe way to evolve** — migrations, idempotent and fail-fast.
- **[Part 3 — Keycloak & OAuth 2.0](/cortex/production-engineering/iam-keycloak-oauth):** you **secured** it — OAuth, OIDC, PKCE, and real token verification.
- **[Part 4 — Docker & Kubernetes](/cortex/production-engineering/docker-kubernetes):** you **shipped** it — containers, orchestration, and zero-downtime production.

Write it, evolve it, secure it, ship it. That's production engineering — and you didn't learn it from toy examples, but from one real system you can now read, run, extend, and operate end to end. Go build something, and run it like you mean it.
