---
title: "Maximum Subarray Sum"
summary: "Given an integer array arr, find the non-empty subarray with the maximum sum and return that sum."
prereqs:
  - 09-pattern-variable-sliding-window/01-pattern
difficulty: medium
---

# Maximum Subarray Sum

## Problem Statement

Given an integer array `arr`, find the **non-empty subarray with the maximum sum** and return that sum.

```
arr = [-2, 1, -3, 4, -1, 2, 1, -5, 4]   →  6        (subarray [4, -1, 2, 1])
arr = [5, 4, -1, 7, 8]                  →  23       (the whole array)
arr = [1]                                →  1
```

---

## Examples

**Example 1**
```
Input:  arr = [-2, 1, -3, 4, -1, 2, 1, -5, 4]
Output: 6
Explanation: The subarray [4, -1, 2, 1] has the largest sum of 6.
```

**Example 2**
```
Input:  arr = [5, 4, -1, 7, 8]
Output: 23
Explanation: The subarray [5, 4, -1, 7, 8] has the largest sum of 23.
```

**Example 3**
```
Input:  arr = [1]
Output: 1
Explanation: The subarray [1] has the largest sum of 1.
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is **sign-driven dominance**. Once the running sum from `start` to `end` turns negative, that prefix can only drag down any extension — a fresh start from `end + 1` would produce a strictly higher sum on the same suffix. The window invariant is therefore "the running sum from `start` to `end` is non-negative".

`end` walks forward; `start` only moves when the invariant breaks. The aggregate is `sum`, the running total of `arr[start..end]`. When `arr[end]` pushes `sum` below zero, we don't shrink one step at a time — we **leap** `start` past the dead region to `end + 1` and reset `sum` to `arr[end]`. A second variable `max_sum` records the best window sum we have seen so far. We seed both `sum` and `max_sum` with `arr[0]` (not `0`) so the algorithm still returns the largest single element when every element is negative.

The naive approach evaluates every subarray and tracks the largest sum — O(N²) subarrays, O(1) incremental sum per step, O(N²) total. The leap is what saves us: it discards a negative-prefix region in a single move, never re-examining any subarray rooted there. The correctness proof (below) shows that none of the discarded subarrays could have beaten what is yet to come.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Maximum Subarray Sum |
|---|---|
| **Single-result over subarrays?** | Yes — return one integer: the largest sum among all non-empty contiguous subarrays. |
| **O(1) add to the aggregate?** | Yes — `sum += arr[end]` is one operation. |
| **O(1) remove from the aggregate?** | Not needed in the gradual sense — the contraction is a single leap (`sum = arr[end]`, `start = end + 1`) that discards the entire prefix at once. |
| **Provable skipping?** | Yes — when `sum < 0`, every subarray starting in `[start, end]` and ending at or beyond `end` is provably dominated by one starting at `end + 1`. (Three-case proof below.) |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. If the array is empty, return `0`.
2. Initialize `start = 0`, `end = 0`. Seed `sum = arr[0]`, `max_sum = arr[0]`. Advance `end` to `1` because index `0` has already been folded into both.
3. Loop while `end < len(arr)`:
   1. If `sum < 0`: reset — set `sum = arr[end]` and `start = end + 1` (leaping the dead prefix).
   2. Otherwise: extend — `sum += arr[end]`.
   3. Update `max_sum = max(max_sum, sum)`.
   4. Advance `end` by 1.
4. Return `max_sum`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array viz-root=arr
from typing import List

class Solution:
    def max_subarray_sum(self, arr: List[int]) -> int:
        if not arr:
            return 0

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # Initialize sum to a default value (current sum)
        sum = arr[end]

        # To store the maximum subarray sum found so far
        max_sum = arr[end]

        # Increment to start from index 1 as index 0 has already been
        # taken into account
        end += 1

        # Sliding window
        while end < len(arr):

            # If the current sum becomes negative, reset the window
            if sum < 0:
                sum = arr[end]
                start = end + 1

            # Otherwise, add the contribution of arr[end]
            else:
                sum += arr[end]

            # Update the maximum subarray sum found so far
            max_sum = max(max_sum, sum)

            # Expand the window from the right
            end += 1

        return max_sum


# Examples from the problem statement
print(Solution().max_subarray_sum([-2, 1, -3, 4, -1, 2, 1, -5, 4]))   # 6
print(Solution().max_subarray_sum([5, 4, -1, 7, 8]))                   # 23
print(Solution().max_subarray_sum([1]))                                 # 1

# Edge cases
print(Solution().max_subarray_sum([-1]))                                # -1  — single negative
print(Solution().max_subarray_sum([2, -1]))                             # 2   — two elements
print(Solution().max_subarray_sum([-3, -1, -2]))                        # -1  — all negative
print(Solution().max_subarray_sum([0, 0, 0]))                           # 0   — all zeros
print(Solution().max_subarray_sum([1, 2, 3, 4, 5]))                     # 15  — all positive
```

