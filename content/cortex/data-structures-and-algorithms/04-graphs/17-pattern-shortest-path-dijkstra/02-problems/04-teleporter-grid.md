---
title: "Teleporter Grid"
summary: "Given an NxM grid, a source cell, and a destination cell, find the minimum cost to reach from source to destination. Cells with value 0 are blocked; cells with the same value > 1 are linked teleporters (each usable at most once, cost 1)."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: hard
kind: problem
topics: [shortest-path-dijkstra, graph]
---

# Problem: Teleporter Grid

## Problem Statement

Given an **N × M grid**, a **source** cell `[r1, c1]`, and a **destination** cell `[r2, c2]`, find the minimum cost to travel from source to destination. The rules:

- Moving to an **adjacent cell** (up, right, down, left) costs **1**.
- A cell with value **0** is **blocked** and cannot be visited.
- A cell with value **1** is a normal passable cell.
- A cell with value **> 1** is a **teleporter**. All cells sharing the same value are linked. Moving **into** a teleporter costs 1, and you may then instantly teleport to any other cell with the same ID at a cost of 1. Each teleporter may be used **at most once** during the entire path.

Return the minimum cost, or `-1` if the destination is unreachable.

## Examples

**Example 1:**
```
Input:  grid = [[1, 5, 0, 2],
                [0, 1, 1, 0],
                [2, 0, 1, 1],
                [1, 5, 5, 1]]
        source = [0, 0], destination = [3, 3]
Output: 3
```

One path: `(0,0) → (1,0)` blocked, so go `(0,0) → (2,0)` via teleporter 2 at cost 1 → teleport to `(0,3)` cost 1 → walk to `(3,3)` would take more steps. Actually the cheapest is: step into teleporter 5 at `(0,1)` (cost 1), teleport to `(3,1)` or `(3,2)` (cost 1), walk to `(3,3)` (cost 1) = **3 total**.

**Example 2:**
```
Input:  grid = [[1, 5, 0, 5],
                [0, 1, 1, 2],
                [2, 0, 1, 0],
                [1, 3, 3, 2]]
        source = [0, 0], destination = [3, 3]
Output: 5
```

## Constraints

- `1 ≤ N, M ≤ 500`
- `grid[i][j] ∈ {0, 1, 2, …, 10⁴}`
- `source` and `destination` are valid cells with value ≥ 1
- Return `-1` if destination is unreachable

```python run viz=grid viz-root=grid
import ast, heapq

class Solution:
    def teleporter_grid(self, grid, source, destination):
        # Your code goes here — run Dijkstra with state (row, col, teleporter_used).
        # minCost is 3D: [rows][cols][2]. Cardinal moves cost 1 and keep the
        # teleporter_used flag. When on a teleporter cell with flag=0, relax all
        # linked cells at cost+1 with flag=1.
        return -1

grid = ast.literal_eval(input())
source = ast.literal_eval(input())
destination = ast.literal_eval(input())
print(Solution().teleporter_grid(grid, source, destination))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        public int teleporterGrid(int[][] grid, int[] source, int[] destination) {
            // Your code goes here — run Dijkstra with state (row, col, teleporter_used).
            // minCost is 3D: [rows][cols][2]. Cardinal moves cost 1 and keep the
            // teleporter_used flag. When on a teleporter cell with flag=0, relax all
            // linked cells at cost+1 with flag=1.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        int[] source = parseIntArray(sc.nextLine());
        int[] destination = parseIntArray(sc.nextLine());
        System.out.println(new Solution().teleporterGrid(grid, source, destination));
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

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[1,5,0,2],[0,1,1,0],[2,0,1,1],[1,5,5,1]]" },
    { "id": "source", "label": "source", "type": "int[]", "placeholder": "[0, 0]" },
    { "id": "destination", "label": "destination", "type": "int[]", "placeholder": "[3, 3]" }
  ],
  "cases": [
    { "args": { "grid": "[[1,5,0,2],[0,1,1,0],[2,0,1,1],[1,5,5,1]]", "source": "[0, 0]", "destination": "[3, 3]" }, "expected": "3" },
    { "args": { "grid": "[[1,5,0,5],[0,1,1,2],[2,0,1,0],[1,3,3,2]]", "source": "[0, 0]", "destination": "[3, 3]" }, "expected": "5" },
    { "args": { "grid": "[[1]]", "source": "[0, 0]", "destination": "[0, 0]" }, "expected": "0" },
    { "args": { "grid": "[[1,1],[1,1]]", "source": "[0, 0]", "destination": "[1, 1]" }, "expected": "2" },
    { "args": { "grid": "[[1,0],[0,1]]", "source": "[0, 0]", "destination": "[1, 1]" }, "expected": "-1" },
    { "args": { "grid": "[[2,1],[1,2]]", "source": "[0, 0]", "destination": "[1, 1]" }, "expected": "1" }
  ]
}
```

