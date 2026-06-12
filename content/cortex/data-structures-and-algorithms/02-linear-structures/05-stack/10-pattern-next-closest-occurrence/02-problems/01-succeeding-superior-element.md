---
title: "Succeeding Superior Element"
summary: "Given two arrays arr1 and arr2 (where arr2 is a subset of arr1 and all elements are unique), return for each value in arr2 its succeeding superior element in arr1 — the first strictly-greater element "
prereqs:
  - 10-pattern-next-closest-occurrence/01-pattern
difficulty: easy
kind: problem
topics: [next-closest-occurrence, stack]
---

# Succeeding superior element

## Problem Statement

Given two arrays `arr1` and `arr2` (where `arr2` is a subset of `arr1` and all elements are unique), return for each value in `arr2` its **succeeding superior element** in `arr1` — the first strictly-greater element to its right. Return `-1` if none.

### Example 1
> -   **Input:** `arr1 = [3, 5, 1, 6, 8, 7]`, `arr2 = [3, 1, 8, 7]`
> -   **Output:** `[5, 6, -1, -1]`

### Example 2
> -   **Input:** `arr1 = [5, 9, 7, 8, 1]`, `arr2 = [5, 9, 7]`
> -   **Output:** `[9, -1, 8]`

## Examples

**Example 1**
```
Input:  arr1 = [3, 5, 1, 6, 8, 7], arr2 = [3, 1, 8, 7]
Output: [5, 6, -1, -1]
Explanation: 3 sees 5 just after it → 5. 1 finds 6 as its first taller successor → 6.
8 is followed only by 7 → -1. 7 ends the array → -1.
```

**Example 2**
```
Input:  arr1 = [5, 9, 7, 8, 1], arr2 = [5, 9, 7]
Output: [9, -1, 8]
Explanation: 5 sees 9 next → 9. 9 is the running maximum to its right → -1.
7 finds 8 as its nearest strictly-greater successor → 8.
```

**Example 3**
```
Input:  arr1 = [1, 2, 3, 4], arr2 = [1, 3]
Output: [2, 4]
Explanation: 1 sees 2 next → 2. 3 sees 4 next → 4. Each value's successor is its right neighbour.
```

**Example 4**
```
Input:  arr1 = [4, 3, 2, 1], arr2 = [4, 1]
Output: [-1, -1]
Explanation: The array is strictly decreasing, so nothing has a greater value to its right.
```

