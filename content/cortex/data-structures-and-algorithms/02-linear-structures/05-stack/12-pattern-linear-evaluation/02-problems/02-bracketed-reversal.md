---
title: "Bracketed Reversal"
summary: "Given a string of letters and [/] brackets, reverse the substring inside each pair of brackets and return the result. Brackets nest."
prereqs:
  - 12-pattern-linear-evaluation/01-pattern
difficulty: medium
---

# Bracketed Reversal

## Problem Statement

Given a string of letters and `[`/`]` brackets, **reverse the substring inside each pair of brackets** and return the result. Brackets nest.

### Example 1
> -   **Input:** `s = "a[bcd]e"` → **Output:** `"adcbe"`

### Example 2
> -   **Input:** `s = "abcd[ef[gh]i]j"` → **Output:** `"abcdihgfej"`

### Example 3
> -   **Input:** `s = "abcdefghij"` → **Output:** `"abcdefghij"`

## Examples

**Example 1**
```
Input:  s = "a[bcd]e"
Output: "adcbe"
Explanation: 'a' stays. Inside the brackets, "bcd" reverses to "dcb".
'e' stays. Result: a + dcb + e = "adcbe".
```

**Example 2**
```
Input:  s = "abcd[ef[gh]i]j"
Output: "abcdihgfej"
Explanation: the inner "[gh]" reverses to "hg" first, giving "ef" + "hg"
+ "i" = "efhgi" inside the outer brackets, which reverses to "ighfe".
With "abcd" before and "j" after: "abcd" + "ighfe" + "j".
```

**Example 3**
```
Input:  s = "abcdefghij"
Output: "abcdefghij"
Explanation: no brackets, so nothing reverses. Every letter is pushed
and the stack joins back to the original string.
```

**Example 4**
```
Input:  s = "[[ab]]"
Output: "ab"
Explanation: the inner "[ab]" reverses to "ba", then the outer pair
reverses "ba" back to "ab" — a double reversal cancels out.
```


<details>
<summary><h2>Intuition</h2></summary>


This is a **linear-evaluation** problem because the string is scanned once and each `]` folds the substring built since its matching `[`. Brackets nest, so an inner group must be fully reversed before the outer group sees it — the classic "evaluate the freshest pending chunk first" shape. The stack parks each outer context while the inner group resolves.

The stack holds **the characters and `[` markers not yet folded**, with the freshest on top. A letter or `[` is pushed as it arrives. When `]` fires, the run back to the nearest `[` is exactly the substring to reverse, and it sits right on top. The trick is in *how* you pop it: appending characters in pop order builds the reversed string for free, because the stack returns them last-in-first-out — the character pushed last comes out first and lands at the front of the reversed result.

A naive approach scans for each `[...]` pair, reverses it in place, and rescans for nested brackets — re-reading inner groups on every outer pass, which costs `O(N²)` time. The stack avoids that: an inner `]` folds and pushes its reversed string back as one token, so the outer `]` reverses across that already-reversed unit without re-reading its characters. One pass replaces the repeated rescans.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Bracketed Reversal |
|---|---|
| **Q1.** Is the input a single linear sequence scanned once? | **Yes** — one left-to-right walk over the characters of `s`. |
| **Q2.** Does some token defer work — open a group awaiting a closer? | **Yes** — every `[` opens a substring whose reversal waits until its matching `]`. |
| **Q3.** Does a trigger fold only the *most recent* pending chunk? | **Yes** — `]` reverses the run back to the nearest `[`, which is always on top. |
| **Q4.** Is the answer read off the stack at end-of-input? | **Yes** — the surviving tokens, concatenated bottom-to-top, are the result. |

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Push letters and `[`; on `]`, pop-while-appending to reverse the inner substring, then push it back.

1. **Initialise an empty stack** holding characters and reversed-substring tokens.
2. **Walk the string left to right.**
3. **Letter or `[` → push.** Both go on the stack as pending context.
4. **`]` → fold.** Pop characters into a result string *as you pop them* — pop order is reverse order, so this builds the reversed substring directly.
5. **Discard the matching `[`.** Pop it off and throw it away; it was only a marker.
6. **Push the reversed substring back** as a single token, so the next `]` can reverse across it.
7. **After the pass, concatenate the stack** bottom-to-top and return it.

</details>
<details>
<summary><h2>Approach</h2></summary>


Push characters and `[` onto a stack. On `]`, pop characters until you hit `[` — but **append them as you pop**, which builds the reversed substring naturally. Pop the `[`, push the reversed substring back as a single string token. Final answer = concatenate the stack bottom-to-top.

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
    R["read char"] --> Q{"type?"}
    Q -->|"letter or '['"| P["push as string"]
    Q -->|"']'"| EVAL["pop chars until '['<br/>(append as you pop = reversed)<br/>discard '['<br/>push reversed string"]
    P --> R; EVAL --> R
    R -->|"end"| OUT["concatenate stack bottom-to-top"]
