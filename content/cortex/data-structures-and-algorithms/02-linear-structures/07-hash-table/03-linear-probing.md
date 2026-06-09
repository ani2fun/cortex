---
title: "Linear Probing"
summary: "Open addressing on one array: on a collision, walk forward one slot at a time to the next free cell. Deletions leave a DELETED tombstone so probe chains don't break. Cache-friendly and pointer-free, but suffers primary clustering as the table fills."
prereqs:
  - 02-linear-structures/07-hash-table/01-what-is-a-hash-table
---

# Linear Probing

## Why It Exists

Separate chaining resolved collisions in side-lists — but every entry then carries list/pointer overhead, and walking a chain hops around memory, defeating the cache. **Open addressing** takes the opposite approach: store every entry *in the array itself*, no side structures. When a key's slot is taken, you **probe** for another open slot.

**Linear probing** is the simplest probe sequence: if slot `h` is occupied, try `h+1`, then `h+2`, … wrapping around, until you find an empty slot. Everything lives in one contiguous array, so lookups are cache-friendly and there are no pointers. The price is twofold: the table can actually *fill up* (load factor must stay below 1), and occupied slots tend to clump into long runs — *primary clustering* — that lengthen future probes.

## See It Work

A capacity-8 table where `1`, `9`, `17` all hash to slot `1` (`% 8`), so they probe to slots 1, 2, 3. Then delete `9` and confirm `17` is still findable — that's what the tombstone protects. Run it.

```python run viz=array
EMPTY = None
DELETED = object()                  # tombstone sentinel

class LinearProbing:
    def __init__(self, capacity=8):
        self.capacity = capacity
        self.slots = [EMPTY] * capacity
    def put(self, key, value):
        start, first_deleted = key % self.capacity, -1
        for step in range(self.capacity):
            i = (start + step) % self.capacity      # walk forward, wrapping
            slot = self.slots[i]
            if slot is EMPTY:
                self.slots[first_deleted if first_deleted != -1 else i] = (key, value)
                return
            if slot is DELETED:
                if first_deleted == -1: first_deleted = i
            elif slot[0] == key:
                self.slots[i] = (key, value); return     # update in place
    def get(self, key):
        start = key % self.capacity
        for step in range(self.capacity):
            slot = self.slots[(start + step) % self.capacity]
            if slot is EMPTY:           return None        # empty ⇒ key absent
            if slot is not DELETED and slot[0] == key: return slot[1]
        return None
    def delete(self, key):
        start = key % self.capacity
        for step in range(self.capacity):
            i = (start + step) % self.capacity
            slot = self.slots[i]
            if slot is EMPTY: return False
            if slot is not DELETED and slot[0] == key:
                self.slots[i] = DELETED; return True       # tombstone, NOT empty
        return False

t = LinearProbing(8)
t.put(1, "a"); t.put(9, "b"); t.put(17, "c")   # collide at slot 1 → probe to 1,2,3
t.delete(9)                                     # leaves a tombstone at slot 2
print(t.get(17))                                # c — probe walks PAST the tombstone
print(t.get(9))                                 # None
```

## How It Works

Each slot is `EMPTY`, a `(key, value)` entry, or a `DELETED` **tombstone**. From `h = hash(key) % capacity`, probe forward (`h, h+1, h+2, …` mod capacity):

- **put** — stop at the first `EMPTY` slot (insert) or a slot holding the key (update). Reuse the first tombstone seen along the way.
- **get** — walk until you find the key (return it) or hit an `EMPTY` slot (the key is absent — stop). Skip over tombstones.
- **delete** — find the key and mark its slot `DELETED`, **not** `EMPTY`.

```mermaid
flowchart LR
  H["h = hash(key) % cap"] --> S1{"slot h empty?"}
  S1 -->|"no — occupied"| S2{"slot h+1 empty?"}
  S2 -->|"no"| S3["… h+2, h+3 … (wrap)"]
  S1 -->|"yes"| P["place / found empty"]
  S2 -->|"yes"| P
```

