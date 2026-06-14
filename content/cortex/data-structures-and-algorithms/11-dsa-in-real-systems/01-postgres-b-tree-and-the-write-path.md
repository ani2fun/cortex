---
title: "Postgres B-Tree and the Write Path"
summary: "The textbook B-tree taught the shape; Postgres taught it to survive a crash. CREATE INDEX builds a B+-tree on disk, and every change goes to the Write-Ahead Log before it touches a page — so a crash mid-update replays cleanly. Plus the B-link trick that lets concurrent INSERTs split without locking the whole tree."
prereqs:
  - trees-b-tree-introduction-to-b-trees
  - 03-trees/05-self-balancing-bst-overview/01-self-balancing-bst-overview
---

## Why It Exists

You taught yourself the [B-tree](/cortex/data-structures-and-algorithms/trees/b-tree/introduction-to-b-trees) in the abstract: high fanout, shallow height, `O(log n)` search. Postgres builds exactly that for every `CREATE INDEX ON users(email)` — but on a real disk, two problems the textbook ignored become load-bearing:

- **A crash can interrupt a write.** Updating a B-tree page is a *random* write to an 8 KB block. If the machine loses power halfway through, the page is left torn — half old, half new — and the index is corrupt. The structure being correct in memory means nothing if it can't survive `kill -9`.
- **Many writers hit the tree at once.** A naive "lock the whole root-to-leaf path while I split" strategy would serialize every INSERT. A busy table needs thousands of concurrent inserts a second.

Postgres's answer is two production ideas layered onto the B-tree. The **Write-Ahead Log (WAL)**: every change is appended to a sequential, durable log *before* it's applied to a page — so recovery just replays the log. And the **B-link tree** (Lehman-Yao, 1981): each page carries a right-sibling pointer, so a page can split *locally* and a concurrent reader who lands on the stale page follows the link to find the moved keys — no whole-tree lock. This lesson is where the clean data structure meets the messiness that makes it usable.

## See It Work

The write path in miniature: an insert appends to the WAL first, *then* applies to the in-memory page. Crash the process — the page buffer evaporates — and replaying the WAL reconstructs it exactly.

```python run viz=array
import ast

class Database:
    def __init__(self):
        self.wal = []            # durable, append-only log (survives a crash)
        self.page = {}           # in-memory B-tree leaf buffer (lost on crash)
    def insert(self, key, tid):
        self.wal.append((key, tid))   # 1. WRITE-AHEAD: append to the WAL first (sequential, fsync'd)
        self.page[key] = tid          # 2. then apply to the page (a random in-place write)
    def recover(self):
        self.page = {}                # the page buffer was lost...
        for key, tid in self.wal:     # ...replay the WAL to rebuild it
            self.page[key] = tid

keys = ast.literal_eval(input())   # list of integer keys to insert

db = Database()
for i, key in enumerate(keys):
    db.insert(key, f"row-{i}")
print("before crash:", sorted(db.page))
db.page = {}                          # CRASH: in-memory buffer gone; the WAL is safe on disk
print("after crash:", sorted(db.page))
db.recover()                          # replay the WAL from the last checkpoint
print("after WAL replay:", sorted(db.page))
```

```java run viz=array
import java.util.*;
public class Main {
    static class Record { int key; String tid; Record(int k, String t) { key = k; tid = t; } }
    static class Database {
        List<Record> wal = new ArrayList<>();              // append-only log; survives a crash
        TreeMap<Integer, String> page = new TreeMap<>();   // in-memory leaf buffer; lost on crash
        void insert(int key, String tid) {
            wal.add(new Record(key, tid));                 // 1. WRITE-AHEAD: log first
            page.put(key, tid);                            // 2. then apply to the page
        }
        void recover() {
            page = new TreeMap<>();                        // buffer lost...
            for (Record r : wal) page.put(r.key, r.tid);   // ...replay the log to rebuild it
        }
    }
    public static void main(String[] x) {
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().trim();
        line = line.substring(1, line.length() - 1);   // strip [ ]
        String[] parts = line.split(",\\s*");
        Database db = new Database();
        for (int i = 0; i < parts.length; i++) {
            int key = Integer.parseInt(parts[i].trim());
            db.insert(key, "row-" + i);
        }
        System.out.println("before crash: " + db.page.keySet());
        db.page = new TreeMap<>();                          // CRASH: buffer gone, WAL safe on disk
        System.out.println("after crash: " + db.page.keySet());
        db.recover();                                       // replay the WAL
        System.out.println("after WAL replay: " + db.page.keySet());
    }
}
```

