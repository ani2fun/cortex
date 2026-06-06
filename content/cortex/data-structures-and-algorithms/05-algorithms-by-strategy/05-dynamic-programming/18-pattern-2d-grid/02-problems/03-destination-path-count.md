---
title: "Destination Path Count"
summary: "Given an n × m matrix of non-negative cell costs and a target cost, count the number of paths from (0, 0) to (n-1, m-1) whose summed costs equal the target. Moves are right or down only."
prereqs:
  - 18-pattern-2d-grid/01-pattern
difficulty: medium
---

# Destination Path Count

## The Problem

Given an `n × m` matrix of non-negative cell costs and a target cost, count the number of paths from `(0, 0)` to `(n-1, m-1)` whose summed costs equal the target. Moves are right or down only.

```
Input:  matrix = [[1, 2, 9],
                  [5, 3, 8],
                  [4, 6, 7]],
        cost = 19
Output: 1                          One path summing to 19

Input:  matrix = [[1, 2, 3],
                  [1, 5, 6],
                  [2, 8, 9]],
        cost = 21
Output: 2                          Two paths sum to 21
```

<details>
<summary><h2>The Recurrence — 3D State</h2></summary>


Add a third dimension to the standard "count paths to `(r, c)`" DP: the remaining budget. `dp[r][c][k]` = number of paths from `(0, 0)` to `(r, c)` whose costs sum to exactly `k`.

```
dp[r][c][k] = dp[r-1][c][k - matrix[r][c]] + dp[r][c-1][k - matrix[r][c]]
              (with appropriate guards for r=0, c=0, k < matrix[r][c])
```

Base case: `dp[0][0][matrix[0][0]] = 1` (the only path of length 1 hits `matrix[0][0]`).

For implementation, recursion + memoization is cleaner than building a giant 3D array. We memoize on the tuple `(r, c, k)`.

> *Pause. Why is the state 3D, not 2D? Predict the answer.*

Because the answer at `(r, c)` depends on the *budget* still available — two different remaining budgets give two different answers, and the budget changes as we walk. 2D `(r, c)` doesn't have enough information.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=grid viz-root=matrix
from typing import List, Dict

class Solution:
    def destination_path_count_helper(
        self,
        matrix: List[List[int]],
        row: int,
        col: int,
        cost: int,
        dp: Dict[str, int],
    ) -> int:

        # base case
        if cost < 0:
            return 0

        # if we are at the first cell (0, 0)
        if row == 0 and col == 0:
            if matrix[0][0] - cost == 0:
                return 1
            else:
                return 0

        # construct a unique map key from dynamic elements of the input
        key = f"{row}|{col}|{cost}"

        # if the subproblem is seen for the first time, solve it and
        # store its result in a map
        if key not in dp:

            # if we are at the first row, we can only go left
            if row == 0:
                dp[key] = self.destination_path_count_helper(
                    matrix, 0, col - 1, cost - matrix[row][col], dp
                )

            # if we are at the first column, we can only go up
            elif col == 0:
                dp[key] = self.destination_path_count_helper(
                    matrix, row - 1, 0, cost - matrix[row][col], dp
                )

            # recur to count total paths by going both left and top
            else:
                dp[key] = self.destination_path_count_helper(
                    matrix, row - 1, col, cost - matrix[row][col], dp
                ) + self.destination_path_count_helper(
                    matrix, row, col - 1, cost - matrix[row][col], dp
                )

        # return the total number of paths to reach cell (row, col)
        return dp[key]

    def destination_path_count(
        self, matrix: List[List[int]], cost: int
    ) -> int:

        # base case
        if len(matrix) == 0:
            return 0

        row: int = len(matrix)
        col: int = len(matrix[0])

        # create a dictionary to store solutions to subproblems
        dp: Dict[str, int] = {}

        return self.destination_path_count_helper(
            matrix, row - 1, col - 1, cost, dp
        )


