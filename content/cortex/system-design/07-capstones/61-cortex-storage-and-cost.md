---
title: '61. Cortex storage & cost'
group: 'Capstone: Cortex Platform (a system you are currently looking at)'
summary: What grows without bound (the Mongo log), what the real ceilings are (one Postgres, a pool of 10), what the homelab costs versus an equivalent cloud bill, and the per-coached-session token model that makes BYOK the difference between a $7 and a $5,000 month.
---

# 61. Cortex storage & cost

## TL;DR
> Cortex's **storage** is modest and mostly bounded — except one thing: the **Mongo `hello_events` log is append-only with no TTL**, so it grows forever (slowly — ~100 bytes/event, so years before it matters, but *forever*). Postgres holds the only data that grows with *use*: the tutor's sessions and messages (~6–24 KB per completed session → ~20 GB at a million sessions), and the real ceiling there isn't disk, it's the **HikariCP pool of 10 connections**. The content tree ships *inside the image* and lives in the 1 GiB heap. On **cost**, two axes: the **homelab runs the whole 4-node cluster for ~€50/month** (mostly amortized hardware + a power bill), versus **~$200–300/month** for an equivalent always-on managed-cloud stack — a 4–6× difference whose price is "you are the SRE." But the axis that actually decides scalability is **AI tokens**: a coached session is ~9 turns, each a Haiku *gate* (~$0.003) plus a Sonnet *coach* (~$0.017), so **~$0.17/session** if the operator pays — which at **1,000 learners/day is ~$5,100/month, untenable for a homelab.** **BYOK** flips it: the visitor's key funds the expensive coach, the operator pays only the ~$0.003 gate, so the same 1,000/day costs the operator **~$750** (and 10/day costs **~$7**). BYOK isn't a feature; it's the thing that makes an AI tutor survivable on a homelab. **One update since:** coach sessions are now **ephemeral** — a sliding TTL with a background purge — so the tutor's session/message tables no longer grow with cumulative *use*; the durable, use-growing stores are code submissions and the transcripts allow-listed users explicitly **Save**.

## 1. Motivation

Two questions decide whether a system can *keep* running: **what grows, and what does it cost?** They're usually treated separately, but for an AI-bearing homelab they collide — because the thing that grows fastest (coaching usage) is also the thing that costs the most (model tokens). A system-design chapter that only sized disks would miss the entire story. So we do both axes, with real prices, and find the one decision (BYOK) that turns an exponential cost curve into a flat one.

## 2. What grows — storage

```d2
direction: right
app: cortex / tutor
pg: Postgres (single) {
  shape: cylinder
  visits: visits — 1 row, constant
  tutor: tutor sessions/messages — grows with use
}
redis: Redis (single) — cache + counters (ephemeral) { shape: cylinder }
mongo: Mongo (single) — hello_events { shape: cylinder; style.stroke: "#ef4444" }
img: content tree (in the image → 1 GiB heap)
app -> pg: "JDBC (pool = 10) ← the real ceiling"
app -> redis: "fail-open"
app -> mongo: "append-only, NO TTL → unbounded ⚠"
app -> img: "resident index"
```

| Store | Growth | Real ceiling | Runway |
|---|---|---|---|
| **Mongo `hello_events`** | **append-only, no TTL → unbounded** (~100 B/event) | single-node disk | 1M events ≈ 100 MB; 100M ≈ 10 GB. Years — *but forever.* **Add a TTL.** |
| **Postgres `visits`** | one row, `UPDATE … +1` | trivial | constant (only a hot-row lock under extreme write concurrency) |
| **Postgres `tutor`** | grows with coaching: ~6–12 messages/session, ~0.5–2 KB each | **single instance; pool of 10** | ~6–24 KB/session → 10k ≈ 0.2 GB, 1M ≈ 20 GB. The **connection cap bites before disk.** |
| **Content tree** | the whole markdown corpus, in the image | image size + the **1 GiB heap** (index resident) | megabytes today; the ceiling is heap headroom (ties to [ch 60](/cortex/system-design/capstones/cortex-failure-thresholds) OOM) |

