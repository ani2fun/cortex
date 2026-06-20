---
title: '3. Back-of-envelope estimation'
summary: Multiply five numbers, double the result, design for the doubled number — the drill that every senior engineer does before any code is written.
---

# 3. Back-of-envelope estimation

## TL;DR
> Before you write a single line of code, you should be able to estimate — *to within an order of magnitude* — how many requests per second your system will see, how much storage it needs, and how much bandwidth it consumes. This is multiplication, not magic. The trick is knowing which five numbers to pull out of the air, and which constants to multiply them by. By the end of this lesson, you will do Twitter-scale and YouTube-scale on demand.

## 1. Motivation

In **2010**, Twitter's site was famously held together with bailing wire. The [Fail Whale](https://en.wikipedia.org/wiki/Twitter#Outages) had its own Wikipedia entry. Every World Cup goal generated a traffic spike that took the site down. A small infrastructure team eventually learned to estimate the next spike before it arrived — and reroute around it — by doing exactly the kind of math we are about to learn.

When the 2014 World Cup in Brazil peaked at **580,166 tweets per minute — about 9,700 tweets/sec — at Germany's fifth goal** in the 7–1 demolition ([the most-tweeted sporting event to that point](https://variety.com/2014/digital/news/world-cup-germany-brazil-match-scores-twitter-records-for-sports-chatter-1201259646/)), Twitter stayed up. The discipline that buys you that headroom is exactly the math we are about to learn: *viewers × tweets-per-viewer × goal-event amplification = an upper bound on QPS at the moment of a goal* — then provision a comfortable multiple of that estimate and keep a cache-shedding path for the inevitable underestimate.

That is the entire skill of this lesson: **multiply five numbers, double the result, design for the doubled number**.

## 2. Intuition (Analogy)

Think of estimation like **packing a backpack for a 5-day hike**.

You do not weigh every item to the gram. You estimate: "shirt ≈ 200 g, food ≈ 600 g/day × 5 = 3 kg, water ≈ 2 kg, sleeping bag ≈ 1.5 kg, miscellaneous ≈ 1 kg, total ≈ 8 kg, plus 50% slack for things I forgot ≈ 12 kg." That number tells you whether you need a 30-litre or 60-litre pack — *long before* you have the actual gear.

Designing systems works the same way. You estimate: "1 M users × 5 requests/day each ≈ 5 M req/day ≈ 60 req/sec average × 5 spike multiplier = 300 req/sec peak". That number tells you whether you need a single web server or a fleet — *long before* you write any code.

The mistake is wanting precision. **The estimate is wrong by definition.** That is fine — it is wrong on the right *order of magnitude*, which is enough to pick a class of solution.

## 3. Formal Definition

A back-of-envelope estimate (BOTE) is a calculation that:

1. **Takes ≤ 5 minutes**, fits on the back of a real envelope.
2. **Uses round numbers** — `1 M`, `100 ms`, `1 KB`. Never `1,247,832` or `99 ms`.
3. **Yields an answer that is correct to within a factor of 10**, ideally 3.
4. **Drives a decision** — "single server vs fleet", "one DB vs sharded", "cache vs no cache".

Three categories of estimate dominate every system-design conversation:

| Category | Question it answers | Driving units |
|---|---|---|
| **QPS** (Queries / sec) | "How busy is the read/write path?" | requests/sec, peak factor |
| **Storage** | "How much data accumulates over time?" | bytes/event × events/year |
| **Bandwidth** | "How much network capacity do we need?" | bytes/event × events/sec |

### The five universal numbers

Memorise these. Every BOTE you ever do uses at least three of them.

| Number | Value | Where it shows up |
|---|---|---|
| **Seconds per day** | ~100,000 (actually 86,400 — round up) | Convert daily counts to QPS |
| **Seconds per year** | ~30 M (actually 31.5 M — round) | Convert annual events to QPS / storage |
| **Average daily-active-user (DAU) ratio** | ~10–20% of registered users | Translate "how many users" to "how many active users" |
| **Peak-to-average ratio** | ~3× for consumer products, ~10× for entertainment, ~100× for "viral moments" | Convert average QPS to peak QPS |
| **Read-to-write ratio** | ~10–100× for content systems, ~1× for messaging | Decide where the bottleneck is |

