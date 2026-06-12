---
title: "Design a Dynamic Array"
summary: "Build a DynamicArray class from scratch with pushBack, get, and size each in amortised O(1) time using geometric capacity doubling."
prereqs:
  - linear-structures-arrays-what-is-an-array
difficulty: medium
kind: problem
topics: [arrays, design]
---

# Design a Dynamic Array

## The Problem

You call `list.append` or `ArrayList.add` a million times and the container always has room — yet a raw array is a *fixed* block that can't grow. Build the structure that fakes unlimited growth: a `DynamicArray` that stays **amortised O(1)** per operation even though it sits on fixed blocks underneath.

> Complete a `DynamicArray` class that supports the following operations, each with **amortised O(1)** time:
>
> - `DynamicArray()` — construct an empty dynamic array.
> - `pushBack(val)` — append `val` to the end of the array.
> - `get(index)` — return the value at position `index`.
> - `size()` — return the current number of stored elements.

## Example

```
Input:
  ops   = [DynamicArray, pushBack, pushBack, get, size, pushBack, size, get]
  args  = [[],           [2],      [3],      [1], [],   [5],      [],   [0]]

Output:
  [null, null, null, 3, 2, null, 3, 2]

Step-by-step:
  DynamicArray()  → arr = []
  pushBack(2)     → arr = [2]
  pushBack(3)     → arr = [2, 3]
  get(1)          → 3
  size()          → 2
  pushBack(5)     → arr = [2, 3, 5]
  size()          → 3
  get(0)          → 2
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "after pushing 5 values into an empty DynamicArray (capacity doubles 1 → 2 → 4 → 8)",
  "options": ["capacity 8", "capacity 5", "capacity 4", "capacity 16"],
  "answer": "capacity 8"
}
```

## Constraints

- `0 ≤ values.length ≤ 10^5`, `-10^9 ≤ values[i] ≤ 10^9`
- every operation runs in **amortised O(1)** time

The workbench drives your class the simple way: it pushes a list of `values` one by one, then prints the live elements, the current `size`, and the backing `capacity` — so you can watch the block **double** (`1 → 2 → 4 → 8`) as it fills. Implement `push_back`, `get`, and `size`.

