---
title: "Vowel Exchange"
summary: "Given a string s, reverse the vowels in the string and return the updated string. All non-vowel characters stay in place. The vowels to consider are the English-alphabet ones — a, e, i, o, u, in both "
prereqs:
  - 04-pattern-two-pointers/01-pattern
difficulty: easy
kind: problem
topics: [two-pointers, arrays]
---

# Vowel Exchange

## The Problem

Given a string `s`, **reverse the vowels** in the string and return the updated string. All non-vowel characters stay in place. The vowels to consider are the English-alphabet ones — `a`, `e`, `i`, `o`, `u`, in both uppercase and lowercase.

```
Input:  s = "random"     →   Output: "rondam"
Input:  s = "afegijoku"  →   Output: "ufogijeka"
Input:  s = "bcdf"       →   Output: "bcdf"
```

---

## Examples

**Example 1**
```
Input:  s = "random"
Output: "rondam"
Explanation: The vowels 'a' (index 1) and 'o' (index 2) are swapped.
```

**Example 2**
```
Input:  s = "afegijoku"
Output: "ufogijeka"
Explanation: The vowels are swapped in mirror-pair order:
             - 'a' (index 0) is swapped with 'u' (index 8)
             - 'e' (index 2) is swapped with 'o' (index 6)
             - 'i' (index 4) is its own mirror; it stays in place.
```

**Example 3**
```
Input:  s = "bcdf"
Output: "bcdf"
Explanation: No vowels — the string is unchanged.
```

**Example 4 — all vowels**
```
Input:  s = "aeiou"
Output: "uoiea"
Explanation: Every position is a vowel, so the swap reduces to a full reversal.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "s = \"cortex\"",
  "options": ["cortex", "certox", "cortxe", "cxrteo"],
  "answer": "certox"
}
```

## Constraints

- `0 ≤ s.length ≤ 1000`
- `s` consists of printable ASCII characters

```python run viz=array viz-root=chars
class Solution:
    def vowel_exchange(self, s: str) -> str:
        # Your code goes here — two pointers from both ends; slide past
        # consonants, swap when both sides land on a vowel.
        return s

s = input()                          # the test case's s
print(Solution().vowel_exchange(s))
```

```java run viz=array viz-root=chars
import java.util.*;

public class Main {
    static class Solution {
        public String vowelExchange(String s) {
            // Your code goes here — two pointers from both ends; slide past
            // consonants, swap when both sides land on a vowel.
            return s;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        System.out.println(new Solution().vowelExchange(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "afegijoku" }
  ],
  "cases": [
    { "args": { "s": "random" }, "expected": "rondam" },
    { "args": { "s": "afegijoku" }, "expected": "ufogijeka" },
    { "args": { "s": "bcdf" }, "expected": "bcdf" },
    { "args": { "s": "aeiou" }, "expected": "uoiea" },
    { "args": { "s": "AEIou" }, "expected": "uoIEA" },
    { "args": { "s": "hello" }, "expected": "holle" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


Reversing the vowels means pairing them up — the first vowel from the left swaps with the first vowel from the right, the second from the left with the second from the right, and so on. Non-vowel characters keep their original positions. The pairing structure is identical to Flip Characters, but now you must skip past every consonant before each swap.

Place `left` at index `0` and `right` at index `n − 1`. At each iteration the cascade decides which pointer moves. If `chars[left]` is a consonant, advance `left` alone — that side hasn't found its vowel yet. If `chars[right]` is a consonant, retreat `right` alone. When both pointers sit on vowels in the same iteration, swap them and step both inward. The outer `while left < right` guard stays the loop's only terminator; no inner loop is needed, because each "skip" advances exactly one pointer and re-enters the same outer check.

A single-pointer approach would scan once to collect every vowel's index, then walk a second pass to swap mirrored vowels using that list. That costs `O(n)` time but also `O(v)` extra space for the index list (where `v` is the vowel count). The two-pointer version drops the auxiliary list entirely — `left` and `right` implicitly track "the leftmost unswapped vowel" and "the rightmost unswapped vowel," so the pairing happens in place.

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
          "label": "f",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "g",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "j",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "u",
          "kind": "cell",
          "meta": [],
          "slot": 8,
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
          "target": "8",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Both pointers are on vowels — swap arr[left]='a' with arr[right]='u'.",
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
          "label": "f",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "g",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "j",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 8,
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
          "target": "7",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Move inward — arr[left]='f' is a consonant (skip), arr[right]='k' is a consonant (skip).",
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
          "label": "f",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "g",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "j",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 8,
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
          "target": "6",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Both pointers are on vowels — swap arr[left]='e' with arr[right]='o'.",
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
          "label": "f",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "g",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "j",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "3",
          "color": "#3b82f6"
        },
        {
          "name": "right",
          "target": "5",
          "color": "#f59e0b"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "arr[left]='g' is a consonant (skip), arr[right]='j' is a consonant (skip).",
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
          "label": "f",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "o",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": "g",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "i",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "j",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "7",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "8",
          "label": "a",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "4",
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
      "annotation": "Pointers meet at index 4 — the middle 'i' is its own mirror; nothing to swap. Result: \"ufogijeka\".",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ],
  "title": "Vowel exchange on \"afegijoku\""
}
```

