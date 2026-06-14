---
title: "Solve Sudoku"
summary: "Given a partially filled 9 × 9 sudoku grid (with 0 marking empty cells), fill in the empty cells so that:"
prereqs:
  - 05-pattern-backtracking-search/01-pattern
difficulty: hard
kind: problem
topics: [backtracking-search, backtracking]
---

# Solve Sudoku

The hardest worked problem in this section. Sudoku's state space is enormous, but constraint propagation prunes it relentlessly.

---

## The Problem

Given a partially filled `9 × 9` sudoku grid (with `0` marking empty cells), fill in the empty cells so that:
1. Every row contains digits `1`-`9` exactly once.
2. Every column contains digits `1`-`9` exactly once.
3. Each of the nine `3 × 3` sub-boxes contains digits `1`-`9` exactly once.

The grid is mutated in place; return the solved grid as a list-of-lists.

---

## Examples

**Example 1**
```
Input:  [[5,3,0,0,7,0,0,0,0],
         [6,0,0,1,9,5,0,0,0],
         [0,9,8,0,0,0,0,6,0],
         [8,0,0,0,6,0,0,0,3],
         [4,0,0,8,0,3,0,0,1],
         [7,0,0,0,2,0,0,0,6],
         [0,6,0,0,0,0,2,8,0],
         [0,0,0,4,1,9,0,0,5],
         [0,0,0,0,8,0,0,7,9]]
Output: [[5,3,4,6,7,8,9,1,2],
         [6,7,2,1,9,5,3,4,8],
         [1,9,8,3,4,2,5,6,7],
         [8,5,9,7,6,1,4,2,3],
         [4,2,6,8,5,3,7,9,1],
         [7,1,3,9,2,4,8,5,6],
         [9,6,1,5,3,7,2,8,4],
         [2,8,7,4,1,9,6,3,5],
         [3,4,5,2,8,6,1,7,9]]
```

## Constraints

- Grid is always `9 × 9`.
- `0` marks an empty cell; digits `1`-`9` are pre-filled clues.
- The puzzle is guaranteed to have a unique solution.

```python run viz=grid viz-root=board
import ast

class Solution:
    def solve_sudoku(self, board):
        # Your code goes here
        # Find next empty cell (0), try digits 1-9 that fit, recurse, undo on failure
        pass

board = ast.literal_eval(input())
Solution().solve_sudoku(board)
print(board)
```

```java run viz=grid viz-root=board
import java.util.*;

public class Main {
    static class Solution {
        public void solveSudoku(int[][] board) {
            // Your code goes here
            // Find next empty cell (0), try digits 1-9 that fit, recurse, undo on failure
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
        int[][] board = parseIntMatrix(sc.nextLine());
        new Solution().solveSudoku(board);
        List<List<Integer>> result = new ArrayList<>();
        for (int[] row : board) {
            List<Integer> rowList = new ArrayList<>();
            for (int v : row) rowList.add(v);
            result.add(rowList);
        }
        System.out.println(result);
    }
}
```

```testcases
{
  "args": [
    { "id": "board", "label": "board", "type": "int[][]", "placeholder": "[[5,3,0,0,7,0,0,0,0],...]" }
  ],
  "cases": [
    { "args": { "board": "[[5,3,0,0,7,0,0,0,0],[6,0,0,1,9,5,0,0,0],[0,9,8,0,0,0,0,6,0],[8,0,0,0,6,0,0,0,3],[4,0,0,8,0,3,0,0,1],[7,0,0,0,2,0,0,0,6],[0,6,0,0,0,0,2,8,0],[0,0,0,4,1,9,0,0,5],[0,0,0,0,8,0,0,7,9]]" }, "expected": "[[5, 3, 4, 6, 7, 8, 9, 1, 2], [6, 7, 2, 1, 9, 5, 3, 4, 8], [1, 9, 8, 3, 4, 2, 5, 6, 7], [8, 5, 9, 7, 6, 1, 4, 2, 3], [4, 2, 6, 8, 5, 3, 7, 9, 1], [7, 1, 3, 9, 2, 4, 8, 5, 6], [9, 6, 1, 5, 3, 7, 2, 8, 4], [2, 8, 7, 4, 1, 9, 6, 3, 5], [3, 4, 5, 2, 8, 6, 1, 7, 9]]" }
  ]
}
```

<details>
<summary><h2>What's the Recursion Doing?</h2></summary>


Find the first empty cell. Try every digit `1`-`9` that doesn't conflict with the row, column, or sub-box. For each viable digit, place it and recurse. If the recursion solves the rest, return `true`. If not, undo the placement and try the next digit. If no digit works, return `false`.

The world is the grid; the answer *is* the grid; the validation check is "does placing `d` here satisfy all three constraints (row, col, box)?"

