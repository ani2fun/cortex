---
title: "Subarray Sum Equals K"
summary: "Given an integer array arr and target k, return the maximum length of a subarray whose elements sum to k. Return 0 if no such subarray exists."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
---

# Subarray sum equals k

## Problem Statement

Given an integer array `arr` and target `k`, return the maximum length of a subarray whose elements sum to `k`. Return `0` if no such subarray exists.

### Example 1
> -   **Input:** `arr = [4, 4, 2, 6, 4], k = 10` → **Output:** `3` (`[4, 4, 2]`)

### Example 2
> -   **Input:** `arr = [2, 2, 1, 2, 4, 3], k = 7` → **Output:** `4` (`[2, 2, 1, 2]`)

### Example 3
> -   **Input:** `arr = [2, 3, 1, 2, 4, 3], k = 100` → **Output:** `0`

## Examples

**Example 1**
```
Input:  arr = [4, 4, 2, 6, 4], k = 10
Output: 3
Explanation: [4, 4, 2] (indices 0..2) sums to 10 → length 3. No longer subarray
sums to exactly 10.
```

**Example 2**
```
Input:  arr = [2, 2, 1, 2, 4, 3], k = 7
Output: 4
Explanation: [2, 2, 1, 2] (indices 0..3) sums to 7 → length 4, the longest such run.
```

**Example 3**
```
Input:  arr = [2, 3, 1, 2, 4, 3], k = 100
Output: 0
Explanation: no subarray reaches a sum of 100, so the answer is 0.
```

**Example 4**
```
Input:  arr = [1, -1, 1], k = 1
Output: 3
Explanation: the whole array sums to 1 → length 3. The negative element is why a
plain window fails and prefix sums are needed.
```

<details>
<summary><h2>Approach</h2></summary>


