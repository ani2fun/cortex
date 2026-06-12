---
title: "Succeeding Superior Element II"
summary: "Circular variant — arr is treated as a ring; for each element find the next strictly-greater element, allowing one wrap-around to the start of the array."
prereqs:
  - 10-pattern-next-closest-occurrence/01-pattern
difficulty: medium
kind: problem
topics: [next-closest-occurrence, stack]
---

# Succeeding superior element II

## Problem Statement

Circular variant — `arr` is treated as a ring; for each element find the next strictly-greater element, allowing one wrap-around to the start of the array. If no greater value exists even after a full circle, return `-1`.

### Example 1
> -   **Input:** `arr = [2, 5, 1, 6, 10, 3]` → **Output:** `[5, 6, 6, 10, -1, 5]`

### Example 2
> -   **Input:** `arr = [6, 7, 8, 9, 8]` → **Output:** `[7, 8, 9, -1, 9]`

## Examples

**Example 1**
```
Input:  arr = [2, 5, 1, 6, 10, 3]
Output: [5, 6, 6, 10, -1, 5]
Explanation: 2 sees 5 → 5. 5 sees 6 → 6. 1 sees 6 → 6. 6 sees 10 → 10.
10 is the global maximum → -1 even after a full circle. 3 wraps past the end to 5 → 5.
```

**Example 2**
```
Input:  arr = [6, 7, 8, 9, 8]
Output: [7, 8, 9, -1, 9]
Explanation: 6→7, 7→8, 8→9 directly. 9 is the maximum → -1.
The trailing 8 wraps around to 9 → 9.
```

**Example 3**
```
Input:  arr = [3, 2, 1]
Output: [-1, 3, 3]
Explanation: 3 is the global maximum → -1. 2 and 1 both wrap to 3 → 3.
```

**Example 4**
```
Input:  arr = [5, 5, 5]
Output: [-1, -1, -1]
Explanation: Equal values are never strictly greater, so the pop test removes them all.
```

```quiz
{
  "prompt": "For arr = [1, 3, 2], what is the circular next-greater result?",
  "options": ["[3, -1, 3]", "[3, -1, -1]", "[-1, -1, 3]", "[3, 3, 3]"],
  "answer": "[3, -1, 3]"
}
```

## Constraints

- `1 ≤ arr.length ≤ 10000`
- `-10^9 ≤ arr[i] ≤ 10^9`

```python run
import ast
from typing import List

class Solution:
    def succeeding_superior_element_ii(self, arr: List[int]) -> List[int]:
        # Your code goes here — iterate 2n times in reverse using i % n,
        # maintain a decreasing stack (pop while top <= num), record the
        # surviving top (or -1), always push num.
        return [-1] * len(arr)

arr = ast.literal_eval(input())
print(Solution().succeeding_superior_element_ii(arr))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingSuperiorElementII(int[] arr) {
            // Your code goes here — iterate 2n times in reverse using i % n,
            // maintain a decreasing stack (pop while top <= num), record the
            // surviving top (or -1), always push num.
            int[] result = new int[arr.length];
            Arrays.fill(result, -1);
            return result;
        }
    }

    public static void main(String[] args) {
        int[] arr = parseIntArray(new Scanner(System.in).nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingSuperiorElementII(arr)));
    }

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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[2, 5, 1, 6, 10, 3]" }
  ],
  "cases": [
    { "args": { "arr": "[2, 5, 1, 6, 10, 3]" }, "expected": "[5, 6, 6, 10, -1, 5]" },
    { "args": { "arr": "[6, 7, 8, 9, 8]" }, "expected": "[7, 8, 9, -1, 9]" },
    { "args": { "arr": "[3, 2, 1]" }, "expected": "[-1, 3, 3]" },
    { "args": { "arr": "[5, 5, 5]" }, "expected": "[-1, -1, -1]" },
    { "args": { "arr": "[1, 2]" }, "expected": "[2, -1]" },
    { "args": { "arr": "[1, 2, 3]" }, "expected": "[2, 3, -1]" },
    { "args": { "arr": "[5]" }, "expected": "[-1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is the same next-greater query, but the array is now **circular** — a value's nearest strictly-greater successor may live to its left, reachable only by wrapping past the end. That single change is what separates this from the linear superior problem; the monotonic stack itself is untouched.

The trick is to **linearise the ring by iterating `2n` indices** with `i % n` indexing. Each original position is processed twice: once on the natural pass and once after a full wrap. The stack still holds a strictly decreasing chain of unresolved candidates; the second lap lets a wrapped-around value finally find its successor. Because every element is visited exactly twice, the work stays `O(N)`.

The naive circular approach breaks the time budget badly. For each index you would scan up to `n` other positions wrapping around — `O(N²)` time and easy to get wrong at the wrap boundary. The doubled-pass stack keeps the single-sweep `O(N)` guarantee and handles the wrap by construction, never special-casing the seam between the end and the start.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Succeeding Superior Element II |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *after* it? | **Yes** — but "after" wraps circularly, so the search may continue past the end. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-greater value reachable going right-then-wrapping. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict greater-than test drives every pop (decreasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — `2n` iterations, each value pushed and popped at most once across the doubled pass. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Run the reverse next-greater scan over a doubled index range, so each value gets a second lap to find a wrapped successor.

1. **Allocate the holders.** Create `result` of length `n`, filled with `-1`, and an empty `stack`.
2. **Iterate `2n` times in reverse.** For loop counter `i` from `2n − 1` down to `0`, take the circular index `index = i % n` and the value `num = arr[index]`.
3. **Pop the dominated candidates.** While the stack is non-empty and its top `≤ num`, pop.
4. **Record the survivor.** If the stack is non-empty, set `result[index]` to the top. The second lap re-assigns the same answer for already-solved positions, so overwriting is safe.
5. **Always push.** Push `num` onto the stack so it can serve later (including wrapped) elements.
6. **Return the result.** After `2n` iterations every position with a wrapped or direct successor is filled; the rest stay `-1`.

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>

```python solution time=O(N) space=O(N)
import ast
from typing import List

