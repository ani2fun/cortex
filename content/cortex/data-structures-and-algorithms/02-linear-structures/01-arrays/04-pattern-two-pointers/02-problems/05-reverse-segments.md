---
title: "Reverse Segments"
summary: "Given a string s and an integer k, process the string in groups of 2k characters and reverse the first k characters of every group. Return the updated string."
prereqs:
  - 04-pattern-two-pointers/01-pattern
difficulty: medium
kind: problem
topics: [two-pointers, arrays]
---

# Reverse Segments

## The Problem

Given a string `s` and an integer `k`, process the string in groups of `2k` characters and reverse the **first `k` characters of every group**. Return the updated string.

Two rules cover the tail of the string when a full `2k` group doesn't fit:

- If fewer than `k` characters remain, reverse all of them.
- If at least `k` but fewer than `2k` characters remain, reverse only the first `k` and leave the rest unchanged.

```
Input:  s = "abcdefghij",  k = 2
Output: "bacdfeghji"

  groups of 2k = 4:    abcd      efgh      ij
  reverse first k = 2: [ba]cd    [fe]gh    [ji]     (last group: only k chars, reverse all)
  result:              bacdfeghji
```

---

## Examples

**Example 1 — full groups plus a trailing chunk of exactly `k`**
```
Input:  s = "abcdefghij",  k = 2
Output: "bacdfeghji"
```
- The first 2k characters are `abcd`; reverse the first k: `ab` → `ba`.
- The next 2k characters are `efgh`; reverse the first k: `ef` → `fe`.
- `ij` is left — `k` characters, fewer than `2k` — so reverse the first k: `ij` → `ji`.

**Example 2 — fewer than `k` characters remain**
```
Input:  s = "dfgh",  k = 5
Output: "hgfd"
```
- There are fewer than `k` characters left, so reverse all of them.

**Example 3 — exactly `2k` characters, second half untouched**
```
Input:  s = "qwerty",  k = 3
Output: "ewqrty"
```
- The first k characters are reversed: `qwe` → `ewq`.
- The remaining characters (`rty`) are left unchanged.

**Example 4 — trailing chunk between `k` and `2k`**
```
Input:  s = "abcdefg",  k = 2
Output: "bacdfeg"
```
- Groups are `abcd` and `efg`. In `abcd`, reverse `ab` → `ba`. `efg` has 3 characters (≥ `k`, < `2k`), so reverse the first `k` — `ef` → `fe` — and leave `g`.

```quiz
{
  "prompt": "Now your turn!",
  "input": "s = \"codewars\", k = 2",
  "options": ["ocdeawrs", "codewars", "srawedoc", "ocdewars"],
  "answer": "ocdeawrs"
}
```

## Constraints

- `0 ≤ s.length ≤ 1000`
- `1 ≤ k ≤ 1000`
- `s` consists of printable ASCII characters

```python run viz=array viz-root=arr
class Solution:
    def reverse_segments(self, s: str, k: int) -> str:
        # Your code goes here — stride start by 2k; for each block reverse
        # arr[start .. min(start+k-1, n-1)] in place with two pointers.
        return s

s = input()                          # the test case's s
k = int(input())                     # the test case's k
print(Solution().reverse_segments(s, k))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public String reverseSegments(String s, int k) {
            // Your code goes here — stride start by 2k; for each block reverse
            // arr[start .. min(start+k-1, n-1)] in place with two pointers.
            return s;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().reverseSegments(s, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "abcdefghij" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "s": "abcdefghij", "k": "2" }, "expected": "bacdfeghji" },
    { "args": { "s": "codewars", "k": "2" }, "expected": "ocdeawrs" },
    { "args": { "s": "dfgh", "k": "5" }, "expected": "hgfd" },
    { "args": { "s": "qwerty", "k": "3" }, "expected": "ewqrty" },
    { "args": { "s": "abcdef", "k": "6" }, "expected": "fedcba" },
    { "args": { "s": "abcdef", "k": "1" }, "expected": "abcdef" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


The problem has a regular structure: the string splits into consecutive blocks of `2k` characters, and inside each block only the first `k` characters get reversed. The "reverse the first `k`" step is the Flip Characters reversal applied to a sub-range — `left = block_start`, `right = block_start + k − 1`. The wrapper around it is an outer loop that picks block start positions.

Place `start = 0` and step it by `2k` until `start >= n`. At each landing point, set `left = start` and `right = min(start + k − 1, n − 1)` and run the inner reversal. Two arithmetic tricks fold both tail rules into the same code path. Striding by `2k` (not `k`) means the second half of every block — indices `start + k .. start + 2k − 1` — is never an endpoint of any swap, so "leave the rest unchanged" needs no branch. Clamping `right` with `min(..., n − 1)` means that when fewer than `k` characters remain, the inner reversal runs from `start` to the actual last index — so "if fewer than `k` characters remain, reverse all of them" is also branchless.

A direct port of the rules would split into three cases — full `2k` block, trailing `k..2k` chunk, trailing `<k` chunk — each with its own reversal call. The stride-plus-clamp version collapses all three into one loop. The cost stays `O(n)` time and `O(n)` extra space for the mutable character array; the win is structural, not asymptotic — fewer code paths means fewer places for off-by-one bugs.

```d2
direction: down

