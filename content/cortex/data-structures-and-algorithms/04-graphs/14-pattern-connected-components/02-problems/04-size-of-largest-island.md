---
title: "Size of Largest Island"
summary: "Same grid, same 8-direction connectivity. Now return the size (cell count) of the *largest* island."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
kind: problem
topics: [connected-components, graph]
---

# Problem: Size of Largest Island

## Problem Statement

A grid of `0`s and `1`s with 8-direction connectivity (same as Island Count). Now return the **size** (cell count) of the *largest* island. Return `0` if there are no land cells.

## Examples

**Example 1:**
```
Input:  grid = [[1, 1, 0, 0],
                [0, 0, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 6
```

The large connected blob (rows 0–3, right side) spans 6 cells; the top-left pair is only 2. Largest = 6.

**Example 2:**
```
Input:  grid = [[1, 1, 0, 0],
                [0, 1, 1, 1],
                [1, 0, 1, 1],
                [1, 0, 0, 0]]
Output: 9
```

All 9 land cells connect into one island.

## Constraints

- `0 ≤ R, C ≤ 300`
- `grid[r][c] ∈ {0, 1}`
- 8-direction connectivity (diagonals count)
- Return `0` for an all-water grid

```python run viz=grid viz-root=grid
import ast

def size_of_largest_island(grid):
    # Your code goes here
    return 0

grid = ast.literal_eval(input())
print(size_of_largest_island(grid))
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

    static int sizeOfLargestIsland(int[][] grid) {
        // Your code goes here
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(sizeOfLargestIsland(grid));
    }
}
```

```testcases
{
  "args": [
    { "id": "grid", "label": "grid", "type": "int[][]", "placeholder": "[[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }
  ],
  "cases": [
    { "args": { "grid": "[[1, 1, 0, 0], [0, 0, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }, "expected": "6" },
    { "args": { "grid": "[[1, 1, 0, 0], [0, 1, 1, 1], [1, 0, 1, 1], [1, 0, 0, 0]]" }, "expected": "9" },
    { "args": { "grid": "[]" }, "expected": "0" },
    { "args": { "grid": "[[0]]" }, "expected": "0" },
    { "args": { "grid": "[[1]]" }, "expected": "1" },
    { "args": { "grid": "[[1, 1], [1, 1]]" }, "expected": "4" },
    { "args": { "grid": "[[1, 0, 1], [0, 0, 0], [1, 0, 1]]" }, "expected": "1" },
    { "args": { "grid": "[[0, 0], [0, 0]]" }, "expected": "0" },
    { "args": { "grid": "[[1, 0, 0], [0, 1, 0], [0, 0, 1]]" }, "expected": "3" }
  ]
}
```

<details>
<summary>Editorial</summary>

**Approach:** a tiny variation on Island Count — the DFS now *returns* the component size rather than just marking. On entry, `size = 1`; for each unvisited land neighbour, `size += dfs(neighbour)`. The outer loop tracks `max(largest, returned_size)` instead of incrementing a count. This is the `f = +1, g = max` specialisation of the connected-components template.

```python solution time=O(R×C) space=O(R×C)
import ast

def size_of_largest_island(grid):
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
        size = 1
        for dr, dc in directions:
            nr, nc = r + dr, c + dc
            if is_valid(nr, nc) and not visited[nr][nc]:
                size += dfs(nr, nc)
        return size

    largest = 0
    for row in range(rows):
        for col in range(cols):
            if grid[row][col] == 0 or visited[row][col]:
                continue
            largest = max(largest, dfs(row, col))
    return largest

grid = ast.literal_eval(input())
print(size_of_largest_island(grid))
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

    static int dfs(int[][] grid, int r, int c) {
        visited[r][c] = true;
        int size = 1;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (isValid(grid, nr, nc) && !visited[nr][nc]) size += dfs(grid, nr, nc);
        }
        return size;
    }

    static int sizeOfLargestIsland(int[][] grid) {
        if (grid.length == 0) return 0;
        rows = grid.length; cols = grid[0].length;
        visited = new boolean[rows][cols];
        int largest = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == 1 && !visited[r][c])
                    largest = Math.max(largest, dfs(grid, r, c));
        return largest;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] grid = parseIntMatrix(sc.nextLine());
        System.out.println(sizeOfLargestIsland(grid));
    }
}
```

**Complexity:**

| Problem | Time | Space |
|---|---|---|
| Connected components | O(V + E) | O(V) |
| Sum of minimums | O(V + E) | O(V) |
| Island count | O(R × C) | O(R × C) |
| Size of largest island | O(R × C) | O(R × C) |

Each cell or node is visited exactly once. The outer-loop iterations don't multiply work — they just spread it across components, so the total remains linear in input size regardless of how many components exist.

> **Key Takeaway.** The connected-components pattern is a tiny structural addition over a plain traversal: **a per-component aggregate that resets between components**. Once you see this two-level structure, dozens of "find / count / process every group" problems fold into the same template. The pattern works equally on graphs (adjacency list) and grids (direction array). 4-direction or 8-direction connectivity is a trivial change. The choice between DFS and BFS doesn't matter — both walk the component once, in different orders.

</details>
