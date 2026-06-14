---
title: "Positive Index"
summary: "Sorted array. Return the index of the first positive element, or -1 if none."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
kind: problem
topics: [upper-bound, searching]
---

# Positive Index

## Problem Statement

Sorted array. Return the index of the first positive element, or `-1` if none.

```
Input:  arr = [-5, -3, -1, 0, 2, 4, 6]
Output: 4

Input:  arr = [-1, 2, 2, 2, 3, 4]
Output: 1

Input:  arr = [-1, -2]
Output: -1
```

## Examples

**Example 1**
```
Input:  arr = [-5, -3, -1, 0, 2, 4, 6]
Output: 4
Explanation: arr[4] = 2 is the first element strictly greater than 0.
```

**Example 2**
```
Input:  arr = [-1, -2]
Output: -1
Explanation: No element is positive; the array contains only negative values.
```

## Constraints

- `0 ≤ arr.length ≤ 10^5`
- `-10^9 ≤ arr[i] ≤ 10^9`, array is sorted in non-decreasing order

```python run viz=array
import ast
from typing import List

class Solution:
    def positive_index(self, arr: List[int]) -> int:
        # Your code goes here — upper_bound(arr, 0) gives the first index
        # where arr[i] > 0. Return -1 if that index equals len(arr).
        return -1

arr = ast.literal_eval(input())
print(Solution().positive_index(arr))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int positiveIndex(int[] arr) {
            // Your code goes here — upper_bound(arr, 0) gives the first index
            // where arr[i] > 0. Return -1 if that index equals arr.length.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().positiveIndex(arr));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[-5, -3, -1, 0, 2, 4, 6]" }
  ],
  "cases": [
    { "args": { "arr": "[-5, -3, -1, 0, 2, 4, 6]" }, "expected": "4" },
    { "args": { "arr": "[-1, 2, 2, 2, 3, 4]" }, "expected": "1" },
    { "args": { "arr": "[-1, -2]" }, "expected": "-1" },
    { "args": { "arr": "[1]" }, "expected": "0" },
    { "args": { "arr": "[-5]" }, "expected": "-1" },
    { "args": { "arr": "[0, 0, 0]" }, "expected": "-1" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

"First positive" means "first element strictly greater than 0" — the classic upper-bound trigger. `upper_bound(arr, 0)` runs the `≤` binary search against 0: it steps past every element `≤ 0` (negatives and zeros) and lands on the first `> 0`. If the returned index equals the array length, no positive element exists and we return `-1`. One call, `O(log n)`.

</details>
<details>
<summary><h2>The Solution</h2></summary>


`upper_bound(arr, 0)` returns the first index where `arr[i] > 0` — exactly the first positive element. Return `-1` if it's `n`.


```python solution time=O(log n) space=O(1)
import ast
from typing import List

class Solution:
    def upper_bound(self, arr: List[int], target: int) -> int:

        # Initialise starting index to 0
        low: int = 0

        # Initialise ending index to len(arr) instead of len(arr) - 1
        # to cover the entire array as if all elements in the array are less
        # than target, the upper bound index would be equal to len(arr)
        high: int = len(arr)

        # 'high' is exclusive (can be len(arr)), so we use 'low < high' instead
        # of 'low <= high'. This loop finds the first index where the element is
        # > the target without going out of bounds.
        while low < high:

            # Find the middle index
            mid: int = low + (high - low) // 2

            # If arr[mid] is less than or equal to target, then find
            # in the right subarray
            if arr[mid] <= target:
                low = mid + 1

            # If arr[mid] is greater than the target, then it may be the answer.
            # So, instead of high = mid - 1, we do high = mid to include mid in
            # the next search space
            else:
                high = mid

        # Return the upper bound index, it could be equal to arr.length
        # if all elements are less than target
        return low

    def positive_index(self, arr: List[int]) -> int:

        # Find the upper bound index for 0 i.e first index where arr[i] >
        # 0
        upper_bound_index: int = self.upper_bound(arr, 0)

        # If upper_bound_index == len(arr), no positive element exists
        if upper_bound_index == len(arr):
            return -1

        # Return the first positive index
        return upper_bound_index


arr = ast.literal_eval(input())
print(Solution().positive_index(arr))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int upperBound(int[] arr, int target) {

            // Initialise starting index to 0
            int low = 0;

            // Initialise ending index to arr.length instead of arr.length -
            // 1 to cover the entire array as if all elements in the array
            // are less than target, the upper bound index would be equal to
            // arr.length
            int high = arr.length;

            // 'high' is exclusive (can be arr.length), so we use 'low <
            // high' instead of 'low <= high'. This loop finds the first
            // index where the element is > the target without going out of
            // bounds.
            while (low < high) {

                // Find the middle index
                int mid = low + (high - low) / 2;

                // If arr[mid] is less than or equal to target, then find
                // in the right subarray
                if (arr[mid] <= target) {
                    low = mid + 1;
                }

                // If arr[mid] is greater than the target, then it may be the
                // answer. So, instead of high = mid - 1, we do high = mid to
                // include mid in the next search space
                else {
                    high = mid;
                }
            }

            // Return the upper bound index, it could be equal to arr.length
            // if all elements are less than target
            return low;
        }

        public int positiveIndex(int[] arr) {

            // Find the upper bound index for 0 i.e first index where arr[i]
            // > 0
            int upperBoundIndex = upperBound(arr, 0);

            // If upperBoundIndex == arr.length, no positive element exists
            if (upperBoundIndex == arr.length) {
                return -1;
            }

            // Return the first positive index
            return upperBoundIndex;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        System.out.println(new Solution().positiveIndex(arr));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

</details>