**Why these stores grow the way they do — the engine underneath.** All four rows are **OLTP**: small reads and writes keyed by an id, never a full-table scan for a report. That's the access pattern relational engines and document stores are built for, and it's why none of them is columnar — column-oriented layouts pay off only for *analytics* (scan two columns out of a hundred across millions of rows), which Cortex doesn't run online. Two engine families shape the table:

- **Postgres is a B-tree store.** A B-tree updates data *in place*: to write a row it overwrites a fixed-size page, and — because a torn page write would corrupt the tree — it first journals that page to a **write-ahead log**, so every logical write hits disk at least twice. That double-write is **write amplification**, the ratio of bytes actually written to bytes you'd have written with a bare append-only log. It's why a single Postgres has a real write ceiling — but Cortex never approaches it, because the binding constraint arrives far earlier: the **HikariCP pool of 10**. Deleted rows also leave dead tuples that `VACUUM` must reclaim (B-trees fragment; the free space can't trivially be handed back to the OS), so "grows with use" is really "grows, then needs background compaction."
- **The Mongo `hello_events` log is the append-only extreme.** It only ever appends — the cheapest possible write, sequential rather than the random page-scattered writes a B-tree makes — which is exactly why it was chosen for a fire-and-forget telemetry sink. The same property is the heart of **LSM-tree** storage engines (see [ch 24 — LSM-trees vs B-trees](/cortex/system-design/storage-and-search/lsm-trees-vs-btrees)): write sequentially now, sort and merge later. The price of "cheapest write" is that nothing ever reclaims space on its own — hence the unbounded growth flagged in red.
- **Every index is a second copy you also pay to write.** An index is a derived structure: it speeds reads but each one adds to the write cost and the disk footprint. Mongo's `ts_desc` index on `hello_events` makes `/api/recent` a fast range scan — but it grows in lockstep with the log it indexes, so the "100 B/event" figure is really *event plus its index entry*. Indexes are never free; they trade write-time and space for read-time.

**The one thing to fix:** the unbounded Mongo log. It's harmless for years, but "append forever with no TTL" is a latent problem and — conveniently — the cleanest hook for making the system [data-intensive](/cortex/system-design/capstones/cortex-data-intensive) (turn the fire-and-forget log into a real event stream with retention). The standard discipline is **hot / warm / cold tiering**: keep only the recent window on the fast, expensive store that answers live queries (hot), and age everything older onto progressively cheaper, slower media. For an event log the cold tier is almost always **[object storage](/cortex/system-design/storage-and-search/object-storage)** — compressed, append-friendly, an order of magnitude cheaper per GB than database disk, and a natural fit because old events are read rarely and never updated. A TTL is the crudest tiering rule (hot-or-deleted); a retention-plus-archive policy is the grown-up version (hot in Mongo → cold in object storage → analyzed offline). And the *log* compresses beautifully: a stream of near-identical 100-byte records is exactly the high-repetition input compression eats for free, trading a little CPU for a large cut in bytes-on-disk and backup size. Size the tutor's growth yourself:

```d3 widget=estimation-calculator
{
  "title": "Tutor Postgres growth — sessions as 'writes' (≈12 KB each)",
  "peakFactor": 2,
  "replicationFactor": 1,
  "presets": [
    { "name": "10 learners/day",   "dau": 10,   "writesPerUser": 1, "readsPerUser": 12, "bytesPerWrite": 12000 },
    { "name": "100 learners/day",  "dau": 100,  "writesPerUser": 1, "readsPerUser": 12, "bytesPerWrite": 12000 },
    { "name": "1000 learners/day", "dau": 1000, "writesPerUser": 1, "readsPerUser": 12, "bytesPerWrite": 12000 }
  ]
}
```

The fix in one picture — today's "append forever" replaced by a hot window that ages onto cheap cold storage:

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  W[New hello_event] --> H[Hot: Mongo recent window<br/>fast range scan, costly per GB]
  H -->|age out past TTL| C[Cold: object storage<br/>compressed, ~10x cheaper per GB]
  H -->|/api/recent| R[Live read]
  C -.->|rare replay / analytics| A[Offline scan]
  T[Today: NO TTL] -.->|append forever| H
```

## 3. What it costs — infrastructure

**Homelab (assumptions stated):** four mini-PC-class nodes drawing ~80 W aggregate → ~58 kWh/month → **~€17/month** electricity (at €0.30/kWh). Amortize 4 × €400 hardware over 4 years → **~€33/month**. Plus ~€2 domain/DNS. **All-in ≈ €50–55/month**, mostly the amortized hardware.

**Equivalent always-on cloud (ballpark):**

| Piece | Small managed equivalent | $/month |
|---|---|---|
| K8s + 1–2 small workers | managed control plane + nodes | ~$70–150 |
| Postgres | smallest HA-ish managed | ~$15–50 |
| Redis | smallest managed | ~$10–15 |
| Mongo | Atlas M10-ish | ~$55–60 |
| LB / egress / ingress | — | ~$20+ |
| **Total** | | **~$170–330** |

So the homelab runs the *same topology* for **~€50** versus a **~$200–300** cloud equivalent — a **4–6×** saving whose price is that *you* are the SRE, the pager, and the power bill. (The [Homelab from Scratch](/cortex/homelab-from-scratch) book is the long version of that trade.) Read **egress**, meanwhile, is effectively **free**: once Cloudflare proxies the static assets they're edge-served at no metered cost — the optimization with no cost-axis trade-off, measured in [ch 64](/cortex/system-design/capstones/cortex-edge-delivery).

## 4. What it costs — AI tokens (the axis that matters)

This is where scalability is actually decided. A coaching **turn** is two model calls; here's one annotated with tokens and dollars at current prices (Haiku $1/$5 per 1M in/out; Sonnet $3/$15):

```mermaid
sequenceDiagram
  autonumber
  participant U as Learner
  participant T as Tutor
  participant A as Anthropic
  U->>T: answer (this step)
  T->>A: GATE — Haiku · ~2,000 in + ~150 out
  A-->>T: verdict  ($0.0020 + $0.00075 = $0.00275)
  T->>A: COACH — Sonnet · ~3,000 in + ~500 out
  A-->>T: streamed reply  ($0.0090 + $0.0075 = $0.0165)
  Note over T,A: per turn ≈ $0.0193  ·  gate is ~14% of it
```

This is **[back-of-envelope estimation](/cortex/system-design/foundations/back-of-envelope-estimation)** applied to money instead of milliseconds, and the same discipline applies: build the unit cost bottom-up (tokens × price), find the dominant term, then multiply out. Notice the structure — the cost is a *per-token meter*, so it scales **linearly with usage** with no fixed plant to amortize. That's the opposite of the infra bill in §3 (mostly sunk hardware, ~free at the margin). The metaphor that makes the whole chapter click: **infrastructure is rent, tokens are a taxi meter.** Rent is fixed whether you sit at home or run a thousand sessions; the meter ticks on every turn. A homelab can absorb rent forever, but it cannot absorb a meter that runs on someone else's joyride — which is precisely the problem BYOK solves below.

**Per turn ≈ $0.019.** A completed 6-step session runs ~**9 turns** (some retries) → **≈ $0.17/session** if the operator pays. Prompt caching on the stable system+rubric+grounding prefix knocks the input cost down (cache reads ≈ 0.1× input), pulling it toward **~$0.10–0.12** — a nice, concrete payoff of the caching the [Claude Stack book](/cortex/the-claude-stack/building-with-the-claude-api/prompt-caching) teaches.

**Now the tiers, across three usage scenarios (monthly cost *to the operator*):**

| Learners/day | Sessions/mo | **Homelab tier** (operator pays ~$0.17) | **BYOK tier** (operator pays gate only ~$0.0025/turn ≈ $0.025) |
|---|---|---|---|
| **10** | ~300 | **~$51** | **~$7.5** |
| **100** | ~3,000 | **~$510** | **~$75** |
| **1,000** | ~30,000 | **~$5,100** | **~$750** |

Read the homelab column and the design justifies itself. At 10 learners/day, operator-pays is fine (~$51, on par with the infra bill). At **100/day it already dwarfs the €50 infra cost**, and at **1,000/day it's ~$5,100/month — absurd for a homelab.** The **BYOK** column is why the tutor can grow: the operator keeps only the cheap, deterministic **gate** (which is what produces the *verdict* the FSM needs), and pushes the expensive generative **coach** onto each visitor's own key. Cost then scales with *visitors' wallets*, not the operator's — the unbounded-user-safe property. (And it's *measured*, not guessed: each turn records `TurnUsage.costUsd`, so these estimates can be replaced with telemetry.)

## 5. Build It — the cost model, runnable

Punch in your own token assumptions and learner counts:

```python run
# Anthropic list prices ($ per 1M tokens), as of 2026 — refresh as needed.
HAIKU  = dict(inp=1.0,  out=5.0)    # the gate
SONNET = dict(inp=3.0,  out=15.0)   # the coach

def cost(tokens_in, tokens_out, price):
    return tokens_in/1e6*price["inp"] + tokens_out/1e6*price["out"]

gate  = cost(2000, 150, HAIKU)      # ~$0.00275
coach = cost(3000, 500, SONNET)     # ~$0.0165
turn  = gate + coach
turns_per_session = 9

session_homelab = turn  * turns_per_session     # operator pays gate + coach
session_byok    = gate  * turns_per_session      # operator pays ONLY the gate

print(f"per turn:            ${turn:.4f}  (gate ${gate:.4f} + coach ${coach:.4f})")
print(f"per session homelab: ${session_homelab:.3f}")
print(f"per session BYOK:    ${session_byok:.3f}   ({session_byok/session_homelab*100:.0f}% of homelab)\n")

print(f"{'learners/day':>12} | {'sessions/mo':>11} | {'homelab $/mo':>12} | {'BYOK $/mo':>9}")
for per_day in (10, 100, 1000):
    sessions = per_day * 30
    print(f"{per_day:>12} | {sessions:>11} | {sessions*session_homelab:>11.0f}  | {sessions*session_byok:>8.0f}")
print("\nBYOK turns an exponential operator bill into a flat one — that's the whole point of the two-tier design.")
```

## 6. Trade-offs

| Decision | Choice | Why |
|---|---|---|
| Coaching cost | **two-tier (homelab/BYOK)** | operator pays full only for the allowlist; everyone else funds their own coach — the only way it scales |
| Gate model | **Haiku, not Sonnet** | the *judgment* is cheap structured classification; pay for the strong model only on the *conversation* |
| Prompt caching | **cache the stable prefix** | ~40–50% off input across a session's turns — measurable, free latency win too |
| Mongo log | **append-only today** | simplest possible write; the cost is unbounded growth → add a TTL (or [stream it](/cortex/system-design/capstones/cortex-data-intensive)) |
| Stores | **single instances** | cheap and simple; the ceiling is the **connection pool**, not disk, for a long time |

## 7. Edge cases

- **A whale learner.** Someone does 50 sessions in a day. Homelab tier: ~$8.50 of *your* money. BYOK tier: ~$1.25 of yours (gates) + the rest on *their* key. BYOK makes a power user self-funding.
- **Cache cold-start.** The first turn of a session can't hit a warm prefix cache, so it's full price; the savings accrue over the session's later turns. Short sessions benefit less.
- **Mongo at 100M events.** ~10 GB of documents — fine on disk, but the `ts_desc` index has grown alongside it (an index is a second copy you keep writing), so the real bill is *documents + index + every backup of both*. Each `/api/recent` still rides that index for a cheap range scan, which is the point — but it's also the reminder that **unbounded ≠ free forever**, and that a hot/warm/cold split would keep the hot index small while the cold tail compresses onto object storage.

## 8. Practice

> **Exercise 1 — Where does operator-pays break?**
> At what daily-learner count does the homelab-tier AI bill exceed the entire ~€50 infra cost? What does BYOK do to that crossover?
>
> <details>
> <summary>Solution</summary>
>
> Homelab tier is ~$0.17/session ≈ ~$5.10 per learner-month (30 sessions). €50 ≈ ~$54. So the AI bill passes the *infra* bill at about **`54 / 5.10 ≈ 11 learners/day`** — i.e. almost immediately. By 100/day it's ~$510/mo (10× the infra), and 1,000/day is ~$5,100 (~100× the infra). **BYOK** drops the operator's per-session cost to ~$0.025 (gate only), so the same crossover moves to **`54 / 0.75 ≈ 72 learners/day`**, and even 1,000/day is only ~$750 — the curve flattens because the expensive half is funded by visitors. The exercise's real lesson: for a self-hosted AI feature, **who pays for tokens is the single most important scalability decision** — more than any database or replica choice.
>
> </details>

> **Exercise 2 — Cheaper gate?**
> Could you cut cost by running the *gate* on an even cheaper/local model? What's the risk?
>
> <details>
> <summary>Solution</summary>
>
> The gate is already the cheap half (~14% of a turn), so moving it to a local model saves little of the operator's *homelab-tier* bill and *nothing* of the BYOK bill (where the operator only pays the gate — so a local gate would make BYOK nearly free to operate). The catch is **quality**: the gate produces the *verdict that drives the FSM*, so a weaker gate that's too lenient lets learners advance without understanding (defeating the whole point) or too harsh frustrates them. That's exactly why the tutor **CI-gates the gate with eval suites** — you can swap the gate model, but only if it still passes the known-good/known-bad verdict tests. Cost-optimizing the *judge* is fine; cost-optimizing it into *being wrong* is not. (This is the [diligence](/cortex/the-claude-stack/ai-fluency/diligence) discipline: verify the model's output, don't trust it.)
>
> </details>

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## 9. In the Wild

- **[`TutorContract.scala` → `TurnUsage`](https://github.com/ani2fun/cortex)** — `inputTokens/outputTokens/cacheRead/cacheWrite/costUsd` recorded per turn. The §4 model is something the system *measures*.
- **[Anthropic pricing](https://www.anthropic.com/pricing)** & **[prompt caching](https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching)** — the live numbers behind §4 (refresh them; they move).
- **[Cortex Tutor → Tiers & BYOK](/cortex/cortex-onboarding/cortex-tutor/tiers-and-byok)** — how the key stays in the browser, the mechanism that makes the BYOK column real.
- **[Designing Data-Intensive Applications](https://dataintensive.net/)** ch. 1 — "maintainability" includes the operational *cost* of a system, not just its correctness. And **ch. 4 (Storage and Retrieval)** is the framing behind §2: write/space amplification, why B-trees double-write through the WAL, why append-only LSM logs are the cheap-write extreme, compression, and why analytics — not OLTP — is what wants columnar storage.
- **[ch 24 — LSM-trees vs B-trees](/cortex/system-design/storage-and-search/lsm-trees-vs-btrees)** & **[ch 28 — object storage](/cortex/system-design/storage-and-search/object-storage)** — the engine that explains Postgres's in-place writes vs the Mongo log's append-only growth, and the cheap cold tier the log should age into.
- **[ch 03 — back-of-envelope estimation](/cortex/system-design/foundations/back-of-envelope-estimation)** — the unit-cost discipline §4 and §5 run on; **[ch 63 — making Cortex data-intensive](/cortex/system-design/capstones/cortex-data-intensive)** — where the unbounded log becomes a real, retained event stream.

---

> **Next:** [62. Scaling Cortex like LeetCode](/cortex/system-design/capstones/scaling-cortex-like-leetcode) — today it's one replica and a semaphore of 8. What's the staged path to a system that serves a classroom, a campus, then the internet — and which single move (it's the judge queue) actually matters most?