</details>
<details>
<summary><h2>Applying the Diagnostic Questions</h2></summary>


| # | Check | Answer |
|---|---|---|
| **Q1** | State IS the answer? | **Yes** — the grid is the candidate; when filled, it's the solution. |
| **Q2** | Boolean propagation? | **Yes** — `solve()` returns `true` if a solution exists from the current state. |
| **Q3** | Explicit undo? | **Yes** — write digit, recurse; on failure, set cell back to empty. |

### Q1 — Why "state IS"?

The 9×9 grid we're filling is the candidate. Once it's fully populated and valid, it's the answer. ✓

### Q2 — Why "boolean propagation"?

Sudoku has a unique solution (in well-formed puzzles). We propagate `true` upward as soon as we find it. ✓

### Q3 — Why "explicit undo"?

If a digit doesn't lead to a solution, we have to clear that cell so we can try the next digit (or so the parent's loop can try a different placement). ✓

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python solution time=O(9^81) space=O(81)
import ast
from typing import List

class Solution:

    # Checks if placing 'num' in the specified row is valid
    def is_valid_row(
        self, board: List[List[int]], row: int, num: int
    ) -> bool:
        for col in range(9):
            if board[row][col] == num:
                return False
        return True

    # Checks if placing 'num' in the specified column is valid
    def is_valid_col(
        self, board: List[List[int]], col: int, num: int
    ) -> bool:
        for row in range(9):
            if board[row][col] == num:
                return False
        return True

    # Checks if placing 'num' in the 3x3 sub-grid containing (row, col)
    # is valid
    def is_valid_subgrid(
        self, board: List[List[int]], row: int, col: int, num: int
    ) -> bool:
        start_row = (row // 3) * 3
        start_col = (col // 3) * 3
        for r in range(start_row, start_row + 3):
            for c in range(start_col, start_col + 3):
                if board[r][c] == num:
                    return False
        return True

    # Checks if placing 'num' at (row, col) is valid in all respects
    def is_valid_placement(
        self, board: List[List[int]], row: int, col: int, num: int
    ) -> bool:

        # Check row, column, and sub-grid constraints
        return (
            self.is_valid_row(board, row, num)
            and self.is_valid_col(board, col, num)
            and self.is_valid_subgrid(board, row, col, num)
        )

    # Recursive search function to fill the Sudoku board
    def search_solution(self, board: List[List[int]]) -> bool:

        # Iterate through each cell of the board
        for row in range(9):
            for col in range(9):

                # Only attempt to fill empty cells
                if board[row][col] == 0:

                    # Try all digits from 1 to 9 in this cell
                    for num in range(1, 10):

                        # Check if placing the number is valid (solution
                        # state possible)
                        if self.is_valid_placement(board, row, col, num):

                            # Place the number in the cell (make choice)
                            board[row][col] = num

                            # Recursively attempt to fill the rest of the
                            # board
                            if self.search_solution(board):
                                return True

                            # If it did not lead to a solution, remove
                            # the number (revert choice)
                            board[row][col] = 0

                    # If no valid number can be placed in this cell,
                    # backtrack
                    return False

        # If all cells are filled successfully, the board is solved
        return True

    def solve_sudoku(self, board: List[List[int]]) -> None:

        # Start the search process to fill the board
        self.search_solution(board)


board = ast.literal_eval(input())
Solution().solve_sudoku(board)
print(board)
```

```java solution
import java.util.*;

public class Main {
    static class Solution {

        // Checks if placing 'num' in the specified row is valid
        private boolean isValidRow(int[][] board, int row, int num) {
            for (int col = 0; col < 9; col++) {
                if (board[row][col] == num) {
                    return false;
                }
            }
            return true;
        }

        // Checks if placing 'num' in the specified column is valid
        private boolean isValidCol(int[][] board, int col, int num) {
            for (int row = 0; row < 9; row++) {
                if (board[row][col] == num) {
                    return false;
                }
            }
            return true;
        }

        // Checks if placing 'num' in the 3x3 sub-grid containing (row, col)
        // is valid
        private boolean isValidSubGrid(
            int[][] board,
            int row,
            int col,
            int num
        ) {
            int startRow = (row / 3) * 3;
            int startCol = (col / 3) * 3;
            for (int r = startRow; r < startRow + 3; r++) {
                for (int c = startCol; c < startCol + 3; c++) {
                    if (board[r][c] == num) {
                        return false;
                    }
                }
            }
            return true;
        }

        // Checks if placing 'num' at (row, col) is valid in all respects
        private boolean isValidPlacement(
            int[][] board,
            int row,
            int col,
            int num
        ) {

            // Check row, column, and sub-grid constraints
            return (
                isValidRow(board, row, num) &&
                isValidCol(board, col, num) &&
                isValidSubGrid(board, row, col, num)
            );
        }

        // Recursive search function to fill the Sudoku board
        private boolean searchSolution(int[][] board) {

            // Iterate through each cell of the board
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {

                    // Only attempt to fill empty cells
                    if (board[row][col] == 0) {

                        // Try all digits from 1 to 9 in this cell
                        for (int num = 1; num <= 9; num++) {

                            // Check if placing the number is valid (solution
                            // state possible)
                            if (isValidPlacement(board, row, col, num)) {

                                // Place the number in the cell (make choice)
                                board[row][col] = num;

                                // Recursively attempt to fill the rest of
                                // the board
                                if (searchSolution(board)) {

                                    // If successful, propagate success back
                                    return true;
                                }

                                // If it did not lead to a solution, remove
                                // the number (revert choice)
                                board[row][col] = 0;
                            }
                        }

                        // If no valid number can be placed in this cell,
                        // backtrack
                        return false;
                    }
                }
            }

            // If all cells are filled successfully, the board is solved
            return true;
        }

        public void solveSudoku(int[][] board) {

            // Start the search process to fill the board
            searchSolution(board);
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
        int[][] board = parseIntMatrix(sc.nextLine());
        new Solution().solveSudoku(board);
        List<List<Integer>> result = new ArrayList<>();
        for (int[] row : board) {
            List<Integer> rowList = new ArrayList<>();
            for (int v : row) rowList.add(v);
            result.add(rowList);
        }
        System.out.println(result);
    }
}
```

### Complexity Analysis

| Resource | Cost |
|---|---|
| **Time** | `O(9^81)` worst case (exponential) |
| **Space (stack)** | `O(81)` (one frame per empty cell) |

In practice, the constraint propagation (rule out digits that conflict with row/col/box) reduces this enormously — typical Sudokus solve in milliseconds.

### Edge Cases

| Case | Example | Expected |
|---|---|---|
| Already solved | All cells filled | Return immediately. |
| Unsolvable | Contradictory clues | `false` returned (no solution). |
| Empty board | All cells 0 | Generates *some* valid Sudoku (not unique). |
| Multiple solutions | Some easier puzzles | Returns the first found. |

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Sudoku is backtracking search at maximum complexity: 81 cells, up to 9 choices per cell, three constraints to validate per placement. The recipe — find empty cell, try digits, recurse, undo — is identical to the maze and N-Queens. Only the constraint check is richer.

You came in suspecting search and enumeration were two faces of the same algorithm. You're leaving knowing they share the recursion's structure but differ in three specific ways: state mutation vs append, boolean return vs leaf record, explicit vs implicit undo. With these four problems — maze pathfinding, word search, N-queens, sudoku — you have the canonical examples for every search-flavoured backtracking problem you'll meet.

The next major topic in the course is **sorting**, where the four backtracking patterns and recursion's mechanics from the previous chapter give way to a different style of algorithm design: divide-and-conquer (merge sort, quicksort) and engineering trade-offs across worst-case, average-case, and stability.

**Transfer challenge — close out backtracking:** Take the Sudoku solver and modify it to count *all* solutions instead of returning the first one. (Don't worry about run-time; for some puzzles this is intractable.) What changes? Hint: when a solution is found, *don't* return `true`; record the board state and continue.

<details>
<summary><strong>Answer — open after you've thought about it</strong></summary>

```python run viz=grid
import ast

class Solution:
    def count_sudoku_solutions(self, board):
        count = [0]                      # mutable counter (closure trick)
        self._search(board, count)
        return count[0]

    def _search(self, board, count):
        for row in range(9):
            for col in range(9):
                if board[row][col] == 0:
                    for d in range(1, 10):
                        if self._is_valid(board, row, col, d):
                            board[row][col] = d
                            self._search(board, count)
                            board[row][col] = 0      # undo (always)
                    return                              # don't propagate "true"
        count[0] += 1                                   # all cells filled — record one more solution

    def _is_valid(self, board, row, col, num):
        for c in range(9):
            if board[row][c] == num: return False
        for r in range(9):
            if board[r][col] == num: return False
        sr, sc = (row // 3) * 3, (col // 3) * 3
        for r in range(sr, sr + 3):
            for c in range(sc, sc + 3):
                if board[r][c] == num: return False
        return True
```

The change: instead of returning `true` and propagating, *continue exploring* even after a solution is found. The undo step now always runs (no "if true: return"). Time complexity is much worse — we no longer get the exponential speedup of early termination — but the recipe is otherwise identical.

This is the same enumeration-vs-search distinction we set up at the top of this lesson. **Search → Enumeration just by removing the early-termination return.** The world is your candidate; the world is your output; the world is your accumulator. You've now seen all four backtracking patterns and built the muscle memory to spot them on sight.

</details>

</details>
