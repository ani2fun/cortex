---
title: "Constructibility Check"
summary: "Given two strings s1 and s2, return true if s1 can be constructed using the letters from s2 (each letter usable at most once). Return false otherwise."
prereqs:
  - 07-pattern-counting/01-pattern
difficulty: easy
---

# Constructibility check

## Problem Statement

Given two strings `s1` and `s2`, return `true` if `s1` can be constructed using the letters from `s2` (each letter usable at most once). Return `false` otherwise.

### Example 1
> -   **Input:** `s1 = "somenote", s2 = "enetomoselse"`
> -   **Output:** `true`

### Example 2
> -   **Input:** `s1 = "thief", s2 = "hifacqet"`
> -   **Output:** `true`

### Example 3
> -   **Input:** `s1 = "alpha", s2 = "beta"`
> -   **Output:** `false`

## Examples

**Example 1**
```
Input:  s1 = "somenote", s2 = "enetomoselse"
Output: true
Explanation: s2 has every letter s1 needs, with enough copies — s1 is buildable.
```

**Example 2**
```
Input:  s1 = "thief", s2 = "hifacqet"
Output: true
Explanation: t, h, i, e, f all appear in s2 at least once → buildable.
```

**Example 3**
```
Input:  s1 = "alpha", s2 = "beta"
Output: false
Explanation: s1 needs 'l' and 'p', which s2 lacks → not buildable.
```

**Example 4**
```
Input:  s1 = "aa", s2 = "a"
Output: false
Explanation: s1 needs two 'a's but s2 supplies only one → not buildable.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **counting** problem is that order is irrelevant — `s1` is buildable from `s2` only if `s2` supplies *enough copies* of each needed letter. That is a per-letter count comparison, the signal the counting pattern fires on.

The frequency map of `s2` models the available-letters pool. Build it once, then walk `s1` and *consume* one copy per character by decrementing its count. A count that is missing or already zero when `s1` still demands the letter means the pool is short — the build is impossible. The map is the natural structure because it answers "how many of this letter remain?" in `O(1)`.

The naive approach breaks the time budget. For each character of `s1` it scans `s2` for an unused match and marks it, costing `O(|s1| × |s2|)` time. That re-derives the same availability counts repeatedly. Counting builds the pool once in `O(|s2|)`, so each consume step is an `O(1)` decrement-and-check.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Constructibility Check |
|---|---|
| **Q1.** Does the answer depend on how *often* items appear? | **Yes** — `s2` must supply at least as many copies of each letter as `s1` needs. |
| **Q2.** Is the input a linear sequence? | **Yes** — two strings, each walked character by character. |
| **Q3.** Can the answer be read off the counts after one pass? | **Yes** — count `s2`, then decrement while walking `s1`. |
| **Q4.** Is the per-item work `O(1)` amortised? | **Yes** — one hash-map update or lookup per character. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Build the frequency map of `s2`. Then walk `s1`; for each character, *consume* one from the map by decrementing it. If any character's count drops to zero (or below) while we still need it, `s2` doesn't have enough letters — return `false`.

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
    S2["s2 = 'enetomoselse'"] -->|"count"| F["{e:4, n:1, t:1, o:2, m:1, s:2, l:1}"]
    S1["s1 = 'somenote'"] -->|"consume 1 per char"| F
    F --> R["all available → true"]
    style R fill:#dcfce7,stroke:#22c55e
```

<p align="center"><strong>Constructibility — the s2 frequency map is the "available letters" pool. Walking s1 consumes from the pool. If you ever try to consume a letter that's exhausted, the build fails.</strong></p>

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Count the supply, then spend it as you walk the demand.

1. **Build the supply pool.** Count every character in `s2` into a `frequency` map — this is the multiset of available letters.
2. **Walk `s1` and consume.** For each character of `s1`, check its remaining count in the pool.
3. **Fail on shortage.** If the count is zero or the letter is absent, `s2` cannot supply it — return `false`.
4. **Spend one copy.** Otherwise decrement the letter's count and continue.
5. **Succeed if `s1` finishes.** Reaching the end of `s1` means every demand was met — return `true`.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array
from collections import defaultdict
from typing import Dict

class Solution:
    def count_frequency(self, s: str) -> Dict[str, int]:
        frequency = defaultdict(int)
        for ch in s:
            frequency[ch] += 1

        return frequency

    def constructibility_check(self, s1: str, s2: str) -> bool:

        # Create a map to store the frequency of each character in s2
        s2_frequency = self.count_frequency(s2)

        # Iterate over the characters in s1
        for ch in s1:

            # If the frequency of the character is zero, return False
            if s2_frequency.get(ch, 0) == 0:
                return False

            # Decrement the frequency of the character in the map
            s2_frequency[ch] -= 1

        # If all characters in s1 can be constructed from s2, return True
        return True


