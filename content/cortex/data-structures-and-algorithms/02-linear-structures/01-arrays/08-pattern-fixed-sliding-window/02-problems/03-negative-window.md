---
title: "Negative Window"
summary: "Given an integer array arr and a positive integer k, return the count of negative numbers in every subarray of size k — one count per window position, in order."
prereqs:
  - 08-pattern-fixed-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [sliding-window, arrays]
---

# Negative Window

## The Problem

Given an integer array `arr` and a positive integer `k`, return the **count of negative numbers in every subarray of size `k`** — one count per window position, in order.

```
arr = [4, -4, 5, -1, 4],  k = 3  →  [1, 2, 1]
arr = [1, -2, 3, -5],     k = 1  →  [0, 1, 0, 1]
arr = [-1, -2, 3, -5],    k = 4  →  [3]
```

---

## Examples

**Example 1**
```
Input:  arr = [4, -4, 5, -1, 4], k = 3
Output: [1, 2, 1]
Explanation: The count of negative numbers in each subarray of size 3 —
             [4, -4, 5] has 1 negative; [-4, 5, -1] has 2; [5, -1, 4] has 1.
```

**Example 2**
```
Input:  arr = [1, -2, 3, -5], k = 1
Output: [0, 1, 0, 1]
Explanation: Each window of size 1 is one element; the count is 1 if that
             element is negative, 0 otherwise.
```

**Example 3**
```
Input:  arr = [-1, -2, 3, -5], k = 4
Output: [3]
Explanation: One window — the whole array — with 3 negatives.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "arr = [-1, 2, -3, -4, 5], k = 2",
  "options": ["[1, 1, 2, 1]", "[1, 2, 2, 1]", "[1, 1, 1, 1]", "[2, 1, 2, 1]"],
  "answer": "[1, 1, 2, 1]"
}
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- `1 ≤ k ≤ 10^5`
- `-10^9 ≤ arr[i] ≤ 10^9`

```python run viz=array viz-root=result
import ast
from typing import List

class Solution:
    def negative_window(self, arr: List[int], k: int) -> List[int]:
        # Your code goes here — slide a fixed window of size k and append the
        # count of negatives in each full window to the result list.
        return []

arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().negative_window(arr, k))
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> negativeWindow(int[] arr, int k) {
            // Your code goes here — slide a fixed window of size k and append the
            // count of negatives in each full window to the result list.
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().negativeWindow(arr, k));
    }

    // "[1, -2, 3]" → {1, -2, 3} — reads the test case's arr
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[4, -4, 5, -1, 4]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "arr": "[4, -4, 5, -1, 4]", "k": "3" }, "expected": "[1, 2, 1]" },
    { "args": { "arr": "[-1, 2, -3, -4, 5]", "k": "2" }, "expected": "[1, 1, 2, 1]" },
    { "args": { "arr": "[1, -2, 3, -5]", "k": "1" }, "expected": "[0, 1, 0, 1]" },
    { "args": { "arr": "[-1, -2, -3, -4]", "k": "3" }, "expected": "[3, 3]" },
    { "args": { "arr": "[1, 2, 3, 4]", "k": "2" }, "expected": "[0, 0, 0]" },
    { "args": { "arr": "[-5]", "k": "1" }, "expected": "[1]" }
  ]
}
```

<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Question | Answer for Negative Window |
|---|---|
| **Q1.** Fixed size subarray? | **Yes** — every window has exactly `k` elements; the question is asked once per valid window position. |
| **Q2.** O(1) add and remove? | **Yes** — a single sign check (`arr[end] < 0`) and a conditional `negative_count += 1`. The remove side mirrors it. |
| **Q3.** Per-window report or single best? | **Per-window report** — the answer is a list of `n − k + 1` counts, one per window. The `process` step appends `negative_count` to `result`. |
| **Q4.** Edge cases defined? | **Yes** — for `n < k`, no full window exists so `result` is empty; `k == n` produces one entry; `k == 1` produces one count per element (0 or 1). |

</details>
<details>
<summary><h2>Intuition</h2></summary>


The structural property is identical to Maximum Ones: count a predicate (`< 0` here, `== 1` there) over contiguous slices of fixed size `k`. What changes is the *output shape* — the problem wants the count for **every** window, not only the maximum, so the answer is a list of `n − k + 1` integers. The window mechanics and the per-slide cost are unchanged.

The two pointers are `start` and `end`, with the aggregate held in a single integer `negative_count`. When `end` advances, the counter increments by `1` only if `arr[end] < 0`; when `start` advances on a contraction, it decrements by `1` only if `arr[start] < 0`. Non-negative elements entering or leaving are no-ops. The per-slide cost remains O(1) — one sign check per side.

What breaks if you use the naive nested-loop approach is the same O(N × k) cost as before: each window re-scans `k` elements for the sign check, despite sharing `k − 1` of them with the previous window. The sliding window saves that wasted work and runs in O(N) time, with the only extra space being the O(N − k + 1) result list that the problem itself demands.

```d2
direction: right

w1: "Window [-1, 2, -3]: neg_count=2" {
  grid-columns: 5
  grid-gap: 0
  a0: "-1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a1: "2" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a2: "-3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a3: "4"
  a4: "-5"
}

w2: "Window [2, -3, 4]: neg_count=1" {
  grid-columns: 5
  grid-gap: 0
  b0: "-1 ✗" {style.fill: "#f1f5f9"; style.stroke: "#94a3b8"}
  b1: "2" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b2: "-3" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b3: "4" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b4: "-5"
}

