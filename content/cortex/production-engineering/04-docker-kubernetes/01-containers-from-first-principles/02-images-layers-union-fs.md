---
title: '2. Images, layers & the union filesystem'
summary: An image isn''t a monolithic blob — it''s a stack of read-only layers, one per build step, combined by a union filesystem. That design is why rebuilds are fast, images are small, and a container starts instantly.
---

# 2. Images, layers & the union filesystem

## TL;DR

> A Docker image is a **stack of read-only layers**, each produced by one build instruction. A **union filesystem** merges them into a single view, so a container sees one coherent filesystem assembled from many layers. Two consequences make Docker practical: layers are **cached** (rebuilds re-run only the steps whose inputs changed) and **shared** (ten images on the same base store that base *once*). A running container adds a thin **writable layer** on top, so starting one is instant — no copying the whole image. Understanding layers is the key to fast builds and small images.

## 1. Motivation

If an image bundled an entire environment as one giant blob, Docker would be impractical: every tiny code change would mean rebuilding and re-shipping gigabytes, and every image would redundantly store its own copy of the same base OS. The reason Docker is *fast* and *space-efficient* — the reason a rebuild after a one-line change takes seconds, not minutes, and a new image pull downloads only what's new — is its **layered** design. An image is built up in slices, each slice cached independently, each shareable across images.

This isn't an implementation detail you can ignore; it's the single biggest lever on your build speed and image size, and it directly shapes how you *write* a Dockerfile (Chapter 3). Order your build steps wrong and every code change busts the cache for your slow dependency-download step, making every build glacial. Order them right — exploiting layer caching deliberately — and builds fly. So understanding layers isn't optional Docker trivia; it's the difference between a 10-second rebuild and a 10-minute one.

## 2. Intuition (Analogy)

A stack of **transparent acetate sheets** (the kind used on old overhead projectors). Each sheet has *some* of the picture drawn on it; stacked and projected, they *combine* into one complete image. To change one element, you swap *one sheet* — not redraw the whole picture. And if ten different presentations all start from the same "company logo background" sheet, you store that sheet *once* and reuse it under each.

Image layers are those sheets. Each build step draws one sheet (adds/changes some files). The **union filesystem** is the projector merging them into one visible picture. Change your app code and you redraw only the top sheet — the heavy "install the JRE" sheet underneath is untouched and reused from cache. And every image built `FROM` the same base shares that base sheet, stored once. Swap one sheet, keep the rest: that's why layered images are fast to rebuild and cheap to store.

## 3. Formal Definition

- **Layer.** A read-only set of filesystem changes (files added, modified, deleted) produced by **one image-building instruction**. Layers are stacked in order; each is content-addressed by a hash of its contents.
- **Union (overlay) filesystem.** A mechanism (e.g. OverlayFS) that merges multiple read-only layers into a single unified directory view. Upper layers shadow lower ones (a file in a higher layer hides the same path below). The container sees *one* filesystem, assembled on the fly.
- **Image = ordered list of layers + metadata.** The metadata says what command to run, which ports/volumes, environment, etc.
- **Container = image + a writable top layer.** Starting a container adds one thin **read-write** layer on top of the image's read-only layers (copy-on-write: a file is only copied up when *modified*). This is why `docker run` is instant — nothing is copied upfront — and why a container's changes vanish when it's removed (the writable layer is discarded).
- **Layer caching.** When building, Docker reuses a cached layer if its instruction *and* its inputs are unchanged. The first step whose input changed (and every step after it) re-runs; earlier steps are reused.
- **Layer sharing.** Identical layers (same hash) are stored once on a host/registry and shared across all images that use them — so pulling a new image only downloads layers you don't already have.

> An image is a cached, shareable stack of read-only layers merged by a union filesystem; a container is that stack plus a thin writable top. Caching makes rebuilds fast; sharing makes storage and pulls cheap; copy-on-write makes startup instant.

## 4. Worked Example — the cache, and why order matters

Consider a simplified Cortex build with these steps (each an image instruction = a layer):

```text
Layer 1:  FROM eclipse-temurin:21-jre     # the JRE base (big, ~200MB) — rarely changes
Layer 2:  COPY  dependencies/             # downloaded libs (large) — changes when deps change
Layer 3:  COPY  app.jar                    # the compiled app (small) — changes EVERY code edit
Layer 4:  COPY  content/                   # markdown content — changes when you write chapters
```

Now you change one line of Scala and rebuild. Docker walks the layers checking the cache:

