---
title: "Simple Cycles"
summary: "Given a directed graph, source, and destination, count the number of simple cycles that start at the source, pass through the destination, and return to the source without repeating any other node."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: medium
kind: problem
topics: [depth-first-search, graph]
---

# Simple Cycles

## Problem Statement

Given a directed graph, a source, and a destination, **count** the number of **simple cycles** that:
1. start at the source,
2. pass through the destination, and
3. return to the source without repeating any other node.

A simple cycle has **at least 3 nodes** (source + destination + at least one intermediate node).

## Examples

**Example 1:**
```
Input:  graph = [[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]],
        source = 0, destination = 3
Output: 2
```

The two cycles are `0→1→3→2→0` and `0→2→3→1→0` — both start and end at 0 and pass through node 3.

**Example 2:**
```
Input:  graph = [[1, 2], [2, 0], [0, 1]],
        source = 0, destination = 1
Output: 2
```

Cycles: `0→1→2→0` and `0→2→1→0` — both pass through node 1.

## Constraints

- `2 ≤ N ≤ 12`
- source and destination are valid node indices
- A valid cycle must contain at least 3 distinct nodes; 2-node back-edges (`u→v→u`) are not counted

```python run viz=graph viz-root=graph viz-kind=graph
import ast

def simple_cycles(graph, source, destination):
    # Your code goes here — DFS from source, tracking on_path.
    # The cycle-close check happens INLINE with neighbour iteration:
    #   if a neighbour IS the source AND len(on_path) > 2 AND destination is in on_path → count += 1.
    # Do not recurse into on_path nodes; recurse only into unvisited ones.
    pass

graph = ast.literal_eval(input())
source = int(input())
destination = int(input())
print(simple_cycles(graph, source, destination))
```

```java run viz=graph viz-root=graph viz-kind=graph
import java.util.*;

public class Main {
    static int[][] graph;
    static Set<Integer> onPath = new HashSet<>();
    static int source, destination, cycles;

    static void dfs(int node) {
        // Your code goes here — enter: onPath.add(node).
        // For each neighbor: if not in onPath recurse; else if neighbor==source && size>2 && dest in onPath → cycles++.
        // exit: onPath.remove(node).
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseIntMatrix(sc.nextLine());
        source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        cycles = 0;
        dfs(source);
        System.out.println(cycles);
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
    { "args": { "graph": "[[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]]", "source": "0", "destination": "3" }, "expected": "2" },
    { "args": { "graph": "[[1], [0, 2], [1, 3], [2]]", "source": "0", "destination": "3" }, "expected": "0" },
    { "args": { "graph": "[[1], [2, 0], [0]]", "source": "0", "destination": "1" }, "expected": "1" },
    { "args": { "graph": "[[1, 2], [2, 0], [0, 1]]", "source": "0", "destination": "1" }, "expected": "2" },
    { "args": { "graph": "[[1, 2], [0], [0]]", "source": "0", "destination": "1" }, "expected": "0" }
  ]
}
```

<details>
<summary>Editorial</summary>

The structural difference from path-enumeration: there is no explicit "destination reached → record" branch. Instead, the cycle-closing check is **inline** with the neighbour loop. For each neighbour of the current node:

- If the neighbour is **not** in `on_path` — recurse normally.
- If the neighbour **is** the source **and** `len(on_path) > 2` **and** the destination is already in `on_path` — a valid simple cycle has just closed; increment the counter.

The `len(on_path) > 2` guard enforces the 3-node minimum. The `destination in on_path` check ensures the cycle actually passes through the required node — without it, any back-edge to the source would count regardless of whether it visited the destination.

The rest — add on entry, remove on exit — is identical to every other DFS enumeration in this section.

```python solution time=O(V! · E) space=O(V)
import ast

def simple_cycles(graph, source, destination):
    cycles = 0
    on_path = set()
    def dfs(node):
        nonlocal cycles
        on_path.add(node)
        for neighbor in graph[node]:
            if neighbor not in on_path:
                dfs(neighbor)
            elif neighbor == source and len(on_path) > 2 and destination in on_path:
                cycles += 1
        on_path.remove(node)
    dfs(source)
    return cycles

graph = ast.literal_eval(input())
source = int(input())
destination = int(input())
print(simple_cycles(graph, source, destination))
```

```java solution
import java.util.*;

public class Main {
    static int[][] graph;
    static Set<Integer> onPath = new HashSet<>();
    static int source, destination, cycles;

    static void dfs(int node) {
        onPath.add(node);
        for (int neighbor : graph[node]) {
            if (!onPath.contains(neighbor)) {
                dfs(neighbor);
            } else if (neighbor == source && onPath.size() > 2 && onPath.contains(destination)) {
                cycles++;
            }
        }
        onPath.remove(node);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseIntMatrix(sc.nextLine());
        source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        cycles = 0;
        dfs(source);
        System.out.println(cycles);
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
