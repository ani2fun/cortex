---
title: "Overlap Reduction"
summary: "Given an array of intervals where intervals[i] = [si, ei], merge all overlapping intervals and return a list of non-overlapping intervals that covers all the intervals in the input. You can return the"
prereqs:
  - 10-pattern-interval-merging/01-pattern
difficulty: medium
kind: problem
topics: [intervals, arrays]
---

# Overlap Reduction

## The Problem

Given an array of `intervals` where `intervals[i] = [si, ei]`, merge all **overlapping** intervals and return a list of non-overlapping intervals that covers all the intervals in the input. You can return the answer in **any order**.

Two intervals `[s1, e1]` and `[s2, e2]` are considered **overlapping** if `e1 ≥ s2` (so touching counts as overlap and the two halves merge).

```
intervals = [[1, 4], [2, 3], [3, 4], [4, 6]]   →  [[1, 6]]
intervals = [[1, 5], [1, 5], [1, 5]]            →  [[1, 5]]
intervals = [[1, 5], [6, 7], [8, 9]]            →  [[1, 5], [6, 7], [8, 9]]
```

---

## Examples

**Example 1**
```
Input:  intervals = [[1, 4], [2, 3], [3, 4], [4, 6]]
Output: [[1, 6]]
Explanation: [2, 3] and [3, 4] already lie within [1, 4]; [1, 4] and [4, 6]
             touch (overlap by our definition) and merge into [1, 6].
```

**Example 2**
```
Input:  intervals = [[1, 5], [1, 5], [1, 5]]
Output: [[1, 5]]
Explanation: All three are the same interval — they collapse into one.
```

**Example 3**
```
Input:  intervals = [[1, 5], [6, 7], [8, 9]]
Output: [[1, 5], [6, 7], [8, 9]]
Explanation: The intervals are already pairwise non-overlapping; nothing merges.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "intervals = [[1, 3], [2, 4]]",
  "options": ["[[1, 4]]", "[[1, 3], [2, 4]]", "[[2, 4], [1, 3]]", "[[1, 3]]"],
  "answer": "[[1, 4]]"
}
```

## Constraints

- `1 ≤ intervals.length ≤ 10^4`
- `intervals[i] = [si, ei]` with `0 ≤ si ≤ ei ≤ 10^6`

```python run viz=grid viz-root=intervals
import ast
from typing import List

class Solution:
    def overlap_reduction(self, intervals: List[List[int]]) -> List[List[int]]:
        # Your code goes here — sort by start, then sweep, extending the last
        # merged interval on overlap (start ≤ last.end) and appending otherwise.
        return []

intervals = ast.literal_eval(input())    # the test case's intervals
print(Solution().overlap_reduction(intervals))
```

