---
title: '3. Writing a Dockerfile: Cortex''s multi-stage build'
summary: A Dockerfile is the recipe that builds an image, one instruction per layer. Cortex uses a multi-stage build — a heavy builder that compiles everything, and a tiny runtime that ships only the result.
---

# 3. Writing a Dockerfile: Cortex's multi-stage build

## TL;DR

> A **Dockerfile** is the recipe for building an image: a sequence of instructions (`FROM`, `COPY`, `RUN`, `CMD`), each producing a layer (Chapter 2). The professional pattern is a **multi-stage build**: a first **builder** stage with all the heavy build tooling (here: sbt, the Scala compiler, Node) that *compiles* the app, and a second **runtime** stage built on a minimal base that `COPY --from=builder`s *only the finished artifacts*. The compilers, caches, and source never reach the final image — so it's small, fast to pull, and has less attack surface. Cortex's `Dockerfile` does exactly this: an sbt+Node builder, then a tiny JRE runtime.

## 1. Motivation

To build Cortex you need a *lot* of tooling: the Scala build tool (sbt), the Scala compiler, Node.js and npm (for the Vite frontend bundle), and gigabytes of downloaded dependencies. To *run* Cortex you need almost none of that — just a Java runtime and the compiled artifacts. If you built the image naively (install all the build tools, compile, ship the whole thing), your runtime image would be *enormous* — carrying compilers and caches it never uses — slow to pull, slow to deploy, and a bigger security target (every tool in the image is something an attacker might exploit).

The multi-stage build solves this elegantly: do the heavy lifting in a throwaway builder stage, then start *fresh* from a minimal runtime base and copy in *only the outputs*. The build tools evaporate; the final image contains the JRE, the app, and nothing else. This is the difference between a 2GB image full of build cruft and a lean ~200MB one — and it's a standard you should apply to essentially every compiled application. Cortex's Dockerfile is a clean example worth reading line by line.

## 2. Intuition (Analogy)

A **woodworking shop versus the finished table you deliver.** To *build* a table you need a whole workshop: saws, sanders, clamps, jigs, sawdust everywhere, offcuts piling up. But you don't deliver the *workshop* to the customer — you deliver the *table*. Imagine shipping the entire sawdust-covered workshop along with each table; absurd, heavy, and now the customer's living room has power tools in it.

A multi-stage build is "build in the workshop, ship only the table." The **builder stage** is the messy workshop (sbt, compilers, downloaded dependencies, build caches — all the sawdust). The **runtime stage** is the clean delivery: start from an empty room (a minimal base image) and bring in *only the finished table* (the compiled app + the runtime to use it). The workshop is left behind entirely. Your customer — the production server — gets a lean, clean artifact with no power tools lying around.

## 3. Formal Definition

- **Dockerfile.** A text file of instructions Docker executes top-to-bottom to build an image. Key instructions:
  - `FROM <image> [AS <name>]` — start from a base image (and name this **stage**).
  - `WORKDIR <dir>` — set the working directory.
  - `COPY <src> <dst>` — copy files from the build context into the image (a layer).
  - `RUN <cmd>` — execute a command at *build* time (compile, install) — a layer.
  - `ENV <k>=<v>` — set an environment variable in the image.
  - `EXPOSE <port>` — document the port the app listens on.
  - `CMD ["..."]` — the default command run when a *container starts* (run time, not build time).
- **Multi-stage build.** Multiple `FROM` stages in one Dockerfile. A later stage uses `COPY --from=<earlier-stage>` to pull *artifacts* from an earlier stage *without* inheriting its bulk. Only the **last** stage becomes the final image; earlier stages are discarded.
- **`COPY --from=builder <path> <dst>`** — the heart of multi-stage: copy a *built artifact* out of the builder into the lean runtime.
- **Build vs run time.** `RUN` happens during `docker build` (compiling); `CMD`/`ENTRYPOINT` happen during `docker run` (starting the app). Confusing the two is a classic beginner error.

> A Dockerfile is the layered build recipe; multi-stage builds compile in a heavy throwaway *builder* stage and ship only the artifacts in a minimal *runtime* stage. The result: a small, clean image with no build tooling inside.

## 4. Worked Example — Cortex's Dockerfile

Cortex's real [`Dockerfile`](https://github.com/ani2fun/cortex/blob/main/Dockerfile), in two stages:

