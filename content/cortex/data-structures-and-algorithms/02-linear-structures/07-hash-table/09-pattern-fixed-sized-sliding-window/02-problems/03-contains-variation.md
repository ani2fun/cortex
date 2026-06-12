---
title: "Contains Variation"
summary: "Given two strings s1 and s2, return true if s2 contains a permutation of s1, else false."
prereqs:
  - 09-pattern-fixed-sized-sliding-window/01-pattern
difficulty: medium
kind: problem
topics: [fixed-sized-sliding-window, hash-table]
---

# Contains variation

## Problem Statement

Given two strings `s1` and `s2`, return `true` if `s2` contains a permutation of `s1`, else `false`.

### Example 1
> -   **Input:** `s1 = "abc", s2 = "edbaclm"` → **Output:** `true` (`"bac"` is a permutation of `"abc"`)

### Example 2
> -   **Input:** `s1 = "cod", s2 = "intdoce"` → **Output:** `true` (`"doc"`)

### Example 3
> -   **Input:** `s1 = "abc", s2 = "defghiab"` → **Output:** `false`

## Examples

**Example 1**
```
Input:  s1 = "abc", s2 = "edbaclm"
Output: true
Explanation: the window "bac" is a permutation of "abc" → match found.
```

**Example 2**
```
Input:  s1 = "cod", s2 = "intdoce"
Output: true
Explanation: the window "doc" is a permutation of "cod" → match found.
```

**Example 3**
```
Input:  s1 = "abc", s2 = "defghiab"
Output: false
Explanation: no length-3 window of s2 is a permutation of "abc".
```

**Example 4**
```
Input:  s1 = "aa", s2 = "aab"
Output: true
Explanation: the window "aa" matches s1's map {a:2} → match found.
```

## Constraints

- `1 <= len(s1) <= len(s2)`
- Both strings contain only lowercase letters

```python run
def contains_variation(s1, s2):
    # Your code goes here
    pass

s1 = input()
s2 = input()
r = contains_variation(s1, s2)
print("true" if r else "false")
```

```java run
import java.util.*;

public class Main {
  static boolean containsVariation(String s1, String s2) {
    // Your code goes here
    return false;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    String s1 = sc.nextLine();
    String s2 = sc.nextLine();
    System.out.println(containsVariation(s1, s2));
  }
}
```

```testcases
{
  "args": [
    { "id": "s1", "label": "s1", "type": "string", "placeholder": "abc" },
    { "id": "s2", "label": "s2", "type": "string", "placeholder": "edbaclm" }
  ],
  "cases": [
    { "args": { "s1": "abc", "s2": "edbaclm"  }, "expected": "true"  },
    { "args": { "s1": "cod", "s2": "intdoce"  }, "expected": "true"  },
    { "args": { "s1": "abc", "s2": "defghiab" }, "expected": "false" },
    { "args": { "s1": "aa",  "s2": "aab"      }, "expected": "true"  },
    { "args": { "s1": "ab",  "s2": "ba"       }, "expected": "true"  },
    { "args": { "s1": "abc", "s2": "ab"       }, "expected": "false" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **fixed-sized sliding window** problem is the window width: a permutation of `s1` has exactly `len(s1)` characters, so every candidate window in `s2` is exactly that size. A permutation is a multiset of characters, and a frequency map is precisely a multiset, so the window's map is the summary to maintain.

The window's two pointers each carry a fixed job. The `end` pointer adds the entering character; the `start` pointer drops the leaving one once the window reaches `len(s1)`. A window is a permutation of `s1` exactly when its frequency map equals `s1`'s frequency map — so the test is a map-equality check, run only when the window is the right width.

The naive approach breaks the time budget. For each of the `len(s2) − len(s1) + 1` windows it builds a fresh map and compares, costing `O(N·K)` time for `O(K)` space where `K = len(s1)`. That re-counts the `K − 1` shared characters on every slide. The sliding window edits the map in `O(1)` per step, so only the equality check remains per window.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Contains Variation |
|---|---|
| **Q1.** Is the window size fixed at exactly `k`? | **Yes** — every window is `len(s1)` wide; the size comes from the first string. |
| **Q2.** Is the input a linear sequence? | **Yes** — `s2` is a string, walked character by character. |
| **Q3.** Is the per-window answer read from an `O(1)`-updatable map? | **Yes** — the window's frequency map is compared against `s1`'s; the map itself updates in `O(1)`. |
| **Q4.** Is the per-step work `O(1)` amortised? | **Yes** — one increment on expand and one decrement on contract; the equality check is bounded by the alphabet. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Pre-count `s1`, then slide a window of size `len(s1)` over `s2`, comparing maps each time the window is full.

1. **Build `s1`'s frequency map.** Count every character of `s1` once; this is the target multiset.
2. **Add the entering character.** Read `s2[end]` and increment its count in the window map.
3. **Compare when the window is full.** When `end − start + 1 == len(s1)`, test whether the window map equals `s1`'s map; if so, return `true`.
4. **Contract from the left.** Still in the full-window branch, decrement `s2[start]`'s count, delete the key if it hits zero, and advance `start`.
5. **Advance and finish.** Increment `end` and continue; return `false` if no window ever matches.

> *Optimisation* — to avoid an `O(K)` map comparison each step, track a counter of how many distinct characters hold *exactly* the right count. The solution below uses the clearer `map == map` check; the counter only matters for large `K`.

</details>
<details>
<summary>Editorial</summary>

Build `s1`'s frequency map. Slide a window of exactly `len(s1)` characters over `s2`: increment the entering character, compare maps when the window is full, then evict the leaving character (decrement, delete at zero). Return `true` on the first match; `false` if no window matches. Delete-at-zero keeps the window map's key set honest for the equality check. `O(n)` time, `O(A)` space for alphabet size `A`.

```python solution time=O(n) space=O(A)
from collections import defaultdict

