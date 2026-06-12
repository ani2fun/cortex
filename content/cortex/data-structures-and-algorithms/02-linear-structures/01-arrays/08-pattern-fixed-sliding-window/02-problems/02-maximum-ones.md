---
title: "Maximum Ones"
summary: "Given a binary array arr (containing only 0s and 1s) and a positive integer k, find the maximum number of 1s among all subarrays of size k."
prereqs:
  - 08-pattern-fixed-sliding-window/01-pattern
difficulty: easy
kind: problem
topics: [sliding-window, arrays]
---

# Maximum Ones

## The Problem

Given a **binary** array `arr` (containing only `0`s and `1`s) and a positive integer `k`, find the **maximum number of `1`s** among all subarrays of size `k`.

```
arr = [1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0],  k = 5  →  4
arr = [1, 1, 0, 1, 0, 1, 1, 0, 1, 0],     k = 4  →  3
arr = [0, 0, 0],                          k = 2  →  0
```

---

## Examples

**Example 1**
```
Input:  arr = [1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0], k = 5
Output: 4
Explanation: The maximum number of 1s among all subarrays of size 5 is 4 —
             the window arr[0..4] = [1, 1, 1, 0, 1] has four 1s.
```

**Example 2**
```
Input:  arr = [1, 1, 0, 1, 0, 1, 1, 0, 1, 0], k = 4
Output: 3
Explanation: The maximum number of 1s among all subarrays of size 4
             is 3 (e.g. the window [1, 0, 1, 1] at indices 4..7).
```

**Example 3**
```
Input:  arr = [0, 0, 0], k = 2
Output: 0
Explanation: There are no 1s anywhere, so every size-2 window has zero.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "arr = [1, 0, 1, 1, 0, 1, 1, 1], k = 3",
  "options": ["3", "2", "4", "1"],
  "answer": "3"
}
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- `1 ≤ k ≤ 10^5`
- `arr[i]` is either `0` or `1`

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def maximum_ones(self, arr: List[int], k: int) -> int:
        # Your code goes here — slide a fixed window of size k, tracking the
        # maximum count of 1s in any window.
        return 0

arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().maximum_ones(arr, k))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int maximumOnes(int[] arr, int k) {
            // Your code goes here — slide a fixed window of size k, tracking the
            // maximum count of 1s in any window.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().maximumOnes(arr, k));
    }

    // "[1, 0, 1]" → {1, 0, 1} — reads the test case's arr
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "arr": "[1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0]", "k": "5" }, "expected": "4" },
    { "args": { "arr": "[1, 0, 1, 1, 0, 1, 1, 1]", "k": "3" }, "expected": "3" },
    { "args": { "arr": "[0, 0, 0]", "k": "2" }, "expected": "0" },
    { "args": { "arr": "[1, 1, 1]", "k": "3" }, "expected": "3" },
    { "args": { "arr": "[1, 0, 1, 0, 1]", "k": "2" }, "expected": "1" },
    { "args": { "arr": "[0, 1]", "k": "5" }, "expected": "0" }
  ]
}
```

<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Question | Answer for Maximum Ones |
|---|---|
| **Q1.** Fixed size subarray? | **Yes** — every window has exactly `k` elements, identical to Subarray Size Equals K. |
| **Q2.** O(1) add and remove? | **Yes** — a single equality check (`arr[end] == 1`) and a conditional `count_ones += 1`. The remove side mirrors it. |
| **Q3.** Per-window report or single best? | **Single best** — only the maximum count across all windows is returned, so `process` updates a running `max_ones`. |
| **Q4.** Edge cases defined? | **Yes** — `k > n` is not specified by the problem, so the loop never produces a full window and returns `0`; `k == n` is one window over the whole array; `k == 1` returns `1` if any element is `1`. |

</details>
<details>
<summary><h2>Intuition &amp; Brute Force</h2></summary>

### Intuition

The structural property that fits the pattern is that the input is a binary array of `0`s and `1`s, and the aggregate of interest is the **count of `1`s** inside a contiguous slice of length exactly `k`. Counting is monoidal over disjoint slices: the count of a window's `1`s equals the count from the left half plus the count from the right half. That decomposability is what makes the aggregate incrementally updatable.