```java run viz=grid viz-root=intervals
import java.util.*;

public class Main {
    static class Solution {
        public int[][] overlapReduction(int[][] intervals) {
            // Your code goes here — sort by start, then sweep, extending the last
            // merged interval on overlap (start <= last.end) and appending otherwise.
            return new int[0][];
        }
    }

    public static void main(String[] args) {
        int[][] intervals = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(Arrays.deepToString(new Solution().overlapReduction(intervals)));
    }

    // "[[1, 3], [2, 6]]" → {{1, 3}, {2, 6}} — reads the test case's intervals
    static int[][] parseIntMatrix(String line) {
        String s = line.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        s = s.trim();
        if (s.isEmpty()) return new int[0][];
        String[] rows = s.split("\\]\\s*,\\s*\\[");
        int[][] out = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String inner = rows[i].replaceAll("[\\[\\]\\s]", "");
            if (inner.isEmpty()) { out[i] = new int[0]; continue; }
            String[] parts = inner.split(",");
            int[] pair = new int[parts.length];
            for (int j = 0; j < parts.length; j++) pair[j] = Integer.parseInt(parts[j]);
            out[i] = pair;
        }
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "intervals", "label": "intervals", "type": "int[][]", "placeholder": "[[1, 4], [2, 3], [3, 4], [4, 6]]" }
  ],
  "cases": [
    { "args": { "intervals": "[[1, 4], [2, 3], [3, 4], [4, 6]]" }, "expected": "[[1, 6]]" },
    { "args": { "intervals": "[[1, 5], [1, 5], [1, 5]]" }, "expected": "[[1, 5]]" },
    { "args": { "intervals": "[[1, 5], [6, 7], [8, 9]]" }, "expected": "[[1, 5], [6, 7], [8, 9]]" },
    { "args": { "intervals": "[[1, 3], [2, 4]]" }, "expected": "[[1, 4]]" },
    { "args": { "intervals": "[[1, 10], [2, 3], [4, 5]]" }, "expected": "[[1, 10]]" },
    { "args": { "intervals": "[[5, 6], [1, 2], [3, 4]]" }, "expected": "[[1, 2], [3, 4], [5, 6]]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is one-dimensional ordering on a number line — every interval is a `[start, end]` pair on the same axis. Sorting by `start` collapses the problem to a left-to-right scan and forces every overlap to appear between an interval and the most recently kept merged block. Intervals earlier in the sorted list with smaller `end` values are forever closed; they cannot be touched by anything that follows.

The state is a running `merged` list, and only its **last** entry is ever inspected. For each `intervals[i]`, two pointers play minimal roles: the read pointer `i` walking the sorted input, and an implicit write pointer at `merged[-1]`. If `intervals[i].start ≤ merged[-1].end`, the current interval extends the last block via `max(merged[-1].end, intervals[i].end)` — the `max` matters because `intervals[i]` could be nested inside `merged[-1]` (smaller end). Otherwise the current interval becomes a fresh entry in `merged`.

The naive "compare every pair, repeat until stable" approach pays up to O(N³) — it re-checks pairs that were already disjoint and rediscovers ordering it never enforced. By contrast, the sorted-then-sweep version visits each interval exactly once and performs a single O(1) comparison per visit. The O(N log N) sort is the only cost beyond linear; the merge itself is O(N) time and O(N) space for the output.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

| Check | Answer for Overlap Reduction |
|---|---|
| **Is the input a set of `[start, end]` intervals on a 1-D axis?** | Yes — the entire input is an interval list |
| **Does the answer depend on which intervals overlap?** | Yes — overlapping intervals are exactly the ones to fuse |
| **Would the answer be unchanged if you replaced the input with its merged form?** | Yes — the merged form **is** the answer |
| **Can you derive the answer from one left-to-right pass after sorting by `start`?** | Yes — extend `merged[-1].end` on overlap, append otherwise |

This is the canonical interval-merging problem; every diagnostic check fires `yes` and the output is literally the merged list.

</details>
<details>
<summary><h2>Approach</h2></summary>

1. Sort `intervals` by `start` ascending; break ties by `end` ascending.
2. Seed the output: `merged = [intervals[0]]`.
3. Walk `i` from `1` to `len(intervals) - 1`. At each step, compare `intervals[i].start` against `merged[-1].end`.
4. **Overlap** (`intervals[i].start ≤ merged[-1].end`) → extend the last entry: `merged[-1].end = max(merged[-1].end, intervals[i].end)`. The `max` is what keeps a fully-nested interval from accidentally shrinking the merged block.
5. **No overlap** (`intervals[i].start > merged[-1].end`) → append `intervals[i]` as a fresh entry in `merged`.
6. After the sweep, return `merged`.

The `<=` is what makes touching intervals like `[1, 4]` and `[4, 6]` merge under this problem's overlap rule. Switching to `<` would treat them as adjacent but distinct.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(N log N) space=O(N)
import ast
from typing import List

class Solution:
    def overlap_reduction(
        self, intervals: List[List[int]]
    ) -> List[List[int]]:

        # Sort intervals by start time
        intervals.sort(key=lambda x: x[0])

        # Initialize output array and push the first interval
        merged = [intervals[0]]

        # Loop through intervals and merge if overlapping or adjacent
        for i in range(1, len(intervals)):

            # If the current interval overlaps with the last interval in
            # the merged list, merge them
            if intervals[i][0] <= merged[-1][1]:
                merged[-1][1] = max(merged[-1][1], intervals[i][1])

            # If the current interval is non-overlapping, add it to the
            # merged list
            else:
                merged.append(intervals[i])
        return merged


intervals = ast.literal_eval(input())    # the test case's intervals
print(Solution().overlap_reduction(intervals))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[][] overlapReduction(int[][] intervals) {

            // Sort intervals by start time
            Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));

            // Initialize output list and push the first interval
            List<int[]> merged = new ArrayList<>();
            merged.add(intervals[0]);

            // Loop through intervals and merge if overlapping or adjacent
            for (int i = 1; i < intervals.length; i++) {

                // If the current interval overlaps with the last interval in
                // the merged list, merge them
                if (intervals[i][0] <= merged.get(merged.size() - 1)[1]) {
                    merged.get(merged.size() - 1)[1] = Math.max(
                        merged.get(merged.size() - 1)[1],
                        intervals[i][1]
                    );
                }

                // If the current interval is non-overlapping, add it to the
                // merged list
                else {
                    merged.add(intervals[i]);
                }
            }

            // Convert output list to 2D array and return
            return merged.toArray(new int[merged.size()][]);
        }
    }

    public static void main(String[] args) {
        int[][] intervals = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(Arrays.deepToString(new Solution().overlapReduction(intervals)));
    }

    // "[[1, 3], [2, 6]]" → {{1, 3}, {2, 6}} — reads the test case's intervals
    static int[][] parseIntMatrix(String line) {
        String s = line.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        s = s.trim();
        if (s.isEmpty()) return new int[0][];
        String[] rows = s.split("\\]\\s*,\\s*\\[");
        int[][] out = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String inner = rows[i].replaceAll("[\\[\\]\\s]", "");
            if (inner.isEmpty()) { out[i] = new int[0]; continue; }
            String[] parts = inner.split(",");
            int[] pair = new int[parts.length];
            for (int j = 0; j < parts.length; j++) pair[j] = Integer.parseInt(parts[j]);
            out[i] = pair;
        }
        return out;
    }
}
```


