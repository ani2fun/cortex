---
title: "Cheapest Flights with K Stops"
summary: "Given a list of one-way flights (from, to, cost), source city, destination, and K (max stops allowed), find the minimum-cost flight path. Return -1 if impossible."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
---

# Problem: Cheapest Flights With K Stops

## The Problem

Given a list of one-way flights (`from`, `to`, `cost`), source city, destination, and `K` (max stops allowed), find the minimum-cost flight path. Return -1 if impossible.

```
Input:  flights = [[[1,2],[3,1]], [[4,4]], [[4,1]], [[2,2],[4,5]], []]
        source = 0, destination = 4, K = 2
Output: 4 (path 0 → 3 → 2 → 4 with 2 stops)
```

<details>
<summary><h2>Pattern Mapping — Dijkstra With State</h2></summary>


The crucial twist: you can't use the standard Dijkstra distance array, because two paths to the same city might have different stop counts — and the "fewer stops" path might still be useful even if it costs more.

**Solution: 2D distance array.** Track `min_cost[city][flights_taken]` instead of `min_cost[city]`. Every queue entry carries `(cost, city, flights_used)` — flights = the number of edges taken so far.

This is the **stateful Dijkstra** variant — the same pattern, but you augment the node with a state dimension that affects which transitions are valid.

> *Before reading on — why doesn't the standard 1D Dijkstra work here? What can go wrong?*

The standard Dijkstra finalises a city's distance the first time it's popped. But here, "first popped" might mean "popped after using K+1 flights" — useless because we've exceeded the budget. A 1D Dijkstra would close off the destination too early. The 2D state (cost + stops) lets us discover *paths within budget* even if they're longer.

</details>
<details>
<summary><h2>The Solution</h2></summary>



```python run viz=graph viz-root=flights
import heapq
from typing import List, Tuple, Dict

# Structure to represent the state of a stop
class Stop:
    def __init__(self, city: int, cost: int, flights: int):
        self.city = city
        self.cost = cost
        self.flights = flights

    # Comparator function for the priority queue to create a min-heap
    def __lt__(self, other):
        return self.cost < other.cost

class Solution:
    def cheapest_flights(
        self,
        flights: List[List[List[int]]],
        source: int,
        destination: int,
        k: int,
    ) -> int:
        nodes = len(flights)
        if nodes == 0:
            return -1

        # 2D minCost: nodes x (k + 2) (flights from 0 to k + 1)
        min_cost = [[float("inf")] * (k + 2) for _ in range(nodes)]

        # Create a priority queue (min-heap) to store the stops with
        # their costs
        pq = []

        # Assign the minimum cost of the starting point, need 0 flights
        # to reach source
        min_cost[source][0] = 0

        # Enqueue starting stop and the cost to move on it
        heapq.heappush(pq, Stop(source, 0, 0))

        while pq:
            curr_stop = heapq.heappop(pq)
            curr_city = curr_stop.city
            curr_cost = curr_stop.cost
            curr_flights = curr_stop.flights

            # If we reached the destination, return the cost. We check for K + 1 as
            # we can have as for K stops between source and destination we need K + 1 flights.
            # For example if K = 1, we can have a path like 0 -> 1 -> 2 which has 1 stop
            # but 2 flights.
            if curr_city == destination and curr_flights <= k + 1:
                return curr_cost

            # If the cost is greater than the recorded minimum cost, skip
            # processing
            if curr_cost > min_cost[curr_city][curr_flights]:
                continue

            # Can only take up to k + 1 flights
            if curr_flights < k + 1:
                for flight in flights[curr_city]:
                    new_city, new_cost_flight = flight
                    new_cost = curr_cost + new_cost_flight
                    if new_cost < min_cost[new_city][curr_flights + 1]:
                        min_cost[new_city][curr_flights + 1] = new_cost
                        heapq.heappush(
                            pq,
                            Stop(new_city, new_cost, curr_flights + 1),
                        )

        # If the destination is unreachable, return -1
        return -1


# Examples from the problem statement
print(Solution().cheapest_flights([[[1,2],[3,1]],[[4,4]],[[4,1]],[[2,2],[4,5]],[]], 0, 4, 2))  # 4
print(Solution().cheapest_flights([[[4,2]],[[3,3],[0,4]],[[4,3],[0,1]],[[2,1],[4,4]],[[1,5]]], 3, 0, 2))  # 2

# Edge cases
print(Solution().cheapest_flights([], 0, 0, 0))                                                 # -1 — empty graph
print(Solution().cheapest_flights([[]], 0, 0, 0))                                               # 0 — source is destination
print(Solution().cheapest_flights([[[1,5]],[]], 0, 1, 0))                                       # -1 — needs 1 stop, k=0
print(Solution().cheapest_flights([[[1,5]],[]], 0, 1, 1))                                       # 5 — direct flight, k=1
print(Solution().cheapest_flights([[[1,10],[2,3]],[[3,2]],[[1,4]],[]], 0, 3, 1))               # 9 — 0->2->1->3 needs 2 stops; within k=1: 0->1->3=12; answer -1 if only k=1
```

