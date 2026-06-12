---
title: "Twin in Proximity"
summary: "Given an array arr and integer k, return true if there are two distinct indices i and j with arr[i] == arr[j] and |i − j| ≤ k. Otherwise return false."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [variable-sized-sliding-window, hash-table]
---

# Twin in proximity

## Problem Statement

Given an array `arr` and integer `k`, return `true` if there are two distinct indices `i` and `j` with `arr[i] == arr[j]` and `|i − j| ≤ k`. Otherwise return `false`.

## Examples

**Example 1**
```
Input:  arr = [1, 2, 3, 4, 1], k = 5
Output: true
Explanation: the value 1 sits at indices 0 and 4. The gap is 4, which is ≤ 5 → a
twin within distance k exists.
```

**Example 2**
```
Input:  arr = [1, 2, 3, 4, 5, 6, 1], k = 5
Output: false
Explanation: the only repeated value is 1, at indices 0 and 6. The gap is 6 > 5, so
no twin is close enough.
```

**Example 3**
```
Input:  arr = [1, 7], k = 5
Output: false
Explanation: all values are distinct — there is no repeated value at any distance.
```

**Example 4**
```
Input:  arr = [1, 2, 1], k = 1
Output: false
Explanation: the two 1's are at indices 0 and 2, a gap of 2 > 1. With k = 1 only
adjacent equal values count.
```

<details>
<summary><h2>Approach</h2></summary>


Keep a hash map `element_index` from each value to the **most recent index** at which it appeared. When the right pointer reaches `arr[end]`, look the value up: if it's in the map *and* the gap to its stored index is `≤ k`, the two occurrences are a twin within distance `k` — return `true`. Otherwise overwrite the map entry with the current index. Conceptually the map's live entries are a sliding set of the last `k + 1` values; the window is kept that size by deleting `arr[start]`'s entry once `end − start ≥ k` and advancing `start`.

```d2
direction: right

inp: "arr = [1, 2, 3, 4, 1], k = 5"

s: "set after [1, 2, 3, 4]" {
  grid-columns: 4
  grid-gap: 0
  e1: "1"
  e2: "2"
  e3: "3"
  e4: "4"
}

check: "read 1 -> already in set"

r: "return true" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}

inp -> s -> check -> r
```

<p align="center"><strong>Twin in proximity — maintain a set of the last <code>k+1</code> values; if the new element is already in the set, a twin exists within distance <code>k</code>.</strong></p>

</details>
<details>
<summary><h2>Intuition</h2></summary>


This is a **fixed-width existence** problem, a different shape from the longest-window problems before it. The question is not "how long" but "does a duplicate value sit within `k` indices of an earlier copy?" — a yes/no over every contiguous window of width `k + 1`. A hash map from value to its most recent index turns each check into an `O(1)` lookup. This is the variable-sized window's existence cousin: the window has a *capped* size instead of a searched one.

The window is held to exactly `k + 1` indices, and that cap is the placement insight. As `end` advances, the map's live entries represent the last `k + 1` values seen. When `end − start ≥ k`, the element at `start` has fallen out of range, so its entry is deleted and `start` advances — keeping the window's width at most `k + 1`. Before inserting `arr[end]`, you check whether its value already lives in that window: if so, an earlier copy is within distance `k`, and you return `true` immediately.

The naive approach is correct but wasteful. For each index you would scan the previous `k` elements for a match, costing **O(N · k)** time. Sorting to bring equal values together loses the original indices, which are the whole point. The windowed map remembers only what is in range and answers each membership test in `O(1)`, giving **O(N)** time overall.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Twin in Proximity |
|---|---|
| **Q1.** Is the answer the longest/shortest/count of a contiguous subsequence? | **Yes (existence variant)** — a boolean over every contiguous window of width `k + 1`. |
| **Q2.** Can a hash map summarise the window for an `O(1)` rule check? | **Yes** — a value-to-latest-index map; the rule is "is `arr[end]` already in the window?" |
| **Q3.** Can you add `arr[end]` and remove `arr[start]` in `O(1)`? | **Yes** — insert the new value's index; delete `arr[start]`'s entry once it falls out of range. |
| **Q4.** Is the rule monotonic as the window grows? | **Yes** — the window is held to a fixed width `k + 1`, so eviction is mechanical, not condition-driven. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `start = 0` and an empty map `element_index` from value to its most recent index.
2. Advance `end` across the array. First, check the rule: if `arr[end]` is already a key *and* `end − element_index[arr[end]] ≤ k`, return `true`.
3. Otherwise, record the current position: set `element_index[arr[end]] = end`.
4. Enforce the window width: if `end − start ≥ k`, delete `element_index[arr[start]]` and advance `start`, so the window never exceeds `k + 1` indices.
5. After the loop, return `false` — no twin within distance `k` was found.

</details>

```quiz
{
  "prompt": "What does twin_in_proximity([1, 2, 1], 2) return?",
  "input": "arr = [1, 2, 1], k = 2",
  "options": ["true", "false"],
  "answer": "true"
}
```

## Constraints

- `1 ≤ arr.length ≤ 10⁵`
- `-10⁹ ≤ arr[i] ≤ 10⁹`
- `0 ≤ k ≤ 10⁵`