<details>
<summary>Editorial</summary>

This is grid Dijkstra with a **state dimension** for teleporter usage. The key insight: the same cell reachable without having teleported vs. with having teleported are *different states* — the cheaper path without teleporting might not be the winner once we add the one-shot teleport option. So `minCost` becomes a 3D array `[rows][cols][2]` where the last index is `teleporter_used` (0 or 1).

Two types of transitions from state `(row, col, teleporter_used)`:

1. **Cardinal move** to a valid adjacent cell at cost + 1, same `teleporter_used`.
2. **Teleport** (only when `grid[row][col] > 1` and `teleporter_used == 0`): relax all other cells with the same teleporter ID at cost + 1, with `teleporter_used = 1`.

Standard Dijkstra machinery (min-heap, lazy stale-skip, early return on destination pop) handles everything else.

```python solution time=O(R·C·log(R·C)) space=O(R·C)
import ast, heapq

class Cell:
    def __init__(self, row, col, cost, teleporter_used):
        self.row = row; self.col = col; self.cost = cost; self.teleporter_used = teleporter_used
    def __lt__(self, other): return self.cost < other.cost

class Solution:
    def is_valid_cell(self, grid, row, col):
        return 0 <= row < len(grid) and 0 <= col < len(grid[0]) and grid[row][col] != 0

    def build_teleporter_map(self, grid):
        teleporters = {}
        for row in range(len(grid)):
            for col in range(len(grid[row])):
                if grid[row][col] > 1:
                    teleporters.setdefault(grid[row][col], []).append((row, col))
        return teleporters

    def teleporter_grid(self, grid, source, destination):
        rows = len(grid)
        if rows == 0:
            return -1
        cols = len(grid[0])
        min_cost = [[[float("inf")] * 2 for _ in range(cols)] for _ in range(rows)]
        teleporters = self.build_teleporter_map(grid)
        pq = []
        min_cost[source[0]][source[1]][0] = 0
        heapq.heappush(pq, Cell(source[0], source[1], 0, 0))
        directions = [(-1, 0), (0, 1), (1, 0), (0, -1)]
        while pq:
            curr_cell = heapq.heappop(pq)
            curr_row = curr_cell.row; curr_col = curr_cell.col
            cost = curr_cell.cost; teleporter_used = curr_cell.teleporter_used
            if curr_row == destination[0] and curr_col == destination[1]:
                return cost
            if cost > min_cost[curr_row][curr_col][teleporter_used]:
                continue
            for dr, dc in directions:
                new_row = curr_row + dr; new_col = curr_col + dc
                if self.is_valid_cell(grid, new_row, new_col):
                    new_cost = cost + 1
                    if new_cost < min_cost[new_row][new_col][teleporter_used]:
                        min_cost[new_row][new_col][teleporter_used] = new_cost
                        heapq.heappush(pq, Cell(new_row, new_col, new_cost, teleporter_used))
            if grid[curr_row][curr_col] > 1 and teleporter_used == 0:
                teleporter_id = grid[curr_row][curr_col]
                for new_row, new_col in teleporters.get(teleporter_id, []):
                    if new_row == curr_row and new_col == curr_col:
                        continue
                    new_cost = cost + 1
                    if new_cost < min_cost[new_row][new_col][1]:
                        min_cost[new_row][new_col][1] = new_cost
                        heapq.heappush(pq, Cell(new_row, new_col, new_cost, 1))
        return -1

grid = ast.literal_eval(input())
source = ast.literal_eval(input())
destination = ast.literal_eval(input())
print(Solution().teleporter_grid(grid, source, destination))
```

