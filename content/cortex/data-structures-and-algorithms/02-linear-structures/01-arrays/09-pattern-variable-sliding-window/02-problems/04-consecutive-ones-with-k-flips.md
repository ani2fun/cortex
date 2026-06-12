---
title: "Consecutive Ones with K Flips"
summary: "Given a binary array arr and a non-negative integer k, find and return the maximum number of consecutive 1s in the array if you can flip at most k 0s."
prereqs:
  - 09-pattern-variable-sliding-window/01-pattern
difficulty: hard
kind: problem
topics: [sliding-window, arrays]
---

# Consecutive Ones with K Flips

## Problem Statement

Given a binary array `arr` and a non-negative integer `k`, find and return the **maximum number of consecutive `1`s** in the array **if you can flip at most `k` `0`s**.

```
arr = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0],  k = 1   →  5    (flip the last 0 inside the run)
arr = [1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0],  k = 2   →  9    (flip 0s at indices 3 and 5)
arr = [0, 0, 0, 0],                        k = 2   →  2    (flip any 2 consecutive 0s)
```

---

## Examples

**Example 1**
```
Input:  arr = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0], k = 1
Output: 5
Explanation: The maximum number of consecutive 1s is 5 if we flip the
             last 0 between the two runs of 1s.
```

**Example 2**
```
Input:  arr = [1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0], k = 2
Output: 9
Explanation: The maximum number of consecutive 1s is 9 if we flip the 0s
             at indices 3 and 5.
```

