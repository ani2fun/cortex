---
title: "Group Colourable"
summary: "Given an undirected graph represented as an adjacency list and a list of groups, write a function that returns true if the graph can be colored with two colours and false otherwise."
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: hard
---

# Problem: Group Colourable

## The Problem

Given an **undirected** **graph** represented as an adjacency list and a list of **groups**, write a function that returns `true` if the graph can be colored with **two** colours and `false` otherwise.

The graph is given as follows: `graph[i]` is a list of all nodes you can visit from node `i` (i.e., there is a directed edge from node `i` to node `graph[i][j]`).

> You must abide by the following constraint:
>
> -   You must colour the graph such that no two adjacent vertices of the graph are colored with the same colour.
> -   All the nodes in a given group should be coloured with the same colour.

```
Input:  graph = [[1, 3], [0, 2], [1, 3], [0, 2]], groups = [[0, 2], [1, 3]]
Output: true
Input:  graph = [[1, 3], [0, 2], [1, 3], [0, 2]], groups = [[0, 1], [2, 3]]
Output: false
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


This is two-colouring with an extra constraint stacked on top: every node in a group must share one colour. The solution still alternates colours during DFS, but before exploring a node's neighbours it calls `colorGroup`, which propagates the node's colour to every other member of its group — failing if a group member is already coloured differently. A `group_map` precomputes, for each node, the full list of its group-mates so this check is a quick lookup.

The two failure modes are unchanged in spirit: a same-colour adjacency conflict, or a same-group node that's already been forced into the opposite colour. An empty graph returns `false`, matching the base two-colourable convention.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=graph
from typing import List, Dict

class Solution:
    def color_group(
        self,
        node: int,
        colour: Dict[int, int],
        colour_value: int,
        group_map: Dict[int, List[int]],
    ) -> bool:

        # If node belongs to a group, assign the same colour to all
        # nodes in the group
        if node in group_map:

            # Traverse all nodes in the group and assign them the same
            # colour
            for group_node in group_map[node]:

                # If the group node is not coloured, colour it with the same
                # colour as the current node
                if group_node not in colour:
                    colour[group_node] = colour_value

                # If the group node is coloured with a different
                # colour, return false
                elif colour[group_node] != colour_value:
                    return False

        # If all group nodes are coloured successfully,
        return True

    def colour_graph(
        self,
        graph: List[List[int]],
        node: int,
        colour: Dict[int, int],
        colour_value: int,
        group_map: Dict[int, List[int]],
    ) -> bool:

        # Colour the node with colourValue
        colour[node] = colour_value

        # If node belongs to a group, assign the same colour to all
        # nodes in the group, if it fails return false
        if not self.color_group(node, colour, colour_value, group_map):
            return False

        # Traverse all the neighbours of the current node
        for neighbour in graph[node]:

            # If the neighbour is not coloured, colour it with the
            # opposite colour and recursively call the function on the
            # neighbour
            if neighbour not in colour:

                # If the neighbour is not coloured, colour it with the
                # opposite colour
                if not self.colour_graph(
                    graph, neighbour, colour, 1 - colour_value, group_map
                ):

                    # If the colouring fails, return false
                    # (i.e., if a neighbour has the same colour)
                    return False

            # Else if the neighbour is coloured with the same colour
            # return false
            elif colour[neighbour] == colour_value:
                return False

        return True

    def group_colourable(
        self, graph: List[List[int]], groups: List[List[int]]
    ) -> bool:
        n = len(graph)

        # If the graph is empty, return false
        if n == 0:
            return False

        # Create a map to store the colour of each node
        colour: Dict[int, int] = {}

        # Map each node to all nodes in its group
        group_map: Dict[int, List[int]] = {}
        for group in groups:
            for node in group:
                group_map[node] = group

        # Traverse all nodes in the graph
        for node in range(len(graph)):

            # If a node is not coloured, start colouring its
            # connected component recursively starting with colour 1
            if node not in colour:

                # If the colouring fails, return false
                # (i.e., if a neighbour has the same colour)
                if not self.colour_graph(
                    graph, node, colour, 1, group_map
                ):
                    return False

        # If all nodes are coloured successfully, return true
        return True


# Examples from the problem statement
print(Solution().group_colourable([[1,3],[0,2],[1,3],[0,2]], [[0,2],[1,3]]))  # True
print(Solution().group_colourable([[1,3],[0,2],[1,3],[0,2]], [[0,1],[2,3]]))  # False

# Edge cases
print(Solution().group_colourable([], []))                                    # False
print(Solution().group_colourable([[1],[0]], [[0],[1]]))                      # True
# All in same group adjacent to each other
print(Solution().group_colourable([[1],[0]], [[0,1]]))                        # False — adjacent grouped nodes
print(Solution().group_colourable([[1,3],[0,2],[1,3],[0,2]], []))             # True — no group constraints
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private boolean colorGroup(
            int node,
            Map<Integer, Integer> colour,
            int colourValue,
            Map<Integer, List<Integer>> groupMap
        ) {

            // If node belongs to a group, assign the same colour to all
            // nodes in the group
            if (groupMap.containsKey(node)) {

                // Traverse all nodes in the group and assign them the same
                // colour
                for (int groupNode : groupMap.get(node)) {

                    // If the group node is not coloured, colour it with the
                    // same colour as the current node
                    if (!colour.containsKey(groupNode)) {
                        colour.put(groupNode, colourValue);
                    }

                    // If the group node is coloured with a different
                    // colour, return false
                    else if (colour.get(groupNode) != colourValue) {
                        return false;
                    }
                }
            }

            // If all group nodes are coloured successfully,
            return true;
        }

        private boolean colourGraph(
            List<List<Integer>> graph,
            int node,
            Map<Integer, Integer> colour,
            int colourValue,
            Map<Integer, List<Integer>> groupMap
        ) {

            // Colour the node with colourValue
            colour.put(node, colourValue);

            // If node belongs to a group, assign the same colour to all
            // nodes in the group, if it fails return false
            if (!colorGroup(node, colour, colourValue, groupMap)) {
                return false;
            }

            // Traverse all the neighbours of the current node
            for (int neighbour : graph.get(node)) {

                // If the neighbour is not coloured, colour it with the
                // opposite colour and recursively call the function on the
                // neighbour
                if (!colour.containsKey(neighbour)) {

                    // If the neighbour is not coloured, colour it with the
                    // opposite colour
                    if (
                        !colourGraph(
                            graph,
                            neighbour,
                            colour,
                            1 - colourValue,
                            groupMap
                        )
                    ) {

                        // If the colouring fails, return false
                        // (i.e., if a neighbour has the same colour)
                        return false;
                    }
                }

                // Else if the neighbour is coloured with the same colour
                // return false
                else if (colour.get(neighbour) == colourValue) {
                    return false;
                }
            }

            return true;
        }

        public boolean groupColourable(
            List<List<Integer>> graph,
            List<List<Integer>> groups
        ) {
            int N = graph.size();

            // If the graph is empty, return false
            if (N == 0) {
                return false;
            }

            // Create a map to store the colour of each node
            Map<Integer, Integer> colour = new HashMap<>();

            // Map each node to all nodes in its group
            Map<Integer, List<Integer>> groupMap = new HashMap<>();
            for (List<Integer> group : groups) {
                for (int node : group) {
                    groupMap.put(node, group);
                }
            }

            // Traverse all nodes in the graph
            for (int node = 0; node < graph.size(); node++) {

                // If a node is not coloured, start colouring its
                // connected component recursively starting with colour 1
                if (!colour.containsKey(node)) {

                    // If the colouring fails, return false
                    // (i.e., if a neighbour has the same colour)
                    if (!colourGraph(graph, node, colour, 1, groupMap)) {
                        return false;
                    }
                }
            }

            // If all nodes are coloured successfully, return true
            return true;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.groupColourable(List.of(List.of(1,3),List.of(0,2),List.of(1,3),List.of(0,2)), List.of(List.of(0,2),List.of(1,3))));  // true
        System.out.println(sol.groupColourable(List.of(List.of(1,3),List.of(0,2),List.of(1,3),List.of(0,2)), List.of(List.of(0,1),List.of(2,3))));  // false

        // Edge cases
        System.out.println(sol.groupColourable(new ArrayList<>(), new ArrayList<>()));  // false
        System.out.println(sol.groupColourable(List.of(List.of(1), List.of(0)), List.of(List.of(0), List.of(1))));  // true
        System.out.println(sol.groupColourable(List.of(List.of(1), List.of(0)), List.of(List.of(0, 1))));  // false
        System.out.println(sol.groupColourable(List.of(List.of(1,3),List.of(0,2),List.of(1,3),List.of(0,2)), new ArrayList<>()));  // true
    }
}
```

