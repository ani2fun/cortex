---
title: "Pattern: Shortest Path (Dijkstra)"
summary: "Extend BFS with a min-heap keyed on cumulative cost — greedily settle the cheapest unvisited vertex until the target is reached."
prereqs:
  - 04-graphs/08-single-source-shortest-path
---

## Why It Exists

BFS gives the shortest path in *number of hops*. Dijkstra gives the shortest path in *cumulative weight*. The dividing line is simple: does each edge cost the same (1), or a varying real cost?

A grid where every step costs 1? BFS. A grid where each cell charges a different toll? Dijkstra. The pattern appears whenever tolls, travel times, fuel, or stacked reliability scores differ across edges — anywhere you must **minimise an additive cost**, not just count steps. Its one hard requirement: **all edge weights ≥ 0** (negative weights break the greedy argument — that's Bellman-Ford's job, as the [single-source shortest path](/cortex/data-structures-and-algorithms/graphs/single-source-shortest-path) lesson shows).

```d2
direction: right

bfs: "BFS — fewest hops" {
  grid-rows: 1
  grid-columns: 1
  grid-gap: 0
  l: |md
    Every step costs 1.

    First time you reach a node = shortest distance (in hops).

    FIFO queue is enough.
  |
}

dij: "Dijkstra — minimum cumulative weight" {
  grid-rows: 1
  grid-columns: 1
  grid-gap: 0
  l: |md
    Steps have varying cost.

    First time you POP a node from a min-heap = shortest weighted distance.

    Min-heap (priority queue) is required.
  |
}
```

<p align="center"><strong>Same shape — repeatedly settle the "closest" unvisited node — but "closest" means hop-depth in BFS and cumulative weight in Dijkstra.</strong></p>

## See It Work

Single-source shortest weighted distances. Note the cheapest route to node 1 is the *two-hop* `0 → 2 → 1` (cost 3), not the direct edge `0 → 1` (cost 4) — exactly the case BFS would get wrong.

```python run viz=graph viz-kind=graph
import heapq

def dijkstra(adj, n, src):
    dist = [float('inf')] * n
    dist[src] = 0
    heap = [(0, src)]                                   # (cumulative cost, node)
    while heap:
        d, u = heapq.heappop(heap)                      # settle the cheapest open node
        if d > dist[u]:                                 # lazy stale-entry skip
            continue
        for v, w in adj[u]:
            nd = d + w
            if nd < dist[v]:                            # weight-aware relaxation
                dist[v] = nd
                heapq.heappush(heap, (nd, v))
    return dist

adj = {0: [(1, 4), (2, 1)], 1: [(3, 1)], 2: [(1, 2), (3, 5)], 3: []}
print("distances:", dijkstra(adj, 4, 0))                # [0, 3, 1, 4]
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static int[] dijkstra(Map<Integer, int[][]> adj, int n, int src) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> a[0] - b[0]);  // (cost, node)
        heap.add(new int[]{0, src});
        while (!heap.isEmpty()) {
            int[] cur = heap.poll();
            int d = cur[0], u = cur[1];
            if (d > dist[u]) continue;                  // lazy stale-entry skip
            for (int[] e : adj.getOrDefault(u, new int[0][])) {
                int v = e[0], w = e[1], nd = d + w;
                if (nd < dist[v]) {                     // weight-aware relaxation
                    dist[v] = nd;
                    heap.add(new int[]{nd, v});
                }
            }
        }
        return dist;
    }

    public static void main(String[] args) {
        Map<Integer, int[][]> adj = Map.of(
            0, new int[][]{{1, 4}, {2, 1}}, 1, new int[][]{{3, 1}},
            2, new int[][]{{1, 2}, {3, 5}}, 3, new int[][]{});
        System.out.println("distances: " + Arrays.toString(dijkstra(adj, 4, 0)));  // [0, 3, 1, 4]
    }
}
```

Both print `[0, 3, 1, 4]`: node 1 settles at cost 3 (via 2), node 3 at cost 4 (`0 → 2 → 1 → 3`).

## How It Works

Dijkstra is BFS with the FIFO queue swapped for a **min-heap keyed on cumulative cost**. Four elements appear in every instance:

```
dijkstra(graph, source):
    dist = [∞]*N;  dist[source] = 0
    heap = [(0, source)]                  # min-heap of (cost, node)
    while heap:
        (d, u) = heappop(heap)
        if d > dist[u]: continue          # (3) skip stale entries
        for (v, w) in graph[u]:
            if d + w < dist[v]:           # (4) weight-aware relaxation
                dist[v] = d + w
                heappush(heap, (d + w, v))
```

