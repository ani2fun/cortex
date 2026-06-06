---
title: "Limit Count"
summary: "Sorted array, integer k. Return the count of elements ≤ k."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: easy
---

# Limit Count

## The Problem

Sorted array, integer `k`. Return the count of elements `≤ k`.

```
Input:  arr = [1, 3, 5, 8, 9], k = 7
Output: 3   (elements 1, 3, 5)

Input:  arr = [1, 2, 2, 2, 3, 4], k = 3
Output: 5

Input:  arr = [1, 2, 2, 2, 3, 4], k = 8
Output: 6
```

<details>
<summary><h2>The Solution</h2></summary>


The count of elements `≤ k` is exactly `upper_bound(arr, k)` — that's the first index strictly greater than `k`, which equals the number of elements `≤ k`.


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

    def limit_count(self, arr: List[int], k: int) -> int:

        # The number of elements <= k is given by the
        # upper bound index of k
        return self.upper_bound(arr, k)


# Examples from the problem statement
print(Solution().limit_count([1, 3, 5, 8, 9], 7))       # 3
print(Solution().limit_count([1, 2, 2, 2, 3, 4], 3))    # 5
print(Solution().limit_count([1, 2, 2, 2, 3, 4], 8))    # 6

# Edge cases
print(Solution().limit_count([], 5))                     # 0 — empty array
print(Solution().limit_count([5], 5))                    # 1 — single element equal to k
print(Solution().limit_count([5], 3))                    # 0 — single element greater than k
print(Solution().limit_count([5], 7))                    # 1 — single element less than k
print(Solution().limit_count([1, 2, 2, 2, 3, 4], 0))    # 0 — none qualify
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

        public int limitCount(int[] arr, int k) {

            // The number of elements <= k is given by the
            // upper bound index of k
            return upperBound(arr, k);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().limitCount(new int[]{1, 3, 5, 8, 9}, 7));       // 3
        System.out.println(new Solution().limitCount(new int[]{1, 2, 2, 2, 3, 4}, 3));    // 5
        System.out.println(new Solution().limitCount(new int[]{1, 2, 2, 2, 3, 4}, 8));    // 6

        // Edge cases
        System.out.println(new Solution().limitCount(new int[]{}, 5));                     // 0 — empty array
        System.out.println(new Solution().limitCount(new int[]{5}, 5));                    // 1 — single element equal to k
        System.out.println(new Solution().limitCount(new int[]{5}, 3));                    // 0 — single element greater than k
        System.out.println(new Solution().limitCount(new int[]{5}, 7));                    // 1 — single element less than k
        System.out.println(new Solution().limitCount(new int[]{1, 2, 2, 2, 3, 4}, 0));    // 0 — none qualify
    }
}
```

</details>