```dockerfile
# ── Stage 1: BUILDER — heavy tooling, compiles everything, then is discarded ──
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.7_3.6.2 AS builder
# (install Node 20 for the Vite frontend build)
WORKDIR /build

# Copy build definitions FIRST and cache dependency downloads (layer-order trick, Chapter 2):
COPY build.sbt ./
COPY project ./project
RUN sbt update                      # download sbt deps — cached unless build.sbt changes

# Now copy the source and build the artifacts:
COPY api ./api
COPY shared ./shared
COPY server ./server
COPY client ./client
COPY content ./content
RUN sbt "server/Universal/stage" "client/fullLinkJS"   # compile server + link Scala.js
RUN cd client && npm install && npm run build           # bundle the frontend with Vite

# ── Stage 2: RUNTIME — minimal JRE, copies ONLY the finished artifacts ──
FROM eclipse-temurin:21-jre-jammy AS runtime
COPY --from=builder /build/server/target/universal/stage /app      # the compiled server
COPY --from=builder /build/client/dist                  /app/static # the bundled frontend
COPY --from=builder /build/content                      /app/content # the markdown content
ENV STATIC_DIR=/app/static
ENV CORTEX_ROOT=/app/content/cortex
ENV PORT=8080
EXPOSE 8080
CMD ["bin/cortex-server"]            # what runs when the container starts
```