```testcases
{
  "args": [
    { "id": "keys", "label": "keys", "type": "string", "placeholder": "[42, 17, 99]" }
  ],
  "cases": [
    { "args": { "keys": "[42, 17, 99]" }, "expected": "before crash: [17, 42, 99]\nafter crash: []\nafter WAL replay: [17, 42, 99]" },
    { "args": { "keys": "[5, 3, 8, 1]" }, "expected": "before crash: [1, 3, 5, 8]\nafter crash: []\nafter WAL replay: [1, 3, 5, 8]" },
    { "args": { "keys": "[100]" }, "expected": "before crash: [100]\nafter crash: []\nafter WAL replay: [100]" }
  ]
}
```

Both print `before crash: [17, 42, 99]`, then `after crash: []`, then `after WAL replay: [17, 42, 99]`. The crash wiped the in-memory page, but every key had already been written to the WAL *before* it touched the page — so replaying the log restores the index byte-for-byte. That ordering, log-before-page, is the whole guarantee.

## How It Works

Three mechanics turn the textbook B-tree into Postgres's `nbtree`:

```d2
direction: right
ins: "INSERT (key, TID)" {style.fill: "#fef9c3"}
wal: "1. WAL — append the change\n(sequential write, fsync'd to disk)\nDURABLE, survives a crash" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
page: "2. B-tree page — apply the change\n(random 8 KB write, in the buffer pool)\nFAST, but lost on crash" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
crash: "CRASH\nbuffer pool gone" {style.fill: "#fecaca"; style.stroke: "#dc2626"}
recover: "RECOVERY: replay the WAL\nfrom the last checkpoint\n-> pages reconstructed" {style.fill: "#e9d5ff"; style.stroke: "#9333ea"}
ins -> wal: first
wal -> page: then
page -> crash
crash -> recover: WAL still on disk
```

<p align="center"><strong>The write path: the change is made <em>durable</em> in the sequential WAL before the <em>random</em> page write that a crash could tear. Recovery replays the log — so the index always survives.</strong></p>

