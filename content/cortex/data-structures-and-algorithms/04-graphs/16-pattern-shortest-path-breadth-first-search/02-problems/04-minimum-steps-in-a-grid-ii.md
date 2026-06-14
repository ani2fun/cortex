---
title: "Minimum Steps in a Grid II"
summary: "Given an NxM grid of 0s and 1s and a non-negative integer k, find the minimum steps from (0,0) to (N-1,M-1) with at most k wall removals allowed."
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: hard
kind: problem
topics: [shortest-path-bfs, graph]
---

# Problem: Minimum Steps in a Grid II

## Problem Statement

Given an **N×M** grid where `1` = walkable and `0` = wall, and a non-negative integer **k**, return the minimum number of steps from `(0, 0)` to `(N-1, M-1)`. You may convert **at most k** wall cells to walkable. Return `-1` if unreachable.

You can only move in the four cardinal directions.

## Examples

**Example 1:**
```
Input:  grid = [[1, 0, 1, 1],
                [0, 1, 1, 1],
                [0, 1, 0, 1]], k = 1
Output: 5
```

**Example 2:**
```
Input:  grid = [[1, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 1]], k = 5
Output: 5
```

## Constraints

- `1 ≤ N, M ≤ 40`
- `0 ≤ k ≤ N × M`
- Grid cells are `0` (wall) or `1` (walkable)
- Return `-1` if no path exists even with k removals

```python run viz=grid viz-root=grid
import ast

class Solution:
    def minimum_steps_in_a_grid_ii(self, grid, k):
        # Your code goes here — BFS with state (row, col, walls_left).
        # Moving onto a 0-cell costs one removal; a 1-cell costs none.
        # visited[row][col][walls_left] prevents re-visiting the same state.
        return -1

grid = ast.literal_eval(input())
k = int(input())
print(Solution().minimum_steps_in_a_grid_ii(grid, k))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        public int minimumStepsInAGridII(int[][] grid, int k) {
            // Your code goes here — BFS with state (row, col, wallsLeft).
            // Moving onto a 0-cell costs one removal; a 1-cell costs none.
            // visited[row][col][wallsLeft] prevents re-visiting the same state.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumStepsInAGridII(grid, k));
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
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[1, 0, 1, 1], [0, 1, 1, 1], [0, 1, 0, 1]]" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "1" }
  ],
  "cases": [
    { "args": { "grid": "[[1, 0, 1, 1], [0, 1, 1, 1], [0, 1, 0, 1]]", "k": "1" }, "expected": "5" },
    { "args": { "grid": "[[1, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 1]]", "k": "5" }, "expected": "5" },
    { "args": { "grid": "[[1]]", "k": "0" }, "expected": "0" },
    { "args": { "grid": "[[0]]", "k": "1" }, "expected": "0" },
    { "args": { "grid": "[[1, 1], [1, 1]]", "k": "0" }, "expected": "2" },
    { "args": { "grid": "[[1, 0], [0, 1]]", "k": "0" }, "expected": "-1" },
    { "args": { "grid": "[[1, 0], [0, 1]]", "k": "1" }, "expected": "2" },
    { "args": { "grid": "[[1, 0, 0], [0, 0, 0], [0, 0, 1]]", "k": "0" }, "expected": "-1" }
  ]
}
```

<details>
<summary>Editorial</summary>

A plain `visited[row][col]` is no longer enough — the same cell reached with more wall-removals remaining may enable shorter future routes. The fix is to expand the BFS state to `(row, col, walls_left)`, with a 3D visited array `visited[row][col][walls_left]`. Moving onto a `0`-cell decrements `walls_left` by 1; moving onto a `1`-cell leaves it unchanged. A neighbour is only enqueued when `walls_left ≥ 0` and that exact `(row, col, walls_left)` triple hasn't been seen. Because every step still costs 1, FIFO order guarantees the first time `(N-1, M-1)` is dequeued is the minimum step count.

```python solution time=O(N*M*k) space=O(N*M*k)
import ast
from collections import deque

class Solution:
    def minimum_steps_in_a_grid_ii(self, grid, k):
        rows, cols = len(grid), len(grid[0])
        visited = [[[False] * (k + 1) for _ in range(cols)] for _ in range(rows)]
        queue = deque([(0, 0, 0, k)])   # (row, col, steps, walls_left)
        visited[0][0][k] = True
        dirs = [(-1, 0), (0, 1), (1, 0), (0, -1)]
        while queue:
            r, c, steps, walls_left = queue.popleft()
            if r == rows - 1 and c == cols - 1:
                return steps
            for dr, dc in dirs:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols:
                    new_walls = walls_left - (1 if grid[nr][nc] == 0 else 0)
                    if new_walls >= 0 and not visited[nr][nc][new_walls]:
                        visited[nr][nc][new_walls] = True
                        queue.append((nr, nc, steps + 1, new_walls))
        return -1

grid = ast.literal_eval(input())
k = int(input())
print(Solution().minimum_steps_in_a_grid_ii(grid, k))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int minimumStepsInAGridII(int[][] grid, int k) {
            int rows = grid.length, cols = grid[0].length;
            boolean[][][] visited = new boolean[rows][cols][k + 1];
            Deque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{0, 0, 0, k});  // row, col, steps, wallsLeft
            visited[0][0][k] = true;
            int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                int r = cur[0], c = cur[1], steps = cur[2], wallsLeft = cur[3];
                if (r == rows - 1 && c == cols - 1) return steps;
                for (int[] dir : dirs) {
                    int nr = r + dir[0], nc = c + dir[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        int newWalls = wallsLeft - (grid[nr][nc] == 0 ? 1 : 0);
                        if (newWalls >= 0 && !visited[nr][nc][newWalls]) {
                            visited[nr][nc][newWalls] = true;
                            queue.add(new int[]{nr, nc, steps + 1, newWalls});
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
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumStepsInAGridII(grid, k));
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
