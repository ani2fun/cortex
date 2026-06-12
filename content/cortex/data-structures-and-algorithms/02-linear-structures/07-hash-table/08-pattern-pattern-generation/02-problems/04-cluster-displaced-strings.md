---
title: "Cluster Displaced Strings"
summary: "Given an array strs of lowercase strings, group strings that belong to the same displacing sequence. A displacing sequence shifts every character by the same amount, with wrap-around (e.g. abc → bcd →"
prereqs:
  - 08-pattern-pattern-generation/01-pattern
difficulty: medium
kind: problem
topics: [pattern-generation, hash-table]
---

# Cluster displaced strings

## Problem Statement

Given an array `strs` of lowercase strings, group strings that belong to the same **displacing sequence**. A displacing sequence shifts every character by the same amount, with wrap-around (e.g. `abc → bcd → ... → xyz → yza`).

### Example 1
> -   **Input:** `["abc","ghi","xyz","b","c","ab","cd"]`
> -   **Output:** `[[ab, cd], [abc, ghi, xyz], [b, c]]`

### Example 2
> -   **Input:** `["ad","k","cf"]`
> -   **Output:** `[[ad, cf], [k]]`

### Example 3
> -   **Input:** `["abcd","efg","hi","j"]`
> -   **Output:** `[[abcd], [efg], [hi], [j]]`

## Examples

**Example 1**
```
Input:  ["abc", "ghi", "xyz", "b", "c", "ab", "cd"]
Output: [[ab, cd], [abc, ghi, xyz], [b, c]]
Explanation: abc, ghi, xyz all have gap sequence "1,1,". b and c are single chars (empty
gaps). ab and cd both have gap "1,". Three displacement classes.
```

**Example 2**
```
Input:  ["ad", "k", "cf"]
Output: [[ad, cf], [k]]
Explanation: ad → gap (d−a)=3 → "3,"; cf → gap (f−c)=3 → "3,". k is a single char → "".
```

**Example 3**
```
Input:  ["abcd", "efg", "hi", "j"]
Output: [[abcd], [efg], [hi], [j]]
Explanation: lengths differ, so the gap-sequence lengths differ — every string is alone.
```

**Example 4**
```
Input:  ["az", "ba"]
Output: [[az, ba]]
Explanation: az → (z−a)=25 → "25,". ba → (a−b)=−1, wrapped +26 → "25,". Same class.
```

## Constraints

- `1 ≤ strs.length ≤ 200`
- `1 ≤ strs[i].length ≤ 50`
- `strs[i]` consists of only lowercase English letters.

```python run
import ast
from typing import List

class Solution:
    def cluster_displaced_strings(self, strs: List[str]) -> List[List[str]]:
        # Your code goes here
        pass

strs = ast.literal_eval(input())
groups = Solution().cluster_displaced_strings(strs)
result = sorted([sorted(g) for g in groups])
inner = [", ".join(g) for g in result]
print("[" + ", ".join("[" + s + "]" for s in inner) + "]")
```

```java run
import java.util.*;

public class Main {
    static class Solution {
        public List<List<String>> clusterDisplacedStrings(String[] strs) {
            // Your code goes here
            return new ArrayList<>();
        }
    }

    static String[] parseStringArray(String line) {
        line = line.trim();
        if (line.equals("[]")) return new String[0];
        line = line.substring(1, line.length() - 1);
        String[] parts = line.split(",\\s*");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if ((parts[i].startsWith("\"") && parts[i].endsWith("\"")) ||
                (parts[i].startsWith("'") && parts[i].endsWith("'")))
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
        }
        return parts;
    }

    static String formatGroups(List<List<String>> groups) {
        List<List<String>> sorted = new ArrayList<>();
        for (List<String> g : groups) {
            List<String> sg = new ArrayList<>(g);
            Collections.sort(sg);
            sorted.add(sg);
        }
        sorted.sort(Comparator.comparing(g -> g.get(0)));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            sb.append("[").append(String.join(", ", sorted.get(i))).append("]");
            if (i < sorted.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] strs = parseStringArray(sc.nextLine());
        System.out.println(formatGroups(new Solution().clusterDisplacedStrings(strs)));
    }
}
```

```testcases
{
  "args": [
    { "id": "strs", "label": "strs", "type": "string", "placeholder": "[\"abc\",\"ghi\",\"xyz\",\"b\",\"c\",\"ab\",\"cd\"]" }
  ],
  "cases": [
    { "args": { "strs": "[\"abc\",\"ghi\",\"xyz\",\"b\",\"c\",\"ab\",\"cd\"]" }, "expected": "[[ab, cd], [abc, ghi, xyz], [b, c]]" },
    { "args": { "strs": "[\"ad\",\"k\",\"cf\"]" }, "expected": "[[ad, cf], [k]]" },
    { "args": { "strs": "[\"abcd\",\"efg\",\"hi\",\"j\"]" }, "expected": "[[abcd], [efg], [hi], [j]]" },
    { "args": { "strs": "[\"az\",\"ba\"]" }, "expected": "[[az, ba]]" },
    { "args": { "strs": "[\"a\"]" }, "expected": "[[a]]" }
  ]
}
```

<details>
<summary>Editorial</summary>

The key per string is the sequence of **mod-26 gaps** between consecutive characters: `abc → "1,1,"`, `ghi → "1,1,"`, so they cluster. A single-character string has no gaps → key `""` → all single-character strings cluster together. Bucket by key in a hash map, collect the values. Canonicalize both levels for determinism: sort within each group, then sort groups by their first element. `O(S)` time and space where `S` is total character length.

