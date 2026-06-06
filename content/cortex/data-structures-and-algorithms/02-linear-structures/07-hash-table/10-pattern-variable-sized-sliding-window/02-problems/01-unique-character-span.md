---
title: "Unique Character Span"
summary: "Given a string s, return the length of the longest substring with distinct characters."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: easy
---

# Unique character span

## Problem Statement

Given a string `s`, return the length of the longest substring with **distinct** characters.

### Example 1
> -   **Input:** `s = "abcbed"` → **Output:** `4` (`"cbed"`)

### Example 2
> -   **Input:** `s = "aaaaabc"` → **Output:** `3` (`"abc"`)

### Example 3
> -   **Input:** `s = "abcdefgh"` → **Output:** `8` (the whole string)

## Examples

**Example 1**
```
Input:  s = "abcbed"
Output: 4
Explanation: "cbed" is the longest run with all-distinct characters. The earlier
"abc" is only length 3 because the second 'b' breaks the streak.
```

**Example 2**
```
Input:  s = "aaaaabc"
Output: 3
Explanation: the long 'a' prefix collapses to a single-character window each step.
The window only grows once distinct characters arrive → "abc", length 3.
```

**Example 3**
```
Input:  s = "abcdefgh"
Output: 8
Explanation: every character is distinct, so the window never contracts and spans
the whole string → length 8.
```

**Example 4**
```
Input:  s = "aab"
Output: 2
Explanation: the second 'a' forces a contraction past index 0, then 'b' extends
the window to "ab" → length 2.
```

<details>
<summary><h2>Solution</h2></summary>


Already implemented above as the canonical example. The core invariant: when the loop body finishes, the window contains only distinct characters.


```python run viz=array viz-root=s
class Solution:
    def unique_character_span(self, s: str) -> int:

        # Dictionary to store character frequencies
        frequency = {}

        # To store the maximum length of the substring
        max_length = 0

        # Sliding window pointers
        start, end = 0, 0

        while end < len(s):

            # Add the end character to the map
            end_char = s[end]
            frequency[end_char] = frequency.get(end_char, 0) + 1

            # If a character appears more than once, shrink the window
            while frequency.get(end_char, 0) > 1:
                start_char = s[start]
                frequency[start_char] -= 1

                # Remove character if count is 0
                if frequency[start_char] == 0:
                    del frequency[start_char]

                # Move the start pointer to shrink the window
                start += 1

            # Update the maximum length of the valid substring
            max_length = max(max_length, end - start + 1)

            # Expand the window
            end += 1

        return max_length


# Examples from the problem statement
print(Solution().unique_character_span("abcbed"))    # 4
print(Solution().unique_character_span("aaaaabc"))   # 3
print(Solution().unique_character_span("abcdefgh"))  # 8

# Edge cases
print(Solution().unique_character_span(""))          # 0
print(Solution().unique_character_span("a"))         # 1
print(Solution().unique_character_span("aa"))        # 1
print(Solution().unique_character_span("ab"))        # 2
print(Solution().unique_character_span("aab"))       # 2
```

