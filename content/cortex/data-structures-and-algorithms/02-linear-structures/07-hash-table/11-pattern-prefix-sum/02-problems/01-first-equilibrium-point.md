---
title: "First Equilibrium Point"
summary: "Given an array arr, return the first index i such that sum(arr[0..i-1]) == sum(arr[i+1..n-1]). Return -1 if no such index exists."
prereqs:
  - 11-pattern-prefix-sum/01-pattern
difficulty: easy
---

# First equilibrium point

## Problem Statement

Given an array `arr`, return the first index `i` such that `sum(arr[0..i-1]) == sum(arr[i+1..n-1])`. Return `-1` if no such index exists.

### Example 1
> -   **Input:** `arr = [1, 3, 5, 2, 2]` → **Output:** `2` (sum left = sum right = 4)

### Example 2
> -   **Input:** `arr = [5, 5, 5, 5, 5]` → **Output:** `2` (sum left = sum right = 10)

### Example 3
> -   **Input:** `arr = [1, 3, 5, 10]` → **Output:** `-1`

## Examples

**Example 1**
```
Input:  arr = [1, 3, 5, 2, 2]
Output: 2
Explanation: at index 2, the left side [1, 3] sums to 4 and the right side [2, 2]
sums to 4. Index 2 itself is excluded from both sides.
```

**Example 2**
```
Input:  arr = [5, 5, 5, 5, 5]
Output: 2
Explanation: at index 2, the left side [5, 5] and the right side [5, 5] each sum
to 10. It is the only equilibrium point — the centre of a symmetric array.
```

**Example 3**
```
Input:  arr = [1, 3, 5, 10]
Output: -1
Explanation: no split point balances the two sides, so the function returns -1.
```

**Example 4**
```
Input:  arr = [1]
Output: 0
Explanation: a single element has empty sides — both sum to 0 — so index 0 is an
equilibrium point.
```

<details>
<summary><h2>Approach</h2></summary>


Build a `prefix_sum` array where `prefix_sum[i]` holds the sum of the first `i` elements (so `prefix_sum[0] = 0`). With it, any range sum is a constant-time subtraction. For candidate index `i − 1`, the elements strictly to the left sum to `prefix_sum[i] − arr[i - 1]`, and the elements strictly to the right sum to `prefix_sum[n] − prefix_sum[i]`. Walk the array and return the first index where those two equal. One warm-up pass to fill `prefix_sum`, one pass to scan — **O(N)**.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=prefix_sum
from typing import List

class Solution:
    def first_equilibrium_point(self, arr: List[int]) -> int:

        # calculate the prefix sum of the array
        prefix_sum = [0] * (len(arr) + 1)
        for i in range(1, len(arr) + 1):
            prefix_sum[i] = prefix_sum[i - 1] + arr[i - 1]

        # check for equilibrium point
        for i in range(1, len(arr) + 1):

            # calculate sum of elements before and after the current
            # index
            left_sum = prefix_sum[i] - arr[i - 1]
            right_sum = prefix_sum[len(arr)] - prefix_sum[i]

            # if both sums are equal, return the current index as
            # equilibrium point
            if left_sum == right_sum:
                return i - 1

        # no equilibrium point found
        return -1


# Examples from the problem statement
print(Solution().first_equilibrium_point([1, 3, 5, 2, 2]))  # 2
print(Solution().first_equilibrium_point([5, 5, 5, 5, 5]))  # 2
print(Solution().first_equilibrium_point([1, 3, 5, 10]))    # -1

# Edge cases
print(Solution().first_equilibrium_point([1]))               # 0
print(Solution().first_equilibrium_point([0, 0, 0]))         # 0
print(Solution().first_equilibrium_point([1, 2, 3]))         # -1
print(Solution().first_equilibrium_point([2, 1, 2]))         # 1
print(Solution().first_equilibrium_point([0]))               # 0
```

```java run viz=array viz-root=prefix_sum
public class Main {
    static class Solution {
        public int firstEquilibriumPoint(int[] arr) {

            // calculate the prefix sum of the array
            int[] prefixSum = new int[arr.length + 1];
            prefixSum[0] = 0;
            for (int i = 1; i <= arr.length; i++) {
                prefixSum[i] = prefixSum[i - 1] + arr[i - 1];
            }

            // check for equilibrium point
            for (int i = 1; i <= arr.length; i++) {

                // calculate sum of elements before and after the current
                // index
                int leftSum = prefixSum[i] - arr[i - 1];
                int rightSum = prefixSum[arr.length] - prefixSum[i];

                // if both sums are equal, return the current index as
                // equilibrium point
                if (leftSum == rightSum) {
                    return i - 1;
                }
            }

            // no equilibrium point found
            return -1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{1, 3, 5, 2, 2}));  // 2
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{5, 5, 5, 5, 5}));  // 2
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{1, 3, 5, 10}));    // -1

        // Edge cases
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{1}));               // 0
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{0, 0, 0}));         // 0
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{1, 2, 3}));         // -1
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{2, 1, 2}));         // 1
        System.out.println(new Solution().firstEquilibriumPoint(new int[]{0}));               // 0
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