```java run viz=array viz-root=arr
public class Main {
    static class Solution {
        public int maxSubarraySum(int[] arr) {
            if (arr.length == 0) {
                return 0;
            }

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // Initialize sum to a default value (current sum)
            int sum = arr[end];

            // To store the maximum subarray sum found so far
            int maxSum = arr[end];

            // Increment to start from index 1 as index 0 has already been
            // taken into account
            end++;

            // Sliding window
            while (end < arr.length) {

                // If the current sum becomes negative, reset the window
                if (sum < 0) {
                    sum = arr[end];
                    start = end + 1;
                }

                // Otherwise, add contribution of arr[end]
                else {
                    sum += arr[end];
                }

                // Update the maximum subarray sum found so far
                maxSum = Math.max(maxSum, sum);

                // Expand the window from the right
                end++;
            }

            return maxSum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().maxSubarraySum(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));   // 6
        System.out.println(new Solution().maxSubarraySum(new int[]{5, 4, -1, 7, 8}));                   // 23
        System.out.println(new Solution().maxSubarraySum(new int[]{1}));                                 // 1

        // Edge cases
        System.out.println(new Solution().maxSubarraySum(new int[]{-1}));                                // -1  — single negative
        System.out.println(new Solution().maxSubarraySum(new int[]{2, -1}));                             // 2   — two elements
        System.out.println(new Solution().maxSubarraySum(new int[]{-3, -1, -2}));                        // -1  — all negative
        System.out.println(new Solution().maxSubarraySum(new int[]{0, 0, 0}));                           // 0   — all zeros
        System.out.println(new Solution().maxSubarraySum(new int[]{1, 2, 3, 4, 5}));                     // 15  — all positive
    }
}
```


<details>
<summary><strong>Dry Run — arr = [-2, 1, -3, 4, -1, 2, 1, -5, 4]</strong></summary>

