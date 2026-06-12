---
title: "Maximal Character Swap"
summary: "Given an uppercase string s and integer k, you may replace at most k characters with any uppercase letters of your choice. Return the length of the longest substring of equal letters achievable."
prereqs:
  - 10-pattern-variable-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [variable-sized-sliding-window, hash-table]
---

# Maximal character swap

## Problem Statement

Given an uppercase string `s` and integer `k`, you may replace at most `k` characters with any uppercase letters of your choice. Return the length of the longest substring of equal letters achievable.

## Examples

**Example 1**
```
Input:  s = "ABAB", k = 2
Output: 4
Explanation: the window has 4 characters with A appearing twice. Replacing the two
B's (4 − 2 = 2 ≤ k) makes the whole string one letter → length 4.
```

**Example 2**
```
Input:  s = "ABCDEF", k = 4
Output: 5
Explanation: pick any single letter as the dominant one (count 1). A window of 5
needs 5 − 1 = 4 replacements, exactly k → length 5.
```

**Example 3**
```
Input:  s = "A", k = 5
Output: 1
Explanation: one character, already uniform. The generous budget is irrelevant →
length 1.
```

**Example 4**
```
Input:  s = "AABB", k = 1
Output: 3
Explanation: a window like "AAB" has dominant A (count 2); 3 − 2 = 1 ≤ k replacement
makes it "AAA" → length 3. A 4-wide window would need 2 replacements.
```

<details>
<summary><h2>Approach</h2></summary>


For a window `[start..end]` to be turn-able into all-same-letter with ≤ K replacements, it must satisfy `(window_size − count_of_most_frequent_letter) ≤ k`. The "extra" characters (everything except the dominant letter) are exactly what we'd need to replace.

So slide the window; track frequencies; track `maxFreq` (the highest count any letter has had so far in the window). The rule is `(end − start + 1) − maxFreq > k` → contract.

A subtle but allowed shortcut: when contracting, we *don't* need to shrink `maxFreq` — even a stale `maxFreq` is a valid lower bound, and the answer only cares about the maximum window seen, which only grows when `maxFreq` grows. This makes the algorithm clean and still correct.

```d2
direction: right

w: |md
  **window 'AABA'**

  size 4

  most freq: A -> 3
| {style.fill: "#fef9c3"; style.stroke: "#d97706"}

calc: "replacements needed = 4 - 3 = 1"

ok: "<= k = 2 ? yes -> window valid" {style.fill: "#dcfce7"; style.stroke: "#16a34a"}

w -> calc -> ok
```

<p align="center"><strong>Maximal character swap — replacements needed = window size − count of most frequent letter. As long as that count is ≤ K, the window is achievable.</strong></p>

</details>
<details>
<summary><h2>Intuition</h2></summary>


This is a **longest contiguous subsequence** problem with a rule that reads off a frequency map indirectly. A window of width `w` can be made all-one-letter with `w − maxFreq` replacements, where `maxFreq` is the count of the window's most frequent letter — those are exactly the non-dominant characters you would overwrite. The window is achievable when `w − maxFreq ≤ k`. Because the answer is a contiguous run and the rule is a count comparison, the variable-sized sliding window fits.

The pointers move asymmetrically, with one twist. The `end` pointer admits a character and updates `maxFreq` to the highest count seen. The `start` pointer contracts while `(end − start + 1) − maxFreq > k` — too many characters to replace. The twist: when contracting, you do **not** lower `maxFreq`, even though evicting a character could in principle reduce it. A stale `maxFreq` is a safe lower bound, and the answer only grows when `maxFreq` grows, so leaving it stale never produces a wrong, larger window. This keeps the inner loop branch-free.

The naive approach is correct but quadratic. Trying every window and counting its dominant letter from scratch is **O(N²)** or worse. The window approach never rewinds `start`, and since `maxLength` only increases when a genuinely better dominant count appears, the single forward pass suffices — **O(N)** total.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Maximal Character Swap |
|---|---|
| **Q1.** Is the answer the longest/shortest/count of a contiguous subsequence? | **Yes** — the longest window turn-able into one repeated letter with `≤ k` swaps. |
| **Q2.** Can a hash map summarise the window for an `O(1)` rule check? | **Yes** — a frequency map plus a running `maxFreq`; the rule is `(width − maxFreq) ≤ k`. |
| **Q3.** Can you add `s[end]` and remove `s[start]` in `O(1)`? | **Yes** — increment-and-update-`maxFreq` on expand; decrement on contract. |
| **Q4.** Is the rule monotonic as the window grows? | **Yes** — widening without raising `maxFreq` only increases the replacements needed. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialise `start = 0`, an empty `frequency` map, `maxFreq = 0`, and `maxLength = 0`.
2. Advance `end` across the string. For each `end`, increment `frequency[s[end]]` and set `maxFreq = max(maxFreq, frequency[s[end]])`.
3. While `(end − start + 1) − maxFreq > k` — more than `k` characters would need replacing — contract from the left: decrement `frequency[s[start]]` and advance `start`. Leave `maxFreq` unchanged.
4. Record `maxLength = max(maxLength, end − start + 1)`.
5. After the loop, return `maxLength`.