An equilibrium point splits the array so the elements strictly to its left sum to the same value as the elements strictly to its right. The brute-force read of that definition is a double loop: for each candidate index, sum everything to its left and everything to its right, then compare. Each candidate costs an `O(N)` re-sum, and there are `N` candidates, so the work is `O(N²)` time — and most of it re-adds the same elements the previous candidate already summed.

The prefix sum collapses both side-sums into constant-time lookups. Build `prefix_sum` where `prefix_sum[i]` is the total of the first `i` elements, so `prefix_sum[0] = 0`. For a candidate at array index `i − 1`, the left side is `prefix_sum[i] − arr[i - 1]` (the prefix up to and including the candidate, minus the candidate itself), and the right side is `prefix_sum[n] − prefix_sum[i]` (the whole-array total minus everything up to the candidate). Both are subtractions, so each candidate check is `O(1)`.

This is the no-map flavour of prefix sums: a precomputed prefix *array*, not a hash map, because the query is positional rather than value-based. What breaks if you skip the prefix array is the quadratic re-summing — recomputing each side from scratch turns an `O(N)` scan back into `O(N²)`. The diagnostic signal is "compare a left-of-`i` total to a right-of-`i` total at every index", which is exactly what one prefix-sum pass answers in linear time.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for First Equilibrium Point |
|---|---|
| **Q1.** Does the answer reduce to a subarray sum? | **Yes** — each side of the split is a range sum, read as a difference of prefix sums. |
| **Q2.** Is the input a linear sequence walked once? | **Yes** — an integer array; one pass fills `prefix_sum`, a second scans for the split. |
| **Q3.** Is the matching slice found by a hash-map lookup? | **No** — the query is positional, so a prefix *array* suffices; no hash map of values is needed. |
| **Q4.** Does the rule survive negatives and zeros? | **Yes** — subtraction of prefix sums is correct regardless of sign, so negatives and zeros are fine. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Allocate `prefix_sum` of length `n + 1` and set `prefix_sum[0] = 0`.
2. Fill it left to right so `prefix_sum[i] = prefix_sum[i - 1] + arr[i - 1]` — the total of the first `i` elements.
3. Scan `i` from `1` to `n`. For each, compute `left_sum = prefix_sum[i] − arr[i - 1]` (elements strictly before the candidate) and `right_sum = prefix_sum[n] − prefix_sum[i]` (elements strictly after it).
4. If `left_sum == right_sum`, the candidate index `i − 1` is an equilibrium point — return it immediately, which guarantees the *first* one.
5. If the scan finishes with no match, return `-1`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `arr = [1, 3, 5, 2, 2]`, expected output `2`. First the prefix array, then the scan:

```
prefix_sum = [0, 1, 4, 9, 11, 13]   (prefix_sum[i] = sum of first i elements)
total = prefix_sum[5] = 13

i=1  candidate index 0  left = prefix_sum[1] − arr[0] = 1 − 1 = 0    right = 13 − 1  = 12   0 ≠ 12
i=2  candidate index 1  left = prefix_sum[2] − arr[1] = 4 − 3 = 1    right = 13 − 4  = 9    1 ≠ 9
i=3  candidate index 2  left = prefix_sum[3] − arr[2] = 9 − 5 = 4    right = 13 − 9  = 4    4 = 4 → return 2

result = 2
```

The result `2` matches the expected output — at index `2` the left side `[1, 3]` and the right side `[2, 2]` both sum to `4`.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | One pass builds `prefix_sum`, a second scans for the split; each step is `O(1)`. |
| **Space** | **O(N)** | The auxiliary `prefix_sum` array holds `N + 1` entries. Can be reduced to `O(1)` by tracking running left and right sums instead. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `arr = [1]` | `0` | A single element has empty sides — both sum to `0` — so index `0` balances. |
| `arr = [0]` | `0` | Same single-element logic; the lone value is excluded from both empty sides. |
| `arr = [0, 0, 0]` | `0` | At index `0` the left is empty (`0`) and the right `[0, 0]` sums to `0`; the first match wins. |
| `arr = [2, 1, 2]` | `1` | The centre splits `[2]` and `[2]`, each summing to `2`. |
| `arr = [1, 2, 3]` | `-1` | No split balances the two sides, so the scan returns `-1`. |
| `arr = [1, 3, 5, 10]` | `-1` | A strictly growing array has no equilibrium point. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


When a problem compares a left-of-index total to a right-of-index total, a precomputed prefix-sum array turns each `O(N)` side-sum into an `O(1)` subtraction, collapsing the whole scan from `O(N²)` to `O(N)`.

</details>