```quiz
{
  "prompt": "arr1 = [1, 3, 2, 4], arr2 = [3, 2]. What does the next-greater for arr2 return?",
  "options": ["[4, 4]", "[4, 3]", "[2, 4]", "[-1, 4]"],
  "answer": "[4, 4]"
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
    def succeeding_superior_element(self, arr_1: List[int], arr_2: List[int]) -> List[int]:
        # Your code goes here — build a next-greater map for arr1 via a
        # right-to-left monotonic stack, then look up each arr2 value.
        return [-1] * len(arr_2)

arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().succeeding_superior_element(arr1, arr2))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingSuperiorElement(int[] arr1, int[] arr2) {
            // Your code goes here — build a next-greater map for arr1 via a
            // right-to-left monotonic stack, then look up each arr2 value.
            int[] result = new int[arr2.length];
            Arrays.fill(result, -1);
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingSuperiorElement(arr1, arr2)));
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
    { "id": "arr1", "label": "arr1", "type": "int[]", "placeholder": "[3, 5, 1, 6, 8, 7]" },
    { "id": "arr2", "label": "arr2", "type": "int[]", "placeholder": "[3, 1, 8, 7]" }
  ],
  "cases": [
    { "args": { "arr1": "[3, 5, 1, 6, 8, 7]", "arr2": "[3, 1, 8, 7]" }, "expected": "[5, 6, -1, -1]" },
    { "args": { "arr1": "[5, 9, 7, 8, 1]", "arr2": "[5, 9, 7]" }, "expected": "[9, -1, 8]" },
    { "args": { "arr1": "[1, 2, 3, 4]", "arr2": "[1, 3]" }, "expected": "[2, 4]" },
    { "args": { "arr1": "[4, 3, 2, 1]", "arr2": "[4, 1]" }, "expected": "[-1, -1]" },
    { "args": { "arr1": "[1, 2]", "arr2": "[1, 2]" }, "expected": "[2, -1]" },
    { "args": { "arr1": "[2, 1]", "arr2": "[2, 1]" }, "expected": "[-1, -1]" },
    { "args": { "arr1": "[1]", "arr2": "[1]" }, "expected": "[-1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **monotonic-stack** problem is the *next-greater* query — each value in `arr1` wants the nearest strictly-greater value to its right. That "nearest qualifying successor" shape is the exact signal the next-closest pattern fires on. The queries in `arr2` only re-index answers that already exist inside `arr1`.

The stack holds the indices of values still *waiting* for a greater successor, in strictly decreasing order of value — bottom largest, top smallest. When a new value arrives, every stacked value it exceeds has just found its next-greater, so each is resolved and popped. The new value is then pushed to wait for its own successor.

The naive approach re-scans for every query and breaks the time budget. For each value in `arr2` it walks `arr1` rightward until a greater value appears — `O(N × M)` time, quadratic when `arr2` is as long as `arr1`. The stack pass computes every next-greater in `arr1` once, so each query becomes an `O(1)` index-map lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Succeeding Superior Element |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *after* it? | **Yes** — the next-greater of each `arr1` index ranges only over later indices. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-greater successor per index. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict greater-than test drives every resolve-and-pop (decreasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — each index is pushed once, popped at most once; the index-map read is `O(1)`. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Solve the all-positions problem on `arr1` first, then answer `arr2` by lookup. This implementation scans `arr1` in reverse and reuses the previous-closest technique unchanged.

1. **Allocate the result holders.** Create `nextGreater` over `arr1`, filled with `-1`, an empty `stack`, and an empty `value → index` map.
2. **Walk `arr1` right to left.** For each value `num` at index `i`, pop while the stack is non-empty and its top `≤ num`.
3. **Record the survivor.** If the stack is non-empty, set `nextGreater[i]` to the top — the nearest strictly-greater successor.
4. **Push and index.** Push `num` onto the stack, then store `map[num] = i` for the lookup pass.
5. **Answer the queries.** For each value in `arr2`, look up its index in the map and append `nextGreater[index]` to the result, using `-1` when the value is absent.
6. **Return the result.** It holds one next-greater answer per query, in `arr2` order.

</details>
<details>
<summary><h2>Solution & Analysis</h2></summary>

```python solution time=O(N+M) space=O(N)
import ast
from typing import List

class Solution:
    def succeeding_superior_element(
        self, arr_1: List[int], arr_2: List[int]
    ) -> List[int]:

        # Array to store the next greater elements for arr_1
        next_greater = [-1] * len(arr_1)

        # Map to store the last index of each element in arr_1
        index_map = {}

        # Stack to help find the next greater element efficiently
        stack = []

        # Step 1: Build the next greater elements array for arr_1
        # (Traverse in reverse order)
        for i in range(len(arr_1) - 1, -1, -1):
            num = arr_1[i]

            # Remove elements from the stack that are smaller than or
            # equal to the current element
            while stack and stack[-1] <= num:
                stack.pop()

            # If the stack is not empty, set the next greater element
            if stack:
                next_greater[i] = stack[-1]

            # Push the current element onto the stack for future elements
            stack.append(num)

            # Store the index of the current element in the index map
            index_map[num] = i

        # Step 2: Process arr_2 to generate the result
        result = []
        for num in arr_2:

            # Push the next greater element if found, otherwise -1
            result.append(
                next_greater[index_map[num]] if num in index_map else -1
            )

        return result

