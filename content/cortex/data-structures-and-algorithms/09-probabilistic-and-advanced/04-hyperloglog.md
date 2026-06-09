---
title: HyperLogLog
summary: "Estimate how many DISTINCT items a stream had, in a few KB regardless of stream size. The trick — count leading zeros in hashes: a hash with k leading zeros appears about once per 2^k distinct items, so max-leading-zeros ≈ log2(distinct). Buckets plus a harmonic mean tame the variance. Used by Redis and BigQuery."
prereqs:
  - probabilistic-and-advanced-count-min-sketch
  - algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms
---

## Why It Exists

The [count-min sketch](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/count-min-sketch) estimates *how often* each item appeared. HyperLogLog answers a different streaming question: *how many **distinct** items were there?* — the cardinality. Counting unique visitors, distinct search queries, unique IPs. The exact way is a hash set of everything you've seen, costing memory proportional to the *number of distinct items* — gigabytes for a billion uniques.

HyperLogLog estimates the same number in a **fixed few kilobytes**, whatever the stream size, at a small relative error. The whole thing rests on one beautiful observation about randomness: if you hash items to uniform bit strings, the **maximum number of leading zeros** you ever see is evidence of how many distinct items there were. A hash starting with `k` zeros happens with probability `2⁻ᵏ`, so spotting one is like flipping `k` heads in a row — you'd expect to need about `2ᵏ` tries. See a maximum of `k` leading zeros, and you've probably seen about `2ᵏ` distinct items. It's a [randomized estimator](/cortex/data-structures-and-algorithms/algorithms-by-strategy/randomized-algorithms/introduction-to-randomized-algorithms), and it powers Redis `PFCOUNT`, BigQuery's `APPROX_COUNT_DISTINCT`, and essentially every analytics platform.

## See It Work

Hash each item; the first `p` bits choose a bucket (register), and the leading-zero count of the rest updates that register's max. The cardinality estimate combines all registers via a bias-corrected harmonic mean.

```python run viz=array
import math, hashlib
def _hash32(x):                                        # deterministic, well-mixed: top 32 bits of MD5
    return int.from_bytes(hashlib.md5(x.encode()).digest()[:4], "big")

class HyperLogLog:
    def __init__(self, p):
        self.p = p; self.m = 1 << p                    # m = 2^p registers
        self.reg = [0] * self.m
    def add(self, x):
        h = _hash32(x)
        idx = h >> (32 - self.p)                        # first p bits -> bucket
        w = h & ((1 << (32 - self.p)) - 1)             # remaining bits
        rank = (32 - self.p) - w.bit_length() + 1 if w else (32 - self.p) + 1   # leading zeros + 1
        self.reg[idx] = max(self.reg[idx], rank)        # keep the max per bucket
    def count(self):
        alpha = 0.7213 / (1 + 1.079 / self.m) if self.m >= 128 else 0.673
        Z = sum(2.0 ** (-r) for r in self.reg)         # harmonic-mean denominator
        E = alpha * self.m * self.m / Z
        if E <= 2.5 * self.m:                           # small-range correction
            V = self.reg.count(0)
            if V:
                E = self.m * math.log(self.m / V)
        return E

hll = HyperLogLog(p=10)                                 # 1024 registers (~1 KB)
for i in range(10000):
    hll.add(f"item{i}")
print(round(hll.count()))                               # ~10482 (true 10000)
for i in range(10000):
    hll.add(f"item{i}")                                # re-add everything
print(round(hll.count()))                               # ~10482 — duplicates don't change cardinality
```

```java run viz=array
import java.security.MessageDigest;
public class Main {
    static long hash32(String x) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(x.getBytes("UTF-8"));
            return ((d[0]&0xFFL)<<24) | ((d[1]&0xFFL)<<16) | ((d[2]&0xFFL)<<8) | (d[3]&0xFFL);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    static class HLL {
        int p, m; int[] reg;
        HLL(int p) { this.p = p; this.m = 1 << p; reg = new int[m]; }
        void add(String x) {
            long h = hash32(x);
            int idx = (int) (h >>> (32 - p));
            long w = h & ((1L << (32 - p)) - 1);
            int bitlen = (w == 0) ? 0 : (64 - Long.numberOfLeadingZeros(w));
            int rank = (w != 0) ? (32 - p) - bitlen + 1 : (32 - p) + 1;
            if (rank > reg[idx]) reg[idx] = rank;
        }
        double count() {
            double alpha = m >= 128 ? 0.7213 / (1 + 1.079 / m) : 0.673;
            double Z = 0; int V = 0;
            for (int r : reg) { Z += Math.pow(2, -r); if (r == 0) V++; }
            double E = alpha * m * m / Z;
            if (E <= 2.5 * m && V > 0) E = m * Math.log((double) m / V);
            return E;
        }
    }
    public static void main(String[] args) {
        HLL hll = new HLL(10);
        for (int i = 0; i < 10000; i++) hll.add("item" + i);
        System.out.println(Math.round(hll.count()));   // ~10482
        for (int i = 0; i < 10000; i++) hll.add("item" + i);
        System.out.println(Math.round(hll.count()));   // ~10482
    }
}
```