Read the two stages. The **builder** starts from a fat `scala-sbt` image (it has sbt, the compiler, and we add Node) — it copies `build.sbt`/`project` *first* and runs `sbt update` to cache dependency downloads (Chapter 2's ordering trick!), *then* copies source and compiles the server (`server/Universal/stage`), links the Scala.js client (`client/fullLinkJS`), and bundles the frontend with Vite. After this stage, `/build` contains gigabytes of compilers, caches, and intermediate output.

The **runtime** stage throws all that away. It starts *fresh* from `eclipse-temurin:21-jre-jammy` — a minimal image with *just a Java runtime, no build tools* — and `COPY --from=builder` pulls in **only three things**: the compiled server, the bundled frontend (into `/app/static`), and the content (into `/app/content`). The `ENV` lines set the config defaults (these are the env vars Part 1, Chapter 14's config reads, and Part 4 later overrides in Kubernetes), `EXPOSE 8080` documents the port, and `CMD ["bin/cortex-server"]` is what runs when a container starts. The final image is the JRE + Cortex's artifacts — *none* of sbt, the compiler, Node, or the dependency caches. Small, clean, and exactly what Kubernetes pulls and runs in production.

## 5. Build It

Run this. It models a multi-stage build, computing the final image size with and without the multi-stage trick.

```python run
# Each "step" adds some megabytes to its stage's image.
builder_stage = {
    "scala-sbt base":     900,    # compiler + sbt + node
    "downloaded deps":    700,    # gigabytes of libraries
    "compiled artifacts":  60,    # the actual output we want
}
runtime_base = {"jre-jammy base": 180}     # minimal Java runtime, no build tools

def single_stage_image():
    # Naive: ship the WHOLE builder (tools, deps, AND artifacts).
    return sum(builder_stage.values())

def multi_stage_image():
    # Start from the minimal runtime, COPY --from=builder ONLY the artifacts.
    return sum(runtime_base.values()) + builder_stage["compiled artifacts"]

print(f"single-stage image (ships the workshop): {single_stage_image()} MB")
print(f"multi-stage image  (ships the table)   : {multi_stage_image()} MB")
print(f"savings: {single_stage_image() - multi_stage_image()} MB "
      f"({100*(1 - multi_stage_image()/single_stage_image()):.0f}% smaller)")
print("\nThe compilers, deps, and caches never reach production — smaller, faster, safer.")
```

**Now break it.** Add a "build cache" of 1200 MB to the `builder_stage` (sbt and npm caches are huge) and recompute — the single-stage image balloons while the multi-stage image is *unchanged*, because the cache lives only in the discarded builder. That's the multi-stage guarantee: no matter how much junk the build accumulates, *none of it* reaches the runtime image, which contains only the artifacts you explicitly `COPY --from`. The final image's size depends on what you *ship*, not on what it took to *build*.

## 6. Trade-offs & Complexity

| Multi-stage build (Cortex) | Single-stage build |
|---|---|
| Tiny runtime image (no build tools) | Huge image full of compilers/caches |
| Smaller attack surface in production | Every build tool is exposed |
| Fast to pull and deploy | Slow pulls, slow rollouts |
| Slightly more complex Dockerfile | One straightforward stage |
| Clear build-vs-runtime separation | Build and runtime tangled |

The cost is a marginally more complex Dockerfile (two `FROM`s, the `COPY --from` lines) and the discipline to know *which* artifacts to copy across. That's trivial next to the benefits: a production image that's a fraction of the size, deploys faster, and exposes far less to attackers (no compiler, no shell tools, no source). For any compiled language — Scala, Go, Rust, Java, a bundled frontend — multi-stage is the default professional pattern, and there's rarely a reason not to use it.

## 7. Edge Cases & Failure Modes

- **`RUN` vs `CMD` confusion.** `RUN` executes at *build* time (baked into a layer); `CMD` runs at *container start*. Putting "start the server" in a `RUN` tries to run it during the build (and fails/hangs). The app's start command goes in `CMD`/`ENTRYPOINT`.
- **Cache-busting copies.** `COPY . .` early (before installing deps) re-runs everything on any file change (Chapter 2). Copy build definitions and install deps *before* copying source, as Cortex does.
- **Forgetting to copy a needed artifact.** If the runtime stage doesn't `COPY --from` something the app needs (e.g. the content files), it'll start but fail at runtime. The image only has what you explicitly copy.
- **A shell/root-heavy base.** Even the runtime base matters: a smaller base (distroless, alpine, or a slim JRE) means less attack surface. And running as root inside the container is a risk addressed in Chapter 12.

## 8. Practice

> **Exercise 1 — Builder or runtime?** For each, say which stage it belongs in: (a) the Scala compiler; (b) the JRE; (c) downloaded sbt dependencies; (d) the compiled `cortex-server`; (e) Node/npm.

<details>
<summary><strong>Answer</strong></summary>

The test (§1–2): is it needed to **build** the app (→ builder, gets discarded) or to **run** it (→ runtime, ships)? The builder is the messy workshop; the runtime is the clean delivery of just the table.

- **(a) Scala compiler → builder.** A build-time tool — it compiles source into the artifact, then is never needed again. Shipping it would just bloat the image and add attack surface.
- **(b) JRE → runtime.** Needed to *execute* the compiled app at container start; it's the minimal base the runtime stage builds `FROM`.
- **(c) downloaded sbt dependencies → builder.** Needed during compilation. (Their *compiled* output is folded into the artifact; the raw download cache itself stays in the discarded builder.)
- **(d) compiled `cortex-server` → runtime.** This *is* the table — the very artifact you `COPY --from=builder` into the runtime stage.
- **(e) Node/npm → builder.** Build-time only — used to bundle the Vite frontend. The *bundled output* ships; Node and npm do not.

Rule of thumb: compilers and package managers (a, c, e) stay behind in the workshop; the runtime and the finished artifacts (b, d) are what you deliver.

</details>

> **Exercise 2 — Why two stages?** In two sentences, explain what `COPY --from=builder` does and why Cortex's final image doesn't contain sbt or Node.

<details>
<summary><strong>Answer</strong></summary>

`COPY --from=builder <path> <dst>` reaches *into the earlier builder stage* and copies out **only the named artifacts** (the compiled server, the bundled frontend, the content) into the fresh runtime stage — the heart of a multi-stage build (§3). Cortex's final image doesn't contain sbt or Node because **only the last stage becomes the image**: the builder (with sbt, the compiler, Node, and all the dependency caches) is discarded entirely, and the runtime stage starts from a minimal JRE base that receives *nothing* except the explicitly copied artifacts.

</details>

> **Exercise 3 — Order for cache.** Cortex copies `build.sbt` + `project` and runs `sbt update` *before* copying the source. Explain how this speeds up rebuilds after a code change (tie to Chapter 2).

<details>
<summary><strong>Answer</strong></summary>

This is Chapter 2's layer-ordering rule applied deliberately: each instruction is a layer, and the **first** layer whose input changed invalidates itself *and every layer after it*. So you want the slow, rarely-changing step *early* and the fast, frequently-changing step *late*.

`build.sbt` and `project` define the *dependencies*, and they change rarely. By copying them first and running `sbt update` (the expensive download) *before* copying source, that heavy layer's input is just the build definitions — so on an ordinary code edit, `build.sbt` is unchanged, `sbt update` is a **cache hit**, and the gigabytes of dependencies are *not* re-downloaded. Only the later `COPY <source>` + compile layers (the first to actually change) re-run.

Flip the order — copy source before `sbt update` — and every code edit busts the source layer, cascading into a full dependency re-download on *every* build (the slow-build trap from Chapter 2, §4). Same code change, 10-second rebuild vs 10-minute one, purely from where the dependency step sits.

</details>

```quiz
{
  "prompt": "What does a multi-stage Docker build achieve?",
  "input": "Choose one:",
  "options": [
    "It compiles the app in a heavy 'builder' stage, then `COPY --from`s only the finished artifacts into a minimal runtime stage — so the build tools, caches, and source never reach the small final image",
    "It builds the image twice for redundancy",
    "It runs the application in two containers",
    "It encrypts the build stage"
  ],
  "answer": "It compiles the app in a heavy 'builder' stage, then `COPY --from`s only the finished artifacts into a minimal runtime stage — so the build tools, caches, and source never reach the small final image"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Docker — Multi-stage builds](https://docs.docker.com/build/building/multi-stage/)** — the official guide to `FROM ... AS` and `COPY --from`.
- **[Docker — Dockerfile best practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)** — instruction ordering, caching, and image size.
- **[Cortex `Dockerfile`](https://github.com/ani2fun/cortex/blob/main/Dockerfile)** — the real builder + runtime stages dissected above.

---

**Next:** Cortex isn't just one container — it needs Postgres, Redis, Mongo, Keycloak, and more. `docker-compose` brings the whole stack up on your laptop with one command. → [4. docker-compose: the whole stack on your laptop](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/docker-compose)
