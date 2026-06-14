---
title: "Sets with Smallest Discrepancy"
summary: "Given an array of non-negative integers, partition it into two subsets S1 and S2 such that |sum(S1) − sum(S2)| is minimum. Return that minimum."
prereqs:
  - 17-pattern-subset-sum/01-pattern
difficulty: hard
kind: problem
topics: [subset-sum, dynamic-programming]
---

# Sets With Smallest Discrepancy

## The Problem

Given an array of non-negative integers, partition it into two subsets `S1` and `S2` such that `|sum(S1) − sum(S2)|` is minimum. Return that minimum.

```
Input:  arr = [1, 5, 3, 10]
Output: 1                          [1, 5, 3] (sum 9) and [10] (sum 10) → diff 1

Input:  arr = [1, 2, 3, 4, 5]
Output: 1                          [5, 3] (sum 8) and [1, 2, 4] (sum 7) → diff 1

Input:  arr = [1, 1]
Output: 0                          Both [1] subsets → diff 0
```

---

## Examples

**Example 1**
```
Input:  arr = [1, 5, 3, 10]
Output: 1
Explanation: [1, 5, 3] sums to 9 and [10] sums to 10; |9 − 10| = 1.
```

**Example 2**
```
Input:  arr = [1, 2, 3, 4, 5]
Output: 1
Explanation: [5, 3] sums to 8 and [1, 2, 4] sums to 7; |8 − 7| = 1.
```

**Example 3**
```
Input:  arr = [1, 1]
Output: 0
Explanation: Both subsets get one element summing to 1; |1 − 1| = 0.
```

## Constraints

- `1 ≤ arr.length ≤ 200`
- `0 ≤ arr[i] ≤ 100`

```python run viz=graph viz-root=dp
import ast
from typing import List

class Solution:
    def sets_with_smallest_discrepancy(self, arr: List[int]) -> int:
        # Your code goes here
        return 0

arr = ast.literal_eval(input())
print(Solution().sets_with_smallest_discrepancy(arr))
```

```java run viz=graph viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public int setsWithSmallestDiscrepancy(int[] arr) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().setsWithSmallestDiscrepancy(arr));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 5, 3, 10]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 5, 3, 10]" }, "expected": "1" },
    { "args": { "arr": "[1, 2, 3, 4, 5]" }, "expected": "1" },
    { "args": { "arr": "[1, 1]" }, "expected": "0" },
    { "args": { "arr": "[5, 5]" }, "expected": "0" },
    { "args": { "arr": "[1, 2, 3]" }, "expected": "0" },
    { "args": { "arr": "[10, 1, 1, 1]" }, "expected": "7" }
  ]
}
```

<details>
<summary><h2>The Reduction</h2></summary>


If one subset sums to `s`, the other sums to `total − s`. The difference is `total − 2s`. To minimise the difference (with `s ≤ total / 2` to keep it non-negative), maximise `s`.

So:
1. Build the same subset-sum table `dp[n][s]` for `s ∈ [0, total]` (but you only need up to `total / 2`).
2. Walk `s` downward from `total / 2` to `0`. The first `s` for which `dp[n][s] = true` is the largest reachable subset sum ≤ `total / 2`.
3. Answer = `total − 2s`.

```d2
direction: right
flow: "Discrepancy via subset sum" {
  grid-rows: 1
  grid-columns: 3
  grid-gap: 20
  step1: |md
    **1. Build dp**
    `dp[n][s]` for `s` up to `total / 2`
  |
  step2: |md
    **2. Scan downward**
    Find largest `s ≤ total / 2`
    with `dp[n][s] = true`
  |
  step3: |md
    **3. Answer**
    `total − 2 · s`
  |
}
```

<p align="center"><strong>Three-step reduction. The DP gives the achievable-sums oracle; the scan picks the best half-sum; arithmetic recovers the discrepancy.</strong></p>