**Example 3**
```
Input:  arr = [0, 0, 0, 0], k = 2
Output: 2
Explanation: The maximum number of consecutive 1s is 2 if we flip any 2
             consecutive 0s.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "arr = [1, 0, 1, 1, 0, 1], k = 1",
  "options": ["3", "4", "5", "6"],
  "answer": "4"
}
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- Each `arr[i]` is either `0` or `1`
- `0 ≤ k ≤ arr.length`

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def consecutive_ones_with_k_flips(self, arr: List[int], k: int) -> int:
        # Your code goes here — grow the window, shrink from the left while it
        # holds more than k zeros, and track the longest valid window length.
        return 0

arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().consecutive_ones_with_k_flips(arr, k))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int consecutiveOnesWithKFlips(int[] arr, int k) {
            // Your code goes here — grow the window, shrink from the left while it
            // holds more than k zeros, and track the longest valid window length.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().consecutiveOnesWithKFlips(arr, k));
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads the test case's arr
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "arr": "[1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]", "k": "1" }, "expected": "5" },
    { "args": { "arr": "[1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0]", "k": "2" }, "expected": "9" },
    { "args": { "arr": "[0, 0, 0, 0]", "k": "2" }, "expected": "2" },
    { "args": { "arr": "[1, 0, 1, 1, 0, 1]", "k": "1" }, "expected": "4" },
    { "args": { "arr": "[1, 0, 1]", "k": "0" }, "expected": "1" },
    { "args": { "arr": "[0, 0, 0]", "k": "3" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property generalises Consecutive Ones. There, the window had to contain zero `0`s; here it can contain up to `k`. The window invariant is **"the current window contains at most `k` zeros"** — within that budget every zero is mentally "flipped" to a `1`, so the entire window is a candidate run.

`end` walks forward and counts each `0` it picks up. `start` follows behind, advancing only when the zero count exceeds `k`. The aggregate is `count_zeros`. Unlike Consecutive Ones (a single leap) or Maximum Subarray Sum (also a leap), this problem needs a **`while`** on contraction: when `arr[end]` is the `(k+1)`-th zero, `start` must walk past one zero to eject it, which may require crossing several `1`s along the way. Each contraction is O(1), but several may happen per outer iteration — amortised to O(N) total because `start` never moves backwards.

The naive approach evaluates every subarray, counts its zeros, and asks whether that count is `≤ k` — O(N²) subarrays times O(N) counting equals O(N³). The single-pass version updates the zero count in O(1) on expansion and contraction. And the elegant special case: set `k = 0`, and this algorithm becomes **identical in behaviour** to Consecutive Ones — same invariant ("at most `0` zeros" = "all ones"), same answer. Consecutive Ones was a special case of this all along.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Consecutive Ones with K Flips |
|---|---|
| **Single-result over subarrays?** | Yes — return one integer: the length of the longest window containing at most `k` zeros. |
| **O(1) add to the aggregate?** | Yes — `count_zeros += 1` when `arr[end] == 0`. |
| **O(1) remove from the aggregate?** | Yes — `count_zeros -= 1` when stepping `start` past a `0`; no change when stepping past a `1`. |
| **Provable skipping?** | Yes — once the invariant fails, every window containing more than `k` zeros is invalid, so subarrays straddling the offending zero can be discarded by advancing `start` past it. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialize `start = 0`, `end = 0`, `count_zeros = 0`, `max_ones = 0`.
2. Loop while `end < len(arr)`:
   1. If `arr[end] == 0`, increment `count_zeros` by 1.
   2. While `count_zeros > k`: if `arr[start] == 0`, decrement `count_zeros`; then advance `start` by 1.
   3. Update `max_ones = max(max_ones, end - start + 1)`.
   4. Advance `end` by 1.
3. Return `max_ones`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(N) space=O(1)
import ast
from typing import List

class Solution:
    def consecutive_ones_with_k_flips(
        self, arr: List[int], k: int
    ) -> int:

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # To store the current count of 0s in the window
        count_zeros = 0

        # To store the maximum number of 1s in any subarray
        max_ones = 0

        # Move the window one step to the right until it reaches the end
        # of the array
        while end < len(arr):

            # Add contribution of arr[end]
            if arr[end] == 0:

                # Increment count of zeros
                count_zeros += 1

            # Process aggregate
            while count_zeros > k:

                # Remove contribution of arr[start] using the inverse
                # function
                if arr[start] == 0:

                    # Decrement count of zeros if we move past a zero
                    count_zeros -= 1

                # Contract window
                start += 1

            # Update the maximum number of consecutive ones seen so far
            max_ones = max(max_ones, end - start + 1)

            # Expand the window
            end += 1

        return max_ones


arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().consecutive_ones_with_k_flips(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int consecutiveOnesWithKFlips(int[] arr, int k) {

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // To store the current count of 0s in the window
            int countZeros = 0;

            // To store the maximum number of 1s in any subarray
            int maxOnes = 0;

            // Move the window one step to the right until it reaches the end
            // of the array
            while (end < arr.length) {

                // Add contribution of arr[end]
                if (arr[end] == 0) {

                    // Increment count of zeros
                    countZeros++;
                }

                // Process aggregate
                while (countZeros > k) {

                    // Remove contribution of arr[start] using the inverse
                    // function
                    if (arr[start] == 0) {

                        // Decrement count of zeros if we move past a zero
                        countZeros--;
                    }

                    // Contract window
                    start++;
                }

                // Update the maximum number of consecutive ones seen so far
                maxOnes = Math.max(maxOnes, end - start + 1);

                // Expand the window
                end++;
            }

            return maxOnes;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().consecutiveOnesWithKFlips(arr, k));
    }

    // "[1, 2, 3]" → {1, 2, 3} — reads the test case's arr
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


<details>
<summary><strong>Dry Run — arr = [1, 1, 0, 0, 1, 1, 1, 0, 1], k = 2</strong></summary>

```
start=0, zeros=0, max_len=0

