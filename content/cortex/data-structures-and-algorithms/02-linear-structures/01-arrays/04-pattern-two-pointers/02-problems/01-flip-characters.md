---
title: "Flip Characters"
summary: "Given an array of characters arr, reverse the array by swapping equidistant elements from the start and the end. The reversal must happen in-place — modify the input array directly and use O(1) extra "
prereqs:
  - 04-pattern-two-pointers/01-pattern
difficulty: easy
---

# Flip Characters

## The Problem

Given an array of characters `arr`, reverse the array by **swapping equidistant elements** from the start and the end. The reversal must happen **in-place** — modify the input array directly and use **O(1) extra space**.

```
Input:  arr = [a, e, i, o, u]
Output:       [u, o, i, e, a]
```

This is the canonical direct application of the two-pointer pattern — the template and the algorithm are identical.

---

## Examples

**Example 1**
```
Input:  arr = [a, e, i, o, u]
Output:       [u, o, i, e, a]
```

**Example 2**
```
Input:  arr = [a, b, c, d, e]
Output:       [e, d, c, b, a]
```

**Example 3 — empty array**
```
Input:  arr = []
Output:       []
```

<details>
<summary><h2>Intuition</h2></summary>


To reverse a sequence, the first element must become the last, the second must become the second-to-last, and so on. Every character has a **mirror partner** equidistant from the opposite end. The operation reduces to swapping each pair.

Two pointers map directly onto that mirror structure. Place `left` at index `0` (the first character) and `right` at index `n − 1` (the last character) — they are exactly the first pair that needs to swap. After the swap, both step inward by one position, landing on the next pair. The loop ends when `left >= right`: the pointers have either met at the middle (odd length) or crossed (even length), and every mirror pair has been processed.

A single-pointer traversal breaks here. If you walk forward and overwrite `arr[i]` with `arr[n − 1 − i]`, the original `arr[i]` is gone before you can place it at index `n − 1 − i`. You'd need an `O(n)` temporary buffer to remember evicted values — exactly the brute-force cost the two-pointer template avoids by swapping atomically.

```d3 widget=array-1d
{
  "steps": [
    {
      "nodes": [
        {
          "id": "0",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "u",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "0",
          "color": "#3b82f6"
        },
        {
          "name": "right",
          "target": "4",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Initial — left = 0, right = 4. Swap arr[left] and arr[right] (a ↔ u).",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "u",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "1",
          "color": "#3b82f6"
        },
        {
          "name": "right",
          "target": "3",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Move inward — left = 1, right = 3. Swap arr[left] and arr[right] (e ↔ o).",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "u",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "2",
          "color": "#3b82f6"
        },
        {
          "name": "right",
          "target": "2",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Pointers meet at index 2 — the middle element is its own mirror; no swap needed.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "u",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Done — arr is reversed: [u, o, i, e, a].",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ],
  "title": "Reversing [a, e, i, o, u] in place with two pointers"
}
```

<p align="center"><strong>Flipping <code>[a, e, i, o, u]</code> in place — two swaps reverse the array; the middle element at index 2 is its own mirror.</strong></p>

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Flip Characters |
|---|---|
| ✅ Two positions simultaneously? | Yes — `chars[left]` and `chars[right]` are read and swapped together at every step |
| ✅ One near start, one near end? | Yes — `left = 0`, `right = n-1` |
| ✅ Both move inward? | Yes — `left++`, `right--` after every swap |
| ✅ Simple work at each step? | Yes — one swap per iteration |

Every box is checked with nothing extra needed. This is the purest direct application — the template and the algorithm are identical.

**Why does every element have exactly one partner?** Because reversal is a bijection: element at position `i` maps to position `n-1-i`. Two pointers exploit this directly — `left` tracks "the element at distance 0 from the left" and `right` tracks "the element at distance 0 from the right." Every step, both advance one position inward, so the i-th iteration handles the i-th mirror pair. When `left >= right`, all pairs have been processed.

**What breaks if you use one pointer instead?** A single forward pointer at position `i` can move `chars[i]` to its destination at `n-1-i`, but it has already overwritten whatever was at `n-1-i` — you need a temp variable and a second loop. Two pointers avoid this entirely: the swap is symmetric, so both elements land in their correct positions in one step, no temp array required.

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Set `left = 0`, `right = len(chars) - 1`
2. While `left < right`:
   - Swap `chars[left]` and `chars[right]`
   - `left += 1`, `right -= 1`
3. Done — the array is reversed in-place

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python run viz=array viz-root=arr
from typing import List

class Solution:
    def flip_characters(self, arr: List[str]) -> None:

        # Initialize two pointers, one pointing to the beginning of the
        # array and the other pointing to the end of the array
        left: int = 0
        right = len(arr) - 1

        # Use a while loop to traverse the array using the two pointers
        while left < right:

            # Swap the characters pointed by the left and right pointers
            arr[left], arr[right] = arr[right], arr[left]

            # Move the pointers towards the center of the array
            left  += 1
            right -= 1


