---
title: "Balanced Binary Subarray"
summary: "Given a binary array arr (only 0s and 1s), return the length of the longest subarray with an equal number of 0s and 1s."
prereqs:
  - 11-pattern-prefix-sum/01-pattern
difficulty: medium
kind: problem
topics: [prefix-sum, hash-table]
---

# Balanced binary subarray

## Problem Statement

Given a binary array `arr` (only 0s and 1s), return the length of the longest subarray with an **equal** number of 0s and 1s.

### Example 1
> -   **Input:** `[1, 0, 1, 1, 1, 0, 0]` → **Output:** `6` (indices 1..6)

### Example 2
> -   **Input:** `[0, 0, 1, 1, 0]` → **Output:** `4`

### Example 3
> -   **Input:** `[1, 1, 1, 1]` → **Output:** `0`

## Examples

**Example 1**
```
Input:  [1, 0, 1, 1, 1, 0, 0]
Output: 6
Explanation: the slice from index 1 to 6 holds three 0s and three 1s → length 6.
No longer balanced slice exists.
```

**Example 2**
```
Input:  [0, 0, 1, 1, 0]
Output: 4
Explanation: the slice from index 0 to 3 holds two 0s and two 1s → length 4. The
slice 1..4 is also length 4; either is a valid longest.
```

**Example 3**
```
Input:  [1, 1, 1, 1]
Output: 0
Explanation: all 1s — no slice can balance 0s against 1s, so the answer is 0.
```

**Example 4**
```
Input:  [0, 1]
Output: 2
Explanation: one 0 and one 1 — the whole array is balanced → length 2.
```

## Constraints

- `1 ≤ arr.length ≤ 10^5`
- `arr[i]` is `0` or `1`

```python run
import ast

class Solution:
    def balanced_binary_subarray(self, arr):
        # Your code goes here — re-encode 0 → -1, 1 → +1, then find the
        # longest subarray with prefix sum 0 using a hash map of first occurrences.
        return 0

arr = ast.literal_eval(input())
print(Solution().balanced_binary_subarray(arr))
```

```java run
import java.util.*;

public class Main {
  static class Solution {
    public int balancedBinarySubarray(int[] arr) {
      // Your code goes here — re-encode 0 → -1, 1 → +1, then find the
      // longest subarray with prefix sum 0 using a map of first occurrences.
      return 0;
    }
  }

  static int[] parseIntArray(String s) {
    s = s.trim().replaceAll("[\\[\\]\\s]", "");
    if (s.isEmpty()) return new int[0];
    String[] t = s.split(",");
    int[] a = new int[t.length];
    for (int i = 0; i < t.length; i++) a[i] = Integer.parseInt(t[i].trim());
    return a;
  }

  public static void main(String[] args) {
    int[] arr = parseIntArray(new Scanner(System.in).nextLine());
    System.out.println(new Solution().balancedBinarySubarray(arr));
  }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 0, 1, 1, 1, 0, 0]" }
  ],
  "cases": [
    { "args": { "arr": "[1, 0, 1, 1, 1, 0, 0]" }, "expected": "6" },
    { "args": { "arr": "[0, 0, 1, 1, 0]" },       "expected": "4" },
    { "args": { "arr": "[1, 1, 1, 1]" },           "expected": "0" },
    { "args": { "arr": "[0, 1]" },                 "expected": "2" },
    { "args": { "arr": "[1, 0]" },                 "expected": "2" },
    { "args": { "arr": "[0, 0, 1, 1]" },           "expected": "4" }
  ]
}
```

<details>
<summary>Editorial</summary>

Re-encode `0 → −1`, `1 → +1`. Now "equal 0s and 1s" becomes "subarray sums to 0" — two equal prefix sums. Keep a map `prefix_sum → first index seen`, seeded with `{0: -1}`. When `prefix_sum` recurs at index `i`, the slice `arr[j+1..i]` is balanced; update `max_length = max(max_length, i − j)`. `O(N)` time, `O(N)` space. The `{0: -1}` seed catches slices anchored at index 0.

```python solution time=O(n) space=O(n)
import ast

class Solution:
    def balanced_binary_subarray(self, arr):
        prefix_sum_index = {}
        max_length = 0
        prefix_sum = 0
        prefix_sum_index[0] = -1

        for i in range(len(arr)):
            prefix_sum += -1 if arr[i] == 0 else 1
            if prefix_sum in prefix_sum_index:
                length = i - prefix_sum_index[prefix_sum]
                max_length = max(max_length, length)
            else:
                prefix_sum_index[prefix_sum] = i

        return max_length

arr = ast.literal_eval(input())
print(Solution().balanced_binary_subarray(arr))
```

```java solution
import java.util.*;

public class Main {
  static class Solution {
    public int balancedBinarySubarray(int[] arr) {
      Map<Integer, Integer> psIdx = new HashMap<>();
      int maxLen = 0, ps = 0;
      psIdx.put(0, -1);

      for (int i = 0; i < arr.length; i++) {
        ps += (arr[i] == 0 ? -1 : 1);
        if (psIdx.containsKey(ps)) {
          maxLen = Math.max(maxLen, i - psIdx.get(ps));
        } else {
          psIdx.put(ps, i);
        }
      }
      return maxLen;
    }
  }

  static int[] parseIntArray(String s) {
    s = s.trim().replaceAll("[\\[\\]\\s]", "");
    if (s.isEmpty()) return new int[0];
    String[] t = s.split(",");
    int[] a = new int[t.length];
    for (int i = 0; i < t.length; i++) a[i] = Integer.parseInt(t[i].trim());
    return a;
  }

  public static void main(String[] args) {
    int[] arr = parseIntArray(new Scanner(System.in).nextLine());
    System.out.println(new Solution().balancedBinarySubarray(arr));
  }
}
```

