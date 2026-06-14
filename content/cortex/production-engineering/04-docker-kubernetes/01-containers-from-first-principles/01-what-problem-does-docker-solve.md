---
title: '1. What problem does Docker actually solve?'
summary: '"It works on my machine" is a deployment disaster. A container packages the whole environment — app, libraries, runtime, exact versions — into one immutable artifact that runs identically everywhere. Ship the machine, not just the code.'
---

# 1. What problem does Docker actually solve?

## TL;DR

> Software depends on its *environment* — the OS libraries, the runtime version, dozens of system packages — and that environment differs between your laptop, a teammate's, CI, and production. "Works on my machine" is the symptom; *environment drift* is the disease. A **container** cures it by packaging the application *together with its entire environment* into one **immutable image** that runs **identically** anywhere a container runtime exists. You stop shipping *code and hoping the target has the right environment*; you ship the *environment too*. Containers do this far more cheaply than VMs by **sharing the host's kernel**.

## 1. Motivation

You write code, it runs perfectly, you hand it to a colleague — and it explodes. Wrong Java version. A missing system library. A different OpenSSL. Python 3.11 vs 3.9. The code didn't change; the *environment* did. Multiply this across every developer machine, every CI runner, and every production server, and you get a permanent, exhausting class of bugs that have nothing to do with your logic and everything to do with *where the code runs*.

The root cause: an application is not self-contained. It assumes a runtime, system libraries, environment variables, file paths — an entire *context* — and that context is implicit and different everywhere. The traditional "fix" (documentation: "install Java 21, then libpq, then...") is fragile and forever out of date. The real fix is to stop *assuming* the environment and start *shipping* it. If the app travels *with* the exact runtime and libraries it needs, sealed together, then "where it runs" stops mattering — the box brings its own world. That sealed box is a container, and eliminating environment drift is the problem Docker solves. Everything else containers enable (orchestration, scaling, the whole of Kubernetes) is built on that one foundational guarantee: *the artifact runs the same everywhere.*

## 2. Intuition (Analogy)

Before the 1950s, shipping cargo was chaos: every item — barrels, sacks, crates of different shapes — was loaded individually, by hand, and what worked on one dock, truck, or ship didn't fit the next. Then came the **intermodal shipping container**: a standardized steel box. Suddenly it didn't matter *what* was inside — coffee, cars, electronics — because the *box* was standard. Any crane, any ship, any truck could handle any container identically. Global trade exploded, because the *interface* was standardized even though the *contents* varied wildly.

Software containers are the same idea. Your app's contents vary infinitely (a ZIO server, a Python script, a database), but the *container* is a standard box that any container runtime can run identically. The host doesn't need to know or care what's inside or what it depends on — the box is sealed and self-sufficient. Standardize the box, not the cargo, and "it works here but not there" disappears, because *here* and *there* both just run boxes.

## 3. Formal Definition

- **Container.** A running instance of an **image**: an isolated process (or group) on a host, with its *own* filesystem, network, and process namespace, but **sharing the host's kernel**. It sees its own libraries and files (from the image), not the host's.
- **Image.** An immutable, read-only template containing the application *plus everything it needs to run*: a base OS userland, libraries, the runtime, the app, and metadata (what command to run, what port). Built once, run anywhere.
- **Container runtime.** The software that runs containers (Docker Engine, containerd, etc.), using Linux kernel features — **namespaces** (isolation: each container gets its own view of processes, network, filesystem) and **cgroups** (limits: bound CPU/memory) — to isolate and constrain them.
- **Container vs VM.** A **virtual machine** virtualizes *hardware* and runs a *full guest OS* (its own kernel) — heavy (gigabytes, slow boot). A **container** virtualizes the *OS* and *shares the host kernel* — light (megabytes, sub-second start). Containers give you most of a VM's isolation at a tiny fraction of the cost.
- **Immutability.** An image doesn't change once built. You don't patch a running container; you build a *new image* and replace the container. This is what makes "identical everywhere" and easy rollback possible.

> A container ships the app *with its whole environment* as an immutable image that runs identically anywhere, isolated by kernel namespaces and bounded by cgroups — far lighter than a VM because it shares the host kernel. The guarantee: build once, run the same everywhere.

## 4. Worked Example — the same Cortex, everywhere

Cortex is a perfect case. Its server needs a *specific* environment: a Java 21 runtime, specific native libraries, the compiled application, the content files, particular environment variables. Without containers, running Cortex anywhere means reproducing all of that by hand — and getting it subtly wrong somewhere.

