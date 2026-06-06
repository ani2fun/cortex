---
title: "Find Connected Components"
summary: "Given an undirected graph and a values array, return a list of all connected components — but only of the *visitable* nodes (values[i] > 0)."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: easy
---

# Problem: Find Connected Components

## The Problem

Given an undirected graph and a `values` array, return a list of all connected components — but only of the *visitable* nodes (`values[i] > 0`).

```
Input:  graph = [[1], [0, 2], [1, 3], [2, 4], [3, 5], [4, 6], [5]],
        values = [1, 0, 1, 0, 1, 0, 1]
Output: [[0], [2], [4], [6]]
```

The "visitable" twist makes the problem more interesting: nodes with `values[i] == 0` block the DFS entirely, so a chain of zeros isolates the visitable nodes from each other.

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: append node to current component's list.
- `g`: append the component to the master result list.
- *Visitability filter*: only descend into neighbours where `values[neighbour] > 0`.

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
        values: List[int],
        visited: Set[int],
        component: List[int],
    ) -> None:

        # Mark the current node as visited in the graph to avoid
        # visiting it again
        visited.add(node)

        # Add the current node to the component list
        component.append(node)

        # Traverse all the neighbours of the current node
        for neighbour in graph[node]:

            # If the neighbour is not visited and has a positive value,
            # recursively visit it
            if neighbour not in visited and values[neighbour] != 0:

                # Recursively visit all the nodes in the connected
                # component
                self.dfs(graph, neighbour, values, visited, component)

    def connected_components(
        self, graph: List[List[int]], values: List[int]
    ) -> List[List[int]]:

        # Number of nodes in the graph
        n = len(graph)

        # Initialize visited set
        visited: Set[int] = set()

        # Initialize a list to store the connected components
        components: List[List[int]] = []

        # Iterate through all nodes in the graph
        for node in range(n):

            # Start DFS only if node is unvisited and has a positive
            # value, visiting all nodes in the connected component
            # and adding them to the components list
            if values[node] > 0 and node not in visited:

                # Create a new component to store the nodes in the
                # connected component
                component: List[int] = []

                # Start DFS from the current node and find all nodes
                # in the connected component
                self.dfs(graph, node, values, visited, component)

                # Add the found component to the components list
                components.append(component)

        # Return the list of connected components
        return components


# Examples from the problem statement
print(Solution().connected_components([[1],[0,2],[1,3],[2,4],[3,5],[4,6],[5]], [1,0,1,0,1,0,1]))  # [[0],[2],[4],[6]]
print(Solution().connected_components([[1],[0],[],[4],[3]], [1,1,1,1,1]))  # [[0,1],[2],[3,4]]

# Edge cases
print(Solution().connected_components([], []))                    # []
print(Solution().connected_components([[]], [1]))                  # [[0]]
print(Solution().connected_components([[]], [0]))                  # [] — zero value, unvisitable
print(Solution().connected_components([[1],[0]], [1,1]))           # [[0,1]]
print(Solution().connected_components([[1],[0]], [1,0]))           # [[0]] — node 1 blocked
# All zeros
print(Solution().connected_components([[1],[0]], [0,0]))           # []
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private void dfs(
            List<List<Integer>> graph,
            int node,
            int[] values,
            Set<Integer> visited,
            List<Integer> component
        ) {

            // Mark the current node as visited in the graph to avoid
            // visiting it again
            visited.add(node);

            // Add the current node to the component list
            component.add(node);

            // Traverse all the neighbours of the current node
            for (int neighbour : graph.get(node)) {

                // If the neighbour is not visited and has a positive value,
                // recursively visit it
                if (!visited.contains(neighbour) && values[neighbour] != 0) {

                    // Recursively visit all the nodes in the connected
                    // component
                    dfs(graph, neighbour, values, visited, component);
                }
            }
        }

        public List<List<Integer>> connectedComponents(
            List<List<Integer>> graph,
            int[] values
        ) {

            // Number of nodes in the graph
            int N = graph.size();

            // Initialize visited set
            Set<Integer> visited = new HashSet<>();

            // Initialize a list to store the connected components
            List<List<Integer>> components = new ArrayList<>();

            // Iterate through all nodes in the graph
            for (int node = 0; node < N; node++) {

                // Start DFS only if node is unvisited and has a positive
                // value, visiting all nodes in the connected component
                // and adding them to the components list
                if (values[node] > 0 && !visited.contains(node)) {

                    // Create a new component to store the nodes in the
                    // connected component
                    List<Integer> component = new ArrayList<>();

                    // Start DFS from the current node and find all nodes
                    // in the connected component
                    dfs(graph, node, values, visited, component);

                    // Add the found component to the components list
                    components.add(component);
                }
            }

            // Return the list of connected components
            return components;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.connectedComponents(List.of(List.of(1),List.of(0,2),List.of(1,3),List.of(2,4),List.of(3,5),List.of(4,6),List.of(5)), new int[]{1,0,1,0,1,0,1}));  // [[0],[2],[4],[6]]
        System.out.println(sol.connectedComponents(List.of(List.of(1),List.of(0),new ArrayList<>(),List.of(4),List.of(3)), new int[]{1,1,1,1,1}));  // [[0,1],[2],[3,4]]

        // Edge cases
        System.out.println(sol.connectedComponents(new ArrayList<>(), new int[]{}));  // []
        System.out.println(sol.connectedComponents(List.of(new ArrayList<>()), new int[]{1}));  // [[0]]
        System.out.println(sol.connectedComponents(List.of(new ArrayList<>()), new int[]{0}));  // []
        System.out.println(sol.connectedComponents(List.of(List.of(1), List.of(0)), new int[]{1,1}));  // [[0,1]]
        System.out.println(sol.connectedComponents(List.of(List.of(1), List.of(0)), new int[]{1,0}));  // [[0]]
        System.out.println(sol.connectedComponents(List.of(List.of(1), List.of(0)), new int[]{0,0}));  // []
    }
}
```

</details>
