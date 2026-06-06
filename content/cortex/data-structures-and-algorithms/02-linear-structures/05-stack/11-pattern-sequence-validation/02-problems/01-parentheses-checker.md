---
title: "Parentheses Checker"
summary: "Given a string s containing only (, ), [, ], {, }, return true iff every bracket is matched and closed in the right order."
prereqs:
  - 11-pattern-sequence-validation/01-pattern
difficulty: easy
---

# Parentheses checker

## Problem Statement

Given a string `s` containing only `(`, `)`, `[`, `]`, `{`, `}`, return `true` iff every bracket is matched and closed in the right order.

### Example 1
> -   **Input:** `s = "()"` → **Output:** `true`

### Example 2
> -   **Input:** `s = "(({}))[]"` → **Output:** `true`

### Example 3
> -   **Input:** `s = "({{)[]"` → **Output:** `false`

## Examples

**Example 1**
```
Input:  s = "()"
Output: true
Explanation: '(' is pushed, then ')' matches the top '(' and pops it.
The stack ends empty, so the string is valid.
```

**Example 2**
```
Input:  s = "(({}))[]"
Output: true
Explanation: openers stack up as ( ( {, then every closer matches its
freshest opener top-down. The stack drains to empty → valid.
```

**Example 3**
```
Input:  s = "({{)[]"
Output: false
Explanation: when ')' arrives the top is '{', not '('. The freshest
unmatched opener is the wrong type, so the string is invalid.
```

**Example 4**
```
Input:  s = "([)]"
Output: false
Explanation: counts balance — one of each bracket — but ')' tries to
close while '[' is the freshest opener. Order, not count, decides validity.
```


<details>
<summary><h2>Intuition</h2></summary>


This is a **sequence-validation** problem because the brackets pair up: every closer demands an opener of the same type earlier in the string, and the pairing must respect order. A closer is legal only against the *most recent* unmatched opener — that "newest-first" requirement is the signal the stack pattern fires on. Counting brackets alone cannot decide validity.

The stack holds the **openers seen so far that have not yet been matched**, with the most recent on top. Each opener is pushed as a pending promise; each closer must redeem the freshest promise, so you check the top and pop it. The top is the only opener a closer is allowed to match, because anything pushed after it must close first. When the scan ends, an empty stack means every opener found its partner.

The naive approach — counting openers and closers — passes strings it should reject. `"([)]"` has one opener and one closer of each kind, so the totals balance, yet it is invalid: `)` closes against an open `[`. A single running depth counter fails the same way, since `"([)]"` and `"()[]"` share the depth trace `1, 2, 1, 0`. Only a structure that remembers opener *types* in newest-on-top order distinguishes them, and that is exactly the stack.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Parentheses Checker |
|---|---|
| **Q1.** Does the input pair up — openers matched by later closers? | **Yes** — every closing bracket expects an opener of the same type earlier in `s`. |
| **Q2.** Must a closer match the *most recent* unmatched opener? | **Yes** — `"([)]"` balances by count but fails because `)` does not match the freshest opener `[`. |
| **Q3.** Is one pass with `O(1)` work per token enough? | **Yes** — each character drives a single push, peek, or pop; no re-scanning. |
| **Q4.** Is validity decided by the stack's contents and final emptiness? | **Yes** — a mismatch fails mid-scan; a non-empty stack at the end means leftover openers. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Push openers; match-and-pop closers; demand an empty stack at the end.

1. **Initialise an empty stack** of characters to hold unmatched openers.
2. **Walk the string left to right.** For each character, decide whether it is an opener or a closer.
3. **Opener → push.** Push `(`, `[`, or `{` onto the stack as a pending match.
4. **Closer → check then pop.** If the stack is empty or its top is not the matching opener for this closer, return `false` immediately. Otherwise pop the matched opener.
5. **After the pass, return `stack` is empty.** A non-empty stack means openers were left unmatched, so the string is invalid.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def is_matching_pair(self, opening: str, closing: str) -> bool:
        return (
            (opening == "(" and closing == ")")
            or (opening == "{" and closing == "}")
            or (opening == "[" and closing == "]")
        )

    def parentheses_checker(self, s: str) -> bool:

        # Create a stack to store the opening parentheses
        stack: List[str] = []

        # Iterate through each character in the string
        for ch in s:

            # If the character is an opening parenthesis, push it onto
            # the stack
            if ch == "(" or ch == "{" or ch == "[":

                # Push opening parentheses onto the stack
                stack.append(ch)

            # If the character is a closing parenthesis
            else:

                # If the stack is empty, the closing parenthesis does
                # not match the corresponding opening parenthesis
                # Return false as the string is invalid
                if not stack or not self.is_matching_pair(stack[-1], ch):
                    return False

                # Remove the corresponding opening parenthesis from the
                # stack
                stack.pop()

        # If the stack is empty at the end, the string is valid
        return not stack


