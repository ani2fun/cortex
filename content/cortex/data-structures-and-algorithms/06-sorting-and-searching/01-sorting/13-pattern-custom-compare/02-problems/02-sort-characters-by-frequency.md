---
title: "Sort Characters by Frequency"
summary: "Given a string s, return it with characters reordered: highest-frequency first, then lexicographic order on ties."
prereqs:
  - 13-pattern-custom-compare/01-pattern
difficulty: medium
kind: problem
topics: [custom-compare, sorting]
---

# Sort Characters by Frequency

Sort a string's characters by frequency (descending), with lexicographic tiebreaks.

## Problem Statement

Given a string `s`, return it with characters reordered: highest-frequency first, then lexicographic order on ties.

```
Input:  s = "eeeaaabc"
Output: "aaaeeebc"

Input:  s = "zzzxxyyyb"
Output: "yyyzzzxxb"
```

## Examples

**Example 1**
```
Input:  s = "eeeaaabc"
Output: "aaaeeebc"
Explanation: 'a' appears 3 times, 'e' 3 times (tie broken lex: 'a' < 'e'), 'b' once, 'c' once (tie broken lex: 'b' < 'c').
```

**Example 2**
```
Input:  s = "zzzxxyyyb"
Output: "yyyzzzxxb"
Explanation: 'y' appears 3 times, 'z' 3 times (tie broken lex: 'y' < 'z'), 'x' 2 times, 'b' once.
```

## Constraints

- `1 ≤ s.length ≤ 5 × 10^5`
- `s` consists of uppercase and lowercase English letters and digits.

```python run viz=hashmap viz-root=frequency viz-kind=hashmap
class Solution:
    def sort_characters_by_frequency(self, s: str) -> str:
        # Your code goes here — count frequencies, sort by (-freq, char),
        # then rebuild the string by repeating each char freq times.
        return s

s = input()
print(Solution().sort_characters_by_frequency(s))
```

```java run viz=hashmap viz-root=frequency viz-kind=hashmap
import java.util.*;

public class Main {
    static class Solution {
        public String sortCharactersByFrequency(String s) {
            // Your code goes here — count frequencies into a map, build a list
            // of entries, sort by (-freq, char), then append each char freq times.
            return s;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(new Solution().sortCharactersByFrequency(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "eeeaaabc" }
  ],
  "cases": [
    { "args": { "s": "eeeaaabc" }, "expected": "aaaeeebc" },
    { "args": { "s": "zzzxxyyyb" }, "expected": "yyyzzzxxb" },
    { "args": { "s": "zzzxxyyb" }, "expected": "zzzxxyyb" },
    { "args": { "s": "aabb" }, "expected": "aabb" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

Build a frequency map, then sort the unique characters by `(-frequency, char)` — the negative flips to descending frequency, while the character itself breaks ties lexicographically. Because each character's sort key is entirely self-contained, a key function suffices (no pairwise comparator needed). Finally, expand each character back into its repeated form and join. The comparator guarantees a total order — same-frequency characters with different letters always resolve deterministically.

</details>
<details>
<summary><h2>The Custom Compare</h2></summary>

Transform: `t(c) = (-frequency[c], c)`. The negative sign reverses the order on frequency (we want descending). The tuple's second element gives lex tiebreaks ascending.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n log n) space=O(n)
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


s = input()
print(Solution().sort_characters_by_frequency(s))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(new Solution().sortCharactersByFrequency(s));
    }
}
```

The Python and Java implementations follow the same pattern: build a frequency map, sort the keys (or pairs) by `(-freq, char)`, then expand. The exact comparator syntax differs per language (lambda, comparator class, etc.) but the transform `(-freq, char)` is universal.

### Complexity

- Time: `O(n log n)` — building the frequency map is `O(n)`; sorting the distinct characters is `O(k log k)` where `k` is the number of unique characters; rebuilding the string is `O(n)`. Dominates at `O(n log n)`.
- Space: `O(n)` for the frequency map and output string.

</details>