> *Pause. Why scan downward from `total / 2`, not upward from 0?*

Because we want the *largest* feasible `s ≤ total / 2`. Scanning down hits it first; we break and return. Scanning up would force us to walk all the way to `total / 2` and remember the largest hit — same final answer, but uglier code.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n×total) space=O(n×total)
import ast
from typing import List
import sys

class Solution:
    def sets_with_smallest_discrepancy(self, arr: List[int]) -> int:
        n: int = len(arr)
        total_sum: int = sum(arr)

        # Initialize the dp array
        dp: List[List[bool]] = [
            [False] * (total_sum + 1) for _ in range(n + 1)
        ]

        # Base cases
        # If the sum is 0, we can always achieve it by not selecting any
        # element
        for i in range(n + 1):
            dp[i][0] = True

        # Fill the dp array
        for i in range(1, n + 1):
            for j in range(1, total_sum + 1):

                # If we can achieve the sum without including the current
                # element
                dp[i][j] = dp[i - 1][j]

                # If the current element is smaller than or equal to the
                # required sum, we can either include it or exclude it
                if arr[i - 1] <= j:
                    dp[i][j] = dp[i][j] or dp[i - 1][j - arr[i - 1]]

        min_diff: int = sys.maxsize

        # Find the minimum difference between two subsets
        # Start from total_sum/2 and go downwards to find the maximum
        # possible sum that can be achieved by one subset
        for j in range(total_sum // 2, -1, -1):
            if dp[n][j]:

                # If the current sum is achievable, calculate the
                # difference between the total sum and the sum of the
                # current subset
                min_diff = total_sum - 2 * j
                break

        return min_diff


arr = ast.literal_eval(input())
print(Solution().sets_with_smallest_discrepancy(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int setsWithSmallestDiscrepancy(int[] arr) {
            int n = arr.length;
            int totalSum = 0;

            // Calculating the total sum of all elements in the array
            for (int i = 0; i < n; i++) {
                totalSum += arr[i];
            }

            // Initialize the dp array
            boolean[][] dp = new boolean[n + 1][totalSum + 1];

            // Base cases
            // If the sum is 0, we can always achieve it by not selecting any
            // element
            for (int i = 0; i <= n; i++) {
                dp[i][0] = true;
            }

            // Fill the dp array
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= totalSum; j++) {

                    // If we can achieve the sum without including the
                    // current element
                    dp[i][j] = dp[i - 1][j];

                    // If the current element is smaller than or equal to the
                    // required sum, we can either include it or exclude it
                    if (arr[i - 1] <= j) {
                        dp[i][j] = dp[i][j] || dp[i - 1][j - arr[i - 1]];
                    }
                }
            }

            int minDiff = Integer.MAX_VALUE;

            // Find the minimum difference between two subsets
            // Start from totalSum/2 and go downwards to find the maximum
            // possible sum that can be achieved by one subset
            for (int j = totalSum / 2; j >= 0; j--) {
                if (dp[n][j]) {

                    // If the current sum is achievable, calculate the
                    // difference between the total sum and the sum of the
                    // current subset
                    minDiff = totalSum - 2 * j;
                    break;
                }
            }

            return minDiff;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().setsWithSmallestDiscrepancy(arr));
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

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n × total)` for the DP + `O(total)` for the scan |
| Space | `O(n × total)` |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `[7]` | `7` | Only subset split: `{7}` and `{}` → diff 7. |
| All zeros | `[0, 0, 0]` | `0` | Any partition is balanced. |
| Two equal | `[5, 5]` | `0` | Each subset gets one. |
| Large outlier | `[1, 1, 1, 100]` | `97` | Best is `{1, 1, 1}` (sum 3) and `{100}` (sum 100) → diff 97. |
| Already balanced | `[1, 2, 3, 4, 5, 5, 4, 3, 2, 1]` | `0` | Total 30, half 15; reachable. |

</details>
