---
title: "Reverse the String"
summary: "Given a string s, return its reverse using a stack."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
---

# Reverse the string

## Problem Statement

Given a string `s`, return its reverse using a stack.

## Examples

**Example 1:**
```
Input:  s = "abcdefgh"
Output: "hgfedcba"
```

**Example 2:**
```
Input:  s = "c"
Output: "c"
```

**Example 3:**
```
Input:  s = ""
Output: ""
```


---

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that a string is a flat sequence of characters and the task asks for those characters in the opposite order. A stack delivers reverse order for free: push the characters left to right, and the last one pushed is the first one popped. Routing the string through a stack converts "reverse the order" into "load, then unload", with no index math.

The **placement** of the data is two distinct containers. The stack is the temporary holding area — after the load pass it holds every character with the last one (`'h'` for `"abcdefgh"`) on top. The result is a separate growing string that the unload pass appends to. The character is the unit being reversed, so the letters themselves flip; if the problem instead pushed whole words, the same loop would reverse word order and leave letters intact.

What **breaks if you reach for a naive approach**? Nothing breaks for a plain string — reading `s` back-to-front with an index is `O(N)` time and arguably simpler. The stack version is the teaching vehicle: it uses pop-only reads, so the exact same code reverses a true stack ADT, a linked-list-backed stack, or any source that refuses random access. The transferable skill is the load-then-unload shape, not this specific string.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse the String |
|---|---|
| **Q1.** Does the problem ask for the sequence in opposite order? | **Yes** — return the characters of `s` reversed. |
| **Q2.** Is the input read through one end only (or its unit coarser than an index)? | **Acceptable** — characters are indexable, but the pattern reads pop-only so it ports to index-free sources. |
| **Q3.** Are two linear passes (load, unload) enough with no comparison? | **Yes** — push every character, then pop every character; no character is ever compared. |
| **Q4.** Is `O(N)` auxiliary space acceptable? | **Yes** — the stack holds all `N` characters; `O(N)` time, `O(N)` space. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Load the stack with characters, then unload it into a result string.

1. **Create an empty stack** of characters and an empty result string.
2. **Load pass.** Iterate over `s` left to right; push each character onto the stack. The last character of `s` ends on top.
3. **Unload pass.** While the stack is not empty, pop the top character and append it to the result string.
4. **Return the result string.** It now holds the characters of `s` in reverse order.

</details>
<details>
<summary><h2>Solution</h2></summary>


The textbook two-pass: push every character, then pop until empty into a result string.


```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def reverse_the_string(self, s: str) -> str:

        # Create a stack to store characters
        stack: List[str] = []

        # Create an empty string to store the reversed string
        result: str = ""

        # Push each character into the stack
        for ch in s:
            stack.append(ch)

        # Pop characters from the stack to form the reversed string
        while stack:

            # Append the top character to the result string
            result += stack.pop()

        # Return the reversed string
        return result


# Examples from the problem statement
print(Solution().reverse_the_string("abcdefgh"))   # hgfedcba
print(Solution().reverse_the_string("c"))          # c

# Edge cases
print(Solution().reverse_the_string(""))           # "" — empty string
print(Solution().reverse_the_string("ab"))         # ba — two characters
print(Solution().reverse_the_string("aba"))        # aba — palindrome unchanged
print(Solution().reverse_the_string("12345"))      # 54321 — digits
print(Solution().reverse_the_string("aAbB"))       # BbAa — mixed case
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public String reverseTheString(String s) {

            // Create a stack to store characters
            Stack<Character> stack = new Stack<>();

            // Create an empty string to store the reversed string
            StringBuilder result = new StringBuilder();

            // Push each character into the stack
            for (char ch : s.toCharArray()) {
                stack.push(ch);
            }

            // Pop characters from the stack to form the reversed string
            while (!stack.empty()) {

                // Append the top character to the result string
                result.append(stack.pop());
            }

            // Return the reversed string
            return result.toString();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().reverseTheString("abcdefgh"));   // hgfedcba
        System.out.println(new Solution().reverseTheString("c"));          // c

        // Edge cases
        System.out.println(new Solution().reverseTheString(""));           // "" — empty
        System.out.println(new Solution().reverseTheString("ab"));         // ba
        System.out.println(new Solution().reverseTheString("aba"));        // aba — palindrome
        System.out.println(new Solution().reverseTheString("12345"));      // 54321
        System.out.println(new Solution().reverseTheString("aAbB"));       // BbAa
    }
}
```

### Dry Run

Trace Example 1 with `s = "abcdefgh"`. Shown on the shorter prefix `"abc"` for space; the full string follows the same shape.

```
Load pass — push every character (stack shown bottom→top):
  push 'a' → a
  push 'b' → a b
  push 'c' → a b c        (top is 'c')

Unload pass — pop into result:
  pop 'c' → result = "c"     stack: a b
  pop 'b' → result = "cb"    stack: a
  pop 'a' → result = "cba"   stack: (empty)

Return "cba".  For the full input "abcdefgh" the same process returns "hgfedcba" ✓
```

The last character pushed (`'c'`) is the first popped, so it leads the result; the first character pushed (`'a'`) is popped last and ends the result.

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | One push per character in the load pass and one pop per character in the unload pass; both are `O(1)`. |
| **Space** | `O(N)` | The stack holds all `N` characters, and the result string grows to `N` characters. |

### Edge Cases

| Case | What happens |
|---|---|
| Empty string (`""`) | The load pass pushes nothing; the unload `while` never runs; an empty result is returned. |
| Single character (`"c"`) | One push, one pop; the result `"c"` is its own reverse. |
| Two characters (`"ab"`) | Push `a, b`; pop `b, a`; result `"ba"`. |
| Palindrome (`"aba"`) | Characters are never compared; the reversed result `"aba"` happens to read the same. |
| Mixed case (`"aAbB"`) | Case is preserved per character; result is `"BbAa"`. |
| Digits (`"12345"`) | Digit characters reverse like any others; result `"54321"`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reversing a string is the character-unit instance of the pattern — load every character, unload into a result — and it is the canonical example precisely because the pop-only read pattern survives unchanged on any index-free source.

</details>