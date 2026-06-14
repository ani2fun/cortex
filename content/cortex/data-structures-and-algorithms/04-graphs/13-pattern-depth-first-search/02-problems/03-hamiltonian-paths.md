---
title: "Hamiltonian Paths"
summary: "A Hamiltonian path visits *every* vertex of the graph exactly once. Given a directed graph, source, and destination, find all Hamiltonian paths from source to destination."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: hard
kind: problem
topics: [depth-first-search, graph]
---

# Hamiltonian Paths

## Problem Statement

A **Hamiltonian path** visits every vertex of the graph **exactly once**. Given a directed graph as an adjacency list, a source, and a destination, find all Hamiltonian paths from source to destination.

## Examples

**Example 1:**
```
Input:  graph = [[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]],
        source = 0, destination = 3
Output: [[0, 1, 2, 3], [0, 2, 1, 3]]
```

Both paths visit all four nodes (`0, 1, 2, 3`) exactly once and end at `3`.

**Example 2:**
```
Input:  graph = [[1], [0, 2], [1, 3], [2]],
        source = 0, destination = 3
Output: [[0, 1, 2, 3]]
```

## Constraints

- `2 ≤ N ≤ 12` (Hamiltonian path is NP-hard; DFS is tractable for small N)
- source and destination are valid node indices (`0 ≤ source, destination < N`)
- A valid path must visit every node exactly once; the `on_path` set enforces this

> *Before coding — Hamiltonian path detection is NP-hard in general. Why is DFS-enumeration still reasonable here?* Because NP-hardness shows up at large N. For small graphs (N ≤ ~12) the DFS-with-pruning tree is small enough in practice. The intractability is visible at N ≈ 20+ where path counts explode.

```python run viz=graph viz-root=graph
import ast

def hamiltonian_paths(graph, source, destination):
    # Your code goes here — DFS from source.
    # The destination check: node == destination AND len(on_path) == len(graph).
    # "Hamiltonian" = every node visited = on_path covers all N nodes.
    pass

graph = ast.literal_eval(input())
source = int(input())
destination = int(input())
print(hamiltonian_paths(graph, source, destination))
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static int[][] graph;
    static List<List<Integer>> res;
    static List<Integer> path;
    static Set<Integer> onPath;
    static int destination;

    static void dfs(int node) {
        // Your code goes here — enter: onPath.add(node); path.add(node);
        // if node == destination && onPath.size() == graph.length record copy;
        // else recurse each neighbour not in onPath;
        // exit: onPath.remove(node); path.remove(last).
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseIntMatrix(sc.nextLine());
        int source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        res = new ArrayList<>(); path = new ArrayList<>(); onPath = new HashSet<>();
        dfs(source);
        System.out.println(res);
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
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]]" },
    { "id": "source", "label": "source", "type": "int", "placeholder": "0" },
    { "id": "destination", "label": "destination", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "graph": "[[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]]", "source": "0", "destination": "3" }, "expected": "[[0, 1, 2, 3], [0, 2, 1, 3]]" },
    { "args": { "graph": "[[1], [0, 2], [1, 3], [2]]", "source": "0", "destination": "3" }, "expected": "[[0, 1, 2, 3]]" },
    { "args": { "graph": "[[], [2], []]", "source": "0", "destination": "2" }, "expected": "[]" },
    { "args": { "graph": "[[1, 2], [2, 0], [0, 1]]", "source": "0", "destination": "2" }, "expected": "[[0, 1, 2]]" },
    { "args": { "graph": "[[1], [0]]", "source": "0", "destination": "1" }, "expected": "[[0, 1]]" }
  ]
}
```

<details>
<summary>Editorial</summary>

The Hamiltonian condition adds one extra check to the standard path-enumeration skeleton: instead of recording any path that reaches the destination, record only those that reach the destination **and** have visited every node (`len(on_path) == len(graph)`). Because `on_path` already ensures no node repeats, checking its size against `n` is the complete Hamiltonian condition.

The DFS structure is otherwise identical: add on entry, recurse into unvisited neighbours, remove on exit. The `on_path` pruning also prunes the exponential search space heavily — any branch that cannot possibly cover all remaining nodes is abandoned the moment its path gets blocked.

```python solution time=O(N!) space=O(N)
import ast

def hamiltonian_paths(graph, source, destination):
    n = len(graph)
    paths, path, on_path = [], [], set()
    def dfs(node):
        on_path.add(node); path.append(node)
        if node == destination and len(on_path) == n:
            paths.append(path[:])
        else:
            for neighbour in graph[node]:
                if neighbour not in on_path:
                    dfs(neighbour)
        path.pop(); on_path.remove(node)
    dfs(source)
    return paths

graph = ast.literal_eval(input())
source = int(input())
destination = int(input())
print(hamiltonian_paths(graph, source, destination))
```

```java solution
import java.util.*;

public class Main {
    static int[][] graph;
    static List<List<Integer>> res;
    static List<Integer> path;
    static Set<Integer> onPath;
    static int destination;

    static void dfs(int node) {
        onPath.add(node); path.add(node);
        if (node == destination && onPath.size() == graph.length) {
            res.add(new ArrayList<>(path));
        } else {
            for (int neighbour : graph[node])
                if (!onPath.contains(neighbour)) dfs(neighbour);
        }
        path.remove(path.size() - 1); onPath.remove(node);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseIntMatrix(sc.nextLine());
        int source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        res = new ArrayList<>(); path = new ArrayList<>(); onPath = new HashSet<>();
        dfs(source);
        System.out.println(res);
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
