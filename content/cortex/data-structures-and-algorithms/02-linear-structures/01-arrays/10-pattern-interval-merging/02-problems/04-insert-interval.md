---
title: "Insert Interval"
summary: "> You are given an array intervals of non-overlapping intervals, sorted by start coordinate ascending, and a new interval newInterval = [s, e]. Insert newInterval into intervals and return the resulti"
prereqs:
  - 10-pattern-interval-merging/01-pattern
difficulty: hard
---

# Insert Interval

<details>
<summary><h2>The Hook</h2></summary>


You manage a calendar that's already been carefully merged — every entry sits in sorted order, and none overlap. A new event comes in. Naively you could append it and re-run the full merge for an `O(N log N)` rebuild. But you've been handed a **gift**: the existing list is already sorted and disjoint. The right algorithm exploits that and finishes in a single linear pass — **O(N)**, no sort. This is the cleanest possible expression of the interval-merging idea, and it shows up in calendar libraries, schedule builders, and version-control merge tools everywhere.

</details>
## The Problem

> You are given an array `intervals` of non-overlapping intervals, sorted by start coordinate ascending, and a new interval `newInterval = [s, e]`. Insert `newInterval` into `intervals` and return the resulting array, **still sorted and still non-overlapping** (merging where necessary). Touching intervals are merged.

```
Input:  intervals = [[1, 3], [6, 9]],          newInterval = [2, 5]
Output: [[1, 5], [6, 9]]               ([2,5] eats into [1,3])

Input:  intervals = [[1, 2], [3, 5], [6, 7], [8, 10], [12, 16]],   newInterval = [4, 8]
Output: [[1, 2], [3, 10], [12, 16]]    ([4,8] swallows [3,5], [6,7], [8,10])

Input:  intervals = [],                        newInterval = [5, 7]
Output: [[5, 7]]

Input:  intervals = [[1, 5]],                  newInterval = [6, 8]
Output: [[1, 5], [6, 8]]               (no overlap → just append in the right place)

Input:  intervals = [[3, 5], [12, 15]],        newInterval = [6, 10]
Output: [[3, 5], [6, 10], [12, 15]]    (slots cleanly between)
```

---

## Examples

**Example 1**
```
Input:  intervals = [[1, 3], [6, 9]],   newInterval = [2, 5]
Output: [[1, 5], [6, 9]]
Explanation: [2, 5] overlaps [1, 3] (start 2 ≤ end 3), absorbing
             it into [1, 5]. [6, 9] is strictly after and untouched.
```

**Example 2**
```
Input:  intervals = [[1, 2], [3, 5], [6, 7], [8, 10], [12, 16]]
        newInterval = [4, 8]
Output: [[1, 2], [3, 10], [12, 16]]
Explanation: [4, 8] absorbs [3, 5], [6, 7], and [8, 10] — its bounds
             grow to [3, 10] before being pushed.
```

**Example 3**
```
Input:  intervals = [],   newInterval = [5, 7]
Output: [[5, 7]]
Explanation: Empty input — only the push-the-new-interval step runs.
```

<details>
<summary><h2>Intuition</h2></summary>

The structural property is the **precondition**: `intervals` is already sorted by `start` and pairwise disjoint. That precondition is a gift — it means no sort is needed and any pass that respects the existing order takes O(N). The new interval splits the existing list into three contiguous groups: everything strictly before the new interval, everything that overlaps it, and everything strictly after. Because the input is sorted, these three groups appear in order; once you leave one you never come back.

Three index ranges, one read pointer. A single `i` walks left to right and lands in exactly one of three phases at each step. In **Phase 1**, the interval ends before the new interval starts — copy it through. In **Phase 2**, the interval overlaps the new one — absorb it by stretching the new interval's bounds via `min(start)` and `max(end)`. In **Phase 3**, the interval starts after the new interval's grown end — copy it through. The pointer never moves backwards because the sorted precondition guarantees the three phases are contiguous.

The naive alternative is to append the new interval and re-run the standard sort + merge — correct but O(N log N), throwing away the precondition. The three-phase sweep avoids the sort entirely by exploiting it. For a single insert the gap is one log factor; for streaming inserts in a calendar UI, repeatedly rebuilding the sort would dominate.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