> *A small detour from sliding windows* — when the array can contain negatives, the window-shrinking-on-violation trick fails (extending might *decrease* the sum, and shrinking might *increase* it; the rule isn't monotonic). The right tool here is a **prefix-sum + hash map**, which the next lesson covers in full. We touch on it here as a preview.

The trick: for each prefix sum `P[i]`, we want to find an earlier index `j` with `P[j] = P[i] − k` — because then the subarray `arr[j+1..i]` sums to exactly `k`. Maintain a hash map `sum_index_map` from each prefix sum to the earliest index at which it occurred; for each new prefix sum, look up `sum − k` and compute the length.

This is technically a hash-table technique, not a sliding window, but the original course groups it here.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array
from typing import List
from collections import defaultdict

class Solution:
    def subarray_sum_equals_k(self, arr: List[int], k: int) -> int:

        # Create a map to store the sum of elements up to each index
        sum_index_map = defaultdict(int)

        # Initialize the sum to zero and the maximum length to zero
        sum = 0
        max_len = 0

        # Initialize start and end to 0
        start = 0
        end = 0

        # Move the window one step to the right until it reaches the end
        # of the array
        while end < len(arr):

            # Add contribution of arr[end]
            sum += arr[end]

            # Check if the current sum equals the target value k
            if sum == k:

                # Update the maximum length
                max_len = end + 1

            # Check if sum - k exists in the map
            if sum - k in sum_index_map:

                # Update the maximum length if the current length is
                # greater
                max_len = max(max_len, end - sum_index_map[sum - k])

            # Store the current sum with the current index if not already
            # present
            if sum not in sum_index_map:
                sum_index_map[sum] = end

            # Move the end index
            end += 1

        # Return the maximum length
        return max_len


# Examples from the problem statement
print(Solution().subarray_sum_equals_k([4, 4, 2, 6, 4], 10))      # 3
print(Solution().subarray_sum_equals_k([2, 2, 1, 2, 4, 3], 7))    # 4
print(Solution().subarray_sum_equals_k([2, 3, 1, 2, 4, 3], 100))  # 0

# Edge cases
print(Solution().subarray_sum_equals_k([], 0))                     # 0
print(Solution().subarray_sum_equals_k([1], 1))                    # 1
print(Solution().subarray_sum_equals_k([1, 2, 3], 6))              # 3
print(Solution().subarray_sum_equals_k([1, -1, 1], 1))             # 3
print(Solution().subarray_sum_equals_k([1, 2, 3], 0))              # 0
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int subarraySumEqualsK(int[] arr, int k) {

            // Create a map to store the sum of elements up to each index
            HashMap<Integer, Integer> sumIndexMap = new HashMap<>();

            // Initialize the sum to zero and the maximum length to zero
            int sum = 0;
            int maxLen = 0;

            // Initialize start and end to 0
            int start = 0;
            int end = 0;

            // Move the window one step to the right until it reaches the end
            // of the array
            while (end < arr.length) {

                // Add contribution of arr[end]
                sum += arr[end];

                // Check if the current sum equals the target value k
                if (sum == k) {

                    // Update the maximum length
                    maxLen = end + 1;
                }

                // Check if sum - k exists in the map
                if (sumIndexMap.containsKey(sum - k)) {

                    // Update the maximum length if the current length is
                    // greater
                    maxLen = Math.max(
                        maxLen,
                        end - sumIndexMap.get(sum - k)
                    );
                }

                // Store the current sum with the current index if not
                // already present
                if (!sumIndexMap.containsKey(sum)) {
                    sumIndexMap.put(sum, end);
                }

                // Move the end index
                end++;
            }

            // Return the maximum length
            return maxLen;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().subarraySumEqualsK(new int[]{4, 4, 2, 6, 4}, 10));      // 3
        System.out.println(new Solution().subarraySumEqualsK(new int[]{2, 2, 1, 2, 4, 3}, 7));    // 4
        System.out.println(new Solution().subarraySumEqualsK(new int[]{2, 3, 1, 2, 4, 3}, 100));  // 0

        // Edge cases
        System.out.println(new Solution().subarraySumEqualsK(new int[]{}, 0));                    // 0
        System.out.println(new Solution().subarraySumEqualsK(new int[]{1}, 1));                   // 1
        System.out.println(new Solution().subarraySumEqualsK(new int[]{1, 2, 3}, 6));             // 3
        System.out.println(new Solution().subarraySumEqualsK(new int[]{1, -1, 1}, 1));            // 3
        System.out.println(new Solution().subarraySumEqualsK(new int[]{1, 2, 3}, 0));             // 0
    }
}
```


> *Spoiler* — this is the prefix-sum pattern, the topic of the next lesson. Read it as a preview; the full treatment is one click away.

</details>
<details>
<summary><h2>Intuition</h2></summary>


This problem *looks* like a variable-sized sliding window — a longest contiguous subarray with a sum condition — but it hides a trap. The window trick relies on a **monotonic** rule: extending must move the sum one way, contracting the other. When `arr` can contain negative numbers, that guarantee dies. Extending the window might *decrease* the sum, and contracting from the left might *increase* it, so when the sum overshoots `k` there is no safe direction to shrink. The plain window cannot decide whether to expand or contract.

The escape hatch is **prefix sums plus a hash map**. Let `P[i]` be the sum of `arr[0..i]`. A subarray `arr[j+1..i]` sums to `k` exactly when `P[i] − P[j] = k`, which rearranges to `P[j] = P[i] − k`. So as you sweep `i` forward, keep a map from each prefix sum to the *earliest* index where it occurred. At each `i`, look up `P[i] − k`: if it exists at some earlier index `j`, then `arr[j+1..i]` is a valid subarray of length `i − j`. Storing only the earliest index for each prefix sum maximises that length.

This is technically a hash-table technique, not a sliding window — the original course groups it here as a bridge to the next lesson. The takeaway is diagnostic: when a "subarray sum" problem allows negatives or asks for an *exact* sum, reach for prefix sums, not a window. The full treatment is the prefix-sum pattern, one lesson away.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Subarray Sum Equals K |
|---|---|
| **Q1.** Is the answer the longest/shortest/count of a contiguous subsequence? | **Yes** — the longest contiguous subarray summing to exactly `k`. |
| **Q2.** Can a hash map summarise the window for an `O(1)` rule check? | **Partly** — a prefix-sum-to-index map answers the rule in `O(1)`, but it indexes *prefixes*, not the live window's contents. |
| **Q3.** Can you add `arr[end]` and remove `arr[start]` in `O(1)`? | **No** — with negatives there is no safe way to decide *which* end to shrink, so contraction is undefined. |
| **Q4.** Is the rule monotonic as the window grows? | **No** — a negative element lets extending lower the sum and contracting raise it. This is the disqualifier; use prefix sums instead. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `sum = 0`, `maxLen = 0`, and an empty map `sum_index_map` from prefix sum to earliest index.
2. Advance `end` across the array, adding `arr[end]` to the running prefix sum `sum`.
3. If `sum == k`, the whole prefix `arr[0..end]` works — set `maxLen = end + 1`.
4. Look up `sum − k` in the map. If present at index `j`, the subarray `arr[j+1..end]` sums to `k` — update `maxLen = max(maxLen, end − j)`.
5. If `sum` is not already a key, store `sum_index_map[sum] = end`. Recording only the *first* occurrence keeps the matched subarray as long as possible.
6. After the loop, return `maxLen`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `arr = [4, 4, 2, 6, 4]`, `k = 10`, expected output `3`. `sum` is the running prefix sum; the map stores each prefix sum's earliest index:

```
end=0  arr=4   sum=4    ==10? no   sum−k=−6 in map? no   store {4:0}
end=1  arr=4   sum=8    ==10? no   sum−k=−2 in map? no   store {4:0, 8:1}
end=2  arr=2   sum=10   ==10? yes → maxLen=end+1=3       sum−k=0 in map? no   store {…, 10:2}
end=3  arr=6   sum=16   ==10? no   sum−k=6 in map? no    store {…, 16:3}
end=4  arr=4   sum=20   ==10? no   sum−k=10 in map? yes (index 2)
                                    → maxLen=max(3, 4−2)=max(3, 2)=3
                                    store {…, 20:4}

return maxLen = 3
```

The result `3` matches the expected output — `[4, 4, 2]` (indices `0..2`) sums to `10`.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | One pass over the array; each step does a constant number of `O(1)` hash-map reads and one write. |
| **Space** | **O(N)** | The map can hold one entry per distinct prefix sum — up to `N` entries when every prefix sum is unique. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `arr = [], k = 0` | `0` | Empty array — the loop never runs. |
| `arr = [1], k = 1` | `1` | The single-element prefix equals `k` → length `1`. |
| `arr = [1, 2, 3], k = 6` | `3` | The whole array sums to `6` — the `sum == k` branch fires at the last index. |
| `arr = [1, -1, 1], k = 1` | `3` | Negatives present — prefix sums recover the full-array match a window would miss. |
| `arr = [2, 3, 1, 2, 4, 3], k = 100` | `0` | No subarray reaches the target; `maxLen` stays `0`. |
| `arr = [1, 2, 3], k = 0` | `0` | All-positive with target `0` — no non-empty subarray sums to `0`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


When a "subarray sum" rule is non-monotonic — negatives present, or an *exact* target — the sliding window fails because there is no safe direction to contract. Prefix sums plus a hash map of earliest indices restore an `O(N)` single pass.

</details>