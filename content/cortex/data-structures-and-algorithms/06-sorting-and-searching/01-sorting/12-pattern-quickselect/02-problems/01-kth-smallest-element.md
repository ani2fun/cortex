---
title: "Kth Smallest Element"
summary: "Given an array arr and a positive integer k, return the k-th smallest element."
prereqs:
  - 12-pattern-quickselect/01-pattern
difficulty: medium
---

# Kth Smallest Element

The canonical top-K problem. Find the k-th smallest element of an array. The **bounded-size max-heap** is one classical answer; **quickselect** is the other, and it's what we'll implement here — one partition step puts the pivot at its final sorted position, then we recurse on only the half that contains index `k - 1` until the pivot lands there.

---

## The Problem

Given an array `arr` and a positive integer `k`, return the k-th smallest element.

```
Input:  arr = [5, 4, 2, 8], k = 2
Output: 4

Input:  arr = [1, 2, 3, 4, 5], k = 5
Output: 5

Input:  arr = [7, 5, 9], k = 3
Output: 9
```

---

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Run quickselect with `k` interpreted as a 1-based rank, so the target sorted position is index `k - 1`. The `partition` helper picks a random pivot, swaps it to the end, scans `[left, right)` routing every value smaller than the pivot to a sliding `next_smaller_index`, then drops the pivot at that index — its final sorted position. The `quickselect` driver compares the returned pivot index against `k - 1`: equal means we're done, larger means recurse on the left half, smaller means recurse on the right half. Once it returns, `arr[k - 1]` holds the k-th smallest element.

```python run viz=array
import random
from typing import List

class Solution:

    # Function to partition the array based on comparison to pivot
    def partition(self, arr: List[int], left: int, right: int) -> int:

        # Randomly select a pivot index between left and right
        pivot: int = left + random.randint(0, right - left)

        # 1. Get the pivot value
        pivot_value: int = arr[pivot]

        # Move the pivot to the end
        arr[pivot], arr[right] = arr[right], arr[pivot]

        # 2. Move elements around the pivot such that smaller elements
        # come to the left
        next_smaller_index: int = left
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
    ) -> None:
        if left >= right:
            return

        # Partition the array and get the pivot index
        pivot: int = self.partition(arr, left, right)

        # If the pivot is at the k-1th position (in 0-indexed from the
        # right)
        if pivot == k - 1:
            return

        # If pivot is greater than k - 1, search in the left half
        elif pivot > k - 1:
            self.quickselect(arr, left, pivot - 1, k)

        # If k is greater than the pivot index, search in the right half
        else:
            self.quickselect(arr, pivot + 1, right, k)

    def kth_smallest_elements(self, arr: List[int], k: int) -> int:
        n: int = len(arr)

        # Step 1: Perform Quickselect to position smallest k elements
        self.quickselect(arr, 0, n - 1, k)

        # Step 2: Return the k-th smallest element
        return arr[k - 1]


print(Solution().kth_smallest_elements([5, 4, 2, 8], 2))      # 4
print(Solution().kth_smallest_elements([1, 2, 3, 4, 5], 5))   # 5
print(Solution().kth_smallest_elements([7, 5, 9], 3))          # 9
print(Solution().kth_smallest_elements([1], 1))                # 1
print(Solution().kth_smallest_elements([3, 1], 1))             # 1
print(Solution().kth_smallest_elements([3, 1], 2))             # 3
print(Solution().kth_smallest_elements([4, 4, 4], 2))          # 4
print(Solution().kth_smallest_elements([10, 5, 3, 8, 1], 3))   # 5
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private Random rand = new Random();

        // Helper method to swap elements in the array
        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
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
        private void quickselect(int[] arr, int left, int right, int k) {
            if (left >= right) {
                return;
            }

            // Partition the array and get the pivot index
            int pivot = partition(arr, left, right);

            // If the pivot is at the k-1th position (in 0-indexed from the
            // right)
            if (pivot == k - 1) {
                return;
            }

            // If pivot is greater than k - 1, search in the left half
            else if (pivot > k - 1) {
                quickselect(arr, left, pivot - 1, k);
            }

            // If k is greater than the pivot index, search in the right half
            else {
                quickselect(arr, pivot + 1, right, k);
            }
        }

        public int kthSmallestElement(int[] arr, int k) {
            int n = arr.length;

            // Step 1: Perform Quickselect to position smallest k elements
            quickselect(arr, 0, n - 1, k);

            // Step 2: Return the k-th smallest element
            return arr[k - 1];
        }
    }

    public static void main(String[] args) {
        System.out.println(new Solution().kthSmallestElement(new int[]{5, 4, 2, 8}, 2));      // 4
        System.out.println(new Solution().kthSmallestElement(new int[]{1, 2, 3, 4, 5}, 5));   // 5
        System.out.println(new Solution().kthSmallestElement(new int[]{7, 5, 9}, 3));          // 9
        System.out.println(new Solution().kthSmallestElement(new int[]{1}, 1));                // 1
        System.out.println(new Solution().kthSmallestElement(new int[]{3, 1}, 1));             // 1
        System.out.println(new Solution().kthSmallestElement(new int[]{3, 1}, 2));             // 3
        System.out.println(new Solution().kthSmallestElement(new int[]{4, 4, 4}, 2));          // 4
        System.out.println(new Solution().kthSmallestElement(new int[]{10, 5, 3, 8, 1}, 3));   // 5
    }
}
```

### Complexity

| Resource | Cost |
|---|---|
| **Time** | `O(n)` average — each partition is `O(n)` and the search space halves each recursion, summing to `2n`. `O(n²)` worst case on maximally unbalanced pivots. |
| **Space** | `O(1)` for the in-place partition, plus `O(log n)` average for the recursion stack. |

For `k << n`, quickselect's `O(n)` average beats a full `O(n log n)` sort and the `O(n log k)` bounded-heap alternative; the random pivot makes the `O(n²)` worst case practically unreachable. The remaining problems below — Median Finder and K Closest Elements — reuse this same partition-based algorithm with the comparison rule adjusted.

</details>
