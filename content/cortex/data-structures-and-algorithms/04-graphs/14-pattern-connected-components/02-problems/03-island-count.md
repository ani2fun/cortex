---
title: "Island Count"
summary: "A grid of 0s and 1s. 1 = land, 0 = water. An island is a maximal group of connected 1s. Two land cells are connected if they're adjacent in any of 8 directions (cardinals + diagonals)."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
---

# Problem: Island Count

## The Problem

A grid of `0`s and `1`s. `1` = land, `0` = water. An **island** is a maximal group of connected `1`s. Two land cells are connected if they're adjacent in **any of 8 directions** (cardinals + diagonals).

Return the number of islands.

```
Input:  grid = [[1, 1, 0, 0],
                [0, 0, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 2
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


The grid is just a graph in disguise. Each cell is a node. Each "is-adjacent" relation is an edge.

- `f`: nothing per-cell (just visit).
- `g`: +1 per island found.
- *Connectivity*: 8 directions instead of 4.

The 8-direction array is the only structural change from grid traversal in lesson 5.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=grid
from typing import List, Tuple

class Solution:
    def is_valid_cell(
        self, grid: List[List[int]], row: int, col: int
    ) -> bool:

        # Check if a cell is valid and belongs to a region of 1's, also
        # check that the cell is not water
        return (
            row >= 0
            and row < len(grid)
            and col >= 0
            and col < len(grid[0])
            and grid[row][col] == 1
        )

    def dfs(
        self,
        grid: List[List[int]],
        row: int,
        col: int,
        visited: List[List[bool]],
    ) -> None:

        # Mark the current cell as visited
        visited[row][col] = True

        # Define the possible movements: all 8 directions (up, right, 
        # down, left, and diagonals)
        directions: List[Tuple[int, int]] = [
            (-1,  0), # Top
            (-1,  1), # Top-right
            (0,  1),  # Right
            (1,  1),  # Bottom-right
            (1,  0),  # Bottom
            (1, -1),  # Bottom-left
            (0, -1),  # Left
            (-1, -1), # Top-left
        ]

        # Check all 8 neighbouring cells
        for dr, dc in directions:
            new_row = row + dr
            new_col = col + dc

            # If the neighbour is not visited, recursively call the DFS
            # function on the neighbour
            if (
                self.is_valid_cell(grid, new_row, new_col)
                and not visited[new_row][new_col]
            ):
                self.dfs(grid, new_row, new_col, visited)

    def island_count(self, grid: List[List[int]]) -> int:
        rows = len(grid)

        # Check if the grid is empty
        if rows == 0:
            return 0

        cols = len(grid[0])

        # Initialise the island count to 0
        islands = 0

        # Initialize visited array
        visited = [[False] * cols for _ in range(rows)]

        # Traverse each cell of the grid
        for row in range(rows):
            for col in range(cols):

                # If the cell is a water cell or it's already visited,
                # all the cells connected to it are also visited
                if grid[row][col] == 0 or visited[row][col]:
                    continue

                # Found a new land cell
                islands += 1

                # Perform DFS on this new cell to visit all the cells
                # connected to it.
                self.dfs(grid, row, col, visited)

        # Return the number of islands
        return islands


# Examples from the problem statement
print(Solution().island_count([[1,1,0,0],[0,0,1,1],[1,0,1,1],[1,0,0,0]]))  # 2
print(Solution().island_count([[1,1,0,0],[0,1,1,1],[1,0,1,1],[1,0,0,0]]))  # 1

# Edge cases
print(Solution().island_count([]))                                          # 0
print(Solution().island_count([[0]]))                                       # 0
print(Solution().island_count([[1]]))                                       # 1
print(Solution().island_count([[0,0,0],[0,0,0]]))                           # 0
print(Solution().island_count([[1,1],[1,1]]))                               # 1
print(Solution().island_count([[1,0,1],[0,0,0],[1,0,1]]))                   # 4
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        private boolean isValidCell(int[][] grid, int row, int col) {

            // Check if a cell is valid and belongs to a region of 1's, also
            // check that the cell is not water
            return (
                row >= 0 &&
                row < grid.length &&
                col >= 0 &&
                col < grid[0].length &&
                grid[row][col] == 1
            );
        }

        private void dfs(
            int[][] grid,
            int row,
            int col,
            boolean[][] visited
        ) {

            // Mark the current cell as visited
            visited[row][col] = true;

            // Define the possible movements: all 8 directions (up, right, 
            // down, left, and diagonals)
            int[][] directions = {
                {-1,  0}, // Top
                {-1,  1}, // Top-right
                {0,  1},  // Right
                {1,  1},  // Bottom-right
                {1,  0},  // Bottom
                {1, -1},  // Bottom-left
                {0, -1},  // Left
                {-1, -1}  // Top-left
            };

            // Check all 8 neighbouring cells
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                // If the neighbour is not visited, recursively call the DFS
                // function on the neighbour
                if (
                    isValidCell(grid, newRow, newCol) &&
                    !visited[newRow][newCol]
                ) {
                    dfs(grid, newRow, newCol, visited);
                }
            }
        }

        public int islandCount(int[][] grid) {
            int rows = grid.length;

            // Check if the grid is empty
            if (rows == 0) {
                return 0;
            }

            int cols = grid[0].length;

            // Initialise the island count to 0
            int islands = 0;

            // Initialize visited array
            boolean[][] visited = new boolean[rows][cols];

            // Traverse each cell of the grid
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {

                    // If the cell is a water cell or it's already visited,
                    // all the cells connected to it are also visited
                    if (grid[row][col] == 0 || visited[row][col]) {
                        continue;
                    }

                    // Found a new land cell
                    islands++;

                    // Perform DFS on this new cell to visit all the cells
                    // connected to it.
                    dfs(grid, row, col, visited);
                }
            }

            // Return the number of islands
            return islands;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.islandCount(new int[][]{{1,1,0,0},{0,0,1,1},{1,0,1,1},{1,0,0,0}}));  // 2
        System.out.println(sol.islandCount(new int[][]{{1,1,0,0},{0,1,1,1},{1,0,1,1},{1,0,0,0}}));  // 1

        // Edge cases
        System.out.println(sol.islandCount(new int[][]{}));                          // 0
        System.out.println(sol.islandCount(new int[][]{{0}}));                       // 0
        System.out.println(sol.islandCount(new int[][]{{1}}));                       // 1
        System.out.println(sol.islandCount(new int[][]{{0,0,0},{0,0,0}}));           // 0
        System.out.println(sol.islandCount(new int[][]{{1,1},{1,1}}));               // 1
        System.out.println(sol.islandCount(new int[][]{{1,0,1},{0,0,0},{1,0,1}}));   // 4
    }
}
```

</details>
