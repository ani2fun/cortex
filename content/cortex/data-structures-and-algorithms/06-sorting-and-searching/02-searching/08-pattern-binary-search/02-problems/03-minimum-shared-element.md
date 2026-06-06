---
title: "Minimum Shared Element"
summary: "Given an N × M matrix where each row is sorted ascending, return the smallest element present in all rows. Return -1 if no such element exists. Must run in O(N log M)."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
---

# Minimum Shared Element

Multi-row sorted matrix. Find the smallest element that appears in *every* row.

## The Problem

Given an `N × M` matrix where each row is sorted ascending, return the smallest element present in all rows. Return `-1` if no such element exists. **Must run in `O(N log M)`.**

```
Input:  matrix = [[1, 2, 3]]
Output: 1   (only one row; smallest element is 1)

Input:  matrix = [[2, 3, 4], [1, 3, 5], [1, 2, 3]]
Output: 3   (only 3 is in every row; rows have [2,3,4], [1,3,5], [1,2,3])

Input:  matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
Output: -1   (no shared element)
```

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Iterate over the elements of the first row (left to right, ascending). For each, binary-search every other row. The first element that's found in all rows is the answer (smallest because the first row is sorted).


```python run viz=array
from typing import List

class Solution:
    def binary_search(self, arr: List[int], target: int) -> int:

        # Starting index of the search range
        low: int = 0

        # Ending index of the search range
        high: int = len(arr) - 1

        while low <= high:

            # Calculate the middle index
            mid: int = (low + high) // 2

            # Found the target, return the index
            if arr[mid] == target:
                return mid

            # If the arr[mid] is less than the target, adjust the search
            # range to the right half
            if arr[mid] < target:
                low = mid + 1

            # Else if the arr[mid] is greater than the target, adjust
            # the search range to the left half
            else:
                high = mid - 1

        # Target not found in the array
        return -1

    def minimum_shared_element(self, matrix: List[List[int]]) -> int:
        rows: int = len(matrix)

        # If the matrix has no rows, return -1
        if rows == 0:
            return -1

        cols: int = len(matrix[0])

        # Iterate through the columns of the matrix
        for col in range(cols):
            target: int = matrix[0][col]
            found: bool = True

            # Check if the target element is present in all rows
            for row in range(1, rows):

                # Use binary search to check if target is present in this
                # row
                if self.binary_search(matrix[row], target) == -1:

                    # Target not found in this row, break out of the loop
                    found = False
                    break

            # If target is found in all rows, it is the smallest common
            # element
            if found:
                return target

        # No common element found in all rows
        return -1


# Examples from the problem statement
print(Solution().minimum_shared_element([[1, 2, 3]]))                         # 1
print(Solution().minimum_shared_element([[2, 3, 4], [1, 3, 5], [1, 2, 3]]))  # 3
print(Solution().minimum_shared_element([[1, 2, 3], [4, 5, 6], [7, 8, 9]]))  # -1

# Edge cases
print(Solution().minimum_shared_element([[]]))                                # -1 — empty row
print(Solution().minimum_shared_element([[5], [5], [5]]))                     # 5  — single-col all same
print(Solution().minimum_shared_element([[5], [6]]))                          # -1 — single-col no match
print(Solution().minimum_shared_element([[1, 2, 3], [1, 2, 3]]))              # 1  — identical rows
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        private int binarySearch(int[] arr, int target) {

            // Starting index of the search range
            int low = 0;

            // Ending index of the search range
            int high = arr.length - 1;

            while (low <= high) {

                // Calculate the middle index
                int mid = (low + high) / 2;

                // Found the target, return the index
                if (arr[mid] == target) {
                    return mid;
                }

                // If the arr[mid] is less than the target, adjust the search
                // range to the right half
                else if (arr[mid] < target) {
                    low = mid + 1;
                }

                // Else if the arr[mid] is greater than the target, adjust
                // the search range to the left half
                else {
                    high = mid - 1;
                }
            }

            // Target not found in the array
            return -1;
        }

        public int minimumSharedElement(int[][] matrix) {
            int rows = matrix.length;

            // If the matrix has no rows, return -1
            if (rows == 0) {
                return -1;
            }

            int cols = matrix[0].length;

            // Iterate through the columns of the matrix
            for (int col = 0; col < cols; col++) {
                int target = matrix[0][col];
                boolean found = true;

                // Check if the target element is present in all rows
                for (int row = 1; row < rows; row++) {

                    // Use binary search to check if target is present in
                    // this row
                    if (binarySearch(matrix[row], target) == -1) {

                        // Target not found in this row, break out of the
                        // loop
                        found = false;
                        break;
                    }
                }

                // If target is found in all rows, it is the smallest common
                // element
                if (found) {
                    return target;
                }
            }

            // No common element found in all rows
            return -1;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().minimumSharedElement(new int[][]{{1, 2, 3}}));                               // 1
        System.out.println(new Solution().minimumSharedElement(new int[][]{{2, 3, 4}, {1, 3, 5}, {1, 2, 3}}));        // 3
        System.out.println(new Solution().minimumSharedElement(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));        // -1

        // Edge cases
        System.out.println(new Solution().minimumSharedElement(new int[][]{{5}, {5}, {5}}));                           // 5  — single-col all same
        System.out.println(new Solution().minimumSharedElement(new int[][]{{5}, {6}}));                                // -1 — single-col no match
        System.out.println(new Solution().minimumSharedElement(new int[][]{{1, 2, 3}, {1, 2, 3}}));                    // 1  — identical rows
    }
}
```

### Complexity

`O(M · N · log M)` where `N = rows`, `M = cols`. The outer loop iterates over the first row (`M` elements); for each, we binary-search the remaining `N - 1` rows in `O(log M)` each.

</details>
