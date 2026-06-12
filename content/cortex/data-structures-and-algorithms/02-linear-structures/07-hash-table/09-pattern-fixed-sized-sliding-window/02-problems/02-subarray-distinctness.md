---
title: "Subarray Distinctness"
summary: "Given arr and a positive integer k, return an array containing the count of distinct elements in every contiguous subarray of size k."
prereqs:
  - 09-pattern-fixed-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [fixed-sized-sliding-window, hash-table]
---

# Subarray distinctness

## Problem Statement

Given `arr` and a positive integer `k`, return an array containing the count of distinct elements in every contiguous subarray of size `k`.

### Example 1
> -   **Input:** `arr = [2,1,2,3,2,1,4,5], k = 5` → **Output:** `[3, 3, 4, 5]`

### Example 2
> -   **Input:** `arr = [1,1,2,4], k = 3` → **Output:** `[2, 3]`

### Example 3
> -   **Input:** `arr = [1,2,3,4], k = 1` → **Output:** `[1, 1, 1, 1]`

## Examples

**Example 1**
```
Input:  arr = [2, 1, 2, 3, 2, 1, 4, 5], k = 5
Output: [3, 3, 4, 5]
Explanation: windows [2,1,2,3,2]→{2,1,3}=3, [1,2,3,2,1]→{1,2,3}=3,
[2,3,2,1,4]→{2,3,1,4}=4, [3,2,1,4,5]→{3,2,1,4,5}=5.
```

**Example 2**
```
Input:  arr = [1, 1, 2, 4], k = 3
Output: [2, 3]
Explanation: windows [1,1,2]→{1,2}=2, [1,2,4]→{1,2,4}=3.
```

**Example 3**
```
Input:  arr = [1, 2, 3, 4], k = 1
Output: [1, 1, 1, 1]
Explanation: every size-1 window holds exactly one distinct value.
```

**Example 4**
```
Input:  arr = [5, 5, 5], k = 3
Output: [1]
Explanation: the only window [5,5,5] holds one distinct value → [1].
```

## Constraints

- `1 <= k <= n` (where `n = len(arr)`)
- Array values are integers

```python run
import ast

def subarray_distinctness(arr, k):
    # Your code goes here
    pass

arr = ast.literal_eval(input())
k = int(input())
print(subarray_distinctness(arr, k))
```

```java run
import java.util.*;

public class Main {
  static int[] parseIntArray(String s) {
    s = s.trim().replaceAll("[\\[\\]\\s]", "");
    if (s.isEmpty()) return new int[]{};
    String[] parts = s.split(",");
    int[] a = new int[parts.length];
    for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
    return a;
  }

  static List<Integer> subarrayDistinctness(int[] arr, int k) {
    // Your code goes here
    return new ArrayList<>();
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(subarrayDistinctness(arr, k));
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "array",  "placeholder": "[2, 1, 2, 3, 2, 1, 4, 5]" },
    { "id": "k",   "label": "k",   "type": "number", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "arr": "[2, 1, 2, 3, 2, 1, 4, 5]", "k": "5" }, "expected": "[3, 3, 4, 5]" },
    { "args": { "arr": "[1, 1, 2, 4]",              "k": "3" }, "expected": "[2, 3]" },
    { "args": { "arr": "[1, 2, 3, 4]",              "k": "1" }, "expected": "[1, 1, 1, 1]" },
    { "args": { "arr": "[5, 5, 5]",                 "k": "3" }, "expected": "[1]" },
    { "args": { "arr": "[1, 2, 3, 4]",              "k": "4" }, "expected": "[4]" },
    { "args": { "arr": "[1, 1, 1, 1]",              "k": "2" }, "expected": "[1, 1, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **fixed-sized sliding window** problem is the phrase *every contiguous subarray of size `k`* — the answer is one number per window, and the window width is pinned at `k`. The number of distinct elements in a window is exactly the number of keys in its frequency map, so the map is the running summary the pattern maintains.

The window's two pointers each carry a fixed job. The `end` pointer adds the entering value and bumps its count; the `start` pointer drops the leaving value once the window has reached size `k`. The count of distinct elements is `len(map)` — but only if a key is removed the moment its count falls to zero, so the map's size never counts a value that has left the window.

The naive approach breaks the time budget. For each of the `n − k + 1` windows it rebuilds a fresh set or map by scanning all `k` elements, costing `O(N·k)` time for `O(k)` space. That re-counts the `k − 1` shared elements every slide. The sliding window edits the map in `O(1)` per step, so each window's distinct count is read directly from `len(map)`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Subarray Distinctness |
|---|---|
| **Q1.** Is the window size fixed at exactly `k`? | **Yes** — one distinct count is reported per size-`k` window; the size is given in the input. |
| **Q2.** Is the input a linear sequence? | **Yes** — an integer array, walked index by index. |
| **Q3.** Is the per-window answer read from an `O(1)`-updatable map? | **Yes** — the distinct count is `len(map)`, read in `O(1)` once the window is full. |
| **Q4.** Is the per-step work `O(1)` amortised? | **Yes** — one increment on expand, one decrement (plus an optional key delete) on contract. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Slide a window of size `k`, and report the map's key count each time the window is full.

1. **Add the entering element.** Read `arr[end]` and increment its count in the map.
2. **Report when the window is full.** When `end − start + 1 == k`, append `len(map)` — the distinct count for this exact window — to the result.
3. **Contract from the left.** Still inside the full-window branch, decrement `arr[start]`'s count, delete the key if it reaches zero, and advance `start`.
4. **Advance the right edge.** Increment `end` and continue until the sweep ends.
5. **Return the result.** The list holds one distinct count per window, `n − k + 1` entries in total.

</details>
<details>
<summary><strong>How the distinct count is maintained</strong></summary>


The number of *distinct* elements in the window is exactly `len(freq_map)` — the number of keys with non-zero count. The trick: when a frequency drops to zero on contraction, **delete the key** from the map so the size reflects only currently-present elements.

```d2
direction: right

