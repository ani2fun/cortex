---
title: "Solve N Queens"
summary: "Given n, return all distinct solutions to the n-queens problem. Each solution is a list of n strings, each of length n, where Q is a queen and . is empty. Two queens attack iff they share a row, colum"
prereqs:
  - 05-pattern-backtracking-search/01-pattern
difficulty: hard
kind: problem
topics: [backtracking-search, backtracking]
---

# Solve N Queens

The classic. Place `n` queens on an `n × n` board so no two attack each other. Find *all* valid configurations.

---

## The Problem

Given `n`, return all distinct solutions to the n-queens problem. Each solution is a list of `n` integers representing the column position of the queen in each row (row `i` → `solution[i]`). Two queens attack iff they share a row, column, or diagonal.

```
Input:  n = 4
Output: [[1, 3, 0, 2], [2, 0, 3, 1]]
```

There are exactly two solutions for `n = 4`. For `n = 8`, the famous answer is 92 solutions.

---

## Examples

**Example 1**
```
Input:  n = 4
Output: [[1, 3, 0, 2], [2, 0, 3, 1]]
Explanation: Each inner list gives queen columns per row.
  [1,3,0,2] → row0:col1, row1:col3, row2:col0, row3:col2  =  .Q.. / ...Q / Q... / ..Q.
  [2,0,3,1] → row0:col2, row1:col0, row2:col3, row3:col1  =  ..Q. / Q... / ...Q / .Q..
```

**Example 2**
```
Input:  n = 1
Output: [[0]]
Explanation: One queen on a 1×1 board — trivially placed in column 0.
```

```quiz
{
  "prompt": "How many solutions exist for n = 8?",
  "options": ["12", "42", "92", "184"],
  "answer": "92"
}
```

## Constraints

- `1 ≤ n ≤ 9`
- `n = 2` and `n = 3` have no solutions (output `[]`).
- Results are returned in natural backtracking order (column 0 first at each row) — no sorting.

```python run viz=array viz-root=board
from typing import List

class Solution:
    def solve_n_queens(self, n: int) -> List[List[int]]:
        # Your code goes here — place one queen per row, track columns/diagonals
        # Return list of queen-column-per-row solutions in natural order
        return []

n = int(input())
print(Solution().solve_n_queens(n))
```

```java run viz=array viz-root=board
import java.util.*;

public class Main {
    static class Solution {
        public List<List<Integer>> solveNQueens(int n) {
            // Your code goes here — place one queen per row, track columns/diagonals
            // Return list of queen-column-per-row solutions in natural order
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        int n = Integer.parseInt(new Scanner(System.in).nextLine().trim());
        System.out.println(new Solution().solveNQueens(n));
    }
}
```

```testcases
{
  "args": [
    { "id": "n", "label": "n", "type": "int", "placeholder": "4" }
  ],
  "cases": [
    { "args": { "n": "4" }, "expected": "[[1, 3, 0, 2], [2, 0, 3, 1]]" },
    { "args": { "n": "1" }, "expected": "[[0]]" },
    { "args": { "n": "2" }, "expected": "[]" },
    { "args": { "n": "3" }, "expected": "[]" },
    { "args": { "n": "5" }, "expected": "[[0, 2, 4, 1, 3], [0, 3, 1, 4, 2], [1, 3, 0, 2, 4], [1, 4, 2, 0, 3], [2, 0, 3, 1, 4], [2, 4, 1, 3, 0], [3, 0, 2, 4, 1], [3, 1, 4, 2, 0], [4, 1, 3, 0, 2], [4, 2, 0, 3, 1]]" },
    { "args": { "n": "6" }, "expected": "[[1, 3, 5, 0, 2, 4], [2, 5, 1, 4, 0, 3], [3, 0, 4, 1, 5, 2], [4, 2, 0, 5, 3, 1]]" }
  ]
}
```

<details>
<summary><h2>Why This Is a Search With "Find All"</h2></summary>


Two structural choices simplify the problem:
1. **One queen per row.** No two queens can share a row, so we place exactly one per row. Reduces the choice set per level from `n²` cells to `n` columns.
2. **Process rows in order.** The state at depth `r` is "queens placed in rows 0..r-1." Each frame picks a column for row `r`.

The constraint check at each placement: **the chosen column must not conflict with any already-placed queen** — same column, same diagonal, or same anti-diagonal.

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| # | Check | Answer |
|---|---|---|
| **Q1** | State IS the answer? | **Yes** — the array of column-positions is the configuration. |
| **Q2** | Boolean propagation? | Almost — but here we want *all* solutions, so we use a record-and-continue variant. |
| **Q3** | Explicit undo? | **Yes** — set the column-position to `-1` after exploring its subtree. |

### Q1 — Why "state IS"?

The state — `queenPositions[i]` = column of the queen in row `i` — completely determines a board configuration. ✓

### Q2 — Why "find-all variant"?

For the maze and word problems, we wanted *one* solution and propagated `true` to stop. Here we want *all*, so the search doesn't stop on first success — instead, it records each valid configuration and continues. ✓

### Q3 — Why "explicit undo"?

After fully exploring "what configurations exist with the queen for row 0 in column 0?", we move on to "...with the queen in column 1." That requires resetting `queenPositions[0]` so it doesn't pollute later rows' conflict checks. ✓

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(n!) space=O(n)
from typing import List

