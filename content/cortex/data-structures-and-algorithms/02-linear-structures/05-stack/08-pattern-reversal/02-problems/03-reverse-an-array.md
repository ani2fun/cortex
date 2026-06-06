---
title: "Reverse an Array"
summary: "Given an integer array arr, reverse its elements in place using a stack. Don't return a new array — mutate the input."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
---

# Reverse an array

## Problem Statement

Given an integer array `arr`, reverse its elements **in place** using a stack. Don't return a new array — mutate the input.

## Examples

**Example 1:**
```
Input:  arr = [1, 2, 3, 4, 5, 6]
Output: arr = [6, 5, 4, 3, 2, 1]   (after the call; same array object)
```

**Example 2:**
```
Input:  arr = []
Output: arr = []
```

**Example 3:**
```
Input:  arr = [7]
Output: arr = [7]
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that an array is a flat indexed sequence and the task is to put its elements in the opposite order. A stack supplies that order: push the elements front to back, and they pop back-to-front. The extra constraint here is "in place" — the result must land back in the same array object, not in a fresh one.

The **placement** of the data is one stack plus a write cursor over the original array. The load pass pushes every element, leaving the last element on top. The unload pass introduces a `counter` starting at index `0`: each pop overwrites `arr[counter]` and then `counter` advances. Because the pops arrive in reverse order, writing them into ascending indices `0, 1, 2, …` reverses the array. The element is the unit, so integers reverse exactly like characters did in the string variant.

What **breaks if you reach for a naive approach**? The classic in-place reversal swaps `arr[i]` with `arr[n-1-i]` for the first half, using `O(1)` extra space — strictly better than this stack version on a real array. The stack solution deliberately trades that `O(N)` space to demonstrate the pattern on indexable data. The danger to avoid is reading from the stack *and* indexing the source in the same pass; here the load pass fully fills the stack first, so the overwrite pass never races the reads.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse an Array |
|---|---|
| **Q1.** Does the problem ask for the sequence in opposite order? | **Yes** — the array's elements, reversed in place. |
| **Q2.** Is the input read through one end only (or its unit coarser than an index)? | **Acceptable** — arrays are indexable, but the pattern still routes through pop-only reads from the stack. |
| **Q3.** Are two linear passes (load, unload) enough with no comparison? | **Yes** — push every element, then pop every element into ascending indices; no comparison. |
| **Q4.** Is `O(N)` auxiliary space acceptable? | **Yes** — the stack holds all `N` elements; `O(N)` time, `O(N)` space (the result reuses the input array). |

</details>
<details>
<summary><h2>Approach</h2></summary>


Load the stack with every element, then overwrite the array's indices in order with the pops.

1. **Create an empty stack** of integers.
2. **Load pass.** Iterate over `arr` front to back; push each element. The last element ends on top.
3. **Initialise a `counter` at `0`** — the write index into the original array.
4. **Unload pass.** While the stack is not empty, pop the top element, write it to `arr[counter]`, and increment `counter`.
5. **Return nothing.** The array `arr` has been reversed in place.

</details>
<details>
<summary><h2>Solution</h2></summary>


Same recipe; the destination is the input array itself. Pass 1 pushes; pass 2 overwrites positions 0..n−1 with stack pops.


```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def reverse_an_array(self, arr: List[int]) -> None:

        # Create a stack to store elements of arr
        stack: List[int] = []

        # Pushing elements of arr into the stack
        for num in arr:
            stack.append(num)

        counter: int = 0

        # Popping elements from the stack and storing them back into arr
        # in reverse order
        while stack:
            arr[counter] = stack.pop()
            counter += 1


# Examples from the problem statement
a1 = [1, 2, 3, 4, 5, 6]
Solution().reverse_an_array(a1); print(a1)   # [6, 5, 4, 3, 2, 1]

a2: List[int] = []
Solution().reverse_an_array(a2); print(a2)   # []

# Edge cases
a3 = [7]
Solution().reverse_an_array(a3); print(a3)   # [7] — single element

a4 = [1, 2]
Solution().reverse_an_array(a4); print(a4)   # [2, 1] — two elements

a5 = [5, 5, 5]
Solution().reverse_an_array(a5); print(a5)   # [5, 5, 5] — all same

