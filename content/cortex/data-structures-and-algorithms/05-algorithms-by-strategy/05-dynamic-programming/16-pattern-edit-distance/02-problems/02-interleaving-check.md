---
title: "Interleaving Check"
summary: "Given three strings s1, s2, s3, return true if s3 is an interleaving of s1 and s2 — that is, s3 is formed by merging the characters of s1 and s2 while preserving each one's internal order."
prereqs:
  - 16-pattern-edit-distance/01-pattern
difficulty: hard
---

# Interleaving Check

## The Problem

Given three strings `s1`, `s2`, `s3`, return `true` if `s3` is an interleaving of `s1` and `s2` — that is, `s3` is formed by merging the characters of `s1` and `s2` while preserving each one's internal order.

```
Input:  s1 = "code", s2 = "intuition", s3 = "cointuitionde"
Output: true
        Merge as: c-o-(intuition)-d-e ?  No — let's verify properly.
        s1 contributes: c, o, ..., d, e in order. s2 contributes: i, n, t, u, i, t, i, o, n in order.
        The merged sequence "co" + "intuition" + "de" alternates s1[c, o] then s2[intuition] then s1[d, e].

Input:  s1 = "abc", s2 = "def", s3 = "adbecf"
Output: true
        a (s1) d (s2) b (s1) e (s2) c (s1) f (s2) — strict alternation.

Input:  s1 = "abc", s2 = "def", s3 = "adcebf"
Output: false
        s3 has c before b — violates s1's internal order.
```

<details>
<summary><h2>The Recurrence</h2></summary>


`dp[i][j]` = whether `s3[0..i+j-1]` is an interleaving of `s1[0..i-1]` and `s2[0..j-1]`. Note: `s3`'s position is *implicit* — `i + j - 1`.

**Base case.** `dp[0][0] = true` — empty plus empty equals empty, trivially an interleaving.

**Inductive case.** Two ways `s3[i+j-1]` could have been produced:
- It matches the last character of `s1`: `s1[i-1] == s3[i+j-1]` AND `dp[i-1][j]` is true (the rest interleaves correctly).
- It matches the last character of `s2`: `s2[j-1] == s3[i+j-1]` AND `dp[i][j-1]` is true.

OR them:
```
dp[i][j] = (s1[i-1] == s3[i+j-1] AND dp[i-1][j])
        OR (s2[j-1] == s3[i+j-1] AND dp[i][j-1])
```

> *Pause. Why is there no diagonal `dp[i-1][j-1]` term? Predict the reasoning.*

Because each character of `s3` must come from *exactly one* of `s1` or `s2` — not both, not neither. The two terms above cover both options; a diagonal term would correspond to "consume one from each", which doesn't make sense for an interleaving (which consumes one character at a time, alternating arbitrarily between the two source strings).

**Fast fail.** If `len(s3) != len(s1) + len(s2)`, return `false` immediately. Lengths must add up by the pigeonhole-style accounting.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=dp
from typing import List

class Solution:
    def interleaving_check(self, s1: str, s2: str, s3: str) -> bool:
        n: int = len(s1)
        m: int = len(s2)

        # base case: length of given strings doesn't match
        if n + m != len(s3):
            return False

        # Create a 2D matrix to store the intermediate results
        # dp[i][j] will represent whether s3[0...i+j-1] can be formed by
        # interleaving s1[0...i-1] and s2[0...j-1]
        dp: List[List[bool]] = [[False] * (m + 1) for _ in range(n + 1)]

        # Base case: an empty s1 and s2 can form an empty s3
        dp[0][0] = True

        # Fill the matrix in a bottom-up manner
        for i in range(n + 1):
            for j in range(m + 1):

                # Check if s1's current character matches with s3's
                # current character and the previous characters of s1 and
                # s2 have already formed s3
                if i > 0 and s1[i - 1] == s3[i + j - 1]:
                    dp[i][j] = dp[i][j] or dp[i - 1][j]

                # Check if s2's current character matches with s3's
                # current character and the previous characters of s1 and
                # s2 have already formed s3
                if j > 0 and s2[j - 1] == s3[i + j - 1]:
                    dp[i][j] = dp[i][j] or dp[i][j - 1]

        # The bottom-right element of the matrix represents whether
        # s3 can be formed by interleaving s1 and s2
        return dp[n][m]