class Solution:

    # Helper function to check if a queen can be safely placed at (row,
    # col)
    def can_place_queen(
        self, queen_positions: List[int], row: int, col: int
    ) -> bool:
        for i in range(row):

            # Check for column conflict: no other queen should be in the same column
            # Check for diagonal conflict: no other queen should be in
            # the same diagonal
            if queen_positions[i] == col or row - i == abs(
                col - queen_positions[i]
            ):
                return False
        return True

    def search_solutions(
        self,
        queen_positions: List[int],
        row: int,
        n: int,
        solutions: List[List[int]],
    ) -> None:

        # Check if all queens have been successfully placed
        if row == n:

            # Record the queen column positions as the solution
            solutions.append(list(queen_positions))

            # Continue to find all solutions
            return

        # Loop through each column in the current row to try placing a
        # queen (all choices)
        for col in range(n):

            # Check if placing a queen at (row, col) is safe
            if self.can_place_queen(queen_positions, row, col):

                # Place the queen in the current row at column col (make
                # choice)
                queen_positions[row] = col

                # Recursively try to place queens in the next row
                self.search_solutions(
                    queen_positions, row + 1, n, solutions
                )

                # Remove the queen from the current row to backtrack and
                # try the next column (revert choice)
                queen_positions[row] = -1

    def solve_n_queens(self, n: int) -> List[List[int]]:

        # List to store all valid queen-column-per-row configurations
        solutions: List[List[int]] = []

        # State list: queen_positions[i] stores the column index of the
        # queen placed in row i
        queen_positions: List[int] = [-1] * n

        # Start the search process from the first row (row 0)
        self.search_solutions(queen_positions, 0, n, solutions)

        # Return all valid solutions found
        return solutions


n = int(input())
print(Solution().solve_n_queens(n))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Helper function to check if a queen can be safely placed at (row,
        // col)
        private boolean canPlaceQueen(
            int[] queenPositions,
            int row,
            int col
        ) {
            for (int i = 0; i < row; i++) {

                // Check for column conflict: no other queen should be in the
                // same column Check for diagonal conflict: no other queen
                // should be in the same diagonal
                if (
                    queenPositions[i] == col ||
                    row - i == Math.abs(col - queenPositions[i])
                ) {
                    return false;
                }
            }
            return true;
        }

        private void searchSolutions(
            int[] queenPositions,
            int row,
            int n,
            List<List<Integer>> solutions
        ) {

            // Check if all queens have been successfully placed
            if (row == n) {

                // Record the queen column positions as the solution
                List<Integer> sol = new ArrayList<>();
                for (int c : queenPositions) sol.add(c);
                solutions.add(sol);

                // Continue to find all solutions
                return;
            }

            // Loop through each column in the current row to try placing a
            // queen (all choices)
            for (int col = 0; col < n; col++) {

                // Check if placing a queen at (row, col) is safe
                if (canPlaceQueen(queenPositions, row, col)) {

                    // Place the queen in the current row at column col (make
                    // choice)
                    queenPositions[row] = col;

                    // Recursively try to place queens in the next row
                    searchSolutions(queenPositions, row + 1, n, solutions);

                    // Remove the queen from the current row to backtrack and
                    // try the next column (revert choice)
                    queenPositions[row] = -1;
                }
            }
        }

        public List<List<Integer>> solveNQueens(int n) {

            // List to store all valid queen-column-per-row configurations
            List<List<Integer>> solutions = new ArrayList<>();

            // State list: queenPositions[i] stores the column index of the
            // queen placed in row i
            int[] queenPositions = new int[n];
            Arrays.fill(queenPositions, -1);

            // Start the search process from the first row (row 0)
            searchSolutions(queenPositions, 0, n, solutions);

            // Return all valid solutions found
            return solutions;
        }
    }

    public static void main(String[] args) {
        int n = Integer.parseInt(new Scanner(System.in).nextLine().trim());
        System.out.println(new Solution().solveNQueens(n));
    }
}
```


<details>
<summary><strong>Trace — n = 4</strong></summary>

```
Place row 0:
  col 0: ok, recurse
    row 1: try col 0 conflict, col 1 conflict (diag), col 2 conflict (diag), col 3 ok
      row 2: col 0 conflict, col 1 (with row 0's q at 0)? same col, no — actually position 0 and 1 differ by row diff 2, col diff 1: no diag. ok... actually let me redo
        try col 0: conflicts with row 0's queen at 0 (same col)
        try col 1: row diff 2, col diff 1, not equal — but does conflict with row 1's q at 3? col diff 2, row diff 1, not equal — ok!
        ... (continues)

(Full trace would take many lines; the algorithm finds 2 solutions for n=4.)

Result for n=4: [[1, 3, 0, 2], [2, 0, 3, 1]]
```

</details>

### Complexity Analysis

| Resource | Cost | Why |
|---|---|---|
| **Time** | `O(n!)` worst case | Each row has up to `n` columns; pruning reduces this in practice. |
| **Space (stack)** | `O(n)` | Recursion depth = number of rows. |
| **Space (output)** | `O(num_solutions × n)` | Each solution is a list of `n` column positions. |

For `n = 8`, the algorithm finds 92 solutions; for `n = 12`, 14,200; growth is super-exponential.

### Edge Cases

| Case | Example | Expected |
|---|---|---|
| `n = 1` | n = 1 | `[[0]]` (one solution). |
| `n = 2, 3` | n = 2 or 3 | `[]` (no valid configurations). |
| `n = 4` | n = 4 | 2 solutions. |
| `n = 8` | n = 8 | 92 solutions. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


N-Queens is the canonical "find all configurations" search problem. Place-recurse-undo, with a `canPlace` pruning function that catches conflicts early. The next problem turns the dial up to its maximum — every cell of the world has 9 possible values, and we have to fill 81 of them.

</details>
