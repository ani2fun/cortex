---
title: "Maximum Submatrix Sum"
summary: "Given an n × n matrix (with possibly negative values), find the maximum sum among all submatrices (any size, any position)."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: hard
---

# Maximum Submatrix Sum

## The Problem

Given an `n × n` matrix (with possibly negative values), find the maximum sum among **all** submatrices (any size, any position).

```
Input:  matrix = [[1, 2, 9],
                  [-5, 3, 8],
                  [4, 6, -7]]
Output: 22                         Submatrix excluding the negatives optimally

Input:  matrix = [[1, -2, -3],
                  [-4, -5, -6],
                  [-7, -8, -9]]
Output: 1                          Single cell (0, 0)
```

<details>
<summary><h2>The Approach</h2></summary>


There are `O(n²)` choices of top-left corner and `O(n²)` choices of bottom-right corner — `O(n⁴)` submatrices total. With prefix sums, each is `O(1)` to evaluate. Total: `O(n⁴)`.

(There's a faster `O(n³)` algorithm using Kadane's-on-collapsed-rows, but this lesson sticks to the straightforward prefix-sum quadruple-loop, which makes the pattern's structure transparent.)

> *Pause. Why is `O(n⁴)` already a major win over the naive baseline?*

Without prefix sums, computing each submatrix's sum requires scanning all its cells — up to `O(n²)` per submatrix, total `O(n⁶)`. Prefix sums kill the inner sum loop, dropping the total by a factor of `n²`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=matrix
from typing import List
import sys

class Solution:
    def maximum_submatrix_sum(self, matrix: List[List[int]]) -> int:
        n: int = len(matrix)

        # Create a prefix sum matrix with size (n+1) x (n+1)
        prefix_sum: List[List[int]] = [
            [0] * (n + 1) for _ in range(n + 1)
        ]

        # Compute the prefix sum matrix
        for i in range(1, n + 1):
            for j in range(1, n + 1):

                # Each cell in the prefix sum matrix is the sum of the
                # corresponding submatrix in the original matrix
                prefix_sum[i][j] = (
                    prefix_sum[i - 1][j]
                    + prefix_sum[i][j - 1]
                    - prefix_sum[i - 1][j - 1]
                    + matrix[i - 1][j - 1]
                )

        max_sum: int = -sys.maxsize

        # Iterate over all possible submatrices
        for r1 in range(1, n + 1):
            for c1 in range(1, n + 1):
                for r2 in range(r1, n + 1):
                    for c2 in range(c1, n + 1):

                        # Compute the sum of the submatrix using the
                        # prefix sum matrix
                        sum: int = (
                            prefix_sum[r2][c2]
                            - prefix_sum[r1 - 1][c2]
                            - prefix_sum[r2][c1 - 1]
                            + prefix_sum[r1 - 1][c1 - 1]
                        )

                        # Update the maximum sum if the current sum is
                        # greater
                        max_sum = max(max_sum, sum)

        return max_sum


# Examples from the problem statement
print(Solution().maximum_submatrix_sum([[1,2,9],[-5,3,8],[4,6,-7]]))      # 22
print(Solution().maximum_submatrix_sum([[1,-2,-3],[-4,-5,-6],[-7,-8,-9]])) # 1

# Edge cases
print(Solution().maximum_submatrix_sum([[5]]))                             # 5  — 1x1
print(Solution().maximum_submatrix_sum([[-1]]))                            # -1 — all negative 1x1
print(Solution().maximum_submatrix_sum([[1,2],[3,4]]))                     # 10 — whole 2x2
print(Solution().maximum_submatrix_sum([[-1,-2],[-3,-4]]))                 # -1 — all negative
print(Solution().maximum_submatrix_sum([[1,2,3],[4,5,6],[7,8,9]]))         # 45 — all positive
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class Solution {
        public int maximumSubmatrixSum(int[][] matrix) {
            int n = matrix.length;

            // Create a prefix sum matrix with size (n+1) x (n+1)
            int[][] prefixSum = new int[n + 1][n + 1];

            // Compute the prefix sum matrix
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= n; j++) {

                    // Each cell in the prefix sum matrix is the sum of the
                    // corresponding submatrix in the original matrix
                    prefixSum[i][j] =
                        prefixSum[i - 1][j] +
                        prefixSum[i][j - 1] -
                        prefixSum[i - 1][j - 1] +
                        matrix[i - 1][j - 1];
                }
            }

            int maxSum = Integer.MIN_VALUE;

            // Iterate over all possible submatrices
            for (int r1 = 1; r1 <= n; r1++) {
                for (int c1 = 1; c1 <= n; c1++) {
                    for (int r2 = r1; r2 <= n; r2++) {
                        for (int c2 = c1; c2 <= n; c2++) {

                            // Compute the sum of the submatrix using the
                            // prefix sum matrix
                            int sum =
                                prefixSum[r2][c2] -
                                prefixSum[r1 - 1][c2] -
                                prefixSum[r2][c1 - 1] +
                                prefixSum[r1 - 1][c1 - 1];

                            // Update the maximum sum if the current sum is
                            // greater
                            maxSum = Math.max(maxSum, sum);
                        }
                    }
                }
            }

            return maxSum;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{1,2,9},{-5,3,8},{4,6,-7}}));       // 22
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{1,-2,-3},{-4,-5,-6},{-7,-8,-9}})); // 1

        // Edge cases
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{5}}));                             // 5
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{-1}}));                            // -1
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{1,2},{3,4}}));                     // 10
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{-1,-2},{-3,-4}}));                 // -1
        System.out.println(new Solution().maximumSubmatrixSum(new int[][]{{1,2,3},{4,5,6},{7,8,9}}));         // 45
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n⁴)` for the brute-force enumeration (`O(n³)` possible with Kadane variant) |
| Space | `O(n²)` for prefix table |

</details>
