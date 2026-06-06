---
title: Count-Min Sketch
summary: "Bloom filter's count-valued cousin — estimate how often each item appeared in a stream using a fixed d×w counter grid and d hashes. Increment d cells per item; estimate by the MIN of those cells. Over-counts only (never under), so the min is the tightest estimate."
prereqs:
  - probabilistic-and-advanced-bloom-filter
  - algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms
---

## Why It Exists

A [bloom filter](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-bloom-filter) answers *is this item present?* in tiny space. A **count-min sketch** answers the next question — *how many times has this item appeared?* — under the same budget. You're a CDN watching a million requests a second and you need the top talkers; you're a database tracking the hottest keys; you're counting words in a firehose. An exact counter per item works until the number of *distinct* items explodes (billions of IPs, the long tail of language), at which point the hash map of counters no longer fits in memory.

The sketch trades exactness for a *fixed* footprint: a small `d × w` grid of counters plus `d` hash functions. Each item bumps one counter per row; you estimate its count by reading those `d` counters and taking the **minimum**. Like the bloom filter, the error is **one-sided** — the estimate is *always ≥* the true count, never below — because counters only ever get added to. That makes it a [Monte Carlo](/cortex/data-structures-and-algorithms/algorithms-by-strategy-randomized-algorithms-introduction-to-randomized-algorithms) structure, and it's the engine behind real-time analytics, heavy-hitter detection, and DDoS monitoring.

## See It Work

Each item hashes to one column per row. `add` increments those `d` cells; `estimate` reads them and returns the smallest. With a wide enough grid (few collisions) the estimate is exact.

```python run viz=grid
MOD = (1 << 31) - 1
class CountMinSketch:
    BASES = [131, 137, 139]                          # one hash per row (so d = 3 rows)
    def __init__(self, w):
        self.w = w
        self.grid = [[0] * w for _ in range(len(self.BASES))]
    def _pos(self, x, i):
        h = 0
        for c in x:
            h = (h * self.BASES[i] + ord(c)) % MOD
        return h % self.w
    def add(self, x, count=1):
        for i in range(len(self.BASES)):             # bump one counter per row
            self.grid[i][self._pos(x, i)] += count
    def estimate(self, x):
        return min(self.grid[i][self._pos(x, i)] for i in range(len(self.BASES)))   # MIN of the d cells

cms = CountMinSketch(w=64)
for x in ["apple"] * 5 + ["banana"] * 3 + ["date"] * 7:
    cms.add(x)
print(cms.estimate("apple"))     # 5
print(cms.estimate("banana"))    # 3
print(cms.estimate("date"))      # 7
```

```java run viz=grid
public class Main {
    static final long MOD = (1L << 31) - 1;
    static final int[] BASES = {131, 137, 139};
    static class CMS {
        int w; long[][] grid;
        CMS(int w) { this.w = w; grid = new long[BASES.length][w]; }
        int pos(String x, int i) {
            long h = 0;
            for (int j = 0; j < x.length(); j++) h = (h * BASES[i] + x.charAt(j)) % MOD;
            return (int) (h % w);
        }
        void add(String x, long c) { for (int i = 0; i < BASES.length; i++) grid[i][pos(x, i)] += c; }
        long estimate(String x) {
            long m = Long.MAX_VALUE;
            for (int i = 0; i < BASES.length; i++) m = Math.min(m, grid[i][pos(x, i)]);
            return m;
        }
    }
    public static void main(String[] args) {
        CMS cms = new CMS(64);
        for (int i = 0; i < 5; i++) cms.add("apple", 1);
        for (int i = 0; i < 3; i++) cms.add("banana", 1);
        for (int i = 0; i < 7; i++) cms.add("date", 1);
        System.out.println(cms.estimate("apple"));    // 5
        System.out.println(cms.estimate("banana"));   // 3
        System.out.println(cms.estimate("date"));     // 7
    }
}
```

Both print `5`, `3`, `7` — exact, because a 3×64 grid leaves these few items collision-free. The sketch stores `3 × 64` counters no matter how many distinct items stream through; only the *accuracy* degrades as collisions rise, never the footprint.