# Examples from the problem statement
a1 = ['a', 'e', 'i', 'o', 'u']
Solution().flip_characters(a1); print(a1)         # ['u', 'o', 'i', 'e', 'a']

a2 = ['a', 'b', 'c', 'd', 'e']
Solution().flip_characters(a2); print(a2)         # ['e', 'd', 'c', 'b', 'a']

a3: List[str] = []
Solution().flip_characters(a3); print(a3)         # []

# Edge cases
a4 = ['x']                                        # single element — no swap
Solution().flip_characters(a4); print(a4)         # ['x']

a5 = ['x', 'y']                                   # two elements — single swap
Solution().flip_characters(a5); print(a5)         # ['y', 'x']

a6 = ['a', 'b', 'a']                              # palindrome stays a palindrome
Solution().flip_characters(a6); print(a6)         # ['a', 'b', 'a']

a7 = ['z', 'z', 'z', 'z']                         # all same — output identical
Solution().flip_characters(a7); print(a7)         # ['z', 'z', 'z', 'z']

a8 = list('abcdefghij')                           # longer even-length input
Solution().flip_characters(a8); print(a8)         # ['j', 'i', 'h', 'g', 'f', 'e', 'd', 'c', 'b', 'a']
```

```java run viz=array viz-root=arr
import java.util.Arrays;

public class Main {
    static class Solution {
        void flipCharacters(char[] arr) {

            // Initialize two pointers, one pointing to the beginning of the
            // array and the other pointing to the end of the array
            int left  = 0;
            int right = arr.length - 1;

            // Use a while loop to traverse the array using the two pointers
            while (left < right) {

                // Swap the characters pointed by the left and right pointers
                char tmp     = arr[left];
                arr[left]    = arr[right];
                arr[right]   = tmp;

                // Move the pointers towards the center of the array
                left++;
                right--;
            }
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        char[] a1 = {'a', 'e', 'i', 'o', 'u'};
        new Solution().flipCharacters(a1);
        System.out.println(Arrays.toString(a1));     // [u, o, i, e, a]

        char[] a2 = {'a', 'b', 'c', 'd', 'e'};
        new Solution().flipCharacters(a2);
        System.out.println(Arrays.toString(a2));     // [e, d, c, b, a]

        char[] a3 = {};
        new Solution().flipCharacters(a3);
        System.out.println(Arrays.toString(a3));     // []

        // Edge cases
        char[] a4 = {'x'};                           // single element — no swap
        new Solution().flipCharacters(a4);
        System.out.println(Arrays.toString(a4));     // [x]

        char[] a5 = {'x', 'y'};                      // two elements — single swap
        new Solution().flipCharacters(a5);
        System.out.println(Arrays.toString(a5));     // [y, x]

        char[] a6 = {'a', 'b', 'a'};                 // palindrome stays a palindrome
        new Solution().flipCharacters(a6);
        System.out.println(Arrays.toString(a6));     // [a, b, a]

        char[] a7 = {'z', 'z', 'z', 'z'};            // all same — output identical
        new Solution().flipCharacters(a7);
        System.out.println(Arrays.toString(a7));     // [z, z, z, z]

        char[] a8 = "abcdefghij".toCharArray();      // longer even-length input
        new Solution().flipCharacters(a8);
        System.out.println(Arrays.toString(a8));     // [j, i, h, g, f, e, d, c, b, a]
    }
}
```

### Dry Run — Example 1

`arr = [a, e, i, o, u]`, `n = 5`

| Iteration | `left` | `right` | Swap | Array after swap |
|---|---|---|---|---|
| 1 | 0 | 4 | `a ↔ u` | `[u, e, i, o, a]` |
| 2 | 1 | 3 | `e ↔ o` | `[u, o, i, e, a]` |
| — | 2 | 2 | `left ≥ right` — stop | `[u, o, i, e, a]` ✓ |

The middle element at index 2 (`i`) is its own mirror — no swap needed.

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(n) | Each character is visited once; `left` and `right` together make n/2 swaps |
| **Space** | O(1) | Only two pointer variables — no auxiliary array |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty array | `[]` | `[]` | `left = 0 > right = -1` — loop never runs |
| Single character | `['A']` | `['A']` | `left = right = 0` — loop never runs |
| Two characters | `['A','B']` | `['B','A']` | One swap, then `left = right = 1` — stops |
| Even length | `['A','B','C','D']` | `['D','C','B','A']` | All pairs swapped, no middle element |
| Odd length | `['A','B','C']` | `['C','B','A']` | Two pairs swapped, middle `'B'` unchanged |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Flip Characters is the two-pointer reversal applied to a character array — mechanics identical to reversing integers, only the element type differs. Every future problem in this section is a variation on this same swap-and-converge core.

</details>
