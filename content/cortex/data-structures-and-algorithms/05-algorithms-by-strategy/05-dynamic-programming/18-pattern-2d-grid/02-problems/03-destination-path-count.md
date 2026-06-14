---
title: "Destination Path Count"
summary: "Given an n × m matrix of non-negative cell costs and a target cost, count the number of paths from (0, 0) to (n-1, m-1) whose summed costs equal the target. Moves are right or down only."
prereqs:
  - 18-pattern-2d-grid/01-pattern
difficulty: medium
kind: problem
topics: [2d-grid, dynamic-programming]
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

---

## Examples

**Example 1**
```
Input:  matrix = [[1, 2, 9], [5, 3, 8], [4, 6, 7]], cost = 19
Output: 1
Explanation: Only path 1 → 2 → 3 → 6 → 7 sums to 19.
```

**Example 2**
```
Input:  matrix = [[1, 1], [1, 1]], cost = 3
Output: 2
Explanation: Both paths (right-down and down-right) sum to 1+1+1 = 3.
```

```quiz
{
  "prompt": "Why is the DP state 3D (r, c, remaining_cost) rather than 2D (r, c)?",
  "options": [
    "The grid is 3D",
    "Two paths to the same cell with different remaining budgets give different answers",
    "To handle negative costs",
    "Because we move in 3 directions"
  ],
  "answer": "Two paths to the same cell with different remaining budgets give different answers"
}
```

## Constraints

- `1 ≤ n, m ≤ 50`
- `0 ≤ matrix[i][j] ≤ 100`
- `0 ≤ cost ≤ 10000`
- Moves are right or down only.

```python run viz=grid
import ast

class Solution:
    def destination_path_count(self, matrix, cost):
        # Your code goes here — memoize on (r, c, remaining_cost).
        # dp[r][c][k] = number of paths from (0,0) to (r,c) summing to k.
        return 0

matrix = ast.literal_eval(input())   # the test case's matrix
cost = int(input())                   # the test case's cost
print(Solution().destination_path_count(matrix, cost))
```

```java run viz=grid
import java.util.*;

public class Main {
    static class Solution {
        public int destinationPathCount(int[][] matrix, int cost) {
            // Your code goes here — memoize on (r, c, remaining_cost).
            // dp[r][c][k] = number of paths from (0,0) to (r,c) summing to k.
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        int cost = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().destinationPathCount(matrix, cost));
    }

    // "[[1, 2], [3, 4]]" → int[][] — reads the test case's matrix
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
    { "id": "matrix", "label": "matrix", "type": "int[][]", "placeholder": "[[1, 1, 0], [0, 1, 1]]" },
    { "id": "cost", "label": "cost", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "matrix": "[[1, 2, 9], [5, 3, 8], [4, 6, 7]]", "cost": "19" }, "expected": "1" },
    { "args": { "matrix": "[[1, 2, 3], [1, 5, 6], [2, 8, 9]]", "cost": "21" }, "expected": "2" },
    { "args": { "matrix": "[[5]]", "cost": "5" }, "expected": "1" },
    { "args": { "matrix": "[[5]]", "cost": "3" }, "expected": "0" },
    { "args": { "matrix": "[[1, 2], [3, 4]]", "cost": "7" }, "expected": "1" },
    { "args": { "matrix": "[[1, 2], [3, 4]]", "cost": "8" }, "expected": "1" },
    { "args": { "matrix": "[[1, 1], [1, 1]]", "cost": "3" }, "expected": "2" }
  ]
}
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

```python solution time=O(rows × cols × cost) space=O(rows × cols × cost)
import ast
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


matrix = ast.literal_eval(input())   # the test case's matrix
cost = int(input())                   # the test case's cost
print(Solution().destination_path_count(matrix, cost))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[][] matrix = parseIntMatrix(sc.nextLine());
        int cost = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().destinationPathCount(matrix, cost));
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
| Time | `O(rows × cols × cost)` — three-dimensional state, each cell `O(1)` work |
| Space | `O(rows × cols × cost)` for the memo |

</details>
