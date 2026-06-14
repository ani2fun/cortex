---
title: "Minimum Travel Time"
summary: "A network of routes with travel times. From source to destination, find the minimum total time, accounting for a 1-unit wait penalty when arriving at any city at an odd time."
prereqs:
  - 17-pattern-shortest-path-dijkstra/01-pattern
difficulty: medium
kind: problem
topics: [shortest-path-dijkstra, graph]
---

# Problem: Minimum Travel Time

## Problem Statement

You are given a city network as a **weighted adjacency list** where `routes[u]` is a list of `[neighbour, travel_time]` pairs. Find the minimum time to travel from `source` to `destination`, subject to the following rule:

> If you **arrive at a city at an odd time**, you must wait **1 extra unit** before you can depart on the next leg. If you arrive at an even time, you may leave immediately.

Return the arrival time at `destination`, or `-1` if it is unreachable.

## Examples

**Example 1:**
```
Input:  routes = [[[1,1],[2,4]],[[2,2],[3,2]],[[3,1]],[]]
        source = 0, destination = 3
Output: 4
```

Path `0 → 1 → 3`: arrive at 1 at time 1 (odd) → wait 1 → depart at time 2 → arrive at 3 at time 4.
Path `0 → 2 → 3`: arrive at 2 at time 4 (even) → depart → arrive at 3 at time 5. Cheaper path wins.

**Example 2:**
```
Input:  routes = [[[1,1],[2,2]],[[3,2]],[[3,1]],[]]
        source = 0, destination = 3
Output: 3
```

Path `0 → 2 → 3`: arrive at 2 at time 2 (even) → no wait → arrive at 3 at time 3.

## Constraints

- `0 ≤ number of cities ≤ 10⁴`
- `1 ≤ travel_time ≤ 10⁴`
- Arrival time at source is `0`
- Return `-1` if destination is unreachable

```python run viz=graph viz-root=routes
import ast, heapq

class Solution:
    def minimum_travel_time(self, routes, source, destination):
        # Your code goes here — standard Dijkstra, but when relaxing an edge
        # add 1 to the departure time if curr_time is odd before adding the
        # edge weight.
        return -1

routes = ast.literal_eval(input())
src = int(input())
dst = int(input())
print(Solution().minimum_travel_time(routes, src, dst))
```

```java run viz=graph viz-root=routes
import java.util.*;

public class Main {
    static class Solution {
        public int minimumTravelTime(int[][][] routes, int source, int destination) {
            // Your code goes here — standard Dijkstra, but when relaxing an edge
            // add 1 to the departure time if curr_time is odd before adding the
            // edge weight.
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][][] routes = parseWeightedAdj(sc.nextLine());
        int src = Integer.parseInt(sc.nextLine().trim());
        int dst = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumTravelTime(routes, src, dst));
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
                int[] pair = new int[2]; int k = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k++] = Integer.parseInt(line.substring(start, i));
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
    { "id": "routes", "label": "routes", "type": "int[][]", "placeholder": "[[[1,1],[2,4]],[[2,2],[3,2]],[[3,1]],[]]" },
    { "id": "src", "label": "src", "type": "int", "placeholder": "0" },
    { "id": "dst", "label": "dst", "type": "int", "placeholder": "3" }
  ],
  "cases": [
    { "args": { "routes": "[[[1,1],[2,4]],[[2,2],[3,2]],[[3,1]],[]]", "src": "0", "dst": "3" }, "expected": "4" },
    { "args": { "routes": "[[[1,1],[2,2]],[[3,2]],[[3,1]],[]]", "src": "0", "dst": "3" }, "expected": "3" },
    { "args": { "routes": "[[], []]", "src": "0", "dst": "0" }, "expected": "0" },
    { "args": { "routes": "[[[1,2]],[]]", "src": "0", "dst": "1" }, "expected": "2" },
    { "args": { "routes": "[[[1,1]],[]]", "src": "0", "dst": "1" }, "expected": "1" },
    { "args": { "routes": "[[], []]", "src": "0", "dst": "1" }, "expected": "-1" }
  ]
}
```

