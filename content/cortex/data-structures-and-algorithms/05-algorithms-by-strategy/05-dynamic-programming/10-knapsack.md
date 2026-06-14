---
title: "The Knapsack Family"
summary: "Maximize value under a capacity limit by an include-or-exclude decision per item. The backtracking pick/skip tree memoized into a 2D DP — dp[i][c] = max(skip, value + dp[i-1][c-weight]). Space-opts to 1D, where the capacity loop's DIRECTION decides 0/1 (descending) vs unbounded (ascending)."
prereqs:
  - 05-algorithms-by-strategy/04-backtracking/05-pattern-backtracking-search/01-pattern
  - 05-algorithms-by-strategy/05-dynamic-programming/01-linear-dp
---

## Why It Exists

You have a knapsack that holds `W` kilos and a pile of items, each with a weight and a value. Take a subset that maximises value without exceeding the weight limit — and in the **0/1** version, each item is take-it-or-leave-it (one copy, no fractions). That's resource allocation in disguise: cargo loading, ad-budget selection, cutting stock, choosing which features fit a sprint.

The honest brute force is the [backtracking](/cortex/data-structures-and-algorithms/algorithms-by-strategy-backtracking-pattern-backtracking-search) include/exclude tree: for each item, branch on *take* or *skip* — `2ⁿ` leaves. But those branches collapse: "best value from items `i…n` with capacity `c`" depends only on `(i, c)`, and the same `(i, c)` recurs across countless branches. Memoise that and the exponential tree becomes an `O(n·W)` table. Knapsack is *the* canonical bridge from backtracking enumeration to dynamic programming.

## See It Work

`dp[i][c]` = the best value achievable using the first `i` items within capacity `c`. For each item, take the better of **excluding** it (`dp[i-1][c]`) or **including** it (its value plus the best for the remaining capacity, `dp[i-1][c - weight]`) when it fits.

```python run viz=grid
import ast

def knapsack(weights, values, W):
    n = len(weights)
    dp = [[0] * (W + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        for c in range(W + 1):
            dp[i][c] = dp[i - 1][c]                                  # exclude item i
            if weights[i - 1] <= c:                                 # include it, if it fits
                dp[i][c] = max(dp[i][c], values[i - 1] + dp[i - 1][c - weights[i - 1]])
    return dp[n][W]

weights = ast.literal_eval(input())
values = ast.literal_eval(input())
W = int(input())
print(knapsack(weights, values, W))
```

```java run viz=grid
import java.util.*;

public class Main {
    static int knapsack(int[] w, int[] v, int W) {
        int n = w.length;
        int[][] dp = new int[n + 1][W + 1];
        for (int i = 1; i <= n; i++)
            for (int c = 0; c <= W; c++) {
                dp[i][c] = dp[i - 1][c];                                       // exclude
                if (w[i - 1] <= c)
                    dp[i][c] = Math.max(dp[i][c], v[i - 1] + dp[i - 1][c - w[i - 1]]);   // include
            }
        return dp[n][W];
    }
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] weights = parseIntArray(sc.nextLine());
        int[] values = parseIntArray(sc.nextLine());
        int W = Integer.parseInt(sc.nextLine().trim());
        System.out.println(knapsack(weights, values, W));
    }
}
```

```testcases
{
  "args": [
    { "id": "weights", "label": "weights", "type": "int[]", "placeholder": "[1, 3, 4, 5]" },
    { "id": "values", "label": "values", "type": "int[]", "placeholder": "[1, 4, 5, 7]" },
    { "id": "W", "label": "W", "type": "int", "placeholder": "7" }
  ],
  "cases": [
    { "args": { "weights": "[1, 3, 4, 5]", "values": "[1, 4, 5, 7]", "W": "7" }, "expected": "9" },
    { "args": { "weights": "[2, 3, 4]", "values": "[3, 4, 5]", "W": "5" }, "expected": "7" },
    { "args": { "weights": "[1]", "values": "[10]", "W": "1" }, "expected": "10" },
    { "args": { "weights": "[5]", "values": "[10]", "W": "3" }, "expected": "0" }
  ]
}
```

Both print `9`: the weight-3 (value 4) and weight-4 (value 5) items exactly fill the capacity-7 bag for value 9, beating any other combination. Cost `O(n·W)` time and space.

## How It Works

Every cell answers one yes/no question about one item, then defers to a smaller subproblem:

```d2
direction: right
cell: "dp[i][c]\n(best value, first i items, capacity c)" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
exclude: "EXCLUDE item i -> dp[i-1][c]\n(don't take it; capacity unchanged)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
include: "INCLUDE item i (if w[i] <= c)\n-> value[i] + dp[i-1][c - w[i]]\n(take it; pay its weight)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
pick: "dp[i][c] = max(exclude, include)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
cell -> exclude
cell -> include
exclude -> pick
include -> pick
```

<p align="center"><strong>The whole algorithm is one decision repeated: for item <code>i</code>, take the max of skipping it (value carried from <code>dp[i-1][c]</code>) and taking it (its value plus the best use of the leftover capacity <code>dp[i-1][c-w[i]]</code>).</strong></p>

