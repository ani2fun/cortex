---
title: "Minimum Shared Element"
summary: "Given an N × M matrix where each row is sorted ascending, return the smallest element present in all rows. Return -1 if no such element exists. Must run in O(N log M)."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: medium
kind: problem
topics: [binary-search, searching]
---

# Minimum Shared Element

## Problem Statement

Given an `N × M` matrix where each row is sorted ascending, return the smallest element present in all rows. Return `-1` if no such element exists. **Must run in `O(N log M)`.**

## Examples

**Example 1**
```
Input:  matrix = [[2, 3, 4], [1, 3, 5], [1, 2, 3]]
Output: 3
Explanation: Iterating the first row left-to-right: 2 is absent from row 2; 3 is found in every row — it is the smallest common element.
```

**Example 2**
```
Input:  matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
Output: -1
Explanation: No element appears in all three rows; the disjoint ranges share nothing.
```

## Constraints

- `1 ≤ N, M ≤ 500`
- `-10^9 ≤ matrix[i][j] ≤ 10^9`
- Each row is sorted in strictly ascending order.

```python run viz=array
import ast
from typing import List

class Solution:
    def minimum_shared_element(self, matrix: List[List[int]]) -> int:
        # Your code goes here — iterate over the first row left-to-right;
        # for each candidate, binary-search every other row. Return the
        # first element found in all rows, or -1.
        return -1

matrix = ast.literal_eval(input())
print(Solution().minimum_shared_element(matrix))
```

```java run viz=array
import java.util.*;

public class Main {
    static class Solution {
        public int minimumSharedElement(int[][] matrix) {
            // Your code goes here — iterate over the first row left-to-right;
            // for each candidate, binary-search every other row. Return the
            // first element found in all rows, or -1.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumSharedElement(matrix));
    }

    static int[][] parseIntMatrix(String line) {
        line = line.trim();
        // Strip outer brackets: [[1,2],[3,4]] → [1,2],[3,4]
        line = line.substring(1, line.length() - 1).trim();
        if (line.isEmpty()) return new int[0][];
        List<int[]> rows = new ArrayList<>();
        // Split on "]," to find row boundaries
        int depth = 0, start = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    rows.add(parseIntArray(line.substring(start, i + 1)));
                    start = i + 2; // skip ","
                }
            }
        }
        return rows.toArray(new int[0][]);
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[2, 3, 4], [1, 3, 5], [1, 2, 3]]" }
  ],
  "cases": [
    { "args": { "matrix": "[[1, 2, 3]]" }, "expected": "1" },
    { "args": { "matrix": "[[2, 3, 4], [1, 3, 5], [1, 2, 3]]" }, "expected": "3" },
    { "args": { "matrix": "[[1, 2, 3], [4, 5, 6], [7, 8, 9]]" }, "expected": "-1" },
    { "args": { "matrix": "[[5], [5], [5]]" }, "expected": "5" },
    { "args": { "matrix": "[[5], [6]]" }, "expected": "-1" },
    { "args": { "matrix": "[[1, 2, 3], [1, 2, 3]]" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

The key insight is that the first row is sorted ascending, so iterating it left-to-right checks candidates in ascending order — the first element that appears in every other row is automatically the smallest shared element. For each candidate, the other rows are also sorted, so you can binary-search them in `O(log M)` rather than scanning in `O(M)`. The outer loop over the first row's `M` elements combined with binary search over the remaining `N-1` rows gives `O(M · N · log M)`, but since we stop at the first match this is `O(N log M)` in the best case.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Iterate over the elements of the first row (left to right, ascending). For each, binary-search every other row. The first element that's found in all rows is the answer (smallest because the first row is sorted).


```python solution time=O(M * N * log M) space=O(1)
import ast
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


matrix = ast.literal_eval(input())
print(Solution().minimum_shared_element(matrix))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumSharedElement(matrix));
    }

    static int[][] parseIntMatrix(String line) {
        line = line.trim();
        line = line.substring(1, line.length() - 1).trim();
        if (line.isEmpty()) return new int[0][];
        List<int[]> rows = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    rows.add(parseIntArray(line.substring(start, i + 1)));
                    start = i + 2;
                }
            }
        }
        return rows.toArray(new int[0][]);
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

### Complexity

`O(M · N · log M)` where `N = rows`, `M = cols`. The outer loop iterates over the first row (`M` elements); for each, we binary-search the remaining `N - 1` rows in `O(log M)` each.

</details>