## How It Works

Every counter is a *sum over all items that hash there*, so each cell **over-counts** the target by whatever else collided with it. The minimum picks the least-polluted cell:

```d2
direction: right
add: "add(x): for each row i, grid[i][hi(x)] += 1\n(one counter bumped per row)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
cells: "estimate reads x's d cells.\nEach = true_count(x) + collisions in that cell\n-> every cell is >= the true count" {style.fill: "#fde68a"; style.stroke: "#d97706"}
mn: "estimate = MIN of the d cells\n-> the least-collided row\n-> tightest over-estimate, still >= true" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
err: "no under-counts ever; error <= eps*N with\nw = ceil(e/eps), d = ceil(ln(1/delta)),\nfailing with prob <= delta" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
add -> cells
cells -> mn
mn -> err
```

<p align="center"><strong>Each of an item's <code>d</code> counters equals its true count plus whatever collided into that cell — so every cell is an over-estimate. Taking the <em>minimum</em> across rows finds the least-collided one: the tightest over-estimate, and never below the truth.</strong></p>

Three load-bearing facts:

- **Estimates only over-count, so MIN is correct.** A counter accumulates *every* item that hashes to it; it can never be *below* the target's true count (the target itself added to it), but it can be *above* (collisions). Every one of the `d` cells is therefore `≥ true`, and the **minimum** is the closest to truth while still safe — taking the *min* of over-estimates is exactly right; summing or averaging would *compound* the collision error.
- **`w` controls collision noise, `d` controls confidence.** Wider rows (`w = ⌈e/ε⌉`) make any single collision rarer, so each cell over-counts less; more rows (`d = ⌈ln(1/δ)⌉`) give more independent chances for a *clean* cell, so the min is tight with probability `1 − δ`. The guarantee: estimate ≤ true `+ εN` (where `N` is the total stream count) with probability `≥ 1 − δ`.
- **It's the bloom filter for counts.** Bloom: `k` bits per item, "present" can be a false positive, "absent" is certain. Count-min: `d` counters per item, the estimate can be too *high*, never too *low*. Same one-sided philosophy, lifted from membership to frequency — and it composes linearly (merge two sketches by adding their grids), which is why it's perfect for distributed stream aggregation.

> **Key takeaway.** A count-min sketch is a `d × w` counter grid + `d` hashes: `add` increments one cell per row; `estimate` returns the **min** of an item's `d` cells. Counters only over-count (collisions add), so every cell is `≥ true` and the **min is the tightest safe estimate** — never an under-count. Error `≤ εN` with prob `1 − δ` for `w = ⌈e/ε⌉`, `d = ⌈ln(1/δ)⌉`. Fixed memory, mergeable, Monte Carlo.

## Trace It

Why the *minimum*, specifically — not a single row, not the sum, not the average? Because a collision in one row shouldn't pollute the estimate if another row is clean.

**Predict before you run:** in a narrow `3 × 5` sketch, you add a *rare* key `"aa"` once and a *heavy* key `"bb"` twenty times. It happens that `"aa"` and `"bb"` collide in exactly one of `"aa"`'s three rows. So `"aa"`'s three counters read `[1, 1, 21]`. What does `estimate("aa")` return — and what would summing the cells give?

```python run viz=grid
MOD = (1 << 31) - 1
class CountMinSketch:
    BASES = [131, 137, 139]
    def __init__(self, w):
        self.w = w; self.grid = [[0] * w for _ in range(len(self.BASES))]
    def _pos(self, x, i):
        h = 0
        for c in x: h = (h * self.BASES[i] + ord(c)) % MOD
        return h % self.w
    def add(self, x, count=1):
        for i in range(len(self.BASES)): self.grid[i][self._pos(x, i)] += count
    def cells(self, x):
        return [self.grid[i][self._pos(x, i)] for i in range(len(self.BASES))]
    def estimate(self, x):
        return min(self.cells(x))

cms = CountMinSketch(w=5)
cms.add("aa", 1)                                      # true count of 'aa' is 1
cms.add("bb", 20)                                     # heavy key, collides in one of aa's rows
print("'aa' cells per row:", cms.cells("aa"))
print("estimate (MIN)    :", cms.estimate("aa"))
print("if we summed cells:", sum(cms.cells("aa")))
print("the collided row alone:", max(cms.cells("aa")))
```

