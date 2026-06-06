---
title: "K Characters Span"
summary: "Given a string s and integer k, return the length of the longest substring with at most K distinct characters."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
---

# K characters span

## Problem Statement

Given a string `s` and integer `k`, return the length of the longest substring with **at most K distinct** characters.

### Example 1
> -   **Input:** `s = "abcbed", k = 2` → **Output:** `3` (`"bcb"`)

### Example 2
> -   **Input:** `s = "aaaaabc", k = 3` → **Output:** `7` (whole string)

### Example 3
> -   **Input:** `s = "abcdefgh", k = 3` → **Output:** `3` (`"abc"`, `"bcd"`, etc.)

## Examples

**Example 1**
```
Input:  s = "abcbed", k = 2
Output: 3
Explanation: "bcb" holds only 2 distinct characters (b, c) → length 3. Any longer
window here would pull in a third distinct letter.
```

**Example 2**
```
Input:  s = "aaaaabc", k = 3
Output: 7
Explanation: the whole string has exactly 3 distinct characters (a, b, c), which
is ≤ 3, so the window never has to contract → length 7.
```

**Example 3**
```
Input:  s = "abcdefgh", k = 3
Output: 3
Explanation: every character is distinct, so any window of 4 holds 4 distinct
letters. The cap of 3 keeps the best window at length 3 ("abc", "bcd", …).
```

**Example 4**
```
Input:  s = "abc", k = 0
Output: 0
Explanation: zero distinct characters allowed means no character fits. Every added
character forces immediate contraction → length 0.
```

<details>
<summary><h2>Approach</h2></summary>


Same skeleton; the **rule** is now "at most K distinct characters in the window", which is exactly `len(freq_map) ≤ k`. Expand `end` greedily; when the map has more than K keys, contract from the left until it doesn't.

> *Observation* — `len(freq_map)` is the distinct-count *only if* you delete keys whose frequency drops to zero. The boundary work is the same as in the fixed-window pattern; only the rule changed.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=array viz-root=s
class Solution:
    def k_characters_span(self, s: str, k: int) -> int:

        # Dictionary to store character frequencies
        frequency = {}

        # To store the maximum length of the substring
        max_length = 0

        # Sliding window pointers
        start, end = 0, 0

        while end < len(s):

            # Add the end character to the dictionary
            end_char = s[end]
            frequency[end_char] = frequency.get(end_char, 0) + 1

            # If the number of distinct characters exceeds k, shrink the
            # window
            while len(frequency) > k:
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
print(Solution().k_characters_span("abcbed", 2))    # 3
print(Solution().k_characters_span("aaaaabc", 3))   # 7
print(Solution().k_characters_span("abcdefgh", 3))  # 3

# Edge cases
print(Solution().k_characters_span("", 2))          # 0
print(Solution().k_characters_span("a", 1))         # 1
print(Solution().k_characters_span("aaa", 1))       # 3
print(Solution().k_characters_span("abc", 0))       # 0
print(Solution().k_characters_span("aab", 2))       # 3
```

```java run viz=array viz-root=s
import java.util.*;

