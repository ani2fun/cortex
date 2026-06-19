---
title: '53. Cortex, made fast — compression, caching & the edge'
group: 'Capstone: Cortex Platform (a system you are currently looking at)'
summary: Chapter 48 found the reader ceiling was the home upload link and said "you'll need a CDN." This is that stage — shipped and measured. gzip at the origin, immutable cache headers, a landing bundle that lazy-loads its heavy chunks, and a Cloudflare proxy cut first-load ~10.8 MB → 929 KB (~12×), made repeat visits ~free, and lifted the egress-bound reader ceiling from ~500 reads/s toward CPU-bound — with cf-cache HITs proving assets now leave the home tunnel entirely.
---

# 53. Cortex, made fast — compression, caching & the edge

## TL;DR
> [Chapter 48](/cortex/system-design/capstones/cortex-capacity-today) found the reader ceiling isn't CPU — it's the **home upload link** (~500 reads/s of 25 KB pages ≈ 100 Mbit/s), and flagged a CDN as the fix. This chapter ships and **measures** it. Three origin-side layers plus the edge: **(1) gzip at the origin** (JS **3.8×**, CSS **7.9×**, API JSON **7.8×**) so the WireGuard tunnel carries shrunk bytes; **(2) `Cache-Control: immutable`** on content-hashed assets + `no-cache` on `index.html`, so a returning visitor re-downloads **nothing**; **(3) a slim landing bundle** — the editor (Monaco, 3.9 MB) and diagram engine (Mermaid, 2.8 MB) now lazy-load, so the home page ships **0 heavy chunks** instead of 4–5; and **(4) a Cloudflare proxy** that edge-caches the immutable assets near each visitor (`cf-cache-status: HIT`) so they **stop crossing the home tunnel**. Measured result on `cortex.kakde.eu`: **first visit ~10.8 MB → 929 KB (~12×), repeat visit → ~1.4 KB**, and the egress ceiling moves from ~500 reads/s to **CPU-bound (~2,000 req/s)** for cached content. The lesson layers onto ch 48's: **find the binding constraint, then attack it in the right order — shrink the payload, cache it, then distribute it; a CDN in front of a 10 MB uncompressed bundle just serves the same bloat from more places.**

## 1. Motivation — the ceiling ch 48 predicted

[Chapter 48](/cortex/system-design/capstones/cortex-capacity-today) did the capacity math and found the reader path's binding constraint isn't the 1-vCPU pod (~2,000 req/s) — it's the **residential upload link**: at 25 KB/page the ~100 Mbit/s home uplink saturates around **500 reads/s**, and it told you to "watch *Egress at peak* — that's the number that decides when you need a CDN." [Chapter 51](/cortex/system-design/capstones/scaling-cortex-like-leetcode) put that CDN at "stage 4."

This is that stage — except instead of a hypothetical, it's the **actual change that shipped to `cortex.kakde.eu`**, with the before/after measured by `curl`. It's the most honest kind of capacity chapter: not "here's the model," but "here's the model, here's what we changed, and here's what the wire said afterward." It also corrects an omission in ch 48: that chapter sized *steady-state* 25 KB chapter reads, but never modelled the **first-load cost** — the SPA bundle a brand-new visitor downloads before any chapter renders. That number was **~10.8 MB, uncompressed, on every visit**.

## 2. The measured baseline — what was actually being served

Before any change, the landing page pulled this — uncompressed, uncached, and most of it for features the home page never uses:

| What loaded | Size (uncompressed) | Why it was there |
|---|---|---|
| `index.js` (Scala.js app) | 3.25 MB | the entry |
| `monaco` (code editor) | 3.94 MB | eagerly imported, used only on runnable blocks |
| `mermaid` (diagrams) | 2.76 MB | eagerly imported, used only on diagram pages |
| `katex` + `prismjs` + CSS | ~0.85 MB | math + highlight + styles |
| **Total, every visit** | **~10.8 MB** | **no gzip, no cache headers** |

Three independent problems: the bytes weren't **compressed**, they weren't **cacheable**, and the page shipped code it didn't need. Each has a different fix, and the order matters.

## 2.5 Why the home tunnel makes this worse than a cloud app

Recall the topology ([platform overview](/cortex/system-design/capstones/cortex-platform-overview)): the pods run on the **home** workers; the only public door is a Contabo edge VM joined to home by a WireGuard tunnel. So every origin byte is paid for **twice over the internet** (edge⇄home, then edge⇄visitor) and the return leg is gated by **home upload**. A 10 MB bundle isn't just slow to download — it's slow to *push out of the house*. That's why the same optimizations that are "nice" for a cloud app are **load-bearing** here.

