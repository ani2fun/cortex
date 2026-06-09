---
title: Bloom Filter
summary: "A probabilistic set in a fraction of a hash set's memory — a bit array plus k hashes. Its answers are one-sided: definitely-absent is certain, probably-present can be a false positive, and false negatives never happen. The Monte Carlo first line of defence in databases and caches."
prereqs:
  - linear-structures-hash-table-what-is-a-hash-table
  - algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms
---

## Why It Exists

You want to answer "have I seen this item before?" over a billion items — recently-logged queries, crawled URLs, keys that might live on disk. A [hash set](/cortex/data-structures-and-algorithms/linear-structures/hash-table/what-is-a-hash-table) does it exactly, but storing a billion strings costs tens of gigabytes. Often you don't need exactness; you need *cheap* — and you can tolerate occasionally being told "yes" when the answer is "no," as long as you're never told "no" when the answer is "yes."

A **bloom filter** is that trade. It's a bit array plus `k` hash functions, using a *handful of bits per item* instead of the whole item. Its answers are deliberately **one-sided**: "definitely not in the set" is always correct, but "probably in the set" might be a **false positive** — there are **no false negatives**. That asymmetry is the entire design. It's a [Monte Carlo](/cortex/data-structures-and-algorithms/algorithms-by-strategy/randomized-algorithms/introduction-to-randomized-algorithms) structure (bounded one-sided error), and it's the first line of defence in Cassandra and Bigtable (skip a disk read for keys *definitely* not in an SSTable), Chrome's Safe Browsing (is this URL *definitely* not malicious?), and CDN caches.

## See It Work

A bloom filter hashes each item to `k` bit positions. `add` sets those bits; `contains` checks whether *all* of them are set.

```python run viz=array
MOD = (1 << 31) - 1
def _h(s, base):                                      # deterministic polynomial hash
    h = 0
    for c in s:
        h = (h * base + ord(c)) % MOD
    return h

class BloomFilter:
    def __init__(self, m, k):
        self.m, self.k = m, k                         # m bits, k hash functions
        self.bits = [0] * m
    def _positions(self, x):                          # double hashing -> k positions
        h1, h2 = _h(x, 131), _h(x, 137) | 1
        return [(h1 + i * h2) % self.m for i in range(self.k)]
    def add(self, x):
        for p in self._positions(x):
            self.bits[p] = 1                          # set all k bits
    def contains(self, x):
        return all(self.bits[p] for p in self._positions(x))   # present only if ALL k bits are set

bf = BloomFilter(m=64, k=3)
for w in ["apple", "banana", "cherry"]:
    bf.add(w)
print(bf.contains("apple"))     # True
print(bf.contains("banana"))    # True
print(bf.contains("grape"))     # False
```

```java run viz=array
public class Main {
    static final long MOD = (1L << 31) - 1;
    static long h(String s, long base) {
        long v = 0;
        for (int i = 0; i < s.length(); i++) v = (v * base + s.charAt(i)) % MOD;
        return v;
    }
    static class BloomFilter {
        int m, k; boolean[] bits;
        BloomFilter(int m, int k) { this.m = m; this.k = k; bits = new boolean[m]; }
        int[] positions(String x) {
            long h1 = h(x, 131), h2 = h(x, 137) | 1;
            int[] p = new int[k];
            for (int i = 0; i < k; i++) p[i] = (int) ((h1 + (long) i * h2) % m);
            return p;
        }
        void add(String x) { for (int p : positions(x)) bits[p] = true; }
        boolean contains(String x) { for (int p : positions(x)) if (!bits[p]) return false; return true; }
    }
    public static void main(String[] args) {
        BloomFilter bf = new BloomFilter(64, 3);
        for (String w : new String[]{"apple", "banana", "cherry"}) bf.add(w);
        System.out.println(bf.contains("apple"));    // true
        System.out.println(bf.contains("banana"));   // true
        System.out.println(bf.contains("grape"));    // false
    }
}
```

