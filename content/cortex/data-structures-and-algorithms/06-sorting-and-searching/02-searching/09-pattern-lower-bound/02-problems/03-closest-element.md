---
title: "Closest Element"
summary: "Given a sorted array and target, return the closest element. Ties broken by smaller value."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: medium
---

# Closest Element

Use lower bound to find the threshold position; the answer is either at the threshold or just before it.

## The Problem

Given a sorted array and target, return the closest element. Ties broken by smaller value.

```
Input:  arr = [1, 2, 3, 4, 5, 6], target = 4
Output: 4

Input:  arr = [2, 4, 6, 8, 10, 12], target = 5
Output: 4   (4 and 6 are equidistant; smaller wins)

Input:  arr = [1, 10], target = 7
Output: 10
```

<details>
<summary><h2>The Solution</h2></summary>


`lower_bound(target)` gives the smallest index `i` with `arr[i] >= target`. The closest element is either `arr[i]` or `arr[i - 1]` — compare distances.


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

    def closest_element(self, arr: List[int], target: int) -> int:

        # Return -1 if the array is empty
        if not arr:
            return -1

        lower_bound_index = self.lower_bound(arr, target)

        # If lower bound index is 0, return the first element
        if lower_bound_index == 0:
            return arr[0]

        # If lower bound index is equal to the size of the array,
        # return the last element
        elif lower_bound_index == len(arr):
            return arr[-1]

        # Else, return the element which is closest to the target
        # among the two closest elements
        else:

            # Get the element strictly less than target
            lower_element = arr[lower_bound_index - 1]

            # Get the element greater than or equal to target
            upper_element = arr[lower_bound_index]

            # Return the closest element
            if target - lower_element <= upper_element - target:
                return lower_element
            else:
                return upper_element


# Examples from the problem statement
print(Solution().closest_element([1, 2, 3, 4, 5, 6], 4))    # 4
print(Solution().closest_element([2, 4, 6, 8, 10, 12], 5))  # 4
print(Solution().closest_element([1, 10], 7))                # 10

# Edge cases
print(Solution().closest_element([], 5))                     # -1 — empty array
print(Solution().closest_element([5], 5))                    # 5  — single element match
print(Solution().closest_element([5], 1))                    # 5  — single element, target before
print(Solution().closest_element([5], 9))                    # 5  — single element, target after
print(Solution().closest_element([1, 2, 3, 4, 5, 6], 0))    # 1  — target before all elements
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

        public int closestElement(int[] arr, int target) {

            // Return -1 if the array is empty
            if (arr.length == 0) {
                return -1;
            }

            int lowerBoundIndex = lowerBound(arr, target);

            // If lower bound index is 0, return the first element
            if (lowerBoundIndex == 0) {
                return arr[0];
            }

            // If lower bound index is equal to the size of the array,
            // return the last element
            else if (lowerBoundIndex == arr.length) {
                return arr[arr.length - 1];
            }

            // Else, return the element which is closest to the target
            // among the two closest elements
            else {

                // Get the element strictly less than target
                int lowerElement = arr[lowerBoundIndex - 1];

                // Get the element greater than or equal to target
                int upperElement = arr[lowerBoundIndex];

                // Return the closest element
                if (target - lowerElement <= upperElement - target) {
                    return lowerElement;
                } else {
                    return upperElement;
                }
            }
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().closestElement(new int[]{1, 2, 3, 4, 5, 6}, 4));    // 4
        System.out.println(new Solution().closestElement(new int[]{2, 4, 6, 8, 10, 12}, 5));  // 4
        System.out.println(new Solution().closestElement(new int[]{1, 10}, 7));                // 10

        // Edge cases
        System.out.println(new Solution().closestElement(new int[]{}, 5));                     // -1 — empty array
        System.out.println(new Solution().closestElement(new int[]{5}, 5));                    // 5  — single element match
        System.out.println(new Solution().closestElement(new int[]{5}, 1));                    // 5  — single element, target before
        System.out.println(new Solution().closestElement(new int[]{5}, 9));                    // 5  — single element, target after
        System.out.println(new Solution().closestElement(new int[]{1, 2, 3, 4, 5, 6}, 0));    // 1  — target before all elements
    }
}
```

</details>
