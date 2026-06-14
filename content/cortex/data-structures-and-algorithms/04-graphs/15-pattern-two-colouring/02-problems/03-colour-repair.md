---
title: "Colour Repair"
summary: "A graph that's almost two-colourable: it can be made bipartite by removing at most one edge. Return true if so, false otherwise."
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: medium
kind: problem
topics: [two-colouring, graph]
---

# Problem: Colour Repair

## Problem Statement

Given an undirected graph as an adjacency list, determine whether it can be made bipartite by removing **at most one** edge. Return `true` if so, `false` otherwise.

A graph that is already bipartite (zero conflicts) trivially passes. A graph with exactly one conflicting edge passes after that edge is removed. Two or more distinct conflict edges cannot be fixed with a single removal.

## Examples

**Example 1:**
```
Input:  graph = [[1, 3], [0, 2, 3], [1, 3], [0, 1, 2]]
Output: true  (remove edge 1–3)
```

**Example 2:**
```
Input:  graph = [[1, 2, 3], [0, 2, 3], [0, 1, 3], [0, 1, 2]]
Output: false  (K₄ — two distinct conflict edges remain)
```

## Constraints

- `0 ≤ n ≤ 2000` — `n = graph.length`
- `0 ≤ graph[u].length ≤ n`
- Graph is undirected — edges appear in both directions in the adjacency list
- An empty graph (`n = 0`) returns `false` (no valid bipartition)

```python run viz=graph viz-kind=graph
import ast

class Solution:
    def colour_repair(self, graph):
        # Your code goes here — run two-colouring but instead of returning False
        # at a conflict, record the conflicting edge. After the full traversal,
        # count distinct conflicts (each undirected edge appears twice → divide by 2).
        # Return true if distinct_conflicts <= 1.
        pass

graph = ast.literal_eval(input())
result = Solution().colour_repair(graph)
print("true" if result else "false")
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static class Solution {
        boolean colourRepair(int[][] graph) {
            // Your code goes here — run two-colouring but record conflicts
            // instead of returning false. Return true if distinct conflicts <= 1.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().colourRepair(graph));
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
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1, 3], [0, 2, 3], [1, 3], [0, 1, 2]]" }
  ],
  "cases": [
    { "args": { "graph": "[[1, 3], [0, 2, 3], [1, 3], [0, 1, 2]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2, 3], [0, 2, 3], [0, 1, 3], [0, 1, 2]]" }, "expected": "false" },
    { "args": { "graph": "[[1, 2], [0, 2], [0, 1]]" }, "expected": "true" },
    { "args": { "graph": "[[1], [0]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2, 3], [0, 2], [0, 1], [0]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2], [0], [0]]" }, "expected": "true" }
  ]
}
```

<details>
<summary>Editorial</summary>

The trick: instead of returning `false` immediately at a colour conflict, **record the conflicting edge** and continue colouring. At the end, count distinct conflicts — since the graph is undirected, each conflicting edge appears twice (once from each endpoint), so divide by 2. If distinct conflicts ≤ 1, the graph can be made bipartite by removing at most one edge.

```python solution time=O(V + E) space=O(V + E)
import ast

class Solution:
    def colour_graph(self, graph, node, colour, colour_value, conflicts):
        colour[node] = colour_value
        for neighbour in graph[node]:
            if neighbour not in colour:
                if not self.colour_graph(graph, neighbour, colour, 1 - colour_value, conflicts):
                    return False
            elif colour.get(neighbour) == colour_value:
                conflicts.append((node, neighbour))
        return True

    def colour_repair(self, graph):
        n = len(graph)
        if n == 0:
            return False
        colour = {}
        conflicts = []
        for node in range(n):
            if node not in colour:
                self.colour_graph(graph, node, colour, 0, conflicts)
        return len(conflicts) // 2 <= 1

graph = ast.literal_eval(input())
result = Solution().colour_repair(graph)
print("true" if result else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private boolean colourGraph(int[][] graph, int node, int[] colour, int colourValue, List<int[]> conflicts) {
            colour[node] = colourValue;
            for (int neighbour : graph[node]) {
                if (colour[neighbour] == -1) {
                    if (!colourGraph(graph, neighbour, colour, 1 - colourValue, conflicts)) return false;
                } else if (colour[neighbour] == colourValue) {
                    conflicts.add(new int[]{node, neighbour});
                }
            }
            return true;
        }

        boolean colourRepair(int[][] graph) {
            int n = graph.length;
            if (n == 0) return false;
            int[] colour = new int[n];
            Arrays.fill(colour, -1);
            List<int[]> conflicts = new ArrayList<>();
            for (int node = 0; node < n; node++) {
                if (colour[node] == -1) {
                    colourGraph(graph, node, colour, 0, conflicts);
                }
            }
            return conflicts.size() / 2 <= 1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().colourRepair(graph));
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
