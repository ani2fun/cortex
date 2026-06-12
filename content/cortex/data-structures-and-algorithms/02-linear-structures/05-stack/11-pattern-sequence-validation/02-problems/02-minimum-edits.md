---
title: "Minimum Edits"
summary: "Given a string s of ( and ) only, return the minimum number of insertions or deletions needed to make the sequence valid."
prereqs:
  - 11-pattern-sequence-validation/01-pattern
difficulty: medium
kind: problem
topics: [sequence-validation, stack]
---

# Minimum edits

## Problem Statement

Given a string `s` of `(` and `)` only, return the minimum number of insertions or deletions needed to make the sequence valid.

## Examples

**Example 1**
```
Input:  s = "())"
Output: 1
Explanation: '(' matches the first ')'. The second ')' has no opener
left → one unmatched closer. One edit fixes it.
```

**Example 2**
```
Input:  s = "))"
Output: 2
Explanation: both ')' arrive with an empty stack — two unmatched
closers, two edits.
```

**Example 3**
```
Input:  s = "(((())))"
Output: 0
Explanation: every '(' finds a later ')'. The stack drains to empty
and no closer is ever orphaned → zero edits.
```

**Example 4**
```
Input:  s = ")()("
Output: 2
Explanation: the leading ')' is unmatched (+1 edit), then "()" cancels,
then the trailing '(' is left on the stack (+1). Total 2.
```

```quiz
{
  "prompt": "How many edits does ')()(' need?",
  "input": "s = \")()( \"",
  "options": ["0", "1", "2", "4"],
  "answer": "2"
}
```

## Constraints

- `1 ≤ s.length ≤ 10⁴`
- `s` consists only of `(` and `)`

