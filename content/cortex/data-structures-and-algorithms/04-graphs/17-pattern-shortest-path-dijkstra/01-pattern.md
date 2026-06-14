---
title: "Pattern: Shortest Path (Dijkstra)"
summary: "Extend BFS with a min-heap keyed on cumulative cost — greedily settle the cheapest unvisited vertex until the target is reached."
prereqs:
  - 04-graphs/08-single-source-shortest-path
---

# Pattern: Shortest Path (Dijkstra)

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

Single-source shortest weighted distances. The graph crosses stdin as a **weighted adjacency list** — `graph[u]` is a list of `[neighbour, weight]` pairs. Note the cheapest route to node 1 is the *two-hop* `0 → 2 → 1` (cost 3), not the direct edge `0 → 1` (cost 4) — exactly the case BFS would get wrong. Pick a case and **Run** it.

```python run viz=graph viz-kind=graph
import ast, heapq

def dijkstra(graph, src):
    n = len(graph)
    dist = [float('inf')] * n
    dist[src] = 0
    heap = [(0, src)]                                   # (cumulative cost, node)
    while heap:
        d, u = heapq.heappop(heap)                      # settle the cheapest open node
        if d > dist[u]:                                 # lazy stale-entry skip
            continue
        for v, w in graph[u]:
            nd = d + w
            if nd < dist[v]:                            # weight-aware relaxation
                dist[v] = nd
                heapq.heappush(heap, (nd, v))
    return dist

graph = ast.literal_eval(input())   # weighted adjacency: graph[u] = [[nbr, wt], ...]
src = int(input())
print(dijkstra(graph, src))
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static int[] dijkstra(int[][][] graph, int src) {
        int n = graph.length;
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> a[0] - b[0]);  // (cost, node)
        heap.add(new int[]{0, src});
        while (!heap.isEmpty()) {
            int[] cur = heap.poll();
            int d = cur[0], u = cur[1];
            if (d > dist[u]) continue;                  // lazy stale-entry skip
            for (int[] e : graph[u]) {
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
        Scanner sc = new Scanner(System.in);
        int[][][] graph = parseWeightedAdj(sc.nextLine());
        int src = Integer.parseInt(sc.nextLine().trim());
        System.out.println(Arrays.toString(dijkstra(graph, src)));
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
    { "id": "graph", "label": "graph", "type": "int[][]", "placeholder": "[[[1, 4], [2, 1]], [[3, 1]], [[1, 2], [3, 5]], []]" },
    { "id": "src", "label": "src", "type": "int", "placeholder": "0" }
  ],
  "cases": [
    { "args": { "graph": "[[[1, 4], [2, 1]], [[3, 1]], [[1, 2], [3, 5]], []]", "src": "0" }, "expected": "[0, 3, 1, 4]" },
    { "args": { "graph": "[[[1, 1], [2, 4]], [[2, 2], [3, 2]], [[3, 1]], []]", "src": "0" }, "expected": "[0, 1, 3, 3]" },
    { "args": { "graph": "[[[1, 5]], []]", "src": "0" }, "expected": "[0, 5]" },
    { "args": { "graph": "[[]]", "src": "0" }, "expected": "[0]" }
  ]
}
```

Both print `[0, 3, 1, 4]`: node 1 settles at cost 3 (via 2), node 3 at cost 4 (`0 → 2 → 1 → 3`). The `[neighbour, weight]` pairs reach Java through a small `parseWeightedAdj` helper; Python reads them directly with `ast.literal_eval`. (These test graphs are fully connected from the source — when a node can be unreachable, replace `float('inf')` / `Integer.MAX_VALUE` with a shared sentinel so the two languages print the same thing.)

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

graph = [[[1, 1], [2, 10]], [[2, 1]], []]              # cheap detour vs expensive direct

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

