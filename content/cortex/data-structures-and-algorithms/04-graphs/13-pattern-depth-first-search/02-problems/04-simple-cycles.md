---
title: "Simple Cycles"
summary: "Given a directed graph, source, and destination, count the number of simple cycles that *start at the source*, *pass through the destination*, and *return to the source* without repeating any other no"
prereqs:
  - 13-pattern-depth-first-search/01-pattern
difficulty: medium
---

# Problem: Simple Cycles

## The Problem

Given a directed graph, source, and destination, count the number of **simple cycles** that *start at the source*, *pass through the destination*, and *return to the source* without repeating any other node.

```
Input:  graph = [[1, 2], [0, 2, 3], [0, 1, 3], [1, 2]], source = 0, destination = 3
Output: 2
Explanation: Cycles 0 → 1 → 3 → 2 → 0 and 0 → 2 → 3 → 1 → 0 both start/end at 0 and pass through 3.
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


- `f`: same in_path tracking.
- The loop check at each step: if a neighbour is the *source* AND the path has length ≥ 3 (a cycle needs at least 3 nodes) AND the destination has been visited along the way → count one cycle.
- `f⁻¹`: same.

The only structural difference from previous problems: there's no explicit "destination reached → record" branch. Instead, the cycle-completion check is *inline* with the neighbour iteration — when we find a neighbour that's the source and the path qualifies, we increment the counter.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=graph viz-root=graph
from typing import List, Set

class Solution:
    def __init__(self) -> None:

        # Counter to store total simple cycles
        self.cycles: int = 0

    def dfs(
        self,
        graph: List[List[int]],
        node: int,
        source: int,
        destination: int,
        nodes_in_path: Set[int],
    ) -> None:

        # Insert the current node into the set of nodes in the current
        # path to detect cycles
        nodes_in_path.add(node)

        # Explore all neighbors of the current node
        for neighbor in graph[node]:

            # Case 1: Neighbor is not visited yet, continue DFS
            if neighbor not in nodes_in_path:
                self.dfs(
                    graph, neighbor, source, destination, nodes_in_path
                )

            # Case 2: Neighbor is the starting node and forms a valid cycle
            # Path must have at least 3 nodes and include the destination
            elif (
                neighbor == source
                and len(nodes_in_path) > 2
                and destination in nodes_in_path
            ):
                self.cycles += 1

        # Remove the current node from the current path as we are done
        # exploring it
        nodes_in_path.remove(node)

    def simple_cycles(
        self, graph: List[List[int]], source: int, destination: int
    ) -> int:

        # Set to keep track of nodes in the current path
        nodes_in_path: Set[int] = set()

        # Perform DFS starting from the source node
        self.dfs(graph, source, source, destination, nodes_in_path)

        # Return total cycles found
        return self.cycles


# Examples from the problem statement
print(Solution().simple_cycles([[1,2],[0,2,3],[0,1,3],[1,2]], 0, 3))  # 2
print(Solution().simple_cycles([[1],[0,2],[1,3],[2]], 0, 3))           # 0

# Edge cases
print(Solution().simple_cycles([[0]], 0, 0))                           # 0 — self-loop, not 3+ nodes
print(Solution().simple_cycles([[1],[0]], 0, 0))                       # 0 — cycle length 2, no dest!=src
print(Solution().simple_cycles([[1,2],[2,0],[0,1]], 0, 1))             # 1
# Source == destination: any cycle through it counts
print(Solution().simple_cycles([[1,2],[2,0],[0,1]], 0, 0))             # 0 — src==dst, must pass through dst
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {

        // Counter to store total simple cycles
        private int cycles = 0;

        private void dfs(
            List<List<Integer>> graph,
            int node,
            int source,
            int destination,
            Set<Integer> nodesInPath
        ) {

            // Insert the current node into the set of nodes in the current
            // path to detect cycles
            nodesInPath.add(node);

            // Explore all neighbors of the current node
            for (int neighbor : graph.get(node)) {

                // Case 1: Neighbor is not visited yet, continue DFS
                if (!nodesInPath.contains(neighbor)) {
                    dfs(graph, neighbor, source, destination, nodesInPath);
                }

                // Case 2: Neighbor is the starting node and forms a valid
                // cycle Path must have at least 3 nodes and include the
                // destination
                else if (
                    neighbor == source &&
                    nodesInPath.size() > 2 &&
                    nodesInPath.contains(destination)
                ) {
                    cycles++;
                }
            }

            // Remove the current node from the current path as we are done
            // exploring it
            nodesInPath.remove(node);
        }

        public int simpleCycles(
            List<List<Integer>> graph,
            int source,
            int destination
        ) {

            // Set to keep track of nodes in the current path
            Set<Integer> nodesInPath = new HashSet<>();

            // Perform DFS starting from the source node
            dfs(graph, source, source, destination, nodesInPath);

            // Return total cycles found
            return cycles;
        }
    }

    public static void main(String[] args) {
        // Examples from the problem statement
        System.out.println(new Solution().simpleCycles(List.of(List.of(1,2),List.of(0,2,3),List.of(0,1,3),List.of(1,2)), 0, 3));  // 2
        System.out.println(new Solution().simpleCycles(List.of(List.of(1),List.of(0,2),List.of(1,3),List.of(2)), 0, 3));           // 0

        // Edge cases
        System.out.println(new Solution().simpleCycles(List.of(List.of(0)), 0, 0));  // 0
        System.out.println(new Solution().simpleCycles(List.of(List.of(1), List.of(0)), 0, 0));  // 0
        System.out.println(new Solution().simpleCycles(List.of(List.of(1,2), List.of(2,0), List.of(0,1)), 0, 1));  // 1
    }
}
```

### Complexity Analysis

| | Complexity | Reasoning |
|---|---|---|
| **Time** | O(V! × E) worst case | Number of paths can be up to V! in dense graphs; each visit costs O(E) |
| **Space** | O(V) | Recursion depth + path storage + in_path set |

This is the price of enumeration — exponential in the worst case. The `in_path` constraint prunes heavily on most real inputs.

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


The DFS pattern is **the** tool when you need to *enumerate, score, or filter* paths through a graph. Once you internalise the four-step recipe — *enter, check destination, recurse, leave* — the rest is choosing what `f` and `g` should compute.

Watch for the giveaways: phrasing like *"all paths"*, *"paths with [property]"*, *"count cycles"*, *"Hamiltonian"*, *"longest/shortest path"* — these are pattern-matching signals that DFS is the right approach.

The next pattern lessons explore three other DFS-flavoured problem families: **connected components** (count or label disjoint pieces of a graph), **two-colouring** (test for bipartiteness), and **shortest paths** with BFS and Dijkstra. Each one applies a small twist to DFS or BFS — and once you recognise the family, the implementation is mechanical.

> **Transfer challenge.** A delivery-robot pathfinding system needs to count the number of distinct valid routes from a warehouse to a destination, with the constraint that the route cost (sum of edge weights) is below a budget. Sketch the f and g you'd use.

</details>
<details>
<summary><strong>Sketch</strong></summary>

- `f` (per node): add edge weight to running sum.
- `g` (at destination): if running sum ≤ budget, increment a counter.
- `f⁻¹` (on exit): subtract edge weight.

This is exactly "Target paths with given weight" generalised from "= target" to "≤ budget". Same skeleton; one symbol changes.

</details>
