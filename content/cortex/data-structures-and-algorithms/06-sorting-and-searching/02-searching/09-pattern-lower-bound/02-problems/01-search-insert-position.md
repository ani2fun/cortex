---
title: "Search Insert Position"
summary: "Given a sorted array arr and target, return target's index if present; otherwise return the index where target would be inserted to keep the array sorted."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: easy
---

# Search Insert Position

Direct lower-bound application.

## The Problem

Given a sorted array `arr` and `target`, return target's index if present; otherwise return the index where target would be inserted to keep the array sorted.

```
Input:  arr = [1, 2, 3, 4, 5, 6], target = 3
Output: 2

Input:  arr = [1, 2, 7, 8, 9, 10], target = 3
Output: 2   (would insert before 7)

Input:  arr = [1, 2, 7, 9, 10, 11], target = 8
Output: 3
```

<details>
<summary><h2>The Solution</h2></summary>


Just lower bound.


```python run viz=array
from typing import List

class Solution:
    def lower_bound(self, arr: List[int], target: int) -> int:

        # Initialise starting index to 0
        low: int = 0

        # Initialise ending index to len(arr) instead of len(arr) - 1
        # to cover the entire array as if all elements in the array are less
        # than target, the lower bound index would be equal to len(arr)
        high: int = len(arr)

        # 'high' is exclusive (can be len(arr)), so we use 'low < high' instead
        # of 'low <= high'. This loop finds the first index where the element is
        # >= the target without going out of bounds.
        while low < high:

            # Find the middle index
            mid: int = low + (high - low) // 2

            # If arr[mid] is less than arr[target], then find in
            # right subarray
            if arr[mid] < target:
                low = mid + 1

            # If arr[mid] is greater than or equal to target, then it may
            # be the answer. So, instead of high = mid - 1, we do high = mid
            # to include mid in the next search space
            else:
                high = mid

        # Return the lower bound index, it could be equal to len(arr)
        # if all elements are less than target
        return low

    def search_insert_position(self, arr: List[int], target: int) -> int:
        return self.lower_bound(arr, target)


# Examples from the problem statement
print(Solution().search_insert_position([1, 2, 3, 4, 5, 6], 3))   # 2
print(Solution().search_insert_position([1, 2, 7, 8, 9, 10], 3))  # 2
print(Solution().search_insert_position([1, 2, 7, 9, 10, 11], 8)) # 3

# Edge cases
print(Solution().search_insert_position([], 5))                    # 0 — empty array
print(Solution().search_insert_position([5], 5))                   # 0 — single element match
print(Solution().search_insert_position([5], 3))                   # 0 — insert before only element
print(Solution().search_insert_position([5], 7))                   # 1 — insert after only element
print(Solution().search_insert_position([1, 2, 3, 4, 5, 6], 7))   # 6 — insert at end
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private int lowerBound(int[] arr, int target) {

            // Initialise starting index to 0
            int low = 0;

            // Initialise ending index to arr.length instead of arr.length -
            // 1 to cover the entire array as if all elements in the array
            // are less than target, the lower bound index would be equal to
            // arr.length
            int high = arr.length;

            // 'high' is exclusive (can be arr.length), so we use 'low <
            // high' instead of 'low <= high'. This loop finds the first
            // index where the element is
            // >= the target without going out of bounds.
            while (low < high) {

                // Find the middle index
                int mid = low + (high - low) / 2;

                // If arr[mid] is less than arr[target], then find in
                // right subarray
                if (arr[mid] < target) {
                    low = mid + 1;
                }

                // If arr[mid] is greater than or equal to target, then it
                // may be the answer. So, instead of high = mid - 1, we do
                // high = mid to include mid in the next search space
                else {
                    high = mid;
                }
            }

            // Return the lower bound index, it could be equal to arr.length
            // if all elements are less than target
            return low;
        }

        public int searchInsertPosition(int[] arr, int target) {
            return lowerBound(arr, target);
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().searchInsertPosition(new int[]{1, 2, 3, 4, 5, 6}, 3));   // 2
        System.out.println(new Solution().searchInsertPosition(new int[]{1, 2, 7, 8, 9, 10}, 3));  // 2
        System.out.println(new Solution().searchInsertPosition(new int[]{1, 2, 7, 9, 10, 11}, 8)); // 3

        // Edge cases
        System.out.println(new Solution().searchInsertPosition(new int[]{}, 5));                    // 0 — empty array
        System.out.println(new Solution().searchInsertPosition(new int[]{5}, 5));                   // 0 — single element match
        System.out.println(new Solution().searchInsertPosition(new int[]{5}, 3));                   // 0 — insert before only element
        System.out.println(new Solution().searchInsertPosition(new int[]{5}, 7));                   // 1 — insert after only element
        System.out.println(new Solution().searchInsertPosition(new int[]{1, 2, 3, 4, 5, 6}, 7));   // 6 — insert at end
    }
}
```


`O(log n)` time, `O(1)` space.

</details>
