---
title: "Balanced Span"
summary: "Given a string s of ( and ), return the length of the longest valid (balanced) parentheses substring."
prereqs:
  - 11-pattern-sequence-validation/01-pattern
difficulty: medium
---

# Balanced span

## Problem Statement

Given a string `s` of `(` and `)`, return the length of the **longest valid (balanced) parentheses substring**.

### Example 1
> -   **Input:** `s = "((()()"` → **Output:** `4` (`"()()"`)

### Example 2
> -   **Input:** `s = "(()())(()"` → **Output:** `6` (`"(()())"`)

### Example 3
> -   **Input:** `s = "(((("` → **Output:** `0`

## Examples

**Example 1**
```
Input:  s = "((()()"
Output: 4
Explanation: the longest valid run is "()()" — positions 2..5. The two
leading '(' are never closed, so the run starts after them.
```

**Example 2**
```
Input:  s = "(()())(()"
Output: 6
Explanation: "(()())" spans positions 0..5 — a fully balanced block.
The trailing "(()" is incomplete and cannot extend the answer.
```

**Example 3**
```
Input:  s = "(((("
Output: 0
Explanation: no closer ever arrives, so no valid substring exists.
```

**Example 4**
```
Input:  s = ")()("
Output: 2
Explanation: the leading ')' resets the sentinel; "()" at positions 1..2
is the longest valid run; the trailing '(' is unmatched.
```


<details>
<summary><h2>Intuition</h2></summary>


This is a **sequence-validation** problem, but the answer is a *length*, not a yes/no — the longest contiguous run of correctly matched brackets. Validity is still decided by matching closers to the most recent openers; the new demand is measuring how far the current valid run stretches. That needs positions, so the stack stores **indices** rather than characters.

The stack holds the index of every unmatched `(`, plus a sentinel index at the bottom. The sentinel — pre-pushed as `-1` — marks "one position before the current valid run". The core trick: after popping on a `)`, the new top is the index just before the run that this closer extends, so `i − stack.top()` is the run's current length. When a `)` empties the stack, no valid run can cross that closer, so its own index becomes a fresh sentinel for everything after it.

The naive approach checks every substring for balance — `O(N³)` time across all start/end pairs with an `O(N)` validity test, or `O(N²)` with running counts. Both re-examine overlapping spans repeatedly. The index stack computes the longest valid length in one pass: each closer that matches immediately measures its run against the boundary on top, with no substring re-checking.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Balanced Span |
|---|---|
| **Q1.** Does the input pair up — openers matched by later closers? | **Yes** — each `)` matches the most recent unmatched `(`; only matched pairs extend a valid run. |
| **Q2.** Must a closer match the *most recent* unmatched opener? | **Yes** — order decides which `(` a `)` closes, and therefore where the current run begins. |
| **Q3.** Is one pass with `O(1)` work per token enough? | **Yes** — each index is pushed once and popped once; the span read is `O(1)`. |
| **Q4.** Is the answer decided by what the stack holds — here, boundary indices? | **Yes** — the top is "one before the current run", so `i − stack.top()` yields the length. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Push indices, seed a sentinel, and measure each valid run against the boundary on top.

1. **Initialise the stack with a sentinel `-1`** and a `maxLength` of `0`. The sentinel marks the position before any run.
2. **Walk the string by index `i`**, classifying each character as `(` or `)`.
3. **`(` → push its index `i`.** It is an unmatched opener and a potential run boundary.
4. **`)` → pop.** This closer consumes the freshest unmatched opener (or the sentinel).
5. **If the stack is now empty, push `i` as a new sentinel.** No valid run can span this unmatched closer, so it becomes the new left boundary.
6. **Otherwise measure the run.** Set `maxLength = max(maxLength, i − stack.top())`, where the top is one index before the current valid run.
7. **After the pass, return `maxLength`** — the length of the longest valid substring.

</details>
<details>
<summary><h2>Approach — index stack with sentinel</h2></summary>


The trick is to push **indices** (not characters), starting with a sentinel `-1` at the bottom. The top of the stack always represents *the index just before the current valid substring started*. When we hit `(`: push its index. When we hit `)`: pop. If the stack is now empty (we popped the sentinel), push the current index as a *new sentinel* (no valid substring can include it). Otherwise, the new top is "one before the current valid run", so the current run length is `i − stack.top()`.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
    INIT["push -1 (sentinel)"] --> R["read s[i]"]
    R --> Q{"s[i] == '('?"}
    Q -->|"yes"| P["push i"]
    Q -->|"no (closer)"| POP["pop"]
    POP --> CHK{"stack empty?"}
    CHK -->|"yes"| RESET["push i<br/>(new sentinel — no valid run<br/>can cross this point)"]
    CHK -->|"no"| LEN["len = i - stack.top()<br/>maxLen = max(maxLen, len)"]
    P --> R; RESET --> R; LEN --> R
