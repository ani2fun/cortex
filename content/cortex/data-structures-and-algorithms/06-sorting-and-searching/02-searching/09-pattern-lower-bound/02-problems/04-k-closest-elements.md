---
title: "K Closest Elements"
summary: "Given a sorted array, integer k, and integer target, return the k closest elements to target, sorted ascending. Ties broken by smaller value first."
prereqs:
  - 09-pattern-lower-bound/01-pattern
difficulty: medium
---

# K Closest Elements

Lower bound anchors a sliding window that expands outward.

## The Problem

Given a sorted array, integer `k`, and integer `target`, return the `k` closest elements to target, sorted ascending. Ties broken by smaller value first.

```
Input:  arr = [1, 2, 3, 4, 5, 6], k = 3, target = 4
Output: [3, 4, 5]

Input:  arr = [1, 4, 5, 6, 7, 8], k = 3, target = 4
Output: [4, 5, 6]

Input:  arr = [1, 5, 8, 10, 12, 13], k = 3, target = 10
Output: [8, 10, 12]
```

<details>
<summary><h2>The Solution</h2></summary>


`lower_bound(target)` gives the first index `>= target`; that splits the array into a left side (all `< target`) and a right side (all `>= target`). Set `right` to that index and `left` to `right - 1` — the two candidates straddling the target. Expand the window outward `k` times: on each step, take whichever of `arr[left]` / `arr[right]` is closer to `target` (ties broken by the smaller value, i.e. the `left` side), stepping that pointer outward; if one side has run off the array, take the other. The `k` elements collected between the final pointers — `arr[left + 1 : right]` — are the answer, already sorted ascending.


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

    def k_closest_elements(
        self, arr: List[int], k: int, target: int
    ) -> List[int]:
        if not arr or k <= 0:
            return []

        right = self.lower_bound(arr, target)
        left = right - 1

        # Expand the window to the left and right
        remaining = k
        while remaining > 0:
            remaining -= 1

            # If left pointer is out of bounds,
            # move the right pointer
            if left < 0:
                right += 1

            # If right pointer is out of bounds,
            # move the left pointer
            elif right >= len(arr):
                left -= 1

            # If the element at left pointer is closer to target,
            # move the left pointer
            elif target - arr[left] <= arr[right] - target:
                left -= 1

            # Else, if the element at right pointer is closer to target,
            # move the right pointer
            else:
                right += 1

        # Return the k closest elements collected between left and right
        # pointers
        return arr[left + 1: right]


# Examples from the problem statement
print(Solution().k_closest_elements([1, 2, 3, 4, 5, 6], 3, 4))     # [3, 4, 5]
print(Solution().k_closest_elements([1, 4, 5, 6, 7, 8], 3, 4))     # [4, 5, 6]
print(Solution().k_closest_elements([1, 5, 8, 10, 12, 13], 3, 10)) # [8, 10, 12]

# Edge cases
print(Solution().k_closest_elements([], 3, 4))                      # [] — empty array
print(Solution().k_closest_elements([1, 2, 3, 4, 5, 6], 0, 4))     # [] — k = 0
print(Solution().k_closest_elements([5], 1, 5))                     # [5] — single element match
print(Solution().k_closest_elements([1, 2, 3, 4, 5, 6], 6, 4))     # [1, 2, 3, 4, 5, 6] — k = all
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

        public int[] kClosestElements(int[] arr, int k, int target) {
            if (arr.length == 0 || k <= 0) {
                return new int[0];
            }

            int right = lowerBound(arr, target);
            int left = right - 1;

            // Expand the window to the left and right
            while (k-- > 0) {

                // If left pointer is out of bounds,
                // move the right pointer
                if (left < 0) {
                    right++;
                }

                // If right pointer is out of bounds,
                // move the left pointer
                else if (right >= arr.length) {
                    left--;
                }

                // If the element at left pointer is closer to target,
                // move the left pointer
                else if (target - arr[left] <= arr[right] - target) {
                    left--;
                }

                // Else, if the element at right pointer is closer to target,
                // move the right pointer
                else {
                    right++;
                }
            }

            // Return the k closest elements collected between left and right
            // pointers
            int[] result = new int[right - left - 1];
            for (int i = left + 1, j = 0; i < right; i++, j++) {
                result[j] = arr[i];
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{1, 2, 3, 4, 5, 6}, 3, 4)));     // [3, 4, 5]
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{1, 4, 5, 6, 7, 8}, 3, 4)));     // [4, 5, 6]
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{1, 5, 8, 10, 12, 13}, 3, 10))); // [8, 10, 12]

        // Edge cases
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{}, 3, 4)));                      // [] — empty array
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{1, 2, 3, 4, 5, 6}, 0, 4)));     // [] — k = 0
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{5}, 1, 5)));                     // [5] — single element match
        System.out.println(Arrays.toString(new Solution().kClosestElements(new int[]{1, 2, 3, 4, 5, 6}, 6, 4)));     // [1, 2, 3, 4, 5, 6] — k = all
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Lower bound is the single primitive behind a family of "where should this go?" queries. The four problems showed insertion position, range queries, closest neighbour, and k-closest. Each adds a small post-processing step to the same `O(log n)` lower bound query.

The next lesson (the Upper Bound Pattern lesson) handles the dual — **upper bound pattern** for problems that require the *first index strictly greater than target*.

**Transfer challenge — try before the Upper Bound Pattern lesson:** Use lower bound to count how many elements in a sorted array are *strictly less than* target. (Hint: that's the lower bound's return value.)

</details>