```python run
class Solution:
    def minimum_edits(self, s: str) -> int:
        # Your code goes here — push '(' onto a stack; for each ')' with
        # a '(' on top pop it (matched); otherwise count one edit.
        # At the end, return len(stack) + edits.
        return 0

s = input()
print(Solution().minimum_edits(s))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int minimumEdits(String s) {
            // Your code goes here — push '(' onto a stack; for each ')' with
            // a '(' on top pop it (matched); otherwise count one edit.
            // At the end, return stack.size() + edits.
            return 0;
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().minimumEdits(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "())" }
  ],
  "cases": [
    { "args": { "s": "())" }, "expected": "1" },
    { "args": { "s": "))" }, "expected": "2" },
    { "args": { "s": "(((())))" }, "expected": "0" },
    { "args": { "s": ")()(" }, "expected": "2" },
    { "args": { "s": "()" }, "expected": "0" },
    { "args": { "s": "((" }, "expected": "2" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


This is a **sequence-validation** problem because `(` and `)` pair up, and an edit is needed for every bracket that cannot find a partner. The pairing must respect order: a `)` matches the most recent unmatched `(`. The minimum number of fixes is exactly the number of brackets left unpaired after a single matching pass.

The stack holds the **unmatched `(` seen so far**. When a `)` arrives with a `(` on top, the pair cancels and you pop — that closer is free. When a `)` arrives with nothing to match, it is an orphan; you count one edit, because either inserting a `(` before it or deleting it costs one. After the pass, whatever `(` remain on the stack are also orphans, each needing one edit. The total is leftover openers plus orphaned closers.

A counting shortcut that tracks only a single balance fails on order-sensitive inputs. Consider `")("`: the balance dips to `-1` then returns to `0`, suggesting validity, yet both brackets are unpaired and the answer is `2`. The stack — or its arithmetic equivalent of an opener count plus a separate orphan-closer count — is what correctly separates the two unmatched sides.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Minimum Edits |
|---|---|
| **Q1.** Does the input pair up — openers matched by later closers? | **Yes** — each `(` seeks a later `)`; unpaired brackets on either side need an edit. |
| **Q2.** Must a closer match the *most recent* unmatched opener? | **Yes** — a `)` cancels the freshest `(` on the stack; order decides what stays unmatched. |
| **Q3.** Is one pass with `O(1)` work per token enough? | **Yes** — each character drives a single push, pop, or counter bump. |
| **Q4.** Is the answer decided by the stack's contents and an orphan count? | **Yes** — leftover `(` on the stack plus closers counted on the fly give the total edits. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Match what you can in one pass; count what is left over.

1. **Initialise an empty stack** of `(` and an `edits` counter at `0`.
2. **Walk the string left to right**, classifying each character as `(` or `)`.
3. **`(` → push.** Record it as a pending opener awaiting a closer.
4. **`)` with a matching top → pop.** A `(` is available, so the pair cancels and the closer is matched for free.
5. **`)` with an empty stack → count an edit.** No opener is available, so this closer is an orphan; add `1` to `edits`.
6. **After the pass, return `len(stack) + edits`.** The leftover `(` each need one edit, plus the orphaned closers already counted.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

```python solution time=O(n) space=O(n)
from typing import List

class Solution:
    def minimum_edits(self, s: str) -> int:

        # Stack to track unmatched '('
        stack: List[str] = []

        # Count of edits needed
        edits: int = 0

        for c in s:

            # If '(', push to stack to find a match later
            if c == "(":
                stack.append(c)

            # Else if ')', try to match with a '('
            else:

                # Found a ')', check for matching '('
                if stack and stack[-1] == "(":

                    # Found a match, pop the '(' from stack
                    stack.pop()

                # No matching '(', need an edit
                else:

                    # Need to insert a '(' before this ')' or delete
                    # this ')' which counts as one edit
                    edits += 1

        # Any unmatched '(' in stack need to be closed with ')' edits
        # plus the edits we made for unmatched ')'
        return len(stack) + edits

s = input()
print(Solution().minimum_edits(s))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int minimumEdits(String s) {

            // Stack to track unmatched '('
            Stack<Character> stack = new Stack<>();

            // Count of edits needed
            int edits = 0;

            for (char c : s.toCharArray()) {

                // If '(', push to stack to find a match later
                if (c == '(') {
                    stack.push(c);
                }

                // Else if ')', try to match with a '('
                else {

                    // Found a ')', check for matching '('
                    if (!stack.isEmpty() && stack.peek() == '(') {

                        // Found a match, pop the '(' from stack
                        stack.pop();
                    }

                    // No matching '(', need an edit
                    else {

                        // Need to insert a '(' before this ')' or delete
                        // this ')' which counts as one edit
                        edits++;
                    }
                }
            }

            // Any unmatched '(' in stack need to be closed with ')' edits
            // plus the edits we made for unmatched ')'
            return stack.size() + edits;
        }
    }

    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        System.out.println(new Solution().minimumEdits(s));
    }
}
```

### Dry Run — `s = "())"` 

```
s = "())"          stack=[]  edits=0

'('  push                 → stack: (      edits=0
')'  top='(' matches, pop → stack: (empty) edits=0
')'  stack empty, orphan  → stack: (empty) edits=1

end of input: len(stack)=0, edits=1 → return 0 + 1 = 1 ✓
```

A mixed case `s = ")()("` shows both kinds of orphan contributing:

```
s = ")()("         stack=[]  edits=0

')'  stack empty, orphan  → stack: (empty) edits=1
'('  push                 → stack: (      edits=1
')'  top='(' matches, pop → stack: (empty) edits=1
'('  push                 → stack: (      edits=1

end of input: len(stack)=1, edits=1 → return 1 + 1 = 2 ✓
```

### Complexity Analysis

| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | One pass over `N` characters; each does `O(1)` push, pop, or counter bump. |
| Space | **O(N)** worst, **O(1)** best | All-opener input (`"((("`) pushes every character; all-closer input pushes nothing. |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single opener | `s = "("` | `1` | One `(` left on the stack at the end → one edit. |
| Single closer | `s = ")"` | `1` | One orphaned `)` counted on the fly → one edit. |
| Already valid | `s = "()"` | `0` | The pair cancels; stack empty and no orphans. |
| All openers | `s = "(("` | `2` | Two `(` remain on the stack, each needing one edit. |
| Closer-then-opener | `s = ")()("` | `2` | One orphaned `)` plus one leftover `(` — the `()` between them cancels. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The minimum edits equal the count of unpaired brackets: orphaned closers counted during the pass, plus leftover openers on the stack at the end. The new idea over the bracket checker is returning a *count* of unmatched items rather than a yes/no — failure no longer stops the scan, it accumulates.

</details>
