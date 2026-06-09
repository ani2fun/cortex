---
title: Concurrent Hash Map
summary: "The hash map you actually use under concurrency. One global lock turns a map into a serial bottleneck; lock striping shards the table into N independently-locked segments for ~N-way write concurrency, reads stay lock-free, and Java 8+ refines this to per-bucket CAS."
prereqs:
  - linear-structures-hash-table-what-is-a-hash-table
  - concurrency-and-systems-cas-and-atomics
---

## Why It Exists

Wrap an ordinary [hash map](/cortex/data-structures-and-algorithms/linear-structures/hash-table/what-is-a-hash-table) in a `synchronized` block and it's correct — but it's a *queue with a hash interface*. Every `get` and `put` acquires one global lock, so threads can't touch the map at the same time even when they're working on completely unrelated keys. Under load, that single lock is the bottleneck and your eight cores run like one.

The fix is to lock *less of the map at once*. **Lock striping** partitions the table into `N` segments (stripes), each with its own lock; a write locks only the stripe its key hashes to, so two threads writing keys in *different* stripes proceed in parallel — up to `N`-way concurrency instead of 1. Reads can skip locking entirely (a bucket-head read is atomic). That's pre-Java-8 `ConcurrentHashMap`. Java 8+ pushes the idea further — no segment objects, just **CAS on each bucket head** with a short lock only when a bucket is contended, plus treeifying long collision chains. The principle is constant: shrink the unit of mutual exclusion from "the whole map" to "one stripe" to "one bucket."

## See It Work

