---
title: "Pattern: 2D Grid"
summary: "DP where the table IS the grid — each cell aggregates from its already-filled neighbours (usually top and left). Count paths, cheapest path, largest square. The boundary row/column are the base cases, and the aggregator picks the problem."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
---

## Why It Exists

A whole class of problems lives on a literal grid: count the routes through a maze, find the cheapest path of costs, locate the largest solid square of 1s, score a dungeon crawl. What unites them is that the **DP table *is* the grid** — `dp[i][j]` is the answer *at* cell `(i, j)`, computed from neighbours you've already filled (typically the cell above and the cell to the left).

That's different from the [edit-distance pattern](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-pattern-edit-distance), whose grid indexes two *sequences*. Here the grid is *spatial* — the indices are physical row/column positions. The recognition cue is unmistakable: you're given a 2-D matrix and asked to optimise or count something about moving through it. Reach for `dp[i][j]` over the same shape, decide which neighbours feed each cell, and pick the aggregator.

## See It Work

**Unique Paths** ([LeetCode 62](https://leetcode.com/problems/unique-paths/)) — how many routes from the top-left to the bottom-right of an `m × n` grid, moving only right or down? Each cell is reached from the cell above plus the cell to the left, so `dp[i][j] = dp[i-1][j] + dp[i][j-1]`. The top row and left column have exactly one route each.

```python run viz=grid
def unique_paths(m, n):
    dp = [[1] * n for _ in range(m)]                 # first row & column: one way along the edge
    for i in range(1, m):
        for j in range(1, n):
            dp[i][j] = dp[i - 1][j] + dp[i][j - 1]   # arrive from TOP + from LEFT
    return dp[m - 1][n - 1]

print(unique_paths(3, 7))   # 28
print(unique_paths(3, 3))   # 6
```

```java run viz=grid
public class Main {
    static int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];
        for (int i = 0; i < m; i++) dp[i][0] = 1;    // left column
        for (int j = 0; j < n; j++) dp[0][j] = 1;    // top row
        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++)
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
        return dp[m - 1][n - 1];
    }
    public static void main(String[] args) {
        System.out.println(uniquePaths(3, 7));   // 28
        System.out.println(uniquePaths(3, 3));   // 6
    }
}
```

Both print `28` then `6`. A `3×7` grid has 28 monotone routes; a `3×3` has 6. Cost `O(m·n)` time, reducible to one row of space.

## How It Works

The grid pattern is: lay a DP cell over every grid cell, feed each from its already-computed neighbours, and seed the boundary:

```d2
direction: down
top: "dp[i-1][j]  (cell ABOVE)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
left: "dp[i][j-1]  (cell to the LEFT)" {style.fill: "#fbcfe8"; style.stroke: "#db2777"}
cell: "dp[i][j]  = grid[i][j] combined with its neighbours\nvia the AGGREGATOR" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
agg: "aggregator picks the problem:\nSUM -> count routes (unique paths)\nMIN -> cheapest route (min path sum)\nmin(top,left,diag)+1 -> largest all-1s square" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
base: "BOUNDARY: row 0 / column 0\nhave ONE inbound direction -> seed them" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
top -> cell
left -> cell
cell -> agg
cell -> base
```

<p align="center"><strong>The table mirrors the grid. Each interior cell combines its filled neighbours (top, left, sometimes the diagonal) with the aggregator the problem dictates; the boundary row and column are base cases because they have only one inbound direction.</strong></p>

Three things define the pattern:

- **The table is the grid, the aggregator is the problem.** `dp[i][j]` is the answer *at* `(i, j)`. **Sum** of (top, left) counts routes; **`grid[i][j] + min`** of (top, left) is the cheapest path; **`min(top, left, diagonal) + 1`** is the largest square ending at `(i, j)`. Same traversal, different combine.
- **The boundary row and column are the base cases.** Row 0 has no cell above; column 0 has no cell to the left — each has exactly one inbound direction, so they're seeded directly (all-1 for path *counts*, cumulative for path *sums*). Mishandle them and the whole interior is poisoned.
- **Fill order follows the dependency.** When each cell reads top and left, a plain row-major double loop has them ready. Some grid problems read all four neighbours (e.g. longest *increasing* path) and need memoised DFS or topological order instead — but the "DP cell per grid cell" idea is the same.

> **Key takeaway.** The 2-D-grid pattern lays a DP cell over every grid cell: `dp[i][j]` combines its filled neighbours (usually top `dp[i-1][j]` and left `dp[i][j-1]`) via the problem's aggregator — **sum** to count paths, **`+min`** for cheapest path, **`min(3 neighbours)+1`** for largest square. The boundary row/column are the base cases; fill row-major; `O(m·n)`.

## Trace It

The aggregator choice isn't cosmetic — on the "largest all-1s square" problem it's the whole correctness argument. A square of side `s` ending at `(i, j)` requires three squares of side `s-1` to its top, left, and top-left to *all* be full. So `dp[i][j] = min(top, left, diagonal) + 1`. The `min` is a **veto**: any short neighbour caps the square.

**Predict before you run:** on this grid (a `3×3` of 1s with a single `0` in the centre), the largest all-1s square is just `1×1` — every `2×2` swallows the hole. If you write `max(top, left, diagonal) + 1` instead of `min`, what square side does the DP claim?

```python run viz=grid
def maximal_square(grid, use_max=False):
    m, n = len(grid), len(grid[0])
    dp = [[0] * n for _ in range(m)]
    best = 0
    for i in range(m):
        for j in range(n):
            if grid[i][j] == 1:
                if i == 0 or j == 0:
                    dp[i][j] = 1
                else:
                    nbrs = (dp[i-1][j], dp[i][j-1], dp[i-1][j-1])   # top, left, diagonal
                    dp[i][j] = 1 + (max(nbrs) if use_max else min(nbrs))
                best = max(best, dp[i][j])
    return best                                       # side length of the largest all-1s square

hole = [[1, 1, 1],
        [1, 0, 1],
        [1, 1, 1]]
print("correct (min):", maximal_square(hole))
print("buggy   (max):", maximal_square(hole, use_max=True))
```

<details>
<summary><strong>Reveal</strong></summary>

Correct (`min`) gives side `1`; the buggy `max` claims side `3`. The true answer is `1`: the central `0` sits inside every possible `2×2` (let alone `3×3`), so no square bigger than a single cell is all-1s. The `min` formulation enforces this — when the DP reaches the bottom-right corner, one of its three neighbours traces back through the hole and is small, vetoing any large square. The `max` version does the opposite: it cascades *around* the hole, picking whichever neighbour is biggest and ignoring the one the hole shrank, so it happily reports a `3×3` square that doesn't exist. A square needs *all* of its sub-squares full, which is exactly what `min` (the weakest link) captures and `max` (the strongest link) destroys. The aggregator isn't a detail — on this problem it's the definition of "square."

</details>

## Your Turn

**Minimum Path Sum** ([LeetCode 64](https://leetcode.com/problems/minimum-path-sum/)) — find the cheapest top-left-to-bottom-right path through a grid of costs, moving only right or down. Same traversal as unique paths, but the aggregator is `grid[i][j] + min(top, left)`, and the boundary is *cumulative* (only one way to reach an edge cell).

```python run viz=grid
def min_path_sum(grid):
    m, n = len(grid), len(grid[0])
    dp = [[0] * n for _ in range(m)]
    dp[0][0] = grid[0][0]
    for j in range(1, n):
        dp[0][j] = dp[0][j - 1] + grid[0][j]         # top row: reachable only from the left
    for i in range(1, m):
        dp[i][0] = dp[i - 1][0] + grid[i][0]         # left col: reachable only from above
    for i in range(1, m):
        for j in range(1, n):
            dp[i][j] = grid[i][j] + min(dp[i - 1][j], dp[i][j - 1])
    return dp[m - 1][n - 1]

print(min_path_sum([[1, 3, 1], [1, 5, 1], [4, 2, 1]]))   # 7
print(min_path_sum([[1, 2, 3], [4, 5, 6]]))              # 12
```

```java run viz=grid
public class Main {
    static int minPathSum(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] dp = new int[m][n];
        dp[0][0] = grid[0][0];
        for (int j = 1; j < n; j++) dp[0][j] = dp[0][j - 1] + grid[0][j];
        for (int i = 1; i < m; i++) dp[i][0] = dp[i - 1][0] + grid[i][0];
        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++)
                dp[i][j] = grid[i][j] + Math.min(dp[i - 1][j], dp[i][j - 1]);
        return dp[m - 1][n - 1];
    }
    public static void main(String[] args) {
        System.out.println(minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}}));   // 7
        System.out.println(minPathSum(new int[][]{{1, 2, 3}, {4, 5, 6}}));              // 12
    }
}
```

Both print `7` then `12`. The cheapest route through the first grid is `1→3→1→1→1 = 7`; the second is `1→2→3→6 = 12`. Notice it's *byte-for-byte* the unique-paths structure with `+` swapped for `grid[i][j] + min` and a cumulative boundary — the clearest sign you've internalised the pattern, not the problem.

## Reflect & Connect

- **The table is the grid.** `dp[i][j]` answers the question *at* cell `(i, j)`, fed by already-filled neighbours. This is the spatial cousin of the [edit-distance pattern](/cortex/data-structures-and-algorithms/algorithms-by-strategy-dynamic-programming-pattern-edit-distance) (which grids two *sequences*).
- **Aggregator = problem.** Sum → count paths (unique paths); `grid + min` → cheapest path (min path sum); `min(top, left, diag) + 1` → largest square. One traversal, swappable combine.
- **Boundary row/column are the base cases.** They have a single inbound direction; seed them (all-1 for counts, cumulative for sums) or the interior is wrong. The most common grid-DP bug.
- **Direction sets the fill order.** Top-and-left → row-major. All-four-neighbours (longest increasing path, [329](https://leetcode.com/problems/longest-increasing-path-in-a-matrix/)) → memoised DFS. Down-and-right-from-the-end (dungeon game, [174](https://leetcode.com/problems/dungeon-game/)) → reverse fill.
- **The family.** Unique paths ([62](https://leetcode.com/problems/unique-paths/)), min path sum ([64](https://leetcode.com/problems/minimum-path-sum/)), maximal square ([221](https://leetcode.com/problems/maximal-square/)), dungeon game ([174](https://leetcode.com/problems/dungeon-game/)), longest increasing path ([329](https://leetcode.com/problems/longest-increasing-path-in-a-matrix/)). The [problems here](02-problems) drill the recognition; most space-optimise to one or two rows.

## Recall

<details>
<summary><strong>Q:</strong> What characterises the 2-D-grid DP pattern?</summary>

**A:** The DP table mirrors the grid — `dp[i][j]` is the answer at cell `(i, j)`, computed from already-filled neighbours (usually top `dp[i-1][j]` and left `dp[i][j-1]`). It indexes *spatial* positions, unlike the edit-distance pattern's two sequences.

</details>
<details>
<summary><strong>Q:</strong> How does the aggregator change the problem?</summary>

**A:** Sum of (top, left) counts paths (unique paths); `grid[i][j] + min(top, left)` is the cheapest path (min path sum); `min(top, left, diagonal) + 1` is the largest all-1s square. Same traversal, different combine.

</details>
<details>
<summary><strong>Q:</strong> Why are the first row and column special?</summary>

**A:** They have only one inbound direction (row 0 has nothing above, column 0 has nothing to the left), so they're base cases — seeded directly (all-1 for counts, cumulative for sums). Getting them wrong corrupts the entire interior.

</details>
<details>
<summary><strong>Q:</strong> In maximal square, why is the aggregator <code>min</code> of three neighbours, not <code>max</code>?</summary>

**A:** A square of side `s` at `(i, j)` needs the squares to its top, left, and top-left to *all* have side `s-1`. `min` enforces "weakest link" — any short neighbour vetoes a big square. `max` would ignore a hole and over-report (e.g. claim a `3×3` where a centre `0` actually limits it to `1×1`).

</details>
<details>
<summary><strong>Q:</strong> When does a grid DP need memoised DFS instead of a row-major loop?</summary>

**A:** When a cell depends on neighbours in *all four* directions (e.g. longest increasing path), there's no fixed fill order that has them ready — use memoised DFS (or topological order). Top-and-left dependencies allow a simple row-major fill.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., Ch. 15 — optimal substructure and the grid-shaped DP tables this pattern generalises.
- **LeetCode** 62 (Unique Paths), 64 (Minimum Path Sum), 221 (Maximal Square), 329 (Longest Increasing Path), 174 (Dungeon Game) are the canonical drills; the `28`/`6` path counts, the correct-vs-buggy `1`/`3` maximal square, and the `7`/`12` path sums above all come from the runnable blocks — re-run to verify.