<p align="center"><strong>on collision, walk forward one slot at a time to the first empty cell; a deleted slot becomes a tombstone so later probes don't stop early.</strong></p>

Why must delete write a tombstone? A `get` stops at the first `EMPTY` slot. If deleting `9` (slot 2) wrote `EMPTY` there, then looking up `17` (sitting at slot 3, after probing past slots 1 and 2) would hit the new `EMPTY` at slot 2 and **wrongly conclude `17` is absent**. The `DELETED` tombstone keeps the probe chain continuous — `get` skips it and continues — while still freeing the slot for reuse by `put`. The cost: tombstones accumulate and lengthen probes, so heavily-churned tables must occasionally rehash. Load factor must stay `< 1` (and well below it — performance degrades sharply past ~0.7).

### Key Takeaway

Linear probing stores entries in the array and resolves collisions by walking forward to the next free slot — cache-friendly, pointer-free. Deletions must leave a `DELETED` tombstone (not `EMPTY`) or probe chains break. Keep the load factor well under 1; primary clustering is the cost.

## Trace It

Inserting `1, 9, 17` into a capacity-8 table (all `≡ 1 mod 8`):

| put | start | probe walk | lands at |
|---|---|---|---|
| `1` | 1 | slot 1 empty | slot 1 |
| `9` | 1 | slot 1 taken → slot 2 empty | slot 2 |
| `17` | 1 | slots 1, 2 taken → slot 3 empty | slot 3 |

Before you read on: three keys with the *same* hash formed a run of occupied slots 1–3. Now a *fourth* key that hashes to slot `2` arrives. How many slots must it probe — and what is this snowballing effect called?

It hashes to slot `2`, which is taken, so it probes `2 → 3` (both taken) → slot `4` (empty) — three probes, even though only *one* other key (`9`) actually shares its home slot. This is **primary clustering**: once a run of occupied slots forms, *any* key hashing *anywhere inside that run* extends it, and longer runs make future collisions and longer probes more likely — the cluster feeds itself. It's the central weakness of linear probing, and exactly what quadratic probing is designed to break up.

## Your Turn

The reusable open-addressing table (with tombstone delete):

```python run viz=array
EMPTY = None
DELETED = object()

class LinearProbing:
    def __init__(self, capacity=8):
        self.capacity = capacity
        self.slots = [EMPTY] * capacity
    def put(self, key, value):
        start, first_deleted = key % self.capacity, -1
        for step in range(self.capacity):
            i = (start + step) % self.capacity
            slot = self.slots[i]
            if slot is EMPTY:
                self.slots[first_deleted if first_deleted != -1 else i] = (key, value); return
            if slot is DELETED:
                if first_deleted == -1: first_deleted = i
            elif slot[0] == key:
                self.slots[i] = (key, value); return
    def get(self, key):
        start = key % self.capacity
        for step in range(self.capacity):
            slot = self.slots[(start + step) % self.capacity]
            if slot is EMPTY: return None
            if slot is not DELETED and slot[0] == key: return slot[1]
        return None

t = LinearProbing()
t.put(2, "x"); t.put(10, "y")          # collide at slot 2 → 2, 3
print(t.get(2), t.get(10), t.get(99))  # x y None
```