```java solution
import java.util.*;

public class Main {
    static class Cell {
        int row, col, cost, teleporterUsed;
        Cell(int row, int col, int cost, int teleporterUsed) {
            this.row = row; this.col = col; this.cost = cost; this.teleporterUsed = teleporterUsed;
        }
    }

    static class Solution {
        private boolean isValidCell(int[][] grid, int row, int col) {
            return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length && grid[row][col] != 0;
        }

        private Map<Integer, List<int[]>> buildTeleporterMap(int[][] grid) {
            Map<Integer, List<int[]>> teleporters = new HashMap<>();
            for (int row = 0; row < grid.length; row++)
                for (int col = 0; col < grid[row].length; col++)
                    if (grid[row][col] > 1)
                        teleporters.computeIfAbsent(grid[row][col], k -> new ArrayList<>()).add(new int[]{row, col});
            return teleporters;
        }

        public int teleporterGrid(int[][] grid, int[] source, int[] destination) {
            int rows = grid.length;
            if (rows == 0) return -1;
            int cols = grid[0].length;
            int[][][] minCost = new int[rows][cols][2];
            for (int[][] arr : minCost) for (int[] a : arr) Arrays.fill(a, Integer.MAX_VALUE);
            Map<Integer, List<int[]>> teleporters = buildTeleporterMap(grid);
            PriorityQueue<Cell> pq = new PriorityQueue<>((a, b) -> a.cost - b.cost);
            minCost[source[0]][source[1]][0] = 0;
            pq.add(new Cell(source[0], source[1], 0, 0));
            int[][] dirs = {{-1,0},{0,1},{1,0},{0,-1}};
            while (!pq.isEmpty()) {
                Cell cur = pq.poll();
                if (cur.row == destination[0] && cur.col == destination[1]) return cur.cost;
                if (cur.cost > minCost[cur.row][cur.col][cur.teleporterUsed]) continue;
                for (int[] d : dirs) {
                    int nr = cur.row + d[0], nc = cur.col + d[1];
                    if (isValidCell(grid, nr, nc)) {
                        int newCost = cur.cost + 1;
                        if (newCost < minCost[nr][nc][cur.teleporterUsed]) {
                            minCost[nr][nc][cur.teleporterUsed] = newCost;
                            pq.add(new Cell(nr, nc, newCost, cur.teleporterUsed));
                        }
                    }
                }
                if (grid[cur.row][cur.col] > 1 && cur.teleporterUsed == 0) {
                    int tid = grid[cur.row][cur.col];
                    for (int[] tp : teleporters.getOrDefault(tid, Collections.emptyList())) {
                        if (tp[0] == cur.row && tp[1] == cur.col) continue;
                        int newCost = cur.cost + 1;
                        if (newCost < minCost[tp[0]][tp[1]][1]) {
                            minCost[tp[0]][tp[1]][1] = newCost;
                            pq.add(new Cell(tp[0], tp[1], newCost, 1));
                        }
                    }
                }
            }
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        int[] source = parseIntArray(sc.nextLine());
        int[] destination = parseIntArray(sc.nextLine());
        System.out.println(new Solution().teleporterGrid(grid, source, destination));
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

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }
}
```

</details>
