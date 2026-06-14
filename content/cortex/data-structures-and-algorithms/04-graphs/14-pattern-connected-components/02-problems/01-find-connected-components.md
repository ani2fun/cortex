---
title: "Find Connected Components"
summary: "Given an undirected graph and a values array, return a list of all connected components — but only of the *visitable* nodes (values[i] > 0)."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: easy
kind: problem
topics: [connected-components, graph]
---

# Problem: Find Connected Components

## Problem Statement

Given an undirected graph (as an adjacency list) and a `values` array, return a list of all connected components — but only of the *visitable* nodes (`values[i] > 0`). Nodes with `values[i] == 0` act as blockers: the DFS will not enter them, so a chain of zero-value nodes can split the graph into isolated visitable islands.

## Examples

**Example 1:**
```
Input:  graph = [[1], [0, 2], [1, 3], [2, 4], [3, 5], [4, 6], [5]],
        values = [1, 0, 1, 0, 1, 0, 1]
Output: [[0], [2], [4], [6]]
```

The alternating `0` values in `values` block every connection. Nodes 0, 2, 4, 6 each become an isolated singleton component.

**Example 2:**
```
Input:  graph = [[1], [0], [], [4], [3]],
        values = [1, 1, 1, 1, 1]
Output: [[0, 1], [2], [3, 4]]
```

Node 2 is isolated (no edges); nodes 0–1 and 3–4 form pairs.

## Constraints

- `0 ≤ n ≤ 10⁴` nodes
- `0 ≤ values[i] ≤ 10⁴`
- Output components are sorted internally and sorted by their first element (for determinism)

```python run viz=graph viz-root=graph
import ast

def connected_components(graph, values):
    # Your code goes here
    return []

graph = ast.literal_eval(input())
values = ast.literal_eval(input())
result = connected_components(graph, values)
# sort for determinism
result = [sorted(c) for c in result]
result.sort(key=lambda c: c[0] if c else 0)
print(result)
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

    static List<List<Integer>> connectedComponents(int[][] graph, int[] values) {
        // Your code goes here
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        int[] values = parseIntArray(sc.nextLine());
        List<List<Integer>> result = connectedComponents(graph, values);
        // sort for determinism
        for (List<Integer> c : result) Collections.sort(c);
        result.sort((a, b) -> a.isEmpty() ? 0 : a.get(0) - b.get(0));
        System.out.println(result);
    }
}
```

```testcases
{
  "args": [
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1], [0, 2], [1, 3], [2, 4], [3, 5], [4, 6], [5]]" },
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[1, 0, 1, 0, 1, 0, 1]" }
  ],
  "cases": [
    { "args": { "graph": "[[1], [0, 2], [1, 3], [2, 4], [3, 5], [4, 6], [5]]", "values": "[1, 0, 1, 0, 1, 0, 1]" }, "expected": "[[0], [2], [4], [6]]" },
    { "args": { "graph": "[[1], [0], [], [4], [3]]", "values": "[1, 1, 1, 1, 1]" }, "expected": "[[0, 1], [2], [3, 4]]" },
    { "args": { "graph": "[[1], [0]]", "values": "[1, 1]" }, "expected": "[[0, 1]]" },
    { "args": { "graph": "[[1], [0]]", "values": "[1, 0]" }, "expected": "[[0]]" },
    { "args": { "graph": "[[1], [0]]", "values": "[0, 0]" }, "expected": "[]" },
    { "args": { "graph": "[[1], [0], []]", "values": "[1, 1, 1]" }, "expected": "[[0, 1], [2]]" },
    { "args": { "graph": "[]", "values": "[]" }, "expected": "[]" }
  ]
}
```

<details>
<summary>Editorial</summary>

**Approach:** apply the standard connected-components skeleton, but add a visitability guard — only start a new flood from a node if `values[node] > 0`, and only enter a neighbour if `values[neighbour] != 0`. This is the exact `f`/`g` pattern from the lesson: `f` = append node to the current component list; `g` = append the finished component to the master result. Because the output lists components (an unordered set), we sort within each component and sort the components by first element to make the output deterministic.

```python solution time=O(V+E) space=O(V)
import ast

def dfs(graph, node, values, visited, component):
    visited.add(node)
    component.append(node)
    for neighbour in graph[node]:
        if neighbour not in visited and values[neighbour] != 0:
            dfs(graph, neighbour, values, visited, component)

def connected_components(graph, values):
    n = len(graph)
    visited = set()
    components = []
    for node in range(n):
        if values[node] > 0 and node not in visited:
            component = []
            dfs(graph, node, values, visited, component)
            components.append(component)
    components = [sorted(c) for c in components]
    components.sort(key=lambda c: c[0] if c else 0)
    return components

graph = ast.literal_eval(input())
values = ast.literal_eval(input())
print(connected_components(graph, values))
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

    static void dfs(int[][] graph, int node, int[] values, boolean[] visited, List<Integer> component) {
        visited[node] = true;
        component.add(node);
        for (int nbr : graph[node])
            if (!visited[nbr] && values[nbr] != 0)
                dfs(graph, nbr, values, visited, component);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] graph = parseIntMatrix(sc.nextLine());
        int[] values = parseIntArray(sc.nextLine());
        int n = graph.length;
        boolean[] visited = new boolean[n];
        List<List<Integer>> components = new ArrayList<>();
        for (int node = 0; node < n; node++) {
            if (values[node] > 0 && !visited[node]) {
                List<Integer> comp = new ArrayList<>();
                dfs(graph, node, values, visited, comp);
                Collections.sort(comp);
                components.add(comp);
            }
        }
        components.sort((a, b) -> a.isEmpty() ? 0 : a.get(0) - b.get(0));
        System.out.println(components);
    }
}
```

</details>
