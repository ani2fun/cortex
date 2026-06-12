---
title: "K-Way List Merge"
summary: "Given k sorted arrays, merge them into one sorted array using a K-way min-heap that always yields the global minimum. O(n log k) time."
prereqs:
  - 05-pattern-comparator/01-pattern
difficulty: hard
kind: problem
topics: [comparator, heap]
---

# K-way list merge

## Problem Statement

Given an array of `k` sorted integer arrays, merge all of them into one sorted array and return it.

## Examples

**Example 1:**
```
Input:  lists = [[1, 4, 5], [1, 3, 4], [2, 6]]
Output: [1, 1, 2, 3, 4, 4, 5, 6]
```

**Example 2:**
```
Input:  lists = []
Output: []
```

**Example 3:**
```
Input:  lists = [[1, 2, 3]]
Output: [1, 2, 3]
```

## Constraints

- `0 ≤ lists.length ≤ 10⁴`
- `0 ≤ lists[i].length ≤ 500`
- `-10⁴ ≤ lists[i][j] ≤ 10⁴`
- Each `lists[i]` is sorted in ascending order
- The total number of elements across all lists does not exceed 10⁴

```python run
import ast
import heapq

class Solution:
    def k_way_list_merge(self, lists):
        # Your code goes here — push (value, list_idx, element_idx) for the
        # first element of each non-empty list. Repeatedly pop the minimum,
        # append to result, then push the next element from that list.
        return []

raw = input().strip()
lists = ast.literal_eval(raw) if raw != "[]" else []
print(Solution().k_way_list_merge(lists))
```

```java run
import java.util.*;

public class Main {
  static int[][] parseIntMatrix(String line) {
    String trimmed = line.trim();
    if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
    String inner = trimmed.substring(1, trimmed.length() - 1).trim();
    String[] rows = inner.split("\\],\\s*\\[");
    int[][] mat = new int[rows.length][];
    for (int r = 0; r < rows.length; r++) {
      String row = rows[r].replaceAll("[\\[\\]\\s]", "");
      if (row.isEmpty()) { mat[r] = new int[0]; continue; }
      String[] parts = row.split(",");
      mat[r] = new int[parts.length];
      for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
    }
    return mat;
  }

  static class Solution {
    public List<Integer> kWayListMerge(int[][] lists) {
      // Your code goes here — push {value, listIdx, elemIdx} for the first
      // element of each non-empty list. Pop minimum repeatedly, appending to
      // result and pushing the next element from that list.
      return new ArrayList<>();
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[][] lists = parseIntMatrix(sc.nextLine());
    System.out.println(new Solution().kWayListMerge(lists));
  }
}
```

```testcases
{
  "args": [
    { "id": "lists", "label": "lists", "type": "int[][]", "placeholder": "[[1, 4, 5], [1, 3, 4], [2, 6]]" }
  ],
  "cases": [
    { "args": { "lists": "[[1, 4, 5], [1, 3, 4], [2, 6]]" }, "expected": "[1, 1, 2, 3, 4, 4, 5, 6]" },
    { "args": { "lists": "[]" }, "expected": "[]" },
    { "args": { "lists": "[[1, 2, 3]]" }, "expected": "[1, 2, 3]" },
    { "args": { "lists": "[[1], [2], [3]]" }, "expected": "[1, 2, 3]" },
    { "args": { "lists": "[[1, 1, 1], [1, 1]]" }, "expected": "[1, 1, 1, 1, 1]" }
  ]
}
```

<details>
<summary><h2>The Strategy</h2></summary>

The textbook K-way merge: at every step, the next element of the merged list is the *globally smallest* among the heads of all unmerged lists. A min-heap of size K holds those heads. Pop the smallest, append to the output, push the *next* element of that list (if any). Done in `O(N log K)` total, where `N` is the total number of elements.

The comparator is "compare list elements by value, ascending". The heap entry carries `(value, listIndex, elementIndex)` so we can retrieve the next element.

</details>
<details>
<summary><h2>Solution</h2></summary>

A min-heap of `(value, listIdx, elemIdx)` tuples (using `listIdx` as a tiebreaker to avoid comparing lists). Each pop yields the current global minimum; push its successor to continue. The merged output is naturally sorted — no post-sort needed.

```python solution time=O(n log k) space=O(k)
import ast
import heapq

class Solution:
    def k_way_list_merge(self, lists):
        result = []
        # push (value, list_idx, elem_idx) — list_idx breaks value ties
        min_heap = []
        for i, lst in enumerate(lists):
            if lst:
                heapq.heappush(min_heap, (lst[0], i, 0))
        while min_heap:
            val, i, j = heapq.heappop(min_heap)
            result.append(val)
            if j + 1 < len(lists[i]):
                heapq.heappush(min_heap, (lists[i][j + 1], i, j + 1))
        return result

raw = input().strip()
lists = ast.literal_eval(raw) if raw != "[]" else []
print(Solution().k_way_list_merge(lists))
```

```java solution
import java.util.*;

public class Main {
  static int[][] parseIntMatrix(String line) {
    String trimmed = line.trim();
    if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
    String inner = trimmed.substring(1, trimmed.length() - 1).trim();
    String[] rows = inner.split("\\],\\s*\\[");
    int[][] mat = new int[rows.length][];
    for (int r = 0; r < rows.length; r++) {
      String row = rows[r].replaceAll("[\\[\\]\\s]", "");
      if (row.isEmpty()) { mat[r] = new int[0]; continue; }
      String[] parts = row.split(",");
      mat[r] = new int[parts.length];
      for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
    }
    return mat;
  }

  static class Solution {
    public List<Integer> kWayListMerge(int[][] lists) {
      List<Integer> result = new ArrayList<>();
      // heap entry: {value, listIdx, elemIdx} — listIdx breaks ties
      PriorityQueue<int[]> minHeap = new PriorityQueue<>(
        (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
      for (int i = 0; i < lists.length; i++) {
        if (lists[i].length > 0) minHeap.offer(new int[]{lists[i][0], i, 0});
      }
      while (!minHeap.isEmpty()) {
        int[] top = minHeap.poll();
        int val = top[0], i = top[1], j = top[2];
        result.add(val);
        if (j + 1 < lists[i].length) minHeap.offer(new int[]{lists[i][j + 1], i, j + 1});
      }
      return result;
    }
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int[][] lists = parseIntMatrix(sc.nextLine());
    System.out.println(new Solution().kWayListMerge(lists));
  }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

A comparator is the **bridge between a generic priority queue and any custom type with a total order**. Once you can plug a comparator in, every Top-K problem from lesson 3 generalises to records, structs, tree nodes, list nodes — anything with a defined ordering.

Three patterns to take with you:

1. **Heap of records, ordered by score.** Word + frequency, point + distance, pair + sum, list-node + value. The heap holds *records*, the comparator orders by the *score field*.
2. **K-way merge with a heap of size K.** When you need the global minimum across K sorted streams, a heap of size K with one head per stream gives it to you in O(log K) per pop. K-way merge, K-sorted ranges, K-way list merge — all the same skeleton.
3. **Tiebreakers in language-specific ways.** Most heap libraries can't compare arbitrary types directly (Python tuples, Rust `Box`); inserting a unique counter or a list index as a tiebreaker is a common idiom that prevents the comparator from ever needing to look at non-comparable fields.

</details>
