---
title: "Maximum Submatrix Sum"
summary: "Given an n × n matrix (with possibly negative values), find the maximum sum among all submatrices (any size, any position)."
prereqs:
  - 19-pattern-prefix-sum/01-pattern
difficulty: hard
kind: problem
topics: [prefix-sum, dynamic-programming]
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

## Examples

**Example 1**
```
Input:  matrix = [[1, 2, 9], [-5, 3, 8], [4, 6, -7]]
Output: 22
Explanation: The 2×3 submatrix rows 0–1, cols 0–2 gives 1+2+9+(-5)+3+8 = 18;
             the 2×2 submatrix rows 0–1, cols 1–2 gives 2+9+3+8 = 22.
             Prefix sums enumerate all O(n⁴) candidates in O(1) each.
```

**Example 2**
```
Input:  matrix = [[1, -2, -3], [-4, -5, -6], [-7, -8, -9]]
Output: 1
Explanation: Every submatrix that includes more than cell (0,0) has a lower sum
             due to the surrounding negatives.
```

## Constraints

- `1 ≤ n ≤ 100`
- Matrix values can be negative.

```python run viz=grid viz-root=matrix
import ast

class Solution:
    def maximum_submatrix_sum(self, matrix):
        # Your code goes here — build a 2-D prefix sum table (size (n+1)×(n+1)),
        # iterate all pairs (r1,c1)..(r2,c2) using 1-indexed prefix, track max.
        return 0

matrix = ast.literal_eval(input())
print(Solution().maximum_submatrix_sum(matrix))
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class Solution {
        public int maximumSubmatrixSum(int[][] matrix) {
            // Your code goes here — build a 2-D prefix sum table (size (n+1)×(n+1)),
            // iterate all pairs (r1,c1)..(r2,c2) using 1-indexed prefix, track max.
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
        System.out.println(new Solution().maximumSubmatrixSum(matrix));
    }
}
```

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1,2,9],[-5,3,8],[4,6,-7]]" }
  ],
  "cases": [
    { "args": { "matrix": "[[1,2,9],[-5,3,8],[4,6,-7]]" }, "expected": "22" },
    { "args": { "matrix": "[[1,-2,-3],[-4,-5,-6],[-7,-8,-9]]" }, "expected": "1" },
    { "args": { "matrix": "[[5]]" }, "expected": "5" },
    { "args": { "matrix": "[[-1]]" }, "expected": "-1" },
    { "args": { "matrix": "[[1,2],[3,4]]" }, "expected": "10" },
    { "args": { "matrix": "[[-1,-2],[-3,-4]]" }, "expected": "-1" },
    { "args": { "matrix": "[[1,2,3],[4,5,6],[7,8,9]]" }, "expected": "45" }
  ]
}
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

```python solution time=O(n⁴) space=O(n²)
import ast
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

matrix = ast.literal_eval(input())
print(Solution().maximum_submatrix_sum(matrix))
```

```java solution
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
        System.out.println(new Solution().maximumSubmatrixSum(matrix));
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(n⁴)` for the brute-force enumeration (`O(n³)` possible with Kadane variant) |
| Space | `O(n²)` for prefix table |

</details>