arr1 = ast.literal_eval(input())
arr2 = ast.literal_eval(input())
print(Solution().succeeding_superior_element(arr1, arr2))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[] succeedingSuperiorElement(int[] arr1, int[] arr2) {

            // Array to store the next greater elements for arr1
            int[] nextGreater = new int[arr1.length];
            Arrays.fill(nextGreater, -1);

            // Map to store the last index of each element in arr1
            Map<Integer, Integer> indexMap = new HashMap<>();

            // Stack to help find the next greater element efficiently
            Stack<Integer> stack = new Stack<>();

            // Step 1: Build the next greater elements array for arr1
            // (Traverse in reverse order)
            for (int i = arr1.length - 1; i >= 0; i--) {
                int num = arr1[i];

                // Remove elements from the stack that are smaller than or
                // equal to the current element
                while (!stack.isEmpty() && stack.peek() <= num) {
                    stack.pop();
                }

                // If the stack is not empty, set the next greater element
                if (!stack.isEmpty()) {
                    nextGreater[i] = stack.peek();
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

                // Push the next greater element if found, otherwise -1
                result[i] = indexMap.containsKey(num)
                    ? nextGreater[indexMap.get(num)]
                    : -1;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr1 = parseIntArray(sc.nextLine());
        int[] arr2 = parseIntArray(sc.nextLine());
        System.out.println(Arrays.toString(new Solution().succeedingSuperiorElement(arr1, arr2)));
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


Walk Example 1 — `arr1 = [3, 5, 1, 6, 8, 7]`, `arr2 = [3, 1, 8, 7]`. Scan `arr1` in reverse with a strictly decreasing stack; pop while the top `≤ num`:

```
i=5  num=7   pop none           stack empty → nge[5] = -1   push 7   stack=[7]
i=4  num=8   pop 7 (7≤8)        stack empty → nge[4] = -1   push 8   stack=[8]
i=3  num=6   pop none (8>6)     top=8       → nge[3] = 8    push 6   stack=[8,6]
i=2  num=1   pop none (6>1)     top=6       → nge[2] = 6    push 1   stack=[8,6,1]
i=1  num=5   pop 1 (1≤5)        top=6       → nge[1] = 6    push 5   stack=[8,6,5]
i=0  num=3   pop none (5>3)     top=5       → nge[0] = 5    push 3   stack=[8,6,5,3]

nextGreater = [5, 6, 6, 8, -1, -1]
index_map   = {7:5, 8:4, 6:3, 1:2, 5:1, 3:0}

queries: 3→idx0→5 | 1→idx2→6 | 8→idx4→-1 | 7→idx5→-1
result = [5, 6, -1, -1]
```

The result `[5, 6, -1, -1]` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N + M)** | One amortised `O(N)` stack pass over `arr1` plus `O(M)` lookups for `arr2`. |
| Space | **O(N)** | `nextGreater` array, the index map, and the stack each hold up to `N` entries. |

The stack pass is `O(N)` amortised: each value is pushed once and popped at most once across the whole walk, capping stack operations at `2N`.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr1 = [1]`, `arr2 = [1]` | `[-1]` | No successor exists for the only value. |
| Two descending | `arr1 = [2, 1]`, `arr2 = [2, 1]` | `[-1, -1]` | Nothing greater follows either value. |
| Two ascending | `arr1 = [1, 2]`, `arr2 = [1, 2]` | `[2, -1]` | `1` sees `2`; `2` ends the array. |
| Sorted ascending | `arr1 = [1, 2, 3, 4]`, `arr2 = [1, 3]` | `[2, 4]` | Each value's next-greater is its immediate right neighbour. |
| Sorted descending | `arr1 = [4, 3, 2, 1]`, `arr2 = [4, 1]` | `[-1, -1]` | A strictly decreasing array has no greater successor anywhere. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the canonical next-greater problem and the mirror of *preceding superior element*: build the next-greater array for the full `arr1`, then resolve each `arr2` query as an `O(1)` index-map lookup. The only change from the previous-closest version is the scan direction — walk `arr1` in reverse so a value's previous-greater-in-reverse is its next-greater going forward.

</details>
