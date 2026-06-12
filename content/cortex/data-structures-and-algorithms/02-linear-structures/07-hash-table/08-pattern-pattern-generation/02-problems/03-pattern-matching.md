---
title: "Pattern Matching"
summary: "Given a pattern string and a string s of space-separated words, return true if s follows pattern — meaning there is a bijection between letters of pattern and non-empty words of s."
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
kind: problem
topics: [pattern-generation, hash-table]
---

# Pattern matching

## Problem Statement

Given a `pattern` string and a string `s` of space-separated words, return `true` if `s` follows `pattern` — meaning there is a **bijection** between letters of `pattern` and non-empty words of `s`.

### Example 1
> -   **Input:** `pattern = "mom", s = "hello world hello"` → **Output:** `true`

### Example 2
> -   **Input:** `pattern = "abc", s = "hello my name"` → **Output:** `true`

### Example 3
> -   **Input:** `pattern = "abc", s = "hello my my"` → **Output:** `false`

## Examples

**Example 1**
```
Input:  pattern = "mom", s = "hello world hello"
Output: true
Explanation: pattern keys to "0,1,0,"; words [hello, world, hello] key to "0,1,0,". Equal.
```

**Example 2**
```
Input:  pattern = "abc", s = "hello my name"
Output: true
Explanation: pattern keys to "0,1,2,"; words [hello, my, name] key to "0,1,2,". Equal.
```

**Example 3**
```
Input:  pattern = "abc", s = "hello my my"
Output: false
Explanation: pattern keys to "0,1,2," but words [hello, my, my] key to "0,1,1," — "my" repeats.
```

**Example 4**
```
Input:  pattern = "aab", s = "x x y"
Output: true
Explanation: pattern "aab" keys to "0,0,1,"; words [x, x, y] key to "0,0,1,". Equal.
```

## Constraints

- `1 ≤ pattern.length ≤ 300`
- `pattern` contains only lower-case English letters.
- `1 ≤ s.length ≤ 3000`
- `s` contains only lower-case English letters and spaces, with no leading or trailing spaces, no two consecutive spaces.

```python run
pattern = input()
s = input()

from typing import List

class Solution:
    def pattern_matching(self, pattern: str, s: str) -> bool:
        # Your code goes here
        pass

r = Solution().pattern_matching(pattern, s)
print("true" if r else "false")
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public boolean patternMatching(String pattern, String s) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String pattern = sc.nextLine();
        String s = sc.nextLine();
        System.out.println(new Solution().patternMatching(pattern, s));
    }
}
```

```testcases
{
  "args": [
    { "id": "pattern", "label": "pattern", "type": "string", "placeholder": "mom" },
    { "id": "s", "label": "s", "type": "string", "placeholder": "hello world hello" }
  ],
  "cases": [
    { "args": { "pattern": "mom", "s": "hello world hello" }, "expected": "true" },
    { "args": { "pattern": "abc", "s": "hello my name" }, "expected": "true" },
    { "args": { "pattern": "abc", "s": "hello my my" }, "expected": "false" },
    { "args": { "pattern": "a", "s": "hello" }, "expected": "true" },
    { "args": { "pattern": "aa", "s": "hello world" }, "expected": "false" },
    { "args": { "pattern": "ab", "s": "hello hello" }, "expected": "false" },
    { "args": { "pattern": "aab", "s": "x x y" }, "expected": "true" },
    { "args": { "pattern": "abc", "s": "a b c" }, "expected": "true" }
  ]
}
```

<details>
<summary>Editorial</summary>

The first-occurrence-index key generator works on *any* iterable, so treat `pattern` as a list of single characters and `s` as a list of words. Key each independently; the sequences match iff the keys are byte-identical. Reject on count mismatch before keying. The bijection requirement is enforced for free: two distinct items never share an index. `O(N)` time where `N` is the total input size; `O(W + P)` space for the two maps. Print `true`/`false` lowercase for consistent py/java output.