```d2
direction: right

code: "Cortex source\n(Scala, content)" {shape: rectangle}
image: "Cortex IMAGE\n(JRE 21 + app + content + config)" {shape: package}
code -> image: "docker build"

laptop: "Your laptop" {shape: rectangle}
ci: "CI runner" {shape: rectangle}
prod: "Production (K8s)" {shape: rectangle}

image -> laptop: "docker run → identical"
image -> ci: "docker run → identical"
image -> prod: "docker run → identical"
```

`docker build` turns Cortex's source into *one image* — `ghcr.io/ani2fun/cortex:latest` — that bundles the JRE, the app, the content, and the config defaults. That *same image* runs on your laptop, in CI, and in production (Part 4's whole back half is about that production run). Because the image carries Java 21 *inside it*, it doesn't matter whether your laptop has Java 17, Java 21, or no Java at all — the container brings its own. "Works on my machine" becomes "works as the image," and the image is the same artifact everywhere. When Cortex deploys, Kubernetes pulls *that exact image* and runs it — the bytes that ran in CI are the bytes that serve `cortex.kakde.eu`.

## 5. Build It

Run this. It models the difference: code that depends on the *host* environment (drifts) versus an image that *carries* its environment (identical everywhere).

```python run
# --- WITHOUT containers: the app depends on whatever the HOST provides ---
def run_on_host(app, host_env):
    needed = app["requires"]
    missing = {k: v for k, v in needed.items() if host_env.get(k) != v}
    return "OK" if not missing else f"BROKEN — host has wrong/missing {missing}"

cortex = {"requires": {"java": "21", "libpq": "15"}}

laptop = {"java": "21", "libpq": "15"}
ci     = {"java": "17"}                       # wrong java, no libpq
prod   = {"java": "21", "libpq": "14"}        # wrong libpq

print("== without containers (depends on host env) ==")
print("  laptop:", run_on_host(cortex, laptop))
print("  ci    :", run_on_host(cortex, ci))
print("  prod  :", run_on_host(cortex, prod), "\n")

# --- WITH a container: the IMAGE carries the environment; the host is irrelevant ---
def build_image(app):
    return {"app": app, "bundled_env": dict(app["requires"])}   # environment sealed INSIDE

def run_image(image, host_env):
    # The container uses its OWN bundled env, not the host's.
    return "OK (identical everywhere)"

image = build_image(cortex)
print("== with a container image (carries its env) ==")
for name, host in (("laptop", laptop), ("ci", ci), ("prod", prod)):
    print(f"  {name:6}:", run_image(image, host))
```

**Now break it.** In the no-container section, try to make all three hosts work — you'd have to manually fix `java` and `libpq` on each, forever, as versions drift. In the container section, the host environments are *ignored* entirely, because the image carries its own. That's the whole point: containers don't make you *match* environments across machines; they make the machine's environment *irrelevant*. The artifact is self-sufficient, so identical-everywhere is the default, not a heroic ongoing effort.

## 6. Trade-offs & Complexity

| Containers | "Just install it on the server" |
|---|---|
| Identical environment everywhere | Drift, "works on my machine" |
| Immutable artifact, easy rollback | Mutable servers, snowflake state |
| Lightweight (shares host kernel) | (VMs: heavy; bare metal: fragile) |
| Foundation for orchestration (K8s) | Hard to scale/schedule |
| A build step + runtime to learn | "Simpler" until it drifts |

The cost is a new layer in your workflow: you write a `Dockerfile`, build images, run a container runtime, and learn its concepts. For a one-off script on one machine, that's overhead. But the moment your software runs in *more than one place* — and "my laptop + CI + production" is the minimum for anything real — containers eliminate an entire category of environment bugs and unlock everything that follows (compose, orchestration, scaling). The complexity is front-loaded and pays back continuously.

## 7. Edge Cases & Failure Modes

- **Treating containers as VMs.** A container is *one main process*, immutable and disposable — not a little server you SSH into and tweak. Patch by rebuilding the image, not by editing a running container (changes vanish on restart).
- **"It works in the container" but the image is wrong.** If the image doesn't bundle a needed library, it'll fail *everywhere consistently* — which is still better than failing *inconsistently*, and points you straight at the `Dockerfile`.
- **Huge images.** Naively bundling a full OS makes gigabyte images that are slow to pull and ship. Minimal base images and multi-stage builds (Chapter 3) keep them small.
- **Kernel-version assumptions.** Containers share the host *kernel*, so a container needing a very new kernel feature won't get it from an old host. Usually a non-issue, but it's the one thing the host kernel still dictates.

## 8. Practice

> **Exercise 1 — Name the disease.** A feature works on a developer's laptop but 500s in production with a "library not found" error. The code is identical. Name the root cause and how a container would prevent it.

