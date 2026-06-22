---
title: '2. Numbers every engineer should know'
summary: How long each kind of computer operation actually takes — memorise three rows and you can ballpark the rest in seconds, on demand, forever.
---

# 2. Numbers every engineer should know

## TL;DR
> Memory is fast. Disk is slow. Network is slower. *Cross-region* network is *much* slower. Every system-design conversation that does not start with these four sentences and a calibrated sense of "how much faster" is just vibes. By the end of this lesson you will be able to ballpark — to within 2× — how long any computer operation takes, on demand, without looking it up.

## 1. Motivation

In **2009**, Jeff Dean — the Google engineer who, among other things, built MapReduce, BigTable, Spanner, and TensorFlow — gave the keynote at the LADIS 2009 workshop (Big Sky, Montana) called *Designs, Lessons and Advice from Building Large Distributed Systems*. On slide 24 he put up a small table called **["Numbers everyone should know"](http://highscalability.com/numbers-everyone-should-know)**. It listed how long each kind of operation takes — from an L1 cache reference to a packet round-trip across the Atlantic.

That slide changed how a generation of engineers thought about systems. Not because the numbers were new — every CPU architect knew them — but because *application engineers* did not. People were happily writing code that did 10,000 round-trips to a database for a single page load and then wondering why the page was slow.

Jeff's table told them, *to within an order of magnitude*, exactly why.

Does the latency actually *matter* to the business? Probably — though be sceptical of the famous figures. The widely-repeated claims (Google: 400 ms slower search → 20% less traffic; Amazon: 100 ms → 1% of sales) are mostly old, hard to reproduce, and often confounded. DDIA points out, for instance, that one large study found the *fastest*-loading pages also converted poorly — because the fastest pages were often empty 404s. The honest takeaway is directional, not precise: **latency correlates with engagement, the effect is real, and the exact percentage is nobody's to quote with confidence.** That uncertainty is the point — you reach for the *numbers* precisely because the *outcomes* are noisy.

The version below is the modern (2026) update. Memorise the order-of-magnitude column. The exact nanoseconds drift; the orders of magnitude do not.

## 2. Intuition (Analogy)

The numbers feel abstract because nanoseconds and microseconds and milliseconds all sound similar. They are not. They differ by *factors of a thousand*.

The trick that makes them stick: **scale a 1-nanosecond L1 cache hit to feel like 1 second**, and rescale every other operation by the same factor. Now you are reasoning in human time — seconds, hours, days, years — and you can *feel* how much slower each layer is.

| Operation | Real time | If 1 ns = 1 second… |
|---|---|---|
| L1 cache reference | 1 ns | **1 second** |
| Branch mispredict | 3 ns | 3 seconds |
| L2 cache reference | 4 ns | 4 seconds |
| L3 cache reference | 15 ns | 15 seconds |
| Mutex lock/unlock | 17 ns | 17 seconds |
| Main memory reference | 100 ns | **1 minute 40 seconds** |
| Compress 1 KB with Snappy | 2 µs | 33 minutes |
| Send 1 KB over a 1 Gbps network | 10 µs | 2 hours 47 minutes |
| Random read from SSD | 16 µs | 4 hours 27 minutes |
| Read 1 MB sequentially from RAM | 20 µs | 5 hours 33 minutes |
| Round-trip within same datacentre | 500 µs | **5 days 19 hours** |
| Read 1 MB sequentially from SSD | 1 ms | 11 days |
| Disk seek (spinning) | 4 ms | **1 month 18 days** |
| Read 1 MB sequentially from a spinning disk | 5 ms | 2 months |
| Round-trip from California to Netherlands | 150 ms | **4 years 9 months** |

> Sources: Peter Norvig's [original interpretation](http://norvig.com/21-days.html#answers) of Jeff Dean's numbers; Aleksey Shipilëv's [Nanotrusting the nanotime](https://shipilev.net/blog/2014/nanotrusting-nanotime/) for honest nanosecond-scale measurement methodology; AWS / Cloudflare published cross-region RTT numbers. (Spinning-disk rows reflect modern 7200-RPM drives, ~4–5 ms; Jeff Dean's 2009 figures were 10 ms seek / 20 ms per MB.)

