---
title: "Employee Free Time"
summary: "Given an array of meetings consisting of the start and end times [[s1, e1], [s2, e2], ...] (with si < ei) of all meetings of the employees of a company, find and return the time intervals where all th"
prereqs:
  - 10-pattern-interval-merging/01-pattern
difficulty: medium
kind: problem
topics: [intervals, arrays]
---

# Employee Free Time

## The Problem

Given an array of `meetings` consisting of the start and end times `[[s1, e1], [s2, e2], ...]` (with `si < ei`) of all meetings of the employees of a company, find and return the **time intervals where all the employees are free** — i.e. none of them are in a meeting.

Two intervals `[s1, e1]` and `[s2, e2]` are considered **overlapping** if `e1 ≥ s2` (touching counts as overlap, so no zero-length free windows are emitted).

```
meetings = [[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]]   →  [[6, 8]]
meetings = [[1, 2], [4, 6], [5, 7], [9, 10]]           →  [[2, 4], [7, 9]]
meetings = [[1, 5], [2, 4], [5, 9]]                    →  []
```

---

## Examples

**Example 1**
```
Input:  meetings = [[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]]
Output: [[6, 8]]
Explanation: All the employees will be free only in the interval [6, 8].
```

**Example 2**
```
Input:  meetings = [[1, 2], [4, 6], [5, 7], [9, 10]]
Output: [[2, 4], [7, 9]]
Explanation: All the employees will be free only in the intervals [2, 4]
             and [7, 9].
```

**Example 3**
```
Input:  meetings = [[1, 5], [2, 4], [5, 9]]
Output: []
Explanation: There are no time intervals during which all employees are
             simultaneously free.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "meetings = [[1, 2], [5, 6]]",
  "options": ["[[2, 5]]", "[]", "[[1, 6]]", "[[2, 5], [6, 6]]"],
  "answer": "[[2, 5]]"
}
```

## Constraints

- `1 ≤ meetings.length ≤ 10^4`
- `meetings[i] = [si, ei]` with `0 ≤ si < ei ≤ 10^6`

```python run viz=grid viz-root=meetings
import ast
from typing import List

class Solution:
    def employee_free_time(self, meetings: List[List[int]]) -> List[List[int]]:
        # Your code goes here — merge the meetings (sort by start, sweep), then
        # emit [prev.end, curr.start] for every gap between consecutive blocks.
        return []

meetings = ast.literal_eval(input())     # the test case's meetings
print(Solution().employee_free_time(meetings))
```

