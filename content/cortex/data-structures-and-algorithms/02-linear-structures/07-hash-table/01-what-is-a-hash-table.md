---
title: Hash Tables
summary: Stop searching, start computing — a hash function turns any key into an array index, so lookup, insert, and delete are O(1) on average. The structure behind every dictionary, database index, and cache, paid for with the inevitability of collisions.
tier: spine
prereqs:
  - linear-structures-arrays-what-is-an-array
  - linear-structures-singly-linked-list-what-is-a-linked-list
  - foundations-measuring-cost
---

# Hash Tables

## Why It Exists

You want to look something up by a **key** — a username, a word, a phone-book name — and get its value back instantly. An array already does instant lookup, but only by *integer index*: `arr[3]` is `O(1)` because the index *is* the address. Your keys aren't integers, though. Searching a million `(name, number)` pairs for `"Neha"` means scanning until you find her — `O(n)`.

Here's the move that should feel like cheating: what if you could *compute* an array index straight from the key? Feed `"Neha"` to a function, get back `2`, and read `arr[2]` — no search at all. You'd have the array's `O(1)` speed, but keyed by *anything*.

That function is a **hash function**, and the array it indexes into is a **hash table**. It's how every dictionary, set, database index, and cache turns "find this key" from a walk into a single jump.

## See It Work

A hash function maps each key to a bucket index. Run this — four keys, four buckets — and click **Visualise** to watch two keys land in the *same* bucket and share it.

> ▶ Run it, then click **Visualise** — each key hashes to a bucket; when two collide, they chain together in that bucket.

```python run viz=hashmap viz-root=table viz-kind=hashmap
class Entry:
    def __init__(self, key, value):
        self.key, self.value = key, value

table = {}                          # bucket index -> chain of entries

def put(key, value):
    i = len(key) % 4                # toy hash: key length, mod 4 buckets
    table.setdefault(i, []).append(Entry(key, value))

for k, v in [("cat", 1), ("dog", 2), ("fish", 3), ("bird", 4)]:
    put(k, v)                       # "cat"/"dog" → bucket 3; "fish"/"bird" → bucket 0
print({i: [e.key for e in ch] for i, ch in sorted(table.items())})
```

## How It Works

A hash table is an **array of buckets** plus a **hash function** that turns a key into a bucket index:

```
index = hash(key) % capacity
```

`hash(key)` produces some integer; `% capacity` folds it into a valid slot. Insert, look up, and delete all do the same two steps — hash the key, go to that slot — so each is **`O(1)` on average**. There's no scan; you *compute* where the key lives.

But two different keys can hash to the same index — a **collision** — and they're not a rare accident: with more keys than buckets they're guaranteed (the pigeonhole principle). So every hash table needs a **collision-resolution** strategy, and there are two families:

- **Separate chaining** — each bucket holds a little *linked list*; colliding keys just join the list at that bucket.
- **Open addressing** — on a collision, *probe* forward to the next free slot and store the key there.

```mermaid
flowchart LR
  K1["key: cat"] --> H["hash(key) % capacity"]
  K2["key: dog"] --> H
  H --> B["bucket 3"]
  B --> CH["chain: cat · dog"]
```

<p align="center"><strong>the hash function maps each key to a bucket; when two keys land in the same bucket (a collision), separate chaining stores both in a small list.</strong></p>

What keeps the average `O(1)` is the **load factor** — `entries / capacity`. Let it climb and buckets get crowded, so lookups drift toward `O(n)`. The fix is to **rehash**: once the load factor crosses a threshold (Java's `HashMap` uses `0.75`), allocate a bigger array and re-place every entry. That occasional `O(n)` rebuild is amortized away, exactly like the dynamic array's doubling.

### Key Takeaway

A hash table computes a key's slot with `hash(key) % capacity` for `O(1)`-average access; collisions are inevitable, so it resolves them (chaining or open addressing) and rehashes to keep the load factor — and the speed — in check.

## Trace It

Insert `"cat"`, `"dog"`, `"fish"` into a 4-bucket table, hashing by length (`len % 4`):

| Key | `len` | `% 4` → bucket | Result |
|---|---|---|---|
| `cat` | 3 | 3 | bucket 3: `[cat]` |
| `dog` | 3 | 3 | bucket 3: `[cat, dog]` ← collision, chained |
| `fish` | 4 | 0 | bucket 0: `[fish]` |

Before you read on: to now *find* `"dog"`, how much of the table does the lookup touch?

Just bucket 3 — hash `"dog"` to `3`, then walk that one short chain. It never looks at buckets 0, 1, or 2. That's the whole game: the hash jumps you to the right bucket, and only a collision adds a tiny bit of walking. (And it's why a *bad* hash that dumps every key into one bucket secretly turns your `O(1)` table back into an `O(n)` linked list.)

