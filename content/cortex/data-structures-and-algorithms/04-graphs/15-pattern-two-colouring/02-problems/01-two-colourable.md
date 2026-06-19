---
title: "Two Colourable"
summary: "Given an undirected graph as an adjacency list, decide whether it can be 2-coloured — i.e. whether it is bipartite."
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: easy
kind: problem
topics: [two-colouring, graph]
---

# Problem: Two Colourable

## Problem Statement

Given an undirected graph as an **adjacency list** (`graph[u]` is the list of `u`'s neighbours), decide whether it is **two-colourable**: can you assign one of two colours to every node so that no edge joins two nodes of the same colour? Return `true` if such a colouring exists, `false` otherwise.

This is the canonical drill for the pattern — it *is* the See-It-Work, packaged as a problem. Two-colourable, bipartite, and "no odd cycle" are three names for the same property. The approach is the pattern verbatim: DFS the graph, paint each uncoloured neighbour the opposite colour, and the instant you meet an already-coloured neighbour that shares your colour, report failure. An outer loop reseeds each disconnected component so every one is checked. An odd cycle is the only thing that can force a same-colour clash.

## Examples

**Example 1:**
```
Input:  graph = [[1, 3], [0, 2], [1, 3], [0, 2]]
Output: true
```
The 4-cycle `0–1–2–3–0` alternates cleanly: `{0, 2}` one colour, `{1, 3}` the other.

**Example 2:**
```
Input:  graph = [[1, 2], [0, 2], [0, 1]]
Output: false
```
The triangle `0–1–2` is an odd cycle: paint `0` red, `1` blue, `2` must be red — but edge `2–0` is then red–red.

```quiz
{
  "prompt": "Which graph is NOT two-colourable?",
  "options": ["A single edge [[1], [0]]", "A 4-cycle [[1,3],[0,2],[1,3],[0,2]]", "A triangle [[1,2],[0,2],[0,1]]", "Two isolated nodes [[], []]"],
  "answer": "A triangle [[1,2],[0,2],[0,1]]"
}
```

## Constraints

- `0 ≤ number of nodes ≤ 10⁴`; the graph is undirected (each edge appears in both endpoints' lists)
- An empty graph (no nodes) is treated as not two-colourable
- `O(V + E)` time, `O(V)` space — every node is coloured once, every edge checked once

```python run viz=graph viz-root=graph viz-kind=graph
import ast

def is_two_colourable(graph):
    # Your code goes here — DFS each uncoloured node, painting neighbours the
    # opposite colour; on meeting an already-coloured neighbour, verify it differs.
    # Reseed an uncoloured node per component. Empty graph → False.
    pass

graph = ast.literal_eval(input())   # adjacency list: graph[u] = u's neighbours
print("true" if is_two_colourable(graph) else "false")
```

```java run viz=graph viz-root=graph viz-kind=graph
import java.util.*;

public class Main {
    static boolean isTwoColourable(int[][] graph) {
        // Your code goes here — DFS each uncoloured node, painting neighbours the
        // opposite colour; on meeting an already-coloured neighbour, verify it differs.
        // Reseed an uncoloured node per component. Empty graph → false.
        return false;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(isTwoColourable(parseIntMatrix(sc.nextLine())));
    }

    // "[[1, 3], [0, 2]]" → adjacency list graph[u] = u's neighbours
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
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[1, 3], [0, 2], [1, 3], [0, 2]]" }
  ],
  "cases": [
    { "args": { "graph": "[[1, 3], [0, 2], [1, 3], [0, 2]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2], [0, 2], [0, 1]]" }, "expected": "false" },
    { "args": { "graph": "[[1], [0]]" }, "expected": "true" },
    { "args": { "graph": "[[], []]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2, 3], [0], [0], [0]]" }, "expected": "true" },
    { "args": { "graph": "[[1, 2], [0, 2], [0, 1, 3], [2]]" }, "expected": "false" },
    { "args": { "graph": "[]" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

Straight from the pattern. Colour the start node `1`, recurse into each neighbour with the opposite colour `1 - value`, and on meeting an *already-coloured* neighbour, check it differs — a same-colour neighbour is the contradiction that proves an odd cycle, so return `false`. The outer loop reseeds an uncoloured node for each component, because a disconnected graph is two-colourable only if *every* component is. The empty graph returns `false` by convention (there's nothing to colour). The conflict check on visited neighbours is the entire detection mechanism — without it you'd merely traverse and write labels, never noticing the clash on a closing edge.

```python solution time=O(V + E) space=O(V)
import ast

def colour(graph, node, col, value):
    col[node] = value
    for nb in graph[node]:
        if nb not in col:                               # uncoloured → paint it the opposite colour
            if not colour(graph, nb, col, 1 - value): return False
        elif col[nb] == value:                          # coloured the SAME → contradiction
            return False
    return True

def is_two_colourable(graph):
    if not graph: return False
    col = {}
    for node in range(len(graph)):                      # outer loop: cover every component
        if node not in col and not colour(graph, node, col, 1):
            return False
    return True

graph = ast.literal_eval(input())   # adjacency list: graph[u] = u's neighbours
print("true" if is_two_colourable(graph) else "false")
```

```java solution
import java.util.*;

public class Main {
    static boolean colour(int[][] graph, int node, Map<Integer, Integer> col, int value) {
        col.put(node, value);
        for (int nb : graph[node]) {
            if (!col.containsKey(nb)) {                  // uncoloured → opposite colour
                if (!colour(graph, nb, col, 1 - value)) return false;
            } else if (col.get(nb) == value) {           // coloured the SAME → contradiction
                return false;
            }
        }
        return true;
    }

    static boolean isTwoColourable(int[][] graph) {
        if (graph.length == 0) return false;
        Map<Integer, Integer> col = new HashMap<>();
        for (int node = 0; node < graph.length; node++)  // outer loop: cover every component
            if (!col.containsKey(node) && !colour(graph, node, col, 1)) return false;
        return true;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(isTwoColourable(parseIntMatrix(sc.nextLine())));
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