1. **Min-heap keyed by cumulative cost** — the cheapest open node comes out first.
2. **Distance array initialised to ∞** — final values are the answers.
3. **Lazy stale-entry skip** — instead of decreasing a heap key, push a fresh entry and discard outdated pops with `if d > dist[u]: continue`.
4. **Weight-aware relaxation** — `d + w < dist[v]` is the line that does the real work.

The greedy invariant: when a node is *popped*, its distance is final — no cheaper route can exist, because every other open node already costs at least as much and edges are non-negative. (That proof, and the negative-weight counterexample, live in the structural [SSSP lesson](/cortex/data-structures-and-algorithms/graphs/single-source-shortest-path).) Cost: `O((V + E) log V)`. Grids are implicit — neighbours come from a direction array.

> **Key takeaway.** Dijkstra = BFS + a min-heap keyed on *cumulative cost* + weight-aware relaxation. Pop-by-cheapest makes a node's first pop its final distance — but only when every weight is `≥ 0`. Equal weights? It degenerates to BFS. Negative weights? Use Bellman-Ford.

## Trace It

The min-heap's *ordering key* is the whole point. Take a graph with an expensive direct edge `0 → 2` (cost 10) and a cheap two-hop detour `0 → 1 → 2` (cost 1 + 1 = 2).

**Predict before you run:** Dijkstra orders its frontier by cumulative *cost*; BFS orders by *hop count*. What cost-to-target does each report?

```python run viz=graph viz-kind=graph
import heapq
from collections import deque

graph = {0: [(1, 1), (2, 10)], 1: [(2, 1)], 2: []}      # cheap detour vs expensive direct

def by_cost(src, tgt):                                  # Dijkstra: min-heap by cumulative cost
    dist = {src: 0}; heap = [(0, src)]
    while heap:
        d, u = heapq.heappop(heap)
        if d > dist.get(u, float('inf')): continue
        for v, w in graph[u]:
            if d + w < dist.get(v, float('inf')):
                dist[v] = d + w; heapq.heappush(heap, (d + w, v))
    return dist[tgt]

def by_hops(src, tgt):                                  # BFS: FIFO by hop count, settle on first reach
    q = deque([(src, 0)]); seen = {src}
    while q:
        u, c = q.popleft()
        if u == tgt: return c
        for v, w in graph[u]:
            if v not in seen: seen.add(v); q.append((v, c + w))
    return -1

print("by cost (Dijkstra):", by_cost(0, 2))
print("by hops (BFS):     ", by_hops(0, 2))
```

<details>
<summary><strong>Reveal</strong></summary>

Dijkstra reports `2`; BFS reports `10`. BFS reaches node 2 in *one hop* via the direct edge and — ordering only by hop count — settles it immediately at cost 10, never considering the cheaper two-hop route. Dijkstra pops by *cumulative cost*, so it expands `0 → 1` (cost 1) before the cost-10 direct edge, relaxes `1 → 2` down to 2, and only settles node 2 once it's genuinely the cheapest open node. The min-heap isn't an optimisation over BFS — on a weighted graph it's what makes the answer *correct*. (Swap the heap's key from cost to hop count and you've silently rebuilt BFS.)

</details>

## Your Turn