Both print `true`, `true`, `false`. The three added fruits are found; `"grape"` isn't (here a true negative). The filter stores no fruit names — only 64 bits — yet answers membership. (We use a homemade polynomial hash because Python's built-in `hash` is randomized per process; a real filter uses a fixed, well-mixed hash.)

## How It Works

`add` and `contains` walk the same `k` positions; the asymmetry is in what a *miss* on those bits means:

```d2
direction: right
add: "add(x): set bits h1(x), h2(x), ..., hk(x) = 1" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
absent: "contains(x): any of x's k bits = 0?\n-> DEFINITELY NOT in the set (certain)\n(an added item would have set that bit)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
present: "all k bits = 1?\n-> PROBABLY in the set\n(maybe a false positive: other adds set those bits)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
nofn: "NO false negatives, EVER\nbut false-positive rate ~ (1 - e^(-kn/m))^k" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
add -> absent
add -> present
present -> nofn
absent -> nofn
```

<p align="center"><strong>A zero bit proves absence (an added item would have set it), so "definitely not" is certain. All-ones only suggests presence — other items may have set those bits — so "probably yes" can be a false positive. False negatives are impossible by construction.</strong></p>

Three load-bearing facts:

- **No false negatives, by construction.** `add(x)` sets all `k` of `x`'s bits to 1, and bits never reset. So if `x` was ever added, every one of its bits is 1 and `contains(x)` is `True` — always. The error is strictly one-sided, which is what makes a bloom filter *safe* as a pre-filter: a "no" is final.
- **False positives come from bit collisions.** `contains(y)` is `True` whenever *all* `k` of `y`'s bits happen to be set — even if `y` was never added, because *other* items collectively set them. The false-positive rate is `≈ (1 − e^{−kn/m})^k` for `n` items in `m` bits with `k` hashes; it's minimised at `k ≈ (m/n)·ln 2`, costing about `1.44·log₂(1/ε)` bits per item for rate `ε`. More bits or tuned `k` → fewer lies.
- **You can't delete (from a plain one).** Clearing `x`'s bits might clear a bit some *other* item relies on, creating a false *negative* — the one error a bloom filter must never make. So deletion is forbidden; if you need it, a **counting bloom filter** replaces bits with small counters (increment on add, decrement on delete).

> **Key takeaway.** A bloom filter is a bit array + `k` hashes: `add` sets `k` bits, `contains` checks them all. **No false negatives** ("definitely absent" is certain); **possible false positives** ("probably present" may be wrong), at rate `≈ (1 − e^{−kn/m})^k`, optimal `k ≈ (m/n)ln 2`. One-sided error = Monte Carlo. No deletion without a counting variant. A few bits per item replaces the whole set.

## Trace It

The one-sided error is the whole point — and it's easy to half-remember as "bloom filters are sometimes wrong." They're wrong in *exactly one direction*.

**Predict before you run:** a small filter (10 bits, 2 hashes) has had `cat`, `dog`, `fox`, `owl` added. Query a word that was **never** added. Can `contains` return `True` for it — and can it ever return `False` for `cat`?

```python run viz=array
MOD = (1 << 31) - 1
def _h(s, base):
    h = 0
    for c in s:
        h = (h * base + ord(c)) % MOD
    return h
class BloomFilter:
    def __init__(self, m, k):
        self.m, self.k = m, k; self.bits = [0] * m
    def _positions(self, x):
        h1, h2 = _h(x, 131), _h(x, 137) | 1
        return [(h1 + i * h2) % self.m for i in range(self.k)]
    def add(self, x):
        for p in self._positions(x): self.bits[p] = 1
    def contains(self, x):
        return all(self.bits[p] for p in self._positions(x))

bf = BloomFilter(m=10, k=2)
for w in ["cat", "dog", "fox", "owl"]:
    bf.add(w)
never_added = ["bee", "ant", "elk", "cow", "pig", "ram", "hen", "eel", "jay", "yak"]
false_pos = [w for w in never_added if bf.contains(w)]
print("never-added words that test PRESENT (false positives):", false_pos)
print("contains('cat')  — an added item:", bf.contains("cat"))
print("any added item ever tests absent?:", not all(bf.contains(w) for w in ["cat", "dog", "fox", "owl"]))
```

