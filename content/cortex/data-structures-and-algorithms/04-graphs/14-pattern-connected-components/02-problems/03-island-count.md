---
title: "Island Count"
summary: "A grid of 0s and 1s. 1 = land, 0 = water. An island is a maximal group of connected 1s. Two land cells are connected if they're adjacent in any of 8 directions (cardinals + diagonals)."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
kind: problem
topics: [connected-components, graph]
---

# Problem: Island Count

## Problem Statement

A grid of `0`s and `1`s. `1` = land, `0` = water. An **island** is a maximal group of connected `1`s. Two land cells are connected if they're adjacent in **any of 8 directions** (cardinals + diagonals).

Return the number of islands.

## Examples

**Example 1:**
```
Input:  grid = [[1, 1, 0, 0],
                [0, 0, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 2
```

The top-left `1,1` pair is one island; the remaining `1`s form a single connected blob (they touch diagonally at row 1 col 2 / row 2 col 2).

**Example 2:**
```
Input:  grid = [[1, 1, 0, 0],
                [0, 1, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 1
```

All land cells connect into one island.

## Constraints

- `0 ≤ R, C ≤ 300`
- `grid[r][c] ∈ {0, 1}`
- 8-direction connectivity (diagonals count)

```python run viz=grid viz-root=grid
import ast

def island_count(grid):
    # Your code goes here
    return 0

grid = ast.literal_eval(input())
print(island_count(grid))
```

```java run viz=grid viz-root=grid
import java.util.*;

public class Main {
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

    static int islandCount(int[][] grid) {
        // Your code goes here
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(islandCount(grid));
    }
}
```

```testcases
{
  "args": [
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }
  ],
  "cases": [
    { "args": { "grid": "[[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }, "expected": "2" },
    { "args": { "grid": "[[1, 1, 0, 0], [0, 1, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }, "expected": "1" },
    { "args": { "grid": "[]" }, "expected": "0" },
    { "args": { "grid": "[[0]]" }, "expected": "0" },
    { "args": { "grid": "[[1]]" }, "expected": "1" },
    { "args": { "grid": "[[0, 0, 0], [0, 0, 0]]" }, "expected": "0" },
    { "args": { "grid": "[[1, 1], [1, 1]]" }, "expected": "1" },
    { "args": { "grid": "[[1, 0, 1], [0, 0, 0], [1, 0, 1]]" }, "expected": "4" },
    { "args": { "grid": "[[1, 0, 0], [0, 1, 0], [0, 0, 1]]" }, "expected": "1" }
  ]
}
```

<details>
<summary>Editorial</summary>

**Approach:** the grid is a graph in disguise — each `1` cell is a node, and the 8 direction deltas define the edges. Apply the standard connected-components flood-fill: outer loop over every cell; when a land cell is found unvisited, increment the count and DFS to mark the whole island as visited. The `visited` boolean matrix plays the role of the global `visited` set from the pattern. The only structural change from 4-direction grids is the 8-entry direction array.

```python solution time=O(R×C) space=O(R×C)
import ast

def island_count(grid):
    rows = len(grid)
    if rows == 0:
        return 0
    cols = len(grid[0])
    directions = [(-1,0),(-1,1),(0,1),(1,1),(1,0),(1,-1),(0,-1),(-1,-1)]
    visited = [[False] * cols for _ in range(rows)]

    def is_valid(r, c):
        return 0 <= r < rows and 0 <= c < cols and grid[r][c] == 1

    def dfs(r, c):
        visited[r][c] = True
        for dr, dc in directions:
            nr, nc = r + dr, c + dc
            if is_valid(nr, nc) and not visited[nr][nc]:
                dfs(nr, nc)

    islands = 0
    for row in range(rows):
        for col in range(cols):
            if grid[row][col] == 0 or visited[row][col]:
                continue
            islands += 1
            dfs(row, col)
    return islands

grid = ast.literal_eval(input())
print(island_count(grid))
```

```java solution time=O(R×C) space=O(R×C)
import java.util.*;

public class Main {
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

    static int rows, cols;
    static boolean[][] visited;
    static int[][] DIRS = {{-1,0},{-1,1},{0,1},{1,1},{1,0},{1,-1},{0,-1},{-1,-1}};

    static boolean isValid(int[][] grid, int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols && grid[r][c] == 1;
    }

    static void dfs(int[][] grid, int r, int c) {
        visited[r][c] = true;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (isValid(grid, nr, nc) && !visited[nr][nc]) dfs(grid, nr, nc);
        }
    }

    static int islandCount(int[][] grid) {
        if (grid.length == 0) return 0;
        rows = grid.length; cols = grid[0].length;
        visited = new boolean[rows][cols];
        int count = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == 1 && !visited[r][c]) { count++; dfs(grid, r, c); }
        return count;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(islandCount(grid));
    }
}
```

</details>
