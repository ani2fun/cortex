---
title: "Group Colourable"
summary: "Given an undirected graph and a list of groups, return true if the graph can be 2-coloured such that all nodes in every group share the same colour."
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: hard
kind: problem
topics: [two-colouring, graph]
---

# Problem: Group Colourable

## Problem Statement

Given an **undirected graph** as an adjacency list and a list of **groups**, return `true` if the graph can be 2-coloured such that:
- No two adjacent nodes share the same colour.
- All nodes within the same group share the same colour.

## Examples

**Example 1:**
```
Input:  graph = [[1, 3], [0, 2], [1, 3], [0, 2]], groups = [[0, 2], [1, 3]]
Output: true  ({0,2} → colour 1, {1,3} → colour 0; no adjacent pair shares a colour)
```

**Example 2:**
```
Input:  graph = [[1, 3], [0, 2], [1, 3], [0, 2]], groups = [[0, 1], [2, 3]]
Output: false  (nodes 0 and 1 must share a colour, but edge 0–1 requires they differ)
```

## Constraints

- `0 ≤ n ≤ 500` — `n = graph.length`
- Groups are disjoint; each node appears in at most one group
- An empty graph (`n = 0`) returns `false`
- Groups may be empty (`[]`) — no group constraints, pure two-colouring

```python run viz=graph viz-kind=graph
import ast

class Solution:
    def group_colourable(self, graph, groups):
        # Your code goes here — standard two-colouring DFS, but before exploring
        # neighbours, propagate the current node's colour to all group-mates.
        # Fail if a group-mate is already a different colour.
        pass

graph = ast.literal_eval(input())
groups = ast.literal_eval(input())
result = Solution().group_colourable(graph, groups)
print("true" if result else "false")
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static class Solution {
        boolean groupColourable(int[][] graph, int[][] groups) {
            // Your code goes here — two-colour but propagate colour to all
            // group-mates before exploring neighbours.
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        int[][] groups = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().groupColourable(graph, groups));
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
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1, 3], [0, 2], [1, 3], [0, 2]]" },
    { "id": "groups", "label": "groups", "type": "int[][]", "placeholder": "[[0, 2], [1, 3]]" }
  ],
  "cases": [
    { "args": { "graph": "[[1, 3], [0, 2], [1, 3], [0, 2]]", "groups": "[[0, 2], [1, 3]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 3], [0, 2], [1, 3], [0, 2]]", "groups": "[[0, 1], [2, 3]]" }, "expected": "false" },
    { "args": { "graph": "[[1], [0]]", "groups": "[[0, 1]]" }, "expected": "false" },
    { "args": { "graph": "[[1], [0]]", "groups": "[[0], [1]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 3], [0, 2], [1, 3], [0, 2]]", "groups": "[]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2], [0, 2], [0, 1]]", "groups": "[[0, 2], [1]]" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

Two-colouring with an extra group constraint. Before exploring a node's neighbours, propagate the node's colour to all its group-mates: if a group-mate is uncoloured, colour it the same; if already a different colour, fail immediately. After that, continue with the standard conflict check on neighbours. A `group_map` precomputes each node's group-mates for O(1) lookup.

```python solution time=O(V + E + G) space=O(V + G)
import ast

class Solution:
    def color_group(self, node, colour, colour_value, group_map):
        if node in group_map:
            for group_node in group_map[node]:
                if group_node not in colour:
                    colour[group_node] = colour_value
                elif colour[group_node] != colour_value:
                    return False
        return True

    def colour_graph(self, graph, node, colour, colour_value, group_map):
        colour[node] = colour_value
        if not self.color_group(node, colour, colour_value, group_map):
            return False
        for neighbour in graph[node]:
            if neighbour not in colour:
                if not self.colour_graph(graph, neighbour, colour, 1 - colour_value, group_map):
                    return False
            elif colour[neighbour] == colour_value:
                return False
        return True

    def group_colourable(self, graph, groups):
        n = len(graph)
        if n == 0:
            return False
        colour = {}
        group_map = {}
        for group in groups:
            for node in group:
                group_map[node] = group
        for node in range(n):
            if node not in colour:
                if not self.colour_graph(graph, node, colour, 1, group_map):
                    return False
        return True

graph = ast.literal_eval(input())
groups = ast.literal_eval(input())
result = Solution().group_colourable(graph, groups)
print("true" if result else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        private boolean colorGroup(int node, int[] colour, int colourValue, Map<Integer, int[]> groupMap) {
            if (groupMap.containsKey(node)) {
                for (int groupNode : groupMap.get(node)) {
                    if (colour[groupNode] == -1) {
                        colour[groupNode] = colourValue;
                    } else if (colour[groupNode] != colourValue) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean colourGraph(int[][] graph, int node, int[] colour, int colourValue, Map<Integer, int[]> groupMap) {
            colour[node] = colourValue;
            if (!colorGroup(node, colour, colourValue, groupMap)) return false;
            for (int neighbour : graph[node]) {
                if (colour[neighbour] == -1) {
                    if (!colourGraph(graph, neighbour, colour, 1 - colourValue, groupMap)) return false;
                } else if (colour[neighbour] == colourValue) {
                    return false;
                }
            }
            return true;
        }

        boolean groupColourable(int[][] graph, int[][] groups) {
            int n = graph.length;
            if (n == 0) return false;
            int[] colour = new int[n];
            Arrays.fill(colour, -1);
            Map<Integer, int[]> groupMap = new HashMap<>();
            for (int[] group : groups) {
                for (int node : group) {
                    groupMap.put(node, group);
                }
            }
            for (int node = 0; node < n; node++) {
                if (colour[node] == -1) {
                    if (!colourGraph(graph, node, colour, 1, groupMap)) return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        int[][] groups = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().groupColourable(graph, groups));
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
