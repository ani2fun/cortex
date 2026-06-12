---
title: "Homomorphic Strings"
summary: "Given two strings s and t, return true if they are homomorphic: each unique character of s can be replaced (consistently, no two distinct characters mapping to the same target) to produce t."
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
kind: problem
topics: [pattern-generation, hash-table]
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

## Constraints

- `1 ≤ s.length, t.length ≤ 5 × 10⁴`
- `s` and `t` consist of any valid ASCII characters.

```python run
s = input()
t = input()

class Solution:
    def homomorphic_strings(self, s: str, t: str) -> bool:
        # Your code goes here
        pass

r = Solution().homomorphic_strings(s, t)
print("true" if r else "false")
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public boolean homomorphicStrings(String s, String t) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String t = sc.nextLine();
        System.out.println(new Solution().homomorphicStrings(s, t));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "add" },
    { "id": "t", "label": "t", "type": "string", "placeholder": "qpp" }
  ],
  "cases": [
    { "args": { "s": "add", "t": "qpp" }, "expected": "true" },
    { "args": { "s": "dad", "t": "mom" }, "expected": "true" },
    { "args": { "s": "all", "t": "mom" }, "expected": "false" },
    { "args": { "s": "ab", "t": "aa" }, "expected": "false" },
    { "args": { "s": "abc", "t": "xyz" }, "expected": "true" },
    { "args": { "s": "a", "t": "b" }, "expected": "true" },
    { "args": { "s": "aa", "t": "ab" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

Generate the first-occurrence-index key for each string independently, then compare. Two strings are homomorphic exactly when they share the same shape — the same pattern of firsts and repeats. A length mismatch is an instant `false`. The key comparison is a single equality check; the bijection constraint falls out for free because two distinct source characters never share an index. `O(N)` time (one scan per string plus the key comparison), `O(K)` space for the character map where `K ≤ N`. Print `true`/`false` in lowercase for consistent py/java output.

```python solution time=O(N) space=O(K)
s = input()
t = input()

class Solution:
    def generate_pattern(self, s: str) -> str:
        char_to_index = {}
        pattern = ""
        index = 0
        for ch in s:
            if ch not in char_to_index:
                char_to_index[ch] = index
                index += 1
            pattern += str(char_to_index[ch]) + ","
        return pattern

    def homomorphic_strings(self, s: str, t: str) -> bool:
        if len(s) != len(t): return False
        return self.generate_pattern(s) == self.generate_pattern(t)

r = Solution().homomorphic_strings(s, t)
print("true" if r else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private String generatePattern(String str) {
            Map<Character, Integer> charToIndex = new HashMap<>();
            StringBuilder pattern = new StringBuilder();
            int index = 0;
            for (char ch : str.toCharArray()) {
                if (!charToIndex.containsKey(ch)) charToIndex.put(ch, index++);
                pattern.append(charToIndex.get(ch)).append(",");
            }
            return pattern.toString();
        }

        public boolean homomorphicStrings(String s, String t) {
            if (s.length() != t.length()) return false;
            return generatePattern(s).equals(generatePattern(t));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String t = sc.nextLine();
        System.out.println(new Solution().homomorphicStrings(s, t));
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that homomorphism depends only on each string's *repeat structure*, not its actual characters. Two strings are homomorphic exactly when they have the same shape — the same pattern of "new character here, repeat of an earlier one there." That shape is the key.

The key per string is its **first-occurrence-index** encoding: the first distinct character becomes `0`, the second becomes `1`, and every repeat reuses the index its first appearance earned. Position in the key mirrors position in the string, so `s` and `t` are homomorphic if and only if their keys are byte-identical. A length mismatch is an instant `false` — strings of different lengths cannot share a shape.

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

key(s) = "0,1,1,"  ==  key(t) = "0,1,1,"  →  return true
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `N` is the string length; each string is scanned once, and the final key comparison is `O(N)`. |
| Space | **O(K)** | Each `charToIndex` map holds one entry per distinct character — `K` entries — plus an `O(N)` key string. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
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