```
Initial: start=0, end=0, sum = arr[0] = -2, max_sum = -2.  Increment end to 1.

end=1: sum=-2 < 0 → reset.    sum = arr[1] = 1,  start = 2.  max_sum = max(-2, 1) = 1
end=2: sum=1  ≥ 0 → extend.   sum = 1 + (-3) = -2.           max_sum = max(1, -2) = 1
end=3: sum=-2 < 0 → reset.    sum = arr[3] = 4,  start = 4.  max_sum = max(1, 4) = 4
end=4: sum=4  ≥ 0 → extend.   sum = 4 + (-1) = 3.            max_sum = max(4, 3) = 4
end=5: sum=3  ≥ 0 → extend.   sum = 3 + 2 = 5.               max_sum = max(4, 5) = 5
end=6: sum=5  ≥ 0 → extend.   sum = 5 + 1 = 6.               max_sum = max(5, 6) = 6
end=7: sum=6  ≥ 0 → extend.   sum = 6 + (-5) = 1.            max_sum = max(6, 1) = 6
end=8: sum=1  ≥ 0 → extend.   sum = 1 + 4 = 5.               max_sum = max(6, 5) = 6

Return: 6 ✓   (the subarray [4, -1, 2, 1], indices 3..6)
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | Single pass; every element visited exactly once |
| **Space** | O(1) | Constant — `start`, `end`, `sum`, `maxSum` |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| All negative | `[-3, -1, -2]` | `-1` | Seed from `arr[0]` and record every iteration — the single largest element wins |
| Single element | `[5]` | `5` | Loop never runs; seed values are the answer |
| Entire array positive | `[5, 4, -1, 7, 8]` | `23` | Invariant never breaks; `sum` just keeps accumulating |
| Starts with huge negative | `[-100, 1, 2, 3]` | `6` | One reset at `end=1`, then pure accumulation |
| Zero prefix | `[0, 0, -1, 5]` | `5` | Zeros are non-negative — invariant holds until the `-1`; code still correct |
| Empty array | `[]` | `0` | Early-return guard |

</details>
<details>
<summary><h2>Proof of Correctness</h2></summary>

The leap looks aggressive — we throw away an entire prefix in one move. The proof below shows that none of the discarded subarrays could ever have been optimal.

Let `arr` be the input array, with the window `[start, end]` such that the invariant **`sum(start, i) ≥ 0` for all `i` in `[start, end)`** has held so far. Suppose that adding `arr[end]` pushes `sum(start, end)` strictly below zero. We claim three categories of subarrays can be safely discarded without missing the maximum.

### 1. Subarrays starting at `start` and ending beyond `end`

Consider a subarray `arr[start..b]` for some `b > end`. Decompose:

`sum(start, b) = sum(start, end) + sum(end+1, b)`

Since `sum(start, end) < 0`, this gives `sum(start, b) < sum(end+1, b)`. So `arr[start..b]` is strictly beaten by `arr[end+1..b]`, which the algorithm will evaluate later. **Discard `arr[start..b]`.**

### 2. Subarrays starting between `start+1` and `end` (inclusive) and ending beyond `end`

Consider `arr[a..b]` for some `a` in `(start, end]` and `b > end`. By the invariant, `sum(start, a-1) ≥ 0`. And we know `sum(start, end) < 0`. Therefore `sum(a, end)` must be negative (a non-negative prefix cannot be responsible for a negative total — the remainder must be).

Once `sum(a, end) < 0`, the same decomposition from Case 1 applies:

`sum(a, b) = sum(a, end) + sum(end+1, b) < sum(end+1, b)`

So `arr[a..b]` is strictly beaten by `arr[end+1..b]`. **Discard.**

### 3. Subarrays starting between `start+1` and `end` (inclusive) and ending at or before `end`

Consider `arr[a..b]` for some `a` in `(start, end]` and `b ≤ end`. By the invariant, `sum(start, a-1) ≥ 0`, so:

`sum(start, b) = sum(start, a-1) + sum(a, b) ≥ sum(a, b)`

Prepending a non-negative prefix never reduces the sum. So `arr[a..b]` is dominated by `arr[start..b]`, which the algorithm already evaluated in a previous iteration when `end` was at position `b`. **Discard.**

### Putting it together

The three cases together cover every subarray that touches the failed window from a starting index in `[start, end]`. Exactly two categories of subarrays remain in play:

1. `arr[start..i]` for `i < end` — these were already evaluated as `end` advanced, and `max_sum` recorded their best.
2. `arr[j..b]` for `j > end` — these will be evaluated in future iterations, with the window reset cleanly to `start = end + 1`.

So the key idea is: leaping `start` to `end + 1` after a negative prefix discards only provably-dominated subarrays, and the maximum is guaranteed to be either already in `max_sum` or in a future window.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Maximum Subarray Sum is Kadane's algorithm in variable-sliding-window form — when the prefix sum turns negative, every continuation is provably dominated by a fresh start, so `start` leaps to `end + 1` instead of shrinking step by step. Seed both `sum` and `max_sum` with `arr[0]` (not `0`) so all-negative arrays still return the single largest element.

</details>
