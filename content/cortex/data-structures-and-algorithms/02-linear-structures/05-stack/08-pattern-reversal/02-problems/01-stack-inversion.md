---
title: "Stack Inversion"
summary: "Given a stack s, return a new stack containing the same elements in *reversed* order."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
kind: problem
topics: [reversal, stack]
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

```quiz
{
  "prompt": "For s = [3, 1, 4] (top is 4), what does stack inversion return?",
  "input": "s = [3, 1, 4]",
  "options": ["[3, 1, 4]", "[4, 1, 3]", "[1, 3, 4]", "[4, 3, 1]"],
  "answer": "[4, 1, 3]"
}
```

## Constraints

- `0 ≤ s.length ≤ 10⁴`
- `-10⁵ ≤ s[i] ≤ 10⁵`

```python run viz=array viz-root=reversed_stack viz-kind=stack
import ast

class Solution:
    def stack_inversion(self, s):
        # Your code goes here — pop from s, push onto a new stack
        return []

s = ast.literal_eval(input())           # the test case's s (bottom-to-top)
print(Solution().stack_inversion(s))
```

```java run viz=array viz-root=reversed_stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> stackInversion(List<Integer> s) {
            // Your code goes here — pop from s, push onto a new stack
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        int[] raw = parseIntArray(new Scanner(System.in).nextLine());
        List<Integer> s = new ArrayList<>();
        for (int v : raw) s.add(v);
        System.out.println(new Solution().stackInversion(s));
    }

    // "[9, 5, 1, 2]" → {9, 5, 1, 2} — reads the test case's s
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
    { "id": "s", "label": "s", "type": "int[]", "placeholder": "[9, 5, 1, 2]" }
  ],
  "cases": [
    { "args": { "s": "[9, 5, 1, 2]" }, "expected": "[2, 1, 5, 9]" },
    { "args": { "s": "[7]" }, "expected": "[7]" },
    { "args": { "s": "[]" }, "expected": "[]" },
    { "args": { "s": "[1, 2]" }, "expected": "[2, 1]" },
    { "args": { "s": "[1, 2, 3, 4, 5]" }, "expected": "[5, 4, 3, 2, 1]" },
    { "args": { "s": "[-1, 0, 1]" }, "expected": "[1, 0, -1]" }
  ]
}
```

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

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

```python solution time=O(n) space=O(n)
import ast

class Solution:
    def stack_inversion(self, s):
        reversed_stack = []
        while s:
            top = s[-1]
            s.pop()
            reversed_stack.append(top)
        return reversed_stack

s = ast.literal_eval(input())           # the test case's s
print(Solution().stack_inversion(s))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> stackInversion(List<Integer> s) {
            List<Integer> reversedStack = new ArrayList<>();
            while (!s.isEmpty()) {
                int top = s.get(s.size() - 1);
                s.remove(s.size() - 1);
                reversedStack.add(top);
            }
            return reversedStack;
        }
    }

    public static void main(String[] args) {
        int[] raw = parseIntArray(new Scanner(System.in).nextLine());
        List<Integer> s = new ArrayList<>();
        for (int v : raw) s.add(v);
        System.out.println(new Solution().stackInversion(s));
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

Trace Example 1 with `s = [9, 5, 1, 2]`, written bottom-to-top so `2` is on top.

```
Init: s = [9, 5, 1, 2] (top 2), reversed_stack = []

Iter 1: top = 2 → pop s → s = [9, 5, 1]    → push 2 → reversed_stack = [2]
Iter 2: top = 1 → pop s → s = [9, 5]       → push 1 → reversed_stack = [2, 1]
Iter 3: top = 5 → pop s → s = [9]          → push 5 → reversed_stack = [2, 1, 5]
Iter 4: top = 9 → pop s → s = []           → push 9 → reversed_stack = [2, 1, 5, 9]

s is empty → return [2, 1, 5, 9]  (top is 9) ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | Each of the `N` elements is popped from the input once and pushed onto the output once; both are `O(1)`. |
| **Space** | `O(N)` | The output stack holds a full copy of the `N` elements. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty stack (`[]`) | The `while` guard is false immediately; an empty output stack is returned. |
| Single element (`[7]`) | One iteration moves `7` across; the result `[7]` is its own reverse. |
| Two elements (`[1, 2]`, top `2`) | Pop `2` then `1`; output becomes `[2, 1]` (top `1`). |
| All equal (`[3, 3, 3]`) | Values are never compared; three transfers produce `[3, 3, 3]`. |
| Negatives (`[-1, 0, 1]`, top `1`) | Sign is irrelevant to a transfer; output is `[1, 0, -1]`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Stack inversion is the purest form of the pattern: popping a stack *is* the reversal, so a single pop-from-input, push-to-output transfer reverses it in `O(N)` time and `O(N)` space — no buffer and no indexing, only the LIFO contract.

</details>
