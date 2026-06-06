---
title: "Source to Target Paths"
summary: "Given a directed graph as adjacency list, return *all* paths from node 0 to node N-1."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: medium
---

# Problem: Source to Target Paths

## The Problem

Given a directed graph as adjacency list, return *all* paths from node `0` to node `N-1`.

```
Input:  graph = [[1, 2], [4], [3, 4], [4], []]
Output: [[0, 1, 4], [0, 2, 3, 4], [0, 2, 4]]

Input:  graph = [[4], [0, 3], [0, 4], [2, 4], []]
Output: [[0, 4]]
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: append node to current path list.
- `g`: append a copy of the current path to `paths` when destination reached.
- `f⁻¹`: pop last element from current path list on exit.

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
        path: List[int],
        paths: List[List[int]],
        nodes_in_path: Set[int],
    ) -> None:

        # Insert the current node into the set of nodes in the current
        # path to avoid cycles
        nodes_in_path.add(node)

        # Add the current node to the path
        path.append(node)

        # If the current node is the destination node, add the current
        # path to the paths list
        if node == len(graph) - 1:
            paths.append(path.copy())

        # Else, recursively explore all the neighbours of the current
        # node
        else:
            for neighbour in graph[node]:

                # Perform DFS on the neighbour node if it is not already
                # in the current path to avoid cycles
                if neighbour not in nodes_in_path:
                    self.dfs(
                        graph, neighbour, path, paths, nodes_in_path
                    )

        # Remove the current node from the path as we are done exploring
        path.pop()

        # Remove the current node from the set of nodes in the current
        # path to allow it to be visited again in other paths
        nodes_in_path.remove(node)

    def source_to_target_paths(
        self, graph: List[List[int]]
    ) -> List[List[int]]:

        # Result list to store all the paths
        paths: List[List[int]] = []

        # List to store the current path
        path: List[int] = []

        # Set to keep track of nodes in the current path
        nodes_in_path: Set[int] = set()

        # Perform DFS starting from node 0
        self.dfs(graph, 0, path, paths, nodes_in_path)

        # Return the list of paths
        return paths


# Examples from the problem statement
print(Solution().source_to_target_paths([[1,2],[4],[3,4],[4],[0]]))   # [[0,1,4],[0,2,3,4],[0,2,4]]
print(Solution().source_to_target_paths([[4],[0,3],[0,4],[2,4],[1]])) # [[0,4]]

# Edge cases
print(Solution().source_to_target_paths([[0]]))          # [[0]] — single node, src==dst
print(Solution().source_to_target_paths([[1], []]))      # [[0, 1]]
print(Solution().source_to_target_paths([[], [0]]))      # [] — no path from 0 to 1
print(Solution().source_to_target_paths([[1,2],[2],[]]))  # [[0,1,2],[0,2]]
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private void dfs(
            List<List<Integer>> graph,
            int node,
            List<Integer> path,
            List<List<Integer>> paths,
            Set<Integer> nodesInPath
        ) {

            // Insert the current node into the set of nodes in the current
            // path to avoid cycles
            nodesInPath.add(node);

            // Add the current node to the path
            path.add(node);

            // If the current node is the destination node, add the current
            // path to the paths list
            if (node == graph.size() - 1) {
                paths.add(new ArrayList<>(path));
            }

            // Else, recursively explore all the neighbours of the current
            // node
            else {
                for (int neighbour : graph.get(node)) {

                    // Perform DFS on the neighbour node if it is not already
                    // in the current path to avoid cycles
                    if (!nodesInPath.contains(neighbour)) {
                        dfs(graph, neighbour, path, paths, nodesInPath);
                    }
                }
            }

            // Remove the current node from the path as we are done exploring
            // it
            path.remove(path.size() - 1);

            // Remove the current node from the set of nodes in the current
            // path to allow it to be visited again in other paths
            nodesInPath.remove(node);
        }

        public List<List<Integer>> sourceToTargetPaths(
            List<List<Integer>> graph
        ) {

            // Result list to store all the paths
            List<List<Integer>> paths = new ArrayList<>();

            // List to store the current path
            List<Integer> path = new ArrayList<>();

            // Set to keep track of nodes in the current path
            Set<Integer> nodesInPath = new HashSet<>();

            // Perform DFS starting from node 0
            dfs(graph, 0, path, paths, nodesInPath);

            // Return the list of paths
            return paths;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.sourceToTargetPaths(List.of(List.of(1,2),List.of(4),List.of(3,4),List.of(4),List.of(0))));  // [[0,1,4],[0,2,3,4],[0,2,4]]
        System.out.println(sol.sourceToTargetPaths(List.of(List.of(4),List.of(0,3),List.of(0,4),List.of(2,4),List.of(1))));  // [[0,4]]

        // Edge cases
        System.out.println(sol.sourceToTargetPaths(List.of(List.of(0))));  // [[0]] — single node, src==dst
        System.out.println(sol.sourceToTargetPaths(List.of(List.of(1), new ArrayList<>())));  // [[0, 1]]
        System.out.println(sol.sourceToTargetPaths(List.of(new ArrayList<>(), List.of(0))));  // [] — no path from 0 to 1
        System.out.println(sol.sourceToTargetPaths(List.of(List.of(1,2), List.of(2), new ArrayList<>())));  // [[0,1,2],[0,2]]
    }
}
```

</details>
