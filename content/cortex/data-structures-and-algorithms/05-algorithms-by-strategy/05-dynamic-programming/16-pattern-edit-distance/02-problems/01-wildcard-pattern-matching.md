---
title: "Wildcard Pattern Matching"
summary: "Given a string s and a pattern that may include wildcards:"
prereqs:
  - 16-pattern-edit-distance/01-pattern
difficulty: hard
---

# Wildcard Pattern Matching

## The Problem

Given a string `s` and a `pattern` that may include wildcards:
- `?` matches any single character.
- `*` matches any sequence of characters (including empty).

Return `true` if the pattern matches the entire string `s`.

```
Input:  s = "abcdef", pattern = "abc??f"
Output: true                          ?? matches "de"; rest is literal

Input:  s = "abcdef", pattern = "ab*"
Output: true                          * matches "cdef"

Input:  s = "abcdef", pattern = "ab?"
Output: false                         Pattern length 3, but ? matches 1 char — too short
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i][j]` = whether `pattern[0..j-1]` matches `s[0..i-1]`.

**Base cases.**
- `dp[0][0] = true` — empty pattern matches empty string.
- `dp[0][j] = dp[0][j-1]` if `pattern[j-1] == '*'`; else `false`. (A `*` can match the empty string, so a leading streak of `*`s still matches.)
- `dp[i][0] = false` for `i ≥ 1` — empty pattern can't match a non-empty string.

**Inductive case.** Three sub-cases on `pattern[j-1]`:
- **Literal match (`pattern[j-1] == s[i-1]`)** or **single-char wildcard (`pattern[j-1] == '?'`)** — both consume one char on each side: `dp[i][j] = dp[i-1][j-1]`.
- **Multi-char wildcard (`pattern[j-1] == '*'`)** — two options:
  - `*` matches zero characters → `dp[i][j-1]` (pattern shrinks, string unchanged).
  - `*` matches at least one character → `dp[i-1][j]` (string shrinks, pattern unchanged — `*` keeps consuming).
  - Combine: `dp[i][j] = dp[i][j-1] OR dp[i-1][j]`.
