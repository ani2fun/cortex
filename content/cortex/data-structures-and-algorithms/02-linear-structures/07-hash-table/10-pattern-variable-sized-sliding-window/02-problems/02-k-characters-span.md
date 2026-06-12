---
title: "K Characters Span"
summary: "Given a string s and integer k, return the length of the longest substring with at most K distinct characters."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [variable-sized-sliding-window, hash-table]
---

# K characters span

## Problem Statement

Given a string `s` and integer `k`, return the length of the longest substring with **at most K distinct** characters.

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

```quiz
{
  "prompt": "What does k_characters_span(\"aab\", 2) return?",
  "input": "s = \"aab\", k = 2",
  "options": ["1", "2", "3", "0"],
  "answer": "3"
}
```

## Constraints

- `1 ≤ s.length ≤ 10⁵`
- `s` consists of lowercase English letters
- `0 ≤ k ≤ 26`

```python run
s = input()
k = int(input())

class Solution:
    def k_characters_span(self, s: str, k: int) -> int:
        # Your code goes here
        return 0

print(Solution().k_characters_span(s, k))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int kCharactersSpan(String s, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kCharactersSpan(s, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "abcbed" },
    { "id": "k", "label": "k", "type": "number", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "s": "abcbed", "k": "2" }, "expected": "3" },
    { "args": { "s": "aaaaabc", "k": "3" }, "expected": "7" },
    { "args": { "s": "abcdefgh", "k": "3" }, "expected": "3" },
    { "args": { "s": "abc", "k": "0" }, "expected": "0" },
    { "args": { "s": "a", "k": "1" }, "expected": "1" },
    { "args": { "s": "aaa", "k": "1" }, "expected": "3" },
    { "args": { "s": "aab", "k": "2" }, "expected": "3" },
    { "args": { "s": "abaccc", "k": "2" }, "expected": "4" }
  ]
}
```

<details>
<summary>Editorial</summary>

Same skeleton as unique-character-span; the only change is the rule: contract while `len(frequency) > k` instead of while a single count exceeds `1`. Delete map keys on count-zero so `len(map)` equals the exact distinct-count. `O(n)` time, `O(k)` space.

```python solution time=O(n) space=O(k)
class Solution:
    def k_characters_span(self, s: str, k: int) -> int:
        frequency = {}
        max_length = 0
        start, end = 0, 0
        while end < len(s):
            end_char = s[end]
            frequency[end_char] = frequency.get(end_char, 0) + 1
            while len(frequency) > k:
                start_char = s[start]
                frequency[start_char] -= 1
                if frequency[start_char] == 0:
                    del frequency[start_char]
                start += 1
            max_length = max(max_length, end - start + 1)
            end += 1
        return max_length

s = input()
k = int(input())
print(Solution().k_characters_span(s, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int kCharactersSpan(String s, int k) {
            Map<Character, Integer> frequency = new HashMap<>();
            int maxLength = 0, start = 0, end = 0;
            while (end < s.length()) {
                char endChar = s.charAt(end);
                frequency.put(endChar, frequency.getOrDefault(endChar, 0) + 1);
                while (frequency.size() > k) {
                    char startChar = s.charAt(start);
                    frequency.put(startChar, frequency.get(startChar) - 1);
                    if (frequency.get(startChar) == 0) frequency.remove(startChar);
                    start++;
                }
                maxLength = Math.max(maxLength, end - start + 1);
                end++;
            }
            return maxLength;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kCharactersSpan(s, k));
    }
}
```

### Dry Run

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

### Complexity Analysis

| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | `end` advances `N` times; `start` advances at most `N` times. Each character enters and leaves the map once, so the inner `while` is amortised `O(1)`. |
| **Space** | **O(K)** | The map holds at most `k + 1` entries during a contraction — bounded by the alphabet size. |

### Edge Cases

| Input | Output | Why |
|---|---|---|
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