<details>
<summary><strong>Reveal</strong></summary>

`contains` returns `True` for `"ant"` (and possibly others) even though it was never added — a **false positive** — while every actually-added word (`cat`, `dog`, ...) tests `True`, and *no* added word ever tests `False`. With only 10 bits and four items each setting 2 bits, most of the array is already 1, so a never-added word whose two hash positions both happen to land on set bits is reported as present. That's a lie in the "present" direction. But the "absent" direction is incorruptible: an added word set *all* its bits, and bits never reset, so it can never test absent. This is what "no false negatives, possible false positives" means concretely — and it's why a bloom filter is a safe *pre-filter*: when it says "definitely not here," you can skip the expensive lookup with total confidence; when it says "maybe," you fall back to the real, exact check. Shrinking the filter (fewer bits per item) trades space for *more* false positives but never introduces a false negative — the error stays one-sided no matter how you tune it.

</details>

## Your Turn

**Measure the false-positive rate** and watch it track the theory. Fill a filter with `n` items, query a large set of never-added items, and count how many falsely test present — then compare to `(1 − e^{−kn/m})^k`.

```python run viz=array
import math
MOD = (1 << 31) - 1
def _h(s, base):
    h = 0
    for c in s:
        h = (h * base + ord(c)) % MOD
    return h
class BloomFilter:
    def __init__(self, m, k):
        self.m, self.k = m, k; self.bits = [0] * m
    def _positions(self, x):
        h1, h2 = _h(x, 131), _h(x, 137) | 1
        return [(h1 + i * h2) % self.m for i in range(self.k)]
    def add(self, x):
        for p in self._positions(x): self.bits[p] = 1
    def contains(self, x):
        return all(self.bits[p] for p in self._positions(x))

def empirical_fp(m, k, n, queries):
    bf = BloomFilter(m, k)
    for i in range(n):
        bf.add(f"item{i}")
    fp = sum(1 for j in range(queries) if bf.contains(f"query{j}"))
    return fp / queries

m, k, n, q = 1000, 3, 100, 10000
print(f"empirical FP rate : {empirical_fp(m, k, n, q):.4f}")          # ~0.0219
print(f"theoretical       : {(1 - math.exp(-k*n/m))**k:.4f}")          # ~0.0174
```

```java run viz=array
public class Main {
    static final long MOD = (1L << 31) - 1;
    static long h(String s, long base) {
        long v = 0;
        for (int i = 0; i < s.length(); i++) v = (v * base + s.charAt(i)) % MOD;
        return v;
    }
    static class BloomFilter {
        int m, k; boolean[] bits;
        BloomFilter(int m, int k) { this.m = m; this.k = k; bits = new boolean[m]; }
        int[] positions(String x) {
            long h1 = h(x, 131), h2 = h(x, 137) | 1;
            int[] p = new int[k];
            for (int i = 0; i < k; i++) p[i] = (int) ((h1 + (long) i * h2) % m);
            return p;
        }
        void add(String x) { for (int p : positions(x)) bits[p] = true; }
        boolean contains(String x) { for (int p : positions(x)) if (!bits[p]) return false; return true; }
    }
    public static void main(String[] args) {
        int m = 1000, k = 3, n = 100, q = 10000;
        BloomFilter bf = new BloomFilter(m, k);
        for (int i = 0; i < n; i++) bf.add("item" + i);
        int fp = 0;
        for (int j = 0; j < q; j++) if (bf.contains("query" + j)) fp++;
        System.out.printf("empirical FP rate : %.4f%n", (double) fp / q);              // ~0.0219
        System.out.printf("theoretical       : %.4f%n", Math.pow(1 - Math.exp(-(double)k*n/m), k));  // ~0.0174
    }
}
```

