---
title: "Subarray Distinctness"
summary: "Given arr and a positive integer k, return an array containing the count of distinct elements in every contiguous subarray of size k."
prereqs:
  - 09-pattern-fixed-sized-sliding-window/01-pattern
difficulty: medium
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
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=result
from collections import defaultdict
from typing import List

class Solution:
    def subarray_distinctness(self, arr: List[int], k: int) -> List[int]:

        # Initialize a dictionary to keep track of the count of elements
        # in the current window
        frequency = defaultdict(int)

        # Initialize the start and end indices of the window
        start, end = 0, 0

        # Initialize the result list to hold the count of distinct
        # elements in every subarray
        result = []

        # Loop through the array
        while end < len(arr):

            # Add the current element to the count dictionary
            frequency[arr[end]] += 1

            # If the current window size is equal to k, calculate the
            # count of distinct elements
            if end - start + 1 == k:
                result.append(len(frequency))

                # Remove the leftmost element from the count dictionary
                frequency[arr[start]] -= 1
                if frequency[arr[start]] == 0:
                    del frequency[arr[start]]

                # Contract the window
                start += 1

            # Expand the window to the right
            end += 1

        return result


# Examples from the problem statement
print(Solution().subarray_distinctness([2, 1, 2, 3, 2, 1, 4, 5], 5))  # [3, 3, 4, 5]
print(Solution().subarray_distinctness([1, 1, 2, 4], 3))               # [2, 3]
print(Solution().subarray_distinctness([1, 2, 3, 4], 1))               # [1, 1, 1, 1]

# Edge cases
print(Solution().subarray_distinctness([1], 1))                         # [1]
print(Solution().subarray_distinctness([1, 1, 1, 1], 2))               # [1, 1, 1]
print(Solution().subarray_distinctness([1, 2, 3, 4], 4))               # [4]
print(Solution().subarray_distinctness([5, 5, 5], 3))                  # [1]
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> subarrayDistinctness(int[] arr, int k) {

            // Initialize a map to keep track of the count of elements in the
            // current window
            Map<Integer, Integer> frequency = new HashMap<>();

            // Initialize the start and end indices of the window
            int start = 0;
            int end = 0;

            // Initialize the result list to hold the count of distinct
            // elements in every subarray
            List<Integer> result = new ArrayList<>();

            // Loop through the array
            while (end < arr.length) {

                // Add the current element to the count map
                frequency.put(
                    arr[end],
                    frequency.getOrDefault(arr[end], 0) + 1
                );

                // If the current window size is equal to k, calculate the
                // count of distinct elements
                if (end - start + 1 == k) {
                    result.add(frequency.size());

                    // Remove the leftmost element from the count map
                    int startElement = arr[start];
                    frequency.put(
                        startElement,
                        frequency.get(startElement) - 1
                    );
                    if (frequency.get(startElement) == 0) {
                        frequency.remove(startElement);
                    }

                    // Contract the window
                    start++;
                }

                // Expand the window to the right
                end++;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().subarrayDistinctness(new int[]{2, 1, 2, 3, 2, 1, 4, 5}, 5)); // [3, 3, 4, 5]
        System.out.println(new Solution().subarrayDistinctness(new int[]{1, 1, 2, 4}, 3));              // [2, 3]
        System.out.println(new Solution().subarrayDistinctness(new int[]{1, 2, 3, 4}, 1));              // [1, 1, 1, 1]

        // Edge cases
        System.out.println(new Solution().subarrayDistinctness(new int[]{1}, 1));                       // [1]
        System.out.println(new Solution().subarrayDistinctness(new int[]{1, 1, 1, 1}, 2));              // [1, 1, 1]
        System.out.println(new Solution().subarrayDistinctness(new int[]{1, 2, 3, 4}, 4));              // [4]
        System.out.println(new Solution().subarrayDistinctness(new int[]{5, 5, 5}, 3));                 // [1]
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