## 3. The four layers, and what each measured

| Layer | Where | Mechanism | Measured |
|---|---|---|---|
| **Compress** | server (`HttpApp` `Server.Config.responseCompression`) | gzip/deflate > 1 KiB, **at the origin** so the tunnel carries shrunk bytes | JS **3.8×** (3.2 MB→853 KB), CSS **7.9×**, API JSON **7.8×** (162→21 KB) |
| **Cache** | server (`FileServer`) | `immutable, max-age=1y` on hashed `/assets/*`; `no-cache` on `index.html` | repeat visit **→ ~1.4 KB** (HTML only; assets from disk cache) |
| **Slim** | client (`vite.config.mjs`, `markdown/monaco.ts`) | `React.lazy(()=>import("./monaco"))` + **delete `manualChunks`** | landing **0 heavy chunks** (was 4–5) |
| **Edge** | DNS (Cloudflare proxy) | edge-cache the immutable assets near the visitor | `cf-cache-status: HIT`; TLS terminates at a nearby PoP |

Two of these have a non-obvious twist worth keeping:

- **Compress at the *origin*, not just the edge.** A CDN can gzip on egress to the visitor, but then full-size bytes still cross your slow tunnel. Compressing in the app shrinks them *before* the bottleneck — the leg that actually binds.
- **Over-eager `manualChunks` back-fired.** Hand-mapping each big library to a named vendor chunk made the bundler co-locate its own `__vitePreload` helper *inside* those chunks, so the entry **statically imported them anyway** — dragging ~9 MB back onto the landing page. Deleting the manual map and letting the bundler auto-split (everything heavy is already reached via dynamic `import()`) fixed it. *Don't hand-chunk libraries that are already dynamically imported.*

## 4. Recomputing ch 48's reader ceiling

Now redo ch 48's egress arithmetic with the measured changes. Two regimes — the one-time **first load**, and **steady-state reads**:

| | Before | After compression | After + edge cache |
|---|---|---|---|
| **First load** (new visitor) | ~10.8 MB from home uplink | ~929 KB from home uplink | ~929 KB **from a nearby PoP** (origin hit once globally, then HIT) |
| **Per chapter read** (25 KB JSON) | 25 KB → home uplink binds at **~500 reads/s** | ~5 KB gz → uplink binds at **~2,500 reads/s** | API JSON still origin (`DYNAMIC`), but static assets leave the tunnel → uplink no longer the reader bind |
| **Repeat visitor** | re-downloads ~10.8 MB | re-downloads ~929 KB | **~0** (immutable assets cached in the browser) |

So compression alone lifts the egress ceiling **~5×** (500 → 2,500 reads/s), and the edge removes static assets from the home uplink **entirely** — after the first global fetch they're served from Cloudflare's edge (`HIT`, measured). The home uplink then carries only **cache-miss + dynamic API JSON (gzipped)**, so the reader ceiling reverts to ch 48's *CPU* bound (~2,000 req/s) rather than the uplink. The CDN ch 48 predicted doesn't just help — it **changes which resource binds.**

Run the recomputation yourself — the egress ceiling as a function of page size and uplink:

```python run
UPLINK_MBIT = 100          # typical residential upload
def reads_per_sec(kb_per_read, uplink_mbit=UPLINK_MBIT):
    return uplink_mbit * 1_000_000 / (kb_per_read * 1024 * 8)

print(f"{'page over the wire':>22} | {'reads/s before uplink binds':>28}")
for label, kb in [("25 KB (uncompressed)", 25), ("5 KB (gzip ~5x)", 5), ("served from CDN edge", 0.001)]:
    rps = reads_per_sec(kb)
    note = "  (uplink no longer the bind — CPU ~2000/s wins)" if rps > 2000 else ""
    print(f"{label:>22} | {rps:>22,.0f} reads/s{note}")

first_load_before, first_load_after = 10.8 * 1024, 929   # KB
print(f"\nfirst-load payload: {first_load_before:,.0f} KB -> {first_load_after:,.0f} KB "
      f"(~{first_load_before/first_load_after:.0f}x), and edge-served after the first global fetch")
print("Order matters: compress -> cache -> distribute. A CDN over a 10 MB bundle just serves bloat from more places.")
```

## 5. What it costs (a footnote to ch 50)

[Chapter 50](/cortex/system-design/capstones/cortex-storage-and-cost) put the infra at ~€50/month and showed the axis that actually scales is **AI tokens**, not bytes. The edge layer doesn't move that needle — and crucially it's **free**: Cloudflare's plan doesn't meter proxied bandwidth, so edge-caching the assets *removes* read egress from the home uplink at **no new line item**, while hiding the origin IP and adding DDoS protection. Brotli on the edge shrinks HTML ~15% more than the origin's gzip. Net: a strictly-better delivery story for €0 — the rare optimization with no trade-off on the cost axis. (The cost chapter's conclusion stands: **who pays for tokens** decides scalability; bytes were never the bill.)