<details>
<summary>Editorial</summary>

This is Dijkstra with a one-line tweak to the relaxation rule. The distance array holds arrival times; the heap is keyed on arrival time. When you're about to leave city `u` at time `t`, compute the wait: `wait = 1 if t % 2 == 1 else 0`. Then the arrival time at neighbour `v` is `t + wait + edge_weight`. Everything else — distance array, lazy stale-skip, early return on destination pop — is standard. The `-1` return for unreachable is shared by both languages so there's no `inf`/`MAX_VALUE` divergence in the output.

```python solution time=O((N + E) log N) space=O(N)
import ast, heapq

class TravelState:
    def __init__(self, city, time):
        self.city = city; self.time = time
    def __lt__(self, other): return self.time < other.time

class Solution:
    def minimum_travel_time(self, routes, source, destination):
        cities = len(routes)
        if cities == 0:
            return -1
        min_arrival_time = [float("inf")] * cities
        pq = []
        min_arrival_time[source] = 0
        heapq.heappush(pq, TravelState(source, 0))
        while pq:
            curr = heapq.heappop(pq)
            curr_city = curr.city
            curr_time = curr.time
            if curr_city == destination:
                return curr_time
            if curr_time > min_arrival_time[curr_city]:
                continue
            for next_city, road_time in routes[curr_city]:
                waiting_time = 1 if curr_time % 2 == 1 else 0
                arrival_time = curr_time + waiting_time + road_time
                if arrival_time < min_arrival_time[next_city]:
                    min_arrival_time[next_city] = arrival_time
                    heapq.heappush(pq, TravelState(next_city, arrival_time))
        return -1

routes = ast.literal_eval(input())
src = int(input())
dst = int(input())
print(Solution().minimum_travel_time(routes, src, dst))
```

```java solution
import java.util.*;

public class Main {
    static class TravelState {
        int city, time;
        TravelState(int city, int time) { this.city = city; this.time = time; }
    }

    static class Solution {
        public int minimumTravelTime(int[][][] routes, int source, int destination) {
            int cities = routes.length;
            if (cities == 0) return -1;
            int[] minArrivalTime = new int[cities];
            Arrays.fill(minArrivalTime, Integer.MAX_VALUE);
            PriorityQueue<TravelState> pq = new PriorityQueue<>((a, b) -> a.time - b.time);
            minArrivalTime[source] = 0;
            pq.add(new TravelState(source, 0));
            while (!pq.isEmpty()) {
                TravelState cur = pq.poll();
                if (cur.city == destination) return cur.time;
                if (cur.time > minArrivalTime[cur.city]) continue;
                for (int[] neighbor : routes[cur.city]) {
                    int nextCity = neighbor[0], travelTime = neighbor[1];
                    int waitingTime = (cur.time % 2 == 1) ? 1 : 0;
                    int arrivalTime = cur.time + waitingTime + travelTime;
                    if (arrivalTime < minArrivalTime[nextCity]) {
                        minArrivalTime[nextCity] = arrivalTime;
                        pq.add(new TravelState(nextCity, arrivalTime));
                    }
                }
            }
            return -1;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][][] routes = parseWeightedAdj(sc.nextLine());
        int src = Integer.parseInt(sc.nextLine().trim());
        int dst = Integer.parseInt(sc.nextLine().trim());
        System.out.println(new Solution().minimumTravelTime(routes, src, dst));
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
                int[] pair = new int[2]; int k = 0;
                while (i < n && line.charAt(i) != ']') {
                    while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == ',')) i++;
                    if (line.charAt(i) == ']') break;
                    int start = i;
                    while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '-')) i++;
                    pair[k++] = Integer.parseInt(line.substring(start, i));
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
