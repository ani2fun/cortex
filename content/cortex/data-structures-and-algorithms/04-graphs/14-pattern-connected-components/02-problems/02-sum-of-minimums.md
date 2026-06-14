---
title: "Sum of Minimums"
summary: "For each connected component, find the minimum value among its nodes. Return the sum of those minima across all components."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
kind: problem
topics: [connected-components, graph]
---

# Problem: Sum of Minimums

## Problem Statement

Given an undirected graph (as an adjacency list) and a `values` array (one integer per node), find the **minimum value** within each connected component. Return the **sum of those per-component minima** across all components.

## Examples

**Example 1:**
```
Input:  graph = [[1], [0, 4], [3], [2], [1]],
        values = [2, 5, 1, 6, 7]
Output: 3
```

Component `{0, 1, 4}` has `min(2, 5, 7) = 2`. Component `{2, 3}` has `min(1, 6) = 1`. Sum = 3.

**Example 2:**
```
Input:  graph = [[1], [0], [], [4], [3]],
        values = [2, 5, 1, 6, 7]
Output: 9
```

Three components: `{0, 1}` → min 2, `{2}` → min 1, `{3, 4}` → min 6. Sum = 9.

## Constraints

- `0 ≤ n ≤ 10⁴` nodes
- `-10⁴ ≤ values[i] ≤ 10⁴`
- Empty graph returns 0

```python run viz=graph viz-root=graph
import ast

def sum_of_minimums(graph, values):
    # Your code goes here
    return 0

graph = ast.literal_eval(input())
values = ast.literal_eval(input())
print(sum_of_minimums(graph, values))
```

```java run viz=graph viz-root=graph
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

    static int[] parseIntArray(String line) {
        String s = line.replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] a = new int[parts.length];
        for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
        return a;
    }

    static int sumOfMinimums(int[][] graph, int[] values) {
        // Your code goes here
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        int[] values = parseIntArray(sc.nextLine());
        System.out.println(sumOfMinimums(graph, values));
    }
}
```

```testcases
{
  "args": [
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1], [0, 4], [3], [2], [1]]" },
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[2, 5, 1, 6, 7]" }
  ],
  "cases": [
    { "args": { "graph": "[[1], [0, 4], [3], [2], [1]]", "values": "[2, 5, 1, 6, 7]" }, "expected": "3" },
    { "args": { "graph": "[[1], [0], [], [4], [3]]", "values": "[2, 5, 1, 6, 7]" }, "expected": "9" },
    { "args": { "graph": "[]", "values": "[]" }, "expected": "0" },
    { "args": { "graph": "[[1], [0]]", "values": "[3, 7]" }, "expected": "3" },
    { "args": { "graph": "[[], [], []]", "values": "[4, 2, 9]" }, "expected": "15" },
    { "args": { "graph": "[[1, 2], [0], [0]]", "values": "[1, 2, 3]" }, "expected": "1" }
  ]
}
```

<details>
<summary>Editorial</summary>

**Approach:** the DFS now *returns* the component minimum instead of building a list. On entry, `minimum_so_far = values[node]`; for each unvisited neighbour, recurse and take `min(minimum_so_far, returned_min)`. The outer loop accumulates those per-component minima into a sum. This is the `f = min, g = sum` specialisation of the connected-components template — the structure is otherwise identical to the standard flood-fill.

```python solution time=O(V+E) space=O(V)
import ast

def dfs(graph, node, visited, values):
    visited.add(node)
    minimum_so_far = values[node]
    for neighbour in graph[node]:
        if neighbour not in visited:
            min_val = dfs(graph, neighbour, visited, values)
            minimum_so_far = min(minimum_so_far, min_val)
    return minimum_so_far

def sum_of_minimums(graph, values):
    n = len(graph)
    if n == 0:
        return 0
    visited = set()
    min_sum = 0
    for node in range(n):
        if node in visited:
            continue
        min_val = dfs(graph, node, visited, values)
        min_sum += min_val
    return min_sum

graph = ast.literal_eval(input())
values = ast.literal_eval(input())
print(sum_of_minimums(graph, values))
```

```java solution time=O(V+E) space=O(V)
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

    static int[] parseIntArray(String line) {
        String s = line.replaceAll("[\\[\\]\\s]", "");
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] a = new int[parts.length];
        for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i].trim());
        return a;
    }

    static int[] values;

    static int dfs(int[][] graph, int node, boolean[] visited) {
        visited[node] = true;
        int minimumSoFar = values[node];
        for (int nbr : graph[node]) {
            if (!visited[nbr]) {
                int minVal = dfs(graph, nbr, visited);
                minimumSoFar = Math.min(minimumSoFar, minVal);
            }
        }
        return minimumSoFar;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        values = parseIntArray(sc.nextLine());
        int n = graph.length;
        if (n == 0) { System.out.println(0); return; }
        boolean[] visited = new boolean[n];
        int minSum = 0;
        for (int node = 0; node < n; node++) {
            if (!visited[node]) minSum += dfs(graph, node, visited);
        }
        System.out.println(minSum);
    }
}
```

</details>
