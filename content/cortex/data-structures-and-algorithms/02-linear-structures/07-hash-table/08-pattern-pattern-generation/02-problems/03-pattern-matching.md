---
title: "Pattern Matching"
summary: "Given a pattern string and a string s of space-separated words, return true if s follows pattern — meaning there is a bijection between letters of pattern and non-empty words of s."
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
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


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that "following a pattern" is a statement about *shared repeat structure* between two sequences of different element types. The `pattern` is a sequence of characters; `s` is a sequence of words. They match exactly when both sequences have the same shape — the same arrangement of firsts and repeats. That shape is the key.

The key generator works on *any* iterable, so the element type does not matter. Treat `pattern` as a list of single characters and `s` as a list of words; run the identical first-occurrence-index scan on each. Position in one key lines up with position in the other, so `s` follows `pattern` if and only if the two keys are byte-identical. A length mismatch — more words than pattern letters or vice versa — is an instant `false`.

The bijection requirement is what would make a naive approach fiddly: no two pattern letters may map to the same word, *and* no two words may map to the same letter. The first-occurrence-index encoding enforces both directions for free. If two different items would ever need the same token, they don't get it — each new item earns a fresh index — so any difference in distinct-item count shows up as a key mismatch.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Pattern Matching |
|---|---|
| **Q1.** Does the answer depend on a *canonical form* of each input? | **Yes** — both the letter sequence and the word sequence reduce to first-occurrence-index keys; matching is key-equality. |
| **Q2.** Can you define equivalence as a function from input to bytes? | **Yes** — `generate_pattern` maps each sequence to a byte string regardless of element type. |
| **Q3.** Is each input keyed independently in a single pass? | **Yes** — the letters and the words are each scanned once, then the two keys are compared. |
| **Q4.** Is the per-item work `O(1)`? | **Yes** — each character or word is one map lookup and one append, both `O(1)` amortised. |

</details>
<details>
<summary><h2>Approach</h2></summary>


The key generator works on *any* iterable. Treat `pattern` as a sequence of characters and `s` as a sequence of words; generate a first-occurrence-index pattern for each. The strings match iff the keys are equal.

The bijection requirement is a real constraint: no two pattern letters can map to the same word, *and* no two words can map to the same pattern letter. The first-occurrence-index encoding handles both directions: if it would assign two different items the same index, it doesn't — each gets a fresh index. So if `s` has more distinct words than `pattern` has distinct letters (or vice versa), the keys differ.

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
<summary><h2>Solution</h2></summary>



```python run viz=graph viz-root=words
from typing import List

class Solution:
    def generate_pattern(self, words: List[str]) -> str:
        word_to_index = {}
        pattern = ""
        index = 0

        # Create a mapped value based on the first occurrence of each
        # word
        for word in words:
            if word not in word_to_index:
                word_to_index[word] = index
                index += 1
            pattern += str(word_to_index[word]) + ","

        return pattern

    def pattern_matching(self, pattern: str, s: str) -> bool:

        # Split the string s into an array of words
        words = s.split(" ")

        # If the length of pattern and words are different, return false
        if len(pattern) != len(words):
            return False

        # If the generated patterns are the same, return true
        return self.generate_pattern(
            list(pattern)
        ) == self.generate_pattern(words)


# Examples from the problem statement
print(Solution().pattern_matching("mom", "hello world hello"))  # True
print(Solution().pattern_matching("abc", "hello my name"))      # True
print(Solution().pattern_matching("abc", "hello my my"))        # False

# Edge cases
print(Solution().pattern_matching("a", "hello"))                # True
print(Solution().pattern_matching("aa", "hello world"))         # False
print(Solution().pattern_matching("ab", "hello hello"))         # False
print(Solution().pattern_matching("aab", "x x y"))              # True
print(Solution().pattern_matching("abc", "a b c"))              # True
```

```java run viz=graph viz-root=words
import java.util.*;

public class Main {
    static class Solution {
        private List<String> stringToList(String s) {

            // Convert pattern string into a list of single-character strings
            List<String> result = new ArrayList<>();
            for (char c : s.toCharArray()) {
                result.add(String.valueOf(c));
            }
            return result;
        }

        private String generatePattern(List<String> words) {
            Map<String, Integer> wordToIndex = new HashMap<>();
            StringBuilder pattern = new StringBuilder();
            int index = 0;

            // Create a mapped value based on the first occurrence of each
            // word
            for (String word : words) {
                if (!wordToIndex.containsKey(word)) {
                    wordToIndex.put(word, index++);
                }
                pattern.append(wordToIndex.get(word)).append(",");
            }

            return pattern.toString();
        }

        public boolean patternMatching(String pattern, String s) {

            // Split the string s into an array of words
            List<String> words = List.of(s.split(" "));

            // If the length of pattern and words are different, return false
            if (pattern.length() != words.size()) {
                return false;
            }

            // If the generated patterns are the same, return true
            return generatePattern(stringToList(pattern)).equals(
                generatePattern(words)
            );
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().patternMatching("mom", "hello world hello")); // true
        System.out.println(new Solution().patternMatching("abc", "hello my name"));     // true
        System.out.println(new Solution().patternMatching("abc", "hello my my"));       // false

        // Edge cases
        System.out.println(new Solution().patternMatching("a", "hello"));               // true
        System.out.println(new Solution().patternMatching("aa", "hello world"));        // false
        System.out.println(new Solution().patternMatching("ab", "hello hello"));        // false
        System.out.println(new Solution().patternMatching("aab", "x x y"));             // true
        System.out.println(new Solution().patternMatching("abc", "a b c"));             // true
    }
}
```

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

key(pattern) = "0,1,0,"  ==  key(words) = "0,1,0,"  →  return True
```

The result `true` matches the expected output.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(N)** | `N` is the total character length of `s`; splitting, keying, and the final comparison are each linear. |
| Space | **O(W + P)** | The word map holds up to `W` distinct words and the letter map up to `P` distinct letters, plus the key strings. |

Keying a word costs work proportional to that word's length for the map lookup, so the whole pass is linear in the total input size rather than the word count alone.

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