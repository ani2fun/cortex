---
title: Serving performance & the Cloudflare edge
summary: How cortex.kakde.eu is made fast — origin compression + immutable caching, a landing bundle that lazy-loads its heavy chunks, and a Cloudflare proxy that edge-caches assets near the visitor. The curl checks that prove each layer, the measured before/after, and how to smoke-test an unreleased build on the cluster.
---

> 📓 **Three layers, each independently verifiable.** Payload (compress + slim the bundle), repeat-cost (cache headers), and distance (Cloudflare edge). Each is one `curl` away — if a check below fails, that layer has regressed.

## Why this exists

cortex pods run on the **home** workers (`wk-1`/`wk-2`); the only public entrypoint is the Contabo edge (`vm-1`) reached over a **WireGuard tunnel** (see [Topology](/cortex/cortex-onboarding/runbooks/production/topology)). So every byte the origin serves crosses that tunnel, gated by home upload bandwidth. Before this work the landing page shipped **~10.8 MB of uncompressed, uncached JS on every visit** — including Monaco (editor, 3.9 MB) + Mermaid (2.8 MB) + KaTeX the home page never uses. "More pods" can't help that — it's payload + delivery, not CPU.

## The three layers

| Layer | Where it lives | What it does | Measured win |
|---|---|---|---|
| **1. Compression** | server image (`HttpApp` `Server.Config.responseCompression`) | gzip/deflate every response > 1 KiB, at the **origin** so the tunnel carries shrunk bytes | JS **3.8×**, CSS **7.9×**, API JSON **7.8×** |
| **2a. Cache headers** | server image (`FileServer` constants) | `immutable` on hashed `/assets/*`, `no-cache` on `index.html` | repeat visit **~10.8 MB → ~1.4 KB** |
| **2b. Slim landing** | client build (`vite.config.mjs`, `markdown/monaco.ts`) | Monaco/Mermaid/shiki/katex load on demand, not on the home page | landing JS: **0 heavy chunks** (was 4–5) |
| **3. Cloudflare edge** | DNS / Cloudflare zone (no code) | edge-cache the `immutable` assets near each visitor; TLS terminates at a nearby PoP | entry 853 KB served from edge **HIT**, not the tunnel |

Net: first visit **~10.8 MB → 929 KB**; repeat visit **→ ~1.4 KB**; and once proxied, cached assets never touch the home tunnel again.

## Verify each layer (copy-paste)

```bash
# Layer 1 — compression (expect content-encoding: gzip; raw vs wire shows the ratio)
A=$(curl -s --compressed https://cortex.kakde.eu/ | grep -oE 'assets/[^"]+\.js' | head -1)
curl -sS -D - -o /dev/null -H 'Accept-Encoding: gzip' "https://cortex.kakde.eu/$A" \
  | grep -iE 'content-encoding|cache-control'
#   content-encoding: gzip
#   cache-control: public, max-age=31536000, immutable      <- Layer 2a

# Layer 2a — index.html must NOT be immutable (so new asset hashes are picked up)
curl -sS -D - -o /dev/null https://cortex.kakde.eu/ | grep -i cache-control   # -> no-cache

# Layer 2b — the landing page must reference ZERO heavy chunks
curl -s --compressed https://cortex.kakde.eu/ | grep -oE 'assets/[^"]+\.js' \
  | grep -cE 'monaco|mermaid|shiki|katex|d2'      # -> 0

# Layer 3 — served through Cloudflare, and assets edge-cache (MISS then HIT)
for i in 1 2; do curl -sS -D - -o /dev/null -H 'Accept-Encoding: br,gzip' \
  "https://cortex.kakde.eu/$A" | grep -iE 'server:|cf-cache-status'; echo ---; done
#   server: cloudflare ; cf-cache-status: MISS    (1st)
#   server: cloudflare ; cf-cache-status: HIT     (2nd)

# Dynamic endpoints must NOT be edge-cached
curl -sS -D - -o /dev/null https://cortex.kakde.eu/api/cortex/index | grep -i cf-cache-status  # -> DYNAMIC
```

The four `curl` blocks above **are** the measurement — wrap them in a one-off shell script if you run them often. The teaching version, with the full before/after capacity recompute, is [ch 53 — Cortex, made fast](/cortex/system-design/capstones/cortex-edge-delivery).

## The Cloudflare edge — what's configured and how it works

DNS for `kakde.eu` is on Cloudflare (nameservers `clyde`/`nora.ns.cloudflare.com`). What makes the edge work:

