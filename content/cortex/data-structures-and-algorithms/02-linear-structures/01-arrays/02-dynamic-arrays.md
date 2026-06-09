---
title: Dynamic Arrays
summary: How a fixed-size array becomes a growable one. Doubling the capacity when it fills makes append O(1) amortized while keeping O(1) random access — the state machine inside every list, vector, and ArrayList.
tier: spine
prereqs:
  - linear-structures-arrays-what-is-an-array
  - foundations-measuring-cost
---

# Dynamic Arrays

## Why It Exists

You just learned that an array is a *fixed-size* block — you decide its length when you create it, and it can't grow. Yet every day you write `list.append(x)` in Python or `ArrayList.add(x)` in Java and push a thousand, a million values without once thinking about the size. The container always has room.

So how does a structure built on fixed-size blocks pretend to be infinitely growable? And here's the part that should bother you: when the block fills up, the only way to get a bigger one is to allocate a fresh block and **copy everything across** — an `O(n)` move. If that happened on every push, a million appends would cost a *trillion* copies. It doesn't. Appending is `O(1)` on average. How?

That's the **dynamic array**, and the trick that makes it work is one of the most reused ideas in systems programming.

## See It Work

Here's a `DynamicArray` built from a fixed block plus two counters. Run it — then click **Visualise** and watch the backing block *jump* to a bigger size each time it fills.

> ▶ Run it, then click **Visualise** — watch the backing array jump to a bigger block each time it fills; the trailing `0`s are reserved-but-unused slots.

```python run viz=array viz-root=arr
class DynamicArray:
    def __init__(self):
        self.arr = []          # the fixed backing block
        self.size = 0          # how many slots the caller has filled
        self.capacity = 0      # how many slots are allocated

    def push_back(self, val):
        if self.size == self.capacity:                       # block is full → grow
            self.capacity = 1 if self.capacity == 0 else self.capacity * 2
            bigger = [0] * self.capacity
            for i in range(self.size):                       # copy everything across
                bigger[i] = self.arr[i]
            self.arr = bigger
        self.arr[self.size] = val
        self.size += 1

da = DynamicArray()
for v in [1, 2, 3, 4, 5]:
    da.push_back(v)
print(da.arr, "size =", da.size)   # [1, 2, 3, 4, 5, 0, 0, 0] size = 5
```

## How It Works

A dynamic array is a fixed array plus one idea: **track two numbers, and grow the block by doubling when it fills.**

- **size** — how many elements the caller has actually stored.
- **capacity** — how many slots are currently allocated. The gap between them is *headroom* that absorbs the next push for free.
- **grow by doubling** — when `size` hits `capacity`, allocate a new block of `capacity × 2`, copy the elements over, and keep going.

```mermaid
flowchart TB
  P["push(x)"] --> Q{"size == capacity?"}
  Q -- "no — room left" --> W["write x at the end, size += 1"]
  Q -- "yes — full" --> R["allocate a block twice as big,<br/>copy all elements over"] --> W
```

<p align="center"><strong>most pushes just write and bump the size; only a <em>full</em> block takes the slow path: double the capacity and copy everything across.</strong></p>

The doubling is the whole secret, and it's worth seeing *why* it works instead of the obvious alternative. Suppose instead you grew by **one slot** each time it filled. Every push past the first would copy every existing element: `1 + 2 + 3 + … + n` copies in total, which is `O(n²)` — pushing a million items would do half a trillion copies.

Doubling turns that sum into a *geometric* one — each term double the one before. To reach `n` elements the block doubles at capacities `1, 2, 4, 8, …, n`, so the total copy work across **all** resizes is:

```
1 + 2 + 4 + 8 + … + n  <  2n
```

That's `O(n)` total work for `n` pushes — which is `O(1)` per push **on average**. This is what "amortized" means, and it's a stronger promise than it first looks. It is not "fast on average if you're lucky": amortized `O(1)` is a *worst-case* guarantee over **any** sequence of pushes. Each rare, expensive resize is paid for in advance by all the cheap pushes around it.

Why does *multiplying* the capacity work when *adding* to it didn't? Because multiplying makes the capacities grow geometrically, so the copy costs at successive resizes form a geometric series too — and a geometric series always sums to a constant times `n`, never more. Doubling sums to `< 2n`; a gentler factor like `1.5×` copies more in total, but still only a (larger) constant times `n`, so it stays `O(n)`. That's why **any** growth factor above `1` gives amortized `O(1)` — the factor just trades how often you resize against how much memory you waste. Real libraries pick different points: Java's `ArrayList` grows `1.5×`, Python's `list` by roughly an eighth (`≈1.125×`), and a `std::vector` typically `1.5×` or `2×`.

### Key Takeaway

Doubling makes resizes geometrically rarer as they grow more expensive, so the total copy work stays linear in `n`: append is `O(1)` amortized and random access stays `O(1)` — paid for with up to `2×` slack memory.

## Trace It

Push `1, 2, 3, …` into an empty dynamic array and watch when a resize fires. The **cost** of a push is the copies it does plus the one write:

