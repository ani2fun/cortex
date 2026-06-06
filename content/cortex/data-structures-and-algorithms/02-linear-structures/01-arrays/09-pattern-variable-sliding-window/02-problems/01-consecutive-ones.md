---
title: "Consecutive Ones"
summary: "Given a binary array arr, find and return the maximum number of consecutive 1s in the array."
prereqs:
  - 09-pattern-variable-sliding-window/01-pattern
difficulty: easy
---

# Consecutive Ones

## Problem Statement

Given a binary array `arr`, find and return the **maximum number of consecutive `1`s** in the array.

```
arr = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]   →  4
arr = [1, 1, 0, 0, 0, 1, 1, 0, 1, 0]      →  2
arr = [0, 0, 0]                            →  0
```

---

## Examples

**Example 1**
```
Input:  arr = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]
Output: 4
Explanation: The maximum number of consecutive ones is 4 — the run at
             indices 6..9.
```

**Example 2**
```
Input:  arr = [1, 1, 0, 0, 0, 1, 1, 0, 1, 0]
Output: 2
Explanation: The maximum number of consecutive ones is 2 — both the run
             [1, 1] at indices 0..1 and the run [1, 1] at indices 5..6.
```

**Example 3**
```
Input:  arr = [0, 0, 0]
Output: 0
Explanation: The maximum number of consecutive ones is 0.
```

<details>
<summary><h2>Intuition</h2></summary>


The structural property: a "run of `1`s" is exactly a contiguous subarray containing no `0`. The window invariant is therefore **"the current window contains no `0`s"**. Whenever the window holds at least one `0`, it is invalid as an answer candidate and must be discarded.

The window placement reflects this directly. `end` walks the array one step at a time. A counter `count_ones` plays the role of `aggregate` — it stores the length of the current run ending at `end`. The pointer `start` is not maintained explicitly here; it is implicitly "the index just past the last seen `0`". When `arr[end]` is a `0`, the run breaks, so we compare `count_ones` against the running maximum `max_ones`, then reset the counter to zero — equivalent to leaping `start` forward to `end + 1`.

The naive approach evaluates every subarray and asks "is it all `1`s?" — O(N²) candidates, O(N) test per candidate, O(N³) total. The single-pass version only ever asks one local question: "is the current element a `1`?" Tracking the running length lets each `0` finalise a candidate cheaply, and a **final check after the loop** folds in any run that ended at the last index without seeing a closing `0`.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| Check | Answer for Consecutive Ones |
|---|---|
| **Single-result over subarrays?** | Yes — return one integer: the length of the longest run of `1`s. |
| **O(1) add to the aggregate?** | Yes — incrementing `count_ones` on a `1` is one operation. |
| **O(1) remove from the aggregate?** | Yes — a `0` resets `count_ones` to zero (a single leap, not a gradual shrink). |
| **Provable skipping?** | Yes — any window touching a `0` cannot be a candidate, so all subarrays straddling the most recent `0` are safely discarded. |

</details>
<details>
<summary><h2>Approach</h2></summary>


1. Initialize `start = 0`, `end = 0`, `count_ones = 0`, `max_ones = 0`.
2. Loop while `end < len(arr)`:
   1. If `arr[end] == 1`, increment `count_ones` by 1.
   2. Otherwise (the element is a `0`), update `max_ones = max(max_ones, count_ones)` and reset `count_ones = 0`.
   3. Advance `end` by 1.
3. After the loop, run a final `max_ones = max(max_ones, count_ones)` to fold in any trailing run that never met a closing `0`.
4. Return `max_ones`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=array viz-root=arr
from typing import List

class Solution:
    def consecutive_ones(self, arr: List[int]) -> int:

        # To store the starting index of the subarray
        start = 0

        # To store the ending index of the subarray
        end = 0

        # To store the current count of 1s in the window
        count_ones = 0

        # To store the maximum number of 1s in any subarray
        max_ones = 0

        # Move the window one step to the right until it reaches the end
        # of the array
        while end < len(arr):

            # Add the current element to the count if it's 1
            if arr[end] == 1:
                count_ones += 1

            # Otherwise, process the aggregate and reset count
            else:

                # Process aggregate when we encounter a 0
                max_ones = max(max_ones, count_ones)

                # Reset count for consecutive ones
                count_ones = 0

            # Expand the window
            end += 1

        # Final check for the last segment of ones
        max_ones = max(max_ones, count_ones)

        return max_ones


# Examples from the problem statement
print(Solution().consecutive_ones([1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]))   # 4
print(Solution().consecutive_ones([1, 1, 0, 0, 0, 1, 1, 0, 1, 0]))       # 2
print(Solution().consecutive_ones([0, 0, 0]))                              # 0