| Check | Answer for Insert Interval |
|---|---|
| **Is the input a set of `[start, end]` intervals on a 1-D axis?** | Yes — and the input is additionally **sorted and disjoint** |
| **Does the answer depend on which intervals overlap?** | Yes — overlap with `newInterval` decides absorb vs copy |
| **Would the answer be unchanged if you replaced the input with its merged form?** | The input is already merged — that is the whole point of the precondition |
| **Can you derive the answer from one left-to-right pass after sorting by `start`?** | Yes — and the sort itself is free, since the input is already in order |

All four checks fire `yes` with the unique twist that the sort step costs nothing — the cost collapses from O(N log N) to O(N).

</details>
<details>
<summary><h2>Approach</h2></summary>

1. Initialise an empty output list `merged` and an index `i = 0`.
2. **Phase 1 — copy strictly-before intervals.** While `i < n` and `intervals[i].end < newInterval.start`, append `intervals[i]` to `merged` and advance `i`.
3. **Phase 2 — absorb overlapping intervals.** While `i < n` and `intervals[i].start ≤ newInterval.end`, grow `newInterval` via `newInterval.start = min(newInterval.start, intervals[i].start)` and `newInterval.end = max(newInterval.end, intervals[i].end)`, then advance `i`. The `min`/`max` formulation correctly handles fully-nested intervals.
4. Append the (possibly-grown) `newInterval` to `merged`.
5. **Phase 3 — copy strictly-after intervals.** While `i < n`, append `intervals[i]` to `merged` and advance `i`.
6. Return `merged`.

The strict `<` in Phase 1 and the `≤` in Phase 2 are coordinated: a touching interval like `[1, 3]` against `newInterval = [3, 5]` skips Phase 1 (`3 < 3` is false) and enters Phase 2 (`1 ≤ 5` is true), so touching intervals merge.

</details>
<details>
<summary><h2>The Three-Phase Sweep</h2></summary>


The single linear pass partitions the existing intervals into **three groups** relative to `newInterval`:

1. **Strictly before** `newInterval` — keep them as-is.
2. **Overlap with** `newInterval` — absorb them into `newInterval` by stretching its `start` and `end`.
3. **Strictly after** `newInterval` — keep them as-is.

```d2
direction: right

input_arr: "intervals = [[1,2], [3,5], [6,7], [8,10], [12,16]],  newInterval = [4, 8]" {
  grid-columns: 5
  grid-gap: 16
  i1: "[1,2]" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  i2: "[3,5]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  i3: "[6,7]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  i4: "[8,10]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  i5: "[12,16]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
}

phase1: "Phase 1 — strictly before [4,8]" {
  p1: "[1,2]" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
}

phase2: "Phase 2 — overlapping [4,8] → absorb" {
  grid-rows: 2
  grid-gap: 16
  p2a: "[3,5]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  p2b: "[6,7]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  p2c: "[8,10]" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  note: "newInterval grows: [4,8] → [3,8] → [3,8] → [3,10]"
}

phase3: "Phase 3 — strictly after [3,10]" {
  p3: "[12,16]" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
}

result: "Output = [[1,2], [3,10], [12,16]]"

input_arr -> phase1
input_arr -> phase2
input_arr -> phase3
phase1 -> result
phase2 -> result
phase3 -> result
```

<p align="center"><strong>Three contiguous groups: copy-as-is, absorb, copy-as-is. The middle group collapses into a single grown <code>newInterval</code> before being appended.</strong></p>

The "copy / absorb / copy" structure is what makes the algorithm linear. Because the input is sorted, the three groups appear in order — once you leave group 1 you never go back, once you leave group 2 you never go back. A single index walks left-to-right.

</details>
<details>
<summary><h2>The Decision Rules</h2></summary>


For each `iv` in `intervals`:
- **Phase 1 (`iv.end < newInterval.start`)** → strictly before; copy to output.
- **Phase 3 (`iv.start > newInterval.end`)** → strictly after; copy to output (but make sure `newInterval` itself has been pushed first).
- **Otherwise** → overlapping with `newInterval`; absorb by `newInterval = [min(starts), max(ends)]`.

> *Predict — what happens if `intervals = [[1, 5]]` and `newInterval = [2, 3]` (fully contained)?*
>
> `iv = [1,5]`: not strictly before (`5 ≥ 2`), not strictly after (`1 ≤ 3`) → absorb. `newInterval` becomes `[min(1,2), max(5,3)] = [1, 5]`. We push `[1,5]`. Result: `[[1,5]]`. The `min/max` formulation handles containment automatically.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=intervals
from typing import List

