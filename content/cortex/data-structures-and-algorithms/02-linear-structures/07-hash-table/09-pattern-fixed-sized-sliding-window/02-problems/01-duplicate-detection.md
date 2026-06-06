---
title: "Duplicate Detection"
summary: "Given an integer array arr and a positive integer k, return true if any subarray of size k contains a duplicate, false otherwise."
prereqs:
  - 09-pattern-fixed-sized-sliding-window/01-pattern
difficulty: easy
---

# Duplicate detection

## Problem Statement

Given an integer array `arr` and a positive integer `k`, return `true` if any subarray of size `k` contains a duplicate, `false` otherwise.

### Example 1
> -   **Input:** `arr = [2, 1, 2, 3, 2, 1, 4, 5], k = 5` → **Output:** `true`

### Example 2
> -   **Input:** `arr = [1, 1, 2, 4], k = 3` → **Output:** `true`

### Example 3
> -   **Input:** `arr = [1, 2, 3, 4], k = 2` → **Output:** `false`

## Examples

**Example 1**
```
Input:  arr = [2, 1, 2, 3, 2, 1, 4, 5], k = 5
Output: true
Explanation: the first window [2, 1, 2, 3, 2] holds three 2s → a duplicate exists.
```

**Example 2**
```
Input:  arr = [1, 1, 2, 4], k = 3
Output: true
Explanation: the window [1, 1, 2] contains two 1s → a duplicate exists.
```

**Example 3**
```
Input:  arr = [1, 2, 3, 4], k = 2
Output: false
Explanation: every size-2 window holds two distinct values → no duplicate.
```

**Example 4**
```
Input:  arr = [1, 2, 1], k = 5
Output: true
Explanation: k exceeds the array, so the only window is the whole array [1, 2, 1],
which holds two 1s → a duplicate exists.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **fixed-sized sliding window** problem is the phrase *subarray of size `k`* — the answer is asked of every contiguous `k`-wide window, and the size never changes. A duplicate inside a window is a pure frequency fact: some value appears more than once. That is exactly the summary a hash map maintains as the window slides.

The window's two pointers each play a fixed role. The `end` pointer brings a new value in and bumps its count; the `start` pointer drops the value that just fell off the left edge once the window already spans `k`. The frequency map between them holds the count of every value currently inside, so a duplicate check is reading whether the just-added value's count exceeds `1`.

The naive approach breaks the time budget. For each starting index it re-examines the whole `k`-window with a pairwise scan, costing `O((N − k)·k²)` time for `O(1)` space. That re-counts the `k − 1` shared elements on every slide. The sliding window edits the map in `O(1)` per step, so the duplicate check drops to a single `O(1)` lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Duplicate Detection |
|---|---|
| **Q1.** Is the window size fixed at exactly `k`? | **Yes** — every checked subarray is exactly `k` wide; the size is given in the input. |
| **Q2.** Is the input a linear sequence? | **Yes** — an integer array, walked index by index. |
| **Q3.** Is the per-window answer read from an `O(1)`-updatable map? | **Yes** — a frequency map; a duplicate is any count `> 1`, read after each insert. |
| **Q4.** Is the per-step work `O(1)` amortised? | **Yes** — one increment on expand, one decrement on contract, one count lookup. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Slide a window of size `k` while maintaining a frequency map, and short-circuit the moment a count climbs above `1`.

1. **Add the entering element.** Read `arr[end]` and increment its count in the map.
2. **Contract once the window spans `k`.** When `end − start >= k`, decrement `arr[start]`'s count, delete the key if it hits zero, and advance `start` — this keeps the window at exactly `k`.
3. **Check for a duplicate.** If the just-added element's count exceeds `1`, a value repeats inside the current `k`-window, so return `true`.
4. **Advance the right edge.** Increment `end` and continue until the sweep ends.
5. **Return `false`.** If no window ever held a repeat, no duplicate exists.

> *Mental shortcut* — duplicate-in-window is "is the window's distinct-set smaller than `k`?". Equivalently, "did any insert push a frequency above `1`?". The hash map answers both views in `O(1)`.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array
from collections import defaultdict
from typing import List

class Solution:
    def duplicate_detection(self, arr: List[int], k: int) -> bool:

        # Map to store elements within the window and their counts
        frequency = defaultdict(int)

        # The start and end pointers for the window
        start, end = 0, 0

        while end < len(arr):

            # Add the current element to the window
            end_element = arr[end]
            frequency[end_element] += 1

            # Adjust the window size if it exceeds k
            if end - start >= k:
                start_element = arr[start]
                frequency[start_element] -= 1

                # Erase the current element from the window if its
                # frequency becomes 0
                if frequency[start_element] == 0:
                    del frequency[start_element]
                start += 1

            # Check if there's a duplicate in the window
            if frequency[end_element] > 1:
                return True

            # Move the end pointer to expand the window
            end += 1

        return False


# Examples from the problem statement
print(Solution().duplicate_detection([2, 1, 2, 3, 2, 1, 4, 5], 5))  # True
print(Solution().duplicate_detection([1, 1, 2, 4], 3))               # True
print(Solution().duplicate_detection([1, 2, 3, 4], 2))               # False

# Edge cases
print(Solution().duplicate_detection([], 3))                          # False
print(Solution().duplicate_detection([1], 1))                         # False
print(Solution().duplicate_detection([1, 2], 1))                      # False
print(Solution().duplicate_detection([1, 1], 2))                      # True
print(Solution().duplicate_detection([1, 2, 1], 5))                   # True
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public boolean duplicateDetection(int[] arr, int k) {

            // Map to store elements within the window and their counts
            Map<Integer, Integer> frequency = new HashMap<>();

            // The start and end pointers for the window
            int start = 0;
            int end = 0;

            while (end < arr.length) {

                // Add the current element to the window
                int endElement = arr[end];
                frequency.put(
                    endElement,
                    frequency.getOrDefault(endElement, 0) + 1
                );

                // Adjust the window size if it exceeds k
                if (end - start >= k) {
                    int startElement = arr[start];
                    frequency.put(
                        startElement,
                        frequency.get(startElement) - 1
                    );

                    // Erase the current element from the window if its
                    // frequency becomes 0
                    if (frequency.get(startElement) == 0) {
                        frequency.remove(startElement);
                    }
                    start++;
                }

                // Check if there's a duplicate in the window
                if (frequency.get(endElement) > 1) {
                    return true;
                }

                // Move the end pointer to expand the window
                end++;
            }

            return false;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().duplicateDetection(new int[]{2, 1, 2, 3, 2, 1, 4, 5}, 5)); // true
        System.out.println(new Solution().duplicateDetection(new int[]{1, 1, 2, 4}, 3));              // true
        System.out.println(new Solution().duplicateDetection(new int[]{1, 2, 3, 4}, 2));              // false

        // Edge cases
        System.out.println(new Solution().duplicateDetection(new int[]{}, 3));                        // false
        System.out.println(new Solution().duplicateDetection(new int[]{1}, 1));                       // false
        System.out.println(new Solution().duplicateDetection(new int[]{1, 2}, 1));                    // false
        System.out.println(new Solution().duplicateDetection(new int[]{1, 1}, 2));                    // true
        System.out.println(new Solution().duplicateDetection(new int[]{1, 2, 1}, 5));                 // true
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `arr = [2, 1, 2, 3, 2, 1, 4, 5]`, `k = 5`. The duplicate surfaces before the window even fills:

```
start=0, end=0, frequency={}

