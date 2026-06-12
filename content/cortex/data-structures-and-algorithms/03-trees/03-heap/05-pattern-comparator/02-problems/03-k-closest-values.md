---
title: "K Closest Values"
summary: "Given an array of integers and a target value, return the K values closest to target. Use a max-heap of size K ordered by distance; tie-break by value ascending for determinism."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: medium
kind: problem
topics: [comparator, heap]
---

# K closest values

## Problem Statement

Given an array of integers `vals`, a **target** value (real number), and a non-negative integer `k`, return the K values in `vals` closest to `target`. If multiple values are equidistant from `target`, prefer the smaller value. Return the result sorted in ascending order.

## Examples

**Example 1:**
```
Input:  vals = [1, 2, 4, 6, 7], target = 4.63, k = 3
Output: [4, 6, 7]
```

**Example 2:**
```
Input:  vals = [1, 3, 4, 7], target = 7.49, k = 2
Output: [4, 7]
```

**Example 3:**
```
Input:  vals = [4, 2, 6, 1, 7], target = 4.0, k = 2
Output: [2, 4]
```
(2 and 4 are equidistant at dist=2 vs dist=0; closest are 4 at dist=0 and 2 at dist=2, beating 6 at dist=2 by the tie-break: smaller value wins.)

## Constraints

- `1 ≤ vals.length ≤ 10⁴`
- `-10⁴ ≤ vals[i] ≤ 10⁴`
- `0 ≤ target ≤ 10⁹`
- `1 ≤ k ≤ vals.length`
- All values in `vals` are distinct

```python run
import ast
import heapq

class Solution:
    def k_closest_values(self, vals, target, k):
        # Your code goes here — use a max-heap of size K ordered by distance
        # from target (negate distance for max-heap). Tie-break by value
        # descending (so smaller value is evicted last). Return sorted result.
        return []

vals = ast.literal_eval(input())
target = float(input())
k = int(input())
print(Solution().k_closest_values(vals, target, k))
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
    public List<Integer> kClosestValues(int[] vals, double target, int k) {
      // Your code goes here — use a max-heap of size K ordered by distance
      // from target. Tie-break by value descending (evict larger value first).
      // Return sorted result.
      return new ArrayList<>();
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] vals = parseIntArray(sc.nextLine());
    double target = Double.parseDouble(sc.nextLine().trim());
    int k = Integer.parseInt(sc.nextLine().trim());
    List<Integer> result = new Solution().kClosestValues(vals, target, k);
    Collections.sort(result);
    System.out.println(result);
  }
}
```

```testcases
{
  "args": [
    { "id": "vals", "label": "vals", "type": "int[]", "placeholder": "[1, 2, 4, 6, 7]" },
    { "id": "target", "label": "target", "type": "float", "placeholder": "4.63" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "vals": "[1, 2, 4, 6, 7]", "target": "4.63", "k": "3" }, "expected": "[4, 6, 7]" },
    { "args": { "vals": "[1, 3, 4, 7]", "target": "7.49", "k": "2" }, "expected": "[4, 7]" },
    { "args": { "vals": "[4, 2, 6, 1, 7]", "target": "4.0", "k": "2" }, "expected": "[2, 4]" },
    { "args": { "vals": "[5]", "target": "3.0", "k": "1" }, "expected": "[5]" },
    { "args": { "vals": "[4, 2, 6, 1, 7]", "target": "1.0", "k": "1" }, "expected": "[1]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

This is **Top-K-smallest by distance**, applied to an array. We walk the array, pushing each value paired with its absolute distance to the target. We use a **max-heap** of size K, where the top is the *farthest* of our current best K — the threshold we evict against.

The comparator: "compare by distance, descending" (so the farthest is on top of the max-heap). Tie-break by value descending — when two values are equidistant, the larger one goes to the top and gets evicted first, keeping the smaller value in the heap.

</details>
<details>
<summary><h2>Solution</h2></summary>

A max-heap of size K stores `(-distance, -value)` tuples (Python's heapq is a min-heap, so negate both to get max-heap behavior). For equal distances, the larger value is evicted first (tie-break: prefer smaller). The final result is sorted ascending.

```python solution time=O(n log K) space=O(K)
import ast
import heapq

class Solution:
    def k_closest_values(self, vals, target, k):
        # max-heap via negation: (-dist, -val) so farthest/largest evicted first
        heap = []
        for v in vals:
            dist = abs(v - target)
            heapq.heappush(heap, (-dist, -v))
            if len(heap) > k:
                heapq.heappop(heap)
        result = sorted(-v for d, v in heap)
        return result

vals = ast.literal_eval(input())
target = float(input())
k = int(input())
print(Solution().k_closest_values(vals, target, k))
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
    public List<Integer> kClosestValues(int[] vals, double target, int k) {
      // max-heap: farthest distance on top; for equal distance, larger value on top
      PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> {
        int cmp = Double.compare(Math.abs(b[0] - target), Math.abs(a[0] - target));
        return cmp != 0 ? cmp : b[0] - a[0];  // tie-break: larger value on top (evict it)
      });
      for (int v : vals) {
        heap.offer(new int[]{v});
        if (heap.size() > k) heap.poll();
      }
      List<Integer> result = new ArrayList<>();
      while (!heap.isEmpty()) result.add(heap.poll()[0]);
      Collections.sort(result);
      return result;
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[] vals = parseIntArray(sc.nextLine());
    double target = Double.parseDouble(sc.nextLine().trim());
    int k = Integer.parseInt(sc.nextLine().trim());
    List<Integer> result = new Solution().kClosestValues(vals, target, k);
    Collections.sort(result);
    System.out.println(result);
  }
}
```

</details>
