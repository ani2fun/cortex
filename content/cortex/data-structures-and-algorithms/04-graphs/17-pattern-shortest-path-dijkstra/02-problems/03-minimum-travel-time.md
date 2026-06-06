---
title: "Minimum Travel Time"
summary: "A network of routes with travel times. From source to destination, find the minimum *total time*. Twist:"
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
---

# Problem: Minimum Travel Time

## The Problem

A network of routes with travel times. From `source` to `destination`, find the minimum *total time*. Twist:

> If you arrive at a city at an **odd** time, you must wait 1 extra unit before continuing.
> If you arrive at an **even** time, you can continue immediately.

```
Input:  graph = [[[1, 1], [2, 4]], [[2, 2], [3, 2]], [[3, 1]], []]
        source = 0, destination = 3
Output: 4
```

<details>
<summary><h2>Pattern Mapping</h2></summary>


A standard Dijkstra computes arrival times. The twist: when we relax an edge, we don't just add the edge weight — we also **add 1 if our current arrival time is odd**.

This is **Dijkstra with a wait-time tweak** — the relaxation rule becomes:

```
wait = 1 if cur_time is odd else 0
arrival_time_at_neighbour = cur_time + wait + edge_weight
```

That tiny addition handles the parity rule cleanly within the standard pattern.

</details>
<details>
<summary><h2>Solution &amp; Analysis</h2></summary>

### The Solution

```python run viz=graph viz-root=routes
import heapq
from typing import List, Tuple, Dict

# Structure to represent the state of a travel
class TravelState:
    def __init__(self, city: int, time: int):
        self.city = city
        self.time = time

    # Comparator function for the priority queue to create a min-heap
    def __lt__(self, other):
        return self.time < other.time

class Solution:
    def minimum_travel_time(
        self,
        routes: List[List[Tuple[int, int]]],
        source: int,
        destination: int,
    ) -> int:
        cities = len(routes)
        if cities == 0:
            return -1

        # Create a list to store the minimum arrival time at each city
        min_arrival_time = [float("inf")] * cities

        # Create a priority queue (min-heap) to store the stops with
        # their costs
        pq: List[TravelState] = []

        # Assign the minimum arrival time of the starting point with 0
        min_arrival_time[source] = 0

        # Enqueue starting city and the time to reach it
        heapq.heappush(pq, TravelState(source, 0))

        while pq:
            curr_travel_state = heapq.heappop(pq)
            curr_city = curr_travel_state.city
            curr_time = curr_travel_state.time

            # If we reached the destination, return the time
            if curr_city == destination:
                return curr_time

            # If the time is greater than the recorded minimum time, skip
            # processing
            if curr_time > min_arrival_time[curr_city]:
                continue

            for next_city, road_time in routes[curr_city]:

                # If you arrive at an odd time, wait 1 unit for a red
                # light.
                waiting_time = 1 if curr_time % 2 == 1 else 0

                arrival_time = curr_time + waiting_time + road_time
                if arrival_time < min_arrival_time[next_city]:
                    min_arrival_time[next_city] = arrival_time
                    heapq.heappush(
                        pq, TravelState(next_city, arrival_time)
                    )

        # If the destination is unreachable, return -1
        return -1


# Examples from the problem statement
print(Solution().minimum_travel_time([[[1,1],[2,4]],[[2,2],[3,2]],[[3,1]],[]], 0, 3))  # 4
print(Solution().minimum_travel_time([[[1,1],[2,2]],[[3,2]],[[3,1]],[]], 0, 3))        # 3

# Edge cases
print(Solution().minimum_travel_time([], 0, 0))                                         # -1 — empty
print(Solution().minimum_travel_time([[]], 0, 0))                                       # 0 — source is dest
print(Solution().minimum_travel_time([[[1,2]],[]], 0, 1))                               # 2 — single edge even time
print(Solution().minimum_travel_time([[[1,1]],[]], 0, 1))                               # 1 — single edge, arrive at odd; dest reached before wait
print(Solution().minimum_travel_time([[[1,2]],[[]]], 0, 1))                             # -1 — no path to dest
```