# Examples from the problem statement
print(Solution().parentheses_checker("()"))          # True
print(Solution().parentheses_checker("(({}))[]{"))  # False — extra open
print(Solution().parentheses_checker("({{)[]{"))    # False

# Edge cases
print(Solution().parentheses_checker(""))            # True — empty string is valid
print(Solution().parentheses_checker("("))           # False — unmatched open
print(Solution().parentheses_checker(")"))           # False — unmatched close
print(Solution().parentheses_checker("([{}])"))      # True
print(Solution().parentheses_checker("([)]"))        # False — wrong order
print(Solution().parentheses_checker("{[()]}"))      # True
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        private boolean isMatchingPair(char opening, char closing) {
            return (
                (opening == '(' && closing == ')') ||
                (opening == '{' && closing == '}') ||
                (opening == '[' && closing == ']')
            );
        }

        public boolean parenthesesChecker(String s) {

            // Create a stack to store the opening parentheses
            Stack<Character> stack = new Stack<>();

            // Iterate through each character in the string
            for (char ch : s.toCharArray()) {

                // If the character is an opening parenthesis, push it onto
                // the stack
                if (ch == '(' || ch == '{' || ch == '[') {

                    // Push opening parentheses onto the stack
                    stack.push(ch);
                }

                // If the character is a closing parenthesis
                else {

                    // If the stack is empty, the closing parenthesis does
                    // not match the corresponding opening parenthesis
                    // Return false as the string is invalid
                    if (
                        stack.isEmpty() || !isMatchingPair(stack.peek(), ch)
                    ) {
                        return false;
                    }

                    // Remove the corresponding opening parenthesis from the
                    // stack
                    stack.pop();
                }
            }

            // If the stack is empty at the end, the string is valid
            return stack.isEmpty();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().parenthesesChecker("()"));          // true
        System.out.println(new Solution().parenthesesChecker("(({}))[]{"));  // false
        System.out.println(new Solution().parenthesesChecker("({{)[]{"));    // false

        // Edge cases
        System.out.println(new Solution().parenthesesChecker(""));            // true
        System.out.println(new Solution().parenthesesChecker("("));           // false
        System.out.println(new Solution().parenthesesChecker(")"));           // false
        System.out.println(new Solution().parenthesesChecker("([{}])"));      // true
        System.out.println(new Solution().parenthesesChecker("([)]"));        // false
        System.out.println(new Solution().parenthesesChecker("{[()]}"));      // true
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "()"`. The stack holds unmatched openers; push on an opener, match-and-pop on a closer:

```
s = "()"

'('  opener → push          → stack (bottom→top): (
')'  closer, top='(' matches → pop  → stack: (empty)

end of input, stack empty → return true ✓
```

A longer trace on `s = "(({}))[]"` shows the nesting clearly — openers stack up, then drain top-down:

```
'('  push          → stack: (
'('  push          → stack: ( (
'{'  push          → stack: ( ( {
'}'  top='{' ✓ pop → stack: ( (
')'  top='(' ✓ pop → stack: (
')'  top='(' ✓ pop → stack: (empty)
'['  push          → stack: [
']'  top='[' ✓ pop → stack: (empty)

end of input, stack empty → return true ✓
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** worst, **O(1)** best | One pass over `N` characters with `O(1)` push/peek/pop each; a leading unmatched closer fails on the first token. |
| Space | **O(N)** worst, **O(1)** best | A string of all openers (`"((((("`) pushes every character; an immediate failure pushes nothing. |

The worst case is `O(N)` time and `O(N)` space: when every character is an opener, the single pass pushes all `N` and the stack grows to `N`. The best case is `O(1)` time and `O(1)` space: a string starting with a closer (`")"`) hits the empty-stack check on the first token and returns `false` before pushing anything.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty string | `s = ""` | `true` | No brackets to match; the stack starts and ends empty. |
| Single opener | `s = "("` | `false` | The opener is never matched, so the stack is non-empty at the end. |
| Single closer | `s = ")"` | `false` | The closer finds an empty stack on the first token and fails immediately. |
| Wrong order | `s = "([)]"` | `false` | Counts balance, but `)` does not match the freshest opener `[`. |
| Trailing extra opener | `s = "(({}))[]{"` | `false` | All pairs match, but the final `{` is left unmatched on the stack. |
| Deeply nested | `s = "{[()]}"` | `true` | Each closer matches its freshest opener top-down; the stack drains to empty. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The stack is a matching register: push openers, pop on a matching closer, and return whether it ends empty. The new idea over the generic pattern is the *type-matching* check on the top — a closer must match not just any opener but the correct kind, which a plain depth counter cannot enforce.

</details>