```python solution time=O(S) space=O(S)
import ast
from typing import List

class Solution:
    def generate_pattern(self, s: str) -> str:
        pattern = ""
        for i in range(1, len(s)):
            difference = ord(s[i]) - ord(s[i - 1])
            if difference < 0: difference += 26
            pattern += str(difference) + ","
        return pattern

    def cluster_displaced_strings(self, strs: List[str]) -> List[List[str]]:
        clusters = {}
        for s in strs:
            pattern = self.generate_pattern(s)
            if pattern not in clusters: clusters[pattern] = []
            clusters[pattern].append(s)
        return list(clusters.values())

strs = ast.literal_eval(input())
groups = Solution().cluster_displaced_strings(strs)
result = sorted([sorted(g) for g in groups])
inner = [", ".join(g) for g in result]
print("[" + ", ".join("[" + s + "]" for s in inner) + "]")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private String generatePattern(String s) {
            StringBuilder pattern = new StringBuilder();
            for (int i = 1; i < s.length(); i++) {
                int difference = s.charAt(i) - s.charAt(i - 1);
                if (difference < 0) difference += 26;
                pattern.append(difference).append(",");
            }
            return pattern.toString();
        }

        public List<List<String>> clusterDisplacedStrings(String[] strs) {
            Map<String, List<String>> clusters = new HashMap<>();
            for (String str : strs) {
                String pattern = generatePattern(str);
                clusters.putIfAbsent(pattern, new ArrayList<>());
                clusters.get(pattern).add(str);
            }
            List<List<String>> result = new ArrayList<>();
            for (List<String> group : clusters.values()) result.add(group);
            return result;
        }
    }

    static String[] parseStringArray(String line) {
        line = line.trim();
        if (line.equals("[]")) return new String[0];
        line = line.substring(1, line.length() - 1);
        String[] parts = line.split(",\\s*");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if ((parts[i].startsWith("\"") && parts[i].endsWith("\"")) ||
                (parts[i].startsWith("'") && parts[i].endsWith("'")))
                parts[i] = parts[i].substring(1, parts[i].length() - 1);
        }
        return parts;
    }

    static String formatGroups(List<List<String>> groups) {
        List<List<String>> sorted = new ArrayList<>();
        for (List<String> g : groups) {
            List<String> sg = new ArrayList<>(g);
            Collections.sort(sg);
            sorted.add(sg);
        }
        sorted.sort(Comparator.comparing(g -> g.get(0)));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            sb.append("[").append(String.join(", ", sorted.get(i))).append("]");
            if (i < sorted.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] strs = parseStringArray(sc.nextLine());
        System.out.println(formatGroups(new Solution().clusterDisplacedStrings(strs)));
    }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>


The structural property that makes this a **key-generation** problem is that "belongs to the same displacing sequence" is a property of each string's *internal shape*, not of any pair. Shifting every character by the same amount leaves the gaps between consecutive characters unchanged, so two strings displace into each other exactly when their gap sequences match. That gap sequence is the key.

The key per string is the sequence of **consecutive-character gaps**, taken modulo 26 so wrap-around survives the encoding. `abc` has gaps `(b−a, c−b) = (1, 1)` → `"1,1,"`. `xyz` has gaps `(y−x, z−y) = (1, 1)` → `"1,1,"`, and `yza` keeps `"1,1,"` because `a − z = −25` wraps to `+1`. A single-character string has no gaps at all, so its key is the empty string, and all single-character strings cluster together.

</details>
<details>
<summary><h2>Approach</h2></summary>


Two strings are in the same displacing class iff the **gaps between consecutive characters** are identical (modulo 26). `abc` has gaps `(b−a, c−b) = (1, 1)`. `ghi` has gaps `(h−g, i−h) = (1, 1)`. `xyz` has gaps `(y−x, z−y) = (1, 1)`. All three: `(1, 1)`. They cluster.

The key per string is the **gap-sequence**, with negative gaps wrapped to `+ 26` so `xyz → yza` doesn't break the encoding (`a − z = -25` becomes `+1` after wrap).

Single-character strings have an empty gap sequence `""`, so they all share one key and cluster together — in Example 1, `b` and `c` form the single bucket `[b, c]`. Two-character strings cluster by their one gap, three-character strings by their two-gap sequence, and so on.

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

result = [[ab, cd], [abc, ghi, xyz], [b, c]]  (sorted by group first element)
```

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>


| Measure | Value | Why |
|---|---|---|
| Time  | **O(S)** | `S` is the total length of all strings; each character pair is one `O(1)` gap computation. |
| Space | **O(S)** | The clusters map stores every input string once, plus one key string per input. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>


| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Single string | `["a"]` | `[[a]]` | One string forms its own (only) cluster. |
| All single chars | `["b", "c"]` | `[[b, c]]` | Every single character keys to `""`, so all land in one bucket. |
| Wrap-around | `["az", "ba"]` | `[[az, ba]]` | `az` → `25,`; `ba` → `(a−b)=−1` wrapped to `25,`. Same class. |
| Different lengths | `["abcd", "efg", "hi", "j"]` | `[[abcd], [efg], [hi], [j]]` | Gap-sequence lengths differ, so no two strings can share a key. |
| Same gaps, different start | `["abc", "ghi"]` | `[[abc, ghi]]` | Both have gaps `(1, 1)`; the starting letter is irrelevant to the key. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The new idea here is a **relational key** — the mod-26 sequence of gaps between consecutive characters — which is invariant under shifting every character by a constant, so displaced strings collapse to one bucket. The key-generation pattern is the rosetta stone of hash-table problem solving: *anywhere you can define what makes two inputs "the same"*, encode that sameness as a key and let the hash map group them.

</details>