## Your Turn

You'll almost never hand-roll one — every language ships a hash table as its dictionary. Here it is doing `O(1)` work:

```python run viz=array
phone = {}                      # Python's dict IS a hash table
phone["Neha"] = 23
phone["Hari"] = 7
print(phone["Neha"])            # O(1) average lookup → 23
print("Karan" in phone)         # O(1) membership test → False
```

```java run viz=array
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    Map<String, Integer> phone = new HashMap<>();   // Java's hash table
    phone.put("Neha", 23);
    phone.put("Hari", 7);
    System.out.println(phone.get("Neha"));            // O(1) avg → 23
    System.out.println(phone.containsKey("Karan"));   // O(1) → false
  }
}
```

Want the two collision strategies in full? [Separate Chaining](/cortex/data-structures-and-algorithms/linear-structures-hash-table-separate-chaining) builds the linked-list-per-bucket version from scratch; the probing lessons cover open addressing.

## Reflect & Connect

The hash table is the workhorse of practical computing — any time you map keys to values or test membership, it's almost certainly underneath:

- **Every language's dictionary / map / set** — Python `dict`, Java `HashMap`, Go `map`, JS `Object`/`Map` — is a hash table.
- **Database indexes and caches** key on it; **compilers** keep a *symbol table* of names; **deduplication** drops a set of seen items into one.

Two cautions to carry forward. First, `O(1)` is an *average* — a poor hash function (or an adversary feeding worst-case keys) collapses it to `O(n)`, which is why production tables use well-mixed hashes and randomized seeds. Second, the order is gone: a hash table scatters keys by hash, so it gives up the *sorted* or *insertion* order that an array or a balanced tree keeps. When you need ordering, that's the tradeoff that sends you elsewhere.

**Prerequisites:** [Arrays](/cortex/data-structures-and-algorithms/linear-structures-arrays-what-is-an-array), [Linked Lists](/cortex/data-structures-and-algorithms/linear-structures-singly-linked-list-what-is-a-linked-list), and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations-measuring-cost).
**What's next:** a structure that keeps the *largest* item always within reach — the [Heap](/cortex/data-structures-and-algorithms/trees-heap-what-is-a-heap).

## Recall

> **Mnemonic:** *Compute the slot, don't search it: `index = hash(key) % capacity`. Collisions are inevitable — chain or probe — and rehash to stay fast.*

| Operation | Cost | Why |
|---|---|---|
| insert / lookup / delete | `O(1)` average | hash the key, jump to the bucket — no scan |
| same, worst case | `O(n)` | a bad hash piles every key into one bucket |
| space | `O(n)` | buckets plus the entries they hold |

<details>
<summary><strong>Q:</strong> How does a hash table turn a key into a position?</summary>

**A:** `index = hash(key) % capacity` — it *computes* the slot instead of searching.

</details>
<details>
<summary><strong>Q:</strong> What is a collision, and why is it unavoidable?</summary>

**A:** Two keys hashing to the same bucket; with more keys than buckets the pigeonhole principle guarantees it.

</details>
<details>
<summary><strong>Q:</strong> Name the two collision-resolution families.</summary>

**A:** Separate chaining (a list per bucket) and open addressing (probe to the next free slot).

</details>
<details>
<summary><strong>Q:</strong> What is the load factor, and why rehash?</summary>

**A:** `entries / capacity`; when it climbs, buckets crowd and lookups slow, so you grow the array and re-place entries to restore `O(1)`.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 4th ed., **Ch. 11 — Hash Tables**: hashing, chaining, open addressing, and the average-case `O(1)` analysis under simple uniform hashing.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §3.4 — hash tables, separate chaining vs linear probing, and load-factor resizing.
- **CPython** `Objects/dictobject.c` (open addressing + perturbation probe) and **OpenJDK** `HashMap.java` (chaining, `0.75` load factor, treeify at 8) — the real production designs; verify the load-factor and collision claims against source.
- Both runnable blocks are verified by running; the `O(1)`-average / `O(n)`-worst-case split follows from the collision argument.