The canonical drill: **Network Delay Time** ([LeetCode 743](https://leetcode.com/problems/network-delay-time/)). A signal starts at node `k`; each directed edge `[u, v, w]` takes `w` time. Return the time for *all* `n` nodes to receive it — the maximum of the shortest distances — or `-1` if some node is unreachable. Here the graph arrives as a flat **edge list** (`times`), so you build the adjacency yourself before running Dijkstra. Write it.

```python run viz=graph viz-kind=graph
import ast, heapq

def network_delay_time(times, n, k):
    # Your code goes here — build adjacency from the edge list, run Dijkstra from k,
    # then return max(distances) if every node was reached, else -1.
    pass

times = ast.literal_eval(input())   # directed weighted edges [u, v, w] (nodes 1..n)
n = int(input())
k = int(input())
print(network_delay_time(times, n, k))
```

```java run viz=graph viz-kind=graph
import java.util.*;

public class Main {
    static int networkDelayTime(int[][] times, int n, int k) {
        // Your code goes here — build adjacency from the edge list, run Dijkstra from k,
        // then return max(distances) if every node was reached, else -1.
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[][] times = parseIntMatrix(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(networkDelayTime(times, n, k));
    }

    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }
}
```

```testcases
{
  "args": [
    { "id": "times", "label": "times", "type": "int[][]", "placeholder": "[[2, 1, 1], [2, 3, 1], [3, 4, 1]]" },
    { "id": "n", "label": "n", "type": "int", "placeholder": "4" },
    { "id": "k", "label": "k", "type": "int", "placeholder": "2" }
  ],
  "cases": [
    { "args": { "times": "[[2, 1, 1], [2, 3, 1], [3, 4, 1]]", "n": "4", "k": "2" }, "expected": "2" },
    { "args": { "times": "[[1, 2, 1]]", "n": "2", "k": "2" }, "expected": "-1" },
    { "args": { "times": "[[1, 2, 1], [2, 3, 2], [1, 3, 4]]", "n": "3", "k": "1" }, "expected": "3" },
    { "args": { "times": "[[1, 2, 1], [2, 3, 7], [1, 3, 4]]", "n": "3", "k": "1" }, "expected": "4" }
  ]
}
```

<details>
<summary>Editorial</summary>

The graph arrives 1-indexed as `times`, so the first job is to turn the edge list into adjacency: a map from each node to its `(neighbour, weight)` pairs. Then it's textbook Dijkstra from `k`, using the *settled-set* form of the stale-entry skip (`if u in dist: continue`) instead of the `d > dist[u]` form — equivalent, and natural when `dist` is a dict you fill on first settle. The answer is the largest settled distance, because the signal arrives everywhere only once the farthest node hears it; if fewer than `n` nodes settle, one is unreachable and the answer is `-1`. The unreachable case returns `-1` in both languages, so there's no `inf`-vs-`MAX_VALUE` divergence to worry about.

```python solution time=O((V + E) log V) space=O(V + E)
import ast, heapq

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

times = ast.literal_eval(input())   # directed weighted edges [u, v, w] (nodes 1..n)
n = int(input())
k = int(input())
print(network_delay_time(times, n, k))
```

```java solution
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
        Scanner sc = new Scanner(System.in);
        int[][] times = parseIntMatrix(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        int k = Integer.parseInt(sc.nextLine().trim());
        System.out.println(networkDelayTime(times, n, k));
    }

    static int[][] parseIntMatrix(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("[]") || trimmed.equals("[[]]")) return new int[0][];
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] rows = inner.split("\\],\\s*\\[");
        int[][] mat = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replaceAll("[\\[\\]\\s]", "");
            if (row.isEmpty()) { mat[r] = new int[0]; continue; }
            String[] parts = row.split(",");
            mat[r] = new int[parts.length];
            for (int c = 0; c < parts.length; c++) mat[r][c] = Integer.parseInt(parts[c].trim());
        }
        return mat;
    }
}
```

</details>

## Reflect & Connect

The four problems in this section's **Problems** folder push further — minimum-cost paths, cheapest flights *with a stop limit* (which needs an extra `(cost, node, stops)` state), and weighted-grid routing.

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
