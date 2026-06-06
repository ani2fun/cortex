---
title: "Positive Index"
summary: "Sorted array. Return the index of the first positive element, or -1 if none."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
---

# Positive Index

## The Problem

Sorted array. Return the index of the first positive element, or `-1` if none.

```
Input:  arr = [-5, -3, -1, 0, 2, 4, 6]
Output: 4

Input:  arr = [-1, 2, 2, 2, 3, 4]
Output: 1

Input:  arr = [-1, -2]
Output: -1
```

<details>
<summary><h2>The Solution</h2></summary>


`upper_bound(arr, 0)` returns the first index where `arr[i] > 0` — exactly the first positive element. Return `-1` if it's `n`.


```python run viz=array
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


# Examples from the problem statement
print(Solution().positive_index([-5, -3, -1, 0, 2, 4, 6]))  # 4
print(Solution().positive_index([-1, 2, 2, 2, 3, 4]))        # 1
print(Solution().positive_index([-1, -2]))                    # -1

# Edge cases
print(Solution().positive_index([]))                          # -1  (empty)
print(Solution().positive_index([1]))                         # 0   (single positive)
print(Solution().positive_index([-5]))                        # -1  (single negative)
print(Solution().positive_index([0, 0, 0]))                   # -1  (all zero)
print(Solution().positive_index([-3, -2, -1, 0, 1]))          # 4   (positive at last)
print(Solution().positive_index([5, 10, 15]))                 # 0   (all positive)
```

```java run viz=array
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
        // Examples from the problem statement
        System.out.println(new Solution().positiveIndex(new int[]{-5, -3, -1, 0, 2, 4, 6}));  // 4
        System.out.println(new Solution().positiveIndex(new int[]{-1, 2, 2, 2, 3, 4}));        // 1
        System.out.println(new Solution().positiveIndex(new int[]{-1, -2}));                    // -1

        // Edge cases
        System.out.println(new Solution().positiveIndex(new int[]{}));                          // -1  (empty)
        System.out.println(new Solution().positiveIndex(new int[]{1}));                         // 0   (single positive)
        System.out.println(new Solution().positiveIndex(new int[]{-5}));                        // -1  (single negative)
        System.out.println(new Solution().positiveIndex(new int[]{0, 0, 0}));                   // -1  (all zero)
        System.out.println(new Solution().positiveIndex(new int[]{-3, -2, -1, 0, 1}));          // 4   (positive at last)
        System.out.println(new Solution().positiveIndex(new int[]{5, 10, 15}));                 // 0   (all positive)
    }
}
```

</details>
