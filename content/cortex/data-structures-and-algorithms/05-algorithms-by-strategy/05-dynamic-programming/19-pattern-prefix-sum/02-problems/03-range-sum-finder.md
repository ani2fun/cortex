---
title: "Range Sum Finder"
summary: "Build a class RangeSumFinder that takes a 2D matrix in its constructor and supports O(1) rectangle-sum queries afterward."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: medium
kind: problem
topics: [prefix-sum, dynamic-programming]
---

# Range Sum Finder

## The Problem

Build a class `RangeSumFinder` that takes a 2D matrix in its constructor and supports `O(1)` rectangle-sum queries afterward.

```
Operations: [RangeSumFinder, sumRegion, sumRegion, sumRegion]
Matrix:     [[1, 2, 3],
             [4, 5, 6],
             [7, 8, 9]]
Queries:    [[], [1, 1, 1, 1], [0, 0, 2, 2], [1, 1, 2, 2]]

Output:     [null, 5, 45, 28]
```

The interface is the standard `(row1, col1, row2, col2)` rectangle. The contract: each query *must* be `O(1)` after construction.

## Examples

**Example 1**
```
Input:  matrix = [[1,2,3],[4,5,6],[7,8,9]], row1=1, col1=1, row2=1, col2=1
Output: 5
Explanation: Single cell (1,1) = 5.
```

**Example 2**
```
Input:  matrix = [[1,2,3],[4,5,6],[7,8,9]], row1=0, col1=0, row2=2, col2=2
Output: 45
Explanation: The whole 3×3 matrix sums to 45.
```

## Constraints

- `1 ≤ rows, cols ≤ 200`
- `0 ≤ row1 ≤ row2 < rows`, `0 ≤ col1 ≤ col2 < cols`
- Matrix values can be negative.
- `sumRegion` must run in `O(1)` after construction.

