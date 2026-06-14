---
title: "Cheapest Flights with K Stops"
summary: "Given a list of one-way flights (from, to, cost), source city, destination, and K (max stops allowed), find the minimum-cost flight path. Return -1 if impossible."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
kind: problem
topics: [shortest-path-dijkstra, graph]
---

# Problem: Cheapest Flights With K Stops

## Problem Statement

You are given a flight network as a **weighted adjacency list** where `flights[u]` is a list of `[destination, cost]` pairs. Given a `source` city, a `destination` city, and `k` (the maximum number of **stops** allowed), return the minimum cost to fly from `source` to `destination` using **at most `k` stops**. Return `-1` if no such path exists.

A "stop" is an intermediate city. A direct flight (`source → destination`) uses 0 stops. A path `source → A → destination` uses 1 stop.

## Examples

**Example 1:**
```
Input:  flights = [[[1,2],[3,1]], [[4,4]], [[4,1]], [[2,2],[4,5]], []]
        source = 0, destination = 4, k = 2
Output: 4
```

Path `0 → 3 → 2 → 4` costs `1 + 2 + 1 = 4` with exactly 2 stops. The alternative `0 → 1 → 4` costs `2 + 4 = 6` with 1 stop.

**Example 2:**
```
Input:  flights = [[[4,2]], [[3,3],[0,4]], [[4,3],[0,1]], [[2,1],[4,4]], [[1,5]]]
        source = 3, destination = 0, k = 2
Output: 2
```

## Constraints

- `0 ≤ number of cities ≤ 10⁴`
- `0 ≤ k ≤ number of cities - 1`
- `1 ≤ edge cost ≤ 10⁴`
- Return `-1` if destination is unreachable within `k` stops

```python run viz=graph viz-root=flights
import ast, heapq

class Solution:
    def cheapest_flights(self, flights, source, destination, k):
        # Your code goes here — augment Dijkstra state with flights taken.
        # A 2D minCost[city][flights_used] tracks the best cost per (city, depth).
        # Only expand when flights_used < k+1 (k stops = k+1 flights).
        return -1

flights = ast.literal_eval(input())
src = int(input())
dst = int(input())
k = int(input())
print(Solution().cheapest_flights(flights, src, dst, k))
```

```java run viz=graph viz-root=flights
import java.util.*;

public class Main {
    static class Solution {
        public int cheapestFlights(int[][][] flights, int source, int destination, int k) {
            // Your code goes here — augment Dijkstra state with flights taken.
            // A 2D minCost[city][flights_used] tracks the best cost per (city, depth).
            // Only expand when flights_used < k+1 (k stops = k+1 flights).
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][][] flights = parseWeightedAdj(sc.nextLine());
        int src = Integer.parseInt(sc.nextLine().trim());
        int dst = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().cheapestFlights(flights, src, dst, k));
    }

    // "[[[1, 4], [2, 1]], [[3, 1]], []]" → graph[u] = list of {neighbour, weight}
    static int[][][] parseWeightedAdj(String line) {
        List<int[][]> g = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n && line.charAt(i) != '[') i++;
        i++;                                             // consume outer '['
        while (i < n) {
            while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
            if (i >= n || line.charAt(i) == ']') break;  // end of outer list
            i++;                                         // consume a node group's '['
            List<int[]> node = new ArrayList<>();
            while (i < n) {
                while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                if (line.charAt(i) == ']') { i++; break; }   // end of this node group
                i++;                                     // consume a pair's '['
                int[] pair = new int[2]; int k2 = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k2++] = Integer.parseInt(line.substring(start, i));
                }
                i++;                                     // consume the pair's ']'
                node.add(pair);
            }
            g.add(node.toArray(new int[0][]));
        }
        return g.toArray(new int[0][][]);
    }
}
```

```testcases
{
  "args": [
    { "id": "flights", "label": "flights", "type": "int[][]", "placeholder": "[[[1,2],[3,1]],[[4,4]],[[4,1]],[[2,2],[4,5]],[]]" },
    { "id": "src", "label": "src", "type": "int", "placeholder": "0" },
    { "id": "dst", "label": "dst", "type": "int", "placeholder": "4" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "flights": "[[[1,2],[3,1]],[[4,4]],[[4,1]],[[2,2],[4,5]],[]]", "src": "0", "dst": "4", "k": "2" }, "expected": "4" },
    { "args": { "flights": "[[[4,2]],[[3,3],[0,4]],[[4,3],[0,1]],[[2,1],[4,4]],[[1,5]]]", "src": "3", "dst": "0", "k": "2" }, "expected": "2" },
    { "args": { "flights": "[[], []]", "src": "0", "dst": "0", "k": "0" }, "expected": "0" },
    { "args": { "flights": "[[[1,5]],[]]", "src": "0", "dst": "1", "k": "0" }, "expected": "5" },
    { "args": { "flights": "[[[1,5]],[[2,3]],[]]", "src": "0", "dst": "2", "k": "0" }, "expected": "-1" },
    { "args": { "flights": "[[[1,5]],[[2,3]],[]]", "src": "0", "dst": "2", "k": "1" }, "expected": "8" }
  ]
}
```

