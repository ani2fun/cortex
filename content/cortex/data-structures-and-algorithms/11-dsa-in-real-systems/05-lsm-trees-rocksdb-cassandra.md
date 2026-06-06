---
title: "LSM Trees in RocksDB and Cassandra"
summary: "The Log-Structured Merge tree — the write-optimised alternative to the B-tree behind RocksDB, Cassandra, LevelDB, and CockroachDB. Buffer writes in memory, flush them as immutable sorted files, and merge those files in the background. Sequential writes instead of random ones, paid for with read amplification that compaction keeps in check."
prereqs:
  - trees-b-tree-introduction-to-b-trees
  - probabilistic-and-advanced-skip-list
  - probabilistic-and-advanced-bloom-filter
---

## Why It Exists

A [B-tree](/cortex/data-structures-and-algorithms/trees-b-tree-introduction-to-b-trees) — the structure behind [Postgres `nbtree`](/cortex/data-structures-and-algorithms/dsa-in-real-systems-postgres-b-tree-and-the-write-path) — is **read-optimised**: a point lookup is a handful of seeks, range scans walk linked leaves. But every *write* modifies a page in place, which is a **random** write to disk. On spinning disks a random write is hundreds of times slower than a sequential one, and even on SSDs random writes cost extra wear. For a write-heavy workload — metrics, logs, event streams, a busy key-value store — the B-tree's random-write tax dominates.

The **LSM tree (Log-Structured Merge tree)** flips the trade-off by *never writing randomly*. Writes go into an in-memory buffer (the **memtable**) plus a sequential **WAL** for durability. When the memtable fills, it's flushed to disk as one **immutable, sorted file** — an **SSTable** — written sequentially, start to finish. More writes make more SSTables, and a background **compaction** merges them (a streaming merge-sort) into fewer, larger files. Every disk write is sequential; the price is that a read may have to consult several SSTables, and compaction burns CPU and bandwidth. That's the bargain RocksDB, Cassandra, LevelDB, ScyllaDB, HBase, and CockroachDB all take — trade read and space amplification for write throughput.

## See It Work

The defining behavior: writes accumulate newest-on-top, and a read returns the *first* (newest) version it finds. An update doesn't overwrite the old value on disk — it just shadows it.

```python run viz=array
TOMBSTONE = "<deleted>"
class LSM:
    def __init__(self):
        self.memtable = {}      # in-memory buffer holding the newest writes
        self.sstables = []      # immutable on-disk files, newest first
    def put(self, k, v):
        self.memtable[k] = v
    def flush(self):            # memtable fills -> frozen into an immutable SSTable
        self.sstables.insert(0, dict(self.memtable)); self.memtable = {}
    def get(self, k):
        for layer in [self.memtable] + self.sstables:   # newest -> oldest, first hit wins
            if k in layer:
                return None if layer[k] == TOMBSTONE else layer[k]
        return None

db = LSM()
db.put("user:1", "alice")
db.flush()                       # "alice" now lives in an SSTable on disk
db.put("user:1", "alice2")       # update -> lands in the fresh memtable (newer)
print("read user:1 ->", db.get("user:1"))
print("# sstables:", len(db.sstables), "| old value still on disk:", db.sstables[0]["user:1"])
```

```java run viz=array
import java.util.*;
public class Main {
    static final String TOMBSTONE = "<deleted>";
    static class LSM {
        Map<String,String> memtable = new LinkedHashMap<>();   // newest writes, in memory
        List<Map<String,String>> sstables = new ArrayList<>(); // immutable files, newest first
        void put(String k, String v) { memtable.put(k, v); }
        void flush() { sstables.add(0, new LinkedHashMap<>(memtable)); memtable = new LinkedHashMap<>(); }
        String get(String k) {
            List<Map<String,String>> layers = new ArrayList<>(); layers.add(memtable); layers.addAll(sstables);
            for (Map<String,String> layer : layers)            // newest -> oldest, first hit wins
                if (layer.containsKey(k)) { String v = layer.get(k); return v.equals(TOMBSTONE) ? null : v; }
            return null;
        }
    }
    public static void main(String[] x) {
        LSM db = new LSM();
        db.put("user:1", "alice");
        db.flush();                       // "alice" now lives in an SSTable
        db.put("user:1", "alice2");       // update -> fresh memtable (newer)
        System.out.println("read user:1 -> " + db.get("user:1"));
        System.out.println("# sstables: " + db.sstables.size() + " | old value still on disk: " + db.sstables.get(0).get("user:1"));
    }
}
```

