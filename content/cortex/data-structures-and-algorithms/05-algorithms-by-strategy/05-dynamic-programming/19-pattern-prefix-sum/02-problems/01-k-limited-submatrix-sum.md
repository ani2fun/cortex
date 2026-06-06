---
title: "K-Limited Submatrix Sum"
summary: "Given an n × m matrix and an integer k, find the maximum sum among all k × k submatrices."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: medium
---

# K-Limited Submatrix Sum

## The Problem

Given an `n × m` matrix and an integer `k`, find the maximum sum among all `k × k` submatrices.

```
Input:  matrix = [[1, 2, 9],
                  [5, 3, 8],
                  [4, 6, 7]],
        k = 2
Output: 24                         Submatrix at (1,1)-(2,2): 3 + 8 + 6 + 7 = 24

Input:  matrix = [[1, 2, 3],
                  [4, 5, 6],
                  [7, 8, 9]],
        k = 3
Output: 45                         The whole matrix is the only k × k submatrix
```

<details>
<summary><h2>The Approach</h2></summary>


Naively: enumerate every `k × k` submatrix (there are `(n-k+1) × (m-k+1)` of them), and sum each in `O(k²)` — total `O(n × m × k²)`. With prefix sums precomputed in `O(n × m)`, each submatrix sum is `O(1)` — total drops to `O(n × m)`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=matrix
from typing import List
import sys

class Solution:
    def k_limited_submatrix_sum(
        self, matrix: List[List[int]], k: int
    ) -> int:
        rows: int = len(matrix)
        cols: int = len(matrix[0])

        # Precompute the prefix sum matrix
        prefix_sum: List[List[int]] = [
            [0] * (cols + 1) for _ in range(rows + 1)
        ]
        for i in range(1, rows + 1):
            for j in range(1, cols + 1):

                # Calculate the sum of values in the submatrix (0,0) to
                # (i-1,j-1) and store it in prefix_sum[i][j]
                prefix_sum[i][j] = (
                    prefix_sum[i - 1][j]
                    + prefix_sum[i][j - 1]
                    - prefix_sum[i - 1][j - 1]
                    + matrix[i - 1][j - 1]
                )

        max_sum: int = -sys.maxsize
        max_sum_submatrix: List[List[int]] = [[0] * k for _ in range(k)]

        # Find the maximum sum submatrix
        for i in range(rows - k + 1):
            for j in range(cols - k + 1):

                # Calculate the sum of values in the submatrix (i,j) to
                # (i+k-1,j+k-1)
                sum_ = (
                    prefix_sum[i + k][j + k]
                    - prefix_sum[i][j + k]
                    - prefix_sum[i + k][j]
                    + prefix_sum[i][j]
                )

                if sum_ > max_sum:
                    max_sum = sum_
        return max_sum


# Examples from the problem statement
print(Solution().k_limited_submatrix_sum([[1,2,9],[5,3,8],[4,6,7]], 2))   # 24
print(Solution().k_limited_submatrix_sum([[1,2,3],[4,5,6],[7,8,9]], 3))   # 45

# Edge cases
print(Solution().k_limited_submatrix_sum([[5]], 1))                        # 5  — 1x1 matrix, k=1
print(Solution().k_limited_submatrix_sum([[1,2],[3,4]], 1))                # 4  — k=1, max element
print(Solution().k_limited_submatrix_sum([[1,2],[3,4]], 2))                # 10 — whole 2x2
print(Solution().k_limited_submatrix_sum([[-1,-2],[-3,-4]], 1))            # -1 — negative matrix
print(Solution().k_limited_submatrix_sum([[1,2,3],[4,5,6],[7,8,9]], 2))   # 28 — bottom-right 2x2
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class Solution {
        public int kLimitedSubmatrixSum(int[][] matrix, int k) {
            int rows = matrix.length;
            int cols = matrix[0].length;

            // Precompute the prefix sum matrix
            int[][] prefixSum = new int[rows + 1][cols + 1];
            for (int i = 1; i <= rows; i++) {
                for (int j = 1; j <= cols; j++) {

                    // Calculate the sum of values in the submatrix (0,0) to
                    // (i-1,j-1) and store it in prefixSum[i][j]
                    prefixSum[i][j] =
                        prefixSum[i - 1][j] +
                        prefixSum[i][j - 1] -
                        prefixSum[i - 1][j - 1] +
                        matrix[i - 1][j - 1];
                }
            }

            int maxSum = Integer.MIN_VALUE;
            int[][] maxSumSubmatrix = new int[k][k];

            // Find the maximum sum submatrix
            for (int i = 0; i <= rows - k; i++) {
                for (int j = 0; j <= cols - k; j++) {

                    // Calculate the sum of values in the submatrix (i,j) to
                    // (i+k-1,j+k-1)
                    int sum =
                        prefixSum[i + k][j + k] -
                        prefixSum[i][j + k] -
                        prefixSum[i + k][j] +
                        prefixSum[i][j];

                    if (sum > maxSum) {
                        maxSum = sum;
                    }
                }
            }

            return maxSum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{1,2,9},{5,3,8},{4,6,7}}, 2));   // 24
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{1,2,3},{4,5,6},{7,8,9}}, 3));   // 45

        // Edge cases
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{5}}, 1));                        // 5
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{1,2},{3,4}}, 1));                // 4
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{1,2},{3,4}}, 2));                // 10
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{-1,-2},{-3,-4}}, 1));            // -1
        System.out.println(new Solution().kLimitedSubmatrixSum(new int[][]{{1,2,3},{4,5,6},{7,8,9}}, 2));   // 28
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n × m)` — `O(n × m)` precompute + `O((n-k+1) × (m-k+1))` queries |
| Space | `O(n × m)` for the prefix table |

</details>
