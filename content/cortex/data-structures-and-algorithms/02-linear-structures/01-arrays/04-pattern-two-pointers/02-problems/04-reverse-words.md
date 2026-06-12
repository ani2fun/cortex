---
title: "Reverse Words"
summary: "Given a string s, reverse the characters of every word while preserving the original word order and the original whitespace exactly. The string may contain leading or trailing spaces, and words may be"
prereqs:
  - 04-pattern-two-pointers/01-pattern
difficulty: easy
kind: problem
topics: [two-pointers, arrays]
---

# Reverse Words

## The Problem

Given a string `s`, reverse the characters of every word **while preserving the original word order** and the original whitespace exactly. The string may contain leading or trailing spaces, and words may be separated by more than a single space — every space stays where it was; only the letters inside each word are flipped.

```
Input:  s = "This is a string"
Output:     "sihT si a gnirts"
```

The words stay in place — only the characters inside each word are reversed.

---

## Examples

**Example 1**
```
Input:  s = "This is a string"
Output:     "sihT si a gnirts"
Explanation: All four words are reversed; spaces are preserved.
```

**Example 2 — multiple spaces between words**
```
Input:  s = "I  love  coding"
Output:     "I  evol  gnidoc"
Explanation: Words separated by more than one space — the double spaces stay
             intact; only the words' characters reverse.
```

**Example 3 — single word**
```
Input:  s = "random"
Output:     "modnar"
Explanation: The string contains one word; it is reversed.
```

**Example 4 — single-character words**
```
Input:  s = "a b c"
Output:     "a b c"
Explanation: Reversing a single character is a no-op.
```

```quiz
{
  "prompt": "Now your turn!",
  "input": "s = \"keep coding\"",
  "options": ["peek gnidoc", "gnidoc peek", "keep coding", "peekgnidoc"],
  "answer": "peek gnidoc"
}
```

## Constraints

- `0 ≤ s.length ≤ 1000`
- `s` consists of printable ASCII characters; words are separated by spaces

```python run viz=array viz-root=arr
class Solution:
    def reverse_words(self, s: str) -> str:
        # Your code goes here — scan for each word's [start, end] range,
        # then reverse the characters inside that range with two pointers.
        return s

s = input()                          # the test case's s
print(Solution().reverse_words(s))
```

```java run viz=array viz-root=arr
import java.util.*;

public class Main {
    static class Solution {
        public String reverseWords(String s) {
            // Your code goes here — scan for each word's [start, end] range,
            // then reverse the characters inside that range with two pointers.
            return s;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        System.out.println(new Solution().reverseWords(s));
    }
}
```