| Setting | Value | Why |
|---|---|---|
| **DNS record** | `*.kakde.eu` CNAME → `kakde.eu` (apex A → `84.247.143.66`), both **Proxied (orange)** | The wildcard covers `cortex.kakde.eu` with no per-host record — it resolves to Cloudflare anycast (`104.21.x` / `172.67.x`), not the origin. |
| **SSL/TLS mode** | **Full (strict)** | CF↔origin leg is HTTPS *and* validates the origin cert. Origin has a valid cert-manager Let's Encrypt cert, so strict is safe. *Never* use Flexible — Traefik forces HTTPS → redirect loop. |
| **CAA records** | `letsencrypt.org` **+ `pki.goog`** + digicert/ssl.com (issue + issuewild) | `letsencrypt.org` lets cert-manager mint the **origin** cert; `pki.goog` lets Cloudflare mint its Google-issued **edge** cert. If CAA were Let's-Encrypt-only, the edge cert would eventually fail to renew → TLS outage. |
| **Caching** | default (respects origin `Cache-Control`) | The `immutable` assets edge-cache (HIT); `index.html` (`no-cache`) and `/api/*` stay `DYNAMIC`. Optionally add a Cache Rule `URI path starts with /api/ → Bypass cache` as belt-and-suspenders. |

How it pays off: a visitor resolves `cortex.kakde.eu` to a **nearby Cloudflare PoP**, TLS terminates there, and the `immutable` JS/CSS are served from that edge after the first fetch — so they **stop crossing the home tunnel entirely**, and distant users no longer round-trip to Contabo→home. CF also re-compresses HTML with **Brotli**.

> ⚠️ Two watch-items: (1) Cloudflare's **Free plan has a ~100 s proxy timeout** — fine for `/api/run` (fast) and SSE that streams continuously, but a long-idle `/tutor/*` SSE stream could be cut if the coach ever goes public. (2) Toggling SSL mode is **zone-wide** — it affects every already-proxied `*.kakde.eu` host (argocd, keycloak, grafana…); they all use the same cert-manager certs, so Full (strict) is correct for all.

## Smoke-test an unreleased build on the cluster (no CI round-trip)

To validate a server/client change on the **real edge** before squashing to `main`, side-load a thin image and point the live Deployment at it briefly. The artifacts are arch-neutral (JVM bytecode + JS), so a thin amd64 image builds fast even from an arm64 Mac.

```bash
# 1. Build artifacts natively, then a THIN amd64 image (no compilation in the image)
sbt server/Universal/stage           # -> server/target/universal/stage/{bin,lib}
( cd client && npm run build )        # -> client/dist
#   thin Dockerfile: FROM eclipse-temurin:21-jre-jammy; COPY stage -> /app, dist -> /app/static,
#   content -> /app/content; ENV STATIC_DIR/CORTEX_ROOT/BLOG_ROOT/PORT; ENTRYPOINT /app/bin/cortex-server
docker buildx build --platform linux/amd64 --load -t cortex:smoke <ctx>

# 2. Side-load into containerd on BOTH workers (2 replicas; pullPolicy IfNotPresent)
docker save cortex:smoke | ssh wk-1 'k3s ctr -n k8s.io images import -'
docker save cortex:smoke | ssh wk-2 'k3s ctr -n k8s.io images import -'

# 3. Capture the live image, PAUSE Argo (it has prune+selfHeal and WILL revert manual edits), swap
ssh ms-1 'kubectl get deploy cortex -n apps-prod -o jsonpath="{.spec.template.spec.containers[0].image}"'  # save this!
ssh ms-1 'kubectl -n argocd patch application cortex --type merge -p "{\"spec\":{\"syncPolicy\":null}}"'
ssh ms-1 'kubectl -n apps-prod set image deploy/cortex cortex=docker.io/library/cortex:smoke'
ssh ms-1 'kubectl -n apps-prod rollout status deploy/cortex --timeout=150s'

# 4. Verify on cortex.kakde.eu (the curl checks above), THEN restore:
ssh ms-1 'kubectl -n apps-prod set image deploy/cortex cortex=<the-saved-image>'
ssh ms-1 'kubectl -n argocd patch application cortex --type merge -p "{\"spec\":{\"syncPolicy\":{\"automated\":{\"prune\":true,\"selfHeal\":true}}}}"'
ssh wk-1 'k3s ctr -n k8s.io images rm docker.io/library/cortex:smoke'   # and wk-2
```

Restoring Argo reconciles the Deployment back to the git-pinned image — re-enabling it *is* the rollback. (The normal release is just a push to `main`; see [Deploy & rollback](/cortex/cortex-onboarding/runbooks/production/deploy-and-rollback).)

## When a page is slow again — where to look

1. **Headers regressed?** Run the four verify blocks above. No `content-encoding` / no `immutable` → the server image lost Layer 1/2 (check `HttpApp`/`FileServer`). Heavy-chunk count > 0 → `manualChunks` crept back into `vite.config.mjs` (it co-locates Vite's `__vitePreload` into vendor chunks → the entry static-imports them; keep it deleted).
2. **`cf-cache-status: MISS` every time?** The asset isn't cacheable (lost `immutable`), or you're hitting a cold PoP, or the record went grey — `dig +short @1.1.1.1 cortex.kakde.eu` should return Cloudflare IPs, not `84.247.143.66`.
3. **Everything cached but still slow?** Now it's the origin path — see [Observability & incidents](/cortex/cortex-onboarding/runbooks/production/observability-and-incidents).

> **Next:** the teaching version of this — why the site was slow and how each layer works — is [Compress once, cache forever, edge-serve globally](/cortex/homelab-from-scratch/the-edge/serving-performance-and-cdn) in *Homelab from Scratch*.
