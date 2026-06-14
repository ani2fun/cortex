---
title: "Breaking Index"
summary: "Sorted array and integer delta. Return the first index i where arr[i] - arr[0] > delta, or -1."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
kind: problem
topics: [upper-bound, searching]
---

# Breaking Index

## Problem Statement

Sorted array and integer `delta`. Return the first index `i` where `arr[i] - arr[0] > delta`, or `-1`.

## Examples

**Example 1**
```
Input:  arr = [1, 5, 10, 15, 20, 25], delta = 6
Output: 2
Explanation: arr[2] - arr[0] = 10 - 1 = 9 > 6; index 2 is the first that exceeds the threshold.
```

**Example 2**
```
Input:  arr = [1, 5], delta = 6
Output: -1
Explanation: arr[1] - arr[0] = 5 - 1 = 4 ≤ 6; no element exceeds the delta, so return -1.
```

## Constraints

- `1 ≤ arr.length ≤ 10^4`
- `0 ≤ delta ≤ 10^9`
- `-10^9 ≤ arr[i] ≤ 10^9`
- `arr` is sorted in ascending order.

```python run viz=array
import ast
from typing import List

class Solution:
    def breaking_index(self, arr: List[int], delta: int) -> int:
        # Your code goes here — the condition arr[i] - arr[0] > delta
        # is equivalent to arr[i] > arr[0] + delta; apply upper_bound
        # with target = arr[0] + delta; return -1 if the result equals
        # len(arr), otherwise return the result.
        return -1

arr = ast.literal_eval(input())
delta = int(input())
print(Solution().breaking_index(arr, delta))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int breakingIndex(int[] arr, int delta) {
            // Your code goes here — the condition arr[i] - arr[0] > delta
            // is equivalent to arr[i] > arr[0] + delta; apply upperBound
            // with target = arr[0] + delta; return -1 if the result equals
            // arr.length, otherwise return the result.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int delta = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().breakingIndex(arr, delta));
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
    { "id": "arr", "label": "arr", "type": "int[]", "placeholder": "[1, 5, 10, 15, 20, 25]" },
    { "id": "delta", "label": "delta", "type": "number", "placeholder": "6" }
  ],
  "cases": [
    { "args": { "arr": "[1, 5, 10, 15, 20, 25]", "delta": "6" }, "expected": "2" },
    { "args": { "arr": "[1, 2, 4, 5]", "delta": "2" }, "expected": "2" },
    { "args": { "arr": "[1, 5]", "delta": "6" }, "expected": "-1" },
    { "args": { "arr": "[3]", "delta": "0" }, "expected": "-1" },
    { "args": { "arr": "[1, 2]", "delta": "0" }, "expected": "1" },
    { "args": { "arr": "[1, 2, 3, 4, 5]", "delta": "3" }, "expected": "4" },
    { "args": { "arr": "[0, 0, 0, 0]", "delta": "0" }, "expected": "-1" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The condition `arr[i] - arr[0] > delta` is algebraically equivalent to `arr[i] > arr[0] + delta`. This is exactly the definition of an upper bound query with `target = arr[0] + delta`. Since the array is sorted, `upper_bound(arr[0] + delta)` locates the first index where the difference exceeds `delta` in `O(log n)`. If no such index exists (upper bound returns `len(arr)`), return `-1`.

</details>
<details>
<summary><h2>The Solution</h2></summary>


The condition `arr[i] - arr[0] > delta` is `arr[i] > arr[0] + delta`. So `upper_bound(arr, arr[0] + delta)` gives the answer.


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

    def breaking_index(self, arr: List[int], delta: int) -> int:

        # If the array is empty, return -1
        if len(arr) == 0:
            return -1

        # Calculate the target value which is arr[0] + delta
        target = arr[0] + delta

        # Find the upper bound index for the target value
        upper_bound_index = self.upper_bound(arr, target)

        # If upper_bound_index is equal to arr.length, it means no
        # element is greater than target, so return -1
        if upper_bound_index == len(arr):
            return -1

        # Otherwise, return the upper bound index
        else:
            return upper_bound_index


arr = ast.literal_eval(input())
delta = int(input())
print(Solution().breaking_index(arr, delta))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int upperBound(int[] arr, int target) {

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

        public int breakingIndex(int[] arr, int delta) {

            // If the array is empty, return -1
            if (arr.length == 0) {
                return -1;
            }

            // Calculate the target value which is arr[0] + delta
            int target = arr[0] + delta;

            // Find the upper bound index for the target value
            int upperBoundIndex = upperBound(arr, target);

            // If upperBoundIndex is equal to arr.length, it means no
            // element is greater than target, so return -1
            if (upperBoundIndex == arr.length) {
                return -1;
            }

            // Otherwise, return the upper bound index
            else {
                return upperBoundIndex;
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] arr = parseIntArray(sc.nextLine());
        int delta = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().breakingIndex(arr, delta));
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
<details>
<summary><h2>Key Takeaway</h2></summary>


Upper bound is the dual primitive to lower bound. Together they cover most "where in this sorted data does X go?" questions. The four problems showed direct count, threshold-crossing, ceiling lookup, and a derived-target query — all reducing to a single upper-bound call.

The next two lessons generalise binary search beyond *array indexing* — Minimum Predicate Search and Maximum Predicate Search cover the **predicate search patterns**, where the search space is a *range of integer values* (or a continuous range) and the comparison is a custom *predicate* function. This is the technique behind algorithms like "minimum number of pages a student must read in K days," "smallest divisor that fits a budget," and many other "binary search on the answer" problems.

**Transfer challenge — try before the Minimum Predicate Search Pattern lesson:** Use upper bound to find the *largest element strictly less than target* in a sorted array. Hint: the answer is at index `lower_bound(target) - 1`.

</details>