```testcases
{
  "args": [
    { "id": "s", "label": "s", "type": "string", "placeholder": "This is a string" }
  ],
  "cases": [
    { "args": { "s": "This is a string" }, "expected": "sihT si a gnirts" },
    { "args": { "s": "I love coding" }, "expected": "I evol gnidoc" },
    { "args": { "s": "random" }, "expected": "modnar" },
    { "args": { "s": "  hello  world  " }, "expected": "  olleh  dlrow  " },
    { "args": { "s": "aa bb cc" }, "expected": "aa bb cc" },
    { "args": { "s": "a b c" }, "expected": "a b c" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>


Each word is a contiguous run of non-space characters bounded by spaces or by the ends of the string. Reversing every word means applying the Flip Characters reversal once per such run — same two-pointer mechanics, but on a sub-range `[word_start, word_end]` instead of the whole array. The structural shift is that there are now *many* mirror axes (one per word), not a single one across the whole string.

The algorithm splits cleanly in two. An outer linear scan walks the array left to right and locates each word's start by skipping spaces; from that start, a forward sweep finds the next space (or the end of the array), giving `word_end`. With both endpoints known, the inner two-pointer reversal runs on `[word_start, word_end]` in `O(word_length)` time. The outer pointer then jumps to `word_end + 1` to look for the next word, so each character is touched at most twice — once by the outer scan, once by the inner reversal.

A single global two-pointer pass breaks here. Whole-array `left/right` pointers can't tell where one word ends and the next begins; they'd swap characters across word boundaries and destroy word identity. Storing word slices into a list and reversing each in a second pass works but costs `O(n)` extra space for the slice copies. The outer-scan-plus-inner-reversal structure does the same work in place: `O(n)` time, with extra space only for the mutable character array the language forces (immutable strings in Python or Java).

```d3 widget=array-1d
{
  "steps": [
    {
      "nodes": [
        {
          "id": "0",
          "label": "t",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "h",
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
          "label": " ",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "s",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "y",
          "kind": "cell",
          "meta": [],
          "slot": 6,
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
          "target": "2",
          "color": "#f59e0b"
        }
      ],
      "highlight": [
        "0",
        "1",
        "2"
      ],
      "changed": [],
      "removed": [],
      "annotation": "Scan finds the first word at indices [0..2]. Reverse it: swap arr[0]='t' with arr[2]='e'.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "h",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "t",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": " ",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "s",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "y",
          "kind": "cell",
          "meta": [],
          "slot": 6,
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
          "target": "1",
          "color": "#f59e0b"
        }
      ],
      "highlight": [
        "0",
        "1",
        "2"
      ],
      "changed": [],
      "removed": [],
      "annotation": "Inside word 1, the pointers meet at index 1; the middle 'h' is its own mirror.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "h",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "t",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": " ",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "s",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "y",
          "kind": "cell",
          "meta": [],
          "slot": 6,
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
          "target": "6",
          "color": "#f59e0b"
        }
      ],
      "highlight": [
        "4",
        "5",
        "6"
      ],
      "changed": [],
      "removed": [],
      "annotation": "Scan skips the space at index 3, then finds the second word at indices [4..6]. Reverse it: swap arr[4]='s' with arr[6]='y'.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "0",
          "label": "e",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "1",
          "label": "h",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "2",
          "label": "t",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "3",
          "label": " ",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "4",
          "label": "y",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "5",
          "label": "k",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "6",
          "label": "s",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "left",
          "target": "5",
          "color": "#3b82f6"
        },
        {
          "name": "right",
          "target": "5",
          "color": "#f59e0b"
        }
      ],
      "highlight": [
        "4",
        "5",
        "6"
      ],
      "changed": [],
      "removed": [],
      "annotation": "Inside word 2, the pointers meet at index 5. Result: \"eht yks\".",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ],
  "title": "Reverse each word in \"the sky\""
}
```

<p align="center"><strong>Reverse Words on <code>"the sky"</code> — the outer scan finds each word's boundaries (highlighted band); two pointers then reverse the characters inside that range.</strong></p>

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Reverse Words |
|---|---|
| ✅ Two positions simultaneously? | Yes — inside each word's reversal, `chars[left]` and `chars[right]` are swapped together |
| ✅ One near start, one near end? | Yes — for each word, `left = word_start`, `right = word_end` |
| ✅ Both move inward? | Yes — `left++`, `right--` within each word's reversal loop |
| ✅ Simple work at each step? | Yes — one swap per pair within the word |

The outer scan that finds word boundaries is bookkeeping — once a `[word_start, word_end]` range is identified, the inner two-pointer reversal is a textbook direct application on that sub-range.

**Why find word boundaries with a linear scan instead of outer two pointers?** This problem operates on each word independently, not on a single pair of positions across the whole string. The outer scan moves linearly left-to-right, identifying the next word. For each word found, two inner pointers cover it from start to end. Trying to maintain two outer pointers across the full string wouldn't give word-by-word control — you'd lose the ability to identify where each word's characters start and end.

**What connects this to the direct-application pattern?** The `reverse(chars, l, r)` helper is a pure direct application. The outer scan is word-boundary discovery. Reverse Words decomposes as: discover word boundary → directly apply two-pointer reversal on that range → repeat. Composing multiple direct applications, each on a different sub-range, is still the direct-application pattern — the same four checks hold for every inner reversal call.

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Convert the string to a mutable character list (if needed)
2. Use an outer pointer `i` to scan from left to right
3. For each position `i`:
   - If `chars[i]` is not a space, it's the **start of a word** — record `word_start = i`
   - Advance `i` until you hit a space or the end of the array — `i - 1` is `word_end`
   - Apply two-pointer reversal on `chars[word_start : word_end]`
4. Return the joined result

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### Solution

```python solution time=O(n) space=O(n)
class Solution:
    def find_word_end(self, arr, start):

        # Assign the start index to the end index
        end = start

        # Iterate through the string until a space is encountered
        while end < len(arr) and arr[end] != " ":
            end += 1

        # Return the index of the last character of the word
        return end - 1

    def reverse_word(self, arr, left, right):

        # Use a while loop to traverse the string using the two pointers
        while left < right:

            # Swap the characters pointed by the left and right pointers
            arr[left], arr[right] = arr[right], arr[left]

            # Move the pointers towards the center of the string
            left += 1
            right -= 1

    def reverse_words(self, s: str) -> str:
        arr = list(s)
        start = 0

        # Iterate through the string
        while start < len(arr):

            # Skip any leading spaces
            if arr[start] == " ":
                start = start + 1
                continue

            # Find the end of the current word
            end = self.find_word_end(arr, start)

            # Reverse the characters in the current word using two
            # pointer method
            self.reverse_word(arr, start, end)

            # Move the start pointer to the next word
            start = end + 1

        return "".join(arr)