```java run viz=grid viz-root=meetings
import java.util.*;

public class Main {
    static class Solution {
        public int[][] employeeFreeTime(int[][] meetings) {
            // Your code goes here — merge the meetings (sort by start, sweep), then
            // emit [prev.end, curr.start] for every gap between consecutive blocks.
            return new int[0][];
        }
    }

    public static void main(String[] args) {
        int[][] meetings = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(Arrays.deepToString(new Solution().employeeFreeTime(meetings)));
    }

    // "[[1, 3], [2, 6]]" → {{1, 3}, {2, 6}} — reads the test case's meetings
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
    { "id": "meetings", "label": "meetings", "type": "int[][]", "placeholder": "[[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]]" }
  ],
  "cases": [
    { "args": { "meetings": "[[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]]" }, "expected": "[[6, 8]]" },
    { "args": { "meetings": "[[1, 2], [4, 6], [5, 7], [9, 10]]" }, "expected": "[[2, 4], [7, 9]]" },
    { "args": { "meetings": "[[1, 5], [2, 4], [5, 9]]" }, "expected": "[]" },
    { "args": { "meetings": "[[1, 2], [5, 6]]" }, "expected": "[[2, 5]]" },
    { "args": { "meetings": "[[1, 2], [4, 5], [7, 8]]" }, "expected": "[[2, 4], [5, 7]]" },
    { "args": { "meetings": "[[1, 3], [3, 5]]" }, "expected": "[]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is that "everyone free" reduces to "nobody busy". The set of busy moments is the union of every meeting interval, and the set of free moments is its complement on the time axis. Once you state the problem this way, you don't need to track who is in which meeting — only the union of all meetings as a single set of disjoint busy blocks.

The sweep is the standard interval-merge sweep, with a second pass tacked on. Sorting by `start` lets you merge overlapping meetings into a list of pairwise-disjoint busy blocks. The merged list is the bookkeeping; the answer falls out of the **gaps** between consecutive blocks. Walking `merged` once more and emitting `[prev.end, curr.start]` whenever `prev.end < curr.start` produces the free intervals. The `<=` comparator during merge means touching meetings collapse into one block, so no zero-length free windows are emitted.

The naive approach iterates time minute-by-minute (or per-event tick) and asks "is anyone busy now?" — O(T × N) where `T` is the time span and `N` is the meeting count. That cost is unbounded if `T` is large or continuous. Sorting plus the merge sweep replaces the time-axis scan with a meeting-axis scan: O(N log N) sort + O(N) merge + O(N) gap pass. The complement is computed without ever enumerating an empty moment.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

| Check | Answer for Employee Free Time |
|---|---|
| **Is the input a set of `[start, end]` intervals on a 1-D axis?** | Yes — meetings on a single time axis (any employee identity is irrelevant) |
| **Does the answer depend on which intervals overlap?** | Yes — overlapping meetings collapse, and only the gaps between merged blocks become free time |
| **Would the answer be unchanged if you replaced the input with its merged form?** | Yes — the merged form is the only thing the gap pass needs |
| **Can you derive the answer from one left-to-right pass after sorting by `start`?** | Yes — one merge pass plus one gap pass, both O(N) |

The pattern fits with one extra step: a second pass reads gaps from the merged list.

</details>
<details>
<summary><h2>Approach</h2></summary>

1. Sort `meetings` by `start` ascending.
2. Seed the merged list: `merged = [meetings[0]]`.
3. For each subsequent meeting, compare its `start` against `merged[-1].end`. If `start ≤ merged[-1].end`, extend via `merged[-1].end = max(merged[-1].end, meetings[i].end)`. Otherwise append the meeting as a new merged block.
4. Walk `merged` again from index `1`. For every consecutive pair `(prev, curr)`, if `prev.end < curr.start`, append `[prev.end, curr.start]` to the `free_times` output.
5. Return `free_times`. Time before the earliest meeting and after the latest are not reported — only gaps bounded on both sides by busy blocks.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(N log N) space=O(N)
import ast
from typing import List

class Solution:
    def employee_free_time(
        self, meetings: List[List[int]]
    ) -> List[List[int]]:

        # Sort meetings by start time
        meetings.sort(key=lambda x: x[0])

        # Initialize output array and push the first meeting
        merged = [meetings[0]]

        # Loop through meetings and merge if overlapping or adjacent
        for i in range(1, len(meetings)):

            # If the current meeting overlaps with the last meeting in
            # the merged list, merge them
            if meetings[i][0] <= merged[-1][1]:
                merged[-1][1] = max(merged[-1][1], meetings[i][1])

            # If the current meeting is non-overlapping, add it to the
            # merged list
            else:
                merged.append(meetings[i])

        # Find free time slots between merged meetings
        free_times: List[List[int]] = []
        for i in range(1, len(merged)):
            start: int = merged[i - 1][1]
            end: int = merged[i][0]
            if start < end:
                free_times.append([start, end])
        return free_times


meetings = ast.literal_eval(input())     # the test case's meetings
print(Solution().employee_free_time(meetings))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[][] employeeFreeTime(int[][] meetings) {

            // Sort meetings by start time
            Arrays.sort(meetings, (a, b) -> Integer.compare(a[0], b[0]));

            // Initialize output list and push the first meeting
            List<int[]> merged = new ArrayList<>();
            merged.add(meetings[0]);

            // Loop through meetings and merge if overlapping or adjacent
            for (int i = 1; i < meetings.length; i++) {

                // If the current meeting overlaps with the last meeting in
                // the merged list, merge them
                if (meetings[i][0] <= merged.get(merged.size() - 1)[1]) {
                    merged.get(merged.size() - 1)[1] = Math.max(
                        merged.get(merged.size() - 1)[1],
                        meetings[i][1]
                    );
                }

                // If the current meeting is non-overlapping, add it to the
                // merged list
                else {
                    merged.add(meetings[i]);
                }
            }

            // Find gaps between merged meetings to get free time
            List<int[]> freeTimes = new ArrayList<>();
            for (int i = 1; i < merged.size(); i++) {
                int start = merged.get(i - 1)[1];
                int end = merged.get(i)[0];
                if (start < end) {
                    freeTimes.add(new int[] { start, end });
                }
            }

            // Convert the list to an array and return
            return freeTimes.toArray(new int[freeTimes.size()][]);
        }
    }

    public static void main(String[] args) {
        int[][] meetings = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(Arrays.deepToString(new Solution().employeeFreeTime(meetings)));
    }

    // "[[1, 3], [2, 6]]" → {{1, 3}, {2, 6}} — reads the test case's meetings
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
<summary><strong>Trace — meetings = [[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]]</strong></summary>

```
After sort by start: [[1, 4], [2, 3], [3, 4], [4, 6], [8, 9]] (already sorted)