```python run
import ast

arr = ast.literal_eval(input())
k = int(input())

class Solution:
    def twin_in_proximity(self, arr, k):
        # Your code goes here
        return False

r = Solution().twin_in_proximity(arr, k)
print("true" if r else "false")
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
        public boolean twinInProximity(int[] arr, int k) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        boolean r = new Solution().twinInProximity(arr, k);
        System.out.println(r ? "true" : "false");
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "array", "placeholder": "[1, 2, 3, 4, 1]" },
    { "id": "k", "label": "k", "type": "number", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 1]", "k": "5" }, "expected": "true" },
    { "args": { "arr": "[1, 2, 3, 4, 5, 6, 1]", "k": "5" }, "expected": "false" },
    { "args": { "arr": "[1, 7]", "k": "5" }, "expected": "false" },
    { "args": { "arr": "[1, 2, 1]", "k": "1" }, "expected": "false" },
    { "args": { "arr": "[1, 2, 1]", "k": "2" }, "expected": "true" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "false" },
    { "args": { "arr": "[1, 1]", "k": "1" }, "expected": "true" }
  ]
}
```

<details>
<summary>Editorial</summary>

Map each value to its most recent index; before updating, check if the gap is `≤ k`. Cap the window at `k + 1` by deleting `arr[start]` once `end − start ≥ k`. Short-circuit `true` on first match; return `false` after the loop. `O(n)` time, `O(min(n, k))` space.

```python solution time=O(n) space=O(min(n,k))
import ast

class Solution:
    def twin_in_proximity(self, arr, k):
        element_index = {}
        start, end = 0, 0
        while end < len(arr):
            if arr[end] in element_index and end - element_index[arr[end]] <= k:
                return True
            element_index[arr[end]] = end
            if end - start >= k:
                del element_index[arr[start]]
                start += 1
            end += 1
        return False

arr = ast.literal_eval(input())
k = int(input())
r = Solution().twin_in_proximity(arr, k)
print("true" if r else "false")
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
        public boolean twinInProximity(int[] arr, int k) {
            Map<Integer, Integer> elementIndex = new HashMap<>();
            int start = 0, end = 0;
            while (end < arr.length) {
                if (elementIndex.containsKey(arr[end]) && end - elementIndex.get(arr[end]) <= k)
                    return true;
                elementIndex.put(arr[end], end);
                if (end - start >= k) {
                    elementIndex.remove(arr[start]);
                    start++;
                }
                end++;
            }
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        boolean r = new Solution().twinInProximity(arr, k);
        System.out.println(r ? "true" : "false");
    }
}
```

### Dry Run

Walk Example 1: `arr = [1, 2, 3, 4, 1]`, `k = 5`, expected output `true`. The map stores each value's most recent index:

```
end=0  arr=1   1 in map? no                          store {1:0}   end−start=0 ≥ 5? no
end=1  arr=2   2 in map? no                          store {1:0, 2:1}   1 ≥ 5? no
end=2  arr=3   3 in map? no                          store {…, 3:2}   2 ≥ 5? no
end=3  arr=4   4 in map? no                          store {…, 4:3}   3 ≥ 5? no
end=4  arr=1   1 in map (index 0)?  4 − 0 = 4 ≤ 5 → return true

result = true
```

### Complexity Analysis

| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | One pass; each step does a constant number of `O(1)` map operations (lookup, insert, and at most one delete). |
| **Space** | **O(min(N, k))** | The map holds at most `k + 1` live entries — the values currently in the window — capped by the array length. |

### Edge Cases

| Input | Output | Why |
|---|---|---|
| `arr = [1], k = 1` | `false` | A single element has no twin. |
| `arr = [1, 1], k = 1` | `true` | Equal adjacent values at distance `1 ≤ 1`. |
| `arr = [1, 2, 1], k = 1` | `false` | The two `1`s are distance `2 > 1` — out of range. |
| `arr = [1, 2, 1], k = 2` | `true` | Same array, wider budget — distance `2 ≤ 2` now qualifies. |
| `arr = [1, 2, 3, 4, 5, 6, 1], k = 5` | `false` | The only repeat is distance `6 > 5` — too far apart. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The variable-sized sliding window is the most flexible hash-table technique in this section. It handles a vast family of "find the longest/shortest contiguous something with property P" problems in **O(N)**, replacing nested-loop brute force with a single pass of two pointers.

Three lessons:

1. **Expand greedily, contract conditionally.** The right pointer always moves forward by one. The left pointer moves forward *only when the rule is violated*, and as far as needed to restore it. This asymmetry is what gives the algorithm its O(N) bound.
2. **`while`, not `if`.** Contract until the rule is satisfied — not just by one step. A single expansion can blow past the rule by many slots; the loop has to drain all of them before the next expansion.
3. **The map summarises the window.** Frequencies, distinct-counts, max-counts, sums — the map is whatever the rule needs to check in O(1). Pick the *smallest* summary that lets you decide expand-vs-contract; bigger summaries are wasted work.

> *Coming up — the **prefix-sum + hash** pattern. Sliding windows fail when the rule is non-monotonic (think arrays with negatives, or "exact sum equals K"). The prefix-sum trick rescues these problems by transforming "subarray sum" into "difference of two prefix sums" — and a hash map of prefix sums turns that into a single-pass O(N) algorithm. We saw a teaser in the subarray-sum-equals-k problem above; the next lesson opens the toolbox.*

Unlike the longest-window problems, the window here is *capped* at `k + 1` and the answer is a boolean — short-circuit `true` the moment an in-range duplicate appears. The map is keyed on value and stores the latest index, so each membership test is `O(1)`.

</details>