```python solution time=O(N) space=O(W+P)
pattern = input()
s = input()

from typing import List

class Solution:
    def generate_pattern(self, words: List[str]) -> str:
        word_to_index = {}
        pat = ""
        index = 0
        for word in words:
            if word not in word_to_index:
                word_to_index[word] = index
                index += 1
            pat += str(word_to_index[word]) + ","
        return pat

    def pattern_matching(self, pattern: str, s: str) -> bool:
        words = s.split(" ")
        if len(pattern) != len(words): return False
        return self.generate_pattern(list(pattern)) == self.generate_pattern(words)

r = Solution().pattern_matching(pattern, s)
print("true" if r else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private List<String> stringToList(String s) {
            List<String> result = new ArrayList<>();
            for (char c : s.toCharArray()) result.add(String.valueOf(c));
            return result;
        }

        private String generatePattern(List<String> words) {
            Map<String, Integer> wordToIndex = new HashMap<>();
            StringBuilder pattern = new StringBuilder();
            int index = 0;
            for (String word : words) {
                if (!wordToIndex.containsKey(word)) wordToIndex.put(word, index++);
                pattern.append(wordToIndex.get(word)).append(",");
            }
            return pattern.toString();
        }

        public boolean patternMatching(String pattern, String s) {
            List<String> words = List.of(s.split(" "));
            if (pattern.length() != words.size()) return false;
            return generatePattern(stringToList(pattern)).equals(generatePattern(words));
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String pattern = sc.nextLine();
        String s = sc.nextLine();
        System.out.println(new Solution().patternMatching(pattern, s));
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that "following a pattern" is a statement about *shared repeat structure* between two sequences of different element types. The `pattern` is a sequence of characters; `s` is a sequence of words. They match exactly when both sequences have the same shape — the same arrangement of firsts and repeats. That shape is the key.

The key generator works on *any* iterable, so the element type does not matter. Treat `pattern` as a list of single characters and `s` as a list of words; run the identical first-occurrence-index scan on each. Position in one key lines up with position in the other, so `s` follows `pattern` if and only if the two keys are byte-identical. A length mismatch — more words than pattern letters or vice versa — is an instant `false`.

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Split `s` into words, then key both sequences and compare.

1. **Split `s` into words.** Break the string on spaces to get the list of words.
2. **Reject on count mismatch.** If the number of words differs from the number of pattern letters, return `false`.
3. **Key the pattern letters.** Treat `pattern` as a list of single characters and run the first-occurrence-index scan.
4. **Key the words.** Run the identical scan over the word list.
5. **Compare the keys.** Return whether the two pattern strings are byte-identical.

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `pattern = "mom"`, `s = "hello world hello"`. Splitting `s` gives `[hello, world, hello]` — three words, matching the three pattern letters.

```
key(pattern) over chars [m, o, m]
  'm'  new → index 0   map={m:0}        pattern="0,"
  'o'  new → index 1   map={m:0,o:1}    pattern="0,1,"
  'm'  seen → reuse 0                   pattern="0,1,0,"

key(words) over [hello, world, hello]
  "hello"  new → index 0   map={hello:0}            pattern="0,"
  "world"  new → index 1   map={hello:0,world:1}    pattern="0,1,"
  "hello"  seen → reuse 0                            pattern="0,1,0,"

key(pattern) = "0,1,0,"  ==  key(words) = "0,1,0,"  →  return true
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `N` is the total character length of `s`; splitting, keying, and the final comparison are each linear. |
| Space | **O(W + P)** | The word map holds up to `W` distinct words and the letter map up to `P` distinct letters, plus the key strings. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single letter and word | `pattern = "a"`, `s = "hello"` | `true` | Both key to `"0,"` — one item, one index. |
| Same letters, distinct words | `pattern = "aa"`, `s = "hello world"` | `false` | `aa` → `"0,0,"` but the two distinct words → `"0,1,"`. |
| Distinct letters, same word | `pattern = "ab"`, `s = "hello hello"` | `false` | `ab` → `"0,1,"` but the repeated word → `"0,0,"`. |
| Repeat structure matches | `pattern = "aab"`, `s = "x x y"` | `true` | Both key to `"0,0,1,"` — first two repeat, third is new. |
| All distinct | `pattern = "abc"`, `s = "a b c"` | `true` | Both key to `"0,1,2,"` — three distinct items in order. |
| Count mismatch | `pattern = "ab"`, `s = "one two three"` | `false` | Two letters versus three words fails the length check before keying. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The new idea here is that the key generator is **element-type agnostic** — the same first-occurrence-index scan keys characters and words alike, so a cross-type match between a letter sequence and a word sequence reduces to comparing two keys.

</details>