Three things to lock in:

- **The state is 2D because there are two things to track**: which items remain *and* how much capacity is left. The include branch consumes capacity (`c - w[i]`), so capacity must be an axis — unlike the prefix DPs of the last few lessons, one index isn't enough.
- **It space-optimises to 1D**, because row `i` only reads row `i-1`. Keep a single `dp[c]` array and overwrite it per item. But the **iteration direction now matters** (the [Trace It](#trace-it) below): for 0/1 you must sweep capacity **descending**, so each item updates a cell using values that *don't yet* include that same item.
- **It's pseudo-polynomial, not polynomial.** `O(n·W)` looks fast, but `W` is a *number*, encoded in `~log W` bits — so the runtime is exponential in the input *size*. 0/1 knapsack is NP-complete; this DP is only efficient when `W` is small. That's the catch that separates it from the genuinely-polynomial DPs.

> **Key takeaway.** 0/1 knapsack is the memoised include/exclude tree: `dp[i][c] = max(dp[i-1][c], value[i] + dp[i-1][c - weight[i]])`. `O(n·W)` time, pseudo-polynomial (NP-complete in general). Space-opts to a 1D array swept **descending** in capacity; sweeping **ascending** turns it into *unbounded* knapsack.

## Trace It

The 1D space optimisation is where a one-character slip changes the problem. With a single `dp[c]` array, you loop each item over capacities — but in which direction? Descending and ascending both compile, both run, and they answer *different questions*.

**Predict before you run:** you own **one** item of weight 2, value 3, and a bag of capacity 6. The 1D update is `dp[c] = max(dp[c], value + dp[c - weight])`. Swept capacity **descending** it gives the 0/1 answer. What does sweeping **ascending** give — still 3, or something else?

```python run viz=array
def knap_1d(weights, values, W, ascending):
    dp = [0] * (W + 1)
    for i in range(len(weights)):
        rng = range(weights[i], W + 1) if ascending else range(W, weights[i] - 1, -1)
        for c in rng:
            dp[c] = max(dp[c], values[i] + dp[c - weights[i]])
    return dp[W]

print("descending (0/1):", knap_1d([2], [3], 6, ascending=False))   # 3   (one copy)
print("ascending:       ", knap_1d([2], [3], 6, ascending=True))    # 9   (three copies!)
```

<details>
<summary><strong>Reveal</strong></summary>

Descending gives `3`; ascending gives `9`. Sweeping **descending**, when we compute `dp[c]` the cell `dp[c - 2]` still holds the value from *before* this item was considered — so the item contributes at most once. That's 0/1: one copy, value 3.

Sweeping **ascending**, `dp[c - 2]` has *already been updated with this same item* earlier in the pass, so taking it again reads a value that already includes it — at capacity 6 the item gets used three times for value 9. That's the **unbounded** knapsack (unlimited copies of each item), which is a perfectly good algorithm — just not the one you meant. The recurrence is byte-for-byte identical; only the loop direction differs. It's the sharpest "one line *is* the problem definition" case in the DP section: descending forbids reuse, ascending permits it.

</details>

## Your Turn

**Partition Equal Subset Sum** ([LeetCode 416](https://leetcode.com/problems/partition-equal-subset-sum/)) — can the array be split into two subsets with equal sums? That's knapsack with values *equal to* weights and a target of `total/2`: a **boolean** "can we hit exactly this sum?" instead of "maximise value." Same 1D array, same descending sweep for 0/1, `or` instead of `max`.

```python run viz=array
import ast

def can_partition(nums):
    total = sum(nums)
    if total % 2:
        return False                                            # odd total can't split evenly
    target = total // 2
    dp = [False] * (target + 1)
    dp[0] = True                                                # the empty subset sums to 0
    # Your code goes here — DESCENDING capacity sweep for 0/1
    return dp[target]

nums = ast.literal_eval(input())
result = can_partition(nums)
print("true" if result else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static boolean canPartition(int[] nums) {
        // Your code goes here — odd total check, then DESCENDING sweep
        return false;
    }
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] nums = parseIntArray(sc.nextLine());
        System.out.println(canPartition(nums));
    }
}
```

```testcases
{
  "args": [
    { "id": "nums", "label": "nums", "type": "int[]", "placeholder": "[1, 5, 11, 5]" }
  ],
  "cases": [
    { "args": { "nums": "[1, 5, 11, 5]" }, "expected": "true" },
    { "args": { "nums": "[1, 2, 3, 5]" }, "expected": "false" },
    { "args": { "nums": "[1, 1]" }, "expected": "true" },
    { "args": { "nums": "[1, 2, 5]" }, "expected": "false" }
  ]
}
```

<details>
<summary>Editorial</summary>

Knapsack with values equal to weights and a target of `total/2`. Use a boolean 1D array, seed `dp[0] = True`, and sweep capacity **descending** so each item is used at most once.

```python solution time=O(n·W) space=O(W)
import ast

def can_partition(nums):
    total = sum(nums)
    if total % 2:
        return False                                            # odd total can't split evenly
    target = total // 2
    dp = [False] * (target + 1)
    dp[0] = True                                                # the empty subset sums to 0
    for x in nums:
        for c in range(target, x - 1, -1):                      # DESCENDING -> each item once (0/1)
            dp[c] = dp[c] or dp[c - x]
    return dp[target]

nums = ast.literal_eval(input())
result = can_partition(nums)
print("true" if result else "false")
```

```java solution
import java.util.*;

public class Main {
    static boolean canPartition(int[] nums) {
        int total = 0; for (int x : nums) total += x;
        if (total % 2 != 0) return false;
        int target = total / 2;
        boolean[] dp = new boolean[target + 1];
        dp[0] = true;
        for (int x : nums)
            for (int c = target; c >= x; c--)                   // descending -> 0/1
                dp[c] = dp[c] || dp[c - x];
        return dp[target];
    }
    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] nums = parseIntArray(sc.nextLine());
        System.out.println(canPartition(nums));
    }
}
```

</details>

`[1,5,11,5]` splits as `{11}` vs `{1,5,5}` (each sums to 11); `[1,2,3,5]` has odd total 11 and can't split at all. This is **subset-sum**, the boolean heart of the knapsack family — you'll meet it again as its own pattern. Note the descending sweep is doing the same 0/1 work as above: each number lands in at most one subset.

## Reflect & Connect

- **0/1 vs unbounded is a loop direction.** Descending capacity → each item used once (0/1); ascending → unlimited copies (unbounded). Same recurrence, same array. Internalise *why*: descending reads pre-item values, ascending reads post-item values.
- **Subset-sum is boolean knapsack.** Set values = weights, ask "reach exactly `T`?" with `or` instead of `max`. Partition, "[target sum](https://leetcode.com/problems/target-sum/)," and "[coin change](https://leetcode.com/problems/coin-change/)" are all this skeleton — subset-sum gets [its own pattern lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/pattern-subset-sum/pattern) later in this section.
- **Pseudo-polynomial ≠ polynomial.** `O(n·W)` is exponential in the *bit-length* of `W`; 0/1 knapsack is NP-complete. The table is only fast for small `W` — for huge capacities you need approximation (FPTAS) or branch-and-bound.
- **Recover the chosen items by backtracing.** Walk `dp` from `dp[n][W]`: if `dp[i][c] != dp[i-1][c]`, item `i` was taken — subtract its weight and continue. The same parent-pointer move from LCS, edit distance, and palindrome partitioning.
- **Memoisation tamed a `2ⁿ` tree.** This is the backtracking include/exclude search with a cache on `(i, c)`. Seeing that equivalence is the real lesson — most "choose a subset under a constraint" problems are a knapsack wearing a costume, and the [next lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack-applications) cashes that in.

## Recall

<details>
<summary><strong>Q:</strong> What is the 0/1 knapsack recurrence?</summary>

**A:** `dp[i][c] = max(dp[i-1][c], value[i] + dp[i-1][c - weight[i]])` (the include term only when `weight[i] <= c`). `dp[i-1][c]` excludes item `i`; the other term includes it. Answer `dp[n][W]`; cost `O(n·W)`.

</details>
<details>
<summary><strong>Q:</strong> Why is the state 2D when the recent string DPs were 1D?</summary>

**A:** Two quantities vary independently: which items remain *and* remaining capacity. Including an item consumes capacity (`c - w[i]`), so capacity must be its own axis — a single prefix index can't capture it.

</details>
<details>
<summary><strong>Q:</strong> In the 1D space-optimised version, why must the capacity loop run descending for 0/1?</summary>

**A:** Descending means that when you compute `dp[c]`, the cell `dp[c - w]` still holds its value from *before* this item — so the item is used at most once. Ascending reads a `dp[c - w]` already updated with this item, allowing reuse → that's unbounded knapsack.

</details>
<details>
<summary><strong>Q:</strong> Why is `O(n·W)` called pseudo-polynomial?</summary>

**A:** `W` is a number encoded in about `log W` bits, so `O(n·W)` is exponential in the input's bit-length. 0/1 knapsack is NP-complete; the DP is only efficient when `W` is small.

</details>
<details>
<summary><strong>Q:</strong> How does partition-equal-subset-sum reduce to knapsack?</summary>

**A:** It's subset-sum: values = weights, target = `total/2`, boolean "can we hit exactly the target?" using `or` instead of `max`. If the total is odd, it's immediately impossible.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed. — 0/1 knapsack (Ch. 16's fractional vs 0/1 contrast) and NP-completeness (Ch. 34/35), the basis for the pseudo-polynomial claim.
- **Garey & Johnson**, *Computers and Intractability* (1979) — knapsack and subset-sum as canonical NP-complete problems.
- **LeetCode** 416 (Partition Equal Subset Sum), 494 (Target Sum), 518 (Coin Change II, unbounded) are the canonical drills; the `9`, the descending-vs-ascending `3`/`9`, and the `true`/`false` partitions above all come from the runnable blocks — re-run to verify.
