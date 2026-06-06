---
title: "Homomorphic Strings"
summary: "Given two strings s and t, return true if they are homomorphic: each unique character of s can be replaced (consistently, no two distinct characters mapping to the same target) to produce t."
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
---

# Homomorphic strings

## Problem Statement

Given two strings `s` and `t`, return `true` if they are **homomorphic**: each unique character of `s` can be replaced (consistently, no two distinct characters mapping to the same target) to produce `t`.

### Example 1
> -   **Input:** `s = "add", t = "qpp"` → **Output:** `true`

### Example 2
> -   **Input:** `s = "dad", t = "mom"` → **Output:** `true`

### Example 3
> -   **Input:** `s = "all", t = "mom"` → **Output:** `false`

## Examples

**Example 1**
```
Input:  s = "add", t = "qpp"
Output: true
Explanation: a→q and d→p. Both strings have shape "first, second, second" → key "0,1,1,".
```

**Example 2**
```
Input:  s = "dad", t = "mom"
Output: true
Explanation: d→m and a→o. Both have shape "first, second, first" → key "0,1,0,".
```

**Example 3**
```
Input:  s = "all", t = "mom"
Output: false
Explanation: "all" has shape "0,1,1," but "mom" has shape "0,1,0," — different repeat structure.
```

**Example 4**
```
Input:  s = "ab", t = "aa"
Output: false
Explanation: "ab" → "0,1," (two distinct chars) but "aa" → "0,0," (one repeated char).
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that homomorphism depends only on each string's *repeat structure*, not its actual characters. Two strings are homomorphic exactly when they have the same shape — the same pattern of "new character here, repeat of an earlier one there." That shape is the key.

The key per string is its **first-occurrence-index** encoding: the first distinct character becomes `0`, the second becomes `1`, and every repeat reuses the index its first appearance earned. Position in the key mirrors position in the string, so `s` and `t` are homomorphic if and only if their keys are byte-identical. A length mismatch is an instant `false` — strings of different lengths cannot share a shape.

The naive approach builds an explicit character-to-character map while walking both strings together, rejecting on any conflict. That works for one pair but reasons about a *relationship* between two inputs. Keying reframes the question as a property of each string alone: encode each independently, then compare. The comparison is one equality check, and the bijection constraint falls out for free — two distinct source characters never collide on one target because each earns a fresh index.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Homomorphic Strings |
|---|---|
| **Q1.** Does the answer depend on a *canonical form* of each input? | **Yes** — the first-occurrence-index key captures each string's shape; homomorphism is key-equality. |
| **Q2.** Can you define equivalence as a function from input to bytes? | **Yes** — `generate_pattern` maps each string to a byte string; equal bytes mean homomorphic. |
| **Q3.** Is each input keyed independently in a single pass? | **Yes** — `s` and `t` are each scanned once on their own, then compared. |
| **Q4.** Is the per-item work `O(1)`? | **Yes** — each character is one map lookup and one append, both `O(1)` amortised. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Apply the `generatePattern` function we built above to both strings; compare the resulting keys. The first-occurrence-index encoding *is* the canonical shape of a string, so two strings are homomorphic iff their patterns match exactly.

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Encode each string's shape, then compare.

1. **Reject on length mismatch.** If `s` and `t` differ in length, return `false` — they cannot share a shape.
2. **Key the first string.** Scan `s`, assigning each new character the next index and reusing the index for repeats, building `s`'s pattern string.
3. **Key the second string.** Run the identical scan on `t`, building `t`'s pattern string.
4. **Compare the keys.** Return whether the two pattern strings are byte-identical.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=graph viz-root=char_to_index
from typing import Dict

class Solution:
    def generate_pattern(self, s: str) -> str:
        char_to_index: Dict[str, int] = {}
        pattern = ""
        index = 0

        # Create a mapped value based on the first occurrence of each
        # character
        for ch in s:
            if ch not in char_to_index:
                char_to_index[ch] = index
                index += 1
            pattern += str(char_to_index[ch]) + ","

        return pattern

    def homomorphic_strings(self, s: str, t: str) -> bool:

        # Strings of different lengths can't be homomorphic
        if len(s) != len(t):
            return False

        # If the generated patterns are the same, the strings are
        # homomorphic
        return self.generate_pattern(s) == self.generate_pattern(t)


# Examples from the problem statement
print(Solution().homomorphic_strings("add", "qpp"))   # True
print(Solution().homomorphic_strings("dad", "mom"))   # True
print(Solution().homomorphic_strings("all", "mom"))   # False

# Edge cases
print(Solution().homomorphic_strings("", ""))         # True
print(Solution().homomorphic_strings("a", "b"))       # True
print(Solution().homomorphic_strings("ab", "aa"))     # False
print(Solution().homomorphic_strings("aa", "ab"))     # False
print(Solution().homomorphic_strings("abc", "xyz"))   # True
```

