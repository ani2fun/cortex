---
title: "Minimum Steps in a Grid"
summary: "In an N×M grid where 1 = walkable and 0 = wall, find the minimum number of cardinal-direction moves from (0, 0) to (N-1, M-1). Return -1 if no path exists."
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: medium
kind: problem
topics: [shortest-path-bfs, graph]
---

# Problem: Minimum Steps in a Grid

## Problem Statement

In an N×M grid where `1` = walkable and `0` = wall, find the minimum number of cardinal-direction moves from `(0, 0)` to `(N-1, M-1)`. Return `-1` if no path exists.

## Examples

**Example 1:**
```
Input:  grid = [[1, 0, 1, 1],
                [1, 1, 1, 1],
                [0, 1, 0, 1]]
Output: 5
```

**Example 2:**
```
Input:  grid = [[1, 1, 1, 1],
                [1, 1, 1, 1],
                [1, 1, 0, 1]]
Output: 5
```

## Constraints

- `1 ≤ N, M ≤ 100`
- Grid cells are either `0` (wall) or `1` (walkable)
- Return `-1` if the start or end is a wall, or no path exists

```python run viz=grid viz-root=grid
import ast
from collections import deque

class Solution:
    def minimum_steps_in_a_grid(self, grid):
        # Your code goes here — BFS from (0,0) to (N-1,M-1) through 1-cells,
        # 4 cardinal directions. Carry step count in each queue entry. Return -1
        # if the start or end is blocked or no path exists.
        return -1

grid = ast.literal_eval(input())
print(Solution().minimum_steps_in_a_grid(grid))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        public int minimumStepsInAGrid(int[][] grid) {
            // Your code goes here — BFS from (0,0) to (N-1,M-1) through 1-cells,
            // 4 cardinal directions. Carry step count in each queue entry. Return -1
            // if the start or end is blocked or no path exists.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumStepsInAGrid(grid));
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
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[1, 0, 1, 1], [1, 1, 1, 1], [0, 1, 0, 1]]" }
  ],
  "cases": [
    { "args": { "grid": "[[1, 0, 1, 1], [1, 1, 1, 1], [0, 1, 0, 1]]" }, "expected": "5" },
    { "args": { "grid": "[[1, 1, 1, 1], [1, 1, 1, 1], [1, 1, 0, 1]]" }, "expected": "5" },
    { "args": { "grid": "[[1]]" }, "expected": "0" },
    { "args": { "grid": "[[0]]" }, "expected": "-1" },
    { "args": { "grid": "[[1, 1], [1, 1]]" }, "expected": "2" },
    { "args": { "grid": "[[1, 0], [0, 1]]" }, "expected": "-1" },
    { "args": { "grid": "[[0, 1, 1], [1, 1, 1], [1, 1, 1]]" }, "expected": "-1" },
    { "args": { "grid": "[[1, 1, 1], [1, 1, 1], [1, 1, 0]]" }, "expected": "-1" }
  ]
}
```

<details>
<summary>Editorial</summary>

The grid is an implicit unweighted graph. BFS from `(0, 0)` carries the step count in each queue entry; the first time `(N-1, M-1)` is dequeued, that count is the minimum. Mark cells visited at push time (not pop) to prevent re-enqueueing. Check validity — within bounds and `grid[r][c] == 1` — before pushing a neighbour. A blocked start or end is an immediate `-1` return.

```python solution time=O(N*M) space=O(N*M)
import ast
from collections import deque

class Solution:
    def minimum_steps_in_a_grid(self, grid):
        rows, cols = len(grid), len(grid[0])
        if grid[0][0] == 0 or grid[rows-1][cols-1] == 0:
            return -1
        visited = [[False] * cols for _ in range(rows)]
        queue = deque([(0, 0, 0)])
        visited[0][0] = True
        directions = [(-1, 0), (0, 1), (1, 0), (0, -1)]
        while queue:
            r, c, steps = queue.popleft()
            if r == rows - 1 and c == cols - 1:
                return steps
            for dr, dc in directions:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == 1 and not visited[nr][nc]:
                    visited[nr][nc] = True
                    queue.append((nr, nc, steps + 1))
        return -1

grid = ast.literal_eval(input())
print(Solution().minimum_steps_in_a_grid(grid))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int minimumStepsInAGrid(int[][] grid) {
            int rows = grid.length, cols = grid[0].length;
            if (grid[0][0] == 0 || grid[rows-1][cols-1] == 0) return -1;
            boolean[][] visited = new boolean[rows][cols];
            Deque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{0, 0, 0});
            visited[0][0] = true;
            int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                int r = cur[0], c = cur[1], steps = cur[2];
                if (r == rows - 1 && c == cols - 1) return steps;
                for (int[] dir : dirs) {
                    int nr = r + dir[0], nc = c + dir[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                            && grid[nr][nc] == 1 && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc, steps + 1});
                    }
                }
            }
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().minimumStepsInAGrid(grid));
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