ORIG: "Original — s = abcdefghij, k = 2  (block size 2k = 4)" {
  grid-columns: 10
  grid-gap: 0
  a: "a"
  b: "b"
  c: "c"
  d: "d"
  e: "e"
  f: "f"
  g: "g"
  h: "h"
  i: "i"
  j: "j"
}
ORIG.a.style.fill: "#fde68a"
ORIG.b.style.fill: "#fde68a"
ORIG.e.style.fill: "#fde68a"
ORIG.f.style.fill: "#fde68a"
ORIG.i.style.fill: "#fde68a"
ORIG.j.style.fill: "#fde68a"

RESULT: "Result — first k of every 2k block reversed" {
  grid-columns: 10
  grid-gap: 0
  a: "b"
  b: "a"
  c: "c"
  d: "d"
  e: "f"
  f: "e"
  g: "g"
  h: "h"
  i: "j"
  j: "i"
}
RESULT.a.style.fill: "#dcfce7"
RESULT.b.style.fill: "#dcfce7"
RESULT.e.style.fill: "#dcfce7"
RESULT.f.style.fill: "#dcfce7"
RESULT.i.style.fill: "#dcfce7"
RESULT.j.style.fill: "#dcfce7"

ORIG -> RESULT: "reverse first k=2 of every 2k=4 block (last block has only k chars left)"
```

<p align="center"><strong>Reversing the first <code>k</code> of every <code>2k</code> block in <code>abcdefghij</code> — the highlighted cells are the windows that get reversed; the gaps (<code>cd</code>, <code>gh</code>) are stepped over entirely.</strong></p>

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse Segments |
|---|---|
| ✅ Two positions simultaneously? | Yes — within each `k`-window, `arr[left]` and `arr[right]` are swapped together |
| ✅ One near start, one near end? | Yes — for each window, `left = start` and `right = min(start + k - 1, n - 1)` |
| ✅ Both move inward? | Yes — `left++`, `right--` within each window's reversal |
| ✅ Simple work at each step? | Yes — one swap per iteration |

Reverse Segments is structurally identical to Flip Characters — the only difference is that `left` and `right` start from a computed window `(start, start + k - 1)` instead of `(0, n-1)`. The two-pointer pattern is unchanged; the `2k` stride and the `min` clamp just decide *which* sub-ranges to feed it.

**Why is this still "direct application" and not something more complex?** The reversal inside each window is the unmodified swap-and-converge loop. The only thing wrapped around it is a `for` loop that picks window start positions by counting in `2k` strides. There's no data transformation, no searching for a range, no condition-based pointer movement inside the window. Two pointers enter a window, march toward each other, and exit.

**Why doesn't the "leave the rest unchanged" rule need any code?** Because the outer loop strides by `2k` and each reversal only touches `start .. start + k - 1`. The second half of every block — indices `start + k .. start + 2k - 1` — is never an endpoint and never swapped. The rule is satisfied by *omission*: those indices are simply skipped, so they keep their original values for free.

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Convert `s` to a mutable character array `arr` (strings are immutable in most languages).
2. For each block start `start` = `0, 2k, 4k, …` while `start < n`:
   - Set `left = start` and `right = min(start + k - 1, n - 1)` — the `min` clamps the short tail.
   - While `left < right`: swap `arr[left]` and `arr[right]`, `left++`, `right--`.
3. Join `arr` back into a string and return it.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n) space=O(n)
from typing import List

class Solution:
    def reverse_segment(
        self, arr: List[str], left: int, right: int
    ) -> None:

        # Use a while loop to traverse the string using the two pointers
        while left < right:

            # Swap the characters pointed by the left and right pointers
            arr[left], arr[right] = arr[right], arr[left]

            # Move the pointers towards the center of the string
            left += 1
            right -= 1

    def reverse_segments(self, s: str, k: int) -> str:

        # convert the string to list for in-place modification
        arr = list(s)
        n = len(arr)

        for start in range(0, n, 2 * k):

            # Initialize left and right pointers to the current segment
            left = start
            right = min(start + k - 1, n - 1)

            # Reverse the segment using the two-pointer method
            self.reverse_segment(arr, left, right)

        # convert the list back to string
        return "".join(arr)


s = input()                          # the test case's s
k = int(input())                     # the test case's k
print(Solution().reverse_segments(s, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private void reverseSegment(char[] arr, int left, int right) {

            // Use a while loop to traverse the string using the two pointers
            while (left < right) {

                // Swap the characters pointed by the left and right pointers
                char temp = arr[left];
                arr[left] = arr[right];
                arr[right] = temp;

                // Move the pointers towards the center of the string
                left++;
                right--;
            }
        }

        public String reverseSegments(String s, int k) {
            char[] arr = s.toCharArray();
            int n = arr.length;
            for (int start = 0; start < n; start += 2 * k) {

                // Initialize left and right pointers to the current segment
                int left = start;
                int right = Math.min(start + k - 1, n - 1);

                // Reverse the segment using the two-pointer method
                reverseSegment(arr, left, right);
            }
            return new String(arr);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().reverseSegments(s, k));
    }
}
```