Both print `read user:1 -> alice2`, then `# sstables: 1 | old value still on disk: alice`. The update to `alice2` never touched the SSTable — it went into the new memtable, which the read checks *first*. The stale `alice` is still sitting on disk, harmlessly shadowed, until a future compaction discards it. Writes are append-only; reads layer newest-over-oldest.

## How It Works

The whole machine is "buffer in memory, flush sequentially, merge in the background":

```d2
direction: down
w: "WRITE (k, v)" {style.fill: "#fef9c3"}
wal: "WAL — sequential append (durable)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
mem: "MEMTABLE — in-memory sorted (skiplist)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
sst: "SSTables — immutable sorted files on disk\n(newest → oldest; each has a Bloom filter)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
comp: "COMPACTION — background merge-sort:\nkeep newest per key, drop tombstones\n→ fewer files, bounded read amplification" {style.fill: "#e9d5ff"; style.stroke: "#9333ea"}
r: "READ (k): memtable first, then SSTables\nnewest → oldest, first hit wins" {style.fill: "#fed7aa"; style.stroke: "#ea580c"}
w -> wal: "1. durability"
w -> mem: "2. apply"
mem -> sst: "flush when full"
sst -> comp: "merge"
r -> mem
r -> sst
```

<p align="center"><strong>Writes hit the WAL (durability) and the in-memory memtable (speed); a full memtable flushes to an immutable SSTable; compaction merges SSTables in the background. Reads check memtable then SSTables newest→oldest, with a Bloom filter skipping files that can't hold the key.</strong></p>

The load-bearing pieces:

- **The write path is all sequential.** A write appends to the WAL (durability) and inserts into the memtable — usually a [skip list](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-skip-list), chosen because it sorts on insert and handles concurrent writers cleanly. At ~64 MB the memtable is frozen and flushed to an SSTable: a sorted, compressed file written in one sequential pass. No random page writes ever happen.
- **Reads layer newest-over-oldest, and Bloom filters cut the cost.** A lookup checks the memtable, then each SSTable from newest to oldest, returning the first hit. Left unchecked that's one disk seek *per SSTable*, so each SSTable carries a [Bloom filter](/cortex/data-structures-and-algorithms/probabilistic-and-advanced-bloom-filter): if it says "definitely not here," the read skips that file entirely with no disk access. This is what keeps read amplification survivable.
- **Compaction bounds the file count — two strategies.** Without merging, SSTables pile up and reads slow forever. Compaction streams several sorted SSTables together into one (keeping the newest value per key, dropping tombstones — [Your Turn](#your-turn)). **Leveled** compaction (RocksDB default) keeps non-overlapping levels each ~10× the last, so a read checks ~one file per level (~7 total) — low read amplification, but every key is rewritten ~70× over its life. **Tiered** compaction (Cassandra's classic default) merges K equal-size files into the next tier — fewer rewrites (lower write amplification) but more files to check per read. You can't win everywhere: the **RUM conjecture** says **R**ead, **U**pdate, and **M**emory amplification trade off against each other. LSM spends read and memory to minimise update cost; the B-tree does the reverse.

> **Key takeaway.** An LSM tree turns random writes into sequential ones: buffer writes in an in-memory **memtable** (plus a WAL), flush a full memtable to an **immutable sorted SSTable**, and **compact** SSTables in the background. Reads layer newest-over-oldest (a Bloom filter per SSTable skips files that can't hold the key), so an update *shadows* the old value rather than overwriting it, and a delete writes a **tombstone**. The cost is read and space amplification that compaction keeps bounded — the **RUM** trade-off. It's the write-optimised mirror of the read-optimised [B-tree](/cortex/data-structures-and-algorithms/dsa-in-real-systems-postgres-b-tree-and-the-write-path); pick by whether your workload writes or reads more.

## Trace It

If an update just shadows the old value, how do you *delete*? There's no random write to go erase the on-disk copy.

**Predict before you run:** a key `k` is written and flushed to an SSTable. Then you delete `k` (which, in an LSM, writes a **tombstone** — a "deleted" marker — into the memtable). A read for `k` follows. Does it return the old value (still sitting in the SSTable), or "not found"?

```python run viz=array
TOMBSTONE = "<deleted>"
class LSM:
    def __init__(self): self.memtable = {}; self.sstables = []
    def put(self, k, v): self.memtable[k] = v
    def delete(self, k): self.memtable[k] = TOMBSTONE      # a delete is just a write of a tombstone
    def flush(self): self.sstables.insert(0, dict(self.memtable)); self.memtable = {}
    def get(self, k):
        for layer in [self.memtable] + self.sstables:      # newest -> oldest
            if k in layer:
                return None if layer[k] == TOMBSTONE else layer[k]
        return None

db = LSM()
db.put("k", "v"); db.flush()       # "v" is in an SSTable on disk
db.delete("k")                      # delete -> writes a tombstone to the memtable
print("read after delete:", db.get("k"))
print("old value still on disk:", db.sstables[0]["k"])
```

<details>
<summary><strong>Reveal</strong></summary>

The read returns `None` — "not found" — even though `old value still on disk: v` shows the original value is *still physically there* in the SSTable. The delete didn't erase anything; it wrote a tombstone into the memtable, which the read encounters *first* (newest layer) and interprets as "this key is gone," stopping before it ever reaches the older SSTable. The old `v` is dead weight until a compaction merges the tombstone with the original key and drops both. This is why **delete-heavy LSM workloads bloat**: every delete *adds* data (a tombstone) rather than removing it, and the space isn't reclaimed until compaction runs. It's also why Cassandra's TTL'd rows can linger past their expiry — expiration, like deletion, is resolved lazily at compaction time, not the instant the clock ticks over. The shadowing rule that makes writes cheap is the same rule that makes deletes a deferred cost.

</details>

## Your Turn

Tombstones and shadowed values pile up; **compaction** is the garbage collector that reclaims them. Merge two SSTables and watch it resolve everything in one streaming pass.

**Predict:** a newer SSTable deletes `k1` (tombstone) and updates `k2` to `v2new`; an older SSTable holds `k1=v1`, `k2=v2old`, `k3=v3`. After compacting them into one file, which keys survive, and with what values?

```python run viz=array
TOMBSTONE = "<deleted>"
def compact(sstables):                 # merge newest-first SSTables into one
    merged = {}
    for sst in reversed(sstables):     # apply OLDEST first so newer writes overwrite
        merged.update(sst)
    return {k: v for k, v in merged.items() if v != TOMBSTONE}   # drop tombstoned keys

sst2 = {"k1": TOMBSTONE, "k2": "v2new"}             # newer SSTable: deletes k1, updates k2
sst1 = {"k1": "v1", "k2": "v2old", "k3": "v3"}      # older SSTable
before = [sst2, sst1]                                # newest first
after = compact(before)
print("before: 2 SSTables, keys per:", [sorted(s) for s in before])
print("after compaction:", dict(sorted(after.items())))
```

```java run viz=array
import java.util.*;
public class Main {
    static final String TOMBSTONE = "<deleted>";
    static Map<String,String> compact(List<Map<String,String>> sstables) {
        Map<String,String> merged = new TreeMap<>();
        for (int i = sstables.size() - 1; i >= 0; i--) merged.putAll(sstables.get(i));   // oldest first -> newer overwrites
        Map<String,String> out = new TreeMap<>();
        for (var e : merged.entrySet()) if (!e.getValue().equals(TOMBSTONE)) out.put(e.getKey(), e.getValue());
        return out;
    }
    public static void main(String[] x) {
        Map<String,String> sst2 = new TreeMap<>(Map.of("k1", TOMBSTONE, "k2", "v2new"));        // newer
        Map<String,String> sst1 = new TreeMap<>(Map.of("k1", "v1", "k2", "v2old", "k3", "v3")); // older
        List<Map<String,String>> before = List.of(sst2, sst1);  // newest first
        System.out.println("before: 2 SSTables, keys per: [" + new TreeSet<>(sst2.keySet()) + ", " + new TreeSet<>(sst1.keySet()) + "]");
        System.out.println("after compaction: " + compact(before));
    }
}
```

Both compact the two files into one holding `k2=v2new` and `k3=v3` (Python prints `{'k2': 'v2new', 'k3': 'v3'}`, Java `{k2=v2new, k3=v3}`). Three things happened in a single sorted pass: `k1`'s tombstone met its old value and **both were dropped** (space reclaimed); `k2` kept only the **newer** `v2new`, discarding `v2old`; and `k3`, present in just the old file, carried through unchanged. Two SSTables became one, so the next read for any of these keys checks half as many files. That's compaction's double payoff — it reclaims the space shadowing leaves behind *and* shrinks read amplification — and it's why an LSM must keep compaction running fast enough to match the write rate.

## Reflect & Connect

- **Sequential beats random — that's the whole idea.** Buffering writes and flushing them as immutable sorted files means the disk only ever sees sequential writes, the fast kind. The same "make random writes sequential" instinct as the [WAL](/cortex/data-structures-and-algorithms/dsa-in-real-systems-postgres-b-tree-and-the-write-path), taken all the way to the primary storage layout.
- **Shadowing is the unifying rule.** Updates and deletes both just *append* a newer version (a value or a tombstone) that shadows the old one; reads take the newest. It makes writes cheap and uniform, and makes deletes a deferred cost paid at compaction.
- **Compaction is the price and the cleanup.** It reclaims shadowed values and tombstones and bounds read amplification — but it costs CPU, bandwidth, and write amplification (~70× for leveled). If it can't keep up, the system stalls writes.
- **The RUM triangle has no free corner.** Read, Update, and Memory amplification trade off. LSM minimises update amplification; the [B-tree](/cortex/data-structures-and-algorithms/dsa-in-real-systems-postgres-b-tree-and-the-write-path) minimises read amplification. "Which database?" usually reduces to "which corner does my workload care about?"
- **It's everywhere write-heavy.** RocksDB (Kafka, CockroachDB, Flink, MongoDB's WiredTiger), Cassandra, ScyllaDB, HBase, LevelDB, InfluxDB — all LSM. Recognising the memtable→SSTable→compaction shape lets you reason about all of them at once.

## Recall

<details>
<summary><strong>Q:</strong> What are the three layers of an LSM write path, and why?</summary>

**A:** WAL (sequential durability log), memtable (in-memory sorted buffer, usually a skip list), and SSTables (immutable sorted files flushed from the memtable). The point is that every disk write is sequential — append to the WAL, flush the memtable in one pass — never a random in-place page write.

</details>
<details>
<summary><strong>Q:</strong> How does a read work when a key might be in several SSTables?</summary>

**A:** Check the memtable, then SSTables newest to oldest, returning the first hit (newest wins). Each SSTable has a Bloom filter, so the read skips any file that "definitely" lacks the key without a disk seek — keeping read amplification bounded.

</details>
<details>
<summary><strong>Q:</strong> How does an LSM tree delete a key, and what's the consequence?</summary>

**A:** It writes a tombstone (a "deleted" marker) into the memtable; that shadows older copies so reads return "not found." Nothing is physically removed until a compaction merges the tombstone with the original key and drops both — so delete-heavy workloads bloat until compaction catches up.

</details>
<details>
<summary><strong>Q:</strong> What does compaction do, and why is it mandatory?</summary>

**A:** It streams several sorted SSTables into one, keeping the newest value per key and dropping tombstones (and their shadowed values). Without it, SSTables accumulate without bound and every read checks ever-more files; compaction reclaims space and keeps read amplification bounded.

</details>
<details>
<summary><strong>Q:</strong> Leveled vs tiered compaction, and the RUM conjecture?</summary>

**A:** Leveled (RocksDB) keeps non-overlapping levels — low read amplification, high write amplification (~70×). Tiered (Cassandra) merges K files per tier — lower write amplification, higher read amplification. The RUM conjecture says Read, Update, and Memory amplification trade off; you can't minimise all three.

</details>

## Sources & Verify

- **O'Neil et al.** (1996), "The Log-Structured Merge-Tree (LSM-Tree)" — the original paper; **Chang et al.** (2006), "Bigtable" — the SSTable/memtable design that spawned this whole family.
- **Athanassoulis et al.** (2016), "Designing Access Methods: The RUM Conjecture" — the Read/Update/Memory trade-off framework.
- **RocksDB source & wiki**: `db/memtable.cc` (skip-list memtable), `db/db_impl/db_impl_write.cc` (WAL + memtable write path), `db/compaction/compaction_picker.cc` (leveled compaction), `util/bloom_impl.h` (per-SSTable Bloom filter); the [RocksDB wiki](https://github.com/facebook/rocksdb/wiki) is the best tour. Cassandra's compaction strategies live in `src/java/org/apache/cassandra/db/compaction/`.
- The newest-shadows-oldest read (`alice2` while `alice` stays on disk), the tombstone delete (`None` returned while `v` persists), and the compaction (`k1` dropped, `k2=v2new`, `k3` kept) all come from the runnable blocks above (deterministic models of the LSM write/read/compaction path) — re-run to verify.
