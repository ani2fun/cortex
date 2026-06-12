---
title: "K Most Frequent Elements"
summary: "Given an array arr and a positive integer k, return the K most frequent elements. Use a size-K min-heap ordered by frequency; sort the result by descending frequency then ascending value for determinism."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: medium
kind: problem
topics: [comparator, heap]
---

# K most frequent elements

## Problem Statement

Given an array `arr` and a positive integer `k`, return the K most frequent elements. Use a heap. If multiple elements have the same frequency, return them in ascending order of value.

## Examples

**Example 1:**
```
Input:  arr = [1, 2, 2, 3, 3, 3], k = 2
Output: [3, 2]
```

**Example 2:**
```
Input:  arr = [1, 5, 6, 6], k = 1
Output: [6]
```

**Example 3:**
```
Input:  arr = [1], k = 1
Output: [1]
```

## Constraints

- `1 ≤ arr.length ≤ 10⁵`
- `-10⁴ ≤ arr[i] ≤ 10⁴`
- `1 ≤ k ≤ number of unique elements`
- The answer is **unique** when there are no frequency ties among the top-K boundary.

```python run
import ast
import heapq

class Solution:
    def k_most_frequent_elements(self, arr, k):
        # Your code goes here — count frequencies, then maintain a size-K
        # min-heap ordered by (frequency, value). Return result sorted by
        # descending frequency then ascending value.
        return []

arr = ast.literal_eval(input())
k = int(input())
print(Solution().k_most_frequent_elements(arr, k))
```

```java run
import java.util.*;

public class Main {
  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
    return out;
  }

  static class Solution {
    public List<Integer> kMostFrequentElements(int[] arr, int k) {
      // Your code goes here — count frequencies, then maintain a size-K
      // min-heap ordered by frequency (ascending), value (ascending) for
      // tie-breaking. Return result sorted by descending frequency then
      // ascending value.
      return new ArrayList<>();
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(new Solution().kMostFrequentElements(arr, k));
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 2, 3, 3, 3]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 2, 3, 3, 3]", "k": "2" }, "expected": "[3, 2]" },
    { "args": { "arr": "[1, 5, 6, 6]", "k": "1" }, "expected": "[6]" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "[1]" },
    { "args": { "arr": "[7, 7, 7]", "k": "1" }, "expected": "[7]" },
    { "args": { "arr": "[1, 1, 2, 2]", "k": "2" }, "expected": "[1, 2]" },
    { "args": { "arr": "[4, 4, 4, 4, 4]", "k": "1" }, "expected": "[4]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

Two steps:

1. Count frequencies into a hash map (`O(N)` time, `O(U)` space where `U` is the number of unique values).
2. Run Top-K-largest *over the hash map's entries*, comparing by frequency. Use a min-heap of size K.

The comparator is "compare by frequency, ascending" (for a min-heap of size K → top is the smallest frequency, which is exactly the threshold we evict against). Tie-break by value ascending so results are deterministic across Python and Java.

</details>
<details>
<summary><h2>Solution</h2></summary>

A size-K min-heap ordered by `(frequency, value)`. When the heap overflows, the element with the lowest (frequency, value) is evicted. The final result is sorted by descending frequency then ascending value for a consistent order.

```python solution time=O(n log K) space=O(n)
import ast
import heapq
from collections import Counter

class Solution:
    def k_most_frequent_elements(self, arr, k):
        freq = Counter(arr)
        heap = []
        for val, f in freq.items():
            heapq.heappush(heap, (f, val))
            if len(heap) > k:
                heapq.heappop(heap)
        result = sorted((val for f, val in heap), key=lambda v: (-freq[v], v))
        return result

arr = ast.literal_eval(input())
k = int(input())
print(Solution().k_most_frequent_elements(arr, k))
```

```java solution
import java.util.*;

public class Main {
  static int[] parseIntArray(String line) {
    String inner = line.replaceAll("[\\[\\]\\s]", "");
    if (inner.isEmpty()) return new int[0];
    String[] parts = inner.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
    return out;
  }

  static class Solution {
    public List<Integer> kMostFrequentElements(int[] arr, int k) {
      Map<Integer, Integer> freq = new HashMap<>();
      for (int x : arr) freq.merge(x, 1, Integer::sum);
      // min-heap: lower freq first; for ties, lower value first
      PriorityQueue<Integer> heap = new PriorityQueue<>(
        (a, b) -> freq.get(a).equals(freq.get(b)) ? a - b : freq.get(a) - freq.get(b));
      for (int val : freq.keySet()) {
        heap.offer(val);
        if (heap.size() > k) heap.poll();
      }
      List<Integer> out = new ArrayList<>(heap);
      // sort by descending freq, then ascending value
      out.sort((a, b) -> freq.get(b).equals(freq.get(a)) ? a - b : freq.get(b) - freq.get(a));
      return out;
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] arr = parseIntArray(sc.nextLine());
    int k = Integer.parseInt(sc.nextLine().trim());
    System.out.println(new Solution().kMostFrequentElements(arr, k));
  }
}
```

</details>
