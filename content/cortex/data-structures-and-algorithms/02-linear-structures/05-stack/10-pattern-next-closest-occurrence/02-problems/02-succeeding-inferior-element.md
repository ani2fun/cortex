---
title: "Succeeding Inferior Element"
summary: "Same as above but strictly smaller. Maintain an *increasing* monotonic stack; resolve when current value is *smaller* than the stack's top."
prereqs:
  - 10-pattern-next-closest-occurrence/01-pattern
difficulty: easy
kind: problem
topics: [next-closest-occurrence, stack]
---

# Succeeding inferior element

## Problem Statement

Given two arrays `arr1` and `arr2` (where `arr2` is a subset of `arr1` and all elements are unique), return for each value in `arr2` its **succeeding inferior element** in `arr1` — the first strictly-smaller element to its right. Return `-1` if none. This is the *next-smaller* mirror: maintain an *increasing* monotonic stack and resolve when the current value is smaller than the stack's top.

### Example 1
> -   **Input:** `arr1 = [3, 5, 1, 6, 8, 9]`, `arr2 = [3, 1, 8, 9]`
> -   **Output:** `[1, -1, -1, -1]`

### Example 2
> -   **Input:** `arr1 = [5, 9, 7, 8, 1]`, `arr2 = [5, 9, 7]`
> -   **Output:** `[1, 7, 1]`

## Examples

**Example 1**
```
Input:  arr1 = [3, 5, 1, 6, 8, 9], arr2 = [3, 1, 8, 9]
Output: [1, -1, -1, -1]
Explanation: 3 sees 1 as its first smaller successor → 1. 1 has nothing smaller after it → -1.
8 is followed only by 9 → -1. 9 ends the array → -1.
```

**Example 2**
```
Input:  arr1 = [5, 9, 7, 8, 1], arr2 = [5, 9, 7]
Output: [1, 7, 1]
Explanation: 5 finds 1 as its nearest smaller successor → 1. 9 sees 7 next → 7.
7 finds 1 further along → 1.
```

**Example 3**
```
Input:  arr1 = [4, 3, 2, 1], arr2 = [4, 2]
Output: [3, 1]
Explanation: 4 sees 3 next → 3. 2 sees 1 next → 1. Each value's successor is its right neighbour.
```

**Example 4**
```
Input:  arr1 = [1, 2, 3, 4], arr2 = [4, 1]
Output: [-1, -1]
Explanation: The array is strictly increasing, so nothing has a smaller value to its right.
```

```quiz
{
  "prompt": "arr1 = [3, 1, 4, 1, 5], arr2 = [4, 3]. Note: assume all elements unique — arr2 = [4, 3] for arr1 = [3, 4, 5, 1, 2]. What does succeeding inferior return for arr2 = [4, 5] on arr1 = [3, 4, 5, 1, 2]?",
  "options": ["[1, 1]", "[3, 4]", "[1, 3]", "[-1, 1]"],
  "answer": "[1, 1]"
}
```

## Constraints

- `1 ≤ arr1.length ≤ 1000`
- `1 ≤ arr2.length ≤ arr1.length`
- All elements in `arr1` are unique
- `arr2` is a subset of `arr1`