end=0  add 2 → freq={2:1}        end-start=0 < k → freq[2]=1, not > 1 → continue
end=1  add 1 → freq={2:1, 1:1}   end-start=1 < k → freq[1]=1, not > 1 → continue
end=2  add 2 → freq={2:2, 1:1}   end-start=2 < k → freq[2]=2 > 1 → return true

result = true
```

The result `true` matches the expected output — the partial window `[2, 1, 2]` already repeats `2`, so the full size-`5` window covering it does too.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `end` sweeps the array once; each step is one map insert, at most one delete, and one lookup — all amortised `O(1)`. |
| Space | **O(k)** | The map holds at most `k` distinct values — the elements currently inside the window. |

The early return on the first duplicate can only make a run faster; the worst case (no duplicate anywhere) still touches each element once.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty array | `arr = [], k = 3` | `false` | No elements, so no window and no duplicate. |
| Single element | `arr = [1], k = 1` | `false` | A size-`1` window holds one value — never a duplicate. |
| `k == 1` | `arr = [1, 2], k = 1` | `false` | Every window is one element, so no count can exceed `1`. |
| Whole-array duplicate | `arr = [1, 1], k = 2` | `true` | The single size-`2` window `[1, 1]` repeats `1`. |
| `k > n` | `arr = [1, 2, 1], k = 5` | `true` | `k` exceeds the array; the only window is `[1, 2, 1]`, which repeats `1`. |
| No duplicate | `arr = [1, 2, 3, 4], k = 2` | `false` | Every size-`2` window holds two distinct values. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the boolean-detection shape of the fixed window: maintain a frequency map and short-circuit to `true` the instant any count exceeds `1`. The duplicate is found inside the smallest window that contains it, so no full-size window scan is needed.

</details>