---
title: Arrays
summary: A block of consecutive memory slots, reachable by index in one step. The structure almost every other one in this book is built from — fast to read, costly to reshape.
tier: spine
prereqs:
  - foundations-how-a-computer-stores-data
  - foundations-measuring-cost
---

# Arrays

## Why It Exists

You can already picture a single value sitting in memory. But real programs juggle *thousands* at once — every pixel in a photo, every temperature reading in a day.

You can't give each one its own name. Writing `temp1`, `temp2`, … `temp1000` is madness, and worse, you can't *loop* over a thousand separate names. You need to store a whole **collection** under one handle, and still reach any single item instantly.

That's an array. It's the oldest data structure there is, and it's the one nearly every other structure in this book is secretly built on. Let's see why it's so fast.

## See It Work

Here's an array of six numbers, and a loop that visits each one to add them up. Run it — then click **Visualise** and watch the index `i` walk the cells one at a time.

> ▶ Run it, then hit **Visualise** — watch the index `i` step across the cells, one slot of consecutive memory at a time.

```python run viz=array viz-root=arr
arr = [5, 2, 8, 1, 9, 3]
total = 0
for i in range(len(arr)):
    total += arr[i]      # reach cell i directly
print(total)
```

## How It Works

An array is a **fixed-size** block of **consecutive** memory slots, all the same size, holding your items in order.

That word *consecutive* is the whole secret. Because the slots sit back-to-back starting at a known **base address**, the machine finds any item with pure arithmetic — no searching:

```
address of item i  =  base + i × item_size
```

The pieces:

- **index** — an item's position, counted from `0`. The first item is `arr[0]`.
- **element** — a single item stored in the array.

```d2
arr: one contiguous block of memory {
  grid-rows: 2
  grid-columns: 6
  grid-gap: 0
  v0: "5"
  v1: "2"
  v2: "8"
  v3: "1"
  v4: "9"
  v5: "3"
  i0: "[0]"
  i1: "[1]"
  i2: "[2]"
  i3: "[3]"
  i4: "[4]"
  i5: "[5]"
}
```

<p align="center"><strong>six elements packed in one contiguous block; the index beneath each cell names the slot, and <code>base + i × size</code> turns that index into an address.</strong></p>

To make this concrete: building on how a computer stores data, reaching `arr[i]` is one multiply, one add, and a jump — the `O(1)` address access you already met. It costs the same whether the array holds ten items or ten million.

But that speed has a price, and it's the array's defining tradeoff. The packed, fixed layout makes **reading and overwriting** any item `O(1)` — yet **inserting or removing** costs up to `O(n)`: every item *after* the gap must shift over to keep the slots consecutive, so changing the front moves all `n` items, while changing the very end moves none.

And because the block is a *fixed* size, you can't grow it — making room means a bigger block and a copy. Languages hide that behind a **growable array** (Python's `list`, Java's `ArrayList`) that appends cheaply *most* of the time and occasionally pauses to copy everything into a larger block. That's the **dynamic array** — a lesson of its own, and where the word "amortized" earns its keep.

So the key idea is: **an array trades flexibility for speed — instant access by index, but expensive to reshape.**

### Key Takeaway

`O(1)` to read or write any item by its index; up to `O(n)` to insert or remove one, because everything after the gap must shift to stay packed — most work at the front, none at the end.

## Trace It

Read `arr[3]`: the machine computes `base + 3 × item_size` and jumps there. One step — it never looks at `arr[0]`, `arr[1]`, or `arr[2]`.

Before you read on: to read `arr[999999]` in a million-item array, how many items does the CPU examine?

Still one. Indexing doesn't care how big the array is. Now the other side: insert a new value at the *front*, and all one million existing items must slide one slot to the right to make room. That's the `O(n)` cost of reshaping — and the reason so many patterns ahead work hard to avoid it.

## Your Turn

Reach in, overwrite, and reshape — and feel which operations are cheap:

```python run viz=array
arr = [5, 2, 8, 1, 9, 3]
print(arr[0], arr[3])     # O(1): jump straight to an index
arr[3] = 99               # O(1): overwrite in place
print(arr)
arr.insert(0, 7)          # O(n): everything shifts right
print(arr)
```

```java run viz=array
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    int[] arr = {5, 2, 8, 1, 9, 3};
    System.out.println(arr[0] + " " + arr[3]);   // O(1) access
    arr[3] = 99;                                  // O(1) update
    System.out.println(Arrays.toString(arr));

    // a growable list to show the O(n) front-insert
    ArrayList<Integer> list = new ArrayList<>(List.of(5, 2, 8, 1, 9, 3));
    list.add(0, 7);                               // shifts everything right
    System.out.println(list);
  }
}
```

