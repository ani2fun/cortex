---
title: "Reverse an Array"
summary: "Given an integer array arr, reverse its elements in place using a stack. Don't return a new array — mutate the input."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
kind: problem
topics: [reversal, stack]
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

```quiz
{
  "prompt": "For arr = [1, 2, 3], what is arr after reverse_an_array(arr)?",
  "input": "arr = [1, 2, 3]",
  "options": ["[1, 2, 3]", "[3, 2, 1]", "[2, 1, 3]", "[1, 3, 2]"],
  "answer": "[3, 2, 1]"
}
```

## Constraints

- `0 ≤ arr.length ≤ 10⁴`
- `-10⁵ ≤ arr[i] ≤ 10⁵`

```python run viz=array viz-root=stack viz-kind=stack
import ast

class Solution:
    def reverse_an_array(self, arr) -> None:
        # Your code goes here — push all elements onto a stack,
        # then overwrite arr[0], arr[1], ... with successive pops.
        pass

arr = ast.literal_eval(input())      # the test case's arr
Solution().reverse_an_array(arr)
print(arr)
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public void reverseAnArray(int[] arr) {
            // Your code goes here — push all elements onto a stack,
            // then overwrite arr[0], arr[1], ... with successive pops.
        }
    }

    public static void main(String[] args) {
        int[] arr = parseIntArray(new Scanner(System.in).nextLine());
        new Solution().reverseAnArray(arr);
        System.out.println(Arrays.toString(arr));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 2, 3, 4, 5, 6]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 2, 3, 4, 5, 6]" }, "expected": "[6, 5, 4, 3, 2, 1]" },
    { "args": { "arr": "[]" }, "expected": "[]" },
    { "args": { "arr": "[7]" }, "expected": "[7]" },
    { "args": { "arr": "[1, 2]" }, "expected": "[2, 1]" },
    { "args": { "arr": "[5, 5, 5]" }, "expected": "[5, 5, 5]" },
    { "args": { "arr": "[-3, 0, 3]" }, "expected": "[3, 0, -3]" },
    { "args": { "arr": "[1, 2, 3]" }, "expected": "[3, 2, 1]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that an array is a flat indexed sequence and the task is to put its elements in the opposite order. A stack supplies that order: push the elements front to back, and they pop back-to-front. The extra constraint here is "in place" — the result must land back in the same array object, not in a fresh one.

The **placement** of the data is one stack plus a write cursor over the original array. The load pass pushes every element, leaving the last element on top. The unload pass introduces a `counter` starting at index `0`: each pop overwrites `arr[counter]` and then `counter` advances. Because the pops arrive in reverse order, writing them into ascending indices `0, 1, 2, …` reverses the array. The element is the unit, so integers reverse exactly like characters did in the string variant.

What **breaks if you reach for a naive approach**? The classic in-place reversal swaps `arr[i]` with `arr[n-1-i]` for the first half, using `O(1)` extra space — strictly better than this stack version on a real array. The stack solution deliberately trades that `O(N)` space to demonstrate the pattern on indexable data. The danger to avoid is reading from the stack *and* indexing the source in the same pass; here the load pass fully fills the stack first, so the overwrite pass never races the reads.

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
<summary><h2>Solution &amp; Analysis</h2></summary>

Same recipe; the destination is the input array itself. Pass 1 pushes; pass 2 overwrites positions 0..n−1 with stack pops.

```python solution time=O(n) space=O(n)
import ast

class Solution:
    def reverse_an_array(self, arr) -> None:
        stack = []
        for num in arr:
            stack.append(num)
        counter = 0
        while stack:
            arr[counter] = stack.pop()
            counter += 1

arr = ast.literal_eval(input())
Solution().reverse_an_array(arr)
print(arr)
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public void reverseAnArray(int[] arr) {
            List<Integer> stack = new ArrayList<>();
            for (int num : arr) stack.add(num);
            int counter = 0;
            while (!stack.isEmpty())
                arr[counter++] = stack.remove(stack.size() - 1);
        }
    }

    public static void main(String[] args) {
        int[] arr = parseIntArray(new Scanner(System.in).nextLine());
        new Solution().reverseAnArray(arr);
        System.out.println(Arrays.toString(arr));
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

### Dry Run

Trace Example 1 with `arr = [1, 2, 3, 4, 5, 6]`.

```
Load pass (stack shown bottom→top):
  push 1,2,3,4,5,6 → stack: 1 2 3 4 5 6     (top is 6)

Unload pass — counter starts at 0:
  pop 6 → arr[0] = 6   counter = 1
  pop 5 → arr[1] = 5   counter = 2
  pop 4 → arr[2] = 4   counter = 3
  pop 3 → arr[3] = 3   counter = 4
  pop 2 → arr[4] = 2   counter = 5
  pop 1 → arr[5] = 1   counter = 6

Stack empty → arr = [6, 5, 4, 3, 2, 1] ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | One push per element in the load pass and one pop-plus-write per element in the unload pass; each is `O(1)`. |
| **Space** | `O(N)` | The stack holds all `N` elements. The result reuses the input array, but the stack is still `O(N)` auxiliary memory. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty array (`[]`) | The load pass pushes nothing; the unload `while` never runs; `arr` stays `[]`. |
| Single element (`[7]`) | One push, one pop into `arr[0]`; the array is unchanged. |
| Two elements (`[1, 2]`) | Push `1, 2`; pop `2` into index `0`, `1` into index `1`; `arr = [2, 1]`. |
| All equal (`[5, 5, 5]`) | Values are never compared; the array reads `[5, 5, 5]` after the same three writes. |
| Negatives (`[-3, 0, 3]`) | Sign is irrelevant; `arr` becomes `[3, 0, -3]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reversing an array in place is the integer-unit instance of the pattern: the unload pass writes pops into ascending indices `0..n-1` of the original array. Here "in place" means no second *result* array, but the stack still costs `O(N)` space.

</details>