A striped map routes each key to `stripe = hash(key) % N`; operations on different stripes are independent. (We simulate single-threaded for determinism — a real implementation gives each stripe its own lock and runs them in parallel. Python's built-in `hash` is per-process salted, so we use a fixed polynomial hash.)

```python run viz=array
def _hash(key):                                        # deterministic polynomial hash
    h = 0
    for c in str(key):
        h = (h * 131 + ord(c)) % (2**31 - 1)
    return h

class StripedHashMap:
    def __init__(self, n_stripes=4):
        self.n = n_stripes
        self.stripes = [dict() for _ in range(n_stripes)]   # each stripe also has its OWN lock in reality
    def stripe_of(self, key):
        return _hash(key) % self.n
    def put(self, key, value):                         # a real put locks ONLY stripes[stripe_of(key)]
        self.stripes[self.stripe_of(key)][key] = value
    def get(self, key):                                # reads are lock-free (atomic bucket-head read)
        return self.stripes[self.stripe_of(key)].get(key)

m = StripedHashMap(4)
for k, v in [("apple", 1), ("banana", 2), ("cherry", 3), ("date", 4)]:
    m.put(k, v)
print(m.get("apple"), m.get("cherry"))                 # 1 3
print(m.get("grape"))                                  # None
print({k: m.stripe_of(k) for k in ["apple", "banana", "cherry", "date"]})   # which stripe each lands in
```

```java run viz=array
import java.util.*;
public class Main {
    static final long MOD = (1L << 31) - 1;
    static long hash(String key) {                     // same polynomial hash as the Python block
        long h = 0;
        for (int i = 0; i < key.length(); i++) h = (h * 131 + key.charAt(i)) % MOD;
        return h;
    }
    static class StripedHashMap {
        int n; Map<String, Integer>[] stripes;
        @SuppressWarnings("unchecked")
        StripedHashMap(int n) { this.n = n; stripes = new HashMap[n]; for (int i = 0; i < n; i++) stripes[i] = new HashMap<>(); }
        int stripeOf(String k) { return (int) (hash(k) % n); }    // a real put locks ONLY this stripe
        void put(String k, int v) { stripes[stripeOf(k)].put(k, v); }
        Integer get(String k) { return stripes[stripeOf(k)].get(k); }   // reads are lock-free
    }
    public static void main(String[] args) {
        StripedHashMap m = new StripedHashMap(4);
        m.put("apple", 1); m.put("banana", 2); m.put("cherry", 3); m.put("date", 4);
        System.out.println(m.get("apple") + " " + m.get("cherry"));    // 1 3
        System.out.println(m.get("grape"));                            // null
        StringBuilder sb = new StringBuilder();
        for (String k : new String[]{"apple", "banana", "cherry", "date"}) sb.append(k).append("=").append(m.stripeOf(k)).append(" ");
        System.out.println(sb.toString().trim());
    }
}
```

Both print `1 3`, then `None`/`null`, then the stripe of each key (`apple=3 banana=2 cherry=1 date=2`). Correctness is identical to a plain hash map; the win is that `apple` (stripe 3) and `banana` (stripe 2) live in *separate* stripes, so concurrent writers to them never block each other.

## How It Works

The whole design is about *which* lock — and how little of the map it covers:

```d2
direction: right
global: "ONE global lock\n-> all ops serialize (1-way)\nevery thread waits on every other" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
stripe: "N STRIPE locks (lock striping)\nput locks only stripe = hash(key) % N\n-> different stripes -> CONCURRENT (~N-way)\nsame stripe -> still serialize" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
reads: "READS lock-free\n(atomic / volatile bucket-head read;\nreaders never block writers or each other)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
bucket: "Java 8+: per-BUCKET CAS\n(no segment objects; lock only a contended bin;\ntreeify chains > 8)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
global -> stripe: "shrink the lock"
stripe -> reads
stripe -> bucket: "shrink further"
```

<p align="center"><strong>Shrink the unit of mutual exclusion: from the whole map (1-way), to one stripe (~N-way), to one bucket (Java 8+). Reads stay lock-free throughout — a bucket-head read is atomic, so readers never block.</strong></p>

Three load-bearing facts:

- **Striping turns 1-way into N-way — bounded by N.** With `N` stripes, writes to different stripes run in parallel, so throughput scales with `N` (the *concurrency level*). But two keys that hash to the *same* stripe still serialize on that stripe's lock — striping reduces contention, it doesn't eliminate it. More stripes → less collision but more memory and weaker per-stripe locality; Java's default concurrency level was 16. Per-bucket CAS (Java 8+) is the limit case: the "stripe" is a single bucket.
- **Reads are lock-free.** A `get` reads the bucket head atomically (a `volatile` reference in Java) and walks the chain — no lock taken, so readers never block writers or each other. This is the common case (maps are read-heavy), and it's why a concurrent map can hugely outperform a synchronized one even with a single writer.
- **Linearizable ops, weakly-consistent iterators.** Each `put`/`get`/`remove` appears to happen atomically at some instant (linearizable) — you never see a half-updated entry. But an *iterator* reflects the map at *some* point during its traversal and may or may not see concurrent updates; it's guaranteed never to throw `ConcurrentModificationException` and never to crash, but `size()` is approximate under concurrency. You trade a globally-consistent snapshot for non-blocking iteration.

> **Key takeaway.** A concurrent hash map shrinks the lock. **Lock striping** shards the table into `N` independently-locked segments, giving up to `N`-way write concurrency (keys on different stripes never block; same-stripe keys still serialize). **Reads are lock-free** (atomic bucket-head read). Java 8+ refines striping to **per-bucket CAS** (lock only a contended bin, treeify long chains). Operations are linearizable; iterators are weakly consistent.

## Trace It

The selling point — "writes don't block each other" — has an asterisk, and the asterisk is the stripe assignment.

**Predict before you run:** a synchronized map serialises *all* writes. With `N = 4` stripes, two threads write two different keys. Are they *always* able to proceed concurrently, or does it depend on something?

```python run viz=graph viz-kind=graph
from collections import defaultdict
def _hash(key):
    h = 0
    for c in str(key):
        h = (h * 131 + ord(c)) % (2**31 - 1)
    return h

keys = ["apple", "banana", "cherry", "date", "fig", "grape", "kiwi", "lime"]
N = 4
stripe = {k: _hash(k) % N for k in keys}
by_stripe = defaultdict(list)
for k, s in stripe.items():
    by_stripe[s].append(k)

print("keys per stripe:", dict(by_stripe))
diff = next((a, b) for a in keys for b in keys if a < b and stripe[a] != stripe[b])
same = next(ks for ks in by_stripe.values() if len(ks) >= 2)
print(f"different stripes -> CONCURRENT: {diff}  (stripes {stripe[diff[0]]}, {stripe[diff[1]]})")
print(f"same stripe -> SERIALIZE: {same[0]}, {same[1]}  (both stripe {stripe[same[0]]})")
```

<details>
<summary><strong>Reveal</strong></summary>

It **depends on whether the two keys hash to the same stripe.** With 4 stripes and 8 keys, the assignment is uneven (`apple→3`, `banana→2`, `cherry→1`, `date→2`, ...): some stripes hold one key, others hold several. `apple` and `banana` land on *different* stripes (3 and 2), so two threads writing them lock *different* locks and proceed **concurrently** — exactly the speedup striping promises. But `banana` and `date` both hash to stripe 2, so two threads writing them contend on the *same* lock and **serialize**, no better than the global-lock case for that pair. That's the fundamental limit of striping: it reduces the *probability* of contention by a factor of `N`, but two keys that collide on a stripe still block each other. With random keys and `N` stripes, you get roughly `N`-way concurrency *on average*, never a guarantee for any specific pair. This is precisely why Java 8 moved to **per-bucket** granularity — the "stripe" shrank to a single bucket, so two writers collide only if their keys land in the *same bucket* (far rarer than the same stripe), and even then it's a short CAS or a brief bin lock rather than a coarse segment lock.

</details>

## Your Turn

**Profile the concurrency** as you add stripes. Spread the same keys over `1`, `4`, and `16` stripes and watch the per-stripe load fall and the achievable parallelism rise.

```python run viz=array
def _hash(key):
    h = 0
    for c in str(key):
        h = (h * 131 + ord(c)) % (2**31 - 1)
    return h

class StripedHashMap:
    def __init__(self, n_stripes):
        self.n = n_stripes
        self.stripes = [dict() for _ in range(n_stripes)]
    def put(self, key, value):
        self.stripes[_hash(key) % self.n][key] = value

def profile(num_keys, n_stripes):
    m = StripedHashMap(n_stripes)
    for i in range(num_keys):
        m.put(f"key{i}", i)
    loads = [len(s) for s in m.stripes]
    distinct = sum(1 for L in loads if L > 0)          # stripes actually used = max parallelism
    return max(loads), distinct

for n in (1, 4, 16):
    max_load, distinct = profile(64, n)
    print(f"{n:>2} stripes: busiest stripe holds {max_load} keys, {distinct} stripes used -> up to {distinct}-way concurrency")
```

```java run viz=array
import java.util.*;
public class Main {
    static final long MOD = (1L << 31) - 1;
    static long hash(String key) {
        long h = 0;
        for (int i = 0; i < key.length(); i++) h = (h * 131 + key.charAt(i)) % MOD;
        return h;
    }
    public static void main(String[] args) {
        for (int n : new int[]{1, 4, 16}) {
            @SuppressWarnings("unchecked")
            Map<String, Integer>[] stripes = new HashMap[n];
            for (int i = 0; i < n; i++) stripes[i] = new HashMap<>();
            for (int i = 0; i < 64; i++) stripes[(int) (hash("key" + i) % n)].put("key" + i, i);
            int max = 0, distinct = 0;
            for (Map<String, Integer> s : stripes) { max = Math.max(max, s.size()); if (!s.isEmpty()) distinct++; }
            System.out.println(n + " stripes: busiest " + max + " keys, " + distinct + " used -> up to " + distinct + "-way");
        }
    }
}
```

Both print the same scaling: `1 stripe` → 1-way, busiest holds all `64`; `4 stripes` → 4-way, busiest ~`17`; `16 stripes` → 16-way, busiest ~`5`. More stripes spread the keys, so the busiest lock guards fewer keys and more writers run in parallel — at the cost of `N` lock objects and worse cache locality. That trade (concurrency vs memory/locality) is exactly the `concurrencyLevel` knob older `ConcurrentHashMap` exposed, and why Java 8's per-bucket scheme sidesteps it.

## Reflect & Connect

- **Shrink the lock.** Global lock (1-way) → stripe locks (`N`-way) → per-bucket CAS (Java 8+). Each step narrows the unit of mutual exclusion so more independent operations run at once.
- **Striping's limit is collision on a stripe.** Two keys on different stripes are concurrent; two on the same stripe still serialize. You get `~N`-way concurrency *on average*, never a guarantee — which is why finer (per-bucket) granularity wins.
- **Reads are lock-free.** A bucket-head read is atomic, so the read-heavy common case never blocks. This alone makes a concurrent map far faster than a synchronized one.
- **Linearizable, weakly-consistent iterators.** Single operations appear atomic; iterators reflect *some* point in time, never crash, and `size()` is approximate under concurrency. You trade a consistent snapshot for non-blocking traversal.
- **It builds on the rest of Part 10.** Per-bucket updates are [CAS](/cortex/data-structures-and-algorithms/concurrency-and-systems/cas-and-atomics) retry loops (like the [lock-free queue](/cortex/data-structures-and-algorithms/concurrency-and-systems/lock-free-queue)); safe resize and node reclamation lean on [RCU/hazard pointers](/cortex/data-structures-and-algorithms/concurrency-and-systems/rcu-and-hazard-pointers). The concurrent hash map is where all of Part 10's primitives meet in one production structure.

## Recall

<details>
<summary><strong>Q:</strong> Why is a <code>synchronized</code> HashMap a bottleneck under concurrency?</summary>

**A:** It has one global lock, so every `get`/`put` serialises — threads can't operate even on unrelated keys at the same time. It behaves like a queue with a hash interface.

</details>
<details>
<summary><strong>Q:</strong> What is lock striping, and what concurrency does it give?</summary>

**A:** Partition the table into `N` segments, each with its own lock; a write locks only the stripe `hash(key) % N`. Writes to different stripes proceed in parallel — up to `N`-way concurrency. Two keys on the same stripe still serialize.

</details>
<details>
<summary><strong>Q:</strong> Why can reads be lock-free?</summary>

**A:** A `get` reads the bucket head atomically (a `volatile` reference) and walks the chain without taking a lock, so readers never block writers or each other. The read-heavy common case scales freely.

</details>
<details>
<summary><strong>Q:</strong> How does Java 8+ improve on segment striping?</summary>

**A:** It drops segment objects and uses **CAS on each bucket head** directly, taking a short lock only on a contended bin, and treeifies collision chains longer than ~8. The lock unit shrinks from a segment to a single bucket, so collisions (and thus serialization) are far rarer.

</details>
<details>
<summary><strong>Q:</strong> What consistency does a concurrent hash map provide?</summary>

**A:** Individual operations are linearizable (each appears atomic at some instant — no half-updated entries). Iterators are weakly consistent: they reflect the map at some point, never throw `ConcurrentModificationException`, and `size()` is approximate under concurrent updates.

</details>

## Sources & Verify

- **Herlihy & Shavit**, *The Art of Multiprocessor Programming* (2nd ed.) — striped/refinable hash sets, lock-free reads, and linearizability.
- **Doug Lea**, `java.util.concurrent.ConcurrentHashMap` (and its Java 8 rewrite to per-bin CAS + treeification) — the canonical production concurrent map; **Cliff Click**, "A Lock-Free Wait-Free Hash Table" — a fully lock-free design.
- The `1 3` / `None` gets, the `apple=3 banana=2 cherry=1 date=2` stripe assignments, the concurrent-vs-serialize pair, and the `1`/`4`/`16`-way profile above come from the runnable blocks — the code *simulates* striping single-threaded (real stripes each carry a lock and run in parallel); Python's built-in `hash` is salted, so a fixed polynomial hash is used. Re-run to verify.
