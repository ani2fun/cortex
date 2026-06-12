---
title: "Preceding Inferior Element"
summary: "Same as preceding superior element but inferior = strictly smaller. Maintain an increasing monotonic stack; pop while top >= current."
prereqs:
  - 09-pattern-previous-closest-occurrence/01-pattern
difficulty: easy
kind: problem
topics: [previous-closest-occurrence, stack]
---

# Preceding inferior element

## Problem Statement

Given two arrays `arr1` and `arr2` (where `arr2` is a subset of `arr1` and all elements are unique), return for each value in `arr2` its **preceding inferior element** in `arr1` — the first strictly-smaller element to its left in `arr1`. Return `-1` for values with no preceding inferior.

### Example 1
> -   **Input:** `arr1 = [3, 5, 1, 6, 8, 2]`, `arr2 = [3, 1, 8, 2]`
> -   **Output:** `[-1, -1, 6, 1]`

### Example 2
> -   **Input:** `arr1 = [5, 9, 7, 8, 1]`, `arr2 = [5, 9, 7]`
> -   **Output:** `[-1, 5, 5]`

## Examples

**Example 1**
```
Input:  arr1 = [3, 5, 1, 6, 8, 2], arr2 = [3, 1, 8, 2]
Output: [-1, -1, 6, 1]
Explanation: 3 starts the array → -1. 1 is the running minimum at its position → -1.
8 sees 6 as its nearest strictly-smaller predecessor → 6. 2 sees 1 → 1.
```

**Example 2**
```
Input:  arr1 = [5, 9, 7, 8, 1], arr2 = [5, 9, 7]
Output: [-1, 5, 5]
Explanation: 5 has nothing smaller to its left → -1. 9 sees 5 → 5. 7 also sees 5 → 5.
```

**Example 3**
```
Input:  arr1 = [2, 5, 3], arr2 = [5, 3]
Output: [2, 2]
Explanation: Both 5 and 3 have 2 as their nearest strictly-smaller predecessor.
```

**Example 4**
```
Input:  arr1 = [5], arr2 = [5]
Output: [-1]
Explanation: A single element has no predecessor.
```

## Constraints

- `1 ≤ arr1.length ≤ 1000`, `1 ≤ arr2.length ≤ arr1.length`
- `arr2` is a subset of `arr1`; all elements of `arr1` are unique
- `1 ≤ arr1[i] ≤ 10^4`

```python run
import ast
from typing import List

class Solution:
    def preceding_inferior_element(self, arr_1: List[int], arr_2: List[int]) -> List[int]:
        # Your code goes here — same structure as preceding_superior_element,
        # but flip the stack comparison: pop while top >= num (increasing stack).
        return []

arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().preceding_inferior_element(arr1, arr2))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingInferiorElement(int[] arr1, int[] arr2) {
            // Your code goes here — same structure as precedingSuperiorElement,
            // but flip the stack comparison: pop while top >= num (increasing stack).
            return new int[arr2.length];
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
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().precedingInferiorElement(arr1, arr2)));
    }
}
```

```testcases
{
  "args": [
    { "id": "arr1", "label": "arr1", "type": "int[]", "placeholder": "[3, 5, 1, 6, 8, 2]" },
    { "id": "arr2", "label": "arr2", "type": "int[]", "placeholder": "[3, 1, 8, 2]" }
  ],
  "cases": [
    { "args": { "arr1": "[3, 5, 1, 6, 8, 2]", "arr2": "[3, 1, 8, 2]" },  "expected": "[-1, -1, 6, 1]" },
    { "args": { "arr1": "[5, 9, 7, 8, 1]",    "arr2": "[5, 9, 7]" },     "expected": "[-1, 5, 5]" },
    { "args": { "arr1": "[1, 2, 3]",           "arr2": "[1, 2, 3]" },     "expected": "[-1, 1, 2]" },
    { "args": { "arr1": "[3, 2, 1]",           "arr2": "[3, 2, 1]" },     "expected": "[-1, -1, -1]" },
    { "args": { "arr1": "[5]",                 "arr2": "[5]" },           "expected": "[-1]" },
    { "args": { "arr1": "[2, 5, 3]",           "arr2": "[5, 3]" },        "expected": "[2, 2]" },
    { "args": { "arr1": "[4, 1, 3]",           "arr2": "[1, 3]" },        "expected": "[-1, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property is identical to the superior version, with the comparison reversed — each value wants the nearest strictly-**smaller** value to its left. That "nearest qualifying predecessor" shape is still the previous-closest pattern; only the test that disqualifies a candidate flips from greater-than to less-than.

The stack now holds an un-disqualified chain of **previous-smaller candidates** in strictly *increasing* order — bottom smallest, top largest. When a new value arrives, any candidate that matches or exceeds it is popped, because that candidate now sits behind a value no larger and can never be a future element's previous-smaller. The survivor on top is the answer, and the new value is pushed for the elements that follow.

The naive approach breaks the time budget the same way as before — scanning `arr1` afresh for every `arr2` query is `O(N × M)` time. One increasing-stack pass computes every previous-smaller in `arr1`, turning each query into an `O(1)` index-map lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Preceding Inferior Element |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *before* it? | **Yes** — the previous-smaller of each `arr1` index ranges only over earlier indices. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-smaller predecessor per index. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict less-than test drives the survivor choice (increasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — each value is pushed once, popped at most once; the index-map read is `O(1)`. |

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>


Mirror the superior version with the comparison flipped to build an *increasing* stack.

```python solution time=O(N+M) space=O(N)
import ast
from typing import List