</details>
<details>
<summary><h2>Key Takeaway</h2></summary>


Two-colouring is a deceptively simple algorithm — alternate colours during DFS, conflict on same-colour adjacency — that decides one of the most fundamental properties of a graph: **bipartiteness**. Once you can compute it, an entire family of "split into two" problems becomes a 10-line function.

The key idea: **treat structural questions as colouring questions**. Whenever a problem asks *"can these be split into two camps?"*, *"is this an alternating arrangement?"*, *"can pairings avoid conflicts?"* — reach for two-colouring. If you can colour the graph, the structural property holds; if you find a conflict, it doesn't.

Two more pattern lessons remain — **shortest path with BFS** (the unweighted version) and **shortest path with Dijkstra** (the weighted version). They wrap algorithms you've already met into the pattern-recognition framework that makes them easy to deploy.

> **Transfer challenge.** A meeting room can host two parallel sessions. You have a list of "incompatible-session" pairs (because of overlapping speakers, shared equipment, etc.). Sketch how you'd decide whether you can schedule all sessions in two streams without conflicts.

</details>
<details>
<summary><strong>Sketch</strong></summary>

Build an undirected graph with sessions as nodes and incompatibility pairs as edges. Run two-colouring. If the graph is two-colourable, the colour assignment *is* the stream assignment — colour 0 = stream A, colour 1 = stream B. If not, you can't fit everything in two streams (you'd need more rooms or some sessions to be cancelled).

This is *literally* the dislike-pairs problem with sessions instead of people. Same algorithm, same code.

</details>
