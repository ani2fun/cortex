---
title: "Knapsack Applications"
summary: "Four famous problems that are knapsack in disguise — coin change (min), coin change II (count), rod cutting (max), subset sum (boolean). Same unbounded-knapsack table; the aggregator picks the problem. And in counting, the loop NESTING decides combinations vs permutations."
prereqs:
  - 05-algorithms-by-strategy/05-dynamic-programming/10-knapsack
---

## Why It Exists

The [knapsack lesson](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack) gave you one recurrence: decide what to take from a list under a budget. The payoff is how *many* unrelated-looking problems are that recurrence wearing a costume. A vending machine making change with the fewest coins? Unbounded knapsack, **min** aggregator. Cutting a steel rod into pieces for maximum revenue? Unbounded knapsack where the "items" are cut lengths, **max** aggregator. Counting the distinct ways to make change? **Sum** aggregator. Does a multiset contain a subset hitting a target? 0/1 knapsack, **boolean** aggregator.

The transferable skill isn't four algorithms — it's *recognising the shape* and *picking the aggregator*. Once you see "choose pieces under a budget, optimise/count some quantity," you already have the table; you only choose how to combine subproblems.

## See It Work

**Coin Change** ([LeetCode 322](https://leetcode.com/problems/coin-change/)) — the minimum number of coins to make `amount`, with unlimited copies of each coin. Coins are reusable, so this is *unbounded* knapsack (the ascending capacity sweep from last lesson), and the aggregator is **min**: `dp[a] = 1 + min(dp[a - c])` over coins `c` that fit.

```python run viz=array
import ast

def coin_change(coins, amount):
    INF = float('inf')
    dp = [0] + [INF] * amount                     # dp[a] = fewest coins to make a; dp[0] = 0
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a and dp[a - c] + 1 < dp[a]:
                dp[a] = dp[a - c] + 1             # take one coin c, add to best for (a - c)
    return dp[amount] if dp[amount] != INF else -1

coins = ast.literal_eval(input())
amount = int(input())
print(coin_change(coins, amount))
```

```java run viz=array
import java.util.*;

public class Main {
    static int coinChange(int[] coins, int amount) {
        int INF = Integer.MAX_VALUE;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, INF);
        dp[0] = 0;
        for (int a = 1; a <= amount; a++)
            for (int c : coins)
                if (c <= a && dp[a - c] != INF) dp[a] = Math.min(dp[a], dp[a - c] + 1);
        return dp[amount] == INF ? -1 : dp[amount];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] coins = parseIntArray(sc.nextLine());
        int amount = Integer.parseInt(sc.nextLine().trim());
        System.out.println(coinChange(coins, amount));
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
    { "id": "coins", "label": "coins", "type": "int[]", "placeholder": "[1, 2, 5]" },
    { "id": "amount", "label": "amount", "type": "int", "placeholder": "11" }
  ],
  "cases": [
    { "args": { "coins": "[1, 2, 5]", "amount": "11" }, "expected": "3" },
    { "args": { "coins": "[2]", "amount": "3" }, "expected": "-1" },
    { "args": { "coins": "[1]", "amount": "0" }, "expected": "0" },
    { "args": { "coins": "[1, 5, 10, 25]", "amount": "41" }, "expected": "4" }
  ]
}
```

Both print `3` then `-1`. Eleven is `5 + 5 + 1` (three coins, reusing the 5); three is unreachable with only 2-coins, so `-1`. Cost `O(amount · #coins)`.

## How It Works

Every one of these is the same `dp[budget]` table over reusable items; only the aggregator and the seed change:

```d2
direction: right
skeleton: "UNBOUNDED knapsack skeleton\nfor budget b ascending:\n  for each item x (reusable):\n    combine dp[b] with f(dp[b - x])" {style.fill: "#dbeafe"; style.stroke: "#3b82f6"}
minA: "MIN  -> coin change\ndp[a] = min(dp[a], dp[a-c] + 1)\nseed dp[0]=0, else INF" {style.fill: "#bbf7d0"; style.stroke: "#16a34a"}
maxA: "MAX  -> rod cutting\ndp[L] = max(dp[L], price[cut] + dp[L-cut])\nseed dp[0]=0" {style.fill: "#fde68a"; style.stroke: "#d97706"}
sumA: "SUM  -> coin change II (count)\ndp[a] += dp[a-c]\nseed dp[0]=1" {style.fill: "#fbcfe8"; style.stroke: "#db2777"}
boolA: "OR   -> subset sum (0/1, descending)\ndp[c] = dp[c] or dp[c-x]\nseed dp[0]=True" {style.fill: "#f3e8ff"; style.stroke: "#9333ea"}
skeleton -> minA
skeleton -> maxA
skeleton -> sumA
skeleton -> boolA
```

<p align="center"><strong>One table, four aggregators. MIN counts the fewest pieces, MAX optimises value, SUM counts arrangements, OR tests feasibility. Reusable items sweep capacity ascending (unbounded); one-shot items sweep descending (0/1, subset sum).</strong></p>

Two ideas carry the whole lesson:

- **The aggregator is the problem.** Swap `min` for `max` and "fewest coins" becomes "most rod revenue." Swap it for `+=` and you *count* solutions instead of optimising one. Swap it for `or` and you ask "is it even possible?" The table-filling loop never changes — recognising that is the entire knowledge transfer.
- **0/1 vs unbounded is still the loop direction.** Coin change, coin change II, and rod cutting allow unlimited copies → ascending capacity (an item's update can read a cell already updated with that item). Subset sum is 0/1 → descending. This is exactly the [knapsack](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack) loop-direction rule, reused.

> **Key takeaway.** Coin change (min), rod cutting (max), coin change II (count), subset sum (boolean) are *one* DP — knapsack — distinguished only by the **aggregator** (`min` / `max` / `+=` / `or`) and the 0/1-vs-unbounded **loop direction**. The hard part is seeing the disguise; the code is a one-line change.

## Trace It

Counting introduces a trap that optimising doesn't. To *count the distinct ways* to make an amount, you iterate coins and amounts — but **which loop is outer** silently changes what you count.

**Predict before you run:** with coins `{1, 2, 5}` and amount `5`, the standard "coins outer, amount inner" loop counts **4** ways. If you swap the nesting to "amount outer, coins inner" — same `dp[a] += dp[a-c]`, same arrays — does it still print `4`?

```python run viz=array
def count_combinations(coins, amount):            # coins OUTER -> unordered combinations
    dp = [1] + [0] * amount
    for c in coins:                               # fix coin c, then sweep amounts
        for a in range(c, amount + 1):
            dp[a] += dp[a - c]
    return dp[amount]

def count_permutations(coins, amount):            # amount OUTER -> ordered sequences
    dp = [1] + [0] * amount
    for a in range(1, amount + 1):                # fix amount a, then try every coin
        for c in coins:
            if c <= a:
                dp[a] += dp[a - c]
    return dp[amount]

print("coins outer (combinations):", count_combinations([1, 2, 5], 5))
print("amount outer (permutations):", count_permutations([1, 2, 5], 5))
```

<details>
<summary><strong>Reveal</strong></summary>

Coins-outer prints `4`; amount-outer prints `9`. The four *combinations* of `{1,2,5}` summing to 5 are `5`, `2+2+1`, `2+1+1+1`, `1+1+1+1+1`. The nine *permutations* additionally count orderings as distinct — `1+2+2`, `2+1+2`, `2+2+1` are three separate sequences, and so on.

Why the loop order decides this: with **coins outer**, by the time you start using coin `c`, every amount already reflects all *smaller-indexed* coins — so coin 1 is fully "used up" before coin 2 is introduced, and a combination is only ever built in one canonical order (non-decreasing coin index). With **amount outer**, every coin is reconsidered fresh at every amount, so `2 then 1` and `1 then 2` are both counted — that's ordered sequences. This is the [coin change II](https://leetcode.com/problems/coin-change-ii/) (combinations, LeetCode 518) vs [combination sum IV](https://leetcode.com/problems/combination-sum-iv/) (permutations, LeetCode 377) distinction, and getting it backwards is the single most common counting-DP bug. (Note this is *orthogonal* to last lesson's ascending-vs-descending direction — that toggles 0/1 vs unbounded; this toggles combinations vs permutations.)

</details>

## Your Turn

**Rod Cutting** (CLRS §15.1) — given prices for each length, cut a length-`n` rod to maximise total revenue. The "items" are cut-lengths, reusable (you can make many length-2 pieces), so it's unbounded knapsack with a **max** aggregator: `dp[L] = max(price[cut] + dp[L - cut])` over every first-cut length.

```python run viz=array
import ast

def rod_cutting(prices, n):
    # Your code goes here
    return 0

prices = ast.literal_eval(input())
n = int(input())
print(rod_cutting(prices, n))
```

```java run viz=array
import java.util.*;

public class Main {
    static int rodCutting(int[] prices, int n) {
        // Your code goes here
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] prices = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        System.out.println(rodCutting(prices, n));
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
    { "id": "prices", "label": "prices", "type": "int[]", "placeholder": "[1, 5, 8, 9, 10, 17, 17, 20]" },
    { "id": "n", "label": "n", "type": "int", "placeholder": "8" }
  ],
  "cases": [
    { "args": { "prices": "[1, 5, 8, 9, 10, 17, 17, 20]", "n": "8" }, "expected": "22" },
    { "args": { "prices": "[1, 5, 8, 9, 10, 17, 17, 20]", "n": "4" }, "expected": "10" },
    { "args": { "prices": "[3, 5, 8, 9]", "n": "3" }, "expected": "9" },
    { "args": { "prices": "[1]", "n": "1" }, "expected": "1" }
  ]
}
```

<details>
<summary>Editorial</summary>

Both languages print `22` then `10`. The length-8 rod earns most as a 2 + 6 split (`5 + 17`), beating selling it whole for `20`; length-4 is best as 2 + 2 (`5 + 5 = 10`), beating `9` whole. It's coin change's twin — `max` instead of `min`, "revenue" instead of "coin count" — which is exactly the point: you wrote it without learning a new algorithm.

```python solution time=O(n²) space=O(n)
import ast

def rod_cutting(prices, n):                       # prices[i] = price of a length-(i+1) piece
    dp = [0] * (n + 1)                            # dp[L] = max revenue from a length-L rod
    for L in range(1, n + 1):
        for cut in range(1, L + 1):
            dp[L] = max(dp[L], prices[cut - 1] + dp[L - cut])   # first piece = cut, rest optimal
    return dp[n]

prices = ast.literal_eval(input())
n = int(input())
print(rod_cutting(prices, n))
```

```java solution
import java.util.*;

public class Main {
    static int rodCutting(int[] prices, int n) {
        int[] dp = new int[n + 1];
        for (int L = 1; L <= n; L++)
            for (int cut = 1; cut <= L; cut++)
                dp[L] = Math.max(dp[L], prices[cut - 1] + dp[L - cut]);
        return dp[n];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] prices = parseIntArray(sc.nextLine());
        int n = Integer.parseInt(sc.nextLine().trim());
        System.out.println(rodCutting(prices, n));
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

</details>

## Reflect & Connect

- **The aggregator picks the problem.** `min` (coin change), `max` (rod cutting), `+=` (count ways), `or` (subset sum) — one knapsack table, four questions. Internalising this turns a dozen "new" problems into one you already know.
- **Loop direction = 0/1 vs unbounded.** Reusable items (coins, rod cuts) sweep capacity ascending; one-shot items (subset sum, [partition](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/knapsack)) sweep descending. Straight from the knapsack lesson.
- **Loop *nesting* = combinations vs permutations.** In counting DPs, coins-outer counts unordered combinations; amount-outer counts ordered sequences. Orthogonal to direction, and a notorious interview trap.
- **`-1` / `INF` sentinels matter.** Coin change must distinguish "0 coins" (amount 0) from "impossible" (no combination) — seed unreachable cells to infinity and translate to `-1` at the end. Off-by-a-sentinel is a classic bug.
- **Recognising the disguise is the meta-skill.** "Choose reusable/one-shot pieces under a budget, optimise or count a quantity" → knapsack. [Subset sum](/cortex/data-structures-and-algorithms/algorithms-by-strategy/dynamic-programming/pattern-subset-sum/pattern) gets its own pattern lesson; combination/permutation counting recurs throughout DP.

## Recall

<details>
<summary><strong>Q:</strong> What single DP underlies coin change, rod cutting, coin change II, and subset sum?</summary>

**A:** The knapsack table `dp[budget]` over items. They differ only in the **aggregator** — `min` (coin change), `max` (rod cutting), `+=` (coin change II count), `or` (subset sum) — and the 0/1-vs-unbounded loop direction.

</details>
<details>
<summary><strong>Q:</strong> What is the coin-change (min) recurrence?</summary>

**A:** `dp[a] = 1 + min(dp[a - c])` over coins `c ≤ a`, with `dp[0] = 0` and unreachable amounts seeded to infinity (returned as `-1`). Coins are reusable → unbounded → ascending amount sweep.

</details>
<details>
<summary><strong>Q:</strong> In counting ways to make change, why does loop nesting matter?</summary>

**A:** Coins outer (sweep amounts inside each coin) counts **combinations** — each multiset built in one canonical order. Amount outer (try every coin at each amount) counts **permutations** — orderings are distinct. Same `dp[a] += dp[a-c]`, different meaning.

</details>
<details>
<summary><strong>Q:</strong> How is rod cutting a knapsack?</summary>

**A:** The "items" are cut lengths with prices, reusable (many pieces of one length). It's unbounded knapsack with a `max` aggregator: `dp[L] = max(price[cut] + dp[L - cut])` over every first-cut length.

</details>
<details>
<summary><strong>Q:</strong> When do you sweep capacity ascending vs descending in these reductions?</summary>

**A:** Ascending for reusable items (coin change, rod cutting, coin change II — unbounded); descending for one-shot items (subset sum — 0/1). Direction controls whether an item can be reused within one pass.

</details>

## Sources & Verify

- **CLRS** (Cormen, Leiserson, Rivest, Stein), *Introduction to Algorithms*, 3rd ed., §15.1 — rod cutting, the textbook introduction to DP via an unbounded-knapsack reduction.
- **LeetCode** 322 (Coin Change, min), 518 (Coin Change II, combinations), 377 (Combination Sum IV, permutations), 416 (Partition Equal Subset Sum) are the canonical drills; the `3`/`-1`, the combinations-vs-permutations `4`/`9`, and the rod-cutting `22`/`10` above all come from the runnable blocks — re-run to verify.