<p align="center"><strong>Vowel exchange on <code>"afegijoku"</code> — each pointer scans past consonants until it finds a vowel; matched vowel pairs swap; pointers meet at the middle <code>'i'</code>.</strong></p>

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Vowel Exchange |
|---|---|
| ✅ Two positions simultaneously? | Yes — `chars[left]` and `chars[right]` are both evaluated, and swapped together once both sit on vowels |
| ✅ One near start, one near end? | Yes — `left = 0`, `right = n-1` |
| ✅ Both move inward? | Yes — when both are on vowels, both advance after the swap; when one sits on a consonant, that side alone advances |
| ✅ Simple work at each step? | Yes — one constant-time check on each pointer, with at most one swap per iteration |

This is a direct application with one variation: not every iteration produces a swap. The loop body uses an `if / elif / else` cascade — if `chars[left]` is a consonant, advance `left`; otherwise if `chars[right]` is a consonant, retreat `right`; otherwise both are vowels and the pair gets swapped. The template is still the same; some iterations just slide a pointer instead of swapping.

**Why advance only one pointer per iteration on a consonant?** Vowels and consonants are interleaved arbitrarily. Each side needs to settle on a vowel before any swap is safe, but the two sides reach their next vowel at different speeds. By advancing one pointer at a time, the `while left < right` guard still controls termination cleanly — there is no inner loop that could run past `right` (or under `left`) and break the invariant. When both pointers happen to land on vowels in the same iteration, the `else` branch swaps them and steps both inward at once.

**What breaks if you use only one pointer?** A single forward pointer can collect vowel positions into a list, but then you need a second pass and a stack (or reverse of that list) to know which vowel to pair each one with. Two pointers eliminate that storage — the right pointer always tracks the vowel that the left's current vowel should swap with. The two-pointer structure implicitly encodes "pair the leftmost unswapped vowel with the rightmost unswapped vowel," which is exactly what reversal of vowels requires.

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Convert the string to a list of characters (strings are immutable in Python)
2. `left = 0`, `right = len - 1`, define `vowels = set("aeiouAEIOU")`
3. While `left < right`:
   - If `chars[left]` is not a vowel, advance `left` and continue
   - Else if `chars[right]` is not a vowel, retreat `right` and continue
   - Else (both pointers are on vowels), swap `chars[left]` and `chars[right]`, then `left++`, `right--`
4. Return `"".join(chars)`

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n) space=O(n)
class Solution:
    def vowel_exchange(self, s: str) -> str:

        # Create a set to store all the vowels in both uppercase and
        # lowercase
        vowels = set(["a", "e", "i", "o", "u", "A", "E", "I", "O", "U"])

        # Initialize two pointers, one pointing to the beginning of the
        # string and the other pointing to the end of the string
        left: int = 0
        right: int = len(s) - 1

        # Convert the string to an array for easier manipulation
        chars = list(s)

        # Use a while loop to traverse the string using the two pointers
        while left < right:

            # Check if the character pointed by the first pointer is a
            # vowel. If it is not a vowel, move the pointer to the next
            # character
            if chars[left] not in vowels:
                left += 1

            # Check if the character pointed by the second pointer is a
            # vowel. If it is not a vowel, move the pointer to the
            # previous character
            elif chars[right] not in vowels:
                right -= 1

            # If both pointers point to vowels, swap the characters
            else:
                chars[left], chars[right] = chars[right], chars[left]
                left += 1
                right -= 1

        # Convert the array back to a string and return the modified
        # string
        return "".join(chars)