</details>
<details>
<summary><h2>Intuition</h2></summary>

A balanced subarray holds an equal number of `0`s and `1`s. The brute-force read counts both within every candidate slice: fix a start, extend an end, tally the two counts, record the length whenever they match. Each of the `O(N)` starts runs an `O(N)` extension, so the work is `O(N²)` time — and the inner tally re-counts elements the previous slice already saw.

The re-encoding trick turns counting into summing: **treat `0` as `−1` and `1` as `+1`**. Now a slice has equal counts exactly when its encoded elements sum to `0`, which happens exactly when two prefix sums are equal. Maintain a running `prefix_sum` over the `±1` values and a hash map from each prefix value to the *first* index where it appeared. When the current `prefix_sum` matches an earlier index `j`, the slice `arr[j+1..i]` nets to zero, so its length is `i − j`.

This is the same-value-search flavour of prefix sums, and the hash map is what makes it `O(1)` per index. What breaks without the `prefix_sum_index[0] = -1` base case is any balanced slice anchored at index `0` — its prefix value is `0`, and without the seeded `−1` index there is nothing earlier to match against. The diagnostic signal is "longest slice with a net-zero property", which the same-value search answers in one pass.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>

| Check | Answer for Balanced Binary Subarray |
|---|---|
| **Q1.** Does the answer reduce to a subarray sum? | **Yes** — re-encoding `0 → −1` turns "equal counts" into "slice sums to `0`". |
| **Q2.** Is the input a linear sequence walked once? | **Yes** — a binary array, swept index by index in a single pass. |
| **Q3.** Is the matching slice found by a hash-map lookup? | **Yes** — a repeated prefix value names both ends of a zero-sum slice in `O(1)`. |
| **Q4.** Does the rule survive negatives and zeros? | **Yes** — the `±1` encoding deliberately introduces negatives, exactly where a sliding window would fail. |

</details>
<details>
<summary><h2>Approach</h2></summary>

1. Initialise `prefix_sum = 0`, `max_length = 0`, and a map `prefix_sum_index` seeded with `{0: -1}` so a balanced slice starting at index `0` is caught.
2. Sweep `i` across the array. Add `+1` to `prefix_sum` for a `1` and `−1` for a `0`.
3. If `prefix_sum` has appeared before at index `j`, the slice `arr[j+1..i]` nets to zero — compute its length `i − j` and update `max_length` if it is larger.
4. Otherwise, record `prefix_sum → i` as its *first* occurrence; storing only the earliest index keeps every future matched slice as long as possible.
5. After the loop, return `max_length`.

</details>
<details>
<summary><h2>Dry Run</h2></summary>

Walk Example 1: `arr = [1, 0, 1, 1, 1, 0, 0]`, encoded as `+1, -1, +1, +1, +1, -1, -1`, expected output `6`. The map stores each prefix value's first index; a repeat marks a balanced slice:

```
prefix_sum=0, map={0:-1}, max=0

i=0  +1  P=1   first seen → store {1:0}            max=0
i=1  -1  P=0   seen @ -1 → len 1−(−1)=2            max=2
i=2  +1  P=1   seen @ 0  → len 2−0=2               max=2
i=3  +1  P=2   first seen → store {2:3}            max=2
i=4  +1  P=3   first seen → store {3:4}            max=2
i=5  -1  P=2   seen @ 3  → len 5−3=2               max=2
i=6  -1  P=1   seen @ 0  → len 6−0=6               max=6

result = 6
```

The result `6` matches the expected output — prefix value `1` first appears at index `0` and reappears at index `6`, so the slice `arr[1..6]` is the longest balanced run.

</details>
<details>
<summary><h2>Complexity Analysis</h2></summary>

| | Cost | Why |
|---|---|---|
| **Time** | **O(N)** | One pass over the array; each step does a constant number of `O(1)` hash-map reads and at most one write. |
| **Space** | **O(N)** | The map holds one entry per distinct prefix value — up to `N + 1` entries when every prefix sum is unique. |

</details>
<details>
<summary><h2>Edge Cases</h2></summary>

| Input | Output | Why |
|---|---|---|
| `[0]` | `0` | A single `0` can never balance against a `1`. |
| `[1, 1, 1, 1]` | `0` | All `1`s — no prefix value ever repeats except via the seeded `0`, which never recurs. |
| `[0, 1]` | `2` | One `0`, one `1` — the whole array nets to zero → length `2`. |
| `[1, 0]` | `2` | Order does not matter; the full slice is still balanced. |
| `[0, 0, 1, 1]` | `4` | Two `0`s then two `1`s — the entire array balances → length `4`. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>

Re-encoding `0 → −1` converts "equal counts" into "two prefix sums are equal", so a hash map of first occurrences finds the longest balanced subarray in one `O(N)` pass — and the `map[0] = -1` base case is what catches slices anchored at index `0`.

</details>