```java run viz=graph viz-root=routes
import java.util.*;

public class Main {
    // Structure to represent the state of a stop
    static class TravelState {

        int city;
        int time;

        TravelState(int city, int time) {
            this.city = city;
            this.time = time;
        }
    }

    // Comparator function for the priority queue to create a min-heap
    static class CompareMinHeap implements Comparator<TravelState> {
        public int compare(TravelState a, TravelState b) {

            // Min-heap based on time
            return Integer.compare(a.time, b.time);
        }
    }

    static class Solution {
        public int minimumTravelTime(
            List<List<List<Integer>>> routes,
            int source,
            int destination
        ) {
            int cities = routes.size();
            if (cities == 0) {
                return -1;
            }

            // Create a list to store the minimum arrival time at each city
            int[] minArrivalTime = new int[cities];
            Arrays.fill(minArrivalTime, Integer.MAX_VALUE);

            // Create a priority queue (min-heap) to store the stops with
            // their costs
            PriorityQueue<TravelState> pq = new PriorityQueue<>(
                new CompareMinHeap()
            );

            // Assign the minimum arrival time of the starting point with 0
            minArrivalTime[source] = 0;

            // Enqueue starting city and the time to reach it
            pq.add(new TravelState(source, 0));

            while (!pq.isEmpty()) {
                TravelState currTravelState = pq.poll();
                int currCity = currTravelState.city;
                int currTime = currTravelState.time;

                // If we reached the destination, return the time
                if (currCity == destination) {
                    return currTime;
                }

                // If the time is greater than the recorded minimum time,
                // skip processing
                if (currTime > minArrivalTime[currCity]) {
                    continue;
                }

                for (List<Integer> neighbor : routes.get(currCity)) {
                    int nextCity = neighbor.get(0);
                    int travelTime = neighbor.get(1);

                    // If you arrive at an odd time, wait 1 unit for a red
                    // light.
                    int waitingTime = 0;
                    if (currTime % 2 == 1) {
                        waitingTime = 1;
                    }

                    int arrivalTime = currTime + waitingTime + travelTime;
                    if (arrivalTime < minArrivalTime[nextCity]) {
                        minArrivalTime[nextCity] = arrivalTime;
                        pq.add(new TravelState(nextCity, arrivalTime));
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
        System.out.println(sol.minimumTravelTime(List.of(List.of(List.of(1,1),List.of(2,4)),List.of(List.of(2,2),List.of(3,2)),List.of(List.of(3,1)),new ArrayList<>()), 0, 3));  // 4
        System.out.println(sol.minimumTravelTime(List.of(List.of(List.of(1,1),List.of(2,2)),List.of(List.of(3,2)),List.of(List.of(3,1)),new ArrayList<>()), 0, 3));              // 3

        // Edge cases
        System.out.println(sol.minimumTravelTime(new ArrayList<>(), 0, 0));                   // -1
        System.out.println(sol.minimumTravelTime(List.of(new ArrayList<>()), 0, 0));          // 0
        System.out.println(sol.minimumTravelTime(List.of(List.of(List.of(1,2)),new ArrayList<>()), 0, 1));  // 2
        System.out.println(sol.minimumTravelTime(List.of(List.of(List.of(1,1)),new ArrayList<>()), 0, 1));  // 1
    }
}
```

### Complexity Analysis

| Problem | Time | Space |
|---|---|---|
| Minimum cost path | O((R × C) log(R × C)) | O(R × C) |
| Cheapest flights with K stops | O((N × (K+2)) log(N × (K+2))) | O(N × (K+2)) |
| Minimum travel time | O((N + E) log N) | O(N) |

Adding state dimensions multiplies the search space by the size of the state — so the cheapest-flights problem with K stops is K times slower than vanilla Dijkstra. The trade-off is worth it: a stateful Dijkstra handles a much richer family of constraints than the unstateful one.

</details>
