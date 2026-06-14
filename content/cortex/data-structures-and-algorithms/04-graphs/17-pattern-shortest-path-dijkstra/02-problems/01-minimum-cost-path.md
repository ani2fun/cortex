---
title: "Minimum Cost Path"
summary: "Grid where each cell is the cost to step on it. Find the minimum total cost from (0, 0) to (N-1, M-1), where you can move in 4 cardinal directions."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
kind: problem
topics: [shortest-path-dijkstra, graph]
---

# Problem: Minimum Cost Path

## Problem Statement

Given an **N × M grid** where each cell holds its step cost, find the minimum total cost to travel from the top-left corner `(0, 0)` to the bottom-right corner `(N-1, M-1)`. You may move in the four cardinal directions (up, right, down, left). The cost of a path is the sum of the costs of every cell visited, including the start and end cells.

## Examples

**Example 1:**
```
Input:  grid = [[9, 4, 9, 9],
                [6, 7, 6, 4],
                [8, 3, 3, 7],
                [7, 4, 9, 10]]
Output: 43
```

The cheapest path goes: `(0,0)→(1,0)→(2,0)→(2,1)→(2,2)→(2,3)→(3,3)` → `9+6+8+3+3+4+10 = 43`.

**Example 2:**
```
Input:  grid = [[9, 4, 9, 9],
                [1, 7, 6, 4],
                [1, 3, 3, 7],
                [1, 2, 2, 10]]
Output: 26
```

## Constraints

- `1 ≤ N, M ≤ 500`
- `1 ≤ grid[i][j] ≤ 10⁵`
- Return `0` for an empty grid

```python run viz=grid viz-root=grid
import ast, heapq

class Solution:
    def minimum_cost_path(self, grid):
        # Your code goes here — model each cell as a node, run Dijkstra
        # from (0,0), return the settled cost at (N-1, M-1).
        return 0

grid = ast.literal_eval(input())
print(Solution().minimum_cost_path(grid))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        public int minimumCostPath(int[][] grid) {
            // Your code goes here — model each cell as a node, run Dijkstra
            // from (0,0), return the settled cost at (N-1, M-1).
            return 0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumCostPath(grid));
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

```testcases
{
  "args": [
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[9,4,9,9],[6,7,6,4],[8,3,3,7],[7,4,9,10]]" }
  ],
  "cases": [
    { "args": { "grid": "[[9,4,9,9],[6,7,6,4],[8,3,3,7],[7,4,9,10]]" }, "expected": "43" },
    { "args": { "grid": "[[9,4,9,9],[1,7,6,4],[1,3,3,7],[1,2,2,10]]" }, "expected": "26" },
    { "args": { "grid": "[[5]]" }, "expected": "5" },
    { "args": { "grid": "[[1,2],[3,4]]" }, "expected": "7" },
    { "args": { "grid": "[[1,100],[1,1]]" }, "expected": "3" },
    { "args": { "grid": "[[1,1,1],[1,1,1],[1,1,1]]" }, "expected": "5" }
  ]
}
```

<details>
<summary>Editorial</summary>

Treat every grid cell as a graph node. An edge from cell `A` to its valid cardinal neighbour `B` has weight `grid[B]` — the cost of stepping onto `B`. Seed the min-heap with `(grid[0][0], 0, 0)` and initialise a `minCost` table to `∞`, setting `minCost[0][0] = grid[0][0]`. Standard Dijkstra relaxation fills the table; the answer is `minCost[N-1][M-1]`. The source cell's cost is added at seeding time, not during relaxation — that avoids double-counting. No "unreachable" sentinel issue here because the grid is fully connected from `(0,0)`.

```python solution time=O((N·M) log(N·M)) space=O(N·M)
import ast, heapq

class Cell:
    def __init__(self, row, col, cost):
        self.row = row; self.col = col; self.cost = cost
    def __lt__(self, other): return self.cost < other.cost

class Solution:
    def is_valid_cell(self, row, col, rows, cols):
        return row >= 0 and row < rows and col >= 0 and col < cols

    def minimum_cost_path(self, grid):
        rows = len(grid)
        if rows == 0:
            return 0
        cols = len(grid[0])
        min_cost = [[float("inf")] * cols for _ in range(rows)]
        pq = []
        min_cost[0][0] = grid[0][0]
        heapq.heappush(pq, Cell(0, 0, grid[0][0]))
        directions = [(-1, 0), (0, 1), (1, 0), (0, -1)]
        while pq:
            curr_cell = heapq.heappop(pq)
            curr_row = curr_cell.row
            curr_col = curr_cell.col
            cost = curr_cell.cost
            for dr, dc in directions:
                new_row = curr_row + dr
                new_col = curr_col + dc
                if self.is_valid_cell(new_row, new_col, rows, cols):
                    new_cost = cost + grid[new_row][new_col]
                    if new_cost < min_cost[new_row][new_col]:
                        min_cost[new_row][new_col] = new_cost
                        heapq.heappush(pq, Cell(new_row, new_col, new_cost))
        return min_cost[rows - 1][cols - 1]

grid = ast.literal_eval(input())
print(Solution().minimum_cost_path(grid))
```

```java solution
import java.util.*;

public class Main {
    static class Cell {
        int row, col, cost;
        Cell(int row, int col, int cost) { this.row = row; this.col = col; this.cost = cost; }
    }

    static class Solution {
        boolean isValidCell(int row, int col, int rows, int cols) {
            return row >= 0 && row < rows && col >= 0 && col < cols;
        }

        public int minimumCostPath(int[][] grid) {
            int rows = grid.length;
            if (rows == 0) return 0;
            int cols = grid[0].length;
            int[][] minCost = new int[rows][cols];
            for (int[] row : minCost) Arrays.fill(row, Integer.MAX_VALUE);
            PriorityQueue<Cell> pq = new PriorityQueue<>((a, b) -> a.cost - b.cost);
            minCost[0][0] = grid[0][0];
            pq.add(new Cell(0, 0, grid[0][0]));
            int[][] dirs = {{-1,0},{0,1},{1,0},{0,-1}};
            while (!pq.isEmpty()) {
                Cell cur = pq.poll();
                for (int[] d : dirs) {
                    int nr = cur.row + d[0], nc = cur.col + d[1];
                    if (isValidCell(nr, nc, rows, cols)) {
                        int newCost = cur.cost + grid[nr][nc];
                        if (newCost < minCost[nr][nc]) {
                            minCost[nr][nc] = newCost;
                            pq.add(new Cell(nr, nc, newCost));
                        }
                    }
                }
            }
            return minCost[rows - 1][cols - 1];
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumCostPath(grid));
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

</details>
