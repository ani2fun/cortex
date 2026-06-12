---
title: "Redundant Parentheses"
summary: "Given a balanced expression s (containing operators, operands, and parentheses), return true if there exists a redundant pair of parentheses — a pair that wraps either nothing or a single operand, contributing no precedence value."
prereqs:
  - 11-pattern-sequence-validation/01-pattern
difficulty: medium
kind: problem
topics: [sequence-validation, stack]
---

# Redundant parentheses

## Problem Statement

Given a balanced expression `s` (containing operators, operands, and parentheses), return `true` if there exists a redundant pair of parentheses — a pair that wraps **either nothing or a single operand**, contributing no precedence value.

## Examples

**Example 1**
```
Input:  s = "((2+3))+7"
Output: true
Explanation: the inner "(2+3)" is a real grouping, but the outer pair
wraps it alone — when its ')' arrives, the matching '(' sits directly
on top with nothing between them. Redundant.
```

**Example 2**
```
Input:  s = "(2+3)"
Output: false
Explanation: a single pair around the operation "2+3". Operators sit
between the '(' and ')', so the pair carries meaning — not redundant.
```

**Example 3**
```
Input:  s = "((2+3)+7)"
Output: false
Explanation: every pair encloses at least one operator. The outer pair
wraps "(2+3)+7", the inner wraps "2+3" — both meaningful.
```

**Example 4**
```
Input:  s = "(())"
Output: true
Explanation: the inner "()" wraps nothing at all. When its ')' arrives,
the top is '(' immediately → redundant empty pair.
```

```quiz
{
  "prompt": "What makes a pair of parentheses redundant?",
  "input": "s = \"((2+3))\"",
  "options": [
    "No operator between its ( and )",
    "It contains nested parentheses",
    "It appears at the end of the string",
    "The count of ( exceeds )"
  ],
  "answer": "No operator between its ( and )"
}
```

## Constraints

- `1 ≤ s.length ≤ 10⁴`
- `s` is a balanced expression containing letters, digits, `+`, `-`, `*`, `/`, `(`, `)`

