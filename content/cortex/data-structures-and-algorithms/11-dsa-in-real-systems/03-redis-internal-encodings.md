---
title: "Redis Internal Encodings"
summary: "Redis stores a Set of 5 integers one way and a Set of 5000 another way — switching encodings automatically as data grows. A small collection lives in a packed contiguous layout (intset, listpack) that beats a hash table on both memory and cache; past a threshold it upgrades to the asymptotic structure. The constants Big-O hides, made into a production design."
prereqs:
  - linear-structures-arrays-what-is-an-array
  - probabilistic-and-advanced-skip-list
---

## Why It Exists

Big-O says a hash table's `O(1)` lookup beats an array's `O(n)` scan, so a Set should always be a hash table — right? For a Set of ten integers, **wrong**. A ten-entry hash table burns ~1 KB on a bucket array, per-node `dictEntry` structs, pointers, and padding, all scattered across memory so each probe risks a cache miss. The same ten integers packed into a sorted array — Redis's `intset` — fit in one ~50-byte allocation that lives in a cache line or two; a binary search over it finishes before the hash table has finished chasing its first pointer. These are [the constants Big-O hides](/cortex/data-structures-and-algorithms/foundations/memory-model-and-cache), and for small `n` they flip the verdict.

Redis turns that into policy. Each collection type has a **compact encoding** for when it's small (packed, contiguous, cache-friendly, `O(n)` ops) and a **general encoding** for when it's large (a real hash table or skip list, `O(1)`/`O(log n)`, more overhead). Redis transparently upgrades from one to the other when the collection crosses a configurable threshold — `OBJECT ENCODING mykey` shows which one a key is using right now. The result is one of the cleanest "memory hierarchy as a design decision" stories in open-source code, and the reason Redis fits so much in so little RAM.

## See It Work

A Set of integers starts as an `intset` — a sorted packed array with binary-search membership. The moment it outgrows the threshold, Redis converts it to a hash table. The stored data is identical; only the physical encoding changes.

```python run viz=array
import bisect
class AdaptiveSet:
    THRESHOLD = 4                          # tiny for the demo; Redis default is 512
    def __init__(self):
        self.intset = []                   # sorted packed array of ints (the compact encoding)
        self.hashset = None                # general hashtable, allocated on upgrade
    def encoding(self):
        return "hashtable" if self.hashset is not None else "intset"
    def add(self, x):
        if self.hashset is not None:
            self.hashset.add(x); return
        i = bisect.bisect_left(self.intset, x)          # keep the array sorted
        if i < len(self.intset) and self.intset[i] == x: return
        self.intset.insert(i, x)
        if len(self.intset) > self.THRESHOLD:           # outgrew the compact encoding
            self.hashset = set(self.intset); self.intset = []   # convert to hashtable
    def contains(self, x):
        if self.hashset is not None:
            return x in self.hashset                     # O(1) hash lookup
        i = bisect.bisect_left(self.intset, x)           # O(log n) binary search
        return i < len(self.intset) and self.intset[i] == x

s = AdaptiveSet()
for x in [50, 20, 40, 10]:                  # 4 ints: within threshold
    s.add(x)
print("size 4:", s.encoding(), "| contains 40?", s.contains(40))
s.add(30)                                    # 5th int pushes past THRESHOLD=4
print("size 5:", s.encoding(), "| contains 40?", s.contains(40))
```

```java run viz=array
import java.util.*;
public class Main {
    static class AdaptiveSet {
        static final int THRESHOLD = 4;
        List<Integer> intset = new ArrayList<>();   // sorted packed ints (the compact encoding)
        HashSet<Integer> hashset = null;            // general hashtable, allocated on upgrade
        String encoding() { return hashset != null ? "hashtable" : "intset"; }
        void add(int x) {
            if (hashset != null) { hashset.add(x); return; }
            int i = Collections.binarySearch(intset, x);
            if (i >= 0) return;                      // already present
            intset.add(-i - 1, x);                   // insert at sorted position
            if (intset.size() > THRESHOLD) { hashset = new HashSet<>(intset); intset = new ArrayList<>(); }
        }
        boolean contains(int x) {
            if (hashset != null) return hashset.contains(x);    // O(1)
            return Collections.binarySearch(intset, x) >= 0;    // O(log n)
        }
    }
    public static void main(String[] a) {
        AdaptiveSet s = new AdaptiveSet();
        for (int x : new int[]{50, 20, 40, 10}) s.add(x);
        System.out.println("size 4: " + s.encoding() + " | contains 40? " + s.contains(40));
        s.add(30);
        System.out.println("size 5: " + s.encoding() + " | contains 40? " + s.contains(40));
    }
}
```

