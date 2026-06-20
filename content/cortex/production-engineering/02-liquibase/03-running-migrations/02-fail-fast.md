---
title: '8. The fail-fast philosophy'
summary: If a migration fails at startup, Cortex crashes — on purpose. A dead pod is better than one serving against a broken, half-migrated schema. Learn why failing loud and early beats limping on.
---

# 8. The fail-fast philosophy

## TL;DR

> When a startup migration fails, the *worst* thing an app can do is shrug and start serving anyway against a broken or half-applied schema — every request now corrupts data or returns 500s, silently. The *right* thing is **fail-fast**: crash the boot, loudly, with the error. In Cortex, a failed `Migrations.run` makes the whole `run` effect fail (it short-circuits `*> serve`), the process exits non-zero, and — in production — Kubernetes sees a crashing pod, **doesn't route traffic to it**, restarts it, and alerts. A dead pod is a *visible, contained* problem; a pod serving on a broken schema is an *invisible, spreading* one.

## 1. Motivation

Failure is inevitable; what matters is *how* you fail. Imagine a migration that fails halfway — changeset 4 applied, changeset 5 errored. Two philosophies diverge sharply. **Fail-slow ("limp on"):** catch the error, log a warning, start the server anyway. Now the app is serving against a schema that's *partially* migrated — some code paths work, others hit missing columns and 500 (or worse, write inconsistent data). The damage is silent, spreading, and maddening to debug, because the app *looks* up. **Fail-fast:** let the error crash the boot. The app never serves a single request on the broken schema. The failure is loud, immediate, and *contained to startup* — nobody's data got corrupted, and you have a clean stack trace pointing at changeset 5.

Fail-fast feels scarier ("the app won't start!") but is far safer, because a system that *stops* on a problem it can't safely handle is protecting you. This is the same instinct as ZIO's defects (Part 1, Chapter 4: let bugs crash, don't swallow them) and the same instinct that makes a fuse blow rather than let your house wiring melt. A startup migration is precisely the place to apply it: better to not start than to start broken.

## 2. Intuition (Analogy)

A **circuit breaker** in your home. When something's wrong — a short, an overload — the breaker *trips and cuts the power*. That's annoying (the lights go out!) but it's *saving your house from burning down*. The alternative — a system that kept the power flowing through a fault "so you're not inconvenienced" — is how electrical fires start. The breaker fails *loud and safe*: you immediately know something's wrong, and the damage is prevented, not hidden.

A failed migration should trip the breaker. Crashing the boot is the lights going out — visibly, immediately. Limping on with a broken schema is letting current flow through the fault: the app stays "on," but every request is feeding a fire of 500s and corrupt writes. You *want* the breaker to trip. A dead, restarting, *alerting* pod is the breaker doing its job.

## 3. Formal Definition

- **Fail-fast.** On an unrecoverable startup condition (a failed migration, missing required config, an unreachable database), *halt immediately* with a clear error, rather than continuing in a degraded or undefined state.
- **In ZIO.** `Migrations.run` has error type `Throwable`. If it fails, `Migrations.run *> serve` short-circuits (Part 1, Chapter 5) — `serve` never runs — and the top-level `run` effect *fails*. `ZIOAppDefault` translates that into a **non-zero process exit** (Part 1, Chapter 3). The server simply never starts.
- **Changeset atomicity.** On databases that support transactional DDL (PostgreSQL does), Liquibase runs each changeset in a **transaction**, so a changeset that errors mid-way **rolls back atomically** — you don't get "half a changeset" applied. The database is left at a clean changeset boundary, and `DATABASECHANGELOG` records exactly what succeeded.
- **In production (Kubernetes).** A non-zero exit means the container *crashes*. The orchestrator: (a) does **not** send traffic to a pod that isn't ready (readiness probe fails / pod isn't Running), (b) **restarts** it (and on repeated failure, `CrashLoopBackOff` — a loud, monitorable signal), and (c) during a rolling deploy, **keeps the old, healthy version running** because the new one never became ready. (Part 4 covers probes and rollouts.)

> Fail-fast = stop loudly on an unrecoverable startup problem instead of serving broken. ZIO short-circuits and exits non-zero; Postgres makes each changeset atomic; Kubernetes withholds traffic, restarts, and alerts. The bad version never serves a request.

## 4. Worked Example — what fail-fast buys you in production

Trace a bad deploy with and without fail-fast:

```text
A buggy changeset (cortex:5) ships in a new image. It errors on apply.

WITHOUT fail-fast (limp on):
  pod boots -> migration 5 fails -> caught + logged -> server starts anyway
  -> /api/submissions hits the half-migrated schema -> 500s and/or corrupt writes
  -> the pod is "Ready", so Kubernetes ROUTES TRAFFIC to it
  -> users hit errors; the rollout "succeeded"; nobody noticed until reports pile up

WITH fail-fast (Cortex):
  pod boots -> migration 5 fails -> Migrations.run FAILS -> serve never runs -> exit 1
  -> the pod crashes, never becomes Ready -> Kubernetes sends it NO traffic
  -> the rolling deploy STALLS: the old healthy version keeps serving
  -> CrashLoopBackOff fires an alert -> you see a clean stack trace at changeset 5
  -> zero user impact; you fix changeset 5 and redeploy
```

The fail-fast column is strictly better *in production*: because the new pod never becomes ready, Kubernetes' rolling update never shifts traffic to it, so **the broken version is never user-visible** — the old version just keeps running while you fix the migration. That synergy between *fail-fast-on-bad-migration* (this chapter) and *don't-route-to-unready-pods* (Part 4) is what turns a potentially catastrophic schema bug into a non-event with an alert. Cortex gets this for free precisely because `Migrations.run` fails the effect and Postgres rolls the bad changeset back cleanly.

## 5. Build It

Run this. It contrasts limp-on vs fail-fast, and shows why the dead-but-honest path is the safe one.

```python run
class BadSchema(Exception): pass

def run_migrations(changesets):
    applied = []
    for cs in changesets:
        if cs == "5-buggy":
            # Postgres rolls the failed changeset back atomically; we surface the error.
            raise BadSchema(f"changeset {cs} failed (rolled back); schema at {applied[-1] if applied else 'empty'}")
        applied.append(cs)
    return applied

CHANGESETS = ["1-visits", "2-allowlist", "3-submissions", "5-buggy"]

# ❌ LIMP ON: catch the error, serve anyway.
print("== limp-on ==")
try:
    run_migrations(CHANGESETS)
except BadSchema as e:
    print(f"   migration failed: {e}")
    print("   ...starting server anyway")          # the mistake
    print("   [server] serving requests on a BROKEN schema -> 500s / corruption (and Ready=true!)\n")

# ✓ FAIL-FAST: let it crash the boot. The server never starts.
print("== fail-fast (Cortex) ==")
def boot_fail_fast():
    run_migrations(CHANGESETS)                       # raises -> not caught
    print("   [server] serving")                     # never reached
try:
    boot_fail_fast()
except BadSchema as e:
    print(f"   BOOT ABORTED: {e}")
    print("   process exits non-zero -> pod crashes -> NO traffic routed -> alert fires")
    print("   the old healthy version keeps serving; zero user impact")
```

**Now break it.** Try to make limp-on "safe" by adding more error handling after the catch — you'll find there's no amount of post-failure cleanup that makes serving on a half-migrated schema acceptable, because you don't *know* what state the schema is in or which requests will corrupt data. The only safe response to "I couldn't reach a known-good schema" is "don't serve." That's why fail-fast isn't pessimism — it's the *only* honest option when the precondition for serving correctly hasn't been met.

## 6. Trade-offs & Complexity

| Fail-fast (Cortex) | Limp on / degrade |
|---|---|
| Broken version never serves a request | Serves on a broken schema |
| Loud, contained, with a clean error | Silent, spreading, hard to debug |
| K8s withholds traffic + alerts | "Ready" pod gets traffic |
| Old version keeps serving during a bad deploy | Bad version replaces good one |
| "The app won't start" can look alarming | "It started" hides the real problem |

The cost is the *appearance* of fragility: a fail-fast app refuses to start on a bad migration, which can feel worse than one that "at least came up." But that feeling is exactly backwards — the one that came up is the dangerous one. The real prerequisite for fail-fast to be *safe* is the surrounding machinery: health probes so the orchestrator knows the pod isn't ready, and alerting so a `CrashLoopBackOff` reaches a human. Cortex (with Part 4's probes) has that, so fail-fast is purely upside.

## 7. Edge Cases & Failure Modes

- **Catch-all that hides the failure.** A broad `catchAll` around `Migrations.run` that logs and continues *defeats* fail-fast (and is the ZIO defect-swallowing anti-pattern, Part 1 Chapter 4). Let migration failures propagate.
- **No health probe.** Fail-fast relies on the orchestrator not routing to an unready pod. Without a readiness probe (Part 4), a crash loop might still flap traffic. Pair fail-fast with probes.
- **Non-transactional DDL.** Some databases (and some statements) don't support transactional DDL, so a failed changeset can leave a *partial* change. Postgres mostly does; know your database, and keep changesets small so a partial failure is easy to reason about.
- **Crash loop with no alert.** A pod crash-looping silently is fail-fast without the "tell a human" half. Ensure `CrashLoopBackOff` (or equivalent) pages someone.

