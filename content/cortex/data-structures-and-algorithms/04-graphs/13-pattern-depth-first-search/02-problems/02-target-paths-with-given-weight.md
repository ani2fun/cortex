---
title: "Target Paths with Given Weight"
summary: "Given a weighted directed graph, source, destination, and target weight, return all paths from source to destination whose edge weights sum to exactly the target."
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: medium
---

# Problem: Target Paths With Given Weight

## The Problem

Given a **weighted** directed graph, source, destination, and target weight, return all paths from source to destination whose **edge weights sum to exactly the target**.

```
Input:  graph = [[(1,2),(3,5)], [(4,2)], [(4,1)], [(2,2)], [(3,1)]],
        source = 0, destination = 3, target = 5
Output: [[0,1,4,3], [0,3]]
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: append node to path list AND add edge weight to running sum.
- `g`: append the path *only if* the running sum equals target.
- `f⁻¹`: pop node AND subtract the edge weight on exit.

The only twist from the previous problem is the running edge-weight sum carried alongside the path.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=graph
from typing import List, Set, Tuple

class Solution:
    def dfs(
        self,
        graph: List[List[Tuple[int, int]]],
        node: int,
        destination: int,
        current_sum: int,
        target: int,
        path: List[int],
        paths: List[List[int]],
        nodes_in_path: Set[int],
    ) -> None:

        # Insert the current node into the set of nodes in the current
        # path to avoid revisiting the same node
        nodes_in_path.add(node)

        # Add the current node to the path
        path.append(node)

        # If the current node is the destination and the path sum equals
        # the target sum, store the current path
        if node == destination and current_sum == target:
            paths.append(path.copy())

        # Else, explore all the neighbours of the current node
        else:
            for neighbour, weight in graph[node]:

                # Perform DFS on the neighbour node if it is not already
                # in the current path to avoid cycles
                if neighbour not in nodes_in_path:

                    # Explore neighbour and add its edge weight to the
                    # current sum
                    self.dfs(
                        graph,
                        neighbour,
                        destination,
                        current_sum + weight,
                        target,
                        path,
                        paths,
                        nodes_in_path,
                    )

        # Remove the current node from the path as we are done exploring
        # it
        path.pop()

        # Remove the current node from the set of nodes in the current
        # path to allow it to be visited again in other paths
        nodes_in_path.remove(node)

    def target_paths(
        self,
        graph: List[List[Tuple[int, int]]],
        source: int,
        destination: int,
        target: int,
    ) -> List[List[int]]:

        # Result list to store all the Hamiltonian paths
        paths: List[List[int]] = []

        # List to store the current path being explored
        path: List[int] = []

        # Set to keep track of nodes currently in the path
        nodes_in_path: Set[int] = set()

        # Perform DFS starting from the source node with an initial sum
        # of 0
        self.dfs(
            graph,
            source,
            destination,
            0,
            target,
            path,
            paths,
            nodes_in_path,
        )

        # Return the list of valid paths with the given sum
        return paths


# Examples from the problem statement
print(Solution().target_paths([[[1,2],[3,5]],[[4,2]],[[4,1]],[[2,2]],[[3,1]]], 0, 3, 5))  # [[0,1,4,3],[0,3]]
print(Solution().target_paths([[[4,2]],[[3,3],[0,4]],[[4,3],[0,1]],[[2,1],[4,4]],[[1,5]]], 3, 4, 4))  # [[3,2,4],[3,2,0,4],[3,4]]

# Edge cases
print(Solution().target_paths([], 0, 0, 0))                           # []
print(Solution().target_paths([[]], 0, 0, 0))                         # [[0]]
print(Solution().target_paths([[[1,3]],[]], 0, 1, 3))                 # [[0,1]]
print(Solution().target_paths([[[1,3]],[]], 0, 1, 5))                 # [] — wrong weight
print(Solution().target_paths([[[1,2],[2,3]],[[2,1]],[]], 0, 2, 3))   # [[0,2],[0,1,2]]
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private void dfs(
            List<List<List<Integer>>> graph,
            int node,
            int destination,
            int currentSum,
            int target,
            List<Integer> path,
            List<List<Integer>> paths,
            Set<Integer> nodesInPath
        ) {

            // Insert the current node into the set of nodes in the current
            // path to avoid revisiting the same node
            nodesInPath.add(node);

            // Add the current node to the path
            path.add(node);

            // If the current node is the destination and the path sum equals
            // the target sum, store the current path
            if (node == destination && currentSum == target) {
                paths.add(new ArrayList<>(path));
            }

            // Else, explore all the neighbours of the current node
            else {
                for (List<Integer> edge : graph.get(node)) {
                    int neighbour = edge.get(0);
                    int weight = edge.get(1);

                    // Perform DFS on the neighbour node if it is not already
                    // in the current path to avoid cycles
                    if (!nodesInPath.contains(neighbour)) {

                        // Explore neighbour and add its edge weight to the
                        // current sum
                        dfs(
                            graph,
                            neighbour,
                            destination,
                            currentSum + weight,
                            target,
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
            // path to allow it to be visited again in other paths
            nodesInPath.remove(node);
        }

        public List<List<Integer>> targetPaths(
            List<List<List<Integer>>> graph,
            int source,
            int destination,
            int target
        ) {

            // Result list to store all the Hamiltonian paths
            List<List<Integer>> paths = new ArrayList<>();

            // List to store the current path being explored
            List<Integer> path = new ArrayList<>();

            // Set to keep track of nodes currently in the path
            Set<Integer> nodesInPath = new HashSet<>();

            // Perform DFS starting from the source node with an initial sum
            // of 0
            dfs(
                graph,
                source,
                destination,
                0,
                target,
                path,
                paths,
                nodesInPath
            );

            // Return the list of valid paths with the given sum
            return paths;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.targetPaths(List.of(List.of(List.of(1,2),List.of(3,5)),List.of(List.of(4,2)),List.of(List.of(4,1)),List.of(List.of(2,2)),List.of(List.of(3,1))), 0, 3, 5));  // [[0,1,4,3],[0,3]]
        System.out.println(sol.targetPaths(List.of(List.of(List.of(4,2)),List.of(List.of(3,3),List.of(0,4)),List.of(List.of(4,3),List.of(0,1)),List.of(List.of(2,1),List.of(4,4)),List.of(List.of(1,5))), 3, 4, 4));  // [[3,2,4],[3,2,0,4],[3,4]]

        // Edge cases
        System.out.println(sol.targetPaths(List.of(new ArrayList<>()), 0, 0, 0));  // [[0]]
        System.out.println(sol.targetPaths(List.of(List.of(List.of(1,3)), new ArrayList<>()), 0, 1, 3));  // [[0,1]]
        System.out.println(sol.targetPaths(List.of(List.of(List.of(1,3)), new ArrayList<>()), 0, 1, 5));  // []
        System.out.println(sol.targetPaths(List.of(List.of(List.of(1,2),List.of(2,3)),List.of(List.of(2,1)),new ArrayList<>()), 0, 2, 3));  // [[0,2],[0,1,2]]
    }
}
```

</details>