## 6. What the edge does *not* fix

This is a **read-path** win. It's important to be precise about what it leaves untouched, because [ch 49's](/cortex/system-design/capstones/cortex-failure-thresholds) failure ladder is mostly about the *other* paths:

- **The run path is unchanged.** `/api/run` is still an M/M/c queue of **8 permits** (ch 48 §3); a CDN can't cache a code execution. The ranks-1–4 failure modes (single replica, 1 GiB OOM, go-judge RAM, the permit queue) are exactly as before.
- **Dynamic + authenticated responses stay at the origin.** `/api/*` is served `DYNAMIC` (never edge-cached) — correct, or you'd cache someone's authed payload at the edge. So the Postgres pool of 10 (rank 5) is unmoved.
- **The single replica is still rank 1.** Edge caching means a brief origin outage is *invisible to readers of already-cached assets*, but a new visitor on a cache-miss, every `/api/*` call, and every code run still need the one pod. The CDN softens the blast radius for cached reads; it doesn't add a replica.

So this chapter lifts the **capacity ceiling** ch 48 measured; it does **not** touch the **crash thresholds** ch 49 ranked. Different chapters, different resources — which is the whole point of sizing workloads separately.

## 7. Trade-offs

| Decision | Choice | Why | Cost |
|---|---|---|---|
| Where to compress | **origin (gzip)** + edge (Brotli) | shrink bytes *before* the tunnel, the binding leg | a little server CPU per response (cheap; readers are CPU-light) |
| Asset caching | **`immutable` + content hash** | repeat visits ~free; deploys bust cache via new hashes | `immutable` is only safe *because* the filename is hashed — never put it on `index.html` |
| Heavy libs | **lazy `import()`, no `manualChunks`** | landing ships only what it needs | first editor/diagram mount pays a one-time chunk fetch (Suspense fallback covers it) |
| CDN | **Cloudflare proxy, Full (strict)** | edge cache + TLS near user + free egress | a third party in the request path; Free-plan ~100 s proxy timeout (watch long SSE) |

## 8. Edge cases & failure modes

- **Stale bundle after deploy.** Can't happen by construction: `index.html` is `no-cache`, so a returning visitor always revalidates it, gets the new asset hashes, and those miss cache exactly once. Immutable + hashed is the licence; the un-hashed entry point is the escape hatch.
- **Compression + secrets (BREACH).** Compressing HTTPS responses that mix a secret with attacker-controlled, reflected input is theoretically exploitable. Cortex's compressed responses are public content or JSON; the JWT rides in a header, never reflected into a compressed body — so no vector. Worth a thought before compressing an authed, reflective endpoint.
- **The record goes grey.** If the Cloudflare proxy is flipped to "DNS only," `cortex.kakde.eu` resolves straight to the origin again and every byte crosses the tunnel — the ceiling collapses back to ~500 reads/s. `dig +short @1.1.1.1 cortex.kakde.eu` returning `84.247.143.66` (not Cloudflare IPs) is the tell.
- **CAA lockout.** Cloudflare's edge cert is Google-issued (`pki.goog`); cert-manager's origin cert is Let's Encrypt. The CAA record must permit **both**, or the edge cert silently fails to renew and the proxied site goes dark.

## 9. In the Wild

- **The change itself** — [`HttpApp` / `FileServer`](https://github.com/ani2fun/cortex) (origin compression + `Cache-Control`), `markdown/monaco.ts` (the `React.lazy` boundary), and the deleted `manualChunks` in `vite.config.mjs`. The operational version is the [Serving performance & the Cloudflare edge](/cortex/cortex-onboarding/runbooks/production/serving-performance-and-edge) runbook.
- **[`Cache-Control: immutable`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control)** — the directive that makes content-hashed assets free on repeat visits; the SPA's `no-cache` HTML is its essential pair.
- **Cloudflare** doesn't meter proxied egress — the economic reason a homelab can put a global CDN in front for €0, the missing piece of [ch 48's](/cortex/system-design/capstones/cortex-capacity-today) egress bind.

---

> **Next:** back to [49. Cortex failure thresholds](/cortex/system-design/capstones/cortex-failure-thresholds) — this chapter lifted the *capacity ceiling*; that one ranks what still *breaks* when you cross the ceilings it can't touch (the single replica and the 1 GiB heap are #1 and #2, and the edge doesn't change that).
