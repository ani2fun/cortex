---
title: "Product Conundrum"
summary: "Given an array of positive integers arr and an integer k, find and return the number of contiguous subarrays where the product of all the elements in the subarray is strictly less than k."
prereqs:
  - 09-pattern-variable-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [sliding-window, arrays]
---

# Product Conundrum

## Problem Statement

Given an array of positive integers `arr` and an integer `k`, find and return the **number of contiguous subarrays** where the product of all the elements in the subarray is **strictly less than `k`**.

```
arr = [10, 5, 2, 6],  k = 100   →  8
arr = [10, 5],        k = 50    →  2
arr = [1],            k = 1     →  0
```

---

## Examples

**Example 1**
```
Input:  arr = [10, 5, 2, 6], k = 100
Output: 8
Explanation: The 8 subarrays whose product is less than 100 are:
             [10], [5], [2], [6], [10, 5], [5, 2], [2, 6], [5, 2, 6].
             Note that [10, 5, 2] is not included — its product 100 is
             not strictly less than k.
```

**Example 2**
```
Input:  arr = [10, 5], k = 50
Output: 2
Explanation: The 2 subarrays whose product is less than 50 are [10] and [5].
             Note that [10, 5] is not included — its product 50 is not
             strictly less than k.
```

**Example 3**
```
Input:  arr = [1], k = 1
Output: 0
Explanation: There are no subarrays whose product is less than k.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "arr = [2, 3, 4], k = 25",
  "options": ["3", "5", "6", "8"],
  "answer": "6"
}
```

## Constraints

- `0 ≤ arr.length ≤ 10^4`
- `1 ≤ arr[i] ≤ 1000` (positive integers only)
- `0 ≤ k ≤ 10^9`

```python run viz=array viz-root=arr
import ast
from typing import List

class Solution:
    def product_conundrum(self, arr: List[int], k: int) -> int:
        # Your code goes here — grow a window, divide arr[start] out while the
        # product reaches k, and add end - start + 1 valid subarrays each step.
        return 0

arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().product_conundrum(arr, k))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public int productConundrum(int[] arr, int k) {
            // Your code goes here — grow a window, divide arr[start] out while the
            // product reaches k, and add end - start + 1 valid subarrays each step.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().productConundrum(arr, k));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[10, 5, 2, 6]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "100" }
  ],
  "cases": [
    { "args": { "arr": "[10, 5, 2, 6]", "k": "100" }, "expected": "8" },
    { "args": { "arr": "[10, 5]", "k": "50" }, "expected": "2" },
    { "args": { "arr": "[1]", "k": "1" }, "expected": "0" },
    { "args": { "arr": "[2, 3, 4]", "k": "25" }, "expected": "6" },
    { "args": { "arr": "[1, 1, 1]", "k": "2" }, "expected": "6" },
    { "args": { "arr": "[100, 1, 1]", "k": "50" }, "expected": "3" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is **monotonic growth**. All elements are positive integers (≥ 1), so adding any element to the window can only keep the product the same (when that element is `1`) or grow it. The window invariant is therefore "product < `k`". Once the invariant breaks at the right edge, it can only be restored by shrinking from the left.

`end` walks the array; `start` follows behind. The aggregate is the running product of `arr[start..end]`. Whenever appending `arr[end]` pushes the product to `≥ k`, we contract — divide out `arr[start]` and advance `start` — and we may need to do this **several times in a row** before the product drops below `k` again. The right counting trick is the payoff: once the invariant holds for `arr[start..end]`, every subarray ending at `end` whose start is in `[start, end]` also satisfies it (shorter windows have smaller or equal products). That contributes exactly `end - start + 1` new valid subarrays this iteration.

The naive approach enumerates every subarray and multiplies its elements — O(N²) subarrays, O(N) multiply per check, O(N³) total. The single-pass version replaces the inner multiply with O(1) division on contraction and O(1) multiplication on expansion. The "several contractions in one step" cost amortises away: across the whole run, `start` advances at most `N` times total, so the inner `while` contributes O(N) total work, not O(N²).

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Product Conundrum |
|---|---|
| **Single-result over subarrays?** | Yes — return one integer: the count of subarrays whose product is `< k`. |
| **O(1) add to the aggregate?** | Yes — `product *= arr[end]` is one operation. |
| **O(1) remove from the aggregate?** | Yes — `product //= arr[start]` is one operation, exact because the elements are positive integers and `product` is a multiple of `arr[start]`. |
| **Provable skipping?** | Yes — if `product(arr[start..end]) ≥ k`, no superset starting at `start` can be `< k` (positive elements only grow the product), so all such supersets are safely discarded. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialize `start = 0`, `end = 0`, `product = 1`, `answer = 0`.
2. Loop while `end < len(arr)`:
   1. Multiply `arr[end]` into `product`.
   2. While `start <= end` and `product >= k`: divide `arr[start]` out of `product` and advance `start`.
   3. Add `end - start + 1` to `answer` — the count of valid subarrays ending at `end`.
   4. Advance `end` by 1.
