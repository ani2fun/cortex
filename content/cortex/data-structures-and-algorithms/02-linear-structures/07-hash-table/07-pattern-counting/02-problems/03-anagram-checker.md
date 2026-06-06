---
title: "Anagram Checker"
summary: "Given two strings s and p, return true if p is an anagram of s (same multiset of characters), else false."
prereqs:
  - 07-pattern-counting/01-pattern
difficulty: easy
---

# Anagram checker

## Problem Statement

Given two strings `s` and `p`, return `true` if `p` is an anagram of `s` (same multiset of characters), else `false`.

### Example 1
> -   **Input:** `s = "codeintuition", p = "cdoenoitiutni"`
> -   **Output:** `true`

### Example 2
> -   **Input:** `s = "abc", p = "ade"`
> -   **Output:** `false`

### Example 3
> -   **Input:** `s = "abcdef", p = "dfecba"`
> -   **Output:** `true`

## Examples

**Example 1**
```
Input:  s = "codeintuition", p = "cdoenoitiutni"
Output: true
Explanation: both strings hold the same letters with the same counts → anagrams.
```

**Example 2**
```
Input:  s = "abc", p = "ade"
Output: false
Explanation: p has 'd' and 'e' that s lacks → not anagrams.
```

**Example 3**
```
Input:  s = "abcdef", p = "dfecba"
Output: true
Explanation: p is a reordering of s with identical counts → anagrams.
```

**Example 4**
```
Input:  s = "ab", p = "a"
Output: false
Explanation: different lengths cannot be anagrams.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **counting** problem is that an anagram is defined by *multiset equality* — `p` is an anagram of `s` exactly when both hold the same characters with the same counts. Order does not matter, only frequency, which is the signal the counting pattern fires on.

The frequency map *is* the multiset. Count `s` into a map, then walk `p` and decrement each character's count. A character missing from the map, or a count that disagrees, means the multisets differ. If the map drains to empty, every character matched one-for-one and the two are anagrams. A fast length check up front rejects the obvious non-anagrams before any counting.

The naive approach breaks the time budget. Sorting both strings and comparing is `O(N log N)` time, and a per-character search is worse at `O(N²)`. Counting compares the multisets in two linear passes — `O(N)` time — without ever ordering the characters.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Anagram Checker |
|---|---|
| **Q1.** Does the answer depend on how *often* items appear? | **Yes** — anagrams share an identical character-frequency map. |
| **Q2.** Is the input a linear sequence? | **Yes** — two strings, each walked character by character. |
| **Q3.** Can the answer be read off the counts after one pass? | **Yes** — count `s`, then decrement over `p` and check the map is empty. |
| **Q4.** Is the per-item work `O(1)` amortised? | **Yes** — one hash-map update per character in each string. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Anagrams have the same length and the same character frequency map. Build the frequency of `s`, then walk `p` and decrement; if any character is missing or counts disagree, return `false`. The map ends empty iff the two are anagrams.

> *Mental shortcut* — anagram checking is "does the multiset match?". The frequency map *is* the multiset.

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Reject on length first, then compare multisets by counting and draining.

1. **Length gate.** If `s` and `p` differ in length, they cannot be anagrams — return `false`.
2. **Count `s`.** Build a `frequency` map of every character in `s`.
3. **Drain with `p`.** For each character of `p`, return `false` if it is absent from the map; otherwise decrement its count.
4. **Prune zeros.** When a count hits `0`, remove the key so the map tracks only outstanding characters.
5. **Check empty.** After walking `p`, an empty map means every character matched — return whether the map is empty.

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

    def anagram_checker(self, s: str, t: str) -> bool:

        # If the strings are of different lengths, they cannot be
        # anagrams
        if len(s) != len(t):
            return False

        # Create a map to store the frequency of each character in the
        # first string
        s_frequency = self.count_frequency(s)

        # Traverse the second string and decrement the frequency of each
        # character in the hash map
        for ch in t:
            if ch not in s_frequency:
                return False

            s_frequency[ch] -= 1
            if s_frequency[ch] == 0:
                del s_frequency[ch]

        return len(s_frequency) == 0


# Examples from the problem statement
print(Solution().anagram_checker("codeintuition", "cdoenoitiutni"))  # True
print(Solution().anagram_checker("abc", "ade"))                      # False
print(Solution().anagram_checker("abcdef", "dfecba"))                # True

# Edge cases
print(Solution().anagram_checker("", ""))                            # True
print(Solution().anagram_checker("a", "a"))                          # True
print(Solution().anagram_checker("a", "b"))                          # False
print(Solution().anagram_checker("ab", "a"))                         # False
print(Solution().anagram_checker("aab", "baa"))                      # True
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

        public boolean anagramChecker(String s, String t) {

            // If the strings are of different lengths, they cannot be
            // anagrams
            if (s.length() != t.length()) {
                return false;
            }

            // Create a map to store the frequency of each character in the
            // first string
            Map<Character, Integer> sFrequency = countFrequency(s);

            // Traverse the second string and decrement the frequency of each
            // character in the hash map
            for (char ch : t.toCharArray()) {
                if (!sFrequency.containsKey(ch)) {
                    return false;
                }

                sFrequency.put(ch, sFrequency.get(ch) - 1);
                if (sFrequency.get(ch) == 0) {
                    sFrequency.remove(ch);
                }
            }

            return sFrequency.isEmpty();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().anagramChecker("codeintuition", "cdoenoitiutni")); // true
        System.out.println(new Solution().anagramChecker("abc", "ade"));                     // false
        System.out.println(new Solution().anagramChecker("abcdef", "dfecba"));               // true

        // Edge cases
        System.out.println(new Solution().anagramChecker("", ""));                           // true
        System.out.println(new Solution().anagramChecker("a", "a"));                         // true
        System.out.println(new Solution().anagramChecker("a", "b"));                         // false
        System.out.println(new Solution().anagramChecker("ab", "a"));                        // false
        System.out.println(new Solution().anagramChecker("aab", "baa"));                     // true
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "codeintuition"`, `p = "cdoenoitiutni"`. Both have length `13`, so count `s` and drain with `p`:

```
length gate: |s| = |p| = 13  → continue

count s = "codeintuition"
  {c:1, o:2, d:1, e:1, i:3, n:2, t:2, u:1}

drain with p = "cdoenoitiutni"  (delete a key when its count hits 0)
  'c'  1→0 del   'd'  1→0 del   'o'  2→1        'e'  1→0 del
  'n'  2→1       'o'  1→0 del   'i'  3→2        't'  2→1
  'i'  2→1       'u'  1→0 del   't'  1→0 del    'n'  1→0 del
  'i'  1→0 del

map is empty → return true
```

The result `true` matches the expected output — `p` holds the same characters as `s` with identical counts.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | The length gate is `O(1)`; counting `s` and draining with `p` are each one linear pass. |
| Space | **O(N)** | The map holds up to `N` distinct characters — `O(1)` for a fixed alphabet, `O(N)` in general. |

Here `N` is the common string length; mismatched lengths short-circuit before any counting.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Both empty | `s = "", p = ""` | `true` | Two empty multisets are equal. |
| Single match | `s = "a", p = "a"` | `true` | Identical one-character multisets. |
| Single mismatch | `s = "a", p = "b"` | `false` | `'b'` is absent from `s`'s map. |
| Different lengths | `s = "ab", p = "a"` | `false` | The length gate rejects before counting. |
| Reordered | `s = "aab", p = "baa"` | `true` | Same counts (`a:2, b:1`), different order. |
| Extra letter | `s = "abc", p = "ade"` | `false` | `p` introduces `'d'`/`'e'` not in `s`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the multiset-equality shape of counting: gate on length, count one string, then drain with the other and check the map empties. The delete-on-zero step keeps the map holding only the characters still unmatched, so emptiness is the verdict.

</details>