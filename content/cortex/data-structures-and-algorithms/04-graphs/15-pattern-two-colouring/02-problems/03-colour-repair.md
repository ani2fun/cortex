---
title: "Colour Repair"
summary: "A graph that's *almost* two-colourable: it can be made bipartite by removing at most one edge. Return true if so, false otherwise."
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: medium
---

# Problem: Colour Repair

## The Problem

A graph that's *almost* two-colourable: it can be made bipartite by removing **at most one** edge. Return `true` if so, `false` otherwise.

```
Input:  graph = [[1, 3], [0, 2, 3], [1, 3], [0, 1, 2]]
Output: true (remove edge 1-3)
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


The trick: instead of returning `false` immediately at a colour conflict, **record the conflicting edge** and keep colouring. At the end, count distinct conflicts:

- 0 conflicts → already two-colourable; trivially repairable.
- 1 conflict → removing that one edge restores bipartiteness.
- 2+ conflicts → can't be fixed with a single edge removal.

Because the graph is undirected, each conflict edge gets recorded twice (once from each endpoint). Divide the count by 2 to get distinct conflicts.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=graph
from typing import List, Dict, Tuple

class Solution:
    def colour_graph(
        self,
        graph: List[List[int]],
        node: int,
        colour: Dict[int, int],
        colour_value: int,
        conflicts: List[Tuple[int, int]],
    ) -> bool:

        # Colour the node with colourValue
        colour[node] = colour_value

        # Traverse all the neighbours of the current node
        for neighbour in graph[node]:

            # If the neighbour is not coloured, colour it with the
            # opposite colour
            if neighbour not in colour:
                if not self.colour_graph(
                    graph, neighbour, colour, 1 - colour_value, conflicts
                ):
                    return False

            # Else if the neighbour is coloured with the same colour,
            # record the conflict
            elif colour.get(neighbour) == colour_value:
                conflicts.append((node, neighbour))

        return True

    def colour_repair(self, graph: List[List[int]]) -> bool:
        n = len(graph)

        # If the graph is empty, return false
        if n == 0:
            return False

        # Create a map to store the colour of each node
        colour: Dict[int, int] = {}

        # List to store all edges that cause conflicts (same-coloured
        # endpoints)
        conflicts: List[Tuple[int, int]] = []

        # Traverse all nodes in the graph
        for node in range(n):

            # If a node is not coloured, start colouring its connected
            # component recursively
            if node not in colour:
                self.colour_graph(graph, node, colour, 0, conflicts)

        # The graph can be made bipartite if there is at most one
        # conflict edge. Divide by 2 to account for double counting
        # of edges in an undirected graph
        return len(conflicts) // 2 <= 1


# Examples from the problem statement
print(Solution().colour_repair([[1,3],[0,2,3],[1,3],[0,1,2]]))  # True
print(Solution().colour_repair([[1,2,3],[0,2],[0,1],[0]]))      # True

# Edge cases
print(Solution().colour_repair([]))                              # False
print(Solution().colour_repair([[1],[0]]))                       # True — no conflict
print(Solution().colour_repair([[1,2],[0,2],[0,1]]))             # True — triangle: 1 conflict edge
# Two conflict edges — needs 2 removals, not possible
print(Solution().colour_repair([[1,2,3],[0,2,3],[0,1,3],[0,1,2]]))  # False
print(Solution().colour_repair([[],[]]))                         # True — no edges
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private boolean colourGraph(
            List<List<Integer>> graph,
            int node,
            Map<Integer, Integer> colour,
            int colourValue,
            List<List<Integer>> conflicts
        ) {

            // Colour the node with colourValue
            colour.put(node, colourValue);

            // Traverse all the neighbours of the current node
            for (int neighbour : graph.get(node)) {

                // If the neighbour is not coloured, colour it with the
                // opposite colour
                if (!colour.containsKey(neighbour)) {
                    if (
                        !colourGraph(
                            graph,
                            neighbour,
                            colour,
                            1 - colourValue,
                            conflicts
                        )
                    ) {
                        return false;
                    }
                }

                // Else if the neighbour is coloured with the same colour,
                // record the conflict
                else if (colour.get(neighbour) == colourValue) {
                    conflicts.add(Arrays.asList(node, neighbour));
                }
            }

            return true;
        }

        public boolean colourRepair(List<List<Integer>> graph) {
            int N = graph.size();

            // If the graph is empty, return false
            if (N == 0) {
                return false;
            }

            // Create a map to store the colour of each node
            Map<Integer, Integer> colour = new HashMap<>();

            // List to store all edges that cause conflicts (same-coloured
            // endpoints)
            List<List<Integer>> conflicts = new ArrayList<>();

            // Traverse all nodes in the graph
            for (int node = 0; node < N; node++) {

                // If a node is not coloured, start colouring its connected
                // component recursively
                if (!colour.containsKey(node)) {
                    colourGraph(graph, node, colour, 0, conflicts);
                }
            }

            // The graph can be made bipartite if there is at most one
            // conflict edge. Divide by 2 to account for double counting
            // of edges in an undirected graph
            return conflicts.size() / 2 <= 1;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.colourRepair(List.of(List.of(1,3),List.of(0,2,3),List.of(1,3),List.of(0,1,2))));  // true
        System.out.println(sol.colourRepair(List.of(List.of(1,2,3),List.of(0,2),List.of(0,1),List.of(0))));      // true

        // Edge cases
        System.out.println(sol.colourRepair(new ArrayList<>()));                   // false
        System.out.println(sol.colourRepair(List.of(List.of(1), List.of(0))));     // true
        System.out.println(sol.colourRepair(List.of(List.of(1,2),List.of(0,2),List.of(0,1))));  // true
        System.out.println(sol.colourRepair(List.of(List.of(1,2,3),List.of(0,2,3),List.of(0,1,3),List.of(0,1,2))));  // false
        System.out.println(sol.colourRepair(List.of(new ArrayList<>(), new ArrayList<>())));  // true
    }
}
```

</details>