# Edge cases
print(Solution().consecutive_ones([1]))                                    # 1  — single one
print(Solution().consecutive_ones([0]))                                    # 0  — single zero
print(Solution().consecutive_ones([1, 0]))                                 # 1  — two elements
print(Solution().consecutive_ones([1, 1, 1, 1]))                           # 4  — all ones
print(Solution().consecutive_ones([1, 1, 1, 1, 0, 1]))                    # 4  — ones at end
```

```java run viz=array viz-root=arr
public class Main {
    static class Solution {
        public int consecutiveOnes(int[] arr) {

            // To store the starting index of the subarray
            int start = 0;

            // To store the ending index of the subarray
            int end = 0;

            // To store the current count of 1s in the window
            int countOnes = 0;

            // To store the maximum number of 1s in any subarray
            int maxOnes = 0;

            // Move the window one step to the right until it reaches the end
            // of the array
            while (end < arr.length) {

                // Add the current element to the count if it's 1
                if (arr[end] == 1) {
                    countOnes++;
                }

                // Otherwise, process the aggregate and reset count
                else {

                    // Process aggregate when we encounter a 0
                    maxOnes = Math.max(maxOnes, countOnes);

                    // Reset count for consecutive ones
                    countOnes = 0;
                }

                // Expand the window
                end++;
            }

            // Final check for the last segment of ones
            maxOnes = Math.max(maxOnes, countOnes);

            return maxOnes;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().consecutiveOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}));   // 4
        System.out.println(new Solution().consecutiveOnes(new int[]{1, 1, 0, 0, 0, 1, 1, 0, 1, 0}));       // 2
        System.out.println(new Solution().consecutiveOnes(new int[]{0, 0, 0}));                              // 0

        // Edge cases
        System.out.println(new Solution().consecutiveOnes(new int[]{1}));                                    // 1  — single one
        System.out.println(new Solution().consecutiveOnes(new int[]{0}));                                    // 0  — single zero
        System.out.println(new Solution().consecutiveOnes(new int[]{1, 0}));                                 // 1  — two elements
        System.out.println(new Solution().consecutiveOnes(new int[]{1, 1, 1, 1}));                           // 4  — all ones
        System.out.println(new Solution().consecutiveOnes(new int[]{1, 1, 1, 1, 0, 1}));                    // 4  — ones at end
    }
}
```


```d3 widget=array-1d
{
  "steps": [
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "a",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "arr[0] = 1 → count_ones = 1. max_ones = 0.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "c",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "After three 1s in a row: count_ones = 3. max_ones still 0 (no 0 seen yet).",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "d",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "arr[3] = 0 → process: max_ones = max(0, 3) = 3. Reset count_ones = 0.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "f",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "More 0s — max_ones stays 3, count_ones stays 0.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "j",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Four 1s at indices 6..9: count_ones = 4. max_ones still 3.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [
        {
          "name": "end",
          "target": "k",
          "color": "#10b981"
        }
      ],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "arr[10] = 0 → process: max_ones = max(3, 4) = 4. Reset count_ones = 0.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    },
    {
      "nodes": [
        {
          "id": "a",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 0,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "b",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 1,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "c",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 2,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "d",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 3,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "e",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 4,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "f",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 5,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "g",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 6,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "h",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 7,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "i",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 8,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "j",
          "label": "1",
          "kind": "cell",
          "meta": [],
          "slot": 9,
          "cardId": "",
          "layoutKind": ""
        },
        {
          "id": "k",
          "label": "0",
          "kind": "cell",
          "meta": [],
          "slot": 10,
          "cardId": "",
          "layoutKind": ""
        }
      ],
      "edges": [],
      "cursor": [],
      "highlight": [],
      "changed": [],
      "removed": [],
      "annotation": "Loop ends. Final check: max_ones = max(4, 0) = 4. Return 4.",
      "line": 0,
      "frames": [],
      "cardCursor": []
    }
  ],
  "title": "Consecutive Ones on [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]"
}
```

<details>
<summary><strong>Dry Run — arr = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0]</strong></summary>

```
start=0, end=0, count_ones=0, max_ones=0

end=0:  arr[0]=1  → count_ones=1.  max_ones=0.
end=1:  arr[1]=1  → count_ones=2.  max_ones=0.
end=2:  arr[2]=1  → count_ones=3.  max_ones=0.
end=3:  arr[3]=0  → max_ones=max(0,3)=3. count_ones=0.
end=4:  arr[4]=0  → max_ones=max(3,0)=3. count_ones=0.
end=5:  arr[5]=0  → max_ones=max(3,0)=3. count_ones=0.
end=6:  arr[6]=1  → count_ones=1.
end=7:  arr[7]=1  → count_ones=2.
end=8:  arr[8]=1  → count_ones=3.
end=9:  arr[9]=1  → count_ones=4.
end=10: arr[10]=0 → max_ones=max(3,4)=4. count_ones=0.

Final check: max_ones = max(4, 0) = 4.

Return: 4 ✓  (the run at indices 6..9)
```

</details>

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(N) | One pass through the array; constant work per element |
| **Space** | O(1) | Two integer counters (`count_ones`, `max_ones`) |

### Edge Cases

| Case | Example | Expected | Reasoning |
|---|---|---|---|
| Empty array | `[]` | `0` | Loop never runs; both counters stay 0 |
| All zeros | `[0, 0, 0]` | `0` | Each 0 resets `count_ones` to 0; `max_ones` never updates |
| All ones | `[1, 1, 1, 1]` | `4` | No 0 to trigger comparison; final check folds the run in |
| Single one | `[1]` | `1` | One iteration, then final check returns 1 |
| Trailing ones | `[0, 1, 1]` | `2` | Final check catches the run that didn't see a closing 0 |
| Single zero | `[0]` | `0` | One iteration enters the `else` branch with `count_ones = 0` |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Consecutive Ones is the simplest aggregate-then-reset variation of the sliding window — a single counter, a hard reset on each `0`, and a mandatory final check after the loop for runs that never met a closing `0`.

</details>
