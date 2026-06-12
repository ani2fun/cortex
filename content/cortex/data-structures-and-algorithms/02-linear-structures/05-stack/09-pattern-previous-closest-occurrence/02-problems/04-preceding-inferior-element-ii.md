---
title: "Preceding Inferior Element II"
summary: "Circular variant of preceding inferior. Same approach with the comparison flipped — increasing stack, pop while top >= current, iterate 2n times."
prereqs:
  - 09-pattern-previous-closest-occurrence/01-pattern
difficulty: medium
kind: problem
topics: [previous-closest-occurrence, stack]
---

# Preceding inferior element II

## Problem Statement

Given a circular array `arr`, return for each position the **preceding inferior element** — the nearest strictly-smaller value to its left, allowing the search to wrap around past the start of the array. If no smaller exists even after a full circle, return `-1`.

### Example 1
> -   **Input:** `arr = [2, 5, 1, 6, 10, 3]`
> -   **Output:** `[1, 2, -1, 1, 6, 1]`

### Example 2
> -   **Input:** `arr = [6, 7, 8, 9, 8]`
> -   **Output:** `[-1, 6, 7, 8, 7]`

## Examples

**Example 1**
```
Input:  arr = [2, 5, 1, 6, 10, 3]
Output: [1, 2, -1, 1, 6, 1]
Explanation: 2 wraps to 1 → 1. 5 sees 2 directly → 2. 1 is the global minimum → -1.
6 sees 1 → 1. 10 sees 6 → 6. 3 sees 1 → 1.
```

**Example 2**
```
Input:  arr = [6, 7, 8, 9, 8]
Output: [-1, 6, 7, 8, 7]
Explanation: 6 is the minimum → -1. 7 sees 6 → 6. 8 sees 7 → 7. 9 sees 8 → 8.
The trailing 8 sees the 7 before it → 7.
```

**Example 3**
```
Input:  arr = [3, 2, 1]
Output: [1, 1, -1]
Explanation: 3 and 2 both wrap to the trailing 1 → 1. 1 is the minimum → -1.
```

**Example 4**
```
Input:  arr = [5, 5, 5]
Output: [-1, -1, -1]
Explanation: Equal values are popped by the >= test; nothing strictly smaller survives.
```

## Constraints

- `0 ≤ arr.length ≤ 1000`
- `1 ≤ arr[i] ≤ 10^4`

```python run
import ast
from typing import List

class Solution:
    def preceding_inferior_element_ii(self, arr: List[int]) -> List[int]:
        # Your code goes here — iterate 2*n times with i % n indexing,
        # using an increasing stack (pop while top >= num).
        return [-1] * len(arr)

arr = ast.literal_eval(input())
print(Solution().preceding_inferior_element_ii(arr))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingInferiorElementII(int[] arr) {
            // Your code goes here — iterate 2*n times with i % n indexing,
            // using an increasing stack (pop while top >= num).
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
        System.out.println(Arrays.toString(new Solution().precedingInferiorElementII(arr)));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[2, 5, 1, 6, 10, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[2, 5, 1, 6, 10, 3]" }, "expected": "[1, 2, -1, 1, 6, 1]" },
    { "args": { "arr": "[6, 7, 8, 9, 8]" },      "expected": "[-1, 6, 7, 8, 7]" },
    { "args": { "arr": "[5]" },                   "expected": "[-1]" },
    { "args": { "arr": "[3, 1]" },                "expected": "[1, -1]" },
    { "args": { "arr": "[1, 2, 3]" },             "expected": "[-1, 1, 2]" },
    { "args": { "arr": "[3, 2, 1]" },             "expected": "[1, 1, -1]" },
    { "args": { "arr": "[5, 5, 5]" },             "expected": "[-1, -1, -1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is the circular previous-**smaller** query — each value wants the nearest strictly-smaller value to its left, with the search allowed to wrap past the array start. This combines the two twists seen so far: the *inferior* comparison from problem 2 and the *circular* doubled pass from problem 3.

The stack holds a strictly *increasing* chain of previous-smaller candidates, and the loop runs `2n` times with `i % n` indexing. The increasing order means a new value pops every candidate that matches or exceeds it, leaving the nearest smaller survivor on top. The second lap lets a value whose smaller predecessor lives past the wrap finally resolve.

The naive circular approach breaks the time budget the same way both predecessors did — per index it scans up to `n` wrapped positions for `O(N²)` time. The doubled increasing-stack pass keeps the `O(N)` guarantee and treats the wrap as just more iterations of the same loop.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Preceding Inferior Element II |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *before* it? | **Yes** — "before" wraps circularly, so the search may continue past the start. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-smaller value reachable going left-then-wrapping. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict less-than test drives the survivor choice (increasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — `2n` iterations, each value pushed and popped at most once across the doubled pass. |

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>


Run the linear previous-smaller walk over a doubled index range.

```python solution time=O(N) space=O(N)
import ast
from typing import List