| push | full? | capacity before → after | copies | push cost |
|---|---|---|---|---|
| 1 | yes | 0 → 1 | 0 | 1 |
| 2 | yes | 1 → 2 | 1 | 2 |
| 3 | yes | 2 → 4 | 2 | 3 |
| 4 | no | 4 → 4 | 0 | 1 |
| 5 | yes | 4 → 8 | 4 | 5 |

Push 4 fit without resizing. Before you read on: of pushes 5, 6, 7, and 8, how many trigger a resize?

Only push 5. The jump to capacity `8` at push 5 buys room for pushes 6, 7, and 8 to land for free. Across all 8 pushes the total cost is `1+2+3+1+5+1+1+1 = 15` — under `2 × 8` — so the average is `≈1.87`, a constant. The resizes fire at pushes `1, 2, 3, 5, 9, 17, …`: every power of two, exponentially rarer as the array grows.

## Your Turn

You just watched the machinery. In real code it's invisible — `list.append` *is* this dynamic array, resizing itself only a couple dozen times for a thousand items, never once telling you:

```python run viz=array
nums = []
for i in range(1000):
    nums.append(i)          # amortized O(1) — a couple dozen hidden resizes, not 1000
print(len(nums), "items; first", nums[0], "last", nums[-1])
```

```java run viz=array
import java.util.ArrayList;

public class Main {
  public static void main(String[] args) {
    // ArrayList is Java's dynamic array — the same doubling underneath (it grows 1.5×).
    ArrayList<Integer> nums = new ArrayList<>();
    for (int i = 0; i < 1000; i++) nums.add(i);
    System.out.println(nums.size() + " items; first " + nums.get(0) + " last " + nums.get(999));
  }
}
```

Want to build the whole thing yourself — `get`, `size`, and a `popBack` that *shrinks* the block without thrashing at the boundary? That's the [Design a Dynamic Array](/cortex/data-structures-and-algorithms/linear-structures/arrays/design-a-dynamic-array/design-a-dynamic-array) challenge.

## Reflect & Connect

The dynamic array is the structure you reach for without naming it. Python's `list`, Java's `ArrayList`, C++'s `std::vector`, Go's slices, Rust's `Vec` — every one runs this exact resize-and-copy state machine on every append.

The tradeoff to remember: you buy `O(1)` amortized append *and* `O(1)` random access by paying up to `2×` slack memory — right after a resize, half the block is empty. Two consequences worth carrying forward:

- **Pre-size when you know the count.** `[0] * n` or `new ArrayList<>(n)` reserves once and skips every resize — the fix for the "append in a hot loop occasionally stalls" surprise from the [arrays pitfalls](/cortex/data-structures-and-algorithms/linear-structures/arrays/what-is-an-array).
- **Shrinking is trickier than growing.** A `popBack` that halves the block as soon as it's half-empty will *oscillate* — one push past the boundary doubles, one pop halves, forever. The fix is to shrink only at a *quarter* full, leaving a buffer zone.

**Prerequisites:** [Arrays](/cortex/data-structures-and-algorithms/linear-structures/arrays/what-is-an-array) and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/measuring-cost).
**What's next:** the first *pattern* built on the array's layout — [Two Pointers](/cortex/data-structures-and-algorithms/linear-structures/arrays/pattern-two-pointers/pattern).

## Recall

> **Mnemonic:** *Full? Double and copy. Rare big cost, paid for by the cheap pushes around it.*

| Operation | Cost | Why |
|---|---|---|
| `append` / `push_back` | `O(1)` amortized | doubling spreads the rare `O(n)` resize across `n` cheap pushes |
| `append` on a resize | `O(n)` (one-off) | the full block is copied into one twice the size |
| read / write `arr[i]` | `O(1)` | still a contiguous block — address arithmetic, unchanged |
| extra memory | up to `2×` size | half the block can be empty right after a resize |

<details>
<summary><strong>Q:</strong> Why grow by doubling instead of by a fixed number of slots?</summary>

**A:** Doubling gives `O(n)` total copy work (`O(1)` amortized); fixed growth gives `O(n²)`.

</details>
<details>
<summary><strong>Q:</strong> What does "amortized `O(1)`" guarantee?</summary>

**A:** Any sequence of `n` pushes costs `O(n)` total — a worst-case bound over the sequence, not a lucky average.

</details>
<details>
<summary><strong>Q:</strong> What's the price of amortized `O(1)` append?</summary>

**A:** Up to `2×` memory — half the block sits empty right after a resize.

</details>
<details>
<summary><strong>Q:</strong> How do you skip the resizes entirely?</summary>

**A:** Pre-size the block when you know the final count.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 4th ed., **Ch. 17 — Amortized Analysis**: dynamic tables, table doubling, and the aggregate / accounting / potential methods. The canonical proof that doubling gives `O(1)` amortized.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §1.3–1.4 — resizing arrays and the amortized cost of `push`/`pop` (`algs4.cs.princeton.edu`).
- **CPython** [`Objects/listobject.c`](https://github.com/python/cpython/blob/main/Objects/listobject.c) (`list_resize`) and **OpenJDK** [`ArrayList.java`](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/ArrayList.java) (`grow`) — the real growth factors (`≈1.125×` and `1.5×`); verify the numbers quoted above against the source.
- The `1 + 2 + 4 + … + n < 2n` bound is the standard geometric-series argument; both code blocks are verified by running.
