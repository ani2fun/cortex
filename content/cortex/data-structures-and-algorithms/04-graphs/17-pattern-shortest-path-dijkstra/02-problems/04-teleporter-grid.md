---
title: "Teleporter Grid"
summary: "Given an NxM grid, a source cell (r1, c1), and a destination cell (r2, c2), write a function to find and return the minimum cost to reach from the source to the destination."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: hard
---

# Problem: Teleporter Grid

## The Problem

Given an **NxM** **grid**, a **source** cell `(r1, c1)`, and a **destination** cell `(r2, c2)`, write a function to find and return the minimum cost to reach from the source to the destination. 

> -   The cost to move from a cell to its adjacent cell is `1`.
> -   A value of `0` in a cell means the call cannot be visited.
> -   A value of `1` in a cell means the cell can be visited.
> -   A value greater than `1` in a cell is a teleporter cell. All cells with the same number represent linked teleporters. Moving into a teleporter costs `1`, and you may instantly teleport to any other teleporter with the same ID at a cost of `1`.
> -   Each teleporter may be used at most once during the path.

> You must abide by the following constraint:
>
> -   You can only move in the four cardinal directions, i.e., `up`, `right`, `down`, and `left`.

```
Input:  grid = [[1, 5, 0, 2], [0, 1, 1, 0], [2, 0, 1, 1], [1, 5, 5, 1]], source = [0, 0], destination = [3, 3]
Output: 3
Input:  grid = [[1, 5, 0, 5], [0, 1, 1, 2], [2, 0, 1, 0], [1, 3, 3, 2]], source = [0, 0], destination = [3, 3]
Output: 5
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


This is grid Dijkstra with a state dimension for teleporter usage. The puzzle says a teleporter may be used **at most once** on the whole path, so the search state is `(row, col, teleporter_used)` where `teleporter_used` is `0` or `1` — and `min_cost` becomes a 3D array indexed by that flag. The same cell reached without having teleported is a different state from the same cell reached after teleporting.

Cardinal moves cost `1` and keep the flag unchanged. When the popped cell sits on a teleporter and the flag is still `0`, every linked teleporter (same ID, excluding the current cell) is relaxed at cost `1` with the flag flipped to `1` — capturing the one-shot rule. The standard min-heap, infinity-initialised distances, and lazy stale-skip otherwise behave exactly as in plain Dijkstra.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=grid viz-root=grid
import heapq
from typing import List, Tuple, Dict

# Structure to represent a Cell in the graph
class Cell:
    def __init__(
        self, row: int, col: int, cost: int, teleporter_used: int
    ):
        self.row = row
        self.col = col
        self.cost = cost
        self.teleporter_used = teleporter_used

    # Comparator function for the priority queue to create a min-heap
    def __lt__(self, other):
        return self.cost < other.cost

class Solution:
    def is_valid_cell(
        self, grid: List[List[int]], row: int, col: int
    ) -> bool:
        return (
            0 <= row < len(grid)
            and 0 <= col < len(grid[0])
            and grid[row][col] != 0
        )

    def build_teleporter_map(
        self, grid: List[List[int]]
    ) -> Dict[int, List[Tuple[int, int]]]:
        teleporters = {}
        for row in range(len(grid)):
            for col in range(len(grid[row])):
                if grid[row][col] > 1:
                    teleporters.setdefault(grid[row][col], []).append(
                        (row, col)
                    )
        return teleporters

    def teleporter_grid(
        self,
        grid: List[List[int]],
        source: Tuple[int, int],
        destination: Tuple[int, int],
    ) -> int:
        rows = len(grid)
        if rows == 0:
            return -1

        cols = len(grid[0])

        # 3D minCost: rows x cols x 2 (teleporter usage state)
        min_cost = [
            [[float("inf")] * 2 for _ in range(cols)]
            for _ in range(rows)
        ]

        # Build teleporter map for quick access to teleporter pairs
        teleporters = self.build_teleporter_map(grid)

        # Create a priority queue (min-heap) to store the cells with
        # their costs
        pq = []

        # Assign the minimum cost of the starting point with teleporter
        # unused
        min_cost[source[0]][source[1]][0] = 0

        # Enqueue starting cell and the cost to move on it
        heapq.heappush(pq, Cell(source[0], source[1], 0, 0))

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
            teleporter_used = curr_cell.teleporter_used

            # If we reached the destination, return the cost
            if curr_row == destination[0] and curr_col == destination[1]:
                return cost

            # If the cost is greater than the recorded minimum cost, skip
            # processing
            if cost > min_cost[curr_row][curr_col][teleporter_used]:
                continue

            # Explore the neighbours
            for dr, dc in directions:
                new_row = curr_row + dr
                new_col = curr_col + dc

                # Check if the new cell is within the grid
                if self.is_valid_cell(grid, new_row, new_col):

                    # Cost to move to an adjacent cell is always 1
                    new_cost = cost + 1

                    # If a shorter path is found, update the minimum
                    # cost and add the new cell to the priority queue
                    if (
                        new_cost
                        < min_cost[new_row][new_col][teleporter_used]
                    ):

                        # Update the minimum cost for the new cell
                        min_cost[new_row][new_col][
                            teleporter_used
                        ] = new_cost

                        # Add the new cell to the priority queue
                        heapq.heappush(
                            pq,
                            Cell(
                                new_row,
                                new_col,
                                new_cost,
                                teleporter_used,
                            ),
                        )

            # Add teleporter usage if on a teleporter cell and teleporter
            # not used yet
            if grid[curr_row][curr_col] > 1 and teleporter_used == 0:
                teleporter_id = grid[curr_row][curr_col]
                for new_row, new_col in teleporters.get(
                    teleporter_id, []
                ):

                    # Skip the current cell
                    if new_row == curr_row and new_col == curr_col:
                        continue

                    # Teleportation cost is 1 (same as moving to adjacent
                    # cell)
                    new_cost = cost + 1

                    # If a shorter path is found using the teleporter, update the minimum
                    # cost and add the new cell to the priority queue
                    if new_cost < min_cost[new_row][new_col][1]:
                        min_cost[new_row][new_col][1] = new_cost
                        heapq.heappush(
                            pq, Cell(new_row, new_col, new_cost, 1)
                        )

        # If the destination is unreachable, return -1
        return -1


# Examples from the problem statement
print(Solution().teleporter_grid([[1,5,0,2],[0,1,1,0],[2,0,1,1],[1,5,5,1]], (0,0), (3,3)))  # 3
print(Solution().teleporter_grid([[1,5,0,5],[0,1,1,2],[2,0,1,0],[1,3,3,2]], (0,0), (3,3)))  # 5

# Edge cases
print(Solution().teleporter_grid([], (0,0), (0,0)))                                         # -1 — empty grid
print(Solution().teleporter_grid([[1]], (0,0), (0,0)))                                      # 0 — source is dest
print(Solution().teleporter_grid([[1,1],[1,1]], (0,0), (1,1)))                              # 2 — no teleporters
print(Solution().teleporter_grid([[1,0],[0,1]], (0,0), (1,1)))                              # -1 — blocked path
print(Solution().teleporter_grid([[2,1],[1,2]], (0,0), (1,1)))                              # 1 — teleport shortcut
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    // Structure to represent a Cell in the graph
    static class Cell {

        int row;
        int col;
        int cost;
        int teleporterUsed;

        Cell(int row, int col, int cost, int teleporterUsed) {
            this.row = row;
            this.col = col;
            this.cost = cost;
            this.teleporterUsed = teleporterUsed;
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
        private boolean isValidCell(int[][] grid, int row, int col) {
            return (
                row >= 0 &&
                row < grid.length &&
                col >= 0 &&
                col < grid[0].length &&
                grid[row][col] != 0
            );
        }

        private Map<Integer, List<List<Integer>>> buildTeleporterMap(
            int[][] grid
        ) {
            Map<Integer, List<List<Integer>>> teleporters = new HashMap<>();
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[row].length; col++) {
                    if (grid[row][col] > 1) {
                        teleporters
                            .computeIfAbsent(
                                grid[row][col],
                                k -> new ArrayList<>()
                            )
                            .add(Arrays.asList(row, col));
                    }
                }
            }
            return teleporters;
        }

        public int teleporterGrid(
            int[][] grid,
            List<Integer> source,
            List<Integer> destination
        ) {
            int rows = grid.length;
            if (rows == 0) {
                return -1;
            }

            int cols = grid[0].length;

            // 3D minCost: rows x cols x 2 (teleporter usage state)
            int[][][] minCost = new int[rows][cols][2];
            for (int[][] arr : minCost) {
                for (int[] a : arr) {
                    Arrays.fill(a, Integer.MAX_VALUE);
                }
            }

            // Build teleporter map for quick access to teleporter pairs
            Map<Integer, List<List<Integer>>> teleporters =
                buildTeleporterMap(grid);

            // Create a priority queue (min-heap) to store the cells with
            // their costs
            PriorityQueue<Cell> pq = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Assign the minimum cost of the starting point with teleporter
            // unused
            minCost[source.get(0)][source.get(1)][0] = 0;

            // Enqueue starting cell and the cost to move on it
            pq.add(new Cell(source.get(0), source.get(1), 0, 0));

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
                int teleporterUsed = currCell.teleporterUsed;

                // If we reached the destination, return the cost
                if (
                    currRow == destination.get(0) &&
                    currCol == destination.get(1)
                ) {
                    return cost;
                }

                // If the cost is greater than the recorded minimum cost,
                // skip processing
                if (cost > minCost[currRow][currCol][teleporterUsed]) {
                    continue;
                }

                // Explore the neighbours
                for (int[] dir : directions) {
                    int newRow = currRow + dir[0];
                    int newCol = currCol + dir[1];

                    // Check if the new cell is within the grid
                    if (isValidCell(grid, newRow, newCol)) {

                        // Cost to move to an adjacent cell is always 1
                        int newCost = cost + 1;

                        // If a shorter path is found, update the minimum
                        // cost and add the new cell to the priority queue
                        if (
                            newCost < minCost[newRow][newCol][teleporterUsed]
                        ) {

                            // Update the minimum cost for the new cell
                            minCost[newRow][newCol][teleporterUsed] =
                                newCost;

                            // Add the new cell to the priority queue
                            pq.add(
                                new Cell(
                                    newRow,
                                    newCol,
                                    newCost,
                                    teleporterUsed
                                )
                            );
                        }
                    }
                }

                // Add teleporter usage if on a teleporter cell and
                // teleporter not used yet
                if (grid[currRow][currCol] > 1 && teleporterUsed == 0) {
                    int teleporterID = grid[currRow][currCol];
                    for (List<Integer> teleporter : teleporters.get(
                        teleporterID
                    )) {
                        int newRow = teleporter.get(0);
                        int newCol = teleporter.get(1);

                        // Skip the current cell
                        if (newRow == currRow && newCol == currCol) {
                            continue;
                        }

                        // Teleportation cost is 1 (same as moving to
                        // adjacent cell)
                        int newCost = cost + 1;

                        // If a shorter path is found using the teleporter,
                        // update the minimum cost and add the new cell to
                        // the priority queue
                        if (newCost < minCost[newRow][newCol][1]) {
                            minCost[newRow][newCol][1] = newCost;
                            pq.add(new Cell(newRow, newCol, newCost, 1));
                        }
                    }
                }
            }

            // If the destination is unreachable, return -1
            return -1;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.teleporterGrid(new int[][]{{1,5,0,2},{0,1,1,0},{2,0,1,1},{1,5,5,1}}, List.of(0,0), List.of(3,3)));  // 3
        System.out.println(sol.teleporterGrid(new int[][]{{1,5,0,5},{0,1,1,2},{2,0,1,0},{1,3,3,2}}, List.of(0,0), List.of(3,3)));  // 5

        // Edge cases
        System.out.println(sol.teleporterGrid(new int[][]{}, List.of(0,0), List.of(0,0)));            // -1
        System.out.println(sol.teleporterGrid(new int[][]{{1}}, List.of(0,0), List.of(0,0)));         // 0
        System.out.println(sol.teleporterGrid(new int[][]{{1,1},{1,1}}, List.of(0,0), List.of(1,1))); // 2
        System.out.println(sol.teleporterGrid(new int[][]{{1,0},{0,1}}, List.of(0,0), List.of(1,1))); // -1
        System.out.println(sol.teleporterGrid(new int[][]{{2,1},{1,2}}, List.of(0,0), List.of(1,1))); // 1
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Dijkstra is **the** weighted shortest-path tool, and the *pattern* extends naturally to stateful problems by augmenting nodes with extra dimensions (stops, parity, fuel left, …). The four-step recipe — *priority queue keyed by cost, distance array, lazy stale-skip, weight-aware relaxation* — handles essentially every "minimum cumulative cost" problem you'll meet, with or without state.

The full progression of shortest-path tools you now have:

| Tool | When |
|---|---|
| **BFS** | Unweighted graph; "minimum hops" |
| **Dijkstra** | Non-negative weights; "minimum cost" |
| **Bellman-Ford** | Negative weights allowed; detects negative cycles |
| **Floyd-Warshall** | All-pairs shortest path |

You've now completed the **graph chapter**. Together with the data-structure foundations (arrays, linked lists, hash tables, stacks, queues, trees) and the algorithmic patterns covered here (DFS, components, two-colour, BFS-shortest-path, Dijkstra), you have the toolkit to solve essentially every graph-shaped problem you'll meet — interview, real-world, or research.

> **Transfer challenge.** A real GPS routing system has to consider *real-time traffic* — edge weights change throughout the day. Sketch (don't implement) how you'd extend the Dijkstra pattern to handle "the cost of edge `u → v` depends on the time you arrive at `u`."

</details>
<details>
<summary><strong>Sketch</strong></summary>

This is **time-dependent Dijkstra**. Each edge weight is a function `w(u, v, t)` of arrival time `t` at `u`. When relaxing, compute `new_time = t + w(u, v, t)`. Push `(new_time, v)`. The classical Dijkstra invariant still holds *if* the cost function is non-decreasing in time (no time-travel arbitrage) — known as the *FIFO property*. Real GPS systems use this exact pattern with traffic predictions for `w`.

When the FIFO property fails (e.g. carpool lanes become free at certain times), you'd switch to label-correcting algorithms — closer to Bellman-Ford. The pattern stretches; the underlying logic is the same.

</details>