```

<p align="center"><strong>Bracketed reversal — popping while appending naturally builds the reversed substring (the topmost char comes out first and goes to the front of the result).</strong></p>

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=stack viz-kind=stack
from typing import List

class Solution:
    def bracketed_reversal(self, s: str) -> str:

        # Stack to store characters and decoded parts
        stack: List[str] = []

        i: int = 0
        while i < len(s):

            # If the character is '[' or a letter, push it as a string
            if s[i] == "[" or s[i].isalpha():
                stack.append(s[i])

            # If the character is ']', it indicates the end of a
            # bracketed section
            else:

                # Variable to store the substring inside the brackets
                reversed_str = ""

                # Pop elements from the stack until we reach '['
                while stack and stack[-1] != "[":

                    # Build substring in reversed order
                    reversed_str += stack.pop()

                # Remove the '[' from the stack
                if stack:
                    stack.pop()

                # Push the reversed substring back onto the stack
                stack.append(reversed_str)

            i += 1

        # Return the final decoded string
        return "".join(stack)


# Examples from the problem statement
print(Solution().bracketed_reversal("a[bcd]e"))       # adcbe
print(Solution().bracketed_reversal("abcd[ef[gh]i]j")) # abcdihgfej
print(Solution().bracketed_reversal("abcdefghij"))     # abcdefghij

# Edge cases
print(Solution().bracketed_reversal(""))               # ''
print(Solution().bracketed_reversal("[a]"))            # a
print(Solution().bracketed_reversal("[ab]"))           # ba
print(Solution().bracketed_reversal("[[ab]]"))         # ab — double nesting reverses back
print(Solution().bracketed_reversal("x[y[z]]"))        # xzy
```

```java run viz=array viz-root=stack viz-kind=stack
import java.util.*;

public class Main {
    static class Solution {
        public String bracketedReversal(String s) {

            // Stack to store characters and decoded parts
            Stack<String> stack = new Stack<>();

            for (int i = 0; i < s.length(); i++) {

                // If the character is '[' or a letter, push it as a string
                if (s.charAt(i) == '[' || Character.isLetter(s.charAt(i))) {
                    stack.push(String.valueOf(s.charAt(i)));
                }

                // If the character is ']', it indicates the end of a
                // bracketed section
                else {

                    // Variable to store the substring inside the brackets
                    StringBuilder reversedStr = new StringBuilder();

                    // Pop elements from the stack until we reach '['
                    while (!stack.isEmpty() && !stack.peek().equals("[")) {

                        // Build substring in reversed order
                        reversedStr.append(stack.pop());
                    }

                    // Remove the '[' from the stack
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }

                    // Push the reversed substring back onto the stack
                    stack.push(reversedStr.toString());
                }
            }

            // Collect the final result by popping from the stack
            StringBuilder result = new StringBuilder();
            while (!stack.isEmpty()) {

                // Prepend the elements to the result string
                result.insert(0, stack.pop());
            }

            // Return the final decoded string
            return result.toString();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().bracketedReversal("a[bcd]e"));        // adcbe
        System.out.println(new Solution().bracketedReversal("abcd[ef[gh]i]j")); // abcdihgfej
        System.out.println(new Solution().bracketedReversal("abcdefghij"));      // abcdefghij

        // Edge cases
        System.out.println(new Solution().bracketedReversal(""));                // ''
        System.out.println(new Solution().bracketedReversal("[a]"));             // a
        System.out.println(new Solution().bracketedReversal("[ab]"));            // ba
        System.out.println(new Solution().bracketedReversal("[[ab]]"));          // ab
        System.out.println(new Solution().bracketedReversal("x[y[z]]"));         // xzy
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "a[bcd]e"`. The stack holds characters and `[`; on `]`, pop-while-appending builds the reversed substring:

```
s = "a[bcd]e"

'a'  letter → push          → stack (bottom→top): a
'['  marker → push          → stack: a [
'b'  letter → push          → stack: a [ b
'c'  letter → push          → stack: a [ b c
'd'  letter → push          → stack: a [ b c d
']'  trigger → pop d,c,b appending → "dcb"; discard '[' → stack: a
                push "dcb"  → stack: a dcb
'e'  letter → push          → stack: a dcb e

end of input → concatenate → "a" + "dcb" + "e" = "adcbe" ✓
```

A trace on `s = "x[y[z]]"` shows nesting — the inner `]` folds before the outer:

```
'x' push → x ;  '[' push → x [ ;  'y' push → x [ y ;  '[' push → x [ y [ ;  'z' push → x [ y [ z
']' pop z → "z"; discard '['; push "z"  → stack: x [ y z
']' pop "z","y" → "zy"; discard '['; push "zy"  → stack: x zy

end of input → "x" + "zy" = "xzy" ✓
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | One pass over `N` characters; each character is pushed once and popped at most once during a fold. |
| Space | **O(N)** | The stack holds the unfolded prefix; a bracket-free string pushes every character before any fold. |

The time is `O(N)` where `N` is the string length: each character is pushed once and contributes to at most one fold when its enclosing `]` arrives, so total push/pop work is linear. The space is `O(N)`: the stack stores every character not yet folded, and a string with no brackets (or one giant group) holds the whole input before the final concatenation. Building each reversed substring reuses characters already on the stack, so it adds no extra order of space.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty string | `""` | `''` | Nothing to scan; the stack stays empty and joins to the empty string. |
| Single pair | `[a]` | `a` | One character reverses to itself; the brackets are stripped. |
| Two-char reversal | `[ab]` | `ba` | `a` then `b` are pushed; `]` pops `b` then `a`, building `ba`. |
| Double nesting | `[[ab]]` | `ab` | The inner pair reverses `ab` to `ba`, the outer reverses `ba` back to `ab`. |
| Nested with prefix | `x[y[z]]` | `xzy` | `z` folds to `z`, then `y z` folds to `zy`; `x` prefixes it. |
| No brackets | `abcdefghij` | `abcdefghij` | Every letter pushes and none folds, so the join is the original string. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Push characters and `[` markers; on `]`, pop the run back to the marker *while appending*, which yields the reversed substring with no separate reverse step. The new idea over path canonicalisation is that the fold *transforms* the popped chunk — pop order doubles as reversal — and the combined token is pushed back so nesting composes automatically.

</details>