---
title: "Size of Largest Island"
summary: "Same grid, same 8-direction connectivity. Now return the size (cell count) of the *largest* island."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
---

# Problem: Size of Largest Island

## The Problem

Same grid, same 8-direction connectivity. Now return the **size** (cell count) of the *largest* island.

```
Input:  same grid as before
Output: 6
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: +1 per cell visited.
- `g`: max across components.

The key change: DFS now **returns the size** of the component instead of just side-effecting visited.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

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
    ) -> int:

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

        # Initialize the size of the region
        size = 1

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
                size += self.dfs(grid, new_row, new_col, visited)

        return size

    def size_of_largest_island(self, grid: List[List[int]]) -> int:
        rows = len(grid)

        # Check if the grid is empty
        if rows == 0:
            return 0

        cols = len(grid[0])

        # Initialise the largest island size to 0
        largest_island_size = 0

        # Initialize visited array
        visited = [[False] * cols for _ in range(rows)]

        # Traverse each cell of the grid
        for row in range(rows):
            for col in range(cols):

                # If the cell is a water cell or it's already visited,
                # all the cells connected to it are also visited
                if grid[row][col] == 0 or visited[row][col]:
                    continue

                # Perform DFS on this new cell to visit all the cells
                # connected to it and get the size of this island.
                island_size = self.dfs(grid, row, col, visited)

                # Update the size of the largest island
                largest_island_size = max(
                    largest_island_size, island_size
                )

        # Return the size of the largest island
        return largest_island_size


# Examples from the problem statement
print(Solution().size_of_largest_island([[1,1,0,0],[0,0,1,1],[1,0,1,1],[1,0,0,0]]))  # 6
print(Solution().size_of_largest_island([[1,1,0,0],[0,1,1,1],[1,0,1,1],[1,0,0,0]]))  # 9

# Edge cases
print(Solution().size_of_largest_island([]))                                          # 0
print(Solution().size_of_largest_island([[0]]))                                       # 0
print(Solution().size_of_largest_island([[1]]))                                       # 1
print(Solution().size_of_largest_island([[1,1],[1,1]]))                               # 4
print(Solution().size_of_largest_island([[1,0,1],[0,0,0],[1,0,1]]))                   # 1
print(Solution().size_of_largest_island([[0,0],[0,0]]))                               # 0
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

        private int dfs(int[][] grid, int row, int col, boolean[][] visited) {

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

            // Initialize the size of the region
            int size = 1;

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
                    size += dfs(grid, newRow, newCol, visited);
                }
            }

            return size;
        }

        public int sizeOfLargestIsland(int[][] grid) {
            int rows = grid.length;

            // Check if the grid is empty
            if (rows == 0) {
                return 0;
            }

            int cols = grid[0].length;

            // Initialise the largest island size to 0
            int largestIslandSize = 0;

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

                    // Perform DFS on this new cell to visit all the cells
                    // connected to it and get the size of this island.
                    int islandSize = dfs(grid, row, col, visited);

                    // Update the size of the largest island
                    largestIslandSize = Math.max(
                        largestIslandSize,
                        islandSize
                    );
                }
            }

            // Return the size of the largest island
            return largestIslandSize;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{1,1,0,0},{0,0,1,1},{1,0,1,1},{1,0,0,0}}));  // 6
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{1,1,0,0},{0,1,1,1},{1,0,1,1},{1,0,0,0}}));  // 9

        // Edge cases
        System.out.println(sol.sizeOfLargestIsland(new int[][]{}));                          // 0
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{0}}));                       // 0
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{1}}));                       // 1
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{1,1},{1,1}}));               // 4
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{1,0,1},{0,0,0},{1,0,1}}));   // 1
        System.out.println(sol.sizeOfLargestIsland(new int[][]{{0,0},{0,0}}));               // 0
    }
}
```

### Complexity Analysis

| Problem | Time | Space |
|---|---|---|
| Connected components | O(N + E) | O(N) |
| Sum of minimums | O(N + E) | O(N) |
| Island count | O(R × C) | O(R × C) |
| Size of largest island | O(R × C) | O(R × C) |

Each cell or node is visited exactly once, total. The pattern's strength is that **any number of components** sums to the same `O(N + E)` because each node/edge is processed exactly once across *all* DFS calls combined — the outer-loop iterations don't multiply work, they just spread it.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The connected-components pattern is a tiny structural addition over a plain traversal: **a per-component aggregate that resets between components**. Once you see this two-level structure, dozens of "find / count / process every group" problems fold into the same template.

The pattern works equally on graphs (use the adjacency list) and grids (use the direction array). 4-direction or 8-direction connectivity is a trivial change. The choice between DFS and BFS doesn't matter — both walk the component once, in different orders.

Coming up: **two-colouring** — a cousin of the connected-components pattern that uses DFS/BFS to *paint* every node and check for a contradiction. It's the algorithm that decides whether a graph is bipartite.

> **Transfer challenge.** A photo of a chessboard has been corrupted — some squares are white, some black, some grey (unknown). You're told the original was a valid chessboard (white and black alternate). Sketch how connected-components could detect whether the corruption is consistent with an original chessboard.

</details>
<details>
<summary><strong>Sketch</strong></summary>

Treat each non-grey cell as a node; connect cells sharing an edge that are *both* non-grey. For each component, check every adjacent pair: are their colours opposite (W next to B, B next to W)? If yes for every adjacency in every component, the corruption is consistent. If no, it's not.

This is *almost* two-colouring (next lesson) — components do the partitioning, two-colouring does the consistency check. Combining both gives the full chessboard test.

</details>