<details>
<summary><strong>Reveal</strong></summary>

`estimate("aa")` is `1` — *exactly* the true count — even though one of its counters reads `21`. The three cells are `[1, 1, 21]`: two rows where `"aa"` sits alone (reading its true count of 1), and one row where the heavy `"bb"` collided into the same cell (`1 + 20 = 21`). The **minimum**, `1`, ignores the polluted row entirely and reads the truth off a clean one. Now look at the alternatives: a *single* row might be the unlucky `21` (a 21× over-estimate); the **sum** is `23` (it triple-counts `"aa"` and folds in the whole collision); the **average** is ~`7.7` (still way over). Only the `min` works, and it works precisely *because* every cell is an over-estimate: the smallest over-estimate is the closest to the truth, and it's guaranteed not to dip below it. The `d` rows aren't redundancy for averaging — they're `d` independent chances to find a *collision-free* counter, and the `min` cashes in the best one. Widen the grid and collisions like this one grow rarer; the estimate only ever gets tighter, never wrong in the other direction.

</details>

## Your Turn

**Heavy hitters** — find the most frequent item in a stream, and confirm the one-sided guarantee. Add a stream with known true counts, then verify every estimate is `≥` its true count (never under) and that the sketch fingers the heaviest key.

```python run viz=grid
MOD = (1 << 31) - 1
class CountMinSketch:
    BASES = [131, 137, 139]
    def __init__(self, w):
        self.w = w; self.grid = [[0] * w for _ in range(len(self.BASES))]
    def _pos(self, x, i):
        h = 0
        for c in x: h = (h * self.BASES[i] + ord(c)) % MOD
        return h % self.w
    def add(self, x, count=1):
        for i in range(len(self.BASES)): self.grid[i][self._pos(x, i)] += count
    def estimate(self, x):
        return min(self.grid[i][self._pos(x, i)] for i in range(len(self.BASES)))

cms = CountMinSketch(w=16)
truth = {"x": 50, "y": 12, "z": 3, "p": 7, "q": 1}
for k, c in truth.items():
    cms.add(k, c)
print("every estimate >= true?:", all(cms.estimate(k) >= truth[k] for k in truth))   # True
heavy = max(truth, key=cms.estimate)
print("heavy hitter:", heavy, "->", cms.estimate(heavy))                              # x -> 50
```

```java run viz=grid
public class Main {
    static final long MOD = (1L << 31) - 1;
    static final int[] BASES = {131, 137, 139};
    static class CMS {
        int w; long[][] grid;
        CMS(int w) { this.w = w; grid = new long[BASES.length][w]; }
        int pos(String x, int i) {
            long h = 0;
            for (int j = 0; j < x.length(); j++) h = (h * BASES[i] + x.charAt(j)) % MOD;
            return (int) (h % w);
        }
        void add(String x, long c) { for (int i = 0; i < BASES.length; i++) grid[i][pos(x, i)] += c; }
        long estimate(String x) {
            long m = Long.MAX_VALUE;
            for (int i = 0; i < BASES.length; i++) m = Math.min(m, grid[i][pos(x, i)]);
            return m;
        }
    }
    public static void main(String[] args) {
        CMS cms = new CMS(16);
        String[] ks = {"x", "y", "z", "p", "q"}; long[] cs = {50, 12, 3, 7, 1};
        for (int i = 0; i < ks.length; i++) cms.add(ks[i], cs[i]);
        boolean ok = true; String heavy = ks[0];
        for (int i = 0; i < ks.length; i++) {
            if (cms.estimate(ks[i]) < cs[i]) ok = false;
            if (cms.estimate(ks[i]) > cms.estimate(heavy)) heavy = ks[i];
        }
        System.out.println("every estimate >= true?: " + ok);            // true
        System.out.println("heavy hitter: " + heavy + " -> " + cms.estimate(heavy));   // x -> 50
    }
}
```

