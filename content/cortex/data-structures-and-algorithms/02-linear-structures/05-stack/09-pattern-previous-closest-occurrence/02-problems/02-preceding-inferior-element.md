---
title: "Preceding Inferior Element"
summary: "Same as above but inferior = strictly smaller. Maintain an *increasing* monotonic stack; pop while top ≥ current."
prereqs:
  - 09-pattern-previous-closest-occurrence/01-pattern
difficulty: easy
---

# Preceding inferior element

## Problem Statement

Same as above but **inferior** = strictly smaller. Maintain an *increasing* monotonic stack; pop while top `≥` current.

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


<details>
<summary><h2>Intuition</h2></summary>


The structural property is identical to the superior version, with the comparison reversed — each value wants the nearest strictly-**smaller** value to its left. That "nearest qualifying predecessor" shape is still the previous-closest pattern; only the test that disqualifies a candidate flips from greater-than to less-than.

The stack now holds an un-disqualified chain of **previous-smaller candidates** in strictly *increasing* order — bottom smallest, top largest. When a new value arrives, any candidate that matches or exceeds it is popped, because that candidate now sits behind a value no larger and can never be a future element's previous-smaller. The survivor on top is the answer, and the new value is pushed for the elements that follow.

The naive approach breaks the time budget the same way as before. Scanning `arr1` afresh for every `arr2` query is `O(N × M)` time, quadratic when the arrays are comparable in length. One increasing-stack pass computes every previous-smaller in `arr1`, turning each query into an `O(1)` index-map lookup.

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
<summary><h2>Approach</h2></summary>


Mirror the superior version with the comparison flipped to build an *increasing* stack.

1. **Allocate the result holders.** Create `previousSmaller` over `arr1`, filled with `-1`, an empty `stack`, and an empty `value → index` map.
2. **Walk `arr1` left to right.** For each value `num` at index `i`, pop while the stack is non-empty and its top `≥ num`.
3. **Record the survivor.** If the stack is non-empty, set `previousSmaller[i]` to the top — the nearest strictly-smaller predecessor.
4. **Push and index.** Push `num` onto the stack, then store `map[num] = i`.
5. **Answer the queries.** For each value in `arr2`, look up its index and append `previousSmaller[index]`, using `-1` when the value is absent.
6. **Return the result.** One previous-smaller answer per query, in `arr2` order.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
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


# Examples from the problem statement
print(Solution().preceding_inferior_element([3,5,1,6,8,2], [3,1,8,2]))   # [-1, -1, 6, 1]
print(Solution().preceding_inferior_element([5,9,7,8,1], [5,9,7]))       # [-1, 5, 5]

# Edge cases
print(Solution().preceding_inferior_element([1,2,3], [1,2,3]))           # [-1, 1, 2] — ascending
print(Solution().preceding_inferior_element([3,2,1], [3,2,1]))           # [-1, -1, -1] — descending
print(Solution().preceding_inferior_element([5], [5]))                   # [-1] — single element
print(Solution().preceding_inferior_element([2,5,3], [5,3]))             # [2, 2]
print(Solution().preceding_inferior_element([4,1,3], [1,3]))             # [-1, 1]
```

```java run viz=array viz-root=stack viz-kind=stack
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
            Stack<Integer> stack = new Stack<>();

            // Step 1: Build the previous smaller elements array for arr1
            for (int i = 0; i < arr1.length; i++) {
                int num = arr1[i];

                // Remove elements from the stack that are greater than or
                // equal to the current element
                while (!stack.isEmpty() && stack.peek() >= num) {
                    stack.pop();
                }

                // If the stack is not empty, set the previous smaller
                // element
                if (!stack.isEmpty()) {
                    previousSmaller[i] = stack.peek();
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

                // Push the previous smaller element if found, otherwise -1
                result[i] = indexMap.containsKey(num)
                    ? previousSmaller[indexMap.get(num)]
                    : -1;
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{3,5,1,6,8,2}, new int[]{3,1,8,2})
        ));  // [-1, -1, 6, 1]
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{5,9,7,8,1}, new int[]{5,9,7})
        ));  // [-1, 5, 5]

        // Edge cases
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{1,2,3}, new int[]{1,2,3})
        ));  // [-1, 1, 2]
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{3,2,1}, new int[]{3,2,1})
        ));  // [-1, -1, -1]
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{5}, new int[]{5})
        ));  // [-1]
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{2,5,3}, new int[]{5,3})
        ));  // [2, 2]
        System.out.println(Arrays.toString(
            new Solution().precedingInferiorElement(new int[]{4,1,3}, new int[]{1,3})
        ));  // [-1, 1]
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


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

The result `[-1, -1, 6, 1]` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N + M)** | One amortised `O(N)` stack pass over `arr1` plus `O(M)` lookups for `arr2`. |
| Space | **O(N)** | `previousSmaller` array, the index map, and the stack each hold up to `N` entries. |

The flip from greater to smaller changes neither bound — the stack still admits each value once and evicts it at most once.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single element | `arr1 = [5]`, `arr2 = [5]` | `[-1]` | No predecessor exists for the only value. |
| Sorted ascending | `arr1 = [1, 2, 3]`, `arr2 = [1, 2, 3]` | `[-1, 1, 2]` | Each value's nearest smaller predecessor is the one just before it. |
| Sorted descending | `arr1 = [3, 2, 1]`, `arr2 = [3, 2, 1]` | `[-1, -1, -1]` | Each value is the running minimum — nothing smaller precedes it. |
| Shared predecessor | `arr1 = [2, 5, 3]`, `arr2 = [5, 3]` | `[2, 2]` | `2` precedes and stays below both queries. |
| Query in the middle | `arr1 = [4, 1, 3]`, `arr2 = [1, 3]` | `[-1, 1]` | `1` is the running minimum → -1; `3` sees `1` as its nearest smaller. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


What is new here is a single operator flip: the stack becomes *increasing* and pops while the top is `≥` the current value, returning the nearest strictly-smaller predecessor instead of the greater one. Everything else — the two-pass structure, the index map, the `O(N + M)` cost — is unchanged from the superior variant.

</details>