def contains_variation(s1, s2):
    def count_frequency(s):
        d = defaultdict(int)
        for ch in s:
            d[ch] += 1
        return d

    s1_frequency = count_frequency(s1)
    frequency = defaultdict(int)
    start, end = 0, 0
    while end < len(s2):
        char_end = s2[end]
        frequency[char_end] += 1
        if end - start + 1 == len(s1):
            if frequency == s1_frequency:
                return True
            char_start = s2[start]
            frequency[char_start] -= 1
            if frequency[char_start] == 0:
                del frequency[char_start]
            start += 1
        end += 1
    return False

s1 = input()
s2 = input()
r = contains_variation(s1, s2)
print("true" if r else "false")
```

```java solution
import java.util.*;

public class Main {
  static Map<Character, Integer> countFrequency(String s) {
    Map<Character, Integer> d = new HashMap<>();
    for (char ch : s.toCharArray()) d.put(ch, d.getOrDefault(ch, 0) + 1);
    return d;
  }

  static boolean containsVariation(String s1, String s2) {
    Map<Character, Integer> s1Frequency = countFrequency(s1);
    Map<Character, Integer> frequency = new HashMap<>();
    int start = 0, end = 0;
    while (end < s2.length()) {
      char endChar = s2.charAt(end);
      frequency.put(endChar, frequency.getOrDefault(endChar, 0) + 1);
      if (end - start + 1 == s1.length()) {
        if (frequency.equals(s1Frequency)) return true;
        char startChar = s2.charAt(start);
        frequency.put(startChar, frequency.get(startChar) - 1);
        if (frequency.get(startChar) == 0) frequency.remove(startChar);
        start++;
      }
      end++;
    }
    return false;
  }

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    String s1 = sc.nextLine();
    String s2 = sc.nextLine();
    System.out.println(containsVariation(s1, s2));
  }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s1 = "abc"`, `s2 = "edbaclm"`, window size `3`. The target map is `{a:1, b:1, c:1}`:

```
s1_frequency = {a:1, b:1, c:1}
start=0, end=0, frequency={}

end=0  add e → freq={e:1}            size 1 < 3 → continue
end=1  add d → freq={e:1,d:1}        size 2 < 3 → continue
end=2  add b → freq={e:1,d:1,b:1}    size 3 == 3 → {e,d,b} ≠ target
                                      drop s2[0]=e → freq={d:1,b:1}, start=1
end=3  add a → freq={d:1,b:1,a:1}    size 3 == 3 → {d,b,a} ≠ target
                                      drop s2[1]=d → freq={b:1,a:1}, start=2
end=4  add c → freq={b:1,a:1,c:1}    size 3 == 3 → {a,b,c} == target → return true

result = true
```

The result `true` matches the expected output — the window `"bac"` at `start=2` has map `{a:1, b:1, c:1}`, equal to `s1`'s map.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N · A)** | `N = len(s2)`; `end` sweeps once with `O(1)` map edits, but each full-window map-equality check costs `O(A)` for an alphabet of size `A` (constant for fixed alphabets, so effectively `O(N)`). |
| Space | **O(A)** | Two maps — `s1`'s and the window's — each bounded by the alphabet size `A`. |

The counter optimisation in the Approach note collapses the per-window check to `O(1)`, giving a strict `O(N)` time bound.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Equal single chars | `s1 = "a", s2 = "a"` | `true` | The one-character window matches `s1`'s map `{a:1}`. |
| Reversed pair | `s1 = "ab", s2 = "ba"` | `true` | `"ba"` is a permutation of `"ab"`; maps both `{a:1, b:1}`. |
| Exact match | `s1 = "abc", s2 = "abc"` | `true` | The whole of `s2` is the single window and matches. |
| `s2` shorter than `s1` | `s1 = "abc", s2 = "ab"` | `false` | No window of size `3` exists in a length-`2` string. |
| Repeated letters | `s1 = "aa", s2 = "aab"` | `true` | The window `"aa"` matches `s1`'s map `{a:2}`. |
| No permutation | `s1 = "abc", s2 = "defghiab"` | `false` | No length-`3` window equals `{a:1, b:1, c:1}`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is a pattern-match shape of the fixed window: the size is `len(s1)`, and a window matches when its frequency map equals `s1`'s. Returning on the first match makes it the single-answer cousin of Anagram Finder, which appends every matching index instead.

</details>