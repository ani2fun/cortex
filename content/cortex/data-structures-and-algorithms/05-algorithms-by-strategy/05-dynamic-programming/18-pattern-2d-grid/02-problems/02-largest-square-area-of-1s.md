---
title: "Largest Square Area of 1s"
summary: "Given a binary matrix of 0s and 1s, find the area of the largest *axis-aligned square* of 1s."
prereqs:
  - 18-pattern-2d-grid/01-pattern
difficulty: medium
kind: problem
topics: [2d-grid, dynamic-programming]
---

# Largest Square Area of 1s

## The Problem

Given a binary matrix of 0s and 1s, find the area of the largest *axis-aligned square* of 1s.

```
Input:  grid = [[1, 1, 0, 0],
                [0, 0, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 4                       2 × 2 square at rows 1-2, cols 2-3

Input:  grid = [[1, 1, 0, 0],
                [0, 1, 1, 1],
                [1, 1, 1, 1],
                [1, 0, 0, 0]]
Output: 4                       Multiple 2 × 2 squares
```

---

## Examples

**Example 1**
```
Input:  matrix = [[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]
Output: 4
Explanation: The 2×2 square at rows 1-2, cols 2-3 is the largest; area = 2×2 = 4.
```

**Example 2**
```
Input:  matrix = [[1, 1], [1, 1]]
Output: 4
Explanation: The entire 2×2 grid is a square of 1s; area = 2×2 = 4.
```

```quiz
{
  "prompt": "Why is the aggregator min(top, left, diagonal) + 1 rather than max?",
  "options": [
    "min is faster to compute",
    "min enforces that all three predecessor squares are fully filled — the weakest link caps the new square",
    "max would double-count cells",
    "diagonal is always the smallest"
  ],
  "answer": "min enforces that all three predecessor squares are fully filled — the weakest link caps the new square"
}
```

## Constraints

- `1 ≤ rows, cols ≤ 300`
- `matrix[i][j]` is `0` or `1`.

```python run viz=grid
import ast

class Solution:
    def largest_square_area(self, matrix):
        # Your code goes here — dp[r][c] = side of largest square whose
        # bottom-right corner is (r, c). Recurrence:
        # dp[r][c] = min(dp[r-1][c-1], dp[r-1][c], dp[r][c-1]) + 1
        # if matrix[r][c] == 1, else 0. Return max_side^2.
        return 0

matrix = ast.literal_eval(input())   # the test case's matrix
print(Solution().largest_square_area(matrix))
```

```java run viz=grid
import java.util.*;

public class Main {
    static class Solution {
        public int largestSquareArea(int[][] matrix) {
            // Your code goes here — dp[r][c] = side of largest square whose
            // bottom-right corner is (r, c). Recurrence:
            // dp[r][c] = min(dp[r-1][c-1], dp[r-1][c], dp[r][c-1]) + 1
            // if matrix[r][c] == 1, else 0. Return maxSide^2.
            return 0;
        }
    }

    public static void main(String[] args) {
        int[][] matrix = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(new Solution().largestSquareArea(matrix));
    }

    // "[[1, 0], [1, 1]]" → int[][] — reads the test case's matrix
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
}
```

```testcases
{
  "args": [
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1, 1, 0], [0, 1, 1]]" }
  ],
  "cases": [
    { "args": { "matrix": "[[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }, "expected": "4" },
    { "args": { "matrix": "[[1, 1, 0, 0], [0, 1, 1, 1], [1, 1, 1, 1], [1, 0, 0, 0]]" }, "expected": "4" },
    { "args": { "matrix": "[[1]]" }, "expected": "1" },
    { "args": { "matrix": "[[0]]" }, "expected": "0" },
    { "args": { "matrix": "[[0, 0], [0, 0]]" }, "expected": "0" },
    { "args": { "matrix": "[[1, 1], [1, 1]]" }, "expected": "4" },
    { "args": { "matrix": "[[1, 0, 1], [0, 1, 0], [1, 0, 1]]" }, "expected": "1" }
  ]
}
```

<details>
<summary><h2>The Recurrence — Three-Neighbour Min Plus One</h2></summary>


`dp[r][c]` = side length of the largest square *whose bottom-right corner is* `(r, c)`. If `grid[r][c] = 0`, it can't be a corner: `dp[r][c] = 0`. If `grid[r][c] = 1`:
```
dp[r][c] = 1 + min(dp[r-1][c-1], dp[r-1][c], dp[r][c-1])
```