The canonical drill: **Network Delay Time** ([LeetCode 743](https://leetcode.com/problems/network-delay-time/)). A signal starts at node `k`; each directed edge `(u, v, w)` takes `w` time. Return the time for *all* `n` nodes to receive it — the maximum of the shortest distances — or `-1` if some node is unreachable.

```python run viz=graph viz-kind=graph
import heapq

def network_delay_time(times, n, k):
    adj = {i: [] for i in range(1, n + 1)}
    for u, v, w in times: adj[u].append((v, w))
    dist = {}; heap = [(0, k)]
    while heap:
        d, u = heapq.heappop(heap)
        if u in dist: continue                          # settled-set form of the stale skip
        dist[u] = d
        for v, w in adj[u]:
            if v not in dist: heapq.heappush(heap, (d + w, v))
    return max(dist.values()) if len(dist) == n else -1

print(network_delay_time([[2,1,1],[2,3,1],[3,4,1]], 4, 2))   # 2
print(network_delay_time([[1,2,1]], 2, 2))                   # -1
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static int networkDelayTime(int[][] times, int n, int k) {
        Map<Integer, List<int[]>> adj = new HashMap<>();
        for (int i = 1; i <= n; i++) adj.put(i, new ArrayList<>());
        for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]});
        Map<Integer, Integer> dist = new HashMap<>();
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        heap.add(new int[]{0, k});
        while (!heap.isEmpty()) {
            int[] cur = heap.poll();
            int d = cur[0], u = cur[1];
            if (dist.containsKey(u)) continue;          // settled set
            dist.put(u, d);
            for (int[] e : adj.get(u))
                if (!dist.containsKey(e[0])) heap.add(new int[]{d + e[1], e[0]});
        }
        if (dist.size() != n) return -1;
        int ans = 0;
        for (int v : dist.values()) ans = Math.max(ans, v);
        return ans;
    }

    public static void main(String[] args) {
        System.out.println(networkDelayTime(new int[][]{{2,1,1},{2,3,1},{3,4,1}}, 4, 2)); // 2
        System.out.println(networkDelayTime(new int[][]{{1,2,1}}, 2, 2));                 // -1
    }
}
```

Both print `2` then `-1`: from node 2 the farthest node is 2 time units away, and a node with no inbound path is unreachable. The four problems in this section's **Problems** folder push further — minimum-cost paths, cheapest flights *with a stop limit* (which needs an extra `(cost, node, stops)` state), and weighted-grid routing.

## Reflect & Connect

- **Dijkstra is weighted BFS.** With all weights equal, the min-heap reduces to a FIFO queue and Dijkstra *is* BFS (just with a `log` factor of heap overhead). Reach for [BFS](/cortex/data-structures-and-algorithms/graphs-pattern-shortest-path-breadth-first-search) when every edge costs the same — it's simpler and faster.
- **Negative weights break it.** The greedy "first pop is final" invariant assumes no later edge can lower a settled distance — false with negatives. Use Bellman-Ford (`O(VE)`), covered in the SSSP lesson.
- **A\* is Dijkstra + a heuristic.** Add an admissible estimate of remaining distance to the heap key and you steer the search toward the goal — the basis of game and map pathfinding.
- **State can be richer than "node".** "Cheapest flight with ≤ K stops" makes the heap key a `(cost, node, stops)` triple; the pattern survives — you're just running Dijkstra over an expanded state space.

## Recall

<details>
<summary><strong>Q:</strong> BFS vs Dijkstra — when each?</summary>

**A:** BFS for unweighted graphs (or all-equal weights) — shortest path in *hops*. Dijkstra for non-negative weighted graphs — shortest path in *cumulative cost*, via a min-heap.

</details>
<details>
<summary><strong>Q:</strong> What is the heap keyed on, and what does popping a node mean?</summary>

**A:** Keyed on cumulative cost from the source. Popping a node settles it: its distance is final, because every other open node already costs at least as much (given non-negative weights).

</details>
<details>
<summary><strong>Q:</strong> What is the lazy stale-entry skip?</summary>

**A:** Rather than decrease-key, push a fresh `(cost, node)` on each relaxation and discard outdated pops with `if d > dist[u]: continue` (or a settled-set check). Keeps the heap operations simple.

</details>
<details>
<summary><strong>Q:</strong> Why does Dijkstra fail on negative edges?</summary>

**A:** A settled node could later be reached more cheaply through a negative edge, violating the "first pop is final" invariant. Use Bellman-Ford instead.

</details>
<details>
<summary><strong>Q:</strong> Time complexity?</summary>

**A:** `O((V + E) log V)` with a binary-heap priority queue — each edge may push once, each node pops once.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §24.3 — Dijkstra's algorithm, the greedy correctness proof, and the non-negative-weight requirement.
- **Sedgewick & Wayne**, *Algorithms*, 4th ed., §4.4 — `DijkstraSP`, the lazy vs eager (index priority queue) implementations, and shortest-path properties.
- **Skiena**, *The Algorithm Design Manual*, 3rd ed., §8.3.1 — Dijkstra, its relationship to Prim's MST, and when to choose it over BFS/Bellman-Ford.
- **LeetCode 743** "Network Delay Time" and **1631** "Path With Minimum Effort" are the canonical drills. The `[0,3,1,4]`, cost-vs-hops `2`/`10`, and `2`/`-1` outputs above come from the runnable blocks — re-run to verify.
