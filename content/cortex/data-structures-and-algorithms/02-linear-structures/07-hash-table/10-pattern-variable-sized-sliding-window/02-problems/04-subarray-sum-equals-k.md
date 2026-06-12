---
title: "Subarray Sum Equals K"
summary: "Given an integer array arr and target k, return the maximum length of a subarray whose elements sum to k. Return 0 if no such subarray exists."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [variable-sized-sliding-window, hash-table]
---

# Subarray sum equals k

## Problem Statement

Given an integer array `arr` and target `k`, return the maximum length of a subarray whose elements sum to `k`. Return `0` if no such subarray exists.

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

## Constraints

- `1 ≤ arr.length ≤ 10⁵`
- `-10⁴ ≤ arr[i] ≤ 10⁴`
- `-10⁹ ≤ k ≤ 10⁹`

```python run
import ast

arr = ast.literal_eval(input())
k = int(input())

class Solution:
    def subarray_sum_equals_k(self, arr, k):
        # Your code goes here
        return 0

print(Solution().subarray_sum_equals_k(arr, k))
```

```java run
import java.util.*;

public class Main {
    static int[] parseIntArray(String s) {
        s = s.trim().replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    static class Solution {
        public int subarraySumEqualsK(int[] arr, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().subarraySumEqualsK(arr, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "array", "placeholder": "[4, 4, 2, 6, 4]" },
    { "id": "k", "label": "k", "type": "number", "placeholder": "10" }
  ],
  "cases": [
    { "args": { "arr": "[4, 4, 2, 6, 4]", "k": "10" }, "expected": "3" },
    { "args": { "arr": "[2, 2, 1, 2, 4, 3]", "k": "7" }, "expected": "4" },
    { "args": { "arr": "[2, 3, 1, 2, 4, 3]", "k": "100" }, "expected": "0" },
    { "args": { "arr": "[1, -1, 1]", "k": "1" }, "expected": "3" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "1" },
    { "args": { "arr": "[1, 2, 3]", "k": "6" }, "expected": "3" },
    { "args": { "arr": "[1, 2, 3]", "k": "0" }, "expected": "0" }
  ]
}
```

<details>
<summary>Editorial</summary>

Prefix-sum + hash map: at each `end`, check if `sum == k` (whole prefix) and if `sum − k` is in the map (subarray `[j+1..end]`). Store only the *earliest* index for each prefix sum to maximise length. `O(n)` time, `O(n)` space.

```python solution time=O(n) space=O(n)
import ast

class Solution:
    def subarray_sum_equals_k(self, arr, k):
        sum_index_map = {}
        total = 0
        max_len = 0
        end = 0
        while end < len(arr):
            total += arr[end]
            if total == k:
                max_len = end + 1
            if total - k in sum_index_map:
                max_len = max(max_len, end - sum_index_map[total - k])
            if total not in sum_index_map:
                sum_index_map[total] = end
            end += 1
        return max_len

arr = ast.literal_eval(input())
k = int(input())
print(Solution().subarray_sum_equals_k(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static int[] parseIntArray(String s) {
        s = s.trim().replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    static class Solution {
        public int subarraySumEqualsK(int[] arr, int k) {
            HashMap<Integer, Integer> sumIndexMap = new HashMap<>();
            int sum = 0, maxLen = 0, end = 0;
            while (end < arr.length) {
                sum += arr[end];
                if (sum == k) maxLen = end + 1;
                if (sumIndexMap.containsKey(sum - k))
                    maxLen = Math.max(maxLen, end - sumIndexMap.get(sum - k));
                if (!sumIndexMap.containsKey(sum))
                    sumIndexMap.put(sum, end);
                end++;
            }
            return maxLen;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().subarraySumEqualsK(arr, k));
    }
}
```

### Dry Run

Walk Example 1: `arr = [4, 4, 2, 6, 4]`, `k = 10`, expected output `3`. `sum` is the running prefix sum:

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

### Complexity Analysis

| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | One pass over the array; each step does a constant number of `O(1)` hash-map reads and one write. |
| **Space** | **O(N)** | The map can hold one entry per distinct prefix sum — up to `N` entries when every prefix sum is unique. |

### Edge Cases

| Input | Output | Why |
|---|---|---|
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