### Dry Run — Example 1

`s = "abcdefghij"`, `k = 2` → `n = 10`, stride `2k = 4`. Block starts: `0, 4, 8`.

**Block start = 0:** `left = 0`, `right = min(0 + 1, 9) = 1`

| Step | `left` | `right` | Swap | Array |
|---|---|---|---|---|
| 1 | 0 | 1 | `a ↔ b` | `bacdefghij` |
| — | 1 | 0 | stop | — |

**Block start = 4:** `left = 4`, `right = min(4 + 1, 9) = 5`

| Step | `left` | `right` | Swap | Array |
|---|---|---|---|---|
| 1 | 4 | 5 | `e ↔ f` | `bacdfeghij` |
| — | 5 | 4 | stop | — |

**Block start = 8:** `left = 8`, `right = min(8 + 1, 9) = 9` — only `k` characters remain; the clamp is a no-op here

| Step | `left` | `right` | Swap | Array |
|---|---|---|---|---|
| 1 | 8 | 9 | `i ↔ j` | `bacdfeghji` |
| — | 9 | 8 | stop | — |

**Result: `"bacdfeghji"`** ✓

### Complexity Analysis

Let `n` = the length of the string.

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(n) | Every index is an endpoint of at most one swap; the skipped second-halves aren't visited at all. Building the character array and joining it back are also O(n). |
| **Space** | O(n) | The mutable character array is a copy of the string — unavoidable because strings are immutable (in Python, Java, Scala). The two-pointer reversal itself adds only O(1) on top of that copy. |

### Edge Cases

| Scenario | Input | Effect |
|---|---|---|
| `k = 1` | any `s` | Every window is a single character — `left = right`, the loop never runs; the string returns unchanged |
| `k ≥ n` | `s = "dfgh", k = 5` | One block; `right` clamps to `n - 1`; the whole string is reversed |
| Last block `< k` chars | trailing chunk | `min` clamps `right`; the short tail is fully reversed |
| Last block `k`–`2k` chars | trailing chunk | First `k` reversed; the rest is never visited, left as-is |
| Empty string | `s = ""` | `n = 0`; the outer loop never runs; returns `""` |
| `k = 0` | — | Out of scope — the problem constraints assume `k ≥ 1` (a `2k = 0` stride would not advance) |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reverse Segments shows the two-pointer reversal as a **reusable utility** — aimed at any sub-range, not only the whole array. The new idea is letting *index arithmetic* carry the irregular requirements: a `2k` stride skips untouched halves, and a `min` clamp folds two tail rules into one expression. Reverse Word Order composes sub-range reversals the same way.

</details>
