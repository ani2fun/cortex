---
title: "Pattern: Subset Sum"
summary: "A 1-D DP over a target where dp[j] tells whether sum j is reachable from a subset. The boolean/count specialization of knapsack — values equal weights — solving partition, minimum-difference, and target-sum problems with a descending sweep."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/10-knapsack
---

## Why It Exists

A huge family of problems is secretly one question: *can I pick a subset of these numbers that sums to exactly `T`* (or as close as possible, or in how many ways)? Splitting a list into two equal halves, balancing two teams, "last stone weight," target-sum with `±` signs — all of them. The moment you spot "choose a subset to hit/approach a number," you're in subset-sum territory.

It's the boolean (or counting) specialization of [knapsack](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack): set each item's *value equal to its weight* and ask not "max value under capacity" but "is capacity `j` reachable?" That collapses the 2-D knapsack table to a **1-D** array `dp[j]` over the target, swept **descending** so each number is used at most once. This lesson is the recognition layer — see the shape, reach for the 1-D boolean sweep.

## See It Work

`dp[j]` = "is sum `j` reachable from some subset seen so far?" Seed `dp[0] = True` (the empty subset sums to 0). For each number, sweep targets **descending** and mark `dp[j]` reachable if `dp[j - x]` already was.

```python run viz=array
import ast

def subset_sum(nums, target):
    dp = [False] * (target + 1)
    dp[0] = True                                     # empty subset sums to 0 — the seed
    for x in nums:
        for j in range(target, x - 1, -1):           # DESCENDING -> each number used at most once
            dp[j] = dp[j] or dp[j - x]
    return dp[target]

nums = ast.literal_eval(input())
target = int(input())
print("true" if subset_sum(nums, target) else "false")
```

```java run viz=array
import java.util.*;

public class Main {
    static boolean subsetSum(int[] nums, int target) {
        boolean[] dp = new boolean[target + 1];
        dp[0] = true;                                    // the seed
        for (int x : nums)
            for (int j = target; j >= x; j--)            // descending -> 0/1
                dp[j] = dp[j] || dp[j - x];
        return dp[target];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] nums = parseIntArray(sc.nextLine());
        int target = Integer.parseInt(sc.nextLine().trim());
        System.out.println(subsetSum(nums, target));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "nums", "label": "nums", "type": "int[]", "placeholder": "[3, 34, 4, 12, 5, 2]" },
    { "id": "target", "label": "target", "type": "int", "placeholder": "9" }
  ],
  "cases": [
    { "args": { "nums": "[3, 34, 4, 12, 5, 2]", "target": "9" }, "expected": "true" },
    { "args": { "nums": "[1, 2, 5]", "target": "4" }, "expected": "false" },
    { "args": { "nums": "[1, 2, 3]", "target": "6" }, "expected": "true" },
    { "args": { "nums": "[5, 10, 3]", "target": "7" }, "expected": "false" }
  ]
}
```

Both print `true` then `false`. `4 + 5 = 9` is reachable; no subset of `{1,2,5}` hits 4 (the reachable sums are 0,1,2,3,5,6,7,8). Cost `O(n · target)` — pseudo-polynomial, inherited from knapsack.

## How It Works

The 2-D knapsack table has one row per item; subset sum only needs to know *reachability*, so a single row, overwritten per item, suffices — provided you sweep the right way:

```d2
direction: right
state: "dp[j] = is sum j reachable\nfrom a subset of items seen so far?" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
seed: "dp[0] = True\n(empty subset sums to 0 — the only seed)" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
update: "for each x, for j DESCENDING:\n  dp[j] = dp[j] OR dp[j - x]\n(either j was already reachable,\nor it is via this x)" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
dir: "DESCENDING j -> each x used ONCE (0/1)\nascending j -> each x reusable (unbounded)" {style.fill: "#fde68a"; style.stroke: "#d97706"}
state -> seed
state -> update
update -> dir
```

<p align="center"><strong>One boolean array over the target. <code>dp[0]=True</code> seeds it; <code>dp[j] |= dp[j-x]</code> propagates reachability. The sweep direction decides 0/1 (descending) versus unbounded (ascending) — exactly the knapsack rule.</strong></p>

Three things define the pattern:

- **State is the target, not the item count.** `dp[j]` answers "is `j` reachable?" — a 1-D array of size `target + 1`. The items are consumed by the outer loop, not stored as a dimension. This is the knapsack table with the item axis rolled away.
- **`dp[0] = True` is the seed, and the whole engine.** Reachability propagates from it: `dp[x] = dp[0] or …` is how the first number ever marks anything. Forget it and the array stays all-False forever ([Trace It](#trace-it)).
- **The sweep direction is the 0/1-vs-unbounded switch.** Descending `j` means `dp[j-x]` still reflects subsets *without* the current `x`, so `x` is used at most once — 0/1. Ascending reuses `x` — that's the *unbounded* variant (coin change). Identical line, opposite meaning — the [knapsack loop-direction rule](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack) again.

> **Key takeaway.** Subset sum is knapsack with values = weights, reduced to a **1-D boolean** `dp[j]` over the target: seed `dp[0] = True`, then `dp[j] |= dp[j-x]` for each number, sweeping `j` **descending** (0/1). Swap `or` for `+=` to *count* subsets; sweep ascending for the *unbounded* variant. `O(n · target)`, pseudo-polynomial.

## Trace It

The descending sweep gets all the attention, but the quieter bug is the seed. `dp[0] = True` encodes "the empty subset sums to 0" — and it's the only `True` the whole array starts with. Every other reachable sum is *derived* from it.

**Predict before you run:** you initialize `dp` to all-`False` and forget the `dp[0] = True` seed, but the descending sweep and `dp[j] |= dp[j-x]` update are perfect. Does `subset_sum([3, 4, 5], 9)` still find the `4 + 5` subset?

```python run viz=array
def subset_sum(nums, target):                        # correct: seeded
    dp = [False] * (target + 1)
    dp[0] = True
    for x in nums:
        for j in range(target, x - 1, -1):
            dp[j] = dp[j] or dp[j - x]
    return dp[target]

def subset_sum_buggy(nums, target):                  # bug: no dp[0] = True seed
    dp = [False] * (target + 1)
    for x in nums:
        for j in range(target, x - 1, -1):
            dp[j] = dp[j] or dp[j - x]
    return dp[target]

print("correct:", subset_sum([3, 4, 5], 9))
print("buggy:  ", subset_sum_buggy([3, 4, 5], 9))
```

<details>
<summary><strong>Reveal</strong></summary>

Correct is `True`; the buggy version returns `False` — and it would return `False` for *every* input, because nothing is ever reachable. Trace it: the update `dp[j] = dp[j] or dp[j - x]` can only set `dp[j]` to `True` if some `dp[j - x]` is *already* `True`. With the array all-`False`, the right-hand side is always `False or False`, so nothing ever flips. The single `dp[0] = True` is the spark: processing `x = 3` reads `dp[3-3] = dp[0] = True` and lights up `dp[3]`; then `x = 4` lights `dp[7]` (from `dp[3]`) and `dp[4]` (from `dp[0]`); then `x = 5` reaches `dp[9]` from `dp[4]`. Pull the seed and the chain reaction never starts. The lesson: in a reachability/counting DP, the base case isn't bookkeeping — it's the generator the entire table unfolds from. (Counting versions seed `dp[0] = 1` for the same reason.)

</details>

## Your Turn

**Minimum Subset Sum Difference** (a.k.a. Last Stone Weight II, [LeetCode 1049](https://leetcode.com/problems/last-stone-weight-ii/)) — split the numbers into two groups minimizing `|sum₁ − sum₂|`. The trick: one group's sum determines the other's, so find the *reachable* subset sum closest to `total/2`. Subset sum builds the full reachability set; you read off the best.

```python run viz=array
import ast

def min_subset_diff(nums):
    # Your code goes here
    return 0

nums = ast.literal_eval(input())
print(min_subset_diff(nums))
```

```java run viz=array
import java.util.*;

public class Main {
    static int minSubsetDiff(int[] nums) {
        // Your code goes here
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] nums = parseIntArray(sc.nextLine());
        System.out.println(minSubsetDiff(nums));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

```testcases
{
  "args": [
    { "id": "nums", "label": "nums", "type": "int[]", "placeholder": "[1, 2, 3, 9]" }
  ],
  "cases": [
    { "args": { "nums": "[1, 2, 3, 9]" }, "expected": "3" },
    { "args": { "nums": "[1, 6, 11, 5]" }, "expected": "1" },
    { "args": { "nums": "[1, 2, 3, 4]" }, "expected": "0" },
    { "args": { "nums": "[5]" }, "expected": "5" }
  ]
}
```

<details>
<summary>Editorial</summary>

```python solution time=O(n·total) space=O(total)
import ast

def min_subset_diff(nums):
    total = sum(nums)
    half = total // 2
    dp = [False] * (half + 1)
    dp[0] = True
    for x in nums:
        for j in range(half, x - 1, -1):
            dp[j] = dp[j] or dp[j - x]
    best = max(j for j in range(half + 1) if dp[j])  # reachable sum closest to total/2
    return total - 2 * best                          # the other group is total - best

nums = ast.literal_eval(input())
print(min_subset_diff(nums))
```

```java solution
import java.util.*;

public class Main {
    static int minSubsetDiff(int[] nums) {
        int total = 0; for (int x : nums) total += x;
        int half = total / 2;
        boolean[] dp = new boolean[half + 1];
        dp[0] = true;
        for (int x : nums)
            for (int j = half; j >= x; j--)
                dp[j] = dp[j] || dp[j - x];
        int best = 0;
        for (int j = half; j >= 0; j--) if (dp[j]) { best = j; break; }   // closest to half
        return total - 2 * best;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] nums = parseIntArray(sc.nextLine());
        System.out.println(minSubsetDiff(nums));
    }

    static int[] parseIntArray(String line) {
        String inner = line.replaceAll("[\\[\\]\\s]", "");
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }
}
```

Both print `3` then `1`. The reachability array is the same subset-sum DP; the only new step is scanning it for the sum nearest `total/2`. That move — *build the reachable set, then read the answer off it* — turns a boolean "can we?" into an optimization "how close can we get?", which is why this one pattern covers partition, balancing, and difference-minimization alike.

</details>

## Reflect & Connect

- **Knapsack with values = weights.** Subset sum is the boolean/count specialization: capacity = target, "is `j` reachable?" instead of "max value." The 1-D array is the knapsack table with the item axis rolled away.
- **`dp[0]` is the generator.** `dp[0] = True` (boolean) or `dp[0] = 1` (count) is the seed every other cell descends from. It is *the* base case; omit it and the table is inert.
- **Descending = 0/1, ascending = unbounded.** Same loop, opposite semantics — straight from the [knapsack lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack). Subset selection (each number once) needs descending.
- **One pattern, three questions.** `or` answers *can we?*; `+=` *counts* subsets; building the full reachable set and scanning it answers *how close?* (minimum difference). The aggregator/read-out changes, the sweep doesn't.
- **The family.** Partition equal subset ([416](https://leetcode.com/problems/partition-equal-subset-sum/)), target sum with ± ([494](https://leetcode.com/problems/target-sum/)), last stone weight II ([1049](https://leetcode.com/problems/last-stone-weight-ii/)), count of subsets with a given sum. The [problems here](02-problems) drill the recognition; it's pseudo-polynomial and NP-complete in general, so watch the target's magnitude.

## Recall

<details>
<summary><strong>Q:</strong> What is the subset-sum recurrence and its dimensions?</summary>

**A:** A 1-D boolean array `dp[j]` over the target (`size target+1`). Seed `dp[0] = True`; for each number `x`, sweep `j` descending and set `dp[j] |= dp[j - x]`. The answer is `dp[target]`; cost `O(n · target)`.

</details>
<details>
<summary><strong>Q:</strong> Why must the inner loop sweep descending?</summary>

**A:** Descending means `dp[j - x]` still reflects subsets *without* the current `x`, so `x` is added at most once — the 0/1 constraint. Ascending reuses `x` within one pass, which solves the *unbounded* variant instead.

</details>
<details>
<summary><strong>Q:</strong> Why is <code>dp[0] = True</code> essential?</summary>

**A:** It encodes "the empty subset sums to 0" and is the only initially-`True` cell. The update `dp[j] |= dp[j-x]` can only propagate existing `True`s, so without the seed the array stays all-`False` and every query returns `False`.

</details>
<details>
<summary><strong>Q:</strong> How does subset sum relate to knapsack?</summary>

**A:** It's knapsack with each item's value equal to its weight, asking "is capacity `j` reachable?" instead of "max value." That lets the 2-D table collapse to a 1-D reachability array.

</details>
<details>
<summary><strong>Q:</strong> How do you solve minimum subset-sum difference with this pattern?</summary>

**A:** Run subset sum up to `total/2` to find all reachable sums, take the largest reachable `best ≤ total/2`, and return `total − 2·best` — the difference between the two groups when one sums to `best`.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., Ch. 34–35 — subset sum as a canonical NP-complete problem and its pseudo-polynomial DP.
- **LeetCode** 416 (Partition Equal Subset Sum), 494 (Target Sum), 1049 (Last Stone Weight II) are the canonical drills; the `true`/`false` reachability, the correct-vs-buggy seed on `[3,4,5]`, and the `3`/`1` minimum differences above all come from the runnable blocks — re-run to verify.
