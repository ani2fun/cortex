---
title: "Sort Characters by Frequency"
summary: "Given a string s, return it with characters reordered: highest-frequency first, then lexicographic order on ties."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: medium
---

# Sort Characters by Frequency

Sort a string's characters by frequency (descending), with lexicographic tiebreaks.

---

## The Problem

Given a string `s`, return it with characters reordered: highest-frequency first, then lexicographic order on ties.

```
Input:  s = "eeeaaabc"
Output: "aaaeeebc"

Input:  s = "zzzxxyyyb"
Output: "yyyzzzxxb"

Input:  s = "zzzxxyyb"
Output: "zzzxxyyb"
```

---

<details>
<summary><h2>The Custom Compare</h2></summary>


Transform: `t(c) = (-frequency[c], c)`. The negative sign reverses the order on frequency (we want descending). The tuple's second element gives lex tiebreaks ascending.

</details>
<details>
<summary><h2>The Solution</h2></summary>


```python run viz=hashmap viz-root=frequency
from collections import Counter

class Solution:
    def sort_characters_by_frequency(self, s: str) -> str:

        # Create a Counter to store the frequency of characters
        frequency = Counter(s)

        # Convert frequency items into a list of tuples (character,
        # frequency)
        char_freq = list(frequency.items())

        # Sort the list using a lambda function
        char_freq.sort(key=lambda x: (-x[1], x[0]))

        # Explanation:
        # -x[1] => sort by frequency descending
        # x[0]  => sort by character ascending for ties

        # Build the final string by repeating each character by its frequency
        # and joining them all together
        return "".join([ch * freq for ch, freq in char_freq])


# Examples from the problem statement
print(Solution().sort_characters_by_frequency("eeeaaabc"))    # aaaeeebc
print(Solution().sort_characters_by_frequency("zzzxxyyyb"))   # yyyzzzxxb
print(Solution().sort_characters_by_frequency("zzzxxyyb"))    # zzzxxyyb

# Edge cases
print(Solution().sort_characters_by_frequency("a"))           # a
print(Solution().sort_characters_by_frequency("aa"))          # aa
print(Solution().sort_characters_by_frequency("ab"))          # ab
print(Solution().sort_characters_by_frequency("aabb"))        # aabb
print(Solution().sort_characters_by_frequency("ccbbaa"))      # aabbcc
```

```java run viz=hashmap viz-root=frequency
import java.util.*;

public class Main {
    static class Solution {
        public String sortCharactersByFrequency(String s) {

            // Create a map to store the frequency of characters
            Map<Character, Integer> frequency = new HashMap<>();

            // Count the frequencies of each character in the input string
            for (char ch : s.toCharArray()) {
                frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
            }

            // Convert the frequency map into a list of Map.Entry<Character,
            // Integer>
            List<Map.Entry<Character, Integer>> charFreq = new ArrayList<>(
                frequency.entrySet()
            );

            // Sort the list using a lambda comparator
            charFreq.sort((a, b) -> {

                // Compare the frequencies of characters 'a' and 'b'
                // If the frequency of 'a' is greater than the frequency of
                // 'b', or if the frequencies are equal but 'a' comes before
                // 'b' in lexicographical order, then 'a' should come before
                // 'b' in the sorted string.
                if (a.getValue().equals(b.getValue())) {

                    // lexicographical order
                    return a.getKey() - b.getKey();
                }

                // Otherwise, sort by frequency in descending order
                return b.getValue() - a.getValue();
            });

            // Build the sorted string by repeating each character by its
            // frequency and appending them all together
            StringBuilder result = new StringBuilder();
            for (Map.Entry<Character, Integer> entry : charFreq) {
                for (int i = 0; i < entry.getValue(); i++) {
                    result.append(entry.getKey());
                }
            }

            return result.toString();
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().sortCharactersByFrequency("eeeaaabc"));   // aaaeeebc
        System.out.println(new Solution().sortCharactersByFrequency("zzzxxyyyb"));  // yyyzzzxxb
        System.out.println(new Solution().sortCharactersByFrequency("zzzxxyyb"));   // zzzxxyyb

        // Edge cases
        System.out.println(new Solution().sortCharactersByFrequency("a"));          // a
        System.out.println(new Solution().sortCharactersByFrequency("aa"));         // aa
        System.out.println(new Solution().sortCharactersByFrequency("ab"));         // ab
        System.out.println(new Solution().sortCharactersByFrequency("aabb"));       // aabb
        System.out.println(new Solution().sortCharactersByFrequency("ccbbaa"));     // aabbcc
    }
}
```

The Python and Java implementations follow the same pattern: build a frequency map, sort the keys (or pairs) by `(-freq, char)`, then expand. The exact comparator syntax differs per language (lambda, comparator class, etc.) but the transform `(-freq, char)` is universal.

</details>