</details>

```quiz
{
  "prompt": "What does maximal_character_swap(\"AABB\", 1) return?",
  "input": "s = \"AABB\", k = 1",
  "options": ["4", "3", "2", "1"],
  "answer": "3"
}
```

## Constraints

- `1 ≤ s.length ≤ 10⁵`
- `s` consists of uppercase English letters
- `0 ≤ k ≤ s.length`

```python run
s = input()
k = int(input())

class Solution:
    def maximal_character_swap(self, s: str, k: int) -> int:
        # Your code goes here
        return 0

print(Solution().maximal_character_swap(s, k))
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public int maximalCharacterSwap(String s, int k) {
            // Your code goes here
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().maximalCharacterSwap(s, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "ABAB" },
    { "id": "k", "label": "k", "type": "number", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "s": "ABAB", "k": "2" }, "expected": "4" },
    { "args": { "s": "ABCDEF", "k": "4" }, "expected": "5" },
    { "args": { "s": "A", "k": "5" }, "expected": "1" },
    { "args": { "s": "AABB", "k": "1" }, "expected": "3" },
    { "args": { "s": "AA", "k": "0" }, "expected": "2" },
    { "args": { "s": "AB", "k": "0" }, "expected": "1" },
    { "args": { "s": "AAAA", "k": "2" }, "expected": "4" }
  ]
}
```

<details>
<summary>Editorial</summary>

Expand `end`, update `frequency[s[end]]` and `maxFreq`. Contract while `(end − start + 1) − maxFreq > k`, decrementing `frequency[s[start]]` but leaving `maxFreq` stale — a safe lower bound that only grows when a genuinely better window appears. Record `end − start + 1`. `O(n)` time, `O(1)` space (26-letter alphabet).

```python solution time=O(n) space=O(1)
from collections import defaultdict

class Solution:
    def maximal_character_swap(self, s: str, k: int) -> int:
        frequency = defaultdict(int)
        start, end = 0, 0
        max_freq = 0
        max_length = 0
        while end < len(s):
            char_end = s[end]
            frequency[char_end] += 1
            max_freq = max(max_freq, frequency[char_end])
            while end - start + 1 - max_freq > k:
                frequency[s[start]] -= 1
                start += 1
            max_length = max(max_length, end - start + 1)
            end += 1
        return max_length

s = input()
k = int(input())
print(Solution().maximal_character_swap(s, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int maximalCharacterSwap(String s, int k) {
            Map<Character, Integer> frequency = new HashMap<>();
            int start = 0, end = 0, maxFreq = 0, maxLength = 0;
            while (end < s.length()) {
                char endChar = s.charAt(end);
                frequency.put(endChar, frequency.getOrDefault(endChar, 0) + 1);
                maxFreq = Math.max(maxFreq, frequency.get(endChar));
                while (end - start + 1 - maxFreq > k) {
                    char startChar = s.charAt(start);
                    frequency.put(startChar, frequency.get(startChar) - 1);
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
        System.out.println(new Solution().maximalCharacterSwap(s, k));
    }
}
```

### Dry Run

Walk Example 1: `s = "ABAB"`, `k = 2`, expected output `4`. The rule is `(width − maxFreq) ≤ k`:

```
end=0  add 'A'  freq={A:1}        maxFreq=1  width 1, 1−1=0 ≤ 2   len 1  maxLength=1
end=1  add 'B'  freq={A:1,B:1}    maxFreq=1  width 2, 2−1=1 ≤ 2   len 2  maxLength=2
end=2  add 'A'  freq={A:2,B:1}    maxFreq=2  width 3, 3−2=1 ≤ 2   len 3  maxLength=3
end=3  add 'B'  freq={A:2,B:2}    maxFreq=2  width 4, 4−2=2 ≤ 2   len 4  maxLength=4

return maxLength = 4
```

### Complexity Analysis

| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | `end` advances `N` times; `start` advances at most `N` times. Each character enters and leaves the map once, so the inner `while` is amortised `O(1)`. |
| **Space** | **O(1)** | The map holds at most one entry per distinct character — bounded by the 26-letter alphabet. |

### Edge Cases

| Input | Output | Why |
|---|---|---|
| `s = "AA", k = 0` | `2` | Already uniform — zero replacements needed; the window spans both. |
| `s = "AB", k = 0` | `1` | With no budget, the window can hold only one matching letter at a time. |
| `s = "AABB", k = 1` | `3` | A 3-wide window has a dominant letter with count `2`; one swap suffices. |
| `s = "AAAA", k = 2` | `4` | Already uniform — the budget is unused, window covers all. |
| `s = "A", k = 5` | `1` | Single character — a generous budget cannot extend a length-1 string. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The rule here is `(width − maxFreq) ≤ k`, where `maxFreq` is the dominant letter's count. The non-obvious move is leaving `maxFreq` stale on contraction — it stays a safe lower bound, so the window never grows on a false premise.

</details>