class Solution:
    def insert_interval(
        self, intervals: List[List[int]], interval: List[int]
    ) -> List[List[int]]:

        # Initialize output list
        merged: List[List[int]] = []

        i: int = 0
        n: int = len(intervals)

        # Add all intervals that come before the new interval
        while i < n and intervals[i][1] < interval[0]:
            merged.append(intervals[i])
            i += 1

        # Merge overlapping intervals with the new interval
        while i < n and intervals[i][0] <= interval[1]:
            interval[0] = min(interval[0], intervals[i][0])
            interval[1] = max(interval[1], intervals[i][1])
            i += 1

        # Add the merged new interval
        merged.append(interval)

        # Add remaining intervals after the new interval
        while i < n:
            merged.append(intervals[i])
            i += 1

        return merged


# Examples from the problem statement
print(Solution().insert_interval([[1, 4], [6, 7]], [3, 6]))           # [[1, 7]]
print(Solution().insert_interval([[4, 5], [7, 9]], [6, 8]))           # [[4, 5], [6, 9]]
print(Solution().insert_interval([[1, 2], [6, 7], [8, 9]], [4, 5]))   # [[1, 2], [4, 5], [6, 7], [8, 9]]

# Edge cases
print(Solution().insert_interval([], [1, 3]))                          # [[1, 3]]  — empty list
print(Solution().insert_interval([[2, 5]], [1, 3]))                    # [[1, 5]]  — single overlap at start
print(Solution().insert_interval([[1, 2]], [3, 4]))                    # [[1, 2], [3, 4]]  — append at end
print(Solution().insert_interval([[3, 5], [7, 9]], [1, 2]))            # [[1, 2], [3, 5], [7, 9]]  — prepend
print(Solution().insert_interval([[1, 3], [4, 6], [7, 9]], [2, 8]))   # [[1, 9]]  — merge all
```

```java run viz=grid viz-root=intervals
import java.util.*;

public class Main {
    static class Solution {
        public int[][] insertInterval(int[][] intervals, int[] interval) {

            // Initialize output list
            List<int[]> merged = new ArrayList<>();

            int i = 0;
            int n = intervals.length;

            // Add all intervals that come before the new interval
            while (i < n && intervals[i][1] < interval[0]) {
                merged.add(intervals[i]);
                i++;
            }

            // Merge overlapping intervals with the new interval
            while (i < n && intervals[i][0] <= interval[1]) {
                interval[0] = Math.min(interval[0], intervals[i][0]);
                interval[1] = Math.max(interval[1], intervals[i][1]);
                i++;
            }

            // Add the merged new interval
            merged.add(interval);

            // Add remaining intervals after the new interval
            while (i < n) {
                merged.add(intervals[i]);
                i++;
            }

            return merged.toArray(new int[merged.size()][]);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{1, 4}, {6, 7}}, new int[]{3, 6})));           // [[1, 7]]
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{4, 5}, {7, 9}}, new int[]{6, 8})));           // [[4, 5], [6, 9]]
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{1, 2}, {6, 7}, {8, 9}}, new int[]{4, 5}))); // [[1, 2], [4, 5], [6, 7], [8, 9]]

        // Edge cases
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{}, new int[]{1, 3})));                          // [[1, 3]]  — empty list
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{2, 5}}, new int[]{1, 3})));                    // [[1, 5]]  — single overlap at start
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{1, 2}}, new int[]{3, 4})));                    // [[1, 2], [3, 4]]  — append at end
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{3, 5}, {7, 9}}, new int[]{1, 2})));            // [[1, 2], [3, 5], [7, 9]]  — prepend
        System.out.println(Arrays.deepToString(new Solution().insertInterval(new int[][]{{1, 3}, {4, 6}, {7, 9}}, new int[]{2, 8}))); // [[1, 9]]  — merge all
    }
}
```


<details>
<summary><strong>Trace — intervals = [[1, 2], [3, 5], [6, 7], [8, 10], [12, 16]], newInterval = [4, 8]</strong></summary>

```
ns = 4, ne = 8

Phase 1 (end < ns=4):
  i=0: [1,2]: 2 < 4 → copy.       result = [[1,2]]
  i=1: [3,5]: 5 < 4 is FALSE → exit phase 1.

Phase 2 (start <= ne, absorbing):
  i=1: [3,5]: 3 ≤ 8 → ns=min(4,3)=3, ne=max(8,5)=8.   ns=3, ne=8
  i=2: [6,7]: 6 ≤ 8 → ns=min(3,6)=3, ne=max(8,7)=8.   ns=3, ne=8
  i=3: [8,10]:8 ≤ 8 → ns=min(3,8)=3, ne=max(8,10)=10. ns=3, ne=10
  i=4: [12,16]: 12 ≤ 10 is FALSE → exit phase 2.
  Push grown [ns, ne] = [3, 10].   result = [[1,2], [3,10]]

