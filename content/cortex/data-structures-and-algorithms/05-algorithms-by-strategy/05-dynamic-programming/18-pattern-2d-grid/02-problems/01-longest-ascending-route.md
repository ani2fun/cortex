---
title: "Longest Ascending Route"
summary: "Given an n × m grid of integers, find the length of the longest path along which strictly-increasing values appear. Moves are 4-connected (up/down/left/right). Diagonals don't count."
prereqs:
  - 18-pattern-2d-grid/01-pattern
difficulty: medium
kind: problem
topics: [2d-grid, dynamic-programming]
---

# Longest Ascending Route

## The Problem

Given an `n × m` grid of integers, find the length of the longest path along which strictly-increasing values appear. Moves are 4-connected (up/down/left/right). Diagonals don't count.

```
Input:  matrix = [[1, 2, 9],
                  [5, 3, 8],
                  [4, 6, 7]]
Output: 7                 Path: 1 → 2 → 3 → 6 → 7 → 8 → 9 (snakes through the grid)

Input:  matrix = [[1, 2, 3],
                  [4, 5, 6],
                  [7, 8, 9]]
Output: 5                 Multiple longest paths; e.g. 1 → 2 → 3 → 6 → 9
```

---

## Examples

**Example 1**
```
Input:  matrix = [[1, 2, 9], [5, 3, 8], [4, 6, 7]]
Output: 7
Explanation: Path 1 → 2 → 3 → 6 → 7 → 8 → 9 snakes through the grid, length 7.
```

**Example 2**
```
Input:  matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
Output: 5
Explanation: Multiple paths of length 5 exist, e.g. 1 → 2 → 3 → 6 → 9.
```

```quiz
{
  "prompt": "Why does the strict-increase rule guarantee the recursion DAG is acyclic?",
  "options": [
    "Because the grid is finite",
    "Every edge points to a strictly greater value, so no path can revisit a cell",
    "DFS never revisits nodes",
    "Memoization prevents cycles"
  ],
  "answer": "Every edge points to a strictly greater value, so no path can revisit a cell"
}
```

## Constraints

- `1 ≤ n, m ≤ 200`
- `0 ≤ matrix[i][j] ≤ 2³¹ − 1`
- Moves are 4-connected (up/down/left/right); diagonals don't count.

```python run viz=grid
import ast

class Solution:
    def longest_ascending_route(self, matrix):
        # Your code goes here — DFS with memoization from every cell;
        # dp[r][c] = length of longest ascending path starting at (r, c).
        return 0

matrix = ast.literal_eval(input())   # the test case's matrix
print(Solution().longest_ascending_route(matrix))
```

```java run viz=grid
import java.util.*;

public class Main {
    static class Solution {
        public int longestAscendingRoute(int[][] matrix) {
            // Your code goes here — DFS with memoization from every cell;
            // dp[r][c] = length of longest ascending path starting at (r, c).
            return 0;
        }
    }

    public static void main(String[] args) {
        int[][] matrix = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(new Solution().longestAscendingRoute(matrix));
    }

    // "[[1, 2, 9], [5, 3, 8]]" → int[][] — reads the test case's matrix
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
    { "args": { "matrix": "[[1, 2, 9], [5, 3, 8], [4, 6, 7]]" }, "expected": "7" },
    { "args": { "matrix": "[[1, 2, 3], [4, 5, 6], [7, 8, 9]]" }, "expected": "5" },
    { "args": { "matrix": "[[1]]" }, "expected": "1" },
    { "args": { "matrix": "[[1, 2]]" }, "expected": "2" },
    { "args": { "matrix": "[[9, 8, 7], [6, 5, 4], [3, 2, 1]]" }, "expected": "5" },
    { "args": { "matrix": "[[1, 1], [1, 1]]" }, "expected": "1" },
    { "args": { "matrix": "[[3, 4, 5], [3, 2, 6], [2, 2, 1]]" }, "expected": "4" }
  ]
}
```

<details>
<summary><h2>The Recurrence — DFS with Memoization</h2></summary>


`dp[r][c]` = length of the longest strictly-ascending path *starting* at `(r, c)`. For each of the 4 neighbours `(r', c')` with `matrix[r'][c'] > matrix[r][c]`, recurse and take 1 plus that:
```
dp[r][c] = 1 + max over up-neighbour ascending of dp[r'][c']
```
If no neighbour is strictly greater, `dp[r][c] = 1` (just the cell itself).