end=0: arr[0]=1 | zeros=0 ≤ 2 ✓ | window=[0..0] len=1 | max_len=1
end=1: arr[1]=1 | zeros=0 ≤ 2 ✓ | window=[0..1] len=2 | max_len=2
end=2: arr[2]=0 → zeros=1 | 1 ≤ 2 ✓ | window=[0..2] len=3 | max_len=3
end=3: arr[3]=0 → zeros=2 | 2 ≤ 2 ✓ | window=[0..3] len=4 | max_len=4
end=4: arr[4]=1 | zeros=2 ≤ 2 ✓ | window=[0..4] len=5 | max_len=5
end=5: arr[5]=1 | zeros=2 ≤ 2 ✓ | window=[0..5] len=6 | max_len=6
end=6: arr[6]=1 | zeros=2 ≤ 2 ✓ | window=[0..6] len=7 | max_len=7
end=7: arr[7]=0 → zeros=3 | 3 > 2 → contract:
         arr[0]=1 → start=1 (zeros still 3)
         arr[1]=1 → start=2 (zeros still 3)
         arr[2]=0 → zeros=2, start=3 (stop — invariant restored)
       window=[3..7] len=5                              | max_len=7
end=8: arr[8]=1 | zeros=2 ≤ 2 ✓ | window=[3..8] len=6 | max_len=7

Return: 7 ✓   (flipping the two 0s at indices 2,3 gives a run of 7)

The multi-step contraction at end=7 is the 'while' in action — three
shrinks needed to eject one troublesome 0.
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | `end` advances `N` times; `start` also advances at most `N` times in total (amortised) |
| **Space** | O(1) | Three integers: `start`, `zeros`, `max_len` |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| `k = 0` | `arr=[1,0,1]`, `k=0` | `1` | Reduces to Consecutive Ones — any `0` forces a leap |
| `k ≥ number of zeros` | `arr=[0,1,0]`, `k=10` | `3` | Every zero fits within budget; window = entire array |
| All ones | `arr=[1,1,1]`, `k=2` | `3` | `zeros` never grows; `start` never moves |
| All zeros, `k=0` | `arr=[0,0,0]`, `k=0` | `0` | Every expansion breaks the invariant; `start` chases `end` forever |
| All zeros, `k ≥ n` | `arr=[0,0,0]`, `k=3` | `3` | Every zero fits; entire array is a valid window |
| Single zero, `k=1` | `arr=[0]`, `k=1` | `1` | One iteration; budget covers the lone zero |

</details>
<details>
<summary><h2>Comparison: The Four Problems at a Glance</h2></summary>


| Problem | Invariant | Contract with | Key insight |
|---|---|---|---|
| Consecutive Ones | Window has zero `0`s | Leap (`start = end + 1`) | One bad element voids every window touching it |
| Product Conundrum | Product `< k` | `while` loop | One big element can eject several small ones |
| Maximum Subarray Sum | Prefix sum `≥ 0` | Leap (`current = arr[end]`) | A negative prefix can never help — skip it whole |
| Consecutive Ones with K flips | Zero count `≤ k` | `while` loop | Budget-bounded tolerance — generalises Consecutive Ones |

The leap variants trade a potential slow shrink for a single jump; the `while` variants earn their amortised O(N) by only ever moving `start` forward. Every problem in this section is one of these two shapes.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Consecutive Ones with K flips is the capstone — a window that tolerates a bounded amount of "badness" and uses that tolerance to find a longer run. The `while`-loop contraction is the general-purpose tool; the single leap (seen in problems 1 and 3) is an optimised special case. If you can answer **"what is my invariant?"** and **"how do I restore it after a bad element enters?"**, every variable-window problem on the rest of this course — and most on any interview whiteboard — will yield to the same two-pointer dance.

> **Transfer Challenge:** Extend the solution to return the **actual flipped array** (the original with up to `k` zeros replaced by `1`s inside the best window). Can you do it without a second pass?
>
> <details><summary><strong>Solution hint</strong></summary>
>
> Track `best_start` and `best_end` alongside `max_len`. After the main loop, copy `arr`, then iterate `i` from `best_start` to `best_end` and set every `0` in that range to `1`. It is *technically* a second pass over the window, but not over the full array — the total work is still O(N).
>
> </details>

---

With the four problems complete, you have every move the variable-sized sliding window knows. The next section — **pattern interval merging** — takes the sliding idea off the raw array and applies it to a sorted list of intervals. The window gets replaced by a sweeping line, but the two-pointer shape you have internalised here stays exactly the same.

</details>
