---
title: "Minimum Cost Path"
summary: "Grid where each cell is the cost to step on it. Find the minimum total cost from (0, 0) to (N-1, M-1), where you can move in 4 cardinal directions."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
---

# Problem: Minimum Cost Path

## The Problem

Grid where each cell is the cost to step on it. Find the minimum total cost from `(0, 0)` to `(N-1, M-1)`, where you can move in 4 cardinal directions.

```
Input:  grid = [[9, 4, 9, 9],
                [6, 7, 6, 4],
                [8, 3, 3, 7],
                [7, 4, 9, 10]]
Output: 43
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


The grid is a graph where each cell is a node and each move is an edge. Edge weight from cell `A` to cell `B` is the cost of cell `B` (the cell you're stepping onto). The starting cell's cost is also added — that's the seed.

Standard Dijkstra from `(0, 0)`. Return `distance[N-1][M-1]`.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=grid
import heapq
from typing import List, Tuple

# Structure to represent a Cell in the graph
class Cell:
    def __init__(self, row: int, col: int, cost: int):
        self.row = row
        self.col = col
        self.cost = cost

    # Comparator function for the priority queue to create a min-heap
    def __lt__(self, other):
        return self.cost < other.cost

class Solution:
    def is_valid_cell(
        self, row: int, col: int, rows: int, cols: int
    ) -> bool:
        return row >= 0 and row < rows and col >= 0 and col < cols

    def minimum_cost_path(self, grid: List[List[int]]) -> int:
        rows = len(grid)
        if rows == 0:
            return 0

        cols = len(grid[0])

        # Create a matrix to store the minimum cost to reach each cell
        min_cost = [[float("inf")] * cols for _ in range(rows)]

        # Create a priority queue (min-heap) to store the cells with
        # their costs
        pq = []

        # Assign the minimum cost of the starting point
        min_cost[0][0] = grid[0][0]

        # Enqueue starting cell and the cost to move on it
        heapq.heappush(pq, Cell(0, 0, grid[0][0]))

        # Define the possible movements: up, right, down, left
        directions: List[Tuple[int, int]] = [
            (-1, 0),  # up
            (0, 1),   # right
            (1, 0),   # down
            (0, -1)   # left
        ]

        while pq:
            curr_cell = heapq.heappop(pq)
            curr_row = curr_cell.row
            curr_col = curr_cell.col
            cost = curr_cell.cost

            # Explore the neighbours
            for dr, dc in directions:
                new_row = curr_row + dr
                new_col = curr_col + dc

                # Check if the new cell is within the grid
                if self.is_valid_cell(new_row, new_col, rows, cols):
                    new_cost = cost + grid[new_row][new_col]

                    # If a shorter path is found, update the minimum
                    # cost and add the new cell to the priority queue
                    if new_cost < min_cost[new_row][new_col]:

                        # Update the minimum cost for the new cell
                        min_cost[new_row][new_col] = new_cost

                        # Add the new cell to the priority queue
                        heapq.heappush(
                            pq, Cell(new_row, new_col, new_cost)
                        )

        # Return the minimum cost to reach the bottom right cell
        return min_cost[rows - 1][cols - 1]


# Examples from the problem statement
print(Solution().minimum_cost_path([[9,4,9,9],[6,7,6,4],[8,3,3,7],[7,4,9,10]]))  # 43
print(Solution().minimum_cost_path([[9,4,9,9],[1,7,6,4],[1,3,3,7],[1,2,2,10]]))  # 26

# Edge cases
print(Solution().minimum_cost_path([]))                                            # 0 — empty grid
print(Solution().minimum_cost_path([[5]]))                                         # 5 — single cell
print(Solution().minimum_cost_path([[1,2],[3,4]]))                                 # 7 — 2x2
print(Solution().minimum_cost_path([[1,100],[1,1]]))                               # 3 — prefer going down
print(Solution().minimum_cost_path([[1,1,1],[1,1,1],[1,1,1]]))                     # 5 — all ones
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    // Structure to represent a Cell in the graph
    static class Cell {

        int row;
        int col;
        int cost;

        Cell(int row, int col, int cost) {
            this.row = row;
            this.col = col;
            this.cost = cost;
        }
    }

    // Comparator function for the priority queue to create a min-heap
    static class CompareMinHeap implements Comparator<Cell> {
        public int compare(Cell a, Cell b) {

            // Min-heap based on cost
            return Integer.compare(a.cost, b.cost);
        }
    }

    static class Solution {

        boolean isValidCell(int row, int col, int rows, int cols) {
            return row >= 0 && row < rows && col >= 0 && col < cols;
        }

        public int minimumCostPath(int[][] grid) {
            int rows = grid.length;
            if (rows == 0) {
                return 0;
            }

            int cols = grid[0].length;

            // Create a matrix to store the minimum cost to reach each cell
            int[][] minCost = new int[rows][cols];
            for (int[] row : minCost) {
                java.util.Arrays.fill(row, Integer.MAX_VALUE);
            }

            // Create a priority queue (min-heap) to store the cells with
            // their costs
            PriorityQueue<Cell> pq = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Assign the minimum cost of the starting point
            minCost[0][0] = grid[0][0];

            // Enqueue starting cell and the cost to move on it
            pq.add(new Cell(0, 0, grid[0][0]));

            // Define the possible movements: up, right, down, left
            int[][] directions = {
                {-1, 0}, // up
                {0, 1},  // right
                {1, 0},  // down
                {0, -1}  // left
            };

            while (!pq.isEmpty()) {
                Cell currCell = pq.poll();
                int currRow = currCell.row;
                int currCol = currCell.col;
                int cost = currCell.cost;

                // Explore the neighbours
                for (int[] dir : directions) {
                    int newRow = currRow + dir[0];
                    int newCol = currCol + dir[1];

                    // Check if the new cell is within the grid
                    if (isValidCell(newRow, newCol, rows, cols)) {
                        int newCost = cost + grid[newRow][newCol];

                        // If a shorter path is found, update the minimum
                        // cost and add the new cell to the priority queue
                        if (newCost < minCost[newRow][newCol]) {

                            // Update the minimum cost for the new cell
                            minCost[newRow][newCol] = newCost;

                            // Add the new cell to the priority queue
                            pq.add(new Cell(newRow, newCol, newCost));
                        }
                    }
                }
            }

            // Return the minimum cost to reach the bottom right cell
            return minCost[rows - 1][cols - 1];
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.minimumCostPath(new int[][]{{9,4,9,9},{6,7,6,4},{8,3,3,7},{7,4,9,10}}));  // 43
        System.out.println(sol.minimumCostPath(new int[][]{{9,4,9,9},{1,7,6,4},{1,3,3,7},{1,2,2,10}}));  // 26

        // Edge cases
        System.out.println(sol.minimumCostPath(new int[][]{}));                           // 0
        System.out.println(sol.minimumCostPath(new int[][]{{5}}));                        // 5
        System.out.println(sol.minimumCostPath(new int[][]{{1,2},{3,4}}));                // 7
        System.out.println(sol.minimumCostPath(new int[][]{{1,100},{1,1}}));              // 3
        System.out.println(sol.minimumCostPath(new int[][]{{1,1,1},{1,1,1},{1,1,1}}));   // 5
    }
}
```

</details>
