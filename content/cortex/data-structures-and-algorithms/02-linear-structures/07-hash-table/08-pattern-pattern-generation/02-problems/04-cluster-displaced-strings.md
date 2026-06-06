---
title: "Cluster Displaced Strings"
summary: "Given an array strs of lowercase strings, group strings that belong to the same displacing sequence. A displacing sequence shifts every character by the same amount, with wrap-around (e.g. abc → bcd →"
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
---

# Cluster displaced strings

## Problem Statement

Given an array `strs` of lowercase strings, group strings that belong to the same **displacing sequence**. A displacing sequence shifts every character by the same amount, with wrap-around (e.g. `abc → bcd → ... → xyz → yza`).

### Example 1
> -   **Input:** `["abc","ghi","xyz","b","c","ab","cd"]`
> -   **Output:** `[["abc","ghi","xyz"], ["b","c"], ["ab","cd"]]`

### Example 2
> -   **Input:** `["ad","k","cf"]`
> -   **Output:** `[["ad","cf"], ["k"]]`

### Example 3
> -   **Input:** `["abcd","efg","hi","j"]`
> -   **Output:** `[["abcd"],["efg"],["hi"],["j"]]`

## Examples

**Example 1**
```
Input:  ["abc", "ghi", "xyz", "b", "c", "ab", "cd"]
Output: [["abc", "ghi", "xyz"], ["b", "c"], ["ab", "cd"]]
Explanation: abc, ghi, xyz all have gap sequence "1,1,". b and c are single chars (empty
gaps). ab and cd both have gap "1,". Three displacement classes.
```

**Example 2**
```
Input:  ["ad", "k", "cf"]
Output: [["ad", "cf"], ["k"]]
Explanation: ad → gap (d−a)=3 → "3,"; cf → gap (f−c)=3 → "3,". k is a single char → "".
```

**Example 3**
```
Input:  ["abcd", "efg", "hi", "j"]
Output: [["abcd"], ["efg"], ["hi"], ["j"]]
Explanation: lengths differ, so the gap-sequence lengths differ — every string is alone.
```

**Example 4**
```
Input:  ["az", "ba"]
Output: [["az", "ba"]]
Explanation: az → (z−a)=25 → "25,". ba → (a−b)=−1, wrapped +26 → "25,". Same class.
```


<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that "belongs to the same displacing sequence" is a property of each string's *internal shape*, not of any pair. Shifting every character by the same amount leaves the gaps between consecutive characters unchanged, so two strings displace into each other exactly when their gap sequences match. That gap sequence is the key.

The key per string is the sequence of **consecutive-character gaps**, taken modulo 26 so wrap-around survives the encoding. `abc` has gaps `(b−a, c−b) = (1, 1)` → `"1,1,"`. `xyz` has gaps `(y−x, z−y) = (1, 1)` → `"1,1,"`, and `yza` keeps `"1,1,"` because `a − z = −25` wraps to `+1`. A single-character string has no gaps at all, so its key is the empty string, and all single-character strings cluster together.

The naive approach compares every pair of strings to test displacement, which re-derives the same gap facts `O(M²)` times over `M` strings. Keying each string once and bucketing by key collapses the grouping to `O(1)` per string after the scan. The mod-26 step is the one subtlety — without it, a wrapped gap like `a − z` would be negative and the encoding would split strings that actually displace into each other.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Cluster Displaced Strings |
|---|---|
| **Q1.** Does the answer depend on a *canonical form* of each input? | **Yes** — each string's mod-26 gap sequence is its key; same key means same displacement class. |
| **Q2.** Can you define equivalence as a function from input to bytes? | **Yes** — `generate_pattern` maps each string to a gap-sequence byte string. |
| **Q3.** Is each input keyed independently in a single pass? | **Yes** — each string is scanned once on its own, then dropped into a bucket. |
| **Q4.** Is the per-item work `O(1)`? | **Yes** — each character pair is one subtraction, one wrap check, and one append, all `O(1)`. |

</details>
<details>
<summary><h2>Approach</h2></summary>


Two strings are in the same displacing class iff the **gaps between consecutive characters** are identical (modulo 26). `abc` has gaps `(b−a, c−b) = (1, 1)`. `ghi` has gaps `(h−g, i−h) = (1, 1)`. `xyz` has gaps `(y−x, z−y) = (1, 1)`. All three: `(1, 1)`. They cluster.

The key per string is the **gap-sequence**, with negative gaps wrapped to `+ 26` so `xyz → yza` doesn't break the encoding (`a − z = -25` becomes `+1` after wrap).

Single-character strings have an empty gap sequence `""`, so they all share one key and cluster together — in Example 1, `b` and `c` form the single bucket `[b, c]`. Two-character strings cluster by their one gap, three-character strings by their two-gap sequence, and so on. So the key idea is: strings cluster by *gap-sequence length and contents*, which is why same-length displaced strings group while different-length strings never can.