class Solution:
    def preceding_inferior_element(
        self, arr_1: List[int], arr_2: List[int]
    ) -> List[int]:

        # Array to store the previous smaller elements for arr_1
        previous_smaller = [-1] * len(arr_1)

        # Map to store the last index of each element in arr_1
        index_map = {}

        # Stack to help find the previous smaller element efficiently
        stack = []

        # Step 1: Build the previous smaller elements array for arr_1
        for i, num in enumerate(arr_1):

            # Remove elements from the stack that are greater than or
            # equal to the current element
            while stack and stack[-1] >= num:
                stack.pop()

            # If the stack is not empty, set the previous smaller element
            if stack:
                previous_smaller[i] = stack[-1]

            # Push the current element onto the stack for future elements
            stack.append(num)

            # Store the index of the current element in the index map
            index_map[num] = i

        # Step 2: Process arr_2 to generate the result
        result = []
        for num in arr_2:

            # Push the previous smaller element if found, otherwise -1
            result.append(
                previous_smaller[index_map[num]]
                if num in index_map
                else -1
            )

        return result


arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().preceding_inferior_element(arr1, arr2))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingInferiorElement(int[] arr1, int[] arr2) {

            // Array to store the previous smaller elements for arr1
            int[] previousSmaller = new int[arr1.length];
            Arrays.fill(previousSmaller, -1);

            // Map to store the last index of each element in arr1
            Map<Integer, Integer> indexMap = new HashMap<>();

            // Stack to help find the previous smaller element efficiently
            Deque<Integer> stack = new ArrayDeque<>();

            // Step 1: Build the previous smaller elements array for arr1
            for (int i = 0; i < arr1.length; i++) {
                int num = arr1[i];

                // Remove elements from the stack that are greater than or
                // equal to the current element
                while (!stack.isEmpty() && stack.peek() >= num) {
                    stack.pop();
                }

                // If the stack is not empty, set the previous smaller element
                if (!stack.isEmpty()) {
                    previousSmaller[i] = stack.peek();
                }

                // Push the current element onto the stack for future elements
                stack.push(num);

                // Store the index of the current element in the index map
                indexMap.put(num, i);
            }

            // Step 2: Process arr2 to generate the result
            int[] result = new int[arr2.length];
            for (int i = 0; i < arr2.length; i++) {
                int num = arr2[i];

                // Push the previous smaller element if found, otherwise -1
                result[i] = indexMap.containsKey(num)
                    ? previousSmaller[indexMap.get(num)]
                    : -1;
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
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(arr1, arr2)
        ));
    }
}
```

### Dry Run

Walk Example 1 — `arr1 = [3, 5, 1, 6, 8, 2]`, `arr2 = [3, 1, 8, 2]`. The stack stays strictly increasing; pop while the top `≥ num`:

```
i=0  num=3   pop none           stack empty → psm[0] = -1   push 3   stack=[3]
i=1  num=5   pop none (3<5)     top=3       → psm[1] = 3    push 5   stack=[3,5]
i=2  num=1   pop 5, pop 3       stack empty → psm[2] = -1   push 1   stack=[1]
i=3  num=6   pop none (1<6)     top=1       → psm[3] = 1    push 6   stack=[1,6]
i=4  num=8   pop none (6<8)     top=6       → psm[4] = 6    push 8   stack=[1,6,8]
i=5  num=2   pop 8, pop 6       top=1       → psm[5] = 1    push 2   stack=[1,2]

previousSmaller = [-1, 3, -1, 1, 6, 1]
index_map       = {3:0, 5:1, 1:2, 6:3, 8:4, 2:5}

queries: 3→idx0→-1 | 1→idx2→-1 | 8→idx4→6 | 2→idx5→1
result = [-1, -1, 6, 1]
```

### Complexity Analysis

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N + M)** | One amortised `O(N)` stack pass over `arr1` plus `O(M)` lookups for `arr2`. |
| Space | **O(N)** | `previousSmaller` array, the index map, and the stack each hold up to `N` entries. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr1 = [5]`, `arr2 = [5]` | `[-1]` | No predecessor exists for the only value. |
| Sorted ascending | `arr1 = [1, 2, 3]`, `arr2 = [1, 2, 3]` | `[-1, 1, 2]` | Each value's nearest smaller predecessor is the one just before it. |
| Sorted descending | `arr1 = [3, 2, 1]`, `arr2 = [3, 2, 1]` | `[-1, -1, -1]` | Each value is the running minimum — nothing smaller precedes it. |
| Shared predecessor | `arr1 = [2, 5, 3]`, `arr2 = [5, 3]` | `[2, 2]` | `2` precedes and stays below both queries. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here is a single operator flip: the stack becomes *increasing* and pops while the top is `≥` the current value, returning the nearest strictly-smaller predecessor instead of the greater one. Everything else — the two-pass structure, the index map, the `O(N + M)` cost — is unchanged from the superior variant.

</details>