The natural fill order is *anti-topological* — from peaks downward — but the simplest implementation is a DFS with memoization. The memo prevents re-exploring the same `(r, c)` once its value is settled.

> *Pause. Why does the strict-increase rule guarantee no cycles? Predict the consequence.*

Because every edge points to a strictly greater value, no path can revisit a cell — that would require returning to a smaller value somewhere, contradicting monotonicity. The recursion DAG is acyclic, so DFS terminates after each cell is visited at most once.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(rows × cols) space=O(rows × cols)
import ast
from typing import List

class Solution:
    def dfs(
        self,
        matrix: List[List[int]],
        row: int,
        col: int,
        dp: List[List[int]],
    ) -> int:
        rows = len(matrix)
        cols = len(matrix[0])

        if dp[row][col] != -1:
            return dp[row][col]

        max_length = 1

        # Array for row directions: up, right, down, left
        dx = [-1, 0, 1, 0]

        # Array for column directions: up, right, down, left
        dy = [0, 1, 0, -1]

        # Explore all four directions
        for i in range(4):

            # Get the new row index
            new_row = row + dx[i]

            # Get the new column index
            new_col = col + dy[i]

            # Check if the new position is within bounds and the value is
            # greater than the current position
            if (
                new_row >= 0
                and new_row < rows
                and new_col >= 0
                and new_col < cols
                and matrix[new_row][new_col] > matrix[row][col]
            ):

                # Recursively call dfs and update max_length
                max_length = max(
                    max_length,
                    1 + self.dfs(matrix, new_row, new_col, dp),
                )

        # Store the computed max_length for current position in the dp
        # matrix
        dp[row][col] = max_length
        return max_length

    def longest_ascending_route(self, matrix: List[List[int]]) -> int:
        rows = len(matrix)
        cols = len(matrix[0])

        # Initialize dp matrix with -1s
        dp = [[-1] * cols for _ in range(rows)]

        max_length = 0

        for row in range(rows):
            for col in range(cols):

                # Call dfs for each position and update max_length
                max_length = max(
                    max_length, self.dfs(matrix, row, col, dp)
                )

        # Return the final max_length
        return max_length


matrix = ast.literal_eval(input())   # the test case's matrix
print(Solution().longest_ascending_route(matrix))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private int dfs(int[][] matrix, int row, int col, int[][] dp) {
            int rows = matrix.length;
            int cols = matrix[0].length;

            if (dp[row][col] != -1) {
                return dp[row][col];
            }

            int maxLength = 1;

            // Array for row directions: up, right, down, left
            int[] dx = { -1, 0, 1, 0 };

            // Array for column directions: up, right, down, left
            int[] dy = { 0, 1, 0, -1 };

            // Explore all four directions
            for (int i = 0; i < 4; i++) {

                // Get the new row index
                int newRow = row + dx[i];

                // Get the new column index
                int newCol = col + dy[i];

                // Check if the new position is within bounds and the value
                // is greater than the current position
                if (
                    newRow >= 0 &&
                    newRow < rows &&
                    newCol >= 0 &&
                    newCol < cols &&
                    matrix[newRow][newCol] > matrix[row][col]
                ) {

                    // Recursively call dfs and update maxLength
                    maxLength = Math.max(
                        maxLength,
                        1 + dfs(matrix, newRow, newCol, dp)
                    );
                }
            }

            // Store the computed maxLength for current position in the dp
            // matrix
            dp[row][col] = maxLength;
            return maxLength;
        }

        public int longestAscendingRoute(int[][] matrix) {
            int rows = matrix.length;
            int cols = matrix[0].length;

            // Initialize dp matrix with -1s
            int[][] dp = new int[rows][cols];
            for (int[] row : dp) Arrays.fill(row, -1);

            int maxLength = 0;

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {

                    // Call dfs for each position and update maxLength
                    maxLength = Math.max(
                        maxLength,
                        dfs(matrix, row, col, dp)
                    );
                }
            }

            // Return the final maxLength
            return maxLength;
        }
    }

    public static void main(String[] args) {
        int[][] matrix = parseIntMatrix(new Scanner(System.in).nextLine());
        System.out.println(new Solution().longestAscendingRoute(matrix));
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
| Time | `O(rows × cols)` — each cell's DFS is O(1) thanks to memoization |
| Space | `O(rows × cols)` for the memo table + recursion stack |

</details>