Both print about `10482` twice — a ~5% estimate of the true `10000`, in 1024 small registers (~1 KB), and re-adding every item leaves the estimate unchanged (it counts *distinct* items, not occurrences). A real Redis HLL uses `m = 16384` registers (~12 KB) for ~0.8% error on cardinalities up to billions.

## How It Works

The leading-zero idea works, but a *single* counter is wildly noisy — one lucky hash with many zeros throws it off. HyperLogLog fixes that by splitting the stream into `m` independent buckets and averaging cleverly:

```d2
direction: down
hash: "hash(item) -> uniform bit string" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
split: "first p bits -> bucket index (m = 2^p buckets)\nremaining bits -> count leading zeros (the 'rank')" {style.fill: "#fde68a"; style.stroke: "#d97706"}
reg: "each register keeps the MAX rank it has seen\n(max leading zeros ~ log2(items in that bucket))" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
combine: "estimate = alpha * m^2 / SUM(2^-register)\n(HARMONIC mean of 2^rank, bias-corrected)\n-> relative error ~ 1.04 / sqrt(m)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
hash -> split
split -> reg
reg -> combine
```

<p align="center"><strong>One leading-zero counter is too noisy; HyperLogLog runs <code>m</code> of them (bucketed by the hash's first bits) and combines them with a bias-corrected harmonic mean, shrinking the error to about <code>1.04/√m</code>.</strong></p>

Three load-bearing facts:

- **Max leading zeros ≈ log₂(cardinality).** A uniformly random hash has `k` leading zeros with probability `2⁻ᵏ`. Across `n` distinct hashes, the *maximum* leading-zero count is about `log₂ n`. So one register storing "most zeros I've seen" already estimates `2^{max}` ≈ the count — crudely. (Duplicates don't matter: the same item hashes the same way and can't raise the max twice, which is why HLL counts *distinct* items.)
- **Buckets + harmonic mean kill the variance.** A single estimator has huge variance (one freak hash ruins it). Split into `m = 2^p` registers by the hash's first `p` bits, each tracking its own max-rank, and combine with the **harmonic mean** of `2^{rank}` (bias-corrected by `α_m`). Averaging `m` independent estimates cuts the relative error to `≈ 1.04/√m` — `m = 16384` gives ~0.8%.
- **Fixed, tiny, and mergeable.** Memory is `m` registers of ~6 bits each — kilobytes, *independent of stream size or cardinality*. And two HLLs over different streams **merge** by taking the element-wise **max** of their registers, yielding the cardinality of the *union* with no recomputation ([Your Turn](#your-turn)) — perfect for distributed counting (per-shard HLLs merged at query time).

> **Key takeaway.** HyperLogLog estimates distinct-count from the **maximum leading zeros** in item hashes (a `k`-zero hash implies ~`2ᵏ` distinct items). It buckets into `m = 2^p` registers (each keeping its max rank) and combines them with a bias-corrected **harmonic mean**, for `≈ 1.04/√m` error in a *fixed* few KB regardless of stream size. Registers **merge** by element-wise max → union cardinality.

## Trace It

The leading-zero intuition is the soul of the algorithm, and it's worth seeing the relationship directly before trusting the machinery on top.

**Predict before you run:** you hash a stream of distinct items and track the maximum number of leading zeros any hash has had. After a stream of `1000` distinct items, roughly what's that maximum — about `3`, about `10`, or about `100`?

```python run viz=array
import hashlib, math
def _hash32(x):
    return int.from_bytes(hashlib.md5(x.encode()).digest()[:4], "big")

def max_leading_zeros(n):
    mx = 0
    for i in range(n):
        h = _hash32(f"x{i}")
        lz = 32 - h.bit_length() if h else 32         # leading zeros in the 32-bit hash
        mx = max(mx, lz)
    return mx

for n in (100, 1000, 10000):
    mx = max_leading_zeros(n)
    print(f"n={n:>5} distinct -> max leading zeros = {mx}  (log2(n) = {math.log2(n):.1f},  2^max = {2**mx})")
```

<details>
<summary><strong>Reveal</strong></summary>

After `1000` distinct items the maximum is about **11** — close to `log₂(1000) ≈ 10`, *not* 3 or 100. Across the three sizes the max leading-zero count is `8, 11, 13` while `log₂ n` is `6.6, 10, 13.3`: it tracks the logarithm of the cardinality, growing by roughly one each time the distinct count *doubles*. That's the entire engine — the rarest event you witness (the longest run of leading zeros) encodes the *scale* of how many distinct things you've seen, in a single small integer. But notice the crude single-estimator `2^max` reads `256, 2048, 8192` for true counts `100, 1000, 10000` — right order of magnitude, badly off in detail. *One* leading-zero counter is far too noisy to use directly: a single freakishly-zero-heavy hash inflates it. HyperLogLog's contribution is the *averaging* — `m` independent registers combined by a harmonic mean — which turns this noisy `log₂` signal into a cardinality estimate good to a couple of percent. The leading-zero trick supplies the idea; the buckets supply the accuracy.

</details>

## Your Turn

**Merge two sketches into a union count.** HyperLogLog's superpower for distributed systems: combine per-stream sketches by taking the element-wise *max* of their registers, and read off the cardinality of the union — no access to the original items needed.

```python run viz=array
import hashlib, math
def _hash32(x):
    return int.from_bytes(hashlib.md5(x.encode()).digest()[:4], "big")
class HyperLogLog:
    def __init__(self, p):
        self.p = p; self.m = 1 << p; self.reg = [0] * self.m
    def add(self, x):
        h = _hash32(x); idx = h >> (32 - self.p); w = h & ((1 << (32 - self.p)) - 1)
        rank = (32 - self.p) - w.bit_length() + 1 if w else (32 - self.p) + 1
        self.reg[idx] = max(self.reg[idx], rank)
    def count(self):
        alpha = 0.7213 / (1 + 1.079 / self.m) if self.m >= 128 else 0.673
        Z = sum(2.0 ** (-r) for r in self.reg)
        E = alpha * self.m * self.m / Z
        if E <= 2.5 * self.m:
            V = self.reg.count(0)
            if V: E = self.m * math.log(self.m / V)
        return E
    def merge(self, other):                            # union = element-wise max of registers
        out = HyperLogLog(self.p)
        out.reg = [max(a, b) for a, b in zip(self.reg, other.reg)]
        return out

A = HyperLogLog(p=10)
B = HyperLogLog(p=10)
for i in range(0, 1000):
    A.add(f"u{i}")                                     # A = {u0 .. u999}
for i in range(500, 1500):
    B.add(f"u{i}")                                     # B = {u500 .. u1499}, overlapping 500..999
print(round(A.count()), round(B.count()))              # ~1004, ~945  (each true 1000)
print(round(A.merge(B).count()))                       # ~1465  (true union 1500)
```

```java run viz=array
import java.security.MessageDigest;
public class Main {
    static long hash32(String x) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(x.getBytes("UTF-8"));
            return ((d[0]&0xFFL)<<24)|((d[1]&0xFFL)<<16)|((d[2]&0xFFL)<<8)|(d[3]&0xFFL);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    static class HLL {
        int p, m; int[] reg;
        HLL(int p) { this.p = p; this.m = 1 << p; reg = new int[m]; }
        void add(String x) {
            long h = hash32(x); int idx = (int)(h >>> (32 - p)); long w = h & ((1L << (32 - p)) - 1);
            int bitlen = (w == 0) ? 0 : (64 - Long.numberOfLeadingZeros(w));
            int rank = (w != 0) ? (32 - p) - bitlen + 1 : (32 - p) + 1;
            if (rank > reg[idx]) reg[idx] = rank;
        }
        double count() {
            double alpha = m >= 128 ? 0.7213 / (1 + 1.079 / m) : 0.673;
            double Z = 0; int V = 0;
            for (int r : reg) { Z += Math.pow(2, -r); if (r == 0) V++; }
            double E = alpha * m * m / Z;
            if (E <= 2.5 * m && V > 0) E = m * Math.log((double) m / V);
            return E;
        }
        HLL merge(HLL o) { HLL out = new HLL(p); for (int i = 0; i < m; i++) out.reg[i] = Math.max(reg[i], o.reg[i]); return out; }
    }
    public static void main(String[] args) {
        HLL A = new HLL(10), B = new HLL(10);
        for (int i = 0; i < 1000; i++) A.add("u" + i);
        for (int i = 500; i < 1500; i++) B.add("u" + i);
        System.out.println(Math.round(A.count()) + " " + Math.round(B.count()));   // ~1004 ~945
        System.out.println(Math.round(A.merge(B).count()));                        // ~1465
    }
}
```

Both estimate the two streams near `1000` each and their union near `1465` (true `1500`) — computed from the registers alone, never touching the original `u*` items. Element-wise max works because a register holds the *deepest* leading-zero run seen in its bucket, and the union's deepest run is just the max of the two. That mergeability is why HyperLogLog scales horizontally: every shard keeps its own tiny sketch, and a coordinator merges them for a global distinct-count.

## Reflect & Connect

- **Cardinality from the rarest event.** The maximum leading-zero count ≈ `log₂(distinct)`; a `k`-zero hash implies ~`2ᵏ` distinct items. One small integer per bucket captures the *scale* of the stream.
- **Buckets + harmonic mean buy the accuracy.** A single estimator is hopeless (huge variance); `m` registers combined by a bias-corrected harmonic mean give `≈ 1.04/√m` error. The leading-zero trick is the idea, averaging is the engineering.
- **Fixed memory, any cardinality.** `m` registers of ~6 bits — kilobytes regardless of stream size. Redis uses 16384 registers (~12 KB) for ~0.8% error up to ~`2⁶⁴`.
- **Mergeable by max → distributed counting.** Union two HLLs by element-wise max of registers. Per-shard sketches merge into a global count with no re-scan — the property that makes it production infrastructure.
- **The probabilistic-structures arc.** [Bloom](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/bloom-filter) (membership), [count-min](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/count-min-sketch) (frequency), HyperLogLog (cardinality) — three different streaming questions, all answered by *hash into a small fixed structure and accept a bounded approximation*. Its lineage: Flajolet-Martin → LogLog → HyperLogLog.

## Recall

<details>
<summary><strong>Q:</strong> What does HyperLogLog estimate, and what's the core observation?</summary>

**A:** The number of *distinct* items (cardinality) in a stream. Core observation: a uniform hash has `k` leading zeros with probability `2⁻ᵏ`, so the maximum leading-zero count seen ≈ `log₂(distinct)` — seeing `k` zeros implies ~`2ᵏ` distinct items.

</details>
<details>
<summary><strong>Q:</strong> Why aren't a single leading-zero counter's results usable directly?</summary>

**A:** Huge variance — one freakishly zero-heavy hash inflates the estimate. HyperLogLog runs `m` independent registers (bucketed by the hash's first bits) and combines them with a bias-corrected harmonic mean, reducing relative error to `≈ 1.04/√m`.

</details>
<details>
<summary><strong>Q:</strong> Why don't duplicate items change the estimate?</summary>

**A:** A duplicate hashes to the same value and the same bucket, and can't raise that register's max-rank beyond what the first occurrence already set. So HLL counts *distinct* items, ignoring repeats — by construction.

</details>
<details>
<summary><strong>Q:</strong> How do you merge two HyperLogLogs, and why does it work?</summary>

**A:** Take the element-wise maximum of their registers. Each register holds the deepest leading-zero run in its bucket; the union's deepest run is just the larger of the two, so the merged sketch estimates the union's cardinality — without the original items.

</details>
<details>
<summary><strong>Q:</strong> Why is the memory fixed regardless of how many items stream through?</summary>

**A:** It stores only `m` registers of ~6 bits each — a function of the chosen accuracy (`m`), not of the stream size or cardinality. Whether you see a thousand or a billion distinct items, the footprint is the same few KB.

</details>

## Sources & Verify

- **Flajolet, Fusy, Gandouet & Meunier** (2007), "HyperLogLog: the analysis of a near-optimal cardinality estimation algorithm" — the algorithm, the `α_m` bias correction, and the `1.04/√m` error.
- **Flajolet & Martin** (1985, probabilistic counting) and **Durand & Flajolet** (2003, LogLog) — the lineage HyperLogLog refines.
- **Redis** `PFADD`/`PFCOUNT`/`PFMERGE` and **Google BigQuery** `APPROX_COUNT_DISTINCT` — production HLLs; the `~10482` estimate of 10000, the `8/11/13` max-leading-zeros, and the `~1465` merged union above come from the runnable blocks — re-run to verify (deterministic MD5 hashing; the estimates are approximate by design — within a few percent, not exact).