class Solution:
    def succeeding_superior_element_ii(
        self, arr: List[int]
    ) -> List[int]:
        n = len(arr)
        result = [-1] * n

        # Stack to store elements
        stack = []

        # Iterate twice through the array in reverse order (circularly)
        for i in range(2 * n - 1, -1, -1):

            # Circular index
            index = i % n
            num = arr[index]

            # Check if we can pop elements from the stack
            # (i.e., find the succeeding greater element for those
            # elements)
            while stack and stack[-1] <= num:
                stack.pop()

            # If stack is not empty, the top element is the succeeding
            # superior element
            if stack:
                result[index] = stack[-1]

            # Always push the element to the stack
            stack.append(num)

        return result

arr = ast.literal_eval(input())
print(Solution().succeeding_superior_element_ii(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingSuperiorElementII(int[] arr) {
            int n = arr.length;
            int[] result = new int[n];

            // Initialize result with -1
            for (int i = 0; i < n; i++) {
                result[i] = -1;
            }

            // Stack to store elements
            Stack<Integer> stack = new Stack<>();

            // Iterate twice through the array in reverse order (circularly)
            for (int i = 2 * n - 1; i >= 0; i--) {

                // Circular index
                int index = i % n;
                int num = arr[index];

                // Check if we can pop elements from the stack
                // (i.e., find the succeeding greater element for those
                // elements)
                while (!stack.isEmpty() && stack.peek() <= num) {
                    stack.pop();
                }

                // If stack is not empty, the top element is the succeeding
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

    public static void main(String[] args) {
        int[] arr = parseIntArray(new Scanner(System.in).nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingSuperiorElementII(arr)));
    }

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

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `arr = [2, 5, 1, 6, 10, 3]`, `n = 6`, so `12` iterations counting *down* from `11`. Decreasing stack, pop while the top `≤ num`. The first six steps fill direct answers; the last six wrap and finish the rest:

```
i=11 idx5 num=3    pop none          empty   res[5]=-1   push 3   [3]
i=10 idx4 num=10   pop 3             empty   res[4]=-1   push 10  [10]
i= 9 idx3 num=6    pop none (10>6)   top=10  res[3]=10   push 6   [10,6]
i= 8 idx2 num=1    pop none (6>1)    top=6   res[2]=6    push 1   [10,6,1]
i= 7 idx1 num=5    pop 1 (1≤5)       top=6   res[1]=6    push 5   [10,6,5]
i= 6 idx0 num=2    pop none (5>2)    top=5   res[0]=5    push 2   [10,6,5,2]
i= 5 idx5 num=3    pop 2 (2≤3)       top=5   res[5]=5    push 3   [10,6,5,3]
i= 4 idx4 num=10   pop 3,5,6,10      empty   res[4]=-1   push 10  [10]
i= 3 idx3 num=6    pop none (10>6)   top=10  res[3]=10   push 6   [10,6]
i= 2 idx2 num=1    pop none (6>1)    top=6   res[2]=6    push 1   [10,6,1]
i= 1 idx1 num=5    pop 1 (1≤5)       top=6   res[1]=6    push 5   [10,6,5]
i= 0 idx0 num=2    pop none (5>2)    top=5   res[0]=5    push 2   [10,6,5,2]

result = [5, 6, 6, 10, -1, 5]
```

The result `[5, 6, 6, 10, -1, 5]` matches the expected output. Note `res[4]` stays `-1` because `10` is the global maximum — even the wrap finds nothing strictly greater.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `2n` iterations, each value pushed once and popped at most once across the doubled pass. |
| Space | **O(N)** | The result holds `n` entries; the stack holds up to `n` values during a single lap. |

Doubling the iteration count multiplies the work by a constant `2`, which `O(N)` absorbs. The space does not double — the stack only ever holds the live candidates of one lap.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr = [5]` | `[-1]` | One element wrapping onto itself still has no strictly-greater successor. |
| Two ascending | `arr = [1, 2]` | `[2, -1]` | `1` sees `2`; `2` is the maximum → -1. |
| Wrap-dependent | `arr = [3, 2, 1]` | `[-1, 3, 3]` | `3` is the global maximum → -1; `2` and `1` both wrap to `3`. |
| All equal | `arr = [5, 5, 5]` | `[-1, -1, -1]` | Equal values are popped by the `≤` test; nothing strictly greater survives. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here is circularity: iterate `2n` times with `i % n` indexing so a value's next-greater can wrap past the array end. The monotonic stack, the comparison, and the `O(N)` time / `O(N)` space bounds are identical to the linear superior problem — only the loop bound and the modular index change.

</details>