s = input()                          # the test case's s
print(Solution().vowel_exchange(s))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public String vowelExchange(String s) {

            // Create a hash set to store all the vowels in both uppercase
            // and lowercase
            HashSet<Character> vowels = new HashSet<Character>();
            vowels.add('a');
            vowels.add('e');
            vowels.add('i');
            vowels.add('o');
            vowels.add('u');
            vowels.add('A');
            vowels.add('E');
            vowels.add('I');
            vowels.add('O');
            vowels.add('U');

            // Initialize two pointers, one pointing to the beginning of the
            // string and the other pointing to the end of the string
            int left = 0;
            int right = s.length() - 1;

            // Convert the string to a character array for easier
            // manipulation
            char[] chars = s.toCharArray();

            // Use a while loop to traverse the string using the two pointers
            while (left < right) {

                // Check if the character pointed by the first pointer is a
                // vowel If it is not a vowel, move the pointer to the next
                // character
                if (!vowels.contains(chars[left])) {
                    left++;
                }

                // Check if the character pointed by the second pointer is a
                // vowel If it is not a vowel, move the pointer to the
                // previous character
                else if (!vowels.contains(chars[right])) {
                    right--;
                }

                // If both pointers point to vowels, swap the characters
                else {
                    char temp = chars[left];
                    chars[left] = chars[right];
                    chars[right] = temp;
                    left++;
                    right--;
                }
            }

            // Convert the character array back to a string and return the
            // modified string
            return new String(chars);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        System.out.println(new Solution().vowelExchange(s));
    }
}
```

### Dry Run — "afegijoku"

`s = "afegijoku"`, `n = 9`. Vowels live at indices `0 (a)`, `2 (e)`, `4 (i)`, `6 (o)`, `8 (u)`.

| Iteration | `left` | `right` | Branch taken | Action | String |
|---|---|---|---|---|---|
| 1 | 0 (`a`, vowel) | 8 (`u`, vowel) | `else` (both vowels) | swap `a ↔ u`, then `left++`, `right--` | `"ufegijoka"` |
| 2 | 1 (`f`, consonant) | 7 (`k`, consonant) | `if` (left consonant) | `left++` | `"ufegijoka"` |
| 3 | 2 (`e`, vowel) | 7 (`k`, consonant) | `elif` (right consonant) | `right--` | `"ufegijoka"` |
| 4 | 2 (`e`, vowel) | 6 (`o`, vowel) | `else` (both vowels) | swap `e ↔ o`, then `left++`, `right--` | `"ufogijeka"` |
| 5 | 3 (`g`, consonant) | 5 (`j`, consonant) | `if` (left consonant) | `left++` | `"ufogijeka"` |
| 6 | 4 (`i`, vowel) | 5 (`j`, consonant) | `elif` (right consonant) | `right--` | `"ufogijeka"` |
| — | 4 | 4 | — | `left ≥ right` — stop | `"ufogijeka"` ✓ |

Each iteration takes exactly one branch of the cascade — either it slides one pointer past a consonant, or both pointers sit on vowels and the pair gets swapped. The middle character at index 4 (`'i'`) is its own mirror, so no swap touches it.

**Return `"ufogijeka"`** ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(n) | Each character is visited at most once by each pointer — total work is O(n) |
| **Space** | O(n) | The `chars` list copy of the input string |

> If the input were a mutable character array (as in C++/Java), space would drop to O(1). In Python we need the list copy because strings are immutable.

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| No vowels | `"bcdf"` | `"bcdf"` | Pointers never stop to swap |
| All vowels | `"aeiou"` | `"uoiea"` | Every step swaps |
| Single character | `"a"` | `"a"` | Loop never runs |
| Already reversed vowels | `"uoiea"` | `"aeiou"` | Swap brings original back |
| Mixed case | `"hEllo"` | `"hollE"` | Uppercase vowels counted too |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Vowel Exchange adds a filter to the swap-and-converge core: not every position is a candidate. The `if / elif / else` cascade slides one pointer past non-candidates and swaps only when both sides land on vowels — the same "slide past, then act" shape that recurs whenever a two-pointer problem operates on a subset of the array.

</details>
