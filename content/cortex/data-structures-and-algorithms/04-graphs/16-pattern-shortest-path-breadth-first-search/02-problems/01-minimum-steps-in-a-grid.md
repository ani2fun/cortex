---
title: "Minimum Steps in a Grid"
summary: "In an N×M grid where 1 = walkable and 0 = wall, find the minimum number of cardinal-direction moves from (0, 0) to (N-1, M-1). Return -1 if no path exists."
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: medium
---

# Problem: Minimum Steps in a Grid

## The Problem

In an N×M grid where `1` = walkable and `0` = wall, find the minimum number of cardinal-direction moves from `(0, 0)` to `(N-1, M-1)`. Return -1 if no path exists.

```
Input:  grid = [[1, 0, 1, 1],
                [1, 1, 1, 1],
                [0, 1, 0, 1]]
Output: 5
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


The grid is a graph with implicit edges. BFS from `(0,0)`. As soon as the dequeued cell is `(N-1, M-1)`, return the carried distance. If the queue empties without finding the destination, return -1.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=grid
from typing import List, Tuple
from queue import Queue

class Cell:
    def __init__(self, row: int, col: int, steps: int):
        self.row = row
        self.col = col
        self.steps = steps

class Solution:
    def is_valid_cell(
        self, grid: List[List[int]], row: int, col: int
    ) -> bool:
        return (
            0 <= row < len(grid)
            and 0 <= col < len(grid[0])
            and grid[row][col] == 1
        )

    def minimum_steps_in_a_grid(self, grid: List[List[int]]) -> int:
        rows = len(grid)
        cols = len(grid[0])

        # Create a visited matrix to keep track of visited cells
        visited = [[False] * cols for _ in range(rows)]

        # Create a queue for BFS traversal
        queue = Queue()
        queue.put(Cell(0, 0, 0))
        visited[0][0] = True

        # Define the possible movements: up, right, down, left
        directions: List[Tuple[int, int]] = [
            (-1, 0),  # up
            (0, 1),   # right
            (1, 0),   # down
            (0, -1)   # left
        ]

        while not queue.empty():
            curr_cell = queue.get()

            curr_row = curr_cell.row
            curr_col = curr_cell.col
            curr_steps = curr_cell.steps

            # Check if reached the destination cell
            if curr_row == rows - 1 and curr_col == cols - 1:
                return curr_steps

            # Explore the neighbours
            for dr, dc in directions:
                new_row = curr_row + dr
                new_col = curr_col + dc

                # Check if the new cell is within the grid boundaries
                # and contains 1
                if self.is_valid_cell(grid, new_row, new_col):

                    # Check if the new cell has not been visited before
                    if not visited[new_row][new_col]:

                        # Add the new cell to the queue
                        queue.put(Cell(new_row, new_col, curr_steps + 1))

                        # Mark the new cell as visited
                        visited[new_row][new_col] = True

        # No path found
        return -1


# Examples from the problem statement
print(Solution().minimum_steps_in_a_grid([[1,0,1,1],[1,1,1,1],[0,1,0,1]]))  # 5
print(Solution().minimum_steps_in_a_grid([[1,1,1,1],[1,1,1,1],[1,1,0,1]]))  # 5

# Edge cases
print(Solution().minimum_steps_in_a_grid([[1]]))                             # 0 — 1x1 passable
print(Solution().minimum_steps_in_a_grid([[0]]))                             # -1 — 1x1 wall
print(Solution().minimum_steps_in_a_grid([[1,1],[1,1]]))                     # 2 — 2x2 all passable
print(Solution().minimum_steps_in_a_grid([[1,0],[0,1]]))                     # -1 — no path
print(Solution().minimum_steps_in_a_grid([[0,1,1],[1,1,1],[1,1,1]]))         # -1 — blocked start
print(Solution().minimum_steps_in_a_grid([[1,1,1],[1,1,1],[1,1,0]]))         # -1 — blocked end
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Cell {

        int row;
        int col;
        int steps;

        public Cell(int row, int col, int steps) {
            this.row = row;
            this.col = col;
            this.steps = steps;
        }
    }

    static class Solution {
        private boolean isValidCell(int[][] grid, int row, int col) {
            return (
                row >= 0 &&
                row < grid.length &&
                col >= 0 &&
                col < grid[0].length &&
                grid[row][col] == 1
            );
        }

        public int minimumStepsInAGrid(int[][] grid) {
            int rows = grid.length;
            int cols = grid[0].length;

            // Create a visited matrix to keep track of visited cells
            boolean[][] visited = new boolean[rows][cols];

            // Create a queue for BFS traversal
            Queue<Cell> queue = new LinkedList<>();
            queue.add(new Cell(0, 0, 0));
            visited[0][0] = true;

            // Define the possible movements: up, right, down, left
            int[][] directions = {
                {-1, 0}, // up
                {0, 1},  // right
                {1, 0},  // down
                {0, -1}  // left
            };

            while (!queue.isEmpty()) {
                Cell currCell = queue.poll();

                int currRow = currCell.row;
                int currCol = currCell.col;
                int currSteps = currCell.steps;

                // Check if reached the destination cell
                if (currRow == rows - 1 && currCol == cols - 1) {
                    return currSteps;
                }

                // Explore the neighbours
                for (int[] dir : directions) {
                    int newRow = currRow + dir[0];
                    int newCol = currCol + dir[1];

                    // Check if the new cell is within the grid boundaries
                    // and contains 1
                    if (isValidCell(grid, newRow, newCol)) {

                        // Check if the new cell has not been visited before
                        if (!visited[newRow][newCol]) {

                            // Add the new cell to the queue
                            queue.add(
                                new Cell(newRow, newCol, currSteps + 1)
                            );

                            // Mark the new cell as visited
                            visited[newRow][newCol] = true;
                        }
                    }
                }
            }

            // No path found
            return -1;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1,0,1,1},{1,1,1,1},{0,1,0,1}}));  // 5
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1,1,1,1},{1,1,1,1},{1,1,0,1}}));  // 5

        // Edge cases
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1}}));                             // 0
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{0}}));                             // -1
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1,1},{1,1}}));                     // 2
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1,0},{0,1}}));                     // -1
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{0,1,1},{1,1,1},{1,1,1}}));         // -1
        System.out.println(sol.minimumStepsInAGrid(new int[][]{{1,1,1},{1,1,1},{1,1,0}}));         // -1
    }
}
```

</details>
