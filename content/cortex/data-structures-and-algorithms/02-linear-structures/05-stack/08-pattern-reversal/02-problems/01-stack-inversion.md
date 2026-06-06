---
title: "Stack Inversion"
summary: "Given a stack s, return a new stack containing the same elements in *reversed* order."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
---

# Stack inversion

## Problem Statement

Given a stack `s`, return a new stack containing the same elements in *reversed* order. The stack is written bottom-to-top, so in `s = [9, 5, 1, 2]` the value `2` is on top.

## Examples

**Example 1:**
```
Input:  s = [9, 5, 1, 2]   (top is 2)
Output: [2, 1, 5, 9]       (top is 9)
```

**Example 2:**
```
Input:  s = [7]
Output: [7]
```

**Example 3:**
```
Input:  s = []
Output: []
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that a stack only exposes its top, so the only legal read is a pop — and popping a stack already yields its elements in reverse order. The input `[9, 5, 1, 2]` has `2` on top, so the pops arrive as `2, 1, 5, 9`. There is no comparison and no index arithmetic; the order flips purely because of the Last In, First Out contract.

The **placement** of the data is the whole trick: pop from the input and push directly onto a second, output stack. The first element popped from the input (its old top, `2`) is the first pushed onto the output, so it lands at the *bottom* of the output. The last element popped (the input's old bottom, `9`) is pushed last and ends on top. One element is in flight at any moment — there is no buffer and no auxiliary list, only a single transfer loop between two stacks.

What **breaks if you reach for a naive approach**? You might try to index the stack as if it were an array and read `s[0], s[1], ...` into the output. That defeats the exercise: a real stack ADT has no index operator, so the code would not port to an actual stack. You might also pop into a temporary list and re-push — that works but adds an `O(N)` buffer the single transfer never needs. The clean move is pop-from-`s`, push-to-output, nothing in between.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Stack Inversion |
|---|---|
| **Q1.** Does the problem ask for the sequence in opposite order? | **Yes** — the same elements, top-to-bottom reversed into a new stack. |
| **Q2.** Is the input read through one end only (or its unit coarser than an index)? | **Yes** — a stack exposes only its top; pop is the only legal read, and pop already reverses. |
| **Q3.** Are two linear passes (load, unload) enough with no comparison? | **One pass here** — the load and unload collapse into a single pop-then-push transfer; still no comparison. |
| **Q4.** Is `O(N)` auxiliary space acceptable? | **Yes** — the output stack holds all `N` elements; `O(N)` time, `O(N)` space. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Two stacks, one transfer loop. Pop everything from the input and push onto the output — *that single transfer reverses the order, because the topmost element of the input is pushed first onto the output, ending up at the bottom*.

1. **Create an empty output stack** to hold the reversed elements.
2. **While the input stack is not empty,** read its top element, pop it off the input, and push it onto the output stack.
3. **Return the output stack.** The input's old top is now at the output's bottom and the input's old bottom is on the output's top — the stack is reversed.

```d2
direction: right

inp: "input stack" {
  grid-rows: 4
  grid-gap: 0
  i1: "2 ← top"
  i2: "1"
  i3: "5"
  i4: "9 ← bot"
}

out: "output stack" {
  grid-rows: 4
  grid-gap: 0
  o1: "9 ← top"
  o2: "5"
  o3: "1"
  o4: "2 ← bot"
}

inp -> out: "pop, push"
```

<p align="center"><strong>Stack inversion — pop the input top, push to output. The first popped item lands at the bottom of the output, which is exactly where it started in the input. The whole stack flips.</strong></p>

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=reversed_stack viz-kind=stack
from typing import List

class Solution:
    def stack_inversion(self, s: List[int]) -> List[int]:
        reversed_stack: List[int] = []

        # Transfer elements from original stack to reversed stack
        while s:

            # Get the top element from the original stack
            top = s[-1]

            # Remove the top element from the original stack
            s.pop()

            # Push the element onto the reversed stack
            reversed_stack.append(top)

        # Return the reversed stack
        return reversed_stack


# Example from the problem statement
print(Solution().stack_inversion([9, 5, 1, 2]))     # [2, 1, 5, 9]

# Edge cases
print(Solution().stack_inversion([]))               # [] — empty stack
print(Solution().stack_inversion([7]))              # [7] — single element
print(Solution().stack_inversion([1, 2]))           # [2, 1] — two elements
print(Solution().stack_inversion([3, 3, 3]))        # [3, 3, 3] — all same
print(Solution().stack_inversion([1, 2, 3, 4, 5])) # [5, 4, 3, 2, 1]
print(Solution().stack_inversion([-1, 0, 1]))       # [1, 0, -1] — negatives
```