3. Return `answer`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(N) space=O(1)
import ast
from typing import List

class Solution:
    def product_conundrum(self, arr: List[int], k: int) -> int:

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # Initialize product to 1
        product = 1

        # Initialize answer to 0
        answer = 0

        # Move the window one step to the right until it reaches the end
        # of the array
        while end < len(arr):

            # Add contribution of arr[end]
            product *= arr[end]

            # Process aggregate
            while start <= end and product >= k:

                # Remove contribution of arr[start] using the inverse
                # function
                product /= arr[start]

                # Contract window
                start += 1

            # Count valid subarrays
            answer += end - start + 1

            # Expand the window
            end += 1

        return answer


arr = ast.literal_eval(input())      # the test case's arr
k = int(input())                     # the test case's k
print(Solution().product_conundrum(arr, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int productConundrum(int[] arr, int k) {

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // Initialize product to 1
            int product = 1;

            // Initialize answer to 0
            int answer = 0;

            // Move the window one step to the right until it reaches the end
            // of the array
            while (end < arr.length) {

                // Add contribution of arr[end]
                product *= arr[end];

                // Process aggregate
                while (start <= end && product >= k) {

                    // Remove contribution of arr[start] using the inverse
                    // function
                    product /= arr[start];

                    // Contract window
                    start++;
                }

                // Count valid subarrays
                answer += end - start + 1;

                // Expand the window
                end++;
            }

            return answer;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().productConundrum(arr, k));
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
<summary><strong>Dry Run — arr = [10, 5, 2, 6], k = 100</strong></summary>

```
start=0, end=0, product=1, answer=0

end=0: product *= 10 → 10. 10 < 100 — no contract.
       answer += (0 - 0 + 1) = 1 → answer = 1.   subarray [10]
end=1: product *= 5 → 50. 50 < 100 — no contract.
       answer += (1 - 0 + 1) = 2 → answer = 3.   subarrays [5], [10, 5]
end=2: product *= 2 → 100. 100 ≥ 100 — contract:
         product //= 10 → 10, start=1. 10 < 100 — stop.
       answer += (2 - 1 + 1) = 2 → answer = 5.   subarrays [2], [5, 2]
end=3: product *= 6 → 60. 60 < 100 — no contract.
       answer += (3 - 1 + 1) = 3 → answer = 8.   subarrays [6], [2, 6], [5, 2, 6]

Return: 8 ✓
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | Each index is visited at most twice overall — once by `end`, once by `start`. The inner `while` is amortised |
| **Space** | O(1) | Four integers: `start`, `end`, `product`, `answer` |

The amortised argument matters: the inner `while` looks quadratic, but across the *whole* run, `start` moves forward at most `N` times total, so the sum of all inner iterations is bounded by `N`.

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| `k ≤ 1` | `arr=[1, 2, 3]`, `k=1` | `0` | No product of positive ints can be strictly less than 1 |
| Single element `≥ k` | `arr=[10]`, `k=5` | `0` | Inner contract empties window; nothing to count |
| All elements `< k` | `arr=[1, 1, 1]`, `k=2` | `6` | Every non-empty subarray valid: 3 + 2 + 1 = 6 |
| Empty array | `arr=[]`, `k=10` | `0` | Loop never executes |
| Product exactly equals `k` | `arr=[2, 5]`, `k=10` | `2` | `2 * 5 = 10` not strictly less — only `[2]` and `[5]` count |
| Large single element | `arr=[100, 1, 1]`, `k=50` | `3` | `start` leaps past `100`; the valid windows are `[1]`, `[1]`, and `[1, 1]` |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Product Conundrum is the first problem in this section where contraction is a `while` loop, not a single leap — one new element can force several shrinks before the invariant holds — and the aggregate is a **count of subarrays**, calculated as `end - start + 1` because every shorter window ending at `end` is also valid.

</details>