s = input()                          # the test case's s
print(Solution().reverse_words(s))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int findWordEnd(char[] arr, int start) {

            // Assign the start index to the end index
            int end = start;

            // Iterate through the string until a space is encountered
            while (end < arr.length && arr[end] != ' ') {
                end++;
            }

            // Return the index of the last character of the word
            return end - 1;
        }

        private void reverseWord(char[] arr, int left, int right) {

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

        public String reverseWords(String s) {
            char[] arr = s.toCharArray();
            int start = 0;

            // Iterate through the string
            while (start < s.length()) {

                // Skip any leading spaces
                if (arr[start] == ' ') {
                    start++;
                    continue;
                }

                // Find the end of the current word
                int end = findWordEnd(arr, start);

                // Reverse the characters in the current word using two
                // pointer method
                reverseWord(arr, start, end);

                // Move the start pointer to the next word
                start = end + 1;
            }

            return new String(arr);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        System.out.println(new Solution().reverseWords(s));
    }
}
```

### Dry Run — "the sky"

`arr = ['t','h','e',' ','s','k','y']`, `n = 7`

**Word 1:** outer scan finds non-space at `start = 0`; `findWordEnd` returns `end = 2`.

| Step | `left` | `right` | Swap | Array |
|---|---|---|---|---|
| 1 | 0 | 2 | `t ↔ e` | `['e','h','t',' ','s','k','y']` |
| — | 1 | 1 | `left ≥ right` — stop | — |

`start` advances to `end + 1 = 3`. The space at index 3 is skipped; `start = 4`.

**Word 2:** `findWordEnd` returns `end = 6`.

| Step | `left` | `right` | Swap | Array |
|---|---|---|---|---|
| 1 | 4 | 6 | `s ↔ y` | `['e','h','t',' ','y','k','s']` |
| — | 5 | 5 | `left ≥ right` — stop | — |

`start` advances to `end + 1 = 7 = n` — outer loop exits.

**Return `"eht yks"`** ✓

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(n) | Each character is visited at most twice — once by the outer scan, once by the inner reversal |
| **Space** | O(n) | The `chars` list (O(1) if the input were already mutable) |

### Edge Cases

| Scenario | Input | Output | Note |
|---|---|---|---|
| Empty string | `""` | `""` | Outer loop never runs |
| Single word | `"hello"` | `"olleh"` | One reversal |
| All spaces | `"   "` | `"   "` | Every char is a space — no reversals |
| Leading/trailing spaces | `" hi "` | `" ih "` | Spaces skipped; only `"hi"` reversed |
| Single-char words | `"a b"` | `"a b"` | Reversing one character is a no-op |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Reverse Words composes two ideas: an outer scan that finds each word's boundaries, and a two-pointer reversal that operates on the range it finds. This "find a range, then reverse it in place" shape recurs across the two-pointer family — Reverse Segments and Reverse Word Order are both variants of the same composition.

</details>
