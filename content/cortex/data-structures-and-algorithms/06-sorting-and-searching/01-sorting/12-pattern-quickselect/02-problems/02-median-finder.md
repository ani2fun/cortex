---
title: "Median Finder"
summary: "Return the median of arr. For odd-length arrays, the middle element. For even-length arrays, the floor of the two middle elements' average."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
---

# Median Finder

The median is the middle element. For odd `n`, it's the `(n/2 + 1)`-th smallest. For even `n`, it's the *floor* of the average of the two middles. Either way, it's a quickselect problem.

---

## The Problem

Return the median of `arr`. For odd-length arrays, the middle element. For even-length arrays, the floor of the two middle elements' average.

```
Input:  arr = [5, 4, 2, 8, 9]
Output: 5      (sorted: [2, 4, 5, 8, 9], middle = 5)

Input:  arr = [5, 8, 1, 2]
Output: 3      (sorted: [1, 2, 5, 8], middle two avg = 3.5 → floor = 3)

Input:  arr = [-3, -4]
Output: -3     ((-3 + -4) // 2 = -3 — floor division of negatives rounds toward -∞ in some languages; here we use truncation toward zero)
```

---

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

The trick: for odd `n`, one quickselect call. For even `n`, two calls — one for `n/2 - 1`, one for `n/2`. Take the floor of their average.