w3: "Window [-3, 4, -5]: neg_count=2" {
  grid-columns: 5
  grid-gap: 0
  c0: "-1 ✗" {style.fill: "#f1f5f9"; style.stroke: "#94a3b8"}
  c1: "2 ✗" {style.fill: "#f1f5f9"; style.stroke: "#94a3b8"}
  c2: "-3" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  c3: "4" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  c4: "-5" {style.fill: "#fde68a"; style.stroke: "#d97706"}
}

w1 -> w2: "remove -1 (-1), add 4 (+0) → neg=1"
w2 -> w3: "remove 2 (+0), add -5 (+1) → neg=2"
```

<p align="center"><strong>Each slide removes the outgoing element's contribution and adds the incoming one. The count updates in O(1) — one conditional check per side per step.</strong></p>

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Approach

1. **Initialise the window state.** Set `start = 0`, `end = 0`, `negative_count = 0`, and `result = []` (the per-window report buffer).
2. **Loop while `end < len(arr)` and apply the four-step template.**
3. **Step 3.1 — Expand.** If `arr[end] < 0`, increment `negative_count`. Otherwise leave it unchanged.
4. **Step 3.2 — Contract if oversized.** If `end − start + 1 > k`, check `arr[start]`: if it is negative, decrement `negative_count`. Then advance `start` by one.
5. **Step 3.3 — Process if full.** If `end − start + 1 == k`, append `negative_count` to `result`.
6. **Step 3.4 — Advance.** Increment `end` by one and continue.
7. **Return the result.** After the loop, `result` holds exactly `n − k + 1` counts — one per window position, in left-to-right order.

### Solution

```python solution time=O(N) space=O(N)
import ast
from typing import List

class Solution:
    def negative_window(self, arr: List[int], k: int) -> List[int]:

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # To store the current count of negative numbers in the window
        negative_count = 0

        # To store the count of negative numbers in subarrays of size k
        result: List[int] = []

        # Loop through the array
        while end < len(arr):

            # Add the current element if it is negative
            if arr[end] < 0:
                negative_count += 1

            # If the current subarray has more than k elements
            # then shrink it from the start
            if end - start + 1 > k:

                # Remove the contribution of arr[start]
                if arr[start] < 0:
                    negative_count -= 1

                # Move the start pointer forward
                start += 1

            # If the current subarray has exactly k elements
            # then add the count to the result
            if end - start + 1 == k:
                result.append(negative_count)

            # Move the end pointer forward
            end += 1

        return result


arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().negative_window(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> negativeWindow(int[] arr, int k) {

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // To store the current count of negative numbers in the window
            int negativeCount = 0;

            // To store the count of negative numbers in subarrays of size k
            List<Integer> result = new ArrayList<>();

            // Loop through the array
            while (end < arr.length) {

                // Add the current element if it is negative
                if (arr[end] < 0) {
                    negativeCount++;
                }

                // If the current subarray has more than k elements
                // then shrink it from the start
                if (end - start + 1 > k) {

                    // Remove the contribution of arr[start]
                    if (arr[start] < 0) {
                        negativeCount--;
                    }

                    // Move the start pointer forward
                    start++;
                }

                // If the current subarray has exactly k elements
                // then add the count to the result
                if (end - start + 1 == k) {
                    result.add(negativeCount);
                }

                // Move the end pointer forward
                end++;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().negativeWindow(arr, k));
    }

    // "[1, -2, 3]" → {1, -2, 3} — reads the test case's arr
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

### Dry Run — Example 1

`arr = [4, -4, 5, -1, 4]`, `k = 3`

<details>
<summary><strong>Trace — arr = [4, -4, 5, -1, 4],  k = 3</strong></summary>

```
start=0, end=0, negative_count=0, result=[]

end=0: ① arr[0]=4  ≥ 0 → neg=0. size=1, not k.
end=1: ① arr[1]=-4 < 0 → neg=1. size=2, not k.
end=2: ① arr[2]=5  ≥ 0 → neg=1. ③ size=3==k → result=[1].
end=3: ① arr[3]=-1 < 0 → neg=2. ② size=4>k → arr[0]=4 ≥ 0 → neg=2, start=1.
       ③ size=3==k → result=[1, 2].
end=4: ① arr[4]=4  ≥ 0 → neg=2. ② size=4>k → arr[1]=-4 < 0 → neg=1, start=2.
       ③ size=3==k → result=[1, 2, 1].
end=5: end >= n=5 → loop exits.

Return: [1, 2, 1] ✓

Window [4, -4, 5]  → 1 negative (-4)
Window [-4, 5, -1] → 2 negatives (-4 and -1)
Window [5, -1, 4]  → 1 negative (-1)
```

</details>

### Result Size

The result always has exactly `n − k + 1` elements — one per valid window. For `n=5, k=3`: `5 − 3 + 1 = 3` windows, 3 results. This is consistent with all fixed sliding window "report per window" problems.

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | `end` visits each element once; `start` moves at most N times total |
| **Space** | O(N − k + 1) | The result array holds one entry per window; O(1) working space beyond that |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| No negatives | `[1, 2, 3, 4]`, k=2 | `[0, 0, 0]` | All counts are zero |
| All negatives | `[-1, -2, -3]`, k=2 | `[2, 2]` | Every window is all negative |
| k == n | `[-1, 2, -3]`, k=3 | `[2]` | One window: the whole array, 2 negatives |
| k == 1 | `[1, -2, 3, -5]`, k=1 | `[0, 1, 0, 1]` | Each element is its own window |
| Single element positive | `[5]`, k=1 | `[0]` | One window, zero negatives |
| Single element negative | `[-5]`, k=1 | `[1]` | One window, one negative |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Negative Window is the first **per-window report** variant of the pattern: the `process` step appends to a `result` list instead of comparing against a running extremum. Everything else — the aggregate update, the size checks, the pointer motion — is identical to Maximum Ones.

</details>