Both print `size 4: intset | contains 40? true`, then `size 5: hashtable | contains 40? true`. At four elements the Set is a compact sorted array searched in `O(log n)`; the fifth element trips the threshold and Redis rebuilds it as a hash table. Membership stays correct the whole way — `contains(40)` is true before and after — because the *logical* Set never changed, only its bytes.

## How It Works

Every Redis collection type has this small-vs-large split:

| Type | Small encoding | Threshold (default) | Large encoding |
|---|---|---|---|
| **Set** (all ints) | `intset` (sorted packed array) | `set-max-intset-entries` (512) | `hashtable` |
| **Set** (mixed) | `listpack` | `set-max-listpack-entries` (128) | `hashtable` |
| **Hash** | `listpack` | `hash-max-listpack-entries` (128) | `hashtable` |
| **Sorted Set** | `listpack` | `zset-max-listpack-entries` (128) | `skiplist` + `hashtable` |
| **List** | `listpack` | `list-max-listpack-size` (128) | `quicklist` |

```d2
direction: down
small: "SMALL collection\n(few entries — fits a cache line or two)" {style.fill: "#fef9c3"}
packed: "PACKED encoding (intset / listpack)\none contiguous allocation, O(n) scan\nbut cache-local + 4-10x less memory" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
general: "GENERAL encoding (hashtable / skiplist)\nO(1)/O(log n) ops, bucket+node overhead\n(hashtable rehashes incrementally: 1 bucket/op)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
small -> packed: "below threshold"
packed -> general: "grows past the threshold\nOR (intset) a non-integer is added\n— ONE-WAY: never reverts"
```

<p align="center"><strong>Small collections live in a packed, cache-friendly layout; crossing the size threshold (or, for an intset, adding a non-integer) upgrades to the asymptotic structure. The upgrade is one-way.</strong></p>

The encodings themselves:

- **`listpack` (and the `ziplist` it replaced).** A single contiguous byte buffer of size-prefixed entries — one allocation for the whole collection. A 10-field hash is ~200 bytes here versus ~1 KB as a hash table: **5× smaller**, and contiguous so it's cache-friendly. Every operation is `O(n)` (scan from the front), but for ≤128 entries the linear scan over packed bytes beats a hash table's function call + pointer chase. Redis 7 replaced `ziplist` with `listpack` to kill the old "cascade update" bug, where one entry's size change could trigger `O(n²)` rewrites.
- **`intset`.** The Set-of-integers special case: a sorted array of the narrowest int type that fits (int16/32/64). Membership is binary search, `O(log n)`; it beats a hash table by 4–8× memory for the very common int-only set (user IDs, post IDs). Two triggers upgrade it to a hash table — size, **or** a non-integer member ([Trace It](#trace-it)).
- **`hashtable` and `skiplist`.** The general fallbacks. Redis's hash table (`dict.c`) uses separate chaining with **incremental rehashing**: when it grows, it keeps *both* the old and new tables and migrates just **one bucket per operation**, so no single op ever stalls the single-threaded server with an `O(n)` rehash. A Sorted Set uses *both* a skip list (ordered, for `ZRANGE`) and a hash table (member→score, for `O(1)` `ZSCORE`) — Redis picks a skip list over a balanced tree purely for simpler code, since it's single-threaded and doesn't need the concurrency story.

> **Key takeaway.** Redis stores each collection in a **compact, contiguous encoding** (`intset`, `listpack`) while it's small — fewer bytes, better cache locality, and `O(n)` ops whose constants beat a hash table's for small `n` — then transparently **upgrades to the asymptotic structure** (`hashtable`, `skiplist`) once it crosses a configurable threshold. It's the memory hierarchy turned into policy: Big-O picks the large-`n` winner, but the constants Big-O hides pick the small-`n` winner, and Redis follows whichever applies. The upgrade is one-way, and the hash table rehashes incrementally so the single-threaded server never stalls.

## Trace It

The size threshold isn't the only thing that can force an upgrade. An `intset` has a structural constraint: it holds *only* integers.

**Predict before you run:** a Set holds just three integers — `size 3`, nowhere near the 512 threshold, comfortably an `intset`. You add the string `"hello"`. Does it stay an `intset` (it's still tiny), or convert?

```python run viz=array
class IntsetOrHash:
    THRESHOLD = 512
    def __init__(self): self.intset = []; self.hashset = None
    def encoding(self): return "hashtable" if self.hashset is not None else "intset"
    def add(self, x):
        if self.hashset is not None: self.hashset.add(x); return
        if not isinstance(x, int):                  # an intset holds ONLY integers
            self.hashset = set(self.intset); self.hashset.add(x); self.intset = []
            return
        if x not in self.intset: self.intset.append(x)
        if len(self.intset) > self.THRESHOLD:
            self.hashset = set(self.intset); self.intset = []

s = IntsetOrHash()
for x in [7, 3, 9]: s.add(x)          # 3 integers, far below threshold 512
print("3 ints (size 3):", s.encoding())
s.add("hello")                         # add a NON-integer
print("after adding a string:", s.encoding())
```

<details>
<summary><strong>Reveal</strong></summary>

It converts: `3 ints (size 3): intset`, then `after adding a string: hashtable`. The string forced the upgrade even though the Set has only four elements, because an `intset` *physically cannot represent* a non-integer — it's a packed array of fixed-width ints. So there are **two independent triggers** for the upgrade: crossing the size threshold (`set-max-intset-entries`), **or** adding an element the compact encoding can't hold. (For a mixed-type Set, Redis goes intset → `listpack` first if still small, then → `hashtable`; here we model the direct jump.) This is the general shape of adaptive encodings: a representation is chosen on an *assumption* about the data — "small" or "all integers" — and the moment the data violates that assumption, the representation has to change. The encoding is a bet, and the upgrade is how Redis pays up when the bet is lost.

</details>

## Your Turn

You've seen the Set grow *into* a hash table. Now shrink it back down.

**Predict:** a Set grows to 5 elements and upgrades to `hashtable`. You then delete 3 of them, leaving just 2 — far below the threshold again. Does Redis revert to the compact `intset`, or stay a `hashtable`?

```python run viz=array
class AdaptiveSet:
    THRESHOLD = 4
    def __init__(self): self.intset = []; self.hashset = None
    def encoding(self): return "hashtable" if self.hashset is not None else "intset"
    def add(self, x):
        if self.hashset is not None: self.hashset.add(x); return
        if x not in self.intset: self.intset.append(x); self.intset.sort()
        if len(self.intset) > self.THRESHOLD:
            self.hashset = set(self.intset); self.intset = []
    def remove(self, x):
        if self.hashset is not None: self.hashset.discard(x)   # stays a hashtable...
        elif x in self.intset: self.intset.remove(x)
    def size(self):
        return len(self.hashset) if self.hashset is not None else len(self.intset)

s = AdaptiveSet()
for x in [1, 2, 3, 4, 5]: s.add(x)         # 5 > THRESHOLD 4 -> upgrades
print("after growth:", s.encoding(), "size", s.size())
for x in [1, 2, 3]: s.remove(x)            # shrink back to 2 elements
print("after shrink:", s.encoding(), "size", s.size())
```

```java run viz=array
import java.util.*;
public class Main {
    static class AdaptiveSet {
        static final int THRESHOLD = 4;
        List<Integer> intset = new ArrayList<>();
        HashSet<Integer> hashset = null;
        String encoding() { return hashset != null ? "hashtable" : "intset"; }
        void add(int x) {
            if (hashset != null) { hashset.add(x); return; }
            if (!intset.contains(x)) { intset.add(x); Collections.sort(intset); }
            if (intset.size() > THRESHOLD) { hashset = new HashSet<>(intset); intset = new ArrayList<>(); }
        }
        void remove(int x) {
            if (hashset != null) hashset.remove(x);             // stays a hashtable...
            else intset.remove(Integer.valueOf(x));
        }
        int size() { return hashset != null ? hashset.size() : intset.size(); }
    }
    public static void main(String[] a) {
        AdaptiveSet s = new AdaptiveSet();
        for (int x : new int[]{1, 2, 3, 4, 5}) s.add(x);   // 5 > THRESHOLD 4 -> upgrades
        System.out.println("after growth: " + s.encoding() + " size " + s.size());
        for (int x : new int[]{1, 2, 3}) s.remove(x);      // shrink back to 2
        System.out.println("after shrink: " + s.encoding() + " size " + s.size());
    }
}
```

Both print `after growth: hashtable size 5`, then `after shrink: hashtable size 2`. The encoding stays `hashtable` even though the Set is now smaller than the threshold — **the upgrade is one-way.** Redis never downgrades an encoding on delete: checking "could I shrink back?" on every removal would cost more than it saves, and a collection that grew once tends to grow again. The practical consequence is a real production gotcha — a Hash that briefly spiked over the threshold keeps paying the hash-table memory price forever; to reclaim the compact encoding you must `DUMP`/`RESTORE` or re-insert into a fresh key.

## Reflect & Connect

- **Big-O is the large-`n` story; constants are the small-`n` story.** A packed array's `O(n)` scan beats a hash table's `O(1)` for small `n` because one contiguous allocation crushes per-node overhead and cache misses. Redis encodes both verdicts and switches at the crossover.
- **The encoding is a bet on the data.** "Small" or "all integers" are assumptions; the upgrade is what happens when the data breaks them. Two triggers: size, or an element the compact form can't hold.
- **Upgrades are one-way.** Reversing on every delete would cost more than it saves, so a collection that spiked over the threshold keeps the heavier encoding. Know it when you size Redis.
- **Incremental rehashing keeps a single thread responsive.** The dual-table, one-bucket-per-op migration means no operation ever stalls — the same "amortize the big cost across many small steps" idea behind [dynamic arrays](/cortex/data-structures-and-algorithms/linear-structures/arrays/dynamic-arrays) and incremental GC.
- **Same theme as the other systems.** Like [Postgres `nbtree`](/cortex/data-structures-and-algorithms/dsa-in-real-systems/postgres-b-tree-and-the-write-path) and the [CFS scheduler](/cortex/data-structures-and-algorithms/dsa-in-real-systems/linux-red-black-tree-in-the-cfs-scheduler), the textbook structure is only the starting point — the production win is in the engineering around it (encoding choice, cache locality, incremental work).

## Recall

<details>
<summary><strong>Q:</strong> Why does a small collection's packed encoding (intset/listpack) beat a hash table?</summary>

**A:** It's one contiguous allocation with size-prefixed entries — no bucket array, no per-node structs, no pointer indirection — so it uses 4–10× less memory and stays cache-local. For small `n` the `O(n)` scan over packed bytes beats the hash table's `O(1)` lookup once the hash-function and pointer-chasing constants are counted.

</details>
<details>
<summary><strong>Q:</strong> What two things can trigger a Set of integers (intset) to upgrade to a hash table?</summary>

**A:** (1) Growing past `set-max-intset-entries` (default 512), or (2) adding a non-integer member — an intset is a packed array of fixed-width integers and physically can't store anything else.

</details>
<details>
<summary><strong>Q:</strong> Is an encoding upgrade reversible?</summary>

**A:** No — it's one-way. Deleting elements back below the threshold does not revert a hashtable to intset/listpack. To reclaim the compact encoding you must dump and restore (or re-insert into a fresh key).

</details>
<details>
<summary><strong>Q:</strong> What is incremental rehashing and what problem does it solve?</summary>

**A:** When Redis's hash table grows, it keeps both the old and new tables and migrates one bucket per operation instead of all at once. This avoids the `O(n)` stall a single rehash would cause on the single-threaded server — no operation ever takes more than a few microseconds.

</details>
<details>
<summary><strong>Q:</strong> How is a Redis Sorted Set stored, and why a skip list?</summary>

**A:** As two structures in tandem: a skip list keyed by score (for `ZRANGE`/range queries) plus a hash table mapping member→score (for `O(1)` `ZSCORE`/`ZRANK`). Redis chose a skip list over a balanced tree for simpler code — being single-threaded, it doesn't need the skip list's concurrency advantages.

</details>

## Sources & Verify

- **Redis source**: `src/intset.c` (packed sorted ints), `src/listpack.c` (contiguous encoding, Redis 7+), `src/dict.c` (`dictRehashStep` — incremental rehashing), `src/t_zset.c` (skip list + hash table), `src/quicklist.c`. The [Redis docs on memory optimization](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/memory-optimization/) describe the encoding thresholds.
- **Salvatore Sanfilippo (antirez)**, Redis design notes and the `t_zset.c` comments — the rationale for skip lists over balanced trees and the encoding-per-size approach.
- **[Memory Model and Cache](/cortex/data-structures-and-algorithms/foundations/memory-model-and-cache)** — why contiguous, cache-local layouts win for small `n` (the constants Big-O hides).
- The size-triggered transition (`intset` at 4 → `hashtable` at 5, membership preserved), the non-integer trigger (a 3-element set upgrading on a string), and the one-way upgrade (`hashtable` staying after shrinking to 2) all come from the runnable blocks above (deterministic models of the encoding switch) — re-run to verify.
