---
title: "Unique Subsets"
summary: "Given an array of n distinct elements, return every possible subset (the power set). Order of subsets doesn't matter."
prereqs:
  - 05-pattern-bitmasking/01-pattern
difficulty: medium
kind: problem
topics: [bitmasking, bit-manipulation]
---

# Unique Subsets

Enumerate the entire power set by treating each integer `0..2^n−1` as a bitmask encoding one subset.

## Problem Statement

Given an array of `n` distinct elements, return every possible subset (the power set). Order of subsets doesn't matter.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 3]
Output: [[], [1], [2], [1, 2], [3], [1, 3], [2, 3], [1, 2, 3]]
Explanation: 8 subsets for 3 elements (2^3 = 8), listed in natural bitmask order.
```

**Example 2**
```
Input:  arr = [1]
Output: [[], [1]]
Explanation: 2 subsets for 1 element.
```

**Example 3**
```
Input:  arr = []
Output: [[]]
Explanation: The empty array has exactly one subset — the empty set.
```

## Constraints

- `0 ≤ n ≤ 20` — `n` is small; there are `2^n` subsets (feasibility ceiling for bitmask enumeration).
- All elements are distinct integers.
- Return subsets in natural bitmask order (mask `0` to `2^n - 1`); both Python and Java iterate identically so output is byte-exact.

```python run viz=array viz-root=arr
import ast

class Solution:
    def unique_subsets(self, arr):
        # Your code goes here
        return [[]]

arr = ast.literal_eval(input())
print(Solution().unique_subsets(arr))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
  static int[] parseIntArray(String s) {
    s = s.trim();
    if (s.equals("[]")) return new int[0];
    s = s.substring(1, s.length() - 1);
    String[] parts = s.split(",");
    int[] a = new int[parts.length];
    for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
    return a;
  }

  static class Solution {
    public List<List<Integer>> uniqueSubsets(int[] arr) {
      // Your code goes here
      List<List<Integer>> result = new ArrayList<>();
      result.add(new ArrayList<>());
      return result;
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine().trim());
    System.out.println(new Solution().uniqueSubsets(arr));
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "array", "placeholder": "[1, 2, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3]" }, "expected": "[[], [1], [2], [1, 2], [3], [1, 3], [2, 3], [1, 2, 3]]" },
    { "args": { "arr": "[1]" }, "expected": "[[], [1]]" },
    { "args": { "arr": "[]" }, "expected": "[[]]" },
    { "args": { "arr": "[1, 2]" }, "expected": "[[], [1], [2], [1, 2]]" }
  ]
}
```

<details>
<summary><h2>The Recipe — Bitmask Enumeration</h2></summary>


Every subset is one binary choice per element: *include* `arr[i]` or *don't*. Pack those `n` choices into the bits of an integer — bit `j` set ⇒ `arr[j]` is in — and a single `mask` value *is* a subset. There are `2^n` such masks, so a plain outer loop `mask = 0 .. 2^n - 1` walks every subset exactly once.

The recipe: for each `mask`, build its subset by scanning bit positions `j = 0 .. n - 1` and appending `arr[j]` whenever `(mask >> j) & 1` is set. Collect every subset into the output. No recursion, no backtracking — the loop counter does all the enumeration.

```
unique_subsets(arr):
    n = len(arr)
    power_set_size = 1 << n            # 2^n subsets
    for mask in 0 .. power_set_size - 1:
        subset = []
        for j in 0 .. n - 1:
            if (mask >> j) & 1:        # j-th bit set ⇒ include arr[j]
                subset.append(arr[j])
        output.append(subset)
```

The outer loop runs `2^n` times, the inner bit-scan `n` times, so total work `O(n × 2^n)` — and that's *optimal* for outputting every subset, since the total output size is `Σ(n choose k) × k = n × 2^(n-1)`.

```d2
direction: right
loop: "n = 3, mask runs 0 to 7" {
  grid-rows: 4
  grid-columns: 2
  grid-gap: 0
  m0: "mask = 000"
  s0: "{}"
  m1: "mask = 001"
  s1: "{a}"
  m2: "mask = 010"
  s2: "{b}"
  m3: "mask = 011"
  s3: "{a, b}"
}
```

<p align="center"><strong>The 8 subsets for <code>n = 3</code>, each tagged with the bitmask that encodes it. The outer loop visits masks <code>000</code> through <code>111</code> in order, landing on each subset exactly once.</strong></p>

> *Pause. Why does the loop produce every subset exactly once? Predict before reading on.*

Because the map from masks to subsets is a *bijection*: each `n`-bit integer encodes exactly one subset (bit `j` decides whether `arr[j]` is in), and every subset corresponds to exactly one integer (read off which elements it contains). The loop runs the counter `mask` over all `2^n` integers `0 .. 2^n - 1` with no repeats, so it visits each subset once and only once.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n × 2^n) space=O(n × 2^n)
import ast
from typing import List

class Solution:
    def unique_subsets(self, arr: List[int]) -> List[List[int]]:
        n: int = len(arr)

        # Total number of unique_subsets will be 2^n
        power_set_size: int = 1 << n

        # List to store all unique_subsets
        unique_subsets: List[List[int]] = [
            [] for _ in range(power_set_size)
        ]

        # Generate unique_subsets using bitmasking
        for i in range(power_set_size):
            subset: List[int] = []
            for j in range(n):

                # Check if j-th bit is set in i
                if (i >> j) & 1:

                    # Add arr[j] to the current subset
                    subset.append(arr[j])
            unique_subsets[i] = subset

        return unique_subsets


arr = ast.literal_eval(input())
print(Solution().unique_subsets(arr))
```

```java solution
import java.util.*;

public class Main {
  static int[] parseIntArray(String s) {
    s = s.trim();
    if (s.equals("[]")) return new int[0];
    s = s.substring(1, s.length() - 1);
    String[] parts = s.split(",");
    int[] a = new int[parts.length];
    for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
    return a;
  }

  static class Solution {
    public List<List<Integer>> uniqueSubsets(int[] arr) {
      int n = arr.length;

      // Total number of uniqueSubsets will be 2^n
      int powerSetSize = 1 << n;

      // List to store all uniqueSubsets
      List<List<Integer>> uniqueSubsets = new ArrayList<>(
          powerSetSize
      );

      // Generate uniqueSubsets using bitmasking
      for (int i = 0; i < powerSetSize; i++) {
        List<Integer> subset = new ArrayList<>();
        for (int j = 0; j < n; j++) {

          // Check if j-th bit is set in i
          if (((i >> j) & 1) == 1) {

            // Add arr[j] to the current subset
            subset.add(arr[j]);
          }
        }
        uniqueSubsets.add(subset);
      }

      return uniqueSubsets;
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine().trim());
    System.out.println(new Solution().uniqueSubsets(arr));
  }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n × 2^n)` — optimal: output size is `Θ(n × 2^n)` |
| Space | `O(n × 2^n)` for the output |

</details>