```d2
direction: right

inputs: input strings {
  grid-rows: 2
  grid-gap: 8
  s1: abc
  s2: ghi
  s3: xyz
  s4: ab
  s5: cd
  s6: b
  s7: c
}

keys: gap-sequence keys {
  k1: "1,1,"
  k2: "1,"
  k3: "(empty)"
}

inputs.s1 -> keys.k1: "gaps (1,1)"
inputs.s2 -> keys.k1: "gaps (1,1)"
inputs.s3 -> keys.k1: "gaps (1,1)"
inputs.s4 -> keys.k2: "gap (1,)"
inputs.s5 -> keys.k2: "gap (1,)"
inputs.s6 -> keys.k3: "no gaps"
inputs.s7 -> keys.k3: "no gaps"
```

<p align="center"><strong>Cluster displaced strings — the key is the sequence of consecutive-character gaps (modulo 26 to handle wrap). Strings with identical gap sequences belong to the same displacing class and collide into the same hash-map bucket.</strong></p>

</details>
<details>
<summary><h2>Approach in Words</h2></summary>


Key each string by its gap sequence, then bucket by key.

1. **Define the gap-sequence key.** For a string, walk adjacent character pairs and record each difference; if a difference is negative, add `26` to wrap it.
2. **Key every string.** Run that scan over each input string to get its gap-sequence pattern.
3. **Bucket by key.** Use a map from key to list; append each string to the list under its key, creating the list on first sight.
4. **Collect the buckets.** Gather the map's value lists into the result; each list is one displacement class.
5. **Return the result.** It holds the groups in any order, as the problem permits.

</details>
<details>
<summary><h2>Solution</h2></summary>



```python run viz=graph viz-root=strs
from typing import List

class Solution:
    def generate_pattern(self, s: str) -> str:
        pattern = ""

        for i in range(1, len(s)):

            # Find the difference between consecutive characters
            difference = ord(s[i]) - ord(s[i - 1])
            if difference < 0:

                # Handle wrap-around case (e.g., from 'z' to 'a')
                difference += 26

            # Add the displacement to the pattern
            pattern += str(difference) + ","

        return pattern

    def cluster_displaced_strings(
        self, strs: List[str]
    ) -> List[List[str]]:
        clusters = {}

        # Process each string and group them by their displacement
        # pattern
        for str in strs:

            # Generate the pattern for each string
            pattern = self.generate_pattern(str)

            # Group the strings with the same pattern
            if pattern not in clusters:
                clusters[pattern] = []
            clusters[pattern].append(str)

        # Prepare the result with all grouped strings
        result = []
        for group in clusters.values():

            # Add each group to the result
            result.append(group)

        return result


# Examples from the problem statement
r1 = Solution().cluster_displaced_strings(["abc", "ghi", "xyz", "b", "c", "ab", "cd"])
print(sorted([sorted(g) for g in r1]))  # [['abc', 'ghi', 'xyz'], ['ab', 'cd'], ['b', 'c']]

r2 = Solution().cluster_displaced_strings(["ad", "k", "cf"])
print(sorted([sorted(g) for g in r2]))  # [['ad', 'cf'], ['k']]

r3 = Solution().cluster_displaced_strings(["abcd", "efg", "hi", "j"])
print(sorted([sorted(g) for g in r3]))  # [['abcd'], ['efg'], ['hi'], ['j']]

# Edge cases
print(Solution().cluster_displaced_strings([]))              # []
print(Solution().cluster_displaced_strings(["a"]))           # [['a']]
r6 = Solution().cluster_displaced_strings(["az", "ba"])
print(sorted([sorted(g) for g in r6]))  # [['az', 'ba']]
```

