---
title: "Breaking Index"
summary: "Sorted array and integer delta. Return the first index i where arr[i] - arr[0] > delta, or -1."
prereqs:
  - 10-pattern-upper-bound/01-pattern
difficulty: medium
---

# Breaking Index

## The Problem

Sorted array and integer `delta`. Return the first index `i` where `arr[i] - arr[0] > delta`, or `-1`.

```
Input:  arr = [1, 5, 10, 15, 20, 25], delta = 6
Output: 2   (arr[2] - arr[0] = 9 > 6)

Input:  arr = [1, 2, 4, 5], delta = 2
Output: 2

Input:  arr = [1, 5], delta = 6
Output: -1
```

<details>
<summary><h2>The Solution</h2></summary>


The condition `arr[i] - arr[0] > delta` is `arr[i] > arr[0] + delta`. So `upper_bound(arr, arr[0] + delta)` gives the answer.


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


# Examples from the problem statement
print(Solution().breaking_index([1, 5, 10, 15, 20, 25], 6))  # 2
print(Solution().breaking_index([1, 2, 4, 5], 2))            # 2
print(Solution().breaking_index([1, 5], 6))                   # -1

# Edge cases
print(Solution().breaking_index([], 5))                       # -1  (empty)
print(Solution().breaking_index([3], 0))                      # -1  (single element)
print(Solution().breaking_index([1, 2], 0))                   # 1   (delta=0, first i where arr[i]>arr[0])
print(Solution().breaking_index([1, 2], 5))                   # -1  (diff never exceeds delta)
print(Solution().breaking_index([1, 2, 3, 4, 5], 3))          # 4   (breaking at last index)
print(Solution().breaking_index([0, 0, 0, 0], 0))             # -1  (all same)
```

```java run viz=array
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
        // Examples from the problem statement
        System.out.println(new Solution().breakingIndex(new int[]{1, 5, 10, 15, 20, 25}, 6));  // 2
        System.out.println(new Solution().breakingIndex(new int[]{1, 2, 4, 5}, 2));            // 2
        System.out.println(new Solution().breakingIndex(new int[]{1, 5}, 6));                   // -1

        // Edge cases
        System.out.println(new Solution().breakingIndex(new int[]{}, 5));                       // -1  (empty)
        System.out.println(new Solution().breakingIndex(new int[]{3}, 0));                      // -1  (single element)
        System.out.println(new Solution().breakingIndex(new int[]{1, 2}, 0));                   // 1   (delta=0)
        System.out.println(new Solution().breakingIndex(new int[]{1, 2}, 5));                   // -1  (diff never exceeds delta)
        System.out.println(new Solution().breakingIndex(new int[]{1, 2, 3, 4, 5}, 3));          // 4   (breaking at last index)
        System.out.println(new Solution().breakingIndex(new int[]{0, 0, 0, 0}, 0));             // -1  (all same)
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