w: "window contents" {
  grid-columns: 5
  grid-gap: 0
  a0: "2"
  a1: "1"
  a2: "2"
  a3: "3"
  a4: "2"
}

m: "freq map" {
  m1: "2 -> 3"
  m2: "1 -> 1"
  m3: "3 -> 1"
}

d: |md
  **distinct = len(freq) = 3**
| {style.fill: "#dcfce7"; style.stroke: "#16a34a"}

w -> m
m -> d
```

<p align="center"><strong>Distinct count via map size — every distinct element is one key in the map. Maintain the map's invariant that "count is non-zero" by deleting zero-count keys on contraction, and <code>len(map)</code> is your answer.</strong></p>

</details>
<details>
<summary>Editorial</summary>

Slide a window of size `k`. When the window is full (`end - start + 1 == k`), append `len(frequency)` — the distinct count — then evict the left element (decrement, delete at zero) before advancing `start`. Deleting zero-count keys is what keeps `len(map)` honest as a distinct count. `O(n)` time, `O(k)` space.

```python solution time=O(n) space=O(k)
import ast
from collections import defaultdict

def subarray_distinctness(arr, k):
    frequency = defaultdict(int)
    start, end = 0, 0
    result = []
    while end < len(arr):
        frequency[arr[end]] += 1
        if end - start + 1 == k:
            result.append(len(frequency))
            frequency[arr[start]] -= 1
            if frequency[arr[start]] == 0:
                del frequency[arr[start]]
            start += 1
        end += 1
    return result

arr = ast.literal_eval(input())
k = int(input())
print(subarray_distinctness(arr, k))
```

```java solution
import java.util.*;

public class Main {
  static int[] parseIntArray(String s) {
    s = s.trim().replaceAll("[\\[\\]\\s]", "");
    if (s.isEmpty()) return new int[]{};
    String[] parts = s.split(",");
    int[] a = new int[parts.length];
    for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
    return a;
  }

  static List<Integer> subarrayDistinctness(int[] arr, int k) {
    Map<Integer, Integer> frequency = new HashMap<>();
    int start = 0, end = 0;
    List<Integer> result = new ArrayList<>();
    while (end < arr.length) {
      frequency.put(arr[end], frequency.getOrDefault(arr[end], 0) + 1);
      if (end - start + 1 == k) {
        result.add(frequency.size());
        int startElement = arr[start];
        frequency.put(startElement, frequency.get(startElement) - 1);
        if (frequency.get(startElement) == 0) frequency.remove(startElement);
        start++;
      }
      end++;
    }
    return result;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(subarrayDistinctness(arr, k));
  }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `arr = [2, 1, 2, 3, 2, 1, 4, 5]`, `k = 5`. The map fills for four steps, then reports and contracts on each full window:

```
start=0, end=0, frequency={}, result=[]

end=0  add 2 → freq={2:1}              size 1 < k → continue
end=1  add 1 → freq={2:1,1:1}          size 2 < k → continue
end=2  add 2 → freq={2:2,1:1}          size 3 < k → continue
end=3  add 3 → freq={2:2,1:1,3:1}      size 4 < k → continue
end=4  add 2 → freq={2:3,1:1,3:1}      size 5 == k → append len=3 → result=[3]
                                        drop arr[0]=2 → freq={2:2,1:1,3:1}, start=1
end=5  add 1 → freq={2:2,1:2,3:1}      size 5 == k → append len=3 → result=[3,3]
                                        drop arr[1]=1 → freq={2:2,1:1,3:1}, start=2
end=6  add 4 → freq={2:2,1:1,3:1,4:1}  size 5 == k → append len=4 → result=[3,3,4]
                                        drop arr[2]=2 → freq={2:1,1:1,3:1,4:1}, start=3
end=7  add 5 → freq={2:1,1:1,3:1,4:1,5:1}  size 5 == k → append len=5 → result=[3,3,4,5]
                                        drop arr[3]=3 → freq={2:1,1:1,4:1,5:1}, start=4

result = [3, 3, 4, 5]
```

The result `[3, 3, 4, 5]` matches the expected output — note the append uses `len(map)` *before* the contraction, so each report reflects the full size-`5` window.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `end` sweeps the array once; each step is one insert, at most one decrement-and-delete, and one `len` read — all amortised `O(1)`. |
| Space | **O(k)** | The map holds at most `k` distinct values; the result list adds `O(n − k + 1)` for the per-window output. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr = [1], k = 1` | `[1]` | One window of size `1` with one distinct value. |
| `k == 1` | `arr = [1, 2, 3, 4], k = 1` | `[1, 1, 1, 1]` | Every size-`1` window holds exactly one distinct value. |
| All identical | `arr = [5, 5, 5], k = 3` | `[1]` | The only window has one distinct value despite three elements. |
| `k == n` | `arr = [1, 2, 3, 4], k = 4` | `[4]` | One window covering the whole array; all four values distinct. |
| Repeats within window | `arr = [1, 1, 1, 1], k = 2` | `[1, 1, 1]` | Each size-`2` window is `[1, 1]` → one distinct value, three windows. |
| Mixed counts | `arr = [1, 1, 2, 4], k = 3` | `[2, 3]` | `[1,1,2]`→`{1,2}`=2, `[1,2,4]`→`{1,2,4}`=3. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the per-window-report shape of the fixed window: append `len(map)` once each window reaches size `k`. The result holds exactly `n − k + 1` values, and deleting zero-count keys is what keeps `len(map)` an honest distinct count.

</details>