Why three neighbours? A `k × k` square at `(r, c)` requires:
- A `(k-1) × (k-1)` square at `(r-1, c-1)` (the top-left chunk).
- A `(k-1) × (k-1)` square at `(r-1, c)` (covering the top edge).
- A `(k-1) × (k-1)` square at `(r, c-1)` (covering the left edge).

The smallest of these three caps the size of the square that can grow from `(r, c)`. Plus one for the cell itself.

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#777777"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
flowchart LR
  CELL["(r, c)"]
  CELL --> NW["dp[r-1][c-1]<br/>top-left chunk"]
  CELL --> N["dp[r-1][c]<br/>top edge"]
  CELL --> W["dp[r][c-1]<br/>left edge"]
  NW --> COMB["1 + min(...)"]
  N --> COMB
  W --> COMB
```

<p align="center"><strong>The square ending at <code>(r, c)</code> can only be as large as the smallest of three predecessor squares — the top-left, top, and left neighbours. Plus one for the current cell.</strong></p>

> *Pause. Why is min the right aggregator here? Predict the consequence of using max.*

Min ensures the square is *fully* filled with 1s. If any of the three neighbours has a smaller largest-square, that's the binding constraint — extending beyond would require 1s in cells that aren't 1. Using max would let one good corner override missing cells elsewhere — wrong.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(rows × cols) space=O(rows × cols)
import ast
from typing import List

class Solution:
    def largest_square_area(self, matrix: List[List[int]]) -> int:
        rows: int = len(matrix)
        cols: int = len(matrix[0])
        max_size: int = 0

        # Create a 2D dynamic programming table
        dp: List[List[int]] = [[0] * cols for _ in range(rows)]

        # Fill the first row and column of the dp table
        for row in range(rows):
            dp[row][0] = matrix[row][0]
            max_size = max(max_size, dp[row][0])

        for col in range(cols):
            dp[0][col] = matrix[0][col]
            max_size = max(max_size, dp[0][col])

        # Fill the remaining dp table using the recurrence relation
        for row in range(1, rows):
            for col in range(1, cols):
                if matrix[row][col] == 1:

                    # Calculate the size of the square submatrix ending
                    # at (row, col) based on the sizes of the submatrices
                    # ending at (row-1, col-1), (row-1, col), and (row,
                    # col-1)
                    dp[row][col] = (
                        min(
                            dp[row - 1][col - 1],
                            min(dp[row - 1][col], dp[row][col - 1]),
                        )
                        + 1
                    )
                    max_size = max(max_size, dp[row][col])

        return max_size * max_size


matrix = ast.literal_eval(input())   # the test case's matrix
print(Solution().largest_square_area(matrix))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int largestSquareArea(int[][] matrix) {
            int rows = matrix.length;
            int cols = matrix[0].length;
            int maxSize = 0;

            // Create a 2D dynamic programming table
            int[][] dp = new int[rows][cols];

            // Fill the first row and column of the dp table
            for (int row = 0; row < rows; row++) {
                dp[row][0] = matrix[row][0];
                maxSize = Math.max(maxSize, dp[row][0]);
            }

            for (int col = 0; col < cols; col++) {
                dp[0][col] = matrix[0][col];
                maxSize = Math.max(maxSize, dp[0][col]);
            }

            // Fill the remaining dp table using the recurrence relation
            for (int row = 1; row < rows; row++) {
                for (int col = 1; col < cols; col++) {
                    if (matrix[row][col] == 1) {

                        // Calculate the size of the square submatrix ending
                        // at (row, col) based on the sizes of the
                        // submatrices ending at (row-1, col-1), (row-1,
                        // col), and (row, col-1)
                        dp[row][col] =
                            Math.min(
                                dp[row - 1][col - 1],
                                Math.min(dp[row - 1][col], dp[row][col - 1])
                            ) +
                            1;
                        maxSize = Math.max(maxSize, dp[row][col]);
                    }
                }
            }

            return maxSize * maxSize;
        }
    }

    public static void main(String[] args) {
        int[][] matrix = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(new Solution().largestSquareArea(matrix));
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
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(rows × cols)` |
| Space | `O(rows × cols)` (reducible to `O(cols)` with rolling rows) |

</details>
