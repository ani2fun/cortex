---
title: "Preceding Superior Element"
summary: "Given two arrays arr1 and arr2 (where arr2 is a subset of arr1 and all elements are unique), return for each value in arr2 its preceding superior element in arr1 — the first strictly-greater element t"
prereqs:
  - 09-pattern-previous-closest-occurrence/01-pattern
difficulty: easy
---

# Preceding superior element

## Problem Statement

Given two arrays `arr1` and `arr2` (where `arr2` is a subset of `arr1` and all elements are unique), return for each value in `arr2` its **preceding superior element** in `arr1` — the first strictly-greater element to its left in `arr1`. Return `-1` for values with no preceding superior.

### Example 1
> -   **Input:** `arr1 = [3, 5, 1, 6, 8, 7]`, `arr2 = [3, 1, 8, 7]`
> -   **Output:** `[-1, 5, -1, 8]`

### Example 2
> -   **Input:** `arr1 = [5, 9, 7, 8, 1]`, `arr2 = [5, 9, 7]`
> -   **Output:** `[-1, -1, 9]`

## Examples

**Example 1**
```
Input:  arr1 = [3, 5, 1, 6, 8, 7], arr2 = [3, 1, 8, 7]
Output: [-1, 5, -1, 8]
Explanation: 3 has nothing greater to its left → -1. 1 sees 5 just before it → 5.
8 is the running maximum at its position → -1. 7 sits right after 8 → 8.
```

**Example 2**
```
Input:  arr1 = [5, 9, 7, 8, 1], arr2 = [5, 9, 7]
Output: [-1, -1, 9]
Explanation: 5 starts the array → -1. 9 is larger than everything before it → -1.
7 has 9 as its nearest strictly-greater predecessor → 9.
```

**Example 3**
```
Input:  arr1 = [4, 1, 2], arr2 = [1, 2]
Output: [4, 4]
Explanation: Both 1 and 2 have 4 as their nearest strictly-greater predecessor.
2 does not knock 4 off the answer because 4 sits to the left of both queries.
```