```

<p align="center"><strong>Balanced span — index stack with sentinel <code>-1</code>. The top is always "one before the current valid run". Pop on closer; if the stack drops to empty, the current index becomes the new sentinel (no run can cross an unmatched closer).</strong></p>

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def balanced_span(self, s: str) -> int:
        stack = []
        max_length = 0

        # Push -1 to handle base case when there's no match
        stack.append(-1)

        for i in range(len(s)):

            # If the character is an opening bracket push its index
            # to the stack
            if s[i] == "(":

                # Push index of '('
                stack.append(i)

            # If the character is a closing bracket
            else:

                # Pop the last element
                stack.pop()

                # Push the current index if the stack is empty
                if not stack:
                    stack.append(i)

                # Otherwise, calculate the length of the valid substring
                else:
                    max_length = max(max_length, i - stack[-1])

        return max_length


# Examples from the problem statement
print(Solution().balanced_span("((()()"))    # 4
print(Solution().balanced_span("(()())(()")) # 6
print(Solution().balanced_span("(((("))      # 0

# Edge cases
print(Solution().balanced_span(""))          # 0 — empty string
print(Solution().balanced_span("()"))        # 2
print(Solution().balanced_span(")()"))       # 2
print(Solution().balanced_span("(()"))       # 2
print(Solution().balanced_span("()()"))      # 4
print(Solution().balanced_span("))))"))      # 0
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public int balancedSpan(String s) {
            Stack<Integer> stack = new Stack<>();
            int maxLength = 0;

            // Push -1 to handle base case when there's no match
            stack.push(-1);

            for (int i = 0; i < s.length(); ++i) {

                // If the character is an opening bracket push the index
                // to the stack
                if (s.charAt(i) == '(') {

                    // Push index of '('
                    stack.push(i);
                }

                // If the character is a closing bracket
                else {

                    // Pop the last element
                    stack.pop();

                    // Push the current index if the stack is empty
                    if (stack.isEmpty()) {
                        stack.push(i);
                    }

                    // Otherwise, calculate the length of the valid substring
                    else {
                        maxLength = Math.max(maxLength, i - stack.peek());
                    }
                }
            }

            return maxLength;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().balancedSpan("((()()"));    // 4
        System.out.println(new Solution().balancedSpan("(()())(()"));  // 6
        System.out.println(new Solution().balancedSpan("(((("));       // 0

        // Edge cases
        System.out.println(new Solution().balancedSpan(""));           // 0
        System.out.println(new Solution().balancedSpan("()"));         // 2
        System.out.println(new Solution().balancedSpan(")()"));        // 2
        System.out.println(new Solution().balancedSpan("(()"));        // 2
        System.out.println(new Solution().balancedSpan("()()"));       // 4
        System.out.println(new Solution().balancedSpan("))))"));       // 0
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Three lessons:

1. **A stack is a matching memory.** Push openers, pop on closers, demand empty at the end. It's the simplest correct way to validate any LIFO-paired sequence — brackets, HTML tags, JSON nesting, function-call frames.
2. **Indices, not characters, when length matters.** Storing indices on the stack lets you measure spans (balanced-span), compute widths (histogram), and resolve answers retroactively (next-greater).
3. **A sentinel `-1` makes the boundary case disappear.** Pre-pushing `-1` on the balanced-span stack means *every* `i − stack.top()` calculation works even at the start of input, with no special-casing.

> *Coming up — the **linear evaluation** pattern. The last problem-solving pattern in this section. It's the umbrella term for stack-based algorithms that build up an *answer* one element at a time by repeatedly popping until a condition is met, then pushing. Score-of-parentheses, decode-string, simplified-Unix-paths, and the asteroid collision problem all fall here.*

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "((()()"`. The stack stores indices, seeded with sentinel `-1`; the top is always "one before the current valid run":

```
s = "((()()"        stack=[-1]  max=0
     012345

i=0 '('  push 0          → stack: [-1, 0]
i=1 '('  push 1          → stack: [-1, 0, 1]
i=2 '('  push 2          → stack: [-1, 0, 1, 2]
i=3 ')'  pop 2           → stack: [-1, 0, 1]   len = 3 - 1 = 2   max=2
i=4 '('  push 4          → stack: [-1, 0, 1, 4]
i=5 ')'  pop 4           → stack: [-1, 0, 1]   len = 5 - 1 = 4   max=4

return max = 4 ✓
```

The two leading `(` at indices `0` and `1` are never closed, so they stay on the stack as the run boundary. The valid run `"()()"` spans indices `2..5`, measured as `5 − 1 = 4`.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | One pass over `N` characters; each index is pushed once and popped at most once. |
| Space | **O(N)** | The stack holds the sentinel plus every unmatched opener index — up to `N + 1` for an all-opener string. |

The runtime is `O(N)` time: a single index-walk with `O(1)` push, pop, and span arithmetic per character. The space is `O(N)`: an all-opener input (`"(((("`) pushes every index on top of the sentinel, so the stack grows to `N + 1`. There is no second pass and no substring re-checking.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty string | `s = ""` | `0` | No characters, so no valid run; `maxLength` stays `0`. |
| All openers | `s = "(((("` | `0` | No closer ever arrives, so no pair is matched. |
| All closers | `s = "))))"` | `0` | Each `)` empties the stack and re-seeds a sentinel; no run forms. |
| Leading closer | `s = ")()"` | `2` | The first `)` resets the sentinel; `"()"` at indices `1..2` gives length `2`. |
| Incomplete tail | `s = "(()"` | `2` | The inner `"()"` scores `2`; the outer `(` is never closed. |
| Adjacent runs | `s = "()()"` | `4` | Both pairs share the sentinel boundary, so the run measures across both as `4`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Storing *indices* with a sentinel `-1` turns validity into measurement: the top is always one position before the current valid run, so `i − stack.top()` reads off its length in `O(1)`. The new idea over the bracket checker is using the stack to compute a span, not just to confirm matching.

</details>