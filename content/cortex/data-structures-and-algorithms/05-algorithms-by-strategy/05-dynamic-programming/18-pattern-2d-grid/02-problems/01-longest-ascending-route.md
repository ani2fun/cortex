---
title: "Longest Ascending Route"
summary: "Given an n × m grid of integers, find the length of the longest path along which strictly-increasing values appear. Moves are 4-connected (up/down/left/right). Diagonals don't count."
prereqs:
  - 18-pattern-2d-grid/01-pattern
difficulty: medium
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

```python run viz=grid viz-root=matrix
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


# Examples from the problem statement
print(Solution().longest_ascending_route([[1, 2, 9], [5, 3, 8], [4, 6, 7]]))  # 7
print(Solution().longest_ascending_route([[1, 2, 3], [4, 5, 6], [7, 8, 9]]))  # 5

# Edge cases
print(Solution().longest_ascending_route([[1]]))                               # 1  — 1x1
print(Solution().longest_ascending_route([[1, 2]]))                            # 2  — 1x2
print(Solution().longest_ascending_route([[9, 8, 7], [6, 5, 4], [3, 2, 1]])) # 9  — strictly decreasing grid
print(Solution().longest_ascending_route([[1, 1], [1, 1]]))                    # 1  — all same
print(Solution().longest_ascending_route([[3, 4, 5], [3, 2, 6], [2, 2, 1]])) # 4
```

```java run viz=grid viz-root=matrix
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
        // Examples from the problem statement
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{1,2,9},{5,3,8},{4,6,7}}));  // 7
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{1,2,3},{4,5,6},{7,8,9}}));  // 5

        // Edge cases
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{1}}));                      // 1
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{1,2}}));                    // 2
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{9,8,7},{6,5,4},{3,2,1}})); // 9
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{1,1},{1,1}}));              // 1
        System.out.println(new Solution().longestAscendingRoute(new int[][]{{3,4,5},{3,2,6},{2,2,1}})); // 4
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(rows × cols)` — each cell's DFS is O(1) thanks to memoization |
| Space | `O(rows × cols)` for the memo table + recursion stack |

</details>