```java run viz=array viz-root=s
import java.util.*;

public class Main {
    static class Solution {
        public int uniqueCharacterSpan(String s) {

            // Map to store character frequencies
            Map<Character, Integer> frequency = new HashMap<>();

            // To store the maximum length of the substring
            int maxLength = 0;

            // Sliding window pointers
            int start = 0;
            int end = 0;

            while (end < s.length()) {

                // Add the end character to the map
                char endChar = s.charAt(end);
                frequency.put(
                    endChar,
                    frequency.getOrDefault(endChar, 0) + 1
                );

                // If a character appears more than once, shrink the window
                while (frequency.get(endChar) > 1) {
                    char startChar = s.charAt(start);
                    frequency.put(startChar, frequency.get(startChar) - 1);

                    // Remove character if count is 0
                    if (frequency.get(startChar) == 0) {
                        frequency.remove(startChar);
                    }

                    // Move the start pointer to shrink the window
                    start++;
                }

                // Update the maximum length of the valid substring
                maxLength = Math.max(maxLength, end - start + 1);

                // Expand the window
                end++;
            }

            return maxLength;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().uniqueCharacterSpan("abcbed"));    // 4
        System.out.println(new Solution().uniqueCharacterSpan("aaaaabc"));   // 3
        System.out.println(new Solution().uniqueCharacterSpan("abcdefgh"));  // 8

        // Edge cases
        System.out.println(new Solution().uniqueCharacterSpan(""));          // 0
        System.out.println(new Solution().uniqueCharacterSpan("a"));         // 1
        System.out.println(new Solution().uniqueCharacterSpan("aa"));        // 1
        System.out.println(new Solution().uniqueCharacterSpan("ab"));        // 2
        System.out.println(new Solution().uniqueCharacterSpan("aab"));       // 2
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


This is a **longest contiguous subsequence** problem, and the rule — "no repeated characters" — reads directly off a frequency map. That combination is the signature of the variable-sized sliding window. Keep a window `s[start..end]` and a count of every character inside it; the window is valid exactly when every count is `1`. Because the answer is a contiguous run and the rule is a simple map check, the pattern fits.

The two pointers play asymmetric roles, and the placement is what makes a single pass work. The `end` pointer marks the rightmost character admitted so far and advances every step. The `start` pointer marks the left edge and advances *only* when a duplicate appears — it slides forward just far enough to evict the earlier copy of `s[end]`. At every moment after contraction, `s[start..end]` is the longest valid window that ends at `end`, so recording `end − start + 1` there captures every candidate.

The naive approach breaks on cost, not correctness. Restarting the scan from each index rebuilds the window from scratch and pays **O(N²)** time. The window approach never rewinds `start`: once a duplicate disqualifies every window containing the earlier copy, those windows are gone for good. Each character is admitted once and evicted at most once, so the whole scan is **O(N)**.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Unique Character Span |
|---|---|
| **Q1.** Is the answer the longest/shortest/count of a contiguous subsequence? | **Yes** — the longest contiguous substring with all-distinct characters. |
| **Q2.** Can a hash map summarise the window for an `O(1)` rule check? | **Yes** — a character-frequency map; the rule is "the newest character's count is `1`". |
| **Q3.** Can you add `s[end]` and remove `s[start]` in `O(1)`? | **Yes** — one increment on expand, one decrement (and possible key delete) on contract. |
| **Q4.** Is the rule monotonic as the window grows? | **Yes** — adding a character can only introduce a duplicate; removing one can only clear it. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `start = 0`, an empty `frequency` map, and `maxLength = 0`.
2. Advance `end` across the string. For each `end`, increment `frequency[s[end]]`.
3. While `frequency[s[end]] > 1` — the newly added character is a duplicate — contract from the left: decrement `frequency[s[start]]`, delete the key if its count reaches `0`, and advance `start`.
4. The window `s[start..end]` now has no duplicates. Record `maxLength = max(maxLength, end − start + 1)`.
5. After the loop, return `maxLength`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `s = "abcbed"`, expected output `4`. The window is `s[start..end]`; the rule is "no duplicate character":

```
end=0  add 'a'  freq={a:1}              window "a"     len 1  maxLength=1
end=1  add 'b'  freq={a:1,b:1}          window "ab"    len 2  maxLength=2
end=2  add 'c'  freq={a:1,b:1,c:1}      window "abc"   len 3  maxLength=3
end=3  add 'b'  freq={a:1,b:2,c:1}      'b' count 2 → duplicate
       evict 'a'  start 0→1  freq={b:2,c:1}   'b' still 2
       evict 'b'  start 1→2  freq={b:1,c:1}   window "cb"   len 2  maxLength=3
end=4  add 'e'  freq={b:1,c:1,e:1}      window "cbe"   len 3  maxLength=3
end=5  add 'd'  freq={b:1,c:1,e:1,d:1}  window "cbed"  len 4  maxLength=4

return maxLength = 4
```

The result `4` matches the expected output — `"cbed"` is the longest substring with no repeated characters.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | `end` advances `N` times; `start` advances at most `N` times. Each character is admitted once and evicted at most once, so the inner `while` is amortised `O(1)`. |
| **Space** | **O(K)** | The map holds at most one entry per distinct character in the window — `K` is the alphabet size, `O(1)` for a fixed alphabet. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `""` | `0` | Empty string — the loop never runs, `maxLength` stays `0`. |
| `"a"` | `1` | Single character — one valid window of length `1`. |
| `"aa"` | `1` | The second `'a'` forces an immediate contraction; the window never exceeds length `1`. |
| `"ab"` | `2` | All distinct — the window spans the whole string. |
| `"aab"` | `2` | Contracts past the first `'a'` at index 1, then grows to `"ab"`. |
| `"abcdefgh"` | `8` | All distinct — no contraction ever fires; the window is the whole string. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the base case of the pattern — the rule "every character count `≤ 1`" maps to contracting while the newest character's count exceeds `1`. Every later problem swaps in a different map-readable rule on the same expand-contract-record skeleton.

</details>