<details>
<summary>Editorial</summary>

Standard Dijkstra settles a city on its *first pop*, but that pop might use too many flights. The fix is to track `(cost, city, flights_taken)` as state and use a **2D distance table** `minCost[city][flights_taken]`. Only expand a state when `flights_taken < k+1`; return immediately when the popped city is the destination. The stale-entry skip checks `cost > minCost[city][flights]` rather than just `cost > minCost[city]`.

Key identity: `k` stops = at most `k+1` flights (edges). A direct flight counts as 1 flight, 0 stops.

```python solution time=O(N·(K+2)·log(N·(K+2))) space=O(N·(K+2))
import ast, heapq

class Stop:
    def __init__(self, city, cost, flights):
        self.city = city; self.cost = cost; self.flights = flights
    def __lt__(self, other): return self.cost < other.cost

class Solution:
    def cheapest_flights(self, flights, source, destination, k):
        nodes = len(flights)
        if nodes == 0:
            return -1
        min_cost = [[float("inf")] * (k + 2) for _ in range(nodes)]
        pq = []
        min_cost[source][0] = 0
        heapq.heappush(pq, Stop(source, 0, 0))
        while pq:
            curr_stop = heapq.heappop(pq)
            curr_city = curr_stop.city
            curr_cost = curr_stop.cost
            curr_flights = curr_stop.flights
            if curr_city == destination and curr_flights <= k + 1:
                return curr_cost
            if curr_cost > min_cost[curr_city][curr_flights]:
                continue
            if curr_flights < k + 1:
                for flight in flights[curr_city]:
                    new_city, new_cost_flight = flight
                    new_cost = curr_cost + new_cost_flight
                    if new_cost < min_cost[new_city][curr_flights + 1]:
                        min_cost[new_city][curr_flights + 1] = new_cost
                        heapq.heappush(pq, Stop(new_city, new_cost, curr_flights + 1))
        return -1

flights = ast.literal_eval(input())
src = int(input())
dst = int(input())
k = int(input())
print(Solution().cheapest_flights(flights, src, dst, k))
```

```java solution
import java.util.*;

public class Main {
    static class Stop {
        int city, cost, flights;
        Stop(int city, int cost, int flights) { this.city = city; this.cost = cost; this.flights = flights; }
    }

    static class Solution {
        public int cheapestFlights(int[][][] flights, int source, int destination, int k) {
            int nodes = flights.length;
            if (nodes == 0) return -1;
            int[][] minCost = new int[nodes][k + 2];
            for (int[] row : minCost) Arrays.fill(row, Integer.MAX_VALUE);
            PriorityQueue<Stop> pq = new PriorityQueue<>((a, b) -> a.cost - b.cost);
            minCost[source][0] = 0;
            pq.add(new Stop(source, 0, 0));
            while (!pq.isEmpty()) {
                Stop cur = pq.poll();
                if (cur.city == destination && cur.flights <= k + 1) return cur.cost;
                if (cur.cost > minCost[cur.city][cur.flights]) continue;
                if (cur.flights < k + 1) {
                    for (int[] flight : flights[cur.city]) {
                        int newCity = flight[0], newCost = cur.cost + flight[1];
                        if (newCost < minCost[newCity][cur.flights + 1]) {
                            minCost[newCity][cur.flights + 1] = newCost;
                            pq.add(new Stop(newCity, newCost, cur.flights + 1));
                        }
                    }
                }
            }
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][][] flights = parseWeightedAdj(sc.nextLine());
        int src = Integer.parseInt(sc.nextLine().trim());
        int dst = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().cheapestFlights(flights, src, dst, k));
    }

    // "[[[1, 4], [2, 1]], [[3, 1]], []]" → graph[u] = list of {neighbour, weight}
    static int[][][] parseWeightedAdj(String line) {
        List<int[][]> g = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n && line.charAt(i) != '[') i++;
        i++;                                             // consume outer '['
        while (i < n) {
            while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
            if (i >= n || line.charAt(i) == ']') break;  // end of outer list
            i++;                                         // consume a node group's '['
            List<int[]> node = new ArrayList<>();
            while (i < n) {
                while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                if (line.charAt(i) == ']') { i++; break; }   // end of this node group
                i++;                                     // consume a pair's '['
                int[] pair = new int[2]; int k2 = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k2++] = Integer.parseInt(line.substring(start, i));
                }
                i++;                                     // consume the pair's ']'
                node.add(pair);
            }
            g.add(node.toArray(new int[0][]));
        }
        return g.toArray(new int[0][][]);
    }
}
```

</details>