```java run viz=graph viz-root=flights
import java.util.*;

public class Main {
    // Structure to represent the state of a stop
    static class Stop {

        int city;
        int cost;
        int flights;

        Stop(int city, int cost, int flights) {
            this.city = city;
            this.cost = cost;
            this.flights = flights;
        }
    }

    // Comparator function for the priority queue to create a min-heap
    static class CompareMinHeap implements Comparator<Stop> {
        public int compare(Stop a, Stop b) {

            // Min-heap based on cost
            return Integer.compare(a.cost, b.cost);
        }
    }

    static class Solution {
        public int cheapestFlights(
            List<List<List<Integer>>> flights,
            int source,
            int destination,
            int K
        ) {
            int nodes = flights.size();
            if (nodes == 0) {
                return -1;
            }

            // 2D minCost: nodes x (K + 2) (flights from 0 to K + 1)
            int[][] minCost = new int[nodes][K + 2];
            for (int i = 0; i < nodes; i++) {
                Arrays.fill(minCost[i], Integer.MAX_VALUE);
            }

            // Create a priority queue (min-heap) to store the stops with
            // their costs
            PriorityQueue<Stop> pq = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Assign the minimum cost of the starting point, need 0 flights
            // to reach source
            minCost[source][0] = 0;

            // Enqueue starting stop and the cost to move on it
            pq.add(new Stop(source, 0, 0));

            while (!pq.isEmpty()) {
                Stop currStop = pq.poll();
                int currCity = currStop.city;
                int currCost = currStop.cost;
                int currFlights = currStop.flights;

                // If we reached the destination, return the cost. We check
                // for K + 1 as we can have as for K stops between source and
                // destination we need K + 1 flights. For example if K = 1,
                // we can have a path like 0 -> 1 -> 2 which has 1 stop but 2
                // flights.
                if (currCity == destination && currFlights <= K + 1) {
                    return currCost;
                }

                // If the cost is greater than the recorded minimum cost,
                // skip processing
                if (currCost > minCost[currCity][currFlights]) {
                    continue;
                }

                if (currFlights < K + 1) {
                    for (List<Integer> flight : flights.get(currCity)) {
                        int newCity = flight.get(0);
                        int newCost = currCost + flight.get(1);
                        if (newCost < minCost[newCity][currFlights + 1]) {
                            minCost[newCity][currFlights + 1] = newCost;
                            pq.add(
                                new Stop(newCity, newCost, currFlights + 1)
                            );
                        }
                    }
                }
            }

            // If the destination is unreachable, return -1
            return -1;
        }
    }

    public static void main(String[] args) {
        Solution sol = new Solution();

        // Examples from the problem statement
        System.out.println(sol.cheapestFlights(List.of(List.of(List.of(1,2),List.of(3,1)),List.of(List.of(4,4)),List.of(List.of(4,1)),List.of(List.of(2,2),List.of(4,5)),new ArrayList<>()), 0, 4, 2));  // 4
        System.out.println(sol.cheapestFlights(List.of(List.of(List.of(4,2)),List.of(List.of(3,3),List.of(0,4)),List.of(List.of(4,3),List.of(0,1)),List.of(List.of(2,1),List.of(4,4)),List.of(List.of(1,5))), 3, 0, 2));  // 2

        // Edge cases
        System.out.println(sol.cheapestFlights(new ArrayList<>(), 0, 0, 0));  // -1
        System.out.println(sol.cheapestFlights(List.of(new ArrayList<>()), 0, 0, 0));  // 0
        System.out.println(sol.cheapestFlights(List.of(List.of(List.of(1,5)),new ArrayList<>()), 0, 1, 0));  // -1
        System.out.println(sol.cheapestFlights(List.of(List.of(List.of(1,5)),new ArrayList<>()), 0, 1, 1));  // 5
    }
}
```

</details>