## Pitfalls

The array is the simplest structure in this book and somehow ships with the longest list of bugs. Four worth keeping in view:

- **The fence-post.** Valid indices run `0` to `n − 1`, never `1` to `n`. Writing `range(1, n + 1)` when you meant `range(n)` is the most common array bug there is — check the bound at the *last* element first.
- **Out-of-bounds can be silent.** Python raises `IndexError`, Java throws, Rust panics — but C and C++ quietly read whatever bytes sit past the array. The program can look fine for years, then leak data or crash. Bounds-check yourself when the input is untrusted.
- **Don't reshape an array while you loop over it.** Deleting items as you iterate skips elements or throws. Filter into a *new* array instead: `[x for x in arr if keep(x)]`.
- **`arr[-1]` isn't universal.** In Python it means the last element; in C it reads memory *before* the array (undefined), and in Java it throws. Write `arr[len(arr) - 1]` when porting across languages.

## Reflect & Connect

Arrays are the floor of the whole book. Strings are arrays of characters; heaps and hash tables are array-backed, and stacks and queues are commonly built on arrays too (though a linked list works for those as well). Master this one tradeoff — instant indexing, costly reshaping — and the rest have a familiar shape.

And it isn't only textbook structures. The same contiguous block is the workhorse under real systems — don't worry if a name below is new (each gets its own lesson); the point is just how far the array reaches:

- **NumPy's `ndarray`** is one contiguous C buffer. That's why `np.sum(a)` flies while a hand-written Python `for` loop over the same data crawls — the loop pays interpreter overhead on every element; the buffer walk doesn't.
- **A Postgres B-tree index** stores each 8 KB page as a *sorted array* and binary-searches inside it, so a lookup on a billion-row table is just a handful of array searches.
- **A hash table is an array of buckets** — Java's `HashMap` is literally a `Node<K,V>[] table`, and Python's `dict` an array of entries. The hash code picks the index; the array does the rest.

**Prerequisites:** [How a Computer Stores Data](/cortex/data-structures-and-algorithms/foundations/how-a-computer-stores-data) and [Measuring Cost](/cortex/data-structures-and-algorithms/foundations/measuring-cost).
**What's next:** how a fixed array becomes a growable one — [Dynamic Arrays](/cortex/data-structures-and-algorithms/linear-structures/arrays/dynamic-arrays).

> Going deeper? Multi-dimensional arrays — grids, matrices, and row-major vs. column-major layout — get their own lesson: [Multidimensional](/cortex/data-structures-and-algorithms/linear-structures/arrays/multidimensional).

## Recall

> **Mnemonic:** *Consecutive slots, indexed from zero — instant to read, costly to reshape.*

| Operation | Cost | Why |
|---|---|---|
| read / write `arr[i]` | `O(1)` | address = base + i × size, then jump |
| insert / remove (middle or front) | `O(n)` | everything after the gap must shift to stay packed |
| append to a *growable* array | `O(1)` *amortized* | usually free; occasionally copies into a bigger block — "amortized" is defined in the next lesson |

<details>
<summary><strong>Q:</strong> Why is `arr[i]` `O(1)` no matter how large the array is?</summary>

**A:** The address is computed (`base + i × size`) and jumped to — no scanning.

</details>
<details>
<summary><strong>Q:</strong> What makes inserting at the front `O(n)`?</summary>

**A:** Every existing item must shift to keep the slots consecutive.

</details>
<details>
<summary><strong>Q:</strong> What single property gives arrays their speed?</summary>

**A:** Elements are stored in consecutive memory.

</details>
<details>
<summary><strong>Q:</strong> Name three structures built on arrays.</summary>

**A:** Strings, heaps, and hash tables (stacks and queues too, commonly).

</details>

## Sources & Verify

Don't take this on faith — check it against the canon:

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 4th ed. — arrays and the RAM cost model (Ch. 2); **dynamic tables / amortized append** (Ch. 17).
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §1.3–1.4 — resizing arrays and *amortized* analysis (`algs4.cs.princeton.edu`).
- **Skiena**, *The Algorithm Design Manual*, 3rd ed., §3 — contiguous vs. linked structures.
- **Ulrich Drepper**, *What Every Programmer Should Know About Memory* — why contiguous memory is fast at the hardware level (cache lines, prefetching); the "why" behind the NumPy speed gap above.
- **CPython source**, [`Objects/listobject.c`](https://github.com/python/cpython/blob/main/Objects/listobject.c) — the real growable-array implementation, including the over-allocation rule that gives `append` its amortized `O(1)`.
- Index-access is `O(1)`; appending to a growable array is `O(1)` **amortized**, `Θ(n)` on a resize — verify the "amortized" wording in any of the above.
