---
title: "First Non-Repeating Character"
summary: "Given a string s, find and return the index of the first non-repeating character. Return -1 if no such character exists."
prereqs:
  - 07-pattern-counting/01-pattern
difficulty: easy
---

# First non repeating character

## Problem Statement

Given a string `s`, find and return the index of the first non-repeating character. Return `-1` if no such character exists.

### Example 1
> -   **Input:** `s = "codeintuition"`
> -   **Output:** `0`
> -   **Explanation:** `'c'` is the first non-repeating character.

### Example 2
> -   **Input:** `s = "aaabcd"`
> -   **Output:** `3`
> -   **Explanation:** `'b'` is the first non-repeating character.

### Example 3
> -   **Input:** `s = "aaabbccdd"`
> -   **Output:** `-1`
> -   **Explanation:** No character appears exactly once.

## Examples

**Example 1**
```
Input:  s = "codeintuition"
Output: 0
Explanation: 'c' appears once and is the earliest such character → index 0.
```

**Example 2**
```
Input:  s = "aaabcd"
Output: 3
Explanation: 'a' repeats, so the first three indices are skipped. 'b' at index 3
appears once → return 3.
```

**Example 3**
```
Input:  s = "aaabbccdd"
Output: -1
Explanation: every character appears exactly twice, so no index has count 1.
```

**Example 4**
```
Input:  s = "abcabc"
Output: -1
Explanation: each of a, b, c appears twice → no non-repeating character.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **counting** problem is the word *non-repeating* — the answer depends only on how often each character appears, never on order or position. "Appears exactly once" is a pure frequency test, the exact signal the counting pattern fires on.

The frequency map is the right structure because it records every character's count in one place. Build it in a single pass, and "is this character unique?" becomes a constant-time read of `frequency[ch]`. A second walk over the string returns the first index whose character maps to `1`. Walking left to right guarantees that index is the *earliest* one.

The naive approach breaks the time budget. For each character it re-scans the whole string to check for a repeat, costing `O(N²)` time for `O(1)` space. That re-derives the same per-character count `N` times over. Counting computes every count once in `O(N)`, so each uniqueness check drops to an `O(1)` lookup.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for First Non-Repeating Character |
|---|---|
| **Q1.** Does the answer depend on how *often* items appear? | **Yes** — "non-repeating" means a count of exactly `1`. |
| **Q2.** Is the input a linear sequence? | **Yes** — a string, walked character by character. |
| **Q3.** Can the answer be read off the counts after one pass? | **Yes** — build the map first, then re-scan and read each count. |
| **Q4.** Is the per-item work `O(1)` amortised? | **Yes** — one hash-map increment per character, then one lookup per index. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Build the counts first, then re-scan for the earliest unique character.

1. **Build the frequency map.** Walk `s` once; for each character, increment its entry in `frequency`.
2. **Re-scan in order.** Walk `s` again from index `0`, reading `frequency[ch]` for each character.
3. **Return the first unique index.** The first character whose count is `1` is the answer — return its index.
4. **Handle the empty case.** If the re-scan finds no count-1 character, return `-1`.

</details>
<details>
<summary><h2>Solution</h2></summary>


Two passes: build the frequency map, then re-walk to find the first frequency-1 character.


```python run viz=array
from collections import defaultdict
from typing import Dict

class Solution:
    def count_frequency(self, s: str) -> Dict[str, int]:
        frequency = defaultdict(int)

        # Traverse the string and store the frequency of each character
        # in a hash map
        for ch in s:
            frequency[ch] = frequency.get(ch, 0) + 1

        return frequency

    def first_non_repeating_character(self, s: str) -> int:

        # Create a map to store the frequency of each character in the
        # string
        frequency = self.count_frequency(s)

        # Traverse the string again and return the index of the first
        # non-repeating character
        for i, ch in enumerate(s):
            if frequency[ch] == 1:
                return i

        return -1


# Examples from the problem statement
print(Solution().first_non_repeating_character("codeintuition"))  # 0
print(Solution().first_non_repeating_character("aaabcd"))         # 3
print(Solution().first_non_repeating_character("aaabbccdd"))      # -1

# Edge cases
print(Solution().first_non_repeating_character(""))               # -1
print(Solution().first_non_repeating_character("a"))              # 0
print(Solution().first_non_repeating_character("aabb"))           # -1
print(Solution().first_non_repeating_character("abcabc"))         # -1
print(Solution().first_non_repeating_character("abcd"))           # 0
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Map<Character, Integer> countFrequency(String s) {
            Map<Character, Integer> frequency = new HashMap<>();

            // Traverse the string and store the frequency of each character
            // in a hash map
            for (char ch : s.toCharArray()) {
                frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
            }

            return frequency;
        }

        public int firstNonRepeatingCharacter(String s) {

            // Create a map to store the frequency of each character in the
            // string
            Map<Character, Integer> frequency = countFrequency(s);

            // Traverse the string again and return the index of the first
            // non-repeating character
            for (int i = 0; i < s.length(); i++) {
                if (frequency.get(s.charAt(i)) == 1) {
                    return i;
                }
            }

            return -1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().firstNonRepeatingCharacter("codeintuition")); // 0
        System.out.println(new Solution().firstNonRepeatingCharacter("aaabcd"));        // 3
        System.out.println(new Solution().firstNonRepeatingCharacter("aaabbccdd"));     // -1

        // Edge cases
        System.out.println(new Solution().firstNonRepeatingCharacter(""));              // -1
        System.out.println(new Solution().firstNonRepeatingCharacter("a"));             // 0
        System.out.println(new Solution().firstNonRepeatingCharacter("aabb"));          // -1
        System.out.println(new Solution().firstNonRepeatingCharacter("abcabc"));        // -1
        System.out.println(new Solution().firstNonRepeatingCharacter("abcd"));          // 0
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "codeintuition"`. Pass 1 builds the counts; pass 2 returns the first count-1 index:

```
pass 1 (build the map)
  c→1 o→1 d→1 e→1 i→1 n→1 t→1 u→1 i→2 t→2 i→3 o→2 n→2
  frequency = {c:1, o:2, d:1, e:1, i:3, n:2, t:2, u:1}

pass 2 (first index with count 1)
  i=0  s[0]='c'  freq 1 = 1  return 0

result = 0
```

The result `0` matches the expected output — `'c'` appears once and is the earliest such character.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | One pass to build the map, one pass to re-scan; each step is amortised `O(1)`. |
| Space | **O(N)** | The frequency map holds up to `N` distinct characters in the worst case. |

For a fixed alphabet (e.g. 26 lowercase letters), the space is bounded by the alphabet size — `O(1)` — but the general bound is `O(N)` distinct characters.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty string | `s = ""` | `-1` | No characters, so nothing can be non-repeating. |
| Single character | `s = "a"` | `0` | The only character appears once → index `0`. |
| All repeating | `s = "aabb"` | `-1` | Every character has count `2`; no index qualifies. |
| Unique at the end | `s = "aab"` | `2` | `'a'` repeats; `'b'` at index `2` is the first count-1 character. |
| All distinct | `s = "abcd"` | `0` | Every count is `1`, so the first index wins. |
| Repeats then unique | `s = "abcabc"` | `-1` | Each of `a`, `b`, `c` appears twice → no answer. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the base case of the counting pattern: tally every character once, then re-scan in order and return the first index with count `1`. The second pass over the original string — not the map — is what preserves "first" by position.

</details>