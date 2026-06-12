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

Here's an array of numbers, and a loop that visits each one by index to add them up. Pick a test case below and **Run** it — then click **Visualise** and watch the index `i` walk the cells one at a time. Try your own `arr` too.

> ▶ Run it against a case, then click **Visualise** — watch the index `i` step across the cells, one slot of consecutive memory at a time.

```python run viz=array viz-root=arr
import ast

arr = ast.literal_eval(input())   # the test case's arr
total = 0
for i in range(len(arr)):
    total += arr[i]               # reach cell i directly
print(total)
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
  public static void main(String[] args) {
    int[] arr = parseIntArray(new Scanner(System.in).nextLine());
    int total = 0;
    for (int i = 0; i < arr.length; i++) {
      total += arr[i];                 // reach cell i directly
    }
    System.out.println(total);
  }

  // "[1, 2, 3]" → {1, 2, 3} — reads the test case's arr
  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
    return out;
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[5, 2, 8, 1, 9, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[5, 2, 8, 1, 9, 3]" }, "expected": "28" },
    { "args": { "arr": "[1, 2, 3, 4, 5]" }, "expected": "15" },
    { "args": { "arr": "[10]" }, "expected": "10" },
    { "args": { "arr": "[]" }, "expected": "0" }
  ]
}
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

You just saw that reading `arr[i]` is one instant jump — but reshaping is the expensive part: insert near the front and every later element has to shift right to keep the slots consecutive. Now do that shift yourself.

Implement `insert_at(arr, index, value)`: return a **new** array equal to `arr` with `value` placed at position `index`, so everything from `index` onward slides one slot to the right. Inserting at `0` shifts the whole array; inserting at `len(arr)` is a plain append that shifts nothing.

```python run viz=array viz-root=result
import ast

def insert_at(arr, index, value):
    # Your code goes here — build a new array with value at position index,
    # copying arr[0:index], then value, then the rest shifted right by one.
    return []

arr = ast.literal_eval(input())    # the test case's arr
index = int(input())               # where to insert
value = int(input())               # what to insert
result = insert_at(arr, index, value)
print(result)
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
  static int[] insertAt(int[] arr, int index, int value) {
    // Your code goes here — build a new array with value at position index,
    // copying arr[0..index), then value, then the rest shifted right by one.
    return new int[0];
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int index = Integer.parseInt(sc.nextLine().trim());
    int value = Integer.parseInt(sc.nextLine().trim());
    int[] result = insertAt(arr, index, value);
    System.out.println(Arrays.toString(result));
  }

  // "[1, 2, 3]" → {1, 2, 3} — reads the test case's arr
  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
    return out;
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[5, 2, 8, 1, 9, 3]" },
    { "id": "index", "label": "index", "type": "int", "placeholder": "0" },
    { "id": "value", "label": "value", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "arr": "[5, 2, 8, 1, 9, 3]", "index": "0", "value": "7" }, "expected": "[7, 5, 2, 8, 1, 9, 3]" },
    { "args": { "arr": "[5, 2, 8, 1, 9, 3]", "index": "3", "value": "99" }, "expected": "[5, 2, 8, 99, 1, 9, 3]" },
    { "args": { "arr": "[1, 2, 3]", "index": "3", "value": "4" }, "expected": "[1, 2, 3, 4]" },
    { "args": { "arr": "[]", "index": "0", "value": "1" }, "expected": "[1]" }
  ]
}
```

<details>
<summary>Editorial</summary>

The new value lands at `index`, so the array splits there: everything before `index` stays put, the value drops in, and everything from `index` onward shifts one slot to the right. Building a fresh array makes the shift explicit — copy the left part, append the value, then copy the right part. That's the `O(n)` reshape the chapter warned about: insert at the front (`index = 0`) and all `n` elements move; insert at the end (`index = len(arr)`) and none do.

```python solution time=O(n) space=O(n)
import ast

def insert_at(arr, index, value):
    result = []
    for i in range(index):              # the part before the gap stays put
        result.append(arr[i])
    result.append(value)                # the new value lands at index
    for i in range(index, len(arr)):    # everything after shifts right by one
        result.append(arr[i])
    return result

arr = ast.literal_eval(input())
index = int(input())
value = int(input())
result = insert_at(arr, index, value)
print(result)
```

```java solution
import java.util.*;

public class Main {
  static int[] insertAt(int[] arr, int index, int value) {
    int[] result = new int[arr.length + 1];
    for (int i = 0; i < index; i++) {            // the part before the gap stays put
      result[i] = arr[i];
    }
    result[index] = value;                       // the new value lands at index
    for (int i = index; i < arr.length; i++) {   // everything after shifts right by one
      result[i + 1] = arr[i];
    }
    return result;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int index = Integer.parseInt(sc.nextLine().trim());
    int value = Integer.parseInt(sc.nextLine().trim());
    int[] result = insertAt(arr, index, value);
    System.out.println(Arrays.toString(result));
  }

  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
    return out;
  }
}
```

</details>

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
