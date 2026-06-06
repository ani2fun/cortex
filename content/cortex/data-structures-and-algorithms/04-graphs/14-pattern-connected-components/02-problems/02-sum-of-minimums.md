---
title: "Sum of Minimums"
summary: "For each connected component, find the minimum value among its nodes. Return the sum of those minima across all components."
prereqs:
  - 14-pattern-connected-components/01-pattern
difficulty: medium
---

# Problem: Sum of Minimums

## The Problem

For each connected component, find the minimum `value` among its nodes. Return the **sum of those minima** across all components.

```
Input:  graph = [[1], [0, 4], [3], [2], [1]], values = [2, 5, 1, 6, 7]
Output: 3
Explanation: Component {0, 1, 4} has min(2, 5, 7) = 2.
             Component {2, 3} has min(1, 6) = 1.
             2 + 1 = 3.
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: take min of running component-min and current node's value.
- `g`: sum across components.

The DFS now *returns* the component min instead of building a list. That's a small but important variation: the per-component aggregate doesn't need to be a parameter — it can be the function's return value.

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
        visited: Set[int],
        values: List[int],
    ) -> int:

        # Mark the current node as visited in the graph to avoid
        # visiting it again
        visited.add(node)

        # Make this as the minimum value so far
        minimum_so_far = values[node]

        # Traverse all the neighbours of the current node
        for neighbour in graph[node]:

            # If the neighbour is not visited, recursively call the DFS
            # function on the neighbour
            if neighbour not in visited:

                # Get the minimum value from all the connected nodes
                min_val = self.dfs(graph, neighbour, visited, values)

                # Update minimum_so_far if there was another node smaller
                # than it
                minimum_so_far = min(minimum_so_far, min_val)

        # Return the minimum value for this component
        return minimum_so_far

    def sum_of_minimums(
        self, graph: List[List[int]], values: List[int]
    ) -> int:

        # Number of nodes in the graph
        n = len(graph)

        # If the graph is empty, return 0
        if n == 0:
            return 0

        # Initialize visited set
        visited: Set[int] = set()

        # Initialise the minimum sum to 0
        min_sum = 0

        # Traverse all nodes in the graph
        for node in range(n):

            # If the node is already visited, all the nodes connected to
            # it are also visited
            if node in visited:
                continue

            # Perform DFS on this new node to visit all the nodes
            # connected to it and get the minimum value in it.
            min_val = self.dfs(graph, node, visited, values)

            # Add the min_val to the min_sum variable
            min_sum += min_val

        # Return the size of min_sum
        return min_sum


# Examples from the problem statement
print(Solution().sum_of_minimums([[1],[0,4],[3],[2],[1]], [2,5,1,6,7]))  # 3
print(Solution().sum_of_minimums([[1],[0],[],[4],[3]], [2,5,1,6,7]))     # 9

# Edge cases
print(Solution().sum_of_minimums([], []))                               # 0
print(Solution().sum_of_minimums([[]], [5]))                             # 5
print(Solution().sum_of_minimums([[1],[0]], [3,7]))                      # 3
print(Solution().sum_of_minimums([[],[],[]], [4,2,9]))                   # 15 — 3 isolated nodes
print(Solution().sum_of_minimums([[1,2],[0],[0]], [1,2,3]))              # 1 — one component
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private int dfs(
            List<List<Integer>> graph,
            int node,
            Set<Integer> visited,
            int[] values
        ) {

            // Mark the current node as visited in the graph to avoid
            // visiting it again
            visited.add(node);

            // Make this as the minimum value so far
            int minimumSoFar = values[node];

            // Traverse all the neighbours of the current node
            for (int neighbour : graph.get(node)) {

                // If the neighbour is not visited, recursively call the DFS
                // function on the neighbour
                if (!visited.contains(neighbour)) {

                    // Get the minimum value from all the connected nodes
                    int minVal = dfs(graph, neighbour, visited, values);

                    // Update minimumSoFar if there was another node smaller
                    // than it
                    minimumSoFar = Math.min(minimumSoFar, minVal);
                }
            }

            // Return the minimum value for this component
            return minimumSoFar;
        }

        public int sumOfMinimums(List<List<Integer>> graph, int[] values) {

            // Number of nodes in the graph
            int N = graph.size();

            // If the graph is empty, return 0
            if (N == 0) {
                return 0;
            }

            // Initialize visited set
            Set<Integer> visited = new HashSet<>();

            // Initialise the minimum sum to 0
            int minSum = 0;

            // Traverse all nodes in the graph
            for (int node = 0; node < N; node++) {

                // If the node is already visited, continue to the next node
                if (visited.contains(node)) {
                    continue;
                }

                // Perform DFS on this new node to visit all the nodes
                // connected to it and get the minimum value in it.
                int minVal = dfs(graph, node, visited, values);

                // Add the minVal to the minSum variable
                minSum += minVal;
            }

            // Return the size of minSum
            return minSum;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.sumOfMinimums(List.of(List.of(1),List.of(0,4),List.of(3),List.of(2),List.of(1)), new int[]{2,5,1,6,7}));  // 3
        System.out.println(sol.sumOfMinimums(List.of(List.of(1),List.of(0),new ArrayList<>(),List.of(4),List.of(3)), new int[]{2,5,1,6,7}));  // 9

        // Edge cases
        System.out.println(sol.sumOfMinimums(new ArrayList<>(), new int[]{}));  // 0
        System.out.println(sol.sumOfMinimums(List.of(new ArrayList<>()), new int[]{5}));  // 5
        System.out.println(sol.sumOfMinimums(List.of(List.of(1), List.of(0)), new int[]{3,7}));  // 3
        System.out.println(sol.sumOfMinimums(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new int[]{4,2,9}));  // 15
        System.out.println(sol.sumOfMinimums(List.of(List.of(1,2), List.of(0), List.of(0)), new int[]{1,2,3}));  // 1
    }
}
```

</details>