```python run viz=array viz-root=arr
import ast
from typing import List

class DynamicArray:
    def __init__(self):
        self.arr = None          # the backing block
        self.current_size = 0    # elements stored
        self.capacity = 0        # slots allocated

    def push_back(self, val: int) -> None:
        # Your code goes here — when current_size >= capacity, allocate a block
        # of capacity*2 (or 1 if empty), copy the elements across, then append.
        pass

    def get(self, index: int) -> int:
        # Your code goes here — return the element at index.
        return 0

    def size(self) -> int:
        # Your code goes here — return the number of stored elements.
        return 0

values = ast.literal_eval(input())   # the test case's values to push
da = DynamicArray()
for v in values:
    da.push_back(v)
print([da.get(i) for i in range(da.size())], "size =", da.size(), "capacity =", da.capacity)
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class DynamicArray {
        private int[] arr;        // the backing block
        private int currentSize;  // elements stored
        private int capacity;     // slots allocated

        public DynamicArray() {
            arr = null;
            currentSize = 0;
            capacity = 0;
        }

        public void pushBack(int val) {
            // Your code goes here — when currentSize >= capacity, allocate a
            // block of capacity*2 (or 1 if empty), copy elements across, append.
        }

        public int get(int index) {
            // Your code goes here — return the element at index.
            return 0;
        }

        public int size() {
            // Your code goes here — return the number of stored elements.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] values = parseIntArray(sc.nextLine());
        DynamicArray da = new DynamicArray();
        for (int v : values) da.pushBack(v);
        StringBuilder live = new StringBuilder("[");
        for (int i = 0; i < da.size(); i++) {
            if (i > 0) live.append(", ");
            live.append(da.get(i));
        }
        live.append("]");
        System.out.println(live + " size = " + da.size() + " capacity = " + da.capacity);
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

```testcases
{
  "args": [
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[2, 3, 5]" }
  ],
  "cases": [
    { "args": { "values": "[2, 3, 5]" }, "expected": "[2, 3, 5] size = 3 capacity = 4" },
    { "args": { "values": "[1, 2, 3, 4, 5]" }, "expected": "[1, 2, 3, 4, 5] size = 5 capacity = 8" },
    { "args": { "values": "[1, 2, 3, 4]" }, "expected": "[1, 2, 3, 4] size = 4 capacity = 4" },
    { "args": { "values": "[7]" }, "expected": "[7] size = 1 capacity = 1" },
    { "args": { "values": "[]" }, "expected": "[] size = 0 capacity = 0" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

A raw array is a fixed block — allocate ten slots and the eleventh value has nowhere to go. A dynamic array fakes unlimited growth by tracking **two numbers** and growing the block when it fills:

- **size** — how many elements the caller has stored.
- **capacity** — how many slots are allocated. The gap between them is headroom that absorbs the next push for free.

When `size` reaches `capacity`, allocate a new block **twice** as big, copy the elements across, and keep going. That single choice — *double* on overflow rather than grow by one — is what keeps `pushBack` cheap.

**Why doubling gives amortised `O(1)`.** Growing by one slot would copy every element on every push: `1 + 2 + … + N = O(N²)` total. Doubling instead resizes at capacities `1, 2, 4, …, N`, so the copy work across *all* resizes is a geometric series `1 + 2 + 4 + … + N < 2N` — linear in `N`. Add `N` cheap writes and the total for `N` pushes is under `3N` operations: `O(N)` total, **`O(1)` per push on average**. "Amortised" is a worst-case guarantee over the whole sequence, not a lucky average — each rare `O(N)` resize is pre-paid by the cheap pushes around it.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(1)-amortized space=O(1)
import ast
from typing import List

class DynamicArray:
    def __init__(self):

        # Pointer to the dynamically allocated array
        self.arr = None

        # Current number of elements in the array
        self.current_size = 0

        # Current capacity of the array
        self.capacity = 0

    def push_back(self, val: int) -> None:
        if self.current_size >= self.capacity:

            # If the capacity is not enough, resize the array
            new_capacity = 1 if self.capacity == 0 else self.capacity * 2
            new_arr = [0] * new_capacity

            # Copy the existing elements to the new array
            for i in range(self.current_size):
                new_arr[i] = self.arr[i]

            # Assign the new array and update the capacity
            self.arr = new_arr
            self.capacity = new_capacity

        # Add the new element to the end of the array
        self.arr[self.current_size] = val
        self.current_size += 1

    def get(self, index: int) -> int:
        return self.arr[index]

    def size(self) -> int:
        return self.current_size


values = ast.literal_eval(input())   # the test case's values to push
da = DynamicArray()
for v in values:
    da.push_back(v)
print([da.get(i) for i in range(da.size())], "size =", da.size(), "capacity =", da.capacity)
```

```java solution
import java.util.*;

public class Main {
    static class DynamicArray {

        // Pointer to the dynamically allocated array
        private int[] arr;

        // Current number of elements in the array
        private int currentSize;

        // Current capacity of the array
        private int capacity;

        public DynamicArray() {

            // Initialize the array
            arr = null;

            // Initialize the currentSize to 0
            currentSize = 0;

            // Initialize the capacity to 0
            capacity = 0;
        }

        public void pushBack(int val) {
            if (currentSize >= capacity) {

                // If the capacity is not enough, resize the array
                int newCapacity = (capacity == 0) ? 1 : capacity * 2;
                int[] newArr = new int[newCapacity];

                // Copy the existing elements to the new array
                if (arr != null) {
                    System.arraycopy(arr, 0, newArr, 0, currentSize);
                }

                // Assign the new array and update the capacity
                arr = newArr;
                capacity = newCapacity;
            }

            // Add the new element to the end of the array
            arr[currentSize] = val;
            currentSize++;
        }

        public int get(int index) {
            return arr[index];
        }

        public int size() {
            return currentSize;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] values = parseIntArray(sc.nextLine());
        DynamicArray da = new DynamicArray();
        for (int v : values) da.pushBack(v);
        StringBuilder live = new StringBuilder("[");
        for (int i = 0; i < da.size(); i++) {
            if (i > 0) live.append(", ");
            live.append(da.get(i));
        }
        live.append("]");
        System.out.println(live + " size = " + da.size() + " capacity = " + da.capacity);
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

### Dry Run — pushBack 2, 3, 5 into an empty array

```
State format: arr | currentSize | capacity

Init           │ []                      │ size=0 │ cap=0

pushBack(2):
  size ≥ cap (0 ≥ 0) → resize: newCap = 1, new_arr = [0]
  copy 0 elements (nothing to do)
  write arr[0] = 2, size++
  State: [2]                              │ size=1 │ cap=1

pushBack(3):
  size ≥ cap (1 ≥ 1) → resize: newCap = 2, new_arr = [0, 0]
  copy 1 element: new_arr[0] = 2 → [2, 0]
  write arr[1] = 3, size++
  State: [2, 3]                           │ size=2 │ cap=2

pushBack(5):
  size ≥ cap (2 ≥ 2) → resize: newCap = 4, new_arr = [0, 0, 0, 0]
  copy 2 elements: new_arr = [2, 3, 0, 0]
  write arr[2] = 5, size++
  State: [2, 3, 5, 0]                     │ size=3 │ cap=4
```

After 3 pushes the capacity is already 4 — the slot `arr[3] = 0` is the wasted headroom that buys amortised `O(1)`; the next push fits without a resize.

### Complexity Analysis

| Operation | Time (worst case) | Time (amortised) | Space |
|---|---|---|---|
| `DynamicArray()` | O(1) | O(1) | O(1) |
| `pushBack(val)` | **O(N)** during resize | **O(1) amortised** | O(1) extra |
| `get(index)` | O(1) | O(1) | O(1) |
| `size()` | O(1) | O(1) | O(1) |

`N` pushes trigger resizes at capacities `1, 2, 4, …, N`, so total copy work is `1 + 2 + 4 + … + N < 2N`, plus `N` writes — under `3N` ops for `N` pushes. **Space** is `O(N)`, worst case `2×` the size actually used right after a resize: that slack is the explicit trade-off for amortised `O(1)`.

### Edge Cases

| Case | Example | Expected Behaviour |
|---|---|---|
| First push into empty array | `pushBack(5)` on a fresh instance | Resizes `0 → 1`, stores `5` |
| Push exactly at capacity | push when `size == capacity` | Triggers a resize to `capacity * 2` |
| `get` out of range | `get(10)` after 3 pushes | Language-dependent crash (raw-array semantics) |
| `size()` after no pushes | just constructed | Returns `0` — not `capacity` |
| Very large `N` | push 10⁶ elements | Only ~20 resizes total; overall `O(N)` work |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

A dynamic array is a fixed-size array plus two choices: **track `size` and `capacity` separately** (the gap is headroom), and **double the capacity on overflow** (geometric growth makes resizes exponentially rarer). Together they buy `O(1)` random access *and* `O(1)` amortised append, paid for with a bounded amount of slack memory — the exact state machine behind every `list.append` and `vec.push`.

> **Transfer Challenge:** add a `popBack()` — should it ever *shrink* the block? Shrink when `size` drops to a **quarter** of capacity (not half), halving the block. The ¼ threshold leaves a buffer zone so alternating push/pop can't oscillate into a resize every step. That's the [Dynamic Arrays](/cortex/data-structures-and-algorithms/linear-structures/arrays/dynamic-arrays) lesson's Your-Turn exercise.

</details>
