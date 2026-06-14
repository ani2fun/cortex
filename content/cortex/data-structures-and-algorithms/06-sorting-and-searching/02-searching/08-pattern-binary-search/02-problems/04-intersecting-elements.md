---
title: "Intersecting Elements"
summary: "Given an N × M matrix where each row is sorted ascending, return a sorted list of all elements present in every row."
prereqs:
  - 08-pattern-binary-search/01-pattern
difficulty: hard
kind: problem
topics: [binary-search, searching]
---

# Intersecting Elements

Same setup as the previous problem; collect *all* shared elements instead of just the smallest.

## Problem Statement

Given an `N × M` matrix where each row is sorted ascending, return a sorted list of all elements present in every row.

## Examples

**Example 1**
```
Input:  matrix = [[1, 2, 3, 4], [0, 1, 4, 5]]
Output: [1, 4]
Explanation: 1 and 4 both appear in every row; 2, 3 are absent from row 2; 0, 5 are absent from row 1.
```

**Example 2**
```
Input:  matrix = [[5, 9, 11], [1, 4, 5], [2, 5, 9]]
Output: [5]
Explanation: Only 5 appears in all three rows.
```

## Constraints

- `1 ≤ N, M ≤ 500`
- `-10^9 ≤ matrix[i][j] ≤ 10^9`
- Each row is sorted in strictly ascending order.

```python run viz=array viz-root=result
import ast
from typing import List

class Solution:
    def intersecting_elements(self, matrix: List[List[int]]) -> List[int]:
        # Your code goes here — iterate each element of the first row;
        # binary-search that element in every other row; collect the ones
        # found in all rows into result (they are already in sorted order).
        return []

matrix = ast.literal_eval(input())
print(Solution().intersecting_elements(matrix))
```

```java run viz=array viz-root=result
import java.util.*;

public class Main {
    static class Solution {
        public List<Integer> intersectingElements(int[][] matrix) {
            // Your code goes here — iterate each element of the first row;
            // binary-search that element in every other row; collect the ones
            // found in all rows into result (they are already in sorted order).
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().intersectingElements(matrix));
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

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1, 2, 3, 4], [0, 1, 4, 5]]" }
  ],
  "cases": [
    { "args": { "matrix": "[[1, 2, 3, 4], [0, 1, 4, 5]]" }, "expected": "[1, 4]" },
    { "args": { "matrix": "[[5, 9, 11], [1, 4, 5], [2, 5, 9]]" }, "expected": "[5]" },
    { "args": { "matrix": "[[1, 2, 3, 4], [0, 1, 4, 5], [6, 7, 8]]" }, "expected": "[]" },
    { "args": { "matrix": "[[1, 2, 3]]" }, "expected": "[1, 2, 3]" },
    { "args": { "matrix": "[[5], [5], [5]]" }, "expected": "[5]" },
    { "args": { "matrix": "[[5], [6]]" }, "expected": "[]" },
    { "args": { "matrix": "[[1, 2, 3], [1, 2, 3]]" }, "expected": "[1, 2, 3]" }
  ]
}
```

<details>
<summary><h2>Intuition</h2></summary>

Iterate over every element in the first row (which is sorted ascending, so we visit candidates in order). For each candidate, binary-search every other row to check membership in `O(log M)`. Collecting all hits produces the intersection already in ascending order because the first row is sorted. This is the "all matches" extension of the minimum-shared-element pattern — replace the early return with an accumulator.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

Same as the previous problem but accumulate matches into a result list instead of returning the first.

```python solution time=O(M * N * log M) space=O(M)
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


matrix = ast.literal_eval(input())
print(Solution().intersecting_elements(matrix))
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
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().intersectingElements(matrix));
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

Same as the previous problem: `O(M · N · log M)` time, `O(M)` space for the result.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The binary search pattern: when the input is sorted (in any monotone direction), `O(log n)` lookup beats linear scan. Recognise it by the sorted structure plus a "find this thing" question. The four problems show four flavours: single-array lookup, descending sort, multi-row matrix membership, and intersection extraction — all using binary search as the inner primitive.

The next lesson lifts the lookup to **lower bound** problems — same pattern, but the question is "where does this go?" rather than "is it there?" That's the right tool for insertion-position queries, "first occurrence of," and many real-world database operations.

**Transfer challenge — try before the Lower Bound Pattern lesson:** Given two sorted arrays `A` and `B`, return their intersection (elements in both). Use binary search to make it `O(min(N, M) · log max(N, M))`. Hint: iterate over the shorter, binary-search in the longer.

</details>
