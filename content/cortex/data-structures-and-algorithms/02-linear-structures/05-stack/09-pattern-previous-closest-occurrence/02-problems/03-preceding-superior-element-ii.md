---
title: "Preceding Superior Element II"
summary: "Same as preceding superior element, but the array is circular — when looking for a preceding greater you may wrap around past the start to the end of the array. If no greater exists even after a full circle, return -1."
prereqs:
  - 09-pattern-previous-closest-occurrence/01-pattern
difficulty: medium
kind: problem
topics: [previous-closest-occurrence, stack]
---

# Preceding superior element II

## Problem Statement

Given a circular array `arr`, return for each position the **preceding superior element** — the nearest strictly-greater value to its left, allowing the search to wrap around past the start of the array. If no greater exists even after a full circle, return `-1`.

### Example 1
> -   **Input:** `arr = [2, 5, 1, 6, 10, 3]`
> -   **Output:** `[3, 10, 5, 10, -1, 10]`

### Example 2
> -   **Input:** `arr = [6, 7, 8, 9, 8]`
> -   **Output:** `[8, 8, 9, -1, 9]`

## Examples

**Example 1**
```
Input:  arr = [2, 5, 1, 6, 10, 3]
Output: [3, 10, 5, 10, -1, 10]
Explanation: 2 finds nothing greater to its left until the wrap reaches 3 → 3.
5 wraps to 10 → 10. 1 sees 5 directly → 5. 6 wraps to 10 → 10.
10 is the global maximum → -1 even after a full circle. 3 sees 10 → 10.
```

**Example 2**
```
Input:  arr = [6, 7, 8, 9, 8]
Output: [8, 8, 9, -1, 9]
Explanation: 6 and 7 both wrap to the trailing 8 → 8. 8 wraps to 9 → 9.
9 is the maximum → -1. The trailing 8 sees 9 directly → 9.
```

**Example 3**
```
Input:  arr = [5, 4, 3, 2, 1]
Output: [-1, 5, 4, 3, 2]
Explanation: 5 is the maximum → -1. Each later value sees the one just before it,
strictly greater, so no wrap is needed.
```

**Example 4**
```
Input:  arr = [5, 5, 5]
Output: [-1, -1, -1]
Explanation: Equal values are never strictly greater, so the pop test removes them all.
```

## Constraints

- `1 ≤ arr.length ≤ 1000`
- `1 ≤ arr[i] ≤ 10^4`

```python run
import ast
from typing import List

class Solution:
    def preceding_superior_element_ii(self, arr: List[int]) -> List[int]:
        # Your code goes here — iterate 2*n times with i % n indexing
        # to simulate circular "previous greater" for each position.
        return [-1] * len(arr)

arr = ast.literal_eval(input())
print(Solution().preceding_superior_element_ii(arr))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingSuperiorElementII(int[] arr) {
            // Your code goes here — iterate 2*n times with i % n indexing
            // to simulate circular "previous greater" for each position.
            int[] result = new int[arr.length];
            Arrays.fill(result, -1);
            return result;
        }
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().precedingSuperiorElementII(arr)));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[2, 5, 1, 6, 10, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[2, 5, 1, 6, 10, 3]" }, "expected": "[3, 10, 5, 10, -1, 10]" },
    { "args": { "arr": "[6, 7, 8, 9, 8]" },      "expected": "[8, 8, 9, -1, 9]" },
    { "args": { "arr": "[1]" },                   "expected": "[-1]" },
    { "args": { "arr": "[5, 5, 5]" },             "expected": "[-1, -1, -1]" },
    { "args": { "arr": "[1, 2]" },                "expected": "[2, -1]" },
    { "args": { "arr": "[3, 1, 2]" },             "expected": "[-1, 3, 3]" },
    { "args": { "arr": "[5, 4, 3, 2, 1]" },       "expected": "[-1, 5, 4, 3, 2]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is the same previous-greater query, but the array is now **circular** — a value's nearest strictly-greater predecessor may live to its right, reachable only by wrapping past the start. That single change is what separates this from the linear superior problem; the monotonic stack itself is untouched.

The trick is to **linearise the circle by iterating `2n` indices** with `i % n` indexing. Each original position is processed twice: once on the natural left-to-right pass and once after a full wrap. The stack still holds a strictly decreasing chain of previous-greater candidates; the second lap lets a wrapped-around value finally find its predecessor. Because every element is visited exactly twice, the work stays `O(N)`.

The naive circular approach breaks the time budget badly. For each index you would scan up to `n` other positions wrapping around — `O(N²)` time and easy to get wrong at the wrap boundary. The doubled-pass stack keeps the single-sweep `O(N)` guarantee and handles the wrap by construction, never special-casing the seam between the end and the start.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Preceding Superior Element II |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *before* it? | **Yes** — but "before" wraps circularly, so the search may continue past the start. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-greater value reachable going left-then-wrapping. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict greater-than test drives every pop (decreasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — `2n` iterations, each value pushed and popped at most once across the doubled pass. |

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>


A circular array can be linearised by **iterating over `2n` indices**, mapping each index `i` to `i % n`. Each element gets two chances at finding its preceding greater — once on the "natural" pass and once with the wrap-around in play. Because every original element is processed twice, the time is still O(N).

```python solution time=O(N) space=O(N)
import ast
from typing import List