public class Main {
    static class Solution {
        public int kCharactersSpan(String s, int k) {

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

                // If the number of distinct characters exceeds k, shrink the
                // window
                while (frequency.size() > k) {
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
        System.out.println(new Solution().kCharactersSpan("abcbed", 2));    // 3
        System.out.println(new Solution().kCharactersSpan("aaaaabc", 3));   // 7
        System.out.println(new Solution().kCharactersSpan("abcdefgh", 3));  // 3

        // Edge cases
        System.out.println(new Solution().kCharactersSpan("", 2));          // 0
        System.out.println(new Solution().kCharactersSpan("a", 1));         // 1
        System.out.println(new Solution().kCharactersSpan("aaa", 1));       // 3
        System.out.println(new Solution().kCharactersSpan("abc", 0));       // 0
        System.out.println(new Solution().kCharactersSpan("aab", 2));       // 3
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


This is a **longest contiguous subsequence** problem whose rule — "at most `k` distinct characters" — reads off a frequency map as `len(map) ≤ k`. The window holds the characters under consideration; the map's *size* is the distinct-count. Because the answer is a contiguous run and the rule is a single map-size check, the variable-sized sliding window fits. Only the rule has changed from the unique-character-span problem; the skeleton is identical.

The pointers keep their asymmetric roles. The `end` pointer admits one new character per step and always advances. The `start` pointer evicts from the left, but only while the window holds more than `k` distinct characters — sliding forward until the distinct-count drops back to `k`. The map must delete a key the instant its count hits zero, because `len(map)` is the distinct-count *only* if dead keys are removed. After each contraction, `s[start..end]` is the longest window ending at `end` with `≤ k` distinct characters.

The naive approach is correct but quadratic. Fixing a start and re-scanning forward, rebuilding the distinct-set each time, costs **O(N²)** time. The window approach never rewinds `start`: once a window has too many distinct characters, every window sharing that left edge and extending further is also too wide, so those are skipped. Each character is admitted once and evicted at most once — **O(N)** total.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for K Characters Span |
|---|---|
| **Q1.** Is the answer the longest/shortest/count of a contiguous subsequence? | **Yes** — the longest contiguous substring with at most `k` distinct characters. |
| **Q2.** Can a hash map summarise the window for an `O(1)` rule check? | **Yes** — a frequency map; the rule is `len(map) ≤ k`, read in `O(1)`. |
| **Q3.** Can you add `s[end]` and remove `s[start]` in `O(1)`? | **Yes** — increment on expand; decrement and delete-on-zero on contract. |
| **Q4.** Is the rule monotonic as the window grows? | **Yes** — adding a character can only raise the distinct-count; removing one can only lower it. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `start = 0`, an empty `frequency` map, and `maxLength = 0`.
2. Advance `end` across the string. For each `end`, increment `frequency[s[end]]`.
3. While `len(frequency) > k` — too many distinct characters — contract from the left: decrement `frequency[s[start]]`, delete the key if its count reaches `0`, and advance `start`.
4. The window `s[start..end]` now holds at most `k` distinct characters. Record `maxLength = max(maxLength, end − start + 1)`.
5. After the loop, return `maxLength`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1: `s = "abcbed"`, `k = 2`, expected output `3`. The rule is `len(freq) ≤ 2`:

```
end=0  add 'a'  freq={a:1}            distinct 1 ≤ 2   window "a"    len 1  maxLength=1
end=1  add 'b'  freq={a:1,b:1}        distinct 2 ≤ 2   window "ab"   len 2  maxLength=2
end=2  add 'c'  freq={a:1,b:1,c:1}    distinct 3 > 2
       evict 'a'  start 0→1  freq={b:1,c:1}  distinct 2  window "bc"  len 2  maxLength=2
end=3  add 'b'  freq={b:2,c:1}        distinct 2 ≤ 2   window "bcb"  len 3  maxLength=3
end=4  add 'e'  freq={b:2,c:1,e:1}    distinct 3 > 2
       evict 'b'  start 1→2  freq={b:1,c:1,e:1}  distinct 3  still > 2
       evict 'c'  start 2→3  freq={b:1,e:1}      distinct 2  window "be"  len 2  maxLength=3
end=5  add 'd'  freq={b:1,e:1,d:1}    distinct 3 > 2
       evict 'b'  start 3→4  freq={e:1,d:1}      distinct 2  window "ed"  len 2  maxLength=3

return maxLength = 3
```

The result `3` matches the expected output — `"bcb"` is the longest substring with at most `2` distinct characters.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | `end` advances `N` times; `start` advances at most `N` times. Each character enters and leaves the map once, so the inner `while` is amortised `O(1)`. |
| **Space** | **O(K)** | The map holds at most `k + 1` entries during a contraction — bounded by the alphabet size, `O(1)` for a fixed alphabet. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Input | Output | Why |
|---|---|---|
| `s = "", k = 2` | `0` | Empty string — the loop never runs. |
| `s = "a", k = 1` | `1` | One character, one distinct — the window spans it. |
| `s = "aaa", k = 1` | `3` | A single distinct character — never exceeds `k`, window covers all. |
| `s = "abc", k = 0` | `0` | Zero distinct allowed — every character forces immediate contraction. |
| `s = "aab", k = 2` | `3` | Exactly `2` distinct (`a`, `b`) — the whole string is valid. |
| `s = "abaccc", k = 2` | `4` | `"accc"` holds `2` distinct (`a`, `c`) → length `4`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The only change from unique-character span is the rule — contract while `len(map) > k` instead of while a single count exceeds `1`. Tracking distinct-count means deleting map keys the moment they hit zero, or `len(map)` overcounts.

</details>