class Solution:
    def preceding_inferior_element_ii(self, arr: List[int]) -> List[int]:
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
            # (i.e., find the preceding smaller element)
            while stack and stack[-1] >= num:
                stack.pop()

            # If stack is not empty, the top element is the preceding
            # inferior element
            if stack:
                result[index] = stack[-1]

            # Always push the element to the stack
            stack.append(num)

        return result


arr = ast.literal_eval(input())
print(Solution().preceding_inferior_element_ii(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingInferiorElementII(int[] arr) {
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
                // (i.e., find the preceding smaller element)
                while (!stack.isEmpty() && stack.peek() >= num) {
                    stack.pop();
                }

                // If stack is not empty, the top element is the preceding
                // inferior element
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
        System.out.println(Arrays.toString(new Solution().precedingInferiorElementII(arr)));
    }
}
```

### Dry Run

Walk Example 1 — `arr = [2, 5, 1, 6, 10, 3]`, `n = 6`, so `12` iterations. Increasing stack, pop while the top `≥ num`:

```
i= 0 idx0 num=2    pop none           empty   res[0]=-1   push 2   [2]
i= 1 idx1 num=5    pop none (2<5)     top=2   res[1]=2    push 5   [2,5]
i= 2 idx2 num=1    pop 5, pop 2       empty   res[2]=-1   push 1   [1]
i= 3 idx3 num=6    pop none (1<6)     top=1   res[3]=1    push 6   [1,6]
i= 4 idx4 num=10   pop none (6<10)    top=6   res[4]=6    push 10  [1,6,10]
i= 5 idx5 num=3    pop 10, pop 6      top=1   res[5]=1    push 3   [1,3]
i= 6 idx0 num=2    pop 3              top=1   res[0]=1    push 2   [1,2]
i= 7 idx1 num=5    pop none (2<5)     top=2   res[1]=2    push 5   [1,2,5]
i= 8 idx2 num=1    pop 5, pop 2, pop 1 empty  res[2]=-1   push 1   [1]
i= 9 idx3 num=6    pop none (1<6)     top=1   res[3]=1    push 6   [1,6]
i=10 idx4 num=10   pop none (6<10)    top=6   res[4]=6    push 10  [1,6,10]
i=11 idx5 num=3    pop 10, pop 6      top=1   res[5]=1    push 3   [1,3]

result = [1, 2, -1, 1, 6, 1]
```

`res[2]` stays `-1` because `1` is the global minimum — no value, wrapped or direct, is strictly smaller.

### Complexity Analysis

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `2n` iterations, each value pushed once and popped at most once across the doubled pass. |
| Space | **O(N)** | The result holds `n` entries; the stack holds up to `n` values during a single lap. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr = [5]` | `[-1]` | One element wrapping onto itself has no strictly-smaller predecessor. |
| Two descending | `arr = [3, 1]` | `[1, -1]` | `3` wraps to `1`; `1` is the minimum → -1. |
| Ascending | `arr = [1, 2, 3]` | `[-1, 1, 2]` | Each value sees its strictly-smaller left neighbour; `1` is the minimum → -1. |
| All equal | `arr = [5, 5, 5]` | `[-1, -1, -1]` | Equal values are popped by the `≥` test; nothing strictly smaller survives. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This problem stacks both prior twists: the *increasing* stack of the inferior variant runs over the `2n`-iteration doubled pass of the circular variant. Nothing new is invented — it is the composition of the operator flip and the modular index, still `O(N)` time and `O(N)` space.

Three lessons to carry forward:

1. **A monotonic stack stores un-disqualified candidates.** The moment a new element arrives that dominates something on the stack, the dominated value is no longer a viable answer for any future query. Pop it.
2. **Amortised O(N) is the magic.** A nested `while` looks like O(N²) but each element enters and leaves the stack at most once, capping total stack ops at 2N.
3. **Circular arrays double the iteration, not the memory.** Iterate `2*n` times with `i % n` indexing; the second pass catches answers that need to wrap around the start.

</details>
