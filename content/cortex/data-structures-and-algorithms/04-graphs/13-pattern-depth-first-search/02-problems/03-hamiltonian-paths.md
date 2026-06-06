---
title: "Hamiltonian Paths"
summary: "A Hamiltonian path visits *every* vertex of the graph exactly once. Given a directed graph, source, and destination, find all Hamiltonian paths from source to destination."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: hard
---

# Problem: Hamiltonian Paths

## The Problem

A **Hamiltonian path** visits *every* vertex of the graph exactly once. Given a directed graph, source, and destination, find all Hamiltonian paths from source to destination.

```
Input:  graph = [[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]], source = 0, destination = 3
Output: [[0, 1, 2, 3], [0, 2, 1, 3]]
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: same as before (append to path).
- `g`: record the path *only if* destination is reached **and** every node has been visited.
- `f⁻¹`: same as before.

The only twist: the destination check now requires `path.length == N`.

> *Before reading on — Hamiltonian path detection is famously **NP-hard**. Why is it still tractable here? What property of the input keeps the algorithm fast?*

It's tractable because we're enumerating, not deciding existence faster than brute force. DFS with the "in_path" pruning has worst case O(N!) in pathological cases — but for typical small graphs (N ≤ ~20) it's fast enough. The intractability shows up when N gets larger; below that, DFS is the only sane approach.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=graph
from typing import List, Set

class Solution:
    def dfs(
        self,
        graph: List[List[int]],
        node: int,
        destination: int,
        path: List[int],
        paths: List[List[int]],
        nodes_in_path: Set[int],
    ) -> None:

        # Insert the current node into the set of nodes in the current
        # path to avoid revisiting the same node
        nodes_in_path.add(node)

        # Add the current node to the path
        path.append(node)

        # If the current node is the destination node and all nodes
        # have been visited, we have found a valid Hamiltonian Path
        if node == destination and len(nodes_in_path) == len(graph):
            paths.append(path.copy())

        # Else, recursively explore all the neighbours of the current
        # node
        else:
            for neighbour in graph[node]:

                # Perform DFS on the neighbour node if it is not already
                # in the current path to avoid cycles
                if neighbour not in nodes_in_path:
                    self.dfs(
                        graph,
                        neighbour,
                        destination,
                        path,
                        paths,
                        nodes_in_path,
                    )

        # Remove the current node from the path as we are done exploring
        # it
        path.pop()

        # Remove the current node from the set of nodes in the current
        # path to allow it to be visited again in other possible paths
        nodes_in_path.remove(node)

    def hamiltonian_paths(
        self, graph: List[List[int]], source: int, destination: int
    ) -> List[List[int]]:

        # Result list to store all the Hamiltonian paths
        paths: List[List[int]] = []

        # List to store the current path being explored
        path: List[int] = []

        # Set to keep track of nodes currently in the path
        nodes_in_path: Set[int] = set()

        # Perform DFS starting from the source node
        self.dfs(graph, source, destination, path, paths, nodes_in_path)

        # Return the list of all valid Hamiltonian paths
        return paths


# Examples from the problem statement
print(Solution().hamiltonian_paths([[1,2],[0,2,3],[0,1,3],[1,2]], 0, 3))  # [[0,1,2,3],[0,2,1,3]]
print(Solution().hamiltonian_paths([[1],[0,2],[1,3],[2]], 0, 3))           # [[0,1,2,3]]

# Edge cases
print(Solution().hamiltonian_paths([[]], 0, 0))                            # [[0]] — single node
print(Solution().hamiltonian_paths([[1],[]], 0, 1))                        # [[0,1]]
# No Hamiltonian path (missing edges to visit all)
print(Solution().hamiltonian_paths([[1],[2],[]], 0, 2))                    # [[0,1,2]]
# Disconnected — no path
print(Solution().hamiltonian_paths([[],[2],[]], 0, 2))                     # []
# src == dst but must visit all
print(Solution().hamiltonian_paths([[1],[0]], 0, 0))                       # [[0,1,0]] — visits all
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private void dfs(
            List<List<Integer>> graph,
            int node,
            int destination,
            List<Integer> path,
            List<List<Integer>> paths,
            Set<Integer> nodesInPath
        ) {

            // Insert the current node into the set of nodes in the current
            // path to avoid revisiting the same node
            nodesInPath.add(node);

            // Add the current node to the path
            path.add(node);

            // If the current node is the destination node and all nodes
            // have been visited, we have found a valid Hamiltonian Path
            if (node == destination && nodesInPath.size() == graph.size()) {
                paths.add(new ArrayList<>(path));
            }

            // Else, recursively explore all the neighbours of the current
            // node
            else {
                for (int neighbour : graph.get(node)) {

                    // Perform DFS on the neighbour node if it is not already
                    // in the current path to avoid cycles
                    if (!nodesInPath.contains(neighbour)) {
                        dfs(
                            graph,
                            neighbour,
                            destination,
                            path,
                            paths,
                            nodesInPath
                        );
                    }
                }
            }

            // Remove the current node from the path as we are done exploring
            // it
            path.remove(path.size() - 1);

            // Remove the current node from the set of nodes in the current
            // path to allow it to be visited again in other possible paths
            nodesInPath.remove(node);
        }

        public List<List<Integer>> hamiltonianPaths(
            List<List<Integer>> graph,
            int source,
            int destination
        ) {

            // Result list to store all the Hamiltonian paths
            List<List<Integer>> paths = new ArrayList<>();

            // List to store the current path being explored
            List<Integer> path = new ArrayList<>();

            // Set to keep track of nodes currently in the path
            Set<Integer> nodesInPath = new HashSet<>();

            // Perform DFS starting from the source node
            dfs(graph, source, destination, path, paths, nodesInPath);

            // Return the list of all valid Hamiltonian paths
            return paths;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.hamiltonianPaths(List.of(List.of(1,2),List.of(0,2,3),List.of(0,1,3),List.of(1,2)), 0, 3));  // [[0,1,2,3],[0,2,1,3]]
        System.out.println(sol.hamiltonianPaths(List.of(List.of(1),List.of(0,2),List.of(1,3),List.of(2)), 0, 3));            // [[0,1,2,3]]

        // Edge cases
        System.out.println(sol.hamiltonianPaths(List.of(new ArrayList<>()), 0, 0));  // [[0]]
        System.out.println(sol.hamiltonianPaths(List.of(List.of(1), new ArrayList<>()), 0, 1));  // [[0,1]]
        System.out.println(sol.hamiltonianPaths(List.of(List.of(1), List.of(2), new ArrayList<>()), 0, 2));  // [[0,1,2]]
        System.out.println(sol.hamiltonianPaths(List.of(new ArrayList<>(), List.of(2), new ArrayList<>()), 0, 2));  // []
        System.out.println(sol.hamiltonianPaths(List.of(List.of(1), List.of(0)), 0, 0));  // [[0,1,0]]
    }
}
```

</details>