# Examples from the problem statement
print(Solution().interleaving_check("code", "intuition", "cointuitionde"))  # True
print(Solution().interleaving_check("abc", "def", "adbecf"))                # True
print(Solution().interleaving_check("abc", "def", "adcebf"))                # False

# Edge cases
print(Solution().interleaving_check("", "", ""))                            # True  — all empty
print(Solution().interleaving_check("a", "", "a"))                          # True  — s2 empty
print(Solution().interleaving_check("", "b", "b"))                          # True  — s1 empty
print(Solution().interleaving_check("ab", "cd", "abcd"))                    # True  — s1 then s2
print(Solution().interleaving_check("ab", "cd", "acbd"))                    # True  — interleaved
print(Solution().interleaving_check("ab", "cd", "abdc"))                    # False — wrong order
```

```java run viz=grid viz-root=dp
import java.util.*;

public class Main {
    static class Solution {
        public boolean interleavingCheck(String s1, String s2, String s3) {
            int n = s1.length();
            int m = s2.length();

            // base case: length of given strings doesn't match
            if (n + m != s3.length()) {
                return false;
            }

            // Create a 2D matrix to store the intermediate results
            // dp[i][j] will represent whether s3[0...i+j-1] can be formed by
            // interleaving s1[0...i-1] and s2[0...j-1]
            boolean[][] dp = new boolean[n + 1][m + 1];

            // Base case: an empty s1 and s2 can form an empty s3
            dp[0][0] = true;

            // Fill the matrix in a bottom-up manner
            for (int i = 0; i <= n; i++) {
                for (int j = 0; j <= m; j++) {

                    // Check if s1's current character matches with s3's
                    // current character and the previous characters of s1
                    // and s2 have already formed s3
                    if (
                        i > 0 && s1.charAt(i - 1) == s3.charAt(i + j - 1)
                    ) dp[i][j] = dp[i][j] || dp[i - 1][j];

                    // Check if s2's current character matches with s3's
                    // current character and the previous characters of s1
                    // and s2 have already formed s3
                    if (
                        j > 0 && s2.charAt(j - 1) == s3.charAt(i + j - 1)
                    ) dp[i][j] = dp[i][j] || dp[i][j - 1];
                }
            }

            // The bottom-right element of the matrix represents whether
            // s3 can be formed by interleaving s1 and s2
            return dp[n][m];
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().interleavingCheck("code", "intuition", "cointuitionde"));  // true
        System.out.println(new Solution().interleavingCheck("abc", "def", "adbecf"));                // true
        System.out.println(new Solution().interleavingCheck("abc", "def", "adcebf"));                // false

        // Edge cases
        System.out.println(new Solution().interleavingCheck("", "", ""));                            // true
        System.out.println(new Solution().interleavingCheck("a", "", "a"));                          // true
        System.out.println(new Solution().interleavingCheck("", "b", "b"));                          // true
        System.out.println(new Solution().interleavingCheck("ab", "cd", "abcd"));                    // true
        System.out.println(new Solution().interleavingCheck("ab", "cd", "acbd"));                    // true
        System.out.println(new Solution().interleavingCheck("ab", "cd", "abdc"));                    // false
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
| Both empty, target empty | `("", "", "")` | `true` | `dp[0][0] = true`. |
| One source empty, target equals other | `("", "abc", "abc")` | `true` | Pure pass-through. |
| Length mismatch | `("a", "b", "abc")` | `false` | Quick reject. |
| Same character on both sides | `("aa", "ab", "aaba")` | `true` | Multiple valid interleavings; OR collects them. |

</details>
