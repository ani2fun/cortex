---
title: "Target Paths with Given Weight"
summary: "Given a weighted directed graph, source, destination, and target weight, return all paths from source to destination whose edge weights sum to exactly the target."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: medium
kind: problem
topics: [depth-first-search, graph]
---

# Target Paths with Given Weight

## Problem Statement

Given a **weighted** directed graph, a source, a destination, and a target weight, return all paths from source to destination whose **edge weights sum to exactly the target**. A path cannot visit any node more than once.

The weighted adjacency list has the form `graph[u] = [[nbr, wt], ...]` — each entry is a `[neighbour, weight]` pair.

## Examples

**Example 1:**
```
Input:  graph = [[[1, 2], [3, 5]], [[4, 2]], [[4, 1]], [[2, 2]], [[3, 1]]],
        source = 0, destination = 3, target = 5
Output: [[0, 1, 4, 3], [0, 3]]
```

Path `0→1→4→3` costs `2+2+1 = 5`. Path `0→3` costs `5`. Both qualify.

**Example 2:**
```
Input:  graph = [[[4, 2]], [[3, 3], [0, 4]], [[4, 3], [0, 1]], [[2, 1], [4, 4]], [[1, 5]]],
        source = 3, destination = 4, target = 4
Output: [[3, 2, 4], [3, 2, 0, 4], [3, 4]]
```

## Constraints

- `1 ≤ N ≤ 15`
- All edge weights are positive integers.
- source and destination are valid node indices.
- No node may appear more than once in a path; the source node at position 0 counts as the first visit.

```python run viz=graph viz-root=graph viz-kind=graph
import ast

def target_paths(graph, source, destination, target):
    # Your code goes here — DFS from source to destination, tracking running weight sum.
    # On entry: add node to on_path, append to path.
    # At destination: if current_sum == target, record path copy.
    # For each [nbr, wt] in graph[node]: if nbr not in on_path, recurse with sum + wt.
    # On exit: remove from on_path, pop path.
    pass

graph = ast.literal_eval(input())   # weighted adjacency: graph[u] = [[nbr, wt], ...]
source = int(input())
destination = int(input())
target = int(input())
print(target_paths(graph, source, destination, target))
```

```java run viz=graph viz-root=graph viz-kind=graph
import java.util.*;

public class Main {
    static int[][][] graph;
    static List<List<Integer>> res;
    static List<Integer> path;
    static Set<Integer> onPath;
    static int destination, target;

    static void dfs(int node, int currentSum) {
        // Your code goes here — enter: onPath.add(node); path.add(node);
        // if node == destination && currentSum == target record a copy;
        // else for each int[] edge in graph[node]: if edge[0] not in onPath recurse(edge[0], currentSum+edge[1]);
        // exit: onPath.remove(node); path.remove(last).
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseWeightedAdj(sc.nextLine());
        int source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        target = Integer.parseInt(sc.nextLine().trim());
        res = new ArrayList<>(); path = new ArrayList<>(); onPath = new HashSet<>();
        dfs(source, 0);
        System.out.println(res);
    }

    static int[][][] parseWeightedAdj(String line) {
        List<int[][]> g = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n && line.charAt(i) != '[') i++;
        i++;                                             // consume outer '['
        while (i < n) {
            while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
            if (i >= n || line.charAt(i) == ']') break;  // end of outer list
            i++;                                         // consume a node group's '['
            List<int[]> node = new ArrayList<>();
            while (i < n) {
                while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                if (line.charAt(i) == ']') { i++; break; } // end of this node group
                i++;                                     // consume a pair's '['
                int[] pair = new int[2]; int k = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k++] = Integer.parseInt(line.substring(start, i));
                }
                i++;                                     // consume the pair's ']'
                node.add(pair);
            }
            g.add(node.toArray(new int[0][]));
        }
        return g.toArray(new int[0][][]);
    }
}
```