Phase 3 (rest):
  i=4: [12,16] → copy.            result = [[1,2], [3,10], [12,16]]

Result: [[1,2], [3,10], [12,16]] ✓
The grown new interval absorbed three originals — note how the absorb loop
extended ne from 8 → 10 thanks to [8,10] sneaking in via the touch at start=8.
```

</details>
<details>
<summary><strong>Trace — intervals = [[1, 5]], newInterval = [6, 8]  (no overlap)</strong></summary>

```
ns = 6, ne = 8

Phase 1: i=0: [1,5]: 5 < 6 → copy.     result = [[1,5]]
Phase 2: i=1 = n, loop doesn't run. Push [6,8]. result = [[1,5], [6,8]]
Phase 3: nothing left.

Result: [[1,5], [6,8]] ✓
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | Single linear pass; index `i` only moves forward |
| **Space** | O(N) | Output array (no extra working storage beyond the result) |

Notice what's **not** in this table: there is **no sort**. We exploit the precondition that `intervals` is already sorted and disjoint to skip the O(N log N) step entirely. This is the structural reward for a well-prepared input.

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty input | `intervals=[]`, `new=[5,7]` | `[[5,7]]` | All three phases skip; only the push runs |
| New before everything | `[[3,5]]`, `[1,2]` | `[[1,2],[3,5]]` | Phase 1 empty; Phase 2 empty; Phase 3 copies |
| New after everything | `[[1,2]]`, `[5,7]` | `[[1,2],[5,7]]` | Phase 1 copies; Phase 2 empty; Phase 3 empty |
| Touching at left | `[[1,3],[6,9]]`, `[3,4]` | `[[1,4],[6,9]]` | `intervals[0].end=3 < ns=3` is **false**; absorbed |
| Touching at right | `[[1,3],[6,9]]`, `[3,6]` | `[[1,9]]` | Both originals absorb into the new |
| New fully contained | `[[1,10]]`, `[3,4]` | `[[1,10]]` | Phase 2 absorbs; `min/max` keeps original bounds |
| New swallows everything | `[[2,3],[5,6]]`, `[1,10]` | `[[1,10]]` | Both originals absorbed |

</details>
<details>
<summary><h2>Why Not Just Append + Re-Merge?</h2></summary>


It would be **correct**, but slower:

| Approach | Time | Space | Notes |
|---|---|---|---|
| **Append + sort + merge** | O(N log N) | O(N) | Throws away the precondition |
| **Three-phase sweep** | **O(N)** | O(N) | Exploits sorted+disjoint invariant |

For a single insert this difference matters; for *streaming* inserts (many in a row), it matters enormously — the three-phase sweep can be the difference between a snappy calendar UI and a sluggish one.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Insert Interval is the cleanest demonstration that a structural precondition can collapse a cost class. What is new vs Employee Free Time: the input arrives pre-sorted and disjoint, so the sort step disappears and the merge becomes a tailored **copy / absorb / copy** O(N) sweep instead of O(N log N).

> **Transfer Challenge:** Suppose the input list is the same (sorted and disjoint), but you receive a **batch** of `K` new intervals to insert (in any order). Design an algorithm better than calling `insert` `K` times. What's your best time complexity?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> Sort the K new intervals by start (O(K log K)). Now you have two sorted, possibly-overlapping streams: original (`N`) and new (`K`). Merge them in a single pass like the merge step of mergesort, then run the standard interval-merge over the combined sorted output (one more O(N+K) pass). Total: **O(N + K log K)** — beating the naive `K × O(N) = O(NK)` whenever `K log K < NK`, which is essentially always.
>
> </details>

---

You've now seen four problems that all reduce to the same core idea: **sort, then sweep with a single piece of state**. The state was the merged list (Verify, Overlap, Free Time), or the growing newInterval (Insert), or just the previous end (Verify). Once you internalize sort-then-sweep, every "interval" or "schedule" problem on the rest of this course — and on most interview whiteboards — yields to the same two-step dance.

The next section, **maximum overlap**, takes the sweep idea one step further: instead of merging, it *counts* — how many intervals overlap at any single point in time? The data structure changes (a counter, sometimes a heap), but the sweep is the same shape you've internalized here.

</details>