# Examples from the problem statement
print(Solution().constructibility_check("somenote", "enetomoselse"))  # True
print(Solution().constructibility_check("thief", "hifacqet"))         # True
print(Solution().constructibility_check("alpha", "beta"))             # False

# Edge cases
print(Solution().constructibility_check("", "abc"))                   # True
print(Solution().constructibility_check("a", ""))                     # False
print(Solution().constructibility_check("aa", "a"))                   # False
print(Solution().constructibility_check("a", "a"))                    # True
print(Solution().constructibility_check("abc", "abc"))                # True
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Map<Character, Integer> countFrequency(String s) {
            Map<Character, Integer> frequency = new HashMap<>();
            for (char ch : s.toCharArray()) {
                frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
            }

            return frequency;
        }

        public boolean constructibilityCheck(String s1, String s2) {

            // Create a map to store the frequency of each character in s2
            Map<Character, Integer> s2Frequency = countFrequency(s2);

            // Iterate over the characters in s1
            for (char ch : s1.toCharArray()) {

                // If the frequency of the character is zero, return false
                if (s2Frequency.getOrDefault(ch, 0) == 0) {
                    return false;
                }

                // Decrement the frequency of the character in the map
                s2Frequency.put(ch, s2Frequency.get(ch) - 1);
            }

            // If all characters in s1 can be constructed from s2, return
            // true
            return true;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().constructibilityCheck("somenote", "enetomoselse")); // true
        System.out.println(new Solution().constructibilityCheck("thief", "hifacqet"));        // true
        System.out.println(new Solution().constructibilityCheck("alpha", "beta"));            // false

        // Edge cases
        System.out.println(new Solution().constructibilityCheck("", "abc"));                  // true
        System.out.println(new Solution().constructibilityCheck("a", ""));                    // false
        System.out.println(new Solution().constructibilityCheck("aa", "a"));                  // false
        System.out.println(new Solution().constructibilityCheck("a", "a"));                   // true
        System.out.println(new Solution().constructibilityCheck("abc", "abc"));               // true
    }
}
```


**Complexity:** O(|s1| + |s2|) time, O(unique chars in s2) space.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s1 = "somenote"`, `s2 = "enetomoselse"`. Build the `s2` pool, then consume one copy per `s1` character:

```
build pool from s2 = "enetomoselse"
  {e:4, n:1, t:1, o:2, m:1, s:2, l:1}

consume s1 = "somenote"
  's'  count 2 > 0  →  {e:4, n:1, t:1, o:2, m:1, s:1, l:1}
  'o'  count 2 > 0  →  {e:4, n:1, t:1, o:1, m:1, s:1, l:1}
  'm'  count 1 > 0  →  {e:4, n:1, t:1, o:1, m:0, s:1, l:1}
  'e'  count 4 > 0  →  {e:3, n:1, t:1, o:1, m:0, s:1, l:1}
  'n'  count 1 > 0  →  {e:3, n:0, t:1, o:1, m:0, s:1, l:1}
  'o'  count 1 > 0  →  {e:3, n:0, t:1, o:0, m:0, s:1, l:1}
  't'  count 1 > 0  →  {e:3, n:0, t:0, o:0, m:0, s:1, l:1}
  'e'  count 3 > 0  →  {e:2, n:0, t:0, o:0, m:0, s:1, l:1}

every character consumed without shortage → return true
```

The result `true` matches the expected output — `s2` supplies every letter `s1` needs.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(\|s1\| + \|s2\|)** | One pass to count `s2`, one pass to consume over `s1`; each step is amortised `O(1)`. |
| Space | **O(k)** | The pool holds `k` distinct characters of `s2` — `O(1)` for a fixed alphabet, `O(\|s2\|)` in general. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty `s1` | `s1 = "", s2 = "abc"` | `true` | Nothing to build, so any pool suffices. |
| Empty `s2` | `s1 = "a", s2 = ""` | `false` | An empty pool can supply nothing. |
| Not enough copies | `s1 = "aa", s2 = "a"` | `false` | `s1` needs two `'a'`s; the pool has one. |
| Exact match | `s1 = "abc", s2 = "abc"` | `true` | Every letter present with exactly enough copies. |
| Surplus pool | `s1 = "a", s2 = "aaa"` | `true` | Extra copies are fine — only shortage fails. |
| Missing letter | `s1 = "alpha", s2 = "beta"` | `false` | `'l'` and `'p'` never appear in the pool. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the two-sequence reconcile shape: count the supply (`s2`) once, then decrement it while walking the demand (`s1`), failing the instant a needed letter is exhausted. The directionality matters — the pool is `s2`, and `s1` only spends from it.

</details>