- **Layer 1** (`FROM ...jre`): inputs unchanged → **cache hit**, reused instantly.
- **Layer 2** (dependencies): your dependency list didn't change → **cache hit**, the big download is *skipped*.
- **Layer 3** (`app.jar`): the jar changed (you edited code) → **cache miss**, rebuilt.
- **Layer 4** (content): rebuilt too (it's *after* the miss — everything after a miss re-runs).

So a one-line code change rebuilds only layers 3–4 (small, fast), reusing the expensive JRE and dependency layers. *That's* why the build is seconds. But watch what happens if you ordered it badly — put `COPY app.jar` *before* the dependency download. Now every code change busts the dependency layer's cache, re-downloading hundreds of megabytes of libraries on *every* build. **The golden rule of Dockerfile ordering: put the things that change *least often* (base, dependencies) *early*, and the things that change *most often* (your app code) *late*** — so frequent changes only invalidate cheap, late layers. Cortex's real multi-stage Dockerfile (Chapter 3) is structured exactly this way, caching dependency downloads before copying source.

## 5. Build It

Run this. It models layer caching and shows how step order determines whether a code change triggers a fast or slow rebuild.

```python run
def build(steps, changed_inputs, cache):
    """Re-run a step if its input changed OR any earlier step was rebuilt (cache invalidation cascades)."""
    rebuilt_from = None
    total_cost = 0
    for i, (name, cost) in enumerate(steps):
        if rebuilt_from is None and (name in changed_inputs or cache.get(name) != "valid"):
            rebuilt_from = i                       # first miss; everything after also rebuilds
        if rebuilt_from is not None and i >= rebuilt_from:
            print(f"   layer {i+1} [{name:14}] REBUILD (cost {cost})")
            total_cost += cost; cache[name] = "valid"
        else:
            print(f"   layer {i+1} [{name:14}] cache hit")
    print(f"   -> total build cost: {total_cost}\n")

# GOOD order: cheap-and-frequent (app) LAST.
good = [("jre-base", 200), ("dependencies", 300), ("app-code", 10), ("content", 5)]
cache = {n: "valid" for n, _ in good}              # warm cache from a previous build
print("== good order: change app code ==")
build(good, changed_inputs={"app-code"}, cache=cache)

# BAD order: app code BEFORE the heavy dependency download.
bad = [("jre-base", 200), ("app-code", 10), ("dependencies", 300), ("content", 5)]
cache = {n: "valid" for n, _ in bad}
print("== bad order: change app code ==")
build(bad, changed_inputs={"app-code"}, cache=cache)
```

**Now break it.** Compare the two total costs: the good order rebuilds ~15 (app + content), the bad order rebuilds ~315 (app + the giant dependency re-download + content). Same one-line code change, a 20× difference in build cost — purely from *layer order*. Then change `"dependencies"` in the good build and watch app + content rebuild too (they're after the miss). This is the entire art of writing a fast Dockerfile: arrange layers so your *frequent* changes invalidate only *cheap, late* layers, never the expensive early ones.

## 6. Trade-offs & Complexity

| Layered images | A single monolithic blob |
|---|---|
| Fast rebuilds (cache unchanged layers) | Rebuild everything every time |
| Small pulls (download only new layers) | Re-download the whole thing |
| Shared base layers across images | Redundant copies everywhere |
| Instant container start (writable top) | Copy the whole image to start |
| Must order steps thoughtfully | No ordering to think about |

The "cost" of layers is mostly *learning to exploit them*: you have to understand caching to write a good Dockerfile, and a careless order silently makes builds slow. There's also a real trade in *number* of layers — each instruction adds one, and too many tiny layers add overhead, so you sometimes combine related commands. But these are optimizations on top of a design that's almost pure upside: without layers, Docker simply wouldn't be fast or space-efficient enough to be practical. The complexity is "understand caching"; the payoff is every build and every pull.

## 7. Edge Cases & Failure Modes

- **Cache-busting order.** Copying source *before* installing dependencies makes every code change re-run the dependency step. Order least-changing first (Chapter 3).
- **Secrets in a layer.** A file you `COPY` then `rm` in a *later* layer is *still in the earlier layer* — layers are append-only history. A leaked secret in any layer is in the image forever. Use build secrets/multi-stage, never `COPY` a secret.
- **Image bloat from intermediate junk.** Build tools, caches, and source copied in early layers stay in the image even if unused at runtime — unless you use multi-stage builds (Chapter 3) to leave them behind.
- **Surprising deletes.** Deleting a file in a higher layer *hides* it (a "whiteout"), but the bytes still exist in the lower layer, so it doesn't shrink the image. Don't rely on late `rm` to reduce size.

## 8. Practice

> **Exercise 1 — Predict the rebuild.** Given layers `[base, deps, app, content]` (cache warm), which rebuild when you (a) edit app code, (b) add a dependency, (c) write a new chapter? Why?

<details>
<summary><strong>Answer</strong></summary>

The rule (§3): Docker reuses a cached layer only if its instruction *and* inputs are unchanged; the **first** layer whose input changed re-runs, **and every layer after it** re-runs too (the cascade), because each layer builds on the one below.

- **(a) edit app code →** `app` and `content` rebuild. `base` and `deps` are unchanged (cache hits); `app` is the first miss, and `content` is after it, so it rebuilds too — even though the content itself didn't change. Cheap, because both are small, late layers.
- **(b) add a dependency →** `deps`, `app`, and `content` all rebuild. `deps` is now the first miss (its input — the dependency list — changed), so the expensive download re-runs, and everything after (`app`, `content`) cascades. Only `base` is reused.
- **(c) write a new chapter →** only `content` rebuilds. `base`, `deps`, `app` are all unchanged and earlier, so they're cache hits; `content` is the last layer and the only miss. The cheapest possible rebuild.

The pattern to internalize: a change's cost is set by *how early* the first-affected layer sits. That's why the next exercise's ordering matters so much.

</details>

> **Exercise 2 — Fix the order.** A Dockerfile does `COPY . .` (all source) and *then* `download dependencies`. Explain why every build is slow, and reorder it.

<details>
<summary><strong>Answer</strong></summary>

**Why it's slow:** `COPY . .` copies *all* source, so *any* file change — even a one-line code edit — busts that layer's cache. Because cache invalidation cascades to every later layer (§3), the `download dependencies` step that comes *after* it re-runs on **every** build, re-downloading hundreds of megabytes of libraries that didn't actually change. You pay the heavy, slow step for a trivial edit.

**The fix** — apply the golden rule (§4): put what changes *least often* early, what changes *most often* late. Copy the dependency *manifest* (which rarely changes) and install dependencies *before* copying the source:

```text
COPY <dependency-manifest>        # e.g. build.sbt / package.json — changes rarely
RUN  download dependencies        # cached unless the manifest changes
COPY . .                          # the frequently-changing source — LAST
```

Now a code edit only invalidates the final `COPY . .` layer; the expensive dependency download stays cached. This is exactly how Cortex's real Dockerfile is structured — `build.sbt`/`project` copied and `sbt update` run before the source (Chapter 3).

</details>

> **Exercise 3 — Where's the secret?** A teammate `COPY`s an API key file, uses it, then `rm`s it in a later instruction, and ships the image. Explain why the key is still exposed.

<details>
<summary><strong>Answer</strong></summary>

Because **layers are append-only history**, not a final snapshot (§7). The `COPY` of the key created one layer that *contains the key's bytes forever*. The later `rm` creates a *new* layer on top whose only effect is a **whiteout** — it *hides* the file from the merged view, but it cannot reach down and erase bytes from the earlier read-only layer. Those bytes are immutable history.

So anyone with the image can inspect the earlier layer directly (e.g. `docker history`, or unpacking the layer tarballs) and read the key in plaintext. A late `rm` reduces neither the image size nor the exposure — it just paints over the file.

The correct approach: never `COPY` a secret into a layer at all. Use **build secrets** (mounted only during a `RUN`, never persisted) or a **multi-stage build** (Chapter 3) where the secret lives only in a discarded builder stage and never reaches the final image.

</details>

```quiz
{
  "prompt": "Why is rebuilding an image after a one-line code change usually fast?",
  "input": "Choose one:",
  "options": [
    "Images are layered and cached: Docker reuses unchanged layers (like the base and dependencies) and rebuilds only the layer(s) whose inputs changed and those after it",
    "Docker recompiles the kernel each time",
    "Docker only stores the source code, not the binaries",
    "Every build downloads the whole image fresh"
  ],
  "answer": "Images are layered and cached: Docker reuses unchanged layers (like the base and dependencies) and rebuilds only the layer(s) whose inputs changed and those after it"
}
```

## In the Wild

- **[Docker — Images and layers / build cache](https://docs.docker.com/build/cache/)** — how layers are cached and the ordering rules to exploit them.
- **[Docker — About storage drivers (OverlayFS)](https://docs.docker.com/storage/storagedriver/)** — the union filesystem that merges layers and the copy-on-write writable layer.
- **[Cortex `Dockerfile`](https://github.com/ani2fun/cortex/blob/main/Dockerfile)** — a real multi-stage build that orders layers to cache dependency downloads before source (next chapter).

---

**Next:** now we write one. Cortex's `Dockerfile` is a *multi-stage* build — a heavy builder stage that compiles, and a tiny runtime stage that ships. Let's read it. → [3. Writing a Dockerfile: Cortex's multi-stage build](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/writing-a-dockerfile)
