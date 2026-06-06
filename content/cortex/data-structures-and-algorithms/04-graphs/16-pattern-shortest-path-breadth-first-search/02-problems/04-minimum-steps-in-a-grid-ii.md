---
title: "Minimum Steps in a Grid II"
summary: "Given an NxM grid filled with values of either 0, or 1 and a non-negative integer k, write a function to find and return the minimum number of steps required to reach the cell (N-1, M-1) from the cell"
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: hard
---

# Problem: Minimum Steps in a Grid II

## The Problem

Given an **NxM** **grid** filled with values of either `0`, or `1` and a non-negative integer **k**, write a function to find and return the minimum number of steps required to reach the cell `(N-1, M-1)` from the cell `(0, 0)`. If there is no valid path, return `-1` instead.

You can convert at most **k** non-walkable cells to walkable cells.

> -   A value of `1` in a cell means it's walkable.
> -   A value of `0` in a cell means it's a wall and not walkable.

> You must abide by the following constraint:
>
> -   You can only move in the four cardinal directions, i.e., `up`, `right`, `down`, and `left`.

```
Input:  grid = [[1, 0, 1, 1], [0, 1, 1, 1], [0, 1, 0, 1]], k = 1
Output: 5
Input:  grid = [[1, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 1]], k = 5
Output: 5
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


This is BFS shortest path with a state dimension bolted onto each node. A plain `visited[row][col]` is no longer enough: a cell reached with more wall-removals still in hand may open a shorter route than the same cell reached with none left. The fix is a 3D `visited[row][col][walls_left]` — the BFS state is `(row, col, walls_left)`, not just `(row, col)`.

Moving onto a wall (`0`) costs one of the `k` removals; moving onto a `1` costs none. A neighbour is only enqueued when `walls_left` stays non-negative and that exact state hasn't been visited. Because every step still costs 1, the FIFO queue guarantees the first time the destination is dequeued is the minimum step count.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=grid
from typing import List, Tuple
from queue import Queue

class Cell:
    def __init__(self, row: int, col: int, steps: int, walls_left: int):
        self.row = row
        self.col = col
        self.steps = steps
        self.walls_left = walls_left

class Solution:
    def is_valid_cell(
        self, row: int, col: int, rows: int, cols: int
    ) -> bool:
        return 0 <= row < rows and 0 <= col < cols

    def minimum_steps_in_a_grid_ii(
        self, grid: List[List[int]], k: int
    ) -> int:
        rows = len(grid)
        cols = len(grid[0])

        # Create a visited grid to keep track of visited cells
        visited = [
            [[False] * (k + 1) for _ in range(cols)] for _ in range(rows)
        ]

        # Create a queue for BFS traversal
        queue = Queue()
        queue.put(Cell(0, 0, 0, k))
        visited[0][0][k] = True

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
            curr_walls_left = curr_cell.walls_left

            # Check if reached the destination
            if curr_row == rows - 1 and curr_col == cols - 1:
                return curr_steps

            # Explore the neighbours
            for dr, dc in directions:
                new_row = curr_row + dr
                new_col = curr_col + dc

                # Check if the new cell is within the grid boundaries
                if self.is_valid_cell(new_row, new_col, rows, cols):
                    new_walls_left = curr_walls_left - (
                        1 if grid[new_row][new_col] == 0 else 0
                    )

                    # If we have walls left to remove and haven't
                    # visited this state
                    if (
                        new_walls_left >= 0
                        and not visited[new_row][new_col][new_walls_left]
                    ):
                        
                        # Add the new cell to the queue
                        queue.put(
                            Cell(
                                new_row,
                                new_col,
                                curr_steps + 1,
                                new_walls_left,
                            )
                        )

                        # Mark the new cell as visited
                        visited[new_row][new_col][new_walls_left] = True

        # No path found
        return -1


# Examples from the problem statement
print(Solution().minimum_steps_in_a_grid_ii([[1,0,1,1],[0,1,1,1],[0,1,0,1]], 1))  # 5
print(Solution().minimum_steps_in_a_grid_ii([[1,0,0,0],[0,0,0,0],[0,0,0,1]], 5))  # 5

# Edge cases
print(Solution().minimum_steps_in_a_grid_ii([[1]], 0))                             # 0 — 1x1
print(Solution().minimum_steps_in_a_grid_ii([[0]], 1))                             # 0 — 1x1 wall removed
print(Solution().minimum_steps_in_a_grid_ii([[1,1],[1,1]], 0))                     # 2 — all passable
print(Solution().minimum_steps_in_a_grid_ii([[1,0],[0,1]], 0))                     # -1 — no removals allowed
print(Solution().minimum_steps_in_a_grid_ii([[1,0],[0,1]], 1))                     # 2 — one removal allowed
print(Solution().minimum_steps_in_a_grid_ii([[1,0,0],[0,0,0],[0,0,1]], 0))         # -1 — need walls but k=0
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Cell {

        int row;
        int col;
        int steps;
        int wallsLeft;

        public Cell(int row, int col, int steps, int wallsLeft) {
            this.row = row;
            this.col = col;
            this.steps = steps;
            this.wallsLeft = wallsLeft;
        }
    }

    static class Solution {
        private boolean isValidCell(int row, int col, int rows, int cols) {
            return row >= 0 && row < rows && col >= 0 && col < cols;
        }

        public int minimumStepsInAGridII(int[][] grid, int k) {
            int rows = grid.length, cols = grid[0].length;

            // Create a visited grid to keep track of visited cells
            boolean[][][] visited = new boolean[rows][cols][k + 1];

            // Create a queue for BFS traversal
            Queue<Cell> queue = new LinkedList<>();
            queue.add(new Cell(0, 0, 0, k));
            visited[0][0][k] = true;

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
                int currWallsLeft = currCell.wallsLeft;

                // Check if reached the destination
                if (currRow == rows - 1 && currCol == cols - 1) {
                    return currSteps;
                }

                // Explore the neighbours
                for (int[] dir : directions) {
                    int newRow = currRow + dir[0];
                    int newCol = currCol + dir[1];

                    // Check if the new cell is within the grid boundaries
                    if (isValidCell(newRow, newCol, rows, cols)) {
                        int newWallsLeft =
                            currWallsLeft -
                            (grid[newRow][newCol] == 0 ? 1 : 0);

                        // If we have walls left to remove and haven't
                        // visited this state
                        if (
                            newWallsLeft >= 0 &&
                            !visited[newRow][newCol][newWallsLeft]
                        ) {

                            // Add the new cell to the queue
                            queue.add(
                                new Cell(
                                    newRow,
                                    newCol,
                                    currSteps + 1,
                                    newWallsLeft
                                )
                            );

                            // Mark the new cell as visited
                            visited[newRow][newCol][newWallsLeft] = true;
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
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,0,1,1},{0,1,1,1},{0,1,0,1}}, 1));  // 5
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,0,0,0},{0,0,0,0},{0,0,0,1}}, 5));  // 5

        // Edge cases
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1}}, 0));                             // 0
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{0}}, 1));                             // 0
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,1},{1,1}}, 0));                     // 2
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,0},{0,1}}, 0));                     // -1
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,0},{0,1}}, 1));                     // 2
        System.out.println(sol.minimumStepsInAGridII(new int[][]{{1,0,0},{0,0,0},{0,0,1}}, 0));         // -1
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


BFS shortest path is the **simplest, most reliable, fastest** algorithm for *unweighted* shortest-path problems. The recipe is dead easy: queue with distance, mark on push, early exit at target. The two power-ups — **multi-source BFS** (seed every starting point at d=0) and **implicit graphs** (compute neighbours on the fly) — turn the same algorithm into a swiss-army knife for grid, word-ladder, and graph-shortest-path problems alike.

When BFS isn't enough — when edges have varying weights — you upgrade to Dijkstra, the next pattern. The mental shift is small: replace the FIFO queue with a min-heap that orders by *cumulative weight* instead of *insertion order*. Same shape, different ordering principle.

> **Transfer challenge.** A virus enters a hospital ward modelled as a grid. Initially several rooms are infected. Each minute, the virus spreads to all empty 4-cardinal neighbours. Find the minimum minutes until every room is infected, or `-1` if some rooms can never be reached. *Hint: this is multi-source BFS in disguise.*

</details>
<details>
<summary><strong>Sketch</strong></summary>

Multi-source BFS from all initially-infected rooms, distance 0. Carry distance with each cell; the maximum distance reached is the answer. If any "empty" room remains unvisited at the end, return -1 (it can never be reached). Same code as nearest-distance, with one extra max-tracking variable.

This is *exactly* LeetCode's "Rotting Oranges" problem. Multi-source BFS = the right tool.

</details>