<details>
<summary><strong>Answer</strong></summary>

The root cause is **environment drift** (§1): the code is identical, but the *environment* isn't. The laptop has the library installed; production doesn't (or has a different version). Because the application *assumes* a context — system libraries, runtime, paths — that is implicit and different everywhere, "works on my machine" is guaranteed eventually.

A container prevents it by **shipping the environment with the code** as one immutable image. The needed library is bundled *inside* the image, so the artifact carries its own world and "where it runs" stops mattering — the laptop image and the production image are the same bytes. Note the subtler win: if the library were *still* missing from the image, it would fail *consistently everywhere*, not mysteriously in only one place — which already points you straight at the `Dockerfile`. Containers don't make you match environments across machines; they make the machine's environment irrelevant.

</details>

> **Exercise 2 — Container or VM?** For each, choose and justify: (a) running 50 lightweight microservices on one host; (b) running an old app that needs a *different kernel*; (c) a sub-second-startup function. Why do containers win (a) and (c)?

<details>
<summary><strong>Answer</strong></summary>

The deciding question is always: do you need a *different kernel*, or just a different *userland* (libraries, runtime)? A VM virtualizes hardware and runs a full guest OS with its own kernel (heavy, gigabytes, slow boot); a container shares the host kernel and virtualizes only the OS userland (light, megabytes, sub-second start) — §3.

- **(a) 50 microservices on one host → containers.** Fifty full guest OSes would be gigabytes each and crush the host; fifty containers *share the one host kernel*, so each is just its own process + filesystem, costing megabytes. Containers win because sharing the kernel is exactly what makes density cheap.
- **(b) old app needing a *different kernel* → VM.** This is the one case a container *can't* serve: containers share the host kernel, so an app requiring a different kernel must get its own — which only a VM provides. The kernel is the one thing the host still dictates (§7).
- **(c) sub-second-startup function → containers.** Starting a container adds only a thin writable layer on top of the read-only image — nothing is copied or booted upfront — so start is sub-second. A VM must boot a whole OS first, far too slow. Containers win on startup latency for the same reason: no guest kernel to boot.

</details>

> **Exercise 3 — What's in the box?** List everything Cortex's image must bundle for the server to run *without depending on the host*. (Hint: runtime, app, content, config.)

<details>
<summary><strong>Answer</strong></summary>

The whole point of an image is *self-sufficiency* (§4): if the host is to be irrelevant, every dependency travels *inside* the box. For Cortex that means:

- **A base OS userland** — the minimal system libraries the runtime links against (an image starts `FROM` a base, §3).
- **The Java 21 runtime (JRE)** — bundled *inside*, so it doesn't matter whether the host has Java 17, Java 21, or no Java at all; the container brings its own (§4).
- **The compiled application** — Cortex's server artifact (the staged output).
- **The content files** — the markdown chapters the server renders.
- **Configuration defaults** — the `ENV` values (port, content root, etc.) baked into the image so it has sane defaults (later *overridden* in production by Kubernetes).
- **Metadata** — the command to run and the port to expose.

The test for "did I bundle enough?": could this image run on a host with *nothing* but a container runtime installed? If anything is still assumed from the host (a library, a JRE), it isn't truly in the box, and drift creeps back in.

</details>

```quiz
{
  "prompt": "What core problem does a container image solve?",
  "input": "Choose one:",
  "options": [
    "It packages the app together with its entire environment (runtime, libraries, exact versions) into one immutable artifact that runs identically everywhere — eliminating 'works on my machine' environment drift",
    "It makes the application run faster",
    "It encrypts the source code",
    "It replaces the need for a programming language"
  ],
  "answer": "It packages the app together with its entire environment (runtime, libraries, exact versions) into one immutable artifact that runs identically everywhere — eliminating 'works on my machine' environment drift"
}
```

## In the Wild

- **[Docker — Why Docker? / Getting started](https://docs.docker.com/get-started/docker-overview/)** — the official framing of the environment-drift problem and the container model.
- **[Julia Evans — "What even is a container?"](https://jvns.ca/blog/2016/10/10/what-even-is-a-container/)** — a delightful, deep explanation of namespaces and cgroups, the kernel features under the hood.
- **[Cortex `Dockerfile`](https://github.com/ani2fun/cortex/blob/main/Dockerfile)** — the recipe that turns Cortex's source into the run-anywhere image (you'll read it in Chapter 3).

---

**Next:** an image isn't a monolith — it's a *stack of layers*, which is why builds are fast and images stay small. Let's open one up. → [2. Images, layers & the union filesystem](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/images-layers-union-fs)
