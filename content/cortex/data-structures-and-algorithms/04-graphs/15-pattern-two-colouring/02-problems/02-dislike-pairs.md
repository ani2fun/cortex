---
title: "Dislike Pairs"
summary: "N people, a list of dislikes pairs. Can the people be split into two groups such that no two who dislike each other end up in the same group?"
prereqs:
  - 15-pattern-two-colouring/01-pattern
difficulty: medium
---

# Problem: Dislike Pairs

## The Problem

`N` people, a list of `dislikes` pairs. Can the people be split into two groups such that no two who dislike each other end up in the same group?

```
Input:  N = 4, dislikes = [[1, 3], [0, 2], [1, 3], [0, 2]]
Output: true (groups {0, 2} and {1, 3})

Input:  N = 3, dislikes = [[0, 1], [1, 2], [2, 0]]
Output: false (3-cycle of dislikes)
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


This is *literally* two-colourable in disguise:

- People = nodes.
- Dislike pairs = edges.
- "Same group" = same colour.
- "No same-group dislikes" = no same-colour edge endpoints.

Build the adjacency list from the dislikes list (undirected — both directions), then run two-colouring.

> *Before reading on — why is the dislikes graph **undirected**? What would change if we were told "A dislikes B but B's feelings toward A aren't given"?*

Dislikes here are mutual: if A dislikes B, the conflict is the same as if B dislikes A. The graph is undirected. If the dislikes were one-way, you could *still* use this same algorithm — antagonism in a graph is asymmetric only when one side feels it, but the *seating constraint* ("don't put them together") is symmetric. So even a directed-dislikes input would be solved by treating each edge as undirected.

The implementation just adds an edge-list-to-adjacency-list build step in front of the colouring code:


```python run viz=graph viz-root=graph
from typing import List, Dict

class Solution:
    def colour_graph(
        self,
        graph: List[List[int]],
        node: int,
        colour: Dict[int, int],
        colour_value: int,
    ) -> bool:

        # Colour the node with colourValue
        colour[node] = colour_value

        # Traverse all the neighbours of the current node
        for neighbour in graph[node]:

            # If the neighbour is not coloured, colour it with the
            # opposite colour and recursively call the function on the
            # neighbour
            if neighbour not in colour:

                # If the neighbour is not coloured, colour it with the
                # opposite colour
                if not self.colour_graph(
                    graph, neighbour, colour, 1 - colour_value
                ):

                    # If the colouring fails, return false
                    # (i.e., if a neighbour has the same colour)
                    return False

            # Else if the neighbour is coloured with the same colour
            # return false
            elif colour[neighbour] == colour_value:
                return False

        return True

    def dislike_pairs(self, n: int, dislikes: List[List[int]]) -> bool:

        # If the number of people is 0 return false
        if n == 0:
            return False

        # Create an adjacency list for the graph
        graph: List[List[int]] = [[] for _ in range(n)]

        # Add edges to the graph nodes by updating
        # the adjacency list
        for dislike in dislikes:
            graph[dislike[0]].append(dislike[1])
            graph[dislike[1]].append(dislike[0])

        # Create a map to store the colour of each node
        colour: Dict[int, int] = {}

        for node in range(len(graph)):

            # If a node is not coloured, start coloring its
            # connected component recursively starting with colour 1
            if node not in colour:

                # If the colouring fails, return false
                # (i.e., if a neighbour has the same colour)
                if not self.colour_graph(graph, node, colour, 1):
                    return False

        # If all nodes are coloured successfully, return true
        return True


# Examples from the problem statement
print(Solution().dislike_pairs(4, [[1,3],[0,2],[1,3],[0,2]]))  # True
print(Solution().dislike_pairs(3, [[0,1],[1,2],[2,0]]))        # False

# Edge cases
print(Solution().dislike_pairs(0, []))                          # False
print(Solution().dislike_pairs(1, []))                          # True
print(Solution().dislike_pairs(2, [[0,1]]))                     # True
print(Solution().dislike_pairs(4, []))                          # True — no dislikes
# Triangle = odd cycle
print(Solution().dislike_pairs(3, [[0,1],[0,2],[1,2]]))         # False
```

```java run viz=graph viz-root=graph
import java.util.*;

public class Main {
    static class Solution {
        private boolean colourGraph(
            List<List<Integer>> graph,
            int node,
            Map<Integer, Integer> colour,
            int colourValue
        ) {

            // Colour the node with colourValue
            colour.put(node, colourValue);

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
                            1 - colourValue
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

        public boolean dislikePairs(int N, List<List<Integer>> dislikes) {

            // If the number of people is 0 return false
            if (N == 0) {
                return false;
            }

            // Create an adjacency list for the graph
            List<List<Integer>> graph = new ArrayList<>();
            for (int i = 0; i < N; i++) {
                graph.add(new ArrayList<>());
            }

            // Add edges to the graph nodes by updating the adjacency list
            for (List<Integer> dislike : dislikes) {
                graph.get(dislike.get(0)).add(dislike.get(1));
                graph.get(dislike.get(1)).add(dislike.get(0));
            }

            // Create a map to store the colour of each node
            Map<Integer, Integer> colour = new HashMap<>();

            // Traverse all nodes in the graph
            for (int node = 0; node < graph.size(); node++) {

                // If a node is not coloured, start coloring its
                // connected component recursively starting with colour 1
                if (!colour.containsKey(node)) {

                    // If the colouring fails, return false
                    // (i.e., if a neighbour has the same colour)
                    if (!colourGraph(graph, node, colour, 1)) {
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
        System.out.println(sol.dislikePairs(4, List.of(List.of(1,3),List.of(0,2),List.of(1,3),List.of(0,2))));  // true
        System.out.println(sol.dislikePairs(3, List.of(List.of(0,1),List.of(1,2),List.of(2,0))));               // false

        // Edge cases
        System.out.println(sol.dislikePairs(0, new ArrayList<>()));          // false
        System.out.println(sol.dislikePairs(1, new ArrayList<>()));          // true
        System.out.println(sol.dislikePairs(2, List.of(List.of(0,1))));      // true
        System.out.println(sol.dislikePairs(4, new ArrayList<>()));          // true
        System.out.println(sol.dislikePairs(3, List.of(List.of(0,1),List.of(0,2),List.of(1,2))));  // false
    }
}
```

</details>
