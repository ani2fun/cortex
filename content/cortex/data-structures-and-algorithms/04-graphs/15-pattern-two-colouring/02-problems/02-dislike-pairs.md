---
title: "Dislike Pairs"
summary: "N people, a list of dislikes pairs. Can the people be split into two groups such that no two who dislike each other end up in the same group?"
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: medium
kind: problem
topics: [two-colouring, graph]
---

# Problem: Dislike Pairs

## Problem Statement

`N` people (0-indexed, nodes `0..N-1`) and a list of `dislikes` pairs. Can the people be split into two groups such that no two who dislike each other end up in the same group?

Build an undirected adjacency list from the dislike pairs, then check whether the resulting graph is two-colourable.

## Examples

**Example 1:**
```
Input:  N = 4, dislikes = [[1, 3], [0, 2], [1, 3], [0, 2]]
Output: true  (groups {0, 2} and {1, 3})
```

**Example 2:**
```
Input:  N = 3, dislikes = [[0, 1], [1, 2], [2, 0]]
Output: false  (3-cycle of mutual dislikes — odd cycle)
```

## Constraints

- `1 ≤ N ≤ 2000`
- `0 ≤ dislikes.length ≤ 10⁴`
- `dislikes[i]` are 0-indexed pairs; `dislikes[i][0] != dislikes[i][1]`
- Build the adjacency by iterating dislikes in order — no sorting needed

```python run viz=graph viz-kind=graph
import ast

class Solution:
    def colour_graph(self, graph, node, colour, colour_value):
        # Your code goes here
        pass

    def dislike_pairs(self, n, dislikes):
        # Your code goes here
        pass

n = int(input())
dislikes = ast.literal_eval(input())
result = Solution().dislike_pairs(n, dislikes)
print("true" if result else "false")
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static class Solution {
        boolean colourGraph(List<List<Integer>> graph, int node, int[] colour, int colourValue) {
            // Your code goes here
            return false;
        }

        boolean dislikePairs(int n, int[][] dislikes) {
            // Your code goes here
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int[][] dislikes = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().dislikePairs(n, dislikes));
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
    { "id": "n", "label": "N", "type": "int", "placeholder": "4" },
    { "id": "dislikes", "label": "dislikes", "type": "int[][]", "placeholder": "[[1, 3], [0, 2], [1, 3], [0, 2]]" }
  ],
  "cases": [
    { "args": { "n": "4", "dislikes": "[[1, 3], [0, 2], [1, 3], [0, 2]]" }, "expected": "true" },
    { "args": { "n": "3", "dislikes": "[[0, 1], [1, 2], [2, 0]]" }, "expected": "false" },
    { "args": { "n": "1", "dislikes": "[]" }, "expected": "true" },
    { "args": { "n": "2", "dislikes": "[[0, 1]]" }, "expected": "true" },
    { "args": { "n": "4", "dislikes": "[]" }, "expected": "true" },
    { "args": { "n": "3", "dislikes": "[[0, 1], [0, 2], [1, 2]]" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

This is two-colouring in disguise: people are nodes, dislike pairs are undirected edges, and "same group" means "same colour". Build an undirected adjacency list from the dislikes (both directions), then run the standard DFS two-colouring over every uncoloured component. A clash on any already-coloured neighbour means the graph contains an odd cycle — not bipartite.

```python solution time=O(N + E) space=O(N + E)
import ast

class Solution:
    def colour_graph(self, graph, node, colour, colour_value):
        colour[node] = colour_value
        for neighbour in graph[node]:
            if neighbour not in colour:
                if not self.colour_graph(graph, neighbour, colour, 1 - colour_value):
                    return False
            elif colour[neighbour] == colour_value:
                return False
        return True

    def dislike_pairs(self, n, dislikes):
        if n == 0:
            return False
        graph = [[] for _ in range(n)]
        for dislike in dislikes:
            graph[dislike[0]].append(dislike[1])
            graph[dislike[1]].append(dislike[0])
        colour = {}
        for node in range(n):
            if node not in colour:
                if not self.colour_graph(graph, node, colour, 1):
                    return False
        return True

n = int(input())
dislikes = ast.literal_eval(input())
result = Solution().dislike_pairs(n, dislikes)
print("true" if result else "false")
```

```java solution
import java.util.*;

public class Main {
    static class Solution {
        boolean colourGraph(List<List<Integer>> graph, int node, int[] colour, int colourValue) {
            colour[node] = colourValue;
            for (int neighbour : graph.get(node)) {
                if (colour[neighbour] == -1) {
                    if (!colourGraph(graph, neighbour, colour, 1 - colourValue)) return false;
                } else if (colour[neighbour] == colourValue) {
                    return false;
                }
            }
            return true;
        }

        boolean dislikePairs(int n, int[][] dislikes) {
            if (n == 0) return false;
            List<List<Integer>> graph = new ArrayList<>();
            for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
            for (int[] d : dislikes) {
                graph.get(d[0]).add(d[1]);
                graph.get(d[1]).add(d[0]);
            }
            int[] colour = new int[n];
            Arrays.fill(colour, -1);
            for (int node = 0; node < n; node++) {
                if (colour[node] == -1) {
                    if (!colourGraph(graph, node, colour, 1)) return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = Integer.parseInt(sc.nextLine().trim());
        int[][] dislikes = parseIntMatrix(sc.nextLine());
        System.out.println(new Solution().dislikePairs(n, dislikes));
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