```testcases
{
  "args": [
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[[1, 2], [3, 5]], [[4, 2]], [[4, 1]], [[2, 2]], [[3, 1]]]" },
    { "id": "source", "label": "source", "type": "int", "placeholder": "0" },
    { "id": "destination", "label": "destination", "type": "int", "placeholder": "3" },
    { "id": "target", "label": "target weight", "type": "int", "placeholder": "5" }
  ],
  "cases": [
    { "args": { "graph": "[[[1, 2], [3, 5]], [[4, 2]], [[4, 1]], [[2, 2]], [[3, 1]]]", "source": "0", "destination": "3", "target": "5" }, "expected": "[[0, 1, 4, 3], [0, 3]]" },
    { "args": { "graph": "[[[4, 2]], [[3, 3], [0, 4]], [[4, 3], [0, 1]], [[2, 1], [4, 4]], [[1, 5]]]", "source": "3", "destination": "4", "target": "4" }, "expected": "[[3, 2, 4], [3, 2, 0, 4], [3, 4]]" },
    { "args": { "graph": "[[[1, 3]], []]", "source": "0", "destination": "1", "target": "3" }, "expected": "[[0, 1]]" },
    { "args": { "graph": "[[[1, 2], [2, 3]], [[2, 1]], []]", "source": "0", "destination": "2", "target": "3" }, "expected": "[[0, 1, 2], [0, 2]]" },
    { "args": { "graph": "[[[1, 3]], []]", "source": "0", "destination": "1", "target": "5" }, "expected": "[]" }
  ]
}
```

<details>
<summary>Editorial</summary>

Extend the standard DFS path-enumeration skeleton with a running weight sum. On entry, add the node to `on_path` and append it to `path`. When the current node is the destination **and** the accumulated sum equals the target, record a copy of the path. Otherwise, iterate over each `[nbr, wt]` pair in `graph[node]`: if `nbr` is not already `on_path`, recurse with `currentSum + wt`. On exit, remove the node from `on_path` and pop it from `path` — same backtrack as the unweighted version.

The key difference from source-to-target-paths: the check at the destination is a conjunction (`node == destination AND sum == target`), not a simple equality. A path that overshoots the target can still backtrack and contribute another path through a different route.

```python solution time=O(2^N · N) space=O(N)
import ast

def target_paths(graph, source, destination, target):
    paths, path, on_path = [], [], set()
    def dfs(node, current_sum):
        on_path.add(node); path.append(node)
        if node == destination and current_sum == target:
            paths.append(path[:])
        else:
            for neighbour, weight in graph[node]:
                if neighbour not in on_path:
                    dfs(neighbour, current_sum + weight)
        path.pop(); on_path.remove(node)
    dfs(source, 0)
    return paths

graph = ast.literal_eval(input())   # weighted adjacency: graph[u] = [[nbr, wt], ...]
source = int(input())
destination = int(input())
target = int(input())
print(target_paths(graph, source, destination, target))
```

```java solution
import java.util.*;

public class Main {
    static int[][][] graph;
    static List<List<Integer>> res;
    static List<Integer> path;
    static Set<Integer> onPath;
    static int destination, target;

    static void dfs(int node, int currentSum) {
        onPath.add(node); path.add(node);
        if (node == destination && currentSum == target) {
            res.add(new ArrayList<>(path));
        } else {
            for (int[] edge : graph[node]) {
                int nbr = edge[0], wt = edge[1];
                if (!onPath.contains(nbr)) dfs(nbr, currentSum + wt);
            }
        }
        path.remove(path.size() - 1); onPath.remove(node);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        graph = parseWeightedAdj(sc.nextLine());
        int source = Integer.parseInt(sc.nextLine().trim());
        destination = Integer.parseInt(sc.nextLine().trim());
        target = Integer.parseInt(sc.nextLine().trim());
        res = new ArrayList<>(); path = new ArrayList<>(); onPath = new HashSet<>();
        dfs(source, 0);
        System.out.println(res);
    }

    static int[][][] parseWeightedAdj(String line) {
        List<int[][]> g = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n && line.charAt(i) != '[') i++;
        i++;                                             // consume outer '['
        while (i < n) {
            while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
            if (i >= n || line.charAt(i) == ']') break;  // end of outer list
            i++;                                         // consume a node group's '['
            List<int[]> node = new ArrayList<>();
            while (i < n) {
                while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                if (line.charAt(i) == ']') { i++; break; } // end of this node group
                i++;                                     // consume a pair's '['
                int[] pair = new int[2]; int k = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k++] = Integer.parseInt(line.substring(start, i));
                }
                i++;                                     // consume the pair's ']'
                node.add(pair);
            }
            g.add(node.toArray(new int[0][]));
        }
        return g.toArray(new int[0][][]);
    }
}
```

</details>