```java run viz=array viz-root=reversed_stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public Stack<Integer> stackInversion(Stack<Integer> s) {
            Stack<Integer> reversedStack = new Stack<>();

            // Transfer elements from original stack to reversed stack
            while (!s.empty()) {

                // Get the top element from the original stack
                int top = s.peek();

                // Remove the top element from the original stack
                s.pop();

                // Push the element onto the reversed stack
                reversedStack.push(top);
            }

            // Return the reversed stack
            return reversedStack;
        }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        Stack<Integer> s1 = new Stack<>();
        for (int v : new int[]{9, 5, 1, 2}) s1.push(v);
        System.out.println(new Solution().stackInversion(s1));     // [2, 1, 5, 9]

        // Edge cases
        Stack<Integer> s2 = new Stack<>();
        System.out.println(new Solution().stackInversion(s2));     // [] — empty

        Stack<Integer> s3 = new Stack<>();
        s3.push(7);
        System.out.println(new Solution().stackInversion(s3));     // [7]

        Stack<Integer> s4 = new Stack<>();
        s4.push(1); s4.push(2);
        System.out.println(new Solution().stackInversion(s4));     // [1, 2] — top was 2

        Stack<Integer> s5 = new Stack<>();
        for (int v : new int[]{1, 2, 3, 4, 5}) s5.push(v);
        System.out.println(new Solution().stackInversion(s5));     // [1, 2, 3, 4, 5]

        Stack<Integer> s6 = new Stack<>();
        for (int v : new int[]{-1, 0, 1}) s6.push(v);
        System.out.println(new Solution().stackInversion(s6));     // [-1, 0, 1]
    }
}
```


> **Complexity** — Time: **O(N)** | Space: **O(N)**.

### Dry Run

Trace Example 1 with `s = [9, 5, 1, 2]`, written bottom-to-top so `2` is on top.

```
Init: s = [9, 5, 1, 2] (top 2), reversed_stack = []

Iter 1: top = s[-1] = 2 → pop s → s = [9, 5, 1]    → push 2 → reversed_stack = [2]
Iter 2: top = s[-1] = 1 → pop s → s = [9, 5]       → push 1 → reversed_stack = [2, 1]
Iter 3: top = s[-1] = 5 → pop s → s = [9]          → push 5 → reversed_stack = [2, 1, 5]
Iter 4: top = s[-1] = 9 → pop s → s = []           → push 9 → reversed_stack = [2, 1, 5, 9]

s is empty → return reversed_stack = [2, 1, 5, 9]  (top is 9) ✓
```

The input's old top (`2`) was pushed first, so it sits at the bottom of the result; the old bottom (`9`) was pushed last and is now on top.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | Each of the `N` elements is popped from the input once and pushed onto the output once; both are `O(1)`. |
| **Space** | `O(N)` | The output stack holds a full copy of the `N` elements. The input is drained as the output fills, but the two never overlap by more than the whole set. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty stack (`[]`) | The `while` guard is false immediately; an empty output stack is returned. |
| Single element (`[7]`) | One iteration moves `7` across; the result `[7]` is its own reverse. |
| Two elements (`[1, 2]`, top `2`) | Pop `2` then `1`; output becomes `[2, 1]` (top `1`). |
| All equal (`[3, 3, 3]`) | Values are never compared; three transfers produce `[3, 3, 3]`, indistinguishable by value but reversed by identity. |
| Negatives (`[-1, 0, 1]`, top `1`) | Sign is irrelevant to a transfer; output is `[1, 0, -1]`. |
| Mutates the input | The loop pops `s` to empty — the caller's input stack is consumed. Copy `s` first if it must survive. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Stack inversion is the purest form of the pattern: popping a stack *is* the reversal, so a single pop-from-input, push-to-output transfer reverses it in `O(N)` time and `O(N)` space — no buffer and no indexing, only the LIFO contract.

</details>