class Solution:
    def preceding_superior_element_ii(self, arr: List[int]) -> List[int]:
        n = len(arr)
        result = [-1] * n

        # Stack to store elements
        stack = []

        # Iterate twice through the array (circularly)
        for i in range(2 * n):

            # Circular index
            index = i % n
            num = arr[index]

            # Check if we can pop elements from the stack
            # (i.e., find the preceding greater element)
            while stack and stack[-1] <= num:
                stack.pop()

            # If stack is not empty, the top element is the preceding
            # superior element
            if stack:
                result[index] = stack[-1]

            # Always push the element to the stack
            stack.append(num)

        return result


arr = ast.literal_eval(input())
print(Solution().preceding_superior_element_ii(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingSuperiorElementII(int[] arr) {
            int n = arr.length;
            int[] result = new int[n];
            Arrays.fill(result, -1);

            // Stack to store elements
            Deque<Integer> stack = new ArrayDeque<>();

            // Iterate twice through the array (circularly)
            for (int i = 0; i < 2 * n; i++) {

                // Circular index
                int index = i % n;
                int num = arr[index];

                // Check if we can pop elements from the stack
                // (i.e., find the preceding greater element)
                while (!stack.isEmpty() && stack.peek() <= num) {
                    stack.pop();
                }

                // If stack is not empty, the top element is the preceding
                // superior element
                if (!stack.isEmpty()) {
                    result[index] = stack.peek();
                }

                // Always push the element to the stack
                stack.push(num);
            }

            return result;
        }
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().precedingSuperiorElementII(arr)));
    }
}
```

### Dry Run

Walk Example 1 — `arr = [2, 5, 1, 6, 10, 3]`, `n = 6`, so `12` iterations. Decreasing stack, pop while the top `≤ num`. The first six steps fill direct answers; the last six wrap and finish the rest:

```
i= 0 idx0 num=2    pop none          empty   res[0]=-1   push 2   [2]
i= 1 idx1 num=5    pop 2             empty   res[1]=-1   push 5   [5]
i= 2 idx2 num=1    pop none (5>1)    top=5   res[2]=5    push 1   [5,1]
i= 3 idx3 num=6    pop 1, pop 5      empty   res[3]=-1   push 6   [6]
i= 4 idx4 num=10   pop 6             empty   res[4]=-1   push 10  [10]
i= 5 idx5 num=3    pop none (10>3)   top=10  res[5]=10   push 3   [10,3]
i= 6 idx0 num=2    pop none (3>2)    top=3   res[0]=3    push 2   [10,3,2]
i= 7 idx1 num=5    pop 2, pop 3      top=10  res[1]=10   push 5   [10,5]
i= 8 idx2 num=1    pop none (5>1)    top=5   res[2]=5    push 1   [10,5,1]
i= 9 idx3 num=6    pop 1, pop 5      top=10  res[3]=10   push 6   [10,6]
i=10 idx4 num=10   pop 6, pop 10     empty   res[4]=-1   push 10  [10]
i=11 idx5 num=3    pop none (10>3)   top=10  res[5]=10   push 3   [10,3]

result = [3, 10, 5, 10, -1, 10]
```

Note `res[4]` stays `-1` because `10` is the global maximum — even the wrap finds nothing strictly greater.

### Complexity Analysis

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `2n` iterations, each value pushed once and popped at most once across the doubled pass. |
| Space | **O(N)** | The result holds `n` entries; the stack holds up to `n` values during a single lap. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr = [1]` | `[-1]` | One element wrapping onto itself still has no strictly-greater predecessor. |
| All equal | `arr = [5, 5, 5]` | `[-1, -1, -1]` | Equal values are popped by the `≤` test; nothing strictly greater survives. |
| Two ascending | `arr = [1, 2]` | `[2, -1]` | `1` wraps to `2`; `2` is the maximum → -1. |
| Wrap-dependent | `arr = [3, 1, 2]` | `[-1, 3, 3]` | `1` and `2` both see `3` directly; `3` is the global maximum → -1. |
| Strictly descending | `arr = [5, 4, 3, 2, 1]` | `[-1, 5, 4, 3, 2]` | Each value sees its strictly-greater left neighbour; `5` is the max → -1. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here is circularity: iterate `2n` times with `i % n` indexing so a value's previous-greater can wrap past the array start. The monotonic stack, the comparison, and the `O(N)` time / `O(N)` space bounds are identical to the linear superior problem — only the loop bound and the modular index change.

</details>
