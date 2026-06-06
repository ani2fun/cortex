---
title: "Intersecting Elements"
summary: "Given an N × M matrix where each row is sorted ascending, return a sorted list of all elements present in every row."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: hard
---

# Intersecting Elements

Same setup as the previous problem; collect *all* shared elements instead of just the smallest.

## The Problem

Given an `N × M` matrix where each row is sorted ascending, return a sorted list of all elements present in every row.

```
Input:  matrix = [[1, 2, 3, 4], [0, 1, 4, 5]]
Output: [1, 4]

Input:  matrix = [[5, 9, 11], [1, 4, 5], [2, 5, 9]]
Output: [5]

Input:  matrix = [[1, 2, 3, 4], [0, 1, 4, 5], [6, 7, 8]]
Output: []
```

<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Same as the previous problem but accumulate matches into a result list instead of returning the first.


```python run viz=array viz-root=result
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

    def intersecting_elements(
        self, matrix: List[List[int]]
    ) -> List[int]:
        rows: int = len(matrix)

        # If the matrix has no rows, return an empty result
        if rows == 0:
            return []

        cols: int = len(matrix[0])
        result: List[int] = []

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

            # If target is found in all rows, add it to the result
            if found:
                result.append(target)

        # Return the intersection of elements present in all rows
        return result


# Examples from the problem statement
print(Solution().intersecting_elements([[1, 2, 3, 4], [0, 1, 4, 5]]))           # [1, 4]
print(Solution().intersecting_elements([[5, 9, 11], [1, 4, 5], [2, 5, 9]]))     # [5]
print(Solution().intersecting_elements([[1, 2, 3, 4], [0, 1, 4, 5], [6, 7, 8]]))  # []

# Edge cases
print(Solution().intersecting_elements([[1, 2, 3]]))                              # [1, 2, 3] — single row
print(Solution().intersecting_elements([[5], [5], [5]]))                          # [5] — single-col match
print(Solution().intersecting_elements([[5], [6]]))                               # [] — single-col no match
print(Solution().intersecting_elements([[1, 2, 3], [1, 2, 3]]))                   # [1, 2, 3] — identical rows
```

```java run viz=array viz-root=result
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

        public List<Integer> intersectingElements(int[][] matrix) {
            int rows = matrix.length;

            // If the matrix has no rows, return an empty result
            if (rows == 0) {
                return new ArrayList<>();
            }

            int cols = matrix[0].length;
            List<Integer> result = new ArrayList<>();

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

                // If target is found in all rows, add it to the result
                if (found) {
                    result.add(target);
                }
            }

            // Return the intersection of elements present in all rows
            return result;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().intersectingElements(new int[][]{{1, 2, 3, 4}, {0, 1, 4, 5}}));           // [1, 4]
        System.out.println(new Solution().intersectingElements(new int[][]{{5, 9, 11}, {1, 4, 5}, {2, 5, 9}}));     // [5]
        System.out.println(new Solution().intersectingElements(new int[][]{{1, 2, 3, 4}, {0, 1, 4, 5}, {6, 7, 8}}));  // []

        // Edge cases
        System.out.println(new Solution().intersectingElements(new int[][]{{1, 2, 3}}));                              // [1, 2, 3] — single row
        System.out.println(new Solution().intersectingElements(new int[][]{{5}, {5}, {5}}));                          // [5] — single-col match
        System.out.println(new Solution().intersectingElements(new int[][]{{5}, {6}}));                               // [] — single-col no match
        System.out.println(new Solution().intersectingElements(new int[][]{{1, 2, 3}, {1, 2, 3}}));                   // [1, 2, 3] — identical rows
    }
}
```

### Complexity

Same as the previous problem: `O(M · N · log M)` time, `O(M)` space for the result.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The binary search pattern: when the input is sorted (in any monotone direction), `O(log n)` lookup beats linear scan. Recognise it by the sorted structure plus a "find this thing" question. The four problems show four flavours: single-array lookup, descending sort, multi-row matrix membership, and intersection extraction — all using binary search as the inner primitive.

The next lesson lifts the lookup to **lower bound** problems — same pattern, but the question is "where does this go?" rather than "is it there?" That's the right tool for insertion-position queries, "first occurrence of," and many real-world database operations.

**Transfer challenge — try before the Lower Bound Pattern lesson:** Given two sorted arrays `A` and `B`, return their intersection (elements in both). Use binary search to make it `O(min(N, M) · log max(N, M))`. Hint: iterate over the shorter, binary-search in the longer.

</details>