The two pointers are still `start` and `end`, but the aggregate is a single integer counter `count_ones` rather than a numeric sum. When `end` advances, the aggregate changes by `+1` only if `arr[end] == 1`; when `start` advances on a contraction, it changes by `−1` only if `arr[start] == 1`. Zeros entering or leaving do not change the counter, so the per-slide cost is a single conditional integer update — O(1).

What breaks if you use the naive nested-loop approach is the same shared-work problem as before: adjacent windows share `k − 1` elements, but the brute force re-scans every element on every window for the `== 1` check. The cost is O(N × k); for `n = 10⁶, k = 1000`, that's a thousand-fold slowdown over the O(N) sliding-window solution.

```d2
direction: right

w1: "Window [1,0,1,1]: ones=3" {
  grid-columns: 8
  grid-gap: 0
  a0: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a1: "0" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a2: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a3: "1" {style.fill: "#fde68a"; style.stroke: "#d97706"}
  a4: "0"
  a5: "1"
  a6: "1"
  a7: "0"
}

w2: "Window [0,1,1,0]: ones=2" {
  grid-columns: 8
  grid-gap: 0
  b0: "1 ✗" {style.fill: "#f1f5f9"; style.stroke: "#94a3b8"}
  b1: "0" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b2: "1" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b3: "1" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b4: "0" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}
  b5: "1"
  b6: "1"
  b7: "0"
}

w1 -> w2: "remove arr[0]=1 (-1), add arr[4]=0 (+0) → ones=2"
```

<p align="center"><strong>Sliding the window right: remove the outgoing element's contribution to the ones count, add the incoming element's contribution. The max ones count is tracked across all windows.</strong></p>

### Brute Force: Nested Loops, O(N × k)

```python run viz=array
from typing import List

def max_ones_brute(arr: List[int], k: int) -> int:
    n = len(arr)
    max_ones = 0
    for i in range(n - k + 1):
        ones = 0
        for j in range(k):
            if arr[i + j] == 1:
                ones += 1
        max_ones = max(max_ones, ones)
    return max_ones

print(max_ones_brute([1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0], 5))  # 4
print(max_ones_brute([1, 1, 0, 1, 0, 1, 1, 0, 1, 0], 4))     # 3
print(max_ones_brute([0, 0, 0], 2))                           # 0
```

```java run viz=array
public class Main {
    static int maxOnesBrute(int[] arr, int k) {
        int n = arr.length, maxOnes = 0;
        for (int i = 0; i <= n - k; i++) {
            int ones = 0;
            for (int j = 0; j < k; j++) if (arr[i + j] == 1) ones++;
            maxOnes = Math.max(maxOnes, ones);
        }
        return maxOnes;
    }

    public static void main(String[] args) {
        System.out.println(maxOnesBrute(new int[]{1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0}, 5));  // 4
        System.out.println(maxOnesBrute(new int[]{1, 1, 0, 1, 0, 1, 1, 0, 1, 0}, 4));     // 3
        System.out.println(maxOnesBrute(new int[]{0, 0, 0}, 2));                           // 0
    }
}
```

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Approach

1. **Initialise the window state.** Set `start = 0`, `end = 0`, `count_ones = 0` (the running aggregate), and `max_ones = 0` (the running maximum — `0` is the trivial lower bound).
2. **Loop while `end < len(arr)` and apply the four-step template.**
3. **Step 3.1 — Expand.** If `arr[end] == 1`, increment `count_ones`. Otherwise leave it unchanged.
4. **Step 3.2 — Contract if oversized.** If `end − start + 1 > k`, check `arr[start]`: if it is `1`, decrement `count_ones`. Then advance `start` by one.
5. **Step 3.3 — Process if full.** If `end − start + 1 == k`, update `max_ones = max(max_ones, count_ones)`.
6. **Step 3.4 — Advance.** Increment `end` by one and continue.
7. **Return the result.** After the loop, `max_ones` holds the largest count of `1`s seen in any window of size `k`.

### Solution