```java run viz=graph viz-root=char_to_index
import java.util.*;

public class Main {
    static class Solution {
        private String generatePattern(String str) {
            Map<Character, Integer> charToIndex = new HashMap<>();
            StringBuilder pattern = new StringBuilder();
            int index = 0;

            // Create a mapped value based on the first occurrence of each
            // character
            for (char ch : str.toCharArray()) {
                if (!charToIndex.containsKey(ch)) {
                    charToIndex.put(ch, index++);
                }
                pattern.append(charToIndex.get(ch)).append(",");
            }

            return pattern.toString();
        }

        public boolean homomorphicStrings(String s, String t) {

            // Strings of different lengths can't be homomorphic
            if (s.length() != t.length()) {
                return false;
            }

            // If the generated patterns are the same, the strings are
            // homomorphic
            return generatePattern(s).equals(generatePattern(t));
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().homomorphicStrings("add", "qpp"));  // true
        System.out.println(new Solution().homomorphicStrings("dad", "mom"));  // true
        System.out.println(new Solution().homomorphicStrings("all", "mom"));  // false

        // Edge cases
        System.out.println(new Solution().homomorphicStrings("", ""));        // true
        System.out.println(new Solution().homomorphicStrings("a", "b"));      // true
        System.out.println(new Solution().homomorphicStrings("ab", "aa"));    // false
        System.out.println(new Solution().homomorphicStrings("aa", "ab"));    // false
        System.out.println(new Solution().homomorphicStrings("abc", "xyz"));  // true
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `s = "add"`, `t = "qpp"`. Both have equal length, so the keys are built and compared.

```
key(s) for "add"
  ch='a'  new → index 0   charToIndex={a:0}        pattern="0,"
  ch='d'  new → index 1   charToIndex={a:0,d:1}    pattern="0,1,"
  ch='d'  seen → reuse 1                           pattern="0,1,1,"

key(t) for "qpp"
  ch='q'  new → index 0   charToIndex={q:0}        pattern="0,"
  ch='p'  new → index 1   charToIndex={q:0,p:1}    pattern="0,1,"
  ch='p'  seen → reuse 1                           pattern="0,1,1,"

key(s) = "0,1,1,"  ==  key(t) = "0,1,1,"  →  return True
```

The result `true` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `N` is the string length; each string is scanned once, and the final key comparison is `O(N)`. |
| Space | **O(K)** | Each `charToIndex` map holds one entry per distinct character — `K` entries — plus an `O(N)` key string. |

`K` ranges from `1` (all characters identical) to `N` (all distinct), but the scan length is fixed by `N`, so the time stays `O(N)` in every case.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Both empty | `s = ""`, `t = ""` | `true` | Two empty keys (`""`) are trivially equal. |
| Single distinct chars | `s = "a"`, `t = "b"` | `true` | Both key to `"0,"` — one character, one index. |
| Distinct vs repeated | `s = "ab"`, `t = "aa"` | `false` | `ab` → `"0,1,"` but `aa` → `"0,0,"` — different shapes. |
| Repeated vs distinct | `s = "aa"`, `t = "ab"` | `false` | `aa` → `"0,0,"` but `ab` → `"0,1,"` — symmetric to the previous case. |
| All distinct, equal length | `s = "abc"`, `t = "xyz"` | `true` | Both key to `"0,1,2,"` — three distinct characters in order. |
| Length mismatch | `s = "ab"`, `t = "a"` | `false` | Different lengths fail the early check before any keying. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


This is the canonical first-occurrence-index problem: key each string by its repeat structure and compare. What is new versus the row-specific filter is that the key is a *derived order*, not a fixed categorical label, and the answer is a single equality between two keys.

</details>