```python run viz=array
import random
from typing import List

class Solution:

    # Function to partition the array based on comparison to pivot
    def partition(self, arr: List[int], left: int, right: int) -> int:

        # Randomly select a pivot index between left and right
        pivot = left + random.randint(0, right - left)

        # 1. Get the pivot value
        pivot_value = arr[pivot]

        # Move the pivot to the end
        arr[pivot], arr[right] = arr[right], arr[pivot]

        # 2. Move elements around the pivot such that smaller elements
        # come to the left
        next_smaller_index = left
        for i in range(left, right):

            # Elements smaller than pivot come to left
            if arr[i] < pivot_value:
                arr[next_smaller_index], arr[i] = (
                    arr[i],
                    arr[next_smaller_index],
                )
                next_smaller_index += 1

        # 3. Move pivot to its final position
        arr[next_smaller_index], arr[right] = (
            arr[right],
            arr[next_smaller_index],
        )

        # next_smaller_index is now the final index of the pivot_value
        return next_smaller_index

    # Quickselect to find the Kth smallest element
    def quickselect(
        self, arr: List[int], left: int, right: int, k: int
    ) -> int:
        if left >= right:
            return arr[left]

        # Partition the array and get the pivot index
        pivot = self.partition(arr, left, right)

        # If the pivot is at the k-th position (0-indexed),
        # we've found the k-th smallest element and can return it.
        # Note: We are **not using k-1** here because k is already treated
        # as a 0-based index in this implementation.
        # In other words, k = 0 corresponds to the smallest element,
        # k = 1 to the second smallest, and so on.
        if pivot == k:
            return arr[pivot]

        # If the pivot's index is greater than k,
        # the k-th smallest element must be in the left partition
        elif pivot > k:
            return self.quickselect(arr, left, pivot - 1, k)

        # If k is greater than the pivot index, search in the right half
        else:
            return self.quickselect(arr, pivot + 1, right, k)

    def find_median(self, arr: List[int]) -> int:
        n = len(arr)

        # If odd, return the middle element
        if n % 2 == 1:
            return self.quickselect(arr, 0, n - 1, n // 2)

        # If even, take the average of the two middle elements and round
        # up
        left_mid = self.quickselect(arr, 0, n - 1, n // 2 - 1)
        right_mid = self.quickselect(arr, 0, n - 1, n // 2)

        # Round down the average of the two middle elements
        return int((left_mid + right_mid) / 2)


print(Solution().find_median([5, 4, 2, 8, 9]))    # 5
print(Solution().find_median([5, 8, 1, 2]))        # 3
print(Solution().find_median([-3, -4]))            # -3
print(Solution().find_median([1]))                 # 1
print(Solution().find_median([1, 2]))              # 1
print(Solution().find_median([3, 1, 4, 1, 5]))    # 3
print(Solution().find_median([10, 20, 30, 40]))   # 25 -> truncated to 25
print(Solution().find_median([7, 7, 7, 7, 7]))    # 7
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        private void swap(int[] arr, int i, int j) {
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }

        // Function to partition the array based on comparison to pivot
        private int partition(int[] arr, int left, int right) {

            // Randomly select a pivot index between left and right
            int pivot = left + rand.nextInt(right - left + 1);

            // 1. Get the pivot value
            int pivotValue = arr[pivot];

            // Move the pivot to the end
            swap(arr, pivot, right);

            // 2. Move elements around the pivot such that smaller elements
            // come to the left
            int nextSmallerIndex = left;
            for (int i = left; i < right; i++) {

                // Elements smaller than pivot come to left
                if (arr[i] < pivotValue) {
                    swap(arr, nextSmallerIndex, i);
                    nextSmallerIndex++;
                }
            }

            // 3. Move pivot to its final position
            swap(arr, nextSmallerIndex, right);

            // nextSmallerIndex is now the final index of the pivotValue
            return nextSmallerIndex;
        }

        // Quickselect to find the Kth smallest element
        private int quickselect(int[] arr, int left, int right, int k) {
            if (left >= right) {
                return arr[left];
            }

            // Partition the array and get the pivot index
            int pivot = partition(arr, left, right);

            // If the pivot is at the k-th position (0-indexed),
            // we've found the k-th smallest element and can return it.
            // Note: We are **not using k-1** here because k is already
            // treated as a 0-based index in this implementation. In other
            // words, k = 0 corresponds to the smallest element, k = 1 to the
            // second smallest, and so on.
            if (pivot == k) {
                return arr[pivot];
            }

            // If the pivot's index is greater than k,
            // the k-th smallest element must be in the left partition
            else if (pivot > k) {
                return quickselect(arr, left, pivot - 1, k);
            }

            // If k is greater than the pivot index, search in the right half
            else {
                return quickselect(arr, pivot + 1, right, k);
            }
        }

        public int findMedian(int[] arr) {
            int n = arr.length;

            // If odd, return the middle element
            if (n % 2 == 1) {
                return quickselect(arr, 0, n - 1, n / 2);
            }

            // If even, take the average of the two middle elements and round
            // up
            int leftMid = quickselect(arr, 0, n - 1, n / 2 - 1);
            int rightMid = quickselect(arr, 0, n - 1, n / 2);

            // Round down the average of the two middle elements
            return (leftMid + rightMid) / 2;
        }
    }

    public static void main(String[] args) {
        System.out.println(new Solution().findMedian(new int[]{5, 4, 2, 8, 9}));    // 5
        System.out.println(new Solution().findMedian(new int[]{5, 8, 1, 2}));        // 3
        System.out.println(new Solution().findMedian(new int[]{-3, -4}));            // -3
        System.out.println(new Solution().findMedian(new int[]{1}));                 // 1
        System.out.println(new Solution().findMedian(new int[]{1, 2}));              // 1
        System.out.println(new Solution().findMedian(new int[]{3, 1, 4, 1, 5}));    // 3
        System.out.println(new Solution().findMedian(new int[]{10, 20, 30, 40}));   // 25
        System.out.println(new Solution().findMedian(new int[]{7, 7, 7, 7, 7}));    // 7
    }
}
```

The implementation has three pieces: a Lomuto-style `partition` that picks a random pivot, scans `[left, right)` and routes values smaller than the pivot to a sliding `next_smaller_index`, then drops the pivot at its final position; a `quickselect` driver that recurses on whichever side of the pivot contains the target index; and the `find_median` wrapper that chooses how many calls to make based on parity. The recursion treats `k` as a 0-based index throughout — that's why the base case checks `pivot == k` instead of `pivot == k - 1`. For even-length arrays the floor-of-average is computed as `int((left_mid + right_mid) / 2)`, which truncates toward zero (matching the `Input: [-3, -4] → -3` example above).

### Complexity

| Resource | Cost |
|---|---|
| **Time** | `O(n)` average for both odd-n (one call) and even-n (two calls). |
| **Space (stack)** | `O(log n)` average. |

</details>