```python solution time=O(N) space=O(1)
import ast
from typing import List

class Solution:
    def maximum_ones(self, arr: List[int], k: int) -> int:

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # To store the current count of 1s in the window
        count_ones = 0

        # To store the maximum number of 1s in any subarray of size k
        max_ones = 0

        # Loop through the array
        while end < len(arr):

            # Add the current element to the count if it's 1
            if arr[end] == 1:
                count_ones += 1

            # If the current subarray has more than k elements
            # then shrink it from the start
            if end - start + 1 > k:

                # Remove the contribution of arr[start] if it was 1
                if arr[start] == 1:
                    count_ones -= 1

                # Move the start pointer forward
                start += 1

            # If the current subarray has exactly k elements
            # then update the maximum number of 1s
            if end - start + 1 == k:
                max_ones = max(max_ones, count_ones)

            # Move the end pointer forward
            end += 1

        return max_ones


arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().maximum_ones(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int maximumOnes(int[] arr, int k) {

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // To store the current count of 1s in the window
            int countOnes = 0;

            // To store the maximum number of 1s in any subarray of size k
            int maxOnes = 0;

            // Loop through the array
            while (end < arr.length) {

                // Add the current element to the count if it's 1
                if (arr[end] == 1) {
                    countOnes++;
                }

                // If the current subarray has more than k elements
                // then shrink it from the start
                if (end - start + 1 > k) {

                    // Remove the contribution of arr[start] if it was 1
                    if (arr[start] == 1) {
                        countOnes--;
                    }

                    // Move the start pointer forward
                    start++;
                }

                // If the current subarray has exactly k elements
                // then update the maximum number of 1s
                if (end - start + 1 == k) {
                    maxOnes = Math.max(maxOnes, countOnes);
                }

                // Move the end pointer forward
                end++;
            }

            return maxOnes;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().maximumOnes(arr, k));
    }

    // "[1, 0, 1]" → {1, 0, 1} — reads the test case's arr
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

`arr = [1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0]`, `k = 5`

<details>
<summary><strong>Trace — arr = [1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0],  k = 5</strong></summary>

```
start=0, end=0, count_ones=0, max_ones=0

end=0:  ① arr[0]=1 → count=1.  size=1 < k.
end=1:  ① arr[1]=1 → count=2.  size=2 < k.
end=2:  ① arr[2]=1 → count=3.  size=3 < k.
end=3:  ① arr[3]=0 → count=3.  size=4 < k.
end=4:  ① arr[4]=1 → count=4.  size=5 = k → max_ones = 4.   ← window [1,1,1,0,1]
end=5:  ① arr[5]=0 → count=4.  ② size=6 > k → arr[0]=1 → count=3, start=1.  size=5 = k → max stays 4.
end=6:  ① arr[6]=0 → count=3.  ② size=6 > k → arr[1]=1 → count=2, start=2.  size=5 = k → max stays 4.
end=7:  ① arr[7]=1 → count=3.  ② size=6 > k → arr[2]=1 → count=2, start=3.  size=5 = k → max stays 4.
end=8:  ① arr[8]=1 → count=3.  ② size=6 > k → arr[3]=0 → count=3, start=4.  size=5 = k → max stays 4.
end=9:  ① arr[9]=1 → count=4.  ② size=6 > k → arr[4]=1 → count=3, start=5.  size=5 = k → max stays 4.
end=10: ① arr[10]=0 → count=3. ② size=6 > k → arr[5]=0 → count=3, start=6.  size=5 = k → max stays 4.
end=11: end >= n=11 → loop exits.

Return: 4 ✓

The best window is the very first one — [1, 1, 1, 0, 1] at indices 0..4 with four 1s.
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | `end` visits each element once; `start` moves at most N times total |
| **Space** | O(1) | Two pointer variables plus a count variable |

Versus brute force O(N × k) — the count updates in O(1) per slide because only one element enters and one leaves.

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| All zeros | `[0, 0, 0]`, k=2 | `0` | No 1s anywhere — max stays 0 |
| All ones | `[1, 1, 1, 1]`, k=3 | `3` | Every window is full of 1s |
| k == n | `[1, 0, 1]`, k=3 | `2` | One window: the whole array |
| k == 1 | `[1, 0, 1, 0]`, k=1 | `1` | Best single-element window is a 1 |
| Single element | `[1]`, k=1 | `1` | One window, one 1 |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Maximum Ones generalises Subarray Size Equals K by swapping the scalar `sum` aggregate for a **conditional counter**: each element contributes `0` or `1` based on a predicate (`== 1`), and the per-slide update becomes one branch instead of one arithmetic op.

</details>