The same data on a horizontal log scale — bars span nine orders of magnitude, so the gap between the cache layers and the cross-region hop is something the eye picks up in one beat. Click any row to update the human-scale caption.

```d3 widget=latency-scaled-time
{
  "title": "Latency landscape — median operations on a log axis (1 ns ≡ 1 second on the human scale)",
  "scaleSeconds": 1.0e9,
  "items": [
    { "label": "L1 cache",                  "ns": 1,         "highlight": true },
    { "label": "Branch mispredict",         "ns": 3 },
    { "label": "L2 cache",                  "ns": 4 },
    { "label": "L3 cache",                  "ns": 15 },
    { "label": "Mutex lock/unlock",         "ns": 17 },
    { "label": "Main memory",               "ns": 100,       "highlight": true },
    { "label": "Snappy compress 1 KB",      "ns": 2000 },
    { "label": "1 KB over 1 Gbps",          "ns": 10000 },
    { "label": "Random SSD read",           "ns": 16000 },
    { "label": "Same-DC RTT",               "ns": 500000 },
    { "label": "1 MB SSD sequential",       "ns": 1000000 },
    { "label": "Disk seek (spinning)",      "ns": 4000000 },
    { "label": "Cross-region RTT (CA↔NL)",  "ns": 150000000, "highlight": true }
  ]
}
```

Memorise just *three* of those rows and the rest become deducible:

> 1 ns ≈ **L1 cache** (1 second on the human scale)
> 100 ns ≈ **main memory** (~minute on the human scale)
> 100 ms ≈ **cross-continent network** (~3–5 years on the human scale)

That is the whole curriculum of this lesson, in three lines. Everything else is interpolation.

## 3. Formal Definition

Three words get used interchangeably and shouldn't be. DDIA draws the lines precisely, and the rest of the track depends on getting them right:

- **Response time** is what the *client* sees end-to-end. It is `service time + queueing delay + network latency` — everything between "I asked" and "I have the answer," including time the request spent doing nothing.
- **Latency** is narrower: it is the catch-all for the time a request is *latent* — waiting, in flight, **not** being actively worked on. Network latency (the packet's travel time) is the kind you'll quote most. The numbers in the table below are latencies in this sense.
- **Throughput** is a different axis entirely: requests per second, or bytes per second, that the whole system processes. Latency is "how long for *one*"; throughput is "how many *per second*."

The trap is treating these as one number. A single operation can have a 1 ms latency and the *service* still deliver 100,000 requests/second — because thousands are in flight at once. We make that relationship exact with Little's Law in [Lesson 5](/cortex/system-design/foundations/latency-throughput-usl). And the two axes are coupled by **queueing**: as throughput climbs toward a server's capacity, requests start waiting behind each other and response time spikes — gently at first, then off a cliff near saturation. (A motorway at 30% occupancy flows at the speed limit; at 95% it's a stop-start crawl, even though no individual car got slower. Same physics.)

**One number is never enough — use a distribution.** Response time varies request to request even for *identical* requests: a GC pause, a TCP retransmit, a context switch, a cold page fault. So we report **percentiles**, not the mean. Sort the responses fastest-to-slowest:

- **p50 (median):** the halfway point — your *typical* user's wait. "p50 = 200 ms" means half of requests finished faster, half slower.
- **p95 / p99 / p999:** the **tail**. "p99 = 1 s" means 1 in 100 requests took at least a second. These directly shape how the service *feels* under load.

Why not just average? Because **the mean hides the tail**. One 10-second request among ninety-nine 100 ms requests pulls the mean to ~200 ms — a figure *no actual user experienced*. The mean is fine for capacity math (it predicts throughput); it is misleading for "how slow is slow." The deeper consequence — that one slow component in a fan-out drags the whole request down — gets its own treatment in §7 (head-of-line blocking and tail amplification).

The numbers in the table are **medians (p50)** for *typical* operations on *typical* modern hardware. Specific values vary by:

- **CPU** — server-class chips have larger caches and faster memory.
- **Storage class** — NVMe is 5–10× faster than SATA SSD; spinning disk is 10–100× slower than SSD; tape is *seconds* to first byte.
- **Network conditions** — same-rack RTT can be 50 µs; same-DC ~500 µs; same-region ~1 ms; cross-region 30–250 ms.
- **Operation size** — bigger payloads dominate by *bandwidth*, not latency. The latency table assumes ≤ 1 KB; bandwidth gets its own treatment below.

Two related numbers you will need for the rest of the track:

| Quantity | Modern value (2026) | Notes |
|---|---|---|
| Datacentre 10 Gbps NIC bandwidth | ~1.25 GB/s | "How much can leave one server per second?" |
| Modern NVMe Gen5 SSD sequential read | ~14 GB/s | A single disk can saturate ten 10 G NICs |
| Modern DDR5 memory bandwidth (per channel) | ~50 GB/s | Per 64-bit DIMM/channel (DDR5-6400 ≈ 51 GB/s); ~25 GB/s per 32-bit sub-channel. RAM is ~3–4× faster than NVMe Gen5 |
| Inter-AZ (intra-region) per-instance bandwidth | ~5–10 Gbps single-flow | Internal AWS / GCP backbone; ~5 Gbps per flow, ~10 Gbps in a cluster placement group, 100 Gbps spines exist. Cross-*region* is a separate, WAN-bound scope. |

The bandwidth column matters because **a 1 GB transfer** does not take 10 µs (the latency of one packet) — it takes ~800 ms over a 10 Gbps link, dominated by bandwidth, not latency. Throughput = concurrency ÷ latency (equivalently, Little's Law: concurrency = latency × throughput). We will use this exact relationship in [Lesson 5 — Little's Law](/cortex/system-design/foundations/latency-throughput-usl).

One last set of numbers belongs in muscle memory: **availability, counted in "nines."** SLOs are written as a percentage of successful requests (or uptime), and the only intuitive way to read them is as *downtime budget per year*. Each extra nine is ~10× less downtime — and exponentially more expensive to buy.

| Availability | "Nines" | Downtime per year | Downtime per day |
|---|---|---|---|
| 99% | two nines | ~3.65 days | ~14.4 min |
| 99.9% | three nines | ~8.77 hours | ~1.44 min |
| 99.99% | four nines | ~52.6 min | ~8.6 s |
| 99.999% | five nines | ~5.26 min | ~0.86 s |

Major cloud providers set their compute SLAs at **99.9% or better**. The jump from three nines to five is not 2× the work — it is the difference between "one person can reboot it during business hours" and "no human can be in the loop at all." Note this is the *same statistical idea* as the percentiles above: 99.9% availability is just "the p999 request still succeeds."

## 4. Worked Example

A junior engineer says:

> "I added a feature that sends one HTTP call to a microservice for each item in the user's shopping cart, then displays the page. It is slow. Why?"

You ask: how many items? They say: ten. You ask: where is the microservice? They say: same region but different cluster.

You can answer **the why** without running anything. Each round-trip across the cluster is roughly 2 ms (1 ms each way + serialisation + connection setup if not pooled). Ten of them in series is **20 ms**. That alone is not bad.

But you also know the connection pool is probably empty (cold lambdas, anyone?), so the first call also pays a TLS handshake. A handshake costs round-trips × RTT (1 RTT for TLS 1.3, 2 for TLS 1.2); at ~1 ms in-region RTT that is only a few extra ms — call it ~5 ms. So the *first* page load is **~25 ms** of pure waiting. (A ~100 ms handshake would imply a cross-continent RTT — at which point every per-item call would be ~50 ms, not 2 ms.)

You ask: in production, how often does the cart have 50 items? They say: about 20% of the time. Now the math is:

| Cart size | Round-trips | Latency budget | User experience |
|---|---|---|---|
| 1 item | 1 | ~2 ms | Snappy |
| 10 items | 10 | ~20 ms | Imperceptible |
| 50 items | 50 | ~100 ms | Just noticeable |
| 200 items (power user) | 200 | **~400 ms** | "Why is the cart slow?" |

The fix is one of the standard four:

1. **Batch:** one HTTP call with 50 IDs in the body, not 50 calls.
2. **Parallel:** fire all calls concurrently; latency drops to ≈ slowest single call.
3. **Cache:** if items are read-mostly, hit a 100-ns memory cache instead of a 2-ms network call.
4. **Avoid:** does the page actually need every detail of every item, or could you defer the rare ones?

You did all of this without writing code, without running benchmarks, and without consulting the microservice team. You did it because **you knew the numbers**.

That is the entire point of this lesson.

## 5. Build It

Calibrate yourself on your own laptop. Run this once and *write down* the actual numbers you see — they will become your reference for the rest of the track.

```python run
import time
import statistics

# We measure each operation many times to defeat noise from interrupts, GC, etc.

def measure(label, fn, repeats=10_000):
    # Warm up — fill caches, JIT (Pyodide / CPython has none, but doesn't hurt)
    for _ in range(1_000):
        fn()
    samples = []
    for _ in range(repeats):
        t0 = time.perf_counter_ns()
        fn()
        samples.append(time.perf_counter_ns() - t0)
    median_ns = statistics.median(samples)
    p99_ns    = statistics.quantiles(samples, n=100)[98]   # 99th percentile
    print(f"{label:<32} median={median_ns:>8} ns   p99={p99_ns:>8} ns")

# 1. Trivial work — basically the loop overhead. Set the floor.
measure("noop (loop floor)", lambda: None)

# 2. L1-ish: tiny tuple unpack
small = (1, 2, 3)
measure("tuple unpack",        lambda: (small[0], small[1], small[2]))

# 3. Memory access via Python list — bigger than L1, hits L2/L3
big = list(range(1_000_000))
measure("list index (1 M list)", lambda: big[500_000])

# 4. Hash lookup — small dict
d = {i: i for i in range(1000)}
measure("dict lookup",          lambda: d[500])

# 5. Sequential scan over 1 MB-ish data — bandwidth-bound
buf = bytes(1_000_000)
measure("scan 1 MB bytes",      lambda: sum(buf), repeats=200)

# 6. Print the takeaways
print("\nTakeaways:")
print(f"- A noop loop iteration on this machine is ~50–500 ns; that is your floor.")
print(f"- A 1-MB scan is bandwidth-bound; expect ~20 µs per MB on RAM, ~1 ms on SSD, ~5 ms on spinning disk.")
print(f"- p99 / median ratio reveals tail latency. Ratios > 5x mean GC/preemption is a thing.")
```

**Now break it.** Increase the list size to `100_000_000`. Notice the median jumps. Why? *Because you stopped fitting in L3.* You just *measured* the cache hierarchy on your own laptop. Senior engineers do this regularly — not because they need the exact numbers but because they need to *see* the cliff between cache and main memory, and between memory and disk, in their own data.

> **A measurement gotcha that bites everyone:** the script computes p99 from raw samples, which is correct here. But once you collect percentiles in production, never *average* them — the mean of two p99s is not the combined p99, and "average p99 across our fleet" is mathematically meaningless. To combine latency data across machines or time windows, add the **histograms**, then read the percentile off the merged histogram. (This is why tools like HdrHistogram, t-digest, and DDSketch exist.)

For network-tier numbers (which Pyodide cannot measure from inside the browser), use this from a real terminal:

```bash
# Same-region cloud RTT — adjust hostname to your nearest cloud region
ping -c 20 cloudflare.com | tail -3

# Cross-continent RTT — pick a host on the other side of the world
ping -c 20 www.scaleway.com | tail -3

# Disk write bandwidth (Linux/macOS). `conv=fdatasync` forces the kernel to
# wait for the data to hit the disk before reporting completion — without it
# you measure the OS page cache, which gives suspiciously fast numbers.
dd if=/dev/zero of=/tmp/bench bs=1M count=1024 conv=fdatasync status=progress 2>&1 | tail -2
```

Write down the three numbers you get. You will reference them every time anyone asks "is this design feasible?" for the rest of your career.

## 6. Trade-offs & Complexity

| Layer | Latency | Throughput per device | Cost per GB† | When to use |
|---|---|---|---|---|
| **CPU register** | ~0.3 ns | — | — | Hot loop variables only |
| **L1 cache** | 1 ns | ~200 GB/s | — | The compiler chose this; you do not |
| **L2/L3 cache** | 4–15 ns | ~60–80 GB/s | — | Tight inner loops |
| **DRAM** | 100 ns | ~50 GB/s | ~$5 / GB (capex) | Hot data, working set |
| **NVMe SSD** | 16–100 µs | ~14 GB/s (Gen5) | $0.10 / GB-mo | Warm data; durable; primary DB |
| **Network (same DC)** | ~500 µs | ~1 GB/s per NIC | — | Microservice calls, cache fetches |
| **Spinning disk** | ~4 ms | ~150 MB/s | ~$0.02 / GB (capex) | Cold storage, archives |
| **Network (cross-region)** | 30–250 ms | ~1 GB/s | $0.02 / GB transferred | Disaster recovery, replication |
| **Object storage (S3)** | ~100–200 ms | virtually unbounded with parallelism | $0.023 / GB-mo | Blobs, backups, data lake |

> † Hardware rows (DRAM, spinning disk) list one-time *purchase* price per GB; cloud rows (NVMe, S3, network) list *monthly* rent or per-GB transfer.

**The rule of the table:** every row down is roughly 10–100× *slower* than the row above, and roughly 10–100× *cheaper per GB*. Picking the right row is mostly about asking "what is my access pattern?" and matching it.

A small table is also worth memorising: the **ratios** of bandwidth between RAM, SSD, and network at modern speeds. They tell you *which one bottlenecks first* in any pipeline.

| Pair | Ratio (modern hardware, ballpark) |
|---|---|
| RAM vs NVMe SSD | RAM is ~3–4× faster (DDR5 channel vs NVMe Gen5) |
| NVMe SSD vs 10 G NIC | SSD is ~6× faster |
| 10 G NIC vs spinning disk | NIC is ~7× faster |
| Same-DC RTT vs cross-region RTT | Cross-region is **~300× slower** |

That last one is why "just put the database in the other region" is almost never the right answer to a latency problem.

## 7. Edge Cases & Failure Modes

- **Confusing latency with bandwidth.** A 1-ms RTT does not mean you can transfer 1 GB in 1 ms. It means *the first byte* shows up after 1 ms; the rest is bandwidth-bound. *(See: every "why is my big file upload slow over fast network?" thread on the internet.)*
- **Quoting medians when only tails matter.** This is the single most common latency mistake, and the math is brutal. Say each backend call is independently slow 1% of the time (its p99). A page that fans out to 100 of those calls is slow whenever *any one* of them is slow, and `P(at least one slow) = 1 − 0.99¹⁰⁰ ≈ 63%`. So a *per-call* p99 of "only 1%" becomes a *per-page* experience of "almost two requests in three feel the tail." Worse, parallelising the calls does not save you — the page still waits for the **slowest** of the 100, so the user effectively *always* lives at the tail of the call distribution. DDIA names this **tail-latency amplification**, and it is exactly why Amazon specifies internal-service SLOs at the **99.9th** percentile even though that affects only 1 in 1,000 *calls*: by the time a request fans out, the rare call is the common experience. (There is a poignant twist Amazon noticed: the customers who hit the slow path are disproportionately the *most valuable* ones — they have the most data on their account, so their queries are the heaviest.) [The Tail at Scale (Dean & Barroso, 2013)](https://research.google/pubs/the-tail-at-scale/) is required reading.
- **Optimising L1 cache when network is the bottleneck.** A 100 ns memory access embedded inside a 100 ms cross-region call is *one millionth* of the latency. Senior engineers triage where the time actually goes *before* optimising.
- **Forgetting that even a "fast" SSD is still ~1,000× slower than RAM (and a spinning-disk seek ~100,000×).** This is why caching exists. This is also why "we made the database faster" hits a wall — the disk is still the disk.
- **Trusting cloud "best-case" latencies.** Vendors quote *un-loaded* RTT. Loaded systems queue. A "1 ms" service averages ~10 ms at 90% CPU utilisation (M/M/1: response = service ÷ (1 − utilisation)), and its p99 tail can hit ~50 ms; a 50 ms *typical* figure needs ~98% utilisation. The lesson: the published number is the floor, not the forecast. We will quantify this in [Lesson 5 — Little's Law](/cortex/system-design/foundations/latency-throughput-usl).
- **Head-of-line blocking.** A server processes only a handful of things at once (bounded by cores). One genuinely slow request can wedge the queue, so *fast* requests stacked behind it inherit the wait — even though their own service time was tiny. This is why you must measure response time on the **client** side: the queueing delay is invisible from inside the server, which only sees its own fast service time. (Think of one shopper paying by cheque at the only open till: everyone behind them is "slow" through no fault of their own.)
- **Retry storms / metastable failure.** When a system is already near overload, timeouts trigger retries, retries add load, and the extra load causes more timeouts — a vicious cycle that can keep a service down *even after the original spike passes*, until someone reboots it. The defences are all about *not* piling on: exponential backoff with jitter, circuit breakers, and server-side load shedding. A latency budget that ignores what happens past the saturation knee is a budget for an outage.
- **Comparing wall-clock from different machines.** Same code, different CPU, can be 10× different. Always measure on the hardware you will run on.
- **Forgetting cold caches.** First-request latency is dominated by everything that *should* have been hot but was not — DNS, TLS handshake, JIT, page cache, CDN warm-up. Every system has a "first 30 seconds" pathology. Production load tests should always include cold-start measurement.

## 8. Practice

> **Exercise 1 — Estimate without running.**
> A user uploads a 5 MB image. Your service stores it in S3 (~100 ms first-byte latency, ~100 MB/s bandwidth from the same region) and writes a row in Postgres. Estimate the total time for one upload, *without* running anything. Then identify *which* component is the bottleneck.
>
> <details>
> <summary>Solution</summary>
>
> The 5 MB upload to S3 takes 5 MB / 100 MB/s = **50 ms** of transfer time, *plus* the ~100 ms first-byte latency to issue and start the request = **~150 ms total** for S3.
>
> The Postgres write is a same-region RTT plus serialisation plus fsync — call it **5 ms**.
>
> If done **sequentially**, the user waits ~155 ms. The bottleneck is S3 — and at this object size the first-byte *latency* (~100 ms) actually exceeds the *transfer* (~50 ms), so a faster pipe alone barely helps; the object has to grow to tens of MB before bandwidth dominates.
>
> The 1% senior insight: parallelise the Postgres write with a placeholder row and update it on S3 success — get the user response down to ~5 ms while the upload completes in the background.
>
> </details>

> **Exercise 2 — Spot the latency lie.**
> A vendor claims "sub-millisecond writes". You read the small print: this is "single-region" with "fire-and-forget" writes. Translate that to honest English. What is the *durability* implication? What does the latency look like for *durable cross-region* writes on the same product?
>
> <details>
> <summary>Solution</summary>
>
> "Sub-millisecond fire-and-forget" means: the write returns success *as soon as the closest in-region node has it in memory*, before durability is confirmed and before any other replica has seen it.
>
> If that node crashes within milliseconds (entirely possible), the write is lost — even though the client got a "success".
>
> An honest *durable* write goes to disk *and* a quorum of replicas. Same-region durable writes are typically 5–15 ms. *Cross-region* durable writes are bounded by the cross-region RTT — 30–250 ms — and that is a hard physics limit, not an engineering one.
>
> </details>

> **Exercise 3 — Build the table for your own machine.**
> Run the calibration script in section 5. Add a row for "git clone of a 100 MB repo over your home internet". Compare to your in-region datacentre numbers. By what factor is your home internet slower? *(This is why CDNs exist.)*

> **Exercise 4 — The cross-region cost reflex.**
> A request inside your service touches the L1 cache once (1 ns), reads from NVMe SSD once (16 µs), and makes a single cross-region call (150 ms). What fraction of the total latency is the cross-region hop? Round to one decimal.
>
> <details>
> <summary>Solution</summary>
>
> Total latency: 1 ns + 16,000 ns + 150,000,000 ns = **150,016,001 ns**.
>
> Cross-region share: 150,000,000 / 150,016,001 = **99.989%**.
>
> The cross-region hop is essentially the *entire* latency budget. The other two operations contribute about *one part in ten thousand* — round-off noise from the user's perspective.
>
> The senior reflex this teaches: when you see *any* cross-region call in a hot path, that hop is the only thing that matters until eliminated. Re-arranging the local steps to be 2× faster gives the user **zero perceptible benefit**. Pinning the data into the same region as the caller gives the user a 1,000× speedup. Match the optimisation to where the time actually is.
>
> This is the same lesson Stack Overflow keeps stress-testing in production: their stack is fast because *no request crosses the public internet a second time*, not because they tuned their Postgres knobs.
>
> </details>

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Jeff Dean — Designs, Lessons and Advice from Building Large Distributed Systems](https://www.cs.cornell.edu/projects/ladis2009/talks/dean-keynote-ladis2009.pdf)** (2009, LADIS keynote PDF) — the talk that launched the table; slide 24 is the original *Numbers Everyone Should Know*.

- **[Peter Norvig — Teach Yourself Programming in Ten Years (Answers section)](http://norvig.com/21-days.html#answers)** — a closely related public table of the same numbers, with Norvig's own annotations (an independent origin of these figures, not a reproduction of Dean's).

- **[Aleksey Shipilëv — Nanotrusting the Nanotime](https://shipilev.net/blog/2014/nanotrusting-nanotime/)** (2014) — honest methodology for measuring nanosecond-scale latency (the resolution and overhead of `System.nanoTime()` itself). Especially useful for JVM-ecosystem engineers; see also his [JVM Anatomy Quarks](https://shipilev.net/jvm/anatomy-quarks/) series.

- **[Cloudflare — Network performance update: Birthday Week 2024](https://blog.cloudflare.com/network-performance-update-birthday-week-2024/)** (2024) — real-world p95 end-user-to-Cloudflare TCP connect times across ISPs worldwide. A ground-truth check on last-mile RTT.

---

**Next:** the *combinatorial* skill that turns these numbers into design decisions — back-of-envelope estimation. By the end of [Lesson 3](/cortex/system-design/foundations/back-of-envelope-estimation), you will be able to estimate the QPS, storage, and bandwidth of any system in 5 minutes, with nothing but multiplication. → [Lesson 3 — Back-of-envelope estimation](/cortex/system-design/foundations/back-of-envelope-estimation)