a6 = [-3, 0, 3]
Solution().reverse_an_array(a6); print(a6)   # [3, 0, -3] — negatives

a7 = [1, 2, 3]
Solution().reverse_an_array(a7); print(a7)   # [3, 2, 1]
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public void reverseAnArray(int[] arr) {

            // Create a stack to store elements of arr
            Stack<Integer> stack = new Stack<>();

            // Pushing elements of arr into the stack
            for (int i = 0; i < arr.length; i++) {
                stack.push(arr[i]);
            }

            int counter = 0;

            // Popping elements from the stack and storing them back into arr
            // in reverse order
            while (!stack.empty()) {
                arr[counter++] = stack.pop();
            }
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        int[] a1 = {1, 2, 3, 4, 5, 6};
        new Solution().reverseAnArray(a1);
        System.out.println(Arrays.toString(a1));   // [6, 5, 4, 3, 2, 1]

        int[] a2 = {};
        new Solution().reverseAnArray(a2);
        System.out.println(Arrays.toString(a2));   // []

        // Edge cases
        int[] a3 = {7};
        new Solution().reverseAnArray(a3);
        System.out.println(Arrays.toString(a3));   // [7]

        int[] a4 = {1, 2};
        new Solution().reverseAnArray(a4);
        System.out.println(Arrays.toString(a4));   // [2, 1]

        int[] a5 = {5, 5, 5};
        new Solution().reverseAnArray(a5);
        System.out.println(Arrays.toString(a5));   // [5, 5, 5]

        int[] a6 = {-3, 0, 3};
        new Solution().reverseAnArray(a6);
        System.out.println(Arrays.toString(a6));   // [3, 0, -3]

        int[] a7 = {1, 2, 3};
        new Solution().reverseAnArray(a7);
        System.out.println(Arrays.toString(a7));   // [3, 2, 1]
    }
}
```

### Dry Run

Trace Example 1 with `arr = [1, 2, 3, 4, 5, 6]`.

```
Load pass — push every element (stack shown bottom→top):
  push 1,2,3,4,5,6 → stack: 1 2 3 4 5 6     (top is 6)

Unload pass — counter starts at 0:
  pop 6 → arr[0] = 6 → arr = [6, 2, 3, 4, 5, 6]   counter = 1
  pop 5 → arr[1] = 5 → arr = [6, 5, 3, 4, 5, 6]   counter = 2
  pop 4 → arr[2] = 4 → arr = [6, 5, 4, 4, 5, 6]   counter = 3
  pop 3 → arr[3] = 3 → arr = [6, 5, 4, 3, 5, 6]   counter = 4
  pop 2 → arr[4] = 2 → arr = [6, 5, 4, 3, 2, 6]   counter = 5
  pop 1 → arr[5] = 1 → arr = [6, 5, 4, 3, 2, 1]   counter = 6

Stack empty → arr = [6, 5, 4, 3, 2, 1] ✓
```

The first pop (`6`, the old last element) lands at index `0`; the last pop (`1`, the old first element) lands at the final index. The intermediate array shows the front overwritten while the back still holds stale values — harmless, because every index is overwritten exactly once.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | One push per element in the load pass and one pop-plus-write per element in the unload pass; each is `O(1)`. |
| **Space** | `O(N)` | The stack holds all `N` elements. The result reuses the input array, so there is no second result array — but the stack is still `O(N)` auxiliary memory. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty array (`[]`) | The load pass pushes nothing; the unload `while` never runs; `arr` stays `[]`. |
| Single element (`[7]`) | One push, one pop into `arr[0]`; the array is unchanged, which is its own reverse. |
| Two elements (`[1, 2]`) | Push `1, 2`; pop `2` into index `0`, `1` into index `1`; `arr = [2, 1]`. |
| All equal (`[5, 5, 5]`) | Values are never compared; the array reads `[5, 5, 5]` after the same three writes. |
| Negatives (`[-3, 0, 3]`) | Sign is irrelevant; `arr` becomes `[3, 0, -3]`. |
| Caller keeps the reference | The function returns `None`/`void`; the caller observes the reversal through the same `arr` object it passed in. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reversing an array in place is the integer-unit instance of the pattern: the unload pass writes pops into ascending indices `0..n-1` of the original array. Here "in place" means no second *result* array, but the stack still costs `O(N)` space.

</details>