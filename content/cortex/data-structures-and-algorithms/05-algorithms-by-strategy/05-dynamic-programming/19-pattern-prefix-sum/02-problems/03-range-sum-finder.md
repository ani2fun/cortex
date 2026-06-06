---
title: "Range Sum Finder"
summary: "Build a class RangeSumFinder that takes a 2D matrix in its constructor and supports O(1) rectangle-sum queries afterward."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: medium
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

<details>
<summary><h2>The Approach</h2></summary>


Build a prefix-sum table once in `O(n × m)`. Each `sumRegion` is then a four-term inclusion-exclusion subtraction — `O(1)`.

> *Pause. Why is this important? Why not just compute each query on the fly?*

Because the query rate dominates the cost. If `k` queries each take `O(n × m)` work, total is `O(k × n × m)`. With prefix sums, `O(n × m + k)` — almost `n × m` cheaper for large `k`. Many real systems answer billions of range queries per day; the constant-time per-query is critical.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=matrix
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

        # The sum of the rectangle is calculated using the values in the
        # new matrix prefixSum The formula is: sum =
        # prefixSum[row2+1][col2+1] - prefixSum[row1][col2 + 1] -
        # prefixSum[row2 + 1][col1] + prefixSum[row1][col1] It subtracts
        # the sum of elements above and to the left of the rectangle,
        # and adds back the sum of the elements above and to the left of
        # the rectangle, to avoid double subtraction
        return (
            self.prefix_sum[row2 + 1][col2 + 1]
            - self.prefix_sum[row1][col2 + 1]
            - self.prefix_sum[row2 + 1][col1]
            + self.prefix_sum[row1][col1]
        )


# Example from the problem statement
rsf = RangeSumFinder([[1,2,3],[4,5,6],[7,8,9]])
print(rsf.sum_region(1, 1, 1, 1))  # 5
print(rsf.sum_region(0, 0, 2, 2))  # 45
print(rsf.sum_region(1, 1, 2, 2))  # 28

# Edge cases
rsf2 = RangeSumFinder([[1]])
print(rsf2.sum_region(0, 0, 0, 0)) # 1  — single cell

rsf3 = RangeSumFinder([[1,2],[3,4]])
print(rsf3.sum_region(0, 0, 0, 0)) # 1  — top-left cell
print(rsf3.sum_region(1, 1, 1, 1)) # 4  — bottom-right cell
print(rsf3.sum_region(0, 0, 1, 1)) # 10 — whole matrix

rsf4 = RangeSumFinder([[-1,-2],[-3,-4]])
print(rsf4.sum_region(0, 0, 1, 1)) # -10 — negative values
```

```java run viz=grid viz-root=matrix
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

            // The sum of the rectangle is calculated using the values in the
            // new matrix prefixSum The formula is: sum =
            // prefixSum[row2+1][col2+1] - prefixSum[row1][col2 + 1] -
            // prefixSum[row2 + 1][col1] + prefixSum[row1][col1] It subtracts
            // the sum of elements above and to the left of the rectangle,
            // and adds back the sum of the elements above and to the left of
            // the rectangle, to avoid double subtraction
            return (
                prefixSum[row2 + 1][col2 + 1] -
                prefixSum[row1][col2 + 1] -
                prefixSum[row2 + 1][col1] +
                prefixSum[row1][col1]
            );
        }
    }

    public static void main(String[] args) {
        // Example from the problem statement
        RangeSumFinder rsf = new RangeSumFinder(new int[][]{{1,2,3},{4,5,6},{7,8,9}});
        System.out.println(rsf.sumRegion(1, 1, 1, 1));  // 5
        System.out.println(rsf.sumRegion(0, 0, 2, 2));  // 45
        System.out.println(rsf.sumRegion(1, 1, 2, 2));  // 28

        // Edge cases
        RangeSumFinder rsf2 = new RangeSumFinder(new int[][]{{1}});
        System.out.println(rsf2.sumRegion(0, 0, 0, 0)); // 1

        RangeSumFinder rsf3 = new RangeSumFinder(new int[][]{{1,2},{3,4}});
        System.out.println(rsf3.sumRegion(0, 0, 0, 0)); // 1
        System.out.println(rsf3.sumRegion(1, 1, 1, 1)); // 4
        System.out.println(rsf3.sumRegion(0, 0, 1, 1)); // 10

        RangeSumFinder rsf4 = new RangeSumFinder(new int[][]{{-1,-2},{-3,-4}});
        System.out.println(rsf4.sumRegion(0, 0, 1, 1)); // -10
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