Both print `True` then `x -> 50`. Every estimate honours the floor (never below the true count), and the argmax over estimates is the genuine heavy hitter `x`. This is the production pattern: a count-min sketch maintains approximate counts for *every* key in fixed memory, and a small heap of the top estimates surfaces the heavy hitters — exactly how stream processors find the trending hashtag or the IP flooding your servers.

## Reflect & Connect

- **Over-count only, so take the min.** Each counter is true-count plus collisions, hence `≥ true`. The minimum of the `d` cells is the tightest estimate and never dips below the truth — the one-sided error that makes it safe.
- **`d` rows are clean-cell chances, not averaging.** More rows give more independent shots at a collision-free counter; the `min` grabs the best. Wider rows shrink each collision. Tune `w = ⌈e/ε⌉`, `d = ⌈ln(1/δ)⌉` for error `εN` with confidence `1 − δ`.
- **Bloom filter's count cousin.** Bloom answers membership with possible false positives; count-min answers frequency with possible over-estimates. Same one-sided Monte Carlo bargain, lifted to counts — and **mergeable** (add two grids cell-wise) for distributed aggregation.
- **Heavy hitters fall out.** Maintain the sketch plus a small top-k heap of estimates; the argmax is the most frequent item. The basis of trending-topic, hot-key, and DDoS detection at stream scale.
- **The probabilistic-structures family.** [Skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-skip-list) (Las Vegas, exact), [bloom filter](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-bloom-filter) (membership), count-min (frequency), and HyperLogLog (cardinality, next) all share one idea — *hash into a small fixed array and accept a bounded, one-sided approximation* — applied to a different question each time.

## Recall

<details>
<summary><strong>Q:</strong> What does a count-min sketch estimate, and with what structure?</summary>

**A:** The frequency of each item in a stream, using a `d × w` grid of counters and `d` hash functions (one per row). `add` increments one counter per row; `estimate` takes the minimum of an item's `d` counters.

</details>
<details>
<summary><strong>Q:</strong> Why is the estimate the minimum of the cells, not the sum or average?</summary>

**A:** Every cell over-counts (collisions only add), so each is `≥` the true count. The minimum is the least-collided cell — the tightest over-estimate, still never below the truth. Summing or averaging would compound the collision error.

</details>
<details>
<summary><strong>Q:</strong> Can a count-min sketch ever under-count?</summary>

**A:** No. The item itself added to all its counters, and counters never decrease, so every cell — and thus their minimum — is `≥` the true count. The error is strictly one-sided (over-estimates only).

</details>
<details>
<summary><strong>Q:</strong> What do the dimensions `w` and `d` control?</summary>

**A:** `w` (columns) controls collision noise — wider rows make each cell over-count less (`w = ⌈e/ε⌉` bounds error to `εN`). `d` (rows) controls confidence — more rows give more chances for a clean cell, so the min is tight with probability `1 − δ` (`d = ⌈ln(1/δ)⌉`).

</details>
<details>
<summary><strong>Q:</strong> How does a count-min sketch find heavy hitters?</summary>

**A:** Maintain the sketch over the stream and a small heap of the highest estimates; the items with the largest estimated counts are the heavy hitters. Estimates never under-count, so a true heavy hitter is never missed.

</details>

## Sources & Verify

- **Cormode & Muthukrishnan** (2005), "An improved data stream summary: the count-min sketch and its applications", *J. Algorithms* — the original, with the `(ε, δ)` guarantees and heavy-hitter use.
- **Cormode** (2011), "Sketch techniques for approximate query processing" — count-min in the broader sketching landscape (vs HyperLogLog, AMS).
- **Apache** Spark/Flink and stream processors implement count-min for approximate frequency; the `5`/`3`/`7` exact estimates, the `[1,1,21]`-cells / `1`-min trace, and the `x -> 50` heavy hitter above come from the runnable blocks — re-run to verify (deterministic hashes; Python's built-in `hash` is per-process salted and unsuitable).