```java run viz=array
public class Main {
  static final int[] DELETED = new int[0];
  static class LinearProbing {
    int capacity; int[][] slots;
    LinearProbing(int cap) { capacity = cap; slots = new int[cap][]; }   // null = EMPTY
    void put(int key, int value) {
      int start = Math.floorMod(key, capacity), firstDel = -1;
      for (int step = 0; step < capacity; step++) {
        int i = (start + step) % capacity;
        int[] s = slots[i];
        if (s == null) { slots[firstDel != -1 ? firstDel : i] = new int[]{key, value}; return; }
        if (s == DELETED) { if (firstDel == -1) firstDel = i; }
        else if (s[0] == key) { slots[i] = new int[]{key, value}; return; }
      }
    }
    Integer get(int key) {
      int start = Math.floorMod(key, capacity);
      for (int step = 0; step < capacity; step++) {
        int[] s = slots[(start + step) % capacity];
        if (s == null) return null;
        if (s != DELETED && s[0] == key) return s[1];
      }
      return null;
    }
  }
  public static void main(String[] args) {
    LinearProbing t = new LinearProbing(8);
    t.put(2, 20); t.put(10, 100);          // collide at slot 2 → 2, 3
    System.out.println(t.get(2) + " " + t.get(10) + " " + t.get(99));   // 20 100 null
  }
}
```

## Reflect & Connect

Linear probing is open addressing at its simplest, and it frames the whole family:

- **Open addressing vs chaining** — open addressing wins on **cache locality** (one contiguous array, no pointer-chasing) and memory (no list nodes), but it can't exceed load factor 1, needs **tombstones** for deletion, and suffers clustering. Chaining tolerates `α > 1` and deletes trivially. The trade is locality vs simplicity.
- **The tombstone is the subtle part** — deletion can't just empty a slot, or every key probed *past* the deleted one becomes unreachable. Tombstones preserve probe chains but accumulate, so churn-heavy tables rehash periodically.
- **Primary clustering motivates the sequels** — the `+1` walk merges runs. The [next lesson](/cortex/data-structures-and-algorithms/linear-structures/hash-table/quadratic-probing) replaces it with a quadratic jump to scatter colliding keys and break those clusters.

**Prerequisites:** [What Is a Hash Table?](/cortex/data-structures-and-algorithms/linear-structures/hash-table/what-is-a-hash-table) (and the contrast with [Separate Chaining](/cortex/data-structures-and-algorithms/linear-structures/hash-table/separate-chaining)).
**What's next:** swap the one-step walk for a quadratic jump — [Quadratic Probing](/cortex/data-structures-and-algorithms/linear-structures/hash-table/quadratic-probing).

## Recall

> **Mnemonic:** *Collision → walk `h, h+1, h+2 …` to the first empty slot. Delete = tombstone (not empty) or probes break. Load factor `< 1`; primary clustering is the cost.*

| | |
|---|---|
| Probe sequence | `h, h+1, h+2, …` (mod capacity) |
| Slot states | `EMPTY`, an entry, or `DELETED` (tombstone) |
| get stops at | the key, or an `EMPTY` slot (absent) — skips tombstones |
| delete | write a tombstone, never `EMPTY` |
| Cost / limit | `O(1)` average at low `α`; load factor must stay `< 1`; primary clustering |

<details>
<summary><strong>Q:</strong> How does linear probing resolve a collision?</summary>

**A:** It walks forward one slot at a time from the hashed index to the first empty slot.

</details>
<details>
<summary><strong>Q:</strong> Why must deletion leave a tombstone instead of emptying the slot?</summary>

**A:** A `get` stops at the first `EMPTY` slot, so emptying a slot mid-chain would make keys probed past it unreachable.

</details>
<details>
<summary><strong>Q:</strong> What is primary clustering?</summary>

**A:** Occupied slots form runs; any key hashing into a run extends it, so clusters grow and lengthen probes — a self-reinforcing slowdown.

</details>
<details>
<summary><strong>Q:</strong> Open addressing vs chaining — the core trade?</summary>

**A:** Open addressing gives cache locality and no pointers but caps `α < 1` and needs tombstones; chaining tolerates `α > 1` and deletes trivially.

</details>

## Sources & Verify

- **CLRS**, *Introduction to Algorithms*, 4th ed., §11.4 — open addressing, linear probing, and clustering.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §3.4 — linear-probing hash tables and load-factor management.
- Linear probing with tombstones and primary clustering is standard; both runnable blocks are verified by running (collision probes to adjacent slots; tombstone keeps `get(17)` correct after deleting `9`).