<details>
<summary><strong>Trace — intervals = [[1, 4], [2, 3], [3, 4], [4, 6]]</strong></summary>

```
After sort by start: [[1, 4], [2, 3], [3, 4], [4, 6]] (already sorted)

Initial: merged = [[1, 4]]

i=1: [2, 3]. 2 ≤ merged[-1].end (4) → merge: merged[-1].end = max(4, 3) = 4.
     merged = [[1, 4]]
i=2: [3, 4]. 3 ≤ 4 → merge: merged[-1].end = max(4, 4) = 4.
     merged = [[1, 4]]
i=3: [4, 6]. 4 ≤ 4 → merge: merged[-1].end = max(4, 6) = 6.
     merged = [[1, 6]]

Result: [[1, 6]] ✓
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N log N) | Sort dominates; the merge sweep is O(N) |
| **Space** | O(N) | The `merged` list holds at most `N` intervals |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single interval | `[[3, 7]]` | `[[3, 7]]` | Loop body never runs; the seed is the answer |
| All identical | `[[1, 5], [1, 5], [1, 5]]` | `[[1, 5]]` | Each merges into the seed |
| Already disjoint | `[[1, 2], [4, 5]]` | `[[1, 2], [4, 5]]` | The else branch fires every iteration |
| Touching intervals | `[[1, 4], [4, 6]]` | `[[1, 6]]` | `4 ≤ 4` triggers merge — touching counts as overlap here |
| Fully nested | `[[1, 10], [3, 5]]` | `[[1, 10]]` | After sort, `[3, 5]` is contained in `[1, 10]`; `max(10, 5) = 10` |
| Out-of-order input | `[[8, 9], [1, 5], [6, 7]]` | `[[1, 5], [6, 7], [8, 9]]` | Sort fixes order; nothing merges |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Overlap Reduction is the canonical **merge overlapping intervals** problem — the merged list returned by sort + sweep **is** the answer. What is new vs Verify Schedule: you now build and return the `merged` list rather than only detecting the first overlap, and the comparison flips from `<` to `<=` so that touching intervals fuse.

</details>