### Storage units, the right way

Engineers reach for "1 GB ≈ 1 billion bytes" so often that they forget the difference between KB and KiB. For BOTE you do not need to. Round to the nearest power of ten and move on.

| Unit | Bytes | What it holds |
|---|---|---|
| 1 KB | 10³ | A short text message; an HTTP header |
| 1 MB | 10⁶ | A photo, a long blog post |
| 1 GB | 10⁹ | An hour of compressed video; a small book library |
| 1 TB | 10¹² | A year of detailed logs from a mid-sized service |
| 1 PB | 10¹⁵ | The total user data of a 100-million-user social product |

## 4. Worked Example

Let's estimate the QPS, storage, and bandwidth of **Twitter**, twice — once with rough public numbers, then again with realistic peaks.

### The setup

- **MAU (monthly active users):** 400 million.
- **DAU (daily active users):** ~50% of MAU = **200 million** (engagement-heavy social runs high — see the DAU/MAU rule of thumb in §6).
- **Tweets per active user per day:** 2 (rough public number).
- **Reads per active user per day:** 50 (people read way more than they post — this is the read-to-write ratio).
- **Average tweet size:** 280 chars + metadata ≈ **1 KB** (yes, even with avatars and timestamps).

### Calculation 1 — Write QPS

```
Tweets/day  = 200 M × 2 = 400 M
Tweets/sec  = 400 M / 100,000 ≈ 4,000 tweets/sec (average)
Peak (3×)  ≈ 12,000 tweets/sec
Spike (10× during World Cup goal) ≈ 40,000 tweets/sec
```