- **Write-ahead logging — log before page.** A random 8 KB page write isn't atomic; a crash can tear it. So Postgres first appends a small WAL record (`XLOG_BTREE_INSERT_LEAF`, `XLOG_BTREE_SPLIT_L/R`, `XLOG_BTREE_NEWROOT`, …) to the *sequential* log and fsyncs that, then applies the page change lazily in the buffer pool. The invariant: **the log record reaches durable storage before the page does.** On restart, Postgres replays the WAL from the last checkpoint and every `_bt_redo_*` handler re-applies its record, rebuilding the index identically. Sequential log writes are also far cheaper than an fsync per random page — durability without the seek tax.
- **B-link concurrency — split locally, fix up later.** Each page stores a right-sibling pointer (`btpo_next`). When a full leaf splits, the upper half moves to a *new* right sibling and the original's right-link is repointed to it — a local operation that doesn't lock the parent. A concurrent reader who descended through a now-stale parent can land on the original page, notice the key it wants is past the page's high key, and follow the right-link to the new sibling. Inserting the new page into the parent happens *afterward*, amortised across inserts. That's why INSERT throughput holds up under high concurrency ([Your Turn](#your-turn)).
- **Fanout buys shallowness.** With 8 KB pages (matching the OS page size) and ~50-byte tuples, a leaf holds ~150 keys and an internal node ~250 routing keys. A B+-tree over a billion rows is only **4–5 levels** deep — so a worst-case index lookup is 4–5 page reads. (MVCC adds a tax: `DELETE` only marks tuples dead; VACUUM reclaims them later, and an un-vacuumed index bloats monotonically.)

> **Key takeaway.** Postgres's `nbtree` is a B+-tree plus two production ideas. **Write-ahead logging** makes mutations crash-safe: append the change to the sequential, durable WAL *before* the random page write a crash could tear, and recovery replays the log to rebuild the index. **B-link trees** make them concurrent: a right-sibling pointer per page lets a leaf split locally — readers who hit the stale page follow the link to the moved keys — so concurrent INSERTs never lock the whole path. The clean structure from the B-tree chapter; the durability and concurrency that let it run a database.

## Trace It

The order "log, *then* page" looks like bookkeeping until you ask what a crash costs without it.

**Predict before you run:** two engines take the same three inserts, then crash and lose the in-memory page buffer. Engine A writes straight to the page (no log). Engine B logs each change first, then applies it. After recovery, what does each engine have — all three keys, or none?

```python run viz=array
class NoWAL:                          # applies straight to the buffer, no log
    def __init__(self): self.page = {}
    def insert(self, k, v): self.page[k] = v
    def crash(self): self.page = {}   # buffer lost -> nothing on disk to recover from

class WAL:                            # log first, then apply
    def __init__(self): self.log = []; self.page = {}
    def insert(self, k, v): self.log.append((k, v)); self.page[k] = v
    def crash(self): self.page = {}
    def recover(self):
        for k, v in self.log: self.page[k] = v   # the log survived the crash

a = NoWAL()
for k in [10, 20, 30]: a.insert(k, "v")
a.crash()
print("no-WAL after crash:", sorted(a.page))

b = WAL()
for k in [10, 20, 30]: b.insert(k, "v")
b.crash(); b.recover()
print("WAL after crash + replay:", sorted(b.page))
```

<details>
<summary><strong>Reveal</strong></summary>

The no-WAL engine prints `[]` — **everything is gone**. Its only record of the inserts lived in the in-memory page buffer, and the crash took it; there's nothing on disk to recover from. The WAL engine prints `[10, 20, 30]` — **fully recovered** — because each change was appended to the durable log *before* it touched the volatile page, so replay reconstructs the state. This is the entire justification for "write-ahead": the page in the buffer pool is fast but volatile, and the disk write that would persist it isn't atomic. By forcing the small, sequential log record to disk first, Postgres guarantees that any change acknowledged to the client can be replayed after a crash — even if the page write never happened or was torn in half. Durability isn't a property of the B-tree; it's a property of the *order* in which you write.

</details>

## Your Turn

**The B-link rescue.** A leaf holding `[10, 20, 30, 40]` overflowed and split: `10, 20` stay on the original page, `30, 40` move to a new right sibling, and the original's right-link points at it. A concurrent reader descended through a now-stale parent and landed on the *original* page, searching for a key — which may have just moved to the right sibling.

**Implement `search(left, key)`** — traverse the B-link chain, following `right` pointers when the search key is past the current page's high key, and return whether the key exists.

```python run viz=array
import ast

class Leaf:
    def __init__(self, keys): self.keys = keys; self.right = None

# Fixed B-link structure: leaf [10,20,30,40] split into left=[10,20], right=[30,40]
left = Leaf([10, 20]); right = Leaf([30, 40])
left.right = right

def search(leaf, key):
    # Your code goes here
    return False

key = ast.literal_eval(input())
result = search(left, key)
print("true" if result else "false")
```

```java run viz=array
import java.util.*;
public class Main {
    static class Leaf {
        List<Integer> keys; Leaf right = null;
        Leaf(Integer... ks) { keys = Arrays.asList(ks); }
    }
    static boolean search(Leaf leaf, int key) {
        // Your code goes here
        return false;
    }
    public static void main(String[] x) {
        Leaf left = new Leaf(10, 20), right = new Leaf(30, 40);
        left.right = right;
        Scanner sc = new Scanner(System.in);
        int key = Integer.parseInt(sc.nextLine().trim());
        System.out.println(search(left, key));
    }
}
```

```testcases
{
  "args": [
    { "id": "key", "label": "key", "type": "string", "placeholder": "30" }
  ],
  "cases": [
    { "args": { "key": "30" }, "expected": "true" },
    { "args": { "key": "99" }, "expected": "false" },
    { "args": { "key": "10" }, "expected": "true" },
    { "args": { "key": "20" }, "expected": "true" },
    { "args": { "key": "15" }, "expected": "false" }
  ]
}
```

<details>
<summary><strong>Editorial</strong></summary>

Walk the B-link chain: at each page, if the key is present return true; if the key is past the page's high key and a right sibling exists, follow the link; otherwise return false. The right-link is the safety net for any reader caught mid-split — it turns a local split into a globally-correct traversal.

```python solution time=O(n) space=O(1)
import ast

class Leaf:
    def __init__(self, keys): self.keys = keys; self.right = None

# Fixed B-link structure: leaf [10,20,30,40] split into left=[10,20], right=[30,40]
left = Leaf([10, 20]); right = Leaf([30, 40])
left.right = right

def search(leaf, key):
    while leaf is not None:
        if key in leaf.keys:
            return True
        if leaf.keys and key > leaf.keys[-1] and leaf.right is not None:
            leaf = leaf.right                    # key may have moved right in a split: follow the link
        else:
            return False
    return False

key = ast.literal_eval(input())
result = search(left, key)
print("true" if result else "false")
```

```java solution
import java.util.*;
public class Main {
    static class Leaf {
        List<Integer> keys; Leaf right = null;
        Leaf(Integer... ks) { keys = Arrays.asList(ks); }
    }
    static boolean search(Leaf leaf, int key) {
        while (leaf != null) {
            if (leaf.keys.contains(key)) return true;
            if (!leaf.keys.isEmpty() && key > leaf.keys.get(leaf.keys.size() - 1) && leaf.right != null)
                leaf = leaf.right;               // follow the B-link: key may have moved right in a split
            else
                return false;
        }
        return false;
    }
    public static void main(String[] x) {
        Leaf left = new Leaf(10, 20), right = new Leaf(30, 40);
        left.right = right;
        Scanner sc = new Scanner(System.in);
        int key = Integer.parseInt(sc.nextLine().trim());
        System.out.println(search(left, key));
    }
}
```

</details>

## Reflect & Connect

- **Durability is an ordering property.** The WAL works because the log record is forced to disk *before* the page. Log-then-page survives any crash; page-only loses whatever was still in the buffer. Same idea powers redo logs in MySQL/InnoDB and journals in ext4/NTFS.
- **Sequential beats random.** The WAL is an append-only sequential write (cheap to fsync); the page write is random (expensive to fsync per page). Logging first lets Postgres batch and defer the random writes — durability *and* throughput.
- **B-link = concurrency without whole-path locks.** A right-sibling pointer turns a split into a local edit; readers who hit a stale page follow the link. This is why a textbook B-tree and `nbtree` differ — and why Postgres INSERTs scale.
- **Fanout still rules the height.** 8 KB pages, ~hundreds of keys per node, height 4–5 for a billion rows — the [B-tree](/cortex/data-structures-and-algorithms/trees/b-tree/introduction-to-b-trees)'s core promise, unchanged by all the production machinery around it.
- **The write-optimised cousin.** When writes dominate even more, databases reach for [LSM trees](/cortex/data-structures-and-algorithms/dsa-in-real-systems/lsm-trees-rocksdb-cassandra) (RocksDB, Cassandra) — buffer in memory, flush sequentially, merge later. Same "make random writes sequential" instinct as the WAL, taken all the way.

## Recall

<details>
<summary><strong>Q:</strong> What does write-ahead logging guarantee, and why does the order matter?</summary>

**A:** Every page modification is appended to the durable WAL *before* the page itself is written to disk. Because the log hits disk first, a crash can always be recovered by replaying the WAL from the last checkpoint — even if the (non-atomic, tearable) page write never completed. Page-before-log would lose any change whose page write didn't finish.

</details>
<details>
<summary><strong>Q:</strong> What does the "B-link" add over a textbook B-tree, and what does it buy?</summary>

**A:** A right-sibling pointer on every page (`btpo_next`). It lets a leaf split *locally* without locking the root-to-leaf path: a concurrent reader who lands on a stale page follows the right-link to find keys the split moved. Result: concurrent INSERTs scale.

</details>
<details>
<summary><strong>Q:</strong> Why is the WAL a *sequential* write while the page update is *random* — and why does that matter?</summary>

**A:** The WAL is append-only (always writes at the end), so fsync is cheap; a page update lands at an arbitrary 8 KB block (random). Logging first lets Postgres make the durable write sequential and defer/batch the expensive random page writes — durability without an fsync per page.

</details>
<details>
<summary><strong>Q:</strong> Why doesn't a <code>DELETE</code> immediately shrink a Postgres index?</summary>

**A:** MVCC keeps deleted ("dead") tuples around until no transaction can still see them. The B-tree retains their entries until VACUUM removes them and reclaims pages. Without adequate vacuuming, the index bloats monotonically.

</details>
<details>
<summary><strong>Q:</strong> Roughly how tall is a Postgres B+-tree over a billion-row table, and why?</summary>

**A:** About 4–5 levels. With 8 KB pages, internal nodes hold ~200 routing keys, so `log200(10^9) approx 4`. A worst-case lookup is therefore only 4–5 page reads.

</details>

## Sources & Verify

- **Lehman & Yao** (1981), "Efficient Locking for Concurrent Operations on B-Trees", *ACM TODS* — the B-link tree and its right-link concurrency invariant.
- **Postgres source**: `src/backend/access/nbtree/` — `nbtinsert.c` (`_bt_doinsert`, `_bt_split`), `nbtsearch.c` (`_bt_search`), `nbtxlog.c` (WAL replay), and the `README` design notes. **Mohan et al.** (1992), "ARIES" — the write-ahead logging and recovery model Postgres follows.
- **Hellerstein, Stonebraker & Hamilton**, "Architecture of a Database System" — buffer pool, WAL, and access methods in context; the [Postgres docs on WAL](https://www.postgresql.org/docs/current/wal-intro.html).
- The crash-and-replay (`[17, 42, 99] -> [] -> [17, 42, 99]`), the no-WAL-vs-WAL contrast (`[]` vs `[10, 20, 30]`), and the B-link rescue (`30` found, `99` not) all come from the runnable blocks above (deterministic models of the WAL write path and a B-link split) — re-run to verify.
