---
title: "Reverse Binary Search"
summary: "Given a descending-sorted array arr and target, return target's index, or -1."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
---

# Reverse Binary Search

Same algorithm with one comparison flipped: works on descending-sorted arrays.

## The Problem

Given a descending-sorted array `arr` and `target`, return target's index, or `-1`.

```
Input:  arr = [6, 5, 4, 3, 2, 1], target = 3
Output: 3

Input:  arr = [6, 5, 4, 3, 2, 1], target = 6
Output: 0

Input:  arr = [6, 5, 4, 3, 2, 1], target = 10
Output: -1
```

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

The skeleton is identical to plain binary search — only the comparison logic flips. In ascending order: `arr[mid] < target` means "look right." In descending order: `arr[mid] < target` means "look *left*" (because larger values are on the left).


```python run viz=array
from typing import List

class Solution:
    def reverse_binary_search(self, arr: List[int], target: int) -> int:

        # Starting index of the search range (leftmost element)
        low = 0

        # Ending index of the search range (rightmost element)
        high = len(arr) - 1

        while low <= high:

            # Calculate the middle index to avoid potential overflow
            mid = low + (high - low) // 2

            # Found the target, return the index
            if arr[mid] == target:
                return mid

            # Since the array is sorted in descending order:
            # If arr[mid] is smaller than the target,
            # move to the left half (where larger elements are)
            elif arr[mid] < target:
                high = mid - 1

            # Else if arr[mid] is greater than the target,
            # move to the right half (where smaller elements are)
            else:
                low = mid + 1

        # Target not found in the array
        return -1


# Examples from the problem statement
print(Solution().reverse_binary_search([6, 5, 4, 3, 2, 1], 3))   # 3
print(Solution().reverse_binary_search([6, 5, 4, 3, 2, 1], 6))   # 0
print(Solution().reverse_binary_search([6, 5, 4, 3, 2, 1], 10))  # -1

# Edge cases
print(Solution().reverse_binary_search([], 3))                    # -1 — empty array
print(Solution().reverse_binary_search([5], 5))                   # 0  — single element present
print(Solution().reverse_binary_search([5], 3))                   # -1 — single element absent
print(Solution().reverse_binary_search([6, 5, 4, 3, 2, 1], 1))   # 5  — target at last index
print(Solution().reverse_binary_search([5, 3], 3))                # 1  — two elements, last
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int reverseBinarySearch(int[] arr, int target) {

            // Starting index of the search range (leftmost element)
            int low = 0;

            // Ending index of the search range (rightmost element)
            int high = arr.length - 1;

            while (low <= high) {

                // Calculate the middle index to avoid potential overflow
                int mid = low + (high - low) / 2;

                // Found the target, return the index
                if (arr[mid] == target) {
                    return mid;
                }

                // Since the array is sorted in descending order:
                // If arr[mid] is smaller than the target,
                // move to the left half (where larger elements are)
                else if (arr[mid] < target) {
                    high = mid - 1;
                }

                // Else if arr[mid] is greater than the target,
                // move to the right half (where smaller elements are)
                else {
                    low = mid + 1;
                }
            }

            // Target not found in the array
            return -1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().reverseBinarySearch(new int[]{6, 5, 4, 3, 2, 1}, 3));   // 3
        System.out.println(new Solution().reverseBinarySearch(new int[]{6, 5, 4, 3, 2, 1}, 6));   // 0
        System.out.println(new Solution().reverseBinarySearch(new int[]{6, 5, 4, 3, 2, 1}, 10));  // -1

        // Edge cases
        System.out.println(new Solution().reverseBinarySearch(new int[]{}, 3));                    // -1 — empty array
        System.out.println(new Solution().reverseBinarySearch(new int[]{5}, 5));                   // 0  — single element present
        System.out.println(new Solution().reverseBinarySearch(new int[]{5}, 3));                   // -1 — single element absent
        System.out.println(new Solution().reverseBinarySearch(new int[]{6, 5, 4, 3, 2, 1}, 1));   // 5  — target at last index
        System.out.println(new Solution().reverseBinarySearch(new int[]{5, 3}, 3));                // 1  — two elements, last
    }
}
```

### Complexity

`O(log n)` time, `O(1)` space.

</details>