**Example 4**
```
Input:  arr1 = [5], arr2 = [5]
Output: [-1]
Explanation: A single element has no predecessor at all.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **monotonic-stack** problem is the *previous-greater* query — each value in `arr1` wants the nearest strictly-greater value to its left. That "nearest qualifying predecessor" shape is the exact signal the previous-closest pattern fires on. The queries in `arr2` only re-index answers that already exist inside `arr1`.

The stack holds an un-disqualified chain of **previous-greater candidates** in strictly decreasing order — bottom largest, top smallest. When a new value arrives, any candidate it matches or exceeds is popped, because that candidate now sits behind a value at least as large and can never be a future element's previous-greater. The survivor on top is the answer for the current element, and the new value is pushed for those that follow.

The naive approach re-scans for every query and breaks the time budget. For each value in `arr2` it walks `arr1` left to right, tracking the most recent greater value until it reaches the query's position — `O(N × M)` time, quadratic when `arr2` is as long as `arr1`. The stack pass computes every previous-greater in `arr1` once, so each query becomes an `O(1)` index-map lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Preceding Superior Element |
|---|---|
| **Q1.** Does each position need an answer drawn from elements *before* it? | **Yes** — the previous-greater of each `arr1` index ranges only over earlier indices. |
| **Q2.** Is the answer the *closest* such element, not all of them? | **Yes** — the single nearest strictly-greater predecessor per index. |
| **Q3.** Is the comparison monotone — strictly greater or smaller? | **Yes** — a strict greater-than test drives every pop (decreasing stack). |
| **Q4.** Is the per-element work `O(1)` amortised? | **Yes** — each value is pushed once, popped at most once; the index-map read is `O(1)`. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Two passes:

1. Compute the previous-greater-element array `pge` for `arr1` using the monotonic stack (O(N)).
2. Build a `value → index` map for `arr1`. Then for each query in `arr2`, look up its index and read `pge[index]`.

Total: O(N + M) time, O(N) space.

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Solve the all-positions problem on `arr1` first, then answer `arr2` by lookup.

1. **Allocate the result holders.** Create `previousGreater` over `arr1`, filled with `-1`, an empty `stack`, and an empty `value → index` map.
2. **Walk `arr1` left to right.** For each value `num` at index `i`, pop while the stack is non-empty and its top `≤ num`.
3. **Record the survivor.** If the stack is non-empty, set `previousGreater[i]` to the top — the nearest strictly-greater predecessor.
4. **Push and index.** Push `num` onto the stack, then store `map[num] = i` for the lookup pass.
5. **Answer the queries.** For each value in `arr2`, look up its index in the map and append `previousGreater[index]` to the result, using `-1` when the value is absent.
6. **Return the result.** It holds one previous-greater answer per query, in `arr2` order.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def preceding_superior_element(
        self, arr_1: List[int], arr_2: List[int]
    ) -> List[int]:

        # Array to store the previous greater elements for arr_1
        previous_greater = [-1] * len(arr_1)

        # Map to store the last index of each element in arr_1
        index_map = {}

        # Stack to help find the previous greater element efficiently
        stack = []

        # Step 1: Build the previous greater elements array for arr_1
        for i, num in enumerate(arr_1):

            # Remove elements from the stack that are smaller than or
            # equal to the current element
            while stack and stack[-1] <= num:
                stack.pop()

            # If the stack is not empty, set the previous greater element
            if stack:
                previous_greater[i] = stack[-1]

            # Push the current element onto the stack for future elements
            stack.append(num)

            # Store the index of the current element in the index map
            index_map[num] = i

        # Step 2: Process arr_2 to generate the result
        result = []
        for num in arr_2:

            # Push the previous greater element if found, otherwise -1
            result.append(
                previous_greater[index_map[num]]
                if num in index_map
                else -1
            )

        return result


# Examples from the problem statement
print(Solution().preceding_superior_element([3,5,1,6,8,7], [3,1,8,7]))   # [-1, 5, -1, 8]
print(Solution().preceding_superior_element([5,9,7,8,1], [5,9,7]))       # [-1, -1, 9]

# Edge cases
print(Solution().preceding_superior_element([1,2,3], [1,2,3]))           # [-1, -1, -1] — sorted ascending
print(Solution().preceding_superior_element([3,2,1], [3,2,1]))           # [-1, 3, 2] — sorted descending
print(Solution().preceding_superior_element([5], [5]))                   # [-1] — single element
print(Solution().preceding_superior_element([1,3,2], [3,2]))             # [-1, 3]
print(Solution().preceding_superior_element([4,1,2], [1,2]))             # [4, 4]
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public int[] precedingSuperiorElement(int[] arr1, int[] arr2) {

            // Array to store the previous greater elements for arr1
            int[] previousGreater = new int[arr1.length];
            Arrays.fill(previousGreater, -1);

            // Map to store the last index of each element in arr1
            Map<Integer, Integer> indexMap = new HashMap<>();

            // Stack to help find the previous greater element efficiently
            Stack<Integer> stack = new Stack<>();

            // Step 1: Build the previous greater elements array for arr1
            for (int i = 0; i < arr1.length; i++) {
                int num = arr1[i];

                // Remove elements from the stack that are smaller than or
                // equal to the current element
                while (!stack.isEmpty() && stack.peek() <= num) {
                    stack.pop();
                }

                // If the stack is not empty, set the previous greater
                // element
                if (!stack.isEmpty()) {
                    previousGreater[i] = stack.peek();
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

                // Push the previous greater element if found, otherwise -1
                result[i] = indexMap.containsKey(num)
                    ? previousGreater[indexMap.get(num)]
                    : -1;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{3,5,1,6,8,7}, new int[]{3,1,8,7})
        ));  // [-1, 5, -1, 8]
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{5,9,7,8,1}, new int[]{5,9,7})
        ));  // [-1, -1, 9]

        // Edge cases
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{1,2,3}, new int[]{1,2,3})
        ));  // [-1, -1, -1]
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{3,2,1}, new int[]{3,2,1})
        ));  // [-1, 3, 2]
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{5}, new int[]{5})
        ));  // [-1]
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{1,3,2}, new int[]{3,2})
        ));  // [-1, 3]
        System.out.println(Arrays.toString(
            new Solution().precedingSuperiorElement(new int[]{4,1,2}, new int[]{1,2})
        ));  // [4, 4]
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `arr1 = [3, 5, 1, 6, 8, 7]`, `arr2 = [3, 1, 8, 7]`. The stack stays strictly decreasing; pop while the top `≤ num`:

```
i=0  num=3   pop none           stack empty → pge[0] = -1   push 3   stack=[3]
i=1  num=5   pop 3 (3≤5)        stack empty → pge[1] = -1   push 5   stack=[5]
i=2  num=1   pop none (5>1)     top=5       → pge[2] = 5    push 1   stack=[5,1]
i=3  num=6   pop 1, pop 5       stack empty → pge[3] = -1   push 6   stack=[6]
i=4  num=8   pop 6 (6≤8)        stack empty → pge[4] = -1   push 8   stack=[8]
i=5  num=7   pop none (8>7)     top=8       → pge[5] = 8    push 7   stack=[8,7]

previousGreater = [-1, -1, 5, -1, -1, 8]
index_map       = {3:0, 5:1, 1:2, 6:3, 8:4, 7:5}

queries: 3→idx0→-1 | 1→idx2→5 | 8→idx4→-1 | 7→idx5→8
result = [-1, 5, -1, 8]
```

The result `[-1, 5, -1, 8]` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N + M)** | One amortised `O(N)` stack pass over `arr1` plus `O(M)` lookups for `arr2`. |
| Space | **O(N)** | `previousGreater` array, the index map, and the stack each hold up to `N` entries. |

The stack pass is `O(N)` amortised: each value is pushed once and popped at most once across the whole walk, capping stack operations at `2N`.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr1 = [5]`, `arr2 = [5]` | `[-1]` | No predecessor exists for the only value. |
| Sorted ascending | `arr1 = [1, 2, 3]`, `arr2 = [1, 2, 3]` | `[-1, -1, -1]` | Each value is the running maximum — nothing greater precedes it. |
| Sorted descending | `arr1 = [3, 2, 1]`, `arr2 = [3, 2, 1]` | `[-1, 3, 2]` | Every value's predecessor is strictly greater, so each gets the value just before it. |
| Query in the middle | `arr1 = [1, 3, 2]`, `arr2 = [3, 2]` | `[-1, 3]` | `3` is the maximum so far → -1; `2` sees `3` as its nearest greater. |
| Shared predecessor | `arr1 = [4, 1, 2]`, `arr2 = [1, 2]` | `[4, 4]` | `4` precedes and dominates both queries; `2` does not shadow `4` for index 2. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the canonical previous-greater problem: build the previous-greater array for the full `arr1` with a decreasing monotonic stack, then resolve each `arr2` query as an `O(1)` index-map lookup. The `arr2`-subset framing is the only twist — the core technique is the unmodified previous-closest skeleton.

</details>