```python run
import ast
from typing import List

class Solution:
    def succeeding_inferior_element(self, arr_1: List[int], arr_2: List[int]) -> List[int]:
        # Your code goes here — build a next-smaller map for arr1 via a
        # right-to-left increasing monotonic stack (pop while top >= num),
        # then look up each arr2 value.
        return [-1] * len(arr_2)

arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().succeeding_inferior_element(arr1, arr2))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingInferiorElement(int[] arr1, int[] arr2) {
            // Your code goes here — build a next-smaller map for arr1 via a
            // right-to-left increasing monotonic stack (pop while top >= num),
            // then look up each arr2 value.
            int[] result = new int[arr2.length];
            Arrays.fill(result, -1);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingInferiorElement(arr1, arr2)));
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
    { "id": "arr1", "label": "arr1", "type": "int[]", "placeholder": "[3, 5, 1, 6, 8, 9]" },
    { "id": "arr2", "label": "arr2", "type": "int[]", "placeholder": "[3, 1, 8, 9]" }
  ],
  "cases": [
    { "args": { "arr1": "[3, 5, 1, 6, 8, 9]", "arr2": "[3, 1, 8, 9]" }, "expected": "[1, -1, -1, -1]" },
    { "args": { "arr1": "[5, 9, 7, 8, 1]", "arr2": "[5, 9, 7]" }, "expected": "[1, 7, 1]" },
    { "args": { "arr1": "[4, 3, 2, 1]", "arr2": "[4, 2]" }, "expected": "[3, 1]" },
    { "args": { "arr1": "[1, 2, 3, 4]", "arr2": "[4, 1]" }, "expected": "[-1, -1]" },
    { "args": { "arr1": "[2, 1]", "arr2": "[2, 1]" }, "expected": "[1, -1]" },
    { "args": { "arr1": "[1, 2]", "arr2": "[1, 2]" }, "expected": "[-1, -1]" },
    { "args": { "arr1": "[1]", "arr2": "[1]" }, "expected": "[-1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **monotonic-stack** problem is the *next-smaller* query — each value in `arr1` wants the nearest strictly-smaller value to its right. That "nearest qualifying successor" shape is the exact signal the next-closest pattern fires on; only the comparison flips from greater to smaller.

The stack holds the indices of values still *waiting* for a smaller successor, in strictly increasing order of value — bottom smallest, top largest. When a new value arrives, every stacked value that exceeds it has just found its next-smaller, so each is resolved and popped. The new value is then pushed to wait for its own successor.

The naive approach re-scans for every query and breaks the time budget. For each value in `arr2` it walks `arr1` rightward until a smaller value appears — `O(N × M)` time, quadratic when `arr2` is as long as `arr1`. The stack pass computes every next-smaller in `arr1` once, so each query becomes an `O(1)` index-map lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Succeeding Inferior Element |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *after* it? | **Yes** — the next-smaller of each `arr1` index ranges only over later indices. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-smaller successor per index. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict smaller-than test drives every resolve-and-pop (increasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — each index is pushed once, popped at most once; the index-map read is `O(1)`. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Identical to the superior version with one flip: the stack is increasing, and a value resolves when the current value is *smaller* than the stack top. The scan still runs `arr1` in reverse.

1. **Allocate the result holders.** Create `nextSmaller` over `arr1`, filled with `-1`, an empty `stack`, and an empty `value → index` map.
2. **Walk `arr1` right to left.** For each value `num` at index `i`, pop while the stack is non-empty and its top `≥ num`.
3. **Record the survivor.** If the stack is non-empty, set `nextSmaller[i]` to the top — the nearest strictly-smaller successor.
4. **Push and index.** Push `num` onto the stack, then store `map[num] = i` for the lookup pass.
5. **Answer the queries.** For each value in `arr2`, look up its index in the map and append `nextSmaller[index]` to the result, using `-1` when the value is absent.
6. **Return the result.** It holds one next-smaller answer per query, in `arr2` order.

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>

```python solution time=O(N+M) space=O(N)
import ast
from typing import List

class Solution:
    def succeeding_inferior_element(
        self, arr_1: List[int], arr_2: List[int]
    ) -> List[int]:

        # Array to store the next smaller elements for arr_1
        next_smaller = [-1] * len(arr_1)

        # Map to store the last index of each element in arr_1
        index_map = {}

        # Stack to help find the next smaller element efficiently
        stack = []

        # Step 1: Build the next smaller elements array for arr_1
        # (Traverse in reverse order)
        for i in range(len(arr_1) - 1, -1, -1):
            num = arr_1[i]

            # Remove elements from the stack that are greater than or
            # equal to the current element
            while stack and stack[-1] >= num:
                stack.pop()

            # If the stack is not empty, set the next smaller element
            if stack:
                next_smaller[i] = stack[-1]

            # Push the current element onto the stack for future elements
            stack.append(num)

            # Store the index of the current element in the index map
            index_map[num] = i

        # Step 2: Process arr_2 to generate the result
        result = []
        for num in arr_2:

            # Push the next smaller element if found, otherwise -1
            result.append(
                next_smaller[index_map[num]] if num in index_map else -1
            )

        return result

arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().succeeding_inferior_element(arr1, arr2))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingInferiorElement(int[] arr1, int[] arr2) {

            // Array to store the next smaller elements for arr1
            int[] nextSmaller = new int[arr1.length];
            Arrays.fill(nextSmaller, -1);

            // Map to store the last index of each element in arr1
            Map<Integer, Integer> indexMap = new HashMap<>();

            // Stack to help find the next smaller element efficiently
            Stack<Integer> stack = new Stack<>();

            // Step 1: Build the next smaller elements array for arr1
            // (Traverse in reverse order)
            for (int i = arr1.length - 1; i >= 0; i--) {
                int num = arr1[i];

                // Remove elements from the stack that are greater than or
                // equal to the current element
                while (!stack.isEmpty() && stack.peek() >= num) {
                    stack.pop();
                }

                // If the stack is not empty, set the next smaller element
                if (!stack.isEmpty()) {
                    nextSmaller[i] = stack.peek();
                }

                // Push the current element onto the stack for future
                // elements
                stack.push(num);

                // Store the index of the current element in the index map
                indexMap.put(num, i);
            }

            // Step 2: Process arr2 to generate the result
            int[] result = new int[arr2.length];
            for (int i = 0; i < arr2.length; i++) {
                int num = arr2[i];

                // Push the next smaller element if found, otherwise -1
                result[i] = indexMap.containsKey(num)
                    ? nextSmaller[indexMap.get(num)]
                    : -1;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingInferiorElement(arr1, arr2)));
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


Walk Example 1 — `arr1 = [3, 5, 1, 6, 8, 9]`, `arr2 = [3, 1, 8, 9]`. Scan `arr1` in reverse with a strictly increasing stack; pop while the top `≥ num`:

```
i=5  num=9   pop none            stack empty → nse[5] = -1   push 9   stack=[9]
i=4  num=8   pop 9 (9≥8)         stack empty → nse[4] = -1   push 8   stack=[8]
i=3  num=6   pop 8 (8≥6)         stack empty → nse[3] = -1   push 6   stack=[6]
i=2  num=1   pop 6 (6≥1)         stack empty → nse[2] = -1   push 1   stack=[1]
i=1  num=5   pop none (1<5)      top=1       → nse[1] = 1    push 5   stack=[1,5]
i=0  num=3   pop 5 (5≥3)         top=1       → nse[0] = 1    push 3   stack=[1,3]

nextSmaller = [1, 1, -1, -1, -1, -1]
index_map   = {9:5, 8:4, 6:3, 1:2, 5:1, 3:0}

queries: 3→idx0→1 | 1→idx2→-1 | 8→idx4→-1 | 9→idx5→-1
result = [1, -1, -1, -1]
```

The result `[1, -1, -1, -1]` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N + M)** | One amortised `O(N)` stack pass over `arr1` plus `O(M)` lookups for `arr2`. |
| Space | **O(N)** | `nextSmaller` array, the index map, and the stack each hold up to `N` entries. |

The stack pass is `O(N)` amortised: each value is pushed once and popped at most once across the whole walk, capping stack operations at `2N`.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr1 = [1]`, `arr2 = [1]` | `[-1]` | No successor exists for the only value. |
| Two ascending | `arr1 = [1, 2]`, `arr2 = [1, 2]` | `[-1, -1]` | Nothing smaller follows either value. |
| Two descending | `arr1 = [2, 1]`, `arr2 = [2, 1]` | `[1, -1]` | `2` sees `1`; `1` ends the array. |
| Sorted descending | `arr1 = [4, 3, 2, 1]`, `arr2 = [4, 2]` | `[3, 1]` | Each value's next-smaller is its immediate right neighbour. |
| Sorted ascending | `arr1 = [1, 2, 3, 4]`, `arr2 = [4, 1]` | `[-1, -1]` | A strictly increasing array has no smaller successor anywhere. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here versus the superior problem is a single flip: an *increasing* stack popped on `≥`, surfacing the nearest strictly-*smaller* successor instead of the greater one. The reverse scan, the index-map lookup, and the `O(N + M)` time / `O(N)` space bounds are otherwise identical.

</details>
