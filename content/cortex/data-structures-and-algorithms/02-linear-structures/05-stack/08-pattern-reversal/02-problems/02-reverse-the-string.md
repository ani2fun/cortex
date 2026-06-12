---
title: "Reverse the String"
summary: "Given a string s, return its reverse using a stack."
prereqs:
  - 08-pattern-reversal/01-pattern
difficulty: easy
kind: problem
topics: [reversal, stack]
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
Input:  s = "aba"
Output: "aba"
```

```quiz
{
  "prompt": "What does reversing \"hello\" with a stack produce?",
  "input": "s = \"hello\"",
  "options": ["hello", "olleh", "hlloe", "oellh"],
  "answer": "olleh"
}
```

## Constraints

- `1 ≤ s.length ≤ 10⁴`
- `s` consists of printable ASCII characters

```python run viz=array viz-root=stack viz-kind=stack
class Solution:
    def reverse_the_string(self, s: str) -> str:
        # Your code goes here — push every character onto a stack,
        # then pop them all into a result string.
        return s

s = input()                          # the test case's s
print(Solution().reverse_the_string(s))
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public String reverseTheString(String s) {
            // Your code goes here — push every character onto a stack,
            // then pop them all into a result string.
            return s;
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().reverseTheString(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "abcdefgh" }
  ],
  "cases": [
    { "args": { "s": "abcdefgh" }, "expected": "hgfedcba" },
    { "args": { "s": "c" }, "expected": "c" },
    { "args": { "s": "ab" }, "expected": "ba" },
    { "args": { "s": "aba" }, "expected": "aba" },
    { "args": { "s": "12345" }, "expected": "54321" },
    { "args": { "s": "aAbB" }, "expected": "BbAa" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The **structural property** that makes this a reversal problem is that a string is a flat sequence of characters and the task asks for those characters in the opposite order. A stack delivers reverse order for free: push the characters left to right, and the last one pushed is the first one popped. Routing the string through a stack converts "reverse the order" into "load, then unload", with no index math.

The **placement** of the data is two distinct containers. The stack is the temporary holding area — after the load pass it holds every character with the last one (`'h'` for `"abcdefgh"`) on top. The result is a separate growing string that the unload pass appends to. The character is the unit being reversed, so the letters themselves flip; if the problem instead pushed whole words, the same loop would reverse word order and leave letters intact.

What **breaks if you reach for a naive approach**? Nothing breaks for a plain string — reading `s` back-to-front with an index is `O(N)` time and arguably simpler. The stack version is the teaching vehicle: it uses pop-only reads, so the exact same code reverses a true stack ADT, a linked-list-backed stack, or any source that refuses random access. The transferable skill is the load-then-unload shape, not this specific string.

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
<summary><h2>Solution &amp; Analysis</h2></summary>

The textbook two-pass: push every character, then pop until empty into a result string.

```python solution time=O(n) space=O(n)
class Solution:
    def reverse_the_string(self, s: str) -> str:
        stack = []
        result = ""
        for ch in s:
            stack.append(ch)
        while stack:
            result += stack.pop()
        return result

s = input()
print(Solution().reverse_the_string(s))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public String reverseTheString(String s) {
            List<Character> stack = new ArrayList<>();
            for (char ch : s.toCharArray()) stack.add(ch);
            StringBuilder result = new StringBuilder();
            while (!stack.isEmpty())
                result.append(stack.remove(stack.size() - 1));
            return result.toString();
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().reverseTheString(s));
    }
}
```

### Dry Run

Trace Example 1 with `s = "abcdefgh"`. Shown on the shorter prefix `"abc"` for space.

```
Load pass (stack shown bottom→top):
  push 'a' → a
  push 'b' → a b
  push 'c' → a b c     (top is 'c')

Unload pass:
  pop 'c' → result = "c"    stack: a b
  pop 'b' → result = "cb"   stack: a
  pop 'a' → result = "cba"  stack: (empty)

Return "cba".  Full input "abcdefgh" → "hgfedcba" ✓
```

### Complexity Analysis

| | Complexity | Reason |
|---|---|---|
| **Time** | `O(N)` | One push per character in the load pass and one pop per character in the unload pass; both are `O(1)`. |
| **Space** | `O(N)` | The stack holds all `N` characters, and the result string grows to `N` characters. |

### Edge Cases

| Case | What happens |
|---|---|
| Single character (`"c"`) | One push, one pop; the result `"c"` is its own reverse. |
| Two characters (`"ab"`) | Push `a, b`; pop `b, a`; result `"ba"`. |
| Palindrome (`"aba"`) | Characters are never compared; the reversed result `"aba"` reads the same. |
| Mixed case (`"aAbB"`) | Case is preserved per character; result is `"BbAa"`. |
| Digits (`"12345"`) | Digit characters reverse like any others; result `"54321"`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reversing a string is the character-unit instance of the pattern — load every character, unload into a result — and it is the canonical example precisely because the pop-only read pattern survives unchanged on any index-free source.

</details>