## 8. Practice

> **Exercise 1 — Two outcomes.** For a changeset that fails at boot, list what happens to (a) the schema, (b) the server process, (c) production traffic — under fail-fast.

<details>
<summary><strong>Answer</strong></summary>

Under fail-fast (§3):

- **(a) The schema** stays at a **clean changeset boundary.** On Postgres (transactional DDL), the failing changeset runs in a transaction and **rolls back atomically** — you never get "half a changeset" applied — and `DATABASECHANGELOG` records exactly what succeeded *before* it.
- **(b) The server process** never starts and then **exits non-zero.** `Migrations.run` (error type `Throwable`) fails, which short-circuits `Migrations.run *> serve` so `serve` never runs; `ZIOAppDefault` turns the failed `run` effect into a non-zero process exit.
- **(c) Production traffic** is **not routed to the broken pod.** The pod crashes and never becomes Ready, so Kubernetes sends it zero traffic; during a rolling deploy the rollout stalls and the **old healthy version keeps serving**, and a repeated crash (`CrashLoopBackOff`) fires an alert.

Net effect: the bad version never serves a single request.

</details>

> **Exercise 2 — Why a dead pod is better.** In two sentences, argue why a crashing, no-traffic pod is *safer* than a "Ready" pod serving on a half-migrated schema.

<details>
<summary><strong>Answer</strong></summary>

A crashing pod is a *visible, contained* problem — Kubernetes withholds traffic from it, the old healthy version keeps serving, and `CrashLoopBackOff` alerts a human, so the blast radius is zero users and you get a clean stack trace at the failing changeset. A "Ready" pod on a half-migrated schema is an *invisible, spreading* one — because it reports Ready, Kubernetes routes real traffic to it, and every request either 500s or silently corrupts data while the deploy looks like it "succeeded," so the damage grows undetected until reports pile up.

</details>

> **Exercise 3 — Make fail-fast safe.** Fail-fast alone isn't enough. Name the two surrounding mechanisms (one in the orchestrator, one in ops) that turn a fail-fast crash into a contained, fixable incident.

<details>
<summary><strong>Answer</strong></summary>

- **In the orchestrator: health probes** (a readiness/startup probe). Fail-fast is only *safe* because the orchestrator knows the crashed pod isn't Ready and therefore **doesn't route traffic to it** (and keeps the old version serving during a rollout). Without a probe, a flapping pod could still receive traffic.
- **In ops: alerting** on the crash (e.g. paging on `CrashLoopBackOff`). A crash that no human ever hears about is fail-fast missing its "tell someone" half — the failure is contained but never *fixed*.

Together they convert a raw crash into a contained, fixable incident: the broken version is held back from users (probe) *and* someone is told to go fix it (alert). Crashing alone protects data; these two make it actionable.

</details>

```quiz
{
  "prompt": "Why does Cortex crash the boot (rather than serve) when a startup migration fails?",
  "input": "Choose one:",
  "options": [
    "Serving on a broken/half-migrated schema silently corrupts data and 500s; crashing keeps the broken version from getting traffic (K8s withholds it), preserves the old healthy version, and surfaces a clear error",
    "Because Liquibase deletes the database on failure",
    "To force developers to run migrations manually",
    "Crashing is faster than serving"
  ],
  "answer": "Serving on a broken/half-migrated schema silently corrupts data and 500s; crashing keeps the broken version from getting traffic (K8s withholds it), preserves the old healthy version, and surfaces a clear error"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Liquibase — Transaction handling / DDL in transactions](https://docs.liquibase.com/concepts/changelogs/changeset.html)** — how changesets are wrapped in transactions (on databases that support transactional DDL like Postgres).
- **[Kubernetes — Pod lifecycle & restart policy](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)** — `CrashLoopBackOff` and why an unready pod gets no traffic (Part 4 goes deeper).
- **[Cortex `Main.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/Main.scala)** — `Migrations.run *> serve`: a failed migration fails the whole `run`, so the server never starts.

---

**Next:** the basics are solid. Now the advanced toolkit — starting with running *different* changesets in *different* environments using contexts and labels. → [9. Contexts and labels](/cortex/production-engineering/liquibase/advanced/contexts-and-labels)