Sanity check: Twitter [publicly reported](https://blog.twitter.com/engineering/en_us/a/2014/streaming-mapreduce-with-summingbird) handling **143,199 tweets/sec** during the *Castle in the Sky* TV moment in 2013. We are off by ~3.5×. That is **a perfect BOTE result** — order-of-magnitude correct.

### Calculation 2 — Read QPS

```
Reads/day  = 200 M × 50 = 10 B
Reads/sec  = 10 B / 100,000 ≈ 100,000 reads/sec (average)
Peak (3×) ≈ 300,000 reads/sec
```

The read path is **25× the write path**. That is the *single most important number* for designing this system. It tells you:

- Reads must be cached.
- Writes can be slow; reads cannot.
- The architecture is read-optimised.

This is exactly why Twitter's core design uses *fan-out on write* — pre-compute every follower's timeline at write time so reads are O(1) cache hits. We will design that exact pipeline in [Capstone 38](/cortex/system-design/capstones/news-feed).

### Calculation 3 — Storage growth

```
Tweets/year       = 400 M × 365 ≈ 150 B
Bytes per tweet   ≈ 1 KB
New data/year     = 150 B × 1 KB ≈ 150 TB
With replication (3×) ≈ 450 TB / year
With indexes / search / analytics duplication (~3× original) ≈ 1.4 PB / year
```

Sanity check: Twitter's Manhattan team [reported](https://blog.twitter.com/engineering/en_us/a/2016/observability-at-twitter-technical-overview-part-i) the order of "hundreds of TB of new tweet data per year" plus much more in derived stores. Order-of-magnitude correct.

### Calculation 4 — Bandwidth (egress)

```
Reads/sec (peak) × bytes per read ≈ 300,000 × 1 KB = 300 MB/s = 2.4 Gbps
```

Plus images and video (which dwarf text). For full media-attached reads, multiply by ~50 for an estimated **120 Gbps egress at peak**. Twitter operates this through CDNs — never hitting their core for image bytes.

> **Friction prompt — before reading on:**
> Try the same five-step calculation for **WhatsApp**: 2 B users, 60 B messages per day (publicly stated). Without scrolling, what is the message QPS? What does it imply for the architecture compared to Twitter?
>
> *(Hint: the read-to-write ratio is ~1, not 25. That changes everything.)*

<details>
<summary><strong>Solution</strong></summary>

Messages/sec ≈ 60 B / 100,000 = **600,000 msgs/sec average**, peak ~1.8 M/sec.

Read/write ≈ 1× (every sent message is read once), so the architecture is **write-optimised** — the opposite of Twitter. WhatsApp uses [an aggressively de-normalised "delivered to one device" log](https://www.youtube.com/watch?v=yJDV-DJo-ng) optimised for write-once-read-once, which would be a terrible fit for Twitter's read-heavy timeline.

Same math, completely different architecture. The numbers told you which.
</details>

## 5. Build It

Use this template for any new system. Five lines of arithmetic, in this order, every time.

```python run
# A reusable BOTE template. Plug in the five numbers; everything else falls out.
# Round generously — you are doing back-of-envelope, not accounting.

# --- Inputs (the only numbers you need to look up or guess) ---
DAU                 = 200_000_000   # daily active users
ACTIONS_PER_USER_PER_DAY = {
    "writes": 2,                     # e.g. tweets per user per day
    "reads":  50,                    # e.g. timeline views per user per day
}
BYTES_PER_WRITE     = 1_000          # ~1 KB per write
PEAK_FACTOR         = 3              # peak QPS / average QPS
REPLICATION_FACTOR  = 3              # 3 copies for durability

# --- Constants ---
SECONDS_PER_DAY = 100_000   # 86_400 rounded up
DAYS_PER_YEAR   = 365

def fmt(n):
    # Return a value with a human-friendly unit suffix.
    for unit, threshold in [("T", 1e12), ("B", 1e9), ("M", 1e6), ("K", 1e3)]:
        if n >= threshold:
            return f"{n / threshold:.1f}{unit}"
    return f"{n:.1f}"

# --- QPS ---
write_qps_avg  = DAU * ACTIONS_PER_USER_PER_DAY["writes"] / SECONDS_PER_DAY
read_qps_avg   = DAU * ACTIONS_PER_USER_PER_DAY["reads"]  / SECONDS_PER_DAY
write_qps_peak = write_qps_avg * PEAK_FACTOR
read_qps_peak  = read_qps_avg  * PEAK_FACTOR

print(f"Write QPS — avg: {fmt(write_qps_avg)}, peak: {fmt(write_qps_peak)}")
print(f"Read  QPS — avg: {fmt(read_qps_avg)}, peak: {fmt(read_qps_peak)}")
print(f"Read/write ratio: {ACTIONS_PER_USER_PER_DAY['reads'] / ACTIONS_PER_USER_PER_DAY['writes']:.0f}×")

# --- Storage growth (per year, including replication) ---
writes_per_year = DAU * ACTIONS_PER_USER_PER_DAY["writes"] * DAYS_PER_YEAR
bytes_per_year  = writes_per_year * BYTES_PER_WRITE * REPLICATION_FACTOR
print(f"Storage growth/year: {fmt(bytes_per_year)} bytes")

# --- Bandwidth (egress at peak read load) ---
egress_peak_bytes_per_sec = read_qps_peak * BYTES_PER_WRITE
print(f"Egress at peak: {fmt(egress_peak_bytes_per_sec)} bytes/s "
      f"= {fmt(egress_peak_bytes_per_sec * 8)} bits/s")
```

Run it. Now **break it** — change `ACTIONS_PER_USER_PER_DAY["reads"]` to `1` (a write-heavy system like WhatsApp). Notice how the read/write ratio collapses to 1× — that single number flips the architectural answer entirely.

Now make `BYTES_PER_WRITE` = `5_000_000` (a video upload service like YouTube). Notice the bandwidth number explodes from gigabits to **terabits** per second. That number alone tells you why YouTube cannot serve from a single region.

This is the BOTE habit. Five numbers, three derived totals, one architectural decision. Five minutes.

The same template, made interactive — pick a preset and watch the four output numbers move; type your own and feel the read/write-ratio flip. *The estimates below use rounded, illustrative public figures, not real-time stats; that is the BOTE point.*

```d3 widget=estimation-calculator
{
  "title": "Pick a preset or punch in your own numbers — the outputs update on every keystroke",
  "peakFactor": 3,
  "replicationFactor": 3,
  "presets": [
    { "name": "Twitter/X (illustrative)",     "dau": 200000000,  "writesPerUser": 2,  "readsPerUser": 50, "bytesPerWrite": 1000 },
    { "name": "YouTube views (illustrative)", "dau": 750000000,  "writesPerUser": 0,  "readsPerUser": 10, "bytesPerWrite": 5000000 },
    { "name": "WhatsApp (illustrative)",      "dau": 2000000000, "writesPerUser": 30, "readsPerUser": 30, "bytesPerWrite": 250 },
    { "name": "Personal book tracker (10 readers)", "dau": 10,    "writesPerUser": 1,  "readsPerUser": 10, "bytesPerWrite": 500 }
  ]
}
```

## 6. Trade-offs & Complexity

There is no algorithmic complexity in BOTE — it is multiplication. But there *are* trade-offs in how aggressive your assumptions are.

| Choice | Conservative (over-estimate) | Aggressive (under-estimate) | Right answer |
|---|---|---|---|
| **Peak-to-average ratio** | 10× | 1.2× | **3× for consumer apps; 10× for entertainment; 30× for "TV moment" services** |
| **DAU/MAU ratio** | 50% | 5% | **15–25% for most consumer apps; ~40–50% for engagement-heavy social; up to 60% for messaging** |
| **Replication overhead** | 5× | 1× | **3× for durable; 1× for cache-only** |
| **Slack** ("safety margin") | 5× | 1× | **2× for compute; 1.5× for storage** (cheap to add) |

The instinct to be precise is wrong. The instinct to slap "× 10 for safety" on every number is also wrong — you will provision a ten-million-dollar fleet for a thousand-dollar problem. Land in the middle: **double** the estimate, not multiply by ten.

## 7. Edge Cases & Failure Modes

- **Inflating the user count.** "If we have 1 billion users…". You do not. You will not. Estimate what is realistic in 12–18 months. The Stack Overflow architecture story (Lesson 1) illustrates this perfectly.
- **Forgetting metadata.** A "tweet" is not 280 bytes. It is 280 chars *plus* a 64-bit ID, an author ID, a timestamp, geolocation, language, source, hashtags array, mention array, attachment URLs, retweet status, like-count cache, etc. — **easily 1 KB**, often 4 KB. The same goes for "messages", "events", "rows".
- **Forgetting indexes.** Storage is not just user data. Every secondary index, every search index, every materialised view, every analytics aggregate is **another full copy** of the data, often partially. The 3× "indexes / derived" multiplier is *generous on the low end*.
- **Conflating write QPS with row growth.** Edits, deletes, and revisions all count toward write QPS but reduce or stay-flat for storage growth. Likes, retweets, saves all count as writes but add ~50 bytes each, not 1 KB. Be specific.
- **Ignoring multi-region replication overhead.** A "1 KB write" replicated to 3 regions is *3 KB of cross-region bandwidth*, dominated by the cross-region cost in dollars. We will quantify this in [Lesson 11 — Replication](/cortex/system-design/building-blocks/replication).
- **Estimating averages and shipping for them.** A QPS estimate is the *integral over time*. Real traffic has spikes. Provision for the **99th-percentile minute**, not the daily average.
- **Trusting your peak factor.** "We expect 3× peaks". Then a celebrity tweets your URL and traffic is **300×** for 90 seconds. The right design has a **shedding strategy** (rate limit, queue, serve from cache) for the case where the estimate is wrong.

## 8. Practice

> **Exercise 1 — Estimate YouTube.**
> Public numbers: ~2.5 billion logged-in monthly users; ~500 hours of video uploaded per minute; average view session is ~15 minutes; average video is encoded into ~5 quality tiers. Estimate (a) write QPS in video-uploads/sec, (b) read QPS in video-views/sec, (c) bytes-per-day of new encoded video, (d) peak egress bandwidth.
>
> <details>
> <summary>Solution sketch</summary>
>
> (a) **Uploads:** 500 hours/min × 60 min ≈ 30,000 video-hours/day × 5 quality tiers = ~150,000 video-hours/day = ~5,200 video-hours/sec on average … but uploads are batched, so write QPS in *files* is much smaller. Better: at ~500 MB per video-hour and 5 tiers, **~75 GB/min of new encoded data**, or ~1.25 GB/sec.
>
> (b) **Views:** 2.5 B users × ~30% DAU × ~10 videos/day average = ~7.5 B views/day = ~75,000 views/sec average. Peak ~3× = **~225,000 views/sec**.
>
> (c) New encoded video/day ≈ 75 GB/min × 1,440 min/day ≈ **108 TB/day**. With original-quality archives and metadata, double it: **~200 TB/day**.
>
> (d) Egress: 225,000 views/sec × ~3 MB/sec average bitrate per stream = **~675 GB/sec ≈ 5.4 Tbps** sustained. Compare to YouTube's [actual reported](https://blog.youtube/news-and-events/youtube-by-the-numbers/) ~1 billion hours of video watched per day → ~10 Tbps. Order-of-magnitude correct.
>
> </details>

> **Exercise 2 — Pick the architecture from the numbers.**
> Two services land on your desk: **Service A** has a read/write ratio of 1×, average write size 1 KB, peak 100k QPS. **Service B** has a read/write ratio of 1000×, average write size 1 KB, peak 100k QPS. For each, identify (a) where the bottleneck will be, (b) whether you would put a cache in front, and (c) whether you would shard the database, all *purely from the numbers*.
>
> <details>
> <summary>Solution sketch</summary>
>
> **Service A** (write-heavy, e.g. messaging): bottleneck is the *write path*. A cache helps very little — every read is a one-time event. Sharding the database is essential to absorb 100k writes/sec.
>
> **Service B** (read-heavy, e.g. social timeline): bottleneck is the *read path*. A cache is essential — at 1000:1 read/write, even a 90% cache-hit rate cuts DB reads by 10×. Sharding may or may not be needed depending on cache effectiveness.
>
> The numbers told you. You did not need to know the product.
>
> </details>

> **Exercise 3 — Time the estimate.**
> Set a 5-minute timer. Pick a system you use daily — Spotify, Discord, Strava, Notion. Without looking up *any* number, do the full BOTE: QPS, storage, bandwidth. After the timer, look up the company's published engineering blog and compare your estimate to their reported numbers.
>
> If you are within 10× on every dimension, you are doing it right. If you are within 3×, you are doing it like a senior engineer. If you are within 1.5×, you cheated and looked something up.

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Twitter — The Infrastructure Behind Twitter: Scale](https://blog.twitter.com/engineering/en_us/topics/infrastructure/2017/the-infrastructure-behind-twitter-scale)** (2017) — published numbers for QPS, storage, and the *operational* sizes of the fleets that handle them. Excellent for calibrating your peak-factor instinct.

- **[Discord — Discord's Trillion-Message Move from Cassandra to ScyllaDB](https://discord.com/blog/how-discord-stores-trillions-of-messages)** (2023) — the post documents *exactly* the kind of growth-curve estimation that drove the migration. The estimates are explicit and reproducible.

- **[YouTube — Press / By the numbers](https://blog.youtube/press/)** — periodically updated stats on hours watched, uploads, monthly users. Your YouTube BOTE estimate (Exercise 1) should land within a factor of 3 of the numbers quoted here.

- **[High Scalability](https://highscalability.com/)** — a long-running archive of architecture posts from named systems (Reddit, Pinterest, Tumblr, Etsy, Foursquare, etc.) with traffic numbers in each. Useful as a "known answers" reference for sanity-checking your own estimates.

---

**Next:** the most-misunderstood theorem in distributed systems. We will demystify CAP, fix it with PACELC, and *feel* a partition with a runnable simulator. → [Lesson 4 — The CAP theorem and PACELC, honestly](/cortex/system-design/foundations/cap-and-pacelc)