Merge:
  init     merged = [[1, 4]]
  [2, 3]: 2 ≤ 4 → merge: merged[-1].end = max(4, 3) = 4 → merged = [[1, 4]]
  [3, 4]: 3 ≤ 4 → merge: merged[-1].end = max(4, 4) = 4 → merged = [[1, 4]]
  [4, 6]: 4 ≤ 4 → merge: merged[-1].end = max(4, 6) = 6 → merged = [[1, 6]]
  [8, 9]: 8 > 6 → append → merged = [[1, 6], [8, 9]]

Gaps:
  pair ([1, 6], [8, 9]): 6 < 8 → emit [6, 8]

Result: [[6, 8]] ✓
```

</details>

### Complexity Analysis

Let `N` be the total number of meetings in the (already flattened) `meetings` input.

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N log N) | Sorting the meetings list dominates; the merge sweep and gap-scan are each O(N) |
| **Space** | O(N) | The `merged` list holds at most `N` intervals; the `free_times` output is at most `N − 1` gaps |

If the input arrives as a per-employee schedule (a list of lists), flatten it into a single meetings list before calling this function — the flatten step is O(N) and doesn't change the overall complexity. A more advanced technique uses a min-heap of one interval per employee for O(N log K) time (`K` employees), trading sort overhead for heap overhead. The flatten-and-merge approach above is simpler, just as correct, and almost always fast enough.

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| All meetings overlap into one block | `[[1,5], [2,4], [3,6]]` | `[]` | Merge → `[[1,6]]`; only one block, no internal gaps |
| Single meeting | `[[1,2]]` | `[]` | Single merged block, no internal gaps |
| Two meetings with a gap | `[[1,2], [5,6]]` | `[[2,5]]` | Merged list of 2 → one gap reported |
| Touching busy intervals | `[[1,3], [3,5]]` | `[]` | `<=` merges them into `[1,5]` — no gap |
| Outside-the-range gaps excluded | `[[3,5], [7,9]]` | `[[5,7]]` | Time before 3 and after 9 is *not* reported |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Employee Free Time is interval merging applied to the **complement**. What is new vs Overlap Reduction: the merged list is bookkeeping, not the answer — a second O(N) pass reads gaps between consecutive merged blocks and that gap list is what gets returned.

> **Transfer Challenge:** Modify the function to also return the **total** amount of free time (sum of gap lengths) along with the gap intervals.
>
> <details><summary><strong>Solution hint</strong></summary>
>
> While building the `free` list, also accumulate `total += gap_end - gap_start`. Return both. No extra pass needed.
>
> </details>

</details>
