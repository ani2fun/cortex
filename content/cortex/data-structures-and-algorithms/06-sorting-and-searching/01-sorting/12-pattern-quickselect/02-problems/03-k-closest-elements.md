---
title: "K Closest Elements"
summary: "Given an array arr, an integer k, and a target target, return the k closest elements to target. Closeness is measured by |x - target|; ties broken by smaller value first."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
---

# K Closest Elements

Quickselect's partition step compares against a pivot. Change *what* you compare and you can find the k-th most-anything: closest to a target, brightest, oldest, etc.

---

## The Problem

Given an array `arr`, an integer `k`, and a target `target`, return the `k` closest elements to `target`. Closeness is measured by `|x - target|`; ties broken by smaller value first.

```
Input:  arr = [1, 2, 3, 4, 5, 6], k = 3, target = 4
Output: [4, 3, 5]

Input:  arr = [1, 4, 5, 6, 7, 8], k = 4, target = 3
Output: [4, 1, 5, 6]

Input:  arr = [1, 5, 8, 10, 12, 13], k = 3, target = 10
Output: [10, 8, 12]
```

---

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Replace the partition's "compare elements directly" with "compare distance-to-target." Everything else is identical.

```python run viz=array
import random
from typing import List

class Solution:

    # Function to partition the array based on the absolute difference
    # to the target
    def partition(
        self, arr: List[int], left: int, right: int, target: int
    ) -> int:

        # Randomly select a pivot index between left and right
        pivot = left + random.randint(0, right - left)

        # 1. Get the pivot value and its absolute difference to the
        # target
        pivot_val = arr[pivot]
        pivot_diff = abs(pivot_val - target)

        # Move the pivot to the end and update the index
        arr[pivot], arr[right] = arr[right], arr[pivot]

        # 2. Move elements around the pivot such that closer elements
        # come to the left
        next_closest_index = left
        for i in range(left, right):

            # If the current element is closer to the target than the
            # pivot element, swap it with the element at
            # next_closest_index
            if abs(arr[i] - target) < pivot_diff or (
                abs(arr[i] - target) == pivot_diff and arr[i] < pivot_val
            ):
                arr[next_closest_index], arr[i] = (
                    arr[i],
                    arr[next_closest_index],
                )
                next_closest_index += 1

        # 3. Move pivot to its final position
        arr[next_closest_index], arr[right] = (
            arr[right],
            arr[next_closest_index],
        )
        return next_closest_index

    # Quickselect to find the k closest elements
    def quickselect(
        self, arr: List[int], left: int, right: int, k: int, target: int
    ) -> None:
        if left == right:
            return

        # Partition the array and get the pivot index
        pivot = self.partition(arr, left, right, target)

        # If the pivot is at the k-th position (in 0-indexed)
        if k - 1 == pivot:
            return

        # If pivot is greater than k - 1, search in the left half
        elif pivot > k - 1:
            self.quickselect(arr, left, pivot - 1, k, target)

        # If k is greater than the pivot index, search in the right half
        else:
            self.quickselect(arr, pivot + 1, right, k, target)

    def k_closest_elements(
        self, arr: List[int], k: int, target: int
    ) -> List[int]:

        # Step 1: Perform Quickselect to find the k closest elements
        self.quickselect(arr, 0, len(arr) - 1, k, target)

        # Step 2: The first k elements will be the closest elements
        return arr[:k]


print(sorted(Solution().k_closest_elements([1, 2, 3, 4, 5, 6], 3, 4)))     # [3, 4, 5]
print(sorted(Solution().k_closest_elements([1, 4, 5, 6, 7, 8], 4, 3)))     # [1, 4, 5, 6]
print(sorted(Solution().k_closest_elements([1, 5, 8, 10, 12, 13], 3, 10))) # [8, 10, 12]
print(sorted(Solution().k_closest_elements([1], 1, 5)))                    # [1]
print(sorted(Solution().k_closest_elements([1, 2], 1, 2)))                 # [2]
print(sorted(Solution().k_closest_elements([1, 10, 20], 2, 15)))           # [10, 20]
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {

        // Helper method to swap elements in the array
        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }

        // Function to partition the array based on the absolute difference
        // to the target
        private int partition(int[] arr, int left, int right, int target) {

            // Randomly select a pivot index between left and right
            Random rand = new Random();
            int pivot = left + rand.nextInt(right - left + 1);

            // 1. Get the pivot value and its absolute difference to the
            // target
            int pivotVal = arr[pivot];
            int pivotDiff = Math.abs(pivotVal - target);

            // Move the pivot to the end and update the index
            swap(arr, pivot, right);

            // 2. Move elements around the pivot such that closer elements
            // come to the left
            int nextClosestIndex = left;
            for (int i = left; i < right; i++) {

                // If the current element is closer to the target than the
                // pivot element, swap it with the element at
                // nextClosestIndex
                if (
                    Math.abs(arr[i] - target) < pivotDiff ||
                    (Math.abs(arr[i] - target) == pivotDiff &&
                        arr[i] < pivotVal)
                ) {
                    swap(arr, nextClosestIndex, i);
                    nextClosestIndex++;
                }
            }

            // 3. Move pivot to its final position
            swap(arr, nextClosestIndex, right);
            return nextClosestIndex;
        }

        // Quickselect to find the k closest elements
        private void quickselect(
            int[] arr,
            int left,
            int right,
            int k,
            int target
        ) {
            if (left == right) {
                return;
            }

            // Partition the array and get the pivot index
            int pivot = partition(arr, left, right, target);

            // If the pivot is at the k-th position (in 0-indexed)
            if (k - 1 == pivot) {
                return;
            }

            // If pivot is greater than k - 1, search in the left half
            else if (pivot > k - 1) {
                quickselect(arr, left, pivot - 1, k, target);
            }

            // If k is greater than the pivot index, search in the right half
            else {
                quickselect(arr, pivot + 1, right, k, target);
            }
        }

        // Function to return the k closest elements as an array
        public int[] kClosestElements(int[] arr, int k, int target) {

            // Step 1: Perform Quickselect to find the k closest elements
            quickselect(arr, 0, arr.length - 1, k, target);

            // Step 2: Create a new array to store the closest elements
            return Arrays.copyOfRange(arr, 0, k);
        }
    }

    public static void main(String[] args) {
        int[] r1 = new Solution().kClosestElements(new int[]{1, 2, 3, 4, 5, 6}, 3, 4);
        Arrays.sort(r1); System.out.println(Arrays.toString(r1));    // [3, 4, 5]

        int[] r2 = new Solution().kClosestElements(new int[]{1, 4, 5, 6, 7, 8}, 4, 3);
        Arrays.sort(r2); System.out.println(Arrays.toString(r2));    // [1, 4, 5, 6]

        int[] r3 = new Solution().kClosestElements(new int[]{1, 5, 8, 10, 12, 13}, 3, 10);
        Arrays.sort(r3); System.out.println(Arrays.toString(r3));    // [8, 10, 12]

        int[] r4 = new Solution().kClosestElements(new int[]{1}, 1, 5);
        Arrays.sort(r4); System.out.println(Arrays.toString(r4));    // [1]

        int[] r5 = new Solution().kClosestElements(new int[]{1, 2}, 1, 2);
        Arrays.sort(r5); System.out.println(Arrays.toString(r5));    // [2]

        int[] r6 = new Solution().kClosestElements(new int[]{1, 10, 20}, 2, 15);
        Arrays.sort(r6); System.out.println(Arrays.toString(r6));    // [10, 20]
    }
}
```

The partition compares with the score-tuple `(|x - target|, x)` instead of the raw value `x`: an element wins the swap if its distance to the target is strictly less than the pivot's, or — on a tie — its value is strictly less than the pivot's. The structure of `quickselect` and the recursive driver is unchanged from the basic version; once `pivot == k - 1`, the first `k` slots of `arr` hold the k closest elements (in arbitrary order).

### Complexity

`O(n)` average — same as basic quickselect. Computing `abs(arr[i] - target)` is `O(1)`, so the partition is still linear.

</details>