# Examples from the problem statement
print(Solution().destination_path_count([[1,2,9],[5,3,8],[4,6,7]], 19))   # 1
print(Solution().destination_path_count([[1,2,3],[1,5,6],[2,8,9]], 21))   # 2

# Edge cases
print(Solution().destination_path_count([[5]], 5))                         # 1  — 1x1 exact
print(Solution().destination_path_count([[5]], 3))                         # 0  — 1x1 wrong cost
print(Solution().destination_path_count([[1,2],[3,4]], 7))                 # 1  — [1,3,4]
print(Solution().destination_path_count([[1,2],[3,4]], 8))                 # 0  — no path sums to 8 (paths are 1+2+4=7 or 1+3+4=8)
print(Solution().destination_path_count([[1,1],[1,1]], 3))                 # 2  — both paths cost 3
```

```java run viz=grid viz-root=matrix
import java.util.*;

public class Main {
    static class Solution {
        private int destinationPathCountHelper(
            int[][] matrix,
            int row,
            int col,
            int cost,
            Map<String, Integer> dp
        ) {

            // base case
            if (cost < 0) {
                return 0;
            }

            // if we are at the first cell (0, 0)
            if (row == 0 && col == 0) {
                if (matrix[0][0] - cost == 0) {
                    return 1;
                } else {
                    return 0;
                }
            }

            // construct a unique map key from dynamic elements of the input
            String key = row + "|" + col + "|" + cost;

            // if the subproblem is seen for the first time, solve it and
            // store its result in a map
            if (!dp.containsKey(key)) {

                // if we are at the first row, we can only go left
                if (row == 0) {
                    dp.put(
                        key,
                        destinationPathCountHelper(
                            matrix,
                            0,
                            col - 1,
                            cost - matrix[row][col],
                            dp
                        )
                    );
                }

                // if we are at the first column, we can only go up
                else if (col == 0) {
                    dp.put(
                        key,
                        destinationPathCountHelper(
                            matrix,
                            row - 1,
                            0,
                            cost - matrix[row][col],
                            dp
                        )
                    );
                }

                // recur to count total paths by going both left and top
                else {
                    dp.put(
                        key,
                        destinationPathCountHelper(
                            matrix,
                            row - 1,
                            col,
                            cost - matrix[row][col],
                            dp
                        ) +
                        destinationPathCountHelper(
                            matrix,
                            row,
                            col - 1,
                            cost - matrix[row][col],
                            dp
                        )
                    );
                }
            }

            // return the total number of paths to reach cell (m, n)
            return dp.get(key);
        }

        public int destinationPathCount(int[][] matrix, int cost) {

            // base case
            if (matrix.length == 0) {
                return 0;
            }

            int row = matrix.length;
            int col = matrix[0].length;

            // create a map to store solutions to subproblems
            Map<String, Integer> dp = new HashMap<>();

            return destinationPathCountHelper(
                matrix,
                row - 1,
                col - 1,
                cost,
                dp
            );
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().destinationPathCount(new int[][]{{1,2,9},{5,3,8},{4,6,7}}, 19));   // 1
        System.out.println(new Solution().destinationPathCount(new int[][]{{1,2,3},{1,5,6},{2,8,9}}, 21));   // 2

        // Edge cases
        System.out.println(new Solution().destinationPathCount(new int[][]{{5}}, 5));                         // 1
        System.out.println(new Solution().destinationPathCount(new int[][]{{5}}, 3));                         // 0
        System.out.println(new Solution().destinationPathCount(new int[][]{{1,2},{3,4}}, 7));                 // 1
        System.out.println(new Solution().destinationPathCount(new int[][]{{1,2},{3,4}}, 8));                 // 0
        System.out.println(new Solution().destinationPathCount(new int[][]{{1,1},{1,1}}, 3));                 // 2
    }
}
```

### Complexity

| Aspect | Cost |
|---|---|
| Time | `O(rows × cols × cost)` — three-dimensional state, each cell `O(1)` work |
| Space | `O(rows × cols × cost)` for the memo |

</details>
