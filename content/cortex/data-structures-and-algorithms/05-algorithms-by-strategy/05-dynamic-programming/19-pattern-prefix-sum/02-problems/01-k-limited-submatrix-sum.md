---
title: "K-Limited Submatrix Sum"
summary: "Given an n × m matrix and an integer k, find the maximum sum among all k × k submatrices."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: medium
kind: problem
topics: [prefix-sum, dynamic-programming]
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

## Examples

**Example 1**
```
Input:  matrix = [[1, 2, 9], [5, 3, 8], [4, 6, 7]], k = 2
Output: 24
Explanation: The 2×2 submatrix at rows 1–2, cols 1–2 gives 3 + 8 + 6 + 7 = 24,
             which is the largest among all four 2×2 windows.
```

**Example 2**
```
Input:  matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]], k = 3
Output: 45
Explanation: k = n = m = 3, so the only 3×3 submatrix is the whole matrix: sum = 45.
```

## Constraints

- `1 ≤ k ≤ min(n, m)`
- Matrix values can be negative.

```python run viz=grid viz-root=matrix
import ast

class Solution:
    def k_limited_submatrix_sum(self, matrix, k):
        # Your code goes here — build a 2-D prefix sum table, then slide
        # every k×k window using the four-corner formula and track the max.
        return 0

matrix = ast.literal_eval(input())
k = int(input())
print(Solution().k_limited_submatrix_sum(matrix, k))
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class Solution {
        public int kLimitedSubmatrixSum(int[][] matrix, int k) {
            // Your code goes here — build a 2-D prefix sum table, then slide
            // every k×k window using the four-corner formula and track the max.
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
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kLimitedSubmatrixSum(matrix, k));
    }
}
```

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1,2,9],[5,3,8],[4,6,7]]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "matrix": "[[1,2,9],[5,3,8],[4,6,7]]", "k": "2" }, "expected": "24" },
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]", "k": "3" }, "expected": "45" },
    { "args": { "matrix": "[[5]]", "k": "1" }, "expected": "5" },
    { "args": { "matrix": "[[1,2],[3,4]]", "k": "1" }, "expected": "4" },
    { "args": { "matrix": "[[1,2],[3,4]]", "k": "2" }, "expected": "10" },
    { "args": { "matrix": "[[-1,-2],[-3,-4]]", "k": "1" }, "expected": "-1" },
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]", "k": "2" }, "expected": "28" }
  ]
}
```

<details>
<summary><h2>The Approach</h2></summary>


Naively: enumerate every `k × k` submatrix (there are `(n-k+1) × (m-k+1)` of them), and sum each in `O(k²)` — total `O(n × m × k²)`. With prefix sums precomputed in `O(n × m)`, each submatrix sum is `O(1)` — total drops to `O(n × m)`.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n × m) space=O(n × m)
import ast
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

matrix = ast.literal_eval(input())
k = int(input())
print(Solution().k_limited_submatrix_sum(matrix, k))
```

```java solution
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
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().kLimitedSubmatrixSum(matrix, k));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n × m)` — `O(n × m)` precompute + `O((n-k+1) × (m-k+1))` queries |
| Space | `O(n × m)` for the prefix table |

</details>
