---
title: "Nearest Distance"
summary: "Grid of 0s and 1s. For every cell, return its distance to the nearest 1 (Manhattan distance, only horizontal/vertical movement)."
prereqs:
  - 16-pattern-shortest-path-breadth-first-search/01-pattern
difficulty: medium
kind: problem
topics: [shortest-path-bfs, multi-source-bfs, graph]
---

# Problem: Nearest Distance (Multi-Source BFS)

## Problem Statement

Given a grid of `0`s and `1`s, for *every* cell return its shortest distance to the nearest `1` (horizontal/vertical moves only). Cells containing `1` have distance `0`.

## Examples

**Example 1:**
```
Input:  grid = [[0, 0, 0, 0],
                [0, 0, 1, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]]
Output: [[3, 2, 1, 2],
         [2, 1, 0, 1],
         [3, 2, 1, 2],
         [4, 3, 2, 3]]
```

**Example 2:**
```
Input:  grid = [[1, 0, 0],
                [0, 1, 0],
                [0, 0, 0]]
Output: [[0, 1, 2],
         [1, 0, 1],
         [2, 1, 2]]
```

## Constraints

- `1 ≤ rows, cols ≤ 100`
- Every cell in the grid is `0` or `1`
- At least one cell contains `1` (so every cell is reachable)
- `O(rows × cols)` time — multi-source BFS, not repeated single-source BFS

```python run viz=grid viz-root=grid
import ast
from collections import deque

class Solution:
    def nearest_distance(self, grid):
        # Your code goes here — seed BFS with ALL cells containing 1 at distance 0
        # simultaneously, then expand outward. The first time any wave reaches a cell,
        # that's its distance to the nearest 1. Return the result grid.
        return grid

grid = ast.literal_eval(input())
print(Solution().nearest_distance(grid))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
    static class Solution {
        public int[][] nearestDistance(int[][] grid) {
            // Your code goes here — seed BFS with ALL cells containing 1 at distance 0
            // simultaneously, then expand outward. The first time any wave reaches a cell,
            // that's its distance to the nearest 1. Return the result grid.
            return grid;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        int[][] result = new Solution().nearestDistance(grid);
        // print as list-of-lists matching Python's print(list_of_lists)
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.length; i++) {
            sb.append("[");
            for (int j = 0; j < result[i].length; j++) {
                sb.append(result[i][j]);
                if (j < result[i].length - 1) sb.append(", ");
            }
            sb.append("]");
            if (i < result.length - 1) sb.append(", ");
        }
        sb.append("]");
        System.out.println(sb);
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
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[0, 0, 0, 0], [0, 0, 1, 0], [0, 0, 0, 0], [0, 0, 0, 0]]" }
  ],
  "cases": [
    { "args": { "grid": "[[0, 0, 0, 0], [0, 0, 1, 0], [0, 0, 0, 0], [0, 0, 0, 0]]" }, "expected": "[[3, 2, 1, 2], [2, 1, 0, 1], [3, 2, 1, 2], [4, 3, 2, 3]]" },
    { "args": { "grid": "[[1, 0, 0], [0, 1, 0], [0, 0, 0]]" }, "expected": "[[0, 1, 2], [1, 0, 1], [2, 1, 2]]" },
    { "args": { "grid": "[[1]]" }, "expected": "[[0]]" },
    { "args": { "grid": "[[1, 1], [1, 1]]" }, "expected": "[[0, 0], [0, 0]]" },
    { "args": { "grid": "[[1, 0], [0, 0]]" }, "expected": "[[0, 1], [1, 2]]" },
    { "args": { "grid": "[[0, 0], [0, 1]]" }, "expected": "[[2, 1], [1, 0]]" }
  ]
}
```

<details>
<summary>Editorial</summary>

The key insight is **multi-source BFS**: instead of running BFS separately from each `1` cell (O(K × R × C)), enqueue *all* `1` cells at distance 0 simultaneously and BFS once (O(R × C)). The wave expands outward from every source at once; the first time any wave reaches a cell, that cell's distance to its nearest source is settled. Initialize a result grid to all-zeros (the `1` cells are already correct); update each `0` cell's distance when first reached. Mark on push (updating the result is the mark) to avoid re-enqueueing.

```python solution time=O(R*C) space=O(R*C)
import ast
from collections import deque

class Solution:
    def nearest_distance(self, grid):
        rows, cols = len(grid), len(grid[0])
        result = [[0] * cols for _ in range(rows)]
        queue = deque()
        for r in range(rows):
            for c in range(cols):
                if grid[r][c] == 1:
                    queue.append((r, c, 0))
                else:
                    result[r][c] = -1           # unvisited marker
        dirs = [(-1, 0), (0, 1), (1, 0), (0, -1)]
        while queue:
            r, c, d = queue.popleft()
            for dr, dc in dirs:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols and result[nr][nc] == -1:
                    result[nr][nc] = d + 1
                    queue.append((nr, nc, d + 1))
        return result

grid = ast.literal_eval(input())
print(Solution().nearest_distance(grid))
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        public int[][] nearestDistance(int[][] grid) {
            int rows = grid.length, cols = grid[0].length;
            int[][] result = new int[rows][cols];
            Deque<int[]> queue = new ArrayDeque<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c] == 1) {
                        queue.add(new int[]{r, c, 0});
                    } else {
                        result[r][c] = -1;          // unvisited marker
                    }
                }
            }
            int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                int r = cur[0], c = cur[1], d = cur[2];
                for (int[] dir : dirs) {
                    int nr = r + dir[0], nc = c + dir[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && result[nr][nc] == -1) {
                        result[nr][nc] = d + 1;
                        queue.add(new int[]{nr, nc, d + 1});
                    }
                }
            }
            return result;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        int[][] result = new Solution().nearestDistance(grid);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.length; i++) {
            sb.append("[");
            for (int j = 0; j < result[i].length; j++) {
                sb.append(result[i][j]);
                if (j < result[i].length - 1) sb.append(", ");
            }
            sb.append("]");
            if (i < result.length - 1) sb.append(", ");
        }
        sb.append("]");
        System.out.println(sb);
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