```python run
class Solution:
    def redundant_parentheses(self, s: str) -> bool:
        # Your code goes here — push every character except ')' onto a stack.
        # On ')', if the top is '(' immediately, the pair is redundant.
        # Otherwise pop to the matching '(' and discard that group.
        return False

s = input()
print("true" if Solution().redundant_parentheses(s) else "false")
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public boolean redundantParentheses(String s) {
            // Your code goes here — push every character except ')' onto a stack.
            // On ')', if the top is '(' immediately, the pair is redundant.
            // Otherwise pop to the matching '(' and discard that group.
            return false;
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().redundantParentheses(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "((2+3))+7" }
  ],
  "cases": [
    { "args": { "s": "((2+3))+7" }, "expected": "true" },
    { "args": { "s": "(())" }, "expected": "true" },
    { "args": { "s": "((a+b))" }, "expected": "true" },
    { "args": { "s": "(2+3)" }, "expected": "false" },
    { "args": { "s": "((2+3)+7)" }, "expected": "false" },
    { "args": { "s": "(a+(b+c))" }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


This is a **sequence-validation** problem with a twist: the input is already balanced, so the question is not *whether* brackets match but *whether a pair carries meaning*. A pair of parentheses is redundant when it wraps either nothing or a single operand — no operator lives between its `(` and `)`. The stack tracks the context between each opener and its closer.

The stack holds **every character not yet resolved by a closer** — operators and operands as well as openers. When a `)` arrives, the run of characters back to its matching `(` is whatever sits on top. The core observation: if the top is `(` the *instant* the `)` arrives, then nothing was pushed between them, so the pair wraps an empty or single-token group and is redundant. Otherwise you pop the inner run down to the `(` and discard it, because that grouping was meaningful.

A naive approach re-parses each parenthesised span to recount its operators, re-reading nested groups repeatedly. The stack avoids that. Meaningful inner groups are popped and discarded the moment their `)` is seen. So when an outer `)` arrives, a `(` directly on top signals that this pair added nothing the inner group had not already enclosed. One pass replaces the repeated re-scanning.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Redundant Parentheses |
|---|---|
| **Q1.** Does the input pair up — openers matched by later closers? | **Yes** — the expression is balanced, so every `)` has a matching `(` earlier. |
| **Q2.** Must a closer match the *most recent* unmatched opener? | **Yes** — a `)` resolves the freshest `(`, and the characters between them are read off the top. |
| **Q3.** Is one pass with `O(1)` work per token enough? | **Mostly** — each character is pushed once and popped once, so the work is `O(1)` amortised per token. |
| **Q4.** Is the answer decided by what the stack holds between a pair? | **Yes** — a `(` directly on top when `)` arrives means an empty/single-token group → redundant. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Push everything except `)`; on `)`, ask whether the pair wrapped anything meaningful.

1. **Handle the trivial pair up front.** Return `false` for `"()"`, the single-pair case the loop cannot flag as redundant.
2. **Initialise an empty stack** of characters.
3. **Walk the string left to right.** Push every character that is not `)` — openers, operators, and operands all go on.
4. **On `)`, check the top first.** If the top is `(`, nothing was pushed since that opener, so the pair is redundant — return `true`.
5. **Otherwise discard the inner run.** Pop characters until the matching `(`, then pop the `(` itself — that grouping was meaningful, so move on.
6. **After the pass, return `false`.** No redundant pair was ever found.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

```python solution time=O(n) space=O(n)
from typing import List

class Solution:
    def redundant_parentheses(self, s: str) -> bool:

        # Edge case for single pair of parentheses
        if s == "()":
            return False

        # Create a stack to store characters
        stack: List[str] = []

        # Iterate through each character in the string
        for ch in s:

            # If the character is a closing parenthesis
            if ch == ")":

                # If top of stack is an opening parenthesis, it's
                # redundant
                if stack and stack[-1] == "(":
                    return True

                # Pop elements until we find the corresponding '('
                while stack and stack[-1] != "(":
                    stack.pop()

                # Pop the '(' as well
                stack.pop()

            # If the character is not a closing parenthesis, push it
            # onto the stack
            else:
                stack.append(ch)

        # No redundant parentheses found
        return False

s = input()
print("true" if Solution().redundant_parentheses(s) else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public boolean redundantParentheses(String s) {

            // Edge case for single pair of parentheses
            if ("()".equals(s)) {
                return false;
            }

            // Create a stack to store characters
            Stack<Character> stack = new Stack<>();

            // Iterate through each character in the string
            for (char ch : s.toCharArray()) {

                // If the character is a closing parenthesis
                if (ch == ')') {

                    // If top of stack is an opening parenthesis, it's
                    // redundant
                    if (!stack.isEmpty() && stack.peek() == '(') {
                        return true;
                    }

                    // Pop elements until we find the corresponding '('
                    while (!stack.isEmpty() && stack.peek() != '(') {
                        stack.pop();
                    }

                    // Pop the '(' as well
                    stack.pop();
                }

                // If the character is not a closing parenthesis, push it
                // onto the stack
                else {
                    stack.push(ch);
                }
            }

            // No redundant parentheses found
            return false;
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().redundantParentheses(s));
    }
}
```

### Dry Run — `s = "((2+3))+7"`

```
s = "((2+3))+7"

'('  push                       → stack (bottom→top): (
'('  push                       → stack: ( (
'2'  push                       → stack: ( ( 2
'+'  push                       → stack: ( ( 2 +
'3'  push                       → stack: ( ( 2 + 3
')'  top='3' ≠ '(' → pop 3,+,2  → top now '(' → pop '(' → stack: (
')'  top='(' immediately        → REDUNDANT → return true ✓
```

The first `)` clears a meaningful group (`2+3` had operators between the parens). The second `)` finds `(` already on top — the outer pair wrapped only the already-grouped inner expression, so it is redundant.

### Complexity Analysis

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | Each character is pushed once and popped at most once across the whole pass — `2N` stack operations. |
| Space | **O(N)** | The stack can hold the full expression before a `)` triggers any popping (e.g. `"(((((..."`). |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty inner pair | `s = "(())"` | `true` | The inner `()` wraps nothing — its `)` finds `(` directly on top. |
| Meaningful single pair | `s = "(a+b)"` | `false` | Operators sit between the parens, so the pair groups a real expression. |
| Double-wrapped expression | `s = "((a+b))"` | `true` | The outer pair adds nothing the inner pair had not grouped. |
| Properly nested | `s = "(a+(b+c))"` | `false` | Each pair encloses at least one operator; none is redundant. |
| Redundant then tail | `s = "((2+3))+7"` | `true` | The outer pair around `(2+3)` is redundant; the `+7` tail does not affect it. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


A pair of parentheses is redundant when its `)` finds the matching `(` directly on top of the stack — nothing meaningful was pushed between them. The new idea over the bracket checker is reading the *content between* a matched pair, not just confirming the pair exists.

</details>