- **Literal mismatch** (`pattern[j-1]` is a regular character but doesn't equal `s[i-1]`) → `dp[i][j] = false`.

> *Pause. Why does `*` matching "one or more" recurse on `dp[i-1][j]` (not `dp[i-1][j-1]`)?*

Because the same `*` can keep matching more characters. After consuming one `s[i-1]`, the `*` is still alive — same column `j`, with `i` decremented. If we recursed on `dp[i-1][j-1]`, we'd be saying `*` matched exactly one character, losing the "match many more" semantics.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=dp
from typing import List

class Solution:
    def wildcard_pattern_matching(self, s: str, pattern: str) -> bool:
        n: int = len(s)
        m: int = len(pattern)

        # Create a 2D list to store the dynamic programming results
        dp: List[List[bool]] = [[False] * (m + 1) for _ in range(n + 1)]

        # Initialize the base case
        dp[0][0] = True

        # Fill in the first row of dp
        for j in range(1, m + 1):

            # If the current character is '*', copy the result from the
            # previous column
            if pattern[j - 1] == "*":
                dp[0][j] = dp[0][j - 1]

        # Fill in the remaining cells of dp
        for i in range(1, n + 1):
            for j in range(1, m + 1):

                # If the characters at the current positions match or if
                # the pattern has a '?', copy the result from the
                # diagonal element (top-left)
                if pattern[j - 1] == "?" or pattern[j - 1] == s[i - 1]:
                    dp[i][j] = dp[i - 1][j - 1]

                # If the current character in the pattern is '*', we have
                # two options:
                # 1. Use '*' to match 0 characters, so copy the result
                # from the cell above (dp[i - 1][j]).
                # 2. Use '*' to match 1 or more characters, so copy the
                # result from the cell to the left (dp[i][j - 1]).
                elif pattern[j - 1] == "*":
                    dp[i][j] = dp[i - 1][j] or dp[i][j - 1]

        # Return the result stored in the bottom-right cell of dp
        return dp[n][m]


# Examples from the problem statement
print(Solution().wildcard_pattern_matching("abcdef", "abc??f"))   # True
print(Solution().wildcard_pattern_matching("abcdef", "ab*"))      # True
print(Solution().wildcard_pattern_matching("abcdef", "ab?"))      # False

# Edge cases
print(Solution().wildcard_pattern_matching("", ""))               # True  — empty matches empty
print(Solution().wildcard_pattern_matching("", "*"))              # True  — star matches empty
print(Solution().wildcard_pattern_matching("abc", ""))            # False — empty pattern, non-empty string
print(Solution().wildcard_pattern_matching("abc", "abc"))         # True  — exact match
print(Solution().wildcard_pattern_matching("abc", "???"))         # True  — all question marks
print(Solution().wildcard_pattern_matching("abc", "a*c"))         # True  — star in middle
print(Solution().wildcard_pattern_matching("abc", "a*d"))         # False — star can't fix tail mismatch
```

```java run viz=grid viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public boolean wildcardPatternMatching(String s, String pattern) {
            int n = s.length();
            int m = pattern.length();

            // Create a 2D array to store the dynamic programming results
            boolean[][] dp = new boolean[n + 1][m + 1];

            // Initialize the base case
            dp[0][0] = true;

            // Fill in the first row of dp
            for (int j = 1; j <= m; j++) {

                // If the current character is '*', copy the result from the
                // previous column
                if (pattern.charAt(j - 1) == '*') {
                    dp[0][j] = dp[0][j - 1];
                }
            }

            // Fill in the remaining cells of dp
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= m; j++) {

                    // If the characters at the current positions match or if
                    // the pattern has a '?', copy the result from the
                    // diagonal element (top-left)
                    if (
                        pattern.charAt(j - 1) == '?' ||
                        pattern.charAt(j - 1) == s.charAt(i - 1)
                    ) {
                        dp[i][j] = dp[i - 1][j - 1];
                    }

                    // If the current character in the pattern is '*', we
                    // have two options:
                    // 1. Use '*' to match 0 characters, so copy the result
                    // from the cell above (dp[i - 1][j]).
                    // 2. Use '*' to match 1 or more characters, so copy the
                    // result from the cell to the left (dp[i][j - 1]).
                    else if (pattern.charAt(j - 1) == '*') {
                        dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                    }
                }
            }

            // Return the result stored in the bottom-right cell of dp
            return dp[n][m];
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().wildcardPatternMatching("abcdef", "abc??f"));   // true
        System.out.println(new Solution().wildcardPatternMatching("abcdef", "ab*"));      // true
        System.out.println(new Solution().wildcardPatternMatching("abcdef", "ab?"));      // false

        // Edge cases
        System.out.println(new Solution().wildcardPatternMatching("", ""));               // true
        System.out.println(new Solution().wildcardPatternMatching("", "*"));              // true
        System.out.println(new Solution().wildcardPatternMatching("abc", ""));            // false
        System.out.println(new Solution().wildcardPatternMatching("abc", "abc"));         // true
        System.out.println(new Solution().wildcardPatternMatching("abc", "???"));         // true
        System.out.println(new Solution().wildcardPatternMatching("abc", "a*c"));         // true
        System.out.println(new Solution().wildcardPatternMatching("abc", "a*d"));         // false
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n × m)` |
| Space | `O(n × m)` (reducible to `O(m)` with rolling rows) |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty inputs | `s="", pattern=""` | `true` | `dp[0][0] = true`. |
| Pattern is just `*` | `s="abc", pattern="*"` | `true` | `*` matches anything, including empty. |
| Pattern empty, string non-empty | `s="abc", pattern=""` | `false` | Column 0 stays false past row 0. |
| Multiple `*`s | `s="abcd", pattern="*c*"` | `true` | Two `*`s coverage. |
| Adversarial mismatch | `s="abc", pattern="abd"` | `false` | Literal mismatch at position 2. |

</details>