```java run viz=graph viz-root=strs
import java.util.*;

public class Main {
    static class Solution {
        private String generatePattern(String s) {
            StringBuilder pattern = new StringBuilder();

            for (int i = 1; i < s.length(); i++) {

                // Find the difference between consecutive characters
                int difference = s.charAt(i) - s.charAt(i - 1);
                if (difference < 0) {

                    // Handle wrap-around case (e.g., from 'z' to 'a')
                    difference += 26;
                }

                // Add the displacement to the pattern
                pattern.append(difference).append(",");
            }

            return pattern.toString();
        }

        public List<List<String>> clusterDisplacedStrings(String[] strs) {
            Map<String, List<String>> clusters = new HashMap<>();

            // Process each string and group them by their displacement
            // pattern
            for (String str : strs) {

                // Generate the pattern for each string
                String pattern = generatePattern(str);

                // Group the strings with the same pattern
                clusters.putIfAbsent(pattern, new ArrayList<>());
                clusters.get(pattern).add(str);
            }

            // Prepare the result with all grouped strings
            List<List<String>> result = new ArrayList<>();
            for (List<String> group : clusters.values()) {

                // Add each group to the result
                result.add(group);
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        var r1 = new Solution().clusterDisplacedStrings(new String[]{"abc", "ghi", "xyz", "b", "c", "ab", "cd"});
        r1.forEach(g -> { Collections.sort(g); System.out.print(g + " "); }); System.out.println();
        // [abc, ghi, xyz] [b, c] [ab, cd] (group order may vary)

        var r2 = new Solution().clusterDisplacedStrings(new String[]{"ad", "k", "cf"});
        r2.forEach(g -> { Collections.sort(g); System.out.print(g + " "); }); System.out.println();
        // [ad, cf] [k] (group order may vary)

        var r3 = new Solution().clusterDisplacedStrings(new String[]{"abcd", "efg", "hi", "j"});
        r3.forEach(g -> System.out.print(g + " ")); System.out.println();
        // [abcd] [efg] [hi] [j]

        // Edge cases
        System.out.println(new Solution().clusterDisplacedStrings(new String[]{}));     // []
        System.out.println(new Solution().clusterDisplacedStrings(new String[]{"a"}));  // [[a]]
        var r6 = new Solution().clusterDisplacedStrings(new String[]{"az", "ba"});
        r6.forEach(g -> { Collections.sort(g); System.out.print(g + " "); }); System.out.println();
        // [az, ba]
    }
}
```

</details>
<details>
<summary><h2>Dry Run</h2></summary>


Walk Example 1 — `["abc", "ghi", "xyz", "b", "c", "ab", "cd"]`. Each string is keyed by its mod-26 gap sequence, then bucketed:

```
"abc"  gaps (b−a, c−b) = (1, 1)            key "1,1,"     clusters={"1,1,":[abc]}
"ghi"  gaps (h−g, i−h) = (1, 1)            key "1,1,"     append → [abc, ghi]
"xyz"  gaps (y−x, z−y) = (1, 1)            key "1,1,"     append → [abc, ghi, xyz]
"b"    no gaps                             key ""         clusters[""]=[b]
"c"    no gaps                             key ""         append → [b, c]
"ab"   gap (b−a) = 1                       key "1,"       clusters["1,"]=[ab]
"cd"   gap (d−c) = 1                       key "1,"       append → [ab, cd]

result = [["abc", "ghi", "xyz"], ["b", "c"], ["ab", "cd"]]
```

The three buckets match the expected output (group order may vary).

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(S)** | `S` is the total length of all strings; each character pair is one `O(1)` gap computation. |
| Space | **O(S)** | The clusters map stores every input string once, plus one key string per input. |

Keying a string is linear in its length, and bucketing is one `O(1)` map operation per string, so the whole grouping is one pass over all characters.

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty list | `[]` | `[]` | No strings to key, so no buckets. |
| Single string | `["a"]` | `[["a"]]` | One string forms its own (only) cluster. |
| All single chars | `["b", "c"]` | `[["b", "c"]]` | Every single character keys to `""`, so all land in one bucket. |
| Wrap-around | `["az", "ba"]` | `[["az", "ba"]]` | `az` → `25,`; `ba` → `(a−b)=−1` wrapped to `25,`. Same class. |
| Different lengths | `["abcd", "efg", "hi", "j"]` | `[["abcd"], ["efg"], ["hi"], ["j"]]` | Gap-sequence lengths differ, so no two strings can share a key. |
| Same gaps, different start | `["abc", "ghi"]` | `[["abc", "ghi"]]` | Both have gaps `(1, 1)`; the starting letter is irrelevant to the key. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The new idea here is a **relational key** — the mod-26 sequence of gaps between consecutive characters — which is invariant under shifting every character by a constant, so displaced strings collapse to one bucket.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The key-generation pattern is the rosetta stone of hash-table problem solving. *Anywhere you can define what makes two inputs "the same"*, you can encode that sameness as a key, throw the keys at a hash map, and let the structure do the grouping for you.

The skill is **inventing the key**. A few common shapes:

- **Sorted form** — for anagrams (`"cab" → "abc"`).
- **First-occurrence index** — for shape/homomorphism (`"add" → "0,1,1"`).
- **Gap sequence (mod cyclic group)** — for displaced/shifted strings.
- **Frequency tuple** — for multiset equality.
- **Categorical id** — for keyboard rows, parity classes, modular buckets.

Once the key is right, the rest is one line:

```python
groups[key(x)].append(x)
```

> *Coming up — the **fixed-size sliding window** pattern. So far we've used hash maps to summarise *static* sequences. Next we'll learn to slide a fixed-size window across a long sequence while the hash map tracks the multiset *inside* the window in O(1) per shift. Fixed-window anagrams, frequency-bounded substrings, repeating-character runs — the same hash map you've been building for two lessons becomes the engine of a moving picture.*

</details>