Both report an empirical rate near **2.2%**, close to the theoretical **1.7%** — the small gap is because double-hashing approximates `k` truly-independent hashes. The point is that the false-positive rate is a *knob*: more bits (`m`) or a tuned `k` drives it down predictably, so you size a bloom filter to whatever error budget you can afford. (False negatives stay at zero regardless — they're not a knob, they're impossible.)

## Reflect & Connect

- **One-sided error is the design.** "Definitely absent" is certain (a zero bit proves it); "probably present" can be a false positive (collisions). False negatives never happen — which is why it's a *safe* pre-filter for an expensive exact lookup.
- **Tunable space-vs-accuracy.** Rate `≈ (1 − e^{−kn/m})^k`, optimal `k ≈ (m/n)ln 2`, cost `≈ 1.44·log₂(1/ε)` bits per item. You pick the error budget; the structure delivers it in a fraction of a hash set's memory.
- **No deletion without counters.** Clearing bits could create a false *negative*, the forbidden error. A counting bloom filter swaps bits for counters to allow removal.
- **Monte Carlo, like Miller-Rabin.** Bounded one-sided error you accept for speed/space — contrast the [skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/skip-list)'s Las Vegas guarantee (always correct, random runtime). Different bargains: bloom trades *accuracy* for space; skip list trades *runtime* for simplicity.
- **Everywhere in systems.** SSTable read-skipping (Cassandra, Bigtable, RocksDB), Safe Browsing URL checks, CDN/proxy cache filters, spell-checkers, and the [count-min sketch](/cortex/data-structures-and-algorithms/probabilistic-and-advanced/count-min-sketch) / HyperLogLog (next lessons) that extend the "hash into a small array, accept approximation" idea from membership to *counts* and *cardinality*.

## Recall

<details>
<summary><strong>Q:</strong> What are a bloom filter's two possible errors, and which can actually happen?</summary>

**A:** False positive ("probably present" when absent) — possible. False negative ("absent" when present) — impossible by construction. The error is strictly one-sided.

</details>
<details>
<summary><strong>Q:</strong> Why can a bloom filter never give a false negative?</summary>

**A:** `add(x)` sets all `k` of `x`'s bits to 1 and bits never reset, so an added item always has all its bits set and tests present. A zero bit therefore *proves* an item was never added.

</details>
<details>
<summary><strong>Q:</strong> Where do false positives come from, and how do you reduce them?</summary>

**A:** From bit collisions — a never-added item whose `k` bits were all set by *other* items. The rate is `≈ (1 − e^{−kn/m})^k`; reduce it with more bits `m` or the optimal `k ≈ (m/n)ln 2`.

</details>
<details>
<summary><strong>Q:</strong> Why can't you delete from a plain bloom filter?</summary>

**A:** Clearing an item's bits might clear a bit another item depends on, producing a false negative — the one error that's not allowed. A counting bloom filter (counters instead of bits) supports deletion.

</details>
<details>
<summary><strong>Q:</strong> Why is a bloom filter a safe pre-filter for an expensive lookup?</summary>

**A:** Because "definitely absent" is always correct, a "no" lets you skip the costly exact check with full confidence. A "maybe" just falls back to the real lookup — so the filter only ever saves work, never causes a missed item.

</details>

## Sources & Verify

- **Bloom** (1970), "Space/time trade-offs in hash coding with allowable errors", *Comm. ACM* — the original structure and the false-positive analysis.
- **Broder & Mitzenmacher** (2004), "Network applications of Bloom filters: a survey" — tuning `m`, `k`, counting variants, and systems uses.
- **Cassandra / Bigtable / RocksDB** docs (SSTable bloom filters) and **Google Safe Browsing** — production deployments; **LeetCode**-style "design a HashSet / data stream dedup" problems are adjacent drills. The `true`/`true`/`false` memberships, the constructed false positive, and the `~2.2%`-vs-`~1.7%` rates above come from the runnable blocks — re-run to verify.