```python run viz=grid viz-root=matrix
import ast

class RangeSumFinder:
    def __init__(self, matrix):
        # Your code goes here — build a prefix sum table (rows+1)×(cols+1)
        # so every sumRegion query is O(1) using four-corner inclusion-exclusion.
        pass

    def sum_region(self, row1, col1, row2, col2):
        # Your code goes here — return prefix_sum[row2+1][col2+1]
        # - prefix_sum[row1][col2+1] - prefix_sum[row2+1][col1] + prefix_sum[row1][col1]
        return 0

matrix = ast.literal_eval(input())
row1 = int(input())
col1 = int(input())
row2 = int(input())
col2 = int(input())
rsf = RangeSumFinder(matrix)
print(rsf.sum_region(row1, col1, row2, col2))
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class RangeSumFinder {
        // Your code goes here — store a prefix sum table built in the constructor.
        public RangeSumFinder(int[][] matrix) {
        }

        public int sumRegion(int row1, int col1, int row2, int col2) {
            return 0;
        }
    }

    // "[1, 2, 3], [4, 5, 6]]" → int[][] — reads a 2-D matrix from one stdin line
    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        int row1 = Integer.parseInt(sc.nextLine().trim());
        int col1 = Integer.parseInt(sc.nextLine().trim());
        int row2 = Integer.parseInt(sc.nextLine().trim());
        int col2 = Integer.parseInt(sc.nextLine().trim());
        RangeSumFinder rsf = new RangeSumFinder(matrix);
        System.out.println(rsf.sumRegion(row1, col1, row2, col2));
    }
}
```

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1,2,3],[4,5,6],[7,8,9]]" },
    { "id": "row1", "label": "row1", "type": "int", "placeholder": "1" },
    { "id": "col1", "label": "col1", "type": "int", "placeholder": "1" },
    { "id": "row2", "label": "row2", "type": "int", "placeholder": "1" },
    { "id": "col2", "label": "col2", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]", "row1": "1", "col1": "1", "row2": "1", "col2": "1" }, "expected": "5" },
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]", "row1": "0", "col1": "0", "row2": "2", "col2": "2" }, "expected": "45" },
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]", "row1": "1", "col1": "1", "row2": "2", "col2": "2" }, "expected": "28" },
    { "args": { "matrix": "[[1]]", "row1": "0", "col1": "0", "row2": "0", "col2": "0" }, "expected": "1" },
    { "args": { "matrix": "[[-1,-2],[-3,-4]]", "row1": "0", "col1": "0", "row2": "1", "col2": "1" }, "expected": "-10" },
    { "args": { "matrix": "[[1,2],[3,4]]", "row1": "0", "col1": "0", "row2": "0", "col2": "0" }, "expected": "1" },
    { "args": { "matrix": "[[1,2],[3,4]]", "row1": "1", "col1": "1", "row2": "1", "col2": "1" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>The Approach</h2></summary>


Build a prefix-sum table once in `O(n × m)`. Each `sumRegion` is then a four-term inclusion-exclusion subtraction — `O(1)`.

> *Pause. Why is this important? Why not just compute each query on the fly?*

Because the query rate dominates the cost. If `k` queries each take `O(n × m)` work, total is `O(k × n × m)`. With prefix sums, `O(n × m + k)` — almost `n × m` cheaper for large `k`. Many real systems answer billions of range queries per day; the constant-time per-query is critical.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n × m) construction, O(1) query space=O(n × m)
import ast
from typing import List

class RangeSumFinder:
    def __init__(self, matrix: List[List[int]]):

        # Number of rows in the matrix
        rows: int = len(matrix)

        # Number of columns in the matrix
        cols: int = len(matrix[0])

        # Create a new matrix with dimensions (rows+1) x (cols+1) and
        # initialize all elements to 0
        self.prefix_sum: List[List[int]] = [
            [0] * (cols + 1) for _ in range(rows + 1)
        ]

        # Fill in the values of the new matrix using dynamic programming
        for i in range(1, rows + 1):
            for j in range(1, cols + 1):

                # The value at prefix_sum[i][j] is the sum of the current
                # element in matrix, the value above it, the value to its
                # left, and the value in the top-left corner, all
                # subtracted by the value in the top-left corner (to
                # avoid double counting)
                self.prefix_sum[i][j] = (
                    matrix[i - 1][j - 1]
                    + self.prefix_sum[i - 1][j]
                    + self.prefix_sum[i][j - 1]
                    - self.prefix_sum[i - 1][j - 1]
                )

    # Method to calculate the sum of elements within a given rectangle
    def sum_region(
        self, row1: int, col1: int, row2: int, col2: int
    ) -> int:
        return (
            self.prefix_sum[row2 + 1][col2 + 1]
            - self.prefix_sum[row1][col2 + 1]
            - self.prefix_sum[row2 + 1][col1]
            + self.prefix_sum[row1][col1]
        )

matrix = ast.literal_eval(input())
row1 = int(input())
col1 = int(input())
row2 = int(input())
col2 = int(input())
rsf = RangeSumFinder(matrix)
print(rsf.sum_region(row1, col1, row2, col2))
```

```java solution
import java.util.*;

public class Main {
    static class RangeSumFinder {
        private int[][] prefixSum;

        // Constructor
        public RangeSumFinder(int[][] matrix) {

            // Number of rows in the matrix
            int rows = matrix.length;

            // Number of columns in the matrix
            int cols = matrix[0].length;

            // Create a new matrix with dimensions (rows+1) x (cols+1) and
            // initialize all elements to 0
            prefixSum = new int[rows + 1][cols + 1];

            // Fill in the values of the new matrix using dynamic programming
            for (int i = 1; i <= rows; i++) {
                for (int j = 1; j <= cols; j++) {

                    // The value at prefixSum[i][j] is the sum of the current
                    // element in matrix, the value above it, the value to
                    // its left, and the value in the top-left corner, all
                    // subtracted by the value in the top-left corner (to
                    // avoid double counting)
                    prefixSum[i][j] =
                        matrix[i - 1][j - 1] +
                        prefixSum[i - 1][j] +
                        prefixSum[i][j - 1] -
                        prefixSum[i - 1][j - 1];
                }
            }
        }

        // Method to calculate the sum of elements within a given rectangle
        public int sumRegion(int row1, int col1, int row2, int col2) {
            return (
                prefixSum[row2 + 1][col2 + 1] -
                prefixSum[row1][col2 + 1] -
                prefixSum[row2 + 1][col1] +
                prefixSum[row1][col1]
            );
        }
    }

    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        int row1 = Integer.parseInt(sc.nextLine().trim());
        int col1 = Integer.parseInt(sc.nextLine().trim());
        int row2 = Integer.parseInt(sc.nextLine().trim());
        int col2 = Integer.parseInt(sc.nextLine().trim());
        RangeSumFinder rsf = new RangeSumFinder(matrix);
        System.out.println(rsf.sumRegion(row1, col1, row2, col2));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Construction | `O(n × m)` |
| Each query | `O(1)` |
| Space | `O(n × m)` |

</details>
